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
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
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
        SiteGet instance = new SiteGet(repository, services);
        return instance.executeImplImpl( req, status, cache );
    }

    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {
                
                WorkspaceNode workspace = getWorkspace( req );
                
                JSONArray jsonArray = handleSite(workspace);
                json = new JSONObject();
                json.put("sites", jsonArray);
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

    /**
     * Get all the sites that are contained in the workspace, and create json with that
     * info in it.
     * 
     * @param workspace  The workspace to get sites for
     * @return json to return
     * @throws JSONException 
     */
    private JSONArray handleSite(WorkspaceNode workspace) throws JSONException {    	
    	    JSONArray json = new JSONArray();      
        EmsScriptNode emsNode;
        String name;
        NodeRef parentRef;
        
        // TODO: get all the sites in this workspace along w/ parent workspaces, as this
        //		 will not get the specific site folders created for a workspace
        List<SiteInfo> sites = services.getSiteService().listSites(null);
        
        // Create json array of info for each site in the workspace:
        //	Note: currently every workspace should contain every site created
        for (SiteInfo siteInfo : sites ) {
            	emsNode = new EmsScriptNode(siteInfo.getNodeRef(), services);
            	name = emsNode.getName();
            	parentRef = (NodeRef)emsNode.getProperty(Acm.ACM_SITE_PARENT);
            	
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
            		    
            		json.put(siteJson);
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

        String viewId = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
        if (!checkRequestVariable(viewId, WORKSPACE_ID)) {
            return false;
        }

        return true;
    }
}
