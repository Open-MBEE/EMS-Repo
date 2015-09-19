package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff.DiffType;

import java.util.ArrayList;
import java.io.Serializable;
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
import java.util.HashSet;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

/**
 * Class for keeping track of differences between two workspaces. WS1 always has the original
 * elements, while WS2 always has just the deltas.
 * @author cinyoung
 *
 */
public class WorkspaceDiff implements Serializable {
    private static final long serialVersionUID = 2532475442685498671L;

    public static final String LATEST_NO_TIMESTAMP = "latest";

    public static boolean glomming = true;
    public boolean glom = glomming;

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
    public boolean conflictMustBeChangeToSameProperty = true;

    public boolean onlyModelElements = true;

    public boolean forceJsonCacheUpdate = true;

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
    
    private DiffType diffType = DiffType.MERGE;

    NodeDiff nodeDiff = null;
    
    private StringBuffer response = null;
    private Status status = null;

    public JSONObject diffJson;

    public JsonDiffDiff jsonDiffDiff = null;
    
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
    public WorkspaceDiff(WorkspaceNode ws1, WorkspaceNode ws2, StringBuffer response, Status status) {
        this();
        this.ws1 = ws1;
        this.ws2 = ws2;
        this.response = response;
        this.status = status;
        this.diffType = DiffType.MERGE;
    }

    public WorkspaceDiff(WorkspaceNode ws1, WorkspaceNode ws2, Date timestamp1, Date timestamp2,
                         StringBuffer response, Status status, DiffType diffType) {

        this(ws1, ws2, timestamp1, timestamp2, response, status, diffType, glomming);
    }
    
    public WorkspaceDiff(WorkspaceNode ws1, WorkspaceNode ws2, Date timestamp1, Date timestamp2,
                         StringBuffer response, Status status, DiffType diffType,
                         boolean glom) {

        this(ws1, ws2, response, status);
        this.timestamp1 = timestamp1;
        this.timestamp2 = timestamp2;
        this.diffType = diffType;
        this.glom = glom;
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
                name = node1.getSysmlId();
                deletedElements.put( name, node1 );
            } else {
                // neither exist so no difference!
            }
        } else {
            name = node2.getSysmlId();
            if ( exists1 ) {
                // both node1 and node2 exist
                name = node1.getSysmlId();
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
            String sysmlid = nodeFromRef.getSysmlId();
            NodeRef ref1 = NodeUtil.findNodeRefById( sysmlid, false, getWs1(),
                                                     getTimestamp1(), getServices(), true );
            EmsScriptNode node1 = ref1 == null ? null : new EmsScriptNode( ref1, getServices() );
            NodeRef ref2 = NodeUtil.findNodeRefById( sysmlid, false, getWs2(),
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
        Set< String > ids = new TreeSet< String >( );

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

        // Moved
        for ( Entry< String, EmsScriptNode > e : updatedElements.entrySet() ) {
            Map< String, Pair< Object, Object >> changes =
                    nodeDiff.getPropertyChanges( e.getKey() );
            if ( changes != null ) {
                Pair< Object, Object > ownerChange = changes.get( NodeUtil.createQName("ems:owner").toString() );
                if ( ownerChange != null && ownerChange.first != null
                     && ownerChange.second != null
                     && !ownerChange.first.equals( ownerChange.second ) ) {
                    EmsScriptNode node = e.getValue();
                    movedElements.put( e.getKey(), node);
                    
                    // Add this new owner to element ids, so it can be added to elements:
                    EmsScriptNode newOwner =
                            node != null ?
                            node.getOwningParent( getTimestamp1(), getWs1(), false ) :
                            null;
                    if (newOwner != null) {
                        ids.add( newOwner.getSysmlId() );
                    }
                    
                }
            }
        }

        // Conflicted
        computeConflicted();

        // Elements
        Set< NodeRef > removedUpdated = new HashSet< NodeRef >(nodeDiff.getRemoved());
        removedUpdated.addAll( nodeDiff.getUpdated() );
        // Add all of the removed and updated ids:
        for (NodeRef ref : removedUpdated) {
            if (ref != null) {
                EmsScriptNode node = new EmsScriptNode(ref, getServices());
                ids.add( node.getSysmlId() );
            }
        }
        // Add all of the parents of the added ids:
        for (NodeRef ref : nodeDiff.getAdded()) {
            if (ref != null) {
                EmsScriptNode node = new EmsScriptNode(ref, getServices());
                EmsScriptNode parent = node.getOwningParent( getTimestamp1(), getWs1(), false );
                if (parent != null) {
                    ids.add( parent.getSysmlId() );
                }
            }
        }
        for ( String id : ids ) {
            NodeRef ref = NodeUtil.findNodeRefById( id, false, getWs1(), getTimestamp1(), getServices(), true );
            if ( ref != null ) {
                EmsScriptNode node = new EmsScriptNode( ref, getServices() );
                if ( node.exists() ) {
                    EmsScriptNode parent = node;
                    String parentId = id;
                    while ( parent != null && parent.isModelElement() ) {
                        elements.put( parentId, parent );
                        parent = parent.getOwningParent( getTimestamp1(), getWs1(), false, true );
                        parentId = parent.getSysmlId();
                    }
                }
            }
        }

        // TODO -- ElementVersions?????

    }
    
    /**
     * TODO This has been copied from NodeDiff and slightly altered.  Should
     * re-factor to use common code b/t the two methods.
     * 
     * Remove ValueSpecification elements in diff and include the differences as
     * value property updates in the owning element.
     * <p>
     * If a ValueSpecification (which has many subclasses/subaspects) has
     * changed, and it is owned by a element, then the property of the
     * owning element has changed. In the JSON output, we should show the
     * changed value in the owning element (in its specialization) instead of as
     * a separate element.
     * <p>
     * To do this, this WorkspaceDiff is altered by
     * <ul>
     * <li>removing owned ValueSpecifications from the added, removed,
     * and updatedElements maps,
     * <li>adding the owning elements to these element maps if not
     * already there, and
     * <li>adding the properties to the nodeDiff.propertyChanges as well
     * as the added, removed, and updatedProperties maps of the nodeDiff.
     * </ul>
     * <p>
     * One tricky part is that an Expression may be owned by another Expression
     * that is owned by a Property.
     * <p>
     * Another tricky part is that an owning Property may be added or deleted,
     * in which case the ValueSpecification may need to be handled differently.
     *
     * @param workspaceDiff
     *            TODO
     */
    protected void fixValueSpecifications() {

        // Identify the elements that own changed ValueSpecifications and add
        // them to the updatedElements map.

        LinkedHashMap< EmsScriptNode, Pair<EmsScriptNode,Boolean > > valueSpecMap = new LinkedHashMap< EmsScriptNode, Pair<EmsScriptNode,Boolean >>();
        
        for ( EmsScriptNode node : addedElements.values() ) {
    
            if ( node.isOwnedValueSpec(timestamp2, ws2) ) {
                EmsScriptNode owningProp = node.getValueSpecOwner(timestamp2, ws2);
                if (owningProp != null) {
                // TODO -- REVIEW -- Does the if statement below need to be uncommented?
//                if ( !getRemoved().contains( owningProp ) ) {
                    valueSpecMap.put( node, new Pair< EmsScriptNode, Boolean >(owningProp,false) );
//                }
                }
            }
        }
        for ( EmsScriptNode node : updatedElements.values()) {
            if ( node.isOwnedValueSpec(timestamp2, ws2) ) { 
                EmsScriptNode owningProp = node.getValueSpecOwner(timestamp2, ws2);
                if (owningProp != null) {
                    valueSpecMap.put( node, new Pair< EmsScriptNode, Boolean >(owningProp,false) );
                }
            }
        }
        for ( EmsScriptNode node : deletedElements.values() ) {
            // Note: Removed nodes are in ws1 and timestamp1 and not in ws2 and timestamp2 by definition:
            if ( node.isOwnedValueSpec(timestamp1, ws1) ) {
                EmsScriptNode owningProp = node.getValueSpecOwner(timestamp1, ws1);
                if (owningProp != null) {
                    valueSpecMap.put( node, new Pair< EmsScriptNode, Boolean >(owningProp,true) );
                }
            }
        }

        // adding the owning Property elements to these element maps if not already there
        for ( Pair< EmsScriptNode, Boolean > pair : valueSpecMap.values() ) {
            EmsScriptNode node = pair.first;
            if ( !addedElements.containsValue( node ) ) {
                if ( !deletedElements.containsValue( node ) ) {
                    if ( !updatedElements.containsValue( node ) ) {
                        updatedElements.put( node.getSysmlId(), node );
                    }
                }
            }
        }

        // Remove the owned ValueSpecifications from everything.
        for ( EmsScriptNode node : valueSpecMap.keySet() ) {
            addedElements.remove( node.getSysmlId() );
            updatedElements.remove( node.getSysmlId() );
            deletedElements.remove( node.getSysmlId() );
        }

    }
    
    protected void populateMembersSkeleton(Set<NodeRef> allChangedNodes) {
        addedElements.clear();
        deletedElements.clear();
        updatedElements.clear();
        movedElements.clear();
        conflictedElements.clear();
        elements.clear();
        elementsVersions.clear(); // ??? REVIEW
        Set< String > ids = new TreeSet< String >( );

        // Compute the diff:
        addDiffs( allChangedNodes );

        // TODO may not be worth the performance hit to call fixValueSpecifications(), but if 
        // we dont the front end would have to filter out the value specs
        fixValueSpecifications();
        
        // TODO calculating moved/conflicted requires looking at node properties,
        //      so not doing it.  Doris is fine with this for now.
//        // Moved
//        for ( Entry< String, EmsScriptNode > e : updatedElements.entrySet() ) {
//            Map< String, Pair< Object, Object >> changes =
//                    nodeDiff.getPropertyChanges( e.getKey() );
//            if ( changes != null ) {
//                Pair< Object, Object > ownerChange = changes.get( NodeUtil.createQName("ems:owner").toString() );
//                if ( ownerChange != null && ownerChange.first != null
//                     && ownerChange.second != null
//                     && !ownerChange.first.equals( ownerChange.second ) ) {
//                    EmsScriptNode node = e.getValue();
//                    movedElements.put( e.getKey(), node);
//                    
//                    // Add this new owner to element ids, so it can be added to elements:
//                    EmsScriptNode newOwner =
//                            node != null ?
//                            node.getOwningParent( getTimestamp1(), getWs1(), false ) :
//                            null;
//                    if (newOwner != null) {
//                        ids.add( newOwner.getSysmlId() );
//                    }
//                    
//                }
//            }
//        }
//
//        // Conflicted
//        computeConflicted();

        // Elements
        Set< String > removedUpdated = new HashSet< String >(deletedElements.keySet());
        removedUpdated.addAll( updatedElements.keySet() );
        // Add all of the removed and updated ids:
        ids.addAll( removedUpdated );

        // Add all of the parents of the added ids:
        for (EmsScriptNode node : addedElements.values()) {
            if (node != null) {
                EmsScriptNode parent = node.getOwningParent( getTimestamp1(), getWs1(), false );
                if (parent != null) {
                    ids.add( parent.getSysmlId() );
                }
            }
        }
        for ( String id : ids ) {
            NodeRef ref = NodeUtil.findNodeRefById( id, false, getWs1(), getTimestamp1(), getServices(), true );
            if ( ref != null ) {
                EmsScriptNode node = new EmsScriptNode( ref, getServices() );
                if ( node.exists() ) {
                    EmsScriptNode parent = node;
                    String parentId = id;
                    while ( parent != null && parent.isModelElement() ) {
                        elements.put( parentId, parent );
                        parent = parent.getOwningParent( getTimestamp1(), getWs1(), false, true );
                        parentId = parent.getSysmlId();
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
     * 
     * On 1/16/15 we defined conflict as the following:
     * Given a branch Bi, creation time of that branch off the common parent PCi, 
     * we define a conflict as a change to a element for all Bi after min(PCi), where
     * a change to a element on a branch includes all parent branch changes also.
     * If one of the branches is the common parent, then it does not have a PCi,
     * and if all branches are the same, then a conflict is not possible.
     */
    protected void computeConflicted() {
        // Get intersection.
        Set< NodeRef > nodes = nodeDiff.get1();
        Set<String> intersection = NodeUtil.getSysmlIds( nodes );
        Set<String> names2 = NodeUtil.getSysmlIds( nodeDiff.get2() );
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
     * Returns the child of the commonParent that is a parent of workspace.
     * 
     * @param workspace
     * @param commonParent
     * @return
     */
    private WorkspaceNode getChildOfCommonParent(WorkspaceNode workspace,
                                                 WorkspaceNode commonParent) {
        
        WorkspaceNode parent = workspace;
        WorkspaceNode parentLast = workspace;
        while (parent != null && !parent.equals( commonParent )) {
            if (parent != null) parentLast = parent;
            parent = parent.getParentWorkspace();
        }
        
        return parentLast;
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
        
        // Note: assuming that any node passed to this method, or part of the nodeDiff
        //       already are contained within the desired timestamps
        
        String elementName = NodeUtil.getSysmlId( ref1 );
        Map< String, Pair< Object, Object >> propChanges = nodeDiff.getPropertyChanges( elementName );
        if ( propChanges == null ) return false;
        Map< String, Pair< Object, Object > > changes =
                new LinkedHashMap< String, Pair<Object,Object> >( propChanges );
        Utils.removeAll( changes, WorkspaceNode.workspaceMetaProperties );
        if ( Utils.isNullOrEmpty( changes ) ) return false;
        
        WorkspaceNode ws1 = getWs1();
        WorkspaceNode ws2 = getWs2();

        WorkspaceNode parentWs =
                WorkspaceNode.getCommonParent( ws1, ws2 );
        
        boolean ws1Equal = NodeUtil.workspacesEqual(ws1,parentWs);
        boolean ws2Equal = NodeUtil.workspacesEqual(ws2,parentWs);
        
        // If both workspaces are the same (possible b/c of different timepoints) then 
        // a conflict is not possible:
        if (NodeUtil.workspacesEqual(ws1,ws2)) {
            return false;
        }
        // If either of the workspaces are the common parent, then just need to verify that
        // node was modified in the common parent after the other workspaces parent off the 
        // common parent was created:
        else if (ws1Equal || ws2Equal) {
            
            WorkspaceNode otherWs = ws1Equal ? ws2 : ws1;
            NodeRef thisRef = ws1Equal ? ref1 : ref2;
            EmsScriptNode n = new EmsScriptNode(thisRef, getServices());
            Date lastModified = n.getLastModified(null);
            
            // Go up the parent tree of the other workspace until you find the workspace
            // that is branched off the common parent:
            WorkspaceNode otherWsParent = getChildOfCommonParent(otherWs, parentWs);
            Date creationDate = otherWsParent != null ? otherWsParent.getCopyOrCreationTime() : null;
                    
            return lastModified != null && creationDate != null && lastModified.after( creationDate );
        }
        // Neither workspace is the common parent, so must verify each node was modified
        // after minimum of the branch time off the common parent:
        else {
            
            EmsScriptNode n1 = new EmsScriptNode(ref1, getServices());
            EmsScriptNode n2 = new EmsScriptNode(ref2, getServices());
            Date lastModified1 = n1.getLastModified(null);
            Date lastModified2 = n2.getLastModified(null);
            
            // Go up the parent tree of the workspaces until you find the workspace
            // that is branched off the common parent:
            WorkspaceNode wsParent1 = getChildOfCommonParent(ws1, parentWs);
            WorkspaceNode wsParent2 = getChildOfCommonParent(ws2, parentWs);

            Date creationDate1 = wsParent1 != null ? wsParent1.getCopyOrCreationTime() : null;
            Date creationDate2 = wsParent2 != null ? wsParent2.getCopyOrCreationTime(): null;
            
            // Find the minimum of the creation time:
            if (creationDate1 != null && creationDate2 != null && lastModified1 != null &&
                lastModified2 != null) {
                Date minCreationDate = creationDate1.before( creationDate2 ) ? creationDate1 : creationDate2;
            
                return lastModified1.after(minCreationDate) && lastModified2.after(minCreationDate);
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
    
    public static Set< NodeRef > getAllChangedElementsInDiffJson( JSONArray diffJson,
                                                                  WorkspaceNode ws,
                                                                  ServiceRegistry services,
                                                                  Date dateTime)
                                                                                throws JSONException {
                
        Set< NodeRef > nodes = new LinkedHashSet< NodeRef >();
        for ( int i = 0; i < diffJson.length(); ++i ) {
            JSONObject element = diffJson.getJSONObject( i );
            if ( element.has( "sysmlid" ) ) {
                String sysmlid = element.getString( "sysmlid" );
                NodeRef ref = NodeUtil.findNodeRefById( sysmlid, false, ws, dateTime,
                                                        services, true );
                if ( ref != null ) {
                    nodes.add( ref );                    
                }  
            }
        }
        return nodes;
    }

    public static Set< NodeRef > getAllChangedElementsInDiffJson( JSONObject diffJson,
                                                                        ServiceRegistry services,
                                                                  Date dateTime)
                                                                                throws JSONException {
        LinkedHashSet< NodeRef > nodes = new LinkedHashSet< NodeRef >();
        JSONObject jsonObj = diffJson;
        JSONArray jsonArr = null;
        
        WorkspaceNode ws = null;
        
        if ( diffJson.has( "workspace2" ) ) {
            jsonObj = jsonObj.getJSONObject( "workspace2" );
            if ( jsonObj.has( "id" ) ) {
                String name = jsonObj.getString( "id" );
                ws = WorkspaceNode.getWorkspaceFromId( name, services,
                                                               null, null, //false
                                                               null );
            }
        }
        Set< NodeRef > elements = null;
        if ( jsonObj.has( "addedElements" ) ) {
            jsonArr = jsonObj.getJSONArray( "addedElements" );
            elements = getAllChangedElementsInDiffJson( jsonArr, ws, services, dateTime );
            if ( elements != null ) nodes.addAll( elements );
        }
        if ( jsonObj.has( "deletedElements" ) ) {
            jsonArr = jsonObj.getJSONArray( "deletedElements" );
            elements = getAllChangedElementsInDiffJson( jsonArr, ws, services, dateTime );
            if ( elements != null ) nodes.addAll( elements );
        }
        if ( jsonObj.has( "updatedElements" ) ) {
            jsonArr = jsonObj.getJSONArray( "updatedElements" );
            elements = getAllChangedElementsInDiffJson( jsonArr, ws, services, dateTime );
            if ( elements != null ) nodes.addAll( elements );
        }
        return nodes;
    }


    /**
     * Dumps the JSON delta based.
     * @param   time1   Timestamp to dump ws1
     * @param   time2   Timestamp to dump ws2
     * @return  JSONObject of the delta
     * @throws JSONException
     */
    public JSONObject toJSONObject(Date time1, Date time2) throws JSONException {
        JSONObject deltaJson = toJSONObject( time1, time2, true );
        // If we came up with nothing (!isDiff()), then maybe we computed it
        // another way and should return the existing diffJson.
        if ( diffJson == null || isDiff( deltaJson ) ) {
            diffJson = deltaJson;
        }
        
        return diffJson;
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
        JSONObject deltaJson = NodeUtil.newJsonObject();
        JSONObject ws1Json = NodeUtil.newJsonObject();
        JSONObject ws2Json = NodeUtil.newJsonObject();

        addJSONArray(ws1Json, "elements", elements, elementsVersions, ws1, time1, true);
        addWorkspaceMetadata( ws1Json, ws1, time1 );

        addJSONArray(ws2Json, "addedElements", addedElements, ws2, time2, true);
        addJSONArray(ws2Json, "movedElements", movedElements, ws2, time2, showAll);
        // Note: deleteElements should use time1 and not time2, and ws1 not ws2, 
        //       as element was found in ws1 at time1, not ws2 at time2!
        addJSONArray(ws2Json, "deletedElements", deletedElements, ws1, time1, showAll);
        addJSONArray(ws2Json, "updatedElements", updatedElements, ws2, time2, showAll);
        addJSONArray(ws2Json, "conflictedElements", conflictedElements, ws2, time2, showAll);
        addWorkspaceMetadata( ws2Json, ws2, time2);

        deltaJson.put( "workspace1", ws1Json );
        deltaJson.put( "workspace2", ws2Json );

        // If we came up with nothing (!isDiff()), then maybe we computed it
        // another way and should return the existing diffJson.
        if ( diffJson == null || isDiff() ) {
            diffJson = deltaJson;
        }
        
        return diffJson;

    }
    
    public static boolean isDiff( JSONObject diff ) {
        JsonDiffDiff diffDiff = new JsonDiffDiff( diff );
        return !diffDiff.getAffectedIds().isEmpty();
    }
    
    /**
     * Add the workspace metadata onto the provided JSONObject
     * @param jsonObject
     * @param ws
     * @param dateTime
     * @throws JSONException
     */
    private void addWorkspaceMetadata(JSONObject jsonObject, WorkspaceNode ws, Date dateTime) throws JSONException {
        WorkspaceNode.addWorkspaceNamesAndIds( jsonObject, ws, false );
        if (dateTime != null) {
            jsonObject.put( "timestamp", TimeUtils.toTimestamp( dateTime ) );
        }
    }

    private boolean addJSONArray(JSONObject jsonObject, String key, Map< String, EmsScriptNode > map,
                                 WorkspaceNode ws, Date dateTime, boolean showAll) throws JSONException {
            return addJSONArray(jsonObject, key, map, null, ws, dateTime, showAll);
    }

    private boolean addJSONArray(JSONObject jsonObject, String key, Map< String, EmsScriptNode > map, 
                                 Map< String, Version> versions, WorkspaceNode ws, Date dateTime, boolean showAll) throws JSONException {
        return addJSONArray( jsonObject, key, map, versions, ws, dateTime, showAll, nodeDiff );
    }
    public static boolean addJSONArray(JSONObject jsonObject, String key, Map< String, EmsScriptNode > map, 
                                        Map< String, Version> versions, WorkspaceNode ws, Date dateTime, boolean showAll,
                                        NodeDiff nodeDiff ) throws JSONException {
        boolean emptyArray = true;
        if (map != null && map.size() > 0) {
            jsonObject.put( key, convertMapToJSONArray( map, versions, ws, dateTime, showAll, nodeDiff ) );
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
            if ( possiblePrefixString.startsWith( "sysml:" ) && possiblePrefixString.length() >= 6) {
                possiblePrefixString = possiblePrefixString.substring( 6 );
            } else if ( possiblePrefixString.startsWith( "ems:" ) && possiblePrefixString.length() >= 4 ) {
                possiblePrefixString = possiblePrefixString.substring( 4 );
            } else if ( possiblePrefixString.startsWith( ":" ) && possiblePrefixString.length() >= 1 ) {
                possiblePrefixString = possiblePrefixString.substring( 1 );
            }
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

    protected JSONArray convertMapToJSONArray(Map<String, EmsScriptNode> map,
                                              Map<String, Version> versions, 
                                              WorkspaceNode workspace, Date dateTime,
                                              boolean showAll) throws JSONException {
        return convertMapToJSONArray( map, versions, workspace, dateTime, showAll, nodeDiff );
    }

    protected static JSONArray convertMapToJSONArray(Map<String, EmsScriptNode> map,
                                                     Map<String, Version> versions, 
                                                     WorkspaceNode workspace, Date dateTime,
                                                     boolean showAll, NodeDiff nodeDiff) throws JSONException {
        Set<String> filter = null;
        if (!showAll) {
            filter = getFilter();
//            filter = new HashSet<String>();
//            filter.add("id");
        }

        JSONArray array = new JSONArray();
        for (EmsScriptNode node: map.values()) {
            // Make sure the element exists at the dateTime.
            if ( node != null ) {
                NodeRef r = NodeUtil.getNodeRefAtTime( node.getNodeRef(),
                                                       workspace, dateTime );
                if ( r == null ) node = null;
                //else node = new EmsScriptNode( r, getServices() ); 
            }
            if ( node == null ) {
                continue;
            }
            // allow the possibility that nodeDiff isn't being used to make the diff call
            if ( nodeDiff != null ) {
                Map< String, Pair< Object, Object > > propChanges =
                        nodeDiff.getPropertyChanges( node.getSysmlId() );
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
            // This is for the skeleton diff.  Want these additional properties:
            else if (filter != null) {
                filter.add(Acm.SYSMLID);
                filter.add("qualifiedId");
                filter.add("qualifiedName");
                filter.add(Acm.JSON_OWNER);
                filter.add(Acm.JSON_NAME);
                filter.add(Acm.JSON_TYPE);
            }
            boolean includeQualified = true;
            Version version = null;
            if ( !Utils.isNullOrEmpty( versions ) ) {
                version = versions.get( node.getSysmlId() );
//                filter.add( "id" );
//                filter.add( "version" );
            }
            JSONObject jsonObject =
                    node.toJSONObject( filter, false, workspace, dateTime,
                                       includeQualified, version, null );
            array.put( jsonObject );
        }

        return array;
    }

    public boolean diff() {
        boolean status = true;

        if ( glom ) {
            jsonDiffDiff  = captureDeltasFromCommits();
            diffJson = jsonDiffDiff.toJsonObject();
        } else {
            captureDeltas();
        }
        //captureDeltasSkeleton();

        return status;
    }

    protected static Set<String> ignoredPropIds = getIgnoredPropIds();
    protected static Set<QName> ignoredPropIdQnames = getIgnoredPropIdQNames();

    public static boolean noFind = true;
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
            
            // Dont want to ignore the owner for moved elements:
            ignoredPropIds.remove( NodeUtil.createQName( "ems:owner" ).toString());

            List<String> prefixes = Utils.newList( "sysml:id",
                                                   "view2:snapshotProduct",
                                                   "view2:productSnapshots",
                                                   "view2:docbookNode",
                                                   "view2:pdfNode",
                                                   "view2:htmlZipNode",
                                                   "view2:timestamp",
                                                   "cm:name",
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
    
    protected void captureDeltasSkeleton() {
        
        // TODO
        // Embedded value specs are not being filtered correctly.  Should the server
        // do this for the skeleton diff or can the front end?  Doris said the front
        // end can filter out all value specs from the diff b/c currently magic draw
        // does not have stand alone value specs; however, having the front end do this
        // doesn't seem wise if this assumption ever changes.  That being said, if
        // server does this filtering, it will make the skeleton diff less efficient.
        // The server filtering has been implemented in fixValueSpecifications(), which
        // is a lot of the same code as NodeDiff version.
                
        // Note:
        // The commit nodes only have the sysmlids of the nodes, so will still need
        // to get a set of nodes for the other properties that will be displayed
        // in the skeleton diff, ie name, owner, qualifiedId, type, etc        
        
        Set< NodeRef > s1 =
                WorkspaceNode.getChangedNodeRefsWithRespectTo( ws1, ws2,
                                                               timestamp1,
                                                               timestamp2,
                                                               getServices(),
                                                               response, status );
        Set< NodeRef > s2 =
                WorkspaceNode.getChangedNodeRefsWithRespectTo( ws2, ws1,
                                                               timestamp2,
                                                               timestamp1,
                                                               getServices(),
                                                               response, status );
        
        // If either of these are null then we caught an exception above, 
        // so just bail
        if (s1 == null || s2 == null) {
            return;
        }
        
        if ( onlyModelElements ) {
            s1 = NodeUtil.getModelElements(s1);
            s2 = NodeUtil.getModelElements(s2);
        }
        
        // create lists of deleted in s1 and deleted in s2 
        List< NodeRef > deletedFromS1 = new ArrayList< NodeRef >();
        List< NodeRef > deletedFromS2 = new ArrayList< NodeRef >();
        
        // need to make sure both sets have each others' nodes
        for ( NodeRef n : s1 ) {
            String sysmlId = NodeUtil.getSysmlId( n );
            NodeRef ref =
                    NodeUtil.findNodeRefById( sysmlId, false, ws2, timestamp2,
                                              getServices(), false );
            if ( ref != null ) {
                s2.add( ref );
            }
            if ( NodeUtil.isDeleted(n)) {
                deletedFromS1.add(n);
            }
        }
        for ( NodeRef n : s2 ) {
            String sysmlId = NodeUtil.getSysmlId( n );
            NodeRef ref =
                    NodeUtil.findNodeRefById( sysmlId, false, ws1, timestamp1,
                                              getServices(), false );
            if ( ref != null ) {
                s1.add( ref );
            }
            if ( NodeUtil.isDeleted(n)) {
                deletedFromS2.add(n);
            }
        }
                
        // Remove the deleted nodes from s1 and s2  
        for ( NodeRef n : deletedFromS1 ) {
            s1.remove(n);
        }
        for ( NodeRef n : deletedFromS2 ) {
            s2.remove(n);
        }
        
        // Create a union of the two sets for the diff skeleton:
        Set<NodeRef> allChangedNodes = new HashSet<NodeRef>(s1);
        allChangedNodes.addAll( s2 );
        
        populateMembersSkeleton(allChangedNodes);
    }

    protected JsonDiffDiff captureDeltasFromCommits() {
        JSONObject commitDiff1 =
                WorkspaceNode.getChangeJsonWithRespectTo( ws1, ws2,
                                                          timestamp1,
                                                          timestamp2,
                                                          getServices(),
                                                          response, status );
        JSONObject commitDiff2 =
                WorkspaceNode.getChangeJsonWithRespectTo( ws2, ws1,
                                                          timestamp2,
                                                          timestamp1,
                                                          getServices(),
                                                          response, status );
        
        Pair< WorkspaceNode, Date > p =
                getCommonBranchPoint( ws1, ws2, timestamp1, timestamp2 );
        WorkspaceNode commonParent = p.first;
        Date commonBranchTime = p.second;

        return performDiffGlom( null, commitDiff1, commitDiff2, commonParent,
                                commonBranchTime, getServices(), response, diffType );
    }
    
    protected void captureDeltas() {
        Set< NodeRef > s1 =
                WorkspaceNode.getChangedNodeRefsWithRespectTo( ws1, ws2,
                                                               timestamp1,
                                                               timestamp2,
                                                               getServices(),
                                                               response, status );
        Set< NodeRef > s2 =
                WorkspaceNode.getChangedNodeRefsWithRespectTo( ws2, ws1,
                                                               timestamp2,
                                                               timestamp1,
                                                               getServices(),
                                                               response, status );
        
        // If either of these are null then we caught an exception above, 
        // so just bail
        if (s1 == null || s2 == null) {
            return;
        }
        
        if ( onlyModelElements ) {
            s1 = NodeUtil.getModelElements(s1);
            s2 = NodeUtil.getModelElements(s2);
        }
        
        // create lists of deleted in s1 and deleted in s2 
        List< NodeRef > deletedFromS1 = new ArrayList< NodeRef >();
        List< NodeRef > deletedFromS2 = new ArrayList< NodeRef >();
        
        // need to make sure both sets have each others' nodes
        for ( NodeRef n : s1 ) {
            String sysmlId = NodeUtil.getSysmlId( n );
            NodeRef ref =
                    NodeUtil.findNodeRefById( sysmlId, false, ws2, timestamp2,
                                              getServices(), false );
            if ( ref != null ) {
                s2.add( ref );
            }
            if ( NodeUtil.isDeleted(n)) {
            	deletedFromS1.add(n);
            }
        }
        for ( NodeRef n : s2 ) {
            String sysmlId = NodeUtil.getSysmlId( n );
            NodeRef ref =
                    NodeUtil.findNodeRefById( sysmlId, false, ws1, timestamp1,
                                              getServices(), false );
            if ( ref != null ) {
                s1.add( ref );
            }
            if ( NodeUtil.isDeleted(n)) {
            	deletedFromS2.add(n);
            }
        }
                
        // Remove the deleted nodes from s1 and s2  
        for ( NodeRef n : deletedFromS1 ) {
        	s1.remove(n);
        }
        for ( NodeRef n : deletedFromS2 ) {
        	s2.remove(n);
        }
        
        // Add owned value specs if needed, as we need them to be separated out for
        // the diff to work correctly:
        Set<NodeRef> tempSet1 = new TreeSet< NodeRef >(NodeUtil.nodeRefComparator);
        Set<NodeRef> tempSet2 = new TreeSet< NodeRef >(NodeUtil.nodeRefComparator);
        tempSet1.addAll( s1 );
        tempSet2.addAll( s2 );
        for ( NodeRef n : tempSet1 ) {
            String sysmlId = NodeUtil.getSysmlId( n );
            NodeRef ref =
                    NodeUtil.findNodeRefById( sysmlId, false, ws1, timestamp1,
                                              getServices(), false );
            if ( ref != null ) {                
                // Add owned value specs if needed, as we need them to be separated out for
                // the diff to work correctly.  
                NodeUtil.addEmbeddedValueSpecs(ref, s1, getServices(), timestamp1, ws1);
            }
        }
        for ( NodeRef n : tempSet2 ) {
            String sysmlId = NodeUtil.getSysmlId( n );
            NodeRef ref =
                    NodeUtil.findNodeRefById( sysmlId, false, ws2, timestamp2,
                                              getServices(), false );
            if ( ref != null ) {                
                // Add owned value specs if needed, as we need them to be separated out for
                // the diff to work correctly.  
                NodeUtil.addEmbeddedValueSpecs(ref, s2, getServices(), timestamp2, ws2);
            }
        }

        nodeDiff = new NodeDiff( s1, s2, timestamp1, timestamp2, ws1, ws2 );
        nodeDiff.addPropertyIdsToIgnore( getIgnoredPropIds() );
        populateMembers();
    }

    public boolean ingestJSON(JSONObject json) {
        Debug.error("ingestJson() not implemented!");
        // TODO??
        return true;
    }

    public boolean isDiff() {
        if (addedElements.size() > 0 || deletedElements.size() > 0 || movedElements.size() > 0 || updatedElements.size() > 0) {
            return true;
        }
        return false;
    }
    
    
//    public String toSimpleString() {
//        JSONObject top = new JSONObject();
//        JSONObject ws1 = new JSONObject();
//        JSONObject ws2 = new JSONObject();
//        
//        ws1.put( "workspace", arg1 )
//        
//        for (int ii = 0; ii < addedElements.size(); ii++) {
//            
//        }
//        
//        return top.toString();
//    }
    
    /**
     * Compute a new diff based on an old diff0 with changes to workspace1 
     * @param diff0
     * @param diff1
     * @param diff2
     * @param commonParent
     * @param commonBranchTime
     * @param services
     * @param response
     * @return
     */
    public static JsonDiffDiff performDiffGlom( JSONObject diff0,
                                              JSONObject diff1,
                                              JSONObject diff2,
                                              WorkspaceNode commonParent,
                                              Date commonBranchTime,
                                              ServiceRegistry services,
                                              StringBuffer response,
                                              DiffType diffType) {
        // If diff0 is null, we need to build a diff0 from scratch. Collect all
        // element ids in diff1 and diff2, get their json for the common-branch
        // timepoint, and put that into workspace1.elements of a diff0.
        if ( diff0 == null ) {
            diff0 = JsonDiffDiff.makeEmptyDiffJson();
            
            JsonDiffDiff diffDiff1 = new JsonDiffDiff(diff1);
            JsonDiffDiff diffDiff2 = new JsonDiffDiff(diff2);
            
            Set<EmsScriptNode> elements = Utils.newSet();
            
            if ( noFind  ) {
                // Get the previous values of the changed nodes from the
                // workspace1 elements of each diff
                JSONArray diff1Elements = null;
                JSONObject diff1_ws1 = diff1.optJSONObject( "workspace1" );
                if ( diff1_ws1 != null ) {
                    diff1Elements = diff1_ws1.optJSONArray("elements");
                }
                
                JSONArray diff2Elements = null;
                JSONObject diff2_ws1 = diff2.optJSONObject( "workspace1" );
                if ( diff2_ws1 != null ) {
                    diff2Elements = diff2_ws1.optJSONArray("elements");
                }

                JSONObject diff0_ws1 = diff0.getJSONObject( "workspace1" );
                JSONArray diff0Elements = diff0_ws1.getJSONArray( "elements" );

                // Add only the elements from diff2 that are not in diff1 to the
                // elements of diff0.
                if ( diff2Elements != null ) {
                    for ( int i = 0; i < diff2Elements.length(); ++i ) {
                        JSONObject element2_1 = diff2Elements.optJSONObject( i );
                        if ( element2_1 != null ) {
                            String sysmlId = element2_1.optString( "sysmlid" );
                            if ( sysmlId != null && !diffDiff1.diffMap1.containsKey( sysmlId ) ) {
                                diff0Elements.put( element2_1 );
                            }
                        }
                    }
                }
                // Add the elements from diff1 to the elements of diff0.
                if ( diff1Elements != null ) {
                    for ( int i = 0; i < diff1Elements.length(); ++i ) {
                        JSONObject element1_1 = diff1Elements.optJSONObject( i );
                        if ( element1_1 != null ) {
                            diff0Elements.put( element1_1 );
                        }
                    }
                }
            } else {
            
            Set<String> sysmlIds = diffDiff1.getAffectedIds();
            sysmlIds.addAll(diffDiff2.getAffectedIds());

            for (String id : sysmlIds)
            {
                //create ArrayList of node refs by calling getNodeRefsById
                //add to set of EmsScriptNodes
                EmsScriptNode node =
                        NodeUtil.findScriptNodeById( id, commonParent,
                                                     commonBranchTime, false,
                                                     services, response, null );
                if ( node != null ) {
                    elements.add( node );
                }
            }
            Map<String, EmsScriptNode> elementsMap = Utils.toMap(elements);
           
            JSONObject elementsJson = diff0.getJSONObject( "workspace1" );
            addJSONArray( elementsJson , "elements", elementsMap, null, commonParent,
                          commonBranchTime, true, null );
            }
        }
        
        
        // Now add/glom diff2 to diff0 (oldDiffJson) and then diff with/subtract
        // diff1.
        JsonDiffDiff diffResult = null; //glom( oldDiffJson, diff2Json );
        diffResult = JsonDiffDiff.diff( diff0, diff1, diff2, diffType );
    
        return diffResult;
    }

    /**
     * utility for traversing a WsDiff JSON file that leaves only sysml ids with versioned
     * node information as well as ids for workspaces
     * @param json
     * @return
     */
    public static JSONObject cleanWorkspaceJson(JSONObject json) {
        JSONObject result = new JSONObject();
        
        if (json.has( "workspace1" )) {
            JSONObject ws1 = json.getJSONObject( "workspace1" );
            JSONObject resultWs1 = new JSONObject();
            resultWs1.put( "elements", cleanElementsJson(ws1, "elements") );
            resultWs1.put( "id",  ws1.get( "id" ));
            result.put( "workspace1", resultWs1 );
        }
        
        if (json.has( "workspace2" )) {
            JSONObject ws2 = json.getJSONObject( "workspace2" );
            JSONObject resultWs2 = new JSONObject();
            String keys[] = {"addedElements", "movedElements", "deletedElements", "updatedElements", "conflictedElements"};
            for (String key: keys) {
                resultWs2.put( key, cleanElementsJson(ws2, key) );
            }
            resultWs2.put( "id",  ws2.get( "id" ));
            result.put( "workspace2", resultWs2 );
        }
        
        return result;
    }
    
    private static JSONArray cleanElementsJson(JSONObject json, String key) {
        JSONArray elements = json.getJSONArray( key );
        JSONArray results = new JSONArray();
        for (int ii = 0; ii < elements.length(); ii++) {
            results.put( cleanElementJson(elements.getJSONObject( ii )) );
        }
        return results;
    }
    
    private static JSONObject cleanElementJson(JSONObject json) {
        JSONObject result = new JSONObject();
        String keys[] = {"sysmlid", "id", "version", "qualifiedId", "qualifiedName", "name"};
        for (String key: keys) {
            if (json.has( key )) result.put( key, json.get( key ) );
        }
        return result;  
    }

    public static Date dateFromWorkspaceTimestamp( String timestamp ) {
        Date timepoint = ( ( timestamp == null || 
                             timestamp.equals( LATEST_NO_TIMESTAMP ) ) ?
                           null : TimeUtils.dateFromTimestamp( timestamp ) );
        return timepoint;
    }
    
    public static Date earlierWorkspaceDate( Date d1, Date d2 ) {
        d1 = ( d1 == null ? d2 : ( d2 == null || d1.before( d2 ) ? d1 : d2 ) );
        return d1;
    }

    public static Date earlierWorkspaceDate( String timestamp1,
                                             String timestamp2 ) {
        // checking for null to avoid converting a timestamp unnecessarily
        if ( timestamp1 == null ) {
            return dateFromWorkspaceTimestamp( timestamp2 );
        }
        if ( timestamp2 == null ) {
            return dateFromWorkspaceTimestamp( timestamp1 );
        } else {
            Date t1 = dateFromWorkspaceTimestamp( timestamp1 );
            Date t2 = dateFromWorkspaceTimestamp( timestamp2 );                
            return earlierWorkspaceDate( t1, t2 );
        }
    }

    /**
     * Get the common branch-time (branch and time) after which the two
     * branch-times could have differences. If neither workspace is a parent of
     * the other, then the result getCopyTime() works. If one is a parent of the
     * other, then the time is the earlier of the timepoint of the parent and
     * getCopyTime(). If they are the same, then the time is the earlier of
     * the two.
     * 
     * @param ws1
     * @param ws2
     * @param timestamp1
     * @param timestamp2
     * @return the branch-time pair
     */
    public static Pair< WorkspaceNode, Date >
            getCommonBranchPoint( WorkspaceNode ws1, WorkspaceNode ws2,
                                  String timestamp1, String timestamp2 ) {
        Date t1 = dateFromWorkspaceTimestamp( timestamp1 );
        Date t2 = dateFromWorkspaceTimestamp( timestamp2 );
        return getCommonBranchPoint( ws1, ws2, t1, t2 );
    }
    public static Pair< WorkspaceNode, Date >
        getCommonBranchPoint( WorkspaceNode ws1, WorkspaceNode ws2,
                              Date t1, Date t2 ) {
        Date commonBranchTimePoint = null;
        WorkspaceNode commonParent = WorkspaceNode.getCommonParent(ws1, ws2);

        // If the workspaces are the same
        if ( NodeUtil.workspacesEqual( ws1, ws2 ) ) {
            return new Pair< WorkspaceNode, Date >( commonParent,
                                                    earlierWorkspaceDate( t1, t2 ) );
        }
        
        // Not the same workspace; use relative copyTime.
        //commonBranchTimePoint = ws1 == null ? ws2.getCopyTime(ws1) : ws1.getCopyTime(ws2);
        Date commonBranchTimePoint1 = null;
        Date commonBranchTimePoint2 = null;
        if (ws1 != null) {
            commonBranchTimePoint1 = ws1.getCopyTime(ws2);
        }
        if (ws2 != null) {
            commonBranchTimePoint2 = ws2.getCopyTime(ws1);
        }
        commonBranchTimePoint = earlierWorkspaceDate( commonBranchTimePoint1, commonBranchTimePoint2 );
        
        if ( commonParent == null ? ws1 == null : commonParent.equals( ws1 ) ) {
            commonBranchTimePoint = earlierWorkspaceDate( commonBranchTimePoint, t1 );
        }
        if ( commonParent == null ? ws2 == null : commonParent.equals( ws2 )) {
            commonBranchTimePoint = earlierWorkspaceDate( commonBranchTimePoint, t2 );
        }

        return  new Pair< WorkspaceNode, Date >( commonParent, commonBranchTimePoint );
    }
}
