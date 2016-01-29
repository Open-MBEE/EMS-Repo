/**
 * JenkinsEngine
 * ---------------------------------------------
 * Implements the ExecutionEngine as a way to execute jobs (events) on the Jenkins server.
 *  
 */
package gov.nasa.jpl.pma;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;

public class JenkinsEngine implements ExecutionEngine {

    private Queue<Object> jobQueue;
    private JenkinsServer jenkins;
    private String serverUrl = "http://localhost:8080";
    private String username;
    private String passwordOrToken;
    private URI jenkinsURI;
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
    public void setUsername(String name) {
        this.username = name;
    }

    /**
     * Sets the password that is associated with the username that will be
     * connected with Jenkins
     * 
     * @param pass
     */
    public void setPassword(String pass) {
        this.passwordOrToken = pass;
    }

    /**
     * Creates an instance of the Jenkins Engine
     */
    @Override
    public void createEngine() {
        // Create a server using default values for the Jenkins URI, username
        // and Password / Token
        jenkins = new JenkinsServer(this.jenkinsURI, this.username,
                this.passwordOrToken);
    }

    /**
     * Creates an instance of the Jenkins Engine
     */
    public void createEngine(URI serverURI, String name, String pass) {
        // Create a server using default values for the Jenkins URI, username
        // and Password / Token
        jenkins = new JenkinsServer(serverURI, name, pass);
    }

    /**
     * Executes the current job that is in the job queue
     */
    @Override
    public void execute(){ }

    @Override
    public void execute(Object event) { }

    public String runScript(String script) throws IOException {
        return jenkins.runScript(script);
    }

    @Override
    public void execute(List<Object> events) {
        // TODO Auto-generated method stub

    }

    @Override
    public void executeQueue() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isRunning() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getExecutionStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object getEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getEvents() { return null; }
    
    public Map<String, Job> getJobs() throws IOException{
        return this.jenkins.getJobs();
    }

    @Override
    public String getExecutionQueue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getEventDetail(String detail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getEventDetails() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setEvent(Object event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEvents(List<Object> event) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean stopExecution() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeEvent(Object event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void updateEvent(String event) {
        // TODO Auto-generated method stub
        
    }

}
