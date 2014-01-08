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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class NewViewGet extends AbstractJavaWebScript {
	private JSONArray viewsJson;
	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String viewId = req.getServiceMatch().getTemplateVars().get("id");
		if (!checkRequestVariable(viewId, "id")) {
			return false;
		}
		
		EmsScriptNode view = findScriptNodeByName(viewId);
		if (!checkPermissions(view.getNodeRef(), "Read")) {
			log(LogLevel.WARNING, "No read permissions", HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		
		return true;
	}
	
	@Override
	protected void clearCaches() {
		super.clearCaches();
		viewsJson = new JSONArray();
	}
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();
		boolean recurse = jwsUtil.checkArgEquals(req, "recurse", "true") ? true : false;
		
		if (validateRequest(req, status)) {
			try {
				String viewId = req.getServiceMatch().getTemplateVars().get("id");
				handleView(viewId, recurse);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
			try {
				if (!recurse) {
					model.put("res", viewsJson.getJSONObject(0).toString());
				} else {
					JSONObject json = new JSONObject();
						json.put("views", viewsJson);
					model.put("res", json.toString());
				}
			} catch (JSONException e) {
				e.printStackTrace();
				log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				model.put("res", response.toString());
			}
		} else {
			model.put("res", response.toString());
		}

		status = responseStatus;
		return model;
	}

	
	private void handleView(String viewId, boolean recurse) throws JSONException {
		JSONObject viewJson = new JSONObject();
		EmsScriptNode view = findScriptNodeByName(viewId);
		if (view == null) {
			log(LogLevel.ERROR, "View not found with ID: " + viewId, HttpServletResponse.SC_NOT_FOUND);
		}
		Object property = view.getProperty("sysml:id");
		if (property != null) {
			viewJson.put("id", property);
		}
		property = view.getProperty("view2:displayedElements");
		if (property != null) {
			viewJson.put("displayedElements", new JSONArray(property.toString()));
		}
		property = view.getProperty("view2:allowedElements");
		if (property != null) {
			viewJson.put("allowedElements", new JSONArray(property.toString()));
		}
		property = view.getProperty("view2:contains");
		if (property != null) {
			viewJson.put("contains", new JSONArray(property.toString()));
		}
		property = view.getProperty("view2:childrenViews");
		if (property != null) {
			JSONArray childrenJson = new JSONArray(property.toString());
			viewJson.put("childrenViews", childrenJson);
			if (recurse) {
				for (int ii = 0; ii < childrenJson.length(); ii++) {
					handleView(childrenJson.getString(ii), recurse);
				}
			}
		}
		
		viewsJson.put(viewJson);
	}
	
}
