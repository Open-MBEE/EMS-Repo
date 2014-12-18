package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.CommitActionExecuter;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.connections.RestPostConnection;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Calendar;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import junit.framework.Assert;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
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
        }
        
        // Create the date folders if needed.  Want to return the "commit" folder
        // if create is false:
        if (create) {
            commitPkg = NodeUtil.getOrCreateDateFolder( commitPkg );
        }

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
	    ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();
	    
	    // Note: if workspace is null, then will get the master workspace commits
	    EmsScriptNode commitPkg = getCommitPkg(workspace, services, response);

	    if (commitPkg != null) {
            commits.addAll(WebScriptUtil.getAllNodesInPath(commitPkg.getQnamePath(),
                                                           "TYPE",
                                                           "cm:content",
                                                           workspace,
                                                           null,
                                                           services,
                                                           response));

            Collections.sort( commits, new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator() );
	    }

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
                        commits.addAll(WebScriptUtil.getAllNodesInPath(dayFolder.getQnamePath(),
                                                                       "TYPE",
                                                                       "cm:content",
                                                                       ws,
                                                                       null,
                                                                       services,
                                                                       response));

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
                            commits.addAll(WebScriptUtil.getAllNodesInPath(dayFolder.getQnamePath(),
                                                                           "TYPE",
                                                                           "cm:content",
                                                                           workspace,
                                                                           null,
                                                                           services,
                                                                           response));
                            
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

	public static ArrayList<EmsScriptNode> getCommitsInDateTimeRange( Date fromDateTime,
	                                                                  Date toDateTime,
	                                                                  WorkspaceNode workspace,
	                                                                  ServiceRegistry services,
	                                                                  StringBuffer response) {
	    
	    // TODO REVIEW consider using date folders to narrow the range of commits to be parsed
	    //             through, rather than using getLastCommit().  
	    
	    // skip over too new workspaces
	    while ( workspace != null ) {
	        Date created = workspace.getCreationDate();
	        if ( created.after( toDateTime ) ) {
	            workspace = workspace.getParentWorkspace();
	        } else {
	            break;
	        }
	    }
	    // gather commits between dates while walking up workspace parents
	    ArrayList<EmsScriptNode> commits = new ArrayList< EmsScriptNode >();
        while ( true ) { // run until workspace is equal to null and once while it is null (for the master branch)
            EmsScriptNode commit = getLastCommit( workspace, services, response );
            while ( commit != null ) {
                Date created = commit.getCreationDate();
                if ( created.before( fromDateTime ) ) break;
                if ( !created.after( toDateTime ) ) {
                    commits.add( commit );
                }
                commit = getPreviousCommit(commit);
            }
            if ( workspace == null ) break;
            workspace = workspace.getParentWorkspace();
        }
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

    public static NodeRef commit(JSONObject wsDiff,
                       WorkspaceNode workspace,
                       String msg,
                       boolean runWithoutTransactions,
                       ServiceRegistry services,
                       StringBuffer response) {
        NodeRef commitRef = null;
        if (runWithoutTransactions) {
            try {
                commitRef = commitTransactionable(wsDiff, workspace, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                commitRef = commitTransactionable(wsDiff, workspace, msg, services, response);
                trx.commit();
            } catch (Throwable e) {
                try {
                    e.printStackTrace();
                    trx.rollback();
                } catch (Throwable ee) {
                    ee.printStackTrace();
                }
            }
        }
        return commitRef;
	}
    

	private static NodeRef commitTransactionable( JSONObject wsDiff,
	                                           WorkspaceNode workspace,
	                                           String msg,
	                                           ServiceRegistry services,
	                                           StringBuffer response) throws JSONException {

	    String body = null;
	    if (wsDiff != null) {
	        body = wsDiff.toString();
	    }
	    return createCommitNode( workspace, null, workspace, null, null, "COMMIT", msg,
	                             body, services, response );
    }


    public static NodeRef merge(JSONObject wsDiff,
                             WorkspaceNode source,
                             WorkspaceNode target,
                             Date dateTimeSrc,
                             Date dateTimeTarget,
                             String msg,
                             boolean runWithoutTransactions,
                             ServiceRegistry services,
                             StringBuffer response) {
        NodeRef mergeRef = null;
        if (runWithoutTransactions) {
            mergeRef = mergeTransactionable(wsDiff, source, target, target, dateTimeSrc, dateTimeTarget,
                                            msg, services, response);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                mergeRef = mergeTransactionable(wsDiff, source, target, target, dateTimeSrc, dateTimeTarget,
                                                msg, services, response);
                trx.commit();
            } catch (Throwable e) {
                try {
                    e.printStackTrace();
                    trx.rollback();
                } catch (Throwable ee) {
                    ee.printStackTrace();
                }
            }
        }
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
	
	public static NodeRef branch(WorkspaceNode srcWs, WorkspaceNode dstWs,
	                             String msg,
	                             boolean runWithoutTransactions,
	                             ServiceRegistry services, StringBuffer response) {
	    NodeRef branchRef = null;
        if (runWithoutTransactions) {
            try {
                branchRef = branchTransactionable(srcWs, dstWs, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                branchRef = branchTransactionable(srcWs, dstWs, msg, services, response);
                trx.commit();
            } catch (Throwable e) {
                try {
                    e.printStackTrace();
                    trx.rollback();
                } catch (Throwable ee) {
                    ee.printStackTrace();
                }
            }
        }
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
    public static void merge(WorkspaceNode srcWs, WorkspaceNode dstWs,
                             String msg,
                             boolean runWithoutTransactions,
                             ServiceRegistry services, StringBuffer response) {
        if (runWithoutTransactions) {
            try {
                mergeTransactionable(srcWs, dstWs, dstWs, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                mergeTransactionable(srcWs, dstWs, dstWs, msg, services, response);
                trx.commit();
            } catch (Throwable e) {
                try {
                    e.printStackTrace();
                    trx.rollback();
                } catch (Throwable ee) {
                    ee.printStackTrace();
                }
            }
        }
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
	                                             EmsScriptNode currCommit) {
	    if (prevCommit == null || currCommit == null) {
	        return false;
	    } else {
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
	protected static NodeRef createCommitNode(WorkspaceNode srcWs1, WorkspaceNode srcWs2,
	                                          WorkspaceNode dstWs,
	                                          Date dateTime1,
                                              Date dateTime2,
	                                          String type, String msg, String body,
	                                          ServiceRegistry services, StringBuffer response,
	                                          boolean twoSourceWorkspaces) {
	    
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
            return null;
        } else {
            Date now = new Date();
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
                updateCommitHistory(prevCommit1, currCommit);
            }            
            if (prevCommit2 != null) {
                updateCommitHistory(prevCommit2, currCommit);
            }
            
            return currCommit.getNodeRef();
        }
	}
	
	
	/**
	 * Update commit node reference with final body 
	 * @param commitRef
	 * @param body
	 * @param msg
	 * @param services
	 * @param response
	 */
	public static void updateCommitNodeRef(NodeRef commitRef, String body, String msg, ServiceRegistry services, StringBuffer response) {
	    EmsScriptNode commitNode = new EmsScriptNode(commitRef, services, response);
	    // FIXME: make commitNode read only after updating it, so it can no longer be updated
	    if (commitNode != null && commitNode.exists()) {
        	    if (msg != null) {
        	        commitNode.createOrUpdateProperty("cm:description", msg );
        	    }
        	    commitNode.createOrUpdateProperty( "ems:commit", body );
	    } else {
	        logger.error("CommitNode doesn't exist for updating, dumping out content");
	        logger.error(body);
	    }
	}

	
    /**
     * Send off the deltas to various endpoints
     * @param deltas    JSONObject of the deltas to be published
     * @return          true if publish completed
     * @throws JSONException
     */
    public static boolean sendDeltas(JSONObject deltaJson, String workspaceId, String projectId) throws JSONException {
        boolean jmsStatus = false;
        boolean restStatus = false;

        if (jmsConnection != null) {
            jmsConnection.setWorkspace( workspaceId );
            jmsConnection.setProjectId( projectId );
            jmsStatus = jmsConnection.publish( deltaJson, workspaceId );
        }
        if (restConnection != null) {
            try {
                restStatus = restConnection.publish( deltaJson, "MMS" );
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return jmsStatus && restStatus;
    }


    /**
     * 
     * @param targetWS
     * @param start
     * @param end
     * @param projectId
     * @param status
     * @throws Exception
     */
    public static void commitAndStartAction( WorkspaceNode targetWS,
                                             WorkspaceDiff wsDiff,
                                             long start, long end,
                                             String projectId,
                                             Status status ) throws Exception {
        if (false == wsDiff.isDiff()) {
            return;
        }

        String wsId = "master";
        if (targetWS != null) {
            wsId = targetWS.getId();
        }

        // Commit history
        ActionService actionService = services.getActionService();
        Action commitAction = actionService.createAction(CommitActionExecuter.NAME);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_PROJECT_ID, projectId);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_WS_ID, wsId);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_WS_DIFF, wsDiff);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_START, start);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_END, end);
        
        // create empty commit for now (executing action will fill it in later)
        NodeRef commitRef = CommitUtil.commit(null, targetWS, "", false, services, new StringBuffer() );

        services.getActionService().executeAction(commitAction , commitRef, true, true);
    }
}
