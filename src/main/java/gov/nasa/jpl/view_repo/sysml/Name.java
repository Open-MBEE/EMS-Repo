package gov.nasa.jpl.view_repo.sysml;

import java.util.List;

import gov.nasa.jpl.mbee.util.HasPreference;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * An implementation of a {@link sysml.view.Name}
 * 
 * @see sysml.view.Name
 * 
 */
public class Name extends ElementReference implements sysml.view.Name<EmsScriptNode>, HasPreference<List< Class > > {

    /**
     * The preferred constructor arguments.
     */
    protected static final HasPreference.Helper< List< Class > > preferences =
            new HasPreference.Helper< List< Class > >( (List< List< Class > >)Utils.newList( (List< Class >)Utils.newList( (Class)EmsScriptNode.class ),
                                                                           (List< Class >)Utils.newList( (Class)String.class ) ) );
    boolean editable = true;
    Object object = null;

    public Name( EmsScriptNode element ) {
        super( element, ElementReference.Attribute.NAME );
    }

//	public Name( HasName<String> named ) {
//	    super( named, ElementReference.Attribute.NAME );
//	}

    public Name( String id ) {
        super( id, ElementReference.Attribute.NAME );
    }
//
//    public Name( Object object ) {
//        super( object, ElementReference.Attribute.NAME );
//    }

    @Override
    public boolean prefer( List< Class > t1, List< Class > t2 ) {
        return preferences.prefer( t1, t2 );
    }


    
//    @Override
//    public JSONObject toViewJson( Date dateTime ) {
//        if ( element == null && object instanceof Expression ) {
//            
//        }
//    }
    
}
