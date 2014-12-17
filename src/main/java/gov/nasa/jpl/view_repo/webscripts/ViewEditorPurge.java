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

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.log4j.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Web service to purge a project recursively with each hard delete within its own
 * transaction.
 * 
 * @author cinyoung
 *
 */
public class ViewEditorPurge extends AbstractJavaWebScript {
 	public ViewEditorPurge() {
 	    super();
 	}
    
    public ViewEditorPurge(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    private EmsScriptNode siteNode = null;
	private String siteName;
	
	@Override
	protected void clearCaches() {
		super.clearCaches();
	}

	
	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		siteName = req.getServiceMatch().getTemplateVars().get("id");
		
		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
		if (siteInfo != null) {
		    siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
		} else {
			log(Level.ERROR, "Site not found: " + siteName + ".\n", HttpServletResponse.SC_NOT_FOUND);
			return false;
		}
		
		return true;
	}

	
	/**
	 * Entry point
	 * Leave synchronized as deletes can interfere with one another
	 */
	@Override
	protected synchronized Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
        printHeader( req );

		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();

		boolean recurse = true;
		if (validateRequest(req, status)) {
			EmsScriptNode veNode = null;

	        if (req.getParameter("project") != null) {
	        		String projectid = req.getParameter("project");
	        		if (projectid.startsWith("ViewEditor") || projectid.startsWith("Models")) {
		        		veNode = siteNode.childByNamePath("/" + projectid);
		        		log(Level.INFO, "Attempting to delete dir " + projectid);
	        		}
	        }
	        
	        if (veNode != null) {
	        		handleElementHierarchy(veNode, recurse);
	        } else {
	        		log(Level.INFO, "could not find node to delete");
	        }
		}
		
		model.put("res", "okay");
				
		status.setCode(responseStatus.getCode());

        printFooter();

        return model;
	}


	/**
	 * Deletes a node in a transaction
	 * @param node
	 */
	private void delete(EmsScriptNode node) {
        UserTransaction trx;
        trx = services.getTransactionService().getNonPropagatingUserTransaction();
        try {
            trx.begin();
            String key = (String)node.getProperty("cm:name");
            log(Level.INFO, "delete: beginning transaction {" + node.getNodeRef());
            services.getNodeService().deleteNode(node.getNodeRef());
            log(Level.INFO, "} delete ending transaction: " + key);
            trx.commit();
        } catch (Throwable e) {
            try {
                log(Level.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                trx.rollback();
            } catch (Throwable ee) {
                log(Level.ERROR, "\tRollback failed: " + ee.getMessage());
            }
        }
    }

		
	/**
	 * Build up the element hierarchy from the specified root
	 * @param root		Root node to get children for
	 * @throws JSONException
	 */
	protected void handleElementHierarchy(EmsScriptNode root, boolean recurse) {
		if (recurse) {
			for (ChildAssociationRef assoc: root.getChildAssociationRefs()) {
			    try {
				EmsScriptNode child = new EmsScriptNode(assoc.getChildRef(), services, response);
				handleElementHierarchy(child, recurse);
			    } catch (Exception e) {
			        // do nothing
			    }
			}
		}
		delete(root);
	}	
}
