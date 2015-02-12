package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;
import gov.nasa.jpl.view_repo.webscripts.DocBookWrapper;
import gov.nasa.jpl.view_repo.webscripts.SnapshotPost;

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
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.connector.User;
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
        ArrayList<String> formats = (ArrayList<String>)action.getParameterValue(PARAM_FORMAT_TYPE);
        SnapshotPost snapshotService = new SnapshotPost(repository, services);
        snapshotService.setRepositoryHelper(repository);
        snapshotService.setServices(services);
        snapshotService.setLogLevel(LogLevel.DEBUG);
        Status status = new Status();
        JSONObject snapshot = null;
        try{
        	    WorkspaceNode workspace = (WorkspaceNode)action.getParameterValue(PARAM_WORKSPACE);
        	    
        	    // lets check whether or not docbook has been generated
        	    StringBuffer response = new StringBuffer();
        	    EmsScriptNode snapshotNode = NodeUtil.findScriptNodeById(snapshotId, workspace, null, false, services, response);
        	    if ( !snapshotNode.hasAspect( "view2:docbook" )) {
                String snapshotName = snapshotNode.getSysmlId();
                Date timestamp = (Date)snapshotNode.getProperty("view2:timestamp");

                NodeRef viewRef = (NodeRef)snapshotNode.getProperty( "view2:snapshotProduct" );
        	        if (viewRef == null) {
        	            
        	        }
        	        EmsScriptNode viewNode = new EmsScriptNode(viewRef, services, response);
        	        String viewId = viewNode.getSysmlId();
        	        EmsScriptNode snapshotFolder = SnapshotPost.getSnapshotFolderNode(viewNode);
        	        String contextPath = "alfresco/service";
        	        
                DocBookWrapper docBookWrapper = snapshotService.createDocBook( viewNode, viewId, snapshotName, contextPath, snapshotNode, workspace, timestamp );
                if ( docBookWrapper == null ) {
                    logger.error("Failed to generate DocBook!" );
                    snapshotNode = null;
                } else {
                    docBookWrapper.save();
                    docBookWrapper.saveDocBookToRepo( snapshotFolder, timestamp );
                }

        	    }
        	    
	        for(String format:formats){
        	        	if(format.compareToIgnoreCase("pdf") == 0){
        	        		try{
        	        			snapshot = snapshotService.generatePDF(snapshotId, workspace);
        	        		}
        	        		catch(JSONException ex){
        	        			status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	        			logger.error("Failed to generate PDF for snapshot Id: " + snapshotId);
        	        		}
        	        	}
        	        	else if(format.compareToIgnoreCase("html") == 0){
        	        		try{
        	        			snapshot = snapshotService.generateHTML(snapshotId, workspace);
        	        		}
        	        		catch(JSONException ex){
        	        			status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	        			logger.error("Failed to generate HTML zip for snapshot Id: " + snapshotId);
        	        		}
        	        	}
        	        	
        	        	if (status.getCode() != HttpServletResponse.SC_OK) {
        	            	jobStatus = "Failed";
        	            	response.append("[ERROR]: could not make snapshot for " + snapshotId);
        	        	} else {
        	            	response.append("[INFO]: Successfully generated artifact for snapshot: " + snapshotId);
        	        	}
        	        	response.append(snapshot.toString());
	        }
	        // Send off notification email
	        String subject = "Snapshot Generation " + jobStatus;
	        String msg = buildEmailMessage(snapshot);
        	    ActionUtil.sendEmailToModifier(jobNode, msg, subject, services);
        	    if (logger.isDebugEnabled()) logger.debug("Completed snapshot artifact(s) generation.");
        }
        catch(Exception ex){
            	logger.error("Failed to complete snapshot artifact(s) generation!");
            	ex.printStackTrace();
            	ActionUtil.sendEmailToModifier(jobNode, "An unexpected error occurred and your snapshot artifact generation failed. " + ex.getMessage(), "Snapshot Generation Failed", services);
        }
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub
    }
    
    private String buildEmailMessage(JSONObject snapshot) throws Exception{
        	StringBuffer buf = new StringBuffer();
        	try{
        		//String hostname = ActionUtil.getHostName();
                //if (!hostname.endsWith( ".jpl.nasa.gov" )) {
                //    hostname += ".jpl.nasa.gov";
                //}
                //String contextUrl = "https://" + hostname + "/alfresco";
    	    	JSONArray formats = snapshot.getJSONArray("formats");
    	    	for(int i=0; i < formats.length(); i++){
    				JSONObject format = formats.getJSONObject(i);
    				String formatType = format.getString("type");
    				String formatUrl = format.getString("url");
    				buf.append("Snapshot ");
    				buf.append(formatType.toUpperCase());
    				buf.append(": ");
    //				buf.append(contextUrl);
    				buf.append(formatUrl);
    				buf.append(System.lineSeparator());
    				buf.append(System.lineSeparator());
    			}
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

}
