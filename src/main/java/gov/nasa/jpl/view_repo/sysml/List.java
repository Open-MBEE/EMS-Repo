package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONObject;

import sysml.Viewable;

/**
 * @see sysml.List
 *
 */
public class List extends ArrayList< Viewable< EmsScriptNode > > implements sysml.List< EmsScriptNode > {

    private static final long serialVersionUID = 3954654861037876503L;

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
     * @returns a String in the JSON format above
     * @see sysml.Viewable#toViewJson()
     */
    @Override
    public JSONObject toViewJson() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see sysml.Viewable#getDisplayedElements()
     */
    @Override
    public Collection< EmsScriptNode > getDisplayedElements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        return toViewJson().toString();
    }
}
