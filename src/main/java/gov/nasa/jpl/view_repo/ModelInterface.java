/**
 * 
 */
package gov.nasa.jpl.view_repo;

import java.util.Collection;

/**
 * A generic interface for talking to models.  Looking to be sufficient for simplified SysML (without UML).
 * REVIEW -- What else might this need to be compatible with other things, like CMIS, OSLC, EMF, etc.  
 */
public interface ModelInterface<O, T, P, N, R> {
	// accessors for class/object/element
	N getName( O object );
	T getType( O object );
	Collection<P> getProperties( O object );
	P getProperty( O object, N propertyName );
	Collection<R> getRelationships( O object );
	R getRelationship( O object, N relationshipName );
	Collection<O> getRelated( O object, N relationshipName );
	
}
