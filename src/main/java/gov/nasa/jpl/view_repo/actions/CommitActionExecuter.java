package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;

import java.util.Date;
import java.util.List;

import javax.transaction.UserTransaction;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.JsonObject;


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
    public static final String TRANSACTION = "transaction";

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
        Boolean doTransaction = (Boolean)action.getParameterValue(TRANSACTION);

        try {
            if ( !doTransaction ) {
                Exception e = new Exception();
                logger.error( "BAD!!!!", e );
                JsonObject deltaJson =
                        wsDiff.toJsonObject( new Date(start), new Date(end) );

                // FIXME: Need to split by projectId
                if ( !CommitUtil.sendDeltas(deltaJson, wsId, projectId) ) {
                    logger.warn("send deltas not posted properly");
                }

                CommitUtil.updateCommitNodeRef( nodeRef, deltaJson.toString(),
                                                "", services, response );
            } else {
                UserTransaction trx;
                trx = services.getTransactionService().getNonPropagatingUserTransaction();
                try {
                    trx.begin();
                    NodeUtil.setInsideTransactionNow( true );
                    JsonObject deltaJson = wsDiff.toJsonObject( new Date(start), new Date(end) );

                    // FIXME: Need to split by projectId
                    if ( !CommitUtil.sendDeltas(deltaJson, wsId, projectId) ) {
                        logger.warn("send deltas not posted properly");
                    }

                    CommitUtil.updateCommitNodeRef( nodeRef,
                                                    deltaJson.toString(), "",
                                                    services, response );
                    trx.commit();
                    NodeUtil.setInsideTransactionNow( false );
                } catch (Throwable e) {
                    try {
                        trx.rollback();
                        NodeUtil.setInsideTransactionNow( false );
                        logger.error( "\t####### ERROR: Needed to rollback: "
                                      + e.getMessage() );
                        logger.error("\t####### when getProjectNodeFromRequest()");
                        e.printStackTrace();
                    } catch (Throwable ee) {
                        logger.error("\tRollback failed: " + ee.getMessage());
                        logger.error("\tafter calling getProjectNodeFromRequest()");
                        ee.printStackTrace();
                    }
                }
            }
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