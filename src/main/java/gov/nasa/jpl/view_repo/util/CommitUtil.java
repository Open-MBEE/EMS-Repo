package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript.LogLevel;

import java.util.Date;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.service.ServiceRegistry;

public class CommitUtil {
	private CommitUtil() {
		// prevent instantiation
	}

	public static void commitChangeSet(Set<EmsScriptNode> changeSet,
			boolean runWithoutTransactions, ServiceRegistry services) {
		if (runWithoutTransactions) {
			commitTransactionableChangeSet(changeSet);
		} else {
			UserTransaction trx;
			trx = services.getTransactionService()
					.getNonPropagatingUserTransaction();
			try {
				trx.begin();
				commitTransactionableChangeSet(changeSet);
				trx.commit();
			} catch (Throwable e) {
				try {
					// log(LogLevel.ERROR,
					// "commitChangeSet: DB transaction failed: " +
					// e.getMessage(),
					// HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					e.printStackTrace();
					trx.rollback();
				} catch (Throwable ee) {
					// log(LogLevel.ERROR, "commitChangeSet: rollback failed: "
					// + ee.getMessage());
					ee.printStackTrace();
				}
			}
		}
	}

	/**
	 * Make a commit change set that can be reverted
	 */
	public static void commitTransactionableChangeSet(
			Set<EmsScriptNode> changeSet) {
		EmsScriptNode commitPkg = null;
		for (EmsScriptNode changedNode : changeSet) {
			if (commitPkg != null) {
				EmsScriptNode siteNode = changedNode.getSiteNode();
				commitPkg = siteNode.childByNamePath("Commits");
				if (commitPkg == null) {
					commitPkg = siteNode.createFolder("Commits", "cm:folder");
				}
			}
		}

		EmsScriptNode commitNode = commitPkg.createNode(
				EmsScriptNode.getIsoTime(new Date()), "ems:ConfigurationSet");
		for (EmsScriptNode changedNode : changeSet) {
			commitNode.createOrUpdateAssociation(changedNode,
					"ems:configuredSnapshots", true);
		}
	}

	public static boolean revertCommit(EmsScriptNode commitNode, ServiceRegistry services) {
		boolean status = false;
		
		
		return status;
	}
}
