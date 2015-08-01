package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.MmsDiffGet;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

/**
 * Handles workspace diffs in the background.
 * 
 * @author gcgandhi
 *
 */
public class WorkspaceDiffActionExecuter extends ActionExecuterAbstractBase {

    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;

    private StringBuffer response;
    private Status responseStatus;
    private String jobStatus;
    
    // Parameter values to be passed in when the action is created
    public static final String NAME = "workspaceDiff";
    public static final String PARAM_TIME_1 = "time1";
    public static final String PARAM_TIME_2 = "time2";
    public static final String PARAM_WS_1 = "ws1";
    public static final String PARAM_WS_2 = "ws2";
    public static final String PARAM_TS_1 = "ts1";
    public static final String PARAM_TS_2 = "ts2";
    public static final String OLD_JOB = "oldJob";

    static Logger logger = Logger.getLogger(WorkspaceDiffActionExecuter.class);
    
    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }

    @Override
    protected void executeImpl( Action action, NodeRef actionedUponNodeRef ) {
        
        final Timer timer = new Timer();
        final Date dateTime1 = (Date) action.getParameterValue(PARAM_TIME_1);
        final Date dateTime2 = (Date) action.getParameterValue(PARAM_TIME_2);
        final WorkspaceNode ws1 = (WorkspaceNode) action.getParameterValue(PARAM_WS_1);
        final WorkspaceNode ws2 = (WorkspaceNode) action.getParameterValue(PARAM_WS_2);
        final String ts1 = (String) action.getParameterValue(PARAM_TS_1);
        final String ts2 = (String) action.getParameterValue(PARAM_TS_2);
        final EmsScriptNode oldJobNode = (EmsScriptNode) action.getParameterValue(OLD_JOB);

        final EmsScriptNode jsonNode = new EmsScriptNode(actionedUponNodeRef, services, response);

        final boolean glom = true;
        
        if (logger.isDebugEnabled()) logger.debug( "started execution of diff for " + WorkspaceNode.getWorkspaceName(ws1) + " and "+ WorkspaceNode.getWorkspaceName(ws2));
        clearCache();

        jobStatus = "Failed";
        new EmsTransaction(services, response, responseStatus) {
            @Override
            public void run() throws Exception {
                
                Status status = new Status();
                Map<String, Object> results = new HashMap<String, Object>();
                
                // Perform diff:
                MmsDiffGet diffService = new MmsDiffGet(repository, services, ws1, ws2, dateTime1, dateTime2);
                diffService.performDiff( results );
                
                status.setCode(diffService.getResponseStatus().getCode());

                if (status.getCode() == HttpServletResponse.SC_OK) {
                    jobStatus = "Succeeded";
                }
                response.append(diffService.getResponse().toString());
                if (logger.isDebugEnabled()) logger.debug( "completed diff with status [" + jobStatus + "]");
                
                // Save off diff json:
                if (results.containsKey( "res" )) {
                    ActionUtil.saveStringToFile(jsonNode, "application/json", services, (String) results.get( "res" ));
                }
                
                // Set the status
                jsonNode.setProperty("ems:job_status", jobStatus);
   
                // Send off the notification email
                String subject =
                        "Workspace diff for " + WorkspaceNode.getWorkspaceName(ws1) + " and " + WorkspaceNode.getWorkspaceName(ws2) + " completed";
                
                ActionUtil.sendEmailToModifier(jsonNode, subject, services, response.toString(), ts1, ts2, ws1, ws2);
                
                if (logger.isDebugEnabled()) logger.debug( "WorkspaceDiffActionExecuter: " + timer );
            }
        };
                
    }
    
    protected JSONObject diffJsonFromJobNode( EmsScriptNode jobNode ) {
        ContentReader reader = services.getContentService().getReader(jobNode.getNodeRef(), 
                                                                      ContentModel.PROP_CONTENT);
        
        if (reader != null) {
            try {
                JSONObject diffResults = new JSONObject(reader.getContentString());
                return diffResults;
            } catch (ContentIOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void
            addParameterDefinitions( List< ParameterDefinition > paramList ) {
        // TODO Auto-generated method stub

    }
    
    protected void clearCache() {
        response = new StringBuffer();
        responseStatus = new Status();
        NodeUtil.setBeenInsideTransaction( false );
        NodeUtil.setBeenOutsideTransaction( false );
        NodeUtil.setInsideTransactionNow( false );
    }
}