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
import gov.nasa.jpl.view_repo.util.Acm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
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
    // injected via spring configuration
    protected boolean isViewRequest = false;
    
	private JSONObject elementHierarchy = new JSONObject();
	protected JSONArray elements = new JSONArray();
	private JSONObject relationships = new JSONObject();
	private EmsScriptNode modelRootNode = null;
	private Set<String> foundRelationships = new HashSet<String>();
	private Set<String> foundProperties = new HashSet<String>();
	protected Map<String, EmsScriptNode> elementsFound = new HashMap<String, EmsScriptNode>();

	
	@Override
	protected void clearCaches() {
		super.clearCaches();
		elementHierarchy = new JSONObject();
		elements = new JSONArray();
		relationships = new JSONObject();
		foundProperties = new HashSet<String>();
		foundRelationships = new HashSet<String>();
		elementsFound = new HashMap<String, EmsScriptNode>();
	}

	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String modelId = req.getServiceMatch().getTemplateVars().get("modelid");
		if (modelId == null) {
			modelId = req.getServiceMatch().getTemplateVars().get("elementid");
		}
		
		if (!checkRequestVariable(modelId, "modelid")) {
			log(LogLevel.ERROR, "Element id not specified.\n", HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		
		modelRootNode = findScriptNodeByName(modelId);
		if (modelRootNode == null) {
			log(LogLevel.ERROR, "Element not found with id: " + modelId + ".\n", HttpServletResponse.SC_NOT_FOUND);
			return false;
		}
		
		// TODO: need to check permissions on every node ref - though it looks like this might throw an error
		if (!checkPermissions(modelRootNode, PermissionService.READ)) {
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

		boolean recurse = jwsUtil.checkArgEquals(req, "recurse", "true") ? true : false;
		
		if (validateRequest(req, status)) {
			try {
				if (isViewRequest) {
					handleViewHierarchy(modelRootNode, recurse);
				} else {
					handleElementHierarchy(modelRootNode, recurse);
				}
				handleElements();
//				handleRelationships();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		JSONObject top = new JSONObject();
		try {
//			top.put("elementHierarchy", elementHierarchy);
			top.put("elements", elements);
//			top.put("relationships", relationships);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			model.put("res", top.toString(4));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		status.setCode(responseStatus.getCode());
		return model;
	}


	/**
	 * Recurse a view hierarchy to get all allowed elements
	 * @param root		Root view to find elements for
	 * @param recurse	If true, find elements for children views
	 * @throws JSONException	JSON element creation error
	 */
	protected void handleViewHierarchy(EmsScriptNode root, boolean recurse) throws JSONException {
		Object allowedElements = root.getProperty(Acm.ACM_ALLOWED_ELEMENTS);
		if (allowedElements != null) {
			JSONArray childElementJson = new JSONArray(allowedElements.toString());
			for (int ii = 0; ii < childElementJson.length(); ii++) {
				String id = childElementJson.getString(ii);
				EmsScriptNode childElement = findScriptNodeByName(id);
				if (checkPermissions(childElement, PermissionService.READ)) {
				    elementsFound.put(id, childElement);
				}
			}
			if (recurse) {
				Object childrenViews = root.getProperty(Acm.ACM_CHILDREN_VIEWS);
				if (childrenViews != null) {
					JSONArray childViewJson = new JSONArray(childrenViews.toString());
					for (int ii = 0; ii < childViewJson.length(); ii++) {
						String id = childViewJson.getString(ii);
						EmsScriptNode childView = findScriptNodeByName(id);
						if (checkPermissions(childView, PermissionService.READ)) {
						    handleViewHierarchy(childView, recurse);
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Build up the element hierarchy from the specified root
	 * @param root		Root node to get children for
	 * @throws JSONException
	 */
	protected void handleElementHierarchy(EmsScriptNode root, boolean recurse) throws JSONException {
		JSONArray array = new JSONArray();
		
		// add root element to elementsFound if its not already there (if it's there, it's probably because the root is a reified pkg node)
		String sysmlId = (String)root.getProperty(Acm.ACM_ID);
		if (!elementsFound.containsKey(sysmlId)) {
		    // dont add reified packages
		    if (!((String)root.getProperty("cm:name")).contains("_pkg")) {
		        elementsFound.put((String)root.getProperty(Acm.ACM_ID), root);
		    }
		}

		if (recurse) {
			// find all the children, recurse or add to array as needed
		    // TODO: figure out why the child association creation from the reification isn't being picked up
		    String rootName = (String)root.getProperty("cm:name");
		    if (!rootName.contains("_pkg")) {
		        EmsScriptNode reifiedNode = findScriptNodeByName(rootName + "_pkg");
		        if (reifiedNode != null) {
		            handleElementHierarchy(reifiedNode, recurse);
		        }
		    } 
			for (ChildAssociationRef assoc: root.getChildAssociationRefs()) {
				EmsScriptNode child = new EmsScriptNode(assoc.getChildRef(), services, response);
				if (checkPermissions(child, PermissionService.READ)) {
			       if (child.getTypeShort().equals(Acm.ACM_ELEMENT_FOLDER)) {
						handleElementHierarchy(child, recurse);
    			   } else {
    					String value = (String)child.getProperty(Acm.ACM_ID);
    					if (value != null) {
    						array.put(value);
    						elementsFound.put(value, child);
    						// add empty hierarchies as well
    						elementHierarchy.put(value, new JSONArray());
    					}
    			   }
				}
			}
	    	
			// if there were any children add them to the hierarchy object
			String key = (String)root.getProperty(Acm.ACM_ID);
			if (root.getTypeShort().equals(Acm.ACM_ELEMENT_FOLDER) && key == null) {
				// TODO this is temporary? until we can get sysml:id from Element Folder?
				key = root.getProperty("cm:name").toString().replace("_pkg", "");
			}
			
			elementHierarchy.put(key, array);
		}
	}
	
	/**
	 * Build up the element JSONObject
	 * @throws JSONException
	 */
	protected void handleElements() throws JSONException {
		for (String id: elementsFound.keySet()) {
			EmsScriptNode node = elementsFound.get(id);

			if (checkPermissions(node, PermissionService.READ)){ 
                elements.put(node.toJSONObject(Acm.JSON_TYPE_FILTER.ELEMENT));
//              // check for relationships to be handled later
//              if (node.isSubType("sysml:DirectedRelationship")) {
//                  foundRelationships.add(id);
//              } else if (node.isSubType("sysml:Property")) {
//                  foundProperties.add(id);
//              }
			}
		}
	}
	

	/**
	 * Handle all the relationship JSONObjects
	 * @throws JSONException
	 */
	protected void handleRelationships() throws JSONException {
		handleElementRelationships();
		handlePropertyTypes();
		handleElementValues();
	}
	
	/**
	 * Create the Element Values JSONObject
	 * @throws JSONException
	 */
	protected void handleElementValues() throws JSONException {
		NodeService nodeService = services.getNodeService();
		QName sysmlId = jwsUtil.createQName(Acm.ACM_ID);

		JSONObject elementValues = new JSONObject();
		for (String id: foundProperties) {
			EmsScriptNode node = foundElements.get(id);
			if (checkPermissions(node, PermissionService.READ)){ 
    			@SuppressWarnings("unchecked")
    			ArrayList<NodeRef> values = (ArrayList<NodeRef>)node.getProperty(Acm.ACM_ELEMENT_VALUE);
    			if (values != null) {
    				JSONArray array = new JSONArray();
    				for (NodeRef value: values) {
    					array.put((String)nodeService.getProperty(value, sysmlId));
    				}
    				elementValues.put(id, array);
    			}
			}
		}
		relationships.put("elementValues", elementValues);
	}

	/**
	 * Create the PropertyTypes JSONObject
	 * @throws JSONException
	 */
	protected void handlePropertyTypes() throws JSONException {
		JSONObject propertyTypes = new JSONObject();
		
		for (String id: foundProperties) {
			EmsScriptNode node = foundElements.get(id);
			if (checkPermissions(node, PermissionService.READ)){ 
    			EmsScriptNode targetNode = node.getFirstAssociationByType(Acm.ACM_TYPE);
    			if (targetNode != null) {
    				propertyTypes.put(id, targetNode.getProperty(Acm.ACM_ID));
    			}
			}
		}
		
		relationships.put("propertyTypes", propertyTypes);
	}

	/**
	 * Create the ElementRelationships JSONObject
	 * @throws JSONException
	 */
	protected void handleElementRelationships() throws JSONException {
		// handle relationship elements, property types and element values
		JSONObject relationshipElements = new JSONObject();
		for (String id: foundRelationships) {
			EmsScriptNode node = foundElements.get(id);
			EmsScriptNode sysmlSourceNode = node.getFirstAssociationByType(Acm.ACM_SOURCE);
			EmsScriptNode sysmlTargetNode = node.getFirstAssociationByType(Acm.ACM_TARGET);

			JSONObject relationshipElement = new JSONObject();
			if (sysmlSourceNode != null && sysmlTargetNode != null) {
			    if (checkPermissions(node, PermissionService.READ) && 
			            checkPermissions(sysmlSourceNode, PermissionService.READ) &&
			            checkPermissions(sysmlTargetNode, PermissionService.READ)) {
		            relationshipElement.put(Acm.JSON_SOURCE, sysmlSourceNode.getProperty(Acm.ACM_ID));
		            relationshipElement.put(Acm.JSON_TARGET, sysmlTargetNode.getProperty(Acm.ACM_ID));
			    }
			}
			relationshipElements.put(id, relationshipElement);
		}
		relationships.put("relationshipElements", relationshipElements);
	}
	
	
	public void setIsViewRequest(boolean flag) {
	    isViewRequest = flag;
	}
}
