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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.view_repo.db.EdgeTypes;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

/**
 *
 * @author Bruce Meeks Jr
 *
 */
public class DoorsArtifactLinkMappingPost extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;

    public DoorsArtifactLinkMappingPost() {

        super();

    }

    public DoorsArtifactLinkMappingPost( Repository repositoryHelper,
                                         ServiceRegistry registry ) {

        super( repositoryHelper, registry );

    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {

        DoorsArtifactLinkMappingPost instance =
                new DoorsArtifactLinkMappingPost( repository, getServices() );

        return instance.executeImplImpl( req, status, cache );
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status,
                                                     Cache cache ) {

        Map< String, Object > postStatus = new HashMap< String, Object >();

        JSONObject message = new JSONObject();

        try {

            JSONArray postedConfiguration =
                    new JSONArray( req.getContent().getContent() );

            postStatus =
                    storeArtifactLinkMappings( postedConfiguration, false );

        }

        catch ( JSONException e ) {

            e.printStackTrace();
            message.put( "result", "Invalid JSON format" );
            postStatus.put( "res", NodeUtil.jsonToString( message ) );
            return postStatus;

        }

        catch ( IOException e ) {

            e.printStackTrace();
            message.put( "result", "Problem reading configuration" );
            postStatus.put( "res", NodeUtil.jsonToString( message ) );

        }

        return postStatus;

    }

    private static Map< String, Object >
            storeArtifactLinkMappings( JSONArray configuration,
                                       boolean staticLoading ) {

        Map< String, Object > postStatus = new HashMap< String, Object >();

        JSONObject message = new JSONObject();

        try {

            JSONArray postedConfiguration = configuration;
            JSONObject curProj = new JSONObject();
            String project = "";
            JSONArray artifactLinkMappings = new JSONArray();
            JSONObject curArtifactLinkMapping = new JSONObject();
            String curSysmlappliedmetatype = "";
            String curSource = "";
            String curTarget = "";

            // if bootstrapping, create table
            if ( staticLoading ) {

                pgh = new PostgresHelper( "null" );

                try {

                    pgh.connect();

                    pgh.execUpdate( "CREATE TABLE doorsartifactlinkmappings (project text not null, sysmlappliedmetatypeid text not null, source text not null, target text not null);" );

                } catch ( SQLException e ) {
                    e.printStackTrace(); // table may already exists, doesn't
                                         // matter will be overwritten anyway
                }

            }

            // For now must drop table first, else can't account for artifact
            // mapping removals
            if ( pgh.execQuery( "SELECT * from doorsartifactlinkmappings" )
                    .next() ) {

                pgh.execUpdate( "delete from doorsartifactlinkmappings" );

            }

            for ( int p = 0; p < postedConfiguration.length(); p++ ) {

                curProj = (JSONObject)postedConfiguration.get( p );

                project = curProj.getString( "project" );

                artifactLinkMappings =
                        curProj.getJSONArray( "artifactLinkMappings" );

                for ( int a = 0; a < artifactLinkMappings.length(); a++ ) {

                    curArtifactLinkMapping =
                            (JSONObject)artifactLinkMappings.get( a );

                    curSysmlappliedmetatype =
                            curArtifactLinkMapping.getString( "sysmlappliedmetatypeid" );

                    curSource = curArtifactLinkMapping.getString( "source" );

                    curTarget = curArtifactLinkMapping.getString( "target" );

                    try {

                        pgh.execUpdate( "insert into doorsartifactlinkmappings (project, sysmlappliedmetatypeid, source, target) VALUES ('"
                                        + project + "','"
                                        + curSysmlappliedmetatype + "','"
                                        + curSource + "','" + curTarget
                                        + "')" );

                    } catch ( SQLException e ) {

                        e.printStackTrace();
                        message.put( "status",
                                     "Problem inserting artifact link mapping into database" );
                        postStatus.put( "res",
                                        NodeUtil.jsonToString( message ) );

                        return postStatus;

                    }

                }

            }

        } catch ( JSONException e ) {

            e.printStackTrace();
            message.put( "status", "Invalid JSON input configuration" );
            postStatus.put( "res", NodeUtil.jsonToString( message ) );

            return postStatus;

        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            pgh.close();
        }

        staticLoading = false;

        message.put( "status",
                     "Doors Artifact Link Types configured successfully" );
        postStatus.put( "res", NodeUtil.jsonToString( message ) );

        return postStatus;

    }

    public static boolean bootstrapArtifactLinkMappings() {

        BufferedReader configurationReader = null;

        String configurationInput = "";

        try {

            configurationReader =
                    new BufferedReader( new FileReader( "doorsArtifactLinkMappings.txt" ) );

            while ( ( configurationInput =
                    configurationReader.readLine() ) != null ) {

                JSONArray configuration = new JSONArray( configurationInput );

                storeArtifactLinkMappings( configuration, true );

                configurationReader.close();

                break;

            }

        } catch ( JSONException e ) {
            e.printStackTrace();

        } catch ( FileNotFoundException e ) {
            e.printStackTrace();

        } catch ( IOException e ) {
            e.printStackTrace();

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return true;

    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        return true;
    }

}
