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
import gov.nasa.jpl.pma.JenkinsEngine;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.ModelGet;


import java.util.Date;
import java.util.Map;


import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class JobGet extends ModelGet {
    static Logger logger = Logger.getLogger(JobGet.class);
    
    // It is required to perform a JobPost in order to make sure the status is 
    // up to date with "jobs" and "elements" json
    AbstractJavaWebScript job = new JobPost(repository, getServices());
    
    // These IDs are important to identifying jobs and job properties
    public static final String jobStereotypeId = "_18_0_5_407019f_1458258829038_313297_14086";
    public static final String slotId = "_9_0_62a020a_1105704885275_885607_7905";
    public static final String instanceSpecId = "_9_0_62a020a_1105704885251_933969_7897";
    
    public JobGet() {
        super();
    }

    public JobGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        
        AbstractJavaWebScript instance = new JobGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache,
                runWithoutTransactions);
    }
 
    @Override
    public JSONObject getJsonForElement( EmsScriptNode job,
                                         WorkspaceNode ws, Date dateTime,
                                         String id,
                                         boolean includeQualified,
                                         boolean isIncludeDocument ) {
        return getJsonForElementAndJob( job, null, false, ws, dateTime, id,
                                        includeQualified, isIncludeDocument );
    }
    
    @Override
    public void postProcessJson( JSONObject top ) {
        if ( jobsJsonArray != null ) {
            // TODO -- Shouldn't we do this in JobPost, too?
            correctJobStatus();
            top.put( "jobs", jobsJsonArray );
            // flush the jobs array so that it can be repopulated for
            // returned json after sending deltas
            jobsJsonArray = new JSONArray();
        }
    }
    
    public static boolean isRunning( String status ) {
        if ( status == null ) return false;
        if ( status.equals( "running" ) ) return true;
        return false;
    }
    
    /**
     * If the status of a job in the MMS is running, then we need to
     * get an update from Jenkins to see if the job died before
     * reporting its status back to the MMS.
     */
    protected void correctJobStatus() {
        if ( jobsJsonArray == null ) return;
        for ( int i = 0; i < jobsJsonArray.length(); ++i ) {
            JSONObject jobJson = jobsJsonArray.optJSONObject( i );
            if ( jobJson == null ) continue;
            String jobStatus = jobJson.optString( "status" );
            if ( jobStatus != null ) {
                // If the status of a job in the mms is running, then we need to
                // get an update from Jenkins to see if the job died before
                // reporting its status back to the mms.
                String jobName = jobJson.optString( "sysmlid" );
                if ( !Utils.isNullOrEmpty( jobName ) ) {
                    JenkinsEngine eng = new JenkinsEngine();
                    
                    JSONObject jenkinsJobJson = eng.getJob( jobName );

                    if( jenkinsJobJson != null) {
                        // TODO -- this will change and the logic will be moved to
                        //         JobPost when queuePosition is an appliedMetatype 
                        
                        JSONObject jobInQueue = eng.isJobInQueue( jenkinsJobJson );
                        
                        if (jobInQueue != null) {
                            jenkinsJobJson.put( "color", "queue" );
                            int pos = eng.numberInQueue( jobInQueue );
                            jobJson.put( "queuePosition", pos + 1 );
                        }
                        
                        String newStatus = getMmsStatus(jenkinsJobJson);
                        // TODO -- The job json is corrected below, but the
                        // status should also be changed in the model
                        // repository.
                        if ( !Utils.isNullOrEmpty( newStatus ) ) {                                                          
                            String jobId = jobJson.optString( "sysmlid" );
                            
                            if( jobId != null ) {
                                EmsScriptNode j = findScriptNodeById( jobId, null, null, true );                                
                                
                                EmsScriptNode p = job.getJobPropertyNode( j, "status" );  
                                
                                JSONObject prop = p.toJSONObject( null, null );
                                
                                JSONObject specJson = prop.optJSONObject( Acm.JSON_SPECIALIZATION );
                                if ( specJson != null && specJson.has( "value"  ) ) {
                                    JSONArray valueArr = specJson.getJSONArray( "value" );
                                    
                                    // FIXME -- if the code goes into here, the value array
                                    //          contains some random string (i.e. "a5m2nva636") 
                                    //          is this because of the conversion from script node to json?
                                    
                                    //          also, sometimes it seems that this process is slow and the MMS status value
                                    //          is only the previous state of the Job json status value
                                    valueArr.remove( 0 );
                                    JSONObject valueSpec = new JSONObject();
                                    
                                    valueSpec.put( "string", newStatus);
                                    valueSpec.put( "type", "LiteralString");                                        
                                    valueArr.put(valueSpec);                               
                                }

                                JSONArray json = new JSONArray();
                                json.put( prop );
                                JSONObject elements = new JSONObject();
                                elements.put( "elements", json );
                                                               
                                updateMmsStatus( elements ); 

                                jobJson.put( "status", newStatus );
                            }     
                                                                    
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the job status as stored by MMS for the job json from Jenkins in the
     * format of the examples below.
     * 
     * <pre>
     *    {
     *      "description" : " ",
     *      "name" : "MMS_1460066361360_37dcd9e7-4d36-44aa-9dff-f36552689e74",
     *      "url" : "https://some-jenkins-server.someorganization.com/job/MMS_1460066361360_37dcd9e7-4d36-44aa-9dff-f36552689e74/",
     *      "color" : "notbuilt",
     *      "lastCompletedBuild" : null
     *    },
     *    {
     *      "description" : " ",
     *      "name" : "MMS_1460067117709_b5f26105-8581-406e-b54d-8525012044c5",
     *      "url" : "https://some-jenkins-server.someorganization.com/job/MMS_1460067117709_b5f26105-8581-406e-b54d-8525012044c5/",
     *      "color" : "blue",
     *      "lastCompletedBuild" : {
     *        "duration" : 2108466,
     *        "estimatedDuration" : 1326904,
     *        "timestamp" : 1460072080801
     *      }
     *    },
     *    {
     *      "description" : " ",
     *      "name" : "MMS_1460067294691_30e71836-727b-4472-95bb-c8a7e8e3b243",
     *      "url" : "https://some-jenkins-server.someorganization.com/job/MMS_1460067294691_30e71836-727b-4472-95bb-c8a7e8e3b243/",
     *      "color" : "red",
     *      "lastCompletedBuild" : {
     *        "duration" : 899,
     *        "estimatedDuration" : 356602,
     *        "timestamp" : 1460092200445
     *      }
     *    }
     * </pre>
     * 
     * @param jenkinsJobJson
     * @return
     */
    protected String getMmsStatus( JSONObject jenkinsJobJson ) {
        
        if ( jenkinsJobJson == null ) return "job not found";
        
        String color = jenkinsJobJson.optString( "color" );
        if ( Utils.isNullOrEmpty( color ) ) return null;
        String status = jenkinsColorToMmsStatus( color ); 
        return status;
    }

    protected String jenkinsColorToMmsStatus( String color ) {
        if ( Utils.isNullOrEmpty( color ) ) return null;
        if (color.contains( "queue" ) ) return "in queue";
        if ( color.contains( "anime" ) ) return "running";
        if ( color.equals( "red" ) ) return "failed";
        if ( color.equals( "blue" ) ) return "completed";
        if ( color.equals( "grey" ) ) return "aborted";
        if ( color.equals( "gray" ) ) return "aborted";
        if ( color.equals( "yellow" ) ) return "unstable";
        if ( color.equals( "disabled" ) ) return "disabled";
        if ( color.equalsIgnoreCase( "notbuilt" ) ) return "in queue";

        return color;
    }
    
    protected void updateMmsStatus(JSONObject elements) {
        
        // This will allow a JobPost to be performed 
        ModelLoadActionExecuter.loadJson( elements, null, null );                             
    }
}
