package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.extensions.webscripts.Status;

import sysml.AbstractSystemModel;

// <E, C, T, P, N, I, U, R, V, W, CT>
//public class EmsSystemModel extends AbstractSystemModel< EmsScriptNode, EmsScriptNode, String, ? extends Serializable, String, String, Object, EmsScriptNode, String, String, EmsScriptNode > {
public class EmsSystemModel extends AbstractSystemModel< EmsScriptNode, EmsScriptNode, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode > {

    protected ServiceRegistry services;
    protected EmsScriptNode serviceNode;

    public EmsSystemModel() {
        this( null );
    }

    public EmsSystemModel( ServiceRegistry services ) {
        this.services = ( services == null ? NodeUtil.getServiceRegistry() : services );
        System.out.println("ServiceRegistry = " + this.services);
        if ( this.services == null ) {
            // TODO -- complain!
        } else {
            serviceNode =
                    new EmsScriptNode( NodeUtil.getCompanyHome( this.services )
                                               .getNodeRef(),
                                       this.services );
        }
    }
    
    
    /**
     * @return the nodes for the Alfresco sites on this EMS server. 
     */
    public EmsScriptNode[] getSiteNodes() {
        Collection< EmsScriptNode > sitesNodes = getElementWithName( null, "Sites" );
        if ( sitesNodes == null ) return new EmsScriptNode[]{};
        if ( sitesNodes.isEmpty() ) return new EmsScriptNode[]{};
        EmsScriptNode sitesNode = null;
        for ( EmsScriptNode node : sitesNodes ) {
            if ( node.getType().equals( "cm:folder" ) ) {
                sitesNode = node; // hopefully! REVIEW
                break;
            }
        }
        if ( sitesNode == null ) sitesNode = sitesNodes.iterator().next();
        
        //children = sitesNode.
                return null;
        
    }
    
    /**
     * @return the names of the Alfresco sites on this EMS server. 
     */
    public String[] getSites() {
        // TODO
        return null;
    }
    
    /**
     * @return the URL to the ViewEdtor for a given EMS site name or null if the site does not exist. 
     */
    String getViewEditorUrlForSite( String siteName ){
        // TODO
        return null;
    }
    
    /**
     * @return the URL to this EMS server
     */
    String getEmsUrl() {
        // TODO -- optional?
        return null;
    }
    /**
     * @return the name of this EMS server
     */
    String getEmsName() {
        // TODO -- optional?
        return null;
    }

    
    @Override
    public boolean isDirected( EmsScriptNode relationship ) {
        if ( relationship == null ) return false;
        return services.getDictionaryService()
                       .isSubClass( relationship.getQNameType(),
                                    QName.createQName( "sysml:DirectedRelationship" ) );
    }

    @Override
    public Collection< EmsScriptNode >
            getRelatedElements( EmsScriptNode relationship ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementForRole( EmsScriptNode relationship, String role ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getSource( EmsScriptNode relationship ) {
        
        return getProperty(relationship, Acm.ACM_SOURCE);
    }

    @Override
    public Collection< EmsScriptNode > getTarget( EmsScriptNode relationship ) {
        
        return getProperty(relationship, Acm.ACM_TARGET);
    }

    @Override
    public Class< EmsScriptNode > getElementClass() {
        return EmsScriptNode.class;
    }

    @Override
    public Class< EmsScriptNode > getContextClass() {
        return EmsScriptNode.class;
    }

    @Override
    public Class< EmsScriptNode > getTypeClass() {
        return EmsScriptNode.class;
    }

    @Override
    public Class< EmsScriptNode > getPropertyClass() {
        return EmsScriptNode.class;
    }

    @Override
    public Class< String > getNameClass() {
        return String.class;
    }

    @Override
    public Class< String > getIdentifierClass() {
        return String.class;
    }

    @Override
    public Class< Object > getValueClass() {
        return Object.class;
    }

    @Override
    public Class< EmsScriptNode > getRelationshipClass() {
        return EmsScriptNode.class;
    }

    @Override
    public Class< String > getVersionClass() {
        return String.class;
    }

    @Override
    public Class< String > getWorkspaceClass() {
        return String.class;
    }

    @Override
    public Class< EmsScriptNode > getConstraintClass() {
        return EmsScriptNode.class;
    }

    @Override
    public Class< ? extends EmsScriptNode > getViewClass() {
        return EmsScriptNode.class;
    }

    @Override
    public Class< ? extends EmsScriptNode > getViewpointClass() {
        return EmsScriptNode.class;
    }

    @Override
    public EmsScriptNode createConstraint( Object context ) {
        if ( context instanceof EmsScriptNode ) {
            
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmsScriptNode createElement( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createIdentifier( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createName( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmsScriptNode createProperty( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmsScriptNode createRelationship( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmsScriptNode createType( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmsScriptNode createValue( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createVersion( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmsScriptNode createView( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmsScriptNode createViewpoint( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createWorkspace( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object delete( Object object ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getConstraint( Object context,
                                                      Object specifier ) {
        
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithIdentifier( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getConstraintWithName( Object context,
                                                              String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithRelationship( Object context,
                                           EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithValue( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithVersion( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< EmsScriptNode >
            getConstraintWithViewpoint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintWithWorkspace( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getElement( Object context,
                                                   Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithConstraint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithIdentifier( Object context, String specifier ) {
        // TODO -- need to take into account the context!
        NodeRef element = NodeUtil.findNodeRefById( specifier, services );
        EmsScriptNode emsSN = new EmsScriptNode( element, services );
        return Utils.newList( emsSN );
    }

    @Override
    public Collection< EmsScriptNode > getElementWithName( Object context,
                                                           String specifier ) {
        StringBuffer response = new StringBuffer();
        Status status = new Status();
        // TODO -- need to take into account the context!
        Map< String, EmsScriptNode > elements =
                NodeUtil.searchForElements( specifier, services, response,
                                            status );
        if ( elements != null ) return elements.values();
        return Collections.emptyList();
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< EmsScriptNode >
            getElementWithRelationship( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithValue( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getElementWithVersion( Object context,
                                                              String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithViewpoint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithWorkspace( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< String > getName( Object context ) {
        
    	// Assuming that we can only have EmsScriptNode context:
    	if (context instanceof EmsScriptNode) {
    		
    		EmsScriptNode node = (EmsScriptNode) context;
    		
    		// Note: This returns the sysml:name not the cm:name, which is what we
    		//		 want
    		Object name = node.getProperty(Acm.ACM_NAME);
    		
    		return Utils.asList(name, String.class);
    	}
    	
    	else {
            // TODO -- error????  Are there any other contexts than an EmsScriptNode that would have a property?
            Debug.error("context is not an EmsScriptNode!");
            return null;
        }
        
    }

    @Override
    public Collection< String > getIdentifier( Object context ) {
     
        return null;
    }
    
    /**
     * Attempts to convert propVal to a EmsScriptNode.  If conversion is possible, adds
     * to the passed List.
     * 
     * @param propVal the property to try and convert
     * @param returnList the list of nodes to possibly add to
     */
    private void convertToScriptNode(Object propVal, List<EmsScriptNode> returnList) {
    	    	
       	// The propVal can be a ArrayList<NodeRef>, ArrayList<Object>, NodeRef, or
    	// Object
    	
    	if (propVal != null) {
    	
	 		if (propVal instanceof ArrayList) {
				 			
				// Loop through the arrayList and convert each NodeRef to a EmsScriptNode
				ArrayList<?> propValArray = (ArrayList<?>)propVal;
				for (Object propValNode : propValArray) {
					
					// If its a NodeRef then convert:
					if (propValNode instanceof NodeRef) {
						
						returnList.add(new EmsScriptNode((NodeRef)propValNode, services));
					}
					
					// TODO what do we do for other objects?  For now, nothing....
				}
	
			} // ends if propVal is a ArrayList
			
			else if (propVal instanceof NodeRef) {
				returnList.add(new EmsScriptNode((NodeRef)propVal, services));
			}
	 		
			else if (propVal instanceof String) {
				// Get the corresponding node with a name of the propVal:
				Collection<EmsScriptNode> nodeList = getElementWithName(null, (String)propVal);
				if (!Utils.isNullOrEmpty(nodeList)) {
					returnList.add(nodeList.iterator().next());
				}
			}
			
			else {
				// TODO what do we do for other objects?  For now, nothing....
			}
		
    	}
 
    }

    @Override
    public Collection< EmsScriptNode > getProperty( Object context,
                                                    Object specifier ) {
    	
        ArrayList< EmsScriptNode > allProperties = new ArrayList< EmsScriptNode >();

        // find the specified property inside the context
        if ( context instanceof EmsScriptNode ) {
        	
            EmsScriptNode node = (EmsScriptNode)context;

            if ( specifier == null ) {
                // if no specifier, return all properties
                Map< String, Object > props = node.getProperties();
                if ( props != null ) {
                
                	// Loop through all of returned properties:
                	Collection<Object> propValues = props.values();
                	for (Object propVal : propValues) {
                		
                		// Attempt to convert to a EmsScriptNode and add to the list
                		// to later return if conversion succeeded:
                		convertToScriptNode(propVal, allProperties);

                	} // ends for loop through properties
                }
                
            } // ends if specifies is null
            
            else {
                Object prop = node.getProperty( "" + specifier );
                
        		// Attempt to converted to a EmsScriptNode and add to the list
        		// to later return if conversion succeeded:
                convertToScriptNode(prop, allProperties);

        	}
            
            return allProperties;
        }
        
        if ( context != null ) {
            // TODO -- error????  Are there any other contexts than an EmsScriptNode that would have a property?
            Debug.error("context is not an EmsScriptNode!");
            return null;
        }
        
        // context is null; look for nodes of type Property that match the specifier
        if ( specifier != null ) {
            return getElementWithName( context, "" + specifier );
        }
        
        // context and specifier are both be null
        // REVIEW -- error?
        // Debug.error("context and specifier cannot both be null!");
        // REVIEW -- What about returning all properties?
        Collection< EmsScriptNode > propertyTypes = getTypeWithName( context, "Property" );
        if ( !Utils.isNullOrEmpty( propertyTypes ) ) {
            for ( EmsScriptNode prop : propertyTypes ) {
                allProperties.addAll( getElementWithType( context, propertyTypes.iterator().next() ) );
            }
            return allProperties;
        }
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithConstraint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithIdentifier( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< EmsScriptNode >
            getPropertyWithRelationship( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithValue( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithVersion( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithViewpoint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getPropertyWithWorkspace( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getRelationship( Object context,
                                                        Object specifier ) {
    	
    	// TODO
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithConstraint( Object context,
                                           EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< EmsScriptNode >
            getRelationshipWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithIdentifier( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithName( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< EmsScriptNode >
            getRelationshipWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithValue( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithVersion( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithViewpoint( Object context,
                                          EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getRelationshipWithWorkspace( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Return a 
     */
    @Override
    public Collection< EmsScriptNode >
            getType( Object context, Object specifier ) {
    	    	
    	// TODO ScriptNode getType returns a QName or String, why does he want a collection
    	// of EmsScriptNode?  I think we should change T to String.
    	
    	// Ignoring context b/c it doesnt make sense
    	
    	// Search for all elements with the specified type name:
    	if (specifier instanceof String) {
	        StringBuffer response = new StringBuffer();
	        Status status = new Status();
	        Map< String, EmsScriptNode > elements =
	                NodeUtil.searchForElements( "@sysml\\:type:\"", (String)specifier, services, response,
	                                            status );

	        if ( elements != null ) return elements.values();
    	}
    	
        return Collections.emptyList();
    }
    
    // TODO remove this once we fix getType()
    @Override
   public String getTypeString( Object context, Object specifier ) {
    	
        // TODO finish this, just a partial implementation
    	
 
        if (context instanceof EmsScriptNode) {
        	EmsScriptNode node = (EmsScriptNode) context;
        	
        	return node.getTypeShort();
            
        }
        
        return null;
        
    }

    @Override
    public Collection< EmsScriptNode >
            getTypeWithConstraint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getTypeWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getTypeWithIdentifier( Object context,
                                                              String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getTypeWithName( Object context,
                                                        String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getTypeWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getTypeWithRelationship( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getTypeWithValue( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getTypeWithVersion( Object context,
                                                           String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getTypeWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getTypeWithViewpoint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getTypeWithWorkspace( Object context,
                                                             String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object > getValue( Object context,
    									  Object specifier ) {
    		
    	// Assuming that we can only have EmsScriptNode context:
    	if (context instanceof EmsScriptNode) {
    		
    		EmsScriptNode node = (EmsScriptNode) context;
    		
			// If it is a Property type, then the value is a NodeRef, which
			// we convert to a EmsScriptNode:
			if (node.getTypeShort().equals(Acm.ACM_PROPERTY)) {
				
		    	List<EmsScriptNode> returnList = new ArrayList<EmsScriptNode>();
				Object valueNode = node.getProperty(Acm.ACM_VALUE);
				convertToScriptNode(valueNode , returnList);
				
	    		return Utils.asList(returnList, Object.class);
			}
			
			// Otherwise, return the Object for the value
			else {
			
	    		// If no specifier is supplied:
				if (specifier == null) {
					// TODO what should we do here?
	    		}
				else {
					
					Object valueNode = node.getProperty("" + specifier);
					
					if (valueNode != null) {
						return Utils.newList(valueNode);
					}
				}
    			
			}
    		
    	}
    	
    	else {
            // TODO -- error????  Are there any other contexts than an EmsScriptNode that would have a property?
            Debug.error("context is not an EmsScriptNode!");
            return null;
        }
    	
    	return null;
    	
    }

    @Override
    public Collection< Object >
            getValueWithConstraint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            getValueWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            getValueWithIdentifier( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object > getValueWithName( Object context,
                                                         String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            getValueWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            getValueWithRelationship( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            getValueWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object > getValueWithVersion( Object context,
                                                            String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            getValueWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            getValueWithViewpoint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object > getValueWithWorkspace( Object context,
                                                              String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< String > getVersion( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getView( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getViewpoint( Object context,
                                                     Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< EmsScriptNode >
            getViewpointWithConstraint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithIdentifier( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getViewpointWithName( Object context,
                                                             String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithRelationship( Object context,
                                          EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithValue( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithVersion( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewpointWithWorkspace( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewWithConstraint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getViewWithIdentifier( Object context,
                                                              String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getViewWithName( Object context,
                                                        String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewWithRelationship( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewWithValue( Object context, Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getViewWithVersion( Object context,
                                                           String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViewWithViewpoint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getViewWithWorkspace( Object context,
                                                             String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< String > getWorkspace( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object set( Object object, Object specifier, Object value ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean idsAreWritable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean namesAreWritable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean versionsAreWritable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public EmsScriptNode getDomainConstraint( EmsScriptNode element,
                                              String version, String workspace ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addConstraint( EmsScriptNode constraint, String version,
                               String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addDomainConstraint( EmsScriptNode constraint, String version,
                                     Set< Object > valueDomainSet,
                                     String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public
            void
            addDomainConstraint( EmsScriptNode constraint,
                                 String version,
                                 Pair< Object, Object > valueDomainRange,
                                 String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void relaxDomain( EmsScriptNode constraint, String version,
                             Set< Object > valueDomainSet,
                             String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void
            relaxDomain( EmsScriptNode constraint, String version,
                         Pair< Object, Object > valueDomainRange,
                         String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Collection< EmsScriptNode >
            getConstraintsOfElement( EmsScriptNode element, String version,
                                     String workspace ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getViolatedConstraintsOfElement( EmsScriptNode element,
                                             String version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOptimizationFunction( Method method, Object... arguments ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Number getScore() {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceRegistry getServices() {
        return services;
    }

    @Override
    public boolean fixConstraintViolations( EmsScriptNode element,
                                            String version ) {
        // TODO Auto-generated method stub
        return false;
    }

}
