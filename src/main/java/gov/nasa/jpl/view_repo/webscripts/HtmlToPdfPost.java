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

	// filesystem working directory
	protected String fsWorkingDir; // format: dirRoot + "[USER_ID]/[GUID]

	// dir containing full doc generation resources (prerenderer.io, phantomJS,
	// wkhtmltopdf)
	// protected String fullDocGenDir = "/opt/local/fullDocGen/";
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
			String htmlContent, String coverContent, String toc, String tof,
			String tot, String indices, String headerContent,
			String footerContent, String docNum, String displayTime,
			String customCss, Boolean isSameWidthTableCell) {
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
		String zipFilename = String.format("%s_%s.zip", docId, timeStamp)
				.replace(":", "");
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
					coverFilename, coverContent, toc, tof, tot, indices,
					footerFilename, footerContent, headerFilename, headerContent,
					tagId, timeStamp, docNum, displayTime,
					customCss, isSameWidthTableCell);

			handleEmbeddedImage(coverFilename);
			handleEmbeddedImage(htmlFilename);
			// saveCoverToRepo(coverFilename, coverContent);
			saveHtmlToRepo(htmlFilename, htmlContent);

			String pdfPath = html2pdf(docId, tagId, timeStamp, htmlPath,
					pdfFilename, coverFilename, userHomeFolder, headerFilename,
					footerFilename, customCss);

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
			if (!Files.exists(Paths.get(this.fsWorkingDir))) {
				log(String.format("Unable to create directory %s...%s",
						this.fsWorkingDir, ex.getMessage()));
				throw new Throwable();
			}
		}
	}

	protected void saveCoverToRepo(String coverFilename, String coverContent) {
		log(String.format("Saving %s to repository...", coverFilename));
		try {
			EmsScriptNode htmlNode = createScriptNode(coverFilename);
			ActionUtil.saveStringToFile(htmlNode, "text/html", services,
					coverContent);
		} catch (Throwable ex) {
			// allowing process to continue;
			log("Failed to save %s to repository!");
			log(ex.getMessage());
		}
	}

	protected EmsScriptNode saveHtmlToRepo(String htmlFilename,
			String htmlContent) throws Throwable {
		log(String.format("Saving %s to repository...", htmlFilename));
		Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);

		if (!Files.exists(htmlPath)) {
			try {
				return saveStringToRepo(htmlFilename, htmlContent, "text/html");
			} catch (Throwable ex) {
				response.append("Failed to save HTML content to repository!");
				throw new Throwable();
			}
		} else {
			try {
				return saveFileToRepo(htmlFilename, "text/html",
						htmlPath.toString());
			} catch (Throwable ex) {
				response.append(ex.getMessage());
				response.append(String.format(
						"Failed to save %s to repository!", htmlFilename));
				throw new Throwable();
			}
		}
	}

	protected EmsScriptNode createScriptNode(String filename) {
		EmsScriptNode node = NodeUtil.getOrCreateContentNode(
				this.nodeUserHomeSubDir, filename, services);
		if (node == null)
			response.append(String.format("Failed to create nodeRef for %s",
					filename));
		return node;
	}

	protected EmsScriptNode saveStringToRepo(String filename, String content,
			String mimeType) throws Throwable {
		EmsScriptNode node = createScriptNode(filename);
		if (node == null)
			throw new Throwable();
		ActionUtil.saveStringToFile(node, mimeType, services, content);
		return node;
	}

	protected EmsScriptNode savePdfToRepo(String pdfPath) {
		log(String.format("Saving %s to repository...", Paths.get(pdfPath)
				.getFileName()));
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
		log(String.format("Saving %s to repository...", zipFilename));

		try {
			return saveFileToRepo(zipFilename, MimetypeMap.MIMETYPE_ZIP,
					zipPath);
		} catch (Throwable ex) {
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
		if (node == null) {
			response.append("Failed to create nodeRef!");
			throw new Throwable();
		}
		if (!saveFileToRepo(node, mimeType, filePath)) {
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
			FileUtils.copyDirectory(new File(cssPath.toString()), new File(
					Paths.get(this.fsWorkingDir, "css").toString()));
		} catch (IOException e) {
			// not having CSS is not critical; allow process to continue w/o CSS
			log("Failed to copy CSS files to working directory!");
			log(e.getMessage());
			e.printStackTrace();
		}
	}

	protected String addCssLinks(String htmlContent, String headerContent,
			String footerContent, String tagId, String timeStamp,
			String displayTime) throws Throwable {
		log("Adding CSS links to HTML...");
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		Element head = document.head();
		head.append("<meta charset=\"utf-8\" />");
		// head.append("<link href=\"https://fonts.googleapis.com/css?family=Droid+Serif:400,700,400italic,700italic\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<link href=\"css/mm-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append("<link href=\"css/ve-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		StringBuffer style = new StringBuffer();
		style.append("<style type=\"text/css\">");
		style.append(" 	li > a[href]::after {content: leader(\".\") target-counter(attr(href), page);}");
		style.append("  img {max-width: 100%;}");
		style.append("  .pull-right {float: right;}");
		style.append("  table {width: 100%; border-collapse: collapse;}");
		style.append("  table, th, td {border: 1px solid black;}");
		style.append("  H1 {font-size: 20px;}");
		style.append("  .ng-hide {display: none;}");
		style.append("  BODY {font-size: 10px;}");
		style.append("  .tof, .tot {page-break-after:always}");
		style.append("  .tof .header, .tot .header { font-size:32px; }");
		style.append("  .tof UL, .tot UL {list-style-type:none;}");
		style.append("  .indices { page-break-before: always;}");
		style.append("  @page {");
		style.append("     @bottom {");
		style.append("        font-size: 9px;");
		style.append("        content: \"");
		style.append(footerContent);
		style.append("    \"}");
		style.append("     @top {");
		style.append("        font-size: 9px;");
		style.append("        content: \"");
		style.append(headerContent);
		style.append("    \"}");
		style.append("    @top-right {");
		style.append("        font-size: 9px;");
		style.append("        content: \"");
		style.append(buildHeaderContentRHS(tagId, timeStamp, displayTime));
		style.append("    \"}");
		style.append("    @bottom-right {");
		style.append("        font-size: 9px;");
		style.append("        content: counter(page)");
		style.append("    }");
		style.append("}");
		style.append("</style>");
		head.append(style.toString());
		// adding custom CSS link
		head.append("<link href=\"css/customStyles.css\" rel=\"stylesheet\" type=\"text/css\" />");

		return document.outerHtml();
	}

	/**
	 * @param htmlFilename
	 * @param htmlContent
	 * @param coverFilename
	 * @param coverContent
	 * @param toc
	 *            TODO
	 * @param tof
	 *            TODO
	 * @param tot
	 *            TODO
	 * @param indices TODO
	 * @param headerFilename
	 *            TODO
	 * @param headerContent
	 *            TODO
	 * @param tagId
	 *            TODO
	 * @param displayTime
	 *            TODO
	 * @param isSameWidthTableCell
	 *            TODO
	 * @param timestamp
	 *            TODO
	 * @param jplDocNum
	 *            TODO
	 * @return path to saved HTML file.
	 * @throws Throwable
	 */
	protected String saveHtmlToFilesystem(String htmlFilename,
			String htmlContent, String coverFilename, String coverContent,
			String toc, String tof, String tot, String indices,
			String footerFilename, String footerContent, String headerFilename,
			String headerContent, String tagId, String timeStamp, String docNum,
			String displayTime, String customCss, Boolean isSameWidthTableCell) throws Throwable {
		log(String.format("Saving %s to filesystem...", htmlFilename));
		Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);
		Path coverPath = Paths.get(this.fsWorkingDir, coverFilename);
		Path customCssPath = Paths.get(this.fsWorkingDir, "css",
				"customStyles.css");

		try {
			if (Files.exists(htmlPath)) {
				// TODO file already exists, should we override?
			}
			copyCssFilesToWorkingDir();
			htmlContent = addCssLinks(htmlContent, headerContent,
					footerContent, tagId, timeStamp, displayTime);
			// htmlContent = addJscripts(htmlContent);
			htmlContent = addTot(htmlContent, tot);
			htmlContent = addTof(htmlContent, tof);
			htmlContent = addToc(htmlContent, toc);
			htmlContent = addIndices(htmlContent, indices);

			File htmlFile = new File(htmlPath.toString());
			BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile));
			bw.write(htmlContent);
			bw.close();

			// log(String.format("Saving %s to filesystem...", coverFilename));
			// File coverFile = new File(coverPath.toString());
			// bw = new BufferedWriter(new FileWriter(coverFile));
			// bw.write(coverContent);
			// bw.close();

			if (!Utils.isNullOrEmpty(customCss)) {
				log(String.format("Saving %s to filesystem...",
						customCssPath.getFileName()));
				File customCssFile = new File(customCssPath.toString());
				bw = new BufferedWriter(new FileWriter(customCssFile));
				bw.write(customCss);
				bw.close();
			}

			createCoverPage(coverFilename, coverContent);
			// createFooterPage(footerFilename, footerContent);
			// createHeaderPage(headerFilename, headerContent, tagId, timeStamp,
			// docNum, displayTime);
		} catch (Throwable ex) {
			logger.error(ex.getMessage(), ex);
			log(String.format("Failed to save %s to filesystem!", htmlFilename));
			log(ex.getMessage());
			throw new Throwable();
		}
		return htmlPath.toString();
	}

	protected String addToc(String htmlContent, String toc) throws Throwable {
		if (Utils.isNullOrEmpty(toc))
			return htmlContent;
		log("Adding table of contents to HTML...");
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		document.body().child(0).before(toc);
		return document.outerHtml();
	}

	protected String addTof(String htmlContent, String tof) throws Throwable {
		if (Utils.isNullOrEmpty(tof))
			tof = buildTableOfFigures(htmlContent);
		log("Adding list of figures to HTML...");
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		document.body().child(0).before(tof);
		return document.outerHtml();
	}

	protected String addTot(String htmlContent, String tot) throws Throwable {
		if (Utils.isNullOrEmpty(tot))
			tot = buildTableOfTables(htmlContent);
		log("Adding list of tables to HTML...");
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		document.body().child(0).before(tot);
		return document.outerHtml();
	}

	protected String addIndices(String htmlContent, String indices) throws Throwable{
		if(Utils.isNullOrEmpty(indices)) return htmlContent;
		log("Adding indices to HTML...");
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		Element elem = document.createElement("DIV");
		document.body().appendChild(elem);
		elem.attr("class", "indices");
		elem.append(indices);
		return document.outerHtml();
	}
	
	protected String buildTableOfFigures(String htmlContent) throws Throwable {
		StringBuffer tof = new StringBuffer();
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		Elements figures = document.body().select("FIGURE");
		if (figures.size() > 0) {
			tof.append("<div class='tof'>");
			tof.append("   <div class='header'>List of Figures</div>");
			tof.append("   <ul>");
			int index = 0;
			for (Element f : figures) {
				tof.append("  <li><a href='#");
				tof.append(f.parent().parent().attr("id"));
				tof.append(" '>");
				Elements caption = f.select("> figcaption");
				tof.append("Figure ");
				tof.append(++index);
				tof.append(": ");
				if (caption != null && caption.size() > 0) {
					tof.append(caption.get(0).text());
				} else {
					tof.append("Untitled");
				}
				tof.append("</a></li>");
			}

			tof.append("	</ul>");
			tof.append("</div>");
		}
		return tof.toString();
	}

	protected String buildTableOfTables(String htmlContent) throws Throwable {
		StringBuffer tot = new StringBuffer();
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		Elements tables = document.body().select("TABLE");
		if (tables.size() > 0) {
			tot.append("<div class='tot'>");
			tot.append("   <div class='header'>List of Tables</div>");
			tot.append("   <ul>");
			int index = 0;
			for (Element t : tables) {
				tot.append("  <li><a href='#");
				tot.append(t.parent().parent().attr("id"));
				tot.append(" '>");
				Elements caption = t.select("> caption");
				tot.append("Table ");
				tot.append(++index);
				tot.append(": ");
				if (caption != null && caption.size() > 0) {
					tot.append(caption.get(0).text());
				} else {
					tot.append("Untitled");
				}
				tot.append("</a></li>");
			}

			tot.append("	</ul>");
			tot.append("</div>");
		}
		return tot.toString();
	}

	protected String buildTableOfTablesAndFigures(String htmlContent)
			throws Throwable {
		StringBuffer tot = new StringBuffer();
		Document document = Jsoup.parse(htmlContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML content!");
		}
		Elements tables = document.body().select("TABLE, FIGURE");
		if (tables.size() > 0) {
			tot.append("<div class='tot'>");
			tot.append("   <div class='header'>List of Tables and Figures</div>");
			tot.append("   <ul>");
			int figIndex = 0;
			int tableIndex = 0;
			String name = null;
			String tagName = null;
			for (Element t : tables) {
				tot.append("  <li><a href='#");
				tot.append(t.parent().parent().attr("id"));
				tot.append(" '>");
				Elements caption = null;
				tagName = t.tagName().toUpperCase();
				if (tagName.compareToIgnoreCase("TABLE") == 0) {
					caption = t.select("> caption");
					tot.append("Table ");
					tot.append(++tableIndex);
				} else if (tagName.compareToIgnoreCase("FIGURE") == 0) {
					caption = t.select("> figcaption");
					tot.append("Figure ");
					tot.append(++figIndex);
				}
				
				tot.append(": ");
				if (caption != null && caption.size() > 0) {
					tot.append(caption.get(0).text());
				} else {
					tot.append("Untitled");
				}
				tot.append("</a></li>");
			}

			tot.append("	</ul>");
			tot.append("</div>");
		}
		return tot.toString();
	}

	// protected String buildTocItems(Element elem, int index){
	// StringBuffer sb = new StringBuffer();
	//
	// Elements elems = elem.select("> DIV > DIV > mms-view > DIV > H1");
	// elems.addAll(elem.select("> DIV > DIV > mms-view > DIV > H2"));
	// elems.addAll(elem.select("> DIV > DIV > mms-view > DIV > H3"));
	// elems.addAll(elem.select("> DIV > DIV > mms-view > DIV > H4"));
	// elems.addAll(elem.select("> DIV > DIV > mms-view > DIV > H5"));
	// elems.addAll(elem.select("> DIV > DIV > mms-view > DIV > H6"));
	// String elemIid = null;
	// if(elems.size() > 0){
	// sb.append("<ol>");
	// for(Element e: elems){
	// if(Utils.isNullOrEmpty(e.text())) continue;
	//
	// System.out.println(String.format("<%s> : %s", e.tagName(), e.ownText()));
	// if(e.hasAttr("ID")){
	// elemIid = e.attr("ID");
	// }
	// else{
	// elemIid="#tocItem_" + ++index;
	// e.attr("ID", elemIid);
	// }
	// sb.append("<li>");
	// sb.append("<a href=\"");
	// sb.append(elemIid);
	// sb.append("\">");
	// sb.append(e.text());
	// sb.append("</a>");
	// sb.append("</li>");
	// }
	// sb.append("</ol>");
	// }
	// return sb.toString();
	// }

	// protected String html2pdf(String docId, String tagId, String timeStamp,
	// String htmlPath, String pdfFilename, String coverFilename,
	// EmsScriptNode userHomeFolder, String headerFilename,
	// String footerFilename) throws Throwable {
	// log("Converting HTML to PDF...");
	// if (!Files.exists(Paths.get(htmlPath))) {
	// throw new Throwable(
	// String.format(
	// "Failed to transform HTML to PDF. Expected %s HTML file but it does not exist!",
	// htmlPath));
	// }
	//
	// String pdfPath = Paths.get(this.fsWorkingDir, pdfFilename).toString();
	// String coverPath = Paths.get(this.fsWorkingDir,
	// coverFilename).toString();
	// String footerPath = Paths.get(this.fsWorkingDir,
	// footerFilename).toString();
	// String headerPath = Paths.get(this.fsWorkingDir,
	// headerFilename).toString();
	//
	// List<String> command = new ArrayList<String>();
	// command.add("wkhtmltopdf");
	// command.add("-q");
	// command.add("--load-error-handling");
	// command.add("ignore");
	// command.add("--load-media-error-handling");
	// command.add("ignore");
	// command.add("--header-spacing");
	// command.add("5");
	// command.add("--header-html");
	// command.add(headerPath);
	// command.add("--footer-spacing");
	// command.add("5");
	// command.add("--footer-html");
	// command.add(footerPath);
	//
	// command.add("cover"); // NEED FOR COVER
	// command.add(coverPath); // NEED FOR COVER
	// command.add("toc");
	// command.add("--toc-header-text");
	// command.add("Table of Contents");
	// command.add(htmlPath);
	// command.add(pdfPath);
	//
	// System.out.println("htmltopdf command: "
	// + command.toString().replace(",", ""));
	// log("htmltopdf command: " + command.toString().replace(",", ""));
	//
	// int attempts = 0;
	// int ATTEMPTS_MAX = 3;
	// boolean success = false;
	// RuntimeExec exec = null;
	// ExecutionResult execResult = null;
	// Process process = null;
	//
	// boolean runProcess = true;
	// while (attempts++ < ATTEMPTS_MAX && !success) {
	// if (!runProcess) {
	// exec = new RuntimeExec();
	// // exec.setProcessDirectory(this.fullDocGenDir);
	// exec.setCommand(list2Array(command));
	// execResult = exec.execute();
	// } else {
	// ProcessBuilder pb = new ProcessBuilder(command);
	// // File file = new File(this.fullDocGenDir);
	// // pb.directory(file);
	//
	// process = pb.start();
	// process.waitFor();
	// }
	// if (Files.exists(Paths.get(pdfPath))) {
	// success = true;
	// break;
	// }
	// Thread.sleep(5000);
	// }
	//
	// if (!success && !Files.exists(Paths.get(pdfPath))) {
	// String msg = null;
	// if (!runProcess) {
	// msg = String
	// .format("Failed to transform HTML file '%s' to PDF. Exit value: %d",
	// htmlPath, execResult);
	// } else {
	// msg = String
	// .format("Failed to transform HTML file '%s' to PDF. Exit value: %d",
	// htmlPath, process.exitValue());
	// }
	// log(msg);
	// throw new Throwable(msg);
	// }
	// return pdfPath;
	// }

	protected String html2pdf(String docId, String tagId, String timeStamp,
			String htmlPath, String pdfFilename, String coverFilename,
			EmsScriptNode userHomeFolder, String headerFilename,
			String footerFilename, String customCss) throws Throwable {
		log("Converting HTML to PDF...");
		if (!Files.exists(Paths.get(htmlPath))) {
			throw new Throwable(
					String.format(
							"Failed to transform HTML to PDF. Expected %s HTML file but it does not exist!",
							htmlPath));
		}

		String pdfPath = Paths.get(this.fsWorkingDir, pdfFilename).toString();
		String coverPath = Paths.get(this.fsWorkingDir, coverFilename)
				.toString();
		String footerPath = Paths.get(this.fsWorkingDir, footerFilename)
				.toString();
		String headerPath = Paths.get(this.fsWorkingDir, headerFilename)
				.toString();

		List<String> command = new ArrayList<String>();
		command.add("prince");
		command.add("--media");
		command.add("print");
		if (!Utils.isNullOrEmpty(customCss)) {
			command.add("--style");
			command.add(customCss);
		}
		command.add(coverPath);
		command.add(htmlPath);
		command.add("-o");
		command.add(pdfPath);

		System.out.println("htmltopdf command: "
				+ command.toString().replace(",", ""));
		log("htmltopdf command: " + command.toString().replace(",", ""));

		int attempts = 0;
		int ATTEMPTS_MAX = 3;
		boolean success = false;
		RuntimeExec exec = null;
		ExecutionResult execResult = null;
		Process process = null;

		boolean runProcess = true;
		while (attempts++ < ATTEMPTS_MAX && !success) {
			if (!runProcess) {
				exec = new RuntimeExec();
				// exec.setProcessDirectory(this.fullDocGenDir);
				exec.setCommand(list2Array(command));
				execResult = exec.execute();
			} else {
				ProcessBuilder pb = new ProcessBuilder(command);
				// File file = new File(this.fullDocGenDir);
				// pb.directory(file);

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
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_IS_SAME_WIDTH_TABLE_CELL,postJson.optString("isSameWidthTableCell"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_CUSTOM_CSS, postJson.optString("customCss"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TOC, postJson.optString("toc"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TOF, postJson.optString("tof"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TOT, postJson.optString("tot"));
		htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_INDEX, postJson.optString("index"));
		services.getActionService().executeAction(htmlToPdfAction, jobNode.getNodeRef(), true, true);
	}

	protected void createCoverPage(String coverFilename, String coverContent)
			throws Throwable {
		log(String.format("Saving %s to filesystem...", coverFilename));
		Path coverPath = Paths.get(this.fsWorkingDir, coverFilename);
		Document document = Jsoup.parse(coverContent, "UTF-8");
		if (document == null) {
			throw new Throwable("Failed to parse HTML cover content!");
		}
		Element head = document.head();
		head.append("<meta charset=\"utf-8\" />");
		StringBuffer style = new StringBuffer();
		style.append("<link href=\"css/mm-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		style.append("<link href=\"css/ve-mms.styles.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
		head.append(style.toString());
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(coverPath.toString()));
			bw.write(document.toString());
		} catch (Throwable ex) {
			ex.printStackTrace();
			throw new Throwable(String.format(
					"Failed to save %s to filesystem! %s", coverFilename,
					ex.getMessage()));
		} finally {
			if (bw != null)
				bw.close();
		}
	}

	// protected void createFooterPage(String footerFilename, String
	// footerContent) throws IOException{
	// log(String.format("Saving %s to filesystem...", footerFilename));
	// Path footerPath = Paths.get(this.fsWorkingDir, footerFilename);
	//
	// File footerFile = new File(footerPath.toString());
	// StringBuffer html = new StringBuffer();
	// //
	// html.append("<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
	// html.append("<!DOCTYPE html>");
	// html.append(System.lineSeparator());
	// html.append("<html><head><title></title>");
	// html.append(System.lineSeparator());
	// html.append("	<style type=\"text/css\">");
	// html.append(System.lineSeparator());
	// html.append("		BODY{font-family:\"Times New Roman\"; margin:0 auto; padding: 0 auto;}");
	// html.append(System.lineSeparator());
	// html.append("		HR{width: 100%;margin: 0 auto;}");
	// html.append(System.lineSeparator());
	// html.append("		.itar{position:relative;margin:0 0 3px 0;display:inline-block;text-align:center;font-size:8pt;font-style:italic;font-weight:normal;line-height:9px;}");
	// html.append(System.lineSeparator());
	// html.append("		.page{text-align:center;font-size:8pt;}");
	// html.append(System.lineSeparator());
	// html.append("	</style>");
	// html.append(System.lineSeparator());
	// html.append("</head>");
	// html.append(System.lineSeparator());
	// html.append("<body>");
	// html.append(System.lineSeparator());
	// html.append(" 	<hr/>");
	// html.append(System.lineSeparator());
	// html.append("	<div class=\"page\"></div>");
	// html.append(System.lineSeparator());
	// html.append("		<div class=\"itar\">");
	// html.append(System.lineSeparator());
	// html.append(footerContent);
	// html.append(System.lineSeparator());
	// html.append("		</div>");
	// html.append(System.lineSeparator());
	// html.append("		<script type=\"text/javascript\">");
	// html.append(System.lineSeparator());
	// html.append("			(function() {");
	// html.append(System.lineSeparator());
	// html.append("			var vars={};");
	// html.append(System.lineSeparator());
	// html.append("			var x=document.location.search.substring(1).split('&');");
	// html.append(System.lineSeparator());
	// html.append("			for(var i in x) {var z=x[i].split('=',2);vars[z[0]] = unescape(z[1]);}");
	// html.append(System.lineSeparator());
	// html.append("			var x=['frompage','topage','page','webpage','section','subsection','subsubsection'];");
	// html.append(System.lineSeparator());
	// html.append("			for(var i in x) {");
	// html.append(System.lineSeparator());
	// html.append("				var y = document.getElementsByClassName(x[i]);");
	// html.append(System.lineSeparator());
	// html.append("				for(var j=0; j<y.length; ++j) y[j].textContent = vars[x[i]];");
	// html.append(System.lineSeparator());
	// html.append("			}");
	// html.append(System.lineSeparator());
	// html.append("		})();");
	// html.append(System.lineSeparator());
	// html.append("		</script>");
	// html.append(System.lineSeparator());
	// html.append("</body></html>");
	// BufferedWriter bw = new BufferedWriter(new FileWriter(footerFile));
	// bw.write(html.toString());
	// bw.close();
	// }

	/*
	 * builds up of header right-hand-side
	 */
	protected String buildHeaderContentRHS(String tagId, String timestamp,
			String displayTime) throws IOException {
		String contentRight = displayTime;
		if (Utils.isNullOrEmpty(displayTime)) {
			Date d = new Date();
			if (timestamp.compareToIgnoreCase("latest") != 0)
				d = TimeUtils.dateFromTimestamp(timestamp);
			contentRight = getFormattedDate(d);
		}
		if (timestamp.compareToIgnoreCase("latest") != 0)
			contentRight = String.format("%s %s", tagId, contentRight);
		return contentRight;
	}

	// protected void createHeaderPage(String headerFilename,
	// String headerContent, String tagId, String timestamp,
	// String docNum, String displayTime) throws IOException {
	// log(String.format("Saving %s to filesystem...", headerFilename));
	// Path headerPath = Paths.get(this.fsWorkingDir, headerFilename);
	//
	// File headerFile = new File(headerPath.toString());
	//
	// StringBuffer html = new StringBuffer();
	// if(Utils.isNullOrEmpty(displayTime)){
	// Date d = new Date();
	// if(timestamp.compareToIgnoreCase("latest") != 0) d =
	// TimeUtils.dateFromTimestamp(timestamp);
	// displayTime = getFormattedDate(d);
	// }
	//
	// html.append("<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
	// html.append(System.lineSeparator());
	// html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
	// html.append(System.lineSeparator());
	// html.append("    <head>");
	// html.append(System.lineSeparator());
	// html.append("        <title></title>");
	// html.append(System.lineSeparator());
	// html.append("        <style type=\"text/css\">");
	// html.append(System.lineSeparator());
	// html.append("            BODY{");
	// html.append(System.lineSeparator());
	// html.append("				font-size:8pt;font-family: \"Times New Roman\";");
	// html.append(System.lineSeparator());
	// html.append("				margin: 0 auto;}");
	// html.append(System.lineSeparator());
	// html.append("            HR{ width: 100%;}");
	// html.append(System.lineSeparator());
	// html.append("            .container{ }");
	// html.append(System.lineSeparator());
	// html.append("            .child{ margin: 5px 0 5px 0;overflow:hidden;line-heigh:11px;}");
	// html.append(System.lineSeparator());
	// html.append("            .left{ float:left; width:84%;}");
	// html.append(System.lineSeparator());
	// html.append("            .right{float:right; width:15%;text-align:right;}");
	// html.append(System.lineSeparator());
	// html.append("			</style>");
	// html.append(System.lineSeparator());
	// html.append("    </head>");
	// html.append(System.lineSeparator());
	// html.append("    <body>");
	// html.append(System.lineSeparator());
	// html.append("        <div class=\"container\">");
	// html.append(System.lineSeparator());
	// html.append("            <div class=\"child left\">");
	// html.append(System.lineSeparator());
	// html.append(headerContent);
	// html.append(System.lineSeparator());
	// html.append("            </div>");
	// html.append(System.lineSeparator());
	// html.append("            <div class=\"child right\">");
	// html.append(System.lineSeparator());
	// html.append("                <div class=\"date\">");
	// html.append(System.lineSeparator());
	// html.append(displayTime);
	// html.append(System.lineSeparator());
	// html.append("				</div>");
	// html.append(System.lineSeparator());
	// html.append("            </div>");
	// html.append(System.lineSeparator());
	// html.append("            <hr/>");
	// html.append(System.lineSeparator());
	// html.append("        </div>");
	// html.append(System.lineSeparator());
	// html.append("    </body>");
	// html.append(System.lineSeparator());
	// html.append("</html>");
	//
	// BufferedWriter bw = new BufferedWriter(new FileWriter(headerFile));
	// bw.write(html.toString());
	// bw.close();
	// }

	protected String getFormattedDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int day = cal.get(Calendar.DATE);

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

	public void handleEmbeddedImage(String htmlFilename) throws Exception {
		log(String.format("Saving images in %s to filesystem...", htmlFilename));
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

	private DBImage retrieveEmbeddedImage(String storeName, String nodeId,
			String imgName, WorkspaceNode workspace, Object timestamp)
			throws UnsupportedEncodingException {
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

	protected String zipWorkingDir(String zipFilename) throws IOException,
			InterruptedException {
		log("Zipping artifacts within working directory...");
		RuntimeExec exec = new RuntimeExec();
		exec.setProcessDirectory(this.fsWorkingDir);
		List<String> command = new ArrayList<String>();
		command.add("zip");
		command.add("-r");
		command.add(zipFilename);
		command.add(".");
		exec.setCommand(list2Array(command));
		// System.out.println("zip command: " + command);
		ExecutionResult result = exec.execute();

		if (!result.getSuccess()) {
			System.out.println("zip failed!");
			System.out.println("exit code: " + result.getExitValue());
		}

		return Paths.get(this.fsWorkingDir, zipFilename).toString();
	}

	public void cleanupFiles() {
		if (gov.nasa.jpl.mbee.util.FileUtils.exists(this.fsWorkingDir)) {
			try {
				FileUtils.forceDelete(new File(this.fsWorkingDir));
			} catch (IOException ex) {
				System.out.println(String.format(
						"Failed to cleanup temporary files at %s",
						this.fsWorkingDir));
			}
		}
	}
}
