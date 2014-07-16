package gov.nasa.jpl.view_repo.util;

public class ModStatus {
    public enum State {
        ADDED, DELETED, UPDATED, NONE
    }
    
    private State state = State.NONE;
    
    public void setState(State state) {
        this.state = state;
    }
    
    public State getState() {
        return state;
    }
}
