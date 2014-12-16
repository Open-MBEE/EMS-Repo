package gov.nasa.jpl.view_repo.webscripts;

/**
 * Allows timeEvents to be turned on/off
 */
public class ModelPostTimeEventsGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        ModelPost.timeEvents = val;
    }

    @Override
    protected boolean get() {
        return ModelPost.timeEvents;
    }

    @Override
    protected String flagName() {
        return "timeEvents";
    }
 
}
