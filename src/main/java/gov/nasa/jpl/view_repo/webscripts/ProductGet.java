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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ProductGet extends AbstractJavaWebScript {
	private JSONArray productsJson;
	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String productId = req.getServiceMatch().getTemplateVars().get("id");
		if (!checkRequestVariable(productId, "id")) {
			return false;
		}
		
		EmsScriptNode product = findScriptNodeByName(productId);
		if (product == null) {
			log(LogLevel.ERROR, "Product not found with id: " + productId + ".\n", HttpServletResponse.SC_NOT_FOUND);
			return false;
		}
		
		if (!checkPermissions(product, PermissionService.READ)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	protected void clearCaches() {
		super.clearCaches();
		productsJson = new JSONArray();
	}
	
	@Override
	protected synchronized Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();

		if (validateRequest(req, status)) {
			try {
				String productId = req.getServiceMatch().getTemplateVars().get("id");
				handleProduct(productId);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
			try {
			    JSONObject top = new JSONObject();
			    top.put("products", productsJson);
				model.put("res", top.toString(4));
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

	
	private void handleProduct(String productId) throws JSONException {
		EmsScriptNode product = findScriptNodeByName(productId);
		if (product == null) {
			log(LogLevel.ERROR, "Product not found with ID: " + productId, HttpServletResponse.SC_NOT_FOUND);
		}

		if (checkPermissions(product, PermissionService.READ)){ 
		    productsJson.put(product.toJSONObject(Acm.JSON_TYPE_FILTER.PRODUCT));
		}
	}

}
