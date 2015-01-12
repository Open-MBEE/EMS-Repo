package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows timeEvents to be turned on/off
 */
public class TimeEventsGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.timeEvents = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.timeEvents;
    }

    @Override
    protected String flagName() {
        return "timeEvents";
    }
 
}
