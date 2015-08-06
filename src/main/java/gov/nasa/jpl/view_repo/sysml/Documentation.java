package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * An implementation of a {@link sysml.view.Documentation}
 * 
 * @see sysml.view.Documentation
 * 
 */
public class Documentation extends ElementReference implements sysml.view.Documentation<EmsScriptNode> {

    public Documentation(EmsScriptNode element ) {
        super( element, ElementReference.Attribute.DOCUMENTATION );
    }
    
    // TODO -- add other two constructors
    
}
