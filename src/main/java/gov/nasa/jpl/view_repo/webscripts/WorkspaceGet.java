package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

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

public class WorkspaceGet extends AbstractJavaWebScript{
	
	public WorkspaceGet(){
		super();
	}
	
	public WorkspaceGet(Repository repositoryHelper, ServiceRegistry service){
		super(repositoryHelper, service);
	}
	
	protected Map<String, Object> executeImpl (WebScriptRequest req, Status status, Cache cache) {
		printHeader(req);
		clearCaches();
		Map<String, Object> model = new HashMap<String, Object>();
		JSONObject object = null;
		
		try{
			if(validateRequest(req, status)){
				String userName = AuthenticationUtil.getRunAsUser();
				String wsID = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
				WorkspaceNode workspace = AbstractJavaWebScript.getWorkspaceFromId(wsID, getServices(), getResponse(), status, false, userName);
				object = getWorkspace(workspace, wsID);
			}
		} catch (JSONException e) {
			log(LogLevel.ERROR, "JSON object could not be created \n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
		} catch (Exception e) {
			log(LogLevel.ERROR, "Internal error stack trace \n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
		}
		
		if(object == null){
			model.put("res", response.toString());
		} else {
			try{
				model.put("res", object.toString(4));
			} catch (JSONException e){
				e.printStackTrace();
			}
		}
		status.setCode(responseStatus.getCode());
		printFooter();
		return model;
		
	}
	protected Object getStringIfNull(Object object){
		if(object == null)
			return "null";
		return object;
	}
	
	protected JSONObject getWorkspace(WorkspaceNode ws, String wsID) throws JSONException {
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject interiorJson = new JSONObject();
		if(ws == null && wsID.equals("master")){
			interiorJson.put("creator", "null");
			interiorJson.put("created",  "null");
			interiorJson.put("name", wsID);
			interiorJson.put("parent", "null");
			interiorJson.put("branched", "null");
			jsonArray.put(interiorJson);
		}
		else if(checkPermissions(ws, PermissionService.READ)){
			jsonArray.put(ws.toJSONObject(null));
		}
		else
			log(LogLevel.WARNING, "No permission to read: " + ws.getSysmlId(), HttpServletResponse.SC_NOT_FOUND);
		json.put("workspace" , jsonArray);
		return json;
	}
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// TODO Auto-generated method stub
		if(checkRequestContent(req) == false) 
			return false;
		
		String wsId = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
		
		if(checkRequestVariable(wsId, WORKSPACE_ID) == false)
			return false;
		return true;
	}

}
