package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.AbstractDiff;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.junit.Assert;

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
        return node.getName();
    }

    @Override
    public String getPropertyId( Object property ) {
        Assert.assertFalse( true );
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
        return node.getProperty( id );
    }

    @Override
    public Map<String, Object> getPropertyMap( NodeRef ref ) {
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        Map< String, Object > props = node.getProperties();
        // TODO --REVIEW -- HERE!  Do we need to add anything here for specializations? Expressions?
        Utils.removeAll( props, getPropertyIdsToIgnore() );
        return props;
    }

//    /**
//     * Remove ValueSpecification elements in diff and include the differences as
//     * value property updates in the owning Property.
//     *
//     * If a ValueSpecification (which has many subclasses/subaspects) has
//     * changed, and it is owned by a Property, then the value property of the
//     * owning Property has changed. In the JSON output, we should show the
//     * changed value in the owning element (in its specialization) instead of as
//     * a separate element.
//     *
//     * To do this, this WorkspaceDiff is altered by
//     * <ul>
//     * <li>removing Property-owned ValueSpecifications from the added, removed,
//     * and updatedElements maps,
//     * <li>adding the owning Property elements to these element maps if not
//     * already there, and
//     * <li>adding the value properties to the nodeDiff.propertyChanges as well
//     * as the added, removed, and updatedProperties maps of the nodeDiff.
//     * </ul>
//     *
//     * One tricky part is that an Expression may be owned by another Expression
//     * that is owned by a Property.
//     *
//     * Another tricky part is that an owning Property may be added or deleted,
//     * in which case the ValueSpecification may need to be handled differently.
//     * @param workspaceDiff TODO
//     */
//    protected void fixValueSpecifications(WorkspaceDiff workspaceDiff ) {
//
//        // Identify the Properties that own changed ValueSpecifications and add
//        // them to the updatedElements map.
//
//        Set< EmsScriptNode > valueSpecs = new LinkedHashSet<EmsScriptNode>();
//        //Map< String, EmsScriptNode > refs = getAddedElements();
//        for ( Entry< String, EmsScriptNode > e : workspaceDiff.getAddedElements().entrySet() ) {
//            EmsScriptNode node = e.getValue();
//            if ( isPropertyOwnedValueSpecification( node ) ) {
//                valueSpecs.add( node );
//            }
//        }
//        for ( Entry< String, EmsScriptNode > e : workspaceDiff.getUpdatedElements().entrySet() ) {
//            EmsScriptNode node = e.getValue();
//            if ( isPropertyOwnedValueSpecification( node ) ) {
//                valueSpecs.add( node );
//            }
//        }
//        // what about getDeletedElements()?
//        // TODO
//
//
//        // Remove the owned ValueSpecifications from the element maps.
//
//        for ( EmsScriptNode node : valueSpecs ) {
//            workspaceDiff.getAddedElements().remove( node.getName() );
//            workspaceDiff.getUpdatedElements().remove( node.getName() );
//            workspaceDiff.getDeletedElements().remove( node.getName() ); // Is this right?????!!!
//        }
//
//        // Add the owning Properties' values to the nodeDiff property change maps.
//
//        //for ( )
//        // TODO
//
//    }

    /**
     * Remove ValueSpecification elements in diff and include the differences as
     * value property updates in the owning Property.
     * <p>
     * If a ValueSpecification (which has many subclasses/subaspects) has
     * changed, and it is owned by a Property, then the value property of the
     * owning Property has changed. In the JSON output, we should show the
     * changed value in the owning element (in its specialization) instead of as
     * a separate element.
     * <p>
     * To do this, this WorkspaceDiff is altered by
     * <ul>
     * <li>removing Property-owned ValueSpecifications from the added, removed,
     * and updatedElements maps,
     * <li>adding the owning Property elements to these element maps if not
     * already there, and
     * <li>adding the value properties to the nodeDiff.propertyChanges as well
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

        // Identify the Properties that own changed ValueSpecifications and add
        // them to the updatedElements map.

        System.out.println("added = " + getAdded() );
        System.out.println("removed = " + getRemoved() );
        System.out.println("updated = " + getUpdated() );

        System.out.println("added properties= " + getAddedProperties() );
        System.out.println("removed properties= " + getRemovedProperties() );
        System.out.println("updated properties= " + getUpdatedProperties() );
        System.out.println("property changes = " + getPropertyChanges() );

        Set< EmsScriptNode > valueSpecs = new LinkedHashSet<EmsScriptNode>();
        Set< EmsScriptNode > owningProperties = new LinkedHashSet<EmsScriptNode>();
        //Map< String, EmsScriptNode > refs = getAddedElements();
        for ( NodeRef e : getAdded() ) {
            EmsScriptNode node = new EmsScriptNode( e, getServices() );
            if ( isPropertyOwnedValueSpecification( node ) ) {
                EmsScriptNode owningProp = getOwningProperty( node );
                // TODO -- REVIEW -- Does the if statement below need to be uncommented?
//                if ( !getRemoved().contains( owningProp ) ) {
                    owningProperties.add( owningProp );
                    valueSpecs.add( node );
//                }
            }
        }
        for ( NodeRef e : getUpdated() ) {
            EmsScriptNode node = new EmsScriptNode( e, getServices() );
            if ( isPropertyOwnedValueSpecification( node ) ) {
                valueSpecs.add( node );
                owningProperties.add( getOwningProperty( node ) );
            }
        }
        for ( NodeRef e : getRemoved() ) {
            EmsScriptNode node = new EmsScriptNode( e, getServices() );
            if ( isPropertyOwnedValueSpecification( node ) ) {
                valueSpecs.add( node );
                owningProperties.add( getOwningProperty( node ) );
            }
        }

        System.out.println("valueSpecs = " + valueSpecs );
        System.out.println("owningProperties = " + owningProperties );

        // adding the owning Property elements to these element maps if not already there
        for ( EmsScriptNode node : owningProperties ) {
            if ( !getAdded().contains( node.getNodeRef() ) ) {
                if ( !getRemoved().contains( node.getNodeRef() ) ) {
                    if ( !getUpdated().contains( node.getNodeRef() ) ) {
                        getUpdated().add( node.getNodeRef() );
                    }
                }
            }
        }

        // Add the owning Properties' values to the nodeDiff property change maps.
        for ( EmsScriptNode valueNode : valueSpecs ) {
            EmsScriptNode owningPropNode = getOwningProperty( valueNode );
            Map< String, Pair< Object, Object > > propChanges = getPropertyChanges( owningPropNode.getName() );
//            if ( propChanges == null ) {
//                propChanges = new LinkedHashMap< String, Pair<Object,Object> >();
//                getPropertyChanges().put( owningPropNode.getName(), propChanges );
//            }
            String valueName = NodeUtil.createQName( "sysml:value", getServices() ).toString();
            Pair< Object, Object > valueChange = propChanges.get( valueName );
            if ( valueChange == null ) {
                valueChange = new Pair< Object, Object >( null, null );
                propChanges.put( valueName, valueChange );
            }
            if ( getRemoved().contains( valueNode.getNodeRef() ) ) {
                valueChange.first = valueNode;
                getRemovedProperties( owningPropNode.getName() ).put( valueName, valueNode );
            } else {
                valueChange.second = valueNode;
                if ( getAdded().contains( valueNode.getNodeRef() ) ) {
                    getAddedProperties( valueNode.getName() ).put( valueName, valueNode );
                } else {
                    getUpdatedProperties( valueNode.getName() ).put( valueName, valueChange );
                }
            }
        }

        // Remove the owned ValueSpecifications from everything.
        for ( EmsScriptNode node : valueSpecs ) {
            getAdded().remove( node.getNodeRef() );
            getUpdated().remove( node.getNodeRef() );
            getRemoved().remove( node.getNodeRef() ); // Is this right?????!!!

            getPropertyChanges().remove( node.getName() );
            getAddedProperties().remove( node.getName() );
            getRemovedProperties().remove( node.getName() );
            getUpdatedProperties().remove( node.getName() );

            get1().remove( node.getNodeRef() );
            getMap1().remove( node.getName() );
            get2().remove( node.getNodeRef() );
            getMap2().remove( node.getName() );
        }

        System.out.println("added = " + getAdded() );
        System.out.println("removed = " + getRemoved() );
        System.out.println("updated = " + getUpdated() );

        System.out.println("added properties= " + getAddedProperties() );
        System.out.println("removed properties= " + getRemovedProperties() );
        System.out.println("updated properties= " + getUpdatedProperties() );
        System.out.println("property changes = " + getPropertyChanges() );

    }


    protected static boolean isProperty( EmsScriptNode node ) {
        if ( node == null ) return false;
        if ( node.hasOrInheritsAspect( "sysml:Property" ) ) return true;
        if ( node.getProperty(Acm.ACM_VALUE ) != null ) return true;
        return false;
    }

    protected static EmsScriptNode getOwningProperty( EmsScriptNode node ) {
        System.out.println("getOwningProperty(" + node + ")");
        EmsScriptNode parent = node;
        while ( parent != null && !isProperty( parent ) ) {
            System.out.println("parent = " + parent );
            parent = parent.getUnreifiedParent( null );  // TODO -- REVIEW -- need timestamp??!!
        }
        System.out.println("returning " + parent );
        return parent;
    }

    protected static boolean isPropertyOwnedValueSpecification( EmsScriptNode node ) {
//        NodeService ns = NodeUtil.getServices().getNodeService();
//        if ( ns.hasAspect( node.getNodeRef(), NodeUtil.createQName( "sysml:ValueSpecification" ) ) ) {
        if ( node.hasOrInheritsAspect( "sysml:ValueSpecification" ) ) {
            EmsScriptNode parent = getOwningProperty( node );
            return parent != null && isProperty( parent );
        }
        return false;
    }

}
