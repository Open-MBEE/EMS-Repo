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
public class DoorsArtifactLinkMappingGet extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;

    HashMap< String, HashMap< String, String > > linkTypeConfiguration =
            new HashMap< String, HashMap< String, String > >();

    public DoorsArtifactLinkMappingGet() {

        super();

    }

    public DoorsArtifactLinkMappingGet( Repository repositoryHelper,
                                        ServiceRegistry registry ) {

        super( repositoryHelper, registry );

    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {

        DoorsArtifactLinkMappingGet instance =
                new DoorsArtifactLinkMappingGet( repository, getServices() );

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
                    pgh.execQuery( "SELECT * FROM doorsartifactlinkmappings" );

            while ( rs.next() ) {

                String project = rs.getString( 1 );

                String sysmlappliedmetatypeid = rs.getString( 2 );

                String uri = rs.getString( 3 );
                
                /*String source = rs.getString( 3 );

                String target = rs.getString( 4 );*/

                if ( !linkTypeConfiguration.keySet().contains( project ) ) {

                    linkTypeConfiguration.put( project,
                                               new HashMap< String, String >() );

                    linkTypeConfiguration.get( project )
                                         .put( sysmlappliedmetatypeid,
                                               uri );

                }

                else {

                    if ( !linkTypeConfiguration.get( project ).keySet()
                                               .contains( sysmlappliedmetatypeid ) ) {

                        linkTypeConfiguration.get( project )
                                             .put( sysmlappliedmetatypeid,
                                                   uri );

                    }

                    // shouldn't occur?
                    else {

                        linkTypeConfiguration.get( project )
                        .put( sysmlappliedmetatypeid,
                              uri );

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

        message.put( "artifactLinkTypeMapping", getJSON() );
        getResult.put( "res", getJSON().toString() );

        return getResult;

    }

    private JSONArray getJSON() {

        JSONArray projectArtifactLinkMappings = new JSONArray();
        JSONObject newProj = new JSONObject();
        HashMap< String, String > curProjLinkConfiguration =
                new HashMap< String, String  >();
        HashMap< String, ArrayList< String > > curSysMlLinkMappings =
                new HashMap< String, ArrayList< String > >();
        ArrayList< String > curAppliedMetatypeIDs = new ArrayList< String >();
        JSONArray newAppliedMetatypeIDs = new JSONArray();
        JSONObject newSysmlLinkMapping = new JSONObject();
        JSONArray sysMlLinkMappingArray = new JSONArray();
        JSONObject newAppliedMetatypeID = new JSONObject();
        Set< Map.Entry< String, HashMap< String, String > > > setOfArtifactLinkMappings = linkTypeConfiguration.entrySet();

        try {

            for ( Map.Entry< String, HashMap< String, String > > curProj : setOfArtifactLinkMappings ) {

                newProj = new JSONObject();

                sysMlLinkMappingArray = new JSONArray();

                newProj.put( "project", curProj.getKey() );

                curProjLinkConfiguration = curProj.getValue();

                Set< Map.Entry< String, String  > > setOfSyslMlLinkMappings = curProjLinkConfiguration.entrySet();

                for ( Map.Entry< String, String  > curSysmlLinkMapping : setOfSyslMlLinkMappings ) {

                    newSysmlLinkMapping = new JSONObject();

                    newSysmlLinkMapping.put( "sysmlappliedmetatypeid",
                                             curSysmlLinkMapping.getKey() );

                    newSysmlLinkMapping.put( "URI",
                                             curSysmlLinkMapping.getValue() );

                    sysMlLinkMappingArray.put( newSysmlLinkMapping );

                }

                newProj.put( "artifactLinkMappings", sysMlLinkMappingArray );

                projectArtifactLinkMappings.put( newProj );

            }

        } catch ( JSONException e ) {

            e.printStackTrace();
            return projectArtifactLinkMappings;

        } catch ( Exception e ) {

            e.printStackTrace();
            return projectArtifactLinkMappings;
        }

        return projectArtifactLinkMappings;

    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        return true;
    }

}
