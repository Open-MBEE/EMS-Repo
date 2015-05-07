package gov.nasa.jpl.view_repo.connections;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import gov.nasa.jpl.view_repo.util.NodeUtil;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * curl -u admin:admin -H Content-Type:application/json http://localhost:8080/alfresco/service/connection/jms -d '{"ctxFactory":"weblogic.jndi.WLInitialContextFactory", "username":"mmsjmsuser", "password":"mm$jm$u$3r", "connFactory":"jms/JPLEuropaJMSModuleCF", "topicName":"jms/MMSDistributedTopic", "uri":"t3s://orasoa-dev07.jpl.nasa.gov:8111"}'
 * curl -u admin:admin -H Content-Type:application/json http://localhost:8080/alfresco/service/connection/jms -d '{"ctxFactory":"weblogic.jndi.WLInitialContextFactory", "username":"mmsjmsuser", "password":"mm$jm$u$3r", "connFactory":"jms/JPLEuropaJMSModuleCF", "topicName":"jms/MMSDistributedTopic", "uri":"t3://orasoa-dev07.jpl.nasa.gov:8011"}'
 * curl -u admin:admin -H Content-Type:application/json http://localhost:8080/alfresco/service/connection/jms -d '{"ctxFactory":"org.apache.activemq.jndi.ActiveMQInitialContextFactory", "username":null, "password":null, "connFactory":"ConnectionFactory", "topicName":"master", "uri":"tcp://localhost:61616"}'
 * @author cinyoung
 *
 */
public class JmsConnection2 implements ConnectionInterface {
    private static Logger logger = Logger.getLogger(JmsConnection2.class);
    private long sequenceId = 0;
    private String workspace = null;
    private String projectId = null;

    private static String hostname = null;
    private static ServiceRegistry services;
    private static Map<String, ConnectionInfo> connectionMap = new HashMap<String, ConnectionInfo>();

    public enum DestinationType {
        TOPIC, QUEUE
    }

    class ConnectionInfo {
        public InitialContext ctx = null;
        public String ctxFactory = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
        public String connFactory = "ConnectionFactory";
        public String username = null;
        public String password = null;
        public String name = "master";
        public String uri = null;
        public ConnectionFactory connectionFactory = null;
        public DestinationType destType = DestinationType.TOPIC;
    }
    
    public JmsConnection2() {
    }
    
    protected String getHostname() {
        if (hostname == null) {
            hostname = services.getSysAdminParams().getAlfrescoHost();
        }
        return hostname;
    }
    
    protected boolean init() {
        System.setProperty("weblogic.security.SSL.ignoreHostnameVerification", "true");
        System.setProperty ("jsse.enableSNIExtension", "false");
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
        return publish( json, topicName );
    }
    
    public boolean publishTopic(String msg, String topic) {
        if (init() == false) return false;
        
        boolean status = true;
        try {
            // Create a Connection
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // lookup the destination
            Destination destination;
            try {
                destination = (Topic) ctx.lookup( topic );
            } catch (NameNotFoundException nnfe) {
                // Create the destination (Topic or Queue)
                destination = session.createTopic(topic);
            }

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
            message.setLongProperty( "MessageID", sequenceId++ );
            message.setStringProperty( "MessageSource", getHostname() );
            message.setStringProperty( "MessageRecipient", "TMS" );
            message.setStringProperty( "MessageType", "DELTA" );


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
        JmsConnection2.ctxFactory = ctxFactory;
    }

    public static String getConnFactory() {
        return connFactory;
    }

    public static void setConnFactory( String connFactory ) {
        JmsConnection2.connFactory = connFactory;
    }
    
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (uri.contains( "localhost" )) {
            uri = uri.replace("localhost", getHostname());
        }
        json.put( "uri", uri );
        json.put( "connFactory", connFactory );
        json.put( "ctxFactory", ctxFactory );
        json.put( "password", password );
        json.put( "username", username );
        json.put( "topicName", topicName );
        return json;
    }

    @Override
    public void ingestJson(JSONObject json) {
        String eventType = null;
        if (json.has( "eventType" )) {
            eventType = json.isNull( "eventType" ) ? null : json.getString( "eventType" );
        }
        if (eventType == null) {
            eventType = "delta";
        }
        
        ConnectionInfo ci;
        if (connectionMap.containsKey( eventType )) {
            ci = connectionMap.get( eventType );
        } else {
            ci = new ConnectionInfo();
        }
        
        if (json.has( "uri" )) {
            ci.uri = json.isNull( "uri" ) ? null : json.getString( "uri" );
        }
        if (json.has( "connFactory" )) {
            ci.connFactory = json.isNull("connFactory") ? null : json.getString( "connFactory" );
        }
        if (json.has( "ctxFactory" )) {
            ci.ctxFactory = json.isNull("ctxFactory") ? null : json.getString( "ctxFactory" );
        }
        if (json.has( "password" )) {
            ci.password = json.isNull("password") ? null : json.getString( "password" );
        }
        if (json.has( "username" )) {
            ci.username = json.isNull("username") ? null : json.getString( "username" );
        }
        if (json.has( "destName" )) {
            ci.name = json.isNull( "destName" ) ? null : json.getString( "destName" );
        }
        if (json.has( "destType" )) {
            
        }
    }

    public void setServices( ServiceRegistry services ) {
        JmsConnection2.services = services;
    }

}
