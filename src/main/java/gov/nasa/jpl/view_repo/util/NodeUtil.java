package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.CompareUtils.GenericComparator;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService.FindNodeParameters;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.digester.SetRootRule;
import org.mozilla.javascript.Scriptable;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.Status;

import com.ibm.icu.impl.LinkedHashMap;

public class NodeUtil {
    
    public enum SearchType {
        DOCUMENTATION( "@sysml\\:documentation:\"" ),
        NAME( "@sysml\\:name:\"" ),
        ID( "@sysml\\:id:\"" ),
        STRING( "@sysml\\:string:\"" ),
        BODY( "@sysml\\:body:\"" ),
        CHECKSUM( "@view\\:cs\"" );
        
        public String prefix;

        SearchType( String prefix ) {
            this.prefix = prefix;
        }
    };

    public static ServiceRegistry services = null;
    
    // needed for Lucene search
    public static StoreRef SEARCH_STORE = null;
            //new StoreRef( StoreRef.PROTOCOL_WORKSPACE, "SpacesStore" );

    public static StoreRef getStoreRef() {
        if ( SEARCH_STORE == null ) {
            SEARCH_STORE =
                    new StoreRef( StoreRef.PROTOCOL_WORKSPACE, "SpacesStore" );
        }
        return SEARCH_STORE;
    }
    
    public static ApplicationContext getApplicationContext() {
        String[] contextPath =
                new String[] { "classpath:alfresco/application-context.xml" };
        ApplicationContext applicationContext =
                ApplicationContextHelper.getApplicationContext( contextPath );
        return applicationContext;
    }
    
    public static ServiceRegistry getServices() {
        return getServiceRegistry();
    }
    public static void setServices( ServiceRegistry services ) {
        NodeUtil.services = services;
    }

    public static ServiceRegistry getServiceRegistry() {
        if ( services == null ) {
            services = (ServiceRegistry)getApplicationContext().getBean( "ServiceRegistry" );
        }
        return services;
    }

    public static Collection<EmsScriptNode> luceneSearchElements(String queryPattern ) {
        ArrayList<EmsScriptNode> nodes = new ArrayList<EmsScriptNode>();
        ResultSet resultSet = luceneSearch( queryPattern, (SearchService)null );
        System.out.println( "luceneSearch(" + queryPattern + ") returns "
                            + resultSet.length() + " matches." );
       for ( ResultSetRow row : resultSet ) {
            EmsScriptNode node =
                    new EmsScriptNode( row.getNodeRef(), getServices() );
            nodes.add( node );
        }
        return nodes;
    }
    
    public static ResultSet luceneSearch(String queryPattern ) {
        return luceneSearch( queryPattern, (SearchService)null );
    }
    public static ResultSet luceneSearch( String queryPattern,
                                          ServiceRegistry services ) {
        if ( services == null ) services = getServices();
        return luceneSearch( queryPattern,
                             services == null ? null
                                              : services.getSearchService() );
    }
    public static ResultSet luceneSearch(String queryPattern,
                                         SearchService searchService ) {
        if ( searchService == null ) {
            if ( getServiceRegistry() != null ) {
                searchService = getServiceRegistry().getSearchService();
            }
        }
        ResultSet results = null;
        if ( searchService != null ) { 
            results = searchService.query( getStoreRef(),
                                           SearchService.LANGUAGE_LUCENE,
                                           queryPattern );
        }
        return results;
    }
    
    protected static ResultSet findNodeRefsByType(String name, SearchType type,
                                                  ServiceRegistry services) {
        return findNodeRefsByType( name, type.prefix, services );
    }
    protected static ResultSet findNodeRefsByType(String name, String prefix,
                                                  ServiceRegistry services) {
        ResultSet results = null;
        String queryPattern = prefix + name + "\"";
        results = luceneSearch( queryPattern, services );
        return results;
    }

    public static NodeRef findNodeRefByType(String name, SearchType type, ServiceRegistry services) {
        return findNodeRefByType( name, type.prefix, services );
    }
    
    public static NodeRef findNodeRefByType(String name, String prefix, ServiceRegistry services) {
        ResultSet results = null;
        NodeRef nodeRef = null;
        try {
            results = findNodeRefsByType( name, prefix, services );
            if (results != null) {
                //boolean nameMatches = false;
                for (ResultSetRow row: results) {
                    NodeRef nr = row.getNodeRef();
//                    if ( nodeRef == null ) nodeRef = nr;
//                    else {
                        // Make sure we didn't just get a near match.
                        EmsScriptNode esn = new EmsScriptNode( nr, getServices() );
                        try {
                        String acmType = Utils.join( prefix.split( "[\\W]+" ), ":").replaceFirst( "^:", "" );
                        Object o = esn.getProperty( acmType );
                        if ( ( "" + o ).equals( name ) ) {
                            nodeRef = nr;
                            break;
                        }
                        } catch ( Throwable e ) {
                            e.printStackTrace();
                        }
//                    }
                }
            }
        } finally {
            if (results != null) {
                results.close();
            }
        }

        return nodeRef;     
    }
    
    /**
     * Find a NodeReference by name (returns first match, assuming things are unique)
     * 
     * @param id Node name to search for
     * @return     NodeRef of first match, null otherwise
     */
    public static NodeRef findNodeRefById(String id, ServiceRegistry services) {
        NodeRef r = findNodeRefByType(id, SearchType.ID, services); // TODO: temporarily search by ID
        if ( r == null ) r = findNodeRefByType(id, "@cm\\:name:\"", services);
        return r;
    }
    
    
    /**
     * Perform Lucene search for the specified pattern and ACM type
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type      escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    public static Map< String, EmsScriptNode >
    searchForElements( String pattern,
                       ServiceRegistry services, StringBuffer response,
                       Status status) {
        Map<String, EmsScriptNode> elementsFound = new HashMap<String, EmsScriptNode>();
        for (SearchType searchType: SearchType.values() ) {
            elementsFound.putAll(searchForElements(searchType.prefix, pattern, services, response, status));
        }
        return elementsFound;
    }
    
    /**
     * Perform Lucene search for the specified pattern and ACM type
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type      escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    public static Map< String, EmsScriptNode >
            searchForElements( String type, String pattern,
                               ServiceRegistry services, StringBuffer response,
                               Status status ) {
        Map<String, EmsScriptNode> searchResults = new HashMap<String, EmsScriptNode>();

        ResultSet resultSet = null;
        try {

            resultSet = findNodeRefsByType(pattern, type, getServices());

//            pattern = type + pattern + "\"";
//            resultSet =
//                    services.getSearchService().query( getStoreRef(),
//                                                       SearchService.LANGUAGE_LUCENE,
//                                                       pattern );
            for ( ResultSetRow row : resultSet ) {
                EmsScriptNode node =
                        new EmsScriptNode( row.getNodeRef(), services, response );
                if ( node.checkPermissions( PermissionService.READ, response, status ) ) {
                    String id = (String)node.getProperty( Acm.ACM_ID );
                    if ( id != null ) {
                        searchResults.put( id, node );
                    }
                }
            }
        } catch (Exception e) {
            if ( response != null || status != null ) {
                String msg = "Error! Could not parse search: " + pattern + ".\n"
                             + e.getLocalizedMessage();
                System.out.println(msg);
                e.printStackTrace( System.out );
                if ( response != null ) response.append( msg );
                if ( status != null ) status.setCode( HttpServletResponse.SC_BAD_REQUEST,
                                                      msg );
            } else {
                e.printStackTrace();
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }

        return searchResults;
    }
    
    /**
     * This method behaves the same as if calling
     * {@link #isType(String, ServiceRegistry)} while passing null as the
     * ServiceRegistry.
     * 
     * @param typeName
     * @return true if and only if a type is defined in the content model with a
     *         matching name.
     */
    public static boolean isType( String typeName ) {
        return isType( typeName, null );
    }

    /**
     * Determine whether the input type name is a type in the content model (as
     * opposed to an aspect, for example).
     * 
     * @param typeName
     * @param services
     * @return true if and only if a type is defined in the content model with a
     *         matching name.
     */
    public static boolean isType( String typeName, ServiceRegistry services ) {
        if ( typeName == null ) return false;
        if ( Acm.getJSON2ACM().keySet().contains( typeName ) ) {
            typeName = Acm.getACM2JSON().get( typeName );
        }
//        String[] split = typeName.split( ":" );
//        
//        String nameSpace = null;
//        String localName = null;
//        if ( split.length == 2 ) {
//            nameSpace = split[0];
//            localName = split[1];
//        } else if ( split.length == 1 ) {
//            localName = split[0];
//        } else {
//            Debug.error(true, false, "Bad type name " + typeName );
//            return false;
//        }
//        if ( localName == null ) {
//            Debug.error(true, false, "Bad type name " + typeName );
//            return false;
//        }
        if ( services == null ) services = getServices();
        DictionaryService dServ = services.getDictionaryService();
//        Collection< QName > types = dServ.getAllTypes();
//        //{http://www.alfresco.org/model/transfer/1.0}
        QName qName = createQName( typeName, services );
        if ( qName != null ) {
            TypeDefinition t = dServ.getType( qName );
            if ( t != null ) {
//              System.out.println("\n\n*** getType(" + typeName + ") worked!!!\n" );
              return true;
            }
        }
//        //        System.out.println("all types: " + types);
////        TypeDefinition t = dServ.getType( QName.createQName( typeName ) );
////        System.out.println("getType(" + typeName + ") = " + t );
//        for ( QName type : types ) {
////            System.out.println( "getLocalName() = " + type.getLocalName() );
////            System.out.println( "getPrefixString() = " + type.getPrefixString() );
////            System.out.println( "toPrefixString() = " + type.toPrefixString() );
////            System.out.println( "toString() = " + type.toString() );
//            if ( typeName.equals( type.getPrefixString() ) ) {
//                System.out.println("isType(" + typeName + ") = true");
//                return true;
//            }
//        }
//        System.out.println("isType(" + typeName + ") = false");
        return false;
    }
    
    /**
     * This method behaves the same as if calling
     * {@link #isAspect(String, ServiceRegistry)} while passing null as the
     * ServiceRegistry.
     * 
     * @param aspectName
     * @return true if and only if an aspect is defined in the content model
     *         with a matching name.
     */
    public static boolean isAspect( String aspectName ) {
        return isAspect( aspectName, null );
    }

    /**
     * Determine whether the input aspect name is an aspect in the content model
     * (as opposed to a type, for example).
     * 
     * @param aspectName
     * @param services
     * @return true if and only if an aspect is defined in the content model
     *         with a matching name.
     */
    public static boolean isAspect( String aspectName, ServiceRegistry services ) {
        if ( Acm.getJSON2ACM().keySet().contains( aspectName ) ) {
            aspectName = Acm.getACM2JSON().get( aspectName );
        }
        if ( services == null ) services = getServices();
        DictionaryService dServ = services.getDictionaryService();

        QName qName = createQName( aspectName, services );
        if ( qName != null ) {
            AspectDefinition t = dServ.getAspect( qName );
            if ( t != null ) {
              System.out.println("\n\n*** getAspect(" + aspectName + ") worked!!!\n" );
              return true;
            }
        }
        
        Collection< QName > aspects = dServ.getAllAspects();
        System.out.println("all aspects: " + aspects);

        for ( QName aspect : aspects ) {
            if ( aspect.getPrefixString().equals( aspectName ) ) {
                System.out.println("\n\n*** getAspect(" + aspectName + ") returning true\n" );
                return true;
            }
        }
        System.out.println("\n\n*** getAspect(" + aspectName + ") returning false\n" );
        return false;
    }

    /**
     * Given a long-form QName, this method uses the namespace service to create a
     * short-form QName string.
     * <p>
     * Copied from {@link ScriptNode#getShortQName(QName)}.
     * @param longQName
     * @return the short form of the QName string, e.g. "cm:content"
     */
    public static String getShortQName(QName longQName)
    {
        return longQName.toPrefixString(getServices().getNamespaceService());
    }
    
    /**
     * Helper to create a QName from either a fully qualified or short-name
     * QName string
     * <P>
     * Copied from {@link ScriptNode#createQName(QName)}.
     * 
     * @param s
     *            Fully qualified or short-name QName string
     * 
     * @return QName
     */
    public static QName createQName(String s) {
        return createQName( s, null );
    }
    
    /**
     * Helper to create a QName from either a fully qualified or short-name
     * QName string
     * <P>
     * Copied from {@link ScriptNode#createQName(QName)}.
     * 
     * @param s
     *            Fully qualified or short-name QName string
     * 
     * @param services
     *            ServiceRegistry for getting the service to resolve the name
     *            space
     * @return QName
     */
    public static QName createQName(String s, ServiceRegistry services )
    {
        if ( s == null ) return null;
        if ( Acm.getJSON2ACM().keySet().contains( s ) ) {
            s = Acm.getACM2JSON().get( s );
        }
        QName qname;
        if (s.indexOf("{") != -1)
        {
            qname = QName.createQName(s);
        }
        else
        {
            if ( services == null ) services = getServices();
            qname = QName.createQName(s, getServices().getNamespaceService());
        }
        return qname;
    }

    
//    public static QName makeContentModelQName( String cmName ) {
//        return makeContentModelQName( cmName, null );
//    }
//    public static QName makeContentModelQName( String cmName, ServiceRegistry services ) {
//        if ( services == null ) services = getServices();
//        
//        if ( cmName == null ) return null;
//        if ( Acm.getJSON2ACM().keySet().contains( cmName ) ) {
//            cmName = Acm.getACM2JSON().get( cmName );
//        }
//        String[] split = cmName.split( ":" );
//        
//        String nameSpace = null;
//        String localName = null;
//        if ( split.length == 2 ) {
//            nameSpace = split[0];
//            localName = split[1];
//        } else if ( split.length == 1 ) {
//            localName = split[0];
//        } else {
//            return null;
//        }
//        if ( localName == null ) {
//            return null;
//        }
//        DictionaryService dServ = services.getDictionaryService();
//        QName qName = null;
//        if ( nameSpace != null ) {
//            if ( nameSpace.equals( "sysml" ) ) {
//                qName = QName.createQName( "{http://jpl.nasa.gov/model/sysml-lite/1.0}"
//                                           + localName );
//            } else if ( nameSpace.equals( "view2" ) ) {
//                qName = QName.createQName( "{http://jpl.nasa.gov/model/view/2.0}"
//                                           + localName );
//            } else if ( nameSpace.equals( "view" ) ) {
//                qName = QName.createQName( "{http://jpl.nasa.gov/model/view/1.0}"
//                                           + localName );
//            }
//        }
//        return qName;
//    }
    
    /**
     * This method behaves the same as if calling
     * {@link #getContentModelTypeName(String, ServiceRegistry)} while passing
     * null as the ServiceRegistry.
     * 
     * @param typeOrAspectName
     * @return the input type name if it matches a type; otherwise,
     *         return the Element type, assuming that all nodes are Elements
     */
    public static String getContentModelTypeName( String typeOrAspectName ) {
        return getContentModelTypeName( typeOrAspectName, null );
    }

    /**
     * Get the type defined in the Alfresco content model. Return the input type
     * name if it matches a type; otherwise, return the Element type, assuming
     * that all nodes are Elements.
     * 
     * @param typeOrAspectName
     * @return
     */
    public static String getContentModelTypeName( String typeOrAspectName,
                                                  ServiceRegistry services ) {
        if ( services == null ) services = getServices();
        if ( Acm.getJSON2ACM().keySet().contains( typeOrAspectName ) ) {
            typeOrAspectName = Acm.getACM2JSON().get( typeOrAspectName );
        }
        String type = typeOrAspectName;
        boolean notType = !NodeUtil.isType( type, services ); 
        if ( notType ) {
            type = Acm.ACM_ELEMENT;
        }
        return type;
    }
    
    /**
     * Get site of specified short name
     * @param siteName
     * @return  ScriptNode of site with name siteName
     */
    public static EmsScriptNode getSiteNode( String siteName,
                                             ServiceRegistry services,
                                             StringBuffer response ) {
        if ( Utils.isNullOrEmpty( siteName ) ) return null;
        SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        if (siteInfo != null) {
            return new EmsScriptNode(siteInfo.getNodeRef(), services, response);
        }
        return null;
    }

    public static EmsScriptNode getCompanyHome( ServiceRegistry services ) {
        EmsScriptNode companyHome = null;
        if ( services == null ) services = getServiceRegistry();
        if ( services == null || services.getNodeLocatorService() == null ) {
            System.out.println( "getCompanyHome() failed, no services or no nodeLocatorService: "
                                + services );
        }
        NodeRef companyHomeNodeRef =
                services.getNodeLocatorService().getNode( "companyhome", null,
                                                          null );
        if ( companyHomeNodeRef != null ) {
            companyHome = new EmsScriptNode( companyHomeNodeRef, services );
        }
        return companyHome;
    }
    
    public static Set< NodeRef > getRootNodes(ServiceRegistry services ) {
        return services.getNodeService().getAllRootNodes( getStoreRef() );
    }

    /**
     * Find an existing NodeRef with the input Alfresco id.
     * 
     * @param id
     *            an id similar to
     *            workspace://SpacesStore/e297594b-8c24-427a-9342-35ea702b06ff
     * @return If there a node exists with the input id in the Alfresco
     *         database, return a NodeRef to it; otherwise return null. An error
     *         is printed if the id doesn't have the right syntax.
     */
    public static NodeRef findNodeRefByAlfrescoId(String id) {
        if ( !NodeRef.isNodeRef( id ) ) {
            Debug.error("Bad NodeRef id: " + id );
            return null;
        }
        NodeRef n = new NodeRef(id);
        EmsScriptNode node = new EmsScriptNode( n, getServices() );
        if ( !node.exists() ) return null;
        return n;
    }

//    private static <T> int indexedBinarySearchLower(List<? extends T> l, T key, Comparator<? super T> c) {
//        int low = 0;
//        int high = l.size()-1;
//
//        while (low <= high) {
//            int mid = (low + high) >>> 1;
//            T midVal = l.get(mid);
//            int cmp = c.compare(midVal, key);
//
//            if (cmp < 0)
//                low = mid + 1;
//            else if (cmp > 0)
//                high = mid - 1;
//            else
//                return mid; // key found
//        }
//        return -(low + 1);  // key not found
//    }


    
    public static class VersionLowerBoundComparator implements Comparator< Object > {
        
        @Override
        public int compare( Object o1, Object o2 ) {
            Date d1 = null;
            Date d2 = null;
            if ( o1 instanceof Version ) {
                d1 = ((Version)o1).getFrozenModifiedDate();
            } else if ( o1 instanceof Date ) {
                d1 = (Date)o1;
            } else if ( o1 instanceof Long ) {
                d1 = new Date( (Long)o1 );
            } else if ( o1 instanceof String ) {
                d1 = TimeUtils.dateFromTimestamp( (String)o1 );
            }
            if ( o2 instanceof Version ) {
                d2 = ((Version)o2).getFrozenModifiedDate();
            } else if ( o2 instanceof Date ) {
                d2 = (Date)o2;
            } else if ( o2 instanceof Long ) {
                d2 = new Date( (Long)o2 );
            } else if ( o1 instanceof String ) {
                d2 = TimeUtils.dateFromTimestamp( (String)o2 );
            }
            // more recent first
            return -GenericComparator.instance().compare( d1, d2 );
        }
    }
    
    public static VersionLowerBoundComparator versionLowerBoundComparator =
            new VersionLowerBoundComparator();
    
    public static NodeRef getNodeRefAtTime( String id, String timestamp ) {
        NodeRef ref = findNodeRefById( id, getServices() );
        return getNodeRefAtTime( ref, timestamp );
    }

    public static NodeRef getNodeRefAtTime( NodeRef ref, String timestamp ) {
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        return getNodeRefAtTime( ref, dateTime );
    }
    public static NodeRef getNodeRefAtTime( NodeRef ref, Date dateTime ) {
        VersionHistory history = getServices().getVersionService().getVersionHistory( ref );
        Collection< Version > versions = history.getAllVersions();
        Vector<Version> vv = new Vector<Version>( versions );
        int index = Collections.binarySearch( vv, dateTime, versionLowerBoundComparator );
        Version v = null;
        if ( index < 0 ) {
            // search returned index = -(lowerBound+1), so lowerbound = -index-1
            index = -index - 1;
        }
        v = vv.get( index );
        return v.getVersionedNodeRef();
    }

    
    /**
     * Find or create a folder
     * 
     * @param source
     *            node within which folder will have been created; may be null
     * @param path
     *            folder path as folder-name1 + '/' + folder-name2 . . .
     * @return the existing or new folder
     */
    public static EmsScriptNode mkdir( EmsScriptNode source, String path,
                                       ServiceRegistry services,
                                       StringBuffer response, Status status ) {
        EmsScriptNode result = null;
        // if no source is specified, see if path include the site
        if ( source == null ) {
            Pattern p = Pattern.compile( "(Sites)?/?(\\w*)" );
            Matcher m = p.matcher( path );
            if ( m.matches() ) {
                String siteName = m.group( 1 ); 
                source = getSiteNode( siteName, services, response );
                if ( source != null ) {
                    result = mkdir( source, path, services, response, status );
                    if ( result != null ) return result;
                    source = null;
                }
            }
        }
        // if no source is identified, see if path is from the company home
        if ( source == null ) {
            source = getCompanyHome( services );
            if ( source != null ) {
                result = mkdir( source, path, services, response, status );
                if ( result != null ) return result;
                source = null;
            }
        }
        // if no source is identified, see if path is from a root node
        if ( source == null ) {
            Set< NodeRef > roots = getRootNodes( services );
            for ( NodeRef ref : roots ) {
                source = new EmsScriptNode( ref, services, response, status );
                result = mkdir( source, path, services, response, status );
                if ( result != null ) return result;
                source = null;
            }
        }
        if ( source == null ) {
            log( "Can't determine source node of path " + path + "!", response );
            return null;
        }
        // find the folder corresponding to the path beginning from the source node
        EmsScriptNode folder = source.childByNamePath( path );
        if ( folder == null ) {
            String[] arr = path.split( "/" );
            for ( String p : arr ) {
                folder = source.childByNamePath(p);
                if (folder == null) {
                    folder = source.createFolder( p );
                    if ( folder == null ) {
                        log( "Can't create folder for path " + path + "!", response );
                        return null;
                    }
                }
                source = folder;
            }
        }
        return folder;
    }
    
    /**
     * Append onto the response for logging purposes
     * @param msg   Message to be appened to response
     * TODO: fix logger for EmsScriptNode
     */
    public static void log(String msg, StringBuffer response ) {
//      if (response != null) {
//          response.append(msg + "\n");
//      }
    }


}
