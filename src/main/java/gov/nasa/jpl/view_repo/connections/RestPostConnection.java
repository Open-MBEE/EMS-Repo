package gov.nasa.jpl.view_repo.connections;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class RestPostConnection implements AbstractConnection {
    private static RestPostConnection INSTANCE = null;
    
    private static long sequenceId = 1;
    
    private String uri = "https://orasoa-dev07.jpl.nasa.gov:8121/PublishMessageRestful"; // TODO: Springify
    
    private RestPostConnection() {
        
    }
    
    public static RestPostConnection getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RestPostConnection();
        }
        return INSTANCE;
    }
    
    public boolean publish(JSONObject jsonObject, String dst) {
        boolean status = true;
        Client client = Client.create();
        String msg = jsonObject.toString( );
        
        WebResource webResource = client.resource(uri);
        ClientResponse response = getResourceBuilder(webResource, dst).post( ClientResponse.class, msg);
        if (response.getStatus() != 200) {
            status = false;
        }
        System.out.println("\n#### Response From Server ####\n");
        System.out.println(response.getEntity( String.class ));
        
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
        headers.put( "MessageType", "JSON" );
        
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
