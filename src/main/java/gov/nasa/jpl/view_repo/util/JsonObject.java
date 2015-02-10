package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonObject extends org.json.JSONObject {

    public static boolean doCaching = true;
    public static Map<JsonObject, Map< Integer, Pair< Date, String > > > stringCache =
            Collections.synchronizedMap( new HashMap< JsonObject, Map< Integer, Pair< Date, String > > >() );
    public static long cacheHits = 0;
    public static long cacheMisses = 0;

    public JsonObject( JsonObject arg0, String[] arg1 ) throws JSONException {
        super( arg0, arg1 );
    }

    public JsonObject( JSONTokener arg0 ) throws JSONException {
        super( arg0 );
    }

    public JsonObject( JSONObject arg0 ) {
        super( toMap( arg0 ) );
        //this(arg0, arg0 == null ? null : toArray(arg0.keys()));
    }
    
    public static Map<String,Object> toMap( JSONObject arg0 ) {
        Map< String, Object > m = new LinkedHashMap< String, Object >();
        String[] names = arg0.getNames( arg0 );
        try {
            for ( String name : names ) {
                m.put( name, arg0.get(name) );
            }
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
        return m;
    }

    public static JsonObject make( JSONObject arg0 ) {
        if ( arg0 instanceof JsonObject ) return (JsonObject)arg0;
        return new JsonObject( arg0 );
    }

    // TODO -- move this to Utils
    public static String[] toArray( Iterator<?> keys ) {
        ArrayList<String> keyList = new ArrayList< String >();
        while ( keys.hasNext() ) {
            keyList.add( (String)keys.next() );
        }
        String[] keyArray = new String[keyList.size()];
        keyList.toArray( keyArray );
        return keyArray;
    }

//    public JsonObject( Map arg0, boolean arg1 ) {
//        super( arg0, arg1 );
//    }

    public JsonObject( Map arg0 ) {
        super( arg0 );
    }

//    public JsonObject( Object arg0, boolean arg1 ) {
//        super( arg0, arg1 );
//    }

    public JsonObject( Object arg0, String[] arg1 ) {
        super( arg0, arg1 );
    }

    public JsonObject( Object arg0 ) {
        super( arg0 );
    }

    public JsonObject( String arg0 ) throws JSONException {
        super( arg0 );
    }

    public JsonObject() {
        super();
    }

    @Override
    public JsonArray toJSONArray( JSONArray arg0 ) throws JSONException {
        return JsonArray.make( super.toJSONArray( arg0 ) );
    }

    @Override
    public JsonArray getJSONArray( String arg0 ) throws JSONException {
        return JsonArray.make( super.getJSONArray( arg0 ) );
    }

    @Override
    public JsonObject getJSONObject( String arg0 ) throws JSONException {
        return make( super.getJSONObject( arg0 ) );
    }

    @Override
    public JsonArray optJSONArray( String arg0 ) {
        try {
            return JsonArray.make( super.optJSONArray( arg0 ) );
        } catch ( JSONException e ) {
        }
        return null;
    }

    @Override
    public JsonObject optJSONObject( String arg0 ) {
        return make( super.optJSONObject( arg0 ) );
    }
    
    @Override
    public String toString() {
        if ( !doCaching ) return super.toString();
        try {
            return toString( 0 );
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public String toString(int numSpacesToIndent) throws JSONException {
        if ( !doCaching ) return super.toString( numSpacesToIndent );
        String result = null;
        String modString = optString("modified");
        Date mod = null;
        // Only cache json with a modified date so that we know when to update
        // it.
        if ( modString != null ) {
            mod = TimeUtils.dateFromTimestamp( modString );
            if ( mod != null && stringCache.containsKey( this ) ) {
                Pair< Date, String > p = Utils.get( stringCache, this, numSpacesToIndent );//stringCache.get( this );
                if ( p != null ) {
                    if ( p.first != null && !mod.after( p.first ) ) {
                        result = p.second;
                        // cache hit
                        ++cacheHits;
                        return result;
                    }
                }
            }
        }
//        if ( numSpacesToIndent == 0 ) {
//            result = super.toString();
//        } else {
            result = super.toString(numSpacesToIndent);
//        }
        if ( mod == null ) {
            // cache not applicable
        } else {
            // cache miss; add to cache
            ++cacheMisses;
            Utils.put(stringCache, this, numSpacesToIndent,
                      new Pair< Date, String >( mod, result ) );
        }
        return result;
    }
}

