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
import java.util.Set;
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
public class DoorsArtifactLinkMappingDelete extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;

    HashMap<String, HashMap<String, ArrayList<String>>> linkTypeConfiguration =
                    new HashMap<String, HashMap<String, ArrayList<String>>>();

    public DoorsArtifactLinkMappingDelete() {

        super();

    }

    public DoorsArtifactLinkMappingDelete(Repository repositoryHelper, ServiceRegistry registry) {

        super(repositoryHelper, registry);

    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        DoorsArtifactLinkMappingDelete instance = new DoorsArtifactLinkMappingDelete(repository, getServices());

        return instance.executeImplImpl(req, status, cache);

    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {

        Map<String, Object> deleteStatus = new HashMap<String, Object>();

        JSONObject message = new JSONObject();

        try {

            JSONArray postedConfiguration = new JSONArray(req.getContent().getContent());

            pgh = new PostgresHelper("null");;

            try {

                pgh.connect();
                
                deleteStatus = deleteArtifactLinkMappings(postedConfiguration.toString());


            } catch (SQLException e) {
                e.printStackTrace();
                message.put("status", "Could not connect to database");
                deleteStatus.put("res", NodeUtil.jsonToString(message));
                return deleteStatus;
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            finally{
                pgh.close();
            }
            

        }

        catch (JSONException e) {
            e.printStackTrace();
            message.put("result", "Invalid JSON format");
            deleteStatus.put("res", NodeUtil.jsonToString(message));
            return deleteStatus;
        }

        catch (IOException e) {
            e.printStackTrace();
            message.put("result", "Problem reading configuration");
            deleteStatus.put("res", NodeUtil.jsonToString(message));
        }

        return deleteStatus;

    }

    private static Map<String, Object> deleteArtifactLinkMappings(String configuration) {

        Map<String, Object> postStatus = new HashMap<String, Object>();
        JSONObject message = new JSONObject();

        try {

            JSONArray artifactMappingConfiguration = new JSONArray(configuration);
            JSONObject curMapping = new JSONObject();

            for (int p = 0; p < artifactMappingConfiguration.length(); p++) {

                curMapping = (JSONObject) artifactMappingConfiguration.get(p);

                ResultSet rs = pgh.execQuery("SELECT * FROM doorsartifactlinkmappings WHERE project = '"
                                + curMapping.getString("project") + "' AND sysmlappliedmetatypeid = '"
                                + curMapping.getString("sysmlappliedmetatypeid") + "';");

                if (rs.next()) {
                    pgh.execUpdate("DELETE from doorsartifactlinkmappings WHERE project = '"
                                    + curMapping.getString("project") + "' AND sysmlappliedmetatypeid = '"
                                    + curMapping.getString("sysmlappliedmetatypeid") + "';");
                } else {
                    message.put("status",
                                    "This mapping doesn't exist; " + curMapping.getString("project") + " and " +  curMapping.getString("sysmlappliedmetatypeid") + "Please POST new link relationship mappings to /doors/artifact/link/mappings/");
                    postStatus.put("res", NodeUtil.jsonToString(message));
                }
            }

            pgh.close();

        } catch (JSONException e) {
            e.printStackTrace();
            message.put("status",
                            "Invalid JSON input configuration. Please use the following format: [{project:,sysmlappliedmetatypeid:,uri:}]");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;
        } catch (Exception e) {
            e.printStackTrace();
            message.put("status", "Internal error occurred");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;
        }

        message.put("status", "Doors Artifact Type mapping deleted successfully");
        postStatus.put("res", NodeUtil.jsonToString(message));
        return postStatus;

    }



    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return true;
    }

}
