<import resource="classpath:alfresco/extension/js/artifact_utils.js">

// lets get all the arguments
var path = "";
var extension = "";
var cs = null;
var filename = null;

main();

/**
 * Main function for executing logic
 */
function main() {
	var matchNode = undefined;
	
	//return not-found if file isn't matched
	status.code=404;
	
	//Query parameters on URL as exposed as args dictionary)
	extension = getExtension(args);
	
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
		matchNode = searchForFile(filename);
		if (checkCs(matchNode, cs)) {
			status.code = 200;
		}
	} else {
		path += "Artifacts/";
		for (var ii = 0; ii < tokens.length - 1; ii++) {
			path += tokens[ii] + "/";
		}
		filename = tokens[ii];
		if (extension != null) {
			filename += extension;
		}
		matchNode = companyhome.childByNamePath(path + filename);
		if (matchNode != null && checkCs(matchNode, cs)) {
			status.code = 200;
		}
	}
	
	if (status.code == 200) {
	  status.message = "File " + filename + " found";
	  model.link = matchNode.getUrl();
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
 * Utility function for checking whether checksums match
 */
function checkCs(node, cs) {
	if (cs == null || node.properties.cs == cs) {
		return true;
	} else {
		return false;
	}
}
