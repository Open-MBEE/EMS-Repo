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

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
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
	protected Map<String, EmsScriptNode> foundElements = new HashMap<String, EmsScriptNode>();

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
	
	// Alfresco Content Model 2 JSON types
	protected final Map<String, String> acm2json = new HashMap<String, String>() {
		private static final long serialVersionUID = -4682311676740055702L;
		{
			// TODO update to use new sysmlModel.xml types
			put("view:View", "View");
			put("view:Property", "Property");
			put("view:Comment", "Comment");
			put("view:ModelElement", "ModelElement");
			
			put("sysml:Package", "Package");
			put("sysml:Property", "Property");
			put("sysml:Element", "Element");
			put("sysml:Dependency", "Dependency");
			put("sysml:Generalization", "Generalization");
			put("sysml:DirectedRelationship", "DirectedRelationship");
			put("sysml:Conform", "Conform");
			put("sysml:Expose", "Expose");
			put("sysml:Viewpoint", "Viewpoint");
			put("sysml:name", "name");
			put("sysml:documentation", "documentation");
			put("sysml:isDerived", "isDerived");
			put("sysml:isSlot", "isSlot");
			put("sysml:boolean", "boolean");
			put("sysml:string", "string");
			put("sysml:integer", "integer");
			put("sysml:double", "double");
			put("sysml:expression", "expression");
			put("sysml:valueType", "valueType");
			
			put("cm:modified", "lastModified");
			put("cm:modifier", "author");
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

	protected void clearCaches() {
		foundElements = new HashMap<String, EmsScriptNode>();
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
	protected EmsScriptNode getSiteNode(String siteName) {
		return new EmsScriptNode(services.getSiteService().getSite(siteName).getNodeRef(), services, response);
	}

	
	/**
	 * Find node of specified name (returns first found - so assume uniquely named ids
	 * TODO extend so context can be specified
	 * @param name	Node name to search for
	 * @return		ScriptNode with name if found, null otherwise
	 */
	protected EmsScriptNode findScriptNodeByName(String name) {
//		long start=System.currentTimeMillis();
		EmsScriptNode result = null;

		// be smart about search if possible
		if (foundElements.containsKey(name)) {
			result = foundElements.get(name);
		} else if (name.endsWith("_pkg")) {
			String elementName = name.replace("_pkg", "");
			EmsScriptNode elementNode = findScriptNodeByName(elementName);
			result = elementNode.getParent().childByNamePath(name);
		} else {
			NodeRef nodeRef = findNodeRefByName(name);
			if (nodeRef != null) {
				result = new EmsScriptNode(nodeRef, services, response);
				foundElements.put(name, result); // add to cache
			}
		}
		
//		long end=System.currentTimeMillis(); System.out.println("\tfindScriptNodeByName " + name + ": " + (end-start) + " ms");
		return result;
	}
	
	protected NodeRef findNodeRefByName(String name) {
		ResultSet results = null;
		NodeRef node = null;
		try {
			results = services.getSearchService().query(SEARCH_STORE, SearchService.LANGUAGE_LUCENE, "@cm\\:name:\"" + name + "\"");
			for (ResultSetRow row: results) {
				node = row.getNodeRef();
				break ; //Assumption is things are uniquely named - TODO: fix since snapshots have same name?...
			}
		} finally {
			if (results != null) {
				results.close();
			}
		}

		return node;		
	}
		
}