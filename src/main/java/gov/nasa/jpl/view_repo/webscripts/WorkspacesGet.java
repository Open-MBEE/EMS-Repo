package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.service.ServiceRegistry;


public class WorkspacesGet extends AbstractJavaWebScript{
	
    protected boolean gettingContainedWorkspaces = false;
	
    public WorkspacesGet() {
        super();
    }
    
    public WorkspacesGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);	
    }
    
	/**
	 * Entry point
	 */
	@Override
	protected Map<String, Object> executeImpl (WebScriptRequest req, Status status, Cache cache) {
		printHeader( req );

        clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {
                
                String userName = AuthenticationUtil.getRunAsUser();
                EmsScriptNode homeFolder = NodeUtil.getUserHomeFolder(userName);
                
                json = handleWorkspace (homeFolder);
            }
        } catch (JSONException e) {
            log(LogLevel.ERROR, "JSON could not be created\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } catch (Exception e) {
            log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
        if (json == null) {
            model.put("res", response.toString());
        } else {
            model.put("res", json.toString());
        }
        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
	}
    
	protected JSONObject handleWorkspace (EmsScriptNode homeFolder) throws JSONException{
		
		JSONObject json = null;
		JSONArray jArray = null;
		
        ResultSet refs = NodeUtil.findNodeRefsByType( "*", SearchType.WORKSPACE, services );
        List< EmsScriptNode > nodes = NodeUtil.resultSetToList( refs );
		
        for (EmsScriptNode workspaceNode: nodes){
        	JSONObject interiorJson = null;
        	if (checkPermissions(workspaceNode, PermissionService.READ)){
	        	interiorJson.put(Acm.JSON_TYPE, workspaceNode.getProperty(Acm.ACM_TYPE));
	        	interiorJson.put(Acm.JSON_ID, workspaceNode.getSysmlId());
	        	interiorJson.put(Acm.JSON_NAME, workspaceNode.getProperty(Acm.CM_TITLE));
	        	interiorJson.put(Acm.JSON_SOURCE, "ems:source"/*((WorkspaceNode) workspaceNode).getParentWorkspace().getSysmlId()*/);
	        	interiorJson.put("timestamp", workspaceNode.getProperty("ems:lastTimeSyncParent"));
        	}
        	else {
        		log(LogLevel.WARNING,"No permission to read: "+ workspaceNode.getSysmlId(),HttpServletResponse.SC_NOT_FOUND);
        	}
        	
        	jArray.put(interiorJson);
        }
		
        json.put("workspaces", jArray);
        return json;
	}
    
    /**
     * Validate the request and check some permissions
     */
    @Override
	protected boolean validateRequest (WebScriptRequest req, Status status){
		
    	String workspaceID = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
    	
		if (checkRequestContent(req)==false){
			return false;
		}
		else if (checkRequestVariable(workspaceID, WORKSPACE_ID) == false){
			return false;
		}
		
		return true;
	
	}
	
	
	
}
