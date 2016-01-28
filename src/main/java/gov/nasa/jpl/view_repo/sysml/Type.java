package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * An implementation of a {@link sysml.view.Id}
 * 
 * @see sysml.view.Value
 * 
 */
public class Type extends ElementReference implements sysml.view.Type<EmsScriptNode> {

    public Type(EmsScriptNode element ) {
        super( element, ElementReference.Attribute.ID );
    }
    
    // TODO -- add other two constructors

}
