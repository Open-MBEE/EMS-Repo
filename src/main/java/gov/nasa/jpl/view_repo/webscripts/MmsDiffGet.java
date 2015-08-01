package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Pair;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    private final static String DIFF_STARTED = "STARTED";
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
    
    /**
     * Calculate the diff that would result after applying one diff followed by
     * another, "glomming" them together. This can be done by making a copy of
     * the first diff and modifying it based on the second.
     * <p>
     * In the element diff json, there is a workspace1 to show the original
     * elements and a workspace2 for the changes to those elements. Only
     * properties that have changed are included in the added and updated
     * elements in the workspace2 JSON, so the actual element in workspace 2 is
     * computed as the element in workspace1 (if it exists) augmented or
     * overwritten with the properties in the corresponding workspace2 element.
     * <p>
     * So, how do we merge two diff JSON objects? The workspace1 in diff2 could
     * be a modification of the workspace1 in diff1, possibly sa a result of the
     * workspace2 changes in diff1. In this case, it makes sense to use diff1's
     * workspace1 as that of the glommed diff since it is the pre-existing state
     * of the workspace before both diffs are applied. If this is not the case,
     * then it might make sense to add elements in workspace1 of diff2 that are
     * not in workspace1 of diff1 and that are not added by workspace2 of diff1.
     * <p>
     * To combine the workspace2 changes of the two diffs, the changes in
     * workspace2 of diff2 should be applied to those of workspace2 of diff1 to
     * get the glommed workspace2 changes. But, how to do this at the property
     * level is not obvious. For example, if diff1 and diff2 add the same
     * element with different properties, should the individual properties of
     * the add in diff2 be merged with those of diff1 or should the diff2 add
     * replace the diff1 add? This situation may indicate a conflict in
     * workspaces that the user should control. If the element were a view, then
     * merging would not make much sense, especially if it leads to
     * inconsistency among its properties. So, replacing the add is chosen as
     * the appropriate behavior. Below is a table showing how workspace2 changes
     * ore glommed:
     * 
     * <table style="width:100%", border="1">
     *  <tr>
     *    <th></th>
     *    <th>add(x2) </th>
     *    <th>delete(x)</th> 
     *    <th>update(x2)</th>
     *  </tr>
     *  <tr>
     *    <th>add(x1)</th>
     *    <td>add(x2) [potential conflict]</td>
     *    <td>delete(x)</td>
     *    <td>add(x1 &lt;- x2)</td>
     *  </tr>
     *  <tr>
     *    <th>delete(x)</th>
     *    <td>add(x2)</td>
     *    <td>delete(x)</td>
     *    <td>update(x2) [potential conflict]</td>
     *  </tr>
     *  <tr>
     *    <th>update(x1)</th>
     *    <td>update(x2) [potential conflict]</td>
     *    <td>delete(x)</td>
     *    <td>update(x1 &lt;- x2)</td>
     *  </tr>
     * </table>
     * 
     * @param diff1
     *            workspace diff JSON
     * @param diff2
     *            workspace diff JSON
     * @return the combined diff of applying diff1 followed by diff2
     */
    public JSONObject glom( JSONObject diff1, JSONObject diff2 ) {
       JSONObject diff3 = NodeUtil.clone( diff1 ); 
        return null;
    }

    enum DiffOp { ADD, UPDATE, DELETE };
    
    public JSONObject glom( ArrayList<JSONObject> diffs ) {
        if ( Utils.isNullOrEmpty( diffs ) ) return null;
        JSONObject glommedDiff = makeEmptyDiffJson();
        if ( diffs.size() == 1 ) return glommedDiff;
        LinkedHashMap<String, Pair<DiffOp, List<JSONObject> > > diffMap1 =
                new LinkedHashMap< String, Pair<DiffOp,List<JSONObject>> >();
        LinkedHashMap<String, Pair<DiffOp, List<JSONObject> > > diffMap2 =
                new LinkedHashMap< String, Pair<DiffOp,List<JSONObject>> >();
        
        // Glom workspace 1 changes        
        // Iterate through each diff in order adding any new elements that were
        // not in previous diffs.
        JSONArray elements = glommedDiff.getJSONArray( "elements" );
        for ( int i = 0; i < diffs.size(); ++i ) {
            JSONObject diff =  diffs.get( i );
            JSONObject ws1 = diff.optJSONObject( "workspace1" );            
            JSONArray dElements = ws1.getJSONArray( "elements" );
            for ( int j = 0; j < dElements.length(); ++j ) {
                JSONObject element = dElements.getJSONObject( j );
                String sysmlid = element.getString( "sysmlid" );
                if ( !diffMap1.containsKey( sysmlid ) ) {
                    elements.put( element );
                }
            }
        }
        
        // Glom workpace 2 changes
        for ( JSONObject diff : diffs ) {
            JSONObject ws2 = diff.optJSONObject( "workspace2" );
            if ( ws2 == null ) continue;
            JSONArray added = ws2.optJSONArray( "addedElements" );
            JSONArray updated = ws2.optJSONArray( "updatedElements" );
            JSONArray deleted = ws2.optJSONArray( "deletedElements" );
            // Diffs are applied in the order of add, update, delete
            glom( DiffOp.ADD, added, diffMap2 );
            glom( DiffOp.UPDATE, updated, diffMap2 );
            glom( DiffOp.DELETE, deleted, diffMap2 );
        }

        // now we need to merge the properties of chained updates
        JSONObject gws2 = glommedDiff.getJSONObject( "workspace2" );
        JSONArray added = gws2.getJSONArray( "addedElements" );
        JSONArray updated = gws2.getJSONArray( "updatedElements" );
        JSONArray deleted = gws2.getJSONArray( "deletedElements" );
        for ( Entry< String, Pair< DiffOp, List< JSONObject > > > entry : diffMap2.entrySet() ) {
            Pair< DiffOp, List< JSONObject > > p = entry.getValue();
            JSONObject glommedElement = null; //NodeUtil.newJsonObject();
            for ( JSONObject element : p.second ) {
                if ( glommedElement == null ) glommedElement = NodeUtil.clone( element );
                else addProperties( glommedElement, element );
            }
            switch ( p.first ) {
                case ADD:
                    added.put( glommedElement );
                    break;
                case UPDATE:
                    updated.put( glommedElement );
                    break;
                case DELETE:
                    deleted.put( glommedElement );
                    break;
                default:
                    // BAD! -- TODO
            }
            // TODO -- What about moved and conflicted elements?
        }
        
        return glommedDiff;
     }

    public static JSONObject makeEmptyDiffJson() throws JSONException {
        JSONObject diffJson = NodeUtil.newJsonObject();
        JSONObject ws1Json = NodeUtil.newJsonObject();
        JSONObject ws2Json = NodeUtil.newJsonObject();

        diffJson.put("workspace1", ws1Json);
        diffJson.put("workspace2", ws2Json);
        
        JSONArray ws1Elements = new JSONArray();
        JSONArray ws2Added = new JSONArray();
        JSONArray ws2Updated = new JSONArray();
        JSONArray ws2Deleted = new JSONArray();
        ws1Json.put( "elements", ws1Elements );
        ws2Json.put( "addedElements", ws2Added );
        ws2Json.put( "updatedElements", ws2Updated );
        ws2Json.put( "deletedElements", ws2Deleted );

        // TODO -- moved and conflicted elements

        return diffJson;
    }

    
    /**
     * Glom the specified elements per the specified operation to the glom map.
     * The map is used to avoid unnecessary merging of updates. For example,
     * three updates followed by a delete requires no update merging since the
     * element is getting deleted anyway. The map tracks the minimum number of
     * operation to glom all of the diffs.
     * 
     * @param op
     *            the ADD, UPDATE, or DELETE operation to apply to the elements
     * @param elements
     *            the elements to which the operation is applied and glommed
     *            with the glom map
     * @param glomMap
     *            a partial computation of a diff glomming as a map from sysmlid
     *            to an operation and a list of elements whose properties will
     *            be merged
     */
    protected void glom( DiffOp op,
                         JSONArray elements,
                         LinkedHashMap<String, Pair<DiffOp, List<JSONObject> > > glomMap ) {
        if ( glomMap == null || elements == null) return;
        // Apply the operation on each element to the map of the glommed diff (glomMap) 
        for ( int i = 0; i < elements.length(); ++i ) {
            JSONObject element = elements.optJSONObject( i );
            if ( element == null ) continue;
            String sysmlId = element.optString( Acm.JSON_ID );
            if ( sysmlId == null ) continue;
            Pair< DiffOp, List< JSONObject > > p = glomMap.get( sysmlId );
            // If there is no entry in the map for the sysmlid, create a new
            // entry with the operation and element.
            if ( p == null ) {
                p = new Pair< DiffOp,List< JSONObject > >( op, Utils.newList( element ) );
                glomMap.put( sysmlId, p );
            } else {
                switch( op ) {
                    case ADD:
                        // ADD always fully replaces ADD, UPDATE, and DELETE according to the
                        // table in the comments for glom( diff1, diff2).
                        p.second.clear();
                        p.second.add( element );
                        // already replaced above--now just update op for DELETE
                        switch ( p.first ) {
                            case ADD:
                                // ADD + ADD = ADD [potential conflict]
                            case UPDATE:
                                // UPDATE + ADD = UPDATE [potential conflict]
                                break;
                            case DELETE:
                                // DELETE + ADD = ADD
                                p.first = DiffOp.ADD;
                            default:
                                // BAD! -- TODO
                        }
                        break;
                    case UPDATE:
                        // UPDATE replaces DELETE but augments UPDATE and ADD
                        switch ( p.first ) {
                            case ADD:
                                // ADD + UPDATE = ADD --> augment
                            case UPDATE:
                                // UPDATE + UPDATE = UPDATE --> augment
                                p.second.add( element );
                                break;
                            case DELETE:
                                // DELETE + UPDATE = UPDATE --> replace [potential conflict]
                                p.first = DiffOp.UPDATE;
                                p.second.clear();
                                p.second.add( element );
                            default:
                                // BAD! -- TODO
                        }
                        break;
                    case DELETE:
                        // DELETE always fully replaces ADD and UPDATE. No
                        // change to an already deleted element.
                        switch ( p.first ) {
                            case ADD:
                                // ADD + DELETE = DELETE --> replace
                            case UPDATE:
                                // UPDATE + DELETE = DELETE --> replace
                                p.first = DiffOp.DELETE;
                                p.second.clear();
                                p.second.add( element );
                                break;
                            case DELETE:
                                // DELETE + DELETE = DELETE (no change)
                            default:
                                // BAD! -- TODO
                        }
                        break;
                    default:
                        // BAD! -- TODO
                }
            }
        }
    }
    
    protected static HashSet<String> ignoredJsonIds = new HashSet<String>() {
        {
            add("sysmlid");
            add("creator");
            add("modified");
            add("created");
            add("modifier");
        }
    };

    protected void addProperties( JSONObject element1,
                                  JSONObject element2 ) {
        Iterator i = element2.keys();
        while ( i.hasNext() ) {
            String k = (String)i.next();
            if ( ignoredJsonIds.contains( k ) ) continue;
            element1.put( k, element2.get( k ) );
        }
    }

//    private JSONObject glomProperties( JSONObject element1,
//                                       JSONObject element2 ) { 
//        JSONObject elementG = NodeUtil.clone( element1 );
//        Iterator i = element2.keys();
//        while ( i.hasNext() ) {
//            String k = (String)i.next();
//            if ( ignoredJsonIds.contains( k ) ) continue;
//            elementG.put( k, element2.get( k ) );
//        }
//        return elementG;
//    }
    
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
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_WS_1, ws1);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.PARAM_WS_2, ws2);
                loadAction.setParameterValue(WorkspaceDiffActionExecuter.OLD_JOB, oldJob );

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
