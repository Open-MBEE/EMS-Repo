/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.view_repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MoreToString;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cmis.client.CMISConnectionManager;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.nodelocator.XPathNodeLocator;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.DocumentType;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.FolderType;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

/**
 * This is a Java interface that uses the native Alfresco Java interface for
 * accessing and querying model information.
 */
public class JavaQuery extends AbstractModuleComponent {
                       //implements SystemModel< NodeRef, NodeRef, String, Object, String, String, Object, AssociationRef, String, Object, NodeRef > {

    public static ApplicationContext applicationContext;
    public static ServiceRegistry services = null;
    protected static JavaQuery instance;// = initAppContext(); // only use this in unit test mode
    protected static final String ADMIN_USER_NAME = "admin";
    public static Log log = LogFactory.getLog( JavaQuery.class );
    
    public static int anInt = 3;

    public NodeService nodeService;
    
    protected NodeLocatorService nodeLocatorService;

    protected ContentService contentService;

    protected SearchService searchService;
    
    protected DictionaryService dictionaryService;
    
	protected static CMISConnectionManager localConnectionManager;

    //protected DictionaryLocatorService dictionaryLocatorService;

    public JavaQuery() {
        super();
        if ( instance == null ) instance = this;
    }
    
    public static void setServices( ServiceRegistry services ) {
        JavaQuery.services = services;
    }

    public void setContentService( ContentService contentService ) {
        this.contentService = contentService;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    public void setNodeService( NodeService nodeService ) {
        this.nodeService = nodeService;
    }

    public void setNodeLocatorService( NodeLocatorService nodeLocatorService ) {
        this.nodeLocatorService = nodeLocatorService;
    }

    public void setDictionaryService( DictionaryService dictionaryService ) {
        this.dictionaryService = dictionaryService;
    }

    public void setLocalConnectionManager(
			CMISConnectionManager connectionManager) {
		localConnectionManager = connectionManager;
	}


//    public void setDictionaryLocatorService( DictionaryLocatorService dictionaryLocatorService ) {
//        this.dictionaryLocatorService = dictionaryLocatorService;
//    }

    /**
     * Returns the NodeRef of "Company Home"
     * 
     * @return
     */
    public NodeRef getCompanyHome()
    {

        return nodeLocatorService.getNode("companyhome", null, null);
        
    }

    public StoreRef getStoreRefByName( String storeName ) {
        List< StoreRef > stores = nodeService.getStores();
        if ( stores == null ) return null;
        for ( StoreRef store : stores ) {
            if ( store != null
                 && store.getIdentifier().trim().equals( storeName.trim() ) ) {
                if (Debug.isOn()) Debug.outln( "getStoreRefByName(" + storeName
                             + ") found store = "
                             + MoreToString.Helper.toString( store ) );
                return store;
            } else {
//                if (Debug.isOn()) Debug.outln( "getStoreRefByName(" + storeName
//                             + ") does not match store id = "
//                             + store.getIdentifier() );
            }
        }
        if (Debug.isOn()) Debug.outln( "getStoreRefByName(" + storeName + ") failed; stores = "
                     + MoreToString.Helper.toString( stores ) );
        return null;
    }
    
    private static DynamicNamespacePrefixResolver namespacePrefixResolver1 = null;
    private static DynamicNamespacePrefixResolver namespacePrefixResolver2 = null;
    {
        namespacePrefixResolver1 = new DynamicNamespacePrefixResolver(null);
        namespacePrefixResolver2 = new DynamicNamespacePrefixResolver(null);
        namespacePrefixResolver1.registerNamespace( NamespaceService.ALFRESCO_PREFIX, NamespaceService.ALFRESCO_URI);
        namespacePrefixResolver2.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        if (Debug.isOn()) Debug.outln( "namespace prefix resolver for NamespaceService.ALFRESCO_PREFIX, NamespaceService.ALFRESCO_URI has prefixes "
                            + namespacePrefixResolver1.getPrefixes()
                            + " and URIs " + namespacePrefixResolver1.getURIs() );
        if (Debug.isOn()) Debug.outln( "namespace prefix resolver for NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI has prefixes "
                            + namespacePrefixResolver2.getPrefixes()
                            + " and URIs " + namespacePrefixResolver2.getURIs() );
    }
    
    public List< NodeRef > getNodes( String string ) {
     // A name space resolver is required - this could be the name space service
//        namespacePrefixResolver.addDynamicNamespace(NamespaceService.ALFRESCO_TEST_PREFIX, NamespaceService.ALFRESCO_TEST_URI);
        
        StoreRef storeRef = getStoreRefByName("SpacesStore");
        NodeRef rootNodeRef = nodeService.getRootNode( storeRef  );
        //rootNodeRef.isNodeRef( "foo" );
        // Select all nodes below the context node
        List< NodeRef > answer =  null;
        try {
            answer = searchService.selectNodes(rootNodeRef, string, null, namespacePrefixResolver1, false);
        } catch (Throwable e) {
            if (Debug.isOn()) Debug.outln("JavaQuery.getNode(" + string + "): failed to selectNodes; " + e.getLocalizedMessage());
            while ( e.getCause() != null && e.getCause() != e ) {
                e = e.getCause();
                if (Debug.isOn()) Debug.outln( e.getLocalizedMessage() );
            }
//            e.printStackTrace();
        }
        // Find all the property values for @alftest:animal    
        //List<Serializable> attributes = searchService.selectProperties(rootNodeRef, "//@alftest:animal", null, namespacePrefixResolver, false);
        if ( Utils.isNullOrEmpty( answer ) ) {
            //DynamicNamespacePrefixResolver namespacePrefixResolver2 = new DynamicNamespacePrefixResolver(null);
            //namespacePrefixResolver2.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
            try {
                answer = searchService.selectNodes(rootNodeRef, string, null, namespacePrefixResolver2, false);
            } catch (Throwable e) {
                if (Debug.isOn()) Debug.outln("JavaQuery.getNode(" + string + "): failed to selectNodes; " + e.getLocalizedMessage());
                while ( e.getCause() != null && e.getCause() != e ) {
                    e = e.getCause();
                    if (Debug.isOn()) Debug.outln( e.getLocalizedMessage() );
                }
//                e.printStackTrace();
            }
            if ( Utils.isNullOrEmpty( answer ) ) {
                return null;
            }
        }
        return answer;
    }
    
    private static String getType(ObjectType o) {
        if(o instanceof FolderType) {
            return "Folder";
        }
        else if(o instanceof DocumentType) {
            return "Document";
        }
        return o.toString();
    }
    
    /**
     * Tells whether the input string is valid syntax for a NodeRef id.
     * <p>
     * For example, <code>JavaQuery.isNodeRef("workspace://SpacesStore/e297594b-8c24-427a-9342-35ea702b06ff")</code> may return true, but the node may not exist.
     * @param refString
     * @return
     */
    private static boolean isNodeRef( String refString ) {
        boolean result = NodeRef.isNodeRef( refString );
        if (Debug.isOn()) Debug.outln( refString + " is " + ( result ? "" : "not " )
                            + "a NodeRef" );
        return result;
    }

    public static Collection<NodeRef> queryResultsToNodes( final List<QueryResult> results ) {
        LinkedHashSet<NodeRef> nodes = new LinkedHashSet<NodeRef>();
        if (Debug.isOn()) Debug.outln( "queryResultsToNodes(" + results + ")" );
        for (QueryResult result: results) {
            if (Debug.isOn()) Debug.outln("Properties = " + result.getProperties());
            for (PropertyData<?> data: result.getProperties()) {
                if (Debug.isOn()) Debug.outln("  Query name:" + data.getQueryName());
                if (Debug.isOn()) Debug.outln("      Values:" + data.getValues());
            }
            for(PropertyData<?> data: result.getProperties()) {
                if (Debug.isOn()) Debug.outln(data.getQueryName());
                if (Debug.isOn()) Debug.outln(data.getValues().toString());
                if (Debug.isOn()) Debug.outln("queryResultsToNodes(" + results + ") adding " + data.getValues() );
                for ( Object v : data.getValues() ) {
                    String vs = MoreToString.Helper.toString( v ); 
                    if (Debug.isOn()) Debug.outln("queryResultsToNodes(" + results + ") += " + vs );
                    if ( v instanceof NodeRef ) {
                        nodes.add( (NodeRef)v );
                        if (Debug.isOn()) Debug.outln("queryResultsToNodes(" + results + "): nodes.add(v) " + nodes );
                    } else {
                        if ( v != null && isNodeRef( vs ) ) {
                            List< NodeRef > refs = NodeRef.getNodeRefs( vs );
                            if ( refs != null ) nodes.addAll( refs );
                            if (Debug.isOn()) Debug.outln("queryResultsToNodes(" + results + "): nodes.addAll(" + refs + ") " + nodes );
                        } else {
                            if (Debug.isOn()) Debug.outln("queryResultsToNodes(" + results + "): " + vs + " is not a NodeRef!" );
                        }
                    }
                }
            }
        }
        if (Debug.isOn()) Debug.outln( "queryResultsToNodes(" + results + ") returning " + nodes );
        return nodes;
    }

    public static String toString(List<QueryResult> results) {
        //StringBuffer sb = new StringBuffer();
        List<Map<?,?>> list = queryResultsToTable( results );
        String s = MoreToString.Helper.toString( list );
        return s;
        //return sb.toString();
    }
    public static List<Map<?,?>> queryResultsToTable(List<QueryResult> results) {
        ArrayList<Map< ?, ? >> list = new ArrayList<Map<?,?>>();
        //if (Debug.isOn()) Debug.outln("Results");
        for (QueryResult result : results) {
            LinkedHashMap<String, List<?>> map = new LinkedHashMap<String, List<?>>();
            //if (Debug.isOn()) Debug.outln("Properties: " + result.getProperties());
            for (PropertyData<?> data: result.getProperties()) {
                map.put( data.getQueryName(), data.getValues() );
                //if (Debug.isOn()) Debug.outln("  Query name:" + data.getQueryName());
                //if (Debug.isOn()) Debug.outln("      Values:" + data.getValues());
            }
            list.add( map );
        }
        return list;
    }
    public static List<Map<?,?>> cmisQueryToTable(String query) {
        List<QueryResult> queryResults = cmisQuery( query );
        List<Map<?,?>> queryResultsTable = queryResultsToTable( queryResults );
        return queryResultsTable;
    }
    
    public static List<QueryResult> cmisQuery( String query ) {
        Map<String, String> parameter = new HashMap<String,String>();
        org.json.JSONObject hello = null;

        // Set the user credentials
        parameter.put(SessionParameter.USER, "admin");
        parameter.put(SessionParameter.PASSWORD, "admin");

        // Specify the connection settings
        parameter.put(SessionParameter.ATOMPUB_URL, "http://localhost:8080/view-repo/cmisatom");
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

        // Set the alfresco object factory
        parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

        // Create a session
        SessionFactory factory = SessionFactoryImpl.newInstance();
        List<Repository> repositories = factory.getRepositories(parameter);
        if (Debug.isOn()) Debug.outln(repositories.size() + " Repositories");
        for(Repository r: repositories) {
            if (Debug.isOn()) Debug.outln("  Id: " + r.getId());
            if (Debug.isOn()) Debug.outln("  Name: " + r.getName());
            if (Debug.isOn()) Debug.outln("  Description: " + r.getDescription());
        }
        if (Debug.isOn()) Debug.outln("");
        Session session = repositories.get(0).createSession();
//        Session session = localConnectionManager.getConnection().getSession();
        
        // Folder browsing example:
        Folder folder = session.getRootFolder();
        if ( folder.getChildren().iterator().hasNext() ) if (Debug.isOn()) Debug.outln("Sessions");
        for(CmisObject obj: folder.getChildren()) {
            if (Debug.isOn()) Debug.outln("  Name: " + obj.getName());
            if (Debug.isOn()) Debug.outln("  Id: " + obj.getId());
            if (Debug.isOn()) Debug.outln("  Type: " + getType(obj.getType()));
            if (Debug.isOn()) Debug.outln("");
        }
        
        // Query example:
        //"SELECT cmis:name, cmis:objectId AS MyId from cmis:document where cmis:name =  'myfile.ext'", false);
        ItemIterable< QueryResult > resultsI = session.query(query, false);
        List<QueryResult> results = new ArrayList<QueryResult>();
        for (QueryResult result : resultsI) {
            results.add( result );
        }
        if (Debug.isOn()) Debug.outln("cmisQuery(" + query + ") returning " + results );
        return results;
    }

    public static Collection<NodeRef> cmisNodeQuery( String query ) {
        Collection<NodeRef> results = queryResultsToNodes( cmisQuery( query ) );
        if (Debug.isOn()) Debug.outln("cmisNodeQuery(" + query + ") returning " + results  );
        return results;
    }
    public List<NodeRef> cmisTest(String query) {
        return query( query, SearchService.LANGUAGE_CMIS_ALFRESCO );
    }
    public List<NodeRef> xpathTest(String query) {
        List< NodeRef > nodes = getNodes(query);
        if (Debug.isOn()) Debug.outln( "testXPath: get(" + query + ") got node: "
                            + nodes );
        if ( Utils.isNullOrEmpty( nodes ) ) return nodes;
        if (Debug.isOn()) Debug.outln("SUCCEEDED!!!\n");
//        String nodeName =
//                (String)nodeService.getProperty( node, ContentModel.PROP_NAME );
//        if (Debug.isOn()) Debug.outln( "testXPath() got nodeName " + nodeName );
        return nodes;
    }    
    public List<NodeRef> luceneTest(String query) {
        return query( query, SearchService.LANGUAGE_LUCENE );
    }

    public static List< NodeRef > query( String query, String language ) {
        //String query = "PATH:\"/app:company_home/cm:Data_Dictionary//*\"";
        SearchParameters sp = new SearchParameters();
        StoreRef storeRef =
                new StoreRef( StoreRef.PROTOCOL_WORKSPACE, "SpacesStore" );
        sp.addStore( storeRef );
        sp.setLanguage( language );
        sp.setQuery( query );
        ResultSet results = null;
        List<NodeRef> nodeList = new ArrayList<NodeRef>();
        try {
            getInstance().searchService = getInstance().serviceRegistry.getSearchService();
            results = getInstance().searchService.query( sp );
            for ( ResultSetRow row : results ) {
                NodeRef currentNodeRef = row.getNodeRef();
                nodeList.add( currentNodeRef );
            }
        } catch ( Throwable e ) {
            if (Debug.isOn()) Debug.outln( "JavaQuery.query(" + query + ", " + language
                                + "): failed to selectNodes; "
                                + e.getLocalizedMessage() );
            while ( e.getCause() != null && e.getCause() != e ) {
                e = e.getCause();
                if (Debug.isOn()) Debug.outln( e.getLocalizedMessage() );
            }
        } finally {
            if ( results != null ) {
                results.close();
            }
        }
        return nodeList;
    }

    public NodeRef oldGetNode( String string ) {
        Map< String, Serializable > params = new TreeMap<String, Serializable>();
//        //int pos = string.indexOf( '/' );
//        String[] storePath = string.split( "/", 3 );
//        String storeName = null;
//        if ( storePath != null && storePath.length > 2 ) {
//            Assert.assertEquals(storePath.length, 3);
//            storeName = storePath[1];
//            params.put( XPathNodeLocator.STORE_ID_KEY, storeName );
//            string = "/" + storePath[2];
//        }
        params.put( XPathNodeLocator.QUERY_KEY, string );
        //nodeService.findNodes( params );
        NodeRef source = getCompanyHome();
//        if ( storeName != null ) {
//            StoreRef storeRef = getStoreRefByName(storeName);
//            rootNodes = nodeService.getAllRootNodes( storeRef );
//            
//            //nodeLocatorService.ge
//            //source = nodeService.getRootNode(  )
//        }
        return nodeLocatorService.getNode( "xpath", source, params );
    }

    // JUnit

    //@Override
    public NodeRef getElement( NodeRef context, String identifier ) {//, String version ) {
        List<NodeRef> nodes = get( identifier );
        if ( Utils.isNullOrEmpty( nodes ) ) {
            if ( Debug.isOn() ) Debug.errln( "CMIS getObject(): Could not find node " + identifier + "!" );
            return null;
        }
        if ( nodes.size() > 1 ) {
            if ( Debug.isOn() ) Debug.errln( "CMIS getObject(): Got multiple objects for identifier " + identifier + "! Returning first of " + MoreToString.Helper.toString( nodes ) );
        }
        return nodes.get( 0 );
    }

    //@Override
    public String getElementId( NodeRef object ) {//, String version ) {
        return object.getId();
    }

    //@Override
    public String getName( NodeRef object ) {//, String version ) {
        return (String)NodeUtil.getNodeProperty( object, ContentModel.PROP_NAME,
                                                 services, true, true );
    }

    //@Override
    public String getTypeOf( NodeRef object ) {//, String version ) {
        QName type = nodeService.getType( object );
        if ( type == null ) return null;
        return type.toPrefixString();
    }

    //@Override
    public Collection< Object > getTypeProperties( String typeName ) {//,
                                                   //String version ) {
        if ( typeName == null ) return null;
        TypeDefinition type = getType(typeName);
        Map< QName, PropertyDefinition > props = type.getProperties();
        if ( props == null ) return null;
        List<Object> propObjs = new ArrayList<Object>();
        propObjs.addAll( props.values() );
        return propObjs;
    }

    public TypeDefinition getType( String typeName ) {
        if ( typeName == null ) return null;
        QName typeQName = QName.createQName( typeName );
        TypeDefinition type = dictionaryService.getType( typeQName );
        return type;
    }
    
    public Collection< Object > getProperties( NodeRef object ) {//, String version ) {
        Map< QName, Serializable > props = nodeService.getProperties( object );
        if ( props == null ) return null;
        //return props.values();
        List<Object> propObjs = new ArrayList<Object>();
        propObjs.addAll( props.values() );
        return propObjs;
    }

    public Serializable getProperty( NodeRef object, String propertyName ) {//,
                                     //String version ) {
        Serializable prop =
                (Serializable)NodeUtil.getNodeProperty( object, propertyName,
                                                        services, true, true );
//                nodeService.getProperty( object,
//                                         QName.createQName( propertyName ) );
        return prop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.nasa.jpl.view_repo.ModelInterface#getRelationships(java.lang.Object)
     */
    public Collection< AssociationRef > getRelationships( NodeRef object ) {//,
                                                          //String version ) {
        List< AssociationRef > sassocs =
                nodeService.getSourceAssocs( object,
                                             new RegexQNamePattern( ".*" ) );
        List< AssociationRef > tassocs =
                nodeService.getTargetAssocs( object,
                                             new RegexQNamePattern( ".*" ) );
        List< AssociationRef > assocs = new ArrayList< AssociationRef >();
        assocs.addAll( sassocs );
        assocs.addAll( tassocs );
        return assocs;
    }

    //@Override
    public Collection<AssociationRef> getRelationships( NodeRef object,
                                                        String relationshipName ) {//,
                                                        //String version ) {
        List< AssociationRef > results = new ArrayList<AssociationRef>();
        List< AssociationRef > assocs1 = nodeService.getSourceAssocs( object, QName.createQName( relationshipName) );
        if ( !Utils.isNullOrEmpty( assocs1 ) ) {
            //return assocs1.get( 0 );
            results.addAll( assocs1 );
        }
        List< AssociationRef > assocs2 = nodeService.getTargetAssocs( object, QName.createQName( relationshipName) );
        if ( !Utils.isNullOrEmpty( assocs2 ) ) {
            //return assocs2.get( 0 );
            results.addAll( assocs2 );
        }
        return results;
    }
    
    //@Override
    public Collection< NodeRef > getRelated( NodeRef object,
                                             String relationshipName ) {//,
                                             //String version ) {
        Set< NodeRef > results = new LinkedHashSet<NodeRef>();
        List< AssociationRef > assocs = nodeService.getSourceAssocs( object, QName.createQName( relationshipName) );
        if ( !Utils.isNullOrEmpty( assocs ) ) {
            for ( AssociationRef ref : assocs ) {
                results.add( ref.getSourceRef() );
            }
        }
        assocs = nodeService.getTargetAssocs( object, QName.createQName( relationshipName) );
        if ( !Utils.isNullOrEmpty( assocs ) ) {
            for ( AssociationRef ref : assocs ) {
                results.add( ref.getTargetRef() );
            }
        }
        return results;
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (Debug.isOn()) Debug.outln( "JavaQuery has been executed (although it does nothing by itself)" );
        //JavaQueryTest.log.debug( "Test debug logging. Congratulation your AMP is working" );
        //JavaQueryTest.log.info( "This is only for information purposed. Better remove me from the log in Production" );
    }
    
    public static JavaQuery getInstance() {
        if ( instance == null || applicationContext == null ) {
            instance = JavaQuery.initAppContext();
        }
        return instance;
    }

    public static Object get(String xpath, QName property, boolean cacheOkay) {
        if (Debug.isOn()) Debug.outln( "get(" + xpath + ", " + property + ")" );
        List<NodeRef> nodes = get( xpath );
        assertNotNull( nodes );
        NodeRef node = nodes.get( 0 );
        Object nodeName = NodeUtil.getNodeProperty( node, property,
                                                    getInstance().serviceRegistry,
                                                    true, true );
        assertNotNull( nodeName );
        assertEquals( nodeName, nodeName );
        return nodeName;
    }

    public static List<NodeRef> get(String xpath) {
        List<NodeRef> nodes = getInstance().getNodes( xpath ); // ( JavaQueryTest.theNodePath + "/" + JavaQueryTest.theNodeName );
        return nodes;
    }

    public static JavaQuery initAppContext() {
        // TODO: Make testing properly working without need for helpers
        // TODO: Provide this in an SDK base class
        JavaQuery javaQueryComponent = null;
        ApplicationContextHelper.setUseLazyLoading( false );
        ApplicationContextHelper.setNoAutoStart( false );
        String[] contextPath = new String[] { "classpath:alfresco/application-context.xml" };
        try {
            instance = javaQueryComponent;
            if ( services == null ) {
                if ( applicationContext == null ) {
                    applicationContext =
                            ApplicationContextHelper.getApplicationContext( contextPath );
                }
                javaQueryComponent =
                        (JavaQuery)applicationContext.getBean( "java_query" );
                services = (ServiceRegistry)applicationContext.getBean( "ServiceRegistry" );
            } else {
                javaQueryComponent = (JavaQuery)services.getService( QName.createQName( "java_query" ) );
            }
            if ( services != null ) {
                javaQueryComponent.services = services;
                javaQueryComponent.nodeService = services.getNodeService();
                javaQueryComponent.nodeLocatorService =services.getNodeLocatorService();
                javaQueryComponent.searchService = services.getSearchService();
                javaQueryComponent.contentService = services.getContentService();
                javaQueryComponent.dictionaryService = services.getDictionaryService();
            } else {
                javaQueryComponent.nodeService =
                        (NodeService)applicationContext.getBean( "NodeService" );
                javaQueryComponent.nodeLocatorService =
                        (NodeLocatorService)applicationContext.getBean( "nodeLocatorService" );
                javaQueryComponent.searchService =
                        (SearchService)applicationContext.getBean( "SearchService" );
                javaQueryComponent.contentService =
                        (ContentService)applicationContext.getBean( "ContentService" );
                javaQueryComponent.dictionaryService =
                        (DictionaryService)applicationContext.getBean( "DictionaryService" );
            }
            
            AuthenticationUtil.setFullyAuthenticatedUser( ADMIN_USER_NAME );
            log.debug( "Sample test logging: Application Context properly loaded for JavaQuery" );
        } catch ( AlfrescoRuntimeException e ) {
            e.printStackTrace();
        }
        return javaQueryComponent;
    }


}
