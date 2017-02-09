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
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.Collection;
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

public class ViewGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ViewGet.class);

    protected boolean gettingDisplayedElements = false;
    protected boolean gettingContainedViews = false;

    // injected via spring configuration
    protected boolean isViewRequest = false;

    protected boolean prettyPrint = true;

    public ViewGet() {
        super();
    }

    public ViewGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        String viewId = getIdFromRequest(req);
        if (!checkRequestVariable(viewId, "id")) {
            return false;
        }

        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        
        WorkspaceNode workspace = getWorkspace( req );

        // see if prettyPrint default is overridden and change
        prettyPrint = getBooleanArg( req, "pretty", prettyPrint );
    
        EmsScriptNode view = findScriptNodeById(viewId, workspace, dateTime, false);
        if (view == null) {
            String msg = "View not found with id: "+viewId;
            if (dateTime != null) {
                msg = msg + " at " + dateTime + " .\n";
            }
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, msg);
            return false;
        }

        if (!checkPermissions(view, PermissionService.READ)) {
            return false;
        }

        return true;
    }

    protected static String getRawViewId( WebScriptRequest req ) {
        if ( req == null || req.getServiceMatch() == null ||
             req.getServiceMatch().getTemplateVars() == null ) {
            return null; 
        }
        String viewId = req.getServiceMatch().getTemplateVars().get("id");
        if ( viewId == null ) {
            viewId = req.getServiceMatch().getTemplateVars().get("modelid");
        }
        if ( viewId == null ) {
            viewId = req.getServiceMatch().getTemplateVars().get("elementid");
        }
        if (logger.isDebugEnabled()) logger.debug("Got raw id = " + viewId);
        return viewId;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ViewGet instance = new ViewGet();
        instance.setServices( getServices() );
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
    }
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        Timer timer = new Timer();
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader( user, logger, req );

        //clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
//        TODO: REMOVE THIS CODE
//            if (checkMmsVersions) {
//                if(compareMmsVersions(req, getResponse(), getResponseStatus()));{
//                    model.put("res", createResponseJson());
//                    return model;
//                }
//            }
            // default recurse=false but recurse only applies to displayed elements and contained views
            boolean recurse = getBooleanArg(req, "recurse", false);
            // default generate=false - generation with viewpoints takes a long time
            boolean generate = getBooleanArg( req, "generate", false );
            boolean evaluate = getBooleanArg( req, "evaluate", false );
            generate = generate || evaluate;
            boolean isQualified = getBooleanArg(req, "extended", false);

        JSONArray viewsJson = new JSONArray();
        if (validateRequest(req, status)) {
            String viewId = getIdFromRequest( req );
            gettingDisplayedElements = isDisplayedElementRequest( req );
            if ( !gettingDisplayedElements ) {
                gettingContainedViews = isContainedViewRequest( req );
            } 
            if (logger.isDebugEnabled()) logger.debug("viewId = " + viewId);

            // get timestamp if specified
            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            WorkspaceNode workspace = getWorkspace( req );

            try {
                handleView(viewId, viewsJson, generate, recurse, workspace, dateTime, isQualified);
            } catch ( JSONException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject json = NodeUtil.newJsonObject();
                json.put(gettingDisplayedElements ? "elements" : "views", viewsJson);
                if (!Utils.isNullOrEmpty(response.toString())) json.put("message", response.toString());
                if ( prettyPrint ) model.put("res", NodeUtil.jsonToString( json, 4 ));
                else model.put("res", NodeUtil.jsonToString( json )); 
            } catch (JSONException e) {
                e.printStackTrace();
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON creation error");
                model.put("res", createResponseJson());
                e.printStackTrace();
            }
        } else {
            model.put("res", createResponseJson());
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    private void handleView( String viewId, final JSONArray viewsJson,
                             boolean generate, boolean recurse,
                             final WorkspaceNode workspace, final Date dateTime,
                             final boolean isQualified)
                                     throws JSONException {
        EmsScriptNode view = findScriptNodeById(viewId, workspace, dateTime, false);

        if (view == null) {
            log( Level.ERROR,
                 HttpServletResponse.SC_NOT_FOUND, "View not found with ID: %s", viewId);
        }

        if (checkPermissions(view, PermissionService.READ)) {
            try {
                View v = new View(view);
                v.setGenerate( generate );
                v.setRecurse( recurse );

                if ( gettingDisplayedElements ) {
                    if (logger.isDebugEnabled()) logger.debug("+ + + + + gettingDisplayedElements");
                    // TODO -- need to use recurse flag!
                    Collection< EmsScriptNode > elems =
                            v.getDisplayedElements( workspace, dateTime,
                                                    generate, recurse, null );
                    for ( final EmsScriptNode n : elems ) {

                        new EmsTransaction(getServices(), getResponse(),
                                           getResponseStatus(), false, true) {
                            @Override
                            public void run() throws Exception {
                                viewsJson.put( n.toJSONObject( workspace, dateTime, isQualified ) );
                            }
                        };

                    }
                } else if ( gettingContainedViews ) {
                    if (logger.isDebugEnabled()) logger.debug("+ + + + + gettingContainedViews");
                    Collection< EmsScriptNode > elems =
                            v.getContainedViews( recurse, workspace, dateTime,
                                                 null );
                    elems.add( view );
                    for ( final EmsScriptNode n : elems ) {
                        new EmsTransaction(getServices(), getResponse(),
                                           getResponseStatus(), false, true) {
                            @Override
                            public void run() throws Exception {
                                viewsJson.put( n.toJSONObject( workspace, dateTime, isQualified ) );
                            }
                        };
                    }
                } else {
                    if (logger.isDebugEnabled()) logger.debug("+ + + + + just the view");
                    viewsJson.put( view.toJSONObject( workspace, dateTime, isQualified ) );
                }
            } catch ( JSONException e ) {
                log( Level.ERROR,
                     HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create views JSON array");
                e.printStackTrace();
            }
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
