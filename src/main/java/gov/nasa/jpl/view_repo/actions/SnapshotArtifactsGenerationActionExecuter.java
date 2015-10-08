package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;




//import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;
import org.apache.log4j.*;

import gov.nasa.jpl.view_repo.webscripts.DocBookWrapper;
import gov.nasa.jpl.view_repo.webscripts.FullDocPost;
import gov.nasa.jpl.view_repo.webscripts.HostnameGet;
import gov.nasa.jpl.view_repo.webscripts.SnapshotPost;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;
import org.springframework.web.context.request.WebRequest;

public class SnapshotArtifactsGenerationActionExecuter  extends ActionExecuterAbstractBase {
    static Logger logger = Logger.getLogger(SnapshotArtifactsGenerationActionExecuter.class);
    static public boolean makeDocBook = true;
    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;

    private StringBuffer response;
    
    // Variables needed for transactions
    private FullDocPost fullDoc;
    private EmsScriptNode snapshotFolder;
    private EmsScriptNode snapshotNode;
    private Status status;
    private String snapshotId;
    private SnapshotPost snapshotService;
    private String jobStatus;
    private JSONObject snapshot;
    private Date timestamp;
    private WorkspaceNode workspace;
    private EmsScriptNode jobNode;
    private String siteName;
    private Date dateTime;
    private String tagTitle;
    
    protected EmsScriptNode docbookPdfNode = null;
    protected EmsScriptNode docbookZipNode = null;
    
    protected boolean sentEmail = false;
    
    //backup docbook gen
    private SnapshotPost docbook;
    
    // Parameter values to be passed in when the action is created
    public static final String NAME = "snapshotArtifactsGeneration";
    public static final String PARAM_SITE_NAME = "siteName";
    public static final String PARAM_SNAPSHOT_ID = "snapshotId";
    public static final String PARAM_SYSML_ID = "symlId";
    public static final String PARAM_FORMAT_TYPE = "formatType";
    public static final String PARAM_USER_EMAIL = "userEmail";
    public static final String PARAM_WORKSPACE = "workspace";
    public static final String PARAM_WORKSPACE_NAME = "workspaceName";
    public static final String PARAM_TIME_STAMP = "timestamp";
    public static final String PARAM_CONFIGURATION_ID = "configId";
    
    
    public SnapshotArtifactsGenerationActionExecuter() {
        super();
    }
    public SnapshotArtifactsGenerationActionExecuter(Repository repositoryHelper, ServiceRegistry registry) {
        super();
        setRepository( repositoryHelper );
        setServices( registry );
    }
    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }
    
    @Override
    protected void executeImpl(final Action action, final NodeRef nodeRef) {
        SnapshotArtifactsGenerationActionExecuter instance = new SnapshotArtifactsGenerationActionExecuter(repository, services );
        instance.clearCache();
        instance.executeImplImpl(action, nodeRef);
    }
    
    private void handleException(Exception ex, Action action) {
        
        @SuppressWarnings( "unchecked" )
        ArrayList<String> formats = (ArrayList<String>)action.getParameterValue(PARAM_FORMAT_TYPE);

        for(String format:formats){
            if(format.compareToIgnoreCase("pdf") == 0){ 
                if(SnapshotPost.getPdfNode(snapshotNode, timestamp)==null){ 
                    setPdfStatus(snapshotService, snapshotNode, "Error");
                }
            }
            else if(format.compareToIgnoreCase("html") == 0){
                if(SnapshotPost.getHtmlZipNode(snapshotNode, timestamp)==null){ 
                    setZipStatus(snapshotService, snapshotNode, "Error");
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        Throwable throwable = ex.getCause();
        while(throwable != null){
            sb.append(throwable.getMessage());
            throwable = throwable.getCause();
        }
        
        logger.error("Failed to complete snapshot artifact(s) generation!");
        logger.error(sb.toString());
        ex.printStackTrace();
        response.append("\n[ERROR]: " + sb.toString());
        ActionUtil.sendEmailToModifier(jobNode, String.format("An unexpected error occurred and your PDF generation failed.\n%s%s", ex.getMessage(), sb.toString()), "PDF Generation Failed", services);
        ActionUtil.sendEmailTo("mbee-dev-admin@jpl.nasa.gov", "mbee-dev-admin@jpl.nasa.gov", 
                String.format("Server: %s\nSite: %s\nWorkspace: %s\nSnapshot Id: %s\nError: %s%s%s", 
                        new HostnameGet(this.repository, this.services).getAlfrescoUrl(),
                        siteName,
                        workspace,
                        snapshotId,
                        ex.getMessage(), sb.toString(), response.toString()), 
                "PDF Generation Failed", services);
    }
    
    private void executeImplImplSetup(Action action, NodeRef nodeRef) {
        
        // Get timestamp if specified. This is for the products, not the
        // snapshots or configuration.
        dateTime = null;
        if ( action instanceof WebRequest) {
            WebRequest req = (WebRequest)action;
            String timestamp = req.getParameter("timestamp");
            dateTime = TimeUtils.dateFromTimestamp( timestamp );
        }

        jobNode = new EmsScriptNode(nodeRef, services, response);
        // clear out any existing associated snapshots
        siteName = (String) action.getParameterValue(PARAM_SITE_NAME);
        if (logger.isDebugEnabled()) {
            logger.debug("SnapshotArtifactsGenerationActionExecuter started execution of " + siteName);   
        };
       
        SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        if (siteInfo == null) {
            logger.error("could not find site: " + siteName);
            return;
        }
        NodeRef siteRef = siteInfo.getNodeRef();
        
        // If the version of the site ever changes, its products may also
        // change, so get the products for the version of the site according to
        // the specified date/time. Actually, this probably doesn't matter based
        // on how the site node is used.
       if ( dateTime != null ) {
            NodeRef vRef = NodeUtil.getNodeRefAtTime( siteRef, dateTime );
            if ( vRef != null ) siteRef = vRef;
        }
        
        jobStatus = "Succeeded";
        snapshotId = (String)action.getParameterValue(PARAM_SNAPSHOT_ID);
        snapshotService = new SnapshotPost(repository, services);
        snapshotService.setRepositoryHelper(repository);
        snapshotService.setServices(services);
        snapshotService.setLogLevel(Level.DEBUG);
        status = new Status();
        snapshot = null;
        timestamp = null;
        workspace = (WorkspaceNode)action.getParameterValue(PARAM_WORKSPACE);
                
    }
    
    private void executeImplImpl(final Action action, final NodeRef nodeRef) {
        final StringBuffer allResponse = new StringBuffer();

        try{
        	response.append("\n[INFO]: Setting up background process...");
        	new EmsTransaction(services, response, new Status()) {
        		@Override
            	public void run() throws Exception {
                	executeImplImplSetup(action, nodeRef);
	            }
	        };
   
	        response.append("\n[INFO]: Retrieving node references...");
	        new EmsTransaction(services, response, new Status()) {
                @Override
                public void run() throws Exception {
        
                	try{
                        // lets check whether or not docbook has been generated
                        StringBuffer response = new StringBuffer();
                        snapshotNode = snapshotService.getSnapshotNode(snapshotId);
                        if(snapshotNode == null) throw new Exception("Failed to find snapshot with alfresco ID: " + snapshotId);
                        timestamp = (Date)snapshotNode.getProperty("view2:timestamp");
                        
                        //get tag name from config id
                        String configId = action.getParameterValue(PARAM_CONFIGURATION_ID).toString(); 
                        ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
                        EmsScriptNode configNode = configWs.getConfiguration(configId);
                        tagTitle = configNode.getProperty("cm:title").toString();
                        
                        NodeRef viewRef = (NodeRef)snapshotNode.getNodeRefProperty( "view2:snapshotProduct", dateTime,
                                                                                    workspace);
                        if (viewRef == null) {
                            // if missing, then check for backwards compatibility
                            EmsScriptNode viewNode = snapshotNode.getFirstSourceAssociationByType( "view2:snapshots" );
                            if (viewNode != null) {
                                viewRef = viewNode.getNodeRef();
                            }
                        }
                        EmsScriptNode viewNode = new EmsScriptNode(viewRef, services, response);
                        snapshotFolder = SnapshotPost.getSnapshotFolderNode(viewNode);
                        
                        fullDoc = new FullDocPost(repository, services);
                        fullDoc.setFullDocId(snapshotId);
                        
                        docbook = new SnapshotPost(repository, services);
	                }
	                catch(Exception ex){
	                    handleException( ex, action);
	                }
                }
	        };
	        
	        //make backup docbookxml
	        if (makeDocBook) {
	        	response.append("\n[INFO]: Generating artifacts using DocBook...\n");
		        new EmsTransaction(services, response, new Status()) {
		            @Override
		            public void run() throws Exception {
		                String docbookJobStatus = "Succeeded";
		                try { //mostly copied from tag 2.1.4's code
		                    NodeRef viewRef = (NodeRef)snapshotNode.getNodeRefProperty( "view2:snapshotProduct", dateTime,
	                            workspace);
		                    if (viewRef == null) {
		                        // if missing, then check for backwards compatibility
		                        EmsScriptNode viewNode = snapshotNode.getFirstSourceAssociationByType( "view2:snapshots" );
		                        if (viewNode != null) {
		                            viewRef = viewNode.getNodeRef();
		                        }
		                    }
		                    
		                    EmsScriptNode viewNode = new EmsScriptNode(viewRef, services, response);
		                    String viewId = viewNode.getSysmlId();
		                    String contextPath = "alfresco/service";
	                    
//		                    DocBookWrapper docBookWrapper =  docbook.createDocBook(viewNode, viewId, snapshotNode.getSysmlId(), contextPath, snapshotNode, workspace, timestamp, response);
		                    DocBookWrapper docBookWrapper =  docbook.createDocBook(viewNode, viewId, snapshotNode.getId(), contextPath, snapshotNode, workspace, timestamp, response);
		                    if ( docBookWrapper == null ) {
		                        logger.error("Failed to generate DocBook!" );
		                        snapshotNode = null;
		                    } 
		                    else {
		                    	response.append("\n[INFO]: Saving DocBook XML file to filesystem...");
		                        docBookWrapper.save();
		                        response.append("\n[INFO]: Saving DocBook XML file to Alfresco...");
		                        docBookWrapper.saveDocBookToRepo( snapshotFolder, timestamp );
		                    }

		                    try{
		                    	response.append("\n[INFO]: Transforming DocBook XML to PDF...");
		                        snapshotNode = snapshotService.generatePDF(docbookPdfNode, snapshotId, timestamp, workspace, siteName);
		                        response.append(snapshotService.getResponse().toString());
		                    }
		                    catch(Exception ex){
		                        logger.error("Failed to generate docbook PDF for snapshot Id: " + snapshotId);
		                        response.append(String.format("\n[ERROR]: Failed to generate docbook PDF for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
		                        docbookJobStatus = "Failed";
		                    }
		                    try{
		                    	response.append("\n[INFO]: Transforming DocBook XML to HTML...");
		                        snapshotNode = snapshotService.generateHTML(docbookZipNode, snapshotId, timestamp, workspace);
		                        response.append(snapshotService.getResponse().toString());
		                    }
		                    catch(Exception ex){
		                        logger.error("Failed to generate zip artifact for snapshot Id: " + snapshotId);
		                        response.append(String.format("\n[ERROR]: Failed to generate docbook zip artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
		                        docbookJobStatus = "Failed";
		                    }
		                    
		                    allResponse.append(response.toString());
		                    if ( !sentEmail ) {
		                    	response.append("\n[INFO]: Sending email on DocBook job status...");
		                        sendEmail( allResponse, docbookJobStatus );
		                    }
		                } 
		                catch(Exception ex) {
		                   StringBuffer sb = new StringBuffer();
		                   Throwable throwable = ex.getCause();
		                   while(throwable != null){
		                       sb.append(throwable.getMessage());
		                       throwable = throwable.getCause();
		                   }
		                   
		                   logger.error("Failed to complete docbook snapshot artifact(s) generation!");
		                   logger.error(sb.toString());
		                   ex.printStackTrace();
		                }
		            }
		        };
	        }
        
	        // downloadHtml() will have its own transactions
	        try{
	            String sysmlId = (String)action.getParameterValue(PARAM_SYSML_ID);
	            String workspaceName = action.getParameterValue(PARAM_WORKSPACE_NAME).toString();
	            String timestampVE = action.getParameterValue(PARAM_TIME_STAMP).toString();
	            int attempts = 0;
	            while(attempts++ < 3 && !fullDoc.isFullDocHtmlExist()){
	            	response.append("\n[INFO]: Downloading HTML...");
	            	fullDoc.downloadHtml(workspaceName, siteName, sysmlId, timestampVE, tagTitle);
	            }
	            
	            if(!fullDoc.allViewsSucceeded){ 
	            	jobStatus = "Failed";
	            	response.append("\n[ERROR]: Not all views were downloaded.");
	            }
	        }
	        catch(Exception ex){
	        	jobStatus = "Failed";
	            status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	            logger.error("[Prerender] Failed to generate HTML artifact for snapshot Id: " + snapshotId);
	            response.append(String.format("\n[ERROR]: [Prerender] Failed to generate HTML artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
	        }
	        
	        new EmsTransaction(services, response, new Status()) {
	            @Override
	            public void run() throws Exception {
	                
	                try {
	                    try{
	                    	response.append("\n[INFO]: Transforming HTML to PDF...");
	                        fullDoc.html2pdf(snapshotFolder, snapshotNode); //convert html to pdf and saves it to repo
	                        EmsScriptNode docbookPdf = snapshotService.getPdfNode();
	                        if(docbookPdf == null || (docbookPdf != null && !docbookPdf.exists())){
	                        	fullDoc.setPdfAspect(snapshotNode);
	                        }
	                    }
	                    catch(Exception ex){
	                    	jobStatus = "Failed";
	                        status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	                        logger.error("[Prerender] Failed to generate PDF artifact for snapshot Id: " + snapshotId);
	                        String errMsg = String.format("\n[ERROR]: [Prerender] Failed to generate PDF artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace());
	                        response.append(errMsg);
//	                        setPdfStatus(snapshotService, snapshotNode, "Error");
//	                        throw new Exception(errMsg, ex);
	                    }
	                    
	                    try{
	                    	response.append("\n[INFO]: Zipping files...");
	                        fullDoc.saveZipToRepo(snapshotFolder, snapshotNode);
	                    }
	                    catch(Exception z){
	                    	jobStatus = "Failed";
	                        status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	                        logger.error("[Prerender] Failed to generate Zip artifact for snapshot Id: " + snapshotId);
	                        String zErrMsg = String.format("\n[ERROR]: [Prerender] Failed to generate Zip artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, z.getMessage(), z.getStackTrace());
	                        response.append(zErrMsg);
	                        setZipStatus(snapshotService, snapshotNode, "Error");
	                    }
	                    
	                    if (status.getCode() != HttpServletResponse.SC_OK) {
	                        jobStatus = "Failed";
	                        response.append(String.format("\n[ERROR]: could not make snapshot for %s.\n", snapshotId));
	                        snapshot = populateSnapshotProperties(snapshotNode, timestamp, workspace, "Error");
	                    }
	                    else {
	                        response.append(String.format("\n[INFO]: Successfully generated artifact(s) for snapshot: %s.\n", snapshotId));
	                        snapshot = populateSnapshotProperties(snapshotNode, timestamp, workspace, "Completed");
	                    }
	                    
	                    if ( makeDocBook ) {
	                        // Create placeholder docbook nodes now so that we can
	                        // include URLs for them in the e-mail message.
	                        String snapshotName = snapshotNode.getName();
	                        docbookPdfNode =
	                                NodeUtil.getOrCreateContentNode( snapshotFolder,
	                                                                 snapshotName
	                                                                 + DocBookWrapper.docbookFileSuffix
	                                                                 + ".pdf",
	                                                                 services );                   
	                        
	                        docbookZipNode =
	                                NodeUtil.getOrCreateContentNode( snapshotFolder,
	                                                                 snapshotName
	                                                                 + DocBookWrapper.docbookFileSuffix
	                                                                 + ".zip",
	                                                                 services );                   
	                    }
	                    response.append("\nSnapshot JSON:\n");
	                    response.append(snapshot);
	                    response.append("\n\n");
	                    allResponse.append(response.toString());
//	                    if ( jobStatus.equals( "Succeeded" ) ) {
	                    if(!sentEmail){
	                        sendEmail( response );
	                        sentEmail = true;
	                    }
	                }
	                catch(Exception ex){
	                    handleException( ex, action);
	                }
	           }
	        };
        }
        catch(Throwable ex){
        	ex.printStackTrace();
        }
        finally{
        	if(fullDoc != null){
        		fullDoc.cleanupFiles();
        	}
        	
        	if(docbook != null){
        		docbook.cleanupFiles();
        	}
        }
    }

    protected void sendEmail( StringBuffer response ) {
        try{
            String statusStr = jobStatus;
            if ( !fullDoc.allViewsFailed && !fullDoc.allViewsSucceeded ) {
                statusStr = "completed with errors";
            }
            String subject = "PDF generation " + statusStr.toLowerCase();
            EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode, "text/plain", services, response.toString());
            String msg = buildEmailMessage(snapshot, logNode);
            ActionUtil.sendEmailToModifier(jobNode, msg, subject, services);
            if (logger.isDebugEnabled()) logger.debug("Completed snapshot artifact(s) generation.");
        }
        catch(Exception ex){
            System.out.println("Failed to email PDF generation status.");
            Throwable throwable = ex.getCause();
            while(throwable != null){
                System.out.println(throwable.getMessage());
                System.out.println(throwable.getCause());
            }
        }

    }

    protected void sendEmail( StringBuffer response, String statusOfJob ) {
      try{
          String statusStr = jobStatus.toLowerCase();
          if ( !fullDoc.allViewsFailed && !fullDoc.allViewsSucceeded ) {
              statusStr = "completed with errors";
          }
          //String subject = "PDF Generation " + statusStr + "";
          String subject = "Docbook PDF generation " + statusOfJob.toLowerCase();
//          String sentence = subject + " after PDF generation " + statusStr + ".";
          String msg1 = subject + System.lineSeparator() + System.lineSeparator() + response.toString();
          EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode, "text/plain", services, msg1);
          String msg = buildEmailMessageForDocbook(snapshotService.getPdfNode(), snapshotService.getZipNode(), snapshot, logNode);
          ActionUtil.sendEmailToModifier(jobNode, msg, subject, services);
          if (logger.isDebugEnabled()) logger.debug("Completed docbook snapshot artifact(s) generation.");
      }
      catch(Exception ex){
          System.out.println("Failed to email PDF generation status.");
          Throwable throwable = ex.getCause();
          while(throwable != null){
              System.out.println(throwable.getMessage());
              System.out.println(throwable.getCause());
          }
      }
}

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub
    }
    
    private String buildEmailMessage(JSONObject snapshot, EmsScriptNode logNode) throws Exception{
        	StringBuffer buf = new StringBuffer();
        	try{
        		HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
    	    	String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
    	    	
    	    	JSONArray formats = (JSONArray)snapshot.getJSONArray("formats");
    	    	for(int i=0; i < formats.length(); i++){
    				JSONObject format = formats.getJSONObject(i);
    				String formatType = format.getString("type");
    				if(formatType.compareToIgnoreCase("HTML")==0) formatType = "ZIP";
    				String formatUrl = format.getString("url");
    				buf.append("Snapshot ");
    				buf.append(formatType.toUpperCase());
    				buf.append(": ");
    				if(formatType.toUpperCase().compareTo("PDF")==0) 
    					buf.append(contextUrl + fullDoc.getPdfNode().getUrl());
    				else
    					buf.append(formatUrl);
    				
    				buf.append(System.lineSeparator());
    				buf.append(System.lineSeparator());
    			}
    	    	buf.append("Log: ");
				buf.append(contextUrl);
				buf.append(logNode.getUrl());
				
				if ( makeDocBook ) {
                    boolean gotPdfNode = NodeUtil.exists( docbookPdfNode );
                    boolean gotZipNode = NodeUtil.exists( docbookZipNode );
                    if ( gotPdfNode || gotZipNode ) {
                        buf.append(System.lineSeparator());
                        buf.append(System.lineSeparator());
                        String pdfHref = "pdf";
                        String zipHref = "zip";
                        if ( gotPdfNode ) {
                            pdfHref = "<a href=\"" + contextUrl + docbookPdfNode.getUrl() + "\">pdf</a>";
                        }
                        if ( gotZipNode ) {
                            zipHref = "<a href=\"" + contextUrl + docbookZipNode.getUrl() + "\">zip</a>";
                        }
//                        buf.append("DocBook " + pdfHref + " and " + zipHref + 
//                                   " are processing and will be available at a later time.");
                        buf.append(System.lineSeparator());
                        buf.append("DocBook PDF and ZIP generation");
                        buf.append(System.lineSeparator());
                        buf.append(System.lineSeparator());
                        buf.append("DocBook PDF: " + contextUrl + docbookPdfNode.getUrl());
                        buf.append(System.lineSeparator() );
                        buf.append(System.lineSeparator());
                        buf.append("DocBook zip: " + contextUrl + docbookZipNode.getUrl());
                        buf.append(System.lineSeparator() );
                    }
				}
				
				if (snapshotFolder != null) {
				    buf.append(System.lineSeparator());
				    buf.append(System.lineSeparator());
				    buf.append("Folder: ");
				    buf.append(contextUrl + snapshotFolder.getUrl());
				}
				
        	}
        	catch(JSONException ex){
        		throw new Exception("Failed to build email message!", ex);
        	}
        	return buf.toString();
    }	
    	
    private String buildEmailMessageForDocbook(EmsScriptNode pdf, EmsScriptNode zip, JSONObject snapshot, EmsScriptNode logNode) throws Exception{
        StringBuffer buf = new StringBuffer();
        try{
            HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
            String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
            if (pdf != null) {
                buf.append("Snapshot PDF: " + contextUrl + pdf.getUrl());
                buf.append(System.lineSeparator());
                buf.append(System.lineSeparator());
            }
            if (zip != null) {
                buf.append("Snapshot ZIP: " + contextUrl + zip.getUrl());
                buf.append(System.lineSeparator());
                buf.append(System.lineSeparator());
            }
            buf.append("Log: ");
            buf.append(contextUrl);
            buf.append(logNode.getUrl());
            if (snapshotFolder != null) {
                buf.append(System.lineSeparator());
                buf.append(System.lineSeparator());
                buf.append("Folder: ");
                buf.append(contextUrl + snapshotFolder.getUrl());
            }
            
            buf.append(System.lineSeparator());
            buf.append(System.lineSeparator());
            buf.append("Prerender PDF and ZIP generation are processing and will be available at a later time.");
            buf.append(System.lineSeparator());
        }
        catch(JSONException ex){
            throw new Exception("Failed to build email message!", ex);
        }
        return buf.toString();
    }
    
    protected void clearCache() {
        response = new StringBuffer();
        NodeUtil.setBeenInsideTransaction( false );
        NodeUtil.setBeenOutsideTransaction( false );
        NodeUtil.setInsideTransactionNow( false );
    }

    private JSONObject populateSnapshotProperties( EmsScriptNode snapshotNode, Date dateTime, WorkspaceNode workspace, String status ) throws JSONException {
        JSONObject snapshoturl = snapshotNode.toJSONObject( workspace, dateTime );
        if ( SnapshotPost.hasPdf( snapshotNode ) || SnapshotPost.hasHtmlZip( snapshotNode ) ) {
        	HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        	String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
        	JSONArray formats = new JSONArray();
            if ( SnapshotPost.hasPdfNode( snapshotNode, dateTime ) ) {
                EmsScriptNode pdfNode = SnapshotPost.getPdfNode( snapshotNode, dateTime );
                JSONObject pdfJson = new JSONObject();
                pdfJson.put("status", status);
                pdfJson.put("type", "pdf");
                pdfJson.put("url", contextUrl + pdfNode.getUrl());
                formats.put(pdfJson);
            }
            if ( SnapshotPost.hasHtmlZipNode( snapshotNode, dateTime ) ) {
                EmsScriptNode htmlZipNode = SnapshotPost.getHtmlZipNode( snapshotNode, dateTime );
                JSONObject htmlJson = new JSONObject();
                htmlJson.put("status", status);
                htmlJson.put("type","html");
                htmlJson.put("url", contextUrl + htmlZipNode.getUrl());
                formats.put(htmlJson);
            }

            snapshoturl.put( "formats", formats );
        }
        return snapshoturl;
    }
    
    protected void setPdfStatus(SnapshotPost snapshotService, EmsScriptNode snapshotNode, String status){
    	try{
			snapshotService.setPdfStatus(snapshotNode, status);
		}
		catch(Exception e){
			logger.error(String.format("Failed to set snapshot PDF status to '%s'! %s", status, e.getMessage()));
		}
    }
    
    protected void setZipStatus(SnapshotPost snapshotService, EmsScriptNode snapshotNode, String status){
    	try{
			snapshotService.setHtmlZipStatus(snapshotNode, status);
		}
		catch(Exception e){
			logger.error(String.format("Failed to set snapshot Zip status to '%s'! %s", status, e.getMessage()));
		}
    }

}

