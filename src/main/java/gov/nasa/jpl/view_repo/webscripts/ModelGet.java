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

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
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
	private EmsScriptNode modelRootNode = null;
	private Map<String, EmsScriptNode> foundElements = new HashMap<String, EmsScriptNode>();
	private Set<String> foundRelationships = new HashSet<String>();
	private Set<String> foundProperties = new HashSet<String>();

	private void clearCaches() {
		response = new StringBuffer();
		elementHierarchy = new JSONObject();
		elements = new JSONObject();
		relationships = new JSONObject();
		foundElements = new HashMap<String, EmsScriptNode>();
		foundProperties = new HashSet<String>();
		foundRelationships = new HashSet<String>();
	}
	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String modelId = JwsRequestUtils.getRequestVar(req, "modelid");
		if (!JwsRequestUtils.validateRequestVariable(status, response, modelId, "modelid")) {
			return false;
		}
		
		modelRootNode = findScriptNodeByName(modelId);
		if (!JwsRequestUtils.validatePermissions(req, status, response, services, modelRootNode.getNodeRef(), "Read")) {
			return false;
		}
		
//		String siteName = JwsRequestUtils.getRequestVar(req, JwsRequestUtils.SITE_NAME);
//		if (!JwsRequestUtils.validateRequestVariable(status, response, siteName, JwsRequestUtils.SITE_NAME)) {
//			return false;
//		} 
//
//		String projectId = JwsRequestUtils.getRequestVar(req, JwsRequestUtils.PROJECT_ID);
//		if (!JwsRequestUtils.validateRequestVariable(status, response, projectId, JwsRequestUtils.PROJECT_ID)) {
//			return false;
//		}
//
//		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
//		if (!JwsRequestUtils.validateSiteExists(siteInfo, status, response)) {
//			return false;
//		}
//				
//		if (!JwsRequestUtils.validatePermissions(req, status, response, services, siteInfo.getNodeRef(), "Read")) {
//			return false;
//		}
//
//		EmsScriptNode siteNode = getSiteNode(siteName);
//		projectNode = siteNode.childByNamePath("/ViewEditor/" + projectId);
//		if (projectNode == null) {
//			return false;
//		}
		
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

		boolean recurse = jwsUtil.checkArgEquals(req, "recurse", "true") ? true : false;
		if (validateRequest(req, status)) {
			try {
				if (recurse) {
					handleElementHierarchy(modelRootNode);
				}
				// make sure to put the root node in as a found element
				foundElements.put((String)modelRootNode.getProperty("cm:name"), modelRootNode);
				handleElements();
				handleRelationships();
			} catch (JSONException e) {
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


	/**
	 * Build up the element hierarchy from the specified root
	 * @param root		Root node to get children for
	 * @throws JSONException
	 */
	private void handleElementHierarchy(EmsScriptNode root) throws JSONException {
		JSONArray array = new JSONArray();		
		
		// find all the children, recurse or add to array as needed
		for (ChildAssociationRef assoc: root.getChildAssociationRefs()) {
			EmsScriptNode child = new EmsScriptNode(assoc.getChildRef(), services, response);
			if (child.getTypeShort().equals("sysml:ElementFolder")) {
				handleElementHierarchy(child);
			} else {
				String value = (String)child.getProperty("sysml:id");
				if (value != null) {
					array.put(value);
					foundElements.put(value, child);
					// add empty hierarchies as well
					elementHierarchy.put(value, new JSONArray());
				}
			}
		}
		
		// if there were any children add them to the hierarchy object
		String key = (String)root.getProperty("sysml:id");
		if (root.getTypeShort().equals("sysml:ElementFolder") && key == null) {
			// TODO this is temporary? until we can get sysml:id from Element Folder?
			key = root.getProperty("cm:name").toString().replace("_pkg", "");
		}
		elementHierarchy.put(key, array);
	}
	
	/**
	 * Build up the element JSONObject
	 * @throws JSONException
	 */
	private void handleElements() throws JSONException {
		for (String id: foundElements.keySet()) {
			EmsScriptNode node = foundElements.get(id);
			JSONObject element = new JSONObject();
			
			element.put("type", node.getQNameType().getLocalName());
			
			// check for relationships to be handled later
			if (node.isSubType("sysml:DirectedRelationship")) {
				foundRelationships.add(id);
			} else if (node.isSubType("sysml:Property")) {
				foundProperties.add(id);
			}
			
			// lets grab all the property values
			for (String acmType: acm2json.keySet()) {
				element.put(acm2json.get(acmType), node.getProperty(acmType));
			}
			
			// check if it's a view
			if (node.hasAspect("sysml:View")) {
				element.put("isView", true);
			} else {
				element.put("isView", false);
			}
			
			// add owner
			EmsScriptNode parent = node.getParent();
			element.put("owner", parent.getName().replace("_pkg", ""));
			// TODO perhaps get the sysml id onto the reified container as well?
//			element.put("owner", nodeService.getProperty(nodeService.getPrimaryParent(node).getParentRef(), jwsUtil.createQName("sysml:id")));
			
			elements.put(id, element);
		}
	}

	/**
	 * Handle all the relationship JSONObjects
	 * @throws JSONException
	 */
	private void handleRelationships() throws JSONException {
		handleElementRelationships();
		handlePropertyTypes();
		handleElementValues();
	}
	
	/**
	 * Create the Element Values JSONObject
	 * @throws JSONException
	 */
	private void handleElementValues() throws JSONException {
		NodeService nodeService = services.getNodeService();
		QName sysmlId = jwsUtil.createQName("sysml:id");

		JSONObject elementValues = new JSONObject();
		for (String id: foundProperties) {
			EmsScriptNode node = foundElements.get(id);
			@SuppressWarnings("unchecked")
			ArrayList<NodeRef> values = (ArrayList<NodeRef>)node.getProperty("sysml:elementValue");
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

	/**
	 * Create the PropertyTypes JSONObject
	 * @throws JSONException
	 */
	private void handlePropertyTypes() throws JSONException {
		JSONObject propertyTypes = new JSONObject();
		
		for (String id: foundProperties) {
			EmsScriptNode node = foundElements.get(id);
			EmsScriptNode targetNode = node.getFirstAssociationByType("sysml:type");
			if (targetNode != null) {
				propertyTypes.put(id, targetNode.getProperty("sysml:id"));
			}
		}
		
		relationships.put("propertyTypes", propertyTypes);
	}

	/**
	 * Create the ElementRelationships JSONObject
	 * @throws JSONException
	 */
	private void handleElementRelationships() throws JSONException {
		// handle relationship elements, property types and element values
		JSONObject relationshipElements = new JSONObject();
		for (String id: foundRelationships) {
			EmsScriptNode node = foundElements.get(id);
			EmsScriptNode sysmlSourceNode = node.getFirstAssociationByType("sysml:source");
			EmsScriptNode sysmlTargetNode = node.getFirstAssociationByType("sysml:target");

			JSONObject relationshipElement = new JSONObject();
			if (sysmlSourceNode != null) {
				relationshipElement.put("source", sysmlSourceNode.getProperty("sysml:id"));
			}
			if (sysmlTargetNode != null) {
				relationshipElement.put("target", sysmlTargetNode.getProperty("sysml:id"));
			}
			relationshipElements.put(id, relationshipElement);
		}
		relationships.put("relationshipElements", relationshipElements);
	}
}
