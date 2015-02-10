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
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;

import gov.nasa.jpl.view_repo.util.JsonArray;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.jpl.view_repo.util.JsonObject;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Services /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/view/element.post.desc.xml
 *
 * @author cinyoung
 *
 */
public class ViewModelPost extends ModelPost {
    public ViewModelPost() {
        super();
    }

    public ViewModelPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ViewModelPost instance = new ViewModelPost(repository, getServices());
        // Run without transactions since ViewModePost breaks them up itself, and calls
        // ModelPost methods which also have transactions:
        return instance.executeImplImpl(req,  status, cache, true);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();

        String[] idKeys = {"modelid", "elementId"};
        String viewid = null;

        for (String idKey: idKeys) {
            viewid = req.getServiceMatch().getTemplateVars().get(idKey);
        }
        UserTransaction trx = services.getTransactionService().getUserTransaction();
        try {
            WorkspaceNode workspace = getWorkspace( req );
            trx.begin();
            EmsScriptNode view = findScriptNodeById(viewid, workspace, null, true);
            view.createOrUpdateProperty("cm:modifier", AuthenticationUtil.getFullyAuthenticatedUser());
            trx.commit();
        } catch (Throwable e) {
            try {
                if (e instanceof JSONException) {
            			log(LogLevel.ERROR, "ViewModelPost: JSON malformed for: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
                } else {
            			log(LogLevel.ERROR, "ViewModelPost: DB transaction failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                e.printStackTrace();
                if (Debug.isOn()) System.out.println("\t####### ERROR: Needed to ViewModelPost rollback: " + e.getMessage());
                trx.rollback();
            } catch (Throwable ee) {
                log(LogLevel.ERROR, "\tViewModelPost: Rollback failed: " + ee.getMessage());
                ee.printStackTrace();
            }
        }

        ViewModelPost instance = new ViewModelPost(repository, services);

        try {
//            Set< EmsScriptNode > elements =
                    instance.createOrUpdateModel(req, status);
            appendResponseStatusInfo(instance);
        } catch (JSONException e) {
            log(LogLevel.ERROR, "JSON malformed\n", HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        } catch (Exception e) {
            log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

        // UNCOMMENT THIS
        // Create JSON object of the elements to return:
//        JsonObject top = new JsonObject();
//        JsonArray elementsJson = new JsonArray();
//        for ( EmsScriptNode element : elements ) {
//            elementsJson.put( element.toJsonObject(null) );
//        }
//        top.put( "elements", elementsJson );
//        model.put( "res", top.toString( 4 ) );

        status.setCode(responseStatus.getCode());
        model.put("res", response.toString());

        printFooter();

        return model;
    }

    protected void createOrUpdateModel(WebScriptRequest req, Status status) throws Exception {
        clearCaches();

        JsonObject postJson = JsonObject.make( (JSONObject)req.parseContent() );
        JsonArray array = postJson.getJSONArray("elements");

        WorkspaceNode workspace = getWorkspace( req );

        for (int ii = 0; ii < array.length(); ii++) {
            JsonObject elementJson = array.getJSONObject(ii);

            // If element does not have a ID, then create one for it using the alfresco id (cm:id):
            if (!elementJson.has(Acm.JSON_ID)) {
                elementJson.put( Acm.JSON_ID, NodeUtil.createId( services ) );
            }
            String id = elementJson.getString(Acm.JSON_ID);

            EmsScriptNode elementNode = findScriptNodeById(id, workspace, null, true);
            if (elementNode != null) {
                updateOrCreateElement(elementJson, elementNode.getParent(), workspace, false);
            } else {
                // new element, we need a proper parent
                boolean parentFound = true;

                // for now only support new comments
                if (elementJson.has(Acm.JSON_ANNOTATED_ELEMENTS)) {
                    JsonArray annotatedJson = elementJson.getJSONArray(Acm.JSON_ANNOTATED_ELEMENTS);
                    // lets make parent first annotated element
                    if (annotatedJson.length() <= 0) {
                        parentFound = false;
                    } else {
                        EmsScriptNode commentParent = findScriptNodeById(annotatedJson.getString(0), workspace, null, true);
                            if (commentParent == null) {
                                parentFound = false;
                            } else {
                                if (checkPermissions(commentParent, PermissionService.WRITE)) {
                                    newElements.add(id);
                                    updateOrCreateElement(elementJson, commentParent.getOwningParent(null), workspace, false);
                                }
                            }
                    }

                    if (!parentFound) {
                        log(LogLevel.WARNING, "Could not find parent for element with id: " + id, HttpServletResponse.SC_BAD_REQUEST);
                    }
                }
            }
        }

        updateOrCreateAllRelationships(relationshipsJson, workspace);

        updateNodeReferencesForView( array, workspace );
    }

    protected void updateNodeReferencesForView( JsonArray array,
                                                WorkspaceNode workspace )
                                                        throws Exception {
        for (int ii = 0; ii < array.length(); ii++) {
            JsonObject elementJson = array.getJSONObject(ii);

            String id = elementJson.getString(Acm.JSON_ID);
            EmsScriptNode elementNode = findScriptNodeById(id, workspace, null, true);
            if (elementNode != null) {
                updateOrCreateElement(elementJson, elementNode.getParent(), workspace, true);
            } else {
                // new element, we need a proper parent
                boolean parentFound = true;

                // for now only support new comments
                if (elementJson.has(Acm.JSON_ANNOTATED_ELEMENTS)) {
                    JsonArray annotatedJson = elementJson.getJSONArray(Acm.JSON_ANNOTATED_ELEMENTS);
                    // lets make parent first annotated element
                    if (annotatedJson.length() <= 0) {
                        parentFound = false;
                    } else {
                        EmsScriptNode commentParent = findScriptNodeById(annotatedJson.getString(0), workspace, null, true);
                            if (commentParent == null) {
                                parentFound = false;
                            } else {
                                if (checkPermissions(commentParent, PermissionService.WRITE)) {
                                    updateOrCreateElement(elementJson, commentParent.getParent(), workspace, true);
                                }
                            }
                    }

                    if (!parentFound) {
                        log(LogLevel.WARNING, "Could not find parent for element with id: " + id, HttpServletResponse.SC_BAD_REQUEST);
                    }
                }
            }

        }
    }
}
