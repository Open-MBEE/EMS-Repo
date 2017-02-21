/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
public class DoorsArtifactMappingUpdate extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;
    static Logger logger = Logger.getLogger(DoorsArtifactMappingUpdate.class);

    public DoorsArtifactMappingUpdate() {

        super();

    }

    public DoorsArtifactMappingUpdate(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        DoorsArtifactMappingUpdate instance = new DoorsArtifactMappingUpdate(repository, getServices());
        return instance.executeImplImpl(req, status, cache);

    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {

        Map<String, Object> updateStatus = new HashMap<String, Object>();
        JSONObject message = new JSONObject();
        String updateConfiguration = "";
        pgh = new PostgresHelper(getWorkspace(req));

        try {
            pgh.connect();
            updateConfiguration = req.getContent().getContent().toString();
            updateStatus = updateArtifactMappings(updateConfiguration);
        } catch (SQLException e) {
            e.printStackTrace();
            message.put("status", "Could not connect to database");
            updateStatus.put("res", NodeUtil.jsonToString(message));
            return updateStatus;
        } catch (IOException e) {
            e.printStackTrace();
            message.put("status", "Could not read from I/O");
            updateStatus.put("res", NodeUtil.jsonToString(message));
            return updateStatus;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            message.put("status", "Class not found");
            updateStatus.put("res", NodeUtil.jsonToString(message));
            return updateStatus;
        } finally {
            pgh.close();
        }

        return updateStatus;

    }
    
    private static Map<String, Object> updateArtifactMappings(String configuration) {

        Map<String, Object> postStatus = new HashMap<String, Object>();
        JSONObject message = new JSONObject();

        JSONArray configurations = new JSONArray(configuration);
        JSONObject curSetting = new JSONObject();
        String project = "";
        String doorsArtifactType = "";
        String sysmlappliedmetatype = "";
        String updateStatus = "";
        
        String oldProject = "";
        String oldDoorsArtifactType = "";
        String oldSysmlappliedmetatype = "";
        
        String newProject = "";
        String newDoorsArtifactType = "";
        String newSysmlappliedmetatype = "";
        
        boolean found = false;

        try {

            for (int c = 0; c < 2; c++) {

                curSetting = (JSONObject) configurations.get(c);

                project = curSetting.getString("project");
                
                doorsArtifactType = curSetting.getString("doorsartifacttype");
                
                sysmlappliedmetatype = curSetting.getString("sysmlappliedmetatype");
                
                ResultSet result = pgh.execQuery("SELECT * from doorsartifactmappings WHERE project = '" + project
                                + "' AND doorsartifacttype = '" + doorsArtifactType
                                + "' AND sysmlappliedmetatype = '"
                                + sysmlappliedmetatype + "';");
                

                if(result.next() && c==0){
                    
                    found = true; 
                    
                    oldProject = project;
                    oldDoorsArtifactType = doorsArtifactType;
                    oldSysmlappliedmetatype = sysmlappliedmetatype;
                    
                    pgh.execUpdate("DELETE from doorsartifactmappings WHERE project = '" + project
                                    + "' AND doorsartifacttype = '" + doorsArtifactType
                                    + "' AND sysmlappliedmetatype = '"
                                    + sysmlappliedmetatype + "';");
                    
                    System.out.println("jnini");
                    
                }
                else {
                    
                    newProject = project;
                    newDoorsArtifactType = doorsArtifactType;
                    newSysmlappliedmetatype = sysmlappliedmetatype;
                    
                }

            }
            
            if(found) {
            pgh.execUpdate("insert into doorsartifactmappings (project, doorsartifacttype, sysmlappliedmetatype) VALUES ('"
                            + newProject + "','" + newDoorsArtifactType + "','"
                            + newSysmlappliedmetatype + "')");
            updateStatus = "Update successful";
            }
            else{
                message.put("status", "Mapping doesn't exist");
                postStatus.put("res", NodeUtil.jsonToString(message));
                return postStatus;
            }
            
            System.out.println();

        } catch (JSONException e) {

            e.printStackTrace();
            message.put("status", "Invalid JSON input configuration");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;

        } catch (Exception e) {

            e.printStackTrace();
            message.put("status", "Internal error occurred");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;

        }

        message.put("status", updateStatus);
        postStatus.put("res", NodeUtil.jsonToString(message));
        return postStatus;

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
