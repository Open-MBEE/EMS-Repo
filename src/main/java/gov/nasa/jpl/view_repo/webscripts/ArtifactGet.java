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
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Gets artifacts.  Replaces artifact.get.js, as this version is workspace aware.
 *
 * @author gcgandhi
 *
 */
public class ArtifactGet extends AbstractJavaWebScript {
	public ArtifactGet() {
	    super();
	}

    public ArtifactGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		if (!checkRequestContent(req)) {
			return false;
		}
		return true;
	}

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    	ArtifactGet instance = new ArtifactGet(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions );
    }

//    protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
//                                                     final Status status, final Cache cache,
//                                                     boolean withoutTransactions ) {
//        if ( withoutTransactions ) {
//            return executeImplImpl( req, status, cache );
//        }
//        final Map< String, Object > model = new HashMap<String, Object>();
//        new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
//            @Override
//            public void run() {
//                Map< String, Object > m = executeImplImpl( req, status, cache );
//                if ( m != null ) {
//                    model.putAll( m );
//                }
//            }
//        };
////            UserTransaction trx;
////            trx = services.getTransactionService().getNonPropagatingUserTransaction();
////            try {
////                trx.begin();
////                NodeUtil.setInsideTransactionNow( true );
////            } catch ( Throwable e ) {
////                String msg = null;
////                tryRollback( trx, e, msg );
////            }
//        //Map<String, Object> model = new HashMap<String, Object>();
//        if ( response != null && response.toString().length() > 0 ) {
//            model.put( "res", response.toString() );
//        }
//        return model;
//    }


//    private void tryRollback( UserTransaction trx , Throwable e, String msg ) {
//        if ( msg == null || msg.length() <= 0 ) {
//            msg = "DB transaction failed";
//        }
//        try {
//            log( LogLevel.ERROR,
//                 msg + "\n\t####### ERROR: Need to rollback:" + e.getMessage(),
//                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
//            log( LogLevel.ERROR,
//                 " "
//                         + e.getMessage() );
//            trx.rollback();
//            NodeUtil.setInsideTransactionNow( false );
//            e.printStackTrace();
//        } catch ( Throwable ee ) {
//            log( LogLevel.ERROR,
//                 "\tMmsModelDelete.handleRequest: rollback failed: "
//                         + ee.getMessage() );
//            ee.printStackTrace();
//            e.printStackTrace();
//        }
//    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {

		JSONObject resultJson = null;
		Map<String, Object> model = new HashMap<String, Object>();

        printHeader( req );
		clearCaches();

        String cs = req.getParameter("cs");
        String extensionArg = req.getParameter("extension");
        String extension = extensionArg != null ? extensionArg : ".svg";  // Assume .svg if no extension provided
        String timestamp = req.getParameter("timestamp");

        if (!Utils.isNullOrEmpty(extension) && !extension.startsWith(".")) {
        	extension = "." + extension;
        }

        WorkspaceNode workspace = getWorkspace( req, AuthenticationUtil.getRunAsUser());

        if (validateRequest(req,status) ) {

        	try {
	        	// Get the artifact name from the url:
	        	String artifactIdPath = getArtifactId(req);

	        	if (artifactIdPath != null) {
	        		int lastIndex = artifactIdPath.lastIndexOf("/");

	        		if (artifactIdPath.length() > (lastIndex+1)) {

	        			String artifactId = lastIndex != -1 ? artifactIdPath.substring(lastIndex+1) : artifactIdPath;
	        			String filename = extension != null ? artifactId + extension : artifactId;

	    	    		EmsScriptNode matchingNode = null;

	    	        	// Search for artifact file by checksum (this may return nodes in parent workspaces):
	        			if (!Utils.isNullOrEmpty(cs)) {
		    	        	ArrayList< NodeRef > refs = NodeUtil.findNodeRefsByType( "" + cs,
																	          SearchType.CHECKSUM.prefix, false,
																	          workspace,
																	          TimeUtils.dateFromTimestamp( timestamp ),
																	          false, false,
																	          services, false );
		    	    		List< EmsScriptNode > nodeList = EmsScriptNode.toEmsScriptNodeList( refs, services, response, status );

		    	    		// Find the first node with matching name (just in case there is multiple artifacts with
		    	    		// the same checksum but different names):
		    	    		for (EmsScriptNode node : nodeList) {

		    	        		if (node.getSysmlId().equals(filename)) {
		    	        			matchingNode = node;
		    	        			break;
		    	        		}
		    	    		}
	        			}
	        			// Otherwise, search by the id (this may return nodes in parent workspaces):
	        			else {
	        				matchingNode = NodeUtil.findScriptNodeById(filename, workspace,
	        						 								   TimeUtils.dateFromTimestamp( timestamp ),
	        						 								   false, services, response);
	        			}

	    	    		// Create return json if matching node found:
	    	    		if (matchingNode != null) {

		    	        	resultJson = new JSONObject();
		    	        	JSONArray jsonArray = new JSONArray();
		    	        	JSONObject jsonArtifact = new JSONObject();
		    	        	resultJson.put("artifacts",jsonArray);
		    	        	jsonArtifact.put("id", matchingNode.getSysmlId());
		    	        	String url = matchingNode.getUrl();
		    	        	if (url != null) {
		    	        		jsonArtifact.put("url", url.replace("/d/d/", "/service/api/node/content/"));
		    	        	}
		    	        	jsonArray.put(jsonArtifact);
	    	    		}
	    	    		else {
	    	    			String fileStr = "File "+filename;
	    	    			String err = Utils.isNullOrEmpty(cs) ? fileStr+" not found!\n" :
	    	    												  (fileStr+" with cs="+cs+" not found!\n");
						    log(LogLevel.ERROR, err, HttpServletResponse.SC_BAD_REQUEST);
						    model.put("res", response.toString());
	    	    		}

	        		}
	        		else {
	        			  log(LogLevel.ERROR, "Invalid artifactId!\n", HttpServletResponse.SC_BAD_REQUEST);
	    		          model.put("res", response.toString());
	    		    }

	        	}
	        	else {
	        		  log(LogLevel.ERROR, "artifactId not supplied!\n", HttpServletResponse.SC_BAD_REQUEST);
    		          model.put("res", response.toString());
    		    }

        	}
	        catch (JSONException e) {
	            log(LogLevel.ERROR, "Issues creating return JSON\n", HttpServletResponse.SC_BAD_REQUEST);
	            e.printStackTrace();
	            model.put("res", response.toString());
	        }
        }
        else {
        	log(LogLevel.ERROR, "Invalid request!\n", HttpServletResponse.SC_BAD_REQUEST);
        	model.put("res", response.toString());
	    }

        status.setCode(responseStatus.getCode());
        if ( !model.containsKey( "res" ) ) {
        	model.put("res", resultJson != null ? resultJson : response.toString());
        }

		printFooter();

		return model;
	}

}
