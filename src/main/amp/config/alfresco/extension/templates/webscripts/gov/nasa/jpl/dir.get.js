
// extract folder listing arguments from URI
var verbose = (args.verbose == "true" ? true : false);
var folderpath = url.templateArgs.folderpath;

// search for folder within Alfresco content repository
var folder = roothome.childByNamePath(folderpath);

// validate that folder has been found
if (folder == undefined || !folder.isContainer) {
   status.code = 404;
   status.message = "Folder " + folderpath + " not found.";
   status.redirect = true;
}

// construct model for response template to render
model.verbose = verbose;
model.folder = folder; 
