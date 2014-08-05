package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.AbstractDiff;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
 */
public class NodeDiff extends AbstractDiff<NodeRef, Object, String> {

    public NodeDiff( Map< String, NodeRef > m1, Map< String, NodeRef > m2,
                     Boolean ignoreRemovedProperties ) {
        super( m1, m2, ignoreRemovedProperties );
        // TODO Auto-generated constructor stub
    }

    public NodeDiff( Map< String, NodeRef > map1, Map< String, NodeRef > map2 ) {
        super( map1, map2 );
        if ( map1 != null && map1.size() == 1 && map2 != null && map2.size() == 1 ) {
            node1 = getMap1().values().iterator().next();
            node2 = getMap2().values().iterator().next();
        }
        // TODO Auto-generated constructor stub
    }

    public NodeDiff( Set< NodeRef > s1, Set< NodeRef > s2,
                     Boolean ignoreRemovedProperties ) {
        super( s1, s2, ignoreRemovedProperties );
        // TODO Auto-generated constructor stub
    }

    public NodeDiff( Set< NodeRef > s1, Set< NodeRef > s2 ) {
        super( s1, s2 );
        // TODO Auto-generated constructor stub
    }

    public NodeRef node1, node2;
    protected Set<String> removedAspects = null;
    protected Set<String> addedAspects = null;
    public ServiceRegistry services = null;

    public NodeDiff( NodeRef node1, NodeRef node2 ) {
        this( node1, node2, null );
    }

    public NodeDiff( NodeRef node1, NodeRef node2,
                     Boolean ignoreRemovedProperties ) {
        super( Utils.newSet( node1 ), Utils.newSet( node2 ), ignoreRemovedProperties );
        if ( ignoreRemovedProperties != null ) {
            this.ignoreRemovedProperties = ignoreRemovedProperties;
        }
        this.node1 = node1;
        this.node2 = node2;
        if ( computeDiffOnConstruction ) diffProperties();
    }

    private ServiceRegistry getServices() {
        if ( services == null ) {
            services = NodeUtil.getServices();
        }
        return services;
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
        return new LinkedHashSet< Object >( getPropertyMap( t ).values() );
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
        return props;
    }

}
