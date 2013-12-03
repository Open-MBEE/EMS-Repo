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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ProjectPost extends AbstractJavaWebScript {
	private final String MODEL_PATH = "ViewEditor";
	private final String MODEL_PATH_SEARCH = "/" + MODEL_PATH;
	private final String MODEL_PATH_NESTED_SEARCH = MODEL_PATH_SEARCH + "/";
	private final String PROJECT_ID = "projectId";
	private final String SITE_NAME = "siteName";
	
	/**
	 * Webscript entry point
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();
		StringBuffer response = new StringBuffer();
		ScriptNode siteNode = null;

		// grab the request variables
		String siteName = getRequestVar(req, SITE_NAME);
		String projectId = getRequestVar(req, PROJECT_ID);
		if (parseRequest(req, status, response)) {
			siteNode = getSiteNode(siteName);
			ScriptNode modelContainerNode = siteNode.childByNamePath(MODEL_PATH_SEARCH);
			if (modelContainerNode == null) {
				modelContainerNode = siteNode.createFolder(MODEL_PATH);
			}
			ScriptNode projectNode = modelContainerNode.createFolder(projectId);
			JSONObject postJson = (JSONObject)req.parseContent();
			try {
				jwsUtil.setNodeProperty(projectNode, "cm:title", postJson.getString("name"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
			response.append("Project created");
			status.setCode(HttpServletResponse.SC_ACCEPTED);
		}
		
		boolean delete = jwsUtil.checkArgEquals(req, "delete", "true") ? true : false;
		if (delete && status.getCode() == HttpServletResponse.SC_FOUND) {
			response.delete(0, response.length());
			ScriptNode projectNode = siteNode.childByNamePath(MODEL_PATH_NESTED_SEARCH + projectId);
			projectNode.remove();
			response.append("Project deleted");
			status.setCode(HttpServletResponse.SC_ACCEPTED);
		}
		
		response.append("\n");
		model.put("res", response.toString());
		return model;
	}

	
	/**
	 * Note that this has side effect of setting the siteNode if parsed successfully
	 */
	@Override
	protected boolean parseRequest(WebScriptRequest req, Status status, StringBuffer response) {
		if (req.getContent() == null) {
			status.setCode(HttpServletResponse.SC_NO_CONTENT);
			response.append("No content provided");
			return false;
		}
		
		// check for valid siteName
		String siteName = getRequestVar(req, SITE_NAME);
		if (siteName == null) {
			status.setCode(HttpServletResponse.SC_BAD_REQUEST);
			response.append("Site name not provided");
			return false;
		} else {
			// check if site exists
			SiteInfo siteInfo = services.getSiteService().getSite(siteName);
			if (siteInfo == null) {
				status.setCode(HttpServletResponse.SC_NOT_FOUND);
				response.append("Site " + siteName + " not found");
				return false;
			}
			
			// check if user has permissions to write to site
			if (services.getPermissionService().hasPermission(siteInfo.getNodeRef(), "Write") != AccessStatus.ALLOWED) {
				status.setCode(HttpServletResponse.SC_UNAUTHORIZED);
				response.append("No write priveleges to site " + siteName);
				return false;
			}
			
			// check for valid projectId
			String projectId = getRequestVar(req, PROJECT_ID);
			if (projectId == null) {
				status.setCode(HttpServletResponse.SC_BAD_REQUEST);
				response.append("Project ID not provided");
				return false;
			} else {
				// check if node already exists
				ScriptNode siteNode = getSiteNode(siteName);
				if (siteNode.childByNamePath(MODEL_PATH_NESTED_SEARCH + projectId) != null) {
					status.setCode(HttpServletResponse.SC_FOUND);
					response.append("Project ID already exists");
					return false;
				}
			}
		}
		
		return true;
	}
	
	protected ScriptNode getSiteNode(String siteName) {
		return new ScriptNode(services.getSiteService().getSite(siteName).getNodeRef(), services);
	}
}
