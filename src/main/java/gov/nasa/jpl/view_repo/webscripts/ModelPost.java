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
import gov.nasa.jpl.ae.sysml.SystemModelSolver;
import gov.nasa.jpl.ae.sysml.SystemModelToAeExpression;
import gov.nasa.jpl.ae.util.ClassData;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Random;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsSystemModel;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.ModStatus;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ShareUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import kexpparser.KExpParser;
//import k.frontend.Frontend;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.apache.log4j.Logger;
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
    static Logger logger = Logger.getLogger(ModelPost.class);

    public ModelPost() {
        super();
    }

    public ModelPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
     }

//    private static boolean activeVersionCaching = true;

    // Set the flag to time events that occur during a model post using the timers
    // below
    public static boolean timeEvents = false;
    private Timer timerCommit = null;
    private Timer timerIngest = null;
    private Timer timerUpdateModel = null;
    private Timer timerToJson = null;

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

    private EmsScriptNode projectNode = null;
    private EmsScriptNode siteNode = null;
    private EmsScriptNode sitePackageNode = null;
    private boolean internalRunWithoutTransactions = false;

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

    public EmsScriptNode getProjectNode() {
        return projectNode;
    }

    public void setProjectNode( EmsScriptNode projectNode ) {
        this.projectNode = projectNode;
    }

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
                                 WorkspaceNode targetWS, WorkspaceNode sourceWS,
                                 boolean createCommit) throws Exception {
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
            elements.addAll(createOrUpdateModel2(object, status, targetWS, sourceWS, createCommit));
        }

        return elements;
    }
    
    private void processRootElement(String rootElement, WorkspaceNode targetWS,
                                    TreeMap<String, EmsScriptNode> nodeMap,
                                    TreeSet<EmsScriptNode> elements) throws Exception {
        
        if (projectNode == null ||
            !rootElement.equals(projectNode.getProperty(Acm.CM_NAME))) {

            EmsScriptNode owner = getOwner(rootElement,targetWS, true);

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
        
    }
    
    private void sendDeltasAndCommit(WorkspaceNode targetWS,  TreeSet<EmsScriptNode> elements,
                                     long start, long end) throws JSONException {
        
        // FIXME: Need to split elements by project Id - since they won't always be in same project
        //      CommitUtil.commitAndStartAction( targetWS, wsDiff, start, end, elements.first().getProjectId(), status, true );
        
        NodeRef commitRef = CommitUtil.commit(null, targetWS, "", true, services, new StringBuffer() );
        String projectId = elements.first().getProjectId();
        String wsId = "master";
        if (targetWS != null) {
            wsId = targetWS.getId();
        }

        JSONObject deltaJson = wsDiff.toJSONObject( new Date(start), new Date(end) );

        // FIXME: Need to split by projectId
        if ( !CommitUtil.sendDeltas(deltaJson, wsId, projectId) ) {
            //logger.warn("send deltas not posted properly");
        }

        CommitUtil.updateCommitNodeRef( commitRef, deltaJson.toString(), "", services, response );

        timerCommit = Timer.startTimer(timerCommit, timeEvents);
        
        Timer.stopTimer(timerCommit, "!!!!! updateOrCreateElement(): ws metadata time", timeEvents);
    }
    
    private void handleRelationships(WorkspaceNode targetWS,
                                     TreeMap<String, EmsScriptNode> nodeMap,
                                     TreeSet<EmsScriptNode> elements,
                                     boolean singleElement,
                                     JSONObject postJson) throws Exception {
        
        // handle the relationships
        updateOrCreateAllRelationships(relationshipsJson, targetWS);

        // make another pass through the elements and update their properties
        Set< EmsScriptNode > updatedElements = updateNodeReferences( singleElement, postJson,
                                                                     targetWS );
        for ( EmsScriptNode node : updatedElements ) {
            nodeMap.put(node.getName(), node);
            if ( NodeUtil.activeVersionCaching ) {
                //NodeUtil.cacheNodeVersion( node );
                node.getOrSetCachedVersion();
            }
        }
        elements.addAll( updatedElements );
    }
    
    public Set< EmsScriptNode >
            createOrUpdateModel2( Object content, Status status,
                                  final WorkspaceNode targetWS, WorkspaceNode sourceWS,
                                  boolean createCommit) throws Exception {
        Date now = new Date();
        log(LogLevel.INFO, "Starting createOrUpdateModel: " + now);
        final long start = System.currentTimeMillis();

        log( LogLevel.DEBUG, "****** NodeUtil.doSimpleCaching = " + NodeUtil.doSimpleCaching );
        log( LogLevel.DEBUG, "****** NodeUtil.doFullCaching = " + NodeUtil.doFullCaching );

        if(sourceWS == null)
            setWsDiff( targetWS );
        else
            setWsDiff(targetWS, sourceWS, null, null);


        clearCaches();

        final JSONObject postJson = (JSONObject) content;

        final boolean singleElement = !postJson.has(ELEMENTS);

        final TreeSet<EmsScriptNode> elements =
                new TreeSet<EmsScriptNode>();
        final TreeMap<String, EmsScriptNode> nodeMap =
                new TreeMap< String, EmsScriptNode >();

        timerUpdateModel= Timer.startTimer(timerUpdateModel, timeEvents);

        boolean oldRunWithoutTransactions = internalRunWithoutTransactions;
        internalRunWithoutTransactions = true;

        // create the element map and hierarchies
        if (buildElementMap(postJson.getJSONArray(ELEMENTS), targetWS)) {
            // start building up elements from the root elements
            for (final String rootElement : rootElements) {
                log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
                
                if (runWithoutTransactions) {
                    processRootElement( rootElement, targetWS, nodeMap, elements );
                }
                else {
                    new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                        @Override
                        public void run() throws Exception {
                            processRootElement( rootElement, targetWS, nodeMap, elements );
                        }
                    };
                }
                
            } // end for (String rootElement: rootElements) {
        } // end if (buildElementMap(postJson.getJSONArray(ELEMENTS))) {

        Timer.stopTimer(timerUpdateModel, "!!!!! createOrUpdateModel(): main loop time", timeEvents);

        if (runWithoutTransactions) {
            handleRelationships( targetWS, nodeMap, elements, singleElement, postJson );
        }
        else {
            new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                @Override
                public void run() throws Exception {
                    handleRelationships( targetWS, nodeMap, elements, singleElement, postJson );
                }
            };
        }

        internalRunWithoutTransactions = oldRunWithoutTransactions;

        now = new Date();
        final long end = System.currentTimeMillis();
        log(LogLevel.INFO, "createOrUpdateModel completed" + now + " : " +  (end - start) + "ms\n");

        timerUpdateModel = Timer.startTimer(timerUpdateModel, timeEvents);

        // Send deltas to all listeners
        if (createCommit && wsDiff.isDiff()) {
            if (runWithoutTransactions) {
                sendDeltasAndCommit( targetWS, elements, start, end );
            }
            else {
                new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                    @Override
                    public void run() throws Exception {
                        sendDeltasAndCommit( targetWS, elements, start, end );
                    }
                };
            }
        }

        Timer.stopTimer(timerUpdateModel, "!!!!! createOrUpdateModel(): Deltas time", timeEvents);

        return new TreeSet< EmsScriptNode >( nodeMap.values() );
    }


    protected Set<EmsScriptNode> updateNodeReferences(boolean singleElement,
                                                      JSONObject postJson,
                                                      WorkspaceNode workspace) throws Exception {
        TreeSet<EmsScriptNode> elements =
                new TreeSet<EmsScriptNode>();

        if ( singleElement ) {
            elements.addAll( updateOrCreateElement( postJson, projectNode, workspace, true ) );
        }
        for (String rootElement : rootElements) {
            log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
            if (projectNode == null || !rootElement.equals(projectNode.getProperty(Acm.CM_NAME))) {
                EmsScriptNode owner = getOwner( rootElement, workspace, false );

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

    /**
     * Resurrect the parent from the dead
     *
     * @param owner
     */
    protected void resurrectParent(EmsScriptNode owner, boolean ingest) {

        log( LogLevel.WARNING, "Owner with name: "
             + owner.getSysmlId() + " was deleted.  Will resurrect it");

        ModStatus modStatus = new ModStatus();
        owner.removeAspect( "ems:Deleted" );
        modStatus.setState( ModStatus.State.ADDED );
        updateTransactionableWsStateImpl(owner, owner.getSysmlId(), modStatus, ingest);
    }

    /**
     * Resurrect the parents of the node from the dead if needed
     *
     */
    protected void resurrectParents(EmsScriptNode nodeToUpdate, boolean ingest) {

        EmsScriptNode nodeParent = nodeToUpdate.getParent();
        EmsScriptNode reifiedNodeParent = nodeParent != null ? nodeParent.getReifiedNode(true) : null;
        while (nodeParent != null  && nodeParent.scriptNodeExists()) {
            if (nodeParent.isDeleted()) {
                resurrectParent(nodeParent, ingest);
            }
            if (reifiedNodeParent != null && reifiedNodeParent.isDeleted()) {
                resurrectParent(reifiedNodeParent, ingest);
            }
            if (nodeParent.isWorkspaceTop()) {
                break;
            }
            nodeParent = nodeParent.getParent();
            reifiedNodeParent = nodeParent != null ? nodeParent.getReifiedNode(true) : null;
        }

    }

    protected EmsScriptNode getOwner( String elementId,
                                      WorkspaceNode workspace,
                                      boolean createOwnerPkgIfNotFound ) throws Exception {
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
        boolean createdHoldingBin = false;

        if (Utils.isNullOrEmpty( ownerName ) ) {
            EmsScriptNode elementNode = findScriptNodeById(elementId, workspace, null, true);
            // If the element was not found, or it was found but does not exist, then create holding bin:
            if ( elementNode == null || (!elementNode.exists() && !elementNode.isDeleted()) ) {

                // Place elements with no owner in a holding_bin_<site>_<project> package:
                String projectNodeId = ((projectNode == null || projectNode.getSysmlId() == null) ? NO_PROJECT_ID : projectNode.getSysmlId());
                String siteName;
                // If posting to a site package:
                if (sitePackageNode != null) {
                    siteName = sitePackageNode.getSysmlId();
                }
                else {
                    siteName = (siteNode == null || siteNode.getName() == null) ? NO_SITE_ID : siteNode.getName();
                }
                ownerName = "holding_bin_"+siteName+"_"+projectNodeId;
                createdHoldingBin = true;
            } else {
                // Parent will be a reified package, which we never delete, so no need to
                // check if we need to resurrect it.  If elementNode is deleted, it will
                // resurrected later when processing that node.
                owner = elementNode.getParent();
            }
        }

        if (!Utils.isNullOrEmpty(ownerName)) {
            boolean foundOwnerElement = true;
            owner = findScriptNodeById(ownerName, workspace, null, true);

            if (owner == null || !owner.exists()) {

                // If the owner was found, but deleted, then make a zombie node!
                if (owner != null && owner.isDeleted()) {
                    log( LogLevel.WARNING, "Owner with name: "
                            + ownerName + " was deleted.  Will resurrect it, and put "+elementId
                            + " into it.");

                    resurrectParent(owner, false);
                }
                // Otherwise, owner wasnt found, or found but doesnt exists:
                else {

                    // FIXME: HERE! ATTENTION BRAD!  add to elements, so it is returned, and remind Doris
                    //        to fix her code also.
                    // Creating a reifiedNode here also, for magic draw sync to work with holding bin,
                    // and for ems:owner to be correct for the node this reifiedNode will own, and to get
                    // the correct cm:name for the reifiedPackage as it is based on the reifiedNode cm:name.
                    String type;
                    String acmName;
                    ModStatus modStatus = new ModStatus();
                    EmsScriptNode sitePackageReifPkg = null;
                    EmsScriptNode nodeBinOwner = null;

                    if (createdHoldingBin) {
                        type = Acm.ACM_PACKAGE;
                        acmName = "holding_bin";
                    }
                    else {
                        type = Acm.ACM_ELEMENT;
                        acmName = ownerName;
                    }

                    // Get or create the reified package for the site package if needed:
                    if (sitePackageNode != null) {
                         sitePackageReifPkg = getOrCreateReifiedPackageNode(sitePackageNode, sitePackageNode.getSysmlId(),
                                                                            workspace, true);
                    }
                    if (sitePackageReifPkg != null) {
                        nodeBinOwner = sitePackageReifPkg;
                    }
                    // Otherwise, use the project reified package:
                    else {
                        // Place the reified node in project reified package:
                        EmsScriptNode projectNodePkg = getOrCreateReifiedPackageNode(projectNode, projectNode.getSysmlId(),
                                                                                workspace, true);
                        nodeBinOwner = projectNodePkg != null ? projectNodePkg : projectNode;
                    }

                    // FIXME: Need to respond with warning that owner couldn't be found?
                    log( LogLevel.WARNING, "Could not find owner with name: "
                                           + ownerName + " putting " + elementId
                                           + " into: " + nodeBinOwner);

                    // Finally, create the reified node for the owner:
                    EmsScriptNode nodeBin = nodeBinOwner.createSysmlNode(ownerName, type,
                                                                        modStatus, workspace);
                    if (nodeBin != null) {
                        nodeBin.setProperty( Acm.ACM_NAME, acmName );
                        owner = nodeBin;
                    }
                    else {
                        foundOwnerElement = false;
                        owner = nodeBinOwner;
                    }
                    updateTransactionableWsStateImpl(nodeBin, ownerName, modStatus, false);

                    if ( nodeBin != null ) nodeBin.getOrSetCachedVersion();
                    nodeBinOwner.getOrSetCachedVersion();
                }

            }
            // really want to add pkg as owner.  Currently we do not delete reified pkgs,
            // so dont need to check for deleted nodes.
            reifiedPkg = findScriptNodeById(ownerName + "_pkg", workspace, null, false);
            if (reifiedPkg == null || !reifiedPkg.exists()) {
                if ( createOwnerPkgIfNotFound) {
                    // If we found the owner element, then it exists but not its
                    // reified package, so we need the reified package to be
                    // created in the same folder as the owner element, so pass
                    // true into useParent parameter. Else, it's owner is the
                    // project folder, the actual folder in which to create the
                    // pkg, so pass false.
                    reifiedPkg = getOrCreateReifiedPackageNode(owner, ownerName, workspace,
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
    protected void updateOrCreateRelationships(final JSONObject jsonObject, final String key,
                                               final WorkspaceNode workspace) throws JSONException {
        
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateRelationships" + key + ": ");
        
        if (runWithoutTransactions || internalRunWithoutTransactions) {
            updateOrCreateTransactionableRelationships(jsonObject, key, workspace);
        }
        else {
            log(LogLevel.INFO, "updateOrCreateRelationships: beginning transaction {");
            new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                @Override
                public void run() throws Exception {
                    updateOrCreateTransactionableRelationships(jsonObject, key, workspace);
                }
            };
            log(LogLevel.INFO, "} updateOrCreateRelationships committing: " + key);
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
     *      these should be done for ValueSpecification types
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
    protected boolean buildElementMap(final JSONArray jsonArray, 
                                      final WorkspaceNode workspace) throws JSONException {
        boolean isValid = true;
        final List<Boolean> validList = new ArrayList<Boolean>();
        
        if (runWithoutTransactions || internalRunWithoutTransactions) {
            isValid =  buildTransactionableElementMap(jsonArray, workspace);
        }
        else {
            log(LogLevel.INFO, "buildElementMap begin transaction {");
            new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                @Override
                public void run() throws Exception {
                    boolean valid = buildTransactionableElementMap(jsonArray, workspace);
                    validList.add( valid);
                }
            };
            isValid = validList.get( 0 );
            log(LogLevel.INFO, "} buildElementMap committing");
        }
        
        return isValid;
    }

    protected boolean buildTransactionableElementMap( JSONArray jsonArray,
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
                        ownerId = projectNode.getSysmlId();
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
     * @param elementJson
     *            Metadata to be added to element
     * @param parent
     * @param workspace
     * @param ingest
     * @return
     * @throws Exception
     */
    protected Set< EmsScriptNode > updateOrCreateElement( final JSONObject elementJson,
                                                          final EmsScriptNode parent,
                                                          final WorkspaceNode workspace,
                                                          final boolean ingest)
                                                                  throws Exception {
        final TreeSet<EmsScriptNode> elements = new TreeSet<EmsScriptNode>();
        TreeMap<String, EmsScriptNode> nodeMap =
                new TreeMap< String, EmsScriptNode >();

        if ( !elementJson.has( Acm.JSON_ID ) ) {
            return elements;
        }
        String jsonId = elementJson.getString( Acm.JSON_ID );
        final EmsScriptNode element = findScriptNodeById( jsonId, workspace, null, true );
        if ( element != null ) {

            // Make sure we have the most recent version of the element's node ref.  This is needed for conflict
            // checks below, and also to make sure we get the most recent properties of the element.
            // This is only needed b/c of an alfresco bug, where it intermittently does not give the most
            // recent node ref.  This does have a performance hit, so commenting out for now:
            //element.checkNodeRefVersion( null );

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
            String id = parent.getSysmlId();
            if (id == null) {
                id = "not sysml type";
            }
            log(LogLevel.WARNING, "Node " + name + " is not of type folder, so cannot create children [id=" + id + "]");
            return elements;
        }

        final JSONArray children = new JSONArray();

        EmsScriptNode reifiedNode = null;
        final ModStatus modStatus = new ModStatus();
        final Pair<Boolean,EmsScriptNode> returnPair = new Pair<Boolean,EmsScriptNode>(false,null);

        if (runWithoutTransactions || internalRunWithoutTransactions) {
            // Check to see if the element has been updated since last read/modified by the
            // posting application.
            if (inConflict(element, elementJson)) {
                return elements;
            }

            reifiedNode =
                    updateOrCreateTransactionableElement( elementJson, parent,
                                                          children, workspace, ingest, false, modStatus,
                                                          element);
        }
        else {
            log(LogLevel.INFO, "updateOrCreateElement begin transaction {");
            new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                @Override
                public void run() throws Exception {
                    // Check to see if the element has been updated since last read/modified by the
                    // posting application.  Want this to be within the transaction
                    boolean conflict = inConflict(element, elementJson);
                    returnPair.first = conflict;
                    
                    if (!conflict) {
                        returnPair.second =
                                updateOrCreateTransactionableElement( elementJson,
                                                                      parent, children,
                                                                      workspace,
                                                                      ingest, false, modStatus, 
                                                                      element );
                    }
                }
            };
            log(LogLevel.INFO, "} updateOrCreateElement end transaction");
            
            if (returnPair.first) {
                return elements;
            }
            reifiedNode = returnPair.second;
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

        // Only need to search again for the element, if it was created again for the first time:
        EmsScriptNode finalElement = element == null ? 
                                     findScriptNodeById( jsonId, workspace, null, true ) : 
                                     element;
        updateTransactionableWsState(finalElement, jsonId, modStatus, ingest);

        fixReadTimeForConflictTransaction(finalElement, elementJson);

        return new TreeSet< EmsScriptNode >( nodeMap.values() );
    }

    /**
     * Update the read/modified time in the json, so that we do not get any
     * conflicts on the second pass, as we may modify the node on the first
     * pass. Make sure this is after any modifications to the node.
     *
     * @param element
     * @param elementJson
     * @throws JSONException
     */
    protected void fixReadTimeForConflict( EmsScriptNode element, JSONObject elementJson  ) throws JSONException {

        if ( elementJson == null ) return;
        Date modTime = ( element == null ? null : element.getLastModified( null ) );

        Date now = new Date();
        if ( modTime == null || now.after( modTime ) ) {
            modTime = now;
        }
        String currentTime = EmsScriptNode.getIsoTime( modTime );
        if ( elementJson.has( Acm.JSON_READ) ) {
            elementJson.put( Acm.JSON_READ, currentTime );
        }
        if ( elementJson.has( Acm.JSON_LAST_MODIFIED ) ) {
            elementJson.put( Acm.JSON_LAST_MODIFIED, currentTime );
        }
    }

    /**
     * Update the read/modified time in the json, so that we do not get any
     * conflicts on the second pass, as we may modify the node on the first
     * pass. Make sure this is after any modifications to the node.
     *
     * @param element
     * @param elementJson
     * @param withoutTransactions
     */
    protected void fixReadTimeForConflictTransaction( final EmsScriptNode element,
                                                      final JSONObject elementJson ) {
        
        if (runWithoutTransactions || internalRunWithoutTransactions) {
            try {
                fixReadTimeForConflict( element, elementJson );
            } catch ( JSONException e ) {
                e.printStackTrace();
            }
        }
        else {
            new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                @Override
                public void run() throws Exception {
                    fixReadTimeForConflict( element, elementJson );
                }
            };
        }
        
    }


    private void updateTransactionableWsState(final EmsScriptNode element, final String jsonId, 
                                              final ModStatus modStatus, final boolean ingest) {

        if (runWithoutTransactions || internalRunWithoutTransactions) {
            updateTransactionableWsStateImpl(element, jsonId, modStatus, ingest);
        } else {
            new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                @Override
                public void run() throws Exception {
                    updateTransactionableWsStateImpl(element, jsonId, modStatus, ingest);
                }
            };
        }
    }

    private void updateTransactionableWsStateImpl(EmsScriptNode element, String jsonId, ModStatus modStatus, boolean ingest) {
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
     * Special processing for elements with properties that point to ValueSpecifications.
     * Modifies the passed elementJson or specializeJson.
     *
     * @param type
     * @param nestedNode
     * @param elementJson
     * @param specializeJson
     * @param node
     * @param ingest
     * @param reifiedPkgNode
     * @param parent
     * @param id
     * @throws Exception
     */
    private boolean processValueSpecProperty(String type, boolean nestedNode, JSONObject elementJson,
                                             JSONObject specializeJson, EmsScriptNode node,
                                             boolean ingest, EmsScriptNode reifiedPkgNode,
                                             EmsScriptNode parent, String id,
                                             WorkspaceNode workspace) throws Exception {
        // TODO REVIEW
        //      Wanted to do a lot of processing in buildTransactionElementMap(), so that we make the
        //      node a owner and in the elementHierachyJson, so that the children will be processed
        //      normally instead of having the code below.  That solution was not a neat as desired either
        //      b/c you need the node itself to retrieve its properties, to see if it already has value or
        //      operand property values stored.  This would involve duplicating a lot of the above code to
        //      create a node if needed, etc.

        // If it is a property that points to a ValueSpecification then need to convert
        // the elementJson to just contain the sysmlid for the nodes,
        // instead of the nodes themselves.  Also, need to create or modify nodes the
        // properties map to.
        boolean changed = false;

        // If it is a nested node then it doesnt have a specialize property
        JSONObject jsonToCheck = nestedNode ? elementJson : specializeJson;

        // If the json has the type/properties of interest:
        if (Acm.TYPES_WITH_VALUESPEC.containsKey(type) && jsonToCheck != null) {

            // Loop through all the properties that need to be processed:
            for (String acmType : Acm.TYPES_WITH_VALUESPEC.get(type)) {
                String jsonType = Acm.getACM2JSON().get( acmType );
                if (jsonType != null && jsonToCheck.has(jsonType)) {
                    Collection< EmsScriptNode > oldVals = getSystemModel().getProperty( node, acmType);

                    boolean myChanged = processValueSpecPropertyImpl( jsonToCheck, jsonType, oldVals, node,
                                                                      ingest, reifiedPkgNode, parent, id,
                                                                      workspace );
                    changed = changed || myChanged;
                }
            }

        }

        return changed;
    }

    /**
     * Special processing for elements with properties that point to ValueSpecifications.
     * Modifies the passed jsonToCheck.
     *
     * @throws Exception
     */
    private boolean processValueSpecPropertyImpl(JSONObject jsonToCheck,
                                                 String jsonKey,
                                                 Collection< EmsScriptNode > oldVals,
                                                 EmsScriptNode node,
                                                 boolean ingest,
                                                 EmsScriptNode reifiedPkgNode,
                                                 EmsScriptNode parent,
                                                 String id,
                                                 WorkspaceNode workspace) throws Exception {

        boolean changed = false;
        JSONArray newVals = jsonToCheck.optJSONArray(jsonKey);
        JSONObject newVal = newVals != null ? null : jsonToCheck.optJSONObject(jsonKey);
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
        if (newVals != null) {
            for (int i = 0; i < newVals.length(); ++i) {
                newVal = newVals.optJSONObject(i);
                boolean myChanged = processValueSpecPropertyImplImpl( jsonToCheck, jsonKey, oldVals,
                                                                       node, ingest, reifiedPkgNode,
                                                                       parent, id, nodeWorkspace,
                                                                       iter, nodeNames, newVal,
                                                                       nodeWorkspace);
                changed = changed || myChanged;
            }

            // Replace the property in the JSON with the sysmlids
            // before ingesting:
            JSONArray jsonArry = new JSONArray(nodeNames);
            jsonToCheck.put(jsonKey, jsonArry);
        }
        // The property is not multi-valued, so just have one value to process:
        else if (newVal != null){
            changed = processValueSpecPropertyImplImpl( jsonToCheck, jsonKey, oldVals,
                                                        node, ingest, reifiedPkgNode,
                                                        parent, id, nodeWorkspace,
                                                        iter, nodeNames, newVal,
                                                        nodeWorkspace);

            // Replace the property in the JSON with the sysmlids
            // before ingesting:
            jsonToCheck.put(jsonKey, nodeNames.get(0));
        }

        return changed;
    }

    private boolean processValueSpecPropertyImplImpl(JSONObject jsonToCheck,
                                                 String jsonKey,
                                                 Collection< EmsScriptNode > oldVals,
                                                 EmsScriptNode node,
                                                 boolean ingest,
                                                 EmsScriptNode reifiedPkgNode,
                                                 EmsScriptNode parent,
                                                 String id,
                                                 WorkspaceNode workspace,
                                                 Iterator<EmsScriptNode> iter,
                                                 ArrayList<String> nodeNames,
                                                 JSONObject newVal,
                                                 WorkspaceNode nodeWorkspace) throws Exception {

        boolean changed = false;
        ModStatus modStatus = new ModStatus();

        // Get the sysmlid of the old value if it exists:
        if (iter != null && iter.hasNext()) {
            EmsScriptNode oldValNode = iter.next();

            // Modified convertIdToEmsScriptNode() to check for alfresco id also,
            // so that we can use the alfresco id here instead.  This fixes a bug
            // found where the lucene search for element based on sysmlid failed, and
            // also improves performance.
            nodeNames.add(oldValNode.getId());
            //nodeNames.add(oldValNode.getSysmlId());

            if ( workspace != null && workspace.exists()
                 && !workspace.equals( oldValNode.getWorkspace() ) ) {

                EmsScriptNode nestedParent = null;
                if (reifiedPkgNode == null) {
                    EmsScriptNode reifiedPkg =
                        getOrCreateReifiedPackageNode( node, id,
                                                       workspace,
                                                       true );
                    nestedParent = reifiedPkg == null ? parent : reifiedPkg;
                }
                else {
                    nestedParent = reifiedPkgNode;
                }

                EmsScriptNode reifiedPkgInWorkspace = nestedParent;
                if ( !workspace.equals( nestedParent.getWorkspace() ) ) {
                    reifiedPkgInWorkspace =
                            workspace.replicateWithParentFolders( nestedParent );
                }
                EmsScriptNode newNode = oldValNode.clone(reifiedPkgInWorkspace);
                newNode.setWorkspace( workspace, oldValNode.getNodeRef() );

                //EmsScriptNode newNode = oldValNode.clone( node );
                //newNode.setWorkspace( workspace, oldValNode.getNodeRef() );
                oldValNode = newNode;
            }

            JSONObject newValJson = newVal;
            // types are mutually exclusive so put in right aspect
            if (newValJson.has( "type" )) {
                if (oldValNode.createOrUpdateAspect(newValJson.getString( "type" ))) {
                    changed = true;
                }
            }

            // Ingest the JSON for the value to update properties
            timerIngest = Timer.startTimer(timerIngest, timeEvents);
            processValue( node, id, reifiedPkgNode, parent, nodeWorkspace, newValJson, ingest, modStatus, oldValNode );
            changed = (modStatus != null && modStatus.getState() != ModStatus.State.NONE );
            //updateOrCreateTransactionableElement
            //boolean didChange = processValueSpecProperty( type, nestedNode, elementJson, specializeJson, oldValNode, ingest, reifiedPkgNode, parent, id, nodeWorkspace );
//            if ( oldValNode.ingestJSON( newValJson ) ) {
//                changed = true;
//            }
            Timer.stopTimer(timerIngest, "!!!!! processExpressionOrProperty(): ingestJSON time", timeEvents);
            oldValNode.getOrSetCachedVersion();
        }
        // Old value doesnt exists, so create a new node:
        else {

            EmsScriptNode newValNode =
                    processValue( node, id, reifiedPkgNode, parent,
                                  nodeWorkspace, newVal, ingest, modStatus, null );
            if ( newValNode == null ) return false;
            // Modified convertIdToEmsScriptNode() to check for alfresco id also,
            // so that we can use the alfresco id here instead.  This fixes a bug
            // found where the lucene search for element based on sysmlid failed, and
            // also improves performance.
            nodeNames.add(newValNode.getId());
            //nodeNames.add(newValNode.getSysmlId());
            changed = true;
            newValNode.getOrSetCachedVersion();
        }

        return changed;
    }

    private EmsScriptNode processValue( EmsScriptNode node, String id,
                                       EmsScriptNode reifiedPkgNode,
                                       EmsScriptNode parent,
                                       WorkspaceNode workspace,
                                       JSONObject newVal, boolean ingest,
                                       ModStatus modStatus,
                                       EmsScriptNode nodeToUpdate ) throws Exception {
        //  The refiedNode will be null if the node is not in the elementHierachy, which
        //  will be the case if no other elements have it as a owner, so in that case
        //  we make a reifiedNode for it here.  If all of that fails, then use the parent
        EmsScriptNode nestedParent = null;
        if (reifiedPkgNode == null) {
             EmsScriptNode reifiedPkg = getOrCreateReifiedPackageNode(node, id, workspace, true);
             nestedParent = reifiedPkg == null ? parent : reifiedPkg;
        }
        else {
            nestedParent = reifiedPkgNode;
        }

        // TODO: Need to get the MODIFICATION STATUS out of here?!!
        EmsScriptNode newValNode =
                updateOrCreateTransactionableElement( newVal,
                                                      nestedParent, null,
                                                      workspace, ingest, true,
                                                      modStatus, nodeToUpdate );
        return newValNode;
    }

    /**
     * Determine whether the post to the element is based on old information based on a "read" JSON attribute
     * whose value is the date when the posting process originally read the element's data, and the "modified"
     * JSON attribute whose value is the date when the posting process originally modified the element's data.
     *
     * @param element
     * @param elementJson
     * @return whether the "read" date or "modified" date is older than the last modification date.
     */
    public boolean inConflict( EmsScriptNode element, JSONObject elementJson ) {

        if (element == null) {
            return false;
        }

        // Make sure we have the most recent version of
        // Get the last modified time from the element:
        Date lastModified = element.getLastModified( null );
        if (Debug.isOn()) System.out.println( "%% %% %% lastModified = " + lastModified );
        String lastModString = TimeUtils.toTimestamp( lastModified );
        String msg = null;

        // Compare read time to last modified time:
        if (inConflictImpl( element, elementJson, lastModified, lastModString, true ) ) {

            msg = "Error! Tried to post concurrent edit to element, "
                            + element + ".\n";
            log( LogLevel.WARNING,
                 msg + "  --> lastModified = " + lastModified
                 + "  --> lastModString = " + lastModString
                 + "  --> elementJson = " + elementJson );
        }

        // Compare last modified to last modified time:
        if (msg == null && inConflictImpl( element, elementJson, lastModified, lastModString, false )) {

            msg = "Error! Tried to post overwrite to element, "
                            + element + ".\n";
            log( LogLevel.WARNING,
                 msg + "  --> lastModified = " + lastModified
                 + "  --> lastModString = " + lastModString
                 + "  --> elementJson = " + elementJson );
        }

        // If there was one of the conflicts then return true:
        if (msg != null) {
            if ( getResponse() == null || getResponseStatus() == null ) {
                Debug.error( msg );
            } else {
                getResponse().append( msg );
                if ( getResponseStatus() != null ) {
                    getResponseStatus().setCode( HttpServletResponse.SC_CONFLICT,
                                                 msg );
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Determine whether the post to the element is based on old information based on a "read" JSON attribute
     * whose value is the date when the posting process originally read the element's data, and the "modified"
     * JSON attribute whose value is the date when the posting process originally modified the element's data.
     *
     * @param element
     * @param elementJson
     * @param lastModified
     * @param lastModString
     * @param checkRead True to check the "read" date, otherwise checks the "modified" date in the JSON
     * @return whether the "read" date or "modified" date is older than the last modification date.
     */
    private boolean inConflictImpl( EmsScriptNode element, JSONObject elementJson,
                                    Date lastModified, String lastModString,
                                    boolean checkRead) {
        // TODO -- could check for which properties changed since the "read"
        // date to allow concurrent edits to different properties of the same
        // element.

        String readTime = null;
        try {
            readTime = elementJson.getString( checkRead ? Acm.JSON_READ : Acm.JSON_LAST_MODIFIED );
        } catch ( JSONException e ) {
            return false;
        }
        if (Debug.isOn()) System.out.println( "%% %% %% time = " + readTime );
        if ( readTime == null) {
            return false;
        }

        Date readDate = null;
        readDate = TimeUtils.dateFromTimestamp( readTime );
        if (Debug.isOn()) System.out.println( "%% %% %% date = " + readDate );

        if ( readDate != null ) {
            return readDate.compareTo( lastModified ) < 0;
        }

        Debug.error( "Bad date format or parse bug! lastModified = "
                     + lastModified
                     + ", date = " + readDate + ", elementJson="
                     + elementJson );

        return readTime.compareTo( lastModString ) > 0;  // FIXME?  This sign should be reversed, right?
    }

    protected EmsScriptNode
            updateOrCreateTransactionableElement( JSONObject elementJson,
                                                  EmsScriptNode parent,
                                                  JSONArray children,
                                                  WorkspaceNode workspace,
                                                  boolean ingest,
                                                  boolean nestedNode,
                                                  ModStatus modStatus,
                                                  EmsScriptNode nodeToUpdate) throws Exception {

        // Add the sysmlid to the newVal json if needed:
        if (!elementJson.has(Acm.JSON_ID)) {

            if (nodeToUpdate != null) {
                elementJson.put( Acm.JSON_ID, nodeToUpdate.getSysmlId() );
            }
            else {
                elementJson.put( Acm.JSON_ID, NodeUtil.createId( services ) );
            }
            //return null;
        }
        String id = elementJson.getString(Acm.JSON_ID);
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateElement " + id);

        // TODO Need to permission check on new node creation
        String existingNodeType = null;
        String existingNodeName = null;
        if ( nodeToUpdate != null ) {
            nodeToUpdate.setResponse( getResponse() );
            nodeToUpdate.setStatus( getResponseStatus() );
            existingNodeType = nodeToUpdate.getTypeName();
            existingNodeName = nodeToUpdate.getSysmlName();

            // Resurrect if found node is deleted and is in this exact workspace.
            if ( nodeToUpdate.isDeleted() &&
                 NodeUtil.workspacesEqual( nodeToUpdate.getWorkspace(), workspace ) ) {
                nodeToUpdate.removeAspect( "ems:Deleted" );
                modStatus.setState( ModStatus.State.ADDED );
            }
        }
        EmsScriptNode reifiedPkgNode = null;

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

    	if (ingest && existingNodeType != null && !jsonType.equals(existingNodeType)) {
    		log(LogLevel.WARNING, "The type supplied "+jsonType+" is different than the stored type "+existingNodeType);
    	}

        String acmSysmlType = null;
        String type = null;
        if ( jsonType != null ) {
            acmSysmlType = Acm.getJSON2ACM().get( jsonType );
        }

        // Error if could not determine the type and processing the non-nested node:
        //  Note:  Must also have a specialization in case they are posting just a Element, which
        //         doesnt need a specialization key
        if (acmSysmlType == null && !nestedNode && elementJson.has(Acm.JSON_SPECIALIZATION)) {
                log(LogLevel.ERROR,"Type was not supplied and no existing node to query for the type",
                    HttpServletResponse.SC_BAD_REQUEST);
                return null;
        }

        // Error if posting a element with the same sysml name, type, and parent as another if the
        // name is non-empty and its not a Untyped type:
        String sysmlName = elementJson.has( Acm.JSON_NAME ) ? elementJson.getString( Acm.JSON_NAME ) :
                                                              existingNodeName;
        if (!Utils.isNullOrEmpty( sysmlName ) && jsonType != null && !jsonType.equals( Acm.JSON_UNTYPED )
            && id != null && parent != null) {
            ArrayList<EmsScriptNode> nodeArray = findScriptNodesBySysmlName(sysmlName, workspace, null, false);

            if (!Utils.isNullOrEmpty( nodeArray )) {
                for (EmsScriptNode n : nodeArray) {
                    if ( !id.equals( n.getSysmlId() ) &&
                         jsonType.equals( n.getTypeName() ) &&
                         parent.equals( n.getParent() ) ) {
                        log(LogLevel.ERROR,"Found another element with the same sysml name: "
                                           +n.getSysmlName()+" type: "+n.getTypeName()
                                           +" parent: "+n.getParent()+" as the element trying to be posted",
                            HttpServletResponse.SC_BAD_REQUEST);
                        return null;
                    }
                }
            }
        }

        type = NodeUtil.getContentModelTypeName( acmSysmlType, services );

        // Move the node to the specified workspace if the node is not a
        // workspace itself.
        if ( workspace != null && workspace.exists() ) {
            if ( nodeToUpdate == null || !nodeToUpdate.exists() ) {
                parent = workspace.replicateWithParentFolders( parent );
            } else if ( nodeToUpdate != null && nodeToUpdate.exists()
                        && !nodeToUpdate.isWorkspace()
                        && !workspace.equals( nodeToUpdate.getWorkspace() ) ) {

                // If its owner is changing, need to bring in the old parent
                // into the new workspace and remove the old child.  Not bringing
                // in the corresponding reified package--hope that's okay!  REVIEW
                EmsScriptNode oldParent = nodeToUpdate.getOwningParent( null );
                EmsScriptNode newOldParent =
                        workspace.replicateWithParentFolders( oldParent );
                newOldParent.removeFromPropertyNodeRefs( "ems:ownedChildren",
                                                         nodeToUpdate.getNodeRef() );

                // Now create in the new, new parent.
                parent = workspace.replicateWithParentFolders( parent ); // This gets the new, new parent.
                EmsScriptNode oldNode = nodeToUpdate;
                nodeToUpdate = nodeToUpdate.clone(parent);
                nodeToUpdate.setWorkspace( workspace, oldNode.getNodeRef() );
            }
        }

        if ( nodeToUpdate == null || !nodeToUpdate.exists() ) {// && newElements.contains( id ) ) {
            if ( type == null || type.trim().isEmpty() ) {
                if (Debug.isOn()) System.out.println( "PREFIX: type not found for " + jsonType );
                return null;
            } else {
                log( LogLevel.INFO, "\tcreating node" );
                try {
//                    if ( parent != null && parent.exists() ) {
                        nodeToUpdate = parent.createSysmlNode( id, acmSysmlType, modStatus, workspace );
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

                    // Resurrect any parent nodes if needed:
                    resurrectParents(nodeToUpdate, ingest);

                    // Update the aspect if the type has changed and its a aspect, or if it is
                    // being changed to an Element.  Need to call this for Elements for downgrading,
                    // which will remove all of the needed aspects.
                    if ( (!type.equals( acmSysmlType ) && NodeUtil.isAspect( acmSysmlType )) ||
                         acmSysmlType.equals(Acm.ACM_ELEMENT)) {
                        if (ingest && nodeToUpdate.createOrUpdateAspect( acmSysmlType )) {
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
            reifiedPkgNode = getOrCreateReifiedPackageNode(nodeToUpdate, id, workspace, true); // TODO -- Is last argument correct?

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

            // Special processing for elements with properties that are value specs:
            //  Note: this will modify elementJson
            if ( processValueSpecProperty(acmSysmlType, nestedNode, elementJson, specializeJson, nodeToUpdate,
                                        ingest, reifiedPkgNode, parent, id, workspace) ) {
                modStatus.setState( ModStatus.State.UPDATED );
            }

            // Don't modify modified time--let alfresco do that.
            if ( elementJson != null && elementJson.has( Acm.JSON_LAST_MODIFIED ) ) {
                elementJson.remove( Acm.JSON_LAST_MODIFIED );
            }
            timerIngest = Timer.startTimer(timerIngest, timeEvents);
            if ( nodeToUpdate.ingestJSON(elementJson) ) {
                Timer.stopTimer(timerIngest, "!!!!! updateOrCreateTransactionableElement(): ingestJSON", timeEvents);
                modStatus.setState( ModStatus.State.UPDATED );
            }

            // If it is a package, then create or delete the the site package if needed:
            if (nodeToUpdate.hasAspect(Acm.ACM_PACKAGE)) {
                handleSitePackage(nodeToUpdate, workspace);
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

        if ( nodeToUpdate != null ) nodeToUpdate.getOrSetCachedVersion();

        return nestedNode ? nodeToUpdate : reifiedPkgNode;
    }

    private EmsScriptNode createSitePkg(EmsScriptNode pkgSiteNode,
                                            WorkspaceNode workspace) {
        // site packages are only for major site, nothing to do with workspaces
        String siteName = "site_" + pkgSiteNode.getSysmlId();
        EmsScriptNode siteNode = getSiteNode( siteName, workspace, null, false );

        SiteInfo siteInfo = services.getSiteService().getSite( siteName );
        if ( siteInfo == null ) {
            String sitePreset = "site-dashboard";
            String siteTitle = pkgSiteNode.getSysmlName();
            String siteDescription = (String) pkgSiteNode.getProperty( Acm.ACM_DOCUMENTATION );
            boolean isPublic = true;
            if (false == ShareUtils.constructSiteDashboard( sitePreset, siteName, siteTitle, siteDescription, isPublic )) {
                // FIXME: add some logging and response here that there were issues creating the site
            }
            // siteInfo doesnt give the node ref we want, so must search for it:
            siteNode = getSiteNode( siteName, null, null );
            if (siteNode != null) {
                siteNode.createOrUpdateAspect( "cm:taggable" );
                siteNode.createOrUpdateAspect( Acm.ACM_SITE );
                siteNode.createOrUpdateProperty( Acm.ACM_SITE_PACKAGE, pkgSiteNode.getNodeRef() );
                pkgSiteNode.createOrUpdateAspect( Acm.ACM_SITE_CHARACTERIZATION);
                pkgSiteNode.createOrUpdateProperty( Acm.ACM_SITE_SITE, siteNode.getNodeRef() );
            }
        }

        return siteNode;
    }

    /**
     * Does processing for site packages.  Creates the alfresco Site for it, or
     * remove it based on the isSite property.  Sets the siteParent and
     * siteChildren properties if needed.
     *
     * @param nodeToUpdate
     * @param workspace
     */
    private void handleSitePackage(EmsScriptNode nodeToUpdate, WorkspaceNode workspace) {

        Boolean isSite = (Boolean) nodeToUpdate.getProperty( Acm.ACM_IS_SITE );

        if (isSite != null) {
            // Create site/permissions if needed:
            if (isSite) {
                EmsScriptNode pkgSiteNode = createSitePkg(nodeToUpdate, workspace);

                // Determine the parent package:
                // Note: will do this everytime, even if the site package node already existed, as the parent site
                //       could have changed with this post
                EmsScriptNode pkgSiteParentNode = findParentPkgSite(nodeToUpdate, siteNode,
                                                                    projectNode, workspace);

                // Add the children/parent properties:
                if (pkgSiteParentNode != null && pkgSiteNode != null) {
                    pkgSiteParentNode.appendToPropertyNodeRefs( Acm.ACM_SITE_CHILDREN,
                                                                pkgSiteNode.getNodeRef() );
                    pkgSiteNode.setProperty( Acm.ACM_SITE_PARENT, pkgSiteParentNode.getNodeRef() );
                }

            } // ends if (isSite)
            else {
                // Revert permissions to inherit
                services.getPermissionService().deletePermissions(nodeToUpdate.getNodeRef());
                nodeToUpdate.setInheritsPermissions( true );

                NodeRef reifiedPkg = (NodeRef) nodeToUpdate.getProperty( "ems:reifiedPkg" );
                if (reifiedPkg != null) {
                    services.getPermissionService().deletePermissions( reifiedPkg );
                    services.getPermissionService().setInheritParentPermissions( reifiedPkg, true );
                }
            }
        } // ends if (isSite != null)

    }

    protected EmsScriptNode getOrCreateReifiedPackageNode( EmsScriptNode node,
                                                           String id,
                                                           WorkspaceNode workspace,
                                                           boolean useParent ) {
        EmsScriptNode reifiedPkgNode = null;
        EmsScriptNode reifiedPkgNodeAll = null;

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
            try {
                parent = workspace.replicateWithParentFolders( parent );
            } catch ( Exception e ) {
                log( LogLevel.ERROR,
                     "\t failed to replicate folder, " + parent.getName()
                             + ", in workspace, "
                             + WorkspaceNode.getName( workspace ) );
                e.printStackTrace();
                //throw e; // pass it up the chain to roll back transaction // REVIEW -- compiler won't allow throw like below--why??
                return null;
            }
        }

        // If node is not in the correct workspace then clone it:
        //  Note: this can occur if the parent workspace has the reified node, but not the
        //        reified pkg when getOwner() calls this.  See CMED-501.
        if (!NodeUtil.workspacesEqual( workspace, node.getWorkspace() )) {
            node = node.clone( parent );

            if ( node == null || !node.exists() ) {
                log( LogLevel.ERROR,
                     "Clone failed for node id = " + id );
                return null;
            }
        }

        if (checkPermissions(parent, PermissionService.WRITE)) {
            String pkgName = id + "_pkg";
            reifiedPkgNodeAll = findScriptNodeById( pkgName, workspace, null, true );
            reifiedPkgNode = (reifiedPkgNodeAll != null && NodeUtil.workspacesEqual(reifiedPkgNodeAll.getWorkspace(),workspace)) ?
                                                                                                         reifiedPkgNodeAll : null;
            // Verify the reified pkg and node have the same site.
            // This is needed b/c of CMED-531 as the same pkg can be in multiple sites:
            reifiedPkgNode = (reifiedPkgNode != null && reifiedPkgNode.getSiteNode().equals( node.getSiteNode() )) ?
                                                                                              reifiedPkgNode : null;

            if (reifiedPkgNode == null || !reifiedPkgNode.exists()) {
                try {
                    reifiedPkgNode = parent.createFolder(pkgName, Acm.ACM_ELEMENT_FOLDER,
                                                         reifiedPkgNodeAll != null ? reifiedPkgNodeAll.getNodeRef() : null);
                } catch ( Throwable e ) {
                    log( LogLevel.ERROR,
                         "\t failed to create reified node " + pkgName
                                 + " in parent, "
                                 + parent.getSysmlId() + " = "
                                 + parent + " because of exception." );
                    throw e; // pass it up the chain to roll back transaction
                }
                if (reifiedPkgNode == null || !reifiedPkgNode.exists()) {
                    log( LogLevel.ERROR,
                         "\t failed to create reified node " + pkgName
                                 + " in parent, "
                                 + parent.getSysmlId() + " = "
                                 + parent );
                    return null;
                } else {
                    reifiedPkgNode.setProperty(Acm.ACM_ID, pkgName);

                    if ( useParent ) {
                        reifiedPkgNode.setProperty(Acm.ACM_NAME, (String) node.getProperty(Acm.ACM_NAME));
                    } else {
                        reifiedPkgNode.setProperty( Acm.ACM_NAME, pkgName.replaceAll( "_pkg$", "" ) );
                    }
                    log(LogLevel.INFO, "\tcreating " + pkgName + " in " + parent.getSysmlId() + " : " + reifiedPkgNode.getNodeRef().toString());
                }
            }
            if (checkPermissions(reifiedPkgNode, PermissionService.WRITE)) {
                foundElements.put(pkgName, reifiedPkgNode);

                // check for the case where the id isn't the same as the node
                // reference - this happens when creating a root level package
                // for example
                if ( !id.equals( node.getProperty( "sysml:id" ) )) {
                    node = findScriptNodeById(id, workspace, null, false);
                }

                if (node != null) {

                    // We are now setting the cm:name to the alfresco id.
                    // Note: this must be set after getting the correct node above
                    reifiedPkgNode.setProperty(Acm.CM_NAME, node.getName()+"_pkg");

                    // lets keep track of reification
                    node.createOrUpdateAspect( "ems:Reified" );
                    node.createOrUpdateProperty( "ems:reifiedPkg", reifiedPkgNode.getNodeRef() );

                    reifiedPkgNode.createOrUpdateAspect( "ems:Reified" );
                    reifiedPkgNode.createOrUpdateProperty( "ems:reifiedNode", node.getNodeRef() );
                }
            }
        }
        if ( reifiedPkgNode != null ) reifiedPkgNode.getOrSetCachedVersion();

        return reifiedPkgNode;
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
                      //      need to be able to handle all ValueSpecification types?
                      //      some of them have properties that point to nodes, so
                      //      would need to process them also
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

        Set<String> names = new LinkedHashSet<String>();

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

            //loop x times for now
            for(int i=0; i<10; i++)
            {
                // Loop through all the listeners:
                for (ParameterListenerImpl listener : cd.getAeClasses().values()) {

                    // TODO: REVIEW
                    //       Can we get duplicate ParameterListeners in the aeClassses map?
                    constraints.addAll( listener.getConstraints( true, null ) );
                }

                // Solve!!!!
                boolean result = false;
                try {
                    Debug.turnOn();
                    Random.reset();
                    result = solver.solve(constraints);
                    // loop to check thru all constraints

                } finally {
                    Debug.turnOff();
                }
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
        // Run without transactions since ModePost breaks them up itself.
        return instance.executeImplImpl(req,  status, cache, true);
    }

    @Override
    protected Map<String, Object> executeImplImpl(final WebScriptRequest req,
                                                  Status status, Cache cache) {
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

        if (logger.isDebugEnabled()) {
            logger.debug( user + " " + req.getURL() );
            logger.debug( req.parseContent() );
        }

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

        if (wsFound && validateRequest(req, status)) {
            try {
                if (runInBackground) {
                    saveAndStartAction(req, workspace, status);
                    response.append("JSON uploaded, model load being processed in background.\n");
                    response.append("You will be notified via email when the model load has finished.\n");
                }
                else {
                    JSONObject postJson = (JSONObject)req.parseContent();
                    JSONArray jarr = postJson.getJSONArray("elements");

                    if ( !Utils.isNullOrEmpty( expressionString ) ) {

                        JSONObject exprJson = new JSONObject(KExpParser.parseExpression(expressionString));
                        log(LogLevel.DEBUG, "********************************************************************************");
                        log(LogLevel.DEBUG, expressionString);
                        log(LogLevel.DEBUG, exprJson.toString(4));
//                        log(LogLevel.DEBUG, exprJson0.toString(4));
                        log(LogLevel.DEBUG, "********************************************************************************");
                        JSONArray expJarr = exprJson.getJSONArray("elements");
                        for (int i=0; i<expJarr.length(); ++i) {
                            jarr.put(expJarr.get( i ) );
                        }
                    }

                    // Get the project node from the request:
                    if (runWithoutTransactions || internalRunWithoutTransactions) {
                        getProjectNodeFromRequest( req, true );
                    }
                    else {
                        new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                            @Override
                            public void run() throws Exception {
                                getProjectNodeFromRequest( req, true );
                            }
                        };
                    }

                    if (projectNode != null) {
                        handleUpdate( postJson, status, workspace, fix, model, true );
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

        log( LogLevel.INFO, "ModelPost: " + timer );

        return model;
    }

    protected Set< EmsScriptNode > handleUpdate(JSONObject postJson, Status status, 
                                                WorkspaceNode workspace,
                                                boolean fix, Map<String, Object> model,
                                                boolean createCommit) throws Exception {
        JSONObject top = new JSONObject();
        final Set< EmsScriptNode > elements = createOrUpdateModel( postJson, status, workspace, null, createCommit );

        addRelationshipsToProperties( elements );
        if ( !Utils.isNullOrEmpty( elements ) ) {

            // Fix constraints if desired:
            if (fix) {               
                if (runWithoutTransactions || internalRunWithoutTransactions) {
                    fix(elements);
                }
                else {
                    new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                        @Override
                        public void run() throws Exception {
                            fix(elements);
                        }
                    };
                }
            }

            // Create JSON object of the elements to return:
            final JSONArray elementsJson = new JSONArray();
          
            if (runWithoutTransactions || internalRunWithoutTransactions) {
                for ( EmsScriptNode element : elements ) {
                    elementsJson.put( element.toJSONObject(null) );
                }            
            }
            else {
                new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                    @Override
                    public void run() throws Exception {
                        for ( EmsScriptNode element : elements ) {
                            elementsJson.put( element.toJSONObject(null) );
                        }
                    }
                };
            }
            
            top.put( "elements", elementsJson );
            if (!Utils.isNullOrEmpty(response.toString())) top.put("message", response.toString());
            if ( prettyPrint ) {
                model.put( "res", top.toString( 4 ) );
            } else {
                model.put( "res", top.toString() );
            }
        }

        return elements;
    }

    public void addRelationshipsToProperties( Set< EmsScriptNode > elems ) {
        
        for ( final EmsScriptNode element : elems ) {
            if (runWithoutTransactions || internalRunWithoutTransactions) {
                element.addRelationshipToPropertiesOfParticipants();
            }
            else {
                new EmsTransaction(getServices(), getResponse(), getResponseStatus() ) {
                    @Override
                    public void run() throws Exception {
                        element.addRelationshipToPropertiesOfParticipants();
                    }
                };
            }
        }
    }

    protected void saveAndStartAction( WebScriptRequest req,
                                       WorkspaceNode workspace,
                                       Status status ) throws Exception {

        // Find the siteNode and projectNode:
        getProjectNodeFromRequest(req, true);

        if (projectNode != null) {
            String projectId = projectNode.getSysmlId();

            String jobName = "Load Job " + projectId + ".json";
            EmsScriptNode jobNode = ActionUtil.getOrCreateJob(siteNode, jobName, "ems:Job", status, response);

            // write out the json
            ActionUtil.saveStringToFile(jobNode, "application/json", services, ((JSONObject)req.parseContent()).toString(4));

            // kick off the action
            ActionService actionService = services.getActionService();
            Action loadAction = actionService.createAction(ModelLoadActionExecuter.NAME);
            loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_ID, projectId);
            loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NAME, (String)projectNode.getProperty(Acm.ACM_NAME));
            loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NODE, projectNode);

            String workspaceId = getWorkspaceId( req );
            loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_WORKSPACE_ID, workspaceId);

            services.getActionService().executeAction(loadAction , jobNode.getNodeRef(), true, true);
        }
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

        WorkspaceNode workspace = getWorkspace( req );
        String projectId = getProjectId(req);
        String siteName = getSiteName(req);
        EmsScriptNode mySiteNode = getSiteNode( siteName, workspace, null, false );

        // If the site was not found and site was specified in URL, then return a 404.
        if (mySiteNode == null || !mySiteNode.exists()) {

            // Special case for when the site is not specified in the URL:
            if (siteName.equals( NO_SITE_ID )) {
                mySiteNode = createSite(siteName, workspace);
            }

            if (mySiteNode == null || !mySiteNode.exists()) {
                log(LogLevel.ERROR, "Site "+siteName+" could not be found in workspace "+workspace,
                    HttpServletResponse.SC_NOT_FOUND);
                return null;
            }
        }

        // Find the project site and site package node if applicable:
        Pair<EmsScriptNode,EmsScriptNode> sitePair = findProjectSite(req, siteName, workspace, mySiteNode);
        if (sitePair == null) {
            return null;
        }

        sitePackageNode = sitePair.first;
        siteNode = sitePair.second;  // Should be non-null

        if (sitePackageNode != null) {
            siteName = siteNode.getName();
        }

        setSiteInfoImpl(siteName); // Setting the site info in case we just created the site for the first time

        // If the project was not supplied on the URL, then look for the first project found within
        // the site.  Give a warning if multiple projects are found.  There is a requirement that
        // there should never be more than one project per site on Europa.
        if (projectId.equals( NO_PROJECT_ID )) {

            Map< String, EmsScriptNode > nodeList = searchForElements(NodeUtil.SearchType.TYPE.prefix,
                                                                    Acm.ACM_PROJECT, false,
                                                                    workspace, null,
                                                                    siteName);

            if (nodeList != null && nodeList.size() > 0) {
                EmsScriptNode projectNodeNew = nodeList.values().iterator().next();
                String projectIdNew = projectNodeNew != null ? projectNodeNew.getSysmlId() : projectId;
                projectId = projectIdNew != null ? projectIdNew : projectId;

                if (nodeList.size() > 1) {
                    log(LogLevel.WARNING, "ProjectId not supplied and multiple projects found for site "+siteName+" using ProjectId "+projectId);
                }
            }
        }

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
                json.put( Acm.JSON_NAME, projectId );
                pp.updateOrCreateProject( json, workspace, projectId, siteName,
                                          createIfNonexistent, false );
                projectNode = findScriptNodeById( projectId, workspace, null, false, siteName );
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
        setSiteInfoImpl(siteName);
    }

    private void setSiteInfoImpl(String siteName) {
        if (!siteName.startsWith(NodeUtil.sitePkgPrefix)) {
            siteInfo = services.getSiteService().getSite(siteName);
        }
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
