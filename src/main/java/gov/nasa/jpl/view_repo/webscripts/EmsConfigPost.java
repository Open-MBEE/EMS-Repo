package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsConfig;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Utility service for setting log levels of specified classes on the fly
 * @author cinyoung
 *
 */
public class EmsConfigPost extends DeclarativeJavaWebScript {
    static Logger logger = Logger.getLogger( EmsConfigPost.class );
    
    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map< String, Object > result = new HashMap< String, Object >();

        StringBuffer msg = new StringBuffer();

        JSONObject response = new JSONObject();

        JSONArray requestJson;
        try {
            requestJson = (JSONArray)req.parseContent();
        } catch (JSONException e) {
            status.setCode( HttpServletResponse.SC_BAD_REQUEST );
            response.put( "msg", "JSON malformed" );
            result.put( "res", response );
            return result;
        }
        
        for ( int ii = 0; ii < requestJson.length(); ii++ ) {
            boolean failed = false;
            JSONObject json = requestJson.getJSONObject( ii );

            String propertyName = json.getString( "key" );
            String propertyValue = json.getString( "value" );

            try {
                EmsConfig.setProperty( propertyName, propertyValue );
            } catch ( Exception e ) {
                if (logger.isInfoEnabled()) logger.info( e.getMessage() );
                failed = true;            
            }
            
            if ( !failed ) {
                try {
                    JSONObject propertyObject = new JSONObject();
                    propertyObject.put( "key", propertyName );
                    propertyObject.put( "value", EmsConfig.get( propertyName ) );
                    if (!response.has( "properties" )) {
                        response.put( "properties", new JSONArray() );
                    }
                    response.getJSONArray( "properties" ).put( propertyObject );
                } catch (Exception e) {
                    if (logger.isInfoEnabled()) logger.info( e.getMessage() );
                    failed = true;
                }
            }
            
            if (failed) {
                msg.append( String.format( "could not update: %s=%s",
                                           propertyName, propertyValue ) );
            }
        }

        if (msg.length() > 0 ) {
            response.put( "msg", msg.toString() );
        }
        result.put( "res", response.toString( 2 ) );
        status.setCode( HttpServletResponse.SC_OK );

        return result;
    }

    public static
            Object
            getStaticValue( final String className, final String fieldName )
                                                                            throws SecurityException,
                                                                            NoSuchFieldException,
                                                                            ClassNotFoundException,
                                                                            IllegalArgumentException,
                                                                            IllegalAccessException {
        // Get the private field
        final Field field =
                Class.forName( className ).getDeclaredField( fieldName );
        // Allow modification on the field
        field.setAccessible( true );
        // Return the Obect corresponding to the field
        return field.get( Class.forName( className ) );
    }
}
