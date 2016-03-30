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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;


/**
 *
 * @author Bruce Meeks Jr
 *
 */
public class ConfigureAppliedMetatypes extends AbstractJavaWebScript {
	

    PostgresHelper pgh = null;
    

    public ConfigureAppliedMetatypes() {
    	
        super();
        
    }

    public ConfigureAppliedMetatypes(Repository repositoryHelper, ServiceRegistry registry) {
    	
        super(repositoryHelper, registry);
        
    }

   
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    	ConfigureAppliedMetatypes instance = new ConfigureAppliedMetatypes(repository, getServices());

        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {

    	Map<String, Object> model = new HashMap<String, Object>();
    	 
        JSONObject json = new JSONObject();

    	WorkspaceNode workspace = getWorkspace(req);
    	
        HashMap<String,ArrayList<String>> projectsAppliedMetatypes = new HashMap<String,ArrayList<String>>();

        projectsAppliedMetatypes = ingestProjectAppliedMetatypes();
    	
        if(projectsAppliedMetatypes.size() > 0) {
        	
		        
		        pgh = new PostgresHelper(workspace);
		        
		        try {
		        	
					pgh.connect();
					
					if( pgh.createProjectAppliedMetatypesTable() ) {
					
						storeProjectAppliedMetatypes(projectsAppliedMetatypes);
						
					}
					else {
				        json.put("status", "problem creating projectappliedmetatypes table");
				        model.put("res", NodeUtil.jsonToString(json));
				        return model;
					}
		
					
				} catch (ClassNotFoundException e) {
					
					e.printStackTrace();
					json.put("status", "Could not connect to postgres");
			        model.put("res", NodeUtil.jsonToString(json));
			        
			        return model;
					
				} catch (SQLException e) {
					
					e.printStackTrace();
					json.put("status", "Could not connect to postgres");
			        model.put("res", NodeUtil.jsonToString(json));
			        
			        return model;
					
				} 
		        
		        
		        json.put("status", "AppliedMetatypesIDs Configured Successfully");
		        
		        model.put("res", NodeUtil.jsonToString(json));
		       
		        return model;

        }
        else {
        	
        	
        	json.put("status", "No AppliedMetatypesIDs Configuration Found");
            model.put("res", NodeUtil.jsonToString(json));
           
            return model;
        	
        	
        	
        }
        
        
        
    }
    
    
    /***
     * Reading in project/applied metatype id configurations and storing in data structure
     * @return
     */
    private HashMap<String,ArrayList<String>> ingestProjectAppliedMetatypes() {
    	
    	
    	HashMap<String,ArrayList<String>> projectsAppliedMetatypes = new HashMap<String,ArrayList<String>>();
    	
    	BufferedReader configurationReader = null;
    	
    	String curMapping = "";
    	
    	
    	try {
    		
    		 configurationReader = new BufferedReader( new FileReader("applied_metatypes.properties"));
    		 
    		 StringTokenizer tokenizer = null;
    		 
    		 String curProj = "";
    		 
    		 ArrayList<String> curAppliedMetatypes = null;
    		 
    		 String curAppliedMetatype = null;
    		 
    		 int index = 0;
    		
			while ((curMapping = configurationReader.readLine()) != null ) {
				
					tokenizer = new StringTokenizer(curMapping);
					
					curAppliedMetatypes = new ArrayList<String>();
					
					while(tokenizer.hasMoreTokens() ) {
						
						if( index==0 ){
							
							curProj = tokenizer.nextToken();
							index++;
						
						}
						else {
							
							curAppliedMetatype = tokenizer.nextToken();
							
							//prevent duplicates applied metatype ids for same project
							if(!curAppliedMetatypes.contains(curAppliedMetatype)) {
								
								curAppliedMetatypes.add(curAppliedMetatype);
							
							}
							
						}
						
					}
					
					index = 0;
					
					//prevent duplicate project entries
					if(!projectsAppliedMetatypes.containsKey(curProj)) {
						
						projectsAppliedMetatypes.put(curProj,curAppliedMetatypes);
					
					}
					
				
			}
			
		} 
    	catch (FileNotFoundException e) {
    		
			e.printStackTrace();
			
		}catch (IOException e) {
			
			e.printStackTrace();
			
		}
    	catch (Exception e) {
			
			e.printStackTrace();
			
		}
    
    	
        return projectsAppliedMetatypes;
        
    	
    }
    
    
    /***
     * Storing project and applied metatypes id relationships in PG
     * @param projectAppliedMetatypes
     */
    private void storeProjectAppliedMetatypes(HashMap<String,ArrayList<String>> projectAppliedMetatypes) {
    	
    	
    	Set<Map.Entry<String,ArrayList<String>>> configurations = projectAppliedMetatypes.entrySet();
    	
    	ArrayList<String> curAppliedMetatypes = null;
    	
    	for(Map.Entry<String,ArrayList<String>> curRelationship : configurations) {
    		
    		curAppliedMetatypes = curRelationship.getValue();
    		
    			for(int i=0; i < curAppliedMetatypes.size(); i++ ){
    			
    				pgh.storeAppliedMetatypeId(curRelationship.getKey(),curAppliedMetatypes.get(i));
    		
    			
    			}
    			
    		
    	}
    	
        	
    	
    }
    
   

    /**
     * Validate the request and check some permissions
     */
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
