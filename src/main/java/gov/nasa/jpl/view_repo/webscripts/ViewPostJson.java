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

import gov.nasa.jpl.view_repo.util.UserUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ViewPostJson extends AbstractJavaWebScript {
	private ScriptNode modelFolder = null;
	// snapshotFolder not needed here
	private Map<String, ScriptNode> modelMapping;
	private List<Map<String, String>> merged;
	private String user;

	/**
	 * Utility for initializing member variables as necessary
	 */
	@Override
	protected void initMemberVariables(String siteName) {
		super.initMemberVariables(siteName);
		
		String site = siteName;
		if (site == null) {
			site = "europa";
		} 
		modelFolder = companyhome.childByNamePath("Sites/" + site + "/ViewEditor/model");
		
		modelMapping = new HashMap<String, ScriptNode>();
		merged = new ArrayList<Map<String, String>>();
		user = AuthenticationUtil.getFullyAuthenticatedUser();
	}


	private ScriptNode updateOrCreateModelElement(JSONObject element, boolean force) throws JSONException {
		String mdid = element.has("mdid") ? (String) element.get("mdid") : null;
		String elementType = element.has("type") ? (String) element.get("type") : null;
		String elementName = element.has("name") ? (String) element.getString("name") : null;

		ScriptNode modelNode = modelMapping.get(mdid);

		if (modelNode == null) {
			modelNode = jwsUtil.getModelElement(modelFolder, mdid);
			if (modelNode == null) {
				String modeltype = typeMap.containsKey(elementType) ? typeMap.get(elementType) : typeMap.get("ModelElement");
				modelNode = jwsUtil.createModelElement(modelFolder, mdid, modeltype);
				if (elementName != null) {
					jwsUtil.setName(modelNode, elementName);
				}
			}
		}
		
		if (elementName != null && !elementName.equals(jwsUtil.getNodeProperty(modelNode, "view:name"))) {
			if (force) {
				jwsUtil.setName(modelNode, elementName);
			} else {
				jwsUtil.setName(modelNode, jwsUtil.getNodeProperty(modelNode, "view:name") + " - MERGED - " + elementName);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "name");
			merged.add(mergedEntry);
		}
		
		String elementDocumentation = element.has("documentation") ? (String) element.get("documentation") : null;
		if (elementDocumentation != null && !elementDocumentation.equals(jwsUtil.getNodeProperty(modelNode, "view:documentation"))) {
			if (force) {
				jwsUtil.setNodeProperty(modelNode, "view:documentation", elementDocumentation);
			} else {
				jwsUtil.setNodeProperty(modelNode, "view:documentation", jwsUtil.getNodeProperty(modelNode, "view:documentation") + " <p><strong><i> MERGED NEED RESOLUTION! </i></strong></p> " + elementDocumentation);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "doc");
			merged.add(mergedEntry);
		}
		
		String dvalue = element.has("dvalue") ? (String)element.get("dvalue") : null;
		if (elementType != null && elementType.equals("Property") && dvalue != null && !dvalue.equals(jwsUtil.getNodeProperty(modelNode, "view:defaultValue"))) {
			if (force) {
				jwsUtil.setNodeProperty(modelNode, "view:defaultValue", dvalue);
			} else {
				jwsUtil.setNodeProperty(modelNode, "view:defaultValue", jwsUtil.getNodeProperty(modelNode, "view:defaultValue") + " - MERGED - " + dvalue);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "dvalue");
			merged.add(mergedEntry);
		}
		
		jwsUtil.setNodeProperty(modelNode, "view:mdid", mdid);
		modelMapping.put(mdid, modelNode);
		return modelNode;
	}

	
	private ScriptNode updateOrCreateView(JSONObject view, boolean ignoreNoSection) throws JSONException {
		String mdid = (String)view.get("mdid");
		ScriptNode viewNode = modelMapping.get(mdid);
		
		if (viewNode == null) {
			viewNode = jwsUtil.getModelElement(modelFolder, mdid);
			if (viewNode == null) {
				return null;
			}
		}
		
		List<String> sources = new ArrayList<String>();
		JSONArray array;
		array = view.getJSONArray("contains");
		for (int ii = 0; ii < array.length(); ii++) {
			fillSources((JSONObject)array.get(ii), sources);
		}
		array = new JSONArray(sources);
		jwsUtil.setNodeProperty(viewNode, "view:sourcesJson", array.toString());
		
		if (view.get("noSection") != null && !ignoreNoSection) {
			jwsUtil.setNodeProperty(viewNode, "view:noSection", (Serializable)view.get("noSection"));
		} else {
			jwsUtil.setNodeProperty(viewNode, "view:noSection", false);
		}

		jwsUtil.setNodeProperty(viewNode, "view:containsJson", view.getJSONArray("contains").toString());
		jwsUtil.setNodeProperty(viewNode, "view:author", user);
		jwsUtil.setNodeProperty(viewNode, "view:lastModified", new Date());
		
		return viewNode;
	}


	private void fillSources(JSONObject contained, List<String> sources) throws JSONException {
		String type = contained.has("type") ? (String)contained.get("type") : null;
		String source = contained.has("source") ? (String)contained.get("source") : null;
		
		if (type != null && type.equals("Paragraph")) {
			if (source == null || source.equals("text")) {
				// do nothing
			} else {
				ScriptNode modelNode = modelMapping.get(source);
				if (!sources.contains((String)jwsUtil.getNodeProperty(modelNode, "view:mdid"))) {
					sources.add((String)jwsUtil.getNodeProperty(modelNode, "view:mdid"));
				}
			}
		} else if (type.equals("Table") || type.equals("List")) {
			JSONArray array = contained.getJSONArray("sources");
			for (int ii = 0; ii < array.length(); ii++) {
				String sourceid = array.getString(ii); 
				if (!sources.contains(sourceid)) {
					sources.add(sourceid);
				}
			}	
		}
	}


	/**
	 * Main execution method
	 * @param req
	 * @throws InterruptedException 
	 */
	private void mainMethod(WebScriptRequest req) throws InterruptedException {
		JSONObject postjson = null;
		String viewid = null; 

		initMemberVariables(null);

		if (req.getContent() == null) {
			return; // TODO should probably return with failure status code...
		}
		
		postjson = (JSONObject)req.parseContent();
		if (postjson == null) {
			return;
		}
		
		viewid = req.getServiceMatch().getTemplateVars().get("viewid");
		if (viewid == null) {
			return;
		}
		
		ScriptNode topview = jwsUtil.getModelElement(modelFolder, viewid);
		
		boolean product = false;
		if (jwsUtil.checkArgEquals(req, "product", "true")) {
			product = true;
		}
		if (topview == null) {
			if (jwsUtil.checkArgEquals(req, "doc", "true")) {
				topview = jwsUtil.createModelElement(modelFolder, viewid, "view:DocumentView");
				if (product) {
					jwsUtil.setNodeProperty(topview, "view:product", true);
				}
			} else {
				topview = jwsUtil.createModelElement(modelFolder, viewid, "view:View");
			}
			jwsUtil.setNodeProperty(topview, "view:mdid", viewid);
		}
		modelMapping.put(viewid, topview);
		
		if ( !topview.getTypeShort().equals("view:DocumentView") && jwsUtil.checkArgEquals(req, "doc", "true") ) {
			topview.specializeType("view:DocumentView");
			if (product) {
				jwsUtil.setNodeProperty(topview, "view:product", true);
			}
		}
		boolean force = jwsUtil.checkArgEquals(req, "force", "true") ? true : false;
		
		try {
			JSONArray array;
			
			array = postjson.getJSONArray("elements");
			int interval = 100;
			
			jwsUtil.splitTransactions(new JwsFunctor() {
				@Override
				public Object execute(JSONArray jsonArray, int index,
						Boolean... flags) throws JSONException {
					updateOrCreateModelElement((JSONObject)jsonArray.get(index), flags[0]);
					return null;
				}
			}, array, force);
			
			array = postjson.getJSONArray("views");
			jwsUtil.splitTransactions(new JwsFunctor() {
				@Override
				public Object execute(JSONArray jsonArray, int index,
						Boolean... flags) throws JSONException {
					updateOrCreateView((JSONObject)jsonArray.get(index), flags[0]);
					return null;
				}
			}, array, product);

			if (jwsUtil.checkArgEquals(req, "recurse", "true") && !product) {
				updateViewHierarchy(modelMapping, postjson.getJSONObject("view2view"));
			}
			
			if (product) {
				List<String> noSections = new ArrayList<String>();
				for (int ii = 0; ii < array.length(); ii++) {
					JSONObject view = array.getJSONObject(ii);
					if (view.has("noSection")) {
						noSections.add((String)view.get("mdid"));
					}
				}
				jwsUtil.setNodeProperty(topview, "view:view2viewJson", postjson.getJSONArray("view2view").toString());
				
				JSONArray nosectionjsa = new JSONArray(noSections);
				jwsUtil.setNodeProperty(topview, "view:noSectionsJson", nosectionjsa.toString());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * Webscript entry point
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();
		String response;
	
		// check user site permissions for webscript
		if (UserUtil.hasWebScriptPermissions()) {
			status.setCode(HttpServletResponse.SC_OK);
			try {
				mainMethod(req);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			status.setCode(HttpServletResponse.SC_UNAUTHORIZED);
		}
		
		// set response based on status
		switch (status.getCode()) {
		case HttpServletResponse.SC_OK:
			response = "ok";
			if (!merged.isEmpty()) {
				// response = jsonUtils.toJSONString(merged);
			}
			break;
		case HttpServletResponse.SC_UNAUTHORIZED:
			response = "unauthorized";
			break;
		case HttpServletResponse.SC_NOT_FOUND:
		default:
			response = "NotFound";
		}
		model.put("res", response);
		
		return model;
	}


}
