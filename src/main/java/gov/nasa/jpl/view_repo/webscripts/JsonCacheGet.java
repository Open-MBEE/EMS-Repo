package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.JsonObject;
import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows NodeUtil.doJsonCaching to be turned on/off
 */
public class JsonCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.doJsonCaching = val;
        JsonObject.doCaching = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.doJsonCaching;
    }

    @Override
    protected String flagName() {
        return "doJsonCaching";
    }

}
