package gov.nasa.jpl.view_repo.webscripts;

import org.json.JSONObject;
import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows NodeUtil.doJsonCaching to be turned on/off
 */
public class JsonCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        NodeUtil.doJsonCaching = val;
        //JSONObject.doCaching = val;
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
