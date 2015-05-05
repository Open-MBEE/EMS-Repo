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
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor at /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/m
 * @author johnli
 *
 */

public class WorkspacesPost extends AbstractJavaWebScript{

    public WorkspacesPost() {
        super();
    }

    public WorkspacesPost(Repository repositoryHelper, ServiceRegistry registry){
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent ( req )) {
            return false;
        }
        if (!userHasWorkspaceLdapPermissions()) {
            return false;
        }
        return true;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        WorkspacesPost instance = new WorkspacesPost(repository, getServices());
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache){
        printHeader( req );
        Map<String, Object> model = new HashMap<String, Object>();
        int statusCode = HttpServletResponse.SC_OK;
        String user = AuthenticationUtil.getRunAsUser();
        JSONObject json = null;
        try{
            if(validateRequest(req, status)){
                String sourceWorkspaceParam = req.getParameter("sourceWorkspace");
                String newName = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
                String copyTime = req.getParameter("copyTime");
                Date copyDateTime = TimeUtils.dateFromTimestamp( copyTime );
                JSONObject reqJson = //JSONObject.make( 
                        (JSONObject)req.parseContent();// );
                WorkspaceNode ws = createWorkSpace(sourceWorkspaceParam, newName, copyDateTime, reqJson, user, status);
                statusCode = status.getCode();
                json = printObject(ws);
            } else {
                statusCode = responseStatus.getCode();
            }
        } catch (JSONException e) {
            log(LogLevel.ERROR, "JSON malformed\n", HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        } catch (Exception e){
            log(LogLevel.ERROR, "Internal stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
        if(json == null)
            model.put( "res", createResponseJson() );
        else
            try {
            	if (!Utils.isNullOrEmpty(response.toString())) json.put("message", response.toString());
                model.put("res", NodeUtil.jsonToString( json, 4 ));
            } catch (JSONException e) {
                e.printStackTrace();
                model.put( "res", createResponseJson() );
            }
        status.setCode(statusCode);
        printFooter();
        return model;
    }
    protected JSONObject printObject(WorkspaceNode ws) throws JSONException{
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        if (ws != null) {
            if(checkPermissions(ws, PermissionService.READ)) {
                jsonArray.put(ws.toJSONObject( ws, null ));
            }
            else {
                log(LogLevel.WARNING,"No permission to read: "+ ws.getSysmlId(),HttpServletResponse.SC_NOT_FOUND);
            }
        }
        json.put("workspaces", jsonArray);
        return json;
    }

    public WorkspaceNode createWorkSpace(String sourceWorkId, String newWorkName, Date cpyTime,
                               JSONObject jsonObject, String user, Status status) throws JSONException {
        status.setCode( HttpServletResponse.SC_OK );

        String sourceWorkspaceId = null;
        String newWorkspaceId = null;
        String workspaceName = null;
        String desc = null;
        Date copyTime = null;
        String permission = "read";  // Default is public read permission
        WorkspaceNode finalWorkspace = null;

        // If the workspace is supplied in the json object then get all parameters from there
        // and ignore any URL parameters:
        if (jsonObject != null) {

            JSONArray jarr = jsonObject.getJSONArray("workspaces");
            JSONObject wsJson = jarr.getJSONObject( 0 );  // Will only post/update one workspace
            sourceWorkspaceId = wsJson.optString( "parent", null );
            newWorkspaceId = wsJson.optString( "id", null ); // alfresco id of workspace node
            workspaceName = wsJson.optString( "name", null ); // user or auto-generated name, ems:workspace_name
            copyTime = TimeUtils.dateFromTimestamp( wsJson.optString( "branched", null ) );
            desc = wsJson.optString( "description", null );
            permission = wsJson.optString( "permission", "read" );  // "read" or "write"
        }
        // If no json object given, this is mainly for backwards compatibility:
        else {
            sourceWorkspaceId = sourceWorkId;
            workspaceName = newWorkName;   // The name is given on the URL typically, not the ID
            copyTime = cpyTime;
        }

        if( (newWorkspaceId != null && newWorkspaceId.equals( "master" )) ||
            (workspaceName != null && workspaceName.equals( "master" )) ) {
            log(LogLevel.WARNING, "Cannot change attributes of the master workspace.", HttpServletResponse.SC_BAD_REQUEST);
            status.setCode( HttpServletResponse.SC_BAD_REQUEST );
            return null;
        }

        // Only create the workspace if the workspace id was not supplied:
        if (newWorkspaceId == null) {

            WorkspaceNode srcWs =
                    WorkspaceNode.getWorkspaceFromId( sourceWorkspaceId,
                                                      services,
                                                      response, status, // false,
                                                      user );
            if (!"master".equals( sourceWorkspaceId ) && srcWs == null) {
                log(LogLevel.WARNING, "Source workspace not found.", HttpServletResponse.SC_NOT_FOUND);
                status.setCode( HttpServletResponse.SC_NOT_FOUND );
                return null;
            } else {
                EmsScriptNode folder = null;
                WorkspaceNode dstWs = WorkspaceNode.createWorkspaceFromSource(workspaceName, user, sourceWorkspaceId,
                                                                              copyTime, folder, getServices(),
                                                                              getResponse(), status, desc);

                if (dstWs != null) {
                    // keep history of the branch
                    CommitUtil.branch( srcWs, dstWs,"", true, services, response );                    
                    finalWorkspace = dstWs;
                }
            }
        }
        // Otherwise, update the workspace:
        else {

            // First try and find the workspace by id:
            WorkspaceNode existingWs =
                    WorkspaceNode.getWorkspaceFromId( newWorkspaceId, services,
                                                      response, status, // false,
                                                      user );

            // Workspace was found, so update it:
            if ( existingWs != null ) {

                if (existingWs.isDeleted()) {
                    existingWs.removeAspect( "ems:Deleted" );
                    log(LogLevel.INFO, "Workspace undeleted and modified", HttpServletResponse.SC_OK);
                } else {
                    log(LogLevel.INFO, "Workspace is modified", HttpServletResponse.SC_OK);
                }

                // Update the name/description:
                // Note: allowing duplicate workspace names, so no need to check for other
                //       workspaces with the same name
                if (workspaceName != null) {
                    existingWs.createOrUpdateProperty("ems:workspace_name", workspaceName);
                }
                if (desc != null) {
                    existingWs.createOrUpdateProperty("ems:description", desc);
                }
                finalWorkspace = existingWs;
            }
            else {
                log(LogLevel.WARNING, "Workspace not found.", HttpServletResponse.SC_NOT_FOUND);
                status.setCode( HttpServletResponse.SC_NOT_FOUND );
                return null;
            }
        }
        
        // Finally, apply the permissions:
        if (finalWorkspace != null) {
            if (permission.equals( "write" )) {
                finalWorkspace.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
            }
            // Default is read (this handles the empty string case also)
            else {
                finalWorkspace.setPermission( "SiteConsumer", "GROUP_EVERYONE" );
            }
            finalWorkspace.createOrUpdateProperty( "ems:permission", permission );
        }
        
        return finalWorkspace;
    }

}

