package gov.nasa.jpl.view_repo.webscripts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Utility class for handling transactions, node creation, node get, etc.
 * 
 * @author cinyoung
 *
 */
public class JwsUtil {
	// injected
	private Boolean useFoundationalApi = true;
	private int transactionInterval = 200;
	protected ServiceRegistry services = null;

	// internal
	protected final static String NAMESPACE_BEGIN = "" + QName.NAMESPACE_BEGIN;

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
	protected boolean checkArgEquals(WebScriptRequest req, String name,
			String value) {
		if (req.getParameter(name) == null) {
			return false;
		}
		return req.getParameter(name).equals(value);
	}

	/**
	 * Create a model element under specified parent
	 * 
	 * @param parent
	 *            Parent container
	 * @param name
	 *            Name of element to create
	 * @param type
	 *            Type of element to create (short form of name, e.g.,
	 *            view:mdid)
	 * @return Created model element
	 */
	protected ScriptNode createModelElement(ScriptNode parent, String name,
			String type) {
		if (useFoundationalApi) {
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(
					1, 1.0f);
			// don't forget to set the name
			props.put(ContentModel.PROP_NAME, name);

			ChildAssociationRef assoc = services.getNodeService().createNode(
					parent.getNodeRef(),
					ContentModel.ASSOC_CONTAINS,
					QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
							QName.createValidLocalName(name)),
					createQName(type), props);
			return new ScriptNode(assoc.getChildRef(), services);
		} else {
			return parent.createNode(name, type);
		}
	}

	/**
	 * Helper to create a QName from either a fully qualified or short-name
	 * QName string (taken from ScriptNode)
	 * 
	 * @param s
	 *            Fully qualified or short-name QName string
	 * 
	 * @return QName
	 */
	public QName createQName(String s) {
		QName qname;
		if (s.indexOf(NAMESPACE_BEGIN) != -1) {
			qname = QName.createQName(s);
		} else {
			qname = QName.createQName(s, this.services.getNamespaceService());
		}
		return qname;
	}

	/**
	 * Wrapper for additional behavior to be added to get an element of
	 * specified name in a particular package
	 * 
	 * @param parent
	 *            Container package to look for element
	 * @param name
	 *            Name of the element to find
	 * @return ScriptNode of found element, null otherwise
	 */
	protected ScriptNode getModelElement(ScriptNode parent, String name) {
		return parent.childByNamePath(name);
	}

	/**
	 * Wrapper get NodeRef's property using Foundational API or ScriptNode API
	 * 
	 * @param node
	 *            Node to get property for
	 * @param key
	 *            Short type for the property to get (e.g., "view:mdid")
	 * @return Node's specified property value
	 */
	protected Object getNodeProperty(ScriptNode node, String key) {
		if (useFoundationalApi) {
			return services.getNodeService().getProperty(node.getNodeRef(),
					createQName(key));
		} else {
			return node.getProperties().get(key);
		}
	}

	protected void setName(ScriptNode node, String name) {
		setNodeProperty(node, "view:name", name);
		setNodeProperty(node, "cm:title", name);
	}


	/**
	 * Wrapper set NodeRef's property using Foundational API or ScriptNode API
	 * 
	 * ScriptNode has some odd null pointer exceptions when dealing with
	 * properties
	 * 
	 * @param node
	 *            Node to set property for
	 * @param type
	 *            Short type for the property to set (e.g., "view:mdid")
	 * @param value
	 *            Serializable value to set the property to
	 */
	protected void setNodeProperty(ScriptNode node, String type,
			Serializable value) {
		if (useFoundationalApi) {
			services.getNodeService().setProperty(node.getNodeRef(),
					createQName(type), value);
		} else {
			node.getProperties().put(type, value);
			node.save();
		}
	}

	public void setServices(ServiceRegistry sr) {
		services = sr;
	}

	public void setUseFoundationalApi(Boolean option) {
		useFoundationalApi = option;
	}
	
	public void setTransactionInterval(int transactionInterval) {
		this.transactionInterval = transactionInterval;
	}

	protected void doTransaction(final JwsFunctor functor, final JSONArray jsonArray, final int start,
			final int range, final Boolean... flags) {
		TransactionService transactionService = services
				.getTransactionService();

		RetryingTransactionCallback<Object> work = new RetryingTransactionCallback<Object>() {
			@Override
			public Object execute() throws Throwable {
				int max = start + range > jsonArray.length() ? jsonArray.length() : start + range;
				for (int ii = start; ii < max; ii++) {
					functor.execute(jsonArray, ii, flags);
				}
				return null;
			}
		};
		transactionService.getRetryingTransactionHelper().doInTransaction(work);
	}
	
	public void splitTransactions(JwsFunctor functor, JSONArray jsonArray, Boolean... flags) {
		int max = (int) Math.ceil((double) jsonArray.length()/transactionInterval);
		for (int ii = 0; ii < max; ii++) {
			doTransaction(functor, jsonArray, ii*transactionInterval, transactionInterval, flags);
		}
	}
	
}
