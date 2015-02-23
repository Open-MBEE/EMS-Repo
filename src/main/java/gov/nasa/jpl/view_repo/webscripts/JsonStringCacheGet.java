package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows NodeUtil.doJsonStringCaching to be turned on/off
 */
public class JsonStringCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.doJsonStringCaching = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.doJsonStringCaching;
    }

    @Override
    protected String flagName() {
        return "doJsonStringCaching";
    }

}
