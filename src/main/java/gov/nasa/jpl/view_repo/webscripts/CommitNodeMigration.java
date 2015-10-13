package gov.nasa.jpl.view_repo.webscripts;


import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.*;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;


/**
 * Migrates commit nodes to be used by the diff glom
 *
 */
public class CommitNodeMigration extends AbstractJavaWebScript {

    static Logger logger = Logger.getLogger(CommitNodeMigration.class);

    private WorkspaceNode ws;
    private String workspaceId;
    private ArrayList<WorkspaceNode> wsList = new ArrayList<WorkspaceNode>();
    
    public CommitNodeMigration() {
        super();
    }
    
    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        
        if (!userHasWorkspaceLdapPermissions()) {
            return false;
        }
        
        workspaceId = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
        
        // Search for all workspaces if not supplied in URL:
        if (Utils.isNullOrEmpty( workspaceId )) {
            Collection <EmsScriptNode> nodes = NodeUtil.luceneSearchElements("ASPECT:\"ems:workspace\"" );
            for (EmsScriptNode workspaceNode: nodes) {
                WorkspaceNode wsNode = new WorkspaceNode(workspaceNode.getNodeRef(), services, response);
                wsList.add( wsNode );
            }
        }
        // Otherwise a workspaceId was supplied:
        else {
            ws = WorkspaceNode.getWorkspaceFromId( workspaceId, getServices(), response, status, //false,
                                                   null );
    
            boolean wsFound = ( ws != null || ( workspaceId != null && workspaceId.equalsIgnoreCase( "master" ) ) );
    
            if ( !wsFound ) {
                log( Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Workspace id , %s , not found",workspaceId);
                return false;
            }
        }
        
        return true;
    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        CommitNodeMigration commitNodeMigration = new CommitNodeMigration();
        return commitNodeMigration.executeImplImpl( req, status, cache, runWithoutTransactions );
    }
    
    private void migrateWorkspaceCommits(WorkspaceNode workspace, Status status) {
        
        ArrayList<EmsScriptNode> commits = CommitUtil.getCommits( workspace, services, response );
        
        if (!Utils.isNullOrEmpty( commits )) {
            
            for (EmsScriptNode commitNode : commits ) {
                if (logger.isInfoEnabled()) logger.info( "Migrating commit node: "+ commitNode);
                CommitUtil.migrateCommitNode( commitNode, response, status );
            }
        }
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

        // Migrate the commit nodes of the single workspace:
        if (!Utils.isNullOrEmpty( workspaceId )) {
            if (logger.isInfoEnabled()) logger.info( "Migrating commit nodes for workspace: "+ workspaceId);
            migrateWorkspaceCommits(ws, status);
            
            String msg = "Completed commit node migration for workspace: "+workspaceId;
            if (logger.isInfoEnabled()) logger.info(msg);
            results.put("res", msg);
        }
        // Migrate the commit nodes of all workspaces:
        else if (!Utils.isNullOrEmpty( wsList )){
            if (logger.isInfoEnabled()) logger.info("Migrating commit nodes for all workspaces.  Total number of workspaces: "+wsList.size());
            for (WorkspaceNode workspaceNode : wsList) {
                if (logger.isInfoEnabled()) logger.info( "Migrating commit nodes for workspace: "+ WorkspaceNode.getId( workspaceNode ));
                migrateWorkspaceCommits(workspaceNode, status);
            }
            
            String msg = "Completed commit node migration for all workspaces.  Total number of workspaces: "+wsList.size();
            if (logger.isInfoEnabled()) logger.info(msg);
            results.put("res", msg);
        }
        else {
            log( Level.WARN, "No workspaces found to migrate");
        }
        
        status.setCode(responseStatus.getCode());

        printFooter();

        return results;
    }
    
}
