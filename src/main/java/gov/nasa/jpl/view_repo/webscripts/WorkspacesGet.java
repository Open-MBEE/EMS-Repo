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
            try {
                model.put("res", json.toString(4));
            } catch ( JSONException e ) {
                e.printStackTrace();
            }
        }
        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
	}

	protected JSONObject handleWorkspace (EmsScriptNode homeFolder) throws JSONException{

		JSONObject json = new JSONObject ();
		JSONArray jArray = new JSONArray ();
        Collection <EmsScriptNode> nodes = NodeUtil.luceneSearchElements("ASPECT:\"ems:workspace\"" );

        for (EmsScriptNode workspaceNode: nodes){

        	JSONObject interiorJson = new JSONObject();

        	if (checkPermissions(workspaceNode, PermissionService.READ)){

//        		Map <String, Object> wkspProperties = new HashMap <String, Object> ();
//        		wkspProperties = workspaceNode.getProperties();
//        		for (String key: wkspProperties.keySet()) {
//        			interiorJson.put(key, wkspProperties.get(key));
//        		}

	        	//interiorJson.put(Acm.JSON_ID,workspaceNode.getProperty("ems:id"));
	        	//interiorJson.put(Acm.JSON_NAME, workspaceNode.getProperty(Acm.CM_TITLE));
	        	//interiorJson.put("sysml:parent", getStringIfNull(((WorkspaceNode) workspaceNode).getParentWorkspace().getSysmlId()));
	        	interiorJson.put("lastTimeSyncParent", getStringIfNull(workspaceNode.getProperty("ems:lastTimeSyncParent")));
	        	interiorJson.put("ems:parent", getStringIfNull(workspaceNode.getProperty("ems:parent")));
	        	interiorJson.put(Acm.JSON_TYPE, getStringIfNull(workspaceNode.getProperty(Acm.ACM_TYPE)));
        		interiorJson.put(Acm.JSON_ID, getStringIfNull(workspaceNode.getProperty(Acm.JSON_ID)));
        		interiorJson.put(Acm.JSON_NAME, getStringIfNull(workspaceNode.getProperty(Acm.CM_NAME)));
        		interiorJson.put("mergesource", getStringIfNull(workspaceNode.getProperty("ems:Mergesource")));
        		interiorJson.put("ems:Workspace", getStringIfNull(workspaceNode.getProperty("ems:Workspace")));
        	}
        	else {
        		log(LogLevel.WARNING,"No permission to read: "+ workspaceNode.getSysmlId(),HttpServletResponse.SC_NOT_FOUND);
        	}

        	jArray.put(interiorJson);
        }

        json.put("workspaces", jArray);
        return json;
	}

	protected Object getStringIfNull (Object obj){

		if (obj == null)
			return "null";
		else
			return obj;

	}

    /**
     * Validate the request and check some permissions
     */
    @Override
	protected boolean validateRequest (WebScriptRequest req, Status status){

		if (checkRequestContent(req)==false){
			return false;
		}

		return true;

	}



}
