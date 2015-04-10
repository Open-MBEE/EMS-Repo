package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.docbook.model.DBBook;
import gov.nasa.jpl.docbook.model.DBSerializeVisitor;
import gov.nasa.jpl.view_repo.DocBookContentTransformer;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class DocBookWrapper {
	public static final String DOC_BOOK_DIR_NAME = "docbook";

	private DBBook dbBook;
	private Path docGenCssFileName;
	private Path dbDirName;
	private Path dbFileName;
	private DBSerializeVisitor dbSerializeVisitor;
	private String content;
	private Path fobFileName;
	private Path fobXslFileName;
	private Path htmlXslFileName;
	private Path imageDirName;
	private Path jobDirName;
	private String snapshotName;
	private EmsScriptNode snapshotNode;
	private Path xalanDirName;
	private boolean hadFailed;

	public DocBookWrapper(String snapshotName, EmsScriptNode snapshotNode, boolean hadFailed){
		this.snapshotName = snapshotName;
		this.snapshotNode = snapshotNode;
		this.hadFailed = hadFailed;
		this.dbSerializeVisitor = new DBSerializeVisitor(true, null);
		setPaths();
	}

	private boolean createDocBookDir(){
		boolean bSuccess = true;
		if(!Files.exists(this.dbDirName)){
    		if(!new File(this.dbDirName.toString()).mkdirs()) {
    			bSuccess = false;
    			System.out.println("Failed to create DocBook Directory: " + this.dbDirName);
    		}
    	}
		return bSuccess;
	}

	/**
	 * Helper to execute the command using RuntimeExec
	 *
	 * @param srcFile	File to transform
	 * @return 			Absolute path of the generated file
	 * @throws Exception
	 */
	private String doPDFTransformation(File srcFile) throws Exception {
		RuntimeExec re = new RuntimeExec();
		List<String> command = new ArrayList<String>();

//		System.out.println("srcFile: " + srcFile.getAbsolutePath());
		String source = srcFile.getAbsolutePath();
		String target = source.subSequence(0, source.lastIndexOf(".")) + ".pdf";
		command.add(this.getFobFileName());
		command.add("-r");	//relaxed validation
		command.add("-xml");
		command.add(source);
		command.add("-xsl");
		command.add(this.getFobXslFileName());
		command.add("-pdf");
		command.add(target);

//		System.out.println("DO_TRANSFORM source: " + source);
//		System.out.println("DO_TRANSFORM target: " + target);
		System.out.println("DO_TRANSFROM cmd: " + command);

		re.setCommand(list2Array(command));
		ExecutionResult result = re.execute();

		if (!result.getSuccess()) {
			System.out.println("FOP transformation command unsuccessful\n");
			//logger.error("FOP transformation command unsuccessful\n");
			//System.out.println("Exit value: " + result.getExitValue());
			throw new Exception("FOP transformation command failed! Exit value: " + result.getExitValue());
		}

		return target;
	}

//	private String formatContent(String rawContent){
//		Document document = Jsoup.parseBodyFragment(rawContent);
//		Elements lits = document.getElementsByTag("literallayout");
//		for(Element lit : lits){
//			Elements cirRefs = lit.getElementsByTag("CircularReference");
//			for(Element cirRef : cirRefs){
//				//cirRef.before("</literallayout><font color='red'>Circular Reference!</font><literallayout>");
//				//cirRef.remove();
//			}
//		}
//		return document.body().html();
//	}
	
//	private String handleSpecialChars(String rawContent){
//		String[][] replacements = {
//				{"Α", "&#X0391;"},
//				{"Β", "&#X0392;"},
//				{"Γ", "&#x0393;"}, 
//                {"Δ", "&#x0394;"},
//                {"Ε", "&#x0395;"},
//                {"Ζ", "&#X0396;"},
//                {"Η", "&#X0397;"},
//                {"Θ", "&#x0398;"},
//                {"Ι", "&#x0399;"},
//                {"Κ", "&#x039A;"},
//                {"Λ", "&#x039B;"},
//                {"Μ", "&#x039C;"},
//                {"Ν", "&#x039D;"},
//                {"Ξ", "&#x039E;"},
//                {"Ο", "&#x039F;"},
//                {"Π", "&#x03A0;"},
//                {"Ρ", "&#x03A1;"},
//                {"Σ", "&#x03A3;"},
//                {"Τ", "&#x03A4;"},
//                {"Υ", "&#x03A5;"},
//                {"Φ", "&#x03A6;"},
//                {"Χ", "&#x03A7;"},
//                {"Ψ", "&#x03A8;"},
//                {"Ω", "&#x03A9;"},
//                {"α", "&#x03B1;"},
//                {"β", "&#x03B2;"},
//                {"γ", "&#x03B3;"},
//                {"δ", "&#x03B4;"},
//                {"ε", "&#x03B5;"},
//                {"ζ", "&#x03B6;"},
//                {"η", "&#x03B7;"},
//                {"θ", "&#x03B8;"},
//                {"ι", "&#x03B9;"},
//                {"κ", "&#x03BA;"},
//                {"λ", "&#x03BB;"},
//                {"μ", "&#x03BC;"},
//                {"ν", "&#x03BD;"},
//                {"ξ", "&#x03BE;"},
//                {"ο", "&#x03BF;"},
//                {"π", "&#x03C0;"},
//                {"ρ", "&#x03C1;"},
//                {"ς", "&#x03C2;"},
//                {"σ", "&#x03C3;"},
//                {"τ", "&#x03C4;"},
//                {"υ", "&#x03C5;"},
//                {"φ", "&#x03C6;"},
//                {"χ", "&#x03C7;"},
//                {"ψ", "&#x03C8;"},
//                {"ω", "&#x03C9;"},
//                {"ϑ", "&#x03D1;"},
//                {"ϒ", "&#x03D2;"},
//                {"ϖ", "&#x03D6;"}
//			};
//
//		String strOutput = rawContent;
//		for(String[] replacement: replacements) {
//			strOutput = strOutput.replace(replacement[0], replacement[1]);
//		}
//		return strOutput;
//	}

	public String getContent(){
		//this.dbBook.accept(this.dbSerializeVisitor);
		if(this.content == null || this.content.isEmpty()){
			this.dbSerializeVisitor.visit(dbBook);
			String rawContent = this.dbSerializeVisitor.getOut();
			rawContent = rawContent.replaceAll("<ulink", "<link");
			rawContent = rawContent.replaceAll("</ulink", "</link");
			rawContent = rawContent.replaceAll("<utable", "<table");
			rawContent = rawContent.replaceAll("</utable", "</table");
			rawContent = rawContent.replaceAll("<uthead", "<thead");
			rawContent = rawContent.replaceAll("</uthead", "</thead");
			rawContent = rawContent.replaceAll("<utbody", "<tbody");
			rawContent = rawContent.replaceAll("</utbody", "</tbody");
			rawContent = rawContent.replaceAll("<utfoot", "<tfoot");
			rawContent = rawContent.replaceAll("</utfoot", "</tfoot");
			rawContent = rawContent.replaceAll("&nbsp;", "&#160;");
			rawContent = rawContent.replaceAll("(?i)<removalTag>", "");
			rawContent = rawContent.replaceAll("(?i)</removalTag>", "");
			rawContent = rawContent.replaceAll("(?i)<entry/>", "<entry><para>&#160;</para></entry>");
//			rawContent = handleSpecialChars(rawContent);
			//this.content = formatContent(rawContent);
			this.content = rawContent;
		}
		return this.content;
	}

	public String getDBFileName(){
		return this.dbFileName.toString();
	}

	public String getDBDirImage(){
		return this.imageDirName.toString();
	}

	public String getDBDirName(){
		return this.dbDirName.toString();
	}

	public String getDocGenCssFileName(){
		return this.docGenCssFileName.toString();
	}

	public String getFobFileName(){
		return this.fobFileName.toString();
	}

	public String getFobXslFileName(){
		return this.fobXslFileName.toString();
	}

	public String getHtmlXslFileName(){
		return this.htmlXslFileName.toString();
	}

	public String getJobDirName(){
		return this.jobDirName.toString();
	}

	public EmsScriptNode getSnapshotNode(){
		return this.snapshotNode;
	}

	public String getXalanDirName(){
		return this.xalanDirName.toString();
	}

	/**
	 * Helper method to convert a list to an array of specified type
	 * @param list
	 * @return
	 */
	private String[] list2Array(List<String> list) {
		return Arrays.copyOf(list.toArray(), list.toArray().length, String[].class);
	}

	private void retrieveDocBook(WorkspaceNode ws, Date dateTime) throws Exception{
		File file = new File(this.dbFileName.toString());
		if(file.exists()) return;
		
		if(this.snapshotNode.hasAspect("view2:docbook")){
    		NodeRef dbNodeRef = (NodeRef)this.snapshotNode.getNodeRefProperty("view2:docbookNode", dateTime, ws);
    		if(dbNodeRef==null) throw new Exception("Failed to retrieve DocBook from repository! NodeRef is null!");
    		try{
    			retrieveStringPropContent(dbNodeRef, this.dbFileName);
    			System.out.println("retrieved docbook!");
    		}
    		catch(Exception ex){
    			ex.printStackTrace();
    			throw new Exception("Failed to retrieve DocBook!", ex);
    		}
    	}
    }

	private void retrieveImages(File srcFile, ServiceRegistry services, WorkspaceNode workspace, Date timestamp){
		DocBookContentTransformer dbTransf = new DocBookContentTransformer();
		//System.out.println("getting images...");
		for (String img: dbTransf.findImages(srcFile))
		{
			String imgFilename = this.getDBDirImage() + File.separator + img;
			File imgFile = new File(imgFilename);
			if (!imgFile.exists())
			{
				//System.out.println("finding image: " + imgFilename);
				NodeRef nr = NodeUtil.findNodeRefById(img, false, workspace, timestamp, services, false);

				ContentReader imgReader;
				//System.out.println("retrieving image file...");
				imgReader = services.getContentService().getReader(nr, ContentModel.PROP_CONTENT);
				//System.out.println("saving image file...");
				if(!Files.exists(this.imageDirName)){
					if(!new File(this.imageDirName.toString()).mkdirs()){
						System.out.println("Failed to create directory for " + this.imageDirName);
					}
				}
				imgReader.getContent(imgFile);
			}
		}
	}

    private void retrieveStringPropContent(NodeRef node, Path savePath) throws Exception{
    	if(!Files.exists(savePath.getParent())){
	    	if(!new File(savePath.getParent().toString()).mkdirs()){
	    		throw new Exception("Could not create path: " + savePath.getParent());
	    	}
    	}
    	ContentService contentService = this.getSnapshotNode().getServices().getContentService();
    	if(contentService == null) throw new Exception("Failed to retrieve content from repository! Content service is null!");

		ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
		if(reader==null) throw new Exception("Failed to retrieve content from repository! Content reader is null!");

		try{
			File srcFile = new File(savePath.toString());
			reader.getContent(srcFile);
		}
		catch(Exception ex){
			throw new Exception("Failed to write repository content to filesystem! " + savePath.toString(), ex);
		}

		int counter = 60;
		File dbFile = new File(this.dbFileName.toString());
		while(counter-- > 0){
			if(!dbFile.exists()){
				Thread.sleep(1000);
				continue;
			}
			if(!dbFile.canRead()){
				Thread.sleep(1000);
				continue;
			}
			return;
		}
		if(counter <= 0) throw new Exception("Failed to access DocBook.xml file! " + this.dbFileName.toString());
    }

	public void save() throws Exception{
		String docBookXml = this.getContent();
		if(docBookXml == null || docBookXml.isEmpty()) throw new Exception("Failed to save DBBook! DBBook content is null or empty!");

		try{
			new File(this.dbDirName.toString()).mkdirs();
	    	File f = new File(this.dbFileName.toString());
	    	FileWriter fw = new FileWriter(f);
	    	fw.write(docBookXml);
	    	fw.close();
		}
		catch(IOException ex){
			throw new Exception("Failed to write DBBook to filesystem.", ex);
		}
	}

	public void saveDocBookToRepo(EmsScriptNode snapshotFolder, Date timestamp) throws Exception{
		ServiceRegistry services = this.snapshotNode.getServices();
		try{
			ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( this.snapshotName + "_docbook.xml", "@cm\\:content:\"", services );
			if (nodeRefs != null && nodeRefs.size() == 1) {
				EmsScriptNode nodePrev = new EmsScriptNode(nodeRefs.get( 0 ), services, new StringBuffer());
				if(nodePrev != null && nodePrev.getName()==this.snapshotName + "_docbook.xml"){ 
					try{
						nodePrev.remove();
					}
					catch(Exception ex){
						System.out.println(String.format("problem removing previous docbook node. %s", ex.getMessage()));
						ex.printStackTrace();
					}
				}
			}

			EmsScriptNode node = snapshotFolder.createNode(this.snapshotName + "_docbook.xml", "cm:content");
			ActionUtil.saveStringToFile(node, "application/docbook+xml", services, this.getContent());
			if(this.snapshotNode.createOrUpdateAspect("view2:docbook")){
				this.snapshotNode.createOrUpdateProperty("view2:docbookNode", node.getNodeRef());
			}
			this.snapshotNode.createOrUpdateAspect("view2:timestamped");
			this.snapshotNode.createOrUpdateProperty("view2:timestamp", timestamp);

			if ( node != null ) node.getOrSetCachedVersion();
		}
		catch(Exception ex){
			ex.printStackTrace();
			throw new Exception("Failed to create docbook child node!", ex);
		}
	}

	public boolean saveFileToRepo(EmsScriptNode scriptNode, String mimeType, String filePath){
		boolean bSuccess = false;
		if(filePath == null || filePath.isEmpty()){
			System.out.println("File path parameter is missing!");
			return false;
		}
		if(!Files.exists(Paths.get(filePath))){
			System.out.println(filePath + " does not exist!");
			return false;
		}

		NodeRef nodeRef = scriptNode.getNodeRef();
		ContentService contentService = scriptNode.getServices().getContentService();

		ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
		writer.setLocale(Locale.US);
		File file = new File(filePath);
		writer.setMimetype(mimeType);
		try{
			writer.putContent(file);
			bSuccess = true;
		}
		catch(Exception ex){
			System.out.println("Failed to save '" + filePath + "' to repository!");
		}
		return bSuccess;
	}

	public void saveHtmlZipToRepo(EmsScriptNode snapshotFolder, WorkspaceNode workspace, Date timestamp) throws Exception{
		try{
			// removes any previously generated Zip node.
			ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( this.snapshotName + ".zip", "@cm\\:name:\"", snapshotFolder.getServices() );
			if(nodeRefs==null) nodeRefs = NodeUtil.findNodeRefsByType( this.snapshotName + ".zip", "@sysml\\:id:\"", snapshotFolder.getServices() );
			if (nodeRefs != null && nodeRefs.size() > 0) {
				EmsScriptNode nodePrev = new EmsScriptNode(nodeRefs.get( 0 ), snapshotFolder.getServices(), new StringBuffer());
				if(nodePrev != null && nodePrev.getName()==this.snapshotName + ".zip"){ 
					try{
						nodePrev.remove();
					}
					catch(Exception ex){
						System.out.println(String.format("problem removing previous artifact node. %s", ex.getMessage()));
						ex.printStackTrace();
					}
				}
			}

			//this.transformToHTML(workspace, timestamp);
			createDocBookDir();
			retrieveDocBook(workspace, timestamp);
			tableToCSV();
			String zipPath = this.zipHtml();
			if(zipPath == null || zipPath.isEmpty()) throw new Exception("Failed to zip files and resources!");

			EmsScriptNode node = snapshotFolder.createNode(this.snapshotName + ".zip", "cm:content");
			if(node == null) throw new Exception("Failed to create zip repository node!");

			if(!this.saveFileToRepo(node, MimetypeMap.MIMETYPE_ZIP, zipPath)) throw new Exception("Failed to save zip artifact to repository!");
			this.snapshotNode.createOrUpdateAspect("view2:htmlZip");
			this.snapshotNode.createOrUpdateProperty("view2:htmlZipNode", node.getNodeRef());

			if ( node != null ) node.getOrSetCachedVersion();
		}
		catch(Exception ex){
			throw new Exception("Failed to generate zip artifact!", ex);
		}
	}

	public void savePdfFailureToRepo(EmsScriptNode snapshotFolder, WorkspaceNode workspace, Date timestamp, String siteName) throws Exception{
		try{
			// removes any previously generated PDF node.
			ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( this.snapshotName + ".pdf", "@cm\\:name:\"", snapshotFolder.getServices() );
			if(nodeRefs==null) nodeRefs = NodeUtil.findNodeRefsByType( this.snapshotName + ".pdf", "@sysml\\:id:\"", snapshotFolder.getServices() );
			if (nodeRefs != null && nodeRefs.size() > 0) {
				EmsScriptNode nodePrev = new EmsScriptNode(nodeRefs.get( 0 ), snapshotFolder.getServices(), new StringBuffer());
				if(nodePrev != null && nodePrev.getName()==this.snapshotName + ".pdf"){ 
					try{
						nodePrev.remove();
					}
					catch(Exception ex){
						System.out.println(String.format("problem removing previous artifact node. %s", ex.getMessage()));
						ex.printStackTrace();
					}
				}
			}
			
			EmsScriptNode node = snapshotFolder.createNode(this.snapshotName + ".pdf", "cm:content");
			if(node == null) throw new Exception("Failed to create PDF repository node!");

			writeFailureDocBookFile();
			
			String pdfPath = transformToPDF(workspace, timestamp);
			if(pdfPath == null || pdfPath.isEmpty()) throw new Exception("Failed to transform from DocBook to PDF!");

			if(!this.saveFileToRepo(node, MimetypeMap.MIMETYPE_PDF, pdfPath)) throw new Exception("Failed to save PDF artifact to repository!");
			this.snapshotNode.createOrUpdateAspect("view2:pdf");
			this.snapshotNode.createOrUpdateProperty("view2:pdfNode", node.getNodeRef());

			if ( node != null ) node.getOrSetCachedVersion();
		}
		catch(Exception ex){
			throw new Exception("Failed to generate 'failure' PDF!", ex);
		}
	}
	
	public void savePdfToRepo(EmsScriptNode snapshotFolder, WorkspaceNode workspace, Date timestamp, String siteName) throws Exception{
		try{
			// removes any previously generated PDF node.
			ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( this.snapshotName + ".pdf", "@cm\\:name:\"", snapshotFolder.getServices() );
			if(nodeRefs==null) nodeRefs = NodeUtil.findNodeRefsByType( this.snapshotName + ".pdf", "@sysml\\:id:\"", snapshotFolder.getServices() );
			if (nodeRefs != null && nodeRefs.size() > 0) {
				EmsScriptNode nodePrev = new EmsScriptNode(nodeRefs.get( 0 ), snapshotFolder.getServices(), new StringBuffer());
				if(nodePrev != null && nodePrev.getName()==this.snapshotName + ".pdf"){ 
					try{
						nodePrev.remove();
					}
					catch(Exception ex){
						System.out.println(String.format("problem removing previous artifact node. %s", ex.getMessage()));
						ex.printStackTrace();
					}
				}
			}
			
			EmsScriptNode node = snapshotFolder.createNode(this.snapshotName + ".pdf", "cm:content");
			if(node == null) throw new Exception("Failed to create PDF repository node!");

			String pdfPath = transformToPDF(workspace, timestamp);
			if(pdfPath == null || pdfPath.isEmpty()) throw new Exception("Failed to transform from DocBook to PDF!");

			if(!this.saveFileToRepo(node, MimetypeMap.MIMETYPE_PDF, pdfPath)) throw new Exception("Failed to save PDF artifact to repository!");
			this.snapshotNode.createOrUpdateAspect("view2:pdf");
			this.snapshotNode.createOrUpdateProperty("view2:pdfNode", node.getNodeRef());

			if ( node != null ) node.getOrSetCachedVersion();
		}
		catch(Exception ex){
			throw new Exception("Failed to genearate PDF!", ex);
		}
	}

	public void setDBBook(DBBook dbBook){
		this.dbBook = dbBook;
	}

	private void setPaths(){
//		String tmpDirName	= TempFileProvider.getTempDir().getAbsolutePath();
//    	this.jobDirName = Paths.get(tmpDirName, this.snapshotName);
//		this.dbDirName = Paths.get(jobDirName.toString(), "docbook");
//		this.imageDirName = Paths.get(dbDirName.toString(), "images");
//		this.dbFileName = Paths.get(this.dbDirName.toString(), this.snapshotName + ".xml");
		
		String tmpDirName	= TempFileProvider.getTempDir().getAbsolutePath();
    	this.jobDirName = Paths.get(tmpDirName);
		this.dbDirName = Paths.get(jobDirName.toString(), this.snapshotName);
		this.imageDirName = Paths.get(dbDirName.toString(), "images");
		this.dbFileName = Paths.get(this.dbDirName.toString(), this.snapshotName + ((this.hadFailed) ? "_error" : "") + ".xml");

		//String docgenDirName = "/opt/local/alfresco/tomcat/webapps/alfresco/docgen/";
		String docgenDirName = "/opt/local/docbookgen/";
		if(!Files.exists(Paths.get(docgenDirName))){
			String userHome = System.getProperty("user.home");
			docgenDirName = Paths.get(userHome, "git/docbookgen").toString();
			if(!Files.exists(Paths.get(docgenDirName)))
				System.out.println("Failed to find docbookgen/fop directory!");
			else{
				docgenDirName = Paths.get(docgenDirName).toAbsolutePath().toString();
			}
		}
		this.fobFileName = Paths.get(docgenDirName, "fop-1.0", "fop");
		this.fobXslFileName = Paths.get(docgenDirName, "xsl/fo/mgss.xsl");
		this.xalanDirName = Paths.get(docgenDirName, "xalan-j_2_7_1");
		this.htmlXslFileName = Paths.get(docgenDirName, "xsl/html", "chunk_custom.xsl");
		this.docGenCssFileName = Paths.get(docgenDirName, "xsl", "docgen.css");
	}
	
	private void tableToCSV() throws Exception{
		File input = new File(this.dbFileName.toString());
		try {
			FileInputStream fileStream = new FileInputStream(input);
			Document document = Jsoup.parse(fileStream, "UTF-8", "http://xml.org", Parser.xmlParser());
			if(document == null) throw new Exception("Failed to convert tables to CSV! Unabled to load file: " + this.dbFileName.toString());
//			if(document.body() == null) throw new Exception(String.format("Failed to convert tables to CSV! DocBook file \"%s\" has not content.", this.dbFileName.toString()));
			
			int tableIndex = 1;
			int rowIndex = 1;
			String filename = "";
			int cols = 0;
			for(Element table:document.select("table")){
				Elements tgroups = table.select(" > tgroup");
				if(tgroups==null || tgroups.size()==0) continue;
				Element tgroup = tgroups.first();
				cols = Integer.parseInt(tgroup.attr("cols"));
				List<List<String>> csv = new ArrayList<List<String>>();
				Queue<TableCell> rowQueue = new LinkedList<TableCell>();
				Elements elements = tgroup.select("> thead");
				elements.addAll(tgroup.select("> tbody"));
				elements.addAll(tgroup.select("> tfoot"));
				for(Element row: elements.select("> row")){
					List<String> csvRow = new ArrayList<String>();
					
					for(int i=0; i < cols; i++){
						if(i >= row.children().size()){
							for(int k=cols; k > i; k--) csvRow.add("");
							break;
						}
						Element entry = row.child(i);
						if(entry != null && entry.text() != null && !entry.text().isEmpty()){ 
							csvRow.add(entry.text());
							
							//***handling multi-rows***
							String moreRows = entry.attr("morerows");
							if(moreRows != null && !moreRows.isEmpty()){
								int additionalRows = Integer.parseInt(moreRows);
								if(additionalRows > 0){
									for(int ar = 1; ar <= additionalRows; ar++){
										TableCell tableCell = new TableCell(rowIndex+ar, i);
										rowQueue.add(tableCell);
									}
								}
							}
							//***handling multi-rows***
							
							//***handling multi-columns***
							String colStart = entry.attr("namest");
							String colEnd = entry.attr("nameend");
							if(colStart == null || colEnd == null || colStart.isEmpty() || colEnd.isEmpty()) continue;
							
							int icolStart = Integer.parseInt(colStart);
							int icolEnd = Integer.parseInt(colEnd);
							for(int j=icolEnd; j > icolStart; j--, i++){
								csvRow.add("");
							}
							//***handling multi-columns***
						}
						else csvRow.add("");
						
						
					}
					csv.add(csvRow);
					rowIndex++;
				}

				boolean hasTitle = false;
				Elements title = table.select(" > title");
				if(title != null && title.size() > 0){
					String titleText = title.first().text();
					if(titleText != null && !titleText.isEmpty()){
						filename = title.first().text();
						hasTitle = true;
					}
				}
				
				if(!hasTitle) filename = "Untitled"; 
				filename = "table_" + tableIndex++ + "_" + filename;
				
				writeCSV(csv, filename, rowQueue, cols);
			}
			
		} 
		catch (IOException e) {
			e.printStackTrace();
			throw new Exception("IOException: unable to read/access file: " + this.dbFileName.toString());
		}
		catch(NumberFormatException ne){
			ne.printStackTrace();
			throw new Exception("One or more table row/column does not contain a parsable integer.");
		}
		
		
	}
	
	private void transformToHTML(WorkspaceNode workspace, Date timestamp) throws Exception{
		if(!createDocBookDir()) return;
//		System.out.println("Retrieving DocBook...");
		retrieveDocBook(workspace, timestamp);
		File srcFile = new File(this.getDBFileName());
//		System.out.println("Retrieving images...");
		retrieveImages(srcFile, this.snapshotNode.getServices(), workspace, timestamp);
		RuntimeExec re = new RuntimeExec();
		List<String> command = new ArrayList<String>();

		String source = this.getDBFileName();
		//String target = source.substring(0, source.indexOf(".")) + ".html";
		String xalanDir = this.getXalanDirName();
		String cp = xalanDir + "/xalan.jar:" + xalanDir + "/xercesImpl.jar:" + xalanDir + "/serializer.jar:" +xalanDir + "/xml-apis.jar";
		command.add("java");
		command.add("-cp");
		command.add(cp);
		command.add("org.apache.xalan.xslt.Process");
		command.add("-in");
		command.add(source);
		command.add("-xsl");
		command.add(this.getHtmlXslFileName());
		//command.add("-out");
		//command.add(target);
		command.add("-param");
		command.add("chunk.tocs.and.lots");
		command.add("1");
		command.add("-param");
		command.add("chunk.tocs.and.lots.has.title");
		command.add("1");
		command.add("-param");
		command.add("html.stylesheet");
		command.add("docgen.css");
		command.add("-param");
		command.add("chunk.first.sections");
		command.add("1");
		command.add("-param");
		command.add("chunk.section.depth");
		command.add("10");

		//System.out.println("DO_TRANSFORM source: " + source);
		//System.out.println("DO_TRANSFORM target: " + target);
		System.out.println("DO_TRANSFROM cmd: " + command);

		re.setCommand(list2Array(command));
		ExecutionResult result = re.execute();

		if (!result.getSuccess()) {
			System.out.println("Failed HTML transformation!\n");
			//logger.error("FOP transformation command unsuccessful\n");
			throw new Exception("Failed HTML transformation!");
		}
		else{
			String title = "";
			File frame = new File(Paths.get(this.getDBDirName(), "frame.html").toString());
			try{
				BufferedWriter writer = new BufferedWriter(new FileWriter(frame));
				writer.write("<html><head><title>" + title + "</title></head><frameset cols='30%,*' frameborder='1' framespacing='0' border='1'><frame src='bk01-toc.html' name='list'><frame src='index.html' name='body'></frameset></html>");
		        writer.close();
		        Files.copy(Paths.get(this.getDocGenCssFileName()), Paths.get(this.getDBDirName(), "docgen.css"), StandardCopyOption.REPLACE_EXISTING);
			}
			catch(Exception ex){
				throw new Exception("Failed to transform DocBook to HTML!", ex);
			}
		}
	}

	private String transformToPDF(WorkspaceNode workspace, Date timestamp) throws Exception{
    	if(!createDocBookDir()){
    		throw new Exception("Failed to create DocBook directory!");
    	}
//    	System.out.println("Retrieving DocBook...");
    	retrieveDocBook(workspace, timestamp);
		//ContentService contentService = this.snapshotNode.getServices().getContentService();
    	File srcFile = new File(this.getDBFileName());
//    	System.out.println("Retrieving images...");
    	retrieveImages(srcFile, this.snapshotNode.getServices(), workspace, timestamp);
		// do transformation then put result into writer
		String targetFilename = doPDFTransformation(srcFile);
		return targetFilename;
	}

	private void writeCSV(List<List<String>> csv, String filename, Queue<TableCell> rowQueue, int cols) throws Exception{
		String QUOTE = "\"";
	    String ESCAPED_QUOTE = "\"\"";
	    char[] CHARACTERS_THAT_MUST_BE_QUOTED = { ',', '"', '\n' };
	    
	    if(filename.length() > 100) filename = filename.substring(0,100);
		File outputFile = new File(Paths.get(this.dbDirName.toString(), filename+".csv").toString());
		try {
			FileWriter fw = new FileWriter(outputFile);
			BufferedWriter writer = new BufferedWriter(fw);
			int rowIndex = 1;
			boolean hasMoreRows;
			for(List<String> row : csv){
				for(int i=0; i < row.size() && i < cols; i++){
					if(i >= cols) break;
					hasMoreRows = false;
					if(!rowQueue.isEmpty()){
						TableCell tableCell = rowQueue.peek();
						if(tableCell.getRow()==rowIndex && tableCell.getColumn()==i){
							hasMoreRows = true;
							rowQueue.remove();
							if(i < cols-1) writer.write(",");
						}
					}
					String s = row.get(i);
					if(s.contains(QUOTE)){
						s = s.replace(QUOTE, ESCAPED_QUOTE);
					}
					if(StringUtils.indexOfAny(s, CHARACTERS_THAT_MUST_BE_QUOTED) > -1){
						s = QUOTE + s + QUOTE;
					}
				
					writer.write(s);
					if(!hasMoreRows) if(i < cols-1) writer.write(",");
				}
				writer.write(System.lineSeparator());
				rowIndex++;
			}
			writer.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception("Failed to save table-CSV to file system!");
		}
	}
	
	private void writeFailureDocBookFile() throws Exception{
		StringBuffer sb = new StringBuffer();
		sb.append("<book xmlns=\"http://docbook.org/ns/docbook\" xmlns:xl=\"http://www.w3.org/1999/xlink\" version=\"5.0\">");
		sb.append("<info><title>PDF Generation Failed</title></info>");
		sb.append("<preface>");
		sb.append("<info><title> </title></info>");
		sb.append("<para>Please refer to the DocBook XML file, found within the associated zip artifact; open it ");
		sb.append("with \"Oxygen XML Editor\" and manually resolve any validation issues and then transform it ");
		sb.append("into PDF.</para>");
		sb.append("</preface>");
		sb.append("</book>");
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this.dbFileName.toString())));
		bw.write(sb.toString());
		bw.flush();
		bw.close();
	}
	
	public String zipHtml() throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File(this.getJobDirName()));
//		System.out.println("zip working directory: " + processBuilder.directory());
		List<String> command = new ArrayList<String>();
		String zipFile = this.snapshotName + ".zip";
		command.add("zip");
		command.add("-r");
		command.add(zipFile);
		//command.add("\"*.html\"");
		//command.add("\"*.css\"");
		command.add(this.snapshotName);

		// not including docbook and pdf files
		//command.add("-x");
		//command.add("*.db");
		//command.add("*.pdf");

		processBuilder.command(command);
		System.out.println("zip command: " + processBuilder.command());
		Process process = processBuilder.start();
		int exitCode = process.waitFor();
		if(exitCode != 0){
			System.out.println("zip failed!");
			System.out.println("exit code: " + exitCode);
		}

		return Paths.get(this.getJobDirName(), zipFile).toString();
	}
}
