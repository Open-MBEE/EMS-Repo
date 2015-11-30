package gov.nasa.jpl.view_repo.util;

import java.util.Date;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.springframework.extensions.webscripts.Status;

/**
 * QueryContext encapsulates many context variables that are often necessary to
 * interpret the meaning of a query.
 */
public class QueryContext extends ModelContext {
    public boolean justFirst = false;
    public boolean exactMatch = true;
    public boolean includeDeleted = false;
    public ServiceContext serviceContext = null;
    
    public QueryContext() {
        super();
    }
    
    public QueryContext( boolean ignoreWorkspaces, WorkspaceNode workspace,
                         boolean onlyThisWorkspace, Date dateTime,
                         boolean justFirst, boolean exactMatch,
                         boolean includeDeleted,
                         String projectName, EmsScriptNode projectNode,
                         String siteName, EmsScriptNode siteNode ) {
        this( ignoreWorkspaces, workspace, onlyThisWorkspace, dateTime,
              justFirst, exactMatch, includeDeleted, projectName, projectNode,
              siteName, siteNode, false, false, 0, false, false, null, null,
              null, null );
    }
    
    public QueryContext( boolean ignoreWorkspaces, WorkspaceNode workspace,
                         boolean onlyThisWorkspace, Date dateTime,
                         boolean justFirst, boolean exactMatch,
                         boolean includeDeleted, String projectName,
                         EmsScriptNode projectNode, String siteName,
                         EmsScriptNode siteNode, boolean recursive,
                         boolean connected, int depth, boolean evaluate,
                         boolean fix, Repository repository,
                         ServiceRegistry services, StringBuffer response,
                         Status status ) {
        super( ignoreWorkspaces, workspace, onlyThisWorkspace, dateTime,
               projectName, projectNode, siteName, siteNode );
        this.justFirst = justFirst;
        this.exactMatch = exactMatch;
        this.includeDeleted = includeDeleted;
//        this( ignoreWorkspaces, workspace, onlyThisWorkspace, dateTime,
//              justFirst, exactMatch, includeDeleted, projectName, projectNode,
//              siteName, siteNode, repository, services, response, status );
        serviceContext = new ServiceContext( recursive, connected, depth,
                                             evaluate, fix, repository, services,
                                             response, status );
    }
    
}
