package gov.nasa.jpl.view_repo.connections;

import java.util.Hashtable;

import gov.nasa.jpl.view_repo.util.NodeUtil;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;



//import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class JmsConnection implements ConnectionInterface {
    private static Logger logger = Logger.getLogger(JmsConnection.class);
    private long sequenceId = 0;
    private static String uri = null;
    private String workspace = null;
    private String projectId = null;

    private static InitialContext ctx = null;
    private static String ctxFactory = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
    private static String connFactory = "ConnectionFactory";
    private static String username = null;
    private static String password = null;
    private ConnectionFactory connectionFactory = null;
    
    
    public JmsConnection() {
    }
    
    protected boolean init() {
        if (connectionFactory == null) {
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put(Context.INITIAL_CONTEXT_FACTORY, ctxFactory);
            properties.put(Context.PROVIDER_URL, uri);
            if (username != null && password != null) {
                properties.put(Context.SECURITY_PRINCIPAL, username);
                properties.put(Context.SECURITY_CREDENTIALS, password);
            }

            try {
                ctx = new InitialContext(properties);
            } catch (NamingException ne) {
                ne.printStackTrace(System.err);
                return false;
            }

            try {
                connectionFactory = (ConnectionFactory) ctx.lookup(connFactory);
            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean publish(JSONObject json, String topic) {
        if (uri == null) return false;
        boolean result = false;
        try {
            json.put( "sequence", sequenceId++ );
            result = publishTopic(NodeUtil.jsonToString( json, 2 ), topic);
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    public boolean publish(JSONObject json) {
        if (uri == null) return false;
        // topic is always the same since we're using metadata for workspaces now
        return publish( json, "master" );
    }
    
    public boolean publishTopic(String msg, String topic) {
        if (connectionFactory == null) {
            if (init() == false) return false;
        }
        
        boolean status = true;
        try {
            // Create a Connection
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createTopic(topic);

            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // Create a message
            TextMessage message = session.createTextMessage(msg);
            if (workspace != null) {
                message.setStringProperty( "workspace", workspace );
            } else {
                message.setStringProperty( "workspace", "master" );
            }
            if (projectId != null) {
                message.setStringProperty( "projectId", projectId );
            }

            // Tell the producer to send the message
            producer.send(message);

            // Clean up
            session.close();
            connection.close();
        }
        catch (Exception e) {
            logger.error( "JMS exception caught, probably means JMS broker not up");
            status = false;
        }
        
        return status;
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

    public static String getCtxFactory() {
        return ctxFactory;
    }

    public static void setCtxFactory( String ctxFactory ) {
        JmsConnection.ctxFactory = ctxFactory;
    }

    public static String getConnFactory() {
        return connFactory;
    }

    public static void setConnFactory( String connFactory ) {
        JmsConnection.connFactory = connFactory;
    }
    
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put( "uri", uri );
        json.put( "connFactory", connFactory );
        json.put( "ctxFactory", ctxFactory );
        json.put( "password", password );
        json.put( "username", username );
        return json;
    }

    
    @Override
    public void ingestJson(JSONObject json) {
        if (json.has( "uri" )) {
            uri = json.isNull( "uri" ) ? null : json.getString( "uri" );
        }
        if (json.has( "connFactory" )) {
            connFactory = json.isNull("connFactory") ? null : json.getString( "connFactory" );
        }
        if (json.has( "ctxFactory" )) {
            ctxFactory = json.isNull("ctxFactory") ? null : json.getString( "ctxFactory" );
        }
        if (json.has( "password" )) {
            password = json.isNull("password") ? null : json.getString( "password" );
        }
        if (json.has( "username" )) {
            username = json.isNull("username") ? null : json.getString( "username" );
        }
    }

}
