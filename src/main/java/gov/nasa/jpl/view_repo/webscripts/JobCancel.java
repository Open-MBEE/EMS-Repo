package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.pma.JenkinsEngine;

public class JobCancel extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(JobCancel.class);
    
    public JobCancel() {
        super();
    }

    public JobCancel(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }
    
    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        JobCancel instance = new JobCancel(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }
    
    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Timer timer = new Timer();
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);

        Map<String, Object> model = new HashMap<String, Object>();

        JSONObject result = null;

        try {
            String[] idKeys =
                { "jobid", "jobId" };
            
            String jobId = null;
            for ( String idKey : idKeys ) {
                jobId = req.getServiceMatch().getTemplateVars().get( idKey );
                if ( jobId != null ) {
                    break;
                }
            }
            
            String cancelId = null;
            
            // Using multiple Jenkins instances to work around the Entity Consume issue
            // NOTE: The Entity Consume issue is that we cannot issue multiple forms
            //       of HTTP requests for one instance of a JenkinsEngine class
            //       (i.e. if you post, you cannot subsequently make another post
            //             or a get, put, delete, etc... )
            JenkinsEngine jenkins = new JenkinsEngine();           
            JenkinsEngine grabBuildNumber = new JenkinsEngine();
            JenkinsEngine getQueueId = new JenkinsEngine();
            
            boolean isInQueue = jenkins.isJobInQueue( jobId );
            
            if( isInQueue ) {
                cancelId = getQueueId.getQueueId( jobId );    
            }
            else {               
                cancelId = grabBuildNumber.getBuildNumber( jobId );                
            }
                        
            // create a URL that will stop a job in it's current running state
            jenkins.cancelJob( jobId, cancelId, isInQueue );

        } catch (JSONException e) {
           log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON\n");
           e.printStackTrace();
        } catch (Exception e) {

           if (e.getCause() instanceof JSONException) {
               log(Level.WARN, HttpServletResponse.SC_BAD_REQUEST,"Bad JSON body provided\n"); 
           } else {
               log(Level.ERROR,  HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error\n");
           }
           e.printStackTrace();
        }
        if (result == null) {
            model.put( "res", createResponseJson());
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

}
