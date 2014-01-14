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

package gov.nasa.jpl.view_repo.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

/**
 * Extension of ScriptNode to support EMS needs
 * @author cinyoung
 *
 */
public class EmsScriptNode extends ScriptNode {
	private static final long serialVersionUID = 9132455162871185541L;

	// provide logging capability of what is done
	private StringBuffer response = null;
	
	// provide status as necessary
	private Status status = null;
	
	boolean useFoundationalApi = true;

	// for lucene search
	protected static final StoreRef SEARCH_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services) {
		super(nodeRef, services);
	}


	public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services, StringBuffer response, Status status) {
		this(nodeRef, services);
		setStatus(status);
	}

	public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services, StringBuffer response) {
		this(nodeRef, services);
		setResponse(response);
	}

	
	@Override
	public EmsScriptNode childByNamePath(String path) {
		ScriptNode child = super.childByNamePath(path);
		if (child == null) {
			return null;
		}
		return new EmsScriptNode(child.getNodeRef(), services, response);
	}


	@Override
	public EmsScriptNode createFolder(String name) {
		return new EmsScriptNode(super.createFolder(name).getNodeRef(), services, response, status);
	}


	@Override
	public EmsScriptNode createFolder(String name, String type) {
		return new EmsScriptNode(super.createFolder(name, type).getNodeRef(), services, response, status);
	}


	/**
	 * Check whether or not a node has the specified aspect, add it if not
	 * @param string	Short name (e.g., sysml:View) of the aspect to look for
	 * @return			true if node updated with aspect
	 */
	public boolean createOrUpdateAspect(String aspect) {
		if (!hasAspect(aspect)) {
			log(getName() + ": " + aspect + " aspect added");
			return addAspect(aspect);
		}
		return false;
	}

	
	/**
	 * Check whether an association exists of the specified type between source and target, create/update as necessary
	 * 
	 * NOTE: do not use for child associations
	 * @param target	Target node of the association
	 * @param type		Short name of the type of association to create 
	 * @return			true if association updated or created
	 */
	public boolean createOrUpdateAssociation(ScriptNode target, String type) {
		QName typeQName = createQName(type);
		List<AssociationRef> refs = services.getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL );

		// check all associations to see if there's a matching association
		for (AssociationRef ref: refs) {
			if (ref.getTypeQName().equals(typeQName)) {
				if (ref.getSourceRef() != null && ref.getTargetRef() != null) {
					if (ref.getSourceRef().equals(nodeRef) && 
							ref.getTargetRef().equals(target.getNodeRef())) {
						// found it, no need to update
						return false; 
					}
				}
			
				// association doesn't match, no way to modify a ref, so need to remove then create
				services.getNodeService().removeAssociation(nodeRef, target.getNodeRef(), typeQName);
				break;
			}
		}
		
		// Target nodeRef isn't found?
//		log(getName() + ": " + type + " peer association updated, target: " + target.getName());
		services.getNodeService().createAssociation(nodeRef, target.getNodeRef(), typeQName);
		return true;
	}
	
	
	/**
	 * Create a child association between a parent and child node of the specified type
	 * 
	 * // TODO investigate why alfresco repo deletion of node doesn't remove its reified package
	 * 
	 * NOTE: do not use for peer associations
	 * @param child		Child node
	 * @param type		Short name of the type of child association to create
	 * @return			True if updated or created child relationship
	 */
	public boolean createOrUpdateChildAssociation(ScriptNode child, String type) {
		List<ChildAssociationRef> refs = services.getNodeService().getChildAssocs(nodeRef);
		QName typeQName = createQName(type);

		// check all associations to see if there's a matching association
		for (ChildAssociationRef ref: refs) {
			if (ref.getTypeQName().equals(typeQName)) {
				if (ref.getParentRef().equals(nodeRef) && 
						ref.getChildRef().equals(child.getNodeRef())) {
					// found it, no need to update
					return false; 
				} else {
					services.getNodeService().removeChildAssociation(ref);
					break;
				}
			}
		}

		log(getName() + ": " + type + " added child association to child = " + child.getName());
		services.getNodeService().addChild(nodeRef, child.getNodeRef(), typeQName, typeQName);
		return true;		
	}
	
	
	/**
	 * Check whether or not a node has a property, update or create as necessary
	 * 
	 * NOTE: this only works for non-collection properties - for collections handwrite (or see how it's done in ModelPost.java)
	 * @param acmType	Short name for the Alfresco Content Model type
	 * @param value		Value to set property to
	 * @return			true if property updated, false otherwise (e.g., value did not change)
	 */
	public <T extends Serializable> boolean createOrUpdateProperty(String acmType, T value) {
		@SuppressWarnings("unchecked")
		T oldValue = (T) getProperty(acmType);
		if (oldValue != null) {
			if (!value.equals(oldValue)) {
				setProperty(acmType, value);
				log(getName() + ": " + acmType + " property updated to value = " + value);
				return true;
			}
		} else {
			// TODO create if oldvalue doesn't exist?
			log(getName() + ": " + acmType + " property created with value = " + value);
			setProperty(acmType, value);
		}
		return false;
	}	

	
	/**
	 * Checks and updates properties that have multiple values
	 * @param type		Short name of the content model property to be updated
	 * @param array		New list of values to update
	 * @param valueType	The value type (needed for casting and making things generic)
	 * @return			True if values updated/create, false if unchanged
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> boolean createOrUpdatePropertyValues(String type, JSONArray array, T valueType) throws JSONException {
		ArrayList<T> values = new ArrayList<T>();
		for (int ii = 0; ii < array.length(); ii++) {
			values.add((T) array.get(ii));
		}
		
		ArrayList<T> oldValues = (ArrayList<T>) getProperty(type);
		if (!checkIfListsEquivalent(oldValues, values)) {
			log(getName() + ": " + type + " multivalue property updated to " + values);
			setProperty(type, values);
		} else {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Utility to compare lists of node refs to one another
	 * @param x	First list to compare
	 * @param y	Second list to compare
	 * @return	true if same, false otherwise
	 */
	public static <T extends Serializable> boolean checkIfListsEquivalent(ArrayList<T> x, ArrayList<T> y) {
		if (x == null || y == null) {
			return false;
		}
		if (x.size() != y.size()) {
			return false;
		}
		for (int ii = 0; ii < x.size(); ii++) {
			if (!x.get(ii).equals(ii)) {
				return false;
			}
		}
		return true;
	}
	
	
	@Override
	public EmsScriptNode createNode(String name, String type) {
		EmsScriptNode result = null;
//		Date start = new Date(), end; 

		if (!useFoundationalApi) {
			result = new EmsScriptNode(super.createNode(name, type).getNodeRef(), services, response);
		} else {
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(
					1, 1.0f);
			// don't forget to set the name
			props.put(ContentModel.PROP_NAME, name);

			ChildAssociationRef assoc = services.getNodeService().createNode(
					nodeRef,
					ContentModel.ASSOC_CONTAINS,
					QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
							QName.createValidLocalName(name)),
					createQName(type), props);
			log("Node " + name + " created");
			result = new EmsScriptNode(assoc.getChildRef(), services, response);			
		}
		
//		end = new Date(); System.out.println("\tcreateNode: " + (end.getTime()-start.getTime()));
		return result;
	}

	/**
	 * Returns a list of the AssociationRefs by the specified type
	 * FIXME The regexp doesn't seem to work to getting all the associations of a particular type
	 * @param type		Short name for type to filter on
	 * @return
	 */
	public List<AssociationRef> getAssociationsByType(String type) {
		String tokens[] = type.split(":");
		if (tokens.length != 2) {
			return null;
		}
		RegexQNamePattern pattern = new RegexQNamePattern(tokens[0], tokens[1]);
		return services.getNodeService().getTargetAssocs(nodeRef, pattern);
	}
	
	/**
	 * Return the first AssociationRef of a particular type
	 * @param type	Short name for type to filter on
	 * @return
	 */
	public EmsScriptNode getFirstAssociationByType(String type) {
		List<AssociationRef> assocs = services.getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
		if (assocs != null) {
			// check all associations to see if there's a matching association
			for (AssociationRef ref: assocs) {
				if (ref.getTypeQName().equals(createQName(type))) {
					return new EmsScriptNode(ref.getTargetRef(), services, response);
				}
			}
		}
		return null;
	}

	/**
	 * Get list of ChildAssociationRefs
	 * @return
	 */
	public List<ChildAssociationRef> getChildAssociationRefs() {
		return services.getNodeService().getChildAssocs(nodeRef);
	}
	
	
	@Override
	public String getName() {
		return (String)getProperty("cm:name");
	}


	@Override
	public EmsScriptNode getParent() {
		return new EmsScriptNode(super.getParent().getNodeRef(), services, response);
	}


	/**
	 * Get the property of the specified type
	 * @param acmType	Short name of property to get
	 * @return
	 */
	public Object getProperty(String acmType) {
		if (useFoundationalApi) {
			return services.getNodeService().getProperty(nodeRef, createQName(acmType));
		} else {
			return getProperties().get(acmType);
		}
	}

	
	public StringBuffer getResponse() {
		return response;
	}


	public Status getStatus() {
		return status;
	}

	/**
	 * Append onto the response for logging purposes
	 * @param msg	Message to be appened to response
	 */
	public void log(String msg) {
//		if (response != null) {
//			response.append(msg + "\n");
//		}
	}

	
	/**
	 * Genericized function to set property for non-collection types (collections are a bit hairier)
	 * @param acmType
	 * @param value
	 */
	public <T extends Serializable >void setProperty(String acmType, T value) {
		if (useFoundationalApi) {
			log(getName() + ": " + acmType + " property set to " + value);
			services.getNodeService().setProperty(nodeRef, createQName(acmType), value);
		} else {
			getProperties().put(acmType, value);
			save();
		}
	}
	
	
	public void setResponse(StringBuffer response) {
		this.response = response;
	}

	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * Gets the SysML qualified name for an object - if not SysML, won't return anything
	 * @return
	 */
	public String getSysmlQName() {
        StringBuffer qname = new StringBuffer();

        NodeService nodeService = services.getNodeService();
        Path path = nodeService.getPath(this.getNodeRef());
        Iterator<Path.Element> pathElements = path.iterator();
        while (pathElements.hasNext()) {
            Path.Element pathElement = pathElements.next();
            if (pathElement instanceof ChildAssocElement) {
                   ChildAssociationRef elementRef = ((ChildAssocElement)pathElement).getRef();
                    if (elementRef.getParentRef() != null)
                    {
                        Serializable nameProp = null;
                        nameProp = nodeService.getProperty(elementRef.getChildRef(), QName.createQName(Acm.ACM_NAME, services.getNamespaceService()));
                        if (nameProp != null) {
                            // use the name property if we are allowed access to it
                            qname.append("/" + nameProp.toString());
                        }
                    }
            }
        }

        return qname.toString();
	}
	

	/**
	 * Get the children views as a JSONArray
	 * @return
	 */
	public JSONArray getChildrenViewsJSONArray() {
	    JSONArray childrenViews = new JSONArray();
        try {
            Object property = this.getProperty(Acm.ACM_CHILDREN_VIEWS);
            if (property != null) {
                childrenViews = new JSONArray(property.toString());
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

	    return childrenViews;
	}
	
	
	public JSONObject toJSONObject() {
	    return toJSONObject(Acm.JSON_TYPE_FILTER.ALL);
	}
	
	public JSONObject toJSONObject(Acm.JSON_TYPE_FILTER renderType) {
	    return toJSONObject(renderType, true, true);
	}
	
	/**
	 * Convert node into our custom JSONObject
	 */
	public JSONObject toJSONObject(Acm.JSON_TYPE_FILTER renderType, boolean showQualifiedName, boolean showEditable) {
	    JSONObject element = new JSONObject();
	    try {
            // add in all the properties
            for (String acmType: Acm.ACM2JSON.keySet()) {
                Object elementValue = this.getProperty(acmType);
                if (elementValue != null) {
                    String jsonType = Acm.ACM2JSON.get(acmType);
                    if (Acm.JSON_FILTER_MAP.get(renderType).contains(jsonType)) {
                        if (Acm.JSON_ARRAYS.contains(jsonType)) {
                            element.put(jsonType, new JSONArray(elementValue.toString()));
                        } else {
                            element.put(jsonType, elementValue);
                        }
                    }
                }
            }

            // add custom properties
            if (Acm.JSON_FILTER_MAP.get(renderType).contains(Acm.JSON_TYPE)) {
                element.put(Acm.JSON_TYPE,  this.getQNameType().getLocalName());
            }

            if (Acm.JSON_FILTER_MAP.get(renderType).contains(Acm.JSON_VALUE_TYPE)) {
                Object valueType = this.getProperty(Acm.ACM_VALUE_TYPE);
                if (valueType != null) {
                    element.put("value",  this.getProperty(Acm.JSON2ACM.get((String) valueType)));
                    element.put(Acm.JSON_VALUE_TYPE,  valueType);
                }
            }
            
            if (Acm.JSON_FILTER_MAP.get(renderType).contains(Acm.JSON_OWNER)) {
                EmsScriptNode parent = this.getParent();
                element.put(Acm.JSON_OWNER,  parent.getName().replace("_pkg", ""));
            }
            
            if (showQualifiedName) {
                element.put("qualifiedName",  this.getSysmlQName());
            }
            
            if (showEditable) {
                element.put("editable", this.hasPermission(PermissionService.WRITE));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }    

	    return element;
	}
	
	
	/**
	 * Given an JSONObject, filters it to find the appropriate relationships
	 * @param jsonObject
	 * @return
	 */
	public static JSONObject filterRelationsJSONObject(JSONObject jsonObject) {
        JSONObject relations = new JSONObject();
        JSONObject elementValues = new JSONObject();
        JSONObject propertyTypes = new JSONObject();
        JSONObject relationshipElements = new JSONObject();
        JSONArray array;

        try {
            if (jsonObject.has(Acm.JSON_VALUE_TYPE)) {
                array = jsonObject.getJSONArray("value");
                if (jsonObject.get(Acm.JSON_VALUE_TYPE).equals(Acm.JSON_ELEMENT_VALUE)) {
                    elementValues.put(jsonObject.getString(Acm.JSON_ID), array);
                }
            } else if (jsonObject.has(Acm.JSON_PROPERTY_TYPE)) {
                String propertyType = jsonObject.getString(Acm.JSON_PROPERTY_TYPE);
                propertyTypes.put(jsonObject.getString(Acm.JSON_ID), propertyType);
            } else if (jsonObject.has(Acm.JSON_SOURCE) && jsonObject.has(Acm.JSON_TARGET)) {
                JSONObject relJson = new JSONObject();
                String source = jsonObject.getString(Acm.JSON_SOURCE);
                String target = jsonObject.getString(Acm.JSON_TARGET);
                relJson.put(Acm.JSON_SOURCE, source);
                relJson.put(Acm.JSON_TARGET, target);
                relationshipElements.put(jsonObject.getString(Acm.JSON_ID), relJson);
            }

            relations.put("relationshipElements", relationshipElements);
            relations.put("propertyTypes", propertyTypes);
            relations.put("elementValues", elementValues);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return relations;
	}
	
	/**
	 * Update the node with the properties from the jsonObject
	 * @param jsonObject
	 */
	public void ingestJSON(JSONObject jsonObject) {
        try {
    	    JSONArray array;
    	    
    	    // fill in all the properties
    	    for (String jsonType: Acm.JSON2ACM.keySet()) {
    	        String acmType = Acm.JSON2ACM.get(jsonType);
    	        if (jsonObject.has(jsonType)) {
    	            if (jsonType.equals(Acm.JSON_VIEW_2_VIEW) || jsonType.equals(Acm.JSON_NO_SECTIONS)) {
    	                array = jsonObject.getJSONArray(jsonType);
    	                this.createOrUpdateProperty(acmType, array.toString());
    	            } else {
    	                String property = jsonObject.getString(jsonType);
    	                if (jsonType.startsWith("is")) {
    	                    this.createOrUpdateProperty(acmType, new Boolean(property));
    	                } else {
    	                    this.createOrUpdateProperty(acmType, new String(property));
    	                }
    	            }
    	        }
    	    }
    	    
    	    // fill in the valueTypes and all relationships
            if (jsonObject.has(Acm.JSON_VALUE_TYPE)) {
                String acmType = Acm.JSON2ACM.get(jsonObject.get(Acm.JSON_VALUE_TYPE));
                array = jsonObject.getJSONArray("value");
                if (acmType.equals("sysml:boolean")) {
                    this.createOrUpdatePropertyValues(acmType, array, new Boolean(true));
                } else if (acmType.equals("sysml:integer")) {
                    this.createOrUpdatePropertyValues(acmType, array, new Integer(0));
                } else if (acmType.equals("sysml:double")) {
                    this.createOrUpdatePropertyValues(acmType, array, new Double(0.0));
                } else {
                    this.createOrUpdatePropertyValues(acmType, array, new String(""));
                }
            }
	    } catch (JSONException e) {
	        e.printStackTrace();
	    }
	}
}