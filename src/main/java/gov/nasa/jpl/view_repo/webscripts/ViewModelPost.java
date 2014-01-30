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
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.json.JSONArray;
import org.json.JSONObject;
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

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();

        String viewid = req.getServiceMatch().getTemplateVars().get("modelid");
        UserTransaction trx = services.getTransactionService().getUserTransaction();
        try {
            EmsScriptNode view = findScriptNodeByName(viewid);
            view.createOrUpdateProperty("cm:modifier", AuthenticationUtil.getFullyAuthenticatedUser());

            trx.commit();
        } catch (Throwable e) {
            try {
                System.out.println("\t####### ERROR: Needed to ViewModelPost rollback: " + e.getMessage());
                trx.rollback();
            } catch (Throwable ee) {
                System.out.println("\tRollback ViewModelPost failed: " + ee.getMessage());
            }
        }
        
        try {
            createOrUpdateModel(req, status);
        } catch (Exception e) {
            log(LogLevel.ERROR, "JSON malformed\n",
                    HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());
        model.put("res", response.toString());
        return model;
    }
    
    protected void createOrUpdateModel(WebScriptRequest req, Status status)
            throws Exception {
        JSONObject postJson = (JSONObject) req.parseContent();
        
        JSONArray array = postJson.getJSONArray("elements");
        
        for (int ii = 0; ii < array.length(); ii++) {
            JSONObject elementJson = array.getJSONObject(ii);
            
            String id = elementJson.getString(Acm.JSON_ID);
            EmsScriptNode elementNode = findScriptNodeById(id);
            if (elementNode != null) {
                updateOrCreateElement(elementJson, elementNode.getParent());
            } else {
                // new element, we need a proper parent
                boolean parentFound = true;
                
                // for now only support new comments
                if (elementJson.has(Acm.JSON_ANNOTATED_ELEMENTS)) {
                    JSONArray annotatedJson = elementJson.getJSONArray(Acm.JSON_ANNOTATED_ELEMENTS);
                    // lets make parent first annotated element
                    if (annotatedJson.length() <= 0) {
                        parentFound = false;
                    } else {
                        EmsScriptNode commentParent = findScriptNodeById(annotatedJson.getString(0));
                        if (commentParent == null) {
                            parentFound = false;
                        } else {
                            newElements.add(id);
                            updateOrCreateElement(elementJson, commentParent.getParent());
                        }
                    }
                    
                    if (!parentFound) {
                        log(LogLevel.WARNING, "Could not find parent for element with id: " + id, HttpServletResponse.SC_BAD_REQUEST);
                    }
                }
            }
        }
        
        updateOrCreateAllRelationships(relationshipsJson);
    }
}
