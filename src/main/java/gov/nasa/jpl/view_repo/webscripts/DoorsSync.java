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

import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import gov.nasa.jpl.mbee.doorsng.DoorsClient;
import gov.nasa.jpl.mbee.doorsng.Requirement;
import gov.nasa.jpl.mbee.doorsng.RequirementCollection;
import gov.nasa.jpl.mbee.doorsng.Folder;

import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.net.URI;

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
public class DoorsSync extends AbstractJavaWebScript {

    PostgresHelper pgh = null;
    DoorsClient doors = null;

    static Logger logger = Logger.getLogger(DoorsSync.class);

    public DoorsSync() {
        super();
    }

    public DoorsSync(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        DoorsSync instance = new DoorsSync(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {

        String projectArea = "Test Project";

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status) || true) {
                WorkspaceNode workspace = getWorkspace(req);
                pgh = new PostgresHelper(workspace);

                doors = new DoorsClient(projectArea);

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

        return model;
    }

    private JSONArray handleSite(WorkspaceNode workspace, Date dateTime, String sitesReq) throws JSONException {
        JSONArray json = new JSONArray();
        EmsScriptNode siteNode;
        String name;
        NodeRef siteRef;
        List<SiteInfo> sites = services.getSiteService().listSites(null);

        for (SiteInfo siteInfo : sites) {
            JSONObject siteJson = new JSONObject();

            siteRef = siteInfo.getNodeRef();

            if (dateTime != null) {
                siteRef = NodeUtil.getNodeRefAtTime(siteRef, dateTime);
            }

            if (siteRef != null) {
                siteNode = new EmsScriptNode(siteRef, services);
                name = siteNode.getName();

                if (workspace == null || (workspace != null && workspace.contains(siteNode))) {
                    try {
                        pgh.connect();

                        for (EmsScriptNode n : siteNode.getChildNodes()) {

                            int nodesInserted = sendToDoors(n, dateTime, name);
                            siteJson.put("sysmlid", name);
                            siteJson.put("name", siteInfo.getTitle());
                            siteJson.put("elementCount", nodesInserted);
                            json.put(siteJson);

                        }

                        pgh.close();

                    } catch (ClassNotFoundException | SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return json;
    }

    protected Integer sendToDoors(EmsScriptNode n, Date dt, String parentId) {
        return sendToDoors(n, dt, parentId, false);
    }

    protected Integer sendToDoors(EmsScriptNode n, Date dt, String parentId, Boolean single) {

        int i = 0;

        ArrayList appliedMetatype = (ArrayList) n.getProperty(Acm.ACM_APPLIED_METATYPES);

        /*
        if(appliedMetatype != null) {
            for(int k = 0; k < appliedMetatype.size() -1; k++) {
                System.out.println(appliedMetatype.get(k));
            }
        }
        */

        if (appliedMetatype != null && appliedMetatype.contains( "_11_5EAPbeta_be00301_1147873190330_159934_2220" )) {
            i += createUpdateRequirement(n, dt);
        }

        try {
            if(!single) {
                for (EmsScriptNode cn : n.getChildNodes()) {
                    String cnSysmlId = cn.getSysmlId().replace( "_pkg",  "" );

                    if (!cn.isDeleted()) {
                        i += sendToDoors(cn, dt, cnSysmlId);
                    }
                }
            }
        } catch ( org.alfresco.service.cmr.repository.MalformedNodeRefException mnre ) {
            logger.error( String.format("could not get children for parent %s:", n.getId()) );
            mnre.printStackTrace();
        }

        return i;
    }

    protected Integer createUpdateRequirement(EmsScriptNode n, Date dt) {

        String resourceUrl = getResourceUrl(n.getSysmlId());

        System.out.println(resourceUrl);

        String sysmlid = (String) n.getProperty(Acm.ACM_ID);
        String name = (String) n.getProperty(Acm.CM_NAME);
        String title = (String) n.getSysmlName();
        String description = (String) n.getProperty(Acm.ACM_DOCUMENTATION);

        Requirement doorsReq = new Requirement();

        doorsReq.setTitle(title);
        doorsReq.setSysmlid(sysmlid);
        doorsReq.setDescription(description);

        if(n.getParent() != null) {
            String parentResourceUrl = getResourceUrl(n.getParent().getSysmlId());
            if(parentResourceUrl == null) {
                //sendToDoors(n, dt, null, true);
            } else {
                doorsReq.setParent(URI.create(parentResourceUrl));
            }
        }

        if(resourceUrl != null) {
            doorsReq.setResourceUrl(resourceUrl);
            doors.update(doorsReq);
        } else {
            resourceUrl = doors.create(doorsReq);
        }

        if (setResourceUrl(resourceUrl, n.getSysmlId())) {
            return 1;
        }

        return 0;
    }

    protected Integer createFolder(EmsScriptNode n, Date dt) {
        return 1;
    }

    protected String getResourceUrl(String sysmlId) {
        try {
            String query = String.format("SELECT resourceUrl FROM doors WHERE sysmlid = '%s'", sysmlId);
            ResultSet rs = pgh.execQuery(query);
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Boolean setResourceUrl(String resourceUrl, String sysmlId) {
        try {
            Map<String, String> values = new HashMap<String, String>();
            values.put("sysmlid", sysmlId);
            values.put("resourceUrl", resourceUrl);

            if (pgh.insert("doors", values) > 0)  {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected Boolean deleteResourceUrl(String resourceUrl) {
        try {
            String query = String.format("DELETE FROM doors WHERE resourceUrl = '%s'", resourceUrl);
            pgh.execQuery(query);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
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
