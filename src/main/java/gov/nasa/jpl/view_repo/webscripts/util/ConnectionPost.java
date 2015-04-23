package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.view_repo.connections.ConnectionInterface;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.connections.JmsWLConnection;
import gov.nasa.jpl.view_repo.connections.RestPostConnection;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ConnectionPost extends DeclarativeWebScript {
    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = (JSONObject) req.parseContent();

        
        ConnectionInterface connection = null;
        
        if (req.getServicePath().endsWith( "jms" )) {
            connection = new JmsConnection();
        } else if (req.getServicePath().endsWith( "jmswl" )) {
            connection = new JmsWLConnection();
        } else if (req.getServicePath().endsWith( "rest" )) {
            connection = new RestPostConnection();
        }
        
        if (connection == null) {
            model.put( "res", "connection not found" );
            status.setCode( HttpServletResponse.SC_NOT_FOUND );
        } else {
            if (json.has( "uri" )) {
                connection.setUri(json.getString( "uri" ));
            } else {
                connection.setUri( null );
            }

            String msg = String.format("{\"msg\": \"%s uri set to %s.\"}", 
                                       connection.getClass(), connection.getUri() );
            model.put("res", msg);
            status.setCode( HttpServletResponse.SC_OK );
        }
        
        return model;
    }

}
