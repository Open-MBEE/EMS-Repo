package gov.nasa.jpl.view_repo.webscripts;

/**
 * Allows AbstractJavaWebScript.alwaysTurnOffDebugOut to be turned on/off
 */
public class AlwaysTurnDebugOffGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        AbstractJavaWebScript.alwaysTurnOffDebugOut = val;
    }

    @Override
    protected boolean get() {
        return AbstractJavaWebScript.alwaysTurnOffDebugOut;
    }

    @Override
    protected String flagName() {
        return "alwaysTurnOffDebugOut";
    }
 
}
