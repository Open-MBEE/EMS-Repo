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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.springframework.extensions.webscripts.Status;

/**
 * Static class of Action utilities for saving log, sending email, etc.
 * @author cinyoung
 *
 */
public class ActionUtil {
	private static String hostname = null;
	
    // defeat instantiation
    private ActionUtil() {
        // do nothing
    }
    
    /**
     * Send off an email to the modifier of the node
     * @param node      Node whose modifier should be sent an email
     * @param msg       Message to send modifier
     * @param subject   Subjecto of message
     * @param services
     * @param response
     */
    public static void sendEmailToModifier(EmsScriptNode node, String msg, String subject, ServiceRegistry services, StringBuffer response) {
        String username = (String)node.getProperty("cm:modifier");
        EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username), services, response);
        String recipient = (String) user.getProperty("cm:email");
        
        sendEmailTo("europaems@jpl.nasa.gov", recipient, msg, subject, services);
    }
    
    /**
     * Send email to recipient
     * @param sender
     * @param recipient
     * @param msg
     * @param subject
     * @param services
     */
    public static void sendEmailTo(String sender, String recipient, String msg, String subject, ServiceRegistry services) {
        try {
            Action mailAction = services.getActionService().createAction(MailActionExecuter.NAME);
            mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, subject);
            mailAction.setParameterValue(MailActionExecuter.PARAM_TO, recipient);
            mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, sender);
            mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, msg);
            services.getActionService().executeAction(mailAction, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Saves log file for the specified node (creates the log file if necessary)
     * @param node      Node to save file to
     * @param mimeType  Mimetype of file
     * @param services  
     * @param data      Stringbuffer to save
     * @return  Log file
     */
    public static EmsScriptNode saveLogToFile(EmsScriptNode node, String mimeType, ServiceRegistry services, String data) {
        String logName = ((String) node.getProperty("cm:name")) + ".log";
        
        // create logNode if necessary
        EmsScriptNode logNode = node.getParent().childByNamePath(logName);
        if (logNode == null) {
            logNode = node.getParent().createNode(logName, "cm:content");
            if (!logNode.hasAspect("cm:versionable")) {
                logNode.addAspect("cm:versionable");
            }
            // TODO: check if node for log is of type Job
            node.createOrUpdateProperty("ems:job_log", logNode);
        }

        saveStringToFile(logNode, mimeType, services, data);
        return logNode;
    }
    
    public static void saveStringToFile(EmsScriptNode node, String mimeType, ServiceRegistry services, String data) {
        ContentWriter writer = services.getContentService().getWriter(node.getNodeRef(), ContentModel.PROP_CONTENT, true);
        writer.putContent(data.toString());
        setContentDataMimeType(writer, node, mimeType, services);
    }
    
    /**
     * Set the content mimetype so Alfresco knows how to deliver the HTTP response header correctly
     * @param writer    
     * @param node
     * @param mimetype
     * @param sr
     */
    public static void setContentDataMimeType(ContentWriter writer, EmsScriptNode node, String mimetype, ServiceRegistry sr) {
        ContentData contentData = writer.getContentData();
        contentData = ContentData.setMimetype(contentData, mimetype);
        sr.getNodeService().setProperty(node.getNodeRef(), ContentModel.PROP_CONTENT, contentData);
    }
    
    /**
     * Create a job inside a particular site
     * @return The created job node
     */
    public static EmsScriptNode getOrCreateJob(EmsScriptNode siteNode, String jobName, String jobType, Status status, StringBuffer response) {
        EmsScriptNode jobPkgNode = siteNode.childByNamePath("Jobs");
        if (jobPkgNode == null) {
            jobPkgNode = siteNode.createFolder("Jobs", "cm:folder");
        }
        
        EmsScriptNode jobNode = jobPkgNode.childByNamePath(jobName);
        if (jobNode == null) {
            jobNode = jobPkgNode.createNode(jobName, jobType);
            jobNode.createOrUpdateProperty("cm:isContentIndexed", false);
        } else {
            String jobStatus = (String)jobNode.getProperty("ems:job_status");
            if (jobStatus.equals("Active")) {
                status.setCode(HttpServletResponse.SC_CONFLICT, "Previous job is still active.");
                return null;
            }
        }
        jobNode.createOrUpdateProperty("ems:job_status", "Active");

        return jobNode;
    }
    
    public static void setJobStatus(EmsScriptNode jobNode, String value) {
        jobNode.createOrUpdateProperty("ems:job_status", value);
    }
    
    public static String getHostName() {
    		if (hostname == null) {
		    	Process tr;
			try {
				tr = Runtime.getRuntime().exec( new String[]{ "hostname" } );
			    	BufferedReader rd = new BufferedReader( new InputStreamReader( tr.getInputStream() ) );
			    	hostname = rd.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		}
	    return hostname;
    }
}
