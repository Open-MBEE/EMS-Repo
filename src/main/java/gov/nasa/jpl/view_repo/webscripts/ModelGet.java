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

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

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

    @Override
	protected void clearCaches() {
		super.clearCaches();
		elementHierarchy = new JSONObject();
		elements = new JSONArray();
		elementsFound = new HashMap<String, EmsScriptNode>();
	}

	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String modelId = req.getServiceMatch().getTemplateVars().get("modelid");
		if (modelId == null) {
			modelId = req.getServiceMatch().getTemplateVars().get("elementid");
		}
		
		if (!checkRequestVariable(modelId, "modelid")) {
			log(LogLevel.ERROR, "Element id not specified.\n", HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		
        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        
		EmsScriptNode modelRootNode = findScriptNodeById(modelId, dateTime);
		if (modelRootNode == null) {
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
	    printHeader( req );

		clearCaches();
        
		Map<String, Object> model = new HashMap<String, Object>();
		ModelGet instance = new ModelGet(repository, services);
		// make sure to pass down view request flag to instance
		instance.setIsViewRequest(isViewRequest);

		JSONArray elementsJson = new JSONArray();
		if (validateRequest(req, status)) {
		    elementsJson = instance.handleRequest(req);
		    appendResponseStatusInfo(instance);
		}
		
		JSONObject top = new JSONObject();
		try {
		    if (elementsJson.length() > 0) {
		        top.put("elements", elementsJson);
	            model.put("res", top.toString(4));
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

		return model;
	}
	
	/**
	 * Wrapper for handling a request and getting the appropriate JSONArray of elements
	 * @param req
	 * @return
	 */
	private JSONArray handleRequest(WebScriptRequest req) {
        try {
            String modelId = req.getServiceMatch().getTemplateVars().get("modelid");
            if (modelId == null) {
                modelId = req.getServiceMatch().getTemplateVars().get("elementid");
            }

            // get timestamp if specified
            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
            
            System.out.println("modelId = " + modelId );
            EmsScriptNode modelRootNode = findScriptNodeById(modelId, dateTime);
            System.out.println("modelRootNode = " + modelRootNode );
            
            if ( modelRootNode == null ) {
                    log( LogLevel.ERROR,
                         "Element " + modelId
                         + ( dateTime == null ? "" : " at " + dateTime ) + " not found",
                         HttpServletResponse.SC_NOT_FOUND );
            }
            
            // recurse default is false
            boolean recurse = checkArgEquals(req, "recurse", "true") ? true : false;
            
            if (isViewRequest) {
                handleViewHierarchy(modelRootNode, recurse, dateTime);
            } else {
                handleElementHierarchy(modelRootNode, recurse, dateTime);
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
	                                   Date dateTime) throws JSONException {
		Object allowedElements = root.getProperty(Acm.ACM_ALLOWED_ELEMENTS);
		if (allowedElements != null) {
			JSONArray childElementJson = new JSONArray(allowedElements.toString());
			for (int ii = 0; ii < childElementJson.length(); ii++) {
				String id = childElementJson.getString(ii);
				EmsScriptNode childElement = findScriptNodeById(id, dateTime);
				
				// TODO Need to report that allowedElements can't be found
				if (childElement != null) {
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
						EmsScriptNode childView = findScriptNodeById(id, dateTime);
						if (childView != null) {
						    if (checkPermissions(childView, PermissionService.READ)) {
						        handleViewHierarchy(childView, recurse, dateTime);
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
	 * Build up the element hierarchy from the specified root
	 * @param root		Root node to get children for
	 * @param dateTime 
	 * @throws JSONException
	 */
	protected void handleElementHierarchy(EmsScriptNode root, boolean recurse, Date dateTime) throws JSONException {
		JSONArray array = new JSONArray();
		
		// add root element to elementsFound if its not already there (if it's there, it's probably because the root is a reified pkg node)
		String sysmlId = (String)root.getProperty(Acm.ACM_ID);
		if (!elementsFound.containsKey(sysmlId)) {
		    // dont add reified packages
		    if (!((String)root.getProperty(Acm.CM_NAME)).contains("_pkg")) {
		        elementsFound.put((String)root.getProperty(Acm.ACM_ID), root);
		    }
		}

		if (recurse) {
			// find all the children, recurse or add to array as needed
		    // TODO: figure out why the child association creation from the reification isn't being picked up
		    String rootName = (String)root.getProperty(Acm.CM_NAME);
		    if (!rootName.contains("_pkg")) {
		        EmsScriptNode reifiedNode = findScriptNodeById(rootName + "_pkg", dateTime);
		        if (reifiedNode != null) {
		            handleElementHierarchy(reifiedNode, recurse, dateTime);
		        } // TODO -- REVIEW -- Warning or error?
		    }
			for (ChildAssociationRef assoc: root.getChildAssociationRefs()) {
			    NodeRef childRef = assoc.getChildRef();
			    NodeRef vChildRef = NodeUtil.getNodeRefAtTime( childRef, dateTime );
                if ( vChildRef == null ) {
                    log( LogLevel.WARNING,
                         "Element " + childRef
                         + ( dateTime == null ? "" : " at " + dateTime ) + " not found",
                         HttpServletResponse.SC_NOT_FOUND );
			        continue;
			    }
                childRef = vChildRef;
                EmsScriptNode child =
                        new EmsScriptNode( childRef, services, response );
                if ( checkPermissions( child, PermissionService.READ ) ) {
                    if ( child.getTypeShort().equals( Acm.ACM_ELEMENT_FOLDER ) ) {
                        handleElementHierarchy( child, recurse, dateTime );
                    } else {
                        String value = (String)child.getProperty( Acm.ACM_ID );
                        if ( value != null ) {
                            array.put( value );
                            elementsFound.put( value, child );
                            // add empty hierarchies as well
                            elementHierarchy.put( value, new JSONArray() );
                        }
                    }
                }
			}
	    	
			// if there were any children add them to the hierarchy object
			String key = (String)root.getProperty(Acm.ACM_ID);
			if (root.getTypeShort().equals(Acm.ACM_ELEMENT_FOLDER) && key == null) {
				// TODO this is temporary? until we can get sysml:id from Element Folder?
				key = root.getProperty(Acm.CM_NAME).toString().replace("_pkg", "");
			}
			
			elementHierarchy.put(key, array);
		}
	}
	
	/**
	 * Build up the element JSONObject
	 * @throws JSONException
	 */
	protected void handleElements(Date dateTime) throws JSONException {
		for (String id: elementsFound.keySet()) {
			EmsScriptNode node = elementsFound.get(id);

			if (checkPermissions(node, PermissionService.READ)){ 
                elements.put(node.toJSONObject(Acm.JSON_TYPE_FILTER.ELEMENT, dateTime));
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
