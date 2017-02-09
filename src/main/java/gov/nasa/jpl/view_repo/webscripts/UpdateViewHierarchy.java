package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateViewHierarchy {
    static Logger logger = Logger.getLogger( UpdateViewHierarchy.class );

    protected ModelPost mp;
	protected JSONObject jsonObject;

	Map<String, String> owners = new LinkedHashMap<String, String>();
	Map<String, ArrayList<String>> elementOwnedAttributes = new LinkedHashMap<String, ArrayList<String>>();
	// Map<String, String> viewOwners = new HashMap< String, String >();
	Map<String, ArrayList<String>> viewOwners = new LinkedHashMap<String, ArrayList<String>>();
	Map<String, ArrayList<String>> viewChildViews = new LinkedHashMap<String, ArrayList<String>>();
    Map<String, ArrayList<String>> view2Views = new LinkedHashMap<String, ArrayList<String>>();
    Map<String, String> childViewAggregations = new LinkedHashMap<String, String>();
    Map<String, String> ownedAttributeAggregations = new LinkedHashMap<String, String>();
	Map<String, JSONObject> viewsInJson = new LinkedHashMap<String, JSONObject>();
	Map<String, JSONObject> viewAssociationsInJson = new LinkedHashMap<String, JSONObject>();
	Map<String, JSONObject> viewAssociations = new LinkedHashMap<String, JSONObject>();
	Map<String, JSONObject> associations = new LinkedHashMap<String, JSONObject>();
	Map<String, Set<JSONObject>> associationSources = new LinkedHashMap<String, Set<JSONObject>>();
	Map<String, Set<JSONObject>> associationTargets = new LinkedHashMap<String, Set<JSONObject>>();
	Map<String, JSONObject> elementsInJson = new LinkedHashMap<String, JSONObject>();

    private ServiceRegistry services;
    private WorkspaceNode workspace;
    private StringBuffer response;
    private List<String> idsToDelete = new ArrayList<String>();

    // FIXME -- need to add workspace to constructor and use in methods in this
    // class instead of the workspaces of nodes encountered.
	public UpdateViewHierarchy(ModelPost mp) {
		this.mp = mp;
	}

    public UpdateViewHierarchy(ModelPost mp, WorkspaceNode workspace, ServiceRegistry services, StringBuffer response) {
        this.mp = mp;
        this.workspace = workspace;
        this.services = services;
        this.response = response;
    }

	/**
	 * Process the payload JSON
	 * 
	 * @param jsonObj
	 *            - JSON passed in from client
	 */
	protected void preprocessJson(JSONObject jsonObj) {
		this.jsonObject = jsonObj;
		// Pull stuff out of json
		JSONArray elementsJson = jsonObject.optJSONArray("elements");
		if (elementsJson != null && elementsJson.length() > 0) {
			for (int i = 0; i < elementsJson.length(); ++i) {
				JSONObject elementJson = elementsJson.optJSONObject(i);
				preprocessElementJson(elementJson);
			}
		}
	}

	/**
	 * process the individual element within payload > elements json
	 * 
	 * @param elementJson
	 */
	protected void preprocessElementJson(JSONObject elementJson) {
		if (elementJson == null)
			return;

		String id = elementJson.optString("sysmlid");
		if (Utils.isNullOrEmpty(id)) {
			id = NodeUtil.createId(mp.getServices());
			elementJson.put("sysmlid", id);
		}
		elementsInJson.put(id, elementJson);
		JSONObject spec = elementJson.optJSONObject("specialization");
		if (spec != null) {
			preprocessSpecializationJSONObject(id, elementJson, spec);
		}
	}

	/**
	 * process the specialization object within the element's object found
	 * inside the payload > elements json
	 * 
	 * @param id
	 *            - (String) sysml id
	 * @param elementJson
	 *            - (JSONObject) element wrapping specialization object
	 * @param spec
	 *            - (JSONObject) specialization object
	 */
	protected void preprocessSpecializationJSONObject(String id,
			JSONObject elementJson, JSONObject spec) {
        boolean b = spec.has( "type" );
		String type = b ? spec.optString("type") : null;
		if ("View".equals(type) || "Product".equals(type)) {
			preprocessViewOrProduct(id, elementJson, spec);
		} else if ("Association".equals(type)) {
			preprocessAssociation(id, elementJson, spec);
		}
	}

	/**
	 * process View or Product type found within specialization object
	 * 
	 * @param id
	 *            - (String) sysml id
	 * @param elementJson
	 *            - (JSONObject) element wrapping specialization object
	 * @param spec
	 *            - (JSONObject) specialization object
	 */
	protected void preprocessViewOrProduct(String id, JSONObject elementJson,
			JSONObject spec) {
		// viewInJson = true;
		viewsInJson.put(id, elementJson);

		boolean b = elementJson.has( "owner" );
		String owner = b ? elementJson.optString("owner") : null;
		// TODO -- what if owner is specified as null in json? null =
		// JSONObject.NULL
		if (!Utils.isNullOrEmpty(owner)) {
			owners.put(id, owner);
		}

		// If view2view exists, it is used instead of childViews (because of a bug).
		// FIXME
        JSONArray view2viewArray = getView2ViewArray(elementJson, spec);
        if (view2viewArray != null) {
            preprocessView2Views( view2viewArray);
		} else {
		    JSONArray childViewsArray = getChildViewsArray(elementJson, spec);
		    if (childViewsArray != null) {
		        preprocessChildViews(id, childViewsArray);
	        }
		}

		JSONArray ownedAttributesArray = getOwnedAttributesArray(elementJson,
		                                                         spec);
		if (ownedAttributesArray != null) {
			preprocessOwnedAttributesArray(id, ownedAttributesArray);
		}
	}

	// FIXME -- The association aggregation (composite, shared, none) is ignored!
	// FIXME -- The aggregation should be used to update associations.
	protected void preprocessChildViews(String id, JSONArray childViewsArray) {
		ArrayList<String> childViews = new ArrayList<String>();
		for (int j = 0; j < childViewsArray.length(); ++j) {
		    JSONObject childViewObj = childViewsArray.optJSONObject( j );
		    String childView = null;
		    String aggregation = null;
		    if ( childViewObj != null ) {
		        childView = childViewObj.optString( "id" );
		        aggregation = childViewObj.optString( "aggregation" );
		        if (childView != null && aggregation != null) {
		            childViewAggregations.put(childView, aggregation);
		        }
		    } else {
		        childView = childViewsArray.optString(j);
		    }
		    
		    if ( !Utils.isNullOrEmpty( childView ) ) childViews.add(childView);
			ArrayList<String> viewOwnerSet = viewOwners.get(childView);
			// create set if it doesn't yet exist
			if (viewOwnerSet == null) {
				viewOwnerSet = new ArrayList<String>();
				viewOwners.put(childView, viewOwnerSet);
			}
			viewOwnerSet.add(id);
		}
		viewChildViews.put(id, childViews);
	}

    protected void preprocessView2Views(JSONArray view2viewArray) {
        for (int j = 0; j < view2viewArray.length(); ++j) {
            JSONObject v2vObj = view2viewArray.optJSONObject( j );
            if ( v2vObj == null ) continue;
            String parentId = v2vObj.optString("id");
            if ( Utils.isNullOrEmpty( parentId ) ) continue;
            JSONArray childrenViews = v2vObj.optJSONArray( "childrenViews" );
            if ( childrenViews == null ) continue;
            
            ArrayList<String> childViews = new ArrayList<String>();
            
            for ( int i = 0; i < childrenViews.length(); ++i ) {
                String childViewId = childrenViews.optString( i );
                childViews.add( childViewId );
                ArrayList<String> viewOwnerSet = viewOwners.get(childViewId);
                // create set if it doesn't yet exist
                if (viewOwnerSet == null) {
                    viewOwnerSet = new ArrayList<String>();
                    viewOwners.put(childViewId, viewOwnerSet);
                }
                viewOwnerSet.add(parentId);
            }
            if ( viewChildViews.containsKey( parentId ) ) {
                // This information exists from somewhere else--could be in conflict!
                ArrayList< String > existing = viewChildViews.get( parentId );
                if ( CompareUtils.compareCollections( existing, childViews,
                                                      false, false ) != 0 ) {
                    Debug.error( "Conflicting childViews for " + parentId
                                 + ": existing=" + existing
                                 + "; from view2view: " + childViews );
                }
            } else {
                viewChildViews.put(parentId, childViews);
            }
        }
    }

    protected void preprocessOwnedAttributesArray(String id,
			JSONArray ownedAttributesArray) {
		ArrayList<String> ownedAttributes = new ArrayList<String>();
		for (int j = 0; j < ownedAttributesArray.length(); ++j) {
			String ownedAttribute = ownedAttributesArray.optString(j);
			ownedAttributes.add(ownedAttribute);
		}
		elementOwnedAttributes.put(id, ownedAttributes);
	}

	/**
	 * process Association type found within specialization object
	 * 
	 * @param id
	 *            - (String) sysml id
	 * @param elementJson
	 *            - (JSONObject) element wrapping specialization object
	 * @param spec
	 *            - (JSONObject) specialization object
	 */
	protected void preprocessAssociation(String id, JSONObject elementJson,
			JSONObject spec) {
		associations.put(id, elementJson);
		String sourceId = spec.optString("source");
		if (!Utils.isNullOrEmpty(sourceId)) {
			add(associationSources, sourceId, elementJson);
			// associationSources.put( sourceId, elementJson );
		}
		String targetId = spec.optString("target");
		if (!Utils.isNullOrEmpty(targetId)) {
			add(associationTargets, targetId, elementJson);
		}
	}

	public static <T1, T2> void add(Map<T1, Set<T2>> map, T1 t1, T2 t2) {
		if (Debug.errorOnNull("Error! Called Utils.put() with null argument!",
				map, t1, t2)) {
			return;
		}
		Set<T2> innerMap = map.get(t1);
		if (innerMap == null) {
			innerMap = new LinkedHashSet<T2>();
			map.put(t1, innerMap);
		}
		innerMap.add(t2);
	}

	/**
	 * Add json for changes to Associations, Views, and Products based on
	 * changes in the posted json to elements of the same classes.
	 * <p>
	 * 
	 * 
	 * @param postJson
	 * @return
	 * @throws Exception
	 */
	protected void addJsonForViewHierarchyChanges(JSONObject jsonObj)
			throws Exception {
		Debug.outln("%%%%%%%%%%%%%%%%%         start addJsonForViewHierarchyChanges          %%%%%%%%%%%%%%%%%");
		if (jsonObj == null)
			return; // null;
		this.jsonObject = jsonObj;

		preprocessJson(jsonObject);

		if ( Debug.isOn() ) Debug.outln("before jsonObject = " + jsonObject);
		if ( Debug.isOn() ) Debug.outln("before elementsInJson = " + elementsInJson);

		// Make sure that the ownedAttributes and Associations agree with the
		// childViews.
		for (Entry<String, ArrayList<String>> e : viewChildViews.entrySet()) {
			processViewChildView(e);
		}

        if ( Debug.isOn() ) {
            Debug.outln( "after jsonObject = " + jsonObject );
            Debug.outln( "after elementsInJson = " + elementsInJson );
            Debug.outln( "%%%%%%%%%%%%%%%%%         end addJsonForViewHierarchyChanges          %%%%%%%%%%%%%%%%%" );
            Debug.outln( "%%%%%%%%%%%%%%%%%         end addJsonForViewHierarchyChanges          %%%%%%%%%%%%%%%%%" );
        }
		
		// For each new view that does not have one, create an
		// InstanceSpecification and make the view its parent. The contents of
		// the new view will be an InstanceValue in an Expression that
		// references this
		// InstanceSpecification.

		// The ownedAttributes and childViews must agree on ordering. A parent
		// view's ownedAttributes include Properties, whose propertyTypes are
		// childViews to its child views. The childViews are the ordered ids of
		// the child views. Posted childViews override the ownedAttributes.

		// Add a view, v, to a parent, p, by finding the association with its
		// current parent, and changing it to point to the new one. If there is
		// no association, generate the json from a template.

		// Move the new or updated association (change its owner) to the first
		// Package found walking from the parent up the owner chain. If no
		// Package is found, place it directly under the project.

		// The view is added to the parent if the parent is specified, and the
		// view was not already added to the parent.

		// Add a "childViews" to the content model and output JSON: this should
		// interpret the
		// ownedAttribute of the view/product and give back:
		// [ {"id", childViewId, "aggregation": "composite", "shared", or
		// "none"}
		// , ...]
		// where
		// childViewId is the sysmlid of the propertyType of ownedAttribute
		// properties, if it's also a view/product, aggregation is the
		// aggregation of the ownedAttribute property. Ordering matters!

		// childViews can change if the ownedAttributes or aggregationType of a
		// Property changes.

		// Clear out view2view in the Product or keep it consistent with the
		// childViews/InstanceSpecs.

		// A View, v, is added to a parent, p, by creating
		// * a composite Association, a, owning
		// * a Property of type p, pp, (also including pp as an ownedEnd) and
		// * a Property of type v, pv, which is an ownedAttribute of p.

		// return jsonObject;
	}

	protected void removeOwnedAttributeIds(String parentId,
			List<String> ownedAttributeIds) throws Exception {
		for (String oaId : ownedAttributeIds) {
			try {
				removeAssociation(parentId, oaId, null);
			} catch (Exception ex) {
				throw new Exception(
						String.format(
								"Failed to remove association from view [%s] with property [%s]!",
								parentId, oaId));
			}
		}
	}

	protected void processViewChildView(Entry<String, ArrayList<String>> e)
			throws Exception {
		boolean ownedAttributesChanged = false;
		String parent = e.getKey();
		ArrayList<String> childViewsArray = e.getValue();
        List<String> ownedAttributeIds = getOwnedAttributes(parent, elementOwnedAttributes);

		if (Utils.isNullOrEmpty(childViewsArray)
				&& Utils.isNullOrEmpty(ownedAttributeIds))
			// nothing to do. both viewChilds[] and ownedAttribute[] are empty
			return;
		else if (Utils.isNullOrEmpty(childViewsArray)
				&& !Utils.isNullOrEmpty(ownedAttributeIds)) {
			// viewChilds[] is empty so remove ownedAttribute[] from repo
			ownedAttributesChanged = true;
			setOwnedAttributes(parent, elementOwnedAttributes, elementsInJson,
					jsonObject, new ArrayList<String>());
			removeOwnedAttributeIds(parent, ownedAttributeIds);
			return;
		}

		ArrayList<String> newOwnedAttributes = new ArrayList<String>();
		Set<String> childViewIds = new HashSet<String>();

		// Go ahead and find the view ids for the ownedAttributes.
		Map<String, String> viewIdsForOwnedAttributeIds = new LinkedHashMap<String, String>();
		Map<String, String> ownedAttributeIdsForViewIds = new LinkedHashMap<String, String>();
		if (ownedAttributeIds != null) {
			translateOwnedAttributeIdsToViewIds(ownedAttributeIds,
					viewIdsForOwnedAttributeIds, ownedAttributeIdsForViewIds);
		}
		
		// add aggregations for the ownedAttributeIds
		for (String vid: childViewAggregations.keySet()) {
		    ownedAttributeAggregations.put(ownedAttributeIdsForViewIds.get(vid), childViewAggregations.get( vid ));
		}

		// Initialize loop variables.
		String ownedAttributeId = null;
		String viewIdForOwnedAttribute = null;
		Iterator<String> attrIter = null;
		if (!Utils.isNullOrEmpty(ownedAttributeIds)) {
			attrIter = ownedAttributeIds.iterator();
			ownedAttributeId = attrIter.next();
			viewIdForOwnedAttribute = viewIdsForOwnedAttributeIds
					.get(ownedAttributeId);
		}
		// See if the ownedAttributes are correct.
		for (String childViewStr : childViewsArray) {
		    JSONObject childViewJSONObject = null;
		    String childViewId = null;
		    try {
	            childViewJSONObject = new JSONObject(childViewStr);
		        childViewId = childViewJSONObject.optString("id");
		    } catch (JSONException x) {
		        childViewId = childViewStr;
		        childViewJSONObject = new JSONObject();
		        childViewJSONObject.put( "id", childViewId );
		    }
		    childViewIds.add( childViewId );
		    
			// Walk through non-view typed attributes
			while (viewIdForOwnedAttribute == null && ownedAttributeId != null) {
				if (ownedAttributeId != null) {
					// got a non-view attribute; add to owned attributes
					newOwnedAttributes.add(ownedAttributeId);
				}
				if (attrIter.hasNext()) {
					ownedAttributeId = attrIter.next();
					viewIdForOwnedAttribute = viewIdsForOwnedAttributeIds
							.get(ownedAttributeId);
				} else {
					viewIdForOwnedAttribute = null;
					ownedAttributeId = null;
					break;
				}
			}

			// At this point, either the owned attribute is a view, or we
			// are finished iterating through the attribute ids.

			// Walk through matching and non-matching ownedAttributes for
			// the views.
			//
			// If ownedAttribute does not match childView, either
			// (1) it's an ordering change, and we need only change the
			// order of ownedAttributes,
			// (2) it's a new childView, and we need to create or revise the
			// Association and Property and add the Property to
			// ownedAttributes, or
			// (3) it's a loss of an ownedAttribute for a view, in which
			// case we need to revise or remove the Association and the
			// Property.
			boolean matched = false;
			boolean first = true;
			while (first || viewIdForOwnedAttribute != null) {
				first = false;
				matched = viewIdForOwnedAttribute != null
						&& viewIdForOwnedAttribute.equals(childViewId);
				ownedAttributesChanged = true;
				boolean inOwned = ownedAttributeIdsForViewIds.keySet()
						.contains(childViewId);
				boolean inChildViews = isViewIdInChildViews(
						viewIdForOwnedAttribute, childViewsArray);
				String propId = null;

				if (inOwned && inChildViews) {
					// (1) reordered
					String newOwnedAttributeId = ownedAttributeIdsForViewIds
							.get(childViewId);
					if (!newOwnedAttributes.contains(newOwnedAttributeId)) {
						newOwnedAttributes.add(newOwnedAttributeId);
					}
					// Check for the association in case it wasn't added.
					// updateOrCreateAssociation(parent, newOwnedAttributeId,
					// childId);
				} else {
					if (!inChildViews) {
						// (3) view lost -- remove Association
						removeAssociation(parent, ownedAttributeId,
								viewIdForOwnedAttribute);
					}
					if (!inOwned) {
						// (2) case new child -- create or revise
						// Association
						propId = updateOrCreateAssociation(parent, null,
								childViewJSONObject);
						if (!Utils.isNullOrEmpty(propId)) {
							newOwnedAttributes.add(propId);
						}
						// REVIEW -- does the above call create the
						// property??!
						// TODO -- This assumes that previous call updates
						// maps.
						// String newOwnedAttributeId =
						// ownedAttributeIdsForViewIds
						// .get(childId);
						// newOwnedAttributes.add(newOwnedAttributeId);
						// newOwnedAttributes.add(childId);
						break;
					}
				}

				// Get next ownedAttribute.
				if (attrIter.hasNext()) {
					ownedAttributeId = attrIter.next();
					viewIdForOwnedAttribute = getViewIdForOwnedAttribute(
							ownedAttributeId, elementsInJson);
				} else {
					viewIdForOwnedAttribute = null;
					ownedAttributeId = null;
					break;
				}
				if (matched)
					break;
			}

		}

        // Remove ownedAttributeIds that are views that didn't show up in the childViews
        List<String> origOwnedAttributeIds = getOwnedAttributes(parent, null);
        
        Map<String, String> origViewIdsForOwnedAttributeIds = new LinkedHashMap<String, String>();
        Map<String, String> origOwnedAttributeIdsForViewIds = new LinkedHashMap<String, String>();
        if (ownedAttributeIds != null) {
            translateOwnedAttributeIdsToViewIds(origOwnedAttributeIds,
                    origViewIdsForOwnedAttributeIds, origOwnedAttributeIdsForViewIds);
        }
        List<String> removedOwnedAttributeIds = new ArrayList<String>();
        for (String origOwnedAttributeId: origOwnedAttributeIds) {
            boolean found = false;
            String origViewId = origViewIdsForOwnedAttributeIds.get( origOwnedAttributeId ); 
            if ( origViewId != null) {
                if (childViewIds.contains( origViewIdsForOwnedAttributeIds.get(origOwnedAttributeId) )) {
                    found = true;
                }
                if (!found) {
                    removedOwnedAttributeIds.add( origOwnedAttributeId );
                }
            }
        }
        removeOwnedAttributeIds( parent, removedOwnedAttributeIds );

		if (ownedAttributesChanged) {
			setOwnedAttributes(parent, elementOwnedAttributes, elementsInJson,
					jsonObject, newOwnedAttributes);
		}
	}

	private boolean isViewIdInChildViews(String viewIdForOwnedAttribute,
			ArrayList<String> childViewsArray) {
		if (Utils.isNullOrEmpty(childViewsArray))
			return false;
		if (Utils.isNullOrEmpty(viewIdForOwnedAttribute))
			return true;

		for (String jsonStr : childViewsArray) {
            JSONObject jsonObj = null;
            String viewId = null;
            try {
                jsonObj = new JSONObject(jsonStr);
                viewId = jsonObj.optString("id");
            } catch (JSONException x) {
                viewId = jsonStr;
            }
			if (viewId != null && viewId.equals(viewIdForOwnedAttribute))
				return true;
		}
		return false;
		// viewIdForOwnedAttribute == null
		// || childViewsArray.contains(viewIdForOwnedAttribute);
	}

	public static JSONObject removeElement(String sysmlId, JSONObject obj) {
		JSONArray elements = obj.optJSONArray("elements");
		return removeElement(sysmlId, elements);
	}

	// This is inefficient. If a set of ids to be removed can be collected and
	// done at once, that would be better. Tracking the reverse lookup won't
	// work since indices change.
	public static JSONObject removeElement(String sysmlId, JSONArray elements) {
		for (int i = 0; i < elements.length(); ++i) {
			JSONObject element = elements.optJSONObject(i);
			if (element == null) {
				// TODO -- error
				continue;
			}
			String id = element.optString("sysmlid");
			if (sysmlId.equals(id)) {
				elements.remove(i);
				return element;
			}
		}
		return null;
	}

	protected void removeElementFromJson(String id) {
		this.removeElement(id, (Set<String>) null);
	}

	protected void removeElement(String id, Set<String> seen) {
		Pair<Boolean, Set<String>> p = Utils.seen(id, true, seen);
		if (p.first)
			return;
		seen = p.second;

		// remove from JSON
		elementsInJson.remove(id);
		elementOwnedAttributes.remove(id);
		viewOwners.remove(id);
		viewChildViews.remove(id);
		viewsInJson.remove(id);
		viewAssociations.remove(id);
		viewAssociationsInJson.remove(id);
		associations.remove(id);
		associationSources.remove(id);
		associationTargets.remove(id);
		removeElement(id, this.jsonObject);

		Set<JSONObject> assocs = associationSources.get(id);
		if (assocs != null) {
		    if (associationTargets.get( id ) != null) {
        		    assocs.addAll(associationTargets.get(id));
		    }
        		for (JSONObject assoc : assocs) {
        			String sysmlId = assoc.optString("sysmlid");
        			if (!Utils.isNullOrEmpty(sysmlId)) {
        				removeElement(sysmlId, seen);
        			}
        		}
		}
	}

	protected String addElement(JSONObject element) {
		if (element == null)
			return null;
		boolean b = element.has( "sysmlid" );
		String id = b ? element.optString("sysmlid") : null;
		if (Utils.isNullOrEmpty(id))
			id = NodeUtil.createId(mp.getServices());
		JSONArray elements = jsonObject.optJSONArray("elements");
		if (elements != null)
			elements.put(element); // TODO -- else ERROR!
		return id;
	}

	/**
	 * Get associations relating the parent and Property with the specified
	 * property type. The parentId must be non-null and is the source of the
	 * associations. One of the propertyId and propertyTypeId may be null, in
	 * which case associations matching the non-null arguments are returned.
	 * 
	 * @param parentId
	 * @param propertyId
	 * @param propertyTypeId
	 * @return
	 */
	protected List<String> getAssociationIdsFromJson(String parentId,
			String propertyId, String propertyTypeId) {
		if (parentId == null) {
			// TODO -- error
			return null;
		}
		if (propertyId == null) {
			if (propertyTypeId == null) {
				// TODO -- error
				return null;
			}
		}
		List<String> assocIds = new ArrayList<String>();
        
		Set<JSONObject> assocs = associationSources.get(parentId);
		if (assocs != null) {
			for (JSONObject assoc : assocs) {
				boolean found = false;
				String targetId = assoc.optString("target");
				if (propertyId != null && propertyId.equals(targetId)) {
					found = true;
				} else if (propertyId == null && !Utils.isNullOrEmpty(targetId)
						&& propertyTypeId != null) {
					String propType = getPropertyType(targetId);
					if (propertyTypeId.equals(propType)) {
						found = true;
					}
				}
				if (found) {
					String assocId = assoc.optString("sysmlid");
					if (assocId != null) {
						assocIds.add(assocId);
					}
				}
			}
		}

        // need to search for associations that have the property as a source
        if (propertyId != null ) {
        EmsScriptNode propNode = NodeUtil.findScriptNodeById( propertyId, workspace, null, false, services, response );
            if (propNode != null) {
                String searchString = "workspace://SpacesStore/" + propNode.getNodeRef().getId();
                Map< String, EmsScriptNode > results = NodeUtil.searchForElements( "@sysml\\:source:\"", searchString, false, workspace, null, services, response, null );
                for (String id: results.keySet()) {
                    assocIds.add( id );
                }
            }
        }
		return assocIds;
	}

    /**
     * Given a view node, retrieves its childViews based on ownedAttribute propertiesIds
     *  
     * @param parentNode   viewNode to retrieve children for
     * @param ws           Workspace to look children up in
     * @param dateTime     time to lookup
     * @return  JSONArray, e.g. [{"id":"childView1","aggregation":"COMPOSITE"}]
     */
    public static JSONArray getChildViewsJsonArray(EmsScriptNode parentNode, WorkspaceNode ws, Date dateTime) {
        return getChildViewsJsonArray(parentNode, ws, dateTime, false);
    }
    
    
    /**
     * Given a view node, retrieves its childViews based on ownedAttribute propertiesIds
     *  
     * @param parentNode   viewNode to retrieve children for
     * @param ws           Workspace to look children up in
     * @param dateTime     time to lookup
     * @param recurse       true if recursing over childViews, false otherwise
     * @return  JSONArray, e.g. [{"id":"childView1","aggregation":"COMPOSITE"}]
     */
	public static JSONArray getChildViewsJsonArray(EmsScriptNode parentNode, WorkspaceNode ws, Date dateTime, boolean recurse) {
	    if (parentNode == null || !NodeUtil.exists( parentNode )) return null;
	    
	    List<Pair< String, EmsScriptNode > > childViews = new ArrayList<Pair<String, EmsScriptNode>>();
        Set< String > visited = new HashSet<String>();
        getChildViews( parentNode, ws, dateTime, recurse, childViews, visited );
        JSONArray childViewsJsonArray = new JSONArray();
        
        for (Pair<String, EmsScriptNode> pair: childViews) {
            JSONObject childJson = new JSONObject();
            childJson.put( "id", pair.second.getSysmlId() );
            childJson.put( Acm.JSON_AGGREGATION, pair.first );
            childViewsJsonArray.put( childJson );
        }
        
        return childViewsJsonArray;
	}

    /**
     * Given a view node, retrieves its childViews based on ownedAttribute propertiesIds
     *  
     * @param parentNode   viewNode to retrieve children for
     * @param ws           Workspace to look children up in
     * @param dateTime     time to lookup
     * @param recurse       true if recursing over childViews, false otherwise
     * @return  Set of all view ids that are children
     */
	public static Set<EmsScriptNode> getChildViews(EmsScriptNode parentNode, WorkspaceNode ws, Date dateTime, boolean recurse) {
        List<Pair< String, EmsScriptNode > > childViews = new ArrayList< Pair<String, EmsScriptNode>>();
        Set< String > visited = new HashSet<String>();
        getChildViews( parentNode, ws, dateTime, recurse, childViews, visited );
        
        Set<EmsScriptNode> childViewsSet = new HashSet<EmsScriptNode>();
        for (Pair<String, EmsScriptNode> cv: childViews) {
            childViewsSet.add(cv.second);
        }
        
        return childViewsSet;
	}

	/**
	 * Given a view node, retrieves its childViews based on ownedAttribute propertiesIds
	 * 
     * @param parentNode   viewNode to retrieve children for
     * @param ws           Workspace to look children up in
     * @param dateTime     time to lookup
     * @param recurse       true if recursing over childViews, false otherwise
	 * @param childViews   List of ORDERED ids to the childViews and their aggregation type - this is built up recursively
	 * @param visited      Set of all ids already visited, so recursion doesn't cycle
	 */
   public static void getChildViews(EmsScriptNode parentNode, WorkspaceNode ws, Date dateTime, boolean recurse, List<Pair<String, EmsScriptNode>> childViews, Set<String> visited) {
        if (childViews == null || visited == null || parentNode == null || !NodeUtil.exists(parentNode)) {
            return;
        }

        visited.add( parentNode.getSysmlId() );
        ServiceRegistry services = parentNode.getServices();

        // get ownedAttributes
        // TODO reuse instance getOwnedAttributes()
        List<String> ownedAttributeIds = new ArrayList<String>();
        Object ownedAttRefs = parentNode.getNodeRefProperty(
                Acm.ACM_OWNED_ATTRIBUTE, dateTime, ws);
        if (ownedAttRefs instanceof Collection) {
            List<NodeRef> refs = Utils.asList((Collection<?>) ownedAttRefs,
                    NodeRef.class);
            ownedAttributeIds = EmsScriptNode.getSysmlIds(EmsScriptNode
                    .toEmsScriptNodeList(refs));
        }
        if (Utils.isNullOrEmpty(ownedAttributeIds))
            return;

        // translate ownedAttributes to View Ids
        // TODO reuse instance translateOwnedAttributeIds()
        for (String ownedAttributeId : ownedAttributeIds) {
            String viewId = null;
            EmsScriptNode viewNode = null;
            EmsScriptNode ownedAttributeNode = NodeUtil.findScriptNodeById(
                    ownedAttributeId, ws, dateTime, false, services, null);
            if (NodeUtil.exists(ownedAttributeNode)) {
                Object propType = ownedAttributeNode.getNodeRefProperty(
                        Acm.ACM_PROPERTY_TYPE, true, null, ws);
                Collection<?> propTypes = null;
                if (propType instanceof Collection) {
                    propTypes = (Collection<?>) propType;
                } else if (propType instanceof NodeRef) {
                    propTypes = Utils.newList(propType);
                }
                if (Utils.isNullOrEmpty(propTypes))
                    continue;

                for (Object pType : propTypes) {
                    if (UpdateViewHierarchy.isView(pType, services, ws)) {
                        if (pType instanceof NodeRef) {
                            EmsScriptNode propTypeNode = new EmsScriptNode(
                                    (NodeRef) pType, services);
                            if (NodeUtil.exists(propTypeNode)
                                    && propTypeNode
                                            .hasOrInheritsAspect(Acm.ACM_VIEW)) {
                                viewId = propTypeNode.getSysmlId();
                                viewNode = propTypeNode;
                            }
                        } else if (pType instanceof String) {
                            viewId = (String) pType;
                            viewNode = NodeUtil.findScriptNodeById( viewId, ws, dateTime, false, services, null );
                        }
                    }
                    if (!Utils.isNullOrEmpty( viewId )) {
                        break;
                    }
                }

                if (!Utils.isNullOrEmpty(viewId)) {
                    String aggregation = (String) ownedAttributeNode
                            .getProperty(Acm.ACM_AGGREGATION);
                    if (Utils.isNullOrEmpty(aggregation)) {
                        aggregation = "NONE";
                    }
                    Pair<String, EmsScriptNode> pair = new Pair<String, EmsScriptNode>(aggregation, viewNode);
                    childViews.add( pair );
                }
            }
        }
        
        if (recurse) {
            // Copy off child views, so we can modify childViews without concurrent modification exception
            List<Pair<String, EmsScriptNode>> copiedViews = new ArrayList<Pair<String, EmsScriptNode>>(childViews);
            for ( Pair<String, EmsScriptNode> cv: copiedViews ) {
                String id = cv.second.getSysmlId();
                if (!visited.contains( id )) {
                    getChildViews( cv.second, ws, dateTime, recurse, childViews, visited );
                }
            }
        }
    }


	protected List<EmsScriptNode> getAssociationNodes(String parentId,
			String propertyId, String propertyTypeId) {
		if (parentId == null) {
			// TODO -- error
			return null;
		}
		if (propertyId == null) {
			if (propertyTypeId == null) {
				// TODO -- error
				return null;
			}
		}

		List<EmsScriptNode> assocNodes = new ArrayList<EmsScriptNode>();
		EmsScriptNode targetPropNode = mp.findScriptNodeById(propertyId,
				mp.myWorkspace, null, false);
		if (NodeUtil.exists(targetPropNode)) {
			Set<EmsScriptNode> rels = targetPropNode.getRelationships(null,
					mp.myWorkspace);
			for (EmsScriptNode rel : rels) {
				Object prop = rel.getNodeRefProperty(Acm.ACM_SOURCE, null,
						mp.myWorkspace);
				if (prop instanceof NodeRef) {
					EmsScriptNode propNode = new EmsScriptNode((NodeRef) prop,
							mp.getServices());
					if (NodeUtil.exists(propNode)) {
						if (propertyId == null
								|| propNode.getSysmlId().equals(propertyId)) {
							if (propNode.hasOrInheritsAspect(Acm.ACM_PROPERTY)) {
								Object propType = propNode.getNodeRefProperty(
										Acm.ACM_PROPERTY_TYPE, null,
										mp.myWorkspace);
								if (propType instanceof NodeRef) {
									EmsScriptNode node = new EmsScriptNode(
											(NodeRef) propType,
											mp.getServices());
									if (NodeUtil.exists(node)) {
										if (propertyTypeId == null
												|| node.getSysmlId().equals(
														propertyTypeId)) {
											assocNodes.add(rel);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return assocNodes;
	}

	protected void removeAssociation(String parentId, String propertyId,
			String childId) throws Exception {

		List<String> assocIds = getAssociationIdsFromJson(parentId, propertyId,
				childId);
		for (String assoc : assocIds) {
			removeElementFromJson(assoc);
		}

		List<EmsScriptNode> deleteNodes = getAssociationNodes(parentId,
				propertyId, childId);
		List<String> deleteIds = EmsScriptNode.getSysmlIds(deleteNodes);
		deleteIds.add( propertyId );
		if (logger.isInfoEnabled()) logger.info( "Deleting: " + deleteIds.toString() );

		// cache these for use later, otherwise conflict check will trigger
		this.idsToDelete.addAll( deleteIds );
	}

	protected boolean deleteNodes() {
	    if (this.idsToDelete == null) return true;
		MmsModelDelete mmd = new MmsModelDelete(mp.repository, mp.getServices());
		mmd.setWsDiff(mp.myWorkspace);
		try {
			mmd.findNodesToDelete(this.idsToDelete, mp.myWorkspace);
			mmd.applyDeletedAspects(); 
			if (mp.wsDiff == null) {
				mp.setWsDiff(mp.myWorkspace);
			} 
			mp.wsDiff.getDeletedElements().putAll(
					mmd.wsDiff.getDeletedElements());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected String getPropertyType(String propertyId) {
		// Try in json
		JSONObject element = elementsInJson.get(propertyId);
		if (element != null) {
			JSONObject spec = element.optJSONObject("specialization");
			if (spec != null) {
				String propertyTypeId = spec.optString(Acm.JSON_PROPERTY_TYPE);
				if (!Utils.isNullOrEmpty(propertyTypeId)) {
					return propertyTypeId;
				}
			}
		}

		// Try in DB
		EmsScriptNode propNode = mp.findScriptNodeById(propertyId,
				mp.myWorkspace, null, false);
		if (NodeUtil.exists(propNode)) {
			Object propType = propNode.getNodeRefProperty(
					Acm.ACM_PROPERTY_TYPE, null, mp.myWorkspace);
			if (propType instanceof NodeRef) {
				EmsScriptNode propTypeNode = new EmsScriptNode(
						(NodeRef) propType, mp.getServices());
				if (propTypeNode.exists()) {
					return propTypeNode.getSysmlId();
				}
			}
		}
		return null;
	}

//	protected String addAssociationToProperty(String parentViewId,
//			String propertyId) {
//		// Create the Association
//		JSONObject element = new JSONObject();
//		String id = NodeUtil.createId(mp.getServices());
//		element.put("sysmlid", id);
//		JSONObject spec = new JSONObject();
//		element.put("specialization", spec);
//		spec.put("type", "Association");
//		spec.put(Acm.JSON_SOURCE, parentViewId);
//		spec.put(Acm.JSON_TARGET, propertyId);
//		return id;
//	}

	protected String addProperty(String propertyId, String parentViewId,
			String propertyTypeId) {
		return updateOrCreatePropertyJson(propertyId, parentViewId,
				propertyTypeId, null, null);
	}

	protected String updateOrCreatePropertyJson(String propertyId,
			String parentViewId, String propertyTypeId, JSONObject propElement,
			String aggregation) {
		boolean addedNew = false;
		if (propertyId == null) {
			propertyId = NodeUtil.createId(mp.getServices());
		} else if (propElement == null) {
			propElement = elementsInJson.get(propertyId);
		}
		if (propElement == null) {
			propElement = new JSONObject();
			addedNew = true;
			propElement.put("sysmlid", propertyId);
			addElement(propElement);
		}
		propElement.put("sysmlid", propertyId);
		propElement.put("owner", parentViewId);
		JSONObject spec = propElement.optJSONObject("specialization");
		if (spec == null) {
			spec = new JSONObject();
			propElement.put("specialization", spec);
		}
		spec.put("type", "Property");
		if ( !Utils.isNullOrEmpty( propertyTypeId ) ) {
		    spec.put(Acm.JSON_PROPERTY_TYPE, propertyTypeId);
		}
		if (!Utils.isNullOrEmpty(aggregation)) {
			spec.put(Acm.JSON_AGGREGATION, aggregation);
		}
		if (addedNew) {
			preprocessElementJson(propElement);
		}
		return propertyId;
	}

//	protected String addAssociationToView(String parentViewId,
//			String childViewId) {
//		// Need a Property of type childView to associate with the parentView.
//		String propertyId = addProperty(null, parentViewId, childViewId);
//		String id = addAssociationToProperty(parentViewId, propertyId);
//		return id;
//	}

	protected String findAssociationOwner(String parentId) {
		String lastId = null;
		String id = parentId;
		Set<String> seen = new HashSet<String>();
		while (id != null) {
			if (seen.contains(id)) {
				return null;
			}
			if (isPackage(id)) {
				return id;
			}
			if ("Models".equals(id)) {
				return lastId;
			}
			seen.add(id);
			lastId = id;
			id = getOwnerId(id);
		}
		return null;
	}

	protected String getOwnerId(String id) {
		// Try in JSON first.
		JSONObject element = elementsInJson.get(id);
		if (element != null) {
			String owner = element.optString(Acm.JSON_OWNER);
			if (!Utils.isNullOrEmpty(owner)) {
				return owner;
			}
		}
		// Try in DB.
		EmsScriptNode node = mp.findScriptNodeById(id, mp.myWorkspace, null,
				false);
		if (NodeUtil.exists(node)) {
			EmsScriptNode owner = node.getOwningParent(null, mp.myWorkspace,
					false);
			if (NodeUtil.exists(owner)) {
				return owner.getSysmlId();
			}
		}
		return null;
	}

	protected String getType(String id) {
		// Try in JSON first.
		JSONObject element = elementsInJson.get(id);
		if (element != null) {
			JSONObject spec = element.optJSONObject(Acm.JSON_SPECIALIZATION);
			if (spec != null) {
				String type = spec.optString("type");
				if (!Utils.isNullOrEmpty(type)) {
					return type;
				}
			}
		}
		// Try in DB.
		EmsScriptNode node = mp.findScriptNodeById(id, mp.myWorkspace, null,
				false);
		if (NodeUtil.exists(node)) {
			String type = node.getTypeName();
			return type;
		}
		return null;
	}

	protected boolean isPackage(String id) {
		String type = getType(id);
		if ("Package".equals(type)) {
			return true;
		}
		return false;
	}

	protected String updateOrCreateAssociationJson(String assocId,
			String parentViewId, String propertyId,
			JSONObject childViewJSONObject) {
		boolean addedNew = false;

		JSONObject element = null;
		if (assocId == null) {
			addedNew = true;
			assocId = NodeUtil.createId(mp.getServices());
			element = associations.get(assocId);
		}

		if (element == null) {
			element = new JSONObject();
			element.put("sysmlid", assocId);
			addElement(element);
		}

		String childViewId = childViewJSONObject.optString("id");
		String aggregation = null;
		// MMS-657: use childViewAggregations type since not always available as childViewJSONObject
		//  manual test: create view on VE, create subview on VE, should retain aggregation type
		//               can remove subview and readd by searching for view to test
		if (childViewAggregations.containsKey( childViewId )) {
		    aggregation = childViewAggregations.get( childViewId );
		} else {
		    aggregation = childViewJSONObject.optString(Acm.JSON_AGGREGATION);
		}
        // FIXME -- Won't this overwrite an existing aggregation if the
        // aggregation is null? What if we just don't set the aggregation in the
        // case that it is null?
		if ( Utils.isNullOrEmpty(aggregation) ) {
			aggregation = "COMPOSITE";
		}

		// Association target is its property 
		String propA = updateOrCreatePropertyJson(propertyId, assocId,
				parentViewId, null, null);
		// Association source is the views property
		String propB = updateOrCreatePropertyJson(propertyId, parentViewId,
				childViewId, null, aggregation);

		// String owner = element.getString(Acm.JSON_OWNER);
		String owner = element.optString(Acm.JSON_OWNER);
		if (Utils.isNullOrEmpty(owner)) {
			owner = findAssociationOwner(parentViewId);
			if (owner != null) {
				element.put(Acm.JSON_OWNER, owner);
			}
		}
		if (!Utils.isNullOrEmpty(propA)) {
			List<String> list = new ArrayList<String>();
			list.add(propA);
			JSONArray jsonList = new JSONArray(list);
			element.put(Acm.JSON_OWNED_END, jsonList);
		}

		JSONObject spec = element.optJSONObject("specialization");
		if (spec == null) {
			spec = new JSONObject();
			element.put("specialization", spec);
		}
		spec.put("type", "Association");
		spec.put(Acm.JSON_SOURCE, propB);
		spec.put(Acm.JSON_TARGET, propA);

		if (addedNew) {
			preprocessElementJson(element);
		}

		return propB;
	}

	protected String updateOrCreateAssociation(String parentId,
			String propertyId, JSONObject childJSONObject) {
		// Find existing associations that match the input.
		String idOfAssocToUpdate = null;
		String childViewId = childJSONObject.optString("id");
		List<String> assocIds = getAssociationIdsFromJson(parentId, propertyId,
				childViewId);
		if (!Utils.isNullOrEmpty(assocIds)) {
			if (assocIds.size() > 1) {
				// TODO -- WARNING -- ambiguous choice
			}
			idOfAssocToUpdate = assocIds.get(0);
		}
		List<EmsScriptNode> assocNodes = null;
		if (idOfAssocToUpdate == null) {
			assocNodes = getAssociationNodes(parentId, propertyId, childViewId);
			if (!Utils.isNullOrEmpty(assocNodes)) {
				if (assocNodes.size() > 1) {
					// TODO -- WARNING -- ambiguous choice
				}
				EmsScriptNode assocNode = assocNodes.get(0);
				idOfAssocToUpdate = assocNode.getSysmlId();
			}
		}
		// Update or create the json.
		return updateOrCreateAssociationJson(idOfAssocToUpdate, parentId,
				propertyId, childJSONObject);
	}

	protected void setOwnedAttributes(String parentId,
			Map<String, ArrayList<String>> elementOwnedAttributes,
			Map<String, JSONObject> elementsInJson, JSONObject jsonObject,
			ArrayList<String> newOwnedAttributes) {
		if (Utils.isNullOrEmpty(parentId)) {
			return;
		}

		elementOwnedAttributes.put(parentId, newOwnedAttributes);
		JSONArray newOwnedAttributesArray = new JSONArray(newOwnedAttributes);
		JSONObject elementJson = elementsInJson.get(parentId);
		// If the element is not already in the json, create and add it.
		if (elementJson == null) {
			elementJson = new JSONObject();
			elementJson.put("sysmlid", parentId);
			JSONArray elements = jsonObject.optJSONArray("elements");
			if (elements != null) {
			    elements.put(elementJson);
			}
		}
	    elementJson.put(Acm.JSON_OWNED_ATTRIBUTE, newOwnedAttributesArray);

	    // inject all the aggregation properties for the childViews
	    for (String ownedAttr: newOwnedAttributes) {
	        if (!elementsInJson.containsKey( ownedAttr )) {
	            if (ownedAttributeAggregations.containsKey( ownedAttr )) {
        	            JSONObject specJson = new JSONObject();
        	            specJson.put( "aggregation", ownedAttributeAggregations.get( ownedAttr ) );
        	            specJson.put( "type", "Property" );
        	            
                    elementJson = new JSONObject();
                    elementJson.put( "sysmlid", ownedAttr );
                    elementJson.put( "specialization", specJson );
        	            JSONArray elements = jsonObject.optJSONArray("elements");
        	            if (elements != null) {
        	                elements.put(elementJson);
        	            }
	            }
	        }
	    }
	}

	/**
	 * retrieves View/Product specialization > ownedAttributes. 1st attempts to
	 * retrieve from payload JSON; retrieves from alfresco if it's not found in
	 * JSON
	 * 
	 * @param parentId
	 * @param elementOwnedAttributes
	 * @return
	 */
	protected List<String> getOwnedAttributes(String parentId,
            Map<String, ArrayList<String>> elementOwnedAttributes) {
		List<String> atts = new ArrayList<String>();
		// Find in input json.
        if (elementOwnedAttributes != null) {
        		atts = elementOwnedAttributes.get(parentId);
        		if (atts != null)
        			return atts;
        }

		// Find in existing model database.
		EmsScriptNode parentNode = mp.findScriptNodeById(parentId,
				mp.myWorkspace, null, false);
		if (NodeUtil.exists(parentNode)) {
			Object ownedAttRefs = parentNode.getNodeRefProperty(
					Acm.ACM_OWNED_ATTRIBUTE, true, null, mp.myWorkspace);
			if (ownedAttRefs instanceof Collection) {
				List<NodeRef> refs = Utils.asList((Collection<?>) ownedAttRefs,
						NodeRef.class);
				atts = EmsScriptNode.getSysmlIds(EmsScriptNode
						.toEmsScriptNodeList(refs));
			}
		}
		return atts;
	}

	protected String getViewIdForOwnedAttribute(String ownedAttributeId,
			Map<String, JSONObject> elementsInJson) {
		// Find in input json
		JSONObject ownedAttributeJson = elementsInJson.get(ownedAttributeId);
		if (ownedAttributeJson != null) {
			String propType = ownedAttributeJson
					.optString(Acm.JSON_PROPERTY_TYPE);

			// TODO -- Return null if not a view?!!

			if (!Utils.isNullOrEmpty(propType)) {
				return propType;
			}
		}

		// Find in existing model database.
		EmsScriptNode ownedAttributeNode = mp.findScriptNodeById(
				ownedAttributeId, mp.myWorkspace, null, false);
		if (NodeUtil.exists(ownedAttributeNode)) {
			Object propType = ownedAttributeNode.getNodeRefProperty(
					Acm.ACM_PROPERTY_TYPE, true, null, mp.myWorkspace);
			Collection<?> propTypes = null;
			if (propType instanceof Collection) {
				propTypes = (Collection<?>) propType;
			} else if (propType instanceof NodeRef) {
				propTypes = Utils.newList(propType);
			}
			if (Utils.isNullOrEmpty(propTypes))
				return null;

			for (Object pType : propTypes) {
				if (isView(pType)) {
					if (pType instanceof NodeRef) {
						EmsScriptNode propTypeNode = new EmsScriptNode(
								(NodeRef) pType, mp.getServices());
						if (NodeUtil.exists(propTypeNode)
								&& propTypeNode
										.hasOrInheritsAspect(Acm.ACM_VIEW)) {
							return propTypeNode.getSysmlId();
						}
					} else if (pType instanceof String) {
						return (String) pType;
					}
				}
			}
		}

		return null;
	}

	public boolean isView(Object viewMaybe) {
		if (viewMaybe instanceof EmsScriptNode) {
			return ((EmsScriptNode) viewMaybe)
					.hasOrInheritsAspect(Acm.ACM_VIEW);
		} else if (viewMaybe instanceof NodeRef) {
			EmsScriptNode view = new EmsScriptNode((NodeRef) viewMaybe,
					mp.getServices());
			return view.hasOrInheritsAspect(Acm.ACM_VIEW);
		} else if (viewMaybe instanceof String) {
			EmsScriptNode node = mp.findScriptNodeById((String) viewMaybe,
					mp.myWorkspace, null, false);
			return isView(node);
		}
		return false;
	}

	public static boolean isView(Object viewMaybe, ServiceRegistry services,
			WorkspaceNode workspace) {
		if (viewMaybe instanceof EmsScriptNode) {
			return ((EmsScriptNode) viewMaybe)
					.hasOrInheritsAspect(Acm.ACM_VIEW);
		} else if (viewMaybe instanceof NodeRef) {
			EmsScriptNode view = new EmsScriptNode((NodeRef) viewMaybe,
					services);
			return view.hasOrInheritsAspect(Acm.ACM_VIEW);
		} else if (viewMaybe instanceof String) {
			EmsScriptNode node = NodeUtil.findScriptNodeById(
					(String) viewMaybe, workspace, null, false, services, null);
			return UpdateViewHierarchy.isView(node, services, workspace);
		}
		return false;
	}

	/**
	 * retrieves "childViews" JSONArray within "specialization" object. attempts
	 * to retrieve it from "specialization" wrapper object if it's not found.
	 * 
	 * @param elementJson
	 *            - "speicialization"'s wrapper object
	 * @param spec
	 *            - "specialization" object
	 * @return - (JSONArray) this view's children views
	 */
	protected JSONArray getChildViewsArray(JSONObject elementJson,
			JSONObject spec) {
		JSONArray childViewsArray = spec.optJSONArray(Acm.JSON_CHILD_VIEWS);
		// In case childViews is not in the specialization part of the
		// json, . . .
		if (childViewsArray == null) {
			childViewsArray = elementJson.optJSONArray(Acm.JSON_CHILD_VIEWS);
		}
		return childViewsArray;
	}
    /**
     * retrieves "childViews" JSONArray within "specialization" object. attempts
     * to retrieve it from "specialization" wrapper object if it's not found.
     * 
     * @param elementJson
     *            - "specialization"'s wrapper object
     * @param spec
     *            - "specialization" object
     * @return - (JSONArray) this view's children views
     */
    protected JSONArray getView2ViewArray(JSONObject elementJson,
                                          JSONObject spec) {
        JSONArray view2viewArray = spec.optJSONArray(Acm.JSON_VIEW_2_VIEW);
        
        // In case childViews is not in the specialization part of the 
        // json, . . .
        if (view2viewArray == null) {
            view2viewArray = elementJson.optJSONArray(Acm.JSON_VIEW_2_VIEW);
        }
        return view2viewArray;
    }


	/**
	 * retrieves "ownedAttributes" JSONArray within "specialization" object.
	 * attempts to retrieve it from "specialization" wrapper object if it's not
	 * found.
	 * 
	 * @param elementJson
	 *            - (JSONObject) "specialization" wrapper object
	 * @param spec
	 *            - (JSONObject) "specialization" object
	 * @return - (JSONArray)
	 */
	protected JSONArray getOwnedAttributesArray(JSONObject elementJson,
			JSONObject spec) {
		JSONArray ownedAttributesArray = spec
				.optJSONArray(Acm.JSON_OWNED_ATTRIBUTE);
		// In case ownedAttributes is not in the specialization part of
		// the json, . . .
		if (ownedAttributesArray == null) {
			ownedAttributesArray = elementJson
					.optJSONArray(Acm.JSON_OWNED_ATTRIBUTE);
		}
		return ownedAttributesArray;
	}

	protected void translateOwnedAttributeIdsToViewIds(
			List<String> ownedAttributeIds,
			Map<String, String> viewIdsForOwnedAttributeIds,
			Map<String, String> ownedAttributeIdsForViewIds) {
		if (!Utils.isNullOrEmpty(ownedAttributeIds)) {
			for (String ownedAttributeId : ownedAttributeIds) {
				String viewIdForOwnedAttribute = getViewIdForOwnedAttribute(
						ownedAttributeId, elementsInJson);
				if (!Utils.isNullOrEmpty(viewIdForOwnedAttribute)) {
					viewIdsForOwnedAttributeIds.put(ownedAttributeId,
							viewIdForOwnedAttribute);
					ownedAttributeIdsForViewIds.put(viewIdForOwnedAttribute,
							ownedAttributeId);
				}
			}
		}
	}
}
