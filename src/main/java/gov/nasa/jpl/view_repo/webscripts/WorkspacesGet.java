package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
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


public class WorkspacesGet extends AbstractJavaWebScript{
    static Logger logger = Logger.getLogger( WorkspacesGet.class );

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
	    WorkspacesGet instance = new WorkspacesGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
	}

	/**
	 * Need wrapper for actual execution to be run in different instance since
	 * @param req
	 * @param status
	 * @param cache
	 * @return
	 */
    @Override
    protected Map<String, Object> executeImplImpl (WebScriptRequest req, Status status, Cache cache) {
        Timer timer = new Timer();
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {
                String userName = AuthenticationUtil.getRunAsUser();
                EmsScriptNode homeFolder = NodeUtil.getUserHomeFolder(userName);
                json = handleWorkspace (homeFolder, status, userName, req.getParameter( "deleted" ) == null ? false : true);
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON could not be created\n");
            e.printStackTrace();
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace:\n %s \n", e.getLocalizedMessage());
            e.printStackTrace();
        }
        if (json == null) {
            model.put("res", createResponseJson());
        } else {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) json.put("message", response.toString());
                model.put("res", NodeUtil.jsonToString( json, 4 ));
            } catch ( JSONException e ) {
                e.printStackTrace();
                model.put("res", createResponseJson());
            }
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

	protected JSONObject handleWorkspace (EmsScriptNode homeFolder, Status status, String user, boolean findDeleted) throws JSONException{
		JSONObject json = new JSONObject ();
		JSONArray jArray = new JSONArray ();
        if (!findDeleted) {
            //This is for the master workspace (not located in the user home folder).
            JSONObject interiorJson = new JSONObject();
            WorkspaceNode.addWorkspaceNamesAndIds(interiorJson, null, true );
            jArray.put(interiorJson);
        }

        Collection <EmsScriptNode> nodes = NodeUtil.luceneSearchElements("ASPECT:\"ems:workspace\"" );
        for (EmsScriptNode workspaceNode: nodes) {
            	if (checkPermissions(workspaceNode, PermissionService.READ)){
            	    WorkspaceNode wsNode = new WorkspaceNode(workspaceNode.getNodeRef(), services, response);
            	    if (findDeleted) {
            	        if (wsNode.isDeleted()) {
            	            jArray.put(wsNode.toJSONObject(wsNode, null, false ));
            	        }
            	    } else {
            	        if (wsNode.exists()) {
            	            jArray.put(wsNode.toJSONObject(wsNode, null, false ));
            	        }
            	    }
            }
        }

        json.put("workspaces", jArray);
        return json;
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
