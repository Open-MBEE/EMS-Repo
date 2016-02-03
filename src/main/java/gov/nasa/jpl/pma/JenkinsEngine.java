/**
 * JenkinsEngine --------------------------------------------- Implements the
 * ExecutionEngine as a way to execute jobs (events) on the Jenkins server.
 * 
 */
package gov.nasa.jpl.pma;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildWithDetails;
// import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.JobWithDetails;

import gov.nasa.jpl.view_repo.webscripts.JobGet;

public class JenkinsEngine implements ExecutionEngine {
    static Logger logger = Logger.getLogger(JenkinsEngine.class);
    
    private JenkinsServer jenkins;
    private String username;
    private String passwordOrToken;
    private String url = "https://cae-jenkins.jpl.nasa.gov";
    private String jenkinsToken = "build";
    public String job = "empty";
    public String executeUrl;
    public DefaultHttpClient jenkinsClient;
    private long executionTime;
    
    //private List< QueueItem > eventQueue;

    public JenkinsEngine() {

        /**
         * Simple class to launch a jenkins build on run@Cloud platform, should
         * also work on every jenkins instance (not tested)
         *
         */

        // Credentials
        String username = this.username;
        String password = this.passwordOrToken;

        // Jenkins url
        String jenkinsUrl = this.url;
        // Build name
        String jobName = "MDKTest";

        // Build token
        String buildToken = this.jenkinsToken;

        // Create your httpclient
        jenkinsClient = new DefaultHttpClient();

        // Then provide the right credentials
        jenkinsClient.getCredentialsProvider()
                     .setCredentials( new AuthScope( AuthScope.ANY_HOST,
                                                     AuthScope.ANY_PORT ),
                                      new UsernamePasswordCredentials( username,
                                                                       password ) );

        // Generate BASIC scheme object and stick it to the execution
        // context
        BasicScheme basicAuth = new BasicScheme();
        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute( "preemptive-auth", basicAuth );

        // Add as the first (because of the zero) request interceptor
        // It will first intercept the request and preemptively
        // initialize the authentication scheme if there is not
        jenkinsClient.addRequestInterceptor( new PreemptiveAuth(), 0 );

        // You get request that will start the build
        String getUrl =
                jenkinsUrl + "/job/" + jobName + "/build?token=" + buildToken;
        System.out.println( "The Build url is " + getUrl );
        HttpGet get = new HttpGet( getUrl );

        try {
            // Execute your request with the given context
            HttpResponse response = jenkinsClient.execute( get, context );
            System.out.println( "The response is " + response.toString() );
            HttpEntity entity = response.getEntity();
            EntityUtils.consume( entity );
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Preemptive authentication interceptor
     *
     */
    static class PreemptiveAuth implements HttpRequestInterceptor {

        /*
         * (non-Javadoc)
         *
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
     * Default Constructor of JenkinsEngine
     */
    public JenkinsEngine( URI uri ) {
        // TODO Auto-generated constructor stub
        jenkins = new JenkinsServer( uri );
        System.out.println( "Creating a new jenkins server.\n" );
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
     * Creates an instance of the Jenkins Engine
     */
    @Override
    public void createEngine() {}

    /**
     * Creates an instance of the Jenkins Engine
     */
    public void createEngine( URI serverURI, String name, String pass ) {
        // Create a server using default values for the Jenkins URI, username
        // and Password / Token
        jenkins = new JenkinsServer( serverURI, name, pass );
    }

    @Override
    public void execute( Object event ) {
        // This depends on what we want to do with the event that comes in...
        // could be trigger a build, etc.
//        try {
//            ( (Job)event ).build();
//        }
//        catch (IOException e) {
//            // some exception
//        }
    }

    @Override
    public void execute( List< Object > events ) {
        // TODO Auto-generated method stub
//        try {
//            for(Object event : events) 
//                ( (Job)event ).build();
//        }
//        catch (IOException e) {
//         // some exception
//        }
    }

    @Override
    public boolean isRunning() {
        return jenkins.isRunning();
    }

    @Override
    public int getExecutionStatus() {
        if (jenkins.isRunning()) {
            
        }
        return 0;
    }

    /**
     * This method is used to find the job that the user specifies within <b>eventName</b> and specifying
     *  which detail they would like from the job.
     *  <b>detailName</b> These are the parameters it accepts: 
     *  <ul>
     *  <li>name
     *  <li>url
     *  <li>failed
     *  <li>successful
     *  <li>unsuccessful
     *  <li>stable
     *  <li>unstable
     *  </ul>
     * @param String eventName, String detailName
     * @return Event details in a string form
     * @Override
     */
    public String getEventDetail( String eventName, String detailName ) {
        // Declare Variables
        List< String > details; // List of strings representing the details of a
                                // job.
        JobWithDetails jobDetails; // Jenkins Job Details Class
        JobWithDetails singleJob; // Will contain the details of a single job
        BuildWithDetails singleBuild; // Build containing the details of a job
        String detail; // An Individual detail from a job

        // Initialize Variables
        detail = "none";
        jobDetails = null;
        singleJob = null;

        // Checks to see if Jenkins is running before attempting to retreive the
        // jobs
        if ( jenkins.isRunning() ) {
            try {
                singleJob = jenkins.getJob( eventName );
            } catch ( IOException e ) {
                e.printStackTrace();
                return null;
            }
        }

        if ( singleJob != null ) {
            try {
                jobDetails = singleJob.details();
            } catch ( IOException e ) {
                e.printStackTrace();
            }

            switch ( detailName.toLowerCase() ) {
                case "name":
                    detail = singleJob.getName();
                    break;
                case "url":
                    detail = singleJob.getUrl();
                    break;
                case "failed":
                    detail = singleJob.getLastFailedBuild().toString();
                    break;
                case "successful":
                    detail = singleJob.getLastSuccessfulBuild().toString();
                    break;
                case "unsuccessful":
                    detail = singleJob.getLastUnsuccessfulBuild().toString();
                    break;
                case "stable":
                    detail = singleJob.getLastStableBuild().toString();
                    break;
                case "unstable":
                    detail = singleJob.getLastUnstableBuild().toString();
                    break;
                default:

                    detail = detailName + " is not a proper detail parameter.";
                    break;
            }
        }
        return detail;
    }

    @Override
    public void setEvent( String event ) {
        try {
            String eventXml = jenkins.getJobXml( event );
            jenkins.createJob( event, eventXml );
        } catch (IOException e) {
            // some exception 
        }
        
        // There will be some queue of events ... should these events be QueueItem?
        //events.add(event);
        //execute( events );
        
    }

    @Override
    public void setEvents( List< String > events ) {

        for(String event: events)
            setEvent( event );
        
        // events may need to be List< QueueItem > 
        //for(Object event: events) 
        //    events.add(event);
        //execute( event );
    }

    @Override
    public boolean stopExecution() {
        /*
         * Stop all running instances of job / jobs
         */
        
        if ( jenkins.isRunning() ) {
            // stop execution only if the server is running
        }
        return false;
    }

    @Override
    public boolean removeEvent( String event ) {

//        // TODO Auto-generated method stub
//                
//        if( events.remove( event ) )
//            return true;

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
}
