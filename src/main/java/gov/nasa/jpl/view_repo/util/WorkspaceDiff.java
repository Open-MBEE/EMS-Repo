package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.AbstractDiff;
import gov.nasa.jpl.mbee.util.TimeUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class for keeping track of differences between two workspaces. WS1 always has the original
 * elements, while WS2 always has just the deltas.
 * @author cinyoung
 *
 */
public class WorkspaceDiff {
    private WorkspaceNode ws1;
    private Map<String, EmsScriptNode> elements;
    private Map<String, Version> elementsVersions;

    private WorkspaceNode ws2;
    private Map<String, EmsScriptNode> addedElements;
    private Map<String, EmsScriptNode> conflictedElements;
    private Map<String, EmsScriptNode> deletedElements;
    private Map<String, EmsScriptNode> movedElements;
    private Map< String, EmsScriptNode > updatedElements;

    NodeDiff nodeDiff = null;

    public WorkspaceDiff() {
        elements = new TreeMap<String, EmsScriptNode>();
        elementsVersions = new TreeMap<String, Version>();

        addedElements = new TreeMap<String, EmsScriptNode>();
        conflictedElements = new TreeMap<String, EmsScriptNode>();
        deletedElements = new TreeMap<String, EmsScriptNode>();
        movedElements = new TreeMap<String, EmsScriptNode>();
        updatedElements = new TreeMap< String, EmsScriptNode >();

        ws1 = null;
        ws2 = null;
    }

    public WorkspaceDiff(WorkspaceNode ws1, WorkspaceNode ws2) {
        this();
        this.ws1 = ws1;
        this.ws2 = ws2;
    }

    public Map< String, EmsScriptNode > getAddedElements() {
        return addedElements;
    }

    public Map<String, EmsScriptNode> getConflictedElements() {
        return conflictedElements;
    }

    public Map< String, EmsScriptNode > getDeletedElements() {
        return deletedElements;
    }

    public Map< String, EmsScriptNode > getElements() {
        return elements;
    }

    public Map< String, Version > getElementsVersions() {
        return elementsVersions;
    }

    public Map< String, EmsScriptNode > getMovedElements() {
        return movedElements;
    }

    public Map< String, EmsScriptNode > getUpdatedElements() {
        return updatedElements;
    }

    public WorkspaceNode getWs1() {
        return ws1;
    }

    public WorkspaceNode getWs2() {
        return ws2;
    }

    public void setAddedElements( Map< String, EmsScriptNode > addedElements ) {
        this.addedElements = addedElements;
    }

    public void setConflictedElements( Map<String, EmsScriptNode> conflictedElements ) {
        this.conflictedElements = conflictedElements;
    }

    public void setDeletedElements( Map< String, EmsScriptNode > deletedElements ) {
        this.deletedElements = deletedElements;
    }

    public void setElements(Map< String, EmsScriptNode > elements ) {
        this.elements = elements;
    }

    public void setElementsVersions( Map< String, Version > elementsVersions ) {
        this.elementsVersions = elementsVersions;
    }

    public void setMovedElements( Map< String, EmsScriptNode > movedElements ) {
        this.movedElements = movedElements;
    }

    public void setUpdatedElements( Map< String, EmsScriptNode> updatedElements ) {
        this.updatedElements = updatedElements;
    }

    public void setWs1( WorkspaceNode ws1 ) {
        this.ws1 = ws1;
    }

    public void setWs2( WorkspaceNode ws2 ) {
        this.ws2 = ws2;
    }
    /**
     * Dumps the JSON delta based.
     * @param   time1   Timestamp to dump ws1
     * @param   time2   Timestamp to dump ws2
     * @return  JSONObject of the delta
     * @throws JSONException
     */
    public JSONObject toJSONObject(Date time1, Date time2) throws JSONException {
            return toJSONObject( time1, time2, false );
    }

    /**
     * Dumps the JSON delta based.
     * @param   time1   Timestamp to dump ws1
     * @param   time2   Timestamp to dump ws2
     * @param   showAll If true, shows all keys in JSONObject, if false, only shows ids
     * @return  JSONObject of the delta
     * @throws JSONException
     */
    public JSONObject toJSONObject(Date time1, Date time2, boolean showAll) throws JSONException {
        JSONObject deltaJson = new JSONObject();
        JSONObject ws1Json = new JSONObject();
        JSONObject ws2Json = new JSONObject();

        addJSONArray(ws1Json, "elements", elements, elementsVersions, time1, showAll);
        addWorkspaceMetadata( ws1Json, ws1, time1 );

        addJSONArray(ws2Json, "addedElements", addedElements, time2, showAll);
        addJSONArray(ws2Json, "movedElements", movedElements, time2, showAll);
        addJSONArray(ws2Json, "deletedElements", deletedElements, time2, showAll);
        addJSONArray(ws2Json, "updatedElements", updatedElements, time2, showAll);
        addWorkspaceMetadata( ws2Json, ws2, time2);

        deltaJson.put( "workspace1", ws1Json );
        deltaJson.put( "workspace2", ws2Json );

        return deltaJson;
    }

    /**
     * Add the workspace metadata onto the provided JSONObject
     * @param jsonObject
     * @param ws
     * @param dateTime
     * @throws JSONException
     */
    private void addWorkspaceMetadata(JSONObject jsonObject, EmsScriptNode ws, Date dateTime) throws JSONException {
        if (ws == null) {
            jsonObject.put( "name", "master" );
        } else {
            jsonObject.put("name", ws.getName());
        }
        if (dateTime != null) {
            jsonObject.put( "timestamp", TimeUtils.toTimestamp( dateTime ) );
        }
    }

    private boolean addJSONArray(JSONObject jsonObject, String key, Map< String, EmsScriptNode > map, Date dateTime, boolean showAll) throws JSONException {
            return addJSONArray(jsonObject, key, map, null, dateTime, showAll);
    }

    private boolean addJSONArray(JSONObject jsonObject, String key, Map< String, EmsScriptNode > map, Map< String, Version> versions, Date dateTime, boolean showAll) throws JSONException {
        boolean emptyArray = true;
        if (map != null && map.size() > 0) {
            jsonObject.put( key, convertMapToJSONArray( map, versions, dateTime, showAll ) );
            emptyArray = false;
        } else {
            // add in the empty array
            jsonObject.put( key, new JSONArray() );
        }
        return !emptyArray;
    }

    private JSONArray convertMapToJSONArray(Map<String, EmsScriptNode> set, Map<String, Version> versions, Date dateTime, boolean showAll) throws JSONException {
        Set<String> filter = null;
        if (!showAll) {
            filter = new HashSet<String>();
            filter.add("id");
        }

        JSONArray array = new JSONArray();
        for (EmsScriptNode node: set.values()) {
            if ( versions == null || versions.size() <= 0 ) {
                array.put( node.toJSONObject( filter, dateTime ) );
            } else {
                JSONObject jsonObject = node.toJSONObject( filter, dateTime );
                Version version = versions.get( node.getName() );
                if ( version != null) {
                    // TODO: perhaps add service and response in method call rather than using the nodes?
                    EmsScriptNode changedNode = new EmsScriptNode(version.getVersionedNodeRef(), node.getServices(), node.getResponse());

                    // for reverting need to keep track of noderef and versionLabel
                    jsonObject.put( "id", changedNode.getId() );
                    jsonObject.put( "version", version.getVersionLabel() );
                    array.put( jsonObject );
                }
            }
        }

        return array;
    }

    public boolean diff() {
        boolean status = true;

        captureDeltas(ws2);

        return status;
    }

    protected void captureDeltas(WorkspaceNode node) {
        Set<NodeRef> s1 = ws1.getChangedNodeRefsWithRespectTo( node );
        Set<NodeRef> s2 = node.getChangedNodeRefsWithRespectTo( ws1 );
        nodeDiff = new NodeDiff( s1, s2 );
        populateMembers();
    }

    protected void populateMembers() {
        if ( nodeDiff == null ) return;

        Set< NodeRef > foo = nodeDiff.getAdded();

        // TODO Auto-generated method stub

    }

    public boolean ingestJSON(JSONObject json) {
        return true;
    }
}
