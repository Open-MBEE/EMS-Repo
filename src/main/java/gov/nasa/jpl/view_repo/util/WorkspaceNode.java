/**
 * 
 */
package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;

import java.util.Date;
import java.util.Map;
import java.util.Set;
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
    }

    /**
     * @param nodeRef
     * @param services
     * @param response
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services,
                          StringBuffer response ) {
        super( nodeRef, services, response );
    }

    /**
     * @param nodeRef
     * @param services
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services ) {
        super( nodeRef, services );
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

    /**
     * Create a workspace folder within the specified folder or (if the folder
     * is null) within the specified user's home folder.
     * 
     * @param sysmlId
     *            the name/identifier of the workspace
     * @param userName
     *            the name of the user that is creating the workspace
     * @param folder
     *            the folder within which to create the workspace
     * @param services
     * @param response
     * @param status
     * @return the new workspace or null if the workspace could not be created
     *         because both the containing folder and the user name were both
     *         unspecified (non-existent)
     */
    public static WorkspaceNode createWorskpaceInFolder( String sysmlId,
                                                         //String wsName,
                                                         String userName,
                                                         EmsScriptNode folder,
                                                         ServiceRegistry services,
                                                         StringBuffer response,
                                                         Status status ) {
        if ( sysmlId == null ) {
            sysmlId = NodeUtil.createId( services );
        }
        if ( folder == null || !folder.exists() ) {
            //String userName = ws.getOwner();
            if ( userName != null && userName.length() > 0 ) {
                folder = NodeUtil.getUserHomeFolder( userName );
                if ( Debug.isOn() ) Debug.outln( "user home folder: " + folder );
            }
        }
        if ( folder == null || !folder.exists() ) {
            Debug.error( true, false, "\n%%% Error! no folder, " + folder
                                      + ", within which to create workspace, "
                                      + sysmlId );
        }

        WorkspaceNode ws = new WorkspaceNode( folder.createFolder( sysmlId ).getNodeRef(),
                                              services, response, status );
        ws.addAspect( "ems:Workspace" );
        
        ws.setProperty( "ems:parent", folder );
        if ( folder.isWorkspace() ) {
            if ( Debug.isOn() ) Debug.outln( "folder is a workspace: " + folder );
            WorkspaceNode parentWorkspace =
                    new WorkspaceNode( folder.getNodeRef(), services, response,
                                       status );
            if ( Debug.isOn() ) Debug.outln( "parent workspace: " + parentWorkspace );
            parentWorkspace.appendToPropertyNodeRefs( "ems:children", ws.getNodeRef() );
        }
        ws.setProperty( "ems:lastTimeSyncParent", new Date() );
        if ( Debug.isOn() ) Debug.outln( "created workspace " + ws + " in folder " + folder );
        return ws;
    }

    // A workspace is not created inside the folder of another workspace, so
    // this method is commented out.
//    public WorkspaceNode createWorskpace( String sysmlId ) {
//        return createWorskpaceInFolder( sysmlId, this.getOwner(), this,
//                                        getServices(), getResponse(),
//                                        getStatus() );
//    }
    
    /**
     * Determine whether the given node is correct for this workspace, meaning
     * that it is either modified in this workspace or is contained by the
     * parent workspace and unmodified in this workspace.
     * 
     * @param node
     * @return true iff the node is in this workspace
     */
    public boolean contains( EmsScriptNode node ) {
        WorkspaceNode nodeWs = node.getWorkspace();
        if ( this.equals( nodeWs ) ) return true;
        WorkspaceNode parentWs = getParentWorkspace();
        if ( parentWs == null ) return ( nodeWs == null );
        return parentWs.contains( node );
    }
    
    // Replicate this folder and its parent/grandparents in this workspace.
    public EmsScriptNode replicateFolderWithChain( EmsScriptNode folder ) {
        if ( Debug.isOn() ) Debug.outln( "replicateFolderWithChain( " + folder + " )" );
        if ( folder == null ) return null;
        EmsScriptNode newFolder = folder;

        // make sure the folder's parent is replicated
        EmsScriptNode parent = folder.getParent();

        if ( parent == null || parent.isWorkspaceTop() ) {
            parent = this;
//            if ( Debug.isOn() ) Debug.outln( "returning newFolder for workspace top: " + newFolder );
//            return newFolder;
        }
        if ( parent != null && parent.exists() && !this.equals( parent.getWorkspace() ) ) {
            parent = replicateFolderWithChain( parent );
            //if ( Debug.isOn() ) Debug.outln( "moving newFolder " + newFolder + " to parent " + parent );
            //newFolder.move( parent );
        } else if ( parent == null || !parent.exists() ) {
            Debug.error("Error! Bad parent when replicating folder chain! " + parent );
        }

        // If the folder is not already in this workspace, clone it.
        if ( folder.getWorkspace() == null || !folder.getWorkspace().equals( this ) ) {
            newFolder = folder.clone(parent);
            newFolder.setWorkspace( this, folder.getNodeRef() );
        }
        
        if ( Debug.isOn() ) Debug.outln( "returning newFolder: " + newFolder );
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
        
        // find deepest common parent
        
        // Collect the sysmlids of elements changed in this and the other
        // workspace as well as those of their parents up to (but not including
        // the deepest common parent). These should also include deleted
        // elements.
        
        // For each sysmlid, get the corresponding element from each of the two
        // workspaces. Map the one from this workspace to that from the other
        // workspace.
        
        // NOTE: This doesn't specify the changes and conflicts explicitly.  
        
        //Set<EmsScriptNode> myElements = getLocalChanges();
        
        return map;
    }
    
}
