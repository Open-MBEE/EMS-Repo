package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
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
    
    public EmsScriptNode getConfiguration(String id, EmsScriptNode context, String timestamp) {
        boolean noSort = false;
        List<EmsScriptNode> configurations = getConfigurations(context, timestamp, noSort);
        for (EmsScriptNode config: configurations) {
            if (config.getProperty( "cm:name" ).equals( id )) {
                return config;
            }
        }
        
        return null;
    }
    
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
     * Create JSONObject of Configuration sets
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
        Object timestampObject = config.getProperty("ems:timestamp");
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
        json.put("id", EmsScriptNode.getStoreRef().toString() + "/" + config.getNodeRef().getId());
        
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
//                JSONObject snapshotJson = new JSONObject();
//                //snapshotJson.put("url", contextPath + "/service/snapshots/" + snapshot.getProperty(Acm.ACM_ID));
//                EmsScriptNode view = views.get(0);
//                snapshotJson.put("sysmlid", view.getProperty(Acm.ACM_NAME));
//                snapshotJson.put("id", snapshot.getProperty(Acm.CM_NAME));
//                snapshotJson.put( "created",  EmsScriptNode.getIsoTime( (Date)snapshot.getProperty( "cm:created" )));
//                snapshotJson.put( "creator", snapshot.getProperty( "cm:modifier" ) );
//                snapshotJson.put( "configuration", config.getProperty( Acm.CM_NAME ) );
//                snapshotsJson.put(snapshotJson);
                snapshotsJson.put( getSnapshotJson(snapshot, views.get(0), config) );
            }
        }
        
        return snapshotsJson;
    }
    
    public JSONObject getSnapshotJson(EmsScriptNode snapshot, EmsScriptNode view, EmsScriptNode config) throws JSONException {
        JSONObject snapshotJson = new JSONObject();
        //snapshotJson.put("url", contextPath + "/service/snapshots/" + snapshot.getProperty(Acm.ACM_ID));
        snapshotJson.put("sysmlid", view.getProperty(Acm.ACM_NAME));
        snapshotJson.put("id", snapshot.getProperty(Acm.CM_NAME));
        snapshotJson.put( "created",  EmsScriptNode.getIsoTime( (Date)snapshot.getProperty( "cm:created" )));
        snapshotJson.put( "creator", snapshot.getProperty( "cm:modifier" ) );
        if (config != null) {
            // TODO: look up configurations
            snapshotJson.put( "configuration", config.getProperty( Acm.CM_NAME ) );
        }
        
        return snapshotJson;
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
