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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.app.Application;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * Provides the WWW-Authenticate response header that browsers need to
 * clear authentication caches...
 * 
 * Extends AbstractWebScript that provides access to response header
 * @author cinyoung
 *
 */
public class LogoutWebScript extends AbstractWebScript {
    private boolean logoutBasicAuth = true;
//	private final String NEXT_PARAM = "next";
	
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {

	    // No need to do anything for basic authentication since server will handle bad
	    // credentials being sent by client
//		logout(req, status);
//		
//		String next = getServicePath(req.getServiceContextPath()) + "%2Fwcs%2Fui%2F";
//		
//		if (req.getParameter(NEXT_PARAM) != null) {
//			next = req.getParameter(NEXT_PARAM);
//		}
//		
//		// set redirection parameters
//		status.setRedirect(true);
//		if (logoutBasicAuth) {
//            status.setLocation(req.getServerPath() + getServicePath(req.getServiceContextPath()) + next);
//		} else {
//	        status.setLocation(req.getServerPath() + getServicePath(req.getServiceContextPath()) 
//	                + "/faces/jsp/login.jsp?_alfRedirect=" + next);
//		}
		
		return new HashMap<String, Object>();
	}

	/**
	 * Simple utility that logs out the user
	 * @param wsr
	 */
	@SuppressWarnings("unused")
    private void logout(WebScriptRequest wsr, Status status) {
	    if (logoutBasicAuth) {
	        status.setCode(HttpServletResponse.SC_UNAUTHORIZED);
	    } else {
	        // logging out for WebClient
            FacesContext fc = FacesContext.getCurrentInstance();
            Application.logOut(fc);
            status.setCode(HttpServletResponse.SC_TEMPORARY_REDIRECT);
	    }
	}
	
	/**
	 * Simple utility to get the service path out of the service context
	 * @param scpath	Service context path
	 * @return			service path
	 */
	@SuppressWarnings("unused")
    private String getServicePath(String scpath) {
		return scpath.replace("/wcservice","").replace("/wcs","").replace("/service","");
	}

    @Override
    /**
     * Stripped out the DeclarativeWebScript and hacked to get the appropriate response header
     */
    final public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
        // retrieve requested format
        String format = req.getFormat();

        try
        {
            // establish mimetype from format
            String mimetype = getContainer().getFormatRegistry().getMimeType(req.getAgent(), format);
            // construct model for script / template
            Status status = new Status();
            Cache cache = new Cache(getDescription().getRequiredCache());
            Map<String, Object> model = executeImpl(req, status, cache);
            if (model == null)
            {
                model = new HashMap<String, Object>(8, 1.0f);
            }
            model.put("status", status);
            model.put("cache", cache);
            
            int statusCode = status.getCode();
            if (statusCode != HttpServletResponse.SC_OK && !req.forceSuccessStatus())
            {
                res.setStatus(statusCode);
            }
            

            res.setCache(cache);
            res.setContentType(mimetype + ";charset=UTF-8");
            res.setHeader("WWW-Authenticate", "Basic realm=\"Alfresco\"");
        }
        catch(Throwable e)
        {
            throw createStatusException(e, req, res);
        }
    }
}
