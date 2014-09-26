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
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
 * Retrieve a listing of all the documents for the appropriate project
 * @author cinyoung
 *
 */
public class ProductListGet extends AbstractJavaWebScript {
	public ProductListGet() {
	    super();
	}

    public ProductListGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    private JSONObject productJson;
	private Set<EmsScriptNode> productSet;
	private EmsScriptNode siteNode;
    JSONObject volumes;
    JSONObject volume2volumes;
    JSONObject documents;
    JSONObject volume2documents;
    JSONArray projectVolumes;

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {

        String siteId = req.getServiceMatch().getTemplateVars().get("id");
        if (!checkRequestVariable(siteId, "id")) {
            return false;
        }

        siteNode = getSiteNodeFromRequest(req);

        if (!checkPermissions(siteNode, PermissionService.READ)) {
            return false;
        }

        return true;
	}

	@Override
	protected void clearCaches() {
		super.clearCaches();
		siteNode = null;
	}

	protected void initDataStructs() {
        productJson = new JSONObject();
        productSet = new HashSet<EmsScriptNode>();
        volumes = new JSONObject();
        volume2volumes = new JSONObject();
        documents = new JSONObject();
        volume2documents = new JSONObject();
        projectVolumes = new JSONArray();
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

		clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();

		ProductListGet instance = new ProductListGet(repository, services);

		if (validateRequest(req, status)) {
        		try {
        	        // get timestamp if specified
        	        String timestamp = req.getParameter("timestamp");
        	        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        	    WorkspaceNode workspace = getWorkspace( req );
                JSONObject jsonObject =
                        instance.handleProductList( siteNode, workspace, dateTime );
                appendResponseStatusInfo(instance);
                if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
                model.put("res", jsonObject.toString(4));
                model.put("title", siteNode.getProperty(Acm.CM_TITLE));
                model.put("siteName", siteNode.getProperty(Acm.CM_NAME));
                model.put("siteTitle", siteNode.getProperty(Acm.CM_TITLE));
            } catch (JSONException e) {
                log(LogLevel.ERROR, "Could not create JSON Object", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                model.put("res", response.toString());
                model.put("title", "ERROR");
                model.put("siteName", "");
                model.put("siteTitle", "ERROR site not found");
                e.printStackTrace();
            }
    		} else {
    			model.put("res", "{}");
    			model.put("title", "Could not find site");
    		}

		status.setCode(responseStatus.getCode());

		printFooter();

		return model;
	}

	public Set<EmsScriptNode> getProductSet(String qnamePath,
	                                        WorkspaceNode workspace,
	                                        Date dateTime) {
        // TODO -- need to pass in workspace to getAllNodesInPath()
	    productSet =
                WebScriptUtil.getAllNodesInPath( qnamePath, "ASPECT",
                                                 Acm.ACM_PRODUCT, workspace,
                                                 dateTime,
                                                 services, response );

        return productSet;
	}

	public JSONObject handleProductList(EmsScriptNode pnode,
	                                    WorkspaceNode workspace,
	                                    Date dateTime) throws JSONException {
	    initDataStructs();
	    getProductSet(pnode.getQnamePath(), workspace, dateTime);

        for (EmsScriptNode node: productSet) {
            if (checkPermissions(node, PermissionService.READ)) {
                String id = (String)node.getProperty(Acm.ACM_ID);
                String name = (String)node.getProperty(Acm.ACM_NAME);
                documents.put(id, name);

                EmsScriptNode parent = node.getOwningParent(dateTime);
                String parentId = (String)parent.getProperty(Acm.ACM_ID);
                String parentName = (String)parent.getProperty(Acm.CM_NAME);
                if (parentName.contains("_pkg")) {
                    parentId = parentName.replace("_pkg", "");
                }
                if (!volume2documents.has(parentId)) {
                    volume2documents.put(parentId, new JSONArray());
                }
                ((JSONArray)volume2documents.get(parentId)).put(id);
                handleParents(node);
            }
        }

        productJson.put("name", pnode.getProperty(Acm.CM_TITLE));
        productJson.put("volumes", volumes);
        // lets clean volume2volumes - html page doesn't support empty volume2volumes
        Set<String> emptyV = new HashSet<String>();
        Iterator<?> v2v = volume2volumes.keys();
        while (v2v.hasNext()) {
            String vol = (String)v2v.next();
            if (volume2volumes.getJSONArray(vol).length() <= 0) {
                emptyV.add(vol);
            }
        }
        for (String r: emptyV) {
            volume2volumes.remove(r);
        }
        productJson.put("volume2volumes", volume2volumes);
        productJson.put("documents", documents);
        productJson.put("volume2documents", volume2documents);
        productJson.put("projectVolumes", projectVolumes);

        return productJson;
	}

	/**
	 * Work up the package hierarchy until the project folder is reached
	 * @param node
	 * @throws JSONException
	 */
	protected void handleParents(EmsScriptNode node) throws JSONException {
        String id = (String)node.getProperty(Acm.ACM_ID);
        String sysmlName = (String)node.getProperty(Acm.ACM_NAME);
        if (id == null) {
            String cmName = (String)node.getProperty(Acm.CM_NAME);
            id = cmName.replace("_pkg", "");
        } else {
            id = id.replace("_pkg", "");
        }
        if (!documents.has(id)) {
            volumes.put(id, sysmlName);
        }

        EmsScriptNode parent = node.getParent();
        if (checkPermissions(parent, PermissionService.READ)) {
            String parentSysmlName = (String)parent.getProperty(Acm.ACM_NAME);
            String parentId = (String)parent.getProperty(Acm.ACM_ID);
            String parentCmName = (String)parent.getProperty(Acm.CM_NAME);
            if (parentCmName.contains("_pkg")) {
                parentId = parentCmName.replace("_pkg", "");
            }
            if (parentId == null) {
                if (!projectVolumes.toString().contains(id)) {
                    projectVolumes.put(id);
                }
            } else {
                parentSysmlName = (String)parent.getProperty(Acm.CM_NAME);
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
                handleParents(parent);
            }
        }
	}

}
