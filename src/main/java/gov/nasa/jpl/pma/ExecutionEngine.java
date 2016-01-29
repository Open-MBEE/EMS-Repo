package gov.nasa.jpl.pma;

import java.util.List;
import java.util.Map;

public interface ExecutionEngine {

    /**
     * createEngine
     * 
     * Creates an instance of the engine to be ran
     */
    void createEngine();

    /**
     * Execute currently queued job
     */
    void execute();

    /**
     * execute
     * 
     * Execute specified event
     * 
     * @param event
     */
    void execute(Object event);

    /**
     * execute
     * 
     * Provides a set of events for the executionEngine to process.
     * 
     * @param events
     */
    void execute(List<Object> events);

    /**
     * executeQueue
     * 
     * Executes all events that are in queue.
     * 
     * @param event
     */
    void executeQueue();
    
    /**
     * isRunning
     * 
     * Returns the status of the executionEngine if it is running or not.
     * 
     * @return
     */
    boolean isRunning();

    /**
     * getExecutionStatus
     * 
     * Returns the integer form of status from the last execution
     * 
     * @return
     */
    int getExecutionStatus();

    /**
     * getEvent
     * 
     * Returns an Object of the event to be executed.
     * 
     * @return Object type of event
     */
    Object getEvent();

    /**
     * getEvents
     * 
     * Returns a list of objects that are in queue to be executed
     * 
     * @return
     */
    Map<String, Object> getEvents();

    /**
     * getExecutionQueue
     * 
     * Returns all events that are in queue to be executed by the
     * ExecutionEngine
     * 
     * @return
     */
    String getExecutionQueue();

    /**
     * getEventDetail
     *  Searches for specific detail within the queue of events to be executed.
     *  
     * @param detail
     * @return
     */
    Object getEventDetail(String detail);
    
    /**
     * getEventDetail
     * 
     * This method will return the details of the event that is to be executed next.
     * @return
     */
    Object getEventDetails();
    
    /**
     * setEvent
     * 
     * Sets a single event to be executed.
     * 
     * @param event 
     */
    void setEvent(Object event);

    /**
     * setEvents
     * 
     * Sets multiple events to be executed.
     * 
     * @param event 
     */
    void setEvents(List<Object> event);
    
    void updateEvent(String event);

    /**
     * Stops currently executing event and should also stop all further events
     * to be executed as well.
     * 
     * @return Returns boolean if the execution was stopped properly. Should Return false if the execution is not running or if event is not found.
     */
    boolean stopExecution();

    /**
     * removeEventFromQueue
     * 
     * This method should receive an object that represents a queue to be
     * removed from the list.
     * 
     * @param event
     * @return returns if the removal of the event was successful. Should return false if the event is not found.
     */
    boolean removeEvent(Object event);
}
