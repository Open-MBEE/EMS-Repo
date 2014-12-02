package gov.nasa.jpl.view_repo.webscripts.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.webservice.authentication.AuthenticationFault;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.alfresco.repo.security.authentication.AuthenticationUtil;

public class ShareUtils {
    static Logger logger = Logger.getLogger(ShareUtils.class);
    
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final String UTF_8 = "UTF-8";
    private static String BASE_URL = "http://localhost:8081/share";
    private static final String LOGIN_URL = BASE_URL + "/page/dologin";
    private static final String CREATE_SITE_URL = BASE_URL + "/page/modules/create-site";
    private static String username = "admin";
    private static String password = "admin";
    private static ServiceRegistry services = null;

    public static void constructSiteDashboard(String sitePreset, String siteId, String siteTitle, String siteDescription, boolean isPublic) {
        HttpClient httpClient = new HttpClient();
  
        String loginData = "username=" + username + "&password=" + password;
        makePostCall(httpClient, LOGIN_URL, loginData, CONTENT_TYPE_FORM, "Login to Alfresco Share", HttpStatus.SC_MOVED_TEMPORARILY);
        
        JSONObject json = new JSONObject();
        try {
            json.put("shortName", siteId);
            json.put( "sitePreset", sitePreset );
            json.put( "title", siteTitle );
            json.put( "description", siteDescription );
            json.put( "isPublic", isPublic );
        } catch ( JSONException e ) {
            e.printStackTrace();
            return;
        }
        makePostCall(httpClient, CREATE_SITE_URL, json.toString() , CONTENT_TYPE_JSON, "Create site with name: " + siteId, HttpStatus.SC_OK);
        // for some reason need to do this twice unsure why this is the case
        makePostCall(httpClient, CREATE_SITE_URL, json.toString() , CONTENT_TYPE_JSON, "Create site with name: " + siteId, HttpStatus.SC_OK);
    }
    
    private static void makePostCall(HttpClient httpClient, String url, String data, String dataType,
                              String callName, int expectedStatus) {
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
    }
    
    private static PostMethod createPostMethod(String url, String body, String contentType)
            throws UnsupportedEncodingException {
        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestHeader(HEADER_CONTENT_TYPE, contentType);
        postMethod.setRequestEntity(new StringRequestEntity(body, CONTENT_TYPE_TEXT_PLAIN, UTF_8));

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
    
    public static void createAdminUser() {
//        AuthenticationUtil.runAs( new AuthenticationUtil.RunAsWork< NodeRef >() {
//            @Override
//            public NodeRef doWork() throws Exception {
                NodeRef personRef = services.getPersonService().getPerson( username );
                if ( personRef == null ) {
                    UserTransaction trx;
                    trx = services.getTransactionService().getNonPropagatingUserTransaction();
                    try {
                        trx.begin();
                        Map< QName, Serializable > properties = new HashMap< QName, Serializable >();
                        properties.put( ContentModel.PROP_USERNAME, username );
                        properties.put( ContentModel.PROP_FIRSTNAME, "MMS" );
                        properties.put( ContentModel.PROP_LASTNAME, "Admin");
                        properties.put( ContentModel.PROP_EMAIL, "mmsadmin@ems.gov" );
                        properties.put( ContentModel.PROP_PASSWORD, password );
                        personRef = services.getPersonService().createPerson( properties );
//
//                        QName firstNameQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}firstName");
//                        QName lastNameQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}lastName");
//                        QName passwordQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}password");
//                        QName homeFolderQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}homeFolder");
//                        QName usernameQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}username");
//                     
//                        Map<QName, Serializable> props = null;
//                        props = new HashMap<QName, Serializable>(4);
//                        
//                        props.put(firstNameQName, "MMS Admin");
//                        props.put(lastNameQName, "");
//                        props.put(passwordQName, Hex.encodeHex( md4(password) ));
//                        props.put( homeFolderQName, username );
//                        props.put( usernameQName, username );
//                    
//                        personRef = services.getPersonService().createPerson( props );
                        trx.commit();
                    } catch (Throwable e) {
                        
                    }
                }
                
                UserTransaction trx;
                trx = services.getTransactionService().getNonPropagatingUserTransaction();
                try {
                    trx.begin();
                    services.getAuthenticationService().setAuthenticationEnabled( username, true );
//                    services.getAuthenticationService().setAuthentication( username, Hex.encodeHex( md4(password) ) );
                    services.getPermissionService().setPermission( personRef, username, services.getPermissionService().getAllPermission(), true );
                    services.getAuthenticationService().setAuthentication( username, password.toCharArray() );
                    services.getAuthorityService().addAuthority( "GROUP_ALFRESCO_ADMINISTRATORS", username );
                    trx.commit();
                } catch (Throwable e) {
                }
                
//                return personRef;
//            }
//        }, "admin" );
        
    }
    
    private static byte[] md4(String input) {
        try         
        {         
            MessageDigest digester = MessageDigest.getInstance("MD4");             
            return digester.digest(input.getBytes("UnicodeLittleUnmarked"));             
        }         
        catch (NoSuchAlgorithmException e)         
        {         
            throw new RuntimeException(e.getMessage(), e);             
        }         
        catch (UnsupportedEncodingException e)         
        {         
            throw new RuntimeException(e.getMessage(), e);             
        }         
    }     
    
}
