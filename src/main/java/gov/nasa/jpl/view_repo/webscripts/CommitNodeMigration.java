package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.WorkspaceDiffActionExecuter;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff.DiffType;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;


/**
 * Migrates commit nodes to be used by the diff glom
 *
 */
public class CommitNodeMigration extends AbstractJavaWebScript {

    private WorkspaceNode ws;
    
    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        
        if (!userHasWorkspaceLdapPermissions()) {
            return false;
        }
        
        String workspaceId = getWorkspaceId( req );
        ws = WorkspaceNode.getWorkspaceFromId( workspaceId, getServices(), response, status, //false,
                                               null );

        boolean wsFound = ( ws != null || ( workspaceId != null && workspaceId.equalsIgnoreCase( "master" ) ) );

        if ( !wsFound ) {
            log( Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Workspace id , %s , not found",workspaceId);
            return false;
        }
        return true;
    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        CommitNodeMigration commitNodeMigration = new CommitNodeMigration();
        return commitNodeMigration.executeImplImpl( req, status, cache, runWithoutTransactions );
    }
    


    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                   Status status, Cache cache ) {
        printHeader( req );

        Map<String, Object> results = new HashMap<String, Object>();

        if (!validateRequest(req, status)) {
            status.setCode(responseStatus.getCode());
            results.put("res", createResponseJson());
            return results;
        }

        ArrayList<EmsScriptNode> commits = CommitUtil.getCommits( ws, services, response );
        
        if (!Utils.isNullOrEmpty( commits )) {
            
            for (EmsScriptNode commitNode : commits ) {
                MmsDiffGet.migrateCommitNode( commitNode, response, status );
            }
        }
        
        status.setCode(responseStatus.getCode());

        printFooter();

        return results;
    }
    
}
