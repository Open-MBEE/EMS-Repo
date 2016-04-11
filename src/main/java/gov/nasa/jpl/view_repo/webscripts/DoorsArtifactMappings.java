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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.view_repo.db.EdgeTypes;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;


/**
 *
 * @author Bruce Meeks Jr
 *
 */
public class DoorsArtifactMappings extends AbstractJavaWebScript {
	

    static PostgresHelper pgh = null;
    
    //global data structure that will encapsulate doors artifact mappings to sysmlappliedmetatypes for each project
    //backup in case of database problems during DoorsSync
    static HashMap<String,HashMap<String,ArrayList<String>>> artifactConfiguration = new HashMap<String,HashMap<String,ArrayList<String>>>();
    
    static Logger logger = Logger.getLogger(DoorsArtifactMappings.class);
        

    public DoorsArtifactMappings() {
    	
        super();
        
    }

    public DoorsArtifactMappings(Repository repositoryHelper, ServiceRegistry registry) {
    	
        super(repositoryHelper, registry);
        
    }

   
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    	DoorsArtifactMappings instance = new DoorsArtifactMappings(repository, getServices());

        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
    	
    	Map<String, Object> postStatus = new HashMap<String, Object>();
    	
    	JSONObject message = new JSONObject();
    	    	    	
        pgh = new PostgresHelper(getWorkspace(req));
        
        try {
        	
        	pgh.connect();
        
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
        catch(ClassNotFoundException e) {
        	
        	e.printStackTrace();
        }
        
	    
	   try {
		   
		   postStatus = storeArtifactMappings(req.getContent().getContent(),false);
	   
	   }
	   catch(IOException e) {
		   		   
		   e.printStackTrace();
		   message.put("status", "Problem reading configuration POST content");
		   postStatus.put("res", NodeUtil.jsonToString(message));
		        
		   if (logger.isDebugEnabled()) {
		        	
	             logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Problem reading configuration POST content\n");
	             e.printStackTrace();
	             
	                
	        }
		   
		   return postStatus;
		   
	   }
	
	    
	     
   
	   return postStatus;
    	 
        
    }
    
    
    
    
    private static Map<String, Object> storeArtifactMappings(String configuration, boolean staticLoading) {
    	
    	Map<String, Object> postStatus = new HashMap<String, Object>();
    	
    	JSONObject message = new JSONObject();
    	
    	JSONArray configurations = null;
    	
    	
    	try {
    		
    		configurations = new JSONArray(configuration);
		
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
					
					pgh.execQuery("CREATE TABLE doorsartifactmappings (project text not null, doorsartifacttype text not null, sysmlappliedmetatype text not null);");
					
				}catch(SQLException e) {
					
					e.printStackTrace(); //table may already exists
					
				}
				catch(ClassNotFoundException e) {
					
					e.printStackTrace(); 
					
				}
				
		}
			
			
		try {
				//For now must wipe table data clean first, else can't account for artifact mapping removals
	 		    if(pgh.execQuery("SELECT * from doorsartifactmappings").next()) {
	
		    			pgh.execUpdate("delete from doorsartifactmappings");
	
	 		    }
	 		    
		}
		catch(SQLException e) {
				
			e.printStackTrace();	
			
			return postStatus;
			
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

		    							} catch (SQLException e) {
				    			
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
			
        
    	 staticLoading = false;
    	
    	 message.put("status", "Doors Artifact Types configured successfully");
		 
	     postStatus.put("res", NodeUtil.jsonToString(message));
	     
	     backupArtifactMappings(); //cache db artifact mappings into static data structure
	     
	     return postStatus;
    	
    	
    }

  
    private static void processConfigurationFile() {
    	
    	    	
    	BufferedReader configurationReader = null;
    	    	
    	String configurationInput = "";
    	
    	
    	try {
    		
    		
    		 configurationReader = new BufferedReader( new FileReader("doorsArtifactMappings.txt"));
    		 
    		
    		 while ((configurationInput = configurationReader.readLine()) != null ) {
				
    			 storeArtifactMappings(configurationInput,true);
				 
				     break;
				
				
			}
			
			configurationReader.close();
			
			
		}
    	catch(FileNotFoundException e) {
    		
			if (logger.isDebugEnabled()) {
        	
				logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Could not find doors artifact configuration file \n");
				e.printStackTrace();
            
			}
		
    	}
    	catch(Exception e) {
    		
    			if (logger.isDebugEnabled()) {
	        	
    				logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Problem reading configuration file\n");
    				e.printStackTrace();
                
    			}
    		
    	}
    	
    	
    }
    
   
    //Called from Spring at MMS startup
    public static void loadDefaultMappings() {
    	
    	
    	processConfigurationFile();
    	
    
    	
    }
    
    
    
    private static void backupArtifactMappings() {
     			 		
 		
 		
 		try {
 		
 			
 			ResultSet rs = pgh.execQuery("SELECT * FROM doorsartifactmappings");

 			while (rs.next()) {
 				
 				String project = rs.getString(1);
 				
 				String artifacttype = rs.getString(2);

 				String appliedMetatype = rs.getString(3);
 				
 				if(!artifactConfiguration.keySet().contains(project)) {
 					
 					artifactConfiguration.put(project, new HashMap<String,ArrayList<String>>());
 					
 					artifactConfiguration.get(project).put(artifacttype, new ArrayList<String>());
 					
 					artifactConfiguration.get(project).get(artifacttype).add(appliedMetatype);

 					
 					
 				}
 				
 				//unique project has already been added , now we want to add the current applied metatype to the project
 				else  {
 					
 	 				if(!artifactConfiguration.get(project).keySet().contains(artifacttype)) {
 	 					
 	 					artifactConfiguration.get(project).put(artifacttype,new ArrayList<String>());
 	 					
 	 					artifactConfiguration.get(project).get(artifacttype).add(appliedMetatype);
 	 					
 	 					
 	 				}
 	 				
 	 				else {
 	 					
 	 					artifactConfiguration.get(project).get(artifacttype).add(appliedMetatype);

 	 					
 	 				}

 					
 	 					
 				}
 				
 				
 				
 				
 				
 			}
 			
 			

 		} 
 		catch (SQLException e) {
 			
 			e.printStackTrace();
 			
 			if (logger.isDebugEnabled()) {
	        	
                logger.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "Could not retrieve artifact mappings from the database\n");
                e.printStackTrace();
                
          }
 			
 		}
 		catch (Exception e) {
 			
 			e.printStackTrace();
 			
 		}

		
 	 		
 		
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
