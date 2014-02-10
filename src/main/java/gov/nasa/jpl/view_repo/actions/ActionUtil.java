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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

/**
 * Static class of action utilities
 * @author cinyoung
 *
 */
public class ActionUtil {
    // defeat instantiation
    private ActionUtil() {
        // do nothing
    }
    
    public static void sendEmailToModifier(EmsScriptNode node, String msg, String subject, ServiceRegistry services, StringBuffer response) {
        String username = (String)node.getProperty("cm:modifier");
        EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username), services, response);
        String recipient = (String) user.getProperty("cm:email");
        
        sendEmailTo("europaems@jpl.nasa.gov", recipient, msg, subject, services);
    }
    
    public static void sendEmailTo(String sender, String recipient, String msg, String subject, ServiceRegistry services) {
        Action mailAction = services.getActionService().createAction(MailActionExecuter.NAME);
        mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, subject);
        mailAction.setParameterValue(MailActionExecuter.PARAM_TO, recipient);
        mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, sender);
        mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, msg);
        services.getActionService().executeAction(mailAction, null);

    }
    
    public static EmsScriptNode saveLogToFile(EmsScriptNode node, String mimeType, ServiceRegistry services, StringBuffer response) {
        String logName = ((String) node.getProperty("cm:name")) + ".log";
        EmsScriptNode logNode = node.getParent().childByNamePath(logName);
        ContentWriter writer = services.getContentService().getWriter(logNode.getNodeRef(), ContentModel.PROP_CONTENT, true);
        writer.putContent(response.toString());
        setContentDataMimeType(writer, logNode, mimeType, services);
        return logNode;
    }
    
    public static void setContentDataMimeType(ContentWriter writer, EmsScriptNode node, String mimetype, ServiceRegistry sr) {
        ContentData contentData = writer.getContentData();
        contentData = ContentData.setMimetype(contentData, mimetype);
        sr.getNodeService().setProperty(node.getNodeRef(), ContentModel.PROP_CONTENT, contentData);
    }
}
