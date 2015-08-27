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
    
    
    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }
    
    @Override
    protected void executeImpl(final Action action, final NodeRef nodeRef) {
        clearCache();
        executeImplImpl(action, nodeRef);
    }
    
    private void handleException(Exception ex, Action action) {
        
        @SuppressWarnings( "unchecked" )
        ArrayList<String> formats = (ArrayList<String>)action.getParameterValue(PARAM_FORMAT_TYPE);

        for(String format:formats){
            if(format.compareToIgnoreCase("pdf") == 0){ 
                if(SnapshotPost.getPdfNode(snapshotNode, timestamp, workspace)==null){ 
                    setPdfStatus(snapshotService, snapshotNode, "Error");
                }
            }
            else if(format.compareToIgnoreCase("html") == 0){
                if(SnapshotPost.getHtmlZipNode(snapshotNode, timestamp, workspace)==null){ 
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

        new EmsTransaction(services, response, new Status()) {
            @Override
            public void run() throws Exception {
                executeImplImplSetup(action, nodeRef);
            }
        };
               
   
            new EmsTransaction(services, response, new Status()) {
                @Override
                public void run() throws Exception {
        
                    try{
                        // lets check whether or not docbook has been generated
                        StringBuffer response = new StringBuffer();
                        // lookup snapshotNode using standard lucene as snapshotId is unique across all workspaces
                        ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( snapshotId, "@cm\\:name:\"", services );
                        if (nodeRefs == null || nodeRefs.size() != 1) {
                            nodeRefs = NodeUtil.findNodeRefsByType( snapshotId, "@sysml\\:id:\"", services );
                            if (nodeRefs == null || nodeRefs.size() != 1) {
                                throw new Exception("Failed to find snapshot with Id: " + snapshotId);
                            }
                        }
                        
                        snapshotNode = new EmsScriptNode(nodeRefs.get( 0 ), services, response);
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
        
        // downloadHtml() will have its own transactions
        try{
            String sysmlId = (String)action.getParameterValue(PARAM_SYSML_ID);
            String workspaceName = action.getParameterValue(PARAM_WORKSPACE_NAME).toString();
            String timestampVE = action.getParameterValue(PARAM_TIME_STAMP).toString();
            int attempts = 0;
            while(attempts < 3 && !fullDoc.isFullDocHtmlExist()){
            	fullDoc.downloadHtml(workspaceName, siteName, sysmlId, timestampVE, tagTitle);
            }
        }
        catch(Exception ex){
            status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.error("[NodeJS] Failed to generate HTML artifact for snapshot Id: " + snapshotId);
            response.append(String.format("[ERROR]: [NodeJS] Failed to generate HTML artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
        }
        
        new EmsTransaction(services, response, new Status()) {
            @Override
            public void run() throws Exception {
                
                try {
                    try{
                        fullDoc.html2pdf(snapshotFolder, snapshotNode); //convert html to pdf and saves it to repo 
                    }
                    catch(Exception ex){
                        status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        logger.error("[NodeJS] Failed to generate PDF artifact for snapshot Id: " + snapshotId);
                        response.append(String.format("[ERROR]: [NodeJS] Failed to generate PDF artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
                        setPdfStatus(snapshotService, snapshotNode, "Error");
                    }
                    
                    try{
                        fullDoc.saveZipToRepo(snapshotFolder, snapshotNode);
                    }
                    catch(Exception z){
                        status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        logger.error("[NodeJS] Failed to generate PDF artifact for snapshot Id: " + snapshotId);
                        response.append(String.format("[ERROR]: [NodeJS] Failed to generate PDF artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, z.getMessage(), z.getStackTrace()));
                        setZipStatus(snapshotService, snapshotNode, "Error");
                    }
                    
                    if (status.getCode() != HttpServletResponse.SC_OK) {
                        jobStatus = "Failed";
                        response.append(String.format("[ERROR]: could not make snapshot for %s.\n", snapshotId));
                        snapshot = populateSnapshotProperties(snapshotNode, timestamp, workspace, "Error");
                    } 
                    else {
                        response.append(String.format("[INFO]: Successfully generated artifact(s) for snapshot: %s.\n", snapshotId));
                        snapshot = populateSnapshotProperties(snapshotNode, timestamp, workspace, "Completed");
                    }
                    
                    response.append("Snapshot JSON:\n");
                    response.append(snapshot);
                    response.append("\n\n");

                    try{
                        String subject = "PDF Generation " + jobStatus;
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
                catch(Exception ex){
                    handleException( ex, action);
                }
           }
        };
        //make backup docbookxml
        new EmsTransaction(services, response, new Status()) {
            @Override
            public void run() throws Exception {
                
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

                    DocBookWrapper docBookWrapper =  docbook.createDocBook(viewNode, viewId, snapshotNode.getSysmlId(), contextPath, snapshotNode, workspace, timestamp, response);
                    if ( docBookWrapper == null ) {
                        logger.error("Failed to generate DocBook!" );
                        snapshotNode = null;
                    } 
                    else {
                        docBookWrapper.save();
                        docBookWrapper.saveDocBookToRepo( snapshotFolder, timestamp );
                    }
//                      
                    try{
                        snapshotNode = snapshotService.generatePDF(snapshotId, timestamp, workspace, siteName);
                        //response.append(snapshotService.getResponse().toString());
                    }
                    catch(Exception ex){
                        //status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        logger.error("Failed to generate PDF for snapshot Id: " + snapshotId);
                        //response.append(String.format("[ERROR]: Failed to generate PDF for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
                        //snapshot = snapshotService.generatedPDFFailure(snapshotId, timestamp, workspace, siteName);
                    }
//                          }
//                          else if(format.compareToIgnoreCase("html") == 0){
                    try{
                        snapshotNode = snapshotService.generateHTML(snapshotId, timestamp, workspace);
                        //response.append(snapshotService.getResponse().toString());
                    }
                    catch(Exception ex){
                        //status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        logger.error("Failed to generate zip artifact for snapshot Id: " + snapshotId);
                        //response.append(String.format("[ERROR]: Failed to generate zip artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
                    }
                } catch(Exception ex){
               
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

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub
    }
    
    private String buildEmailMessage(JSONObject snapshot, EmsScriptNode logNode) throws Exception{
        	StringBuffer buf = new StringBuffer();
        	try{
    	    	JSONArray formats = (JSONArray)snapshot.getJSONArray("formats");
    	    	for(int i=0; i < formats.length(); i++){
    				JSONObject format = formats.getJSONObject(i);
    				String formatType = format.getString("type");
    				if(formatType.compareToIgnoreCase("HTML")==0) formatType = "ZIP";
    				String formatUrl = format.getString("url");
    				buf.append("Snapshot ");
    				buf.append(formatType.toUpperCase());
    				buf.append(": ");
    				buf.append(formatUrl);
    				buf.append(System.lineSeparator());
    				buf.append(System.lineSeparator());
    			}
    	    	HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
    	    	String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
    	    	buf.append("Log: ");
				buf.append(contextUrl);
				buf.append(logNode.getUrl());
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
            if ( SnapshotPost.hasPdfNode( snapshotNode, dateTime, workspace ) ) {
                EmsScriptNode pdfNode = SnapshotPost.getPdfNode( snapshotNode, dateTime, workspace  );
                JSONObject pdfJson = new JSONObject();
                pdfJson.put("status", status);
                pdfJson.put("type", "pdf");
                pdfJson.put("url", contextUrl + pdfNode.getUrl());
                formats.put(pdfJson);
            }
            if ( SnapshotPost.hasHtmlZipNode( snapshotNode, dateTime, workspace  ) ) {
                EmsScriptNode htmlZipNode = SnapshotPost.getHtmlZipNode( snapshotNode, dateTime, workspace  );
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
