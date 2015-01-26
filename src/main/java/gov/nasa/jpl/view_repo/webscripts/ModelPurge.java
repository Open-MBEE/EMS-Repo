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
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Web service to hard delete elements recursively, each within their own transaction.
 *
 * @author cinyoung
 *
 */
@Deprecated
public class ModelPurge extends AbstractJavaWebScript {
    public ModelPurge() {
        super();
    }

    public ModelPurge(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    // injected via spring configuration
    protected boolean isViewRequest = false;

	private EmsScriptNode modelRootNode = null;
	private String modelId;

	@Override
	protected void clearCaches() {
		super.clearCaches();
	}


	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		modelId = req.getServiceMatch().getTemplateVars().get("id");
		if (!checkRequestVariable(modelId, "id")) {
			log(LogLevel.ERROR, "Element id not specified.\n", HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}

		WorkspaceNode workspace = getWorkspace( req );

		modelRootNode = findScriptNodeById(modelId, workspace, null, false);
		if (modelRootNode == null) {
			log(LogLevel.ERROR, "Element not found with id: " + modelId + ".\n", HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		return true;
	}


	/**
	 * Entry point
	 *
	 * Make deletion synchronized to simplify checks for conflicting deletes
	 */
	@Override
	protected synchronized Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
        printHeader( req );

		clearCaches();

		Map<String, Object> model = new HashMap<String, Object>();

		boolean recurse = true;
		if (validateRequest(req, status)) {
	        WorkspaceNode workspace = getWorkspace( req );

		    if (services.getDictionaryService().isSubClass(modelRootNode.getQNameType(), ContentModel.TYPE_FOLDER)) {
		        handleElementHierarchy(modelRootNode, workspace, recurse);
		    } else {
		        delete(modelRootNode, workspace);
		        EmsScriptNode pkgNode = findScriptNodeById(modelId + "_pkg", workspace, null, false);
		        handleElementHierarchy(pkgNode, workspace, recurse);
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
	 * @param workspace
	 */
	private void delete(EmsScriptNode node, WorkspaceNode workspace) {
	    if ( node == null || !node.exists() ) {
	        log(LogLevel.ERROR, "Trying to delete a non-existent node! " + node);
	        return;
	    }

	    // don't delete a _pkg node since it will be automatically deleted by its refied component
	    String id = node.getSysmlId();
	    if (id.endsWith("_pkg")) {
	        return;
	    }

        // Add the element to the specified workspace to be deleted from there.
        if ( workspace != null && workspace.exists() && node != null
             && node.exists() && !node.isWorkspace() ) {
	        EmsScriptNode newNodeToDelete = workspace.replicateWithParentFolders( node );
	        node = newNodeToDelete;
	    }

        UserTransaction trx;
        trx = services.getTransactionService().getNonPropagatingUserTransaction();
        try {
            trx.begin();
            NodeUtil.setInsideTransactionNow( true );
            String key = node.getSysmlId();
            log(LogLevel.INFO, "delete: beginning transaction {" + node.getNodeRef());
            node.makeSureNodeRefIsNotFrozen();
            node.transactionCheck();
            services.getNodeService().deleteNode(node.getNodeRef());
            log(LogLevel.INFO, "} delete ending transaction: " + key);
            trx.commit();
            NodeUtil.setInsideTransactionNow( false );
        } catch (Throwable e) {
            try {
                log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                trx.rollback();
                NodeUtil.setInsideTransactionNow( false );
            } catch (Throwable ee) {
                log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
            }
        }
    }


	/**
	 * Build up the element hierarchy from the specified root
	 * @param root		Root node to get children for
	 * @param workspace
	 * @throws JSONException
	 */
    protected void handleElementHierarchy( EmsScriptNode root,
                                           WorkspaceNode workspace,
                                           boolean recurse ) {
		if (recurse) {
			for (ChildAssociationRef assoc: root.getChildAssociationRefs()) {
				EmsScriptNode child = new EmsScriptNode(assoc.getChildRef(), services, response);
				handleElementHierarchy(child, workspace, recurse);
			}
		}
		delete(root, workspace);
	}
}
