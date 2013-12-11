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
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor at /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/project.post.desc.xml
 * @author cinyoung
 *
 */
public class ProjectPost extends AbstractJavaWebScript {
	private final String MODEL_PATH = "ViewEditor";
	private final String MODEL_PATH_SEARCH = "/" + MODEL_PATH;
	
	private String siteName = null;
	private String projectId = null;
	private boolean delete = false;
	private boolean fix = false;
		
	/**
	 * Utility method for getting the request parameters from the URL template
	 * @param req
	 */
	private void parseRequestVariables(WebScriptRequest req) {
		siteName = JwsRequestUtils.getRequestVar(req, JwsRequestUtils.SITE_NAME);
		projectId = JwsRequestUtils.getRequestVar(req, JwsRequestUtils.PROJECT_ID);
		delete = jwsUtil.checkArgEquals(req, "delete", "true") ? true : false;
		fix = jwsUtil.checkArgEquals(req, "fix", "true") ? true : false;
	}
	
	/**
	 * Webscript entry point
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		// clear out the response cache first (only one instance of each webscript)
		response = new StringBuffer();
		
		Map<String, Object> model = new HashMap<String, Object>();
		int statusCode = HttpServletResponse.SC_OK;

		parseRequestVariables(req);
		try {
			if (validateRequest(req, status)) {
				statusCode = updateOrCreateProject((JSONObject)req.parseContent(), projectId, siteName);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// this is most likely null pointer from poorly undefined request parameters
			// TODO check permissions on Project updating 
			e.printStackTrace();
			response.append("Invalid request.\n");
			statusCode = HttpServletResponse.SC_BAD_REQUEST;
		}

		status.setCode(statusCode);
		model.put("res", response.toString());
		return model;
	}

	/**
	 * Update or create the project specified by the JSONObject
	 * @param jsonObject	JSONObject that has the name of the project
	 * @param projectId		Project ID
	 * @param siteName		Site project should reside in
	 * @return				HttpStatusResponse code for success of the POST request
	 * @throws JSONException
	 */
	private int updateOrCreateProject(JSONObject jsonObject, String projectId, String siteName) throws JSONException {
		// make sure site exists
		ScriptNode siteNode = getSiteNode(siteName);
		if (siteNode == null) {
			response.append("Site not found\n");
			return HttpServletResponse.SC_NOT_FOUND;
		}
		
		// make sure Model packge under site exists
		ScriptNode modelContainerNode = siteNode.childByNamePath(MODEL_PATH_SEARCH);
		if (modelContainerNode == null) {
			if (fix) {
				modelContainerNode = siteNode.createFolder(MODEL_PATH);
				response.append("Model folder created.\n");
			} else {
				response.append("Model folder not found\n");
				return HttpServletResponse.SC_FOUND;
			}
		}
		
		// create project if doesn't exist or update if fix is specified 
		ScriptNode projectNode = findNodeWithName(projectId);
		String projectName = jsonObject.getString("name");
		if (projectNode == null) {
			projectNode = modelContainerNode.createFolder(projectId, "sysml:Project");
			jwsUtil.setNodeProperty(projectNode, "cm:title", projectName);
			jwsUtil.setNodeProperty(projectNode, "sysml:name", projectName);
			response.append("Project created.\n");
		} else {
			if (delete) {
				projectNode.remove();
				response.append("Project deleted.\n");
			} else if (fix) {
				// update Name if different than existing name
				if (checkAndUpdateProperty(projectNode, projectName, "cm:title") || checkAndUpdateProperty(projectNode, projectName, "sysml:name")) {
					response.append("Project renamed.\n");
				}
				// move sites if exists under different site
				if (!projectNode.getParent().equals(modelContainerNode)) {
					projectNode.move(modelContainerNode);
					response.append("Project moved to specified site.\n");
				}
			} else {
				response.append("Project not moved or name not updated.\n");
				return HttpServletResponse.SC_FOUND;
			}
		}
		
		return HttpServletResponse.SC_OK;
	}

	/**
	 * Validate the request and check some permissions
	 */
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		if (!JwsRequestUtils.validateContent(req, status, response)) {
			return false;
		}
		
		if (!JwsRequestUtils.validateRequestVariable(status, response, siteName, JwsRequestUtils.SITE_NAME)) {
			return false;
		} 
		
		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
		if (!JwsRequestUtils.validateSiteExists(siteInfo, status, response)) {
			return false;
		}
				
		if (!JwsRequestUtils.validatePermissions(req, status, response, services, siteInfo.getNodeRef(), "Write")) {
			return false;
		}
		
		String projectId = JwsRequestUtils.getRequestVar(req, JwsRequestUtils.PROJECT_ID);
		if (!JwsRequestUtils.validateRequestVariable(status, response, projectId, JwsRequestUtils.PROJECT_ID)) {
			return false;
		}

		return true;
	}
}
