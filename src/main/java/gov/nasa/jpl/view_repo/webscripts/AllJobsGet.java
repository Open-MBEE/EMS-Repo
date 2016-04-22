package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.pma.JenkinsEngine;
import gov.nasa.jpl.view_repo.util.EmsTransaction;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class AllJobsGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(AllJobsGet.class);

    public AllJobsGet() {
        super();
    }

    public AllJobsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }
    
    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        AllJobsGet instance = new AllJobsGet(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }
    
    @Override
    protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
                                                     final Status status,
                                                     final Cache cache ) {

        JenkinsEngine jenkins = new JenkinsEngine();
        
        JSONObject jenkinsRes = jenkins.getAllJobs();
        
        JSONArray jobs = jenkinsRes.optJSONArray( "jobs" );
        
        final Map< String, Object > model = new HashMap<String, Object>();

        clearCaches( true );
        clearCaches(); // calling twice for those redefine clearCaches() to
                       // always call clearCaches( false )
        
        if (getResponseStatus().getCode() != HttpServletResponse.SC_ACCEPTED) {
            status.setCode( getResponseStatus().getCode() );

        }
        
        
        model.put( "res", jobs.length() );
        
        return model;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return true;
    }
}
