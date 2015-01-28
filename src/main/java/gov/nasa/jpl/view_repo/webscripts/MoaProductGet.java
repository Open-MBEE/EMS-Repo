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
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Mother-Of-All-Product gets everything about a product in one big blob.
 * @author cinyoung
 *
 */
@Deprecated
public class MoaProductGet extends AbstractJavaWebScript {
    public MoaProductGet() {
        super();
    }


	public MoaProductGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String productId = req.getServiceMatch().getTemplateVars().get("id");
		if (!checkRequestVariable(productId, "id")) {
			return false;
		}

        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

		EmsScriptNode product = findScriptNodeById(productId, workspace, dateTime, false);
		if (product == null) {
			log(LogLevel.ERROR, "Product not found with id: " + productId + ".\n", HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		if (!checkPermissions(product, PermissionService.READ)) {
			return false;
		}

		return true;
	}

	@Override
	protected void clearCaches() {
		super.clearCaches();
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
	    MoaProductGet instance = new MoaProductGet( repository, getServices() );
        instance.setServices( getServices() );
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status, Cache cache ) {

        printHeader( req );

        Map<String, Object> model = new HashMap<String, Object>();

        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

		String productId = null;
		JSONObject json = null;
		if (validateRequest(req, status)) {
            productId = req.getServiceMatch().getTemplateVars().get("id");
            json = generateMoaProduct( productId, req.getContextPath(),
                                       workspace, dateTime );
		}

		if (responseStatus.getCode() == HttpServletResponse.SC_OK && json != null) {
	        EmsScriptNode product = findScriptNodeById(productId, workspace, dateTime, false);

		    String jsonString = json.toString();
            model.put("res", jsonString);
            if (productId != null) {
                model.put("title", product.getProperty(Acm.ACM_NAME));
                model.put("siteTitle", product.getSiteTitle());
                model.put("siteName", product.getSiteName());
            }
        } else {
            model.put("res", response.toString());
            model.put("title", "Could not load");
            model.put("siteTitle", "ERROR product not found");
            model.put("siteName", "");
        }

		status.setCode(responseStatus.getCode());

        printFooter();

        return model;
	}

	/**
	 * Public utility for generating the Mother Of All Products
	 * @param productId    Product ID to generate MOA listing for
	 * @param contextPath  Context path needed for the snapshot URLs
	 * @param dateTime
	 * @return             JSON object of the entire product
	 */
    public JSONObject generateMoaProduct( String productId, String contextPath,
                                          WorkspaceNode workspace,
                                          Date dateTime ) {
        clearCaches();
        JSONObject productsJson = null;
        try {
            productsJson = handleProduct(productId, workspace, dateTime);
            handleSnapshots(productId, contextPath, productsJson, workspace, dateTime);
            if (!Utils.isNullOrEmpty(response.toString())) productsJson.put("message", response.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return productsJson;
    }

    /**
     * Create the MOA Product JSON
     * @param productId
     * @param dateTime
     * @throws JSONException
     */
	private JSONObject handleProduct(String productId, WorkspaceNode workspace, Date dateTime) throws JSONException {
		EmsScriptNode product = findScriptNodeById(productId, workspace, dateTime, false);
		JSONObject productsJson = null;
		JSONArray viewsJson = new JSONArray();
		JSONArray elementsJson = new JSONArray();
		if (product == null) {
			log(LogLevel.ERROR, "Product not found with ID: " + productId, HttpServletResponse.SC_NOT_FOUND);
		}

		if (checkPermissions(product, PermissionService.READ)){
		    JSONObject object = product.toJSONObject(dateTime);
		    productsJson = new JSONObject(object, JSONObject.getNames(object));

		    if (object.has(Acm.JSON_VIEW_2_VIEW)) {
		        Object o = object.get( Acm.JSON_VIEW_2_VIEW );
		        JSONArray jarr = null;
		        if ( o instanceof String ) {
                    jarr = new JSONArray( (String)o );
		        } else {
		            jarr = object.getJSONArray(Acm.JSON_VIEW_2_VIEW);
		        }
                handleViews(jarr, viewsJson, elementsJson, workspace, dateTime);
		    }

		    productsJson.put("views", viewsJson);
		    productsJson.put("elements", elementsJson);
		}

		return productsJson;
	}

	/**
	 * Get the snapshots associated
	 * @param productId
	 * @param contextPath
	 * @param dateTime
	 * @throws JSONException
	 */
	private void handleSnapshots(String productId, String contextPath,
	                             JSONObject productsJson, WorkspaceNode workspace,
	                             Date dateTime) throws JSONException {
	    EmsScriptNode product = findScriptNodeById(productId, workspace, dateTime, false);

        JSONArray snapshotsJson = new JSONArray();
        List<EmsScriptNode> snapshotsList =
                product.getTargetAssocsNodesByType("view2:snapshots", workspace,
                                                   null);

        Collections.sort(snapshotsList, new EmsScriptNode.EmsScriptNodeComparator());
        for (EmsScriptNode snapshot: snapshotsList) {
            if ( snapshot == null ) continue;
            if ( dateTime != null ) {
                EmsScriptNode snapshotV = snapshot.getVersionAtTime( dateTime );
                if ( snapshotV != null ) {
                    String msg = "Error! Snapshot " + snapshot + " did not exist at " + dateTime + ".\n";
                    if ( getResponse() == null || this.getResponseStatus() == null ) {
                        Debug.error( msg );
                    } else {
                        getResponse().append( msg );
                        getResponseStatus().setCode( HttpServletResponse.SC_BAD_REQUEST,
                                                     msg );
                    }
                }
                snapshot = snapshotV;
            }
            if ( snapshot == null ) {
                continue;
            }

            String id = snapshot.getSysmlId();
            Date date = snapshot.getLastModified( dateTime );

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id.substring(0, id.lastIndexOf("_")));
            jsonObject.put("created", EmsScriptNode.getIsoTime(date));
            jsonObject.put("url", contextPath + "/service/snapshots/" + id);
            jsonObject.put("creator", snapshot.getProperty("cm:modifier"));
            jsonObject.put("tag", SnapshotGet.getConfigurationSet(snapshot,
                                                                          workspace,
                                                                          dateTime));
            snapshotsJson.put(jsonObject);
        }
        productsJson.put("snapshots", snapshotsJson);
    }

    private void handleViews( JSONArray view2view, JSONArray viewsJson,
                              JSONArray elementsJson, WorkspaceNode workspace,
                              Date dateTime ) throws JSONException {
	    Set<String> viewIds = new HashSet<String>();
	    Set<String> elementIds = new HashSet<String>();

	    // find all the views
	    for (int ii = 0; ii < view2view.length(); ii++) {
	        JSONObject view = view2view.getJSONObject(ii);
	        viewIds.add(view.getString(Acm.JSON_ID));
	        if (view.has(Acm.JSON_CHILDREN_VIEWS)) {
        	        JSONArray childrenViews = view.getJSONArray(Acm.JSON_CHILDREN_VIEWS);
        	        for (int jj = 0; jj < childrenViews.length(); jj++) {
        	            viewIds.add(childrenViews.getString(jj));
        	        }
	        }
	    }

	    // insert all the views and find all the elements
	    for (String viewId: viewIds) {
	        EmsScriptNode view = findScriptNodeById(viewId, workspace, dateTime, false);
	        if (view != null && checkPermissions(view, PermissionService.READ)) {
        	        JSONObject viewJson = view.toJSONObject(dateTime);

        	        // add any related comments as part of the view
        	        JSONArray commentsJson = new JSONArray();
                List< EmsScriptNode > commentList =
                        view.getSourceAssocsNodesByType( Acm.ACM_ANNOTATED_ELEMENTS,
                                                         workspace, dateTime );// new ArrayList<EmsScriptNode>();
        	        Collections.sort(commentList, new EmsScriptNode.EmsScriptNodeComparator());
        	        for (EmsScriptNode comment: commentList) {
        	            commentsJson.put(comment.toJSONObject(dateTime));
        	        }
        	        viewJson.put("comments", commentsJson);

        	        if (viewJson.has(Acm.JSON_ALLOWED_ELEMENTS)) {
                        JSONArray allowedElements = null;
                        Object o = viewJson.get( Acm.JSON_ALLOWED_ELEMENTS );
                        if ( o instanceof String ) {
                            allowedElements = new JSONArray( (String)o );
                        } else {
                            allowedElements = viewJson.getJSONArray(Acm.JSON_ALLOWED_ELEMENTS);
                        }
            	        //JSONArray allowedElements = viewJson.getJSONArray(Acm.JSON_ALLOWED_ELEMENTS);
            	        for (int ii = 0; ii < allowedElements.length(); ii++) {
            	            elementIds.add(allowedElements.getString(ii));
            	        }
        	        }

                viewsJson.put(viewJson);
	        }
	    }

	    // insert all the elements
	    for (String elementId: elementIds) {
	        EmsScriptNode element = findScriptNodeById(elementId, workspace, dateTime, false);
	        if (element != null && checkPermissions(element, PermissionService.READ)) {
	            JSONObject elementJson = element.toJSONObject(dateTime);
	            elementsJson.put(elementJson);
	        }
	    }
	}
}
