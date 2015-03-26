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

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Model search service that returns a JSONArray of elements
 * @author cinyoung
 *
 */
public class ModelSearch extends ModelGet {
	public ModelSearch() {
	    super();
	}


    public ModelSearch(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    protected final String[] searchTypes = {
	        "@sysml\\:documentation:\"",
	        "@sysml\\:name:\"",
	        "@sysml\\:id:\"",
	        "@sysml\\:string:\"",
	        "@sysml\\:body:\""
    };


	@Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ModelSearch instance = new ModelSearch(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

	@Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

		clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();

		try {
	        JSONArray elementsJson = executeSearchRequest(req);

	        JSONObject top = NodeUtil.newJsonObject();
			top.put("elements", elementsJson);
			if (!Utils.isNullOrEmpty(response.toString())) top.put("message", response.toString());
			model.put("res", NodeUtil.jsonToString( top, 4 ));
		} catch (JSONException e) {
			log(LogLevel.ERROR, "Could not create the JSON response", HttpServletResponse.SC_BAD_REQUEST);
			model.put("res", createResponseJson());
			e.printStackTrace();
		}

		status.setCode(responseStatus.getCode());

		printFooter();

		return model;
	}


	private JSONArray executeSearchRequest(WebScriptRequest req) throws JSONException {
        String keyword = req.getParameter("keyword");
        if (keyword != null) {
            // get timestamp if specified
            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            Map<String, EmsScriptNode> rawResults = new HashMap<String, EmsScriptNode>();

            WorkspaceNode workspace = getWorkspace( req );

            for (String searchType: searchTypes) {
                rawResults.putAll( searchForElements( searchType, keyword, false,
                                                         workspace, dateTime ) );
            }

            // need to filter out _pkgs:
            for (Entry< String, EmsScriptNode > element: rawResults.entrySet()) {

                if (!element.getValue().getSysmlId().endsWith( "_pkg" )) {
                    elementsFound.put( element.getKey(), element.getValue() );
                }
//                NodeRef nodeRef = NodeUtil.findNodeRefById( element.getKey(), false, workspace, dateTime, services, true );
//                if ( nodeRef.equals( element.getValue().getNodeRef() ) ) {
//                    if (!element.getValue().getSysmlId().endsWith( "_pkg" )) {
//                        elementsFound.put( element.getKey(), element.getValue() );
//                    }
//                }
            }

            handleElements(workspace, dateTime, true);
        }

        return elements;
	}
}
