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
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.Acm.JSON_TYPE_FILTER;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

public class ViewGet extends AbstractJavaWebScript {
    protected boolean gettingDisplayedElements = false;
    protected boolean gettingContainedViews = false;

    // injected via spring configuration
    protected boolean isViewRequest = false;

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
    
        EmsScriptNode view = findScriptNodeById(viewId, dateTime);
        if (view == null) {
            log(LogLevel.ERROR, "View not found with id: " + viewId + " at " + dateTime + ".\n", HttpServletResponse.SC_NOT_FOUND);
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
        System.out.println("Got raw id = " + viewId);
        return viewId;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
        // default recurse=true but recurse only applies to displayed elements and contained views
        boolean recurse = checkArgEquals(req, "recurse", "false") ? false : true;

        JSONArray viewsJson = new JSONArray();
        if (validateRequest(req, status)) {
            String viewId = getIdFromRequest( req );
            gettingDisplayedElements = isDisplayedElementRequest( req );
            if ( !gettingDisplayedElements ) {
                gettingContainedViews = isContainedViewRequest( req );
            } 
            System.out.println("viewId = " + viewId);
            
            // get timestamp if specified
            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        
            try {
                handleView(viewId, viewsJson, recurse, dateTime);
            } catch ( JSONException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject json = new JSONObject();
                json.put(gettingDisplayedElements ? "elements" : "views", viewsJson);
                model.put("res", json.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
                log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                model.put("res", response.toString());
                e.printStackTrace();
            }
        } else {
            model.put("res", response.toString());
        }

        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }


    private void handleView(String viewId, JSONArray viewsJson, boolean recurse, Date dateTime) throws JSONException {
        EmsScriptNode view = findScriptNodeById(viewId, dateTime);

        if (view == null) {
            log( LogLevel.ERROR, "View not found with ID: " + viewId,
                 HttpServletResponse.SC_NOT_FOUND );
        }

        if (checkPermissions(view, PermissionService.READ)) {
            try {
                View v = new View(view);
                if ( gettingDisplayedElements ) {
                    System.out.println("+ + + + + gettingDisplayedElements");
                    // TODO -- need to use recurse flag!
                    Collection< EmsScriptNode > elems = v.getDisplayedElements();
                    elems = NodeUtil.getVersionAtTime( elems, dateTime );
                    for ( EmsScriptNode n : elems ) {
                        viewsJson.put( n.toJSONObject( JSON_TYPE_FILTER.ELEMENT, dateTime ) );
                    }
                } else if ( gettingContainedViews ) {
                    System.out.println("+ + + + + gettingContainedViews");
                    Collection< EmsScriptNode > elems = v.getContainedViews( recurse, dateTime, null );
                    for ( EmsScriptNode n : elems ) {
                        viewsJson.put( n.toJSONObject( JSON_TYPE_FILTER.VIEW, dateTime ) );
                    }
                } else {
                    System.out.println("+ + + + + just the view");
                    viewsJson.put( view.toJSONObject( JSON_TYPE_FILTER.VIEW, dateTime ) );
                }
            } catch ( JSONException e ) {
                log( LogLevel.ERROR, "Could not create views JSON array",
                     HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
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
