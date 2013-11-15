package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.UserUtil;

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

public class ViewPostJson extends AbstractWebScript {
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
		String mdid = (String) element.get("mdid");
		String elementType = (String) element.get("type");
		String elementName = element.has("name") ? (String) element.getString("name") : null;

		ScriptNode modelNode = modelMapping.get(mdid);

		if (modelNode == null) {
			modelNode = getModelElement(modelFolder, mdid);
			if (modelNode == null) {
				String modeltype = typeMap.containsKey(elementType) ? typeMap.get(elementType) : typeMap.get("ModelElement");
				modelNode = createModelElement(modelFolder, mdid, modeltype);
				if (elementName != null) {
					setName(modelNode, elementName);
				}
			}
			modelNode.save();
		}
		
		if (elementName != null && !elementName.equals(modelNode.getProperties().get("view:name"))) {
			if (force) {
				setName(modelNode, elementName);
			} else {
				setName(modelNode, modelNode.getProperties().get("view:name") + " - MERGED - " + elementName);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "name");
			merged.add(mergedEntry);
		}
		
		String elementDocumentation = (String) element.get("documentation");
		if (!elementDocumentation.equals(modelNode.getProperties().get("view:documentation"))) {
			if (force) {
				modelNode.getProperties().put("view:documentation", elementDocumentation);
			} else {
				modelNode.getProperties().put("view:documentation", modelNode.getProperties().get("view:documentation") + " <p><strong><i> MERGED NEED RESOLUTION! </i></strong></p> " + elementDocumentation);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "doc");
			merged.add(mergedEntry);
		}
		
		String dvalue = (String)element.get("dvalue");
		if (elementType.equals("Property") && !dvalue.equals(modelNode.getProperties().get("view:defaultValue"))) {
			if (force) {
				modelNode.getProperties().put("view:defaultValue", dvalue);
			} else {
				modelNode.getProperties().put("view:defaultValue", modelNode.getProperties().get("view:defaultValue") + " - MERGED - " + dvalue);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "dvalue");
			merged.add(mergedEntry);
		}
		
		modelNode.getProperties().put("view:mdid", mdid);
		modelNode.save();
		modelMapping.put(mdid, modelNode);
		return modelNode;
	}

	
	private ScriptNode updateOrCreateView(JSONObject view, boolean ignoreNoSection) throws JSONException {
		String mdid = (String)view.get("mdid");
		ScriptNode viewNode = modelMapping.get(mdid);
		
		if (viewNode == null) {
			viewNode = getModelElement(modelFolder, mdid);
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
		viewNode.getProperties().put("view:sourcesJson", array.toString());
		
		if (view.get("noSection") != null && !ignoreNoSection) {
			viewNode.getProperties().put("view:noSection", view.get("noSection"));
		} else {
			viewNode.getProperties().put("view:noSection", false);
		}

		viewNode.getProperties().put("view:containsJson", view.getJSONArray("contains").toString());
		viewNode.getProperties().put("view:author", user);
		viewNode.getProperties().put("view:lastModified", new Date());
		viewNode.save();
		
		return viewNode;
	}


	private void fillSources(JSONObject contained, List<String> sources) throws JSONException {
		String type = (String)contained.get("type");
		String source = (String)contained.get("source");
		
		if (type.equals("Paragraph")) {
			if (source.equals("text")) {
				// do nothing
			} else {
				ScriptNode modelNode = modelMapping.get(source);
				if (!sources.contains((String)modelNode.getProperties().get("view:mdid"))) {
					sources.add((String)modelNode.getProperties().get("view:mdid"));
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


	protected boolean checkArgEquals(WebScriptRequest req, String name, String value) {
		if (req.getParameter(name) == null) {
			return false;
		}
		return req.getParameter(name).equals(value);
	}

	/**
	 * Main execution method
	 * @param req
	 */
	private void mainMethod(WebScriptRequest req) {
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
		
		ScriptNode topview = getModelElement(modelFolder, viewid);
		
		boolean product = false;
		if (checkArgEquals(req, "product", "true")) {
			product = true;
		}
		if (topview == null) {
			if (req.getParameter("doc").equals("true")) {
				topview = createModelElement(modelFolder, viewid, "view:DocumentView");
				if (product) {
					topview.getProperties().put("view:product", true);
				}
			} else {
				topview = createModelElement(modelFolder, viewid, "view:View");
			}
			topview.getProperties().put("view:mdid", viewid);
			topview.save();
		}
		modelMapping.put(viewid, topview);
		
		if ( !topview.getTypeShort().equals("view:DocumentView") && checkArgEquals(req, "doc", "true") ) {
			topview.specializeType("view:DocumentView");
			if (product) {
				topview.getProperties().put("view:product", true);
			}
			topview.save();
		}
		boolean force = checkArgEquals(req, "force", "true") ? true : false;
		
		try {
			JSONArray array;
			
			array = postjson.getJSONArray("elements");
			for (int ii = 0; ii < array.length(); ii++) {
				updateOrCreateModelElement((JSONObject) array.get(ii), force);
			}
			
			array = postjson.getJSONArray("views");
			for (int ii = 0; ii < array.length(); ii++) {
				updateOrCreateView((JSONObject)array.get(ii), product);
			}
			
			if (checkArgEquals(req, "recurse", "true") && !product) {
				updateViewHierarchy(modelMapping, postjson.getJSONArray("view2view"));
			}
			
			if (product) {
				List<String> noSections = new ArrayList<String>();
				for (int ii = 0; ii < array.length(); ii++) {
					JSONObject view = array.getJSONObject(ii);
					if (view.has("noSection")) {
						noSections.add((String)view.get("mdid"));
					}
				}
				topview.getProperties().put("view:view2viewJson", postjson.getJSONArray("view2view").toString());
				
				JSONArray nosectionjsa = new JSONArray(noSections);
				topview.getProperties().put("view:noSectionsJson", nosectionjsa.toString());
				topview.save();
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
			mainMethod(req);
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
