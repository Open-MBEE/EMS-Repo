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
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ViewPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ViewPost.class);
    
	public ViewPost() {
	    super();
	}

    public ViewPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// do nothing
		return false;
	}


	@Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ViewPost instance = new ViewPost(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

	@Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

		//clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();

        WorkspaceNode workspace = getWorkspace( req );
        
        String viewId = req.getServiceMatch().getTemplateVars().get( "viewId" );

        // get timestamp if specified
        String parentId = req.getParameter( "parent" );

        try {
            JSONObject json = //JSONObject.make( 
                    (JSONObject)req.parseContent();// );
            updateViews(viewId, parentId, json, workspace);
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "JSON parse exception: %s", e.getMessage());
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());
		model.put("res", createResponseJson());

		printFooter();
		return model;
	}

//<<<<<<< HEAD
//	
//	private void updateViews(JSONObject jsonObject, WorkspaceNode workspace) throws JSONException {
//	    Date start = new Date();
//	    Map<String, EmsScriptNode> elements = new HashMap<String, EmsScriptNode>();
//	    
//	    // actual business logic, everything else is to handle commits
//	    if (jsonObject.has("views")) {
//			JSONArray viewsJson = jsonObject.getJSONArray("views");
//=======
//    
    /**
     * If the view is being added to a new parent, then this function should be
     * called. This function will add or update views. If the view pre-exists,
     * is not changing its parent view, and is only updating other properties or
     * the order, ModelPost should be fine.
     * 
     * Add or update views
     * @param parentId 
     * @param viewId 
     * 
     * @param jsonObject
     * @param workspace
     * @throws JSONException
     */
    protected void updateViews( String viewId, String parentId, JSONObject jsonObject,
                                WorkspaceNode workspace ) throws JSONException {
        Date start = new Date();
        Map<String, EmsScriptNode> elements = new HashMap<String, EmsScriptNode>();
        
        boolean viewIdInUrl = Utils.isNullOrEmpty( viewId );
        boolean parentIdInUrl = Utils.isNullOrEmpty( parentId );
        
        Map<String, String> owners = new HashMap< String, String >();
        Map<String, Set<String> > elementOwnedAttributes = new HashMap< String, Set<String> >();
        //Map<String, String> viewOwners = new HashMap< String, String >();
        Map<String, Set<String> > viewOwners = new HashMap< String, Set<String> >();
        Map<String, Set<String> > viewChildViews = new HashMap< String, Set<String> >();
        Map<String, JSONObject> viewsInJson = new HashMap< String, JSONObject >();
        
        // Pull stuff out of json
        boolean viewInJson = false;
        boolean gotJson = false;
        if ( jsonObject != null ) {
            gotJson = true;
            JSONArray elementsJson = jsonObject.optJSONArray( "elements" );
            if ( elementsJson != null && elementsJson.length() > 0 ) {
                for ( int i = 0; i < elementsJson.length(); ++i ) {
                    JSONObject elementJson = elementsJson.optJSONObject( i );
                    if ( elementJson != null ) {
                        JSONObject spec = elementJson.optJSONObject( "specialization" );
                        if ( spec != null ) {
                            String type = spec.optString( "type" );
                            if ( "View".equals( type ) || "Product".equals( type ) ) {
                                viewInJson = true;
                                
                                String id = elementJson.optString( "sysmlid" );
                                if ( Utils.isNullOrEmpty( id ) ) {
                                    id = NodeUtil.createId( getServices() );
                                }
                                viewsInJson.put( id, elementJson );
                                
                                String owner = elementJson.optString( "owner" );
                                if ( !Utils.isNullOrEmpty( owner ) ) {
                                    owners.put( id, owner );
                                }
                                
                                JSONArray childViewsArray = spec.optJSONArray( Acm.JSON_CHILD_VIEWS );
                                if ( childViewsArray != null ) {
                                    Set<String> childViews = new HashSet< String >();
                                    for ( int j=0; j < childViewsArray.length(); ++j ) {
                                        String childView = childViewsArray.optString( j );
                                        childViews.add( childView );
                                        Set<String> viewOwnerSet = viewOwners.get( childView );
                                        if ( viewOwnerSet == null ) {
                                            viewOwnerSet = new LinkedHashSet< String >();
                                            viewOwners.put( childView, viewOwnerSet );
                                        }
                                        viewOwnerSet.add( id );
                                    }
                                    viewChildViews.put( id, childViews );
                                }
                                
                                JSONArray ownedAttributesArray = spec.optJSONArray( Acm.JSON_OWNED_ATTRIBUTE );
                                if ( ownedAttributesArray != null ) {
                                    Set<String> ownedAttributes = new HashSet< String >();
                                    for ( int j=0; j < ownedAttributesArray.length(); ++j ) {
                                        String ownedAttribute = ownedAttributesArray.optString( j );
                                        ownedAttributes.add( ownedAttribute );
                                    }
                                    elementOwnedAttributes.put( id, ownedAttributes );
                                }
                            }
                        }
                    }
                }
            }
        }
//>>>>>>> refs/heads/viewOrdering

//<<<<<<< HEAD
//			for (int ii = 0; ii < viewsJson.length(); ii++) {
//			    updateView(viewsJson, ii, workspace, elements);
//			}
//		}
//=======
        // Initialize stuff for case where the view id is only in the URL
        if ( viewIdInUrl ) { //!Utils.isNullOrEmpty( viewIdInUrl ) ) {
            
        }
        
        // If no id is given in the URL parameters or json, create a new id.
        
        // If there is no json, or the json has a sysml id of a non-existent
        // view, create a new view.
        
        // If the json is not given for the new view, create an
        // InstanceSpecification and place it in the first parent of the view
        // that is a Package.  The contents of the new view will be an
        // InstanceValue in an Expression that references this InstanceSpecification.

        // The parent view is specified as a URL parameter or is the owner in
        // the view's json if the owner is a view. The json may include the
        // parent.  The parent need not be specified.
        
        // The view is added to the parent if the parent is specified, and the
        // view was not already added to the parent.
        
        // Add a "childViews" to the content model and output JSON: this should interpret the
        // ownedAttribute of the view/product and give back:
        //   [ {"id", childViewId, "aggregation": "composite", "shared", or "none"}
        //   , ...]
        // where
        // childViewId is the sysmlid of the propertyType of ownedAttribute
        // properties, if it's also a view/product, aggregation is the
        // aggregation of the ownedAttribute property. Ordering matters!
        
        // childViews can change if the ownedAttributes or aggregationType of a Property changes. 
        
        // Clear out view2view in the Product or keep it consistent with the
        // childViews/InstanceSpecs.
        
        // A View, v, is added to a parent, p, by creating 
        // * a composite Association, a, owning
        //   * a Property of type p, pp, (also including pp as an ownedEnd) and
        // * a Property of type v, pv, which is an ownedAttribute of p.
//>>>>>>> refs/heads/viewOrdering

	    // commit info
        setWsDiff(workspace);
	    wsDiff.setUpdatedElements( elements );
		
		Date end = new Date();
		JSONObject deltaJson = wsDiff.toJSONObject( start, end );
		String wsId = "master";
		if (workspace != null) wsId = workspace.getId();
        // FIXME: split elements by project Id - since they may not always be in same project
        String projectId = "";
        if (elements.size() > 0) {
            // make sure the following are run as admin, it's possible that workspace
            // doesn't have project and user doesn't have read permissions on parent ws
            String origUser = AuthenticationUtil.getRunAsUser();
            AuthenticationUtil.setRunAsUser("admin");
            projectId = elements.get(elements.keySet().iterator().next()).getProjectId(workspace);
            AuthenticationUtil.setRunAsUser(origUser);
        }

        CommitUtil.commit(workspace, deltaJson, "View Post", runWithoutTransactions, services, response);
		if (!CommitUtil.sendDeltas(deltaJson, wsId, projectId, source)) {
		    logger.warn( "Could not send delta" );
		}
	}


	private void updateView(JSONArray viewsJson, int index,
	                        WorkspaceNode workspace, Map<String, EmsScriptNode> elements) throws JSONException {
		JSONObject viewJson = viewsJson.getJSONObject(index);
		updateView(viewJson, workspace, elements);
	}

	private void updateView(JSONObject viewJson, WorkspaceNode workspace,  Map<String, EmsScriptNode> elements) throws JSONException {
		String id = viewJson.getString(Acm.JSON_ID);
		if (id == null) {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "view id not specified.\n");
			return;
		}

		EmsScriptNode view = findScriptNodeById(id, workspace, null, true);
		if (view == null) {
			log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "could not find view with id: %s", id);
			return;
		}

//<<<<<<< HEAD
//		if (checkPermissions(view, PermissionService.WRITE)) {
//		    view.createOrUpdateAspect(Acm.ACM_VIEW);
//		    view.ingestJSON(viewJson);
//		    elements.put( id, view );
//		}
//	}
//=======
        if (checkPermissions(view, PermissionService.WRITE)) {
            view.createOrUpdateAspect(Acm.ACM_VIEW);
            view.ingestJSON(viewJson);
            elements.put( id, view );
        }
    }
    
    
    protected static final String paragraphClassifierId =
            "_17_0_5_1_407019f_1431903758416_800749_12055";
    
    protected static final String TEMPLATE_NEW_VIEW = "{" +
            "\"elements\": [" +
                         "{" +
                             "\"specialization\": {" +
                                 "\"type\": \"View\"" +
                             "}," +
                             "\"owner\": \"OWNER_ID\"," +
                             "\"name\": \"VIEW_NAME\"," +
                             "\"documentation\": \"\"" +
                         "}" +
                     "]" +
                 "}";
    
    protected static final String TEMPLATE_INSTANCE_SPEC = "{" +
        "\"elements\":[" +
                    "{" +
                        "\"name\": \"View Documentation\"," +
                        "\"specialization\":{" +
                            "\"type\":\"InstanceSpecification\"," +
                            "\"classifier\":[" +
                                "\"" + paragraphClassifierId + "\"" +
                            "]," +
                            "\"instanceSpecificationSpecification\":{" +
                                "\"string\":{\"type\": \"Paragraph\"," +
                                           "\"sourceType\": \"reference\"," +
                                           "\"source\":\"VIEW_ID\"," +
                                           "\"sourceProperty\":\"documentation\"}," +
                                "\"type\": \"LiteralString\"" +
                            "}" +
                        "}," +
                    "\"owner\":\"test-site_no_project\"" +
                    "}" +
                "]" +
            "}";
    
    protected static final String TEMPLATE_NEW_VIEW_WITH_INSTANCE_SPEC = "{" +
        "\"elements\": [" +
            "{" +
                "\"sysmlid\": \"VIEW_ID\"," +
                "\"specialization\":{" +
                    "\"type\": \"View\"," +
                    "\"allowedElements\": []," +
                    "\"displayedElements\":[]," +
                    "\"childrenViews\": []," +
                    "\"contents\": {" +
                        "\"operand\": [" +
                            "{" +
                                "\"instance\":\"SYSMLID_FROM_RESPONSE_JSON\"," +
                                "\"type\":\"InstanceValue\"" +
                            "}" +
                        "]," +
                        "\"type\":\"Expression\"" +
                    "}" +
                "}" +
            "}" +
        "]" +
    "}";

    protected static final String TEMPLATE_POST_PARENT_PRODUCT = "{" +
        "\"elements\": [" +
                     "{" +
                         "\"sysmlid\": \"\"," +
                         "\"specialization\":{" +
                             "\"type\":\"\"," +
                             "\"allowedElements\":[" +
                                 "\"\"" +
                             "]," +
                             "\"displayedElements\":[" +
                                 "\"\"" +
                             "]," +
                             "\"view2view\":[" +
                                 "{" +
                                     "\"id\":\"\"," +
                                     "\"childrenViews\":[" +
                                         "\"\"" +
                                     "]" +
                                 "}," +
                                 "{" +
                                     "\"id\":\"\"," +
                                     "\"childrenViews\":[]" +
                                 "}" +
                             "]" +
                         "}" +
                     "}" +
                 "]" +
             "}";
    
//>>>>>>> refs/heads/viewOrdering
}
