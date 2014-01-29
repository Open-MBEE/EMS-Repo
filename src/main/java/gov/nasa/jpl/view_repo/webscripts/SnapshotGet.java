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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class SnapshotGet extends AbstractJavaWebScript {
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return true;
    }

    @Override
    protected void clearCaches() {
        super.clearCaches();
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        clearCaches();

        String viewId = req.getServiceMatch().getTemplateVars().get("viewid");


        Map<String, Object> model = new HashMap<String, Object>();
        try {
            JSONObject top = new JSONObject();
            top.put("snapshots", handleSnapshots(viewId, req.getContextPath()));
            model.put("res", top.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
            log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            model.put("res", response.toString());
        }

        status.setCode(responseStatus.getCode());
        return model;
    }

    
    /**
     * TODO - make handleSnapshots utility and move duplicate out of MoaProductGet.java
     * @param productId
     * @param contextPath
     * @return
     * @throws JSONException
     */
    private JSONArray handleSnapshots(String productId, String contextPath) throws JSONException {
        EmsScriptNode product = findScriptNodeByName(productId);
        
        JSONArray snapshotsJson = new JSONArray();
        List<EmsScriptNode> snapshotsList = product.getTargetAssocsNodesByType("view2:snapshots");

        Collections.sort(snapshotsList, new EmsScriptNode.EmsScriptNodeComparator());
        for (EmsScriptNode snapshot: snapshotsList) {
            JSONObject jsonObject = new JSONObject();
            // strip off the _{timestamp.html} from the end of the snapshots id
            String id = (String)snapshot.getProperty(Acm.ACM_ID);
            id = id.substring(0,id.lastIndexOf("_"));
            jsonObject.put("id", id);
            DateTime dt = new DateTime((Date)snapshot.getProperty(Acm.ACM_LAST_MODIFIED));
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            jsonObject.put("created", fmt.print(dt));
            jsonObject.put("url", contextPath + snapshot.getUrl());
            jsonObject.put("creator", (String) snapshot.getProperty("cm:modifier"));
            snapshotsJson.put(jsonObject);
        }
        
        return snapshotsJson;
    }
}
