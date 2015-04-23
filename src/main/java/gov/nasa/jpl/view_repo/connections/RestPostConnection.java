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

public class RestPostConnection implements ConnectionInterface {
    private static Logger logger = Logger.getLogger(RestPostConnection.class);
    private long sequenceId = 1;
    private static String uri = null;
    private String workspace = null;
    private String projectId = null;
        
    public RestPostConnection() {
        
    }
        
    public boolean publish(JSONObject jsonObject, String dst) {
        if (uri == null) return false;
        String msg = NodeUtil.jsonToString( jsonObject );
        return publish(msg, dst);
    }
    
    public boolean publish(String msg, String dst) {
        if (uri == null) return false;
        boolean status = true;
        Client client = Client.create();
    
        if (logger.isDebugEnabled()) {
            logger.debug("sending to: " + uri);
        }
        WebResource webResource = client.resource(uri);
        ClientResponse response = getResourceBuilder(webResource, dst).post( ClientResponse.class, msg);
        if (response.getStatus() != 200) {
            status = false;
        }
        
        return status;
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

    @Override
    public String getUri() {
        return uri;
    }
    
    @Override
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
}
