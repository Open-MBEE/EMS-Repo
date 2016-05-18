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


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
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
public class DoorsProjectMappingGet extends AbstractJavaWebScript {
	

    static PostgresHelper pgh = null;
        
    public DoorsProjectMappingGet() {
    	
        super();
        
    }

    public DoorsProjectMappingGet(Repository repositoryHelper, ServiceRegistry registry) {
    	
        super(repositoryHelper, registry);
        
    }

   
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    	DoorsProjectMappingGet instance = new DoorsProjectMappingGet(repository, getServices());

        return instance.executeImplImpl(req, status, cache);
        
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
    	
    	Map<String, Object> getResult = new HashMap<String, Object>();
    	JSONObject message = new JSONObject();
    	
    	JSONArray projectMappings = new JSONArray();
    	JSONObject curProjectMappings = new JSONObject();
    	
        pgh = new PostgresHelper("null");
        
        
		try {
			
			
			pgh.connect();
					
			ResultSet rs = pgh.execQuery("SELECT * FROM doorsprojectmappings");

	 		while (rs.next()) {
	 				
	 			curProjectMappings = new JSONObject();
	 			
	 			curProjectMappings.put( "sysmlprojectid",rs.getString(1));

	 			curProjectMappings.put( "doorsproject",rs.getString(2));
	 			
		 		projectMappings.put(curProjectMappings);

				
			}
	 		
		
		}
		catch(SQLException e) {
			 e.printStackTrace();
			 message.put("status", "Could not retrieve artifact type mappings from database");
			 getResult.put("res", NodeUtil.jsonToString(message));
		     return getResult;
		}
		catch(ClassNotFoundException e) {
			 e.printStackTrace();
			 message.put("status", "Class not found");
			 getResult.put("res", NodeUtil.jsonToString(message));
		     return getResult;
			 
		}
		catch(Exception e) {
			e.printStackTrace();
			return getResult;
			
		}finally {
            pgh.close();
        }
        
      
		message.put("projectMappings", projectMappings);
		getResult.put("res", NodeUtil.jsonToString(message));
	        
    	return getResult;
    	 
        
    }
    
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return true;
    }
   
}
