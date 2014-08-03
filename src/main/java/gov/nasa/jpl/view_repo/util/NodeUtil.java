package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.CompareUtils.GenericComparator;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptNode.NodeValueConverter;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.repo.jscript.ValueConverter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ApplicationContextHelper;
import org.mozilla.javascript.Scriptable;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.Status;

public class NodeUtil {
    
    public enum SearchType {
        DOCUMENTATION( "@sysml\\:documentation:\"" ),
        NAME( "@sysml\\:name:\"" ),
        CM_NAME( "@cm\\:name:\"" ),
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
        ResultSet resultSet = luceneSearch( queryPattern, (SearchService)null );
        if (Debug.isOn()) System.out.println( "luceneSearch(" + queryPattern + ") returns "
                            + resultSet.length() + " matches." );
        return resultSetToList( resultSet );
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
        if ( Debug.isOn() ) {
            Debug.outln( "luceneSearch(" + queryPattern + "): returned "
                         + results.length() + " nodes." );//resultSetToList( results ) );
        }
        return results;
    }
    
    public static List<EmsScriptNode> resultSetToList( ResultSet results ) {
        ArrayList<EmsScriptNode> nodes = new ArrayList<EmsScriptNode>();
       for ( ResultSetRow row : results ) {
            EmsScriptNode node =
                    new EmsScriptNode( row.getNodeRef(), getServices() );
            nodes.add( node );
        }
        return nodes;

    }
    public static String resultSetToString( ResultSet results ) {
        return "" + resultSetToList( results );
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
        //if ( (results == null || results.length() == 0 ) && pref)
        return results;
    }

    public static NodeRef findNodeRefByType( String name, SearchType type,
                                             WorkspaceNode workspace,
                                             Date dateTime, boolean exactMatch,
                                             ServiceRegistry services ) {
        return findNodeRefByType( name, type.prefix, workspace, dateTime,
                                  exactMatch, services );
    }
    
    public static NodeRef findNodeRefByType( String specifier, String prefix,
                                             WorkspaceNode workspace,
                                             Date dateTime, boolean exactMatch,
                                             ServiceRegistry services ) {
        ArrayList< NodeRef > refs =
                findNodeRefsByType( specifier, prefix, workspace, dateTime, true,
                                    exactMatch, services );
        if ( Utils.isNullOrEmpty( refs ) ) return null;
        NodeRef ref = refs.get( 0 );
        return ref;
    }

    public static ArrayList< NodeRef >
            findNodeRefsByType( String specifier, String prefix,
                                WorkspaceNode workspace, Date dateTime,
                                boolean justFirst, boolean exactMatch,
                                ServiceRegistry services ) {
        ResultSet results = null;
        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        NodeRef nodeRef = null;
        try {
            results = findNodeRefsByType( specifier, prefix, services );
            if (results != null) {
                NodeRef lowest = null;
                for (ResultSetRow row: results) {
                    NodeRef nr = row.getNodeRef();
                    if ( nr == null ) continue;
                    EmsScriptNode esn = new EmsScriptNode( nr, getServices() );

                    if ( Debug.isOn() ) {
                        Debug.turnOff();
                        System.out.println( "findNodeRefsByType(" + specifier
                                            + ", " + prefix + ", " + workspace
                                            + ", " + dateTime + ", justFirst="
                                            + justFirst + ", exactMatch="
                                            + exactMatch + "): candidate "
                                            + esn.getWorkspaceName()// false )
                                            + "::" + esn.getName() );
                        Debug.turnOn();
                    }

                    // Get the version for the date/time if specified.
                    if ( dateTime != null ) {
                        nr = getNodeRefAtTime( nr, dateTime );
                    }

                    if ( nr == null ) {
                        if ( Debug.isOn() ) {
                            Debug.outln( "findNodeRefsByType(): no nodeRef at time "
                                         + dateTime );
                        }
                        continue;
                    }
                    
                        esn = new EmsScriptNode( nr, getServices() );
                        if ( !esn.exists() ) {
                            if ( Debug.isOn() ) {
                                Debug.turnOff();
                                System.out.println( "findNodeRefsByType(): element does not exist "
                                             + esn );
                                Debug.turnOn();
                            }

                            continue;
                        }

                        // Make sure it's in the right workspace.
                        if ( workspace != null && !workspace.contains( esn ) ) {
                            if ( Debug.isOn() ) {
                                Debug.turnOff();
                                System.out.println( "findNodeRefsByType(): wrong workspace "
                                             + workspace );
                                Debug.turnOn();
                            }

                            continue;
                        }
                        
                        // Make sure we didn't just get a near match.
                        try {
                            if ( !esn.checkPermissions( PermissionService.READ ) ) {
                                continue;
                            }
                            boolean match = true;
                            if ( exactMatch ) {
                                String acmType =
                                        Utils.join( prefix.split( "[\\W]+" ), ":" )
                                             .replaceFirst( "^:", "" );
                                Object o = esn.getProperty( acmType );
                                if ( !( "" + o ).equals( specifier ) ) {
                                    match = false;
                                }
                            }
                            if ( match ) {
                                nodeRef = nr;
                                if ( exists(workspace) && (!exists(lowest) || isWorkspaceSource(lowest, nodeRef) ) ) {
                                    lowest = nodeRef;
                                    nodeRefs.add( 0, nodeRef );
                                } else {
                                    nodeRefs.add( nodeRef );
                                }
                                if ( Debug.isOn() ) {
                                    Debug.outln( "findNodeRefsByType(): matched!" );
                                 }
                                if ( justFirst && 
                                     ( !exists( workspace ) ||
                                       ( exists( lowest ) && 
                                               workspace.equals( getWorkspace( nodeRef ) ) ) ) ) {
                                    break;
                                }
                            } else {
                                if ( Debug.isOn() ) {
                                   Debug.outln( "findNodeRefsByType(): not an exact match" );
                                }
                            }

                        } catch ( Throwable e ) {
                            e.printStackTrace();
                        }
//                    }
                }
            }
        } finally {
            if (results != null) {
                if ( Debug.isOn() ) {
                    List< EmsScriptNode > set =
                            EmsScriptNode.toEmsScriptNodeList( nodeRefs,
                                                               services, null,
                                                               null );
                    if ( Debug.isOn() ) {
                        Debug.turnOff();
                        System.out.println( "findNodeRefsByType(" + specifier
                                            + ", " + prefix + ", " + workspace
                                            + ", " + dateTime + ", justFirst="
                                            + justFirst + ", exactMatch="
                                            + exactMatch + "): returning "
                                            + set );
                        Debug.turnOn();
                    }
                }

                results.close();
            }
        }
//        // If we found a NodeRef but still have null (maybe because a version
//        // didn't exist at the time), try again for the latest.
//        if ( nodeRefs.isEmpty()//nodeRef == null 
//                && dateTime != null && gotResults) {
//            nodeRefs = findNodeRefsByType( specifier, prefix, null, justFirst,
//                                           exactMatch, services );
//            //nodeRef = findNodeRefByType( specifier, prefix, null, services );
//        }
        return nodeRefs;     
    }
    
    public static WorkspaceNode getWorkspace( NodeRef nodeRef ) {
        EmsScriptNode node = new EmsScriptNode( nodeRef, getServices() );
        return node.getWorkspace();
    }

    public static boolean isWorkspaceSource( EmsScriptNode source, EmsScriptNode changed ) {
        if (!exists(source) || !exists(changed)) return false;
        //if ( changed.equals( source ) ) return true;
        if ( !changed.hasAspect( "ems:HasWorkspace" ) ) return false;
        EmsScriptNode directSource = changed.getWorkspaceSource();
        if ( !exists(directSource) ) return false;
        if ( source.equals( directSource ) ) return true;
        return isWorkspaceSource( source, directSource );
    }
    
    public static boolean isWorkspaceSource( NodeRef source, NodeRef changed ) {
        if ( source == null || changed == null ) return false;
        EmsScriptNode sourceNode = new EmsScriptNode( source, getServices() );
        EmsScriptNode changedNode = new EmsScriptNode( changed, getServices() );
        return isWorkspaceSource( sourceNode, changedNode );
    }

    /**
     * Find a NodeReference by name (returns first match, assuming things are
     * unique)
     * 
     * @param id
     *            Node name to search for
     * @param workspace 
     * @param dateTime
     *            the time specifying which version of the NodeRef to find; null
     *            defaults to the latest version
     * @return NodeRef of first match, null otherwise
     */
    public static NodeRef findNodeRefById(String id, WorkspaceNode workspace,
                                          Date dateTime, ServiceRegistry services) {
        NodeRef r = findNodeRefByType(id, SearchType.ID, workspace, dateTime, true, services); // TODO: temporarily search by ID
        EmsScriptNode esn = null;
        if ( r != null ) {
            esn = new EmsScriptNode( r, getServices() );
        }
        if ( r == null || !esn.exists() ) {
            r = findNodeRefByType( id, "@cm\\:name:\"", workspace, dateTime,
                                   true, services );
        }
        return r;
    }
    
    
    /**
     * Perform Lucene search for the specified pattern and ACM type
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type      escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    public static Map< String, EmsScriptNode >
      searchForElements( String pattern, WorkspaceNode workspace, Date dateTime,
                         ServiceRegistry services, StringBuffer response,
                         Status status) {
        Map<String, EmsScriptNode> elementsFound = new HashMap<String, EmsScriptNode>();
        for (SearchType searchType: SearchType.values() ) {
            elementsFound.putAll( searchForElements( searchType.prefix,
                                                     pattern, workspace,
                                                     dateTime, services,
                                                     response, status ) );
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
                               WorkspaceNode workspace, Date dateTime,
                               ServiceRegistry services, StringBuffer response,
                               Status status ) {
        Map<String, EmsScriptNode> searchResults = new HashMap<String, EmsScriptNode>();

        
        //ResultSet resultSet = null;
        ArrayList<NodeRef> resultSet = null;
        //try {

        resultSet = findNodeRefsByType( pattern, type, workspace, dateTime,
                                        false, false, getServices() );
            //resultSet = findNodeRefsByType(pattern, type, getServices());

//            pattern = type + pattern + "\"";
//            resultSet =
//                    services.getSearchService().query( getStoreRef(),
//                                                       SearchService.LANGUAGE_LUCENE,
//                                                       pattern );
            for ( NodeRef nodeRef : resultSet ) {
            //for ( ResultSetRow row : resultSet ) {
                EmsScriptNode node =
                        new EmsScriptNode( nodeRef, services, response );
                if ( node.checkPermissions( PermissionService.READ, response, status ) ) {
                    String id = (String)node.getProperty( Acm.ACM_ID );
                    if ( id != null ) {
                        searchResults.put( id, node );
                    }
                }
            }
//        } catch (Exception e) {
//            if ( response != null || status != null ) {
//                String msg = "Error! Could not parse search: " + pattern + ".\n"
//                             + e.getLocalizedMessage();
//                if (Debug.isOn()) System.out.println(msg);
//                e.printStackTrace( if (Debug.isOn()) System.out );
//                if ( response != null ) response.append( msg );
//                if ( status != null ) status.setCode( HttpServletResponse.SC_BAD_REQUEST,
//                                                      msg );
//            } else {
//                e.printStackTrace();
//            }
//        } finally {
//            if (resultSet != null) {
//                resultSet.close();
//            }
//        }

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
            typeName = Acm.getJSON2ACM().get( typeName );
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
            // TODO: this prints out a warning, maybe better way to check?
            TypeDefinition t = dServ.getType( qName );
            if ( t != null ) {
//              if (Debug.isOn()) System.out.println("\n\n*** getType(" + typeName + ") worked!!!\n" );
              return true;
            }
        }
//        //        if (Debug.isOn()) System.out.println("all types: " + types);
////        TypeDefinition t = dServ.getType( QName.createQName( typeName ) );
////        if (Debug.isOn()) System.out.println("getType(" + typeName + ") = " + t );
//        for ( QName type : types ) {
////            if (Debug.isOn()) System.out.println( "getLocalName() = " + type.getLocalName() );
////            if (Debug.isOn()) System.out.println( "getPrefixString() = " + type.getPrefixString() );
////            if (Debug.isOn()) System.out.println( "toPrefixString() = " + type.toPrefixString() );
////            if (Debug.isOn()) System.out.println( "toString() = " + type.toString() );
//            if ( typeName.equals( type.getPrefixString() ) ) {
//                if (Debug.isOn()) System.out.println("isType(" + typeName + ") = true");
//                return true;
//            }
//        }
//        if (Debug.isOn()) System.out.println("isType(" + typeName + ") = false");
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
        AspectDefinition aspect = getAspect( aspectName, services );
        return aspect != null;
    }
    
    public static AspectDefinition getAspect( String aspectName, ServiceRegistry services ) {
        if ( Acm.getJSON2ACM().keySet().contains( aspectName ) ) {
            aspectName = Acm.getACM2JSON().get( aspectName );
        }
        if ( services == null ) services = getServices();
        DictionaryService dServ = services.getDictionaryService();

        QName qName = createQName( aspectName, services );
        if ( qName != null ) {
            AspectDefinition t = dServ.getAspect( qName );
            if ( t != null ) {
              if (Debug.isOn()) System.out.println("*** getAspect(" + aspectName + ") worked!!!" );
              return t;
            }
        }
        
        Collection< QName > aspects = dServ.getAllAspects();
        if (Debug.isOn()) System.out.println("all aspects: " + aspects);

        for ( QName aspect : aspects ) {
            if ( aspect.getPrefixString().equals( aspectName ) ) {
                AspectDefinition t = dServ.getAspect( aspect );
                if ( t != null ) {
                    if (Debug.isOn()) System.out.println("*** getAspect(" + aspectName + ") returning " + t + "" );
                    return t;
                }
            }
        }
        if (Debug.isOn()) System.out.println("*** getAspect(" + aspectName + ") returning null" );
        return null;

    }
    
    public static Collection< PropertyDefinition >
            getAspectProperties( String aspectName, ServiceRegistry services ) {
        AspectDefinition aspect = getAspect( aspectName, services );
        if ( aspect == null ) {
            return Utils.newList();
        }
        Map< QName, PropertyDefinition > props = aspect.getProperties();
        if ( props == null ) return Utils.getEmptyList();
        return props.values();
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
            typeOrAspectName = Acm.getJSON2ACM().get( typeOrAspectName );
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
                                             WorkspaceNode workspace,
                                             Date dateTime,
                                             ServiceRegistry services,
                                             StringBuffer response ) {
        if ( Utils.isNullOrEmpty( siteName ) ) return null;

        // Try to find the site in the workspace first.
        ArrayList< NodeRef > refs =
                findNodeRefsByType( siteName, SearchType.CM_NAME.prefix, workspace,
                                    dateTime, true, true, getServices() );
        for ( NodeRef ref : refs ) {
            EmsScriptNode siteNode = new EmsScriptNode(ref, services, response);
            if ( siteNode.isSite() ) {
                return siteNode;
            }
        }
        
        // Get the site from SiteService.
        SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        if (siteInfo != null) {
            NodeRef siteRef = siteInfo.getNodeRef();
            if ( dateTime != null ) {
                NodeRef vRef = NodeUtil.getNodeRefAtTime( siteRef, dateTime );
                if ( vRef != null ) siteRef = vRef;
            }

            EmsScriptNode siteNode = new EmsScriptNode(siteRef, services, response);
            if ( siteNode != null
                 && ( workspace == null || workspace.contains( siteNode ) ) ) {
                return siteNode;
            }
        }
        return null;
    }

    public static EmsScriptNode getCompanyHome( ServiceRegistry services ) {
        EmsScriptNode companyHome = null;
        if ( services == null ) services = getServiceRegistry();
        if ( services == null || services.getNodeLocatorService() == null ) {
            if (Debug.isOn()) System.out.println( "getCompanyHome() failed, no services or no nodeLocatorService: "
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
    
    public static NodeRef getNodeRefAtTime( NodeRef nodeRef, WorkspaceNode workspace,
                                            Date timestamp ) {
        EmsScriptNode node = new EmsScriptNode( nodeRef, getServices() );
        String id = node.getSysmlId();
        return getNodeRefAtTime( id, workspace, timestamp );
    }

    public static NodeRef getNodeRefAtTime( String id, WorkspaceNode workspace,
                                            Object timestamp ) {
        Date dateTime = null;
        if ( timestamp instanceof String ) {
            dateTime = TimeUtils.dateFromTimestamp( (String)timestamp );
        } else if ( timestamp instanceof Date ) {
            dateTime = (Date)timestamp;
        } else if ( timestamp != null ) {
            Debug.error( "getNodeRefAtTime() was not expecting a timestamp of type "
                         + timestamp.getClass().getSimpleName() );
        }
        NodeRef ref = findNodeRefById( id, workspace, dateTime, getServices() );
        //return getNodeRefAtTime( ref, timestamp );
        return ref;
    }

    public static NodeRef getNodeRefAtTime( NodeRef ref, String timestamp ) {
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        return getNodeRefAtTime( ref, dateTime );
    }
    
    /**
     * Get the version of the NodeRef that was the most current at the specified
     * date/time.
     * 
     * @param ref
     *            some version of the NodeRef
     * @param dateTime
     *            the date/time, specifying the version
     * @return the version of the NodeRef that was the latest version at the specified time, or if before any version
     */
    public static NodeRef getNodeRefAtTime( NodeRef ref,
                                            Date dateTime ) {
        if (Debug.isOn())  Debug.outln("getNodeRefAtTime( " + ref + ", " + dateTime + " )" );

        if ( dateTime == null ) {
            return ref;//getServices().getVersionService().getCurrentVersion( ref );
        }
        VersionHistory history = getServices().getVersionService().getVersionHistory( ref );
        if (history == null) {
        		// Versioning doesn't make versions until the first save...
        		EmsScriptNode node = new EmsScriptNode(ref, services);
        		Date createdTime = (Date)node.getProperty("cm:created");
                if ( dateTime != null && dateTime.compareTo( createdTime ) < 0 ) {
                    if (Debug.isOn())  Debug.outln( "no history! dateTime " + dateTime
                                        + " before created " + createdTime );
        			return null;
        		}
                if (Debug.isOn())  Debug.outln( "no history! created " + createdTime );
        		return ref;
        }
        
        Collection< Version > versions = history.getAllVersions();
        Vector<Version> vv = new Vector<Version>( versions );
        if (Debug.isOn())  Debug.outln("versions = " + vv );
        if ( Utils.isNullOrEmpty( vv ) ) {
            // TODO - throw error?!
            return null;
        }
        int index = Collections.binarySearch( vv, dateTime, versionLowerBoundComparator );
        if (Debug.isOn())  Debug.outln( "binary search returns index " + index );
        Version version = null;
        if ( index < 0 ) {
            // search returned index = -(lowerBound+1), so lowerbound = -index-1
            index = -index - 1;
            if (Debug.isOn())  Debug.outln( "index converted to lowerbound " + index );
//            // But, since the order is newest to oldest, we want the one after the lowerbound
//            if ( index >= 0 && index < vv.size()-1 ) {
//                index = index + 1;
//                if (Debug.isOn())  Debug.outln( "index converted to upperbound " + index );
//            }
        }
        if ( index < 0 ) {
            version = vv.get( 0 );
            if ( version != null ) {
                Date d = version.getFrozenModifiedDate();
                if (Debug.isOn())  Debug.outln( "first frozen modified date " + d );
                if ( d != null && d.after( dateTime ) ) {
                    NodeRef fnr = version.getFrozenStateNodeRef();
                    if (Debug.isOn())  Debug.outln( "returning first frozen node ref " + fnr );
                    return fnr;
                }
            }
            // TODO -- throw error?!
            if (Debug.isOn())  Debug.outln( "version is null; returning null!" );
            return null;
        } else if ( index >= vv.size() ) {
            if (Debug.isOn())  Debug.outln( "index is too large, outside bounds!" );
            // TODO -- throw error?!
            return null;
        } else {
            version = vv.get( index );
        }
        if ( Debug.isOn() ) {
            if (Debug.isOn())  Debug.outln( "picking version " + version );
            if (Debug.isOn())  Debug.outln( "version properties " + version.getVersionProperties() );
            String versionLabel = version.getVersionLabel();
            EmsScriptNode emsNode = new EmsScriptNode( ref, getServices() );
            ScriptVersion scriptVersion = emsNode.getVersion( versionLabel );
            if (Debug.isOn())  Debug.outln( "scriptVersion " + scriptVersion );
            ScriptNode node = scriptVersion.getNode();
            if (Debug.isOn())  Debug.outln( "script node " + node );
            //can't get script node properties--generates exception
            //if (Debug.isOn())  Debug.outln( "script node properties " + node.getProperties() );
            NodeRef scriptVersionNodeRef = scriptVersion.getNodeRef();
            if (Debug.isOn())  Debug.outln( "ScriptVersion node ref "
                                             + scriptVersionNodeRef );
            NodeRef vnr = version.getVersionedNodeRef();
            if (Debug.isOn())  Debug.outln( "versioned node ref " + vnr );
        }
        NodeRef fnr = version.getFrozenStateNodeRef();
        if (Debug.isOn())  Debug.outln( "frozen node ref " + fnr );
        if (Debug.isOn())  Debug.outln( "frozen node ref properties: "
                                         + getServices().getNodeService()
                                                        .getProperties( fnr ) );
        if (Debug.isOn())  Debug.outln( "frozen node ref "
                + getServices().getNodeService()
                               .getProperties( fnr ) );

        if (Debug.isOn())  Debug.outln( "returning frozen node ref " + fnr );

        return fnr;
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
                source = getSiteNode( siteName, null, null, services, response );
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

    public static NodeRef getNodeRefFromNodeId(String id) {
        List<NodeRef> nodeRefs = NodeRef.getNodeRefs("workspace://SpacesStore/" + id);
        if (nodeRefs.size() > 0) {
            return nodeRefs.get( 0 );
        }
        return null;
    }

    public static EmsScriptNode getVersionAtTime( EmsScriptNode element,
                                                  Date dateTime ) {
        EmsScriptNode versionedNode = null;
        NodeRef ref = getNodeRefAtTime( element.getNodeRef(), dateTime );
        if ( ref != null ) {
            versionedNode = new EmsScriptNode( ref, element.getServices() );
        }
        return versionedNode;
    }

    public static Collection< EmsScriptNode >
            getVersionAtTime( Collection< EmsScriptNode > moreElements, Date dateTime ) {
        ArrayList<EmsScriptNode> elements = new ArrayList<EmsScriptNode>();
        for ( EmsScriptNode n : moreElements ) {
            EmsScriptNode versionedNode = getVersionAtTime( n, dateTime );
            if ( versionedNode != null ) {
                elements.add( versionedNode );
            }
        }
        return elements;
    }

    public static boolean exists( EmsScriptNode node ) {
        if ( node == null ) return false;
        return node.exists();
    }
    
    public static boolean exists( NodeRef ref ) {
        if ( ref == null ) return false;
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        return node.exists();
    }
    
    public static EmsScriptNode getUserHomeFolder( String userName ) {
        NodeRef homeFolderNode = null;
        EmsScriptNode homeFolderScriptNode = null;
        if ( userName.equals( "admin" ) ) {
            homeFolderNode =
                    findNodeRefByType( userName, SearchType.CM_NAME, null,
                                       null, true, getServices() );
        } else {
            PersonService personService = getServices().getPersonService();
            NodeService nodeService = getServices().getNodeService();
            NodeRef personNode = personService.getPerson(userName);
            homeFolderNode =
                    (NodeRef)nodeService.getProperty( personNode,
                                                      ContentModel.PROP_HOMEFOLDER );
        }
        if ( homeFolderNode == null || !exists(homeFolderNode) ) {
            NodeRef ref = findNodeRefById( "User Homes", null, null, getServices() );
            EmsScriptNode homes = new EmsScriptNode( ref, getServices() );
            if ( homes != null && homes.exists() ) {
                homeFolderScriptNode = homes.createFolder( userName );
            } else {
                Debug.error("Error! No user homes folder!");
            }
        }
        if ( !exists( homeFolderScriptNode ) && exists( homeFolderNode ) ) {
            homeFolderScriptNode = new EmsScriptNode( homeFolderNode, getServices() );
        }
        return homeFolderScriptNode;
    }

    // REVIEW -- should this be in AbstractJavaWebScript?
    public static String createId( ServiceRegistry services ) {
        for ( int i=0; i<10; ++i ) {
            String id = "MMS_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
            // Make sure id is not already used (extremely unlikely)
            if ( findNodeRefById( id, null, null, services ) == null ) {
                return id;
            }
        }
        Debug.error( true, "Could not create a unique id!" );
        return null;
    }
    
}
