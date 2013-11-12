package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.UserUtil;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ViewPostJson extends DeclarativeWebScript {
	// injected members
	private NodeService nodeService;
	private NodeLocatorService nodeLocatorService;
	private Repository repository;
	
	// internal members
	private NodeRef	companyhome = null;
	private NodeRef modelFolder = null;
	private NodeRef snapshotFolder = null;
	
	private String user;

	private Map<String, NodeRef> modelMapping = new HashMap<String, NodeRef>();
	private List<Map<String, String>> merged = new ArrayList<Map<String, String>>();
	
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    
    public void setNodeLocatorService(NodeLocatorService nodeLocatorService) {
        this.nodeLocatorService = nodeLocatorService;
    }
    
	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}
	
    
    
	/**
	 * 
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

	
	private void initMemberVariables() {
		modelFolder = getNodeByPath("Sites/europa/ViewEditor/model");
		snapshotFolder = getNodeByPath("Sites/europa/ViewEditor/snapshots");
		user = AuthenticationUtil.getFullyAuthenticatedUser();
	}

	private void mainMethod(WebScriptRequest req) {
		initMemberVariables();
		
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse((String) req.getParameter("json"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private NodeRef getNodeByPath(String path) {
		if (companyhome == null) {
			companyhome = repository.getCompanyHome();
		}
		return nodeService.getChildByName(companyhome, ContentModel.ASSOC_CONTAINS, path);
	}
}
