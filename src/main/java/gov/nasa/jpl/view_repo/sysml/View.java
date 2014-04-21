/**
 * 
 */
package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.Collection;

import sysml.Viewable;

/**
 * A View embeds {@link Viewable}s and itself is a {@link Viewable}.
 *
 */
public class View extends List implements sysml.View< EmsScriptNode > {

    private static final long serialVersionUID = -7618965504816221446L;

    /**
     * @see List#List() 
     */
    public View() {
        super();
    }

    /**
     * Create a View and add the {@link Viewable}s in the input {@link Collection}.
     * @param c
     * @see List#List(Collection)
     */
    public View( Collection< ? extends Viewable< EmsScriptNode >> c ) {
        super( c );
    }

    /**
     * @param initialCapacity
     */
    public View( int initialCapacity ) {
        super( initialCapacity );
    }

    /* (non-Javadoc)
     * @see sysml.View#getChildViews()
     */
    @Override
    public Collection< sysml.View< EmsScriptNode > > getChildViews() {
        // TODO Auto-generated method stub
        return null;
    }

}
