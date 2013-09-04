/**
 * Utility for getting extension from template args with only one leading "."
 * @param args
 * @returns {String}
 */
function getExtension (args) {
	var extension = "";
	if ("extension" in args) {
		if (args.extension.charAt(0) != ".") {
			extension = ".";
		}
	}
	extension += args.extension;
	return extension;
}

/**
 * Utility for replacing all the artifact URLs in alfresco
 * @param content	String whose image references need to be updated
 * @param escape	True if returned string needs special escape characters
 * @returns			String with updated references
 */
function fixArtifactUrls(content, escape) {
	var result = content;
	result = replaceArtifactUrl(result, 'src="/staging/images/docgen/', /src=\"\/staging\/images\/docgen\/.*?"/g, escape);
	result = replaceArtifactUrl(result, 'src="\\/editor\\/images\\/docgen\\/', /src=\"\\\/editor\\\/images\\\/docgen\\\/.*?\\"/g, escape);
	return result;
}

/**
 * Utility for replacing image references with URLs in alfresco
 * @param content	String whose image references need to be updated
 * @param prefix	The string prefix to match for replacement
 * @param pattern	Filename pattern to search for and replace
 * @param escape	True if the returned string needs special escape characters (for use by JSON parser)
 * @returns			String with updated references	
 */
function replaceArtifactUrl(content, prefix, pattern, escape) {
    var matches = content.match(pattern);
	for (ii in matches) {
		var match = matches[ii];
		var filename = match.replace(prefix,'').replace('"','').replace('_latest','');
		var node = searchForFile(filename);
		if (node != null) {
			var nodeurl = 'src="' + url.context + String(node.getUrl()) + '"';
			if (escape) {
				nodeurl = nodeurl.replace(/\//g, '\\\/').replace(/\"/g, '\\"');
			}
			content = content.replace(match, nodeurl);
		}
	}
	
	return content;
}

/**
 * Utility function for finding a the node for a specific file
 * TODO: need to qualify this somehow if there are duplicates
 */
function searchForFile(filename) {
	// check for name matches (make sure to remove any \ in the string (if they were escaped)
	var searchString = "@cm\\:name:" + filename.replace(/\\/g, '');
	var matchNode = undefined;

	var results = search.luceneSearch(searchString);
	if (results.length > 0) {
	  for (result in results) {
	    matchNode = results[result];
	    break;
	  }
	}
	return matchNode;
}


function guessMimetype(filename) {
	var mimetype;
	var mimetypesByExtension = {
		'bin':	'bin',
		'txt':	'text/plain',
		'css':	'text/css',
		'csv':	'text/csv',
		'js':	'text/javascript',
		'xml':	'text/xml',
		'html':	'text/html',
		'xhtml':'application/xhtml+xml',
		'pdf':	'application/pdf',
		'json':	'application/json',
		'docx':	'application/msword',
		'xslx':	'application/vnd.ms-excel',
		'pptx': 'application/vnd.ms-powerpoint',
		'avi':	'video/x-msvideo',
		'wmv':	'video/x-ms-wmv',
		'mpeg':	'video/mpeg',
		'mp4':	'video/mp4',
		'gif':	'image/gif',
		'jpg':	'image/jpeg',
		'jpeg':	'image/jpeg',
		'svg':	'image/svg+xml',
		'png':	'image/png',
		'tiff':	'image/tiff',
		'zip': 	'application/zip',
		'eps':	'application/eps'
	};
	
	
	if (filename != null && filename.length > 0) {
		var index = filename.lastIndexOf('.');
		if (index > -1 && (index < filename.length-1)) {
			var extension = filename.substring(index+1).toLowerCase();
			mimetype = mimetypesByExtension[extension];
		}
	}
	
	if (mimetype == undefined) {
		mimetype = 'application/octet-stream';
	}
	return mimetype;
}