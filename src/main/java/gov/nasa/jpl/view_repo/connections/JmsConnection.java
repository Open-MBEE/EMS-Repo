package gov.nasa.jpl.view_repo.connections;

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
    
    private String uri = "tcp://localhost:61616";
    private ActiveMQConnectionFactory connectionFactory = null;
    
    public JmsConnection() {
    }

    public void setUri(String uri) {
        if (logger.isDebugEnabled()) {
            logger.debug("uri set to: " + uri);
        }
        this.uri = uri;
    }
    
    protected void init() {
        if (connectionFactory == null) {
            connectionFactory = new ActiveMQConnectionFactory( uri );
        }
    }
    
    public boolean publish(JSONObject json, String topic) {
        boolean result = false;
        try {
            json.put( "sequence", sequenceId++ );
            result = publishTopic(json.toString( 2 ), topic);
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
        
        return result;
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
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

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
