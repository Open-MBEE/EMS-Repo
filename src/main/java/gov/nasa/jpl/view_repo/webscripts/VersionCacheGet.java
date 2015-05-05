package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows NodeUtil.doVersionCaching to be turned on/off
 */
public class VersionCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.doVersionCaching = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.doVersionCaching;
    }

    @Override
    protected String flagName() {
        return "doVersionCaching";
    }

}
