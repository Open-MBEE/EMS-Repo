/**
 * 
 */
package gov.nasa.jpl.view_repo;

// import AbstractContentTransformer2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.nodelocator.XPathNodeLocator;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
//import org.junit.Assert;

// import org.apache.log4j.Logger;

/**
 * 
 */
public class JavaQuery extends AbstractModuleComponent
                       implements ModelInterface< NodeRef, String, Serializable, String, AssociationRef > {

    public NodeService nodeService;

    protected NodeLocatorService nodeLocatorService;

    protected ContentService contentService;

    protected SearchService searchService;

    public void setContentService( ContentService contentService ) {
        this.contentService = contentService;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    public void setNodeService( NodeService nodeService ) {
        this.nodeService = nodeService;
    }

    public void setNodeLocatorService( NodeLocatorService nodeLocatorService ) {
        this.nodeLocatorService = nodeLocatorService;
    }

    /**
     * Returns the NodeRef of "Company Home"
     * 
     * @return
     */
    public NodeRef getCompanyHome()
    {
        return nodeLocatorService.getNode("companyhome", null, null);
    }

    public StoreRef getStoreRefByName( String storeName ) {
        List< StoreRef > stores = nodeService.getStores();
        if ( stores == null ) return null;
        for ( StoreRef store : stores ) {
            if ( store != null & store.getIdentifier().trim().equals( storeName.trim() ) ) {
                return store;
            }
        }
        return null;
    }
    
    
    
    public NodeRef getNode( String string ) {
        Map< String, Serializable > params = new TreeMap<String, Serializable>();
//        //int pos = string.indexOf( '/' );
//        String[] storePath = string.split( "/", 3 );
//        String storeName = null;
//        if ( storePath != null && storePath.length > 2 ) {
//            Assert.assertEquals(storePath.length, 3);
//            storeName = storePath[1];
//            params.put( XPathNodeLocator.STORE_ID_KEY, storeName );
//            string = "/" + storePath[2];
//        }
        params.put( XPathNodeLocator.QUERY_KEY, string );
        //nodeService.findNodes( params );
        NodeRef source = getCompanyHome();
//        if ( storeName != null ) {
//            StoreRef storeRef = getStoreRefByName(storeName);
//            rootNodes = nodeService.getAllRootNodes( storeRef );
//            
//            //nodeLocatorService.ge
//            //source = nodeService.getRootNode(  )
//        }
        return nodeLocatorService.getNode( "xpath", source, params );
    }

    // JUnit

    @Override
    public String getName( NodeRef object ) {
        return (String)nodeService.getProperty( object, ContentModel.PROP_NAME );
    }

    @Override
    public String getType( NodeRef object ) {
        QName type = nodeService.getType( object );
        if ( type == null ) return null;
        return type.toPrefixString();
    }

    @Override
    public Collection< Serializable > getProperties( NodeRef object ) {
        Map< QName, Serializable > propMap = nodeService.getProperties( object );
        return propMap.values();
    }

    @Override
    public Serializable getProperty( NodeRef object, String propertyName ) {
        Serializable prop =
                nodeService.getProperty( object,
                                         QName.createQName( propertyName ) );
        return prop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.nasa.jpl.view_repo.ModelInterface#getRelationships(java.lang.Object)
     */
    @Override
    public Collection< AssociationRef > getRelationships( NodeRef object ) {
        List< AssociationRef > sassocs =
                nodeService.getSourceAssocs( object,
                                             new RegexQNamePattern( ".*" ) );
        List< AssociationRef > tassocs =
                nodeService.getTargetAssocs( object,
                                             new RegexQNamePattern( ".*" ) );
        List< AssociationRef > assocs = new ArrayList< AssociationRef >();
        assocs.addAll( sassocs );
        assocs.addAll( tassocs );
        return assocs;
    }

    @Override
    public AssociationRef getRelationship( NodeRef object,
                                           String relationshipName ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection< NodeRef > getRelated( NodeRef object,
                                             String relationshipName ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void executeInternal() throws Throwable {
        System.out.println( "JavaQuery has been executed (although it does nothing by itself)" );
        //JavaQueryTest.log.debug( "Test debug logging. Congratulation your AMP is working" );
        //JavaQueryTest.log.info( "This is only for information purposed. Better remove me from the log in Production" );
    }

}
