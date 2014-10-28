/**
 *
 */
package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Seen;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;


/**
 * WorkspaceNode is an EmsScriptNode and a folder containing changes to a parent
 * workspace.
 *
 */
public class WorkspaceNode extends EmsScriptNode {

    private static final long serialVersionUID = -7143644366531706115L;
    private static final boolean checkingForEmsSource = true;  // FIXME -- at some point, this should turned off and removed along with the code that uses it.

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
        log( "Warning! calling getWorkspace on a workspace! " + getName() );
        return this;
    }

    @Override
    public WorkspaceNode getParentWorkspace() {
        NodeRef ref = (NodeRef)getProperty("ems:parent");
        if ( ref == null ) {
            // Handle data corrupted by a bug (now fixed)
            if ( checkingForEmsSource ) {
                try {
                    ref = (NodeRef)getProperty("ems:source");
                    if ( ref != null ) {
                        // clean up
                        setProperty( "ems:parent", ref );
                        removeProperty( "ems:source" );
                    }
                } catch ( Throwable e ) {}
            }
            return null;
        }
        WorkspaceNode parentWs = new WorkspaceNode( ref, getServices() );
        return parentWs;
    }
    // delete later
    @Override
    public WorkspaceNode getSourceWorkspace() {
        NodeRef ref = (NodeRef)getProperty("ems:source");
        if ( ref == null ) return null;
        WorkspaceNode sourceWs = new WorkspaceNode( ref, getServices() );
        return sourceWs;
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

//    /**
//     * Create a workspace folder within the specified folder or (if the folder
//     * is null) within the specified user's home folder.
//     *
//     * @param wsName
//     *            the short name of the workspace
//     * @param userName
//     *            the name of the user that is creating the workspace
//     * @param folder
//     *            the folder within which to create the workspace
//     * @param services
//     * @param response
//     * @param status
//     * @return the new workspace or null if the workspace could not be created
//     *         because both the containing folder and the user name were both
//     *         unspecified (non-existent)
//     */
//    public static WorkspaceNode createWorskpaceInFolder( String wsName,
//                                                         EmsScriptNode sourceWs,
//                                                         String userName,
//                                                         EmsScriptNode folder,
//                                                         ServiceRegistry services,
//                                                         StringBuffer response,
//                                                         Status status ) {
//        if ( wsName == null ) {
//            wsName = NodeUtil.createId( services );
//        }
//        if ( folder == null || !folder.exists() ) {
//            //String userName = ws.getOwner();
//            if ( userName != null && userName.length() > 0 ) {
//                folder = NodeUtil.getUserHomeFolder( userName, true );
//                if ( Debug.isOn() ) Debug.outln( "user home folder: " + folder );
//            }
//        }
//        if ( folder == null || !folder.exists() ) {
//            Debug.error( true, false, "\n%%% Error! no folder, " + folder
//                                      + ", within which to create workspace, "
//                                      + wsName );
//        }
//
//        String cmName = null;
//        
//        WorkspaceNode ws = new WorkspaceNode( folder.createFolder( wsName ).getNodeRef(),
//                                              services, response, status );
//        ws.addAspect( "ems:Workspace" );
//
//        ws.setProperty( "ems:parent", folder );
//        if ( folder.isWorkspace() ) {
//            if ( Debug.isOn() ) Debug.outln( "folder is a workspace: " + folder );
//            WorkspaceNode parentWorkspace =
//                    new WorkspaceNode( folder.getNodeRef(), services, response,
//                                       status );
//            if ( Debug.isOn() ) Debug.outln( "parent workspace: " + parentWorkspace );
//            parentWorkspace.appendToPropertyNodeRefs( "ems:children", ws.getNodeRef() );
//        }
//        ws.setProperty( "ems:lastTimeSyncParent", new Date() );
//        if ( Debug.isOn() ) Debug.outln( "created workspace " + ws + " in folder " + folder );
//        return ws;
//    }

    /**
     * Create a workspace folder within the specified folder or (if the folder
     * is null) within the specified user's home folder.
     *
     * @param wsName
     *            the short name of the workspace
     * @param userName
     *            the name of the user that is creating the workspace
     * @param sourceNameOrId
     *            the name or id of the workspace that will be a parent to the new one
     * @param folder
     *            the folder within which to create the workspace
     * @param services
     * @param response
     * @param status
     * @return the new workspace or null if the workspace could not be created
     *         because both the containing folder and the user name were both
     *         unspecified (non-existent)
     */
    public static WorkspaceNode createWorkspaceFromSource( String wsName,
                                                           String userName,
                                                           String sourceNameOrId,
                                                           EmsScriptNode folder,
                                                           ServiceRegistry services,
                                                           StringBuffer response,
                                                           Status status ) {
    	if ( Utils.isNullOrEmpty( wsName ) ) {
    		wsName = NodeUtil.createId( services );
    	}
    	if ( folder == null || !folder.exists() ) {
    		//String userName = ws.getOwner();
    		if ( userName != null && userName.length() > 0 ) {
    			folder = NodeUtil.getUserHomeFolder( userName, true );
    			if ( Debug.isOn() ) Debug.outln( "user home folder: " + folder );
    		}
    	}
    	if ( folder == null || !folder.exists() ) {
    		Debug.error( true, false, "\n%%% Error! no folder, " + folder
    				+ ", within which to create workspace, "
    				+ wsName );
    	}
    	
        WorkspaceNode parentWorkspace = 
                WorkspaceNode.getWorkspaceFromId( sourceNameOrId, services,
                                                  response, status, //false
                                                  userName );
    	String cmName = wsName + '_' + getName( parentWorkspace );
    	
    	// Make sure the workspace does not already exist in the target folder 
    	EmsScriptNode child = folder.childByNamePath( "/" + cmName, true, null );
    	if ( child != null && child.exists() ) {
            String msg = "ERROR! Trying to create an workspace in the same folder with the same name, " + cmName + "!\n";
            response.append( msg );
            if ( status != null ) {
                status.setCode( HttpServletResponse.SC_BAD_REQUEST, msg );
            }
            return null;
    	}
    	
    	// Make sure the workspace does not already exist otherwise
        NodeRef ref = NodeUtil.findNodeRefById( cmName, true, null, null, services, false );
        // FIXME -- This does not find workspaces that are not visible to the user!
        if ( ref != null ) {
            String msg = "ERROR! Trying to create an existing workspace, " + cmName + "!\n";
            response.append( msg );
            if ( status != null ) {
                status.setCode( HttpServletResponse.SC_BAD_REQUEST, msg );
            }
            return null;
        }
    	
    	WorkspaceNode ws = new WorkspaceNode( folder.createFolder( cmName ).getNodeRef(),
    	                                      services, response, status );
        ws.addAspect( "ems:HasWorkspace" );
        ws.setProperty("ems:workspace", ws.getNodeRef() );

    	ws.addAspect( "ems:Workspace" );
        ws.setProperty("ems:workspace_name", wsName );
    	ws.createOrUpdateProperty( "ems:lastTimeSyncParent", new Date() );
    	if ( Debug.isOn() ) Debug.outln( "parent workspace: " + parentWorkspace );
    	if(parentWorkspace != null) {
    		parentWorkspace.appendToPropertyNodeRefs( "ems:children", ws.getNodeRef() );
    		ws.setProperty( "ems:parent", parentWorkspace.getNodeRef() );
    	}
    	if ( Debug.isOn() ) Debug.outln( "created workspace " + ws + " in folder " + folder );
    	return ws;
    }

    public void delete( boolean deleteChildWorkspaces ) {
        if ( !checkPermissions( PermissionService.WRITE, getResponse(), getStatus() ) ) {
            log( "no write permissions to delete workpsace " + getName() );
            return;
        }

        // Add the delete aspect to mark as "deleted"
        addAspect( "ems:Deleted" );

        // FIXME -- REVIEW -- Is that enough?! What about the contents? Don't we
        // need to purge? Or is a "deleted" workspaceNode enough?
        
        // Update parent/child workspace references
        
        // Remove this workspace from parent's children
        WorkspaceNode source = getParentWorkspace();
        if ( Debug.isOn() ) Debug.outln( "deleted workspace " + this + " from source " + getName(source) );
        if ( source == null || !source.exists() ) {
            // TODO -- do we keep the master's children anywhere?
            if ( !source.exists() ) {
                log( "no write permissions to remove reference to child workpsace, " + getName() + ", from parent, " + getName(source) );
            }
        } else {
            if ( !source.checkPermissions( PermissionService.WRITE, getResponse(), getStatus() ) ) {
                String msg = "Warning! No write permissions to delete workpsace " + getName() + ".\n";
                getResponse().append( msg );
                log( msg );
//                if ( getStatus() != null ) {
//                    getStatus().setCode( HttpServletResponse.SC_, msg );
//                }
            } else {
                source.removeFromPropertyNodeRefs( "ems:children", getNodeRef() );
            }
        }
        
        // Not bothering to remove this workspace's ems:parent or ems:children

        // Delete children if requested
        if ( deleteChildWorkspaces ) {
            deleteChildWorkspaces( true );
        }
    }

    public void deleteChildWorkspaces( boolean recursive ) {
        // getting a copy in case it's the same list from which the children will remove themselves
        ArrayList< NodeRef > children = new ArrayList<NodeRef>(getPropertyNodeRefs( "ems:children" ));
        for ( NodeRef ref : children ) {
            WorkspaceNode childWs = new WorkspaceNode( ref, getServices(),
                                                       getResponse(),
                                                       getStatus() );
            if ( !NodeUtil.exists( childWs ) ) {
                log( "trying to delete non-existent child workspace " + 
                     ( childWs == null ? "" : "," + childWs.getName() + ", " ) +
                     " from parent, " + getName() );
            } else {
                childWs.delete( recursive );
            }
        }
    }
    
    /**
     * Determine whether the given node is correct for this workspace, meaning
     * that it is either modified in this workspace or is contained by the
     * parent workspace and unmodified in this workspace.
     *
     * @param node
     * @return true iff the node is in this workspace
     */
    public boolean contains( EmsScriptNode node  ) {
        WorkspaceNode nodeWs = node.getWorkspace();
        if ( this.equals( nodeWs ) ) return true;
        
        WorkspaceNode parentWs = getParentWorkspace();
        if ( parentWs == null ) return ( nodeWs == null );
        return parentWs.contains( node );
    }

    /**
     * Replicate this node and its parent/grandparent folders into this
     * workspace if not already present.
     *
     * @param node
     * @return
     * @throws Exception 
     */
    public EmsScriptNode replicateWithParentFolders( EmsScriptNode node ) {// throws Exception {
        if ( Debug.isOn() ) Debug.outln( "replicateFolderWithChain( " + node + " )" );
        if ( node == null ) return null;
        EmsScriptNode newFolder = node;

        //String thisName = exists() ? getName() : null;
        String nodeName = node != null && node.exists() ? node.getName() : null;

        // make sure the folder's parent is replicated
        EmsScriptNode parent = node.getParent();

        if ( parent == null || parent.isWorkspaceTop() ) {
            parent = this; // put in the workspace
        }
        String parentName = parent != null && parent.exists() ? parent.getName() : null;

        // Get the parent in this workspace. In case there are multiple nodes
        // with the same cm:name, use the grandparent to disambiguate where it
        // should be.
        if ( parent != null && parent.exists() && !this.equals( parent.getWorkspace() ) ) {
            EmsScriptNode grandParent = parent.getParent();
            ArrayList< NodeRef > arr = NodeUtil.findNodeRefsByType( parentName, SearchType.CM_NAME.prefix, false, false, this, null, false, true, getServices(), false );
            for ( NodeRef ref : arr ) {
                EmsScriptNode p = new EmsScriptNode( ref, getServices() );
                EmsScriptNode gp = p.getParent();
                if ( grandParent == gp || ( grandParent != null && gp != null && grandParent.getName().equals( gp.getName() ) ) ) {
                    parent = p;
                    break;
                }
            }
            if ( !this.equals( parent.getWorkspace() ) ) {
                parent = replicateWithParentFolders( parent );
            }
        } else if ( parent == null || !parent.exists() ) {
            Debug.error("Error! Bad parent when replicating folder chain! " + parent );
        }

        // If the node is not already in this workspace, clone it.
        if ( !this.equals( node.getWorkspace() ) ) {
            EmsScriptNode nodeGuess = null;
            ArrayList< NodeRef > array = NodeUtil.findNodeRefsByType( nodeName, SearchType.CM_NAME.prefix, false, false, this, null, false, true, getServices(), false );
            for ( NodeRef ref : array ) {
                EmsScriptNode n = new EmsScriptNode( ref, getServices() );
                EmsScriptNode np = n.getParent();
                // Note: need the last check of the parent's in case the node found was in the workspace, but
                // under a different site, ie Models folder
                if (n != null && n.exists() && this.equals( n.getWorkspace() ) && np != null && np.equals( parent )) {
                    nodeGuess = n;
                    break;
                }
            }
            if ( nodeGuess == null) {
                newFolder = node.clone(parent);
                newFolder.setWorkspace( this, node.getNodeRef() );
            } else {
                newFolder = nodeGuess;
            }
        }

        if ( Debug.isOn() ) Debug.outln( "returning newFolder: " + newFolder );
        return newFolder;
    }
    

    public static String getId( WorkspaceNode ws ) {
        if ( ws == null ) return "master";
        return ws.getNodeRef().getId();
    }

    public static String getWorkspaceName( WorkspaceNode ws ) {
        if ( ws == null ) return "master";
        return ws.getWorkspaceName();
    }
    
    public static String getName( WorkspaceNode ws ) {
        if ( ws == null ) return "master";
        return ws.getName();
    }
    
    // don't want to override getName() in case that causes problems for
    // alfresco's code
    public String getWorkspaceName() {
        return (String)getProperty("ems:workspace_name");
    }
    
    public static String getQualifiedId( WorkspaceNode ws ) {
        return getQualifiedId( ws, null );
    }
    public static String getQualifiedId( WorkspaceNode ws,
                                         Seen<WorkspaceNode> seen ) {
        if ( ws == null ) {
            return getId( ws ); 
        }
        Pair< Boolean, Seen< WorkspaceNode > > p = Utils.seen( ws, true, seen );
        if ( p.first ) return null;
        seen = p.second;
        return getQualifiedId( ws.getParentWorkspace(), seen ) + "/" + ws.getId();
    }

    public static String getQualifiedName( WorkspaceNode ws ) {
        return getQualifiedName( ws, null );
    }
    public static String getQualifiedName( WorkspaceNode ws,
                                           Seen<WorkspaceNode> seen ) {
        if ( ws == null ) {
            return getWorkspaceName( ws ); 
        }
        Pair< Boolean, Seen< WorkspaceNode > > p = Utils.seen( ws, true, seen );
        if ( p.first ) return null;
        seen = p.second;
        return getQualifiedName( ws.getParentWorkspace(), seen ) + "/" + ws.getWorkspaceName();
    }

    public WorkspaceNode getCommonParent(WorkspaceNode other) {
        return getCommonParent( this, other );
    }

    public static WorkspaceNode getCommonParent( WorkspaceNode ws1,
                                                 WorkspaceNode ws2 ) {
        Set<WorkspaceNode> parents = new TreeSet<WorkspaceNode>();
        while ( ( ws1 != null || ws2 != null )
                && ( ws1 == null ? !ws2.equals( ws1 ) : !ws1.equals( ws2 ) )
                && ( ws1 == null || !parents.contains( ws1 ) )
                && ( ws2 == null || !parents.contains( ws2 ) ) ) {
            if ( ws1 != null ) {
                parents.add( ws1 );
                ws1 = ws1.getParentWorkspace();
            }
            if ( ws2 != null ) {
                parents.add( ws2 );
                ws2 = ws2.getParentWorkspace();
            }
        }
        if ( ws1 != null && ( ws1.equals( ws2 ) || parents.contains( ws1 ) ) ) {
            return ws1;
        }
        if ( ws2 != null && parents.contains( ws2 ) ) {
            return ws2;
        }
        return null;
    }

    public Set< NodeRef > getChangedNodeRefs( Date dateTime ) {
        Set< NodeRef > changedNodeRefs = new TreeSet< NodeRef >(NodeUtil.nodeRefComparator);
        if ( dateTime != null && dateTime.before( getCreationDate() ) ) {
            return changedNodeRefs;
        }
        ArrayList< NodeRef > refs =
                NodeUtil.findNodeRefsByType( getNodeRef().toString(),
                                             SearchType.WORKSPACE.prefix, false,
                                             true, null, dateTime, false, true,
                                             getServices(), true );
        changedNodeRefs.addAll( refs );

        // remove commits
        ArrayList< EmsScriptNode > commits =
                CommitUtil.getCommits( this, null, getServices(), getResponse() );
        commits.add( CommitUtil.getCommitPkg( this, null, getServices(),
                                              getResponse() ) );
        List<NodeRef> commitRefs = NodeUtil.getNodeRefs( commits );
        changedNodeRefs.removeAll(commitRefs);

        return changedNodeRefs;
    }

    public Set< String > getChangedElementIds( Date dateTime ) {
        Set< String > changedElementIds = new TreeSet< String >();
        Set< NodeRef > refs = getChangedNodeRefs( dateTime );
        List< EmsScriptNode > nodes = toEmsScriptNodeList( refs );
        changedElementIds.addAll( EmsScriptNode.getNames( nodes ) );
        return changedElementIds;
    }

    
//    /**
//     * Get the NodeRefs of this workspace that have changed with respect to
//     * another workspace. This method need not check the actual changes to see
//     * if they are different and may be a superset of those actually changed.
//     * 
//     * @param other
//     * @param dateTime
//     * @param otherTime
//     * @return
//     */
//    public Set< NodeRef > getChangedNodeRefsWithRespectTo( WorkspaceNode other,
//                                                           Date dateTime,
//                                                           Date otherTime ) {
//        return getChangedNodeRefsWithRespectTo( this, other, dateTime, otherTime );
//    }
    
    
    /**
     * Get the NodeRefs of this workspace that have changed with respect to
     * another workspace. This method need not check the actual changes to see
     * if they are different and may be a superset of those actually changed.
     * 
     * @param thisWs
     * @param otherWs
     * @param dateTime
     * @param otherTime
     * @return
     */
    public static Set< NodeRef > getChangedNodeRefsWithRespectTo( WorkspaceNode thisWs,
                                                                  WorkspaceNode otherWs,
                                                                  Date dateTime,
                                                                  Date otherTime,
                                                                  ServiceRegistry services,
                                                                  StringBuffer response,
                                                                  Status status ) {
        //System.out.println( getName(thisWs) + ".getChangedNodeRefsWithRespectTo(" + getName(otherWs) + ", " + dateTime + ", " + otherTime +  ")" );

        Set< NodeRef > changedNodeRefs = 
                new TreeSet< NodeRef >(NodeUtil.nodeRefComparator);//getChangedNodeRefs());
        WorkspaceNode targetParent = getCommonParent( thisWs, otherWs );
        WorkspaceNode parent = thisWs;
        WorkspaceNode lastParent = parent;
        
        // Get nodes in the workspace that have changed with respect to the
        // common parent. To avoid computation, these do not take time into
        // account except to rule out workspaces with changes only after
        // dateTime.
        while ( parent != null && !parent.equals( targetParent ) ) {
            Set< NodeRef > changes = parent.getChangedNodeRefs( dateTime );
            
            //System.out.println( "nodes in " + getName(parent) + " = " + changes );
            
            changedNodeRefs.addAll( changes );
            parent = parent.getParentWorkspace();
            if ( parent != null ) lastParent = parent;
        }
        
        // Now gather nodes in the common parent chain after otherTime and
        // before dateTime. We need to get these from the transaction history
        // (or potentially the version history) to only include those that
        // changed within a timeframe. Otherwise, we would have to include the
        // entire workspace, which could be master, and that would be too big.
        if ( otherTime != null && dateTime != null && dateTime.after( otherTime ) ) {
            ArrayList< EmsScriptNode > commits =
                    CommitUtil.getCommitsAllSitesInDateTimeRange( otherTime,
                                                                  dateTime,
                                                                  lastParent,
                                                                  services,
                                                                  response,
                                                                  false );
            // TODO -- REVIEW -- The created time of the commit is after the
            // modified times of the items in the diff (right?). Thus, it is
            // unclear whether any commits after the later time point can be
            // ruled out since the nodes in the diff may have been modified long
            // before the commit was created. For instance if a transaction
            // includes posting 100 elements serially, then the commit time
            // could be many seconds after the first element was modified. One
            // solution would be to give the commit both a start and end time
            // bounding the times that changes we made in the transaction. We
            // should not assume that transactions are all atomic (one at a
            // time); thus, the time interval of one commit may overlap with
            // others'.
            for ( EmsScriptNode commit : commits ) {
                String type = (String)commit.getProperty( "ems:commitType" );
                if ( "COMMIT".equals( type ) ) {
                    String diffStr = (String)commit.getProperty( "ems:commit" );
                    try {
                        JSONObject diff = new JSONObject( diffStr );
                        
                        Set< EmsScriptNode > elements =
                                WorkspaceDiff.getAllChangedElementsInDiffJson( diff,
                                                                               services );
                        if ( elements != null )
                            changedNodeRefs.addAll( NodeUtil.getNodeRefs( elements ) );
                    } catch ( JSONException e ) {
                        String msg = "ERROR! Could not parse json from CommitUtil: \"" + diffStr + "\"";
                        if ( response != null ) {
                            response.append( msg + "\n" );
                            if ( status != null ) {
                                status.setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                msg );
                            }
                        }
                        Debug.error( false, msg );
                        e.printStackTrace();
                        changedNodeRefs = null;
                        break;
                    }
                }
            }
        }
        return changedNodeRefs;
    }

    public Set< String > getChangedElementIdsWithRespectTo( WorkspaceNode other, Date dateTime ) {
        Set< String > changedElementIds = new TreeSet< String >();//getChangedElementIds());
        if ( NodeUtil.exists( other ) ) {
            WorkspaceNode targetParent = getCommonParent( other );
            WorkspaceNode parent = this;
            while ( parent != null && !parent.equals( targetParent ) ) {
                changedElementIds.addAll( parent.getChangedElementIds( dateTime ) );
                parent = parent.getParentWorkspace();
            }
        }
        return changedElementIds;
    }

    /**
     * Add the workspace name and id metadata onto the provided JSONObject
     * @param jsonObject
     * @param ws
     * @throws JSONException
     */
    public static void
            addWorkspaceNamesAndIds( JSONObject json, WorkspaceNode ws ) throws JSONException {
        json.put( "name",  getWorkspaceName(ws) );
        json.put( "id", getId(ws) );
        json.put( "qualifiedName", getQualifiedName( ws ) );
        json.put( "qualifiedId", getQualifiedId( ws ) );
    }
    
    @Override
    public JSONObject toJSONObject( Date dateTime ) throws JSONException {
        JSONObject json = new JSONObject();

        addWorkspaceNamesAndIds(json, this );
        json.put( "creator", getProperty( "cm:modifier" ) );
        // REVIEW -- This assumes that the workspace does not changed after it
        // is created, but wouldn't it's ems:lastTimeSyncParent property be
        // expected to change?
        json.put( "created", TimeUtils.toTimestamp( (Date)getProperty("cm:created") ) );
        json.put( "modified", TimeUtils.toTimestamp( (Date)getProperty("cm:modified") ) );
        json.put( "parent", getId(getParentWorkspace())); // this handles null as master

        // REVIEW -- Why is ems:lastTimeSyncParent called the "branched"
        // date? Shouldn't the branched date always be the same as the created
        // date? This is for future functionality when we track when the child pulls from the
        // parent last.
        json.put( "branched", TimeUtils.toTimestamp( (Date)getProperty("ems:lastTimeSyncParent") ) );

        return json;
    }

    /**
     * Get the workspace by name, but since two workspaces can have the same
     * name as long as their parents are different, we need to check the results
     * and at least try to match to the user.
     * 
     * @param workspaceName
     * @param services
     * @param response
     * @param responseStatus
     * @param userName
     * @return
     */
    public static WorkspaceNode getWorkspaceFromName( String workspaceName,
                                                    ServiceRegistry services,
                                                    StringBuffer response,
                                                    Status responseStatus,
                                                    //boolean createIfNotFound,
                                                    String userName ) {
        WorkspaceNode workspace = null;
    
        // Get the workspace by name, but since two workspaces can have
        // the same name as long as their parents are different, we need
        // to check the results and at least try to match to the user.
        ArrayList< NodeRef > refs =
                NodeUtil.findNodeRefsByType( workspaceName, SearchType.WORKSPACE_NAME.prefix,
                                             true, true, null, null,
                                             true, true, services,
                                             false );
        if ( Utils.isNullOrEmpty( refs ) ) {
            return null;
        }
        if ( refs.size() == 1 ) {
            NodeRef ref = refs.get( 0 );
            return existingReadableWorkspaceFromNodeRef( ref, services, response,
                                                         responseStatus );
        }
        boolean matchedUser = false;
        boolean multipleNonMatches = false;
        for ( NodeRef nr : refs ) {
            WorkspaceNode ws = new WorkspaceNode( nr, services );
            EmsScriptNode p = ws.getParent();
            boolean matches = p != null && p.getName().equals( userName );
            if ( !matchedUser ) matchedUser = matches;
            else if ( matches ) {
                String msg = "Warning! Matched multiple workspaces with name "
                             + workspaceName + " for user " + userName;
                response.append( msg );
                break;
            }
            ws = existingReadableWorkspaceFromNodeRef( nr, services,
                                                       response, responseStatus );
            if ( ws != null ) { 
                if ( workspace == null ) {
                    workspace = ws;
                } else if ( matches && !matchedUser ) {
                    workspace = ws;
                    matchedUser = true;
                } else if ( !matches && !matchedUser ) {
                    multipleNonMatches = true;
                }
            }
        }
        if ( !matchedUser && multipleNonMatches ) {
            String msg = "Warning! Matched multiple workspaces with name "
                        + workspaceName + " but not in user home, " + userName;
            response.append( msg );
        }
    
        return workspace;
    }

    public static WorkspaceNode existingReadableWorkspaceFromNodeRef( NodeRef ref,
                                                                      ServiceRegistry services,
                                                                      StringBuffer response,
                                                                      Status responseStatus ) {
        if ( ref != null ) {
            WorkspaceNode workspace = new WorkspaceNode( ref, services, response,
                                                         responseStatus );
            if ( workspace.exists() && workspace.hasAspect( "ems:Workspace" ) ) {
                // TODO -- check read permissions
                if ( workspace.checkPermissions( PermissionService.READ ) ) {
                    if ( Debug.isOn() ) Debug.outln( "workspace exists: " + workspace );
                    return workspace;
                }
            }
        }
        return null;
    }

    public static WorkspaceNode getWorkspaceFromId( String nameOrId,
                                                    ServiceRegistry services,
                                                    StringBuffer response,
                                                    Status responseStatus,
                                                    //boolean createIfNotFound,
                                                    String userName ) {
        if ( Utils.isNullOrEmpty( nameOrId ) ) {
            if ( Debug.isOn() ) {
                Debug.outln( "no workspace for bad id: " + nameOrId );
            }
            return null;
        }
        // Use null to indicate master workspace
        if ( nameOrId.toLowerCase().equals( "master" ) ) {
            return null;
        }
        WorkspaceNode workspace = null;
    
        // Tyr to match the alfresco id
        NodeRef ref = NodeUtil.findNodeRefByAlfrescoId( nameOrId );
        if ( ref != null ) {
            workspace = existingReadableWorkspaceFromNodeRef( ref, services,
                                                              response,
                                                              responseStatus );
            if ( workspace != null ) return workspace;
        }
    
        // Try to match the workspace name 
        workspace = getWorkspaceFromName( nameOrId, services, response,
                                          responseStatus, userName );
            
        if ( workspace != null ) return workspace;
    
        // Try the cm:name
        ref = NodeUtil.findNodeRefById( nameOrId, true, null, null, services, false );
        if ( ref != null ) {
            workspace = existingReadableWorkspaceFromNodeRef( ref, services,
                                                              response,
                                                              responseStatus );
            if ( workspace != null ) return workspace;
        }
        
        if ( Debug.isOn() ) {
            Debug.outln( "workspace does not exist and is not to be created: "
                         + nameOrId );
        }
        return null;
    }

}
