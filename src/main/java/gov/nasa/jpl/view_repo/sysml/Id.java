package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * An implementation of a {@link sysml.view.Id}
 * 
 * @see sysml.view.Value
 * 
 */
public class Id extends ElementReference implements sysml.view.Id<EmsScriptNode> {

    public Id(EmsScriptNode element ) {
        super( element, ElementReference.Attribute.ID );
    }
    
    // TODO -- add other two constructors

//  @Override
//  public JSONObject toViewJson( Date dateTime ) {
//      if ( element == null && object instanceof Expression ) {
//          
//      }
//  }
  
}
