package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.connections.RestPostConnection;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbEdgeTypes;
import gov.nasa.jpl.view_repo.util.JsonDiffDiff.DiffType;
import gov.nasa.jpl.view_repo.webscripts.MmsDiffGet;
import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;
import groovy.lang.Tuple;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

/**
 * Utilities for saving commits and sending out deltas based on commits
 * 
 * @author cinyoung
 *
 */
public class CommitUtil {
	static Logger logger = Logger.getLogger(CommitUtil.class);

	public static final String TYPE_BRANCH = "BRANCH";
	public static final String TYPE_COMMIT = "COMMIT";
	public static final String TYPE_DELTA = "DELTA";
	public static final String TYPE_MERGE = "MERGE";
	public static boolean cleanJson = false;

	private CommitUtil() {
		// defeat instantiation
	}

	private static JmsConnection jmsConnection = null;
	private static RestPostConnection restConnection = null;

	public static void setJmsConnection(JmsConnection jmsConnection) {
		if (logger.isInfoEnabled())
			logger.info("Setting jms");
		CommitUtil.jmsConnection = jmsConnection;
	}

	public static void setRestConnection(RestPostConnection restConnection) {
		if (logger.isInfoEnabled())
			logger.info("Setting rest");
		CommitUtil.restConnection = restConnection;
	}

	/**
	 * Gets the commit package in the specified workspace (creates if possible)
	 * 
	 * @param workspace
	 * @param services
	 * @param response
	 * @param create
	 * @return
	 */
	private static EmsScriptNode getOrCreateCommitPkg(WorkspaceNode workspace,
			ServiceRegistry services, StringBuffer response, boolean create) {
		EmsScriptNode context = null;

		// If it is the master branch then the commits folder is in company
		// home:
		if (workspace == null) {
			context = NodeUtil.getCompanyHome(services);
		}
		// Otherwise, it is in the workspace:
		else {
			context = workspace;
		}

		EmsScriptNode commitPkg = context.childByNamePath("commits");

		if (commitPkg == null && create) {
			commitPkg = context.createFolder("commits");
			// commits directory needs global permissions
			commitPkg.setPermission("SiteCollaborator", "GROUP_EVERYONE");
		}

		// Create the date folders if needed. Want to return the "commit" folder
		// if create is false:
		if (create) {
			commitPkg = NodeUtil.getOrCreateDateFolder(commitPkg);
		}

		if (commitPkg != null)
			commitPkg.getOrSetCachedVersion();
		if (context != null)
			context.getOrSetCachedVersion();

		return commitPkg;
	}

	/**
	 * Gets the commit package for the specified workspace
	 * 
	 * @param workspace
	 * @param siteName
	 * @param services
	 * @param response
	 * @return
	 */
	public static EmsScriptNode getCommitPkg(WorkspaceNode workspace,
			ServiceRegistry services, StringBuffer response) {
		return getOrCreateCommitPkg(workspace, services, response, false);
	}

	/**
	 * Given a workspace gets an ordered list of the commit history
	 * 
	 * @param workspace
	 * @param siteName
	 * @param services
	 * @param response
	 * @return
	 */
	public static ArrayList<EmsScriptNode> getCommits(WorkspaceNode workspace,
			ServiceRegistry services, StringBuffer response) {
		// to make sure no permission issues, run as admin
		String origUser = AuthenticationUtil.getRunAsUser();
		AuthenticationUtil.setRunAsUser("admin");

		ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();

		// Note: if workspace is null, then will get the master workspace
		// commits
		EmsScriptNode commitPkg = getCommitPkg(workspace, services, response);

		if (commitPkg != null) {
			commits.addAll(WebScriptUtil.getAllNodesInPath(
					commitPkg.getQnamePath(), "ASPECT", "ems:Committable",
					workspace, null, services, response));

			Collections
					.sort(commits,
							new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator());
		}

		AuthenticationUtil.setRunAsUser(origUser);

		return commits;
	}

	/**
	 * Gets the latest created folder in the passed context
	 * 
	 * @param context
	 * @return
	 */
	private static EmsScriptNode getLatestFolder(EmsScriptNode context) {

		return getLatestFolderBeforeTime(context, 0);
	}

	/**
	 * Gets the latest created folder in the passed context before or equal to
	 * the passed time. If the passed time is zero then gets the latest folder.
	 *
	 * @param context
	 * @return
	 */
	private static EmsScriptNode getLatestFolderBeforeTime(
			EmsScriptNode context, int time) {

		EmsScriptNode latestFolder = null;
		Set<EmsScriptNode> folders = context.getChildNodes();
		ArrayList<EmsScriptNode> foldersList = new ArrayList<EmsScriptNode>(
				folders);
		Collections
				.sort(foldersList,
						new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator());

		// Get the latest commit folder:
		if (time == 0) {
			if (foldersList.size() > 0) {
				latestFolder = foldersList.get(0);
			}
		}
		// Otherwise, get the latest commit folder before or equal the passed
		// time:
		else {
			for (EmsScriptNode folder : foldersList) {
				String folderName = folder.getName();
				Integer folderVal = Integer.valueOf(folderName);
				if (folderName != null && folderVal != null
						&& time >= folderVal) {
					latestFolder = folder;
					break;
				}
			}
		}

		return latestFolder;
	}

	/**
	 * Get the most recent commit in a workspace
	 * 
	 * @param ws
	 * @param siteName
	 * @param services
	 * @param response
	 * @return
	 */
	public static EmsScriptNode getLastCommit(WorkspaceNode ws,
			ServiceRegistry services, StringBuffer response) {

		EmsScriptNode yearFolder = null;
		EmsScriptNode monthFolder = null;
		EmsScriptNode dayFolder = null;
		ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();

		// Note: if workspace is null, then will get the master workspace
		// commits
		EmsScriptNode commitPkg = getCommitPkg(ws, services, response);

		if (commitPkg != null) {

			// Get the latest year/month/day folder and search for all content
			// within it,
			// then sort:
			yearFolder = getLatestFolder(commitPkg);
			if (yearFolder != null) {
				monthFolder = getLatestFolder(yearFolder);
				if (monthFolder != null) {
					dayFolder = getLatestFolder(monthFolder);
					if (dayFolder != null) {
						commits.addAll(dayFolder.getChildNodes());

						// Sort the commits so that the latest commit is first:
						Collections
								.sort(commits,
										new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator());
					}
				}
			}

		}

		// This method is too inefficient to use
		// ArrayList<EmsScriptNode> commits = getCommits(ws, services,
		// response);

		// Return the latest commit:
		if (commits.size() > 0) {
			return commits.get(0);
		}

		return null;
	}

	/**
	 * Return the latest commit before or equal to the passed date
	 */
	public static EmsScriptNode getLatestCommitAtTime(Date date,
			WorkspaceNode workspace, ServiceRegistry services,
			StringBuffer response) {

		EmsScriptNode yearFolder = null;
		EmsScriptNode monthFolder = null;
		EmsScriptNode dayFolder = null;
		ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();

		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int day = cal.get(Calendar.DAY_OF_MONTH);
			int month = cal.get(Calendar.MONTH) + 1; // Adding 1 b/c this is 0
														// to 11
			int year = cal.get(Calendar.YEAR);

			// Note: if workspace is null, then will get the master workspace
			// commits
			EmsScriptNode commitPkg = getCommitPkg(workspace, services,
					response);

			if (commitPkg != null) {

				// Get the latest year/day/month folder before the date:
				yearFolder = getLatestFolderBeforeTime(commitPkg, year);
				if (yearFolder != null) {
					monthFolder = getLatestFolderBeforeTime(yearFolder, month);
					if (monthFolder != null) {
						dayFolder = getLatestFolderBeforeTime(monthFolder, day);
						if (dayFolder != null) {
							commits.addAll(dayFolder.getChildNodes());

							// Sort the commits so that the latest commit is
							// first:
							Collections
									.sort(commits,
											new ConfigurationsWebscript.EmsScriptNodeCreatedAscendingComparator());
						}
					}
				}
			}

			// Now go through the list of commits to find the latest one
			// before or equal to the desired date:
			EmsScriptNode earliestCommit = null;
			for (EmsScriptNode commit : commits) {
				earliestCommit = commit;
				Date created = commit.getCreationDate();
				if (!date.before(created)) {
					return commit;
				}
			}

			// If we have not returned at this point, then the date must be
			// earlier than any of the
			// commits during the day of the date, so must try the previous
			// commit for the earliest
			// commit found:
			if (earliestCommit != null) {
				earliestCommit = getPreviousCommit(earliestCommit);
				if (earliestCommit != null
						&& !date.before(earliestCommit.getCreationDate())) {
					return earliestCommit;
				}
			}
		}

		return null;
	}

	public static String getTimestamp(EmsScriptNode commitNode, String workspace) {
		String type = null;

		// If the type is branch, must walk backwards to find a non-branch
		// commit:
		while (commitNode != null) {
			type = (String) commitNode.getProperty("ems:commitType");
			if (type != null && !TYPE_BRANCH.equals(type)) {
				break;
			}
			commitNode = getPreviousCommit(commitNode);
		}

		if (commitNode == null) {
			return null;
		}

		Object commit = commitNode.getProperty("ems:commit");
		Date creationDate = commitNode.getCreationDate();
		String timestamp = null;
		if (creationDate != null) {
			timestamp = TimeUtils.toTimestamp(creationDate);
		}

		if (!(commit instanceof String)) {
			return timestamp;
		}
		JSONObject latestCommit = new JSONObject((String) commit);
		JSONObject workspaceObject = null;
		if (latestCommit.length() == 0) {
			return timestamp;
		}
		if (workspace.equals("workspace1")) {
			workspaceObject = latestCommit.getJSONObject("workspace1");
		} else if (workspace.equals("workspace2")) {
			workspaceObject = latestCommit.getJSONObject("workspace2");
		} else {
			return timestamp;
		}
		return workspaceObject.getString("timestamp");
	}

	public static String replaceTimeStampWithCommitTime(Date date,
			WorkspaceNode ws, ServiceRegistry services, StringBuffer response) {

		EmsScriptNode lastCommit = date != null ? getLatestCommitAtTime(date,
				ws, services, response) : getLastCommit(ws, services, response);
		String timestamp = null;

		if (lastCommit != null) {
			timestamp = getTimestamp(lastCommit, "workspace2");
		}

		return timestamp;
	}

	/**
	 * Gets all the commits in the specified time range from the startWorkspace
	 * to the endWorkspace. If the endWorkspace is not a parent of the
	 * startWorkspace, this will search to the master.
	 * 
	 * @param fromDateTime
	 * @param toDateTime
	 * @param startWorkspace
	 * @param endWorkspace
	 * @param services
	 * @param response
	 * @return
	 */
	public static ArrayList<EmsScriptNode> getCommitsInDateTimeRange(
			Date fromDateTime, Date toDateTime, WorkspaceNode startWorkspace,
			WorkspaceNode endWorkspace, ServiceRegistry services,
			StringBuffer response) {
		// to make sure no permission issues, run as admin
		String originalUser = NodeUtil.getUserName();
		AuthenticationUtil.setRunAsUser("admin");

		// TODO REVIEW consider using date folders to narrow the range of
		// commits to be parsed
		// through, rather than using getLastCommit().

		// skip over too new workspaces
		while (startWorkspace != null) {
			Date created = startWorkspace.getCreationDate();
			if (toDateTime != null && created.after(toDateTime)) {
				startWorkspace = startWorkspace.getParentWorkspace();
			} else {
				break;
			}
		}
		// gather commits between dates while walking up workspace parents
		ArrayList<EmsScriptNode> commits = new ArrayList<EmsScriptNode>();
		while (true) { // run until endWorkspace or master
			EmsScriptNode commit = getLastCommit(startWorkspace, services,
					response);
			while (commit != null) {
				String endOfCommit = getTimestamp(commit, "workspace2");
				Date endDate = TimeUtils.dateFromTimestamp(endOfCommit);
				if (endDate == null)
					break;
				if (fromDateTime != null && !endDate.after(fromDateTime))
					break;
				String beginningOfCommit = getTimestamp(commit, "workspace1");
				Date beginningDate = TimeUtils
						.dateFromTimestamp(beginningOfCommit);
				if (beginningDate == null)
					break;
				if (toDateTime == null || beginningDate.before(toDateTime)) {
					commits.add(commit);
				}
				commit = getPreviousCommit(commit);
			}
			if (startWorkspace == null)
				break;
			if (startWorkspace.equals(endWorkspace))
				break;
			startWorkspace = startWorkspace.getParentWorkspace();
		}

		AuthenticationUtil.setRunAsUser(originalUser);
		return commits;
	}

	public static EmsScriptNode getPreviousCommit(EmsScriptNode commit) {
		List<EmsScriptNode> parentRefs = getPreviousCommits(commit);
		if (!Utils.isNullOrEmpty(parentRefs)) {
			return parentRefs.get(parentRefs.size() - 1); // last includes last
															// merge
		}
		return null;
	}

	public static EmsScriptNode getNextCommit(EmsScriptNode commit) {
		List<EmsScriptNode> childRefs = getNextCommits(commit);
		if (!Utils.isNullOrEmpty(childRefs)) {
			return childRefs.get(0);
		}
		return null;
	}

	public static List<EmsScriptNode> getPreviousCommits(EmsScriptNode commit) {
		@SuppressWarnings("unchecked")
		ArrayList<NodeRef> parentNodes = (ArrayList<NodeRef>) commit
				.getProperties().get(
						"{http://jpl.nasa.gov/model/ems/1.0}commitParents");
		List<EmsScriptNode> parentRefs = commit
				.toEmsScriptNodeList(parentNodes);
		return parentRefs;
	}

	public static List<EmsScriptNode> getNextCommits(EmsScriptNode commit) {
		@SuppressWarnings("unchecked")
		ArrayList<NodeRef> childrenNodes = (ArrayList<NodeRef>) commit
				.getProperties().get(
						"{http://jpl.nasa.gov/model/ems/1.0}commitChildren");
		List<EmsScriptNode> childRefs = commit
				.toEmsScriptNodeList(childrenNodes);
		return childRefs;
	}

	private static NodeRef commitRef = null;

	public synchronized static NodeRef commit(final WorkspaceNode workspace,
			final JSONObject body, final String msg,
			final boolean runWithoutTransactions,
			final ServiceRegistry services, final StringBuffer response) {
		// logger.warn( "sync commit start" );
		commitRef = null;
		new EmsTransaction(services, response, null, runWithoutTransactions) {

			@Override
			public void run() throws Exception {
				commitRef = commitTransactionable(workspace, body, msg,
						services, response);
			}
		};
		// logger.warn( "sync commit end" );
		return commitRef;
	}

	private static NodeRef commitTransactionable(WorkspaceNode workspace,
			JSONObject body, String msg, ServiceRegistry services,
			StringBuffer response) throws JSONException {
		return createCommitNode(workspace, null, workspace, null, null,
				TYPE_COMMIT, msg, body, services, response);
	}

	private static NodeRef mergeRef = null;

	public synchronized static NodeRef merge(final JSONObject wsDiff,
			final WorkspaceNode source, final WorkspaceNode target,
			final Date dateTimeSrc, final Date dateTimeTarget,
			final String msg, final boolean runWithoutTransactions,
			final ServiceRegistry services, final StringBuffer response) {
		// logger.warn( "sync merge start" );
		mergeRef = null;
		new EmsTransaction(services, response, null, runWithoutTransactions) {

			@Override
			public void run() throws Exception {
				mergeRef = mergeTransactionable(wsDiff, source, target, target,
						dateTimeSrc, dateTimeTarget, msg, services, response);
			}
		};
		// logger.warn( "sync merge end" );
		return mergeRef;
	}

	private static NodeRef mergeTransactionable(JSONObject wsDiff,
			WorkspaceNode source1, WorkspaceNode source2, WorkspaceNode target,
			Date dateTime1, Date dateTime2, String msg,
			ServiceRegistry services, StringBuffer response) {

		return createCommitNode(source1, source2, target, dateTime1, dateTime2,
				TYPE_MERGE, msg, wsDiff, services, response, true);
	}

	/**
	 */
	public static boolean revertCommit(EmsScriptNode commit,
			ServiceRegistry services) {
		boolean status = true;
		// String content = (String) commit.getProperty( "ems:commit" );
		// TODO: need to revert to original elements
		// TODO: revert moves.... this may not be easy
		// TODO: revert adds (e.g., make them deleted)
		// try {
		// JSONObject changeJson = new JSONObject(content);
		//
		// JSONArray changeArray = changeJson.getJSONArray(COMMIT_KEY);
		// for (int ii = 0; ii < changeArray.length(); ii++) {
		// JSONObject json = changeArray.getJSONObject(ii);
		// EmsScriptNode node = getScriptNodeByNodeRefId(json.getString(ID_KEY),
		// services);
		// if (node != null) {
		// String versionKey = json.getString(VERSION_KEY);
		// node.revert("reverting to version " + versionKey
		// + " as part of commit " + commit.getName(), false,
		// versionKey);
		// }
		// }
		// } catch (JSONException e) {
		// e.printStackTrace();
		// status = false;
		// }

		return status;
	}

	public static EmsScriptNode getScriptNodeByNodeRefId(String nodeId,
			ServiceRegistry services) {
		String store = EmsScriptNode.getStoreRef().toString();
		if (!nodeId.startsWith(store)) {
			nodeId = store + "/" + nodeId;
		}
		List<NodeRef> nodeRefs = NodeRef.getNodeRefs(nodeId);
		if (nodeRefs.size() == 1) {
			return new EmsScriptNode(nodeRefs.get(0), services);
		} else {
			if (nodeRefs.size() <= 0) {
				// not found
			} else {
				// too many found - couldn't disambiguate
			}
		}
		return null;
	}

	private static NodeRef branchRef = null;

	public synchronized static NodeRef branch(final WorkspaceNode srcWs,
			final WorkspaceNode dstWs, final String msg,
			boolean runWithoutTransactions, ServiceRegistry services,
			StringBuffer response) {
		// logger.warn( "sync branch start" );
		branchRef = null;
		new EmsTransaction(services, response, null, runWithoutTransactions) {

			@Override
			public void run() throws Exception {
				branchRef = branchTransactionable(srcWs, dstWs, msg, services,
						response);
			}
		};
		// logger.warn( "sync branch end" );
		return branchRef;
	}

	private static NodeRef branchTransactionable(WorkspaceNode srcWs,
			WorkspaceNode dstWs, String msg, ServiceRegistry services,
			StringBuffer response) throws JSONException {

		return createCommitNode(srcWs, null, dstWs, null, null, TYPE_BRANCH,
				msg, new JSONObject(), services, response);
	}

	// TODO -- REVIEW -- Just copied branch and search/replaced "branch" with
	// "merge"
	@Deprecated
	public static void merge(final WorkspaceNode srcWs,
			final WorkspaceNode dstWs, final String msg,
			boolean runWithoutTransactions, ServiceRegistry services,
			StringBuffer response) {
		new EmsTransaction(services, response, null, runWithoutTransactions) {

			@Override
			public void run() throws Exception {
				mergeTransactionable(srcWs, dstWs, dstWs, msg, services,
						response);
			}
		};
	}

	private static void mergeTransactionable(WorkspaceNode srcWs1,
			WorkspaceNode srcWs2, WorkspaceNode dstWs, String msg,
			ServiceRegistry services, StringBuffer response)
			throws JSONException {
		createCommitNode(srcWs1, srcWs2, dstWs, null, null, TYPE_MERGE, msg,
				new JSONObject(), services, response, true);
	}

	/**
	 * Update a commit with the parent and child information
	 * 
	 * @param prevCommit
	 *            Parent commit node
	 * @param currCommit
	 *            Child commit node
	 * @return
	 */
	protected static boolean updateCommitHistory(EmsScriptNode prevCommit,
			EmsScriptNode currCommit, String originalUser) {
		if (prevCommit == null || currCommit == null) {
			return false;
		} else {
			// This isn't necessary since the user should only be calling this
			// as admin from createCommitNode
			// if (!prevCommit.hasPermission( "Write" )) {
			// logger.error("no permissions to write to previous commit: " +
			// prevCommit);
			// return false;
			// }

			// FIXME: not sure why getting property is providing [null] array
			// ArrayList< NodeRef > parentRefs = currCommit.getPropertyNodeRefs(
			// "ems:commitParents" );
			@SuppressWarnings("unchecked")
			ArrayList<NodeRef> parentRefs = (ArrayList<NodeRef>) currCommit
					.getProperties().get(
							"{http://jpl.nasa.gov/model/ems/1.0}commitParents");
			if (parentRefs == null) {
				parentRefs = new ArrayList<NodeRef>();
			}
			NodeRef nr = prevCommit.getNodeRef();
			if (!parentRefs.contains(nr)) {
				parentRefs.add(nr);
			}
			currCommit.setProperty("ems:commitParents", parentRefs);

			// ArrayList< NodeRef > childRefs = prevCommit.getPropertyNodeRefs(
			// "ems:commitChildren" );
			@SuppressWarnings("unchecked")
			ArrayList<NodeRef> childRefs = (ArrayList<NodeRef>) prevCommit
					.getProperties()
					.get("{http://jpl.nasa.gov/model/ems/1.0}commitChildren");
			if (childRefs == null) {
				childRefs = new ArrayList<NodeRef>();
			}
			NodeRef nrCurr = currCommit.getNodeRef();
			if (!childRefs.contains(nrCurr)) {
				childRefs.add(nrCurr);
			}
			prevCommit.setProperty("ems:commitChildren", childRefs);
			prevCommit.getOrSetCachedVersion();

			// set modifier to original user (since we should be running as
			// admin in here)
			currCommit.setProperty("cm:modifier", originalUser, false, 0);
			prevCommit.setProperty("cm:modifier", originalUser, false, 0);
		}
		return true;
	}

	/**
	 * Create a commit node specifying the workspaces. Typically, since the
	 * serialization takes a while, the commit node is created first, then it is
	 * updated in the background using the ActionExecuter.
	 */
	protected static NodeRef createCommitNode(WorkspaceNode srcWs1,
			WorkspaceNode srcWs2, WorkspaceNode dstWs, Date dateTime1,
			Date dateTime2, String type, String msg, JSONObject body,
			ServiceRegistry services, StringBuffer response) {

		return createCommitNode(srcWs1, srcWs2, dstWs, dateTime1, dateTime2,
				type, msg, body, services, response, false);
	}

	/**
	 * Create a commit node specifying the workspaces. Typically, since the
	 * serialization takes a while, the commit node is created first, then it is
	 * updated in the background using the ActionExecuter.
	 */
	protected static NodeRef createCommitNode(WorkspaceNode srcWs1,
			WorkspaceNode srcWs2, WorkspaceNode dstWs, Date dateTime1,
			Date dateTime2, String type, String msg, JSONObject body,
			ServiceRegistry services, StringBuffer response,
			boolean twoSourceWorkspaces) {
		NodeRef result = null;
		// to make sure no permission issues, run as admin
		// the commit histories are updated based on the original user
		String originalUser = NodeUtil.getUserName();
		AuthenticationUtil.setRunAsUser("admin");

		// Get the most recent commit(s) before creating a new one
		// Note: must do this before getOrCreateCommitPkg() call in case the
		// commit to be created is the
		// first for the day, and so will create the day folder in the
		// getOrCreateCommitPkg() call
		EmsScriptNode prevCommit1 = dateTime1 != null ? getLatestCommitAtTime(
				dateTime1, srcWs1, services, response) : getLastCommit(srcWs1,
				services, response);
		EmsScriptNode prevCommit2 = null;
		if (twoSourceWorkspaces) {
			prevCommit2 = dateTime2 != null ? getLatestCommitAtTime(dateTime2,
					srcWs2, services, response) : getLastCommit(srcWs2,
					services, response);
		}

		EmsScriptNode commitPkg = getOrCreateCommitPkg(dstWs, services,
				response, true);

		if (commitPkg == null) {
			result = null;
		} else {
			Date now = new Date();
			if (!commitPkg.hasPermission("Write")) {
				logger.error("No permissions to write to commit directory: "
						+ commitPkg);
				result = null;
			} else {
				EmsScriptNode currCommit = commitPkg.createNode(
						"commit_" + now.getTime(), "cm:content");
				currCommit.createOrUpdateAspect("cm:titled");
				if (msg != null)
					currCommit.createOrUpdateProperty("cm:description", msg);

				currCommit.createOrUpdateAspect("ems:Committable");
				if (type != null) {
					currCommit.createOrUpdateProperty("ems:commitType", type);
				} else {
					// TODO throw exception
				}
				if (body != null) {
					// clean up JSON for small commits
					JSONObject cleanedJson = body;
					if (cleanJson) {
						cleanedJson = WorkspaceDiff.cleanWorkspaceJson(body);
					}
					currCommit.createOrUpdateProperty("ems:commit",
							cleanedJson.toString());
				}

				if (prevCommit1 != null) {
					updateCommitHistory(prevCommit1, currCommit, originalUser);
				}
				if (prevCommit2 != null) {
					updateCommitHistory(prevCommit2, currCommit, originalUser);
				}

				currCommit.setOwner(originalUser);
				currCommit.getOrSetCachedVersion();

				// Element cache does not support commit nodes.
				// NodeUtil.addElementToCache( currCommit );

				result = currCommit.getNodeRef();
			}
		}

		// make sure we're running back as the originalUser
		AuthenticationUtil.setRunAsUser(originalUser);

		return result;
	}

	private static void processDeltasForDb(JSONObject delta, String workspaceId) {

		PostgresHelper pgh = new PostgresHelper(workspaceId);

		JSONObject ws2 = delta.getJSONObject("workspace2");
		JSONArray added = ws2.optJSONArray("addedElements");
		JSONArray updated = ws2.optJSONArray("updatedElements");
		JSONArray deleted = ws2.optJSONArray("deletedElements");
		JSONArray moved = ws2.optJSONArray("movedElements");
		List<Pair<String, String>> addEdges = new ArrayList<Pair<String, String>>();
		List<Pair<String, String>> documentEdges = new ArrayList<Pair<String, String>>();

		try {
			pgh.connect();

			for (int i = 0; i < added.length(); i++) {
				JSONObject e = added.getJSONObject(i);
				pgh.insertNode(e.getString("nodeRefId"),
						e.getString("versionedRefId"), e.getString("sysmlid"));
				if (e.getString("owner") != null
						&& e.getString("sysmlid") != null) {
					Pair<String, String> p = new Pair<String, String>(
							e.getString("owner"), e.getString("sysmlid"));
					addEdges.add(p);
				}

				if (e.has("documentation")) {
					String doc = (String) e.getString("documentation");
					NodeUtil.processDocumentEdges(e.getString("sysmlid"), doc,
							documentEdges);
				}
				if (e.has("specialization")) {
				    JSONObject specialization = e.getJSONObject("specialization");
        				if (specialization.has("view2view")) {
        					JSONArray view2viewProperty = specialization.getJSONArray("view2view");
        					NodeUtil.processV2VEdges(e.getString("sysmlid"),
        							view2viewProperty, documentEdges);
        				} else if (specialization.has( "contents" )) {
        				    JSONObject contents = specialization.getJSONObject( "contents" );
        				    NodeUtil.processContentsJson( e.getString("sysmlid"), contents, documentEdges );
        				} else if (specialization.has( "instanceSpecificationSpecification" )) {
        				    JSONObject iss = specialization.getJSONObject( "instanceSpecificationSpecification" );
        				    NodeUtil.processInstanceSpecificationSpecificationJson( e.getString("sysmlid"), iss, documentEdges );
        				}
    				}
			}

			for (Pair<String, String> e : addEdges) {
				pgh.insertEdge(e.first, e.second, DbEdgeTypes.REGULAR);
			}

			for (int i = 0; i < deleted.length(); i++) {
				JSONObject e = deleted.getJSONObject(i);
				pgh.deleteEdgesForNode(e.getString("sysmlid"));
				pgh.deleteNode(e.getString("sysmlid"));
			}

			for (int i = 0; i < updated.length(); i++) {
				JSONObject e = updated.getJSONObject(i);
				pgh.updateNodeRefIds(e.getString("sysmlid"),
						e.getString("versionedRefId"), e.getString("nodeRefId"));

				if (e.has("documentation")) {
					String doc = (String) e.getString("documentation");
					NodeUtil.processDocumentEdges(e.getString("sysmlid"), doc,
							documentEdges);
				}
				if (e.has("specialization")
						&& e.getJSONObject("specialization").has("view2view")) {
					JSONArray view2viewProperty = e.getJSONObject(
							"specialization").getJSONArray("view2view");
					NodeUtil.processV2VEdges(e.getString("sysmlid"),
							view2viewProperty, documentEdges);
				}
			}

			for (int i = 0; i < moved.length(); i++) {
				JSONObject e = moved.getJSONObject(i);
				pgh.deleteEdgesForChildNode(e.getString("sysmlid"),
						DbEdgeTypes.REGULAR);
				if (e.getString("owner") != null)
					pgh.insertEdge(e.getString("owner"),
							e.getString("sysmlid"), DbEdgeTypes.REGULAR);
				// need to update the reference for the node
                pgh.updateNodeRefIds(e.getString("sysmlid"),
                                     e.getString("versionedRefId"), e.getString("nodeRefId"));
			}

			for (Pair<String, String> e : documentEdges) {
				pgh.insertEdge(e.first, e.second, DbEdgeTypes.DOCUMENT);
			}

			pgh.close();
		} catch (Exception e1) {
		    logger.warn( "Could not complete graph storage" );
			e1.printStackTrace();
		}

	}

	/**
	 * Send off the deltas to various endpoints
	 * 
	 * @param deltas
	 *            JSONObject of the deltas to be published
	 * @param projectId
	 *            String of the project Id to post to
	 * @param source
	 *            Source of the delta (e.g., MD, EVM, whatever, only necessary
	 *            for MD so it can ignore)
	 * @return true if publish completed
	 * @throws JSONException
	 */
	public static boolean sendDeltas(JSONObject deltaJson, String workspaceId,
			String projectId, String source) throws JSONException {
		boolean jmsStatus = false;
		boolean restStatus = false;

		processDeltasForDb(deltaJson, workspaceId);

		if (source != null) {
			deltaJson.put("source", source);
		}

		jmsStatus = sendJmsMsg(deltaJson, TYPE_DELTA, workspaceId, projectId);

		if (restConnection != null) {
			try {
				restStatus = restConnection.publish(deltaJson, InetAddress
						.getLocalHost().getHostName(), workspaceId, projectId);
			} catch (Exception e) {
				logger.warn("REST connection not available");
				return false;
			}
		}

		return jmsStatus && restStatus;
	}

	public static boolean sendBranch(WorkspaceNode src, WorkspaceNode created,
			Date srcDateTime) throws JSONException {
		JSONObject branchJson = new JSONObject();

		branchJson
				.put("sourceWorkspace", getWorkspaceDetails(src, srcDateTime)); // branch
																				// source
		branchJson.put("createdWorkspace",
				getWorkspaceDetails(created, srcDateTime)); // created branch

		PostgresHelper pgh = new PostgresHelper(src);

		try {
			pgh.connect();
			pgh.createBranchFromWorkspace(created.getId());
			pgh.close();
		} catch (ClassNotFoundException | java.sql.SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sendJmsMsg(branchJson, TYPE_BRANCH, null, null);
	}

	public static JSONObject getWorkspaceDetails(WorkspaceNode ws, Date date) {
		JSONObject json = new JSONObject();
		WorkspaceNode.addWorkspaceNamesAndIds(json, ws, false);
		if (null == date) {
			date = new Date();
		}
		json.put("time", TimeUtils.toTimestamp(date));
		return json;
	}

	public static boolean sendMerge(WorkspaceNode src, WorkspaceNode dst,
			Date srcDateTime) throws JSONException {
		JSONObject mergeJson = new JSONObject();

		mergeJson.put("sourceWorkspace", getWorkspaceDetails(src, srcDateTime));
		mergeJson.put("mergedWorkspace", getWorkspaceDetails(dst, null));

		return sendJmsMsg(mergeJson, TYPE_MERGE, null, null);
	}

	protected static boolean sendJmsMsg(JSONObject json, String eventType,
			String workspaceId, String projectId) {
		boolean status = false;
		if (jmsConnection != null) {
			status = jmsConnection.publish(json, eventType, workspaceId,
					projectId);
		} else {
			if (logger.isInfoEnabled())
				logger.info("JMS Connection not avalaible");
		}

		return status;
	}

	/**
	 * Send off progress to various endpoints
	 * 
	 * @param msg
	 *            String message to be published
	 * @param projectId
	 *            String of the project Id to post to
	 * @return true if publish completed
	 * @throws JSONException
	 */
	public static boolean sendProgress(String msg, String workspaceId,
			String projectId) {
		// FIXME: temporarily remove progress notifications until it's actually
		// ready to be used
		// boolean jmsStatus = false;
		//
		// if (jmsConnection != null) {
		// jmsConnection.setWorkspace( workspaceId );
		// jmsConnection.setProjectId( projectId );
		// jmsStatus = jmsConnection.publishTopic( msg, "progress" );
		// }
		//
		// return jmsStatus;
		return true;
	}

	private static JSONObject migrateCommitUsingDiff(JSONArray elements,
			JSONArray updated, JSONArray added, JSONArray deleted,
			WorkspaceNode ws1, WorkspaceNode ws2, Date dateTime1,
			Date dateTime2, StringBuffer response, Status responseStatus) {

		JSONObject firstElem = null;

		if (added != null && added.length() > 0) {
			firstElem = added.getJSONObject(0);
		} else if (updated != null && updated.length() > 0) {
			firstElem = updated.getJSONObject(0);
		} else if (deleted != null && deleted.length() > 0) {
			firstElem = deleted.getJSONObject(0);
		} else if (elements != null && elements.length() > 0) {
			firstElem = elements.getJSONObject(0);
		}

		if (firstElem != null) {

			if (!firstElem.has(Acm.JSON_SPECIALIZATION)) {

				// Perform the diff using the workspaces and timestamps
				// from the commit node:
				JSONObject json = MmsDiffGet.performDiff(ws1, ws2, dateTime1,
						dateTime2, response, responseStatus, DiffType.COMPARE,
						true, false);
				return json;
			}
		}

		return null;
	}

	private static void migrateJSONArray(WorkspaceNode workspace,
			Date dateTime, JSONArray oldJsonArray, JSONArray newJsonArray,
			Map<String, EmsScriptNode> nodeMap, StringBuffer response) {

		for (int i = 0; i < oldJsonArray.length(); i++) {
			JSONObject elementJson = oldJsonArray.getJSONObject(i);
			if (elementJson != null
					&& !elementJson.has(Acm.JSON_SPECIALIZATION)) {

				String sysmlId = elementJson.optString(Acm.JSON_ID);

				if (sysmlId != null) {

					EmsScriptNode node = null;
					if (nodeMap.containsKey(sysmlId)) {
						node = nodeMap.get(sysmlId);
					} else {
						node = NodeUtil.findScriptNodeById(sysmlId, workspace,
								dateTime, false, NodeUtil.getServices(),
								response, null);
						if (node != null) {
							nodeMap.put(sysmlId, node);
						}
					}

					if (node != null) {
						newJsonArray
								.put(node.toJSONObject(workspace, dateTime));
					}
				}
			}
		}
	}

	private static JSONObject migrateCommitUsingFind(JSONArray elements,
			JSONArray updated, JSONArray added, JSONArray deleted,
			JSONArray conflicted, JSONArray moved, WorkspaceNode ws1,
			WorkspaceNode ws2, JSONObject commitJson, Date dateTime1,
			Date dateTime2, StringBuffer response, Status responseStatus) {

		// Create new json from the old:
		JSONObject newJson = new JSONObject(commitJson.toString());

		JSONObject ws1Json = newJson.optJSONObject("workspace1");
		JSONObject ws2Json = newJson.optJSONObject("workspace2");

		JSONArray newElements = new JSONArray();
		JSONArray newUpdated = new JSONArray();
		JSONArray newAdded = new JSONArray();
		JSONArray newDeleted = new JSONArray();
		JSONArray newConflicted = new JSONArray();
		JSONArray newMoved = new JSONArray();

		if (ws1Json != null) {
			ws1Json.put("elements", newElements);
		}
		if (ws2Json != null) {
			ws2Json.put("addedElements", newAdded);
			ws2Json.put("updatedElements", newUpdated);
			ws2Json.put("deletedElements", newDeleted);
			ws2Json.put("conflictedElements", newConflicted);
			ws2Json.put("movedElements", newMoved);
		}

		Map<String, EmsScriptNode> ws1NodeMap = new HashMap<String, EmsScriptNode>();
		Map<String, EmsScriptNode> ws2NodeMap = new HashMap<String, EmsScriptNode>();

		if (elements != null) {
			migrateJSONArray(ws1, dateTime1, elements, newElements, ws1NodeMap,
					response);
		}
		if (updated != null) {
			migrateJSONArray(ws2, dateTime2, updated, newUpdated, ws2NodeMap,
					response);
		}
		if (added != null) {
			migrateJSONArray(ws2, dateTime2, added, newAdded, ws2NodeMap,
					response);
		}
		if (deleted != null) {
			migrateJSONArray(ws2, dateTime2, deleted, newDeleted, ws2NodeMap,
					response);
		}
		if (conflicted != null) {
			migrateJSONArray(ws2, dateTime2, conflicted, newConflicted,
					ws2NodeMap, response);
		}
		if (moved != null) {
			migrateJSONArray(ws2, dateTime2, moved, newMoved, ws2NodeMap,
					response);
		}

		return newJson;
	}

	/**
	 * Checks if the passed commitNode has all the needed information for glom
	 * diffs to work. Returns true if the node has been migrated, otherwise
	 * returns false.
	 * 
	 * @param commitNode
	 */
	public static boolean checkMigrateCommitNode(EmsScriptNode commitNode,
			StringBuffer response, Status responseStatus) {

		String origUser = NodeUtil.getUserName();
		boolean switchUser = !origUser.equals("admin");

		if (switchUser)
			AuthenticationUtil.setRunAsUser("admin");
		// to make sure no permission issues, run as admin

		String content = (String) commitNode.getProperty("ems:commit");
		try {
			JSONObject commitJson = new JSONObject(content);
			if (commitJson != null) {

				JSONObject ws1Json = commitJson.optJSONObject("workspace1");
				JSONObject ws2Json = commitJson.optJSONObject("workspace2");

				if (ws1Json != null && ws2Json != null) {

					String timestamp1 = ws1Json.optString("timestamp");
					String timestamp2 = ws2Json.optString("timestamp");

					if (timestamp1 == null || timestamp2 == null) {
						return false;
					} else {

						JSONArray elements = ws1Json.optJSONArray("elements");
						JSONArray added = ws2Json.optJSONArray("addedElements");
						JSONArray updated = ws2Json
								.optJSONArray("updatedElements");
						JSONArray deleted = ws2Json
								.optJSONArray("deletedElements");
						JSONArray conflicted = ws2Json
								.optJSONArray("conflictedElements");
						JSONArray moved = ws2Json.optJSONArray("movedElements");

						for (int i = 0; i < elements.length(); i++) {
							JSONObject elementJson = elements.getJSONObject(i);
							if (elementJson != null
									&& !elementJson
											.has(Acm.JSON_SPECIALIZATION)) {
								return false;
							}
						}

						for (int i = 0; i < added.length(); i++) {
							JSONObject elementJson = added.getJSONObject(i);
							if (elementJson != null
									&& !elementJson
											.has(Acm.JSON_SPECIALIZATION)) {
								return false;
							}
						}

						for (int i = 0; i < updated.length(); i++) {
							JSONObject elementJson = updated.getJSONObject(i);
							if (elementJson != null
									&& !elementJson
											.has(Acm.JSON_SPECIALIZATION)) {
								return false;
							}
						}

						for (int i = 0; i < deleted.length(); i++) {
							JSONObject elementJson = deleted.getJSONObject(i);
							if (elementJson != null
									&& !elementJson
											.has(Acm.JSON_SPECIALIZATION)) {
								return false;
							}
						}

						for (int i = 0; i < conflicted.length(); i++) {
							JSONObject elementJson = conflicted
									.getJSONObject(i);
							if (elementJson != null
									&& !elementJson
											.has(Acm.JSON_SPECIALIZATION)) {
								return false;
							}
						}

						for (int i = 0; i < moved.length(); i++) {
							JSONObject elementJson = moved.getJSONObject(i);
							if (elementJson != null
									&& !elementJson
											.has(Acm.JSON_SPECIALIZATION)) {
								return false;
							}
						}
					}

				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			if (switchUser)
				AuthenticationUtil.setRunAsUser(origUser);
		}

		return true;

	}

	/**
	 * Migrates the passed commitNode to have all the needed information for
	 * glom diffs to work.
	 * 
	 * @param commitNode
	 */
	public static void migrateCommitNode(EmsScriptNode commitNode,
			StringBuffer response, Status responseStatus) {

		String origUser = NodeUtil.getUserName();
		boolean switchUser = !origUser.equals("admin");

		if (switchUser)
			AuthenticationUtil.setRunAsUser("admin");
		// to make sure no permission issues, run as admin

		// Checks if the first element found in the commit json has a
		// specialization.
		// If it does not, then the commit node needs to be migrated by
		// re-computing
		// it using a non-glom diff:
		String content = (String) commitNode.getProperty("ems:commit");
		try {
			JSONObject commitJson = new JSONObject(content);
			if (commitJson != null) {

				JSONObject ws1Json = commitJson.optJSONObject("workspace1");
				JSONObject ws2Json = commitJson.optJSONObject("workspace2");

				if (ws1Json != null && ws2Json != null) {

					WorkspaceNode ws1 = null;
					WorkspaceNode ws2 = null;
					String ws1Name = null;
					String ws2Name = null;

					if (ws1Json.has("id")) {
						ws1Name = ws1Json.getString("id");
						ws1 = WorkspaceNode.getWorkspaceFromId(ws1Name,
								NodeUtil.getServices(), null, null, null);
					}
					if (ws2Json.has("id")) {
						ws2Name = ws2Json.getString("id");
						ws2 = WorkspaceNode.getWorkspaceFromId(ws2Name,
								NodeUtil.getServices(), null, null, null);
					}

					String timestamp1 = ws1Json.optString("timestamp");
					String timestamp2 = ws2Json.optString("timestamp");

					Date dateTime1 = TimeUtils.dateFromTimestamp(timestamp1);
					Date dateTime2 = TimeUtils.dateFromTimestamp(timestamp2);

					// If there is no timestamps (due to a previous bug with
					// cleaning the commit
					// nodes), then figure out reasonable ones using the past
					// and next commit nodes:
					boolean timesValid = false;

					if (dateTime1 == null) {
						EmsScriptNode prevCommit = getPreviousCommit(commitNode);
						if (prevCommit != null) {
							// dateTime1 = prevCommit.getLastModified( null );
							dateTime1 = prevCommit.getCreationDate();
						}
					}

					if (dateTime2 == null) {
						dateTime2 = commitNode.getCreationDate();
						// EmsScriptNode nextCommit = getNextCommit(commitNode);
						// if (nextCommit != null) {
						// dateTime2 = nextCommit.getCreationDate(); // TODO
						// REVIEW will this time be too late?
						// }
					}

					// Check that the times are valid:
					if (dateTime1 != null && dateTime2 != null) {
						timesValid = !dateTime1.after(dateTime2);
					} else {
						timesValid = dateTime1 != null
								|| (dateTime1 == null && dateTime2 == null);
					}

					if (timesValid && (ws1 != null || "master".equals(ws1Name))
							&& (ws2 != null || "master".equals(ws2Name))) {

						// Add the timestamps back into original json if they
						// werent there:
						if (Utils.isNullOrEmpty(timestamp1)) {
							Date newTime1 = dateTime1 == null ? new Date()
									: dateTime1;
							ws1Json.put("timestamp",
									TimeUtils.toTimestamp(newTime1));
						}
						if (Utils.isNullOrEmpty(timestamp2)) {
							Date newTime2 = dateTime2 == null ? new Date()
									: dateTime2;
							ws2Json.put("timestamp",
									TimeUtils.toTimestamp(newTime2));
						}

						JSONArray elements = ws1Json.optJSONArray("elements");
						JSONArray added = ws2Json.optJSONArray("addedElements");
						JSONArray updated = ws2Json
								.optJSONArray("updatedElements");
						JSONArray deleted = ws2Json
								.optJSONArray("deletedElements");
						JSONArray conflicted = ws2Json
								.optJSONArray("conflictedElements");
						JSONArray moved = ws2Json.optJSONArray("movedElements");

						/*
						 * // Not using the diff to compute the correct commit
						 * node b/c of bugs with the code, // and the
						 * performance hit JSONObject newCommitJson =
						 * migrateCommitUsingDiff( elements, updated, added,
						 * deleted, ws1, ws2, dateTime1, dateTime2, response,
						 * responseStatus );
						 */

						JSONObject newCommitJson = migrateCommitUsingFind(
								elements, updated, added, deleted, conflicted,
								moved, ws1, ws2, commitJson, dateTime1,
								dateTime2, response, responseStatus);

						// Update the commit node with the new json:
						if (newCommitJson != null) {
							// Can't use normal getProperty because we need to
							// bypass the cache.
							String lastModifier = (String) NodeUtil
									.getNodeProperty(commitNode, "cm:modifier",
											NodeUtil.getServices(), true, false);
							commitNode.createOrUpdateProperty("ems:commit",
									newCommitJson.toString());
							commitNode.createOrUpdateProperty("cm:modifier",
									lastModifier);
						}

					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			if (switchUser)
				AuthenticationUtil.setRunAsUser(origUser);
		}

	}

}
