package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsConfigurationsGet extends AbstractJavaWebScript {
    public enum Type {
        SINGLE,
        MULTIPLE
    }

    private Type type;

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public MmsConfigurationsGet() {
        super();
    }

    public MmsConfigurationsGet( Repository repository, ServiceRegistry services ) {
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
		MmsConfigurationsGet instance = new MmsConfigurationsGet(repository, getServices());
		instance.setType( type );
		return instance.executeImplImpl(req, status, cache, runWithoutTransactions);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        //clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();

        MmsConfigurationsGet instance = new MmsConfigurationsGet(repository, getServices());

        JSONObject jsonObject = new JSONObject();

        try {
            ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, getServices(), instance.response);
            switch(type) {
                case SINGLE:
                    jsonObject.put("configurations", configWs.handleConfiguration(req, true));
                    break;
                case MULTIPLE:
                    jsonObject.put("configurations", configWs.handleConfigurations(req, true));
                    break;
                default:
                    // assume multiple
                    jsonObject.put("configurations", configWs.handleConfigurations(req, true));
            }
            appendResponseStatusInfo( instance );
            if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
            model.put("res", NodeUtil.jsonToString( jsonObject, 2 ));
        } catch (Exception e) {
            model.put("res", createResponseJson());
            if (e instanceof JSONException) {
                log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                log(LogLevel.ERROR, "Internal server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }

}
