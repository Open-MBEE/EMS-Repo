package gov.nasa.jpl.view_repo.connections;

import gov.nasa.jpl.view_repo.util.NodeUtil;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

public class JmsConnection extends AbstractConnection {
    static Logger logger = Logger.getLogger(JmsConnection.class);
    long sequenceId = 0;
    
    // static so Spring can configure URI for everything
    private static String uri = "tcp://localhost:61616";
    private ActiveMQConnectionFactory connectionFactory = null;
    
    public JmsConnection() {
    }

    public void setUri(String uri) {
        if (logger.isInfoEnabled()) {
            logger.info("uri set to: " + uri);
        }
        JmsConnection.uri = uri;
    }
    
    public String getUri() {
        return JmsConnection.uri;
    }
    
    protected void init() {
        if (connectionFactory == null) {
            connectionFactory = new ActiveMQConnectionFactory( uri );
        }
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
            init();
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
}
