package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Java backed webscript for logging out of Alfresco repository. 
 * @author cinyoung
 *
 */
public class LogoutWebScript extends DeclarativeWebScript {
	private final String NEXT_PARAM = "next";
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		logout(req);
		
		String next = getServicePath(req.getServiceContextPath()) + "%2Fwcs%2Fui%2F";
		
		if (req.getParameter(NEXT_PARAM) != null) {
			next = req.getParameter(NEXT_PARAM);
		}
		
		// set redirection parameters
		status.setCode(307);
		status.setRedirect(true);
		status.setLocation(req.getServerPath() + getServicePath(req.getServiceContextPath()) 
				+ "/faces/jsp/login.jsp?_alfRedirect=" + next);

		return new HashMap<String, Object>();
	}

	/**
	 * Simple utility that logs out the user
	 * @param wsr
	 */
	private void logout(WebScriptRequest wsr) {
		FacesContext fc = FacesContext.getCurrentInstance();
		Application.logOut(fc);
	}
	
	/**
	 * Simple utility to get the service path out of the service context
	 * @param scpath	Service context path
	 * @return			service path
	 */
	private String getServicePath(String scpath) {
		return scpath.replace("/wcservice","").replace("/wcs","").replace("/service","");
	}
}
