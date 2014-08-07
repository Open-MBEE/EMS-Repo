package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsDiffGet extends AbstractJavaWebScript {

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        return false;
    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> results = new HashMap<String, Object>();

        WorkspaceNode ws1, ws2;

        ws1 = getWorkspaceFromId( "ws1", services, response, status, false, null );
        ws2 = getWorkspaceFromId( "ws2", services, response, status, false, null );

        WorkspaceDiff diff = new WorkspaceDiff(ws1, ws2);

        return results;
    }
}
