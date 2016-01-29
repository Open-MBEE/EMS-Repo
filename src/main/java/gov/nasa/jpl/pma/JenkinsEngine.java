/**
 * JenkinsEngine --------------------------------------------- Implements the
 * ExecutionEngine as a way to execute jobs (events) on the Jenkins server.
 * 
 */
package gov.nasa.jpl.pma;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.transformer.IntegerToString;
import org.alfresco.repo.cmis.rest.CMISPropertyValueMethod.NULL;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;

public class JenkinsEngine implements ExecutionEngine {

    private JenkinsServer jenkins;
    private String username;
    private String passwordOrToken;
    private URI jenkinsURI;
    private long executionTime;

    /**
     * Default Constructor of JenkinsEngine
     */
    public JenkinsEngine() {
        // TODO Auto-generated constructor stub
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
    public void createEngine() {
        // Create a server using default values for the Jenkins URI, username
        // and Password / Token
        jenkins = new JenkinsServer( this.jenkinsURI, this.username,
                                     this.passwordOrToken );
    }

    /**
     * Creates an instance of the Jenkins Engine
     */
    public void createEngine( URI serverURI, String name, String pass ) {
        // Create a server using default values for the Jenkins URI, username
        // and Password / Token
        jenkins = new JenkinsServer( serverURI, name, pass );
    }

    @Override
    public void execute( Object event ) {}

    public String runScript( String script ) throws IOException {
        return jenkins.runScript( script );
    }

    @Override
    public void execute( List< Object > events ) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isRunning() {
        // TODO Auto-generated method stub
        return jenkins.isRunning();
    }

    @Override
    public int getExecutionStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Map< String, Job > getJenkinBuilds() throws IOException {
        return this.jenkins.getJobs( null, null );
    }

    public Map< String, Job > getEvents( String event ) throws IOException {
        return this.jenkins.getJobs( null, event );
    }

    public Map< String, Job >
           getEvents( FolderJob jobGroup ) throws IOException {
        return this.jenkins.getJobs( jobGroup, null );
    }

    public Map< String, Job > getEvents( FolderJob folder,
                                         String event ) throws IOException {
        return this.jenkins.getJobs( folder, event );
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
        List< String >      details;        // List of strings representing the details of a job.
        JobWithDetails      jobDetails;     // Jenkins Job Details Class
        JobWithDetails      singleJob;      // Will contain the details of a single job
        BuildWithDetails    singleBuild;    // Build containing the details of a job
        String              detail;         // An Individual detail from a job

        // Initialize Variables
        detail = "none";
        jobDetails = null;
        singleJob = null;

        // Checks to see if Jenkins is running before attempting to retreive the jobs
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
                case "unstable" :
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
    public void setEvent( Object event ) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEvents( List< Object > event ) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean stopExecution() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeEvent( Object event ) {
        // TODO Auto-generated method stub
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
