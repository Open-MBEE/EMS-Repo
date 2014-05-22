package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.ConfigurationGet.EmsScriptNodeCreatedAscendingComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CommitUtil {
	private static final String VERSION_KEY = "version";
	private static final String ID_KEY = "nodeid";
	private static final String COMMIT_KEY = "commits";

	private CommitUtil() {
		// prevent instantiation
	}

	public static List<EmsScriptNode> getChangeSets(EmsScriptNode context,
			ServiceRegistry services, StringBuffer response) {
		List<EmsScriptNode> changeSets = new ArrayList<EmsScriptNode>();
		if (context != null) {
			changeSets.addAll(WebScriptUtil.getAllNodesInPath(
					context.getQnamePath(), "TYPE", "cm:content", services,
					response));
			Collections.sort(changeSets,
					new EmsScriptNodeCreatedAscendingComparator());
		}

		return changeSets;
	}

	public static JSONObject getJsonChangeSets(EmsScriptNode context,
			ServiceRegistry services, StringBuffer response)
			throws JSONException {
		EmsScriptNode commitPkg = context.childByNamePath("Commits");
		List<EmsScriptNode> changeSets = getChangeSets(commitPkg, services,
				response);
		
		JSONObject jsonChangeSets = new JSONObject();
		JSONArray array = new JSONArray();
		for (EmsScriptNode changeSet : changeSets) {
			JSONObject json = new JSONObject();
			json.put("nodeid", changeSet.getId());
			json.put("modifier", changeSet.getProperty("cm:creator"));
			json.put("timestamp", changeSet.getProperty("cm:modified"));
			json.put("message", changeSet.getProperty("cm:description"));
			array.put(json);
		}
		jsonChangeSets.put("changeSets", array);

		return jsonChangeSets;
	}

	public static void commitChangeSet(Set<Version> changeSet, String message,
			boolean runWithoutTransactions, ServiceRegistry services) {
		if (runWithoutTransactions) {
			try {
				commitTransactionableChangeSet(changeSet, message, services);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			UserTransaction trx;
			trx = services.getTransactionService()
					.getNonPropagatingUserTransaction();
			try {
				trx.begin();
				commitTransactionableChangeSet(changeSet, message, services);
				trx.commit();
			} catch (Throwable e) {
				try {
					// log(LogLevel.ERROR,
					// "commitChangeSet: DB transaction failed: " +
					// e.getMessage(),
					// HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					e.printStackTrace();
					trx.rollback();
				} catch (Throwable ee) {
					// log(LogLevel.ERROR, "commitChangeSet: rollback failed: "
					// + ee.getMessage());
					ee.printStackTrace();
				}
			}
		}
	}

	/**
	 * Make a commit change set that can be reverted
	 * 
	 * @throws JSONException
	 */
	public static void commitTransactionableChangeSet(Set<Version> changeSet,
			String message, ServiceRegistry services) throws JSONException {
		JSONObject changeJson = new JSONObject();
		JSONArray changeArray = new JSONArray();
		EmsScriptNode commitPkg = null;
		for (Version version : changeSet) {
			EmsScriptNode changedNode = new EmsScriptNode(
					version.getVersionedNodeRef(), services);
			// create the commit directory as necessary
			EmsScriptNode siteNode = changedNode.getSiteNode();
			commitPkg = siteNode.childByNamePath("Commits");
			if (commitPkg == null) {
				commitPkg = siteNode.createFolder("Commits", "cm:folder");
			}

			JSONObject json = new JSONObject();
			json.put(ID_KEY, changedNode.getId());
			json.put(VERSION_KEY, version.getVersionLabel());
			changeArray.put(json);
		}

		changeJson.put(COMMIT_KEY, changeArray);
		// create the node with information
		Date now = new Date();
		EmsScriptNode commitNode = commitPkg.createNode("commit_"
				+ now.getTime(), "cm:content");
		// so we can add the commit message
		commitNode.addAspect("cm:titled");
		commitNode.setProperty("cm:description", message);

		ActionUtil.saveStringToFile(commitNode, "application/json", services, changeJson.toString());
	}

	/**
	 * @param changeSet
	 * @param services
	 * @return
	 * @throws JSONException
	 */
	public static boolean revertCommit(EmsScriptNode changeSet,
			ServiceRegistry services) {
		boolean status = true;

        ContentReader reader = services.getContentService().getReader(changeSet.getNodeRef(), ContentModel.PROP_CONTENT);
        String content = reader.getContentString();

		try {
			JSONObject changeJson = new JSONObject(content);
	
			JSONArray changeArray = changeJson.getJSONArray(COMMIT_KEY);
			for (int ii = 0; ii < changeArray.length(); ii++) {
				JSONObject json = changeArray.getJSONObject(ii);
				EmsScriptNode node = getScriptNodeByNodeRefId(json.getString(ID_KEY), services);
				if (node != null) {
					String versionKey = json.getString(VERSION_KEY);
					node.revert("reverting to version " + versionKey
							+ " as part of commit " + changeSet.getName(), false,
							versionKey);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			status = false;
		}

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
