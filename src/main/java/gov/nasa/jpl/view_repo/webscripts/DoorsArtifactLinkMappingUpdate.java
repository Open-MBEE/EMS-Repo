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
public class DoorsArtifactLinkMappingUpdate extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;

    HashMap<String, HashMap<String, ArrayList<String>>> linkTypeConfiguration =
                    new HashMap<String, HashMap<String, ArrayList<String>>>();

    public DoorsArtifactLinkMappingUpdate() {

        super();

    }

    public DoorsArtifactLinkMappingUpdate(Repository repositoryHelper, ServiceRegistry registry) {

        super(repositoryHelper, registry);

    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        DoorsArtifactLinkMappingUpdate instance = new DoorsArtifactLinkMappingUpdate(repository, getServices());

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
            updateStatus = updateArtifactLinkMappings(updateConfiguration);
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

    private static Map<String, Object> updateArtifactLinkMappings(String configuration) {

        Map<String, Object> postStatus = new HashMap<String, Object>();
        JSONObject message = new JSONObject();

        JSONArray configurations = new JSONArray(configuration);
        JSONObject curSetting = new JSONObject();
        String curProj = "";
        String curMetatypeId = "";
        String curURI = "";
        String updateStatus = "";

        String oldProj = "";
        String oldMetatypeId = "";
        String oldURI = "";

        String newProject = "";
        String newMetatypeId = "";
        String newURI = "";

        boolean found = false;

        try {

            for (int c = 0; c < 2; c++) {

                curSetting = (JSONObject) configurations.get(c);

                curProj = curSetting.getString("project");

                curMetatypeId = curSetting.getString("sysmlappliedmetatypeid");

                curURI = curSetting.getString("URI");

                ResultSet result = pgh.execQuery("SELECT * from doorsartifactlinkmappings WHERE project = '" + curProj
                                + "' AND sysmlappliedmetatypeid = '" + curMetatypeId + "' AND uri = '" + curURI + "';");


                if (result.next() && c == 0) {

                    found = true;

                    oldProj = curProj;
                    oldMetatypeId = curMetatypeId;
                    oldURI = curURI;

                    pgh.execUpdate("DELETE from doorsartifactlinkmappings WHERE project = '" + curProj
                                    + "' AND sysmlappliedmetatypeid = '" + curMetatypeId + "' AND uri = '" + curURI
                                    + "';");

                    System.out.println("jnini");

                } else {

                    newProject = curProj;
                    newMetatypeId = curMetatypeId;
                    newURI = curURI;

                }

            }

            if (found) {
                pgh.execUpdate("insert into doorsartifactlinkmappings (project,sysmlappliedmetatypeid, uri) VALUES ('"
                                + newProject + "','" + newMetatypeId + "','" + newURI + "')");
                updateStatus = "Artifact Link Mappings updated successfully";
            } else {
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
        return true;
    }

}
