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

import gov.nasa.jpl.docbook.model.DBBook;
import gov.nasa.jpl.docbook.model.DBColSpec;
import gov.nasa.jpl.docbook.model.DBImage;
import gov.nasa.jpl.docbook.model.DBList;
import gov.nasa.jpl.docbook.model.DBParagraph;
import gov.nasa.jpl.docbook.model.DBSection;
import gov.nasa.jpl.docbook.model.DBTable;
import gov.nasa.jpl.docbook.model.DBTableEntry;
import gov.nasa.jpl.docbook.model.DBText;
import gov.nasa.jpl.docbook.model.DocumentElement;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.SnapshotArtifactsGenerationActionExecuter;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
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
import org.alfresco.service.cmr.security.PersonService;
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
	private JSONArray view2view;
	private DocBookWrapper docBookMgr;
	protected NodeService nodeService;
	protected PersonService personService;

    public void setNodeService(NodeService nodeService)
    {
       this.nodeService = nodeService;
    }

    public void setPersonService(PersonService personService)
    {
    	this.personService = personService;
    }

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
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        SnapshotPost instance = new SnapshotPost(repository, services);
        return instance.executeImplImpl(req,  status, cache);
    }

    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        printHeader( req );
        clearCaches();

        WorkspaceNode workspace = getWorkspace( req );
        Date timestamp = getTimestamp(req);

        Map< String, Object > model = new HashMap< String, Object >();
        log( LogLevel.INFO, "Starting snapshot creation or snapshot artifact generation..." );
        try {
            JSONObject reqPostJson = (JSONObject)req.parseContent();
            if ( reqPostJson != null ) {
                log( LogLevel.INFO, "Generating snapshot artifact..." );
                //SnapshotPost instance = new SnapshotPost( repository, services );
                JSONObject result = //instance.
                        saveAndStartAction( req, status, workspace );
                //appendResponseStatusInfo( instance );

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
                if ( snapshotFolderNode == null ) {
                    log( LogLevel.ERROR, "Cannot create folder for snapshot",
                         HttpServletResponse.SC_BAD_REQUEST );
                } else {
                    this.snapshotName = viewId + "_" + now.getMillis();

                    if ( checkPermissions( snapshotFolderNode,
                                           PermissionService.WRITE ) ) {
                        snapshotNode =
                                createSnapshot( topview, viewId, snapshotName,
                                                req.getContextPath(),
                                                snapshotFolderNode,
                                                workspace, timestamp);
                    }
                }

                if ( snapshotNode == null ) {
                    log( LogLevel.ERROR, "Error creating snapshot node",
                         HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                } else {
                    try {
                        JSONObject snapshoturl = new JSONObject();
                        if (!Utils.isNullOrEmpty(response.toString())) snapshoturl.put("message", response.toString());
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
                }
                status.setCode( responseStatus.getCode() );
                if ( status.getCode() != HttpServletResponse.SC_OK ) {
                    model.put( "res", response.toString() );
                }
            }
        } catch ( Exception ex ) {
        	status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

    private String buildInlineImageTag(String nodeId, String imgPath){
    	StringBuffer sb = new StringBuffer();
    	sb.append("<inlinemediaobject>");
        sb.append("<imageobject condition='web'>");
        sb.append("<imagedata scalefit='1' width='100%' fileref='");
		sb.append(imgPath);
		sb.append("' />");
        sb.append("</imageobject>");
        sb.append("</inlinemediaobject>");
    	return sb.toString();
    }

    private DBParagraph createDBParagraph( JSONObject obj, DBSection section, WorkspaceNode workspace, Date timestamp  ) {
    	if(obj == null) return null;
        String srcType = (String)obj.opt( "sourceType" );
        String src = (String)obj.opt( "source" );
        String srcProp = (String)obj.opt( "sourceProperty" );

        DBParagraph p = new DBParagraph();
        p.setId( src );
        if (srcType != null && srcType.compareTo( "reference" ) == 0 ) {
            EmsScriptNode node = findScriptNodeById( src, workspace, null, false );
            if(node == null){
            	System.out.println("[WARNING]: Failed to create DBParagraph! Failed to find EmsScriptNode with Id: " + src);
            }
            else{
	            if (srcProp != null && srcProp.compareTo( "value" ) == 0 ) {
	                List< NodeRef > nodeRefs = (List< NodeRef >)node.getProperty( Acm.SYSML + srcProp );
	                if(nodeRefs == null){
	                	System.out.println("[WARNING]: Failed to create DBParagraph! Failed to find values node references.");
	                	return null;
	                }

	                StringBuffer sb = new StringBuffer();
	                int size = nodeRefs.size();
	                for ( int i = 0; i < size; i++ ) {
	                    NodeRef nodeRef = nodeRefs.get( i );
	                    if(nodeRef == null){
	                    	System.out.println("[WARNING]: Failed to get value node ref at index: " + i);
	                    	continue;
	                    }

	                    EmsScriptNode valueNode = new EmsScriptNode( nodeRef, node.getServices() );

	                    String valueProp = (String)node.getProperty( Acm.SYSML + "name" );
	                    if ( valueProp != null && !valueProp.isEmpty() ) {
	                        Object value = valueNode.getProperty( Acm.SYSML + valueProp );
	                        if ( value == null || ( value != null && value.toString().isEmpty() ) ) {
	                            value = extractNodeValue( valueNode );
	                            if ( value == null || ( value != null && value.toString().isEmpty() ) ) continue;
	                        }

	                        if ( value instanceof String ) sb.append( HtmlSanitize( (String)value ) );
	                        else sb.append( value );
	                    } else {
	                        try {
	                            Object valObj = extractNodeValue( valueNode );
	                            if ( valObj == null ) continue;
	                            if ( valObj instanceof String ) sb.append( HtmlSanitize( (String)valObj ) );
	                            else sb.append( valObj );
	                        }
	                        catch ( Exception ex ) {
	                            log( LogLevel.WARNING, "Problem extracting node value from " + node.toJSON() );
	                        }
	                    }
	                    sb.append(" ");
	                }
	                p.setText( sb.toString() );
	            }
	            else {
	                String s = (String)node.getProperty( Acm.SYSML + srcProp );
	                s = handleTransclusion( src, srcProp, s, null, 0 );
	                s = handleEmbeddedImage(src, s, section);
	                s = HtmlSanitize( s );
	                if(s != null && !s.isEmpty()) p.setText(s);
	            }
            }
        }
        else {
            if ( srcProp != null && !srcProp.isEmpty() ) {
                String s = (String)obj.opt( Acm.SYSML + srcProp );
                s = handleTransclusion( src, srcProp, s, null, 0 );
                s = handleEmbeddedImage(src, s, section);
                s = HtmlSanitize( s );
                if(s != null && !s.isEmpty()) p.setText(s);
            }
            else p.setText( HtmlSanitize( (String)obj.opt( "text" ) ) );
        }
        if ( p.getText() == null || p.getText().toString().isEmpty() ) return null;

        return p;
    }

    private DocumentElement createDBSection( JSONObject obj, WorkspaceNode workspace, Date timestamp ) throws JSONException {
        DBSection section = new DBSection();
        section.setTitle( (String)obj.opt( "name" ) );
        createDBSectionContainment( section, obj.getJSONArray( "contains" ), workspace, timestamp );
        return section;
    }

    private void createDBSectionContainment(DBSection section,
                                            JSONArray jsonContains,
                                            WorkspaceNode workspace, Date timestamp) throws JSONException{
        for ( int i = 0; i < jsonContains.length(); i++ ) {
            JSONObject obj = jsonContains.getJSONObject( i );
            DocumentElement e = createElement( obj, section, workspace, timestamp );
            if ( e != null ) section.addElement( e );

            //traverseElements(section, node);
        }
	}

    private DocumentElement createDBText( JSONObject obj, WorkspaceNode workspace, Date timestamp  ) throws JSONException {
        DBText text = new DBText();
        String value = null;
        String sourceType = obj.optString("sourceType");
        if(sourceType != null && !sourceType.isEmpty()){
        	String source = obj.optString(sourceType);
        	if(source != null && !source.isEmpty()) value = source;
        }
        else{
        	value = obj.optString( "name" );
        }
        text.setText(HtmlSanitize(value));
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

    private DocBookWrapper createDocBook( EmsScriptNode product, String productId,
                                          String snapshotName, String contextPath,
                                          EmsScriptNode snapshotFolder,
                                          WorkspaceNode workspace, Date timestamp) throws Exception {
//        log( LogLevel.INFO, "\ncreating DocBook snapshot for view Id: " + productId );
//        log( LogLevel.INFO, "\ncreating DocBook snapshotname: " + snapshotName );

        if ( product == null ) {
            log( LogLevel.WARNING, "null [view] input parameter reference." );
            return null;
        }

        docBookMgr = new DocBookWrapper( this.snapshotName, snapshotFolder );
        try {
            DBBook docBook = createDocBook( product );
            docBook.setRemoveBlankPages( true );

            View productView = product.getView();
            if(productView == null) throw new Exception("Failed to get product's view!");

            JSONArray contains = productView.getContainsJson();
            if(contains == null || contains.length()==0){ throw new Exception("Failed to retrieve 'contains' JSONArray."); }

            for(int i=0; i < contains.length(); i++){
	            JSONObject contain = contains.getJSONObject(0);
	            if(contain == null) throw new Exception("Failed to get contain JSONObject at index: " + i);

	            String sourceType = (String)contain.opt("sourceType");
	            String source = (String)contain.opt("source");
	            if(source == null || source.isEmpty()) throw new Exception("Failed to get contain source property!");

	            this.view2view = productView.getViewToViewPropertyJson();
	            if(view2view == null || view2view.length()==0) throw new Exception ("Failed to retrieve 'view2view' JSONArray.");

	            JSONObject v2vChildNode = getChildrenViews(source);
	            if(v2vChildNode == null) throw new Exception("Failed to retrieve 'view2view' children view for: " + source);

	            JSONArray childrenViews = v2vChildNode.getJSONArray("childrenViews");
	            if(childrenViews == null) throw new Exception("Failed to retrieve 'childrenViews'.");

	            for(int k=0; k< childrenViews.length(); k++){
	            	String childId = childrenViews.getString(k);
	            	if(childId == null || childId.isEmpty()) throw new Exception("Failed to get 'childrenViews'[" + k + "] Id!");

	            	EmsScriptNode childNode = findScriptNodeById(childId, workspace, timestamp, false);
	            	if(childNode == null) throw new Exception("Failed to find EmsScriptNode with Id: " + childId);
	            	//creating chapters
	            	DocumentElement section = emsScriptNodeToDBSection(childNode, true, workspace, timestamp);
	            	if(section != null) docBook.addElement(section);
	            }
            }
            docBookMgr.setDBBook(docBook);
            docBookMgr.save();
        }
        catch ( Exception ex ) {
            log( LogLevel.ERROR, "\nUnable to create DBBook! Failed to parse document.\n" + ex.getStackTrace() );
            ex.printStackTrace();
            throw new Exception( "Unable to create DBBook! Failed to parse document.\n", ex );
        }
        return docBookMgr;
    }

    private DocBookTable createDocBookTable(JSONObject tblJson){
    	DocBookTable dbTable = new DocBookTable();
       	int maxColCount = 0;
    	try{
    		int colCount = 0;
    		JSONArray header = tblJson.getJSONArray( "header" ) ;
    		dbTable.setHeaderRowCount(header.length());
    		for(int i=0; i < header.length(); i++){
    			JSONArray headerRows = header.getJSONArray(i);
    			colCount = 0;
    			for(int j=0; j < headerRows.length(); j++){
    				JSONObject contents = headerRows.getJSONObject(j);
					String colspan = contents.optString("colspan");
					int col = 0;
					if(colspan != null && !colspan.isEmpty()) col = Integer.parseInt(colspan);
					colCount += col;
    				if (colCount > maxColCount) maxColCount = colCount;
    			}
    		}

    		JSONArray body = tblJson.getJSONArray( "body" );
    		dbTable.setBodyRowCount(body.length());
    		for(int i=0; i < body.length(); i++){
    			JSONArray headerRows = body.getJSONArray(i);
    			colCount = 0;
    			for(int j=0; j < headerRows.length(); j++){
    				JSONObject contents = headerRows.getJSONObject(j);
					String colspan = contents.optString("colspan");
					int col = 0;
					if(colspan != null && !colspan.isEmpty()) col = Integer.parseInt(colspan);
					colCount += col;
    				if (colCount > maxColCount) maxColCount = colCount;
    			}
    		}
    	}
    	catch(Exception ex){
    		System.out.println("Failed to setup DocBookTable!");
    	}
    	dbTable.setColCount(maxColCount);
    	dbTable.init();
    	return dbTable;
    }

    private DocumentElement createList( JSONObject obj, DBSection section, WorkspaceNode workspace, Date timestamp  ) throws JSONException {
        Boolean isOrdered = (Boolean)obj.opt( "ordered" );

        DBList list = new DBList();
        list.setOrdered( isOrdered );
        JSONArray listItemWrapper = obj.getJSONArray( "list" );
        for ( int i = 0; i < listItemWrapper.length(); i++ ) {
            JSONArray listItems = listItemWrapper.getJSONArray( i );
            DocumentElement docElem = null;
            for ( int j = 0; j < listItems.length(); j++ ) {
                JSONObject jsObj = listItems.getJSONObject( j );
                DocumentElement e = createElement( jsObj, section, workspace, timestamp );
                if ( j > 0 ) {
                    appendElement( docElem, e );
                } else docElem = e;
            }
            if ( docElem != null ) list.addElement( docElem );
        }

        return list;
    }

    private DocumentElement createElement( JSONObject obj, DBSection section, WorkspaceNode workspace, Date timestamp ) throws JSONException {
        DocumentElement e = null;
        switch ( getType( obj ) ) {
            case "Paragraph":
                e = createDBParagraph( obj, section, workspace, timestamp );
                break;
            case "List":
                e = createList( obj, section, workspace, timestamp );
                break;
            case "Table":
                e = createTable( obj, section, workspace, timestamp );
                break;
            case "Image":
                e = createImage( obj, workspace, timestamp );
                break;
            case "Section":
                e = createDBSection( obj, workspace, timestamp );
                break;
            case "Element":
                e = createDBText( obj, workspace, timestamp );
                break;
            default:
                log( LogLevel.WARNING, "Unexpected type: " + getType( obj ) );
                break;
        }
        return e;
    }

    private DocumentElement createImage( JSONObject obj, WorkspaceNode workspace, Date timestamp  ) {
        DBImage image = new DBImage();
        image.setId( getSymlId( obj ) );

        String id = (String)obj.opt( Acm.SYSMLID );
        EmsScriptNode imgNode = findScriptNodeById( id, null, null, false );  //snapshot => ok to get the latest from 'master'
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
                                         WorkspaceNode workspace, Date timestamp ) {
        this.snapshotName = viewId + "_" + System.currentTimeMillis();
        log(LogLevel.INFO, "Begin creating snapshot: \t" + this.snapshotName);
        
        String contextPath = "alfresco/service/";
        EmsScriptNode viewNode = findScriptNodeById(viewId, workspace, timestamp, true);
        if(viewNode == null){
        	log(LogLevel.ERROR, "Failed to find script node with Id: " + viewId);
        	log(LogLevel.INFO, "End creating snapshot: \t\t" + this.snapshotName);
        	return null;
        }
        EmsScriptNode snapshotFolder = getSnapshotFolderNode(viewNode);
        if(snapshotFolder == null){
            log( LogLevel.ERROR, "Failed to get snapshot folder node!",
                 HttpServletResponse.SC_BAD_REQUEST );
            log(LogLevel.INFO, "End creating snapshot: \t\t" + this.snapshotName);
        	return null;
        }
        log(LogLevel.INFO, "End creating snapshot: \t\t" + this.snapshotName);
        return createSnapshot(view, viewId, snapshotName, contextPath, snapshotFolder, workspace, timestamp);
    }

    public EmsScriptNode createSnapshot( EmsScriptNode view, String viewId,
                                         String snapshotName,
                                         String contextPath,
                                         EmsScriptNode snapshotFolder,
                                         WorkspaceNode workspace, Date timestamp) {
        EmsScriptNode snapshotNode = snapshotFolder.createNode( snapshotName, "view2:Snapshot" );
        if (snapshotNode == null) {
            	log(LogLevel.ERROR, "Failed to create view2:Snapshot!");
            	return null;
        }
        snapshotNode.createOrUpdateProperty( "cm:isIndexed", true );
        snapshotNode.createOrUpdateProperty( "cm:isContentIndexed", false );
        snapshotNode.createOrUpdateProperty( Acm.ACM_ID, snapshotName );

        snapshotNode.createOrUpdateAspect( "view2:Snapshotable" );
        snapshotNode.createOrUpdateProperty( "view2:snapshotProduct", view.getNodeRef() );
        view.createOrUpdateAspect( "view2:Snapshotable" );
        view.appendToPropertyNodeRefs( "view2:productSnapshots", snapshotNode.getNodeRef() );

        JSONObject snapshotJson = new JSONObject();
        try {
            snapshotJson.put( "snapshot", true );
            ActionUtil.saveStringToFile( snapshotNode, "application/json", services, snapshotJson.toString( 4 ) );
            DocBookWrapper docBookWrapper = createDocBook( view, viewId, snapshotName, contextPath, snapshotNode, workspace, timestamp );
            if ( docBookWrapper == null ) {
                log( LogLevel.ERROR, "Failed to generate DocBook!" );
                snapshotNode = null;
            } else {
                docBookWrapper.save();
                docBookWrapper.saveDocBookToRepo( snapshotFolder, timestamp );
            }
        }
        catch ( Exception e1 ) {
            snapshotNode = null;
            e1.printStackTrace();
        }

        if (snapshotNode != null) {
            snapshotNode.getOrSetCachedVersion();
        }
        snapshotFolder.getOrSetCachedVersion();

        return snapshotNode;
    }

    private DocumentElement createTable( JSONObject obj, DBSection section, WorkspaceNode workspace, Date timestamp  ) throws JSONException {
        DBTable table = new DBTable();
        String title = (String)obj.opt( "title" );
        String style = (String)obj.opt( "sytle" );
        table.setId( getSymlId( obj ) );
        table.setTitle( title );
        table.setStyle( style );
        //int columnNum = getTableColumnMaxCount(obj);
        DocBookTable dbTable = createDocBookTable(obj);
        int cols = dbTable.getColCount();
        List<DBColSpec> colspecs = createTableColSpec(cols);
        table.setColspecs(colspecs);
        table.setCols(cols);
        table.setHeaders( createTableHeader( obj, section, dbTable, workspace, timestamp ));
        table.setBody( createTableBody( obj, section, dbTable, workspace, timestamp ));

        // table.setCols(headerCols.length());
        return table;
    }

    private List<DBColSpec> createTableColSpec(int columnNum){
    	List<DBColSpec> colspecs = new ArrayList<DBColSpec>();
    	for(int i=1; i <= columnNum; i++){
    		colspecs.add(new DBColSpec(i, String.valueOf(i)));
    	}
    	return colspecs;
    }

    private List< List< DocumentElement >> createTableBody( JSONObject obj, DBSection section, DocBookTable dbTable, WorkspaceNode workspace, Date timestamp  ) throws JSONException {
        return createTableRows( obj.getJSONArray( "body" ), section, dbTable, false, workspace, timestamp );
    }

    private List< List< DocumentElement >> createTableHeader( JSONObject obj, DBSection section, DocBookTable dbTable, WorkspaceNode workspace, Date timestamp  ) throws JSONException {
        return createTableRows( obj.getJSONArray( "header" ), section, dbTable, true, workspace, timestamp );
    }

    private List< List< DocumentElement >> createTableRows( JSONArray jsonRows, DBSection section, DocBookTable dbTable, boolean isHeader, WorkspaceNode workspace, Date timestamp  ) throws JSONException {
        List< List< DocumentElement >> list =
                new ArrayList< List< DocumentElement >>();
        for ( int i = 0; i < jsonRows.length(); i++ ) {
            JSONArray headerRows = jsonRows.getJSONArray( i );
            List< DocumentElement > rows = new ArrayList< DocumentElement >();
            int startCol = 1;
            for ( int j = 0; j < headerRows.length(); j++ ) {
                JSONObject contents = headerRows.getJSONObject( j );
                JSONArray headerCols = contents.getJSONArray( "content" );
                //create DBTableEntry at this scope level
                String colspan = contents.optString( "colspan" );
                String rowspan = contents.optString( "rowspan" );
                int col = 0; int row = 0;
                if(colspan != null && !colspan.isEmpty()) col = Integer.parseInt(colspan);
                if(rowspan != null && !rowspan.isEmpty()) row = Integer.parseInt(rowspan);
                DBTableEntry te = new DBTableEntry();
                int startColTemp = dbTable.getStartCol(i, j, row, col, isHeader);
                if(startColTemp > startCol) startCol = startColTemp;
                DocumentElement cellContent = null;

                for ( int l = 0; l < headerCols.length(); l++ ) {
                	//gather table cells content
                    JSONObject content = headerCols.getJSONObject( l );
                    DocumentElement cell = null;
                    cell = createElement(content, section, workspace, timestamp);
                    if(l > 0)
                    	appendElement(cellContent, cell);
                    else
                    	cellContent = cell;

                }

                if(col > 1 || row > 1){
                	if(row > 1) te.setMorerows(row-1);
                	if(col > 1){
                		int end = startCol + col - 1;
                		if(end > dbTable.getColCount()) end = dbTable.getColCount();
                		te.setNamest(String.valueOf(startCol));
                		te.setNameend(String.valueOf(end));
                	}
                	//te.addElement(cellContent);
                	//cell = (DocumentElement)te;
                }
                //else{
                	//cell = createElement(content);
                	////cell = createDBText(content);
                //}
                if(cellContent != null){
                	te.addElement(cellContent);
                }
                rows.add( te );

            }
            list.add( rows );
        }
        return list;
    }

    private DBSection emsScriptNodeToDBSection( EmsScriptNode node,
                                                Boolean isChapter,
                                                WorkspaceNode workspace, Date timestamp) throws Exception {
        DBSection section = new DBSection();
        if ( isChapter ) section.setChapter( isChapter );
        section.setTitle( (String)node.getProperty( Acm.ACM_NAME ) );
        section.setId( node.getSysmlId() );

        traverseElements( section, node, workspace, timestamp );
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

    public JSONObject generateHTML( String snapshotId, WorkspaceNode workspace ) throws Exception {
        EmsScriptNode snapshotNode = findScriptNodeById( snapshotId, workspace, null, false );
        if(snapshotNode == null) throw new Exception("Failed to find snapshot with Id: " + snapshotId);
        String status = getHtmlZipStatus(snapshotNode);
        boolean isGenerated = false;
        if(status != null && !status.isEmpty() && status.compareToIgnoreCase("Completed")==0){
        	isGenerated = true;
        }

        if(!isGenerated){
//	        Thread.sleep(10000);
	        try{
		        snapshotNode = generateHTML( snapshotNode, workspace );
		        if(snapshotNode == null) throw new Exception("generateHTML() returned null.");
		        else{
		        	this.setHtmlZipStatus(snapshotNode, "Completed");
		        }
	        }
	        catch(Exception ex){
	        	ex.printStackTrace();
	        	this.setHtmlZipStatus(snapshotNode, "Error");
	        	throw new Exception("Failed to generate HTML artifact!");
	        }
        }
        return populateSnapshotProperties( snapshotNode );
    }

    public EmsScriptNode generateHTML( EmsScriptNode snapshotNode, WorkspaceNode workspace ) throws Exception {
        this.snapshotName = snapshotNode.getSysmlId();
        if(this.snapshotName == null || this.snapshotName.isEmpty()) throw new Exception("Failed to retrieve snapshot Id!");

        ChildAssociationRef childAssociationRef =
                this.services.getNodeService()
                             .getPrimaryParent( snapshotNode.getNodeRef() );
        if(childAssociationRef == null) throw new Exception("Failed to retrieve snapshot association reference!");

        EmsScriptNode snapshotFolderNode = new EmsScriptNode( childAssociationRef.getParentRef(), this.services );
        if(snapshotFolderNode == null) throw new Exception("Failed to retrieve snapshot folder!");

        Date timestamp = (Date)snapshotNode.getProperty("view2:timestamp");
        DocBookWrapper docBookWrapper = new DocBookWrapper( this.snapshotName, snapshotNode );//need workspace and timestamp

        if ( !hasHtmlZipNode( snapshotNode ) ) {
            log( LogLevel.INFO, "Generating HTML zip..." );
            docBookWrapper.saveHtmlZipToRepo( snapshotFolderNode, workspace, timestamp );
        }
        return snapshotNode;
    }

    public JSONObject generatePDF(String snapshotId, WorkspaceNode workspace) throws Exception{
        EmsScriptNode snapshotNode = findScriptNodeById(snapshotId, workspace, null, false);
        if(snapshotNode == null) throw new Exception("Failed to find snapshot with Id: " + snapshotId);
        String status = getPdfStatus(snapshotNode);
        boolean isGenerated = false;
        if(status != null && !status.isEmpty() && status.compareToIgnoreCase("Completed")==0){
        	isGenerated = true;
        }

        if(!isGenerated){
//	        Thread.sleep(10000);
	        try{
		    	snapshotNode = generatePDF(snapshotNode, workspace);
		    	if(snapshotNode == null) throw new Exception("generatePDF() returned null.");
		    	else{
		    		this.setPdfStatus(workspace, snapshotNode, "Completed");
		    	}
	        }
	        catch(Exception ex){
	        	ex.printStackTrace();
	        	this.setPdfStatus(workspace, snapshotNode, "Error");
	    		throw new Exception("Failed to generate PDF artifact!");
	        }
        }
    	return populateSnapshotProperties(snapshotNode);
    }

    public EmsScriptNode generatePDF( EmsScriptNode snapshotNode, WorkspaceNode workspace ) throws Exception {
        this.snapshotName = snapshotNode.getSysmlId();
        if(this.snapshotName == null || this.snapshotName.isEmpty()) throw new Exception("Failed to retrieve snapshot Id!");

        ChildAssociationRef childAssociationRef =
                this.services.getNodeService()
                             .getPrimaryParent( snapshotNode.getNodeRef() );
        if(childAssociationRef == null) throw new Exception("Failed to retrieve snapshot association reference!");

        EmsScriptNode snapshotFolderNode =
                new EmsScriptNode( childAssociationRef.getParentRef(),
                                   this.services );
        if(snapshotFolderNode == null) throw new Exception("Failed to retrieve snapshot folder!");

        Date timestamp = (Date)snapshotNode.getProperty("view2:timestamp");
        DocBookWrapper docBookWrapper = new DocBookWrapper( this.snapshotName, snapshotNode );

        if ( !hasPdfNode( snapshotNode ) ) {
            log( LogLevel.INFO, "Generating PDF..." );
            docBookWrapper.savePdfToRepo(snapshotFolderNode, workspace, timestamp );
        }
        return snapshotNode;
    }

    private JSONObject getChildrenViews(String nodeId){
    	JSONObject childNode = null;
    	for(int j=0; j < view2view.length(); j++){
        	JSONObject tmpJson;
			try {
				tmpJson = this.view2view.getJSONObject(j);
				String tmpId = (String)tmpJson.opt("id");
				if(tmpId == null || tmpId.isEmpty()) tmpId = tmpJson.optString(Acm.SYSMLID);
				if(tmpId == null || tmpId.isEmpty()) continue;
	        	if(tmpId.equals(nodeId)){
	        		childNode = tmpJson;
	        		break;
	        	}
			} catch (JSONException e) {
				System.out.println("Failed to retrieve view2view children views for: " + nodeId);
			}
        }
    	return childNode;
    }

    private String getDocumentElementContent( DocumentElement elem ) {
        if ( elem instanceof DBParagraph ) return ( (DBParagraph)elem ).getText().toString();

        return "";
    }

    private String getEmail(String userName) {
    	try{
    		if(nodeService == null) nodeService = this.services.getNodeService();
    		if(nodeService == null) System.out.println("NodeService is not instantiated!");

    		NodeRef person = getUserProfile(userName);
			if(person == null) return "";
			return (String)nodeService.getProperty(person, ContentModel.PROP_EMAIL);
    	}
    	catch(Exception ex){
    		System.out.println("Failed to get email address for " + userName);
    	}
    	return "";
	}

    /**
     * Retrieve the snapshot folder for the view (goes up chain until it hits ViewEditor)
     *
     * @param viewNode
     * @return
     */
    public static EmsScriptNode getSnapshotFolderNode(EmsScriptNode viewNode) {
        EmsScriptNode sitesFolder = getSitesFolder(viewNode.getWorkspace());
        if ( sitesFolder == null ) {
            return null;
        }

        EmsScriptNode snapshotNode = sitesFolder.childByNamePath("/snapshots");

        if (snapshotNode == null) {
            snapshotNode = sitesFolder.createFolder("snapshots");
            if (snapshotNode != null) snapshotNode.getOrSetCachedVersion();
            sitesFolder.getOrSetCachedVersion();
        }

        if ( snapshotNode == null ) {
            return null;
        }

        EmsScriptNode snapshotDateFolder = NodeUtil.getOrCreateDateFolder(snapshotNode);

        return snapshotDateFolder;
    }

    private String getHostname(){
        	SysAdminParams sysAdminParams = this.services.getSysAdminParams();
        	String hostname = sysAdminParams.getAlfrescoHost();
        	if(hostname.startsWith("ip-128-149")) hostname = "localhost";
        	return String.format("%s://%s", sysAdminParams.getAlfrescoProtocol(), hostname);
    }

    public static EmsScriptNode getHtmlZipNode( EmsScriptNode snapshotNode ) {
        NodeRef node = (NodeRef)snapshotNode.getProperty( "view2:htmlZipNode" );
        if(node == null) return null;
        return new EmsScriptNode( node, snapshotNode.getServices() );
    }

    public static String getHtmlZipStatus( EmsScriptNode snapshotNode ) {
        return (String)snapshotNode.getProperty( "view2:htmlZipStatus" );
    }

    public static EmsScriptNode getPdfNode( EmsScriptNode snapshotNode ) {
        NodeRef node = (NodeRef)snapshotNode.getProperty( "view2:pdfNode" );
        if(node == null) return null;
        return new EmsScriptNode( node, snapshotNode.getServices() );
    }

    public static String getPdfStatus( EmsScriptNode snapshotNode ) {
        return (String)snapshotNode.getProperty( "view2:pdfStatus" );
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


    private Date getTimestamp(WebScriptRequest req){
    	if(req == null) return null;
    	String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        return dateTime;
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
            if(spec != null){
	            content = (String)spec.opt( transcludedType );
	            if ( content != null && !content.isEmpty() ) return content;
            }
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


    private NodeRef getUserProfile(String userName){
    	if(personService == null) personService = this.services.getPersonService();
    	if(personService == null){
    		System.out.println("PersonService is not instantiated.");
    		return null;
    	}
    	return personService.getPerson(userName, false);
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

    private String handleEmbeddedImage( String id, String inputString, DBSection section)
    {
    	if(id == null || id.isEmpty()) return "";
    	if(inputString == null || inputString.isEmpty()) return "";

    	Document document = Jsoup.parseBodyFragment(inputString);
    	if(document == null) return "";

    	Elements images = document.getElementsByTag("img");

    	for(Element image : images){
    		String src = image.attr("src");
    		if(src == null) continue;
    		if(src.toLowerCase().startsWith("http")){
    			String hostname = getHostname();

                try{
                	URL url = new URL(src);
                	String embedHostname = String.format("%s://%s", url.getProtocol(), url.getHost());
                	if(embedHostname.compareToIgnoreCase(hostname)==0){
                		String alfrescoContext = "workspace/SpacesStore/";	//this.services.getSysAdminParams().getAlfrescoContext();
                		String filePath = url.getFile();
                		if(filePath == null || filePath.isEmpty()) return "";

                		String nodeId = null;
                		if(filePath.contains(alfrescoContext)){
                			//filePath = "alfresco/d/d/" + filePath.substring(filePath.indexOf(alfrescoContext));
                			nodeId = filePath.substring(filePath.indexOf(alfrescoContext) + alfrescoContext.length());
                			nodeId = nodeId.substring(0, nodeId.indexOf("/"));
                		}
                		if(nodeId == null || nodeId.isEmpty()) return "";

                		String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
                		DBImage dbImage = retrieveEmbeddedImage(nodeId, filename, null, null);
                		String inlineImageTag = buildInlineImageTag(nodeId, "images/" + filename);
                		//section.addElement(dbImage);
                		image.before(inlineImageTag);
                		image.remove();

                		//if(imgFilename != null && !imgFilename.isEmpty()){
                			//image.attr("src", imgFilename);
                		//}
                	}
                	//http://localhost:8081/share/proxy/alfresco/api/node/content/workspace/SpacesStore/74cd8a96-8a21-47e5-9b3b-a1b3e296787d/graph.JPG
                }
                catch(Exception ex){
                	System.out.println("[WARNING]: Failed to retrieve embedded image.");
                	ex.printStackTrace();
                }
    		}
    	}
    	return document.body().html().toString();
    }

    private JSONObject handleGenerateArtifacts( JSONObject postJson,
                                     EmsScriptNode siteNode, Status status, WorkspaceNode workspace ) throws JSONException {
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

            setArtifactsGenerationStatus(workspace, postJson);
            startAction( jobNode, siteName, postJson, workspace );
            return postJson;
        }
        catch ( JSONException ex ) {
            log( LogLevel.ERROR, "Failed to create snapshot job!" );
            ex.printStackTrace();
        }
        catch(Exception ex){
        	ex.printStackTrace();
        	log( LogLevel.ERROR, "Failed to create snapshot job!" );
        }
        finally {
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
        while(true){
	        if ( cirRefList.size() > index ) break;
	        else{
	            list = new ArrayList< String >();
	            cirRefList.add( list );
	        }
        }
        list = cirRefList.get( index );
        if(list == null){
        	System.out.println("[WARNING]: Failed to retrieve circular reference list at index: " + index);
        	return inputString;
        }
        list.add( id + transclusionType );
        index++;
        String result = parseTransclusionName( cirRefList, index, inputString );
        result = parseTransclusionDoc( cirRefList, index, result );
        result = parseTransclusionVal( cirRefList, index, result );
        return result;
    }

    public static boolean hasHtmlZip( EmsScriptNode snapshotNode ) {
        return snapshotNode.hasAspect( "view2:htmlZip" );
    }

    /**
     *
     * @param snapshotNode
     * @return
     */
    public static boolean hasHtmlZipNode( EmsScriptNode snapshotNode ) {
        boolean hasNode = false;
        if(snapshotNode.hasAspect( "view2:htmlZip" )){
        	EmsScriptNode node = getHtmlZipNode(snapshotNode);
        	if(node != null) hasNode = true;
        }
        return hasNode;
    }

    public static boolean hasPdf( EmsScriptNode snapshotNode ) {
        return snapshotNode.hasAspect( "view2:pdf" );
    }

    public static boolean hasPdfNode( EmsScriptNode snapshotNode ) {
    	boolean hasNode = false;
        if(snapshotNode.hasAspect( "view2:pdf" )){
        	EmsScriptNode node = getPdfNode(snapshotNode);
        	if(node != null) hasNode = true;
        }
        return hasNode;
    }

    private String HtmlSanitize( String s ) {
    	Document document = Jsoup.parseBodyFragment(s);
    	removeHtmlTags(document);
    	StringBuffer sb = new StringBuffer();
    	traverseHtml(document.body(), sb);
    	return sb.toString();
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
		if(inputString == null || inputString.isEmpty()) return inputString;

		Document document = Jsoup.parseBodyFragment(inputString);
		if(document == null || document.body()==null){
			System.out.println("[WARNING]: Failed to parse HTML fragment: " + inputString);
			return inputString;
		}

		Elements elements = document.getElementsByTag("mms-transclude-doc");
		if(elements == null || elements.size() < 1) return document.body().html();

		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(id == null || id.isEmpty()){
				System.out.println("[WARNING]: Failed to parse transclusion doc Id!");
				System.out.println(element.html());
				element.before("[cannot parse Id for " + element.text() + "]");
				element.remove();
				continue;
			}

			if(isCircularReference(id, "documentation", cirRefList, index)){
				System.out.println("[WARNING]: Circular reference!");
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}

			String transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
			EmsScriptNode nameNode = findScriptNodeById(id, null, null, false); //snapshot => ok to get the latest from 'master'
			if(nameNode == null){
				System.out.println("[WARNING]: Failed to find EmsScriptNode Id " + id);
				element.before(transcluded);
				element.remove();
			}
			else{
				try {
					JSONObject jsObj = nameNode.toJSONObject(null);
					if(jsObj == null){
						System.out.println("[WARNING]: JSONObject is null");
						element.before(transcluded);
						element.remove();
					}
					else{
						String doc = getTranscludedContent(jsObj, "documentation");
						if(doc != null && !doc.isEmpty()){
							transcluded = doc;
							while(true){
								transcluded = handleTransclusion(id, "doc", transcluded,
								                                 cirRefList, index);
								if(transcluded.compareToIgnoreCase(doc) == 0) break;
								doc = transcluded;
							}
						}
						if(transcluded == null || transcluded.isEmpty()) transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
						element.before(transcluded);
						element.remove();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					System.out.println("[WARNING]: Failed to transclude doc for Id: " + id);
					System.out.println(element.html());
					e.printStackTrace();
				}
			}
		}
		return document.body().html();
	}

	private String parseTransclusionName(List<List<String>> cirRefList, int index, String inputString){
		if(inputString == null || inputString.isEmpty()) return inputString;
		Document document = Jsoup.parseBodyFragment(inputString);
		if(document == null || document.body() == null){
			System.out.println("[WARNING]: Failed to parse HTML fragment: " + inputString);
			return inputString;
		}

		Elements elements = document.getElementsByTag("mms-transclude-name");
		if(elements == null || elements.size() < 1) return document.body().html();

		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(id == null || id.isEmpty()){
				System.out.println("[WARNING]: Failed to parse transclusion name Id!");
				System.out.println(element.html());
				element.before("[cannot parse Id for " + element.text() + "]");
				element.remove();
				continue;
			}

			if(isCircularReference(id, "name", cirRefList, index)){
				System.out.println("[WARNING]: Circular reference!");
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}

			String transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
			EmsScriptNode nameNode = findScriptNodeById(id, null, null, false); // snapshot => ok to get latest
			if(nameNode == null){
				System.out.println("[WARNING]: Failed to find EmsScriptNode Id: " + id);
				element.before(transcluded);
				element.remove();
				continue;
			}

			try {
				JSONObject jsObj = nameNode.toJSONObject(null);
				if(jsObj == null){
					System.out.println("[WARNING]: JSONObject is null");
					element.before(transcluded);
					element.remove();
				}
				else{
					String name = getTranscludedContent(jsObj, "name");
					if(name != null && !name.isEmpty()){
						transcluded = name;
						while(true){
							transcluded = handleTransclusion(id, "name", transcluded, cirRefList, index);
							if(transcluded.compareToIgnoreCase(name) == 0) break;
							name = transcluded;
						}
					}
					if(transcluded == null || transcluded.isEmpty()) transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
					element.before(transcluded);
					element.remove();
				}
			} catch (JSONException e) {
				System.out.println("[WARNING]: Failed to transclude name for Id: " + id);
				System.out.println(element.html());
				e.printStackTrace();
			}
		}
		return document.body().html();
	}

	private String parseTransclusionVal(List<List<String>> cirRefList, int index, String inputString){
		if(inputString == null || inputString.isEmpty()) return inputString;

		Document document = Jsoup.parseBodyFragment(inputString);
		if(document == null || document.body() == null){
			System.out.println("[WARNING]: Failed to parse HTML fragment: " + inputString);
			return inputString;
		}

		Elements elements = document.getElementsByTag("mms-transclude-val");
		if(elements == null || elements.size() < 1) return document.body().html();

		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(id == null || id.isEmpty()){
				System.out.println("[WARNING]: Failed to parse transclusion value Id!");
				System.out.println(element.html());
				element.before("[cannot parse Id for " + element.text() + "]");
				element.remove();
				continue;
			}

			if(isCircularReference(id, "value", cirRefList, index)){
				System.out.println("[WARNING]: Circular reference!");
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}

			String transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
			EmsScriptNode nameNode = findScriptNodeById(id, null, null, false); // snapshot => ok to get latest
			if(nameNode == null){
				System.out.println("[WARNING]: Failed to find EmsScriptNode Id " + id);
				element.before(transcluded);
				element.remove();
				continue;
			}
			else{
				try {
					JSONObject jsObj = nameNode.toJSONObject(null);
					if(jsObj == null){
						System.out.println("JSONObject is null");
						element.before(transcluded);
						element.remove();
						continue;
					}
					else{
						String val = getTranscludedVal(jsObj);
						element.before(val);
						element.remove();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					System.out.println("[WARNING]: Failed to transclude Id: " + id);
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
        	HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        	String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
        	JSONArray formats = new JSONArray();
            if ( hasPdfNode( snapshotNode ) ) {
                EmsScriptNode pdfNode = getPdfNode( snapshotNode );
                JSONObject pdfJson = new JSONObject();
                pdfJson.put("status", "Completed");
                pdfJson.put("type", "pdf");
                pdfJson.put("url", contextUrl + pdfNode.getUrl());
                formats.put(pdfJson);
            }
            if ( hasHtmlZipNode( snapshotNode ) ) {
                EmsScriptNode htmlZipNode = getHtmlZipNode( snapshotNode );
                JSONObject htmlJson = new JSONObject();
                htmlJson.put("status", "Completed");
                htmlJson.put("type","html");
                htmlJson.put("url", contextUrl + htmlZipNode.getUrl());
                formats.put(htmlJson);
            }

            snapshoturl.put( "formats", formats );
        }
        return snapshoturl;
    }


    /**
     * replaces HTML tags to end up with only the HTML text.
     * @param elem: JSoup Element
     */
    private void removeHtmlTag(Element elem){
    	if(elem == null) return;
    	String tagName = elem.tagName().toUpperCase();
    	//System.out.println("tag name: " + tagName);
    	switch(tagName){
    	case "P":
    	case "DIV":
    	case "BODY":
    	case "INLINEMEDIAOBJECT":
    	case "IMAGEOBJECT":
    		for(Element child:elem.children()){
    			//System.out.println("removing Html tags...");
    			removeHtmlTag(child);
    		}
    		break;
    	case "A":
    		//System.out.println("Anchor tag...");
			elem.before(elem.text() + " (" + elem.attr("href") + ") ");
    		elem.remove();
    		break;
		default:
    			//System.out.println("replacing elem with its text...");
			elem.before(elem.text());
			for(Element child:elem.children()){
				//System.out.println("removing Html tags...");
				removeHtmlTag(child);
			}
			//System.out.println("removing element...");
			elem.remove();
    	}
    }


    private void removeHtmlTags(Document doc){
    	/*
    	Elements elems = doc.getElementsByTag("A");
    	for(Element e:elems){
    		e.before(e.text() + " (" + e.attr("href") + ") ");
    		e.remove();
    	}

    	removeHtmlTag(doc, "B");
    	removeHtmlTag(doc, "BIG");
    	removeHtmlTag(doc, "CENTER");
    	removeHtmlTag(doc, "EM");
    	removeHtmlTag(doc, "FONT");
    	removeHtmlTag(doc, "I");
    	removeHtmlTag(doc, "SMALL");
    	removeHtmlTag(doc, "SPAN");
    	removeHtmlTag(doc, "STRIKE");
    	removeHtmlTag(doc, "STRONG");
    	removeHtmlTag(doc, "SUB");
    	removeHtmlTag(doc, "U");
    	*/
    	for(Element child : doc.body().children()){
    		removeHtmlTag(child);
    	}
	}

    private DBImage retrieveEmbeddedImage(String nodeId, String imgName, String workspace, Object timestamp){
		//NodeUtil.getNodeRefAtTime(nodeId, workspace, timestamp);
		NodeRef imgNodeRef = NodeUtil.getNodeRefFromNodeId(nodeId);
		if(imgNodeRef == null) return null;

		String imgFilename = this.docBookMgr.getDBDirImage() + File.separator + imgName;
		File imgFile = new File(imgFilename);
		ContentReader imgReader;
		imgReader = this.services.getContentService().getReader(imgNodeRef, ContentModel.PROP_CONTENT);
		if(!Files.exists(Paths.get(this.docBookMgr.getDBDirImage()))){
			if(!new File(this.docBookMgr.getDBDirImage()).mkdirs()){
				System.out.println("Failed to create directory for " + this.docBookMgr.getDBDirImage());
			}
		}
		imgReader.getContent(imgFile);

		DBImage image = new DBImage();
		image.setId(nodeId);
		image.setFilePath("images/" + imgName);
		return image;

		/*
		ResultSet rs = findNodeRef(nodeId);
		ContentReader imgReader;
		for (NodeRef nr: rs.getNodeRefs()) {
			imgReader = this.services.getContentService().getReader(nr, ContentModel.PROP_CONTENT);
			if(!Files.exists(Paths.get(this.docBookMgr.getDBDirImage()))){
				if(!new File(this.docBookMgr.getDBDirImage()).mkdirs()){
					System.out.println("Failed to create directory for " + this.docBookMgr.getDBDirImage());
				}
			}
			imgReader.getContent(imgFile);
			break;
		}
		return imgFilename;
		*/
    }

    //nodeUtil.getNodeRefById(id)	//need to get the correct version as well. and workspace

    private JSONObject saveAndStartAction(WebScriptRequest req, Status status, WorkspaceNode workspace) {
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
					jsonObject = handleGenerateArtifacts(postJson, siteNode, status, workspace);
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

    private void saveImage( DBImage image, EmsScriptNode imageEmsScriptNode ) throws Exception {
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
        EmsScriptNode fNode = new EmsScriptNode( imgNodeRef, getServices() );
        fNode.makeSureNodeRefIsNotFrozen();
        fNode.transactionCheck();
        Files.write( filePath, binaryData );
        image.setFilePath( dbDirName.relativize( filePath ).toString() );
    }

    private void setDocumentElementContent( DocumentElement elem, String s ) {
        if ( elem instanceof DBParagraph ) ( (DBParagraph)elem ).setText( s );
    }

    private void setArtifactsGenerationStatus(WorkspaceNode workspace, JSONObject postJson) throws Exception{
    	try{
	       	EmsScriptNode snapshotNode = findScriptNodeById(postJson.getString("id"), workspace, null, false);
	        if(snapshotNode == null){
	        	throw new Exception("Failed to find snapshot with Id: " + postJson.getString("id"));
	        }

			ArrayList<String> formats = getSnapshotFormats(postJson);
			for(String format:formats){
				if(format.compareToIgnoreCase("pdf") == 0){
					if(SnapshotPost.getPdfNode(snapshotNode)==null){
	    				snapshotNode.createOrUpdateAspect("view2:pdf");
	    	            snapshotNode.createOrUpdateProperty("view2:pdfStatus", "Generating");
//	    	    		System.out.println("set PDF status => " + "Generating...");
	    	    		}
	        	}
	        	else if(format.compareToIgnoreCase("html") == 0){
	        		if(SnapshotPost.getHtmlZipNode(snapshotNode)==null){
		        		snapshotNode.createOrUpdateAspect("view2:htmlZip");
		                snapshotNode.createOrUpdateProperty("view2:htmlZipStatus", "Generating");
//		        		System.out.println("set HTML status => " + "Generating...");
	        		}
	        	}
			}
    	}
    	catch(Exception ex){
    		ex.printStackTrace();
    		throw new Exception("Failed to set artifact generation status!");
    	}
    }


    private void setHtmlZipStatus(EmsScriptNode node, String status){
    	if(node==null) return;
    	node.createOrUpdateAspect("view2:htmlZip");
		node.createOrUpdateProperty("view2:htmlZipStatus", status);
//		System.out.println("set HTML status => " + status);
    }

    private void setPdfStatus(WorkspaceNode workspace, EmsScriptNode node, String status){
    	if(node==null) return;
    	node.createOrUpdateAspect("view2:pdf");
		node.createOrUpdateProperty("view2:pdfStatus", status);
		node.getOrSetCachedVersion();
//		System.out.println("set PDF status => " + status);
    }

    /**
	 * Kick off the actual action in the background
	 * @param jobNode
	 * @param siteName
	 * @param snapshot Id
	 * @param snapshot format types
	 */
	public void startAction(EmsScriptNode jobNode, String siteName, JSONObject postJson, WorkspaceNode workspace) throws JSONException {
		String userEmail = null;
		String userName = AuthenticationUtil.getFullyAuthenticatedUser();
		if(userName == null || userName.isEmpty())
			System.out.println("Failed to get authentiated user name!");
		else
			userEmail = getEmail(userName);

		if(userEmail == null || userEmail.isEmpty()) System.out.println("Failed to get user email address!");

		ArrayList<String> formats = getSnapshotFormats(postJson);
        ActionService actionService = services.getActionService();
        Action snapshotAction = actionService.createAction(SnapshotArtifactsGenerationActionExecuter.NAME);
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_SITE_NAME, siteName);
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_SNAPSHOT_ID, postJson.getString("id"));
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_FORMAT_TYPE, formats);
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_USER_EMAIL, userEmail);
        snapshotAction.setParameterValue(SnapshotArtifactsGenerationActionExecuter.PARAM_WORKSPACE, workspace);

       	services.getActionService().executeAction(snapshotAction, jobNode.getNodeRef(), true, true);
	}


    private void traverseElements( DBSection section, EmsScriptNode node, WorkspaceNode workspace, Date timestamp )
            throws Exception {
    	if(node == null) return;
    	//1st process the node
    	JSONArray contains = node.getView().getContainsJson();
        createDBSectionContainment( section, contains, workspace, timestamp );

        //then process it's contains:children if any
    	String nodeId = node.getSysmlId();
    	JSONObject viewJson = getChildrenViews(nodeId);
    	if(viewJson == null) return;

    	JSONArray childrenViews = viewJson.getJSONArray("childrenViews");
        if(childrenViews == null) throw new Exception("Failed to retrieve 'childrenViews'.");
        for(int k=0; k< childrenViews.length(); k++){
        	String childId = childrenViews.getString(k);
        	if(childId.equals(nodeId)) continue;
        	EmsScriptNode childNode = findScriptNodeById(childId, workspace, timestamp, false);
        	DBSection subSection = emsScriptNodeToDBSection(childNode, false, workspace, timestamp);
        	section.addElement(subSection);
        }
    }

    private void traverseHtml(Element elm, StringBuffer sb){
    	if(elm == null) return;
    	if(sb == null) return;

    	if(!elm.isBlock()){
    		sb.append(" ");
    	}

    	if(elm.ownText().length() > 0){
    		sb.append("<![CDATA[");
    		sb.append(elm.ownText());
        	sb.append("]]>");
    	}

    	if(elm.children() != null && elm.children().size() > 0){
    		for(Element e: elm.children()){
    			if(e.tagName().compareToIgnoreCase("inlinemediaobject") == 0){
    				sb.append(e.outerHtml());
    				continue;
    			}
    			traverseHtml(e,sb);
    		}
    	}

    	if(elm.isBlock()) sb.append("<?linebreak?>");
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return false;
    }
}
