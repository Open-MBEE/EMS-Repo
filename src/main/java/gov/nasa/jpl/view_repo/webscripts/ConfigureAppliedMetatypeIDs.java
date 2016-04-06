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
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
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
public class ConfigureAppliedMetatypeIDs extends AbstractJavaWebScript {
	

    static PostgresHelper pgh = null;
    
    static HashMap<String,ArrayList<String>> configuredAppliedMetatypeIDs = new HashMap<String,ArrayList<String>>();

    
    public ConfigureAppliedMetatypeIDs() {
    	
        super();
        
    }

    public ConfigureAppliedMetatypeIDs(Repository repositoryHelper, ServiceRegistry registry) {
    	
        super(repositoryHelper, registry);
        
    }

   
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    	ConfigureAppliedMetatypeIDs instance = new ConfigureAppliedMetatypeIDs(repository, getServices());

        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
    	
    	

    	Map<String, Object> model = new HashMap<String, Object>();
    	
    	
    	JSONObject loadStatus = new JSONObject();
    	
  					   
        if( loadAppliedMetatypeIDs() ) {
        	
        	  
        	loadStatus.put("status", "Applied Metatype IDs configured successfully");
	        
	        model.put("res", NodeUtil.jsonToString(loadStatus));
	       
	        return model;
	        
        	
        }
        
        else {
        	
        	loadStatus.put("status", "Problem loading applied metatype ids into database");
	        
	        model.put("res", NodeUtil.jsonToString(loadStatus));
	       
	        return model;
        	
        	
        }
        
        
    
        
    }
    

    //TODO adding error handling (i.e. bad json format ); debug output

    private static void ingestProjectAppliedMetatypes() {
    	
    	
    	configuredAppliedMetatypeIDs = new HashMap<String,ArrayList<String>>();
    	
    	BufferedReader configurationReader = null;
    	    	
    	String configurationInput = "";
    	
    	
    	try {
    		
    		
    		 configurationReader = new BufferedReader( new FileReader("doorsAppliedMetatypeIDs.txt"));
   
    		 JSONObject curProject = null;
    		 
    		 JSONObject curAppliedMetatypeID = null;

    		 ArrayList<String> curAppliedMetatypeIDs = null;
    		 
    		
    		 while ((configurationInput = configurationReader.readLine()) != null ) {
				
					
				    JSONArray appliedMetatypeIDConfigurationJSON = new JSONArray(configurationInput);
				    
				    //iterate through json configuration of project and applied metatype ids, then store those relationships in static global data structure
				    for(int i=0; i < appliedMetatypeIDConfigurationJSON.length(); i++) {
				    	
				    		curProject = (JSONObject) appliedMetatypeIDConfigurationJSON.get(i);
				    		
			    			curAppliedMetatypeIDs = new ArrayList<String>();

				    		for(int j=0; j < curProject.getJSONArray("appliedMetatypeIDs").length() ; j++) {
				    	
				    			curAppliedMetatypeID = (JSONObject)curProject.getJSONArray("appliedMetatypeIDs").get(j);
				    			
				    			curAppliedMetatypeIDs.add(curAppliedMetatypeID.getString("appliedMetatypeID"));
				    	
				    	
				    		}
				    	
			    			configuredAppliedMetatypeIDs.put(curProject.getString("project"), curAppliedMetatypeIDs );

				    }

				    
				    break;
				    
					
				
			}
			
			configurationReader.close();
			
			
		}
    	
    	catch (FileNotFoundException e) {
    		
			e.printStackTrace();
			
		}catch (IOException e) {
			
			e.printStackTrace();
			
		}
    	catch (Exception e) {
			
			e.printStackTrace();
			
		}
    	
    	        
    	
    }
    
   
    //TODO add error conditions and logging for when problems occur during startup
    // Will be bootstrapped during MMS start up
    public static boolean loadAppliedMetatypeIDs() {
    	
    	//read in json configuration of projects and applied metatype ids
    	ingestProjectAppliedMetatypes();
    	
    
	     try {
	    	 
	    	pgh = new PostgresHelper("master");
	
	 		pgh.connect();
	 		 
			pgh.execUpdate("create table doorsAppliedMetatypeIDs (project text not null, appliedmetatypeid text not null)");
			
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			
			if(!e.getMessage().contains("already exists")) {
			
				return false;
			
			}
			
		}
	     
	    catch (ClassNotFoundException e) {
				
			e.printStackTrace();
			
			return false;
				
	    }

    	
    	//Traverse static global data structure containing projects and applied metatype ids, then store mappings into database
    	Set<Map.Entry<String,ArrayList<String>>> configurations = configuredAppliedMetatypeIDs.entrySet();
    	
    	ArrayList<String> curAppliedMetatypes = null;
    	
    	for(Map.Entry<String,ArrayList<String>> curRelationship : configurations) {
    		
    		curAppliedMetatypes = curRelationship.getValue();
    		
    		
    			for(int i=0; i < curAppliedMetatypes.size(); i++ ){
    			
    				
    				try {
    		  			
    		 			pgh.execUpdate("insert into doorsAppliedMetatypeIDs (project, appliedmetatypeid) VALUES ('" + curRelationship.getKey() + "','" + curAppliedMetatypes.get(i) + "')");

    		 		} 
    		 		catch (SQLException e) {
    		 			
    		 			e.printStackTrace();
    		 			
    		 		}
    		 		
    		   	
    		   }
    		
    
    			
    		
    	}
    	
    	
    	return true;
    	
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
