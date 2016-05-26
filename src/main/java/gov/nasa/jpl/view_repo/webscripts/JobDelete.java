package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.pma.JenkinsEngine;
import gov.nasa.jpl.view_repo.util.NodeUtil;


public class JobDelete extends MmsModelDelete {
    static Logger logger = Logger.getLogger(JobDelete.class);
    
    public JobDelete() {
        super();
    }

    public JobDelete(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }
    
    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        JobDelete instance = new JobDelete(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }
    
    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        if ( logger.isInfoEnabled() ) {
            String user = AuthenticationUtil.getFullyAuthenticatedUser();
            logger.info( user + " " + req.getURL() );
        }


        if ( logger.isInfoEnabled() ) {
            String user = AuthenticationUtil.getFullyAuthenticatedUser();
            logger.info( user + " " + req.getURL() );
        }
        
        Timer timer = new Timer();
        
        printHeader( req );

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
            
            JenkinsEngine jenkins = new JenkinsEngine();
            
            jenkins.deleteJob( jobId );
            
            result = handleRequest( req );
            if (result != null) {
                if (!Utils.isNullOrEmpty(response.toString())) result.put("message", response.toString());
                model.put( "res", NodeUtil.jsonToString( result, 2 ) );
            }
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

        printFooter();

        if (logger.isInfoEnabled()) logger.info( "Deletion completed" );
        if ( logger.isInfoEnabled() ) {
            logger.info( String.format( "JobDelete: %s", timer ) );
        }

        return model;
    }
    
    @Override
    public void postProcessJson( JSONObject top ) {         
        if ( jobsJsonArray != null ) {
            top.put( "jobs", jobsJsonArray );
            // flush the jobs array so that it can be repopulated for
            // returned json after sending deltas
            jobsJsonArray = new JSONArray();
        }
    }


}
