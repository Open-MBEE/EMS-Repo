package gov.nasa.jpl.view_repo.util;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import gov.nasa.jpl.mbee.util.Timer;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Status;

public abstract class EmsTransaction {
    static Logger logger = Logger.getLogger(EmsTransaction.class);
    // injected members
    protected ServiceRegistry services;     // get any of the Alfresco services
    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    protected Status responseStatus = new Status();

    public EmsTransaction(ServiceRegistry services, StringBuffer response, Status responseStatus) {
        this.response = response;
        this.responseStatus = responseStatus;
        this.services = services;
        Timer timerCommit = null;
        UserTransaction trx;
        trx = services.getTransactionService().getNonPropagatingUserTransaction();
        try {
            trx.begin();
            NodeUtil.setInsideTransactionNow( true );
            
            run();
            
            timerCommit = Timer.startTimer(timerCommit, NodeUtil.timeEvents);
            trx.commit();
            Timer.stopTimer(timerCommit, "!!!!! EmsTransaction commit time", NodeUtil.timeEvents);
        } catch (Throwable e) {
            tryRollback( trx, e, "DB transaction failed" );
            responseStatus.setCode( HttpServletResponse.SC_BAD_REQUEST );
            response.append( "Could not complete DB transaction, see Alfresco logs for details" );
        } finally {
            NodeUtil.setInsideTransactionNow( false );
        }
        
    }
    
    abstract public void run() throws Exception;


    protected void tryRollback( UserTransaction trx, Throwable e, String msg ) {
        if ( msg == null || msg.length() <= 0 ) {
            msg = "DB transaction failed";
        }
        try {
            log( Level.ERROR,
                 msg + "\n\t####### ERROR: Need to rollback: " + e.getMessage(),
                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            e.printStackTrace();
            trx.rollback();
            log( Level.ERROR, "### Rollback succeeded!" );
        } catch ( Throwable ee ) {
            log( Level.ERROR, "\tryRollback(): rollback failed: " + ee.getMessage() );
            ee.printStackTrace();
            String addr = null;
            try {
                addr = Inet4Address.getLocalHost().getHostAddress();
            } catch ( UnknownHostException e1 ) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            NodeUtil.sendNotificationEvent( "Heisenbug Occurence!", "rollback failed on " + addr , services );
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
