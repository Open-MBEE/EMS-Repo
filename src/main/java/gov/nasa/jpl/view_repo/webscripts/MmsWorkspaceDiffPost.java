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

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
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
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.version.Version;
import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsWorkspaceDiffPost extends ModelPost {
	public MmsWorkspaceDiffPost() {
	    super();
	}

    public MmsWorkspaceDiffPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!userHasWorkspaceLdapPermissions()) {
            return false;
        }
        return true;
	}


	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
	    MmsWorkspaceDiffPost instance = new MmsWorkspaceDiffPost(repository, getServices());
	    return instance.executeImplImpl( req, status, cache, true);
	}


    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

		//clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();
		JSONObject top = NodeUtil.newJsonObject();
		
		try {
		    JSONObject json = //JSONObject.make( 
		            (JSONObject)req.parseContent(); //);
		    top = handleDiff(req, json, status);
		} catch ( Exception e ) {
            log(LogLevel.ERROR, "Internal server error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    top.put("message", response.toString());
                }
                model.put("res", top.toString(4));
            } catch ( JSONException e ) {
                log(LogLevel.ERROR, "JSON parse exception: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                if (!model.containsKey( "res" )) {
                    model.put( "res", createResponseJson() );
                }
                e.printStackTrace();
            }
        }

        status.setCode(responseStatus.getCode());

		printFooter();

		return model;
	}

    JSONObject srcJson = null;
    JSONObject targetJson = null;
    WorkspaceNode targetWs = null;
    String targetWsId = null;
    String srcWsId = null;
    WorkspaceNode srcWs = null;
    String timestamp1 = null;
    Date dateTimeTarget = null;
    String timestamp2 = null;
    Date dateTimeSrc = null;
    
    protected boolean foo(WebScriptRequest req) throws JSONException {
        srcWsId = srcJson.getString( "id" );
        srcWs = WorkspaceNode.getWorkspaceFromId( srcWsId, services, response, responseStatus, null );

        targetWsId = targetJson.getString( "id" );
        targetWs = WorkspaceNode.getWorkspaceFromId( targetWsId, services, response, responseStatus, null );

        timestamp1 = req.getParameter( "timestamp1" );
        dateTimeTarget = TimeUtils.dateFromTimestamp( timestamp1 );

        timestamp2 = req.getParameter( "timestamp2" );
        dateTimeSrc = TimeUtils.dateFromTimestamp( timestamp2 );

        // Verify that the target workspace timestamp is valid, ie it must use the latest
        // commit:
        if (dateTimeTarget != null) {
            // TODO REVIEW This is not efficient, as getLastCommit()
            //             and getLatestCommitAtTime() do similar operations
            EmsScriptNode lastCommit = CommitUtil.getLastCommit( targetWs, services, response );
            EmsScriptNode prevCommit = CommitUtil.getLatestCommitAtTime( dateTimeTarget,
                                                                         targetWs, services,
                                                                         response );

            // Give error message if there are not commits found before or at the dateTimeTarget:
            if (prevCommit == null) {
                log(LogLevel.ERROR,
                    "Try a later date.  Previous commit could not be found based on date "+dateTimeTarget,
                    HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }

            // Give error message if the latest commit based on the time is not the latest:
            if (lastCommit != null && prevCommit != null && !lastCommit.equals( prevCommit ) ) {

                log(LogLevel.ERROR,
                    "Previous commit "+prevCommit+" based on date "+dateTimeTarget+" is not the same as the latest commit "+lastCommit,
                    HttpServletResponse.SC_CONFLICT);
                return false;
            }
        }

        return true;
    }

    boolean succ = true;

    private JSONObject handleDiff(final WebScriptRequest req, final JSONObject jsonDiff, final Status status) throws Exception {
        
        long start = System.currentTimeMillis();
        JSONObject finalJsonDiff = NodeUtil.newJsonObject();
        
        populateSourceFromJson( jsonDiff );
		if (jsonDiff.has( "workspace1" ) && jsonDiff.has("workspace2")) {
		    srcJson = jsonDiff.getJSONObject( "workspace2" );
		    targetJson = jsonDiff.getJSONObject( "workspace1" );

		    if (srcJson.has( "id" ) && targetJson.has("id")) {
		    	
		    	//WorkspaceNode targetWs = null;
		        JSONObject top = NodeUtil.newJsonObject();
		        JSONArray elements = new JSONArray();
		        final MmsModelDelete deleteService = new MmsModelDelete(repository, services);
		    	
		        if (runWithoutTransactions) {
                    succ = foo(req);
		        }
		        else {
    		    	new EmsTransaction(getServices(), getResponse(), getResponseStatus()) {
    					
    					@Override
    					public void run() throws Exception {
    						succ = foo(req);
    					}
				};
		        }
				if ( !succ ) return finalJsonDiff;
				
		        // Add/update the elements in the target workspace:
		        // Must remove the modified time, as it is for the source workspace, not the target
		        // workspace, so may get errors for trying to modify a element with a old modified time.
		        if (srcJson.has( "addedElements" )) {
		            JSONArray added = srcJson.getJSONArray("addedElements");
		            for (int ii = 0; ii < added.length(); ii++) {
		                JSONObject obj = added.getJSONObject( ii );
		                if (obj.has( Acm.JSON_LAST_MODIFIED )) {
		                    obj.remove( Acm.JSON_LAST_MODIFIED );
		                }
		                elements.put( obj );
		            }
		        }
		        if (srcJson.has( "updatedElements" )) {
		            JSONArray updated = srcJson.getJSONArray("updatedElements");
		            for (int ii = 0; ii < updated.length(); ii++) {
		                JSONObject obj = updated.getJSONObject( ii );
		                if (obj.has( Acm.JSON_LAST_MODIFIED )) {
		                    obj.remove( Acm.JSON_LAST_MODIFIED );
		                }
		                elements.put( obj );
		            }
		        }
		        
	            top.put( "elements", elements );

	            Set<EmsScriptNode> updatedElements = handleUpdate( top, status, targetWs, false,
	                                                               new HashMap<String,Object>(), false );

	            // Delete the elements in the target workspace:
		        WorkspaceDiff deleteWsDiff = null;
	            if (srcJson.has( "deletedElements" )) {
	                final JSONArray deleted = srcJson.getJSONArray( "deletedElements" );
	                deleteService.setWsDiff( targetWs );

	                if (runWithoutTransactions) {
                        for (int ii = 0; ii < deleted.length(); ii++) {
                            String id = ((JSONObject)deleted.get(ii)).getString( "sysmlid" );
                            EmsScriptNode root = NodeUtil.findScriptNodeById( id, targetWs, null, false, services, response );
                            deleteService.deleteNodeRecursively( root, targetWs);
                        }
	                }
	                else {
    	                new EmsTransaction(getServices(), getResponse(), getResponseStatus()) {
    			    		@Override
    			    		public void run() throws Exception {
    		                    for (int ii = 0; ii < deleted.length(); ii++) {
    		                        String id = ((JSONObject)deleted.get(ii)).getString( "sysmlid" );
    		                        EmsScriptNode root = NodeUtil.findScriptNodeById( id, targetWs, null, false, services, response );
    		                        deleteService.deleteNodeRecursively( root, targetWs);
    		                    }
    						}
    					};
	                }

			        deleteWsDiff = deleteService.getWsDiff();
	            }

	            long end = System.currentTimeMillis();
	            
	            // Send deltas and make merge commit:
	            // FIXME: Need to split elements by project Id - since they won't always be in same project
	            String projectId = !updatedElements.isEmpty() ?
	                                           updatedElements.iterator().next().getProjectId(targetWs) :
	                                           NO_PROJECT_ID;
	            boolean modelPostDiff = wsDiff.isDiff();
	            boolean modelDeleteDiff = deleteWsDiff != null && deleteWsDiff.isDiff();
	            
	            if (modelDeleteDiff || modelPostDiff) {
	                
	                // Need to update the jsonDiff object with the diffs output from the model delete:
	                if (modelDeleteDiff) {
	                    Map<String,EmsScriptNode> currentElements = wsDiff.getElements();
	                    Map<String,Version> currentElementVersions = wsDiff.getElementsVersions();
                        Map<String,EmsScriptNode> currentDeletedElements = wsDiff.getDeletedElements();

                        // Add elements, elementVersions, deleted elements:
                        // Note: these all have the same key set for deleted nodes:
	                    for (Entry< String, EmsScriptNode > entry : deleteWsDiff.getElements().entrySet()) {
	                        String id = entry.getKey();
	                        if (!currentElements.containsKey( id )) {
	                            currentElements.put( id, entry.getValue() );
	                            if (deleteWsDiff.getElementsVersions().containsKey( id )) {
	                                currentElementVersions.put( id , deleteWsDiff.getElementsVersions().get( id ) );
	                            }
	                            if (deleteWsDiff.getDeletedElements().containsKey( id )) {
	                                currentDeletedElements.put( id , deleteWsDiff.getDeletedElements().get( id ) );
                                }
	                        }
	                    }
	                    
	                }
	                
	                // This has to be done before adding deleted aspects
	                finalJsonDiff = wsDiff.toJSONObject( new Date(start), new Date(end) ); 
	                
	                // Apply the deleted aspects if needed to the deleted nodes:
	                if (modelDeleteDiff) {
	                    	                    
	                    if (runWithoutTransactions) {
	                        deleteService.applyAspects();
	                    }
	                    else {
	                        new EmsTransaction(getServices(), getResponse(), getResponseStatus()) {
	                            @Override
	                            public void run() throws Exception {
	                                deleteService.applyAspects();
	                            }
	                        };
	                    }
	                }
	                
	                if ( !CommitUtil.sendDeltas(finalJsonDiff, targetWsId, projectId, source) ) {
                        logger.warn( "MmsWorkspaceDiffPost deltas not posted properly");
                    }

	                CommitUtil.merge( finalJsonDiff, srcWs, targetWs, dateTimeSrc, dateTimeTarget,
	                                  null, runWithoutTransactions, services, response );
	            }
	            
		    }
		}
		
        return finalJsonDiff;
	}
}
