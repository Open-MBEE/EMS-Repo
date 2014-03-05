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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Retrieve all Configuration Sets for a site
 * @author cinyoung
 *
 */
public class ConfigurationGet extends AbstractJavaWebScript {
    public ConfigurationGet() {
        super();
    }
        
    public ConfigurationGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // Do nothing
        return false;
    }

    @Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();

        clearCaches();

        // need to create new instance to do evaluation...
        ConfigurationGet instance = new ConfigurationGet(repository, services);
        
        JSONObject jsonObject = instance.handleConfiguration(req);
        appendResponseStatusInfo(instance);
        if (jsonObject != null) {
            try {
                model.put("res", jsonObject.toString(4));
                model.put("title", req.getServiceMatch().getTemplateVars().get(SITE_NAME));
            } catch (JSONException e) {
                log(LogLevel.ERROR, "JSON toString error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                e.printStackTrace();
            }
        } else {
            model.put("res", response.toString());
            model.put("title", "ERROR could not load");
            log(LogLevel.WARNING, "Could not find configuration", HttpServletResponse.SC_NOT_FOUND);
        }
        
        status.setCode(responseStatus.getCode());
        return model;
    }

    /**
     * Create JSONObject of Configuration sets
     * @param req
     * @return
     * @throws JSONException
     */
    public JSONObject handleConfiguration(WebScriptRequest req) {
        String siteName; 
        SiteInfo siteInfo; 
        EmsScriptNode siteNode;
        
        siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        if (siteName == null) {
            log(LogLevel.ERROR, "No sitename provided", HttpServletResponse.SC_BAD_REQUEST);
        }
        
        siteInfo = services.getSiteService().getSite(siteName);
        if (siteInfo == null) {
            log(LogLevel.ERROR, "Could not find site: " + siteName, HttpServletResponse.SC_NOT_FOUND);
        }
        siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);

        // grab all configurations in site and order by date
        List<EmsScriptNode> configurations = new ArrayList<EmsScriptNode>();
        configurations.addAll(WebScriptUtil.getAllNodesInPath(siteNode.getQnamePath(), "TYPE", "ems:ConfigurationSet", services, response));
        Collections.sort(configurations, new EmsScriptNodeCreatedAscendingComparator());
        
        JSONObject jsonObject = null;
        try {
            JSONArray configJsonArray = new JSONArray();
            for (EmsScriptNode config: configurations) {
                configJsonArray.put(getConfigJson(config, req.getContextPath()));
            }
            
            jsonObject = new JSONObject(); 
            jsonObject.put("configurations", configJsonArray);
        } catch (JSONException e) {
            log(LogLevel.ERROR, "Could not create the snapshot JSON", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            jsonObject = null;
        }

        return jsonObject;
    }
    
    /**
     * Given a configuration node, convert to JSON
     * @param config
     * @param contextPath
     * @return
     * @throws JSONException
     */
    private JSONObject getConfigJson(EmsScriptNode config, String contextPath) throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray snapshotsJson = new JSONArray();
        Date date = (Date)config.getProperty("cm:created");
        
        json.put("modified", EmsScriptNode.getIsoTime(date));
        json.put("name", config.getProperty(Acm.CM_NAME));
        json.put("description", config.getProperty("cm:description"));
        json.put("nodeid", EmsScriptNode.getStoreRef().toString() + "/" + config.getNodeRef().getId());
        
        List<EmsScriptNode> snapshots = config.getTargetAssocsNodesByType("ems:configuredSnapshots");
        for (EmsScriptNode snapshot: snapshots) {
            List<EmsScriptNode> views = snapshot.getSourceAssocsNodesByType("view2:snapshots");
            if (views.size() >= 1) {
                JSONObject snapshotJson = new JSONObject();
                snapshotJson.put("url", contextPath + "/service/snapshots/" + snapshot.getProperty(Acm.ACM_ID));
            		snapshotJson.put("name", views.get(0).getProperty(Acm.ACM_NAME));
            		snapshotsJson.put(snapshotJson);
            }
        }
        
        json.put("snapshots", snapshotsJson);
        
        return json;
    }
    
    /**
     * Comparator sorts by ascending created date
     * @author cinyoung
     *
     */
    public static class EmsScriptNodeCreatedAscendingComparator implements Comparator<EmsScriptNode> {
        @Override
        public int compare(EmsScriptNode x, EmsScriptNode y) {
            Date xModified;
            Date yModified;
            
            xModified = (Date) x.getProperty("cm:created");
            yModified = (Date) y.getProperty("cm:created");
            
            if (xModified == null) {
                return -1;
            } else if (yModified == null) {
                return 1;
            } else {
                return (yModified.compareTo(xModified));
            }
        }
    }
}
