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

package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

/**
 * Extension of ScriptNode to support EMS needs
 * 
 * @author cinyoung
 * 
 */
public class EmsScriptNode extends ScriptNode implements Comparator<EmsScriptNode>, Comparable< EmsScriptNode > {
    private static final long serialVersionUID = 9132455162871185541L;

    // provide logging capability of what is done
    private StringBuffer response = null;

    // provide status as necessary
    private Status status = null;

    boolean useFoundationalApi = true; // TODO this will be removed

    // protected static StoreRef storeRef = new
    // StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

    protected EmsScriptNode companyHome = null;

    protected EmsScriptNode siteNode = null;

    private View view;

    protected EmsScriptNode workspace = null;
    protected EmsScriptNode parentWorkspace = null;
    //protected String workspaceId = null;
    //protected String parentWorkspaceId = null;
    
    // TODO add nodeService and other member variables when no longer
    // subclassing ScriptNode
    // extend Serializable after removing ScriptNode extension

    // for lucene search
    // protected static final StoreRef SEARCH_STORE = new
    // StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

    public EmsScriptNode( NodeRef nodeRef, ServiceRegistry services,
                          StringBuffer response, Status status ) {
        this( nodeRef, services );
        setStatus( status );
    }

    public EmsScriptNode( NodeRef nodeRef, ServiceRegistry services,
                          StringBuffer response ) {
        this( nodeRef, services );
        setResponse( response );
    }

    @Override
    public EmsScriptNode childByNamePath( String path ) {
        ScriptNode child = super.childByNamePath( path );
        if ( child == null ) {
            return null;
        }
        return new EmsScriptNode( child.getNodeRef(), services, response );
    }

    @Override
    public EmsScriptNode createFile( String name ) {
        return new EmsScriptNode( super.createFile( name ).getNodeRef(),
                                  services, response, status );
    }

    @Override
    public EmsScriptNode createFolder( String name ) {
        return new EmsScriptNode( super.createFolder( name ).getNodeRef(),
                                  services, response, status );
    }

    @Override
    public EmsScriptNode createFolder( String name, String type ) {
        return new EmsScriptNode( super.createFolder( name, type ).getNodeRef(),
                                  services, response, status );
    }

    /**
     * Check whether or not a node has the specified aspect, add it if not
     * 
     * @param string
     *            Short name (e.g., sysml:View) of the aspect to look for
     * @return true if node updated with aspect
     */
    public boolean createOrUpdateAspect( String aspectName ) {
        if ( Acm.getJSON2ACM().keySet().contains( aspectName ) ) {
            aspectName = Acm.getACM2JSON().get( aspectName );
        }
        if ( !hasAspect( aspectName ) ) {
            return addAspect( aspectName );
        }
        return false;
    }

    /**
     * Check whether an association exists of the specified type between source
     * and target, create/update as necessary TODO: updating associations only
     * works for singular associations, need to expand to multiple NOTE: do not
     * use for child associations
     * 
     * @param target
     *            Target node of the association
     * @param type
     *            Short name of the type of association to create
     * @return true if association updated or created
     */
    public boolean createOrUpdateAssociation( ScriptNode target, String type ) {
        return createOrUpdateAssociation( target, type, false );
    }

    public boolean createOrUpdateAssociation( ScriptNode target, String type,
                                              boolean isMultiple ) {
        QName typeQName = createQName( type );
        List< AssociationRef > refs =
                services.getNodeService()
                        .getTargetAssocs( nodeRef, RegexQNamePattern.MATCH_ALL );

        if ( refs != null ) {
            // check all associations to see if there's a matching association
            for ( AssociationRef ref : refs ) {
                if ( ref.getTypeQName().equals( typeQName ) ) {
                    if ( ref.getSourceRef() != null
                         && ref.getTargetRef() != null ) {
                        if ( ref.getSourceRef().equals( nodeRef )
                             && ref.getTargetRef().equals( target.getNodeRef() ) ) {
                            // found it, no need to update
                            return false;
                        }
                    }
                    // TODO: need to check for multiple associations?
                    if ( !isMultiple ) {
                        // association doesn't match, no way to modify a ref, so
                        // need to remove then create
                        services.getNodeService()
                                .removeAssociation( nodeRef,
                                                    target.getNodeRef(),
                                                    typeQName );
                        break;
                    }
                }
            }
        }

        services.getNodeService().createAssociation( nodeRef,
                                                     target.getNodeRef(),
                                                     typeQName );
        return true;
    }

    public void removeAssociations( String type ) {
        QName typeQName = createQName( type );
        List< AssociationRef > refs =
                services.getNodeService()
                        .getTargetAssocs( nodeRef, RegexQNamePattern.MATCH_ALL );

        if ( refs != null ) {
            // check all associations to see if there's a matching association
            for ( AssociationRef ref : refs ) {
                if ( ref.getTypeQName().equals( typeQName ) ) {
                    services.getNodeService()
                            .removeAssociation( ref.getSourceRef(),
                                                ref.getTargetRef(), typeQName );
                }
            }
        }
    }

    /**
     * Create a child association between a parent and child node of the
     * specified type
     * 
     * // TODO investigate why alfresco repo deletion of node doesn't remove its
     * reified package
     * 
     * NOTE: do not use for peer associations
     * 
     * @param child
     *            Child node
     * @param type
     *            Short name of the type of child association to create
     * @return True if updated or created child relationship
     */
    public boolean
            createOrUpdateChildAssociation( ScriptNode child, String type ) {
        List< ChildAssociationRef > refs =
                services.getNodeService().getChildAssocs( nodeRef );
        QName typeQName = createQName( type );

        if ( refs != null ) {
            // check all associations to see if there's a matching association
            for ( ChildAssociationRef ref : refs ) {
                if ( ref.getTypeQName().equals( typeQName ) ) {
                    if ( ref.getParentRef().equals( nodeRef )
                         && ref.getChildRef().equals( child.getNodeRef() ) ) {
                        // found it, no need to update
                        return false;
                    } else {
                        services.getNodeService().removeChildAssociation( ref );
                        break;
                    }
                }
            }
        }

        services.getNodeService().addChild( nodeRef, child.getNodeRef(),
                                            typeQName, typeQName );
        return true;
    }

    /**
     * Check whether or not a node has a property, update or create as necessary
     * 
     * NOTE: this only works for non-collection properties - for collections
     * handwrite (or see how it's done in ModelPost.java)
     * 
     * @param acmType
     *            Short name for the Alfresco Content Model type
     * @param value
     *            Value to set property to
     * @return true if property updated, false otherwise (e.g., value did not
     *         change)
     */
    public < T extends Serializable > boolean
            createOrUpdateProperty( String acmType, T value ) {
        if ( value instanceof String ) {
            @SuppressWarnings( "unchecked" )
            T t = (T)extractAndReplaceImageData( (String)value );
            value = t;
        }
        @SuppressWarnings( "unchecked" )
        T oldValue = (T)getProperty( acmType );
        if ( oldValue != null ) {
            if ( !value.equals( oldValue ) ) {
                setProperty( acmType, value );
                log( getName() + ": " + acmType
                     + " property updated to value = " + value );
                return true;
            }
        } else {
            log( getName() + ": " + acmType + " property created with value = "
                 + value );
            setProperty( acmType, value );
        }
        return false;
    }

    public EmsScriptNode getCompanyHome() {
        if ( companyHome == null ) {
            companyHome = NodeUtil.getCompanyHome( services );
        }
        return companyHome;
    }

    public Set< NodeRef > getRootNodes() {
        return NodeUtil.getRootNodes( services );
    }

    public static String getMimeType( String type ) {
        Field[] fields = ClassUtils.getAllFields( MimetypeMap.class );
        for ( Field f : fields ) {
            if ( f.getName().startsWith( "MIMETYPE" ) ) {
                if ( ClassUtils.isStatic( f )
                     && f.getName().substring( 8 ).toLowerCase()
                         .contains( type.toLowerCase() ) ) {
                    try {
                        return (String)f.get( null );
                    } catch ( IllegalArgumentException e ) {} catch ( IllegalAccessException e ) {}
                }
            }
        }
        return null;
    }

    public long getChecksum( String dataString ) {
        byte[] data = null;
        data = dataString.getBytes(); // ( "UTF-8" );
        return getChecksum( data );
    }

    public long getChecksum( byte[] data ) {
        long cs = 0;
        Checksum checksum = new CRC32();
        checksum.update( data, 0, data.length );
        cs = checksum.getValue();
        return cs;
    }


//    public Set< EmsScriptNode > toEmsScriptNodeSet( ResultSet resultSet ) {
//        return toEmsScriptNodeSet( resultSet, services, response, status );
//    }
//    
//    // TODO -- move to NodeUtil or some other utility class
//    public static Set< EmsScriptNode > toEmsScriptNodeSet( ResultSet resultSet,
//                                                           ServiceRegistry services,
//                                                           StringBuffer response,
//                                                           Status status ) {
//        
//        Set< EmsScriptNode > emsNodeSet =
//                new TreeSet< EmsScriptNode >( new EmsScriptNodeComparator() );
//        for ( ResultSetRow row : resultSet ) {
//            NodeRef ref = row.getNodeRef();
//            if ( ref == null ) continue;
//            EmsScriptNode node =
//                    new EmsScriptNode( ref, services, response, status );
//            emsNodeSet.add( node );
//        }
//        return emsNodeSet;
//    }
    public Set< EmsScriptNode > toEmsScriptNodeSet( ArrayList<NodeRef> resultSet ) {
        return toEmsScriptNodeSet( resultSet, services, response, status );
    }
    
    public static Set< EmsScriptNode > toEmsScriptNodeSet( ArrayList<NodeRef> resultSet,
                                                           //Date dateTime,
                                                           ServiceRegistry services,
                                                           StringBuffer response,
                                                           Status status ) {
        
        Set< EmsScriptNode > emsNodeSet =
                new TreeSet< EmsScriptNode >( new EmsScriptNodeComparator() );
        for ( NodeRef ref : resultSet ) {
            //NodeRef ref = row.getNodeRef();
            if ( ref == null ) continue;
            EmsScriptNode node =
                    new EmsScriptNode( ref, services, response, status );
            emsNodeSet.add( node );
        }
        return emsNodeSet;
    }

    public EmsScriptNode findOrCreateArtifact( String name, String type,
                                               String base64content,
                                               String targetSiteName,
                                               String subfolderName ) {
        byte[] content =
                ( base64content == null )
                                         ? null
                                         : DatatypeConverter.parseBase64Binary( base64content );
        long cs = getChecksum( content );

        // see if image already exists by looking up by checksum
        ArrayList<NodeRef> refs = NodeUtil.findNodeRefsByType( "" + cs, SearchType.CHECKSUM.prefix, null, false, false, services );
//        ResultSet existingArtifacts =
//                NodeUtil.findNodeRefsByType( "" + cs, SearchType.CHECKSUM,
//                                             services );
//        Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( existingArtifacts );
        Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( refs );
//        existingArtifacts.close();

        EmsScriptNode matchingNode = null;

        if ( nodeSet != null && nodeSet.size() > 0 ) {
            matchingNode = nodeSet.iterator().next();
        }

        EmsScriptNode targetSiteNode =
                NodeUtil.getSiteNode( targetSiteName, null, services, response );
        // boolean nameMatch = false, subfolderMatch = false, siteMatch = false;
        // for ( EmsScriptNode art : nodeSet ) {
        // if ( art == null ) continue;
        // byte[] artContent = art.getContent() == null ? null :
        // art.getContent().getBytes();
        // if ( artContent == null && content != null ) continue;
        // // compare content to see if the file already exists
        // if ( artContent == content || art.getContent().getBytes().equals(
        // content ) ) {
        // // In case there are multiple files that have identical content,
        // // match based on name, site, and subfolder.
        // boolean isBest = false;
        // if ( matchingNode == null ) isBest = true;
        // boolean nameMatches = art.getName().equals( name );
        // if ( !isBest && !nameMatches && nameMatch ) continue;
        // if ( !isBest && nameMatches && !nameMatch ) isBest = true;
        // String artSiteName = art.getSiteName();
        // boolean siteMatches = artSiteName != null &&
        // artSiteName.equals(targetSiteName);
        // if ( !isBest && !siteMatches && siteMatch ) continue;
        // if ( !isBest && siteMatches && !siteMatch ) isBest = true;
        // boolean subfolderMatches = art.getDisplayPath().contains(
        // subfolderName );
        // if ( !isBest && !subfolderMatches && subfolderMatch ) continue;
        // if ( !isBest && subfolderMatches && !subfolderMatch ) isBest = true;
        // if ( isBest ) {
        // matchingNode = art;
        // nameMatch = nameMatches;
        // siteMatch = siteMatches;
        // subfolderMatch = subfolderMatches;
        // }
        // }
        // }

        if ( matchingNode != null ) return matchingNode;

        // create new artifact

        // find subfolder in site or create it
        String artifactFolderName =
                "Artifacts" + ( Utils.isNullOrEmpty( subfolderName )
                                ? "" : "/" + subfolderName );
        // find site; it must exist!
        if ( targetSiteNode == null ) {
            log( "Can't find node for site: " + targetSiteName + "!" );
            return null;
        }
        // find or create subfolder
        EmsScriptNode subfolder =
                NodeUtil.mkdir( targetSiteNode, artifactFolderName, services,
                                response, status );
        if ( subfolder == null ) {
            log( "Can't create subfolder for site, " + targetSiteName
                 + ", in artifact folder, " + artifactFolderName + "!" );
            return null;
        }

        String artifactId = name + "." + type;
        EmsScriptNode artifactNode =
                subfolder.createNode( artifactId, "cm:content" );
        if ( artifactNode == null ) {
            log( "Failed to create new artifact " + artifactId + "!" );
            return null;
        }

        artifactNode.addAspect( "cm:indexControl" );
        artifactNode.createOrUpdateProperty( "cm:isIndexed", true );
        artifactNode.createOrUpdateProperty( "cm:isContentIndexed", false );
        artifactNode.addAspect( Acm.ACM_IDENTIFIABLE );
        artifactNode.createOrUpdateProperty( Acm.ACM_ID, artifactId );
        artifactNode.addAspect( "view:Checksummable" );
        artifactNode.createOrUpdateProperty( "view:cs", cs );

        if (Debug.isOn()) System.out.println( "Creating artifact with indexing: "
                            + artifactNode.getProperty( "cm:isIndexed" ) );
        ContentWriter writer =
                services.getContentService()
                        .getWriter( artifactNode.getNodeRef(),
                                    ContentModel.PROP_CONTENT, true );
        InputStream contentStream = new ByteArrayInputStream( content );
        writer.putContent( contentStream );

        ContentData contentData = writer.getContentData();
        contentData =
                ContentData.setMimetype( contentData, getMimeType( type ) );
        services.getNodeService().setProperty( artifactNode.getNodeRef(),
                                               ContentModel.PROP_CONTENT,
                                               contentData );
        return artifactNode;
    }

    public String extractAndReplaceImageData( String value ) {
        if ( value == null ) return null;
        String v = value;
        while ( true ) {
            Pattern p =
                    Pattern.compile( "(.*)<img\\s*src\\s*=\\s*[\"']data:image/(\\w*);base64,([^\"']*)[\"'][^>]*>(.*)" );
            Matcher m = p.matcher( v );
            if ( !m.matches() ) {
                break;
            } else {
                if ( m.groupCount() != 4 ) {
                    log( "Expected 4 match groups, got " + m.groupCount()
                         + "! " + m );
                    break;
                }
                String extension = m.group( 2 );
                String content = m.group( 3 );
                String name = "img_" + System.currentTimeMillis();
                EmsScriptNode artNode =
                        findOrCreateArtifact( name, extension, content,
                                              getSiteName(), "images" );
                if ( artNode == null ) {
                    log( "Failed to pull out image data for value! " + value );
                    break;
                }

                String url = artNode.getUrl();
                String link = "<img src=\"" + url + "\"/>";
                link =
                        link.replace( "/d/d/",
                                      "/alfresco/service/api/node/content/" );
                v = m.group( 1 ) + link + m.group( 4 );
            }
        }
        return v;
    }

    public String getSiteTitle() {
        EmsScriptNode siteNode = getSiteNode();
        return (String)siteNode.getProperty( Acm.CM_TITLE );
    }

    public String getSiteName() {
        if ( siteName == null ) {
            EmsScriptNode siteNode = getSiteNode();
            if ( siteNode != null ) siteName = siteNode.getName();
        }
        return siteName;
    }

//    /**
//     * Checks and updates properties that have multiple values
//     * 
//     * @param type
//     *            Short name of the content model property to be updated
//     * @param array
//     *            New list of values to update
//     * @param valueType
//     *            The value type (needed for casting and making things generic)
//     * @return True if values updated/create, false if unchanged
//     * @throws JSONException
//     */
//    public < T extends Serializable > boolean
//            createOrUpdatePropertyValues( String type, JSONArray array )
//                    throws JSONException {
//        ArrayList< T > values = new ArrayList< T >();
//        for ( int ii = 0; ii < array.length(); ii++ ) {
//            @SuppressWarnings( "unchecked" )
//            T value = (T)array.get( ii );
//            if ( value instanceof String ) {
//                @SuppressWarnings( "unchecked" )
//                T t = (T)extractAndReplaceImageData( (String)value );
//                value = t;
//            }
//            values.add( value );
//        }
//
//        @SuppressWarnings( "unchecked" )
//        ArrayList< T > oldValues = (ArrayList< T >)getProperty( type );
//        if ( !checkIfListsEquivalent( oldValues, values ) ) {
//            setProperty( type, values );
//        } else {
//            return false;
//        }
//
//        return true;
//    }

//    /**
//     * Checks and updates properties that have multiple values
//     * 
//     * @param type
//     *            Short name of the content model property to be updated
//     * @param array
//     *            New list of values to update
//     * @param valueType
//     *            The value type (needed for casting and making things generic)
//     * @return True if values updated/create, false if unchanged
//     * @throws JSONException
//     */
//    public < T extends Serializable > boolean
//            createOrUpdatePropertyValues( String type, JSONArray array,
//                                          T valueType ) throws JSONException {
//        ArrayList< T > values = new ArrayList< T >();
//        for ( int ii = 0; ii < array.length(); ii++ ) {
//            @SuppressWarnings( "unchecked" )
//            T value = (T)array.get( ii );
//            if ( value instanceof String ) {
//                @SuppressWarnings( "unchecked" )
//                T t = (T)extractAndReplaceImageData( (String)value );
//                value = t;
//            }
//            values.add( value );
//        }
//
//        @SuppressWarnings( "unchecked" )
//        ArrayList< T > oldValues = (ArrayList< T >)getProperty( type );
//        if ( !checkIfListsEquivalent( oldValues, values ) ) {
//            setProperty( type, values );
//        } else {
//            return false;
//        }
//
//        return true;
//    }

    /**
     * Utility to compare lists of node refs to one another
     * 
     * @param x
     *            First list to compare
     * @param y
     *            Second list to compare
     * @return true if same, false otherwise
     */
    public static < T extends Serializable > boolean
            checkIfListsEquivalent( List< T > x, List< T > y ) {
        if ( x == null || y == null ) {
            return false;
        }
        if ( x.size() != y.size() ) {
            return false;
        }
        for ( int ii = 0; ii < x.size(); ii++ ) {
            if ( !x.get( ii ).equals( ii ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create an EmsScriptNode adding aspects based on the input sysml type
     * name.
     * 
     * @param sysmlId
     *            the @sysml:id which is also the @cm:name
     * @param sysmlAcmType
     *            Alfresco Content Model type of node to create or an aspect
     * @return created child EmsScriptNode
     */
    public EmsScriptNode createSysmlNode( String sysmlId, String sysmlAcmType ) {
        String type = NodeUtil.getContentModelTypeName( sysmlAcmType, services );
        EmsScriptNode node = createNode( sysmlId, type );

        if ( node != null && !type.equals( sysmlAcmType )
             && NodeUtil.isAspect( sysmlAcmType ) ) {
            node.createOrUpdateAspect( sysmlAcmType );
        }
        return node;
    }
    
    /**
     * Override createNode to return an EmsScriptNode
     * 
     * @param name
     *            cm:name of node (which may also be the sysml:id) 
     * @param type
     *            Alfresco Content Model type of node to create
     * @return created child EmsScriptNode
     */
    @Override
    public EmsScriptNode createNode( String name, String type ) {
        EmsScriptNode result = null;
        // Date start = new Date(), end;

        // if ( type == null ) {
        // type = "sysml:Element";
        // }
        if ( !useFoundationalApi ) {
            result =
                    new EmsScriptNode( super.createNode( name, type )
                                            .getNodeRef(), services, response );
        } else {
            Map< QName, Serializable > props =
                    new HashMap< QName, Serializable >( 1, 1.0f );
            // don't forget to set the name
            props.put( ContentModel.PROP_NAME, name );

            QName typeQName = createQName( type );
            if ( typeQName != null ) {
                try {
                    ChildAssociationRef assoc =
                            services.getNodeService()
                                    .createNode( nodeRef,
                                                 ContentModel.ASSOC_CONTAINS,
                                                 QName.createQName( NamespaceService.CONTENT_MODEL_1_0_URI,
                                                                    QName.createValidLocalName( name ) ),
                                                 createQName( type ), props );
                    result =
                            new EmsScriptNode( assoc.getChildRef(), services,
                                               response );
                } catch ( Exception e ) {
                    if (Debug.isOn()) System.out.println( "Got exception in "
                                        + "createNode(name=" + name + ", type="
                                        + type + ") for EmsScriptNode(" + this
                                        + ") calling createNode(nodeRef="
                                        + nodeRef + ", . . .)" );
                    e.printStackTrace();
                }

            } else {
                log( "Could not find type " + type );
            }
        }

        // end = new Date(); if (Debug.isOn()) System.out.println("\tcreateNode: " +
        // (end.getTime()-start.getTime()));
        return result;
    }

    /**
     * Return the first AssociationRef of a particular type
     * 
     * @param type
     *            Short name for type to filter on
     * @return
     */
    public EmsScriptNode getFirstAssociationByType( String type ) {
        List< AssociationRef > assocs =
                services.getNodeService()
                        .getTargetAssocs( nodeRef, RegexQNamePattern.MATCH_ALL );
        if ( assocs != null ) {
            // check all associations to see if there's a matching association
            for ( AssociationRef ref : assocs ) {
                if ( ref.getTypeQName().equals( createQName( type ) ) ) {
                    return new EmsScriptNode( ref.getTargetRef(), services,
                                              response );
                }
            }
        }
        return null;
    }

    /**
     * Get list of ChildAssociationRefs
     * 
     * @return
     */
    public List< ChildAssociationRef > getChildAssociationRefs() {
        return services.getNodeService().getChildAssocs( nodeRef );
    }

    @Override
    public String getName() {
        return (String)getProperty( Acm.CM_NAME );
    }

    public String getSysmlName() {
        return (String)getProperty( Acm.ACM_NAME );
    }

    @Override
    public EmsScriptNode getParent() {
        return new EmsScriptNode( super.getParent().getNodeRef(), services,
                                  response );
    }

    /**
     * Get the property of the specified type
     * 
     * @param acmType
     *            Short name of property to get
     * @return
     */
    public Object getProperty( String acmType ) {
        if ( useFoundationalApi ) {
            return services.getNodeService()
                           .getProperty( nodeRef, createQName( acmType ) );
        } else {
            return getProperties().get( acmType );
        }
    }

    
    public Date getLastModified(Date dateTime) {
        Date lastModifiedDate = (Date)getProperty( Acm.ACM_LAST_MODIFIED );

        Object value = getProperty( Acm.ACM_VALUE );
        ArrayList<NodeRef> dependentNodes = new ArrayList<NodeRef>();
        if ( value instanceof Collection ) {
            Collection< ? > c = (Collection< ? >)value;
            dependentNodes.addAll( Utils.asList( c, NodeRef.class ) );
        }
        for ( NodeRef nodeRef : dependentNodes ) {
            nodeRef = NodeUtil.getNodeRefAtTime( nodeRef, dateTime );
            if ( nodeRef == null ) continue;
            EmsScriptNode oNode = new EmsScriptNode(nodeRef, services);
            Date modified = oNode.getLastModified( dateTime );
            if ( modified.after( lastModifiedDate ) ) {
                lastModifiedDate = modified;
            }
        }
        return lastModifiedDate;
    }
    
    // @Override
    // public Map<String, Object> getProperties()
    // {
    //
    // Map<QName, Serializable> props =
    // services.getNodeService().getProperties(nodeRef);
    // // TODO replace w/ this.properties after no longer subclassing, maybe use
    // QNameMap also
    // Map<String, Object> finalProps = new HashMap<String, Object>();
    //
    // // Create map of string representation of QName to the value of the
    // property:
    // for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
    // finalProps.put(entry.getKey().toString(), entry.getValue());
    // }
    //
    // return finalProps;
    // }

    /**
     * Get the properties of this node
     * 
     * @param acmType
     *            Short name of property to get
     * @return
     */
    @Override
    public Map< String, Object > getProperties() {
        if ( useFoundationalApi ) {
            return Utils.toMap( services.getNodeService()
                                        .getProperties( nodeRef ),
                                String.class, Object.class );
        } else {
            return super.getProperties();
        }
    }

    public StringBuffer getResponse() {
        return response;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Append onto the response for logging purposes
     * 
     * @param msg
     *            Message to be appened to response TODO: fix logger for
     *            EmsScriptNode
     */
    public void log( String msg ) {
        // if (response != null) {
        // response.append(msg + "\n");
        // }
    }

    /**
     * Genericized function to set property for non-collection types
     * 
     * @param acmType
     *            Property short name for alfresco content model type
     * @param value
     *            Value to set property to
     */
    public < T extends Serializable > void
            setProperty( String acmType, T value ) {
        log( "setProperty(acmType=" + acmType + ", value=" + value + ")" );
        if ( useFoundationalApi ) {
            try {
                services.getNodeService().setProperty( nodeRef,
                                                       createQName( acmType ),
                                                       value );
            } catch ( Exception e ) {
                if (Debug.isOn()) System.out.println( "Got exception in "
                                    + "setProperty(acmType=" + acmType
                                    + ", value=" + value
                                    + ") for EmsScriptNode " + this
                                    + " calling setProperty(nodeRef=" + nodeRef
                                    + ", " + acmType + ", " + value + ")" );
                e.printStackTrace();
            }
        } else {
            getProperties().put( acmType, value );
            save();
        }
    }

    public void setResponse( StringBuffer response ) {
        this.response = response;
    }

    public void setStatus( Status status ) {
        this.status = status;
    }

    /**
     * @return the storeRef
     */
    public static StoreRef getStoreRef() {
        return NodeUtil.getStoreRef();
    }

    /**
     * Gets the SysML qualified name for an object - if not SysML, won't return
     * anything
     * 
     * @return SysML qualified name (e.g., sysml:name qualified)
     */
    public String getSysmlQName() {
        StringBuffer qname = new StringBuffer();

        NodeService nodeService = services.getNodeService();
        Path path = nodeService.getPath( this.getNodeRef() );
        Iterator< Path.Element > pathElements = path.iterator();
        while ( pathElements.hasNext() ) {
            Path.Element pathElement = pathElements.next();
            if ( pathElement instanceof ChildAssocElement ) {
                ChildAssociationRef elementRef =
                        ( (ChildAssocElement)pathElement ).getRef();
                if ( elementRef.getParentRef() != null ) {
                    Serializable nameProp = null;
                    nameProp =
                            nodeService.getProperty( elementRef.getChildRef(),
                                                     QName.createQName( Acm.ACM_NAME,
                                                                        services.getNamespaceService() ) );
                    // add '/' and the name of the child or just the '/' if the
                    // name is missing or unavailable.
                    qname.append( "/" + (nameProp == null ? "" : "" + nameProp));
                }
            }
        }

        return qname.toString();
    }

    /**
     * Get the children views as a JSONArray
     * 
     * @return
     */
    public JSONArray getChildrenViewsJSONArray() {
        JSONArray childrenViews = new JSONArray();
        try {
            Object property = this.getProperty( Acm.ACM_CHILDREN_VIEWS );
            if ( property != null ) {
                childrenViews = new JSONArray( property.toString() );
            }
        } catch ( JSONException e ) {
            e.printStackTrace();
        }

        return childrenViews;
    }

    public String toString() {
//        try {
//            return "" + toJSONObject();
//        } catch ( JSONException e ) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
        String name = getName();
        String sysmlName = getSysmlName();
        String type = getTypeName();
        return "{type=" + type + ", id=" + name + ", name=" + sysmlName + "}";
    }

    /**
     * Convert node into our custom JSONObject with all possible keys
     * @param timestamp 
     * 
     * @return JSONObject serialization of node
     */
    public JSONObject toJSONObject(Date dateTime) throws JSONException {
        return toJSONObject( Acm.JSON_TYPE_FILTER.ALL, dateTime );
    }

    /**
     * Convert node into our custom JSONObject, showing qualifiedName and
     * editable keys
     * 
     * @param renderType
     *            Type of JSONObject to render, this filters what keys are in
     *            JSONObject
     * @return JSONObject serialization of node
     */
    public JSONObject toJSONObject( Acm.JSON_TYPE_FILTER renderType,
                                    Date dateTime )
            throws JSONException {
        return toJSONObject( renderType, true, true, false, dateTime );
    }

    public String nodeRefToSysmlId( NodeRef ref ) throws JSONException {
        EmsScriptNode node = new EmsScriptNode( ref, services );
        Object sysmlId = node.getProperty( Acm.ACM_ID );
        if ( sysmlId != null ) {
            return "" + sysmlId;
        } else {
            Debug.error( true, "elementValue has no sysml id: " + ref );
            return "" + ref.getId();
        }
    }

    public JSONArray nodeRefsToJSONArray( Collection< ? > nodeRefs )
            throws JSONException {
        JSONArray jarr = new JSONArray();
        for ( Object o : nodeRefs ) {
            if ( !( o instanceof NodeRef ) ) {
                jarr.put( "" + o );
                Debug.error( true, "object is not a nodeRef, adding to json: "
                                   + o );
            } else {
                jarr.put( nodeRefToSysmlId( (NodeRef)o ) );
            }
        }
        return jarr;
    }

    // add in all the properties
    protected static TreeSet< String > acmPropNames = new TreeSet<String>(Acm.getACM2JSON().keySet());
//  acmPropNames.add( Acm.ACM_VALUE );
//  acmPropNames.add( Acm.ACM_PROPERTY_TYPE );

    /**
     * Convert node into our custom JSONObject
     * 
     * @param renderType
     *            Type of JSONObject to render, this filters what keys are in
     *            JSONObject
     * @param showQualifiedName
     *            If true, displays qualifiedName key
     * @param showEditable
     *            If true, displays editable key
     * @param isExprOrProp
     * 			  If true, does not add specialization key, as it is nested call to
     * 			  process the Expression operand or Property value
     * @param dateTime
     *            The time of the specialization, specifying the version.  This
     *            should correspond the this EmsScriptNode's version, but that is
     *            not checked.
     * @return JSONObject serialization of node
     */
    public JSONObject toJSONObject( Acm.JSON_TYPE_FILTER renderType,
                                    boolean showQualifiedName,
                                    boolean showEditable, 
                                    boolean isExprOrProp,
                                    Date dateTime ) throws JSONException {
        JSONObject element = new JSONObject();
        JSONObject specializationJSON = new JSONObject();
        
        if ( !exists() ) return element;

        DictionaryService dServ = services.getDictionaryService();
        
        Long readTime = null;

        for ( String acmType : acmPropNames ) {
        	
            if ( Utils.isNullOrEmpty( acmType ) ) continue;
            
            String jsonType = Acm.getACM2JSON().get( acmType );
            if ( Utils.isNullOrEmpty( jsonType ) ) continue;
            
            if (isExprOrProp && Acm.ELEMENT_PROPS_JSON.contains(jsonType)) continue;

            if ( readTime == null ) readTime = System.currentTimeMillis();
            
            Object elementValue = this.getProperty( acmType );
            
            if ( !Acm.JSON_FILTER_MAP.get( renderType ).contains( jsonType ) ) {
                continue;
            }
            
            PropertyDefinition propDef = dServ.getProperty( createQName( acmType ) );
            boolean isNodeRef = ( propDef != null &&
                                  propDef.getDataType().getName() ==
                                  DataTypeDefinition.NODE_REF );
            if ( isView() ) {
                Collection< EmsScriptNode > elements = null;
                if ( jsonType.equals( Acm.JSON_DISPLAYED_ELEMENTS ) ||
                                   jsonType.equals( Acm.JSON_ALLOWED_ELEMENTS ) ) {
                    elements = getView().getDisplayedElements();
                } else if ( jsonType.equals( Acm.JSON_CHILDREN_VIEWS ) ) {
                    Collection< sysml.view.View< EmsScriptNode > > views = 
                            getView().getChildViews();
                    elements = new ArrayList<EmsScriptNode>();
                    for ( sysml.view.View< EmsScriptNode > v : views ) {
                        elements.add( v.getElement() );
                    }
                } else if ( jsonType.equals( Acm.JSON_CONTAINS ) ) {
                    elementValue = getView().getContainsJson();
                }
                if ( !Utils.isNullOrEmpty( elements ) &&
                     ( elementValue == null
                     || ( elementValue instanceof Collection 
                          && ( (Collection< ? >)elementValue ).isEmpty() ) ) ) {
                    ArrayList< NodeRef > refList = new ArrayList<NodeRef>();
                    for ( EmsScriptNode emsNode : elements ) {
                        if ( emsNode != null ) refList.add( emsNode.getNodeRef() );
                    }
                    elementValue = refList;
                }
            }
            
            if ( elementValue == null ) continue;

            boolean isArray = 
                    ( propDef == null ? Acm.JSON_ARRAYS.contains( jsonType )
                                      : propDef.isMultiValued() );
            if ( !isArray ) {
                if ( Acm.JSON_ARRAYS.contains( jsonType )
                     && elementValue instanceof String
                     && ( (String)elementValue ).trim().startsWith( "[" ) ) {
                    elementValue = new JSONArray( (String)elementValue );
                }
                if ( !( elementValue instanceof Collection ) &&
                     !( elementValue instanceof JSONArray ) ) {
                    elementValue = Utils.newList( elementValue );
                }
            } else if ( !( elementValue instanceof Collection ) ) {
                Debug.error( "Property value is not an array as specified by definition! value = "
                             + elementValue );
            }

            isArray = isArray || Acm.JSON_ARRAYS.contains( jsonType );
            

            JSONArray jarr;// = new JSONArray();
            JSONObject updateJson = (Acm.ELEMENT_PROPS_JSON.contains(jsonType) || isExprOrProp) ? element : specializationJSON;

            if ( elementValue instanceof JSONArray ) {
                jarr = (JSONArray)elementValue;
            } else {
                Collection< ? > c = (Collection< ? >)elementValue;
                if ( !isArray && c.size() > 1 ) {
                    Debug.error( "isArray=false for multiple items in elementValue="
                                 + elementValue
                                 + ", jsonType="
                                 + jsonType
                                 + ", this=" + this );
                }
                jarr = new JSONArray();
                for ( Object o : c ) {
                    String s = null;
                    Boolean isString = true;
                    if ( o instanceof NodeRef ) {
                        if ( dateTime != null ) {
                            NodeRef ref = NodeUtil.getNodeRefAtTime( (NodeRef)o, dateTime );
                            if ( ref == null ) {
                                String msg = "Error! Element " + o + " did not exist at " + dateTime + ".\n";
                                if ( getResponse() == null || getStatus() == null ) {
                                    Debug.error( msg );
                                } else {
                                    getResponse().append( msg );
                                    getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST,
                                                         msg );
                                }
                                continue;
                            }
                            o = ref;
                        }
                    	// If it is a operand or value, must get a json object for the noderef:
                    	if (jsonType.equals(Acm.JSON_VALUE) || jsonType.equals(Acm.JSON_OPERAND)) {
                    		isString = false;
                    		EmsScriptNode oNode = new EmsScriptNode((NodeRef)o, services);
                    		o = oNode.toJSONObject(renderType, showQualifiedName, showEditable, true, dateTime);
                    	}
                    	else {
                    		s = nodeRefToSysmlId( (NodeRef)o );
                    	}
                    } else if ( isNodeRef ) {
                        Debug.error( "Property value is not of type NodeRef as specified by definition! value = "
                                     + o );
                    } else if ( o instanceof String ) {
                        s = (String)o;
                    } else if ( o instanceof Date ) {
                        if ( jsonType.equals( Acm.JSON_LAST_MODIFIED ) ) {
                            o = getLastModified( dateTime );
                        }
                        s = getIsoTime( (Date)o );
                    } else {
                        isString = false;
                    }
                    if ( isArray ) {
                        if ( isString ) {
                            jarr.put( s );
                        } else {
                            jarr.put( o );
                        }
                    } else {
                        if ( isString ) {
                        	updateJson.put( jsonType, s );
                        } else {
                        	updateJson.put( jsonType, o );
                        }
                    }
                }
            }
            if ( isArray ) {
            	updateJson.put( jsonType, jarr );
            }
        }
        
        // add in content type:
        if (Acm.JSON_FILTER_MAP.get(renderType).contains(Acm.JSON_TYPE)) {
            String typeName = getTypeName();
        	if (isExprOrProp) {
        		element.put(Acm.JSON_TYPE, typeName );
        	}
        	else {
        		// TODO: figure out why value specs aren't getting here
        		if (typeName != null && typeName.length() > 0) {
        			specializationJSON.put(Acm.JSON_TYPE, typeName );
        		}
        	}
        }
        
        // add in property type(s)
        if (Acm.JSON_FILTER_MAP.get(renderType).contains(Acm.JSON_PROPERTY_TYPE)) {
            JSONArray propertyTypes = getTargetAssocsIdsByType(Acm.ACM_PROPERTY_TYPE);
            if (propertyTypes.length() > 0) {
                element.put(Acm.JSON_PROPERTY_TYPE, propertyTypes.get(0));
            }
        }
        
        if (!isExprOrProp) {
        	
	        // add in specialization:
	        if (Acm.JSON_FILTER_MAP.get(renderType).contains(Acm.JSON_SPECIALIZATION) &&
	        	specializationJSON.length() > 0) {
	            element.put(Acm.JSON_SPECIALIZATION, specializationJSON );
	        }

	        // add in owner
	        if ( Acm.JSON_FILTER_MAP.get( renderType ).contains( Acm.JSON_OWNER ) ) {
	            EmsScriptNode parent = this.getParent();
	            if ( parent != null ) {
	                element.put( Acm.JSON_OWNER,
	                             parent.getName().replace( "_pkg", "" ) );
	            }
	        }
	
	        // add comment
	        if ( Acm.JSON_FILTER_MAP.get( renderType ).contains( Acm.JSON_COMMENT ) ) {
	            JSONArray annotatedElements =
	                    getTargetAssocsIdsByType( Acm.ACM_ANNOTATED_ELEMENTS );
	            if ( annotatedElements.length() > 0 ) {
	                element.put( Acm.JSON_ANNOTATED_ELEMENTS, annotatedElements );
	            }
	        }
	        
	        // show qualified name if toggled
	        if ( showQualifiedName ) {
	            element.put( "qualifiedName", this.getSysmlQName() );
	        }
	
	        // show editable if toggled
	        if ( showEditable ) {
	            element.put( "editable",
	                         this.hasPermission( PermissionService.WRITE ) );
	        }
        }

        // add read time
        if ( !isExprOrProp ) {
            element.put( Acm.JSON_READ, getIsoTime( new Date( readTime ) ) );
        }
        
        String elementString = element.toString();
        elementString = fixArtifactUrls( elementString, true );
        element = new JSONObject( elementString );
        
        return element;
    }

    public boolean isView() {
        boolean isView = hasAspect( Acm.ACM_VIEW ) || hasAspect( Acm.ACM_PRODUCT );
//        String sysmlId = getName();
//        if (Debug.isOn()) System.out.println(sysmlId + ".isView() = " + isView);
        return isView;
    }

    public View getView() {
        if ( view == null ) {
            view = new View(this);
        }
        return view;
    }

    public String getTypeName() {
        String typeName = null;
        
        for ( String aspect : Acm.ACM_ASPECTS ) {
            if ( hasAspect( aspect ) ) {
                // statement below is safe if no ':' since -1 + 1 = 0
                typeName = aspect.substring( aspect.lastIndexOf( ':' ) + 1 );
                if ( !Utils.isNullOrEmpty( typeName ) ) break;
            }
        }
        if ( typeName == null ) {
//          typeName = this.getQNameType().getLocalName();

            String acmType = getTypeShort();
            
            // Return type w/o sysml prefix:
            if (acmType != null) {
                typeName = Acm.getACM2JSON().get(acmType);
            }
        }
        return typeName;
    }

    public JSONArray getTargetAssocsIdsByType( String acmType ) {
        boolean isSource = false;
        return getAssocsIdsByDirection( acmType, isSource );
    }

    public JSONArray getSourceAssocsIdsByType( String acmType ) {
        boolean isSource = true;
        return getAssocsIdsByDirection( acmType, isSource );
    }

    /**
     * Returns a JSONArray of the sysml:ids of the found associations
     * 
     * @param acmType
     * @param isSource
     * @return JSONArray of the sysml:ids found
     */
    protected JSONArray getAssocsIdsByDirection( String acmType,
                                                 boolean isSource ) {
        JSONArray array = new JSONArray();
        List< AssociationRef > assocs;
        if ( isSource ) {
            assocs =
                    services.getNodeService()
                            .getSourceAssocs( nodeRef,
                                              RegexQNamePattern.MATCH_ALL );
        } else {
            assocs =
                    services.getNodeService()
                            .getTargetAssocs( nodeRef,
                                              RegexQNamePattern.MATCH_ALL );
        }
        for ( AssociationRef aref : assocs ) {
            QName typeQName = createQName( acmType );
            if ( aref.getTypeQName().equals( typeQName ) ) {
                NodeRef targetRef;
                if ( isSource ) {
                    targetRef = aref.getSourceRef();
                } else {
                    targetRef = aref.getTargetRef();
                }
                array.put( services.getNodeService()
                                   .getProperty( targetRef,
                                                 createQName( Acm.ACM_ID ) ) );
            }
        }

        return array;
    }

    public List< EmsScriptNode > getTargetAssocsNodesByType( String acmType,
                                                             Date dateTime ) {
        boolean isSource = false;
        return getAssocsNodesByDirection( acmType, isSource, dateTime );
    }

    public List< EmsScriptNode > getSourceAssocsNodesByType( String acmType,
                                                             Date dateTime ) {
        boolean isSource = true;
        return getAssocsNodesByDirection( acmType, isSource, dateTime );
    }

    /**
     * Get a list of EmsScriptNodes of the specified association type
     * 
     * @param acmType
     * @param isSource
     * @param dateTime 
     * @return
     */
    protected List< EmsScriptNode >
            getAssocsNodesByDirection( String acmType, boolean isSource,
                                       Date dateTime ) {
        List< EmsScriptNode > list = new ArrayList< EmsScriptNode >();
        List< AssociationRef > assocs;
        if ( isSource ) {
            assocs =
                    services.getNodeService()
                            .getSourceAssocs( nodeRef,
                                              RegexQNamePattern.MATCH_ALL );
        } else {
            assocs =
                    services.getNodeService()
                            .getTargetAssocs( nodeRef,
                                              RegexQNamePattern.MATCH_ALL );
        }
        for ( AssociationRef aref : assocs ) {
            QName typeQName = createQName( acmType );
            if ( aref.getTypeQName().equals( typeQName ) ) {
                NodeRef targetRef;
                if ( isSource ) {
                    targetRef = aref.getSourceRef();
                } else {
                    targetRef = aref.getTargetRef();
                }
                if ( targetRef == null ) continue;
                if ( dateTime != null ) {
                    targetRef = NodeUtil.getNodeRefAtTime( targetRef, dateTime );
                }
                if ( targetRef == null ) {
                    String msg = "Error! Element " + targetRef + " did not exist at " + dateTime + ".\n";
                    if ( getResponse() == null || getStatus() == null ) {
                        Debug.error( msg );
                    } else {
                        getResponse().append( msg );
                        getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST,
                                             msg );
                    }
                    continue; // TODO -- error?!
                }
                list.add( new EmsScriptNode( targetRef, services, response ) );
            }
        }

        return list;
    }

    /**
     * Given an JSONObject, filters it to find the appropriate relationships to
     * be provided into model post TODO: filterRelationsJSONObject probably
     * doesn't need to be in EmsScriptNode
     * 
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    public static
            JSONObject
            filterRelationsJSONObject( JSONObject jsonObject )
                                                              throws JSONException {
        JSONObject relations = new JSONObject();
        JSONObject elementValues = new JSONObject();
        JSONObject propertyTypes = new JSONObject();
        JSONObject annotatedElements = new JSONObject();
        JSONObject relationshipElements = new JSONObject();
        JSONArray array;

        if ( jsonObject.has( Acm.JSON_VALUE_TYPE ) ) {
            Object object = jsonObject.get( Acm.JSON_VALUE );
            if ( object instanceof String ) {
                array = new JSONArray();
                array.put( object );
            } else {
                array = jsonObject.getJSONArray( Acm.JSON_VALUE );
            }
            if ( jsonObject.get( Acm.JSON_VALUE_TYPE )
                           .equals( Acm.JSON_ELEMENT_VALUE ) ) {
                elementValues.put( jsonObject.getString( Acm.JSON_ID ), array );
            }
        }

        if ( jsonObject.has( Acm.JSON_PROPERTY_TYPE ) ) {
            Object o = jsonObject.get( Acm.JSON_PROPERTY_TYPE );
            String propertyType = "" + o;// jsonObject.getString(Acm.JSON_PROPERTY_TYPE);
            if ( !propertyType.equals( "null" ) ) {
                propertyTypes.put( jsonObject.getString( Acm.JSON_ID ),
                                   propertyType );
            }
        }

        if ( jsonObject.has( Acm.JSON_SOURCE )
             && jsonObject.has( Acm.JSON_TARGET ) ) {
            JSONObject relJson = new JSONObject();
            String source = jsonObject.getString( Acm.JSON_SOURCE );
            String target = jsonObject.getString( Acm.JSON_TARGET );
            relJson.put( Acm.JSON_SOURCE, source );
            relJson.put( Acm.JSON_TARGET, target );
            relationshipElements.put( jsonObject.getString( Acm.JSON_ID ),
                                      relJson );
        } else if ( jsonObject.has( Acm.JSON_ANNOTATED_ELEMENTS ) ) {
            array = jsonObject.getJSONArray( Acm.JSON_ANNOTATED_ELEMENTS );
            annotatedElements.put( jsonObject.getString( Acm.JSON_ID ), array );
        }

        relations.put( "annotatedElements", annotatedElements );
        relations.put( "relationshipElements", relationshipElements );
        relations.put( "propertyTypes", propertyTypes );
        relations.put( "elementValues", elementValues );

        return relations;
    }

    /**
     * Retrieve the site folder containing this node. If this is a view, then it
     * is the folder containing the Models folder. Otherwise, it is the parent
     * folder contained by the Sites folder.
     * 
     * @return the site folder containing this node
     */
    public EmsScriptNode getSiteNode() {
        if ( siteNode != null ) return siteNode;
        EmsScriptNode parent = this;
        String parentName = (String)parent.getProperty( Acm.CM_NAME );
        while ( !parentName.equals( "Models" )
                || !parentName.equals( "ViewEditor" ) ) {
            EmsScriptNode oldparent = parent;
            parent = oldparent.getParent();
            if ( parent == null ) return null; // site not found!
            parentName = (String)parent.getProperty( Acm.CM_NAME );
            if ( parent.getName().toLowerCase().equals( "sites" ) ) {
                siteNode = oldparent;
                return siteNode;
            }
        }
        // The site is the folder containing the Models folder!
        siteNode = parent.getParent();
        return siteNode;
    }

    private EmsScriptNode convertIdToEmsScriptNode( String valueId, Date dateTime ) {
        return convertIdToEmsScriptNode( valueId, dateTime, services, response, status );
    }
    
    public static EmsScriptNode convertIdToEmsScriptNode( String valueId,
                                                          Date dateTime,
												        ServiceRegistry services,
												        StringBuffer response,
												        Status status ) {
        ArrayList<NodeRef> refs = NodeUtil.findNodeRefsByType( valueId, "@cm\\:name:\"", dateTime, true, true, services );
        Set< EmsScriptNode > nodeSet =
        toEmsScriptNodeSet( refs, services, response, status );

        EmsScriptNode value =
                ( nodeSet == null || nodeSet.size() <= 0 ) ? null
                                                          : nodeSet.iterator()
                                                                   .next();

        return value;
    }

    /**
     * Update or create element values (multiple noderefs ordered in a list)
     * 
     * @param jsonArray
     *            Array of the IDs that house the values for the element
     * @param acmProperty
     *            The property to update or create
     * @throws JSONException
     */
    public void createOrUpdateProperties( JSONArray array,
                                          String acmProperty )
                                                     throws JSONException {
        // Need to check if we're trying to stuff an array into a single-valued
        // property. This happens with the contains and other properties of
        // view.
        DictionaryService dServ = services.getDictionaryService();
        PropertyDefinition propDef = dServ.getProperty( createQName( acmProperty ) );
        boolean singleValued =  propDef != null && !propDef.isMultiValued();

        if ( singleValued ) {
            createOrUpdateProperty( acmProperty, array.toString(4) );
            return;
        }

        ArrayList< Serializable > values = getPropertyValuesFromJson( propDef, array, null );

        // special handling for valueType == ElementValue
        if ( values == null ) {
            if ( Acm.ACM_ELEMENT_VALUE.equals( acmProperty ) ) {
                values = getPropertyValuesFromJson( PropertyType.NODE_REF, array, null );
            } else {
                Debug.error(true, false, "*$*$*$ null array of property values for " + acmProperty );
                return;
            }
        }
        if ( values == null ) {
            if (Debug.isOn()) System.out.println("null property values for " + acmProperty );
        }
        
        // only change if old list is different than new
        if ( checkPermissions( PermissionService.WRITE, response, status ) ) {
            @SuppressWarnings( "unchecked" )
            ArrayList< Serializable > oldValues =
                    (ArrayList< Serializable >)getProperty( acmProperty );
            if ( !EmsScriptNode.checkIfListsEquivalent( values, oldValues ) ) {
                setProperty( acmProperty, values );
            }
        } else {
            log( "no write permissions " + id + "\n" );
        }
    }

    public EmsScriptNode findScriptNodeByName( String id, Date dateTime ) {
        return convertIdToEmsScriptNode( id, dateTime, services, response, status );
////        ResultSet existingArtifacts = findNodeRefsByType( id, "@cm\\:name:\"" );
////        Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( existingArtifacts );
//        ArrayList< NodeRef > refs =
//                NodeUtil.findNodeRefsByType( id, "@cm\\:name:\"", dateTime,
//                                             true, true, services );
//        Set< EmsScriptNode > nodeSet =
//                toEmsScriptNodeSet( refs, services, response, status );
//        //        existingArtifacts.close();
//
//        EmsScriptNode value =
//                ( nodeSet == null || nodeSet.size() <= 0 ) ? null
//                                                          : nodeSet.iterator()
//                                                                   .next();
//        return value;
    }

    private enum PropertyType {INT, LONG, DOUBLE, BOOLEAN, TEXT, NODE_REF, UNKNOWN };

    /**
     * Get an ArrayList of property value objects of the proper type according
     * to the property definition.
     * 
     * @param propDef
     *            the property definition
     * @param jsonArray
     *            the array of values in JSON
     * @param jsonKey
     *            the name of the property for
     * @return the list of properties
     * @throws JSONException
     */
    public ArrayList<Serializable> getPropertyValuesFromJson( PropertyDefinition propDef,
                                                              JSONArray jsonArray,
                                                              Date dateTime )
                                                                      throws JSONException {
//        ArrayList<Serializable> properties = new ArrayList<Serializable>();

        if ( propDef == null ) {
            return null;
//          Object o = jsonObject.get( jsonKey );
//          if ( o instanceof Serializable ) return (Serializable)o;
//          return "" + o;
        }
        
        QName name = propDef.getDataType().getName();
        PropertyType type;
        
        if ( name.equals( DataTypeDefinition.INT ) ) {
            type = PropertyType.INT;
        } else if ( name.equals( DataTypeDefinition.LONG ) ) {
            type = PropertyType.LONG;
        } else if ( name.equals( DataTypeDefinition.DOUBLE ) ) {
            type = PropertyType.DOUBLE;
        } else if ( name.equals( DataTypeDefinition.BOOLEAN ) ) {
            type = PropertyType.BOOLEAN;
        } else if ( name.equals( DataTypeDefinition.TEXT ) ) {
            type = PropertyType.TEXT;
        } else if ( name.equals( DataTypeDefinition.NODE_REF ) ) {
            type = PropertyType.NODE_REF;
        } else {
            type = PropertyType.UNKNOWN;
        }
        return getPropertyValuesFromJson( type, jsonArray, dateTime );
    }  

    public ArrayList<Serializable> getPropertyValuesFromJson( PropertyType type,
                                                              JSONArray jsonArray,
                                                              Date dateTime)
                                                                      throws JSONException {
        if (Debug.isOn()) System.out.println( "getPropertyValuesFromJson(" + type + ", "
                            + jsonArray + ", " + dateTime + ")" );

        ArrayList<Serializable> properties = new ArrayList<Serializable>();

        Serializable property = null;
        for ( int i = 0; i < jsonArray.length(); ++i ) {
            switch ( type ) {
                case INT:
                    property = jsonArray.getInt( i );
                    break;
                case LONG:
                    property = jsonArray.getLong( i );
                    break;
                case DOUBLE:
                    property = jsonArray.getDouble( i );
                    break;
                case BOOLEAN:
                    property = jsonArray.getBoolean( i );
                    break;
                case TEXT:
                    try {
                        property = jsonArray.getString( i );
                    } catch ( JSONException e ) {
                        property = "" + jsonArray.get( i );
                    }
                    break;
                case NODE_REF:
                    String sysmlId = jsonArray.getString( i );
                    EmsScriptNode node = convertIdToEmsScriptNode( sysmlId, dateTime );
                    if ( node != null ) {
                        property = node.getNodeRef();
                    } else {
//                        String jsonStr = "{ \"id\" : \"\", " //"\"owner\" : " + + ", " + 
//                                + "\"type\" : \"Element\" }";
//                        JSONObject json = new JSONObject( jsonStr ) ;
//                        updateOrCreateElement(json, null, true);
                        String msg = "Error! No element found for " + sysmlId + ".\n";
                        if ( getResponse() == null || getStatus() == null ) {
                            Debug.error( msg );
                        } else {
                            getResponse().append( msg );
                            getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST,
                                                 msg );
                        }
                        return null;
                    }
                    break;
                case UNKNOWN:
                    property = jsonArray.getString( i );
                    break;
                default:
                    String msg = "Error! Bad property type = " + type + ".\n";
                    if ( getResponse() == null || getStatus() == null ) {
                        Debug.error( msg );
                    } else {
                        getResponse().append( msg );
                        getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST,
                                             msg );
                    }
                    return null;
            };
        
            properties.add( property );
        }
        return properties;
    }

    protected static Serializable badValue = new Serializable() {
        private static final long serialVersionUID = -357325810740259362L;
    };
    
    public Serializable getPropertyValueFromJson( PropertyDefinition propDef,
                                                  JSONObject jsonObject,
                                                  String jsonKey,
                                                  Date dateTime)
                                                          throws JSONException {
        Serializable property = null;
        QName name = null;
        if ( propDef != null ) {
            name = propDef.getDataType().getName();
        } else {
            // skips property type
            return badValue;
//            Debug.error("*$*$*$ null prop def for " + jsonKey );
//            Object o = jsonObject.get( jsonKey );
//            if ( o instanceof Serializable ) return (Serializable)o;
//            return "" + o;
        }
        
        if (name != null) {
	        if ( name.equals( DataTypeDefinition.INT ) ) {
	            property = jsonObject.getInt( jsonKey );
	        } else if ( name.equals( DataTypeDefinition.LONG ) ) {
	            property = jsonObject.getLong( jsonKey );
	        } else if ( name.equals( DataTypeDefinition.DOUBLE ) ) {
	            property = jsonObject.getDouble( jsonKey );
	        } else if ( name.equals( DataTypeDefinition.BOOLEAN ) ) {
	            property = jsonObject.getBoolean( jsonKey );
	        } else if ( name.equals( DataTypeDefinition.TEXT ) ) {
	            property = jsonObject.getString( jsonKey );
	        } else if ( name.equals( DataTypeDefinition.NODE_REF ) ) {
	            String sysmlId = null; 
	            try {
	                sysmlId = jsonObject.getString( jsonKey );
	            } catch ( JSONException e ) {
	                sysmlId = "" + jsonObject.get( jsonKey );
	            }
	            EmsScriptNode node = convertIdToEmsScriptNode( sysmlId, dateTime );
	            property = node.getNodeRef();
	        } else {
	            property = jsonObject.getString( jsonKey );
	        }
        }
        else {
            property = jsonObject.getString( jsonKey );
        }
        
        if ( property == null ) {
            String msg = "Error! Couldn't get property "
                         + propDef + "=" + property + ".\n";
            if ( getResponse() == null || getStatus() == null ) {
                Debug.error( msg );
            } else {
                getResponse().append( msg );
                getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST,
                                     msg );
            }
        }
        return property;
    }
    
    /**
     * Update the node with the properties from the jsonObject
     * 
     * @param jsonObject
     * @throws JSONException
     */
    public void ingestJSON( JSONObject jsonObject ) throws JSONException {
        // fill in all the properties
        if (Debug.isOn()) System.out.println( "ingestJSON(" + jsonObject + ")" );
        
        DictionaryService dServ = services.getDictionaryService();
        
        Iterator<?> iter = jsonObject.keys();
        while ( iter.hasNext() ) {
            String key = "" + iter.next();
            String acmType = Acm.getJSON2ACM().get(key);
            if ( Utils.isNullOrEmpty( acmType ) ) {
                // skips owner
                //Debug.error( "No content model type found for \"" + key + "\"!" );
                continue;
            } else {
            	
                QName qName = createQName( acmType );
                if ( acmType.equals(Acm.ACM_VALUE) ) {
                    if (Debug.isOn()) System.out.println("qName of " + acmType + " = " + qName.toString() );
                }
                
                // If it is a specialization, then process the json object it maps to:
                if ( acmType.equals(Acm.ACM_SPECIALIZATION) ) {
                	
            	   JSONObject specializeJson = jsonObject.getJSONObject(Acm.JSON_SPECIALIZATION);
            	   
                   if (specializeJson != null) {
                	   
                       if (Debug.isOn()) System.out.println("processing " + acmType );
                	   ingestJSON(specializeJson);
                   }
                }
                else {
	                PropertyDefinition propDef =
	                        dServ.getProperty( qName );
	                if ( propDef == null ) {
	                    if (Debug.isOn()) System.out.println("null PropertyDefinition for " + acmType );
	                    continue; // skips type
	                }
	                boolean isArray =
	                        ( Acm.JSON_ARRAYS.contains( key ) || 
	                        ( propDef != null && propDef.isMultiValued() ) );
	                if ( isArray ) {
	                    JSONArray array = jsonObject.getJSONArray( key );
	                    createOrUpdateProperties( array, acmType );
	                } else {
	                    Serializable propVal =
	                            getPropertyValueFromJson( propDef, jsonObject, key, null );
	                    if ( propVal == badValue ) {
	                        Debug.error("Got bad property value!");
	                    } else {
	                        createOrUpdateProperty( acmType, propVal );
	                    }
	                }
                } // ends else (not a Specialization)
            }
        }
    }

    /**
     * Wrapper for replaceArtifactUrl with different patterns if necessary
     * 
     * @param content
     * @param escape
     * @return
     */
    public String fixArtifactUrls( String content, boolean escape ) {
        String result = content;
        result =
                replaceArtifactUrl( result,
                                    "src=\\\\\"/editor/images/docgen/",
                                    "src=\\\\\"/editor/images/docgen/.*?\\\\\"",
                                    escape );

        return result;
    }

    /**
     * Utility method that replaces the image links with references to the
     * repository urls
     * 
     * @param content
     * @param prefix
     * @param pattern
     * @param escape
     * @return
     */
    public String replaceArtifactUrl( String content, String prefix,
                                      String pattern, boolean escape ) {
        if ( content == null ) {
            return content;
        }

        String result = content;
        Pattern p = Pattern.compile( pattern );
        Matcher matcher = p.matcher( content );

        while ( matcher.find() ) {
            String filename = matcher.group( 0 );
            // not sure why this can't be chained correctly
            filename = filename.replace( "\"", "" );
            filename = filename.replace( "_latest", "" );
            filename = filename.replace( "\\", "" );
            filename = filename.replace( "src=/editor/images/docgen/", "" );
            NodeRef nodeRef = findNodeRefByType( filename, "@cm\\:name:\"", null );
            if ( nodeRef != null ) {
                // this should grab whatever is the latest versions purl - so
                // fine for snapshots
                NodeRef versionedNodeRef =
                        services.getVersionService()
                                .getCurrentVersion( nodeRef )
                                .getVersionedNodeRef();
                EmsScriptNode versionedNode =
                        new EmsScriptNode( versionedNodeRef, services, response );
                String nodeurl = "";
                if ( prefix.indexOf( "src" ) >= 0 ) {
                    nodeurl = "src=\\\"";
                }
                // TODO: need to map context out in case we aren't at alfresco
                String context = "/alfresco";
                nodeurl += context + versionedNode.getUrl() + "\\\"";
                // this is service api for getting the content information
                nodeurl =
                        nodeurl.replace( "/d/d/", "/service/api/node/content/" );
                result = result.replace( matcher.group( 0 ), nodeurl );
            }
        }

        return result;
    }
    
    public EmsScriptNode getVersionAtTime( String timestamp ) {
        return getVersionAtTime( TimeUtils.dateFromTimestamp( timestamp ) );
    }
    
    public EmsScriptNode getVersionAtTime( Date dateTime ) {
        NodeRef versionedRef = NodeUtil.getNodeRefAtTime( getNodeRef(), dateTime );
        if (versionedRef == null) {
            return null;
            //return new EmsScriptNode(getNodeRef(), getServices());
        }
        return new EmsScriptNode( versionedRef, getServices() );
    }
    
    protected NodeRef findNodeRefByType( String name, String type, Date dateTime ) {
        return NodeUtil.findNodeRefByType( name, type, dateTime, true, services );
    }

//    protected static ResultSet findNodeRefsByType( String name, String type, ServiceRegistry services ) {
//        return NodeUtil.findNodeRefsByType( name, type, services );
//    }
    
//    // TODO: make this utility function - used in AbstractJavaWebscript too
//    protected ResultSet findNodeRefsByType( String name, String type ) {
//        return NodeUtil.findNodeRefsByType( name, type, services );
//        // ResultSet results = null;
//        // results = services.getSearchService().query(SEARCH_STORE,
//        // SearchService.LANGUAGE_LUCENE, type + name + "\"");
//        // return results;
//    }

    /**
     * Checks whether user has permissions to the node and logs results and
     * status as appropriate
     * 
     * @param permissions
     *            Permissions to check
     * @return true if user has specified permissions to node, false otherwise
     */
    public boolean checkPermissions( String permissions ) {
        return checkPermissions( permissions, null, null );
    }
    
    /**
     * Checks whether user has permissions to the node and logs results and
     * status as appropriate
     * 
     * @param permissions
     *            Permissions to check
     * @param response
     * @param status
     * @return true if user has specified permissions to node, false otherwise
     */
    public boolean checkPermissions( String permissions, StringBuffer response,
                                     Status status ) {
        if ( !hasPermission( permissions ) && response != null ) {
            Object property = getProperty( Acm.CM_NAME );
            if ( property != null ) {
                String msg =
                        "Warning! No " + permissions + " priveleges to "
                                + property.toString() + ".\n";
                response.append( msg );
                if ( status != null ) {
                    status.setCode( HttpServletResponse.SC_BAD_REQUEST, msg );
                }
            }
            return false;
        }
        return true;
    }

    public static class EmsScriptNodeComparator implements
                                               Comparator< EmsScriptNode > {
        @Override
        public int compare( EmsScriptNode x, EmsScriptNode y ) {
            Date xModified;
            Date yModified;

            xModified = x.getLastModified( null );
            yModified = y.getLastModified( null );

            if ( xModified == null ) {
                return -1;
            } else if ( yModified == null ) {
                return 1;
            } else {
                return ( xModified.compareTo( yModified ) );
            }
        }
    }

    public static Date dateFromIsoTime( String timestamp ) {
        return TimeUtils.dateFromTimestamp( timestamp );
    }
    
    public static String getIsoTime( Date date ) {
        return TimeUtils.toTimestamp( date );
//        DateTime dt = new DateTime( date );
//        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
//        return fmt.print( dt );
    }

    /**
     * Override equals for EmsScriptNodes
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {

        if ( obj instanceof EmsScriptNode ) {
            EmsScriptNode that = (EmsScriptNode)obj;
            return this.nodeRef.equals( that.nodeRef );
        } else {
            return false;
        }
    }

    public boolean isFolder() {
        try {
            services.getNodeService().getType( this.getNodeRef() );
        } catch ( Throwable e ) {
            if (Debug.isOn()) System.out.println( "Call to services.getNodeService().getType(nodeRef=" + this.getNodeRef() + ") for this = "
                                + this + " failed!" );
            e.printStackTrace();
        }
        try {
            if ( isSubType( "cm:folder" ) ) return true;
            return false;
        } catch ( Throwable e ) {
            if (Debug.isOn()) System.out.println( "Call to isSubType() on this = "
                                + this + " failed!" );
            e.printStackTrace();
        }
        try {
            QName type = null;
            type = parent.getQNameType();
            if (type != null && !services.getDictionaryService().isSubClass(type, ContentModel.TYPE_FOLDER)) {
                return true;
            }
            return false;
        } catch ( Throwable e ) {
            if (Debug.isOn()) System.out.println( "Trying to call getQNameType() on parent = "
                                + parent + "." );
            e.printStackTrace();
        }
        try {
            String type = getTypeShort();
            if ( type.equals( "folder" ) || type.endsWith( ":folder" ) ) {
                return true;
            }
            return false;
        } catch ( Throwable e ) {
            if (Debug.isOn()) System.out.println( "Trying to call getQNameType() on parent = "
                                + parent + "." );
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public int compare( EmsScriptNode arg0, EmsScriptNode arg1 ) {
        return arg0.getNodeRef().getId().compareTo( arg1.getNodeRef().getId() );
    }

    @Override
    public int compareTo( EmsScriptNode o ) {
        return this.compare( this, o );
    }

    public ServiceRegistry getServices() {
        return services;
    }

    
    /**
     * @return the workspace
     */
    public EmsScriptNode getWorkspace() {
        if ( workspace == null ) {
            if ( hasAspect( "sysml:HasWorkspace" ) ) {
                NodeRef ref = (NodeRef)getProperty( "sysml:workspace" );
                EmsScriptNode ws = new EmsScriptNode( ref, getServices() );
                setWorkspace( ws );
            }
        }
        return workspace;
    }

    /**
     * @return the workspaceId
     */
    public String getWorkspaceName() {
        String workspaceName = null;
        EmsScriptNode ws = getWorkspace();
        if ( ws != null ) {
            ws.getName();
        }
        return workspaceName;
    }

    /**
     * @param workspaceId the workspaceId to set
     */
    public void setWorkspace( EmsScriptNode workspace ) {
        this.workspace = workspace;
    }

    /**
     * @return the parentWorkspaceId
     */
    public EmsScriptNode getParentWorkspace() {
        EmsScriptNode ws = getWorkspace();
        NodeRef ref = (NodeRef)ws.getProperty("sysml:parent");
        EmsScriptNode parentWs = new EmsScriptNode( ref, getServices() );
        return parentWs;
    }


    public EmsScriptNode( NodeRef nodeRef, ServiceRegistry services ) {
        super( nodeRef, services );
    }

    // HERE!!
    public Object getPropertyValue( String propertyName ) {
        Object o = getProperty( propertyName );
        Object value = o; // default if case is not handled below
        
        if ( o instanceof NodeRef ) {
            EmsScriptNode property = new EmsScriptNode((NodeRef)o, getServices());
            if ( property.hasAspect( "Property" ) ) {
                value = property.getProperty("value");
            }
        }
        return value;
    }
    
    public static class NodeByTypeComparator implements Comparator< Object > {
        public static final NodeByTypeComparator instance =
                new NodeByTypeComparator();

        @Override
        public int compare( Object o1, Object o2 ) {
            if ( o1 == o2 ) return 0;
            if ( o1 == null ) return -1;
            if ( o2 == null ) return 1;
            if ( o1.equals( o2 ) ) return 0;
            String type1 = null;
            String type2 = null;
            if ( o1 instanceof String ) {
                type1 = (String)o1;
            } else if ( o1 instanceof NodeRef ) {
                EmsScriptNode n1 = new EmsScriptNode( (NodeRef)o1, null );
                type1 = n1.getTypeName();
            }
            if ( o2 instanceof String ) {
                type2 = (String)o2;
            } else if ( o2 instanceof NodeRef ) {
                EmsScriptNode n2 = new EmsScriptNode( (NodeRef)o2, null );
                type2 = n2.getTypeName();
            }
            int comp = CompareUtils.GenericComparator.instance().compare( type1, type2 );
            if ( comp != 0 ) return comp;
            if ( o1.getClass() != o2.getClass() ) {
                type1 = o1.getClass().getSimpleName();
                type2 = o2.getClass().getSimpleName();
                comp = CompareUtils.GenericComparator.instance().compare( type1, type2 );
                return comp;
            }
            comp = CompareUtils.GenericComparator.instance().compare( o1, o2 );
            return comp;
        }
        
    }
 
    public ArrayList< NodeRef > getPropertyNodeRefs( String acmProperty ) {
        Object o = getProperty(acmProperty);
        ArrayList< NodeRef > refs = null;
        if ( ! ( o instanceof Collection ) ) {
            if ( o instanceof NodeRef ) {
                refs = new ArrayList<NodeRef>();
                refs.add( (NodeRef)o );
            } else {
                return Utils.getEmptyArrayList();
            }
        } else {
            refs = Utils.asList( (Collection<?>)o, NodeRef.class );
        }
        return refs;
    }
    
    public List<EmsScriptNode> getPropertyElements( String acmProperty ) {
        List< NodeRef > refs = getPropertyNodeRefs( acmProperty );
        List<EmsScriptNode> elements = new ArrayList< EmsScriptNode >();
        for ( NodeRef ref : refs ) {
            elements.add( new EmsScriptNode( ref, services ) );
        }
        return elements;
    }

    public Set<EmsScriptNode> getRelationships() {
        Set<EmsScriptNode> set = new LinkedHashSet< EmsScriptNode >();
        for ( Map.Entry< String, String > e :
              Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.entrySet() ) {
            set.addAll( getPropertyElements( e.getValue() ) );
        }
        return set;
    }

    public Set<EmsScriptNode> getRelationshipsOfType(String typeName) {
        Set<EmsScriptNode> set = new LinkedHashSet< EmsScriptNode >();
        for ( Map.Entry< String, String > e :
            Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.entrySet() ) {
            set.addAll( getRelationshipsOfType( typeName, e.getKey() ) );
        }
        return set;
    }
    
    public ArrayList<EmsScriptNode> getRelationshipsOfType(String typeName,
                                                           String acmAspect) {
        if ( !hasAspect( acmAspect ) ) return Utils.getEmptyArrayList();
        String acmProperty = 
                Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.get( acmAspect );

        ArrayList< NodeRef > relationships = getPropertyNodeRefs( acmProperty );
        int index = Collections.binarySearch( relationships, typeName,
                                              //this.getNodeRef(),
                                              NodeByTypeComparator.instance );
        if ( Debug.isOn() ) Debug.outln( "binary search returns index " + index );
        if ( index >= 0 ) {
            Debug.error( true, true, "Index " + index + " for search for "
                                     + typeName + " in " + relationships
                                     + " should be negative!" );
        }
        if ( index < 0 ) {
            // binarySearch returns index = -(insertion point) - 1
            // So, insertion point = -index - 1.
            index = -index - 1;
        }
        ArrayList< EmsScriptNode > matches = new ArrayList< EmsScriptNode >();
        for ( ; index < relationships.size(); ++index ) {
            NodeRef ref = relationships.get( index );
            EmsScriptNode node = new EmsScriptNode( ref, getServices() );
            int comp = typeName.compareTo( node.getTypeName() );
            if ( comp < 0 ) break;
            if ( comp == 0 ) {
                matches.add( node );
            }
        }
        return matches;
    }
    
    /**
     * Add the relationship NodeRef to an assumed array of NodeRefs in the
     * specified property.
     * 
     * @param relationship
     * @param acmProperty
     * @return true if the NodeRef is already in the property or was
     *         successfully added.
     */
    public boolean addRelationshipToProperty( NodeRef relationship,
                                              String acmAspect ) {
        createOrUpdateAspect(acmAspect);
        String acmProperty = Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.get( acmAspect );
        ArrayList< NodeRef > relationships = getPropertyNodeRefs( acmProperty );
        int index = Collections.binarySearch( relationships,
                                              this.getNodeRef(),
                                              NodeByTypeComparator.instance );
        if ( Debug.isOn() ) Debug.outln( "binary search returns index " + index );
        if ( index >= 0 ) {
            // the relationship is already in the list, so nothing to do.
            return true;
        }
        // binarySearch returns index = -(insertion point) - 1
        // So, insertion point = -index - 1.
        index = -index - 1;
        if (Debug.isOn())  Debug.outln( "index converted to lowerbound " + index );
        if ( index < 0 ) {
            Debug.error( true, true, "Error! Expecting an insertion point >= 0 but got " + index + "!" );
            return false;
        } else if ( index >= relationships.size() ) {
            Debug.error( true, true, "Error! Insertion point is beyond the length of the list: point = " + index + ", length = " + relationships.size() );
            return false;
        } else {
            relationships.add( index, relationship );
        }
 
        setProperty( acmProperty, relationships );
        return true;
    }
    
    public void addRelationshipToPropertiesOfParticipants() {
        if ( hasAspect( Acm.ACM_DIRECTED_RELATIONSHIP )
             || hasAspect( Acm.ACM_DEPENDENCY )
             || hasAspect( Acm.ACM_EXPOSE )
             || hasAspect( Acm.ACM_CONFORM )
             || hasAspect( Acm.ACM_GENERALIZATION ) ) {
            NodeRef source = (NodeRef)getProperty( Acm.ACM_SOURCE );
            NodeRef target = (NodeRef)getProperty( Acm.ACM_TARGET );
            
           if ( source != null ) {
               EmsScriptNode sNode = new EmsScriptNode( source, services );
               sNode.addRelationshipToProperty( getNodeRef(), 
                                                Acm.ACM_RELATIONSHIPS_AS_SOURCE );
           }
           if ( target != null ) {
               EmsScriptNode tNode = new EmsScriptNode( target, services );
               tNode.addRelationshipToProperty( getNodeRef(), 
                                                Acm.ACM_RELATIONSHIPS_AS_TARGET );
           }
        }
    }

}
