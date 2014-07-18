package gov.nasa.jpl.view_repo.jms;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.activemq.ActiveMQConnectionFactory;

public class JmsConnection {
    private long sequenceId = 0;
    private volatile static JmsConnection INSTANCE = null;
    
    private String uri = "tcp://localhost:61616";
    private ActiveMQConnectionFactory connectionFactory = null;
    
    private JmsConnection() {
    }

    public static JmsConnection getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JmsConnection();
        }
        return INSTANCE;
    }
        
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    protected void init() {
        if (connectionFactory == null) {
            connectionFactory = new ActiveMQConnectionFactory( uri );
        }
    }
    
    public boolean publishTopic(JSONObject json, String topic) {
        boolean result = false;
        try {
            json.put( "sequence", sequenceId++ );
            result = publishTopic(json.toString( 2 ), topic);
        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
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

            // Tell the producer to send the message
            producer.send(message);

            // Clean up
            session.close();
            connection.close();
        }
        catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
            status = false;
        }
        
        return status;
    }
    
}
