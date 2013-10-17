/**
 * 
 */
package gov.nasa.jpl.view_repo;

// import AbstractContentTransformer2;

import gov.nasa.jpl.ae.util.Debug;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BindingType;

/**
 * An implementation of a generic model interface for querying CMIS servers
 * using Apache Chemistry OpenCMIS.
 */
public class CmisModelInterfaceImpl 
             implements ModelInterface< CmisObject, ObjectType, Object,
                                        String, String, Relationship > {

    protected static CmisModelInterfaceImpl instance;
    protected String ADMIN_USER_NAME = "admin";
    protected String ADMIN_PASSWORD = "admin";
    protected SessionFactory factory = null;
    protected List<Repository> repositories;
    String bindingType = ATOM_BINDING; // default
    protected Session session = null;

    public static final String LOCAL_BINDING = BindingType.LOCAL.value();
    public static final String ATOM_BINDING = BindingType.ATOMPUB.value();
    public static final String WEBSERVICES_BINDING = BindingType.WEBSERVICES.value();
    public static final String JSON_BINDING = BindingType.BROWSER.value();
    public static final String ATOMPUB_URL = "http://localhost:8080/view-repo/cmisatom";
    // TODO fix these URLs
    public static final String WEBSERVICES_URL = null; //"http://localhost:8080/view-repo/cmisatom";
    public static final String JSON_URL = null; //"http://localhost:8080/view-repo/cmisatom";


    public CmisModelInterfaceImpl() {
        super();
        if ( instance == null ) instance = this;
    }
    
   /**
    * @return the root folder, named "Company Home"
    */
    public Folder getRootFolder()
    {
        Folder folder = getSession().getRootFolder();
        return folder;
    }
    
    public Session getSession() {
        return getSession(false);
    }
    public Session getSession( boolean createNew ) {
        if ( createNew || session == null ) {
            Map<String, String> parameter = new HashMap<String,String>();

            // Set the user credentials
            parameter.put(SessionParameter.USER, "admin");
            parameter.put(SessionParameter.PASSWORD, "admin");

            // Specify the connection settings
            parameter.put(SessionParameter.ATOMPUB_URL, "http://localhost:8080/view-repo/cmisatom");
            parameter.put(SessionParameter.BINDING_TYPE, bindingType);

            // Set the alfresco object factory
            parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

            repositories = getFactory(createNew).getRepositories(parameter);
            Debug.outln(repositories.size() + " Repositories");
            for(Repository r: repositories) {
                Debug.outln("  Id: " + r.getId());
                Debug.outln("  Name: " + r.getName());
                Debug.outln("  Description: " + r.getDescription());
            }
            session = repositories.get(0).createSession();

        }
        return session;
    }
    
    public SessionFactory getFactory() {
        return getFactory( false );
    }
    public SessionFactory getFactory( boolean createNew ) {
        if ( createNew || factory == null ) {
            factory = SessionFactoryImpl.newInstance();
        }
        return factory;
    }
    
    public List<QueryResult> cmisQuery( String query ) {
        if ( query == null ) return null;
        // Query example:
        //"SELECT cmis:name, cmis:objectId AS MyId from cmis:document where cmis:name =  'myfile.ext'", false);
        ItemIterable< QueryResult > resultsI = getSession().query(query, false);
        List<QueryResult> results = new ArrayList<QueryResult>();
//        Debug.outln("Results");
//        int ct = 0;
        for (QueryResult result : resultsI) {
            results.add( result );
//            Debug.outln("Result Properties " + ct++ + ": " + result.result.getProperties());
//            for (PropertyData<?> data: result.getProperties()) {
//                Debug.outln("  Query name:" + data.getQueryName());
//                Debug.outln("      Values:" + data.getValues());
//            }
        }
        Debug.outln("cmisQuery(" + query + ") returning " + results );
        return results;
    }

//    public static Collection<NodeRef> cmisNodeQuery( String query ) {
//        Collection<NodeRef> results = queryResultsToNodes( cmisQuery( query ) );
//        Debug.outln("cmisNodeQuery(" + query + ") returning " + results  );
//        return results;
//    }
//    public List<NodeRef> cmisTest(String query) {
//        return query( query, SearchService.LANGUAGE_CMIS_ALFRESCO );
//    }

    public CmisObject getObjectByPath( String path ) {
        if ( path == null ) return null;
        //String path = "/User Homes/customer1/document.odt"
        CmisObject object = getSession().getObjectByPath(path);
        return object;
    }
    
    
    @Override
    public CmisObject getObject( String identifier ) {
        if ( identifier == null ) return null;
        // check if object id
        String id = null;
        CmisObject object = null;

        // try as object id
        object = getSession().getObject( identifier );

//        List< QueryResult > results = cmisQuery( "select cm:objectId from cm:document where cm:name = '" + identifier + "'");
//        
//        if ( results.size() == 1 ) {
//            PropertyData< Object > data = results.get( 0 ).getPropertyByQueryName( "cm:objectId" );
//            if ( data != null && data.getValues().size() == 1 ) {
//                id = data.getFirstValue().toString();
//            }
//        }

        // try to get object treating identifier as a path
        if ( id == null && object == null ) {
            object = getObjectByPath(identifier);
            //id = object.getId();
        }
        return object;
    }

    @Override
    public String getObjectId( CmisObject object ) {
        if ( object == null ) return null;
        return object.getId();
    }

    @Override
    public String getName( CmisObject object ) {
        if ( object == null ) return null;
        return object.getName();
    }

    @Override
    public ObjectType getType( CmisObject object ) {
        if ( object == null ) return null;
        return object.getType();
    }

    @Override
    public Collection< Object > getTypeProperties( ObjectType type ) {
        if ( type == null ) return null;
        Map< String, PropertyDefinition< ? > > defs = type.getPropertyDefinitions();
        if ( defs == null ) return null;
        Set< Object > set = new LinkedHashSet< Object >();
        set.addAll( defs.values() );
        return set;
    }

    
    @Override
    public Collection< Object > getProperties( CmisObject object ) {
        if ( object == null ) return null;
        List< Property< ? >> props = object.getProperties();
        if ( props == null ) return null;
        List< Object > list = new ArrayList<Object>();
        list.addAll( props );
        return list;
    }

    @Override
    public Object getProperty( CmisObject object, String propertyName ) {
        if ( object == null || propertyName == null ) return null;
        Property< Object > prop = object.getProperty( propertyName );
        return prop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.nasa.jpl.view_repo.ModelInterface#getRelationships(java.lang.Object)
     */
    @Override
    public Collection<Relationship> getRelationships( CmisObject object ) {
        if ( object == null ) return null;
        return object.getRelationships();
    }

    @Override
    public Collection<Relationship> getRelationships( CmisObject object,
                                                      String relationshipName ) {
        if ( object == null || relationshipName == null ) return null;
        List<Relationship> rels = object.getRelationships();
        if ( rels == null ) return null;
        ArrayList<Relationship> relsForProp = new ArrayList<Relationship>();
        for ( Relationship r : rels ) {
            if ( r == null ) continue;
            if ( r.getName().equalsIgnoreCase( relationshipName ) ) {
                relsForProp.add( r );
            } else if ( r.getType().getLocalName().equalsIgnoreCase( relationshipName ) ) {
                relsForProp.add( r );
            }
        }
        return relsForProp;
    }

    @Override
    public Collection< CmisObject > getRelated( CmisObject object,
                                                String relationshipName ) {
        Collection<Relationship> rels = getRelationships(object, relationshipName );
        Set< CmisObject > results = new LinkedHashSet<CmisObject>();
        for ( Relationship r : rels ) {
            if ( r == null ) continue;
            CmisObject source = r.getSource();
            CmisObject target = r.getTarget();
            if ( source != null && !source.equals( object ) ) {
                results.add( source );
            } else if ( target != null && !target.equals( object ) ) {
                results.add( source );
            }
        }
        return results;
    }

    public static CmisModelInterfaceImpl getInstance() {
        if ( instance == null  ) {
            instance = new CmisModelInterfaceImpl();
        }
        return instance;
    }


}
