/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * 
 * U.S. Government sponsorship acknowledged.
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

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor file:
 * /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts
 * /gov/nasa/jpl/javawebscripts/model.post.desc.xml
 * 
 * NOTE: Transactions are independently managed in this Jave webscript, so make
 * sure that the descriptor file has transactions set to none
 * 
 * @author cinyoung
 * 
 *         TODO Need merge? and force? similar to View?
 * 
 */
public class ModelPost extends AbstractJavaWebScript {
    private EmsScriptNode projectNode = null;

    private final String ELEMENTS = "elements";

    /**
     * JSONObject of element hierarchy 
     * { 
     *  elementId: [childElementId, ...], 
     *  ...
     * },
     */
    private JSONObject elementHierarchyJson;

    /**
     * JSONObject of the relationships
     * "relationshipElements": {
     *      relationshipElementId: {
     *          "source": sourceElementId,
     *          "target": targetElementId
     *      },
     *      ... 
     * },
     * "propertyTypes": { 
     *      propertyElementId: typeElementId, //for Property to the property type
     *      ... 
     * },
     * "elementValues": { 
     *      propertyElementId: [elementId], //for property with ElementValue as value types, the value is a noderef
     *      ...
     * }
     */
    private JSONObject relationshipsJson;

    private Set<String> newElements;

    /**
     * Create or update the model as necessary based on the request
     * 
     * @param req
     *            Request used to create/update the model
     * @param status
     *            Status to be updated
     * @throws JSONException
     *             Parse error
     */
    private void createOrUpdateModel(WebScriptRequest req, Status status)
            throws Exception {
        Date now = new Date();
        System.out.println("Starting createOrUpdateModel: " + now);
        // long start, end, total = 0;

        // clear out the response cache first (only one instance of each
        // webscript)
        clearCaches();

        JSONObject postJson = (JSONObject) req.parseContent();

        // check if we have single element or array of elements and create
        // accordingly
        if (!postJson.has(ELEMENTS)) {
//            updateOrCreateElement(postJson.getJSONArray(ELEMENTS).getJSONObject(0), projectNode);
            updateOrCreateElement(postJson, projectNode);
        } else {
            // create the element map and hierarchies
            if (buildElementMap(postJson.getJSONArray(ELEMENTS))) {
                // start building up elements from the root elements
                for (String rootElement : rootElements) {
                    System.out.println("ROOT ELEMENT FOUND: " + rootElement);
                    if (!rootElement.equals((String) projectNode
                            .getProperty("cm:name"))) {
                        String ownerName = null;
                        JSONObject element = elementMap.get(rootElement);
                        if (element.has(Acm.JSON_OWNER)) {
                            ownerName = element.getString(Acm.JSON_OWNER);
                        }
                        
                        // get the owner so we can create node inside owner
                        EmsScriptNode owner = null;
                        if (ownerName == null || ownerName.equals("null")) {
                            owner = projectNode;
                        } else {
                            owner = findScriptNodeByName(ownerName);
                            if (owner == null) {
                                log(LogLevel.WARNING, "Could not find owner with name: " + ownerName + " putting into project", HttpServletResponse.SC_NOT_FOUND);
                                owner = projectNode;
                            }
                            // really want to add pkg as owner
                            EmsScriptNode reifiedPkg = findScriptNodeByName(ownerName
                                    + "_pkg");
                            if (reifiedPkg == null) {
                                reifiedPkg = getOrCreateReifiedNode(owner,
                                        ownerName);
                            }
                            owner = reifiedPkg;
                        }
                        
                        updateOrCreateElement(elementMap.get(rootElement), owner);
                    }
                } // end for (String rootElement: rootElements) {
            } // end if (buildElementMap(postJson.getJSONArray(ELEMENTS))) {
        }

        // handle the relationships
        updateOrCreateRelationships(relationshipsJson, "relationshipElements");
        updateOrCreateRelationships(relationshipsJson, "propertyTypes");
        updateOrCreateRelationships(relationshipsJson, "elementValues");
        updateOrCreateRelationships(relationshipsJson, "annotatedElements");

        now = new Date();
        System.out
                .println("Update Model Elements Done waiting on transaction to complete..."
                        + now + "\n");
    }

    /**
     * Update or create relationships
     * 
     * @param jsonObject
     *            Input data to generate relationships from
     * @param key
     *            The relationship type (e.g., relationshipElements,
     *            projectTypes, or elementValues)
     * @throws JSONException
     */
    protected void updateOrCreateRelationships(JSONObject jsonObject, String key)
            throws JSONException {
        long start = System.currentTimeMillis(), end;
        System.out.print("updateOrCreate" + key + ": ");
        UserTransaction trx;
        trx = services.getTransactionService().getUserTransaction();
        try {
            trx.begin();
            if (jsonObject.has(key)) {
                JSONObject object = jsonObject.getJSONObject(key);
                Iterator<?> ids = object.keys();
                while (ids.hasNext()) {
                    String id = (String) ids.next();
                    if (key.equals("relationshipElements")) {
                        updateOrCreateRelationship(object.getJSONObject(id), id);
                    } else if (key.equals("propertyTypes")) {
                        updateOrCreatePropertyType(object.getString(id), id);
                    } else if (key.equals("elementValues")) {
                        updateOrCreateElementValues(object.getJSONArray(id), id);
                    } else if (key.equals("annotatedElements")) {
                        updateOrCreateAnnotatedElements(
                                object.getJSONArray(id), id);
                    }
                }
            }
            trx.commit();
        } catch (Throwable e) {
            try {
                if (trx.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
                    trx.rollback();
                    System.out.println("\tNeeded to rollback: "
                            + e.getMessage());
                }
            } catch (Throwable ee) {
                // TODO handle double exception in whatever way is appropriate
            }
        }
        end = System.currentTimeMillis();
        System.out.println((end - start) + "ms");
    }

    /**
     * Update or create annotated elements (multiple noderefs ordered in a list)
     * 
     * @param jsonArray
     *            Array of the IDs that house the values for the element
     * @param id
     *            The ID of the element to add the values to
     * @throws JSONException
     */
    private void updateOrCreateAnnotatedElements(JSONArray jsonArray, String id)
            throws JSONException {
        EmsScriptNode source = findScriptNodeByName(id);

        if (checkPermissions(source, PermissionService.WRITE)) {
            for (int ii = 0; ii < jsonArray.length(); ii++) {
                String targetId = jsonArray.getString(ii);
                EmsScriptNode target = findScriptNodeByName(targetId);
                source.createOrUpdateAssociation(target,
                        Acm.ACM_ANNOTATED_ELEMENTS, true);
            }
        }
    }

    /**
     * Update or create element values (multiple noderefs ordered in a list)
     * 
     * @param jsonArray
     *            Array of the IDs that house the values for the element
     * @param id
     *            The ID of the element to add the values to
     * @throws JSONException
     */
    private void updateOrCreateElementValues(JSONArray jsonArray, String id)
            throws JSONException {
        // create an array of the values to be added in as the elementValue
        // property
        ArrayList<NodeRef> values = new ArrayList<NodeRef>();
        for (int ii = 0; ii < jsonArray.length(); ii++) {
            String valueId = jsonArray.getString(ii);
            EmsScriptNode value = findScriptNodeByName(valueId);
            if (value != null
                    && checkPermissions(value, PermissionService.WRITE)) {
                values.add(value.getNodeRef());
            } else {
                log(LogLevel.ERROR,
                        "could not find element value node with id " + valueId
                                + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        // only change if old list is different than new
        EmsScriptNode element = findScriptNodeByName(id);
        if (element != null
                && checkPermissions(element, PermissionService.WRITE)) {
            @SuppressWarnings("unchecked")
            ArrayList<NodeRef> oldValues = (ArrayList<NodeRef>) element
                    .getProperty(Acm.ACM_ELEMENT_VALUE);
            if (!EmsScriptNode.checkIfListsEquivalent(values, oldValues)) {
                element.setProperty(Acm.ACM_ELEMENT_VALUE, values);
            }
        } else {
            log(LogLevel.ERROR, "could not find element node with id " + id
                    + "\n", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Update or create the property type association between an element and its
     * type
     * 
     * @param typeId
     *            ID of the type
     * @param id
     *            ID of the element
     */
    private void updateOrCreatePropertyType(String typeId, String id) {
        EmsScriptNode property = findScriptNodeByName(id);
        EmsScriptNode propertyType = findScriptNodeByName(typeId);

        if (property != null && propertyType != null) {
            if (checkPermissions(property, PermissionService.WRITE)
                    && checkPermissions(propertyType, PermissionService.READ)) {
                property.createOrUpdateAssociation(propertyType, Acm.ACM_PROPERTY_TYPE);
            }
        } else {
            if (property == null) {
                log(LogLevel.ERROR, "could not find property node with id "
                        + id + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
            if (propertyType == null) {
                log(LogLevel.ERROR,
                        "could not find property type node with id " + typeId
                                + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    /**
     * Update or create the element relationship associations
     * 
     * @param jsonObject
     *            JSONObject that defines the source and target of the directed
     *            relationship
     * @param id
     *            Id of the directed relationship element
     * @throws JSONException
     */
    private void updateOrCreateRelationship(JSONObject jsonObject, String id)
            throws JSONException {
        String sourceId = jsonObject.getString(Acm.JSON_SOURCE);
        String targetId = jsonObject.getString(Acm.JSON_TARGET);

        EmsScriptNode relationship = findScriptNodeByName(id);
        EmsScriptNode source = findScriptNodeByName(sourceId);
        EmsScriptNode target = findScriptNodeByName(targetId);

        if (relationship != null && source != null && target != null) {
            if (checkPermissions(relationship, PermissionService.WRITE)
                    && checkPermissions(source, PermissionService.READ)
                    && checkPermissions(target, PermissionService.READ)) {
                relationship.createOrUpdateAssociation(source, Acm.ACM_SOURCE);
                relationship.createOrUpdateAssociation(target, Acm.ACM_TARGET);
            }
        } else {
            if (relationship == null) {
                log(LogLevel.ERROR, "could not find relationship node with id "
                        + id + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
            if (source == null) {
                log(LogLevel.ERROR, "could not find source node with id "
                        + sourceId + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
            if (target == null) {
                log(LogLevel.ERROR, "could not find target node with id "
                        + targetId + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    /**
     * Create or update the element hierarchy as specified by the JSON Object.
     * Does reification of containment
     * 
     * @param jsonObject
     *            Hierarchy of containment
     * @param key
     *            Name of the parent element
     * @throws JSONException
     */
    protected void updateOrCreateElementHierarchy(JSONObject jsonObject,
            String key) throws JSONException {
        String REIFIED_PKG_SUFFIX = "_pkg";
        String pkgName = key + REIFIED_PKG_SUFFIX;

        JSONArray array = jsonObject.getJSONArray(key);

        // only need to create reified container if it has any elements
        if (array.length() > 0) {
            // this is the element node
            EmsScriptNode node = findScriptNodeByName(key);
            if (node == null) {
                log(LogLevel.ERROR, "could not find element node with id "
                        + key + "\n", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (checkPermissions(node, PermissionService.WRITE)) {
                // create reified container if it doesn't exist
                EmsScriptNode reifiedNode = findScriptNodeByName(pkgName); // this
                                                                           // is
                                                                           // the
                                                                           // reified
                                                                           // element
                                                                           // package
                if (reifiedNode == null) {
                    reifiedNode = node.getParent().createFolder(pkgName,
                            Acm.ACM_ELEMENT_FOLDER);
                    reifiedNode.setProperty(Acm.ACM_ID, key);
                    reifiedNode.setProperty("cm:name", pkgName);
                    reifiedNode.setProperty(Acm.ACM_NAME,
                            (String) node.getProperty(Acm.ACM_NAME));
                }
                if (checkPermissions(reifiedNode, PermissionService.WRITE)) {
                    foundElements.put(pkgName, reifiedNode);
                    // make sure element and reified container in same place
                    // node should be accurate if hierarchy is correct
                    if (!reifiedNode.getParent().equals(node.getParent())) {
                        reifiedNode.move(node.getParent());
                    }

                    // move elements to reified container if not already there
                    for (int ii = 0; ii < array.length(); ii++) {
                        String childName = array.getString(ii);
                        EmsScriptNode child = findScriptNodeByName(childName);
                        if (child == null) {
                            log(LogLevel.ERROR,
                                    "could not find child node with id "
                                            + childName + "\n",
                                    HttpServletResponse.SC_BAD_REQUEST);
                            continue;
                        }
                        if (checkPermissions(child, PermissionService.WRITE)) {
                            if (!child.getParent().equals(reifiedNode)) {
                                System.out.println("moving "
                                        + child.getProperty("cm:name") + " to "
                                        + reifiedNode.getProperty("cm:name"));
                                child.move(reifiedNode);
                            }

                            // move reified containers as necessary too
                            EmsScriptNode childPkg = findScriptNodeByName(childName
                                    + REIFIED_PKG_SUFFIX);
                            if (childPkg != null
                                    && !childPkg.getParent()
                                            .equals(reifiedNode)) {
                                System.out.println("moving "
                                        + childPkg.getProperty("cm:name")
                                        + " to "
                                        + reifiedNode.getProperty("cm:name"));
                                childPkg.move(reifiedNode);
                            }
                        } // end if (checkPermissions(child,
                          // PermissionService.WRITE)) {
                    } // end if (checkPermissions(reifiedNode,
                      // PermissionService.WRITE)) {

                    node.createOrUpdateChildAssociation(reifiedNode,
                            Acm.ACM_REIFIED_CONTAINMENT);
                } // end if (checkPermissions(node, PermissionService.WRITE)) {
            }
        }
    }

    Map<String, JSONObject> elementMap = new HashMap<String, JSONObject>();
    Set<String> rootElements = new HashSet<String>();

    /**
     * Builds up the element map and hierarchy and returns true if valid
     * @param jsonArray         Takes in the elements JSONArray
     * @return                  True if all elements and owners can be found with write permissions, false otherwise
     */
    protected boolean buildElementMap(JSONArray jsonArray) throws JSONException {
        boolean isValid = true;
        UserTransaction trx;

        // lets look for everything initially
        trx = services.getTransactionService().getUserTransaction();
        try {
            trx.begin();
            for (int ii = 0; ii < jsonArray.length(); ii++) {
                JSONObject elementJson = jsonArray.getJSONObject(ii);
                String sysmlId = elementJson.getString(Acm.JSON_ID);
                elementMap.put(sysmlId, elementJson);

                if (findScriptNodeByName(sysmlId) == null) {
                    newElements.add(sysmlId);
                }

                // create the hierarchy
                if (elementJson.has(Acm.JSON_OWNER)) {
                    String ownerId = elementJson.getString(Acm.JSON_OWNER);
                    // if owner is null, leave at project root level
                    if (ownerId == null || ownerId.equals("null")) {
                        ownerId = (String) projectNode.getProperty(Acm.ACM_ID);
                        rootElements.add(sysmlId);
                    }
                    if (!elementHierarchyJson.has(ownerId)) {
                        elementHierarchyJson.put(ownerId, new JSONArray());
                    }
                    elementHierarchyJson.getJSONArray(ownerId).put(sysmlId);
                } else {
                    // if no owners are specified, add directly to root elements
                    rootElements.add(sysmlId);
                }
            }

            // lets iterate through elements
           for (String elementId: elementMap.keySet()) {
                if (!newElements.contains(elementId)) {
                    EmsScriptNode element = findScriptNodeByName(elementId);
                    if (element == null) {
                        log(LogLevel.ERROR, "Could not find node with id: " + elementId, HttpServletResponse.SC_NOT_FOUND);
                        isValid = false;
                    } else if (!checkPermissions(element, PermissionService.WRITE)) {
                        isValid = false;
                    }
                }
            }
            
            fillRootElements();
            trx.commit();
        } catch (Throwable e) {
            try {
                if (trx.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
                    trx.rollback();
                    System.out.println("\tNeeded to rollback: "
                            + e.getMessage());
                }
            } catch (Throwable ee) {
                // TODO handle double exception in whatever way is appropriate
            }
        }
        
        return isValid;
    }

    protected void fillRootElements() throws JSONException {
        Iterator<?> iter = elementHierarchyJson.keys();
        while (iter.hasNext()) {
            String ownerId = (String) iter.next();
            if (!elementMap.containsKey(ownerId)) {
                JSONArray hierarchy = elementHierarchyJson
                        .getJSONArray(ownerId);
                for (int ii = 0; ii < hierarchy.length(); ii++) {
                    rootElements.add(hierarchy.getString(ii));
                }
            }
        }
    }

    /**
     * Update or create element with specified metadata
     * 
     * @param jsonObject
     *            Metadata to be added to element
     * @param key
     *            ID of element
     * @throws JSONException
     */
    protected void updateOrCreateElement(JSONObject elementJson,
            EmsScriptNode parent) throws JSONException {
        if (!elementJson.has(Acm.JSON_ID)) {
            return;
        }
        String id = elementJson.getString(Acm.JSON_ID);
        long start = System.currentTimeMillis(), end;
        System.out.println("updateOrCreateElement " + id);
        JSONArray children = new JSONArray();
        EmsScriptNode reifiedNode = null;

        // find node if exists, otherwise create
        EmsScriptNode node;
        UserTransaction trx;
        trx = services.getTransactionService().getUserTransaction();
        try {
            trx.begin();
            if (newElements.contains(id)) {
                // System.out.println("\tupdateOrCreateElement creating: " + id
                // + " under " + parent.getQnamePath());
                node = parent.createNode(id,
                        Acm.JSON2ACM.get(elementJson.getString(Acm.JSON_TYPE)));
                node.setProperty("cm:name", id);
                node.setProperty(Acm.ACM_ID, id);
                // TODO temporarily set title - until we figure out how to use
                // sysml:name in repository browser
                if (elementJson.has("name")) {
                    node.setProperty("cm:title", elementJson.getString("name"));
                }
                // System.out.println("\t  created path: " +
                // node.getQnamePath());
            } else {
                node = findScriptNodeByName(id);
                try {
                    // System.out.println("\tupdateOrCreateElement found: " + id
                    // + " => " + node.getQnamePath() + " => " +
                    // node.getSiteShortName());
                    if (!node.getParent().equals(parent)) {
                        // System.out.println("\tupdateOrCreateElement moving element to new parent");
                        node.move(parent);
                    }
                } catch (Exception e) {
                    System.out
                            .println("could not find node information: " + id);
                    e.printStackTrace();
                }
            }
            foundElements.put(id, node); // cache the found value

            if (checkPermissions(node, PermissionService.WRITE)) {
                // injecting the JSON will return the relationships found
                // System.out.println("\tupdateOrCreateElement updating metadata");
                node.ingestJSON(elementJson);
            }

            if (elementHierarchyJson.has(id)) {
                // System.out.println("\tupdateOrCreateElement creating reification");
                reifiedNode = getOrCreateReifiedNode(node, id);
                children = elementHierarchyJson.getJSONArray(id);
            } // end if (elementHierarchyJson.has(id)) {
            trx.commit();
        } catch (Throwable e) {
            try {
                if (trx.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
                    trx.rollback();
                    System.out.println("\tNeeded to rollback: "
                            + e.getMessage());
                }
            } catch (Throwable ee) {
                System.out.println("\tRollback failed: " + ee.getMessage());
            }
        }
        end = System.currentTimeMillis();
        
        System.out.println("\tupdateOrCreateElement: "
                + ": " + (end - start) + "ms");

        // add the relationships into our maps
        JSONObject relations = EmsScriptNode
                .filterRelationsJSONObject(elementJson);
        String keys[] = { "elementValues", "propertyTypes",
                "relationshipElements", "annotatedElements" };
        for (String key : keys) {
            if (!relationshipsJson.has(key)) {
                relationshipsJson.put(key, new JSONObject());
            }
            if (relations.has(key)) {
                JSONObject json = relations.getJSONObject(key);
                Iterator<?> iter = json.keys();
                while (iter.hasNext()) {
                    String iterId = (String) iter.next();
                    relationshipsJson.getJSONObject(key).put(iterId,
                            json.get(iterId));
                }
            }
        }

        // create the children elements
        if (reifiedNode != null) {
            for (int ii = 0; ii < children.length(); ii++) {
                updateOrCreateElement(elementMap.get(children.getString(ii)),
                        reifiedNode);
            }
        }

        // long end = System.currentTimeMillis(); System.out.println("\tTotal: "
        // + (end-start));
    }

    protected EmsScriptNode getOrCreateReifiedNode(EmsScriptNode node, String id) {
        EmsScriptNode reifiedNode = null;
        EmsScriptNode parent = node.getParent();

        if (checkPermissions(parent, PermissionService.WRITE)) {
            // System.out.println("Creating reified package for " +
            // node.getProperty(Acm.ACM_NAME));
            String pkgName = id + "_pkg";
            reifiedNode = findScriptNodeByName(pkgName);
            if (reifiedNode == null) {
                reifiedNode = parent.createFolder(pkgName,
                        Acm.ACM_ELEMENT_FOLDER);
                // reifiedNode.setProperty(Acm.ACM_ID, id);
                reifiedNode.setProperty("cm:name", pkgName);
                reifiedNode.setProperty(Acm.ACM_NAME,
                        (String) node.getProperty(Acm.ACM_NAME));
            }
            if (checkPermissions(reifiedNode, PermissionService.WRITE)) {
                foundElements.put(pkgName, reifiedNode);
                // node.createOrUpdateChildAssociation(reifiedNode,
                // Acm.ACM_REIFIED_CONTAINMENT);
            }
        }

        return reifiedNode;
    }

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();

        if (validateRequest(req, status)) {
            try {
                createOrUpdateModel(req, status);
            } catch (Exception e) {
                log(LogLevel.ERROR, "JSON malformed\n",
                        HttpServletResponse.SC_BAD_REQUEST);
                e.printStackTrace();
            }
        }

        status.setCode(responseStatus.getCode());
        model.put("res", response.toString());
        return model;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        String elementId = req.getServiceMatch().getTemplateVars()
                .get("elementid");
        if (elementId != null) {
            if (!checkRequestVariable(elementId, "elementid")) {
                return false;
            }

            // projectNode should be the owner..., which should exist
            try {
                JSONObject postJson = (JSONObject) req.parseContent();
                JSONObject elementsJson = postJson.getJSONObject("elements");
                JSONObject elementJson = elementsJson.getJSONObject(elementId);
                projectNode = findScriptNodeByName(elementJson
                        .getString(Acm.JSON_OWNER));
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (projectNode == null) {
                return false;
            }

            if (checkPermissions(projectNode, PermissionService.WRITE)) {
                return false;
            }
        } else {
            String siteName = req.getServiceMatch().getTemplateVars()
                    .get(SITE_NAME);
            if (!checkRequestVariable(siteName, SITE_NAME)) {
                return false;
            }

            String projectId = req.getServiceMatch().getTemplateVars()
                    .get(PROJECT_ID);
            if (!checkRequestVariable(projectId, PROJECT_ID)) {
                return false;
            }

            SiteInfo siteInfo = services.getSiteService().getSite(siteName);
            if (!checkRequestVariable(siteInfo, "Site")) {
                return false;
            }

            if (!checkPermissions(siteInfo.getNodeRef(),
                    PermissionService.WRITE)) {
                return false;
            }

            EmsScriptNode siteNode = getSiteNode(siteName);
            projectNode = siteNode.childByNamePath("/ViewEditor/" + projectId);
            if (projectNode == null) {
                log(LogLevel.ERROR, "Project not found.\n",
                        HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
        }

        return true;
    }

    @Override
    protected void clearCaches() {
        super.clearCaches();
        elementHierarchyJson = new JSONObject();
        relationshipsJson = new JSONObject();
        rootElements = new HashSet<String>();
        elementMap = new HashMap<String, JSONObject>();
        newElements = new HashSet<String>();
    }
}