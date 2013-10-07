package gov.nasa.jpl.view_repo;

import gov.nasa.jpl.ae.util.JavaEvaluator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;

import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class JavaQueryPost extends DeclarativeWebScript {

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
		//model.put("reply", n());//Integer.toString(count));
		Content query = req.getContent();
		String qString = null;
        try {
            qString = query.getContent();
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String packageName = JavaQueryPost.class.getPackage().toString().replace( "package ", "" );
		model.put( "query", qString );
		System.out.println("\n\n\nEvaluating: \"" + qString + "\"\n\n");
		Object reply = JavaEvaluator.evaluate( qString,  packageName );
		if ( reply == null ) reply = "null";
        System.out.println("\n\n\nResult = : \"" + reply + "\"\n\n");
		model.put("reply", reply );
		return model;
	}
}
