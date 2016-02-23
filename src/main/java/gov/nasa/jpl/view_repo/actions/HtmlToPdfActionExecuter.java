package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.HostnameGet;
import gov.nasa.jpl.view_repo.webscripts.HtmlToPdfPost;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

/**
 * Action for converting HTML to PDF in the background asynchronously
 * @author lho
 *
 */
public class HtmlToPdfActionExecuter extends ActionExecuterAbstractBase {
	static Logger logger = Logger.getLogger(HtmlToPdfActionExecuter.class);
	/**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;

    private StringBuffer response;
    private Status responseStatus;

    // Parameter values to be passed in when the action is created
    public static final String NAME = "htmlToPdf";
    public static final String PARAM_SITE_NAME = "siteName";
    public static final String PARAM_DOCUMENT_ID = "documentId";
    public static final String PARAM_COVER = "cover";
    public static final String PARAM_HTML = "html";
    public static final String PARAM_HEADER = "header";
    public static final String PARAM_FOOTER = "footer";
    public static final String PARAM_TAG_ID = "tagId";
    public static final String PARAM_TIME_STAMP = "timeStamp";
    public static final String PARAM_WORKSPACE = "workspace";
    public static final String PARAM_POST_JSON = "postJson";

    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }
    
    public HtmlToPdfActionExecuter(){
    	super();
    }
    
    public HtmlToPdfActionExecuter(Repository repositoryHelper, ServiceRegistry registry){
    	super();
    	setRepository(repositoryHelper);
    	setServices(registry);
    }
    
    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
    	HtmlToPdfActionExecuter instance = new HtmlToPdfActionExecuter(repository, services);
        instance.clearCache();
        instance.executeImplImpl(action, nodeRef);
    }
    
    private void executeImplImpl(final Action action, final NodeRef nodeRef) {
        final StringBuffer allResponse = new StringBuffer();
        
        EmsScriptNode jobNode = new EmsScriptNode(nodeRef, services, response);
        String documentId = (String)action.getParameterValue(PARAM_DOCUMENT_ID);
        String tagId = (String)action.getParameterValue(PARAM_TAG_ID);
        String timeStamp = (String)action.getParameterValue(PARAM_TIME_STAMP);
        String htmlContent = (String)action.getParameterValue(PARAM_HTML);
        String coverContent = (String)action.getParameterValue(PARAM_COVER);
        String headerContent = (String)action.getParameterValue(PARAM_HEADER);
        String footerContent = (String)action.getParameterValue(PARAM_FOOTER);
        HtmlToPdfPost htmlToPdf = new HtmlToPdfPost(repository, services);
        EmsScriptNode pdfNode = htmlToPdf.convert(documentId, tagId, timeStamp, htmlContent, coverContent, headerContent, footerContent);
        if(pdfNode != null){
        	sendEmail(jobNode, pdfNode, response);
        }
    }
    
    protected void sendEmail(EmsScriptNode jobNode, EmsScriptNode pdfNode, StringBuffer response){
    	String status = (pdfNode != null)? "completed":"completed with errors";
    	String subject = String.format("HTML to PDF generation %s.", status);
    	EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode, MimetypeMap.MIMETYPE_TEXT_PLAIN, services, subject + System.lineSeparator() + System.lineSeparator() + response.toString());
    	String msg = buildEmailMessage(pdfNode, response, logNode);
    	ActionUtil.sendEmailToModifier(jobNode,  msg,  subject, services);
        if (logger.isDebugEnabled()) logger.debug("Completed HTML to PDF generation.");

    }
    
	protected String buildEmailMessage(EmsScriptNode pdfNode,
			StringBuffer response, EmsScriptNode logNode) {
		StringBuffer buf = new StringBuffer();
		HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
        
		if(pdfNode == null){
			buf.append("HTML to PDF generation completed with errors. Please review the below link for detailed information.");
		}
		else{
			buf.append("HTML to PDF generation succeeded.");
			buf.append(System.lineSeparator());
			buf.append(System.lineSeparator());
			buf.append("You can access the PDF file at ");
			buf.append(contextUrl + pdfNode.getUrl());
		}
		
		buf.append(System.lineSeparator());
		buf.append(System.lineSeparator());
		buf.append("Log: ");
        buf.append(contextUrl);
        buf.append(logNode.getUrl());
        
		return buf.toString();
	}

	protected void clearCache() {
		response = new StringBuffer();
		responseStatus = new Status();
		NodeUtil.setBeenInsideTransaction(false);
		NodeUtil.setBeenOutsideTransaction(false);
		NodeUtil.setInsideTransactionNow(false);
	}

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub

    }
}
