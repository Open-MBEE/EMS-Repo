package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.json.JSONException;

public class CommitUtil {
	public CommitUtil() {
	}

    private EmsScriptNode getOrCreateCommitPkg(WorkspaceNode workspace, String siteName, ServiceRegistry services, StringBuffer response, boolean create) {
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

	private EmsScriptNode getCommitPkg(WorkspaceNode workspace, String siteName, ServiceRegistry services, StringBuffer response) {
	    return getOrCreateCommitPkg( workspace, siteName, services, response, false );
	}


	public ArrayList<EmsScriptNode> getCommits(WorkspaceNode workspace,
	                                           String siteName,
	                                             ServiceRegistry services,
	                                             StringBuffer response) {
	    ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();
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

	public EmsScriptNode getLastCommit(WorkspaceNode ws, String siteName, ServiceRegistry services, StringBuffer response) {
	    ArrayList<EmsScriptNode> commits = getCommits(ws, siteName, services, response);

	    if (commits.size() > 0) {
	        return commits.get( 0 );
	    }

	    return null;
	}


	public void commit(Map<String, EmsScriptNode> elements,
	                   Map<String, Version> elementsVersions,
	                   Map<String, EmsScriptNode> addedElements,
	                   Map<String, EmsScriptNode> deletedElements,
	                   Map<String, EmsScriptNode> movedElements,
	                   Map<String, EmsScriptNode> updatedElements,
	                   WorkspaceNode workspace,
	                   String siteName,
	                   String message,
	                   boolean runWithoutTransactions,
	                   ServiceRegistry services,
	                   StringBuffer response
	                   ) {
	    WorkspaceDiff wsDiff = new WorkspaceDiff( workspace, workspace, null, null );

	    wsDiff.setElements( elements );
        wsDiff.setElementsVersions( elementsVersions );

	    wsDiff.setAddedElements( addedElements );
	    wsDiff.setDeletedElements( deletedElements );
	    wsDiff.setMovedElements( movedElements );
	    wsDiff.setUpdatedElements( updatedElements );

	    if (runWithoutTransactions) {
            try {
                commitTransactionable(wsDiff, workspace, siteName, message, services, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
	    } else {
            UserTransaction trx;
            trx = services.getTransactionService()
                    .getNonPropagatingUserTransaction();
            try {
                trx.begin();
                commitTransactionable(wsDiff, workspace, siteName, message, services, response);
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


	private void commitTransactionable( WorkspaceDiff wsDiff,
	                                    WorkspaceNode workspace,
	                                    String siteName,
	                                    String message,
                                        ServiceRegistry services,
                                        StringBuffer response) throws JSONException {
	    EmsScriptNode commitPkg = getOrCreateCommitPkg( workspace, siteName, services, response, true );

	    if (commitPkg == null) {
	        // TODO: means commitPkg couldn't be created
	    } else {
	        // get the most recent commit before creating a new one
            EmsScriptNode lastCommitNode = getLastCommit( workspace, siteName, services, response );

	        Date now = new Date();
	        EmsScriptNode commitNode = commitPkg.createNode("commit_" + now.getTime(), "cm:content");
	        commitNode.createOrUpdateAspect( "cm:titled");
	        commitNode.createOrUpdateProperty("cm:description", message);

	        commitNode.createOrUpdateAspect( "ems:Committable" );
	        commitNode.createOrUpdateProperty( "ems:commitType", "COMMIT" );
	        commitNode.createOrUpdateProperty( "ems:commit", wsDiff.toJSONObject( null, null, false ).toString() );

	        if (lastCommitNode != null) {
	            ArrayList< Serializable > values = new ArrayList<Serializable>();

	            values.add( lastCommitNode.getNodeRef() );
	            commitNode.setProperty( "ems:commitParent", values );

	            values = new ArrayList<Serializable>();
	            values.add( commitNode.getNodeRef() );
	            lastCommitNode.setProperty( "ems:commitChildren", values );
	        }
	    }
    }



	/**
	 */
	public boolean revertCommit(EmsScriptNode commit,
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
}
