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
package gov.nasa.jpl.view_repo;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;
import gov.nasa.jpl.view_repo.webscripts.ModelPost;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

/**
 * Action for loading the project model in the background asynchronously
 * @author cinyoung
 */
public class ModelLoadActionExecuter extends ActionExecuterAbstractBase {
    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;
    private String contextUrl;

    private StringBuffer response;

    // Parameter values to be passed in when the action is created
    public static final String NAME = "modelLoad";
    public static final String PARAM_PROJECT_NAME = "projectName";
    public static final String PARAM_PROJECT_ID = "projectId";
    public static final String PARAM_PROJECT_NODE = "projectNode";

    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }
    
    public void setContextUrl(String url) {
        contextUrl = url;
    }

    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        String projectId = (String) action.getParameterValue(PARAM_PROJECT_ID);
        String projectName = (String) action.getParameterValue(PARAM_PROJECT_NAME);
        EmsScriptNode projectNode = (EmsScriptNode) action.getParameterValue(PARAM_PROJECT_NODE);
        System.out.println("ModelLoadActionExecuter started execution of " + projectName + " [id: " + projectId + "]");
        clearCache();

        // Parse the stored file for loading
        EmsScriptNode jsonNode = new EmsScriptNode(nodeRef, services, response);
        ContentReader reader = services.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
        JSONObject content = null;
        try {
            content = new JSONObject(reader.getContentString());
        } catch (ContentIOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Update the model
        String jobStatus = "Failed";
        if (content == null) {
            response.append("ERRROR: Could not load JSON file for job\n");
        } else {
            ModelPost modelService = new ModelPost();
            modelService.setRepositoryHelper(repository);
            modelService.setServices(services);
            modelService.setProjectNode(projectNode);
            modelService.setLogLevel(LogLevel.DEBUG);
            Status status = new Status();
            try {
                modelService.createOrUpdateModel(content, status);
            } catch (Exception e) {
                status.setCode(HttpServletResponse.SC_BAD_REQUEST);
                response.append("ERROR: could not parse request\n");
                e.printStackTrace();
            }
            if (status.getCode() == HttpServletResponse.SC_OK) {
                jobStatus = "Succeeded";
            }
            response.append(modelService.getResponse().toString());
        }

        // Save off the log
        EmsScriptNode logNode; 
        String logName = ((String) jsonNode.getProperty("cm:name")).replace(".json", ".log");
        logNode = jsonNode.getParent().childByNamePath(logName);
        ContentWriter writer = services.getContentService().getWriter(logNode.getNodeRef(), ContentModel.PROP_CONTENT, true);
        writer.putContent(response.toString());
        setContentDataMimeType(writer, logNode, "text/plain", services);

        // Send off the notification email
        String username = (String)jsonNode.getProperty("cm:modifier"); 
        EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username), services, response);
        String recipient = (String)user.getProperty("cm:email");

        jsonNode.setProperty("ems:job_status", jobStatus);

        Action mailAction = services.getActionService().createAction(MailActionExecuter.NAME);
        mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, "[EuropaEMS] Project " + projectName + " Load " + jsonNode.getProperty("ems:job_status"));
        mailAction.setParameterValue(MailActionExecuter.PARAM_TO, recipient);
        mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, "europaems@jpl.nasa.gov");
        mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, "Log URL: " + contextUrl + logNode.getUrl());
        services.getActionService().executeAction(mailAction, null);
    }

    protected void clearCache() {
        response = new StringBuffer();
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub

    }

    public static void setContentDataMimeType(ContentWriter writer, EmsScriptNode node, String mimetype, ServiceRegistry sr) {
        ContentData contentData = writer.getContentData();
        contentData = ContentData.setMimetype(contentData, mimetype);
        sr.getNodeService().setProperty(node.getNodeRef(), ContentModel.PROP_CONTENT, contentData);
    }
}
