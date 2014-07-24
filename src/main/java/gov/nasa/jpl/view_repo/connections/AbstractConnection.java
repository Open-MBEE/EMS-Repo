package gov.nasa.jpl.view_repo.connections;

import org.json.JSONObject;

public interface AbstractConnection {
    public boolean publish(JSONObject jsonObject, String destination);
}
