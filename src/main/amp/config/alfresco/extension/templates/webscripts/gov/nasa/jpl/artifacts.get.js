// lets get all the arguments
var id = null;
var extension = null;
var cs = null;
var filename = null;

// Template Args exposes URL template paraemters (e.g., template Args)
id = url.templateArgs.id;
filename = id;

// Query parameters on URL as exposed as args dictionary)
if ("extension" in args) {
  extension = args.extension;
  filename += "." + extension;
}
if ("cs" in args) {
  cs = args.cs;
}

// check for name matches
var searchString = "@cm\\:name:" + filename;

// return not-found if file isn't matched
status.code=404;
var results = search.luceneSearch(searchString);
if (results.length > 0) {
  for (result in results) {
    var r = results[result];
    if (cs) {
        if (r.properties["view:cs"] == cs) {
           status.code = 200;
           break;
        }
     } else {
        status.code = 200;
        break;
    }
  }
}

if (status.code == 200) {
  status.message = "File " + filename + " found";
} else {
  if (cs) {
    status.message = "File " + filename + " with cs=" + cs + " not found";
  } else {
    status.message = "File " + filename + " not found";
  }
}

status.redirect = true;
