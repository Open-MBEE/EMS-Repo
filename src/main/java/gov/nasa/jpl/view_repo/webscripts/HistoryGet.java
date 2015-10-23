/**
 * 
 */
package gov.nasa.jpl.view_repo.webscripts;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.VersionHistory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import gov.nasa.jpl.view_repo.webscripts.ModelGet;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

/**
 * @author dank
 *
 */
public class HistoryGet extends ModelGet {

	static Logger logger = Logger.getLogger(HistoryGet.class);

	/**
	 * 
	 */
	public HistoryGet() {
		// TODO Auto-generated constructor stub
		super();
		System.out.println("HistoryGet No Args...");
	}

	/**
	 * 
	 * @param repositoryHelper
	 * @param registry
	 */
	public HistoryGet(Repository repositoryHelper, ServiceRegistry registry) {
		super(repositoryHelper, registry);
		System.out.println("HistoryGet with Args...");
	}

	/**
	 * 
	 * @param ref
	 * @return
	 */
	public JSONObject getAllHistory(NodeRef ref) {
		System.out.println("getAllHistory...");
		VersionHistory history = null;
		JSONObject jsonHistory = null;
		history = getServices().getVersionService().getVersionHistory(ref);
		jsonHistory = new JSONObject(history);
		System.out.println(jsonHistory.toString());
		return jsonHistory;
	}
    protected Map< String, Object > executeImpl( WebScriptRequest req,Status status, Cache cache ) {
    		HistoryGet instance = new HistoryGet( repository, getServices() );
    		return instance.executeImplImpl( req, status, cache,runWithoutTransactions );
}
	
	protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> history = new HashMap<String, Object>();
//		JSONObject top = NodeUtil.newJsonObject();
//		JSONArray historyJson = handleRequest(req, top);
		System.out.println("executeImplImpl...");
		
//		if(historyJson != null)
//		{
//			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,historyJson.toString());
//			System.out.println(historyJson.toString());
//		}
		EmsScriptNode nodeRef;
		
		nodeRef = getSiteNodeFromRequest(req, true);	
		if(nodeRef == null)
		{
			System.out.println("NodeRef Null...");
			return null;
		}
		
//		this.getAllHistory(nodeRef.getNodeRef());
		
		// Execute methods in here as if it is main
		return history;
	}
	// @Override
	// protected Map<String, Object> executeImpl(WebScriptRequest req, Status
	// status, Cache cache) {
	// HistoryGet instance = new HistoryGet( repository, getServices());
	//
	// // Execute methods in here as if it is main
	// 	return instance.executeImplImpl(req,
	// status,cache,runWithoutTransactions);
	// }

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Wrapper for handling a request and getting the appropriate JSONArray of
	 * elements
	 * 
	 * @param req
	 * @param top
	 * @return
	 */
//	private JSONArray handleRequest(WebScriptRequest req, final JSONObject top) {
//		// REVIEW -- Why check for errors here if validate has already been
//		// called? Is the error checking code different? Why?
//		System.out.println("Handle Request...");
//		try {
//			String[] idKeys = { "modelid", "elementid", "elementId" };
//			String modelId = null;
//			for (String idKey : idKeys) {
//				modelId = req.getServiceMatch().getTemplateVars().get(idKey);
//				if (modelId != null) {
//					break;
//				}
//			}
//
//			if (null == modelId) {
//				System.out.println("Model ID Null...");
//				log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element %s", modelId);
//				return new JSONArray();
//			}
//
//			// get timestamp if specified
//			String timestamp = req.getParameter("timestamp");
//			Date dateTime = TimeUtils.dateFromTimestamp(timestamp);
//			boolean connected = getBooleanArg(req, "connected", false);
//			boolean evaluate = getBooleanArg(req, "evaluate", false);
//			String relationship = req.getParameter("relationship");
//
//			WorkspaceNode workspace = getWorkspace(req);
//
//			// see if prettyPrint default is overridden and change
//			prettyPrint = getBooleanArg(req, "pretty", prettyPrint);
//
//			Long depth = 0l;
//			boolean includeQualified = getBooleanArg(req, "qualified", true);
//
//			if (logger.isDebugEnabled())
//				logger.debug("modelId = " + modelId);
//			boolean findDeleted = depth == 0 ? true : false;
//			EmsScriptNode modelRootNode = findScriptNodeById(modelId, workspace, dateTime, findDeleted);
//			if (logger.isDebugEnabled())
//				logger.debug("modelRootNode = " + modelRootNode);
//
//			if (modelRootNode == null) {
//				log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Element %s not found",
//						modelId + (dateTime == null ? "" : " at " + dateTime));
//				return new JSONArray();
//			} else if (modelRootNode.isDeleted()) {
//				log(Level.DEBUG, HttpServletResponse.SC_GONE, "Element exists, but is deleted.");
//				return new JSONArray();
//			}
//
//			if (isViewRequest) {
//				handleViewHierarchy(modelRootNode, workspace, dateTime, depth, new Long(0));
//			} else {
//				handleElementHierarchy(modelRootNode, workspace, dateTime, depth, new Long(0), connected, relationship,
//						new HashSet<String>());
//			}
//
//			boolean checkReadPermission = true; // TODO -- REVIEW -- Shouldn't
//												// this be false?
//			handleElements(workspace, dateTime, includeQualified, evaluate, top, checkReadPermission);
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//
//		return elements;
//	}
}
