package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * NodeDiff provides access to differences between two nodes in alfresco, at
 * least, in terms of its properties. All String keys in Sets and Maps are in
 * the short format, such as cm:name. The values are the raw Serializable values
 * returned by getProperties(). The second node is treated as the result of a
 * change to the first.
 * 
 * TODO -- this does not diff content
 */
public class NodeDiff implements Diff<NodeRef, Object, String> {
    protected static boolean computeDiffOnConstruction = false;
    protected boolean lazy = true;
    protected boolean ignoreRemovedProperties = false;
    
    public NodeRef node1, node2;
    public Set<String> removedAspects = null;
    public Set<String> addedAspects = null;
    public Map<String, Object> removedProperties = null;
    public Map<String, Object> addedProperties = null;
    public Map<String, Pair< Object, Object > > updatedProperties = null;
    public Map<String, Pair< Object, Object > > propertyChanges = null;
    private ServiceRegistry services = null;

    public NodeDiff( NodeRef node1, NodeRef node2 ) {
        this( node1, node2, null, null );
    }

    public NodeDiff( NodeRef node1, NodeRef node2,
                     Boolean lazy, Boolean ignoreRemovedProperties ) {
        if ( lazy != null ) this.lazy = lazy;
        if ( ignoreRemovedProperties != null ) {
            this.ignoreRemovedProperties = ignoreRemovedProperties;
        }
        this.node1 = node1;
        this.node2 = node2;
        if ( computeDiffOnConstruction ) diffProperties();
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#areDifferent()
     */
    @Override
    public boolean areDifferent() {
        return !areSame();
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#areSame()
     */
    @Override
    public boolean areSame() {
        return getPropertyChanges().isEmpty();
    }

    /**
     * Compute property changes and save them in propertyChanges.
     */
    protected void diffProperties() {
        NodeService nodeService = getServices().getNodeService();
        Map< QName, Serializable > properties1 = nodeService.getProperties( node1 );
        Map< QName, Serializable > properties2 = nodeService.getProperties( node2 );
        Set< QName > keys = new TreeSet< QName >( properties1.keySet() );
        keys.addAll( properties2.keySet() );
        propertyChanges = new TreeMap< String, Pair<Object,Object> >();
        if ( !lazy ) {
            addedProperties = new TreeMap< String, Object >();
//            if ( !ignoreRemovedProperties ) {
                removedProperties = new TreeMap< String, Object >();
//            }
            updatedProperties = new TreeMap< String, Pair<Object,Object> >();
        }
        if ( !NodeUtil.exists( node1 ) || !NodeUtil.exists( node2 ) ) return;
        for ( QName qName : keys ) {
            Serializable val1 = properties1.get( qName );
            Serializable val2 = properties2.get( qName );
            
            if ( val1 == null && val2 == null ) continue;
            String propName = qName.toPrefixString();
            if ( !lazy ) {
                if ( val1 == null ) {
                    addedProperties.put( propName, val2 );
                } else if ( val2 == null ) {
                    if ( !ignoreRemovedProperties ) {
                        removedProperties.put( propName, val1 );
                    }
                } else {
                    updatedProperties.put( propName, new Pair< Object, Object >( val1, val2 ) );
                }
            }
            propertyChanges.put( propName, new Pair< Object, Object >( val1, val2 ) );
        }
    }

    private ServiceRegistry getServices() {
        if ( services == null ) {
            services = NodeUtil.getServices();
        }
        return services;
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#getNode1()
     */
    @Override
    public Set<NodeRef> get1() {
        return Utils.newSet(node1);
    }
    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#getNode2()
     */
    @Override
    public Set<NodeRef> get2() {
        return Utils.newSet(node2);
    }
    
    protected void diffAspects() {
        // TODO -- create generic diff, union, intersect, subtract utility functions
        Set<QName> aspects1 = NodeUtil.getAspects( node1 );
        Set<QName> aspects2 = NodeUtil.getAspects( node2 );
        Pair<Set<QName>, Set<QName> > p = Utils.diff(aspects1, aspects2);
        addedAspects =
                new LinkedHashSet< String >( NodeUtil.qNamesToStrings( p.first ) );
        removedAspects =
                new LinkedHashSet< String >( NodeUtil.qNamesToStrings( p.second ) );
    }

    public Set< String > getRemovedAspects() {
        if ( removedAspects == null ) {
            diffAspects();
        }
        return removedAspects;
    }

    public Set< String > getAddedAspects() {
        if ( addedAspects == null ) {
            diffAspects();
        }
        return addedAspects;
    }
    
    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#getRemovedProperties()
     */
    @Override
    public Map< NodeRef, Map< String, Object > > getRemovedProperties() {
        if ( removedProperties == null ) {
            removedProperties = new TreeMap< String, Object >();
            if ( !ignoreRemovedProperties ) {
                for ( Map.Entry< String, Pair< Object, Object > > e : getPropertyChanges().get(node1).entrySet() ) {
                    if ( e.getValue().second == null ) {
                        removedProperties.put( e.getKey(),
                                               e.getValue().first );
                    }
                }
            }
            // diffProperties();
        }
        Map< NodeRef, Map< String, Object > > removedMap =
                new HashMap< NodeRef, Map< String, Object > >();
        removedMap.put( node1, removedProperties );
        return removedMap;
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#getAddedProperties()
     */
    @Override
    public Map< NodeRef, Map< String, Object > > getAddedProperties() {
        if ( addedProperties == null ) {
            addedProperties = new TreeMap< String, Object >();
            for ( Map.Entry< String, Pair< Object, Object > > e : getPropertyChanges().get(node1).entrySet() ) {
                if ( e.getValue().first == null ) {
                    addedProperties.put( e.getKey(), e.getValue().second );
                }
            }
//            diffProperties();
        }
        Map< NodeRef, Map< String, Object > > addedMap =
                new HashMap< NodeRef, Map< String, Object > >();
        addedMap.put( node1, addedProperties );
        return addedMap;
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#getUpdatedProperties()
     */
    @Override
    public Map< NodeRef, Map< String, Pair< Object, Object >>> getUpdatedProperties() {
        if ( updatedProperties == null ) {
            updatedProperties = new TreeMap< String, Pair<Object, Object> >( getPropertyChanges().get( node1 ) );
            for ( String k : getAddedProperties().get(node1).keySet() ) {
                updatedProperties.remove( k );
            }
            if ( !ignoreRemovedProperties ) {
                for ( String k : getRemovedProperties().get(node1).keySet() ) {
                    updatedProperties.remove( k );
                }
            }
//            diffProperties();
        }
        Map< NodeRef, Map< String, Pair< Object, Object > > > updatedMap = new HashMap< NodeRef, Map<String,Pair<Object,Object>> >();
        updatedMap.put( node1, updatedProperties );
        return updatedMap;
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.util.Diff#getPropertyChanges()
     */
    @Override
    public Map< NodeRef, Map< String, Pair< Object, Object >>> getPropertyChanges() {
        if ( propertyChanges == null ) {
            diffProperties();
        }
        Map< NodeRef, Map< String, Pair< Object, Object > > > propertyMap = new HashMap< NodeRef, Map<String,Pair<Object,Object>> >();
        propertyMap.put( node1, propertyChanges );
        return propertyMap;
    }

    @Override
    public Object get( NodeRef t, String id ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set< NodeRef > getRemoved() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set< NodeRef > getAdded() {
        // TODO Auto-generated method stub
        return null;
    }
}
