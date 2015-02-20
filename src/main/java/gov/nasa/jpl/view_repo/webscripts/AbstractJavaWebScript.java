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

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import java.util.Formatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteVisibility;
//import org.apache.log4j.Level;
import org.apache.log4j.*;
//import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;


/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities
 * that were in utils.js
 *
 * @author cinyoung
 *
 */
public abstract class AbstractJavaWebScript extends DeclarativeWebScript {
    private static Logger logger = Logger.getLogger(AbstractJavaWebScript.class);
    public Level logLevel = Level.WARN;
    
    public Formatter formatter = new Formatter ();
    /*public enum LogLevel {
		DEBUG(0), INFO(1), WARNING(2), ERROR(3);
		private int value;
		private LogLevel(int value) {
			this.value = value;
		}
	}*/

    public static final int MAX_PRINT = 200;

    public static boolean defaultRunWithoutTransactions = false;

    // injected members
	protected ServiceRegistry services;		// get any of the Alfresco services
	protected Repository repository;		// used for lucene search

	// internal members
    // when run in background as an action, this needs to be false
    public boolean runWithoutTransactions = defaultRunWithoutTransactions;
	protected ScriptNode companyhome;
	protected Map<String, EmsScriptNode> foundElements = new HashMap<String, EmsScriptNode>();

	// needed for Lucene search
	protected static final StoreRef SEARCH_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    protected Status responseStatus = new Status();

    protected WorkspaceDiff wsDiff;

    public static boolean alwaysTurnOffDebugOut = true;

    // keeps track of who made the call to the service
    protected String source = null;

    protected void initMemberVariables(String siteName) {
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}

	public AbstractJavaWebScript( Repository repository,
                                  ServiceRegistry services,
                                  StringBuffer response ) {
        this.setRepositoryHelper( repository );
        this.setServices( services );
        this.response = response ;
        // TODO -- set maximum log level; Overrides that specified in log4j.properties (I THINK)
        logger.setLevel(logLevel);
    }

    public AbstractJavaWebScript(Repository repositoryHelper, ServiceRegistry registry) {
        this.setRepositoryHelper(repositoryHelper);
        this.setServices(registry);
        // TODO -- set maximum log level; Overrides that specified in log4j.properties (I THINK)
        logger.setLevel(logLevel);
    }

    public AbstractJavaWebScript() {
        // default constructor for spring
        super();
        // TODO -- set maximum log level; Overrides that specified in log4j.properties (I THINK)
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

    abstract protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
                                                              final Status status,
                                                              final Cache cache );
    
    protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
                                                     final Status status, final Cache cache,
                                                     boolean withoutTransactions ) {
        clearCaches( true );
        clearCaches(); // calling twice for those redefine clearCaches() to
                       // always call clearCaches( false )
        if ( withoutTransactions ) {
            return executeImplImpl( req, status, cache );
        }
        final Map< String, Object > model = new HashMap<String, Object>();
        new EmsTransaction( getServices(), getResponse(), getResponseStatus() ) {
            @Override
            public void run() throws Exception {
                Map< String, Object > m = executeImplImpl( req, status, cache );
                if ( m != null ) {
                    model.putAll( m );
                }
            }
        };
//            UserTransaction trx;
//            trx = services.getTransactionService().getNonPropagatingUserTransaction();
//            try {
//                trx.begin();
//                NodeUtil.setInsideTransactionNow( true );
//            } catch ( Throwable e ) {
//                String msg = null;
//                tryRollback( trx, e, msg );
//            }
        //Map<String, Object> model = new HashMap<String, Object>();
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
	protected EmsScriptNode findScriptNodeById(String id,
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
    	if (level.toInt() >= logger.getLevel().toInt()) {
    		String formattedMsg = formatMessage(msg,params);
    		//String formattedMsg = formatter.format (msg,params).toString();
    		log (level,code,formattedMsg);
    	}
	}
    
    // If no need for string formatting (calls with no string concatenation)
    protected void log(Level level, int code, String msg) {
		if (level.toInt() >= logger.getLevel().toInt()) {
			// print to response stream if >= existing log level
			String levelMessage = String.format("[%s]: %s\n",level.toString(),msg);
			log(code,levelMessage);
			// print to console if log level >= Warning (i.e. Error, Fatal, or Off)
			if (level.toInt() >= Level.WARN.toInt()) {
				 log (level, msg);
			}
		}
	}

    // only logging loglevel and a message (no code)
	protected void log(Level level, String msg, Object...params) {
	    if (level.toInt() >= logger.getLevel().toInt()) {
        	String formattedMsg = formatter.format (msg,params).toString();
        	response.append(formattedMsg);
	        log (level, formattedMsg);
	    }
	}

	// only logging code and a message (no loglevel, and thus, no check for log level status)
	protected void log(int code, String msg, Object...params) {
		String formattedMsg = formatter.format (msg,params).toString();
		log (code,formattedMsg);
	}
	
	protected void log(String msg, Object...params) {
		String formattedMsg = formatter.format (msg,params).toString();
		log (formattedMsg);
	}
	
	protected void log ( int code, String msg) {
		response.append(msg);
		responseStatus.setCode(code);
		responseStatus.setMessage(msg);
	}

	protected void log(String msg) {
	    response.append(msg + "\n");
	    //TODO: add to responseStatus too (below)?
	    //responseStatus.setMessage(msg);
	}

	protected void log (Level level, String msg){
	    switch(level.toInt()) {
	        case Level.FATAL_INT:
	            logger.fatal(msg);
	            break;
	        case Level.ERROR_INT:
	            logger.error( msg );
	            break;
	        case Level.WARN_INT:
	            logger.warn( msg );
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
	
	// formatMessage function is used to catch certain objects that must be dealt with individually
	// formatter.format() is avoided because it applies toString() directly to objects which provide unreadable outputs
	protected String formatMessage (String initMsg,Object...params){
		String formattedMsg = initMsg;
		Pattern p = Pattern.compile("(%s)");
		Matcher m = p.matcher(formattedMsg);
		
		for (Object obj: params){
			if (obj.getClass().isArray()){
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
				formattedMsg = m.replaceFirst(obj.toString());
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
	        return node.checkPermissions( permissions, response, responseStatus );
	    } else {
	        return false;
	    }
	}


//	/**
//	 * Checks whether user has permissions to the nodeRef and logs results and status as appropriate
//	 * @param nodeRef      NodeRef to check permissions againts
//	 * @param permissions  Permissions to check
//	 * @return             true if user has specified permissions to node, false otherwise
//	 */
//	protected boolean checkPermissions(NodeRef nodeRef, String permissions) {
//		if (services.getPermissionService().hasPermission(nodeRef, permissions) != AccessStatus.ALLOWED) {
//			log(LogLevel.WARNING, "No " + permissions + " priveleges to " + nodeRef.toString() + ".\n", HttpServletResponse.SC_BAD_REQUEST);
//			return false;
//		}
//		return true;
//	}


    protected static final String WORKSPACE_ID = "workspaceId";
	protected static final String PROJECT_ID = "projectId";
	protected static final String ARTIFACT_ID = "artifactId";
    protected static final String SITE_NAME = "siteName";
    protected static final String SITE_NAME2 = "siteId";

    public static final String NO_WORKSPACE_ID = "master"; // default is master if unspecified
    public static final String NO_PROJECT_ID = "no_project";
    public static final String NO_SITE_ID = "no_site";


    public String getSiteName( WebScriptRequest req ) {
        return getSiteName( req, false );
    }
    public String getSiteName( WebScriptRequest req, boolean createIfNonexistent ) {
        String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        if ( siteName == null ) {
            siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME2);
        }
        if ( siteName == null || siteName.length() <= 0 ) {
            siteName = NO_SITE_ID;
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

    public String getProjectId( WebScriptRequest req ) {
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        if ( projectId == null || projectId.length() <= 0 ) {
            projectId = NO_PROJECT_ID;
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


	protected boolean checkRequestVariable(Object value, String type) {
		if (value == null) {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "%s not found.\n",type);
			return false;
		}
		return true;
	}

	/**
	 * Perform Lucene search for the specified pattern and ACM type
	 * TODO: Scope Lucene search by adding either parent or path context
	 * @param type		escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
	 * @param pattern   Pattern to look for
	 */
	protected Map<String, EmsScriptNode> searchForElements(String type,
	                                                       String pattern,
	                                                       boolean ignoreWorkspace,
	                                                       WorkspaceNode workspace,
	                                                       Date dateTime) {
		return this.searchForElements( type, pattern, ignoreWorkspace,
		                               workspace, dateTime, null );
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

        Map<String, EmsScriptNode> searchResults = new HashMap<String, EmsScriptNode>();

        searchResults.putAll( NodeUtil.searchForElements( type, pattern, ignoreWorkspace,
                                                          workspace,
                                                          dateTime, services,
                                                          response,
                                                          responseStatus,
                                                          siteName) );

        return searchResults;

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
        paramVal = paramVal.toLowerCase();
        if ( paramVal.equals( "true" ) ) return true;
        if ( paramVal.equals( "false" ) ) return false;
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
        wsDiff = new WorkspaceDiff(workspace, workspace);
    }
    public void setWsDiff(WorkspaceNode workspace1, WorkspaceNode workspace2, Date time1, Date time2) {
        wsDiff = new WorkspaceDiff(workspace1, workspace2, time1, time2);
    }

    public WorkspaceDiff getWsDiff() {
        return wsDiff;
    }

    /**
     * Determines the project site for the passed site node.  Also, determines the
     * site package node if applicable.
     *
     */
    public Pair<EmsScriptNode,EmsScriptNode> findProjectSite(WebScriptRequest req, String siteName,
                                                             WorkspaceNode workspace,
                                                             EmsScriptNode initialSiteNode) {

        EmsScriptNode sitePackageNode = null;
        EmsScriptNode siteNode = null;

        // If it is a package site, get the corresponding package for the site:
        NodeRef sitePackageRef = (NodeRef) initialSiteNode.getProperty( Acm.ACM_SITE_PACKAGE );
        if (sitePackageRef != null) {
            sitePackageNode = new EmsScriptNode(sitePackageRef, services);
        }
        // Could find the package site using the property, try searching for it:
        else if (siteName != null && siteName.startsWith(NodeUtil.sitePkgPrefix)) {

            String[] splitArry = siteName.split(NodeUtil.sitePkgPrefix);
            if (splitArry != null && splitArry.length > 0) {
                String sitePkgName = splitArry[splitArry.length-1];

                sitePackageNode = findScriptNodeById(sitePkgName,workspace, null, false );

                if (sitePackageNode == null) {
                    log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find site package node for site package name %s",siteName);
                    return null;
                }
            }
        }

        // Found the package for the site:
        if (sitePackageNode != null) {
            // Sanity check:
            NodeRef sitePackageSiteRef = (NodeRef) sitePackageNode.getProperty( Acm.ACM_SITE_SITE );
            if (sitePackageSiteRef != null && !sitePackageSiteRef.equals( initialSiteNode.getNodeRef() )) {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Mismatch between site/package for site package name %s",siteName);
                return null;
            }

            // Get the project site by tracing up the parents until the parent is null:
            NodeRef siteParentRef = (NodeRef) initialSiteNode.getProperty( Acm.ACM_SITE_PARENT );
            EmsScriptNode siteParent = siteParentRef != null ? new EmsScriptNode(siteParentRef, services, response) : null;
            EmsScriptNode oldSiteParent = null;

            while (siteParent != null) {
                oldSiteParent = siteParent;
                siteParentRef = (NodeRef) siteParent.getProperty( Acm.ACM_SITE_PARENT );
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
    public EmsScriptNode getSiteForPkgSite(EmsScriptNode pkgNode, WorkspaceNode workspace) {

        NodeRef pkgSiteParentRef = (NodeRef)pkgNode.getProperty( Acm.ACM_SITE_SITE );
        EmsScriptNode pkgSiteParentNode = null;

        if (pkgSiteParentRef != null) {
            pkgSiteParentNode = new EmsScriptNode(pkgSiteParentRef, services);
        }
        // Couldn't find it using the property, try searching for it:
        else {
            // Search for it. Will have a cm:name = "site_"+sysmlid of site package node:
            String sysmlid = pkgNode.getSysmlId();
            if (sysmlid != null) {
                pkgSiteParentNode = findScriptNodeById(NodeUtil.sitePkgPrefix+sysmlid, workspace, null, false);
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
    public EmsScriptNode findParentPkgSite(EmsScriptNode node, EmsScriptNode siteNode,
                                           EmsScriptNode projectNode, WorkspaceNode workspace) {

        EmsScriptNode pkgSiteParentNode = null;
        EmsScriptNode siteParent = node.getParent();
        EmsScriptNode siteParentReifNode;
        while (siteParent != null && siteParent.exists()) {

            siteParentReifNode = siteParent.getReifiedNode();

            // If the parent is a package and a site, then its the parent site node:
            if (siteParentReifNode != null && siteParentReifNode.hasAspect(Acm.ACM_PACKAGE) ) {
                Boolean isSiteParent = (Boolean) siteParentReifNode.getProperty( Acm.ACM_IS_SITE );
                if (isSiteParent != null && isSiteParent) {

                    // Get the alfresco Site for the site package node:
                    pkgSiteParentNode = getSiteForPkgSite(siteParentReifNode, workspace);
                    break;
                }
            }

            // If the parent is the project, then the site will be the project Site:
            if ((siteParentReifNode != null && siteParentReifNode.equals( projectNode )) ||
                siteParent.equals(projectNode)) {
                if (siteNode != null) {
                    pkgSiteParentNode = siteNode;
                }
                break;  // break no matter what b/c we have reached the project node
            }

            if (siteParent.isWorkspaceTop()) {
                break;
            }

            siteParent = siteParent.getParent();
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
        
        // Send the progress over JMS:
        CommitUtil.sendProgress(msg, workspaceId, projectId);
        
        // Email the progress (this takes a long time, so only do it for critical events):
        if (sendEmail) {
            String hostname = services.getSysAdminParams().getAlfrescoHost();
            if (!Utils.isNullOrEmpty( hostname )) {
                String sender = hostname + "@jpl.nasa.gov";
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
    
}
