package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import gov.nasa.jpl.view_repo.webscripts.HostnameGet;
import gov.nasa.jpl.view_repo.webscripts.SnapshotPost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import gov.nasa.jpl.view_repo.util.JsonArray;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.JsonObject;
import org.springframework.extensions.webscripts.Cache;
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


    public JsonArray handleConfigurations(WebScriptRequest req) throws JSONException {
        return handleConfigurations( req, false );
    }

    public EmsScriptNode getConfiguration(String id) {
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
    public List< EmsScriptNode > getConfigurations( EmsScriptNode siteNode,
                                                    WorkspaceNode workspace,
                                                    String timestamp,
                                                    boolean sort ) {
        List<EmsScriptNode> configurations = new ArrayList<EmsScriptNode>();
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        // Note: not using searchForElements() b/c it checks if the return element has a sysml:id, which
        //       configurations do not
        String siteName = siteNode == null ? null : siteNode.getName();
        ArrayList<NodeRef> resultSet = NodeUtil.findNodeRefsByType( "ems:ConfigurationSet", NodeUtil.SearchType.TYPE.prefix,
                                                                    false, workspace,
                                                                    true, // onlyThisWorkspace
                                                                    dateTime, false, false, services, false,
                                                                    siteName );
        List< EmsScriptNode > nodeList = EmsScriptNode.toEmsScriptNodeList( resultSet, services, response,
                                                                         responseStatus );
        if (nodeList != null) {
            Set<EmsScriptNode> nodes = new HashSet<EmsScriptNode>(nodeList);
            configurations.addAll(nodes);
        }
        if (sort) {
            Collections.sort(configurations, new EmsScriptNodeCreatedAscendingComparator());
        }

        return configurations;
    }

    /**
     * Create JsonObject of Configuration sets based on webrequest
     * @param req
     * @return
     * @throws JSONException
     */
    public JsonArray handleConfigurations(WebScriptRequest req, boolean isMms) throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest(req, false);
        String siteNameFromReq = getSiteName( req );
        if ( siteNode == null && !Utils.isNullOrEmpty( siteNameFromReq )
             && !siteNameFromReq.equals( NO_SITE_ID ) ) {
            log(LogLevel.WARNING, "Could not find site " + siteNameFromReq, HttpServletResponse.SC_NOT_FOUND);
            return new JsonArray();
        }
        JsonArray configJsonArray = new JsonArray();

        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

        boolean sort = true;
        List< EmsScriptNode > configurations =
                getConfigurations( siteNode, workspace, timestamp, sort );

        for (EmsScriptNode config: configurations) {
            if (!config.isDeleted()) {
                if (isMms) {
                    configJsonArray.put( getMmsConfigJson( config,
                                                           workspace, dateTime ) );
                } else {
                    configJsonArray.put( getConfigJson( config,
                                                        workspace, dateTime ) );
                }
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
    public JsonArray handleConfiguration(WebScriptRequest req, boolean isMms) throws JSONException {
        String configId = req.getServiceMatch().getTemplateVars().get("configurationId");

        JsonArray configsJsonArray = handleConfigurations(req, isMms);
        JsonArray result = new JsonArray();

        for (int ii = 0; ii < configsJsonArray.length(); ii++) {
            JsonObject configJson = configsJsonArray.getJSONObject( ii );
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
     * @param workspace
     * @param dateTime
     * @return
     * @throws JSONException
     */
    public JsonObject getMmsConfigJson(EmsScriptNode config,
                                       WorkspaceNode workspace, Date dateTime)
                                               throws JSONException {
        JsonObject json = getConfigJson( config, workspace, dateTime );

        json.remove( "snapshots" );

        return json;
    }

    /**
     * Given a configuration node, convert to JSON
     * @param config
     * @param contextPath
     * @param workspace
     * @param dateTime
     * @return
     * @throws JSONException
     */
    public JsonObject getConfigJson(EmsScriptNode config,
                                    WorkspaceNode workspace, Date dateTime) throws JSONException {
        JsonObject json = new JsonObject();
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

        json.put("snapshots", getSnapshots(config, workspace));

        return json;
    }

    public JsonArray getSnapshots(EmsScriptNode config, WorkspaceNode workspace) throws JSONException {
        JsonArray snapshotsJson = new JsonArray();

        // Need to put in null timestamp so we always get latest version of snapshot
        List< EmsScriptNode > snapshots =
                config.getTargetAssocsNodesByType( "ems:configuredSnapshots",
                                                   workspace, null );
        for (EmsScriptNode snapshot: snapshots) {
            // getting by association is deprecated
            List< EmsScriptNode > views =
                    snapshot.getSourceAssocsNodesByType( "view2:snapshots",
                                                         workspace, null );
            if (views.size() >= 1) {
                if ( !snapshot.isDeleted() ) {
                    snapshotsJson.put( getSnapshotJson(snapshot, views.get(0),
                                                       workspace) );
                }
            }

            NodeRef snapshotProductNodeRef = (NodeRef) snapshot.getProperty( "view2:snapshotProduct" );
            if ( snapshotProductNodeRef != null ) {
                // this is the unversioned snapshot, so we need to get the versioned one
                EmsScriptNode snapshotProduct = new EmsScriptNode(snapshotProductNodeRef, services, response);
                
                String id = snapshotProduct.getSysmlId();
                Date dateTime = (Date) snapshot.getProperty("view2:timestamp");
                
                EmsScriptNode versionedSnapshotProduct = NodeUtil.findScriptNodeById( id, workspace, dateTime, true, services, response );
                if (snapshotProduct.exists()) {
                    snapshotsJson.put( getSnapshotJson(snapshot, versionedSnapshotProduct, workspace) );
                }
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
    public JsonObject
            getSnapshotJson( EmsScriptNode snapshot, EmsScriptNode view,
                             WorkspaceNode workspace ) throws JSONException {
        JsonObject snapshotJson = new JsonObject();
        //snapshotJson.put("url", contextPath + "/service/snapshots/" + snapshot.getSysmlId());
        snapshotJson.put("sysmlid", view.getSysmlId());
        snapshotJson.put("sysmlname", view.getProperty(Acm.ACM_NAME));
        snapshotJson.put("id", snapshot.getProperty(Acm.CM_NAME));
        snapshotJson.put( "created",  EmsScriptNode.getIsoTime( (Date)snapshot.getProperty( "cm:created" )));
        snapshotJson.put( "creator", snapshot.getProperty( "cm:modifier" ) );

        @SuppressWarnings( "rawtypes" )
        LinkedList<HashMap> list = new LinkedList<HashMap>();
        if(SnapshotPost.hasPdf(snapshot) || SnapshotPost.hasHtmlZip(snapshot)){
        	HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        	String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
        	HashMap<String, String> transformMap;
            if(SnapshotPost.hasPdf(snapshot)){
            	String pdfStatus = SnapshotPost.getPdfStatus(snapshot);
            	if(pdfStatus != null && !pdfStatus.isEmpty()){
	            	EmsScriptNode pdfNode = SnapshotPost.getPdfNode(snapshot);
	            	transformMap = new HashMap<String,String>();
	            	transformMap.put("status", pdfStatus);
	            	transformMap.put("type", "pdf");
	            	if(pdfNode != null){
	            		transformMap.put("url", contextUrl + pdfNode.getUrl());
	            	}
	            	list.add(transformMap);
            	}
            }
            if(SnapshotPost.hasHtmlZip(snapshot)){
            	String htmlZipStatus = SnapshotPost.getHtmlZipStatus(snapshot);
            	if(htmlZipStatus != null && !htmlZipStatus.isEmpty()){
	            	EmsScriptNode htmlZipNode = SnapshotPost.getHtmlZipNode(snapshot);
	            	transformMap = new HashMap<String,String>();
	            	transformMap.put("status", htmlZipStatus);
	            	transformMap.put("type", "html");
	            	if(htmlZipNode != null){
	            		transformMap.put("url", contextUrl + htmlZipNode.getUrl());
	            	}
	            	list.add(transformMap);
            	}
            }
        }
        snapshotJson.put("formats", list);

        JsonArray configsJson = new JsonArray();
        List< EmsScriptNode > configs =
                snapshot.getSourceAssocsNodesByType( "ems:configuredSnapshots",
                                                     workspace, null );
        for (EmsScriptNode config: configs) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("name", config.getProperty(Acm.CM_NAME));
            jsonObject.put("id", config.getNodeRef().getId());
            configsJson.put( jsonObject );
        }

        snapshotJson.put( "configurations", configsJson );

        return snapshotJson;
    }

    public JsonArray getProducts(EmsScriptNode config, WorkspaceNode workspace,
                                 Date timestamp) throws JSONException {
        JsonArray productsJson = new JsonArray();

        List< EmsScriptNode > products =
                config.getTargetAssocsNodesByType( "ems:configuredProducts",
                                                   workspace, timestamp );
        for (EmsScriptNode product: products) {
            productsJson.put( product.toJsonObject(timestamp) );
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
    public JsonObject getProductJson(EmsScriptNode product) throws JSONException {
        JsonObject productJson = new JsonObject();
        productJson.put("sysmlid", product.getSysmlId());
        productJson.put( "created",  EmsScriptNode.getIsoTime( (Date)product.getProperty( "cm:created" )));
        productJson.put( "creator", product.getProperty( "cm:modifier" ) );

        return productJson;
    }


    /**
     * Updates the specified configuration with the posted JSON. Returns set of products to
     * be generated.
     * @param config
     * @param postJson
     * @param context
     * @param date
     * @return
     * @throws JSONException
     */
    public HashSet< String > updateConfiguration( EmsScriptNode config,
                                                  JsonObject postJson,
                                                  EmsScriptNode context,
                                                  WorkspaceNode workspace,
                                                  Date date )
                                                          throws JSONException {
        HashSet<String> productSet = new HashSet<String>();
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
            JsonArray productsJson = postJson.getJSONArray( "products" );
            for (int ii = 0; ii < productsJson.length(); ii++) {
                Object productObject = productsJson.get( ii );
                String productId = "";
                if (productObject instanceof String) {
                    productId = (String) productObject;
                } else if (productObject instanceof JsonObject) {
                    productId = ((JsonObject)productObject).getString( "sysmlid" );
                }
                EmsScriptNode product = findScriptNodeById(productId, workspace, null, false);
                if (product != null) {
                    config.createOrUpdateAssociation( product, "ems:configuredProducts", true );
                    productSet.add( productId );
                }
            }
        } else if (postJson.has( "snapshots" )) {
            config.removeAssociations("ems:configuredSnapshots");
            JsonArray snapshotsJson = postJson.getJSONArray("snapshots");
            for (int ii = 0; ii < snapshotsJson.length(); ii++) {
                Object snapshotObject = snapshotsJson.get( ii );
                String snapshotId = "";
                if (snapshotObject instanceof String) {
                    snapshotId = (String) snapshotObject;
                } else if (snapshotObject instanceof JsonObject) {
                    snapshotId = ((JsonObject)snapshotObject).getString( "id" );
                }
                EmsScriptNode snapshot = findScriptNodeById(snapshotId, workspace, null, false);
                if (snapshot != null) {
                    config.createOrUpdateAssociation(snapshot, "ems:configuredSnapshots", true);
                }
            }
        }

        return productSet;
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

    public void handleDeleteConfiguration( WebScriptRequest req ) {
        String configId = req.getServiceMatch().getTemplateVars().get("configurationId");

        NodeRef configNodeRef = NodeUtil.findNodeRefByAlfrescoId( configId );

        if (configNodeRef != null) {
            EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services, response);
            configNode.makeSureNodeRefIsNotFrozen();
            configNode.addAspect( "ems:Deleted" );
        }
    }

    @Override
    protected Map< String, Object >
            executeImplImpl( WebScriptRequest req, Status status, Cache cache ) {
        // TODO Auto-generated method stub
        return null;
    }
}
