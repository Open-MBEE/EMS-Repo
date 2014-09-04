package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.transaction.UserTransaction;

import junit.framework.Assert;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.JSONObject;

public class CommitUtil {

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
//        if ( Utils.isNullOrEmpty( siteName ) ) {
//            String userName = NodeUtil.getUserName();
//            List< SiteInfo > sites = services.getSiteService().listSites( userName );
//            for ( SiteInfo si : sites ) {
//                String aSiteName = si.getShortName();
//                ArrayList< EmsScriptNode > siteCommits = getCommits( workspace, aSiteName, services, response );
//                commits.addAll( siteCommits );
//            }
//            return commits;
//        }
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
                Date created = workspace.getCreationDate();
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

    public static void commit(JSONObject wsDiff,
                       WorkspaceNode workspace,
                       String siteName,
                       String msg,
                       boolean runWithoutTransactions,
                       ServiceRegistry services,
                       StringBuffer response) {
        if (runWithoutTransactions) {
            try {
                commitTransactionable(wsDiff, workspace, siteName, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                commitTransactionable(wsDiff, workspace, siteName, msg, services, response);
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


	private static void commitTransactionable( JSONObject wsDiff,
	                                           WorkspaceNode workspace,
	                                           String siteName,
	                                           String msg,
	                                           ServiceRegistry services,
	                                           StringBuffer response) throws JSONException {
	    createCommitNode( workspace, workspace, "COMMIT", msg,
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

	public static void branch(WorkspaceNode srcWs, WorkspaceNode dstWs,
	                          String siteName, String msg,
	                          boolean runWithoutTransactions,
	                          ServiceRegistry services, StringBuffer response) {
        if (runWithoutTransactions) {
            try {
                branchTransactionable(srcWs, dstWs, siteName, msg, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                branchTransactionable(srcWs, dstWs, siteName, msg, services, response);
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

    private static void branchTransactionable( WorkspaceNode srcWs,
                                               WorkspaceNode dstWs,
                                               String siteName, String msg,
                                               ServiceRegistry services,
                                               StringBuffer response )
                                                       throws JSONException {
	    createCommitNode(srcWs, dstWs, "BRANCH", msg, "{}", siteName, services, response);
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
	 * Create a commit node specifying the workspaces
	 */
	protected static boolean createCommitNode(WorkspaceNode srcWs, WorkspaceNode dstWs,
	                                   String type, String msg, String body, String siteName,
	                                   ServiceRegistry services, StringBuffer response) {
        EmsScriptNode commitPkg = getOrCreateCommitPkg( dstWs, siteName, services, response, true );

        if (commitPkg == null) {
            return false;
        } else {
            // get the most recent commit before creating a new one
            EmsScriptNode prevCommit = getLastCommit( srcWs, siteName, services, response );

            Date now = new Date();
            EmsScriptNode currCommit = commitPkg.createNode("commit_" + now.getTime(), "cm:content");
            currCommit.createOrUpdateAspect( "cm:titled");
            currCommit.createOrUpdateProperty("cm:description", msg);

            currCommit.createOrUpdateAspect( "ems:Committable" );
            currCommit.createOrUpdateProperty( "ems:commitType", type );
            currCommit.createOrUpdateProperty( "ems:commit", body );

            updateCommitHistory(prevCommit, currCommit);

            return true;
        }
	}
}
