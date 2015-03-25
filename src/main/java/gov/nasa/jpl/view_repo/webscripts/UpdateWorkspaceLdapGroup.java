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

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Updates the LDAP group that is allowed to do workspace operations- create, merge, diff,
 * and delete.
 * 
 * @author gcgandhi
 *
 */
public class UpdateWorkspaceLdapGroup extends AbstractJavaWebScript {
    public UpdateWorkspaceLdapGroup() {
        super();
    }

    public UpdateWorkspaceLdapGroup(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }
        
        if (!checkRequestVariable(req.getParameter( "ldapGroup" ), "ldapGroup")) {
            return false;
        }
        
        return true;
    }


    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        UpdateWorkspaceLdapGroup instance = new UpdateWorkspaceLdapGroup(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        
        printHeader( req );
        //clearCaches();
        Map<String, Object> model = new HashMap<String, Object>();

        try {
            if (validateRequest(req, status)) {
                String ldapGroup = req.getParameter( "ldapGroup" );
                handleRequest(ldapGroup);
            }
            
        } catch (Exception e) {
            log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());
        model.put("res", createResponseJson());

        printFooter();
        return model;
    }
    
    private void handleRequest(String ldapGroup) {
        
        EmsScriptNode branchPermNode = null;
        EmsScriptNode mmsFolder = null;
        
        if (Utils.isNullOrEmpty( ldapGroup )) {
            log(LogLevel.ERROR, "Empty or null ldap group", 
                HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        // to make sure no permission issues, run as admin
        AuthenticationUtil.setRunAsUser( "admin" );
        
        // Place the node (branch_perms) to hold the LDAP group in CompanyHome/MMS:
        EmsScriptNode context = NodeUtil.getCompanyHome( services );
        
        if (context != null) {
            mmsFolder = context.childByNamePath( "MMS" );
            
            // Create MMS folder if needed:
            if (mmsFolder == null) {
                // TODO check write permissions on companyhome?
                mmsFolder = context.createFolder( "MMS" );
            }
            
            if (mmsFolder != null) {
                branchPermNode = mmsFolder.childByNamePath( "branch_perm" );
                
                // Create branch_perm node if needed:
                if (branchPermNode == null) {
                    if (mmsFolder.hasPermission( "Write" )) {
                        branchPermNode = mmsFolder.createNode( "branch_perm", "cm:content" );
                       
                        if (branchPermNode == null) {
                            log(LogLevel.ERROR, "Error creating branch permission node", 
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    }
                    else {
                        log(LogLevel.ERROR, "No permissions to write to MMS folder: "+mmsFolder, 
                            HttpServletResponse.SC_FORBIDDEN);
                    }
                }
                
                // Update branch_perm node:
                if (branchPermNode != null) {
                    branchPermNode.createOrUpdateAspect( "ems:BranchPerm" );
                    branchPermNode.createOrUpdateProperty( "ems:ldapGroup", ldapGroup);
                }
    
            }
            // mmsFolder is null:
            else {
                log(LogLevel.ERROR, "Error creating MMS folder", 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        // company home was not found:
        else {
            log(LogLevel.ERROR, "Could not find companyhome", 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        // Save to cache:
        if (context != null) {
            context.getOrSetCachedVersion();
        }
        if (mmsFolder != null) {
            mmsFolder.getOrSetCachedVersion();
        }
        if (branchPermNode != null) {
            branchPermNode.getOrSetCachedVersion();
        }
        
        // make sure we're running back as the originalUser
        AuthenticationUtil.setRunAsUser( AuthenticationUtil.getFullyAuthenticatedUser() );
        
    }

}
