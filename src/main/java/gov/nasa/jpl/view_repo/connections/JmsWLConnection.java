package gov.nasa.jpl.view_repo.connections;

import java.util.Hashtable;

import gov.nasa.jpl.view_repo.util.NodeUtil;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.Logger;

/**
 * FIXME: This is only for testing and should be removed in the future
 * @author cinyoung
 *
 */
public class JmsWLConnection implements ConnectionInterface {
    private static Logger logger = Logger.getLogger(JmsWLConnection.class);
    private long sequenceId = 0;
    private static String uri = null;
    private String workspace = null;
    private String projectId = null;
    
    public JmsWLConnection() {
    }

    
    public boolean publish(JSONObject json) {
        return publish(json, null);
    }
    
    @Override
    public boolean publish(JSONObject json, String topic) {
        if (uri == null) return false;
        boolean result = false;
        try {
            json.put( "sequence", sequenceId++ );
            result = publishQueue(NodeUtil.jsonToString( json, 2 ));
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    
    private static InitialContext ctx = null;
    private static QueueConnectionFactory qcf = null;
    private static QueueConnection qc = null;
    private static QueueSession qsess = null;
    private static Queue q = null;
    private static QueueSender qsndr = null;
    private static final String QCF_NAME = "jms/JPLEuropaJMSModuleCF";
    private static final String QUEUE_NAME = "jms/MMSDistributedQueue";

    public boolean publishQueue(String msg) {
        if (qc == null) {
            initQueueConnection();
        }
        
        boolean status = true;

     // create QueueSession
        try {
            qsess = qc.createQueueSession(false, 0);
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
            return false;
        }
//        if (logger.isInfoEnabled()) logger.info("Got QueueSession " + qsess.toString());
        // lookup Queue
        try {
            q = (Queue) ctx.lookup(QUEUE_NAME);
        }
        catch (NamingException ne) {
            ne.printStackTrace(System.err);
            return false;
        }
//        if (logger.isInfoEnabled()) logger.info("Got Queue " + q.toString());
        // create QueueSender
        try {
            qsndr = qsess.createSender(q);
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
            return false;
        }
//        if (logger.isInfoEnabled()) logger.info("Got QueueSender " + qsndr.toString());
        TextMessage message = null;
        // create TextMessage
        try {
            message = qsess.createTextMessage(msg);
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
            return false;
        }
//        if (logger.isInfoEnabled()) logger.info("Got TextMessage " + message.toString());
        // set message text in TextMessage
        try {
            if (workspace != null) {
                message.setStringProperty( "workspace", workspace );
            } else {
                message.setStringProperty( "workspace", "master" );
            }
            if (projectId != null) {
                message.setStringProperty( "projectId", projectId );
            }
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
            return false;
        }
//        if (logger.isInfoEnabled()) logger.info("Set text in TextMessage " + message.toString());
        // send message
        try {
            qsndr.send(message);
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
            return false;
        }
        if (logger.isInfoEnabled()) logger.info("Sent message ");
        // clean up
        try {
            message = null;
            qsndr.close();
            qsndr = null;
            q = null;
            qsess.close();
            qsess = null;
            qc.close();
            qc = null;
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
        }
//        if (logger.isInfoEnabled()) logger.info("Cleaned up and done.");
        
        return status;
    }

    private void initQueueConnection() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(Context.INITIAL_CONTEXT_FACTORY,
                       "weblogic.jndi.WLInitialContextFactory");
        // NOTE: The port number of the server is provided in the next line,
        //       followed by the userid and password on the next two lines.
        properties.put(Context.PROVIDER_URL, uri);
        properties.put(Context.SECURITY_PRINCIPAL, "mmsjmsuser");
        properties.put(Context.SECURITY_CREDENTIALS, "mm$jm$u$3r");

        try {
            ctx = new InitialContext(properties);
        } catch (NamingException ne) {
            ne.printStackTrace(System.err);
        }
//        if (logger.isInfoEnabled()) logger.info("Got InitialContext " + ctx.toString());

        // create QueueConnectionFactory
        try {
            qcf = (QueueConnectionFactory)ctx.lookup(QCF_NAME);
        }
        catch (NamingException ne) {
            ne.printStackTrace(System.err);
        }
//        if (logger.isInfoEnabled()) logger.info("Got QueueConnectionFactory " + qcf.toString());
        // create QueueConnection
        try {
            qc = qcf.createQueueConnection();
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
        }
    }


    @Override
    public String getUri() {
        return uri;
    }
    
    @Override
    public void setUri( String newUri ) {
        uri = newUri;
    }

    @Override
    public void setWorkspace( String workspace ) {
        this.workspace = workspace;
    }

    @Override
    public void setProjectId( String projectId ) {
        this.projectId = projectId;
    }

    
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put( "uri", uri );
        return json;
    }
    
    @Override
    public void ingestJson(JSONObject json) {
        if (json.has( "uri" )) {
            uri = json.isNull( "uri" ) ? null : json.getString( "uri" );
        }
    }
}
