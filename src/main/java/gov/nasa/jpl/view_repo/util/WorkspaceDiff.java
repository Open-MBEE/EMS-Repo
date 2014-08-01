package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.TimeUtils;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;

public class WorkspaceDiff {
    private EmsScriptNode ws1;
    private Set<EmsScriptNode> elements;

    private EmsScriptNode ws2;
    private Map<String, EmsScriptNode> addedElements;
    private Map<String, EmsScriptNode> deletedElements;
    private Map<String, EmsScriptNode> movedElements;
    private Map< String, Map< EmsScriptNode, Set< String > > > updatedElements;

    public WorkspaceDiff() {
        elements = new TreeSet<EmsScriptNode>();
        
        addedElements = new TreeMap<String, EmsScriptNode>();
        movedElements = new TreeMap<String, EmsScriptNode>();
        deletedElements = new TreeMap<String, EmsScriptNode>();
        updatedElements = new TreeMap< String, Map< EmsScriptNode, Set< String > > >();
        
        ws1 = null;
        ws2 = null;
    }
    
    public WorkspaceDiff(EmsScriptNode ws1, EmsScriptNode ws2) {
        this();
        this.ws1 = ws1;
        this.ws2 = ws2;
    }

    public Map< String, EmsScriptNode > getAddedElements() {
        return addedElements;
    }

    public Map< String, EmsScriptNode > getDeletedElements() {
        return deletedElements;
    }

    public Set< EmsScriptNode > getElements() {
        return elements;
    }

    public Map< String, EmsScriptNode > getMovedElements() {
        return movedElements;
    }

    public Map< String, Map< EmsScriptNode, Set< String >>> getUpdatedElements() {
        return updatedElements;
    }

    public EmsScriptNode getWs1() {
        return ws1;
    }

    public EmsScriptNode getWs2() {
        return ws2;
    }

    public void setAddedElements( Map< String, EmsScriptNode > addedElements ) {
        this.addedElements = addedElements;
    }

    public void setDeletedElements( Map< String, EmsScriptNode > deletedElements ) {
        this.deletedElements = deletedElements;
    }

    public void setElements( Set< EmsScriptNode > elements ) {
        this.elements = elements;
    }

    public void setMovedElements( Map< String, EmsScriptNode > movedElements ) {
        this.movedElements = movedElements;
    }
    
    public void setUpdatedElements( Map< String, Map< EmsScriptNode, Set< String >>> updatedElements ) {
        this.updatedElements = updatedElements;
    }
    
    public void setWs1( EmsScriptNode ws1 ) {
        this.ws1 = ws1;
    }
    
    public void setWs2( EmsScriptNode ws2 ) {
        this.ws2 = ws2;
    }
      
    public JSONObject toJSONObject(Date time1, Date time2) throws JSONException {
        JSONObject deltaJson = new JSONObject();
        JSONObject ws1Json = new JSONObject();
        JSONObject ws2Json = new JSONObject();
        
        addJSONArray(ws1Json, "elements", time1);
        addWorkspaceMetadata( ws1Json, ws1, time1 );
        
        addJSONArray(ws2Json, "addedElements", time2);
        addJSONArray(ws2Json, "movedElements", time2);
        addJSONArray(ws2Json, "deletedElements", time2);
        addJSONArray(ws2Json, "updatedElements", time2);
        addWorkspaceMetadata( ws2Json, ws2, time2);
        
        deltaJson.put( "workspace1", ws1Json );
        deltaJson.put( "workspace2", ws2Json );
        
        return deltaJson;
    }
    
    private void addWorkspaceMetadata(JSONObject jsonObject, EmsScriptNode ws, Date dateTime) throws JSONException {
        if (ws == null) {
            jsonObject.put( "name", "master" );
        } else {
            jsonObject.put("name", ws.getName());
        }
        jsonObject.put( "timestamp", TimeUtils.toTimestamp( dateTime ) );
    }
    
    private void addJSONArray(JSONObject jsonObject, String key, Date dateTime) throws JSONException {
        try {
            Field field = this.getClass().getDeclaredField( key );
            field.setAccessible( true );
            @SuppressWarnings( "unchecked" )
            Set< EmsScriptNode > set = (Set<EmsScriptNode>) field.get( this );
            if (set != null && set.size() > 0) {
                jsonObject.put( key, convertSetToJSONArray( set, dateTime ) );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    private JSONArray convertSetToJSONArray(Set<EmsScriptNode> set, Date dateTime) throws JSONException {
        JSONArray array = new JSONArray();
        for (EmsScriptNode node: set) {
            array.put( node.toJSONObject( dateTime ) );
        }
        return array;
    }
    
    public boolean diff() {
        boolean status = true;
        
        captureDeltas(ws2);
        
        return status;
    }
    
    private void captureDeltas(EmsScriptNode node) {
        // delta 
        Set< EmsScriptNode > children = node.getChildNodes();
        
    }
    
    public static Set<EmsScriptNode> convertMapValuesToSet(Map<String, EmsScriptNode> map) {
        Set<EmsScriptNode> set = new LinkedHashSet<EmsScriptNode>();
        for (EmsScriptNode node: map.values()) {
            set.add( node );
        }
        return set;
    }
}
