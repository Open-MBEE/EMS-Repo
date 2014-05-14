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

import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.Acm.JSON_TYPE_FILTER;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ProductGet extends AbstractJavaWebScript {
	protected boolean gettingContainedElements = false;


    public ProductGet() {
	    super();
	}
    
    public ProductGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }
	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		String productId = req.getServiceMatch().getTemplateVars().get("id");
		if (!checkRequestVariable(productId, "id")) {
			return false;
		}
		
		EmsScriptNode product = findScriptNodeById(productId);
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
        printHeader( req );

		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();

		JSONArray productsJson = null;
		if (validateRequest(req, status)) {
			String productId = req.getServiceMatch().getTemplateVars().get("id");
			gettingContainedElements  = ( productId.toLowerCase().trim().endsWith("/elements") );
			if ( gettingContainedElements ) {
			    productId = productId.substring( 0, productId.lastIndexOf( "/elements" ) );
			}
			System.out.println("Got id = " + productId);
			productsJson = handleProduct(productId);
		}

		if (responseStatus.getCode() == HttpServletResponse.SC_OK && productsJson != null) {
			try {
			    JSONObject top = new JSONObject();
			    top.put("products", productsJson);
				model.put("res", top.toString(4));
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

	
	public Collection<EmsScriptNode> getDisplayedElements(EmsScriptNode product) {
	    ArrayList<EmsScriptNode> nodes = new ArrayList<EmsScriptNode>();
        Object elementValue = product.getProperty( Acm.ACM_VIEW_2_VIEW );
        if ( elementValue instanceof JSONArray ) {
            JSONArray jarr = (JSONArray)elementValue;
            for ( int i=0; i<jarr.length(); ++i ) {
                Object o = null;
                try {
                    o = jarr.get( i );
                } catch ( JSONException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if ( o instanceof JSONObject ) {
                    JSONObject jo = (JSONObject)o;
                    if ( jo.has( "id" ) ) {
                        String id = null;
                        try {
                            id = jo.getString( "id" );
                            if ( id != null && id.equals( product.getProperty( Acm.ACM_ID ) ) ) {
                                if ( jo.has( "childrenViews" ) ) {
                                    Object childrenViews = null;
                                    childrenViews = jo.getString( "childrenViews" );
                                    JSONArray jarr2 = null;
                                    if ( childrenViews instanceof String ) {
                                        jarr2 = new JSONArray( (String)childrenViews );
                                    } else if ( childrenViews instanceof JSONArray ) {
                                        jarr2 = (JSONArray)childrenViews;
                                    } else if ( childrenViews instanceof Collection ) {
                                        jarr2 = new JSONArray( (Collection<?>)childrenViews );
                                    }
                                    if ( jarr2 != null ) {
                                        for ( int j=0; j<jarr2.length(); ++j ) {
                                            Object oo = jarr2.get( j );
                                            if ( oo instanceof String ) {
                                                NodeRef ref = NodeUtil.findNodeRefById( (String)oo, services );
                                                if ( ref != null ) {
                                                    View v = new View( new EmsScriptNode( ref, services ) );
                                                    // TODO -- need to check for infinite loop!
                                                    nodes.addAll( v.getDisplayedElements() );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch ( JSONException e ) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return nodes;
	}
	
	private JSONArray handleProduct(String productId) {
	    JSONArray productsJson = new JSONArray();
		EmsScriptNode product = findScriptNodeById(productId);
		if (product == null) {
			log(LogLevel.ERROR, "Product not found with ID: " + productId, HttpServletResponse.SC_NOT_FOUND);
		}

		if (checkPermissions(product, PermissionService.READ)){ 
            try {
                if ( gettingContainedElements ) {
                    Collection< EmsScriptNode > elems = getDisplayedElements( product );
                    for ( EmsScriptNode n : elems ) {
                        productsJson.put( n.toJSONObject( JSON_TYPE_FILTER.ELEMENT ) );
                    }
                } else {
                    productsJson.put( product.toJSONObject( Acm.JSON_TYPE_FILTER.PRODUCT ) );
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
