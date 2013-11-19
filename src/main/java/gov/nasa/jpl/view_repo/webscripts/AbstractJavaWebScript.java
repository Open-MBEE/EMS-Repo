package gov.nasa.jpl.view_repo.webscripts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;

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

	// internal members
	protected NodeService nodeService;
	protected ScriptNode companyhome;
	protected final static String NAMESPACE_BEGIN = "" + QName.NAMESPACE_BEGIN;

	protected final static boolean USE_FOUNDATIONAL = true;

	@SuppressWarnings("serial")
	protected final Map<String, String> typeMap = new HashMap<String, String>() {
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
				setNodeProperty(cviewnode, "view:index", ci);
				pviewnode.createAssociation(cviewnode, "view:views");
			}
			setNodeProperty(pviewnode, "view:viewsJson", cviews.toString());
			if (nosections != null && nosections.contains(pview)) {
				setNodeProperty(pviewnode, "view:noSection", true);
			}
		}
	}

	/**
	 * Clear all parent volume associations
	 * 
	 * @param dnode
	 */
	protected void cleanDocument(ScriptNode dnode) {
		JSONArray pvs = (JSONArray) dnode.getSourceAssocs().get("view:documents");
		if (pvs != null) {
			for (int ii = 0; ii < pvs.length(); ii++) {
				// TODO: convert pv to ScriptNode to remove?
			}
		}
	}

	protected void setName(ScriptNode node, String name) {
		setNodeProperty(node, "view:name", name);
		setNodeProperty(node, "cm:title", name);
	}

	/**
	 * Wrapper function to switch between different ways to set a NodeRefs
	 * property (foundational API or ScriptNode, latter has some odd null property
	 * issues at the moment) 
	 * 
	 * @param node		Node to set property for
	 * @param type		Short type for the property to set (e.g., "view:mdid")
	 * @param value		Serializable value to set the property to
	 */
	protected void setNodeProperty(ScriptNode node, String type,
			Serializable value) {
		if (USE_FOUNDATIONAL) {
			nodeService
					.setProperty(node.getNodeRef(), createQName(type), value);
		} else {
			node.getProperties().put(type, value);
			node.save();
		}
	}

	protected Object getNodeProperty(ScriptNode node, String key) {
		if (USE_FOUNDATIONAL) {
			return nodeService.getProperty(node.getNodeRef(), createQName(key));
		} else {
			return node.getProperties().get(key);
		}
	}

	protected ScriptNode getModelElement(ScriptNode parent, String name) {
		return parent.childByNamePath(name);
	}

	protected ScriptNode createModelElement(ScriptNode parent, String name,
			String type) {
		if (USE_FOUNDATIONAL) {
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(
					1, 1.0f);
			props.put(ContentModel.PROP_NAME, name);

			ChildAssociationRef assoc = nodeService.createNode(parent
					.getNodeRef(), ContentModel.ASSOC_CONTAINS, QName
					.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
							QName.createValidLocalName(name)),
					createQName(type), props);
			return new ScriptNode(assoc.getChildRef(), services);
		} else {
			return parent.createNode(name, type);
		}
	}

	/**
	 * Helper to create a QName from either a fully qualified or short-name
	 * QName string (taken from ScriptNode)
	 * 
	 * @param s
	 *            Fully qualified or short-name QName string
	 * 
	 * @return QName
	 */
	protected QName createQName(String s) {
		QName qname;
		if (s.indexOf(NAMESPACE_BEGIN) != -1) {
			qname = QName.createQName(s);
		} else {
			qname = QName.createQName(s, this.services.getNamespaceService());
		}
		return qname;
	}

	/**
	 * Helper utility to check the value of a request parameter
	 * @param req		WebScriptRequest with parameter to be checked
	 * @param name		String of the request parameter name to check
	 * @param value		String of the value the parameter is being checked for
	 * @return			True if parameter is equal to value, False otherwise
	 */
	protected boolean checkArgEquals(WebScriptRequest req, String name, String value) {
		if (req.getParameter(name) == null) {
			return false;
		}
		return req.getParameter(name).equals(value);
	}
}
