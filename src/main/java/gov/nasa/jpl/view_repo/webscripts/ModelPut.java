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
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Allows updating of sysml:id for synchronization of Alfresco created elements to MD
 * 
 * /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/element.put.desc.xml
 * @author cinyoung
 *
 */
public class ModelPut extends ModelPost {

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();

        try {
            createOrUpdateModel(req, status);
        } catch (JSONException e) {
            log(LogLevel.ERROR, "JSON malformed\n", HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        } catch (Exception e) {
            log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());
        model.put("res", response.toString());
        return model;
    }
    
    protected void createOrUpdateModel(WebScriptRequest req, Status status)
            throws Exception {
        JSONObject postJson = (JSONObject) req.parseContent();
        
        Iterator<?> oldIds = postJson.keys();
        while(oldIds.hasNext()) {
            String oldId = (String) oldIds.next();
            String newId = postJson.getString(oldId);
            
            if (oldId != null && newId != null && oldId != newId) {
                EmsScriptNode elementNode = findScriptNodeByName(oldId);
                if (checkPermissions(elementNode, PermissionService.WRITE)) {
                    if (elementNode != null) {
                        elementNode.setProperty(Acm.ACM_ID, newId);
                    } else {
                        log(LogLevel.WARNING, "Element not found with id: " + oldId, HttpServletResponse.SC_BAD_REQUEST);
                    }
                }
            }
        }
    }
}
