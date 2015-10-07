///**
// *
// */
//package gov.nasa.jpl.view_repo;
//
//// import AbstractContentTransformer2;
//
//import gov.nasa.jpl.mbee.util.Debug;
//import gov.nasa.jpl.mbee.util.Pair;
//import gov.nasa.jpl.mbee.util.Utils;
//
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.apache.chemistry.opencmis.client.api.ChangeEvent;
//import org.apache.chemistry.opencmis.client.api.CmisObject;
//import org.apache.chemistry.opencmis.client.api.Document;
//import org.apache.chemistry.opencmis.client.api.Folder;
//import org.apache.chemistry.opencmis.client.api.ItemIterable;
//import org.apache.chemistry.opencmis.client.api.ObjectType;
//import org.apache.chemistry.opencmis.client.api.OperationContext;
//import org.apache.chemistry.opencmis.client.api.Property;
//import org.apache.chemistry.opencmis.client.api.QueryResult;
//import org.apache.chemistry.opencmis.client.api.Relationship;
//import org.apache.chemistry.opencmis.client.api.Repository;
//import org.apache.chemistry.opencmis.client.api.Session;
//import org.apache.chemistry.opencmis.client.api.SessionFactory;
//import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
//import org.apache.chemistry.opencmis.commons.SessionParameter;
//import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
//import org.apache.chemistry.opencmis.commons.enums.BindingType;
//
//import sysml.AbstractSystemModel;
//import sysml.SystemModel;
//
///**
// * An implementation of a generic model interface for querying CMIS servers
// * using Apache Chemistry OpenCMIS.
// */
//public class CmisModelInterfaceImpl
//		extends
//		AbstractSystemModel<CmisObject, OperationContext, ObjectType, Object, String, String, Object, Relationship, String, Object, CmisObject> {
//
//	protected static CmisModelInterfaceImpl instance;
//	protected String ADMIN_USER_NAME = "admin";
//	protected String ADMIN_PASSWORD = "admin";
//	protected SessionFactory factory = null;
//	protected List<Repository> repositories;
//	String bindingType = ATOM_BINDING; // default
//	protected Session session = null;
//
//	public static final String LOCAL_BINDING = BindingType.LOCAL.value();
//	public static final String ATOM_BINDING = BindingType.ATOMPUB.value();
//	public static final String WEBSERVICES_BINDING = BindingType.WEBSERVICES
//			.value();
//	public static final String JSON_BINDING = BindingType.BROWSER.value();
//	public static final String ATOMPUB_URL = "http://localhost:8080/view-repo/cmisatom";
//	// TODO fix these URLs
//	public static final String WEBSERVICES_URL = null; // "http://localhost:8080/view-repo/cmisatom";
//	public static final String JSON_URL = null; // "http://localhost:8080/view-repo/cmisatom";
//
//	public CmisModelInterfaceImpl() {
//		super();
//		if (instance == null)
//			instance = this;
//	}
//
//	/**
//	 * @return the root folder, named "Company Home"
//	 */
//	public Folder getRootFolder() {
//		Folder folder = getSession().getRootFolder();
//		return folder;
//	}
//
//	public static String latestVersion(Document doc) {
//		Document o = doc.getObjectOfLatestVersion(false, null);
//		return o.getVersionSeriesId();
//	}
//
//	public String latestVersion(ChangeEvent change) {
//		String id = change.getObjectId();
//		CmisObject obj = getElement(null, id);
//		String v = null;
//		if (obj instanceof Document) {
//			v = latestVersion((Document) obj);
//			// GregorianCalendar c = timeOflatestChange((Document)obj);
//		}
//		return v;
//	}
//
//	public GregorianCalendar timeOflatestChange(ChangeEvent change) {
//		String id = change.getObjectId();
//		CmisObject obj = getElement(null, id);
//		if (obj instanceof Document) {
//			return timeOflatestChange((Document) obj);
//		}
//		return null;
//	}
//
//	public static GregorianCalendar timeOflatestChange(Document doc) {
//		Document o = doc.getObjectOfLatestVersion(false, null);
//		return o.getLastModificationDate();
//	}
//
//	public String latestVersion() {
//		return latestVersionAndTime().first;
//	}
//
//	public GregorianCalendar timeOflatestChange() {
//		return latestVersionAndTime().second;
//	}
//
//	public Pair<String, GregorianCalendar> latestVersionAndTime() {
//		String changeLogToken = getSession().getRepositoryInfo()
//				.getLatestChangeLogToken();
//		String v = latestVersion(changeLogToken);
//
//		GregorianCalendar latestTime = null;
//		if (!Utils.isNullOrEmpty(v)) {
//			latestTime = timeOflatestChange(changeLogToken);
//			return new Pair<String, GregorianCalendar>(v, latestTime);
//		}
//		for (Document doc : getDocuments()) {
//			GregorianCalendar t = timeOflatestChange(doc);
//			if (latestTime == null
//					|| t.getTimeInMillis() > latestTime.getTimeInMillis()) {
//				v = latestVersion(doc);
//				latestTime = t;
//			}
//		}
//		return new Pair<String, GregorianCalendar>(v, latestTime);
//	}
//
//	public Collection<Document> getDocuments() {
//		// Collection< Object > docs = get( Utils.newList(
//		// ModelInterface.ModelItem.OBJECT ), getRootElements( null ), null,
//		// null, null );
//		Collection<Object> docs = op(Operation.GET,
//				Utils.newList(SystemModel.ModelItem.ELEMENT),
//				Utils.newList(new SystemModel.Item(getSession().getRootFolder(),
//						ModelItem.ELEMENT)), Utils.newList(new SystemModel.Item("Document",
//						ModelItem.TYPE)), null, false);
//		return Utils.asList(docs, Document.class);
//	}
//
//	public GregorianCalendar timeOflatestChange(String changeLogToken) {
//		return latestVersionAndTime(changeLogToken).second;
//	}
//
//	public String latestVersion(String changeLogToken) {
//		return latestVersionAndTime(changeLogToken).first;
//	}
//
//	public Pair<String, GregorianCalendar> latestVersionAndTime(
//			String changeLogToken) {
//		List<ChangeEvent> changes = getSession().getContentChanges(
//				changeLogToken, true, Long.MAX_VALUE).getChangeEvents();
//		if (Utils.isNullOrEmpty(changes)) {
//			return null;
//		}
//		GregorianCalendar latestTime = null;
//		String latestVersion = null;
//		for (ChangeEvent change : changes) { // = changes.get( 0 );
//			if (change == null)
//				continue;
//			GregorianCalendar c = timeOflatestChange(change);
//			if (latestTime == null
//					|| (c != null && c.compareTo(latestTime) > 0)) {
//				String v = latestVersion(change);
//				if (v != null) {
//					latestTime = c;
//					latestVersion = v;
//				}
//			}
//		}
//		return new Pair<String, GregorianCalendar>(latestVersion, latestTime);
//	}
//
//	public GregorianCalendar timeOfLatestChange(String changeLogToken) {
//		List<ChangeEvent> changes = getSession().getContentChanges(
//				changeLogToken, true, Long.MAX_VALUE).getChangeEvents();
//		if (Utils.isNullOrEmpty(changes)) {
//			return null;
//		}
//		GregorianCalendar latestTime = null;
//		for (ChangeEvent change : changes) { // = changes.get( 0 );
//			if (change == null)
//				continue;
//			GregorianCalendar c = timeOflatestChange(change);
//			if (latestTime == null
//					|| (c != null && c.compareTo(latestTime) > 0)) {
//				String v = latestVersion(change);
//				if (v != null) {
//					latestTime = c;
//				}
//			}
//		}
//		return latestTime;// obj.get
//	}
//
//	public Session getSession() {
//		return getSession(false);
//	}
//
//	public Session getSession(boolean createNew) {
//		if (createNew || session == null) {
//			Map<String, String> parameter = new HashMap<String, String>();
//
//			// Set the user credentials
//			parameter.put(SessionParameter.USER, "admin");
//			parameter.put(SessionParameter.PASSWORD, "admin");
//
//			// Specify the connection settings
//			parameter.put(SessionParameter.ATOMPUB_URL,
//					"http://localhost:8080/view-repo/cmisatom");
//			parameter.put(SessionParameter.BINDING_TYPE, bindingType);
//
//			// Set the alfresco object factory
//			parameter.put(SessionParameter.OBJECT_FACTORY_CLASS,
//					"org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");
//
//			repositories = getFactory(createNew).getRepositories(parameter);
//			if (Debug.isOn()) Debug.outln(repositories.size() + " Repositories");
//			for (Repository r : repositories) {
//				if (Debug.isOn()) Debug.outln("  Id: " + r.getId());
//				if (Debug.isOn()) Debug.outln("  Name: " + r.getName());
//				if (Debug.isOn()) Debug.outln("  Description: " + r.getDescription());
//			}
//			session = repositories.get(0).createSession();
//
//		}
//		return session;
//	}
//
//	public SessionFactory getFactory() {
//		return getFactory(false);
//	}
//
//	public SessionFactory getFactory(boolean createNew) {
//		if (createNew || factory == null) {
//			factory = SessionFactoryImpl.newInstance();
//		}
//		return factory;
//	}
//
//	public List<QueryResult> cmisQuery(String query) {
//		if (query == null)
//			return null;
//		// Query example:
//		// "SELECT cmis:name, cmis:objectId AS MyId from cmis:document where cmis:name =  'myfile.ext'",
//		// false);
//		ItemIterable<QueryResult> resultsI = getSession().query(query, false);
//		List<QueryResult> results = new ArrayList<QueryResult>();
//		// if (Debug.isOn()) Debug.outln("Results");
//		// int ct = 0;
//		for (QueryResult result : resultsI) {
//			results.add(result);
//			// if (Debug.isOn()) Debug.outln("Result Properties " + ct++ + ": " +
//			// result.result.getProperties());
//			// for (PropertyData<?> data: result.getProperties()) {
//			// if (Debug.isOn()) Debug.outln("  Query name:" + data.getQueryName());
//			// if (Debug.isOn()) Debug.outln("      Values:" + data.getValues());
//			// }
//		}
//		if (Debug.isOn()) Debug.outln("cmisQuery(" + query + ") returning " + results);
//		return results;
//	}
//
//	// public static Collection<NodeRef> cmisNodeQuery( String query ) {
//	// Collection<NodeRef> results = queryResultsToNodes( cmisQuery( query ) );
//	// if (Debug.isOn()) Debug.outln("cmisNodeQuery(" + query + ") returning " + results );
//	// return results;
//	// }
//	// public List<NodeRef> cmisTest(String query) {
//	// return query( query, SearchService.LANGUAGE_CMIS_ALFRESCO );
//	// }
//
//	public CmisObject getElementByPath(String path) {
//		if (path == null)
//			return null;
//		// String path = "/User Homes/customer1/document.odt"
//		CmisObject object = getSession().getObjectByPath(path);
//		return object;
//	}
//
//	//@Override
//	public CmisObject getElement(OperationContext context, String identifier ) {//,
//			//String version) {
//		if (identifier == null)
//			return null;
//		// check if object id
//		String id = null;
//		CmisObject object = null;
//
//		// unfortunately, this isn't in opencmis 0.8.0, found it in 0.10.0
//		//if ( context == null ) context = OperationContextUtils.createMaximumOperationContext();
//        if ( context == null ) {
//            context = session.createOperationContext();
//        }
//
//		// try as object id
//		object = getSession().getObject(identifier, context);
//
//		// List< QueryResult > results = cmisQuery(
//		// "select cm:objectId from cm:document where cm:name = '" + identifier
//		// + "'");
//		//
//		// if ( results.size() == 1 ) {
//		// PropertyData< Object > data = results.get( 0
//		// ).getPropertyByQueryName( "cm:objectId" );
//		// if ( data != null && data.getValues().size() == 1 ) {
//		// id = data.getFirstValue().toString();
//		// }
//		// }
//
//		// try to get object treating identifier as a path
//		if (id == null && object == null) {
//			object = getElementByPath(identifier);
//			// id = object.getId();
//		}
//		return object;
//	}
//
//	//@Override
//	public String getElementId(CmisObject object) {//, String version) {
//		if (object == null)
//			return null;
//		return object.getId();
//	}
//
//	//@Override
//	public String getName(CmisObject object) {//, String version) {
//		if (object == null)
//			return null;
//		return object.getName();
//	}
//
//	//@Override
//	public ObjectType getTypeOf(CmisObject object) {//, String version) {
//		if (object == null)
//			return null;
//		return object.getType();
//	}
//
//	//@Override
//	public Collection<Object> getTypeProperties(ObjectType type) {//, String version) {
//		if (type == null)
//			return null;
//		Map<String, PropertyDefinition<?>> defs = type.getPropertyDefinitions();
//		if (defs == null)
//			return null;
//		Set<Object> set = new LinkedHashSet<Object>();
//		set.addAll(defs.values());
//		return set;
//	}
//
//	//@Override
//	public Collection<Object> getProperties(CmisObject object) {//, String version) {
//		if (object == null)
//			return null;
//		List<Property<?>> props = object.getProperties();
//		if (props == null)
//			return null;
//		List<Object> list = new ArrayList<Object>();
//		list.addAll(props);
//		return list;
//	}
//
//	//@Override
//	public Object getProperty(CmisObject object, String propertyName) {//,
//			//String version) {
//		if (object == null || propertyName == null)
//			return null;
//		Property<Object> prop = object.getProperty(propertyName);
//		return prop;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 *
//	 * @see ModelInterface#getRelationships(java.lang.Object)
//	 */
//	//@Override
//	public Collection<Relationship> getRelationships(CmisObject object) {//,
//			//String version) {
//		if (object == null)
//			return null;
//		return object.getRelationships();
//	}
//
//	//@Override
//	public Collection<Relationship> getRelationships(CmisObject object,
//	                                                 String relationshipName) {//, String version) {
//		if (object == null || relationshipName == null)
//			return null;
//		List<Relationship> rels = object.getRelationships();
//		if (rels == null)
//			return null;
//		ArrayList<Relationship> relsForProp = new ArrayList<Relationship>();
//		for (Relationship r : rels) {
//			if (r == null)
//				continue;
//			if (r.getName().equalsIgnoreCase(relationshipName)) {
//				relsForProp.add(r);
//			} else if (r.getType().getLocalName()
//					.equalsIgnoreCase(relationshipName)) {
//				relsForProp.add(r);
//			}
//		}
//		return relsForProp;
//	}
//
//    //@Override
//    public Collection< CmisObject > getRelated( CmisObject object,
//                                                String relationshipName ) {
//        Collection< Relationship > rels =
//                getRelationships( object, relationshipName );
//        Set< CmisObject > results = new LinkedHashSet< CmisObject >();
//        for ( Relationship r : rels ) {
//            if ( r == null ) continue;
//            CmisObject source = r.getSource();
//            CmisObject target = r.getTarget();
//            if ( source != null && !source.equals( object ) ) {
//                results.add( source );
//            } else if ( target != null && !target.equals( object ) ) {
//                results.add( source );
//            }
//        }
//        return results;
//    }
//
//    public static CmisModelInterfaceImpl getInstance() {
//        if ( instance == null ) {
//            instance = new CmisModelInterfaceImpl();
//        }
//        return instance;
//    }
//
//    @Override
//    public CmisObject createConstraint( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public CmisObject createElement( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public String createIdentifier( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public String createName( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Object createProperty( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Relationship createRelationship( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public ObjectType createType( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Object createValue( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public String createVersion( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public CmisObject createView( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public CmisObject createViewpoint( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Object createWorkspace( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Object delete( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraint( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< CmisObject > getConstraintClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithElement( Object arg0,
//                                                              CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithIdentifier( Object arg0,
//                                                                 String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithName( Object arg0,
//                                                           String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithProperty( Object arg0,
//                                                               Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getConstraintWithRelationship( Object arg0, Relationship arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithType( Object arg0,
//                                                           ObjectType arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithValue( Object arg0,
//                                                            Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithVersion( Object arg0,
//                                                              String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithView( Object arg0,
//                                                           CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getConstraintWithViewpoint( Object arg0, CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintWithWorkspace( Object arg0,
//                                                                Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< OperationContext > getContextClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElement( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< CmisObject > getElementClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementForRole( Relationship arg0,
//                                                       String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithConstraint( Object arg0,
//                                                              CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithIdentifier( Object arg0,
//                                                              String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getElementWithName( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithProperty( Object arg0,
//                                                            Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getElementWithRelationship( Object arg0, Relationship arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithType( Object arg0,
//                                                        ObjectType arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithValue( Object arg0,
//                                                         Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithVersion( Object arg0,
//                                                           String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithView( Object arg0,
//                                                        CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithViewpoint( Object arg0,
//                                                             CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getElementWithWorkspace( Object arg0,
//                                                             Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public String getIdentifier( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< String > getIdentifierClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< String > getName( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< String > getNameClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getProperty( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< Object > getPropertyClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithConstraint( Object arg0,
//                                                           CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithElement( Object arg0,
//                                                        CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithIdentifier( Object arg0,
//                                                           String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithRelationship( Object arg0,
//                                                             Relationship arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithType( Object arg0,
//                                                     ObjectType arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithValue( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object >
//            getPropertyWithVersion( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithView( Object arg0,
//                                                     CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithViewpoint( Object arg0,
//                                                          CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getPropertyWithWorkspace( Object arg0,
//                                                          Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getRelatedElements( Relationship arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship >
//            getRelationship( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< Relationship > getRelationshipClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship >
//            getRelationshipWithConstraint( Object arg0, CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship >
//            getRelationshipWithElement( Object arg0, CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship >
//            getRelationshipWithIdentifier( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship > getRelationshipWithName( Object arg0,
//                                                               String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship > getRelationshipWithProperty( Object arg0,
//                                                                   Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship > getRelationshipWithType( Object arg0,
//                                                               ObjectType arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship > getRelationshipWithValue( Object arg0,
//                                                                Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship > getRelationshipWithVersion( Object arg0,
//                                                                  String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship > getRelationshipWithView( Object arg0,
//                                                               CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship >
//            getRelationshipWithViewpoint( Object arg0, CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Relationship >
//            getRelationshipWithWorkspace( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getSource( Relationship arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getTarget( Relationship arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getType( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< ObjectType > getTypeClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithConstraint( Object arg0,
//                                                           CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithElement( Object arg0,
//                                                        CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithIdentifier( Object arg0,
//                                                           String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithName( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithProperty( Object arg0,
//                                                         Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithRelationship( Object arg0,
//                                                             Relationship arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithValue( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType >
//            getTypeWithVersion( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithView( Object arg0,
//                                                     CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithViewpoint( Object arg0,
//                                                          CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< ObjectType > getTypeWithWorkspace( Object arg0,
//                                                          Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValue( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< Object > getValueClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithConstraint( Object arg0,
//                                                        CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithElement( Object arg0,
//                                                     CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object >
//            getValueWithIdentifier( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithName( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithProperty( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithRelationship( Object arg0,
//                                                          Relationship arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithType( Object arg0, ObjectType arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithVersion( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithView( Object arg0, CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getValueWithViewpoint( Object arg0,
//                                                       CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object >
//            getValueWithWorkspace( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< String > getVersion( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< String > getVersionClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getView( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< ? extends CmisObject > getViewClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithConstraint( Object arg0,
//                                                           CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithElement( Object arg0,
//                                                        CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithIdentifier( Object arg0,
//                                                           String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithName( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithProperty( Object arg0,
//                                                         Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithRelationship( Object arg0,
//                                                             Relationship arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithType( Object arg0,
//                                                     ObjectType arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithValue( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getViewWithVersion( Object arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithViewpoint( Object arg0,
//                                                          CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewWithWorkspace( Object arg0,
//                                                          Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpoint( Object arg0, Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< ? extends CmisObject > getViewpointClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getViewpointWithConstraint( Object arg0, CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithElement( Object arg0,
//                                                             CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithIdentifier( Object arg0,
//                                                                String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithName( Object arg0,
//                                                          String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithProperty( Object arg0,
//                                                              Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getViewpointWithRelationship( Object arg0, Relationship arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithType( Object arg0,
//                                                          ObjectType arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithValue( Object arg0,
//                                                           Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithVersion( Object arg0,
//                                                             String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithView( Object arg0,
//                                                          CmisObject arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject > getViewpointWithWorkspace( Object arg0,
//                                                               Object arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< Object > getWorkspace( Object arg0 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Class< Object > getWorkspaceClass() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public boolean isDirected( Relationship arg0 ) {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//    @Override
//    public Object set( Object arg0, Object arg1, Object arg2 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public void addConstraint( CmisObject arg0, String arg1, Object arg2 ) {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public void addDomainConstraint( CmisObject arg0, String arg1,
//                                     Set< Object > arg2, Object arg3 ) {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public
//            void
//            addDomainConstraint( CmisObject arg0,
//                                 String arg1,
//                                 gov.nasa.jpl.mbee.util.Pair< Object, Object > arg2,
//                                 Object arg3 ) {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public Collection< CmisObject > getConstraintsOfElement( CmisObject arg0,
//                                                             String arg1,
//                                                             Object arg2 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public CmisObject getDomainConstraint( CmisObject arg0, String arg1,
//                                           Object arg2 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Number getScore() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< CmisObject >
//            getViolatedConstraintsOfElement( CmisObject arg0, String arg1 ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public boolean idsAreWritable() {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//    @Override
//    public boolean namesAreWritable() {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//    @Override
//    public void relaxDomain( CmisObject arg0, String arg1, Set< Object > arg2,
//                             Object arg3 ) {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public void
//            relaxDomain( CmisObject arg0, String arg1,
//                         gov.nasa.jpl.mbee.util.Pair< Object, Object > arg2,
//                         Object arg3 ) {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public void setOptimizationFunction( Method arg0, Object... arg1 ) {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public boolean versionsAreWritable() {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
////    @Override
////    public boolean fixConstraintViolations( CmisObject element, String version ) {
////        // TODO Auto-generated method stub
////        return false;
////    }
//
//    // TODO remove this once we fix getType()
//    @Override
//    public String getTypeString( Object context, Object specifier ) {
//
//        return null;
//
//    }
//
//    @Override
//    public boolean fixConstraintViolations( CmisObject element, String version ) {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//}
