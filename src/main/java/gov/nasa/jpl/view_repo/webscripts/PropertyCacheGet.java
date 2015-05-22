package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Allows NodeUtil.doFullCaching to be turned on/off
 */
public class PropertyCacheGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        if ( !NodeUtil.doPropertyCaching && val ) {
            NodeUtil.propertyCache.clear();
        }
        NodeUtil.doPropertyCaching = val;
    }

    @Override
    protected boolean get() {
        return NodeUtil.doPropertyCaching;
    }

    @Override
    protected String flagName() {
        return "doFullCaching";
    }
 
}
