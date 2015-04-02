package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.connections.RestPostConnection;
import gov.nasa.jpl.view_repo.webscripts.HostnameGet;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;


/**
 * Utilities for saving commits and sending out deltas based on commits
 * @author cinyoung
 *
 */
public class CommitUtil {
    static Logger logger = Logger.getLogger(CommitUtil.class);

    private CommitUtil() {
        // defeat instantiation
    }

    private static JmsConnection jmsConnection = null;
    private static RestPostConnection restConnection = null;
    private static ServiceRegistry services = null;

    public static void setJmsConnection(JmsConnection jmsConnection) {
        if (logger.isInfoEnabled()) logger.info( "Setting jms" );
        CommitUtil.jmsConnection = jmsConnection;
    }

    public static void setRestConnection(RestPostConnection restConnection) {
        if (logger.isInfoEnabled()) logger.info( "Setting rest" );
        CommitUtil.restConnection = restConnection;
    }

    public static void setServices(ServiceRegistry services) {
        if (logger.isInfoEnabled()) logger.info( "Setting services" );
        CommitUtil.services = services;
    }

    /**
	 * Gets the commit package in the specified workspace (creates if possible)
	 * @param workspace
	 * @param services
	 * @param response
	 * @param create
	 * @return
	 */
    private static EmsScriptNode getOrCreateCommitPkg( WorkspaceNode workspace,
                                                       ServiceRegistry services,
                                                       StringBuffer response,
                                                       boolean create ) {
        EmsScriptNode context = null;

        // If it is the master branch then the commits folder is in company home:
        if (workspace == null) {
            context = NodeUtil.getCompanyHome( services );
        }
        // Otherwise, it is in the workspace:
        else {
            context = workspace;
        }

        EmsScriptNode commitPkg = context.childByNamePath( "commits" );

        if (commitPkg == null && create) {
            commitPkg = context.createFolder( "commits" );
            // commits directory needs global permissions
            commitPkg.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
        }

        // Create the date folders if needed.  Want to return the "commit" folder
        // if create is false:
        if (create) {
            commitPkg = NodeUtil.getOrCreateDateFolder( commitPkg );
        }

        if ( commitPkg != null ) commitPkg.getOrSetCachedVersion();
        if ( context != null ) context.getOrSetCachedVersion();

        return commitPkg;
    }

    /**
     * Gets the commit package for the specified workspace
     * @param workspace
     * @param siteName
     * @param services
     * @param response
     * @return
     */
	public static EmsScriptNode getCommitPkg(WorkspaceNode workspace,
	                                         ServiceRegistry services,
	                                         StringBuffer response) {
	    return getOrCreateCommitPkg( workspace, services, response,
	                                 false );
	}

	/**
	 * Given a workspace gets an ordered list of the commit history
	 * @param workspace
	 * @param siteName
	 * @param services
	 * @param response
	 * @return
	 */
	public static ArrayList<EmsScriptNode> getCommits(WorkspaceNode workspace,
	                                           ServiceRegistry services,
	                                           StringBuffer response) {
        // to make sure no permission issues, run as admin
        AuthenticationUtil.setRunAsUser( "admin" );
	    
	    ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();

	    // Note: if workspace is null, then will get the master workspace commits
	    EmsScriptNode commitPkg = getCommitPkg(workspace, services, response);

	    if (commitPkg != null) {
            commits.addAll(WebScriptUtil.getAllNodesInPath(commitPkg.getQnamePath(),
                                                           "ASPECT",
                                                           "ems:Committable",
                                                           workspace,
                                                           null,
                                                           services,
                                                           response));

            Collections.sort( commits, new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator() );
	    }

        AuthenticationUtil.setRunAsUser( AuthenticationUtil.getFullyAuthenticatedUser() );

	    return commits;
	}

	/**
	 * Gets the latest created folder in the passed context
	 * @param context
	 * @return
	 */
	private static EmsScriptNode getLatestFolder(EmsScriptNode context) {

	      return getLatestFolderBeforeTime(context, 0);
	}

	/**
     * Gets the latest created folder in the passed context before or equal to
     * the passed time.
     * If the passed time is zero then gets the latest folder.
     *
     * @param context
     * @return
     */
    private static EmsScriptNode getLatestFolderBeforeTime(EmsScriptNode context,
                                                           int time) {

        EmsScriptNode latestFolder = null;
          Set<EmsScriptNode> folders = context.getChildNodes();
          ArrayList<EmsScriptNode> foldersList = new ArrayList<EmsScriptNode>(folders);
          Collections.sort( foldersList,
                            new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator() );

          // Get the latest commit folder:
          if (time == 0) {
              if (foldersList.size() > 0) {
                  latestFolder = foldersList.get( 0 );
              }
          }
          // Otherwise, get the latest commit folder before or equal the passed time:
          else {
              for (EmsScriptNode folder : foldersList) {
                  String folderName = folder.getName();
                  Integer folderVal = Integer.valueOf( folderName );
                  if (folderName != null && folderVal != null && time >= folderVal) {
                      latestFolder = folder;
                      break;
                  }
              }
          }

          return latestFolder;
    }

	/**
	 * Get the most recent commit in a workspace
	 * @param ws
	 * @param siteName
	 * @param services
	 * @param response
	 * @return
	 */
	public static EmsScriptNode getLastCommit(WorkspaceNode ws,
	                                          ServiceRegistry services,
	                                          StringBuffer response) {

	    EmsScriptNode yearFolder = null;
	    EmsScriptNode monthFolder = null;
        EmsScriptNode dayFolder = null;
        ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();

	    // Note: if workspace is null, then will get the master workspace commits
        EmsScriptNode commitPkg = getCommitPkg(ws, services, response);

        if (commitPkg != null) {

            // Get the latest year/month/day folder and search for all content within it,
            // then sort:
            yearFolder = getLatestFolder(commitPkg);
            if (yearFolder != null) {
                monthFolder = getLatestFolder(yearFolder);
                if (monthFolder != null) {
                    dayFolder = getLatestFolder(monthFolder);
                    if (dayFolder != null) {
                        commits.addAll( dayFolder.getChildNodes() );
                        
                        // Sort the commits so that the latest commit is first:
                        Collections.sort( commits, new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator() );
                    }
                }
            }

        }

        // This method is too inefficient to use
	    //ArrayList<EmsScriptNode> commits = getCommits(ws, services, response);

        // Return the latest commit:
	    if (commits.size() > 0) {
	        return commits.get( 0 );
	    }

	    return null;
	}

	/**
	 * Return the latest commit before or equal to the passed date
	 */
	public static EmsScriptNode getLatestCommitAtTime(Date date,
	                                                  WorkspaceNode workspace,
                                                      ServiceRegistry services,
                                                      StringBuffer response) {

        EmsScriptNode yearFolder = null;
        EmsScriptNode monthFolder = null;
        EmsScriptNode dayFolder = null;
        ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();

	    if (date != null) {
    	    Calendar cal = Calendar.getInstance();
    	    cal.setTime( date );
    	    int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) + 1; // Adding 1 b/c this is 0 to 11
            int year = cal.get(Calendar.YEAR);

            // Note: if workspace is null, then will get the master workspace commits
            EmsScriptNode commitPkg = getCommitPkg(workspace, services, response);

            if (commitPkg != null) {

                // Get the latest year/day/month folder before the date:
                yearFolder = getLatestFolderBeforeTime(commitPkg, year);
                if (yearFolder != null) {
                    monthFolder = getLatestFolderBeforeTime(yearFolder, month);
                    if (monthFolder != null) {
                        dayFolder = getLatestFolderBeforeTime(monthFolder, day);
                        if (dayFolder != null) {
                            commits.addAll( dayFolder.getChildNodes() );

                            // Sort the commits so that the latest commit is first:
                            Collections.sort( commits, new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator() );
                        }
                    }
                }
            }

            // Now go through the list of commits to find the latest one
            // before or equal to the desired date:
            EmsScriptNode earliestCommit = null;
            for (EmsScriptNode commit : commits) {
                earliestCommit = commit;
                Date created = commit.getCreationDate();
                if (!date.before( created )) {
                    return commit;
                }
            }

            // If we have not returned at this point, then the date must be earlier than any of the
            // commits during the day of the date, so must try the previous commit for the earliest
            // commit found:
            if (earliestCommit != null) {
                earliestCommit = getPreviousCommit(earliestCommit);
                if (earliestCommit != null && !date.before( earliestCommit.getCreationDate() )) {
                    return earliestCommit;
                }
            }
	    }

	    return null;
	}

	/**
	 * Gets all the commits in the specified time range from the startWorkspace to the 
	 * endWorkspace. If the endWorkspace is not a parent of the startWorkspace, this will
	 * search to the master.
	 * @param fromDateTime
	 * @param toDateTime
	 * @param startWorkspace
	 * @param endWorkspace
	 * @param services
	 * @param response
	 * @return
	 */
	public static ArrayList<EmsScriptNode> getCommitsInDateTimeRange( Date fromDateTime,
	                                                                  Date toDateTime,
	                                                                  WorkspaceNode startWorkspace,
	                                                                  WorkspaceNode endWorkspace,
	                                                                  ServiceRegistry services,
	                                                                  StringBuffer response) {
        // to make sure no permission issues, run as admin
        String originalUser = NodeUtil.getUserName();
        AuthenticationUtil.setRunAsUser( "admin" );
	    
	    // TODO REVIEW consider using date folders to narrow the range of commits to be parsed
	    //             through, rather than using getLastCommit().

	    // skip over too new workspaces
	    while ( startWorkspace != null ) {
	        Date created = startWorkspace.getCreationDate();
	        if ( toDateTime != null && created.after( toDateTime ) ) {
	            startWorkspace = startWorkspace.getParentWorkspace();
	        } else {
	            break;
	        }
	    }
	    // gather commits between dates while walking up workspace parents
	    ArrayList<EmsScriptNode> commits = new ArrayList< EmsScriptNode >();
        while ( true ) { // run until endWorkspace or master
            EmsScriptNode commit = getLastCommit( startWorkspace, services, response );
            while ( commit != null ) {
                Date created = commit.getCreationDate();
                if ( fromDateTime != null && created.before( fromDateTime ) ) break;
                if ( toDateTime == null || !created.after( toDateTime ) ) {
                    commits.add( commit );
                }
                commit = getPreviousCommit(commit);
            }
            if ( startWorkspace == null ) break;
            if ( startWorkspace.equals(endWorkspace) ) break;
            startWorkspace = startWorkspace.getParentWorkspace();
        }
        
        AuthenticationUtil.setRunAsUser( originalUser );
        return commits;
	}

    public static EmsScriptNode getPreviousCommit( EmsScriptNode commit ) {
        List< EmsScriptNode > parentRefs = getPreviousCommits( commit );
        if ( !Utils.isNullOrEmpty( parentRefs ) ) {
            return parentRefs.get( parentRefs.size() - 1 ); // last includes last merge
        }
        return null;
    }

    public static List<EmsScriptNode> getPreviousCommits( EmsScriptNode commit ) {
        @SuppressWarnings( "unchecked" )
        ArrayList< NodeRef > parentNodes =
                (ArrayList< NodeRef >)commit.getProperties().get( "{http://jpl.nasa.gov/model/ems/1.0}commitParents" );
        List<EmsScriptNode> parentRefs = commit.toEmsScriptNodeList( parentNodes );
        return parentRefs;
    }

    private static NodeRef commitRef = null;
    public synchronized static NodeRef commit(final WorkspaceNode workspace,
                                              final String body,
                                              final String msg,
                                              final boolean runWithoutTransactions,
                                              final ServiceRegistry services,
                                              final StringBuffer response) {
        //logger.warn( "sync commit start" );
        commitRef = null;
        new EmsTransaction(services, response, null, runWithoutTransactions ) {
            
            @Override
            public void run() throws Exception {
                commitRef = commitTransactionable(workspace, body, msg, services, response);
            }
        };
        //logger.warn( "sync commit end" );
        return commitRef;
	}


	private static NodeRef commitTransactionable(
	                                           WorkspaceNode workspace,
	                                           String body,
	                                           String msg,
	                                           ServiceRegistry services,
	                                           StringBuffer response) throws JSONException {
	    return createCommitNode( workspace, null, workspace, null, null, "COMMIT", msg,
	                             body, services, response );
    }


    private static NodeRef mergeRef = null;
    public synchronized static NodeRef merge( final JSONObject wsDiff,
                                              final WorkspaceNode source,
                                              final WorkspaceNode target,
                                              final Date dateTimeSrc,
                                              final Date dateTimeTarget,
                                              final String msg,
                                              final boolean runWithoutTransactions,
                                              final ServiceRegistry services,
                                              final StringBuffer response ) {
        //logger.warn( "sync merge start" );
        mergeRef = null;
        new EmsTransaction(services, response, null, runWithoutTransactions ) {
            
            @Override
            public void run() throws Exception {
            mergeRef = mergeTransactionable(wsDiff, source, target, target,
                                            dateTimeSrc, dateTimeTarget,
                                            msg, services, response);
            }
        };
        //logger.warn( "sync merge end" );
        return mergeRef;
    }

	private static NodeRef mergeTransactionable( JSONObject wsDiff,
                                              WorkspaceNode source1,
                                              WorkspaceNode source2,
                                              WorkspaceNode target,
                                              Date dateTime1,
                                              Date dateTime2,
                                              String msg,
                                              ServiceRegistry services,
                                              StringBuffer response ) {

        return createCommitNode( source1, source2, target, dateTime1, dateTime2,
                                 "MERGE", msg,
                                 wsDiff.toString(),services, response, true );
    }

    /**
	 */
	public static boolean revertCommit(EmsScriptNode commit,
			ServiceRegistry services) {
		boolean status = true;
//		String content = (String) commit.getProperty( "ems:commit" );
		// TODO: need to revert to original elements
		// TODO: revert moves.... this may not be easy
		// TODO: revert adds (e.g., make them deleted)
//		try {
//			JSONObject changeJson = new JSONObject(content);
//
//			JSONArray changeArray = changeJson.getJSONArray(COMMIT_KEY);
//			for (int ii = 0; ii < changeArray.length(); ii++) {
//				JSONObject json = changeArray.getJSONObject(ii);
//				EmsScriptNode node = getScriptNodeByNodeRefId(json.getString(ID_KEY), services);
//				if (node != null) {
//					String versionKey = json.getString(VERSION_KEY);
//					node.revert("reverting to version " + versionKey
//							+ " as part of commit " + commit.getName(), false,
//							versionKey);
//				}
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//			status = false;
//		}

		return status;
	}

	public static EmsScriptNode getScriptNodeByNodeRefId(String nodeId, ServiceRegistry services) {
		String store = EmsScriptNode.getStoreRef().toString();
		if (!nodeId.startsWith(store)) {
			nodeId = store + "/" + nodeId;
		}
		List<NodeRef> nodeRefs = NodeRef.getNodeRefs(nodeId);
		if (nodeRefs.size() == 1) {
			return new EmsScriptNode(nodeRefs.get(0), services);
		} else {
			if (nodeRefs.size() <= 0) {
				// not found
			} else {
				// too many found - couldn't disambiguate
			}
		}
		return null;
	}

    private static NodeRef branchRef = null;
	public synchronized static NodeRef branch(final WorkspaceNode srcWs,
	                                          final WorkspaceNode dstWs,
	                                          final String msg,
	                                          boolean runWithoutTransactions,
	                                          ServiceRegistry services,
	                                          StringBuffer response) {
        //logger.warn( "sync branch start" );
	    branchRef = null;
        new EmsTransaction(services, response, null, runWithoutTransactions ) {
            
            @Override
            public void run() throws Exception {
                branchRef = branchTransactionable(srcWs, dstWs, msg, services, response);
            }
        };
        //logger.warn( "sync branch end" );
        return branchRef;
	}

    private static NodeRef branchTransactionable( WorkspaceNode srcWs,
                                               WorkspaceNode dstWs,
                                               String msg,
                                               ServiceRegistry services,
                                               StringBuffer response )
                                                       throws JSONException {

	    return createCommitNode(srcWs, null, dstWs, null, null,
	                            "BRANCH", msg, "{}", services, response);
	}

    // TODO -- REVIEW -- Just copied branch and search/replaced "branch" with "merge"
    @Deprecated
    public static void merge(final WorkspaceNode srcWs, final WorkspaceNode dstWs,
                             final String msg,
                             boolean runWithoutTransactions,
                             ServiceRegistry services, StringBuffer response) {
        new EmsTransaction(services, response, null, runWithoutTransactions ) {
            
            @Override
            public void run() throws Exception {
                mergeTransactionable(srcWs, dstWs, dstWs, msg, services, response);
            }
        };
    }

    private static void mergeTransactionable( WorkspaceNode srcWs1,
                                              WorkspaceNode srcWs2,
                                               WorkspaceNode dstWs,
                                               String msg,
                                               ServiceRegistry services,
                                               StringBuffer response )
                                                       throws JSONException {
        createCommitNode(srcWs1, srcWs2, dstWs, null, null,
                         "MERGE", msg, "{}", services, response, true);
    }

	/**
	 * Update a commit with the parent and child information
	 * @param prevCommit   Parent commit node
	 * @param currCommit   Child commit node
	 * @return
	 */
	protected static boolean updateCommitHistory(EmsScriptNode prevCommit,
                                                 EmsScriptNode currCommit,
                                                 String originalUser) {
	    if (prevCommit == null || currCommit == null) {
	        return false;
	    } else {
// This isn't necessary since the user should only be calling this as admin from createCommitNode
//	        if (!prevCommit.hasPermission( "Write" )) {
//	            logger.error("no permissions to write to previous commit: " + prevCommit);
//	            return false;
//	        }
	        
	        // FIXME: not sure why getting property is providing [null] array
//            ArrayList< NodeRef > parentRefs = currCommit.getPropertyNodeRefs( "ems:commitParents" );
            @SuppressWarnings( "unchecked" )
            ArrayList< NodeRef > parentRefs =
                    (ArrayList< NodeRef >)currCommit.getProperties().get( "{http://jpl.nasa.gov/model/ems/1.0}commitParents" );
            if ( parentRefs == null ) {
                parentRefs = new ArrayList< NodeRef >();
            }
            NodeRef nr = prevCommit.getNodeRef();
            if (!parentRefs.contains( nr )) {
                parentRefs.add( nr );
            }
            currCommit.setProperty( "ems:commitParents", parentRefs );

//            ArrayList< NodeRef > childRefs = prevCommit.getPropertyNodeRefs( "ems:commitChildren" );
            @SuppressWarnings( "unchecked" )
            ArrayList< NodeRef > childRefs =
                    (ArrayList< NodeRef >)prevCommit.getProperties().get( "{http://jpl.nasa.gov/model/ems/1.0}commitChildren" );
            if ( childRefs == null ) {
                childRefs = new ArrayList< NodeRef >();
            }
            NodeRef nrCurr = currCommit.getNodeRef();
            if (!childRefs.contains( nrCurr )) {
                childRefs.add( nrCurr );
            }
            prevCommit.setProperty( "ems:commitChildren", childRefs );
            prevCommit.getOrSetCachedVersion();
            
            // set modifier to original user (since we should be running as admin in here)
            currCommit.setProperty( "cm:modifier", originalUser );
            prevCommit.setProperty( "cm:modifier", originalUser );
	    }
        return true;
	}

	/**
     * Create a commit node specifying the workspaces. Typically, since the serialization takes
     * a while, the commit node is created first, then it is updated in the background using the
     * ActionExecuter.
     */
    protected static NodeRef createCommitNode(WorkspaceNode srcWs1, WorkspaceNode srcWs2,
                                              WorkspaceNode dstWs,
                                              Date dateTime1,
                                              Date dateTime2,
                                              String type, String msg, String body,
                                              ServiceRegistry services, StringBuffer response) {

        return createCommitNode(srcWs1, srcWs2, dstWs, dateTime1, dateTime2, type, msg, body,
                                services, response, false);
    }

	/**
	 * Create a commit node specifying the workspaces. Typically, since the serialization takes
	 * a while, the commit node is created first, then it is updated in the background using the
	 * ActionExecuter.
	 */
    protected static NodeRef
            createCommitNode( WorkspaceNode srcWs1, WorkspaceNode srcWs2,
                              WorkspaceNode dstWs, Date dateTime1,
                              Date dateTime2, String type, String msg,
                              String body, ServiceRegistry services,
                              StringBuffer response, boolean twoSourceWorkspaces ) {
        NodeRef result = null;
        // to make sure no permission issues, run as admin
        // the commit histories are updated based on the original user
        String originalUser = NodeUtil.getUserName();
        AuthenticationUtil.setRunAsUser( "admin" );

        // Get the most recent commit(s) before creating a new one
	    // Note: must do this before getOrCreateCommitPkg() call in case the commit to be created is the
	    //       first for the day, and so will create the day folder in the getOrCreateCommitPkg() call
        EmsScriptNode prevCommit1 = dateTime1 != null ? getLatestCommitAtTime( dateTime1, srcWs1, services, response ) :
                                                        getLastCommit( srcWs1, services, response );
        EmsScriptNode prevCommit2 = null;
        if (twoSourceWorkspaces) {
            prevCommit2 = dateTime2 != null ? getLatestCommitAtTime( dateTime2, srcWs2, services, response ) :
                                              getLastCommit( srcWs2, services, response );
        }

        EmsScriptNode commitPkg = getOrCreateCommitPkg( dstWs, services, response, true );

        if (commitPkg == null) {
            result = null;
        } else {
            Date now = new Date();
            if (!commitPkg.hasPermission("Write")) {
                logger.error("No permissions to write to commit directory: " + commitPkg);
                result = null;
            } else {
                EmsScriptNode currCommit = commitPkg.createNode("commit_" + now.getTime(), "cm:content");
                currCommit.createOrUpdateAspect( "cm:titled");
                if (msg != null) currCommit.createOrUpdateProperty("cm:description", msg);
    
                currCommit.createOrUpdateAspect( "ems:Committable" );
                if (type != null) {
                    currCommit.createOrUpdateProperty( "ems:commitType", type );
                } else {
                    // TODO throw exception
                }
                if (body != null) currCommit.createOrUpdateProperty( "ems:commit", body );
                
                if (prevCommit1 != null) {
                    updateCommitHistory(prevCommit1, currCommit, originalUser);
                }
                if (prevCommit2 != null) {
                    updateCommitHistory(prevCommit2, currCommit, originalUser);
                }
    
                currCommit.setOwner( originalUser );
                currCommit.getOrSetCachedVersion();
                result = currCommit.getNodeRef();
            }
        }
        
        // make sure we're running back as the originalUser
        AuthenticationUtil.setRunAsUser( originalUser );
        return result;
	}


    /**
     * Send off the deltas to various endpoints
     * @param deltas    JSONObject of the deltas to be published
     * @param projectId String of the project Id to post to
     * @param source    Source of the delta (e.g., MD, EVM, whatever, only necessary for MD so it can ignore)
     * @return          true if publish completed
     * @throws JSONException
     */
    public static boolean sendDeltas(JSONObject deltaJson, String workspaceId, String projectId, String source) throws JSONException {
        boolean jmsStatus = false;
        boolean restStatus = false;

        if (source != null) {
            deltaJson.put( "source", source );
        }
        if (jmsConnection != null) {
            jmsConnection.setWorkspace( workspaceId );
            jmsConnection.setProjectId( projectId );
            jmsStatus = jmsConnection.publish( deltaJson );
        }
        if (restConnection != null) {
            try {
                restStatus = restConnection.publish( deltaJson, new HostnameGet().getAlfrescoHost());
            } catch (Exception e) {
                logger.warn("REST connection not available");
                return false;
            }
        }

        return jmsStatus && restStatus;
    }
    
    /**
     * Send off progress to various endpoints
     * @param msg       String message to be published
     * @param projectId String of the project Id to post to
     * @return          true if publish completed
     * @throws JSONException
     */
    public static boolean sendProgress(String msg, String workspaceId, String projectId) {
        // FIXME: temporarily remove progress notifications until it's actually ready to be used
//        boolean jmsStatus = false;
//
//        if (jmsConnection != null) {
//            jmsConnection.setWorkspace( workspaceId );
//            jmsConnection.setProjectId( projectId );
//            jmsStatus = jmsConnection.publishTopic( msg, "progress" );
//        }
//
//        return jmsStatus;
        return true;
    }
}
