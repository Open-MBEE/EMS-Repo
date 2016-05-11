package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsSnapshotGet extends AbstractJavaWebScript {

    public MmsSnapshotGet() {
        super();
    }

    public MmsSnapshotGet( Repository repository, ServiceRegistry services ) {
        this.repository = repository;
        this.services = services;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        return false;
    }

    @Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		MmsSnapshotGet instance = new MmsSnapshotGet(repository, getServices());
        // all desc.xml have transactions required, so need to use the EmsTransaction calls
		return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        Map<String, Object> model = new HashMap<String, Object>();
       	if (checkMmsVersions) {
    		if(compareMmsVersions(req, getResponse(), getResponseStatus()));
		    {
            	model.put("res", createResponseJson());
            	return model;
            }
        }

        MmsSnapshotGet instance = new MmsSnapshotGet(repository, getServices());

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("snapshots", instance.handleRequest(req));
            appendResponseStatusInfo( instance );
            if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
            model.put("res", NodeUtil.jsonToString( jsonObject, 2 ));
        } catch (InvalidNodeRefException inre) {
            model.put( "res", "{\"msg\": \"Snapshot not found\"}" );
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Snapshot does not exist");
        } catch (Exception e) {
            model.put("res", createResponseJson());
            if (e instanceof JSONException) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"JSON creation error");
            } else {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
            e.printStackTrace();
        }
		
        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }

    private JSONArray handleRequest( WebScriptRequest req ) throws JSONException, InvalidNodeRefException {
        String snapshotId = req.getServiceMatch().getTemplateVars().get( "snapshotId" );
        ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);
        
        WorkspaceNode workspace = getWorkspace( req );
        NodeRef snapshotNr = new NodeRef("workspace://SpacesStore/" + snapshotId);
        EmsScriptNode snapshot = new EmsScriptNode( snapshotNr, services, response );
        
        JSONObject jsonObject = configWs.getSnapshotJson( snapshot, null, workspace );
        JSONArray jsonArray = new JSONArray();
        jsonArray.put( jsonObject );
        return jsonArray;
    }
}
