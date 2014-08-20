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

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.ModelPost;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor at /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/m
 * @author johnli
 *
 */

public class WorkspacesPost extends AbstractJavaWebScript{
	public WorkspacesPost() {
		super();
	}
	
	public WorkspacesPost(Repository repositoryHelper, ServiceRegistry registry){
		super(repositoryHelper, registry);
	}
	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		if(!checkRequestContent ( req )) {
			return false;
		}
		
		String workspaceId = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
		if (!checkRequestVariable(workspaceId, WORKSPACE_ID)) {
			return false;
		}
		return true;
	}
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache){
		printHeader( req );
		clearCaches();
		Map<String, Object> model = new HashMap<String, Object>();
		int statusCode = HttpServletResponse.SC_OK;
		String user = AuthenticationUtil.getRunAsUser();
		JSONObject json = null;
		try{
			if(validateRequest(req, status)){
				String sourceWorkspaceParam = req.getParameter("sourceWorkspace");
				String newName = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
				statusCode = createWorkSpace(sourceWorkspaceParam, newName, (JSONObject)req.parseContent(), user, status);
				WorkspaceNode ws = AbstractJavaWebScript.getWorkspaceFromId(newName, getServices(), getResponse(), status, false, user);
				json = printObject(ws);
			} else {
				statusCode = responseStatus.getCode();
			}
		} catch (Exception e){
			log(LogLevel.ERROR, "Internal stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
		}
		if(json == null)
			model.put("res", response.toString());
		else
			try {
				model.put("res", json.toString(4));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		status.setCode(statusCode);
		printFooter();
		return model;
	}
	protected JSONObject printObject(WorkspaceNode ws) throws JSONException{
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject interiorJson = new JSONObject();
		if(checkPermissions(ws, PermissionService.READ)){
			interiorJson.put("lastTimeSyncParent", getStringIfNull(ws.getProperty("ems:lastTimeSyncParent")));
        	if(ws.getSourceWorkspace() != null)
        		interiorJson.put("ems:source", getStringIfNull(ws.getSourceWorkspace().getProperty(Acm.CM_NAME)));
        	else
        		interiorJson.put("ems:source:", "master"); // workspace is null only if master.
        	interiorJson.put(Acm.JSON_TYPE, getStringIfNull(ws.getProperty(Acm.ACM_TYPE)));
    		interiorJson.put(Acm.JSON_NAME, getStringIfNull(ws.getProperty(Acm.CM_NAME)));
    	}
    	else {
    		log(LogLevel.WARNING,"No permission to read: "+ ws.getSysmlId(),HttpServletResponse.SC_NOT_FOUND);
		}
		jsonArray.put(interiorJson);
		json.put("workspace", jsonArray);
		return json;
	}
	
	protected Object getStringIfNull(Object object){
		if(object == null)
			return "null";
		return object;
	}
	
	public int createWorkSpace(String sourceWorkId, String newWorkID, JSONObject jsonObject, String user, Status status){
		if(newWorkID == "master"){
			log(LogLevel.WARNING, "Workspace already exists. \n", HttpServletResponse.SC_BAD_REQUEST);
			return HttpServletResponse.SC_BAD_REQUEST;
		}
		else if(AbstractJavaWebScript.getWorkspaceFromId(newWorkID, services, response, status, false, user) != null){
			log(LogLevel.WARNING, "Workspace already exists. \n", HttpServletResponse.SC_BAD_REQUEST);
			return HttpServletResponse.SC_BAD_REQUEST;
		}	
		else {
			EmsScriptNode folder = null;
			WorkspaceNode ws = WorkspaceNode.createWorskpaceFromSource(newWorkID, user, sourceWorkId, folder, getServices(), getResponse(), status);
		}
		return HttpServletResponse.SC_OK;
	}
}