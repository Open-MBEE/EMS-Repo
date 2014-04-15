package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.digester.SetRootRule;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.Status;

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
    protected static final StoreRef SEARCH_STORE =
            new StoreRef( StoreRef.PROTOCOL_WORKSPACE, "SpacesStore" );
//    protected final static String[] searchTypes = {"@sysml\\:documentation:\"",
//                                                   "@sysml\\:name:\"",
//                                                   "@sysml\\:id:\"",
//                                                   "@sysml\\:string:\"",
//                                                   "@sysml\\:body:\""};

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

    protected static ResultSet findNodeRefsByType(String name, SearchType type, ServiceRegistry services) {
        return findNodeRefsByType( name, type.prefix, services );
    }
    protected static ResultSet findNodeRefsByType(String name, String prefix, ServiceRegistry services) {
        ResultSet results = null;
        results = services.getSearchService().query( SEARCH_STORE,
                                                     SearchService.LANGUAGE_LUCENE,
                                                     prefix + name + "\"" );
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
                for (ResultSetRow row: results) {
                    nodeRef = row.getNodeRef();
                    break; // Assumption is things are uniquely named - TODO:
                           // fix since snapshots have same name?...
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
//      return findNodeRefByType(name, "@cm\\:name:\"");
        return findNodeRefByType(id, SearchType.ID, services); // TODO: temporarily search by ID
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
            pattern = type + pattern + "\"";
            resultSet =
                    services.getSearchService().query( SEARCH_STORE,
                                                       SearchService.LANGUAGE_LUCENE,
                                                       pattern );
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
     * Get site of specified short name
     * @param siteName
     * @return  ScriptNode of site with name siteName
     */
    public static EmsScriptNode getSiteNode(String siteName, ServiceRegistry services, StringBuffer response) {
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
        return services.getNodeService().getAllRootNodes( SEARCH_STORE );
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
