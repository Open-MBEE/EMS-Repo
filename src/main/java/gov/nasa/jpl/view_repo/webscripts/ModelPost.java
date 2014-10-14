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
import gov.nasa.jpl.ae.event.Parameter;
import gov.nasa.jpl.ae.event.ParameterListenerImpl;
import gov.nasa.jpl.ae.solver.Constraint;
import gov.nasa.jpl.ae.solver.ConstraintLoopSolver;
import gov.nasa.jpl.mbee.util.Random;
import gov.nasa.jpl.ae.sysml.SystemModelSolver;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.ae.util.ClassData;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.ModStatus;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
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


    // Set the flag to time events that occur during a model post using the timers
    // below
    private static boolean timeEvents = false;
    private Timer timerCommit = null;
    private Timer timerIngest = null;
    private Timer timerUpdateModel = null;
    private Timer timerToJson = null;

    // when run in background as an action, this needs to be false
    private boolean runWithoutTransactions = false;

    private final String ELEMENTS = "elements";

    /**
     * JSONObject of element hierarchy
     * {
     *  elementId: [childElementId, ...],
     *  ...
     * },
     */
    private JSONObject elementHierarchyJson;

    private EmsSystemModel systemModel;

    private SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel > sysmlToAe;


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

    protected boolean prettyPrint = true;
    
    
    private EmsSystemModel getSystemModel() {
        if ( systemModel == null ) {
            systemModel = new EmsSystemModel(this.services);
        }
        return systemModel;
    }
    private SystemModelToAeExpression getSystemModelAe() {
        if ( sysmlToAe == null ) {
            setSystemModelAe();
        }
        return sysmlToAe;
    }
    private void setSystemModelAe() {

        sysmlToAe =
        		new SystemModelToAeExpression< EmsScriptNode, EmsScriptNode, String, Object, EmsSystemModel >( getSystemModel() );

    }

    /**
     * Keep track of update elements
     */
    Set<Version> changeSet = new HashSet<Version>();

    /**
     * Create or update the model as necessary based on the request
     *
     * @param content
     *            JSONObject used to create/update the model
     * @param status
     *            Status to be updated
     * @param workspaceId
     * @return the created elements
     * @throws JSONException
     *             Parse error
     */

    public Set< EmsScriptNode >
            createOrUpdateModel( Object content, Status status,
                                 EmsScriptNode projectNode, WorkspaceNode targetWS, WorkspaceNode sourceWS ) throws Exception {
    	JSONObject postJson = (JSONObject) content;
    	
    	JSONArray updatedArray = postJson.optJSONArray("updatedElements");
		JSONArray movedArray = postJson.optJSONArray("movedElements");
		JSONArray addedArray = postJson.optJSONArray("addedElements");
		JSONArray elementsArray = postJson.optJSONArray("elements");
		
		Collection<JSONArray> collections = new ArrayList<JSONArray>();
		if(updatedArray != null){
		    if(!(updatedArray.length() == 0 ))
		        collections.add(updatedArray);
		}
		
		if(movedArray != null){
		    if(!(movedArray.length() == 0))
		        collections.add(movedArray);
		}
		
		if(addedArray != null){
		    if(!(addedArray.length() == 0))
		        collections.add(addedArray);
		}
		
		if(!(elementsArray == null))
			collections.add(elementsArray);
		TreeSet<EmsScriptNode> elements = new TreeSet< EmsScriptNode >();

		for(JSONArray jsonArray : collections){
			JSONObject object = new JSONObject();
			object.put("elements", jsonArray);
			elements.addAll(createOrUpdateModel2(object, status, projectNode, targetWS, sourceWS));
		}
    	return elements;
    }
    public Set< EmsScriptNode >
    		createOrUpdateModel2( Object content, Status status,
    								EmsScriptNode projectNode, WorkspaceNode targetWS, WorkspaceNode sourceWS ) throws Exception {
        Date now = new Date();
        log(LogLevel.INFO, "Starting createOrUpdateModel: " + now);
        long start = System.currentTimeMillis(), end, total = 0;

        log( LogLevel.DEBUG, "****** NodeUtil.doCaching = " + NodeUtil.doCaching );
        
        if(sourceWS == null)
            setWsDiff( targetWS );
        else
            setWsDiff(targetWS, sourceWS, null, null);


        clearCaches();

        JSONObject postJson = (JSONObject) content;

        boolean singleElement = !postJson.has(ELEMENTS);

        TreeSet<EmsScriptNode> elements =
                new TreeSet<EmsScriptNode>();
        TreeMap<String, EmsScriptNode> nodeMap =
                new TreeMap< String, EmsScriptNode >();

        timerUpdateModel= Timer.startTimer(timerUpdateModel, timeEvents);
        
        // create the element map and hierarchies
        if (buildElementMap(postJson.getJSONArray(ELEMENTS), projectNode, targetWS)) {
            // start building up elements from the root elements
            for (String rootElement : rootElements) {
                log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
                if (projectNode == null || !rootElement.equals(projectNode.getProperty(Acm.CM_NAME))) {

                    EmsScriptNode owner = null;


                    UserTransaction trx;
                    trx = services.getTransactionService().getNonPropagatingUserTransaction();
                    try {
                        trx.begin();
                        owner = getOwner(rootElement, projectNode, targetWS, true);
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
                        }
                    }



                    // Create element, owner, and reified package folder as
                    // necessary and place element with owner; don't update
                    // properties on this first pass.
                    if (owner != null && owner.exists()) {
                        Set< EmsScriptNode > updatedElements =
                                updateOrCreateElement( elementMap.get( rootElement ),
                                                       owner, targetWS, false );
                        for ( EmsScriptNode node : updatedElements ) {
                            nodeMap.put(node.getName(), node);
                        }
                        elements.addAll( updatedElements );
                    }
                }
            } // end for (String rootElement: rootElements) {
        } // end if (buildElementMap(postJson.getJSONArray(ELEMENTS))) {
        
        Timer.stopTimer(timerUpdateModel, "!!!!! createOrUpdateModel(): main loop time", timeEvents);

        // handle the relationships
        updateOrCreateAllRelationships(relationshipsJson, targetWS);

        // make another pass through the elements and update their properties
        Set< EmsScriptNode > updatedElements = updateNodeReferences( singleElement, postJson,
                                               projectNode, targetWS );
        for ( EmsScriptNode node : updatedElements ) {
            nodeMap.put(node.getName(), node);
        }
        elements.addAll( updatedElements );

        now = new Date();
        end = System.currentTimeMillis();
        total = end - start;
        log(LogLevel.INFO, "createOrUpdateModel completed" + now + " : " +  total + "ms\n");

        timerUpdateModel = Timer.startTimer(timerUpdateModel, timeEvents);

        // Send deltas to all listeners
        if (wsDiff.isDiff()) {
            JSONObject deltaJson = wsDiff.toJSONObject( new Date(start), new Date(end) );
            String wsId = "master";
            if (targetWS != null) {
                wsId = targetWS.getId();
            }
            // FIXME: Need to split by projectId
            if ( !sendDeltas(deltaJson, wsId, elements.first().getProjectId()) ) {
                log(LogLevel.WARNING, "createOrUpdateModel deltas not posted properly");
            }

            // Commit history
            String siteName = null;
            if (projectNode != null) {
                EmsScriptNode siteNode = projectNode.getSiteNode();
                siteName = siteNode.getName();
            }
            CommitUtil.commit( deltaJson, targetWS, siteName,
                               "", false, services, response );
        }

        Timer.stopTimer(timerUpdateModel, "!!!!! createOrUpdateModel(): Deltas time", timeEvents);

        elements = new TreeSet< EmsScriptNode >( nodeMap.values() );
        return elements;
    }


    protected Set<EmsScriptNode> updateNodeReferences(boolean singleElement,
                                                      JSONObject postJson,
                                                      EmsScriptNode projectNode,
                                                      WorkspaceNode workspace) throws Exception {
        TreeSet<EmsScriptNode> elements =
                new TreeSet<EmsScriptNode>();

        if ( singleElement ) {
            elements.addAll( updateOrCreateElement( postJson, projectNode,
                                                    workspace, true ) );
        }
        for (String rootElement : rootElements) {
            log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
            if (projectNode == null || !rootElement.equals(projectNode.getProperty(Acm.CM_NAME))) {
                EmsScriptNode owner = getOwner( rootElement, projectNode, workspace, false );

                try {
                    elements.addAll( updateOrCreateElement( elementMap.get( rootElement ),
                                                            owner, workspace, true ) );
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        } // end for (String rootElement: rootElements) {
        return elements;
    }

    protected EmsScriptNode getOwner( String elementId,
                                      EmsScriptNode projectNode,
                                      WorkspaceNode workspace,
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
            EmsScriptNode elementNode = findScriptNodeById(elementId, workspace, null, false);
            if (elementNode == null || !elementNode.exists()) {
            	// Place elements with no owner in a holding_bin_<site>_<project> package:
                //owner = projectNode; 
            	String projectNodeId = ((projectNode == null || projectNode.getSysmlId() == null) ? NO_PROJECT_ID : projectNode.getSysmlId());
            	// siteInfo will be null if the site was created just for the workspace, but this should never happen.  In that case, can
            	// try using getSiteName(req).
            	String siteName = ((siteInfo == null || siteInfo.getShortName() == null) ? NO_SITE_ID : getSiteInfo().getShortName() );
            	ownerName = "holding_bin_"+siteName+"_"+projectNodeId;  
            } else {
                owner = elementNode.getParent();
            }
        } 
        
        if (!Utils.isNullOrEmpty(ownerName)) {
       		boolean foundOwnerElement = true;
            owner = findScriptNodeById(ownerName, workspace, null, false);
            if (owner == null || !owner.exists()) {
                // FIX: Need to respond with warning that owner couldn't be found?
                log( LogLevel.WARNING, "Could not find owner with name: "
                                       + ownerName + " putting " + elementId
                                       + " into project: " + projectNode);
                owner = projectNode;  
                foundOwnerElement = false;
            }
            // really want to add pkg as owner
            reifiedPkg = findScriptNodeById(ownerName + "_pkg", workspace, null, false);
            if (reifiedPkg == null || !reifiedPkg.exists()) {
                if ( createOwnerPkgIfNotFound) {
                    // If we found the owner element, then it exists but not its
                    // reified package, so we need the reified package to be
                    // created in the same folder as the owner element, so pass
                    // true into useParent parameter. Else, it's owner is the
                    // project folder, the actual folder in which to create the
                    // pkg, so pass false.
                    reifiedPkg = getOrCreateReifiedNode(owner, ownerName, workspace,
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

    protected void updateOrCreateAllRelationships(JSONObject jsonObject, WorkspaceNode workspace) throws JSONException {
        updateOrCreateRelationships(jsonObject, "relationshipElements", workspace);
        updateOrCreateRelationships(jsonObject, "propertyTypes", workspace);
        updateOrCreateRelationships(jsonObject, "elementValues", workspace);
        updateOrCreateRelationships(jsonObject, "annotatedElements", workspace);
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
    protected void updateOrCreateRelationships(JSONObject jsonObject, String key,
                                               WorkspaceNode workspace)
            throws JSONException {
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateRelationships" + key + ": ");
        if (runWithoutTransactions) {
            updateOrCreateTransactionableRelationships(jsonObject, key, workspace);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                log(LogLevel.INFO, "updateOrCreateRelationships: beginning transaction {");
                updateOrCreateTransactionableRelationships(jsonObject, key, workspace);
                log(LogLevel.INFO, "} updateOrCreateRelationships committing: " + key);
                timerCommit = Timer.startTimer(timerCommit, timeEvents);
                trx.commit();
                Timer.stopTimer(timerCommit, "!!!!! updateOrCreateRelationships(): commit time", timeEvents);
            } catch (Throwable e) {
                try {
                    if (e instanceof JSONException) {
	                		log(LogLevel.ERROR, "updateOrCreateRelationships: JSON malformed: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
	                } else {
	                		log(LogLevel.ERROR, "updateOrCreateRelationships: DB transaction failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	                }
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                    e.printStackTrace();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tupdateOrCreateRelationships: rollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                    e.printStackTrace();
                }
            }
        }
        end = System.currentTimeMillis();
        log(LogLevel.INFO, (end - start) + "ms");
    }

    protected void updateOrCreateTransactionableRelationships(JSONObject jsonObject, String key, WorkspaceNode workspace) throws JSONException {
        if (jsonObject.has(key)) {
            JSONObject object = jsonObject.getJSONObject(key);
            Iterator<?> ids = object.keys();
            while (ids.hasNext()) {
                String id = (String) ids.next();
                if (key.equals("relationshipElements")) {
                    updateOrCreateRelationship(object.getJSONObject(id), id, workspace);
                } else if (key.equals("propertyTypes")) {
                    updateOrCreatePropertyType(object.getString(id), id, workspace);
                } else if (key.equals("elementValues")) {
                    updateOrCreateElementValues(object.getJSONArray(id), id, workspace);
                } else if (key.equals("annotatedElements")) {
                    updateOrCreateAnnotatedElements(object.getJSONArray(id), id, workspace);
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
    protected void updateOrCreateAnnotatedElements(JSONArray jsonArray, String id, WorkspaceNode workspace)
            throws JSONException {
        EmsScriptNode source = findScriptNodeById(id, workspace, null, false);

        if (checkPermissions(source, PermissionService.WRITE)) {
            for (int ii = 0; ii < jsonArray.length(); ii++) {
                String targetId = jsonArray.getString(ii);
                EmsScriptNode target = findScriptNodeById(targetId, workspace, null, false);
                if (target != null) {
                    source.createOrUpdateAssociation(target, Acm.ACM_ANNOTATED_ELEMENTS, true);
                }
            }
        }
    }

    /**
     * TODO this may be outdated.  ElementValue is no longer a property.
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
    protected void updateOrCreateElementValues(JSONArray jsonArray, String id, WorkspaceNode workspace)
            throws JSONException {
        EmsScriptNode element = findScriptNodeById(id, workspace, null, true);
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
    protected void updateOrCreatePropertyType(String typeId, String id, WorkspaceNode workspace) {
        EmsScriptNode property = findScriptNodeById(id, workspace, null, true);
        EmsScriptNode propertyType = findScriptNodeById(typeId, workspace, null, true);

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
    protected void updateOrCreateRelationship(JSONObject jsonObject, String id, WorkspaceNode workspace)
            throws JSONException {
        String sourceId = jsonObject.getString(Acm.JSON_SOURCE);
        String targetId = jsonObject.getString(Acm.JSON_TARGET);

        EmsScriptNode relationship = findScriptNodeById(id, workspace, null, true);
        EmsScriptNode source = findScriptNodeById(sourceId, workspace, null, true);
        EmsScriptNode target = findScriptNodeById(targetId, workspace, null, true);

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
    protected boolean buildElementMap(JSONArray jsonArray, EmsScriptNode projectNode, WorkspaceNode workspace) throws JSONException {
        boolean isValid = true;

        if (runWithoutTransactions) {
            isValid =  buildTransactionableElementMap(jsonArray, projectNode, workspace);
        } else {
            UserTransaction trx;
            // building element map is a read-only transaction
            trx = services.getTransactionService().getNonPropagatingUserTransaction(true);
            try {
                trx.begin();
                log(LogLevel.INFO, "buildElementMap begin transaction {");
                isValid = buildTransactionableElementMap(jsonArray, projectNode, workspace);
                log(LogLevel.INFO, "} buildElementMap committing");
                timerCommit = Timer.startTimer(timerCommit, timeEvents);
                trx.commit();
                Timer.stopTimer(timerCommit, "!!!!! buildElementMap(): commit time", timeEvents);
            } catch (Throwable e) {
                try {
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                    if (e instanceof JSONException) {
	                		log(LogLevel.ERROR, "buildElementMap: JSON malformed: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
	                } else {
	                		log(LogLevel.ERROR, "buildElementMap: DB transaction failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	                }
                    trx.rollback();
                    e.printStackTrace();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tbuildElementMap: rollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                    e.printStackTrace();
                }
                isValid = false;
            }
        }

        return isValid;
    }

    protected boolean buildTransactionableElementMap( JSONArray jsonArray,
                                                      EmsScriptNode projectNode,
                                                      WorkspaceNode workspace )
                                                              throws JSONException {
        boolean isValid = true;

        for (int ii = 0; ii < jsonArray.length(); ii++) {
            JSONObject elementJson = jsonArray.getJSONObject(ii);

            // If element does not have a ID, then create one for it using the alfresco id (cm:id):
            if (!elementJson.has(Acm.JSON_ID)) {
                elementJson.put( Acm.JSON_ID, NodeUtil.createId( services ) );
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

            if (findScriptNodeById(sysmlId, workspace, null, true) == null) {
                newElements.add(sysmlId);
            }

            // create the hierarchy
            if (elementJson.has(Acm.JSON_OWNER)) {
                String ownerId = elementJson.getString(Acm.JSON_OWNER);
                // if owner is null, leave at project root level
                if (ownerId == null || ownerId.equals("null")) {
                    if ( projectNode != null ) {
                        ownerId = (String) projectNode.getProperty(Acm.ACM_ID);
                    } else {
                        // If project is null, put it in NO_PROJECT.

                        // TODO -- REVIEW -- this probably deserves a warning--we should never get here, right?
                        ownerId = NO_PROJECT_ID;
                        EmsScriptNode noProjectNode = findScriptNodeById( ownerId, workspace, null, false );
                        if ( noProjectNode == null ) {
                            String siteName =
                                    (getSiteInfo() == null ? NO_SITE_ID : getSiteInfo().getShortName() );
                            ProjectPost pp = new ProjectPost( repository, services );
                            pp.updateOrCreateProject( new JSONObject(),
                                                      workspace, NO_PROJECT_ID,
                                                      siteName, true, false );
                        }
                    }
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
                EmsScriptNode element = findScriptNodeById(elementId, workspace, null, true);
                if (element == null) {
                    log(LogLevel.ERROR, "Could not find node with id: " + elementId, HttpServletResponse.SC_BAD_REQUEST);
                } else if (!checkPermissions(element, PermissionService.WRITE)) {
                		// do nothing, just log inside of checkPermissions
                }
            }
        }

       	if (isValid) {
    	   		isValid = fillRootElements(workspace);
       	}

        return isValid;
    }

    protected boolean fillRootElements(WorkspaceNode workspace) throws JSONException {
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
        		EmsScriptNode rootElement = findScriptNodeById(name, workspace, null, true);
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
     * @param workspace
     *
     * @param jsonObject
     *            Metadata to be added to element
     * @param key
     *            ID of element
     * @return the created elements
     * @throws JSONException
     */
    protected Set< EmsScriptNode > updateOrCreateElement( JSONObject elementJson,
                                                          EmsScriptNode parent,
                                                          WorkspaceNode workspace,
                                                          boolean ingest)
                                                                  throws Exception {
        TreeSet<EmsScriptNode> elements = new TreeSet<EmsScriptNode>();
        TreeMap<String, EmsScriptNode> nodeMap =
                new TreeMap< String, EmsScriptNode >();

        EmsScriptNode element = null;

        if ( !elementJson.has( Acm.JSON_ID ) ) {
            return elements;
        }
        String jsonId = elementJson.getString( Acm.JSON_ID );
        element = findScriptNodeById( jsonId, workspace, null, true );
        if ( element != null ) {
            elements.add( element );
            nodeMap.put( element.getName(), element );
            // only add to original element map if it exists on first pass
            if (!ingest) {
                if (!wsDiff.getElements().containsKey( jsonId )) {
                    wsDiff.getElements().put( jsonId, element );
                    wsDiff.getElementsVersions().put( jsonId, element.getHeadVersion());
                }
            }
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

        // Check to see if the element has been updated since last read by the
        // posting application.
        // Only generate error on first pass (i.e. when ingest == false).
       if ( !ingest && inConflict( element, elementJson ) ) {
            String msg =
                    "Error! Tried to post concurrent edit to element, "
                            + element + ".\n";
            if ( getResponse() == null || getResponseStatus() == null ) {
                Debug.error( msg );
            } else {
                getResponse().append( msg );
                if ( getResponseStatus() != null ) {
                    getResponseStatus().setCode( HttpServletResponse.SC_CONFLICT,
                                                 msg );
                }
            }
            return elements;
       }

        JSONArray children = new JSONArray();

        EmsScriptNode reifiedNode = null;
        ModStatus modStatus = new ModStatus();

        if (runWithoutTransactions) {
            reifiedNode =
                    updateOrCreateTransactionableElement( elementJson, parent,
                                                          children, workspace, ingest, false, modStatus );
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                log(LogLevel.INFO, "updateOrCreateElement begin transaction {");
                reifiedNode =
                        updateOrCreateTransactionableElement( elementJson,
                                                              parent, children,
                                                              workspace,
                                                              ingest, false, modStatus );
                log(LogLevel.INFO, "} updateOrCreateElement end transaction");
                timerCommit = Timer.startTimer(timerCommit, timeEvents);
                trx.commit();
                Timer.stopTimer(timerCommit, "!!!!! updateOrCreateElement(): commit time", timeEvents);
            } catch (Throwable e) {
                try {
                    if (e instanceof JSONException) {
                    		log(LogLevel.ERROR, "updateOrCreateElement: JSON malformed: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
                    } else {
                    		log(LogLevel.ERROR, "updateOrCreateElement: DB transaction failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                    e.printStackTrace();
                    trx.rollback();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tupdateOrCreateElement: rollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                    e.printStackTrace();
                }
            }
        }

        // create the children elements
        if (reifiedNode != null && reifiedNode.exists()) {
            //elements.add( reifiedNode );
            for (int ii = 0; ii < children.length(); ii++) {
                Set< EmsScriptNode > childElements =
                        updateOrCreateElement(elementMap.get(children.getString(ii)),
                                                       reifiedNode, workspace, ingest);
                // Elements in new workspace replace originals.
                for ( EmsScriptNode node : childElements ) {
                    nodeMap.put( node.getName(), node );
                }
            }
        }

        element = findScriptNodeById( jsonId, workspace, null, true );
        if (runWithoutTransactions) {
            updateTransactionableWsState(element, jsonId, modStatus, ingest);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                timerCommit = Timer.startTimer(timerCommit, timeEvents);
                updateTransactionableWsState( element, jsonId, modStatus, ingest );
                trx.commit();
                Timer.stopTimer(timerCommit, "!!!!! updateOrCreateElement(): ws metadata time", timeEvents);
            } catch (Throwable e) {
                try {
                    log(LogLevel.ERROR, "updateOrCreateElement: DB transaction failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    e.printStackTrace();
                    trx.rollback();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tupdateOrCreateElement: rollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                    e.printStackTrace();
                }
            }
        }
        
        elements = new TreeSet< EmsScriptNode >( nodeMap.values() );
        return elements;
    }

    
    private void updateTransactionableWsState(EmsScriptNode element, String jsonId, ModStatus modStatus, boolean ingest) {
        if (element != null && (element.exists() || element.isDeleted())) {
            // can't add the node JSON yet since properties haven't been tied in yet
            switch (modStatus.getState()) {
                case ADDED:
                    if (!ingest) {
                        wsDiff.getAddedElements().put( jsonId, element );
                        element.createOrUpdateAspect( "ems:Added" );
                    }
                    break;
                case UPDATED:
                    if (ingest && !wsDiff.getAddedElements().containsKey( jsonId )) {
                        element.removeAspect( "ems:Moved" );

                        if (element.hasAspect( "ems:Deleted" )) {
                            wsDiff.getAddedElements().put( jsonId,  element );
                            element.removeAspect( "ems:Deleted" );
                            element.removeAspect( "ems:Updated" );
                            element.createOrUpdateAspect( "ems:Added" );
                        } else {
                            element.removeAspect( "ems:Added" );
                            wsDiff.getUpdatedElements().put( jsonId, element );
                            element.createOrUpdateAspect( "ems:Updated" );
                        }
                    }
                    break;
                case MOVED:
                    if (!ingest && !wsDiff.getAddedElements().containsKey( jsonId )) {
                        element.removeAspect( "ems:Updated" );
                        if (element.hasAspect( "ems:Deleted" )) {
                            wsDiff.getAddedElements().put( jsonId,  element );
                            element.removeAspect( "ems:Deleted" );
                            element.removeAspect( "ems:Moved" );
                            element.createOrUpdateAspect( "ems:Added" );
                        } else {
                            element.removeAspect( "ems:Added" );
                            wsDiff.getMovedElements().put( jsonId, element );
                            element.createOrUpdateAspect( "ems:Moved" );
                        }
                    }
                    break;
                case UPDATED_AND_MOVED:
                    if (ingest && !wsDiff.getAddedElements().containsKey( jsonId )) {
                        if (element.hasAspect( "ems:Deleted" )) {
                            wsDiff.getAddedElements().put( jsonId,  element );
                            element.removeAspect( "ems:Deleted" );
                            element.removeAspect( "ems:Moved" );
                            element.removeAspect( "ems:Updated" );
                            element.createOrUpdateAspect( "ems:Added" );
                        } else {
                            element.removeAspect( "ems:Added" );
                            wsDiff.getUpdatedElements().put( jsonId, element );
                            element.createOrUpdateAspect( "ems:Updated" );

                            wsDiff.getMovedElements().put( jsonId, element );
                            element.createOrUpdateAspect( "ems:Moved" );
                        }
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }
    /**
     * Special processing for Expression and Property elements.  Modifies the passed elementJson
     * or specializeJson.
     *
     * @param type
     * @param nestedNode
     * @param elementJson
     * @param specializeJson
     * @param node
     * @param ingest
     * @param reifiedNode
     * @param parent
     * @param id
     * @throws Exception
     */
    private boolean processExpressionOrProperty(String type, boolean nestedNode, JSONObject elementJson,
    										 JSONObject specializeJson, EmsScriptNode node,
    										 boolean ingest, EmsScriptNode reifiedNode,
    										 EmsScriptNode parent, String id,
    										 WorkspaceNode workspace) throws Exception {
        // TODO REVIEW
        //		Wanted to do a lot of processing in buildTransactionElementMap(), so that we make the
        //		node a owner and in the elementHierachyJson, so that the children will be processed
        //		normally instead of having the code below.  That solution was not a neat as desired either
        //		b/c you need the node itself to retrieve its properties, to see if it already has value or
        //		operand property values stored.  This would involve duplicating a lot of the above code to
        //		create a node if needed, etc.
        //
        //		This may be easier if the operand and values objects have their own sysmlid in them, so we dont
        //		need to create one ourselves.  Brad didnt want this, but perhaps we should?

        // If it is a value or operand property then need to convert
        // the elementJson to just contain the sysmlid for the nodes,
        // instead of the nodes themselves.  Also, need to create
        // or modify nodes the properties map to.

        boolean changed = false;

        // If it is a nested node then it doesnt have a specialize property
        JSONObject jsonToCheck = nestedNode ? elementJson : specializeJson;

        // If it is a Property or Expression and json has the properties of interest:
        if ( (type.equals(Acm.ACM_PROPERTY) && jsonToCheck != null && jsonToCheck.has(Acm.JSON_VALUE)) ||
        	 (type.equals(Acm.ACM_EXPRESSION) && jsonToCheck != null && jsonToCheck.has(Acm.JSON_OPERAND)) ) {
            	boolean isProperty = type.equals(Acm.ACM_PROPERTY);
            	String jsonKey = isProperty ? Acm.JSON_VALUE : Acm.JSON_OPERAND;
            	Collection<EmsScriptNode> oldVals = isProperty ?
            											Utils.asList(getSystemModel().getValue(node, null), EmsScriptNode.class) :
            											getSystemModel().getProperty(node, Acm.ACM_OPERAND);

            JSONArray newVals = jsonToCheck.getJSONArray(jsonKey);
            Iterator<EmsScriptNode> iter = !Utils.isNullOrEmpty(oldVals) ?
            									oldVals.iterator() : null;
            ArrayList<String> nodeNames = new ArrayList<String>();

            // Check for workspace disagreement in arguments.
            WorkspaceNode nodeWorkspace = node.getWorkspace();
            if (nodeWorkspace != null && !nodeWorkspace.equals(workspace)) {
                if ( workspace == null ) {
                    workspace = node.getWorkspace();
                } else {
                    log( LogLevel.WARNING,
                         "Property owner's workspace ("
                                 + node.getWorkspaceName()
                                 + ") and specified workspace for property ("
                                 + workspace.getName()
                                 + ") are different!" );
                }
            }

            // Compare the existing values to the new ones
            // in the JSON element.  Assume that they maintain the
            // same ordering.  If there are more values in the
            // JSON element, then make new nodes for them.
            for (int i = 0; i < newVals.length(); ++i) {
            	    Object newVal = newVals.optJSONObject(i);

                	// Get the sysmlid of the old value if it exists:
                	if (iter != null && iter.hasNext()) {
                		EmsScriptNode oldValNode = iter.next();

              			nodeNames.add(oldValNode.getName());

                        if ( workspace != null
                             && !workspace.equals( oldValNode.getWorkspace() ) ) {
                            EmsScriptNode newNode =
                                    workspace.replicateWithParentFolders( oldValNode );
                            //EmsScriptNode newNode = oldValNode.clone( node );
                            //newNode.setWorkspace( workspace, oldValNode.getNodeRef() );
                            oldValNode = newNode;
              			}

                        JSONObject newValJson = (JSONObject) newVal;
                        // types are mutually exclusive so put in right aspect
                        if (newValJson.has( "type" )) {
                            if (oldValNode.createOrUpdateAspect(newValJson.getString( "type" ))) {
                                changed = true;
                            }
                        }

                        // Ingest the JSON for the value to update properties
                        timerIngest = Timer.startTimer(timerIngest, timeEvents);
                        if ( oldValNode.ingestJSON( newValJson ) ) {
                            changed = true;
                        }
                        Timer.stopTimer(timerIngest, "!!!!! processExpressionOrProperty(): ingestJSON time", timeEvents);

                	}
                	// Old value doesnt exists, so create a new node:
                	else {

                		//	The refiedNode will be null if the node is not in the elementHierachy, which
                		//	will be the case if no other elements have it as a owner, so in that case
                		//	we make a reifiedNode for it here.  If all of that fails, then use the parent
                		EmsScriptNode nestedParent = null;
                		if (reifiedNode == null) {
                			 EmsScriptNode reifiedPkg = getOrCreateReifiedNode(node, id, workspace, true);
                			 nestedParent = reifiedPkg == null ? parent : reifiedPkg;
                		}
                		else {
                			nestedParent = reifiedNode;
                		}

                		// TODO: Need to get the MODIFICATION STATUS out of here?!!
                		ModStatus modStatus = new ModStatus();
                    EmsScriptNode newValNode = updateOrCreateTransactionableElement((JSONObject)newVal,nestedParent,
            																		null, workspace, ingest, true, modStatus );
                		nodeNames.add(newValNode.getName());

                		changed = true;
                	}
            }

            // Replace the property in the JSON with the sysmlids
            // before ingesting:
            	JSONArray jsonArry = new JSONArray(nodeNames);
            	jsonToCheck.put(jsonKey, jsonArry);
        } // ends if Property and elementJson has a value or Expression and elementJson has a operand

        return changed;
    }

    /**
     * Determine whether the post to the element is based on old information based on a "read" JSON attribute whose value is the date when the posting process originally read the element's data.
     * @param element
     * @param elementJson
     * @return whether the "read" date is older than the last modification date.
     */
    public boolean inConflict( EmsScriptNode element, JSONObject elementJson ) {
        // TODO -- could check for which properties changed since the "read"
        // date to allow concurrent edits to different properties of the same
        // element.    	
        String readTime = null;
        try {
            readTime = elementJson.getString( Acm.JSON_READ );//"read" );
        } catch ( JSONException e ) {
            return false;
        }
        if (Debug.isOn()) System.out.println( "%% %% %% readTime = " + readTime );
        if ( readTime == null ) return false;
        Date lastModified = new Date();
        if(element != null)
            lastModified = element.getLastModified( null );
        if (Debug.isOn()) System.out.println( "%% %% %% lastModified = " + lastModified );
        //DateTimeFormatter parser = ISODateTimeFormat.dateParser(); // format is different than what is printed

//        DateTime readDateTime = parser.parseDateTime( readTime );
        Date readDate = null;
        readDate = TimeUtils.dateFromTimestamp( readTime );
        if (Debug.isOn()) System.out.println( "%% %% %% readDate = " + readDate );
//        if ( readDateTime != null ) { // return false;
//            readDate = readDateTime.toDate();
        if ( readDate != null ) { // return false;
            return readDate.compareTo( lastModified ) < 0;
        }
//        }
        Debug.error( "Bad date format or parse bug! lastModified = "
                     + lastModified //+ ", readDateTime = " + readDateTime
                     + ", readDate = " + readDate + ", elementJson="
                     + elementJson );
        String lastModString = TimeUtils.toTimestamp( lastModified );
        
        
        return readTime.compareTo( lastModString ) >= 0;
    }

    protected EmsScriptNode
            updateOrCreateTransactionableElement( JSONObject elementJson,
                                                  EmsScriptNode parent,
                                                  JSONArray children,
                                                  WorkspaceNode workspace,
                                                  boolean ingest,
                                                  boolean nestedNode,
                                                  ModStatus modStatus) throws Exception {
        if (!elementJson.has(Acm.JSON_ID)) {
            elementJson.put( Acm.JSON_ID, NodeUtil.createId( services ) );
            //return null;
        }
        String id = elementJson.getString(Acm.JSON_ID);
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateElement " + id);

        // TODO Need to permission check on new node creation
        // find node if exists, otherwise create
        EmsScriptNode nodeToUpdate = findScriptNodeById( id, workspace, null, true );
        String existingNodeType = null;
        if ( nodeToUpdate != null ) {
            nodeToUpdate.setResponse( getResponse() );
            nodeToUpdate.setStatus( getResponseStatus() );
            existingNodeType = nodeToUpdate.getTypeName();
            if ( nodeToUpdate.isDeleted() ) {
                nodeToUpdate.removeAspect( "ems:Deleted" );
                modStatus.setState( ModStatus.State.ADDED );
            }
        }
        EmsScriptNode reifiedNode = null;

        String jsonType = null;
        JSONObject specializeJson = null;
        // The type is now found by using the specialization key
        // if its a non-nested node:
        if (nestedNode) {
            	if (elementJson.has(Acm.JSON_TYPE)) {
            		jsonType = elementJson.getString(Acm.JSON_TYPE);
            	}

        		// Put the type in Json if the was not supplied, but found in the existing node:
            	if (existingNodeType != null && jsonType == null) {
            		jsonType = existingNodeType;
            		elementJson.put(Acm.JSON_TYPE, existingNodeType);
            	}
        }
        else {
	        if (elementJson.has(Acm.JSON_SPECIALIZATION)) {
	        	    specializeJson = elementJson.getJSONObject(Acm.JSON_SPECIALIZATION);
		        if (specializeJson != null) {
        		        	if (specializeJson.has(Acm.JSON_TYPE)) {
        		        		jsonType = specializeJson.getString(Acm.JSON_TYPE);
        		        	}

        		        	// Put the type in Json if the was not supplied, but found in the existing node:
        		        	if (existingNodeType != null && jsonType == null) {
        		        		jsonType = existingNodeType;
        		        		specializeJson.put(Acm.JSON_TYPE, existingNodeType);
        		        	}
		        }
	        }
        }

        if ( jsonType == null ) {
            jsonType = ( existingNodeType == null ? "Element" : existingNodeType );
        }

        	if (existingNodeType != null && !jsonType.equals(existingNodeType)) {
        		log(LogLevel.WARNING, "The type supplied "+jsonType+" is different than the stored type "+existingNodeType);
        	}

        String acmSysmlType = null;
        String type = null;
        if ( jsonType != null ) {
            acmSysmlType = Acm.getJSON2ACM().get( jsonType );
        }

        // Error if could not determine the type and processing the non-nested node:
        //	Note:  Must also have a specialization in case they are posting just a Element, whic
        //		   doesnt need a specialization key
        if (acmSysmlType == null && !nestedNode && elementJson.has(Acm.JSON_SPECIALIZATION)) {
            	log(LogLevel.ERROR,"Type was not supplied and no existing node to query for the type",
            		HttpServletResponse.SC_BAD_REQUEST);
            	return null;
        }

        type = NodeUtil.getContentModelTypeName( acmSysmlType, services );

        // Move the node to the specified workspace if the node is not a
        // workspace itself.
        if ( workspace != null && workspace.exists() && nodeToUpdate != null
             && nodeToUpdate.exists() && !nodeToUpdate.isWorkspace() && !workspace.equals( nodeToUpdate.getWorkspace() ) ) {
            parent = workspace.replicateWithParentFolders( parent );
            EmsScriptNode oldNode = nodeToUpdate;
            nodeToUpdate = nodeToUpdate.clone(parent);
            nodeToUpdate.setWorkspace( workspace, oldNode.getNodeRef() );
        }

        if ( nodeToUpdate == null || !nodeToUpdate.exists() ) {// && newElements.contains( id ) ) {
            if ( type == null || type.trim().isEmpty() ) {
                if (Debug.isOn()) System.out.println( "PREFIX: type not found for " + jsonType );
                return null;
            } else {
                log( LogLevel.INFO, "\tcreating node" );
                try {
//                    if ( parent != null && parent.exists() ) {
                        nodeToUpdate = parent.createSysmlNode( id, acmSysmlType );
                        if ( nodeToUpdate == null || !nodeToUpdate.exists() ) {
                            throw new Exception( "createNode() failed." );
                        }
                        nodeToUpdate.setProperty( Acm.CM_NAME, id );
                        nodeToUpdate.setProperty( Acm.ACM_ID, id );
                        modStatus.setState( ModStatus.State.ADDED  );
                        if ( workspace != null && workspace.exists() ) {
                            nodeToUpdate.setWorkspace( workspace, null );
                        }
//                    } else {
//                        Debug.error( true, true,
//                                     "Error! Attempt to create node, " + id
//                                             + ", from non-existent parent, "
//                                             + parent );
//                    }
                } catch ( Exception e ) {
                    if (Debug.isOn()) System.out.println( "Got exception in "
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
                if (nodeToUpdate != null && nodeToUpdate.exists() ) {
                    if ( Debug.isOn() ) Debug.outln("moving node <<<" + nodeToUpdate + ">>>");
                    if ( Debug.isOn() ) Debug.outln("to parent <<<" + parent + ">>>");
                        if ( nodeToUpdate.move(parent) ) {
                            modStatus.setState( ModStatus.State.MOVED  );
                        }
                    if ( !type.equals( acmSysmlType )
                            && NodeUtil.isAspect( acmSysmlType ) ) {
                        if (nodeToUpdate.createOrUpdateAspect( acmSysmlType )) {
                            modStatus.setState( ModStatus.State.UPDATED  );
                        }
                    }
                }
            } catch (Exception e) {
                log(LogLevel.WARNING, "could not find node information: " + id);
                e.printStackTrace();
            }
        }
        boolean nodeExists = nodeToUpdate != null && nodeToUpdate.exists();
        if (id != null && nodeExists ) {
            foundElements.put(id, nodeToUpdate); // cache the found value
        }

        // Note: Moved this before ingesting the json b/c we need the reifiedNode
        if (nodeExists && elementHierarchyJson.has(id)) {
            log(LogLevel.INFO, "\tcreating reified package");
            reifiedNode = getOrCreateReifiedNode(nodeToUpdate, id, workspace, true); // TODO -- Is last argument correct?

            JSONArray array = elementHierarchyJson.getJSONArray(id);
            if ( array != null ) {
                for (int ii = 0; ii < array.length(); ii++) {
                    children.put(array.get(ii));
                }
            }
        }

        // update metadata
        if (ingest && nodeExists && checkPermissions(nodeToUpdate, PermissionService.WRITE)) {
            log(LogLevel.INFO, "\tinserting metadata");

            // Special processing for Expression or Property:
            //	Note: this will modify elementJson
            if ( processExpressionOrProperty(acmSysmlType, nestedNode, elementJson, specializeJson, nodeToUpdate,
										ingest, reifiedNode, parent, id, workspace) ) {
                modStatus.setState( ModStatus.State.UPDATED );
            }

            timerIngest = Timer.startTimer(timerIngest, timeEvents);
            if ( nodeToUpdate.ingestJSON(elementJson) ) {
                Timer.stopTimer(timerIngest, "!!!!! updateOrCreateTransactionableElement(): ingestJSON", timeEvents);
                modStatus.setState( ModStatus.State.UPDATED );
            }
        } // ends if (ingest && nodeExists && checkPermissions(node, PermissionService.WRITE))

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
        
        return nestedNode ? nodeToUpdate : reifiedNode;
    }

    protected EmsScriptNode getOrCreateReifiedNode( EmsScriptNode node,
                                                    String id,
                                                    WorkspaceNode workspace,
                                                    boolean useParent ) {
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

        if ( workspace != null && workspace.exists() ) {
            parent = workspace.replicateWithParentFolders( parent );
        }

        if (checkPermissions(parent, PermissionService.WRITE)) {
            String pkgName = id + "_pkg";
            reifiedNode = findScriptNodeById(pkgName, workspace, null, true);
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

                // check for the case where the id isn't the same as the node
                // reference - this happens when creating a root level package
                // for example
                if ( !id.equals( node.getProperty( "sysml:id" ) )) {
                    node = findScriptNodeById(id, workspace, null, false);
                }

                if (node != null) {
                    // lets keep track of reification
                    node.createOrUpdateAspect( "ems:Reified" );
                    node.createOrUpdateProperty( "ems:reifiedPkg", reifiedNode.getNodeRef() );
                    
                    reifiedNode.createOrUpdateAspect( "ems:Reified" );
                    reifiedNode.createOrUpdateProperty( "ems:reifiedNode", node.getNodeRef() );
                }
            }
        }

        return reifiedNode;
    }

    /**
     * Parses the Property and returns a set of all the node names
     * in the property.
     *
     * @param propertyNode The node to parse
     * @return Set of cm:name
     */
    private Set<String> getPropertyElementNames(EmsScriptNode propertyNode) {

    	Set<String> names = new HashSet<String>();

    	if (propertyNode != null) {

    		String name = propertyNode.getName();

	    	if (name != null) names.add(name);

	    	// See if it has a value property:
	        Collection< EmsScriptNode > propertyValues =
	              	getSystemModel().getProperty(propertyNode, Acm.JSON_VALUE);

			if (!Utils.isNullOrEmpty(propertyValues)) {
				  for (EmsScriptNode value : propertyValues) {

					  names.add(value.getName());

					  // TODO REVIEW
					  //	  need to be able to handle all ValueSpecification types?
					  //	  some of them have properties that point to nodes, so
					  //	  would need to process them also
				  }
			}
    	}

    	return names;
    }

    /**
     * Parses the Parameter and returns a set of all the node names
     * in the parameter.
     *
     * @param paramNode The node to parse
     * @return Set of cm:name
     */
    private Set<String> getParameterElementNames(EmsScriptNode paramNode) {

    	Set<String> names = new HashSet<String>();

    	if (paramNode != null) {

    		String name = paramNode.getName();

    		if (name != null) names.add(name);

	    	// See if it has a defaultParamaterValue property:
	        Collection< EmsScriptNode > paramValues =
	              	getSystemModel().getProperty(paramNode, Acm.JSON_PARAMETER_DEFAULT_VALUE);

			if (!Utils.isNullOrEmpty(paramValues)) {
				  names.add(paramValues.iterator().next().getName());
			}
    	}

    	return names;
    }

    /**
     * Parses the Operation and returns a set of all the node names
     * in the operation.
     *
     * @param opNode The node to parse
     * @return Set of cm:name
     */
    private Set<String> getOperationElementNames(EmsScriptNode opNode) {

    	Set<String> names = new HashSet<String>();

    	if (opNode != null) {

    		String name = opNode.getName();

	    	if (name != null) names.add(name);

	    	// See if it has a operationParameter and/or operationExpression property:
	        Collection< EmsScriptNode > opParamNodes =
	              	getSystemModel().getProperty(opNode, Acm.JSON_OPERATION_PARAMETER);

			if (!Utils.isNullOrEmpty(opParamNodes)) {
			  for (EmsScriptNode opParamNode : opParamNodes) {
				  names.addAll(getParameterElementNames(opParamNode));
			  }
			}

		    Collection< EmsScriptNode > opExprNodes =
		    		getSystemModel().getProperty(opNode, Acm.JSON_OPERATION_EXPRESSION);

		    if (!Utils.isNullOrEmpty(opExprNodes)) {
		    	names.add(opExprNodes.iterator().next().getName());
		    }
    	}

    	return names;
    }

    /**
     * Parses the expression and returns a set of all the node names
     * in the expression.
     *
     * @param expressionNode The node to parse
     * @return Set of cm:name
     */
    private Set<String> getExpressionElementNames(EmsScriptNode expressionNode) {

    	Set<String> names = new HashSet<String>();

    	if (expressionNode != null) {

	    	// Add the name of the Expression itself:
    		String name = expressionNode.getName();

    		if (name != null) names.add(name);

	    	// Process all of the operand properties:
	        Collection< EmsScriptNode > properties =
	        		getSystemModel().getProperty( expressionNode, Acm.JSON_OPERAND);

	        if (!Utils.isNullOrEmpty(properties)) {

	          EmsScriptNode valueOfElementNode = null;

	          for (EmsScriptNode operandProp : properties) {

	        	if (operandProp != null) {

		            names.add(operandProp.getName());

		            // Get the valueOfElementProperty node:
		            Collection< EmsScriptNode > valueOfElemNodes =
		            		getSystemModel().getProperty(operandProp, Acm.JSON_ELEMENT_VALUE_ELEMENT);

		            // If it is a elementValue, then this will be non-empty:
		            if (!Utils.isNullOrEmpty(valueOfElemNodes)) {

		              // valueOfElemNodes should always be size 1 b/c elementValueOfElement
		              // is a single NodeRef
		              valueOfElementNode = valueOfElemNodes.iterator().next();
		            }

		            // Otherwise just use the node itself as we are not dealing with
		            // elementValue types:
		            else {
		              valueOfElementNode = operandProp;
		            }

		            if (valueOfElementNode != null) {

		              String typeString = getSystemModel().getTypeString(valueOfElementNode, null);

		              // If it is a Operation then see if it then process it:
		              if (typeString.equals(Acm.JSON_OPERATION)) {
		            	  names.addAll(getOperationElementNames(valueOfElementNode));
		              }

		              // If it is a Expression then process it recursively:
		              else if (typeString.equals(Acm.JSON_EXPRESSION)) {
		            	  names.addAll(getExpressionElementNames(valueOfElementNode));
		              }

		              // If it is a Parameter then process it:
		              else if (typeString.equals(Acm.JSON_PARAMETER)) {
		            	  names.addAll(getParameterElementNames(valueOfElementNode));
		              }

		              // If it is a Property then process it:
		              else if (typeString.equals(Acm.JSON_PROPERTY)) {
		            	  names.addAll(getPropertyElementNames(valueOfElementNode));
		              }

		            } // ends if valueOfElementNode != null

	        	} // ends if operandProp != null

	          } // ends for loop through operand properties

	        } // ends if operand properties not null or empty

    	} // ends if expressionNode != null

    	return names;
    }

    /**
     * Parses the expression for the passed constraint, and returns a set of all the node
     * names in the expression.
     *
     * @param constraintNode The node to parse
     * @return Set of cm:name
     */
    private Set<String> getConstraintElementNames(EmsScriptNode constraintNode) {

    	Set<String> names = new HashSet<String>();

    	if (constraintNode != null) {

	    	// Add the name of the Constraint:
	    	String name = constraintNode.getName();

	    	if (name != null) names.add(name);

	    	// Get the Expression for the Constraint:
	        EmsScriptNode exprNode = getConstraintExpression(constraintNode);

	        // Add the names of all nodes in the Expression:
	        if (exprNode != null) {

	        	// Get elements names from the Expression:
	        	names.addAll(getExpressionElementNames(exprNode));

	        	// REVIEW: Not using the child associations b/c
	        	// ElementValue's elementValueOfElement has a different
	        	// owner, and wont work for our demo either b/c
	        	// not everything is under one parent
	        }

    	}

    	return names;
    }

    /**
     * Parse out the expression from the passed constraint node
     *
     * @param constraintNode The node to parse
     * @return The Expression node for the constraint
     */
    private EmsScriptNode getConstraintExpression(EmsScriptNode constraintNode) {

    	if (constraintNode == null) return null;

        // Get the constraint expression:
        Collection<EmsScriptNode> expressions =
        		getSystemModel().getProperty( constraintNode, Acm.JSON_CONSTRAINT_SPECIFICATION );

        // This should always be of size 1:
        return Utils.isNullOrEmpty( expressions ) ? null :  expressions.iterator().next();

    }

    /**
     * Creates a ConstraintExpression for the passed constraint node and adds to the passed constraints
     *
     * @param constraintNode The node to parse and create a ConstraintExpression for
     * @param constraints The list of Constraints to add to
     */
    private void addConstraintExpression(EmsScriptNode constraintNode, Collection<Constraint> constraints) {

    	if (constraintNode == null || constraints == null) return;

        EmsScriptNode exprNode = getConstraintExpression(constraintNode);

        if (exprNode != null) {
            Expression<Call> expressionCall = getSystemModelAe().toAeExpression( exprNode );
            Call call = (Call) expressionCall.expression;
            Expression<Boolean> expression = new Expression<Boolean>(call.evaluate(true, false));

            if (expression != null) {

                constraints.add(new ConstraintExpression( expression ));
            }
        }
    }

    protected void fix( Set< EmsScriptNode > elements ) {

        log(LogLevel.INFO, "Constraint violations will be fixed if found!");

        SystemModelSolver< EmsScriptNode, EmsScriptNode, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode >  solver =
                new SystemModelSolver< EmsScriptNode, EmsScriptNode, EmsScriptNode, EmsScriptNode, String, String, Object, EmsScriptNode, String, String, EmsScriptNode >(getSystemModel(), new ConstraintLoopSolver() );

        Collection<Constraint> constraints = new ArrayList<Constraint>();

        // Search for all constraints in the database:
        Collection<EmsScriptNode> constraintNodes = getSystemModel().getType(null, Acm.JSON_CONSTRAINT);

        if (!Utils.isNullOrEmpty(constraintNodes)) {

            // Loop through each found constraint and check if it contains any of the elements
            // to be posted:
            for (EmsScriptNode constraintNode : constraintNodes) {

                // Parse the constraint node for all of the cm:names of the nodes in its expression tree:
                Set<String> constrElemNames = getConstraintElementNames(constraintNode);

                // Check if any of the posted elements are in the constraint expression tree, and add
                // constraint if they are:
                // Note: if a Constraint element is in elements then it will also get added here b/c it
                //          will be in the database already via createOrUpdateMode()
                for (EmsScriptNode element : elements) {

                    String name = element.getName();
                    if (name != null && constrElemNames.contains(name)) {
                        addConstraintExpression(constraintNode, constraints);
                        break;
                    }

                } // Ends loop through elements

            } // Ends loop through constraintNodes

        } // Ends if there was constraint nodes found in the database

        // Solve the constraints:
        if (!Utils.isNullOrEmpty( constraints )) {

            // Add all of the Parameter constraints:
            ClassData cd = getSystemModelAe().getClassData();

            // Loop through all the listeners:
            for (ParameterListenerImpl listener : cd.getAeClasses().values()) {

                // TODO: REVIEW
                //       Can we get duplicate ParameterListeners in the aeClassses map?
                constraints.addAll( listener.getConstraints( true, null ) );
            }

            // Solve!!!!
            Debug.turnOn();
//            Random.reset();
            boolean result = solver.solve(constraints);
            Debug.turnOff();

            if (!result) {
                log( LogLevel.ERROR, "Was not able to satisfy all of the constraints!" );
            }
            else {
                log( LogLevel.INFO, "Satisfied all of the constraints!" );
                
                // Update the values of the nodes after solving the constraints:
                EmsScriptNode node;
                Parameter<Object> param;
                Set<Entry<EmsScriptNode, Parameter<Object>>> entrySet = sysmlToAe.getExprParamMap().entrySet();
                for (Entry<EmsScriptNode, Parameter<Object>> entry : entrySet) {
                	
                	node = entry.getKey();
                	param = entry.getValue();
                	systemModel.setValue(node, (Serializable)param.getValue());
                }
                
                log( LogLevel.INFO, "Updated all node values to satisfy the constraints!" );
                
            }

        } // End if constraints list is non-empty

    }
    
    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    	
        ModelPost instance = new ModelPost(repository, services);
        instance.setServices( getServices() );
        return instance.executeImplImpl(req,  status, cache);
    }

    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
                                              Status status, Cache cache) {
        NodeUtil.doCaching = true;  // TODO toggle to false
        Timer timer = new Timer();

        printHeader( req );

        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();

        boolean runInBackground = getBooleanArg(req, "background", false);
        boolean fix = getBooleanArg(req, "fix", false);

        // see if prettyPrint default is overridden and change
        prettyPrint = getBooleanArg(req, "pretty", prettyPrint );

        String user = AuthenticationUtil.getRunAsUser();
        String wsId = null;
        WorkspaceNode workspace = getWorkspace( req, //true, // not creating ws!
                                                user );
        boolean wsFound = workspace != null;
        if ( !wsFound ) {
            wsId = getWorkspaceId( req );
            if ( wsId != null && wsId.equalsIgnoreCase( "master" ) ) {
                wsFound = true;
            }
        }
        if ( !wsFound ) {
            log( LogLevel.ERROR,
                 "Could not find or create " + wsId + " workspace.\n",
                 Utils.isNullOrEmpty( wsId ) ? HttpServletResponse.SC_NOT_FOUND
                                             : HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        }

        String expressionString = req.getParameter( "expression" );

        JSONObject top = new JSONObject();
        ArrayList<JSONObject> foo = new ArrayList<JSONObject>();
        if (wsFound && validateRequest(req, status)) {
            try {
                if (runInBackground) {
                    saveAndStartAction(req, workspace, status);
                    response.append("JSON uploaded, model load being processed in background.\n");
                    response.append("You will be notified via email when the model load has finished.\n");
                }
                else {
                    JSONObject postJson = (JSONObject)req.parseContent();
                    EmsScriptNode projectNode = getProjectNodeFromRequest( req, true );
                    Set< EmsScriptNode > elements =
                        createOrUpdateModel( postJson, status,
                                                      projectNode, workspace, null );

                    addRelationshipsToProperties( elements );
                    if ( !Utils.isNullOrEmpty( elements ) ) {

                        // Fix constraints if desired:
                        if (fix) {
                            fix(elements);
                        }

                        // Create JSON object of the elements to return:
                        JSONArray elementsJson = new JSONArray();
                        timerToJson = Timer.startTimer(timerToJson, timeEvents);
                        for ( EmsScriptNode element : elements ) {
                            elementsJson.put( element.toJSONObject(null) );
                        }
                        Timer.stopTimer(timerToJson, "!!!!! executeImpl(): toJSON time", timeEvents);
                        top.put( "elements", elementsJson );
                        if (!Utils.isNullOrEmpty(response.toString())) top.put("message", response.toString());
                        if ( prettyPrint ) model.put( "res", top.toString( 4 ) );
                        else model.put( "res", top.toString() );
                    }
                }
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

        System.out.println( "ModelPost: " + timer );
        
        return model;
    }

    public void addRelationshipsToProperties( Set< EmsScriptNode > elems ) {
        for ( EmsScriptNode element : elems ) {
            element.addRelationshipToPropertiesOfParticipants();
        }
    }

    protected void saveAndStartAction( WebScriptRequest req,
                                       WorkspaceNode workspace,
                                       Status status ) throws Exception {
        //String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        //SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        SiteInfo sInfo = getSiteInfo(req);
        EmsScriptNode siteNode = getSiteNodeFromRequest( req );
        if ( sInfo == null ) {
            log(LogLevel.ERROR, "No site to start model load!", HttpServletResponse.SC_BAD_REQUEST);
            return;
        } else {
            siteNode = new EmsScriptNode(sInfo.getNodeRef(), services, response);
        }
        String projectId = getProjectId( req );

        String jobName = "Load Job " + projectId + ".json";
        EmsScriptNode jobNode = ActionUtil.getOrCreateJob(siteNode, jobName, "ems:Job", status, response);

        // write out the json
        ActionUtil.saveStringToFile(jobNode, "application/json", services, ((JSONObject)req.parseContent()).toString(4));

        EmsScriptNode projectNode = findScriptNodeById(projectId, workspace, null, false);
        // kick off the action
        ActionService actionService = services.getActionService();
        Action loadAction = actionService.createAction(ModelLoadActionExecuter.NAME);
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_ID, projectId);
        if ( projectNode != null ) {
            loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NAME, (String)projectNode.getProperty(Acm.ACM_NAME));
        }
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NODE, projectNode);

        String workspaceId = getWorkspaceId( req );
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_WORKSPACE_ID, workspaceId);

        services.getActionService().executeAction(loadAction , jobNode.getNodeRef(), true, true);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        //String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        setSiteInfo( req );

        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null) {
            // TODO - move this to ViewModelPost - really non hierarchical post
            if (!checkRequestVariable(elementId, "elementid")) {
                return false;
            }
        }

        return true;
    }

    protected EmsScriptNode getProjectNodeFromRequest(WebScriptRequest req, boolean createIfNonexistent) {
        EmsScriptNode projectNode = null;

        WorkspaceNode workspace = getWorkspace( req );

        String siteName = getSiteName(req);
        //String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        //String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        String projectId = getProjectId(req);
        EmsScriptNode siteNode = createSite( siteName, workspace );
        setSiteInfo(req); // Setting the site info in case we just created the site for the first time
        
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
                projectNode = findScriptNodeById( elementId, workspace, null, false );

                if ( projectNode == null ) {
                    // projectNode should be the owner..., which should exist
                    try {
                        JSONObject postJson = (JSONObject)req.parseContent();
                        JSONObject elementsJson =
                                postJson.getJSONObject( "elements" );
                        JSONObject elementJson =
                                elementsJson.getJSONObject( elementId );
                        projectNode =
                                findScriptNodeById( elementJson.getString( Acm.JSON_OWNER ), workspace, null, false );
                    } catch ( JSONException e ) {
                        e.printStackTrace();
                    }
                }
            } else {}
        }

        if ( projectNode == null ) {
            ProjectPost pp = new ProjectPost( repository, services );
            JSONObject json = new JSONObject();
            try {
                pp.updateOrCreateProject( json, workspace, projectId, siteName,
                                          createIfNonexistent, false );
                projectNode = findScriptNodeById( projectId, workspace, null, false );
            } catch ( JSONException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return projectNode;
    }

    public void setRunWithoutTransactions(boolean withoutTransactions) {
        runWithoutTransactions = withoutTransactions;
    }

    public void setSiteInfo( WebScriptRequest req ) {
        if ( req == null ) return;
        String siteName = getSiteName( req );
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
