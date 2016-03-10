package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.docbook.model.DBImage;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.HtmlToPdfActionExecuter;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
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

public class HtmlToPdfPost extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(HtmlToPdfPost.class);
	protected String dirRoot = "/mnt/alf_data/temp/";
	protected UUID guid = UUID.randomUUID();
	
	//dir containing full doc generation resources (prerenderer.io, phantomJS, wkhtmltopdf)
	protected String fullDocGenDir = "/usr/local/bin/";
//	protected String htmlPath;
	private String user, storeName, nodeId, filename;
	
	public HtmlToPdfPost(){
		super();
	}
	
	public HtmlToPdfPost(Repository repositoryHelper, ServiceRegistry registry){
		super(repositoryHelper, registry);
	}
	
	@Override
	protected boolean validateRequest(WebScriptRequest reg, Status status){
		return false;
	}
	
	@Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		HtmlToPdfPost instance = new HtmlToPdfPost(repository, services);
        return instance.executeImplImpl(req, status, cache, runWithoutTransactions);		    
    }
	
	@Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req,
			Status status, Cache cache) {
        if (logger.isInfoEnabled()) {
            String user = AuthenticationUtil.getRunAsUser();
            logger.info( user + " " + req.getURL() );
        }

		Map<String, Object> model = new HashMap<String, Object>();

        printHeader( req );

        //clearCaches();

        HtmlToPdfPost instance = new HtmlToPdfPost(repository, services);

		JSONObject result = instance.saveAndStartAction(req, status);
		appendResponseStatusInfo(instance);

		status.setCode(responseStatus.getCode());
		if (result == null) {
		    model.put("res", createResponseJson());
		} else {
		    try {
		    	if (!Utils.isNullOrEmpty(response.toString())) result.put("message", response.toString());
                model.put("res", NodeUtil.jsonToString( result, 2 ));
            } catch ( JSONException e ) {
                e.printStackTrace();
            }
		}

        printFooter();

		return model;
        
        
        
        
        
        
        
        /*
		String htmlFilename = "test.html";
		String pdfFilename = "test.pdf";
		String htmlContent = "<html><body>Hello, world!</body></html>";
		String docId;
		String timeStamp;
		String ws;
		DateTime now = new DateTime();
		
		try {
			JSONObject reqPostJson = (JSONObject) req.parseContent();
			if (reqPostJson != null) {
				System.out.println("JSON: " + reqPostJson);
				if (reqPostJson.has("document")) {
					JSONArray document = reqPostJson.getJSONArray("document");
					if (document != null) {
						JSONObject json = document.getJSONObject(0);
						htmlContent = json.optString("html");
						docId = json.optString("docId");
						timeStamp = json.optString("time");
						ws = json.optString("workspace");
						
						htmlFilename = String.format("%s_%s_%s.html", docId, timeStamp, now.getMillis());
						pdfFilename = String.format("%s_%s_%s.pdf", docId, timeStamp, now.getMillis());
					}
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

		String user = AuthenticationUtil.getRunAsUser();
		EmsScriptNode userHomeFolder = getUserHomeFolder(user);
		createUserFilesystemPath(user);
		saveHtmlToRepo(userHomeFolder, htmlFilename, htmlContent);
		String htmlPath = saveHtmlToFilesystem(user, htmlFilename, htmlContent);

		try {
			String pdfPath = html2pdf(user, htmlPath, pdfFilename,
					userHomeFolder);
			EmsScriptNode pdfNode = savePdfToRepo(userHomeFolder, pdfPath);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		
		
		//WorkspaceNode workspace = getWorkspace(req);
		//EmsScriptNode siteNode = getSiteNodeFromRequest(req, false);
		//String docId = req.getServiceMatch().getTemplateVars()
		//		.get("documentId");
		// EmsScriptNode node = NodeUtil.findScriptNodeById(docId, workspace,
		// null, true, services, response);
        
        model.put("res", "{\"status\": 200}");
        return model;
        */
	}
	
	public EmsScriptNode convert(String docId, String tagId, String timeStamp,
			String htmlContent, String coverContent, String headerContent, String footerContent) {
		EmsScriptNode pdfNode = null;
		String htmlFilename = "test.html";
		String pdfFilename = "test.pdf";
		String coverFilename = "cover.html";
		DateTime now = new DateTime();
		response.append(String.format("Converting HTML to PDF for document Id %s, tag Id: %s, timestamp: %s...", docId, tagId, timeStamp));
		
		try {
			htmlFilename = String.format("%s_%s_%s.html", docId, timeStamp,
					now.getMillis()).replace(":", "");
			pdfFilename = String.format("%s_%s_%s.pdf", docId, timeStamp,
					now.getMillis()).replace(":", "");
			coverFilename = String.format("%s_%s_%s_cover.html", docId,
					timeStamp, now.getMillis()).replace(":", "");
			this.user = AuthenticationUtil.getRunAsUser();
			EmsScriptNode userHomeFolder = getUserHomeFolder(user);
			createUserFilesystemPath(user);
			String htmlPath = saveHtmlToFilesystem(user, htmlFilename,
					htmlContent, coverFilename, coverContent);
			handleEmbeddedImage(htmlFilename);
			saveHtmlToRepo(userHomeFolder, htmlFilename, htmlContent);
			String pdfPath = html2pdf(docId, tagId, timeStamp, user, htmlPath,
					pdfFilename, coverFilename, userHomeFolder, headerContent, footerContent);
			pdfNode = savePdfToRepo(userHomeFolder, pdfPath);
		} catch (Exception ex) {
			response.append(String.format("ERROR: Failed to convert HTML to PDF for document Id: %s, tag Id: %s, timestamp: %s! %s", docId, tagId, timeStamp, ex.getMessage()));
			System.out.println(ex.getMessage());
		}
		return pdfNode;
	}
	
	protected EmsScriptNode getUserHomeFolder(String user) {
		return NodeUtil.getUserHomeFolder(user, true);
	}
	
	protected void createUserFilesystemPath(String user) {
		Path userPath = Paths.get(dirRoot, user, this.guid.toString());
		try {
			File up = new File(userPath.toString());
			if (!up.exists()) {
				up.mkdirs();
			}
		} catch (Exception ex) {
			logger.warn(ex.getMessage(), ex);
		}
	}
	
	protected void saveHtmlToRepo(EmsScriptNode userHomeFolder,
			String htmlFilename, String htmlContent) {
		EmsScriptNode htmlNode = NodeUtil.getOrCreateContentNode(
				userHomeFolder, htmlFilename, services);
		if (htmlNode == null) {
			logger.error("Failed to create HTML noderef.");
			return;
		}
		ActionUtil.saveStringToFile(htmlNode, "text/html", services,
				htmlContent);
	}
	
	protected EmsScriptNode savePdfToRepo(EmsScriptNode userHomeFolder,
			String pdfPath) {
		Path path = Paths.get(pdfPath);
		if(!Files.exists(path)){
			response.append(String.format("PDF generation failed! Unable to locate PDF file at %s!", pdfPath));
			return null;
		}
		
		EmsScriptNode pdfNode = NodeUtil.getOrCreateContentNode(userHomeFolder,
				Paths.get(pdfPath).getFileName().toString(), services);
		if (pdfNode == null) {
			logger.error("Failed to create PDF nodeRef.");
			return pdfNode;
		}
		if (!saveFileToRepo(pdfNode, MimetypeMap.MIMETYPE_PDF, pdfPath)) {
			logger.error("Failed to save PDF artifact to repository!");
		}
		return pdfNode;
	}
	
	protected boolean saveFileToRepo(EmsScriptNode scriptNode, String mimeType,
			String filePath) {
		boolean bSuccess = false;
		if (filePath == null || filePath.isEmpty()) {
			System.out.println("File path parameter is missing!");
			return false;
		}
		if (!Files.exists(Paths.get(filePath))) {
			System.out.println(filePath + " does not exist!");
			return false;
		}

		NodeRef nodeRef = scriptNode.getNodeRef();
		ContentService contentService = scriptNode.getServices()
				.getContentService();

		ContentWriter writer = contentService.getWriter(nodeRef,
				ContentModel.PROP_CONTENT, true);
		writer.setLocale(Locale.US);
		File file = new File(filePath);
		writer.setMimetype(mimeType);
		try {
			writer.putContent(file);
			bSuccess = true;
		} catch (Exception ex) {
			System.out.println("Failed to save '" + filePath
					+ "' to repository!");
		}
		return bSuccess;
	}

	protected void copyCssFilesToWorkingDir(String user) {
		Path cssPath = Paths
				.get("/opt/local/apache-tomcat/webapps/alfresco/mmsapp/css");
		try {
			FileUtils.copyDirectory(new File(cssPath.toString()), new File(
					Paths.get(dirRoot, user, guid.toString(), "css").toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected String addCssLinks(String htmlContent) throws Exception{
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if(document == null){
			throw new Exception("Failed to parse HTML content!");
		}
		Element head = document.head();
		head.append("<link href=\"https://fonts.googleapis.com/css?family=Droid+Serif:400,700,400italic,700italic\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<link href=\"css/mm-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<link href=\"css/ve-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<style type=\"text/css\">tr {page-break-inside:avoid;} .ng-hide {display:none;} TABLE {width:100%} </style>");
		
		return document.toString();
	}

	/**
	 * @param user
	 * @param htmlFilename
	 * @param htmlContent
	 * @return path to saved HTML file.
	 */
	protected String saveHtmlToFilesystem(String user, String htmlFilename,
			String htmlContent, String coverFilename, String coverContent) {
		Path htmlPath = Paths.get(dirRoot, user, guid.toString(), htmlFilename);
		Path coverPath = Paths.get(dirRoot, user, guid.toString(), coverFilename);

		try {
			if (Files.exists(htmlPath)) {
				// TODO file already exists, should we override?
				// return htmlPath.toString();
			}
			copyCssFilesToWorkingDir(user);
			htmlContent = addCssLinks(htmlContent);
			
			File htmlFile = new File(htmlPath.toString());
			BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile));
			bw.write(htmlContent);
			bw.close();
			
			File coverFile = new File(coverPath.toString());
			bw = new BufferedWriter(new FileWriter(coverFile));
			bw.write(coverContent);
			bw.close();
		} catch (Exception ex) {
			// TODO error handling/logging
			logger.error(ex.getMessage(), ex);
		}
		return htmlPath.toString();
	}
	
	protected String html2pdf(String docId, String tagId, String timeStamp, String user, String htmlPath, String pdfFilename, String coverFilename,
			EmsScriptNode userHomeFolder, String headerContent, String footerContent)
			throws Exception {
		if (!Files.exists(Paths.get(htmlPath))) {
			throw new Exception(
					String.format(
							"Failed to transform HTML to PDF. Expected %s HTML file but it does not exist!",
							htmlPath));
		}
		
		String pdfPath = Paths.get(dirRoot, user, guid.toString(), pdfFilename).toString();
		String coverPath = Paths.get(dirRoot, user, guid.toString(), coverFilename).toString();

		//createCoverPage(docId, tagId, timeStamp, htmlPath, coverPath); //NEED FOR COVER
		// createFooterPage(this.footerPath);
		// createHeaderPage(this.headerPath);
		// String tagName = this.getTimeTagName();
		String tagName = tagId;
		
		if(Utils.isNullOrEmpty(headerContent)) headerContent = "";
		if(Utils.isNullOrEmpty(footerContent)) footerContent = "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.";

		List<String> command = new ArrayList<String>();
		command.add("wkhtmltopdf");
		command.add("-q");
		command.add("--load-error-handling");
		command.add("ignore");
		command.add("--load-media-error-handling");
		command.add("ignore");
		command.add("--header-line");
		command.add("--header-font-size");
		command.add("8");
		command.add("--header-font-name");
		command.add("\"Times New Roman\"");
		command.add("--header-right");
		command.add(tagName);
		command.add("--footer-line");
		command.add("--footer-font-size");
		command.add("8");
		command.add("--footer-font-name");
		command.add("\"Times New Roman\"");
		// command.add("--footer-left");
		// command.add(tagName.substring(0,10));
		command.add("--footer-center");
		command.add(footerContent);
		command.add("--footer-right");
		command.add("[page]");
		// command.add("--footer-html");
		// command.add(this.footerPath);
		 command.add("cover"); //NEED FOR COVER
		 command.add(coverPath); //NEED FOR COVER
		// command.add("--dump-outline");
		// command.add(Paths.get(this.fullDocDir,"toc.xml").toString());
		command.add("toc");
		command.add("--toc-level-indentation");
		command.add("10");
		// command.add("--xsl-style-sheet");
		command.add(Paths
				.get(this.fullDocGenDir, "wkhtmltopdf/xsl/default.xsl")
				.toString());
		// command.add("--footer-center");
		// command.add("Page [page] of [toPage]");
		command.add(htmlPath);
		command.add(pdfPath);

		System.out.println("htmltopdf command: " + command.toString().replace(",", ""));
		response.append("htmltopdf command: " + command.toString().replace(",", ""));

		int attempts = 0;
		int ATTEMPTS_MAX = 3;
		boolean success = false;
		RuntimeExec exec = null;
		ExecutionResult execResult = null;
		Runtime rt = Runtime.getRuntime();
		Process process = null;

		boolean runProcess = true;
		while (attempts++ < ATTEMPTS_MAX && !success) {
			if (!runProcess) {
				exec = new RuntimeExec();
				exec.setProcessDirectory(this.fullDocGenDir);
				exec.setCommand(list2Array(command));
				execResult = exec.execute();
			} else {
				ProcessBuilder pb = new ProcessBuilder(command);
				File file = new File(this.fullDocGenDir);
				pb.directory(file);
				
				process = pb.start();
				process.waitFor();
			}
			if (Files.exists(Paths.get(pdfPath))) {
				success = true;
				break;
			}
			Thread.sleep(5000);
		}

		if (!success && !Files.exists(Paths.get(pdfPath))) {
			String msg = null;
			if (!runProcess) {
				msg = String
						.format("Failed to transform HTML file '%s' to PDF. Exit value: %d",
								htmlPath, execResult);
			} else {
				msg = String
						.format("Failed to transform HTML file '%s' to PDF. Exit value: %d",
								htmlPath, process.exitValue());
			}
			log(Level.ERROR, msg);
			throw new Exception(msg);
		}

		// this.savePdfToRepo(snapshotFolder, pdfNode);

		return pdfPath;
	}
	
	/**
	 * Helper method to convert a list to an array of specified type
	 * @param list
	 * @return
	 */
	private String[] list2Array(List<String> list) {
		return Arrays.copyOf(list.toArray(), list.toArray().length, String[].class);
	}
	
	/**
	 * Save off the configuration set and kick off snapshot creation in
	 * background
	 *
	 * @param req
	 * @param status
	 */
	private JSONObject saveAndStartAction(WebScriptRequest req, Status status) {
//		String htmlFilename = "test.html";
//		String pdfFilename = "test.pdf";
//		String htmlContent = "<html><body>Hello, world!</body></html>";
//		String docId;
//		String timeStamp;
//		String ws;
//		DateTime now = new DateTime();
		
		JSONObject postJson = null;
        WorkspaceNode workspace = getWorkspace( req );
        EmsScriptNode siteNode = getSiteNodeFromRequest( req, false );
		JSONObject reqPostJson = (JSONObject) req.parseContent();
		if (reqPostJson != null) {
			postJson = reqPostJson;
			if (reqPostJson.has("documents")) {
				JSONArray documents = reqPostJson.getJSONArray("documents");
				if (documents != null) {
					JSONObject json = documents.getJSONObject(0);
//					htmlContent = json.optString("html");
//					docId = json.optString("docId");
//					timeStamp = json.optString("time");
//					ws = json.optString("workspace");
					
//					htmlFilename = String.format("%s_%s_%s.html", docId, timeStamp, now.getMillis());
//					pdfFilename = String.format("%s_%s_%s.pdf", docId, timeStamp, now.getMillis());
					
					String user = AuthenticationUtil.getRunAsUser();
					EmsScriptNode userHomeFolder = getUserHomeFolder(user);
					
					postJson = handleCreate(json, siteNode.getName(), userHomeFolder, workspace, status);
				}
			}
		}

		return postJson;
	}
	
	private JSONObject handleCreate(JSONObject postJson, String siteName, EmsScriptNode context,
			WorkspaceNode workspace, Status status)
			throws JSONException {
		EmsScriptNode jobNode = null;

		if (postJson.has("name")) {
			String name = postJson.getString("name");
			if (ActionUtil.jobExists(context, name))
				return postJson;

			jobNode = ActionUtil.getOrCreateJob(context, name, "cm:content",
					status, response, true);

			if (jobNode != null) {
				startAction(jobNode, postJson, siteName, workspace);
				return postJson;
			} else {
				log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
						"Couldn't create HTML to PDF job: %s", name);
				return null;
			}
		} else {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
					"Job name not specified");
			return null;
		}
	}
	
	/**
	 * Kick off the actual action in the background
	 * @param jobNode
	 * @param siteName
	 * @param productList
	 * @param workspace
	 */
	public void startAction(EmsScriptNode jobNode, JSONObject postJson, String siteName,
	                        WorkspaceNode workspace) {
		ActionService actionService = services.getActionService();
		Action htmlToPdfAction = actionService.createAction(HtmlToPdfActionExecuter.NAME);
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_SITE_NAME, siteName);
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_WORKSPACE, workspace);
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_DOCUMENT_ID, postJson.optString("docId"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TAG_ID, postJson.optString("tagId"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TIME_STAMP, postJson.optString("time"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_COVER, postJson.optString("cover"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_HTML, postJson.optString("html"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_HEADER, postJson.optString("header"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_FOOTER, postJson.optString("footer"));
		services.getActionService().executeAction(htmlToPdfAction, jobNode.getNodeRef(), true, true);
	}

	public void createCoverPage(String docId, String tagId, String timeStamp, String htmlPath, String coverDestination) throws IOException{
    	if(!Files.exists(Paths.get(htmlPath))) return;

        File htmlFile = new File(htmlPath);
        File coverFile = new File (coverDestination);
        Document document = Jsoup.parse(htmlFile, "UTF-8", "");
        if(document == null) return;
        ArrayList<String> l  = new ArrayList<String>();
        
        Elements headers = document.getElementsByTag("mms-transclude-name");
        for(Element header :headers){
            l.add(header.text());
        }

        String coverHeader = docId;
		if (!l.isEmpty()) {
			coverHeader = l.get(0);
		}
        
		String legalNotice = "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.";
        String pageLegalNotice = "This Document has not been reviewed for export control. Not for distribution to or access by foreign persons.";
        String jplName = "Jet Propulsion Laboratory";
        String caltechName = "California Institute of Technology";
//        Date date = this.getTime();
//        String tag = this.getTimeTagName();
        String tagTime = timeStamp;
        if(timeStamp.equalsIgnoreCase("latest")) tagTime = new Date().toString();
        String tag = tagId;
                            
        String coverHtml = //"<!DOCTYPE html>" //seems to not print formatted if keep the DOCTYPE
        		//+ 
        		"<html>"
                + "<head><title>" + coverHeader + "</title></head>"
                + "<body style= \"width:100%; height:100%;\">"
                    + "<div style=\"top:10%; left:10%; right: 10%; position:absolute;\">"
                    + "<center><h2>" + coverHeader + "</h2></center>"
                    +"</div>"
                    + "<div style=\"top:60%; left:10%; right:10%; position:absolute;\">"
                    + "<div>" + legalNotice + "<br/>"
                    +   "<i>" + pageLegalNotice + "</i></div>"
                    +"</div>"                       
                    + "<div style=\"top:70%; left:10%; position:absolute;\">"
                    +   "<div>" + tagTime + "<br/>" + tag +  "</div>" //"<div>" + date + "</div>" 
                    +"</div>"
                    + "<div style=\"top:85%; left:10%; position:absolute;\">"
                    + "<div>"
                    + "<img src=\"http://div27.jpl.nasa.gov/2740/files/logos/jpl_logo%28220x67%29.jpg\" alt=\"JPL Logo\"/>"
                    + "<p style=\"color:#B6B6B4\">" + jplName + "<br/><i>" + caltechName + "</i></p>" //did separate jpl/caltech label to always have the stamp on pdf
                    + "</div>"
                    + "</div>"
                + "</body>"
                + "</html>";
        BufferedWriter bw = new BufferedWriter(new FileWriter(coverFile));
        bw.write(coverHtml);
        bw.close();
    } 
	
    public void handleEmbeddedImage(String htmlFilename) throws Exception
    {
    	Path htmlPath = Paths.get(dirRoot, this.user, guid.toString(), htmlFilename);
    	if(!Files.exists(htmlPath)) return;

    	File htmlFile = new File(htmlPath.toString());
    	Document document = Jsoup.parse(htmlFile, "UTF-8", "");
    	if(document == null) return;

    	Elements images = document.getElementsByTag("img");

    	for(final Element image : images){
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
                    String versionStore = "versionStore/version2Store/";

                	// is image local or remote resource?
                	if(embedHostname.compareToIgnoreCase(hostname)==0 || src.startsWith("/alfresco/") || src.contains(alfrescoContext.toLowerCase()) || src.contains(versionStore.toLowerCase())){
                		//local server image > generate image tags
                		String filePath = url.getFile();
                		if(filePath == null || filePath.isEmpty()) continue;

                		nodeId = null;
                		storeName = null;
                		if(filePath.contains(alfrescoContext)){
                			//filePath = "alfresco/d/d/" + filePath.substring(filePath.indexOf(alfrescoContext));
                			nodeId = filePath.substring(filePath.indexOf(alfrescoContext) + alfrescoContext.length());
                			nodeId = nodeId.substring(0, nodeId.indexOf("/"));
                			storeName = "workspace://SpacesStore/";
                		}
                    if(filePath.contains(versionStore)){
                        //filePath = "alfresco/d/d/" + filePath.substring(filePath.indexOf(alfrescoContext));
                        nodeId = filePath.substring(filePath.indexOf(versionStore) + versionStore.length());
                        nodeId = nodeId.substring(0, nodeId.indexOf("/"));
                        storeName = "versionStore://version2Store/";
                    }
                		if(nodeId == null || nodeId.isEmpty()) continue;

                		filename = filePath.substring(filePath.lastIndexOf("/") + 1);
                		try{
                		    // This is the trouble area, where each image needs its own transaction:
                		    new EmsTransaction(services, response, new Status()) {
                	            @Override
                	            public void run() throws Exception {
                                    DBImage dbImage = retrieveEmbeddedImage(user, storeName, nodeId, filename, null, null);
                                    if (dbImage != null) {
                                        image.attr("src", dbImage.getFilePath());
                                    }
                	            }
                	        };
//	                		String inlineImageTag = buildInlineImageTag(nodeId, dbImage);
//	                		image.before(inlineImageTag);
//	                		image.remove();
                		}
                		catch(Exception ex){
                			//in case it's not a local resource > generate hyperlink instead
//                			image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ", src, url.getFile()));
//                    		image.remove();
                		}
                	}
                	else{	//remote resource > generate a hyperlink
//                		image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ", src, url.getFile()));
//                		image.remove();
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
    	try{
    		FileUtils.writeStringToFile(htmlFile, document.outerHtml(), "UTF-8");
    	}
    	catch(Exception ex){
    		log(Level.ERROR, "Failed to save modified HTML %s. %s", htmlPath, ex.getMessage());
        	ex.printStackTrace();
    	}
    }
    
    protected void handleRelativeHyperlinks(String user, String htmlFilename) throws Exception{
    	Path htmlPath = Paths.get(dirRoot, user, guid.toString(), htmlFilename);
    	if(!Files.exists(htmlPath)) return;

    	File htmlFile = new File(htmlPath.toString());
    	
    	Document document;
    	try{
    		document = Jsoup.parse(htmlFile, "UTF-8", "");
    	}
    	catch(Exception ex){
    		log(Level.ERROR, "Failed to load HTML file '%s' to handle relative hyperlinks. %s", htmlPath, ex.getMessage());
        	throw ex;
    	}
    	
    	if(document == null) return;
    	
    	Elements links = document.getElementsByTag("a");
    	for(Element elem:links){
			String href = elem.attr("href").toLowerCase();
			if(!href.startsWith("http")){
				HostnameGet hng = new HostnameGet(this.repository, this.services);
				String hostname = hng.getAlfrescoUrl();
				String alfrescoUrl = hostname + "/alfresco";
				
				if(href.startsWith("service")) href = href.replace("service", alfrescoUrl + "/service");
				else if(href.startsWith("ve.html#") ||
						href.startsWith("mms.html#") ||
						href.startsWith("docweb.html#")){ 
					href = String.format("%s/%s", alfrescoUrl, href);
				}
				else if(href.startsWith("share")) href = href.replace("share", hostname + "/share");
				elem.attr("href", href);
			}
    	}

    	try{
    		FileUtils.writeStringToFile(htmlFile, document.outerHtml(), "UTF-8");
    	}
    	catch(Exception ex){
    		log(Level.ERROR, "Failed to save HTML file '%s' after handling relative hyperlinks. %s", htmlPath, ex.getMessage());
        	throw ex;
    	}
    }
    

    private DBImage retrieveEmbeddedImage(String user, String storeName, String nodeId, String imgName, WorkspaceNode workspace, Object timestamp) throws UnsupportedEncodingException{
    	Path imageDirName = Paths.get(dirRoot, user, guid.toString(), "images");
		NodeRef imgNodeRef = NodeUtil.getNodeRefFromNodeId(storeName, nodeId);
		if(imgNodeRef == null) return null;

		imgName = URLDecoder.decode(imgName, "UTF-8");
		String imgFilename = imageDirName + File.separator + imgName;
		
		File imgFile = new File(imgFilename);
		ContentReader imgReader;
		imgReader = this.services.getContentService().getReader(imgNodeRef, ContentModel.PROP_CONTENT);
		if(!Files.exists(imageDirName)){
			if(!new File(imageDirName.toString()).mkdirs()){
				System.out.println("Failed to create directory for " + imageDirName);
			}
		}
		imgReader.getContent(imgFile);

		DBImage image = new DBImage();
		image.setId(nodeId);
		image.setFilePath("images/" + imgName);
		return image;
    }


    private String getHostname(){
        	SysAdminParams sysAdminParams = this.services.getSysAdminParams();
        	String hostname = sysAdminParams.getAlfrescoHost();
        	if(hostname.startsWith("ip-128-149")) hostname = "localhost";
        	return String.format("%s://%s", sysAdminParams.getAlfrescoProtocol(), hostname);
    }

}
