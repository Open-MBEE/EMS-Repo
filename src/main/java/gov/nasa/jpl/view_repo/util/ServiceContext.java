package gov.nasa.jpl.view_repo.util;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.springframework.extensions.webscripts.Status;

public class ServiceContext {
    public boolean recursive = false;
    public boolean connected = false;
    public int depth = 1;
    public boolean evaluate = false;
    public boolean fix = false;
    //public Repository repository = null;
    public ServiceRegistry services = null;
    public StringBuffer response = null;
    public Status status = null;
 
    public ServiceContext() {
        super();
        this.services = NodeUtil.getServices();
        //this.repository = NodeUtil.getRepository();
    }

    public ServiceContext( boolean recursive, boolean connected, int depth,
                           boolean evaluate, boolean fix,
                           Repository repository, ServiceRegistry services,
                           StringBuffer response, Status status ) {
        this();
        this.recursive = recursive;
        this.connected = connected;
        this.depth = depth;
        this.evaluate = evaluate;
        this.fix = fix;
        //if ( repository == null ) this.repository = NodeUtil.getRepository();
        //else this.repository = repository;
        if ( services == null ) this.services = NodeUtil.getServices();
        else this.services = services;
        this.response = response;
        this.status = status;
    }

//    public ServiceContext(Repository repository, ServiceRegistry services,
//                          StringBuffer response, Status status) {
//        this();
//        this.repository = repository;
//        if ( services == null ) this.services = NodeUtil.getServices();
//        else this.services = services;
//        this.response = response;
//        this.status = status;
//    }

}
