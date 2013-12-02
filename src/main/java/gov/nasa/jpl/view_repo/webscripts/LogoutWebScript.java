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
		
		String next = "%2Fview-repo%2Fwcs%2Fui%2F";
		
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
