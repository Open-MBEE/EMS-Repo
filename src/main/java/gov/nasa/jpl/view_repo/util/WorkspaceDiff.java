package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
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

    /**
     * A conflict between two workspaces may be defined as
     * <ol>
     * <li>non-equal changes to the same element or
     * <li>non-equal changes to the same property of the same element.
     * </ol>
     * The second definition is more strict than the first and implies the
     * first. If conflictMustBeChangeToSameProperty is true, the second
     * definition is used, else the first.
     */
    public boolean conflictMustBeChangeToSameProperty;

    public boolean onlyModelElements = true;

    private WorkspaceNode ws1;
    private WorkspaceNode ws2;

    private Date timestamp1;
    private Date timestamp2;

    private Map<String, EmsScriptNode> elements;
    private Map<String, Version> elementsVersions;

    private Map<String, EmsScriptNode> addedElements;
    private Map<String, EmsScriptNode> conflictedElements;
    private Map<String, EmsScriptNode> deletedElements;
    private Map<String, EmsScriptNode> movedElements;
    private Map< String, EmsScriptNode > updatedElements;

    NodeDiff nodeDiff = null;

    private WorkspaceDiff() {
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

    /**
     * Constructor only for creating
     * @param ws1
     * @param ws2
     */
    public WorkspaceDiff(WorkspaceNode ws1, WorkspaceNode ws2) {
        this();
        this.ws1 = ws1;
        this.ws2 = ws2;
    }

    public WorkspaceDiff(WorkspaceNode ws1, WorkspaceNode ws2, Date timestamp1, Date timestamp2 ) {
        this(ws1, ws2);
        this.timestamp1 = timestamp1;
        this.timestamp2 = timestamp2;
        diff();
    }

    /**
     * A simple utility to add elements to the added, deleted, and updated sets
     * based on differences in their existence.
     *
     * @param node1
     * @param node2
     */
    protected void addToDiff( EmsScriptNode node1, EmsScriptNode node2 ) {
        boolean exists1 = NodeUtil.exists( node1 );
        boolean exists2 = NodeUtil.exists( node2 );
        String name;
        if ( !exists2 ) {
            if ( exists1 ) {
                // node1 exists but not node2
                name = node1.getName();
                deletedElements.put( name, node1 );
            } else {
                // neither exist so no difference!
            }
        } else {
            name = node2.getName();
            if ( exists1 ) {
                // both node1 and node2 exist
                name = node1.getName();
                updatedElements.put( name, node2 );
            } else {
                // node2 exists but not node1
                addedElements.put( name, node2 );
            }
        }
    }


    protected void addDiffs( Set<NodeRef> refs ) {
        for ( NodeRef ref : refs ) {
            EmsScriptNode nodeFromRef = new EmsScriptNode( ref, getServices() );
            String name = nodeFromRef.getName();
            NodeRef ref1 = NodeUtil.findNodeRefById( name, false, getWs1(),
                                                     getTimestamp1(), getServices(), true );
            EmsScriptNode node1 = ref1 == null ? null : new EmsScriptNode( ref1, getServices() );
            NodeRef ref2 = NodeUtil.findNodeRefById( name, false, getWs2(),
                                                     getTimestamp2(), getServices(), true );
            EmsScriptNode node2 = ref2 == null ? null : new EmsScriptNode( ref2, getServices() );
            addToDiff( node1, node2 );
        }
    }

    /**
     * Populate the WorkspaceDiff members based on the already constructed nodeDiff.
     */
    protected void populateMembers() {
        addedElements.clear();
        deletedElements.clear();
        updatedElements.clear();
        movedElements.clear();
        conflictedElements.clear();
        elements.clear();
        elementsVersions.clear(); // ??? REVIEW

        if ( nodeDiff == null ) {
            Debug.error("Trying WorkspaceDiff.populateMembers() when nodeDiff == null!");
            return;
        }

        Set< NodeRef > refs = nodeDiff.getAdded();
        addDiffs( refs );

        // Removed
        refs = nodeDiff.getRemoved();
        addDiffs( refs );

        // Updated
        refs = nodeDiff.getUpdated();
        addDiffs( refs );

        // Fix nested elements (value specifications, Expressions, ???)
        //nodeDiff.fixValueSpecifications(this);

        // Moved
        for ( Entry< String, EmsScriptNode > e : updatedElements.entrySet() ) {
            Map< String, Pair< Object, Object >> changes =
                    nodeDiff.getPropertyChanges( e.getKey() );
            if ( changes != null ) {
                Pair< Object, Object > ownerChange = changes.get( "ems:owner" );
                if ( ownerChange != null && ownerChange.first != null
                     && ownerChange.first != null
                     && !ownerChange.first.equals( ownerChange.second ) ) {
                    movedElements.put( e.getKey(), e.getValue() );
                }
            }
        }

        // Conflicted
        computeConflicted();

        // Elements
        Set< String > ids = new TreeSet< String >( nodeDiff.getMap1().keySet() );
        ids.addAll( nodeDiff.getMap2().keySet() );
        for ( String id : ids ) {
            NodeRef ref = NodeUtil.findNodeRefById( id, false, getWs1(), getTimestamp1(), null, true );
            if ( ref != null ) {
                EmsScriptNode node = new EmsScriptNode( ref, getServices() );
                if ( node.exists() ) {
                    EmsScriptNode parent = node;
                    String parentId = id;
                    while ( parent != null && parent.isModelElement() ) {
                        elements.put( parentId, parent );
                        parent = parent.getOwningParent( null );
                        parentId = parent.getName();
                    }
                }
            }
        }

        // TODO -- ElementVersions?????

    }

    /**
     * The intersection of the two workspace change sets are the potential
     * conflicts. The changes could still be the same in both workspaces, so we
     * need to check the existence of the nodes in the workspaces, and if both
     * exist, then the property changes must differ. If
     * conflictMustBeChangeToSameProperty == true, then properties changes must
     * be compared to the property values in the common parent to see whether
     * they are actually changed in each workspace.
     */
    protected void computeConflicted() {
        // Get intersection.
        Set< NodeRef > nodes = nodeDiff.get1();
        Set<String> intersection = NodeUtil.getNames( nodes );
        Set<String> names2 = NodeUtil.getNames( nodeDiff.get2() );
        boolean intersects = Utils.intersect( intersection, names2 );
        if ( intersects ) {
            for ( String name : intersection ) {
                // REVIEW -- TODO -- Should differences in aspects be recognized?
//                Set< String > aspects = nodeDiff.getAddedAspects( name );
//                Utils.minus( aspects, WorkspaceNode.workspaceMetaAspects );

                // Check to see if the existence of the nodes changed, in which
                // case, it is a definite conflict.
                NodeRef ref1 = NodeUtil.findNodeRefById( name, false, getWs1(),
                                                         getTimestamp1(),
                                                         getServices(),
                                                         false);
                NodeRef ref2 = NodeUtil.findNodeRefById( name, false, getWs2(),
                                                         getTimestamp2(),
                                                         getServices(),
                                                         false);
                boolean isConflict =
                        ( NodeUtil.exists( ref1 ) != NodeUtil.exists( ref2 ) );

                if ( !isConflict && conflictMustBeChangeToSameProperty ) {
                    isConflict = samePropertyChanged( ref1, ref2 );
                }

                if ( isConflict ) {
                    // This assumes that a pair of properties in changes are actually different.
                    EmsScriptNode node = new EmsScriptNode( nodeDiff.get2( name ),
                                                            getServices() );
                    conflictedElements.put( name, node );
                }
            }
        }
    }

    /**
     * Check to see if there is a property that was changed in both workspaces
     * for the given nodes. Need to check against the common parent.
     *
     * @param ref1
     *            a node in workspace 1
     * @param ref2
     *            a corresponding node in workspace 2
     * @return true iff there is a property for which these nodes were both
     *         changed in the respective workspaces to different values.
     */
    protected boolean samePropertyChanged( NodeRef ref1, NodeRef ref2 ) {
        String elementName = NodeUtil.getName( ref1 );
        Map< String, Pair< Object, Object > > changes =
                new LinkedHashMap< String, Pair<Object,Object> >( nodeDiff.getPropertyChanges( elementName ) );
        Utils.removeAll( changes, WorkspaceNode.workspaceMetaProperties );
        if ( Utils.isNullOrEmpty( changes ) ) return false;
        WorkspaceNode parentWs =
                WorkspaceNode.getCommonParent( getWs1(), getWs2() );
        NodeRef pRef1 = NodeUtil.findNodeRefById( elementName, false, parentWs,
                                                 getTimestamp1(),
                                                 getServices(),
                                                 false );
        NodeDiff nodeDiff1 = new NodeDiff( pRef1, ref1 );

        Map< String, Pair< Object, Object >> changes1 =
                nodeDiff1.getPropertyChanges( elementName );
        if ( Utils.isNullOrEmpty( changes1 ) ) return false;

        NodeRef pRef2 =
                NodeUtil.findNodeRefById( elementName, false, parentWs,
                                          getTimestamp2(),
                                          getServices(),
                                          false );
        NodeDiff nodeDiff2 = new NodeDiff( pRef2, ref2 );
        Map< String, Pair< Object, Object >> changes2 =
                nodeDiff2.getPropertyChanges( elementName );
        if ( Utils.isNullOrEmpty( changes2 ) ) return false;
        Set< String > propIds1 =
                new LinkedHashSet< String >( changes1.keySet() );
        Set< String > propIds2 = changes2.keySet();
        if ( !Utils.intersect( propIds1, propIds2 ) ) return false;
        for ( String propName : propIds1 ) {
            Pair< Object, Object > propVals =
                    changes.get( propName );
            if ( propVals.first == propVals.second
                 || ( propVals.first != null
                      && propVals.first.equals( propVals.second ) ) ) {
                return true;
            }
        }
        return false;
    }

    private ServiceRegistry getServices() {
        return NodeUtil.getServices();
    }

    private Date getTimestamp1() {
        return timestamp1;
    }

    private Date getTimestamp2() {
        return timestamp2;
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
        if ( ws1 != null && ws2 != null ) diff();
    }

    public void setWs2( WorkspaceNode ws2 ) {
        this.ws2 = ws2;
        if ( ws1 != null && ws2 != null ) diff();
    }

    public void setTimestamp1( Date timestamp1 ) {
        this.timestamp1 = timestamp1;
        if ( ws1 != null && ws2 != null ) diff();
    }

    public void setTimestamp2( Date timestamp2 ) {
        this.timestamp2 = timestamp2;
        if ( ws1 != null && ws2 != null ) diff();
    }
    /**
     * Dumps the JSON delta based.
     * @param   time1   Timestamp to dump ws1
     * @param   time2   Timestamp to dump ws2
     * @return  JSONObject of the delta
     * @throws JSONException
     */
    public JSONObject toJSONObject(Date time1, Date time2) throws JSONException {
            return toJSONObject( time1, time2, true );
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

        addJSONArray(ws1Json, "elements", elements, elementsVersions, time1, true);
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

    public static String toJsonName( Object name ) {
        QName qn = null;
        String possiblePrefixString = "" + name;
        if ( name instanceof QName ) {
            qn = (QName)name;
        } else {
            qn = NodeUtil.createQName( possiblePrefixString );
        }
        if ( qn != null ) {
            possiblePrefixString = qn.getPrefixString();
        }
        if ( !Acm.getJSON2ACM().containsKey( possiblePrefixString ) &&
             Acm.getACM2JSON().containsKey( possiblePrefixString ) ) {
            return Acm.getACM2JSON().get( possiblePrefixString );
        } else {
            if ( possiblePrefixString.startsWith( "sysml:" ) ) {
                possiblePrefixString = possiblePrefixString.substring( 6 );
            } else if ( possiblePrefixString.startsWith( "ems:" ) ) {
                possiblePrefixString = possiblePrefixString.substring( 4 );
            }// else possiblePrefixString = null;
        }
        return possiblePrefixString;
    }

    protected static Set<String> filter = null;
    protected static Set<String> getFilter() {
        if ( filter == null ) {
            filter = new LinkedHashSet<String>();
            filter.add("sysmlid");
            filter.add("id");
//            for ( QName qn : getIgnoredPropIdQNames() ) {
//                String jsonName = toJsonName( qn );
//                if ( jsonName != null ) {
//                    filter.remove( jsonName );
//                }
//            }
        }
        return filter;
    }


    private JSONArray convertMapToJSONArray(Map<String, EmsScriptNode> set, Map<String, Version> versions, Date dateTime, boolean showAll) throws JSONException {
        Set<String> filter = null;
        if (!showAll) {
            filter = getFilter();
//            filter = new HashSet<String>();
//            filter.add("id");
        }

        JSONArray array = new JSONArray();
        for (EmsScriptNode node: set.values()) {
            // allow the possibility that nodeDiff isn't being used to make the diff call
            if ( nodeDiff != null ) {
                Map< String, Pair< Object, Object > > propChanges =
                        nodeDiff.getPropertyChanges( node.getName() );
                if ( !showAll && propChanges != null && !propChanges.isEmpty() ) {
                    filter = new LinkedHashSet< String >(filter);
                    for ( Entry< String, Pair< Object, Object > > e : propChanges.entrySet() ) {
                        Pair< Object, Object > p = e.getValue();
                        if ( p != null && ( p.first != null || p.second != null ) ) {
                            String jsonName = toJsonName( e.getKey() );
                            if ( jsonName != null ) {
                                filter.add( jsonName );
                            }
                        }
                    }
                }
            }
            if ( versions == null || versions.size() <= 0 ) {
                array.put( node.toJSONObject( filter, dateTime ) );
            } else {
                JSONObject jsonObject = node.toJSONObject( filter, dateTime );
                Version version = versions.get( node.getName() );
                if ( version != null ) {
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

    protected static Set<String> ignoredPropIds = getIgnoredPropIds();
    protected static Set<QName> ignoredPropIdQnames = getIgnoredPropIdQNames();
    public static Set<String> getIgnoredPropIds() {
        if ( ignoredPropIds == null ) {
            DictionaryService ds = NodeUtil.getServices().getDictionaryService();
            //ds.getAllAspects();
            Collection< QName > properties = ds.getAllProperties( null );
            ignoredPropIds = Utils.newSet();
            for ( QName propName : properties ) {
                //PropertyDefinition propDef = ds.getProperty( propName );
                if ( propName == null ) continue;
                if ( propName.getPrefixString().startsWith( "sys:" ) ) {
                    ignoredPropIds.add( propName.toString() );
                }
                if ( propName.getPrefixString().startsWith( "ems:" ) ) {
                    ignoredPropIds.add( propName.toString() );
                }
            }
            List<String> prefixes = Utils.newList(
//                                                  "cm:name",
//                                                  "cm:content",
//                                                  "cm:modelName",
//                                                  "cm:modelDescription",
//                                                  "cm:modelAuthor",
//                                                  "cm:modelPublishedDate",
//                                                  "cm:modelVersion",
//                                                  "cm:modelActive",
//                                                  "cm:destination",
//                                                  "cm:userName",
//                                                  "cm:homeFolder",
//                                                  "cm:firstName",
//                                                  "cm:lastName",
//                                                  "cm:middleName",
//                                                  "cm:email",
//                                                  "cm:organizationId",
//                                                  "cm:homeFolderProvider",
//                                                  "cm:defaultHomeFolderPath",
//                                                  "cm:presenceProvider",
//                                                  "cm:presenceUsername",
//                                                  "cm:organization",
//                                                  "cm:jobtitle",
//                                                  "cm:location",
//                                                  "cm:persondescription",
//                                                  "cm:telephone",
//                                                  "cm:mobile",
//                                                  "cm:companyaddress1",
//                                                  "cm:companyaddress2",
//                                                  "cm:companyaddress3",
//                                                  "cm:companypostcode",
//                                                  "cm:companytelephone",
//                                                  "cm:companyfax",
//                                                  "cm:companyemail",
//                                                  "cm:skype",
//                                                  "cm:instantmsg",
//                                                  "cm:userStatus",
//                                                  "cm:userStatusTime",
//                                                  "cm:googleusername",
//                                                  "cm:emailFeedDisabled",
//                                                  "cm:subscriptionsPrivate",
//                                                  "cm:emailFeedId",
//                                                  "cm:sizeCurrent",
//                                                  "cm:sizeQuota",
//                                                  "cm:authorityName",
//                                                  "cm:authorityDisplayName",
//                                                  "cm:ratingScore",
//                                                  "cm:ratingScheme",
//                                                  "cm:ratedAt",
//                                                  "cm:failureCount",
//                                                  "cm:failedThumbnailTime",
//                                                  "cm:thumbnailName",
//                                                  "cm:contentPropertyName",
//                                                  "cm:title",
//                                                  "cm:description",
                                                  "cm:created",
                                                  "cm:creator",
                                                  "cm:modified",
                                                  "cm:modifier",
                                                  "cm:accessed",
//                                                  "cm:template",
//                                                  "cm:webscript",
//                                                  "cm:summaryWebscript",
//                                                  "cm:removeAfter",
//                                                  "cm:owner",
//                                                  "cm:author",
//                                                  "cm:publisher",
//                                                  "cm:contributor",
//                                                  "cm:type",
//                                                  "cm:identifier",
//                                                  "cm:dcsource",
//                                                  "cm:coverage",
//                                                  "cm:rights",
//                                                  "cm:subject",
//                                                  "cm:from",
//                                                  "cm:to",
//                                                  "cm:summary",
//                                                  "cm:hits",
//                                                  "cm:counter",
//                                                  "cm:workingCopyOwner",
//                                                  "cm:workingCopyMode",
//                                                  "cm:workingCopyLabel",
                                                  "cm:versionLabel",
                                                  "cm:versionType",
                                                  "cm:initialVersion",
                                                  "cm:autoVersion",
                                                  "cm:autoVersionOnUpdateProps",
                                                  "cm:lockOwner",
                                                  "cm:lockType",
                                                  "cm:lockLifetime",
                                                  "cm:expiryDate",
                                                  "cm:lockIsDeep",
//                                                  "cm:categories",
//                                                  "cm:taggable",
//                                                  "cm:tagScopeCache",
//                                                  "cm:tagScopeSummary",
//                                                  "cm:likesRatingSchemeCount",
//                                                  "cm:likesRatingSchemeTotal",
//                                                  "cm:fiveStarRatingSchemeCount",
//                                                  "cm:fiveStarRatingSchemeTotal",
//                                                  "cm:originator",
//                                                  "cm:addressee",
//                                                  "cm:addressees",
//                                                  "cm:subjectline",
//                                                  "cm:sentdate",
//                                                  "cm:noderef",
//                                                  "cm:storeName",
//                                                  "cm:preferenceValues",
//                                                  "cm:published",
//                                                  "cm:updated",
//                                                  "cm:latitude",
//                                                  "cm:longitude",
                                                  "cm:lastThumbnailModification",
                                                  "cm:isIndexed",
                                                  "cm:isContentIndexed"
//                                                  "cm:locale",
//                                                  "cm:automaticUpdate"
                                                  );
            for ( String p : prefixes ) {
                QName qn = NodeUtil.createQName( p );
                ignoredPropIds.add( qn.toString() );
            }
//            ignoredPropIds.addAll( prefixes );
        }
        return ignoredPropIds;
    }
    public static Set<QName> getIgnoredPropIdQNames() {
        if ( ignoredPropIdQnames == null ) {
            ignoredPropIdQnames = new LinkedHashSet<QName>();
            for ( String n : getIgnoredPropIds() ) {
                QName qn = NodeUtil.createQName( n );
                ignoredPropIdQnames.add( qn );
            }
        }
        return ignoredPropIdQnames;
    }

    protected void captureDeltas(WorkspaceNode node) {
        Set< NodeRef > newSet = Utils.newSet();
        Set<NodeRef> s1 = ( ws1 == null ? newSet : ws1.getChangedNodeRefsWithRespectTo( node, timestamp1 ) );
        Set<NodeRef> s2 = ( node == null ? newSet : node.getChangedNodeRefsWithRespectTo( ws1, timestamp2 ) );
        if ( onlyModelElements ) {
            s1 = NodeUtil.getModelElements(s1);
            s2 = NodeUtil.getModelElements(s2);
        }
        nodeDiff = new NodeDiff( s1, s2 );
        nodeDiff.addPropertyIdsToIgnore( getIgnoredPropIds() ); //Utils.newList( "read", "creator", "modified" ) );
        populateMembers();
    }

    public boolean ingestJSON(JSONObject json) {
        // TODO??
        return true;
    }

    public boolean isDiff() {
        if (addedElements.size() > 0 || deletedElements.size() > 0 || movedElements.size() > 0 || updatedElements.size() > 0) {
            return true;
        }
        return false;
    }
}
