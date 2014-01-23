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
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ProductListGet extends AbstractJavaWebScript {
	private JSONObject productJson;
	private Set<EmsScriptNode> productSet;
	private EmsScriptNode projectNode;
    JSONObject volumes;
    JSONObject volume2volumes;
    JSONObject documents;
    JSONObject volume2documents;
    JSONArray projectVolumes;
	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
        String projectId = req.getServiceMatch().getTemplateVars().get("id");
        if (!checkRequestVariable(projectId, "id")) {
            return false;
        }
        
        SiteInfo siteInfo = services.getSiteService().getSite(projectId);
        if (siteInfo == null) {
            log(LogLevel.ERROR, "Project not found with id: " + projectId + ".\n", HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        projectNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
        
        if (!checkPermissions(projectNode, PermissionService.READ)) {
            return false;
        }
        
        return true;
	}
	
	@Override
	protected void clearCaches() {
		super.clearCaches();
		productJson = new JSONObject();
		productSet = new HashSet<EmsScriptNode>();
		projectNode = null;
        volumes = new JSONObject();
        volume2volumes = new JSONObject();
        documents = new JSONObject();
        volume2documents = new JSONObject();
        projectVolumes = new JSONArray();
	}
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();

		if (validateRequest(req, status)) {
        		try {
                    handleProductList();
                    model.put("res", productJson.toString(4));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                    model.put("res", response.toString());
                }
    		}

		status.setCode(responseStatus.getCode());
		return model;
	}

		
	private void handleProductList() throws JSONException {
	    String pattern = "ASPECT:\"" + "view2:Product" + "\"";
        if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
            ResultSet resultSet = null;
            try {
                // TODO: need to scope search for products to the projectNode
                resultSet = services.getSearchService().query(SEARCH_STORE, SearchService.LANGUAGE_LUCENE, pattern);
                for (ResultSetRow row: resultSet) {
                    EmsScriptNode node = new EmsScriptNode(row.getNodeRef(), services, response);
                    if (checkPermissions(node, PermissionService.READ)) {
                        productSet.add(node);
                    }
                }
            } catch (Exception e) {
                log(LogLevel.ERROR, "Could not parse search: " + pattern + ". " + e.getMessage() + "\n", HttpServletResponse.SC_BAD_REQUEST);  
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        }
	    
                
        for (EmsScriptNode node: productSet) {
            String id = (String)node.getProperty(Acm.ACM_ID);
            String name = (String)node.getProperty(Acm.ACM_NAME);
            documents.put(id, name);

            EmsScriptNode parent = node.getParent();
            String parentId = (String)parent.getProperty(Acm.ACM_ID);
            String parentName = (String)parent.getProperty("cm:name");
            if (parentName.contains("_pkg")) {
                parentId = parentName.replace("_pkg", "");
            }
            if (!volume2documents.has(parentId)) {
                volume2documents.put(parentId, new JSONArray());
            }
            ((JSONArray)volume2documents.get(parentId)).put(id);
            handleParents(node, "ViewEditor");
        }
        
        productJson.put("name", projectNode.getProperty("cm:title"));
        productJson.put("volumes", volumes);
        productJson.put("volume2volumes", volume2volumes);
        productJson.put("documents", documents);
        productJson.put("volume2documents", volume2documents);
        productJson.put("projectVolumes", projectVolumes);
	}
	
	protected void handleParents(EmsScriptNode node, String stopName) throws JSONException {
        String id = (String)node.getProperty(Acm.ACM_ID);
        String sysmlName = (String)node.getProperty(Acm.ACM_NAME);
        if (id == null) {
            String cmName = (String)node.getProperty("cm:name");
            id = cmName.replace("_pkg", "");
        }
        if (!documents.has(id)) {
            volumes.put(id, sysmlName);
        }
        
        EmsScriptNode parent = node.getParent();
        String parentSysmlName = (String)parent.getProperty(Acm.ACM_NAME);
        String parentId = (String)parent.getProperty(Acm.ACM_ID);
        String parentCmName = (String)parent.getProperty("cm:name");
        if (parentCmName.contains("_pkg")) {
            parentId = parentCmName.replace("_pkg", "");
        }
        if (parentId == null) {
            if (!projectVolumes.toString().contains(id)) {
                projectVolumes.put(id);
            }
        } else {
            parentSysmlName = (String)parent.getProperty("cm:name");
            if (parentSysmlName.contains("_pkg")) {
                parentId = parentSysmlName.replace("_pkg", "");
            }
            if (!volume2volumes.has(parentId)) {
                volume2volumes.put(parentId, new JSONArray());
            }
            if (!documents.has(id)) {
                JSONArray array = (JSONArray)volume2volumes.get(parentId);
                if (!array.toString().contains(id)) {
                    array.put(id);
                }
            }
            handleParents(parent, stopName);
        }
	}
}
