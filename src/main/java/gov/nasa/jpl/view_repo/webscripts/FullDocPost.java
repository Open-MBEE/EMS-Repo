package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class FullDocPost extends AbstractJavaWebScript {
	protected String fullDocDir;
	protected String fullDocId;
	protected String htmlPath;
	protected String pdfPath;
	
	public void setFullDocId(String id){
		this.fullDocId = id;
		this.setFullDocDir();
		this.setPaths();
	}
	
	private void setFullDocDir(){
		fullDocDir = Paths.get(TempFileProvider.getTempDir().getAbsolutePath(), fullDocId).toString();
	}
	
	private void setPaths(){
		this.htmlPath = Paths.get(TempFileProvider.getTempDir().getAbsolutePath(), fullDocId, String.format("%s_NodeJS.html", fullDocId)).toString();
		this.pdfPath = Paths.get(TempFileProvider.getTempDir().getAbsolutePath(), fullDocId, String.format("%s_NodeJS.pdf", fullDocId)).toString();
	}
	
	public String getHtmlPath(){
		return this.htmlPath;
	}
	
	public String getPdfPath(){
		return this.pdfPath;
	}
	
	public FullDocPost(){
		super();
	}
	
	public FullDocPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }
	
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    	FullDocPost instance = new FullDocPost(repository, getServices());
        return instance.executeImplImpl(req,  status, cache, runWithoutTransactions);
    }

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        printHeader( req );
        Map< String, Object > model = new HashMap< String, Object >();
        JSONObject snapshotJson = (JSONObject)req.parseContent();
        JSONObject json = snapshotJson.getJSONObject("snapshot");
        String ws = json.optString("ws");
        String docId = json.optString("sysmlid");
        String time = json.optString("time");
        String site = json.optString("site");
        fullDocId = docId;
//        fullDocDir = Paths.get(TempFileProvider.getTempDir().getAbsolutePath(), fullDocId).toString();
        this.setFullDocDir();
        this.setPaths();
        try{
        	downloadHtml(ws, site, docId, time);
        	html2pdf();
        }
        catch(Exception ex){
        	
        }
        model.put("res", "testing");
        return model;
    }
    
    public void html2pdf()  throws IOException, InterruptedException {
    	ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File("/Users/lho/git/phantomjs-2.0.0-macosx/examples/target/"));

		List<String> command = new ArrayList<String>();
		command.add("wkhtmltopdf");
		command.add("-q");
		command.add("toc");
		command.add("xsl/default.xsl");
		command.add(this.getHtmlPath());
		command.add(this.getPdfPath());

		processBuilder.command(command);
		System.out.println("htmltopdf command: " + processBuilder.command());
		Process process = processBuilder.start();
		int exitCode = process.waitFor();
		if(exitCode != 0 && exitCode != 1){
			System.out.println("failed to convert HTML to PDF!");
			System.out.println("exit code: " + exitCode);
		}	
    }
    
    public void downloadHtml(String ws, String site, String docId, String time) throws Exception {
    	ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File("/Users/lho/git/phantomjs-2.0.0-macosx/examples/"));	//to do : need to config
		HostnameGet alfresco = new HostnameGet(this.repository, this.services);
		String protocol = alfresco.getAlfrescoProtocol();
		String hostname = alfresco.getAlfrescoHost();
		int alfrescoPort = 9000;	//to do: need to config
		String preRendererUrl = "http://localhost";	//to do: need to config
		int preRendererPort = 3000;	// to do: need to config
		String mmsAdminCredential = "admin:admin";	// to do: need to config
		List<String> command = new ArrayList<String>();
		command.add("/Users/lho/git/phantomjs-2.0.0-macosx/bin/phantomjs");
		command.add("fullDoc.js");
		command.add(String.format("%s:%d/%s://%s@%s:%d/mmsFullDoc.html?ws=%s&site=%s&docId=%s&time=%s",preRendererUrl,preRendererPort, protocol, mmsAdminCredential, hostname,alfrescoPort, ws, site, docId, time));
		command.add(String.format("%s/%s_NodeJS.html", this.fullDocDir, this.fullDocId));
		processBuilder.command(command);
		System.out.println("phantomJS command: " + processBuilder.command());
		Process process = processBuilder.start();
		int exitCode = process.waitFor();
		if(exitCode != 0){
			System.out.println("failed to download full doc HTML!");
			System.out.println("exit code: " + exitCode);
		}
		
		try{
			tableToCSV();
		}
		catch(Exception ex){
			throw new Exception("Failed to convert tables to CSV files!", ex);
		}
    }
    
    private String getHtmlText(String htmlString){
		if(htmlString == null || htmlString.isEmpty()) return "";
		Document document = Jsoup.parseBodyFragment(htmlString);
		if(document == null || document.body()== null) return "";
		return document.body().text();
	}
    
    public void savePdfToRepo(EmsScriptNode snapshotFolder, EmsScriptNode snapshotNode) throws Exception{
//		ServiceRegistry services = this.snapshotNode.getServices();
    	String filename = String.format("%s.pdf", this.fullDocId);
		try{
			ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( filename, "@cm\\:name:\"", snapshotFolder.getServices() );
			if(nodeRefs==null) nodeRefs = NodeUtil.findNodeRefsByType( filename, "@sysml\\:id:\"", snapshotFolder.getServices() );
			if (nodeRefs != null && nodeRefs.size() > 0) {
				EmsScriptNode nodePrev = new EmsScriptNode(nodeRefs.get( 0 ), snapshotFolder.getServices(), new StringBuffer());
				if(nodePrev != null && nodePrev.getName()==filename){ 
					try{
						nodePrev.remove();
					}
					catch(Exception ex){
						System.out.println(String.format("problem removing previous artifact node. %s", ex.getMessage()));
						ex.printStackTrace();
					}
				}
			}
			
			EmsScriptNode node = snapshotFolder.createNode(filename, "cm:content");
			if(node == null) throw new Exception("Failed to create PDF repository node!");

//			String pdfPath = transformToPDF(workspace, timestamp);
//			if(pdfPath == null || pdfPath.isEmpty()) throw new Exception("Failed to transform from DocBook to PDF!");

			if(!this.saveFileToRepo(node, MimetypeMap.MIMETYPE_PDF, this.getPdfPath())) throw new Exception("Failed to save PDF artifact to repository!");
			snapshotNode.createOrUpdateAspect("view2:pdf");
			snapshotNode.createOrUpdateProperty("view2:pdfNode", node.getNodeRef());

			if ( node != null ) node.getOrSetCachedVersion();
		}
		catch(Exception ex){
			ex.printStackTrace();
			throw new Exception("Failed to save PDF!", ex);
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

	public void saveZipToRepo(EmsScriptNode snapshotFolder, EmsScriptNode snapshotNode) throws Exception{
		String filename = String.format("%s.zip", this.fullDocId);
		try{
			// removes any previously generated Zip node.
			ArrayList<NodeRef> nodeRefs = NodeUtil.findNodeRefsByType( filename, "@cm\\:name:\"", snapshotFolder.getServices() );
			if(nodeRefs==null) nodeRefs = NodeUtil.findNodeRefsByType( filename, "@sysml\\:id:\"", snapshotFolder.getServices() );
			if (nodeRefs != null && nodeRefs.size() > 0) {
				EmsScriptNode nodePrev = new EmsScriptNode(nodeRefs.get( 0 ), snapshotFolder.getServices(), new StringBuffer());
				if(nodePrev != null && nodePrev.getName()==filename){ 
					try{
						nodePrev.remove();
					}
					catch(Exception ex){
						System.out.println(String.format("problem removing previous artifact node. %s", ex.getMessage()));
						ex.printStackTrace();
					}
				}
			}

			//createDocBookDir();
			//retrieveDocBook(workspace, timestamp);
//			this.transformToHTML(workspace, timestamp);
//			tableToCSV();
//			String zipPath = this.zipHtml();
//			if(zipPath == null || zipPath.isEmpty()) throw new Exception("Failed to zip files and resources!");
			this.zipHtml();
			EmsScriptNode node = snapshotFolder.createNode(filename, "cm:content");
			if(node == null) throw new Exception("Failed to create zip repository node!");

			if(!this.saveFileToRepo(node, MimetypeMap.MIMETYPE_ZIP, String.format("%s.zip", this.fullDocId))) throw new Exception("Failed to save zip artifact to repository!");
			snapshotNode.createOrUpdateAspect("view2:htmlZip");
			snapshotNode.createOrUpdateProperty("view2:htmlZipNode", node.getNodeRef());

			if ( node != null ) node.getOrSetCachedVersion();
		}
		catch(Exception ex){
			throw new Exception("Failed to generate zip artifact!", ex);
		}
	}

	
    private void tableToCSV() throws Exception{
		File input = new File(this.getHtmlPath());
		try {
			FileInputStream fileStream = new FileInputStream(input);
//			Document document = Jsoup.parse(fileStream, "UTF-8", "http://xml.org", Parser.xmlParser());
			Document document = Jsoup.parse(fileStream, "UTF-8", "");
			if(document == null) throw new Exception("Failed to convert tables to CSV! Unabled to load file: " + this.getHtmlPath());
			
//			if(document.body() == null) throw new Exception(String.format("Failed to convert tables to CSV! DocBook file \"%s\" has not content.", this.dbFileName.toString()));
			
			int tableIndex = 1;
			int rowIndex = 1;
			String filename = "";
			int cols = 0;
			for(Element table:document.select("table")){
//				Elements tgroups = table.select(" > tgroup");
//				if(tgroups==null || tgroups.size()==0) continue;
//				Element tgroup = tgroups.first();
//				cols = Integer.parseInt(tgroup.attr("cols"));
				List<List<String>> csv = new ArrayList<List<String>>();
				Queue<TableCell> rowQueue = new LinkedList<TableCell>();
				Elements elements = table.select("> thead");
				elements.addAll(table.select("> tbody"));
				elements.addAll(table.select("> tfoot"));
				for(Element row: elements.select("> tr")){
					List<String> csvRow = new ArrayList<String>();
					cols = row.children().size();
					for(int i=0; i < cols; i++){
						if(i >= row.children().size()){
							for(int k=cols; k > i; k--) csvRow.add("");
							break;
						}
						Element entry = row.child(i);
						if(entry != null && entry.text() != null && !entry.text().isEmpty()){ 
							csvRow.add(entry.text());
							
							//***handling multi-rows***
							String moreRows = entry.attr("rowspan");
							if(moreRows != null && !moreRows.isEmpty()){
								int additionalRows = Integer.parseInt(moreRows);
								if(additionalRows > 1){
									for(int ar = 1; ar <= additionalRows; ar++){
										TableCell tableCell = new TableCell(rowIndex+ar, i);
										rowQueue.add(tableCell);
									}
								}
							}
							//***handling multi-rows***
							
							//***handling multi-columns***
//							String colStart = entry.attr("colspan");
//							String colEnd = entry.attr("nameend");
							String rowspan = entry.attr("colspan");
							if(rowspan == null || rowspan.isEmpty()) continue;
							
//							int icolStart = Integer.parseInt(colStart);
//							int icolEnd = Integer.parseInt(colEnd);
							int irowspan = Integer.parseInt(rowspan);
							if(irowspan < 2) continue;
							for(int j=2; j < irowspan; j++, i++){
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
				Elements title = table.select(" > caption");
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
			throw new Exception("IOException: unable to read/access file: " + this.getHtmlPath());
		}
		catch(NumberFormatException ne){
			ne.printStackTrace();
			throw new Exception("One or more table row/column does not contain a parsable integer.");
		}
	}

    private void writeCSV(List<List<String>> csv, String filename, Queue<TableCell> rowQueue, int cols) throws Exception{
		String QUOTE = "\"";
	    String ESCAPED_QUOTE = "\"\"";
	    char[] CHARACTERS_THAT_MUST_BE_QUOTED = { ',', '"', '\n' };
	    filename = getHtmlText(filename);
	    if(filename.length() > 100) filename = filename.substring(0,100);
		File outputFile = new File(Paths.get(this.fullDocDir, filename+".csv").toString());
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
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return false;
    }

    public void zipHtml() throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File(this.fullDocDir));
//		System.out.println("zip working directory: " + processBuilder.directory());
		List<String> command = new ArrayList<String>();
		String zipFile = this.fullDocId + ".zip";
		command.add("zip");
		command.add("-r");
		command.add(zipFile);
		//command.add("\"*.html\"");
		//command.add("\"*.css\"");
		command.add(this.fullDocId);

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

//		return Paths.get(this.getJobDirName(), zipFile).toString();
	}
}
