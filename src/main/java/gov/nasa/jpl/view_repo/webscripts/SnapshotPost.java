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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class SnapshotPost extends AbstractJavaWebScript {
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        clearCaches();

        String viewid = req.getServiceMatch().getTemplateVars().get("viewid");
        EmsScriptNode topview = findScriptNodeById(viewid);
        EmsScriptNode snapshotFolderNode = getSnapshotFolderNode(topview);

        Map<String, Object> model = new HashMap<String, Object>();

        String html;
        try {
            html = req.getContent().getContent();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            html = null;
        }

        if (html != null) {
            DateTime now = new DateTime();
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

            String snapshotId = viewid + "_" + now.getMillis() + ".html";
            EmsScriptNode snapshotNode = snapshotFolderNode.createNode(
                    snapshotId, "view2:Snapshot");
            snapshotNode.createOrUpdateProperty("cm:isIndexed", true);
            snapshotNode.createOrUpdateProperty("cm:isContentIndexed", false);
            snapshotNode.createOrUpdateProperty(Acm.ACM_ID, snapshotId);
            
            System.out.println("Creating snapshot with indexing: " + snapshotNode.getProperty("cm:isIndexed"));
            ContentWriter writer = services.getContentService().getWriter(
                    snapshotNode.getNodeRef(), ContentModel.PROP_CONTENT, true);
            writer.putContent(html);
            
            ContentData contentData = writer.getContentData();
            contentData = ContentData.setMimetype(contentData, "text/html");
            services.getNodeService().setProperty(snapshotNode.getNodeRef(), ContentModel.PROP_CONTENT, contentData);

            // Scriptnode has weird toplevelscope null pointer
//            Map<String, Object> properties = snapshotNode.getProperties();
//            if (properties != null) {
//                ScriptNode.ScriptContentData content = (ScriptNode.ScriptContentData)properties.get("content");
//                content.setMimetype("text/html");
//            } else {
//                System.out.println("properties are null....");
//            }
            
            topview.createOrUpdateAssociation(snapshotNode, "view2:snapshots");
            
            
            status.setCode(responseStatus.getCode());
            try {
                JSONObject snapshoturl = new JSONObject();
                snapshoturl.put("id", snapshotId);
                snapshoturl.put("creator",
                        AuthenticationUtil.getFullyAuthenticatedUser());
                snapshoturl.put("created", fmt.print(now));
                snapshoturl.put("url", req.getContextPath() + snapshotNode.getUrl());

                model.put("res", snapshoturl.toString(4));
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                model.put("res", response.toString());
            }
        } else {
            model.put("res", response.toString());
        }
        
        return model;
    }

    public static EmsScriptNode getSnapshotFolderNode(EmsScriptNode viewNode) {
        EmsScriptNode parent = viewNode.getParent();

        String parentName = (String) parent.getProperty(Acm.ACM_CM_NAME);
        while (!parentName.equals("ViewEditor")) {
            EmsScriptNode oldparent = parent;
            parent = oldparent.getParent();
            parentName = (String) parent.getProperty(Acm.ACM_CM_NAME);
        }

        EmsScriptNode snapshotNode = parent.childByNamePath("snapshots");
        if (snapshotNode == null) {
            snapshotNode = parent.createFolder("snapshots");
        }

        return snapshotNode;
    }

}
