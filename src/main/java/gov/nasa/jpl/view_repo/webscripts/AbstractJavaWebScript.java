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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
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
    public enum LogLevel {
		DEBUG(0), INFO(1), WARNING(2), ERROR(3);
		private int value;
		private LogLevel(int value) {
			this.value = value;
		}
	}
	
	// injected members
	protected ServiceRegistry services;		// get any of the Alfresco services
	protected Repository repository;		// used for lucene search
	protected LogLevel logLevel = LogLevel.WARNING;
	
	// internal members
	protected ScriptNode companyhome;
	protected Map<String, EmsScriptNode> foundElements = new HashMap<String, EmsScriptNode>();

	// needed for Lucene search
	protected static final StoreRef SEARCH_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    protected Status responseStatus = new Status();
    
    protected void initMemberVariables(String siteName) {
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}

    public AbstractJavaWebScript(Repository repositoryHelper, ServiceRegistry registry) {
        this.setRepositoryHelper(repositoryHelper);
        this.setServices(registry);
    }
 
    public AbstractJavaWebScript() {
        // default constructor for spring
        super();
    }
    
	/**
	 * Utility for clearing out caches
	 * TODO: do we need to clear caches if Spring isn't making singleton instances
	 */
	protected void clearCaches() {
		foundElements = new HashMap<String, EmsScriptNode>();
		response = new StringBuffer();
		responseStatus.setCode(HttpServletResponse.SC_OK);
	}
	
	/**
	 * Parse the request and do validation checks on request
	 * TODO: Investigate whether or not to deprecate and/or remove
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
	    return NodeUtil.getSiteNode( siteName, services, response );
//		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
//		if (siteInfo != null) {
//			return new EmsScriptNode(siteInfo.getNodeRef(), services, response);
//		}
//		return null;
	}

	
	/**
	 * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
	 * This does caching of found elements so they don't need to be looked up with a different API each time.
	 * 
	 * TODO extend so search context can be specified
	 * @param id	Node id to search for
	 * @return		ScriptNode with name if found, null otherwise
	 */
	protected EmsScriptNode findScriptNodeById(String id) {
//		long start=System.currentTimeMillis();
		EmsScriptNode result = null;

		// be smart about search if possible
		if (foundElements.containsKey(id)) {
			result = foundElements.get(id);
//		} else if (name.endsWith("_pkg")) {
//			String elementName = name.replace("_pkg", "");
//			EmsScriptNode elementNode = findScriptNodeByName(elementName);
//			if (elementNode != null) {
//			    result = elementNode.getParent().childByNamePath(name);
//			}
		} else {
			NodeRef nodeRef = findNodeRefById(id);
			if (nodeRef != null) {
				result = new EmsScriptNode(nodeRef, services, response);
				foundElements.put(id, result); // add to cache
			}
		}
		
//		long end=System.currentTimeMillis(); System.out.println("\tfindScriptNodeByName " + name + ": " + (end-start) + " ms");
		return result;
	}

	protected NodeRef findNodeRefByType(String name, String type) {
	    return NodeUtil.findNodeRefByType( name, type, services );
	}
	
	/**
	 * Find a NodeReference by name (returns first match, assuming things are unique)
	 * 
	 * @param name Node name to search for
	 * @return     NodeRef of first match, null otherwise
	 */
	protected NodeRef findNodeRefById(String name) {
	    return NodeUtil.findNodeRefById( name, services );
	}
	
	protected void log(LogLevel level, String msg, int code) {
		if (level.value >= logLevel.value) {
			log("[" + level.name() + "]: " + msg + "\n", code);
		}
	}
	
	protected void log(LogLevel level, String msg) {
	    if (level.value >= logLevel.value) {
	        log("[" + level.name() + "]: " + msg);
	    }
        System.out.println(msg);
	}
	
	protected void log(String msg, int code) {
		response.append(msg);
		responseStatus.setCode(code);
		responseStatus.setMessage(msg);
	}
	
	protected void log(String msg) {
	    response.append(msg + "\n");
	}
	
	/**
	 * Checks whether user has permissions to the node and logs results and status as appropriate
	 * @param node         EmsScriptNode to check permissions on
	 * @param permissions  Permissions to check
	 * @return             true if user has specified permissions to node, false otherwise
	 */
	protected boolean checkPermissions(EmsScriptNode node, String permissions) {
	    return node.checkPermissions( permissions, response, responseStatus );
	}

	/**
	 * Checks whether user has permissions to the nodeRef and logs results and status as appropriate
	 * @param nodeRef      NodeRef to check permissions againts
	 * @param permissions  Permissions to check
	 * @return             true if user has specified permissions to node, false otherwise
	 */
	protected boolean checkPermissions(NodeRef nodeRef, String permissions) {
		if (services.getPermissionService().hasPermission(nodeRef, permissions) != AccessStatus.ALLOWED) {
			log(LogLevel.WARNING, "No " + permissions + " priveleges to " + nodeRef.toString() + ".\n", HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		return true;
	}

	
	protected static final String PROJECT_ID = "projectId";
    protected static final String SITE_NAME = "siteName";
    
    protected boolean checkRequestContent(WebScriptRequest req) {
        if (req.getContent() == null) {
            log(LogLevel.ERROR, "No content provided.\n", HttpServletResponse.SC_NO_CONTENT);
            return false;
        }
        return true;
    }
    

	protected boolean checkRequestVariable(Object value, String type) {
		if (value == null) {
			log(LogLevel.ERROR, type + " not found.\n", HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		return true;
	}

	/**
	 * Perform Lucene search for the specified pattern and ACM type
	 * TODO: Scope Lucene search by adding either parent or path context
	 * @param type		escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
	 * @param pattern   Pattern to look for
	 */
	protected Map<String, EmsScriptNode> searchForElements(String type, String pattern) {
		Map<String, EmsScriptNode> searchResults = new HashMap<String, EmsScriptNode>();

        searchResults.putAll( NodeUtil.searchForElements( type, pattern, services, response,
                                                          responseStatus ) );
        //foundElements.putAll(searchResults);
//		if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
//			ResultSet resultSet = null;
//			try {
//				pattern = type + pattern + "\"";
//				resultSet = services.getSearchService().query(SEARCH_STORE, SearchService.LANGUAGE_LUCENE, pattern);
//				for (ResultSetRow row: resultSet) {
//					EmsScriptNode node = new EmsScriptNode(row.getNodeRef(), services, response);
//					if (checkPermissions(node, PermissionService.READ)) {
//    					String id = (String) node.getProperty(Acm.ACM_ID);
//        					if (id != null) {
//        						searchResults.put(id, node);
//        					}
//					}
//				}
//			} catch (Exception e) {
//				log(LogLevel.ERROR, "Could not parse search: " + pattern + ".\n", HttpServletResponse.SC_BAD_REQUEST);  
//			} finally {
//				if (resultSet != null) {
//					resultSet.close();
//				}
//			}
//		}

		return searchResults;
	}

	/**
     * Helper utility to check the value of a request parameter
     * 
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param value
     *            String of the value the parameter is being checked for
     * @return True if parameter is equal to value, False otherwise
     */
    public static boolean checkArgEquals(WebScriptRequest req, String name,
            String value) {
        if (req.getParameter(name) == null) {
            return false;
        }
        return req.getParameter(name).equals(value);
    }

    public StringBuffer getResponse() {
        return response;
    }

    public Status getResponseStatus() {
        return responseStatus;
    }
    
    public void setLogLevel(LogLevel level) {
        logLevel = level;
    }
    
    public void appendResponseStatusInfo(AbstractJavaWebScript instance) {
        response.append(instance.getResponse());
        responseStatus.setCode(instance.getResponseStatus().getCode());
    }
}