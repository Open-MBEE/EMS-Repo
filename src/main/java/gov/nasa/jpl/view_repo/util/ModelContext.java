package gov.nasa.jpl.view_repo.util;

import java.util.Date;


/**
 * ModelContext encapsulates many context variables that are often necessary to
 * interpret the meaning of a query or a set of model elements.
 */
public class ModelContext {
    public boolean ignoreWorkspaces = false;
    public WorkspaceNode workspace = null;
    public boolean onlyThisWorkspace = false;
    public Date dateTime = null;
    public String projectName = null;
    public EmsScriptNode projectNode = null;
    public String siteName = null;
    public EmsScriptNode siteNode = null;
    
    public ModelContext() {
        super();
    }

    public ModelContext( boolean ignoreWorkspaces, WorkspaceNode workspace,
                         boolean onlyThisWorkspace, Date dateTime,
                         String projectName, EmsScriptNode projectNode,
                         String siteName, EmsScriptNode siteNode ) {
        this();
        this.ignoreWorkspaces = ignoreWorkspaces;
        this.workspace = workspace;
        this.onlyThisWorkspace = onlyThisWorkspace;
        this.dateTime = dateTime;
        this.projectName = projectName;
        this.projectNode = projectNode;
        this.siteName = siteName;
        this.siteNode = siteNode;
        
    }
    
}
