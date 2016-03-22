/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S.
 * Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

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

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.eclipse.lyo.client.exception.ResourceNotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.doorsng.DoorsClient;
import gov.nasa.jpl.mbee.doorsng.Folder;
import gov.nasa.jpl.mbee.doorsng.Requirement;
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

    List< String > processedRequirements = new ArrayList< String >();
    List< String > processedProjects = new ArrayList< String >();
    List< Map< String, String >> customFields =
            new ArrayList< Map< String, String >>();

    static Logger logger = Logger.getLogger( DoorsSync.class );

    public DoorsSync() {
        super();
    }

    public DoorsSync( Repository repositoryHelper, ServiceRegistry registry ) {
        super( repositoryHelper, registry );
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {

        DoorsSync instance = new DoorsSync( repository, getServices() );

        return instance.executeImplImpl( req, status, cache );
    }

    @Override
    protected Map< String, Object >
            executeImplImpl( WebScriptRequest req, Status status, Cache cache ) {

        Map< String, Object > model = new HashMap< String, Object >();
        JSONObject json = null;

        if ( !NodeUtil.doorsSync ) {
            json = new JSONObject();
            json.put( "status", "DoorsSync is off" );
            model.put( "res", NodeUtil.jsonToString( json ) );
            return model;
        }

        // logger.setLevel(Level.DEBUG);
        String[] idKeys = { "modelid", "elementid", "elementId" };
        String modelId = null;
        for ( String idKey : idKeys ) {
            modelId = req.getServiceMatch().getTemplateVars().get( idKey );
            if ( modelId != null ) {
                break;
            }
        }

        if ( validateRequest( req, status ) || true ) {
            WorkspaceNode workspace = getWorkspace( req );
            pgh = new PostgresHelper( workspace );

            String timestamp = req.getParameter( "timestamp" );
            Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

            try {
                pgh.connect();

                if ( modelId == null ) {
                    JSONArray jsonArray =
                            handleRequirements( workspace, dateTime );
                    json = new JSONObject();
                    json.put( "sites", jsonArray );
                } else {
                    EmsScriptNode modelRootNode = null;
                    Node nodeFromPostgres = pgh.getNodeFromSysmlId( modelId );
                    if ( nodeFromPostgres != null ) {
                        modelRootNode =
                                NodeUtil.getNodeFromPostgresNode( nodeFromPostgres );
                    }

                    if ( modelRootNode == null ) {
                        modelRootNode =
                                findScriptNodeById( modelId, workspace,
                                                    dateTime, false );
                    }

                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "modelRootNode = " + modelRootNode );
                    }

                    if ( modelRootNode == null ) {
                        if ( logger.isDebugEnabled() ) {
                            logger.error( HttpServletResponse.SC_NOT_FOUND
                                          + String.format( " Element %s not found",
                                                           modelId
                                                                   + ( dateTime == null
                                                                                       ? ""
                                                                                       : " at "
                                                                                         + dateTime ) ) );
                        }
                        return model;
                    } else if ( modelRootNode.isDeleted() ) {
                        if ( logger.isDebugEnabled() ) {
                            logger.error( HttpServletResponse.SC_GONE
                                          + " Element exists, but is deleted." );
                        }
                        return model;
                    }
                    String project = modelRootNode.getSiteName(dateTime, workspace);
                    doors = new DoorsClient(getConfig("doors.user"), getConfig("doors.pass"), getConfig("doors.url"), project);

                    customFields = mapFields( project );
                    compRequirement( modelRootNode, null );

                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put( modelRootNode.getSysmlId() );
                    json = new JSONObject();
                    json.put( "element", jsonArray );
                }
            } catch ( JSONException e ) {
                if ( logger.isDebugEnabled() ) {
                    logger.error( HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                                  + " JSON could not be created\n" );
                    e.printStackTrace();
                }
            } catch ( Exception e ) {
                if ( logger.isDebugEnabled() ) {
                    logger.error( HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                                  + String.format( " Internal error stack trace:\n %s \n",
                                                   e.getLocalizedMessage() ) );
                    e.printStackTrace();
                }
            } finally {
                pgh.close();
            }
        }

        if ( json == null ) {
            model.put( "res", createResponseJson() );
        } else {
            model.put( "res", NodeUtil.jsonToString( json ) );
        }
        status.setCode( responseStatus.getCode() );

        return model;
    }

    private JSONArray
            handleRequirements( WorkspaceNode workspace, Date dateTime ) {
        JSONArray json = new JSONArray();
        ArrayList< String > types = new ArrayList< String >();
        types.add( "@sysml\\:appliedMetatypes:\"" );
        String appliedMetatype =
                "_11_5EAPbeta_be00301_1147873190330_159934_2220";

        Map< String, EmsScriptNode > requirements =
                searchForElementsPostgres( types, appliedMetatype, false,
                                           workspace, dateTime, null, null );
        for ( String key : requirements.keySet() ) {
            JSONObject projectJson = null;
            projectJson = new JSONObject();

            EmsScriptNode requirementNode = requirements.get( key );
            EmsScriptNode projectNode = requirementNode.getProjectNode( null );
            projectJson.put( "project", projectNode.getSysmlName() );
            rootProjectId = projectNode.getSysmlId();
            customFields = mapFields( projectNode.getSysmlName() );

            try {
                doors = new DoorsClient(getConfig("doors.user"), getConfig("doors.pass"), getConfig("doors.url"), projectNode.getSysmlName());
                if (!processedProjects.contains(projectNode.getSysmlName())) {
                    syncFromDoors();
                }
                if ( !processedRequirements.contains( requirementNode.getSysmlId() ) ) {
                    compRequirement( requirementNode, null );
                }
                projectJson.put( "status", "Sync Complete" );
            } catch ( ClassNotFoundException | SQLException e ) {
                if ( logger.isDebugEnabled() ) {
                    e.printStackTrace();
                }
            } catch ( ResourceNotFoundException e ) {
                projectJson.put( "status", "Project not found" );
            } catch ( Exception e ) {
                if ( logger.isDebugEnabled() ) {
                    e.printStackTrace();
                }
                projectJson.put( "status", "Issue during sync" );
            }
            json.put( projectJson );
        }

        return json;
    }

    protected void syncFromDoors() {

        Requirement[] requirements = doors.getRequirements();

        for ( Requirement req : requirements ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug( String.format( "Adding/Updating from Doors: %s",
                                             req.getTitle() ) );
            }
            compRequirement( null, req );
        }

        processedProjects.add( doors.getProject() );
    }

    protected void compRequirement( EmsScriptNode n, Requirement r ) {

        String resourceUrl = null;
        String sysmlId = null;

        if ( n == null && r != null ) {
            n = getNodeFromDoors( r );
        }

        if ( n != null ) {
            // In MMS
            sysmlId = n.getSysmlId();
            resourceUrl = mapResourceUrl( sysmlId );

            if ( resourceUrl != null ) {
                if ( n.isDeleted() ) {
                    doors.delete( resourceUrl );
                    deleteResourceUrl( resourceUrl );
                } else {
                    if ( r == null ) {
                        r = doors.getRequirement( resourceUrl );
                        r.setResourceUrl( resourceUrl );
                    }
                    Date lastSync = getLastSynced( sysmlId );
                    Date lastModifiedMMS = getTrueModified( n );
                    Date lastModifiedDoors = r.getModified();

                    if ( logger.isDebugEnabled() ) {
                        logger.debug( String.format( "lastSync Date: %s",
                                                     lastSync ) );
                        logger.debug( String.format( "Doors Date: %s",
                                                     lastModifiedDoors ) );
                        logger.debug( String.format( "MMS Date: %s",
                                                     lastModifiedMMS ) );
                    }

                    if ( ( lastSync.compareTo( lastModifiedDoors ) < 0 )
                         && ( lastSync.compareTo( lastModifiedMMS ) >= 0 ) ) {
                        // Modified in Doors, Not Modified in MMS
                        if ( logger.isDebugEnabled() ) {
                            logger.debug( sysmlId
                                          + " - Modified in Doors, Not Modified in MMS" );
                        }
                        resourceUrl = createUpdateRequirementFromDoors( r );
                    } else if ( ( lastSync.compareTo( lastModifiedDoors ) >= 0 )
                                && ( lastSync.compareTo( lastModifiedMMS ) >= 0 ) ) {
                        // Not Modified in Doors, Not Modified in MMS
                        if ( logger.isDebugEnabled() ) {
                            logger.debug( sysmlId
                                          + " - Not Modified in Doors, Not Modified in MMS" );
                        }
                        processedRequirements.add( sysmlId );
                        // resourceUrl = createUpdateRequirementFromMMS( n );
                    } else if ( ( lastSync.compareTo( lastModifiedDoors ) >= 0 )
                                && ( lastSync.compareTo( lastModifiedMMS ) < 0 ) ) {
                        // Not Modified in Doors, Modified in MMS
                        if ( logger.isDebugEnabled() ) {
                            logger.debug( sysmlId
                                          + " - Not Modified in Doors, Modified in MMS" );
                        }
                        resourceUrl = createUpdateRequirementFromMMS( n );
                    } else if ( ( lastSync.compareTo( lastModifiedDoors ) < 0 )
                                && ( lastSync.compareTo( lastModifiedMMS ) < 0 ) ) {
                        // Modified in Doors, Modified in MMS
                        if ( logger.isDebugEnabled() ) {
                            logger.debug( sysmlId
                                          + " - Modified in Doors, Modified in MMS" );
                        }
                        // Conflict detected, for now get from Doors
                        resourceUrl = createUpdateRequirementFromDoors( r );
                    }
                }

            } else {
                // Not in Doors
                if ( logger.isDebugEnabled() ) {
                    logger.debug( sysmlId + " - Not in Doors" );
                }
                createUpdateRequirementFromMMS( n );
            }
        } else if ( r != null ) {
            // Not in MMS
            if ( logger.isDebugEnabled() ) {
                logger.debug( r.getTitle() + " - Not in MMS" );
            }
            createUpdateRequirementFromDoors( r );
        } else {
            if ( logger.isDebugEnabled() ) {
                logger.debug( "Nothing Happened. What?" );
            }
        }

    }

    protected String createUpdateRequirementFromMMS( EmsScriptNode n ) {

        String sysmlId = n.getSysmlId();
        String title = n.getSysmlName();
        Date lastModified = (Date)n.getProperty( Acm.ACM_LAST_MODIFIED );
        String resourceUrl = mapResourceUrl( sysmlId );
        String description = (String)n.getProperty( Acm.ACM_DOCUMENTATION );
        Set< EmsScriptNode > folders = getFolderHierarchyFromMMS( n );

        String parentResourceUrl = null;
        EmsScriptNode[] folderArray =
                folders.toArray( new EmsScriptNode[ folders.size() ] );
        for ( Integer i = folderArray.length - 1; i >= 0; i-- ) {
            parentResourceUrl = createFolderFromMMS( folderArray[ i ] );
        }

        Requirement doorsReq = new Requirement();

        doorsReq.setTitle( title );
        doorsReq.setDescription( description );
        doorsReq.setModified( lastModified );

        doorsReq = addSlotsFromMMS( n, doorsReq );
        doorsReq.setCustomField( doors.getField( "sysmlid" ), sysmlId );

        if ( parentResourceUrl != null ) {
            doorsReq.setParent( URI.create( parentResourceUrl ) );
        }

        if ( resourceUrl != null ) {
            doorsReq.setResourceUrl( resourceUrl );
            doors.update( doorsReq );
        } else {
            resourceUrl = doors.create( doorsReq );
        }

        if ( mapResourceUrl( sysmlId, resourceUrl ) ) {
            processedRequirements.add( sysmlId );
            return resourceUrl;
        }

        return null;
    }

    protected String createUpdateRequirementFromDoors( Requirement r ) {

        JSONObject postJson = new JSONObject();
        JSONArray elements = new JSONArray();

        JSONObject specElement = new JSONObject();
        JSONArray specAppliedMetatypes = new JSONArray();
        JSONObject specSpecialization = new JSONObject();
        JSONArray specClassifier = new JSONArray();

        JSONObject reqElement = new JSONObject();
        JSONArray reqAppliedMetatypes = new JSONArray();
        JSONObject reqSpecialization = new JSONObject();

        EmsScriptNode existing = getNodeFromDoors( r );

        String specSysmlId = null;
        String sysmlId = null;

        if ( existing == null ) {
            specSysmlId = generateSysmlId();
            sysmlId = r.getCustomField( doors.getField( "sysmlid" ) );
            if ( sysmlId == null ) {
                sysmlId = mapResourceUrl( r.getResourceUrl() );
                if ( sysmlId == null ) {
                    sysmlId = generateSysmlId();
                }
            }
        } else {
            EmsScriptNode instance = getInstanceSpecification( existing );
            specSysmlId = instance.getSysmlId();
            sysmlId = existing.getSysmlId();
        }

        String description = r.getDescription();
        String reqParent = mapResourceUrl( r.getParent() );

        if ( reqParent == null ) {
            reqParent = rootProjectId;

            Set< Folder > folders = getFolderHierarchyFromDoors( r.getParent() );
            Folder[] folderArray =
                    folders.toArray( new Folder[ folders.size() ] );
            for ( Integer i = folderArray.length - 1; i >= 0; i-- ) {
                reqParent =
                        createFolderFromDoors( folderArray[ i ].getTitle(),
                                               reqParent,
                                               folderArray[ i ].getResourceUrl() );
            }
        }

        specElement.put( "name", "" );
        specElement.put( "sysmlid", specSysmlId );
        specElement.put( "owner", sysmlId );
        specElement.put( "documentation", "" );
        specAppliedMetatypes.put( "_9_0_62a020a_1105704885251_933969_7897" );
        specElement.put( "appliedMetatypes", specAppliedMetatypes );
        specClassifier.put( "_11_5EAPbeta_be00301_1147873190330_159934_2220" );
        specSpecialization.put( "classifier", specClassifier );
        specSpecialization.put( "type", "InstanceSpecification" );
        specElement.put( "specialization", specSpecialization );
        specElement.put( "isMetatype", false );

        elements.put( specElement );

        for ( Map< String, String > fieldDef : customFields ) {
            String slotSysmlId =
                    specSysmlId + "-slot-" + fieldDef.get( "propertyId" );

            JSONObject slotElement = new JSONObject();
            JSONObject slotSpecialization = new JSONObject();
            JSONArray slotValues = new JSONArray();
            JSONObject slotValue = new JSONObject();

            String value = null;

            if ( fieldDef.get( "doorsAttr" ).contains( "primaryText" ) ) {
                value = r.getPrimaryText();
            } else {
                value =
                        r.getCustomField( doors.getField( fieldDef.get( "doorsAttr" ) ) );
            }

            if ( value != null ) {
                slotElement.put( "sysmlid", slotSysmlId );
                slotSpecialization.put( "isDerived", false );
                slotSpecialization.put( "isSlot", true );

                slotValue.put( "valueExpression", JSONObject.NULL );
                slotValue.put( fieldDef.get( "propertyType" ), value );
                slotValue.put( "type", "LiteralString" );

                slotValues.put( slotValue );

                slotSpecialization.put( "value", slotValues );
                slotSpecialization.put( "type", "Property" );

                slotElement.put( "specialization", slotSpecialization );

                elements.put( slotElement );
            }
        }

        reqElement.put( "name", r.getTitle() );
        reqElement.put( "sysmlid", sysmlId );
        reqElement.put( "owner", reqParent );
        reqElement.put( "documentation", description );
        reqAppliedMetatypes.put( "_11_5EAPbeta_be00301_1147873190330_159934_2220" );
        reqAppliedMetatypes.put( "_9_0_62a020a_1105704885343_144138_7929" );
        reqElement.put( "appliedMetatypes", reqAppliedMetatypes );
        reqSpecialization.put( "type", "Element" );
        reqElement.put( "isMetatype", false );

        elements.put( reqElement );
        postJson.put( "elements", elements );

        if ( handleElementUpdate( postJson ) ) {
            if ( mapResourceUrl( sysmlId, r.getResourceUrl() ) ) {
                setLastSynced( sysmlId, r.getModified() );
                return sysmlId;
            }
        }

        return sysmlId;
    }

    protected String createFolderFromMMS( EmsScriptNode n ) {

        if ( n.getSysmlName() == null ) {
            return null;
        }

        System.out.println( "Folder Sysmlid: " + n.getSysmlId() );

        String resourceUrl = mapResourceUrl( n.getSysmlId() );
        System.out.println( "Folder Resource: " + resourceUrl );

        String parentResourceUrl =
                mapResourceUrl( n.getParent().getSysmlId().replace( "_pkg", "" ) );

        if ( resourceUrl == null ) {

            Folder folder = new Folder();
            folder.setTitle( n.getSysmlName() );
            folder.setDescription( n.getSysmlId() );

            if ( parentResourceUrl != null ) {
                folder.setParent( URI.create( parentResourceUrl ) );
            }

            if ( logger.isDebugEnabled() ) {
                logger.debug( String.format( "Updating/Creating Folder in Doors: %s",
                                             n.getSysmlName() ) );
            }

            resourceUrl = doors.create( folder );

            if ( mapResourceUrl( n.getSysmlId(), resourceUrl ) ) {

                return resourceUrl;

            } else {
                // Couldn't map for whatever reason, delete the resource.
                doors.delete( resourceUrl );
            }
        }

        return resourceUrl;
    }

    protected String createFolderFromDoors( String name, String parent,
                                            String resourceUrl ) {

        JSONObject postJson = new JSONObject();
        JSONArray elements = new JSONArray();

        JSONObject specElement = new JSONObject();
        JSONObject specSpecialization = new JSONObject();
        String sysmlId = mapResourceUrl( resourceUrl );

        if ( sysmlId == null ) {
            sysmlId = generateSysmlId();

            specElement.put( "name", name );
            specElement.put( "sysmlid", sysmlId );
            specElement.put( "owner", parent );
            specSpecialization.put( "type", "Package" );
            specElement.put( "specialization", specSpecialization );

            elements.put( specElement );
            postJson.put( "elements", elements );

            if ( handleElementUpdate( postJson ) ) {
                if ( mapResourceUrl( sysmlId, resourceUrl ) ) {
                    return sysmlId;
                }
            }
        }

        return sysmlId;
    }

    protected Set< EmsScriptNode > getFolderHierarchyFromMMS( EmsScriptNode n ) {

        Set< EmsScriptNode > result = new LinkedHashSet< EmsScriptNode >();

        EmsScriptNode p = n.getOwningParent( null, null, false );
        while ( p != null ) {
            if ( p.hasAspect( Acm.ACM_PACKAGE )
                 && !p.getSysmlId().endsWith( "_pkg" )
                 && p.getSysmlName() != null ) {
                result.add( p );
                p = p.getOwningParent( null, null, false );
            } else {
                p = null;
            }
        }

        return result;
    }

    protected Set< Folder > getFolderHierarchyFromDoors( String resourceUrl ) {

        Set< Folder > result = new LinkedHashSet< Folder >();
        String sysmlId = mapResourceUrl( resourceUrl );

        if ( sysmlId == null ) {
            Folder folder = doors.getFolder( resourceUrl );
            while ( !folder.getTitle().equals( "root" ) ) {
                if ( !folder.getTitle().equals( "root" ) ) {
                    result.add( folder );
                    folder = doors.getFolder( folder.getParent() );
                }
            }
        }

        return result;
    }

    protected EmsScriptNode getNodeFromDoors( Requirement r ) {

        EmsScriptNode n = null;

        String sysmlId = r.getCustomField( doors.getField( "sysmlid" ) );
        if ( sysmlId != null ) {
            Node nodeFromPostgres = pgh.getNodeFromSysmlId( sysmlId );
            if ( nodeFromPostgres != null ) {
                n = NodeUtil.getNodeFromPostgresNode( nodeFromPostgres );
            }
            if ( n == null ) {
                n = findScriptNodeById( sysmlId, null, null, false );
            }
        }

        return n;
    }

    protected EmsScriptNode getInstanceSpecification( EmsScriptNode n ) {

        EmsSystemModel esm = new EmsSystemModel();
        Collection< EmsScriptNode > is = esm.getProperty( n, null );

        for ( EmsScriptNode instance : is ) {
            ArrayList< ? > appliedMetatype =
                    (ArrayList< ? >)instance.getProperty( Acm.ACM_APPLIED_METATYPES );
            if ( appliedMetatype != null
                 && appliedMetatype.contains( "_9_0_62a020a_1105704885251_933969_7897" ) ) {
                return instance;
            }
        }

        return null;
    }

    protected EmsScriptNode[] getAllSlots( EmsScriptNode n ) {

        EmsScriptNode instanceSpec = getInstanceSpecification( n );

        Collection< EmsScriptNode > slots = new HashSet< EmsScriptNode >();

        if ( instanceSpec != null ) {
            for ( Map< String, String > fieldDef : customFields ) {
                String propertySysmlId =
                        instanceSpec.getSysmlId() + "-slot-"
                                + fieldDef.get( "propertyId" );
                Node nodeFromPostgres =
                        pgh.getNodeFromSysmlId( propertySysmlId );
                EmsScriptNode in = null;
                if ( nodeFromPostgres != null ) {
                    in = NodeUtil.getNodeFromPostgresNode( nodeFromPostgres );
                    if ( in == null ) {
                        in =
                                findScriptNodeById( propertySysmlId, null,
                                                    null, false );
                    }
                }

                if ( in != null ) {
                    slots.add( in );
                }
            }
        }

        return ( slots.isEmpty() )
                                  ? null
                                  : slots.toArray( new EmsScriptNode[ slots.size() ] );
    }

    protected Requirement addSlotsFromMMS( EmsScriptNode n, Requirement r ) {

        EmsSystemModel esm = new EmsSystemModel();
        EmsScriptNode[] childProps = getAllSlots( n );

        if ( childProps != null ) {
            for ( EmsScriptNode cn : childProps ) {
                for ( Map< String, String > fieldDef : customFields ) {
                    Collection< Object > value =
                            esm.getValue( esm.getProperty( cn, "value" ),
                                          fieldDef.get( "propertyType" ) );
                    if ( value.iterator().hasNext() ) {
                        if ( fieldDef.get( "doorsAttr" )
                                     .contains( "primaryText" ) ) {
                            r.setPrimaryText( value.iterator().next()
                                                   .toString() );
                        } else {
                            r.setCustomField( doors.getField( fieldDef.get( "doorsAttr" ) ),
                                              value.iterator().next() );
                        }
                    }
                }
            }
        }

        return r;
    }

    protected Date getTrueModified( EmsScriptNode n ) {

        Date modified = n.getLastModified( null );
        EmsScriptNode[] slots = getAllSlots( n );
        if ( slots != null ) {
            for ( EmsScriptNode cn : slots ) {
                Date cnModified = cn.getLastModified( null );

                if ( modified.compareTo( cnModified ) < 0 ) {
                    modified = cnModified;
                }
            }
        }

        return modified;
    }

    protected Boolean handleElementUpdate( JSONObject postJson ) {

        Map< String, Object > model = new HashMap< String, Object >();
        Status status = new Status();
        ModelPost modelPost = new ModelPost( repository, getServices() );

        try {
            modelPost.handleUpdate( postJson, status, null, true, false, model,
                                    true, true );

            return true;
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return false;
    }

    protected String mapResourceUrl( String identifier ) {

        try {
            String query =
                    String.format( "SELECT resourceUrl,sysmlId FROM doors WHERE sysmlId = '%1$s' OR resourceUrl = '%1$s'",
                                   identifier );
            ResultSet rs = pgh.execQuery( query );
            if ( rs.next() ) {
                if ( rs.getString( 2 ).equals( identifier ) ) {
                    return rs.getString( 1 );
                } else {
                    return rs.getString( 2 );
                }
            } else {
                return null;
            }
        } catch ( SQLException e ) {
            e.printStackTrace();
        }

        return null;
    }

    protected Boolean mapResourceUrl( String sysmlId, String resourceUrl ) {

        try {
            Map< String, String > values = new HashMap< String, String >();
            values.put( "sysmlid", sysmlId );
            values.put( "resourceUrl", resourceUrl );

            if ( mapResourceUrl( sysmlId ) != null ) {
                pgh.execUpdate( String.format( "UPDATE doors SET lastSync = current_timestamp WHERE sysmlid = '%s'",
                                               sysmlId ) );
                return true;
            } else {
                if ( pgh.insert( "doors", values ) > 0 ) {
                    return true;
                }
            }
        } catch ( SQLException e ) {
            e.printStackTrace();
        }

        return false;
    }

    protected Date getLastSynced( String sysmlId ) {

        try {
            String query =
                    String.format( "SELECT lastSync FROM doors WHERE sysmlid = '%s'",
                                   sysmlId );
            ResultSet rs = pgh.execQuery( query );
            if ( rs.next() ) {
                return rs.getTimestamp( 1 );
            } else {
                return null;
            }
        } catch ( SQLException e ) {
            e.printStackTrace();
        }

        return null;
    }

    protected void setLastSynced( String sysmlId, Date lastSync ) {

        try {
            Timestamp ts = new Timestamp( lastSync.getTime() + 1000 );

            String query =
                    String.format( "UPDATE doors SET lastSync = '%1$TD %1$TT' WHERE sysmlid = '%2$s'",
                                   ts, sysmlId );
            pgh.execUpdate( query );
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    protected Boolean deleteResourceUrl( String value ) {

        try {
            String query =
                    String.format( "DELETE FROM doors WHERE resourceUrl = '%1$s' OR sysmlId = '%1$s'",
                                   value );
            pgh.execQuery( query );

            return true;
        } catch ( SQLException e ) {
            e.printStackTrace();
        }

        return false;
    }

    protected List< Map< String, String >> mapFields( String project ) {

        try {
            Map< String, String > values = new HashMap< String, String >();
            List< Map< String, String >> results =
                    new ArrayList< Map< String, String >>();
            String query =
                    String.format( "SELECT propertyId, propertyType, doorsAttr FROM doorsFields WHERE project = '%s'",
                                   project );
            ResultSet rs = pgh.execQuery( query );
            while ( rs.next() ) {
                values.put( "propertyId", rs.getString( 1 ) );
                values.put( "propertyType", rs.getString( 2 ) );
                values.put( "doorsAttr", rs.getString( 3 ) );

                results.add( values );
            }
            return results;
        } catch ( SQLException e ) {
            e.printStackTrace();
        }

        return null;
    }

    protected static String generateSysmlId() {

        return NodeUtil.createId( NodeUtil.getServiceRegistry() );
    }

    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {

        if ( !checkRequestContent( req ) ) {
            return false;
        }

        String id = req.getServiceMatch().getTemplateVars().get( WORKSPACE_ID );
        if ( !checkRequestVariable( id, WORKSPACE_ID ) ) {
            return false;
        }

        return true;
    }

}
