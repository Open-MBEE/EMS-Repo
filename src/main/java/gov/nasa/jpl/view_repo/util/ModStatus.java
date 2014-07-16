package gov.nasa.jpl.view_repo.util;

/**
 * Utility class for keeping track of the state of an EmsScriptNode.
 * This status is used to recreate the deltas for sending events.
 * 
 * @author cinyoung
 *
 */
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
