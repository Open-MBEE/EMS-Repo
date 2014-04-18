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
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ViewGet extends AbstractJavaWebScript {
		
	public ViewGet() {
	    super();
	}
    
    public ViewGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String viewId = req.getServiceMatch().getTemplateVars().get("id");
		if (!checkRequestVariable(viewId, "id")) {
			return false;
		}
		
		EmsScriptNode view = findScriptNodeById(viewId);
		if (view == null) {
			log(LogLevel.ERROR, "View not found with id: " + viewId + ".\n", HttpServletResponse.SC_NOT_FOUND);
			return false;
		}
		
		if (!checkPermissions(view, PermissionService.READ)) {
			return false;
		}
		
		return true;
	}
	

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();
		boolean recurse = checkArgEquals(req, "recurse", "true") ? true : false;
		
		JSONArray viewsJson = new JSONArray();
		if (validateRequest(req, status)) {
			try {
				String viewId = req.getServiceMatch().getTemplateVars().get("id");
				handleView(viewId, viewsJson, recurse);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
			try {
                JSONObject json = new JSONObject();
                json.put("views", viewsJson);
                model.put("res", json.toString(4));
			} catch (JSONException e) {
				e.printStackTrace();
				log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				model.put("res", response.toString());
			}
		} else {
			model.put("res", response.toString());
		}

        status.setCode(responseStatus.getCode());
		return model;
	}

	
	private void handleView(String viewId, JSONArray viewsJson, boolean recurse) throws JSONException {
		EmsScriptNode view = findScriptNodeById(viewId);
		if (view == null) {
			log(LogLevel.ERROR, "View not found with ID: " + viewId, HttpServletResponse.SC_NOT_FOUND);
		}
		
		if (checkPermissions(view, PermissionService.READ)) { 
		    viewsJson.put(view.toJSONObject(Acm.JSON_TYPE_FILTER.VIEW));
        		if (recurse) {
            		JSONArray childrenJson = view.getChildrenViewsJSONArray();
            		for (int ii = 0; ii < childrenJson.length(); ii++) {
            		    handleView(childrenJson.getString(ii), viewsJson, recurse);
            		}
        		}
		}
	}
	
	/**
	 * Gets the matching Expose or Conform element for the passed View
	 * 
	 * @param viewNodeRef
	 * @param elements
	 * @return
	 */
	private EmsScriptNode getMatchingExposeConformElements(NodeRef viewNodeRef,
														   Collection<EmsScriptNode> elements) {
		
		// Check if any of the nodes in the passed collection of Expose or Conform
		// elements have the View as a sysml:source:
		for (EmsScriptNode node : elements) {

	        try {
	
	        	// The Expose element will always be the alfresco source for the association:
	        	JSONArray sourceIds = node.getTargetAssocsIdsByType(Acm.ACM_SOURCE);
	            
	            for (int ii = 0; ii < sourceIds.length(); ii++) {
	                String sourceId = sourceIds.getString(ii);
	                EmsScriptNode source = findScriptNodeById(sourceId);
	                if (source != null) {
	                    
	                	// Check if the source is the view node:
	                	if (viewNodeRef.equals(source.getNodeRef())) {
	                		return source;
	                	}
	                }
	            }
	                      
	        } catch (JSONException e) {
	            log(LogLevel.ERROR, "Could not create the View JSON", HttpServletResponse.SC_BAD_REQUEST);
	            e.printStackTrace();
	        }
		}
        
        return null;
	}
	
	/**
	 * Processes that passes View and converts it to JSON object to be
	 * processed by view editor.
	 * 
	 * @param view
	 */
	private JSONObject viewToJSON(EmsScriptNode view) {
		
		// View editor uses this class to get the jason it needs for the view
		// editor.
		
		EmsSystemModel model = new EmsSystemModel(services);
		NodeRef viewNodeRef = view.getNodeRef();
		
		// Get all elements of Expose type:
		Collection<EmsScriptNode> exposeElements = model.getType(null, "Expose");
		
		// Get all elements of Conform type:
		Collection<EmsScriptNode> conformElements = model.getType(null, "Conform");
		
		// Get the unique Expose and Conform element with the View as a sysml:source:
		EmsScriptNode myExposeElement = getMatchingExposeConformElements(viewNodeRef, exposeElements);
		EmsScriptNode myConformElement = getMatchingExposeConformElements(viewNodeRef, conformElements);
		
		if (myExposeElement != null && myConformElement != null) {
			
			// Get the target of the Conform relationship (the Viewpoint):
			
			// Get the Method Property from the ViewPoint element
			// 		The Method Property owner is the Viewpoint
			
			// Get the value of the elementValue of the Method Property, which is an
			// Operation:
						
			// Parse and convert the Operation:
			
			// Get the target of the Expose relationship:
			
			// Set the function call arguments with the exposed model elements:
			
			// Return the converted JSON from the expression evaluation:
			
		}
		else {
			// TODO: error!
		}
		
		return null;

	}
	
}
