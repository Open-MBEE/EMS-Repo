package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.Acm.JSON_TYPE_FILTER;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;
import gov.nasa.jpl.view_repo.webscripts.SnapshotGet;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

    public ProductsWebscript( Repository repository, ServiceRegistry services,
                              StringBuffer response ) {
        super( repository, services, response );
    }

    public JSONArray handleProducts( WebScriptRequest req )
                                                           throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest( req );
        JSONArray productsJson = new JSONArray();

        if ( siteNode != null ) {
            // get timestamp if specified
            String timestamp = req.getParameter( "timestamp" );
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            Set< EmsScriptNode > productSet =
                    WebScriptUtil.getAllNodesInPath( siteNode.getQnamePath(),
                                                     "ASPECT", Acm.ACM_PRODUCT,
                                                     dateTime, services,
                                                     response );
            for ( EmsScriptNode product : productSet ) {
                JSONObject productJson = new JSONObject();
                String productId = (String)product.getProperty( Acm.ACM_ID );

                productJson.put( Acm.JSON_ID, productId );
                productJson.put( Acm.JSON_NAME,
                                 product.getProperty( Acm.ACM_NAME ) );
                productJson.put( "snapshots",
                                 getProductSnapshots( productId,
                                                      req.getContextPath(),
                                                      dateTime ) );

                productsJson.put( productJson );
            }
        }

        return productsJson;
    }

    private JSONArray
            getProductSnapshots( String productId, String contextPath,
                                 Date dateTime ) throws JSONException {
        EmsScriptNode product = findScriptNodeById( productId, dateTime );

        JSONArray snapshotsJson = new JSONArray();
        List< EmsScriptNode > snapshotsList =
                product.getTargetAssocsNodesByType( "view2:snapshots", dateTime );

        Collections.sort( snapshotsList,
                          new EmsScriptNode.EmsScriptNodeComparator() );
        for ( EmsScriptNode snapshot : snapshotsList ) {
            String id = (String)snapshot.getProperty( Acm.ACM_ID );
            Date date = (Date)snapshot.getProperty( Acm.ACM_LAST_MODIFIED );

            JSONObject jsonObject = new JSONObject();
            jsonObject.put( "id", id );
            jsonObject.put( "created", EmsScriptNode.getIsoTime( date ) );
            jsonObject.put( "creator",
                            (String)snapshot.getProperty( "cm:modifier" ) );
            jsonObject.put( "url", contextPath + "/service/snapshots/"
                                   + snapshot.getProperty( Acm.ACM_ID ) );
            jsonObject.put( "tag",
                            (String)SnapshotGet.getConfigurationSet( snapshot,
                                                                     dateTime ) );
            snapshotsJson.put( jsonObject );
        }

        return snapshotsJson;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    public JSONArray handleProduct( String productId, boolean recurse,
                                    Date dateTime,
                                    boolean gettingDisplayedElements,
                                    boolean gettingContainedViews ) {
        JSONArray productsJson = new JSONArray();
        EmsScriptNode product = findScriptNodeById( productId, dateTime );

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
                    for ( EmsScriptNode n : elems ) {
                        productsJson.put( n.toJSONObject( JSON_TYPE_FILTER.ELEMENT ) );
                    }
                } else if ( gettingContainedViews ) {
                    Collection< EmsScriptNode > elems =
                            v.getContainedViews( recurse, null );
                    for ( EmsScriptNode n : elems ) {
                        productsJson.put( n.toJSONObject( JSON_TYPE_FILTER.VIEW ) );
                    }
                } else {
                    productsJson.put( product.toJSONObject( JSON_TYPE_FILTER.PRODUCT ) );
                }
            } catch ( JSONException e ) {
                log( LogLevel.ERROR, "Could not create products JSON array",
                     HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                e.printStackTrace();
            }
        }

        return productsJson;
    }
}
