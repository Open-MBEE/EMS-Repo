package gov.nasa.jpl.view_repo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.AbstractDiff;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

public class JsonDiffDiff extends AbstractDiff< JSONObject, Object, String > {

    protected Set<JSONObject> elements = Utils.newSet();
    protected LinkedHashMap<String, Pair<DiffOp, List<JSONObject> > > diffMap1 =
            new LinkedHashMap< String, Pair<DiffOp,List<JSONObject> > >();
    protected LinkedHashMap<String, Pair<DiffOp, List<JSONObject> > > diffMap2 =
            new LinkedHashMap< String, Pair<DiffOp,List<JSONObject> > >();
    
    public JsonDiffDiff( Map< String, JSONObject > map1,
                         Map< String, JSONObject > map2 ) {
        super( map1, map2, null );
    }
    
    public JsonDiffDiff( JSONObject diff ) {
        this( null, null );
        JSONObject ws1 = diff.optJSONObject( "workspace1" );            
        JSONArray elems1 = ws1.getJSONArray( "elements" );
        JSONObject ws2 = diff.optJSONObject( "workspace2" );
        if ( ws2 == null ) {
            // TODO -- ERROR
            return;
        }
        JSONArray added2 = ws2.optJSONArray( "addedElements" );
        JSONArray updated2 = ws2.optJSONArray( "updatedElements" );
        JSONArray deleted2 = ws2.optJSONArray( "deletedElements" );
        
        if ( elems1 != null ) getElements().addAll( Utils.asList( toList(elems1),
                                                                  JSONObject.class ) );
        if ( added2 != null ) getAdded().addAll(  Utils.asList( toList(added2),
                                                                JSONObject.class ) );
        if ( updated2 != null ) getUpdated().addAll(  Utils.asList( toList(updated2),
                                                                    JSONObject.class ) );
        if ( deleted2 != null ) getRemoved().addAll(  Utils.asList( toList(deleted2),
                                                                    JSONObject.class ) );
    }
    
    public Set< JSONObject > getElements() {
        return elements;
    }

    public void setElements( Set< JSONObject > elements ) {
        this.elements = elements;
    }

    public LinkedHashMap< String, Pair< DiffOp, List< JSONObject > > > getDiffMap1() {
        if ( Utils.isNullOrEmpty( diffMap1 ) ) {
            diffMap1 = new LinkedHashMap< String, Pair<DiffOp,List<JSONObject> > >();
            glom( DiffOp.ADD, getElements(), diffMap1 );
        }
        return diffMap1;
    }

    public void setDiffMap1( LinkedHashMap< String, Pair< DiffOp, List< JSONObject > > > diffMap1 ) {
        this.diffMap1 = diffMap1;
    }

    public LinkedHashMap< String, Pair< DiffOp, List< JSONObject > > > getDiffMap2() {
        if ( Utils.isNullOrEmpty( diffMap2 ) ) {
            diffMap2 = new LinkedHashMap< String, Pair<DiffOp,List<JSONObject> > >();
            glom( DiffOp.ADD, getAdded(), diffMap2 );
            glom( DiffOp.UPDATE, getUpdated(), diffMap2 );
            glom( DiffOp.DELETE, getRemoved(), diffMap2 );       
        }
        return diffMap2;
    }

//    public void setDiffMap2( LinkedHashMap< String, Pair< DiffOp, List< JSONObject > > > diffMap2 ) {
//        this.diffMap2 = diffMap2;
//    }
    public void set1( String id, JSONObject element ) {
        set( id, DiffOp.ADD, element, true );
    }
    public void set1( String id, DiffOp op, JSONObject element ) {
        set( id, op, element, true );
    }
    public void set2( String id, DiffOp op, JSONObject element ) {
        set( id, op, element, false );
    }
    public void set( String id, DiffOp op, JSONObject element, boolean workspace1 ) {
        LinkedHashMap< String, Pair< DiffOp, List< JSONObject > > > diffMap =
                workspace1 ? diffMap1 : diffMap2;

        // NONE or deleting the element from workspace1 means removing it.
        if ( ( workspace1 && op == DiffOp.DELETE ) || op == DiffOp.NONE ) {
            diffMap.remove( id );
        }
        
        Pair< DiffOp, List< JSONObject > > p = diffMap.get( id );
        
        // If there is no entry in the map for the sysmlid, create a new
        // entry with the operation and element.
        if ( p == null ) {
            ArrayList< JSONObject > list = Utils.newList();
            if ( element != null ) list.add( element );
            p = new Pair< DiffOp,List< JSONObject > >( op, list );
            diffMap.put( id, p );
        } else {
            //DiffOp oldOp = p.first;
            p.first = op;
            if ( p.second == null ) p.second = Utils.newList();
            else p.second.clear();
            if ( element != null ) {
                p.second.add( element );
            }
            switch ( op ) {
                case ADD:
                    removeFromAdded( id );
                    getAdded().add( element );
                    break;
                case UPDATE:
                    removeFromUpdated( id );
                    getUpdated().add( element );
                    break;
                case DELETE:
                    removeFromRemoved( id );
                    getRemoved().add( element );
                    break;
                case NONE:
                default:
                   // TODO -- ERROR
            }
        }
    }

    public static String id( JSONObject o ) {
        return o.optString( "sysmlid" );
    }
    
    protected void removeFromAdded( String id ) {
        removeFrom( id, getAdded() );
    }
    
    protected void removeFromUpdated( String id ) {
        removeFrom( id, getUpdated() );
    }

    protected void removeFromRemoved( String id ) {
        removeFrom( id, getRemoved() );
    }

    protected static void removeFrom( String id, Set<JSONObject> coll ) {
        if ( id == null || coll == null ) return;
        Iterator<JSONObject> i = coll.iterator();
        while ( i.hasNext() ) {
            JSONObject o = i.next();
            if ( id.equals( id( o ) ) ) {
                i.remove();
                break;
            }
        }
    }

    @Override
    public String getId( JSONObject t ) {
        return id( t );
    }

    public Set<String> getAffectedIds() {
        Set<String> ids = Utils.newSet();
        for ( JSONObject o : getAdded() ) {
            ids.add( getId( o ) );
        }
        for ( JSONObject o : getUpdated() ) {
            ids.add( getId( o ) );
        }
        for ( JSONObject o : getRemoved() ) {
            ids.add( getId( o ) );
        }
        return ids;
    }
    
    @Override
    public String getPropertyName( Object property ) {
        // This method isn't applicable.
        if ( property instanceof String ) return (String)property;
        return null;
    }

    @Override
    public String getIdOfProperty( Object property ) {
        // This method isn't applicable.
        if ( property instanceof String ) return (String)property;
        return null;
    }

    @Override
    public Set< Object > getProperties( JSONObject t, boolean isSet1 ) {
        JSONObject obj = null;
        Map<String, JSONObject > m = isSet1 ? getMap1() : getMap2();
        String id = getId( t );
        if ( id != null ) {
            JSONObject objx = m.get(id);
            if ( objx != null ) obj = objx;
        }
        if ( obj == null ) return Utils.newSet();
        Set<Object> set = new LinkedHashSet< Object >( obj.keySet() );
        set.remove( "specialization" );
        JSONObject spec = obj.optJSONObject( "specialization" );
        if ( spec != null ) {
            set.addAll( spec.keySet() );
        }
        return set;
    }

    @Override
    public Object getProperty( JSONObject t, String id, boolean isSet1 ) {
        Map<String, JSONObject > m = isSet1 ? getMap1() : getMap2();
        JSONObject obj = m.get( id );
        if ( obj == null ) obj = t;
        if ( obj != null ) {
            return obj.get( id );
        }
        return null;
    }
    
    public static Collection< String > getPropertyIds( JSONObject t ) {
        Set<String> set = new LinkedHashSet< String >( t.keySet() );
        set.remove( "specialization" );
        JSONObject spec = t.optJSONObject( "specialization" );
        if ( spec != null ) {
            set.addAll( spec.keySet() );
        }
        return set;
    }

    
    public static Map<String, Object> getPropertyMap( JSONObject element ) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        JSONObject specialization = element.optJSONObject( "specialization" );
        for ( String k : getPropertyIds( element ) ) {
            Object v = null;
            if ( element.has( k ) ) {
                v = element.opt( k );
            } else if ( specialization != null && specialization.has( k ) ) {
                v = specialization.opt( k );
            } else {
                continue;
            }
            properties.put( k, v );
        }
        return properties;
    }


    public static Map<String,JSONObject > toElementMap( JSONObject o ) {
        Map< String, Object > map = toMap( o );
        return Utils.toMap( map, String.class, JSONObject.class );
    }

    public static Map<String, Object> toMap( JSONObject o ) {
        Map<String, Object> m = Utils.newMap();
        for ( Object k : o.keySet() ) {
            if ( k instanceof String ) {
                String key = (String)k;
                Object newObj = o.get( key );
                if ( newObj instanceof JSONObject ) {
                    m.put( key, toMap( (JSONObject)newObj ) );
                } else if ( newObj instanceof JSONArray ) {
                    m.put( key, toList( (JSONArray)newObj ) );
                } else {
                    m.put( key, newObj );
                }
            }
        }
        return m;
    }
    
    public static ArrayList< JSONObject > toElementList( JSONArray arr ) {
        List< Object > list = toList( arr );
        return Utils.asList( list, JSONObject.class );
    }
    
    public static List< Object > toList( JSONArray arr ) {
        List< Object > m = Utils.newList();
        for ( int i = 0; i < arr.length(); ++i ) {
            Object newObj = arr.get( i );
            if ( newObj instanceof JSONObject ) {
                m.add( toMap( (JSONObject)newObj ) );
            } else if ( newObj instanceof JSONArray ) {
                m.add( toList( (JSONArray)newObj ) );
            } else {
                m.add( newObj );
            }
        }
        return m;
    }
    
    public static boolean sameElement( JSONObject t1, JSONObject t2 ) {
        int comp = CompareUtils.compareCollections( toMap( t1 ), toMap( t2 ),
                                                    true, false );
        return comp == 0;
    }

    @Override
    public boolean same( JSONObject t1, JSONObject t2 ) {
        return sameElement(t1, t2);
    }

    @Override
    public boolean sameProperty( Object prop1, Object prop2 ) {
        // REVIEW -- might want to check and see if prop1 is a JSONObject or JSONArray.
        int comp = CompareUtils.compare( prop1, prop2 );
        return comp == 0;
    }

    @Override
    public String getName( JSONObject t ) {
        return t.optString( "name" );
    }

    @Override
    public Set< String > filterValues( List< Set< String >> mapDiff ) {
        return super.filterValues(mapDiff);
    }
    
    protected void removeFromDiff(String id) {
        removeFromAdded( id );
        removeFromUpdated( id );
        removeFromRemoved( id );
        diffMap1.remove( id );
        diffMap2.remove( id );
    }

   /**
    * Update a diff with changes to the two workspaces.
    * @param diff0 the original diff that is being modified
    * @param diff1 changes to the first workspace of diff0
    * @param diff2 changes to the second workspace of diff0
    * @return
    */
   public static JSONObject diff( JSONObject diff0, JSONObject diff1, JSONObject diff2 ) {
       // Make a copy of the original diff and update the copy to return.
       JSONObject diff3 = NodeUtil.clone( diff0 );
       
       // Go ahead and combine the changes to the second workspace.
       // We need to be careful to not affect workspace1 of diff3, so let's
       // first remove diff2's workspace1 before glomming.
       JSONObject diff2NoWs1 = NodeUtil.clone( diff2 );
       JSONObject ws1 = diff2NoWs1.optJSONObject( "workspace1" );
       if ( ws1 != null ) {
           JSONArray ws1Elements = ws1.optJSONArray( "elements" );
           if ( ws1Elements == null || ws1Elements.length() > 0 ) {
               ws1.put("elements", new JSONArray() );
           }
       }
       // Now add diff2 to diff3.
       glom(diff3, diff2NoWs1);
       
       // Get all the workpace pieces of the diffs.
       JsonDiffDiff dDiff1 = new JsonDiffDiff( diff1 );

       JsonDiffDiff dDiff3 = new JsonDiffDiff( diff3 );
       
       // Compute the diff for each affected element.
       for ( String id : dDiff1.getAffectedIds() ) {
           DiffOp op1 = dDiff1.getDiffOp(id);
           DiffOp op3 = dDiff3.getDiffOp(id);
           JSONObject element1_2 = dDiff1.getElement2( id );
           JSONObject element1_1 = dDiff1.getElement1( id );
           JSONObject element3_2 = dDiff3.getElement2( id );
           JSONObject element3_1 = dDiff3.getElement1( id );
           // Compute the op3 - op1 case.
           switch ( op1 ) {
               case ADD:
                   switch ( op3 ) {
                       case ADD: // ADD - ADD = UPDATE
                       case UPDATE: // UPDATE - ADD = UPDATE
                           Pair< JSONObject, JSONObject > undonePair =
                                   undo( element3_1, element1_2, true );
                           JSONObject undone = undonePair.first;
                           JSONObject newElement3_1 = undonePair.second;
                           JSONObject updated =
                                   glomElements( undone, element3_2, false );

                           dDiff3.updateDiff( id, newElement3_1, updated,
                                               DiffOp.UPDATE );
                           break;
                       case DELETE:  // DELETE - ADD = DELETE
                           newElement3_1 = NodeUtil.clone( element1_2 );
                           dDiff3.set2( id, DiffOp.DELETE, newElement3_1 );
                           dDiff3.set1( id, newElement3_1 );
                           break;
                       case NONE:  // NONE - ADD = DELETE
                           newElement3_1 = NodeUtil.clone( element1_2 );
                           dDiff3.set2( id, DiffOp.DELETE, newElement3_1 );
                           dDiff3.set1( id, newElement3_1 );
                       default:
                          // TODO -- ERROR
                   }
                   break;
               case UPDATE:
                   switch ( op3 ) {
                       case ADD: // ADD - UPDATE = UPDATE
                       case UPDATE: // UPDATE - UPDATE = UPDATE
                           Pair< JSONObject, JSONObject > undonePair =
                                   undo( element3_1, element1_2, false );
                           JSONObject undone = undonePair.first;
                           JSONObject newElement3_1 = undonePair.second;
                           JSONObject updated = 
                                   glomElements( undone, element3_2, false );
                           
                           dDiff3.updateDiff( id, newElement3_1, updated,
                                              DiffOp.UPDATE );
                           break;
                       case DELETE:  // DELETE - UPDATE = DELETE
                           newElement3_1 = NodeUtil.clone( element1_2 );
                           dDiff3.set2( id, DiffOp.DELETE, newElement3_1 );
                           dDiff3.set1( id, newElement3_1 );
                           break;
                       case NONE:  // NONE - UPDATE = UPDATE
                            JSONObject oldElement3_1 =
                                    ( element3_1 == null ? element1_1
                                                         : element3_1 );
                           undonePair = undo( oldElement3_1, element1_2, false);
                           undone = undonePair.first;
                           newElement3_1 = undonePair.second;

                           dDiff3.updateDiff( id, newElement3_1, undone,
                                              DiffOp.UPDATE );
                           break;
                       default:
                          // TODO -- ERROR
                   }
                   break;
               case DELETE:
                   switch ( op3 ) {
                       case ADD: // ADD - DELETE = ADD
                           dDiff3.set1( id, DiffOp.DELETE, element3_1 );
                           break;
                       case UPDATE: // UPDATE - DELETE = ADD
                           dDiff3.set2( id, DiffOp.ADD, element3_2 );
                           dDiff3.set1( id, DiffOp.DELETE, element3_1 );
                           break;
                       case DELETE:  // DELETE - DELETE = NONE
                           dDiff3.removeFromDiff(id);
                           break;
                       case NONE:  // NONE - DELETE = ADD
                           dDiff3.set2( id, DiffOp.ADD, element3_1 );
                           dDiff3.set1( id, DiffOp.DELETE, element3_1 );
                           break;
                       default:
                           // TODO -- ERROR
                   }
                   break;
               case NONE:
                   // Nothing to do for this case
                   switch ( op3 ) {
                       case ADD: // ADD - NONE = ADD
                       case UPDATE: // UPDATE - NONE = UPDATE
                       case DELETE:  // DELETE - NONE = DELETE
                       case NONE:  // NONE - NONE = NONE
                           break;
                       default:
                   }
                   break;
               default:
                   // TODO -- ERROR
           }
       }
       
       return diff3;
    }
   
    protected void updateDiff( String id,
                               JSONObject newElement1, JSONObject newElement2,
                               DiffOp newOp ) {
        JSONObject elementWithDiffApplied = glom(newElement1, newElement2);
        
        // If there was no change to the element, then remove it from 
        // the diff:
        if (sameElement(elementWithDiffApplied, newElement1)) {
            removeFromDiff(id);
        }
        else {
            // Update workspace1 and workspace2 with the results.
            set2( id, DiffOp.UPDATE, newElement2 );
            set1( id, newElement1 );
        }
    }

    public boolean isAffected( String id ) {
        DiffOp op = getDiffOp( id );
        return op != DiffOp.NONE;
    }

    public DiffOp getDiffOp( String id ) {
        DiffOp op = DiffOp.NONE;
        Pair< DiffOp, List< JSONObject > > p = diffMap2.get( id );
        if ( p != null ) {
            return p.first;
        }
        return op;
    }

    public JSONObject getElement1( String id ) {
        Pair< DiffOp, List< JSONObject > > p = diffMap1.get( id );
        if ( p == null ) return null;
        if ( Utils.isNullOrEmpty( p.second ) ) return null;
        if ( p.second.size() > 1 ) {
            // TODO -- ERROR -- only expected one
        }
        return p.second.get( 0 );
    }

    public JSONObject getElement2( String id ) {
        Pair< DiffOp, List< JSONObject > > p = diffMap2.get( id );
        if ( p == null ) return null;
        if ( Utils.isNullOrEmpty( p.second ) ) return null;
        if ( p.second.size() > 1 ) {
            // TODO -- ERROR -- only expected one
        }
        return p.second.get( 0 );
    }

    public static JSONObject glom( ArrayList<JSONObject> diffs ) {
        if ( Utils.isNullOrEmpty( diffs ) ) return null;
        JSONObject glommedDiff = makeEmptyDiffJson();
        if ( diffs.size() == 1 ) return glommedDiff;
        LinkedHashMap<String, Pair<DiffOp, List<JSONObject> > > diffMap1 =
                new LinkedHashMap< String, Pair<DiffOp,List<JSONObject>> >();
        LinkedHashMap<String, Pair<DiffOp, List<JSONObject> > > diffMap2 =
                new LinkedHashMap< String, Pair<DiffOp,List<JSONObject>> >();
        
        // Glom workspace 1 changes
        // Iterate through each diff in order adding any new elements that were
        // not in previous diffs.
        // TODO -- REVIEW -- Don't you want to overwrite these with any new values?!
        JSONArray elements = glommedDiff.getJSONArray( "elements" );
        for ( int i = 0; i < diffs.size(); ++i ) {
            JSONObject diff =  diffs.get( i );
            JSONObject ws1 = diff.optJSONObject( "workspace1" );            
            JSONArray dElements = ws1.getJSONArray( "elements" );
            for ( int j = 0; j < dElements.length(); ++j ) {
                JSONObject element = dElements.getJSONObject( j );
                String sysmlid = element.getString( "sysmlid" );
                if ( !diffMap1.containsKey( sysmlid ) ) {
                    elements.put( element );
                }
            }
        }
        
        // Glom workpace 2 changes
        for ( JSONObject diff : diffs ) {
            JSONObject ws2 = diff.optJSONObject( "workspace2" );
            if ( ws2 == null ) continue;
            JSONArray added = ws2.optJSONArray( "addedElements" );
            JSONArray updated = ws2.optJSONArray( "updatedElements" );
            JSONArray deleted = ws2.optJSONArray( "deletedElements" );
            // Diffs are applied in the order of add, update, delete
            glom( DiffOp.ADD, JsonDiffDiff.toElementList( added ), diffMap2 );
            glom( DiffOp.UPDATE, JsonDiffDiff.toElementList( updated ), diffMap2 );
            glom( DiffOp.DELETE, JsonDiffDiff.toElementList( deleted ), diffMap2 );
        }
    
        // now we need to merge the properties of chained updates
        JSONObject gws2 = glommedDiff.getJSONObject( "workspace2" );
        JSONArray added = gws2.getJSONArray( "addedElements" );
        JSONArray updated = gws2.getJSONArray( "updatedElements" );
        JSONArray deleted = gws2.getJSONArray( "deletedElements" );
        for ( Entry< String, Pair< DiffOp, List< JSONObject > > > entry : diffMap2.entrySet() ) {
            Pair< DiffOp, List< JSONObject > > p = entry.getValue();
            JSONObject glommedElement = null; //NodeUtil.newJsonObject();
            for ( JSONObject element : p.second ) {
                if ( glommedElement == null ) glommedElement = NodeUtil.clone( element );
                else addProperties( glommedElement, element );
            }
            switch ( p.first ) {
                case ADD:
                    added.put( glommedElement );
                    break;
                case UPDATE:
                    updated.put( glommedElement );
                    break;
                case DELETE:
                    deleted.put( glommedElement );
                    break;
                default:
                    // BAD! -- TODO
            }
            // TODO -- What about moved and conflicted elements?
        }
        
        return glommedDiff;
     }

    /**
     * Glom the specified elements per the specified operation to the glom map.
     * The map is used to avoid unnecessary merging of updates. For example,
     * three updates followed by a delete requires no update merging since the
     * element is getting deleted anyway. The map tracks the minimum number of
     * operation to glom all of the diffs.
     * 
     * @param op
     *            the ADD, UPDATE, or DELETE operation to apply to the elements
     * @param elements
     *            the elements to which the operation is applied and glommed
     *            with the glom map
     * @param glomMap
     *            a partial computation of a diff glomming as a map from sysmlid
     *            to an operation and a list of elements whose properties will
     *            be merged
     */
    protected static void glom( DiffOp op,
                                Collection<JSONObject> elements,
                                LinkedHashMap<String, Pair<DiffOp, 
                                List<JSONObject> > > glomMap ) {
        if ( glomMap == null || elements == null) return;
        // Apply the operation on each element to the map of the glommed diff (glomMap).
        for ( JSONObject element : elements ) {
            if ( element == null ) continue;
            String sysmlId = element.optString( Acm.JSON_ID );
            if ( sysmlId == null ) continue;
            Pair< DiffOp, List< JSONObject > > p = glomMap.get( sysmlId );
            // If there is no entry in the map for the sysmlid, create a new
            // entry with the operation and element.
            if ( p == null ) {
                p = new Pair< DiffOp,List< JSONObject > >( op, Utils.newList( element ) );
                glomMap.put( sysmlId, p );
            } else {
                switch( op ) {
                    case ADD:
                        // ADD always fully replaces ADD, UPDATE, and DELETE according to the
                        // table in the comments for glom( diff1, diff2).
                        p.second.clear();
                        p.second.add( element );
                        // already replaced above--now just update op for DELETE
                        switch ( p.first ) {
                            case ADD:
                                // ADD + ADD = ADD [potential conflict]
                            case UPDATE:
                                // UPDATE + ADD = UPDATE [potential conflict]
                                break;
                            case DELETE:
                                // DELETE + ADD = ADD
                                p.first = DiffOp.ADD;
                            default:
                                // BAD! -- TODO
                        }
                        break;
                    case UPDATE:
                        // UPDATE replaces DELETE but augments UPDATE and ADD
                        switch ( p.first ) {
                            case ADD:
                                // ADD + UPDATE = ADD --> augment
                            case UPDATE:
                                // UPDATE + UPDATE = UPDATE --> augment
                                p.second.add( element );
                                break;
                            case DELETE:
                                // DELETE + UPDATE = UPDATE --> replace [potential conflict]
                                p.first = DiffOp.UPDATE;
                                p.second.clear();
                                p.second.add( element );
                            default:
                                // BAD! -- TODO
                        }
                        break;
                    case DELETE:
                        // DELETE always fully replaces ADD and UPDATE. No
                        // change to an already deleted element.
                        switch ( p.first ) {
                            case ADD:
                                // ADD + DELETE = DELETE --> replace
                            case UPDATE:
                                // UPDATE + DELETE = DELETE --> replace
                                p.first = DiffOp.DELETE;
                                p.second.clear();
                                p.second.add( element );
                                break;
                            case DELETE:
                                // DELETE + DELETE = DELETE (no change)
                            default:
                                // BAD! -- TODO
                        }
                        break;
                    default:
                        // BAD! -- TODO
                }
            }
        }
    }


    /**
     * Calculate the diff that would result after applying one diff followed by
     * another, "glomming" them together. This is the '+' operation described in
     * {@link #performDiffGlom(Map)}.
     * <p>
     * In the element diff json, there is a workspace1 to show the original
     * elements and a workspace2 for the changes to those elements. Only
     * properties that have changed are included in the added and updated
     * elements in the workspace2 JSON, so the actual element in workspace 2 is
     * computed as the element in workspace1 (if it exists) augmented or
     * overwritten with the properties in the corresponding workspace2 element.
     * <p>
     * So, how do we merge two diff JSON objects? The workspace1 in diff2 could
     * be a modification of the workspace1 in diff1, possibly sa a result of the
     * workspace2 changes in diff1. In this case, it makes sense to use diff1's
     * workspace1 as that of the glommed diff since it is the pre-existing state
     * of the workspace before both diffs are applied. If this is not the case,
     * then it might make sense to add elements in workspace1 of diff2 that are
     * not in workspace1 of diff1 and that are not added by workspace2 of diff1.
     * <p>
     * To combine the workspace2 changes of the two diffs, the changes in
     * workspace2 of diff2 should be applied to those of workspace2 of diff1 to
     * get the glommed workspace2 changes. But, how to do this at the property
     * level is not obvious. For example, if diff1 and diff2 add the same
     * element with different properties, should the individual properties of
     * the add in diff2 be merged with those of diff1 or should the diff2 add
     * replace the diff1 add? This situation may indicate a conflict in
     * workspaces that the user should control. If the element were a view, then
     * merging would not make much sense, especially if it leads to
     * inconsistency among its properties. So, replacing the add is chosen as
     * the appropriate behavior. Below is a table showing how workspace2 changes
     * ore glommed:
     * 
     * <table style="width:100%", border="1">
     * <tr>
     * <th></th>
     * <th>add(x2)</th>
     * <th>delete(x)</th>
     * <th>update(x2)</th>
     * </tr>
     * <tr>
     * <th>add(x1)</th>
     * <td>add(x2) [potential conflict]</td>
     * <td>delete(x)</td>
     * <td>add(x1 &lt;- x2)</td>
     * </tr>
     * <tr>
     * <th>delete(x)</th>
     * <td>add(x2)</td>
     * <td>delete(x)</td>
     * <td>update(x2) [potential conflict]</td>
     * </tr>
     * <tr>
     * <th>update(x1)</th>
     * <td>update(x2) [potential conflict]</td>
     * <td>delete(x)</td>
     * <td>update(x1 &lt;- x2)</td>
     * </tr>
     * </table>
     * 
     * @param diff1
     *            workspace diff JSON
     * @param diff2
     *            workspace diff JSON
     * @return the combined diff of applying diff1 followed by diff2
     */
    public static JSONObject glom( JSONObject diff1, JSONObject diff2 ) {
       ArrayList< JSONObject > list = Utils.newList( diff1, diff2 );
       JSONObject diff3 = glom( list );
       return diff3;
    }

    protected static List< Set< String > >
            diffProperties( AbstractDiff< JSONObject, Object, String > aDiff,
                            JSONObject t1, JSONObject t2 ) {
        Map< String, Object > properties1 = getPropertyMap( t1 );
        Map< String, Object > properties2 = getPropertyMap( t2 );
        return diffProperties( aDiff, properties1, properties2 );
    }

    public static JSONObject toJson( List< Set< String > > propertyDiff,
                                     JSONObject element1, JSONObject element2 ) {
                                     //boolean something ) {
        // Start with the element change and alter based on the diff. If a
        // property is not in added or updated, then remove it.
        JSONObject element = NodeUtil.clone( element2 );
        if ( Utils.isNullOrEmpty( propertyDiff ) ) return element;
        Set< String > addedAndUpdatedIds = null, updatedIds = null, removedIds = null;
        addedAndUpdatedIds = propertyDiff.get( 0 ); // add added ids
        if ( propertyDiff.size() > 1 ) updatedIds = propertyDiff.get( 1 );
        if ( updatedIds != null ) addedAndUpdatedIds.addAll( updatedIds );
        if ( propertyDiff.size() > 2 ) removedIds = propertyDiff.get( 2 );
        
        JSONObject spec = element.optJSONObject( "specialization" );
        if ( spec == null ) {
            spec = new JSONObject();
            element.put( "specialization", spec );
        }
        
        // Find properties in element that were not added or updated, ie not
        // changed, and remove them from element:
        for ( String pId : new ArrayList<String>( getPropertyIds( element ) ) ) {
            if ( !addedAndUpdatedIds.contains( pId ) ) {//&& !replace ) {
                removeProperty( pId, element );
            }
        }
        
        JSONObject spec2 = element1.optJSONObject( "specialization" );
        for ( String pId : removedIds ) {
            if ( element1.has( pId ) ) {
                element.put( pId, JSONObject.NULL );
            } else if ( spec2.has( pId ) ) {
                    spec.put( pId, JSONObject.NULL );           
            } else {
                // TODO error
            }
        }
        return element;
    }
    
    public static Object removeProperty( String pId, JSONObject element ) {
        if ( element.has( pId ) ) {
            return element.remove( pId );
        }
        JSONObject spec = element.optJSONObject( "specialization" );
        if ( spec != null ) {
            return spec.remove( pId );
        }
        return null;
    }

    /**
     * Undo the changes represented by {@code element1} that would be made to
     * {@code element0}. In other words, return the element change that would
     * need to be applied to undo the changes that {@code element1} would make
     * to {@code element0} in order to restore {@code element0} to its original
     * state. {@code element0 + element1 + X = element0}. Solve for X.
     * 
     * @param element0
     * @param element1
     * @param replace whether element1 replaces element0 or just updates it
     * @return a pair representing X and element0 + element1
     */
    public static Pair<JSONObject,JSONObject> undo( JSONObject element0, JSONObject element1,
                                                    boolean replace ) {
        // The undoElement below is X in the equation, element0 + element1 + X =
        // element0.
        //
        // We compute element0plus1 by applying (glomming) element1 to element0,
        // taking into account whether element1's properties replace or update
        // property0's. This diff represents the changes that X needs to make,
        // so we just need it in the form of an element JSONObject. The toJson()
        // function does this translation.
        JSONObject element0plus1 = glomElements(element0, element1, replace);
        List< Set< String > > propDiff = diffProperties( null, element0plus1, element0 );
        JSONObject undoElement = toJson( propDiff, element0plus1, element0 );
        
        return new Pair<JSONObject,JSONObject>(undoElement,element0plus1);
    }

    public static JSONObject glomElements( JSONObject element0, JSONObject element1,
                                           boolean replace ) {
        // TODO -- If replacing element0 with element1 we can return element1, but do we
        // need to null the properties that element0 has the element1 does not
        // have?
        if ( replace ) return NodeUtil.clone( element1 );
        // If updating, we just add the properties in element1 to element0,
        // overwriting any those properties that are also in element0.
        JSONObject glommedElement = NodeUtil.clone( element0 );
        addProperties( glommedElement, element1 );
        return glommedElement;
    }
    
    public static JSONObject makeEmptyDiffJson() throws JSONException {
        JSONObject diffJson = NodeUtil.newJsonObject();
        JSONObject ws1Json = NodeUtil.newJsonObject();
        JSONObject ws2Json = NodeUtil.newJsonObject();

        diffJson.put("workspace1", ws1Json);
        diffJson.put("workspace2", ws2Json);
        
        JSONArray ws1Elements = new JSONArray();
        JSONArray ws2Added = new JSONArray();
        JSONArray ws2Updated = new JSONArray();
        JSONArray ws2Deleted = new JSONArray();
        ws1Json.put( "elements", ws1Elements );
        ws2Json.put( "addedElements", ws2Added );
        ws2Json.put( "updatedElements", ws2Updated );
        ws2Json.put( "deletedElements", ws2Deleted );

        // TODO -- moved and conflicted elements

        return diffJson;
    }

    protected static HashSet<String> ignoredJsonIds = new HashSet<String>() {
        {
            add("sysmlid");
            add("creator");
            add("modified");
            add("created");
            add("modifier");
        }
    };

    protected static void addProperties( JSONObject element1,
                                         JSONObject element2 ) {
        Iterator<String> i = element2.keys();
        while ( i.hasNext() ) {
            String k = i.next();
            if ( ignoredJsonIds.contains( k ) ) continue;
            element1.put( k, element2.get( k ) );
        }
    }

}

enum DiffOp { ADD, UPDATE, DELETE, NONE }
