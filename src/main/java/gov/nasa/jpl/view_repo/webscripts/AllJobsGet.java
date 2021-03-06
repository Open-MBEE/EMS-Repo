package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.pma.JenkinsEngine;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
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

        int totalNumberOfJobs = 0;
        
        JenkinsEngine jenkins = new JenkinsEngine();
        
        // jenkins.getAllJobs() can also be called which would provide
        // the total number of jobs in the Jenkins server
        totalNumberOfJobs = jenkins.getTotalNumberOfJobsInQueue();
        
        final Map< String, Object > model = new HashMap<String, Object>();
        
        if (getResponseStatus().getCode() != HttpServletResponse.SC_ACCEPTED) {
            status.setCode( getResponseStatus().getCode() );
        }
        
        model.put( "res", totalNumberOfJobs );
        
        return model;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return true;
    }
}
