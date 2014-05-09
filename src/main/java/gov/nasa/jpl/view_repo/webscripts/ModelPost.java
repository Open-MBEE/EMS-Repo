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

import gov.nasa.jpl.ae.event.Call;
import gov.nasa.jpl.ae.event.ConstraintExpression;
import gov.nasa.jpl.ae.event.Expression;
import gov.nasa.jpl.ae.event.ParameterListenerImpl;
import gov.nasa.jpl.ae.solver.Constraint;
import gov.nasa.jpl.ae.solver.ConstraintLoopSolver;
import gov.nasa.jpl.ae.sysml.SystemModelSolver;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.ae.util.ClassData;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.namespace.QName;
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
 * NOTE: Transactions are independently managed in this Java webscript, so make
 * sure that the descriptor file has transactions set to none
 * 
 * @author cinyoung
 * 
 *         TODO Need merge? and force? similar to View?
 * 
 */
public class ModelPost extends AbstractJavaWebScript {
    public ModelPost() {
        super();
    }
    
    public ModelPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


//    private EmsScriptNode projectNode = null;
    // when run in background as an action, this needs to be false
    private boolean runWithoutTransactions = false;
//    protected String projectId;

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
     * @return the created elements
     * @throws JSONException
     *             Parse error
     */
    public Set< EmsScriptNode >
            createOrUpdateModel( Object content, Status status,
                                 EmsScriptNode projectNode ) throws Exception {
        Date now = new Date();
        log(LogLevel.INFO, "Starting createOrUpdateModel: " + now);
        long start = System.currentTimeMillis(), end, total = 0;

        clearCaches();

        JSONObject postJson = (JSONObject) content;

        boolean singleElement = !postJson.has(ELEMENTS);
        
        TreeSet<EmsScriptNode> elements =
                new TreeSet<EmsScriptNode>();
        
        // create the element map and hierarchies
        if (buildElementMap(postJson.getJSONArray(ELEMENTS), projectNode)) {
            // start building up elements from the root elements
            for (String rootElement : rootElements) {
                log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
                if (!rootElement.equals((String) projectNode.getProperty(Acm.CM_NAME))) {
                    
                    EmsScriptNode owner = null;

                    UserTransaction trx;
                    trx = services.getTransactionService().getNonPropagatingUserTransaction();
                    try {
                        trx.begin();
                        owner = getOwner(rootElement, projectNode, true);
                        trx.commit();
                    } catch (Throwable e) {
                        try {
                            trx.rollback();
                            log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                            log(LogLevel.ERROR, "\t####### when calling getOwner(" + rootElement + ", " + projectNode + ", true)");
                            e.printStackTrace();
                        } catch (Throwable ee) {
                            log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                            log(LogLevel.ERROR, "\tafter calling getOwner(" + rootElement + ", " + projectNode + ", true)");
                            ee.printStackTrace();
//                                e.printStackTrace();
//                                int cnt = 0;
//                                while ( e != e.getCause() && e.getCause() != null && cnt++ < 5) {
//                                    e.getCause().printStackTrace();
//                                    e = e.getCause();
//                                }
                        }
                    }
                    
                    if (owner != null && owner.exists()) {
                        elements.addAll( updateOrCreateElement( elementMap.get( rootElement ),
                                                             owner, false ) );
                    }
                }
            } // end for (String rootElement: rootElements) {
        } // end if (buildElementMap(postJson.getJSONArray(ELEMENTS))) {
    
        // handle the relationships
        updateOrCreateAllRelationships(relationshipsJson);
        
        elements.addAll( updateNodeReferences( singleElement, postJson,
                                               projectNode ) );

        now = new Date();
        end = System.currentTimeMillis();
        total = end -start;
        log(LogLevel.INFO, "createOrUpdateModel completed" + now + " : " +  total + "ms\n");
        return elements;
    }
    
    protected Set<EmsScriptNode> updateNodeReferences(boolean singleElement,
                                                      JSONObject postJson,
                                                      EmsScriptNode projectNode ) throws Exception {
        TreeSet<EmsScriptNode> elements =
                new TreeSet<EmsScriptNode>();

        if ( singleElement ) {
            elements.addAll( updateOrCreateElement(postJson, projectNode, true) );
        }
        for (String rootElement : rootElements) {
            log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
            if (!rootElement.equals((String) projectNode.getProperty(Acm.CM_NAME))) {
                EmsScriptNode owner = getOwner( rootElement, projectNode, false );
                
                try {
                    elements.addAll( updateOrCreateElement( elementMap.get( rootElement ),
                                                            owner, true ) );
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        } // end for (String rootElement: rootElements) {
        return elements;
    }

    protected EmsScriptNode getOwner( String elementId,
                                      EmsScriptNode projectNode,
                                      boolean createOwnerPkgIfNotFound ) {
        JSONObject element = elementMap.get(elementId);
        if ( element == null || element.equals( "null" ) ) {
            log(LogLevel.ERROR, "Trying to get owner of null element!",
                HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        String ownerName = null;
        if (element.has(Acm.JSON_OWNER)) {
            try{
                ownerName = element.getString(Acm.JSON_OWNER);
            } catch ( JSONException e ) {
                e.printStackTrace();
            }
        }
        
        // get the owner so we can create node inside owner
        // DirectedRelationships can be sent with no owners, so, if not
        // specified look for its existing owner
        EmsScriptNode owner = null;
        EmsScriptNode reifiedPkg = null;
        if (Utils.isNullOrEmpty( ownerName ) ) {
            EmsScriptNode elementNode = findScriptNodeById(elementId);
            if (elementNode == null || !elementNode.exists()) {
                owner = projectNode;
            } else {
                owner = elementNode.getParent();
            }
        } else {
       		boolean foundOwnerElement = true;
            owner = findScriptNodeById(ownerName);
            if (owner == null || !owner.exists()) {
                log( LogLevel.WARNING, "Could not find owner with name: "
                                       + ownerName + " putting " + elementId
                                       + " into project: " + projectNode,
                     HttpServletResponse.SC_BAD_REQUEST );
                owner = projectNode;
                foundOwnerElement = false;
            }
            // really want to add pkg as owner
            reifiedPkg = findScriptNodeById(ownerName + "_pkg");
            if (reifiedPkg == null || !reifiedPkg.exists()) {
                if ( createOwnerPkgIfNotFound) {
                    // If we found the owner element, then it exists but not its
                    // reified package, so we need the reified package to be
                    // created in the same folder as the owner element, so pass
                    // true into useParent parameter. Else, it's owner is the
                    // project folder, the actual folder in which to create the
                    // pkg, so pass false.
                    reifiedPkg = getOrCreateReifiedNode(owner, ownerName,
                                                        foundOwnerElement);
                } else {
                    log( LogLevel.WARNING, "Could not find owner package: "
                                           + ownerName,
                         HttpServletResponse.SC_NOT_FOUND );
                }
            }
            owner = reifiedPkg;
        }
//        log( LogLevel.INFO, "\tgetOwner(" + elementId + "): json element=("
//                            + element + "), ownerName=" + ownerName
//                            + ", reifiedPkg=(" + reifiedPkg + ", projectNode=("
//                            + projectNode + "), returning owner=" + owner );
        return owner;
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
                    e.printStackTrace();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                    e.printStackTrace();
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
        EmsScriptNode source = findScriptNodeById(id);

        if (checkPermissions(source, PermissionService.WRITE)) {
            for (int ii = 0; ii < jsonArray.length(); ii++) {
                String targetId = jsonArray.getString(ii);
                EmsScriptNode target = findScriptNodeById(targetId);
                if (target != null) {
                    source.createOrUpdateAssociation(target, Acm.ACM_ANNOTATED_ELEMENTS, true);
                }
            }
        }
    }

    /**
     * TODO this may be outdated.  ElementValue is not longer a property.
     * 		these should be done for ValueSpecification types
     * 
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
        EmsScriptNode element = findScriptNodeById(id);
        element.createOrUpdateProperties( jsonArray, Acm.ACM_ELEMENT_VALUE );
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
        EmsScriptNode property = findScriptNodeById(id);
        EmsScriptNode propertyType = findScriptNodeById(typeId);

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

        EmsScriptNode relationship = findScriptNodeById(id);
        EmsScriptNode source = findScriptNodeById(sourceId);
        EmsScriptNode target = findScriptNodeById(targetId);

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


    Map<String, JSONObject> elementMap = new HashMap<String, JSONObject>();
    Set<String> rootElements = new HashSet<String>();

    protected WebScriptRequest lastReq = null;

    /**
     * Builds up the element map and hierarchy and returns true if valid
     * @param jsonArray         Takes in the elements JSONArray
     * @return                  True if all elements and owners can be found with write permissions, false otherwise
     */
    protected boolean buildElementMap(JSONArray jsonArray, EmsScriptNode projectNode) throws JSONException {
        boolean isValid = true;

        if (runWithoutTransactions) {
            isValid =  buildTransactionableElementMap(jsonArray, projectNode);
        } else {
            UserTransaction trx;
            // building element map is a read-only transaction
            trx = services.getTransactionService().getNonPropagatingUserTransaction(true);
            try {
                trx.begin();
                log(LogLevel.INFO, "buildElementMap begin transaction {");
                isValid = buildTransactionableElementMap(jsonArray, projectNode);
                log(LogLevel.INFO, "} buildElementMap committing");
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                    e.printStackTrace();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                    e.printStackTrace();
                }
                isValid = false;
            }
        }
        
        return isValid;
    }

    protected boolean buildTransactionableElementMap(JSONArray jsonArray, EmsScriptNode projectNode) throws JSONException {
        boolean isValid = true;
        for (int ii = 0; ii < jsonArray.length(); ii++) {
            JSONObject elementJson = jsonArray.getJSONObject(ii);
            if (!elementJson.has(Acm.JSON_ID)) {
                elementJson.put( Acm.JSON_ID, createId( services ) );
                //return null;
            }
            String sysmlId = null;
            try {
                sysmlId = elementJson.getString( Acm.JSON_ID );
            } catch ( JSONException e ) {
                // ignore
            }
            if ( sysmlId == null ) {

                log( LogLevel.ERROR, "No id in element json!",
                     HttpServletResponse.SC_NOT_FOUND );
                continue;
            }
            elementMap.put(sysmlId, elementJson);

            if (findScriptNodeById(sysmlId) == null) {
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
                EmsScriptNode element = findScriptNodeById(elementId);
                if (element == null) {
                    log(LogLevel.ERROR, "Could not find node with id: " + elementId, HttpServletResponse.SC_BAD_REQUEST);
                } else if (!checkPermissions(element, PermissionService.WRITE)) {
                		// do nothing, just log inside of checkPermissions
                }
            }
        }
        
       	if (isValid) {
    	   		isValid = fillRootElements();
       	}
        
        return isValid;
    }

    protected boolean fillRootElements() throws JSONException {
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
        
        for (String name: rootElements) {
        		EmsScriptNode rootElement = findScriptNodeById(name);
        		if (rootElement != null) {
	        		if (!checkPermissions(rootElement, PermissionService.WRITE)) {
	        			log(LogLevel.WARNING, "\tskipping as root element since no write permissions", HttpServletResponse.SC_BAD_REQUEST);
	        		}
        		}
        }
        return true;
    }

    /**
     * Update or create element with specified metadata
     * 
     * @param jsonObject
     *            Metadata to be added to element
     * @param key
     *            ID of element
     * @return the created elements
     * @throws JSONException
     */
    protected Set<EmsScriptNode> updateOrCreateElement(JSONObject elementJson,
                                         EmsScriptNode parent,
                                         boolean ingest) throws Exception {
        TreeSet<EmsScriptNode> elements =
                new TreeSet<EmsScriptNode>();

        EmsScriptNode element = null;
        
        Object jsonId = elementJson.get( Acm.JSON_ID );
        element = findScriptNodeById( "" + jsonId );
        if ( element != null ) {
            elements.add( element );
        }
        
		// check that parent is of folder type
        if ( parent == null ) {
            Debug.error("null parent for elementJson: " + elementJson );
            return elements;
        }
        if ( !parent.exists() ) {
            Debug.error("non-existent parent (" + parent + ") for elementJson: " + elementJson );
            return elements;
        }
        if ( !parent.isFolder() ) {
			String name = (String) parent.getProperty(Acm.ACM_NAME);
			if (name == null) {
				name = (String) parent.getProperty(Acm.CM_NAME);
			}
			String id = (String) parent.getProperty(Acm.ACM_ID);
			if (id == null) {
				id = "not sysml type";
			}
			log(LogLevel.WARNING, "Node " + name + " is not of type folder, so cannot create children [id=" + id + "]");
			return elements;
		}

        JSONArray children = new JSONArray();
        
        EmsScriptNode reifiedNode = null;

        if (runWithoutTransactions) {
            reifiedNode =
                    updateOrCreateTransactionableElement( elementJson, parent,
                                                          children, ingest );
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                log(LogLevel.INFO, "updateOrCreateElement begin transaction {");
                reifiedNode =
                        updateOrCreateTransactionableElement( elementJson,
                                                              parent, children,
                                                              ingest );
                log(LogLevel.INFO, "} updateOrCreateElement end transaction");
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log( LogLevel.ERROR,
                         "\t####### ERROR: Needed to rollback: " + e.getMessage() );
                    e.printStackTrace();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                    e.printStackTrace();
                }
            }
        }

        // create the children elements
        if (reifiedNode != null && reifiedNode.exists()) {
            //elements.add( reifiedNode ); 
            for (int ii = 0; ii < children.length(); ii++) {
                elements.addAll( 
                updateOrCreateElement(elementMap.get(children.getString(ii)),
                                                       reifiedNode, ingest) );
            }
        }
        
        return elements;
    }

    protected EmsScriptNode
            updateOrCreateTransactionableElement( JSONObject elementJson,
                                                  EmsScriptNode parent,
                                                  JSONArray children,
                                                  boolean ingest ) throws Exception {
        if (!elementJson.has(Acm.JSON_ID)) {
            elementJson.put( Acm.JSON_ID, createId( services ) );
            //return null;
        }
        String id = elementJson.getString(Acm.JSON_ID);
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateElement " + id);

        // TODO Need to permission check on new node creation
        // find node if exists, otherwise create
        EmsScriptNode node = findScriptNodeById( id );
        if ( node != null ) {
            node.setResponse( getResponse() );
            node.setStatus( getResponseStatus() );
        }
        EmsScriptNode reifiedNode = null;

        String jsonType = elementJson.getString( Acm.JSON_TYPE );
        String acmSysmlType = null;
        String type = null;
        if ( jsonType != null ) acmSysmlType = Acm.getJSON2ACM().get( jsonType );
        type = NodeUtil.getContentModelTypeName( acmSysmlType, services );
        
        if ( node == null || !node.exists() ) {// && newElements.contains( id ) ) {
            if ( type == null || type.trim().isEmpty() ) {
                System.out.println( "PREFIX: type not found for " + jsonType );
                return null;
            } else {
                log( LogLevel.INFO, "\tcreating node" );
                try {
                    node = parent.createSysmlNode( id, acmSysmlType );
                    if ( node == null || !node.exists() ) {
                        throw new Exception( "createNode() failed." );
                    }
                    node.setProperty( Acm.CM_NAME, id );
                    node.setProperty( Acm.ACM_ID, id );
                } catch ( Exception e ) {
                    System.out.println( "Got exception in "
                                        + "updateOrCreateTransactionableElement(elementJson="
                                        + elementJson + ", parent=("
                                        + parent + "), children=(" + children
                                        + ")), calling parent.createNode(id=" + id + ", " + type + ")" );
                    throw e;
                }
            }
        } else {
            log(LogLevel.INFO, "\tmodifying node");
            // TODO -- Need to be able to handle changed type unless everything
            // is an element and only aspects are used for subclassing.
            try {
                if (node != null && node.exists() ) {
                    if (!node.getParent().equals(parent)) {
                        node.move(parent);
                        EmsScriptNode pkgNode = findScriptNodeById(id + "_pkg");
                        if (pkgNode != null) {
                            pkgNode.move(parent);
                        }
                    }
                    if ( !type.equals( acmSysmlType )
                            && NodeUtil.isAspect( acmSysmlType ) ) {
                        node.createOrUpdateAspect( acmSysmlType );
                    }
                }
            } catch (Exception e) {
                log(LogLevel.WARNING, "could not find node information: " + id);
                e.printStackTrace();
            }
        }
        boolean nodeExists = node != null && node.exists(); 
        if (id != null && nodeExists ) {
            foundElements.put(id, node); // cache the found value
        }

        // update metadata
        if (ingest && nodeExists && checkPermissions(node, PermissionService.WRITE)) {
            log(LogLevel.INFO, "\tinserting metadata");
            node.ingestJSON(elementJson);
        }

        if (nodeExists && elementHierarchyJson.has(id)) {
            log(LogLevel.INFO, "\tcreating reified package");
            reifiedNode = getOrCreateReifiedNode(node, id, true); // TODO -- Is last argument correct?
            
            JSONArray array = elementHierarchyJson.getJSONArray(id);
            if ( array != null ) {
            	// GG: removed !ingest so children can have their metadata inserted
                //for (int ii = 0; !ingest && ii < array.length(); ii++) {
                for (int ii = 0; ii < array.length(); ii++) {
                    children.put(array.get(ii));
                }
            }
        }

        // add the relationships into our maps
        // REVIEW -- Should we skip this if the node or reified node does not exist?
        // REVIEW -- Should we throw an exception if the node or reified node does not exist?
        log(LogLevel.INFO, "\tfiltering relationships");
        JSONObject relations = EmsScriptNode.filterRelationsJSONObject(elementJson);
        String keys[] = { 
                "elementValues",
                "propertyTypes",
                "relationshipElements",
                "annotatedElements" };
        if ( !ingest ) for (String key : keys) {
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
    
    public static String createId( ServiceRegistry services ) {
        for ( int i=0; i<10; ++i ) {
            String id = "MMS_" + System.currentTimeMillis();
            // Make sure id is not already used
            if ( NodeUtil.findNodeRefById( id, services ) == null ) {
                return id;
            }
        }
        Debug.error( true, "Could not create a unique id!" );
        return null;
    }

    protected EmsScriptNode getOrCreateReifiedNode(EmsScriptNode node, String id, boolean useParent) {
        EmsScriptNode reifiedNode = null;
        if ( node == null || !node.exists() ) {
            log( LogLevel.ERROR,
                 "Trying to create reified node for missing node! id = " + id );
            return null;
        }
        EmsScriptNode parent;
        if (useParent) {
            parent= node.getParent();
        } else {
            parent = node;
        }
        if ( parent == null || !parent.exists() ) {
            log( LogLevel.ERROR,
                 "Trying to create reified node folder in missing parent folder for node " + node );
            return null;
        }

        if (checkPermissions(parent, PermissionService.WRITE)) {
            String pkgName = id + "_pkg";
            reifiedNode = findScriptNodeById(pkgName);
            if (reifiedNode == null || !reifiedNode.exists()) {
                try {
                    log( LogLevel.ERROR,
                         "\t trying to create reified node " + pkgName
                                 + " in parent, "
                                 + parent.getProperty( Acm.CM_NAME ) + " = "
                                 + parent + "." );
                    reifiedNode = parent.createFolder(pkgName, Acm.ACM_ELEMENT_FOLDER);
                } catch ( Throwable e ) {
                    log( LogLevel.ERROR,
                         "\t failed to create reified node " + pkgName
                                 + " in parent, "
                                 + parent.getProperty( Acm.CM_NAME ) + " = "
                                 + parent + " because of exception." );
                    throw e; // pass it up the chain to roll back transaction
                }
                if (reifiedNode == null || !reifiedNode.exists()) {
                    log( LogLevel.ERROR,
                         "\t failed to create reified node " + pkgName
                                 + " in parent, "
                                 + parent.getProperty( Acm.CM_NAME ) + " = "
                                 + parent );
                    return null;
                } else {
                    reifiedNode.setProperty(Acm.ACM_ID, pkgName);
                    reifiedNode.setProperty(Acm.CM_NAME, pkgName);
                    if ( useParent ) {
                        reifiedNode.setProperty(Acm.ACM_NAME, (String) node.getProperty(Acm.ACM_NAME));
                    } else {
                        reifiedNode.setProperty( Acm.ACM_NAME, pkgName.replaceAll( "_pkg$", "" ) );
                    }
                    log(LogLevel.INFO, "\tcreating " + pkgName + " in " + parent.getProperty(Acm.CM_NAME) + " : " + reifiedNode.getNodeRef().toString());
                }
            }
            if (checkPermissions(reifiedNode, PermissionService.WRITE)) {
                foundElements.put(pkgName, reifiedNode);
                // this reification containment association is more trouble than it's worth...
//                node.createOrUpdateChildAssociation(reifiedNode, Acm.ACM_REIFIED_CONTAINMENT);
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
        printHeader( req );

        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();

        boolean runInBackground = checkArgEquals(req, "background", "true");
        boolean fix = checkArgEquals(req, "fix", "true");

        if (fix) {
            log(LogLevel.INFO, "Constraint violations will be fixed if found!");
        }
        
        ModelPost instance = new ModelPost(repository, services);
        
        JSONObject top = new JSONObject();
        if (validateRequest(req, status)) {
            try {
                if (runInBackground) {
                    instance.saveAndStartAction(req, status);
                    response.append("JSON uploaded, model load being processed in background.\n");
                    response.append("You will be notified via email when the model load has finished.\n");
                } else {
                    JSONObject postJson = (JSONObject)req.parseContent();
                    Set< EmsScriptNode > elements = 
                        instance.createOrUpdateModel( postJson,
                                                      status,
                                                      getProjectNodeFromRequest( req ) );
                    if ( !Utils.isNullOrEmpty( elements ) ) {
                        
                        // Fix constraints if desired:
                        if (fix) {
                            
                            // TODO 
                            //      should be doing a lucene search to get all of the constraints and see if the elements
                        	// 		posted are in that constraint.  Cant assume user will post the constraint.
                            
                            EmsSystemModel systemModel = new EmsSystemModel(this.services);
                            
                            SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAe = 
                                    new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( systemModel );
                            
                            SystemModelSolver< EmsScriptNode, EmsScriptNode, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode >  solver = 
                                    new SystemModelSolver< EmsScriptNode, EmsScriptNode, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode >(systemModel, new ConstraintLoopSolver() );
                            
                            Collection<Constraint> constraints = new ArrayList<Constraint>();
                            
                            // Loop through all of the elements and create constraints
                            // for the found Constraints:
                            for (EmsScriptNode element : elements) {
                                
                                String type = systemModel.getTypeString(element, null);
                                
                                // If it is a constraint then add it to our constraints:
                                if (type != null && type.equals( Acm.JSON_CONSTRAINT )) {
                                    
                                    // Get the constraint expression:
                                    Collection<EmsScriptNode> expressions = systemModel.getProperty( element, Acm.JSON_CONSTRAINT_SPECIFICATION );
                                    
                                    if (!Utils.isNullOrEmpty( expressions )) {
                                        
                                        // This should always be of size 1:
                                        EmsScriptNode exprNode = expressions.iterator().next();
                                        
                                        Expression<Call> expressionCall = sysmlToAe.toAeExpression( exprNode );
                                        Call call = (Call) expressionCall.expression;
                                        Expression<Boolean> expression = new Expression<Boolean>(call.evaluate(true, false));
                                        
                                        if (expression != null) {
                                            
                                            constraints.add(new ConstraintExpression( expression ));
                                        }
                                    }
                                    
                                } // end if it is a constraint
                            } 
                            
                            // Solve the constraints:
                            if (!Utils.isNullOrEmpty( constraints )) {
                                
                                // Add all of the Parameter constraints:
                                ClassData cd = sysmlToAe.getClassData();
                                
                                // Loop through all the listeners:
                                for (ParameterListenerImpl listener : cd.getAeClasses().values()) {
                                    
                                    // TODO: REVIEW
                                    //       Can we get duplicate ParameterListeners in the aeClassses map?
                                    constraints.addAll( listener.getConstraints( true, null ) );
                                }
                            
                                // Solve!!!!
                                Debug.turnOn();
                                boolean result = solver.solve(constraints);
                                Debug.turnOff();
                                
                                if (!result) {
                                    log( LogLevel.ERROR, "Was not able to solve all of the constraints!" );
                                }
                                else {
                                    log( LogLevel.INFO, "Solved all of the constraints!" );
                                }
                                
                            }
                            
                        } // end if fixing constraints
                        
                        JSONArray elementsJson = new JSONArray();
                        for ( EmsScriptNode element : elements ) {
                            elementsJson.put( element.toJSONObject() );
                        }
                        top.put( "elements", elementsJson );
                        model.put( "res", top.toString( 4 ) );
                    }
                }
                appendResponseStatusInfo(instance);
            } catch (JSONException e) {
                log(LogLevel.ERROR, "JSON malformed\n", HttpServletResponse.SC_BAD_REQUEST);
                e.printStackTrace();
                model.put("res", response.toString());
            } catch (Exception e) {
                log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                e.printStackTrace();
                model.put("res", response.toString());
            }
        }
        if ( !model.containsKey( "res" ) ) {
            model.put( "res", response.toString() );
        }
        status.setCode(responseStatus.getCode());

        printFooter();

        return model;
    }

    protected void saveAndStartAction(WebScriptRequest req, Status status) throws Exception {
        //String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        //SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        EmsScriptNode siteNode = new EmsScriptNode(getSiteInfo(req).getNodeRef(), services, response);
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        
        String jobName = "Load Job " + projectId + ".json";
        EmsScriptNode jobNode = ActionUtil.getOrCreateJob(siteNode, jobName, "ems:Job", status, response);
        
        // write out the json
        ActionUtil.saveStringToFile(jobNode, "application/json", services, ((JSONObject)req.parseContent()).toString(4));
        
        EmsScriptNode projectNode = findScriptNodeById(projectId);
        // kick off the action
        ActionService actionService = services.getActionService();
        Action loadAction = actionService.createAction(ModelLoadActionExecuter.NAME);
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_ID, projectId);
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NAME, (String)projectNode.getProperty(Acm.ACM_NAME));
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NODE, projectNode);
        services.getActionService().executeAction(loadAction , jobNode.getNodeRef(), true, true);
    }
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        setSiteInfo( req );

        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null) {
            // TODO - move this to ViewModelPost - really non hierarchical post
            if (!checkRequestVariable(elementId, "elementid")) {
                return false;
            }
        } else {
            // Handling for project/{id}/elements
            if (!checkRequestVariable(siteName, SITE_NAME)) {
                return false;
            }

            if (!checkRequestVariable(siteInfo, "Site")) {
                return false;
            }
        }
        
        // check project
        EmsScriptNode projectNode = getProjectNodeFromRequest(req);
        if (projectNode == null) {
            log(LogLevel.ERROR, "Project not found.\n", HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        
        return true;
    }

    private EmsScriptNode getProjectNodeFromRequest(WebScriptRequest req) {
        EmsScriptNode projectNode = null;
        
        String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        EmsScriptNode siteNode = getSiteNode(siteName);
        projectNode = siteNode.childByNamePath("/Models/" + projectId);
        if (projectNode == null) {
                // for backwards compatibility
                projectNode = siteNode.childByNamePath("/ViewEditor/" + projectId);
        }

        if ( projectNode == null ) {
            String elementId =
                    req.getServiceMatch().getTemplateVars().get( "elementid" );
            if ( elementId != null ) {
                
                // projectNode is the node with the element id, right?
                projectNode = findScriptNodeById( elementId );
                
                if ( projectNode == null ) {
                    // projectNode should be the owner..., which should exist
                    try {
                        JSONObject postJson = (JSONObject)req.parseContent();
                        JSONObject elementsJson =
                                postJson.getJSONObject( "elements" );
                        JSONObject elementJson =
                                elementsJson.getJSONObject( elementId );
                        projectNode =
                                findScriptNodeById( elementJson.getString( Acm.JSON_OWNER ) );
                    } catch ( JSONException e ) {
                        e.printStackTrace();
                    }
                }
            } else {}
        }
        return projectNode;
    }
    
    public void setRunWithoutTransactions(boolean withoutTransactions) {
        runWithoutTransactions = withoutTransactions;           
    }
    
    public void setSiteInfo( WebScriptRequest req ) {
        if ( req == null ) return;
        String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        siteInfo = services.getSiteService().getSite(siteName);
    }
    public SiteInfo getSiteInfo() {
        return getSiteInfo( null );
    }
    public SiteInfo getSiteInfo( WebScriptRequest req ) {
        if ( req == null ) return siteInfo;
        if ( lastReq == req ) {
            if ( siteInfo != null ) return siteInfo;
        }
        setSiteInfo( req );
        return siteInfo;
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
