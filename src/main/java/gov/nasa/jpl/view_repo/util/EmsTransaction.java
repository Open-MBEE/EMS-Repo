package gov.nasa.jpl.view_repo.util;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import gov.nasa.jpl.mbee.util.Timer;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Status;

public abstract class EmsTransaction {
    public static boolean syncTransactions = false;
    static Logger logger = Logger.getLogger(EmsTransaction.class);
    // injected members
    protected ServiceRegistry services;     // get any of the Alfresco services
    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    protected Status responseStatus = new Status();
    //private UserTransaction trx;

    public EmsTransaction(//UserTransaction oldTrx, 
                          ServiceRegistry services,
                          StringBuffer response, Status responseStatus) {
        this( //oldTrx, 
              services, response, responseStatus, false );
    }

    public EmsTransaction( //UserTransaction oldTrx,
                           ServiceRegistry services,
                           StringBuffer response, Status responseStatus,
                           boolean noTransaction ) {
        //this.trx = oldTrx;
        this.response = response;
        this.responseStatus = responseStatus;
        this.services = services;
        if ( noTransaction ) {
            // run without transactions
            try {
                run();
            } catch ( Throwable e ) {
                e.printStackTrace();
            }
            return;
        }
        
        
        // running with transactions
        
////        if ( trx == null ) {
////            trx = services.getTransactionService().getNonPropagatingUserTransaction();
////        }
////        if ( trx != null && trx.getStatus() == javax.transaction.Status. ) {
////            trx.commit();
////        }
        
        // If we're in a transaction already, commit it and start a new one
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
        // for new transaction
        trx = NodeUtil.createTransaction();
        
        try {
            if ( syncTransactions ) {
                synchronized ( logger ) {
                    transactionWrappedRun( trx );
                }
            } else {
                transactionWrappedRun( trx );
            }
        } catch (Throwable e) {
            tryRollback( trx, e, "DB transaction failed" );
            if (responseStatus.getCode() != HttpServletResponse.SC_BAD_REQUEST) {
                responseStatus.setCode( HttpServletResponse.SC_BAD_REQUEST );
                response.append( "Could not complete DB transaction, see Alfresco logs for details" );
            }
        } finally {
            NodeUtil.setInsideTransactionNow( false );
            
            // Restart outer transaction if there was one.
            if ( wasInTransaction ) {
                trx = NodeUtil.createTransaction();
                tryBegin( trx );
            }
        }
    }
    
    protected void tryCommit( UserTransaction trx ) {
        try {
            commit( trx );
        } catch (Throwable e) {
            tryRollback( trx, e, "DB transaction failed" );
            if (responseStatus.getCode() != HttpServletResponse.SC_BAD_REQUEST) {
                responseStatus.setCode( HttpServletResponse.SC_BAD_REQUEST );
                response.append( "Could not complete DB transaction, see Alfresco logs for details" );
            }
        } finally {
            NodeUtil.setInsideTransactionNow( false );
        }
    }
    
    protected void tryBegin( UserTransaction trx ) {
        try {
            trx.begin();
            //logger.warn( "begin" );
            //logger.warn(Debug.stackTrace());
            NodeUtil.setInsideTransactionNow( true );
        } catch (Throwable e) {
            tryRollback( trx, e, "DB transaction begin failed" );
            if (responseStatus.getCode() != HttpServletResponse.SC_BAD_REQUEST) {
                responseStatus.setCode( HttpServletResponse.SC_BAD_REQUEST );
                response.append( "Could not begin DB transaction, see Alfresco logs for details" );
            }
            NodeUtil.setInsideTransactionNow( false );
        } finally {
        }
    }
    
    protected void transactionWrappedRun( UserTransaction trx ) throws Throwable {
        trx.begin();
        //logger.warn( "begin" );
        //logger.warn(Debug.stackTrace());
        NodeUtil.setInsideTransactionNow( true );
        
        run();

        
        UserTransaction commitTrx = NodeUtil.getTransaction();
        commit( commitTrx );
    }

    protected void commit( UserTransaction trx ) throws SecurityException,
                                                IllegalStateException,
                                                RollbackException,
                                                HeuristicMixedException,
                                                HeuristicRollbackException,
                                                SystemException {
        Timer timerCommit = null;
        timerCommit = Timer.startTimer(timerCommit, NodeUtil.timeEvents);
        //logger.warn( "commit" );
        //logger.warn(Debug.stackTrace());
        trx.commit();
        Timer.stopTimer(timerCommit, "!!!!! EmsTransaction commit time", NodeUtil.timeEvents);

    }
    
    abstract public void run() throws Exception;


    protected void tryRollback( UserTransaction trx, Throwable e, String msg ) {
        if ( msg == null || msg.length() <= 0 ) {
            msg = "DB transaction failed";
        }
        try {
//            log( Level.ERROR,
//                 msg + "\n\t####### ERROR: Need to rollback: " + e.getMessage(),
//                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            e.printStackTrace();
            trx.rollback();
        } catch ( Throwable ee ) {
            log( Level.ERROR, "tryRollback(): rollback failed: " + ee.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            // don't send false positive - e.g. rollback already succeeded
            if (!ee.getMessage().contains( "The transaction has already been rolled back" )) {
                ee.printStackTrace();
                String addr = null;
                try {
                    addr = Inet4Address.getLocalHost().getHostAddress();
                } catch ( UnknownHostException e1 ) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                NodeUtil.sendNotificationEvent( "Transaction did not roll back properly!", "rollback failed on " + addr , services );
            }
        }
    }

    protected void log(Level level, String msg, int code) {
        logger.log( level, msg );
        log("[" + level + "]: " + msg + "\n", code);
    }

    protected void log(Level level, String msg) {
        log("[" + level + "]: " + msg);
    }

    protected void log(String msg, int code) {
        response.append(msg);
        responseStatus.setCode(code);
        responseStatus.setMessage(msg);
    }

    protected void log(String msg) {
        response.append(msg + "\n");
    }


}
