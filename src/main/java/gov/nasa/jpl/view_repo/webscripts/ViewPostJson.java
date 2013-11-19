package gov.nasa.jpl.view_repo.webscripts;

//import gov.nasa.jpl.view_repo.util.UserUtil;

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
			modelNode = getModelElement(modelFolder, mdid);
			if (modelNode == null) {
				String modeltype = typeMap.containsKey(elementType) ? typeMap.get(elementType) : typeMap.get("ModelElement");
				modelNode = createModelElement(modelFolder, mdid, modeltype);
				if (elementName != null) {
					setName(modelNode, elementName);
				}
			}
		}
		
		if (elementName != null && !elementName.equals(getNodeProperty(modelNode, "view:name"))) {
			if (force) {
				setName(modelNode, elementName);
			} else {
				setName(modelNode, getNodeProperty(modelNode, "view:name") + " - MERGED - " + elementName);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "name");
			merged.add(mergedEntry);
		}
		
		String elementDocumentation = element.has("documentation") ? (String) element.get("documentation") : null;
		if (elementDocumentation != null && !elementDocumentation.equals(getNodeProperty(modelNode, "view:documentation"))) {
			if (force) {
				setNodeProperty(modelNode, "view:documentation", elementDocumentation);
			} else {
				setNodeProperty(modelNode, "view:documentation", getNodeProperty(modelNode, "view:documentation") + " <p><strong><i> MERGED NEED RESOLUTION! </i></strong></p> " + elementDocumentation);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "doc");
			merged.add(mergedEntry);
		}
		
		String dvalue = element.has("dvalue") ? (String)element.get("dvalue") : null;
		if (elementType != null && elementType.equals("Property") && dvalue != null && !dvalue.equals(getNodeProperty(modelNode, "view:defaultValue"))) {
			if (force) {
				setNodeProperty(modelNode, "view:defaultValue", dvalue);
			} else {
				setNodeProperty(modelNode, "view:defaultValue", getNodeProperty(modelNode, "view:defaultValue") + " - MERGED - " + dvalue);
			}
			Map<String, String> mergedEntry = new HashMap<String, String>();
			mergedEntry.put("mdid", mdid);
			mergedEntry.put("type", "dvalue");
			merged.add(mergedEntry);
		}
		
		setNodeProperty(modelNode, "view:mdid", mdid);
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
		setNodeProperty(viewNode, "view:sourcesJson", array.toString());
		
		if (view.get("noSection") != null && !ignoreNoSection) {
			setNodeProperty(viewNode, "view:noSection", (Serializable)view.get("noSection"));
		} else {
			setNodeProperty(viewNode, "view:noSection", false);
		}

		setNodeProperty(viewNode, "view:containsJson", view.getJSONArray("contains").toString());
		setNodeProperty(viewNode, "view:author", user);
		setNodeProperty(viewNode, "view:lastModified", new Date());
		
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
				if (!sources.contains((String)getNodeProperty(modelNode, "view:mdid"))) {
					sources.add((String)getNodeProperty(modelNode, "view:mdid"));
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
		
		ScriptNode topview = getModelElement(modelFolder, viewid);
		
		boolean product = false;
		if (checkArgEquals(req, "product", "true")) {
			product = true;
		}
		if (topview == null) {
			if (checkArgEquals(req, "doc", "true")) {
				topview = createModelElement(modelFolder, viewid, "view:DocumentView");
				if (product) {
					setNodeProperty(topview, "view:product", true);
				}
			} else {
				topview = createModelElement(modelFolder, viewid, "view:View");
			}
			setNodeProperty(topview, "view:mdid", viewid);
		}
		modelMapping.put(viewid, topview);
		
		if ( !topview.getTypeShort().equals("view:DocumentView") && checkArgEquals(req, "doc", "true") ) {
			topview.specializeType("view:DocumentView");
			if (product) {
				setNodeProperty(topview, "view:product", true);
			}
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
				setNodeProperty(topview, "view:view2viewJson", postjson.getJSONArray("view2view").toString());
				
				JSONArray nosectionjsa = new JSONArray(noSections);
				setNodeProperty(topview, "view:noSectionsJson", nosectionjsa.toString());
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
//		if (UserUtil.hasWebScriptPermissions()) {
			status.setCode(HttpServletResponse.SC_OK);
			try {
				mainMethod(req);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//		} else {
//			status.setCode(HttpServletResponse.SC_UNAUTHORIZED);
//		}
		
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
