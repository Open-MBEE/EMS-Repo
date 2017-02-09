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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
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
public class DoorsContainmentFlag extends AbstractJavaWebScript {

    public static boolean syncContainment = true;

    public DoorsContainmentFlag() {
        super();
    }

    public DoorsContainmentFlag( Repository repositoryHelper,
                                 ServiceRegistry registry ) {
        super( repositoryHelper, registry );
    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {

        DoorsContainmentFlag instance =
                new DoorsContainmentFlag( repository, getServices() );

        return instance.executeImplImpl( req, status, cache );
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status,
                                                     Cache cache ) {

        Map< String, Object > postStatus = new HashMap< String, Object >();
        JSONObject flagSetMessage = new JSONObject();
        PostgresHelper pgh = new PostgresHelper( "null" );

        try {

           
            pgh.connect();

            if ( req.getParameterNames().length > 0 ) {
                if ( req.getParameterNames()[ 0 ].toUpperCase()
                                                 .equals( "ON" ) ) {

                    pgh.execUpdate( "UPDATE doorsflags SET flag = 'ON' where toggletype = 'containment';" );

                    syncContainment = true;
                    flagSetMessage.put( "status",
                                        "Doors sync containment turned on successfully" );
                    postStatus.put( "res",
                                    NodeUtil.jsonToString( flagSetMessage ) );
                    return postStatus;

                } else if ( req.getParameterNames()[ 0 ].toUpperCase()
                                                        .equals( "OFF" ) ) {

                    pgh.execUpdate( "UPDATE doorsflags SET flag = 'OFF' where toggletype = 'containment';" );

                    syncContainment = false;
                    flagSetMessage.put( "status",
                                        "Doors Sync containment turned off successfully" );
                    postStatus.put( "res",
                                    NodeUtil.jsonToString( flagSetMessage ) );
                    return postStatus;

                } else {

                    flagSetMessage.put( "status",
                                        "Invalid Flag. Please use ON or OFF for the flag parameter" );
                    postStatus.put( "res",
                                    NodeUtil.jsonToString( flagSetMessage ) );
                    return postStatus;

                }
            } else {

                flagSetMessage.put( "status",
                                    "Invalid Flag. Parameter missing ON or OFF" );
                postStatus.put( "res",
                                NodeUtil.jsonToString( flagSetMessage ) );
                return postStatus;

            }
        } catch ( ClassNotFoundException | SQLException e ) {
            e.printStackTrace();
        }
        finally{
            pgh.close();
        }

        return postStatus;

    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        return true;
    }

}
