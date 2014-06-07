package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsConfigurationsGet extends AbstractJavaWebScript {

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        
        JSONObject jsonObject = new JSONObject();

        try {
            ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);
            jsonObject.put("configurations", configWs.handleConfigurations(req));
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

}
