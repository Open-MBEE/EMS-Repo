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

public class RestPostConnection extends AbstractConnection {
    static Logger logger = Logger.getLogger(RestPostConnection.class);

    private long sequenceId = 1;
    
    // static so Spring can configure URI for everything
    private static String uri = "https://orasoa-dev07.jpl.nasa.gov:8121/PublishMessageRestful"; // TODO: Springify
    
    public RestPostConnection() {
        
    }
    
    public void setUri(String uri) {
        if (logger.isInfoEnabled()) {
            logger.info("uri set to: " + uri);
        }
        RestPostConnection.uri = uri;
    }
    
    public String getUri() {
        return RestPostConnection.uri;
    }
    
    public boolean publish(JSONObject jsonObject, String dst) {
        String msg = NodeUtil.jsonToString( jsonObject );
        return publish(msg, dst);
    }
    
    public boolean publish(String msg, String dst) {
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
}
