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

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
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
        int statusCode = HttpServletResponse.SC_OK;

        parseRequestVariables(req);
        try {
            if (validateRequest(req, status)) {
                statusCode = handleProject(projectId, siteName);
            } else {
                statusCode = responseStatus.getCode();
            }
        } catch (Exception e) {
            // this is most likely null pointer from poorly undefined request
            // parameters
            e.printStackTrace();
            log(LogLevel.ERROR, "Invalid request.\n",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        status.setCode(statusCode);
        model.put("res", response.toString());
        return model;
    }

    /**
     * Update or create the project specified by the JSONObject
     * 
     * @param projectId
     *            Project ID
     * @param siteName
     *            Site project should reside in
     * @return HttpStatusResponse code for success of the POST request
     */
    private int handleProject(String projectId, String siteName) {
        EmsScriptNode siteNode = new EmsScriptNode(services.getSiteService().getSite(siteName).getNodeRef(), services, response);
        
        if (siteNode.childByNamePath("ViewEditor/" + projectId) == null) {
//        
//        if (findScriptNodeByName(projectId) == null) {
            return HttpServletResponse.SC_NOT_FOUND;
        }

        return HttpServletResponse.SC_OK;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        // check site exists
        if (!checkRequestVariable(siteName, SITE_NAME)) {
            return false;
        }

        // get the site
        SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        if (!checkRequestVariable(siteInfo, "Site")) {
            return false;
        }

        // check permissions
        if (!checkPermissions(siteInfo.getNodeRef(), PermissionService.READ)) {
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
