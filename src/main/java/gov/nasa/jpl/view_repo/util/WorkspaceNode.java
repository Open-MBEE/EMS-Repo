/**
 * 
 */
package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Status;

/**
 * WorkspaceNode is an EmsScriptNode and a folder containing changes to a parent
 * workspace.
 * 
 */
public class WorkspaceNode extends EmsScriptNode {

    private static final long serialVersionUID = -7143644366531706115L;

    /**
     * @param nodeRef
     * @param services
     * @param response
     * @param status
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services,
                          StringBuffer response, Status status ) {
        super( nodeRef, services, response, status );
        addAspect( "ems:Workspace" );
    }

    /**
     * @param nodeRef
     * @param services
     * @param response
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services,
                          StringBuffer response ) {
        super( nodeRef, services, response );
        addAspect( "ems:Workspace" );
    }

    /**
     * @param nodeRef
     * @param services
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services ) {
        super( nodeRef, services );
        addAspect( "ems:Workspace" );
    }
    
    @Override
    public WorkspaceNode getWorkspace() {
        return this; // This allow
    }
    
    @Override
    public WorkspaceNode getParentWorkspace() {
        NodeRef ref = (NodeRef)getProperty("sysml:parent");
        if ( ref == null ) return null;
        WorkspaceNode parentWs = new WorkspaceNode( ref, getServices() );
        return parentWs;
    }
    
    @Override
    public void setWorkspace( WorkspaceNode workspace, NodeRef source ) {
        String msg = "Cannot set the workspace of a workspace!";
        if ( getResponse() != null ) {
            getResponse().append( msg + "\n" );
            if ( getStatus() != null ) {
                getStatus().setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                     msg );
            }
        }
        Debug.error( msg );
    }

    public static WorkspaceNode createWorskpaceInFolder( String sysmlId,
                                                         //String wsName,
                                                         EmsScriptNode folder,
                                                         ServiceRegistry services,
                                                         StringBuffer response,
                                                         Status status ) {
        if ( sysmlId == null ) {
            sysmlId = NodeUtil.createId( services );
        }
        WorkspaceNode ws = new WorkspaceNode( folder.createFolder( sysmlId ).getNodeRef(),
                                              services, response, status );
        if ( folder == null || !folder.exists() ) {
            String userName = ws.getOwner();
            folder = NodeUtil.getUserHomeFolder( userName );
        }
        ws.setProperty( "ems:parent", folder );
        if ( folder.isWorkspace() ) {
            WorkspaceNode parentWorkspace =
                    new WorkspaceNode( folder.getNodeRef(), services, response,
                                       status );
            parentWorkspace.appendToPropertyNodeRefs( "ems:children", ws.getNodeRef() );
        }
        ws.setProperty( "ems:lastTimeSyncParent", new Date() );
        return ws;
    }
    
    public WorkspaceNode createWorskpace( String sysmlId ) {
        return createWorskpaceInFolder( sysmlId, this, getServices(),
                                        getResponse(), getStatus() );
    }
    
    public boolean contains( EmsScriptNode node ) {
        WorkspaceNode nodeWs = node.getWorkspace();
        if ( nodeWs == null || nodeWs.equals( this ) ) return true;
        WorkspaceNode parentWs = getParentWorkspace();
        if ( parentWs == null ) return false;
        return parentWs.contains( node );
    }
    
    // Replicate this folder and its parent/grandparents in this workspace.
    public EmsScriptNode replicateFolderWithChain( EmsScriptNode folder ) {
        if ( folder == null ) return null;
        EmsScriptNode newFolder = folder;

        // If the folder is not already in this workspace, clone it.
        if ( !contains( folder ) ) {
            newFolder = folder.clone();
            newFolder.setWorkspace( this, folder.getNodeRef() );
        }
        
        if ( folder.isWorkspaceTop() ) return newFolder;
        
        // make sure the folder's parent is replicated
        EmsScriptNode parent = newFolder.getParent();
        if (parent != null && parent.exists() && !contains( parent ) ) {
            parent = replicateFolderWithChain( parent );
            newFolder.move( parent );
        } // REVIEW -- what if parent != null && !parent.exists()
        return newFolder;
    }
    
    // When creating a node, create it in the workspace with the owner (and
    // parent chain up to company home) replicated in the workspace.
    
    // When updating a node, if it is not in the specified workspace, copy it
    // with the same name into a folder chain replicated in the workspace.
    
    // When copying a node, check and see if the other end of each relationship
    // is in the new workspace, and copy the relationship if it is.
    
    /**
     * Find the differences between this workspace and another.
     * 
     * @param other
     *            the workspace to compare
     * @return a map of elements in this workspace to changed elements in the
     *         other workspace
     */
    public Map<EmsScriptNode, EmsScriptNode> diff( WorkspaceNode other ) {
        TreeMap<EmsScriptNode, EmsScriptNode> map = new TreeMap<EmsScriptNode, EmsScriptNode>();
        // TODO
        
        return map;
    }
    
}
