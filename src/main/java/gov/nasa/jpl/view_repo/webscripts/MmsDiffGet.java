package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Workspace diffing service
 * @author cinyoung
 *
 */
public class MmsDiffGet extends AbstractJavaWebScript {

    protected WorkspaceNode ws1, ws2;
    protected String workspaceId1;
    protected String workspaceId2;
    protected Date dateTime1, dateTime2;
    protected WorkspaceDiff workspaceDiff;


    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        workspaceId1 = req.getParameter( "workspace1" );
        workspaceId2 = req.getParameter( "workspace2" );
        ws1 = WorkspaceNode.getWorkspaceFromId( workspaceId1, getServices(), response, status, //false,
                                  null );
        ws2 = WorkspaceNode.getWorkspaceFromId( workspaceId2, getServices(), response, status, //false,
                                  null );
        boolean wsFound1 = ( ws1 != null || ( workspaceId1 != null && workspaceId1.equalsIgnoreCase( "master" ) ) );
        boolean wsFound2 = ( ws2 != null || ( workspaceId2 != null && workspaceId2.equalsIgnoreCase( "master" ) ) );

        if ( !wsFound1 ) {
            log( LogLevel.ERROR,
                 "Workspace 1 id , " + workspaceId1 + ", not found",
                 HttpServletResponse.SC_NOT_FOUND );
            return false;
        }
        if ( !wsFound2 ) {
            log( LogLevel.ERROR,
                 "Workspace 2 id, " + workspaceId2 + ", not found",
                 HttpServletResponse.SC_NOT_FOUND );
            return false;
        }
        return true;
    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        MmsDiffGet mmsDiffGet = new MmsDiffGet();
        return mmsDiffGet.executeImplImpl( req, status, cache, runWithoutTransactions );
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                   Status status, Cache cache ) {
        printHeader( req );

        Map<String, Object> results = new HashMap<String, Object>();

        if (!validateRequest(req, status)) {
            status.setCode(responseStatus.getCode());
            results.put("res", response.toString());
            return results;
        }

        WorkspaceNode ws1, ws2;
        String workspace1 = req.getParameter( "workspace1" );
        String workspace2 = req.getParameter( "workspace2" );
        ws1 = WorkspaceNode.getWorkspaceFromId( workspace1, services, response, status, //false,
                                  null );
        ws2 = WorkspaceNode.getWorkspaceFromId( workspace2, services, response, status, //false,
                                  null );

        String timestamp1 = req.getParameter( "timestamp1" );
        dateTime1 = TimeUtils.dateFromTimestamp( timestamp1 );

        String timestamp2 = req.getParameter( "timestamp2" );
        dateTime2 = TimeUtils.dateFromTimestamp( timestamp2 );

        workspaceDiff = new WorkspaceDiff(ws1, ws2, dateTime1, dateTime2);

        try {
            workspaceDiff.forceJsonCacheUpdate = false;
            JSONObject top = workspaceDiff.toJSONObject( dateTime1, dateTime2, false );
            if (!Utils.isNullOrEmpty(response.toString())) top.put("message", response.toString());
            results.put("res", NodeUtil.jsonToString( top, 4 ));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());

        printFooter();

        return results;
    }
}
