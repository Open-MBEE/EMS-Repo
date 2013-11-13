package gov.nasa.jpl.view_repo.webscripts;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
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
	protected NodeRef companyhome = null;

	protected void initMemberVariables() {
		this.nodeService = services.getNodeService();
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repository = repositoryHelper;
	}

	public void setServices(ServiceRegistry registry) {
		this.services = registry;
	}

	/**
	 * Helper to create a QName from either a fully qualified or short-name
	 * QName string
	 * 
	 * @param s
	 *            Fully qualified or short-name QName string
	 * 
	 * @return QName
	 */
	protected QName createQName(String s) {
		QName qname;
		if (s.indexOf(NAMESPACE_BEGIN) != -1) {
			qname = QName.createQName(s);
		} else {
			qname = QName.createQName(s, this.services.getNamespaceService());
		}
		return qname;
	}

	/**
	 * Search from company home for the specified fully qualified path
	 * 
	 * @param path
	 * @return
	 */
	protected NodeRef getNodeByPath(String path) {
		if (companyhome == null) {
			companyhome = repository.getCompanyHome();
		}
		
		return getModelElement(companyhome, path);
	}

	/**
	 * 
	 * @param parent
	 * @param name
	 * @return
	 */
	protected NodeRef getModelElement(NodeRef parent, String name) {
		String tokens[] = name.split("/");
		NodeRef parentRef, childRef = null;
		
		parentRef = parent;
		for (int ii = 0; ii < tokens.length; ii++) {
			childRef = nodeService.getChildByName(parentRef, ContentModel.ASSOC_CONTAINS, tokens[ii]);
			parentRef = childRef;
		}

		return childRef;
	}

	/**
	 * 
	 * @param parent
	 * @param name
	 * @param type
	 */
	protected NodeRef createModelElement(NodeRef parent, String name,
			String type, Map<QName, Serializable> props) {
		ChildAssociationRef assoc = nodeService.createNode(
				parent,
				ContentModel.ASSOC_CONTAINS,
				QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
						QName.createValidLocalName(name)), createQName(type), props);
		return assoc.getChildRef();
	}
}
