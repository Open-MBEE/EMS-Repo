package gov.nasa.jpl.view_repo.webscripts;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.site.SiteInfo;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

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

public class JwsRequestUtils {
	public static final String PROJECT_ID = "projectId";
	public static final String SITE_NAME = "siteName";
	
	public static boolean validateContent(WebScriptRequest req, Status status, StringBuffer response) {
		if (req.getContent() == null) {
			status.setCode(HttpServletResponse.SC_NO_CONTENT);
			response.append("No content provided");
			return false;
		}
		
		return true;
	}
	
	public static boolean validateSiteExists(SiteInfo siteInfo, Status status, StringBuffer response) {
		if (siteInfo == null) {
			status.setCode(HttpServletResponse.SC_NOT_FOUND);
			response.append("Site not found");
			return false;
		}
		return true;
	}
	
	public static boolean validatePermissions(WebScriptRequest request, Status status, StringBuffer response, ServiceRegistry services, NodeRef nodeRef, String permissions) {
		if (services.getPermissionService().hasPermission(nodeRef, permissions) != AccessStatus.ALLOWED) {
			status.setCode(HttpServletResponse.SC_UNAUTHORIZED);
			response.append("No write priveleges to " + nodeRef.toString());
			return false;
		}
		return true;
	}

	public static boolean validateRequestVariable(Status status, StringBuffer response, String value, String type) {
		if (value == null) {
			status.setCode(HttpServletResponse.SC_BAD_REQUEST);
			response.append(type + " not provided");
			return false;
		}
		return true;
	}
	
	
	/**
	 * Utility to grab template parameters from a request
	 */
	public static String getRequestVar(WebScriptRequest req, String varid) {
		return req.getServiceMatch().getTemplateVars().get(varid);
	}
}
