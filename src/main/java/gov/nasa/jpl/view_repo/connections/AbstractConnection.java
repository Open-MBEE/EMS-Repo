package gov.nasa.jpl.view_repo.connections;

import org.json.JSONObject;

public interface AbstractConnection {
    /**
     * Publish a json object to the specified destination/topic
     * 
     * @param jsonObject    
     * @param destination
     * @return
     */
    public boolean publish(JSONObject jsonObject, String destination);
}
