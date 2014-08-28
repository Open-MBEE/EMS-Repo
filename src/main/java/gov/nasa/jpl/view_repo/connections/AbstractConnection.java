package gov.nasa.jpl.view_repo.connections;

public abstract class AbstractConnection implements ConnectionInterface {
    protected String workspace = null;
    protected String projectId = null;
    
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
