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
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.db.DbContract;
import gov.nasa.jpl.view_repo.db.Node;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 *
 * @author Rahul Kumar
 *
 */
public class Model2Postgres extends AbstractJavaWebScript {

	PostgresHelper pgh = null;

	public Model2Postgres() {
		super();
	}

	public Model2Postgres(Repository repositoryHelper, ServiceRegistry registry) {
		super(repositoryHelper, registry);
	}

	/**
	 * Webscript entry point
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		Model2Postgres instance = new Model2Postgres(repository, getServices());
		return instance.executeImplImpl(req, status, cache);
	}

	@Override
	protected Map<String, Object> executeImplImpl(WebScriptRequest req,
			Status status, Cache cache) {
		printHeader(req);

		Map<String, Object> model = new HashMap<String, Object>();
		JSONObject json = null;

		try {
			if (validateRequest(req, status) || true) {
				WorkspaceNode workspace = getWorkspace(req);
				if (workspace == null)
					pgh = new PostgresHelper("");
				else
					pgh = new PostgresHelper(workspace.getId()
							.replace("-", "_"));
				String timestamp = req.getParameter("timestamp");
				Date dateTime = TimeUtils.dateFromTimestamp(timestamp);
				JSONArray jsonArray = handleSite(workspace, dateTime);
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

	private JSONArray handleSite(WorkspaceNode workspace, Date dateTime)
			throws JSONException {

		JSONArray json = new JSONArray();
		EmsScriptNode emsNode;
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
				emsNode = new EmsScriptNode(siteRef, services);
				name = emsNode.getName();

				if (!name.equals("no_site")
						&& (workspace == null || (workspace != null && workspace
								.contains(emsNode)))) {
					try {
						pgh.connect();

						for (EmsScriptNode n : emsNode.getChildNodes()) {
							if (n.getName().equals("Models")) {
								for (EmsScriptNode c : n.getChildNodes()) {
									int nodesInserted = insertNodes(c, pgh,
											dateTime, edges, documentEdges,
											workspace);
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
				pgh.insertEdge(entry.first, entry.second,
						PostgresHelper.DbEdgeTypes.REGULAR);
			}
			for (Pair<String, String> entry : documentEdges) {
				pgh.insertEdge(entry.first, entry.second,
						PostgresHelper.DbEdgeTypes.DOCUMENT);
			}
			pgh.close();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}

		return json;
	}

	protected int insertNodes(EmsScriptNode n, PostgresHelper pgh, Date dt,
			List<Pair<String, String>> edges,
			List<Pair<String, String>> documentEdges, WorkspaceNode ws) {

		int i = 0;

		pgh.insertNode(n.getNodeRef().toString(),
				NodeUtil.getVersionedRefId(n), n.getSysmlId());

		i++;

		// process node documentation for transclusion links
		String doc = (String) n.getProperty(Acm.ACM_DOCUMENTATION);
		if (doc != null) {
			String MMS_TRANSCLUDE_PATTERN = "\\s*(?i)mms-transclude-doc\\s*mms-eid\\s*=\\s*(\"([^\"]*\"))";
			Pattern pattern = Pattern.compile(MMS_TRANSCLUDE_PATTERN);
			Matcher matcher = pattern.matcher(doc);

			while (matcher.find()) {
				String mmseid = matcher.group(1).replace("\"", "");
				if (mmseid != null)
					documentEdges.add(new Pair<String, String>(n.getSysmlId(),
							mmseid));
			}
		}

		// document edges based on view2view property
		String view2viewProperty = (String) n.getProperty(Acm.ACM_VIEW_2_VIEW);
		if (view2viewProperty != null) {
			JSONArray v2v = new JSONArray(view2viewProperty);
			if (v2v != null) {
				for (int i2 = 0; i2 < v2v.length(); i2++) {
					JSONObject o = v2v.getJSONObject(i2);
					String id = o.getString("id");
					JSONArray childViews = o.getJSONArray("childrenViews");
					for (int j = 0; j < childViews.length(); j++) {
						documentEdges.add(new Pair<String, String>(id,
								childViews.getString(j)));
					}
				}
			}
		}

		for (NodeRef c : n.getOwnedChildren(false, dt, ws)) {
			EmsScriptNode cn = new EmsScriptNode(c, services, response);

			// containment edges
			edges.add(new Pair<String, String>(n.getSysmlId(), cn.getSysmlId()));

			i += insertNodes(new EmsScriptNode(c, services, response), pgh, dt,
					edges, documentEdges, ws);
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
