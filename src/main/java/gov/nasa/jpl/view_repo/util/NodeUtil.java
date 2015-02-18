package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.CompareUtils.GenericComparator;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MethodCall;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode.EmsVersion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.repo.node.db.DbNodeServiceImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.logging.Log;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
        CHECKSUM( "@view\\:cs:\"" ),
        WORKSPACE("@ems\\:workspace:\"" ),
        WORKSPACE_NAME("@ems\\:workspace_name:\"" ),
        OWNER("@ems\\:owner:\"" ),
        ASPECT("ASPECT:\""),
        TYPE("TYPE:\"");

        public String prefix;

        SearchType( String prefix ) {
            this.prefix = prefix;
        }
    }

    /* static flags and constants */

    protected static String txMutex = "";
    protected static boolean beenInsideTransaction = false;
    protected static Map< String, Boolean > beenInsideTransactionMap =
            new LinkedHashMap< String, Boolean >();

    public static synchronized boolean hasBeenInsideTransaction() {
        Boolean b = beenInsideTransactionMap.get( "" + Thread.currentThread().getId());
        if ( b != null ) return b;
        return beenInsideTransaction;
    }
    public static synchronized void setBeenInsideTransaction( boolean b ) {
        beenInsideTransaction = b;
        beenInsideTransactionMap.put( "" + Thread.currentThread().getId(), b );
    }
    protected static boolean beenOutsideTransaction = false;
    protected static Map< String, Boolean > beenOutsideTransactionMap =
            new LinkedHashMap< String, Boolean >();
    public static synchronized boolean hasBeenOutsideTransaction() {
        Boolean b = beenOutsideTransactionMap.get( "" + Thread.currentThread().getId());
        if ( b != null ) return b;
        return beenOutsideTransaction;
    }
    public static synchronized void setBeenOutsideTransaction( boolean b ) {
        beenOutsideTransaction = b;
        beenOutsideTransactionMap.put( "" + Thread.currentThread().getId(), b );
    }
    protected static boolean insideTransactionNow = false;
    protected static Map< String, Boolean > insideTransactionNowMap =
            new LinkedHashMap< String, Boolean >();
    public static synchronized boolean isInsideTransactionNow() {
        Boolean b = insideTransactionNowMap.get( "" + Thread.currentThread().getId());
        if ( b != null ) return b;
        return insideTransactionNow;
    }
    public static synchronized void setInsideTransactionNow( boolean b ) {
        insideTransactionNow = b;
        insideTransactionNowMap.put( "" + Thread.currentThread().getId(), b );
    }
    protected static Map< String, StackTraceElement[] > insideTransactionStrackTrace =
            new LinkedHashMap< String, StackTraceElement[] >();
    protected static Map< String, StackTraceElement[] > outsideTransactionStrackTrace =
            new LinkedHashMap< String, StackTraceElement[] >();
    public static void setInsideTransactionStackTrace() {
        insideTransactionStrackTrace.put( "" + Thread.currentThread().getId(),
                                          Thread.currentThread().getStackTrace() );
    }
    public static void setOutsideTransactionStackTrace() {
        outsideTransactionStrackTrace.put( "" + Thread.currentThread().getId(),
                                           Thread.currentThread().getStackTrace() );
    }
    public static StackTraceElement[] getInsideTransactionStackTrace() {
        return insideTransactionStrackTrace.get( "" + Thread.currentThread().getId() );
    }
    public static StackTraceElement[] getOutsideTransactionStackTrace() {
        return outsideTransactionStrackTrace.get( "" + Thread.currentThread().getId() );
    }

    public static boolean doFullCaching = true;
    public static boolean doSimpleCaching = true;
    public static boolean doHeisenCheck = true;
    public static boolean doVersionCaching = true;
    public static boolean activeVersionCaching = true;
    public static boolean doJsonCaching = true;
    public static boolean doJsonDeepCaching = true;
    public static boolean doJsonStringCaching = true;
    
    // global flag that is enabled once heisenbug is seen, so it will email admins the first time heisenbug is seen
    public static boolean heisenbugSeen = false;

    public static String sitePkgPrefix = "site_";

    /**
     * A cache of alfresco nodes stored as a map from cm:name to node for the master branch only.
     */
    public static Map< String, NodeRef > simpleCache =
            Collections.synchronizedMap( new HashMap< String, NodeRef >() );

    /**
     * A cache of alfresco nodes stored as a map from sysml:id to a set of nodes
     */
    public static Map< String, Map< String, Map< String, Map< Boolean, Map< Long, Map< Boolean, Map< Boolean, Map< Boolean, Map<String, ArrayList< NodeRef > > > > > > > > > >
        elementCache = Collections.synchronizedMap( new HashMap< String, Map< String, Map< String, Map< Boolean, Map< Long, Map< Boolean, Map< Boolean, Map< Boolean, Map<String, ArrayList< NodeRef > > > > > > > > > >() );

    /**
     * A cache of the most nodeRefs, keyed by the nodes' alfresco ids. This is
     * to get around the "Heisenbug" where alfresco's current version is
     * sometimes tied to an old version.
     */
    protected static Map< String, NodeRef > heisenCache =
            Collections.synchronizedMap( new HashMap<String, NodeRef>() );

//  public static HashMap<String, String> versionLabelCache =
//            new HashMap<String, String>();
    public static Map< String, EmsVersion > versionCache =
            Collections.synchronizedMap( new HashMap<String, EmsVersion>() );
    public static Map< NodeRef, NodeRef > frozenNodeCache =
            Collections.synchronizedMap( new HashMap<NodeRef, NodeRef>() );

    // Set< String > filter, boolean isExprOrProp,Date dateTime, boolean isIncludeQualified
    public static Map< String, Map< Long, Map< Boolean, Map< Set<String>, JSONObject > > > > jsonDeepCache =
            Collections.synchronizedMap( new HashMap< String, Map< Long, Map< Boolean, Map< Set<String>, JSONObject > > > >() );
    public static Map< String, Map< Long, JSONObject > > jsonCache =
        Collections.synchronizedMap( new HashMap< String, Map< Long, JSONObject > >() );
    public static long jsonCacheHits = 0;
    public static long jsonCacheMisses = 0;

    // The json string cache maps JSONObjects to an integer (date in millis) to
    // a string rendering of itself paired with the date.
    public static Map<JSONObject, Map< Integer, Pair< Date, String > > > jsonStringCache =
            Collections.synchronizedMap( new HashMap< JSONObject, Map< Integer, Pair< Date, String > > >() );
    public static long jsonStringCacheHits = 0;
    public static long jsonStringCacheMisses = 0;

    
    // REVIEW -- TODO -- Should we try and cache the toString() output of the json, too?    
    // REVIEW -- TODO -- This would mean we'd have to concatenate the json
    // REVIEW -- TODO -- strings ourselves instead of just one big toString() 
    // REVIEW -- TODO -- on the collection as done currently.
    
    // Set the flag to time events that occur during a model post using the timers
    // below
    public static boolean timeEvents = false;
    private static Timer timer = null;
    private static Timer timerByType = null;
    private static Timer timerLucene = null;

    public static final Comparator< ? super NodeRef > nodeRefComparator =
            GenericComparator.instance();

    public static ServiceRegistry services = null;

    // needed for Lucene search
    public static StoreRef SEARCH_STORE = null;
            //new StoreRef( StoreRef.PROTOCOL_WORKSPACE, "SpacesStore" );

    /**
     * clear or create the cache for correcting bad node refs (that refer to
     * wrong versions)
     */
    public static void initHeisenCache() {
        // TODO -- the problem with this is that concurrent services could clear
        // this common cache. It would be better to pass around a context or
        // something with a service identifier as a key to the cache. So, each
        // web service invocation would have its own cache.
        heisenCache.clear();
    }
    public static NodeRef heisenCachePut( String id, NodeRef nodeRef ) {
        return heisenCache.put( id, nodeRef );
    }
    public static NodeRef heisenCacheGet( String id ) {
        return heisenCache.get( id );
    }

    public static NodeRef getCurrentNodeRefFromCache( NodeRef maybeFrozenNodeRef ) {
        NodeRef ref = frozenNodeCache.get( maybeFrozenNodeRef );
        if ( ref != null ) return ref;
        //EmsScriptNode node = new EmsScriptNode( maybeFrozenNodeRef, getServices() );
        return null;//node.getLiveNodeRefFromVersion();
    }
//    public static void cacheNodeVersion( EmsScriptNode node ) {
//        if ( activeVersionCaching && NodeUtil.exists( node ) ) {
//            Version v = node.getCurrentVersion();
//            if ( doVersionCaching ) {
//                EmsVersion cachedVersion = versionCache.get( node.getId() );
//            }
//            NodeUtil.heisenCachePut( node.getName(),
//                                     ( v != null && v.getFrozenStateNodeRef() != null )
//                                     ? v.getFrozenStateNodeRef()
//                                     : node.getNodeRef() );
//        }
//    }

    public static JSONObject jsonCacheGet( String id, long millis,
                                           boolean noMetadata ) {
        JSONObject json = Utils.get( jsonCache, id, millis );
        System.out.println( "jsonCacheGet(" + id + ", " + millis + ", "
                            + noMetadata + ") = " + json );
        return json;
    }

    public static JSONObject jsonCachePut( JSONObject json, String id,
                                           long millis ) {
        json = clone( json );
        if ( doJsonStringCaching ) {
            json = addJsonMetadata( json, id, millis, true, null );
        }
        System.out.println( "jsonCachePut(" + id + ", " + millis + ", " + json
                            + ")" );
        Utils.put( jsonCache, id, millis, json );
        return json;
    }

    public static JSONObject jsonDeepCacheGet( String id, long millis,
                                               boolean isIncludeQualified,
                                               Set< String > jsonFilter,
                                               boolean noMetadata ) {
        jsonFilter = jsonFilter == null ? new TreeSet< String >() : jsonFilter;
        JSONObject json = Utils.get( jsonDeepCache, id, millis,
                                     isIncludeQualified, jsonFilter );
        if ( doJsonStringCaching && noMetadata ) {
            json = clone( json );
            stripJsonMetadata( json );
        }
        System.out.println( "jsonDeepCacheGet(" + id + ", " + millis + ", "
                            + isIncludeQualified + ", " + jsonFilter + ", "
                            + noMetadata + ") = " + json );
        return json;
    }

    public static JSONObject jsonDeepCachePut( JSONObject json, String id,
                                               long millis,
                                               boolean isIncludeQualified,
                                               Set< String > jsonFilter ) {
        json = clone( json );
        jsonFilter = jsonFilter == null ? new TreeSet< String >() : jsonFilter;
        json = addJsonMetadata( json, id, millis, isIncludeQualified, jsonFilter );
        System.out.println( "jsonDeepCachePut(" + id + ", " + millis + ", "
                            + isIncludeQualified + ", " + jsonFilter + ", "
                            + json + ")" );
        Utils.put( jsonDeepCache, id, millis, isIncludeQualified, jsonFilter,
                   json );
        return json;
    }

    public static JSONObject stripJsonMetadata( JSONObject json ) {
        System.out.println("stripJsonMetadata -> " + json );
        json.remove( "cacheKey" );
        json.remove( "jsonString" );
        json.remove( "jsonString4" );
        System.out.println("stripJsonMetadata -> " + json );
        return json;
    }
    
    /**
     * Put json.toString() in the json.
     * @param json
     * @param id
     * @param millis
     * @param isIncludeQualified
     * @param jsonFilter
     * @return
     */
    public static JSONObject addJsonMetadata( JSONObject json, String id,
                                              long millis,
                                              boolean isIncludeQualified,
                                              Set< String > jsonFilter ) {
        if ( json == null ) return null;
        if ( !doJsonStringCaching ) return json;
        
        if ( json.has( "jsonString4" ) ) return json;
        
        jsonFilter = jsonFilter == null ? new TreeSet< String >() : jsonFilter;
        String jsonString4 = null;
    
        try {
            jsonString4 = json.toString(4);
        } catch ( JSONException e1 ) {
            e1.printStackTrace();
            return null;
        }
        String jsonString = jsonString4.replaceAll( "(    |\n)", " " ); 
        JSONArray keyJson = new JSONArray();
        keyJson.put( id );
        keyJson.put( millis );
        keyJson.put( isIncludeQualified );
        keyJson.put( jsonFilter );
        try {
            //json.put( "cacheKey", keyJson );
            json.put( "jsonString", jsonString );
            json.put( "jsonString4", jsonString4 );
        } catch ( JSONException e ) {
            e.printStackTrace();
            return null;
        }
        System.out.println( "addJsonMetadata(" + id + ", " + millis + ", "
                            + isIncludeQualified + ", " + jsonFilter + ") -> "
                            + json );
        return json;
    }

    protected static JSONObject filterJson( JSONObject json, 
                                            Set< String > jsonFilter,
                                            boolean isIncludeQualified ) throws JSONException {
        JSONObject newJson = new JSONObject();
        Iterator keys = json.keys();
        while ( keys.hasNext() ) {
            String key = (String)keys.next();
            if ( jsonFilter.contains( key )
                 || EmsScriptNode.dontFilterOut.contains( key ) ) {
                Object value = json.get( key );
                if ( key.equals( Acm.JSON_SPECIALIZATION )
                     && value instanceof JSONObject ) {
                    // if ( !( value instanceof JSONObject ) ) {
                    // value = JSONObject.make( (JSONObject)value );
                    // }
                    JSONObject newSpec = filterJson( (JSONObject)value, 
                                                     jsonFilter, isIncludeQualified );
                    if ( newSpec != null && newSpec.length() > 0 ) {
                        newJson.put( key, newSpec );
                    }
                } else if ( isIncludeQualified ||
                            ( !key.equals( "qualifiedId" ) && !key.equals( "qualifiedName" ) ) ) {
                    if ( Debug.isOn() ) Debug.outln( "add to newJson = " + key
                                                     + ":" + value );
                    newJson.put( key, value );
                }
            }
        }
        return newJson;
    }

    public static String jsonToString( JSONObject json ) {
        if ( !doJsonStringCaching ) return json.toString();
        try {
            String s = jsonToString( json, -1 );
            
            return s;
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static String jsonToString( JSONObject json, int numSpacesToIndent ) throws JSONException {
        if ( json == null ) return null;
        String s = null;
        if ( !doJsonStringCaching || !json.has( "jsonString4" ) ) {
            stripJsonMetadata( json );
            if ( numSpacesToIndent < 0 ) {
                s = json.toString();
                System.out.println( "jsonToString( json, " + numSpacesToIndent + " ) = json.toString() = " + s );
                return s;
            }
            s = json.toString( numSpacesToIndent );
            System.out.println( "jsonToString( json, " + numSpacesToIndent + " ) = json.toString( " + numSpacesToIndent + " ) = " + s );
            return s;
        }
        if ( numSpacesToIndent < 0 ) {
            if ( json.has( "jsonString" ) ) {
                s = json.getString( "jsonString" );
                System.out.println( "jsonToString( json, " + numSpacesToIndent + " ) = json.getSString( \"jsonString\" ) = " + s );
                return s;
            }
            // TODO -- Warning! shouldn't get here!
            json = stripJsonMetadata( clone( json ) );
            s = json.toString();
            System.out.println( "BAD! jsonToString( json, " + numSpacesToIndent + " ) = json.toString() = " + s );
            return s;
        }
        if ( json.has( "jsonString4" ) ) {
            String jsonString4 = json.getString( "jsonString4" );
            if ( numSpacesToIndent == 4 ) {
                System.out.println( "jsonToString( json, " + numSpacesToIndent + " ) = jsonString4 = " + jsonString4 );
                return jsonString4;
            }
            s = jsonString4.replaceAll("    ", Utils.repeat( " ", numSpacesToIndent ) );
            System.out.println( "jsonToString( json, " + numSpacesToIndent + " ) = jsonString4.replaceAll(\"    \", Utils.repeat( \" \", " + numSpacesToIndent + " ) ) = " + s );
            return s;
        }
        // TODO -- Warning! shouldn't get here!
        json = stripJsonMetadata( clone( json ) );
        s = json.toString( numSpacesToIndent );
        System.out.println( "BAD! jsonToString( json, " + numSpacesToIndent + " ) = json.toString( " + numSpacesToIndent + " ) = " + s );
        return s;
    }

    public static String oldJsonToString( JSONObject json, int numSpacesToIndent ) throws JSONException {
        if ( !doJsonStringCaching ) return json.toString( numSpacesToIndent );
        String result = null;
        String modString = json.optString("modified");
        Date mod = null;
        // Only cache json with a modified date so that we know when to update
        // it.
        if ( modString != null ) {
            mod = TimeUtils.dateFromTimestamp( modString );
            if ( mod != null && jsonStringCache.containsKey( json ) ) {
                Pair< Date, String > p = Utils.get( jsonStringCache, json, numSpacesToIndent );//stringCache.get( this );
                if ( p != null ) {
                    if ( p.first != null && !mod.after( p.first ) ) {
                        result = p.second;
                        // cache hit
                        ++jsonStringCacheHits;
                        //System.out.println("string cache hit : " + result );
                        return result;
                    }
                }
            }
        }
        if ( numSpacesToIndent < 0 ) {
            result = json.toString();
        } else {
            result = json.toString(numSpacesToIndent);
        }
        if ( mod == null ) {
            // cache not applicable
        } else {
            // cache miss; add to cache
            ++jsonStringCacheMisses;
            Utils.put(jsonStringCache, json, numSpacesToIndent,
                      new Pair< Date, String >( mod, result ) );
        }
        return result;
    }
    
    public static JSONObject newJsonObject(String s) throws JSONException {
        JSONObject newJson = new JSONObject(s) {
            @Override
            public String toString() {
              try {
                if ( has( "jsonString" ) ) {
                    System.out.println("has jsonString: " + getString( "jsonString" ));
                    return getString( "jsonString" );
                }
                System.out.println("no jsonString: " + super.toString());
                return super.toString();
              } catch ( JSONException e ) {
                e.printStackTrace();
              }
              return null;
            }
            @Override
            public String toString( int n ) {
              try {
                if ( has( "jsonString4" ) ) {
                    System.out.println("has jsonString4: " + jsonToString( this, n ));
                    return jsonToString( this, n );
                }
                System.out.println("no jsonString4: " + super.toString(n));
                return super.toString( n );
              } catch ( JSONException e ) {
                e.printStackTrace();
              }
              return null;
            }
        };
        return newJson;
    }
    
    public static JSONObject newJsonObject() {
        JSONObject newJson = new JSONObject() {
            @Override
            public String toString() {
              try {
                if ( has( "jsonString" ) ) {
                    System.out.println("has jsonString: " + getString( "jsonString" ));
                    return getString( "jsonString" );
                }
                System.out.println("no jsonString: " + super.toString());
                return super.toString();
              } catch ( JSONException e ) {
                e.printStackTrace();
              }
              return null;
            }
            @Override
            public String toString( int n ) {
              try {
                if ( has( "jsonString4" ) ) {
                    System.out.println("has jsonString4: " + jsonToString( this, n ));
                    return jsonToString( this, n );
                }
                System.out.println("no jsonString4: " + super.toString(n));
                return super.toString( n );
              } catch ( JSONException e ) {
                e.printStackTrace();
              }
              return null;
            }
        };
        return newJson;
    }
    
    public static JSONObject clone( JSONObject json ) {
        if ( json == null ) return null;
        JSONObject newJson = newJsonObject();
        
        Iterator keys = json.keys();
        while ( keys.hasNext() ) {
            String key = (String)keys.next();
            Object value;
            try {
                value = json.get( key );
                if ( key.equals( Acm.JSON_SPECIALIZATION ) && value instanceof JSONObject ) {
                    value = clone( (JSONObject)value );
                }
                newJson.put( key, value );
            } catch ( JSONException e ) {
                e.printStackTrace();
            }
        }
        return newJson;
    }

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

        timerLucene = Timer.startTimer(timerLucene, timeEvents);

        if ( searchService == null ) {
            if ( getServiceRegistry() != null ) {
                searchService = getServiceRegistry().getSearchService();
            }
        }
        ResultSet results = null;
        if ( searchService != null ) {
//            results = searchService.query( getStoreRef(),
//                                           SearchService.LANGUAGE_LUCENE,
//                                           queryPattern );
            results = searchService.query( getSearchParameters(queryPattern) );
        }
        if ( Debug.isOn() ) {
            Debug.outln( "luceneSearch(" + queryPattern + "): returned "
                         + results.length() + " nodes." );//resultSetToList( results ) );
        }

     	Timer.stopTimer(timerLucene, "***** luceneSearch(): time", timeEvents);

        return results;
    }
    
    public static SearchParameters getSearchParameters(String queryPattern) {
        final SearchParameters params = new SearchParameters();
        params.addStore(getStoreRef());
        params.setLanguage(SearchService.LANGUAGE_LUCENE);
        params.setQuery(queryPattern);
        params.setLimitBy(LimitBy.UNLIMITED);
        params.setLimit(0);
        params.setMaxPermissionChecks(100000);
        params.setMaxPermissionCheckTimeMillis(100000);
        params.setMaxItems(-1);
        return params;
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
    public static ArrayList<NodeRef> resultSetToNodeRefList( ResultSet results ) {
        ArrayList<NodeRef> nodes = new ArrayList<NodeRef>();
       for ( ResultSetRow row : results ) {
            if ( row.getNodeRef() != null ) {
                nodes.add( row.getNodeRef() );
            }
        }
        return nodes;
    }
    public static String resultSetToString( ResultSet results ) {
        return "" + resultSetToList( results );
    }

    public static List< NodeRef > findNodeRefsByType(String name,
                                                           SearchType type,
                                                           ServiceRegistry services) {
        return findNodeRefsByType( name, type.prefix, services );
    }

    public static ArrayList< NodeRef > findNodeRefsByType(String name,
                                                           String prefix,
                                                           ServiceRegistry services) {
        ResultSet results = null;
        String queryPattern = prefix + name + "\"";
        results = luceneSearch( queryPattern, services );
        //if ( (results == null || results.length() == 0 ) && pref)
        ArrayList< NodeRef > resultList = resultSetToNodeRefList( results );
        results.close();
        return resultList;
    }

    public static NodeRef findNodeRefByType( String name, SearchType type,
                                             boolean ignoreWorkspace,
                                             WorkspaceNode workspace,
                                             Date dateTime, boolean exactMatch,
                                             ServiceRegistry services, boolean findDeleted ) {
        return findNodeRefByType( name, type.prefix, ignoreWorkspace, workspace, dateTime,
                                  exactMatch, services, findDeleted, null );
    }

    public static NodeRef findNodeRefByType( String name, SearchType type,
                                             boolean ignoreWorkspace,
                                             WorkspaceNode workspace,
                                             Date dateTime, boolean exactMatch,
                                             ServiceRegistry services, boolean findDeleted,
                                             String siteName) {
        return findNodeRefByType( name, type.prefix, ignoreWorkspace, workspace, dateTime,
                                  exactMatch, services, findDeleted, siteName );
    }

    public static NodeRef findNodeRefByType( String specifier, String prefix,
                                             //String parentScopeName,
                                             boolean ignoreWorkspace,
                                             WorkspaceNode workspace,
                                             Date dateTime, boolean exactMatch,
                                             ServiceRegistry services, boolean findDeleted,
                                             String siteName) {
        ArrayList< NodeRef > refs =
                findNodeRefsByType( specifier, prefix,
                                    //parentScopeName,
                                    ignoreWorkspace,
                                    workspace, dateTime, true,
                                    exactMatch, services, findDeleted, siteName );
        if ( Utils.isNullOrEmpty( refs ) ) return null;
        NodeRef ref = refs.get( 0 );
        return ref;
    }

//    public Set<NodeRef> findNodeRefsInDateRange( Date fromDate, Date toDate,
////                      String parentScopeName,
//                      boolean ignoreWorkspace,
//                      WorkspaceNode workspace, Date dateTime,
//                      boolean justFirst, boolean exactMatch,
//                      ServiceRegistry services, boolean includeDeleted ) {
//    }

    public static ArrayList< NodeRef >
    findNodeRefsByType( String specifier, String prefix,
                        boolean ignoreWorkspace,
                        WorkspaceNode workspace, Date dateTime,
                        boolean justFirst, boolean exactMatch,
                        ServiceRegistry services, boolean includeDeleted ) {

        return findNodeRefsByType(specifier, prefix, ignoreWorkspace,
                                  workspace, dateTime, justFirst, exactMatch,
                                  services, includeDeleted, null);
    }

    public static ArrayList< NodeRef >
    findNodeRefsByType( String specifier, String prefix,
                        boolean ignoreWorkspace,
                        WorkspaceNode workspace,
                        Date dateTime,
                        boolean justFirst, boolean exactMatch,
                        ServiceRegistry services, boolean includeDeleted,
                        String siteName) {
        return findNodeRefsByType( specifier, prefix,
                                   ignoreWorkspace, workspace,
                                   false, // onlyThisWorkspace
                                   dateTime, justFirst, exactMatch, services,
                                   includeDeleted, siteName );
    }
    protected static boolean isIdSearch(String prefix, boolean currentVal) {
        return ( currentVal ||
                 SearchType.ID.prefix.equals( prefix )||
                 SearchType.CM_NAME.prefix.equals( prefix ) );
    }
    public static ArrayList< NodeRef >
            findNodeRefsByType( String specifier, String prefix,
                                boolean ignoreWorkspace,
                                WorkspaceNode workspace,
                                boolean onlyThisWorkspace,
                                Date dateTime,
                                boolean justFirst, boolean exactMatch,
                                ServiceRegistry services, boolean includeDeleted,
                                String siteName) {

        ArrayList<NodeRef> results = null;

        timerByType = Timer.startTimer(timerByType, timeEvents);

        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        if ( services == null ) services = getServices();

        // look in cache first
        boolean useSimpleCache = false;
        boolean useFullCache = false;
        if ( doSimpleCaching || doFullCaching ) {

            boolean idSearch = false;
            // Only use the simple cache if in the master workspace, just getting a single node, not
            // looking for deleted nodes, and searching by cm:name or sysml:id.  Otherwise, we
            // may want multiple nodes in our results, or they could have changed since we added
            // them to the cache:
            useSimpleCache = doSimpleCaching && !ignoreWorkspace && !includeDeleted
                             && workspace == null && dateTime == null
                             && justFirst && siteName == null
                             && ( idSearch = isIdSearch( prefix, idSearch ) );

            // Conditions under which the full cache can be used:
            // 1. If dateTime != null and dateTime < now then the cache may be
            //    used for all other argument combinations.
            // 2. Otherwise, the cache may be used for finding nodes by cm:name
            //    and sysml:id if ignoreWorkspace == false and either workspace
            //    == null or onlyThisWorkspace == true.
            //
            // There are other things we can do to expand the applicability of
            // the cache, but they require more code changes.
            //
            // One idea is to track a lastModified time for the database. In
            // this case, we could store results for dateTime == null by
            // inserting dateTime == now in the cache and if there are results
            // for a dateTime after the lastModified time, then they are valid
            // (I think). We need to be careful not to pollute the cache with
            // entries for dateTime == null; a purge of these entries may be
            // necessary.
            useFullCache = doFullCaching && !useSimpleCache &&
                           ( ( dateTime != null && dateTime.before( new Date() ) ) ||
                             ( !ignoreWorkspace &&
                               ( workspace == null || onlyThisWorkspace ) &&
                               ( idSearch = isIdSearch(prefix, idSearch) ) ) );

            if ( useSimpleCache && doSimpleCaching ) {
                NodeRef ref = simpleCache.get( specifier );
                if (services.getPermissionService().hasPermission( ref, PermissionService.READ ) == AccessStatus.ALLOWED) {
                    if ( exists(ref ) ) {
                        results = Utils.newList( ref );
                    }
                }
            } else if ( doFullCaching && useFullCache ) {
                results = getCachedElements( specifier, prefix, ignoreWorkspace,
                                             workspace, onlyThisWorkspace,
                                             dateTime, justFirst, exactMatch,
                                             includeDeleted, siteName );
            }
        }

        boolean wasCached = false;
        boolean caching = false;
        try {
            if ( !Utils.isNullOrEmpty( results ) ) {
                wasCached = true; // doCaching must be true here
            } else {
                results = findNodeRefsByType( specifier, prefix, services );
                if ( (doSimpleCaching || doFullCaching) && !Utils.isNullOrEmpty( results ) ) {
                    caching = true;
                }
            }
            if ( results != null ) {
                if ( doHeisenCheck ) {
                    results = fixVersions( results );
                }
                if ( wasCached && dateTime == null ) {
                    nodeRefs = results;
                }
                else {
                    nodeRefs = filterResults( results, specifier, prefix,
                                              useSimpleCache, ignoreWorkspace,
                                              workspace, onlyThisWorkspace,
                                              dateTime, justFirst, exactMatch,
                                              services, includeDeleted, siteName );
                }

                // Always want to check for deleted nodes, even if using the cache:
                nodeRefs = correctForDeleted( nodeRefs, specifier, prefix,
                                              useSimpleCache, ignoreWorkspace,
                                              workspace, onlyThisWorkspace,
                                              dateTime, justFirst, exactMatch,
                                              services, includeDeleted, siteName );

            } // ends if (results != null)


            // Update cache with results
            if ( ( doSimpleCaching || doFullCaching ) && caching
                 && !Utils.isNullOrEmpty( nodeRefs ) ) {
                if ( useSimpleCache && doSimpleCaching ) {
                    NodeRef r = nodeRefs.get( 0 );
                    simpleCache.put( specifier, r );
                } else if ( useFullCache && doFullCaching  ){
                    putInCache( specifier, prefix, ignoreWorkspace, workspace,
                                onlyThisWorkspace, dateTime, justFirst,
                                exactMatch, includeDeleted, siteName, nodeRefs );
                }
            }
        } finally {
            if ( Debug.isOn() && !Debug.isOn() ) {
                if (results != null) {
                    List< EmsScriptNode > set =
                            EmsScriptNode.toEmsScriptNodeList( nodeRefs,
                                                               services, null,
                                                               null );
                    Debug.outln( "findNodeRefsByType(" + specifier + ", " + prefix + ", " + workspace + ", " + dateTime + ", justFirst=" + justFirst + ", exactMatch=" + exactMatch + "): returning " + set );
                }
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

        Timer.stopTimer(timerByType, "***** findNodeRefsByType(): time ", timeEvents);

        return nodeRefs;
    }

    public static ArrayList<NodeRef> fixVersions( ArrayList<NodeRef> nodeRefs ) {
        // check for alfresco bug where SpacesStore ref is the wrong version
        //if ( !doVersionCaching ) return nodeRefs;
        if ( !doHeisenCheck ) return nodeRefs;
        ArrayList<NodeRef> newNodeRefs = null;
        for ( NodeRef nr : nodeRefs ) {
            EmsScriptNode esn = new EmsScriptNode( nr, getServices() );
            if ( !esn.scriptNodeExists() ) continue;
            if ( esn.getOrSetCachedVersion() ) {
//            if ( !esn.checkNodeRefVersion2( null ) ) {
                if ( newNodeRefs == null ) {
                    newNodeRefs = new ArrayList<NodeRef>();
                    @SuppressWarnings( "unchecked" )

                    // Back up and copy nodes we already skipped
                    ArrayList<NodeRef> copy = (ArrayList<NodeRef>)nodeRefs.clone();
                    for ( NodeRef earlier : copy ) {
                        if ( !scriptNodeExists( earlier ) ) continue;
                        if ( earlier.equals( nr ) ) {
                            break;
                        }
                        newNodeRefs.add( earlier );
                    }
                }
                newNodeRefs.add( esn.getNodeRef() );
            } else if ( newNodeRefs != null ) {
                newNodeRefs.add( nr );
            }
        }
        if ( newNodeRefs == null ) {
            newNodeRefs = nodeRefs;
        }
        return newNodeRefs;
    }

    public static int compare( Version v1, Version v2 ) {
        if ( v1 == v2 ) return 0;
        if ( v1 == null ) return -1;
        if ( v2 == null ) return 1;
        Date d1 = v1.getFrozenModifiedDate();
        Date d2 = v2.getFrozenModifiedDate();
        return d1.compareTo( d2 );
    }

    protected static ArrayList<NodeRef> filterResults(ArrayList<NodeRef> results,
                                                      String specifier, String prefix,
                                                      boolean useSimpleCache,
                                                      boolean ignoreWorkspace,
                                                      WorkspaceNode workspace,
                                                      boolean onlyThisWorkspace,
                                                      Date dateTime,
                                                      boolean justFirst, boolean exactMatch,
                                                      ServiceRegistry services, boolean includeDeleted,
                                                      String siteName) {
        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        NodeRef lowest = null;
        NodeRef nodeRef = null;

        for ( NodeRef nr : results ) {
            //int minParentDistance = Integer.MAX_VALUE;
            if ( nr == null ) continue;
            EmsScriptNode esn = new EmsScriptNode( nr, getServices() );

            if ( Debug.isOn() && !Debug.isOn() ) {
                Debug.outln( "findNodeRefsByType(" + specifier + ", " + prefix + ", " + workspace + ", " + dateTime + ", justFirst=" + justFirst + ", exactMatch=" + exactMatch + "): candidate " + esn.getWorkspaceName() + "::" + esn.getName() );
            }

            // Get the version for the date/time if specified.
            if ( dateTime != null ) {
                nr = getNodeRefAtTime( nr, dateTime );

                // null check
                if ( nr == null ) {
                    if ( Debug.isOn() ) {
                        Debug.outln( "findNodeRefsByType(): no nodeRef at time " + dateTime );
                    }
                    continue;
                }

                // get EmsScriptNode for versioned node ref
                esn = new EmsScriptNode( nr, getServices() );
            }

            // make sure the node still exists
            if ( esn != null && !esn.scriptNodeExists() ) {
                continue;
            }
//            if ( !esn.exists() ) {
//                if ( !(includeDeleted && esn.isDeleted()) ) {
//                    if ( Debug.isOn() ) {
//                        System.out.println( "findNodeRefsByType(): element does not exist "
//                                     + esn );
//                    }
//                    continue;
//                }
//            }
            try {
                // Make sure it's in the right workspace.
                if ( !ignoreWorkspace && esn != null ) {
                    WorkspaceNode esnWs = esn.getWorkspace();
                    if ( ( onlyThisWorkspace && !workspacesEqual( workspace, esnWs ) ) ||
                         ( workspace == null && esnWs != null ) ||
                         ( workspace != null && !workspace.contains( esn ) ) ) {
                        if ( Debug.isOn() && !Debug.isOn()) {
                            System.out.println( "findNodeRefsByType(): wrong workspace "
                                    + workspace );
                        }
                        continue;
                    }
                }
            } catch( InvalidNodeRefException e ) {
                if ( Debug.isOn() ) e.printStackTrace();
                continue;
            } catch( Throwable e ) {
                e.printStackTrace();
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
                // Check that it from the desired site if desired:
                if (siteName != null && !siteName.equals( esn.getSiteName() )) {
                    match = false;
                }
                if ( !match ) {
                    if ( Debug.isOn() ) Debug.outln( "findNodeRefsByType(): not an exact match or incorrect site" );
                } else {
                    // Make sure the lowest/deepest element in the workspace
                    // chain is first in the list. We do this by tracking the
                    // lowest so far, and if it is upstream from current, then
                    // the current becomes the lowest and put in front of the
                    // list. This assumes that the elements have the same
                    // sysmlId, but it is not checked. We fix for isDeleted()
                    // later.
                    nodeRef = nr;
                    if ( exists( workspace )
                         && ( lowest == null ||
                              isWorkspaceAncestor( lowest, nodeRef, true ) ) ) {
                        lowest = nodeRef;
                        nodeRefs.add( 0, nodeRef );
                    } else {
                        nodeRefs.add( nodeRef );
                    }
                    if ( Debug.isOn() ) Debug.outln( "findNodeRefsByType(): matched!" );
                    // If only wanting the first matching element, we try to
                    // break out of the loop when we find it. There are many
                    // conditions under which we may not be able to do this.
                    if ( justFirst &&
                         // This isn't necessary since we check earlier for
                         // this. Just being robust by re-checking.
                         scriptNodeExists( lowest ) &&
                         // If we care about the workspace and it is a branch in
                         // time, then it's possible that this (and other)
                         // noderefs will change due to post processing, so we
                         // don't break in fear of the unknown. We assume the
                         // workspace will not change, so the lowest should
                         // still be valid during post-processing.
                         (ignoreWorkspace || !exists( workspace ) || workspace.getCopyTime() == null ) &&
                         // Since we clean up for deleted nodes later, we can't
                         // break unless we don't care whether it's deleted, or
                         // it's not deleted.
                         ( includeDeleted || !isDeleted( lowest ) ) &&
                         // We cannot break early if looking in a specific
                         // workspace unless we found one that is only in the
                         // target workspace.
                         ( !exists( workspace ) ||
                                    workspace.equals( getWorkspace( nodeRef ) ) ) ) {
                        break;
                    }
                }

            } catch ( Throwable e ) {
                e.printStackTrace();
            }
        } // ends else for

        nodeRefs = correctForWorkspaceCopyTime( nodeRefs, specifier, prefix,
                                                ignoreWorkspace,
                                                workspace, onlyThisWorkspace,
                                                dateTime, justFirst, exactMatch,
                                                services, includeDeleted, siteName );

        return nodeRefs;
    }

    protected static ArrayList<NodeRef> correctForWorkspaceCopyTime(ArrayList<NodeRef> nodeRefs,
                                                                    String specifier, String prefix,
                                                                    boolean ignoreWorkspace,
                                                                    WorkspaceNode workspace,
                                                                    boolean onlyThisWorkspace,
                                                                    Date dateTime,
                                                                    boolean justFirst, boolean exactMatch,
                                                                    ServiceRegistry services, boolean includeDeleted,
                                                                    String siteName) {
        // If the workspace is copied at a time point (as opposed to
        // floating with the parent), then we need to check each element
        // found to see if it is in some parent workspace and last modified
        // after the copy time. If so, then we need to get the element in
        // the parent workspace at the time of the copy.
        if ( nodeRefs != null && workspace != null && !ignoreWorkspace ) {
            Date copyTime = workspace.getCopyTime();
            if ( copyTime != null ) {
                // loop through each result
                ArrayList<NodeRef> correctedRefs = new ArrayList<NodeRef>();
                for ( NodeRef r : nodeRefs) {
                    if ( r == null ) continue;
                    WorkspaceNode resultWs = getWorkspace( r );
                    EmsScriptNode esn;
                    // If a native member of the workspace, no need to correct.
                    if ( workspace.equals( resultWs ) ) {
                        correctedRefs.add( r );
                    } else {
                        esn = new EmsScriptNode( r, getServices() );
                        Date lastModified = esn.getLastModified( dateTime );
                        // Check if modified after the copyTime.
                        if ( lastModified != null &&
                                lastModified.after( copyTime ) ) {
                            // Replace with the versioned ref at the copy time
                            ArrayList< NodeRef > refs =
                                    findNodeRefsByType( esn.getSysmlId(),
                                                        SearchType.ID.prefix,
                                                        ignoreWorkspace,
                                                        resultWs, copyTime,
                                                        true, // justOne
                                                        exactMatch, services,
                                                        includeDeleted,
                                                        siteName );
                            if ( !Utils.isNullOrEmpty( refs ) ) {
                                // only asked for one
                                NodeRef newRef = refs.get( 0 );
                                correctedRefs.add( newRef );
                            }
//                            r = getNodeRefAtTime( r, resultWs, copyTime );
//                            if ( r != null ) {
//                                esn = new EmsScriptNode( r, getServices() );
//                            } else {
//                                esn = null;
//                            }
                        } else {
                            if ( lastModified == null ) {
                                Debug.error( "ERROR!  Should never have null modified date!" );
                            }
                            correctedRefs.add( r );
                        }
//                        if ( exists( esn ) || ( includeDeleted && esn.isDeleted() &&
//                                ()!exactMatch ) ) {
//                            correctedRefs.add( r );
//                        }
                    }
                }
                nodeRefs = correctedRefs;
            }
        }

        return nodeRefs;
    }

    protected static ArrayList< NodeRef >
            correctForDeleted( ArrayList< NodeRef > nodeRefs, String specifier,
                               String prefix, boolean useSimpleCache,
                               boolean ignoreWorkspace,
                               WorkspaceNode workspace,
                               boolean onlyThisWorkspace, Date dateTime,
                               boolean justFirst, boolean exactMatch,
                               ServiceRegistry services,
                               boolean includeDeleted, String siteName ) {
        // Remove isDeleted elements unless includeDeleted.
        if ( includeDeleted || nodeRefs == null ) {
            return nodeRefs;
        }

        // initialize local variables
        ArrayList<NodeRef> correctedRefs = new ArrayList<NodeRef>();
        ArrayList<NodeRef> deletedRefs = null;
        boolean workspaceMatters = !ignoreWorkspace && workspace != null;
        if ( workspaceMatters ) {
            deletedRefs = new ArrayList<NodeRef>();
        }

        // Remove from the results the nodes that are flagged as deleted since
        // we are not including deleted nodes. If the workspace matters, then we
        // need to remove nodes that are upstream from its deletion, so we keep
        // track of which are deleted in this case.
        for ( NodeRef r : nodeRefs ) {
            if ( isDeleted( r ) ) {
                if ( workspaceMatters ) {
                    deletedRefs.add( r );
                }
            } else {
                correctedRefs.add( r );
            }
        }
        if ( workspaceMatters ) {
            // Remove from the results the nodes that are upstream from their
            // deletion.
            for ( NodeRef deleted : deletedRefs ) {
                EmsScriptNode dnode = new EmsScriptNode( deleted, getServices() );
                String dId = dnode.getSysmlId(); // assumes cm_name as backup to sysmlid for non-model elements
                // Remove all nodes with the same ID in parent workspaces.
                ArrayList<NodeRef> correctedRefsCopy = new ArrayList<NodeRef>(correctedRefs);
                for ( NodeRef corrected : correctedRefsCopy ) {
                    EmsScriptNode cnode = new EmsScriptNode( corrected, services );
                    String cId = cnode.getSysmlId();
                    // TODO -- REVIEW -- If the nodes are not model elements, and
                    // the cm:name is used, then the ids may not be the same across
                    // workspaces, in which case this fails.
                    if ( dId.equals( cId ) ) {
                        // Remove the upstream deleted node. Pass true since it
                        // doesn't hurt to remove nodes that should have already
                        // been removed, and we want to make sure we don't trip
                        // over a deleted intermediate source.
                        if ( isWorkspaceSource( corrected, deleted, true ) ) {
                            correctedRefs.remove( corrected );
                        }
                    }
                }
            }
        }
        nodeRefs = correctedRefs;

        return nodeRefs;
    }
//    protected static Map< String, Map< EmsScriptNode, Integer > > parentCache =
//            new HashMap< String, Map< EmsScriptNode, Integer > >();
//
//    protected static int parentDistance( NodeRef nodeRef, String parentScopeName,
//                                         boolean cache ) {
//        EmsScriptNode parent = new EmsScriptNode( nodeRef, getServices() );
//        Integer cachedValue = Utils.get( parentCache, parentScopeName, parent );
//        if ( cache ) {
//            if ( cachedValue != null ) {
//                return cachedValue.intValue();
//            }
//        } else if ( cachedValue != null ) {
//            parentCache.remove( parentScopeName );
//        }
//        int distance = 0;
//        while ( parent != null ) {
//            if ( parent.getName().equals(parentScopeName) ) {
//                if ( cache ) {
//                    Utils.put( parentCache, parentScopeName, parent, distance );
//                }
//                return distance;
//            }
//            parent = parent.getOwningParent( null ); // REVIEW -- need to pass in timestamp?
//            ++distance;
//        }
//        return Integer.MAX_VALUE;
//    }

    public static ArrayList< NodeRef > putInCache( String specifier, String prefix,
                                                   boolean ignoreWorkspaces,
                                                   WorkspaceNode workspace,
                                                   boolean onlyThisWorkspace,
                                                   Date dateTime,
                                                   boolean justFirst,
                                                   boolean exactMatch,
                                                   boolean includeDeleted,
                                                   String siteName,
                                                   ArrayList< NodeRef > results ) {
        String wsId = getWorkspaceId( workspace, ignoreWorkspaces );
        Long dateLong = dateTime == null ? 0 : dateTime.getTime();
        return Utils.put( elementCache, specifier, prefix, wsId, onlyThisWorkspace,
                          dateLong, justFirst, exactMatch, includeDeleted, "" + siteName, results );
    }

    public static ArrayList< NodeRef > getCachedElements( String specifier,
                                                          String prefix,
                                                          boolean ignoreWorkspaces,
                                                          WorkspaceNode workspace,
                                                          boolean onlyThisWorkspace,
                                                          Date dateTime,
                                                          boolean justFirst,
                                                          boolean exactMatch,
                                                          boolean includeDeleted,
                                                          String siteName) {
        String wsId = getWorkspaceId( workspace, ignoreWorkspaces );
        Long dateLong = dateTime == null ? 0 : dateTime.getTime();
        ArrayList< NodeRef > results =
                Utils.get( elementCache, specifier, prefix, wsId,
                           onlyThisWorkspace, dateLong, justFirst, exactMatch,
                           includeDeleted, "" + siteName );
        return results;
    }


    public static String getWorkspaceId( WorkspaceNode workspace, boolean ignoreWorkspaces ) {
        if ( ignoreWorkspaces ) return "all";
        return getWorkspaceId( workspace );
    }
    public static String getWorkspaceId( WorkspaceNode workspace ) {
        if ( workspace == null ) return "master";
        return workspace.getName();
    }

    public static WorkspaceNode getWorkspace( NodeRef nodeRef ) {
        EmsScriptNode node = new EmsScriptNode( nodeRef, getServices() );
        return node.getWorkspace();
    }

    public static boolean isWorkspaceAncestor( WorkspaceNode ancestor,
                                               WorkspaceNode child,
                                               boolean includeDeleted ) {
        if ( ancestor == null ) return true;
        if ( !exists(ancestor, includeDeleted) || !exists(child, includeDeleted) ) return false;
        WorkspaceNode parent = child.getParentWorkspace();
        if ( !exists( parent, includeDeleted ) ) return false;
        if ( ancestor.equals( parent, true ) ) return true;
        return isWorkspaceAncestor( ancestor, parent, includeDeleted );
    }

    /**
     * Determine whether the workspace of the source is an ancestor of that of
     * the changed node.
     *
     * @param source
     * @param changed
     * @param includeDeleted
     *            whether to consider deleted workspaces
     * @return
     */
    public static boolean isWorkspaceAncestor( NodeRef source,
                                               NodeRef changed,
                                               boolean includeDeleted ) {
        if ( !exists( source, true )
                || !exists( changed, true ) ) return false;
        WorkspaceNode ancestor = getWorkspace( source );
        WorkspaceNode child = getWorkspace( changed );
        return isWorkspaceAncestor( ancestor, child, includeDeleted );
    }

    public static boolean isWorkspaceSource( EmsScriptNode source,
                                             EmsScriptNode changed,
                                             boolean includeDeleted ) {
        // TODO: removed exists so we can include ems:Deleted nodes in results, may need to revisit
//        if (!exists(source) || !exists(changed)) return false;
        //if ( changed.equals( source ) ) return true;
        if ( !exists( source, includeDeleted )
             || !exists( changed, includeDeleted ) ) return false;
        if ( !changed.hasAspect( "ems:HasWorkspace" ) ) return false;
        EmsScriptNode directSource = changed.getWorkspaceSource();
//        if ( !exists(directSource) ) return false;
        if ( directSource == null || !directSource.scriptNodeExists() ) return false;
        // We pass true for tryCurrentVersions since they might be different
        // versions, and we don't care which versions.
        if ( source.equals( directSource, true ) ) return true;
        return isWorkspaceSource( source, directSource, includeDeleted );
    }

    public static boolean isWorkspaceSource( NodeRef source, NodeRef changed,
                                             boolean includeDeleted ) {
        if ( !exists( source, includeDeleted )
                || !exists( changed, includeDeleted ) ) return false;
//        if ( source == null || changed == null ) return false;
        EmsScriptNode sourceNode = new EmsScriptNode( source, getServices() );
        EmsScriptNode changedNode = new EmsScriptNode( changed, getServices() );
        return isWorkspaceSource( sourceNode, changedNode, includeDeleted );
    }

    /**
     * Find a NodeReference by id (returns first match, assuming things are
     * unique).
     *
     * @param id
     *            Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime
     *            the time specifying which version of the NodeRef to find; null
     *            defaults to the latest version
     * @return NodeRef of first match, null otherwise
     */
    public static NodeRef findNodeRefById(String id, //String parentScopeName,
                                          boolean ignoreWorkspace,
                                          WorkspaceNode workspace,
                                          Date dateTime, ServiceRegistry services, boolean findDeleted) {
        return findNodeRefById( id, ignoreWorkspace, workspace, dateTime, services, findDeleted, null );
    }

    /**
     * Find a NodeReference by id (returns first match, assuming things are
     * unique).
     *
     * @param id
     *            Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime
     *            the time specifying which version of the NodeRef to find; null
     *            defaults to the latest version
     * @return NodeRef of first match, null otherwise
     */
    public static NodeRef findNodeRefById(String id, //String parentScopeName,
                                          boolean ignoreWorkspace,
                                          WorkspaceNode workspace,
                                          Date dateTime, ServiceRegistry services, boolean findDeleted,
                                          String siteName) {
        ArrayList< NodeRef > array = findNodeRefsById(id, ignoreWorkspace, workspace, dateTime, services,
                                                      findDeleted, true, siteName);

        return !Utils.isNullOrEmpty(array) ? array.get( 0 ) : null;
    }

    /**
     * Find a NodeReferences by id
     *
     * @param id
     *            Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime
     *            the time specifying which version of the NodeRef to find; null
     *            defaults to the latest version
     * @return Array of NodeRefs found or empty list
     */
    public static ArrayList<NodeRef> findNodeRefsById(String id,
                                          boolean ignoreWorkspace,
                                          WorkspaceNode workspace,
                                          Date dateTime, ServiceRegistry services, boolean findDeleted,
                                          boolean justFirst) {

        return findNodeRefsById( id, ignoreWorkspace, workspace, dateTime, services, findDeleted, justFirst, null);
    }

    /**
     * Find a NodeReferences by id
     *
     * @param id
     *            Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime
     *            the time specifying which version of the NodeRef to find; null
     *            defaults to the latest version
     * @return Array of NodeRefs found or empty list
     */
    public static ArrayList<NodeRef> findNodeRefsById(String id,
                                          boolean ignoreWorkspace,
                                          WorkspaceNode workspace,
                                          Date dateTime, ServiceRegistry services, boolean findDeleted,
                                          boolean justFirst, String siteName) {

        ArrayList<NodeRef> returnArray = new ArrayList<NodeRef>();
        ArrayList< NodeRef > array = findNodeRefsByType(id, SearchType.ID.prefix,
                                                        ignoreWorkspace,
                                                        workspace, dateTime, justFirst, true,
                                                        services, findDeleted, siteName);

        EmsScriptNode esn = null;
        if (!Utils.isNullOrEmpty(array)) {
            for (NodeRef r : array) {
                if ( r != null ) {
                    esn = new EmsScriptNode( r, getServices() );
                }
                if ( r == null || (!esn.exists() && !esn.isDeleted()) ) {
                    r = findNodeRefByType( id, SearchType.CM_NAME.prefix,
                                           ignoreWorkspace,
                                           workspace, dateTime,
                                           true, services, findDeleted, siteName );
                }
                returnArray.add(r);
            }
        }
        else {
            returnArray = findNodeRefsByType(id, SearchType.CM_NAME.prefix,
                                             ignoreWorkspace,
                                             workspace, dateTime, justFirst, true,
                                             services, findDeleted, siteName);
        }

        return returnArray;
    }

    /**
     * Find a NodeReferences by sysml:name
     *
     * @param name
     * @param workspace
     * @param dateTime
     *            the time specifying which version of the NodeRef to find; null
     *            defaults to the latest version
     * @return Array of NodeRefs found or empty list
     */
    public static ArrayList<NodeRef> findNodeRefsBySysmlName(String name,
                                          boolean ignoreWorkspace,
                                          WorkspaceNode workspace,
                                          Date dateTime, ServiceRegistry services, boolean findDeleted,
                                          boolean justFirst) {

        ArrayList<NodeRef> returnArray = new ArrayList<NodeRef>();
        ArrayList< NodeRef > array = findNodeRefsByType(name, SearchType.NAME.prefix,
                                                        ignoreWorkspace,
                                                        workspace, dateTime, justFirst, true,
                                                        services, findDeleted);

        if (!Utils.isNullOrEmpty(array)) {
            for (NodeRef r : array) {
                if ( r != null ) {
                    returnArray.add(r);
                }
            }
        }

        return returnArray;
    }

    /**
     * Find EmsScriptNodes by sysml:name
     *
     * @param name
     * @param workspace
     * @param dateTime
     *            the time specifying which version of the NodeRef to find; null
     *            defaults to the latest version
     * @return Array of EmsScriptNodes found or empty list
     */
    public static ArrayList<EmsScriptNode> findScriptNodesBySysmlName(String name,
                                          boolean ignoreWorkspace,
                                          WorkspaceNode workspace,
                                          Date dateTime, ServiceRegistry services, boolean findDeleted,
                                          boolean justFirst) {

        ArrayList<EmsScriptNode> returnArray = new ArrayList<EmsScriptNode>();
        ArrayList<NodeRef> array = findNodeRefsBySysmlName(name,
                                                           ignoreWorkspace,
                                                           workspace,
                                                           dateTime, services, findDeleted,
                                                           justFirst);

        if (!Utils.isNullOrEmpty(array)) {
            for (NodeRef r : array) {
                if ( r != null ) {
                    returnArray.add(new EmsScriptNode(r, services));
                }
            }
        }

        return returnArray;
    }


    /**
     * Perform Lucene search for the specified pattern and ACM type
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type      escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    public static Map< String, EmsScriptNode >
      searchForElements( String pattern, boolean ignoreWorkspace,
                         WorkspaceNode workspace, Date dateTime,
                         ServiceRegistry services, StringBuffer response,
                         Status status) {
        Map<String, EmsScriptNode> elementsFound = new HashMap<String, EmsScriptNode>();
        for (SearchType searchType: SearchType.values() ) {
            elementsFound.putAll( searchForElements( searchType.prefix,
                                                     pattern, ignoreWorkspace,
                                                     workspace,
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
            searchForElements( String type, String pattern, boolean ignoreWorkspace,
                               WorkspaceNode workspace, Date dateTime,
                               ServiceRegistry services, StringBuffer response,
                               Status status,
                               String siteName) {
        Map<String, EmsScriptNode> searchResults = new HashMap<String, EmsScriptNode>();


        //ResultSet resultSet = null;
        ArrayList<NodeRef> resultSet = null;
        //try {

        resultSet = findNodeRefsByType( pattern, type, /*false,*/ ignoreWorkspace, workspace,
                                        dateTime, false, false, getServices(),
                                        false, siteName );
            for ( NodeRef nodeRef : resultSet ) {
                EmsScriptNode node =
                        new EmsScriptNode( nodeRef, services, response );
                if ( node.checkPermissions( PermissionService.READ, response, status ) ) {
                    String id = node.getSysmlId();
                    // We assume that order matters and that if two nodes have the
                    // same id, then the first is preferred (for example, because it
                    // is in the closest workspace).
                    if ( id != null && !searchResults.containsKey( id ) ) {
                        searchResults.put( id, node );
                    }
                }
            }

        return searchResults;
    }

    /**
     * Perform Lucene search for the specified pattern and ACM type
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type      escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    public static Map< String, EmsScriptNode >
            searchForElements( String type, String pattern, boolean ignoreWorkspace,
                               WorkspaceNode workspace, Date dateTime,
                               ServiceRegistry services, StringBuffer response,
                               Status status ) {

        return searchForElements(type, pattern, ignoreWorkspace,
                                 workspace, dateTime, services, response,
                                 status, null);
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
        // quick and dirty - using DictionaryService results in WARNINGS
        typeName = typeName.replace( "sysml:", "" );
        if (typeName.equals( "Element" ) ||
                typeName.equals( "Project" )) {
            return true;
        }
        return false;
//        if ( Acm.getJSON2ACM().keySet().contains( typeName ) ) {
//            typeName = Acm.getJSON2ACM().get( typeName );
//        }
//        if ( services == null ) services = getServices();
//        DictionaryService dServ = services.getDictionaryService();
//        QName qName = createQName( typeName, services );
//        if ( qName != null ) {
//            // TODO: this prints out a warning, maybe better way to check?
//            TypeDefinition t = dServ.getType( qName );
//            if ( t != null ) {
//              return true;
//            }
//        }
//        if (Debug.isOn()) System.out.println("isType(" + typeName + ") = false");


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
            String possibleString = Acm.getACM2JSON().get( s );
            // Bad mapping, ie type, just use the original string:
            if (possibleString != null) {
                s = possibleString;
            }
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
        boolean notType = !isType( type, services );
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
                                             boolean ignoreWorkspace,
                                             WorkspaceNode workspace,
                                             Date dateTime,
                                             ServiceRegistry services,
                                             StringBuffer response ) {
        if ( Utils.isNullOrEmpty( siteName ) ) return null;

        // Try to find the site in the workspace first.
        ArrayList< NodeRef > refs =
                findNodeRefsByType( siteName, SearchType.CM_NAME.prefix,
                                    ignoreWorkspace, workspace, dateTime, true,
                                    true, getServices(), false );
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
                siteRef = getNodeRefAtTime( siteRef, dateTime );
            }

            if (siteRef != null) {
                EmsScriptNode siteNode = new EmsScriptNode(siteRef, services, response);
                if ( siteNode != null
                     && ( workspace == null || workspace.contains( siteNode ) ) ) {
                    return siteNode;
                }
            }
        }
        return null;
    }

    /**
     * Get site of specified short name.  This also checks that the returned node is
     * in the specified workspace, not just whether its in the workspace or any of its parents.
     *
     * @param siteName
     * @return  ScriptNode of site with name siteName
     */
    public static EmsScriptNode getSiteNodeForWorkspace( String siteName,
			                                             boolean ignoreWorkspace,
			                                             WorkspaceNode workspace,
			                                             Date dateTime,
			                                             ServiceRegistry services,
			                                             StringBuffer response ) {

    	EmsScriptNode siteNode = getSiteNode(siteName, ignoreWorkspace, workspace,
    										 dateTime, services, response);
		return (siteNode != null && workspacesEqual(siteNode.getWorkspace(),workspace)) ? siteNode : null;

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
        return findNodeRefByAlfrescoId(id, false);
    }

    public static NodeRef findNodeRefByAlfrescoId(String id, boolean includeDeleted) {
        return findNodeRefByAlfrescoId(id, includeDeleted, true);
    }

    public static NodeRef findNodeRefByAlfrescoId(String id, boolean includeDeleted,
                                                  boolean giveError) {
        if ( !id.contains( "://" ) ) {
            id = "workspace://SpacesStore/" + id;
        }
        if ( !NodeRef.isNodeRef( id ) ) {
            if (giveError) {
                Debug.error("Bad NodeRef id: " + id );
            }
            return null;
        }
        NodeRef n = new NodeRef(id);
        EmsScriptNode node = new EmsScriptNode( n, getServices() );
        if ( !node.exists() ) {
            if (includeDeleted && node.isDeleted()) {
                return n;
            } else {
                return null;
            }
        }
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

    public static int compareVersions( NodeRef ref1, NodeRef ref2 ) {
        Date d1 = getLastModified( ref1 );
        Date d2 = getLastModified( ref2 );
        return CompareUtils.compare( d1, d2 );
//        VersionService vs = getServices().getVersionService();
//        if ( vs == null ) {
//            //TODO -- BAD!
//        }
//        NodeService ns = getServices().getNodeService();
//        ns.getProperty( ref1, QName.createQName( "cm:lastModified" ) );
//        //vs.createVersion( nodeRef, versionProperties, versionChildren );
    }

    public static Date getLastModified( NodeRef ref ) {
        try {
        QName typeQName = createQName( Acm.ACM_LAST_MODIFIED );
        Date date = (Date)services.getNodeService().getProperty( ref, typeQName );
            return date;
        } catch ( Throwable t ) {
            t.printStackTrace();
        }
        return null;
    }

    public static NodeRef getNodeRefAtTime( NodeRef nodeRef, WorkspaceNode workspace,
                                            Date dateTime ) {
        return getNodeRefAtTime( nodeRef, workspace, dateTime, false, false);
    }

    public static NodeRef getNodeRefAtTime( NodeRef nodeRef, WorkspaceNode workspace,
                                            Date dateTime, boolean ignoreWorkspace,
                                            boolean findDeleted) {
        EmsScriptNode node = new EmsScriptNode( nodeRef, getServices() );
        String id = node.getSysmlId();
        return findNodeRefById( id, ignoreWorkspace, workspace, dateTime, getServices(), findDeleted );
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
        NodeRef ref = findNodeRefById( id, false, workspace, dateTime, getServices(), false );
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
        if ( !NodeUtil.exists( ref ) && !NodeUtil.isDeleted( ref ) ) {
            return null;
        }
        if (ref == null || dateTime == null ) {
            return ref;
        }
        VersionHistory history = getServices().getVersionService().getVersionHistory( ref );
        if ( history == null ) {
        		// Versioning doesn't make versions until the first save...
        		EmsScriptNode node = new EmsScriptNode(ref, services);
        		Date createdTime = (Date)node.getProperty("cm:created");
        		if ( dateTime != null && createdTime != null && dateTime.compareTo( createdTime ) < 0 ) {
                if (Debug.isOn())  Debug.outln( "no history! dateTime " + dateTime
                                    + " before created " + createdTime );
        			return null;
        		}
            if (Debug.isOn() && createdTime != null)  Debug.outln( "no history! created " + createdTime );
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
                source = getSiteNode( siteName, true, null, null, services, response );
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
                    source.getOrSetCachedVersion();
                    if ( folder == null ) {
                        log( "Can't create folder for path " + path + "!", response );
                        return null;
                    } else {
                        folder.getOrSetCachedVersion();
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

    public static boolean isDeleted( EmsScriptNode node ) {
        if ( node == null ) return false;
        return node.isDeleted();
    }

    public static boolean isDeleted( NodeRef ref ) {
        if ( ref == null ) return false;
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        return node.isDeleted();
    }

    public static boolean exists( EmsScriptNode node ) {
        return exists( node, false );
    }
    public static boolean exists( EmsScriptNode node, boolean includeDeleted ) {
        if ( node == null ) return false;
        return node.exists( includeDeleted );
    }

    public static boolean exists( NodeRef ref ) {
        return exists( ref, false );
    }
    public static boolean exists( NodeRef ref, boolean includeDeleted ) {
        if ( ref == null ) return false;
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        return node.exists( includeDeleted );
    }

    public static boolean scriptNodeExists( NodeRef ref ) {
        if ( ref == null ) return false;
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        return node.scriptNodeExists();
    }

    public static String getUserName() {
        String userName = AuthenticationUtil.getRunAsUser();
        return userName;
    }

    public static EmsScriptNode getUserHomeFolder() {
        String userName = getUserName();
        return getUserHomeFolder( userName );
    }

    public static EmsScriptNode getUserHomeFolder( String userName ) {
        return getUserHomeFolder(userName, false);
    }

    public static EmsScriptNode getUserHomeFolder( String userName, boolean createIfNotFound) {
        NodeRef homeFolderNode = null;
        EmsScriptNode homeFolderScriptNode = null;
        if ( userName.equals( "admin" ) ) {
            homeFolderNode =
                    findNodeRefByType( userName, SearchType.CM_NAME, /*true,*/ true, null,
                                       null, true, getServices(), false );
        } else {
            PersonService personService = getServices().getPersonService();
            NodeService nodeService = getServices().getNodeService();
            NodeRef personNode = personService.getPerson(userName);
            homeFolderNode =
                    (NodeRef)nodeService.getProperty( personNode,
                                                      ContentModel.PROP_HOMEFOLDER );
        }
        if ( homeFolderNode == null || !exists(homeFolderNode) ) {
            NodeRef ref = findNodeRefById( "User Homes", true, null, null, getServices(), false );
            EmsScriptNode homes = new EmsScriptNode( ref, getServices() );
            if ( createIfNotFound && homes != null && homes.exists() ) {
                homeFolderScriptNode = homes.createFolder( userName );
                if ( homeFolderScriptNode != null ) homeFolderScriptNode.getOrSetCachedVersion();
                homes.getOrSetCachedVersion();
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
            if ( findNodeRefById( id, true, null, null, services, false ) == null ) {
                return id;
            }
        }
        Debug.error( true, "Could not create a unique id!" );
        return null;
    }

    public static Set< QName > getAspects( NodeRef ref ) {
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        return node.getAspectsSet();
    }

    public static List<String> qNamesToStrings( Collection<QName> qNames ) {
        List<String> names = new ArrayList< String >();
        for ( QName qn : qNames ) {
            names.add( qn.toPrefixString() );
        }
        return names;
    }

    public static Set< String > getNames( Collection< NodeRef > refs ) {
        List< EmsScriptNode > nodes = EmsScriptNode.toEmsScriptNodeList( refs );
        TreeSet< String > names =
                new TreeSet< String >( EmsScriptNode.getNames( nodes ) );
        return names;
    }

    public static Set< String > getSysmlIds( Collection< NodeRef > refs ) {
        List< EmsScriptNode > nodes = EmsScriptNode.toEmsScriptNodeList( refs );
        TreeSet< String > names =
                new TreeSet< String >( EmsScriptNode.getSysmlIds( nodes ) );
        return names;
    }

    public static String getName( NodeRef ref ) {
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        return node.getName();
    }

    public static String getSysmlId( NodeRef ref ) {
        EmsScriptNode node = new EmsScriptNode( ref, getServices() );
        return node.getSysmlId();
    }

    public static Set<NodeRef> getModelElements( Set<NodeRef> s1 ) {
        Set<NodeRef> newSet1 = new LinkedHashSet< NodeRef >();
        if (s1 != null) {
            for ( NodeRef ref : s1 ) {
                if ( EmsScriptNode.isModelElement( ref ) ) {
                    newSet1.add( ref );
                }
            }
        }
        return newSet1;
    }

    public static List< NodeRef > getNodeRefs( Collection< EmsScriptNode > nodes ) {
        List< NodeRef > refs = new ArrayList< NodeRef >();
        for ( EmsScriptNode node : nodes ) {
            NodeRef ref = node.getNodeRef();
            if ( ref != null ) refs.add( ref );
        }
        return refs;
    }

    public static EmsScriptNode findScriptNodeById( String id,
                                                    WorkspaceNode workspace,
                                                    Date dateTime,
                                                    boolean findDeleted,
                                                    //Map< String, EmsScriptNode > simpleCache,
                                                    ServiceRegistry services,
                                                    StringBuffer response ) {

        return findScriptNodeById( id, workspace, dateTime, findDeleted, services, response, null );
    }

    public static EmsScriptNode findScriptNodeById( String id,
                                                    WorkspaceNode workspace,
                                                    Date dateTime,
                                                    boolean findDeleted,
                                                    //Map< String, EmsScriptNode > simpleCache,
                                                    ServiceRegistry services,
                                                    StringBuffer response,
                                                    String siteName) {

        timer = Timer.startTimer(timer, timeEvents);
        NodeRef nodeRef = findNodeRefById(id, false, workspace, dateTime, services, findDeleted, siteName);
        if ( nodeRef == null ) {
            Timer.stopTimer(timer, "====== findScriptNodeById(): failed end time", timeEvents);
            return null;
        }
        Timer.stopTimer(timer, "====== findScriptNodeById(): end time", timeEvents);
        return new EmsScriptNode( nodeRef, services );
    }

	public static EmsScriptNode findScriptNodeByIdForWorkspace(String id,
															   WorkspaceNode workspace,
															   Date dateTime, boolean findDeleted,
															   ServiceRegistry services,
			                                                   StringBuffer response) {

		EmsScriptNode node = findScriptNodeById( id, workspace, dateTime, findDeleted,
												services, response );
		return (node != null && workspacesEqual(node.getWorkspace(),workspace)) ? node : null;

	}

    /**
     * Returns true if the passed workspaces are equal, checks for master (null) workspaces
     * also
     *
     * @param ws1
     * @param ws2
     * @return
     */
	public static boolean workspacesEqual(WorkspaceNode ws1, WorkspaceNode ws2)
	{
		return ( (ws1 == null && ws2 == null) || (ws1 != null && ws1.equals(ws2)) );
	}

    /**
     * Updates or creates a artifact with the passed name/type in the specified site name/workspace
     * with the specified content.
     *
     * Only updates the artifact if found if updateIfFound is true.
     *
     * @param name
     * @param type
     * @param base64content
     * @param targetSiteName
     * @param subfolderName
     * @param workspace
     * @param dateTime
     * @param updateIfFound
     * @param response
     * @param status
     * @return
     */
    public static EmsScriptNode updateOrCreateArtifact( String name, String type,
									            		String base64content,
									            		String strContent,
									            		String targetSiteName,
									            		String subfolderName,
									            		WorkspaceNode workspace,
									            		Date dateTime,
									            		StringBuffer response,
									            		Status status,
									            		boolean ignoreName) {

    	EmsScriptNode artifactNode;
    	String myType = Utils.isNullOrEmpty(type) ? "svg" : type;
    	String finalType = myType.startsWith(".") ? myType.substring(1) : myType;
		String artifactId = name + "." + finalType;

		byte[] content =
		( base64content == null )
		      ? null
		      : DatatypeConverter.parseBase64Binary( base64content );

		if (content == null && strContent != null) {
			content = strContent.getBytes(Charset.forName("UTF-8"));
		}

		long cs = EmsScriptNode.getChecksum( content );

		// see if image already exists by looking up by checksum
		ArrayList< NodeRef > refs =
				findNodeRefsByType( "" + cs,
		          SearchType.CHECKSUM.prefix, false,
		          workspace, dateTime, false, false,
		          services, false );
		// ResultSet existingArtifacts =
		// NodeUtil.findNodeRefsByType( "" + cs, SearchType.CHECKSUM,
		// services );
		// Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( existingArtifacts
		// );
		List< EmsScriptNode > nodeList = EmsScriptNode.toEmsScriptNodeList( refs, services, response, status );
		// existingArtifacts.close();

		EmsScriptNode matchingNode = null;

		if ( nodeList != null && nodeList.size() > 0 ) {
			matchingNode = nodeList.iterator().next();
		}

		// No need to update if the checksum and name match (even if it is in a parent branch):
		if ( matchingNode != null && (ignoreName || matchingNode.getSysmlId().equals(artifactId)) ) {
			return matchingNode;
		}

		// Create new artifact:
		// find subfolder in site or create it
		String artifactFolderName =
		"Artifacts"
		+ ( Utils.isNullOrEmpty( subfolderName )
		                             ? ""
		                             : "/"
		                               + subfolderName );

		EmsScriptNode targetSiteNode = getSiteNodeForWorkspace( targetSiteName, false, workspace, dateTime,
									  							services, response );

		// find site; it must exist!
		if ( targetSiteNode == null || !targetSiteNode.exists() ) {
			Debug.err( "Can't find node for site: " + targetSiteName + "!\n" );
			return null;
		}

		// find or create subfolder
		EmsScriptNode subfolder = mkdir( targetSiteNode, artifactFolderName, services,
										 response, status );
		if ( subfolder == null || !subfolder.exists() ) {
			Debug.err( "Can't create subfolder for site, " + targetSiteName
			+ ", in artifact folder, " + artifactFolderName + "!\n" );
			return null;
		}

		// find or create node:
		artifactNode = findScriptNodeByIdForWorkspace(artifactId, workspace, dateTime, false,
										  			  services, response);

		// Node wasnt found, so create one:
		if (artifactNode == null) {
			artifactNode = subfolder.createNode( artifactId, "cm:content" );
			subfolder.getOrSetCachedVersion();
		}

		if ( artifactNode == null || !artifactNode.exists() ) {
			Debug.err( "Failed to create new artifact " + artifactId + "!\n" );
			return null;
		}

        artifactNode.makeSureNodeRefIsNotFrozen();
		if (!artifactNode.hasAspect( "cm:versionable")) {
		    artifactNode.addAspect( "cm:versionable" );
		}
		if (!artifactNode.hasAspect( "cm:indexControl" )) {
			artifactNode.addAspect( "cm:indexControl" );
		}
		if (!artifactNode.hasAspect( Acm.ACM_IDENTIFIABLE )) {
			artifactNode.addAspect( Acm.ACM_IDENTIFIABLE );
		}
		if (!artifactNode.hasAspect( "view:Checksummable" )) {
			artifactNode.addAspect( "view:Checksummable" );
		}

		artifactNode.createOrUpdateProperty( Acm.CM_TITLE, artifactId );
		artifactNode.createOrUpdateProperty( "cm:isIndexed", true );
		artifactNode.createOrUpdateProperty( "cm:isContentIndexed", false );
		artifactNode.createOrUpdateProperty( Acm.ACM_ID, artifactId );
		artifactNode.createOrUpdateProperty( "view:cs", cs );

		if ( Debug.isOn() ) {
			System.out.println( "Creating artifact with indexing: "  + artifactNode.getProperty( "cm:isIndexed" ) );
		}

		ContentWriter writer =
		services.getContentService().getWriter( artifactNode.getNodeRef(),
												ContentModel.PROP_CONTENT, true );
		InputStream contentStream = new ByteArrayInputStream( content );
		writer.putContent( contentStream );

		ContentData contentData = writer.getContentData();
		contentData = ContentData.setMimetype( contentData, EmsScriptNode.getMimeType( finalType ) );
		if (base64content == null) {
			contentData = ContentData.setEncoding( contentData, "UTF-8");
		}
        artifactNode.makeSureNodeRefIsNotFrozen();
        artifactNode.transactionCheck();
		services.getNodeService().setProperty( artifactNode.getNodeRef(),
		            							ContentModel.PROP_CONTENT,contentData );

        // if only version, save dummy version so snapshots can reference
        // versioned images - need to check against 1 since if someone
        // deleted previously a "dead" version is left in its place
		Object[] versionHistory = artifactNode.getEmsVersionHistory();

        if (versionHistory == null || versionHistory.length <= 1) {
            artifactNode.makeSureNodeRefIsNotFrozen();
        	artifactNode.createVersion("creating the version history", false);
        }

        artifactNode.getOrSetCachedVersion();

		return artifactNode;
	}


    /**
     * Given a parent and path, builds the path recursively as necessary
     * @param parent
     * @param path
     * @return
     */
    public static EmsScriptNode getOrCreatePath(EmsScriptNode parent, String path) {
        if ( parent == null ) return null;
        String tokens[] = path.split( "/" );

        String childPath = null;
        for (int ii = 0; ii < tokens.length; ii++) {
            if (!tokens[ii].isEmpty()) {
                childPath = tokens[ii];
                break;
            }
        }

        if (childPath != null) {
            EmsScriptNode child = parent.childByNamePath( childPath );
            if (child == null) {
                child = parent.createFolder( childPath );
                if ( child != null ) child.getOrSetCachedVersion();
                parent.getOrSetCachedVersion();
            }

            if (child != null) {
                if (path.startsWith( "/" )) {
                    if (path.length() >= 1) {
                        path = path.substring( 1 );
                    }
                }
                path = path.replaceFirst( childPath, "" );
                if (!path.isEmpty()) {
                    return getOrCreatePath(child, path);
                } else {
                    return child;
                }
            }
        }

        return null;
    }


    /**
     * Get or create the date folder based on /year/month/day_of_month provided
     * a parent folder
     * @param parent
     * @return
     */
    public static EmsScriptNode getOrCreateDateFolder(EmsScriptNode parent) {
        Calendar cal = Calendar.getInstance();

        String year = Integer.toString( cal.get(Calendar.YEAR) );
        String month = Integer.toString( cal.get(Calendar.MONTH) + 1);
        String day = Integer.toString( cal.get(Calendar.DAY_OF_MONTH));

        String path = String.format("/%s/%s/%s", year, month, day);
        return getOrCreatePath(parent, path);
    }

//    private static  <X extends Serializable, V> void clearAlfrescoNodeCache() {
    public static void clearAlfrescoNodeCache() {
        try {
            //org.alfresco.repo.cache.TransactionalCache< X, V > c;
            //getServiceRegistry().getTransactionService().
//          ((DbNodeServiceImpl)getServiceRegistry().getNodeService()).nodeDAO.clear();
//          ((DbNodeServiceImpl)((Version2ServiceImpl)getServiceRegistry().getVersionService()).dbNodeService).nodeDAO.clear();
            DbNodeServiceImpl dbNodeService = (DbNodeServiceImpl)getServiceRegistry().getNodeService();
            Object[] emptyArray = new Object[]{};
            Method method = ClassUtils.getMethodForArgs( NodeDAO.class, "clear", emptyArray );
//          Field f = ClassUtils.getField( DbNodeServiceImpl.class, "nodeDAO", true );
//          NodeDAO nodeDao = (NodeDAO)f.get( dbNodeService );
            NodeDAO nodeDao = (NodeDAO)ClassUtils.getFieldValue( dbNodeService, "nodeDAO", false );
            MethodCall mc = new MethodCall( nodeDao, method ,emptyArray );
            mc.invoke( false );
        } catch ( Throwable e ) {
            e.printStackTrace();
        }
    }

    public static void transactionCheck( Log logger, EmsScriptNode node ) {
        //logger.error( "inTransaction = " + NodeUtil.isInsideTransactionNow() );
        if ( NodeUtil.isInsideTransactionNow() ) {
            if ( NodeUtil.hasBeenOutsideTransaction() ) {
                Exception e = new Exception();
                logger.error( "In transaction when have been outside! " + node,
                              e );
                logger.error( "Stack trace when last outside transaction:\n"
                              + Utils.toString( getOutsideTransactionStackTrace() ) );
            }
            NodeUtil.setBeenInsideTransaction( true );
            setInsideTransactionStackTrace();
        } else {
            if ( NodeUtil.hasBeenInsideTransaction() ) {
                Exception e = new Exception();
                logger.error( "Outside transaction when have been inside! "
                              + node, e );
                logger.error( "Stack trace when last inside transaction:\n"
                        + Utils.toString( getInsideTransactionStackTrace() ) );
            }
            NodeUtil.setBeenOutsideTransaction( true );
            setOutsideTransactionStackTrace();
        }
    }

    
    public static void transactionCheck( Logger logger, EmsScriptNode node ) {
        //logger.error( "inTransaction = " + NodeUtil.isInsideTransactionNow() );
        if ( NodeUtil.isInsideTransactionNow() ) {
            if ( NodeUtil.hasBeenOutsideTransaction() ) {
                Exception e = new Exception();
                logger.error( "In transaction when have been outside! " + node,
                              e );
                logger.error( "Stack trace when last outside transaction:\n"
                              + Utils.toString( getOutsideTransactionStackTrace() ) );
            }
            NodeUtil.setBeenInsideTransaction( true );
            setInsideTransactionStackTrace();
        } else {
            if ( NodeUtil.hasBeenInsideTransaction() ) {
                Exception e = new Exception();
                logger.error( "Outside transaction when have been inside! "
                              + node, e );
                logger.error( "Stack trace when last inside transaction:\n"
                        + Utils.toString( getInsideTransactionStackTrace() ) );
            }
            NodeUtil.setBeenOutsideTransaction( true );
            setOutsideTransactionStackTrace();
        }
      }
    /**
     * FIXME Recipients and senders shouldn't be hardcoded - need to have these spring injected
     * @param subject
     * @param msg
     */
    public static void sendNotificationEvent( String subject, String msg,
                                              ServiceRegistry services ) {
        if (!heisenbugSeen) {
            String hostname = services.getSysAdminParams().getAlfrescoHost();
            
            String sender = hostname + "@jpl.nasa.gov";
            String recipient;
            
            if (hostname.toLowerCase().contains( "europa" )) {
                recipient = "kerzhner@jpl.nasa.gov";
                ActionUtil.sendEmailTo( sender, recipient, msg, subject, services );
            }
            recipient = "mbee-dev-admin@jpl.nasa.gov";
            ActionUtil.sendEmailTo( sender, recipient, msg, subject, services );
            heisenbugSeen = true;
        }
    }

}
