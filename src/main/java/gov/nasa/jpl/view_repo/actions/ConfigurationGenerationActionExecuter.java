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

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;
import gov.nasa.jpl.view_repo.webscripts.ProductListGet;
import gov.nasa.jpl.view_repo.webscripts.SnapshotPost;

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
    private String contextUrl;

    private StringBuffer response;

    // Parameter values to be passed in when the action is created
    public static final String NAME = "configurationGeneration";
    public static final String PARAM_SITE_NAME = "siteName";

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
        clearCache();

        EmsScriptNode jobNode = new EmsScriptNode(nodeRef, services, response);

        // grab all documents in site
        String siteName = (String) action.getParameterValue(PARAM_SITE_NAME);
        System.out.println("ConfigurationGenerationActionExecuter started execution of " + siteName);
        SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        if (siteInfo == null) {
            return;
        }
        EmsScriptNode site = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
        ProductListGet productService = new ProductListGet();
        Set<EmsScriptNode> productSet = productService.getProductSet(site.getQnamePath());

        // create snapshots of all documents
        // TODO: perhaps roll these in their own transactions
        String jobStatus = "Succeeded";
        Set<EmsScriptNode> snapshots = new HashSet<EmsScriptNode>();
        for (EmsScriptNode product: productSet) {
            // Update the model
            SnapshotPost snapshotService = new SnapshotPost();
            snapshotService.setRepositoryHelper(repository);
            snapshotService.setServices(services);
            snapshotService.setLogLevel(LogLevel.DEBUG);
            Status status = new Status();
            EmsScriptNode snapshot = snapshotService.createSnapshot((String)product.getProperty(Acm.ACM_ID));
            if (status.getCode() != HttpServletResponse.SC_OK) {
                jobStatus = "Failed";
                response.append("[ERROR]: could not make snapshot for " + product.getProperty(Acm.ACM_NAME));
            } else {
                response.append("[INFO]: Successfully created snapshot: " + snapshot.getProperty(Acm.ACM_CM_NAME));
            }
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
            response.append(snapshotService.getResponse().toString());
        }
        // make relationships between configuration nodea nd all the snapshots
        for (EmsScriptNode snapshot: snapshots) {
            jobNode.createOrUpdateAssociation(snapshot, "ems:configuredSnapshots", true);
        }
        
        // save off the status of the job
        jobNode.setProperty("ems:job_status", jobStatus);
        
        // Save off the log
        EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode, "text/plain", services, response);

        // Send off notification email
        String subject = "[EuropaEMS] Configuration " + siteName + " Generation " + jobStatus;
        String msg = "Log URL: " + contextUrl + logNode.getUrl();
        ActionUtil.sendEmailToModifier(jobNode, msg, subject, services, response);
    }

    protected void clearCache() {
        response = new StringBuffer();
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub

    }
}
