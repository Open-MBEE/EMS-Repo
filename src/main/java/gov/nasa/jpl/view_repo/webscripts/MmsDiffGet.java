package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.WorkspaceDiffActionExecuter;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff.DiffType;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.*;
import org.json.JSONArray;
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

    public static boolean glom = true;
    public static boolean diffDefaultIsMerge = true;
        
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
    protected EmsScriptNode diffJob = null;
    protected String diffJobName = null;
    protected String timestamp1, timestamp2;

    private final static String DIFF_STARTED = "STARTED";
    private final static String DIFF_IN_PROGRESS = "GENERATING";
    private final static String DIFF_COMPLETE = "COMPLETED";
    private final static String DIFF_OUTDATED = "OUTDATED";

    public MmsDiffGet() {
        super();
        originalUser = NodeUtil.getUserName();
    }
    
    public MmsDiffGet(Repository repositoryHelper, ServiceRegistry registry, WorkspaceNode workspace1, 
                      WorkspaceNode workspace2, Date time1, Date time2, String ts1, String ts2) {
        
        super(repositoryHelper, registry);
        originalUser = NodeUtil.getUserName();
        ws1 = workspace1;
        ws2 = workspace2;
        dateTime1 = time1;
        dateTime2 = time2;
        timestamp1 = ts1;
        timestamp2 = ts2;
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
        
        // Determine the diffType:
        DiffType diffType;
        if (getBooleanArg( req, "changesForMerge", false )) {
            diffType = DiffType.MERGE;
        }
        else if (getBooleanArg( req, "fullCompare", false )) {
            diffType = DiffType.COMPARE;
        }
        else if (getBooleanArg( req, "bothDirections", false )) {
            diffType = DiffType.BOTH;
        }
        else {
            if (diffDefaultIsMerge) {
                diffType = DiffType.MERGE;
            } 
            else {
                diffType = DiffType.COMPARE;
            }
        }
        
        userTimeStamp1 = getTimestamp1(req);
        userTimeStamp2 = getTimestamp2(req);
        
        //String latestTime1 = CommitUtil.replaceTimeStampWithCommitTime(dateTime1, ws1, services, response);
        //String latestTime2 = CommitUtil.replaceTimeStampWithCommitTime(dateTime2, ws2, services, response);

        String latestTime1 = null;
        String latestTime2 = null;
        //String timestamp1 = null;
        //String timestamp2 = null;
        
        // Replace time string with latest commit node time if it
        // is not 'latest'.  If it is 'latest' then we just re-use
        // the pre-existing node if there is one.
        // This time string is used in the job node name to facilitate
        // fast look up and re-use of diff jobs that resolve to the
        // same commit times.
        if (userTimeStamp1.equals( WorkspaceDiff.LATEST_NO_TIMESTAMP )) { 
            dateTime1 = null;
            latestTime1 = CommitUtil.replaceTimeStampWithCommitTime(dateTime1, ws1, services, response);
            timestamp1 = userTimeStamp1;
        }
        else {
            dateTime1 = TimeUtils.dateFromTimestamp( userTimeStamp1 );
            latestTime1 = CommitUtil.replaceTimeStampWithCommitTime(dateTime1, ws1, services, response);
            timestamp1 = latestTime1 != null ? latestTime1 : userTimeStamp1;
        }
        
        if (userTimeStamp2.equals( WorkspaceDiff.LATEST_NO_TIMESTAMP )) {
            dateTime2 = null;
            latestTime2 = CommitUtil.replaceTimeStampWithCommitTime(dateTime2, ws2, services, response);
            timestamp2 = userTimeStamp2;
        }
        else {
            dateTime2 = TimeUtils.dateFromTimestamp( userTimeStamp2 );
            latestTime2 = CommitUtil.replaceTimeStampWithCommitTime(dateTime2, ws2, services, response);
            timestamp2 = latestTime2 != null ? latestTime2 : userTimeStamp2;
        }
        
        // Doing a background diff:
        if (runInBackground) {
            
            saveAndStartAction(req, latestTime1,
                               latestTime2, status, diffType);
            
            JSONObject top = new JSONObject();

            if (diffStatus.equals( DIFF_IN_PROGRESS ) ||
                diffStatus.equals( DIFF_STARTED )) {
                
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
            performDiff(results, diffType);
        }

        status.setCode(responseStatus.getCode());

        printFooter();

        return results;
    }
    
    
//    public static JSONObject performDiffGlom( WorkspaceNode w1, WorkspaceNode w2,
//                                              Date date1, Date date2,
//                                              StringBuffer aResponse,
//                                              Status aResponseStatus ) {
//        WorkspaceDiff workspaceDiff = null;
//            workspaceDiff =
//                    new WorkspaceDiff(w1, w2, date1, date2, aResponse, aResponseStatus);
//        
//        JSONObject diffJson = null;
//        if ( workspaceDiff != null ) {
//            try {
//                workspaceDiff.forceJsonCacheUpdate = false;
//                diffJson = workspaceDiff.toJSONObject( date1, date2, false );
//                if (!Utils.isNullOrEmpty(aResponse.toString())) diffJson.put("message", aResponse.toString());
//                //results.put("res", NodeUtil.jsonToString( diffJson, 4 ));
//            } catch (JSONException e) {
//                e.printStackTrace();
//                diffJson = null;
//            }
//        }
//        return diffJson;
//    }
    public static JSONObject performDiff( WorkspaceNode w1, WorkspaceNode w2,
                                          Date date1, Date date2,
                                          StringBuffer aResponse,
                                          Status aResponseStatus,
                                          DiffType diffType,
                                          boolean forceNonGlom) {
        
        WorkspaceDiff workspaceDiff = null;
            workspaceDiff =
                    new WorkspaceDiff(w1, w2, date1, date2, aResponse, 
                                      aResponseStatus, diffType, !forceNonGlom);
        
        JSONObject diffJson = null;
        if ( workspaceDiff != null ) {
            try {
                if ( workspaceDiff.glom ) {
                    diffJson = workspaceDiff.diffJson;
                } else {
                    workspaceDiff.forceJsonCacheUpdate = false;
                    diffJson = workspaceDiff.toJSONObject( date1, date2, false );
                }
                if (!Utils.isNullOrEmpty(aResponse.toString())) diffJson.put("message", aResponse.toString());
                //results.put("res", NodeUtil.jsonToString( diffJson, 4 ));
            } catch (JSONException e) {
                e.printStackTrace();
                diffJson = null;
            }
        }
        return diffJson;
    }
        
    /**
     * Get a nearest diff to the one requested, find the commits between the
     * times requested and those of the nearest diff, and calculate the
     * requested diff with those as input.
     * <p>
     * TODO -- Unless we just keep track of the latest diffs, we could search
     * through the existing diffs to find the nearest. For now, just apply this
     * to the latest or, if not found, regenerate from scratch. So, this would
     * allow us to compute diffs with any timepoints, not just the latest.
     * <p>
     * Let diff* be the diff that we want to compute, and let diff0 be a diff
     * prior to diff*. Let diff0 = diff(w1, w2, t1_0, t2_0) and diff* = diff(w1,
     * w2, t1, t2). Let diff1 = diff(w1, w1, t1_0, t1) and diff2 = diff(w2, w2,
     * t2_0, t2). This {@link #performDiffGlom(Map)} function computes diff* =
     * (diff0 + diff2) - diff1.
     * <p>
     * The '+' is computed by {@link #glom(JSONObject, JSONObject)}, and the '-'
     * is computed by {@link #diff(JSONObject, JSONObject)}. We add diff2 to
     * diff0 first instead of subtracting diff1 from diff2 because we use the
     * context of diff0 when subtracting diff1.
     * <p>
     * For example, suppose diff0 adds element x, diff1 adds the same element x
     * with different properties (let's call it x1), and diff2 updates x as x2.
     * If the additions and changes all affect different properties, then diff*
     * should be update ((x + x2) - x1). If they affect the same property, and
     * x1 = x2 then there is no net change and diff* should be empty: (x + x2) -
     * x1 = x2 - x1 = &emptyset;. Subtracting first gives different results.
     * Doing diff2 - diff1 first results in x + (x2 - x1). If affecting
     * different properties, the result is the same, x2. If they are the same,
     * then diff* = x + (x2 - x1) = x, but there should be no change.
     * 
     * @param results
     * @return
     */
    public JSONObject performDiffGlom(Map<String, Object> results, DiffType diffType) {
 
        // For each workspace get the diffs between the request timestamp and the
        // timestamp of the nearest/old diff.
        
        Pair< WorkspaceNode, Date > p =
                WorkspaceDiff.getCommonBranchPoint( ws1, ws2, timestamp1, timestamp2 );
        WorkspaceNode commonParent = p.first;
        Date commonBranchTime = p.second;
        
        Date date1 = WorkspaceDiff.dateFromWorkspaceTimestamp( timestamp1 );
        Date date2 = WorkspaceDiff.dateFromWorkspaceTimestamp( timestamp2 );
        Date date0_1 = null;
        Date date0_2 = null;
        date0_1 = commonBranchTime;
        date0_2 = commonBranchTime;
        
        // This assumes that the timepoint of the new diff is after the
        // timepoint of the old for each workspace.
        JSONObject diff1Json = performDiff( ws1, ws1, date0_1, date1, getResponse(),
                                            getResponseStatus(), DiffType.COMPARE, false );
        JSONObject diff2Json = performDiff( ws2, ws2, date0_2, date2, getResponse(),
                                            getResponseStatus(), DiffType.COMPARE, false );
        
        JsonDiffDiff diffDiffResult =
                WorkspaceDiff.performDiffGlom(diff1Json, diff2Json, commonParent,
                                 commonBranchTime, services, response, diffType );
        
        JSONObject diffResult = diffDiffResult.toJsonObject();
        
        // Add workspace meta-data:
        JSONObject ws1Json = diffResult.getJSONObject( "workspace1" );
        JSONObject ws2Json = diffResult.getJSONObject( "workspace2" );
        WorkspaceNode.addWorkspaceMetadata( ws1Json, ws1, dateTime1 );
        WorkspaceNode.addWorkspaceMetadata( ws2Json, ws2, dateTime2 );
        
        JSONArray ws1Elems = new JSONArray(); 
        JSONArray ws2Elems = new JSONArray(); 
        
        JSONArray addedElems = new JSONArray();
        JSONArray updatedElems = new JSONArray();
        JSONArray deletedElems = new JSONArray();
        
        ws1Elems = ws1Json.getJSONArray("elements");
        addedElems = ws2Json.getJSONArray("addedElements");
        updatedElems = ws2Json.getJSONArray("updatedElements");
        deletedElems = ws2Json.getJSONArray("deletedElements");
        
        for (int i = 0; i < addedElems.length(); i++){
        	ws2Elems.put(addedElems.getJSONObject(i));
        }
        for (int i = 0 ; i < updatedElems.length(); i++){
        	ws2Elems.put(updatedElems.getJSONObject(i));
        }
        for (int i = 0; i < deletedElems.length(); i++){
        	ws2Elems.put(deletedElems.getJSONObject(i));
        }
        
        /*for (int i = ws2Elems.length() - 1; i < ws1Elems.length(); i++){
        	ws2Elems.put(ws1Elems.getJSONObject(i));
        }*/
        
        
        //create a Map of keys from elements and sysmlId
        Set<String> sysmlIdMap = new HashSet<String>();
        for (int i = 0; i< ws1Elems.length(); i++){
        	JSONObject elem = ws1Elems.getJSONObject(i);
        	sysmlIdMap.add(elem.getString("sysmlid"));
        }
        for (int i = 0; i< deletedElems.length(); i++){
        	JSONObject elem = deletedElems.getJSONObject(i);
        	sysmlIdMap.add(elem.getString("sysmlid"));
        }
        for (int i = 0; i< updatedElems.length(); i++){
        	JSONObject elem = updatedElems.getJSONObject(i);
        	sysmlIdMap.add(elem.getString("sysmlid"));
        }
        for (int i = 0; i< addedElems.length(); i++){
        	JSONObject elem = addedElems.getJSONObject(i);
        	sysmlIdMap.add(elem.getString("sysmlid"));
        }
        
        //Add parents of all the elements if they do not already exist
		int l = ws2Elems.length();
		for (int i = 0; i < l; i++) {
			JSONObject elem = ws2Elems.getJSONObject(i);
			if (elem == null || !elem.has("sysmlid") ) {
			    // TODO -- ERROR!
			    continue;
			}
		    String childId = elem.optString("sysmlId");
		    String parentId = elem.optString("owner");
		    JSONObject childJson = null;
		    while ( parentId != null ) {
		        JSONObject parentJson =
		                getElementJson(parentId, diffDiffResult,
		                               ws1, dateTime1, getServices() );
		        if ( parentJson == null ) {
		            // TODO -- ERROR!
		            break;
		        }
                // Adding the child instead of the parent so that we do not add
                // the project, which has no owner.
		        if ( childJson != null && childId != null &&
		             !sysmlIdMap.contains( childId ) ) {
		            ws1Elems.put( childJson );
		            sysmlIdMap.add( childId );
		        }
		        childJson = parentJson;
		        childId = parentId;
		        parentId = parentJson.optString( "owner" );
		    }
		}

        return diffResult; 
    }
    
    /**
     * Get the JSONObject for the element with the specified id in the
     * jsonDiffDiff diffMap1 elements or from the database at the specified
     * workspace and time.
     * 
     * @param id
     * @param jsonDiffDiff
     * @param ws
     * @param dateTime
     * @param services
     * @return the element JSON
     */
    public static JSONObject getElementJson( String id, JsonDiffDiff jsonDiffDiff,
                                             WorkspaceNode ws, Date dateTime,
                                             ServiceRegistry services ) {
        JSONObject elementJson = jsonDiffDiff.getElement1( id );
        if ( elementJson == null ) {
            NodeRef ref = NodeUtil.findNodeRefById( id, false, ws, dateTime,
                                                    services, true );
            if ( NodeUtil.exists( ref ) ) {
                EmsScriptNode node = new EmsScriptNode( ref, services );
                elementJson = node.toJSONObject( ws, dateTime );
            }
        }
        
        return elementJson;
    }

    public void performDiff(Map<String, Object> results, DiffType diffType) {
       
        boolean switchUser = !originalUser.equals( "admin" );
        
        if ( switchUser ) AuthenticationUtil.setRunAsUser( "admin" );
        // to make sure no permission issues, run as admin
       
        JSONObject top = null;
        
        if ( glom ) {
            top = performDiffGlom( results, diffType );
        } else {
            // TODO REVIEW does the static glom flag of this method also
            //      supposed to control the glom flag of WorkspaceDiff?
            //      If it is, then we should pass true for forceNonGlom
            //      in the function call below.
            top = performDiff( ws1, ws2, dateTime1, dateTime2, response,
                               responseStatus, diffType, false );
        }
        if ( top == null ) {
            results.put( "res", createResponseJson() );
        } else {
            results.put( "res", NodeUtil.jsonToString( top, 4 ) );
        }

        if ( switchUser ) AuthenticationUtil.setRunAsUser( originalUser );
        
    }
    
    protected JSONObject diffJsonFromJobNode( EmsScriptNode jobNode ) {
    	if (jobNode == null)
    		return null;
        ContentReader reader = services.getContentService().getReader(jobNode.getNodeRef(), 
                                                                      ContentModel.PROP_CONTENT);

        if (reader != null) {
            try {
                JSONObject diffResults = new JSONObject(reader.getContentString());
                return diffResults;
            } catch (ContentIOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    
    
    public String getJobName( String timestamp1, String timestamp2, DiffType diffType ) {
        if ( diffJobName == null ) {
            diffJobName = getDiffJobName( timestamp1, timestamp2, diffType );
        }
        return diffJobName;
    }

    public static String getDiffJobName(String timestamp1, String timestamp2, DiffType diffType) {
        String timeString1 = timestamp1.replace( ":", "_" );
        String timeString2 = timestamp2.replace( ":", "_" );
        String timeString = timeString1 + "_" + timeString2;
        String diffJobName = "Diff_Job_" + timeString + "_" + diffType.name() + ".json";
        return diffJobName;
    }
    
    public EmsScriptNode getDiffJob(DiffType diffType) {// String timestamp1,
                                     //String timestamp2 ) {
        if ( diffJob == null ) {
            diffJob = getDiffJob( ws1, ws2, getDiffJobName(timestamp1, timestamp2, diffType),
                                  getServices(), getResponse() );
        }
        return diffJob;
    }

    public static EmsScriptNode getDiffJob( WorkspaceNode ws1, 
                                            WorkspaceNode ws2,
                                            String jobName,
                                            ServiceRegistry services,
                                            StringBuffer response ) {
        //String jobName = getDiffJobName( timestamp1, timestamp2 );
        EmsScriptNode companyHome = NodeUtil.getCompanyHome( services );
        
        // Check if there is old diff job using the diffTime if its non-null, otherwise
        // use the jobName:
        EmsScriptNode oldJob = ActionUtil.getDiffJob( companyHome, ws1, ws2, 
                                                      jobName, 
                                                      services, response );
        return oldJob;
    }
    
    public boolean diffIsOutDated( EmsScriptNode oldJob,
                                   String latestCommitTime1, String latestCommitTime2 ) {
        String foundTimeStamp1 = (String) oldJob.getProperty( "ems:timestamp1" );
        String foundTimeStamp2 = (String) oldJob.getProperty( "ems:timestamp2" );
                                                        
        // Diff is not outdated:
        if (foundTimeStamp1 != null && foundTimeStamp2 != null
            && latestCommitTime1 != null && latestCommitTime2 != null
            && foundTimeStamp1.equals(latestCommitTime1) 
            && foundTimeStamp2.equals(latestCommitTime2)) {
            return false;
        }
        return true;
    }
    
    protected void saveAndStartAction( WebScriptRequest req,
                                       String latestCommitTime1,
                                       String latestCommitTime2,
                                       Status status,
                                       DiffType diffType) {

        if (timestamp1 != null && timestamp2 != null) {

            String ws1Name = WorkspaceNode.getWorkspaceName(ws1);
            String ws2Name = WorkspaceNode.getWorkspaceName(ws2);
            EmsScriptNode companyHome = NodeUtil.getCompanyHome( services );

            // Check if there is old diff job using the diffTime if its non-null, otherwise
            // use the jobName:
            String jobName = getDiffJobName( timestamp1, timestamp2, diffType );
            EmsScriptNode oldJob = getDiffJob(diffType);

            diffStatus = DIFF_IN_PROGRESS;
            boolean reComputeDiff = true;
            boolean readJson = false;
            if ( oldJob != null ) {
                
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
                        if (timestamp1.equals( WorkspaceDiff.LATEST_NO_TIMESTAMP ) || 
                            timestamp2.equals( WorkspaceDiff.LATEST_NO_TIMESTAMP )) {
                            
                            if ( !diffIsOutDated( oldJob, latestCommitTime1, latestCommitTime2 ) ) {
                                // Diff is not outdated:
                                errorMsg = 
                                        String.format("Found up-to-date background diff: job[%s]",
                                                      jobName);
                                log( Level.INFO, errorMsg );
                            }
                            // Diff is outdated:
                            else {
                                if (recalculate) {
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
                
                diffStatus = DIFF_STARTED;
                
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
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_TIMESTAMP_1, timestamp1);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_TIMESTAMP_2, timestamp2);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_WS_1, ws1);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_WS_2, ws2);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.OLD_JOB, oldJob );
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_DIFF_TYPE, diffType );

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
