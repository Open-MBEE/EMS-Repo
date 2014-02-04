package gov.nasa.jpl.view_repo.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
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

    // needed for Lucene search
    protected static final StoreRef SEARCH_STORE =
            new StoreRef( StoreRef.PROTOCOL_WORKSPACE, "SpacesStore" );
    protected final static String[] searchTypes = {"@sysml\\:documentation:\"",
                                                   "@sysml\\:name:\"",
                                                   "@sysml\\:id:\"",
                                                   "@sysml\\:string:\"",
                                                   "@sysml\\:body:\""};
    
    public static NodeRef findNodeRefByType(String name, SearchType type, ServiceRegistry services) {
        return findNodeRefByType( name, type.prefix, services );
    }
    
    public static NodeRef findNodeRefByType(String name, String prefix, ServiceRegistry services) {
        ResultSet results = null;
        NodeRef nodeRef = null;
        try {
            results = services.getSearchService().query( SEARCH_STORE,
                                                         SearchService.LANGUAGE_LUCENE,
                                                         prefix + name + "\"" );
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
        for (String searchType: searchTypes ) {
            elementsFound.putAll(searchForElements(searchType, pattern, services, response, status));
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

}
