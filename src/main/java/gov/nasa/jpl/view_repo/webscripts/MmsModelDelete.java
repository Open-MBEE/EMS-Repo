package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsModelDelete extends AbstractJavaWebScript {
    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    public MmsModelDelete() {
        super();
    }

    public MmsModelDelete(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        
        MmsModelDelete instance = new MmsModelDelete(repository, services);
        instance.setServices( getServices() );
        return instance.executeImplImpl(req,  status, cache);
    }
    
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        printHeader( req );
        
        Map<String, Object> model = new HashMap<String, Object>();

        JSONObject result = null;

        try {
            result = handleRequest( req );
            if (result != null) {
                if (!Utils.isNullOrEmpty(response.toString())) result.put("message", response.toString()); 
                model.put( "res", result.toString(2) );
            }
        } catch (JSONException e) {
           log(LogLevel.ERROR, "Could not create JSON\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
           e.printStackTrace();
        } catch (Exception e) {
           log(LogLevel.ERROR, "Internal server error\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
           e.printStackTrace();
        }
        if (result == null) {
            model.put( "res", response.toString());
        }

        status.setCode(responseStatus.getCode());

        printFooter();
        
        return model;
    }

    protected JSONObject handleRequest(WebScriptRequest req) throws JSONException {
        JSONObject result = null;

        Long start = System.currentTimeMillis();
        String user = AuthenticationUtil.getRunAsUser();
        String wsId = null;
        WorkspaceNode workspace = getWorkspace( req, //true, // not creating ws!
                                                user );
        boolean wsFound = workspace != null;
        if ( wsFound ) {
            wsId = workspace.getSysmlId();
        } else {
            wsId = getWorkspaceId( req );
            if ( wsId != null && wsId.equalsIgnoreCase( "master" ) ) {
                wsFound = true;
            }
        }
        if ( !wsFound ) {
            log( LogLevel.ERROR,
                 "Could not find or create " + wsId + " workspace.\n",
                 Utils.isNullOrEmpty( wsId ) ? HttpServletResponse.SC_BAD_REQUEST
                                             : HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            return result;
        }
        setWsDiff(workspace);   // need to initialize the workspace diff

        String elementId = req.getServiceMatch().getTemplateVars().get("elementId");

        // Searching for deleted nodes also, in case they try to delete a element that has
        // already been deleted in the current workspace.
        EmsScriptNode root = findScriptNodeById(elementId, workspace, null, true);
        String siteName = null;
        String projectId = null;

        UserTransaction trx;
        trx = services.getTransactionService().getNonPropagatingUserTransaction();
        try {
            trx.begin();

            if (root != null && root.exists()) {
                handleElementHierarchy( root, workspace, true );
            } else {
                log( LogLevel.ERROR, "Could not find node " + elementId + " in workspace " + wsId + ",it is either deleted or not present.",
                     HttpServletResponse.SC_NOT_FOUND);
                return result;
            }
            siteName = root.getSiteName();

            long end = System.currentTimeMillis();

            boolean showAll = false;
            result = wsDiff.toJSONObject( new Date(start), new Date(end), showAll );

            // apply aspects after JSON has been created (otherwise it won't be output)
            for (EmsScriptNode deletedNode: wsDiff.getDeletedElements().values()) {
                if (deletedNode.exists()) {
                    deletedNode.removeAspect( "ems:Added" );
                    deletedNode.removeAspect( "ems:Updated" );
                    deletedNode.removeAspect( "ems:Moved" );
                    deletedNode.createOrUpdateAspect( "ems:Deleted" );
                    projectId = deletedNode.getProjectId();
                }
            }

            trx.commit();
        } catch (Throwable e) {
            try {
                if (e instanceof JSONException) {
                        log(LogLevel.ERROR, "MmsModelDelete.handleRequest: JSON malformed: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
                } else {
                        log(LogLevel.ERROR, "MmsModelDelete.handleRequest: DB transaction failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                trx.rollback();
                log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                e.printStackTrace();
            } catch (Throwable ee) {
                log(LogLevel.ERROR, "\tMmsModelDelete.handleRequest: rollback failed: " + ee.getMessage());
                ee.printStackTrace();
                e.printStackTrace();
            }
        }

        if (wsDiff.isDiff()) {
            // Send deltas to all listeners
            if ( !CommitUtil.sendDeltas(result, wsId, projectId) ) {
                log(LogLevel.WARNING, "createOrUpdateModel deltas not posted properly");
            }

            CommitUtil.commit( result, workspace, siteName, "", false, services, response );
        }

        return result;
    }

    /**
     * Deletes a node by adding the ems:Deleted aspect
     * @param node
     * @param workspace
     */
    public void delete(EmsScriptNode node, WorkspaceNode workspace, WorkspaceDiff workspaceDiff) {
        if(workspaceDiff != null && wsDiff == null)
            wsDiff = workspaceDiff;
        if (!checkPermissions(node, PermissionService.WRITE)) {
            log(LogLevel.ERROR, "no permissions", HttpServletResponse.SC_FORBIDDEN);
        } else {
            if ( node == null || !node.exists() ) {
                log(LogLevel.ERROR, "Trying to delete a non-existent node! " + node);
                return;
            }

            // Add the element to the specified workspace to be deleted from there.
            if ( workspace != null && workspace.exists() && node != null
                 && node.exists() && !node.isWorkspace() ) {
                EmsScriptNode newNodeToDelete = null;
                if ( !workspace.equals( node.getWorkspace() ) ) {
                    try {
                        newNodeToDelete = workspace.replicateWithParentFolders( node );
                        node = newNodeToDelete;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (newNodeToDelete != null) {
                            node = newNodeToDelete;
                        }
                    }
                }
            }

            if ( node != null && node.exists() ) {
                addToWsDiff( node );

                deleteRelationships(node, "sysml:relationshipsAsSource", "sysml:relAsSource");
                deleteRelationships(node, "sysml:relationshipsAsTarget", "sysml:relAsTarget");
            }
        }
    }

    /**
     * Deletes the relationships that are attached to this node
     * @param node          Node to delete relationships for
     * @param aspectName    String of the aspect name to look for
     * @param propertyName  String of the property to remove
     */
    private void deleteRelationships(EmsScriptNode node, String aspectName, String propertyName) {
        if (node.hasAspect( aspectName )) {
            ArrayList<NodeRef> relRefs = node.getPropertyNodeRefs( propertyName );
            for (NodeRef relRef: relRefs) {
                EmsScriptNode relNode = new EmsScriptNode(relRef, services, response);
                addToWsDiff( relNode );
            }
        }
    }

    /**
     * Build up the element hierarchy from the specified root
     * @param root      Root node to get children for
     * @param workspace
     * @throws JSONException
     */
    protected void handleElementHierarchy( EmsScriptNode root,
                                           WorkspaceNode workspace,
                                           boolean recurse ) {
        if (root == null) {
            return;
        }

        if (recurse) {
            for ( NodeRef childRef : root.getOwnedChildren(true) ) {
                EmsScriptNode child = new EmsScriptNode(childRef, services, response);
                handleElementHierarchy(child, workspace, recurse);
            }
        }
        
        // Delete the node:
        if (root.exists()) {
            delete(root, workspace, null);
        }
        
        // TODO: REVIEW may not need this b/c addToWsDiff() does not add in reified packages
        //       Also, code in ModelPost assumes we never delete reified packages
//        // Delete the reified pkg if it exists also:
//        EmsScriptNode pkgNode = findScriptNodeById(root.getSysmlId() + "_pkg", 
//                                                   workspace, null, false);
//        
//        if (pkgNode != null) {
//            delete(pkgNode, workspace, null);
//        }
    }

    /**
     * Add everything to the commit delete
     * @param node
     */
    private void addToWsDiff( EmsScriptNode node ) {
        String sysmlId = node.getSysmlId();
        if (!sysmlId.endsWith( "_pkg" )) {
            if(wsDiff.getElementsVersions() != null)
                wsDiff.getElementsVersions().put( sysmlId, node.getHeadVersion() );
            if(wsDiff.getElements() != null)
                wsDiff.getElements().put( sysmlId, node );
            if(wsDiff.getDeletedElements() != null)
                wsDiff.getDeletedElements().put( sysmlId, node );
        }
    }
}
