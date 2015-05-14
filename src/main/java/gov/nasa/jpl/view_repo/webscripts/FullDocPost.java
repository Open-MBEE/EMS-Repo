package gov.nasa.jpl.view_repo.webscripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.TempFileProvider;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class FullDocPost extends AbstractJavaWebScript {
	protected String fullDocDir;
	protected String fullDocId;
	
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
        
        String ws = "ws";
        String docId = "MMS_1426295819727_3981ff0f-61fb-4681-a53f-5c5fbc8773f7";
        String time = "latest";
        String site = "europa";
        fullDocId = docId;
        fullDocDir = Paths.get(TempFileProvider.getTempDir().getAbsolutePath(), fullDocId).toString();
        try{
        	downloadHtml(ws, site, docId, time);
        	html2pdf();
        }
        catch(Exception ex){
        	
        }
        model.put("res", "testing");
        return model;
    }
    
    private void html2pdf()  throws IOException, InterruptedException {
    	ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File("/Users/lho/git/phantomjs-2.0.0-macosx/examples/target/"));

		List<String> command = new ArrayList<String>();
		command.add("wkhtmltopdf");
		command.add("-q");
		command.add("toc");
		command.add("xsl/default.xsl");
		command.add("fullDocResult.html");
		command.add(this.fullDocId + ".pdf");

		processBuilder.command(command);
		System.out.println("htmltopdf command: " + processBuilder.command());
		Process process = processBuilder.start();
		int exitCode = process.waitFor();
		if(exitCode != 0){
			System.out.println("failed to convert HTML to PDF!");
			System.out.println("exit code: " + exitCode);
		}	
    }
    
    private void downloadHtml(String ws, String site, String docId, String time) throws IOException, InterruptedException {
    	ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File("/Users/lho/git/phantomjs-2.0.0-macosx/examples/"));

		List<String> command = new ArrayList<String>();
		command.add("/Users/lho/git/phantomjs-2.0.0-macosx/bin/phantomjs");
		command.add("fullDoc.js");
		processBuilder.command(command);
		System.out.println("zip command: " + processBuilder.command());
		Process process = processBuilder.start();
		int exitCode = process.waitFor();
		if(exitCode != 0){
			System.out.println("failed to download full doc HTML!");
			System.out.println("exit code: " + exitCode);
		}	
    }
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return false;
    }

}
