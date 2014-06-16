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

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.eclipse.jetty.util.log.Log;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class SnapshotPost extends AbstractJavaWebScript {
    public SnapshotPost() {
        super();
    }
    
    public SnapshotPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected synchronized Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

        clearCaches();

        String viewId = null;
        String[] viewKeys = {"viewid", "productId"};
        for (String key: viewKeys) {
            viewId = req.getServiceMatch().getTemplateVars().get(key);
            if (viewId != null) {
                break;
            }
        }

        EmsScriptNode topview = findScriptNodeById(viewId, null);
        EmsScriptNode snapshotFolderNode = getSnapshotFolderNode(topview);

        Map<String, Object> model = new HashMap<String, Object>();

        // Don't do anything with the HTML, we just save off a copy of the generated JSON
//        String html;
//        try {
//            html = req.getContent().getContent();
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }

        DateTime now = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

        String snapshotName = viewId + "_" + now.getMillis();
        EmsScriptNode snapshotNode = null;
        if (checkPermissions(snapshotFolderNode, PermissionService.WRITE)) {
            snapshotNode = createSnapshot(topview, viewId, snapshotName, req.getContextPath(), snapshotFolderNode);
        }

        if (snapshotNode != null) {
            try {
                JSONObject snapshoturl = new JSONObject();
                snapshoturl.put("id", snapshotName);
                snapshoturl.put("creator", AuthenticationUtil.getFullyAuthenticatedUser());
                snapshoturl.put("created", fmt.print(now));
                snapshoturl.put("url", req.getContextPath() + "/service/snapshots/" + snapshotName);
                model.put("res", snapshoturl.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
                log(LogLevel.ERROR, "Error generating JSON for snapshot", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            log(LogLevel.ERROR, "Error creating snapshot node", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        
        status.setCode(responseStatus.getCode());
        if (status.getCode() != HttpServletResponse.SC_OK) {
            model.put("res", response.toString());
        }

        printFooter();

        return model;
    }

    public EmsScriptNode createSnapshot(EmsScriptNode view, String viewId) {
        String snapshotName = viewId + "_" + System.currentTimeMillis();
        String contextPath = "alfresco/service/";
        EmsScriptNode viewNode = findScriptNodeById(viewId, null);
        EmsScriptNode snapshotFolder = getSnapshotFolderNode(viewNode);
        return createSnapshot(view, viewId, snapshotName, contextPath, snapshotFolder);
    }
    
    
    public EmsScriptNode createSnapshot(EmsScriptNode view, String viewId, String snapshotName, String contextPath, EmsScriptNode snapshotFolder) {
        EmsScriptNode snapshotNode = snapshotFolder.createNode(snapshotName, "view2:Snapshot");
        snapshotNode.createOrUpdateProperty("cm:isIndexed", true);
        snapshotNode.createOrUpdateProperty("cm:isContentIndexed", false);
        snapshotNode.createOrUpdateProperty(Acm.ACM_ID, snapshotName);
        
        view.createOrUpdateAssociation(snapshotNode, "view2:snapshots");

        // This is deprecated so remove
//        MoaProductGet moaService = new MoaProductGet(repository, services);
//        moaService.setRepositoryHelper(repository);
//        moaService.setServices(services);
//        JSONObject snapshotJson = moaService.generateMoaProduct(viewId, contextPath, null);
//        if (snapshotJson == null) {
//            log(LogLevel.ERROR, "Could not generate the snapshot JSON", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            return null;
//        }

        JSONObject snapshotJson = new JSONObject();
        try {
            snapshotJson.put("snapshot", true);
            ActionUtil.saveStringToFile(snapshotNode, "application/json", services, snapshotJson.toString(4));
            createDocBook(view, viewId, snapshotName, contextPath, snapshotFolder);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        
        return snapshotNode;
    }
    
    private EmsScriptNode getChildrenViews(EmsScriptNode node){
    	log(LogLevel.INFO, "\ngetting " + node.getName() + " children views...");
    	JSONArray jsonArray = node.getChildrenViewsJSONArray();
    	for(int i = 0; i <  jsonArray.length(); i++){
    		try {
    			Object viewId = jsonArray.get(i);
				System.out.println(viewId.toString());
				EmsScriptNode viewNode = findScriptNodeById(viewId.toString(), null);
				System.out.println(viewNode.toJSON());
				View v = viewNode.getView();
				Collection<EmsScriptNode> displayedElems = v.getDisplayedElements();
				for(EmsScriptNode n: displayedElems){
					System.out.println(n.toJSON());
				}
				Collection<EmsScriptNode> childViewElems = v.getChildViewElements();
				for(EmsScriptNode n: childViewElems){
					System.out.println(n.toJSON());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
//    	Object property = node.getProperty( Acm.ACM_CHILDREN_VIEWS );
//    	System.out.println(jsonArray.toString());
    	
//    	System.out.println(property.toString());
    	return null;
    }
    
    private void createDocBook(EmsScriptNode view, String viewId, String snapshotName, String contextPath, EmsScriptNode snapshotFolder) {
    	log(LogLevel.INFO, "\ncreating DocBook snapshot for view Id: " + viewId);
    	if(view == null){
    		log(LogLevel.WARNING, "null [view] input parameter reference.");
    		return;
    	}
    	
    	View v = new View(view);
    	Collection<EmsScriptNode> v2v = v.getViewToViewPropertyViews();
    	EmsScriptNode childrenViews = null;
    	for(EmsScriptNode node:v2v){
    		
    		if(node.isView()){
    			System.out.println("View: " + node.getName());
//    			System.out.println("node JSON: " + node.toJSON());
    			childrenViews = node;
    			getChildrenViews(childrenViews);
    		}
    	}
    	
    	if(childrenViews == null) {
    		log(LogLevel.WARNING, "no children views found.");
    		return;
    	}
    	
    	//childrenViews.get
    }
    /**
     * Retrieve the snapshot folder for the view (goes up chain until it hits ViewEditor)
     * 
     * @param viewNode
     * @return
     */
    public static EmsScriptNode getSnapshotFolderNode(EmsScriptNode viewNode) {
        EmsScriptNode parent = viewNode.getParent();

        String parentName = (String) parent.getProperty(Acm.CM_NAME);
        while (!parentName.equals("Models") && !parentName.equals("ViewEditor")) {
            EmsScriptNode oldparent = parent;
            parent = oldparent.getParent();
            parentName = (String) parent.getProperty(Acm.CM_NAME);
        }
        // put snapshots at the project level
        parent = parent.getParent();
        
        EmsScriptNode snapshotNode = parent.childByNamePath("snapshots");
        if (snapshotNode == null) {
            snapshotNode = parent.createFolder("snapshots");
        }

        return snapshotNode;
    }
}
