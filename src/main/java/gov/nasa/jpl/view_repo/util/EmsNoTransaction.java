/**
 * 
 */
package gov.nasa.jpl.view_repo.util;

import javax.transaction.UserTransaction;

import org.alfresco.service.ServiceRegistry;
import org.springframework.extensions.webscripts.Status;

/**
 * Make sure that code is run completely outside any EmsTransaction by ending
 * any current transaction and running without a transaction.
 *
 */
public abstract class EmsNoTransaction extends EmsTransaction {

    /**
     * @param services
     * @param response
     * @param responseStatus
     */
    public EmsNoTransaction( ServiceRegistry services, StringBuffer response, Status responseStatus ) {
        super( services, response, responseStatus );
    }
    
    @Override
    protected void transactionedRun( boolean noTransaction ) {
        // If we're in a transaction already, commit it
        UserTransaction trx = null;
        boolean wasInTransaction = NodeUtil.isInsideTransactionNow();
        if ( wasInTransaction ) {
            trx = NodeUtil.getTransaction();
            if ( trx == null ) {
                // BAD!!!
                trx = NodeUtil.createTransaction();
            }
            tryCommit( trx );
        }

        // run without transactions
        // REVIEW -- Consider not catching exception or provide flag to
        // optionally throw it again.
        try {
            run();
        } catch ( Throwable e ) {
            e.printStackTrace();
        } finally {
            // Restart outer transaction if there was one.
            if ( wasInTransaction ) {
                trx = NodeUtil.createTransaction();
                tryBegin( trx );
            }

        }

    }

}
