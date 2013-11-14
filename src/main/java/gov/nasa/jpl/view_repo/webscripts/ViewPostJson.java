package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.UserUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ViewPostJson extends AbstractWebScript {
	// internal members
	private ScriptNode modelFolder = null;
	private ScriptNode snapshotFolder = null;
	private String user;

	private Map<String, ScriptNode> modelMapping;
	private List<Map<String, String>> merged;

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
		snapshotFolder = companyhome.childByNamePath("Sites/" + site + "/ViewEditor/snapshots");
		
		modelMapping = new HashMap<String, ScriptNode>();
		merged = new ArrayList<Map<String, String>>();
		user = AuthenticationUtil.getFullyAuthenticatedUser();
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
		
		ScriptNode topview = modelFolder.childByNamePath(viewid);
		
		boolean product = false;
		if (req.getParameter("product") != null && req.getParameter("product").equals("true")) {
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
		
		if ( !topview.getTypeShort().equals("view:DocumentView") && req.getParameter("doc").equals("true") ) {
			topview.specializeType("view:DocumentView");
			if (product) {
				topview.getProperties().put("view:product", true);
			}
			topview.save();
		}
		
		boolean force = req.getParameter("force").equals("true") ? true : false;
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
