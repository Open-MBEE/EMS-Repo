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
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
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

        printHeader( req );
        
        clearCaches();

        // need to create new instance to do evaluation...
        ConfigurationGet instance = new ConfigurationGet(repository, services);
        
        JSONObject jsonObject = new JSONObject();
        
        try {
        		jsonObject.put("configurations", instance.handleConfiguration(req));
        		jsonObject.put("products", instance.handleProducts(req));
        		appendResponseStatusInfo(instance);
        		model.put("res", jsonObject.toString(2));
            model.put("title", req.getServiceMatch().getTemplateVars().get(SITE_NAME));
        } catch (Exception e) {
        		model.put("res", response.toString());
        		model.put("title", "ERROR could not load");
        		if (e instanceof JSONException) {
        			log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        		} else {
        			log(LogLevel.ERROR, "Internal server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        		}
        		e.printStackTrace();
        } 
        
        status.setCode(responseStatus.getCode());

        printFooter();
        
        return model;
    }

    public JSONArray handleProducts(WebScriptRequest req) throws JSONException {
    		EmsScriptNode siteNode = getSiteNodeFromRequest(req);
    		JSONArray productsJson = new JSONArray();

    		if (siteNode != null) {
                // get timestamp if specified
                String timestamp = req.getParameter("timestamp");
                Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            Set< EmsScriptNode > productSet =
                    WebScriptUtil.getAllNodesInPath( siteNode.getQnamePath(),
                                                     "ASPECT", Acm.ACM_PRODUCT,
                                                     dateTime, services,
                                                     response );
	        for (EmsScriptNode product: productSet) {
	        		JSONObject productJson = new JSONObject();
	        		String productId = (String) product.getProperty(Acm.ACM_ID);
	        		
	        		productJson.put(Acm.JSON_ID, productId);
	        		productJson.put(Acm.JSON_NAME, product.getProperty(Acm.ACM_NAME));
	        		productJson.put("snapshots", getProductSnapshots(productId, req.getContextPath(), dateTime));
	        		
	        		productsJson.put(productJson);
	        }
    		}
    		        
    		return productsJson;
    }
    
    /**
     * Create JSONObject of Configuration sets
     * @param req
     * @return
     * @throws JSONException
     */
    public JSONArray handleConfiguration(WebScriptRequest req) throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest(req);
        JSONArray configJsonArray = new JSONArray();
        
        if (siteNode != null) {
            // get timestamp if specified
            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

	        // grab all configurations in site and order by date
	        List<EmsScriptNode> configurations = new ArrayList<EmsScriptNode>();
            Set< EmsScriptNode > nodes =
                    WebScriptUtil.getAllNodesInPath( siteNode.getQnamePath(),
                                                     "TYPE",
                                                     "ems:ConfigurationSet",
                                                     dateTime, services,
                                                     response );
	        configurations.addAll(nodes);
	        Collections.sort(configurations, new EmsScriptNodeCreatedAscendingComparator());
	        
            for (EmsScriptNode config: configurations) {
                configJsonArray.put(getConfigJson(config, req.getContextPath(), dateTime));
            }
        }

        return configJsonArray;
    }
    
    /**
     * Given a configuration node, convert to JSON
     * @param config
     * @param contextPath
     * @param dateTime 
     * @return
     * @throws JSONException
     */
    private JSONObject getConfigJson(EmsScriptNode config, String contextPath,
                                     Date dateTime) throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray snapshotsJson = new JSONArray();
        Date date = (Date)config.getProperty("cm:created");
        
        json.put("modified", EmsScriptNode.getIsoTime(date));
        json.put("name", config.getProperty(Acm.CM_NAME));
        json.put("description", config.getProperty("cm:description"));
        json.put("nodeid", EmsScriptNode.getStoreRef().toString() + "/" + config.getNodeRef().getId());
        
        List< EmsScriptNode > snapshots =
                config.getTargetAssocsNodesByType( "ems:configuredSnapshots",
                                                   dateTime );
        for (EmsScriptNode snapshot: snapshots) {
            List< EmsScriptNode > views =
                    snapshot.getSourceAssocsNodesByType( "view2:snapshots",
                                                         dateTime );
            if (views.size() >= 1) {
                JSONObject snapshotJson = new JSONObject();
                snapshotJson.put("url", contextPath + "/service/snapshots/" + snapshot.getProperty(Acm.ACM_ID));
                EmsScriptNode view = views.get(0);
            		snapshotJson.put("name", view.getProperty(Acm.ACM_NAME));
            		snapshotJson.put("id", snapshot.getProperty(Acm.CM_NAME));
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
    
    
    /**
     * TODO: this is same as handleProductSnapshots in MoaProductGet - work this out into a utility
     * @param productId
     * @param contextPath
     * @param productsJson
     * @throws JSONException
     */
	private JSONArray getProductSnapshots(String productId, String contextPath, Date dateTime) throws JSONException {
	    EmsScriptNode product = findScriptNodeById(productId, dateTime);
	    
        JSONArray snapshotsJson = new JSONArray();
        List< EmsScriptNode > snapshotsList =
                product.getTargetAssocsNodesByType( "view2:snapshots", dateTime );

        Collections.sort(snapshotsList, new EmsScriptNode.EmsScriptNodeComparator());
        for (EmsScriptNode snapshot: snapshotsList) {
            String id = (String)snapshot.getProperty(Acm.ACM_ID);
            Date date = (Date)snapshot.getProperty(Acm.ACM_LAST_MODIFIED);
            
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("created", EmsScriptNode.getIsoTime(date));
            jsonObject.put("creator", (String) snapshot.getProperty("cm:modifier"));
            jsonObject.put("url", contextPath + "/service/snapshots/" + snapshot.getProperty(Acm.ACM_ID));
            jsonObject.put("tag", (String)SnapshotGet.getConfigurationSet(snapshot, dateTime));
            snapshotsJson.put(jsonObject);
        }
        
        return snapshotsJson;
    }

}
