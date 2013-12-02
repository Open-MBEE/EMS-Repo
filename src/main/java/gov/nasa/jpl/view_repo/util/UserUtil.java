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
package gov.nasa.jpl.view_repo.util;

import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Simple utility for user testing functionality
 * 
 * @author cinyoung
 *
 */
public class UserUtil extends BaseProcessorExtension {
	// alternatively could use the more intuitive ScriptSiteService - siteServiceScript bean
	private static SiteService siteService;
	private static PersonService personService;
	private static String siteName;
	
	private static Log logger = LogFactory.getLog(UserUtil.class);
	
	// for local testing without Site created from share, set ignore to true in configuration so check isn't made
	private static boolean ignore = false;
	
	public void setIgnore(boolean value) {
		ignore = value;
	}
	
	public void setPersonService(PersonService service) {
		personService = service;
	}
	
	public void setSiteService(SiteService service) {
		siteService = service;
	}
	
	public void setSiteName(String name) {
		siteName = name;
	}	
	
	/**
	 * 
	 * Checks whether or not the currently authenticated user has permissions to the
	 * statically specified site.
	 * @return
	 */
	public static boolean hasWebScriptPermissions() {
		return UserUtil.hasWebScriptPermissions(siteName);
	}
	
	/**
	 * Checks whether or not the currently authenticated user has permissions to the
	 * specified site. 
	 * 
	 * @return
	 */
	public static boolean hasWebScriptPermissions(String site) {
		if (ignore) {
			return true;
		} else {
			String username = AuthenticationUtil.getFullyAuthenticatedUser();
			String normalized = personService.getUserIdentifier(username);
			return siteService.isMember(site, normalized);
		}
	}
	
	public static void log(Object msg) {
		logger.error(msg);
	}
}
