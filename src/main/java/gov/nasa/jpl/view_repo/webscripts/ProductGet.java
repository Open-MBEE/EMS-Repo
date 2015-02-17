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

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ProductsWebscript;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

// TODO -- this should be a subclass of ViewGet
public class ProductGet extends AbstractJavaWebScript {
	protected boolean gettingDisplayedElements = false;
    protected boolean gettingContainedViews = false;

    // injected via spring configuration
    protected boolean isViewRequest = false;

    protected boolean prettyPrint = true;

    public ProductGet() {
	    super();
	}

    public ProductGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String productId = AbstractJavaWebScript.getIdFromRequest(req);
		if (!checkRequestVariable(productId, "id")) {
			return false;
		}

		// get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

		EmsScriptNode product = findScriptNodeById(productId, workspace, dateTime, false);
		if (product == null) {
			log(LogLevel.ERROR, "Product not found with id: " + productId + ".\n", HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		if (!checkPermissions(product, PermissionService.READ)) {
			return false;
		}

		return true;
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		ProductGet instance = new ProductGet(repository, getServices());
		return instance.executeImplImpl(req, status, cache, runWithoutTransactions);
	}

	@Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

		//clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();

		JSONArray productsJson = null;
		if (validateRequest(req, status)) {
			String productId = getIdFromRequest( req );
			gettingDisplayedElements = isDisplayedElementRequest( req );
			if ( !gettingDisplayedElements ) {
			    gettingContainedViews  = isContainedViewRequest( req );
			}
			if (Debug.isOn()) System.out.println("productId = " + productId);

			// default recurse=true but recurse only applies to displayed elements and contained views
            boolean recurse = getBooleanArg( req, "recurse", true );

            // default simple=false
            boolean simple = getBooleanArg( req, "simple", false );
            //System.out.println("simple=" + simple);

            // get timestamp if specified
            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            WorkspaceNode workspace = getWorkspace( req );

            // see if prettyPrint default is overridden and change
            prettyPrint = getBooleanArg(req, "pretty", prettyPrint );
            //System.out.println("prettyPrint=" + prettyPrint);

            ProductsWebscript productsWs = new ProductsWebscript(repository, services, response);
            productsWs.simpleJson = simple;
			productsJson = productsWs.handleProduct(productId, recurse, workspace, dateTime, gettingDisplayedElements, gettingContainedViews);
		}

		if (responseStatus.getCode() == HttpServletResponse.SC_OK && productsJson != null) {
			try {
			    JSONObject json = new JSONObject();
                json.put( gettingDisplayedElements ? "elements"
                                                  : ( gettingContainedViews
                                                      ? "views" : "products" ),
                         productsJson );
                if (!Utils.isNullOrEmpty(response.toString())) json.put("message", response.toString());
                if ( prettyPrint ) model.put("res", NodeUtil.jsonToString( json, 4 ));
                else model.put("res", NodeUtil.jsonToString( json ));
			} catch (JSONException e) {
				log(LogLevel.ERROR, "JSON creation error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				model.put("res", response.toString());
                e.printStackTrace();
			}
		} else {
			model.put("res", response.toString());
		}

		status.setCode(responseStatus.getCode());

		printFooter();

		return model;
	}


    /**
     * Need to differentiate between View or Element request - specified during Spring configuration
     * @param flag
     */
    public void setIsViewRequest(boolean flag) {
        isViewRequest = flag;
    }

}
