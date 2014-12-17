package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;

import java.util.Date;
import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Kick off the actual workspace difference in the background
 * @author cinyoung
 *
 */
public class CommitActionExecuter extends ActionExecuterAbstractBase {
    public static final String NAME = "commit";
    public static final String PARAM_PROJECT_ID = "projectId";
    public static final String PARAM_WS_ID = "wsId";
    public static final String PARAM_WS_DIFF = "wsDiff";
    public static final String PARAM_START = "start";
    public static final String PARAM_END = "end";

    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;

    private StringBuffer response;

    static Logger logger = Logger.getLogger(CommitActionExecuter.class);

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }
    
    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        String projectId = (String) action.getParameterValue(PARAM_PROJECT_ID);
        String wsId = (String) action.getParameterValue(PARAM_WS_ID);
        WorkspaceDiff wsDiff = (WorkspaceDiff) action.getParameterValue(PARAM_WS_DIFF);
        Long start = (Long) action.getParameterValue(PARAM_START);
        Long end = (Long) action.getParameterValue(PARAM_END);
        
        try {
            JSONObject deltaJson = wsDiff.toJSONObject( new Date(start), new Date(end) );

            // FIXME: Need to split by projectId
            if ( !CommitUtil.sendDeltas(deltaJson, wsId, projectId) ) {
                logger.warn("send deltas not posted properly");
            }

            CommitUtil.updateCommitNodeRef( nodeRef, "", deltaJson.toString(), services, response );
        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            logger.error( "JSON creation error in when updating the difference" );
            e.printStackTrace();
        }
        
    }

    
    @Override
    protected void
            addParameterDefinitions( List< ParameterDefinition > paramList ) {
        // TODO Auto-generated method stub
        
    }
}