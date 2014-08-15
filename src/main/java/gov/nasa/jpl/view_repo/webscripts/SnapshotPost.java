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
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.util.TempFileProvider;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class SnapshotPost extends AbstractJavaWebScript {
	protected String snapshotName;
	private boolean isSnapshotNode = false;	//determines whether we're working with a view/product or a snapshot node reference; true for snapshot node reference
	
    public SnapshotPost() {
        super();
    }
    
    public SnapshotPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }
    
    private String getSnapshotId(WebScriptRequest req){
    	String snapshotId = null;
    	String[] snapshotIds = {"snapshotId"};
        for (String key: snapshotIds) {
        	snapshotId = req.getServiceMatch().getTemplateVars().get(key);
            if (snapshotId != null) {
                break;
            }
        }
		return snapshotId;
    }
    
    private String getViewId(WebScriptRequest req){
    	 String viewId = null;
         String[] viewKeys = {"viewid", "productId"};
         for (String key: viewKeys) {
             viewId = req.getServiceMatch().getTemplateVars().get(key);
             if (viewId != null) {
                 break;
             }
         }
         return viewId;
    }

    @Override
    protected synchronized Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    	Map<String, Object> model = new HashMap<String, Object>();

        printHeader( req );

        clearCaches();

		SnapshotPost instance = new SnapshotPost(repository, services);

		JSONObject result = instance.saveAndStartAction(req, status);
		appendResponseStatusInfo(instance);

		status.setCode(responseStatus.getCode());
		if (result == null) {
		    model.put("res", response.toString());
		} else {
		    model.put("res", result);
		}

        printFooter();

		return model;
		
    	/*
    	printHeader( req );
        clearCaches();

        String snapshotId = getSnapshotId(req);
        String viewId = getViewId(req);

        EmsScriptNode topview = null;
        EmsScriptNode snapshotFolderNode = null;
        EmsScriptNode snapshotNode = null;
        Map<String, Object> model = new HashMap<String, Object>();
        DateTime now = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        
        if(snapshotId != null && !snapshotId.isEmpty()){
          	System.out.println("Got a Snapshot node...");
          	this.isSnapshotNode = true;
        	snapshotNode = findScriptNodeById(snapshotId, null);
        	this.snapshotName = (String)snapshotNode.getProperty(Acm.ACM_ID);
        	ChildAssociationRef childAssociationRef = this.services.getNodeService().getPrimaryParent(snapshotNode.getNodeRef());
        	snapshotFolderNode = new EmsScriptNode(childAssociationRef.getParentRef(), this.services);
        	DocBookWrapper docBookWrapper = new DocBookWrapper(this.snapshotName, snapshotNode);
        	if(!hasPdf(snapshotNode)){
        		System.out.println("Generating PDF...");
        		docBookWrapper.savePdfToRepo(snapshotFolderNode);
        	}
        	if(!hasHtmlZip(snapshotNode)){
        		System.out.println("Generating HTML zip...");
        		docBookWrapper.saveHtmlZipToRepo(snapshotFolderNode);
    		}
        }
        else{
        	topview = findScriptNodeById(viewId, null);
        	snapshotFolderNode = getSnapshotFolderNode(topview);
	        this.snapshotName = viewId + "_" + now.getMillis();
	        if (checkPermissions(snapshotFolderNode, PermissionService.WRITE)) {
	            snapshotNode = createSnapshot(topview, viewId, snapshotName, req.getContextPath(), snapshotFolderNode);
	        }
        }
        
        if (snapshotNode != null) {
        	System.out.println("building JSON...");
            try {
                JSONObject snapshoturl = new JSONObject();
                snapshoturl.put("id", snapshotName);
                snapshoturl.put("creator", AuthenticationUtil.getFullyAuthenticatedUser());
                snapshoturl.put("created", fmt.print(now));
                snapshoturl.put("url", req.getContextPath() + "/service/snapshots/" + snapshotName);
                if(hasPdf(snapshotNode) || hasHtmlZip(snapshotNode)){
                	HashMap<String, String> transformMap;
                	LinkedList<HashMap> list = new LinkedList<HashMap>();
	                if(hasPdf(snapshotNode)){
	                	EmsScriptNode pdfNode = getPdfNode(snapshotNode);
	                	transformMap = new HashMap<String,String>();
	                	transformMap.put("type", "pdf");
	                	transformMap.put("url", pdfNode.getUrl());
	                	list.add(transformMap);
	                }
	                if(hasHtmlZip(snapshotNode)){
	                	EmsScriptNode htmlZipNode = getHtmlZipNode(snapshotNode);
	                	transformMap = new HashMap<String,String>();
	                	transformMap.put("type", "html");
	                	transformMap.put("url", htmlZipNode.getUrl());
	                	list.add(transformMap);
	                }
	                snapshoturl.put("formats", list);
                }
                model.put("res", snapshoturl.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
                log(LogLevel.ERROR, "Error generating JSON for snapshot", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            log(LogLevel.ERROR, "Error creating snapshot node", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        status.setCode(responseStatus.getCode());
        if (status.getCode() != HttpServletResponse.SC_OK) {
            model.put("res", response.toString());
        }
        
        printFooter();
        return model;
        */
    }

/*    private void init(EmsScriptNode product){
    	this.isSnapshotNodeRef(product);
    }*/
    
    private EmsScriptNode getHtmlZipNode(EmsScriptNode snapshotNode){
    	NodeRef node = (NodeRef)snapshotNode.getProperty("view2:htmlZipNode");
    	return new EmsScriptNode(node, snapshotNode.getServices());
    }
    
    private EmsScriptNode getPdfNode(EmsScriptNode snapshotNode){
    	NodeRef node = (NodeRef)snapshotNode.getProperty("view2:pdfNode");
    	return new EmsScriptNode(node, snapshotNode.getServices());
    }
    
    private boolean hasHtmlZip(EmsScriptNode snapshotNode){
    	return snapshotNode.hasAspect("view2:htmlZip");
    }
    
    private boolean hasPdf(EmsScriptNode snapshotNode){
    	return snapshotNode.hasAspect("view2:pdf");
    }
    
    public EmsScriptNode createSnapshot(EmsScriptNode view, String viewId) {
        this.snapshotName = viewId + "_" + System.currentTimeMillis();
        String contextPath = "alfresco/service/";
        EmsScriptNode viewNode = findScriptNodeById(viewId, null);
        EmsScriptNode snapshotFolder = getSnapshotFolderNode(viewNode);
        return createSnapshot(view, viewId, snapshotName, contextPath, snapshotFolder);
    }
    
    public EmsScriptNode generateHTML(EmsScriptNode snapshotNode){
    	this.snapshotName = (String)snapshotNode.getProperty(Acm.ACM_ID);
    	ChildAssociationRef childAssociationRef = this.services.getNodeService().getPrimaryParent(snapshotNode.getNodeRef());
    	EmsScriptNode snapshotFolderNode = new EmsScriptNode(childAssociationRef.getParentRef(), this.services);
    	DocBookWrapper docBookWrapper = new DocBookWrapper(this.snapshotName, snapshotNode);
    	
    	if(!hasHtmlZip(snapshotNode)){
    		System.out.println("Generating HTML zip...");
    		docBookWrapper.saveHtmlZipToRepo(snapshotFolderNode);
		}
    	return snapshotNode;
    }
    
    public EmsScriptNode generatePDF(EmsScriptNode snapshotNode){
    	this.snapshotName = (String)snapshotNode.getProperty(Acm.ACM_ID);
    	ChildAssociationRef childAssociationRef = this.services.getNodeService().getPrimaryParent(snapshotNode.getNodeRef());
    	EmsScriptNode snapshotFolderNode = new EmsScriptNode(childAssociationRef.getParentRef(), this.services);
    	DocBookWrapper docBookWrapper = new DocBookWrapper(this.snapshotName, snapshotNode);
    	if(!hasPdf(snapshotNode)){
    		System.out.println("Generating PDF...");
    		docBookWrapper.savePdfToRepo(snapshotFolderNode);
    	}
    	return snapshotNode;
    }
    
    private String getSiteName(WebScriptRequest req){
    	 String[] siteKeys = {SITE_NAME, "siteId"};
 	    
 		String siteName = null;
 		for (String key: siteKeys) {
 		    siteName = req.getServiceMatch().getTemplateVars().get(key);
 		    if (siteName != null) {
 		        break;
 		    }
 		}
 		return siteName;
    }
    
    private JSONObject saveAndStartAction(WebScriptRequest req, Status status) {
	    JSONObject jsonObject = null;
	  /*  String siteName = getSiteName(req);
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
				J
				jsonObject = handleCreate(postJson, siteNode, status);
			}
		} catch (JSONException e) {
			log(LogLevel.ERROR, "Could not parse JSON", HttpServletResponse.SC_BAD_REQUEST);
			e.printStackTrace();
			return null;
		}*/
		
		return jsonObject;
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
	
	/**
	 * Helper method to convert a list to an array of specified type
	 * @param list
	 * @return
	 */
	/*private String[] list2Array(List<String> list) {
		return Arrays.copyOf(list.toArray(), list.toArray().length, String[].class);
	}
	*/
	/**
	 * Helper to execute the command using RuntimeExec
	 * 
	 * @param srcFile	File to transform
	 * @return 			Absolute path of the generated file
	 */
/*
//	private String doTransformation(DocBookWrapper docBookWrapper, File srcFile) {
//		RuntimeExec re = new RuntimeExec();
//		List<String> command = new ArrayList<String>();
//		
//		String source = srcFile.getAbsolutePath();
//		String target = source.substring(0, source.indexOf(".")) + ".pdf";
//		command.add(docBookWrapper.getFobFileName());
//		command.add("-xml");
//		command.add(source);
//		command.add("-xsl");
//		command.add(docBookWrapper.getFobXslFileName());
//		command.add("-pdf");
//		command.add(target);
//
//		System.out.println("DO_TRANSFORM source: " + source);
//		System.out.println("DO_TRANSFORM target: " + target);
//		System.out.println("DO_TRANSFROM cmd: " + command);
//		
//		re.setCommand(list2Array(command));
//		ExecutionResult result = re.execute();
//
//		if (!result.getSuccess()) {
//			System.out.println("FOP transformation command unsuccessful\n");
//			//logger.error("FOP transformation command unsuccessful\n");
//			System.out.println("Exit value: " + result.getExitValue());
//		}
//		
//		return target;
//	}
    
    
//    private void transformToHtml(EmsScriptNode snapshotNode, DocBookWrapper docBookWrapper) throws IOException{
//    	RuntimeExec re = new RuntimeExec();
//		List<String> command = new ArrayList<String>();
//		
//		String source = docBookWrapper.getDBFileName();
//		String target = source.substring(0, source.indexOf(".")) + ".html";
//		String xalanDir = docBookWrapper.getXalanDirName();
//		String cp = xalanDir + "/xalan.jar:" + xalanDir + "/xercesImpl.jar:" + xalanDir + "/serializer.jar:" +xalanDir + "/xml-apis.jar";
//		command.add("java");
//		command.add("-cp");
//		command.add(cp);
//		command.add("org.apache.xalan.xslt.Process");
//		command.add("-in");
//		command.add(source);
//		command.add("-xsl");
//		command.add(docBookWrapper.getHtmlXslFileName());
//		//command.add("-out");
//		//command.add(target);
//		command.add("-param");
//		command.add("chunk.tocs.and.lots");
//		command.add("1");
//		command.add("-param");
//		command.add("chunk.tocs.and.lots.has.title");
//		command.add("1");		
//
//		System.out.println("DO_TRANSFORM source: " + source);
//		System.out.println("DO_TRANSFORM target: " + target);
//		System.out.println("DO_TRANSFROM cmd: " + command);
//		
//		re.setCommand(list2Array(command));
//		ExecutionResult result = re.execute();
//			    		
//		if (!result.getSuccess()) {
//			System.out.println("Failed HTML transformation!\n");
//			//logger.error("FOP transformation command unsuccessful\n");
//		}
//		else{
//			String title = "";
//			File frame = new File(Paths.get(docBookWrapper.getDBDirName(), "frame.html").toString());
//			BufferedWriter writer = new BufferedWriter(new FileWriter(frame));
//			writer.write("<html><head><title>" + title + "</title></head><frameset cols='30%,*' frameborder='1' framespacing='0' border='1'><frame src='bk01-toc.html' name='list'><frame src='index.html' name='body'></frameset></html>");
//	        writer.close();
//	        Files.copy(Paths.get(docBookWrapper.getDocGenCssFileName()), Paths.get(docBookWrapper.getDBDirName(), "docgen.css"));
//		}
//    }
//    
*/	
/*	private boolean isSnapshotNodeRef(EmsScriptNode view){
		this.isSnapshotNode = (view.getType().equals("{http://jpl.nasa.gov/model/view/2.0}Snapshot"));
		return this.isSnapshotNode;
	}*/
	
    public EmsScriptNode createSnapshot(EmsScriptNode view, String viewId, String snapshotName, String contextPath, EmsScriptNode snapshotFolder) {
    	EmsScriptNode snapshotNode = null;
        if(!this.isSnapshotNode){
	    	snapshotNode = snapshotFolder.createNode(snapshotName, "view2:Snapshot");
	        snapshotNode.createOrUpdateProperty("cm:isIndexed", true);
	        snapshotNode.createOrUpdateProperty("cm:isContentIndexed", false);
	        snapshotNode.createOrUpdateProperty(Acm.ACM_ID, snapshotName);
	        
	        view.createOrUpdateAssociation(snapshotNode, "view2:snapshots");
        }
        else{
        	snapshotNode = view;
        }

        JSONObject snapshotJson = new JSONObject();
        try {
        	if(!this.isSnapshotNode){
	            snapshotJson.put("snapshot", true);
	            ActionUtil.saveStringToFile(snapshotNode, "application/json", services, snapshotJson.toString(4));
        	}
            DocBookWrapper docBookWrapper = createDocBook(view, viewId, snapshotName, contextPath, snapshotNode);
            if(docBookWrapper == null){
            	System.out.println("Failed to generate DocBook!");
            }
            else{
            	docBookWrapper.save();
            	String id = (String)snapshotNode.getProperty(Acm.ACM_ID);
            	System.out.println("id: " + id);
            	//ActionUtil.saveStringToFile(snapshotNode, "application/docbook+xml", services, docBookWrapper.getContent());
            	docBookWrapper.saveDocBookToRepo(snapshotFolder);
            	//docBookWrapper.savePdfToRepo(snapshotFolder);
            	//docBookWrapper.saveHtmlZipToRepo(snapshotFolder);
            	//transformDocBook(snapshotNode, docBookWrapper);
            	
            	//move PDF and HTML generation out of snapshot
            	/*
            	String pdfPath = docBookWrapper.transformToPDF(snapshotNode);
            	if(pdfPath == null || pdfPath.isEmpty()){
            		System.out.println("Failed to transform DocBook to PDF!");
            	}
            	else{
            		docBookWrapper.saveFileToRepo(snapshotNode, MimetypeMap.MIMETYPE_PDF, pdfPath);
            		try{
            			docBookWrapper.transformToHTML();
            			String archivePath = docBookWrapper.zipHtml();
	            		docBookWrapper.saveFileToRepo(snapshotNode, "application/zip", archivePath);
            		}
            		catch(Exception ex){
            			System.out.println(ex.getMessage());
            			ex.printStackTrace();
            		}
            	}
            	*/
            }
        } 
        catch (Exception e1) {
            e1.printStackTrace();
        }
        
        return snapshotNode;
    }
    
    /*
    private EmsScriptNode getChildrenViews(EmsScriptNode node){
    	log(LogLevel.INFO, "\ngetting " + node.getName() + " children views...");
    	JSONArray jsonArray = node.getChildrenViewsJSONArray();
    	for(int i = 0; i <  jsonArray.length(); i++){
    		try {
    			Object viewId = jsonArray.get(i);
				System.out.println(viewId.toString());
				EmsScriptNode viewNode = findScriptNodeById(viewId.toString(), null);
				System.out.println(viewNode.toJSON());
				View v = viewNode.getView();
				Collection<EmsScriptNode> displayedElems = v.getDisplayedElements();
				for(EmsScriptNode n: displayedElems){
					System.out.println(n.toJSON());
				}
				Collection<EmsScriptNode> childViewElems = v.getChildViewElements();
				for(EmsScriptNode n: childViewElems){
					System.out.println(n.toJSON());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    
//    	Object property = node.getProperty( Acm.ACM_CHILDREN_VIEWS );
//    	System.out.println(jsonArray.toString());
    	
//    	System.out.println(property.toString());
    	return null;
    }
    */
    
    private String getSymlId(JSONObject jsonObj){
    	return (String)jsonObj.opt(Acm.SYSMLID);
    }
    
    private String getType(JSONObject jsonObj){
    	return (String)jsonObj.opt("type");
    }
    
	private String getUserProfile(EmsScriptNode node, String userName){
    	StringBuffer sb = new StringBuffer();
    	EmsScriptNode user = new EmsScriptNode(node.getServices().getPersonService().getPerson(userName), node.getServices(),node.getResponse());
    	//System.out.println("User: " + user.toJSON());
    	sb.append(user.getProperty("cm:firstName"));
    	sb.append(",");
    	sb.append(user.getProperty("cm:lastName"));
    	sb.append(",");
    	//job title
    	sb.append(",");
    	sb.append(user.getProperty("cm:organizationId"));
    	sb.append(",");
    	return sb.toString();
    }
    
    private DBBook createDocBook(EmsScriptNode product){
    	String title = (String)product.getProperty(Acm.ACM_NAME);
    	DBBook docBook = new DBBook();
    	docBook.setTitle(title);
    	docBook.setTitlePageLegalNotice("This Document has not been reviewed for export control. Not for distribution to or access by foreign persons.");
    	docBook.setFooterLegalNotice("Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.");
    	String author = getUserProfile(product, (String)product.getProperty(Acm.ACM_AUTHOR));
    	docBook.setAuthor(Arrays.asList(author));
    	return docBook;
    }
    
    /*
     private void saveDocBook(DBSerializeVisitor docBookMgr) throws IOException{
    	String tmpDirName	= TempFileProvider.getTempDir().getAbsolutePath();
    	Path jobDirName = Paths.get(tmpDirName, this.snapshotName);
		Path dbDirName = Paths.get(jobDirName.toString(), "docbook");
		Path dbFileName = Paths.get(dbDirName.toString(), this.snapshotName + ".db");
		String docBookXml = docBookMgr.getOut();
    	System.out.println(docBookXml);
    	//Files.write(dbFileName, bytes, options);
    	File f = new File(dbFileName.toString());
    	FileWriter fw = new FileWriter(f);
    	fw.write(docBookXml);
    	fw.close();
    }
    */
    
    private String HtmlSanitize(String s){
    	//TODO check for case sensitive ....return s.replaceAll("(?i)<p>([^<]*)</p>","$1");
    	return s.replaceAll("(?i)<p>([^<]*)</p>","$1");
    }
    
    private Object extractNodeValue(EmsScriptNode node	){
    	Object valObj = node.getProperty(Acm.SYSML + "integer");
		if(valObj == null || !(valObj instanceof Integer)){
			valObj = node.getProperty(Acm.SYSML + "real");
			if(valObj == null || !(valObj instanceof Float)){ 
				valObj = node.getProperty(Acm.SYSML + "string");
				if(valObj == null || !(valObj instanceof String)){
					valObj = node.getProperty(Acm.SYSML + "boolean");
					if(valObj == null || !(valObj instanceof Boolean)){
						valObj = node.getProperty(Acm.SYSML + "double");
						if(valObj == null || !(valObj instanceof Double)) return null;
					}
				}
			}
		}
		return valObj;
    }
    
    private DBParagraph createDBParagraph(JSONObject obj){
		String srcType = (String)obj.opt("sourceType");
		String src = (String)obj.opt("source");
		String srcProp = (String)obj.opt("sourceProperty");
		
		DBParagraph p = new DBParagraph();
    	p.setId(src);
    	if(srcType.compareTo("reference")==0){
    		EmsScriptNode node = findScriptNodeById(src, null);
    		if(srcProp.compareTo("value")==0){
    			List<NodeRef> nodeRefs = (List<NodeRef>)node.getProperty(Acm.SYSML + srcProp);
    			StringBuffer sb = new StringBuffer();
    			for(int i=0; i < nodeRefs.size(); i++){
    				NodeRef nodeRef = nodeRefs.get(i);
    				EmsScriptNode valueNode = new EmsScriptNode(nodeRef, node.getServices());
    				if(valueNode != null){
    					String valueProp = (String)node.getProperty(Acm.SYSML + "name");
    					if(valueProp != null && !valueProp.isEmpty())	{
    						Object value = valueNode.getProperty(Acm.SYSML + valueProp);
    						if(value == null || (value != null && value.toString().isEmpty())){
    							value = extractNodeValue(valueNode);
    							if(value == null || (value != null && value.toString().isEmpty())) continue;
    						}
    						
    						sb.append("<para>");
    						if(value instanceof String) sb.append(HtmlSanitize((String)value));
    						else sb.append(value);
    						sb.append("</para>");
    					}
    					else{
    						
    						try{
    							Object valObj = extractNodeValue(valueNode);
    							/*
    							Object valObj = valueNode.getProperty(Acm.SYSML + "integer");
    							if(valObj == null || !(valObj instanceof Integer)){
    								valObj = valueNode.getProperty(Acm.SYSML + "real");
    								if(valObj == null || !(valObj instanceof Float)){ 
    									valObj = valueNode.getProperty(Acm.SYSML + "string");
    									if(valObj == null || !(valObj instanceof String)){
    										valObj = valueNode.getProperty(Acm.SYSML + "boolean");
    										if(valObj == null || !(valObj instanceof Boolean)){
    											valObj = valueNode.getProperty(Acm.SYSML + "double");
    											if(valObj == null || !(valObj instanceof Double)) continue;
    										}
    									}
    								}
    							}
    							*/
    							if(valObj == null) continue;
    							sb.append("<para>");
    							if(valObj instanceof String) sb.append(HtmlSanitize((String)valObj));
    							else sb.append(valObj);
    							sb.append("</para>");
    						}
    						catch(Exception ex){
    							System.out.println("node.toJSON(): " + node.toJSON());
							}
    					}
    				}
    			}
    			p.setText(sb.toString());
    		}
    		else
    			p.setText(HtmlSanitize((String)node.getProperty(Acm.SYSML + srcProp)));
    	}
    	else{
    		if(srcProp != null && !srcProp.isEmpty())
    			p.setText(HtmlSanitize((String)obj.opt(Acm.SYSML + srcProp)));
    		else
    			p.setText(HtmlSanitize((String) obj.opt("text")));
    	}
    	if(p.getText() == null || p.getText().toString().isEmpty()) return null;
    	
    	return p;
    }
    
    private DocumentElement createList(JSONObject obj) throws JSONException{
    	Boolean isOrdered = (Boolean)obj.opt("ordered");
    	//Boolean isBulleted = (Boolean)obj.opt("bulleted");
    	
    	DBList list = new DBList();
    	list.setOrdered(isOrdered);
    	JSONArray listItemWrapper = obj.getJSONArray("list");
    	//System.out.println("listItemWrapper: " + listItemWrapper.toString());
    	for(int i=0; i < listItemWrapper.length(); i++){
    		JSONArray listItems = listItemWrapper.getJSONArray(i);
    		for(int j=0; j < listItems.length(); j++){
    			JSONObject jsObj = listItems.getJSONObject(j);
    			DocumentElement e = createElement(jsObj);
    			if(e!=null) list.addElement(e);
    		}
    	}
    	
    	return (DocumentElement)list;
    }
    
    private List<List<DocumentElement>> createTableRows(JSONArray jsonRows) throws JSONException{
    	List<List<DocumentElement>> list = new ArrayList<List<DocumentElement>>();
    	for(int i=0; i < jsonRows.length(); i++){
    		JSONArray headerRows = jsonRows.getJSONArray(i);
    		//System.out.println("headerRows: " + headerRows.toString());
    		List<DocumentElement> rows = new ArrayList<DocumentElement>();
    		for(int j=0; j < headerRows.length(); j++){
	    		JSONObject contents = headerRows.getJSONObject(j);
	    		//System.out.println("contents: " + contents.toString());
	    			JSONArray headerCols = contents.getJSONArray("content");
	    			//System.out.println("headerCols: " + headerCols.toString());
		    		for(int l=0; l < headerCols.length(); l++){
		    			JSONObject content = headerCols.getJSONObject(l);
		    			//System.out.println("content: " + content.toString());
			    		String colspan = (String)content.opt("colspan");
			    		String rowspan = (String)content.opt("rowspan");
			    		DocumentElement e = createElement(content);
			    		if(e != null) rows.add(e);
		    		}
    		}
    		list.add(rows);
    	}
    	return list;
    }
    
    private List<List<DocumentElement>> createTableHeader(JSONObject obj) throws JSONException{
    	return createTableRows(obj.getJSONArray("header"));
    }
    
    private List<List<DocumentElement>> createTableBody(JSONObject obj) throws JSONException {
    	return createTableRows(obj.getJSONArray("body"));
    }
    
    private DocumentElement createTable(JSONObject obj) throws JSONException{
    	DBTable table = new DBTable();
    	String title = (String)obj.opt("title");
    	String style = (String)obj.opt("sytle");
    	table.setId(getSymlId(obj));
    	table.setTitle(title);
    	table.setStyle(style);
    	table.setHeaders(createTableHeader(obj));
    	table.setBody(createTableBody(obj));
    	
    	//table.setCols(headerCols.length());
    	return (DocumentElement)table;
    }
    
    private void saveImage(DBImage image, EmsScriptNode imageEmsScriptNode) throws Exception{
    	String tmpDirName	= TempFileProvider.getTempDir().getAbsolutePath();
    	Path jobDirName = Paths.get(tmpDirName, this.snapshotName);
		Path dbDirName = Paths.get(jobDirName.toString(), "docbook");
		//Path imgDirName = Paths.get(jobDirName.toString(), "images");
		Path imgDirName = Paths.get(dbDirName.toString(), "images");
		
		if( !(new File(jobDirName.toString()).mkdirs())){
			System.out.println("Could not create snapshot job_id temporary directory.");
		}
		if ( !(new File(dbDirName.toString()).mkdirs()) ) {
			System.out.println("Could not create Docbook temporary directory");
		}
		if ( !(new File(imgDirName.toString()).mkdirs()) ) {
			System.out.println("Could not create image temporary directory.");
		}
		ContentService contentService = imageEmsScriptNode.getServices().getContentService();
		NodeService nodeService = imageEmsScriptNode.getServices().getNodeService();
		NodeRef imgNodeRef = imageEmsScriptNode.getNodeRef();
		ContentReader reader = contentService.getReader(imgNodeRef, ContentModel.PROP_CONTENT);
        InputStream originalInputStream = reader.getContentInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final int BUF_SIZE = 1 << 8; //1KiB buffer
        byte[] buffer = new byte[BUF_SIZE];
        int bytesRead = -1;
        while((bytesRead = originalInputStream.read(buffer)) > -1) {
        	outputStream.write(buffer, 0, bytesRead);
        }
        originalInputStream.close();
        byte[] binaryData = outputStream.toByteArray();
		String imgFilename = (String) nodeService.getProperty(imgNodeRef, ContentModel.PROP_NAME);
        Path filePath = Paths.get(imgDirName.toString(), imgFilename);
        Files.write(filePath, binaryData);
        image.setFilePath(dbDirName.relativize(filePath).toString());
        
        TranscoderInput inputSvgImg = new TranscoderInput(filePath.toUri().toString());
        OutputStream pngOstrm = new FileOutputStream(Paths.get(imgDirName.toString(), imgFilename.replace("svg", "png")).toString());
        TranscoderOutput outputPngImg = new TranscoderOutput(pngOstrm);
        PNGTranscoder pngTransCoder = new PNGTranscoder();
        try{
        	pngTransCoder.transcode(inputSvgImg, outputPngImg);
        }
        catch(Exception ex){
        	System.out.println("Failed to convert svg to png!");
        	throw new Exception("Failed to convert svg to pgn!", ex);
        }
    }
    
    private void retrieveDocBook(DocBookWrapper docBookWrapper, NodeRef dbNodeRef){
		try{
			retrieveStringPropContent(docBookWrapper,dbNodeRef, Paths.get(docBookWrapper.getDBFileName()));
		}
		catch(Exception ex){
			System.out.println("Failed to retrieve DocBook!");
			ex.printStackTrace();
		}
    }
    
    private void retrieveStringPropContent(DocBookWrapper docBookWrapper, NodeRef node, Path savePath) throws IOException{
    	if(!Files.exists(savePath.getParent())){
	    	if(!new File(savePath.getParent().toString()).mkdirs()){
	    		System.out.println("Could not create path: " + savePath.getParent());
	    		return;
	    	}
    	}
    	System.out.println("getting content service...");
    	ContentService contentService = docBookWrapper.getSnapshotNode().getServices().getContentService();
    	if(contentService == null) System.out.println("conent service is null!");
		//NodeService nodeService = node.getServices().getNodeService();
    	System.out.println("getting nodeRef...");
		//NodeRef nodeRef = node.getNodeRef();
		//if(nodeRef==null) System.out.println("nodeRef is null!");
		System.out.println("getting prop_content...");
		ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
		if(reader==null) System.out.println("reader is null!");
		File srcFile = new File(savePath.toString());
		reader.getContent(srcFile);
		
		//String content = reader.getContentString();
		System.out.println("retrieved docbook!");
		
//		if(reader==null) System.out.println("reader is null!");
//        InputStream originalInputStream = reader.getContentInputStream();
//        System.out.println("originalInputStream is null: " + (originalInputStream==null));
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        final int BUF_SIZE = 1 << 8; //1KiB buffer
//        byte[] buffer = new byte[BUF_SIZE];
//        int bytesRead = -1;
//        while((bytesRead = originalInputStream.read(buffer)) > -1) {
//        	outputStream.write(buffer, 0, bytesRead);
//        	System.out.println("writing to buffer...");
//        }
//        originalInputStream.close();
//        byte[] binaryData = outputStream.toByteArray();
//        System.out.println("binaryData.length: " + binaryData.length);
//		//String filename = (String) nodeService.getProperty(imgNodeRef, ContentModel.PROP_NAME);
//        String filename = "docbook.db";
//        Path filePath = Paths.get(savePath.toString(), filename);
//        System.out.println("writing to filesystem...");
//        Files.write(filePath, binaryData);
    }
    
    private DocumentElement createImage(JSONObject obj){
    	DBImage image = new DBImage();
    	image.setId(getSymlId(obj));
    	
    	String id = (String)obj.opt(Acm.SYSMLID);
    	EmsScriptNode imgNode = findScriptNodeById(id, null);
    	if(imgNode == null){
    		//TODO error handling
    		return image;
    	}
    	else{
    		try{
    			image.setTitle((String)imgNode.getProperty(Acm.ACM_NAME));
	    		NodeRef nodeRef = imgNode.getNodeRef();
	    		ServiceRegistry services = imgNode.getServices();
	    		NodeService nodeService = imgNode.getServices().getNodeService();
	    		
	    		String fileName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
	    		fileName += ".svg";
	    		ResultSet resultSet = NodeUtil.luceneSearch("@name:" + fileName);
	    		if(resultSet != null && resultSet.length() > 0){
			    //for ( ResultSetRow row : resultSet ) {
			    	//EmsScriptNode node = new EmsScriptNode( row.getNodeRef(), services );
			    	EmsScriptNode node = new EmsScriptNode(resultSet.getNodeRef(0), services);
			    	saveImage(image, node);
			    }
	    		else{
	    			System.out.println(fileName + " image file not found!");
	    		}
    		}
    		catch(Exception ex){;}
    		return image;
    	}
    }

    private void createDBSectionContainment(DBSection section, JSONArray jsonContains) throws JSONException{
    	for(int i=0; i < jsonContains.length(); i++){
    		JSONObject obj = jsonContains.getJSONObject(i);
			DocumentElement e = createElement(obj);
			if(e != null) section.addElement(e);
    	}
	}
    
    private DocumentElement createDBSection(JSONObject obj) throws JSONException{
    	//System.out.println("Sections JSON: " + obj.toString());
    	DBSection section = new DBSection();
    	section.setTitle((String)obj.opt("name"));
    	createDBSectionContainment(section, obj.getJSONArray("contains"));
    	return section;
    }
    
	private DocumentElement createElement(JSONObject obj) throws JSONException{
    	DocumentElement e = null;
    	switch(getType(obj)){
    	case "Paragraph":
    		e = createDBParagraph(obj);
    		break;
    	case "List":
    		e = createList(obj);
    		break;
    	case "Table":
    		e = createTable(obj);
    		break;
    	case "Image":
    		e = createImage(obj);
    		break;
    	case "Section":
    		e = createDBSection(obj);
    		break;
    		default:
    			System.out.println("type: " + getType(obj));
    			break;
    	}
    	return e;
    }
    
    private void traverseElements(DBSection section, EmsScriptNode node) throws JSONException{
    	JSONArray contains = node.getView().getContainsJson();
    	createDBSectionContainment(section, contains);
    	/*
    	  for(int i=0; i < contains.length(); i++){
    		JSONObject obj = contains.getJSONObject(i);
			DocumentElement e = createElement(obj);
			if(e != null) section.addElement(e);
    	}
    	*/
    }
    
    private DBSection emsScriptNodeToDBSection(EmsScriptNode node, Boolean isChapter) throws JSONException{
    	DBSection section = new DBSection();
    	if(isChapter) section.setChapter(isChapter);
    	section.setTitle((String)node.getProperty(Acm.ACM_NAME));
    	section.setId((String)node.getProperty(Acm.ACM_ID));
    	
    	traverseElements(section, node);
    	return section;
    }
    
    private DocBookWrapper createDocBook(EmsScriptNode view, String viewId, String snapshotName, String contextPath, EmsScriptNode snapshotFolder)  throws Exception{
    	log(LogLevel.INFO, "\ncreating DocBook snapshot for view Id: " + viewId);
    	log(LogLevel.INFO, "\ncreating DocBook snapshotname: " + snapshotName);
    	 
    	if(view == null){
    		log(LogLevel.WARNING, "null [view] input parameter reference.");
    		return null;
    	}
    	
    	//DBSerializeVisitor docBookMgr = new DBSerializeVisitor(true, new File(snapshotName));
    	//DocBookWrapper docBookMgr = new DocBookWrapper(view, viewId, this.snapshotName, snapshotFolder);
    	DocBookWrapper docBookMgr = new DocBookWrapper(this.snapshotName, snapshotFolder);
    	try{
    		DBBook docBook = createDocBook(view);
    		docBook.setRemoveBlankPages(true);
    		Collection<EmsScriptNode> v2v = view.getView().getViewToViewPropertyViews();
    		String prodId = view.getId();
    		for(EmsScriptNode node:v2v){
    			String nodeId = node.getId();
    			if(nodeId.compareTo(prodId) == 0) continue;
				DocumentElement section = (DocumentElement)emsScriptNodeToDBSection(node, true);
				docBook.addElement(section);
    		}
    		//docBook.accept(docBookMgr);
    		docBookMgr.setDBBook(docBook);
    		docBookMgr.save();;
    	}
    	catch(Exception ex){
    		log(LogLevel.ERROR, "\nFailed to create DBBook! " + ex.getStackTrace());
    		ex.printStackTrace();
    		throw new Exception("Failed to create DBBook!", ex);
    	}
    	return docBookMgr;
    }
    
    protected String parseScriptNodeIdFromSnapshotId(String snapshotId){
    	return snapshotId.substring(0, snapshotId.lastIndexOf("_"));
    }
    
    protected EmsScriptNode findScriptNodeBySnapshot(EmsScriptNode snapshot){
    	//System.out.println("snapshot.toJSON(): " + snapshot.toJSON());
    	Date dtCreated = (Date)snapshot.getProperty("cm:created");
    	String id = (String)snapshot.getProperty(Acm.ACM_ID);
    	System.out.println("id: " + id);
    	id = parseScriptNodeIdFromSnapshotId(id);
    	System.out.println("dtCreated: " + dtCreated);
    	return findScriptNodeById(id, dtCreated);
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
        if (snapshotNode == null) {
            snapshotNode = parent.createFolder("snapshots");
        }

        return snapshotNode;
    }
}
