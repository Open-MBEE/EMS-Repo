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

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor at /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/javawebscripts/project.post.desc.xml
 * @author cinyoung
 *
 */
public class ProjectPost extends AbstractJavaWebScript {
	public ProjectPost() {
	    super();
	}

    public ProjectPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    private final String MODEL_PATH = "Models";
	private final String MODEL_PATH_SEARCH = "/" + MODEL_PATH;


	/**
	 * Webscript entry point
	 */
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

		clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();
		int statusCode = HttpServletResponse.SC_OK;

		try {
			if (validateRequest(req, status)) {
			    String siteName = getSiteName( req );
		        String projectId = getProjectId( req );
		        boolean delete = getBooleanArg( req, "delete", false );
		        boolean fix = getBooleanArg(req, "fix", false);
		        boolean createSite = getBooleanArg(req, "createSite", false);

                WorkspaceNode workspace = getWorkspace( req );
                if ( siteName != null && !siteName.equals( NO_SITE_ID ) ) {
                    statusCode = updateOrCreateProject( (JSONObject)req.parseContent(),
                                                        workspace, projectId,
                                                        siteName, createSite, fix,
                                                        delete );
			    } else {
                    statusCode = updateOrCreateProject( (JSONObject)req.parseContent(),
                                                        workspace, projectId, fix );
			    }
			} else {
				statusCode = responseStatus.getCode();
			}
        } catch (JSONException e) {
            log(LogLevel.ERROR, "JSON could not be created\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } catch (Exception e) {
            log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

		status.setCode(statusCode);
		model.put("res", response.toString());

        printFooter();

		return model;
	}

	public int updateOrCreateProject(JSONObject jsonObject, WorkspaceNode workspace, String projectId, boolean fix) throws JSONException {
	      EmsScriptNode projectNode = findScriptNodeById(projectId, workspace, null, true);

	      if (projectNode == null) {
	          log(LogLevel.ERROR, "Could not find project\n", HttpServletResponse.SC_NOT_FOUND);
	          return HttpServletResponse.SC_NOT_FOUND;
	      }

	      String projectName = null;
        if (jsonObject.has(Acm.JSON_NAME)) {
            projectName = jsonObject.getString(Acm.JSON_NAME);
        }
        String projectVersion = null;
        if (jsonObject.has(Acm.JSON_PROJECT_VERSION)) {
            projectVersion = jsonObject.getString(Acm.JSON_PROJECT_VERSION);
        }
        if (fix) {
            if (checkPermissions(projectNode, PermissionService.WRITE)){
                projectNode.createOrUpdateProperty(Acm.ACM_ID, projectId);
                if (projectName != null) {
                    projectNode.createOrUpdateProperty(Acm.CM_TITLE, projectName);
                    projectNode.createOrUpdateProperty(Acm.ACM_NAME, projectName);
                }
                if (projectVersion != null) {
                    projectNode.createOrUpdateProperty(Acm.ACM_PROJECT_VERSION, projectVersion);
                }
                log(LogLevel.INFO, "Project metadata updated.\n", HttpServletResponse.SC_OK);
            }
        }

        return HttpServletResponse.SC_OK;
    }

    /**
	 * Update or create the project specified by the JSONObject
	 * @param jsonObject	JSONObject that has the name of the project
	 * @param projectId		Project ID
	 * @param siteName		Site project should reside in
	 * @return				HttpStatusResponse code for success of the POST request
	 * @throws JSONException
	 */
    public int updateOrCreateProject(JSONObject jsonObject, WorkspaceNode workspace,
                                     String projectId, String siteName,
                                     boolean createSite, boolean fix,
                                     boolean delete) throws JSONException {
		// make sure site exists
		EmsScriptNode siteNode = getSiteNode(siteName, workspace, null);
		if (siteNode == null) {
		    if (createSite) {
		        if ( siteName == null || siteName.length() == 0 ) {
	                siteName="europa";
		        }
		        siteNode = createSite( siteName, workspace );
		    } else {
		        log(LogLevel.ERROR, "Site not found for " + siteName + ".\n", HttpServletResponse.SC_NOT_FOUND);
		        return HttpServletResponse.SC_NOT_FOUND;
		    }
		}

		// make sure Model package under site exists
        EmsScriptNode modelContainerNode =
                siteNode.childByNamePath( MODEL_PATH_SEARCH, false, workspace );
		if (modelContainerNode == null) {
		    // always create
		    fix = true;
			if (fix) {
				modelContainerNode = siteNode.createFolder("Models");
				log(LogLevel.INFO, "Model folder created.\n", HttpServletResponse.SC_OK);
			} else {
				log(LogLevel.ERROR, "Model folder not found. Use fix=true to force Model folder creation.\n", HttpServletResponse.SC_NOT_FOUND);
				return HttpServletResponse.SC_NOT_FOUND;
			}
		}

		// create project if doesn't exist or update if fix is specified
		// FIXME: this search is a issue if we have multiple sites in a workspace with the same projectId
		//		  even worse, if the master workspace has the projectId and the current worskpace does not,
		//		  it will return the master workspace projectNode.  This is especially bad for "no_project".
		EmsScriptNode projectNode = findScriptNodeById(projectId, workspace, null, true);
		String projectName = null;
		if (jsonObject.has(Acm.JSON_NAME)) {
		    projectName = jsonObject.getString(Acm.JSON_NAME);
		}
		String projectVersion = null;
		if (jsonObject.has(Acm.JSON_PROJECT_VERSION)) {
		    projectVersion = jsonObject.getString(Acm.JSON_PROJECT_VERSION);
		}
		if (projectNode == null) {
			projectNode = modelContainerNode.createFolder(projectId, Acm.ACM_PROJECT);
			projectNode.setProperty(Acm.CM_TITLE, projectName);
			projectNode.setProperty(Acm.ACM_ID, projectId);
            if (projectName != null) {
                projectNode.setProperty(Acm.ACM_NAME, projectName);
            }
			if (projectVersion != null) {
			    projectNode.setProperty(Acm.ACM_PROJECT_VERSION, projectVersion);
			}
			projectNode.setWorkspace(workspace, null);
			log(LogLevel.INFO, "Project created.\n", HttpServletResponse.SC_OK);
		} else {
			if (delete) {
				projectNode.remove();
				log(LogLevel.INFO, "Project deleted.\n", HttpServletResponse.SC_OK);
			} else if (fix) {
				if (checkPermissions(projectNode, PermissionService.WRITE)){
					projectNode.createOrUpdateProperty(Acm.ACM_ID, projectId);
					if (projectName != null) {
                        projectNode.createOrUpdateProperty(Acm.CM_TITLE, projectName);
                        projectNode.createOrUpdateProperty(Acm.ACM_NAME, projectName);
					}
		            if (projectVersion != null) {
		                projectNode.createOrUpdateProperty(Acm.ACM_PROJECT_VERSION, projectVersion);
		            }
					log(LogLevel.INFO, "Project metadata updated.\n", HttpServletResponse.SC_OK);

					if (checkPermissions(projectNode.getParent(), PermissionService.WRITE)) {
						// move sites if exists under different site
						if (!projectNode.getParent().equals(modelContainerNode)) {
							projectNode.move(modelContainerNode);
							log(LogLevel.INFO, "Project moved to new site.\n", HttpServletResponse.SC_OK);
						}
					}
				}
			} else {
				log(LogLevel.WARNING, "Project already exists.\n", HttpServletResponse.SC_FOUND);
				return HttpServletResponse.SC_FOUND;
			}
		}
		return HttpServletResponse.SC_OK;
	}

	/**
	 * Validate the request and check some permissions
	 */
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		if (!checkRequestContent(req)) {
			return false;
		}

		// check site exists
//		if (!checkRequestVariable(siteName, SITE_NAME)) {
//			return false;
//		}

		// get the site
//		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
//		if (!checkRequestVariable(siteInfo, "Site")) {
//			return false;
//		}

		// check permissions
//		if (!checkPermissions(siteInfo.getNodeRef(), PermissionService.WRITE)) {
//			return false;
//		}

		String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
		if (!checkRequestVariable(projectId, PROJECT_ID)) {
			return false;
		}

		return true;
	}

}
