package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.actions.WorkspaceDiffActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.log4j.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Workspace diffing service
 * @author cinyoung
 *
 */
public class MmsDiffGet extends AbstractJavaWebScript {

    protected WorkspaceNode ws1, ws2;
    protected String workspaceId1;
    protected String workspaceId2;
    protected String userTimeStamp1, userTimeStamp2;
    protected Date dateTime1, dateTime2;
    protected WorkspaceDiff workspaceDiff;
    protected String originalUser;
    protected String diffStatus;
    protected JSONObject diffResults;
    protected EmsScriptNode diffNode = null;
    protected boolean recalculate;

    private final static String DIFF_IN_PROGRESS = "GENERATING";
    private final static String DIFF_COMPLETE = "COMPLETED";
    private final static String DIFF_OUTDATED = "OUTDATED";

    public MmsDiffGet() {
        super();
        originalUser = NodeUtil.getUserName();
    }
    
    public MmsDiffGet(Repository repositoryHelper, ServiceRegistry registry, WorkspaceNode workspace1, 
                      WorkspaceNode workspace2, Date time1, Date time2) {
        
        super(repositoryHelper, registry);
        originalUser = NodeUtil.getUserName();
        ws1 = workspace1;
        ws2 = workspace2;
        dateTime1 = time1;
        dateTime2 = time2;  
    }
    
    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        
        if (!userHasWorkspaceLdapPermissions()) {
            return false;
        }
        
        workspaceId1 = getWorkspace1(req);
        workspaceId2 = getWorkspace2(req);
        ws1 = WorkspaceNode.getWorkspaceFromId( workspaceId1, getServices(), response, status, //false,
                                  null );
        ws2 = WorkspaceNode.getWorkspaceFromId( workspaceId2, getServices(), response, status, //false,
                                  null );
        boolean wsFound1 = ( ws1 != null || ( workspaceId1 != null && workspaceId1.equalsIgnoreCase( "master" ) ) );
        boolean wsFound2 = ( ws2 != null || ( workspaceId2 != null && workspaceId2.equalsIgnoreCase( "master" ) ) );

        if ( !wsFound1 ) {
            log( Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Workspace 1 id , %s , not found",workspaceId1);
            return false;
        }
        if ( !wsFound2 ) {
            log( Level.ERROR, HttpServletResponse.SC_NOT_FOUND , "Workspace 2 id, %s , not found",workspaceId2);
            return false;
        }
        return true;
    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        MmsDiffGet mmsDiffGet = new MmsDiffGet();
        return mmsDiffGet.executeImplImpl( req, status, cache, runWithoutTransactions );
    }
    


    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                   Status status, Cache cache ) {
        printHeader( req );

        Map<String, Object> results = new HashMap<String, Object>();

        if (!validateRequest(req, status)) {
            status.setCode(responseStatus.getCode());
            results.put("res", createResponseJson());
            return results;
        }

        boolean runInBackground = getBooleanArg(req, "background", false);
        recalculate = getBooleanArg( req, "recalculate", false );
        
        userTimeStamp1 = getTimestamp1(req);
        userTimeStamp2 = getTimestamp2(req);
        String latestTime1 = CommitUtil.replaceTimeStampWithCommitTime(dateTime1, ws1, services, response);
        String latestTime2 = CommitUtil.replaceTimeStampWithCommitTime(dateTime2, ws2, services, response);
        String timestamp1 = null;
        String timestamp2 = null;
        
        // Replace time string with latest commit node time if it
        // is not 'latest'.  If it is 'latest' then we just re-use
        // the pre-existing node if there is one.
        // This time string is used in the job node name to facilitate
        // fast look up and re-use of diff jobs that resolve to the
        // same commit times.
        if (userTimeStamp1.equals( NO_TIMESTAMP )) {
            dateTime1 = null;
            timestamp1 = userTimeStamp1;
        }
        else {
            dateTime1 = TimeUtils.dateFromTimestamp( userTimeStamp1 );
            timestamp1 = latestTime1 != null ? latestTime1 : userTimeStamp1;
        }
        
        if (userTimeStamp2.equals( NO_TIMESTAMP )) {
            dateTime2 = null;
            timestamp2 = userTimeStamp2;
        }
        else {
            dateTime2 = TimeUtils.dateFromTimestamp( userTimeStamp2 );
            timestamp2 = latestTime2 != null ? latestTime2 : userTimeStamp2;
        }
        
        // Doing a background diff:
        if (runInBackground) {
            
            saveAndStartAction(req, timestamp1, timestamp2, latestTime1,
                               latestTime2, status);
            
            JSONObject top = new JSONObject();

            if (diffStatus.equals( DIFF_IN_PROGRESS )) {
                
                response.append("Diff being processed in background.\n");
                response.append("You will be notified via email when the diff has finished.\n"); 
                
                top.put( "status", diffStatus );
            }
            else if (diffStatus.equals( DIFF_COMPLETE ) ||
                     diffStatus.equals( DIFF_OUTDATED )) {
                
                if (diffResults != null) {
                    top = diffResults;
                }
                else {
                    log( Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                         "Error retreiving completed diff." );
                }
                top.put( "status", diffStatus );
            }
            
            if (diffNode != null) {
                
                // REVIEW: Use the modifier or creator here?
                String username = (String)NodeUtil.getNodeProperty( diffNode, "cm:modifier",
                                                                    getServices(), true,
                                                                    false );
                EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username), 
                                                       services, new StringBuffer());
                String email = (String) user.getProperty("cm:email");
                Date creationTime = diffNode.getCreationDate();
                
                if (username != null)
                    top.put( "user", username );
                if (email != null)
                    top.put( "email", email );
                if (creationTime != null)
                    top.put( "diffTime", EmsScriptNode.getIsoTime(creationTime) );
            }
            
            if (!Utils.isNullOrEmpty( response.toString() )) {
                top.put("message", response.toString());
            }
            results.put("res", NodeUtil.jsonToString( top, 4 ));
        }
        // Doing a non-background diff:
        else {
            performDiff(results);
        }

        status.setCode(responseStatus.getCode());

        printFooter();

        return results;
    }
    
    public void performDiff(Map<String, Object> results) {
       
        // to make sure no permission issues, run as admin
        AuthenticationUtil.setRunAsUser( "admin" );
        
        workspaceDiff = new WorkspaceDiff(ws1, ws2, dateTime1, dateTime2, response, responseStatus);

        try {
            workspaceDiff.forceJsonCacheUpdate = false;
            JSONObject top = workspaceDiff.toJSONObject( dateTime1, dateTime2, false );
            if (!Utils.isNullOrEmpty(response.toString())) top.put("message", response.toString());
            results.put("res", NodeUtil.jsonToString( top, 4 ));
        } catch (JSONException e) {
            e.printStackTrace();
            results.put("res", createResponseJson());
        }
        
        AuthenticationUtil.setRunAsUser( originalUser );
        
    }
    
    protected void saveAndStartAction( WebScriptRequest req,
                                       String timestamp1,
                                       String timestamp2,
                                       String latestCommitTime1,
                                       String latestCommitTime2,
                                       Status status ) {

        if (timestamp1 != null && timestamp2 != null) {

            String ws1Name = WorkspaceNode.getWorkspaceName(ws1);
            String ws2Name = WorkspaceNode.getWorkspaceName(ws2);
        
            String timeString1 = timestamp1.replace( ":", "_" );
            String timeString2 = timestamp2.replace( ":", "_" );
            String timeString = timeString1 + "_" + timeString2;
           
            String jobName = "Diff_Job_" + timeString + ".json";                      
            EmsScriptNode companyHome = NodeUtil.getCompanyHome( services );
            
            // Check if there is old diff job using the diffTime if its non-null, otherwise
            // use the jobName:
            EmsScriptNode oldJob = ActionUtil.getDiffJob( companyHome, ws1, ws2, 
                                                          jobName, 
                                                          services, response);
            
            diffStatus = DIFF_IN_PROGRESS;
            boolean reComputeDiff = true;
            boolean readJson = false;
            if (oldJob != null) {
                
                diffNode = oldJob;
                String jobStatus = (String)oldJob.getProperty("ems:job_status");
                
                if (jobStatus != null ) {
                    if (jobStatus.equals("Active")) {
                
                        String errorMsg = 
                                String.format("Already a pending job for background diff: job[%s]",
                                              jobName);
                        log( Level.WARN, errorMsg );
                        reComputeDiff = false;
                    }
                    else if (jobStatus.equals("Succeeded")) {
                        
                        String errorMsg = 
                                String.format("Already a completed job for background diff: job[%s]",
                                              jobName);
                        log( Level.INFO, errorMsg );
                        
                        reComputeDiff = false;
                        readJson = true;
                        diffStatus = DIFF_COMPLETE;

                        // If either timestamp is latest then check if diff job node is outdated:
                        if (timestamp1.equals( NO_TIMESTAMP ) || 
                            timestamp2.equals( NO_TIMESTAMP )) {
                            
                            String foundTimeStamp1 = (String) oldJob.getProperty( "ems:timestamp1" );
                            String foundTimeStamp2 = (String) oldJob.getProperty( "ems:timestamp2" );
                                                                            
                            // Diff is not outdated:
                            if (foundTimeStamp1 != null && foundTimeStamp2 != null
                                && latestCommitTime1 != null && latestCommitTime2 != null
                                && foundTimeStamp1.equals(latestCommitTime1) 
                                && foundTimeStamp2.equals(latestCommitTime2)) {
                                
                                errorMsg = 
                                        String.format("Found up-to-date background diff: job[%s]",
                                                      jobName);
                                log( Level.INFO, errorMsg );
                            }
                            // Diff is outdated:
                            else {
                                if (recalculate) {
                                    diffStatus = DIFF_IN_PROGRESS;
                                    reComputeDiff = true;
                                }
                                else {
                                    errorMsg = 
                                            String.format("Outdated background diff: job[%s]",
                                                          jobName);
                                    log( Level.INFO, errorMsg );
                                    diffStatus = DIFF_OUTDATED;
                                }
                            }
                        }
                             
                    }
                    // Otherwise a failure, so re-compute diff
                }
            }
            
            // Compute diff in the background:
            if (reComputeDiff) {
                
                // Store job node in Company Home/Jobs/<ws1 name>/<ws2 name>:
                EmsScriptNode jobNode = ActionUtil.getOrCreateDiffJob(companyHome, ws1Name, ws2Name,
                                                                      dateTime1, dateTime2,
                                                                      latestCommitTime1, latestCommitTime2,
                                                                      jobName, status, response, false);

                if (jobNode == null) {
                    String errorMsg = 
                            String.format("Could not create job for background diff: job[%s]",
                                          jobName);
                    log( Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg );
                    return;
                }
                
                // kick off the action
                diffNode = jobNode;
                ActionService actionService = services.getActionService();
                Action loadAction = actionService.createAction(WorkspaceDiffActionExecuter.NAME);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_TIME_1, dateTime1);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_TIME_2, dateTime2);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_TS_1, userTimeStamp1);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_TS_2, userTimeStamp2);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_WS_1, ws1);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_WS_2, ws2);

                services.getActionService().executeAction(loadAction, jobNode.getNodeRef(), true, true);
            }
            // Otherwise, retrieve saved diff json:
            else if (readJson){
                ContentReader reader = services.getContentService().getReader(oldJob.getNodeRef(), 
                                                                              ContentModel.PROP_CONTENT);
                
                if (reader != null) {
                    try {
                        diffResults = new JSONObject(reader.getContentString());
                    } catch (ContentIOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            
        }
    }
}
