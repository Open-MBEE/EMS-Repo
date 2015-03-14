package gov.nasa.jpl.view_repo.webscripts;

/**
 * Allows running with transactions to be turned on/off
 */
public class RunWithTransactionsGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        AbstractJavaWebScript.defaultRunWithoutTransactions = !val;
    }

    @Override
    protected boolean get() {
        return !AbstractJavaWebScript.defaultRunWithoutTransactions;
    }

    @Override
    protected String flagName() {
        return "runWithTransactions";
    }
        
}
