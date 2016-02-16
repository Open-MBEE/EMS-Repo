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

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.pma.JenkinsBuildConfig;
import gov.nasa.jpl.pma.JenkinsEngine;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff;
import gov.nasa.jpl.view_repo.util.K;
import gov.nasa.jpl.view_repo.util.ModelContext;
import gov.nasa.jpl.view_repo.util.ModStatus;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.ServiceContext;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff.DiffType;
import gov.nasa.jpl.view_repo.webscripts.util.ShareUtils;

//import k.frontend.Frontend;
//import k.frontend.ModelParser;
//import k.frontend.ModelParser.ModelContext;












import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;










//import javax.transaction.UserTransaction;
import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
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
    
    @Override
    protected Map<String, Object> executeImplImpl(final WebScriptRequest req, 
            final Status status, Cache cache) {      

        Timer timer = new Timer();

        printHeader(req);

        Map<String, Object> model = new HashMap<String, Object>();
        // clearCaches();

        boolean runInBackground = getBooleanArg(req, "background", false);
        boolean fix = getBooleanArg(req, "fix", false);
        String expressionString = req.getParameter("expression");
        boolean evaluate = getBooleanArg(req, "evaluate", false);
        boolean suppressElementJson = getBooleanArg(req, "suppressElementJson",
                false);

        // see if prettyPrint default is overridden and change
        prettyPrint = getBooleanArg(req, "pretty", prettyPrint);

        final String user = AuthenticationUtil.getFullyAuthenticatedUser();
        String wsId = null;

        if (logger.isInfoEnabled()) {
            logger.info(user + " " + req.getURL());
            logger.info(req.parseContent());
        }

        if (runWithoutTransactions) {// || internalRunWithoutTransactions) {
            myWorkspace = getWorkspace(req, user);
        } else {
            new EmsTransaction(getServices(), getResponse(),
                    getResponseStatus()) {
                @Override
                public void run() throws Exception {
                    myWorkspace = getWorkspace(req, user);
                }
            };
        }

        boolean wsFound = myWorkspace != null;
        if (!wsFound) {
            wsId = getWorkspaceId(req);
            if (wsId != null && wsId.equalsIgnoreCase("master")) {
                wsFound = true;
            }
        }
        if (!wsFound) {
            log(Level.ERROR,
                    Utils.isNullOrEmpty(wsId) ? HttpServletResponse.SC_NOT_FOUND
                            : HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not find or create %s workspace.\n", wsId);
        }

        if (wsFound && validateRequest(req, status)) {
            try {
                if (runInBackground) {
                    // Get the project node from the request:
                    if (runWithoutTransactions) {// ||
                                                    // internalRunWithoutTransactions)
                                                    // {
                        saveAndStartAction(req, myWorkspace, status);
                    } else {
                        new EmsTransaction(getServices(), getResponse(),
                                getResponseStatus()) {
                            @Override
                            public void run() throws Exception {
                                saveAndStartAction(req, myWorkspace, status);
                            }
                        };
                    }
                    if (status.getCode() == HttpServletResponse.SC_OK) {
                        response.append("JSON uploaded, model load being processed in background.\n");
                        response.append("You will be notified via email when the model load has finished.\n");
                    }
                } else {
                    // Check if input is K or JSON
                    String contentType = req.getContentType() == null ? ""
                            : req.getContentType().toLowerCase();
                    Object content;
                    boolean jsonNotK = !contentType.contains("application/k");
                    if (!jsonNotK) {
                        content = req.getContent().getContent();
                    } else {
                        content = (JSONObject) req.parseContent();
                    }
                    
                    JSONObject postJson = getPostJson(jsonNotK, content,
                            expressionString);

                    /*
                    JSONArray jobs = postJson.getJSONArray( "jobs" );
                    
                    // index starts at 1, to skip the initial element json from JobGet
                    for(int i = 1; i < jobs.length(); i++) {
                        jobs.get( i );
                    }
                    */
                    
                    // Get the project node from the request:
                    new EmsTransaction(getServices(), getResponse(),
                            getResponseStatus(), runWithoutTransactions) {// ||
                                                                            // internalRunWithoutTransactions
                                                                            // )
                                                                            // {
                        @Override
                        public void run() throws Exception {
                            getProjectNodeFromRequest(req, true);
                        }
                    };
 
                    System.out.println( "BEFORE" );
                    System.out.println( postJson );
                    
                    preProcessJson( postJson, myWorkspace );
                    
                    System.out.println( "AFTER" );
                    System.out.println( postJson );
                    
                    //if ( doJenkins ) doJenkinsStuff( postJson );
                    
                    // FIXME: this is a hack to get the right site permissions
                    // if DB rolled back, it's because the no_site node couldn't
                    // be created
                    // this is indicative of no permissions (inside the DB
                    // transaction)
                    if (getResponseStatus().getCode() == HttpServletResponse.SC_BAD_REQUEST) {
                        log(Level.WARN, HttpServletResponse.SC_FORBIDDEN,
                                "No write priveleges");
                    } else if (projectNode != null) {
                        handleUpdate(postJson, status, myWorkspace, evaluate,
                                fix, model, true, suppressElementJson);
                    }
                }
            } catch (JSONException e) {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                        "JSON malformed\n");
                e.printStackTrace();
            } catch (Exception e) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal error stack trace:\n%s\n",
                        e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        if (!model.containsKey("res")) {
            model.put("res", createResponseJson());
        }

        status.setCode(responseStatus.getCode());

        sendProgress("Load/sync/update request is finished processing.",
                projectId, true);

        printFooter();

        if (logger.isInfoEnabled()) {
            logger.info("JobPost: " + timer);
        }

        return model;
    }
    
    protected void doJenkinsStuff(Map<String,String> propertyValues) {
        JenkinsEngine jenkins = new JenkinsEngine();
        JenkinsBuildConfig config = new JenkinsBuildConfig();
        String desiredView = propertyValues.get("desiredView");
        config.setDocumentID( desiredView );
        //jenkins.postConfigXml( propertyValues.getString( "sysmlid" ) );       
    }

    @Override
    protected Set<EmsScriptNode> handleUpdate(JSONObject postJson,
              Status status, final WorkspaceNode workspace, boolean evaluate,
              final boolean fix, Map<String, Object> model, boolean createCommit,
              boolean suppressElementJson) throws Exception {
          final JSONObject top = NodeUtil.newJsonObject();
          
          // TODO: BETWEEN THESE TWO FUNCTIONS, YOU NEED TO RETRIEVE INFORMATION
          //       FOR JOBS...
          final Set<EmsScriptNode> jobs = createOrUpdateModel(postJson,
                  status, workspace, null, createCommit);
          
          //final Set< EmsScriptNode > jobs = 
          //        ModelLoadActionExecuter.loadJson( postJson, null,
          //                                          null );
    
          if (!Utils.isNullOrEmpty(jobs)) {
              sendProgress("Adding relationships to properties", projectId, true);
              addRelationshipsToProperties(jobs, workspace);
    
              // Fix constraints if desired.
              if (fix) {
                  sendProgress("Fixing constraints", projectId, true);
                  fixWithTransactions(jobs, workspace);
                  sendProgress("Fixing constraints completed", projectId,
                          true);
              }
    
              if (!suppressElementJson) {
    
                  // Create JSON object of the elements to return:
                  final JSONArray jobsJson = new JSONArray();
                  final Map<EmsScriptNode, JSONObject> jobsJsonMap = new LinkedHashMap<EmsScriptNode, JSONObject>();
    
                  sendProgress("Getting json for jobs", projectId, true);
                  new EmsTransaction(getServices(), getResponse(),
                          getResponseStatus(), runWithoutTransactions) {
                      @Override
                      public void run() throws Exception {
                          for (EmsScriptNode job : jobs) {
    
                              JSONObject json = null;
                              if ( NodeUtil.doJsonCaching && !fix
                                   && notChanging.contains( job.getSysmlId() ) ) {
                                  json = NodeUtil.jsonCacheGet( job.getNodeRef().toString(),
                                                                0, false );
                              }
                              if ( json == null ) {
                                  json = job.toJSONObject( workspace, null );
                              }                           
                              
                              jobsJson.put(json);
                              jobsJsonMap.put(job, json);
                          }
                          sendProgress("Getting json for jobs completed",
                                  projectId, true);
                      }
                  };
    
                  if (evaluate) {
                      sendProgress("Evaluating constraints and expressions",
                              projectId, true);
    
                      new EmsTransaction(getServices(), getResponse(),
                              getResponseStatus(), runWithoutTransactions) {
                          @Override
                          public void run() throws Exception {
                              evaluate(jobsJsonMap, top, workspace);
                              sendProgress( "Evaluating constraints and expressions completed",
                                            projectId, true);
                          }
                      };
                  }
    
                  top.put("elements", jobsJson);
              }
          }
    
          if (!Utils.isNullOrEmpty(response.toString())) {
              top.put("message", response.toString());
          }
    
          if (!Utils.isNullOrEmpty(ownersNotFound)) {
    
              JSONArray ownerArray = new JSONArray();
              top.put("ownersNotFound", ownerArray);
    
              for (String ownerId : ownersNotFound) {
                  JSONObject element = new JSONObject();
                  ownerArray.put(element);
                  element.put(Acm.JSON_ID, ownerId);
              }
          }
    
          if (prettyPrint) {
              model.put("res", NodeUtil.jsonToString(top, 4));
          } else {
              model.put("res", NodeUtil.jsonToString(top));
          }
    
          return jobs;
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
            doJenkinsStuff( propertyValues );
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
