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

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Handle the creation of configuration sets for a particular site
 *
 * @author cinyoung
 *
 */
public class CommitPost extends AbstractJavaWebScript {
	private static final String COMMIT_ID = "commitId";

	public CommitPost() {
		super();
	}

	public CommitPost(Repository repositoryHelper,
			ServiceRegistry registry) {
		super(repositoryHelper, registry);
	}

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// do nothing
		return false;
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		CommitPost instance = new CommitPost(repository, getServices());
		return instance.executeImplImpl(req, status, cache, runWithoutTransactions);
	}

	@Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
			Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		clearCaches();

		// Isn't necessary since we have the commit ID
        //FIXME: need to add revert capabilities back in
//        String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        String nodeId = req.getServiceMatch().getTemplateVars().get(COMMIT_ID);

//        EmsScriptNode changeSet = CommitUtil.getScriptNodeByNodeRefId(nodeId, services);
//        if (changeSet != null) {
//        		if (CommitUtil.revertCommit(changeSet, services)) {
//        			responseStatus.setCode(HttpServletResponse.SC_OK);
//        			response.append("Okay");
//        		} else {
//        			responseStatus.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//        			response.append("Could not revert");
//        		}
//        } else {
//        		responseStatus.setCode(HttpServletResponse.SC_NOT_FOUND);
//        		response.append("Could not find change set to revert");
//        }

		status.setCode(responseStatus.getCode());
		model.put("res", response.toString());
		return model;
	}
}
