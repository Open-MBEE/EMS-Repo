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
 * Inspiration taken from org.alfresco.repo.action.executer.TransformActionExecuter
 * @author cinyoung
 */
public class ModelLoadActionExecuter extends ActionExecuterAbstractBase {

    //	private static final Log logger = LogFactory.getLog(TransformDBActionExecuter.class);

    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;        // used for lucene search
    private String contextUrl;

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

    private StringBuffer response;

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
