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

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ConfigurationGenerationActionExecuter;
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
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		ConfigurationPost instance = new ConfigurationPost(repository, services);
        return instance.executeImplImpl(req, status, cache, runWithoutTransactions);		    
    }

	@Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
			Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

        printHeader( req );

        //clearCaches();

		ConfigurationPost instance = new ConfigurationPost(repository, services);

		JSONObject result = instance.saveAndStartAction(req, status);
		appendResponseStatusInfo(instance);

		status.setCode(responseStatus.getCode());
		if (result == null) {
		    model.put("res", createResponseJson());
		} else {
		    try {
		    	if (!Utils.isNullOrEmpty(response.toString())) result.put("message", response.toString());
                model.put("res", NodeUtil.jsonToString( result, 2 ));
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

        EmsScriptNode siteNode = getSiteNodeFromRequest( req, false );

        JSONObject reqPostJson = //JSONObject.make( 
                (JSONObject)req.parseContent();// );
		JSONObject postJson;
		try {
		    // for backwards compatibility
		    if (reqPostJson.has( "configurations" )) {
		        JSONArray configsJson = reqPostJson.getJSONArray( "configurations" );
		        postJson = configsJson.getJSONObject( 0 );
		    } else {
		        postJson = reqPostJson;
		    }

            String siteName = null;
            EmsScriptNode context = null;

            if ( siteNode == null ) {
                context = getSitesFolder( workspace );
            } else {
                context = siteNode;
                siteName = siteNode.getName();
            }
            if ( context == null ) {
                log( LogLevel.ERROR,
                     "Couldn't find a place to create the configuration: "
                             + postJson.getString( "name" ),
                     HttpServletResponse.SC_BAD_REQUEST );
            } else {
        			if (postJson.has("nodeid") || postJson.has( "id" )) {
        				jsonObject = handleUpdate(postJson, siteName, context, workspace, status);
        			} else {
        				jsonObject = handleCreate(postJson, siteName, context, workspace, status);
        			}
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

	private JSONObject handleCreate(JSONObject postJson, String siteName,
	                                EmsScriptNode context,
                                    WorkspaceNode workspace, Status status)
                                            throws JSONException {
	    EmsScriptNode jobNode = null;

		if (postJson.has("name")) {
		    String name = postJson.getString( "name" );
		    if ( ActionUtil.jobExists( context, name) ) {
		        return handleUpdate( postJson, siteName, context, workspace, status );
		    }

            jobNode = ActionUtil.getOrCreateJob( context, name,
                                                 "ems:ConfigurationSet",
                                                 status, response, true );

			if (jobNode != null) {
                ConfigurationsWebscript configWs =
                        new ConfigurationsWebscript( repository, services,
                                                     response );
                configWs.updateConfiguration( jobNode, postJson, context,
                                              workspace, new Date() );

	            HashSet<String> productList = getProductList(postJson);

	            // update configuration sets the timestamp, so lets grab timestamp from there
	            Date datetime = (Date) jobNode.getProperty( "view2:timestamp" );
                if (datetime != null) {
    	                // Check that the timestamp is before the workspace was branched/created
    	                // or in the future:
    	                Date now = new Date();
    	                if (datetime.after( now )) {
    	                    log(LogLevel.ERROR, "Timestamp provided in json: "+datetime+" is in the future.  Current time: "+now, 
    	                        HttpServletResponse.SC_BAD_REQUEST);
    	                    return null;
    	                }
    	                else if (workspace != null && datetime.before( workspace.getCopyOrCreationTime() )) {
    	                    log(LogLevel.ERROR, "Timestamp provided in json: "+datetime+" is before the workspace branch/creation time: "+workspace.getCopyOrCreationTime(), 
                                HttpServletResponse.SC_BAD_REQUEST);
    	                    return null;
    	                }
                }
	            startAction(jobNode, siteName, productList, workspace, datetime);

				return configWs.getConfigJson( jobNode, workspace, null );
			} else {
				log(LogLevel.ERROR, "Couldn't create configuration job: " + postJson.getString("name"), HttpServletResponse.SC_BAD_REQUEST);
				return null;
			}
		} else {
			log(LogLevel.ERROR, "Job name not specified", HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}
	}


	private JSONObject handleUpdate(JSONObject postJson,
	                                String siteName,
	                                EmsScriptNode context,
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
        EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services);

        if ( workspace != null && workspace.exists() ) {
            String cmName = configNode.getName();
            NodeRef r = NodeUtil.findNodeRefById( cmName, false, workspace, null,
                                                  getServices(), false );
            EmsScriptNode node = new EmsScriptNode( r, getServices() );
            if ( !workspace.equals( node.getWorkspace() ) ) {
                System.out.println("****************** " + node );
                configNode = workspace.replicateWithParentFolders( node );
            }
        }

		if ( NodeUtil.exists( configNodeRef ) ) {
	        ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
            configWs.updateConfiguration( configNode, postJson, context,
                                          workspace, null );
            return configWs.getConfigJson( configNode, workspace, null );
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
	 * @param workspace
	 */
	public void startAction(EmsScriptNode jobNode, String siteName,
	                        HashSet<String> productList, WorkspaceNode workspace,
	                        Date timestamp) {
		ActionService actionService = services.getActionService();
		Action configurationAction = actionService.createAction(ConfigurationGenerationActionExecuter.NAME);
		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_SITE_NAME, siteName);
		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_PRODUCT_LIST, productList);
		configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_TIME_STAMP, timestamp);
        configurationAction.setParameterValue(ConfigurationGenerationActionExecuter.PARAM_WORKSPACE, workspace);
		services.getActionService().executeAction(configurationAction, jobNode.getNodeRef(), true, true);
	}
}
