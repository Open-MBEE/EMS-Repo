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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 *
 * @author Bruce Meeks Jr
 *
 */
public class DoorsProjectMappingPost extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;

    public DoorsProjectMappingPost() {

        super();

    }

    public DoorsProjectMappingPost( Repository repositoryHelper,
                                    ServiceRegistry registry ) {

        super( repositoryHelper, registry );

    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {

        DoorsProjectMappingPost instance =
                new DoorsProjectMappingPost( repository, getServices() );

        return instance.executeImplImpl( req, status, cache );
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status,
                                                     Cache cache ) {

        Map< String, Object > postStatus = new HashMap< String, Object >();

        JSONObject message = new JSONObject();

        pgh = new PostgresHelper( "null" );

        try {

            pgh.connect();

            JSONArray postedConfiguration =
                    new JSONArray( req.getContent().getContent() );

            postStatus = storeProjectMappings( postedConfiguration, false );

        } catch ( SQLException e ) {

            e.printStackTrace();
            message.put( "status", "Could not connect to database" );
            postStatus.put( "res", NodeUtil.jsonToString( message ) );
            return postStatus;

        } catch ( IOException e ) {

            e.printStackTrace();
            message.put( "status",
                         "Could not read mapping configuration over I/O" );
            postStatus.put( "res", NodeUtil.jsonToString( message ) );
            return postStatus;

        } catch ( ClassNotFoundException e ) {

            e.printStackTrace();
            message.put( "status", "Class not found" );
            postStatus.put( "res", NodeUtil.jsonToString( message ) );
            return postStatus;

        } catch ( Exception e ) {

            e.printStackTrace();
            message.put( "status", "Internal server error occurred" );
            postStatus.put( "res", NodeUtil.jsonToString( message ) );
            return postStatus;

        } finally {
            pgh.close();
        }

        return postStatus;

    }

    private static Map< String, Object >
            storeProjectMappings( JSONArray configuration,
                                  boolean staticLoading ) {

        Map< String, Object > postStatus = new HashMap< String, Object >();
        JSONObject message = new JSONObject();
        JSONArray projectMappings = configuration;

        try {

            JSONObject curProjectMapping = new JSONObject();
            String curSysmlProj = "";
            String curDoorsProj = "";

            if ( staticLoading ) {

                pgh = new PostgresHelper( "null" );

                try {

                    pgh.connect();

                    pgh.execUpdate( "CREATE TABLE doorsprojectmappings (sysmlprojectid text not null, doorsproject text not null);" );

                } catch ( SQLException e ) {

                    e.printStackTrace(); // table may already exists

                }

            }

            // For now must drop table first, else can't account for artifact
            // mapping removals
            if ( pgh.execQuery( "SELECT * from doorsprojectmappings" )
                    .next() ) {

                pgh.execUpdate( "delete from doorsprojectmappings" );

            }

            for ( int p = 0; p < projectMappings.length(); p++ ) {

                curProjectMapping = (JSONObject)projectMappings.get( p );

                curSysmlProj = curProjectMapping.getString( "sysmlprojectid" );

                curDoorsProj = curProjectMapping.getString( "doorsproject" );

                try {

                    pgh.execUpdate( "insert into doorsprojectmappings (sysmlprojectid, doorsproject) VALUES ('"
                                    + curSysmlProj + "','" + curDoorsProj
                                    + "')" );

                } catch ( SQLException e ) {

                    e.printStackTrace();
                    message.put( "status",
                                 "Problem inserting project mapping into database" );
                    postStatus.put( "res", NodeUtil.jsonToString( message ) );
                    return postStatus;

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

        message.put( "status", "Doors projects mapped successfully" );
        postStatus.put( "res", NodeUtil.jsonToString( message ) );

        return postStatus;

    }

    public static boolean bootstrapProjectMappings() {

        BufferedReader configurationReader = null;
        String configurationInput = "";

        try {

            configurationReader =
                    new BufferedReader( new FileReader( "doorsProjectMappings.txt" ) );

            while ( ( configurationInput =
                    configurationReader.readLine() ) != null ) {

                JSONArray configuration = new JSONArray( configurationInput );

                storeProjectMappings( configuration, true );

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
