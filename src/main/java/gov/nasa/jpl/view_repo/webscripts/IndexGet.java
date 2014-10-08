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
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Handler for rendering custom Document landing page
 * 
 * Looks for the "index.json" inside of the Site directory and renders that.
 * If file isn't found, defaults to ProductListGet.
 * 
 * @author cinyoung
 *
 */
public class IndexGet extends AbstractJavaWebScript {
    public IndexGet() {
       super(); 
    }
    
    public IndexGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // do nothing
        return true;
    }

    @Override
    protected void clearCaches() {
        super.clearCaches();
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();

        IndexGet instance = new IndexGet(repository, services);

        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
    
       
        String siteName = req.getServiceMatch().getTemplateVars().get("id");
        SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        
        WorkspaceNode workspace = getWorkspace( req );
        
        if (siteInfo == null) {
            log(LogLevel.ERROR, "Could not find site: " + siteName, HttpServletResponse.SC_NOT_FOUND);
        } else {
            //EmsScriptNode site = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
            EmsScriptNode site = getSiteNode( siteName, workspace, dateTime );
            JSONObject json;
            try {
                json = instance.getIndexJson(site, workspace, dateTime);
                appendResponseStatusInfo(instance);
                if (!Utils.isNullOrEmpty(response.toString())) json.put("message",response.toString());
                model.put("res", json.toString());
                model.put("title", siteName);
                model.put("siteName", site.getProperty(Acm.CM_NAME));
                model.put("siteTitle", site.getProperty(Acm.CM_TITLE));
            } catch (JSONException e) {
                log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                e.printStackTrace();
            }
        }

        status.setCode(responseStatus.getCode());
        if (status.getCode() != HttpServletResponse.SC_OK){
            model.put("res", "{}"); // don't dump anything out
            model.put("title", "ERROR could not render");
            model.put("siteName", "");
            model.put("siteTitle", "ERROR site not found");
        }

        printFooter();

        return model;
    }

    /**
     * Retrieve the index.json file if it exists and parse into JSONObject
     * @param site
     * @param workspace 
     * @return
     * @throws JSONException
     */
    private JSONObject getIndexJson(EmsScriptNode site, WorkspaceNode workspace, Date dateTime) throws JSONException {
        EmsScriptNode index = site.childByNamePath("index.json");
        if (index == null) {
            ProductListGet productListService = new ProductListGet(repository, services);
            return productListService.handleProductList(site, workspace, dateTime); 
        } else {
            ContentReader reader = services.getContentService().getReader(index.getNodeRef(), ContentModel.PROP_CONTENT);
            return new JSONObject(reader.getContentString());
        }
    }
}
