package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Library class for manipulating configurations
 * 
 * Pattern is handle does the retrieval.
 * 
 * @author cinyoung
 * 
 */
public class ConfigurationsWebscript extends AbstractJavaWebScript {

    public ConfigurationsWebscript( Repository repository, ServiceRegistry services,
                              StringBuffer response ) {
        super( repository, services, response );
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    
    public JSONArray handleConfigurations(WebScriptRequest req) throws JSONException {
        return handleConfigurations( req, false );
    }
    
    public EmsScriptNode getConfiguration(String id, String timestamp) {
        NodeRef configNodeRef = NodeUtil.getNodeRefFromNodeId( id );
        if (configNodeRef != null) {
            EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services);
            return configNode;
        } else {
            log(LogLevel.WARNING, "Could not find configuration with id " + id, HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }
    
    /**
     * Method to get Configurations within a specified context
     * @param context
     * @param timestamp
     * @param sort
     * @return
     */
    public List<EmsScriptNode> getConfigurations(EmsScriptNode context, String timestamp, boolean sort) {
        List<EmsScriptNode> configurations = new ArrayList<EmsScriptNode>();
        if (context != null) {
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            // grab all configurations in site and order by date
            Set< EmsScriptNode > nodes =
                    WebScriptUtil.getAllNodesInPath( context.getQnamePath(),
                                                     "TYPE",
                                                     "ems:ConfigurationSet",
                                                     dateTime, services,
                                                     response );
            configurations.addAll(nodes);
            if (sort) {
                Collections.sort(configurations, new EmsScriptNodeCreatedAscendingComparator());
            }
        }
        
        return configurations;
    }
    
    /**
     * Create JSONObject of Configuration sets based on webrequest
     * @param req
     * @return
     * @throws JSONException
     */
    public JSONArray handleConfigurations(WebScriptRequest req, boolean isMms) throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest(req);
        if (siteNode == null) {
            log(LogLevel.WARNING, "Could not find site", HttpServletResponse.SC_NOT_FOUND);
            return new JSONArray();
        }
        JSONArray configJsonArray = new JSONArray();
        
        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        
        boolean sort = true;
        List<EmsScriptNode> configurations = getConfigurations( siteNode, timestamp, sort );
            
        for (EmsScriptNode config: configurations) {
            if (isMms) {
                configJsonArray.put(getMmsConfigJson(config, req.getContextPath(), dateTime));
            } else {
                configJsonArray.put(getConfigJson(config, req.getContextPath(), dateTime));
            }
        }

        return configJsonArray;
    }
    
   
    /**
     * 
     * @param req
     * @param isMms
     * @return
     * @throws JSONException
     */
    public JSONArray handleConfiguration(WebScriptRequest req, boolean isMms) throws JSONException {
        String configId = req.getServiceMatch().getTemplateVars().get("configurationId");
        
        JSONArray configsJsonArray = handleConfigurations(req, isMms);
        JSONArray result = new JSONArray();
        
        for (int ii = 0; ii < configsJsonArray.length(); ii++) {
            JSONObject configJson = configsJsonArray.getJSONObject( ii );
            if (configJson.getString( "id" ).equals(configId)) {
                result.put( configJson );
                break;
            }
        }
        
        return result;
    }
    
    
    /**
     * Retrieve just the MMS API configuration JSON
     * @param config
     * @param contextPath
     * @param dateTime
     * @return
     * @throws JSONException
     */
    public JSONObject getMmsConfigJson(EmsScriptNode config, String contextPath, Date dateTime) throws JSONException {
        JSONObject json = getConfigJson( config, contextPath, dateTime );
        
        json.remove( "snapshots" );
        
        return json;
    }
    
    /**
     * Given a configuration node, convert to JSON
     * @param config
     * @param contextPath
     * @param dateTime 
     * @return
     * @throws JSONException
     */
    public JSONObject getConfigJson(EmsScriptNode config, String contextPath,
                                     Date dateTime) throws JSONException {
        JSONObject json = new JSONObject();
        Date date = (Date)config.getProperty("cm:created");
        
        json.put("modified", EmsScriptNode.getIsoTime(date));
        Object timestampObject = config.getProperty("view2:timestamp");
        Date timestamp = null;
        if ( timestampObject instanceof Date ) {
            timestamp = (Date)timestampObject;
        } else {
            if ( timestampObject != null ) {
                Debug.error( "timestamp is not a date! timestamp = " + timestampObject );
            }
            timestamp = new Date( System.currentTimeMillis() );
        }
        json.put("timestamp", EmsScriptNode.getIsoTime(timestamp));
        json.put("name", config.getProperty(Acm.CM_NAME));
        json.put("description", config.getProperty("cm:description"));
        // need to unravel id with the storeref, which by default is workspace://SpacesStore/
        json.put("id", config.getNodeRef().getId());
        
        json.put("snapshots", getSnapshots(config, timestamp));
        
        return json;
    }
 
    public JSONArray getSnapshots(EmsScriptNode config, Date timestamp) throws JSONException {
        JSONArray snapshotsJson = new JSONArray();
        
        List< EmsScriptNode > snapshots =
                config.getTargetAssocsNodesByType( "ems:configuredSnapshots",
                                                   timestamp );
        for (EmsScriptNode snapshot: snapshots) {
            List< EmsScriptNode > views =
                    snapshot.getSourceAssocsNodesByType( "view2:snapshots",
                                                         timestamp );
            if (views.size() >= 1) {
                snapshotsJson.put( getSnapshotJson(snapshot, views.get(0)) );
            }
        }
        
        return snapshotsJson;
    }
    
    /**
     * Based on a specified snapshot node and corresponding view and configuration returns the
     * snapshot JSON - maybe put this in EmsScriptNode?
     * @param snapshot
     * @param view
     * @param config
     * @return
     * @throws JSONException
     */
    public JSONObject getSnapshotJson(EmsScriptNode snapshot, EmsScriptNode view) throws JSONException {
        JSONObject snapshotJson = new JSONObject();
        //snapshotJson.put("url", contextPath + "/service/snapshots/" + snapshot.getProperty(Acm.ACM_ID));
        snapshotJson.put("sysmlid", view.getProperty(Acm.ACM_ID));
        snapshotJson.put("sysmlname", view.getProperty(Acm.ACM_NAME));
        snapshotJson.put("id", snapshot.getProperty(Acm.CM_NAME));
        snapshotJson.put( "created",  EmsScriptNode.getIsoTime( (Date)snapshot.getProperty( "cm:created" )));
        snapshotJson.put( "creator", snapshot.getProperty( "cm:modifier" ) );

        JSONArray configsJson = new JSONArray();
        List< EmsScriptNode > configs =
                snapshot.getSourceAssocsNodesByType( "ems:configuredSnapshots", null );
        for (EmsScriptNode config: configs) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", config.getProperty(Acm.CM_NAME));
            jsonObject.put("id", config.getNodeRef().getId());
            configsJson.put( jsonObject );
        }
        
        snapshotJson.put( "configurations", configsJson );
            
        return snapshotJson;
    }

    public JSONArray getProducts(EmsScriptNode config, Date timestamp) throws JSONException {
        JSONArray productsJson = new JSONArray();
        
        List< EmsScriptNode > products =
                config.getTargetAssocsNodesByType( "ems:configuredProducts",
                                                   timestamp );
        for (EmsScriptNode product: products) {
            productsJson.put( product.toJSONObject(timestamp) );
        }
        
        return productsJson;
    }
    
    /**
     * Based on a specified snapshot node and corresponding view and configuration returns the
     * snapshot JSON - maybe put this in EmsScriptNode?
     * @param product
     * @param view
     * @param config
     * @return
     * @throws JSONException
     */
    @Deprecated
    public JSONObject getProductJson(EmsScriptNode product) throws JSONException {
        JSONObject productJson = new JSONObject();
        productJson.put("sysmlid", product.getProperty(Acm.CM_NAME));
        productJson.put( "created",  EmsScriptNode.getIsoTime( (Date)product.getProperty( "cm:created" )));
        productJson.put( "creator", product.getProperty( "cm:modifier" ) );
            
        return productJson;
    }

    
    public void updateConfiguration(EmsScriptNode config, JSONObject postJson, EmsScriptNode context, Date date) throws JSONException {
        if (postJson.has("name")) {
            config.createOrUpdateProperty(Acm.CM_NAME, postJson.getString("name"));
        }
        if (postJson.has("description")) {
            config.createOrUpdateProperty("cm:description", postJson.getString("description"));
        }
        if (postJson.has("timestamp")) {
            // if timestamp specified always use
            config.createOrUpdateProperty("view2:timestamp", TimeUtils.dateFromTimestamp(postJson.getString("timestamp")));
        } else if (date != null && date.before( (Date)config.getProperty("cm:created") )) {
            // add in timestamp if supplied date is before the created date
            config.createOrUpdateProperty("view2:timestamp", date);
        } 
        
        // these should be mutually exclusive
        if (postJson.has( "products")) {
            config.removeAssociations( "ems:configuredProducts" );
            JSONArray productsJson = postJson.getJSONArray( "products" );
            for (int ii = 0; ii < productsJson.length(); ii++) {
                EmsScriptNode product = findScriptNodeById( productsJson.getString( ii ), null );
                if (product != null) {
                    config.createOrUpdateAssociation( product, "ems:configuredProducts", true );
                }
            }
        } else if (postJson.has( "snapshots" )) {
            config.removeAssociations("ems:configuredSnapshots");
            JSONArray snapshotsJson = postJson.getJSONArray("snapshots");
            EmsScriptNode snapshotFolder = context.childByNamePath("/snapshots");
            for (int ii = 0; ii < snapshotsJson.length(); ii++) {
                EmsScriptNode snapshot = snapshotFolder.childByNamePath("/" + snapshotsJson.getString(ii));
                if (snapshot != null) {
                    config.createOrUpdateAssociation(snapshot, "ems:configuredSnapshots", true);
                }
            }
        }
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
