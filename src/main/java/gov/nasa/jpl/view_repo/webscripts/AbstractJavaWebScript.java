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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities
 * that were in utils.js
 * 
 * @author cinyoung
 * 
 */
public abstract class AbstractJavaWebScript extends DeclarativeWebScript {
	// injected members
	protected ServiceRegistry services;
	protected Repository repository;
	protected JwsUtil jwsUtil;

	// internal members
	protected ScriptNode companyhome;

	protected final Map<String, String> typeMap = new HashMap<String, String>() {
		private static final long serialVersionUID = -5467934440503910163L;
		{
			put("View", "view:View");
			put("Property", "view:Property");
			put("Comment", "view:Comment");
			put("ModelElement", "view:ModelElement");
		}
	};
	private Exception UnsupportedOperationException;

	protected void initMemberVariables(String siteName) {
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}

	public void setJwsUtil(JwsUtil util) {
		jwsUtil = util;
	}

	protected void updateViewHierarchy(Map<String, ScriptNode> modelMapping,
			JSONObject views) throws JSONException {
		updateViewHierarchy(modelMapping, views, null);
	}

	protected void updateViewHierarchy(Map<String, ScriptNode> modelMapping,
			JSONObject views, List<String> nosections) throws JSONException {
		Iterator<?> keys = views.keys();
		while (keys.hasNext()) {
			String pview = (String) keys.next();
			JSONArray cviews = views.getJSONArray(pview);
			if (!modelMapping.containsKey(pview)) {
				continue;
			}
			ScriptNode pviewnode = modelMapping.get(pview);
			JSONArray oldchildren = (JSONArray) pviewnode.getAssocs().get(
					"view:views");
			if (oldchildren == null) {
				continue;
			}
			for (int ii = 0; ii < oldchildren.length(); ii++) {
				pviewnode.removeAssociation((ScriptNode) oldchildren.get(ii),
						"view:views");
			}
			for (int ci = 0; ci < cviews.length(); ci++) {
				String cvid = cviews.getString(ci);
				ScriptNode cviewnode = modelMapping.get(cvid);
				if (cviewnode == null) {
					continue;
				}
				jwsUtil.setNodeProperty(cviewnode, "view:index", ci);
				pviewnode.createAssociation(cviewnode, "view:views");
			}
			jwsUtil.setNodeProperty(pviewnode, "view:viewsJson",
					cviews.toString());
			if (nosections != null && nosections.contains(pview)) {
				jwsUtil.setNodeProperty(pviewnode, "view:noSection", true);
			}
		}
	}

	/**
	 * Clear all parent volume associations
	 * 
	 * @param dnode
	 */
	protected void cleanDocument(ScriptNode dnode) {
		JSONArray pvs = (JSONArray) dnode.getSourceAssocs().get(
				"view:documents");
		if (pvs != null) {
			for (int ii = 0; ii < pvs.length(); ii++) {
				// TODO: convert pv to ScriptNode to remove?
			}
		}
	}


	/**
	 * Utility to grab template parameters from a request
	 */
	protected String getRequestVar(WebScriptRequest req, String varid) {
		return req.getServiceMatch().getTemplateVars().get(varid);
	}
	
	/**
	 * Parse the request and do validation checks on request
	 * 
	 * @param req		Request to be parsed
	 * @param status	The status to be returned for the request
	 * @param response	The response message to returned to the request
	 * @return			True if request valid and parsed, False otherwise
	 * @throws Exception Must be overridden
	 */
	abstract protected boolean parseRequest(WebScriptRequest req, Status status, StringBuffer response);
}
