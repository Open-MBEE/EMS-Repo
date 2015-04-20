package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.view_repo.connections.JmsConnection;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class JmsConnectionPost extends DeclarativeWebScript {
    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = (JSONObject) req.parseContent();
        
        JmsConnection jc = new JmsConnection();
        if (json.has( "uri" )) {
            jc.setUri(json.getString( "uri" ));
        } else {
            jc.setUri( null );
        }
        
        String msg = String.format("{\"msg\": \"JmsConnection uri set to %s.\"}", 
                                   jc.getUri());
        model.put("res", msg);
        
        status.setCode( HttpServletResponse.SC_OK );
        
        return model;
    }

}
