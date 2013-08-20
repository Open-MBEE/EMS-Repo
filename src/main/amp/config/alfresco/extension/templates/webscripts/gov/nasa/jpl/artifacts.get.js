// lets get all the arguments
var path = "";
var extension = null;
var cs = null;
var filename = null;

main();

/**
 * Main function for executing logic
 */
function main() {
	//return not-found if file isn't matched
	status.code=404;
	
	//Query parameters on URL as exposed as args dictionary)
	if ("extension" in args) {
	  extension = "." + args.extension;
	}
	if ("cs" in args) {
	  cs = args.cs;
	}
	
	//Template Args exposes URL template parameters (e.g., template Args)
	var tokens = url.templateArgs.path.split("/");
	if (tokens.length == 1) {
		filename = tokens[0];
		if (extension != null) {
			filename += extension;
		}
		searchForMatches(filename);
	} else {
		path += "Sites/";
		for (var ii = 0; ii < tokens.length - 1; ii++) {
			path += tokens[ii] + "/";
		}
		filename = tokens[ii];
		if (extension != null) {
			filename += extension;
		}
		var node = companyhome.childByNamePath(path + filename);
		if (node != null && checkCs(node, cs)) {
			status.code = 200;
		}
	}
	
	if (status.code == 200) {
	  status.message = "File " + filename + " found";
	} else {
	  if (cs) {
	    status.message = "File " + filename + " with cs=" + cs + " not found.";
	  } else {
	    status.message = "File " + filename + " not found";
	  }
	}
	
	status.redirect = true;
}

/**
 * Utility function for global filename matching
 */
function searchForMatches(filename) {
	// check for name matches
	var searchString = "@cm\\:name:" + filename;

	var results = search.luceneSearch(searchString);
	if (results.length > 0) {
	  for (result in results) {
	    var r = results[result];
	    if (checkCs(r, cs)) {
	    	status.code = 200;
	    	break;
	    }
	  }
	}
}

/**
 * Utility function for checking whether checksums match
 */
function checkCs(node, cs) {
	if (cs == null || node.properties.cs == cs) {
		return true;
	} else {
		return false;
	}
}
