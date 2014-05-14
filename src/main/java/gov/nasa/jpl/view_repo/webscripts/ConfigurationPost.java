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

import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ConfigurationGenerationActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Handle the creation of configuration sets for a particular site
 * 
 * @author cinyoung
 * 
 */
public class ConfigurationPost extends AbstractJavaWebScript {
	public ConfigurationPost() {
		super();
	}

	public ConfigurationPost(Repository repositoryHelper,
			ServiceRegistry registry) {
		super(repositoryHelper, registry);
	}

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// do nothing
		return false;
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

        printHeader( req );

        clearCaches();

		ConfigurationPost instance = new ConfigurationPost(repository, services);

		instance.saveAndStartAction(req, status);
		appendResponseStatusInfo(instance);

		status.setCode(responseStatus.getCode());
		model.put("res", response.toString());

        printFooter();

		return model;
	}

	/**
	 * Save off the configuration set and kick off snapshot creation in
	 * background
	 * 
	 * @param req
	 * @param status
	 */
	private void saveAndStartAction(WebScriptRequest req, Status status) {
		String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
		if (siteName == null) {
			log(LogLevel.ERROR, "No sitename provided", HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
		if (siteInfo == null) {
			log(LogLevel.ERROR, "Could not find site: " + siteName, HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);

		JSONObject postJson = (JSONObject) req.parseContent();
		try {
			if (postJson.has("nodeid")) {
				handleUpdate(postJson, siteNode, status);
			} else {
				handleCreate(postJson, siteNode, status);
			}
		} catch (JSONException e) {
			log(LogLevel.ERROR, "Could not parse JSON", HttpServletResponse.SC_BAD_REQUEST);
			e.printStackTrace();
			return;
		}

	}

	
	private HashSet<String> getProductList(JSONObject postJson) throws JSONException {
		HashSet<String> productList = new HashSet<String>();
		String keys[] = {"products", "snapshots"};
		for (String key: keys) {
			if (postJson.has(key)) {
				JSONArray documents = postJson.getJSONArray(key);
				for (int ii = 0; ii < documents.length(); ii++) {
					productList.add(documents.getString(ii));
				}
			}
		}
		return productList;
	}
	
	private boolean handleCreate(JSONObject postJson, EmsScriptNode siteNode, Status status) throws JSONException {
		String siteName = (String)siteNode.getProperty(Acm.CM_NAME);
		EmsScriptNode jobNode = null;
		
		if (postJson.has("name")) {
			jobNode = ActionUtil.getOrCreateJob(siteNode, postJson.getString("name"), "ems:ConfigurationSet", status, response);
			
			if (jobNode != null) {
				if (postJson.has("description")) {
					jobNode.createOrUpdateProperty("cm:description", postJson.getString("description"));
				}
				startAction(jobNode, siteName, getProductList(postJson));
			} else {
				log(LogLevel.ERROR, "Couldn't create configuration job: " + postJson.getString("name"), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return false;
			}
		} else {
			log(LogLevel.ERROR, "Job name not specified", HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		
		return true;
	}
	
	private boolean handleUpdate(JSONObject postJson, EmsScriptNode siteNode, Status status) throws JSONException {
		String nodeId = postJson.getString("nodeid");
		EmsScriptNode jobNode = null;
		
		List<NodeRef> nodeRefs = NodeRef.getNodeRefs(nodeId);
		if (nodeRefs.size() > 0) {
			jobNode = new EmsScriptNode(nodeRefs.get(0), services, response);
			if (postJson.has("name")) {
				jobNode.createOrUpdateProperty(Acm.CM_NAME, postJson.getString("name"));
			}
			if (postJson.has("description")) {
				jobNode.createOrUpdateProperty("cm:description", postJson.getString("description"));
			}
			if (postJson.has("snapshots")) {
				// clear out existing list of references
				jobNode.removeAssociations("ems:configuredSnapshots");
				
				// get the snapshot folder
				EmsScriptNode snapshotFolder = siteNode.childByNamePath("/snapshots");
				
				HashSet<String> productList = getProductList(postJson);
				for (String product: productList) {
					EmsScriptNode productNode = snapshotFolder.childByNamePath("/" + product);
					if (productNode != null) {
						jobNode.createOrUpdateAssociation(productNode, "ems:configuredSnapshots", true);
					}
				}
			}
		} else {
			log(LogLevel.ERROR, "Could not find configuration to update for: " + nodeId, HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Kick off the actual action in the background
	 * @param jobNode
	 * @param siteName
	 * @param productList
	 */
	private void startAction(EmsScriptNode jobNode, String siteName, HashSet<String> productList) {
		ActionService actionService = services.getActionService();
		Action configurationAction = actionService.createAction(ConfigurationGenerationActionExecuter.NAME);
		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_SITE_NAME, siteName);
		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_PRODUCT_LIST, productList);
		services.getActionService().executeAction(configurationAction, jobNode.getNodeRef(), true, true);
	}
}
