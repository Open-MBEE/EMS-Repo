package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.HasPreference;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sysml.view.Viewable;

/**
 * An implementation of a {@link Viewable} list of {@link Viewable}s subclassing
 * {@link ArrayList}.
 * 
 * @see viewable.List
 * 
 */
public class List extends ArrayList< Viewable< EmsScriptNode > >
        implements sysml.view.List< EmsScriptNode > {//, HasPreference< java.util.List< Class > > {

    private static final long serialVersionUID = 3954654861037876503L;
    
    /**
     * The preferred constructor arguments.
     */
    protected static final HasPreference.Helper< java.util.List< Class > > preferences =
            new HasPreference.Helper< java.util.List< Class > >( (java.util.List< java.util.List< Class > >)Utils.newList( (java.util.List< Class >)Utils.newList( (Class)EmsScriptNode.class ),
                                                                           (java.util.List< Class >)Utils.newList( (Class)sysml.view.List.class ),
                                                                           (java.util.List< Class >)Utils.newList( (Class)Collection.class ),
                                                                           (java.util.List< Class >)Utils.newList( (Class)Object[].class ) ) );
    
    
    protected boolean ordered = false;
    
    /**
     * Create an empty List.
     * @see java.util.List#List()
     */
    public List() {
        super();
    }

//    /**
//     * Create a List and add the {@link Viewable}s in the input {@link Collection}.
//     * @param c
//     * @see java.util.List#List(Collection)
//     */
//    public List( Collection< ? extends Viewable< EmsScriptNode > > c ) {
//        super( c );
//    }
    
    /**
     * Create a List and add the {@link Viewable}s in the input {@link Collection}.
     * @param c
     * @see java.util.List#List(Collection)
     */
    public List( sysml.view.List< EmsScriptNode > c ) {
        this();
        add(c);
    }

    /**
     * Create a List and add the {@link Viewable}s in the input {@link Collection}.
     * @param c
     * @see java.util.List#List(Collection)
     */
    public List( Collection< ? > c ) {
        this(c.toArray() );
    }
    
    // FIXME java reflection is not smart enough to handle this correctly, 
    //		 as it doesnt work with an array of Names in a Object array, even
    //		 if typecasting that array to an Object, or wrapping it in another
    //		 Object array.  Believe the Object array must be a V array for this 
    //		 to work.
//    public <V extends Viewable<EmsScriptNode>> List(V... c) {
//    	this( Utils.newList(c));
//    }
    
    // FIXME this works, but it a crude solution
    public List(Object... c) {
    	
    	this();

    	addToList( c );
    }
    protected void addToList( Object[] c ) {
    	for (Object obj : c) {
    		if (obj instanceof Expression<?>) {
    			Object eval = null;
                try {
                    eval = ((Expression<?>) obj).evaluate(true);
                    
                // TODO -- figure out why eclipse gives compile errors for
                // including the exceptions while mvn gives errors for not
                // including them.
                } catch ( IllegalAccessException e ) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                } catch ( InvocationTargetException e ) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                } catch ( InstantiationException e ) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }
          if ( eval instanceof Collection ) {
            Collection<?> coll = (Collection<?>)eval;
            this.addToList(coll.toArray());
          } else if ( eval != null && eval.getClass().isArray() ) {
            this.addToList((Object[])eval);
          } else if ( eval instanceof Viewable ) {
    			    this.add((Viewable<EmsScriptNode>)eval);
    			} else {
    			    this.add(new Text("" + eval));
    			}
    		} else if ( obj instanceof Viewable ) {
    		    this.add((Viewable<EmsScriptNode>)obj);
    		} else {
    		    // ERROR
    		    Debug.error(true, false, "bad arg to List(Object[]): " + c);
    		}
    	}
    }

    public static Viewable<EmsScriptNode> toViewable( Object obj ) {
        if ( obj instanceof Viewable ) {
            return (Viewable<EmsScriptNode>)obj;
        }
        if (obj instanceof Expression<?>) {
            Object eval = null;
            try {
                eval = ((Expression<?>) obj).evaluate(true);
                
            // TODO -- figure out why eclipse gives compile errors for
            // including the exceptions while mvn gives errors for not
            // including them.
            } catch ( IllegalAccessException e ) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            } catch ( InvocationTargetException e ) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            } catch ( InstantiationException e ) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            }
            return toViewable( eval );
        }
        if ( obj instanceof String ) {
            return new Text( (String)obj );
        }
        return new Evaluate( obj );
    }
    
//    // this works, but is too limited
//    public <V extends Viewable<EmsScriptNode>>  List(V item1, V item2) {
//    	this(Utils.newList(item1, item2));
//    }
    
//    /**
//     * Create a List with a given number of null entries. 
//     * @param initialCapacity
//     * @see java.util.List#List(int)
//     */
//    public List( int initialCapacity ) {
//        super( initialCapacity );
//    }

    /* 
     * <code>
     *       {
     *           "type": "List",
     *           "list": [
     *               [{ //each list item can have multiple things, Table in a list may not be supported
     *                   "type": "Paragraph"/"List"/"Table",
     *                   (see above...)
     *               }, ...], ...
     *           ],
     *           "ordered": true/false
     *       }
     * </code>
     * @returns a JSON object in the format above
     * @see sysml.Viewable#toViewJson()
     */
    @Override
    public JSONObject toViewJson(Date dateTime) {

        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        try {
        	
            json.put("type", "List");
            json.append("list", jsonArray);
            
            for ( Viewable<EmsScriptNode> viewable : this ) {
                if ( viewable != null ) {
                	jsonArray.put( viewable.toViewJson(dateTime) );
                }
            }
            
            json.put("ordered", ordered);


        } catch ( JSONException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return json;
    }

    /* (non-Javadoc)
     * @see sysml.Viewable#getDisplayedElements()
     */
    @Override
    public Collection< EmsScriptNode > getDisplayedElements() {
        TreeSet< EmsScriptNode > elements =
                new TreeSet< EmsScriptNode >( CompareUtils.GenericComparator.instance() );
        for ( Viewable< EmsScriptNode > v : this ) {
            if ( v == null ) continue;
            Collection< EmsScriptNode > moreElements = v.getDisplayedElements();
            if ( !Utils.isNullOrEmpty( moreElements ) ) {
                elements.addAll( moreElements );
            }
        }
        return elements;
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#toString()
     */
    @Override
    public String toString() {
        JSONObject jo = toViewJson(null);
        if ( jo != null ) return NodeUtil.jsonToString( jo );
        return super.toString();
    }

    //@Override
    public boolean prefer( java.util.List< Class > t1,
                           java.util.List< Class > t2 ) {
        return preferences.prefer( t1, t2 );
    }

    //@Override
    public int rank( java.util.List< Class > t ) {
        return preferences.rank( t );
    }
}
