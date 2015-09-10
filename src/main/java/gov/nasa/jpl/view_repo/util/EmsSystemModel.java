package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.ae.event.Call;
import gov.nasa.jpl.ae.event.FunctionCall;
import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.HasId;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Level;
import org.springframework.extensions.webscripts.Status;

import sysml.AbstractSystemModel;
import sysml.SystemModel;
import sysml.SystemModel.ModelItem;

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
        if (Debug.isOn()) System.out.println("ServiceRegistry = " + this.services);
        if ( this.services == null ) {
            // REVIEW -- complain?
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

        return Utils.toArrayOfType( sitesNodes, EmsScriptNode.class );
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
                                    QName.createQName( Acm.ACM_DIRECTED_RELATIONSHIP ) );
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
    
    public Collection< EmsScriptNode > getOwnedChildren( Object context ) {
        return getOwnedElements( context );
    }
    public Collection< EmsScriptNode > getOwnedElements( Object context ) {
        List<EmsScriptNode> list = new ArrayList< EmsScriptNode >();
        if ( context instanceof EmsScriptNode ) {
            EmsScriptNode n = (EmsScriptNode)context;
            List< NodeRef > c = n.getOwnedChildren( false, null, n.getWorkspace() );
            if ( c != null ) {
                list = EmsScriptNode.toEmsScriptNodeList( c );
            }
        }
        return list;
    }


    @Override
    public Collection< EmsScriptNode > getElement( Object context,
                                                   Object specifier ) {
        return getElement( context, specifier, SystemModel.ModelItem.NAME );
    }
    public Collection< EmsScriptNode > getElement( Object context,
                                                   Object specifier,
                                                   SystemModel.ModelItem itemType) {
        return getElement( context, specifier, itemType, null );
        
    }
    public Collection< EmsScriptNode > getElement( Object context,
                                                   Object specifier,
                                                   ModelItem itemType,
                                                   Date dateTime ) {
        // HERE!!!  TODO -- call this from other getElementWith????()
        StringBuffer response = new StringBuffer();
        Status status = new Status();
        // TODO -- need to take into account the context!
//        Map< String, EmsScriptNode > elements =
//                NodeUtil.searchForElements( specifier, true, null, dateTime,
//                                            services, response, status );
//      if ( elements != null ) return elements.values();
//      return Collections.emptyList();
        
        boolean ignoreWorkspace = false;
        WorkspaceNode workspace = null;
        
        // Convert context from NodeRef to EmsScriptNode or WorkspaceNode
        if ( context instanceof NodeRef ) {
            EmsScriptNode ctxt = new EmsScriptNode( (NodeRef)context, getServices(),
                                                    response, status );
            if ( ctxt.hasAspect( "Workspace" ) ) {
                context = new WorkspaceNode( (NodeRef)context, getServices(),
                                             response, status );
            } else {
                context = ctxt;
            }
        }
        
        // Set workspace from context.
        if ( context instanceof WorkspaceNode ) {
            workspace = (WorkspaceNode)context;
        } else if ( context instanceof EmsScriptNode ) {
            workspace = ( (EmsScriptNode)context ).getWorkspace();
        } else ignoreWorkspace = true;
        
        // Treat the context as the set to search (or to call getElement() on each).
        Object[] arr = null;
        if ( context instanceof Collection ) {
            arr = ( (Collection<?>)context ).toArray();
        } else if ( context != null && context.getClass().isArray() ) {
            arr = (Object[])context;
        }
        // HERE!!!  TODO!!!
        if ( arr != null && arr.length > 0 ) {
            if ( arr[0] instanceof NodeRef ) {
                
            } else if ( arr[0] instanceof EmsScriptNode ) {
                
            }
        }
        
        // Try to get elements with specifier as name.
        ArrayList< NodeRef > refs = null;
        
        switch ( itemType ) {
            case NAME:
                refs = NodeUtil.findNodeRefsBySysmlName( "" + specifier, ignoreWorkspace,
                                                         workspace, dateTime,
                                                         getServices(), false, false );
                if ( !Utils.isNullOrEmpty( refs ) ) break;
            case IDENTIFIER:
                // Try to get elements with specifier as id.
                refs =
                    NodeUtil.findNodeRefsById( "" + specifier, ignoreWorkspace,
                                               workspace, dateTime, getServices(),
                                               false, false );
                break;
            case PROPERTY:
                if ( specifier instanceof String ) {
                    refs = NodeUtil.findNodeRefsBySysmlName( "" + specifier, ignoreWorkspace,
                                                             workspace, dateTime,
                                                             getServices(), false, false );
                } else if ( specifier instanceof EmsScriptNode ) {
                    return getElementWithProperty( context, (EmsScriptNode)specifier );
                }
                break;
            case TYPE:
            case VALUE:
            case ELEMENT:
            case VERSION:
            case CONSTRAINT:
            case RELATIONSHIP:
            case VIEW:
            case VIEWPOINT:
            case WORKSPACE:
            default:
                Debug.error( true, false,
                             "ERROR! Not yet supporting query type " + itemType
                                     + " calling EmsSystemModel.getElement("
                                     + context + ", " + specifier + ", "
                                     + itemType + ", " + dateTime + ")" );
        }
        
        // HERE!!! (Look for HERE above.)
        // What other kinds of specifier should we look for? ...?

        //filter( elements, methodCall, indexOfElementArgument );

        
        if ( Utils.isNullOrEmpty( refs ) ) return Collections.emptyList();
        if ( refs.size() > 1 && context != null ) {//instanceof EmsScriptNode && !(context instanceof WorkspaceNode) ) {
            ArrayList< EmsScriptNode > childNodes = new ArrayList< EmsScriptNode >();
            for ( NodeRef ref : refs ) {
                EmsScriptNode node = new EmsScriptNode( ref, getServices(), response, status );
                EmsScriptNode owner = node.getOwningParent( dateTime, workspace, false );
                if ( context.equals( owner )
                     || ( context instanceof WorkspaceNode && context.equals( node.getWorkspace() ) ) ) {
                    childNodes.add( node );
                }
            }
            if ( childNodes.size() > 0 ) return childNodes;
        }
        List< EmsScriptNode > list = EmsScriptNode.toEmsScriptNodeList( refs, getServices(), response, status );
        //System.out.println("getElementWithName(" + context + ", " + specifier + ", " + dateTime + ") = " + list);
        return list;
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
        NodeRef element = NodeUtil.findNodeRefById( specifier, true, null, null, services, false );
        EmsScriptNode emsSN = new EmsScriptNode( element, services );
        ArrayList< EmsScriptNode > list = Utils.newList( emsSN );
        //System.out.println("getElementWithIdentifier(" + context + ", " + specifier + ") = " + list);
        return list;
    }

    @Override
    public Collection< EmsScriptNode > getElementWithName( Object context,
                                                           String specifier ) {
        return getElementWithName(context, specifier, null);
    }
    
    public Collection< EmsScriptNode > getElementWithName( Object context,
                                                           String specifier,
                                                           Date dateTime ) {
        if ( true ) {
            return getElement( context, specifier, SystemModel.ModelItem.NAME );
        }
        StringBuffer response = new StringBuffer();
        Status status = new Status();
        // TODO -- need to take into account the context!
//        Map< String, EmsScriptNode > elements =
//                NodeUtil.searchForElements( specifier, true, null, dateTime,
//                                            services, response, status );
//      if ( elements != null ) return elements.values();
//      return Collections.emptyList();
        
        boolean ignoreWorkspace = false;
        WorkspaceNode workspace = null;
        if ( context instanceof NodeRef ) {
            EmsScriptNode ctxt = new EmsScriptNode( (NodeRef)context, getServices(),
                                                    response, status );
            if ( ctxt.hasAspect( "Workspace" ) ) {
                context = new WorkspaceNode( (NodeRef)context, getServices(),
                                             response, status );
            } else {
                context = ctxt;
            }
        }
        if ( context instanceof WorkspaceNode ) {
            workspace = (WorkspaceNode)context;
        } else if ( context instanceof EmsScriptNode ) {
            workspace = ( (EmsScriptNode)context ).getWorkspace();
        } else ignoreWorkspace = true;
        ArrayList< NodeRef > refs =
            NodeUtil.findNodeRefsBySysmlName( specifier, ignoreWorkspace,
                                              workspace, dateTime,
                                              getServices(), false, false );
        if ( Utils.isNullOrEmpty( refs ) ) {
            refs =
                NodeUtil.findNodeRefsById( specifier, ignoreWorkspace,
                                           workspace, dateTime, getServices(),
                                           false, false );
        }
        if ( Utils.isNullOrEmpty( refs ) ) return Collections.emptyList();
        if ( refs.size() > 1 && context != null ) {//instanceof EmsScriptNode && !(context instanceof WorkspaceNode) ) {
            ArrayList< EmsScriptNode > childNodes = new ArrayList< EmsScriptNode >();
            for ( NodeRef ref : refs ) {
                EmsScriptNode node = new EmsScriptNode( ref, getServices(), response, status );
                EmsScriptNode owner = node.getOwningParent( dateTime, workspace, false );
                if ( context.equals( owner )
                     || ( context instanceof WorkspaceNode && context.equals( node.getWorkspace() ) ) ) {
                    childNodes.add( node );
                }
            }
            if ( childNodes.size() > 0 ) return childNodes;
        }
        List< EmsScriptNode > list = EmsScriptNode.toEmsScriptNodeList( refs, getServices(), response, status );
        //System.out.println("getElementWithName(" + context + ", " + specifier + ", " + dateTime + ") = " + list);
        return list;
    }

    @Override
    public Collection< EmsScriptNode >
            getElementWithProperty( Object context, EmsScriptNode specifier ) {
        Date date = null;
        WorkspaceNode ws = null;
        if ( context instanceof Date ) {
            date = (Date)context;
        } else if ( context instanceof WorkspaceNode ) {
            ws = (WorkspaceNode)context;
        }
        EmsScriptNode n = specifier.getOwningParent( date, ws, false );
        if ( ws != null ) {
            n = NodeUtil.findScriptNodeById( n.getSysmlId(), ws, date, false,
                                             getServices(), null );
        }
        return Utils.newList( n );
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
    public String getIdentifier( Object context ) {
        if ( context == null ) return null;
        if ( context instanceof HasId ) {
            return "" + ((HasId<?>)context).getId();
        }
        if ( context instanceof EmsScriptNode ) {
            return ( (EmsScriptNode)context ).getSysmlId();
        }
        if ( context instanceof NodeRef ) {
            EmsScriptNode node = new EmsScriptNode( (NodeRef)context, getServices() );
            return getIdentifier( node );
        }
        // Hunt for id using reflection
        Object o = ClassUtils.getId( context );
        if ( o != null ) return o.toString();

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

        Object mySpecifier = specifier;
        // Convert specifier to add ACM type, ie prepend "sysml:":
        Map<String, String> convertMap = Acm.getJSON2ACM();
        if (specifier instanceof String && convertMap.containsKey(specifier)) {
        	 mySpecifier = convertMap.get(specifier);
        }

        // find the specified property inside the context
        if ( context instanceof Collection && ((Collection<?>)context).size() == 1 ) {
            context = ((Collection<?>)context).iterator().next();
        }
        if ( context instanceof EmsScriptNode ) {

            EmsScriptNode node = (EmsScriptNode)context;

            // Look for Properties with specifier as name and
            // context as owner.
            Collection< EmsScriptNode > elements =
                    getElementWithName( context, "" + specifier );
            
            Date date = null;
            WorkspaceNode ws = null;
            if ( context instanceof Date ) {
                date = (Date)context;
            } else if ( context instanceof WorkspaceNode ) {
                ws = (WorkspaceNode)context;
            }
            
            for ( EmsScriptNode n : new ArrayList<EmsScriptNode>(elements) ) {
                if ( context instanceof WorkspaceNode ) {
                    if ( !context.equals( n.getWorkspace() ) ) {
                        elements.remove( n );
                    }
                } else if (!context.equals( n.getOwningParent( date, ws, false ) ) ) {
                    elements.remove( n );
                }
            }
            if ( elements.size() > 0 ) {
                //System.out.println("\ngetProperty(" + context + ", " + specifier + ") = " + elements);
                return elements;
            }
            
            // The property is not a separate Property element, so try and get a
            // meta-data property value.
            if ( mySpecifier == null ) {
                // if no specifier, return all properties
                // TODO need date/workspace
                Map< String, Object > props = node.getNodeRefProperties(null, node.getWorkspace());
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
                // TODO need date/workspace
                Object prop = node.getNodeRefProperty( "" + mySpecifier, null, node.getWorkspace() );

        		// Attempt to converted to a EmsScriptNode and add to the list
        		// to later return if conversion succeeded:
                convertToScriptNode(prop, allProperties);

        	}

            //System.out.println("\ngetProperty(" + context + ", " + specifier + ") = allProperties = " + allProperties);
            return allProperties;
        }

        if ( context != null ) {
            // TODO -- error????  Are there any other contexts than an EmsScriptNode that would have a property?
            Debug.error("context is not an EmsScriptNode!  " + context );
            //System.out.println("getProperty(" + context + ", " + specifier + ") = null");
            return null;
        }

        // context is null; look for nodes of type Property that match the specifier
        if ( mySpecifier != null ) {
            Collection< EmsScriptNode > e =getElementWithName( context, "" + mySpecifier );
            //System.out.println("\ngetProperty(" + context + ", " + specifier + ") = getElementWithName(" + context + ", " + specifier + ") = " + e);
            return e;
        }

        // context and specifier are both be null
        // REVIEW -- error?
        // Debug.error("context and specifier cannot both be null!");
        // REVIEW -- What about returning all properties?
        Collection< EmsScriptNode > propertyTypes = getTypeWithName( context, "Property" );
        if ( !Utils.isNullOrEmpty( propertyTypes ) ) {
            for ( EmsScriptNode prop : propertyTypes ) {
                allProperties.addAll( getElementWithType( context, prop ) );
            }
            //System.out.println("\ngetProperty(" + context + ", " + specifier + ") = allProperties2 = " + allProperties);
            return allProperties;
        }
        //System.out.println("\ngetProperty(" + context + ", " + specifier + ") = null2");
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

    public Collection< EmsScriptNode > getPropertyWithTypeName( Object context, String specifier ) {
        ArrayList< EmsScriptNode > nodes = new ArrayList< EmsScriptNode >();
        Collection< EmsScriptNode > list;
        if ( context instanceof Collection ) {
            Collection<?> coll = (Collection<?>)context;
            for ( Object o : coll ) {
                nodes.addAll( getPropertyWithTypeName( o , specifier ) );
            }
        } else if ( specifier != null && context instanceof EmsScriptNode ) {
            Collection< EmsScriptNode > results = getProperty( context, null );
            if ( results != null ) {
                for ( EmsScriptNode n : results ) {
                    String type = getTypeString( n, null );
                    if ( specifier.equals( type ) ) {//|| type.contains(getElementWithName( context, specifier ))) {
                        nodes.add( n );
                    }
                }
            }
        } else if ( specifier == null ) {
            list = getProperty(context, null);
            //System.out.println("getPropertyWithTypeName(" + context + ", " + specifier  + ") = " + list);
            return list;
        }
        // Remaining case is specifier != nil && !(context instanceof EmsScriptNode)
        //System.out.println("getPropertyWithTypeName(" + context + ", " + specifier + ") = " + nodes);
        return nodes;
    }
    @Override
    public Collection< EmsScriptNode >
            getPropertyWithType( Object context, EmsScriptNode specifier ) {
        ArrayList< EmsScriptNode > nodes = new ArrayList< EmsScriptNode >();
        if ( specifier != null ) {
            Collection< String > typeName = getName( specifier );
            if ( typeName != null ) {
                for ( String name : typeName ) {
                    Collection< EmsScriptNode > result =
                            getPropertyWithTypeName( context, name );
                    if ( result != null ) nodes.addAll( result );
                }
            }
            return nodes;
        } else {
            return getProperty(context, null);
        }
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

    	// TODO see EmsScriptNode.getConnectedNodes(), as a lot of this code can
        //      be used for this method.
        
        List< EmsScriptNode > relationships = null;
        
        if ( context instanceof EmsScriptNode ) {
            String relType = null;
            String relName = null;
            String relId = null;
            List<String> typeNames = new ArrayList< String >();
            if ( specifier instanceof String ) {
                relType = (String)specifier;
                if ( !Utils.isNullOrEmpty( relType ) ) typeNames.add(relType);
            } else {
                if ( specifier instanceof EmsScriptNode ) {
                    EmsScriptNode s = (EmsScriptNode)specifier;
                    relName = s.getSysmlName();
                    if ( !Utils.isNullOrEmpty( relName ) ) typeNames.add(relName);
                    relId = s.getSysmlId();
                    if ( !Utils.isNullOrEmpty( relId ) ) typeNames.add(relId);
                }
            }
            EmsScriptNode n = (EmsScriptNode)context;
            ArrayList< NodeRef > refs = null;
            if ( Utils.isNullOrEmpty( typeNames ) ) {
                refs = n.getConnectedNodes( null, n.getWorkspace(), null );
            } else {
                for ( String type : typeNames ) {
                    refs = n.getConnectedNodes( null, n.getWorkspace(), type );
                    if ( !Utils.isNullOrEmpty( refs ) ) break;
                }
            }
            if ( !Utils.isNullOrEmpty( refs ) ) {
                relationships = n.toEmsScriptNodeList( refs );
            }
        }
        
        return relationships;
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
     * Get matching types
     * <p>
     * Examples:
     * <ul>
     * <li>getType(elementA, "typeX") returns the types of elementA whose name
     * or ID is "typeX."
     * <li>getType(packageB, "typeX") returns the types located inside packageB
     * whose name or id is "typeX."
     * <li>getType(myWorkspace, "typeX") returns the types whose names or IDs
     * are "typeX" for myWorkspace.
     * </ul>
     *
     * @param context
     *            the element whose type is sought or a location as a package or
     *            workspace within which the type is to be found
     * @param specifier
     *            the ID, name, version, workspace, etc. for the type element
     * @return type elements that match any interpretation of the specifier for
     *         any interpretation of the context or an empty list if there are
     *         no such types
     * @see sysml.SystemModel#getType(java.lang.Object, java.lang.Object)
     */
    @Override
    public Collection< EmsScriptNode >
            getType( Object context, Object specifier ) {

        // TODO -- the code below is relevant to getElementWithType(), not getType().

    	// TODO ScriptNode getType returns a QName or String, why does he want a collection
    	// of EmsScriptNode?  I think we should change T to String.

//    	// Ignoring context b/c it doesnt make sense
//
//        if ( context != null && specifier == null ) {
//            EmsScriptNode node = (EmsScriptNode)context;
//            String typeName = node.getTypeName();
//            EmsScriptNode typeNode =
//                NodeUtil.findScriptNodeById( typeName, null, null, false,
//                                             getServices(), node.getResponse() );
//            if ( typeNode != null ) {
//                System.out.println( "getType("+ node.getSysmlName() + ") = " + typeNode );
//                return Utils.newList( typeNode );
//            }
//        }
        WorkspaceNode ws = (context instanceof WorkspaceNode) ? (WorkspaceNode)context : null;
        Date dateTime = (context instanceof Date) ? (Date)context : null;
        
    	// Search for all elements with the specified type name:
    	if (specifier instanceof String) {
//	        StringBuffer response = new StringBuffer();  
//	        Status status = new Status();
//	        Map< String, EmsScriptNode > elements =
//	                NodeUtil.searchForElements( "@sysml\\:type:\"", (String)specifier, services, response,
//	                                            status );
////            NodeUtil.searchForElements( "TYPE:\"", (String)specifier, services, response,
////                                        status );
//
//	        if ( elements != null && !elements.isEmpty()) return elements.values();

//	        if ( elements == null ) elements = new LinkedHashMap<String, EmsScriptNode>();

	        Collection< EmsScriptNode > elementColl = null;
	        try {
//	        		elementColl = NodeUtil.luceneSearchElements( "ASPECT:\"sysml:" + specifier + "\"" );
//Debug.error( true, false, "NodeUtil.findNodeRefsByType( " + (String)specifier + ", SearchType.ASPECT.prefix, false, ws, dateTime, false, true, getServices(), false, null )");
	                ArrayList< NodeRef > refs = NodeUtil.findNodeRefsByType( (String)specifier, SearchType.ASPECT.prefix, false, ws, dateTime, false, true, getServices(), false, null );
	                elementColl = EmsScriptNode.toEmsScriptNodeList( refs, getServices(), null, null );
	        } catch (Exception e) {
	        		// if lucene query fails, most likely due to non-existent aspect, we should look for type now
	        		try {
//Debug.error( true, false, "NodeUtil.luceneSearchElements( \"TYPE:\\\"sysml:" + specifier + "\\\" )");
	        			elementColl = NodeUtil.luceneSearchElements( "TYPE:\"sysml:" + specifier + "\"");
	        		} catch (Exception ee) {
	        			// do nothing
	        		}
	        }
//	        for ( EmsScriptNode e : elementColl ) {
//	            elements.put( e.getId(), e );
//	        }
            if ( elementColl != null && !elementColl.isEmpty()) {
            		return elementColl;
            }

    	}

        return Collections.emptyList();
    }

    // TODO remove this once we fix getType()
    @Override
   public String getTypeString( Object context, Object specifier ) {

        // TODO finish this, just a partial implementation

        if (context instanceof EmsScriptNode) {
        	EmsScriptNode node = (EmsScriptNode) context;
        	return node.getTypeName();
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

    public Object getAlfrescoProperty( EmsScriptNode node, String acmPropertyName,
                                       boolean recursiveGetValueOfNodeRefs ) {
        Object result = null;
        if ( Acm.JSON_NODEREFS.contains( acmPropertyName ) ) {
            result = node.getNodeRefProperty(acmPropertyName, null, node.getWorkspace());
            if ( result instanceof Collection ) {
                Collection<NodeRef> valueNodes = (Collection<NodeRef>)result;
                ArrayList< Object > resultList = new ArrayList<Object>();
                result = resultList;
                for ( NodeRef v : valueNodes  ) {
                    EmsScriptNode n = new EmsScriptNode( (NodeRef)v, getServices() );
                    if ( recursiveGetValueOfNodeRefs ) {
                        Object val = getValueAsObject( n, null );
                        resultList.add( val );
                    } else {
                        resultList.add( n );
                    }
                }
            } else if ( result instanceof NodeRef ) {
                result = new EmsScriptNode( (NodeRef)result, getServices() );
            }
        } else {
            result = node.getProperty( acmPropertyName );
        }
        return result;
//
//        if ( Acm.JSON_NODEREFS.contains( acmPropName ) ) {
//            value = node.getNodeRefProperty(acmPropName, null, node.getWorkspace());
//            for ( NodeRef v : valueNodes ) {
//                EmsScriptNode n = new EmsScriptNode( (NodeRef)v, getServices() );
//                Collection< Object > vals = getValue( n, mySpecifier );
//                resultList.add( vals );
//            }
//        } else {
//            value = node.getProperty(valueType);
//        }
    }
    
    public Object getValueOfValueSpec( EmsScriptNode node,
                                       boolean recursiveGetValueOfNodeRefs ) {
        Object result = null;
        
        String type = node.getTypeName();
        if ( Acm.getJSON2ACM().containsKey( type ) ) {
            type = Acm.getJSON2ACM().get( type );
        }
        if ( type == null || !Acm.VALUE_Of_VALUESPEC.containsKey( type ) ) {
            return null;
        }
        String valuePropName = Acm.VALUE_Of_VALUESPEC.get( type );
        result = getAlfrescoProperty( node, valuePropName, recursiveGetValueOfNodeRefs );

//        if ( node.hasOrInheritsAspect( Acm.ACM_LITERAL_STRING ) ) {
//            result = node.getProperty( Acm.ACM_STRING );
//        } else if ( node.hasOrInheritsAspect( Acm.ACM_LITERAL_INTEGER ) ) { 
//            result = node.getProperty( Acm.ACM_INTEGER );
//        } else if ( node.hasOrInheritsAspect( Acm.ACM_LITERAL_REAL ) ) { 
//            result = node.getProperty( Acm.ACM_DOUBLE );
//        } else if ( node.hasOrInheritsAspect( Acm.ACM_LITERAL_BOOLEAN ) ) { 
//            result = node.getProperty( Acm.ACM_BOOLEAN );
//        }
        return result;
    }
    
    @Override
    public Collection< Object > getValue( Object context,
                                          Object specifier ) {
        Object o = getValueAsObject( context, specifier );
        if ( o == null ) return null;
        if ( o instanceof Collection ) return (Collection< Object >)o;
        if ( o.getClass().isArray() ) {
            Object[] arr = (Object[])o;
            List< Object > list = Arrays.asList( arr );
            return list;
        }
//        if ( o instanceof Map ) {
//            return Arrays.asList( ((Map<?,?>)o).entrySet().toArray() );
//        }
        return Utils.newList( o );
    }
    
    public Object getValueAsObject( Object context,
                                    Object specifier ) {
        // TODO need the workspace, time

        Object mySpecifier = specifier;
        // Convert specifier to add ACM type, ie prepend "sysml:":
        Map<String, String> convertMap = Acm.getJSON2ACM();
        if (specifier instanceof String && convertMap.containsKey(specifier)) {
             mySpecifier = convertMap.get(specifier);
        }
        
        Object result = getValueAsObject( context );

        if ( specifier != null && context instanceof EmsScriptNode ) {
            EmsScriptNode node = (EmsScriptNode)context;
            if ( result == null ) {
                result = node.getNodeRefProperty("" + mySpecifier, null, node.getWorkspace());
            }
        }
        
        return result;
    }
    public Object getValueAsObject( Object context ) {//,

        ArrayList< Object > resultList = new ArrayList< Object >();
        
        // Process the context as a collection of contexts.
        if ( context instanceof Collection ) {
            Collection< ? > coll = ((Collection<?>)context);
            if ( coll.size() == 0 ) return resultList;
            // replace the context with a single value in its Collection
            if ( coll.size() == 1 ) {
                context = coll.iterator().next();
            } else {
                // recursively call getValue() on each element of the context
                for ( Object o : coll ) {
                    Object val = getValueAsObject( o );
                    resultList.add( val );
                }
                return resultList;
            }
        }
    	// Assuming that we can only have EmsScriptNode context:
    	if (context instanceof EmsScriptNode) {

    		EmsScriptNode node = (EmsScriptNode) context;

            Collection<Object> returnList = new ArrayList<Object>();

			// If it is a Property type, then the value is a NodeRef, which
			// we convert to a EmsScriptNode:
            String nodeType = node.getTypeName();
            if ( Acm.getJSON2ACM().containsKey( nodeType ) ) {
                nodeType = Acm.getJSON2ACM().get( nodeType );
            }
            if ( Acm.VALUE_OF_TYPE.keySet().contains( nodeType ) ) {
                Object value = getAlfrescoProperty( node, nodeType, true );
                return value;
    		}
            if ( Acm.VALUESPEC_ASPECTS.contains( nodeType ) ) {
                Object value = getValueOfValueSpec( node, false );
                return value;
//                // check to see if all results were of single values
//                boolean allSingleOrEmpty = true;
//                for ( Object o : resultList ) {
//                    if ( o != null ) {
//                        if ( o instanceof Collection ) {
//                            if ( ((Collection<?>)o).size() > 1 ) {
//                                allSingleOrEmpty = false;
//                                break;
//                            }
//                        } else {
//                            allSingleOrEmpty = false;
//                            break;
//                        }
//                    }
//                }
//                // if all single values, flatten
//                if ( allSingleOrEmpty ) {
//                    ArrayList< Object > oldResults = resultList;
//                    resultList = new ArrayList< Object >();
//                    for ( Object o : oldResults ) {
//                        if ( o == null ) {
//                            resultList.add( null );
//                        } else if ( o instanceof Collection ) {
//                            if ( ((Collection<?>)o).isEmpty() ) {
//                                resultList.add( null );
//                } else {
//                                resultList.add( ((Collection<?>)o).iterator().next() );
//                            }
//                        } else {
//                            // should not be possible to get here
//                            assert( false );
//                        }
//                    }
//                }
//				// TODO -- check specifier
//                return resultList;

			// Otherwise, return the Object for the value
    		} else {

	    		// If no specifier is supplied:
//				if (mySpecifier == null) {
//					// TODO what should we do here?
//	    		}
//				else {
//
//					Object valueNode = node.getNodeRefProperty("" + mySpecifier, null, node.getWorkspace());
//
//					if (valueNode != null) {
//						return Utils.newList(valueNode);
//					}
//				}

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

    /**
     * Set the value for the passed node to the passed value
     *
     * @param node
     * @param value
     */
    public < T extends Serializable > void setValue(EmsScriptNode node, T value) {

    	if (node == null || value == null) {
            Debug.error("setValue(): passed node or value is null!");
    	}
    	else {
	    	String type = getTypeString(node, null);

	    	if (type == null) {
	            Debug.error("setValue(): type for the passed node is null!");
	    	}
	    	else {
	    	    String acmType = Acm.getJSON2ACM().get( type );
	    	    if ( acmType == null ) acmType = type;
	    	    Set< String > valuePropNames = Acm.TYPES_WITH_VALUESPEC.get( acmType );
	    	    if ( !Utils.isNullOrEmpty( valuePropNames ) ) {
	    	        boolean found = false;
	    	        if ( valuePropNames.size() > 1 ) {
	    	            if ( valuePropNames.contains( Acm.ACM_VALUE ) ) {
	    	                acmType = Acm.ACM_VALUE;
	    	                found = true;
	    	            } else {
                            Debug.error( "setValue(): unclear which owned value spec property "
                                         + valuePropNames
                                         + " is to be set to "
                                         + value + ". Picking first by default!" );
	    	            }
	    	        }
	    	        if ( !found ) acmType = valuePropNames.iterator().next();
//                    Object valueSpecRef =
//                            node.getNodeRefProperty( valuePropNames.iterator().next(),
//                                                     true, null, node.getWorkspace() );
//                    EmsScriptNode valueNode = null;
//                    if ( valueSpecRef instanceof NodeRef ) {
//                        valueNode = new EmsScriptNode( (NodeRef)valueSpecRef, getServices() );
//                    } else if ( valueSpecRef instanceof ArrayList ) {
//                        ArrayList< NodeRef > nodeRefs = (ArrayList< NodeRef >))valueSpecRef;
//                        if ( !Utils.isNullOrEmpty( nodeRefs ) ) {
//                            if ( nodeRefs.size() > 1 ) {
//                                
//                            }
//                        }
//                        
//                    }
                    node.createOrUpdateProperty( acmType, value );
                }
	    	    else if (type.equals(Acm.JSON_LITERAL_INTEGER)) {

		        	node.createOrUpdateProperty(Acm.ACM_INTEGER, value);
		        }
		        else if (type.equals(Acm.JSON_LITERAL_REAL)) {

		        	node.createOrUpdateProperty(Acm.ACM_DOUBLE, value);
		        }
		        else if (type.equals(Acm.JSON_LITERAL_BOOLEAN)) {

		        	node.createOrUpdateProperty(Acm.ACM_BOOLEAN, value);
		        }
		        else if (type.equals(Acm.JSON_LITERAL_UNLIMITED_NATURAL)) {

		        	node.createOrUpdateProperty(Acm.ACM_NATURAL_VALUE, value);
		        }
		        else if (type.equals(Acm.JSON_LITERAL_STRING)) {
		        	node.createOrUpdateProperty(Acm.ACM_STRING, value);
		        }
		        else {
		            Debug.error("setValue(): unrecognized type: "+type);
		        }
	    	}
    	}

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

    // TODO dont like dependence on BAE for Call here....
    public Collection< Object >
    		map( Collection< Object > elements,
    			 Call call) throws InvocationTargetException {

    	return call.map( elements, 1 );
    }

    public Collection< Object >
            map( Collection< Object > elements, Call call,
                 int indexOfObjectArgument ) throws InvocationTargetException {

        return call.map( elements, indexOfObjectArgument );
    }
    
    public Collection< Object > map( Collection< Object > elements, Call call,
                                     int indexOfObjectArgument,
                                     Vector< Object > otherArguments )
                                             throws InvocationTargetException {
        call.getParameterTypes();
        int argsSize = call.getArgumentVector().size();
        int otherArgsSize = otherArguments.size();
        // Make sure there are enough arguments. We assume that one to be
        // substituted is skipped unless we can determine otherwise. In the case
        // of a variable number of arguments, otherArguments may not provide
        // one, which is legal, so otherArguments can be two short.
        if ( argsSize > otherArgsSize + 1 + ( call.isVarArgs() ? 1 : 0 ) ) {
            // TODO -- error
        }
        // Make sure there are not too many arguments if the call does not have
        // a variable number of arguments.
        else if ( argsSize < otherArgsSize && !call.isVarArgs() ) {
            // TODO -- error
        }
        // See if we have strong evidence that the substituted arg is included
        // in otherArguments.  By default, we assume not.
        boolean otherArgsIncludesSubstitute =
                ( argsSize == otherArgsSize && !call.isVarArgs() ) ||
                ( argsSize - 1 == otherArgsSize );

        // Set otherArguments before invoking
        for ( int i = 0, j = 0; i < argsSize && j < otherArgsSize; ++i, ++j ) {
            // skip the one to be substituted unless the otherArguments includes 
            if ( i != indexOfObjectArgument || otherArgsIncludesSubstitute ) {
                call.setArgument( i, otherArguments.get( j ) );
            } else {
                --j;
            }
        }
        
        // invoke the map
        return call.map( elements, 1 );
    }

    // TODO dont like dependence on BAE for Call here....
    public Collection< Object >
            filter( Collection< Object > elements,
                    FunctionCall call) throws InvocationTargetException {

        return call.filter( elements, 1 );
    }

    public BigDecimal toBigDecimal( Object o ) {
        BigDecimal d = null;
        if ( o instanceof Number ) {
            d = new BigDecimal( ( (Number)o ).doubleValue() );
        } else {
            String s = "" + o;
            try {
                d = new BigDecimal( s );
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        return d;
    }

        
    public Object sum( Object... numbers ) {
        return sumArr( numbers );
    }
    public Object sumArr( Object[] numbers ) {
        if (numbers == null ) return null;
        if ( numbers.length == 0 ) return 0;
        boolean areAllStrings = true;
        boolean areAllNumbers = true;
        boolean areNoneNumbers = true;
        Double sum = 0.0;
        //BigDecimal sum = new BigDecimal( 0.0 );
        StringBuffer sb = new StringBuffer();
        for ( Object obj : numbers ) {
            Object o = obj;
            
            if ( o == null ) continue;
            if ( o.getClass().isArray() ) {
                o = sumArr( (Object[])o );
            }
            
            if ( o instanceof String ) {
                //sb.append( (String)o );
            } else {
                areAllStrings = false;
            }
            Number n = null;
            try {
                n = ClassUtils.evaluate( obj, Number.class, false );
            } catch (Throwable t) {}

            if ( n != null ) {
                o = n;
            }
            BigDecimal bd = toBigDecimal( o );
            Double d = bd == null ? null : bd.doubleValue();
            if ( d == null ) {
                areAllNumbers = false;
            } else {
                areNoneNumbers = false;
                sum += d;
            }
        }
//        if ( areNoneNumbers ) {
//            sb = new StringBuffer();
//            for ( Object obj : numbers ) {
//                sb.append("" + obj);
//            }
//            return sb.toString();
//        }
        return round((Double)sum.doubleValue(), 5);
    }

    public Double round( Double doubleValue, int decimalPlaces ) {
        double d = doubleValue;
        System.out.println("d = " + d );
        double factor = Math.pow( 10, decimalPlaces );
        System.out.println("factor = " + factor );
        d *= factor;
        System.out.println("d *= factor = " + d);
        long i = Math.round( d );
        System.out.println("i = Math.round(d) = " + i );
        d = i / factor;
        System.out.println("d = i / factor = " + d);
        return d;
    }

    public Collection< Object > filter( Collection< Object > elements,
                                        FunctionCall call,
                                        int indexOfObjectArgument,
                                        Vector< Object > otherArguments )
                                             throws InvocationTargetException {
        call.getParameterTypes();
        int argsSize = call.getArgumentVector().size();
        int otherArgsSize = otherArguments.size();
        // Make sure there are enough arguments. We assume that one to be
        // substituted is skipped unless we can determine otherwise. In the case
        // of a variable number of arguments, otherArguments may not provide
        // one, which is legal, so otherArguments can be two short.
        if ( argsSize > otherArgsSize + 1 + ( call.isVarArgs() ? 1 : 0 ) ) {
            // TODO -- error
        }
        // Make sure there are not too many arguments if the call does not have
        // a variable number of arguments.
        else if ( argsSize < otherArgsSize && !call.isVarArgs() ) {
            // TODO -- error
        }
        // See if we have strong evidence that the substituted arg is included
        // in otherArguments.  By default, we assume not.
        boolean otherArgsIncludesSubstitute =
                ( argsSize == otherArgsSize && !call.isVarArgs() ) ||
                ( argsSize - 1 == otherArgsSize );

        // Set otherArguments before invoking
        for ( int i = 0, j = 0; i < argsSize && j < otherArgsSize; ++i, ++j ) {
            // skip the one to be substituted unless the otherArguments includes 
            if ( i != indexOfObjectArgument || otherArgsIncludesSubstitute ) {
                call.setArgument( i, otherArguments.get( j ) );
            } else {
                --j;
            }
        }
        
        // invoke the filter
        return call.filter( elements, 1 );
    }

}
