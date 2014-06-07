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

import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;
import gov.nasa.jpl.view_repo.webscripts.util.ProductsWebscript;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
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

        JSONObject jsonObject = new JSONObject();
        
        try {
            ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
        		jsonObject.put("configurations", configWs.handleConfigurations(req));
        		ProductsWebscript productWs = new ProductsWebscript(repository, services, response);
        		jsonObject.put("products", productWs.handleProducts(req));
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

    
//    /**
//     * Create JSONObject of Configuration sets
//     * @param req
//     * @return
//     * @throws JSONException
//     */
//    public JSONArray handleConfiguration(WebScriptRequest req) throws JSONException {
//        EmsScriptNode siteNode = getSiteNodeFromRequest(req);
//        JSONArray configJsonArray = new JSONArray();
//        
//        if (siteNode != null) {
//            // get timestamp if specified
//            String timestamp = req.getParameter("timestamp");
//            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
//
//	        // grab all configurations in site and order by date
//	        List<EmsScriptNode> configurations = new ArrayList<EmsScriptNode>();
//            Set< EmsScriptNode > nodes =
//                    WebScriptUtil.getAllNodesInPath( siteNode.getQnamePath(),
//                                                     "TYPE",
//                                                     "ems:ConfigurationSet",
//                                                     dateTime, services,
//                                                     response );
//	        configurations.addAll(nodes);
//	        Collections.sort(configurations, new EmsScriptNodeCreatedAscendingComparator());
//	        
//            for (EmsScriptNode config: configurations) {
//                configJsonArray.put(getConfigJson(config, req.getContextPath(), dateTime));
//            }
//        }
//
//        return configJsonArray;
//    }
//    
//    /**
//     * Given a configuration node, convert to JSON
//     * @param config
//     * @param contextPath
//     * @param dateTime 
//     * @return
//     * @throws JSONException
//     */
//    private JSONObject getConfigJson(EmsScriptNode config, String contextPath,
//                                     Date dateTime) throws JSONException {
//        JSONObject json = new JSONObject();
//        JSONArray snapshotsJson = new JSONArray();
//        Date date = (Date)config.getProperty("cm:created");
//        
//        json.put("modified", EmsScriptNode.getIsoTime(date));
//        Object timestampObject = config.getProperty("ems:timestamp");
//        Date timestamp = null;
//        if ( timestampObject instanceof Date ) {
//            timestamp = (Date)timestampObject;
//        } else {
//            if ( timestampObject != null ) {
//                Debug.error( "timestamp is not a date! timestamp = " + timestampObject );
//            }
//            timestamp = new Date( System.currentTimeMillis() );
//        }
//        //Date timestamp = (Date)timestampObject;
//        json.put("timestamp", EmsScriptNode.getIsoTime(timestamp));
//        json.put("name", config.getProperty(Acm.CM_NAME));
//        json.put("description", config.getProperty("cm:description"));
//        json.put("nodeid", EmsScriptNode.getStoreRef().toString() + "/" + config.getNodeRef().getId());
//        
//        List< EmsScriptNode > snapshots =
//                config.getTargetAssocsNodesByType( "ems:configuredSnapshots",
//                                                   timestamp );
//        for (EmsScriptNode snapshot: snapshots) {
//            List< EmsScriptNode > views =
//                    snapshot.getSourceAssocsNodesByType( "view2:snapshots",
//                                                         timestamp );
//            if (views.size() >= 1) {
//                JSONObject snapshotJson = new JSONObject();
//                //snapshotJson.put("url", contextPath + "/service/snapshots/" + snapshot.getProperty(Acm.ACM_ID));
//                EmsScriptNode view = views.get(0);
//        		snapshotJson.put("name", view.getProperty(Acm.ACM_NAME));
//        		snapshotJson.put("id", snapshot.getProperty(Acm.CM_NAME));
//        		snapshotsJson.put(snapshotJson);
//            }
//        }
//        
//        json.put("snapshots", snapshotsJson);
//        
//        return json;
//    }
//    
//    /**
//     * Comparator sorts by ascending created date
//     * @author cinyoung
//     *
//     */
//    public static class EmsScriptNodeCreatedAscendingComparator implements Comparator<EmsScriptNode> {
//        @Override
//        public int compare(EmsScriptNode x, EmsScriptNode y) {
//            Date xModified;
//            Date yModified;
//            
//            xModified = (Date) x.getProperty("cm:created");
//            yModified = (Date) y.getProperty("cm:created");
//            
//            if (xModified == null) {
//                return -1;
//            } else if (yModified == null) {
//                return 1;
//            } else {
//                return (yModified.compareTo(xModified));
//            }
//        }
//    }

}
