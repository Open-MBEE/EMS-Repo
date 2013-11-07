package gov.nasa.jpl.view_repo.util;

import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;


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
}
