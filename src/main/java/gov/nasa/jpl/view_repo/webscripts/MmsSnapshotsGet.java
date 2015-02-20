package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript.ConfigurationType;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsSnapshotsGet extends AbstractJavaWebScript {

    public MmsSnapshotsGet() {
        super();
    }

    public MmsSnapshotsGet( Repository repository, ServiceRegistry services ) {
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
		MmsSnapshotsGet instance = new MmsSnapshotsGet(repository, getServices());
		return instance.executeImplImpl(req, status, cache, runWithoutTransactions);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        //clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();

        MmsSnapshotsGet instance = new MmsSnapshotsGet(repository, getServices());

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("snapshots", instance.handleRequest(req));
            appendResponseStatusInfo( instance );
            if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
            model.put("res", NodeUtil.jsonToString( jsonObject, 2 ));
        } catch (Exception e) {
            model.put("res", response.toString());
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

    private JSONArray handleRequest( WebScriptRequest req ) throws JSONException {
        String configurationId = req.getServiceMatch().getTemplateVars().get( "configurationId" );
        if (configurationId != null) {
            return handleConfigurationSnapshot(req, configurationId);
        } else {
            String productId = req.getServiceMatch().getTemplateVars().get( "productId" );
            return handleProductSnapshot(req, productId);
        }
    }

    private JSONArray handleConfigurationSnapshot( WebScriptRequest req, String configurationId ) throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest( req, false );
        String siteNameFromReq = getSiteName( req );
        if ( siteNode == null && !Utils.isNullOrEmpty( siteNameFromReq )
             && !siteNameFromReq.equals( NO_SITE_ID ) ) {
            log( Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find site");
            return new JSONArray();
        }
        ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);

        WorkspaceNode workspace = getWorkspace( req );

        EmsScriptNode config = configWs.getConfiguration( configurationId );
        if (config == null) {
            return new JSONArray();
        }

        return configWs.getSnapshots( config, workspace );
    }

    private JSONArray handleProductSnapshot( WebScriptRequest req, String productId ) throws JSONException {
        Date timestamp = TimeUtils.dateFromTimestamp(req.getParameter("timestamp"));
        WorkspaceNode workspace = getWorkspace( req );
        EmsScriptNode product = findScriptNodeById( productId, workspace, timestamp, false);

        if (product == null) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find product");
            return new JSONArray();
        }

        ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);

        JSONArray snapshotsJson = new JSONArray();

        // for backwards compatibility, keep deprecated targetAssocsNodesByType
        List< EmsScriptNode > snapshots =
                product.getTargetAssocsNodesByType( "view2:snapshots",
                                                    workspace, null );
        for (EmsScriptNode snapshot: snapshots) {
            if ( !snapshot.isDeleted() ) {
                snapshotsJson.put( configWs.getSnapshotJson( snapshot, product,
                                                             workspace ) );
            }
        }

        // for backwards compatibility, keep noderefs
        List< NodeRef > productSnapshots = product.getPropertyNodeRefs( "view2:productSnapshots" );
        for (NodeRef productSnapshotNodeRef: productSnapshots) {
            EmsScriptNode productSnapshot = new EmsScriptNode(productSnapshotNodeRef, services, response);
            if ( !productSnapshot.isDeleted() ) {
                snapshotsJson.put( configWs.getSnapshotJson( productSnapshot, product, workspace ) );
            }
        }
        
        // all we really need to do is grab all the configurations and place them in as snapshots
        ConfigurationsWebscript configService = new ConfigurationsWebscript( repository, services, response );
        JSONArray configSnapshots = configService.handleConfigurations( req, ConfigurationType.CONFIG_SNAPSHOT );
        for (int ii = 0; ii < configSnapshots.length(); ii++) {
            snapshotsJson.put( configSnapshots.get( ii ) );
        }
        
        return snapshotsJson;
    }

}
