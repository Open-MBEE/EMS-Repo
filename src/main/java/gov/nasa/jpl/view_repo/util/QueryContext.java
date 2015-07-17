package gov.nasa.jpl.view_repo.util;

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
    
    public QueryContext( boolean justFirst, boolean exactMatch,
                         boolean includeDeleted, ServiceContext serviceContext) {
        this();
        this.justFirst = justFirst;
        this.exactMatch = exactMatch;
        this.includeDeleted = includeDeleted;
        this.serviceContext = serviceContext;
    }
    
}
