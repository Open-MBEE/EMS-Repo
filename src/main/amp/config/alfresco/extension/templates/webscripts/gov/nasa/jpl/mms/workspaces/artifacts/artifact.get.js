<import resource="classpath:alfresco/extension/js/artifact_utils.js">

// lets get all the arguments
var path = "";
var extension = "";
var cs = null;
var filename = null;

/**
 * Main function for executing logic
 */
function main() {
	var matchNode = undefined;
	
	//return not-found if file isn't matched
	status.code=404;
	
	//Query parameters on URL as exposed as args dictionary)
	extension = getExtension(args);
	
	if (extension.length < 1) {
		extension = ".svg";
	}
	
	if ("cs" in args) {
	  cs = args.cs;
	}
	
	var timestamp = null;
	if ("timestamp" in args) {
		offset = args.timestamp.substring(23,29);
		tsString = args.timestamp.substring(0,23) + 'Z'; 
		timestamp = utils.fromISO8601(tsString);
		
		ms = timestamp.getTime();
		
		if (offset[0] == '+' || offset[0] == '-') {
			hrOffset = parseInt(offset.substring(1,3));
			hrOffset = hrOffset * 60 * 60 * 1000;
			
			minOffset = parseInt(offset.substring(3,5));
			minOffset = minOffset * 60 * 1000;
			if (offset[0]=='+') {
				ms = ms - hrOffset - minOffset;
			} else {
				ms = ms + hrOffset + minOffset;
			}
			
			timestamp = new Date(ms);
		}
	}
	
	//Template Args exposes URL template parameters 
	var tokens = url.templateArgs.artifactId.split("/");
	id = tokens[tokens.length-1]
	filename = id + extension;
	matchNode = searchForFile(filename);
	if (matchNode != null && checkCs(matchNode, cs)) {
	  status.code = 200;
	  status.message = "File " + filename + " found";
	  var nodeurl = getVersionedUrl(matchNode, timestamp);
	  model.link = nodeurl.replace("/d/d/", "/service/api/node/content/");
	  model.id = id;
	} else {
	  if (cs) {
	    status.message = "File " + filename + " with cs=" + cs + " not found.";
	  } else {
	    status.message = "File " + filename + " not found";
	  }
	  model.id = 'none';
	  model.link = 'none';
	}
}

/**
 * Utility function to get the versioned url if a timestamp is specified.
 * @param matchNode
 * @param timestamp
 * @returns
 */
function getVersionedUrl(matchNode, timestamp) {
	if (timestamp == null) {
		return matchNode.getUrl();
	}
	
	var versions = matchNode.getVersionHistory();
	for each(var ii = 0; ii < versions.length; ii++) {
		var version = versions[ii];
		if (version.getCreatedDate().compareTo(timestamp) <= 0) {
			return version.getNode().getUrl();
		}
	}
	
	// return latest if nothing is fine
	return matchNode.getUrl();
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


if (UserUtil.hasWebScriptPermissions) {
    main();
} else {
    status.code = 401;
    status.redirect = true;
}