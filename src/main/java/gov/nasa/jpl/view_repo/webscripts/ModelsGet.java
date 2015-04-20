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
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;

import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONObject;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor in /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/model.get.desc.xml
 * @author cinyoung
 *
 */
public class ModelsGet extends AbstractJavaWebScript {
    public ModelsGet() {
        super();
    }

    public ModelsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    protected boolean prettyPrint = true;

    String timestamp;
    Date dateTime;
    WorkspaceNode workspace;

    @Override
    protected void clearCaches() {
        super.clearCaches();
    }


    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // get timestamp if specified
        timestamp = req.getParameter( "timestamp" );
        dateTime = TimeUtils.dateFromTimestamp( timestamp );

        workspace = getWorkspace( req );
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

        return true;
    }


    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
                                              Status status, Cache cache) {
        ModelsGet instance = new ModelsGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
                                                  Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();

        JSONArray elementsJson = new JSONArray();
        if (validateRequest(req, status)) {
            try {
                elementsJson = handleRequest(req);
            } catch ( JSONException e ) {
                log(LogLevel.ERROR, "Malformed JSON request", HttpServletResponse.SC_BAD_REQUEST);
                e.printStackTrace();
            }
        }


        JSONObject top = NodeUtil.newJsonObject();
        try {
            if (elementsJson.length() > 0) {
                top.put("elements", elementsJson);
                if (!Utils.isNullOrEmpty(response.toString())) top.put("message", response.toString());
                if ( prettyPrint ) model.put("res", NodeUtil.jsonToString( top, 4 ));
                else model.put("res", NodeUtil.jsonToString( top ));
            } else {
                log(LogLevel.WARNING, "No elements found",
                    HttpServletResponse.SC_NOT_FOUND);
                model.put("res", createResponseJson());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());

        return model;
    }

    /**
     * Wrapper for handling a request and getting the appropriate JSONArray of elements
     * @param req
     * @return
     */
    private JSONArray handleRequest(WebScriptRequest req) throws JSONException {
        JSONObject requestJson = //JSONObject.make( 
                (JSONObject)req.parseContent();// );
        JSONArray elementsFoundJson = new JSONArray();

        JSONArray elementsToFindJson;
        elementsToFindJson = requestJson.getJSONArray( "elements" );

        for (int ii = 0; ii < elementsToFindJson.length(); ii++) {
            String id = elementsToFindJson.getJSONObject( ii ).getString( "sysmlid" );
            EmsScriptNode node = NodeUtil.findScriptNodeById( id, workspace, dateTime, false, services, response );
            if (node != null) {
                try {
                    elementsFoundJson.put( node.toJSONObject( workspace, dateTime ) );
                } catch (JSONException e) {
                    log(LogLevel.ERROR, "Could not create JSON", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    e.printStackTrace();
                }
            }
        }

        return elementsFoundJson;
    }
}
