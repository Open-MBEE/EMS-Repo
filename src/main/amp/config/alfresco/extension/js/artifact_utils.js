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
 * Utility for replacing image references with URLs in alfresco
 * @param content
 */
function replaceArtifactUrl(content) {
	var prefix = '/editor/images/docgen/'
	var pattern= /\/editor\/images\/docgen\/.*?"/g;

    var matches = content.match(pattern);
	for (ii in matches) {
		var match = matches[ii];
		var filename = match.replace(prefix,'').replace('"','');
		var node = searchForFile(filename);
		if (node != null) {
			content = content.replace(match, url.serviceContext + node.getUrl());
		}
	}
	
	return content;
}

/**
 * Utility function for finding a the node for a specific file - need to qualify
 * this somehow
 */
function searchForFile(filename) {
	// check for name matches
	var searchString = "@cm\\:name:" + filename;
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
