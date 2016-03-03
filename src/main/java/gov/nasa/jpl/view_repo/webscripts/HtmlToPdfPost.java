package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.docbook.model.DBImage;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.HtmlToPdfActionExecuter;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
	protected String userHomeSubDirName; // format: docId_time
	protected EmsScriptNode nodeUserHomeSubDir;
	
	protected UUID guid = UUID.randomUUID();
	
	//filesystem working directory
	protected String fsWorkingDir;	//format: dirRoot + "[USER_ID]/[GUID]

	// dir containing full doc generation resources (prerenderer.io, phantomJS,
	// wkhtmltopdf)
	protected String fullDocGenDir = "/opt/local/fullDocGen/";
	private String user, storeName, nodeId, filename;

	public HtmlToPdfPost() {
		super();
	}

	public HtmlToPdfPost(Repository repositoryHelper, ServiceRegistry registry) {
		super(repositoryHelper, registry);
	}

	@Override
	protected boolean validateRequest(WebScriptRequest reg, Status status) {
		return false;
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		HtmlToPdfPost instance = new HtmlToPdfPost(repository, services);
		return instance.executeImplImpl(req, status, cache,
				runWithoutTransactions);
	}

	@Override
	protected Map<String, Object> executeImplImpl(WebScriptRequest req,
			Status status, Cache cache) {
		if (logger.isInfoEnabled()) {
			String user = AuthenticationUtil.getRunAsUser();
			logger.info(user + " " + req.getURL());
		}

		Map<String, Object> model = new HashMap<String, Object>();
		printHeader(req);
		// clearCaches();

		HtmlToPdfPost instance = new HtmlToPdfPost(repository, services);

		JSONObject result = instance.saveAndStartAction(req, status);
		appendResponseStatusInfo(instance);

		status.setCode(responseStatus.getCode());
		if (result == null) {
			model.put("res", createResponseJson());
		} else {
			try {
				if (!Utils.isNullOrEmpty(response.toString()))
					result.put("message", response.toString());
				model.put("res", NodeUtil.jsonToString(result, 2));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		printFooter();
		return model;
	}

	protected void setUserHomeSubDir(EmsScriptNode userHomeFolder, String docId)
			throws Throwable {
		log("Creating 'user home' subdirectory...");
		this.userHomeSubDirName = String.format("%s_%s", docId,
				new DateTime().getMillis());
		try {
			this.nodeUserHomeSubDir = userHomeFolder
					.createFolder(this.userHomeSubDirName);
		} catch (Throwable ex) {
			log(String.format(
					"ERROR: Failed to create user-home subdirectory! %s",
					ex.getMessage()));
			throw new Throwable();
		}
	}

	public EmsScriptNode convert(String docId, String tagId, String timeStamp,
			String htmlContent, String coverContent, String headerContent,
			String footerContent, String docNum, String displayTime, String customCss) {
		EmsScriptNode pdfNode = null;
		String htmlFilename = String.format("%s_%s.html", docId, timeStamp)
				.replace(":", "");
		String pdfFilename = String.format("%s_%s.pdf", docId, timeStamp)
				.replace(":", "");
		String coverFilename = String.format("%s_%s_cover.html", docId,
				timeStamp).replace(":", "");
		String footerFilename = String.format("%s_%s_footer.html", docId,
				timeStamp).replace(":", "");
		String headerFilename = String.format("%s_%s_header.html", docId,
				timeStamp).replace(":", "");
		String zipFilename = String.format("%s_%s.zip", docId,
				timeStamp).replace(":", "");
		log(String
				.format("Converting HTML to PDF for document Id: %s, tag Id: %s, timestamp: %s...",
						docId, tagId, timeStamp));

		if (Utils.isNullOrEmpty(footerContent))
			footerContent = "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.";
		
		try {
			log("Getting 'run as user'...");
			this.user = AuthenticationUtil.getRunAsUser();
			EmsScriptNode userHomeFolder = getUserHomeFolder(user);

			setUserHomeSubDir(userHomeFolder, docId);
			log(String.format("Created \"%s\" subdirectory!",
					this.userHomeSubDirName));

			createUserFilesystemPath(user);
			String htmlPath = saveHtmlToFilesystem(htmlFilename, htmlContent,
					coverFilename, coverContent, footerFilename, footerContent, headerFilename, headerContent, tagId, timeStamp, docNum, displayTime, customCss);

			handleEmbeddedImage(htmlFilename);
//			saveCoverToRepo(coverFilename, coverContent);
			saveHtmlToRepo(htmlFilename, htmlContent);

			String pdfPath = html2pdf(docId, tagId, timeStamp, htmlPath, pdfFilename,
					coverFilename, userHomeFolder, headerFilename, footerFilename);

			pdfNode = savePdfToRepo(pdfPath);
			
			String zipPath = zipWorkingDir(zipFilename);
			saveZipToRepo(zipPath);
		} catch (Throwable ex) {
			response.append(String
					.format("ERROR: Failed to convert HTML to PDF for document Id: %s, tag Id: %s, timestamp: %s! %s",
							docId, tagId, timeStamp, ex.getMessage()));
			ex.printStackTrace();
		}
		return pdfNode;
	}

	protected EmsScriptNode getUserHomeFolder(String user) {
		log("Retrieving 'user home'...", true);
		return NodeUtil.getUserHomeFolder(user, true);
	}

	protected void createUserFilesystemPath(String user) throws Throwable {
		log("Creating filesystem directory to store working files...");
		Path userPath = Paths.get(dirRoot, user, this.guid.toString());
		this.fsWorkingDir = userPath.toString();
		
		try {
			File up = new File(this.fsWorkingDir);
			if (!up.exists()) {
				up.mkdirs();
			}
		} catch (Throwable ex) {
			if(!Files.exists(Paths.get(this.fsWorkingDir))){
				log(String.format("Unable to create directory %s...%s", this.fsWorkingDir, ex.getMessage()));
				throw new Throwable();
			}
		}
	}

	protected void saveCoverToRepo(String coverFilename, String coverContent) {
		log(String.format("Saving %s to repository...", coverFilename));
		try{
			EmsScriptNode htmlNode = createScriptNode(coverFilename);
			ActionUtil.saveStringToFile(htmlNode, "text/html", services,
				coverContent);
		}
		catch(Throwable ex){
			//allowing process to continue;
			log("Failed to save %s to repository!");
			log(ex.getMessage());
		}
	}

	protected EmsScriptNode saveHtmlToRepo(String htmlFilename, String htmlContent) throws Throwable {
		log(String.format("Saving %s to repository...", htmlFilename));
		Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);
		
		if(!Files.exists(htmlPath)){
			try{
				return saveStringToRepo(htmlFilename, htmlContent, "text/html");
			}
			catch(Throwable ex){
				response.append("Failed to save HTML content to repository!");
				throw new Throwable();
			}
		}
		else{
			try{
				return saveFileToRepo(htmlFilename, "text/html", htmlPath.toString());
			}
			catch(Throwable ex){
				response.append(ex.getMessage());
				response.append(String.format("Failed to save %s to repository!", htmlFilename));
				throw new Throwable();
			}
		}
	}

	protected EmsScriptNode createScriptNode(String filename){
		EmsScriptNode node = NodeUtil.getOrCreateContentNode(this.nodeUserHomeSubDir, filename, services);
		if(node == null) response.append(String.format("Failed to create nodeRef for %s", filename));
		return node;
	}
	
	protected EmsScriptNode saveStringToRepo(String filename, String content, String mimeType) throws Throwable{
		EmsScriptNode node = createScriptNode(filename);
		if(node == null) throw new Throwable();
		ActionUtil.saveStringToFile(node, mimeType, services,
				content);
		return node;
	}
	
	protected EmsScriptNode savePdfToRepo(String pdfPath) {
		log(String.format("Saving %s to repository...",
				Paths.get(pdfPath).getFileName()));
		Path path = Paths.get(pdfPath);
		if (!Files.exists(path)) {
			response.append(String.format(
					"PDF generation failed! Unable to locate PDF file at %s!",
					pdfPath));
			return null;
		}

		EmsScriptNode pdfNode = NodeUtil.getOrCreateContentNode(
				this.nodeUserHomeSubDir, Paths.get(pdfPath).getFileName()
						.toString(), services);
		if (pdfNode == null) {
			logger.error("Failed to create PDF nodeRef.");
			return pdfNode;
		}
		if (!saveFileToRepo(pdfNode, MimetypeMap.MIMETYPE_PDF, pdfPath)) {
			logger.error("Failed to save PDF artifact to repository!");
		}
		return pdfNode;
	}
	
	protected EmsScriptNode saveZipToRepo(String zipPath) throws Throwable {
		String zipFilename = Paths.get(zipPath).getFileName().toString();
		log(String.format("Saving %s to repository...",
				zipFilename));
		
		try{
			return saveFileToRepo(zipFilename, MimetypeMap.MIMETYPE_ZIP, zipPath);
		}
		catch(Throwable ex){
			response.append(ex.getMessage());
			throw new Throwable("Failed to save zip artifact to repository!");
		}
	}

	protected EmsScriptNode saveFileToRepo(String filename, String mimeType,
			String filePath) throws Throwable {
		Path path = Paths.get(filePath);
		if (!Files.exists(path)) {
			response.append(String.format("Unable to locate file at %s.",
					filePath));
			throw new Throwable();
		}

		EmsScriptNode node = NodeUtil.getOrCreateContentNode(
				this.nodeUserHomeSubDir, filename, services);
		if (node == null){
			response.append("Failed to create nodeRef!");
			throw new Throwable();
		}
		if (!saveFileToRepo(node, mimeType, filePath)){
			response.append("Failed to save file to repository!");
			throw new Throwable();
		}
		return node;
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

	protected void copyCssFilesToWorkingDir() {
		log("Copying CSS files to working directory...");
		Path cssPath = Paths
				.get("/opt/local/apache-tomcat/webapps/alfresco/mmsapp/css");
		try {
			FileUtils.copyDirectory(new File(cssPath.toString()),
					new File(Paths.get(this.fsWorkingDir, "css")
							.toString()));
		} catch (IOException e) {
			//not having CSS is not critical; allow process to continue w/o CSS
			log("Failed to copy CSS files to working directory!");
			log(e.getMessage());
			e.printStackTrace();
		}
	}

	protected String addCssLinks(String htmlContent) throws Throwable {
		log("Adding CSS links to HTML...");
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		Element head = document.head();
		head.append("<meta charset=\"utf-8\" />");
		//head.append("<link href=\"https://fonts.googleapis.com/css?family=Droid+Serif:400,700,400italic,700italic\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<link href=\"css/mm-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<link href=\"css/ve-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<style type=\"text/css\">tr {page-break-inside:avoid;} .ng-hide {display:none;} TABLE {width:100%} </style>");
		//adding custom CSS link
		head.append("<link href=\"css/customStyles.css\" rel=\"stylesheet\" type=\"text/css\" />");

		return document.outerHtml();
	}

	protected String addJscripts(String htmlContent) throws Throwable{
		log("Adding JavaScripts to HTML...");
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if(document == null){
			throw new Throwable("Failed to parse HTML content!");
		}
		Element head = document.head();
		String s = getTableVerticalSplitScript();
		head.append(getTableVerticalSplitScript());
		return document.outerHtml();
	}
	
	/**
	 * @param htmlFilename
	 * @param htmlContent
	 * @param coverFilename
	 * @param coverContent
	 * @param headerFilename TODO
	 * @param headerContent TODO
	 * @param tagId TODO
	 * @param displayTime TODO
	 * @param timestamp TODO
	 * @param jplDocNum TODO
	 * @return path to saved HTML file.
	 * @throws Throwable 
	 */
	protected String saveHtmlToFilesystem(String htmlFilename,
			String htmlContent, String coverFilename, String coverContent,
			String footerFilename, String footerContent, String headerFilename,
			String headerContent, String tagId, String timeStamp,
			String docNum, String displayTime, String customCss) throws Throwable {
		log(String.format("Saving %s to filesystem...", htmlFilename));
		Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);
		Path coverPath = Paths.get(this.fsWorkingDir, coverFilename);
		Path customCssPath = Paths.get(this.fsWorkingDir, "css", "customStyles.css");

		try {
			if (Files.exists(htmlPath)) {
				// TODO file already exists, should we override?
			}
			copyCssFilesToWorkingDir();
			htmlContent = addCssLinks(htmlContent);
			htmlContent = addJscripts(htmlContent);

			File htmlFile = new File(htmlPath.toString());
			BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile));
			bw.write(htmlContent);
			bw.close();

			log(String.format("Saving %s to filesystem...", coverFilename));
			File coverFile = new File(coverPath.toString());
			bw = new BufferedWriter(new FileWriter(coverFile));
			bw.write(coverContent);
			bw.close();

			if (!Utils.isNullOrEmpty(customCss)) {
				log(String.format("Saving %s to filesystem...",
						customCssPath.getFileName()));
				File customCssFile = new File(customCssPath.toString());
				bw = new BufferedWriter(new FileWriter(customCssFile));
				bw.write(customCss);
				bw.close();
			}
			
			createFooterPage(footerFilename, footerContent);
			createHeaderPage(headerFilename, headerContent, tagId, timeStamp, docNum, displayTime);
		} catch (Throwable ex) {
			logger.error(ex.getMessage(), ex);
			log(String.format("Failed to save %s to filesystem!", htmlFilename));
			log(ex.getMessage());
			throw new Throwable();
		}
		return htmlPath.toString();
	}

	protected String html2pdf(String docId, String tagId, String timeStamp,
			String htmlPath, String pdfFilename, String coverFilename,
			EmsScriptNode userHomeFolder, String headerFilename,
			String footerFilename) throws Throwable {
		log("Converting HTML to PDF...");
		if (!Files.exists(Paths.get(htmlPath))) {
			throw new Throwable(
					String.format(
							"Failed to transform HTML to PDF. Expected %s HTML file but it does not exist!",
							htmlPath));
		}

		String pdfPath = Paths.get(this.fsWorkingDir, pdfFilename).toString();
		String coverPath = Paths.get(this.fsWorkingDir, coverFilename).toString();
		String footerPath = Paths.get(this.fsWorkingDir, footerFilename).toString();
		String headerPath = Paths.get(this.fsWorkingDir, headerFilename).toString();

		// createCoverPage(docId, tagId, timeStamp, htmlPath, coverPath); //NEED
		// FOR COVER
		// createFooterPage(this.footerPath);
		// createHeaderPage(this.headerPath);
		// String tagName = this.getTimeTagName();
//		String tagName = tagId;

//		if (Utils.isNullOrEmpty(footerContent))
//			footerContent = "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.";

		List<String> command = new ArrayList<String>();
		command.add("wkhtmltopdf");
		command.add("-q");
		command.add("--load-error-handling");
		command.add("ignore");
		command.add("--load-media-error-handling");
		command.add("ignore");
//		command.add("--header-line");
//		command.add("--header-font-size");
//		command.add("8");
//		command.add("--header-font-name");
//		command.add("\"Times New Roman\"");
		// command.add("--header-right");
//		command.add("--header-center");
//		command.add(headerContent);
		// command.add(tagName);
		
		command.add("--header-spacing");
		command.add("5");
		command.add("--header-html");
		command.add(headerPath);
		
//		command.add("--footer-line");
//		command.add("--footer-font-size");
//		command.add("8");
//		command.add("--footer-font-name");
//		command.add("\"Times New Roman\"");
		// command.add("--footer-left");
		// command.add(tagName.substring(0,10));
		
//		command.add("--footer-center");
//		command.add(footerFilename);
//		command.add("--footer-right");
//		command.add("[page]");
		
		command.add("--footer-spacing");
		command.add("5");
		command.add("--footer-html");
		command.add(footerPath);
		
		command.add("cover"); // NEED FOR COVER
		command.add(coverPath); // NEED FOR COVER
		// command.add("--dump-outline");
		// command.add(Paths.get(this.fullDocDir,"toc.xml").toString());
		command.add("toc");
		command.add("--toc-header-text");
		command.add("Table of Contents");
		
//		command.add("--toc-level-indentation");
//		command.add("10");
//		command.add("--xsl-style-sheet");
//		command.add(Paths
//				.get(this.fullDocGenDir, "wkhtmltopdf/xsl/default.xsl")
//				.toString());
		// command.add("--footer-center");
		// command.add("Page [page] of [toPage]");
		command.add(htmlPath);
		command.add(pdfPath);

		System.out.println("htmltopdf command: "
				+ command.toString().replace(",", ""));
		log("htmltopdf command: " + command.toString().replace(",", ""));

		int attempts = 0;
		int ATTEMPTS_MAX = 3;
		boolean success = false;
		RuntimeExec exec = null;
		ExecutionResult execResult = null;
//		Runtime rt = Runtime.getRuntime();
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
			log(msg);
			throw new Throwable(msg);
		}
		return pdfPath;
	}

	/**
	 * Helper method to convert a list to an array of specified type
	 * 
	 * @param list
	 * @return
	 */
	private String[] list2Array(List<String> list) {
		return Arrays.copyOf(list.toArray(), list.toArray().length,
				String[].class);
	}

	/**
	 * Save off the configuration set and kick off snapshot creation in
	 * background
	 * 
	 * @param req
	 * @param status
	 */
	private JSONObject saveAndStartAction(WebScriptRequest req, Status status) {
		JSONObject postJson = null;
		WorkspaceNode workspace = getWorkspace(req);
		EmsScriptNode siteNode = getSiteNodeFromRequest(req, false);
		JSONObject reqPostJson = (JSONObject) req.parseContent();
		if (reqPostJson != null) {
			postJson = reqPostJson;
			if (reqPostJson.has("documents")) {
				JSONArray documents = reqPostJson.getJSONArray("documents");
				if (documents != null) {
					JSONObject json = documents.getJSONObject(0);
					String user = AuthenticationUtil.getRunAsUser();
					EmsScriptNode userHomeFolder = getUserHomeFolder(user);

					postJson = handleCreate(json, siteNode.getName(),
							userHomeFolder, workspace, status);
				}
			}
		}

		return postJson;
	}

	private JSONObject handleCreate(JSONObject postJson, String siteName,
			EmsScriptNode context, WorkspaceNode workspace, Status status)
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
	 * 
	 * @param jobNode
	 * @param siteName
	 * @param productList
	 * @param workspace
	 */
	public void startAction(EmsScriptNode jobNode, JSONObject postJson,
			String siteName, WorkspaceNode workspace) {
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
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_DOC_NUM, postJson.optString("docNum"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_DISPLAY_TIME, postJson.optString("displayTime"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_CUSTOM_CSS, postJson.optString("customCss"));
		services.getActionService().executeAction(htmlToPdfAction, jobNode.getNodeRef(), true, true);
	}
	
	protected void createFooterPage(String footerFilename, String footerContent) throws IOException{
		log(String.format("Saving %s to filesystem...", footerFilename));
		Path footerPath = Paths.get(this.fsWorkingDir, footerFilename);
		
    	File footerFile = new File(footerPath.toString());
    	StringBuffer html = new StringBuffer();
    	html.append("<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        html.append("<html><head><title></title>");
        html.append("	<style type=\"text/css\">");
        html.append("		BODY{font-family:\"Times New Roman\"; margin:0; padding: 0;}");
        html.append("		HR{width: 100%;margin: 0 auto;}");
        html.append("		.itar{text-align:center;font-size:8pt;font-style:italic;font-weight:normal;}");
        html.append("		.page{text-align:center;font-size:10pt;}");
        html.append("	</style>");
        html.append("</head>");
     	html.append("<body>");
     	html.append(" 	<hr/>");
 		html.append("	<div class=\"page\"></div>");
 		html.append("		<div class=\"itar\">");
 		html.append(footerContent);
 		html.append("		</div>");
 		html.append("		<script type=\"text/javascript\">");
        html.append("			(function() {");
        html.append("			var vars={};");
        html.append("			var x=document.location.search.substring(1).split('&');");
        html.append("			for(var i in x) {var z=x[i].split('=',2);vars[z[0]] = unescape(z[1]);}");
        html.append("			var x=['frompage','topage','page','webpage','section','subsection','subsubsection'];");
        html.append("			for(var i in x) {");
        html.append("				var y = document.getElementsByClassName(x[i]);");
        html.append("				for(var j=0; j<y.length; ++j) y[j].textContent = vars[x[i]]-3;");
        html.append("			}");
        html.append("		})();");
        html.append("		</script>");
 		html.append("</body></html>");
 		BufferedWriter bw = new BufferedWriter(new FileWriter(footerFile));
        bw.write(html.toString());
        bw.close();
    }
	
	protected void createHeaderPage(String headerFilename,
			String headerContent, String tagId, String timestamp,
			String docNum, String displayTime) throws IOException {
		log(String.format("Sving %s to filesystem...", headerFilename));
		Path headerPath = Paths.get(this.fsWorkingDir, headerFilename);

    	File headerFile = new File(headerPath.toString());

		StringBuffer html = new StringBuffer();
		if(Utils.isNullOrEmpty(displayTime)){
			Date d = new Date();
			if(timestamp.compareToIgnoreCase("latest") != 0) d = TimeUtils.dateFromTimestamp(timestamp);
			displayTime = getFormattedDate(d);
		}
			
		html.append("<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		html.append("    <head>");
		html.append("        <title></title>");
		html.append("        <style type=\"text/css\">");
		html.append("            BODY{");
		html.append("				font-size:8pt;font-family: \"Times New Roman\";");
		html.append("				margin: 0 auto;}");
		html.append("            HR{ width: 100%;}");
		html.append("            .container{ }");
		html.append("            .child{ margin: 5px 0 5px 0;overflow:hidden;}");
		html.append("            .left{ float:left; width:74%;}");
		html.append("            .right{float:right; width:25%;text-align:right;}");
		html.append("			</style>");
		html.append("    </head>");
		html.append("    <body>");
		html.append("        <div class=\"container\">");
		html.append("            <div class=\"child left\">");
		html.append(headerContent);
//			html.append("                <div class=\"docNum\">");
//			if(!Utils.isNullOrEmpty(docNum)) html.append(docNum);
//			html.append("				</div>");
//			html.append("                <div class=\"version\">");
//			html.append(tagId);
//			html.append("				</div>");
		html.append("            </div>");
		html.append("            <div class=\"child right\">");
		html.append("                <div class=\"date\">");
//			html.append(getFormattedDate(d));
		html.append(displayTime);
		html.append("				</div>");
		html.append("            </div>");
		html.append("            <hr/>");
		html.append("        </div>");
		html.append("    </body>");
		html.append("</html>");

		BufferedWriter bw = new BufferedWriter(new FileWriter(headerFile));
        bw.write(html.toString());
        bw.close();
	}
	
	protected String getFormattedDate(Date date){
        Calendar cal=Calendar.getInstance();
        cal.setTime(date);
        int day=cal.get(Calendar.DATE);

        switch (day % 10) {
	        case 1:  
	            return new SimpleDateFormat("MMMM d'st', yyyy").format(date);
	        case 2:  
	            return new SimpleDateFormat("MMMM d'nd,' yyyy").format(date);
	        case 3:  
	            return new SimpleDateFormat("MMMM d'rd,' yyyy").format(date);
	        default: 
	            return new SimpleDateFormat("MMMM d'th,' yyyy").format(date);
        }
    }
        
/*
	public void createCoverPage(String docId, String tagId, String timeStamp,
			String htmlPath, String coverDestination) throws IOException {
		if (!Files.exists(Paths.get(htmlPath)))
			return;

		File htmlFile = new File(htmlPath);
		File coverFile = new File(coverDestination);
		Document document = Jsoup.parse(htmlFile, "UTF-8", "");
		if (document == null)
			return;
		ArrayList<String> l = new ArrayList<String>();

		Elements headers = document.getElementsByTag("mms-transclude-name");
		for (Element header : headers) {
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
		// Date date = this.getTime();
		// String tag = this.getTimeTagName();
		String tagTime = timeStamp;
		if (timeStamp.equalsIgnoreCase("latest"))
			tagTime = new Date().toString();
		String tag = tagId;

		String coverHtml = // "<!DOCTYPE html>" //seems to not print formatted
							// if keep the DOCTYPE
		// +
		"<html>" + "<head><title>"
				+ coverHeader
				+ "</title></head>"
				+ "<body style= \"width:100%; height:100%;\">"
				+ "<div style=\"top:10%; left:10%; right: 10%; position:absolute;\">"
				+ "<center><h2>"
				+ coverHeader
				+ "</h2></center>"
				+ "</div>"
				+ "<div style=\"top:60%; left:10%; right:10%; position:absolute;\">"
				+ "<div>"
				+ legalNotice
				+ "<br/>"
				+ "<i>"
				+ pageLegalNotice
				+ "</i></div>"
				+ "</div>"
				+ "<div style=\"top:70%; left:10%; position:absolute;\">"
				+ "<div>"
				+ tagTime
				+ "<br/>"
				+ tag
				+ "</div>" // "<div>" + date + "</div>"
				+ "</div>"
				+ "<div style=\"top:85%; left:10%; position:absolute;\">"
				+ "<div>"
				+ "<img src=\"http://div27.jpl.nasa.gov/2740/files/logos/jpl_logo%28220x67%29.jpg\" alt=\"JPL Logo\"/>"
				+ "<p style=\"color:#B6B6B4\">" + jplName + "<br/><i>"
				+ caltechName + "</i></p>" // did separate jpl/caltech label to
											// always have the stamp on pdf
				+ "</div>" + "</div>" + "</body>" + "</html>";
		BufferedWriter bw = new BufferedWriter(new FileWriter(coverFile));
		bw.write(coverHtml);
		bw.close();
	}
*/
	
	public void handleEmbeddedImage(String htmlFilename) throws Exception {
		log("Saving images to filesystem...");
		Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);
		if (!Files.exists(htmlPath))
			return;

		File htmlFile = new File(htmlPath.toString());
		Document document = Jsoup.parse(htmlFile, "UTF-8", "");
		if (document == null)
			return;

		Elements images = document.getElementsByTag("img");

		for (final Element image : images) {
			String src = image.attr("src");
			if (src == null)
				continue;
			try {
				URL url = null;
				if (!src.toLowerCase().startsWith("http")) {
					// relative URL; needs to prepend URL protocol
					String protocol = new HostnameGet(this.repository,
							this.services).getAlfrescoProtocol();
					// System.out.println(protocol + "://" + src);
					src = src.replaceAll("\\.\\./", "");
					// System.out.println("src: " + src);
					url = new URL(String.format("%s://%s", protocol, src));
				} else {
					url = new URL(src);
				}

				String hostname = getHostname();
				try {
					src = src.toLowerCase();
					String embedHostname = String.format("%s://%s",
							url.getProtocol(), url.getHost());
					String alfrescoContext = "workspace/SpacesStore/"; // this.services.getSysAdminParams().getAlfrescoContext();
					String versionStore = "versionStore/version2Store/";

					// is image local or remote resource?
					if (embedHostname.compareToIgnoreCase(hostname) == 0
							|| src.startsWith("/alfresco/")
							|| src.contains(alfrescoContext.toLowerCase())
							|| src.contains(versionStore.toLowerCase())) {
						// local server image > generate image tags
						String filePath = url.getFile();
						if (filePath == null || filePath.isEmpty())
							continue;

						nodeId = null;
						storeName = null;
						if (filePath.contains(alfrescoContext)) {
							// filePath = "alfresco/d/d/" +
							// filePath.substring(filePath.indexOf(alfrescoContext));
							nodeId = filePath.substring(filePath
									.indexOf(alfrescoContext)
									+ alfrescoContext.length());
							nodeId = nodeId.substring(0, nodeId.indexOf("/"));
							storeName = "workspace://SpacesStore/";
						}
						if (filePath.contains(versionStore)) {
							// filePath = "alfresco/d/d/" +
							// filePath.substring(filePath.indexOf(alfrescoContext));
							nodeId = filePath.substring(filePath
									.indexOf(versionStore)
									+ versionStore.length());
							nodeId = nodeId.substring(0, nodeId.indexOf("/"));
							storeName = "versionStore://version2Store/";
						}
						if (nodeId == null || nodeId.isEmpty())
							continue;

						filename = filePath
								.substring(filePath.lastIndexOf("/") + 1);
						try {
							// This is the trouble area, where each image needs
							// its own transaction:
							new EmsTransaction(services, response, new Status()) {
								@Override
								public void run() throws Exception {
									DBImage dbImage = retrieveEmbeddedImage(
											storeName, nodeId, filename, null,
											null);
									if (dbImage != null) {
										image.attr("src", dbImage.getFilePath());
									}
								}
							};
						} catch (Exception ex) {
							// in case it's not a local resource > generate
							// hyperlink instead
							// image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ",
							// src, url.getFile()));
							// image.remove();
						}
					} else { // remote resource > generate a hyperlink
					// image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ",
					// src, url.getFile()));
					// image.remove();
					}
				} catch (Exception ex) {
					log(Level.WARN,
							"Failed to retrieve embedded image at %s. %s", src,
							ex.getMessage());
					ex.printStackTrace();
				}
			}

			catch (Exception ex) {
				log(Level.WARN, "Failed to process embedded image at %s. %s",
						src, ex.getMessage());
				ex.printStackTrace();
			}
		}
		try {
			FileUtils
					.writeStringToFile(htmlFile, document.outerHtml(), "UTF-8");
		} catch (Exception ex) {
			log(Level.ERROR, "Failed to save modified HTML %s. %s", htmlPath,
					ex.getMessage());
			ex.printStackTrace();
		}
	}

/*
	protected void handleRelativeHyperlinks(String user, String htmlFilename)
			throws Exception {
		Path htmlPath = Paths.get(dirRoot, user, guid.toString(), htmlFilename);
		if (!Files.exists(htmlPath))
			return;

		File htmlFile = new File(htmlPath.toString());

		Document document;
		try {
			document = Jsoup.parse(htmlFile, "UTF-8", "");
		} catch (Exception ex) {
			log(Level.ERROR,
					"Failed to load HTML file '%s' to handle relative hyperlinks. %s",
					htmlPath, ex.getMessage());
			throw ex;
		}

		if (document == null)
			return;

		Elements links = document.getElementsByTag("a");
		for (Element elem : links) {
			String href = elem.attr("href").toLowerCase();
			if (!href.startsWith("http")) {
				HostnameGet hng = new HostnameGet(this.repository,
						this.services);
				String hostname = hng.getAlfrescoUrl();
				String alfrescoUrl = hostname + "/alfresco";

				if (href.startsWith("service"))
					href = href.replace("service", alfrescoUrl + "/service");
				else if (href.startsWith("ve.html#")
						|| href.startsWith("mms.html#")
						|| href.startsWith("docweb.html#")) {
					href = String.format("%s/%s", alfrescoUrl, href);
				} else if (href.startsWith("share"))
					href = href.replace("share", hostname + "/share");
				elem.attr("href", href);
			}
		}

		try {
			FileUtils
					.writeStringToFile(htmlFile, document.outerHtml(), "UTF-8");
		} catch (Exception ex) {
			log(Level.ERROR,
					"Failed to save HTML file '%s' after handling relative hyperlinks. %s",
					htmlPath, ex.getMessage());
			throw ex;
		}
	}
*/
	
	private DBImage retrieveEmbeddedImage(String storeName, String nodeId,
			String imgName, WorkspaceNode workspace, Object timestamp) throws UnsupportedEncodingException {
		Path imageDirName = Paths.get(this.fsWorkingDir, "images");
		NodeRef imgNodeRef = NodeUtil.getNodeRefFromNodeId(storeName, nodeId);
		if (imgNodeRef == null)
			return null;

		imgName = URLDecoder.decode(imgName, "UTF-8");
		String imgFilename = imageDirName + File.separator + imgName;

		File imgFile = new File(imgFilename);
		ContentReader imgReader;
		imgReader = this.services.getContentService().getReader(imgNodeRef,
				ContentModel.PROP_CONTENT);
		if (!Files.exists(imageDirName)) {
			if (!new File(imageDirName.toString()).mkdirs()) {
				System.out.println("Failed to create directory for "
						+ imageDirName);
			}
		}
		imgReader.getContent(imgFile);

		DBImage image = new DBImage();
		image.setId(nodeId);
		image.setFilePath("images/" + imgName);
		return image;
	}

	private String getHostname() {
		SysAdminParams sysAdminParams = this.services.getSysAdminParams();
		String hostname = sysAdminParams.getAlfrescoHost();
		if (hostname.startsWith("ip-128-149"))
			hostname = "localhost";
		return String.format("%s://%s", sysAdminParams.getAlfrescoProtocol(),
				hostname);
	}
	
	protected String zipWorkingDir(String zipFilename) throws IOException, InterruptedException {
		log("Zipping artifacts within working directory...");
		RuntimeExec exec = new RuntimeExec();
		exec.setProcessDirectory(this.fsWorkingDir);
		List<String> command = new ArrayList<String>();
		command.add("zip");
		command.add("-r");
		command.add(zipFilename);
		command.add(".");
		exec.setCommand(list2Array(command));
//		System.out.println("zip command: " + command);
		ExecutionResult result = exec.execute();

		if (!result.getSuccess()) {
			System.out.println("zip failed!");
			System.out.println("exit code: " + result.getExitValue());
		}

		return Paths.get(this.fsWorkingDir, zipFilename).toString();
	}
	
	public void cleanupFiles(){
    	if(gov.nasa.jpl.mbee.util.FileUtils.exists(this.fsWorkingDir)){
    		try{
    			FileUtils.forceDelete(new File(this.fsWorkingDir));
    		}
    		catch(IOException ex){
				System.out.println(String.format("Failed to cleanup temporary files at %s", this.fsWorkingDir));
    		}
    	}
    }
	
    protected String getTableVerticalSplitScript(){
    	StringBuilder script = new StringBuilder();
    	script.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js\"></script>");
    	script.append("<script type=\"text/javascript\">\n");
//    	script.append("/**\n");
//    	script.append("* WkHtmlToPdf table vertically-splitting hack\n");
//    	script.append("* Script to automatically split wide HTML tables that doesn't fit the width of the PDF page generated\n");
//		script.append("* by WkHtmlToPdf (or equivalent)\n");
//		script.append("*\n");
//		script.append("* The general idea come from Florin Stancu <niflostancu@gmail.com> and his script wkhtmltopdf.tablesplit.js\n");
//		script.append("* The implementation is quite different because the splitting is done vertically on a excessive\n");
//		script.append("* wide table, while the original script was meant to split horizontally an excessive long table\n");
//		script.append("*\n");
//		script.append("* To use, you must adjust pdfPage object's contents to reflect your PDF's\n");
//		script.append("* page format.\n");
//		script.append("* The tables you want to be automatically splitted when the page ends must\n");
//		script.append("* have the same class name as specified by the variable \"verticalTableSplit_ClassName\": if not set,\n");
//		script.append("* all tables will be checked for split.\n");
//		script.append("* Also, is possible to have a left vertical header repeating in all table slices: columns of this\n");
//		script.append("* vertical header must have td elements with the same class name as specified by the variable\n");
//		script.append("* \"verticalTableSplit_leftHeaderClassName\"\n");
//		script.append("*\n");
//		script.append("* Live demo: http://jsfiddle.net/mU2Ne/\n");
//		script.append("* GitHub: https://github.com/vstefanoxx/JSUtils/tree/master/TableVerticalSplitHack\n");
//		script.append("* Gist: https://gist.github.com/vstefanoxx/574aa61eaf2cc91dd9c9\n");
//		script.append("*\n");
//		script.append("* Dependencies: jQuery.\n");
//		script.append("*\n");
//		script.append("* From original script (and some others forks and hacks) I took some variable name and, as I said,\n");
//		script.append("* the original idea\n");
//		script.append("*\n");
//		script.append("* @author Stefano Vargiu <vstefanoxx@gmail.com>\n");
//		script.append("* @license http://www.opensource.org/licenses/mit-license.php MIT License\n");
//		script.append("*/\n");
//
//		script.append("/**\n");
//		script.append(" * pdfPage, verticalTableSplit_ClassName, verticalTableSplit_leftHeaderClassName\n");
//		script.append(" * You can overwrite these parameters in the page from where you are loading this script\n\n");
//		script.append(" * (and after the point where you loaded it) so you can set pdfPage in one place and use,\n\n");
//		script.append(" * if you need it, both this script and the one that split horizontally the table\n\n");
//		script.append(" * (wkhtmltopdf.tablesplit.js on GitHub or his forks, see the disclaimer on top)\n\n");
//		script.append(" */\n\n");
		script.append("var pdfPage = {\n\n");
		script.append("	width: 11.7,\n\n");
		script.append("	height: 8.3,\n\n");
		script.append("	margins: {\n\n");
		script.append("		top: 2/25.4,\n\n");
		script.append("		left: 2/25.4,\n\n");
		script.append("		right: 2/25.4,\n\n");
		script.append("		bottom: 26/25.4\n\n");
		script.append("	}\n\n");
		script.append("};\n\n");
		script.append("// class name of the tables to automatically split: if not specified, split all tables\n\n");
		script.append("//var verticalTableSplit_ClassName = 'splitForPrint';\n\n");
		script.append("var verticalTableSplit_ClassName = '';\n");
		script.append("// class name to specify which columns are part of the vertical left header\n");
		script.append("var verticalTableSplit_leftHeaderClassName = 'leftHeader';\n");

		script.append("$(window).load(function () {\n");
		script.append("	// add_columns\n");
		script.append("	// Copy columns from the rows $rows to $target_table in the range of indices \"from_idx\" and \"to_idx\"\n");
		script.append("	function add_columns($rows, $target_table, from_idx, to_idx) {\n");
		script.append("		$rows.each(function() {\n");
		script.append("			var $tr = $(this);\n");
		script.append("			$target_table.find('> tbody > tr:eq('+$tr.index()+')')\n");
		script.append("				.html(\n");
		script.append("					$('<div>')\n");
		script.append("						.append(\n");
		script.append("							$tr.find('> td.' + verticalTableSplit_leftHeaderClassName).clone()\n");
		script.append("						)\n");
		script.append("						.append(\n");
		script.append("							$tr.find('td').slice(from_idx, to_idx+1).clone()\n");
		script.append("						)\n");
		script.append("						.html()\n");
		script.append("				);\n");
		script.append("		});\n");
		script.append("	}\n");
    		
		script.append("	// getHeaderRange\n");
		script.append("	// Calculate header columns range based on data columns indeces \"from_idx\" and \"to_idx\", taking into account that headers columns can have colspan\n");
		script.append("	// attribute (while this function don't manage properly data columns with colspan attributes)\n");
		script.append("	function getHeaderRange($row, from_idx, to_idx) {\n");
		script.append("		var $header, $new_header_row, cols_counter, start_idx, end_idx, start_diff_colspan, end_diff_colspan, colspan, diff_col_idx, start_colspan, end_colspan;\n");
		script.append("		cols_counter = 0;\n");
		script.append("		start_idx = undefined;\n");
		script.append("		end_idx = undefined;\n");
		script.append("		start_diff_colspan = undefined;\n");
		script.append("		end_diff_colspan = undefined;\n");
		script.append("		// for every header, find starting and ending header columns indices\n");
		script.append("		$row.find('> th, > td').each(function() {\n");
		script.append("			$header = $(this);\n");
		script.append("			colspan = +($header.attr('colspan') || 1);\n");
		script.append("			if (start_idx == undefined) {\n");
		script.append("				diff_col_idx = from_idx - cols_counter;\n");
		script.append("				if (diff_col_idx >= 0 && diff_col_idx < colspan) {\n");
		script.append("					start_idx = $header.index();\n");
		script.append("					start_colspan = colspan;\n");
		script.append("					if (diff_col_idx > 0) start_diff_colspan = diff_col_idx;\n");
		script.append("				}\n");
		script.append("			}\n");
		script.append("			if (end_idx == undefined) {\n");
		script.append("				diff_col_idx = to_idx - cols_counter;\n");
		script.append("				if (diff_col_idx >= 0 && diff_col_idx < colspan) {\n");
		script.append("					end_idx = $header.index();\n");
		script.append("					end_colspan = colspan;\n");
		script.append("					if (diff_col_idx != colspan - 1) end_diff_colspan = colspan - diff_col_idx - 1;\n");
		script.append("				}\n");
		script.append("			}\n");
		script.append("			if (start_idx != undefined && end_idx != undefined)\n");
		script.append("				return false;\n");
		script.append("			cols_counter += colspan;\n");
		script.append("		});\n");
		script.append("		var is_same_idx = (start_idx == end_idx);\n");
		script.append("		// return info abount the range of header columns\n");
		script.append("		var obj = {\n");
		script.append("			is_same_idx: is_same_idx,\n");
		script.append("			start_idx: start_idx,\n");
		script.append("			end_idx: (!is_same_idx ? end_idx : undefined),\n");
		script.append("			start_colspan: start_colspan,\n");
		script.append("			end_colspan: end_colspan,\n");
		script.append("			start_diff_colspan: (start_diff_colspan || 0) + (!is_same_idx ? 0 : (end_diff_colspan || 0)),\n");
		script.append("			end_diff_colspan: is_same_idx ? undefined : end_diff_colspan\n");
		script.append("		};\n");
		script.append("		return obj;\n");
		script.append("	}\n");
    		
		script.append("	// getHeaderSliceHTML\n");
		script.append("	// Create and return the headers slices HTML as specified by the ranges \"first_range\" (relative to the header on top of the vertical left hader)\n");
		script.append("	// and \"second_range\" (relative to the header on top of data columns).\n");
		script.append("	// If header slices are adjacent, it join them \n");
		script.append("	function getHeaderSliceHTML($row, first_range, second_range) {\n");
		script.append("		var ranges = [];\n");
		script.append("		if (first_range != undefined)\n");
		script.append("			var last_idx_first_range = (first_range.is_same_idx ? first_range.start_idx : first_range.end_idx);\n");
		script.append("		// if ranges are adjacent, join them\n");
		script.append("		if (last_idx_first_range == second_range.start_idx) {\n");
		script.append("			// modify first range to include second range, and add only that single range\n");
		script.append("			if (second_range.is_same_idx) {\n");
		script.append("				if (!first_range.is_same_idx)\n");
		script.append("					first_range.end_diff_colspan += second_range.start_diff_colspan - first_range.colspan;\n");
		script.append("			} else {\n");
		script.append("				first_range.end_idx = second_range.end_idx;\n");
		script.append("				first_range.end_colspan = second_range.end_colspan;\n");
		script.append("				if (!first_range.is_same_idx)\n");
		script.append("					first_range.end_diff_colspan = second_range.end_diff_colspan;\n");
		script.append("			}\n");
		script.append("			if (first_range.is_same_idx)\n");
		script.append("				first_range.start_diff_colspan += second_range.start_diff_colspan - first_range.colspan;\n");
		script.append("			ranges.push(first_range);\n");
		script.append("		// ranges are NOT adjacent, add both of them\n");
		script.append("		} else if (first_range == undefined) {\n");
		script.append("			ranges.push(second_range);\n");
		script.append("		} else {\n");
		script.append("			ranges.push(first_range);\n");
		script.append("			ranges.push(second_range);\n");
		script.append("		}\n");
		script.append("		// create DOM elements from ranges\n");
		script.append("		var $ret_slices = $('<div>');\n");
		script.append("		var $cur_slice;\n");
		script.append("		$.each(ranges, function(idx, range) {\n");
		script.append("			var $cur_slice = $row.find('> th, > td').slice(range.start_idx, (range.is_same_idx ? range.start_idx : range.end_idx)+1).clone();\n");
		script.append("			if (range.start_diff_colspan > 0)\n");
		script.append("				$cur_slice.first().attr('colspan', range.start_colspan - range.start_diff_colspan);\n");
		script.append("			if (range.end_diff_colspan > 0)\n");
		script.append("				$cur_slice.last().attr('colspan', range.end_colspan - range.end_diff_colspan);\n");
		script.append("			$ret_slices.append($cur_slice);\n");
		script.append("		});\n");
		script.append("		// return html code\n");
		script.append("		return $ret_slices.html();\n");
		script.append("	}\n");
    		
		script.append("	// setHeader\n");
		script.append("	// set the header and footer of $target_table according to vertical left header and data columns specified (through column indeces)\n");
		script.append("	function setHeader($header_rows, $footer_rows, $target_table, from_idx, to_idx, leftHeader_last_idx) {\n");
		script.append("		var $row, $header_slice, data_header_range, row_type;\n");
		script.append("		var leftHeader_header_range = undefined;\n");
		script.append("		$.each([ $header_rows, $footer_rows ], function(idx, $rows) {\n");
		script.append("			$rows.each(function() {\n");
		script.append("				$row = $(this);\n");
		script.append("				if (leftHeader_last_idx != undefined)\n");
		script.append("					leftHeader_header_range = getHeaderRange($row, 0, leftHeader_last_idx);\n");
		script.append("				data_header_range = getHeaderRange($row, from_idx, to_idx);\n");
		script.append("				row_type = (idx == 0 ? 'thead' : 'tfoot');\n");
		script.append("				$target_table.find('> ' + row_type + ' > tr:eq('+$row.index()+')')\n");
		script.append("					.html(\n");
		script.append("						getHeaderSliceHTML($row, leftHeader_header_range, data_header_range)\n");
		script.append("					);\n");
		script.append("			});\n");
		script.append("		});\n");
		script.append("	}\n");
    		
		script.append("	// get document resolution\n");
		script.append("	var dpi = $('<div id=\"dpi\"></div>')\n");
		script.append("		.css({\n");
		script.append("			height: '1in', width: '1in',\n");
		script.append("			top: '-100%', left: '-100%',\n");
		script.append("			position: 'absolute'\n");
		script.append("		})\n");
		script.append("		.appendTo('body')\n");
		script.append("		.width();\n");
    			
		script.append("	// separator div\n");
		script.append("	var $separator_div = $('<div class=\"page-breaker\" style=\"height: 10px;\"></div>');\n");
    		
		script.append("	// calculate page width\n");
		script.append("	var pageWidth = Math.ceil((pdfPage.width - pdfPage.margins.left - pdfPage.margins.right) * dpi);\n");
    		
		script.append("	// temporary set body's width and padding to match pdf's size\n");
		script.append("	var $body = $('body');\n");
		script.append("	$body.css('width', (pdfPage.width - pdfPage.margins.left - pdfPage.margins.right) + 'in');\n");
		script.append("	$body.css('padding-left', pdfPage.margins.left + 'in');\n");
		script.append("	$body.css('padding-right', pdfPage.margins.right + 'in');\n");
    		
		script.append("	//\n");
		script.append("	// cycle through all tables and split them if necessary\n");
		script.append("	//\n");
		script.append("	$('table' + (verticalTableSplit_ClassName == '' ? '' : '.' + verticalTableSplit_ClassName)).each(function () {\n");
		script.append("		var $collectableDiv = $('<div>');\n");
		script.append("		var $origin_table = $(this);\n");
		script.append("		var $rows = $origin_table.find('> tbody > tr');\n");
		script.append("		var $first_row = $rows.first();\n");
		script.append("		var $first_row_cols = $first_row.find('> td');\n");
		script.append("		var num_cols = $first_row_cols.size();\n");
		script.append("		var $header_rows = $origin_table.find('> thead > tr');\n");
		script.append("		var $footer_rows = $origin_table.find('> tfoot > tr');\n");
		script.append("		var x_offset = 0;\n");
    			
		script.append("		// create the template for new table slices\n");
		script.append("		var $template = $origin_table.clone();\n");
		script.append("		$template.find('> tbody > tr > td').remove();\n");
		script.append("		$template.find('> thead > tr > th').remove();\n");
		script.append("		$template.find('> tfoot > tr > td').remove();\n");
    			
		script.append("		// create first table slice\n");
		script.append("		var $current_table = $template.clone();\n");
    			
		script.append("		// info abount vertical left header (if present)\n");
		script.append("		var $leftHeader_last_col = $first_row.find('> td.' + verticalTableSplit_leftHeaderClassName + ':last')\n");
		script.append("		// is left vertical header present?\n");
		script.append("		if ($leftHeader_last_col.size() > 0) {\n");
		script.append("			var leftHeader_last_idx = $leftHeader_last_col.index();\n");
		script.append("			var leftHeader_right_x = $leftHeader_last_col.offset().left + $leftHeader_last_col.outerWidth();\n");
		script.append("			var last_idx = leftHeader_last_idx + 1;\n");
		script.append("		// left vertical header is not present\n");
		script.append("		} else {\n");
		script.append("			var leftHeader_last_idx = undefined;\n");
		script.append("			var leftHeader_right_x = 0;\n");
		script.append("			var last_idx = 0;\n");
		script.append("		}\n");
		script.append("			// for every column, check if it fits inside the page width\n");
		script.append("		$first_row_cols.slice(last_idx).each(function() {\n");
		script.append("			var $td = $(this);\n");
		script.append("			// check if column is beyond page right margin\n");
		script.append("			var td_left = $td.offset().left;\n");
		script.append("			var is_overflow = (td_left + $td.outerWidth() - x_offset >= pageWidth);\n");
		script.append("			// if there is no space for the new column, add header and footer to current table slice and create new table slice\n");
		script.append("			if (is_overflow) {\n");
		script.append("				var td_idx = $td.index();\n");
		script.append("				// add header and footer to current table\n");
		script.append("				setHeader($header_rows, $footer_rows, $current_table, last_idx, td_idx-1, leftHeader_last_idx);\n");
    					
		script.append("				// add to the current table all columns from the one next to previous slice to the one before the current column\n");
		script.append("				add_columns($rows, $current_table, last_idx, td_idx-1);\n");
		script.append("				last_idx = td_idx;\n");
    				
		script.append("				// add current table to the array of slices\n");
		script.append("				$collectableDiv.append($current_table);\n");
		script.append("				$collectableDiv.append($separator_div.clone());\n");
		script.append("				x_offset += td_left - leftHeader_right_x;\n");
    					
		script.append("				// create new table slice\n");
		script.append("				$current_table = $template.clone();\n");
		script.append("			}\n");
		script.append("		// END each column\n");
		script.append("		});\n");
    			
		script.append("		// add header, footer and remaining columns to last table slice\n");
		script.append("		setHeader($header_rows, $footer_rows, $current_table, last_idx, num_cols-1, leftHeader_last_idx);\n");
		script.append("		add_columns($rows, $current_table, last_idx, num_cols-1);\n");
		script.append("		$collectableDiv.append($current_table.clone());\n");
    			
		script.append("		// replace original table with new table slices\n");
		script.append("		$origin_table.replaceWith($collectableDiv.html());\n");
		script.append("	// END each table\n");
		script.append("	});\n");
		script.append("});\n");
		script.append("</script>\n");
    	return script.toString();
    }
}
