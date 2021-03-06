/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S.
 * Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.*;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbEdgeTypes;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor in
 * /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts
 * /gov/nasa/jpl/javawebscripts/model.get.desc.xml
 * 
 * @author cinyoung
 * 
 */
public class ModelGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger( ModelGet.class );

    public ModelGet() {
        super();
    }

    public ModelGet( Repository repositoryHelper, ServiceRegistry registry ) {
        super( repositoryHelper, registry );
    }

    // injected via spring configuration
    protected boolean isViewRequest = false;

    protected JSONArray elements = new JSONArray();
    protected Map< String, EmsScriptNode > elementsFound =
            new HashMap< String, EmsScriptNode >();
    protected boolean prettyPrint = true;

    @Override
    protected void clearCaches() {
        super.clearCaches();
        elements = new JSONArray();
        elementsFound = new HashMap< String, EmsScriptNode >();
        elementProperties = new HashMap< String, List< EmsScriptNode > >();
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // This is all unnecessary as it's already checked in the handle
        // String[] idKeys = {"modelid", "elementid", "elementId"};
        // String modelId = null;
        // for (String idKey: idKeys) {
        // modelId = req.getServiceMatch().getTemplateVars().get( idKey );
        // if (modelId != null) {
        // break;
        // }
        // }
        //
        // if (modelId == null) {
        // log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
        // "Element id not specified.\n");
        // return false;
        // }
        //
        // // get timestamp if specified
        // String timestamp = req.getParameter( "timestamp" );
        // Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        //
        // EmsScriptNode modelRootNode = null;
        //
        // WorkspaceNode workspace = getWorkspace( req );
        // boolean wsFound = workspace != null && workspace.exists();
        // if ( !wsFound ) {
        // String wsId = getWorkspaceId( req );
        // if ( wsId != null && wsId.equalsIgnoreCase( "master" ) ) {
        // wsFound = true;
        // } else {
        // log( Level.ERROR, HttpServletResponse.SC_NOT_FOUND,
        // "Workspace with id, %s not found", wsId
        // + ( dateTime == null ? "" : " at " + dateTime ));
        // return false;
        // }
        // }
        // // need to find deleted elements in workspace, so can return not
        // found rather than
        // // the node from parent workspace
        // boolean findDeleted = true;
        // if ( wsFound ) modelRootNode = findScriptNodeById(modelId, workspace,
        // dateTime, findDeleted);
        //
        // if (modelRootNode == null || modelRootNode.isDeleted() ) {
        // log( Level.WARN, HttpServletResponse.SC_NOT_FOUND,
        // "Element with id, %s not found", modelId
        // + ( dateTime == null ? "" : " at " + dateTime ));
        // return false;
        // }
        //
        // // TODO: need to check permissions on every node ref - though it
        // looks like this might throw an error
        // if (!checkPermissions(modelRootNode, PermissionService.READ)) {
        // return false;
        // }

        return true;
    }

    /**
     * Entry point
     */
    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        AbstractJavaWebScript instance = new ModelGet( repository, getServices() );
        return instance.executeImplImpl( req, status, cache,
                                         runWithoutTransactions );
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status,
                                                     Cache cache ) {
        Timer timer = new Timer();
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);

        Map< String, Object > model = new HashMap< String, Object >();
        // make sure to pass down view request flag to instance
        setIsViewRequest( isViewRequest );

        JSONObject top = NodeUtil.newJsonObject();
        JSONArray elementsJson = handleRequest( req, top );
        

        try {
            if ( elementsJson.length() > 0 ) {
                top.put( "elements", elementsJson );
            }
            postProcessJson(top);
            // boolean evaluate = getBooleanArg( req, "evaluate", false );
            // WorkspaceNode ws = getWorkspace( req );
            // if ( evaluate ) {
            // Set< EmsScriptNode > elementSet = new HashSet<EmsScriptNode>(
            // elementsFound.values() );
            // Map< Object, Object > r = evaluate(elementSet , ws);
            // top
            // }

            if ( !Utils.isNullOrEmpty( response.toString() ) ) top.put( "message",
                                                                        response.toString() );
            if ( prettyPrint ) {
                model.put( "res", NodeUtil.jsonToString( top, 4 ) );
            } else {
                model.put( "res", NodeUtil.jsonToString( top ) );
            }
        } catch ( JSONException e ) {
            log( Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                 "Could not create JSONObject" );
            model.put( "res", createResponseJson() );
            e.printStackTrace();
        }

        status.setCode( responseStatus.getCode() );

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Wrapper for handling a request and getting the appropriate JSONArray of
     * elements
     * 
     * @param req
     * @param top
     * @return
     */
    protected JSONArray handleRequest( WebScriptRequest req,
                                       final JSONObject top ) {
        // REVIEW -- Why check for errors here if validate has already been
        // called? Is the error checking code different? Why?
        try {
            String[] idKeys =
                    { "modelid", "elementid", "elementId", "jobid", "jobId" };
            String modelId = null;
            for ( String idKey : idKeys ) {
                modelId = req.getServiceMatch().getTemplateVars().get( idKey );
                if ( modelId != null ) {
                    break;
                }
            }

            if ( null == modelId ) {
                log( Level.ERROR, HttpServletResponse.SC_NOT_FOUND,
                     "Could not find element %s", modelId );
                return new JSONArray();
            }

            // get timestamp if specified
            String timestamp = req.getParameter( "timestamp" );
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
            boolean connected = getBooleanArg( req, "connected", false );
            boolean evaluate = getBooleanArg( req, "evaluate", false );
            boolean affected = getBooleanArg( req, "affected", false );
            String relationship = req.getParameter( "relationship" );
            boolean includeQualified = getBooleanArg(req, "extended", false);
//            boolean includeQualified = getBooleanArg( req, "qualified", true );
//            if ( NodeUtil.doPostProcessQualified ) includeQualified = false;

            WorkspaceNode workspace = getWorkspace( req );

            // see if prettyPrint default is overridden and change
            prettyPrint = getBooleanArg( req, "pretty", prettyPrint );

            Long depth = getDepthFromRequest( req );
            // force qualified to be false so DeclarativeWebscripts can inject
            // it later

			if (logger.isTraceEnabled()) logger.trace("modelId = " + modelId);
			boolean findDeleted = depth == 0 ? true : false;
			boolean notFoundInGraphDb = false;
			EmsScriptNode modelRootNode = null;			
			
			// search using db if enabled - if not there revert to modelRootNode
			// DB can only be used against latest at the moment
			if (NodeUtil.doGraphDb && dateTime == null) {
			    PostgresHelper pgh = new PostgresHelper(workspace);
    			        try {
                        pgh.connect();
                        if (pgh.checkWorkspaceExists()) {
                            modelRootNode = NodeUtil.getNodeFromPostgresNode(pgh.getNodeFromSysmlId( modelId ));
                        }
                    } catch ( Exception e ) {
                        logger.info( "Could not find element in graph db " + modelId );
                    } finally {
                        pgh.close();
                    }
			    if (modelRootNode == null) notFoundInGraphDb = true;
			}
			if (modelRootNode == null) {
			    modelRootNode = findScriptNodeById(modelId,
			                                       workspace, dateTime, findDeleted);
			    if (modelRootNode != null && notFoundInGraphDb) {
			        logger.warn( "Could not find element in graphDb: " + modelId );
			        // FIXME: this needs to be tested, so it's off by default
			        // put it back in the graph if it wasn't there. this will make things a bit slow..
			        if ( NodeUtil.doAutoBuildGraphDb ) {
        			        Model2Postgres m2p = new Model2Postgres(repository, services);
        			        m2p.buildGraphDb( modelRootNode, dateTime, workspace, modelRootNode.getSysmlId() );
			        }
			    }
			}

            if ( logger.isTraceEnabled() ) logger.trace( "modelRootNode = "
                                                         + modelRootNode );

            if ( modelRootNode == null ) {
				log(Level.INFO, HttpServletResponse.SC_NOT_FOUND,
				    String.format("Element %s not found", modelId
								+ (dateTime == null ? "" : " at " + dateTime)));
                return new JSONArray();
            } else if ( modelRootNode.isDeleted() ) {
                log( Level.DEBUG, HttpServletResponse.SC_GONE,
                     "Element exists, but is deleted." );
                return new JSONArray();
            }

			if (isViewRequest) {
				handleViewHierarchy(modelRootNode, workspace, dateTime, depth,
						new Long(0));
			} else {
				handleElementHierarchy(modelRootNode, workspace, dateTime,
						depth, new Long(0), connected, relationship,
						new HashSet<String>(), notFoundInGraphDb);
			}

            boolean checkReadPermission = true; // TODO -- REVIEW -- Shouldn't
                                                // this be false?

            handleElements( workspace, dateTime, includeQualified, false,
                            evaluate, affected, top, checkReadPermission );
        } catch ( JSONException e ) {
            e.printStackTrace();
        } catch ( SQLException e ) {
            e.printStackTrace();
        }

        return elements;
    }

    /**
     * Get the depth to recurse to from the request parameter.
     * 
     * @param req
     * @return Depth < 0 is infinite recurse, depth = 0 is just the element (if
     *         no request parameter)
     */
    private Long getDepthFromRequest( WebScriptRequest req ) {
        Long depth = null;
        String depthParam = req.getParameter( "depth" );
        // recurse default is false
        boolean recurse = getBooleanArg( req, "recurse", false );

        if ( depthParam != null ) {
            try {
                depth = Long.parseLong( depthParam );
            } catch ( NumberFormatException nfe ) {
                if ( !recurse ) {
                    // don't do any recursion, ignore the depth
                    log( Level.WARN, HttpServletResponse.SC_BAD_REQUEST,
                         "Bad depth specified: " + depthParam + ", returning depth 0" );
                }
            }
        }

		// for backwards compatiblity convert recurse to infinite depth (this
        // overrides any depth setting)
		if (recurse) {
            // If depth wasn't specified, it will be null.
            if ( depth == null ) { // || 
//                 ( depth <= 0 && ( Utils.isNullOrEmpty( depthParam ) ||
//                                   !depthParam.trim().equals( "0" ) ) ) ) {
                depth = new Long(-1);
            }
        }

		if (depth == null) {
			depth = new Long(0);
		}

		return depth;
	}

	/**
	 * Recurse a view hierarchy to get all allowed elements
	 * 
	 * @param root
	 *            Root view to find elements for
	 * @param recurse
	 *            If true, find elements for children views
	 * @throws JSONException
	 *             JSON element creation error
	 */
	protected void handleViewHierarchy(EmsScriptNode root,
			final WorkspaceNode workspace, final Date dateTime, final Long maxDepth,
			Long currDepth) throws JSONException {
		Object allowedElements = root.getProperty(Acm.ACM_ALLOWED_ELEMENTS);
		if (allowedElements != null) {
			JSONArray childElementJson = new JSONArray(
					allowedElements.toString());
			for (int ii = 0; ii < childElementJson.length(); ii++) {
			    // FIXME: Use graph db to find all the nodes
				final String id = childElementJson.getString(ii);
                new EmsTransaction(getServices(), getResponse(),
                                   getResponseStatus(), false, true) {
                    
                    @Override
                    public void run() throws Exception {


        				EmsScriptNode childElement = findScriptNodeById(id, workspace,
        						dateTime, false);
        
        				// TODO Need to report that allowedElements can't be found
        				if (childElement != null && childElement.exists()) {
        					if (checkPermissions(childElement, PermissionService.READ)) {
        						elementsFound.put(id, childElement);
        					} // TODO -- REVIEW -- Warning if no permissions?
        				} else {
        					this.log(Level.WARN, 
        							String.format( "Element %s not found", id
        									+ (dateTime == null ? "" : " at "
        											+ dateTime)),
        									HttpServletResponse.SC_NOT_FOUND);
        				}
                    }
                };
			}
			if (maxDepth != null && (maxDepth < 0 || currDepth < maxDepth)) {
				currDepth++;
				Object childrenViews = root.getProperty(Acm.ACM_CHILDREN_VIEWS);
				if (childrenViews != null) {
					JSONArray childViewJson = new JSONArray(
							childrenViews.toString());
					for (int ii = 0; ii < childViewJson.length(); ii++) {
						String id = childViewJson.getString(ii);
						final EmsScriptNode childView = findScriptNodeById(id,
								workspace, dateTime, false);
						if (childView != null && childView.exists()) {
							if (checkPermissions(childView,
									PermissionService.READ)) {
							    
							    final Long currDepthT = currDepth;
				                new EmsTransaction(getServices(), getResponse(),
				                                   getResponseStatus(), false, true) {
				                    
				                    @Override
				                    public void run() throws Exception {

				                        handleViewHierarchy(childView, workspace,
				                                            dateTime, maxDepth,
				                                            currDepthT);
				                    }
				                };
				                
							} // TODO -- REVIEW -- Warning if no permissions?
						} else {
							log(Level.WARN, HttpServletResponse.SC_NOT_FOUND,
									"Element %s not found", id
											+ (dateTime == null ? "" : " at "
													+ dateTime));
						}
					}
				}
			}
		}
	}

	/**
	 * Get all elements in tree from the specified root
	 * 
	 * @param root
	 *            Root node to get children for
	 * @param recurse
	 * @param workspace
	 * @param dateTime
	 * @param maxDepth
	 * @param currDepth
	 * @throws JSONException
	 * @throws SQLException
	 */
	protected void handleElementHierarchy(EmsScriptNode root,
			WorkspaceNode workspace, Date dateTime, final Long maxDepth,
			Long currDepth, boolean connected, String relationship,
			Set<String> visited, boolean notFoundInGraphDb) throws JSONException, SQLException {

		if (!notFoundInGraphDb && dateTime == null && !connected && NodeUtil.doGraphDb) {
			handleElementHierarchyPostgres(root, workspace, dateTime, maxDepth,
					currDepth, connected, relationship, visited);
		}
		else {
			handleElementHierarchyOriginal(root, workspace, dateTime, maxDepth,
					currDepth, connected, relationship, visited);
		}
	}

	protected void handleElementHierarchyPostgres(EmsScriptNode root,
			WorkspaceNode workspace, Date dateTime, final Long maxDepth,
			Long currDepth, boolean connected, String relationship,
			Set<String> visited) throws JSONException, SQLException {
		// Note: root sysmlid will never have _pkg at the end.
		// don't return any elements
		if (!root.exists()) {
			return;
		}
		
		// no permissions, return
		if (!checkPermissions( root, PermissionService.READ )) {
		    return;
		}

		// get children for given sysmlId from database
		PostgresHelper pgh = new PostgresHelper(workspace);

		List<Pair<String, Pair<String, String>>> childrenNodeRefIds = null;
		try {
			int depth = 1000000;
			if (maxDepth >= 0) {
				depth = maxDepth.intValue();
			}
            pgh.connect();
    		    childrenNodeRefIds = pgh.getChildren(root.getSysmlId(),
    					DbEdgeTypes.REGULAR, depth);
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
		    pgh.close();
		}

		if (childrenNodeRefIds == null) return;
		for (Pair<String, Pair<String, String>> c : childrenNodeRefIds) {
			EmsScriptNode ecn = new EmsScriptNode(new NodeRef(c.second.second),
					services, response);

			if (!ecn.exists() || ecn.getSysmlId().endsWith("_pkg")
					|| ecn.isOwnedValueSpec(dateTime, workspace)
					|| !checkPermissions(ecn, PermissionService.READ))
				continue;

			elementsFound.put(c.second.second, ecn);
		}
	}

	protected void handleElementHierarchyOriginal(EmsScriptNode root,
			final WorkspaceNode workspace, final Date dateTime, final Long maxDepth,
			Long currDepth, final boolean connected, final String relationship,
			final Set<String> visited) throws JSONException, SQLException {
		String sysmlId = root.getSysmlId();

		if (visited.contains(sysmlId)) {
			return;
		}

		// don't return any elements
		if (!root.exists()) {
			return;
		}

		// add root element to elementsFound if its not already there
		// (if it's there, it's probably because the root is a reified pkg node)
		String rootName = sysmlId;
		visited.add(sysmlId);
		if (!elementsFound.containsKey(sysmlId)) {
			// dont add reified packages
            if ( maxDepth == null || maxDepth == 0 ||
                 ( !rootName.endsWith( "_pkg" ) &&
                   !root.isOwnedValueSpec( dateTime, workspace ) ) ) {
				elementsFound.put(sysmlId, root);
			}
		}

		if (maxDepth != null && (maxDepth < 0 || currDepth < maxDepth)) {
			++currDepth;
			// Find all the children, recurse or add to array as needed.
			// If it is a reified package, then need get the reifiedNode
			if (rootName.endsWith("_pkg")) {
				EmsScriptNode reifiedNode = findScriptNodeById(
						rootName.substring(0, rootName.lastIndexOf("_pkg")),
						workspace, dateTime, false);
				if (reifiedNode != null) {
					handleElementHierarchyOriginal(reifiedNode, workspace,
							dateTime, maxDepth, currDepth, connected,
							relationship, visited);
				} // TODO -- REVIEW -- Warning or error?
			}

			// Handle all the children in this workspace:
			List<NodeRef> childRefs = connected ? root.getConnectedNodes(
					dateTime, workspace, relationship) : root.getOwnedChildren(
					false, dateTime, workspace);

			for (final NodeRef childRef : childRefs) {
				if (childRef == null)
					continue;
				final EmsScriptNode child = new EmsScriptNode(childRef, services,
						response);
				if (checkPermissions(child, PermissionService.READ)) {
					if (child.exists()
							&& !child.isOwnedValueSpec(dateTime, workspace)) {

						String value = child.getSysmlId();

						if (value != null && !value.endsWith("_pkg")) {
							elementsFound.put(value, child);
						}

						final Long currDepthT = currDepth; 
		                new EmsTransaction(getServices(), getResponse(),
		                                   getResponseStatus(), false, true) {
		                    
		                    @Override
		                    public void run() throws Exception {
		                        handleElementHierarchyOriginal(child, workspace,
								  dateTime, maxDepth, currDepthT, connected,
								  relationship, visited);
		                    }
		                };
		                
					} // ends if (child.exists() && !child.isOwnedValueSpec())
				} // ends if ( checkPermissions( child, PermissionService.READ )
					// )
			}

		} // ends if (recurse)
	}

	/**
	 * Build up the element JSONObject
	 * 
	 * @param top
	 * @param evaluate
	 * 
	 * @throws JSONException
	 */
	protected void handleElements(final WorkspaceNode ws, final Date dateTime,
			final boolean includeQualified, final boolean isIncludeDocument, boolean evaluate, boolean affected, JSONObject top,
			boolean checkPermission) throws JSONException {
		final Map<EmsScriptNode, JSONObject> elementsJsonMap = new LinkedHashMap<EmsScriptNode, JSONObject>();
        if( affected ){
            addAffectedElements(ws, dateTime);
        }
        for ( final String id : elementsFound.keySet() ) {
            final EmsScriptNode node = elementsFound.get( id );

            if ( !checkPermission
                 || checkPermissions( node, PermissionService.READ ) ) {

                // don't return deleted elements
                if (node.isDeleted()) continue;

                new EmsTransaction(getServices(), getResponse(),
                                   getResponseStatus(), false, true) {
                    
                    @Override
                    public void run() throws Exception {
                        JSONObject json = getJsonForElement( node, ws, dateTime,
                                                             id, includeQualified,
                                                             isIncludeDocument );
                        
                        elements.put( json );
                        elementsJsonMap.put( node, json );
                    }
                };
                
            } // TODO -- REVIEW -- Warning if no permissions?
        }
        if ( evaluate ) {
            try {
                evaluate( elementsJsonMap, top, ws );
            } catch ( IllegalAccessException e ) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            } catch ( InvocationTargetException e ) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            } catch ( InstantiationException e ) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
        }
    }

    protected void addAffectedElements( WorkspaceNode ws, Date dateTime ) {
        for ( String id : new ArrayList< String >( elementsFound.keySet() ) ) {
            EmsScriptNode value = elementsFound.get( id );
            if ( value != null ) {
                List< NodeRef > nodeRefs =
                        value.getAffectedElementsRecursive( false, false,
                                                            dateTime, ws, false,
                                                            true, true, false,
                                                            null );
                for ( NodeRef ref : nodeRefs ) {
                    EmsScriptNode node = new EmsScriptNode( ref, services );
                    String nodeId = node.getSysmlId();
                    if ( elementsFound.containsKey( nodeId ) ) {
                        continue;
                    } else {
                        elementsFound.put( nodeId, node );
                    }
                }
            }
        }
    }

    /**
     * Need to differentiate between View or Element request - specified during
     * Spring configuration
     * 
     * @param flag
     */
    public void setIsViewRequest( boolean flag ) {
        isViewRequest = flag;
    }

    /**
     * Special filtering for embedded value specs. Adds element that "owns" the
     * value spec. If propertyName is not null, filters out all the elements
     * that dont "own" properties that have the specified propertyName.
     * 
     * @param propertyName
     * @param ws
     * @param dateTime
     */
    protected void filterValueSpecs( String propertyName, final WorkspaceNode ws,
                                     final Date dateTime ) {

        // REVIEW do we only want to do this for Property? Currently for all
        // value specs.

        Map< String, EmsScriptNode > elementsToAdd =
                new HashMap< String, EmsScriptNode >();
        Set< String > valueSpecsToRemove = new HashSet< String >();

        for ( Entry< String, EmsScriptNode > entry : elementsFound.entrySet() ) {

            EmsScriptNode element = entry.getValue();
            String valueSpecId = entry.getKey();

            // If its a embedded value spec:
            if ( element.isOwnedValueSpec( dateTime, ws ) ) {

                // Get the value spec owner, ie a Property:
                final EmsScriptNode valueSpecOwner =
                        element.getValueSpecOwner( dateTime, ws );

                if ( valueSpecOwner != null ) {
                    EmsScriptNode elementWithProperty = null;
                    String propertyNameFnd = null;

                    
                    if ( valueSpecOwner.hasAspect( Acm.ACM_PROPERTY ) ) {

                        final ArrayList<NodeRef> arr = new ArrayList< NodeRef >();
                        new EmsTransaction(getServices(), getResponse(),
                                           getResponseStatus(), false, true) {
                            
                            @Override
                            public void run() throws Exception {

                                NodeRef propertyTypeRef =
                                        (NodeRef)valueSpecOwner.getNodeRefProperty( Acm.ACM_PROPERTY_TYPE,
                                                                                    dateTime,
                                                                                    ws );
                                if ( propertyTypeRef != null ) {
                                    arr.add( propertyTypeRef );
                                }
                            }
                        };
                        NodeRef propertyTypeRef = null;
                        if ( !arr.isEmpty() ) {
                            propertyTypeRef = arr.get(0);
                        }
                                // The property name is the name of the propertyType
                        // (property is a slot):
                        if ( propertyTypeRef != null ) {
                            EmsScriptNode propertyType =
                                    new EmsScriptNode( propertyTypeRef,
                                                       services );
                            propertyNameFnd = propertyType.getSysmlName();
                            EmsScriptNode stereotypeInstance =
                                    valueSpecOwner.getUnreifiedParent( dateTime,
                                                                       ws );

                            if ( stereotypeInstance != null ) {
                                elementWithProperty =
                                        stereotypeInstance.getUnreifiedParent( dateTime,
                                                                               ws );
                            }
                        }
                        // The property name is the name of the property:
                        else {
                            propertyNameFnd = valueSpecOwner.getSysmlName();
                            elementWithProperty =
                                    valueSpecOwner.getUnreifiedParent( dateTime,
                                                                       ws );
                        }

                    } // Ends if a Property
                    else {
                        // We want the owner of the value spec owner:
                        elementWithProperty =
                                valueSpecOwner.getUnreifiedParent( dateTime,
                                                                   ws );
                    }

                    if ( elementWithProperty != null ) {

                        boolean elementFnd = true;

                        // If we are searching for specific propertyName:
                        if ( !Utils.isNullOrEmpty( propertyName ) ) {

                            // The property names match, so add the element with
                            // the property:
                            elementFnd = propertyName.equals( propertyNameFnd );
                        }

                        if ( elementFnd ) {
                            valueSpecsToRemove.add( valueSpecId );
                            elementsToAdd.put( elementWithProperty.getSysmlId(),
                                               elementWithProperty );
                        }
                    } // ends if (elementWithProperty != null)

                } // ends if (valueSpecOwner != null)
            } // ends if (element.isOwnedValueSpec( dateTime, ws ))

        } // ends for

        // Add the found property owners:
        if ( !Utils.isNullOrEmpty( propertyName ) ) {
            elementsFound = elementsToAdd;
        } else {
            elementsFound.putAll( elementsToAdd );
            elementsFound.keySet().removeAll( valueSpecsToRemove );
        }

    }

    /**
     * Adds the owned properties of the found elements to elementProperties
     * 
     * @param ws
     * @param dateTime
     */
    protected void addElementProperties( final WorkspaceNode ws, final Date dateTime ) {

        // For every element, find the owned properties:
        for ( Entry< String, EmsScriptNode > entry : elementsFound.entrySet() ) {

            final EmsScriptNode element = entry.getValue();
            final List< EmsScriptNode > props = new ArrayList< EmsScriptNode >();

            new EmsTransaction(getServices(), getResponse(),
                               getResponseStatus(), false, true) {
                
                @Override
                public void run() throws Exception {

            // some issues with results in graph DB throw errors, catch and move
            // on
            try {
                for ( NodeRef childRef : element.getOwnedChildren( false,
                                                                   dateTime,
                                                                   ws ) ) {
                    if ( childRef != null ) {
                        EmsScriptNode child =
                                new EmsScriptNode( childRef, services );

                        // If it is a property then add it:
                        if ( child.hasAspect( Acm.ACM_PROPERTY ) ) {
                            props.add( child );
                        }
                        // If it is a applied stereotype, then check its
                        // children:
                        else if ( child.hasAspect( Acm.ACM_INSTANCE_SPECIFICATION ) ) {

                            for ( NodeRef specChildRef : child.getOwnedChildren( false,
                                                                                 dateTime,
                                                                                 ws ) ) {

                                if ( specChildRef != null ) {

                                    EmsScriptNode specChild =
                                            new EmsScriptNode( specChildRef,
                                                               services );

                                    if ( specChild.hasAspect( Acm.ACM_PROPERTY ) ) {
                                        props.add( specChild );
                                    }
                                }
                            } // ends for
                        }
                    } // ends if (childRef != null)
                } // ends for
            } catch ( Exception e ) {
                logger.warn( "Could not find owner. Dumping stack trace." );
                e.printStackTrace();
            }

                } // end of run()
            }; // end of new EmsTransaction()

            elementProperties.put( element.getSysmlId(), props );

        } // ends for

    }

}
