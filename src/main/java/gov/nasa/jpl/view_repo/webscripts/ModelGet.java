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
import org.alfresco.service.cmr.repository.ChildAssociationRef;
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
    
	private JSONObject elementHierarchy = new JSONObject();
	protected JSONArray elements = new JSONArray();
	protected Map<String, EmsScriptNode> elementsFound = new HashMap<String, EmsScriptNode>();

    protected boolean prettyPrint = true;
	
    @Override
	protected void clearCaches() {
		super.clearCaches();
		elementHierarchy = new JSONObject();
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
        ModelGet instance = new ModelGet(repository, services);
        instance.setServices( getServices() );
        return instance.executeImplImpl( req, status, cache );
    }
    
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
        
        System.out.println( "ModelGet: " + timer );

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
            
            if (isViewRequest) {
                handleViewHierarchy(modelRootNode, recurse, workspace, dateTime);
            } else {
                /*
                 FIXME: The solution below is more efficient, but has corner cases
                        it does not handle correctly.  
                        Two cases it does not handle are:
                         Let WS1 be master of WS2, and A,B,Z are elements.
                              Master WS: Proj
                                           |
                                           A
                                           |
                                           B
                                       
                              WS1:       Proj
                                           |
                                           B
                                           
                              WS2:       Proj
                                           |
                                           A
                                           |
                                           Z
                          
                          1. If a get is done on element Proj from WS w/ recurse = true, then it will return the B from the master
                             instead of the B from WS1.
                          2. If a get is done on element A from WS2 w/ recurse = true, then A,B,Z will be returned, but B shouldn't
                             be returned b/c it was moved from A's containment in WS1.
                                       
                */
//                // Process the elements for the desired workspace first, and then use the
//                // ems:source property to process the needed parent workspaces:
//                // Note: Doing two passes, so that we first get the nodes for the desired workspace
//                // before checking the parent workspace.  This is needed in case a node moved
//                // locations.  Otherwise, could do this in one call with source=true:
//                handleElementHierarchy(modelRootNode, recurse, workspace, dateTime, false);
//                if (recurse) {
//                    handleElementHierarchy(modelRootNode, recurse, workspace, dateTime, true);
//                }
                
                /* FIXME
                 * This solution is less efficient, as it has do a lucene search on every
                 * workspace, but solves the 1st corner case described above.  It does NOT
                 * handle the second case correctly.
                 */
                // Go up the workspace tree, and process each workspace that contains the
                // desired element or its reified package:
                handleElementHierarchy(modelRootNode, recurse, workspace, dateTime, false);
                if (recurse) {
                    while (workspace != null) {
                        workspace = workspace.getParentWorkspace();
                        modelRootNode = findScriptNodeByIdForWorkspace(modelId, workspace, 
                                                                       dateTime, false);
                        if (modelRootNode != null) {
                            handleElementHierarchy(modelRootNode, recurse, workspace, dateTime, false);
                        }
                        // Try the reified pkg:
                        else {
                            modelRootNode = findScriptNodeByIdForWorkspace(modelId+"_pkg", workspace, 
                                                                           dateTime, false);
                            if (modelRootNode != null) {
                                handleElementHierarchy(modelRootNode, recurse, workspace, dateTime, false);
                            }
                        }
                    } // ends while loop
                }  // ends if (recurse)
            }
            
            handleElements(dateTime);
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
	 * Build up the element hierarchy from the specified root.  This was extended
	 * to work correctly with workspaces by using the source arg.  When this
	 * argument is set to true, it will look up source workspace recursively also
	 * to add any needed elements that were not in the current workspace.  It will
	 * never overwrite elements already found, so the current workspace
	 * must be processed first.
	 * 
	 * @param root		Root node to get children for
	 * @param workspace 
	 * @param dateTime 
	 * @param source Set to true to look up nodes in source workspaces if needed
	 * @throws JSONException
	 */
	protected void handleElementHierarchy(EmsScriptNode root, boolean recurse,
	                                      WorkspaceNode workspace, Date dateTime,
	                                      boolean source)
	                                              throws JSONException {
	    
		JSONArray array = new JSONArray();
		
		// don't return any elements
		if (!root.exists()) {
		    return;
		}
		
		// add root element to elementsFound if its not already there (if it's there, it's probably because the root is a reified pkg node)
		String sysmlId = (String)root.getProperty(Acm.ACM_ID);
		if (!elementsFound.containsKey(sysmlId)) {
		    // dont add reified packages
		    if (!((String)root.getProperty(Acm.CM_NAME)).contains("_pkg") &&
		        !root.isPropertyOwnedValueSpecification()) {
		        elementsFound.put(sysmlId, root);
		    }
		}

		if (recurse) {
			// Find all the children, recurse or add to array as needed.
            // TODO: figure out why the child association creation from the
            // reification isn't being picked up
		    String rootName = (String)root.getProperty(Acm.CM_NAME);
		    EmsScriptNode reifiedNode = root;
		    if (!rootName.contains("_pkg")) {
		        reifiedNode = findScriptNodeById( rootName + "_pkg", workspace,
		                                          dateTime, false );
		        
		        if (reifiedNode != null) {
                    handleElementHierarchy( reifiedNode, recurse, workspace,
                                            dateTime, source );
		        } // TODO -- REVIEW -- Warning or error?
		    }
		    
		    // Handle all the children in this workspace:
			for (ChildAssociationRef assoc: root.getChildAssociationRefs()) {
			    NodeRef childRef = assoc.getChildRef();
			    NodeRef vChildRef = NodeUtil.getNodeRefAtTime( childRef, workspace, dateTime );
                if ( vChildRef == null ) {
                    // this doesn't elicit a not found response
                    log( LogLevel.WARNING,
                         "Element " + childRef
                         + ( dateTime == null ? "" : " at " + dateTime ) + " not found");
			        continue;
			    }
                childRef = vChildRef;
                EmsScriptNode child =
                        new EmsScriptNode( childRef, services, response );
                if ( checkPermissions( child, PermissionService.READ ) ) {
                    if (child.exists() && !child.isPropertyOwnedValueSpecification()) {
                        boolean elementFolder = false;
                        if ( child.getTypeShort().equals( Acm.ACM_ELEMENT_FOLDER ) ) {
                            handleElementHierarchy( child, recurse, workspace,
                                                    dateTime, source );
                            elementFolder = true;
                        } else {
                            String value = (String)child.getProperty( Acm.ACM_ID );
                            if ( value != null ) {
                                array.put( value );
                                if (!elementsFound.containsKey( value )) {
                                    elementsFound.put( value, child );
                                }
                                // add empty hierarchies as well
                                if (!elementHierarchy.has( value )){
                                    elementHierarchy.put( value, new JSONArray() );
                                }
                            }
                        }
                        
                    } // ends if (child.exists() && !child.isPropertyOwnedValueSpecification())
                } // ends if ( checkPermissions( child, PermissionService.READ ) ) 
			}
			
	    	// Handle the reified package for the source workspace:
			if (reifiedNode != null && source) {
			    EmsScriptNode sourceNode = null;
			    
                // Easiest way to find the parent workspace node is the source
                // property:
                NodeRef sourceRef = (NodeRef)reifiedNode.getProperty( "ems:source" );
                
                if (sourceRef != null) {
                    sourceNode = new EmsScriptNode(sourceRef, services, response);
                    
                    if (sourceNode != null) {
                        handleElementHierarchy( sourceNode, recurse, sourceNode.getWorkspace(),
                                                dateTime, source );
                    }
                }
                
                // Was not able to find the node using the source property, so lets try
                // to find it by tracing up the workspace tree:
                if (sourceNode == null && reifiedNode.getWorkspace() != null) {
                    sourceNode = findScriptNodeById( reifiedNode.getSysmlId(), 
                                                     reifiedNode.getParentWorkspace(),
                                                     dateTime, false );
                    
                    if (sourceNode != null) {
                        handleElementHierarchy( sourceNode, recurse, sourceNode.getWorkspace(),
                                                dateTime, source );
                    }
                }
                
            } 
			
			// if there were any children add them to the hierarchy object
			String key = (String)root.getProperty(Acm.ACM_ID);
			if (root.getTypeShort().equals(Acm.ACM_ELEMENT_FOLDER) && key == null) {
				// TODO this is temporary? until we can get sysml:id from Element Folder?
				key = root.getProperty(Acm.CM_NAME).toString().replace("_pkg", "");
			}
			
			// Add the array to the elemenHierarchy if needed:
            if (!elementHierarchy.has( key )){
                elementHierarchy.put(key, array);
            }
            else {
                JSONArray oldArray = elementHierarchy.getJSONArray(key);
                boolean foundObject;
                Object obj;
                for (int i = 0; i < array.length(); i++) {
                    obj = array.get( i );
                    foundObject = false;
                    if (obj != null) {
                        for (int j = 0; j < oldArray.length(); j++) {
                            if (obj.equals(oldArray.get(j))) {
                                foundObject = true;
                                break;
                            }
                        }
                        if (!foundObject) {
                            oldArray.put( obj );
                        }
                    }
                }
            }
            
		}  // ends if (recurse)
	}
	
	/**
	 * Build up the element JSONObject
	 * @throws JSONException
	 */
	protected void handleElements(Date dateTime) throws JSONException {
		for (String id: elementsFound.keySet()) {
			EmsScriptNode node = elementsFound.get(id);

			if (checkPermissions(node, PermissionService.READ)){ 
                elements.put(node.toJSONObject(dateTime));
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
