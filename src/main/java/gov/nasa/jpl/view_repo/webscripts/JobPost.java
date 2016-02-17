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
import gov.nasa.jpl.pma.JenkinsBuildConfig;
import gov.nasa.jpl.pma.JenkinsEngine;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class JobPost extends ModelPost {
    static Logger logger = Logger.getLogger(JobPost.class);
    
    static Map<String, String> definingFeatures = new LinkedHashMap< String, String >() {
        private static final long serialVersionUID = -6503314776361243306L;
        {
            // WARNING! FIXME! THESE MAY CHANGE!  MAYBE SEARCH FOR THEM?
            put( "status", "_18_0_2_6620226_1453945722485_173783_14567");
            put( "schedule", "_18_0_2_6620226_1453945600718_861466_14565");
            put( "desiredView", "_18_0_2_6620226_1454345919577_538785_14442");
        }
    };
    static String[] jobProperties =
            Utils.toArrayOfType( definingFeatures.keySet().toArray(), String.class );    

    protected boolean doJenkins = false;
    
    public JobPost() {
        super();
    }

    public JobPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {        

        JobPost instance = new JobPost(repository, services);
        instance.setServices(getServices());
        // Run without transactions since JobPost breaks them up itself.
        return instance.executeImplImpl(req, status, cache, true);
    }
        
    protected void createJenkinsConfig(String jobID,
                                       Map<String,String> propertyValues) {
        JenkinsEngine jenkins = new JenkinsEngine();
        JenkinsBuildConfig config = new JenkinsBuildConfig();
        config.setJobID( jobID );
        String desiredView = propertyValues.get("desiredView");
        config.setDocumentID( desiredView );
        String schedule = propertyValues.get( "schedule" );
        config.setSchedule( schedule );
        jenkins.postConfigXml( config, config.getJobID() );
    }

    @Override
    protected void preProcessJson( JSONObject json, WorkspaceNode workspace ) {
        super.preProcessJson( json, workspace );
        processJobsJson( json, workspace );
    }

    protected void processJobsJson( JSONObject json, WorkspaceNode workspace ) {
        if ( json == null ) return;
        
        // Get "jobs" as opposed to "elements"
        JSONArray jobs = json.optJSONArray( "jobs" );
        if ( jobs == null ) {
            return;
        }
        
        // Get or create "elements" array.
        JSONArray elements = json.optJSONArray( "elements" );
        if ( elements == null ) {
            elements = new JSONArray();
            json.put( "elements", elements );
        }
        
        // Generate or update element json for each of the properties.
        for ( int i = 1; i < jobs.length(); i++ ) {
            JSONObject job = jobs.optJSONObject( i );
            processJobJson( job, elements, workspace );
        }
        
        json.remove( "jobs" );
        
    }
    
    protected void processJobJson( JSONObject job, JSONArray elements,
                                   WorkspaceNode workspace ) {
        if ( job == null ) {
            log( Level.ERROR, "Bad job json: " + job );
            return;
        }
        
        // Get the id
        String jobId = job.optString( "id" );
        if ( Utils.isNullOrEmpty( jobId ) ) {
            jobId = job.optString( "sysmlid" );
        }
        if ( Utils.isNullOrEmpty( jobId ) ) {
            jobId = NodeUtil.createId( getServices() );
            job.put( "sysmlid", jobId );
        }
        
        // Find node for id.
        EmsScriptNode jobNode = findScriptNodeById( jobId, workspace, null, false );
        boolean createNewJob = jobNode == null;
        
        LinkedHashMap<String, String> propertyValues = new LinkedHashMap< String, String >();
        
        // Process properties and remove them from the job json, which is being
        // transformed into element json.
        for ( String propertyName : jobProperties ) {
            // Save away the property values for later use;
            String propertyValue = job.optString( propertyName );
            if ( propertyValue == null ) continue;
            propertyValues.put( propertyName, propertyValue );
            
            // Update or create the property json.
            JSONObject propertyElementJson =
                    getJobProperty( propertyName, job, createNewJob, jobNode,
                                    elements );
            elements.put( propertyElementJson );
            
            job.remove( propertyName );
        }

        // Don't add a specialization to the job json since it may already
        // exist, and we don't want to change the type.

        // Use job json as element json and move to "elements" array. The
        // job-specific properties in the json were stripped out above.
        elements.put(job);
        
        // If creating a new job with a desiredDocument, push a new
        // configuration to Jenkins.
        // TODO -- There should be a generic way to do this, using a "command"
        // property that runs a function in an available API that syncs Jenkins
        String desiredView = propertyValues.get( "desiredView" );
        // with the job.
        if ( createNewJob && !Utils.isNullOrEmpty( desiredView ) ) {
            createJenkinsConfig( jobId, propertyValues );
        }
    }

    /**
     * Find the Property representing the job property (such as status, schedule).
     * The method assumes that the Property element already exists, either in
     * the database (MMS) or the input "elements" json.  If it doesn't exist.
     * the method complains and creates one improperly since it does not find
     * the applied stereotype instance.
     * 
     * @param propertyName
     * @param job
     * @param createNewJob
     * @param jobNode
     * @param elements
     * @return
     */
    public JSONObject getJobProperty( String propertyName, JSONObject job,
                                      boolean createNewJob,
                                      EmsScriptNode jobNode,
                                      JSONArray elements ) {
        String propertyValue = job.optString( propertyName );
        String propertyId = null;
        if ( propertyValue == null ) return null;
        
        // Get the property's id.
        if ( !createNewJob ) {
             Collection< EmsScriptNode > statusNodes = 
                     getSystemModel().getProperty( jobNode, propertyName );
             if ( !Utils.isNullOrEmpty( statusNodes ) ) {
                 if ( statusNodes.size() > 1 ) {
                     // TODO -- ERROR
                 }
                 EmsScriptNode statusNode = statusNodes.iterator().next();
                 propertyId = statusNode.getSysmlId();
             }
        }        

        // The property json this method returns
        JSONObject propertyJson = getPropertyJson(propertyId, elements);
        propertyJson.put( "sysmlid", propertyId );  // may already be there

        // add owner
        String jobId = job.optString( "sysmlid" );
        if ( jobId == null ) {
            jobId = job.optString( "id" );
        }        
        if( jobId != null ) {
            propertyJson.put( "owner", jobId );
        }
        
        // add name
        propertyJson.put( "name", propertyName );
        
        // Make sure we have an id for the property.
        if ( Utils.isNullOrEmpty( propertyId ) ) {
            // We would need to find/create the stereotype instance.
            logger.error( propertyName
                          + " element must already exist or be passed in the json." );
            if ( Utils.isNullOrEmpty( jobId ) ) {
                logger.error("JobPost.getJobProperty(): job id not found!");
                propertyId = NodeUtil.createId( getServices() );
            } else {
                String instanceSpecId = getInstanceSpecId( job );
                String definingFeatureId = getDefiningFeatureId( propertyName );
                propertyId = instanceSpecId + "-slot-" + definingFeatureId; 
            }
        }
        
        // add specialization part with value
        JSONObject specJson = new JSONObject();
        propertyJson.put( "specialization", specJson );
        
        specJson.put( "type", "Property" );
        specJson.put( "isSlot", true);
        JSONArray valueArr = new JSONArray();
        specJson.put( "value", valueArr );
        JSONObject value = new JSONObject();
        valueArr.put(value);
        value.put( "type", "LiteralString" );
        value.put( "string", propertyValue );     
                                       
        return propertyJson;
    }

    protected JSONObject getPropertyJson( String propertyId, JSONArray elements ) {
        JSONObject propertyJson = new JSONObject();
        for ( int i = 0; i < elements.length(); ++i ) {
            JSONObject element = elements.optJSONObject( i );
            if ( element == null ) continue;
            String id = element.optString( "sysmlid" );
            if ( id != null && id.equals( propertyId ) ) {
                propertyJson = element;
                break;
            }
        }
        return propertyJson;
    }

    protected String getDefiningFeatureId( String propertyName ) {
        return definingFeatures.get(propertyName);
    }

    protected String getInstanceSpecId( JSONObject job ) {
        logger.error("JobPost.getInstanceSpecId() not yet supported!");
        // TODO Auto-generated method stub
        return null;
    }
    
    

}
