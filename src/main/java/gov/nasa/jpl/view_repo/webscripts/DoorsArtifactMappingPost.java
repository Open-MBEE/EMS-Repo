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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.NodeUtil;



/**
 *
 * @author Bruce Meeks Jr
 *
 */
public class DoorsArtifactMappingPost extends AbstractJavaWebScript {
	

    static PostgresHelper pgh = null;
    
    static Logger logger = Logger.getLogger(DoorsArtifactMappingPost.class);
        

    public DoorsArtifactMappingPost() {
    	
        super();
        
    }

    public DoorsArtifactMappingPost(Repository repositoryHelper, ServiceRegistry registry) {
    	
        super(repositoryHelper, registry);
        
    }

   
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    	DoorsArtifactMappingPost instance = new DoorsArtifactMappingPost(repository, getServices());

        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
    	
    	Map<String, Object> postStatus = new HashMap<String, Object>();
    	JSONObject message = new JSONObject();
    	    
    	String postedConfiguration = "";
        pgh = new PostgresHelper(getWorkspace(req));
                
        
        try {
        	
        	pgh.connect();
        	
        	postedConfiguration = req.getContent().getContent().toString();
        	
        	postStatus = storeArtifactMappings(postedConfiguration,false);
        
        }
        catch(SQLException e) {
        	
        	 e.printStackTrace();
			 message.put("status", "Could not connect to database");
			 postStatus.put("res", NodeUtil.jsonToString(message));
		        
		     if (logger.isDebugEnabled()) {
	                logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Could not connect to database\n");
	                e.printStackTrace();
	          }
		     
		     return postStatus;
        	
        }
        catch(IOException e) {
        	
        	e.printStackTrace();
        	message.put("status", "Could not read from I/O");
			postStatus.put("res", NodeUtil.jsonToString(message));
		        
		    if (logger.isDebugEnabled()) {
	            logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Could not read from I/O\n");
	            e.printStackTrace();
	        }
		     
		     return postStatus;
        	
        }
        catch(ClassNotFoundException e) {
        	
        	e.printStackTrace();
        	message.put("status", "Class not found");
			postStatus.put("res", NodeUtil.jsonToString(message));
		        
		    if (logger.isDebugEnabled()) {
	            logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Class not found\n");
	            e.printStackTrace();
	        }
		    
		     return postStatus;

        }
        
   
    	 return postStatus;
    	 
    	 
    }
    
    
    
    private static Map<String, Object> storeArtifactMappings(String configuration, boolean staticLoading) {
    	
    	Map<String, Object> postStatus = new HashMap<String, Object>();
    	JSONObject message = new JSONObject();
    	
    	try {
    		
    		
			JSONArray configurations = new JSONArray(configuration);
			JSONObject curProj = new JSONObject();
			String project = "";
			String doorsArtifactType = "";
			JSONArray artifactMappings = new JSONArray();
			JSONObject curArtifactMapping = new JSONObject();
			JSONArray appliedMetatypeArray = new JSONArray();
			JSONObject curAppliedMetatype = new JSONObject();
			
			
			if(staticLoading) {
				
				pgh = new PostgresHelper("null");
				
				try {
					
					pgh.connect();
					
					pgh.execUpdate("CREATE TABLE doorsartifactmappings (project text not null, doorsartifacttype text not null, sysmlappliedmetatype text not null);");
				
				}
				catch(SQLException e) {
					
					e.printStackTrace(); //table may already exists, no prob
					
				}
				
			}
			
			 //For now must wipe table first, else can't account for artifact mapping removals; treating new post like gold configuration
 		    if(pgh.execQuery("SELECT * from doorsartifactmappings").next()) {

	    			pgh.execUpdate("delete from doorsartifactmappings");

 		    }
		
			

		    for(int p=0; p < configurations.length(); p++) {
		 
				curProj = (JSONObject)configurations.get(p);
				
				project = curProj.getString("project");
				
				artifactMappings = curProj.getJSONArray("artifactMappings");

				
			    for(int a=0; a < artifactMappings.length(); a++) {

			    	curArtifactMapping = (JSONObject)artifactMappings.get(a);
			    	
			    	doorsArtifactType = curArtifactMapping.getString("doorsArtifactType");
			    	
			    	appliedMetatypeArray = curArtifactMapping.getJSONArray("appliedMetatypeIDs");
			    	
				    for(int a2=0; a2 < appliedMetatypeArray.length(); a2++) {
				    	
				    	curAppliedMetatype = (JSONObject)appliedMetatypeArray.get(a2);
				    	
				    		try {
				    				    		  			
				    			pgh.execUpdate("insert into doorsartifactmappings (project, doorsartifacttype, sysmlappliedmetatype) VALUES ('" + project + "','" + doorsArtifactType  + "','" + curAppliedMetatype.getString("id") + "')");

				    		} 
				    		catch (SQLException e) {
				    			
				    			 e.printStackTrace();
								 message.put("status", "Problem inserting artifact mapping into database");
							     postStatus.put("res", NodeUtil.jsonToString(message));
							        
							     if (logger.isDebugEnabled()) {
							        	
						                logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Problem inserting configuration into database\n");
						                e.printStackTrace();
						                
						          }
					 	   
							     return postStatus;
	    		 			
	    		 			
				    		}
	    		 		
				    	
				    	
				    }

			    		
			    }
			    
			    
		 
		    }
			
			
    	}
        catch(JSONException e) {
    		
    		e.printStackTrace();
    		message.put("status", "Invalid JSON input configuration");
		    postStatus.put("res", NodeUtil.jsonToString(message));
		        
		    if (logger.isDebugEnabled()) {
		        	
	             logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Invalid JSON Array input\n");
	             e.printStackTrace();
	                
	        }
		       
		   return postStatus;
    		
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		message.put("status", "Internal error occurred");
		    postStatus.put("res", NodeUtil.jsonToString(message));
		        
		    if (logger.isDebugEnabled()) {
		        	
	             logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Internal Error occurred\n");
	             e.printStackTrace();
	                
	        }
		    
		    return postStatus;

    	}
    	
    	
    	 staticLoading = false;
    	
    	 message.put("status", "Doors Artifact Types mapped successfully");
	     postStatus.put("res", NodeUtil.jsonToString(message));
	     
	     return postStatus;
    	
    	
    }
    
    
    
    public static boolean bootstrapArtifactMappings() {
    	
    	
    	BufferedReader configurationReader = null;
    	String configurationInput = "";
    	
    	try {
    		
    		 configurationReader = new BufferedReader( new FileReader("doorsArtifactMappings.txt"));
    		 
    		 while ((configurationInput = configurationReader.readLine()) != null ) {
				
    			     storeArtifactMappings(configurationInput,true);
    			     
    				 configurationReader.close();

				     break;
				
    		 }
    		 
    	}
        catch(FileNotFoundException e) {
    	    		
    		if (logger.isDebugEnabled()) {
    			logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Could not find doors aritact type mapping " + "\n");
    			e.printStackTrace();
    		}
    			
    	}
    	catch(IOException e) {
    		
	    	if (logger.isDebugEnabled()) {
	    		logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Problem reading configuration file\n");
	    		e.printStackTrace();
	        }
	    		
    	}
    	catch(Exception e) {
    	    		
    	    if (logger.isDebugEnabled()) {
    	    		logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Problem reading configuration file\n");
    	    		e.printStackTrace();
    	    }
    	    		
    	}
    		 
    		 
    	return true;
    	
    	
    }
    
    
    
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {

        if (!checkRequestContent(req)) {
            return false;
        }

        String id = req.getServiceMatch().getTemplateVars().get(WORKSPACE_ID);
        if (!checkRequestVariable(id, WORKSPACE_ID)) {
            return false;
        }

        return true;
    }
   
}
