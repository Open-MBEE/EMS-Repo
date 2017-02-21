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
public class DoorsArtifactLinkMappingPost extends AbstractJavaWebScript {

    static PostgresHelper pgh = null;

    public DoorsArtifactLinkMappingPost() {

        super();

    }

    public DoorsArtifactLinkMappingPost(Repository repositoryHelper, ServiceRegistry registry) {

        super(repositoryHelper, registry);

    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        DoorsArtifactLinkMappingPost instance = new DoorsArtifactLinkMappingPost(repository, getServices());

        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {

        Map<String, Object> postStatus = new HashMap<String, Object>();

        JSONObject message = new JSONObject();
        String postedConfiguration = "";

        try {

            postedConfiguration = req.getContent().getContent().toString();

            postStatus = storeArtifactLinkMappings(postedConfiguration, false);

        }

        catch (JSONException e) {

            e.printStackTrace();
            message.put("result", "Invalid JSON format");
            postStatus.put("res", NodeUtil.jsonToString(message));
            return postStatus;

        }

        catch (IOException e) {

            e.printStackTrace();
            message.put("result", "Problem reading configuration");
            postStatus.put("res", NodeUtil.jsonToString(message));

        }

        return postStatus;

    }

    private static Map<String, Object> storeArtifactLinkMappings(String configuration, boolean staticLoading) {

        Map<String, Object> postStatus = new HashMap<String, Object>();
        JSONObject message = new JSONObject();
        String artifactMappingLinkStatus = "";

        try {

            ArrayList<String> currentConfiguration = new ArrayList<String>();
            JSONObject curProj = new JSONObject();
            String project = "";
            String sysmlappliedmetatypeid = "";
            String uri = "";
            JSONArray artifactLinkMappings = new JSONArray();
            JSONObject curArtifactLinkMapping = new JSONObject();
            boolean duplicate = false;
            boolean update = false;

            if (staticLoading) {

                pgh = new PostgresHelper("null");;

                try {

                    pgh.connect();

                    pgh.execUpdate("CREATE TABLE doorsartifactlinkmappings (project text not null, sysmlappliedmetatypeid text not null, uri text not null);");

                } catch (SQLException e) {
                    e.printStackTrace(); // table may already exists, no prob
                }

                return postStatus; // no longer loading default mappings b/c they will fail pre-syc
                                   // validation checks

            }

            JSONArray configurations = new JSONArray(configuration);

            pgh = new PostgresHelper("null");;

            try {
                pgh.connect();
            } catch (SQLException e) {
                e.printStackTrace();
                message.put("status", "Could not connect to database");
                postStatus.put("res", NodeUtil.jsonToString(message));
                return postStatus;
            }

            for (int p = 0; p < configurations.length(); p++) {

                curProj = (JSONObject) configurations.get(p);

                project = curProj.getString("project");

                artifactLinkMappings = curProj.getJSONArray("artifactLinkMappings");



                for (int a = 0; a < artifactLinkMappings.length(); a++) {

                    curArtifactLinkMapping = (JSONObject) artifactLinkMappings.get(a);

                    sysmlappliedmetatypeid = curArtifactLinkMapping.getString("sysmlappliedmetatypeid");

                    uri = curArtifactLinkMapping.getString("URI");

                    currentConfiguration = new ArrayList<String>();
                    currentConfiguration.add(project);
                    currentConfiguration.add(sysmlappliedmetatypeid);
                    currentConfiguration.add(uri);


                    try {

                        ResultSet doorsartifactlinkmappings = pgh.execQuery("SELECT * from doorsartifactlinkmappings");


                        while (doorsartifactlinkmappings.next()) {

                            if (doorsartifactlinkmappings.getString(1).equals(project)
                                            && doorsartifactlinkmappings.getString(2).equals(sysmlappliedmetatypeid)
                                            && doorsartifactlinkmappings.getString(3).equals(uri)) {
                                duplicate = true;
                                break;
                            }

                            // if not duplicate, check possible update conditions
                            // update linkURI
                            if (currentConfiguration.contains(doorsartifactlinkmappings.getString(1))
                                            && currentConfiguration.contains(doorsartifactlinkmappings.getString(2))) {

                                pgh.execUpdate("UPDATE doorsartifactlinkmappings SET uri = '" + uri
                                                + "' WHERE project = '" + project + "' AND sysmlappliedmetatypeid = '"
                                                + sysmlappliedmetatypeid + "';");
                                update = true;
                                break;
                            }
                            // update sysmlappliedmetatypeid
                            else if (currentConfiguration.contains(doorsartifactlinkmappings.getString(1))
                                            && currentConfiguration.contains(doorsartifactlinkmappings.getString(3))) {

                                pgh.execUpdate("UPDATE doorsartifactlinkmappings SET sysmlappliedmetatypeid = '"
                                                + sysmlappliedmetatypeid + "' WHERE project = '" + project
                                                + "' AND uri = '" + uri + "';");
                                update = true;
                                break;
                            } // update project
                            /*else if (currentConfiguration.contains(doorsartifactlinkmappings.getString(2))
                                            && currentConfiguration.contains(doorsartifactlinkmappings.getString(3))) {

                                pgh.execUpdate("UPDATE doorsartifactlinkmappings SET project = '" + project
                                                + "' WHERE sysmlappliedmetatypeid = '" + sysmlappliedmetatypeid
                                                + "' AND uri = '" + uri + "';");
                                update = true;
                                break;
                            }*/

                        }

                        if (!duplicate && !update) {
                            pgh.execUpdate("insert into doorsartifactlinkmappings (project,sysmlappliedmetatypeid,uri) VALUES ('"
                                            + project + "','" + sysmlappliedmetatypeid + "','" + uri + "')");
                            artifactMappingLinkStatus = "Doors Artifact Links mapped successfully";
                        } else if (update) {
                            artifactMappingLinkStatus = "Doors Artifact Links mapping updated successfully";
                        } else if (duplicate) {
                            artifactMappingLinkStatus = "Doors Artifact Links mapping already exists";
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

        message.put("status", artifactMappingLinkStatus);
        postStatus.put("res", NodeUtil.jsonToString(message));

        return postStatus;

    }

    public static boolean bootstrapArtifactLinkMappings() {

        try {
            storeArtifactLinkMappings("", true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;

    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return true;
    }

}
