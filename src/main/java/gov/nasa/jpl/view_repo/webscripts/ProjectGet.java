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

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
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
 * Descriptor at
 * /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/project.get.desc.xml
 *
 * @author cinyoung
 *
 */
public class ProjectGet extends AbstractJavaWebScript {
    public ProjectGet() {
        super();
    }

    public ProjectGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ProjectGet instance = new ProjectGet(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
//    	String userName = AuthenticationUtil.getRunAsUser();
        printHeader( req );

        //clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {
                String siteName = getSiteName( req );
                String projectId = getProjectId( req );

                // get timestamp if specified
                String timestamp = req.getParameter("timestamp");
                Date dateTime = TimeUtils.dateFromTimestamp(timestamp);

                WorkspaceNode workspace = getWorkspace( req );

                json = handleProject(projectId, siteName, workspace, dateTime);
                if (json != null && !Utils.isNullOrEmpty(response.toString())) json.put("message", response.toString());
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
            model.put("res", NodeUtil.jsonToString( json ));
        }
        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }

    /**
     * Get the project specified by the JSONObject
     *
     * @param projectId
     *            Project ID
     * @param siteName
     *            Site project should reside in
     * @return HttpStatusResponse code for success of the POST request
     * @throws JSONException
     */
    private JSONObject handleProject( String projectId, String siteName,
                                      WorkspaceNode workspace, Date dateTime )
                                              throws JSONException {
        EmsScriptNode projectNode = null;
        JSONObject json = null;

        EmsScriptNode siteNode = getSiteNodeForWorkspace( siteName, workspace, dateTime );

        if (siteNode == null) {
            projectNode = findScriptNodeByIdForWorkspace(projectId, workspace, dateTime, false);
        } else {
            projectNode = siteNode.childByNamePath("/Models/" + projectId);
            if (projectNode == null) {

            	// If the projectNode is not found, then try just looking for the project if the siteName
            	// was no_site.  This handles the situation that site was not specified, and a no_site
            	// is valid for the workspace:
            	if (siteName.equals(NO_SITE_ID)) {
            		projectNode = findScriptNodeByIdForWorkspace(projectId, workspace, dateTime, false);
            	}

            	// for backwards compatibility
            	if (projectNode == null) {
            		projectNode = siteNode.childByNamePath("/ViewEditor/" + projectId);
            	}
            }
        }
        if (projectNode == null) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find project");
            return null;
        }

        if (checkPermissions(projectNode, PermissionService.READ)) {
            log(Level.INFO, HttpServletResponse.SC_OK, "Found project");
			json = NodeUtil.newJsonObject();
            JSONArray elements = new JSONArray();
            JSONObject project = new JSONObject();
            JSONObject specialization = new JSONObject();

            json.put("elements", elements);
            elements.put(project);
            project.put(Acm.JSON_ID, projectId);
            project.put(Acm.JSON_NAME, projectNode.getProperty(Acm.CM_TITLE));
            project.put(Acm.JSON_SPECIALIZATION, specialization);
            specialization.put(Acm.JSON_PROJECT_VERSION, projectNode.getProperty(Acm.ACM_PROJECT_VERSION));
            specialization.put(Acm.JSON_TYPE, projectNode.getProperty(Acm.ACM_TYPE));
        } else {
            log(Level.ERROR, HttpServletResponse.SC_UNAUTHORIZED, "No permissions to read");
        }

        return json;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        String projectId = req.getServiceMatch().getTemplateVars()
                .get(PROJECT_ID);
        if (!checkRequestVariable(projectId, PROJECT_ID)) {
            return false;
        }

        return true;
    }
}
