package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows NodeUtil.doFullCaching to be turned on/off
 */
public class FullCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.doFullCaching = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.doFullCaching;
    }

    @Override
    protected String flagName() {
        return "doFullCaching";
    }
 
}
