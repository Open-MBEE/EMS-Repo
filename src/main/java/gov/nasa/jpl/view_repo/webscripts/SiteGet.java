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
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 *
 * @author gcgandhi
 *
 */
public class SiteGet extends AbstractJavaWebScript {

    public SiteGet() {
        super();
    }

    public SiteGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        SiteGet instance = new SiteGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        //clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {

                WorkspaceNode workspace = getWorkspace( req );
                String timestamp = req.getParameter( "timestamp" );
                Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

                JSONArray jsonArray = handleSite(workspace, dateTime);
                json = new JSONObject();
                json.put("sites", jsonArray);
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
     * Get all the sites that are contained in the workspace, and create json with that
     * info in it.
     *
     * @param workspace  The workspace to get sites for
     * @param dateTime The time to base the site retrieval, or null
     * @return json to return
     * @throws JSONException
     */
    private JSONArray handleSite(WorkspaceNode workspace, Date dateTime) throws JSONException {

        JSONArray json = new JSONArray();
        EmsScriptNode emsNode;
        String name;
        NodeRef parentRef;
        NodeRef siteRef;

        List<SiteInfo> sites = services.getSiteService().listSites(AuthenticationUtil.getFullyAuthenticatedUser());

        // Create json array of info for each site in the workspace:
        //	Note: currently every workspace should contain every site created
        for (SiteInfo siteInfo : sites ) {
            siteRef = siteInfo.getNodeRef();
            if ( dateTime != null ) {
                siteRef = NodeUtil.getNodeRefAtTime( siteRef, dateTime );
            }

            if (siteRef != null) {
                	emsNode = new EmsScriptNode(siteRef, services);
                	name = emsNode.getName();
                    // Note: skipping the noderef check b/c our node searches return the noderefs that correspond
                    //       to the nodes in the surf-config folder. 
                	parentRef = (NodeRef)emsNode.getProperty(Acm.ACM_SITE_PARENT, true);

                	EmsScriptNode parentNode = null;
                	String parentId = null;
                	if (parentRef != null) {
                	    parentNode = new EmsScriptNode(parentRef, services, response);
                	    parentId = parentNode.getSysmlId();
                	}

                	// Note: workspace is null if its the master, and in that case we consider it to contain
                	//		 all sites.
                	if (!name.equals("no_site") &&
                		(workspace == null || (workspace != null && workspace.contains(emsNode))) ) {

                		JSONObject siteJson = new JSONObject();
                		siteJson.put("sysmlid", name);
                		siteJson.put("name", siteInfo.getTitle());
                		siteJson.put("parent", parentId );
                		siteJson.put("isCharacterization", emsNode.getProperty(Acm.ACM_SITE_PACKAGE) != null );

                		json.put(siteJson);
                	}
            }
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

        String id = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
        if (!checkRequestVariable(id, WORKSPACE_ID)) {
            return false;
        }

        return true;
    }
}
