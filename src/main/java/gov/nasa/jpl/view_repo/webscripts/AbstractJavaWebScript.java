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
package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.ae.event.Call;
import gov.nasa.jpl.ae.event.ConstraintExpression;
import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.event.Parameter;
import gov.nasa.jpl.ae.event.ParameterListenerImpl;
import gov.nasa.jpl.ae.solver.Constraint;
import gov.nasa.jpl.ae.solver.ConstraintLoopSolver;
import gov.nasa.jpl.ae.solver.SingleValueDomain;
import gov.nasa.jpl.ae.solver.Variable;
import gov.nasa.jpl.ae.sysml.SystemModelSolver;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Random;
import gov.nasa.jpl.mbee.util.Seen;
import gov.nasa.jpl.mbee.util.SeenHashSet;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.pma.JenkinsBuildConfig;
import gov.nasa.jpl.pma.JenkinsEngine;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff.DiffType;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.cmr.version.Version;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import sysml.SystemModel;


/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities
 * that were in utils.js
 *
 * @author cinyoung
 *
 */
public abstract class AbstractJavaWebScript extends DeclarativeJavaWebScript {
	
	private static Logger logger = Logger.getLogger(AbstractJavaWebScript.class);
    // FIXME -- Why is this not static? Concurrent webscripts with different
    // loglevels will interfere with each other.
    public Level logLevel = Level.WARN;

    public Formatter formatter = new Formatter ();
    /*public enum LogLevel {
		DEBUG(0), INFO(1), WARNING(2), ERROR(3);
		private int value;
		private LogLevel(int value) {
			this.value = value;
		}
	}*/

    protected static boolean writeConstraintsOut = true;

    public static final int MAX_PRINT = 200;
    public static boolean checkMmsVersions = false;
    public static boolean defaultRunWithoutTransactions = false;
    private JSONObject privateRequestJSON = null;
    // injected members
	protected ServiceRegistry services;		// get any of the Alfresco services
	protected Repository repository;		// used for lucene search

	// internal members
    // when run in background as an action, this needs to be false
    public boolean runWithoutTransactions = defaultRunWithoutTransactions;
    //public UserTransaction trx = null;
	protected ScriptNode companyhome;
	protected Map<String, EmsScriptNode> foundElements = new LinkedHashMap<String, EmsScriptNode>();
    protected Map<String, EmsScriptNode> movedAndRenamedElements = new LinkedHashMap<String, EmsScriptNode>();

	// needed for Lucene search
	protected static final StoreRef SEARCH_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    protected Status responseStatus = new Status();
    protected String deploymentName;

    protected WorkspaceDiff wsDiff = null;

    public static boolean alwaysTurnOffDebugOut = true;

    // keeps track of who made the call to the service
    protected String source = null;
    protected EmsSystemModel systemModel;
    protected SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAe;
    protected static SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > globalSysmlToAe;

    public boolean usingPermCache = true;
    enum PermType { READ, WRITE };
    public static PermType getPermType(String permission) {
        PermType permType = permission.charAt( 0 ) == 'W' ? PermType.WRITE : PermType.READ;
        return permType;
    }
    // Permission map:  username -> node -> permission_type -> true|false
    public Map< String, Map< NodeRef, Map< PermType, Boolean > > > permissionCache =
            new HashMap< String, Map< NodeRef, Map< PermType, Boolean > > >();

    public Boolean permCacheGet( String realUser, NodeRef nodeRef, String permission ) {
        Map< PermType, Boolean > innerMap = Utils.get( permissionCache, realUser, nodeRef );
        if ( innerMap == null ) return null;
        PermType permType = getPermType(permission);
        return innerMap.get( permType );
    }
    public void permCachePut( String realUser, NodeRef nodeRef,
                                 String permission, boolean b ) {
        Utils.put( permissionCache, realUser, nodeRef, getPermType(permission), b );
    }

    // Temporary cache of element id to property name to value used for jobs.
    LinkedHashMap< String, Map< String, String > > propertyValues =
            new LinkedHashMap< String, Map< String, String > >();
    LinkedHashMap< String, Map< String, JSONObject > > propertyJson =
            new LinkedHashMap< String, Map< String, JSONObject > >();
//    LinkedHashMap<String, JSONObject> instanceSpecs = 
//            new LinkedHashMap< String, JSONObject >();

    public boolean usingExistsCache = false; // not yet implemented
    enum ExistType { InAlfresco, InModel };
    // Cache for whether node exists: node -> exist_type -> true|false
    public Map< NodeRef, Map< ExistType, Boolean > > existsCache =
            new HashMap< NodeRef, Map< ExistType, Boolean > >();

    public boolean usingAspectsCache = false; // not yet implemented
    // Cache for whether node exists: node -> aspect -> true|false
    public Map< NodeRef, Map< ExistType, Boolean > > hasAspectCache =
            new HashMap< NodeRef, Map< ExistType, Boolean > >();
    protected Map< String, List< EmsScriptNode > > elementProperties = new HashMap< String, List< EmsScriptNode > >();

    protected Map< String, Map< String, String > > teamworkMap = new HashMap< String, Map< String, String > >();
    
    /**
     * The defining features of slots for the Job stereotype.
     */
    static Map<String, String> definingFeatures = new LinkedHashMap< String, String >() {
        private static final long serialVersionUID = -6503314776361243306L;
        {
            // WARNING! FIXME! THESE MAY CHANGE!  MAYBE SEARCH FOR THEM?
            put( "status", "_18_0_2_6620226_1453945722485_173783_14567");
            put( "schedule", "_18_0_2_6620226_1453945600718_861466_14565");
            put( "command", "_18_0_2_6620226_1453945276117_966030_14557");
            put( "url", "_18_0_2_6620226_1458836769913_512779_14411");
        }
    };

    // This array can be populated with json for found jobs when generating json
    // for elements.  This is defined in AbstractJavaWebscript because job json
    // will be sent with commit deltas for ModelPost.
    protected JSONArray jobsJsonArray = new JSONArray();
    
    // This object will be used to get the job url which can be linked to Jenkins 
    protected JSONObject jobUrl = new JSONObject();

    protected static String[] jobProperties =
            Utils.toArrayOfType( definingFeatures.keySet().toArray(), String.class );

    protected void initMemberVariables(String siteName) {
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
	    if ( repositoryHelper == null ) return;
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
        if ( registry == null ) return;
		this.services = registry;
	}
	
	public void setDeploymentName(String deploymentName) {
        if ( deploymentName == null ) return;
		this.deploymentName = deploymentName;
	}

	public AbstractJavaWebScript( Repository repository,
                                  ServiceRegistry services,
                                  StringBuffer response ) {
        this.setRepositoryHelper( repository );
        this.setServices( services );
        this.response = response ;
        // TODO -- set maximum log level; Overrides that specified in log4j.properties (I THINK)
        // FIXME -- Why is this not static?
        logger.setLevel(logLevel);
    }

    public AbstractJavaWebScript(Repository repositoryHelper, ServiceRegistry registry) {
        this.setRepositoryHelper(repositoryHelper);
        this.setServices(registry);
        // TODO -- set maximum log level; Overrides that specified in log4j.properties (I THINK)
        // FIXME -- Why is this not static?
        logger.setLevel(logLevel);
    }

    public AbstractJavaWebScript() {
        // default constructor for spring
        super();
        // TODO -- set maximum log level; Overrides that specified in log4j.properties (I THINK)
        // FIXME -- Why is this not static?
        logger.setLevel(logLevel);
    }

    /**
	 * Utility for clearing out caches
	 * TODO: do we need to clear caches if Spring isn't making singleton instances
	 */
    protected void clearCaches() {
        clearCaches( true );
    }
	protected void clearCaches( boolean resetTransactionState ) {
	    if ( resetTransactionState ) {
            NodeUtil.setBeenInsideTransaction( false );
            NodeUtil.setBeenOutsideTransaction( false );
            NodeUtil.setInsideTransactionNow( false );
	    }
	
		foundElements = new HashMap<String, EmsScriptNode>();
		response = new StringBuffer();
		responseStatus.setCode(HttpServletResponse.SC_OK);
        NodeUtil.initHeisenCache();
        if ( alwaysTurnOffDebugOut  ) {
            Debug.turnOff();
        }
	}
	
    protected void cleanJsonCache() {
        if ( !NodeUtil.doJsonCaching && !NodeUtil.doJsonDeepCaching &&
             !NodeUtil.doJsonStringCaching ) {
            return;
        }
        Map< String, EmsScriptNode > nodesToClean = new LinkedHashMap< String, EmsScriptNode >();

        Seen< String > seen = new SeenHashSet< String >();
        for ( EmsScriptNode node : foundElements.values() ) {
            if ( node.renamed || node.moved ) {
                String sysmlId = node.getSysmlId();
                collectChildNodesToClean( sysmlId, node, nodesToClean, seen );
            }
        }

        for ( EmsScriptNode node : nodesToClean.values() ) {
            node.removeFromJsonCache( false );
        }
    }

    protected void collectChildNodesToClean( String id, EmsScriptNode node,
                                             Map< String, EmsScriptNode > nodesToClean,
                                             Seen< String > seen ) {
        //String sysmlId = node.getSysmlId();

        Pair< Boolean, Seen< String > > p = Utils.seen( id, true, seen );
        if ( p.first ) return;
        seen = p.second;

        ArrayList< NodeRef > children = node.getOwnedChildren( true, null, null );
        for ( NodeRef ref : children ) {
            EmsScriptNode childNode = new EmsScriptNode( ref, getServices() );
            String sysmlId = childNode.getSysmlId();
            if ( foundElements.containsKey( sysmlId ) ) continue;
            if ( nodesToClean.containsKey( sysmlId ) ) continue;
            nodesToClean.put( sysmlId, childNode );
            collectChildNodesToClean( sysmlId, childNode, nodesToClean, seen );
        }

    }

    abstract protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
                                                              final Status status,
                                                              final Cache cache );

    protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
                                                     final Status status, final Cache cache,
                                                     boolean withoutTransactions ) {
    	
    	final Map< String, Object > model = new HashMap<String, Object>();
    	
    	if(checkMmsVersions)
        {
            if(compareMmsVersions(req, getResponse(), status))
            {
                model.put("res", createResponseJson());
                return model;
            }
        }

        clearCaches( true );
        clearCaches(); // calling twice for those redefine clearCaches() to
                       // always call clearCaches( false )

        new EmsTransaction( getServices(), getResponse(), getResponseStatus(), withoutTransactions ) {
            @Override
            public void run() throws Exception {
                Map< String, Object > m = executeImplImpl( req, status, cache );
                if ( m != null ) {
                    model.putAll( m );
                }
            }
        };
        if ( !model.containsKey( "res" ) && response != null && response.toString().length() > 0 ) {
            model.put( "res", response.toString() );

        }
        // need to check if the transaction resulted in rollback, if so change the status code
        // TODO: figure out how to get the response message in (response is always empty)
        if (getResponseStatus().getCode() != HttpServletResponse.SC_ACCEPTED) {
            status.setCode( getResponseStatus().getCode() );

        }
		
        return model;
    }


	/**
	 * Parse the request and do validation checks on request
	 * TODO: Investigate whether or not to deprecate and/or remove
	 * @param req		Request to be parsed
	 * @param status	The status to be returned for the request
	 * @return			true if request valid and parsed, false otherwise
	 */
	abstract protected boolean validateRequest(WebScriptRequest req, Status status);

    /**
     * Get site by name, workspace, and time
     *
     * @param siteName
     *            short name of site
     * @param workspace
     *            the workspace of the version of the site to return
     * @param dateTime
     *            the point in time for the version of the site to return
     * @return
     */
    protected EmsScriptNode getSiteNode(String siteName, WorkspaceNode workspace,
                                        Date dateTime ) {
        return getSiteNode( siteName, workspace, dateTime, true );
    }
    protected EmsScriptNode getSiteNode(String siteName, WorkspaceNode workspace,
                                        Date dateTime, boolean errorOnNull) {
        return getSiteNodeImpl(siteName, workspace, dateTime, false, errorOnNull);
    }

    /**
     * Helper method for getSideNode* methods
     *
     * @param siteName
     * @param workspace
     * @param dateTime
     * @param forWorkspace
     * @return
     */
    private EmsScriptNode getSiteNodeImpl(String siteName, WorkspaceNode workspace,
            						 	   Date dateTime, boolean forWorkspace, boolean errorOnNull) {

		EmsScriptNode siteNode = null;

		if (siteName == null) {
		    if ( errorOnNull ) log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,"No sitename provided" );
		} else {
			if (forWorkspace) {
				siteNode = NodeUtil.getSiteNodeForWorkspace( siteName, false, workspace, dateTime,
     											 			 services, response );
			}
			else {
				siteNode = NodeUtil.getSiteNode( siteName, false, workspace, dateTime,
			                 					services, response );
			}
	        if ( errorOnNull && siteNode == null ) {	
	            log(Level.ERROR,  HttpServletResponse.SC_BAD_REQUEST, "Site node is null");
	        }
		}

		return siteNode;
	}

    /**
     * Get site by name, workspace, and time.  This also checks that the returned node is
     * in the specified workspace, not just whether its in the workspace or any of its parents.
     *
     * @param siteName
     *            short name of site
     * @param workspace
     *            the workspace of the version of the site to return
     * @param dateTime
     *            the point in time for the version of the site to return
     * @return
     */
    protected EmsScriptNode getSiteNodeForWorkspace(String siteName, WorkspaceNode workspace,
                                                    Date dateTime) {
        return getSiteNode( siteName, workspace, dateTime, true );
    }
    protected EmsScriptNode getSiteNodeForWorkspace(String siteName, WorkspaceNode workspace,
                                        			Date dateTime, boolean errorOnNull) {

        return getSiteNodeImpl(siteName, workspace, dateTime, true, errorOnNull);
    }

    protected EmsScriptNode getSiteNodeFromRequest(WebScriptRequest req, boolean errorOnNull) {
        String siteName = null;
        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

        String[] siteKeys = {"id", "siteId", "siteName"};

        for (String siteKey: siteKeys) {
            siteName = req.getServiceMatch().getTemplateVars().get( siteKey );
            if (siteName != null) break;
        }

        return getSiteNode( siteName, workspace, dateTime, errorOnNull );
    }

	/**
	 * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
	 * This does caching of found elements so they don't need to be looked up with a different API each time.
	 * This also checks that the returned node is in the specified workspace, not just whether its in the workspace
	 * or any of its parents.
	 *
	 * @param id	Node id to search for
	 * @param workspace
     * @param dateTime
	 * @return		ScriptNode with name if found, null otherwise
	 */
	protected EmsScriptNode findScriptNodeByIdForWorkspace(String id,
	                                           				WorkspaceNode workspace,
	                                           				Date dateTime, boolean findDeleted) {
		return NodeUtil.findScriptNodeByIdForWorkspace( id, workspace, dateTime, findDeleted,
	                                        			services, response );
	}

	/**
	 * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
	 * This does caching of found elements so they don't need to be looked up with a different API each time.
	 *
	 * TODO extend so search context can be specified
	 * @param id	Node id to search for
	 * @param workspace
     * @param dateTime
	 * @return		ScriptNode with name if found, null otherwise
	 */
	public EmsScriptNode findScriptNodeById(String id,
	                                           WorkspaceNode workspace,
	                                           Date dateTime, boolean findDeleted) {
	    return findScriptNodeById( id, workspace, dateTime, findDeleted, null );
	}

	/**
     * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
     * This does caching of found elements so they don't need to be looked up with a different API each time.
     *
     * TODO extend so search context can be specified
     * @param id    Node id to search for
     * @param workspace
     * @param dateTime
     * @return      ScriptNode with name if found, null otherwise
     */
    protected EmsScriptNode findScriptNodeById(String id,
                                               WorkspaceNode workspace,
                                               Date dateTime, boolean findDeleted,
                                               String siteName) {
        return findScriptNodeById( id, workspace, dateTime, findDeleted,
                                   siteName, services, response );
    }

    /**
     * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
     * This does caching of found elements so they don't need to be looked up with a different API each time.
     *
     * TODO extend so search context can be specified
     * @param id    Node id to search for
     * @param workspace
     * @param dateTime
     * @return      ScriptNode with name if found, null otherwise
     */
    protected static EmsScriptNode
            findScriptNodeById( String id, WorkspaceNode workspace,
                                Date dateTime, boolean findDeleted,
                                String siteName, ServiceRegistry services,
                                StringBuffer response ) {
        return NodeUtil.findScriptNodeById( id, workspace, dateTime, findDeleted,
                                            services, response, siteName );
    }

	/**
     * Find nodes of specified sysml:name
     *
     */
    protected ArrayList<EmsScriptNode> findScriptNodesBySysmlName(String name,
                                                       WorkspaceNode workspace,
                                                       Date dateTime, boolean findDeleted) {
        return NodeUtil.findScriptNodesBySysmlName( name, false, workspace, dateTime, services, findDeleted, false );
    }


    // Updated log methods with log4j methods (still works with old log calls)
    // String concatenation replaced with C formatting; only for calls with parameters
    protected void log (Level level, int code, String msg, Object...params) {
    		String formattedMsg = formatMessage(msg,params);
    		log (level,code,formattedMsg);
	}

    // If no need for string formatting (calls with no string concatenation)
    protected void log(Level level, int code, String msg) {
        String levelMessage = addLevelInfoToMsg (level,msg);
        updateResponse(code, levelMessage);
		if (level.toInt() >= logger.getLevel().toInt()) {
			// print to response stream if >= existing log level
			log (level, levelMessage);
		}
	}

    // only logging loglevel and a message (no code)
	protected void log(Level level, String msg, Object...params) {
	    if (level.toInt() >= logger.getLevel().toInt()) {
        	String formattedMsg = formatMessage(msg,params); //formatter.format (msg,params).toString();
        	String levelMessage = addLevelInfoToMsg (level,formattedMsg);
        	//TODO: unsure if need to call responseStatus.setMessage(...) since there is no code
        	response.append(levelMessage);
        	log (level, levelMessage);
	    }
	}

	// only logging code and a message (no loglevel, and thus, no check for log level status)
	protected void log(int code, String msg, Object...params) {
		String formattedMsg = formatMessage(msg,params); //formatter.format (msg,params).toString();
		updateResponse (code,formattedMsg);
	}
	
	protected void log(String msg, Object...params) {
		String formattedMsg = formatMessage(msg,params); //formatter.format (msg,params).toString();
		log (formattedMsg);
	}
	
	protected void updateResponse ( int code, String msg) {
		response.append(msg);
		responseStatus.setCode(code);
		responseStatus.setMessage(msg);
	}

	protected void log(String msg) {
	    response.append(msg + "\n");
	    //TODO: add to responseStatus too (below)?
	    //responseStatus.setMessage(msg);
	}

	protected static void log(Level level, String msg) {
	    switch(level.toInt()) {
	        case Level.FATAL_INT:
	            logger.fatal(msg);
	            break;
	        case Level.ERROR_INT:
	            logger.error( msg );
	            break;
	        case Level.WARN_INT:
	            logger.warn(msg);
	            break;
	        case Level.INFO_INT:
	            logger.info( msg );
	            break;
	        case Level.DEBUG_INT:	
	            if (Debug.isOn()){ logger.debug( msg );}
	            break;
            default:
                // TODO: investigate if this the default thing to do
            	if (Debug.isOn()){ logger.debug( msg ); }
	            break;
	    }
	}
	
	protected String addLevelInfoToMsg (Level level, String msg){
		if (level.toInt() != Level.WARN_INT){
			return String.format("[%s]: %s\n",level.toString(),msg);
		}
		else{
			return String.format("[WARNING]: %s\n",msg);
		}
		
	}
	
	// formatMessage function is used to catch certain objects that must be dealt with individually
	// formatter.format() is avoided because it applies toString() directly to objects which provide unreadable outputs
	protected String formatMessage (String initMsg,Object...params){
		String formattedMsg = initMsg;
		Pattern p = Pattern.compile("(%s)");
		Matcher m = p.matcher(formattedMsg);
		
		for (Object obj: params){
			if (obj != null && obj.getClass().isArray()){
				String arrString = "";
				if (obj instanceof int []) { arrString = Arrays.toString((int [])obj);}
				else if (obj instanceof double []) { arrString = Arrays.toString((double [])obj);}
				else if (obj instanceof float []) { arrString = Arrays.toString((float [])obj);}
				else if (obj instanceof boolean []) { arrString = Arrays.toString((boolean [])obj);}
				else if (obj instanceof char []) { arrString = Arrays.toString((char [])obj);}
				else {arrString = Arrays.toString((Object[])obj);}
				formattedMsg = m.replaceFirst(arrString);
			}
			else { // captures Timer, EmsScriptNode, Date, primitive types, NodeRef, JSONObject type objects; applies toString() on all
				formattedMsg = m.replaceFirst(obj == null ? "null" : obj.toString());
			}
			m = p.matcher(formattedMsg);
//			if (obj.getClass().isArray()){	
//				Arrays.toString(obj);
//				String formattedString = m.replaceFirst(o)
//			}
			
		}
		return formattedMsg;
	}
	
	/**
	 * Checks whether user has permissions to the node and logs results and status as appropriate
	 * @param node         EmsScriptNode to check permissions on
	 * @param permissions  Permissions to check
	 * @return             true if user has specified permissions to node, false otherwise
	 */
	protected boolean checkPermissions(EmsScriptNode node, String permissions) {
	    if (node != null) {
	        Boolean b = null;
	        if ( usingPermCache ) {
	            b = permCacheGet( "u", node.getNodeRef(), permissions );
	        }
	        if ( b == null ) {
    	        b = node.checkPermissions( permissions, response, responseStatus );
                if ( usingPermCache ) {
                    permCachePut( "u", node.getNodeRef(), permissions, b );
                }
	        }
	        return b;
	    } else {
	        return false;
	    }
	}


    protected static final String WORKSPACE_ID = "workspaceId";
	protected static final String PROJECT_ID = "projectId";
	protected static final String ARTIFACT_ID = "artifactId";
    protected static final String SITE_NAME = "siteName";
    protected static final String SITE_NAME2 = "siteId";
    protected static final String WORKSPACE1 = "workspace1";
    protected static final String WORKSPACE2 = "workspace2";
    protected static final String TIMESTAMP1 = "timestamp1";
    protected static final String TIMESTAMP2 = "timestamp2";

    public static final String NO_WORKSPACE_ID = "master"; // default is master if unspecified
    public static final String NO_PROJECT_ID = "no_project";
    public static final String NO_SITE_ID = "no_site";


    public String getSiteName( WebScriptRequest req ) {
        return getSiteName( req, false );
    }
    public String getSiteName( WebScriptRequest req, boolean createIfNonexistent ) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !EmsScriptNode.ADMIN_USER_NAME.equals( runAsUser );
        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( EmsScriptNode.ADMIN_USER_NAME );
        }

        String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        if ( siteName == null ) {
            siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME2);
        }
        if ( siteName == null || siteName.length() <= 0 ) {
            siteName = NO_SITE_ID;
        }

        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( runAsUser );
        }

        if ( createIfNonexistent ) {
            WorkspaceNode workspace = getWorkspace( req );
            createSite( siteName, workspace );
        }
        return siteName;
    }

    public static EmsScriptNode getSitesFolder( WorkspaceNode workspace ) {
        EmsScriptNode sitesFolder = null;
        // check and see if the Sites folder already exists
        NodeRef sitesNodeRef = NodeUtil.findNodeRefByType( "Sites", SearchType.CM_NAME, false,
                                                           workspace, null, true, NodeUtil.getServices(), false );
        if ( sitesNodeRef != null ) {
            sitesFolder = new EmsScriptNode( sitesNodeRef, NodeUtil.getServices() );

            // If workspace of sitesNodeRef is this workspace then no need to
            // replicate, otherwise replicate from the master workspace:
            if ( NodeUtil.exists(sitesFolder) && NodeUtil.exists( workspace )
                 && !workspace.equals( sitesFolder.getWorkspace() ) ) {
                sitesFolder = workspace.replicateWithParentFolders( sitesFolder );
            }

        }
        // This case should never occur b/c the master workspace will always
        // have a Sites folder:
        else {
            Debug.error( "Can't find Sites folder in the workspace " + workspace );
        }
        return sitesFolder;
//        EmsScriptNode sf = NodeUtil.getCompanyHome( getServices() ).childByNamePath( "Sites" );
//        return sf;
    }

    public EmsScriptNode createSite( String siteName, WorkspaceNode workspace ) {

        EmsScriptNode siteNode = getSiteNode( siteName, workspace, null, false );
        if (workspace != null) return siteNode; // sites can only be made in master
        boolean validWorkspace = workspace != null && workspace.exists();
        boolean invalidSiteNode = siteNode == null || !siteNode.exists();

        // Create a alfresco Site if creating the site on the master and if the site does not exists:
        if ( invalidSiteNode && !validWorkspace ) {
            NodeUtil.transactionCheck( logger, null );
            SiteInfo foo = services.getSiteService().createSite( siteName, siteName, siteName, siteName, SiteVisibility.PUBLIC );
            siteNode = new EmsScriptNode( foo.getNodeRef(), services );
            siteNode.createOrUpdateAspect( "cm:taggable" );
            siteNode.createOrUpdateAspect(Acm.ACM_SITE);
            // this should always be in master so no need to check workspace
//            if (workspace == null) { // && !siteNode.getName().equals(NO_SITE_ID)) {
                // default creation adds GROUP_EVERYONE as SiteConsumer, so remove
                siteNode.removePermission( "SiteConsumer", "GROUP_EVERYONE" );
                if ( siteNode.getName().equals(NO_SITE_ID)) {
                    siteNode.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
                }
//            }
        }

        // If this site is supposed to go into a non-master workspace, then create the site folders
        // there if needed:
        if ( validWorkspace &&
        	( invalidSiteNode || (!invalidSiteNode && !workspace.equals(siteNode.getWorkspace())) ) ) {

	        EmsScriptNode sitesFolder = getSitesFolder(workspace);

	        // Now, create the site folder:
	        if (sitesFolder == null ) {
	            Debug.error("Could not create site " + siteName + "!");
	        } else {
	            siteNode = sitesFolder.createFolder( siteName, null, !invalidSiteNode ? siteNode.getNodeRef() : null );
	            if ( siteNode != null ) siteNode.getOrSetCachedVersion();
	        }
        }

        return siteNode;
    }

    public String getProjectId( WebScriptRequest req, String siteName ) {
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        if ( projectId == null || projectId.length() <= 0 ) {
            if (siteName == null) {
                siteName = NO_SITE_ID;
            }
            projectId = siteName + "_" + NO_PROJECT_ID;
        }
        return projectId;
    }

    public static String getWorkspaceId( WebScriptRequest req ) {
        String workspaceId = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
        if ( workspaceId == null || workspaceId.length() <= 0 ) {
            workspaceId = NO_WORKSPACE_ID;
        }
        return workspaceId;
    }

    private static String getWorkspaceNum( WebScriptRequest req, boolean isWs1 ) {
        String key = isWs1 ? WORKSPACE1 : WORKSPACE2;
        String workspaceId = req.getServiceMatch().getTemplateVars().get(key);
        if ( workspaceId == null || workspaceId.length() <= 0 ) {
            workspaceId = NO_WORKSPACE_ID;
        }
        return workspaceId;
    }

    private static String getTimestampNum( WebScriptRequest req, boolean isTs1 ) {
        String key = isTs1 ? TIMESTAMP1 : TIMESTAMP2;
        String timestamp = req.getServiceMatch().getTemplateVars().get(key);
        if ( timestamp == null || timestamp.length() <= 0 ) {
            timestamp = WorkspaceDiff.LATEST_NO_TIMESTAMP;
        }
        return timestamp;
    }

    public static String getWorkspace1( WebScriptRequest req) {
        return getWorkspaceNum(req, true);
    }

    public static String getWorkspace2( WebScriptRequest req) {
        return getWorkspaceNum(req, false);
    }

    public static String getTimestamp1( WebScriptRequest req) {
        return getTimestampNum(req, true);
    }

    public static String getTimestamp2( WebScriptRequest req) {
        return getTimestampNum(req, false);
    }

    public static String getArtifactId( WebScriptRequest req ) {
        String artifactId = req.getServiceMatch().getTemplateVars().get(ARTIFACT_ID);
        if ( artifactId == null || artifactId.length() <= 0 ) {
        	artifactId = null;
        }
        return artifactId;
    }

    public WorkspaceNode getWorkspace( WebScriptRequest req ) {
        return getWorkspace( req, //false,
                             null );
    }

    public WorkspaceNode getWorkspace( WebScriptRequest req,
//                                       boolean createIfNotFound,
                                       String userName ) {
        return getWorkspace( req, services, response, responseStatus, //createIfNotFound,
                             userName );
    }

    public static WorkspaceNode getWorkspace( WebScriptRequest req,
                                              ServiceRegistry services,
                                              StringBuffer response,
                                              Status responseStatus,
                                              //boolean createIfNotFound,
                                              String userName ) {
        String nameOrId = getWorkspaceId( req );
        return WorkspaceNode.getWorkspaceFromId( nameOrId, services, response, responseStatus,
                                   //createIfNotFound,
                                   userName );
    }

    public ServiceRegistry getServices() {
        if ( services == null ) {
            services = NodeUtil.getServices();
        }
        return services;
    }

    protected boolean checkRequestContent(WebScriptRequest req) {
        if (req.getContent() == null) {
            log(Level.ERROR,  HttpServletResponse.SC_NO_CONTENT, "No content provided.\n");
            return false;
        }
        return true;
    }

    /**
     * Returns true if the user has permission to do workspace operations, which is determined
     * by the LDAP group or if the user is admin.
     *
     */
    protected boolean userHasWorkspaceLdapPermissions() {

        if (!NodeUtil.userHasWorkspaceLdapPermissions()) {
            log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "User %s does not have LDAP permissions to perform workspace operations.  LDAP group with permissions: %s",
            		NodeUtil.getUserName(), NodeUtil.getWorkspaceLdapGroup());
            return false;
        }
        return true;
    }


	protected boolean checkRequestVariable(Object value, String type) {
		if (value == null) {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "%s not found.\n",type);
			return false;
		}
		return true;
	}

    protected Map< String, EmsScriptNode >
            searchForElements( String type, String pattern,
                                       boolean ignoreWorkspace,
                                       WorkspaceNode workspace, Date dateTime,
                                       Integer maxItems,
                                       Integer skipCount ) {
    	// TODO: can't search against snapshots - non-null date time is caught upstream
    	return searchForElementsOriginal( type, pattern, ignoreWorkspace, workspace, dateTime );
    }
    protected Map< String, EmsScriptNode >
    searchForElements( ArrayList<String> types, String pattern,
                               boolean ignoreWorkspace,
                               WorkspaceNode workspace, Date dateTime,
                               Integer maxItems,
                               Integer skipCount ) {
    	// TODO: can't search against snapshots - non-null date time is caught upstream
    	return searchForElementsPostgres( types, pattern, ignoreWorkspace, workspace, dateTime, maxItems, skipCount );	
    }

	/**
	 * Perform Lucene search for the specified pattern and ACM type
	 * TODO: Scope Lucene search by adding either parent or path context
	 * @param type		escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
	 * @param pattern   Pattern to look for
	 */
	protected Map<String, EmsScriptNode> searchForElementsOriginal(String type,
	                                                       String pattern,
	                                                       boolean ignoreWorkspace,
	                                                       WorkspaceNode workspace,
	                                                       Date dateTime) {
		return this.searchForElements( type, pattern, ignoreWorkspace,
		                               workspace, dateTime, null );
	}

	/**
	 * Perform Lucene search for the specified pattern and ACM type
	 * As opposed to searchForElementsOriginal, this returns a Map of noderef ids to
	 * script nodes - since postgres db needs this to look up properly
	 *
	 * @param type
	 * @param pattern
	 * @param ignoreWorkspace
	 * @param workspace
	 * @param dateTime
	 * @return
	 */
	protected Map<String, EmsScriptNode> searchForElementsPostgres(ArrayList<String> types,
                                                                  String pattern,
                                                                  boolean ignoreWorkspace,
                                                                  WorkspaceNode workspace,
                                                                  Date dateTime,
                                                                  Integer maxItems,
                                                                  Integer skipCount) {
		Map<String, EmsScriptNode> resultsMap = new HashMap<String, EmsScriptNode>();
	    ResultSet results = null;
	    StringBuffer queryPattern= new StringBuffer();
		for(int i = 0; i< types.size()-1;i++){
			if(types.get(i).equals("ASPECT:\"{http://jpl.nasa.gov/model/sysml-lite/1.0}")){
				continue;
			}
			else{
				queryPattern.append(types.get(i) + pattern + "\" OR ");
			}
		}
	    queryPattern.append(types.get(types.size()-1)+ pattern + "\"");
        results = NodeUtil.luceneSearch( queryPattern.toString(), services, maxItems, skipCount );
        if (results != null) {
            ArrayList< NodeRef > resultList =
                    NodeUtil.resultSetToNodeRefList( results );
            results.close();
            for (NodeRef nr: resultList) {
                EmsScriptNode node = new EmsScriptNode(nr, services, response);
                resultsMap.put( node.getNodeRef().toString(), node );
            }
        }		
        return resultsMap;
	}

	
	/**
     * Perform Lucene search for the specified pattern and ACM type for the specified
     * siteName.
     *
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type      escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    protected Map<String, EmsScriptNode> searchForElements(String type,
                                                                  String pattern,
                                                                  boolean ignoreWorkspace,
                                                                  WorkspaceNode workspace,
                                                                  Date dateTime,
                                                                  String siteName) {
        return NodeUtil.searchForElements( type, pattern, ignoreWorkspace,
                                                          workspace,
                                                          dateTime, services,
                                                          response,
                                                          responseStatus,
                                                          siteName);
    }

	/**
     * Helper utility to check the value of a request parameter
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param value
     *            String of the value the parameter is being checked for
     * @return True if parameter is equal to value, False otherwise
     */
    public static boolean checkArgEquals(WebScriptRequest req, String name,
            String value) {
        if (req.getParameter(name) == null) {
            return false;
        }
        return req.getParameter(name).equals(value);
    }

    /**
     * Helper utility to get the value of a Boolean request parameter
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param defaultValue
     *            default value if there is no parameter with the given name
     * @return true if the parameter is assigned no value, if it is assigned
     *         "true" (ignoring case), or if it's default is true and it is not
     *         assigned "false" (ignoring case).
     */
    public static boolean getBooleanArg(WebScriptRequest req, String name,
                                        boolean defaultValue) {
        if ( !Utils.toSet( req.getParameterNames() ).contains( name ) ) {
            return defaultValue;
        }
        String paramVal = req.getParameter(name);
        if ( Utils.isNullOrEmpty( paramVal ) ) return true;
        Boolean b = Utils.isTrue( paramVal, false );
        if ( b != null ) return b;
        return defaultValue;
    }


    public StringBuffer getResponse() {
        return response;
    }

    public Status getResponseStatus() {
        return responseStatus;
    }

    public void setLogLevel(Level level) {
        logLevel = level;
    }

    /**
     * Should create the new instances with the response in constructor, so
     * this can be removed every where
     * @param instance
     */
    public void appendResponseStatusInfo(AbstractJavaWebScript instance) {
        response.append(instance.getResponse());
        responseStatus.setCode(instance.getResponseStatus().getCode());
    }

    protected void printFooter() {
        log( Level.INFO, "*** completed %s %s", ( new Date() ).toString(), getClass().getSimpleName());
    }

    protected void printHeader( WebScriptRequest req ) {
        log( Level.INFO, "*** starting %s %s",( new Date() ).toString(),getClass().getSimpleName() );
        String reqStr = req.getURL();
        log( Level.INFO, "*** request = %s ...", ( reqStr.length() <= MAX_PRINT ? reqStr : reqStr.substring( 0, MAX_PRINT ) ));
    }

    protected static String getIdFromRequest( WebScriptRequest req ) {
        String[] ids = new String[] { "id", "modelid", "modelId", "productid", "productId",
        							  "viewid", "viewId", "workspaceid", "workspaceId",
                                      "elementid", "elementId" };
        String id = null;
        for ( String idv : ids ) {
            id = req.getServiceMatch().getTemplateVars().get(idv);
            if ( id != null ) break;
        }
        if (Debug.isOn()) System.out.println("Got id = " + id);
        if ( id == null ) return null;
        boolean gotElementSuffix  = ( id.toLowerCase().trim().endsWith("/elements") );
        if ( gotElementSuffix ) {
            id = id.substring( 0, id.lastIndexOf( "/elements" ) );
        } else {
            boolean gotViewSuffix  = ( id.toLowerCase().trim().endsWith("/views") );
            if ( gotViewSuffix ) {
                id = id.substring( 0, id.lastIndexOf( "/views" ) );
            }
        }
        if (Debug.isOn()) System.out.println("id = " + id);
        return id;
    }


    protected static boolean urlEndsWith( String url, String suffix ) {
        if ( url == null ) return false;
        url = url.toLowerCase().trim();
        suffix = suffix.toLowerCase().trim();
        if ( suffix.startsWith( "/" ) ) suffix = suffix.substring( 1 );
        int pos = url.lastIndexOf( '/' );
        if (url.substring( pos+1 ).startsWith( suffix ) ) return true;
        return false;
    }
    protected static boolean isDisplayedElementRequest( WebScriptRequest req ) {
        if ( req == null ) return false;
        String url = req.getURL();
        boolean gotSuffix = urlEndsWith( url, "elements" );
        return gotSuffix;
    }

    protected static boolean isContainedViewRequest( WebScriptRequest req ) {
        if ( req == null ) return false;
        String url = req.getURL();
        boolean gotSuffix = urlEndsWith( url, "views" );
        return gotSuffix;
    }


    public void setWsDiff(WorkspaceNode workspace) {
        wsDiff = new WorkspaceDiff(workspace, workspace, response, responseStatus);
    }
    public void setWsDiff(WorkspaceNode workspace1, WorkspaceNode workspace2, Date time1, Date time2, DiffType diffType) {
        wsDiff = new WorkspaceDiff(workspace1, workspace2, time1, time2, response, responseStatus, diffType);
    }

    public WorkspaceDiff getWsDiff() {
        return wsDiff;
    }




    /**
     * Determines the project site for the passed site node.  Also, determines the
     * site package node if applicable.
     *
     */
    public Pair<EmsScriptNode,EmsScriptNode> findProjectSite(String siteName,
                                                             Date dateTime,
                                                             WorkspaceNode workspace,
                                                             EmsScriptNode initialSiteNode) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !EmsScriptNode.ADMIN_USER_NAME.equals( runAsUser );
        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( EmsScriptNode.ADMIN_USER_NAME );
        }
        Pair< EmsScriptNode, EmsScriptNode > p =
                findProjectSiteImpl( siteName, dateTime, workspace, initialSiteNode );
        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( runAsUser );
        }
        return p;
    }
    public Pair<EmsScriptNode,EmsScriptNode> findProjectSiteImpl(String siteName,
                                                                 Date dateTime,
                                                                 WorkspaceNode workspace,
                                                                 EmsScriptNode initialSiteNode) {

        EmsScriptNode sitePackageNode = null;
        EmsScriptNode siteNode = null;

        // If it is a package site, get the corresponding package for the site:
        NodeRef sitePackageRef = (NodeRef) initialSiteNode.getNodeRefProperty( Acm.ACM_SITE_PACKAGE, dateTime,
                                                                               workspace);
        if (sitePackageRef != null) {
            sitePackageNode = new EmsScriptNode(sitePackageRef, services);
        }
        // Could find the package site using the property, try searching for it:
        else if (siteName != null && siteName.startsWith(NodeUtil.sitePkgPrefix)) {

            String[] splitArry = siteName.split(NodeUtil.sitePkgPrefix);
            if (splitArry != null && splitArry.length > 0) {
                String sitePkgName = splitArry[splitArry.length-1];

                sitePackageNode = findScriptNodeById(sitePkgName,workspace, dateTime, false );

                if (sitePackageNode == null) {
                    log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find site package node for site package name %s",siteName);
                    return null;
                }
            }
        }

        // Found the package for the site:
        if (sitePackageNode != null) {
            // Note: not using workspace since sites are all in master.
            NodeRef sitePackageSiteRef = (NodeRef) sitePackageNode.getPropertyAtTime( Acm.ACM_SITE_SITE, dateTime );
            if (sitePackageSiteRef != null && !sitePackageSiteRef.equals( initialSiteNode.getNodeRef() )) {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Mismatch between site/package for site package name %s",siteName);
                return null;
            }

            // Get the project site by tracing up the parents until the parent is null:
            NodeRef siteParentRef = (NodeRef) initialSiteNode.getPropertyAtTime( Acm.ACM_SITE_PARENT, dateTime );
            EmsScriptNode siteParent = siteParentRef != null ? new EmsScriptNode(siteParentRef, services, response) : null;
            EmsScriptNode oldSiteParent = null;

            while (siteParent != null) {
                oldSiteParent = siteParent;
                siteParentRef = (NodeRef) siteParent.getPropertyAtTime( Acm.ACM_SITE_PARENT, dateTime );
                siteParent = siteParentRef != null ? new EmsScriptNode(siteParentRef, services, response) : null;
            }

            if (oldSiteParent != null && oldSiteParent.exists()) {
                siteNode = oldSiteParent;
            }
            else {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find parent project site for site package name %s", siteName);
                return null;
            }

        }
        // Otherwise, assume it is a project site:
        else {
            siteNode = initialSiteNode;
        }

        return new Pair<EmsScriptNode,EmsScriptNode>(sitePackageNode, siteNode);
    }

    /**
     * Return the matching alfresco site for the Package, or null
     *
     * @param pkgNode
     * @param workspace
     * @return
     */
    public EmsScriptNode getSiteForPkgSite(EmsScriptNode pkgNode, Date dateTime, WorkspaceNode workspace) {

        // Note: skipping the noderef check b/c our node searches return the noderefs that correspond
        //       to the nodes in the surf-config folder.  Also, we dont need the check b/c site nodes
        //       are always in the master workspace.
        NodeRef pkgSiteParentRef = (NodeRef)pkgNode.getPropertyAtTime( Acm.ACM_SITE_SITE, dateTime );
        EmsScriptNode pkgSiteParentNode = null;

        if (pkgSiteParentRef != null) {
            pkgSiteParentNode = new EmsScriptNode(pkgSiteParentRef, services);
        }
        // Couldn't find it using the property, try searching for it:
        else {
            // Search for it. Will have a cm:name = "site_"+sysmlid of site package node:
            String sysmlid = pkgNode.getSysmlId();
            if (sysmlid != null) {
                pkgSiteParentNode = findScriptNodeById(NodeUtil.sitePkgPrefix+sysmlid, workspace, dateTime, false);
            }
            else {
            	//TODO NOTE: Not Sure if to invoke pkgNode.toString() or pkgNode.getName() below:
                log(Level.WARN, "Parent package site does not have a sysmlid.  Node %s",pkgNode.toString());
            }
        }

        return pkgSiteParentNode;
    }

    /**
     * Returns the parent site of node, or the project site, or null.  The parent site of
     * the node is the alfresco site for the site package.
     *
     * @param node
     * @param siteNode
     * @param projectNode
     * @param workspace
     * @return
     */
    public EmsScriptNode findParentPkgSite(EmsScriptNode node,
                                           WorkspaceNode workspace,
                                           Date dateTime) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !EmsScriptNode.ADMIN_USER_NAME.equals( runAsUser );
        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( EmsScriptNode.ADMIN_USER_NAME );
        }
        EmsScriptNode n = findParentPkgSiteImpl( node, workspace, dateTime );
        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( runAsUser );
        }
        return n;
    }
    public EmsScriptNode findParentPkgSiteImpl(EmsScriptNode node,
                                               WorkspaceNode workspace,
                                               Date dateTime) {

        EmsScriptNode pkgSiteParentNode = null;
        // Note: must walk up using the getOwningParent() b/c getParent() does not work
        //       for versioned nodes.  Note, that getOwningParent() will be null for
        //       the Project node, but we don't need to go farther up than this anyways
        EmsScriptNode siteParentReifNode = node.getOwningParent(dateTime, workspace, false, true);
        EmsScriptNode siteParent;
        while (siteParentReifNode != null && siteParentReifNode.exists()) {

            siteParent = siteParentReifNode.getReifiedPkg(dateTime, workspace);

            // If the parent is a package and a site, then its the parent site node:
            if (siteParentReifNode.hasAspect(Acm.ACM_PACKAGE) ) {
                Boolean isSiteParent = (Boolean) siteParentReifNode.getProperty( Acm.ACM_IS_SITE );
                if (isSiteParent != null && isSiteParent) {

                    // Get the alfresco Site for the site package node:
                    pkgSiteParentNode = getSiteForPkgSite(siteParentReifNode, dateTime, workspace);
                    break;
                }
            }

            // If the parent is the project, then the site will be the project Site:
            // Note: that projects are never nested so we just need to check if it is of project type
            String siteParentType = siteParent != null ? siteParent.getTypeShort() : null;
            String siteParentReifType = siteParentReifNode.getTypeShort();
            if (Acm.ACM_PROJECT.equals( siteParentType ) || Acm.ACM_PROJECT.equals( siteParentReifType )) {
                pkgSiteParentNode = siteParentReifNode.getSiteNode(dateTime, workspace);
                break;  // break no matter what b/c we have reached the project node
            }

            // siteParent could be null because the reified relationships may not have been
            // created properly (for old models)
            if (siteParent == null || siteParent.isWorkspaceTop()) {
                break;
            }

            siteParentReifNode = siteParentReifNode.getOwningParent(dateTime, workspace, false, true);
        }

        return pkgSiteParentNode;
    }


    /**
     * This needs to be called with the incoming JSON request to populate the local source
     * variable that is used in the sendDeltas call.
     * @param postJson
     * @throws JSONException
     */
    protected void populateSourceFromJson(JSONObject postJson) throws JSONException {
        if (postJson.has( "source" )) {
            source = postJson.getString( "source" );
        } else {
            source = null;
        }
    }

    /**
     * Send progress messages to the log, JMS, and email.
     *
     * @param msg  The message
     * @param projectSysmlId  The project sysml id
     * @param workspaceName  The workspace name
     * @param sendEmail Set to true to send a email also
     */
    public void sendProgress( String msg, String projectSysmlId, String workspaceName,
                              boolean sendEmail) {

        String projectId = Utils.isNullOrEmpty(projectSysmlId) ? "unknown_project" : projectSysmlId;
        String workspaceId = Utils.isNullOrEmpty(workspaceName) ? "unknown_workspace" : workspaceName;
        String subject = "Progress for project: "+projectId+" workspace: "+workspaceId;

        // Log the progress:
        log(Level.INFO,"%s msg: %s\n",subject,msg);
        //logger.info(subject+" msg: "+msg+"\n");

        // Send the progress over JMS:
        CommitUtil.sendProgress(msg, workspaceId, projectId);

        // Email the progress (this takes a long time, so only do it for critical events):
        if (sendEmail) {
            String hostname = NodeUtil.getHostname();
            if (!Utils.isNullOrEmpty( hostname )) {
                String sender = hostname + "@" + EmsConfig.get( "app.domain.name" );
                String username = NodeUtil.getUserName();
                if (!Utils.isNullOrEmpty( username )) {
                    EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username),
                                                           services);
                    if (user != null) {
                        String recipient = (String) user.getProperty("cm:email");
                        if (!Utils.isNullOrEmpty( recipient )) {
                            ActionUtil.sendEmailTo( sender, recipient, msg, subject, services );
                        }
                    }
                }
            }
        }

    }

    /**
     * Creates a json like object in a string and puts the response in the message key
     *
     * @return The resulting string, ie "{'message':response}" or "{}"
     */
    public String createResponseJson() {
        String resToString = response.toString();
        String resStr = !Utils.isNullOrEmpty( resToString ) ? resToString.replaceAll( "\n", "" ) : "";
        return !Utils.isNullOrEmpty( resStr ) ? String.format("{\"message\":\"%s\"}", resStr) : "{}";
    }

    public EmsSystemModel getEmsSystemModel() {
        if ( systemModel == null ) {
            systemModel = new EmsSystemModel(this.services);
        }
        return systemModel;
    }

    public SystemModel< EmsScriptNode, Object, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode > getSystemModel() {
        if ( systemModel == null ) {
            systemModel = new EmsSystemModel(this.services);
        }
        return systemModel;
    }


    public static EmsSystemModel globalSystemModel = null;

    public static EmsSystemModel getGlobalSystemModel() {
        if ( globalSystemModel == null ) {
            globalSystemModel = new EmsSystemModel(NodeUtil.getServices() );
        }
        return globalSystemModel;
    }

    public SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > getSystemModelAe() {
        if ( sysmlToAe == null ) {
            setSystemModelAe();
        }
        return sysmlToAe;
    }

    public static SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > getGlobalSystemModelAe() {
        if ( globalSysmlToAe == null ) {
            setGlobalSystemModelAe();
        }
        return globalSysmlToAe;
    }

    public void setSystemModelAe() {
        sysmlToAe =
                new SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getEmsSystemModel() );

    }

    public static void setGlobalSystemModelAe() {
        globalSysmlToAe =
                new SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getGlobalSystemModel() );
    }

    /**
     * Creates a ConstraintExpression for the passed constraint node and adds to the passed constraints
     *
     * @param constraintNode The node to parse and create a ConstraintExpression for
     * @param constraints The list of Constraints to add for tje node
     */
    public static void addConstraintExpression(EmsScriptNode constraintNode, Map< EmsScriptNode, Collection< Constraint >> constraints, WorkspaceNode ws) {

        if (constraintNode == null || constraints == null) return;

        EmsScriptNode exprNode = getConstraintExpression(constraintNode, ws);

        if (exprNode != null) {
            Expression<Boolean> expression = toAeExpression( exprNode );

            if (expression != null) {

                Collection< Constraint > constrs = constraints.get( constraintNode );
                if ( constrs == null ) {
                    constrs = new ArrayList< Constraint >();
                    constraints.put( constraintNode, constrs );
                }
                constrs.add(new ConstraintExpression( expression ));
            }
        }
    }

    public static <T> Expression<T> toAeExpression( EmsScriptNode exprNode ) {
        return toAeExpression( exprNode, true );
    }
    public static <T> Expression<T> toAeExpression( EmsScriptNode exprNode, boolean evaluate ) {
        if ( exprNode == null ) {
            logger.warn( "called toAeExpression() with null argument" );
            return null;
        }
        Expression<Call> expressionCall = getGlobalSystemModelAe().toAeExpression( exprNode, null );
        if ( expressionCall == null ) {
            logger.warn( "toAeExpression("+exprNode+") returned null" );
            return null;
        }
        Call call = (Call) expressionCall.expression;
        if ( call == null ) {
            logger.warn( "toAeExpression("+exprNode+"): call is null, " + expressionCall );
            return null;
        }
        Expression< T > expression = null;
        if ( !evaluate ) {
            expression = new Expression<T>(call);
        } else {
        try {
            expression = new Expression<T>(call.evaluate(true, false));

        // TODO -- figure out why eclipse gives compile errors for
        // including the exceptions while mvn gives errors for not
        // including them.
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch ( InstantiationException e ) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        }
        return expression;
    }

    // TODO -- look at getAffectedIds()
    public static Map< EmsScriptNode, Collection< Constraint > > getAeConstraints( Set< EmsScriptNode > elements, WorkspaceNode ws ) {
        //Map<EmsScriptNode, Constraint> constraints = new LinkedHashMap<EmsScriptNode, Constraint>();
        Map< EmsScriptNode, Collection< Constraint > >  constraints =
                new LinkedHashMap< EmsScriptNode, Collection< Constraint > >();

        // Search for all constraints in the database:
        Collection<EmsScriptNode> constraintNodes = getGlobalSystemModel().getType(null, Acm.ACM_CONSTRAINT);
//log(Level.INFO, "all constraints in database: " + constraintNodes);

        if (!Utils.isNullOrEmpty(constraintNodes)) {

            // Loop through each found constraint and check if it contains any of the elements
            // to be posted:
            for (EmsScriptNode constraintNode : constraintNodes) {

                // Parse the constraint node for all of the cm:names of the nodes in its expression tree:
                Set<String> constrElemNames = getConstraintElementNames(constraintNode, ws);

                // Check if any of the posted elements are in the constraint expression tree, and add
                // constraint if they are:
                // Note: if a Constraint element is in elements then it will also get added here b/c it
                //          will be in the database already via createOrUpdateMode()
                for (EmsScriptNode element : elements) {

                    String name = element.getName();
//log(Level.INFO, "element (" + element + ") vs. constraint (" + constraintNode + ")");
                    if ( element.equals( constraintNode ) || ( name != null && constrElemNames.contains(name) ) ) {
                        addConstraintExpression(constraintNode, constraints, ws);
                        break;
                    }

                } // Ends loop through elements

            } // Ends loop through constraintNodes

        } // Ends if there was constraint nodes found in the database

        // TODO -- This is temporarily commented out until code is added to tie
        // the constraints back to the nodes such as a constraint that a
        // property be grounded.
//        // Add all of the Parameter constraints:
//        ClassData cd = getGlobalSystemModelAe().getClassData();
//        // Loop through all the listeners:
//        for (ParameterListenerImpl listener : cd.getAeClasses().values()) {
//            // TODO: REVIEW
//            //       Can we get duplicate ParameterListeners in the aeClassses map?
//            constraints.addAll( listener.getConstraints( true, null ) );
//        }

        return constraints;
    }

    public static Map< EmsScriptNode, Expression<?> > getAeExpressions( Collection< EmsScriptNode > elements ) {
        Map<EmsScriptNode, Expression<?>> expressions = new LinkedHashMap< EmsScriptNode, Expression<?> >();
        for ( EmsScriptNode node : elements ) {
            // FIXME -- Don't we need to pass in a date and workspace?
            if ( node.hasAspect( Acm.ACM_EXPRESSION ) ) {
                Expression<?> expression = toAeExpression( node );
                if ( expression != null ) {
                    expressions.put( node, expression );
                }
            } else if ( node.hasValueSpecProperty( null, node.getWorkspace() ) ) {
                ArrayList< NodeRef > values = node.getValueSpecOwnedChildren( false, null, node.getWorkspace() );
                List< EmsScriptNode > valueElements = new ArrayList< EmsScriptNode >();
                for ( NodeRef value : values ) {
                    EmsScriptNode n = new EmsScriptNode( value, NodeUtil.getServices() );
                    if ( n.hasOrInheritsAspect( Acm.ACM_EXPRESSION ) ) {
                        valueElements.add( n );
                    }
                }
//                List< EmsScriptNode > valueElements =
//                        EmsScriptNode.toEmsScriptNodeList( values,
//                                                           NodeUtil.getServices(),
//                                                           null, null );
                Map< EmsScriptNode, Expression< ? > > ownedExpressions =
                       getAeExpressions( valueElements );
                if ( ownedExpressions != null && ownedExpressions.size() == 1 ) {
                    expressions.put( node, ownedExpressions.entrySet().iterator().next().getValue() );
                } else if ( ownedExpressions != null ) {
                    // TODO -- REVIEW -- is wrapping a collection of Expressions
                    // in an Expression goign to evaluate properly?
                    expressions.put( node, new Expression( ownedExpressions ) );
//                    for ( Expression< ? > ownedExpr : ownedExpressions.values() ) {
//                    }
                }
            }
        }
        return expressions;
    }

    public static Map<Object, Object> evaluate( Set< EmsScriptNode > elements, WorkspaceNode ws ) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        log(Level.INFO, "Will attempt to evaluate expressions where found!");
        Map< Object, Object > results = new LinkedHashMap< Object, Object >();

        Map< EmsScriptNode, Expression<?> > expressions = getAeExpressions( elements );
//log(Level.INFO, "expressions: " + expressions);
        if ( elements.size() != expressions.size() &&
                ( elements.size() != 1 || !elements.iterator().next().hasAspect( Acm.ACM_EXPRESSION ) ) ) {
        Map< EmsScriptNode, Collection< Constraint > > constraints = getAeConstraints( elements, ws );
//log(Level.INFO, "constraints: " + constraints);

        if ( !Utils.isNullOrEmpty( constraints ) ) {
            for ( Entry< EmsScriptNode, Collection< Constraint > > e : constraints.entrySet() ) {
                EmsScriptNode constraintNode = null;
                if ( e.getKey().hasOrInheritsAspect( Acm.ACM_CONSTRAINT ) ) {
                    constraintNode = e.getKey();
                }
                Collection< Constraint > constraintCollection = e.getValue();
                for ( Constraint c : constraintCollection ) {
                    if ( c != null ) {
                        if ( constraintNode != null && constraintCollection.size() == 1 ) {
                            results.put( constraintNode, c.isSatisfied( true, null ) );
                        } else {
                            results.put( c, c.isSatisfied( true, null ) );
                        }
                    }
                }
            }
        }
        }
        if ( !Utils.isNullOrEmpty( expressions ) ) {
            for ( Entry< EmsScriptNode, Expression<?> > e : expressions.entrySet() ) {
                if ( e != null && e.getKey() != null && e.getValue() != null ) {
                    Object resultVal = e.getValue().evaluate( true );
                    results.put( e.getKey(), resultVal );
                }
            }
        }
        return results;
    }


    /**
     * Construct a JSONArray from a Collection without triggering an infinite loop.
     *
     * @param collection
     *            A Collection.
     */
    public static JSONArray makeJSONArray(Collection collection) {
        JSONArray a = new JSONArray();
        if (collection != null) {
            Iterator iter = collection.iterator();
            while (iter.hasNext()) {
                a.put(jsonWrap(iter.next()));
            }
        }
        return a;
    }

    /**
     * Construct a JSONArray from an array
     *
     * @throws JSONException
     *             If not an array.
     */
    public static JSONArray makeJSONArray(Object array) throws JSONException {
        JSONArray a = new JSONArray();
        if (array.getClass().isArray()) {
            int length = Array.getLength(array);
            for (int i = 0; i < length; i += 1) {
                a.put(jsonWrap(Array.get(array, i)));
            }
        } else {
            throw new JSONException(
                    "JSONArray initial value should be a string or collection or array.");
        }
        return a;
    }

    /**
     * Fixes bug in JSONObject.wrap() where putting an unexpected object in a
     * JSONObject results in an infinite loop. The fix is to convert the object
     * to a string if it's type is not one of the usual suspects.
     *
     * @param object
     * @return an Object that can be inserted as a value into a JSONObject
     */
    public static Object jsonWrap( Object object ) {
        try {
            if (object == null) {
                return JSONObject.NULL;
            }
            if (object instanceof JSONObject || object instanceof JSONArray
                    || JSONObject.NULL.equals(object) || object instanceof JSONString
                    || object instanceof Byte || object instanceof Character
                    || object instanceof Short || object instanceof Integer
                    || object instanceof Long || object instanceof Boolean
                    || object instanceof Float || object instanceof Double
                    || object instanceof String) {
                return object;
            }

            if (object instanceof Collection) {
                return makeJSONArray((Collection) object);
            }
            if (object.getClass().isArray()) {
                return makeJSONArray(object);
            }
            if (object instanceof Map) {
                return makeJSONObject((Map) object);
            }
            return "" + object;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Construct a JSONObject from a Map without triggering an infinite loop.
     *
     * @param map
     *            A map object that can be used to initialize the contents of
     *            the JSONObject.
     * @throws JSONException
     */
    public static JSONObject makeJSONObject(Map map) {
        JSONObject jo = new JSONObject();
        if (map != null) {
            Iterator i = map.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                Object value = e.getValue();
                if (value != null) {
                    jo.put("" + e.getKey(), jsonWrap(value));
                }
            }
        }
        return jo;
    }

    public void evaluate( final Map<EmsScriptNode, JSONObject> elementsJsonMap,//Set< EmsScriptNode > elements, final JSONArray elementsJson,
                          JSONObject top, WorkspaceNode ws ) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        //final JSONArray elementsJson = new JSONArray();
        Set< EmsScriptNode > elements = elementsJsonMap.keySet();
        Map< Object, Object > results = null;
        try {
            results = evaluate( elements, ws );
        } catch ( Throwable t ) {
            log( Level.WARN, "Evaluation failed for %s: %s", elements, t.getLocalizedMessage() );
            // TODO -- wrap this stack trace as debug output
            t.printStackTrace();
        }
        if ( results == null ) return;
        for ( EmsScriptNode element : elements ) {
            JSONObject json = elementsJsonMap.get( element );//element.toJSONObject(ws, null);
            Object result = results.get( element );
            if ( result != null ) {
                try {
                    json.putOpt( "evaluationResult", jsonWrap( result ) );
                    results.remove( element );
                } catch ( Throwable e ) {
                    log( Level.WARN, "Evaluation failed for %s", element );
                }
            }
            //elementsJson.put( json );
        }
        // Put constraint evaluation results in json.
        JSONArray resultJarr = new JSONArray();
        for ( Object k : results.keySet() ) {
            JSONObject r = new JSONObject();
            r.put( "expression", k.toString() );
            Object v = results.get( k );
            r.put( "value", "" + v );
            resultJarr.put( r );
        }

        //top.put( "elements", elementsJson );
        if ( resultJarr.length() > 0 ) top.put( "evaluations", resultJarr );

    }

    public void fixWithTransactions( final Set< EmsScriptNode > elements, final WorkspaceNode ws ) {
        new EmsTransaction(getServices(), getResponse(),
                           getResponseStatus()) {
            @Override
            public void run() throws Exception {
                fix( elements, ws );
            }
        };
    }

//    public void lock( Variable<?> v ) {
//
//    }
    public static void lock( Variable<?> v ) {
        Object val = v.getValue( true );
        if ( val != null ) {
            v.setDomain( new SingleValueDomain( val ) );
        }
    }

    protected void lockOtherParameters( Set< EmsScriptNode > elements, ArrayList< Constraint > constraints ) {
        lockOtherParameters( elements, constraints, getSystemModelAe() );
    }
    protected static void lockOtherParameters( Set< EmsScriptNode > elements, ArrayList< Constraint > constraints, SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > systemModelToAe ) {
//        // get all Variables from Constraints
//        Set<Variable<?>> vars = new LinkedHashSet<Variable<?>>();
//        for ( Constraint c : constraints ) {
//            vars.addAll( c.getVariables() );
//        }
//
//        // get all cm:names of nodes that we don't want to lock
//        Set<String> unlockedCmNames = new HashSet<String>();
//        for ( EmsScriptNode element : elements ) {
//           unlockedCmNames.add(element.getName() );
//        }

        // lock all Variables that are not in unlockedCmNames by getting all variables from the paramMap in classData that are keyed by cm:name.
        Map< EmsScriptNode, Parameter< Object > > paramMap = systemModelToAe.getExprParamMap();
        for ( Entry< EmsScriptNode, Parameter< Object > > e : paramMap.entrySet() ) {
            if ( !elements.contains( e.getKey() ) ) {
                lock( e.getValue() );
            }
        }
//        for ( element : Element)
//
//        for  ( Entry< String, Map< String, Param > > e : getSystemModelAe().getClassData().getParamTable().entrySet() )
//        {
//            String cmName = e.getKey();
//            if ( !unlockedCmNames.contains( cmName ) ) {
//                Map< String, Param > v = e.getValue();
//                for ( Param p : v.values() ) {
//                    getSystemModelAe().getExprParamMap()
//                }
//            }
//        }
    }


    public void fix( final Set< EmsScriptNode > elements, final WorkspaceNode ws ) {
        fix(elements, ws, getServices(), getResponse(), getResponseStatus(), getEmsSystemModel(), getSystemModelAe() );
    }

    public static void fixBetter( final Set< EmsScriptNode > elements, final WorkspaceNode ws, ServiceRegistry services, StringBuffer response, Status status, final EmsSystemModel systemModel, SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > systemModelToAe ) {
        Map< String, ParameterListenerImpl > classes = getGlobalSystemModelAe().getClassData().getAeClasses();
        for ( ParameterListenerImpl aeClass : classes.values() ) {
            aeClass.satisfy( true, null );
        }
    }

    public static void fix( final Set< EmsScriptNode > elements, final WorkspaceNode ws, ServiceRegistry services, StringBuffer response, Status status, final EmsSystemModel systemModel, SystemModelToAeExpression< Object, EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > systemModelToAe ) {

        log(Level.INFO, "Will attempt to fix constraint violations if found!");

        SystemModelSolver// < EmsScriptNode, Object, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode >
            solver = new SystemModelSolver( //< EmsScriptNode, Object, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode >(systemModel,
                                            new ConstraintLoopSolver() );

        Map< EmsScriptNode, Collection< Constraint > > constraintMap = getAeConstraints( elements, ws );
        ArrayList< Constraint > constraints = new ArrayList< Constraint >();

//        Set< EmsScriptNode > v
//        for ( Entry< EmsScriptNode, Collection< Constraint >> e : constraintMap.entrySet() ) {
//
//            EmsScriptNode constraintNode = e.getKey();
//            Set<String> constrElemNames = getConstraintElementNames(constraintNode , ws);


        for ( Collection< Constraint > coll : constraintMap.values() ) {
            constraints.addAll( coll );
        }
        // Ensure that the class with all of the Parameters can access the
        // constraints in order to do LazyUpdating and caching of Call
        // evaluations.
        ParameterListenerImpl theClass = getGlobalSystemModelAe().getClassData().getCurrentAeClass();
        theClass.getConstraintExpressions().addAll( Utils.asList( constraints,
                                                                  ConstraintExpression.class ) );
//        }

        // Solve the constraints:
        if (Utils.isNullOrEmpty( constraints )) {
            logger.warn( "fix"
                         + EmsScriptNode.getSysmlIds( Utils.asList( elements, EmsScriptNode.class ) )
                         + ": No constraints to solve!" );
        } else {
            lockOtherParameters( elements, constraints, systemModelToAe );

            //Collection<Constraint> constraints = getConstraints( true, null );
            if ( writeConstraintsOut  ) {
              System.out.println("All " + constraints.size() + " constraints: ");
              for (Constraint c : constraints) {
                System.out.println("Constraint: " + c);
              }
            }

            Random.reset();

            // Solve!!!!
            boolean result = false;
            try {
                //ebug.turnOn();
//                for ( Constraint c : constraints ) {
//                    Set< Variable< ? >> vars = c.getFreeVariables();
//                    for ( Variable<?> v : vars ) {
//                        this.
//                        getSystemModelAe().getClassData().getParamTable();
//                        lock( v );
//                    }
//                }
                result = solver.solve(constraints);

            } finally {
                //Debug.turnOff();
            }
            if (!result) {
                logger.warn( "Was not able to satisfy all of the constraints!" );
                Collection< Constraint > unsatisfiedConstraints =
                        ConstraintLoopSolver.getUnsatisfiedConstraints( constraints );
                    if ( unsatisfiedConstraints.isEmpty() ) {
                        logger.warn( (constraints.size() - unsatisfiedConstraints.size())
                                          + " out of " + constraints.size()
                                          + " constraints were satisfied!" );
                    } else {
                        logger.warn( "Could not resolve the following "
                                          + unsatisfiedConstraints.size()
                                          + " constraints:" );
                      for ( Constraint c : unsatisfiedConstraints ) {
                          logger.warn( c.toString() );
                      }
                    }
            }
            else {
                logger.warn( "Satisfied all of the constraints!" );
            }

                // Update the values of the nodes after solving the constraints:
                final Map< EmsScriptNode, Parameter< Object > > params = getGlobalSystemModelAe().getExprParamMap();
                if ( Utils.isNullOrEmpty( params ) ) {
                    logger.error( "Solver had no parameters in map to assign the solution!" );
                } else {
//                    new EmsTransaction(getServices(), getResponse(),
//                                       getResponseStatus()) {
                    new EmsTransaction(services, response, status) {
                        @Override
                        public void run() throws Exception {
                            Set<Entry<EmsScriptNode, Parameter<Object>>> entrySet = params.entrySet();
                            for (Entry<EmsScriptNode, Parameter<Object>> entry : entrySet) {
                                EmsScriptNode node;
                                Parameter<Object> param;
                                node = entry.getKey();
                                // screen out elements that weren't part of the post
                                if ( node == null || !elements.contains( node ) ) {
                                    continue;
                                }
                                param = entry.getValue();
                                Serializable newVal = (Serializable)param.getValue();
                                if ( node.equals( newVal ) ) {
                                    // This may just be a parameter referencing
                                    // the element instead of its value. So,
                                    // (TODO) this will not work if the value is
                                    // an ElementValue of itself.
                                } else {
//                                Object v = systemModel.getValue( node, null );
                                    //if (Debug.isOn())
                                    if ( logger.isEnabledFor( Level.WARN ) ) {
                                        logger.warn( "setting node id="
                                                     + node.getSysmlId()
                                                     + " name="
                                                     + node.getSysmlName()
                                                     + " to value of Parameter: "
                                                     + param );
                                    }
                                    //                              System.out.println("AAAAAAAAAAA  NODEREF = " + node.getId());
//                                System.out.println("XXXXXXXXXXXXXXX  NODEREF = " + node.getId());
//                                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX value before setting = " + v);
                                    systemModel.setValue(node, newVal, ws);
//                                v = systemModel.getValue( node, null );
//                                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX value after setting = " + v);
                                }
                            }
                            logger.info( "Updated all node values to satisfy the constraints!" );
                        }
                    };
                }
            //}
        } // End if constraints list is non-empty

    }

    /**
     * Parses the Property and returns a set of all the node names
     * in the property.
     *
     * @param propertyNode The node to parse
     * @return Set of cm:name
     */
    protected static Set<String> getPropertyElementNames(EmsScriptNode propertyNode) {

        Set<String> names = new HashSet<String>();

        if (propertyNode != null) {

            String name = propertyNode.getName();

            if (name != null) names.add(name);

            // See if it has a value property:
            Collection< EmsScriptNode > propertyValues =
                    getGlobalSystemModel().getProperty(propertyNode, Acm.JSON_VALUE);

            if (!Utils.isNullOrEmpty(propertyValues)) {
                  for (EmsScriptNode value : propertyValues) {

                      names.add(value.getName());

                      // TODO REVIEW
                      //      need to be able to handle all ValueSpecification types?
                      //      some of them have properties that point to nodes, so
                      //      would need to process them also
                  }
            }
        }

        return names;
    }

    /**
     * Parses the Parameter and returns a set of all the node names
     * in the parameter.
     *
     * @param paramNode The node to parse
     * @return Set of cm:name
     */
    protected static Set<String> getParameterElementNames(EmsScriptNode paramNode) {

        Set<String> names = new HashSet<String>();

        if (paramNode != null) {

            String name = paramNode.getName();

            if (name != null) names.add(name);

            // See if it has a defaultParamaterValue property:
            Collection< EmsScriptNode > paramValues =
                    getGlobalSystemModel().getProperty(paramNode, Acm.JSON_PARAMETER_DEFAULT_VALUE);

            if (!Utils.isNullOrEmpty(paramValues)) {
                  names.add(paramValues.iterator().next().getName());
            }
        }

        return names;
    }

    /**
     * Parses the Operation and returns a set of all the node names
     * in the operation.
     *
     * @param opNode The node to parse
     * @return Set of cm:name
     */
    protected static Set<String> getOperationElementNames(EmsScriptNode opNode) {

        Set<String> names = new HashSet<String>();

        if (opNode != null) {

            String name = opNode.getName();

            if (name != null) names.add(name);

            // See if it has a operationParameter and/or operationExpression property:
            Collection< EmsScriptNode > opParamNodes =
                    getGlobalSystemModel().getProperty(opNode, Acm.JSON_OPERATION_PARAMETER);

            if (!Utils.isNullOrEmpty(opParamNodes)) {
              for (EmsScriptNode opParamNode : opParamNodes) {
                  names.addAll(getParameterElementNames(opParamNode));
              }
            }

            Collection< EmsScriptNode > opExprNodes =
                    getGlobalSystemModel().getProperty(opNode, Acm.JSON_OPERATION_EXPRESSION);

            if (!Utils.isNullOrEmpty(opExprNodes)) {
                names.add(opExprNodes.iterator().next().getName());
            }
        }

        return names;
    }

    /**
     * Parses the expression and returns a set of all the node names
     * in the expression.
     *
     * @param expressionNode The node to parse
     * @param ws
     * @param date
     * @return Set of cm:name
     */
    protected static Set<String> getExpressionElementNames(EmsScriptNode expressionNode, Date date , WorkspaceNode ws ) {

        Set<String> names = new HashSet<String>();

        if (expressionNode != null) {

            // Add the name of the Expression itself:
            String name = expressionNode.getName();

            if (name != null) names.add(name);

            // FIXME -- need to give date/workspace context
            // Process all of the operand properties:
            Collection< EmsScriptNode > properties =
                    getGlobalSystemModel().getProperty( expressionNode, Acm.JSON_OPERAND);

            if (!Utils.isNullOrEmpty(properties)) {

              EmsScriptNode valueOfElementNode = null;

              for (EmsScriptNode operandProp : properties) {

                if (operandProp != null) {

                    names.add(operandProp.getName());

                    // FIXME -- need to give date/workspace context
                    // Get the valueOfElementProperty node:
                    Collection< EmsScriptNode > valueOfElemNodes =
                            getGlobalSystemModel().getProperty(operandProp, Acm.JSON_ELEMENT_VALUE_ELEMENT);

                    // If it is a elementValue, then this will be non-empty:
                    if (!Utils.isNullOrEmpty(valueOfElemNodes)) {

                      // valueOfElemNodes should always be size 1 b/c elementValueOfElement
                      // is a single NodeRef
                      valueOfElementNode = valueOfElemNodes.iterator().next();
                    }

                    // Otherwise just use the node itself as we are not dealing with
                    // elementValue types:
                    else {
                      valueOfElementNode = operandProp;
                    }

                    if (valueOfElementNode != null) {

                      // FIXME -- need to give date/workspace context
                      String typeString = getGlobalSystemModel().getTypeString(valueOfElementNode, null);

                      // FIXME -- need to give date/workspace context
                      // If it is a Operation then see if it then process it:
                      if (typeString.equals(Acm.JSON_OPERATION)) {
                          names.addAll(getOperationElementNames(valueOfElementNode));
                      }

                      // If it is a Expression then process it recursively:
                      else if (typeString.equals(Acm.JSON_EXPRESSION)) {
                          names.addAll(getExpressionElementNames(valueOfElementNode, date, ws));
                      }

                      // FIXME -- need to give date/workspace context
                      // If it is a Parameter then process it:
                      else if (typeString.equals(Acm.JSON_PARAMETER)) {
                          names.addAll(getParameterElementNames(valueOfElementNode));
                      }

                      // FIXME -- need to give date/workspace context
                      // If it is a Property then process it:
                      else if (typeString.equals(Acm.JSON_PROPERTY)) {
                          names.addAll(getPropertyElementNames(valueOfElementNode));
                      }

                    } // ends if valueOfElementNode != null

                } // ends if operandProp != null

              } // ends for loop through operand properties

            } // ends if operand properties not null or empty

        } // ends if expressionNode != null

        return names;
    }

    /**
     * Parses the expression for the passed constraint, and returns a set of all the node
     * names in the expression.
     *
     * @param constraintNode The node to parse
     * @param ws
     * @return Set of cm:name
     */
    protected static Set<String> getConstraintElementNames(EmsScriptNode constraintNode, WorkspaceNode ws ) {

        Set<String> names = new LinkedHashSet<String>();

        if (constraintNode != null) {

            // Add the name of the Constraint:
            String name = constraintNode.getName();

            if (name != null) names.add(name);

            // Get the Expression for the Constraint:
            EmsScriptNode exprNode = getConstraintExpression(constraintNode, ws);

            // Add the names of all nodes in the Expression:
            if (exprNode != null) {

                // Get elements names from the Expression:
                names.addAll(getExpressionElementNames(exprNode, null, ws));

                // REVIEW: Not using the child associations b/c
                // ElementValue's elementValueOfElement has a different
                // owner, and wont work for our demo either b/c
                // not everything is under one parent
            }

        }

        return names;
    }

    /**
     * Parse out the expression from the passed constraint node
     *
     * @param constraintNode The node to parse
     * @return The Expression node for the constraint
     */
    private static EmsScriptNode getConstraintExpression(EmsScriptNode constraintNode, WorkspaceNode ws) {

        if (constraintNode == null) return null;

        // FIXME -- need to give date/workspace context
        // Get the constraint expression:
        Collection<EmsScriptNode> expressions =
                getGlobalSystemModel().getProperty( constraintNode, Acm.JSON_CONSTRAINT_SPECIFICATION );

        // This should always be of size 1:
        return Utils.isNullOrEmpty( expressions ) ? null :  expressions.iterator().next();

    }

    /**
     * compareMmsVersions
     * <br>
     * <h3>Note: Returns true if this compare fails for either incorrect versions or if there is an error with the request.<br/>
     * Returns false if the check is successful and the versions match.</h3>
     * <pre>
     * Takes a request created when a service is called and will retrieve the mmsVersion that is sent with it.
     *  <b>The flag checkMmsVersions needs to be set to true for this service to work.</b>
     *  <br/><b>1. </b>Check if there the request comes with the parameter mmsVersion=2.#. If the global flag
     *  is set to check for mmsVersion it will then return either none if either invalid input or if none has been
     *  specified, or the value of the version the service is being called with.
     *  <br/><b>2. </b>If the value that is received after checking for mmsVersion in the request, is 'none' then
     *  it will call parseContent of the request to create a JSONObject. If that fails, an exception is thrown
     *  and the boolean value 'true' is returned to the calling method to signify failure of the check. Else it
     *  will try to grab the mmsVersion from where ever it may lie within the JSONObject.
     *  <br/><b>3. </b>
     * </pre>
     * @param req WebScriptRequest
     * @param response StringBuffer response
     * @param status Status of the request
     * @author EDK
     * @return boolean false if versions match, true if they do not match or if is an incorrect request.
     */
    public boolean compareMmsVersions(WebScriptRequest req,
            StringBuffer response, Status status) {
        // Calls getBooleanArg to check if they have request for mms version
        // TODO: Possibly remove this and implement as an aspect?
        boolean incorrectVersion = true;
        JSONObject jsonRequest = null;
        char logCase = '0';
        JSONObject jsonVersion = null;
        String mmsVersion = null;

        // Checks if the argument is mmsVersion and returns the value specified
        // by the request
        // if there is no request it will return 'none'
        String paramVal = getStringArg(req, "mmsVersion", "none");
        String paramArg = paramVal;
        // Checks data member requestJSON to see if it is not null and if
        // paramVal is none

//     // Check if input is K or JSON
        String contentType = req.getContentType() == null ? ""
                : req.getContentType().toLowerCase();

        boolean jsonNotK = !contentType.contains("application/k");


        if (!jsonNotK && paramVal.equals("none")) {
                jsonRequest = getRequestJSON(req);

            if (jsonRequest != null) {
                paramVal = jsonRequest.optString("mmsVersion");
            }
        }

        if (paramVal != null && !paramVal.equals("none") && paramVal.length() > 0) {
            // Calls NodeUtil's getMMSversion
            jsonVersion = getMMSversion();
            mmsVersion = jsonVersion.get("mmsVersion").toString();
            
            log(Level.INFO, HttpServletResponse.SC_OK, "Comparing Versions....");
            // version match only needs to be on the first two digits
            if (mmsVersion.startsWith(paramVal)) {
                // Compared versions matches
                logCase = '1';
                incorrectVersion = false;
            } else {
                // Versions do not match
                logCase = '2';
            }
        } else if (Utils.isNullOrEmpty(paramVal) || paramVal.equals("none")) {
            // Missing MMS Version parameter
            logCase = '3';
        } else {
            // Wrong MMS Version or Invalid input
            logCase = '4';
        }
        switch (logCase) {
            case '1' :
                log(Level.INFO, HttpServletResponse.SC_OK, "Correct Versions");
                break;
            case '2' :
                log(Level.WARN, HttpServletResponse.SC_NOT_ACCEPTABLE,
                        "Versions do not match! Expected Version " + mmsVersion
                                + ". Instead received " + paramVal);
                break;
            case '3' :
                log(Level.ERROR, HttpServletResponse.SC_NOT_ACCEPTABLE,
                        "Missing MMS Version or invalid parameter. Received parameter:" + paramArg + " and argument:" + mmsVersion + ". Request was: " + jsonRequest);
                break;
            // TODO: This should be removed but for the moment I am leaving this
            // in as a contingency if anything else may break this.
            case '4' :
                log(Level.ERROR, HttpServletResponse.SC_NOT_ACCEPTABLE,
                        "Wrong MMS Version or invalid input. Expected mmsVersion="
                                + mmsVersion + ". Instead received "
                                + paramVal);
                break;
        }
        // Returns true if it is either the wrong version or if it failed to
        // compare it
        // Returns false if it was successful in retrieving the mmsVersions from
        // both the MMS and the request and
        return incorrectVersion;
    }

    /**
     * getMMSversion<br>
     * Returns a JSONObject representing the mms version being used. It's format
     * will be
     *
     * <pre>
     *  {
     *     "mmsVersion":"2.2"
     * }
     * </pre>
     *
     * @return JSONObject mmsVersion
     */
    public static JSONObject getMMSversion() {
        JSONObject version = new JSONObject();
        version.put("mmsVersion", NodeUtil.getMMSversion());
        return version;
    }
    /**
     * getMMSversion <br/>
     * getMMSversion wraps
     *
     * @param req
     * @return
     */
    public static JSONObject getMMSversion(WebScriptRequest req) {
        // Calls getBooleanArg to check if they have request for mms version
        // TODO: Possibly remove this and implement as an aspect?
        JSONObject jsonVersion = null;
        boolean paramVal = getBooleanArg(req, "mmsVersion", false);
        if (paramVal) {
            jsonVersion = new JSONObject();
            jsonVersion = getMMSversion();
        }

        return jsonVersion;
    }

    /**
     * Helper utility to get the String value of a request parameter, calls on
     * getParameterNames from the WebScriptRequest object to compare the
     * parameter name passed in that is desired from the header.
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param defaultValue
     *            default value if there is no parameter with the given name
     * @author dank
     * @return 'empty' if the parameter is assigned no value, if it is assigned
     *         "parameter value" (ignoring case), or if it's default is default
     *         value and it is not assigned "empty" (ignoring case).
     */
    public static String getStringArg(WebScriptRequest req, String name,
            String defaultValue) {
        if (!Utils.toSet(req.getParameterNames()).contains(name)) {
            return defaultValue;
        }
        String paramVal = req.getParameter(name);
        return paramVal;
    }

    /**
     * setRequestJSON <br>
     * This will set the AbstractJavaWebScript data member requestJSON. It will
     * make the parsedContent JSONObject remain within the scope of the
     * AbstractJavaWebScript. {@link #requestJSON}
     *
     * @param req
     *            WebScriptRequest
     */
    public void setRequestJSON(WebScriptRequest req) {

        try {
//            privateRequestJSON = new JSONObject();
            privateRequestJSON = (JSONObject) req.parseContent();
        } catch (Exception e) {
            e.printStackTrace();
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not retrieve JSON");
        }
    }

    public JSONObject getRequestJSON() {
        return privateRequestJSON;
    }

    public JSONObject getRequestJSON(WebScriptRequest req) {
        // Returns immediately if requestJSON has already been set before checking MMS Versions
        if(privateRequestJSON == null) return privateRequestJSON;
        // Sets privateRequestJSON
        setRequestJSON(req);
        return privateRequestJSON;
    }

    protected String createJenkinsConfig(String jobID,
                                       Map<String,String> propertyValues,
                                       boolean createNewJob) {
        if( createNewJob ) System.out.println("CREATING JOB " + jobID);
        else System.out.println("UPDATING JOB " + jobID);
        
        JenkinsEngine jenkins = new JenkinsEngine();
        JenkinsBuildConfig config = new JenkinsBuildConfig();
        config.setJobID( jobID );
        
        String schedule = propertyValues.get("schedule");
        if( schedule != null ) {
            config.setSchedule( schedule );
        }
        
        String command = propertyValues.get("command");
        if( command != null ) {            
            String[] commandArgs = command.split( "," );
            if ( commandArgs.length >= 2 ) {
                
                // TODO: We need a way of parameterizing teamwork items because command 
                //       will not be coming from the client side 
                
                // NOTE: this may also mean that there will be no 'command' property 
                //       in the defining features
                config.setDocumentID( commandArgs[0].trim() );
                config.setTeamworkProject( commandArgs[1].trim() );            
            } else {
                String message = "Command not supported: " + command;
                log(Level.WARN, HttpServletResponse.SC_NOT_IMPLEMENTED, message);
            }
        }
        
        // Some values which are not currently subject to change
        config.setTeamworkServer( "cae-tw-uat.jpl.nasa.gov" ); 
        config.setTeamworkPort( "18051" );        
        config.setWorkspace( "master" );
        config.setMmsServer( ActionUtil.getHostName() );   
               
        jenkins.postConfigXml( config, config.getJobID(), createNewJob );    
               
        // Recreate a Jenkin's instance so we can query for the job's URL and add it to the json
        // so the user can have easy access to it 
        jenkins = new JenkinsEngine();
        
        jenkins.constructBuildUrl( jobID, JenkinsEngine.detail.URL );
        jenkins.execute();
        String jobUrl = jenkins.jsonResponse.optString( "url" );
        
        return jobUrl;
    }
    
    
    class IdNodeJson {
        public IdNodeJson( String id, EmsScriptNode node, JSONObject json ) {
            super();
            this.id = id;
            this.node = node;
            this.json = json;
        }
        String id;
        EmsScriptNode node;
        JSONObject json;
    }
   

    /**
     * For element json that could be for job properties, dig the property
     * values out of the json and save for later use.
     * 
     * @param jobAsElementJson
     * @param elements
     * @param elementMap
     * @param workspace
     * @param isElement
     * 
     * @return whether or not the job is new or already created
     */  
    protected void processJobProperties(JSONArray elements,
                                           Map<String, JSONObject> elementMap,
                                           WorkspaceNode workspace ) {
        for( int i = 0; i < elements.length(); i++ ) {
            
            JSONObject propertyJson = elements.optJSONObject( i );            
            if( EmsScriptNode.maybeJobProperty( propertyJson ) ) {
                // Get the property name from the id of the slot of the job
                // property json.
                String identifiedJobPropertyName =
                        getNameOfJobPropertyForSlot( propertyJson );
                IdNodeJson job = getOwningJobOfPropertyJson( propertyJson, elementMap, workspace, null );
                String jobId = job == null ? null : job.id;
                if( !Utils.isNullOrEmpty( identifiedJobPropertyName ) ) {
                    String propertyValue = getStringValueFromPropertyJson(propertyJson);
                    Utils.put( propertyValues, jobId, identifiedJobPropertyName,
                               propertyValue);
                }
            }
        }
    }

    /**
     * Create element json for properties of the job if they do not already
     * exist. Save the property values from the job (not from the properties)
     * for later use.
     * 
     * @param jobJson
     * @param elements
     * @param elementMap
     * @param workspace
     * @param isElement
     * 
     * @return whether or not the job is new or already created
     */
    protected boolean processJobAsJob(JSONObject jobJson, JSONArray elements,
                                   Map<String, JSONObject> elementMap,
                                   WorkspaceNode workspace ) {
        if ( jobJson == null ) {
            log( Level.ERROR, "Bad job json: " + jobJson );
            return false;
        }

        String jobId = getJobIdFromJson( jobJson, true );

        // Find node for id.
        EmsScriptNode jobNode = findScriptNodeById( jobId, workspace, null, false );

        boolean createNewJob = false;
        if( jobNode == null ) createNewJob = true;
        
        // Process properties and remove them from the job json, which is being
        // transformed into element json.
        for ( String propertyName : jobProperties ) {
            // Save away the property values for later use;
            String propertyValue = jobJson.optString( propertyName );
            if ( !Utils.isNullOrEmpty(propertyValue) ) {
                Utils.put( propertyValues, jobId, propertyName, propertyValue );
            }
   
            // Update or create the property element json. The returned json is
            // null unless new element json was added for the property.
            JSONObject propertyElementJson =
                    createOrUpdatePropertyJson( propertyName, jobJson, createNewJob, jobNode, jobId,
                                                elements );

            if( propertyElementJson != null && propertyName.equals( "url" )) {
                jobUrl = propertyElementJson;

                if( jobJson.has( propertyName ) ) jobJson.remove( propertyName );
            }            
            else if( propertyElementJson != null ) {
                elements.put( propertyElementJson );
                String propertyId = propertyElementJson.optString("sysmlid");
                if ( !Utils.isNullOrEmpty( propertyId ) ) {
                    elementMap.put( propertyId, propertyElementJson );
                }
                                
                // Remove the property from the job json so that we can use it
                // as the element json for the model post.
                if( jobJson.has( propertyName ) ) jobJson.remove( propertyName );
            }

            // Use job json as element json and move to "elements" array. The
            // job-specific properties in the json were stripped out above.
            if( jobJson.length() > 1) elements.put(jobJson);
        }
        
        return createNewJob;
    }
    
    protected String getStringValueFromPropertyJson( JSONObject propertyJson ) {
        JSONObject specJson = propertyJson.optJSONObject( Acm.JSON_SPECIALIZATION );
        if ( specJson != null && specJson.has( "value"  ) ) {
            JSONArray valueArr = specJson.getJSONArray( "value" );
            if ( valueArr.length() > 0 ) {
                JSONObject valueSpec = valueArr.optJSONObject( 0 );
                if ( valueSpec != null ) {
                    if ( !valueSpec.has( "string" ) ) return null;
                    String value = valueSpec.optString( "string" );
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Get the name of the job property based on the defining feature of the slot.
     * 
     * @param elementJson
     * @param elements
     * @param elementMap
     * @param workspace
     * @return the name of the property (ex, schedule, command, status, ...)
     */
    protected String getNameOfJobPropertyForSlot(JSONObject elementJson ) {
        if ( elementJson == null ) {
            log( Level.ERROR, "Bad job json: " + elementJson );
            return null;
        }

        // trying to map out the property name with an object to
        // identify the name coming from MD
        String propertyId = elementJson.getString( "sysmlid" );
        return getNameOfJobPropertyForSlot( propertyId);
    }
    
    protected String getNameOfJobPropertyForSlot(String  slotId ) {
        // split the string and get the defining feature 
        String[] slotIdParts = slotId.split( "-slot-" );
        // then compare it and set it when they map correctly
        if( slotIdParts.length > 1 ) {
            String definingFeatureIdForSlot = slotIdParts[1];
            for ( String propertyName : jobProperties ) {
                if( definingFeatures.get( propertyName ).equals( definingFeatureIdForSlot ) ) {
                    return propertyName;
                }
            }
        }
        return null;
    }

    public EmsScriptNode getJobPropertyNode( EmsScriptNode jobNode, String propertyName ) {
        if( jobNode == null )
            return null;        
        Collection<EmsScriptNode> slots = jobNode.getAllSlots( jobNode, 
                                                               false, null, null, 
                                                               services, response, responseStatus, null );
        
        EmsScriptNode propertyNode = null;
        if( !Utils.isNullOrEmpty( slots ) ) {
            Iterator< EmsScriptNode > itr = slots.iterator();
            
            while( itr.hasNext() ) {
                String definingFeatureId = getDefiningFeatureId( propertyName );
                propertyNode = itr.next();
                
                String[] slotIdParts = propertyNode.getSysmlId().split( "-slot-" );
                // then compare it and set it when they map correctly
                if( slotIdParts.length > 1 ) {
                    if( slotIdParts[1].equals( definingFeatureId ) ) {                    
                        return propertyNode;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find the Property (json or alfresco node) representing the job property
     * (such as status, schedule), create a json element for the property if it
     * does not exist, and then put its value within a map for easy access.
     * 
     * The method assumes that the Property element already exists, either in
     * the database (MMS) or the input "elements" json. If it doesn't exist. the
     * method complains and creates one improperly since it does not find the
     * applied stereotype instance.
     *
     * @param propertyName
     * @param jobJson
     * @param createNewJob
     * @param jobNode
     * @param elements
     * @return the property json only if it is newly created
     */
    public JSONObject createOrUpdatePropertyJson( String propertyName,
                                      JSONObject jobJson,
                                      boolean createNewJob,
                                      EmsScriptNode jobNode,
                                      String jobId,
                                      JSONArray elements ) {  
        //JSONObject propertyJson = property;
        String propertyValue = null;
        
        // check to see if the property value can be grabbed already, 
        // whether it comes in as a job or element 
        // if so, store it
        if( jobJson.has( propertyName ) ) {
            propertyValue = jobJson.optString( propertyName );
        }

        // No need to create a property if we don't have a value for it.
        if ( Utils.isNullOrEmpty( propertyValue ) ) {
            return null;
        }
        
        String propertyId = null;
        
        // Attempt to find an existing property node in the database and get its
        // id.
        if ( jobNode != null ) {
            EmsScriptNode propertyNode =
                    getJobPropertyNode( jobNode, propertyName );
            if ( propertyNode != null ) {
                propertyId = propertyNode.getSysmlId();
            }
        }

//        JSONObject propertyJson = getPropertyJson(propertyId, elements);
//        if ( propertyJson != null ) {
//            // The property value should have been provided in the job
//            // json. If it wasn't, then try to get it from the property json. If
//            // they both provide property values, throw an error if they
//            // disagree.
//            String value = getStringValueFromPropertyJson( propertyJson );
//            if ( value != null && propertyValue != null && !value.equals( propertyValue ) ) {
//                String jsonPropertyId = propertyJson.optString( "sysmlid" );
//                logger.error( "job json says value of " + propertyName + " is \""
//                              + propertyValue
//                              + "\", but posting element json (id=" + jsonPropertyId
//                              + ") says the value is " + value + "; using " + propertyValue + " !" );
//                value = propertyValue;
//            }
//            if ( value != null ) {
//                Utils.put( propertyValues, jobId, propertyName, value );
//            }
//
//            // Returning null to indicate that no new element json was created
//            return null;
//        }

        JSONObject propertyJson = Utils.get(this.propertyJson, jobId, propertyName);

        
        boolean creatingNewPropertyJson = propertyJson == null;
        
        // Add one if it isn't there already.
        if ( creatingNewPropertyJson ) {
            propertyJson = new JSONObject();
            // elements.put( propertyJson ); // do this from caller
        }
        if ( propertyId != null ) {
            propertyJson.put( "sysmlid", propertyId );  // may already be there
            //elementsMap.put(propertyId, propertyJson);  // do this from caller
        } else if ( propertyJson.has( "sysmlid" ) ) {
            propertyId = propertyJson.getString( "sysmlid" );
        }

        // add name
        if( !propertyJson.has("name") ) {
            propertyJson.put( "name", "" );  // Slots in MagicDraw have "" names.
        }

        String instanceSpecId = null;
        
        // We need the instance spec id and defining feature id to determine the property/slot id.
        // We may not have an instance spec for a new job and need to create one.
        instanceSpecId = getOrCreateInstanceSpecFromJob( jobNode, jobId, elements );
        // If we didn't find/create the id, then see if we can dig it out of
        // the property id in the json.
        if ( Utils.isNullOrEmpty( instanceSpecId ) ) {
            instanceSpecId = getInstanceSpecIdFromSlotId( propertyJson );
        }
        if ( Utils.isNullOrEmpty( instanceSpecId ) ) {
            logger.error( "Could not find or create an instance spec id for the "
                          + propertyName + " property of " + jobId );
            return null;
       }
        // Make sure we have an id for the property.
        if ( Utils.isNullOrEmpty( propertyId ) ) {
            String definingFeatureId = getDefiningFeatureId( propertyName );
            propertyId = instanceSpecId + "-slot-" + definingFeatureId;
            
            propertyJson.put( "sysmlid", propertyId );
        }
        
        // add owner
        if( instanceSpecId != null && !propertyJson.has("owner") ) {
            propertyJson.put( "owner", instanceSpecId );
        }

        // add specialization part with value
        JSONObject specJson = new JSONObject();
        propertyJson.put( "specialization", specJson );

        specJson.put( "type", "Property" );
        specJson.put( "isSlot", true);
        JSONArray valueArr = new JSONArray();
        specJson.put( "value", valueArr );
        if ( propertyValue != null ) {
            JSONObject value = new JSONObject();
            valueArr.put(value);
            value.put( "type", "LiteralString" );
            value.put( "string", propertyValue );
        }
      
        return propertyJson;
    }
    
    /**
     * Get or create a job's instance spec for connecting slots for its
     * properties. The element json for the instance spec is placed in this
     * webscript's instanceSpecs map to b
     * 
     * @param jobNode
     *            a node for the job element, for which the instance spec is
     *            sought; this may be null
     * @param jobId
     *            the id of the job (which must match that of the jobNode if
     *            jobNode is not null; this must be non-null
     * @param elements
     *            the set of posted elements
     * @return the id of the existing or new instance spec or null if jobId is
     *         null
     */
    protected String getOrCreateInstanceSpecFromJob( EmsScriptNode jobNode,
                                                     String jobId,
                                                     JSONArray elements ) {
        if ( jobId == null ) return null; 
        String instanceSpecId = null;
        JSONObject instanceSpec = null;

        // the instance spec can already exist in the MMS
        if ( jobNode != null ) {
            EmsScriptNode spec = jobNode.getInstanceSpecification();
            if ( spec != null ) {
                instanceSpecId = spec.getSysmlId();
                // Since the instance spec already exists, we only need the id
                // in the json.
                instanceSpec = new JSONObject();
                instanceSpec.put( "sysmlid", instanceSpecId );
            }
        }

        // The instance spec may also be in the JSON, which we will have to loop
        // through every element to identify it by comparing the owner ID with
        // the job ID.
        if ( elements != null ) {
            for(int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.optJSONObject( i );
                
                String ownerId = element.optString( "owner" );
                
                if( ownerId != null && ownerId.equals( jobId ) ) {
                    // make sure it's an instance spec
                    if ( isInstanceSpec( element ) ) {
                        instanceSpecId = element.optString( "sysmlid" );
                        instanceSpec = element;                
                        break;
                    }
                }
            }
        }

        // Create the instance spec if we can't find it.
        if ( instanceSpec == null ) {
            instanceSpecId = NodeUtil.createId( NodeUtil.getServiceRegistry() );
            instanceSpec = createJobInstanceSpecificationJson( instanceSpecId, jobId );
        }
        
        if ( instanceSpec == null ) {
            // It should be impossible to get here.
            logger.error( "Could not find or create instance spec for " + jobId );
        } else {
            // WARNING: This isn't going into the elementsMap being passed
            // around so don't count on it being there. Find it in the
            // instanceSpecs map for this object where it is placed below.
            elements.put( instanceSpec );
        }
        
//        // Save for use in creating new job json or the ids of the job's slots.
//        instanceSpecs.put( jobId, instanceSpec );
        
        return instanceSpecId;
    }

    public boolean isInstanceSpec(JSONObject element) {
        JSONObject specialization = element.optJSONObject(Acm.JSON_SPECIALIZATION );
        if ( specialization != null ) {
            String type = specialization.optString( Acm.JSON_TYPE );
            if ( type.equals(Acm.JSON_INSTANCE_SPECIFICATION ) ) {
                return true;
            }
        }
        // check metatype
        JSONArray metatypes = element.optJSONArray( Acm.JSON_APPLIED_METATYPES );
        for ( int i = 0; i < metatypes.length(); ++i ) {
            String metatype = metatypes.optString(i);
            if ( metatype != null && metatype.equals( JobGet.instanceSpecId ) ) {
                return true;
            }
        }
        return false;
    }
    
    
    protected JSONObject createJobInstanceSpecificationJson(String specSysmlId, String jobId  ) {
        JSONObject specElement = new JSONObject();
        specElement.put("name", "");
        specElement.put("sysmlid", specSysmlId);
        specElement.put("owner", jobId);
        specElement.put("documentation", "");
        JSONArray specAppliedMetatypes = new JSONArray();
        // Add the instance specification metatype id.
        specAppliedMetatypes.put("_9_0_62a020a_1105704885251_933969_7897");
        specElement.put("appliedMetatypes", specAppliedMetatypes);
        JSONObject specSpecialization = new JSONObject();
        JSONArray specClassifier = new JSONArray();
        specClassifier.put(JobGet.jobStereotypeId);
        specSpecialization.put("classifier", specClassifier);
        specSpecialization.put("type", "InstanceSpecification");
        specElement.put("specialization", specSpecialization);
        specElement.put("isMetatype", false);
        return specElement;
    }
    
    /**
     * Get the JSONObject, if it exists, in the input JSONArray with the
     * specified sysmlid.
     * 
     * @param propertyId
     * @param elements
     * @return the JSONObject or null if not found
     */
    protected JSONObject getPropertyJson( String propertyId, JSONArray elements ) {
        if ( propertyId == null ) return null;
        JSONObject propertyJson = null;
        for ( int i = 0; i < elements.length(); ++i ) {
            JSONObject element = elements.optJSONObject( i );
            if ( element == null ) continue;
            String id = element.optString( "sysmlid" );
            if ( id != null && id.equals( propertyId ) ) {
                propertyJson = element;
                break;
            }
        }
        return propertyJson;
    }

    protected String getDefiningFeatureId( String propertyName ) {
        return definingFeatures.get(propertyName);
    }

    protected String getInstanceSpecIdFromSlotId( JSONObject slotJson ) {

        // make sure the json has a slot id
        String propertyId = slotJson.optString( "sysmlid" );
        if ( Utils.isNullOrEmpty( propertyId ) ) return null;
        if ( !propertyId.contains( "-slot-" ) ) return null;
        
        // split the string and get the instance spec
        String[] slotIdParts = propertyId.split( "-slot-" );
        // store the instance spec in this variable
        String instanceSpecId = null;

        // the instance spec id will be obtained from an existing property
        if( slotIdParts.length > 1 ) {
            instanceSpecId = slotIdParts[0];
            return instanceSpecId;
        }
        
        return null;
    }

    private boolean jenkinsConfigExists( String sysmlId ) {
        // TODO Auto-generated method stub
        return false;
    }

    protected String getJobIdFromJson( JSONObject job, boolean createAndAddIfMissing ) {
        // Get the id
        String jobId = job.optString( "id" );
        if ( Utils.isNullOrEmpty( jobId ) ) {
            jobId = job.optString( "sysmlid" );
        }
        if ( createAndAddIfMissing && Utils.isNullOrEmpty( jobId ) ) {
            jobId = NodeUtil.createId( getServices() );
            job.put( "sysmlid", jobId );
        }
        return jobId;
    }
    
    /**
     * Add to propertyJson the json for job elements whose Properties are posted
     * without the job. We do this because only jobs that show up in the json
     * are processed.
     * 
     * @param elements
     * @return ids of jobs that are not already in the elementMap
     */
    public ArrayList< String >
          getJobsIdsForPropertyElementJson( Map< String, JSONObject > elementMap,
                                            //JSONArray elements,
                                            WorkspaceNode workspace ) {
        ArrayList< String > jobIds = new ArrayList< String >();
        for ( JSONObject elem : elementMap.values() ) {
            if ( elem == null ) continue;
            IdNodeJson jobStuff =
                    getOwningJobOfPropertyJson( elem, elementMap, workspace, null );
            EmsScriptNode job = jobStuff == null ? null : jobStuff.node;
            if ( job != null ) {
                String jobId = job.getSysmlId();
                if ( jobId != null && !elementMap.containsKey( jobId ) ) {
                    jobIds.add( jobId );
                }
                String propName = getNameOfJobPropertyForSlot( elem );
                if ( !Utils.isNullOrEmpty( propName ) ) {
                    Utils.put(propertyJson, jobId, propName, elem);
                }
            }
        }
        return jobIds;
    }
    
    protected void processJobsJson( JSONObject json, WorkspaceNode workspace ) {
        if ( json == null ) return;

        // Get "jobs" as opposed to "elements"
        JSONArray jobs = json.optJSONArray( "jobs" );
        
        // Get or create "elements" array.
        JSONArray elements = json.optJSONArray( "elements" );
        if ( elements == null ) {
            elements = new JSONArray();
            json.put( "elements", elements );
        }

        // Step 1
        // Add the job metatype to the json for the jobs if not already there.
        // Also, add the jobs to the elements array.
        if ( jobs != null ) {
            for ( int i = 0; i < jobs.length(); i++ ) {
                JSONObject job = jobs.optJSONObject( i );
                if ( job != null ) {
                    addJobMetatype(job);
                    elements.put( job );
                }
            }
        }

        // Step 2
        // Create an elementMap for quick lookup of element JSONObjects.
        // Later, we add the jobs to this map and the elements json array.
        Map< String, JSONObject > elementMap = new LinkedHashMap< String, JSONObject >();
        for ( int i = 0; i < elements.length(); i++ ) {
            JSONObject elem = elements.optJSONObject( i );
            if ( elem == null ) continue;
            String sysmlId = elem.optString( "sysmlid" );
            if ( sysmlId == null ) continue;
            elementMap.put( sysmlId, elem );
        }
        
        // Step 3
        // Add json for job elements whose Properties are posted without the job.
        // We do this because only jobs that show up in the json are processed.
        // This call also saves wa
        ArrayList< String > jobIdsNotPosted =
                getJobsIdsForPropertyElementJson( elementMap, //elements,
                                                  workspace );
        for ( String jobId : jobIdsNotPosted ) {
            // add placeholder json for job
            JSONObject jobJson = new JSONObject();
            jobJson.put( "sysmlid", jobId );
            elements.put( jobJson );
            elementMap.put(jobId, jobJson);
        }

        // This is to keep track of whether each job is created or updated, so
        // that we know what to send to jenkins.
        HashMap<String, Boolean> createNewJob = new HashMap<String, Boolean>();
        
        // Step 4
        // Loop through the "elements" json array, and for each element, check
        // to see if it is a job and process it to gather property values to add
        // to the Jenkins configuration.
        for ( int i = 0; i < elements.length(); i++ ) {
            JSONObject elem = elements.optJSONObject( i );
            
            if ( EmsScriptNode.isJob( elem ) ) {
                String id = getJobIdFromJson( elem, false );
//              // Find node for id.
                // FIXME -- if the id of a deleted job is posted, we won't get
                // the properties of the deleted node unless we pass true in the
                // find call below.
                EmsScriptNode jobNode = findScriptNodeById( id, workspace, null, false );
                boolean isNew = jobNode == null;
                if( id != null ) createNewJob.put( id, isNew );
                if ( jobNode != null ) {
                    getMissingPropertyValues(jobNode);
                }
            }
        }

        // Step 5
        // Look for job properties in the posted json and save values in
        // propertyValues.
        processJobProperties( elements, elementMap, workspace );

        // Step 6
        // Generate or update element json for each of the properties.
        if ( jobs != null ) {
            for ( int i = 0; i < jobs.length(); i++ ) {
                JSONObject job = jobs.optJSONObject( i );
                boolean isNew = processJobAsJob( job, elements, elementMap, workspace );
                String id = getJobIdFromJson( job, false );
                if( id != null ) createNewJob.put( id, isNew );                    
            }
        }
        
        // Get missing property values from DB for jenkins config
        //getMissingPropertyValues(jobIds);
        
        // Step 7
        // FIXME -- Don't send the jenkins config until the post is complete; in
        // fact, the propertyValues should be gathered after the fact.
        for ( String jobId : propertyValues.keySet() ) {
            Map< String, String > properties = propertyValues.get( jobId );
            String url = createJenkinsConfig( jobId, properties, createNewJob.get( jobId ) == true );
            
            // NOTE: how can we get the jobUrlJson? either way, up to this point
            //       we have a URL by returning it (implies the job has been created)

            if( url != null ) {                
                JSONObject specJson = jobUrl.optJSONObject( Acm.JSON_SPECIALIZATION );
                if ( specJson != null && specJson.has( "value"  ) ) {
                    JSONArray valueArr = specJson.getJSONArray( "value" );
                    if ( valueArr.length() > 0 ) {
                        JSONObject valueSpec = valueArr.optJSONObject( 0 );
                        if ( valueSpec != null ) {
                            valueSpec.put( "string", url );
                        }
                    }
                }
                
                elements.put( jobUrl );
            }
                       
            /*  
             JSONObject jobUrlJson = ??
             if( jobUrlJson != null) {
                 elements.put( jobUrlJson );
                 String urlId = propertyElementJson.optString("sysmlid");
                 if ( !Utils.isNullOrEmpty( propertyId ) ) {
                     elementMap.put( propertyId, propertyElementJson );
                 }
                 
             }
             
             if( jobJson.has( "url" ) ) jobJson.remove( "url" );
             */
        }

        json.remove( "jobs" );
    }

    /**
     * 
     * @param jobNode
     */
    private void getMissingPropertyValues( EmsScriptNode jobNode ) {
        if ( jobNode == null ) return;
        Collection<EmsScriptNode> slots = jobNode.getAllSlots( jobNode, 
                                                               false, null, null, 
                                                               services, response, responseStatus, null );
        for ( EmsScriptNode slot : slots ) {
            getMissingPropertyValues( slot, jobNode );
        }
//        for ( String propertyName : AbstractJavaWebScript.jobProperties ) {
//            //String definingFeature = definingFeatures.get( propertyName );
//            getMissingPropertyValues( propertyName, jobNode );
//        }
    }
    private void getMissingPropertyValues( EmsScriptNode slot,
                                           EmsScriptNode jobNode ) {
        // Attempts to find the node and retrieve the value that way.
//        EmsScriptNode propertyNode =
//                getJobPropertyNode( jobNode, propertyName );
        
        Collection< Object > values = getSystemModel().getValue( slot, null );

        String slotId = slot.getSysmlId();
        String propertyName = getNameOfJobPropertyForSlot( slotId );
        if ( !Utils.isNullOrEmpty( values ) ) {
            if ( values.size() > 1 ) {
                // TODO -- ERROR?
            }
            String jobId = jobNode.getSysmlId();
            Object value = values.iterator().next();
            if ( value != null ) {
                Utils.put( propertyValues, jobId, propertyName, value.toString() ); 
            }
        }
    }

    private void getMissingPropertyValues( String propertyName,
                                           EmsScriptNode jobNode ) {
        // Attempts to find the node and retrieve the value that way.
        EmsScriptNode propertyNode =
                getJobPropertyNode( jobNode, propertyName );
        
        Collection< Object > values = getSystemModel().getValue( propertyNode, null );

        if ( !Utils.isNullOrEmpty( values ) ) {
            if ( values.size() > 1 ) {
                // TODO -- ERROR?
            }
            String jobId = jobNode.getSysmlId();
            Object value = values.iterator().next();
            if ( value != null ) {
                Utils.put( propertyValues, jobId, propertyName, value.toString() ); 
            }
        }
    }
    
    public void addJobMetatype( JSONObject job ) {
        JSONArray appliedMetatypes = job.optJSONArray( "appliedMetatypes" );
        if ( appliedMetatypes == null ) {
            appliedMetatypes = new JSONArray();
            job.put("appliedMetatypes", appliedMetatypes);
        }
        boolean found = false;
        for ( int j = 0; j < appliedMetatypes.length(); ++j ) {
            String m = appliedMetatypes.optString( j );
            if ( m != null && m.equals( JobGet.jobStereotypeId ) ) {
                found = true;
                break;
            }
        }
        if (!found) {
            appliedMetatypes.put( JobGet.jobStereotypeId );
        }        
    }
    
    protected IdNodeJson getOwningJobOfPropertyJson( JSONObject propertyJson,
                                                     Map< String, JSONObject > elementMap,
                                                     WorkspaceNode workspace,
                                                     Date dateTime ) {
        if ( propertyJson == null ) return null;

        String instanceSpecId = getInstanceSpecIdFromSlotId( propertyJson );
        if ( Utils.isNullOrEmpty( instanceSpecId ) ) return null;

        JSONObject instanceSpecJson = elementMap.get(instanceSpecId);
        EmsScriptNode owner = null;
        String ownerId = null;
        if ( instanceSpecJson != null ) {
            ownerId = instanceSpecJson.optString( "owner" );
        }
        if ( !Utils.isNullOrEmpty( ownerId ) ) {
            // look for node in DB
            EmsScriptNode instanceSpecNode = findScriptNodeById( instanceSpecId, workspace, dateTime, false );
            if ( instanceSpecNode != null ) {
                owner = instanceSpecNode.getOwningParent( dateTime, workspace, false );
                if ( owner != null ) {
                    ownerId = owner.getSysmlId();
                }
            }
        }

        if ( owner == null && ownerId == null ) return null;

        
        if ( owner == null ) {
            owner = findScriptNodeById( ownerId, workspace, dateTime, false );
        }
        // look for json
        JSONObject ownerJson = null;
        if ( !Utils.isNullOrEmpty( ownerId ) ) {
            ownerJson = elementMap.get( ownerId );
        }
        if ( ownerId != null && 
             ( ( owner != null && owner.isJob() ) ||
               ( ownerJson != null && EmsScriptNode.isJob( ownerJson ) ) ) ) {
            return new IdNodeJson( ownerId, owner, ownerJson );
        }

        return null;
    }
    
    public void postProcessJson( JSONObject top ) {
        // redefine this if you want to add jobs or other things; see
    }

    protected JSONObject getJsonForElement( EmsScriptNode element,
                                            WorkspaceNode ws, Date dateTime,
                                            String id,
                                            boolean includeQualified,
                                            boolean isIncludeDocument ) {
        return getJsonForElement( element, null, false, ws, dateTime, includeQualified,
                                  isIncludeDocument, null,
                                  elementProperties.get( id ) );
    }
    protected JSONObject getJsonForElement( EmsScriptNode element,
                                            Set<String> filter,
                                            boolean isExpOrProp,
                                            WorkspaceNode ws, Date dateTime,
                                            boolean includeQualified,
                                            boolean isIncludeDocument,
                                            Version version,
                                            List<EmsScriptNode> ownedProperties ) {
        return element.toJSONObject( filter, isExpOrProp, ws, dateTime,
                                     includeQualified, isIncludeDocument, version,
                                     ownedProperties );
    }
  
    public JSONObject getJsonForElementAndJob( EmsScriptNode job,
                                               Set<String> filter,
                                               boolean isExpOrProp,
                                               WorkspaceNode ws,
                                               Date dateTime, String id,
                                               boolean includeQualified,
                                               boolean isIncludeDocument ) {

        JSONObject json =
                this.getJsonForElement( job, filter, isExpOrProp, ws, dateTime,
                                        includeQualified, isIncludeDocument,
                                        null, elementProperties.get( id ) );
        getJsonForJob(job, json);
        return json;
    }
    
    public JSONObject getJsonForElementAndJob( EmsScriptNode job,
                                               Set<String> filter,
                                               boolean isExpOrProp,
                                               WorkspaceNode ws,
                                               Date dateTime,
                                               boolean includeQualified,
                                               boolean isIncludeDocument,
                                               Version version,
                                               List<EmsScriptNode> ownedProperties ) {
        JSONObject json =
                this.getJsonForElement( job, filter, isExpOrProp, ws, dateTime, includeQualified,
                                        isIncludeDocument, version,
                                        ownedProperties );
        getJsonForJob(job, json);
        return json;
    }

    public void getJsonForJob(EmsScriptNode job, JSONObject json) {
        if ( job.isJob() ) {
            JSONObject jobJson = null;
            for ( String propertyName : jobProperties ) {

                EmsScriptNode propertyNode =
                        getJobPropertyNode( job, propertyName );
                // get property values and add to json (check for null)
                
                if ( propertyNode != null ) {
                    Collection< Object > values =
                            getSystemModel().getValue( propertyNode, null );
                    if ( !Utils.isNullOrEmpty( values ) ) {
                        if ( values.size() > 1 ) {
                            // TODO -- ERROR?
                        }
                        Object value = values.iterator().next();
                        if ( value != null ) {
                            jobJson = addJobPropertyToJson( propertyName, value,
                                                            jobJson, json );
                        }
                    }
                }
            }
            if ( jobJson != null && jobsJsonArray != null) {
                jobsJsonArray.put( jobJson );
            }
        }
    }

    protected JSONObject addJobPropertyToJson( String propertyName,
                                               Object value,
                                               JSONObject jobJson,
                                               JSONObject json ) {
        if ( json == null ) return jobJson;
        if ( jobJson == null ) {
            jobJson = NodeUtil.clone( json );
        } 
        jobJson.put( propertyName, jsonWrap( value ) );
        
        return jobJson;
    }
    
    public static String getConfig(String key) {
        return EmsConfig.get(key);
    }

}
