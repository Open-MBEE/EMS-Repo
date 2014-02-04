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

import gov.nasa.jpl.view_repo.ModelLoadActionExecuter;
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

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentWriter;
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
    // when run in background as an action, this needs to be false
    private boolean runWithoutTransactions = false;
    protected String projectId;

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
    protected JSONObject relationshipsJson;

    protected Set<String> newElements;

    protected SiteInfo siteInfo;
    
    /**
     * Create or update the model as necessary based on the request
     * 
     * @param content
     *            JSONObject used to create/update the model
     * @param status
     *            Status to be updated
     * @throws JSONException
     *             Parse error
     */
    public void createOrUpdateModel(Object content, Status status)
            throws Exception {
        Date now = new Date();
        log(LogLevel.INFO, "Starting createOrUpdateModel: " + now);
        // long start, end, total = 0;

        clearCaches();

        JSONObject postJson = (JSONObject) content;

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
                    log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
                    if (!rootElement.equals((String) projectNode
                            .getProperty(Acm.ACM_CM_NAME))) {
                        String ownerName = null;
                        JSONObject element = elementMap.get(rootElement);
                        if (element.has(Acm.JSON_OWNER)) {
                            ownerName = element.getString(Acm.JSON_OWNER);
                        }
                        
                        // get the owner so we can create node inside owner
                        // DirectedRelationships can be sent with no owners, so, if not specified look for its existing owner
                        EmsScriptNode owner = null;
                        if (ownerName == null || ownerName.equals("null")) {
                            EmsScriptNode elementNode = findScriptNodeByName(rootElement);
                            if (elementNode == null) {
                                owner = projectNode;
                            } else {
                                owner = elementNode.getParent();
                            }
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
        updateOrCreateAllRelationships(relationshipsJson);
        
        now = new Date();
        log(LogLevel.INFO, "Update Model Elements Done waiting on transaction to complete..." + now + "\n");
    }
    
    protected void updateOrCreateAllRelationships(JSONObject jsonObject) throws JSONException {
        updateOrCreateRelationships(jsonObject, "relationshipElements");
        updateOrCreateRelationships(jsonObject, "propertyTypes");
        updateOrCreateRelationships(jsonObject, "elementValues");
        updateOrCreateRelationships(jsonObject, "annotatedElements");
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
        log(LogLevel.INFO, "updateOrCreateRelationships" + key + ": ");
        if (runWithoutTransactions) {
            updateOrCreateTransactionableRelationships(jsonObject, key);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                log(LogLevel.INFO, "updateOrCreateRelationships: beginning transaction {");
                updateOrCreateTransactionableRelationships(jsonObject, key);
                log(LogLevel.INFO, "} updateOrCreateRelationships committing: " + key);
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                }
            }
        }
        end = System.currentTimeMillis();
        log(LogLevel.INFO, (end - start) + "ms");
    }
    
    protected void updateOrCreateTransactionableRelationships(JSONObject jsonObject, String key) throws JSONException {
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
                    updateOrCreateAnnotatedElements(object.getJSONArray(id), id);
                }
            }
        }
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
    protected void updateOrCreateAnnotatedElements(JSONArray jsonArray, String id)
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
    protected void updateOrCreateElementValues(JSONArray jsonArray, String id)
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
    protected void updateOrCreatePropertyType(String typeId, String id) {
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
    protected void updateOrCreateRelationship(JSONObject jsonObject, String id)
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
                EmsScriptNode reifiedNode = findScriptNodeByName(pkgName); 
                if (reifiedNode == null) {
                    reifiedNode = node.getParent().createFolder(pkgName, Acm.ACM_ELEMENT_FOLDER);
                    reifiedNode.setProperty(Acm.ACM_ID, key);
                    reifiedNode.setProperty(Acm.ACM_CM_NAME, pkgName);
                    reifiedNode.setProperty(Acm.ACM_NAME, (String) node.getProperty(Acm.ACM_NAME));
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
                                log(LogLevel.INFO, "moving "
                                        + child.getProperty(Acm.ACM_CM_NAME) + " to "
                                        + reifiedNode.getProperty(Acm.ACM_CM_NAME));
                                child.move(reifiedNode);
                            }

                            // move reified containers as necessary too
                            EmsScriptNode childPkg = findScriptNodeByName(childName
                                    + REIFIED_PKG_SUFFIX);
                            if (childPkg != null && !childPkg.getParent().equals(reifiedNode)) {
                                log(LogLevel.INFO, "moving "
                                        + childPkg.getProperty(Acm.ACM_CM_NAME)
                                        + " to "
                                        + reifiedNode.getProperty(Acm.ACM_CM_NAME));
                                childPkg.move(reifiedNode);
                            }
                        } // end if (checkPermissions(child, PermissionService.WRITE)) {
                    } // end if (checkPermissions(reifiedNode, PermissionService.WRITE)) {

                    node.createOrUpdateChildAssociation(reifiedNode, Acm.ACM_REIFIED_CONTAINMENT);
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

        if (runWithoutTransactions) {
            isValid =  buildTransactionableElementMap(jsonArray);
        } else {
            UserTransaction trx;
            // building element map is a read-only transaction
            trx = services.getTransactionService().getNonPropagatingUserTransaction(true);
            try {
                trx.begin();
                log(LogLevel.INFO, "buildElementMap begin transaction {");
                isValid = buildTransactionableElementMap(jsonArray);
                log(LogLevel.INFO, "} buildElementMap committing");
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                }
                isValid = false;
            }
        }
        
        return isValid;
    }

    protected boolean buildTransactionableElementMap(JSONArray jsonArray) throws JSONException {
        boolean isValid = true;
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
        JSONArray children = new JSONArray();
        
        EmsScriptNode reifiedNode = null;
        
        if (runWithoutTransactions) {
            reifiedNode = updateOrCreateTransactionableElement(elementJson, parent, children);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                log(LogLevel.INFO, "updateOrCreateElement begin transaction {");
                reifiedNode = updateOrCreateTransactionableElement(elementJson, parent, children);
                log(LogLevel.INFO, "} updateOrCreateElement end transaction");
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
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
    }

    protected EmsScriptNode updateOrCreateTransactionableElement(JSONObject elementJson, EmsScriptNode parent, JSONArray children) throws JSONException {
        if (!elementJson.has(Acm.JSON_ID)) {
            return null;
        }
        String id = elementJson.getString(Acm.JSON_ID);
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateElement " + id);

        // TODO Need to permission check on new node creation
        // find node if exists, otherwise create
        EmsScriptNode node;
        EmsScriptNode reifiedNode = null;
        if (newElements.contains(id)) {
            log(LogLevel.INFO, "\tcreating node");
            node = parent.createNode(id, Acm.JSON2ACM.get(elementJson.getString(Acm.JSON_TYPE)));
            node.setProperty(Acm.ACM_CM_NAME, id);
            node.setProperty(Acm.ACM_ID, id);
        } else {
            log(LogLevel.INFO, "\tmodifying node");
            node = findScriptNodeByName(id);
            try {
                if (!node.getParent().equals(parent)) {
                    node.move(parent);
                    EmsScriptNode pkgNode = findScriptNodeByName(id + "_pkg");
                    if (pkgNode != null) {
                        pkgNode.move(parent);
                    }
                }
            } catch (Exception e) {
                log(LogLevel.WARNING, "could not find node information: " + id);
                e.printStackTrace();
            }
        }
        foundElements.put(id, node); // cache the found value

        // update metadata
        if (checkPermissions(node, PermissionService.WRITE)) {
            log(LogLevel.INFO, "\tinserting metadata");
            node.ingestJSON(elementJson);
        }

        if (elementHierarchyJson.has(id)) {
            log(LogLevel.INFO, "\tcreating reified package");
            reifiedNode = getOrCreateReifiedNode(node, id);
            JSONArray array = elementHierarchyJson.getJSONArray(id);
            for (int ii = 0; ii < array.length(); ii++) {
                children.put(array.get(ii));
            }
        }

        // add the relationships into our maps
        log(LogLevel.INFO, "\tfiltering relationships");
        JSONObject relations = EmsScriptNode.filterRelationsJSONObject(elementJson);
        String keys[] = { 
                "elementValues",
                "propertyTypes",
                "relationshipElements",
                "annotatedElements" };
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

        end = System.currentTimeMillis(); log(LogLevel.INFO, "\tTotal: " + (end-start) + " ms");
        return reifiedNode;
    }
    
    protected EmsScriptNode getOrCreateReifiedNode(EmsScriptNode node, String id) {
        EmsScriptNode reifiedNode = null;
        EmsScriptNode parent = node.getParent();

        if (checkPermissions(parent, PermissionService.WRITE)) {
            String pkgName = id + "_pkg";
            reifiedNode = findScriptNodeByName(pkgName);
            if (reifiedNode == null) {
                reifiedNode = parent.createFolder(pkgName, Acm.ACM_ELEMENT_FOLDER);
                reifiedNode.setProperty(Acm.ACM_ID, pkgName);
                reifiedNode.setProperty(Acm.ACM_CM_NAME, pkgName);
                reifiedNode.setProperty(Acm.ACM_NAME, (String) node.getProperty(Acm.ACM_NAME));
                log(LogLevel.INFO, "\tcreating " + pkgName + " in " + parent.getProperty(Acm.ACM_CM_NAME));
            }
            if (checkPermissions(reifiedNode, PermissionService.WRITE)) {
                foundElements.put(pkgName, reifiedNode);
                node.createOrUpdateChildAssociation(reifiedNode, Acm.ACM_REIFIED_CONTAINMENT);
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
        
        boolean runInBackground = checkArgEquals(req, "background", "true");

        if (validateRequest(req, status)) {
            try {
                if (runInBackground) {
                    saveAndStartAction(req, status);
                    response.append("JSON uploaded, model load being processed in background.\n");
                    response.append("You will be notified via email when the model load has finished.\n");
                } else {
                    createOrUpdateModel(req.parseContent(), status);
                }
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

    protected void saveAndStartAction(WebScriptRequest req, Status status) throws Exception {
        EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);

        EmsScriptNode jobPkgNode = siteNode.childByNamePath("Jobs");
        if (jobPkgNode == null) {
            jobPkgNode = siteNode.createFolder("Jobs", "cm:folder");
        }
        
        String jobNodeName = "Job " + projectId + ".json";
        EmsScriptNode jobNode = jobPkgNode.childByNamePath(jobNodeName);
        if (jobNode == null) {
            jobNode = jobPkgNode.createNode(jobNodeName, "ems:Job");
        } else {
//            String jobStatus = (String)jobNode.getProperty("ems:job_status");
//            if (jobStatus.equals("Active")) {
//                status.setCode(HttpServletResponse.SC_CONFLICT, "Previous project export is still loading");
//                return;
//            }
        }
        jobNode.createOrUpdateProperty("ems:job_status", "Active");
        jobNode.createOrUpdateProperty("cm:isContentIndexed", false);
        
        // write out the json
        NodeRef jobNodeRef = jobNode.getNodeRef();
        ContentWriter writer = services.getContentService().getWriter(jobNodeRef, ContentModel.PROP_CONTENT, true);
        writer.putContent(((JSONObject)req.parseContent()).toString(4));
        ModelLoadActionExecuter.setContentDataMimeType(writer, jobNode, "application/json", services);

        // create the log with the association
        String jobLogNodeName = "Job " + projectId + ".log";
        EmsScriptNode jobLogNode = jobPkgNode.childByNamePath(jobLogNodeName);
        if (jobLogNode == null) {
            jobLogNode = jobPkgNode.createNode(jobLogNodeName, "cm:content");
        }
        jobNode.createOrUpdateProperty("ems:job_log", jobLogNode);
        
        
        EmsScriptNode projectNode = findScriptNodeByName(projectId);
        // kick off the action
        ActionService actionService = services.getActionService();
        Action loadAction = actionService.createAction(ModelLoadActionExecuter.NAME);
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_ID, projectId);
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NAME, (String)projectNode.getProperty(Acm.ACM_NAME));
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NODE, projectNode);
        services.getActionService().executeAction(loadAction , jobNodeRef, true, true);
    }
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null) {
            // TODO - move this to ViewModelPost - really non hierarchical post
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
                e.printStackTrace();
            }

            if (projectNode == null) {
                return false;
            }

            if (checkPermissions(projectNode, PermissionService.WRITE)) {
                return false;
            }
        } else {
            String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
            // Handling for project/{id}/elements
            if (!checkRequestVariable(siteName, SITE_NAME)) {
                return false;
            }

            projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
            if (!checkRequestVariable(projectId, PROJECT_ID)) {
                return false;
            }

            siteInfo = services.getSiteService().getSite(siteName);
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

    public void setProjectIdAndNode(String projectId) {
        projectNode = findScriptNodeByName(projectId);
    }
    
    public void setProjectNode(EmsScriptNode pn) {
        projectNode = pn;
    }
    
    public void setRunWithoutTransactions(boolean withoutTransactions) {
        runWithoutTransactions = withoutTransactions;           
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