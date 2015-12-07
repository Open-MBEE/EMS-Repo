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

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.doorsng.DoorsClient;
import gov.nasa.jpl.mbee.doorsng.Folder;
import gov.nasa.jpl.mbee.doorsng.Requirement;
import gov.nasa.jpl.mbee.doorsng.RequirementCollection;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 *
 * @author Jason Han
 *
 */
public class DoorsGet extends AbstractJavaWebScript {

	PostgresHelper pgh = null;

	static Logger logger = Logger.getLogger(DoorsGet.class);

	public DoorsGet() {
		super();
	}

	public DoorsGet(Repository repositoryHelper, ServiceRegistry registry) {
		super(repositoryHelper, registry);
	}

	/**
	 * Webscript entry point
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		DoorsGet instance = new DoorsGet(repository, getServices());
		return instance.executeImplImpl(req, status, cache);
	}

	@Override
	protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
		printHeader(req);

		Map<String, Object> model = new HashMap<String, Object>();
		JSONObject json = null;

		try {
			if (validateRequest(req, status) || true) {
				WorkspaceNode workspace = getWorkspace(req);
				pgh = new PostgresHelper(workspace);

				String sitesReq = req.getParameter("sites");
				String timestamp = req.getParameter("timestamp");
				Date dateTime = TimeUtils.dateFromTimestamp(timestamp);
				JSONArray jsonArray = handleSite(workspace, dateTime, sitesReq);
				json = new JSONObject();
				json.put("sites", jsonArray);
			}
		} catch (JSONException e) {
			log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"JSON could not be created\n");
			e.printStackTrace();
		} catch (Exception e) {
			log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Internal error stack trace:\n %s \n",
					e.getLocalizedMessage());
			e.printStackTrace();
		}
		if (json == null) {
			model.put("res", createResponseJson());
		} else {
			model.put("res", NodeUtil.jsonToString(json));
		}
		status.setCode(responseStatus.getCode());

		printFooter();

		return model;
	}

	private JSONArray handleSite(WorkspaceNode workspace, Date dateTime, String sitesReq) throws JSONException {
		JSONArray json = new JSONArray();
		EmsScriptNode siteNode;
		String name;
		NodeRef siteRef;
		List<SiteInfo> sites = services.getSiteService().listSites(null);
		List<Pair<String, String>> edges = new ArrayList<Pair<String, String>>();
		List<Pair<String, String>> documentEdges = new ArrayList<Pair<String, String>>();

		for (SiteInfo siteInfo : sites) {
			JSONObject siteJson = new JSONObject();

			siteRef = siteInfo.getNodeRef();

			if (dateTime != null) {
				siteRef = NodeUtil.getNodeRefAtTime(siteRef, dateTime);
			}

			if (siteRef != null) {
				siteNode = new EmsScriptNode(siteRef, services);
				name = siteNode.getName();

                if (workspace == null || 
                        (workspace != null && workspace.contains(siteNode))) {
					try {
						pgh.connect();

						for (EmsScriptNode n : siteNode.getChildNodes()) {
							if (n.getName().equals("Models")) {
								for (EmsScriptNode c : n.getChildNodes()) {
									int nodesInserted = insertNodes(c, pgh,
											dateTime, edges, documentEdges,
											workspace, name);
									siteJson.put("sysmlid", name);
									siteJson.put("name", siteInfo.getTitle());
									siteJson.put("elementCount", nodesInserted);
									json.put(siteJson);
								}
							}
						}

						pgh.close();

					} catch (ClassNotFoundException | SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}

		try {
			pgh.connect();
			for (Pair<String, String> entry : edges) {
				try {
					pgh.insertEdge(entry.first, entry.second,
							PostgresHelper.DbEdgeTypes.REGULAR);
				} catch (Exception e) {
				}
			}
			for (Pair<String, String> entry : documentEdges) {
				try {
					pgh.insertEdge(entry.first, entry.second,
							PostgresHelper.DbEdgeTypes.DOCUMENT);
				} catch (Exception e) {
				}
			}
			pgh.close();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}

		return json;
	}

	protected int insertNodes(EmsScriptNode n, PostgresHelper pgh, Date dt, List<Pair<String, String>> edges, List<Pair<String, String>> documentEdges, WorkspaceNode ws, String parentId) {

		int i = 0;

		if (!n.getSysmlId().endsWith( "_pkg" )) {
        		pgh.insertNode(n.getNodeRef().toString(),
        				NodeUtil.getVersionedRefId(n), n.getSysmlId());
        
        		i++;
        
        		// documentation edges 
        		String doc = (String) n.getProperty(Acm.ACM_DOCUMENTATION);
        		NodeUtil.processDocumentEdges(n.getSysmlId(), doc, documentEdges);
        		String view2viewProperty = (String) n.getProperty(Acm.ACM_VIEW_2_VIEW);
        		if (view2viewProperty != null) {
        			NodeUtil.processV2VEdges(n.getSysmlId(), new JSONArray(
        					view2viewProperty), documentEdges);
        		}
		}


		try {
//        		for (NodeRef c : n.getOwnedChildren(false, dt, ws)) {
//        			EmsScriptNode cn = new EmsScriptNode(c, services, response);
//        
//        			if (!cn.isDeleted()) {
//            			// containment edges
//        			    String cnSysmlId = cn.getSysmlId().replace( "_pkg", "" );
//            			edges.add(new Pair<String, String>(n.getSysmlId(), cnSysmlId));
//            			i += insertNodes(cn, pgh, dt, edges, documentEdges, ws, cnSysmlId);
//        			}
//        		}
        		// traverse alfresco containment since ownedChildren may not be accurate
            for (EmsScriptNode cn : n.getChildNodes()) {
                String nSysmlId = n.getSysmlId().replace( "_pkg", "" );
                String cnSysmlId = cn.getSysmlId().replace( "_pkg",  "" );

                if (!cn.isDeleted()) {
                    // add containment edges (no _pkg) otherwise results in duplicate edge
                    if (!cn.getSysmlId().endsWith("_pkg") ) {
                        edges.add(new Pair<String, String>(nSysmlId, cnSysmlId));
                    }
                    i += insertNodes(cn, pgh, dt, edges, documentEdges, ws, cnSysmlId);
                }
            }
		} catch ( org.alfresco.service.cmr.repository.MalformedNodeRefException mnre ) {
		    // keep going and ignore
		    logger.error( String.format("could not get children for parent %s:", n.getId()) );
		    mnre.printStackTrace();
		}
		return i;
	}

	/**
	 * Validate the request and check some permissions
	 */
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		if (!checkRequestContent(req)) {
			return false;
		}

		String id = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
		if (!checkRequestVariable(id, WORKSPACE_ID)) {
			return false;
		}

		return true;
	}
}
