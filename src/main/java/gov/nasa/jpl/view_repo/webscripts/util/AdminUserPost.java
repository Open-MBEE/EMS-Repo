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

package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONObject;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Adds an admin user so synchronization of sites can be done properly
 *
 * @author cinyoung
 *
 */
public class AdminUserPost extends AbstractJavaWebScript {
    public AdminUserPost() {
        super();
    }

    public AdminUserPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }
        return true;
    }

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        AdminUserPost instance = new AdminUserPost(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();

        if (handleRequest(req)) {
            model.put( "res", "success" );
        } else {
            log(LogLevel.ERROR, "Invalid request!\n", HttpServletResponse.SC_BAD_REQUEST);
            model.put("res", response.toString());
        }

        status.setCode(responseStatus.getCode());

        return model;
    }

    private boolean handleRequest(WebScriptRequest req) {
        JSONObject jsonObject = //JSONObject.make( 
                (JSONObject)req.parseContent();// );
        if (jsonObject != null) {
            if (jsonObject.has( "username" ) && jsonObject.has( "password" ) && jsonObject.has( "email" )) {
                try {
                    String username = jsonObject.getString( "username" );
                    String password = jsonObject.getString( "password" );
                    String email = jsonObject.getString( "email" );

                    services.getAuthenticationService().createAuthentication( username, password.toCharArray() );
                    NodeRef person = services.getPersonService().getPerson( username );
                    if (person == null) {
                        Map< QName, Serializable > properties = new HashMap< QName, Serializable >();
                        properties.put( ContentModel.PROP_USERNAME, username );
                        properties.put( ContentModel.PROP_FIRSTNAME, "Share" );
                        properties.put( ContentModel.PROP_LASTNAME, "Admin");
                        properties.put( ContentModel.PROP_EMAIL, email );
                        properties.put( ContentModel.PROP_PASSWORD, password );
                        person = services.getPersonService().createPerson( properties );

                        ShareUtils.setPassword( password );
                        ShareUtils.setUsername( username );
                    }

//                    services.getAuthenticationService().setAuthenticationEnabled( username, true );
//                    services.getPermissionService().setPermission( person, username, services.getPermissionService().getAllPermission(), true );
                    services.getAuthorityService().addAuthority( "GROUP_ALFRESCO_ADMINISTRATORS", username );

                    responseStatus.setCode( HttpServletResponse.SC_OK );
                    return true;
                } catch ( JSONException e ) {
                    log(LogLevel.ERROR, "JSON malformed", HttpServletResponse.SC_BAD_REQUEST);
                    return false;
                }
            }
        }

        log(LogLevel.ERROR, "Please provide JSON body (perhaps -H Content-Type not specified?)", HttpServletResponse.SC_BAD_REQUEST);
        return false;
    }
}
