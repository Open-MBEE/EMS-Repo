package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.webservice.authentication.AuthenticationFault;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class ShareUtils {
    static Logger logger = Logger.getLogger(ShareUtils.class);
    
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final String UTF_8 = "UTF-8";
    private static String SHARE_URL = null;
    private static String REPO_URL = null;
    private static String LOGIN_URL = null;
    private static String CREATE_SITE_URL = null;
    private static String UPDATE_GROUP_URL = null;
    private static String username = "admin";
    private static String password = "admin";
    private static ServiceRegistry services = null;
    
    /**
     * Initialize the URLs based on the Alfresco system settings.
     */
    private static void initializeUrls() {
        // note this handling is due to the way apache serves as https proxy for
        // tomcat
        if (SHARE_URL == null) {
            SysAdminParams adminParams = services.getSysAdminParams();
            int repoPort = adminParams.getAlfrescoPort();
            int sharePort = repoPort;
            if (8080 == repoPort) {
                // this means we're running using maven, so share is on different port
                sharePort = 8081;
            } else {
                // production machine, use local ports to access
                repoPort = 8080;  
                sharePort = 8080;
            }
            
            // use loopback, secure internally
            String protocol = "http"; 
            String host = "127.0.0.1";

            REPO_URL = String.format("%s://%s:%s/alfresco", protocol, host, repoPort);
            SHARE_URL = String.format("%s://%s:%s/share", protocol, host, sharePort);
            LOGIN_URL = SHARE_URL + "/page/dologin";
            CREATE_SITE_URL = SHARE_URL + "/page/modules/create-site";
            UPDATE_GROUP_URL = REPO_URL + "/service/api/groups";
        }
        if (logger.isInfoEnabled()) {
            logger.info( String.format("Repo URL: %s", REPO_URL ) );
            logger.info( String.format("Share URL: %s", SHARE_URL ) );
            logger.info( String.format("Login URL: %s", LOGIN_URL ) );
            logger.info( String.format("Create Site URL: %s", CREATE_SITE_URL ) );
            logger.info( String.format("Update Group URL: %s", UPDATE_GROUP_URL ) );
        }
    }
    
    
    /**
     * Create the site dashboard by using the Share createSite service, then make the current
     * user a manager for the specified site
     * @param sitePreset
     * @param siteId
     * @param siteTitle
     * @param siteDescription
     * @param isPublic
     * @return
     */
    public static boolean constructSiteDashboard(String sitePreset, String siteId, String siteTitle, String siteDescription, boolean isPublic) {
        // only description is allowed to be null
        if (sitePreset == null || siteId == null || siteTitle == null ) {
            logger.error(String.format("Fields cannot be null: sitePreset:%s  siteId:%s  siteTitle:%s", 
                                        sitePreset, siteId, siteTitle));
            return false;
        }
        
        if (siteDescription == null) {
            siteDescription = "";
        }
        
        initializeUrls();
        HttpClient httpClient = new HttpClient();
        
        String loginData = "username=" + username + "&password=" + password;
        
        if (false == makeSharePostCall(httpClient, LOGIN_URL, loginData, CONTENT_TYPE_FORM, "Login to Alfresco Share", HttpStatus.SC_MOVED_TEMPORARILY)) {
            logger.error("Could not login to share site");
            return false;
        }
        
        JSONObject json = new JSONObject();
        try {
            json.put("shortName", siteId);
            json.put( "sitePreset", sitePreset );
            json.put( "title", siteTitle );
            json.put( "description", siteDescription );
            json.put( "isPublic", isPublic );
        } catch ( JSONException e ) {
            e.printStackTrace();
            logger.error( "Could not create JSON for site creation" );
            return false;
        }
        if (false == makeSharePostCall(httpClient, CREATE_SITE_URL, NodeUtil.jsonToString( json ) , CONTENT_TYPE_JSON, "Create site with name: " + siteId, HttpStatus.SC_OK)) {
            logger.error("Could not create site - 1st pass");
            return false;
        }
        // for some reason need to do this twice unsure why this is the case
        if (false == makeSharePostCall(httpClient, CREATE_SITE_URL, NodeUtil.jsonToString( json ) , CONTENT_TYPE_JSON, "Create site with name: " + siteId, HttpStatus.SC_OK)) {
            logger.error("Could not create site -2nd pass");
            return false;
        }
        
        // make calling user Site manger
        // NOTE: need additional site_, because short name is prepended with site_
        String role = String.format("site_%s_SiteManager", siteId);
        String currentUsername = services.getAuthenticationService().getCurrentUserName();
        String groupUrl = String.format("%s/%s/children/%s", UPDATE_GROUP_URL, role, currentUsername);
        if (false == makeRepoPostCall(groupUrl, CONTENT_TYPE_JSON, String.format("add user, %s, as %s site manager", currentUsername, siteId), HttpStatus.SC_OK)) {
            logger.error( "Could not set permissions on site" );;
            return false;
        }
        
        return true;
    }
    
    private static boolean makeSharePostCall(HttpClient httpClient, String url, String data, String dataType,
                              String callName, int expectedStatus) {
        boolean success = false;
        PostMethod postMethod = null;
        try {
            postMethod = createPostMethod(url, data, dataType);
            int status = httpClient.executeMethod(postMethod);

            if (logger.isDebugEnabled()) {
                logger.debug(callName + " returned status: " + status);
            }

            if (status == expectedStatus) {
                if (logger.isDebugEnabled()) {
                    logger.debug(callName + " with user " + username);
                }
                success = true;
            } else {
                logger.error("Could not " + callName + ", HTTP Status code : " + status);
            }
        } catch (HttpException he) {
            logger.error("Failed to " + callName, he);
        } catch (AuthenticationFault ae) {
            logger.error("Failed to " + callName, ae);
        } catch (IOException ioe) {
            logger.error("Failed to " + callName, ioe);
        } finally {
            postMethod.releaseConnection();
        }
        
        return success;
    }
    
    private static PostMethod createPostMethod(String url, String body, String contentType)
            throws UnsupportedEncodingException {
        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestHeader(HEADER_CONTENT_TYPE, contentType);
        postMethod.setRequestEntity(new StringRequestEntity(body, CONTENT_TYPE_TEXT_PLAIN, UTF_8));
        if (url.contains( "service/alfresco" )) {
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
            postMethod.setRequestHeader( "Authorization", basicAuth );
        }

        return postMethod;
    }
    
    public static void setUsername(String username) {
        ShareUtils.username = username;
    }
    
    public static void setPassword(String password) {
        ShareUtils.password = password;
    }
    
    public static void setServices(ServiceRegistry services) {
        ShareUtils.services = services;
    }

    // Used for testing purposes, will bootstrap administrator users in
//    public static void createAdminUser() {
//        AuthenticationUtil.runAs( new AuthenticationUtil.RunAsWork< NodeRef >() {
//            @Override
//            public NodeRef doWork() throws Exception {
//                UserTransaction trx;
//                trx = services.getTransactionService().getNonPropagatingUserTransaction();
//                try {
//                    trx.begin();
//                    services.getAuthenticationService().createAuthentication( username, password.toCharArray() );
//                    if (services.getPersonService().getPerson( username ) == null) {
//                        Map< QName, Serializable > properties = new HashMap< QName, Serializable >();
//                        properties.put( ContentModel.PROP_USERNAME, username );
//                        properties.put( ContentModel.PROP_FIRSTNAME, "MMS" );
//                        properties.put( ContentModel.PROP_LASTNAME, "Admin");
//                        properties.put( ContentModel.PROP_EMAIL, "mmsadmin@ems.gov" );
//                        services.getPersonService().createPerson( properties );
//                        
//                        services.getAuthorityService().addAuthority( "GROUP_ALFRESCO_ADMINISTRATORS", username );
//                    }
//                    trx.commit();
//                } catch (Throwable e) {
//                }
//                                             
//                return null;
//            }
//        }, "admin" );
//    }
    
    /**
     * Method for posting to repository (requires basic authentication that doesn't seem to
     * work with the other call type)
     * @param targetURL
     * @param dataType
     * @param callName
     * @param expectedStatus
     * @return
     */
    private static boolean makeRepoPostCall(String targetURL, String dataType,
                                           String callName, int expectedStatus) {
        logger.debug( String.format("posting to %s", targetURL) );
        boolean success = false;
        
        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty(HEADER_CONTENT_TYPE, dataType);
                                    
            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
            connection.setRequestProperty( "Authorization", basicAuth );

            //Get Response    
            if (connection.getResponseCode() == expectedStatus) {
                success = true;
            } else {
                logger.error( String.format("failed request: " + targetURL) );
            }
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            if(connection != null) {
              connection.disconnect(); 
            }
          }
        return success;
    }
}
