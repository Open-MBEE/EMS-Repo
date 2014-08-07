/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S.
 * Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Diff;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
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
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;
import org.springframework.extensions.webscripts.Status;

/**
 * Extension of ScriptNode to support EMS needs
 *
 * @author cinyoung
 *
 */
public class EmsScriptNode extends ScriptNode implements
                                             Comparator< EmsScriptNode >,
                                             Comparable< EmsScriptNode > {
    private static final long serialVersionUID = 9132455162871185541L;

    /**
     * A set of content model property names that serve as workspace metadata
     * and whose changes are not recorded in a workspace.
     */
    public static TreeSet< String > workspaceMetaProperties =
            new TreeSet< String >() {
                private static final long serialVersionUID =
                        -327817873667229953L;
                {
                    add( "ems:workspace" );
                    add( "ems:source" );
                    add( "ems:lastTimeSyncParent" );
                    add( "ems:mergeSource" );
                }
            };

    // provide logging capability of what is done
    private StringBuffer response = null;

    // provide status as necessary
    private Status status = null;

    boolean useFoundationalApi = true; // TODO this will be removed

    protected EmsScriptNode companyHome = null;

    protected EmsScriptNode siteNode = null;

    private View view;

    protected WorkspaceNode workspace = null;
    protected WorkspaceNode parentWorkspace = null;

    // TODO add nodeService and other member variables when no longer
    // subclassing ScriptNode
    // extend Serializable after removing ScriptNode extension

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

    public EmsScriptNode childByNamePath( String path, WorkspaceNode workspace ) {
        // Make sure this node is in the target workspace.
        EmsScriptNode node = this;
        if ( workspace != null && !workspace.equals( getWorkspace() ) ) {
            node = findScriptNodeByName( getName(), workspace, null );
        }
        // See if the path/child is in this workspace.
        EmsScriptNode child = node.childByNamePath( path );
        if ( child != null && child.exists() ) {
            return child;
        }
        // Find the path/child in a parent workspace.
        EmsScriptNode source = node.getWorkspaceSource();
        while ( source != null && source.exists()
                && ( child == null || !child.exists() ) ) {
            child = source.childByNamePath( path );
            source = source.getWorkspaceSource();
        }
        if ( child != null && child.exists() ) {
            return child;
        }
        return null;
    }

    public EmsScriptNode getWorkspaceSource() {
        if ( !hasAspect( "ems:HasWorkspace" ) ) return null;
        NodeRef ref = (NodeRef)getProperty( "ems:source" );
        if ( ref != null ) {
            return new EmsScriptNode( ref, getServices() );
        }
        String msg = "Error! Node has HasWorkspace aspect but no source node!";
        log( msg );
        Debug.error( msg );
        return null;
    }

    /**
     * Use {@link #childByNamePath(String, WorkspaceNode)} instead.
     *
     * @see org.alfresco.repo.jscript.ScriptNode#childByNamePath(java.lang.String)
     */
    @Override
    public EmsScriptNode childByNamePath( String path ) {
        ScriptNode child = super.childByNamePath( path );
        if ( child == null || !child.exists() ) {
            return null;
        }
        return new EmsScriptNode( child.getNodeRef(), services, response );
    }

    @Override
    public EmsScriptNode createFile( String name ) {
        return new EmsScriptNode( super.createFile( name ).getNodeRef(),
                                  services, response, status );
    }

    public Set< EmsScriptNode > getChildNodes() {
        Set< EmsScriptNode > set = new LinkedHashSet< EmsScriptNode >();
        List< ChildAssociationRef > refs =
                services.getNodeService().getChildAssocs( nodeRef );
        if ( refs != null ) {
            // check all associations to see if there's a matching association
            for ( ChildAssociationRef ref : refs ) {
                if ( ref.getParentRef().equals( nodeRef ) ) {
                    NodeRef child = ref.getChildRef();
                    EmsScriptNode node =
                            new EmsScriptNode( child, getServices() );
                    set.add( node );
                }
            }
        }
        return set;
    }

    // @Override
    // public Scriptable getChildren() {
    // Scriptable myChildren = super.getChildren();
    // //myChildren.
    // //if ( )
    // }

    @Override
    public EmsScriptNode createFolder( String name ) {
        EmsScriptNode folder =
                new EmsScriptNode( super.createFolder( name ).getNodeRef(),
                                   services, response, status );
        WorkspaceNode ws = getWorkspace();
        EmsScriptNode source = getWorkspaceSource();
        // TODO -- the folder is not getting a source here! is that okay?
        // Scriptable myChildren = source.getChildren();
        if ( ws != null && !folder.isWorkspace() ) folder.setWorkspace( ws,
                                                                        null );
        if ( Debug.isOn() ) {
            Debug.outln( "createFolder(" + name + "): returning " + folder );
        }

        return folder;
    }

    @Override
    public EmsScriptNode createFolder( String name, String type ) {
        EmsScriptNode folder =
                new EmsScriptNode( super.createFolder( name, type )
                                        .getNodeRef(), services, response,
                                   status );
        WorkspaceNode ws = getWorkspace();
        // TODO -- the folder is not getting a source here!
        if ( ws != null && !folder.isWorkspace() ) folder.setWorkspace( ws,
                                                                        null );
        return folder;
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
            aspectName = Acm.getJSON2ACM().get( aspectName );
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

    public List< EmsScriptNode >
            toEmsScriptNodeList( ArrayList< NodeRef > resultSet ) {
        return toEmsScriptNodeList( resultSet, services, response, status );
    }

    public static List< EmsScriptNode >
            toEmsScriptNodeList( ArrayList< NodeRef > resultSet,
                                 // Date dateTime,
                                 ServiceRegistry services,
                                 StringBuffer response, Status status ) {

        ArrayList< EmsScriptNode > emsNodeList =
                new ArrayList< EmsScriptNode >();
        for ( NodeRef ref : resultSet ) {
            // NodeRef ref = row.getNodeRef();
            if ( ref == null ) continue;
            EmsScriptNode node =
                    new EmsScriptNode( ref, services, response, status );
            if ( !node.exists() ) continue;
            emsNodeList.add( node );
        }
        return emsNodeList;
    }

    public EmsScriptNode findOrCreateArtifact( String name, String type,
                                               String base64content,
                                               String targetSiteName,
                                               String subfolderName,
                                               WorkspaceNode workspace,
                                               Date dateTime ) {
        byte[] content =
                ( base64content == null )
                                         ? null
                                         : DatatypeConverter.parseBase64Binary( base64content );
        long cs = getChecksum( content );

        // see if image already exists by looking up by checksum
        ArrayList< NodeRef > refs =
                NodeUtil.findNodeRefsByType( "" + cs,
                                             SearchType.CHECKSUM.prefix, // null,
                                                                         // null,
                                             workspace, dateTime, false, false,
                                             services );
        // ResultSet existingArtifacts =
        // NodeUtil.findNodeRefsByType( "" + cs, SearchType.CHECKSUM,
        // services );
        // Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( existingArtifacts
        // );
        List< EmsScriptNode > nodeList = toEmsScriptNodeList( refs );
        // existingArtifacts.close();

        EmsScriptNode matchingNode = null;

        if ( nodeList != null && nodeList.size() > 0 ) {
            matchingNode = nodeList.iterator().next();
        }

        EmsScriptNode targetSiteNode =
                NodeUtil.getSiteNode( targetSiteName, workspace, dateTime,
                                      services, response );

        if ( matchingNode != null ) return matchingNode;

        // create new artifact

        // find subfolder in site or create it
        String artifactFolderName =
                "Artifacts"
                        + ( Utils.isNullOrEmpty( subfolderName )
                                                                ? ""
                                                                : "/"
                                                                  + subfolderName );
        // find site; it must exist!
        if ( targetSiteNode == null || !targetSiteNode.exists() ) {
            log( "Can't find node for site: " + targetSiteName + "!" );
            return null;
        }
        // find or create subfolder
        EmsScriptNode subfolder =
                NodeUtil.mkdir( targetSiteNode, artifactFolderName, services,
                                response, status );
        if ( subfolder == null || !subfolder.exists() ) {
            log( "Can't create subfolder for site, " + targetSiteName
                 + ", in artifact folder, " + artifactFolderName + "!" );
            return null;
        }

        String artifactId = name + "." + type;
        EmsScriptNode artifactNode =
                subfolder.createNode( artifactId, "cm:content" );
        if ( artifactNode == null || !artifactNode.exists() ) {
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

        if ( Debug.isOn() ) System.out.println( "Creating artifact with indexing: "
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
                                              getSiteName(), "images",
                                              getWorkspace(), null );
                if ( artNode == null || !artNode.exists() ) {
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
            if ( !x.get( ii ).equals( y.get( ii ) ) ) {
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

        if ( node != null ) {
            if ( !type.equals( sysmlAcmType )
                 && NodeUtil.isAspect( sysmlAcmType ) ) {
                node.createOrUpdateAspect( sysmlAcmType );
            }

            // everything is created in a reified package, so need to make
            // relations to the reified node rather than the package
            EmsScriptNode reifiedNode = this.getReifiedNode();
            if ( reifiedNode != null ) {
                // store owner with created node
                node.createOrUpdateAspect( "ems:Owned" );
                node.createOrUpdateProperty( "ems:owner",
                                             reifiedNode.getNodeRef() );

                // add child to the parent as necessary
                reifiedNode.createOrUpdateAspect( "ems:Owned" );
                reifiedNode.appendToPropertyNodeRefs( "ems:ownedChildren",
                                                      node.getNodeRef() );
            } else {
                // TODO error handling
            }
        }
        return node;
    }

    private EmsScriptNode getReifiedNode() {
        NodeRef nodeRef = (NodeRef)getProperty( "ems:reifiedNode" );
        if ( nodeRef != null ) {
            return new EmsScriptNode( nodeRef, services, response );
        }
        return null;
    }

    private EmsScriptNode getReifiedPkg() {
        NodeRef nodeRef = (NodeRef)getProperty( "ems:reifiedPkg" );
        if ( nodeRef != null ) {
            return new EmsScriptNode( nodeRef, services, response );
        }
        return null;
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
            ScriptNode scriptNode = super.createNode( name, type );
            result =
                    new EmsScriptNode( scriptNode.getNodeRef(), services,
                                       response );
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
                    if ( Debug.isOn() ) System.out.println( "Got exception in "
                                                            + "createNode(name="
                                                            + name
                                                            + ", type="
                                                            + type
                                                            + ") for EmsScriptNode("
                                                            + this
                                                            + ") calling createNode(nodeRef="
                                                            + nodeRef
                                                            + ", . . .)" );
                    e.printStackTrace();
                }

            } else {
                log( "Could not find type " + type );
            }
        }

        // Set the workspace to be the same as this one's.
        // WARNING! The parent must already be replicated in the specified
        // workspace.
        if ( result != null ) {
            WorkspaceNode parentWs = getWorkspace();
            if ( parentWs != null && !result.isWorkspace() ) {
                result.setWorkspace( parentWs, null );
            }
        }

        // end = new Date(); if (Debug.isOn())
        // System.out.println("\tcreateNode: " +
        // (end.getTime()-start.getTime()));
        return result;
    }

    public List< Object > getValuesFromScriptable( Scriptable values ) {
        NodeValueConverter nvc = new NodeValueConverter();
        Object res = nvc.convertValueForRepo( (Serializable)values );
        if ( res instanceof List ) {
            return (List< Object >)res;
        }
        return null;
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

    public String getSysmlId() {
        String id = (String)getProperty( Acm.ACM_ID );
        if ( id == null ) {
            id = getName();
        }
        return id;
    }

    @Override
    public EmsScriptNode getParent() {
        ScriptNode myParent = super.getParent();
        if ( myParent == null ) return null;
        return new EmsScriptNode( myParent.getNodeRef(), services, response );
    }

    /**
     * Get the property of the specified type
     *
     * @param acmType
     *            Short name of property to get
     * @return
     */
    public Object getProperty( String acmType ) {
        if ( Utils.isNullOrEmpty( acmType ) ) return null;
        Object result = null;
        if ( useFoundationalApi ) {
            QName typeQName = createQName( acmType );
            result = services.getNodeService().getProperty( nodeRef, typeQName );
        } else {
            result = getProperties().get( acmType );
        }
        // get noderefs from the proper workspace unless the property is a
        // workspace meta-property
        if ( !workspaceMetaProperties.contains( acmType ) ) {
            if ( result instanceof NodeRef ) {
                result =
                        NodeUtil.getNodeRefAtTime( (NodeRef)result,
                                                   getWorkspace(), null );
            } else if ( result instanceof Collection ) {
                Collection< ? > resultColl = (Collection< ? >)result;
                ArrayList< Object > arr = new ArrayList< Object >();
                for ( Object o : resultColl ) {
                    if ( o instanceof NodeRef ) {
                        NodeRef ref =
                                NodeUtil.getNodeRefAtTime( (NodeRef)o,
                                                           getWorkspace(), null );
                        arr.add( ref );
                    } else {
                        arr.add( o );
                    }
                }
                result = arr;
            }
        }
        return result;
    }

    public Date getLastModified( Date dateTime ) {
        Date lastModifiedDate = (Date)getProperty( Acm.ACM_LAST_MODIFIED );

        Object value = getProperty( Acm.ACM_VALUE );
        ArrayList< NodeRef > dependentNodes = new ArrayList< NodeRef >();
        if ( value instanceof Collection ) {
            Collection< ? > c = (Collection< ? >)value;
            dependentNodes.addAll( Utils.asList( c, NodeRef.class ) );
        }
        for ( NodeRef nodeRef : dependentNodes ) {
            nodeRef = NodeUtil.getNodeRefAtTime( nodeRef, dateTime );
            if ( nodeRef == null ) continue;
            EmsScriptNode oNode = new EmsScriptNode( nodeRef, services );
            if ( !oNode.exists() ) continue;
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
                if ( Debug.isOn() ) System.out.println( "Got exception in "
                                                        + "setProperty(acmType="
                                                        + acmType
                                                        + ", value="
                                                        + value
                                                        + ") for EmsScriptNode "
                                                        + this
                                                        + " calling setProperty(nodeRef="
                                                        + nodeRef + ", "
                                                        + acmType + ", "
                                                        + value + ")" );
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

    public String getSysmlQName() {
        return getSysmlQPath( true );
    }

    public String getSysmlQId() {
        return getSysmlQPath( false );
    }

    /**
     * Gets the SysML qualified name for an object - if not SysML, won't return
     * anything
     *
     * @param isName
     *            If true, returns the names, otherwise returns ids
     *
     * @return SysML qualified name (e.g., sysml:name qualified)
     */
    public String getSysmlQPath( boolean isName ) {
        String qname = "";

        if ( isName ) {
            qname = "/" + this.getProperty( "sysml:name" ) + qname;
        } else {
            qname = getDisplayPath() + "/" + getProperty( "sysml:id" );
            int pos = qname.indexOf( "Models/" ) + 7;
            if ( qname.length() >= pos ) {
                qname = qname.substring( pos );
            }
            qname = qname.replace( "_pkg", "" );
        }

        NodeRef ownerRef = (NodeRef)this.getProperty( "ems:owner" );
        // Need to look up based on owners...
        while ( ownerRef != null ) {
            EmsScriptNode owner =
                    new EmsScriptNode( ownerRef, services, response );
            String nameProp = null;
            if ( isName ) {
                nameProp = (String)owner.getProperty( "sysml:name" );
            }
            if ( nameProp == null ) {
                break;
            }
            qname = "/" + nameProp + qname;

            ownerRef = (NodeRef)owner.getProperty( "ems:owner" );
        }

        return qname;
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

    @Override
    public String toString() {
        boolean wasOn = Debug.isOn();
        if ( wasOn ) Debug.turnOff();
        // try {
        // return "" + toJSONObject();
        // } catch ( JSONException e ) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // return null;
        if ( !exists() ) {
            return "NON-EXISTENT-NODE";
        }
        String name = getName();
        String sysmlName = getSysmlName();
        String qualifiedName = getSysmlQName();
        String type = getTypeName();
        String workspaceName = getWorkspaceName();
        String result =
                "{type=" + type + ", id=" + name + ", name=" + sysmlName
                        + ", qualified name=" + qualifiedName + ", workspace="
                        + workspaceName + "}";
        if ( wasOn ) Debug.turnOn();
        return result;
    }

    /**
     * Convert node into our custom JSONObject with all possible keys
     *
     * @param timestamp
     *
     * @return JSONObject serialization of node
     */
    public JSONObject toJSONObject( Date dateTime ) throws JSONException {
        return toJSONObject( null, dateTime );
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
    public
            JSONObject
            toJSONObject( Set< String > filter, Date dateTime )
                                                               throws JSONException {
        return toJSONObject( filter, false, dateTime );
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

    public JSONArray
            nodeRefsToJSONArray( Collection< ? > nodeRefs )
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
    protected static TreeSet< String > acmPropNames =
            new TreeSet< String >( Acm.getACM2JSON().keySet() );

    private EmsScriptNode getNodeAtAtime( Date dateTime ) {
        NodeRef nodeRef = NodeUtil.getNodeRefAtTime( getNodeRef(), dateTime );
        if ( nodeRef != null ) {
            return new EmsScriptNode( nodeRef, services, response );
        }
        // return latest if not found
        return this;
    }

    private void putInJson( JSONObject jsonObject, String key, Object value,
                            Set< String > filter ) throws JSONException {
        if ( filter == null || filter.size() == 0 ) {
            jsonObject.put( key, value );
        } else if ( filter.contains( key ) ) {
            jsonObject.put( key, value );
        }
    }

    protected void
            addElementJSON( JSONObject elementJson, Set< String > filter,
                            Date dateTime ) throws JSONException {
        EmsScriptNode node = getNodeAtAtime( dateTime );
        // mandatory elements put in directly
        elementJson.put( Acm.JSON_ID, node.getProperty( Acm.ACM_ID ) );
        elementJson.put( "creator", node.getProperty( "cm:modifier" ) );
        elementJson.put( "modified",
                         getLastModified( (Date)node.getProperty( "cm:modified" ) ) );

        putInJson( elementJson, Acm.JSON_NAME,
                   node.getProperty( Acm.ACM_NAME ), filter );
        putInJson( elementJson, Acm.JSON_DOCUMENTATION,
                   node.getProperty( Acm.ACM_DOCUMENTATION ), filter );
        putInJson( elementJson, "qualifiedName", node.getSysmlQName(), filter );
        putInJson( elementJson, "qualifiedId", node.getSysmlQId(), filter );
        putInJson( elementJson, "editable",
                   node.hasPermission( PermissionService.WRITE ), filter );
        NodeRef ownerRef = (NodeRef)node.getProperty( "ems:owner" );
        EmsScriptNode owner;
        if ( ownerRef != null ) {
            owner = new EmsScriptNode( ownerRef, services, response );
        } else {
            owner = node.getParent();
        }
        String ownerId = null;
        if ( owner != null ) {
            ownerId = (String)owner.getProperty( "sysml:id" );
            if ( ownerId != null ) {
                ownerId = ownerId.replace( "_pkg", "" );
            }
        }
        if ( ownerId == null ) {
            ownerId = "null";
        }
        putInJson( elementJson, "owner", ownerId, filter );
    }

    private void addSpecializationJSON( JSONObject json, Set< String > filter,
                                        Date dateTime ) throws JSONException {
        String typeName = getTypeName();
        if ( typeName == null ) {
            // TODO: error logging
            return;
        }

        json.put( "type", typeName );
        for ( QName aspectQname : this.aspects ) {
            String cappedAspectName =
                    Utils.capitalize( aspectQname.getLocalName() );
            String methodName = "add" + cappedAspectName + "JSON";
            Method method = null;
            try {
                // make sure that the method signature here matches all the add
                // methods
                method =
                        this.getClass().getDeclaredMethod( methodName,
                                                           JSONObject.class,
                                                           EmsScriptNode.class,
                                                           Set.class,
                                                           Date.class );
            } catch ( NoSuchMethodException | SecurityException e ) {
                // do nothing, method isn't implemented yet
                // System.out.println("Method not yet implemented: " +
                // methodName);
            }

            if ( method != null ) {
                EmsScriptNode node = getNodeAtAtime( dateTime );
                try {
                    method.invoke( this, json, node, filter, dateTime );
                } catch ( IllegalAccessException | IllegalArgumentException
                          | InvocationTargetException e ) {
                    // do nothing, internal server error
                }
            }
        }
    }

    /**
     * Convert node into our custom JSONObject
     *
     * @param filter
     *            Set of keys that should be displayed (plus the mandatory
     *            fields)
     * @param isExprOrProp
     *            If true, does not add specialization key, as it is nested call
     *            to process the Expression operand or Property value
     * @param dateTime
     *            The time of the specialization, specifying the version. This
     *            should correspond the this EmsScriptNode's version, but that
     *            is not checked.
     * @return JSONObject serialization of node
     */
    public JSONObject toJSONObject( Set< String > filter, boolean isExprOrProp,
                                    Date dateTime ) throws JSONException {
        JSONObject element = new JSONObject();
        JSONObject specializationJSON = new JSONObject();

        if ( !exists() ) return element;

        Long readTime = null;

        if ( readTime == null ) readTime = System.currentTimeMillis();
        if ( isExprOrProp ) {
            addSpecializationJSON( element, filter, dateTime );
        } else {
            addElementJSON( element, filter, dateTime );
            addSpecializationJSON( specializationJSON, filter, dateTime );
            if ( specializationJSON.length() > 0 ) {
                element.put( Acm.JSON_SPECIALIZATION, specializationJSON );
            }
        }

        // add read time
        if ( !isExprOrProp ) {
            element.put( Acm.JSON_READ, getIsoTime( new Date( readTime ) ) );
        }

        // fix the artifact urls
        String elementString = element.toString();
        elementString = fixArtifactUrls( elementString, true );
        element = new JSONObject( elementString );

        return element;
    }

    public boolean isView() {
        boolean isView =
                hasAspect( Acm.ACM_VIEW ) || hasAspect( Acm.ACM_PRODUCT );
        return isView;
    }

    public View getView() {
        if ( view == null ) {
            view = new View( this );
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
            // typeName = this.getQNameType().getLocalName();

            String acmType = getTypeShort();

            // Return type w/o sysml prefix:
            if ( acmType != null ) {
                typeName = Acm.getACM2JSON().get( acmType );
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

    public List< EmsScriptNode >
            getTargetAssocsNodesByType( String acmType,
                                        WorkspaceNode workspace, Date dateTime ) {
        boolean isSource = false;
        return getAssocsNodesByDirection( acmType, isSource, workspace,
                                          dateTime );
    }

    public List< EmsScriptNode >
            getSourceAssocsNodesByType( String acmType,
                                        WorkspaceNode workspace, Date dateTime ) {
        boolean isSource = true;
        return getAssocsNodesByDirection( acmType, isSource, workspace,
                                          dateTime );
    }

    /**
     * Get a list of EmsScriptNodes of the specified association type
     *
     * @param acmType
     * @param isSource
     * @param workspace
     * @param dateTime
     * @return
     */
    protected List< EmsScriptNode >
            getAssocsNodesByDirection( String acmType, boolean isSource,
                                       WorkspaceNode workspace, Date dateTime ) {
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
                    targetRef =
                            NodeUtil.getNodeRefAtTime( targetRef, workspace,
                                                       dateTime );
                }
                if ( targetRef == null ) {
                    String msg =
                            "Error! Element " + targetRef
                                    + " did not exist in workspace "
                                    + workspace.getName() + " at " + dateTime
                                    + ".\n";
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

    public boolean isSite() {
        return ( getParent() != null && ( getParent().getName().toLowerCase()
                                                     .equals( "sites" ) || isWorkspaceTop() ) );
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
        String parentName = parent.getName();
        while ( !parentName.equals( "Models" )
                || !parentName.equals( "ViewEditor" ) ) {
            EmsScriptNode oldparent = parent;
            parent = oldparent.getParent();
            if ( parent == null ) return null; // site not found!
            parentName = parent.getName();
            if ( parentName.toLowerCase().equals( "sites" ) ) {
                siteNode = oldparent;
                return siteNode;
            }
        }
        // The site is the folder containing the Models folder!
        siteNode = parent.getParent();
        return siteNode;
    }

    private EmsScriptNode convertIdToEmsScriptNode( String valueId,
                                                    WorkspaceNode workspace,
                                                    Date dateTime ) {
        return convertIdToEmsScriptNode( valueId, workspace, dateTime,
                                         services, response, status );
    }

    public static EmsScriptNode
            convertIdToEmsScriptNode( String valueId, WorkspaceNode workspace,
                                      Date dateTime, ServiceRegistry services,
                                      StringBuffer response, Status status ) {
        ArrayList< NodeRef > refs =
                NodeUtil.findNodeRefsByType( valueId, "@cm\\:name:\"",
                                             workspace, dateTime, true, true,
                                             services );
        List< EmsScriptNode > nodeList =
                toEmsScriptNodeList( refs, services, response, status );

        EmsScriptNode value =
                ( nodeList == null || nodeList.size() <= 0 ) ? null
                                                            : nodeList.get( 0 );

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
    public
            boolean
            createOrUpdateProperties( JSONArray array, String acmProperty )
                                                                           throws JSONException {
        boolean changed = false;
        // Need to check if we're trying to stuff an array into a single-valued
        // property. This happens with the contains and other properties of
        // view.
        DictionaryService dServ = services.getDictionaryService();
        PropertyDefinition propDef =
                dServ.getProperty( createQName( acmProperty ) );
        boolean singleValued = propDef != null && !propDef.isMultiValued();

        if ( singleValued ) {
            return createOrUpdateProperty( acmProperty, array.toString( 4 ) );
        }

        ArrayList< Serializable > values =
                getPropertyValuesFromJson( propDef, array, getWorkspace(), null );

        // special handling for valueType == ElementValue
        if ( values == null ) {
            if ( Acm.ACM_ELEMENT_VALUE.equals( acmProperty ) ) {
                values =
                        getPropertyValuesFromJson( PropertyType.NODE_REF,
                                                   array, getWorkspace(), null );
            } else {
                Debug.error( true, false,
                             "*$*$*$ null array of property values for "
                                     + acmProperty );
                return changed;
            }
        }
        if ( values == null ) {
            if ( Debug.isOn() ) System.out.println( "null property values for "
                                                    + acmProperty );
        }

        // only change if old list is different than new
        if ( checkPermissions( PermissionService.WRITE, response, status ) ) {
            @SuppressWarnings( "unchecked" )
            ArrayList< Serializable > oldValues =
                    (ArrayList< Serializable >)getProperty( acmProperty );
            if ( !EmsScriptNode.checkIfListsEquivalent( values, oldValues ) ) {
                setProperty( acmProperty, values );
                changed = true;
            }
        } else {
            log( "no write permissions " + id + "\n" );
        }

        return changed;
    }

    public EmsScriptNode findScriptNodeByName( String id,
                                               WorkspaceNode workspace,
                                               Date dateTime ) {
        return convertIdToEmsScriptNode( id, workspace, dateTime, services,
                                         response, status );
    }

    private enum PropertyType {
        INT, LONG, DOUBLE, BOOLEAN, TEXT, DATE, NODE_REF, UNKNOWN
    };

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
    public
            ArrayList< Serializable >
            getPropertyValuesFromJson( PropertyDefinition propDef,
                                       JSONArray jsonArray,
                                       WorkspaceNode workspace, Date dateTime )
                                                                               throws JSONException {
        // ArrayList<Serializable> properties = new ArrayList<Serializable>();

        if ( propDef == null ) {
            return null;
            // Object o = jsonObject.get( jsonKey );
            // if ( o instanceof Serializable ) return (Serializable)o;
            // return "" + o;
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
            // properties of type date include timestamp and
            // creation/modified dates and are not stored by MMS
            // } else if ( name.equals( DataTypeDefinition.DATE ) ) {
            // type = PropertyType.DATE;
            // } else if ( name.equals( DataTypeDefinition.DATETIME ) ) {
            // type = PropertyType.DATE;
        } else if ( name.equals( DataTypeDefinition.NODE_REF ) ) {
            type = PropertyType.NODE_REF;
        } else {
            type = PropertyType.UNKNOWN;
        }
        return getPropertyValuesFromJson( type, jsonArray, workspace, dateTime );
    }

    public
            ArrayList< Serializable >
            getPropertyValuesFromJson( PropertyType type, JSONArray jsonArray,
                                       WorkspaceNode workspace, Date dateTime )
                                                                               throws JSONException {
        if ( Debug.isOn() ) System.out.println( "getPropertyValuesFromJson("
                                                + type + ", " + jsonArray
                                                + ", " + dateTime + ")" );

        ArrayList< Serializable > properties = new ArrayList< Serializable >();

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
                case DATE:
                    try {
                        property = jsonArray.getString( i );
                    } catch ( JSONException e ) {
                        property = "" + jsonArray.get( i );
                    }
                    break;
                case NODE_REF:
                    String sysmlId = jsonArray.getString( i );
                    EmsScriptNode node =
                            convertIdToEmsScriptNode( sysmlId, workspace,
                                                      dateTime );
                    if ( node != null ) {
                        property = node.getNodeRef();
                    } else {
                        // String jsonStr = "{ \"id\" : \"\", " //"\"owner\" : "
                        // + + ", " +
                        // + "\"type\" : \"Element\" }";
                        // JSONObject json = new JSONObject( jsonStr ) ;
                        // updateOrCreateElement(json, null, true);
                        String msg =
                                "Error! No element found for " + sysmlId
                                        + ".\n";
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

    public
            Serializable
            getPropertyValueFromJson( PropertyDefinition propDef,
                                      JSONObject jsonObject, String jsonKey,
                                      WorkspaceNode workspace, Date dateTime )
                                                                              throws JSONException {
        Serializable property = null;
        QName name = null;
        if ( propDef != null ) {
            name = propDef.getDataType().getName();
        } else {
            // skips property type
            return badValue;
            // Debug.error("*$*$*$ null prop def for " + jsonKey );
            // Object o = jsonObject.get( jsonKey );
            // if ( o instanceof Serializable ) return (Serializable)o;
            // return "" + o;
        }

        if ( name != null ) {
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
                // properties of type date include timestamp and
                // creation/modified dates and are not stored by MMS
                // } else if ( name.equals( DataTypeDefinition.DATE ) ) {
                // property = jsonObject.getString( jsonKey );
                // } else if ( name.equals( DataTypeDefinition.DATETIME ) ) {
                // property = jsonObject.getString( jsonKey );
            } else if ( name.equals( DataTypeDefinition.NODE_REF ) ) {
                String sysmlId = null;
                try {
                    sysmlId = jsonObject.getString( jsonKey );
                } catch ( JSONException e ) {
                    sysmlId = "" + jsonObject.get( jsonKey );
                }
                EmsScriptNode node =
                        convertIdToEmsScriptNode( sysmlId, workspace, dateTime );
                if ( node != null ) {
                    property = node.getNodeRef();
                } else if ( !Utils.isNullOrEmpty( sysmlId ) ) {
                    String msg =
                            "Error! Could not find element for sysml id = "
                                    + sysmlId + ".\n";
                    if ( getResponse() == null || getStatus() == null ) {
                        Debug.error( msg );
                    } else {
                        getResponse().append( msg );
                        getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST,
                                             msg );
                    }
                }
            } else {
                property = jsonObject.getString( jsonKey );
            }
        } else {
            property = jsonObject.getString( jsonKey );
        }

        if ( property == null ) {
            String msg =
                    "Error! Couldn't get property " + propDef + "=" + property
                            + ".\n";
            if ( getResponse() == null || getStatus() == null ) {
                Debug.error( msg );
            } else {
                getResponse().append( msg );
                getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST, msg );
            }
        }
        return property;
    }

    /**
     * Update the node with the properties from the jsonObject
     *
     * @param jsonObject
     *
     *            return true if Element was changed, false otherwise
     * @throws JSONException
     */
    public boolean ingestJSON( JSONObject jsonObject ) throws JSONException {
        boolean changed = false;
        // fill in all the properties
        if ( Debug.isOn() ) System.out.println( "ingestJSON(" + jsonObject
                                                + ")" );

        DictionaryService dServ = services.getDictionaryService();

        Iterator< ? > iter = jsonObject.keys();
        while ( iter.hasNext() ) {
            String key = "" + iter.next();
            String acmType = Acm.getJSON2ACM().get( key );
            if ( Utils.isNullOrEmpty( acmType ) ) {
                // skips owner
                // Debug.error( "No content model type found for \"" + key +
                // "\"!" );
                continue;
            } else {
                QName qName = createQName( acmType );
                if ( acmType.equals( Acm.ACM_VALUE ) ) {
                    if ( Debug.isOn() ) System.out.println( "qName of "
                                                            + acmType + " = "
                                                            + qName.toString() );
                }

                // If it is a specialization, then process the json object it
                // maps to:
                if ( acmType.equals( Acm.ACM_SPECIALIZATION ) ) {

                    JSONObject specializeJson =
                            jsonObject.getJSONObject( Acm.JSON_SPECIALIZATION );

                    if ( specializeJson != null ) {
                        if ( Debug.isOn() ) System.out.println( "processing "
                                                                + acmType );
                        if ( ingestJSON( specializeJson ) ) {
                            changed = true;
                        }
                    }
                } else {
                    PropertyDefinition propDef = dServ.getProperty( qName );
                    if ( propDef == null ) {
                        if ( Debug.isOn() ) System.out.println( "null PropertyDefinition for "
                                                                + acmType );
                        continue; // skips type
                    }
                    boolean isArray =
                            ( Acm.JSON_ARRAYS.contains( key ) || ( propDef != null && propDef.isMultiValued() ) );
                    if ( isArray ) {
                        JSONArray array = jsonObject.getJSONArray( key );
                        if ( createOrUpdateProperties( array, acmType ) ) {
                            changed = true;
                        }
                    } else {
                        // REVIEW -- Passing null for workspace; this assumes
                        // that the
                        // workspace of any NodeRefs referenced by the property
                        // are fixed
                        // by the caller.
                        Serializable propVal =
                                getPropertyValueFromJson( propDef, jsonObject,
                                                          key, getWorkspace(),
                                                          null );
                        if ( propVal == badValue ) {
                            Debug.error( "Got bad property value!" );
                        } else {
                            if ( createOrUpdateProperty( acmType, propVal ) ) {
                                changed = true;
                            }
                        }
                    }
                } // ends else (not a Specialization)
            }
        }

        return changed;
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
            NodeRef nodeRef =
                    findNodeRefByType( filename, "@cm\\:name:\"",
                                       getWorkspace(), null );
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
        NodeRef versionedRef =
                NodeUtil.getNodeRefAtTime( getNodeRef(), dateTime );
        if ( versionedRef == null ) {
            return null;
            // return new EmsScriptNode(getNodeRef(), getServices());
        }
        return new EmsScriptNode( versionedRef, getServices() );
    }

    protected NodeRef
            findNodeRefByType( String name, String type,
                               WorkspaceNode workspace, Date dateTime ) {
        return NodeUtil.findNodeRefByType( name, type, workspace, dateTime,
                                           true, services );
    }

    // protected static ResultSet findNodeRefsByType( String name, String type,
    // ServiceRegistry services ) {
    // return NodeUtil.findNodeRefsByType( name, type, services );
    // }

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

    /**
     * Override exists for EmsScriptNodes
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean exists() {
        if ( !super.exists() ) return false;
        if ( hasAspect( "ems:Deleted" ) ) {
            return false;
        }
        return true;
    }


    public boolean isFolder() {
        try {
            services.getNodeService().getType( this.getNodeRef() );
        } catch ( Throwable e ) {
            if ( Debug.isOn() ) System.out.println( "Call to services.getNodeService().getType(nodeRef="
                                                    + this.getNodeRef()
                                                    + ") for this = "
                                                    + this
                                                    + " failed!" );
            e.printStackTrace();
        }
        try {
            if ( isSubType( "cm:folder" ) ) return true;
            return false;
        } catch ( Throwable e ) {
            if ( Debug.isOn() ) System.out.println( "Call to isSubType() on this = "
                                                    + this + " failed!" );
            e.printStackTrace();
        }
        try {
            QName type = null;
            type = parent.getQNameType();
            if ( type != null
                 && !services.getDictionaryService()
                             .isSubClass( type, ContentModel.TYPE_FOLDER ) ) {
                return true;
            }
            return false;
        } catch ( Throwable e ) {
            if ( Debug.isOn() ) System.out.println( "Trying to call getQNameType() on parent = "
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
            if ( Debug.isOn() ) System.out.println( "Trying to call getQNameType() on parent = "
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

    public WorkspaceNode getWorkspace() {
        if ( workspace == null ) {
            if ( hasAspect( "ems:HasWorkspace" ) ) {
                NodeRef ref = (NodeRef)getProperty( "ems:workspace" );
                WorkspaceNode ws = new WorkspaceNode( ref, getServices() );
                setWorkspace( ws, null );
            }
        }
        return workspace;
    }

    /**
     * @return the sysml id (cm name) of the workspace
     */
    public String getWorkspaceName() {
        String workspaceName = null;
        EmsScriptNode ws = getWorkspace();
        if ( ws != null ) {
            workspaceName = ws.getName();
        }
        return workspaceName;
    }

    /**
     * @param workspace
     *            the workspace to set
     */
    public void setWorkspace( WorkspaceNode workspace, NodeRef source ) {
        if ( Debug.isOn() ) {
            Debug.outln( "setWorkspace( workspace=" + workspace + ", source="
                         + source + " ) for node " + this );
        }

        this.workspace = workspace;
        createOrUpdateAspect( "ems:HasWorkspace" );
        NodeRef ref = (NodeRef)getProperty( "ems:workspace" );
        if ( workspace != null && !workspace.getNodeRef().equals( ref ) ) {
            setProperty( "ems:workspace", workspace.getNodeRef() );
        } else if ( workspace == null && ref != null ) {
            removeAspect( "ems:HasWorkspace" );
        }
        if ( source != null ) {
            setProperty( "ems:source", source );
        }
    }

    /**
     * @return the parentWorkspace
     */
    public WorkspaceNode getParentWorkspace() {
        WorkspaceNode ws = getWorkspace();
        return ws.getParentWorkspace();
    }

    public EmsScriptNode( NodeRef nodeRef, ServiceRegistry services ) {
        super( nodeRef, services );
    }

    public EmsScriptNode clone( EmsScriptNode parent ) {
        if ( !exists() ) {
            Debug.error( true, false, "Warning! cloning non-existent node!" );
        }
        if ( Debug.isOn() ) {
            Debug.outln( "making clone() of " + this + " under parent "
                         + parent );
        }
        if ( parent == null || !parent.exists() ) {
            Debug.error( "Error! Trying to clone a node under a bad parent: "
                         + parent + "; changing parent to node being cloned "
                         + this );
            parent = this;
            if ( !exists() ) {
                Debug.error( "Error! Can't clone under non-existent parent!" );
            }
        }

        // create node of same type, except a site will be of type cm:folder.
        String type = getTypeShort();
        boolean isSiteOrSites = type.startsWith( "st:site" );
        if ( isSiteOrSites ) {
            type = "cm:folder";
        }
        EmsScriptNode node = parent.createNode( getName(), type );

        // add missing aspects
        NodeService nodeService = getServices().getNodeService();
        Set< QName > myAspects = nodeService.getAspects( getNodeRef() );
        for ( QName qName : myAspects ) {
            node.createOrUpdateAspect( qName.toString() );
        }

        // copy properties except those of a site.
        Map< QName, Serializable > properties =
                nodeService.getProperties( getNodeRef() );
        if ( isSiteOrSites ) {
            properties.remove( createQName( "st:sitePreset" ) );
        }
        nodeService.setProperties( node.getNodeRef(), properties );
        return node;
    }

    public Map< String, Pair< Object, Object > > diff( EmsScriptNode source ) {
        if ( !NodeUtil.exists( source ) ) return null;
        Diff nodeDiff = new NodeDiff( source.getNodeRef(), getNodeRef() );
        return nodeDiff.getPropertyChanges();
    }

    public NodeDiff getNodeDiff( EmsScriptNode source ) {
        return getNodeDiff( source, null, null );
    }

    public NodeDiff getNodeDiff( EmsScriptNode source, Boolean lazy,
                                 Boolean ignoreRemovedProperties ) {
        if ( !NodeUtil.exists( source ) ) return null;
        NodeDiff nodeDiff =
                new NodeDiff( source.getNodeRef(), getNodeRef(),
                              ignoreRemovedProperties );;
        return nodeDiff;
    }

    /**
     * Merge the input node into this one. This adds and updates most aspects,
     * properties, and property values of the input node to this one. It does
     * not remove aspects or properties not found for the input node. This
     * ignores workspace metadata (e.g., does not change workspaces or add the
     * HasWorkspace aspect).
     *
     * @param node
     *            the node whose properties are being merged
     * @return true if and only if any changes are made
     */
    public boolean merge( EmsScriptNode node ) {
        boolean changed = false;
        NodeDiff diff = getNodeDiff( node, true, true );
        return merge( diff );
    }

    /**
     * Merge the differences in the input NodeDiff into this node even though
     * this node may not be one of the nodes use to create the NodeDiff. Added
     * properties are treated as updates if the properties already exist in this
     * node. This ignores workspace metadata (e.g., does not change workspaces
     * or add the HasWorkspace aspect).
     *
     * @param diff
     *            the NodeDiff to apply to this node
     * @return true if and only if any changes are made
     */
    public boolean merge( NodeDiff diff ) {
        boolean changed = false;

        for ( String aspect : diff.getRemovedAspects() ) {
            NodeService ns = getServices().getNodeService();
            if ( hasAspect( aspect ) ) {
                try {
                    ns.removeAspect( getNodeRef(), createQName( aspect ) );
                    changed = true;
                } catch ( Throwable e ) {
                    // ignore
                }
            }
        }

        for ( String aspect : diff.getAddedAspects() ) {
            NodeService ns = getServices().getNodeService();
            if ( !hasAspect( aspect ) ) {
                try {
                    createOrUpdateAspect( aspect );
                    changed = true;
                } catch ( Throwable e ) {
                    // ignore
                }
            }
        }

        Map< String, Object > removedProps =
                diff.getRemovedProperties().get(getName());
        if ( removedProps != null )
        for ( Entry< String, Object > e : removedProps.entrySet() ) {
            if ( workspaceMetaProperties.contains( e.getKey() ) ) continue;
            Object myVal = getProperty( e.getKey() );
            if ( myVal != null ) {
                if ( removeProperty( e.getKey() ) ) changed = true;
            }
        }
        Map< String,  Pair< Object, Object > > propChanges =
                diff.getPropertyChanges().get(getName());
        if ( propChanges != null )
        for ( Entry< String, Pair< Object, Object > > e : propChanges.entrySet() ) {
            if ( workspaceMetaProperties.contains( e.getKey() ) ) continue;
            Object newVal = e.getValue().second;
            if ( newVal == null ) continue;
            Object myVal = getProperty( e.getKey() );
            if ( newVal.equals( myVal ) ) continue;
            if ( newVal instanceof Serializable ) {
                Serializable sVal = (Serializable)newVal;
                if ( createOrUpdateProperty( e.getKey(), sVal  ) ) changed = true;
            } else {
                Debug.error("Merging bad property value! " + e.getValue() );
            }
        }
        return changed;
    }

    /**
     * Remove the property with the given name.
     *
     * @param acmProperty
     *            the name of the property in short format (e.g. sysml:value)
     * @return true if and only if the property was successfully removed
     */
    public boolean removeProperty( String acmProperty ) {
        NodeService ns = getServices().getNodeService();
        try {
            ns.removeProperty( getNodeRef(), createQName( acmProperty ) );
            return true;
        } catch ( InvalidNodeRefException e ) {
            // ignore
        }
        return false;
    }

    public void appendToPropertyNodeRefs( String acmProperty, NodeRef ref ) {
        if ( checkPermissions( PermissionService.WRITE, response, status ) ) {
            ArrayList< NodeRef > relationships =
                    getPropertyNodeRefs( acmProperty );
            if ( Utils.isNullOrEmpty( relationships ) ) {
                relationships = Utils.newList( ref );
            } else {
                relationships.add( ref );
            }
            setProperty( acmProperty, relationships );
        } else {
            log( "no write permissions to append " + acmProperty + " to " + id
                 + "\n" );
        }
    }

    private void removeFromPropertyNodeRefs( String acmProperty, NodeRef ref ) {
        if ( checkPermissions( PermissionService.WRITE, response, status ) ) {
            ArrayList< NodeRef > relationships =
                    getPropertyNodeRefs( acmProperty );
            if ( Utils.isNullOrEmpty( relationships ) ) {
                relationships = Utils.newList( ref );
            } else {
                relationships.remove( ref );
            }
            setProperty( acmProperty, relationships );
        } else {
            log( "no write permissions to remove " + acmProperty + " from "
                 + id + "\n" );
        }
    }

    @Override
    public boolean move( ScriptNode destination ) {
        if ( getParent().equals( destination ) ) {
            return false;
        }

        EmsScriptNode oldParentPkg =
                new EmsScriptNode( getParent().getNodeRef(), services, response );
        boolean status = super.move( destination );

        if ( status ) {
            // keep track of owners and children
            EmsScriptNode oldParentReifiedNode = oldParentPkg.getReifiedNode();

            oldParentReifiedNode.removeFromPropertyNodeRefs( "ems:ownedChildren",
                                                             this.getNodeRef() );

            EmsScriptNode newParent =
                    new EmsScriptNode( destination.getNodeRef(), services,
                                       response );
            EmsScriptNode newReifiedNode = newParent.getReifiedNode();
            newReifiedNode.appendToPropertyNodeRefs( "ems:ownedChildren",
                                                     this.getNodeRef() );

            this.createOrUpdateProperty( "ems:owner",
                                         newReifiedNode.getNodeRef() );

            // make sure to move package as well
            EmsScriptNode reifiedPkg = getReifiedPkg();
            if ( reifiedPkg != null ) {
                reifiedPkg.move( destination );
            }
        }

        return status;
    }

    // HERE!! REVIEW -- Is this right?
    public Object getPropertyValue( String propertyName ) {
        // Debug.error("ERROR!  EmsScriptNode.getPropertyValue() doesn't work!");
        Object o = getProperty( propertyName );
        Object value = o; // default if case is not handled below

        if ( o instanceof NodeRef ) {
            EmsScriptNode property =
                    new EmsScriptNode( (NodeRef)o, getServices() );
            if ( property.hasAspect( "Property" ) ) {
                value = property.getProperty( "value" );
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
                EmsScriptNode n1 =
                        new EmsScriptNode( (NodeRef)o1, NodeUtil.getServices() );
                type1 = n1.getTypeName();
            }
            if ( o2 instanceof String ) {
                type2 = (String)o2;
            } else if ( o2 instanceof NodeRef ) {
                EmsScriptNode n2 =
                        new EmsScriptNode( (NodeRef)o2, NodeUtil.getServices() );
                type2 = n2.getTypeName();
            }
            int comp =
                    CompareUtils.GenericComparator.instance().compare( type1,
                                                                       type2 );
            if ( comp != 0 ) return comp;
            if ( o1.getClass() != o2.getClass() ) {
                if ( o1.getClass().equals( String.class ) ) {
                    return -1;
                }
                if ( o2.getClass().equals( String.class ) ) {
                    return 1;
                }
                type1 = o1.getClass().getSimpleName();
                type2 = o2.getClass().getSimpleName();
                comp =
                        CompareUtils.GenericComparator.instance()
                                                      .compare( type1, type2 );
                return comp;
            }
            comp = CompareUtils.GenericComparator.instance().compare( o1, o2 );
            return comp;
        }

    }

    public ArrayList< NodeRef > getPropertyNodeRefs( String acmProperty ) {
        Object o = getProperty( acmProperty );
        ArrayList< NodeRef > refs = null;
        if ( !( o instanceof Collection ) ) {
            if ( o instanceof NodeRef ) {
                refs = new ArrayList< NodeRef >();
                refs.add( (NodeRef)o );
            } else {
                // return Utils.getEmptyArrayList(); // FIXME The array returned
                // here is later modified, and a common empty array is
                // is used. This makes it non-empty.
                return new ArrayList< NodeRef >();

            }
        } else {
            refs = Utils.asList( (Collection< ? >)o, NodeRef.class );
        }
        return refs;
    }

    public List< EmsScriptNode > getPropertyElements( String acmProperty ) {
        List< NodeRef > refs = getPropertyNodeRefs( acmProperty );
        List< EmsScriptNode > elements = new ArrayList< EmsScriptNode >();
        for ( NodeRef ref : refs ) {
            elements.add( new EmsScriptNode( ref, services ) );
        }
        return elements;
    }

    public Set< EmsScriptNode > getRelationships() {
        Set< EmsScriptNode > set = new LinkedHashSet< EmsScriptNode >();
        for ( Map.Entry< String, String > e : Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.entrySet() ) {
            set.addAll( getPropertyElements( e.getValue() ) );
        }
        return set;
    }

    public Set< EmsScriptNode > getRelationshipsOfType( String typeName ) {
        Set< EmsScriptNode > set = new LinkedHashSet< EmsScriptNode >();
        for ( Map.Entry< String, String > e : Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.entrySet() ) {
            ArrayList< EmsScriptNode > relationships =
                    getRelationshipsOfType( typeName, e.getKey() );
            if ( !Utils.isNullOrEmpty( relationships ) ) {
                set.addAll( relationships );
            }
        }
        return set;
    }

    public ArrayList< EmsScriptNode > getRelationshipsOfType( String typeName,
                                                              String acmAspect ) {
        if ( !hasAspect( acmAspect ) ) return new ArrayList< EmsScriptNode >();
        String acmProperty =
                Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.get( acmAspect );

        ArrayList< NodeRef > relationships = getPropertyNodeRefs( acmProperty );
        // Searching for the beginning of the relationships with this typeName
        // b/c relationships are ordered by typeName. Therefore, the search
        // is expected to not find a matching element.
        int index = Collections.binarySearch( relationships, typeName,
        // this.getNodeRef(),
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
        createOrUpdateAspect( acmAspect );
        String acmProperty =
                Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.get( acmAspect );
        ArrayList< NodeRef > relationships = getPropertyNodeRefs( acmProperty );
        int index =
                Collections.binarySearch( relationships,
                                          // this.getNodeRef(),
                                          relationship,
                                          NodeByTypeComparator.instance );
        if ( Debug.isOn() ) Debug.outln( "binary search returns index " + index );
        if ( index >= 0 ) {
            // the relationship is already in the list, so nothing to do.
            return true;
        }
        // binarySearch returns index = -(insertion point) - 1
        // So, insertion point = -index - 1.
        index = -index - 1;
        if ( Debug.isOn() ) Debug.outln( "index converted to lowerbound "
                                         + index );
        if ( index < 0 ) {
            Debug.error( true, true,
                         "Error! Expecting an insertion point >= 0 but got "
                                 + index + "!" );
            return false;
        } else if ( index > relationships.size() ) {
            Debug.error( true, true,
                         "Error! Insertion point is beyond the length of the list: point = "
                                 + index + ", length = " + relationships.size() );
            return false;
        } else {
            relationships.add( index, relationship );
        }

        setProperty( acmProperty, relationships );
        return true;
    }

    public void addRelationshipToPropertiesOfParticipants() {
        if ( hasAspect( Acm.ACM_DIRECTED_RELATIONSHIP )
             || hasAspect( Acm.ACM_DEPENDENCY ) || hasAspect( Acm.ACM_EXPOSE )
             || hasAspect( Acm.ACM_CONFORM )
             || hasAspect( Acm.ACM_GENERALIZATION ) ) {

            // NOTE -- This code assumes that the source and target are from the
            // appropriate workspace!
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

    public boolean isWorkspace() {
        return hasAspect( "ems:Workspace" );
    }

    public boolean isWorkspaceTop() {
        EmsScriptNode myParent = getParent();
        if ( myParent == null ) {
            if ( Debug.isOn() ) {
                Debug.outln( "isWorkspaceTop() = true for node with null parent: "
                             + this );
            }
            return true;
        }
        if ( myParent.isWorkspace() ) {
            if ( Debug.isOn() ) {
                Debug.outln( "isWorkspaceTop() = true for since node is a workspace: "
                             + this );
            }
            return true;
        }
        if ( equals( getCompanyHome() ) ) {
            if ( Debug.isOn() ) {
                Debug.outln( "isWorkspaceTop() = true for company home node: "
                             + this );
            }
            return true;
        }
        if ( Debug.isOn() ) {
            Debug.outln( "isWorkspaceTop() = false for node " + this );
        }

        return false;
    }

    private String getSysmlIdOfProperty( String propertyName, Date dateTime ) {
        NodeRef elementRef = (NodeRef)this.getProperty( propertyName );
        return getSysmlIdFromNodeRef( elementRef );
    }

    private String getSysmlIdFromNodeRef( NodeRef nodeRef ) {
        if ( nodeRef != null ) {
            EmsScriptNode node =
                    new EmsScriptNode( nodeRef, services, response );
            if ( node != null && node.exists() ) {
                return (String)node.getProperty( "sysml:id" );
            }
        }
        return null;
    }

    private ArrayList< String >
            getSysmlIdsFromNodeRefs( ArrayList< NodeRef > nodeRefs ) {
        ArrayList< String > ids = new ArrayList< String >();
        if ( nodeRefs != null ) {
            for ( NodeRef nodeRef : nodeRefs ) {
                String id = getSysmlIdFromNodeRef( nodeRef );
                if ( id != null ) {
                    ids.add( id );
                } else {
                    // TODO error handling
                }
            }
        }
        return ids;
    }

    private void addVersionToArray( NodeRef nRef, Date dateTime,
                                    JSONArray jsonArray ) throws JSONException {
        NodeRef versionedRef = nRef;
        if ( dateTime != null ) {
            versionedRef = NodeUtil.getNodeRefAtTime( nRef, dateTime );
        }
        if ( versionedRef != null ) {
            EmsScriptNode node =
                    new EmsScriptNode( versionedRef, services, response );
            if ( node != null && node.exists() ) {
                jsonArray.put( node.toJSONObject( null, true, null ) );
            }
        } else {
            // TODO error handling
        }
    }

    private
            Object
            addInternalJSON( Object nodeRefs, Date dateTime )
                                                             throws JSONException {
        if ( nodeRefs == null ) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        if ( nodeRefs instanceof Collection ) {
            Collection< NodeRef > nodeRefColl = (Collection< NodeRef >)nodeRefs;
            for ( NodeRef nRef : nodeRefColl ) {
                addVersionToArray( nRef, dateTime, jsonArray );
            }
        } else if ( nodeRefs instanceof NodeRef ) {
            addVersionToArray( (NodeRef)nodeRefs, dateTime, jsonArray );
            return jsonArray.get( 0 );
        }
        return jsonArray;
    }

    private JSONArray addNodeRefIdsJSON( ArrayList< NodeRef > nodeRefs,
                                         Date dateTime ) {
        ArrayList< String > nodeIds = getSysmlIdsFromNodeRefs( nodeRefs );
        JSONArray ids = new JSONArray();
        for ( String nodeId : nodeIds ) {
            ids.put( nodeId );
        }
        return ids;
    }

    /***************************************************************************************
     *
     * Methods that follow are called reflectively to add aspect metadata to
     * JSON object.
     *
     * Follows same order as aspects in sysmlModel.xml. Use protected so
     * warnings about unused private methods don't occur.
     *
     **************************************************************************************/
    protected
            void
            addPackageJSON( JSONObject json, EmsScriptNode node,
                            Set< String > filter, Date dateTime )
                                                                 throws JSONException {
        // do nothing - package currently doesn't have any additional metadata
    }

    protected void addViewpointJSON( JSONObject json, Set< String > filter,
                                     Date dateTime ) throws JSONException {
        json.put( "method",
                  this.getSysmlIdOfProperty( "sysml:method", dateTime ) );
    }

    protected
            void
            addViewJSON( JSONObject json, EmsScriptNode node,
                         Set< String > filter, Date dateTime )
                                                              throws JSONException {
        // TODO: figure out why this isn't working
        // json.put( "contains", getView().getContainsJson() );
        // json.put( "displayedElements", getView().getDisplayedElements() );
        // json.put( "allowedElements", getView().getDisplayedElements() );
        // json.put( "childrenViews", getView().getChildViews() );
        putInJson( json,
                   "contains",
                   new JSONArray( (String)node.getProperty( "view2:contains" ) ),
                   filter );
        putInJson( json,
                   "displayedElements",
                   new JSONArray(
                                  (String)node.getProperty( "view2:displayedElements" ) ),
                   filter );
        putInJson( json,
                   "allowedElements",
                   new JSONArray(
                                  (String)node.getProperty( "view2:allowedElements" ) ),
                   filter );
        putInJson( json,
                   "childrenViews",
                   new JSONArray(
                                  (String)node.getProperty( "view2:childrenViews" ) ),
                   filter );

        // TODO: Snapshots?
    }

    protected
            void
            addProductJSON( JSONObject json, EmsScriptNode node,
                            Set< String > filter, Date dateTime )
                                                                 throws JSONException {
        putInJson( json,
                   "view2view",
                   new JSONArray( (String)node.getProperty( "view2:view2view" ) ),
                   filter );
        putInJson( json,
                   "noSections",
                   new JSONArray( (String)node.getProperty( "view2:noSections" ) ),
                   filter );
        addViewJSON( json, node, filter, dateTime );
    }

    protected
            void
            addPropertyJSON( JSONObject json, EmsScriptNode node,
                             Set< String > filter, Date dateTime )
                                                                  throws JSONException {
        putInJson( json, "isDerived", node.getProperty( "sysml:isDerived" ),
                   filter );
        putInJson( json, "isSlot", node.getProperty( "sysml:isSlot" ), filter );
        putInJson( json,
                   "value",
                   addInternalJSON( node.getProperty( "sysml:value" ), dateTime ),
                   filter );
        Object propertyType = node.getProperty( "sysml:propertyType" );
        if ( propertyType != null ) {
            Object propTypeJson = addInternalJSON( propertyType, dateTime );
            if ( propTypeJson instanceof JSONArray ) {
                putInJson( json, "propertyType", propTypeJson, filter );
            } else {
                putInJson( json, "propertyType", propTypeJson, filter );
            }
        }
    }

    protected
            void
            addDirectedRelationshipJSON( JSONObject json, EmsScriptNode node,
                                         Set< String > filter, Date dateTime )
                                                                              throws JSONException {
        String id;

        id = getSysmlIdOfProperty( "sysml:source", dateTime );
        if ( id != null ) {
            putInJson( json, "source", id, filter );
        }

        id = getSysmlIdOfProperty( "sysml:target", dateTime );
        if ( id != null ) {
            putInJson( json, "target", id, filter );
        }
    }

    protected
            void
            addDependencyJSON( JSONObject json, EmsScriptNode node,
                               Set< String > filter, Date dateTime )
                                                                    throws JSONException {
        addDirectedRelationshipJSON( json, node, filter, dateTime );
    }

    protected
            void
            addExposeJSON( JSONObject json, EmsScriptNode node,
                           Set< String > filter, Date dateTime )
                                                                throws JSONException {
        addDirectedRelationshipJSON( json, node, filter, dateTime );
    }

    protected
            void
            addConformJSON( JSONObject json, EmsScriptNode node,
                            Set< String > filter, Date dateTime )
                                                                 throws JSONException {
        addDirectedRelationshipJSON( json, node, filter, dateTime );
    }

    protected
            void
            addGeneralizationJSON( JSONObject json, EmsScriptNode node,
                                   Set< String > filter, Date dateTime )
                                                                        throws JSONException {
        addDirectedRelationshipJSON( json, node, filter, dateTime );
    }

    protected
            void
            addValueSpecificationJSON( JSONObject json, EmsScriptNode node,
                                       Set< String > filter, Date dateTime )
                                                                            throws JSONException {
        String valueExpressionId =
                node.getSysmlIdOfProperty( "sysml:valueExpression", dateTime );
        if ( valueExpressionId != null ) {
            putInJson( json, "expression", valueExpressionId, filter );
        }
    }

    protected
            void
            addDurationJSON( JSONObject json, EmsScriptNode node,
                             Set< String > filter, Date dateTime )
                                                                  throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
    }

    protected
            void
            addDurationIntervalJSON( JSONObject json, EmsScriptNode node,
                                     Set< String > filter, Date dateTime )
                                                                          throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );

        String id;

        id = getSysmlIdOfProperty( "sysml:durationMax", dateTime );
        if ( id != null ) {
            putInJson( json, "max", id, filter );
        }

        id = getSysmlIdOfProperty( "sysml:durationMin", dateTime );
        if ( id != null ) {
            putInJson( json, "min", id, filter );
        }
    }

    protected
            void
            addElementValueJSON( JSONObject json, EmsScriptNode node,
                                 Set< String > filter, Date dateTime )
                                                                      throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );

        String elementId =
                node.getSysmlIdOfProperty( "sysml:elementValueOfElement",
                                           dateTime );
        if ( elementId != null ) {
            putInJson( json, "element", elementId, filter );
        }
    }

    protected
            void
            addExpressionJSON( JSONObject json, EmsScriptNode node,
                               Set< String > filter, Date dateTime )
                                                                    throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );

        ArrayList< NodeRef > nodeRefs =
                (ArrayList< NodeRef >)node.getProperty( "sysml:operand" );
        JSONArray array = new JSONArray();
        for ( NodeRef nodeRef : nodeRefs ) {
            NodeRef versionedRef = nodeRef;
            if ( dateTime != null ) {
                versionedRef = NodeUtil.getNodeRefAtTime( nodeRef, dateTime );
            }
            if ( versionedRef != null
                 && services.getNodeService().exists( versionedRef ) ) {
                EmsScriptNode versionedNode =
                        new EmsScriptNode( versionedRef, services, response );
                JSONObject jsonObject = new JSONObject();
                // operands can reference anything, so call recursively as
                // necessary
                versionedNode.addSpecializationJSON( jsonObject, filter,
                                                     dateTime );
                array.put( jsonObject );
            } else {
                // TODO: Error handling
            }

        }
        putInJson( json, "operand", array, filter );
    }

    protected
            void
            addInstanceValueJSON( JSONObject json, EmsScriptNode node,
                                  Set< String > filter, Date dateTime )
                                                                       throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );

        String id = node.getSysmlIdOfProperty( "sysml:instance", dateTime );
        if ( id != null ) {
            putInJson( json, "instance", id, filter );
        }
    }

    protected
            void
            addIntervalJSON( JSONObject json, EmsScriptNode node,
                             Set< String > filter, Date dateTime )
                                                                  throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
    }

    protected
            void
            addLiteralBooleanJSON( JSONObject json, EmsScriptNode node,
                                   Set< String > filter, Date dateTime )
                                                                        throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        putInJson( json, "boolean", node.getProperty( "sysml:boolean" ), filter );
    }

    protected
            void
            addLiteralIntegerJSON( JSONObject json, EmsScriptNode node,
                                   Set< String > filter, Date dateTime )
                                                                        throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        putInJson( json, "integer", node.getProperty( "sysml:integer" ), filter );
    }

    protected
            void
            addLiteralNullJSON( JSONObject json, EmsScriptNode node,
                                Set< String > filter, Date dateTime )
                                                                     throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
    }

    protected
            void
            addLiteralRealJSON( JSONObject json, EmsScriptNode node,
                                Set< String > filter, Date dateTime )
                                                                     throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        putInJson( json, "double", node.getProperty( "sysml:double" ), filter );
    }

    protected
            void
            addLiteralStringJSON( JSONObject json, EmsScriptNode node,
                                  Set< String > filter, Date dateTime )
                                                                       throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        putInJson( json, "string", node.getProperty( "sysml:string" ), filter );
    }

    protected
            void
            addLiteralUnlimitedNaturalJSON( JSONObject json,
                                            EmsScriptNode node,
                                            Set< String > filter, Date dateTime )
                                                                                 throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        putInJson( json, "naturalValue",
                   node.getProperty( "sysml:naturalValue" ), filter );
    }

    protected
            void
            addOpaqueExpressionJSON( JSONObject json, EmsScriptNode node,
                                     Set< String > filter, Date dateTime )
                                                                          throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        putInJson( json, "expressionBody",
                   node.getProperty( "sysml:expressionBody" ), filter );
    }

    protected
            void
            addStringExpressionJSON( JSONObject json, EmsScriptNode node,
                                     Set< String > filter, Date dateTime )
                                                                          throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
    }

    protected
            void
            addTimeExpressionJSON( JSONObject json, EmsScriptNode node,
                                   Set< String > filter, Date dateTime )
                                                                        throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
    }

    protected
            void
            addTimeIntervalJSON( JSONObject json, EmsScriptNode node,
                                 Set< String > filter, Date dateTime )
                                                                      throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        String id;

        id = getSysmlIdOfProperty( "sysml:timeIntervalMax", dateTime );
        if ( id != null ) {
            putInJson( json, "max", id, filter );
        }

        id = getSysmlIdOfProperty( "sysml:timeIntervalMin", dateTime );
        if ( id != null ) {
            putInJson( json, "min", id, filter );
        }
    }

    protected
            void
            addOperationJSON( JSONObject json, EmsScriptNode node,
                              Set< String > filter, Date dateTime )
                                                                   throws JSONException {
        ArrayList< NodeRef > nodeRefs =
                (ArrayList< NodeRef >)node.getProperty( "sysml:operationParameter" );
        JSONArray ids = addNodeRefIdsJSON( nodeRefs, dateTime );
        if ( ids.length() > 0 ) {
            putInJson( json, "parameters", ids, filter );
        }

        String id =
                getSysmlIdOfProperty( "sysml:operationExpression", dateTime );
        if ( id != null ) {
            putInJson( json, "expression", id, filter );
        }
    }

    protected
            void
            addInstanceSpecificationJSON( JSONObject json, EmsScriptNode node,
                                          Set< String > filter, Date dateTime )
                                                                               throws JSONException {
        putInJson( json,
                   "specification",
                   node.getProperty( "sysml:instanceSpecificationSpecification" ),
                   filter );
    }

    protected
            void
            addConstraintJSON( JSONObject json, EmsScriptNode node,
                               Set< String > filter, Date dateTime )
                                                                    throws JSONException {
        String specId =
                getSysmlIdOfProperty( "sysml:constraintSpecification", dateTime );
        if ( specId != null ) {
            putInJson( json, "specification", specId, filter );
        }
    }

    protected
            void
            addParameterJSON( JSONObject json, EmsScriptNode node,
                              Set< String > filter, Date dateTime )
                                                                   throws JSONException {
        putInJson( json, "direction",
                   node.getProperty( "sysml:parameterDirection" ), filter );
        putInJson( json, "parameterType",
                   node.getProperty( "sysml:parameterType" ), filter );

        String id =
                node.getSysmlIdOfProperty( "sysml:parameterDefaultValue",
                                           dateTime );
        if ( id != null ) {
            putInJson( json, "defaultValue", id, filter );
        }
    }

    protected
            void
            addConnectorJSON( JSONObject json, EmsScriptNode node,
                              Set< String > filter, Date dateTime )
                                                                   throws JSONException {
        ArrayList< NodeRef > nodeRefs =
                (ArrayList< NodeRef >)node.getProperty( "sysml:roles" );
        JSONArray ids = addNodeRefIdsJSON( nodeRefs, dateTime );
        putInJson( json, "connectorRoles", ids, filter );
    }

    /**************************
     * Miscellaneous functions
     **************************/
    public Version getHeadVersion() {
        Version headVersion = null;
        if ( getIsVersioned() ) {
            VersionHistory history =
                    this.services.getVersionService()
                                 .getVersionHistory( this.nodeRef );
            if ( history != null ) {
                headVersion = history.getHeadVersion();
            }
        }
        return headVersion;
    }

    public static List< String > getNames( List< EmsScriptNode > nodes ) {
        List< String > names = new ArrayList< String >();
        for ( EmsScriptNode node : nodes ) {
            String name = node.getName();
            if ( !Utils.isNullOrEmpty( name ) ) {
                names.add( name );
            }
        }
        return names;
    }

    public static Collection< ? extends NodeRef >
            getNodeRefs( List< EmsScriptNode > nodes ) {
        List< NodeRef > refs = new ArrayList< NodeRef >();
        for ( EmsScriptNode node : nodes ) {
            NodeRef ref = node.getNodeRef();
            refs.add( ref );
        }
        return refs;
    }

    public static List< EmsScriptNode >
            toEmsScriptNodeList( Collection< NodeRef > refs ) {
        ArrayList<EmsScriptNode> nodes = new ArrayList< EmsScriptNode >();
        for ( NodeRef ref : refs ) {
            nodes.add( new EmsScriptNode( ref, NodeUtil.getServices() ) );
        }
        return nodes;
    }
}
