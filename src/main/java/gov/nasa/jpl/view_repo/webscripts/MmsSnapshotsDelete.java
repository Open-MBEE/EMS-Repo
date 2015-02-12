package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsSnapshotsDelete extends AbstractJavaWebScript {

    public MmsSnapshotsDelete() {
        super();
    }

    public MmsSnapshotsDelete( Repository repository, ServiceRegistry services ) {
        this.repository = repository;
        this.services = services;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		MmsSnapshotsDelete instance = new MmsSnapshotsDelete(repository, getServices());
    	    return instance.executeImplImpl(req, status, cache, runWithoutTransactions);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();

        MmsSnapshotsDelete instance = new MmsSnapshotsDelete(repository, getServices());

        JSONObject jsonObject = new JSONObject();
        try {
            instance.handleRequest(req);
            appendResponseStatusInfo( instance );
            if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
            model.put("res", jsonObject.toString(2));
        } catch (Exception e) {
            model.put("res", response.toString());
            if (e instanceof JSONException) {
                log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                log(LogLevel.ERROR, "Internal server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());
        return model;
    }

    private void handleRequest( WebScriptRequest req ) throws JSONException {
        String snapshotId = req.getServiceMatch().getTemplateVars().get("snapshotId");
        NodeRef snapshotNodeRef = NodeUtil.findNodeRefByType( snapshotId, SearchType.CM_NAME, true,
                                                           null, null, true, services, false );
        if (snapshotNodeRef == null) {
            log(LogLevel.ERROR, "Could not find snapshot", HttpServletResponse.SC_NOT_FOUND);
        } else {
            EmsScriptNode snapshot = new EmsScriptNode(snapshotNodeRef, services, response);
            snapshot.delete();
        }
    }

}
