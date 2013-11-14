package gov.nasa.jpl.view_repo.webscripts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.DeclarativeWebScript;

/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. Much of the helpers are inspired from
 * ScriptNode.java.
 * 
 * @author cinyoung
 * 
 */
public abstract class AbstractWebScript extends DeclarativeWebScript {
	// injected members
	protected ServiceRegistry services;
	protected Repository repository;

	// internal members
	protected final static String NAMESPACE_BEGIN = "" + QName.NAMESPACE_BEGIN;
	protected NodeService nodeService;
	protected ScriptNode companyhome = null;

	protected static Map<String,String> param2TypeMap = new HashMap<String,String>() {
		{
			put("doc", "view:DocumentView");
			put("product", "view:product");
			put("view2view", "view:view2viewJson");
			put("noSection", "view:noSectionsJson");
		};
	};
	
    

	
	protected void initMemberVariables(String siteName) {
		this.nodeService = services.getNodeService();
		companyhome = new ScriptNode(repository.getCompanyHome(), services);
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}


	
	protected ScriptNode createModelElement(ScriptNode parent, String name, String type) {
		return parent.createNode(name, type);
	}

}
