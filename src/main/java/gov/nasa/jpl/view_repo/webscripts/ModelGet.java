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
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor in /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/model.get.desc.xml
 * @author cinyoung
 *
 */
public class ModelGet extends AbstractJavaWebScript {
    public ModelGet() {
        super();
    }

    public ModelGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    // injected via spring configuration
    protected boolean isViewRequest = false;

	protected JSONArray elements = new JSONArray();
	protected Map<String, EmsScriptNode> elementsFound = new HashMap<String, EmsScriptNode>();

    protected boolean prettyPrint = true;

    @Override
	protected void clearCaches() {
		super.clearCaches();
		elements = new JSONArray();
		elementsFound = new HashMap<String, EmsScriptNode>();
	}


	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
	    String[] idKeys = {"modelid", "elementid", "elementId"};
	    String modelId = null;
	    for (String idKey: idKeys) {
	        modelId = req.getServiceMatch().getTemplateVars().get( idKey );
	        if (modelId != null) {
	            break;
	        }
	    }

		if (modelId == null) {
			log(LogLevel.ERROR, "Element id not specified.\n", HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}

        // get timestamp if specified
        String timestamp = req.getParameter( "timestamp" );
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        EmsScriptNode modelRootNode = null;

        WorkspaceNode workspace = getWorkspace( req );
        boolean wsFound = workspace != null && workspace.exists();
        if ( !wsFound ) {
            String wsId = getWorkspaceId( req );
            if ( wsId != null && wsId.equalsIgnoreCase( "master" ) ) {
                wsFound = true;
            } else {
                log( LogLevel.ERROR,
                     "Workspace with id, " + wsId
                     + ( dateTime == null ? "" : " at " + dateTime ) + " not found",
                     HttpServletResponse.SC_NOT_FOUND );
                return false;
            }
        }
        // need to find deleted elements in workspace, so can return not found rather than
        // the node from parent workspace
        boolean findDeleted = true;
        if ( wsFound ) modelRootNode = findScriptNodeById(modelId, workspace, dateTime, findDeleted);

		if (modelRootNode == null || modelRootNode.isDeleted() ) {
            log( LogLevel.ERROR,
                 "Element with id, " + modelId
                 + ( dateTime == null ? "" : " at " + dateTime ) + " not found",
                 HttpServletResponse.SC_NOT_FOUND );
			return false;
		}

		// TODO: need to check permissions on every node ref - though it looks like this might throw an error
		if (!checkPermissions(modelRootNode, PermissionService.READ)) {
			return false;
		}

		return true;
	}


    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        ModelGet instance = new ModelGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
    }

	@Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
			Status status, Cache cache) {
	    Timer timer = new Timer();
	    printHeader( req );

		clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();
		// make sure to pass down view request flag to instance
		setIsViewRequest(isViewRequest);

		JSONArray elementsJson = new JSONArray();
		if (validateRequest(req, status)) {
		    elementsJson = handleRequest(req);
		}

		JSONObject top = new JSONObject();
		try {
		    if (elementsJson.length() > 0) {
		        top.put("elements", elementsJson);
		        if (!Utils.isNullOrEmpty(response.toString())) top.put("message", response.toString());
		        if ( prettyPrint ) model.put("res", top.toString(4));
		        else model.put("res", top.toString());
		    } else {
		        log(LogLevel.WARNING, "No elements found",
		            HttpServletResponse.SC_NOT_FOUND);
		        model.put("res", response.toString());
		    }
		} catch (JSONException e) {
			e.printStackTrace();
		}

		status.setCode(responseStatus.getCode());

        printFooter();

        log( LogLevel.INFO, "ModelGet: " + timer );

		return model;
	}

	/**
	 * Wrapper for handling a request and getting the appropriate JSONArray of elements
	 * @param req
	 * @return
	 */
	private JSONArray handleRequest(WebScriptRequest req) {
        // REVIEW -- Why check for errors here if validate has already been
        // called?  Is the error checking code different?  Why?
        try {
            String[] idKeys = {"modelid", "elementid", "elementId"};
            String modelId = null;
            for (String idKey: idKeys) {
                modelId = req.getServiceMatch().getTemplateVars().get(idKey);
                if (modelId != null) {
                    break;
                }
            }

            if (null == modelId) {
                log(LogLevel.ERROR, "Could not find element " + modelId, HttpServletResponse.SC_NOT_FOUND );
                return new JSONArray();
            }

            // get timestamp if specified
            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            WorkspaceNode workspace = getWorkspace( req );

            // see if prettyPrint default is overridden and change
            prettyPrint = getBooleanArg(req, "pretty", prettyPrint );

            if (Debug.isOn()) System.out.println("modelId = " + modelId );
            EmsScriptNode modelRootNode = findScriptNodeById(modelId, workspace, dateTime, false);
            if (Debug.isOn()) System.out.println("modelRootNode = " + modelRootNode );

            if ( modelRootNode == null ) {
                    log( LogLevel.ERROR,
                         "Element " + modelId
                         + ( dateTime == null ? "" : " at " + dateTime ) + " not found",
                         HttpServletResponse.SC_NOT_FOUND );
                    return new JSONArray();
            }

            // recurse default is false
            boolean recurse = getBooleanArg(req, "recurse", false);
            boolean includeQualified = getBooleanArg(req, "qualified", true);

            if (isViewRequest) {
                handleViewHierarchy(modelRootNode, recurse, workspace, dateTime);
            } else {
                handleElementHierarchy( modelRootNode, recurse, workspace, dateTime );
            }

            handleElements(dateTime, includeQualified);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return elements;
	}


	/**
	 * Recurse a view hierarchy to get all allowed elements
	 * @param root		Root view to find elements for
	 * @param recurse	If true, find elements for children views
	 * @throws JSONException	JSON element creation error
	 */
	protected void handleViewHierarchy(EmsScriptNode root, boolean recurse,
	                                   WorkspaceNode workspace, Date dateTime)
	                                           throws JSONException {
		Object allowedElements = root.getProperty(Acm.ACM_ALLOWED_ELEMENTS);
		if (allowedElements != null) {
			JSONArray childElementJson = new JSONArray(allowedElements.toString());
			for (int ii = 0; ii < childElementJson.length(); ii++) {
				String id = childElementJson.getString(ii);
				EmsScriptNode childElement = findScriptNodeById(id, workspace, dateTime, false);

    				// TODO Need to report that allowedElements can't be found
    				if (childElement != null && childElement.exists()) {
                    if ( checkPermissions( childElement, PermissionService.READ ) ) {
                        elementsFound.put( id, childElement );
                    } // TODO -- REVIEW -- Warning if no permissions?
    				} else {
                    log( LogLevel.WARNING,
                         "Element " + id
                         + ( dateTime == null ? "" : " at " + dateTime )
                         + " not found",
                         HttpServletResponse.SC_NOT_FOUND );
    				}
			}
			if (recurse) {
				Object childrenViews = root.getProperty(Acm.ACM_CHILDREN_VIEWS);
				if (childrenViews != null) {
					JSONArray childViewJson = new JSONArray(childrenViews.toString());
					for (int ii = 0; ii < childViewJson.length(); ii++) {
						String id = childViewJson.getString(ii);
                        EmsScriptNode childView =
                                findScriptNodeById( id, workspace, dateTime, false );
						if (childView != null && childView.exists()) {
					        if (checkPermissions(childView, PermissionService.READ)) {
					            handleViewHierarchy( childView, recurse,
					                                 workspace, dateTime );
					        } // TODO -- REVIEW -- Warning if no permissions?
						} else {
		                    log( LogLevel.WARNING,
		                         "Element " + id
		                         + ( dateTime == null ? "" : " at " + dateTime )
                                 + " not found",
                                 HttpServletResponse.SC_NOT_FOUND );
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
     * @param workspace
     * @param dateTime
     * @throws JSONException
     */
	protected void handleElementHierarchy( EmsScriptNode root, boolean recurse,
	                                       WorkspaceNode workspace, Date dateTime )
	                                              throws JSONException {

		// don't return any elements
		if (!root.exists()) {
		    return;
		}

		// add root element to elementsFound if its not already there
		// (if it's there, it's probably because the root is a reified pkg node)
		String sysmlId = root.getSysmlId();
		String rootName = sysmlId;
		if (!elementsFound.containsKey(sysmlId)) {
		    // dont add reified packages
		    if (!rootName.endsWith("_pkg") &&
		        !root.isOwnedValueSpec()) { //isPropertyOwnedValueSpecification()) {
		        elementsFound.put(sysmlId, root);
		    }
		}

		if (recurse) {
			// Find all the children, recurse or add to array as needed.
		    // If it is a reified package, then need get the reifiedNode
		    if ( rootName.endsWith("_pkg") ) {
                EmsScriptNode reifiedNode = findScriptNodeById( rootName.substring( 0, rootName.lastIndexOf("_pkg") ),
                                                                workspace,
                                                                dateTime, false );
		        if (reifiedNode != null) {
                    handleElementHierarchy( reifiedNode, recurse, workspace, dateTime );
		        } // TODO -- REVIEW -- Warning or error?
		    }

		    // Handle all the children in this workspace:
		    for ( NodeRef childRef : root.getOwnedChildren(false) ) {
			    NodeRef vChildRef = NodeUtil.getNodeRefAtTime( childRef, workspace, dateTime );
                if ( vChildRef == null ) {
                    // this doesn't elicit a not found response
                    log( LogLevel.WARNING,
                         "Element " + childRef
                         + ( dateTime == null ? "" : " at " + dateTime ) + " not found");
			        continue;
			    }
                EmsScriptNode child = new EmsScriptNode( vChildRef, services, response );
                if ( checkPermissions( child, PermissionService.READ ) ) {
                    if (child.exists() && !child.isOwnedValueSpec()) { //isPropertyOwnedValueSpecification()) {

                        String value = child.getSysmlId();
                        if ( value != null && !value.endsWith( "_pkg" )) {
                            elementsFound.put( value, child );
                        }

                        handleElementHierarchy( child, recurse, workspace, dateTime );

                    } // ends if (child.exists() && !child.isOwnedValueSpec())
                } // ends if ( checkPermissions( child, PermissionService.READ ) )
			}

		}  // ends if (recurse)
	}

	/**
	 * Build up the element JSONObject
	 * @throws JSONException
	 */
	protected void handleElements(Date dateTime, boolean includeQualified) throws JSONException {
		for (String id: elementsFound.keySet()) {
			EmsScriptNode node = elementsFound.get(id);

			if (checkPermissions(node, PermissionService.READ)){
                elements.put(node.toJSONObject(dateTime, includeQualified));
			} // TODO -- REVIEW -- Warning if no permissions?
		}
	}


	/**
	 * Need to differentiate between View or Element request - specified during Spring configuration
	 * @param flag
	 */
	public void setIsViewRequest(boolean flag) {
	    isViewRequest = flag;
	}
}
