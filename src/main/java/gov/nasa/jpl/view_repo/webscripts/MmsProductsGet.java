package gov.nasa.jpl.view_repo.webscripts;

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
        clearCaches();

        Map<String, Object> model = new HashMap<String, Object>();
        
        MmsProductsGet instance = new MmsProductsGet(repository, services);
        
        JSONObject jsonObject = new JSONObject();

        try {
            ProductsWebscript productWs = new ProductsWebscript(repository, services, instance.response);
            jsonObject.put("products", productWs.handleProducts(req));
            model.put("res", jsonObject.toString(2));
        } catch (Exception e) {
            model.put("res", response.toString());
            if (e instanceof JSONException) {
                log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                log(LogLevel.ERROR, "Internal server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            e.printStackTrace();
        } 
    
        appendResponseStatusInfo( instance );
        status.setCode(responseStatus.getCode());
    
        return model;
    }
}
