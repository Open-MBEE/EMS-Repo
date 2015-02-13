package gov.nasa.jpl.view_repo.util;

import java.util.Collection;

//import org.json.JSONArray;
import org.json.JSONException;
//import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONArray extends org.json.JSONArray {

    public JSONArray() {
        // TODO Auto-generated constructor stub
    }

    public JSONArray( JSONTokener arg0 ) throws JSONException {
        super( arg0 );
        // TODO Auto-generated constructor stub
    }

    public JSONArray( String arg0 ) throws JSONException {
        super( arg0 );
        // TODO Auto-generated constructor stub
    }

    public JSONArray( Collection arg0 ) {
        super( arg0 );
    }

    public JSONArray( org.json.JSONArray arg0 ) throws JSONException {
        this();
        if ( arg0 == null ) return;
        for ( int i=0; i<arg0.length(); ++i ) {
            Object val = arg0.get(i);
            if ( val instanceof org.json.JSONObject ) {
                val = JSONObject.make((org.json.JSONObject)val);
            } else if ( val instanceof org.json.JSONArray ) {
                val = JSONArray.make( (org.json.JSONArray)val );
            }
            this.put( val );
        }
    }
    
    public static JSONArray make( org.json.JSONArray arg0 ) throws JSONException {
        if ( arg0 instanceof JSONArray ) return (JSONArray)arg0;
        return new JSONArray( arg0 );
    }
    
    public JSONArray( Object arg0 ) throws JSONException {
        super( arg0 );
    }

//    public JSONArray( Collection arg0, boolean arg1 ) {
//        super( arg0, arg1 );
//    }
//
//    public JSONArray( Object arg0, boolean arg1 ) throws JSONException {
//        super( arg0, arg1 );
//    }
//
    @Override
    public JSONArray getJSONArray( int arg0 ) throws JSONException {
        return make(super.getJSONArray( arg0 ));
    }

    @Override
    public JSONObject getJSONObject( int arg0 ) throws JSONException {
        return JSONObject.make(super.getJSONObject( arg0 ));
    }

    @Override
    public JSONArray optJSONArray( int arg0 ) {
        try {
            return make(super.optJSONArray( arg0 ));
        } catch ( JSONException e ) {
        }
        return null;
    }

    @Override
    public JSONObject optJSONObject( int arg0 ) {
        // TODO Auto-generated method stub
        return JSONObject.make( super.optJSONObject( arg0 ) );
    }

    @Override
    public JSONObject toJSONObject( org.json.JSONArray arg0 ) throws JSONException {
        // TODO Auto-generated method stub
        return JSONObject.make( super.toJSONObject( arg0 ) );
    }
    
}
