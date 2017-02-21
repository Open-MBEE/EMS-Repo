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


import gov.nasa.jpl.pma.JenkinsEngine;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class JobExecute extends JobPost {
    static Logger logger = Logger.getLogger(JobExecute.class);

    // It is required to perform a JobPost in order to make sure the status is 
    // up to date with "jobs" and "elements" json
    AbstractJavaWebScript job = new JobPost(repository, getServices());
    
    
    /* This is a helper class which is going to send a call to the
     * Jenkin's server to start a job.  
     * */
    
    public JobExecute() {
        super();
    }
    
    public JobExecute(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }
    
    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        JobExecute instance = new JobExecute(repository, services);
        instance.setServices(getServices());
        return instance.executeImplImpl(req, status, cache,
                                        true);
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status,
                                                     Cache cache ) {
        Map< String, Object > model = new HashMap< String, Object >();
        
        JSONObject top = NodeUtil.newJsonObject();
        model.put( "res", NodeUtil.jsonToString( top ) );
        
        status.setCode( responseStatus.getCode() );
        
        String[] idKeys =
            { "jobid", "jobId" };
        
        String jobId = null;
        for ( String idKey : idKeys ) {
            jobId = req.getServiceMatch().getTemplateVars().get( idKey );
            if ( jobId != null ) {
                break;
            }
        }
        
        JenkinsEngine jenkins = new JenkinsEngine();
        
        jenkins.executeJob( jobId );
        
        logger.debug( "JOB STARTED" );
        
        if( jobId != null ) {
            EmsScriptNode j = findScriptNodeById( jobId, null, null, true );                                
            
            if( j != null ) {               
                EmsScriptNode p = job.getJobPropertyNode( j, "status" );  
                
                if( p != null ) {
                    JSONObject prop = p.toJSONObject( null, null, false );
                    
                    if( prop != null ) {
                        JSONObject specJson = prop.optJSONObject( Acm.JSON_SPECIALIZATION );
                        if ( specJson != null && specJson.has( "value"  ) ) {
                            JSONArray valueArr = specJson.getJSONArray( "value" );

                            if( valueArr != null ) {
                                // clear the current values and create the new value 
                                // with the current status
                                valueArr.remove( 0 );
                                JSONObject valueSpec = new JSONObject();
                                
                                valueSpec.put( "string", "in queue");
                                valueSpec.put( "type", "LiteralString");                                        
                                valueArr.put(valueSpec);         
                            }
                        }

                        JSONArray json = new JSONArray();
                        json.put( prop );
                        JSONObject elements = new JSONObject();
                        elements.put( "elements", json );
                                                       
                        updateMmsJob( elements ); 
                    }
                }
                
//                EmsScriptNode queuePos = job.getJobPropertyNode(j , "queuePosition");
//                
//                if( queuePos != null) {
//                	JSONObject prop = queuePos.toJSONObject( null, null );
//                    int pos = getJobQueueNumber(jobId);
//                	
//                    JSONObject specJson = prop.optJSONObject( Acm.JSON_SPECIALIZATION );
//                    if ( specJson != null && specJson.has( "value"  ) ) {
//                        JSONArray valueArr = specJson.getJSONArray( "value" );
//                        
//                        if( valueArr != null ) {
//                            // clear the current values and create the new value 
//                            // with the current status
//                            valueArr.remove( 0 );
//                            JSONObject valueSpec = new JSONObject();
//                            
//                            valueSpec.put( "string", Integer.toString(pos));
//                            valueSpec.put( "type", "LiteralString");                                        
//                            valueArr.put(valueSpec);         
//                        }
//                    }
//
//                    JSONArray json = new JSONArray();
//                    json.put( prop );
//                    JSONObject elements = new JSONObject();
//                    elements.put( "elements", json );
//                                                   
//                    updateMmsJob( elements ); 
//                	
//                }
            }
        }         

        return model;        
    }
    
    protected void updateMmsJob(JSONObject jobs) {
        
        // This will perform a JobPost so the status will be updated
        // in the MMS
        ModelLoadActionExecuter.loadJson( jobs, null, null );                             
    }
}
