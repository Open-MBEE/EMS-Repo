/**
 * 
 */
package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.K;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Get a K string for an element. If the element is a Package, get K for the
 * contents of the Package.
 */
public class KGet extends ModelGet {

    static Logger logger = Logger.getLogger(KGet.class);

    protected boolean prettyPrint = true;

    String timestamp;
    Date dateTime;
    WorkspaceNode workspace;

    public KGet() {
        super();
        System.out.println("KGet()");
    }

    public KGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
        System.out.println("KGet(repo, services)");
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        System.out.println("KGet.validateRequest()");
        return super.validateRequest( req, status );
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript#executeImplImpl(org.springframework.extensions.webscripts.WebScriptRequest, org.springframework.extensions.webscripts.Status, org.springframework.extensions.webscripts.Cache)
     */
    @Override
    protected Map< String, Object >
            executeImpl( WebScriptRequest req, Status status, Cache cache ) {
        System.out.println("KGet.executeImpl()");
        KGet instance = new KGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache, runWithoutTransactions );
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
                                                  Status status, Cache cache) {
        System.out.println("KGet.executeImplImpl()");
        
        if (logger.isDebugEnabled()) {
            String user = AuthenticationUtil.getFullyAuthenticatedUser();
            logger.debug(user + " " + req.getURL());
        }
        printHeader(req);

        Map<String, Object> model = super.executeImplImpl( req, status, cache );
        Object result = model.get( "res" );
        JSONObject json = null;
        try {
            json = new JSONObject(result.toString());
        } catch ( JSONException e ) {}

        
        String k = null;
        if ( json != null ) {
            k = K.jsonToK( json );
        } else if ( result instanceof String ) {
            k = "--- " + ( (String)result ).replaceAll( "\n", "\n--- " ) + "\n" + k;
        }
        model.put("res", k);
      
//        if (validateRequest(req, status)) {
//            try {
//                k = handleRequest(req, NodeUtil.doGraphDb);
//            } catch ( JSONException e ) {
//                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request");
//                e.printStackTrace();
//            }
//        }

//        try {
//            if (!Utils.isNullOrEmpty( k )) {
//                if (!Utils.isNullOrEmpty(response.toString())) {
//                    k = "--- " + response.toString().replaceAll( "\n", "\n--- " ) + "\n" + k;
//                }
//                //if ( prettyPrint ) model.put("res", k);
//                model.put("res", k);
//            } else {
//                log(Level.WARN,
//                    HttpServletResponse.SC_NOT_FOUND, "No elements found");
//                k = "--- " + response.toString().replaceAll( "\n", "\n--- " ) + "\n" + k;
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        status.setCode(responseStatus.getCode());

        return model;
    }

    /**
     * This is an edit of a copy of ModelsGet.
     * 
     * Wrapper for handling a request and getting the appropriate JSONArray of elements
     * @param req
     * @return
     */
    private String handleRequest(WebScriptRequest req) throws JSONException {
        System.out.println("KGet.handleRequest()");
        JSONObject requestJson = //JSONObject.make( 
                (JSONObject)req.parseContent();// );
        String elementsFoundJson = null;

        JSONArray elementsToFindJson;
        elementsToFindJson = requestJson.getJSONArray( "elements" );

        ArrayList<EmsScriptNode> elements = new ArrayList< EmsScriptNode >(); 
        for (int ii = 0; ii < elementsToFindJson.length(); ii++) {
            String id = elementsToFindJson.getJSONObject( ii ).getString( "sysmlid" );
            EmsScriptNode node = NodeUtil.findScriptNodeById( id, workspace, dateTime, false, services, response );
            if (node != null) {
                elements.add( node );
            }
        }
        
        String k = K.toK( elements );

        return k;
    }

}
