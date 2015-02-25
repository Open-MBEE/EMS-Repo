package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Returns all the projects for a given workspace
 * 
 * @author gcgandhi
 *
 */
public class ProjectsGet extends AbstractJavaWebScript{

    public ProjectsGet() {
        super();
    }

    public ProjectsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl (WebScriptRequest req, Status status, Cache cache) {
        ProjectsGet instance = new ProjectsGet(repository, getServices());
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
        
        printHeader( req );

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = new JSONObject();

        try {
            if (validateRequest(req, status)) {
                WorkspaceNode workspace = getWorkspace( req );
                json = handleRequest (workspace, status);
            }
        } catch (JSONException e) {
            log(LogLevel.ERROR, "JSON could not be created\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } catch (Exception e) {
            log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    json.put("message", response.toString());
                }
                model.put("res", NodeUtil.jsonToString( json, 4 ));
            } catch ( JSONException e ) {
                log(LogLevel.ERROR, "JSON parse exception: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                if (!model.containsKey( "res" )) {
                    model.put( "res", createResponseJson() );
                }
                e.printStackTrace();
            }
        }

        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }

    /**
     * Returns all projects found in the given workspace
     * 
     * @param workspace
     * @param status
     * @return
     * @throws JSONException
     */
    protected JSONObject handleRequest (WorkspaceNode workspace, Status status) throws JSONException {
        
        JSONObject json = new JSONObject ();
        JSONArray elements = new JSONArray();
        json.put("elements", elements);

        Map< String, EmsScriptNode > nodeList = searchForElements(NodeUtil.SearchType.TYPE.prefix,
                                                                  Acm.ACM_PROJECT, false,
                                                                  workspace, null,
                                                                  null);

        if (nodeList != null) {
          
          for (EmsScriptNode node : nodeList.values()) {
              
              if (checkPermissions(node, PermissionService.READ)) {
                  
                  JSONObject project = new JSONObject();
                  JSONObject specialization = new JSONObject();
                  
                  elements.put(project);
                  project.put(Acm.JSON_ID, node.getSysmlId());
                  project.put(Acm.JSON_NAME, node.getProperty(Acm.CM_TITLE));
                  project.put(Acm.JSON_SPECIALIZATION, specialization);
                  specialization.put(Acm.JSON_PROJECT_VERSION, node.getProperty(Acm.ACM_PROJECT_VERSION));
                  specialization.put(Acm.JSON_TYPE, node.getProperty(Acm.ACM_TYPE));
              }
              else {
                  log(LogLevel.ERROR, "No permissions to read node: "+node, HttpServletResponse.SC_UNAUTHORIZED);
              }
          }
              
        }

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
