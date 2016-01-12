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

import gov.nasa.jpl.view_repo.webscripts.ModelPost;

import gov.nasa.jpl.mbee.doorsng.DoorsClient;
import gov.nasa.jpl.mbee.doorsng.Requirement;
import gov.nasa.jpl.mbee.doorsng.RequirementCollection;
import gov.nasa.jpl.mbee.doorsng.Folder;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Date;
import java.util.UUID;

import java.net.URI;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;

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
import org.apache.http.client.entity.UrlEncodedFormEntity;

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

    List<String> processed = new ArrayList<String>();

    static Logger logger = Logger.getLogger( DoorsSync.class );

    public DoorsSync() {
        super();
    }

    public DoorsSync(Repository repositoryHelper, ServiceRegistry registry) {
        super( repositoryHelper, registry );
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        DoorsSync instance = new DoorsSync( repository, getServices() );
        return instance.executeImplImpl( req, status, cache );
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {

        Map<String, Object> model = new HashMap<String, Object>();
        JSONObject json = null;

        try {
            String[] idKeys = { "modelid", "elementid", "elementId" };
            String modelId = null;
            for (String idKey : idKeys) {
                modelId = req.getServiceMatch().getTemplateVars().get(idKey);
                if (modelId != null) {
                    break;
                }
            }

            if (validateRequest(req, status) || true) {
                WorkspaceNode workspace = null;
                pgh = new PostgresHelper( workspace );
                String sitesReq = req.getParameter( "sites" );
                String timestamp = req.getParameter( "timestamp" );
                Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

                if(modelId == null) {
                    JSONArray jsonArray = handleSite( workspace, dateTime, sitesReq, req );
                    json = new JSONObject();
                    json.put( "sites", jsonArray );
                } else {
                    EmsScriptNode modelRootNode = null;
                    pgh.connect();
                    modelRootNode = NodeUtil.getNodeFromPostgresNode(pgh.getNodeFromSysmlId( modelId ));
                    pgh.close();

                    if (modelRootNode == null) {
                        modelRootNode = findScriptNodeById(modelId, workspace, dateTime, false);
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("modelRootNode = " + modelRootNode);
                    }

                    if (modelRootNode == null) {
                        logger.error(HttpServletResponse.SC_NOT_FOUND + String.format(" Element %s not found", modelId + (dateTime == null ? "" : " at " + dateTime)));
                        return model;
                    } else if (modelRootNode.isDeleted()) {
                        logger.error(HttpServletResponse.SC_GONE + " Element exists, but is deleted.");
                        return model;
                    }
                    //handleElements( workspace, dateTime, sitesReq );
                }
            }

        } catch (JSONException e) {
            if (logger.isDebugEnabled()) {
                logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + " JSON could not be created\n");
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + String.format(" Internal error stack trace:\n %s \n", e.getLocalizedMessage()));
                e.printStackTrace();
            }
        }
        if (json == null) {
            model.put( "res", createResponseJson() );
        } else {
            model.put( "res", NodeUtil.jsonToString( json ) );
        }
        status.setCode( responseStatus.getCode() );

        return model;
    }

    private JSONArray handleSite(WorkspaceNode workspace, Date dateTime, String sitesReq, WebScriptRequest req) throws JSONException {
        JSONArray json = new JSONArray();
        String name;
        NodeRef siteRef;
        List<SiteInfo> sites = services.getSiteService().listSites( null );

        for (SiteInfo siteInfo : sites) {
            JSONObject siteJson = new JSONObject();

            siteRef = siteInfo.getNodeRef();

            if (dateTime != null) {
                siteRef = NodeUtil.getNodeRefAtTime( siteRef, dateTime );
            }

            if (siteRef != null) {
                siteNode = new EmsScriptNode( siteRef, services );
                name = siteNode.getName();

                siteJson.put( "sysmlId", name );
                siteJson.put( "name", siteInfo.getTitle() );

                if (workspace == null || (workspace != null && workspace.contains( siteNode ))) {
                    try {
                        doors = new DoorsClient( name );
                        pgh.connect();
                        for (EmsScriptNode n : siteNode.getChildNodes()) {
                            syncToDoors( n );
                        }
                        syncFromDoors();
                        pgh.close();

                        siteJson.put( "status", "complete" );
                    } catch (ClassNotFoundException | SQLException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        //e.printStackTrace();
                        siteJson.put( "status", "Project is not in Doors" );
                    }
                }
                json.put( siteJson );
            }
        }

        return json;
    }

    protected void syncToDoors(EmsScriptNode n) {
        String resourceUrl = null;

        if( !n.isDeleted() ) {
            if (n.getParent().getSysmlId().equals("Models") && !n.getSysmlId().endsWith( "_pkg" )) {
                rootProjectId = n.getSysmlId();
            }

            if (n.hasAspect( Acm.ACM_PACKAGE ) && !n.getSysmlId().endsWith( "_pkg" )) {
                if(n.getSysmlName() != null) {
                    logger.debug( String.format( "Updating/Creating Folder in Doors: %s", n.getSysmlName() ) );
                    createFolder( n );
                }
            }

            ArrayList appliedMetatype = (ArrayList) n.getProperty( Acm.ACM_APPLIED_METATYPES );
            if (appliedMetatype != null && appliedMetatype.contains( "_11_5EAPbeta_be00301_1147873190330_159934_2220" )) {
                logger.debug( String.format( "Updating/Creating Requirement in Doors: %s", n.getSysmlId() ) );
                compRequirement( n, null );
            }
        }
        try {
            for (EmsScriptNode cn : n.getChildNodes()) {
                if (!cn.isDeleted()) {
                    syncToDoors( cn );
                }
            }
        } catch ( org.alfresco.service.cmr.repository.MalformedNodeRefException mnre ) {
            if (logger.isDebugEnabled()) {
                logger.error( String.format( "could not get children for parent %s:", n.getId() ) );
                mnre.printStackTrace();
            }
        }
    }

    protected void syncFromDoors() {
        Requirement[] requirements = doors.getRequirements();

        for (Requirement req : requirements) {
            String sysmlId = req.getCustomField( doors.getField("sysmlid"));
            if (sysmlId != null) {
                //Use SysmlId from Doors
                if (!processed.contains( sysmlId )) {
                    createUpdateRequirementFromDoors( req );
                    logger.debug( String.format( "Adding/Updating from Doors: %s : %s", req.getTitle(), sysmlId ) );
                } else {
                    logger.debug( String.format( "Already processed: %s : %s", req.getTitle(), sysmlId ) );
                }
            } else {
                //No SysmlId
                createUpdateRequirementFromDoors( req );
                logger.debug( String.format( "Adding from Doors: %s", req.getTitle() ) );
            }
        }

    }

    protected String compRequirement(EmsScriptNode n, Requirement r) {
        String resourceUrl = null;

        if (n != null) {
            //In MMS
            String sysmlId = n.getSysmlId();
            resourceUrl = mapResourceUrl( sysmlId );

            if (resourceUrl != null) {
                r = doors.getRequirement( resourceUrl );
                Date lastSync = getLastSynced( sysmlId );
                Date lastModifiedMMS = (Date) n.getProperty( Acm.ACM_LAST_MODIFIED );
                Date lastModifiedDoors = r.getModified();

                if ((lastSync.compareTo( lastModifiedDoors ) < 0) && (lastSync.compareTo( lastModifiedMMS ) >= 0)) {
                    //Modified in Doors, Not Modified in MMS
                    logger.debug( "Modified in Doors, Not Modified in MMS" );
                    logger.debug( String.format( "lastSync Date: %s", lastSync ) );
                    logger.debug( String.format( "Doors Date: %s", lastModifiedDoors ) );
                    logger.debug( String.format( "MMS Date: %s", lastModifiedMMS ) );

                    //resourceUrl = createUpdateRequirementFromDoors( r, doors );
                } else if ((lastSync.compareTo( lastModifiedDoors ) >= 0) && (lastSync.compareTo( lastModifiedMMS ) >= 0)) {
                    //Not Modified in Doors, Not Modified in MMS
                    logger.debug( "Not Modified in Doors, Not Modified in MMS" );
                    logger.debug( String.format( "lastSync Date: %s", lastSync ) );
                    logger.debug( String.format( "Doors Date: %s", lastModifiedDoors ) );
                    logger.debug( String.format( "MMS Date: %s", lastModifiedMMS ) );

                    processed.add( sysmlId );
                    //resourceUrl = createUpdateRequirementFromMMS( n );
                } else if ((lastSync.compareTo( lastModifiedDoors ) >= 0) && (lastSync.compareTo( lastModifiedMMS ) < 0)) {
                    //Not Modified in Doors, Modified in MMS
                    logger.debug( "Not Modified in Doors, Modified in MMS" );
                    logger.debug( String.format( "lastSync Date: %s", lastSync ) );
                    logger.debug( String.format( "Doors Date: %s", lastModifiedDoors ) );
                    logger.debug( String.format( "MMS Date: %s", lastModifiedMMS ) );

                    resourceUrl = createUpdateRequirementFromMMS( n );
                } else if ((lastSync.compareTo( lastModifiedDoors ) < 0) && (lastSync.compareTo( lastModifiedMMS ) < 0)) {
                    //Modified in Doors, Modified in MMS
                    logger.debug( "Modified in Doors, Modified in MMS" );
                    logger.debug( String.format( "lastSync Date: %s", lastSync ) );
                    logger.debug( String.format( "Doors Date: %s", lastModifiedDoors ) );
                    logger.debug( String.format( "MMS Date: %s", lastModifiedMMS ) );

                    processed.add( sysmlId );
                }

            } else {
                //Not in Doors
                logger.debug( "Not in Doors" );
                resourceUrl = createUpdateRequirementFromMMS( n );
            }
        }

        return resourceUrl;
    }

    protected String createUpdateRequirementFromMMS(EmsScriptNode n) {

        String sysmlId = n.getSysmlId();
        String title = n.getSysmlName();
        Date lastModified = (Date) n.getProperty( Acm.ACM_LAST_MODIFIED );
        String resourceUrl = mapResourceUrl( sysmlId );
        String parentResourceUrl = mapResourceUrl( n.getParent().getSysmlId().replace( "_pkg",  "" ) );

        String description = (String) n.getProperty(Acm.ACM_DOCUMENTATION);

        Requirement doorsReq = new Requirement();

        doorsReq.setTitle( title );
        doorsReq.setCustomField( doors.getField("sysmlid"), sysmlId);
        doorsReq.setDescription( description );
        doorsReq.setModified( lastModified );

        Map<String, String> customFields = mapFields( n.getSiteCharacterizationId(null, null) );
        Iterator it = customFields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            doorsReq.setCustomField( doors.getField( (String) pair.getValue() ), (String) n.getProperty( (String) pair.getKey() ) );
        }

        if (parentResourceUrl != null) {
            doorsReq.setParent( URI.create( parentResourceUrl ) );
        }

        if (resourceUrl != null) {
            doorsReq.setResourceUrl( resourceUrl );
            doors.update( doorsReq );
        } else {
            resourceUrl = doors.create( doorsReq );
        }

        if (mapResourceUrl( sysmlId, resourceUrl )) {
            processed.add( sysmlId );
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
        JSONArray specClassifer = new JSONArray();

        JSONObject reqElement = new JSONObject();
        JSONArray reqAppliedMetatypes = new JSONArray();
        JSONObject reqSpecialization = new JSONObject();

        String specSysmlId = generateSysmlId();
        String sysmlId = r.getCustomField(doors.getField("sysmlid"));
        String description = r.getDescription();
        String reqParent = mapResourceUrl(r.getParent());

        if (sysmlId == null) {
            sysmlId = mapResourceUrl( r.getResourceUrl() );
            if (sysmlId == null) {
                sysmlId = generateSysmlId();
            }
        }

        if (reqParent == null) {
            reqParent = rootProjectId;

            Set<Folder> folders = getFolderHierarchyFromDoors(r.getParent());
            Folder[] folderArray = folders.toArray(new Folder[folders.size()]);
            for (Integer i = folderArray.length - 1; i >= 0; i--) {
                Folder folder = folderArray[i];
                String folderSysmlId = createFolderFromDoors( folder.getTitle(), reqParent, folder.getResourceUrl() );
                reqParent = folderSysmlId;
            }
        }

        specElement.put("name", "");
        specElement.put("sysmlid", specSysmlId);
        specElement.put("owner", sysmlId);
        specElement.put("documentation", "");
        specAppliedMetatypes.put("_9_0_62a020a_1105704885251_933969_7897");
        specElement.put("appliedMetatypes", specAppliedMetatypes);
        specClassifer.put("_11_5EAPbeta_be00301_1147873190330_159934_2220");
        specSpecialization.put("classifier", specClassifer);
        specSpecialization.put("type", "InstanceSpecification");
        specElement.put("specialization", specSpecialization);
        specElement.put("isMetatype", false);

        elements.put(specElement);

        reqElement.put("name", r.getTitle());
        reqElement.put("sysmlid", sysmlId);
        reqElement.put("owner", reqParent);
        reqElement.put("documentation", description);
        reqAppliedMetatypes.put("_11_5EAPbeta_be00301_1147873190330_159934_2220");
        reqAppliedMetatypes.put("_9_0_62a020a_1105704885343_144138_7929");
        reqElement.put("appliedMetatypes", reqAppliedMetatypes);
        reqSpecialization.put("type", "Element");
        reqElement.put("isMetatype", false);

        Map<String, String> customFields = mapFields( doors.getProject() );
        Iterator it = customFields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            reqElement.put( (String) pair.getKey(), r.getCustomField( doors.getField( (String) pair.getValue() ) ) );
        }

        elements.put(reqElement);
        postJson.put("elements", elements);

        if(handleElementUpdate( postJson )) {
            if (mapResourceUrl( sysmlId, r.getResourceUrl() )) {
                setLastSynced( sysmlId, r.getModified() );
                return sysmlId;
            }
        }

        return sysmlId;
    }

    protected String createFolder(EmsScriptNode n) {

        if (n.getSysmlName() == null) {
            return null;
        }

        String resourceUrl = mapResourceUrl( n.getSysmlId() );
        String parentName = n.getParent().getSysmlName();
        String parentResourceUrl = mapResourceUrl( n.getParent().getSysmlId().replace( "_pkg",  "" ) );

        if(resourceUrl == null) {
            Folder folder = new Folder();
            folder.setTitle( n.getSysmlName() );

            if (parentResourceUrl != null) {
                folder.setParent( URI.create( parentResourceUrl ) );
            }

            resourceUrl = doors.create( folder );
            if (mapResourceUrl( n.getSysmlId(), resourceUrl )) {
                return resourceUrl;
            }
        }
        return resourceUrl;
    }

    protected String createFolderFromDoors(String name, String parent, String resourceUrl) {

        JSONObject postJson = new JSONObject();
        JSONArray elements = new JSONArray();

        JSONObject specElement = new JSONObject();
        JSONArray specAppliedMetatypes = new JSONArray();
        JSONObject specSpecialization = new JSONObject();
        JSONArray specClassifer = new JSONArray();
        String sysmlId = mapResourceUrl( resourceUrl );

        if (sysmlId == null) {
            sysmlId = generateSysmlId();

            specElement.put("name", name);
            specElement.put("sysmlid", sysmlId);
            specElement.put("owner", parent);
            specSpecialization.put("type", "Package");
            specElement.put("specialization", specSpecialization);

            elements.put(specElement);
            postJson.put("elements", elements);

            if(handleElementUpdate( postJson )) {
                if (mapResourceUrl( sysmlId, resourceUrl )) {
                    return sysmlId;
                }
            }
        }

        return sysmlId;
    }

    protected Set<Folder> getFolderHierarchyFromDoors(String resourceUrl) {

        Set<Folder> result = new LinkedHashSet<Folder>();

        String sysmlId = mapResourceUrl( resourceUrl );

        if(sysmlId == null) {
            Folder folder = doors.getFolder(resourceUrl);
            while (!folder.getTitle().equals("root")) {
                if (!folder.getTitle().equals("root")) {
                    result.add(folder);
                    folder = doors.getFolder(folder.getParent());
                }
            }
        }

        return result;
    }

    protected Boolean handleElementUpdate(JSONObject postJson) {
        Map<String, Object> model = new HashMap<String, Object>();
        Status status = new Status();
        ModelPost modelPost = new ModelPost( repository, getServices() );

        try {
            Set<EmsScriptNode> nodes = modelPost.handleUpdate(postJson, status, null, true, true, model, true, true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    protected String mapResourceUrl(String identifier) {
        try {
            String query = String.format( "SELECT resourceUrl,sysmlId FROM doors WHERE sysmlId = '%1$s' OR resourceUrl = '%1$s'", identifier );
            ResultSet rs = pgh.execQuery( query );
            if (rs.next()) {
                if(rs.getString(2).equals(identifier)) {
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
            values.put( "sysmlid", sysmlId );
            values.put( "resourceUrl", resourceUrl );

            if (mapResourceUrl( sysmlId ) != null) {
                pgh.execUpdate( String.format( "UPDATE doors SET lastSync = current_timestamp WHERE sysmlid = '%s'", sysmlId ) );
                return true;
            } else {
                if (pgh.insert( "doors", values ) > 0)  {
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
            String query = String.format( "SELECT lastSync FROM doors WHERE sysmlid = '%s'", sysmlId );
            ResultSet rs = pgh.execQuery( query );
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

            String query = String.format( "UPDATE doors SET lastSync = '%1$TD %1$TT' WHERE sysmlid = '%2$s'", ts, sysmlId );
            pgh.execUpdate( query );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected Boolean deleteResourceUrl(String value) {
        try {
            String query = String.format( "DELETE FROM doors WHERE resourceUrl = '%1$s' OR sysmlId = '%1$s'", value );
            pgh.execQuery( query );

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected String getNodeRefId(String sysmlId) {
        try {
            String query = String.format( "SELECT nodeRefId FROM nodes WHERE sysmlid = '%s'", sysmlId );
            ResultSet rs = pgh.execQuery( query );
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

    protected Map<String, String> mapFields(String project) {
        try {
            Map<String, String> results = new HashMap<String, String>();

            String query = String.format( "SELECT propertyName, doorsAttrName FROM doorsFields WHERE project = '%s'", project );
            ResultSet rs = pgh.execQuery( query );
            while (rs.next()) {
                results.put(rs.getString(1), rs.getString(2));
            }
            return results;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String generateSysmlId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent( req )) {
            return false;
        }

        String id = req.getServiceMatch().getTemplateVars().get( WORKSPACE_ID );
        if (!checkRequestVariable( id, WORKSPACE_ID )) {
            return false;
        }

        return true;
    }

}
