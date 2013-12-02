/**
 * 
 */
package gov.nasa.jpl.view_repo;

// import AbstractContentTransformer2;

import gov.nasa.jpl.ae.util.Debug;
import gov.nasa.jpl.ae.util.Pair;
import gov.nasa.jpl.ae.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.ChangeEvent;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
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
             implements ModelInterface< CmisObject, OperationContext, ObjectType, Object,
                                        String, String, Relationship, String > {

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
    
    public static String latestVersion( Document doc) {
        Document o = doc.getObjectOfLatestVersion( false, null );
        return o.getVersionSeriesId();
    }

    public String latestVersion( ChangeEvent change ) {
        String id = change.getObjectId();
        CmisObject obj = get( null, id, null );
        String v = null;
        if ( obj instanceof Document ) {
            v = latestVersion( (Document)obj );
//            GregorianCalendar c = timeOflatestChange((Document)obj);
        }
        return v;
    }

    public GregorianCalendar timeOflatestChange( ChangeEvent change ) {
        String id = change.getObjectId();
        CmisObject obj = get( null, id, null );
        if ( obj instanceof Document ) {
            return timeOflatestChange((Document)obj);
        }
        return null;
    }
    
    public static GregorianCalendar timeOflatestChange( Document doc ) {
        Document o = doc.getObjectOfLatestVersion( false, null );
        return o.getLastModificationDate();
    }

    public String latestVersion() {
        return latestVersionAndTime().first;
    }
    public GregorianCalendar timeOflatestChange() {
        return latestVersionAndTime().second;
    }
    public Pair<String, GregorianCalendar>latestVersionAndTime() {
        String changeLogToken = getSession().getRepositoryInfo().getLatestChangeLogToken();
        String v = latestVersion( changeLogToken );
        
        GregorianCalendar latestTime = null;
        if ( !Utils.isNullOrEmpty( v ) ) {
            latestTime = timeOflatestChange( changeLogToken );
            return new Pair<String, GregorianCalendar>(v, latestTime);
        }
        for ( Document doc : getDocuments() ) {
            GregorianCalendar t = timeOflatestChange( doc );
            if ( latestTime == null
                 || t.getTimeInMillis() > latestTime.getTimeInMillis() ) {
                v = latestVersion( doc );
                latestTime = t;
            }
        }
        return new Pair<String, GregorianCalendar>(v, latestTime);
    }
    public Collection<Document> getDocuments() {
        //Collection< Object > docs = get( Utils.newList( ModelInterface.ModelItem.ELEMENT ), getRootObjects( null ), null, null, null );
        Collection< Object > docs = 
                op( Operation.GET, 
                    Utils.newList( ModelInterface.ModelItem.ELEMENT ), 
                    Utils.newList( new Item(getSession().getRootFolder(), ModelItem.ELEMENT ) ),
                    Utils.newList(new Item("Document",ModelItem.TYPE) ), 
                                   null, false );
        return Utils.asList( docs, Document.class );
    }

    public GregorianCalendar timeOflatestChange( String changeLogToken ) {
        return latestVersionAndTime( changeLogToken ).second;
    }
    public String latestVersion( String changeLogToken ) {
        return latestVersionAndTime( changeLogToken ).first;
    }
    public Pair<String, GregorianCalendar> latestVersionAndTime( String changeLogToken ) {
        List< ChangeEvent > changes = getSession().getContentChanges( changeLogToken, true, Long.MAX_VALUE ).getChangeEvents();
        //getSession().getRootFolder().createDocument( null, null, null ).getVersionSeriesId();
        Folder root = getSession().getRootFolder();
        if ( Utils.isNullOrEmpty( changes ) ) {
            return null;
        }
        GregorianCalendar latestTime = null;
        String latestVersion = null;
        for ( ChangeEvent change : changes ) { //= changes.get( 0 );
            if ( change == null ) continue;
            GregorianCalendar c = timeOflatestChange(change);
            if ( latestTime == null || ( c != null && c.compareTo( latestTime ) > 0 ) ) {
                String v = latestVersion( change );
                if ( v != null ) {
                    latestTime = c;
                    latestVersion = v;
                }
            }
        }
        return new Pair<String, GregorianCalendar>(latestVersion, latestTime);
    }
    
    public GregorianCalendar timeOfLatestChange( String changeLogToken ) {
        List< ChangeEvent > changes = getSession().getContentChanges( changeLogToken, true, Long.MAX_VALUE ).getChangeEvents();
        //getSession().getRootFolder().createDocument( null, null, null ).getVersionSeriesId();
        Folder root = getSession().getRootFolder();
        if ( Utils.isNullOrEmpty( changes ) ) {
            return null;
        }
        GregorianCalendar latestTime = null;
        String latestVersion = null;
        for ( ChangeEvent change : changes ) { //= changes.get( 0 );
            if ( change == null ) continue;
            GregorianCalendar c = timeOflatestChange(change);
            if ( latestTime == null || ( c != null && c.compareTo( latestTime ) > 0 ) ) {
                String v = latestVersion( change );
                if ( v != null ) {
                    latestTime = c;
                    latestVersion = v;
                }
            }
        }
        return latestTime;//obj.get
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
    public CmisObject get( OperationContext context, String identifier,
                           String version ) {
        if ( identifier == null ) return null;
        // check if object id
        String id = null;
        CmisObject object = null;

        // try as object id
        object = getSession().getObject( identifier, context );

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
    public String getObjectId( CmisObject object, String version ) {
        if ( object == null ) return null;
        return object.getId();
    }

    @Override
    public String getName( CmisObject object, String version ) {
        if ( object == null ) return null;
        return object.getName();
    }

    @Override
    public ObjectType getTypeOf( CmisObject object, String version ) {
        if ( object == null ) return null;
        return object.getType();
    }

    @Override
    public Collection< Object > getTypeProperties( ObjectType type, String version ) {
        if ( type == null ) return null;
        Map< String, PropertyDefinition< ? > > defs = type.getPropertyDefinitions();
        if ( defs == null ) return null;
        Set< Object > set = new LinkedHashSet< Object >();
        set.addAll( defs.values() );
        return set;
    }

    
    @Override
    public Collection< Object > getProperties( CmisObject object, String version ) {
        if ( object == null ) return null;
        List< Property< ? >> props = object.getProperties();
        if ( props == null ) return null;
        List< Object > list = new ArrayList<Object>();
        list.addAll( props );
        return list;
    }

    @Override
    public Object getProperty( CmisObject object, String propertyName, String version ) {
        if ( object == null || propertyName == null ) return null;
        Property< Object > prop = object.getProperty( propertyName );
        return prop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ModelInterface#getRelationships(java.lang.Object)
     */
    @Override
    public Collection<Relationship> getRelationships( CmisObject object, String version ) {
        if ( object == null ) return null;
        return object.getRelationships();
    }

    @Override
    public Collection<Relationship> getRelationships( CmisObject object,
                                                      String relationshipName,
                                                      String version ) {
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
                                                String relationshipName,
                                                String version ) {
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

    @Override
    public Collection< Object > op( ModelInterface.Operation operation,
                                    Collection< ModelInterface.ModelItem > itemTypes,
                                    Collection< ModelInterface.Item > context,
                                    Collection< ModelInterface.Item > specifier,
                                    Object newValue,
                                    Boolean failForMultipleItemMatches ) throws UnsupportedOperationException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        //return null;
    }

    @Override
    public
            boolean
            isAllowed( ModelInterface.Operation operation,
                       Collection< ModelInterface.ModelItem > itemTypes,
                       Collection< ModelInterface.Item > context,
                       Collection< ModelInterface.Item > specifier,
                       ModelInterface.Item newValue,
                       Boolean failForMultipleItemMatches )  throws UnsupportedOperationException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        //return false;
    }

    @Override
    public Collection< Object > op( ModelInterface.Operation operation,
                Collection< ModelInterface.ModelItem > itemTypes,
                Collection< OperationContext > context, String identifier,
                String name, String version, boolean failForMultipleItemMatches ) throws UnsupportedOperationException {
                    // TODO Auto-generated method stub
                    throw new UnsupportedOperationException();
        //return null;
    }

    @Override
    public
            Collection< Object >
            get( Collection< ModelInterface.ModelItem > itemTypes,
                 Collection< OperationContext > context, String identifier,
                 String name, String version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            create( ModelInterface.ModelItem item,
                    Collection< OperationContext > context, String identifier,
                    String name, String version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< Object >
            delete( ModelInterface.ModelItem item,
                    Collection< OperationContext > context, String identifier,
                    String name, String version ) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
//    public CmisObject
//            get( CmisObject context, String identifier, String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public Collection< CmisObject > getRootObjects( String version ) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
//    public String getObjectId( CmisObject element, String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public String getName( CmisObject element, String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public ObjectType getTypeOf( CmisObject element, String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public ObjectType getType( OperationContext context, String name, String version ) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
//    public Collection< Object > getTypeProperties( ObjectType type,
//                                                   String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getProperties( CmisObject element,
//                                               String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public Object getProperty( CmisObject element, String propertyName,
//                               String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public Collection< Relationship >
//            getRelationships( CmisObject element, String relationshipName,
//                              String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public Collection< CmisObject > getRelated( CmisObject element,
//                                                String relationshipName,
//                                                String version ) {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public boolean latestVersion( Collection< OperationContext > context ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean idsAreSettable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean namesAreSettable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean elementsMayBeChangedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean typesMayBeChangedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean propertiesMayBeChangedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean elementsMayBeCreatedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean typesMayBeCreatedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean propertiesMayBeCreatedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean elementsMayBeDeletedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean typesMayBeDeletedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean propertiesMayBeDeletedForVersion( String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public CmisObject createObject( String identifier, String version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setIdentifier( CmisObject element, String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setName( CmisObject element, String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setType( CmisObject element, String version ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public CmisObject deleteObject( String identifier, String version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectType deleteType( CmisObject element, String version ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< Object >
            map( Collection< CmisObject > elements, Method method,
                 int indexOfElementArgument, Object... otherArguments )
                                                                       throws InvocationTargetException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< Object >
            filter( Collection< CmisObject > elements, Method method,
                    int indexOfElementArgument, Object... otherArguments )
                                                                          throws InvocationTargetException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            boolean
            forAll( Collection< CmisObject > elements, Method method,
                    int indexOfElementArgument, Object... otherArguments )
                                                                          throws InvocationTargetException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public
            boolean
            thereExists( Collection< CmisObject > elements, Method method,
                         int indexOfElementArgument, Object... otherArguments )
                                                                               throws InvocationTargetException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public
            Object
            fold( Collection< CmisObject > elements, Object initialValue,
                  Method method, int indexOfElementArgument,
                  int indexOfPriorResultArgument, Object... otherArguments )
                                                                            throws InvocationTargetException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public
            Collection< CmisObject >
            sort( Collection< CmisObject > elements,
                  Comparator< ? > comparator, Method method,
                  int indexOfElementArgument, Object... otherArguments )
                                                                        throws InvocationTargetException {
        // TODO Auto-generated method stub
        return null;
    }


}
