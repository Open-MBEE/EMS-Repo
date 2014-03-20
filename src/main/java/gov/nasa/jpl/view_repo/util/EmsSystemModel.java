package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Status;

import sysml.AbstractSystemModel;

// <E, C, T, P, N, I, U, R, V, W, CT>
//public class EmsSystemModel extends AbstractSystemModel< EmsScriptNode, EmsScriptNode, String, ? extends Serializable, String, String, Object, EmsScriptNode, String, String, EmsScriptNode > {
public class EmsSystemModel extends AbstractSystemModel< EmsScriptNode, EmsScriptNode, EmsScriptNode, EmsScriptNode, String, String, EmsScriptNode, EmsScriptNode, String, String, EmsScriptNode > {

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getTarget( EmsScriptNode relationship ) {
        // TODO Auto-generated method stub
        return null;
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
    public Class< EmsScriptNode > getValueClass() {
        return EmsScriptNode.class;
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
            getConstraintWithValue( Object context, EmsScriptNode specifier ) {
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
            getElementWithValue( Object context, EmsScriptNode specifier ) {
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< String > getIdentifier( Object context ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getProperty( Object context,
                                                    Object specifier ) {
        // find the specified property inside the context
        if ( context instanceof EmsScriptNode ) {
            EmsScriptNode node = (EmsScriptNode)context;
            if ( specifier == null ) {
                // if no specifier, return all properties
                Map< String, Object > props = node.getProperties();
                if ( props == null ) return Utils.newList();
                return Utils.asList( props.values(), EmsScriptNode.class );
            } else {
                Object prop = node.getProperty( "" + specifier );
                return Utils.asList( prop, EmsScriptNode.class );
            }
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
            ArrayList< EmsScriptNode > allProperties = new ArrayList< EmsScriptNode >();
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
            getPropertyWithValue( Object context, EmsScriptNode specifier ) {
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
        // TODO Auto-generated method stub
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
            getRelationshipWithValue( Object context, EmsScriptNode specifier ) {
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

    @Override
    public Collection< EmsScriptNode >
            getType( Object context, Object specifier ) {
        // TODO Auto-generated method stub
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
            getTypeWithValue( Object context, EmsScriptNode specifier ) {
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
    public Collection< EmsScriptNode > getValue( Object context,
                                                 Object specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithConstraint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithElement( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithIdentifier( Object context, String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getValueWithName( Object context,
                                                         String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithProperty( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithRelationship( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithType( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getValueWithVersion( Object context,
                                                            String specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithView( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode >
            getValueWithViewpoint( Object context, EmsScriptNode specifier ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< EmsScriptNode > getValueWithWorkspace( Object context,
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
            getViewpointWithValue( Object context, EmsScriptNode specifier ) {
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
            getViewWithValue( Object context, EmsScriptNode specifier ) {
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
    public Object set( Object object, Object specifier, EmsScriptNode value ) {
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
                                     Set< EmsScriptNode > valueDomainSet,
                                     String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public
            void
            addDomainConstraint( EmsScriptNode constraint,
                                 String version,
                                 Pair< EmsScriptNode, EmsScriptNode > valueDomainRange,
                                 String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void relaxDomain( EmsScriptNode constraint, String version,
                             Set< EmsScriptNode > valueDomainSet,
                             String workspace ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void
            relaxDomain( EmsScriptNode constraint, String version,
                         Pair< EmsScriptNode, EmsScriptNode > valueDomainRange,
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
            getConstraintsOfContext( EmsScriptNode context ) {
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
    public Collection< EmsScriptNode >
            getViolatedConstraintsOfContext( EmsScriptNode context ) {
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

    
}
