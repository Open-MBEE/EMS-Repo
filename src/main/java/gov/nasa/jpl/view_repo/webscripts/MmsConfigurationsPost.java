package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

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

public class MmsConfigurationsPost extends AbstractJavaWebScript {
    public MmsConfigurationsPost() {
        super();
    }
    
    public MmsConfigurationsPost( Repository repository, ServiceRegistry services ) {
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
        
        MmsConfigurationsPost instance = new MmsConfigurationsPost(repository, services);
        
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject = instance.handleUpdate( req );
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
    
    private JSONObject handleUpdate(WebScriptRequest req) throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest(req);
        if (siteNode == null) {
            log(LogLevel.WARNING, "Could not find site", HttpServletResponse.SC_NOT_FOUND);
            return new JSONObject();
        }
        
        String configId = req.getServiceMatch().getTemplateVars().get( "configurationId" );
        NodeRef configNode = NodeUtil.getNodeRefFromNodeId( configId );
        if (configNode == null) {
            log(LogLevel.WARNING, "Could not find configuration with id: " + configId, HttpServletResponse.SC_NOT_FOUND);
            return new JSONObject();
        }
        EmsScriptNode config = new EmsScriptNode(configNode, services);
        
        ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
        boolean requiresAction = configWs.updateConfiguration( config, (JSONObject)req.parseContent(), siteNode, null );
        if (requiresAction) {
            // TODO: kick off action to create snapshot
        }
        return configWs.getConfigJson( config, (String)siteNode.getProperty( Acm.CM_NAME ), null );
    }
}
