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
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.*;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.extensions.webscripts.servlet.FormData.FormField;

/**
 * Posts artifacts.  Replaces artifact.post.js, as this version is workspace aware.
 * 
 * @author gcgandhi
 *
 */
public class ArtifactPost extends AbstractJavaWebScript {
	public ArtifactPost() {
	    super();
	}
    
    public ArtifactPost(Repository repositoryHelper, ServiceRegistry registry) {
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
    	
    	ArtifactPost instance = new ArtifactPost(repository, services);
        instance.setServices( getServices() );
        return instance.executeImplImpl(req,  status, cache);
    }

	protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
		
		String path = null;
		String content = null;
		JSONObject resultJson = null;
		String filename = null;
		Map<String, Object> model = new HashMap<String, Object>();

        printHeader( req );
		clearCaches();
		
        String cs = req.getParameter("cs"); // Ignoring this b/c we calculate it from the data
        String extension = req.getParameter("extension") != null ? req.getParameter("extension") : ".svg";

        if (!extension.startsWith(".")) {
        	extension = "." + extension;
        }
        
        WorkspaceNode workspace = getWorkspace( req, AuthenticationUtil.getRunAsUser());
        
        // Get the content from the form data:
        FormData formData = (FormData) req.parseContent();
        for (FormField field : formData.getFields()) {
        	if (field.getName().equals("content") && field.getIsFile()) {
        		try {
					content = field.getContent().getContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
                
        // Get the site name from the request:
        String siteName = getSiteName(req);
        
        if (siteName != null && validateRequest(req,status) ) {

        	try {
	        	// Get the artifact name from the url:
	        	String artifactIdPath = getArtifactId(req);	        	
	        	
	        	if (artifactIdPath != null) {
	        		int lastIndex = artifactIdPath.lastIndexOf("/");
	        		
	        		if (artifactIdPath.length() > (lastIndex+1)) {
	        			
	        			path = lastIndex != -1 ? artifactIdPath.substring(0, lastIndex) : "";
	        			String artifactId = lastIndex != -1 ? artifactIdPath.substring(lastIndex+1) : artifactIdPath;
	        			filename = extension != null ? artifactId + extension : artifactId;
	    	        	
	    	        	// Create return json:
	    	        	resultJson = new JSONObject();
	    	        	resultJson.put("filename", filename);
	    	        	// TODO: want full path here w/ path to site also, but Dorris doesnt use it,
	    	        	//		 so leaving it as is.
	    	        	resultJson.put("path", path);
	    	        	resultJson.put("site", siteName);
	    	        	
	    	        	// Update or create the artifact if possible:
	    	        	if (!Utils.isNullOrEmpty(artifactId) && !Utils.isNullOrEmpty(content)) {
	    	        		
		    	        	EmsScriptNode artifact = NodeUtil.updateOrCreateArtifact(artifactId, extension, 
		    	        															 null, content,
		    	        															 siteName,
		    																		 path, workspace, null,
		    																		 response, null, false);
		    	        	
		    	        	if (artifact == null) {
		    	        		 log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Was not able to create the artifact!\n");
			    		         model.put("res", response.toString());
		    	        	}
		    	        	else {
		    	        		resultJson.put("upload", artifact);
		    	        	}

	    	        	}
	    	        	else {
	    		            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid artifactId or no content!\n");
	    		            model.put("res", response.toString());
	    	        	}
	        		}
	        		else {
	        			  log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid artifactId!\n");
	    		          model.put("res", response.toString());
	    		    }
	        		
	        	}
	        	else {
	        		  log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "artifactId not supplied!\n");
    		          model.put("res", response.toString());
    		    }
	        	
        	}
	        catch (JSONException e) {
	            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Issues creating return JSON\n");
	            e.printStackTrace();
	            model.put("res", response.toString());
	        }
        }
        else {
        	log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid request, no sitename specified or no content provided!\n");
        	model.put("res", response.toString());
	    }
        
        status.setCode(responseStatus.getCode());
        if ( !model.containsKey( "res" ) ) {
        	model.put("res", resultJson != null ? resultJson : response.toString());
        }

		printFooter();

		return model;
	}
	
}
