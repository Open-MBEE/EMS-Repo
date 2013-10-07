package gov.nasa.jpl.view_repo;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;

import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class TestJavaWebScript extends DeclarativeWebScript {

	private NodeService nodeService;
    
    private NodeLocatorService nodeLocatorService;
    

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    
    public void setNodeLocatorService(NodeLocatorService nodeLocatorService) {
        this.nodeLocatorService = nodeLocatorService;
    }
    
    private String n() {
        return "foo";
    }
    
    private String getReply() {
    	return "120";
    }
    
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();
		NodeRef root = nodeLocatorService.getNode("companyhome", null, null);
		int count = nodeService.countChildAssocs(root, true);
		model.put("reply", n());//Integer.toString(count));
		return model;
	}
}
