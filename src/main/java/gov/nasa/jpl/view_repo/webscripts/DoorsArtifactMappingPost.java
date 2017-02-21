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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
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
        pgh = new PostgresHelper("null");

        try {

            pgh.connect();

            postedConfiguration = req.getContent().getContent().toString();

            postStatus = storeArtifactMappings(postedConfiguration, false);

        } catch (SQLException e) {

            e.printStackTrace();
            message.put("status", "Could not connect to database");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;

        } catch (IOException e) {

            e.printStackTrace();
            message.put("status", "Could not read from I/O");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;

        } catch (ClassNotFoundException e) {

            e.printStackTrace();
            message.put("status", "Class not found");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;

        } finally {
            pgh.close();
        }

        return postStatus;

    }

    private static Map<String, Object> storeArtifactMappings(String configuration, boolean staticLoading) {

        Map<String, Object> postStatus = new HashMap<String, Object>();
        JSONObject message = new JSONObject();
        String artifactMappingStatus = "";

        try {

            JSONArray configurations = new JSONArray(configuration);
            ArrayList<String> currentConfiguration = new ArrayList<String>();
            JSONObject curProj = new JSONObject();
            String project = "";
            String doorsArtifactType = "";
            JSONArray artifactMappings = new JSONArray();
            JSONObject curArtifactMapping = new JSONObject();
            JSONArray appliedMetatypeArray = new JSONArray();
            JSONObject curAppliedMetatype = new JSONObject();
            boolean duplicate = false;
            boolean update = false;

            if (staticLoading) {

                pgh = new PostgresHelper("null");;

                try {

                    pgh.connect();

                    pgh.execUpdate("CREATE TABLE doorsartifactmappings (project text not null, doorsartifacttype text not null, sysmlappliedmetatype text not null);");

                } catch (SQLException e) {

                    e.printStackTrace(); // table may already exists, no prob

                }
                
                return postStatus; //not going to load default configuration anymore b/c they will fail pre-sync validation checks

            }

            for (int p = 0; p < configurations.length(); p++) {

                curProj = (JSONObject) configurations.get(p);

                project = curProj.getString("project");

                artifactMappings = curProj.getJSONArray("artifactMappings");



                for (int a = 0; a < artifactMappings.length(); a++) {

                    curArtifactMapping = (JSONObject) artifactMappings.get(a);

                    doorsArtifactType = curArtifactMapping.getString("doorsArtifactType");

                    appliedMetatypeArray = curArtifactMapping.getJSONArray("appliedMetatypeIDs");
                    

                    for (int a2 = 0; a2 < appliedMetatypeArray.length(); a2++) {

                        curAppliedMetatype = (JSONObject) appliedMetatypeArray.get(a2);
                        

                        currentConfiguration = new ArrayList<String>();
                        currentConfiguration.add(project);
                        currentConfiguration.add(doorsArtifactType);
                        currentConfiguration.add(curAppliedMetatype.getString("id"));


                        try {

                            ResultSet doorsartifactmappings = pgh.execQuery("SELECT * from doorsartifactmappings");

                            while (doorsartifactmappings.next()) {

                                if (doorsartifactmappings.getString(1).equals(project)
                                                && doorsartifactmappings.getString(2).equals(doorsArtifactType)
                                                && doorsartifactmappings.getString(3)
                                                                .equals(curAppliedMetatype.getString("id"))) {
                                    duplicate = true;
                                    break;
                                }

                                
                                // if not duplicate, check possible update conditions
                                
                                // update applied metatype ID
                                if (currentConfiguration.contains(doorsartifactmappings.getString(1))
                                                && currentConfiguration.contains(doorsartifactmappings.getString(2))) {

                                    pgh.execUpdate("UPDATE doorsartifactmappings SET sysmlappliedmetatype = '"
                                                    + curAppliedMetatype.getString("id") + "' WHERE project = '"
                                                    + project + "' AND doorsartifacttype = '" + doorsArtifactType
                                                    + "';");
                                    update = true;
                                    break;
                                }
                                // update artifact type
                                else if (currentConfiguration.contains(doorsartifactmappings.getString(1))
                                                && currentConfiguration.contains(doorsartifactmappings.getString(3))) {

                                    pgh.execUpdate("UPDATE doorsartifactmappings SET doorsartifacttype = '"
                                                    + doorsArtifactType + "' WHERE project = '" + project
                                                    + "' AND sysmlappliedmetatype = '"
                                                    + curAppliedMetatype.getString("id") + "';");
                                    update = true;
                                    break;
                                } // update project
                                /*else if (currentConfiguration.contains(doorsartifactmappings.getString(2))
                                                && currentConfiguration.contains(doorsartifactmappings.getString(3))) {

                                    pgh.execUpdate("UPDATE doorsartifactmappings SET project = '" + project
                                                    + "' WHERE doorsartifacttype = '" + doorsArtifactType
                                                    + "' AND sysmlappliedmetatype = '"
                                                    + curAppliedMetatype.getString("id") + "';");
                                    update = true;
                                    break;
                                }*/

                            }

                            if (!duplicate && !update) {
                                pgh.execUpdate("insert into doorsartifactmappings (project, doorsartifacttype, sysmlappliedmetatype) VALUES ('"
                                                + project + "','" + doorsArtifactType + "','"
                                                + curAppliedMetatype.getString("id") + "')");
                                artifactMappingStatus = "Doors Artifact Types mapped successfully";
                            } 
                            else if (update) {
                                artifactMappingStatus = "Doors Artifact Type mapping updated successfully";
                            }
                            else if (duplicate) {
                                artifactMappingStatus = "Doors Artifact Type mapping already exists";
                            }
                            duplicate = false;

                        } catch (SQLException e) {

                            e.printStackTrace();
                            message.put("status", "Problem inserting artifact mapping into database");
                            postStatus.put("res", NodeUtil.jsonToString(message));
                            return postStatus;

                        }

                    }

                }

            }

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

        } finally {
            pgh.close();
        }

        staticLoading = false;

        message.put("status", artifactMappingStatus);
        postStatus.put("res", NodeUtil.jsonToString(message));

        return postStatus;

    }

    public static boolean bootstrapArtifactMappings() {

        BufferedReader configurationReader = null;
        String configurationInput = "";

        try {
                storeArtifactMappings(configurationInput, true);
        }  catch (Exception e) {

            e.printStackTrace();

        }
        
        return true;

    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return true;
    }

}
