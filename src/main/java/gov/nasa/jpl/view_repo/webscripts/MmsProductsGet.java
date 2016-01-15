package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ProductsWebscript;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsProductsGet extends AbstractJavaWebScript {
    public MmsProductsGet() {
        super();
    }

    public MmsProductsGet( Repository repository, ServiceRegistry services ) {
        this.repository = repository;
        this.services = services;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		MmsProductsGet instance = new MmsProductsGet(repository, getServices());
		return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
//        AuthenticationUtil.setRunAsUser( "admin" );
        printHeader( req );

        //clearCaches();
        Map<String, Object> model = new HashMap<String, Object>();
        
        // Checks mms versions
        if(checkMmsVersions)
        {
        	if(compareMmsVersions(req, getResponse(), getResponseStatus())){
		    	model.put("res", createResponseJson());
		    	return model;
        	}
        }
        
        MmsProductsGet instance = new MmsProductsGet(repository, getServices());

        JSONObject jsonObject = NodeUtil.newJsonObject();

        try {
            ProductsWebscript productWs = new ProductsWebscript(repository, getServices(), instance.response);
            jsonObject.put("products", productWs.handleProducts(req));
            appendResponseStatusInfo( instance );
            if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
            model.put("res", NodeUtil.jsonToString( jsonObject, 2 ));
        } catch (Exception e) {
            model.put("res", createResponseJson());
            if (e instanceof JSONException) {
                log(Level.ERROR,  HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON creation error");
            } else {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error" );
            }
            e.printStackTrace();
        }
        

        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }
}
