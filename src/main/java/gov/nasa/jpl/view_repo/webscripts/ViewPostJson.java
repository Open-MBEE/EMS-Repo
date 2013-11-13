package gov.nasa.jpl.view_repo.webscripts;

//import gov.nasa.jpl.view_repo.util.UserUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ViewPostJson extends AbstractWebScript {
	// internal members
	private NodeRef modelFolder = null;
	private NodeRef snapshotFolder = null;
	
	private static Map<String,String> param2TypeMap = new HashMap<String,String>() {
		{
			put("doc", "view:DocumentView");
			put("product", "view:product");
			put("view2view", "view:view2viewJson");
			put("noSection", "view:noSectionsJson");
		};
	};
	
	private String user;

	private Map<String, NodeRef> modelMapping = new HashMap<String, NodeRef>();
	private List<Map<String, String>> merged = new ArrayList<Map<String, String>>();
	    
    
	/**
	 * 
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();
		String response;
	
		// check user site permissions for webscript
//		if (UserUtil.hasWebScriptPermissions()) {
			status.setCode(HttpServletResponse.SC_OK);
			mainMethod(req);
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

	@Override
	protected void initMemberVariables() {
		super.initMemberVariables();
		
		modelFolder = getNodeByPath("Sites/europa/ViewEditor/model");
		snapshotFolder = getNodeByPath("Sites/europa/ViewEditor/snapshots");
		user = AuthenticationUtil.getFullyAuthenticatedUser();
	}

	private void mainMethod(WebScriptRequest req) {
		JSONObject postjson = null;
		String viewid = null; 

		initMemberVariables();

		if (req.getContent() == null) {
			return;
		}
		
		postjson = (JSONObject)req.parseContent();
		if (postjson == null) {
			return;
		}
		
		viewid = req.getServiceMatch().getTemplateVars().get("viewid");
		if (viewid == null) {
			return;
		}
		
		NodeRef topview = getModelElement(modelFolder, viewid);
		
		boolean product = false;
		if (req.getParameter("product") != null && req.getParameter("product").equals("true")) {
			product = true;
		}
		
		if (topview == null) {
			if (req.getParameter("doc").equals("true")) {
				Map<QName, Serializable> props = new HashMap<QName, Serializable>();
				for (String params: req.getParameterNames()) {
					props.put(createQName(param2TypeMap.get(params)), req.getParameter(params));
				}
				topview = createModelElement(modelFolder, viewid, "view:DocumentView", props);
			}
		}
	}
}
