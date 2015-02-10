package gov.nasa.jpl.view_repo.connections;

import gov.nasa.jpl.view_repo.util.JsonObject;

public interface ConnectionInterface {
    public void setUri(String uri);
    
    public void setWorkspace(String workspace);
    
    public void setProjectId(String projectId);
    
    /**
     * Publish a json object to the specified destination/topic
     * 
     * @param jsonObject    
     * @param destination
     * @return
     */
    public boolean publish(JsonObject jsonObject, String destination);
}
