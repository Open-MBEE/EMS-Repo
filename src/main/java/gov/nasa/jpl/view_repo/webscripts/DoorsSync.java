/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S. Government sponsorship
 * acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.io.InputStream;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import gov.nasa.jpl.view_repo.db.DoorsPostgresHelper;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.alfresco.service.cmr.repository.NodeRef;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.client.exception.ResourceNotFoundException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import gov.nasa.jpl.mbee.doorsng.DoorsClient;
import gov.nasa.jpl.mbee.doorsng.model.Requirement;
import gov.nasa.jpl.mbee.doorsng.model.Folder;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.db.Node;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

/**
 * 
 * @author Jason Han
 * 
 */
public class DoorsSync extends AbstractJavaWebScript {

    PostgresHelper pgh = null;
    DoorsClient doors = null;
    String rootProjectId = null;
    EmsScriptNode siteNode;

    List<String> processedRequirements = new ArrayList<String>();
    List<String> processedProjects = new ArrayList<String>();
    List<Map<String, String>> customFields = new ArrayList<Map<String, String>>();
    String currentProject;

    static Logger logger = Logger.getLogger(DoorsSync.class);

    HashMap<String, HashMap<String, ArrayList<String>>> artifactMappings =
                    new HashMap<String, HashMap<String, ArrayList<String>>>();

    HashMap<String, String> matchingReqArtifactTypeMap = new HashMap<String, String>();
    String currentMatchingArtifactType = "";
    // String currentProject = "";


    ArrayList<String> curAppliedMetatypeIds = new ArrayList<String>();

    // TODO remove
    // ArrayList<Requirement> doorsRequirementsWithLinks = new ArrayList<Requirement>();
    HashMap<String, String> doorsRequirementsWithLinks = new HashMap<String, String>();
    HashMap<String, String> doorsRequirementProjectMap = new HashMap<String, String>();

    String syncConfigurationMessage = "";



    public DoorsSync() {
        super();
    }

    public DoorsSync(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    interface HelloWorld {
        public void greet();

        public void greetSomeone(String someone);
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

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        if (!NodeUtil.doorsSync) {
            json = new JSONObject();
            json.put("status", "DoorsSync is off");
            model.put("res", NodeUtil.jsonToString(json));
            return model;
        }

        // logger.setLevel(Level.DEBUG);
        String[] idKeys = {"modelid", "elementid", "elementId"};
        String modelId = null;
        for (String idKey : idKeys) {
            modelId = req.getServiceMatch().getTemplateVars().get(idKey);
            if (modelId != null) {
                break;
            }
        }

        if (validateRequest(req, status) || true) {
            WorkspaceNode workspace = getWorkspace(req);
            pgh = new PostgresHelper(workspace);

            String timestamp = req.getParameter("timestamp");
            Date dateTime = TimeUtils.dateFromTimestamp(timestamp);

            try {
                pgh.connect();

                if (modelId == null) {

                    JSONArray jsonArray = new JSONArray();

                    if (!syncConfigurationOk()) {
                        json = new JSONObject();
                        json.put("Configuration Error:", syncConfigurationMessage);
                    } else {
                        jsonArray = handleRequirements(workspace, dateTime);
                        json = new JSONObject();
                        json.put("sites", jsonArray);
                    }

                } else {
                    EmsScriptNode modelRootNode = null;
                    Node nodeFromPostgres = pgh.getNodeFromSysmlId(modelId);
                    if (nodeFromPostgres != null) {
                        modelRootNode = NodeUtil.getNodeFromPostgresNode(nodeFromPostgres);
                    }

                    if (modelRootNode == null) {
                        modelRootNode = findScriptNodeById(modelId, workspace, dateTime, false);
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("modelRootNode = " + modelRootNode);
                    }

                    if (modelRootNode == null) {
                        if (logger.isDebugEnabled()) {
                            logger.error(HttpServletResponse.SC_NOT_FOUND + String.format(" Element %s not found",
                                            modelId + (dateTime == null ? "" : " at " + dateTime)));
                        }
                        return model;
                    } else if (modelRootNode.isDeleted()) {
                        if (logger.isDebugEnabled()) {
                            logger.error(HttpServletResponse.SC_GONE + " Element exists, but is deleted.");
                        }
                        return model;
                    }
                    String project = modelRootNode.getSiteName(dateTime, workspace);
                    doors = new DoorsClient(getConfig("doors.user"), getConfig("doors.pass"), getConfig("doors.url"),
                                    project);

                    customFields = mapFields(project);
                    compRequirement(modelRootNode, null);

                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(modelRootNode.getSysmlId());
                    json = new JSONObject();
                    json.put("element", jsonArray);
                }
            } catch (JSONException e) {
                logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + " JSON could not be created\n");
                e.printStackTrace();
            } catch (Exception e) {
                logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                                + String.format(" Internal error stack trace:\n %s \n", e.getLocalizedMessage()));
                e.printStackTrace();
            } finally {
                pgh.close();
            }
        }

        if (json == null) {
            model.put("res", createResponseJson());
        } else {
            model.put("res", NodeUtil.jsonToString(json));
        }
        status.setCode(responseStatus.getCode());

        return model;
    }

    private JSONArray handleRequirements(WorkspaceNode workspace, Date dateTime) {

        JSONArray json = new JSONArray();

        ArrayList<String> types = new ArrayList<String>();

        types.add("@sysml\\:appliedMetatypes:\"");

        Map<String, EmsScriptNode> foundMMSRequirements = new HashMap<String, EmsScriptNode>();

        Map<String, EmsScriptNode> requirements = new HashMap<String, EmsScriptNode>();

        HashMap<String, ArrayList<String>> curArtifactMappings = new HashMap<String, ArrayList<String>>();

        String curArtifactType = "";

        ArrayList<String> curAppliedMetatypeIDs = new ArrayList<String>();

        String curAppliedMetatypeID = "";

        DoorsPostgresHelper dpgh = new DoorsPostgresHelper();
        artifactMappings = dpgh.getArtifactMappings();

        if (artifactMappings.size() > 0) {

            String curProj = "";

            Set<Map.Entry<String, HashMap<String, ArrayList<String>>>> setOfProjArtifactMappings =
                            artifactMappings.entrySet();

            // Traverse all project, artifact type, and appliedmetatype id
            // mappings and search MMS for each appliedmetatype id
            // Also filter on project
            for (Map.Entry<String, HashMap<String, ArrayList<String>>> curProjArtifactMapping : setOfProjArtifactMappings) {

                curArtifactMappings = curProjArtifactMapping.getValue();

                curProj = curProjArtifactMapping.getKey();

                Set<Map.Entry<String, ArrayList<String>>> setOfArtifactMappings = curArtifactMappings.entrySet();

                for (Map.Entry<String, ArrayList<String>> curArtifactMapping : setOfArtifactMappings) {

                    curArtifactType = curArtifactMapping.getKey();

                    curAppliedMetatypeIDs = curArtifactMapping.getValue();

                    for (int i = 0; i < curAppliedMetatypeIDs.size(); i++) {

                        curAppliedMetatypeID = curAppliedMetatypeIDs.get(i);

                        foundMMSRequirements = searchForElementsPostgres(types, curAppliedMetatypeID, false, workspace,
                                        dateTime, null, null);

                        for (Map.Entry<String, EmsScriptNode> curMMSRequirement : foundMMSRequirements.entrySet()) {

                            // Filter on project name
                            if (curMMSRequirement.getValue().getProjectNode(workspace).getSysmlName().equals(curProj)) {

                                // match found
                                requirements.put(curMMSRequirement.getKey(), curMMSRequirement.getValue());


                                matchingReqArtifactTypeMap.put(curMMSRequirement.getValue().getSysmlId(),
                                                curArtifactType);


                            }

                        }

                    }

                }

            }

        }

        // use ...2220 if no project/appliedmetatype mappings were found in
        // database
        else {

            requirements = searchForElementsPostgres(types, "_11_5EAPbeta_be00301_1147873190330_159934_2220", false,
                            workspace, dateTime, null, null);
        }

        for (String key : requirements.keySet()) {

            currentMatchingArtifactType = matchingReqArtifactTypeMap.get(key); // to support custom
                                                                               // artifact types

            JSONObject projectJson = null;
            projectJson = new JSONObject();

            EmsScriptNode requirementNode = requirements.get(key);
            EmsScriptNode projectNode = requirementNode.getProjectNode(null);
            projectJson.put("project", projectNode.getSysmlName());
            rootProjectId = projectNode.getSysmlId();
            customFields = mapFields(projectNode.getSysmlName());

            try {
                if (doors == null || currentProject != projectNode.getSysmlName()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Logging in Doors");
                    doors = new DoorsClient(getConfig("doors.user"), getConfig("doors.pass"), getConfig("doors.url"),
                                    projectNode.getSysmlName(), currentMatchingArtifactType); // new
                                                                                              // constructor
                                                                                              // for
                                                                                              // custom
                                                                                              // artifact
                                                                                              // types

                    currentProject = projectNode.getSysmlName();

                    // doors

                }
                if (!processedProjects.contains(currentProject)) {
                    processedProjects.add(currentProject);
                    syncFromDoors();
                    // syncLinksFromDoors();
                }
                if (!processedRequirements.contains(requirementNode.getSysmlId())) {
                    compRequirement(requirementNode, null);
                }
                projectJson.put("status", "Sync Complete");
            } catch (ClassNotFoundException | SQLException e) {
                if (logger.isDebugEnabled()) {
                    e.printStackTrace();
                }
            } catch (ResourceNotFoundException e) {
                projectJson.put("status", "Project not found");
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    e.printStackTrace();
                }
                projectJson.put("status", "Issue during sync");
            }
            json.put(projectJson);
        }

        if (requirements.size() == 0) {

            try {
                ResultSet doorsartifactmappings = pgh.execQuery("SELECT * from doorsartifactmappings");

                while (doorsartifactmappings.next()) {

                    currentProject = doorsartifactmappings.getString(1);
                    checkDoorsProjWhenEmptyMMS(doorsartifactmappings.getString(1));

                }

                // syncLinksFromDoors();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {

            syncLinksFromMMS(requirements);

        }

        return json;
    }

    /***
     * Author: Bruce Meek Jr Description: For each matching requirement element found in MMS from
     * the handleRequirements method, find all source/target link associations If two MMS nodes
     * share the same link element, create a link relationship between the two corresponding Doors
     * artifacts based on database mappings
     * 
     * @param requirements
     */
    private void syncLinksFromMMS(Map<String, EmsScriptNode> requirements) {

        Map<String, String> artifactResourceMap = new HashMap<String, String>();
        ArrayList<NodeRef> curNodeSrcReferences = new ArrayList<NodeRef>();
        ArrayList<NodeRef> curNodeTgtReferences = new ArrayList<NodeRef>();
        HashMap<String, String> linkMetatypeIDMap = new HashMap<String, String>(); // link sysml id
                                                                                   // to its applied
                                                                                   // metatype id
        ArrayList<String> curLinkMetatypeIDs = new ArrayList<String>();

        EmsScriptNode newEmsScriptNode = null;
        NodeRef newRefNode = null;
        StringBuffer sb = null;

        HashMap<String, String> sourceMap = new HashMap<String, String>(); // link
                                                                           // sysmlid
                                                                           // to
                                                                           // src
                                                                           // element
                                                                           // sysmlid
        HashMap<String, String> targetMap = new HashMap<String, String>(); // link
                                                                           // sysmlid
                                                                           // to
                                                                           // tgt
                                                                           // element
                                                                           // sysmlid
        Set<Map.Entry<String, EmsScriptNode>> matchingRequirements = requirements.entrySet();

        String project = "";

        try {

            ResultSet doorsArtifacts = pgh.execQuery("SELECT * from doors");

            while (doorsArtifacts.next()) {

                artifactResourceMap.put(doorsArtifacts.getString(1), doorsArtifacts.getString(2));

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {

            for (Map.Entry<String, EmsScriptNode> curElementNode : matchingRequirements) {

                curNodeSrcReferences =
                                curElementNode.getValue().getPropertyNodeRefs("sysml:relAsSource", true, null, null);

                curNodeTgtReferences =
                                curElementNode.getValue().getPropertyNodeRefs("sysml:relAsTarget", true, null, null);

                // link nodes found in which current element is a src
                for (int sr = 0; sr < curNodeSrcReferences.size(); sr++) {

                    sb = new StringBuffer();

                    newRefNode = curNodeSrcReferences.get(sr);

                    newEmsScriptNode = new EmsScriptNode(newRefNode, services, sb);

                    curLinkMetatypeIDs = (ArrayList<String>) newEmsScriptNode.getProperty(Acm.ACM_APPLIED_METATYPES);

                    linkMetatypeIDMap.put((String) newEmsScriptNode.getProperty(Acm.ACM_ID), curLinkMetatypeIDs.get(0));

                    sourceMap.put((String) newEmsScriptNode.getProperty(Acm.ACM_ID),
                                    (String) curElementNode.getValue().getProperty(Acm.ACM_ID));

                }

                // link nodes found in which current element is a tgt
                for (int tr = 0; tr < curNodeTgtReferences.size(); tr++) {

                    sb = new StringBuffer();

                    newRefNode = curNodeTgtReferences.get(tr);

                    newEmsScriptNode = new EmsScriptNode(newRefNode, services, sb);

                    // only need once
                    if (tr == 0) {

                        project = newEmsScriptNode.getProjectNode(null).getSysmlName();

                    }

                    curLinkMetatypeIDs = new ArrayList<String>();

                    curLinkMetatypeIDs = (ArrayList<String>) newEmsScriptNode.getProperty(Acm.ACM_APPLIED_METATYPES);

                    linkMetatypeIDMap.put((String) newEmsScriptNode.getProperty(Acm.ACM_ID), curLinkMetatypeIDs.get(0));

                    targetMap.put((String) newEmsScriptNode.getProperty(Acm.ACM_ID),
                                    (String) curElementNode.getValue().getProperty(Acm.ACM_ID));

                }

            }

            Set<Map.Entry<String, String>> sourceMapSet = sourceMap.entrySet();
            Set<Map.Entry<String, String>> targetMapSet = targetMap.entrySet();
            String curLink = "";

            for (Map.Entry<String, String> curSource : sourceMapSet) {

                curLink = curSource.getKey();

                for (Map.Entry<String, String> curTarget : targetMapSet) {

                    // Two elements share a link; link them based on database
                    // mappings
                    if (curTarget.getKey().equals(curLink)) {

                        Requirement source = doors.getRequirement(artifactResourceMap.get(curSource.getValue()));

                        source.setResourceUrl(artifactResourceMap.get(curSource.getValue()));

                        ResultSet customLinkMappings = pgh.execQuery("select uri from doorsartifactlinkmappings"
                                        + " where project ='" + project + "' and sysmlappliedmetatypeid ='"
                                        + linkMetatypeIDMap.get(curLink) + "'");

                        while (customLinkMappings.next()) {

                            doors.addCustomLinkToExistingRequirement(source.getResourceUrl(),
                                            artifactResourceMap.get(curTarget.getValue()),
                                            customLinkMappings.getString(1));

                        }

                    }

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }


    /***
     * Author: Bruce Meeks Jr Description: Handles case where there are requirements in a DoorsNG
     * project but no requirement elements in the corresponding MMS project
     */
    protected JSONArray checkDoorsProjWhenEmptyMMS(String doorsProjectArea) {

        JSONArray result = new JSONArray();
        JSONObject projectJson = new JSONObject();

        try {

            doors = new DoorsClient(getConfig("doors.user"), getConfig("doors.pass"), getConfig("doors.url"),
                            doorsProjectArea);

            if (doors.getRequirements().length > 0) {

                ResultSet nodes = pgh.execQuery("select * from nodes where sysmlid LIKE '%PROJECT%'");

                while (nodes.next()) {

                    if (nodes.getString(5).contains("PROJECT-")) {
                        if (NodeUtil.getNodeFromPostgresNode(pgh.getNodeFromSysmlId(nodes.getString(5))).getSysmlName()
                                        .equals(doorsProjectArea)) {
                            rootProjectId = nodes.getString(5);
                            customFields = mapFields(doorsProjectArea);
                            break;
                        }
                    }

                }

                syncFromDoors();

                projectJson = new JSONObject();
                projectJson.put("project", doorsProjectArea);
                projectJson.put("status", "Sync Complete");
                result.put(projectJson);
                return result;

            }

        } catch (Exception e) {
            projectJson.put("status", "Issue syncing from Doors");
            result.put(projectJson);
            return result;

        }

        return result;
    }


    protected void syncFromDoors() {

        Requirement[] requirements = doors.getRequirements();
        String currentArtifactType = "";

        for (Requirement curRequirement : requirements) {

            try {

                // dynamic artifact types
                currentArtifactType = doors.getArtifactType(curRequirement, currentProject);

                // tailoring doors client for the current artifact type
                doors = new DoorsClient(getConfig("doors.user"), getConfig("doors.pass"), getConfig("doors.url"),
                                currentProject, currentArtifactType);


            } catch (Exception e) {
                e.printStackTrace();
            }

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Adding/Updating from Doors: %s", curRequirement.getTitle()));
            }


            // only sync DNG artifacts whose project and artifact type mappings have been specified
            // by user
            if (artifactMappings.containsKey(currentProject)) {
                if (artifactMappings.get(currentProject).containsKey(currentArtifactType)) {

                    curAppliedMetatypeIds = artifactMappings.get(currentProject).get(currentArtifactType);

                    compRequirement(null, curRequirement);


                }
            }


        }

    }

    /***
     * Author: Bruce Meeks Jr Description: For recently synched over requirements DNG -> MMS,
     * iterate through those who also have link relationships in DNG and sync over to MMS
     */
    protected void syncLinksFromDoors() {

        JSONObject postJson = null;
        JSONArray elements = null;
        Requirement curReq = null;
        String curReqURL = null;

        JSONObject linkElement = null;
        JSONObject linkSpecialization = null;

        /*
         * for (Map.Entry<String, HashMap<String, ArrayList<String>>> curProjArtifactMapping :
         * setOfProjArtifactMappings) {
         * 
         * 
         * 
         * 
         * } for (int r = 0; r < doorsRequirementsWithLinks.size(); r++) {
         * 
         * linkElement = new JSONObject(); linkSpecialization = new JSONObject();
         * 
         * //for each link target artifact found for current artifact for(int m=0; m<
         * getArtifactLinkCount(curReq.getResourceUrl()); m++){ //loop through current requirement
         * linkSpecialization.put("source",
         * doorsRequirementsWithLinks.get(r).getCustomField(doors.getField("sysmlid")) );
         * linkSpecialization.put("type", "Dependency");
         * //linkSpecialization.put("target",doors.getArtifactLinkTarget());
         * 
         * curReq = doorsRequirementsWithLinks.get(r); curReqURL = curReq.getResourceUrl();
         * 
         * }
         * 
         * System.out.println(curReqURL);
         * 
         * 
         * }
         */

        /*
         * JSONObject postJson = new JSONObject(); JSONArray elements = new JSONArray();
         * 
         * JSONObject linkElement = new JSONObject(); JSONObject linkSpecialization = new
         * JSONObject(); JSONArray linkAppliedMetatypes = new JSONArray();
         * 
         * //String newLinkSysmlId = generateSysmlId();
         * 
         * 
         * linkElement.put("owner","PROJECT-4d70ac30-8ce9-4140-b705-ccd86d6f2e89"); //get project ID
         * dynamically linkElement.put("creator","admin"); linkElement.put("isMetatype","admin");
         * linkElement.put("isMetatype",false); linkElement.put("name", "newLink2");
         * 
         * //check if link exists already String sysmlId =
         * mapResourceUrl("https://doors-ng-uat.jpl.nasa.gov:9443/rm/resources/_newlink"); if
         * (sysmlId == null) { sysmlId = generateSysmlId(); }
         * 
         * linkElement.put("sysmlid",sysmlId); linkElement.put("documentation","");
         * 
         * linkSpecialization.put("source", "_18_0_5_2fa0136_1471188555791_337580_14441");
         * linkSpecialization.put("type", "Dependency");
         * linkSpecialization.put("target","_18_0_5_2fa0136_1470894228437_372885_14319");
         * 
         * linkElement.put("specialization",linkSpecialization);
         * 
         * linkAppliedMetatypes.put("_17_0_5beta_17530432_1382584914591_39608_11809");
         * linkAppliedMetatypes.put("_9_0_62a020a_1105704885806_49506_8067");
         * 
         * linkElement.put("appliedMetatypes",linkAppliedMetatypes);
         * 
         * 
         * elements.put(linkElement); postJson.put("elements",elements);
         * 
         * boolean newLinkGood = handleElementUpdate(postJson) ;
         */

        // boolean mappedGood =
        // mapResourceUrl(newLinkSysmlId,"https://doors-ng-uat.jpl.nasa.gov:9443/rm/resources/_newlink");

        // if(mapResourceUrl(newLinkSysmlId,"https://doors-ng-uat.jpl.nasa.gov:9443/rm/resources/_newlink"))
        // {

        // }


        /*
         * if (mapResourceUrl(newLinkSysmlId,
         * "https://doors-ng-uat.jpl.nasa.gov:9443/rm/resources/_WRyhYXIfEeaRI-62F4SlNA")) {
         * 
         * }
         */

        doorsRequirementsWithLinks.clear(); //

    }

    protected void compRequirement(EmsScriptNode n, Requirement r) {

        String resourceUrl = null;
        String sysmlId = null;

        try {

            ResultSet doorsflags = pgh.execQuery("SELECT * from doorsflags where toggletype = 'allchanges';");

            while (doorsflags.next()) {
                if (doorsflags.getString(2).equals("ON")) {
                    DoorsChangesFlag.syncChanges = true;
                } else {
                    DoorsChangesFlag.syncChanges = false;
                }
            }

            doorsflags = pgh.execQuery("SELECT * from doorsflags where toggletype = 'containment';");

            while (doorsflags.next()) {
                if (doorsflags.getString(2).equals("ON")) {
                    DoorsContainmentFlag.syncContainment = true;
                } else {
                    DoorsContainmentFlag.syncContainment = false;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (n == null && r != null) {
            n = getNodeFromDoors(r);
        }

        if (n != null) {
            // In MMS
            sysmlId = n.getSysmlId();
            resourceUrl = mapResourceUrl(sysmlId);

            if (resourceUrl != null) {
                if (n.isDeleted()) {
                    doors.delete(resourceUrl);
                    deleteResourceUrl(resourceUrl);
                } else {
                    if (r == null) {
                        r = doors.getRequirement(resourceUrl);
                        r.setResourceUrl(resourceUrl);
                    }
                    Date lastSync = getLastSynced(sysmlId);
                    Date lastModifiedMMS = getTrueModified(n);
                    Date lastModifiedDoors = r.getModified();

                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("lastSync Date: %s", lastSync));
                        logger.debug(String.format("Doors Date: %s", lastModifiedDoors));
                        logger.debug(String.format("MMS Date: %s", lastModifiedMMS));
                    }

                    if ((lastSync.compareTo(lastModifiedDoors) < 0) && (lastSync.compareTo(lastModifiedMMS) >= 0)
                                    || (containmentChanged(n, r) && DoorsContainmentFlag.syncContainment)) {
                        // Modified in Doors, Not Modified in MMS
                        if (logger.isDebugEnabled()) {
                            logger.debug(sysmlId + " - Modified in Doors, Not Modified in MMS");
                        }

                        if (DoorsChangesFlag.syncChanges) {
                            resourceUrl = createUpdateRequirementFromDoors(r);
                        }
                    } else if ((lastSync.compareTo(lastModifiedDoors) >= 0)
                                    && (lastSync.compareTo(lastModifiedMMS) >= 0)) {
                        // Not Modified in Doors, Not Modified in MMS
                        if (logger.isDebugEnabled()) {
                            logger.debug(sysmlId + " - Not Modified in Doors, Not Modified in MMS");
                        }
                        processedRequirements.add(sysmlId);
                        // resourceUrl = createUpdateRequirementFromMMS( n );
                    } else if ((lastSync.compareTo(lastModifiedDoors) >= 0)
                                    && (lastSync.compareTo(lastModifiedMMS) < 0)) {
                        // Not Modified in Doors, Modified in MMS
                        if (logger.isDebugEnabled()) {
                            logger.debug(sysmlId + " - Not Modified in Doors, Modified in MMS");
                        }
                        resourceUrl = createUpdateRequirementFromMMS(n);
                    } else if ((lastSync.compareTo(lastModifiedDoors) < 0)
                                    && (lastSync.compareTo(lastModifiedMMS) < 0)) {
                        // Modified in Doors, Modified in MMS
                        if (logger.isDebugEnabled()) {
                            logger.debug(sysmlId + " - Modified in Doors, Modified in MMS");
                        }
                        // Conflict detected, for now get from Doors
                        if (DoorsChangesFlag.syncChanges) {
                            resourceUrl = createUpdateRequirementFromDoors(r);
                        }
                    }
                }

            } else {
                // Not in Doors
                if (logger.isDebugEnabled()) {
                    logger.debug(sysmlId + " - Not in Doors");
                }
                createUpdateRequirementFromMMS(n);
            }
        } else if (r != null) {
            // Not in MMS
            if (logger.isDebugEnabled()) {
                logger.debug(r.getTitle() + " - Not in MMS");
            }

            if (DoorsChangesFlag.syncChanges) {
                createUpdateRequirementFromDoors(r);
            }

        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Nothing Happened. What?");
            }
        }

    }

    protected boolean containmentChanged(EmsScriptNode n, Requirement r) {

        boolean reqHierarchyChanged = false;

        Set<EmsScriptNode> result = new LinkedHashSet<EmsScriptNode>();

        EmsScriptNode p = n.getOwningParent(null, null, false);
        while (p != null) {
            if (p.hasAspect(Acm.ACM_PACKAGE) && !p.getSysmlId().endsWith("_pkg") && p.getSysmlName() != null) {
                result.add(p);
                p = p.getOwningParent(null, null, false);
            } else {
                p = null;
            }
        }

        EmsScriptNode esnArray[] = result.toArray(new EmsScriptNode[result.size()]);

        Set<Folder> folders = getFolderHierarchyFromDoors(r.getParent());

        Folder doorsFolderArray[] = folders.toArray(new Folder[folders.size()]);

        if (esnArray.length != doorsFolderArray.length) {
            return true;
        }

        for (int i = 0; i < doorsFolderArray.length; i++) {

            if (!doorsFolderArray[i].getTitle().equals(esnArray[i].getSysmlName())) {
                reqHierarchyChanged = true;
                break;
            }

        }

        return reqHierarchyChanged;
    }

    protected String createUpdateRequirementFromMMS(EmsScriptNode n) {

        String sysmlId = n.getSysmlId();
        String title = n.getSysmlName();

        Date lastModified = (Date) n.getProperty(Acm.ACM_LAST_MODIFIED);
        String resourceUrl = mapResourceUrl(sysmlId);
        String description = (String) n.getProperty(Acm.ACM_DOCUMENTATION);
        Set<EmsScriptNode> folders = getFolderHierarchyFromMMS(n);

        String parentResourceUrl = null;
        EmsScriptNode[] folderArray = folders.toArray(new EmsScriptNode[folders.size()]);
        for (Integer i = folderArray.length - 1; i >= 0; i--) {
            parentResourceUrl = createFolderFromMMS(folderArray[i]);
        }

        Requirement doorsReq = new Requirement();

        doorsReq.setTitle(title);
        doorsReq.setDescription(description);
        doorsReq.setModified(lastModified);

        doorsReq = addSlotsFromMMS(n, doorsReq);
        // doorsReq.setCustomField(doors.getField("sysmlid"), sysmlId);

        // handle custom artifact types
        doorsReq.setCustomField(doors.getField("sysmlid"), sysmlId);


        if (parentResourceUrl != null) {
            doorsReq.setParent(URI.create(parentResourceUrl));
        }

        if (resourceUrl != null) {
            doorsReq.setResourceUrl(resourceUrl);
            doors.update(doorsReq);
        } else {

            // Comment out for now, until doors.getFolders() issue resolved
            // Avoid duplicate artifact creations in DNG
            /*
             * boolean duplicate = false;
             * 
             * Requirement[] existingRequirements = doors.getRequirements();
             * 
             * for ( int i = 0; i < existingRequirements.length; i++ ) { if ( sysmlId.equals(
             * existingRequirements[ i ].getCustomField( doors.getField( "sysmlid" )))) { duplicate
             * = true; break; } }
             * 
             * if ( !duplicate ) { resourceUrl = doors.create( doorsReq ); }
             */


            if (matchingReqArtifactTypeMap.size() > 0) {
                resourceUrl = doors.create(doorsReq, matchingReqArtifactTypeMap.get(sysmlId));

            } else {
                resourceUrl = doors.create(doorsReq);

            }

        }

        if (mapResourceUrl(sysmlId, resourceUrl)) {
            processedRequirements.add(sysmlId);
            return resourceUrl;
        }

        return null;
    }

    protected String createUpdateRequirementFromDoors(Requirement r) {

        JSONObject postJson = new JSONObject();
        JSONArray elements = new JSONArray();

        JSONObject specElement = new JSONObject();
        JSONArray specAppliedMetatypes = new JSONArray();
        JSONObject specSpecialization = new JSONObject();
        JSONArray specClassifier = new JSONArray();

        JSONObject reqElement = new JSONObject();
        JSONArray reqAppliedMetatypes = new JSONArray();
        JSONObject reqSpecialization = new JSONObject();

        EmsScriptNode existing = getNodeFromDoors(r);

        String specSysmlId = null;
        String sysmlId = null;

        if (existing == null) {
            specSysmlId = generateSysmlId();
            sysmlId = r.getCustomField(doors.getField("sysmlid"));
            if (sysmlId == null) {
                sysmlId = mapResourceUrl(r.getResourceUrl());
                if (sysmlId == null) {
                    sysmlId = generateSysmlId();
                }
            }
        } else {
            EmsScriptNode instance = existing.getInstanceSpecification();
            specSysmlId = instance.getSysmlId();
            sysmlId = existing.getSysmlId();
        }

        String description = r.getDescription();
        String reqParent = mapResourceUrl(r.getParent());

        if (!DoorsContainmentFlag.syncContainment) {

            if (reqParent == null) {
                reqParent = rootProjectId;

                Set<Folder> folders = getFolderHierarchyFromDoors(r.getParent());
                Folder[] folderArray = folders.toArray(new Folder[folders.size()]);
                for (Integer i = folderArray.length - 1; i >= 0; i--) {
                    reqParent = createFolderFromDoors(folderArray[i].getTitle(), reqParent,
                                    folderArray[i].getResourceUrl());
                }
            } else {
                if (existing != null) {
                    reqParent = existing.getParent().getSysmlId().replace("_pkg", "");
                }
            }

        } else {

            reqParent = rootProjectId;

            Set<Folder> folders = getFolderHierarchyFromDoors(r.getParent());
            Folder[] folderArray = folders.toArray(new Folder[folders.size()]);
            for (Integer i = folderArray.length - 1; i >= 0; i--) {
                reqParent = createFolderFromDoors(folderArray[i].getTitle(), reqParent,
                                folderArray[i].getResourceUrl());
            }
        }

        specElement.put("name", "");
        specElement.put("sysmlid", specSysmlId);
        specElement.put("owner", sysmlId);
        specElement.put("documentation", "");
        specAppliedMetatypes.put("_9_0_62a020a_1105704885251_933969_7897");
        specElement.put("appliedMetatypes", specAppliedMetatypes);

        for (int a = 0; a < curAppliedMetatypeIds.size(); a++) {
            specClassifier.put(curAppliedMetatypeIds.get(a));
        }

        specSpecialization.put("classifier", specClassifier);
        specSpecialization.put("type", "InstanceSpecification");
        specElement.put("specialization", specSpecialization);
        specElement.put("isMetatype", false);

        elements.put(specElement);

        for (Map<String, String> fieldDef : customFields) {
            String slotSysmlId = specSysmlId + "-slot-" + fieldDef.get("propertyId");

            JSONObject slotElement = new JSONObject();
            JSONObject slotSpecialization = new JSONObject();
            JSONArray slotValues = new JSONArray();
            JSONObject slotValue = new JSONObject();

            String value = null;

            if (fieldDef.get("doorsAttr").contains("primaryText")) {
                value = r.getPrimaryText();
            } else {
                value = r.getCustomField(doors.getField(fieldDef.get("doorsAttr")));
            }

            if (value != null) {

                slotElement.put("sysmlid", slotSysmlId);
                slotElement.put("owner", specSysmlId);
                slotSpecialization.put("isDerived", false);
                slotSpecialization.put("isSlot", true);
                slotSpecialization.put("propertyType", fieldDef.get("propertyType"));

                slotValue.put(fieldDef.get("propertyType"), value);
                slotValue.put("type", "LiteralString");

                slotValues.put(slotValue);

                slotSpecialization.put("value", slotValues);
                slotSpecialization.put("type", "Property");

                slotElement.put("specialization", slotSpecialization);

                elements.put(slotElement);
            }
        }

        reqElement.put("name", r.getTitle());
        reqElement.put("sysmlid", sysmlId);
        reqElement.put("owner", reqParent);
        reqElement.put("documentation", description);

        for (int a = 0; a < curAppliedMetatypeIds.size(); a++) {
            reqAppliedMetatypes.put(curAppliedMetatypeIds.get(a));
        }

        reqAppliedMetatypes.put("_9_0_62a020a_1105704885343_144138_7929");
        reqElement.put("appliedMetatypes", reqAppliedMetatypes);
        reqSpecialization.put("type", "Element");
        reqElement.put("isMetatype", false);

        elements.put(reqElement);
        postJson.put("elements", elements);

        if (handleElementUpdate(postJson)) {
            if (mapResourceUrl(sysmlId, r.getResourceUrl())) {
                setLastSynced(sysmlId, r.getModified());
                return sysmlId;
            }
        }

        // if requirement just created/updated has link relationships in DNG
        //TODO complete from syncing new links from DNG -> MMS
        /*
         * int curRequirementLinkCount = doors.artifactHasLinks(r.getResourceUrl(), currentProject);
         * 
         * if (curRequirementLinkCount > 0) {
         * 
         * doorsRequirementsWithLinks.put(r.getResourceUrl(),String.valueOf(curRequirementLinkCount)
         * ); doorsRequirementProjectMap.put(r.getResourceUrl(), rootProjectId);
         * 
         * }
         */

        return sysmlId;
    }

    protected String createFolderFromMMS(EmsScriptNode n) {

        // Check if user wants to map to specific folder
        boolean specificFolderOn = false;
        String specificFolder = "";

        try {

            ResultSet result = pgh.execQuery("SELECT * from doorsprojectfoldermappings");

            while (result.next()) {

                if (currentProject.equals(result.getString(1))) {
                    specificFolderOn = true;
                    specificFolder = result.getString(2);
                    break;
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (n.getSysmlName() == null) {
            return null;
        }

        if (logger.isDebugEnabled())
            logger.debug("Folder Sysmlid: " + n.getSysmlId());

        String resourceUrl = mapResourceUrl(n.getSysmlId());
        if (logger.isDebugEnabled())
            logger.debug("Folder Resource: " + resourceUrl);

        if (resourceUrl == null) {

            String parentResourceUrl = mapResourceUrl(n.getParent().getSysmlId().replace("_pkg", ""));

            Folder folder = new Folder();
            folder.setTitle(n.getSysmlName());
            folder.setDescription(n.getSysmlId());

            if (parentResourceUrl != null) {
                folder.setParent(URI.create(parentResourceUrl));
            } else if ((parentResourceUrl == null) && specificFolderOn) {
                folder.setParent(URI.create(getConfig("doors.url") + "/folders/" + specificFolder));

            }

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Updating/Creating Folder in Doors: %s", n.getSysmlName()));
            }

            // Commented out until doors.getFolders() is resolved
            // Avoid duplicate artifact creations in DNG
            /*
             * boolean duplicate = false;
             * 
             * Folder[] existingFolders = doors.getFolders();
             * 
             * for ( int i = 0; i < existingFolders.length; i++ ) { if ( existingFolders[ i
             * ].getDescription() .equals( n.getSysmlId() ) ) { duplicate = true; break; } }
             * 
             * if ( !duplicate ) { resourceUrl = doors.create( folder );
             * 
             * if ( mapResourceUrl( n.getSysmlId(), resourceUrl ) ) {
             * 
             * return resourceUrl;
             * 
             * } else { // Couldn't map for whatever reason, delete the resource. doors.delete(
             * resourceUrl ); } }
             */

            resourceUrl = doors.create(folder);

            if (mapResourceUrl(n.getSysmlId(), resourceUrl)) {

                return resourceUrl;

            } else {
                // Couldn't map for whatever reason, delete the resource.
                doors.delete(resourceUrl);
            }

        }

        return resourceUrl;
    }

    protected String createFolderFromDoors(String name, String parent, String resourceUrl) {

        JSONObject postJson = new JSONObject();
        JSONArray elements = new JSONArray();

        JSONObject specElement = new JSONObject();
        JSONObject specSpecialization = new JSONObject();
        String sysmlId = mapResourceUrl(resourceUrl); // if new folder,
                                                      // sysmlid will be null

        // TODO try moving if around the generateSysml, post/update folder info
        // regardless
        // TODO update its sysml only if its a new folder
        // TODO maybe add condition for containment
        /*
         * if ( sysmlId == null ) {
         * 
         * sysmlId = generateSysmlId();
         * 
         * specElement.put( "name", name ); specElement.put( "sysmlid", sysmlId ); specElement.put(
         * "owner", parent ); specSpecialization.put( "type", "Package" ); specElement.put(
         * "specialization", specSpecialization );
         * 
         * elements.put( specElement ); postJson.put( "elements", elements );
         * 
         * if ( handleElementUpdate( postJson ) ) { if ( mapResourceUrl( sysmlId, resourceUrl ) ) {
         * return sysmlId; } } }
         */

        if (sysmlId == null) {
            sysmlId = generateSysmlId();
        }

        specElement.put("name", name);
        specElement.put("sysmlid", sysmlId);
        specElement.put("owner", parent);
        specSpecialization.put("type", "Package");
        specElement.put("specialization", specSpecialization);

        elements.put(specElement);
        postJson.put("elements", elements);

        if (handleElementUpdate(postJson)) {
            if (mapResourceUrl(sysmlId, resourceUrl)) {
                return sysmlId;
            }
        }

        return sysmlId;
    }

    protected Set<EmsScriptNode> getFolderHierarchyFromMMS(EmsScriptNode n) {

        Set<EmsScriptNode> result = new LinkedHashSet<EmsScriptNode>();

        EmsScriptNode p = n.getOwningParent(null, null, false);
        while (p != null) {
            if (p.hasAspect(Acm.ACM_PACKAGE) && !p.getSysmlId().endsWith("_pkg") && p.getSysmlName() != null) {
                result.add(p);
                p = p.getOwningParent(null, null, false);
            } else {
                p = null;
            }
        }

        return result;
    }

    protected Set<Folder> getFolderHierarchyFromDoors(String resourceUrl) {

        Set<Folder> result = new LinkedHashSet<Folder>();
        String sysmlId = mapResourceUrl(resourceUrl);

        // TODO uncomment
        /*
         * if ( sysmlId == null ) { Folder folder = doors.getFolder( resourceUrl ); while (
         * !folder.getTitle().equals( "root" ) ) { if ( !folder.getTitle().equals( "root" ) ) {
         * result.add( folder ); folder = doors.getFolder( folder.getParent() ); } } }
         */

        // TODO remove?
        Folder folder = doors.getFolder(resourceUrl);
        while (!folder.getTitle().equals("root")) {
            if (!folder.getTitle().equals("root")) {
                result.add(folder);
                folder = doors.getFolder(folder.getParent());
            }
        }

        return result;
    }

    protected EmsScriptNode getNodeFromDoors(Requirement r) {

        EmsScriptNode n = null;

        String sysmlId = r.getCustomField(doors.getField("sysmlid"));
        if (sysmlId != null) {
            Node nodeFromPostgres = pgh.getNodeFromSysmlId(sysmlId);
            if (nodeFromPostgres != null) {
                n = NodeUtil.getNodeFromPostgresNode(nodeFromPostgres);
            }
            if (n == null) {
                n = findScriptNodeById(sysmlId, null, null, false);
            }
        }

        return n;
    }

    protected EmsScriptNode[] getAllSlots(EmsScriptNode n) {

        EmsScriptNode instanceSpec = n.getInstanceSpecification();

        Collection<EmsScriptNode> slots = new HashSet<EmsScriptNode>();

        if (instanceSpec != null) {
            for (Map<String, String> fieldDef : customFields) {
                String propertySysmlId = instanceSpec.getSysmlId() + "-slot-" + fieldDef.get("propertyId");
                Node nodeFromPostgres = pgh.getNodeFromSysmlId(propertySysmlId);
                EmsScriptNode in = null;
                if (nodeFromPostgres != null) {
                    in = NodeUtil.getNodeFromPostgresNode(nodeFromPostgres);
                    if (in == null) {
                        in = findScriptNodeById(propertySysmlId, null, null, false);
                    }
                }

                if (in != null) {
                    slots.add(in);
                }
            }
        }

        return (slots.isEmpty()) ? null : slots.toArray(new EmsScriptNode[slots.size()]);
    }

    protected Requirement addSlotsFromMMS(EmsScriptNode n, Requirement r) {

        EmsSystemModel esm = new EmsSystemModel();
        EmsScriptNode[] childProps = getAllSlots(n);

        if (childProps != null) {
            for (EmsScriptNode cn : childProps) {
                // slot id has format {element_id}-slot-{slot type} so get
                // slotId
                String[] tokens = cn.getSysmlId().split("-");
                if (tokens.length <= 0)
                    continue;

                String curSlotType = tokens[tokens.length - 1];

                // TODO: Do we need to follow ElementValue references? e.g.
                // "value": [{
                // "element": "_18_0_2_f4e036d_1430175587664_988897_16535",
                // "type": "ElementValue"
                // }]
                // get JSON for slot, so we can get the value
                JSONObject slotJson = cn.toJSONObject(null, null, null, false, false, null);
                Object value = null;
                if (slotJson.has("specialization")) {
                    JSONObject specializationJson = slotJson.getJSONObject("specialization");
                    if (specializationJson.has("value")) {
                        JSONArray valuesJson = specializationJson.getJSONArray("value");
                        for (int ii = 0; ii < valuesJson.length(); ii++) {
                            JSONObject valueJson = valuesJson.getJSONObject(ii);
                            for (Object key : valueJson.keySet()) {
                                if (key instanceof String) {
                                    String keyString = (String) key;
                                    if (!keyString.equals("type")) {
                                        value = valueJson.get(keyString);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                for (Map<String, String> fieldDef : customFields) {
                    if (fieldDef.get("doorsAttr").contains("primaryText")
                                    && curSlotType.contains(fieldDef.get("propertyId"))) {
                        r.setPrimaryText((String) value);
                        break;
                    } else if (curSlotType.contains(fieldDef.get("propertyId"))) {
                        r.setCustomField(doors.getField(fieldDef.get("doorsAttr")), value);
                        break;
                    }
                }


                // TODO: Should revert to this once ESM is fixed
                // String curSlotType = cn.getSysmlId();
                // for ( Map< String, String > fieldDef : customFields ) {
                // Collection< Object > value =
                // esm.getValue( esm.getProperty( cn, "value" ),
                // fieldDef.get( "propertyType" ) );
                // if ( value.iterator().hasNext() ) {
                //
                // if ( fieldDef.get( "doorsAttr" )
                // .contains( "primaryText" )
                // && curSlotType.contains( fieldDef.get( "propertyId" ) ) ) {
                //
                // r.setPrimaryText( value.iterator().next()
                // .toString() );
                // } else if ( curSlotType.contains( fieldDef.get( "propertyId"
                // ) ) ) {
                // r.setCustomField( doors.getField( fieldDef.get( "doorsAttr" )
                // ),
                // value.iterator().next() );
                // }
                // }
                // }

            }

        }

        // no child slots found, because element's properties did not have applied metatype ids
        else {

            JSONObject nj = n.toJSONObject(null, null, null, false, false, null);

            Object value = null;

            EmsScriptNode curProperty = null;
            JSONObject curPropertyJSON = null;

            if (nj.has("ownedAttribute")) {

                JSONArray arrayOfProperties = nj.getJSONArray("ownedAttribute");

                for (int prop = 0; prop < arrayOfProperties.length(); prop++) {

                    curProperty = NodeUtil.getNodeFromPostgresNode(
                                    pgh.getNodeFromSysmlId((String) arrayOfProperties.get(prop)));

                    curPropertyJSON = curProperty.toJSONObject(null, null, null, false, false, null);

                    JSONObject specializationJson = curPropertyJSON.getJSONObject("specialization");
                    if (specializationJson.has("value")) {
                        JSONArray valuesJson = specializationJson.getJSONArray("value");
                        for (int ii = 0; ii < valuesJson.length(); ii++) {
                            JSONObject valueJson = valuesJson.getJSONObject(ii);
                            for (Object key : valueJson.keySet()) {
                                if (key instanceof String) {
                                    String keyString = (String) key;
                                    if (!keyString.equals("type")) {
                                        value = valueJson.get(keyString);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    try {
                        if (doors.doesAttributeExist(matchingReqArtifactTypeMap.get(n.getSysmlId()),
                                        curProperty.getSysmlName())) {

                            r.setCustomField(doors.getField(curProperty.getSysmlName()), String.valueOf(value));

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }



            }


        }

        return r;
    }

    protected Date getTrueModified(EmsScriptNode n) {

        Date modified = n.getLastModified(null);
        EmsScriptNode[] slots = getAllSlots(n);
        if (slots != null) {
            for (EmsScriptNode cn : slots) {
                Date cnModified = cn.getLastModified(null);

                if (modified.compareTo(cnModified) < 0) {
                    modified = cnModified;
                }
            }
        }

        return modified;
    }

    protected Boolean handleElementUpdate(JSONObject postJson) {

        Map<String, Object> model = new HashMap<String, Object>();
        Status status = new Status();
        ModelPost modelPost = new ModelPost(repository, getServices());

        try {

            modelPost.handleUpdate(postJson, status, null, false, false, model, true, true);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    protected String mapResourceUrl(String identifier) {

        try {
            String query = String.format(
                            "SELECT resourceUrl,sysmlId FROM doors WHERE sysmlId = '%1$s' OR resourceUrl = '%1$s'",
                            identifier);
            ResultSet rs = pgh.execQuery(query);
            if (rs.next()) {
                if (rs.getString(2).equals(identifier)) {
                    return rs.getString(1);
                } else {
                    return rs.getString(2);
                }
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected Boolean mapResourceUrl(String sysmlId, String resourceUrl) {

        try {
            Map<String, String> values = new HashMap<String, String>();
            values.put("sysmlid", sysmlId);
            values.put("resourceUrl", resourceUrl);

            if (mapResourceUrl(sysmlId) != null) {
                pgh.execUpdate(String.format("UPDATE doors SET lastSync = current_timestamp WHERE sysmlid = '%s'",
                                sysmlId));
                return true;
            } else {
                if (pgh.insert("doors", values) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected Date getLastSynced(String sysmlId) {

        try {
            String query = String.format("SELECT lastSync FROM doors WHERE sysmlid = '%s'", sysmlId);
            ResultSet rs = pgh.execQuery(query);
            if (rs.next()) {
                return rs.getTimestamp(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected void setLastSynced(String sysmlId, Date lastSync) {

        try {
            Timestamp ts = new Timestamp(lastSync.getTime() + 1000);

            String query = String.format("UPDATE doors SET lastSync = '%1$TD %1$TT' WHERE sysmlid = '%2$s'", ts,
                            sysmlId);
            pgh.execUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected Boolean deleteResourceUrl(String value) {

        try {
            String query = String.format("DELETE FROM doors WHERE resourceUrl = '%1$s' OR sysmlId = '%1$s'", value);
            pgh.execQuery(query);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected List<Map<String, String>> mapFields(String project) {

        try {

            Map<String, String> values = new HashMap<String, String>();

            List<Map<String, String>> results = new ArrayList<Map<String, String>>();

            String query = String.format(
                            "SELECT propertyId, propertyType, doorsAttr FROM doorsFields WHERE project = '%s'",
                            project);
            ResultSet rs = pgh.execQuery(query);

            while (rs.next()) {
                values.put("propertyId", rs.getString(1));
                values.put("propertyType", rs.getString(2));
                values.put("doorsAttr", rs.getString(3));
                results.add(values);
                values = new HashMap<String, String>();
            }

            return results;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected static String generateSysmlId() {

        return NodeUtil.createId(NodeUtil.getServiceRegistry());
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

    private String getArtifactType(String project, String sysmlappliedmetatype) {

        String artifactType = "defaultResourceURL";

        Set<Map.Entry<String, ArrayList<String>>> treeOfArtifactType = artifactMappings.get(project).entrySet();

        ArrayList<String> curAppliedMetatypeIDs = new ArrayList<String>();

        for (Map.Entry<String, ArrayList<String>> curArtifactMapping : treeOfArtifactType) {

            artifactType = curArtifactMapping.getKey();

            curAppliedMetatypeIDs = curArtifactMapping.getValue();

            for (int i = 0; i < curAppliedMetatypeIDs.size(); i++) {

                if (curAppliedMetatypeIDs.get(i).equals(sysmlappliedmetatype)) {

                    return artifactType;

                }

            }

        }

        return artifactType;

    }

    private boolean syncConfigurationOk() {

        // TODO add check to see if applied metatype IDS found in mappings, existent in MMS before
        // running sync

        try {

            ResultSet result = pgh.execQuery("select * from doorsartifactmappings");


            while (result.next()) {

                doors = new DoorsClient(getConfig("doors.user"), getConfig("doors.pass"), getConfig("doors.url"),
                                result.getString(1));

                if (!doors.doesProjectExists(result.getString(1))) {
                    syncConfigurationMessage = "Project Name: " + result.getString(1)
                                    + " in doors/artifact/mappings does not exist in Doors-NG";
                    return false;
                }



                if (!doors.doesArtifactTypeExist(result.getString(2))) {
                    syncConfigurationMessage = "Artifact Type: " + result.getString(2)
                                    + " in doors/artifact/mappings does not exist in Doors-NG project: "
                                    + result.getString(1);
                    return false;
                }

                if (!doors.doesSysmlIdExist(result.getString(2), "sysmlid")) {
                    syncConfigurationMessage = "sysmlid Attribute for Artifact Type: " + result.getString(2)
                                    + " does not exist in Doors-NG project: " + result.getString(1);
                    return false;
                }


            }

            ResultSet doorsfields = pgh.execQuery("select * from doorsfields");

            while (doorsfields.next()) {

                if (!doors.doesProjectExists(doorsfields.getString(1))) {
                    syncConfigurationMessage = "Project Name: " + doorsfields.getString(1)
                                    + " in doors/attribute/mappings does not exist in Doors-NG";
                    return false;
                }
                // TODO test if attributes specified in doorsfields table, exist in DNG for the
                // specified artifact type :
                // doors.doorsAttributeExists(String artifactType, String attribute)
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;

    }



}
