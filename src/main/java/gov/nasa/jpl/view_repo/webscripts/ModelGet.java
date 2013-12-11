/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor in /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/model.get.desc.xml
 * @author cinyoung
 *
 */
public class ModelGet extends AbstractJavaWebScript {
	private JSONObject elementHierarchy = new JSONObject();
	private JSONObject elements = new JSONObject();
	private JSONObject relationships = new JSONObject();
	private ScriptNode projectNode = null;
	private Map<String, NodeRef> viewedElements = new HashMap<String, NodeRef>();
	private Set<String> viewedRelationships = new HashSet<String>();
	private Set<String> viewedProperties = new HashSet<String>();

	private void clearCaches() {
		response = new StringBuffer();
		elementHierarchy = new JSONObject();
		elements = new JSONObject();
		relationships = new JSONObject();
		viewedElements = new HashMap<String, NodeRef>();
		viewedProperties = new HashSet<String>();
		viewedRelationships = new HashSet<String>();
	}
	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String siteName = JwsRequestUtils.getRequestVar(req, JwsRequestUtils.SITE_NAME);
		if (!JwsRequestUtils.validateRequestVariable(status, response, siteName, JwsRequestUtils.SITE_NAME)) {
			return false;
		} 

		String projectId = JwsRequestUtils.getRequestVar(req, JwsRequestUtils.PROJECT_ID);
		if (!JwsRequestUtils.validateRequestVariable(status, response, projectId, JwsRequestUtils.PROJECT_ID)) {
			return false;
		}

		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
		if (!JwsRequestUtils.validateSiteExists(siteInfo, status, response)) {
			return false;
		}
				
		if (!JwsRequestUtils.validatePermissions(req, status, response, services, siteInfo.getNodeRef(), "Read")) {
			return false;
		}

		ScriptNode siteNode = getSiteNode(siteName);
		projectNode = siteNode.childByNamePath("/ViewEditor/" + projectId);
		if (projectNode == null) {
			return false;
		}
		
		return true;
	}

	/**
	 * Entry point
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();

		if (validateRequest(req, status)) {
			try {
				handleElementHierarchy(projectNode.getNodeRef());
				handleElements();
				handleRelationships();
			} catch (InvalidNodeRefException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		JSONObject top = new JSONObject();
		try {
			top.put("elementHierarchy", elementHierarchy);
			top.put("elements", elements);
			top.put("relationships", relationships);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			model.put("res", top.toString(4));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return model;
	}


	private void handleElementHierarchy(NodeRef root) throws JSONException {
		// ScriptNodes don't work for some reason.. always get Null pointer exception, looks like some timing issues
		NodeService nodeService = services.getNodeService();
		QName sysmlElementFolder = jwsUtil.createQName("sysml:ElementFolder");
		QName sysmlId = jwsUtil.createQName("sysml:id");
		QName cmName = jwsUtil.createQName("cm:name");
		JSONArray array = new JSONArray();
		
		// child associations returns Map<String, Object> where String is the QName association type)
		// Object is an ArrayList of the ScriptNodes with the specified association type
		List<ChildAssociationRef> childrenAssocs = nodeService.getChildAssocs(root);
		for (ChildAssociationRef assoc: childrenAssocs) {
			NodeRef child = assoc.getChildRef();
			if (nodeService.getType(child).equals(sysmlElementFolder)) {
				handleElementHierarchy(child);
			} else {
				String value = (String)nodeService.getProperty(child, sysmlId);
				if (value != null) {
					array.put(value);
					viewedElements.put(value, child);
				}
			}
		}
		
		// if there were any children add them to the hierarchy object
		if (array.length() > 0) {
			String key = (String)nodeService.getProperty(root, sysmlId);
			if (nodeService.getType(root).equals(sysmlElementFolder) && key == null) {
				key = nodeService.getProperty(root, cmName).toString().replace("_pkg", "");
			}
			elementHierarchy.put(key, array);
		}
	}
	
	private void handleElements() throws JSONException {
		NodeService nodeService = services.getNodeService();
		QName sysmlRelationship = jwsUtil.createQName("sysml:DirectedRelationship");
		QName sysmlProperty = jwsUtil.createQName("sysml:Property");
		
		for (String id: viewedElements.keySet()) {
			NodeRef node = viewedElements.get(id);
			JSONObject element = new JSONObject();
			
			QName nodeType = nodeService.getType(node);
			element.put("type", nodeType.getLocalName());
			
			// check for relationships to be handled later
			if (isSubType(nodeType, sysmlRelationship)) {
				viewedRelationships.add(id);
			} else if (isSubType(nodeType, sysmlProperty)) {
				viewedProperties.add(id);
			}
			
			// lets grab all the property values
			for (String acmType: acm2json.keySet()) {
				element.put(acm2json.get(acmType), nodeService.getProperty(node, jwsUtil.createQName(acmType)));
			}
			
			// check if it's a view
			if (nodeService.hasAspect(node, jwsUtil.createQName("sysml:View"))) {
				element.put("isView", true);
			} else {
				element.put("isView", false);
			}
			
			// add owner
			String owner = ((String)nodeService.getProperty(nodeService.getPrimaryParent(node).getParentRef(), jwsUtil.createQName("cm:name"))).replace("_pkg", "");
			element.put("owner", owner);
			// TODO perhaps get the sysml id onto the reified container as well?
//			element.put("owner", nodeService.getProperty(nodeService.getPrimaryParent(node).getParentRef(), jwsUtil.createQName("sysml:id")));
			
			elements.put(id, element);
		}
	}

	private void handleRelationships() throws JSONException {
		handleElementRelationships();
		handlePropertyTypes();
		handleElementValues();
	}
	
	private void handleElementValues() throws JSONException {
		NodeService nodeService = services.getNodeService();
		QName sysmlId = jwsUtil.createQName("sysml:id");

		JSONObject elementValues = new JSONObject();
		for (String id: viewedProperties) {
			NodeRef node = viewedElements.get(id);
			@SuppressWarnings("unchecked")
			ArrayList<NodeRef> values = (ArrayList<NodeRef>)nodeService.getProperty(node, jwsUtil.createQName("sysml:elementValue"));
			if (values != null) {
				JSONArray array = new JSONArray();
				for (NodeRef value: values) {
					array.put((String)nodeService.getProperty(value, sysmlId));
				}
				elementValues.put(id, array);
			}
		}
		relationships.put("elementValues", elementValues);
	}

	private void handlePropertyTypes() throws JSONException {
		NodeService nodeService = services.getNodeService();
		QName sysmlId = jwsUtil.createQName("sysml:id");
		
		JSONObject propertyTypes = new JSONObject();
		for (String id: viewedProperties) {
			NodeRef node = viewedElements.get(id);
			AssociationRef sysmlType = getAssociation(node, "sysml:type");
			if (sysmlType != null) {
				propertyTypes.put(id, nodeService.getProperty(sysmlType.getTargetRef(), sysmlId));
			}
		}
		relationships.put("propertyTypes", propertyTypes);
	}

	private void handleElementRelationships() throws JSONException {
		NodeService nodeService = services.getNodeService();
		QName sysmlId = jwsUtil.createQName("sysml:id");

		// handle relationship elements, property types and element values
		JSONObject relationshipElements = new JSONObject();
		for (String id: viewedRelationships) {
			NodeRef node = viewedElements.get(id);
			AssociationRef sysmlSource = getAssociation(node, "sysml:source");
			AssociationRef sysmlTarget = getAssociation(node, "sysml:target");

			JSONObject relationshipElement = new JSONObject();
			relationshipElement.put("source", nodeService.getProperty(sysmlSource.getTargetRef(), sysmlId));
			relationshipElement.put("target", nodeService.getProperty(sysmlTarget.getTargetRef(), sysmlId));
			relationshipElements.put(id, relationshipElement);
		}
		relationships.put("relationshipElements", relationshipElements);
	}

	private boolean isSubType(QName className, QName ofClassName) {
		return services.getDictionaryService().isSubClass(className, ofClassName);
	}
}
