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

package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.docbook.model.*;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.SnapshotArtifactsGenerationActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.util.TempFileProvider;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Handles (1) creating a snapshot or (2) generating snapshot artifacts -- PDF or HTML zip
 * @author lho
 *
 */
public class SnapshotPost extends AbstractJavaWebScript {
	protected String snapshotName;
	private boolean isSnapshotNode = false;	//determines whether we're working with a view/product or a snapshot node reference; true for snapshot node reference
	
    public SnapshotPost() {
        super();
    }
    
    public SnapshotPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * If there's JSON posted data, then it's a snapshot artifact generation.
     * Otherwise, it's a snapshot creation.
     */
    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        printHeader( req );
        clearCaches();

        WorkspaceNode workspace = getWorkspace( req );

        Map< String, Object > model = new HashMap< String, Object >();
        log( LogLevel.INFO,
             "Starting snapshot creation or snapshot artifact generation..." );
        try {
            JSONObject reqPostJson = (JSONObject)req.parseContent();
            if ( reqPostJson != null ) {
                log( LogLevel.INFO, "Generating snapshot artifact..." );
                SnapshotPost instance = new SnapshotPost( repository, services );
                JSONObject result = instance.saveAndStartAction( req, status );
                appendResponseStatusInfo( instance );

                status.setCode( responseStatus.getCode() );
                if ( result == null ) {
                    model.put( "res", response.toString() );
                } else {
                    model.put( "res", result );
                }
                printFooter();
                return model;
            } else {
                log( LogLevel.INFO, "Creating snapshot..." );
                String viewId = getViewId( req );
                EmsScriptNode snapshotNode = null;
                DateTime now = new DateTime();
                DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

                EmsScriptNode topview = findScriptNodeById( viewId, workspace, null, true );
                EmsScriptNode snapshotFolderNode =
                        getSnapshotFolderNode( topview );
                this.snapshotName = viewId + "_" + now.getMillis();

                if ( checkPermissions( snapshotFolderNode,
                                       PermissionService.WRITE ) ) {
                    snapshotNode =
                            createSnapshot( topview, viewId, snapshotName,
                                            req.getContextPath(),
                                            snapshotFolderNode );
                }

                if ( snapshotNode != null ) {
                    try {
                        JSONObject snapshoturl = new JSONObject();
                        snapshoturl.put( "id", snapshotName );
                        snapshoturl.put( "creator",
                                         AuthenticationUtil.getFullyAuthenticatedUser() );
                        snapshoturl.put( "created", fmt.print( now ) );
                        snapshoturl.put( "url", req.getContextPath()
                                                + "/service/snapshots/"
                                                + snapshotName );
                        model.put( "res", snapshoturl.toString( 4 ) );
                    } catch ( JSONException e ) {
                        e.printStackTrace();
                        log( LogLevel.ERROR,
                             "Error generating JSON for snapshot",
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                    }
                } else {
                    log( LogLevel.ERROR, "Error creating snapshot node",
                         HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                }
                status.setCode( responseStatus.getCode() );
                if ( status.getCode() != HttpServletResponse.SC_OK ) {
                    model.put( "res", response.toString() );
                }
            }
        } catch ( Exception ex ) {
            log( LogLevel.ERROR,
                 "Failed to create snapshot or snapshot artifact! "
                         + ex.getMessage() );
            ex.printStackTrace();
        }
        return model;
    }

    private void appendElement( DocumentElement elem1, DocumentElement elem2 ) {
        if ( elem2 == null ) return;
        StringBuffer sb = new StringBuffer();
        sb.append( getDocumentElementContent( elem1 ) );
        sb.append( System.getProperty( "line.separator" ) );
        sb.append( getDocumentElementContent( elem2 ) );
        setDocumentElementContent( elem1, sb.toString() );
    }

    private DBParagraph createDBParagraph( JSONObject obj ) {
        String srcType = (String)obj.opt( "sourceType" );
        String src = (String)obj.opt( "source" );
        String srcProp = (String)obj.opt( "sourceProperty" );

        DBParagraph p = new DBParagraph();
        p.setId( src );
        if ( srcType.compareTo( "reference" ) == 0 ) {
            WorkspaceNode workspace = null; // TODO -- REVIEW -- Do we need to
                                            // pass this in????!
            EmsScriptNode node =
                    findScriptNodeById( src, workspace, null, false );
            if ( srcProp.compareTo( "value" ) == 0 ) {
                List< NodeRef > nodeRefs =
                        (List< NodeRef >)node.getProperty( Acm.SYSML + srcProp );
                StringBuffer sb = new StringBuffer();
                int size = nodeRefs.size();
                if ( size > 0 ) sb.append( "<literallayout>" );
                for ( int i = 0; i < size; i++ ) {
                    NodeRef nodeRef = nodeRefs.get( i );
                    EmsScriptNode valueNode =
                            new EmsScriptNode( nodeRef, node.getServices() );
                    if ( valueNode != null ) {
                        String valueProp =
                                (String)node.getProperty( Acm.SYSML + "name" );
                        if ( valueProp != null && !valueProp.isEmpty() ) {
                            Object value =
                                    valueNode.getProperty( Acm.SYSML
                                                           + valueProp );
                            if ( value == null
                                 || ( value != null && value.toString()
                                                            .isEmpty() ) ) {
                                value = extractNodeValue( valueNode );
                                if ( value == null
                                     || ( value != null && value.toString()
                                                                .isEmpty() ) ) continue;
                            }

                            if ( value instanceof String ) sb.append( HtmlSanitize( (String)value ) );
                            else sb.append( value );
                        } else {

                            try {
                                Object valObj = extractNodeValue( valueNode );
                                if ( valObj == null ) continue;
                                if ( valObj instanceof String ) sb.append( HtmlSanitize( (String)valObj ) );
                                else sb.append( valObj );
                            } catch ( Exception ex ) {
                                log( LogLevel.WARNING,
                                     "Problem extract node value from "
                                             + node.toJSON() );
                            }
                        }
                    }
                }
                if ( size > 0 ) sb.append( "</literallayout>" );
                p.setText( sb.toString() );
            } else {
                String s = (String)node.getProperty( Acm.SYSML + srcProp );
                s = handleTransclusion( src, srcProp, s, null, 0 );
                s = HtmlSanitize( s );
                if ( s != null && !s.isEmpty() ) p.setText( "<literallayout>"
                                                            + s
                                                            + "</literallayout>" );
            }
        } else {
            if ( srcProp != null && !srcProp.isEmpty() ) {
                String s = (String)obj.opt( Acm.SYSML + srcProp );
                s = handleTransclusion( src, srcProp, s, null, 0 );
                s = HtmlSanitize( s );
                if ( s != null && !s.isEmpty() ) p.setText( "<literallayout>"
                                                            + s
                                                            + "</literallayout>" );
            } else p.setText( HtmlSanitize( (String)obj.opt( "text" ) ) );
        }
        if ( p.getText() == null || p.getText().toString().isEmpty() ) return null;

        return p;
    }
    
    private DocumentElement createDBSection( JSONObject obj ) throws JSONException {
        DBSection section = new DBSection();
        section.setTitle( (String)obj.opt( "name" ) );
        createDBSectionContainment( section, obj.getJSONArray( "contains" ) );
        return section;
    }

    private void createDBSectionContainment(DBSection section,
                                            JSONArray jsonContains) throws JSONException{
        for ( int i = 0; i < jsonContains.length(); i++ ) {
            JSONObject obj = jsonContains.getJSONObject( i );
            DocumentElement e = createElement( obj );
            if ( e != null ) section.addElement( e );
        }
	}

    private DocumentElement createDBText( JSONObject obj ) throws JSONException {
        DBText text = new DBText();
        text.setText( (String)obj.opt( "name" ) );
        return text;
    }

    private DBBook createDocBook( EmsScriptNode product ) {
        String title = (String)product.getProperty( Acm.ACM_NAME );
        DBBook docBook = new DBBook();
        docBook.setTitle( title );
        docBook.setTitlePageLegalNotice( "This Document has not been reviewed for export control. Not for distribution to or access by foreign persons." );
        docBook.setFooterLegalNotice( "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release." );
        String author =
                getUserProfile( product,
                                (String)product.getProperty( Acm.ACM_AUTHOR ) );
        docBook.setAuthor( Arrays.asList( author ) );
        return docBook;
    }
   
    private DocBookWrapper createDocBook( EmsScriptNode view, String viewId,
                                          String snapshotName, String contextPath,
                                          EmsScriptNode snapshotFolder ) throws Exception {
        log( LogLevel.INFO, "\ncreating DocBook snapshot for view Id: "
                            + viewId );
        log( LogLevel.INFO, "\ncreating DocBook snapshotname: " + snapshotName );

        if ( view == null ) {
            log( LogLevel.WARNING, "null [view] input parameter reference." );
            return null;
        }

        DocBookWrapper docBookMgr = new DocBookWrapper( this.snapshotName, snapshotFolder );
        try {
            DBBook docBook = createDocBook( view );
            docBook.setRemoveBlankPages( true );
            Collection< EmsScriptNode > v2v =
                    view.getView().getViewToViewPropertyViews( null, new Date() ); // TODO -- REVIEW -- Do we need to pass a workspace in??????!!!
            // String prodId = view.getId();
            for ( EmsScriptNode node : v2v ) {
                // String nodeId = node.getId();
                JSONObject jsnObj = node.toJSONObject( null );
                JSONObject spec = (JSONObject)jsnObj.get( "specialization" );
                if ( ( (String)spec.opt( "type" ) ).compareTo( "Product" ) == 0 ) continue;
                // if(nodeId.compareTo(prodId) == 0) continue;
                DocumentElement section =
                        (DocumentElement)emsScriptNodeToDBSection( node, true );
                docBook.addElement( section );
            }
            docBookMgr.setDBBook( docBook );
            docBookMgr.save();;
        } catch ( Exception ex ) {
            log( LogLevel.ERROR,
                 "\nFailed to create DBBook! " + ex.getStackTrace() );
            ex.printStackTrace();
            throw new Exception( "Failed to create DBBook!", ex );
        }
        return docBookMgr;
    }

    private DocumentElement createList( JSONObject obj ) throws JSONException {
        Boolean isOrdered = (Boolean)obj.opt( "ordered" );

        DBList list = new DBList();
        list.setOrdered( isOrdered );
        JSONArray listItemWrapper = obj.getJSONArray( "list" );
        for ( int i = 0; i < listItemWrapper.length(); i++ ) {
            JSONArray listItems = listItemWrapper.getJSONArray( i );
            DocumentElement docElem = null;
            for ( int j = 0; j < listItems.length(); j++ ) {
                JSONObject jsObj = listItems.getJSONObject( j );
                DocumentElement e = createElement( jsObj );
                if ( j > 0 ) {
                    appendElement( docElem, e );
                } else docElem = e;
            }
            if ( docElem != null ) list.addElement( docElem );
        }

        return (DocumentElement)list;
    }

    private DocumentElement createElement( JSONObject obj ) throws JSONException {
        DocumentElement e = null;
        switch ( getType( obj ) ) {
            case "Paragraph":
                e = createDBParagraph( obj );
                break;
            case "List":
                e = createList( obj );
                break;
            case "Table":
                e = createTable( obj );
                break;
            case "Image":
                e = createImage( obj );
                break;
            case "Section":
                e = createDBSection( obj );
                break;
            case "Element":
                e = createDBText( obj );
                break;
            default:
                log( LogLevel.WARNING, "Unexpected type: " + getType( obj ) );
                break;
        }
        return e;
    }
    
    private DocumentElement createImage( JSONObject obj ) {
        DBImage image = new DBImage();
        image.setId( getSymlId( obj ) );

        String id = (String)obj.opt( Acm.SYSMLID );
        EmsScriptNode imgNode = findScriptNodeById( id, null, null, false );  // TODO -- REVIEW -- Pass in workspace????!!!!
        if ( imgNode == null ) {
            // TODO error handling
            return image;
        } else {
            try {
                image.setTitle( (String)imgNode.getProperty( Acm.ACM_NAME ) );
                NodeRef nodeRef = imgNode.getNodeRef();
                ServiceRegistry services = imgNode.getServices();
                NodeService nodeService =
                        imgNode.getServices().getNodeService();

                String fileName =
                        (String)nodeService.getProperty( nodeRef,
                                                         ContentModel.PROP_NAME );
                fileName += ".svg";
                ResultSet resultSet =
                        NodeUtil.luceneSearch( "@name:" + fileName );
                if ( resultSet != null && resultSet.length() > 0 ) {
                    EmsScriptNode node =
                            new EmsScriptNode( resultSet.getNodeRef( 0 ),
                                               services );
                    saveImage( image, node );
                } else {
                    log( LogLevel.ERROR, fileName + " image file not found!" );
                }
            } catch ( Exception ex ) {;}

            return image;
        }
    }
    
    public EmsScriptNode createSnapshot( EmsScriptNode view, String viewId,
                                         WorkspaceNode workspace ) {
        this.snapshotName = viewId + "_" + System.currentTimeMillis();
        String contextPath = "alfresco/service/";
        EmsScriptNode viewNode = findScriptNodeById(viewId, workspace, null, true);
        EmsScriptNode snapshotFolder = getSnapshotFolderNode(viewNode);
        return createSnapshot(view, viewId, snapshotName, contextPath, snapshotFolder);
    }
    
    public EmsScriptNode createSnapshot( EmsScriptNode view, String viewId,
                                         String snapshotName,
                                         String contextPath,
                                         EmsScriptNode snapshotFolder ) {
        EmsScriptNode snapshotNode = null;
        if ( !this.isSnapshotNode ) {
            snapshotNode =
                    snapshotFolder.createNode( snapshotName, "view2:Snapshot" );
            snapshotNode.createOrUpdateProperty( "cm:isIndexed", true );
            snapshotNode.createOrUpdateProperty( "cm:isContentIndexed", false );
            snapshotNode.createOrUpdateProperty( Acm.ACM_ID, snapshotName );

            view.createOrUpdateAssociation( snapshotNode, "view2:snapshots" );
        } else {
            snapshotNode = view;
        }

        JSONObject snapshotJson = new JSONObject();
        try {
            if ( !this.isSnapshotNode ) {
                snapshotJson.put( "snapshot", true );
                ActionUtil.saveStringToFile( snapshotNode, "application/json",
                                             services,
                                             snapshotJson.toString( 4 ) );
            }
            DocBookWrapper docBookWrapper =
                    createDocBook( view, viewId, snapshotName, contextPath,
                                   snapshotNode );
            if ( docBookWrapper == null ) {
                log( LogLevel.ERROR, "Failed to generate DocBook!" );
            } else {
                docBookWrapper.save();
                String id = (String)snapshotNode.getProperty( Acm.ACM_ID );
                docBookWrapper.saveDocBookToRepo( snapshotFolder );
            }
        } catch ( Exception e1 ) {
            e1.printStackTrace();
        }

        return snapshotNode;
    }

    private DocumentElement createTable( JSONObject obj ) throws JSONException {
        DBTable table = new DBTable();
        String title = (String)obj.opt( "title" );
        String style = (String)obj.opt( "sytle" );
        table.setId( getSymlId( obj ) );
        table.setTitle( title );
        table.setStyle( style );
        table.setHeaders( createTableHeader( obj ) );
        table.setBody( createTableBody( obj ) );

        // table.setCols(headerCols.length());
        return (DocumentElement)table;
    }

    private List< List< DocumentElement >>
            createTableBody( JSONObject obj ) throws JSONException {
        return createTableRows( obj.getJSONArray( "body" ) );
    }
    
    private List< List< DocumentElement >>
            createTableHeader( JSONObject obj ) throws JSONException {
        return createTableRows( obj.getJSONArray( "header" ) );
    }
    
    private List< List< DocumentElement >>
            createTableRows( JSONArray jsonRows ) throws JSONException {
        List< List< DocumentElement >> list =
                new ArrayList< List< DocumentElement >>();
        for ( int i = 0; i < jsonRows.length(); i++ ) {
            JSONArray headerRows = jsonRows.getJSONArray( i );
            List< DocumentElement > rows = new ArrayList< DocumentElement >();
            for ( int j = 0; j < headerRows.length(); j++ ) {
                JSONObject contents = headerRows.getJSONObject( j );
                JSONArray headerCols = contents.getJSONArray( "content" );
                for ( int l = 0; l < headerCols.length(); l++ ) {
                    JSONObject content = headerCols.getJSONObject( l );
                    String colspan = (String)content.opt( "colspan" );
                    String rowspan = (String)content.opt( "rowspan" );
                    DocumentElement e = createElement( content );
                    if ( e != null ) rows.add( e );
                }
            }
            list.add( rows );
        }
        return list;
    }
    
    private DBSection emsScriptNodeToDBSection( EmsScriptNode node,
                                                Boolean isChapter ) throws JSONException {
        DBSection section = new DBSection();
        if ( isChapter ) section.setChapter( isChapter );
        section.setTitle( (String)node.getProperty( Acm.ACM_NAME ) );
        section.setId( (String)node.getProperty( Acm.ACM_ID ) );

        traverseElements( section, node );
        return section;
    }
   
    private Object extractNodeValue( EmsScriptNode node ) {
        Object valObj = node.getProperty( Acm.SYSML + "integer" );
        if ( valObj == null || !( valObj instanceof Integer ) ) {
            valObj = node.getProperty( Acm.SYSML + "real" );
            if ( valObj == null || !( valObj instanceof Float ) ) {
                valObj = node.getProperty( Acm.SYSML + "string" );
                if ( valObj == null || !( valObj instanceof String ) ) {
                    valObj = node.getProperty( Acm.SYSML + "boolean" );
                    if ( valObj == null || !( valObj instanceof Boolean ) ) {
                        valObj = node.getProperty( Acm.SYSML + "double" );
                        if ( valObj == null || !( valObj instanceof Double ) ) return null;
                    }
                }
            }
        }
        return valObj;
    }
    
    /**
	 * Utility function to find all the NodeRefs for the specified name
	 * @param name
	 * @return
	 */
	private ResultSet findNodeRef(String name) {
	    String pattern = "@cm\\:name:\"" + name + "\"";
		ResultSet query = NodeUtil.luceneSearch( pattern);
		return query;
	}

    private String gatherJobName( JSONObject postJson ) {
        String jobName = "";
        try {
            jobName += postJson.getString( "id" );
            List< String > formats = getSnapshotFormats( postJson );
            for ( String s : formats ) {
                jobName += "_" + s;
            }
        } catch ( JSONException ex ) {
            log( LogLevel.ERROR, "Failed to gather job name!" );
            ex.printStackTrace();
        }
        return jobName;
    }
    
    public JSONObject generateHTML( String snapshotId ) throws JSONException {
        EmsScriptNode snapshotNode = findScriptNodeById( snapshotId, null, null, false ); // TODO -- REVIEW -- Pass in workspace????!!!!
        snapshotNode = generateHTML( snapshotNode );
        return populateSnapshotProperties( snapshotNode );
    }
    
    public EmsScriptNode generateHTML( EmsScriptNode snapshotNode ) {
        this.snapshotName = (String)snapshotNode.getProperty( Acm.ACM_ID );
        ChildAssociationRef childAssociationRef =
                this.services.getNodeService()
                             .getPrimaryParent( snapshotNode.getNodeRef() );
        EmsScriptNode snapshotFolderNode =
                new EmsScriptNode( childAssociationRef.getParentRef(),
                                   this.services );
        DocBookWrapper docBookWrapper = new DocBookWrapper( this.snapshotName, snapshotNode );

        if ( !hasHtmlZip( snapshotNode ) ) {
            log( LogLevel.INFO, "Generating HTML zip..." );
            docBookWrapper.saveHtmlZipToRepo( snapshotFolderNode );
        }
        return snapshotNode;
    }
    
    public JSONObject generatePDF(String snapshotId) throws JSONException{
        EmsScriptNode snapshotNode = findScriptNodeById(snapshotId, null, null, false); // TODO -- REVIEW -- Pass in workspace????!!!!
    	    snapshotNode = generatePDF(snapshotNode);
    	    return populateSnapshotProperties(snapshotNode);
    }
    
    public EmsScriptNode generatePDF( EmsScriptNode snapshotNode ) {
        this.snapshotName = (String)snapshotNode.getProperty( Acm.ACM_ID );
        ChildAssociationRef childAssociationRef =
                this.services.getNodeService()
                             .getPrimaryParent( snapshotNode.getNodeRef() );
        EmsScriptNode snapshotFolderNode =
                new EmsScriptNode( childAssociationRef.getParentRef(),
                                   this.services );
        DocBookWrapper docBookWrapper = new DocBookWrapper( this.snapshotName, snapshotNode );
        if ( !hasPdf( snapshotNode ) ) {
            log( LogLevel.INFO, "Generating PDF..." );
            docBookWrapper.savePdfToRepo( snapshotFolderNode );
        }
        return snapshotNode;
    }

    private String getDocumentElementContent( DocumentElement elem ) {
        if ( elem instanceof DBParagraph ) return ( (DBParagraph)elem ).getText().toString();

        return "";
    }

    /**
     * Retrieve the snapshot folder for the view (goes up chain until it hits ViewEditor)
     * 
     * @param viewNode
     * @return
     */
    public static EmsScriptNode getSnapshotFolderNode(EmsScriptNode viewNode) {
        EmsScriptNode parent = viewNode.getParent();

        String parentName = (String) parent.getProperty(Acm.CM_NAME);
        while (!parentName.equals("Models") && !parentName.equals("ViewEditor")) {
            EmsScriptNode oldparent = parent;
            parent = oldparent.getParent();
            parentName = (String) parent.getProperty(Acm.CM_NAME);
        }
        // put snapshots at the project level
        parent = parent.getParent();
        
        EmsScriptNode snapshotNode = parent.childByNamePath("snapshots");
        if ( viewNode.getWorkspace() != null && viewNode.getWorkspace().exists() &&
                !viewNode.getWorkspace().contains(snapshotNode) ) {
            viewNode.getWorkspace().replicateWithParentFolders( snapshotNode );
        }
       
        if (snapshotNode == null) {
            snapshotNode = parent.createFolder("snapshots");
        }

        return snapshotNode;
    }

    public static EmsScriptNode getHtmlZipNode( EmsScriptNode snapshotNode ) {
        NodeRef node = (NodeRef)snapshotNode.getProperty( "view2:htmlZipNode" );
        return new EmsScriptNode( node, snapshotNode.getServices() );
    }

    /**
     * @param jsonObj
     * @param transcludedType
     *            : "documentation", "name", or "value"
     * @return
     */
    private String getTranscludedContent( JSONObject jsonObj,
                                          String transcludedType ) {
        String content = (String)jsonObj.opt( transcludedType );
        if ( content != null && !content.isEmpty() ) return content;
        try {
            JSONObject spec = (JSONObject)jsonObj.get( "specialization" );
            content = (String)spec.opt( transcludedType );
            if ( content != null && !content.isEmpty() ) return content;

        } catch ( JSONException ex ) {
            System.out.println( "Failed to retrieve transcluded content!" );
        }
        System.out.println( "Unable to find transclude content for JSONObject:" );
        System.out.println( jsonObj.toString() );
        return "";
    }

    private String getTranscludedVal( JSONObject jsonObj ) {
        String val = (String)jsonObj.opt( "value" );
        if ( val != null && !val.isEmpty() ) return val;
        try {
            JSONObject spec = (JSONObject)jsonObj.get( "specialization" );
            JSONArray values = spec.getJSONArray( "value" );
            StringBuffer sb = new StringBuffer();
            Object obj = null;
            for ( int i = 0; i < values.length(); i++ ) {
                JSONObject value = values.getJSONObject( i );
                String type = (String)value.opt( "type" );
                switch ( type ) {
                    case "LiteralInteger":
                        obj = value.opt( "integer" );
                        break;
                    case "LiteralBoolean":
                        obj = value.opt( "boolean" );
                        break;
                    case "LiteralReal":
                        obj = value.opt( "double" );
                        break;
                    case "LiteralString":
                        obj = value.opt( "string" );
                        break;
                }
                if ( obj != null ) sb.append( obj.toString() );
            }
            return sb.toString();
        } catch ( JSONException ex ) {
            System.out.println( "Failed to retrieve transcluded value!" );
        }
        System.out.println( "Unable to find transcluded val for JSONObject:" );
        System.out.println( jsonObj.toString() );
        return "";
    }

    public static EmsScriptNode getPdfNode( EmsScriptNode snapshotNode ) {
        NodeRef node = (NodeRef)snapshotNode.getProperty( "view2:pdfNode" );
        return new EmsScriptNode( node, snapshotNode.getServices() );
    }

    private ArrayList< String >
            getSnapshotFormats( JSONObject postJson ) throws JSONException {
        ArrayList< String > list = new ArrayList< String >();
        JSONArray formats = postJson.getJSONArray( "formats" );
        for ( int i = 0; i < formats.length(); i++ ) {
            JSONObject jsonType = formats.getJSONObject( i );
            String formatType = jsonType.getString( "type" );
            list.add( formatType );
        }
        return list;
    }

    private String getSymlId( JSONObject jsonObj ) {
        return (String)jsonObj.opt( Acm.SYSMLID );
    }

    private String getType( JSONObject jsonObj ) {
        String type = (String)jsonObj.opt( "type" );
        if ( type != null && !type.isEmpty() ) return type;
        try {
            JSONObject spec = (JSONObject)jsonObj.get( "specialization" );
            type = (String)spec.opt( "type" );
            if ( type != null && !type.isEmpty() ) return type;

        } catch ( JSONException ex ) {
            System.out.println( "Failed to retrieve document element type!" );
        }
        return null;
    }

    private String getUserProfile( EmsScriptNode node, String userName ) {
        StringBuffer sb = new StringBuffer();
        EmsScriptNode user =
                new EmsScriptNode( node.getServices().getPersonService()
                                       .getPerson( userName ),
                                   node.getServices(), node.getResponse() );
        sb.append( user.getProperty( "cm:firstName" ) );
        sb.append( "," );
        sb.append( user.getProperty( "cm:lastName" ) );
        sb.append( "," );
        // job title
        sb.append( "," );
        sb.append( user.getProperty( "cm:organizationId" ) );
        sb.append( "," );
        return sb.toString();
    }

    private String getViewId( WebScriptRequest req ) {
        String viewId = null;
        String[] viewKeys = { "viewid", "productId" };
        for ( String key : viewKeys ) {
            viewId = req.getServiceMatch().getTemplateVars().get( key );
            if ( viewId != null ) {
                break;
            }
        }
        return viewId;
    }

    private
            JSONObject
            handleGenerateArtifacts( JSONObject postJson,
                                     EmsScriptNode siteNode, Status status )
                                                                            throws JSONException {
        String siteName = (String)siteNode.getProperty( Acm.CM_NAME );
        EmsScriptNode jobNode = null;

        if ( !postJson.has( "id" ) ) {
            log( LogLevel.ERROR, "Job name not specified",
                 HttpServletResponse.SC_BAD_REQUEST );
            return null;
        }
        if ( !postJson.has( "formats" ) ) {
            log( LogLevel.ERROR, "Snapshot formats not specified",
                 HttpServletResponse.SC_BAD_REQUEST );
            return null;
        }

        String jobName = gatherJobName( postJson );
        try {
            jobNode =
                    ActionUtil.getOrCreateJob( siteNode, jobName,
                                               "cm:content", status,
                                               response );
            if ( jobNode == null ) {
                log( LogLevel.ERROR, "Couldn't create snapshot job: "
                                     + postJson.getString( "id" ),
                     HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                return null;
            }
            startAction( jobNode, siteName, postJson );
            return postJson;
        } catch ( JSONException ex ) {
            log( LogLevel.ERROR, "Failed to create snapshot job!" );
            ex.printStackTrace();
        } finally {
            if ( jobNode != null ) {
                jobNode.createOrUpdateProperty( "ems:job_status", "Succeeded" );
            }
        }
        return null;
    }

    /**
     * 
     * @param id
     * @param transclusionType
     * @param inputString
     * @param cirRefList
     * @param index
     * @return
     */
    private String handleTransclusion( String id, String transclusionType,
                                       String inputString,
                                       List< List< String >> cirRefList,
                                       int index ) {
        if ( cirRefList == null ) {
            cirRefList = new ArrayList< List< String >>();
        }
        List< String > list = null;
        if ( cirRefList.size() <= index ) {
            list = new ArrayList< String >();
            cirRefList.add( list );
        }
        list = cirRefList.get( index );
        list.add( id + transclusionType );
        index++;
        String result = parseTransclusionName( cirRefList, index, inputString );
        result = parseTransclusionDoc( cirRefList, index, result );
        result = parseTransclusionVal( cirRefList, index, result );
        return result;
    }

    /**
     * 
     * @param snapshotNode
     * @return
     */
    public static boolean hasHtmlZip( EmsScriptNode snapshotNode ) {
        return snapshotNode.hasAspect( "view2:htmlZip" );
    }

    public static boolean hasPdf( EmsScriptNode snapshotNode ) {
        return snapshotNode.hasAspect( "view2:pdf" );
    }

    private String HtmlSanitize( String s ) {
        return s.replaceAll( "(?i)<p>([^<]*)</p>", "$1" );
    }

    // private String parseTransclusion(List<String> cirRefList, String
    // inputString){

    private boolean isCircularReference( String id, String transclusionType,
                                         List< List< String >> cirRefList,
                                         int index ) {
        for ( int i = index - 1; i >= 0; i-- ) {
            if ( cirRefList.get( i ).contains( id + transclusionType ) ) return true;
        }
        return false;
    }

	private String parseTransclusionDoc(List<List<String>> cirRefList, int index,
	                                    String inputString){
		Document document = Jsoup.parseBodyFragment(inputString);
		Elements elements = document.getElementsByTag("mms-transclude-doc");
		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(isCircularReference(id, "documentation", cirRefList, index)){
				System.out.println("Circular reference!");
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}
			EmsScriptNode nameNode = findScriptNodeById(id, null, null, false); // TODO -- REVIEW -- Pass in workspace????!!!!
			if(id == null || id.isEmpty()){
				System.out.println("Failed to find EmsScriptNode Id " + id);
			}
			else{
				try {
					JSONObject jsObj = nameNode.toJSONObject(null);
					if(jsObj == null){
						System.out.println("JSONObject is null");
					}
					else{
						String doc = getTranscludedContent(jsObj, "documentation");
						String transcluded = doc;
						while(true){
							transcluded = handleTransclusion(id, "doc", transcluded,
							                                 cirRefList, index);
							if(transcluded.compareToIgnoreCase(doc) == 0) break;
							doc = transcluded;
						}
						element.before(transcluded);
						element.remove();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					System.out.println("Failed to transclude Id: " + id);
					e.printStackTrace();
				}
			}
		}
		return document.body().html();
	}
	
	private String parseTransclusionName(List<List<String>> cirRefList, int index, String inputString){
		Document document = Jsoup.parseBodyFragment(inputString);
		Elements elements = document.getElementsByTag("mms-transclude-name");
		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(isCircularReference(id, "name", cirRefList, index)){
				System.out.println("Circular reference!");
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}
			EmsScriptNode nameNode = findScriptNodeById(id, null, null, false); // TODO -- REVIEW -- Pass in workspace????!!!!
			if(id == null || id.isEmpty()){
				System.out.println("Failed to find EmsScriptNode Id " + id);
			}
			else{
				try {
					JSONObject jsObj = nameNode.toJSONObject(null);
					if(jsObj == null){
						System.out.println("JSONObject is null");
					}
					else{
						String name = getTranscludedContent(jsObj, "name");
						String transcluded = name;
						while(true){
							transcluded = handleTransclusion(id, "name", transcluded, cirRefList, index);
							if(transcluded.compareToIgnoreCase(name) == 0) break;
							name = transcluded;
						}
						element.before(transcluded);
						element.remove();
					}
				} catch (JSONException e) {
					System.out.println("Failed to transclude Id: " + id);
					e.printStackTrace();
				}
			}
		}
		return document.body().html();
	}
	
	private String parseTransclusionVal(List<List<String>> cirRefList, int index, String inputString){
		Document document = Jsoup.parseBodyFragment(inputString);
		Elements elements = document.getElementsByTag("mms-transclude-val");
		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(isCircularReference(id, "value", cirRefList, index)){
				System.out.println("Circular reference!");
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}
			EmsScriptNode nameNode = findScriptNodeById(id, null, null, false); // TODO -- REVIEW -- Pass in workspace????!!!!
			if(id == null || id.isEmpty()){
				System.out.println("Failed to find EmsScriptNode Id " + id);
			}
			else{
				try {
					JSONObject jsObj = nameNode.toJSONObject(null);
					if(jsObj == null){
						System.out.println("JSONObject is null");
					}
					else{
						String val = getTranscludedVal(jsObj);
						element.before(val);
						element.remove();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					System.out.println("Failed to transclude Id: " + id);
					e.printStackTrace();
				}
			}
		}
		return document.body().html();
	}

    private JSONObject populateSnapshotProperties( EmsScriptNode snapshotNode )
            throws JSONException {
        JSONObject snapshoturl = snapshotNode.toJSONObject( null );
        if ( hasPdf( snapshotNode ) || hasHtmlZip( snapshotNode ) ) {
            HashMap< String, String > transformMap;
            LinkedList< HashMap > list = new LinkedList< HashMap >();
            if ( hasPdf( snapshotNode ) ) {
                EmsScriptNode pdfNode = getPdfNode( snapshotNode );
                transformMap = new HashMap< String, String >();
                transformMap.put( "type", "pdf" );
                transformMap.put( "url", pdfNode.getUrl() );
                list.add( transformMap );
            }
            if ( hasHtmlZip( snapshotNode ) ) {
                EmsScriptNode htmlZipNode = getHtmlZipNode( snapshotNode );
                transformMap = new HashMap< String, String >();
                transformMap.put( "type", "html" );
                transformMap.put( "url", htmlZipNode.getUrl() );
                list.add( transformMap );
            }
            snapshoturl.put( "formats", list );
        }
        return snapshoturl;
    }

	private String processTransclusion(List<String> cirRefList, Document document,
	                                   Elements elements){
		if(elements == null) return "";
		
		for(Element element:elements){
			//System.out.println("Found transclusion!");
			String id = element.attr("data-mms-eid");
			//System.out.println("referenced id: " + id);
			if(cirRefList.contains(id)){
				System.out.println("Circular reference!");
				element.before(" CIRCULAR REFERENCE ");
				element.remove();
				continue;
			}
			cirRefList.add(id);
			//System.out.println("findScriptNodeById...");
			EmsScriptNode nameNode = findScriptNodeById(id, null, null, false); // TODO -- REVIEW -- Pass in workspace????!!!!
			try {
				//System.out.println("found nameNode!");
				JSONObject jsObj = nameNode.toJSONObject(null);
				if(jsObj == null){
					System.out.println("JSONObject is null");
				}
				else{
					//System.out.println("Creating element...");
					DocumentElement elm = createElement(jsObj);
					if(elm instanceof DBParagraph){
						System.out.println("transcluded paragraph: " + ((DBParagraph)elm).getText());
						element.before((String)((DBParagraph)elm).getText());
						element.remove();
					}
					else if(elm instanceof DBText){
						System.out.println("transcluded text: " + ((DBText)elm).getText());
						element.before((String)((DBText)elm).getText());
						element.remove();
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return document.body().html();
	}
	
    private JSONObject saveAndStartAction(WebScriptRequest req, Status status) {
	    JSONObject jsonObject = null;
	    String siteName = getSiteName(req);
		if (siteName == null) {
			log(LogLevel.ERROR, "No sitename provided", HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
		if (siteInfo == null) {
			log(LogLevel.ERROR, "Could not find site: " + siteName, HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
		EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);

		JSONObject reqPostJson = (JSONObject) req.parseContent();
		JSONObject postJson;
		try {
		    if (reqPostJson.has( "snapshots" )) {
		        JSONArray configsJson = reqPostJson.getJSONArray( "snapshots" );
		        postJson = configsJson.getJSONObject( 0 );
		    } else {
		        postJson = reqPostJson;
		    }
		    
			if (!postJson.has( "formats" )) {
				log(LogLevel.ERROR, "Missing snapshot formats!", HttpServletResponse.SC_BAD_REQUEST);
			} else {
				try{
					jsonObject = handleGenerateArtifacts(postJson, siteNode, status);
				}
				catch(JSONException ex){
					log(LogLevel.ERROR, "Failed to generate snapshot artifact(s)!");
					ex.printStackTrace();
				}
			}
		} catch (JSONException e) {
			log(LogLevel.ERROR, "Could not parse JSON", HttpServletResponse.SC_BAD_REQUEST);
			e.printStackTrace();
			return null;
		}
		
		return jsonObject;
	}

    private
            void
            saveImage( DBImage image, EmsScriptNode imageEmsScriptNode )
                                                                        throws Exception {
        String tmpDirName = TempFileProvider.getTempDir().getAbsolutePath();
        Path jobDirName = Paths.get( tmpDirName, this.snapshotName );
        Path dbDirName = Paths.get( jobDirName.toString(), "docbook" );
        // Path imgDirName = Paths.get(jobDirName.toString(), "images");
        Path imgDirName = Paths.get( dbDirName.toString(), "images" );

        if ( !( new File( jobDirName.toString() ).mkdirs() ) ) {
            log( LogLevel.ERROR,
                 "Could not create snapshot job_id temporary directory." );
        }
        if ( !( new File( dbDirName.toString() ).mkdirs() ) ) {
            log( LogLevel.ERROR, "Could not create Docbook temporary directory" );
        }
        if ( !( new File( imgDirName.toString() ).mkdirs() ) ) {
            log( LogLevel.ERROR, "Could not create image temporary directory." );
        }
        ContentService contentService =
                imageEmsScriptNode.getServices().getContentService();
        NodeService nodeService =
                imageEmsScriptNode.getServices().getNodeService();
        NodeRef imgNodeRef = imageEmsScriptNode.getNodeRef();
        ContentReader reader =
                contentService.getReader( imgNodeRef, ContentModel.PROP_CONTENT );
        InputStream originalInputStream = reader.getContentInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final int BUF_SIZE = 1 << 8; // 1KiB buffer
        byte[] buffer = new byte[ BUF_SIZE ];
        int bytesRead = -1;
        while ( ( bytesRead = originalInputStream.read( buffer ) ) > -1 ) {
            outputStream.write( buffer, 0, bytesRead );
        }
        originalInputStream.close();
        byte[] binaryData = outputStream.toByteArray();
        String imgFilename =
                (String)nodeService.getProperty( imgNodeRef,
                                                 ContentModel.PROP_NAME );
        Path filePath = Paths.get( imgDirName.toString(), imgFilename );
        Files.write( filePath, binaryData );
        image.setFilePath( dbDirName.relativize( filePath ).toString() );
    }
    
    private void setDocumentElementContent( DocumentElement elem, String s ) {
        if ( elem instanceof DBParagraph ) ( (DBParagraph)elem ).setText( s );
    }
   
    /**
	 * Kick off the actual action in the background
	 * @param jobNode
	 * @param siteName
	 * @param snapshot Id
	 * @param snapshot format types
	 */
	public void startAction(EmsScriptNode jobNode, String siteName, JSONObject postJson) throws JSONException {
		ArrayList<String> formats = getSnapshotFormats(postJson);
        ActionService actionService = services.getActionService();
        Action snapshotAction = actionService.createAction(SnapshotArtifactsGenerationActionExecuter.NAME);
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_SITE_NAME, siteName);
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_SNAPSHOT_ID, postJson.getString("id"));
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_FORMAT_TYPE, formats);
       	services.getActionService().executeAction(snapshotAction, jobNode.getNodeRef(), true, true);
	}

    private void traverseElements( DBSection section, EmsScriptNode node )
            throws JSONException {
        JSONArray contains = node.getView().getContainsJson();
        createDBSectionContainment( section, contains );
    	/*
    	  for(int i=0; i < contains.length(); i++){
    		JSONObject obj = contains.getJSONObject(i);
			DocumentElement e = createElement(obj);
			if(e != null) section.addElement(e);
    	}
    	*/
    }
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }
}
