/**
 *
 */
package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AbstractDiff computes and stores the difference between two sets of objects
 * of type T. T may have properties of type P that each have an identifier of
 * type ID.
 *
 * @param <T>
 *            object type
 * @param <P>
 *            property type
 * @param <ID>
 *            identifier type
 */
public abstract class AbstractDiff<T,P,ID> implements Diff<T,P,ID> {

    protected static boolean computeDiffOnConstruction = false;
    //protected boolean lazy = true;
    protected boolean ignoreRemovedProperties = false;

    public Set<T> set1, set2;
    public Map<ID, T> map1, map2;
    public Set<T> removed = null;
    public Set<T> added = null;
    public Set<T> updated = null;
    public Map< ID, Map< ID, P > > removedProperties = null;
    public Map< ID, Map< ID, P > > addedProperties = null;
    public Map< ID, Map< ID, Pair< P, P > > > updatedProperties = null;
    public Map< ID, Map< ID, Pair< P, P > > >  propertyChanges = null;


    public abstract ID getId( T t );
    public abstract ID getPropertyId( P property );
    public abstract Set< P > getProperties( T t );
    public abstract P getProperty( T t, ID id );

    public AbstractDiff( Set<T> s1, Set<T> s2 ) {
        this( s1, s2, null, null );
    }

    public AbstractDiff( Set<T> s1, Set<T> s2,
                         Boolean lazy, Boolean ignoreRemovedProperties ) {
        //if ( lazy != null ) this.lazy = lazy;
        if ( ignoreRemovedProperties != null ) {
            this.ignoreRemovedProperties = ignoreRemovedProperties;
        }
        set1 = s1;
        set2 = s2;
        if ( computeDiffOnConstruction ) diffProperties();
    }

    public AbstractDiff( Map<ID, T> map1, Map<ID, T> map2 ) {
        this( map1, map2, null, null );
    }

    public AbstractDiff( Map<ID, T> m1, Map<ID, T> m2,
                         Boolean lazy, Boolean ignoreRemovedProperties ) {
        //if ( lazy != null ) this.lazy = lazy;
        if ( ignoreRemovedProperties != null ) {
            this.ignoreRemovedProperties = ignoreRemovedProperties;
        }
        map1 = m1;
        map2 = m2;
        if ( computeDiffOnConstruction ) diffProperties();
    }

    public Map<ID, P> getPropertyMap( T t ) {
        Set< P > propertiesSet = getProperties( t );
        Map< ID, P > properties = convertPropertySetToMap( propertiesSet );
        return properties;
    }

    protected void diffProperties() {
        // re-initialize members
        added = new LinkedHashSet<T>();
        removed = new LinkedHashSet<T>();
        updated = new LinkedHashSet<T>();

        propertyChanges = new LinkedHashMap< ID, Map<ID,Pair<P,P>> >();

        addedProperties = new LinkedHashMap< ID, Map<ID,P> >();
        removedProperties = new LinkedHashMap< ID, Map<ID,P> >();
        updatedProperties = new LinkedHashMap< ID, Map<ID,Pair<P,P>> >();

        List< Set< ID > > mapDiff = Utils.diff( getMap1(), getMap2() );
        Set<ID> addedIds = mapDiff.get( 0 );
        Set<ID> removedIds = mapDiff.get( 1 );
        Set<ID> updatedIds = mapDiff.get( 2 );

        for ( ID id : addedIds ) {
            T t2 = get2( id );
            if ( t2 == null ) {
                Debug.error("AbstractDiff: trying to add null entry for " + id + "!");
            } else {
                added.add( t2 );
            }
        }
        for ( ID id : removedIds ) {
            T t1 = get1( id );
            if ( t1 == null ) {
                Debug.error("AbstractDiff: trying to add null entry for " + id + "!");
            } else {
                removed.add( t1 );
            }
        }
        for ( ID id : updatedIds ) {
            mapDiff = diffProperties( id );
            Set<ID> addedPropIds = mapDiff.get( 0 );
            Set<ID> removedPropIds = mapDiff.get( 1 );
            Set<ID> updatedPropIds = mapDiff.get( 2 );

            LinkedHashMap< ID, P > addedProps = new LinkedHashMap< ID, P >();
            addedProperties.put( id, addedProps );
            LinkedHashMap< ID, P > removedProps = new LinkedHashMap< ID, P >();
            removedProperties.put( id, removedProps  );
            LinkedHashMap< ID, Pair<P,P> > updatedProps = new LinkedHashMap< ID, Pair<P,P> >();
            updatedProperties.put( id, updatedProps  );
            LinkedHashMap< ID, Pair<P,P> > propChanges = new LinkedHashMap< ID, Pair<P,P> >();
            propertyChanges.put( id, propChanges );

            for ( ID pid : addedPropIds ) {
                P p1 = get1( id, pid );
                P p2 = get2( id, pid );
                propChanges.put( pid, new Pair< P, P >( p1, p2 ) );
                addedProps.put( pid, p2 );
            }
            if ( !ignoreRemovedProperties ) {
                for ( ID pid : removedIds ) {
                    P p1 = get1( id, pid );
                    P p2 = get2( id, pid );
                    propChanges.put( pid, new Pair< P, P >( p1, p2 ) );
                    removedProps.put( pid, p1 );
                }
            }
            for ( ID pid : updatedIds ) {
                P p1 = get1( id, pid );
                P p2 = get2( id, pid );
                propChanges.put( pid, new Pair< P, P >( p1, p2 ) );
                updatedProps.put( pid, new Pair< P, P >( p1, p2 ) );
            }
        }
    }


    /**
     * Compute property changes and save them in propertyChanges.
     * @return
     */
    protected List< Set< ID > > diffProperties( ID tid ) {
        T t1 = get1(tid);
        T t2 = get2(tid);

        Map< ID, P > properties1 = getPropertyMap( t1 );
        Map< ID, P > properties2 = getPropertyMap( t2 );

        List< Set< ID > > mapDiff = Utils.diff( properties1, properties2 );

        return mapDiff;
    }

    @Override
    public boolean areDifferent() {
        return !areSame();
    }

    @Override
    public boolean areSame() {
        return getPropertyChanges().isEmpty();
    }

    @Override
    public Set< T > get1() {
        if ( set1 == null  && map1 != null ) {
            set1 = new LinkedHashSet< T >( map1.values() );
        }
        return set1;
    }

    @Override
    public Set< T > get2() {
        if ( set2 == null  && map2 != null ) {
            set2 = new LinkedHashSet< T >( map2.values() );
        }
        return set2;
    }

    protected Map<ID, T> convertSetToMap( Set<T> set ) {
        LinkedHashMap< ID, T > map = new LinkedHashMap< ID, T >();
        for ( T t : set ) {
            ID id = getId( t );
            map.put( id, t );
        }
        return map;
    }

    protected Map<ID, P> convertPropertySetToMap( Set<P> set ) {
        LinkedHashMap< ID, P > map = new LinkedHashMap< ID, P >();
        for ( P p : set ) {
            ID id = getPropertyId( p );
            map.put( id, p );
        }
        return map;
    }

    public Map< ID, T > getMap1() {
        if ( map1 == null && set1 != null ) {
            map1 = convertSetToMap( set1 );
        }
        return map1;
    }

    public Map< ID, T > getMap2() {
        if ( map2 == null && set2 != null ) {
            map2 = convertSetToMap( set2 );
        }
        return map2;
    }

    @Override
    public T get1( ID tid ) {
        return getMap1().get( tid );
    }

    @Override
    public T get2( ID tid ) {
        return getMap2().get( tid );
    }

    @Override
    public P get1( ID tid, ID pid ) {
        T t = get1( tid );
        if ( t == null ) return null;
        P p = getProperty(t, pid);
        return p;
    }

    @Override
    public P get2( ID tid, ID pid ) {
        T t = get2( tid );
        if ( t == null ) return null;
        P p = getProperty(t, pid);
        return p;
    }

    @Override
    public Set< T > getRemoved() {
        if ( removed == null ) {
            diffProperties();
        }
        return removed;
    }

    @Override
    public Set< T > getAdded() {
        if ( added == null ) {
            diffProperties();
        }
        return added;
    }

    @Override
    public Set< T > getUpdated() {
        if ( updated == null ) {
            diffProperties();
        }
        return updated;
    }

    @Override
    public Map< ID, Map< ID, P >> getRemovedProperties() {
        if ( removedProperties == null ) {
            diffProperties();
        }
        return removedProperties;
    }

    @Override
    public Map< ID, Map< ID, P >> getAddedProperties() {
        if ( addedProperties == null ) {
            diffProperties();
        }
        return addedProperties;
    }

    @Override
    public Map< ID, Map< ID, Pair< P, P >>> getUpdatedProperties() {
        if ( updatedProperties == null ) {
            diffProperties();
        }
        return updatedProperties;
    }

    @Override
    public Map< ID, Map< ID, Pair< P, P >>> getPropertyChanges() {
        if ( propertyChanges == null ) {
            diffProperties();
        }
        return propertyChanges;
    }

}
