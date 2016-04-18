/**
 * JenkinsEngine ----
 * 
 * Implements the ExecutionEngine as a way to execute jobs (events) on the
 * Jenkins server.
 *
 * @author Dan Karlsson (dank)
 * @date 2/04/16
 * 
 */
package gov.nasa.jpl.pma;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsConfig;

/**
 * Implements the ExecutionEngine as a way to execute jobs (events) on the
 * Jenkins server.
 *<p>
 * Example jenkins api queries:
 * <ul>
 * <li>
 * https://some-jenkins-server.someorganization.com/job/MMS_1460067117709_b5f26105-8581-406e-b54d-8525012044c5/lastBuild/api/json?pretty=true
 * <li>
 * https://some-jenkins-server.someorganization.com/job/MMS_1460074360091_e6271b6a-0bb6-46d3-8283-0c2aaa7d1866/lastBuild/api/json?tree=building,result&pretty=true
 * <li>
 * https://some-jenkins-server.someorganization.com/api/json?tree=jobs[name,description,color,url,lastCompletedBuild[duration,timestamp,estimatedDuration]]&pretty=true
 * </ul>
 * 
 */
public class JenkinsEngine implements ExecutionEngine {
    static Logger logger = Logger.getLogger( JenkinsEngine.class );

    private String username = EmsConfig.get( "jenkins.user" ); // User name to be used to connect to jenkins
    
    private String passwordOrToken = EmsConfig.get( "jenkins.pass" ); // Token or password
                                                        // that is associated
                                                        // with the user name
    
    // TODO: WE NEED TO ADJUST THE URL ACCORDING TO ANY SERVER,
    //       DEPENDING WHERE THE USER CHOOSES TO 
    private String url = EmsConfig.get( "jenkins.url" ); // URL of the
                                                             // Jenkins server
                                                             // to execute the
                                                             // job on
    private String jenkinsToken = "build"; // The build the token associated
                                           // with the build configuration on
                                           // the Jenkins server.
    public String jobName = ""; // Build name - the name of the job to be
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

    public enum detail {
                        NAME, COLOR, URL, DURATION, EST_DURATION, TIMESTAMP,
                        DESCRIPTION, LAST_SUCCESSFULL_BUILD, LAST_FAILED_BUILD,
                        LAST_COMPLETED_BUILD, LAST_UNSUCCESFULL_BUILD,
                        LAST_BUILD
    }

    private boolean DEBUG = false;

    /**
     * This is the main constructor for using the JenkinsEngine interface. The
     * constructor will create the initial connection to the server that is
     * specified before calling 'new' on JenkinsEngine. It is required that the
     * JenkinesEngine is initialized before attempting to make any queries to
     * the jenkins server because Jenkins will require any calls made to be
     * authenticated before completing.
     */
    public JenkinsEngine() {

        /**
         * Simple class to launch a jenkins build on run@Cloud platform, should
         * also work on every jenkins instance (not tested)
         * 
         *
         */

        // Credentials
        String username = this.username;
        String password = this.passwordOrToken;
        String jenkinsUrl;

        jenkinsUrl = url + jenkinsApiURL + apiCallDepth;

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
        if ( DEBUG ) {

            String getUrl = jenkinsUrl;
            if (logger.isDebugEnabled()) logger.debug( "The Build url is " + getUrl );
            HttpGet get = new HttpGet( getUrl );

            try {
                HttpResponse response =
                        this.jenkinsClient.execute( get, this.context );
                HttpEntity entity = response.getEntity();
                String retSrc = EntityUtils.toString( entity );
                jsonResponse = new JSONObject( retSrc );
                logger.debug( "Content of the JSON Object is "
                                    + jsonResponse.toString() );
                EntityUtils.consume( entity );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
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
            
            // COMMENTED OUT BECAUSE THIS WILL CLOSE THE CONNECTION WHEN
            // YOU GET JSON BUT NEEDS TO STAY OPEN FOR XML TOO
            
            //EntityUtils.consume( entity );

            // Will throw an error if the execution fails from either incorrect
            // setup or if the jenkinsClient has not been instantiated.
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    public void build() {
        // This sets the URL to an Object specifically for making GET calls
        HttpPost post = new HttpPost( this.executeUrl );
        
        try {
            HttpResponse response = 
                    this.jenkinsClient.execute( post, this.context );
            
            EntityUtils.consume( response.getEntity() );
            // Will throw an error if the execution fails from either incorrect
            // setup or if the jenkinsClient has not been instantiated.
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    public void closeConn( HttpEntity entity) {
        
    }

    @Override
    public boolean isRunning() {
        return this.jenkinsClient != null;
    }

    @Override
    public int getExecutionStatus() {
        return 0;
    }

    /**
     * This method is used to find the job that the user specifies within
     * <b>jobName</b> and specifying which detail they would like from the
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
     *            jobName, String detailName
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
    public void setEvent( String event ) {}

    @Override
    public void setEvents( List< String > events ) {

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
    
    public String getMagicDrawLogFromJob( String jobId ) {
        String url;

        if ( !jobId.startsWith( "/" ) ) {
            jobId = "/" + jobId;
        }
        url = "/job" + jobId;

        if ( !url.endsWith( "/" ) ) {
            url = url + "/";
        }

        url = this.url + url + "lastSuccessfulBuild/artifact/MDNotificationWindowText.html"; 
        return url;
        //this.executeUrl = this.url + url;
    }

    
    // https://some-jenkins-server.someorganization.com/job/MMS_1460067117709_b5f26105-8581-406e-b54d-8525012044c5/lastBuild/api/json?pretty=true
    
    
    /**
     * Private method for constructing urls to be executed on Jenkins.
     * 
     * Allowed Arguments for Detail Property:
     * <ul>
     * <li>NAME
     * <li>URL
     * <li>COLOR
     * <li>LAST_COMPLETED_BUILD
     * <li>LAST_FAILED_BUILD
     * <li>LAST_SUCCESSFULL_BUILD
     * <li>LAST_UNSUCCESFULL_BUILD
     * <li>DESCRIPTION 
     * <li>LAST_BUILD
     * </ul>
     * 
     * @param jobUrl
     * @param property
     */
    public void constructJobUrl( detail property ) {
        String url;

        url = "/api/json?tree=jobs";

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
            default:
                break;
        }
        this.executeUrl = this.url + url;
        if (logger.isDebugEnabled()) logger.debug( "Execution url is " + this.executeUrl );
    }

    public void constructBuildUrl( String jobUrl, detail property ) {

        String url;

        if ( !jobUrl.startsWith( "/" ) ) {
            jobUrl = "/" + jobUrl;
        }
        url = "/job" + jobUrl;

        if ( !url.endsWith( "/" ) ) {
            url = url + "/";
        }

        url = url + "api/json?tree=";

        if (logger.isDebugEnabled()) logger.debug( "Current constuction url is " + url );

        switch ( property ) {
            case NAME:
                url = url + "displayName";
                break;
            case URL:
                url = url + "url";
                break;
            case DURATION:
                url = url + "lastCompletedBuild[duration]";
                break;
            case EST_DURATION:
                url = url + "lastCompletedBuild[estimatedDuration]";
                break;
            case TIMESTAMP:
                url = url + "lastCompletedBuild[timestamp]";
                break;
            case DESCRIPTION:
                url = url + "description";
                break;
            default:
                url = "";
        }
        this.executeUrl = this.url + url;
        if (logger.isDebugEnabled()) logger.debug( "Execution url is " + this.executeUrl );
    }

    public void constructAllJobs() {        
        String url = this.url + "/api/json?tree=jobs[name,description,color,url,lastCompletedBuild[duration,timestamp,estimatedDuration]]";
        
        if (logger.isDebugEnabled()) logger.debug( "Current constuction url is " + url );
        this.executeUrl = url;
        if (logger.isDebugEnabled()) logger.debug( "Execution url is " + this.executeUrl );
    }

    public JSONObject configXmlToJson(String jobUrl) throws SAXException, ParserConfigurationException {
        String getUrl = jobUrl + "config.xml";

        JSONObject o = new JSONObject();
        
        HttpGet get = new HttpGet( getUrl );
           
        try {
            HttpResponse response =
                    this.jenkinsClient.execute( get, this.context );
            HttpEntity entity = response.getEntity();
            String xml = EntityUtils.toString( entity );
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();           
            Document doc = db.parse( new InputSource( new StringReader( xml )) );
            
            // get the first element
            Element element = doc.getDocumentElement();
            
            // if there is a schedule for the job, add the property 
            if( element.getElementsByTagName( "spec" ).getLength() > 0) 
                o.put( "schedule", 
                       element.getElementsByTagName( "spec" )
                       .item( 0 )
                       .getTextContent()
                       .replaceAll( "\\n", " " ) );           
            else 
                o.put( "schedule", JSONObject.NULL );
              
            // NOTE: THIS WILL LEAVE THE CONNECTION OPEN, WE MIGHT NOT WANT THIS.
            //EntityUtils.consume( entity );           
        } catch ( IOException e ) {
            e.printStackTrace();
        }
          catch ( SAXException e ) {
            e.printStackTrace();
        } catch ( ParserConfigurationException e ) {
            e.printStackTrace();
        }
        return o; 
    }
    
    // This should be called when you change the name, status, schedule of a job
    public boolean postConfigXml( JenkinsBuildConfig config,String jobName, boolean newConfig ) {
        String postUrl = null;
        if( newConfig ) {
            postUrl = this.url + "/view/DocWeb%20(cae-ems-uat)/createItem?name=" + jobName;
        }
        else {
            postUrl = this.url + "/job/" + jobName + "/config.xml";
        }
        
        String configFile = generateConfigXML( config );
        
        if( configFile == null ) {
            logger.error(
                "FAILED TO CREATE JOB: " + jobName);
            return false;
        }
        
        try {
            HttpEntity xmlEntity = (HttpEntity)new  StringEntity(configFile);
            
            HttpPost post = new HttpPost( postUrl );
            post.setHeader( "Content-Type", "application/xml" );
            post.setEntity( xmlEntity );
            HttpResponse response = 
                    this.jenkinsClient.execute( post, this.context );
            
            EntityUtils.consume( response.getEntity() );
        } catch( Exception e ) {
            e.printStackTrace();
        }
        return true;
    }
    
    public JSONObject getJob( String jobName ) {
        JSONObject json = null;
        JSONObject allJobs = getAllJobs();
        if ( allJobs == null ) return null;
        JSONArray jobs = allJobs.optJSONArray( "jobs" );
        if ( jobs == null || jobs.length() <= 0 ) return null;
        for ( int i = 0; i < jobs.length(); ++i ) {
            JSONObject job = jobs.optJSONObject( i );
            if ( job == null ) continue;
            String name = job.optString("name");
            if ( !Utils.isNullOrEmpty( name ) && name.equals( jobName ) ) {
                json = job;
                break;
            }
        }
        return json;
    }

    
    public JSONObject getAllJobs() {
        constructAllJobs();
        execute();
        return jsonResponse;
    }
    
    public JSONArray getJobUrls() {
        constructJobUrl( detail.URL );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }
    
    public JSONArray getJobNames() {
        constructJobUrl( detail.NAME );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getJobColor() {
        constructJobUrl( detail.COLOR );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getLastSuccessfullBuild() {
        constructJobUrl( detail.LAST_SUCCESSFULL_BUILD );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getLastUnsuccesfullBuild() {
        constructJobUrl( detail.LAST_UNSUCCESFULL_BUILD );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getLastBuild() {
        constructJobUrl( detail.LAST_BUILD );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getLastFailedBuild() {
        constructJobUrl( detail.LAST_FAILED_BUILD );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getLastCompletedBuild() {
        constructJobUrl( detail.LAST_COMPLETED_BUILD );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getJobDescription() {
        constructJobUrl( detail.DESCRIPTION );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getBuildName( String jobConfigUrl ) {
        constructBuildUrl( jobConfigUrl, detail.NAME );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getBuildDuration( String jobConfigUrl ) {
        constructBuildUrl( jobConfigUrl, detail.DURATION );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getBuildEstimatedDuration( String jobConfigUrl ) {
        constructBuildUrl( jobConfigUrl, detail.EST_DURATION );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getBuildTimestamp( String jobConfigUrl ) {
        constructBuildUrl( jobConfigUrl, detail.TIMESTAMP );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }

    public JSONArray getBuildDescription( String jobConfigUrl ) {
        constructBuildUrl( jobConfigUrl, detail.DESCRIPTION );
        execute();
        return jsonResponse.getJSONArray( "jobs" );
    }
    
    public String generateConfigXML( JenkinsBuildConfig config ){
        String xml = config.generateBaseConfigXML();
        
        if( xml != null ) return xml;
        
        return null;
    }
    
    public void executeJob(String jobName){
        try{
            
            this.executeUrl = this.url + "/job/" +jobName + "/build?token=" + this.jenkinsToken;
            this.build();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}