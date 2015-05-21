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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
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

    // Parameter values to be passed in when the action is created
    public static final String NAME = "snapshotArtifactsGeneration";
    public static final String PARAM_SITE_NAME = "siteName";
    public static final String PARAM_SNAPSHOT_ID = "snapshotId";
    public static final String PARAM_SYSML_ID = "symlId";
    public static final String PARAM_FORMAT_TYPE = "formatType";
    public static final String PARAM_USER_EMAIL = "userEmail";
    public static final String PARAM_WORKSPACE = "workspace";
    
    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }
    
    @Override
    protected void executeImpl(final Action action, final NodeRef nodeRef) {
        clearCache();
        
        new EmsTransaction(services, response, new Status()) {
            @Override
            public void run() throws Exception {
                executeImplImpl(action, nodeRef);
            }
        };
    }
    
    private void executeImplImpl(Action action, NodeRef nodeRef) {

        // Get timestamp if specified. This is for the products, not the
        // snapshots or configuration.
        Date dateTime = null;
        if ( action instanceof WebRequest) {
            WebRequest req = (WebRequest)action;
            String timestamp = req.getParameter("timestamp");
            dateTime = TimeUtils.dateFromTimestamp( timestamp );
        }

        EmsScriptNode jobNode = new EmsScriptNode(nodeRef, services, response);
        // clear out any existing associated snapshots
        String siteName = (String) action.getParameterValue(PARAM_SITE_NAME);
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
        //EmsScriptNode site = new EmsScriptNode(siteRef, services, response);
        
        String jobStatus = "Succeeded";
        String snapshotId = (String)action.getParameterValue(PARAM_SNAPSHOT_ID);
        String sysmlId = (String)action.getParameterValue(PARAM_SYSML_ID);
        ArrayList<String> formats = (ArrayList<String>)action.getParameterValue(PARAM_FORMAT_TYPE);
        SnapshotPost snapshotService = new SnapshotPost(repository, services);
        snapshotService.setRepositoryHelper(repository);
        snapshotService.setServices(services);
        snapshotService.setLogLevel(Level.DEBUG);
        Status status = new Status();
        EmsScriptNode snapshotNode = null;
        JSONObject snapshot = null;
        Date timestamp = null;
        WorkspaceNode workspace = (WorkspaceNode)action.getParameterValue(PARAM_WORKSPACE);
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
    	    //if ( !snapshotNode.hasAspect( "view2:docbook" )) {
	    	response.append("[INFO]: Creating docbook.xml...\n");
            String snapshotName = snapshotNode.getSysmlId();
            timestamp = (Date)snapshotNode.getProperty("view2:timestamp");

            NodeRef viewRef = (NodeRef)snapshotNode.getNodeRefProperty( "view2:snapshotProduct", dateTime,
                                                                        workspace);
	        if (viewRef == null) {
	            // if missing, then check for backwards compatibility
	            EmsScriptNode viewNode = 
	                    snapshotNode.getFirstSourceAssociationByType( "view2:snapshots" );
	            if (viewNode != null) {
	                viewRef = viewNode.getNodeRef();
	            }
	        }
	        EmsScriptNode viewNode = new EmsScriptNode(viewRef, services, response);
	        String viewId = viewNode.getSysmlId();
	        EmsScriptNode snapshotFolder = SnapshotPost.getSnapshotFolderNode(viewNode);
	        String contextPath = "alfresco/service";
	        
	        FullDocPost fullDoc = new FullDocPost(repository, services);
        	fullDoc.setFullDocId(snapshotId);
        	try{
        		
        		fullDoc.downloadHtml(workspace, siteName, sysmlId, timestamp);
        	}
        	catch(Exception ex){
        		status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				logger.error("[NodeJS] Failed to generate HTML artifact for snapshot Id: " + snapshotId);
				response.append(String.format("[ERROR]: [NodeJS] Failed to generate HTML artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
        	}
        	
        	try{
        		fullDoc.html2pdf();
        		fullDoc.savePdfToRepo(snapshotFolder, snapshotNode);
        		fullDoc.saveZipToRepo(snapshotFolder, snapshotNode);
        	}
        	catch(Exception ex){
        		status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				logger.error("[NodeJS] Failed to generate PDF artifact for snapshot Id: " + snapshotId);
				response.append(String.format("[ERROR]: [NodeJS] Failed to generate PDF artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
        	}
        	
        	try{
//        		EmsScriptNode node = new EmsScriptNode
        	}
        	catch(Exception ex){
        		status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				logger.error("Failed to save PDF to Alfresco for snapshot Id: " + snapshotId);
				response.append(String.format("[ERROR]: Failed to save PDF to Alfresco for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
        	}
        	
//    	        
//            DocBookWrapper docBookWrapper = snapshotService.createDocBook( viewNode, viewId, snapshotName, contextPath, snapshotNode, workspace, timestamp, response );
//            if ( docBookWrapper == null ) {
//                logger.error("Failed to generate DocBook!" );
//                snapshotNode = null;
//            } 
//            else {
//                docBookWrapper.save();
//                docBookWrapper.saveDocBookToRepo( snapshotFolder, timestamp );
//            }
////    	    }
////    	    else{
////    	    	response.append("[INFO]: docbook.xml already created.\n");
////    	    }
//        	
//    	    // at this time, we're generating both artifacts everytime
////	        for(String format:formats){
////	        	if(format.compareToIgnoreCase("pdf") == 0){
//    		try{
//    			snapshotNode = snapshotService.generatePDF(snapshotId, timestamp, workspace, siteName);
//    			response.append(snapshotService.getResponse().toString());
//    		}
//    		catch(Exception ex){
//    			status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//    			logger.error("Failed to generate PDF for snapshot Id: " + snapshotId);
//    			response.append(String.format("[ERROR]: Failed to generate PDF for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
//    			snapshot = snapshotService.generatedPDFFailure(snapshotId, timestamp, workspace, siteName);
//    		}
////	        	}
////	        	else if(format.compareToIgnoreCase("html") == 0){
//			try{
//				snapshotNode = snapshotService.generateHTML(snapshotId, timestamp, workspace);
//				response.append(snapshotService.getResponse().toString());
//			}
//			catch(Exception ex){
//				status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//				logger.error("Failed to generate zip artifact for snapshot Id: " + snapshotId);
//				response.append(String.format("[ERROR]: Failed to generate zip artifact for snapshot Id: %s.\n%s\n%s\n", snapshotId, ex.getMessage(), ex.getStackTrace()));
//			}
//			
//			if(snapshotNode != null) snapshot = populateSnapshotProperties(snapshotNode, timestamp, workspace);
////	        	}
//	        	
    		if (status.getCode() != HttpServletResponse.SC_OK) {
            	jobStatus = "Failed";
            	response.append(String.format("[ERROR]: could not make snapshot for %s.\n", snapshotId));
        	} 
        		else {
            	response.append(String.format("[INFO]: Successfully generated artifact(s) for snapshot: %s.\n", snapshotId));
        	}
        	response.append("Snapshot JSON:\n");
        	response.append(NodeUtil.jsonToString( snapshot ));
        	response.append("\n\n");
//	        }
	        // Send off notification email
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
        	try{
	        	for(String format:formats){
	        		if(format.compareToIgnoreCase("pdf") == 0){ 
	        			if(SnapshotPost.getPdfNode(snapshotNode, timestamp, workspace)==null) snapshotService.setPdfStatus(snapshotNode, "Error");
	        		}
	        		else if(format.compareToIgnoreCase("html") == 0){
	        			if(SnapshotPost.getHtmlZipNode(snapshotNode, timestamp, workspace)==null) snapshotService.setHtmlZipStatus(snapshotNode, "Error");  
	        		}
	        	}
        	}
        	catch(Exception e){;}

        	StringBuffer sb = new StringBuffer();
        	Throwable throwable = ex.getCause();
        	while(throwable != null){
        		sb.append(throwable.getMessage());
        		throwable = throwable.getCause();
        	}
        	
        	logger.error("Failed to complete snapshot artifact(s) generation!");
        	logger.error(sb.toString());
        	ex.printStackTrace();
//        	ActionUtil.sendEmailToModifier(jobNode, String.format("An unexpected error occurred and your PDF generation failed.\n%s%s", ex.getMessage(), sb.toString()), "PDF Generation Failed", services);
//        	ActionUtil.sendEmailTo("mbee-dev-admin@jpl.nasa.gov", "mbee-dev-admin@jpl.nasa.gov", 
//        			String.format("Server: %s\nSite: %s\nWorkspace: %s\nSnapshot Id: %s\nError: %s%s%s", 
//        					new HostnameGet(this.repository, this.services).getAlfrescoUrl(),
//        					siteName,
//        					workspace,
//        					snapshotId,
//        					ex.getMessage(), sb.toString(), response.toString()), 
//					"PDF Generation Failed", services);
        }
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

    private JSONObject populateSnapshotProperties( EmsScriptNode snapshotNode, Date dateTime, WorkspaceNode workspace ) throws JSONException {
        JSONObject snapshoturl = snapshotNode.toJSONObject( workspace, dateTime );
        if ( SnapshotPost.hasPdf( snapshotNode ) || SnapshotPost.hasHtmlZip( snapshotNode ) ) {
        	HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        	String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
        	JSONArray formats = new JSONArray();
            if ( SnapshotPost.hasPdfNode( snapshotNode, dateTime, workspace ) ) {
                EmsScriptNode pdfNode = SnapshotPost.getPdfNode( snapshotNode, dateTime, workspace  );
                JSONObject pdfJson = new JSONObject();
                pdfJson.put("status", "Completed");
                pdfJson.put("type", "pdf");
                pdfJson.put("url", contextUrl + pdfNode.getUrl());
                formats.put(pdfJson);
            }
            if ( SnapshotPost.hasHtmlZipNode( snapshotNode, dateTime, workspace  ) ) {
                EmsScriptNode htmlZipNode = SnapshotPost.getHtmlZipNode( snapshotNode, dateTime, workspace  );
                JSONObject htmlJson = new JSONObject();
                htmlJson.put("status", "Completed");
                htmlJson.put("type","html");
                htmlJson.put("url", contextUrl + htmlZipNode.getUrl());
                formats.put(htmlJson);
            }

            snapshoturl.put( "formats", formats );
        }
        return snapshoturl;
    }

}
