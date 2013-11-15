package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.extensions.webscripts.DeclarativeWebScript;

/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities that
 * were in utils.js
 * 
 * @author cinyoung
 * 
 */
public abstract class AbstractWebScript extends DeclarativeWebScript {
	// injected members
	protected ServiceRegistry services;
	protected Repository repository;

	// internal members
	protected NodeService nodeService;
	protected ScriptNode companyhome;
	
	@SuppressWarnings("serial")
	protected final Map<String, String> typeMap = new HashMap<String,String>() { 
		{
			put("View", "view:View");
			put("Property", "view:Property");
			put("Comment", "view:Comment");
			put("ModelElement", "view:ModelElement");
		}
	};

	protected void initMemberVariables(String siteName) {
		this.nodeService = services.getNodeService();
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}
	
	protected void updateViewHierarchy(Map<String, ScriptNode> modelMapping,
			JSONArray views) throws JSONException {
		updateViewHierarchy(modelMapping, views, null);
	}
	
	protected void updateViewHierarchy(Map<String, ScriptNode> modelMapping,
			JSONArray views, List<String> nosections) throws JSONException {
		for (int pview = 0; pview < views.length(); pview++) {
			JSONArray cviews = views.getJSONArray(pview);
			ScriptNode pviewnode = modelMapping.get(pview);
			if (pviewnode == null) {
				continue;
			}
			JSONArray oldchildren = (JSONArray)pviewnode.getAssocs().get("view:views");
			for (int ii = 0; ii < oldchildren.length(); ii++) {
				pviewnode.removeAssociation((ScriptNode)oldchildren.get(ii), "view:views");
			}
			for (int ci = 0; ci < cviews.length(); ci++) {
				String cvid = cviews.getString(ci);
				ScriptNode cviewnode = modelMapping.get(cvid);
				if (cviewnode == null) {
					continue;
				}
				cviewnode.getProperties().put("view:index", ci);
				cviewnode.save();
				pviewnode.createAssociation(cviewnode, "view:views");
			}
			pviewnode.getProperties().put("view:viewsJson", cviews.toString());
			if (nosections != null && nosections.contains(pview)) {
				pviewnode.getProperties().put("view:noSection", true);
			}
			pviewnode.save();
		}
	}

	/**
	 * Clear all parent volume associations
	 * @param dnode
	 */
	protected void cleanDocument(ScriptNode dnode) {
		// TODO implement cleanDocument
		dnode.getSourceAssocs().get("view:documents");
	}
	
	protected void setName(ScriptNode node, String name) {
		node.getProperties().put("view:name", name);
		node.getProperties().put("cm:title", name);
	}
	
	protected ScriptNode getModelElement(ScriptNode parent, String name) {
		return parent.childByNamePath(name);
	}
	
	protected ScriptNode createModelElement(ScriptNode parent, String name, String type) {
		return parent.createNode(name, type);
	}
}
