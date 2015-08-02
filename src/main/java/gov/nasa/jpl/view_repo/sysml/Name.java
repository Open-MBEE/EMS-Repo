package gov.nasa.jpl.view_repo.sysml;

import java.util.Date;

import org.json.JSONObject;

import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * An implementation of a {@link sysml.view.Name}
 * 
 * @see sysml.view.Name
 * 
 */
public class Name extends ElementReference implements sysml.view.Name<EmsScriptNode> {

    boolean editable = true;
    Object object = null;
    
	public Name( EmsScriptNode element ) {
	    super( element, ElementReference.Attribute.NAME );
	}

//    public Name( String id ) {
//        super( id, ElementReference.Attribute.NAME );
//    }
//
    public Name( Object object ) {
        super( object, ElementReference.Attribute.NAME );
    }
    
//    @Override
//    public JSONObject toViewJson( Date dateTime ) {
//        if ( element == null && object instanceof Expression ) {
//            
//        }
//    }
    
}
