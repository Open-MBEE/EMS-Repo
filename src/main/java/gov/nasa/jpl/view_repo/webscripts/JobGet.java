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

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.ModelGet;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.pma.JenkinsEngine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.offbytwo.jenkins.JenkinsServer;


public class JobGet extends ModelGet {
    static Logger logger = Logger.getLogger(JobGet.class);
    
    public JobGet() {
        super();
    }

    public JobGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    protected JSONArray jobs = new JSONArray();
    protected Map<String, EmsScriptNode> jobsFound = new HashMap<String, EmsScriptNode>();
    protected Map<String, List<EmsScriptNode>> jobProperties = new HashMap<String, List<EmsScriptNode>>();

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {        
        JobGet instance = new JobGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache,
                runWithoutTransactions);
    }
    
    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, 
            Status status, Cache cache) {
        URI uri = null;

        if (logger.isDebugEnabled()) {
            String user = AuthenticationUtil.getFullyAuthenticatedUser();
            logger.debug(user + " " + req.getURL());
        }
        
//        try {
////            uri = new URI ("cae-jenkins.jpl.nasa.gov");
//            System.out.println( "Setting base URI to CAE-Jenkins.jpl.nasa.gov\n" );
//        } catch ( URISyntaxException e1 ) {
//            // TODO Auto-generated catch block
//            System.out.println( "\nURISyntaxException e1\n") ;
//            e1.printStackTrace();
//        }

//        JenkinsServer jenkins = null;

//      JenkinsEngine jenkins = new JenkinsEngine(uri);

//        try {
//            jenkins = new JenkinsServer(new URI("https://cae-jenkins.jpl.nasa.gov"),"eurointeg","dhcp3LugH#Meg!i");
            JenkinsEngine jenkins = new JenkinsEngine();
//        } catch ( URISyntaxException e1 ) {
//            // TODO Auto-generated catch block
//            System.out.println( "URISyntaxException e1") ;
//            e1.printStackTrace();
//        }
        

        Timer timer = new Timer();
        printHeader(req);

        Map<String, Object> model = new HashMap<String, Object>();
        // make sure to pass down view request flag to instance
        setIsViewRequest(isViewRequest);

        JSONObject top = NodeUtil.newJsonObject();
        JSONArray jobsJson = handleRequest(req, top, NodeUtil.doGraphDb);

        try {
            for(int i = 0; i < jobsJson.length(); i++) {
                JSONObject job = (JSONObject)jobsJson.get(i);
                if (job.has( "specialization" )) 
                    job.remove( "specialization" );
            }
            
            if (jobsJson.length() > 0) {
                top.put("jobs", jobsJson);
            }

            if (!Utils.isNullOrEmpty(response.toString()))
                top.put("message", response.toString());
            else {
                model.put("res", NodeUtil.jsonToString(top));
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not create JSONObject");
            model.put("res", createResponseJson());
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());

        printFooter();

        if (logger.isInfoEnabled()) {
            log(Level.INFO, "JobGet: %s", timer);
        }

        return model;
    }
    
    @Override
    protected JSONObject jobOrEle(EmsScriptNode job, WorkspaceNode ws, Date dateTime, String id,
                             boolean includeQualified, boolean isIncludeDocument ) {
              return job.toJSONObject( ws,  dateTime, includeQualified, isIncludeDocument, jobProperties.get(id) );
    }

}
