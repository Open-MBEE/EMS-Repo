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
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ConfigurationGenerationActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
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

		JSONObject result = instance.saveAndStartAction(req, status);
		appendResponseStatusInfo(instance);

		status.setCode(responseStatus.getCode());
		if (result == null) {
		    model.put("res", response.toString());
		} else {
		    try {
		    	if (!Utils.isNullOrEmpty(response.toString())) result.put("message", response.toString());
                model.put("res", result.toString(2));
            } catch ( JSONException e ) {
                e.printStackTrace();
            }
		}

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
	private JSONObject saveAndStartAction(WebScriptRequest req, Status status) {
	    JSONObject jsonObject = null;

        WorkspaceNode workspace = getWorkspace( req );

        EmsScriptNode siteNode = getSiteNodeFromRequest( req );

		JSONObject reqPostJson = (JSONObject) req.parseContent();
		JSONObject postJson;
		try {
		    // for backwards compatibility
		    if (reqPostJson.has( "configurations" )) {
		        JSONArray configsJson = reqPostJson.getJSONArray( "configurations" );
		        postJson = configsJson.getJSONObject( 0 );
		    } else {
		        postJson = reqPostJson;
		    }

			if (postJson.has("nodeid") || postJson.has( "id" )) {
				jsonObject = handleUpdate(postJson, siteNode, workspace, status);
			} else {
				jsonObject = handleCreate(postJson, siteNode, workspace, status);
			}
		} catch (JSONException e) {
			log(LogLevel.ERROR, "Could not parse JSON", HttpServletResponse.SC_BAD_REQUEST);
			e.printStackTrace();
			return null;
		}

		return jsonObject;
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

	private JSONObject handleCreate(JSONObject postJson, EmsScriptNode siteNode,
                                    WorkspaceNode workspace, Status status)
                                            throws JSONException {
		String siteName = (String)siteNode.getProperty(Acm.CM_NAME);
		EmsScriptNode jobNode = null;

		if (postJson.has("name")) {
		    Date date = new Date();
			jobNode = ActionUtil.getOrCreateJob(siteNode, postJson.getString("name"), "ems:ConfigurationSet", status, response);

			if (jobNode != null) {
	            ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
	            configWs.updateConfiguration( jobNode, postJson, siteNode, workspace, date );

	            HashSet<String> productList = getProductList(postJson);
	            
	            // Only need to start the background action if there are products, otherwise
	            // need to set the job status to succeeded as this is usually done by the 
	            // the background job.  Note, that if jobNode is non-null, we know that there
	            // was not a previous job for the configuration still in progress.
	            if (productList.size() > 0) {
	            	String timestamp = postJson.getString("timestamp");
	            	Date datetime = TimeUtils.dateFromTimestamp(timestamp);
	            	startAction(jobNode, siteName, productList, datetime);
	            }
	            else {
	                jobNode.setProperty("ems:job_status", "Succeeded");
	            }
	            
				return configWs.getConfigJson( jobNode, siteName, workspace, null );
			} else {
				log(LogLevel.ERROR, "Couldn't create configuration job: " + postJson.getString("name"), HttpServletResponse.SC_BAD_REQUEST);
				return null;
			}
		} else {
			log(LogLevel.ERROR, "Job name not specified", HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}
	}


	private JSONObject handleUpdate(JSONObject postJson, EmsScriptNode siteNode,
	                                WorkspaceNode workspace, Status status)
	                                        throws JSONException {
		String[] idKeys = {"nodeid", "id"};
	    String nodeId = null;

		for (String key: idKeys) {
		    if (postJson.has(key)) {
		        nodeId = postJson.getString(key);
		        break;
		    }
		}

		if (nodeId == null) {
		    log(LogLevel.WARNING, "JSON malformed does not include Alfresco id for configuration", HttpServletResponse.SC_BAD_REQUEST);
		    return null;
		}

		NodeRef configNodeRef = NodeUtil.getNodeRefFromNodeId( nodeId );
		if (configNodeRef != null) {
	        EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services);
	        ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
            configWs.updateConfiguration( configNode, postJson, siteNode,
                                          workspace, null );
            return configWs.getConfigJson( configNode,
                                           (String)siteNode.getProperty( Acm.CM_NAME ),
                                           workspace, null );
		} else {
		    log(LogLevel.WARNING, "Could not find configuration with id " + nodeId, HttpServletResponse.SC_NOT_FOUND);
		    return null;
		}
	}


	/**
	 * Kick off the actual action in the background
	 * @param jobNode
	 * @param siteName
	 * @param productList
	 */
	public void startAction(EmsScriptNode jobNode, String siteName, HashSet<String> productList, Date timestamp) {
	    if (productList.size() > 0) {
        		ActionService actionService = services.getActionService();
        		Action configurationAction = actionService.createAction(ConfigurationGenerationActionExecuter.NAME);
        		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_SITE_NAME, siteName);
        		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_PRODUCT_LIST, productList);
        		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_TIME_STAMP, timestamp);
        		services.getActionService().executeAction(configurationAction, jobNode.getNodeRef(), true, true);
	    }
	}
}
