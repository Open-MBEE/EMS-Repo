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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor at
 * /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts
 * /gov/nasa/jpl/javawebscripts/project.get.desc.xml
 * 
 * @author cinyoung
 * 
 */
public class ProjectGet extends AbstractJavaWebScript {
    private String siteName = null;
    private String projectId = null;

    /**
     * Utility method for getting the request parameters from the URL template
     * 
     * @param req
     */
    private void parseRequestVariables(WebScriptRequest req) {
        siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        parseRequestVariables(req);
        try {
            if (validateRequest(req, status)) {
                json = handleProject(projectId, siteName);
            }
        } catch (Exception e) {
            // this is most likely null pointer from poorly undefined request parameters
            e.printStackTrace();
            log(LogLevel.ERROR, "Could not create JSON.\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        if (json == null) {
            model.put("res", response.toString());
        } else {
            model.put("res", json.toString());
        }
        status.setCode(responseStatus.getCode());
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
    private JSONObject handleProject(String projectId, String siteName) throws JSONException {
        EmsScriptNode projectNode;
        JSONObject json = null;
        
        if (siteName == null) {
            projectNode = findScriptNodeByName(projectId);
        } else {
            EmsScriptNode siteNode = new EmsScriptNode(services.getSiteService().getSite(siteName).getNodeRef(), services, response);
            projectNode = siteNode.childByNamePath("ViewEditor/" + projectId);
            if (projectNode == null) {
                log(LogLevel.ERROR, "Could not find project", HttpServletResponse.SC_NOT_FOUND);
                return null;
            }
        }
        
        if (checkPermissions(projectNode, PermissionService.READ)) {
            log(LogLevel.INFO, "Found project", HttpServletResponse.SC_OK);
            json = new JSONObject();
            json.put(Acm.JSON_ID, projectId);
            json.put(Acm.JSON_NAME, projectNode.getProperty(Acm.CM_TITLE));
            json.put(Acm.JSON_PROJECT_VERSION, projectNode.getProperty(Acm.ACM_PROJECT_VERSION));
        } else {
            log(LogLevel.ERROR, "No permissions to read", HttpServletResponse.SC_UNAUTHORIZED);
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
