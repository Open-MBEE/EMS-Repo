package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.CommitActionExecuter;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.connections.RestPostConnection;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.transaction.UserTransaction;

import junit.framework.Assert;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
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
	 * @param siteName
	 * @param services
	 * @param response
	 * @param create
	 * @return
	 */
    private static EmsScriptNode getOrCreateCommitPkg( WorkspaceNode workspace,
                                                       String siteName,
                                                       ServiceRegistry services,
                                                       StringBuffer response,
                                                       boolean create ) {
        EmsScriptNode context = null;

        if (workspace == null) {
            SiteInfo siteInfo = services.getSiteService().getSite( siteName );
            context = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
        } else {
            context = workspace;
        }

        EmsScriptNode commitPkg = context.childByNamePath( "commits" );

        if (commitPkg == null && create) {
            commitPkg = context.createFolder( "commits" );
        }
        
        commitPkg = NodeUtil.getOrCreateDateFolder( commitPkg );

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
	                                         String siteName,
	                                         ServiceRegistry services,
	                                         StringBuffer response) {
	    return getOrCreateCommitPkg( workspace, siteName, services, response,
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
	                                           String siteName,
	                                           ServiceRegistry services,
	                                           StringBuffer response) {
	    ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();
	    if (workspace == null && siteName == null) {
	        return commits;
	    }
	    EmsScriptNode commitPkg = getCommitPkg(workspace, siteName, services, response);

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
     * Given a workspace gets an ordered list of the commit history in a time
     * range.
     * 
     * @param fromDateTime
     * @param toDateTime
     * @param workspace
     * @param services
     * @param response
     * @param justFirst
     * @return
     */
    public static ArrayList<EmsScriptNode> getCommitsAllSitesInDateTimeRange(Date fromDateTime,
                                                                             Date toDateTime,
                                                                             WorkspaceNode workspace,
                                                                             ServiceRegistry services,
                                                                             StringBuffer response,
                                                                             boolean justFirst) {
        ArrayList< EmsScriptNode > commits = new ArrayList< EmsScriptNode >();
        String userName = NodeUtil.getUserName();
        List< SiteInfo > sites = services.getSiteService().listSites( userName );
        Set<String> siteNames = new TreeSet<String>();
        for ( SiteInfo si : sites ) {
            String aSiteName = si.getShortName();
            siteNames.add( aSiteName );
        }
        siteNames.add(AbstractJavaWebScript.NO_SITE_ID);
        System.out.println( siteNames.size() + " sites" );
        for ( String siteName : siteNames ) {
            ArrayList< EmsScriptNode > siteCommits =
                    getCommitsInDateTimeRange( fromDateTime, toDateTime, workspace, siteName, services, response, justFirst );

            //System.out.println( "commits in " + siteName + " = " + siteCommits );

            commits.addAll( siteCommits );
            if ( justFirst && commits.size() > 0 ) break;
        }
        return commits;
    }

    /**
     * Given a workspace gets an ordered list of the commit history
     * @param workspace
     * @param services
     * @param response
     * @return
     */
    public static ArrayList<EmsScriptNode> getCommitsAllSites(WorkspaceNode workspace,
                                                              ServiceRegistry services,
                                                              StringBuffer response) {
        Assert.assertFalse( true ); // TODO -- this does not preserve order and probably gets duplicates
        ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();
//        if ( Utils.isNullOrEmpty( siteName ) ) {
            String userName = NodeUtil.getUserName();
            List< SiteInfo > sites = services.getSiteService().listSites( userName );
            for ( SiteInfo si : sites ) {
                String aSiteName = si.getShortName();
                ArrayList< EmsScriptNode > siteCommits = getCommits( workspace, aSiteName, services, response );
                commits.addAll( siteCommits );
            }
            return commits;
//        }
    }

	/**
	 * Get the most recent commit in a workspace
	 * @param ws
	 * @param siteName
	 * @param services
	 * @param response
	 * @return
	 */
	public static EmsScriptNode getLastCommit(WorkspaceNode ws, String siteName,
	                                          ServiceRegistry services,
	                                          StringBuffer response) {
	    ArrayList<EmsScriptNode> commits = getCommits(ws, siteName, services, response);

	    if (commits.size() > 0) {
	        return commits.get( 0 );
	    }

	    return null;
	}

	public static ArrayList<EmsScriptNode> getCommitsInDateTimeRange( Date fromDateTime,
	                                                                  Date toDateTime,
	                                                                  WorkspaceNode workspace,
	                                                                  String siteName,
	                                                                  ServiceRegistry services,
	                                                                  StringBuffer response,
	                                                                  boolean justFirst ) {
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
            EmsScriptNode commit = getLastCommit( workspace, siteName, services, response );
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
                       String siteName,
                       String msg,
                       boolean runWithoutTransactions,
                       ServiceRegistry services,
                       StringBuffer response) {
        NodeRef commitRef = null;
        if (runWithoutTransactions) {
            try {
                commitRef = commitTransactionable(wsDiff, workspace, siteName, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                commitRef = commitTransactionable(wsDiff, workspace, siteName, msg, services, response);
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
	                                           String siteName,
	                                           String msg,
	                                           ServiceRegistry services,
	                                           StringBuffer response) throws JSONException {
	    String body = null;
	    if (wsDiff != null) {
	        body = wsDiff.toString();
	    }
	    return createCommitNode( workspace, workspace, "COMMIT", msg,
	                      body, siteName,
	                      services, response );
    }


    public static NodeRef merge(JSONObject wsDiff,
                             WorkspaceNode source,
                             WorkspaceNode target,
                             String siteName,
                             String msg,
                             boolean runWithoutTransactions,
                             ServiceRegistry services,
                             StringBuffer response) {
        NodeRef mergeRef = null;
        if (runWithoutTransactions) {
            mergeRef = mergeTransactionable(wsDiff, source, target, siteName, msg, services, response);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                mergeRef = mergeTransactionable(wsDiff, source, target, siteName, msg, services, response);
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
                                              WorkspaceNode source,
                                              WorkspaceNode target,
                                              String siteName, String msg,
                                              ServiceRegistry services,
                                              StringBuffer response ) {
        return createCommitNode( source, target, "MERGE", msg,
                          wsDiff.toString(), siteName,
                          services, response );
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
	                          String siteName, String msg,
	                          boolean runWithoutTransactions,
	                          ServiceRegistry services, StringBuffer response) {
	    NodeRef branchRef = null;
        if (runWithoutTransactions) {
            try {
                branchRef = branchTransactionable(srcWs, dstWs, siteName, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                branchRef = branchTransactionable(srcWs, dstWs, siteName, msg, services, response);
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
                                               String siteName, String msg,
                                               ServiceRegistry services,
                                               StringBuffer response )
                                                       throws JSONException {
	    return createCommitNode(srcWs, dstWs, "BRANCH", msg, "{}", siteName, services, response);
	}

    // TODO -- REVIEW -- Just copied branch and search/replaced "branch" with "merge"
    @Deprecated
    public static void merge(WorkspaceNode srcWs, WorkspaceNode dstWs,
                              String siteName, String msg,
                              boolean runWithoutTransactions,
                              ServiceRegistry services, StringBuffer response) {
        if (runWithoutTransactions) {
            try {
                mergeTransactionable(srcWs, dstWs, siteName, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                mergeTransactionable(srcWs, dstWs, siteName, msg, services, response);
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

    private static void mergeTransactionable( WorkspaceNode srcWs,
                                               WorkspaceNode dstWs,
                                               String siteName, String msg,
                                               ServiceRegistry services,
                                               StringBuffer response )
                                                       throws JSONException {
        createCommitNode(srcWs, dstWs, "MERGE", msg, "{}", siteName, services, response);
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
            parentRefs.add( prevCommit.getNodeRef() );
            currCommit.setProperty( "ems:commitParents", parentRefs );

//            ArrayList< NodeRef > childRefs = prevCommit.getPropertyNodeRefs( "ems:commitChildren" );
            @SuppressWarnings( "unchecked" )
            ArrayList< NodeRef > childRefs =
                    (ArrayList< NodeRef >)prevCommit.getProperties().get( "{http://jpl.nasa.gov/model/ems/1.0}commitChildren" );
            if ( childRefs == null ) {
                childRefs = new ArrayList< NodeRef >();
            }
            childRefs.add( currCommit.getNodeRef() );
            prevCommit.setProperty( "ems:commitChildren", childRefs );
	    }
        return true;
	}


	/**
	 * Create a commit node specifying the workspaces. Typically, since the serialization takes
	 * a while, the commit node is created first, then it is updated in the background using the
	 * ActionExecuter.
	 */
	protected static NodeRef createCommitNode(WorkspaceNode srcWs, WorkspaceNode dstWs,
	                                   String type, String msg, String body, String siteName,
	                                   ServiceRegistry services, StringBuffer response) {
        EmsScriptNode commitPkg = getOrCreateCommitPkg( dstWs, siteName, services, response, true );

        if (commitPkg == null) {
            return null;
        } else {
            // get the most recent commit before creating a new one
            EmsScriptNode prevCommit = getLastCommit( srcWs, siteName, services, response );

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
            
            updateCommitHistory(prevCommit, currCommit);

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

        return jmsStatus && restStatus ? true : false;
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
                                             EmsScriptNode projectNode,
                                             Status status ) throws Exception {
        if (false == wsDiff.isDiff()) {
            return;
        }

        String wsId = "master";
        if (targetWS != null) {
            wsId = targetWS.getId();
        }

        // Commit history
        String siteName = null;
        if (projectNode != null) {
            // Note: not use siteNode here, in case its incorrect.
            EmsScriptNode siteNodeProject = projectNode.getSiteNode();
            siteName = siteNodeProject.getName();
        }

        ActionService actionService = services.getActionService();
        Action commitAction = actionService.createAction(CommitActionExecuter.NAME);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_PROJECT_ID, projectId);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_WS_ID, wsId);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_WS_DIFF, wsDiff);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_START, start);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_END, end);
        commitAction.setParameterValue(CommitActionExecuter.PARAM_SITE_NAME, siteName);
        
        // create empty commit for now (executing action will fill it in later)
        NodeRef commitRef = CommitUtil.commit(null, targetWS, siteName, "", false, services, new StringBuffer() );

        services.getActionService().executeAction(commitAction , commitRef, true, true);
    }
}
