package gov.nasa.jpl.view_repo.webscripts;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Allows heisenCache to be turned on/off
 */
public abstract class FlagSet extends DeclarativeWebScript {
    static Logger logger = Logger.getLogger(FlagSet.class);
   
    protected abstract void set( boolean val ); 
    protected abstract boolean get();
    protected abstract String flagName();    
    
    protected Map<String, Object> executeImpl(WebScriptRequest req,
                                              Status status, Cache cache) {
        FlagSet f = null;
        try {
            f = this.getClass().newInstance();
        } catch ( InstantiationException e ) {
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        }
        if ( f != null ) {
            return f.executeImplImpl( req, status, cache );
        } else {
            return executeImplImpl( req, status, cache );            
        }
    }
    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
                Status status, Cache cache) {
        Map< String, Object > model = new HashMap< String, Object >();

        String turnOnStr = req.getParameter( "on" );
        String turnOffStr = req.getParameter( "off" );

        String isOnStr = null;
        boolean justAsking = false;
        if ( turnOnStr == null && turnOffStr == null ) {
            isOnStr = req.getParameter( "ison" );
            if ( isOnStr == null ) {
                isOnStr = req.getParameter( "isOn" );
            }
            justAsking = isOnStr != null;
        }
        
        boolean turnOn = !justAsking && 
                         !( ( turnOnStr != null &&
                              turnOnStr.trim().equalsIgnoreCase( "false" ) ) ||
                            ( turnOffStr != null &&
                              !turnOffStr.trim().equalsIgnoreCase( "false" ) ) );
        turnOnStr = turnOn ? "on" : "off";
        String msg = null;
        if ( justAsking ) {
            msg = flagName() + " is " + ( get() ? "on" : "off" );
        } else if ( turnOn == get() ) {
            msg = flagName() + " is already " + turnOnStr;
        } else {
            set( turnOn );
            msg = flagName() + " turned " + turnOnStr;
        }
        if (logger.isInfoEnabled()) {
            logger.info( ( new Date() ) + ": " + msg );
        }
        model.put( "res", msg );

        return model;
    }

}
