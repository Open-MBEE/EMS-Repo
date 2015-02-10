package gov.nasa.jpl.view_repo.util;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

public class JsonArray extends JSONArray {

    public JsonArray() {
        // TODO Auto-generated constructor stub
    }

    public JsonArray( JSONTokener arg0 ) throws JSONException {
        super( arg0 );
        // TODO Auto-generated constructor stub
    }

    public JsonArray( String arg0 ) throws JSONException {
        super( arg0 );
        // TODO Auto-generated constructor stub
    }

    public JsonArray( Collection arg0 ) {
        super( arg0 );
    }

    public JsonArray( JSONArray arg0 ) throws JSONException {
        this();
        if ( arg0 == null ) return;
        for ( int i=0; i<arg0.length(); ++i ) {
            this.put( arg0.get( i ) );
        }
    }
    
    public static JsonArray make( JSONArray arg0 ) throws JSONException {
        if ( arg0 instanceof JsonArray ) return (JsonArray)arg0;
        return new JsonArray( arg0 );
    }
    
    public JsonArray( Object arg0 ) throws JSONException {
        super( arg0 );
    }

//    public JsonArray( Collection arg0, boolean arg1 ) {
//        super( arg0, arg1 );
//    }
//
//    public JsonArray( Object arg0, boolean arg1 ) throws JSONException {
//        super( arg0, arg1 );
//    }
//
    @Override
    public JsonArray getJSONArray( int arg0 ) throws JSONException {
        return make(super.getJSONArray( arg0 ));
    }

    @Override
    public JsonObject getJSONObject( int arg0 ) throws JSONException {
        return JsonObject.make(super.getJSONObject( arg0 ));
    }

    @Override
    public JsonArray optJSONArray( int arg0 ) {
        try {
            return make(super.optJSONArray( arg0 ));
        } catch ( JSONException e ) {
        }
        return null;
    }

    @Override
    public JsonObject optJSONObject( int arg0 ) {
        // TODO Auto-generated method stub
        return JsonObject.make( super.optJSONObject( arg0 ) );
    }

    @Override
    public JsonObject toJSONObject( JSONArray arg0 ) throws JSONException {
        // TODO Auto-generated method stub
        return JsonObject.make( super.toJSONObject( arg0 ) );
    }
    
}
