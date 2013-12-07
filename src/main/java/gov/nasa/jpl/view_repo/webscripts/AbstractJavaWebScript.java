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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities
 * that were in utils.js
 * 
 * @author cinyoung
 * 
 */
public abstract class AbstractJavaWebScript extends DeclarativeWebScript {
	// injected members
	protected ServiceRegistry services;
	protected Repository repository;
	protected JwsUtil jwsUtil;

	// internal members
	protected ScriptNode companyhome;
    
	// needed for Lucene search
	protected static final StoreRef SEARCH_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    
	// JSON to Alfresco Content Model mapping
	protected final Map<String, String> json2acm = new HashMap<String, String>() {
		private static final long serialVersionUID = -5467934440503910163L;
		{
			// TODO update to use new sysmlModel.xml types
			put("View", "view:View");
			put("Property", "view:Property");
			put("Comment", "view:Comment");
			put("ModelElement", "view:ModelElement");
			
			put("Package", "sysml:Package");
			put("Property", "sysml:Property");
			put("Element", "sysml:Element");
			put("Dependency", "sysml:Dependency");
			put("Generalization", "sysml:Generalization");
			put("DirectedRelationship", "sysml:DirectedRelationship");
			put("Conform", "sysml:Conform");
			put("Expose", "sysml:Expose");
			put("Viewpoint", "sysml:Viewpoint");
			put("name", "sysml:name");
			put("documentation", "sysml:documentation");
			put("isDerived", "sysml:isDerived");
			put("isSlot", "sysml:isSlot");
			put("boolean", "sysml:boolean");
			put("string", "sysml:string");
			put("integer", "sysml:integer");
			put("double", "sysml:double");
			put("expression", "sysml:expression");
			put("valueType", "sysml:valueType");
		}
	};

	protected void initMemberVariables(String siteName) {
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}

	public void setJwsUtil(JwsUtil util) {
		jwsUtil = util;
	}

	/**
	 * Clear all parent volume associations
	 * 
	 * TODO cleanup?
	 * 
	 * @param dnode
	 */
	protected void cleanDocument(ScriptNode dnode) {
		JSONArray pvs = (JSONArray) dnode.getSourceAssocs().get(
				"view:documents");
		if (pvs != null) {
			for (int ii = 0; ii < pvs.length(); ii++) {
				// TODO: convert pv to ScriptNode to remove?
			}
		}
	}

	/**
	 * Parse the request and do validation checks on request
	 * 
	 * @param req		Request to be parsed
	 * @param status	The status to be returned for the request
	 * @return			true if request valid and parsed, false otherwise
	 */
	abstract protected boolean validateRequest(WebScriptRequest req, Status status);
	
	
	/**
	 * Get site of specified short name
	 * @param siteName
	 * @return	ScriptNode of site with name siteName
	 */
	protected ScriptNode getSiteNode(String siteName) {
		return new ScriptNode(services.getSiteService().getSite(siteName).getNodeRef(), services);
	}

	
	/**
	 * Find node of specified name (returns first found - so assume uniquely named ids
	 * TODO extend so context can be specified
	 * @param name	Node name to search for
	 * @return		ScriptNode with name if found, null otherwise
	 */
	protected ScriptNode findNodeWithName(String name) {
		ResultSet results = null;
		ScriptNode node = null;
		try {
			results = services.getSearchService().query(SEARCH_STORE, SearchService.LANGUAGE_LUCENE, "@cm\\:name:\"" + name + "\"");
			for (ResultSetRow row: results) {
				node = new ScriptNode(row.getNodeRef(), services);
				break ; //Assumption is things are uniquely named - TODO: fix since snapshots have same name?...
			}
		} finally {
			if (results != null) {
				results.close();
			}
		}

		return node;
	}
	
	
	/**
	 * Check whether or not a node has the specified aspect, add it if not
	 * @param node		Node to check
	 * @param string	Short name (e.g., sysml:View) of the aspect to look for
	 * @return			true if node updated with aspect
	 */
	protected boolean checkAndUpdateAspect(ScriptNode node, String aspect) {
		if (!node.hasAspect(aspect)) {
			return node.addAspect(aspect);
		}
		return false;
	}

	/**
	 * Check whether or not a node has a property, update or create as necessary
	 * 
	 * NOTE: this only works for non-collection properties - for collections handwrite (or see how it's done in ModelPost.java)
	 * @param node		Node to update property for
	 * @param value		Value to set property to
	 * @param acmType	Short name for the Alfresco Content Model type
	 * @return			true if property updated, false otherwise (e.g., value did not change)
	 */
	protected <T extends Serializable> boolean checkAndUpdateProperty(ScriptNode node, T value, String acmType) {
		@SuppressWarnings("unchecked")
		T oldValue = (T) jwsUtil.getNodeProperty(node, acmType);
		if (oldValue != null) {
			if (!value.equals(oldValue)) {
				jwsUtil.setNodeProperty(node, acmType, value);
				return true;
			}
		}
		return false;
	}
	

	/**
	 * Create a child association between a parent and child node of the specified type
	 * 
	 * ScriptNode unfortunately doesn't provide this capability
	 * 
	 * NOTE: do not use for peer associations
	 * @param parent	Parent node
	 * @param child		Child node
	 * @param type		Short name of the type of child association to create
	 * @return			The created ChildAssociationRef
	 */
	protected ChildAssociationRef createChildAssociation(ScriptNode parent, ScriptNode child, String type) {
		QName qname = jwsUtil.createQName(type);
		return services.getNodeService().addChild(parent.getNodeRef(), child.getNodeRef(), qname, qname);
	}
	
	/**
	 * Check whether an association exists of the specified type between source and target, create/update as necessary
	 * 
	 * NOTE: do not use for child associations
	 * @param source	Source node of the association
	 * @param target	Target node of the association
	 * @param type		Short name of the type of association to create 
	 * @return			true if association updated or created
	 */
	protected boolean checkAndUpdateAssociation(ScriptNode source, ScriptNode target, String type) {
		List<AssociationRef> refs = services.getNodeService().getTargetAssocs(source.getNodeRef(), RegexQNamePattern.MATCH_ALL );
		QName assocTypeQName = jwsUtil.createQName(type);

		// check all associations to see if there's a matching association
		for (AssociationRef ref: refs) {
			if (ref.getTypeQName().equals(assocTypeQName)) {
				if (ref.getSourceRef().equals(source.getNodeRef()) && 
						ref.getTargetRef().equals(target.getNodeRef())) {
					// found it, no need to update
					return false; 
				} else {
					// association doesn't match, no way to modify a ref, so need to remove then create
					services.getNodeService().removeAssociation(source.getNodeRef(), target.getNodeRef(), assocTypeQName);
					break;
				}
			}
		}
		
		services.getNodeService().createAssociation(source.getNodeRef(), target.getNodeRef(), assocTypeQName);
		return true;
	}
}