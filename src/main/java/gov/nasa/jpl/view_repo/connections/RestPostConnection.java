package gov.nasa.jpl.view_repo.connections;

import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

/**
 * Implements Runnable so can send the post request off in the background so there aren't
 * any timeout issues when sending deltas.
 * @author cinyoung
 *
 */
public class RestPostConnection implements ConnectionInterface, Runnable {
    private static Logger logger = Logger.getLogger(RestPostConnection.class);
    private long sequenceId = 1;
    private static String uri = null;
    private String workspace = null;
    private String projectId = null;
    
    private String message;
    private String destination;
    
    public RestPostConnection() {
        
    }
        
    @Override
    public boolean publish(JSONObject jsonObject, String dst, String workspaceId, String projectId) {
        if (uri == null) return false;
        String msg = NodeUtil.jsonToString( jsonObject );
        this.workspace = workspaceId;
        this.projectId = projectId;
        return publish(msg, dst);
    }
    
    public boolean publish(String msg, String dst) {
        if (uri == null) return false;
    
        message = msg;
        destination = dst;
        
        new Thread(this).start();
        
        return true; 
    }
    
    
    /**
     * Create web resource builder with the default settings
     *  
     * @param webResource
     * @param dst
     * @return
     */
    private Builder getResourceBuilder(WebResource webResource, String dst) {
        Builder builder = webResource.getRequestBuilder();
        
        builder = builder.accept("application/json").type("application/json");
        
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put( "MessageID", sequenceId++ );
        headers.put( "MessageSource", dst );
        headers.put( "MessageRecipient", "TMS" );
        headers.put( "MessageType", "DELTA" );
        
        builder = header(builder, headers);
        
        return builder;
    }
    
    
    /**
     * Add header parameters to the REST request
     * @param builder
     * @param headers
     * @return
     */
    private Builder header(Builder builder, Map<String, Object> headers) {
       for (String key: headers.keySet()) {
           builder = builder.header( key, headers.get( key ) );
       }
       
       return builder;
    }

    public String getUri() {
        return uri;
    }
    
    public void setUri( String newUri ) {
        uri = newUri;
    }

    @Override
    public void setWorkspace( String workspace ) {
        this.workspace = workspace;
    }

    @Override
    public void setProjectId( String projectId ) {
        this.projectId = projectId;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put( "uri", uri );
        return json;
    }
    
    @Override
    public void ingestJson(JSONObject json) {
        if (json.has( "uri" )) {
            uri = json.isNull( "uri" ) ? null : json.getString( "uri" );
        }
    }

    @Override
    public void run() {
        Client client = Client.create();
        int interval = 1000; // in milliseconds
        client.setConnectTimeout( interval );
        client.setReadTimeout( interval );
        WebResource webResource = client.resource(uri);
        ClientResponse response = null;
        try {
            response = getResourceBuilder(webResource, destination).post( ClientResponse.class, message);
        } catch (Throwable e) {
            logger.warn( "Failed to post message in RestPostConnection!  " + e.getLocalizedMessage() );
        }
        if (response == null || response.getStatus() != 200) {
            if (logger.isDebugEnabled()) {
                logger.debug( String.format( "Rest connection failed" ) );
                if ( response != null ) logger.debug( "response: " + response );
            }
        }
    }

}
