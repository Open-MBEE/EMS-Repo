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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.util.ApplicationContextHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.InputStreamContent;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class ProjectPostTest {
	private static final String ADMIN_USER_NAME = "admin";

	protected static ProjectPost projectPostComponent;
	
	protected static ApplicationContext applicationContext;

	private static void initAppContext() {
        ApplicationContextHelper.setUseLazyLoading(false);
        ApplicationContextHelper.setNoAutoStart(true);
        applicationContext = ApplicationContextHelper.getApplicationContext(new String[] { "classpath:alfresco/application-context.xml" });
        
        projectPostComponent = (ProjectPost) applicationContext.getBean("webscript.gov.nasa.jpl.javawebscripts.project.post");
        
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
	}
	
	private static void initAlfresco() {
		ServiceRegistry services = (ServiceRegistry)applicationContext.getBean("ServiceRegistry");
		SiteService siteService = services.getSiteService();
		if (siteService.getSite("europa") == null) {
			siteService.createSite("europa", "europa", "Europa", "europa site", true);
		}
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initAppContext();
        initAlfresco();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws JSONException {
		// need to mockup and stub the WebScriptRequest - mockito cannot handle final classes
		// such as Match, so we have to create the appropriate values here
		Map<String, String> templateVars = new HashMap<String, String>();
		templateVars.put("siteName", "europa");
		templateVars.put("projectId", "123456");
		
		Match match = new Match("", templateVars, "");
		assertNotNull(match);
		
		WebScriptServletRequest request = mock(WebScriptServletRequest.class);
		assertNotNull(request);
		when(request.getServiceMatch()).thenReturn(match);
		when(request.getParameter("fix")).thenReturn("true");
		when(request.getParameter("delete")).thenReturn("false");
		JSONObject value = new JSONObject();
		value.append("name", "Clipper");
		when(request.getContent()).thenReturn(new InputStreamContent(null, null, null));
		when(request.parseContent()).thenReturn(value);
		
		Map<String, Object> model = projectPostComponent.executeImpl(request, new Status(), new Cache());
		
		System.out.println(model);
	}

}
