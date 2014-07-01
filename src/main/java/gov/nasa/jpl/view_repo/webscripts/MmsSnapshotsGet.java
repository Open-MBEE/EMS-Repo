package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
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
        clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();

        MmsSnapshotsGet instance = new MmsSnapshotsGet(repository, services);
        
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("snapshots", instance.handleRequest(req));
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
    
        appendResponseStatusInfo( instance );
        status.setCode(responseStatus.getCode());
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
        EmsScriptNode siteNode = getSiteNodeFromRequest( req );
        if (siteNode == null) {
            log( LogLevel.WARNING, "Could not find site", HttpServletResponse.SC_NOT_FOUND );
            return new JSONArray();
        }
        ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);
        
        String timestamp = req.getParameter("timestamp");
        
        EmsScriptNode config = configWs.getConfiguration( configurationId, timestamp );
        if (config == null) {
            return new JSONArray();
        }
        
        return configWs.getSnapshots( config, TimeUtils.dateFromTimestamp(timestamp) );
    }

    private JSONArray handleProductSnapshot( WebScriptRequest req, String productId ) throws JSONException {
        Date timestamp = TimeUtils.dateFromTimestamp(req.getParameter("timestamp"));
        EmsScriptNode product = findScriptNodeById( productId, timestamp);
        
        if (product == null) {
            log(LogLevel.WARNING, "Could not find product", HttpServletResponse.SC_NOT_FOUND);
            return new JSONArray();
        }

        ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);
        
        JSONArray snapshotsJson = new JSONArray();
        List< EmsScriptNode > snapshots = product.getTargetAssocsNodesByType( "view2:snapshots", timestamp );
        for (EmsScriptNode snapshot: snapshots) {
            snapshotsJson.put( configWs.getSnapshotJson( snapshot, product ) );
        }
        
        return snapshotsJson;
    }

}
