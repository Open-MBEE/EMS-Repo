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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
public class DoorsArtifactMappingGet extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;

    HashMap< String, HashMap< String, ArrayList< String > > > artifactConfiguration =
            new HashMap< String, HashMap< String, ArrayList< String > > >();

    public DoorsArtifactMappingGet() {

        super();

    }

    public DoorsArtifactMappingGet( Repository repositoryHelper,
                                    ServiceRegistry registry ) {

        super( repositoryHelper, registry );

    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {

        DoorsArtifactMappingGet instance =
                new DoorsArtifactMappingGet( repository, getServices() );

        return instance.executeImplImpl( req, status, cache );

    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status,
                                                     Cache cache ) {

        Map< String, Object > getResult = new HashMap< String, Object >();
        JSONObject message = new JSONObject();

        pgh = new PostgresHelper( "null" );

        try {

            pgh.connect();

            ResultSet rs =
                    pgh.execQuery( "SELECT * FROM doorsartifactmappings" );

            while ( rs.next() ) {

                String project = rs.getString( 1 );

                String artifacttype = rs.getString( 2 );

                String appliedMetatype = rs.getString( 3 );

                if ( !artifactConfiguration.keySet().contains( project ) ) {

                    artifactConfiguration.put( project,
                                               new HashMap< String, ArrayList< String > >() );

                    artifactConfiguration.get( project )
                                         .put( artifacttype,
                                               new ArrayList< String >() );

                    artifactConfiguration.get( project ).get( artifacttype )
                                         .add( appliedMetatype );

                }

                else {

                    if ( !artifactConfiguration.get( project ).keySet()
                                               .contains( artifacttype ) ) {

                        artifactConfiguration.get( project )
                                             .put( artifacttype,
                                                   new ArrayList< String >() );

                        artifactConfiguration.get( project ).get( artifacttype )
                                             .add( appliedMetatype );

                    } else {

                        artifactConfiguration.get( project ).get( artifacttype )
                                             .add( appliedMetatype );

                    }

                }

            }

        } catch ( SQLException e ) {

            e.printStackTrace();
            message.put( "status",
                         "Could not retrieve artifact type mappings from database" );
            getResult.put( "res", NodeUtil.jsonToString( message ) );
            return getResult;

        } catch ( ClassNotFoundException e ) {

            e.printStackTrace();

        } catch ( Exception e ) {

            e.printStackTrace();

        } finally {
            pgh.close();
        }

        message.put( "artifactMapping", getJSON() );
        getResult.put( "res", getJSON().toString() );

        return getResult;

    }

    private JSONArray getJSON() {

        JSONArray projectArtifactMappings = new JSONArray();
        JSONObject curProj = new JSONObject();
        HashMap< String, ArrayList< String > > curArtifactMappings =
                new HashMap< String, ArrayList< String > >();
        ArrayList< String > curAppliedMetatypeIDs = new ArrayList< String >();
        JSONArray newAppliedMetatypeIDs = new JSONArray();
        JSONObject curArtifactMetatypeMap = new JSONObject();
        JSONArray curArtifactMetatypeMappings = new JSONArray();
        JSONObject newAppliedMetatypeID = new JSONObject();
        Set< Map.Entry< String, HashMap< String, ArrayList< String > > > > setOfProjArtifactMappings =
                artifactConfiguration.entrySet();

        try {

            for ( Map.Entry< String, HashMap< String, ArrayList< String > > > curProjArtifactMapping : setOfProjArtifactMappings ) {

                curProj = new JSONObject();

                curProj.put( "project", curProjArtifactMapping.getKey() );

                curArtifactMetatypeMappings = new JSONArray();

                curProj.put( "artifactMappings",
                             curArtifactMetatypeMappings );

                curArtifactMappings = curProjArtifactMapping.getValue();

                Set< Map.Entry< String, ArrayList< String > > > curSetArtifactMappings =
                        curArtifactMappings.entrySet();

                for ( Map.Entry< String, ArrayList< String > > curArtifactMapping : curSetArtifactMappings ) {

                    curArtifactMetatypeMap = new JSONObject();

                    curArtifactMetatypeMap.put( "doorsArtifactType",
                                                curArtifactMapping.getKey() );

                    newAppliedMetatypeIDs = new JSONArray();

                    curAppliedMetatypeIDs = curArtifactMapping.getValue();

                    for ( int i = 0; i < curAppliedMetatypeIDs.size(); i++ ) {

                        newAppliedMetatypeID = new JSONObject();

                        newAppliedMetatypeID.put( "id",
                                                  curAppliedMetatypeIDs.get( i ) );

                        newAppliedMetatypeIDs.put( newAppliedMetatypeID );

                    }

                    curArtifactMetatypeMap.put( "appliedMetatypeIDs",
                                                newAppliedMetatypeIDs );

                    curArtifactMetatypeMappings.put( curArtifactMetatypeMap );

                }

                projectArtifactMappings.put( curProj );

            }

        } catch ( JSONException e ) {

            e.printStackTrace();
            return projectArtifactMappings;

        } catch ( Exception e ) {

            e.printStackTrace();
            return projectArtifactMappings;

        }

        return projectArtifactMappings;

    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        return true;
    }

}
