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
//package gov.nasa.jpl.view_repo;
//
//// import AbstractContentTransformer2;
//
//import gov.nasa.jpl.view_repo.test.JavaQueryTest;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.alfresco.model.ContentModel;
//import org.alfresco.repo.module.AbstractModuleComponent;
//import org.alfresco.repo.nodelocator.NodeLocatorService;
//import org.alfresco.repo.nodelocator.XPathNodeLocator;
//import org.alfresco.service.cmr.repository.AssociationRef;
//import org.alfresco.service.cmr.repository.ContentService;
//import org.alfresco.service.cmr.repository.NodeRef;
//import org.alfresco.service.cmr.repository.NodeService;
//import org.alfresco.service.cmr.repository.NodeService.FindNodeParameters;
//import org.alfresco.service.cmr.search.SearchService;
//import org.alfresco.service.namespace.QName;
//import org.alfresco.service.namespace.RegexQNamePattern;
//
//// import org.apache.log4j.Logger;
//
///**
// * 
// */
//public class JavaQuery extends AbstractModuleComponent
//                       implements ModelInterface< NodeRef, String, Serializable, String, AssociationRef > {
//
//    public NodeService nodeService;
//
//    protected NodeLocatorService nodeLocatorService;
//
//    protected ContentService contentService;
//
//    protected SearchService searchService;
//
//    public void setContentService( ContentService contentService ) {
//        this.contentService = contentService;
//    }
//
//    public void setSearchService( SearchService searchService ) {
//        this.searchService = searchService;
//    }
//
//    public void setNodeService( NodeService nodeService ) {
//        this.nodeService = nodeService;
//    }
//
//    public void setNodeLocatorService( NodeLocatorService nodeLocatorService ) {
//        this.nodeLocatorService = nodeLocatorService;
//    }
//
//    public NodeRef getNode( String string ) {
//        Map< String, Serializable > params = new TreeMap<String, Serializable>();
//        params.put( XPathNodeLocator.QUERY_KEY, string );
//        //nodeService.findNodes( params );
//        return nodeLocatorService.getNode( "xpath", null, params );
//    }
//
//    // JUnit
//
//    @Override
//    public String getName( NodeRef object ) {
//        return (String)nodeService.getProperty( object, ContentModel.PROP_NAME );
//    }
//
//    @Override
//    public String getType( NodeRef object ) {
//        QName type = nodeService.getType( object );
//        if ( type == null ) return null;
//        return type.toPrefixString();
//    }
//
//    @Override
//    public Collection< Serializable > getProperties( NodeRef object ) {
//        Map< QName, Serializable > propMap = nodeService.getProperties( object );
//        return propMap.values();
//    }
//
//    @Override
//    public Serializable getProperty( NodeRef object, String propertyName ) {
//        Serializable prop =
//                nodeService.getProperty( object,
//                                         QName.createQName( propertyName ) );
//        return prop;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * gov.nasa.jpl.view_repo.ModelInterface#getRelationships(java.lang.Object)
//     */
//    @Override
//    public Collection< AssociationRef > getRelationships( NodeRef object ) {
//        List< AssociationRef > sassocs =
//                nodeService.getSourceAssocs( object,
//                                             new RegexQNamePattern( ".*" ) );
//        List< AssociationRef > tassocs =
//                nodeService.getTargetAssocs( object,
//                                             new RegexQNamePattern( ".*" ) );
//        List< AssociationRef > assocs = new ArrayList< AssociationRef >();
//        assocs.addAll( sassocs );
//        assocs.addAll( tassocs );
//        return assocs;
//    }
//
//    @Override
//    public AssociationRef getRelationship( NodeRef object,
//                                           String relationshipName ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Collection< NodeRef > getRelated( NodeRef object,
//                                             String relationshipName ) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    protected void executeInternal() throws Throwable {
//        System.out.println( "DemoComponent has been executed" );
//        JavaQueryTest.log.debug( "Test debug logging. Congratulation your AMP is working" );
//        JavaQueryTest.log.info( "This is only for information purposed. Better remove me from the log in Production" );
//    }
//
//}
