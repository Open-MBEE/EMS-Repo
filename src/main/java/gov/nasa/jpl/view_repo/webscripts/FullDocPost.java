package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.docbook.model.DBImage;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.sysml.View;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
    protected String coverPath;//NEED FOR COVER
    protected String footerPath;
    protected String headerPath;
    protected String veCssDir;//NEED FOR COVER
    protected String zipPath;
    protected Date time;
    protected JSONArray view2view;
    protected String timeTagName;
    protected String siteName;
    protected String pageLegalNotice = "The technical data in this document is controlled under the U.S. Export Regulations; release to foreign persons may require an export authorization. Pre-Decisional Information --- For Planning and Discussion Purposes Only.";
//    protected Queue queue;
    static Logger logger = Logger.getLogger(FullDocPost.class);

    // For transactions:
    private String storeName, nodeId, filename;
    public boolean allViewsFailed = true;
    public boolean allViewsSucceeded = true;
	
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
        model.put("res", "fullDocGen");
        return model;
    }
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return false;
    }

    protected void buildHtmlFromViews(String workspaceName, String site, String docId, String timestamp) throws Exception {
//    	this.queue = new LinkedList<String>();
    	WorkspaceNode workspace = WorkspaceNode.getWorkspaceFromId(workspaceName, this.services, this.response, this.responseStatus, null);
    	Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
    	
    	EmsScriptNode document = findScriptNodeById( docId, workspace, dateTime, true );
    	if(document == null) throw new Exception(String.format("Document %s does not exist.", docId));
    	
    	View documentView = document.getView();
    	if(documentView == null) throw new Exception("Missing document's structure; expected to find product's view but it's not found.");
    	
//    	JSONArray contains = documentView.getContainsJson(dateTime, workspace);
//        if(contains == null || contains.length()==0){ throw new Exception("Missing document's structure; expected to find document's 'contains' JSONArray but it's not found."); }

//        for(int i=0; i < contains.length(); i++){
//            JSONObject contain = contains.getJSONObject(i);
//            if(contain == null) throw new Exception(String.format("Missing document's structure; expected to find contain JSONObject at index: %d but it's not found.", i));

//            String source = (String)contain.opt("source");
//            if(source == null || source.isEmpty()) throw new Exception("Missing document's structure; expected to find contain source property but it's not found.");

            this.view2view = documentView.getViewToViewPropertyJson();
            if(view2view == null || view2view.length()==0) throw new Exception ("Missing document's structure; expected to find document's 'view2view' JSONArray but it's not found.");

            downloadView(workspace, site, docId, docId, "", timestamp);
//        }

        joinViews(docId);
    }
    
    protected void joinViews(String docId) throws Exception{
    	File htmlFile = new File(this.htmlPath);
    	String filePath = String.format("%s/%s.html", this.fullDocDir, docId);
    	File file = new File(filePath);
    	//opens the initial document view
    	Document document = Jsoup.parse(file, "UTF-8", "");
    	document.head().append(getTableVerticalSplitScript());
    	//retrieves its views
    	JSONObject v2vChildNode = getChildrenViews(docId);
    	JSONArray childrenViews = v2vChildNode.getJSONArray("childrenViews");
        if(childrenViews == null) throw new Exception("Missing document's structure; expected to find 'view2view' childnode's 'childrenViews' but it's not found.");
        
        for(int j=0; j< childrenViews.length(); j++){
        	String childId = childrenViews.getString(j);
        	if(childId == null || childId.isEmpty()) {
        	    Debug.error(true, false, String.format("Missing document's structure; expected to find childrenViews[%d] Id but it's not found.", j));
        	    continue;
        	}
        	StringBuilder html = new StringBuilder();
        	getViewFromHtmlFile(html, childId);
        	document.body().append(html.toString());
        }
    	
        BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile));
        bw.write(document.html());
        bw.close();
        file.delete();
    }
    
    protected void getViewFromHtmlFile(StringBuilder html, String viewId) throws Exception{
    	String filePath = String.format("%s/%s.html", this.fullDocDir, viewId);
    	File file = new File(filePath);
    	if(!file.exists()){
    		throw new Exception(String.format("HTML file %s does not exist.", filePath));
    	}
    	
    	Document document = Jsoup.parse(file, "UTF-8", "");
    	if(document == null) throw new Exception("Failed to get view from HTML file! Unabled to load file: " + filePath);

//    	html.append("<div style='page-break-after: always;'></div>");
    	html.append(document.body().html());
    	file.delete();
    	
    	JSONObject v2vChildNode = getChildrenViews(viewId);
    	if (v2vChildNode == null) {
    	    Debug.error(true, false, String.format("Missing document's structure; expected to find 'view2view' childnode for: %s but it's not found.", viewId));
            return;
    	}
    	JSONArray childrenViews = v2vChildNode.getJSONArray("childrenViews");
        if(childrenViews == null) throw new Exception("Missing document's structure; expected to find 'view2view' childnode's 'childrenViews' but it's not found.");
        
        for(int j=0; j< childrenViews.length(); j++){
        	String childId = childrenViews.getString(j);
        	if(childId == null || childId.isEmpty()) {
        	    Debug.error(true, false, String.format("Missing document's structure; expected to find childrenViews[%d] Id but it's not found.", j));
        	    continue;
        	}
        	getViewFromHtmlFile(html, childId);
        }
    }
    
    protected String getTableVerticalSplitScript(){
    	StringBuilder script = new StringBuilder();
    	script.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js\"></script>");
    	script.append("<script type=\"text/javascript\">\n");
    	script.append("/**\n");
    	script.append("* WkHtmlToPdf table vertically-splitting hack\n");
    	script.append("* Script to automatically split wide HTML tables that doesn't fit the width of the PDF page generated\n");
		script.append("* by WkHtmlToPdf (or equivalent)\n");
		script.append("*\n");
		script.append("* The general idea come from Florin Stancu <niflostancu@gmail.com> and his script wkhtmltopdf.tablesplit.js\n");
		script.append("* The implementation is quite different because the splitting is done vertically on a excessive\n");
		script.append("* wide table, while the original script was meant to split horizontally an excessive long table\n");
		script.append("*\n");
		script.append("* To use, you must adjust pdfPage object's contents to reflect your PDF's\n");
		script.append("* page format.\n");
		script.append("* The tables you want to be automatically splitted when the page ends must\n");
		script.append("* have the same class name as specified by the variable \"verticalTableSplit_ClassName\": if not set,\n");
		script.append("* all tables will be checked for split.\n");
		script.append("* Also, is possible to have a left vertical header repeating in all table slices: columns of this\n");
		script.append("* vertical header must have td elements with the same class name as specified by the variable\n");
		script.append("* \"verticalTableSplit_leftHeaderClassName\"\n");
		script.append("*\n");
		script.append("* Live demo: http://jsfiddle.net/mU2Ne/\n");
		script.append("* GitHub: https://github.com/vstefanoxx/JSUtils/tree/master/TableVerticalSplitHack\n");
		script.append("* Gist: https://gist.github.com/vstefanoxx/574aa61eaf2cc91dd9c9\n");
		script.append("*\n");
		script.append("* Dependencies: jQuery.\n");
		script.append("*\n");
		script.append("* From original script (and some others forks and hacks) I took some variable name and, as I said,\n");
		script.append("* the original idea\n");
		script.append("*\n");
		script.append("* @author Stefano Vargiu <vstefanoxx@gmail.com>\n");
		script.append("* @license http://www.opensource.org/licenses/mit-license.php MIT License\n");
		script.append("*/\n");

		script.append("/**\n");
		script.append(" * pdfPage, verticalTableSplit_ClassName, verticalTableSplit_leftHeaderClassName\n");
		script.append(" * You can overwrite these parameters in the page from where you are loading this script\n\n");
		script.append(" * (and after the point where you loaded it) so you can set pdfPage in one place and use,\n\n");
		script.append(" * if you need it, both this script and the one that split horizontally the table\n\n");
		script.append(" * (wkhtmltopdf.tablesplit.js on GitHub or his forks, see the disclaimer on top)\n\n");
		script.append(" */\n\n");
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
    
    private void downloadHtmlImpl(String workspaceName, String site, String docId, String timestamp, String tagTitle) throws Exception {
//        RuntimeExec exec = new RuntimeExec();
//        Date d = TimeUtils.dateFromTimestamp( timestamp );
//        this.setTime(d);
//        
//        this.setTimeTagName(tagTitle);
//        
//        HostnameGet alfresco = new HostnameGet(this.repository, this.services);
//        String protocol = alfresco.getAlfrescoProtocol();
//        String hostnameAndPort = this.getAlfrescoHost();
//        String preRendererUrl = "http://localhost";
//        int preRendererPort = 3000;
//        String mmsAdminCredential = getHeadlessUserCredential();
//        List<String> command = new ArrayList<String>();
//        command.add(this.phantomJSPath);
//        command.add(this.phantomJSScriptPath);
//        command.add(String.format("%s:%d/%s://%s@%s/mmsFullDoc.html?ws=%s&site=%s&docId=%s&time=%s",
//                preRendererUrl,preRendererPort, protocol, mmsAdminCredential, hostnameAndPort, workspaceName, site, docId, timestamp));
//        command.add(String.format("%s/%s_NodeJS.html", this.fullDocDir, this.fullDocId));
//        exec.setCommand(list2Array(command));
//        System.out.println("NodeJS command: " + command);
//        int attempts = 0;
//        boolean isSuccess = false;
//        while(!isSuccess && attempts < 3){
//            ExecutionResult result = exec.execute();
//            if (!result.getSuccess()) {
//                String msg = String.format("Failed to download full doc HTML for %s. Exit code: %d. Attempt #%d.", this.fullDocId, result.getExitValue(), attempts+1);
//                log(Level.WARN, msg);
//            }
//            else{ 
//                if(Files.exists(Paths.get(this.htmlPath))) isSuccess = true;
//            }
//            attempts++;
//        }
//        
//        if(!isSuccess){
//            String msg = String.format("Failed to download full doc HTML for %s.", this.fullDocId);
//            log(Level.WARN, msg);
//            try{
//                log(Level.INFO, "Start downloading HTML views...");
//                buildHtmlFromViews(workspaceName, site, docId, timestamp);
//            }
//            catch(Exception ex){
//                throw ex;
//            }
//        }
//        
//        try{
//            tableToCSV();
//        }
//        catch(Exception ex){
//            throw new Exception("Failed to convert tables to CSV files!", ex);
//        }
        
        Date d = TimeUtils.dateFromTimestamp( timestamp );
        this.setTime(d);
        this.setTimeTagName(tagTitle);
        this.setSiteName(site);

        try{
            log(Level.INFO, "Start downloading HTML views...");
            buildHtmlFromViews(workspaceName, site, docId, timestamp);
        }
        catch(Exception ex){
        	logger.error("Failed to build HTML from stiching views! " + ex.getMessage());
            throw ex;
        }
        
        try{
            tableToCSV();
        }
        catch(Exception ex){
        	logger.error("Failed to convert tables to CSV files! " + ex.getMessage());
            throw new Exception("Failed to convert tables to CSV files!", ex);
        }
    }
    
    public void downloadHtml(final String workspaceName, final String site, final String docId, final String timestamp, final String tagTitle) throws Exception {
    	
        new EmsTransaction(services, response, new Status()) {
            @Override
            public void run() throws Exception {
                downloadHtmlImpl(workspaceName, site, docId, timestamp, tagTitle);
            }
        };
        
        // handleEmbeddedImage() Will have its own transactions, handleRelativeHyperlinks() doesnt need it:
        try{
        	response.append("\n[INFO]: Copying CSS files...");
            FileUtils.copyDirectory(new File(this.veCssDir), new File(Paths.get(this.fullDocDir, "css").toString()));
            response.append("\n[INFO]: Downloading embedded imagess...");
            handleEmbeddedImage();
            response.append("\n[INFO]: Formatting relative hyper-links...");
            handleRelativeHyperlinks();
        }
        catch(Exception ex){
        	response.append("\n[ERROR]: Post-HTML download processing failed! " + ex.getMessage());
            throw ex;
        }
    }
    
    private void downloadView(WorkspaceNode workspace, String site, String docId, String viewId, String section, String timestamp) throws Exception{
    	downloadViewWorker(workspace, site, docId, viewId, section, timestamp);
    	
    	JSONObject v2vChildNode = getChildrenViews(viewId);
        if(v2vChildNode == null) {
            Debug.error(true, false, String.format("Missing document's structure; expected to find 'view2view' childnode for: %s but it's not found.", viewId));
            return;
        }

        JSONArray childrenViews = v2vChildNode.getJSONArray("childrenViews");
        if(childrenViews == null) throw new Exception("Missing document's structure; expected to find 'view2view' childnode's 'childrenViews' but it's not found.");
        
        for(int j=0; j< childrenViews.length(); j++){
        	String childId = childrenViews.getString(j);
        	if(childId == null || childId.isEmpty()) {
        	    Debug.error(true, false, String.format("Missing document's structure; expected to find childrenViews[%d] Id but it's not found.", j));
        	    continue;
        	}

        	EmsScriptNode childNode = findScriptNodeById(childId, workspace, TimeUtils.dateFromTimestamp( timestamp ), false);
        	if(childNode == null) throw new Exception(String.format("Failed to find EmsScriptNode with Id: %s", childId));
        	String subSection = (section!=null && !section.isEmpty()) ? String.format("%s.%d", section, j+1) : String.valueOf(j+1);
        	downloadView(workspace, site, docId, childId, subSection, timestamp);
        	//creating chapters
        	//DocumentElement section = emsScriptNodeToDBSection(childNode, true, workspace, TimeUtils.dateFromTimestamp( timestamp ));
        	//if(section != null) docBook.addElement(section);
        }
    }
    
    private void downloadViewWorker(WorkspaceNode workspace, String site, String docId, String viewId, String section, String timestamp) throws Exception{
    	String filePath = String.format("%s/%s.html", this.fullDocDir, viewId);
    	if(Files.exists(Paths.get(filePath))) return;
    	
    	RuntimeExec exec = new RuntimeExec();
    	HostnameGet alfresco = new HostnameGet(this.repository, this.services);
		String protocol = alfresco.getAlfrescoProtocol();
//		String hostname = alfresco.getAlfrescoHost();
		String hostnameAndPort = this.getAlfrescoHost();
		String preRendererUrl = "http://localhost";
		int preRendererPort = 3000;
		String mmsAdminCredential = getHeadlessUserCredential();
//		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
//		String filePath = String.format("%s/%s.html", this.fullDocDir, viewId);
		
		List<String> command = new ArrayList<String>();
		command.add(this.phantomJSPath);
		command.add(this.phantomJSScriptPath);
		// command.add(String.format("%s:%d/%s://%s@%s/mmsFullDoc.html#/workspaces/%s/sites/%s/documents/%s/views/%s",
		// preRendererUrl,preRendererPort, protocol, mmsAdminCredential, hostnameAndPort, workspace.getName(), site, docId, viewId));
		command.add(String.format("%s:%d/%s://%s@%s/mmsFullDoc.html?ws=%s&site=%s&docId=%s&viewId=%s&section=%s&time=%s",
		preRendererUrl,preRendererPort, protocol, mmsAdminCredential, hostnameAndPort, workspace == null ? "master" : workspace.getName(), site, docId, viewId, section, timestamp));
		command.add(filePath);
		exec.setCommand(list2Array(command));
		System.out.println("NodeJS command: " + command);
		int attempts = 0;
		boolean isSuccess = false;
		while(!isSuccess && attempts < 3){
			ExecutionResult result = exec.execute();
			if (!result.getSuccess()) {
				String msg = String.format("Failed to download view for %s. Exit code: %d. Attempt #%d.", viewId, result.getExitValue(), attempts+1);
				log(Level.WARN, msg);
			}
			else{ 
				if(Files.exists(Paths.get(filePath))) isSuccess = true;
			}
			attempts++;
		}
		
		if(isSuccess){
		    allViewsFailed  = false;
		} else {
		    allViewsSucceeded  = false;
		    String errorHtml = "<html><body><div>Error: The html for the view with id = " + viewId + " failed to generate.</div></body></html>";
            gov.nasa.jpl.mbee.util.FileUtils.stringToFile( errorHtml , filePath );
			String msg = String.format("Failed to download view for %s.", viewId);
			log(Level.ERROR, msg);
			logger.error(msg);
		}
    }
    
    private String getAlfrescoHost(){
    	HostnameGet alfresco = new HostnameGet(this.repository, this.services);
    	String hostname = alfresco.getAlfrescoHost();
    	if(hostname.compareToIgnoreCase("localhost")==0){
    		hostname += ":9000";
    	}
    	else{
    		hostname += "/alfresco/mmsapp";
    	}
    	return hostname;
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

    private String getHostname(){
        	SysAdminParams sysAdminParams = this.services.getSysAdminParams();
        	String hostname = sysAdminParams.getAlfrescoHost();
        	if(hostname.startsWith("ip-128-149")) hostname = "localhost";
        	return String.format("%s://%s", sysAdminParams.getAlfrescoProtocol(), hostname);
    }

	public String getHtmlPath(){
		return this.htmlPath;
	}
    //NEED FOR COVER
    public String getCoverPath(){
        return this.coverPath;
    }
    
    private String getFooterPath(){
    	return this.footerPath;
    }
    
    private String getTempDir(){
    	String alfDataDir = Paths.get("/mnt/alf_data").toString();
    	String tmpDirName = Paths.get(alfDataDir, "temp").toString();
        if(gov.nasa.jpl.mbee.util.FileUtils.exists(alfDataDir)){ //EMS server
        	if(!gov.nasa.jpl.mbee.util.FileUtils.exists(tmpDirName)){
        		new File(tmpDirName).mkdirs();
        	}
        }
        else{//local dev
        	tmpDirName = TempFileProvider.getTempDir().getAbsolutePath();
        }
        return tmpDirName;
    }
    
  //NEED FOR COVER
    private Date getTime(){
        return this.time;
    }
    //NEED FOR COVER
    private String getTimeTagName(){
        return this.timeTagName;
    }

    public void cleanupFiles(){
    	if(gov.nasa.jpl.mbee.util.FileUtils.exists(this.fullDocDir)){
    		try{
    			FileUtils.forceDelete(new File(this.fullDocDir));
    		}
    		catch(IOException ex){
				System.out.println(String.format("Failed to cleanup temporary files at %s", this.fullDocDir));
    		}
    	}

		if(gov.nasa.jpl.mbee.util.FileUtils.exists(this.zipPath)){
    		try{
    			FileUtils.forceDelete(new File(this.zipPath));
    		}
    		catch(IOException ex){
    			System.out.println(String.format("Failed to cleanup temporary file %s", this.zipPath));
    		}
		}
    }
    
    //NEED FOR COVER
    public void createCoverPage(String coverDestination) throws IOException{
    	if(!Files.exists(Paths.get(this.htmlPath))) return;

        File htmlFile = new File(this.htmlPath);
        File coverFile = new File (coverDestination);
        Document document = Jsoup.parse(htmlFile, "UTF-8", "");
        if(document == null) return;
        ArrayList<String> l  = new ArrayList<String>();
        
        Elements headers = document.getElementsByTag("mms-transclude-name");
        for(Element header :headers){
            l.add(header.text());
        }
        String coverHeader = this.fullDocId;
        if(!l.isEmpty()){
        coverHeader = l.get(0);
        }
        String legalNotice = "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.";
        String pageLegalNotice = "This Document has not been reviewed for export control. Not for distribution to or access by foreign persons.";
        String jplName = "Jet Propulsion Laboratory";
        String caltechName = "California Institute of Technology";
        Date date = this.getTime();
        String tag = this.getTimeTagName();
                            
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
                    +   "<div>" + date + "<br/>" + tag +  "</div>" //"<div>" + date + "</div>" 
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
    
//    private void createFooterPage(String footerDestination) throws IOException{
//    	File footerFile = new File(footerDestination);
//    	StringBuffer html = new StringBuffer();
//    	html.append("<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
//        html.append("<html><head><title></title>");
//        html.append("<style type=\"text/css\">");
//        html.append("BODY{font-family:\"Times New Roman\";}");
//        html.append(".itar{text-align:center;font-size:8pt;font-style:italic;}");
//        html.append(".page{text-align:center;}");
//        html.append("</style>");
//        html.append("<script type=\"text/javascript\">");
//        html.append("function subst() {");
//        html.append("var vars={};");
//        html.append("var x=document.location.search.substring(1).split('&');");
//        html.append("for(var i in x) {var z=x[i].split('=',2);vars[z[0]] = unescape(z[1]);}");
//        html.append("var x=['frompage','topage','page','webpage','section','subsection','subsubsection'];");
//        html.append("for(var i in x) {");
//        html.append("var y = document.getElementsByClassName(x[i]);");
//        html.append("for(var j=0; j<y.length; ++j) y[j].textContent = vars[x[i]];");
//        html.append("}");
//        html.append("}");
//        html.append("</script>");
//        html.append("</head>");
//     	html.append("<body onload=\"subst()\">");
// 		html.append("<div class=\"page\"></div>");
// 		html.append("<div class=\"itar\">");
// 		html.append(this.pageLegalNotice);
// 		html.append("</div>");
// 		html.append("</body></html>");
// 		BufferedWriter bw = new BufferedWriter(new FileWriter(footerFile));
//        bw.write(html.toString());
//        bw.close();
//    }
    
//    private void createHeaderPage(String headerDestination) throws IOException{
//    	File headerFile = new File(headerDestination);
//    	StringBuffer html = new StringBuffer();
//    	html.append("<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
//    	html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
//    	html.append("<head>");
//    	html.append("<title></title>");
//    	html.append("<style type=\"text/css\">");
//    	html.append("BODY{");
//    	html.append("font-family:\"Times New Roman\";");
//    	html.append("}");
//    	html.append(".left{");
//    	html.append("overflow:auto;");
//    	html.append("float:left;");
//    	html.append("}");
//    	html.append(".right{");
//    	html.append("overflow:auto;");
//    	html.append("float:right;");
//    	html.append("}");
//    	html.append("</style>");
//    	html.append("</head>");
//    	html.append("<body>");
//    	html.append("<div>");
//    	html.append("<div class=\"left\">");
//    	html.append("<div>JPL D-92259</div>");
//    	html.append("<div>Initial Release</div>");
//    	html.append("</div>");
//    	html.append("<div class=\"right\">");
//    	html.append(this.getTime());
//    	html.append("</div>");
//    	html.append("</div>");
//    	html.append("</body>");
//    	html.append("</html>");
//    	BufferedWriter bw = new BufferedWriter(new FileWriter(headerFile));
//        bw.write(html.toString());
//        bw.close();
//    }
    
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
    
	public String getPdfPath(){
		return this.pdfPath;
	}
    
    public void handleEmbeddedImage() throws Exception
    {
    	if(!Files.exists(Paths.get(this.htmlPath))) return;

    	File htmlFile = new File(this.htmlPath);
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
                                    DBImage dbImage = retrieveEmbeddedImage(storeName, nodeId, filename, null, null);
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
    		log(Level.ERROR, "Failed to save modified HTML %s. %s", this.htmlPath, ex.getMessage());
        	ex.printStackTrace();
    	}
    }
    
    protected void handleRelativeHyperlinks() throws Exception{
    	if(!Files.exists(Paths.get(this.htmlPath))) return;

    	File htmlFile = new File(this.htmlPath);
    	
    	Document document;
    	try{
    		document = Jsoup.parse(htmlFile, "UTF-8", "");
    	}
    	catch(Exception ex){
    		log(Level.ERROR, "Failed to load HTML file '%s' to handle relative hyperlinks. %s", this.htmlPath, ex.getMessage());
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
    		log(Level.ERROR, "Failed to save HTML file '%s' after handling relative hyperlinks. %s", this.htmlPath, ex.getMessage());
        	throw ex;
    	}
    }
    
    public void html2pdf(EmsScriptNode snapshotFolder, EmsScriptNode snapshotNode)  throws Exception {
    	if(!Files.exists(Paths.get(this.htmlPath))) throw new Exception(String.format("Failed to transform HTML to PDF. Expected %s HTML file but it does not exist!", this.htmlPath));
    	
    	RuntimeExec exec = new RuntimeExec();
		exec.setProcessDirectory(this.fullDocGenDir);
		createCoverPage(this.coverPath); //NEED FOR COVER
//		createFooterPage(this.footerPath);
//		createHeaderPage(this.headerPath);
		String tagName = this.getTimeTagName();

		List<String> command = new ArrayList<String>();
		command.add("wkhtmltopdf");
		command.add("-q");
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
//		command.add("--footer-left");
//		command.add(tagName.substring(0,10));
		command.add("--footer-center");
		command.add("Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.");  
		command.add("--footer-right");
		command.add("[page]");
//		command.add("--footer-html");
//        command.add(this.footerPath);
		command.add("cover"); //NEED FOR COVER
        command.add(this.coverPath); //NEED FOR COVER
//        command.add("--dump-outline"); 
//		command.add(Paths.get(this.fullDocDir,"toc.xml").toString());
		command.add("toc");
		command.add("--toc-level-indentation");
		command.add("10");
//		command.add("--xsl-style-sheet");
		command.add(Paths.get(this.fullDocGenDir, "wkhtmltopdf/xsl/default.xsl").toString());
//		command.add("--footer-center");
//		command.add("Page [page] of [toPage]");
		command.add(this.getHtmlPath());
		command.add(this.getPdfPath());

		System.out.println("htmltopdf command: " + command);
		exec.setCommand(list2Array(command));
		ExecutionResult result = exec.execute();
		if(!Files.exists(Paths.get(this.pdfPath)))
		{
			String msg = String.format("Failed to transform HTML file '%s' to PDF. Exit value: %d", this.htmlPath, result.getExitValue());
			log(Level.ERROR, msg);
			throw new Exception(msg);
		}
		
		this.savePdfToRepo(snapshotFolder, snapshotNode);
    }

    public boolean isFullDocHtmlExist(){
    	return Files.exists(Paths.get(this.htmlPath));
    }
    
	/**
	 * Helper method to convert a list to an array of specified type
	 * @param list
	 * @return
	 */
	private String[] list2Array(List<String> list) {
		return Arrays.copyOf(list.toArray(), list.toArray().length, String[].class);
	}
    
    private DBImage retrieveEmbeddedImage(String storeName, String nodeId, String imgName, WorkspaceNode workspace, Object timestamp) throws UnsupportedEncodingException{
		NodeRef imgNodeRef = NodeUtil.getNodeRefFromNodeId(storeName, nodeId);
		if(imgNodeRef == null) return null;

		imgName = URLDecoder.decode(imgName, "UTF-8");
		String imgFilename = this.imageDirName + File.separator + imgName;
		
		File imgFile = new File(imgFilename);
		ContentReader imgReader;
		imgReader = this.services.getContentService().getReader(imgNodeRef, ContentModel.PROP_CONTENT);
		if(!Files.exists(this.imageDirName)){
			if(!new File(this.imageDirName.toString()).mkdirs()){
				System.out.println("Failed to create directory for " + this.imageDirName);
			}
		}
		imgReader.getContent(imgFile);

		DBImage image = new DBImage();
		image.setId(nodeId);
		image.setFilePath("images/" + imgName);
		return image;
    }

	public void savePdfToRepo(EmsScriptNode snapshotFolder, EmsScriptNode snapshotNode) throws Exception{
//		ServiceRegistry services = this.snapshotNode.getServices();
    	String filename = String.format("%s.pdf", this.fullDocId);
		try{
            EmsScriptNode node = 
                    NodeUtil.getOrCreateContentNode( snapshotFolder,
                                                     filename,
                                                     snapshotFolder.getServices() );
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
            EmsScriptNode node =
                    NodeUtil.getOrCreateContentNode( snapshotFolder,
                                                     filename,
                                                     snapshotFolder.getServices());
            if(node == null) throw new Exception("Failed to create zip repository node!");


			//createDocBookDir();
			//retrieveDocBook(workspace, timestamp);
//			this.transformToHTML(workspace, timestamp);
//			tableToCSV();
//			String zipPath = this.zipHtml();
//			if(zipPath == null || zipPath.isEmpty()) throw new Exception("Failed to zip files and resources!");
			this.zipHtml();
			
			if(!this.saveFileToRepo(node, MimetypeMap.MIMETYPE_ZIP, this.zipPath)) throw new Exception("Failed to save zip artifact to repository!");
			snapshotNode.createOrUpdateAspect("view2:htmlZip");
			snapshotNode.createOrUpdateProperty("view2:htmlZipNode", node.getNodeRef());

			if ( node != null ) node.getOrSetCachedVersion();
		}
		catch(Exception ex){
			throw new Exception("Failed to generate zip artifact!", ex);
		}
	}
	
	private void setFullDocDir(){
		fullDocDir = Paths.get(getTempDir(), fullDocId).toString();
	}
	
	public void setFullDocId(String id){
		this.fullDocId = id;
		this.setFullDocDir();
		this.setPaths();
	}

	private void setPaths(){
        this.parentPath = Paths.get(this.fullDocDir).getParent().toString();
        //String tmpDirName    = TempFileProvider.getTempDir().getAbsolutePath();
        String tmpDirName = getTempDir();
        this.htmlPath = Paths.get(tmpDirName, fullDocId, String.format("%s_NodeJS.html", fullDocId)).toString();
        this.pdfPath = Paths.get(tmpDirName, fullDocId, String.format("%s_NodeJS.pdf", fullDocId)).toString();
        this.veCssDir = "/opt/local/apache-tomcat/webapps/alfresco/mmsapp/css";
        this.zipPath = String.format("%s/%s.zip", this.parentPath, this.fullDocId);
        this.imgPath = Paths.get(tmpDirName,fullDocId).toString();
        this.imageDirName = Paths.get(imgPath.toString(), "images");
        this.phantomJSPath = "/opt/local/fullDocGen/prerender/node_modules/phantomjs/bin/phantomjs";
        this.phantomJSScriptPath = "/opt/local/fullDocGen/fullDoc.js";
        this.fullDocGenDir = "/opt/local/fullDocGen/";
        this.coverPath = Paths.get(tmpDirName, fullDocId, String.format("%s_cover.html", fullDocId)).toString();
        this.footerPath = Paths.get(tmpDirName, fullDocId, String.format("%s_footer.html", fullDocId)).toString();
        this.headerPath = Paths.get(tmpDirName, fullDocId, String.format("%s_header.html", fullDocId)).toString();
        try{
        	new File(this.imgPath).mkdirs();
        }catch(Exception ex){;}
    }
	
	//NEED FOR COVER
	private void setSiteName(String siteName){
		this.siteName = siteName;
	}
	
    private void setTime(Date t){
        this.time = t;
    }
    
    private void setTimeTagName(String c){
        this.timeTagName = c;
    }
    
    private void tableToCSV() throws Exception{
		File input = new File(this.getHtmlPath());
		if(!input.exists()) return;
		
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
			String msg = String.format("Failed to save table to CSV to file system for %s. %s", outputFile.getAbsoluteFile(), e.getMessage());
			log(Level.ERROR, msg);
			e.printStackTrace();
			throw new Exception(msg);
		}
	}

    public void zipHtml() throws Exception {
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

		//command.add("-x");
		//command.add("*.db");
		//command.add("*.pdf");

		exec.setCommand(list2Array(command));
		System.out.println("zip command: " + command);
		ExecutionResult result = exec.execute();

		if (!result.getSuccess()) {
			String msg = String.format("Failed to zip document generation files for %s. Exit code: %d", this.fullDocId, result.getExitValue());
			log(Level.ERROR, msg);
			throw new Exception(msg);
		}
	}
}
