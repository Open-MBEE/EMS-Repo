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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
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

    static Logger logger = Logger.getLogger(ScriptNode.class);

    public static boolean expressionStuff = false; // The value here is ignored.

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
                    add( "ems:parent" );
                    add( "ems:children" );
                    add( "ems:lastTimeSyncParent" );
                    add( "ems:mergeSource" );
                }
            };
    public static TreeSet< String > workspaceMetaAspects =
            new TreeSet< String >() {
                private static final long serialVersionUID = 1L;
                {
                    add( "ems:HasWorkspace" );
                    add( "ems:Trashed" );
                    add( "ems:Added" );
                    add( "ems:Deleted" );
                    add( "ems:Moved" );
                    add( "ems:Updated" );
                    add( "ems:MergeSource" );
                    add( "ems:Committable" );
                    add( "st:site" );
                    add( "st:sites" );
                }
            };

    // Flag to indicate whether we checked the nodeRef version for this script node,
    // when doing getProperty().
    private boolean checkedNodeVersion = false;
    
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

    /**
     * When writing out JSON, evaluate Expressions and include the results.
     */
    private boolean evaluatingExpressions;
    
    /**
     * Replicates the behavior of ScriptNode versions, which is private.
     */
    protected Object[] myVersions = null;

    public static boolean fixOwnedChildren = false;

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

    public EmsScriptNode childByNamePath( String path, boolean ignoreWorkspace, WorkspaceNode workspace,
                                          boolean onlyWorkspace) {
        // Make sure this node is in the target workspace.
        EmsScriptNode node = this;
        if ( !ignoreWorkspace && workspace != null && !workspace.equals( getWorkspace() ) ) {
            node = findScriptNodeByName( getName(), ignoreWorkspace, workspace, null );
        }
        // See if the path/child is in this workspace.
        EmsScriptNode child = node.childByNamePath( path );
        if ( child != null && child.exists() ) {
            return child;
        }
        
        // Find the path/child in a parent workspace if not constraining only to the current workspace:
        if (!onlyWorkspace) {
            EmsScriptNode source = node.getWorkspaceSource();
            while ( source != null && source.exists()
                    && ( child == null || !child.exists() ) ) {
                child = source.childByNamePath( path );
                source = source.getWorkspaceSource();
            }
            if ( child != null && child.exists() ) {
                return child;
            }
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
//        Debug.error( msg );
        return null;
    }
    
    /**
     * Gets the version history
     * 
     * This is needed b/c the ScriptNode getVersionHistory() generates a NPE
     * 
     * @return  version history
     */
    public Object[] getEmsVersionHistory()
    {

    	if (this.myVersions == null && getIsVersioned())
        {
            VersionHistory history = this.services.getVersionService().getVersionHistory(this.nodeRef);
            if (history != null)
            {
                Collection<Version> allVersions = history.getAllVersions();
                Object[] versions = new Object[allVersions.size()];
                int i = 0;
                for (Version version : allVersions)
                {
                    versions[i++] = new ScriptVersion(version, this.services, this.scope);
                }
                this.myVersions = versions;
            }
        }
        return this.myVersions;
    
    }
    
    /**
     * Create a version of this document.  Note: this will add the cm:versionable aspect.
     * 
     * @param history       Version history note
     * @param majorVersion  True to save as a major version increment, false for minor version.
     * 
     * @return ScriptVersion object representing the newly added version node
     */
    @Override
    public ScriptVersion createVersion(String history, boolean majorVersion)
    {
    	this.myVersions = null;
    	return super.createVersion(history, majorVersion);
    }
    
    /**
     * Check-in a working copy document. The current state of the working copy is copied to the original node,
     * this will include any content updated in the working node. Note that this method can only be called on a
     * working copy Node.
     * 
     * @param history       Version history note
     * @param majorVersion  True to save as a major version increment, false for minor version.
     * 
     * @return the original Node that was checked out.
     */
    @Override
    public ScriptNode checkin(String history, boolean majorVersion)
    {
    	this.myVersions = null;
        return super.checkin(history, majorVersion);
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
        return createFolder(name, null);
    }

    @Override
    public EmsScriptNode createFolder( String name, String type ) {
        return createFolder(name, type, null);
    }
    
    public EmsScriptNode createFolder( String name, String type, NodeRef sourceFolder ) {
        
        NodeRef folderRef = super.createFolder( name, type ).getNodeRef();
        EmsScriptNode folder = new EmsScriptNode(folderRef,services, response, status );
        WorkspaceNode ws = getWorkspace();

        if ( ws != null && !folder.isWorkspace() ) {
            folder.setWorkspace( ws, sourceFolder );
        }

        if ( Debug.isOn() ) {
            Debug.outln( "createFolder(" + name + "): returning " + folder );
        }

        return folder;
    }

    /**
     * Check whether or not a node has the specified aspect, add it if not
     *
     * @param string
     *            Short name (e.g., sysml:View) of the aspect to look for
     * @return true if node updated with aspect
     */
    public boolean createOrUpdateAspect( String type ) {
        if ( Acm.getJSON2ACM().keySet().contains( type ) ) {
            type = Acm.getJSON2ACM().get( type );
        }

        return changeAspect( type );
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
            t = (T) XrefConverter.convertXref((String)t);
            value = t;
        }
        @SuppressWarnings( "unchecked" )
        // It is important we ignore the workspace when getting the property, so we make sure
        // to update this property when needed.  Otherwise, property may have a noderef in 
        // a parent workspace, and this wont detect it; however, all the getProperty() will look
        // for the correct workspace node, so perhaps this is overkill:
        T oldValue = (T)getProperty( acmType, true, null, false, true );
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

    public static long getChecksum( String dataString ) {
        byte[] data = null;
        data = dataString.getBytes(); // ( "UTF-8" );
        return getChecksum( data );
    }

    public static long getChecksum( byte[] data ) {
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
        if ( resultSet != null ) for ( NodeRef ref : resultSet ) {
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
        
    	return NodeUtil.updateOrCreateArtifact(name, type, base64content, null, targetSiteName,
    										   subfolderName, workspace, dateTime,
    										   response, status, false);
    }

    public String extractAndReplaceImageData( String value ) {
        if ( value == null ) return null;
        String v = value;
//        Document doc = Jsoup.parse( v );
//        Elements imgs = doc.select( "img.src" );
//        System.out.println("imgs = " + imgs);
//        for (Element img: imgs) {
//            String src = img.attr("src");
//            int index = src.indexOf( "base64," );
//            System.out.println("indexOf \"base64,\"" + index);
//            System.out.println("src = " + src.substring( 0, Math.min( src.length()-1, 100 ) ) + " . . .");
//            if (src.startsWith( "data" ) && index > 0) {
//                String mediatype = src.substring( "data:".length(), index );
//                System.out.println("mediatype = " + mediatype);
//                if (mediatype.startsWith( "image/" )) {
//                    String extension = mediatype.replace( "image/", "" );
//                    index += "base64,".length();
//                    String content = src.substring( index );
//                    String name = "img_" + System.currentTimeMillis();
//                    EmsScriptNode artNode =
//                            findOrCreateArtifact( name, extension, content,
//                                                  getSiteName(), "images",
//                                                  getWorkspace(), null );
//                    if ( artNode == null || !artNode.exists() ) {
//                        log( "Failed to pull out image data for value! "
//                             + value );
//                        break;
//                    }
//
//                    String url = artNode.getUrl();
//                    String link = url.replace( "/d/d/", "/alfresco/service/api/node/content/" );
//                    img.attr( src, link );
//                }
//            }
//            v = doc.select( "body" ).html();
//        }
        //Debug.turnOn();
        if ( Debug.isOn()) Debug.outln("extractAndReplaceImageData(" + v.substring( 0, Math.min( v.length(), 100 ) ) + (v.length()>100 ? " . . ." :"") + ")");
        while ( true ) {
            Pattern p = Pattern.compile( "(.*)<img[^>]*\\ssrc\\s*=\\s*[\"']data:image/(\\w*);\\s*base64\\s*,([^\"']*)[\"'][^>]*>(.*)",
                                         Pattern.DOTALL );
            Matcher m = p.matcher( v );
            if ( !m.matches() ) {
                if ( Debug.isOn() ) {
                    Debug.outln( "no match found for v=" +v.substring( 0, Math.min( v.length(), 100 ) ) + (v.length()>100 ? " . . ." :"") + ")");
                }
                break;
            } else {
                if ( Debug.isOn() ) {
                    Debug.outln( "match found for v=" +v.substring( 0, Math.min( v.length(), 100 ) ) + (v.length()>100 ? " . . ." :"") + ")");
                }
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
        //Debug.turnOff();
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
     * @param parent - this could be the reified package or the reified node
     */
    public EmsScriptNode setOwnerToReifiedNode( EmsScriptNode parent, WorkspaceNode ws ) {
        // everything is created in a reified package, so need to make
        // relations to the reified node rather than the package
        EmsScriptNode reifiedNode = parent.getReifiedNode();
        if ( reifiedNode == null ) reifiedNode = parent; // just in case
        if ( reifiedNode != null ) {
            EmsScriptNode nodeInWs =
                    NodeUtil.findScriptNodeById( reifiedNode.getSysmlId(), ws,
                                                 null, false, getServices(),
                                                 getResponse() );
            if ( nodeInWs != null ) reifiedNode = nodeInWs;
            // store owner with created node
            this.createOrUpdateAspect( "ems:Owned" );
            this.createOrUpdateProperty( "ems:owner",
                                         reifiedNode.getNodeRef() );

            // add child to the parent as necessary
            reifiedNode.createOrUpdateAspect( "ems:Owned" );
            reifiedNode.appendToPropertyNodeRefs( "ems:ownedChildren",
                                                  this.getNodeRef() );
        }
        return reifiedNode;
    }
    
    /**
     * Create an EmsScriptNode adding aspects based on the input sysml type
     * name.
     *
     * @param sysmlId
     *            the @sysml:id
     * @param sysmlAcmType
     *            Alfresco Content Model type of node to create or an aspect
     * @return created child EmsScriptNode
     * @throws Exception 
     */
    public EmsScriptNode createSysmlNode( String sysmlId, String sysmlAcmType, ModStatus modStatus,
                                          WorkspaceNode nodeWorkspace) throws Exception {
        String type = NodeUtil.getContentModelTypeName( sysmlAcmType, services );
        EmsScriptNode node = createNode( sysmlId, type );

        if ( node != null ) {
            if ( !type.equals( sysmlAcmType )
                 && NodeUtil.isAspect( sysmlAcmType ) ) {
                node.createOrUpdateAspect( sysmlAcmType );
            }

            // everything is created in a reified package, so need to make
            // relations to the reified node rather than the package
            EmsScriptNode reifiedNode = node.setOwnerToReifiedNode( this, nodeWorkspace );
            if ( reifiedNode == null ) {
                // TODO error handling
            }
            
            // We are now setting the cm:name to the alfresco id because we found that
            // magicdraw sysmlids can have the same id with only differing cases, which 
            // alfresco does not allow.  Also, setting the cm:name to the alrefsco id does
            // not work, so pre-pending with "cm_"
            String alfrescoId = node.getId();
            node.setProperty( Acm.CM_NAME, "cm_"+alfrescoId);
            node.setProperty( Acm.ACM_ID, sysmlId );
            modStatus.setState( ModStatus.State.ADDED  );
            if ( nodeWorkspace != null && nodeWorkspace.exists() ) {
                node.setWorkspace( nodeWorkspace, null );
            }
        }
        
        if ( node == null || !node.exists() ) {
            throw new Exception( "createNode() failed." );
        }
        
        return node;
    }

    public EmsScriptNode getReifiedNode(boolean findDeleted) {
        NodeRef nodeRef = (NodeRef)getProperty( "ems:reifiedNode", false, null, 
                                                findDeleted, false );
        if ( nodeRef != null ) {
            return new EmsScriptNode( nodeRef, services, response );
        }
        return null;
    }
    
    public EmsScriptNode getReifiedNode() {
        return getReifiedNode(false);
    }

    public EmsScriptNode getReifiedPkg() {
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
//        NodeRef nr = findNodeRefByType( name, SearchType.CM_NAME.prefix, true,
//                                        workspace, null, false );
//        
//        EmsScriptNode n = new EmsScriptNode( nr, getServices() ); 
//        if ( !n.checkPermissions( PermissionService.ADD_CHILDREN, getResponse(),
//                                 getStatus() ) ) {
//            log( "No permissions to add children to " + n.getName() );
//            return null;
//        }

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
                    if ( Debug.isOn() ) 
                        System.out.println( "Got exception in "
                                            + "createNode(name="
                                            + name + ", type=" + type
                                            + ") for EmsScriptNode(" + this
                                            + ") calling createNode(nodeRef="
                                            + nodeRef + ", . . .)" );
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
        super.getName();
        return (String)getProperty( Acm.CM_NAME );
    }

    public String getSysmlName() {
        return (String)getProperty( Acm.ACM_NAME );
    }
    public String getSysmlName(Date dateTime) {
        EmsScriptNode esn = this;
        if ( dateTime != null ) {
            NodeRef ref = NodeUtil.getNodeRefAtTime( getNodeRef(), dateTime );
            esn = new EmsScriptNode( ref, getServices() );
        }
        return esn.getSysmlName();
    }

    public String getSysmlId() {
        String id = (String)getProperty( Acm.ACM_ID );
        if (id == null) {
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
     * Return the version of the parent at a specific time. This uses the
     * ems:owner property instead of getParent() when it returns non-null; else,
     * it call getParent(). For workspaces, the parent should always be in the
     * same workspace, so there is no need to specify (or use) the workspace.
     *
     * @param dateTime
     * @return the parent/owning node
     */
    public EmsScriptNode getOwningParent( Date dateTime ) {
        EmsScriptNode node = null;
        NodeRef ref = (NodeRef)getProperty( "ems:owner" );
        if ( ref == null ) {
            node = getParent();
        } else {
            node = new EmsScriptNode( ref, getServices() );
        }
        if ( node == null ) return null;
        if ( dateTime != null ) {
            NodeRef vref = NodeUtil.getNodeRefAtTime( node.getNodeRef(), dateTime );
            if ( vref != null ) {
                node = new EmsScriptNode( vref, getServices() );
            }
        }
        return node;
    }
    
    /**
     * Returns the children for this node.  Uses the ems:ownedChildren property.
     * 
     * @param findDeleted Find deleted nodes also
     * @return children of this node
     */
    public ArrayList<NodeRef> getOwnedChildren(boolean findDeleted) {
                
        ArrayList<NodeRef> ownedChildren = new ArrayList<NodeRef>();
        
        ArrayList<NodeRef> oldChildren = this.getPropertyNodeRefs( "ems:ownedChildren",
                                                                   false, null, findDeleted, false);
        if (oldChildren != null) {
            ownedChildren = oldChildren;
        }
    
        return ownedChildren;
       
    }

    public EmsScriptNode getUnreifiedParent( Date dateTime ) {
        EmsScriptNode parent = getOwningParent( dateTime );
        if ( parent != null ) {
            parent = parent.getUnreified( dateTime );
        }
        return parent;
    }

    public EmsScriptNode getUnreified( Date dateTime ) {
        if ( !isReified() ) return this;
        String sysmlId = getSysmlId();
        sysmlId = sysmlId.replaceAll( "^(.*)_pkg$", "$1" );
        EmsScriptNode unreified =
                findScriptNodeByName( sysmlId, false, getWorkspace(), dateTime );
        return unreified;
    }

    public boolean isReified() {
        String sysmlId = getSysmlId();
        if ( isFolder() && sysmlId != null && sysmlId.endsWith( "_pkg" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Get the property of the specified type
     *
     * @param acmType
     *            Short name of property to get
     * @return
     */
    public Object getProperty( String acmType ) {
        // FIXME Sometimes we wont want these defaults, ie want to find the deleted elements. 
        //       Need to check all calls to getProperty() with properties that are NodeRefs.
        return getProperty(acmType, false, null, false, false);
    }
    
    public String getVersionLabel() {
        Version v = getCurrentVersion();
        if ( v != null ) {
            return v.getVersionLabel();
        }
        return null;
    }
    
    public Version getCurrentVersion() {
        VersionService versionService = services.getVersionService();
        
        if (versionService != null) {
            Version currentVersion = versionService.getCurrentVersion( nodeRef );
            return currentVersion;
        }
        return null;
    }
    
    public boolean isAVersion() {
      return getIsVersioned();
    }

    public boolean checkNodeRefVersion2( Date dateTime ) {

        // Because of a alfresco bug, we must verify that we are getting the latest version
        // of the nodeRef if not specifying a dateTime:
        if ( checkedNodeVersion || dateTime != null ) return false; //|| isAVersion()) return false;

        String id = getId();
        NodeRef nr = NodeUtil.heisenCacheGet( id );
        if ( nr != null ) {
            if ( nr.equals( nodeRef ) ) return false;
            nodeRef = nr;
            return true;
        }

        // Not in cache -- need to compute
        boolean changed = checkNodeRefVersion( null );
        NodeUtil.heisenCachePut( id, nodeRef );

        return changed;
    }
    
    /**
     * Verifies that the nodeRef is the most recent if dateTime is null and not
     * already checked for this node.  Replaces the nodeRef with the most recent
     * if needed.  This is needed b/c of a alfresco bug.
     */
    public boolean checkNodeRefVersion(Date dateTime) {
        
        // Because of a alfresco bug, we must verify that we are getting the latest version
        // of the nodeRef if not specifying a dateTime:
        if (dateTime == null && !checkedNodeVersion && !isAVersion()) {
            
            checkedNodeVersion = true;
            Version currentVersion = getCurrentVersion();
            Version headVersion = getHeadVersion();
            
            if (currentVersion != null && headVersion != null) {
                
                String currentVerLabel = currentVersion.getVersionLabel();
                String headVerLabel = headVersion.getVersionLabel();
                
                // If this is not the most current node ref, replace it with the most current:
                if (currentVerLabel != null && headVerLabel != null &&
                    !currentVerLabel.equals( headVerLabel ) ) {
                    
                    NodeRef fnr = headVersion.getFrozenStateNodeRef();
                    if (fnr != null) {
                        // Cache is correct -- fix esn's nodeRef
                        String msg = "Warning! Alfresco Heisenbug returning wrong current version of node, " + this + ".  Replacing node with unmodifiable versioned node, " + getId() + ".";
                        logger.warn( msg );
                        if ( response != null ) {
                            response.append( msg + "\n");
                        }
                        nodeRef = fnr;
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Get the property of the specified type
     *
     * @param acmType
     *            Short name of property to get
     * @return
     */
    public Object getProperty( String acmType, boolean ignoreWorkspace,
                               Date dateTime, boolean findDeleted, 
                               boolean skipNodeRefCheck ) {
        
        if ( Utils.isNullOrEmpty( acmType ) ) return null;
        Object result = null;
        
        // Taking this out for now b/c of performance hit:
        //checkNodeRefVersion(dateTime);
        
        if ( useFoundationalApi ) {
            QName typeQName = createQName( acmType );
            result = services.getNodeService().getProperty( nodeRef, typeQName );
        } else {
            result = getProperties().get( acmType );
        }
        
        // get noderefs from the proper workspace unless the property is a
        // workspace meta-property
        if ( !skipNodeRefCheck && !workspaceMetaProperties.contains( acmType )) {
            if ( result instanceof NodeRef ) {
                result = NodeUtil.getNodeRefAtTime( (NodeRef)result,
                                                    getWorkspace(), dateTime,
                                                    ignoreWorkspace, findDeleted);
            } else if ( result instanceof Collection ) {
                Collection< ? > resultColl = (Collection< ? >)result;
                ArrayList< Object > arr = new ArrayList< Object >();
                for ( Object o : resultColl ) {
                    if ( o instanceof NodeRef ) {
                        NodeRef ref =
                                NodeUtil.getNodeRefAtTime( (NodeRef)o,
                                                           getWorkspace(), dateTime,
                                                           ignoreWorkspace, findDeleted);
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

        // We no longer need to check this, as values specs are always embedded within
        // the nodes that use them, ie Property, so the modified time will always be updated
        // when modifying the value spec
//        Object value = getProperty( Acm.ACM_VALUE );
//        ArrayList< NodeRef > dependentNodes = new ArrayList< NodeRef >();
//        if ( value instanceof Collection ) {
//            Collection< ? > c = (Collection< ? >)value;
//            dependentNodes.addAll( Utils.asList( c, NodeRef.class ) );
//        }
//        for ( NodeRef nodeRef : dependentNodes ) {
//            nodeRef = NodeUtil.getNodeRefAtTime( nodeRef, dateTime );
//            if ( nodeRef == null ) continue;
//            EmsScriptNode oNode = new EmsScriptNode( nodeRef, services );
//            if ( !oNode.exists() ) continue;
//            Date modified = oNode.getLastModified( dateTime );
//            if ( modified.after( lastModifiedDate ) ) {
//                lastModifiedDate = modified;
//            }
//        }
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
        
        // Taking this out for now b/c of performance hit:
        //checkNodeRefVersion(null);

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
                if ( Debug.isOn() ) {
                    System.out.println( "Got exception in "
                                        + "setProperty(acmType=" + acmType
                                        + ", value=" + value
                                        + ") for EmsScriptNode " + this
                                        + " calling setProperty(nodeRef="
                                        + nodeRef + ", " + acmType + ", "
                                        + value + ")" );
                }
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
        String pkgSuffix = "_pkg";
        
        // TODO REVIEW
        // This is currently not called on reified packages, so as long as the ems:owner always points
        // to reified nodes, as it should, then we dont need to replace pkgSuffix in the qname.
        
        if ( isName ) {
            qname = "/" + getProperty( "sysml:name" );
        } else {
            qname =  "/" + getProperty( "sysml:id" );
            //qname = qname.endsWith(pkgSuffix) ? qname.replace(pkgSuffix, "" ) : qname;
        }

        NodeRef ownerRef = (NodeRef)this.getProperty( "ems:owner" );
        // Need to look up based on owners...
        while ( ownerRef != null ) {
            EmsScriptNode owner =
                    new EmsScriptNode( ownerRef, services, response );
            String nameProp = null;
            if ( isName ) {
                nameProp = (String)owner.getProperty( "sysml:name" );
            } else {
                nameProp = (String)owner.getProperty( "sysml:id" );
            }
            if ( nameProp == null ) {
                break;
            }
            //nameProp = nameProp.endsWith(pkgSuffix) ? nameProp.replace(pkgSuffix, "" ) : nameProp;
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
        String result = "";
        boolean wasOn = Debug.isOn();
        if ( wasOn ) Debug.turnOff();
        // try {
        // return "" + toJSONObject();
        // } catch ( JSONException e ) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // return null;
        try {
        if ( !exists() && !isDeleted() ) {
            return "NON-EXISTENT-NODE";
        }
        String deleted = isDeleted() ? "DELETED: " : "";
        String name = getName();
        String id = getSysmlId();
        String sysmlName = getSysmlName();
        String qualifiedName = getSysmlQName();
        String type = getTypeName();
        String workspaceName = getWorkspaceName();
        result = deleted + "{type=" + type + ", id=" + id + ", cm_name=" + name + ", sysml_name=" + sysmlName
                         + ", qualified name=" + qualifiedName + ", workspace="
                         + workspaceName + "}";
        } catch (Throwable t) {
            // ignore
        }
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
    public JSONObject toJSONObject( Set< String > filter, Date dateTime ) throws JSONException {
        return toJSONObject( filter, false, dateTime );
    }

    public String nodeRefToSysmlId( NodeRef ref ) throws JSONException {
        EmsScriptNode node = new EmsScriptNode( ref, services );
        Object sysmlId = node.getSysmlId();
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

    public Date getCreationDate() {
        Date date = (Date)getProperty( "cm:created" );
        return date;
    }

    protected void
            addElementJSON( JSONObject elementJson, Set< String > filter,
                            Date dateTime ) throws JSONException {
        EmsScriptNode node = getNodeAtAtime( dateTime );
        // mandatory elements put in directly
        elementJson.put( Acm.JSON_ID, node.getProperty( Acm.ACM_ID ) );
        elementJson.put( "creator", node.getProperty( "cm:modifier" ) );
//        elementJson.put( "modified",
//                         TimeUtils.toTimestamp( getLastModified( (Date)node.getProperty( "cm:modified" ) ) ) );
        elementJson.put( Acm.JSON_LAST_MODIFIED,
                TimeUtils.toTimestamp( getLastModified( dateTime) ) );

        putInJson( elementJson, Acm.JSON_NAME,
                   node.getProperty( Acm.ACM_NAME ), filter );
        putInJson( elementJson, Acm.JSON_DOCUMENTATION,
                   node.getProperty( Acm.ACM_DOCUMENTATION ), filter );
        putInJson( elementJson, "qualifiedName", node.getSysmlQName(), filter );
        putInJson( elementJson, "qualifiedId", node.getSysmlQId(), filter );
        putInJson( elementJson, "editable",
                   node.hasPermission( PermissionService.WRITE ), filter );
//        NodeRef ownerRef = (NodeRef)node.getProperty( "ems:owner" );
//        EmsScriptNode owner;
//        if ( ownerRef != null ) {
//            owner = new EmsScriptNode( ownerRef, services, response );
//        } else {
        EmsScriptNode owner = node.getOwningParent(dateTime);
//        }
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

    public enum SpecEnum  {
      Association,
      Binding,
      Characterizes,
      Conform,
      Connector,
      Constraint,
      Dependency,
      DirectedRelationship,
      DurationInterval,
      Duration,
      ElementValue,
      Expose,
      Expression,
      Generalization,
      InstanceSpecification,
      InstanceValue,
      Interval,
      LiteralBoolean,
      LiteralInteger,
      LiteralNull,
      LiteralReal,
      LiteralSet,
      LiteralString,
      LiteralUnlimitedNatural,
      MagicDrawData,
      OpaqueExpression,
      Operation,
      Package,
      Parameter,
      Product,
      Property,
      StringExpression,
      Succession,
      TimeExpression,
      TimeInterval,
      ValueSpecification,
      View
    };
    
    public static Map<String, SpecEnum> aspect2Key = new HashMap<String, SpecEnum>() {
        private static final long serialVersionUID = -2080928480362524333L;

        {
            put("Association", SpecEnum.Association);
            put("Binding", SpecEnum.Binding);
            put("Characterizes", SpecEnum.Characterizes);
            put("Conform", SpecEnum.Conform);
            put("Connector", SpecEnum.Connector);
            put("Constraint", SpecEnum.Constraint);
            put("Dependency", SpecEnum.Dependency);
            put("DirectedRelationship", SpecEnum.DirectedRelationship);
            put("DurationInterval", SpecEnum.DurationInterval);
            put("Duration", SpecEnum.Duration);
            put("ElementValue", SpecEnum.ElementValue);
            put("Expose", SpecEnum.Expose);
            put("Expression", SpecEnum.Expression);
            put("Generalization", SpecEnum.Generalization);
            put("InstanceSpecification", SpecEnum.InstanceSpecification);
            put("InstanceValue", SpecEnum.InstanceValue);
            put("Interval", SpecEnum.Interval);
            put("LiteralBoolean", SpecEnum.LiteralBoolean);
            put("LiteralInteger", SpecEnum.LiteralInteger);
            put("LiteralNull", SpecEnum.LiteralNull);
            put("LiteralReal", SpecEnum.LiteralReal);
            put("LiteralSet", SpecEnum.LiteralSet);
            put("LiteralString", SpecEnum.LiteralString);
            put("LiteralUnlimitedNatural", SpecEnum.LiteralUnlimitedNatural);
            put("MagicDrawData", SpecEnum.MagicDrawData);
            put("OpaqueExpression", SpecEnum.OpaqueExpression);
            put("Operation", SpecEnum.Operation);
            put("Package", SpecEnum.Package);
            put("Parameter", SpecEnum.Parameter);
            put("Product", SpecEnum.Product);
            put("Property", SpecEnum.Property);
            put("StringExpression", SpecEnum.StringExpression);
            put("Succession", SpecEnum.StringExpression);
            put("TimeExpression", SpecEnum.TimeExpression);
            put("TimeInterval", SpecEnum.TimeInterval);
            put("ValueSpecification", SpecEnum.ValueSpecification);
            put("View", SpecEnum.View);
        }
    };
    
    private void addSpecializationJSON( JSONObject json, Set< String > filter,
                                        Date dateTime ) throws JSONException {
        addSpecializationJSON( json, filter, dateTime, false );
    }
    private void addSpecializationJSON( JSONObject json, Set< String > filter,
                                        Date dateTime, boolean justTheType ) throws JSONException {
        String typeName = getTypeName();
        if ( typeName == null ) {
            // TODO: error logging
            return;
        }

        if ( filter == null || filter.isEmpty() || filter.contains("type") ) {
            json.put( "type", typeName );
        }
        
        if ( justTheType ) return;
        
        for ( QName aspectQname : this.getAspectsSet() ) {
            // reflection is too slow?
            String cappedAspectName =
                    Utils.capitalize( aspectQname.getLocalName() );
            EmsScriptNode node = getNodeAtAtime( dateTime );
            SpecEnum aspect = aspect2Key.get( cappedAspectName );
            if (aspect == null) {
                
            } else {
                switch (aspect) {
                    case Association:
                        addAssociationJSON( json, node, filter, dateTime );
                        break;
                    case Binding:
                        addBindingJSON( json, node, filter, dateTime );
                        break;
                    case Characterizes:
                        addCharacterizesJSON( json, node, filter, dateTime );
                        break;
                    case Conform:
                        addConformJSON( json, node, filter, dateTime );
                        break;
                    case Connector:
                        addConnectorJSON( json, node, filter, dateTime );
                        break;
                    case Constraint:
                        addConstraintJSON( json, node, filter, dateTime );
                        break;
                    case Dependency:
                        addDependencyJSON( json, node, filter, dateTime );
                        break;
                    case DirectedRelationship:
                        addDirectedRelationshipJSON( json, node, filter, dateTime );
                        break;
                    case Duration:
                        addDurationJSON( json, node, filter, dateTime );
                        break;
                    case DurationInterval:
                        addDurationIntervalJSON( json, node, filter, dateTime );
                        break;
                    case ElementValue:
                        addElementValueJSON( json, node, filter, dateTime );
                        break;
                    case LiteralSet:
                        addLiteralSetJSON( json, node, filter, dateTime );
                        break;
                    case Expose:
                        addExposeJSON( json, node, filter, dateTime );
                        break;
                    case Expression:
                        addExpressionJSON( json, node, filter, dateTime );
                        break;
                    case Generalization:
                        addGeneralizationJSON( json, node, filter, dateTime );
                        break;
                    case InstanceSpecification:
                        addInstanceSpecificationJSON( json, node, filter, dateTime );
                        break;
                    case InstanceValue:
                        addInstanceValueJSON( json, node, filter, dateTime );
                        break;
                    case Interval:
                        addIntervalJSON( json, node, filter, dateTime );
                        break;
                    case LiteralBoolean:
                        addLiteralBooleanJSON( json, node, filter, dateTime );
                        break;
                    case LiteralInteger:
                        addLiteralIntegerJSON( json, node, filter, dateTime );
                        break;
                    case LiteralNull:
                        addLiteralNullJSON( json, node, filter, dateTime );
                        break;
                    case LiteralReal:
                        addLiteralRealJSON( json, node, filter, dateTime );
                        break;
                    case LiteralString:
                        addLiteralStringJSON( json, node, filter, dateTime );
                        break;
                    case LiteralUnlimitedNatural:
                        addLiteralUnlimitedNaturalJSON( json, node, filter, dateTime );
                        break;
                    case MagicDrawData:
                        addMagicDrawDataJSON( json, node, filter, dateTime );
                        break;
                    case OpaqueExpression:
                        addOpaqueExpressionJSON( json, node, filter, dateTime );
                        break;
                    case Operation:
                        addOperationJSON( json, node, filter, dateTime );
                        break;
                    case Package:
                        addPackageJSON( json, node, filter, dateTime );
                        break;
                    case Parameter:
                        addParameterJSON( json, node, filter, dateTime );
                        break;
                    case Product:
                        addProductJSON( json, node, filter, dateTime );
                        break;
                    case Property:
                        addPropertyJSON( json, node, filter, dateTime );
                        break;
                    case StringExpression:
                        addStringExpressionJSON( json, node, filter, dateTime );
                        break;
                    case Succession:
                        addSuccessionJSON( json, node, filter, dateTime );
                        break;
                    case TimeExpression:
                        addTimeExpressionJSON( json, node, filter, dateTime );
                        break;
                    case TimeInterval:
                        addTimeIntervalJSON( json, node, filter, dateTime );
                        break;
                    case ValueSpecification:
                        addValueSpecificationJSON( json, node, filter, dateTime );
                        break;
                    case View:
                        addViewJSON( json, node, filter, dateTime );
                        break;
                    default:
                            
                } // end switch
            } // end if aspect == null
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
            putInJson( element, Acm.JSON_READ, getIsoTime( new Date( readTime ) ), filter );
            //element.put( Acm.JSON_READ, getIsoTime( new Date( readTime ) ) );
        }

        // fix the artifact urls
        String elementString = element.toString();
        elementString = fixArtifactUrls( elementString, true );
        element = new JSONObject( elementString );

        return element;
    }

    public JSONObject toSimpleJSONObject( Date dateTime ) throws JSONException {
        JSONObject element = new JSONObject();
        element.put( "sysmlid", getSysmlId() );
        if ( dateTime == null ) {
            element.put( "name", getSysmlName() );
        } else {
            element.put( "name", getSysmlName( dateTime ) );            
        }
        JSONObject specializationJSON = new JSONObject();
        addSpecializationJSON( specializationJSON, null, dateTime, true );
        if ( specializationJSON.length() > 0 ) {
            element.put( Acm.JSON_SPECIALIZATION, specializationJSON );
        }
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
                if ( dateTime != null || workspace != null ) {
                    targetRef =
                            NodeUtil.getNodeRefAtTime( targetRef, workspace,
                                                       dateTime );
                }
                if ( targetRef == null ) {
                    String msg =
                            "Error! Target of association " + aref
                                    + " did not exist in workspace "
                                    + WorkspaceNode.getName(workspace) + " at "
                                    + dateTime + ".\n";
                    if ( getResponse() == null || getStatus() == null ) {
                        Debug.error( false, msg );
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
        
        // If it is a node from the version store, then we cant trace up the parents
        // to find the site, so must use its owner till we have a non version node:
        VersionService vs = getServices().getVersionService();
        EmsScriptNode owner = this;
        while (owner != null && vs.isAVersion( owner.getNodeRef() )) {
            owner = owner.getOwningParent( null );
        }
        
        EmsScriptNode parent = owner != null ? owner : this;
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

    public EmsScriptNode getProjectNode() {
        EmsScriptNode parent = this;
        EmsScriptNode sites = null;
        EmsScriptNode projectPkg = null;
        EmsScriptNode models = null;
        EmsScriptNode oldparent = null;
        Set<EmsScriptNode> seen = new HashSet<EmsScriptNode>(); 
        while ( parent != null && parent.getSysmlId() != null &&
                !seen.contains( parent ) ) {
            if ( models == null && parent.getName().equals( "Models" ) ) {
                models = parent;
                projectPkg = oldparent;
            } else if ( models != null && sites == null && 
                        parent.getName().equals( "Sites" ) ) {
                sites = parent;
            } else if ( sites != null && parent.isWorkspaceTop() ) {
                EmsScriptNode projectNode = projectPkg.getReifiedNode();
                if ( Debug.isOn() ) Debug.outln( getName()
                                                 + ".getProjectNode() = "
                                                 + projectNode.getName() );
                return projectNode;
            }
            seen.add(parent);
            oldparent = parent;
            parent = parent.getParent();
        }
        if ( seen.contains(parent) ) {
            String msg ="ERROR! recursive parent hierarchy detected for " + parent.getName() + " having visited " + seen + ".\n";
            if ( getResponse() == null || getStatus() == null ) {
                Debug.error( msg );
            } else {
                getResponse().append( msg );
                getStatus().setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg );
            }
        }
        return projectPkg;
    }
    
    public String getProjectId() {
        EmsScriptNode projectNode = getProjectNode();
        if (projectNode == null || projectNode.getSysmlId() == null) {
            return "null";
        }
        return projectNode.getSysmlId().replace("_pkg", "");
    }
    
    private EmsScriptNode convertIdToEmsScriptNode( String valueId,
                                                    boolean ignoreWorkspace,
                                                    WorkspaceNode workspace,
                                                    Date dateTime ) {
        return convertIdToEmsScriptNode( valueId, ignoreWorkspace,
                                         workspace, dateTime,
                                         services, response, status );
    }

    public static EmsScriptNode
            convertIdToEmsScriptNode( String valueId, boolean ignoreWorkspace,
                                      WorkspaceNode workspace,
                                      Date dateTime, ServiceRegistry services,
                                      StringBuffer response, Status status ) {
        ArrayList< NodeRef > refs =
                NodeUtil.findNodeRefsById( valueId, ignoreWorkspace,
                                           workspace, dateTime,
                                           services, false, false );
        
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
            // It is important we ignore the workspace when getting the property, so we make sure
            // to update this property when needed.  Otherwise, property may have a noderef in 
            // a parent workspace, and this wont detect it; however, all the getProperty() will look
            // for the correct workspace node, so perhaps this is overkill::
            ArrayList< Serializable > oldValues =
                    (ArrayList< Serializable >)getProperty( acmProperty, true, null, false, true );
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
                                               boolean ignoreWorkspace,
                                               WorkspaceNode workspace,
                                               Date dateTime ) {
        return convertIdToEmsScriptNode( id, ignoreWorkspace, workspace,
                                         dateTime, services,
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
    public ArrayList< Serializable >
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

    public ArrayList< Serializable >
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
                            convertIdToEmsScriptNode( sysmlId, false, workspace,
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
                        convertIdToEmsScriptNode( sysmlId, false, workspace,
                                                  dateTime );
                if ( node != null ) {
                    property = node.getNodeRef();
                } else if ( !Utils.isNullOrEmpty( sysmlId ) ) {
                    String msg =
                            "Error! Could not find element for sysml id = "
                                    + sysmlId + ".\n";
                    if ( getResponse() == null || getStatus() == null ) {
                        Debug.error( false, msg );
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
                Debug.error( false, msg );
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
                    findNodeRefByType( filename, SearchType.CM_NAME.prefix,
                                       getWorkspace(), null, false );
            if ( nodeRef != null ) {
                // this should grab whatever is the latest versions purl - so
                // fine for snapshots
                EmsScriptNode node = new EmsScriptNode( nodeRef, getServices() );
                NodeRef versionedNodeRef = node.getHeadVersion().getVersionedNodeRef();
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
                               WorkspaceNode workspace, Date dateTime, boolean findDeleted ) {
        return NodeUtil.findNodeRefByType( name, type, false, workspace, dateTime,
                                           true, services, findDeleted );
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
        return equals( obj, false );
    }
    
    /**
     * Check to see if the nodes are the same or (if tryCurrentVersions is true)
     * if their currentVersions are the same.
     * 
     * @param obj
     * @param tryCurrentVersions
     * @return true iff equal
     */
    public boolean equals( Object obj, boolean tryCurrentVersions ) {

        if ( !( obj instanceof EmsScriptNode ) ) return false;
        EmsScriptNode that = (EmsScriptNode)obj;
        boolean same = this.nodeRef.equals( that.nodeRef );
        if ( same || !tryCurrentVersions ) return same;
        
        // See if they are different versions of the same node.
        VersionService vs = getServices().getVersionService();
        boolean isThisV = vs.isAVersion( this.nodeRef );
        boolean isThatV = vs.isAVersion( that.nodeRef );
        if ( !isThisV && !isThatV ) return same;
        NodeRef thisCurrent = this.getHeadVersion().getVersionedNodeRef();
        NodeRef thatCurrent = that.getHeadVersion().getVersionedNodeRef();
        if ( thisCurrent == thatCurrent ) return true;
        if ( thisCurrent == null || thatCurrent == null ) return false;
        return thisCurrent.equals( thatCurrent );
    }

    /**
     * Override exists for EmsScriptNodes
     *
     * @see org.alfresco.repo.jscript.ScriptNode#exists()
     */
    @Override
    public boolean exists() {
        return exists( false );
    }
    public boolean exists(boolean includeDeleted) {
        // REVIEW -- TODO -- Will overriding this cause problems in ScriptNode?
        if ( !scriptNodeExists() ) return false;
        if ( !includeDeleted && hasAspect( "ems:Deleted" ) ) {
            return false;
        }
        return true;
    }
    
    public boolean scriptNodeExists() {
        return super.exists();
    }

    public boolean isDeleted() {
        if (super.exists()) {
            return hasAspect( "ems:Deleted" );
        }
        // may seem counterintuitive, but if it doesn't exist, it isn't deleted
        return false;
    }
    
    /** 
     * this is a soft delete that is used to internally track "deleted" elements
     */
    public void delete() {
        if (!isDeleted()) {
            addAspect( "ems:Deleted" );
        }
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
                if (ref != null) {
                    WorkspaceNode ws = new WorkspaceNode( ref, getServices() );
                    workspace = ws;
                }
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

    public void setWorkspace( WorkspaceNode workspace ) {
        setWorkspace( workspace, null );
    }

    // Warning: this may not work if the cm:name is not unique!
    public NodeRef findSourceInParentWorkspace() {
        if ( getWorkspace() == null ) return null;
        WorkspaceNode parentWs = getParentWorkspace();
//        NodeRef r = NodeUtil.findNodeRefById( getSysmlId(), false, parentWs,
//                                              null, getServices(), false );
        ArrayList< NodeRef > refs = NodeUtil.findNodeRefsById( getSysmlId(), false, parentWs,
                                               null, getServices(), false, false );
        NodeRef r = null;
        for ( NodeRef ref : refs ) {
            EmsScriptNode node = new EmsScriptNode( ref, getServices() );
            EmsScriptNode parent1 = getParent();
            EmsScriptNode parent2 = node.getParent();
            boolean failed = false;
            while ( NodeUtil.exists( parent1 ) && NodeUtil.exists( parent2 ) &&
                    !parent1.isWorkspaceTop() && !parent2.isWorkspaceTop() ) {
//                if ( parent1 == parent2 || ( grandParent != null && gp != null && grandParent.getName().equals( gp.getName() ) ) ) {
                if ( !parent1.getName().equals( parent2.getName() ) ) {
                    failed = true;
                    break;
                } else {
                    if ( parent1.equals( parent2 ) ) break;
                }
                parent1 = parent1.getParent();
                parent2 = parent2.getParent();
            }
            if ( !failed && ( ( parent1 == null ) == ( parent2 == null ) )
                 && ( parent1.isWorkspaceTop() == parent2.isWorkspaceTop() ) ) {
               r = ref;
               break;
            }
        }
        return r;
    }

//    public NodeRef findSourceInParentWorkspace() {
//        EmsScriptNode node = this;
//        // make sure the folder's parent is replicated
//        EmsScriptNode parent = node.getParent();
//    
//        if ( parent == null || parent.isWorkspaceTop() ) {
//            parent = this; // put in the workspace
//        }
//        String parentName = parent != null && parent.exists() ? parent.getName() : null;
//    
//        // Get the parent in this workspace. In case there are multiple nodes
//        // with the same cm:name, use the grandparent to disambiguate where it
//        // should be.
//        if ( parent != null && parent.exists() && !this.equals( parent.getWorkspace() ) ) {
//            EmsScriptNode grandParent = parent.getParent();
//            ArrayList< NodeRef > arr = NodeUtil.findNodeRefsByType( parentName, SearchType.CM_NAME.prefix, false, false, this, null, false, true, getServices(), false );
//            for ( NodeRef ref : arr ) {
//                EmsScriptNode p = new EmsScriptNode( ref, getServices() );
//                EmsScriptNode gp = p.getParent();
//                if ( grandParent == gp || ( grandParent != null && gp != null && grandParent.getName().equals( gp.getName() ) ) ) {
//                    parent = p;
//                    break;
//                }
//            }
//            
//            if ( !this.equals( parent.getWorkspace() ) ) {
//                parent = replicateWithParentFolders( parent );
//            }
//        } else if ( parent == null || !parent.exists() ) {
//            Debug.error("Error! Bad parent when replicating folder chain! " + parent );
//        }
//    }
    
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
        if ( source == null ) {
            source = findSourceInParentWorkspace();
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
       // if( ws == null)
       //   return null;
        return ws.getParentWorkspace();
    }
    // delete later
    public WorkspaceNode getSourceWorkspace() {
        WorkspaceNode ws = getWorkspace();
       if( ws == null)
            return null;
        return ws.getSourceWorkspace();
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
        // EmsScriptNode node =  parent.createSysmlNode( getName(), type, modStatus, workspace );
        
        if ( node == null ) {
            Debug.error( "Could not create node in parent " + parent.getName() );
            return null;
        }

        // add missing aspects
        NodeService nodeService = getServices().getNodeService();
        Set< QName > myAspects = nodeService.getAspects( getNodeRef() );
        for ( QName qName : myAspects ) {
            if ( qName == null ) continue;
            node.createOrUpdateAspect( qName.toString() );
        }

        // copy properties except those of a site.
        Map< QName, Serializable > properties =
                nodeService.getProperties( getNodeRef() );
        if ( isSiteOrSites ) {
            properties.remove( createQName( "st:sitePreset" ) );
            properties.remove( createQName( "sys:undeletable" ) );
        }
        nodeService.setProperties( node.getNodeRef(), properties );
        
        // THIS MUST BE CALLED AFTER setProperties()!
        if ( parent.getWorkspace() != null) {
            node.setWorkspace( parent.getWorkspace(), this.getNodeRef() );
        }
        
        // update ems:owner
        if ( isModelElement() ) {
            // everything is created in a reified package, so need to make
            // relations to the reified node rather than the package
            EmsScriptNode reifiedNode = node.setOwnerToReifiedNode( parent, parent.getWorkspace() );
            if ( reifiedNode == null ) {
                // TODO error handling
            }
        }

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


        for ( String aspect : diff.getRemovedAspects(getSysmlId()) ) {
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

        for ( String aspect : diff.getAddedAspects(getSysmlId()) ) {
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
                diff.getRemovedProperties().get(getSysmlId());
        if ( removedProps != null )
        for ( Entry< String, Object > e : removedProps.entrySet() ) {
            if ( workspaceMetaProperties.contains( e.getKey() ) ) continue;
            Object myVal = getProperty( e.getKey() );
            if ( myVal != null ) {
                if ( removeProperty( e.getKey() ) ) changed = true;
            }
        }
        Map< String,  Pair< Object, Object > > propChanges =
                diff.getPropertyChanges().get(getSysmlId());
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
            } else if (!relationships.contains(ref )) {
                    relationships.add( ref );
            }
            setProperty( acmProperty, relationships );
        } else {
            log( "no write permissions to append " + acmProperty + " to " + id
                 + "\n" );
        }
    }

    // TODO -- It would be nice to return a boolean here. Same goes to many of
    // the callers of this method.
    public void removeFromPropertyNodeRefs( String acmProperty, NodeRef ref ) {
        if ( checkPermissions( PermissionService.WRITE, response, status ) ) {
            ArrayList< NodeRef > relationships =
                    getPropertyNodeRefs( acmProperty );
            if ( !Utils.isNullOrEmpty( relationships ) ) {
                relationships.remove( ref );
                setProperty( acmProperty, relationships );
            }
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

            if ( oldParentReifiedNode != null ) {
                oldParentReifiedNode.removeFromPropertyNodeRefs( "ems:ownedChildren",
                                                                 this.getNodeRef() );
            }

            EmsScriptNode newParent =
                    new EmsScriptNode( destination.getNodeRef(), services,
                                       response );
            if (newParent != null) {
                setOwnerToReifiedNode( newParent, newParent.getWorkspace() );
            }

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
        return getPropertyNodeRefs(acmProperty, false, null, false, false);
    }
    
    public ArrayList< NodeRef > getPropertyNodeRefs( String acmProperty, boolean ignoreWorkspace,
                                                     Date dateTime, boolean findDeleted,
                                                     boolean skipNodeRefCheck) {
        Object o = getProperty( acmProperty, ignoreWorkspace, dateTime, findDeleted, skipNodeRefCheck );
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
            refs = Utils.asList( (Collection< ? >)o, NodeRef.class, false );
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

    public EmsScriptNode getPropertyElement( String acmProperty ) {
        Object e = getProperty( acmProperty );
        if ( e instanceof NodeRef ) {
            return new EmsScriptNode( (NodeRef)e, getServices() );
        } else if ( e == null ) {
        } else {
            Debug.error(true, false, "ERROR! Getting a property as a noderef!");
        }
        return null;
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

    private Set<QName> getAllAspectsAndInherited() {
        Set<QName> aspects = new LinkedHashSet< QName >();
        aspects.addAll( getAspectsSet() );
        ArrayList<QName> queue = new ArrayList< QName >( aspects );
        DictionaryService ds = getServices().getDictionaryService();
        while ( !queue.isEmpty() ) {
            QName a = queue.get(0);
            queue.remove( 0 );
            AspectDefinition aspect = ds.getAspect( a );
            QName p = aspect.getParentName();
            if ( p != null && !aspects.contains( p ) ) {
                aspects.add( p );
                queue.add( p );
            }
        }
        return aspects;
    }

    public boolean hasOrInheritsAspect( String aspectName ) {
        if ( hasAspect( aspectName ) ) return true;
        QName qName = NodeUtil.createQName( aspectName );
        return getAllAspectsAndInherited().contains( qName );
//        QName qn = NodeUtil.createQName( aspectName );
//        if ( getAspectsSet().contains( qn ) ) return true;
//
//        DictionaryService ds = getServices().getDictionaryService();
//        AspectDefinition aspect = ds.getAspect( qn );
//
//        //aspect.
//
//        NodeService ns = NodeUtil.getServices().getNodeService();
//        ns.ge
//        if ( ns.hasAspect( getNodeRef(), NodeUtil.createQName( aspectName ) ) ) {
//
//        }
//        return false;
    }
    
    /**
     * Changes the aspect of the node to the one specified, taking care
     * to save off and re-apply properties from current aspect if 
     * downgrading.  Handles downgrading to a Element, by removing all
     * the needed aspects.  Also removing old sysml aspects if changing
     * the sysml aspect.
     *      
     * @param aspectName The aspect to change to
     */
    private boolean changeAspect(String aspectName) {
        
        Set<QName> aspects = new LinkedHashSet< QName >();
        boolean retVal = false;
        Map<String,Object> oldProps = null;
        DictionaryService dServ = services.getDictionaryService();
        AspectDefinition aspectDef;
        boolean saveProps = false;
        
        if (aspectName == null) {
            return false;
        }
        
        QName qName = NodeUtil.createQName( aspectName );
        
        // If downgrading to an Element, then need to remove
        // all aspects without saving any properties or adding
        // any aspects:
        if (aspectName.equals(Acm.ACM_ELEMENT)) {
            for (String aspect : Acm.ACM_ASPECTS) {
                if (hasAspect(aspect)) {
                    boolean myRetVal = removeAspect(aspect);
                    retVal = retVal || myRetVal;
                }
            }
            
            return retVal;
        }
        
        // Get all the aspects for this node, find all of their parents, and see if
        // the new aspect is any of the parents.  
        aspects.addAll( getAspectsSet() );
        ArrayList<QName> queue = new ArrayList< QName >( aspects );
        QName name;
        QName parentQName;
        while(!queue.isEmpty()) {
            name = queue.get(0);
            queue.remove(0);
            aspectDef = dServ.getAspect(name);
            parentQName = aspectDef.getParentName();
            if (parentQName != null) {
                if (parentQName.equals( qName )) {
                    saveProps = true;
                    break;
                }
                if (!queue.contains(parentQName)) {
                    queue.add(parentQName);
                }
            }
        }
        
        // If changing aspects to a parent aspect (ie downgrading), then we must save off the
        // properties before removing the current aspect, and then re-apply them:        
        if (saveProps) {
            oldProps = getProperties();
        }
        // No need to go any further if it already has the aspect and the new aspect is not
        // a parent of the current aspect:
        else if (hasAspect(aspectName)){
            return false;
        }
        
        // Remove all the existing sysml aspects if the aspect is not a parent of the new aspect,
        // and it is a sysml aspect:
        List<String> sysmlAspects = Arrays.asList(Acm.ACM_ASPECTS);
        if (sysmlAspects.contains( aspectName)) {
            
            Set<QName> parentAspectNames = new LinkedHashSet< QName >();
            parentAspectNames.add(qName);
            name = qName; // The new aspect QName
            while (name != null) {
                aspectDef = dServ.getAspect(name);
                parentQName = aspectDef.getParentName();
                if (parentQName != null) {
                    parentAspectNames.add(parentQName);
                }
                name = parentQName;
            }
                    
            for (String aspect : Acm.ACM_ASPECTS) { 
                if (hasAspect(aspect) && !parentAspectNames.contains(NodeUtil.createQName( aspect ))) {
                    boolean removeVal = removeAspect(aspect);
                    retVal = retVal || removeVal;
                }
            }
        }
        
        // Apply the new aspect if needed:
        if (!hasAspect(aspectName)) {
            retVal = addAspect(aspectName);
        }
        
        // Add the saved properties if needed:
        if (oldProps != null) {
            aspectDef = dServ.getAspect(qName);
            Set<QName> aspectProps = aspectDef.getProperties().keySet();

            // Only add the properties that are valid for the new aspect:
            String propName;
            QName propQName;
            for (Entry<String,Object> entry : oldProps.entrySet()) {
                propName = entry.getKey();
                propQName = NodeUtil.createQName(propName);
                
                if (aspectProps.contains(propQName)) {
                    setProperty(propName, (Serializable)entry.getValue());
                }
            }
        }
        
        return retVal;
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

    private String getSysmlIdOfProperty( String propertyName ) {
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
        putInJson( json, Acm.JSON_IS_SITE, node.getProperty( Acm.ACM_IS_SITE ),
                   filter );   
    }

    protected void addViewpointJSON( JSONObject json, Set< String > filter,
                                     Date dateTime ) throws JSONException {
        json.put( "method",
                  this.getSysmlIdOfProperty( "sysml:method" ) );
    }

    protected void addViewJSON( JSONObject json, EmsScriptNode node,
                                Set< String > filter, Date dateTime )
                                        throws JSONException {
        String property;
        property = (String) node.getProperty("view2:contains");
        if ( expressionStuff && ( property == null || property.length() <= 0 ) ) {
            json.put( "contains", getView().getContainsJson(true) );
            json.put( "displayedElements", getView().getDisplayedElements() );
            json.put( "allowedElements", getView().getDisplayedElements() );
            json.put( "childrenViews", getView().getChildViews() );
        } else {
            if (!Utils.isNullOrEmpty(property)) {
                putInJson( json, "contains", new JSONArray( property ), filter );
            }
            property = (String) node.getProperty("view2:displayedElements");
            if (!Utils.isNullOrEmpty(property)) {
                putInJson( json, "displayedElements", new JSONArray( property ), filter );
            }
            property = (String) node.getProperty("view2:allowedElements");
            if (!Utils.isNullOrEmpty(property)) {
                putInJson( json, "allowedElements", new JSONArray( property ), filter );
            }
            property = (String) node.getProperty("view2:childrenViews");
            if (!Utils.isNullOrEmpty(property)) {
                putInJson( json, "childrenViews", new JSONArray( property ), filter );
            }
        }
        // TODO: Snapshots?
        String id = node.getSysmlIdOfProperty( Acm.ACM_CONTENTS );
        if (id != null) {
            putInJson( json, Acm.JSON_CONTENTS, id, filter );
        }
    }

    protected void addProductJSON( JSONObject json, EmsScriptNode node,
                                   Set< String > filter, Date dateTime )
                                           throws JSONException {
        JSONArray jarr = new JSONArray();
        String v2v = (String)node.getProperty( "view2:view2view" );
        if ( !Utils.isNullOrEmpty( v2v ) ) {
            jarr.put( v2v );
            putInJson( json, "view2view",
                       new JSONArray( (String)node.getProperty( "view2:view2view" ) ),
                       filter );
        }
        jarr = new JSONArray();
        String noSections = (String)node.getProperty( "view2:noSections" );
        if ( !Utils.isNullOrEmpty( noSections ) ) {
            jarr.put( noSections );
            putInJson( json, "noSections",
                       new JSONArray( (String)node.getProperty( "view2:noSections" ) ),
                       filter );
        }
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
        NodeRef propertyType = (NodeRef) node.getProperty( "sysml:propertyType" );
        if ( propertyType != null ) {
            EmsScriptNode propertyTypeNode = new EmsScriptNode(propertyType, services, response);
            putInJson( json, "propertyType", propertyTypeNode.getSysmlId(), filter);
        }
        
        putInJson( json, Acm.JSON_LOWER, 
                   addInternalJSON( node.getProperty(Acm.ACM_LOWER), dateTime ), 
                   filter );
        
        putInJson( json, Acm.JSON_UPPER, 
                   addInternalJSON( node.getProperty(Acm.ACM_LOWER), dateTime ), 
                   filter );
    }

    protected
            void
            addDirectedRelationshipJSON( JSONObject json, EmsScriptNode node,
                                         Set< String > filter, Date dateTime )
                                                                              throws JSONException {
        String id;

        id = node.getSysmlIdOfProperty( "sysml:source" );
        if ( id != null ) {
            putInJson( json, "source", id, filter );
        }

        id = node.getSysmlIdOfProperty( "sysml:target" );
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
                node.getSysmlIdOfProperty( "sysml:valueExpression" );
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

        id = node.getSysmlIdOfProperty( "sysml:durationMax" );
        if ( id != null ) {
            putInJson( json, "max", id, filter );
        }

        id = node.getSysmlIdOfProperty( "sysml:durationMin" );
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

//        String elementId =
//                node.getSysmlIdOfProperty( "sysml:element", dateTime );
//        if ( elementId == null ) {
        String elementId =
                node.getSysmlIdOfProperty( "sysml:elementValueOfElement" );
        //        }
        if ( elementId != null ) {
            putInJson( json, "element", elementId, filter );
        }
    }
    
    protected void addLiteralSetJSON( JSONObject json, EmsScriptNode node,
                                        Set< String > filter, Date dateTime )
                                                              throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );
        
        putInJson( json, Acm.JSON_SET, 
                   addInternalJSON( node.getProperty(Acm.ACM_SET), dateTime ), 
                   filter );

        putInJson( json, Acm.JSON_SET_OPERAND, 
                   addInternalJSON( node.getProperty(Acm.ACM_SET_OPERAND), dateTime ), 
                   filter );
        
    }


    protected void addExpressionJSON( JSONObject json, EmsScriptNode node,
                                      Set< String > filter, Date dateTime )
                                                          throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );

        ArrayList< NodeRef > nodeRefs =
                (ArrayList< NodeRef >)node.getProperty( "sysml:operand" );
        if (nodeRefs == null) {
            return;
        }
        
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
        putInJson( json, "display", getExpressionDisplayString(), filter );
        if ( evaluatingExpressions ) {
            putInJson( json, "evaluation", getExpressionEvaluation(), filter );
        }
    }

    public Object getExpressionEvaluation() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getExpressionDisplayString() {
        // TODO Auto-generated method stub
        return null;
    }

    protected
            void
            addInstanceValueJSON( JSONObject json, EmsScriptNode node,
                                  Set< String > filter, Date dateTime )
                                                                       throws JSONException {
        addValueSpecificationJSON( json, node, filter, dateTime );

        String id = node.getSysmlIdOfProperty( "sysml:instance" );
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

    protected void addMagicDrawDataJSON( JSONObject json,
                                    EmsScriptNode node,
                                    Set< String > filter, Date dateTime )
                                                                         throws JSONException {
        String data = (String) node.getProperty( Acm.ACM_MD_DATA);
        if (data != null) {
            putInJson( json, Acm.JSON_MD_DATA, data, filter );
        }
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

        id = node.getSysmlIdOfProperty( "sysml:timeIntervalMax" );
        if ( id != null ) {
            putInJson( json, "max", id, filter );
        }

        id = node.getSysmlIdOfProperty( "sysml:timeIntervalMin" );
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
                node.getSysmlIdOfProperty( "sysml:operationExpression" );
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
                node.getSysmlIdOfProperty( "sysml:constraintSpecification" );
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
                node.getSysmlIdOfProperty( "sysml:parameterDefaultValue" );
        if ( id != null ) {
            putInJson( json, "defaultValue", id, filter );
        }
    }

    protected void addConnectorJSON( JSONObject json, EmsScriptNode node,
                                     Set< String > filter, Date dateTime )
                                                                   throws JSONException {
        
        addDirectedRelationshipJSON(json, node, filter, dateTime);
        
        ArrayList< NodeRef > nodeRefsSource =
                (ArrayList< NodeRef >)node.getProperty( Acm.ACM_SOURCE_PATH );
        JSONArray sourceIds = addNodeRefIdsJSON( nodeRefsSource, dateTime );
        putInJson( json, Acm.JSON_SOURCE_PATH, sourceIds, filter );
        
        ArrayList< NodeRef > nodeRefsTarget =
                (ArrayList< NodeRef >)node.getProperty( Acm.ACM_TARGET_PATH );
        JSONArray targetIds = addNodeRefIdsJSON( nodeRefsTarget, dateTime );
        putInJson( json, Acm.JSON_TARGET_PATH, targetIds, filter );
        
        String kind = (String) node.getProperty( Acm.ACM_CONNECTOR_KIND );
        if ( kind != null ) {
            putInJson( json, Acm.JSON_CONNECTOR_KIND, kind, filter );
        }   
        
        putInJson( json, Acm.JSON_CONNECTOR_VALUE, 
                   addInternalJSON( node.getProperty(Acm.ACM_CONNECTOR_VALUE), dateTime ), 
                   filter );
        
        putInJson( json, Acm.JSON_TARGET_LOWER, 
                   addInternalJSON( node.getProperty(Acm.ACM_TARGET_LOWER), dateTime ), 
                   filter );
        
        putInJson( json, Acm.JSON_TARGET_UPPER, 
                   addInternalJSON( node.getProperty(Acm.ACM_TARGET_UPPER), dateTime ), 
                   filter );
        
        putInJson( json, Acm.JSON_SOURCE_LOWER, 
                   addInternalJSON( node.getProperty(Acm.ACM_SOURCE_LOWER), dateTime ), 
                   filter );
        
        putInJson( json, Acm.JSON_SOURCE_UPPER, 
                   addInternalJSON( node.getProperty(Acm.ACM_SOURCE_UPPER), dateTime ), 
                   filter );
    
    }
    
    protected void addAssociationJSON( JSONObject json, EmsScriptNode node,
                                     Set< String > filter, Date dateTime )
                                                                   throws JSONException {
        
        addDirectedRelationshipJSON(json, node, filter, dateTime);

        ArrayList< NodeRef > nodeRefsOwnedEnd =
                (ArrayList< NodeRef >)node.getProperty( Acm.ACM_OWNED_END );
        JSONArray ownedEndIds = addNodeRefIdsJSON( nodeRefsOwnedEnd, dateTime );
        putInJson( json, Acm.JSON_OWNED_END, ownedEndIds, filter );
        
        putInJson( json, Acm.JSON_SOURCE_AGGREGATION, 
                   node.getProperty( Acm.ACM_SOURCE_AGGREGATION), filter );
        putInJson( json, Acm.JSON_TARGET_AGGREGATION, 
                   node.getProperty( Acm.ACM_TARGET_AGGREGATION ), filter );
     
    }
    
    protected void addCharacterizesJSON( JSONObject json, EmsScriptNode node,
                                       Set< String > filter, Date dateTime )
                                                                     throws JSONException {
          
          addDirectedRelationshipJSON(json, node, filter, dateTime);          
    }
    
    protected void addSuccessionJSON( JSONObject json, EmsScriptNode node,
                                         Set< String > filter, Date dateTime )
                                                                       throws JSONException {
            
         addConnectorJSON(json, node, filter, dateTime);   
    }
    
    protected void addBindingJSON( JSONObject json, EmsScriptNode node,
                                      Set< String > filter, Date dateTime )
                                                                    throws JSONException {
         
         addConnectorJSON(json, node, filter, dateTime);   
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
        return getNamesOrIdsImpl(nodes, true);
    }
    
    public static List< String > getSysmlIds( List< EmsScriptNode > nodes ) {
        return getNamesOrIdsImpl(nodes, false);
    }
    
    private static List< String > getNamesOrIdsImpl( List< EmsScriptNode > nodes, boolean getName ) {
        
        List< String > names = new ArrayList< String >();
        for ( EmsScriptNode node : nodes ) {
            String name = getName ? node.getName() : node.getSysmlId();
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

    @Override
    public boolean removeAspect(String type) {
        if (hasAspect(type)) {
            return super.removeAspect( type );
        }
        return true;
    }

    public static boolean isModelElement( NodeRef ref ) {
        EmsScriptNode node = new EmsScriptNode( ref, NodeUtil.getServices() );
        return node.isModelElement();
    }

    public boolean isModelElement() {
        if ( getTypeShort().equals( "sysml:Element" ) ) {
            return true;
        }
        return false;
    }

    public boolean isProperty() {
        if ( hasOrInheritsAspect( "sysml:Property" ) ) return true;
        if ( getProperty(Acm.ACM_VALUE ) != null ) return true;
        return false;
    }

    public EmsScriptNode getOwningProperty() {
        if (Debug.isOn()) Debug.outln("getOwningProperty(" + this + ")");
        EmsScriptNode parent = this;
        while ( parent != null && !parent.isProperty() ) { 
            // TODO -- REVIEW -- Should we return null if parent is something
            // other than a property or value spec?!
            // What if it's an Operation?
            if (Debug.isOn()) Debug.outln("parent = " + parent );
            parent = parent.getUnreifiedParent( null );  // TODO -- REVIEW -- need timestamp??!!
        }
        if (Debug.isOn()) Debug.outln("returning " + parent );
        return parent;
    }

    public boolean isPropertyOwnedValueSpecification() {
        if ( hasOrInheritsAspect( "sysml:ValueSpecification" ) ) {
            EmsScriptNode parent = getOwningProperty();
            return parent != null && parent.isProperty();
        }
        return false;
    }
}
