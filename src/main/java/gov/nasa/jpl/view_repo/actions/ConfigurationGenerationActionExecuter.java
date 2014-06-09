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
package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;
import gov.nasa.jpl.view_repo.webscripts.SnapshotPost;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.springframework.extensions.webscripts.Status;
import org.springframework.web.context.request.WebRequest;

/**
 * Action for loading the project model in the background asynchronously
 * @author cinyoung
 */
public class ConfigurationGenerationActionExecuter extends ActionExecuterAbstractBase {
    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;

    private StringBuffer response;

    // Parameter values to be passed in when the action is created
    public static final String NAME = "configurationGeneration";
    public static final String PARAM_SITE_NAME = "siteName";
    public static final String PARAM_PRODUCT_LIST = "docList";

    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }
    
    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        clearCache();

        // Get timestamp if specified. This is for the products, not the
        // snapshots or configuration.
        Date dateTime = null;
        if ( action instanceof WebRequest) {
            WebRequest req = (WebRequest)action;
            String timestamp = req.getParameter("timestamp");
            dateTime = TimeUtils.dateFromTimestamp( timestamp );
        }

        // Do not get an older version of the node based on the timestamp since
        // new snapshots should be associated with a new configuration. The
        // timestamp refers to the products, not the snapshots themselves.
        //        if ( dateTime != null ) {
//            NodeRef vRef = NodeUtil.getNodeRefAtTime( nodeRef, dateTime );
//            if ( vRef != null ) nodeRef = vRef; 
//        }
        EmsScriptNode jobNode = new EmsScriptNode(nodeRef, services, response);
        // clear out any existing associated snapshots
        jobNode.removeAssociations("ems:configuredSnapshots");

        @SuppressWarnings("unchecked")
		HashSet<String> productList = (HashSet<String>) action.getParameterValue(PARAM_PRODUCT_LIST);
        
        String siteName = (String) action.getParameterValue(PARAM_SITE_NAME);
        System.out.println("ConfigurationGenerationActionExecuter started execution of " + siteName);
        SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        if (siteInfo == null) {
        		System.out.println("[ERROR]: could not find site: " + siteName);
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
        EmsScriptNode site = new EmsScriptNode(siteRef, services, response);

        Set< EmsScriptNode > productSet =
                WebScriptUtil.getAllNodesInPath( site.getQnamePath(), "ASPECT",
                                                 Acm.ACM_PRODUCT, dateTime,
                                                 services, response );
        
        // create snapshots of all documents
        // TODO: perhaps roll these in their own transactions
        String jobStatus = "Succeeded";
        Set<EmsScriptNode> snapshots = new HashSet<EmsScriptNode>();
        for (EmsScriptNode product: productSet) {
        		// only create the filtered list of documents
        		if (productList.isEmpty() || productList.contains(product.getProperty(Acm.ACM_ID))) {
	            SnapshotPost snapshotService = new SnapshotPost(repository, services);
	            snapshotService.setRepositoryHelper(repository);
	            snapshotService.setServices(services);
	            snapshotService.setLogLevel(LogLevel.DEBUG);
	            Status status = new Status();
	            EmsScriptNode snapshot = snapshotService.createSnapshot(product, (String)product.getProperty(Acm.ACM_ID));
	            if (status.getCode() != HttpServletResponse.SC_OK) {
	                jobStatus = "Failed";
	                response.append("[ERROR]: could not make snapshot for " + product.getProperty(Acm.ACM_NAME));
	            } else {
	                response.append("[INFO]: Successfully created snapshot: " + snapshot.getProperty(Acm.CM_NAME));
	            }
	            if (snapshot != null) {
	                snapshots.add(snapshot);
	            }
	            response.append(snapshotService.getResponse().toString());
        		}
        }
        // make relationships between configuration node and all the snapshots
        for (EmsScriptNode snapshot: snapshots) {
            jobNode.createOrUpdateAssociation(snapshot, "ems:configuredSnapshots", true);
        }
        
        // save off the status of the job
        jobNode.setProperty("ems:job_status", jobStatus);
        
        // Save off the log
        EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode, "text/plain", services, response.toString());

        String contextUrl = "https://" + ActionUtil.getHostName() + ".jpl.nasa.gov/alfresco";
        
        // Send off notification email
        String subject = "[EuropaEMS] Configuration " + siteName + " Generation " + jobStatus;
        String msg = "Log URL: " + contextUrl + logNode.getUrl();
        // TODO: NOTE!!! The following needs to be commented out for local testing....
        ActionUtil.sendEmailToModifier(jobNode, msg, subject, services, response);
        
        System.out.println("Completed configuration set");
    }

    protected void clearCache() {
        response = new StringBuffer();
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub

    }
}
