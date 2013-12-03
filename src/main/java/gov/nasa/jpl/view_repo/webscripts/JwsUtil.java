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
 * TODO: This may not be thread safe, may need to have a new instance for each transaction
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

	/**
	 * Wraps the execution of the functor in a transaction for the specified number of data values
	 * @param functor		Function callback to execute on data
	 * @param jsonArray		Input data
	 * @param start			Start index into jsonArray
	 * @param range			Range of data to extract from jsonArray
	 * @param flags			Boolean flags passed into functor
	 */
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
	
	/**
	 * Splits the transactions on a JSONArray of input data
	 * @param functor		The function pointer to call on the JSONArray entries
	 * @param jsonArray		The input data to call the functor on
	 * @param flags			Any boolean flags needed for the functor
	 */
	public void splitTransactions(JwsFunctor functor, JSONArray jsonArray, Boolean... flags) {
		int max = (int) Math.ceil((double) jsonArray.length()/transactionInterval);
		for (int ii = 0; ii < max; ii++) {
			doTransaction(functor, jsonArray, ii*transactionInterval, transactionInterval, flags);
		}
	}
	
}
