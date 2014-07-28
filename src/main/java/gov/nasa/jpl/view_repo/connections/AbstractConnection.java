package gov.nasa.jpl.view_repo.connections;

import org.json.JSONObject;

public interface AbstractConnection {
    public void setUri(String uri);
    
    /**
     * Publish a json object to the specified destination/topic
     * 
     * @param jsonObject    
     * @param destination
     * @return
     */
    public boolean publish(JSONObject jsonObject, String destination);
}
