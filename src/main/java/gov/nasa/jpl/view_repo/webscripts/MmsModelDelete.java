package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
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

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> model = new HashMap<String, Object>();

        MmsModelDelete instance = new MmsModelDelete(repository, services);

        JSONObject result = null;
        try {
            result = instance.handleRequest( req );
            if (result != null) {
                model.put( "res", result.toString(2) );
            }
        } catch (JSONException e) {
           log(LogLevel.ERROR, "Could not create JSON\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
           e.printStackTrace();
        } catch (Exception e) {
           log(LogLevel.ERROR, "Internal server error\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
           e.printStackTrace();
        }
        appendResponseStatusInfo( instance );
        if (result == null) {
            model.put( "res", "");
        }

        // REVIEW -- TODO -- shouldn't responseStatus be called from instance?
        status.setCode(responseStatus.getCode());

        return model;
    }

    protected JSONObject handleRequest(WebScriptRequest req) throws JSONException {
        JSONObject result = null;

        Long start = System.currentTimeMillis();
        String user = AuthenticationUtil.getRunAsUser();
        String wsId = null;
        WorkspaceNode workspace = getWorkspace( req, true, user );
        boolean wsFound = workspace != null;
        if ( !wsFound ) {
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

        EmsScriptNode root = findScriptNodeById(elementId, workspace, null, false);

        if (root != null && root.exists()) {
            delete(root, workspace);
            EmsScriptNode pkgNode = findScriptNodeById(elementId + "_pkg", workspace, null, false);
            handleElementHierarchy( pkgNode, workspace, true );
        } else {
            log( LogLevel.ERROR, "Could not find node " + elementId + " in workspace " + wsId,
                 HttpServletResponse.SC_NOT_FOUND);
            return result;
        }
        String siteName = root.getSiteName();

        long end = System.currentTimeMillis();

        boolean showAll = false;
        result = wsDiff.toJSONObject( new Date(start), new Date(end), showAll );

        // apply aspects after JSON has been created (otherwise it won't be output)
        for (EmsScriptNode deletedNode: wsDiff.getDeletedElements().values()) {
            if (deletedNode.exists()) {
                deletedNode.createOrUpdateAspect( "ems:Deleted" );
            }
        }

        if (wsDiff.isDiff()) {
            // Send deltas to all listeners
            if ( !sendDeltas(result) ) {
                log(LogLevel.WARNING, "createOrUpdateModel deltas not posted properly");
            }
    
            CommitUtil commitUtil = new CommitUtil();
            commitUtil.commit( wsDiff, workspace, siteName, "", false, services, response );
        }

        return result;
    }

    /**
     * Deletes a node by adding the ems:Deleted aspect
     * @param node
     * @param workspace
     */
    private void delete(EmsScriptNode node, WorkspaceNode workspace) {
        if (checkPermissions(node, PermissionService.WRITE)) {
            if ( node == null || !node.exists() ) {
                log(LogLevel.ERROR, "Trying to delete a non-existent node! " + node);
                return;
            }
    
            // Add the element to the specified workspace to be deleted from there.
            if ( workspace != null && workspace.exists() && node != null
                 && node.exists() && !node.isWorkspace() ) {
                EmsScriptNode newNodeToDelete = workspace.replicateWithParentFolders( node );
                node = newNodeToDelete;
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
            for (ChildAssociationRef assoc: root.getChildAssociationRefs()) {
                EmsScriptNode child = new EmsScriptNode(assoc.getChildRef(), services, response);
                handleElementHierarchy(child, workspace, recurse);
            }
        }
        delete(root, workspace);
    }

    /**
     * Add everything to the commit delete
     * @param node
     */
    private void addToWsDiff( EmsScriptNode node ) {
        String sysmlId = node.getSysmlId();
        if (!sysmlId.endsWith( "_pkg" )) {
            wsDiff.getElementsVersions().put( sysmlId, node.getHeadVersion() );
            wsDiff.getElements().put( sysmlId, node );
            wsDiff.getDeletedElements().put( sysmlId, node );
        }
    }
}
