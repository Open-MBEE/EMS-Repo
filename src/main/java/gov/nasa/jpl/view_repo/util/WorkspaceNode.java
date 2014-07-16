/**
 * 
 */
package gov.nasa.jpl.view_repo.util;

import java.util.Date;

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
        if ( folder.hasAspect( "ems:Workspace" ) ) {
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
        return node.getWorkspace().equals( this );
    }
    
    // Replicate this folder and its parent/grandparents in this workspace.
    public EmsScriptNode replicateFolderWithChain( EmsScriptNode folder ) {
        if ( folder == null ) return null;
        EmsScriptNode newFolder = folder;

        // If the folder is not already in this workspace, clone it.
        if ( !contains( folder ) ) {
            newFolder = folder.clone();
            newFolder.setWorkspace( this );
        }
        
        if ( folder.isWorkspaceTop() ) return newFolder;
        
        // make sure the folder's parent is replicated
        EmsScriptNode parent = newFolder.getParent();
        if (parent != null && parent.exists() && !contains( parent ) ) {
            parent = replicateFolderWithChain( parent );
            newFolder.move( parent );
        } // REVIEW -- what if parent != null && !parent.exists()
        return folder;
    }
    
    // When creating a node, create it in the workspace with the owner (and
    // parent chain up to company home) replicated in the workspace.
    
    // When updating a node, if it is not in the specified workspace, copy it
    // with the same name into a folder chain replicated in the workspace.
    
    // When copying a node, check and see if the other end of each relationship
    // is in the new workspace, and copy the relationship if it is.
    
}
