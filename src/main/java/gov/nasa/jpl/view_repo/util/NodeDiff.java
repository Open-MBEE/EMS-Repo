package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.AbstractDiff;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * NodeDiff provides access to differences between two nodes in alfresco, at
 * least, in terms of its properties. All String keys in Sets and Maps are in
 * the short format, such as cm:name. The values are the raw Serializable values
 * returned by getProperties(). The second node is treated as the result of a
 * change to the first.
 *
 * TODO -- this does not diff content
 * TODO -- a map from node ID to added and removed aspects
 */
public class NodeDiff extends AbstractDiff<NodeRef, Object, String> {

    public static class NodeNameComparator implements Comparator<NodeRef> {
        public static final NodeNameComparator instance = new NodeNameComparator();
        @Override
        public int compare( NodeRef ref1, NodeRef ref2 ) {
            if ( ref1 == ref2 ) return 0;
            if ( ref1 == null ) return -1;
            if ( ref2 == null ) return 1;
            String n1 = NodeUtil.getName( ref1 );
            String n2 = NodeUtil.getName( ref2 );
            if ( n1 == n2 ) return 0;
            if ( n1 == null ) return -1;
            if ( n2 == null ) return 1;
            return n1.compareTo( n2 );
        }
    }

    public NodeDiff( Map< String, NodeRef > m1, Map< String, NodeRef > m2,
                     Boolean ignoreRemovedProperties ) {
        //super( m1, m2, ignoreRemovedProperties );
        super( m1, m2, NodeNameComparator.instance, ignoreRemovedProperties );
        // TODO Auto-generated constructor stub
    }

    public NodeDiff( Map< String, NodeRef > map1, Map< String, NodeRef > map2 ) {
        super( map1, map2, NodeNameComparator.instance );
        if ( map1 != null && map1.size() == 1 && map2 != null && map2.size() == 1 ) {
            node1 = getMap1().values().iterator().next();
            node2 = getMap2().values().iterator().next();
        }
        // TODO Auto-generated constructor stub
    }

    public NodeDiff( Set< NodeRef > s1, Set< NodeRef > s2,
                     Boolean ignoreRemovedProperties ) {
        super( s1, s2, NodeNameComparator.instance, ignoreRemovedProperties );
        // TODO Auto-generated constructor stub
    }

    public NodeDiff( Set< NodeRef > s1, Set< NodeRef > s2 ) {
        super( s1, s2, NodeNameComparator.instance );
        // TODO Auto-generated constructor stub
    }

    public NodeRef node1, node2;
    protected Map<String, Set< String > > removedAspects = null;
    protected Map< String, Set< String > > addedAspects = null;
    public ServiceRegistry services = null;

    public NodeDiff( NodeRef node1, NodeRef node2 ) {
        this( node1, node2, null );
    }

    public static Set<NodeRef> newSet( NodeRef node1 ) {
        Set< NodeRef > s = new TreeSet< NodeRef >( NodeNameComparator.instance );
        s.add(node1);
        return s;
    }

    public NodeDiff( NodeRef node1, NodeRef node2,
                     Boolean ignoreRemovedProperties ) {
        super( newSet( node1 ), newSet( node2 ),
               NodeNameComparator.instance, ignoreRemovedProperties );
        if ( ignoreRemovedProperties != null ) {
            this.ignoreRemovedProperties = ignoreRemovedProperties;
        }
        this.node1 = node1;
        this.node2 = node2;
        if ( computeDiffOnConstruction ) diff();
    }

    public boolean isValueSpec(NodeRef ref) {
        EmsScriptNode node = ref != null ? new EmsScriptNode(ref, getServices()) : null;
        return isValueSpec(node);
    }
    
    public boolean isValueSpec(EmsScriptNode node) {
        return node != null ? node.hasOrInheritsAspect( Acm.ACM_VALUE_SPECIFICATION ) : false;
    }
    public boolean sameOwnerOfProperty( Object p1, Object p2 ) {
        NodeRef n1 = objectToNodeRef( p1 );
        NodeRef n2 = objectToNodeRef( p2 );
        if ( n1 != null && n2 != null ) {
            return sameOwner(n1, n2);
        }
        return false;
    }

    @Override
    public boolean sameProperty( Object p1, Object p2 ) {
        boolean isSame = false;
        List< EmsScriptNode > n1 = objectToEmsScriptNodes( p1 );
        List< EmsScriptNode > n2 = objectToEmsScriptNodes( p2 );
        // FIXME: May want to do a sort to increase performance here
        if ( n1 != null && n2 != null && n1.size() == n2.size()) {
            isSame = true;
            boolean fndSame = false;
            // Unfortunately, the ordering in the array may not be the
            // same even if they contain the same nodes.  So must check
            // each entry against all others:
            for (int i = 0; i < n1.size(); i++) {
                fndSame = false;
                for (int j = 0; j < n2.size(); j++) {
                    if (same(n1.get( i ),n2.get( j ))) {
                        fndSame = true;
                        break;
                    }
                }
                if (!fndSame) {
                    isSame = false;
                    break;
                }
            }
        }
        return isSame;
    }

    public EmsScriptNode getOwner( EmsScriptNode e ) {
        EmsScriptNode p = e.getOwningParent( null, e.getWorkspace() );
        return p;//.getNodeRef();
    }
    public EmsScriptNode getOwner( NodeRef t ) {
        EmsScriptNode e = new EmsScriptNode( t, getServices() );
        return getOwner( e );
    }

    public boolean sameOwner( NodeRef t1, NodeRef t2 ) {
        EmsScriptNode e1 = new EmsScriptNode( t1, getServices() );
        EmsScriptNode e2 = new EmsScriptNode( t2, getServices() );
        return sameOwner( e1, e2 );
    }
    public boolean sameOwner( EmsScriptNode e1, EmsScriptNode e2 ) {
        EmsScriptNode p1 = getOwner( e1 );
        EmsScriptNode p2 = getOwner( e2 );
        return same( p1, p2 );
    }

    /**
     * @param t1
     * @param t2
     * @return whether the nodes refer to the same element from two different workspaces
     */
    @Override
    public boolean same( NodeRef t1, NodeRef t2 ) {
        EmsScriptNode e1 = new EmsScriptNode( t1, getServices() );
        EmsScriptNode e2 = new EmsScriptNode( t2, getServices() );
        return same( e1, e2 );
    }
    
    public boolean same( EmsScriptNode p1, EmsScriptNode p2 ) {
        if ( p1 == null || p2 == null ) return p1 == p2;
        if ( p1.getSysmlId().equals( p2.getSysmlId() ) ) return true;
        if ( isValueSpec( p1 ) && isValueSpec( p2 ) ) {
            if ( p1.getTypeName().equals( p2.getTypeName() ) ) {
                return sameOwner( p1, p2 );
            }
        }
        return false;
    }

    @Override
    public String getName( NodeRef t ) {
        EmsScriptNode n = new EmsScriptNode(t, getServices());
        return n.getSysmlName( null );
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.mbee.util.AbstractDiff#diff()
     */
    @Override
    public void diff() {
        super.diff();
        fixValueSpecifications();
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.mbee.util.AbstractDiff#diffProperties(java.lang.Object)
     */
    @Override
    protected List< Set< String >> diffProperties( String tid ) {
        return super.diffProperties( tid );
    }

    private ServiceRegistry getServices() {
        if ( services == null ) {
            services = NodeUtil.getServices();
        }
        return services;
    }

    protected void diffAspects() {
        addedAspects = new LinkedHashMap<String, Set< String > >();
        removedAspects = new LinkedHashMap<String, Set< String > >();
        Set< String > intersection =
                new LinkedHashSet< String >( getMap1().keySet() );
        Utils.intersect( intersection, getMap2().keySet() );
        for ( String id : intersection ) {
            NodeRef n1 = get1(id);
            NodeRef n2 = get2(id);
            if ( !NodeUtil.exists( n1 ) ) continue;
            if ( !NodeUtil.exists( n2 ) ) continue;
            Set<QName> aspects1 = NodeUtil.getAspects( n1 );
            Set<QName> aspects2 = NodeUtil.getAspects( n2 );
            Pair<Set<QName>, Set<QName> > p = Utils.diff(aspects1, aspects2);
            if ( p != null ) {
                List< String > aspects = null;
                if ( p.first != null ) {
                    aspects = NodeUtil.qNamesToStrings( p.first );
                    addedAspects.put( id, new TreeSet< String >( aspects ) );
                }
                if ( p.second != null ) {
                    aspects = NodeUtil.qNamesToStrings( p.second );
                    removedAspects.put( id, new TreeSet< String >( aspects ) );
                }
            }
        }
    }

    public Map< String, Set< String > > getRemovedAspects() {
        if ( removedAspects == null ) {
            diffAspects();
        }
        return removedAspects;
    }

    public Set< String > getRemovedAspects( String name ) {
        Set< String > result = getRemovedAspects().get( name );
        if ( result == null ) {
            result = Utils.newSet();
            getAddedAspects().put( name, result );
        }
        return result;
    }

    public Map< String, Set< String > > getAddedAspects() {
        if ( addedAspects == null ) {
            diffAspects();
        }
        return addedAspects;
    }

    public Set< String > getAddedAspects( String name ) {
        Set< String > result = getAddedAspects().get( name );
        if ( result == null ) {
            result = Utils.newSet();
            getAddedAspects().put( name, result );
        }
        return result;
    }

    @Override
    public String getId( NodeRef t ) {
        EmsScriptNode node = new EmsScriptNode( t, getServices() );
        return node.getSysmlId();
    }

    protected EmsScriptNode objectToEmsScriptNode( Object o ) {
        if ( o instanceof NodeRef ) {
            EmsScriptNode n = new EmsScriptNode( (NodeRef)o, getServices() );
            return n;
        } else if ( o instanceof EmsScriptNode ) {
            return (EmsScriptNode)o;
        } else if ( o instanceof Collection ) {
            if ( ( (Collection<?>)o ).size() > 0 ) {
                return objectToEmsScriptNode( ( (Collection<?>)o ).iterator().next() );
            }
        }
        return null;
    }
    
    protected List<EmsScriptNode> objectToEmsScriptNodes( Object o ) {
        List<EmsScriptNode> nodeList = new ArrayList<EmsScriptNode>();
        if ( o instanceof NodeRef ) {
            EmsScriptNode n = new EmsScriptNode( (NodeRef)o, getServices() );
            nodeList.add( n );
        } else if ( o instanceof EmsScriptNode ) {
            nodeList.add((EmsScriptNode)o);
        } else if ( o instanceof Collection ) {
            for (Object ref : (Collection<?>)o) {
                nodeList.add( objectToEmsScriptNode(ref) );
            }
        } else {
            return null;
        }
        return nodeList;
    }

    protected NodeRef objectToNodeRef( Object o ) {
        if ( o instanceof NodeRef ) {
            return (NodeRef)o;
        } else if ( o instanceof EmsScriptNode ) {
            return ((EmsScriptNode)o).getNodeRef();
        } else if ( o instanceof Collection ) {
            if ( ( (Collection<?>)o ).size() > 0 ) {
                return objectToNodeRef( ( (Collection<?>)o ).iterator().next() );
            }
        }
        return null;
    }

    @Override
    public String getIdOfProperty( Object property ) {
        EmsScriptNode n = objectToEmsScriptNode( property );
        if ( n != null ) return n.getSysmlId();
        return null;
    }
    @Override
    public String getPropertyName( Object property ) {
        EmsScriptNode n = objectToEmsScriptNode( property );
        if ( n != null ) return n.getSysmlName();
        return null;
    }

    @Override
    public Set< Object > getProperties( NodeRef t ) {
        Map< String, Object > map = getPropertyMap( t );
        LinkedHashSet< Object > set = new LinkedHashSet< Object >( map.values() );
        return set;
    }

    @Override
    public Object getProperty( NodeRef ref, String id ) {
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        
        // Special case for type b/c it is not a property, and actual based on the
        // aspect applied to the node:
        return id.equals( Acm.JSON_TYPE ) ? node.getTypeName() : node.getNodeRefProperty( id );
    }

    @Override
    public Map<String, Object> getPropertyMap( NodeRef ref ) {
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        Map< String, Object > props = node.getProperties();
        props.put( Acm.JSON_TYPE, node.getTypeName() );
        Utils.removeAll( props, getPropertyIdsToIgnore() );
        return props;
    }
    
    public Map<String, Object> getOwnerPropertyMap( NodeRef ref ) {
        EmsScriptNode owner = getOwner(ref);
        NodeRef ownerRef = owner != null ? owner.getNodeRef() : null;
        return ownerRef != null ? getPropertyMap(ownerRef) : null;
    }
    
    private Pair<NodeRef,String> filterPropValNodeRef(Object propVal, String propName,
                                                      NodeRef t1, List<String> generated) {
        
        NodeRef hasSameOwner = null;
        String hasSameOwnerId = null;
        
        // Try to find a correlated node:
        if (t1.equals( propVal )) {
            // Go through all of the generated ids and look for matches:
            for ( String addedId : generated ) {
                NodeRef t2 = get2( addedId );
                
                if ( same( t1, t2 ) ) {
                    Map< String, Object > props2 = getOwnerPropertyMap(t2);
                    
                    for (Entry< String, Object > entry2 : props2.entrySet()) {
                        // Same property name
                        if (entry2.getKey().equals( propName )) {
                            // This is a matching property for the value:
                            if (t2.equals( entry2.getValue() )) {
                                // REVIEW check anything else here? sysml name?
                                hasSameOwner = t2;
                                hasSameOwnerId = addedId;
                            }
                            break; // dont need to check any more properties
                        }
                    }
                }
                
                // No need to look for multiple matches b/c it should not be
                // possible as we are checking for the same owner, type, and
                // property name and these are only single noderefs:
                if (hasSameOwner != null) {
                    break;
                }
            }
        }
        
        return new Pair<NodeRef,String>(hasSameOwner, hasSameOwnerId);
    }
    
    private Pair<NodeRef,String> filterPropValNodeRefs(Object propVal, String propName,
                                                      NodeRef t1, List<String> generated) {
        
        NodeRef hasSameOwner = null;
        String hasSameOwnerId = null;
        List<NodeRef> propValList = (List<NodeRef>)propVal;
        
        // Loop through the property value noderef list and try to find
        // correlated node:
        for (int i = 0; i < propValList.size(); i++) {
            NodeRef ref = propValList.get( i );
            if (t1.equals( ref )) {
                
                // Go through all of the generated ids and look for matches:
                for ( String addedId : generated ) {
                    NodeRef t2 = get2( addedId );
                    
                    if ( same( t1, t2 ) ) {
                        Map< String, Object > props2 = getOwnerPropertyMap(t2);
                        
                        for (Entry< String, Object > entry2 : props2.entrySet()) {
                            // Same property name
                            if (entry2.getKey().equals( propName )) {
                                // This is a matching property for the value
                                // in the list at the same index:
                                List<NodeRef> propValList2 = (List<NodeRef>)entry2.getValue();
                                if (propValList2.size() > i &&
                                    t2.equals(propValList2.get( i ))) {
                                    // REVIEW check anything else here? sysml name?
                                    hasSameOwner = t2;
                                    hasSameOwnerId = addedId;
                                }
                                break; // dont need to check any more properties
                             }
                        }
                    }
                    
                    // No need to look for multiple matches b/c it should not be
                    // possible as we are checking for the same owner, type, and
                    // property name and the specific index in the node ref array:
                    if (hasSameOwner != null) {
                        break;
                    }
                }
            }
        }
        
        return new Pair<NodeRef,String>(hasSameOwner, hasSameOwnerId);
    }
    
    private boolean filterPropVal(String id, NodeRef t1, List<String> generated, Set<String> addedIds, 
                                  Set<String> updatedIds,
                                  List<String> removeFromRemovedIds) {
        
        NodeRef hasSameOwner = null;
        String hasSameOwnerId = null;
        boolean addToRemoved = true;
        Pair<NodeRef,String> res = new Pair<NodeRef,String>(null,null); 
        Map< String, Object > props = getOwnerPropertyMap(t1);
        
        // Loop through each property of the owner of t1:
        for (Entry< String, Object > entry : props.entrySet()) {
            // TODO For now checking every property, but only need to check that properties
            //      that map to value specs
            String propName = entry.getKey();
            Object propVal = entry.getValue();
            
            // If the property value is a node ref:
            if (propVal instanceof NodeRef) {
                res = filterPropValNodeRef(propVal,propName,t1,generated);
            }
            
            // If the property value is a list of node refs:
            else if (propVal instanceof List) {
                res = filterPropValNodeRefs(propVal,propName,t1,generated);
            }
            
            hasSameOwner = res.first;
            hasSameOwnerId = res.second;
            
            // TODO We would need a more complicated diff map structure to accommodate
            //      the same value spec symlid being used more than once by the owner;
            //      however, this will currently never be the case b/c value spec sysmlids
            //      are auto-generated.
            // Once found, we dont need to look for multiple matches:
            if (hasSameOwner != null) {
                break;
            }
            
        } // ends for (Entry< String, Object > entry : props.entrySet())
        
        // If found a matching node that is correlated, ie same owner, type, property type:
        if ( hasSameOwner != null ) {
            addToRemoved = false;
            added.remove(hasSameOwner);
            addedIds.remove( hasSameOwnerId );
            // Note: dont want to directly alter "generated" b/c of nesting
            removeFromRemovedIds.add( id );
            updatedIds.add( id );
            // Re-add the object to map2 using the key from map1, so
            // that diffProperties() below will find both objects
            // with the same id.
            // REVIEW -- Does any of the code rely on the map key
            // being the same as the sysml id?
            getMap2().put( id, hasSameOwner );
        }
        
        return addToRemoved;
    }
    
    private void filterValuesImpl(String id, Set<String> addedIds, Set<String> updatedIds,
                                  List<String> removeFromRemovedIds,
                                  List<String> generated) {
        
        NodeRef t1 = get1( id );
        if ( t1 == null ) {
            Debug.error("NodeDiff: trying to add null entry for " + id + "!");
        } 
        else {
            boolean addToRemoved = true;
            if ( isValueSpec( t1 ) ) {

                addToRemoved = filterPropVal(id, t1, generated, addedIds, updatedIds,
                                             removeFromRemovedIds);     
            } // ends if ( isValueSpec( t1 ) )
    
            if ( addToRemoved ) removed.add( t1 );

        }  // ends else
        
    }
    
    /**
     * Does the appropriate filtering for value specs, as they are embedded
     * in the owners, and have a unique sysmlid that is auto-generated.
     * Consequently, they end up showing up as diffs even if the contents
     * are the same.
     * 
     * Updates mapDiff, map1, map2, and returned a updated updatedIds set.
     */
    @Override
    public Set<String> filterValues(List<Set<String>> mapDiff)
    {
        Set<String> addedIds = mapDiff.get( 0 );
        Set<String> removedIds = mapDiff.get( 1 );
        Set<String> updatedIds = mapDiff.get( 2 );
        List<String> generated = new ArrayList<String>();
        List<String> removeFromRemovedIds = new ArrayList< String >();
        
        // Create the generated list, which will be used to check for correlated
        // nodes:
        for ( String id : addedIds ) {
            NodeRef t2 = get2( id );
            if ( t2 == null ) {
                Debug.error("NodeDiff: trying to add null entry for " + id + "!");
            } else {
                added.add( t2 );
                if ( isValueSpec( t2 ) ) {
                    generated.add( id );
                }
            }
        }

        /*
         * Go through each value spec property of the owner of the value spec, 
         * and check if the value of the property is the node of interest,
         * or contains the node of interest in the case of a list of NodeRefs.
         * Then for each generated id, check if it maps to the same property and
         * location within the list (if needed), the same owner, and same type.
         * If there is a match then remove the id from addedIds, removedIds, and
         * add to the updatedIds and the id in map1 to map2. 
         */
        for ( String id : removedIds ) {
            filterValuesImpl(id, addedIds, updatedIds, removeFromRemovedIds, generated);           
        }  

        removedIds.removeAll( removeFromRemovedIds );
        
        return updatedIds;
    }

    /**
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

        if (Debug.isOn()) Debug.outln("added = " + getAdded() );
        if (Debug.isOn()) Debug.outln("removed = " + getRemoved() );
        if (Debug.isOn()) Debug.outln("updated = " + getUpdated() );

        if (Debug.isOn()) Debug.outln("added properties= " + getAddedProperties() );
        if (Debug.isOn()) Debug.outln("removed properties= " + getRemovedProperties() );
        if (Debug.isOn()) Debug.outln("updated properties= " + getUpdatedProperties() );
        if (Debug.isOn()) Debug.outln("property changes = " + getPropertyChanges() );

        LinkedHashMap< EmsScriptNode, EmsScriptNode > valueSpecMap = new LinkedHashMap< EmsScriptNode, EmsScriptNode >();
        
        for ( NodeRef e : getAdded() ) {
            EmsScriptNode node = new EmsScriptNode( e, getServices() );
            if ( node.isOwnedValueSpec(node.getWorkspace()) ) {
                EmsScriptNode owningProp = node.getValueSpecOwner(node.getWorkspace());
                if (owningProp != null) {
                // TODO -- REVIEW -- Does the if statement below need to be uncommented?
//                if ( !getRemoved().contains( owningProp ) ) {
                    valueSpecMap.put( node, owningProp );
//                }
                }
            }
        }
        for ( NodeRef e : getUpdated() ) {
            EmsScriptNode node = new EmsScriptNode( e, getServices() );
            if ( node.isOwnedValueSpec(node.getWorkspace()) ) { 
                EmsScriptNode owningProp = node.getValueSpecOwner(node.getWorkspace());
                if (owningProp != null) {
                    valueSpecMap.put( node, owningProp );
                }
            }
        }
        for ( NodeRef e : getRemoved() ) {
            EmsScriptNode node = new EmsScriptNode( e, getServices() );
            if ( node.isOwnedValueSpec(node.getWorkspace()) ) {
                EmsScriptNode owningProp = node.getValueSpecOwner(node.getWorkspace());
                if (owningProp != null) {
                    valueSpecMap.put( node, owningProp );
                }
            }
        }

        // adding the owning Property elements to these element maps if not already there
        for ( EmsScriptNode node : valueSpecMap.values() ) {
            if ( !getAdded().contains( node.getNodeRef() ) ) {
                if ( !getRemoved().contains( node.getNodeRef() ) ) {
                    if ( !getUpdated().contains( node.getNodeRef() ) ) {
                        getUpdated().add( node.getNodeRef() );
                    }
                }
            }
        }

        // Add the owning valuespec properties to the nodeDiff property change maps.
        for ( Entry<EmsScriptNode,EmsScriptNode> entry : valueSpecMap.entrySet() ) {
            EmsScriptNode valueNode = entry.getKey();
            EmsScriptNode owningPropNode = entry.getValue();
            Map< String, Pair< Object, Object > > propChanges = getPropertyChanges( owningPropNode.getSysmlId() );
//            if ( propChanges == null ) {
//                propChanges = new LinkedHashMap< String, Pair<Object,Object> >();
//                getPropertyChanges().put( owningPropNode.getSysmlId(), propChanges );
//            }

            // Find the matching property that this valueSpec maps to:
            String valueName = null;
            //String valueName = NodeUtil.createQName( "sysml:value", getServices() ).toString();
            for ( String acmType : Acm.TYPES_WITH_VALUESPEC.keySet() ) {
                if ( owningPropNode.hasOrInheritsAspect( acmType ) ) {
                    for ( String acmProp : Acm.TYPES_WITH_VALUESPEC.get(acmType) ) {
                        Object propVal = owningPropNode.getNodeRefProperty( acmProp, null, 
                                                                            owningPropNode.getWorkspace());
                        if ( propVal  != null) {
                            // ArrayList of noderefs:
                            if (propVal instanceof List) {
                                for (NodeRef ref : (List<NodeRef>) propVal) {
                                    EmsScriptNode propValNode = new EmsScriptNode(ref, services);
                                    if (propValNode.equals(valueNode, true)) {
                                        valueName = NodeUtil.createQName( acmProp, getServices() ).toString();
                                        break;
                                    }
                                }
                            }
                            // Single noderef:
                            else if (propVal instanceof NodeRef) {
                                EmsScriptNode propValNode = new EmsScriptNode((NodeRef) propVal, services);
                                if (propValNode.equals(valueNode, true)) {
                                    valueName = NodeUtil.createQName( acmProp, getServices() ).toString();
                                    break;
                                }
                            }
                        } // ends ( propVal  != null)
                    }
                    break;
                }
            } // ends for ( String acmType : Acm.TYPES_WITH_VALUESPEC.keySet() )
                        
            if (valueName != null) {
                Pair< Object, Object > valueChange = propChanges.get( valueName );
                if ( valueChange == null ) {
                    valueChange = new Pair< Object, Object >( null, null );
                    propChanges.put( valueName, valueChange );
                }
                if ( getRemoved().contains( valueNode.getNodeRef() ) ) {
                    valueChange.first = valueNode;
                    getRemovedProperties( owningPropNode.getSysmlId() ).put( valueName, valueNode );
                } else {
                    valueChange.second = valueNode;
                    if ( getAdded().contains( valueNode.getNodeRef() ) ) {
                        getAddedProperties( valueNode.getSysmlId() ).put( valueName, valueNode );
                    } else {
                        getUpdatedProperties( valueNode.getSysmlId() ).put( valueName, valueChange );
                    }
                }
            }
        }

        // Remove the owned ValueSpecifications from everything.
        for ( EmsScriptNode node : valueSpecMap.keySet() ) {
            getAdded().remove( node.getNodeRef() );
            getUpdated().remove( node.getNodeRef() );
            getRemoved().remove( node.getNodeRef() ); // Is this right?????!!!

            getPropertyChanges().remove( node.getSysmlId() );
            getAddedProperties().remove( node.getSysmlId() );
            getRemovedProperties().remove( node.getSysmlId() );
            getUpdatedProperties().remove( node.getSysmlId() );

            get1().remove( node.getNodeRef() );
            getMap1().remove( node.getSysmlId() );
            get2().remove( node.getNodeRef() );
            getMap2().remove( node.getSysmlId() );
        }

        if (Debug.isOn()) Debug.outln("added = " + getAdded() );
        if (Debug.isOn()) Debug.outln("removed = " + getRemoved() );
        if (Debug.isOn()) Debug.outln("updated = " + getUpdated() );

        if (Debug.isOn()) Debug.outln("added properties= " + getAddedProperties() );
        if (Debug.isOn()) Debug.outln("removed properties= " + getRemovedProperties() );
        if (Debug.isOn()) Debug.outln("updated properties= " + getUpdatedProperties() );
        if (Debug.isOn()) Debug.outln("property changes = " + getPropertyChanges() );

    }

}
