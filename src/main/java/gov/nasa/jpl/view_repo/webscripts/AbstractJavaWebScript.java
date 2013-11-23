package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.DeclarativeWebScript;

/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities
 * that were in utils.js
 * 
 * @author cinyoung
 * 
 */
public abstract class AbstractJavaWebScript extends DeclarativeWebScript {
	// injected members
	protected ServiceRegistry services;
	protected Repository repository;
	protected JwsUtil jwsUtil;

	// internal members
	protected ScriptNode companyhome;

	protected final Map<String, String> typeMap = new HashMap<String, String>() {
		private static final long serialVersionUID = -5467934440503910163L;
		{
			put("View", "view:View");
			put("Property", "view:Property");
			put("Comment", "view:Comment");
			put("ModelElement", "view:ModelElement");
		}
	};

	protected void initMemberVariables(String siteName) {
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}

	public void setJwsUtil(JwsUtil util) {
		jwsUtil = util;
	}

	protected void updateViewHierarchy(Map<String, ScriptNode> modelMapping,
			JSONObject views) throws JSONException {
		updateViewHierarchy(modelMapping, views, null);
	}

	protected void updateViewHierarchy(Map<String, ScriptNode> modelMapping,
			JSONObject views, List<String> nosections) throws JSONException {
		Iterator<?> keys = views.keys();
		while (keys.hasNext()) {
			String pview = (String) keys.next();
			JSONArray cviews = views.getJSONArray(pview);
			if (!modelMapping.containsKey(pview)) {
				continue;
			}
			ScriptNode pviewnode = modelMapping.get(pview);
			JSONArray oldchildren = (JSONArray) pviewnode.getAssocs().get(
					"view:views");
			if (oldchildren == null) {
				continue;
			}
			for (int ii = 0; ii < oldchildren.length(); ii++) {
				pviewnode.removeAssociation((ScriptNode) oldchildren.get(ii),
						"view:views");
			}
			for (int ci = 0; ci < cviews.length(); ci++) {
				String cvid = cviews.getString(ci);
				ScriptNode cviewnode = modelMapping.get(cvid);
				if (cviewnode == null) {
					continue;
				}
				jwsUtil.setNodeProperty(cviewnode, "view:index", ci);
				pviewnode.createAssociation(cviewnode, "view:views");
			}
			jwsUtil.setNodeProperty(pviewnode, "view:viewsJson",
					cviews.toString());
			if (nosections != null && nosections.contains(pview)) {
				jwsUtil.setNodeProperty(pviewnode, "view:noSection", true);
			}
		}
	}

	/**
	 * Clear all parent volume associations
	 * 
	 * @param dnode
	 */
	protected void cleanDocument(ScriptNode dnode) {
		JSONArray pvs = (JSONArray) dnode.getSourceAssocs().get(
				"view:documents");
		if (pvs != null) {
			for (int ii = 0; ii < pvs.length(); ii++) {
				// TODO: convert pv to ScriptNode to remove?
			}
		}
	}


}
