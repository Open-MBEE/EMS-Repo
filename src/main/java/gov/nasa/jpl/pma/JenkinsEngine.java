/**
 * JenkinsEngine ----
 * 
 * Implements the ExecutionEngine as a way to execute jobs (events) on the
 * Jenkins server.
 * 
 */
package gov.nasa.jpl.pma;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.offbytwo.jenkins.JenkinsServer;

// import gov.nasa.jpl.view_repo.util.JSONObject;

public class JenkinsEngine implements ExecutionEngine {
    static Logger logger = Logger.getLogger( JenkinsEngine.class );

    private JenkinsServer jenkins;
    private String username = "eurointeg"; // User name to be used to connect to
                                           // jenkins
    private String passwordOrToken = "dhcp3LugH#Meg!i"; // Token or password
                                                        // that is associated
                                                        // with the user name
    private String url = "https://cae-jenkins.jpl.nasa.gov"; // URL of the
                                                             // Jenkins server
                                                             // to execute the
                                                             // job on
    private String jenkinsToken = "build"; // The build the token associated
                                           // with the build configuration on
                                           // the Jenkins server.
    public String jobName = "MDKTest"; // Build name - the name of the job to be
                                       // executed on the Jenkins server.
    public String jenkinsApiURL = "/api/json?depth=";
    public int apiCallDepth = 1;
    public String executeUrl;
    public DefaultHttpClient jenkinsClient; //
    private long executionTime;
    public JSONObject jsonResponse; //
    public Map< String, String > detailResultMap;

    private BasicScheme basicAuth;
    private BasicHttpContext context;

    private enum detail {
                         NAME, COLOR, URL, DURATION, EST_DURATION, TIMESTAMP,
                         DESCRIPTION, LAST_SUCCESSFULL_BUILD, LAST_FAILED_BUILD,
                         LAST_COMPLETED_BUILD, LAST_UNSUCCESFULL_BUILD,
                         LAST_BUILD
    }

    public JenkinsEngine() {

        /**
         * Simple class to launch a jenkins build on run@Cloud platform, should
         * also work on every jenkins instance (not tested)
         *
         */

        // Credentials
        String username = this.username;
        String password = this.passwordOrToken;
        String jenkinsUrl;

        // Jenkins url
        jenkinsUrl = url + jenkinsApiURL + apiCallDepth;

        // jenkinsUrl = "https://cae-jenkins.jpl.nasa.gov/api/json?depth=2";

        // Create your httpclient
        this.jenkinsClient = new DefaultHttpClient();

        // Then provide the right credentials
        this.jenkinsClient.getCredentialsProvider()
                          .setCredentials( new AuthScope( AuthScope.ANY_HOST,
                                                          AuthScope.ANY_PORT ),
                                           new UsernamePasswordCredentials( username,
                                                                            password ) );

        // Generate BASIC scheme object and stick it to the execution
        // context
        this.basicAuth = new BasicScheme();
        this.context = new BasicHttpContext();

        this.context.setAttribute( "preemptive-auth", basicAuth );

        // Add as the first (because of the zero) request interceptor
        // It will first intercept the request and preemptively
        // initialize the authentication scheme if there is not
        this.jenkinsClient.addRequestInterceptor( new PreemptiveAuth(), 0 );

        // You get request that will start the build
        // Example for setting a build REST call:
        // String getUrl = jenkinsUrl + "/job/" + jobName + "/build?token="
        // + buildToken;
        String getUrl = jenkinsUrl;
        System.out.println( "The Build url is " + getUrl );
        HttpGet get = new HttpGet( getUrl );

        try {
            HttpResponse response =
                    this.jenkinsClient.execute( get, this.context );
            HttpEntity entity = response.getEntity();
            String retSrc = EntityUtils.toString( entity );
            jsonResponse = new JSONObject( retSrc );
            System.out.println( "Content of the JSON Object is "
                                + jsonResponse.toString() );
            System.out.println();
            EntityUtils.consume( entity );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Preemptive authentication interceptor
     *
     */
    static class PreemptiveAuth implements HttpRequestInterceptor {

        /*
         * @see org.apache.http.HttpRequestInterceptor#process(org.apache.
         * http.HttpRequest, org.apache.http.protocol.HttpContext)
         */
        public void process( HttpRequest request,
                             HttpContext context ) throws HttpException,
                                                   IOException {
            // Get the AuthState
            AuthState authState =
                    (AuthState)context.getAttribute( ClientContext.TARGET_AUTH_STATE );

            // If no auth scheme available yet, try to initialize it
            // preemptively
            if ( authState.getAuthScheme() == null ) {
                AuthScheme authScheme =
                        (AuthScheme)context.getAttribute( "preemptive-auth" );
                CredentialsProvider credsProvider =
                        (CredentialsProvider)context.getAttribute( ClientContext.CREDS_PROVIDER );
                HttpHost targetHost =
                        (HttpHost)context.getAttribute( ExecutionContext.HTTP_TARGET_HOST );
                if ( authScheme != null ) {
                    Credentials creds =
                            credsProvider.getCredentials( new AuthScope( targetHost.getHostName(),
                                                                         targetHost.getPort() ) );
                    if ( creds == null ) {
                        throw new HttpException( "No credentials for preemptive authentication" );
                    }
                    authState.setAuthScheme( authScheme );
                    authState.setCredentials( creds );
                }
            }

        }

    }

    /**
     * Sets the username to be used with the connection on Jenkins
     * 
     * @param name
     */
    public void setUsername( String name ) {
        this.username = name;
    }

    /**
     * Sets the password that is associated with the username that will be
     * connected with Jenkins
     * 
     * @param pass
     */
    public void setPassword( String pass ) {
        this.passwordOrToken = pass;
    }

    /**
     * This method will set the job that will be executed the by the
     * JenkinsEngine.
     * 
     * @param job
     */
    public void setJob( String job ) {
        this.jobName = job;
    }

    /**
     * This method is used to set the token that is required when attempting to
     * execute a build on the jenkins server.
     * 
     * @param token
     */
    public void setJobToken( String token ) {
        this.jenkinsToken = token;
    }

    /**
     * Creates an instance of the Jenkins Engine
     */
    @Override
    public void createEngine() {}

    @Override
    public void execute() {
        // This sets the URL to an Object specifically for making GET calls
        HttpGet get = new HttpGet( this.executeUrl );
        String entityString;

        try {
            // This will tell the Jenkins HTTP Client to execute the GET
            // call with the context that was set during the instantiation
            // of the Jenkins HttpClient.
            HttpResponse response = jenkinsClient.execute( get, this.context );

            // Takes the HttpResponse and turns it into an Entity that can
            // be manipulated into a string.
            HttpEntity entity = response.getEntity();
            entityString = EntityUtils.toString( entity );

            // Converts the HttpEntity String from the response of the GET
            // call into a JSON object then consumes the entity to close the
            // connection.
            jsonResponse = new JSONObject( entityString );
            EntityUtils.consume( entity );

            // Will throw an error if the execution fails from either incorrect
            // setup or if the jenkinsClient has not been instantiated.
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean isRunning() {
        return jenkins != null;
    }

    @Override
    public int getExecutionStatus() {
        if ( jenkins.isRunning() ) {

        }
        return 0;
    }

    /**
     * This method is used to find the job that the user specifies within
     * <b>eventName</b> and specifying which detail they would like from the
     * job. <b>detailName</b> These are the parameters it accepts:
     * <ul>
     * <li>name
     * <li>url
     * <li>failed
     * <li>successful
     * <li>unsuccessful
     * <li>stable
     * <li>unstable
     * </ul>
     * 
     * @param String
     *            eventName, String detailName
     * @return Event details in a string form
     * @Override
     */
    public String getEventDetail( String jobName, String detailName ) {
        String returnString = null;

        if ( !detailName.isEmpty() && jsonResponse != null ) {
            try {

            } catch ( Exception e ) {

            }
        }

        return returnString;
    }

    /**
     * DO NOT USE --- Exception Handling Not Implemented!
     * 
     * @param detailName
     * @return
     */
    public String getEventDetails( List< String > detailName ) {
        String returnString = "";
        // if ( !detailName.isEmpty() && jsonResponse != null ) {
        // for ( String det : detailName ) {
        // System.out.println( "Detail name : "
        // + jsonResponse.get( det ).toString() );
        // detailResultMap.put( det, jsonResponse.get( det ).toString() );
        // returnString += jsonResponse.getString( det ).toString() + ", ";
        // }
        // }
        return returnString;
    }

    @Override
    public void setEvent( String event ) {
        try {
            String eventXml = jenkins.getJobXml( event );
            jenkins.createJob( event, eventXml );
        } catch ( IOException e ) {
            // some exception
        }
    }

    @Override
    public void setEvents( List< String > events ) {
        for ( String event : events )
            setEvent( event );
    }

    @Override
    public boolean stopExecution() {
        return false;
    }

    @Override
    public boolean removeEvent( String event ) {
        return false;
    }

    @Override
    public void updateEvent( String event ) {
        // TODO Auto-generated method stub
    }

    @Override
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * Private method for constructing urls to be executed on Jenkins.
     * 
     * Allowed Arguments for Detail Property:
     * <ul>
     * <li>NAME
     * <li>COLOR
     * <li>URL
     * <li>DURATION
     * <li>EST_DURATION
     * <li>TIMESTAMP
     * <li>DESCRIPTION
     * <li>LAST_SUCCESSFULL_BUILD
     * <li>LAST_FAILED_BUILD
     * <li>LAST_COMPLETED_BUILD
     * <li>LAST_UNSUCCESFULL_BUILD
     * <li>LAST_BUILD
     * </ul>
     * 
     * @param jobUrl
     * @param property
     */
    private void constructJobUrl( detail property ) {
        String url;

        url = "/api/json?tree=jobs";
        System.out.println( "Current constuction url is " + url );

        switch ( property ) {
            case NAME:
                url = url + "[name]";
                break;
            case URL:
                url = url + "[url]";
                break;
            case COLOR:
                url = url + "[color]";
                break;
            case LAST_COMPLETED_BUILD:
                url = url + "[lastCompletedBuild]";
                break;
            case LAST_FAILED_BUILD:
                url = url + "[lastFailedBuild]";
                break;
            case LAST_SUCCESSFULL_BUILD:
                url = url + "[lastSuccessfullBuild]";
                break;
            case LAST_UNSUCCESFULL_BUILD:
                url = url + "[lastUnsuccesfullBuild]";
                break;
            case DESCRIPTION:
                url = url + "[description]";
                break;
            case LAST_BUILD:
                url = url + "[lastBuild]";
        }
        this.executeUrl = this.url + url;
    }

    private void constructBuildUrl( String jobUrl, detail property ) {

        String url;

        if ( !jobUrl.startsWith( "/" ) ) {
            jobUrl = "/" + jobUrl;
        }
        url = "/job" + jobUrl;

        if ( !url.endsWith( "/" ) ) {
            url = url + "/";
        }

        url = url + "api/json?tree=builds";

        System.out.println( "Current constuction url is " + url );

        switch ( property ) {
            case NAME:
                url = url + "[displayName]";
                break;
            case URL:
                url = url + "[url]";
                break;
            case DURATION:
                url = url + "[duration]";
                break;
            case EST_DURATION:
                url = url + "[estimatedDuration]";
                break;
            case TIMESTAMP:
                url = url + "[timestamp]";
                break;
            case DESCRIPTION:
                url = url + "[description]";
                break;
            default:
                url = "";
        }
        this.executeUrl = this.url + url;
        System.out.println( "Execution url is " + this.executeUrl );
    }
    public JSONArray getJobUrls(){
        JSONArray obj;

        constructJobUrl(detail.URL);
        execute();
        obj = jsonResponse.getJSONArray( "jobs" );

        return obj;

    }
}