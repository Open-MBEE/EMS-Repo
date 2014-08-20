package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * An implementation of a {@link sysml.view.Value}
 * 
 * @see sysml.view.Value
 * 
 */
public class Value extends ElementReference implements sysml.view.Value<EmsScriptNode> {

    public Value(EmsScriptNode element ) {
        super( element, ElementReference.Attribute.VALUE );
    }

}
