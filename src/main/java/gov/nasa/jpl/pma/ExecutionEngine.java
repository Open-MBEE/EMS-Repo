package gov.nasa.jpl.pma;

import java.util.List;
import java.util.Map;

public interface ExecutionEngine {

    /**
     * createEngine
     * 
     * Creates an instance of the engine to be ran
     * @return 
     */
    JenkinsEngine createEngine();

    /**
     * execute
     * 
     * Execute specified event
     * 
     * @param event
     */
    void execute( );

    /**
     * getEventDetail Searches for specific detail within the queue of events to
     * be executed.
     * 
     * @param detail
     * @return
     */
    String getEventDetail( String eventName, String detail );

    /**
     * setEvent
     * 
     * Sets a single event to be executed.
     * 
     * @param event
     */
    void setEvent( String event );

    /**
     * setEvents
     * 
     * Sets multiple events to be executed.
     * 
     * @param event
     */
    void setEvents( List< String > event );

    void updateEvent( String event );

    /**
     * Stops currently executing event and should also stop all further events
     * to be executed as well.
     * 
     * @return Returns boolean if the execution was stopped properly. Should
     *         Return false if the execution is not running or if event is not
     *         found.
     */
    boolean stopExecution();

    /**
     * removeEventFromQueue
     * 
     * This method should receive an object that represents a queue to be
     * removed from the list.
     * 
     * @param event
     * @return returns if the removal of the event was successful. Should return
     *         false if the event is not found.
     */
    boolean removeEvent( String event );

    long getExecutionTime();
}
