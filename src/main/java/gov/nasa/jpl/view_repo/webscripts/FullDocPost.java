package gov.nasa.jpl.view_repo.webscripts;

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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.XMLUtil;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.store.Directory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FullDocPost extends AbstractJavaWebScript {
	protected String fullDocGenDir;	//dir containing full doc generation resources (prerenderer.io, phantomJS, wkhtmltopdf)
	protected String phantomJSPath;
	protected String phantomJSScriptPath;
	protected String fullDocDir;
	protected String fullDocId;
	protected String htmlPath;
	protected String imgPath;
	protected Path imageDirName;
	protected String parentPath;
	protected String pdfPath;
	protected String veCssDir;
	protected String zipPath;
	
	public void setFullDocId(String id){
		this.fullDocId = id;
		this.setFullDocDir();
		this.setPaths();
	}
	
	private void setFullDocDir(){
		fullDocDir = Paths.get(TempFileProvider.getTempDir().getAbsolutePath(), fullDocId).toString();
	}
	
	private void setPaths(){
        this.parentPath = Paths.get(this.fullDocDir).getParent().toString();
        String tmpDirName    = TempFileProvider.getTempDir().getAbsolutePath();
        this.htmlPath = Paths.get(tmpDirName, fullDocId, String.format("%s_NodeJS.html", fullDocId)).toString();
        this.pdfPath = Paths.get(tmpDirName, fullDocId, String.format("%s_NodeJS.pdf", fullDocId)).toString();
        this.veCssDir = "/opt/local/apache-tomcat/webapps/alfresco/mmsapp/css";
        this.zipPath = String.format("%s/%s.zip", this.parentPath, this.fullDocId);
        this.imgPath = Paths.get(tmpDirName,fullDocId).toString();
        this.imageDirName = Paths.get(imgPath.toString(), "images");
        this.phantomJSPath = "/opt/local/fullDocGen/prerender/node_modules/phantomjs/bin/phantomjs";
        this.phantomJSScriptPath = "/opt/local/fullDocGen/fullDoc.js";
        this.fullDocGenDir = "/opt/local/fullDocGen/";
        
        try{
        	new File(this.imgPath).mkdirs();
        }catch(Exception ex){;}
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
//        JSONObject snapshotJson = (JSONObject)req.parseContent();
//        JSONObject json = snapshotJson.getJSONObject("snapshot");
//        String ws = json.optString("ws");
//        String docId = json.optString("sysmlid");
//        String time = json.optString("time");
//        String site = json.optString("site");
//        fullDocId = docId;
////        fullDocDir = Paths.get(TempFileProvider.getTempDir().getAbsolutePath(), fullDocId).toString();
//        this.setFullDocDir();
//        this.setPaths();
//        try{
//        	downloadHtml(ws, site, docId, time);
//        	html2pdf();
//        }
//        catch(Exception ex){
//        	
//        }
        model.put("res", "testing");
        return model;
    }
    
    private String getAlfrescoHost(){
    	HostnameGet alfresco = new HostnameGet(this.repository, this.services);
    	String hostname = alfresco.getAlfrescoHost();
    	if(hostname.compareToIgnoreCase("localhost")==0){
    		hostname += ":9000";
    	}
    	return hostname;
    }
    
    public void downloadHtml(WorkspaceNode workspace, String site, String docId, Date time) throws Exception {
    	RuntimeExec exec = new RuntimeExec();
		//exec.setProcessDirectory("/opt/local/prerender/node_modules/phantomjs/bin/");	//to do : need to config
		HostnameGet alfresco = new HostnameGet(this.repository, this.services);
		String protocol = alfresco.getAlfrescoProtocol();
		String hostname = alfresco.getAlfrescoHost();
		String hostnameAndPort = this.getAlfrescoHost();
		String preRendererUrl = String.format("%s:%s", protocol, hostname);	// "http://localhost";	//to do: need to config
		int preRendererPort = 3000;	// to do: need to config
		String mmsAdminCredential = getHeadlessUserCredential();
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		List<String> command = new ArrayList<String>();
		command.add(this.phantomJSPath);
		command.add(this.phantomJSScriptPath);
		command.add(String.format("%s:%d/%s://%s@%s/mmsFullDoc.html?ws=%s&site=%s&docId=%s&time=%s",
				preRendererUrl,preRendererPort, protocol, mmsAdminCredential, hostnameAndPort, workspace.getName(), site, docId, fmt.print(new DateTime(time))));
		command.add(String.format("%s/%s_NodeJS.html", this.fullDocDir, this.fullDocId));
		exec.setCommand(list2Array(command));
		System.out.println("NodeJS command: " + command);
		ExecutionResult result = exec.execute();
		if (!result.getSuccess()) {
			System.out.println("failed to download full doc HTML!");
			System.out.println("exit code: " + result.getExitValue());
		}
		
		//parse HTML
		//for each IMG tag
			//get SRC attr
			//retrieve image from Repo
			//save image to local Filesystem
			//update SRC attr to local filesystem path
//		 
		File imgSrcFile = new File(this.getHtmlPath());
        retrieveImages(imgSrcFile, this.getServices(), workspace, time);    
		
        try{
			tableToCSV();
		}
		catch(Exception ex){
			throw new Exception("Failed to convert tables to CSV files!", ex);
		}
        FileUtils.copyDirectory(new File(this.veCssDir), new File(Paths.get(this.fullDocDir, "css").toString()));
    }
    
    private List<String> findImages(File htmlFile) throws Exception{
        List<String> images = new ArrayList<String>();
        try{
            Document document = Jsoup.parse(htmlFile, "UTF-8", "http://example.com");
            if(document == null) throw new Exception("Failed to read HTML file. Unabled to load file: " + this.getHtmlPath());
            
            for(Element img:document.select("img")){
                Elements elements = img.select("src");
                for(Element imgSrc : elements){
                    String srcAddress = imgSrc.attr("src");
                    System.out.println(srcAddress);
                    images.add(srcAddress);
                    
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
            throw new Exception("Failed to find Images!", ex);
        }
        return images;
    }
    
    private String getHeadlessUserCredential(){
    	String cred = "admin:admin";
    	String usr = null;
    	String psswrd = null;
    	String filePath = Paths.get("/opt/local/apache-tomcat/webapps/alfresco/WEB-INF/classes/alfresco/module/view-repo/context/mms-init-service-context.xml").toAbsolutePath().normalize().toString();
    	
    	try{
    		File fXmlFile = new File(filePath);
    		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    		org.w3c.dom.Document xml = dBuilder.parse(fXmlFile);
    		
    		NodeList list = xml.getElementsByTagName("property");
    		for(int i=list.getLength()-1; i>=0; i--)
    		{
    			Node node = list.item(i);
    			if(node.hasAttributes()){
    				Node value = node.getAttributes().getNamedItem("value");
    				if(value == null) continue;
    				if(value.getNodeValue().compareToIgnoreCase("gov.nasa.jpl.view_repo.webscripts.util.ShareUtils.setUsername")==0){
    					usr = node.getNextSibling().getNextSibling().getAttributes().getNamedItem("value").getNodeValue();
    				}
    				else if(value.getNodeValue().compareToIgnoreCase("gov.nasa.jpl.view_repo.webscripts.util.ShareUtils.setPassword")==0){
    					psswrd = node.getNextSibling().getNextSibling().getAttributes().getNamedItem("value").getNodeValue();
    				}
    			}
    			if(usr!=null && psswrd!=null) return String.format("%s:%s", usr, psswrd);
    		}
    	}
    	catch(Exception ex){
    		System.out.println(String.format("problem retrieving headless credential. %s", ex.getMessage()));
			ex.printStackTrace();
    	}
    	return cred;
    }
    
    private String getHtmlText(String htmlString){
		if(htmlString == null || htmlString.isEmpty()) return "";
		Document document = Jsoup.parseBodyFragment(htmlString);
		if(document == null || document.body()== null) return "";
		return document.body().text();
	}
    
    private String getImgPath(){
    	return this.imgPath;
    }
    
    public void html2pdf()  throws IOException, InterruptedException {
    	RuntimeExec exec = new RuntimeExec();
		exec.setProcessDirectory(this.fullDocGenDir);

		List<String> command = new ArrayList<String>();
		command.add("wkhtmltopdf");
		command.add("-q");
		command.add("toc");
		command.add("wkhtmltopdf/xsl/default.xsl");
		command.add(this.getHtmlPath());
		command.add(this.getPdfPath());

		System.out.println("htmltopdf command: " + command);
		exec.setCommand(list2Array(command));
		ExecutionResult result = exec.execute();
		if(!result.getSuccess()){
			System.out.println("failed to convert HTML to PDF!");
			System.out.println("exit code: " + result.getExitValue());
		}	
    }

	/**
	 * Helper method to convert a list to an array of specified type
	 * @param list
	 * @return
	 */
	private String[] list2Array(List<String> list) {
		return Arrays.copyOf(list.toArray(), list.toArray().length, String[].class);
	}
    
	private void retrieveImages(File srcFile, ServiceRegistry services, WorkspaceNode workspace, Date timestamp) throws Exception{
//      DocBookContentTransformer dbTransf = new DocBookContentTransformer();
      //can't use dbTransf.findImages() because it's basing off docbook.xml file and syntax
      //need to replace the function with one that extract images file name from HTML file.
      try{
          List<String> imgs = this.findImages(srcFile);
          for (String img: imgs)
          {
              String imgFilename = this.getImgPath() + File.separator + img;
              File imgFile = new File(imgFilename);
              if (!imgFile.exists())
              {
                  System.out.println("finding image: " + imgFilename);
                  NodeRef nr = NodeUtil.findNodeRefById(img, false, workspace, timestamp, services, false);
  
                  ContentReader imgReader;
                  System.out.println("retrieving image file...");
                  imgReader = services.getContentService().getReader(nr, ContentModel.PROP_CONTENT);
                  System.out.println("saving image file...");
                  if(!Files.exists(this.imageDirName)){
                      if(!new File(this.imageDirName.toString()).mkdirs()){
                          System.out.println("Failed to create directory for " + this.imageDirName);
                      }
                  }
                  imgReader.getContent(imgFile);
              }
          }
      }
      catch(Exception ex){
          ex.printStackTrace();
          throw new Exception("Failed to find Images!", ex);
      }
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

			if(!this.saveFileToRepo(node, MimetypeMap.MIMETYPE_ZIP, this.zipPath)) throw new Exception("Failed to save zip artifact to repository!");
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
//		ProcessBuilder processBuilder = new ProcessBuilder();
//		processBuilder.directory(new File(Paths.get(this.fullDocDir).getParent().toString()));
////		System.out.println("zip working directory: " + processBuilder.directory());
//		List<String> command = new ArrayList<String>();
//		String zipFile = this.fullDocId + ".zip";
//		command.add("zip");
//		command.add("-r");
//		command.add(zipFile);
//		//command.add("\"*.html\"");
//		//command.add("\"*.css\"");
//		command.add(this.fullDocId);
//
//		// not including docbook and pdf files
//		//command.add("-x");
//		//command.add("*.db");
//		//command.add("*.pdf");
//
//		processBuilder.command(command);
//		System.out.println("zip command: " + processBuilder.command());
//		Process process = processBuilder.start();
//		int exitCode = process.waitFor();
//		if(exitCode != 0){
//			System.out.println("zip failed!");
//			System.out.println("exit code: " + exitCode);
//		}
    	
		RuntimeExec exec = new RuntimeExec();
		exec.setProcessDirectory(Paths.get(this.fullDocDir).getParent().toString());
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

		exec.setCommand(list2Array(command));
		System.out.println("zip command: " + command);
		ExecutionResult result = exec.execute();

		if (!result.getSuccess()) {
			System.out.println("zip failed!");
			System.out.println("exit code: " + result.getExitValue());
		}
	}
}
