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

package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * This class isn't used for anything currently
 * @author cinyoung
 *
 */
public class SnapshotGet extends AbstractJavaWebScript {
    public SnapshotGet() {
        super();
    }
    
    public SnapshotGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return true;
    }

    @Override
    protected void clearCaches() {
        super.clearCaches();
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        clearCaches();

        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );
        
        String id = req.getServiceMatch().getTemplateVars().get("id");
        Map<String, Object> model = new HashMap<String, Object>();

        EmsScriptNode snapshot = findScriptNodeById(id, workspace, dateTime, false);
        if (snapshot != null) {
            String snapshotString = getSnapshotString(snapshot);
            Date date = (Date)snapshot.getLastModified( dateTime );
            model.put("res", snapshotString);
            model.put("title", "Snapshot (" + EmsScriptNode.getIsoTime(date) + ")");
            model.put("tag", getConfigurationSet(snapshot, workspace, dateTime));
            model.put("siteName", snapshot.getSiteName());
            model.put("siteTitle", snapshot.getSiteTitle());
        } else {
            log(LogLevel.ERROR, "Snapshot not found for id: " + id, HttpServletResponse.SC_NOT_FOUND);
            model.put("res", response.toString());
            model.put("title", "ERROR no snapshot found");
            model.put("tag", "");
            model.put("siteName", "");
            model.put("siteTitle", "ERROR snapshot not found");
        }
        model.put("id", id.substring(0, id.lastIndexOf("_")));

        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }

    /**
     * Load the snapshot from the saved JSON file and return as string
     * @param snapshotId
     * @return
     */
    private String getSnapshotString(EmsScriptNode snapshot) {
        ContentReader reader = services.getContentService().getReader(snapshot.getNodeRef(), ContentModel.PROP_CONTENT);
        return reader.getContentString();
    }
    
    
    /**
     * Get the configuration set name associated with the snapshot, if available
     * @param dateTime 
     * @param snapshotId
     * @return
     */
    public static String getConfigurationSet( EmsScriptNode snapshot,
                                              WorkspaceNode workspace,
                                              Date dateTime ) {
		if (snapshot != null) {
            List< EmsScriptNode > configurationSets =
                    snapshot.getSourceAssocsNodesByType( "ems:configuredSnapshots",
                                                         workspace, null );
			if (!configurationSets.isEmpty()) {
				EmsScriptNode configurationSet = configurationSets.get(0);
				String configurationSetName = (String) configurationSet.getProperty(Acm.CM_NAME);
				return configurationSetName;
			}
     	}

		return "";
    }
}
