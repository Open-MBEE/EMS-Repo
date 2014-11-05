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
package gov.nasa.jpl.view_repo;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.transform.AbstractContentTransformer2;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Transformation that converts DocBook to PDF
 * 
 * TODO: Investigate how to add more properties to transformation so the XSL can be specified
 * 
 * TODO: Add in the HTML generation as well or create separate transformer
 * 
 * @author cinyoung
 *
 */
public class DocBookContentTransformer extends AbstractContentTransformer2 {
	// local variables
	private static final Log logger = LogFactory.getLog(DocBookContentTransformer.class);
	
	// Spring injected variables 
	private ContentService contentService;
	private NodeService nodeService;
	private SearchService searchService;
	private String path;
	// TODO: Need to include the FOP and XSL files in distro, specified in service-context.xml
	private String fop;
	private String xsl;

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public void setFop(String fop) {
		this.fop = fop;
	}
	
	public void setXsl(String xsl) {
		this.xsl = xsl;
	}

	/**
	 * Checks whether transformation is applicable
	 * 
	 * Convert both DocBook to PDF and Zip to PDF
	 */
	public boolean isTransformableMimetype(String sourceMimetype, String targetMimetype, TransformationOptions options)
	{
	     return (("text/xslfo".equals(sourceMimetype) || ("text/xml".equals(sourceMimetype)) || ("application/zip".equals(sourceMimetype))) 
	    		 && ("application/pdf".equals(targetMimetype)));
	}
	
	
	/**
	 * Method that is called by transformation service.
	 * 
	 * The input file can be either XML or ZIP.
	 * 
	 */
	@Override
	protected void transformInternal(ContentReader reader,
			ContentWriter writer,
			TransformationOptions options) throws Exception {
		NodeRef sourceNR	= options.getSourceNodeRef();
		String tmpDirName	= TempFileProvider.getTempDir().getAbsolutePath();
		String dbDirName	= tmpDirName + File.separator + "docbook";
		String imageDirName = tmpDirName + File.separator + "images";
		
		// actual file to transform
		File srcFile ;
		
		String srcPath = (String) nodeService.getProperty(sourceNR, ContentModel.PROP_NAME);

		if (srcPath.indexOf(".zip") > 0) {
			File zipFile = new File(tmpDirName + File.separator + srcPath);
			reader.getContent(zipFile);
			if (!unzip(zipFile)) {
				return; // TODO throw exception
			}
			String zipDir = "docbook" + File.separator + zipFile.getName().substring(0, zipFile.getName().indexOf("."));
			srcFile = new File(tmpDirName + File.separator + zipDir + File.separator + "out.xml");
			if (Debug.isOn()) System.out.println("ZIP DIR: " + srcFile.getAbsolutePath());
		} else {
			// Create directories for DB and images
			if ( !(new File(dbDirName).mkdirs()) ) {
				logger.error("Could not create Docbook temporary directory\n");
			}
			if ( !(new File(imageDirName).mkdirs()) ) {
				logger.error("Could not create image temporary directory\n");
			}
			
			// Create temporary files for transformation (need DocBook file and respective images)
			srcFile = new File(dbDirName + File.separator + srcPath);
			reader.getContent(srcFile);
		
			// TODO: check image time information with temp file and replace if image has been updated more recently
			for (String img: findImages(srcFile)) {
				String imgFilename = imageDirName + File.separator + img;
				File imgFile = new File(imgFilename);
				if (!imgFile.exists()) {
					ResultSet rs = findNodeRef(img);
					ContentReader imgReader;
					for (NodeRef nr: rs.getNodeRefs()) {
						imgReader = contentService.getReader(nr, ContentModel.PROP_CONTENT);
						imgReader.getContent(imgFile);		
						break;
					}
				}
			}
		}
		
		// do transformation then put result into writer
		String targetFilename = doTransformation(srcFile);
		File targetFile = new File(targetFilename);
		if (targetFile.exists()) {
			writer.putContent(targetFile);
		} else {
			logger.error("Did not create the PDF file\n");
		}
		
		// clean up (don't worry about images, as we can "cache" them)
//		srcFile.delete();
//		targetFile.delete();
	}

	/**
	 * Utility for unzipping a file
	 * 
	 * @param zipFile	File to unzip
	 * @return			true if success
	 */
	private boolean unzip(File zipFile) {
		RuntimeExec re = new RuntimeExec();
		List<String> command = new ArrayList<String>();
		
		re.setProcessDirectory(TempFileProvider.getTempDir().getAbsolutePath());
		
		command.add("unzip");
		command.add(zipFile.getName());
		
		re.setCommand(list2Array(command));
		ExecutionResult result = re.execute();
		
		if (!result.getSuccess()) {
			logger.error("Unzip failed\n");
			return false;
		}
		
		return true;
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
	 * Helper to execute the command using RuntimeExec
	 * 
	 * @param srcFile	File to transform
	 * @return 			Absolute path of the generated file
	 */
	private String doTransformation(File srcFile) {
		RuntimeExec re = new RuntimeExec();
		List<String> command = new ArrayList<String>();
		
		String source = srcFile.getAbsolutePath();
		String target = source.substring(0, source.indexOf(".")) + ".pdf";
		command.add(path + fop);
		command.add("-xml");
		command.add(source);
		command.add("-xsl");
		command.add(path + xsl);
		command.add("-pdf");
		command.add(target);

		if (Debug.isOn()) System.out.println("DO_TRANSFORM source: " + source);
		if (Debug.isOn()) System.out.println("DO_TRANSFORM target: " + target);
		if (Debug.isOn()) System.out.println("DO_TRANSFROM cmd: " + command);
		
		re.setCommand(list2Array(command));
		ExecutionResult result = re.execute();

		if (!result.getSuccess()) {
			logger.error("FOP transformation command unsuccessful\n"); 
		}
		
		return target;
	}
	
	/**
	 * Utility function to find any image references inside of the DocBook
	 * 
	 * Currently format in DocBook of images is an imagedata tag with an ../images/{filename} value
	 * 
	 * @param dbFile	DocBook file to search in
	 * @return			List of images found
	 */
	public List<String> findImages(File dbFile) {
		List<String> images = new ArrayList<String>();
		BufferedReader in;
		Pattern pattern = Pattern.compile("images/(.*?)\""); 
		Matcher matcher;
		
		try {
			in = new BufferedReader(new FileReader(dbFile));
			while (in.ready()) {
				String line = in.readLine();
				matcher = pattern.matcher(line);
				while (matcher.find()) {
					images.add(matcher.group(1));
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return images;
	}

	
	/**
	 * Utility function to find all the NodeRefs for the specified name
	 * @param name
	 * @return
	 */
	private ResultSet findNodeRef(String name) {
	    String pattern = NodeUtil.SearchType.CM_NAME + name + "\"";
		ResultSet query = NodeUtil.luceneSearch( pattern, searchService);
		return query;
	}
}
