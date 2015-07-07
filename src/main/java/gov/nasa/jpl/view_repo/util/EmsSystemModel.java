package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.ae.event.Call;
import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.HasId;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Level;
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

    @Override
    public Collection< Object > getValue( Object context,
    									  Object specifier ) {

        Object mySpecifier = specifier;
        // Convert specifier to add ACM type, ie prepend "sysml:":
        Map<String, String> convertMap = Acm.getJSON2ACM();
        if (specifier instanceof String && convertMap.containsKey(specifier)) {
        	 mySpecifier = convertMap.get(specifier);
        }

        ArrayList< Object > resultList = new ArrayList< Object >();
        // find the specified property inside the context
        if ( context instanceof Collection ) {
            Collection< ? > coll = ((Collection<?>)context);
            if ( coll.size() == 0 ) return resultList;
            if ( coll.size() == 1 ) {
                context = coll.iterator().next();
            } else {
                for ( Object o : coll ) {
                    Collection< Object > vals = getValue( o, mySpecifier );
                    resultList.addAll( vals );
                }
                return resultList;
            }
        }
    	// Assuming that we can only have EmsScriptNode context:
    	if (context instanceof EmsScriptNode) {

    		EmsScriptNode node = (EmsScriptNode) context;

			// If it is a Property type, then the value is a NodeRef, which
			// we convert to a EmsScriptNode:
    		if (node.hasAspect(Acm.ACM_PROPERTY)) {

		    	List<EmsScriptNode> returnList = new ArrayList<EmsScriptNode>();
//<<<<<<< HEAD
////				Collection<NodeRef> valueNodes =
////				        (Collection< NodeRef >)node.getProperty(Acm.ACM_VALUE);
//                Object value = node.getProperty(Acm.ACM_VALUE);
//                Boolean isColl = null;
//                Boolean isNonEmptyColl = null;
//                if ( value instanceof NodeRef ) {
//                    convertToScriptNode(value, returnList);
//                    resultList.addAll(returnList);
//                } else {
//                    isColl = ( value instanceof Collection );
//                    Collection<?> coll = isColl ? ((Collection<?>)value) : null;
//                    isNonEmptyColl = isColl && coll.size() > 0;
//                    Object first = isNonEmptyColl ? coll.iterator().next() : null;
//                    if ( first instanceof NodeRef || first instanceof String ) {
//                        convertToScriptNode(value, returnList);
//                        if ( returnList.size() < coll.size() ) {
//                            resultList.addAll( coll );
//                        } else {
//                            resultList = Utils.asList( returnList, Object.class );
//                        }
//                    }
//                    
//                }
//=======
		    	// TODO need the workspace, time
				Collection<NodeRef> valueNodes =
				        (Collection< NodeRef >)node.getNodeRefProperty(Acm.ACM_VALUE, null, node.getWorkspace());
				convertToScriptNode(valueNodes, returnList);
//>>>>>>> refs/remotes/origin/develop

//	    		return Utils.asList(returnList, Object.class);
                return resultList;
			}

			// Otherwise, return the Object for the value
			else {

	    		// If no specifier is supplied:
				if (mySpecifier == null) {
					// TODO what should we do here?
	    		}
				else {

					Object valueNode = node.getNodeRefProperty("" + mySpecifier, null, node.getWorkspace());

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
		        if (type.equals(Acm.JSON_LITERAL_INTEGER)) {

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
			map( Collection< Object > elements,
				 Call call,
				 int indexOfObjectArgument) throws InvocationTargetException {

		return call.map( elements, indexOfObjectArgument );
	}


    /**
     * Evaluate the text expression (k, Java?).
     * @param expression
     * @return
     */
    public Object evaluate( String expression ) {
        return null;
    }
    
}
