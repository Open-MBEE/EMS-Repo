package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import gov.nasa.jpl.view_repo.util.JsonArray;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.JsonObject;

import sysml.view.Viewable;

/**
 * An implementation of a {@link Viewable} list of {@link Viewable}s subclassing
 * {@link ArrayList}.
 * 
 * @see viewable.List
 * 
 */
public class List extends ArrayList< Viewable< EmsScriptNode > > implements sysml.view.List< EmsScriptNode > {

    private static final long serialVersionUID = 3954654861037876503L;
    protected boolean ordered = false;
    
    /**
     * Create an empty List.
     * @see java.util.List#List()
     */
    public List() {
        super();
    }

    /**
     * Create a List and add the {@link Viewable}s in the input {@link Collection}.
     * @param c
     * @see java.util.List#List(Collection)
     */
    public List( Collection< ? extends Viewable< EmsScriptNode > > c ) {
        super( c );
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

    	for (Object obj : c) {
    		if (obj instanceof Expression<?>) {
    			Object eval = ((Expression<?>) obj).evaluate(true);
    			this.add((Viewable<EmsScriptNode>)eval);
    		}
    	}
    }
    
//    // this works, but is too limited
//    public <V extends Viewable<EmsScriptNode>>  List(V item1, V item2) {
//    	this(Utils.newList(item1, item2));
//    }
    
    /**
     * Create a List with a given number of null entries. 
     * @param initialCapacity
     * @see java.util.List#List(int)
     */
    public List( int initialCapacity ) {
        super( initialCapacity );
    }

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
    public JsonObject toViewJson() {

        JsonObject json = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        try {
        	
            json.put("type", "List");
            json.append("list", jsonArray);
            
            for ( Viewable<EmsScriptNode> viewable : this ) {
                if ( viewable != null ) {
                	jsonArray.put( viewable.toViewJson() );
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
        JsonObject jo = toViewJson();
        if ( jo != null ) return jo.toString();
        return super.toString();
    }
}
