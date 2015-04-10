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
import gov.nasa.jpl.view_repo.webscripts.HtmlTable.TablePart;

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
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
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
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
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
    static Logger logger = Logger.getLogger(SnapshotPost.class);
    
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
        SnapshotPost instance = new SnapshotPost(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        printHeader( req );
        //clearCaches();

        WorkspaceNode workspace = getWorkspace( req );
        Date timestamp = getTimestamp(req);

        Map< String, Object > model = new HashMap< String, Object >();
        log( Level.INFO, "Starting snapshot creation or snapshot artifact generation..." );
        try {
            JSONObject reqPostJson = //JSONObject.make( 
                    (JSONObject)req.parseContent();// );
            if ( reqPostJson != null ) {
                log( Level.INFO, "Generating snapshot artifact..." );
                //SnapshotPost instance = new SnapshotPost( repository, services );
                JSONObject result = //instance.
                        saveAndStartAction( req, status, workspace );
                //appendResponseStatusInfo( instance );

                status.setCode( responseStatus.getCode() );
                if ( result == null ) {
                    model.put( "res", createResponseJson() );
                } else {
                    model.put( "res", result );
                }
                printFooter();
                return model;
            } else {
                log( Level.INFO, "Creating snapshot..." );
                String viewId = getViewId( req );
                EmsScriptNode snapshotNode = null;
                DateTime now = new DateTime();
                DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

                EmsScriptNode topview = findScriptNodeById( viewId, workspace, null, true );
                EmsScriptNode snapshotFolderNode =
                        getSnapshotFolderNode( topview );
                if ( snapshotFolderNode == null ) {
                    log( Level.ERROR,
                         HttpServletResponse.SC_BAD_REQUEST, "Cannot create folder for snapshot" );
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
                    log( Level.ERROR,
                         HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating snapshot node" );
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
                        model.put( "res", NodeUtil.jsonToString( snapshoturl, 4 ) );
                    } catch ( JSONException e ) {
                        e.printStackTrace();
                        log( Level.ERROR,
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating JSON for snapshot");
                    }
                }
                status.setCode( responseStatus.getCode() );
                if ( status.getCode() != HttpServletResponse.SC_OK ) {
                    model.put( "res", createResponseJson() );
                }
            }
        } catch ( Exception ex ) {
        	status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log( Level.ERROR,
                 "Failed to create snapshot or snapshot artifact! %s",
                         ex.getMessage() );
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

    private String buildInlineImageTag(String nodeId, DBImage dbImage){
    	StringBuffer sb = new StringBuffer();
//    	sb.append(String.format("<figure xml:id=\"%s\" pgwide=\"1\">", nodeId));
//    	sb.append(String.format("<title>%s</title>", dbImage.getTitle()));
    	sb.append("<inlinemediaobject>");
        sb.append("<imageobject>");
        sb.append(String.format("<imagedata scalefit='1' width='100%%' fileref='%s' />", dbImage.getFilePath()));
        sb.append("</imageobject>");
        sb.append("</inlinemediaobject>");
//        sb.append("</figure>");
    	return sb.toString();
    }

    private DBParagraph createDBParagraph( JSONObject obj, DBSection section, WorkspaceNode workspace, Date timestamp  ) {
    	if(obj == null) return null;
        String srcType = (String)obj.opt( "sourceType" );
        String src = (String)obj.opt( "source" );
        String srcProp = (String)obj.opt( "sourceProperty" );

        DBParagraph p = new DBParagraph();
        p.setId( src );
    	String s = null;
        if (srcType != null && srcType.compareTo( "reference" ) == 0 ) {
            EmsScriptNode node = findScriptNodeById( src, workspace, timestamp, false );
            if(node == null){
            	log(Level.WARN, String.format("Failed to create DBParagraph! Failed to find EmsScriptNode with Id: %s", src));
            }
            else{
	            if (srcProp != null && srcProp.compareTo( "value" ) == 0 ) {
	                List< NodeRef > nodeRefs = (List< NodeRef >)node.getNodeRefProperty( Acm.SYSML + srcProp, timestamp, workspace );
	                if(nodeRefs == null){
	                	log(Level.WARN, String.format("Failed to create DBParagraph! Failed to find values node references for %s.", src));
	                	return null;
	                }

	                StringBuffer sb = new StringBuffer();
	                int size = nodeRefs.size();
	                for ( int i = 0; i < size; i++ ) {
	                    NodeRef nodeRef = nodeRefs.get( i );
	                    if(nodeRef == null){
	                    	log(Level.WARN, String.format("Failed to get value for node ref '%s' at index: %d", src, i));
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

	                        if ( value instanceof String ) sb.append( (String)value );
	                        else sb.append( value );
	                    } else {
	                        try {
	                            Object valObj = extractNodeValue( valueNode );
	                            if ( valObj == null ) continue;
	                            if ( valObj instanceof String ) sb.append( (String)valObj );
	                            else sb.append( valObj );
	                        }
	                        catch ( Exception ex ) {
								log( Level.WARN,"Problem extracting node value from %s",node.toJSON() );
//	                            log( LogLevel.WARNING, "Problem extracting node value from " + node.toJSON() );
	                        }
	                    }
	                    sb.append(" ");
	                }
	                s = sb.toString();
	                //p.setText( sb.toString() );
	            }
	            else {
	                s = (String)node.getProperty( Acm.SYSML + srcProp );
	            }

	            s = handleTransclusion( src, srcProp, s, null, 0, workspace, timestamp );
            }
        }
        else {
        	//p.setText( HtmlSanitize( (String)obj.opt( "text" ) ) );
            
            if ( srcProp != null && !srcProp.isEmpty() ) {
                s = (String)obj.opt( Acm.SYSML + srcProp );
                s = handleTransclusion( src, srcProp, s, null, 0, workspace, timestamp );
            }
            else{ 
            	s = obj.optString("text");
            	s = handleTransclusion( src, "text", s, null, 0, workspace, timestamp );
            }
        }
        s = handleEmbeddedImage(s);
        s = HtmlSanitize( s );
        if(s != null && !s.isEmpty()){ 
        	p.setText(s);
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
        value = handleTransclusion( UUID.randomUUID().toString(), "text", value, null, 0, workspace, timestamp );
        value = handleEmbeddedImage(value);
        value = HtmlSanitize(value);
        text.setText(value);
        return text;
    }

    private DBBook createDocBook( EmsScriptNode product ) {
        String title = (String)product.getProperty( Acm.ACM_NAME );
        DBBook docBook = new DBBook();
        // need to make sure that all text is properly escaped for XML inclusion, e.g., & => &amp;
        docBook.setTitle( replaceXmlEntities(title) );
        docBook.setTitlePageLegalNotice( "This Document has not been reviewed for export control. Not for distribution to or access by foreign persons." );
        docBook.setFooterLegalNotice( "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release." );
        String author =
                replaceXmlEntities( getUserProfile( product,
                                (String)product.getProperty( Acm.ACM_AUTHOR ) ) );
        docBook.setAuthor( Arrays.asList( author ) );
        return docBook;
    }

    public DocBookWrapper createDocBook( EmsScriptNode product, String productId,
                                          String snapshotName, String contextPath,
                                          EmsScriptNode snapshotNode,
                                          WorkspaceNode workspace, Date timestamp, StringBuffer response) throws Exception {
        if ( product == null ) {
            log( Level.WARN, "null [view] input parameter reference." );
            return null;
        }

        this.snapshotName = snapshotName;
        docBookMgr = new DocBookWrapper( snapshotName, snapshotNode, false );
        try {
            DBBook docBook = createDocBook( product );
            //docBook.setRemoveBlankPages( true );

            View productView = product.getView();
            if(productView == null) throw new Exception("Missing document's structure; expected to find product's view but it's not found.");

            JSONArray contains = productView.getContainsJson(timestamp, workspace);
            if(contains == null || contains.length()==0){ throw new Exception("Missing document's structure; expected to find document's 'contains' JSONArray but it's not found."); }

            for(int i=0; i < contains.length(); i++){
	            JSONObject contain = contains.getJSONObject(0);
	            if(contain == null) throw new Exception(String.format("Missing document's structure; expected to find contain JSONObject at index: %d but it's not found.", i));

	            String sourceType = (String)contain.opt("sourceType");
	            String source = (String)contain.opt("source");
	            if(source == null || source.isEmpty()) throw new Exception("Missing document's structure; expected to find contain source property but it's not found.");

	            this.view2view = productView.getViewToViewPropertyJson();
	            if(view2view == null || view2view.length()==0) throw new Exception ("Missing document's structure; expected to find document's 'view2view' JSONArray but it's not found.");

	            JSONObject v2vChildNode = getChildrenViews(source);
	            if(v2vChildNode == null) throw new Exception(String.format("Missing document's structure; expected to find 'view2view' childnode for: %s but it's not found.", source));

	            JSONArray childrenViews = v2vChildNode.getJSONArray("childrenViews");
	            if(childrenViews == null) throw new Exception("Missing document's structure; expected to find 'view2view' childnode's 'childrenViews' but it's not found.");

	            String documentation = (String)product.getProperty("sysml:documentation");
	            if(documentation != null && !documentation.isEmpty()){
	            	documentation = handleTransclusion(product.getId(), "documentation", documentation, null, 0, workspace, timestamp);
	            	documentation = handleEmbeddedImage(documentation);
	            	documentation = HtmlSanitize(documentation);
	            	docBook.setPreface(documentation);
//		            DBPreface preface = new DBPreface();
//		            preface.setText(documentation);
//		            docBook.addElement(preface);
	            }

	            for(int k=0; k< childrenViews.length(); k++){
	            	String childId = childrenViews.getString(k);
	            	if(childId == null || childId.isEmpty()) throw new Exception(String.format("Missing document's structure; expected to find childrenViews[%d] Id but it's not found.", k));

	            	EmsScriptNode childNode = findScriptNodeById(childId, workspace, timestamp, false);
	            	if(childNode == null) throw new Exception(String.format("Failed to find EmsScriptNode with Id: %s", childId));
	            	//creating chapters
	            	DocumentElement section = emsScriptNodeToDBSection(childNode, true, workspace, timestamp);
	            	if(section != null) docBook.addElement(section);
	            }
            }
            docBookMgr.setDBBook(docBook);
            docBookMgr.save();
        }
        catch ( Exception ex ) {
        	log( Level.ERROR, "\nUnable to create DBBook! Failed to parse document.\n%s", ex.getMessage());
        	//log( LogLevel.ERROR, "\nUnable to create DBBook! Failed to parse document.\n" + ex.getStackTrace() );
            ex.printStackTrace();
//            setArtifactsGenerationStatus(snapshotNode);
            throw new Exception( "Unable to create DBBook! Failed to parse document.\n", ex );
        }
        finally{
        	response.append(this.response.toString());
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
                response.append(String.format("Warning: Unexpected type: %s", getType(obj)));
            	log( Level.WARN, "Unexpected type: %s", getType( obj ));
                break;
        }
        return e;
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

    private DocumentElement createImage( JSONObject obj, WorkspaceNode workspace, Date timestamp  ) {
        DBImage image = new DBImage();
        image.setId( getSymlId( obj ) );

        String id = (String)obj.opt( Acm.SYSMLID );
        EmsScriptNode imgNode = findScriptNodeById( id, workspace, timestamp, false );  //snapshot => ok to get the latest from 'master'
        if ( imgNode == null ) {
            // TODO error handling
            return image;
        } else {
            ResultSet resultSet = null;
            try {
                image.setTitle( (String)imgNode.getProperty( Acm.ACM_NAME ) );

                String fileName = id + ".svg"; 
                
                resultSet = NodeUtil.luceneSearch( "@name:" + fileName );
                if ( resultSet != null && resultSet.length() > 0 ) {
                    EmsScriptNode node =
                            new EmsScriptNode( resultSet.getNodeRef( 0 ),
                                               services );
                    saveImage( image, node );
                } else {
                    log( Level.WARN, "%s image file not found!", fileName );
                }
            } catch ( Exception ex ) {
                logger.error("Could not get results");
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
            

            return image;
        }
    }

    /**
     * 
     * @param view      Needs to be a spaces store reference
     * @param viewId
     * @param workspace
     * @param timestamp
     * @return
     */
    public EmsScriptNode createSnapshot( EmsScriptNode view, String viewId,
                                         WorkspaceNode workspace, Date timestamp ) {
        this.snapshotName = viewId + "_" + System.currentTimeMillis();
        log(Level.INFO, "Begin creating snapshot: \t%s", this.snapshotName);
        
        String contextPath = "alfresco/service/";
        EmsScriptNode snapshotFolder = getSnapshotFolderNode(view);
        if(snapshotFolder == null){
            log( Level.ERROR,
                 HttpServletResponse.SC_BAD_REQUEST, "Failed to get snapshot folder node!");
            log(Level.INFO, "End creating snapshot: \t\t%s", this.snapshotName);
        	return null;
        }
        log(Level.INFO, "End creating snapshot: \t\t%s",this.snapshotName);
        return createSnapshot(view, viewId, snapshotName, contextPath, snapshotFolder, workspace, timestamp);
    }

    public EmsScriptNode createSnapshot( EmsScriptNode view, String viewId,
                                         String snapshotName,
                                         String contextPath,
                                         EmsScriptNode snapshotFolder,
                                         WorkspaceNode workspace, Date timestamp) {
        EmsScriptNode snapshotNode = snapshotFolder.createNode( snapshotName, "view2:Snapshot" );
        if (snapshotNode == null) {
            	log(Level.ERROR, "Failed to create view2:Snapshot!");
            	return null;
        }
        snapshotNode.createOrUpdateProperty( "cm:isIndexed", true );
        snapshotNode.createOrUpdateProperty( "cm:isContentIndexed", false );
        snapshotNode.createOrUpdateProperty( Acm.ACM_ID, snapshotName );

        snapshotNode.createOrUpdateAspect( "view2:Snapshotable" );
        snapshotNode.createOrUpdateProperty( "view2:snapshotProduct", view.getNodeRef() );
        snapshotNode.createOrUpdateProperty( "view2:timestamp", timestamp );
        // can't update the view since it may be frozen since it can be in parent branch
//        view.createOrUpdateAspect( "view2:Snapshotable" );
//        view.appendToPropertyNodeRefs( "view2:productSnapshots", snapshotNode.getNodeRef() );

        JSONObject snapshotJson = new JSONObject();
        try {
            snapshotJson.put( "snapshot", true );
            ActionUtil.saveStringToFile( snapshotNode, "application/json", services, NodeUtil.jsonToString( snapshotJson, 4 ) );
            // Docbook is generated on demand now rather than ahead of time...
            // see SnapshotArtifactActionExecuter 
//            DocBookWrapper docBookWrapper = createDocBook( view, viewId, snapshotName, contextPath, snapshotNode, workspace, timestamp );
//            if ( docBookWrapper == null ) {
//                log( LogLevel.ERROR, "Failed to generate DocBook!" );
//                snapshotNode = null;
//            } else {
//                docBookWrapper.save();
//                docBookWrapper.saveDocBookToRepo( snapshotFolder, timestamp );
//            }
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
        String style = (String)obj.opt( "style" );
        table.setId( getSymlId( obj ) );
        table.setTitle( HtmlSanitize(title) );
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

    private List< List< DocumentElement >> createTableBody( JSONObject obj, DBSection section, DocBookTable dbTable, WorkspaceNode workspace, Date timestamp  ) throws JSONException {
        return createTableRows( obj.getJSONArray( "body" ), section, dbTable, false, workspace, timestamp );
    }

    private List<DBColSpec> createTableColSpec(int columnNum){
    	List<DBColSpec> colspecs = new ArrayList<DBColSpec>();
    	for(int i=1; i <= columnNum; i++){
    		colspecs.add(new DBColSpec(i, String.valueOf(i)));
    	}
    	return colspecs;
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

                int l=0;
                for ( l = 0; l < headerCols.length(); l++ ) {
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
                }
                if(cellContent != null){
                	te.addElement(cellContent);
                }else{
                	DBText text = new DBText();
                	text.setText(" ");
                	te.addElement(text);
                }
                rows.add( te );
            }
            list.add( rows );
        }
        
        if(isHeader){
        	dbTable.handleRowsDifferences(dbTable.header, dbTable.headerRowCount, list);
        }
        else{
        	dbTable.handleRowsDifferences(dbTable.body, dbTable.bodyRowCount, list);
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
            	if(s.compareToIgnoreCase("html")==0) s = "zip";
                jobName += "_" + s;
            }
        } catch ( JSONException ex ) {
            log( Level.ERROR, "Failed to gather job name!" );
            ex.printStackTrace();
        }
        return jobName;
    }

    public EmsScriptNode generateHTML( String snapshotId, Date dateTime, WorkspaceNode workspace ) throws Exception {
    	clearCaches( false );
        //EmsScriptNode snapshotNode = findScriptNodeById( snapshotId, workspace, null, false );
    	// lookup snapshotNode using standard lucene as snapshotId is unique across all workspaces
		ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( snapshotId, "@cm\\:name:\"", services );
		if (nodeRefs == null || nodeRefs.size() != 1) {
			throw new Exception("Failed to find snapshot with Id: " + snapshotId);
		}
		EmsScriptNode snapshotNode = new EmsScriptNode(nodeRefs.get( 0 ), services, response);
        if(snapshotNode == null) throw new Exception("Failed to find snapshot with Id: " + snapshotId);
        String status = getHtmlZipStatus(snapshotNode);
        boolean isGenerated = false;
        if(status != null && !status.isEmpty() && status.compareToIgnoreCase("Completed")==0){
        	isGenerated = true;
        	log(Level.INFO, "Zip artifacts were already generated.");
        }

        if(!isGenerated){
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
	        	throw new Exception("Failed to generate zip artifact!", ex);
	        }
        }
        //return populateSnapshotProperties( snapshotNode, dateTime, workspace );
        return snapshotNode;
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
        DocBookWrapper docBookWrapper = new DocBookWrapper( this.snapshotName, snapshotNode, false );//need workspace and timestamp


        if ( !hasHtmlZipNode( snapshotNode, timestamp, workspace ) ) {
            log( Level.INFO, "Generating zip artifact..." );
            docBookWrapper.saveHtmlZipToRepo( snapshotFolderNode, workspace, timestamp );
        }
        return snapshotNode;
    }

    
    public EmsScriptNode generatePDF(String snapshotId, Date dateTime, WorkspaceNode workspace, String siteName) throws Exception{
    	clearCaches( false );
        //EmsScriptNode snapshotNode = findScriptNodeById(snapshotId, workspace, null, false);
    	// lookup snapshotNode using standard lucene as snapshotId is unique across all workspaces
		ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( snapshotId, "@cm\\:name:\"", services );
		if (nodeRefs == null || nodeRefs.size() != 1) {
			throw new Exception("Failed to find snapshot with Id: " + snapshotId);
		}
		EmsScriptNode snapshotNode = new EmsScriptNode(nodeRefs.get( 0 ), services, response);
		if(snapshotNode == null) throw new Exception("Failed to find snapshot with Id: " + snapshotId);
        String status = getPdfStatus(snapshotNode);
        boolean isGenerated = false;
        if(status != null && !status.isEmpty() && status.compareToIgnoreCase("Completed")==0){
        	isGenerated = true;
        	log(Level.INFO, "PDF artifacts were already generated.");
        }

        if(!isGenerated){
	        try{
		    	snapshotNode = generatePDF(snapshotNode, workspace, siteName);
		    	if(snapshotNode == null) throw new Exception("generatePDF() returned null.");
		    	else{
		    		this.setPdfStatus(snapshotNode, "Completed");
		    	}
	        }
	        catch(Exception ex){
	        	ex.printStackTrace();
	        	this.setPdfStatus(snapshotNode, "Error");
	    		throw new Exception("Failed to generate PDF artifact!", ex);
	        }
        }
    	//return populateSnapshotProperties(snapshotNode, dateTime, workspace);
        return snapshotNode;
    }

    public EmsScriptNode generatePDF( EmsScriptNode snapshotNode, WorkspaceNode workspace, String siteName ) throws Exception {
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
        DocBookWrapper docBookWrapper = new DocBookWrapper( this.snapshotName, snapshotNode, false );

        if ( !hasPdfNode( snapshotNode, timestamp, workspace ) ) {
            log( Level.INFO, "Generating PDF..." );
            docBookWrapper.savePdfToRepo(snapshotFolderNode, workspace, timestamp, siteName );
        }
        return snapshotNode;
    }

    public JSONObject generatedPDFFailure(String snapshotId, Date dateTime, WorkspaceNode workspace, String siteName) throws Exception{
    	EmsScriptNode snapshotNode = getSnapshotNode(snapshotId);
    	if(snapshotNode == null) return null;
    	
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
        DocBookWrapper docBookWrapper = new DocBookWrapper( this.snapshotName, snapshotNode, true );

        docBookWrapper.savePdfFailureToRepo(snapshotFolderNode, workspace, timestamp, siteName );
        
    	return populateSnapshotProperties(snapshotNode, dateTime, workspace);
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

    private String getHostname(){
        	SysAdminParams sysAdminParams = this.services.getSysAdminParams();
        	String hostname = sysAdminParams.getAlfrescoHost();
        	if(hostname.startsWith("ip-128-149")) hostname = "localhost";
        	return String.format("%s://%s", sysAdminParams.getAlfrescoProtocol(), hostname);
    }

    public static EmsScriptNode getHtmlZipNode( EmsScriptNode snapshotNode, Date dateTime, WorkspaceNode ws ) {
        NodeRef node = (NodeRef)snapshotNode.getNodeRefProperty( "view2:htmlZipNode", true, null, null );
        if(node == null) return null;
        return new EmsScriptNode( node, snapshotNode.getServices() );
    }

    public static String getHtmlZipStatus( EmsScriptNode snapshotNode ) {
        return (String)snapshotNode.getProperty( "view2:htmlZipStatus" );
    }

    public static EmsScriptNode getPdfNode( EmsScriptNode snapshotNode, Date dateTime, WorkspaceNode ws ) {
//        NodeRef node = (NodeRef)snapshotNode.getNodeRefProperty( "view2:pdfNode", true, dateTime, ws );
    	NodeRef node = (NodeRef)snapshotNode.getNodeRefProperty( "view2:pdfNode", true, null, null );
        if(node == null) return null;
        return new EmsScriptNode( node, snapshotNode.getServices() );
    }

    public static String getPdfStatus( EmsScriptNode snapshotNode ) {
        return (String)snapshotNode.getProperty( "view2:pdfStatus" );
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
            snapshotNode.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
            if (snapshotNode != null) snapshotNode.getOrSetCachedVersion();
            sitesFolder.getOrSetCachedVersion();
        }

        EmsScriptNode snapshotDateFolder = NodeUtil.getOrCreateDateFolder(snapshotNode);

        return snapshotDateFolder;
    }
    
    private ArrayList< String > getSnapshotFormats( JSONObject postJson ) throws JSONException {
        ArrayList< String > list = new ArrayList< String >();
        JSONArray formats = postJson.getJSONArray( "formats" );
        for ( int i = 0; i < formats.length(); i++ ) {
            JSONObject jsonType = formats.getJSONObject( i );
            String formatType = jsonType.getString( "type" );
            if(!list.contains(formatType)) list.add( formatType );
        }
        return list;
    }

    private EmsScriptNode getSnapshotNode(JSONObject postJson) throws Exception{
    	if (!postJson.has( "id" )) {
	        throw new Exception("No id found in posted JSON.");
	    }
    	return getSnapshotNode(postJson.getString("id"));
    }
    
    private EmsScriptNode getSnapshotNode(String id){
    	EmsScriptNode node = null;
    	ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( id, "@cm\\:name:\"", services );
	    if (nodeRefs != null && nodeRefs.size() > 0) {
		    node = new EmsScriptNode(nodeRefs.get( 0 ), services, response);
	    }
    	return node;
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
        System.out.println( NodeUtil.jsonToString( jsonObj ) );
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
        System.out.println( NodeUtil.jsonToString( jsonObj ) );
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

    /*
     * process images not originated from MagicDraw; images inserted via VE
     */
    private String handleEmbeddedImage( String inputString)
    {
    	if(inputString == null || inputString.isEmpty()) return "";

    	Document document = Jsoup.parseBodyFragment(inputString);
    	if(document == null) return inputString;

    	Elements images = document.getElementsByTag("img");

    	for(Element image : images){
    		String src = image.attr("src");
    		if(src == null) continue;
    		try{
    			URL url = null;
    			if(!src.toLowerCase().startsWith("http")){
    				//relative URL; needs to prepend URL protocol
    				String protocol = new HostnameGet(this.repository, this.services).getAlfrescoProtocol();
//    				System.out.println(protocol + "://" + src);
    				src = src.replaceAll("\\.\\./", "");
//    				System.out.println("src: " + src);
    				url = new URL(String.format("%s://%s", protocol, src));
    			}
    			else{
	            	url = new URL(src);
    			}
    			
    			String hostname = getHostname();
                try{
                	src = src.toLowerCase();
                	String embedHostname = String.format("%s://%s", url.getProtocol(), url.getHost());
                	String alfrescoContext = "workspace/SpacesStore/";	//this.services.getSysAdminParams().getAlfrescoContext();

                	// is image local or remote resource?
                	if(embedHostname.compareToIgnoreCase(hostname)==0 || src.startsWith("/alfresco/") || src.contains(alfrescoContext.toLowerCase())){
                		//local server image > generate image tags
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
                		try{
                			DBImage dbImage = retrieveEmbeddedImage(nodeId, filename, null, null);
	                		String inlineImageTag = buildInlineImageTag(nodeId, dbImage);
	                		image.before(inlineImageTag);
	                		image.remove();

                		}
                		catch(Exception ex){
                			//in case it's not a local resource > generate hyperlink instead
                			image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ", src, url.getFile()));
                    		image.remove();
                		}
                	}
                	else{	//remote resource > generate a hyperlink
                		image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ", src, url.getFile()));
                		image.remove();
                	}
                }
                catch(Exception ex){
                	log(Level.WARN, "Failed to retrieve embedded image at %s. %s", src, ex.getMessage());
                	ex.printStackTrace();
                }
			}

            catch(Exception ex){
            	log(Level.WARN, "Failed to process embedded image at %s. %s", src, ex.getMessage());
            	ex.printStackTrace();
            }
    	}
    	return document.body().html().toString();
    }

    private JSONObject handleGenerateArtifacts( JSONObject postJson,
                                     EmsScriptNode siteNode, Status status, WorkspaceNode workspace ) throws JSONException {
        String siteName = (String)siteNode.getProperty( Acm.CM_NAME );
        EmsScriptNode jobNode = null;

        if ( !postJson.has( "id" ) ) {
            log( Level.ERROR,
                 HttpServletResponse.SC_BAD_REQUEST, "Job name not specified");
            return null;
        }
        if ( !postJson.has( "formats" ) ) {
            log( Level.ERROR,
                 HttpServletResponse.SC_BAD_REQUEST,"Snapshot formats not specified" );
            return null;
        }

        String jobName = gatherJobName( postJson );
        try {
            jobNode =
                    ActionUtil.getOrCreateJob( siteNode, jobName,
                                               "cm:content", status,
                                               response );
            if ( jobNode == null ) {
                log( Level.ERROR, 
                     HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't create snapshot job: %s",
                             postJson.getString( "id" ));
                return null;
            }

            setArtifactsGenerationStatus(postJson, null, workspace);
            startAction( jobNode, siteName, postJson, workspace );
            return postJson;

        }
        catch ( JSONException ex ) {
            log( Level.ERROR, "Failed to create snapshot job!" );
            ex.printStackTrace();
        }
        catch(Exception ex){
        	ex.printStackTrace();
        	log( Level.ERROR, "Failed to create snapshot job!" );
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
                                       int index, WorkspaceNode workspace, Date timestamp ) {
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
        	log(Level.WARN, "Failed to retrieve circular reference list at index: %d.", index);
        	return inputString;
        }
        list.add( id + transclusionType );
        index++;
        String result = parseTransclusionName( cirRefList, index, inputString, workspace, timestamp );
        result = parseTransclusionDoc( cirRefList, index, result, workspace, timestamp );
        result = parseTransclusionVal( cirRefList, index, result, workspace, timestamp );
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
    public static boolean hasHtmlZipNode( EmsScriptNode snapshotNode, Date dateTime, WorkspaceNode ws ) {
        boolean hasNode = false;
        if(snapshotNode.hasAspect( "view2:htmlZip" )){
        	EmsScriptNode node = getHtmlZipNode(snapshotNode, dateTime, ws);
        	if(node != null) hasNode = true;
        }
        return hasNode;
    }

    public static boolean hasPdf( EmsScriptNode snapshotNode ) {
        return snapshotNode.hasAspect( "view2:pdf" );
    }

    public static boolean hasPdfNode( EmsScriptNode snapshotNode, Date dateTime, WorkspaceNode ws ) {
    	boolean hasNode = false;
        if(snapshotNode.hasAspect( "view2:pdf" )){
        	EmsScriptNode node = getPdfNode(snapshotNode, dateTime, ws);
        	if(node != null) hasNode = true;
        }
        return hasNode;
    }

    private String HtmlSanitize( String s ) {
    	if(s == null || s.isEmpty()) return s;

    	try{
	    	Document document = Jsoup.parseBodyFragment(s);
	    	removeHtmlTags(document.body());
	    	// removes direct, nested para tags
	    	removeNestedTags(document);
	    	return document.body().html();
    	}
    	catch(Exception ex){
    		return s;
    	}
    }

    private String HtmlTableToDocbookTable(String table, String tableContent) throws Exception{
    	if(table == null || table.isEmpty()) return "";
    	if(tableContent == null || tableContent.isEmpty()) return table;
    	Document document = Jsoup.parseBodyFragment(table);
    	if(document == null) throw new Exception("Failed to convert HTML table to DocBook table. Root document element is null.");
    	StringBuffer sb = new StringBuffer();
    	Elements tables = document.select("body > table");
    	for(Element t : tables){
    		HtmlTable htmlTable = new HtmlTable(t);
    		sb.append(htmlTable.toDocBook());
    	}
    	return sb.toString();
    }
    
    private boolean isCircularReference( String id, String transclusionType,
                                         List< List< String >> cirRefList,
                                         int index ) {
        for ( int i = index - 1; i >= 0; i-- ) {
            if ( cirRefList.get( i ).contains( id + transclusionType ) ) return true;
        }
        return false;
    }
    
	private String parseTransclusionDoc(List<List<String>> cirRefList, int index,
	                                    String inputString, WorkspaceNode workspace, Date timestamp){
		if(inputString == null || inputString.isEmpty()) return inputString;

		Document document = Jsoup.parseBodyFragment(inputString);
		if(document == null || document.body()==null){
			log(Level.WARN, "Failed to parse HTML fragment: %s", inputString);
			return inputString;
		}

		Elements elements = document.getElementsByTag("mms-transclude-doc");
		if(elements == null || elements.size() < 1) return document.body().html();

		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(id == null || id.isEmpty()){
				log(Level.WARN, "Failed to parse transclusion doc Id for %s.", element.text());
				element.before("[cannot parse Id for " + element.text() + "]");
				element.remove();
				continue;
			}

			if(isCircularReference(id, "documentation", cirRefList, index)){
				log(Level.WARN, "Circular reference with Id: %s.", id);
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}

			String transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
			EmsScriptNode nameNode = findScriptNodeById(id, workspace, timestamp, false);
			if(nameNode == null){
				log(Level.WARN, "Failed to find EmsScriptNode Id: %s.", id);
				element.before(transcluded);
				element.remove();
			}
			else{
				try {
					JSONObject jsObj = nameNode.toJSONObject(workspace, timestamp);
					if(jsObj == null){
						log(Level.WARN, "JSONObject is null");
						element.before(transcluded);
						element.remove();
					}
					else{
						String doc = getTranscludedContent(jsObj, "documentation");
						if(doc != null && !doc.isEmpty()){
							transcluded = doc;
							while(true){
								transcluded = handleTransclusion(id, "doc", transcluded,
								                                 cirRefList, index, workspace, timestamp);
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
					log(Level.WARN, "Failed to transclude doc for Id: %s.", id);
					e.printStackTrace();
				}
			}
		}
		return document.body().html();
	}

	private String parseTransclusionName(List<List<String>> cirRefList, int index, String inputString, WorkspaceNode workspace, Date timestamp){
		if(inputString == null || inputString.isEmpty()) return inputString;
		Document document = Jsoup.parseBodyFragment(inputString);
		if(document == null || document.body() == null){
			log(Level.WARN, "Failed to parse HTML fragment: %s", inputString);
			return inputString;
		}

		Elements elements = document.getElementsByTag("mms-transclude-name");
		if(elements == null || elements.size() < 1) return document.body().html();

		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(id == null || id.isEmpty()){
				log(Level.WARN, "Failed to parse transclusion Id for %s.", element.text());
				element.before("[cannot parse Id for " + element.text() + "]");
				element.remove();
				continue;
			}

			if(isCircularReference(id, "name", cirRefList, index)){
				log(Level.WARN, "Circular reference with Id: %s.", id);
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}

			String transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
			EmsScriptNode nameNode = findScriptNodeById(id, workspace, timestamp, false);
			if(nameNode == null){
				log(Level.WARN, "Failed to find EmsScriptNode Id: %s", id);
				element.before(transcluded);
				element.remove();
				continue;
			}

			try {
				JSONObject jsObj = nameNode.toJSONObject(workspace, timestamp);
				if(jsObj == null){
					log(Level.WARN, "JSONObject is null");
					element.before(transcluded);
					element.remove();
				}
				else{
					String name = getTranscludedContent(jsObj, "name");
					if(name != null && !name.isEmpty()){
						transcluded = name;
						while(true){
							transcluded = handleTransclusion(id, "name", transcluded, cirRefList, index, workspace, timestamp);
							if(transcluded.compareToIgnoreCase(name) == 0) break;
							name = transcluded;
						}
					}
					if(transcluded == null || transcluded.isEmpty()) transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
					element.before(transcluded);
					element.remove();
				}
			} catch (JSONException e) {
				log(Level.WARN, "Failed to transclude name for Id: %s", id);
				e.printStackTrace();
			}
		}
		return document.body().html();
	}

	private String parseTransclusionVal(List<List<String>> cirRefList, int index, String inputString, WorkspaceNode workspace, Date timestamp){
		if(inputString == null || inputString.isEmpty()) return inputString;

		Document document = Jsoup.parseBodyFragment(inputString);
		if(document == null || document.body() == null){
			log(Level.WARN, "Failed to parse HTML fragment: %s.", inputString);
			return inputString;
		}

		Elements elements = document.getElementsByTag("mms-transclude-val");
		if(elements == null || elements.size() < 1) return document.body().html();

		for(Element element:elements){
			String id = element.attr("data-mms-eid");
			if(id == null || id.isEmpty()){
				log(Level.WARN, "Failed to parse transclusion Id for %s", element.text());
				element.before("[cannot parse Id for " + element.text() + "]");
				element.remove();
				continue;
			}

			if(isCircularReference(id, "value", cirRefList, index)){
				log(Level.WARN, "Circular reference with Id: %s", id);
				element.before("[Circular reference!]");
				element.remove();
				continue;
			}

			String transcluded = "[cannot find " + element.text() + " with Id: " + id + "]";
			EmsScriptNode nameNode = findScriptNodeById(id, workspace, timestamp, false);
			if(nameNode == null){
				log(Level.WARN, "Failed to find EmsScriptNode Id: %s", id);
				element.before(transcluded);
				element.remove();
				continue;
			}
			else{
				try {
					JSONObject jsObj = nameNode.toJSONObject(workspace, timestamp);
					if(jsObj == null){
						log(Level.WARN, "JSONObject is null");
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
					log(Level.WARN, "Failed to transclude Id: %s", id);
					e.printStackTrace();
				}
			}
		}
		return document.body().html();
	}

    private JSONObject populateSnapshotProperties( EmsScriptNode snapshotNode, Date dateTime, WorkspaceNode workspace )
            throws JSONException {
        JSONObject snapshoturl = snapshotNode.toJSONObject( workspace, dateTime );
        if ( hasPdf( snapshotNode ) || hasHtmlZip( snapshotNode ) ) {
        	HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        	String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
        	JSONArray formats = new JSONArray();
            if ( hasPdfNode( snapshotNode, dateTime, workspace ) ) {
                EmsScriptNode pdfNode = getPdfNode( snapshotNode, dateTime, workspace  );
                JSONObject pdfJson = new JSONObject();
                pdfJson.put("status", "Completed");
                pdfJson.put("type", "pdf");
                pdfJson.put("url", contextUrl + pdfNode.getUrl());
                formats.put(pdfJson);
            }
            if ( hasHtmlZipNode( snapshotNode, dateTime, workspace  ) ) {
                EmsScriptNode htmlZipNode = getHtmlZipNode( snapshotNode, dateTime, workspace  );
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

    private void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment") || child.nodeName().equals("#style"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    } 
    
    private void removeHtmlTag(Element elem) throws Exception{
    	if(elem == null) return;
    	Element elemNew = null;
    	Element para = null;
    	Element emphasis = null;
    	String tagName = elem.tagName().toUpperCase();
    	switch(tagName){
	    	case "BODY":
	    	case "COLSPEC":
	    	case "EMPHASIS":
	    	case "ENTRY":
	    	case "FIGURE":
	    	case "IMAGEDATA":
	    	case "IMAGEOBJECT":
    		case "INLINEMEDIAOBJECT":
	    	case "LINK":
	    	case "MEDIAOBJECT":
	    	case "ORDEREDLIST":
	    	case "ITEMIZEDLIST":
	    	case "LISTITEM":
	    	case "PARA":
	    	case "ROW":
	    	case "TBODY":
	    	case "TFOOT":
	    	case "TGROUP":
	    	case "THEAD":
	    	case "TITLE":
	    	case "ULINK":
	    	case "UTABLE":
	    	case "UTBODY":
	    	case "UTFOOT":
	    	case "UTHEAD":
	    		break;
	    	case "A":
	    		String href = elem.attr("href").toLowerCase();
	    		if(!href.startsWith("http")){
	    			HostnameGet hng = new HostnameGet(this.repository, this.services);
	    			String hostname = hng.getAlfrescoUrl();
	    			String alfrescoUrl = hostname + "/alfresco";
	    			
	    			if(href.startsWith("../service")) href = href.replace("../service", alfrescoUrl + "/service");
	    			else if(href.startsWith("ve.html#") ||
	    					href.startsWith("mms.html#") ||
	    					href.startsWith("docweb.html#")){ 
	    				href = String.format("%s/%s", alfrescoUrl, href);
	    			}
	    			else if(href.startsWith("../../share")) href = href.replace("../../share", hostname + "/share");
	    		}
	    		elemNew = new Element(Tag.valueOf("ulink"), "");
	    		elemNew.html(elem.html());
	    		elemNew.attr("xl:href", elem.attr("href"));
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
	    	case "H1":
	    	case "H2":
	    	case "H3":
	    	case "H4":
	    	case "H5":
	    		elemNew = new Element(Tag.valueOf("para"), "");
	    		emphasis = new Element(Tag.valueOf("emphasis"), "");
	    		emphasis.attr("role", "bold");
	    		emphasis.html(elem.html());
	    		elemNew.appendChild(emphasis);
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
	    	case "B":
	    	case "EM":
	    	case "I":
	    	case "STRONG":
	    		elemNew = new Element(Tag.valueOf("para"), "");
	    		emphasis = new Element(Tag.valueOf("emphasis"), "");
	    		if(tagName.compareTo("B")==0 || tagName.compareTo("STRONG")==0) emphasis.attr("role", "bold");
	    		emphasis.html(elem.html());
	    		elemNew.appendChild(emphasis);
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
	    	case "BR":
	    	case "P":
	    	case "DIV":
	    		//replaces with linebreak;
	    		elemNew = new Element(Tag.valueOf("para"), "");
	    		elemNew.html(elem.html());
	    		elemNew.prepend("<?linebreak?>");
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
	    	case "LI":
	    		elemNew = new Element(Tag.valueOf("listitem"), "");
	    		para = new Element(Tag.valueOf("para"), "");
	    		para.html(elem.html());
	    		elemNew.appendChild(para);
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
	    	case "OL":
	    		elemNew = new Element(Tag.valueOf("orderedlist"), "");
	    		elemNew.html(elem.html());
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
	    	case "SCRIPT":
	    	case "STYLE":
	    		elem.remove();
	    		break;
	    	case "TABLE":
    			String dbTable = HtmlTableToDocbookTable(elem.outerHtml(), elem.html());
    			Document doc = Jsoup.parseBodyFragment(dbTable);
    			elemNew = doc.body().select("utable").first();
    			elem.replaceWith(elemNew);
    			elem = elemNew;
    			break;
	    	case "TD":
	    	case "TH":
//	    		elemNew = new Element(Tag.valueOf("entry"),"");
//	    		para = new Element(Tag.valueOf("para"), "");
//	    		para.html(elem.html());
//	    		elemNew.appendChild(para);
//	    		elem.replaceWith(elemNew);
//	    		elem = elemNew;
	    		break;
	    	case "TR":
//	    		elemNew = new Element(Tag.valueOf("row"),"");
//	    		elemNew.html(elem.html());
//	    		elem.replaceWith(elemNew);
//	    		elem = elemNew;
	    		break;
	    	case "UL":
	    		elemNew = new Element(Tag.valueOf("itemizedlist"), "");
	    		elemNew.html(elem.html());
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
	    	case "XML":
	    	case "#COMMENT":
	    	case "COMMENT":
	    	case "MMS-TRANSCLUDE-COM":
	    		elem.remove();
	    		break;
	    	default:
//	    		TextNode textNode = new TextNode(elem.html(), "");
//	    		elem.replaceWith(textNode);
	    		elemNew = new Element(Tag.valueOf("removalTag"),"");
	    		elemNew.html(elem.html());
	    		elem.replaceWith(elemNew);
	    		elem = elemNew;
	    		break;
    	}
    	removeHtmlTags(elem);
    }

    private void removeHtmlTags(Element elem) throws Exception{
    	if(elem==null) return;
    	
    	removeComments(elem);
    	
    	for(Element child : elem.children()){
    		removeHtmlTag(child);
    	}
	}
    
    private void removeNestedTags(Document document){
    	// cleans up generated docbook fragment to pass fop validation
    	Element body = document.body();
    	// shifts nested <itemizedlist> and <orderedlist>
    	Elements list = null;
    	
    	while(true){
	    	list = body.select("itemizedlist > itemizedlist");
	    	list.addAll(body.select("orderedlist > orderedlist"));
	    	list.addAll(body.select("itemizedlist > orderedlist"));
	    	list.addAll(body.select("orderedlist > itemizedlist"));
	    	if(list.size() == 0) break;
	    	for(Element u : list){
	    		Element listItem = new Element(Tag.valueOf("listitem"),"");
	    		listItem.html(u.outerHtml());
	    		u.replaceWith(listItem);
	    		u = listItem;
	    	}
    	}
    	
    	// removes <itemizedlist>/<orderedlist> without <listitem> children
    	list = body.select("itemizedlist");
    	list.addAll(body.select("orderedlist"));
    	list.addAll(body.select("tbody"));
		for(Element item : list){
			if(item.children().size()==0) 
				item.tagName("removalTag");
		}
		
		// shifts chapter > link to chapter > para > link
		while(true){
			list = body.select(" > ulink");
			list.addAll(body.select(" > inlinemediaobject"));
			if(list.size() == 0) break;
			for(Element u : list){
				Element para = new Element(Tag.valueOf("para"), "");
				para.html(u.outerHtml());
				u.replaceWith(para);
				u = para;
			}
		}
		
    	// removes nested <para>
		while(true){
	    	list = body.select("para > para").tagName("removalTag");
	    	list.addAll(body.select("emphasis > para").tagName("removalTag"));
	    	if(list.size() == 0) break;
	    	list.tagName("removalTag");
		}
    	
    }

    protected String replaceXmlEntities(String s) {
        // this was cribbed from HtmlToDocbook.fixString, but that was keying off <html>
        // tags, so we recreated it here
        return s.replaceAll( "&(?![A-Za-z#0-9]+;)", "&amp;" )
                .replaceAll( "<([>=\\s])","&lt;$1" )
                .replaceAll( "<<", "&lt;&lt;" )
                .replaceAll( "<(?![^>]+>)", "&lt;" );
    }
    
    private DBImage retrieveEmbeddedImage(String nodeId, String imgName, WorkspaceNode workspace, Object timestamp){
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
    }

    private JSONObject saveAndStartAction(WebScriptRequest req, Status status, WorkspaceNode workspace) {
	    JSONObject jsonObject = null;
	    String siteName = getSiteName(req);
		if (siteName == null) {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "No sitename provided");
			return null;
		}

		SiteInfo siteInfo = services.getSiteService().getSite(siteName);
		if (siteInfo == null) {
			log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find site: %s", siteName);
			return null;
		}
		EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);

        JSONObject reqPostJson = //JSONObject.make( 
                (JSONObject)req.parseContent();// );
		JSONObject postJson;
		try {
		    if (reqPostJson.has( "snapshots" )) {
		        JSONArray configsJson = reqPostJson.getJSONArray( "snapshots" );
		        postJson = configsJson.getJSONObject( 0 );
		    } else {
		        postJson = reqPostJson;
		    }

			if (!postJson.has( "formats" )) {
				log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Missing snapshot formats!");
			} else {
				try{
					EmsScriptNode snapshotNode = getSnapshotNode(postJson);
					if(snapshotNode != null){
						if(SnapshotPost.getPdfStatus(snapshotNode)=="Generating") return null;
					}
					jsonObject = handleGenerateArtifacts(postJson, siteNode, status, workspace);
				}

				catch(Exception ex){
					log(Level.ERROR, "Failed to generate snapshot artifact(s)!");
					ex.printStackTrace();
				}
			}
		} catch (JSONException e) {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON");
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
            log( Level.ERROR,
                 "Could not create snapshot job_id temporary directory." );
        }
        if ( !( new File( dbDirName.toString() ).mkdirs() ) ) {
            log( Level.ERROR, "Could not create Docbook temporary directory" );
        }
        if ( !( new File( imgDirName.toString() ).mkdirs() ) ) {
            log( Level.ERROR, "Could not create image temporary directory." );
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
    
    /**
     * @param workspace
     * @param postJson
     * @throws Exception
     * sets artifacts generation status to "Generating" when firing off the process
     */
    private void setArtifactsGenerationStatus(JSONObject postJson, Date dateTime, WorkspaceNode ws) throws Exception{
    	try{
//    	    if (!postJson.has( "id" )) {
//    	        throw new Exception("No id found");
//    	    }
//
//    	    String id = postJson.getString("id");
//    	    // do simple lucene search, since snapshotNode ID is unique
//    	    ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( id, "@cm\\:name:\"", services );
//    	    if (nodeRefs == null || nodeRefs.size() != 1) {
//    	    	throw new Exception("Failed to find snapshot with Id: " + postJson.getString("id"));
//    	    }
//    	    EmsScriptNode snapshotNode = new EmsScriptNode(nodeRefs.get( 0 ), services, response);
    		
    		EmsScriptNode snapshotNode = getSnapshotNode(postJson);
			ArrayList<String> formats = getSnapshotFormats(postJson);
			for(String format:formats){
				if(format.compareToIgnoreCase("pdf") == 0){
					if(SnapshotPost.getPdfNode(snapshotNode, dateTime, ws)==null){
        				snapshotNode.createOrUpdateAspect("view2:pdf");
        	            snapshotNode.createOrUpdateProperty("view2:pdfStatus", "Generating");
					}
        	    }
        	    else if(format.compareToIgnoreCase("html") == 0){
        	        if(SnapshotPost.getHtmlZipNode(snapshotNode, dateTime, ws)==null){
        	            snapshotNode.createOrUpdateAspect("view2:htmlZip");
        	            snapshotNode.createOrUpdateProperty("view2:htmlZipStatus", "Generating");
        	        }
        	    }
			}
    	}
    	catch(Exception ex){
    		ex.printStackTrace();
    		log(Level.ERROR, "Failed to set artifact generation status!");
    	}
    }

    public void setHtmlZipStatus(EmsScriptNode node, String status){
    	if(node==null) return;
    	node.createOrUpdateAspect("view2:htmlZip");
		node.createOrUpdateProperty("view2:htmlZipStatus", status);
    }

    public void setPdfStatus(EmsScriptNode node, String status){
    	if(node==null) return;
    	node.createOrUpdateAspect("view2:pdf");
		node.createOrUpdateProperty("view2:pdfStatus", status);
		node.getOrSetCachedVersion();
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
    	JSONArray contains = node.getView().getContainsJson(timestamp, workspace);
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

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return false;
    }
    
}
