package gov.nasa.jpl.view_repo.sysml;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * An implementation of a {@link sysml.view.Name}
 * 
 * @see sysml.view.Name
 * 
 */
public class Name extends ElementReference implements sysml.view.Name<EmsScriptNode> {

	public Name( EmsScriptNode element ) {
	    super( element, ElementReference.Attribute.NAME );
	}

}
