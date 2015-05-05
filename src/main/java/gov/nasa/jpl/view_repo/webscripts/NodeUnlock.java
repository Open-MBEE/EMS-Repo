package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

@Deprecated
public class NodeUnlock extends DeclarativeWebScript {
    static Logger logger = Logger.getLogger( NodeUnlock.class );
    
    private ServiceRegistry services;
    private LockService lockService;

    public ServiceRegistry getServices() {
        return services;
    }

    public void setServices( ServiceRegistry services ) {
        this.services = services;
        lockService = this.services.getLockService();
    }

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map< String, Object > model = new HashMap< String, Object >();

        EmsScriptNode companyHome = NodeUtil.getCompanyHome( services );
        EmsScriptNode sites = companyHome.childByNamePath( "Sites" );

        boolean unlocked = unlockAllSiteNodes( sites );

        if (unlocked) {
            status.setCode( HttpServletResponse.SC_OK );
            model.put( "res", "unlocked" );
        } else {
            status.setCode( HttpServletResponse.SC_FORBIDDEN );
            model.put( "res", "could not unlock" );
        }
        
        
        return model;
    }

    private boolean unlockAllSiteNodes( EmsScriptNode parent ) {
        boolean status = true;
        for ( ChildAssociationRef assoc : parent.getChildAssociationRefs() ) {
            EmsScriptNode child = new EmsScriptNode( assoc.getChildRef(), services, new StringBuffer() );
            if ( !unlockAllSiteNodes( child ) ) {
                status = false;
            }
        }

        if ( parent.getIsLocked() ) {
            lockService.unlock( parent.getNodeRef() );
            LockStatus lockStatus = lockService.getLockStatus( parent.getNodeRef() );
            if (logger.isDebugEnabled()) {
                logger.debug( "unlocking: " + parent.getName() );
            }
            if ( !lockStatus.equals( LockStatus.NO_LOCK ) ) {
                status = false;
                if (logger.isInfoEnabled()) {
                    logger.info( "Could not remove lock on: " + parent.getName() );
                }
            }
        }

        return status;
    }

}
