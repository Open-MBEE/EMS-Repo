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
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.ModelGet;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

//import com.offbytwo.jenkins.JenkinsServer;

public class JobGet extends ModelGet {
    static Logger logger = Logger.getLogger(JobGet.class);
    
    public static final String jobStereotypeId = "_18_0_2_6620226_1453944322658_194833_14413";

    protected JSONArray jobsJsonArray = new JSONArray(); 
    
    public JobGet() {
        super();
    }

    public JobGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    protected JSONArray jobs = new JSONArray();
    protected Map<String, EmsScriptNode> jobsFound = new HashMap<String, EmsScriptNode>();

    private boolean fromJenkins = false;
    
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        JobGet instance = new JobGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache,
                runWithoutTransactions);
    }

    @Override
    protected JSONObject jobOrEle(EmsScriptNode job, WorkspaceNode ws, Date dateTime, String id,
                             boolean includeQualified, boolean isIncludeDocument ) {
               
        JSONObject json =
                job.toJSONObject( ws, dateTime, includeQualified,
                                  isIncludeDocument, elementProperties.get( id ) );
        
        if  ( job.isJob( job ) ) {
            JSONObject jobJson = null;
            for ( String propertyName : jobProperties ) {
                
                EmsScriptNode propertyNode = getJobPropertyNode( job, propertyName );
                // get property values and add to json (check for null)

                if( propertyNode != null ) {
                    Collection< Object> values = getSystemModel().getValue( propertyNode, null );
                    if ( !Utils.isNullOrEmpty( values ) ) {
                        if ( values.size() > 1 ) {
                            // TODO -- ERROR?
                        }
                        Object value = values.iterator().next();
                        if ( value != null ) {
                            jobJson = addJobPropertyToJson( propertyName, value,
                                                            jobJson, json );
                        }
                    }
                }
            }
            if ( jobJson != null ) {
                jobsJsonArray.put(jobJson);
            }
        }
        
        // TODO: return an error if the node is not a job
        
        return json;
    }
    
    protected JSONObject addJobPropertyToJson( String propertyName, Object value, JSONObject jobJson,
                                               JSONObject json ) {
        if ( json == null ) return jobJson;
        if ( jobJson == null ) {    
            jobJson = NodeUtil.clone( json );
        }
        jobJson.put(propertyName, jsonWrap( value ) );
        return jobJson;
    }

    @Override
    protected void postProcessJson( JSONObject top ) {
        if ( jobsJsonArray != null ) {
            top.put( "jobs", jobsJsonArray );
        }        
    }


}
