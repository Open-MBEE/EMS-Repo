package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Seen;
import gov.nasa.jpl.mbee.util.Utils;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import k.frontend.Frontend;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class K {
    static Logger logger = Logger.getLogger(K.class);

    public static JSONObject kToJson( String k, WorkspaceNode ws, Set< String > postSet  ) {
        return kToJson(k, null, ws, postSet);
    }

//    protected static void log(String msg, StringBuffer response) {
//        response.append(msg + "\n");
//        //TODO: add to responseStatus too (below)?
//        //responseStatus.setMessage(msg);
//    }

    protected static void log(Level level, String msg) {
        switch(level.toInt()) {
            case Level.FATAL_INT:
                logger.fatal(msg);
                break;
            case Level.ERROR_INT:
                logger.error( msg );
                break;
            case Level.WARN_INT:
                logger.warn(msg);
                break;
            case Level.INFO_INT:
                logger.info( msg );
                break;
            case Level.DEBUG_INT:   
                logger.debug( msg );
                break;
            default:
                // TODO: investigate if this the default thing to do
                if (Debug.isOn()){ logger.debug( msg ); }
                break;
        }
    }
    

    /**
     * Add elements' sysmlids in given json. sysmlids are only added where they
     * do not already exist. If there is more than one element, the prefix is
     * appended with an underscore followed by a count index. For example, if
     * there are three elements, and the prefix is "foo", then the sysmlids will
     * be foo_0, foo_1, and foo_2. This can be useful for temporary generated
     * elements so that they can overwrite themselves and reduce pollution.
     * 
     * @param json
     * @param sysmlidPrefix
     */
    public static void addSysmlIdsToElementJson(JSONObject json,
    		String sysmlidPrefix) {
    	if (json == null)
    		return;
    	if (sysmlidPrefix == null)
    		sysmlidPrefix = "generated_sysmlid_";
    	JSONArray elemsJson = json.optJSONArray("elements");
    	if (elemsJson != null) {
    		for (int i = 0; i < elemsJson.length(); ++i) {
    			JSONObject elemJson = elemsJson.getJSONObject(i);
    			if (elemJson != null && !elemJson.has("sysmlid")) {
    				String id = sysmlidPrefix
    						+ (elemsJson.length() > 1 ? "_" + i : "");
    				elemJson.put("sysmlid", id);
    			}
    		}
    	}
    }

    public static JSONObject kToJson( String k, String sysmlidPrefix, WorkspaceNode ws, Set< String > postSet  ) {
    	// JSONObject json = new JSONObject(KExpParser.parseExpression(k));
    	JSONObject json = new JSONObject(Frontend.exp2Json2(k));
    
    	if (sysmlidPrefix != null) {
    		addSysmlIdsToElementJson(json, sysmlidPrefix);
    	}
    	Level cat = logger.getLevel();
    	logger.setLevel( Level.DEBUG );
    	if (logger.isDebugEnabled()) {
    		log(Level.DEBUG,
    				"********************************************************************************");
    		log(Level.DEBUG, k);
    		if (logger.isDebugEnabled())
    			log(Level.DEBUG, NodeUtil.jsonToString(json, 4));
    		// log(LogLevel.DEBUG, NodeUtil.jsonToString( exprJson0, 4 ));
    		log(Level.DEBUG,
    				"********************************************************************************");
    
    		log(Level.DEBUG, "kToJson(k) = \n" + json.toString(4));
            log(Level.DEBUG, "jsonToK(json) = \n" + jsonToK(json) );
    	}
    
        changeMissingOperationElementsToStrings(json, ws, postSet);
    
        if ( logger.isDebugEnabled() ) log(Level.DEBUG, "kToJson(k) = \n" + json.toString( 4 ) );
        logger.setLevel( cat );
        
    	return json;
    }

    public static JSONObject toElementJson( Collection<EmsScriptNode> elements,
                                            WorkspaceNode ws, Date dateTime ) {
        JSONObject elementsJson = new JSONObject();
        JSONArray elementsArr = new JSONArray();
        elementsJson.put( "elements", elementsArr );

        for ( EmsScriptNode element : elements ) {
            try {
                JSONObject json = element.toJSONObject(ws, dateTime);
                elementsArr.put( json );
            } catch (JSONException e) {
                try {
                    log(Level.ERROR, "Could not create JSON for " + element );
                } catch ( Throwable t ) {}
                e.printStackTrace();
            }
        }
        return elementsJson;        
    }
    
    public static String toK( EmsScriptNode element ) {
        return toK( Utils.newList( element ) );
    }

    public static String toK( Collection<EmsScriptNode> elements ) {
        return toK( elements, null, null );
    }
    
    public static String toK( Collection<EmsScriptNode> elements, WorkspaceNode ws, Date dateTime ) {
        String k = null;
        try {
            JSONObject elementsJson = toElementJson( elements, ws, dateTime );
            k = jsonToK( elementsJson );
        } catch ( Throwable t ) {
            // TODO -- ignore?
            // t.printStackTrace();
        }
        return k;
    }
    
    public static String jsonToK(JSONObject json) {
        String k = Frontend.json2exp2( json.toString( 4 ) );
        return k;
    }

    public static void changeMissingOperationElementsToStrings( JSONObject json,
                                                                WorkspaceNode ws, Set< String > postSet ) {
        changeMissingOperationElementsToStrings( json, ws, postSet, null );
    }

    public static void changeMissingOperationElementsToStrings( JSONObject json,
                                                                WorkspaceNode ws,
                                                                Set< String > postSet,
                                                                Seen<String> seen ) {
        // Get element value if that's what this is.
        if ( json == null ) return;
        String type = json.optString( "type" );
        if ( type != null && "ElementValue".equals( type ) ) {
            String sysmlId = json.optString( "element" );
            if ( !Utils.isNullOrEmpty( sysmlId ) ) {
                // Make sure we haven't already processed this one.
                Pair< Boolean, Seen< String > > p =
                        Utils.seen( sysmlId, true, seen );
                if ( !p.first ) {
                    seen = p.second;
                    
                    boolean missing = true; // pessimistic; prove it's not missing
                    // See if we can find the operation in the existing set of
                    // posted elements.
                    if ( postSet != null && postSet.contains( sysmlId ) ) {
                        missing = false;
                    } else {
                        // See if it's in the database.
                        EmsScriptNode n =
                            NodeUtil.findScriptNodeById( sysmlId, ws, null, false,
                                                         NodeUtil.getServices(), null );
                        missing = n == null;
                    }
                    if ( missing ) {//|| !n.getTypeName().equals("Operation") ) {
                        // Wait!  no!  just change to a string literal:
                        json.remove("element");
                        json.put("type", "LiteralString");
                        json.put("string", sysmlId);
                    }
                }
            }
        }
        for ( Object ok : json.keySet() ) {
            if ( !( ok instanceof String ) ) {
                Debug.error( "JSONObject key is not a String!!" );
                continue;
            }
            String k = (String)ok;
            JSONObject v = json.optJSONObject( k );
            if ( v != null ) {
                changeMissingOperationElementsToStrings( v, ws, seen );
            } else {
                JSONArray arr = json.optJSONArray( k );
                if ( arr != null ) {
                    for ( int i = 0; i < arr.length(); ++i ) {
                        v = arr.optJSONObject( i );
                        if ( v != null ) {
                            changeMissingOperationElementsToStrings( v, ws, seen );
                        }
                    }
                }
            }
        }
    }

}
