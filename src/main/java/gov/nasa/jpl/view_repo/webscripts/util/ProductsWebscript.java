package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import gov.nasa.jpl.view_repo.webscripts.SnapshotGet;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import gov.nasa.jpl.view_repo.util.JsonArray;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.JsonObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Library class for manipulating products
 *
 * Pattern is handle does the retrieval.
 *
 * @author cinyoung
 *
 */
public class ProductsWebscript extends AbstractJavaWebScript {

    public boolean simpleJson = false;
    private EmsScriptNode sitePackageNode = null;

    public ProductsWebscript( Repository repository, ServiceRegistry services,
                              StringBuffer response ) {
        super( repository, services, response );
    }

    public JsonArray handleProducts( WebScriptRequest req )
                                                           throws JSONException {
        JsonArray productsJson = new JsonArray();

        EmsScriptNode siteNode = null;
        EmsScriptNode mySiteNode = getSiteNodeFromRequest( req, false );
        WorkspaceNode workspace = getWorkspace( req );
        String siteName = getSiteName(req);

        if (!NodeUtil.exists( mySiteNode )) {
            log(LogLevel.WARNING, "Could not find site", HttpServletResponse.SC_NOT_FOUND);
            return productsJson;
        }

        // Find the project site and site package node if applicable:
        Pair<EmsScriptNode,EmsScriptNode> sitePair = findProjectSite(req, siteName, workspace, mySiteNode);
        if (sitePair == null) {
            return productsJson;
        }

        sitePackageNode = sitePair.first;
        siteNode = sitePair.second;  // Should be non-null

        String configurationId = req.getServiceMatch().getTemplateVars().get( "configurationId" );
        if (configurationId == null) {
            // if no configuration id, get all products for site/context
            return handleContextProducts(req, siteNode);
        } else {
            // if configuration exists, get products for configuration
            NodeRef configNodeRef = NodeUtil.getNodeRefFromNodeId( configurationId );
            if (configNodeRef != null) {
                EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services);
                return handleConfigurationProducts(req, configNode);
            } else {
                log(LogLevel.WARNING, "Could not find configuration with id " + configurationId, HttpServletResponse.SC_NOT_FOUND);
                return productsJson;
            }
        }
    }


    public JsonArray handleConfigurationProducts( WebScriptRequest req, EmsScriptNode config) throws JSONException {
        String timestamp = req.getParameter( "timestamp" );
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

        ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
        return configWs.getProducts( config, workspace, dateTime );
    }

    public JsonArray handleContextProducts( WebScriptRequest req, EmsScriptNode siteNode) throws JSONException {
        JsonArray productsJson = new JsonArray();

        // get timestamp if specified
        String timestamp = req.getParameter( "timestamp" );
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        WorkspaceNode workspace = getWorkspace( req );

        // Search for all products within the project site:
        Map< String, EmsScriptNode > nodeList = searchForElements(NodeUtil.SearchType.ASPECT.prefix,
                                                                Acm.ACM_PRODUCT, false,
                                                                workspace, dateTime,
                                                                siteNode.getName());
        if (nodeList != null) {

            boolean checkSitePkg = (sitePackageNode != null && sitePackageNode.exists());
            // Get the alfresco Site for the site package node:
            EmsScriptNode pkgSite = checkSitePkg ? getSiteForPkgSite(sitePackageNode, workspace) : null;

            Set<EmsScriptNode> nodes = new HashSet<EmsScriptNode>(nodeList.values());
            for ( EmsScriptNode node : nodes) {
                if (node != null) {
                    // If we are just retrieving the products for a site package, then filter out the ones
                    // that do not have the site package as the first site package parent:
                    if (checkSitePkg) {
                        if (pkgSite != null &&
                            pkgSite.equals(findParentPkgSite(node, siteNode, null, workspace))) {
                            productsJson.put( node.toJsonObject( null ) );
                        }
                    }
                    else {
                        productsJson.put( node.toJsonObject( null ) );
                    }
                }
            }
        }

        return productsJson;
    }

    public JsonArray
            getProductSnapshots( String productId, String contextPath,
                                 WorkspaceNode workspace, Date dateTime ) throws JSONException {
        EmsScriptNode product = findScriptNodeById( productId, workspace,
                                                    dateTime, false );

        JsonArray snapshotsJson = new JsonArray();
        List< EmsScriptNode > snapshotsList =
                product.getTargetAssocsNodesByType( "view2:snapshots",
                                                    workspace, null );
        // lets add products from node refs
        List<NodeRef> productSnapshots = product.getPropertyNodeRefs( "view2:productSnapshots" );
        for (NodeRef productSnapshotNodeRef: productSnapshots) {
            EmsScriptNode productSnapshot = new EmsScriptNode(productSnapshotNodeRef, services, response);
            snapshotsList.add( productSnapshot );
        }

        Collections.sort( snapshotsList,
                          new EmsScriptNode.EmsScriptNodeComparator() );
        for ( EmsScriptNode snapshot : snapshotsList ) {
            if (!snapshot.isDeleted()) {
                String id = snapshot.getSysmlId();
                Date date = snapshot.getLastModified( dateTime );

                JsonObject jsonObject = new JsonObject();
                jsonObject.put( "id", id );
                jsonObject.put( "created", EmsScriptNode.getIsoTime( date ) );
                jsonObject.put( "creator",
                                snapshot.getProperty( "cm:modifier" ) );
                jsonObject.put( "url", contextPath + "/service/snapshots/"
                                       + snapshot.getSysmlId() );
                jsonObject.put( "tag", SnapshotGet.getConfigurationSet( snapshot,
                                                                                workspace,
                                                                                dateTime ) );
                snapshotsJson.put( jsonObject );
            }
        }

        return snapshotsJson;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    public JsonArray handleProduct( String productId, boolean recurse,
                                    WorkspaceNode workspace,
                                    Date dateTime,
                                    boolean gettingDisplayedElements,
                                    boolean gettingContainedViews ) {
        JsonArray productsJson = new JsonArray();
        EmsScriptNode product = findScriptNodeById( productId, workspace, dateTime, false );

        if ( product == null ) {
            log( LogLevel.ERROR, "Product not found with ID: " + productId,
                 HttpServletResponse.SC_NOT_FOUND );
        }

        if ( checkPermissions( product, PermissionService.READ ) ) {
            try {
                View v = new View( product );
                if ( gettingDisplayedElements ) {
                    Collection< EmsScriptNode > elems =
                            v.getDisplayedElements();
                    elems = NodeUtil.getVersionAtTime( elems, dateTime );
                    for ( EmsScriptNode n : elems ) {
                        if ( simpleJson ) {
                            productsJson.put( n.toSimpleJsonObject( dateTime ) );
                        } else {
                            productsJson.put( n.toJsonObject( dateTime ) );
                        }
                    }
                } else if ( gettingContainedViews ) {
                    Collection< EmsScriptNode > elems =
                            v.getContainedViews( recurse, workspace, dateTime, null );
                    elems.add( product );
                    for ( EmsScriptNode n : elems ) {
                        if ( simpleJson ) {
                            productsJson.put( n.toSimpleJsonObject( dateTime ) );
                        } else {
                            productsJson.put( n.toJsonObject( dateTime ) );
                        }
                    }
                } else {
                    productsJson.put( product.toJsonObject( dateTime ) );
                }
            } catch ( JSONException e ) {
                log( LogLevel.ERROR, "Could not create JSON for product",
                     HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                e.printStackTrace();
            }
        }

        return productsJson;
    }

    @Override
    protected Map< String, Object >
            executeImplImpl( WebScriptRequest req, Status status, Cache cache ) {
        // TODO Auto-generated method stub
        return null;
    }
}
