package gov.nasa.jpl.view_repo.webscripts.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.alfresco.repo.webservice.authentication.AuthenticationFault;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class ShareUtils {
    static Logger logger = Logger.getLogger(SitePermSync.class);
    
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final String UTF_8 = "UTF-8";
    private static String BASE_URL = "http://localhost:8081/share";
    private static final String LOGIN_URL = BASE_URL + "/page/dologin";
    private static final String CREATE_SITE_URL = BASE_URL + "/page/modules/create-site";
    private static String alfrescoUsername = "admin";
    private static String alfrescoPwd = "admin";

    public static void constructSiteDashboard(String sitePreset, String siteId, String siteTitle, String siteDescription, boolean isPublic) {
        HttpClient httpClient = new HttpClient();
        
        String loginData = "username=" + alfrescoUsername + "&password=" + alfrescoPwd;
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
                    logger.debug(callName + " with user " + alfrescoUsername);
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
}
