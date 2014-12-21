package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * Allows EmsScriptNode.versionCacheDebugPrint to be turned on/off
 */
public class VersionCacheDebugGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        EmsScriptNode.versionCacheDebugPrint = val;
    }

    @Override
    protected boolean get() {
        return EmsScriptNode.versionCacheDebugPrint;
    }

    @Override
    protected String flagName() {
        return "versionCacheDebugPrint";
    }

}
