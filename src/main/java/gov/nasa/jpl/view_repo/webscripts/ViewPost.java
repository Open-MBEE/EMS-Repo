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

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

        try {
            JSONObject json = //JSONObject.make( 
                    (JSONObject)req.parseContent();// );
			updateViews(json, workspace);
		} catch (JSONException e) {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "JSON parse exception: %s", e.getMessage());
			e.printStackTrace();
		}

        status.setCode(responseStatus.getCode());
		model.put("res", createResponseJson());

		printFooter();
		return model;
	}

	
	private void updateViews(JSONObject jsonObject, WorkspaceNode workspace) throws JSONException {
	    Date start = new Date();
	    Map<String, EmsScriptNode> elements = new HashMap<String, EmsScriptNode>();
	    
	    // actual business logic, everything else is to handle commits
	    if (jsonObject.has("views")) {
			JSONArray viewsJson = jsonObject.getJSONArray("views");

			for (int ii = 0; ii < viewsJson.length(); ii++) {
			    updateView(viewsJson, ii, workspace, elements);
			}
		}

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
            projectId = elements.get( 0 ).getProjectId(workspace);
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

		if (checkPermissions(view, PermissionService.WRITE)) {
		    view.createOrUpdateAspect(Acm.ACM_VIEW);
		    view.ingestJSON(viewJson);
		    elements.put( id, view );
		}
	}
}
