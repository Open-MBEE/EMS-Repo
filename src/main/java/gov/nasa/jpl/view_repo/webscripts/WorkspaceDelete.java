package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class WorkspaceDelete extends AbstractJavaWebScript {
    public WorkspaceDelete() {
        super();
    }

    public WorkspaceDelete(Repository repositoryHelper, ServiceRegistry service) {
        super(repositoryHelper, service);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        String wsId = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
        if(checkRequestVariable(wsId, WORKSPACE_ID) == false) {
            return false;
        }
        return true;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        WorkspaceDelete instance = new WorkspaceDelete( repository, getServices() );
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
       printHeader(req);
       clearCaches();
       Map<String, Object> model = new HashMap<String, Object>();
       String user = AuthenticationUtil.getRunAsUser();
       JSONObject result = null;
       try {
           if( validateRequest(req, status) ){
               String wsId = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);

               // can't delete master
               if (wsId.equals( "master") ) {
                   log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Cannot delete master workspace");
                   status.setCode(HttpServletResponse.SC_BAD_REQUEST);
               } else {
                   WorkspaceNode target = WorkspaceNode.getWorkspaceFromId(wsId, getServices(),
                                                                           getResponse(), status, user);
                   if (target != null) {
                       result = printObject(target);
                       target.delete( true );
                       status.setCode(HttpServletResponse.SC_OK);
                   } else {
                       log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find workspace %s", wsId);
                       status.setCode(HttpServletResponse.SC_NOT_FOUND);
                   }
               }
           }
       } catch (JSONException e) {
           status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
           log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON object could not be created \n");
           e.printStackTrace();
       } catch (Exception e) {
           status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
           log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal stack trace error \n");
           e.printStackTrace();
       }

       if (result == null) {
          model.put("res", response.toString());
       }
       else {
           try {
               if (!Utils.isNullOrEmpty(response.toString())) result.put("message", response.toString());
               model.put("res", result.toString(4));
           } catch (JSONException e) {
               e.printStackTrace();
           }
       }
       printFooter();
       return model;
    }

    private JSONObject printObject(WorkspaceNode workspace) throws JSONException{
    	JSONObject json = new JSONObject();

    	return json;
    }
}

