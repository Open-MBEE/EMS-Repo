package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.view_repo.connections.RestPostConnection;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class RestConnectionGet extends DeclarativeWebScript {

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> model = new HashMap<String, Object>();
        RestPostConnection rpc = new RestPostConnection();
        String msg = String.format("{\"msg\": \"RestPostConnection uri set to %s \"}", 
                                   rpc.getUri());
        model.put("res", msg);
        status.setCode( HttpServletResponse.SC_OK );
        return model;
    }
    
}
