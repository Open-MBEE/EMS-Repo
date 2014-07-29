package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.TimeUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;

public class WorkspaceDiff {
    private EmsScriptNode ws1;
    private Set<EmsScriptNode> elements;

    private EmsScriptNode ws2;
    private Set<EmsScriptNode> addedElements;
    private Set<EmsScriptNode> deletedElements;
    private Set<EmsScriptNode> movedElements;
    private Set<EmsScriptNode> updatedElements;

    public WorkspaceDiff(EmsScriptNode ws1, EmsScriptNode ws2) {
        this.ws1 = ws1;
        this.ws2 = ws2;
        
        elements = new HashSet<EmsScriptNode>();
        
        addedElements = new HashSet<EmsScriptNode>();
        movedElements = new HashSet<EmsScriptNode>();
        deletedElements = new HashSet<EmsScriptNode>();
        updatedElements = new HashSet<EmsScriptNode>();
    }

    public Set< EmsScriptNode > getAddedElements() {
        return addedElements;
    }

    public Set< EmsScriptNode > getDeletedElements() {
        return deletedElements;
    }

    public Set< EmsScriptNode > getElements() {
        return elements;
    }

    public Set< EmsScriptNode > getMovedElements() {
        return movedElements;
    }

    public Set< EmsScriptNode > getUpdatedElements() {
        return updatedElements;
    }

    public EmsScriptNode getWs1() {
        return ws1;
    }

    public EmsScriptNode getWs2() {
        return ws2;
    }

    public void setAddedElements( Set< EmsScriptNode > addedElements ) {
        this.addedElements = addedElements;
    }

    public void setDeletedElements( Set< EmsScriptNode > deletedElements ) {
        this.deletedElements = deletedElements;
    }

    public void setElements( Set< EmsScriptNode > elements ) {
        this.elements = elements;
    }

    public void setMovedElements( Set< EmsScriptNode > movedElements ) {
        this.movedElements = movedElements;
    }
    
    public void setUpdatedElements( Set< EmsScriptNode > updatedElements ) {
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
        jsonObject.put("name", ws.getName());
        jsonObject.put( "timestamp", TimeUtils.toTimestamp( dateTime ) );
    }
    
    private void addJSONArray(JSONObject jsonObject, String key, Date dateTime) throws JSONException {
        @SuppressWarnings( "unchecked" )
        Set<EmsScriptNode> set = (Set<EmsScriptNode>) ClassUtils.getFieldValue( this, key );
        if (set != null && set.size() > 0) {
            jsonObject.put( key, convertSetToJSONArray( set, dateTime ) );
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
        
        Scriptable json = node.getChildren();
        
        json.getIds();
    }
}
