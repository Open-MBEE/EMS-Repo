package gov.nasa.jpl.view_repo.util;

/**
 * Utility class for keeping track of the modification state of an EmsScriptNode.
 * This status is used to recreate the deltas for sending events.
 * 
 * @author cinyoung
 *
 */
public class ModStatus {
    public enum State {
        ADDED, DELETED, UPDATED, MOVED, UPDATED_AND_MOVED, NONE
    }
    
    private State state = State.NONE;
    
    public void setState(State newState) {
        if ( ( newState.equals( State.MOVED ) && state.equals( State.UPDATED ) )  ||
             ( newState.equals( State.UPDATED ) && state.equals( State.MOVED ))  ) {
            state = State.UPDATED_AND_MOVED;
        } else {
            state = newState;
        }
    }
    
    public State getState() {
        return state;
    }
}
