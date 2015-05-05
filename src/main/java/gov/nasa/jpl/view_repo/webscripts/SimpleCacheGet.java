package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows NodeUtil.doSimpleCaching to be turned on/off
 */
public class SimpleCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.doSimpleCaching = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.doSimpleCaching;
    }

    @Override
    protected String flagName() {
        return "doSimpleCaching";
    }
 
}
