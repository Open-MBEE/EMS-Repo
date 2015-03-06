package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsTransaction;

/**
 * Allows NodeUtil.doJsonCaching to be turned on/off
 */
public class SyncTransactionsGet extends FlagSet {

    @Override
    protected void set( boolean val ) {
        EmsTransaction.syncTransactions = val;
        //JSONObject.doCaching = val;
    }

    @Override
    protected boolean get() {
        return EmsTransaction.syncTransactions;
    }

    @Override
    protected String flagName() {
        return "syncTransactions";
    }

}
