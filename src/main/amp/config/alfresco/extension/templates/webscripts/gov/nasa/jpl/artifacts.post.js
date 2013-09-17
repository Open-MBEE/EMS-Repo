<import resource="classpath:alfresco/extension/js/artifact_utils.js">

// everything is relative to Sites path
var path = "Artifacts/";
var extension = "";
var cs = null;
var filename = undefined;
var content = undefined;

main();

function main() {
    //Query parameters on URL as exposed as args dictionary)
    if ("extension" in args) {
        if (args.extension.charAt(0) != ".") {
            extension = ".";
        }
      extension +=  args.extension;
    }
    if ("cs" in args) {
      cs = args.cs;
    }
    for each (field in formdata.fields) {
        if (field.name == "content" && field.isFile) {
            content = field.content;
        }
    }
    
    //Template Args exposes URL template parameters (e.g., template Args)
    var tokens = url.templateArgs.path.split("/");
    if (tokens.length == 1) {
        filename = tokens[0];
        if (extension != null) {
            filename += extension;
        }
    } else {
        for (var ii = 0; ii < tokens.length - 1; ii++) {
            path += tokens[ii] + "/";
        }
        filename = tokens[ii];
        if (extension != null) {
            filename += extension;
        }
    }
    
    // ensure mandatory file attributes have been located
    if (filename == undefined || content == undefined)
    {
        status.code = 400;
        status.message = "Uploaded file cannot be located in request";
        status.redirect = true;
    }
    else
    {
        upload = companyhome.childByNamePath(path + filename);
        if (upload == null) {
            var node = mkdir(path);
            upload = node.createFile(filename) ;
        }

        if (!upload.hasAspect("cm:versionable")) {
            upload.addAspect("cm:versionable");
        }
        if (!upload.hasAspect("view:Checksummable")) {
            upload.addAspect("view:Checksummable");
        }
        upload.properties.cs = cs;
        
        upload.properties.content.write(content);
        upload.properties.content.setEncoding("UTF-8");
//      upload.properties.content.guessMimetype(filename);
        // TODO: figure out why built in guessMimetype doesn't work
        upload.properties.content.mimetype = guessMimetype(filename);
      
        upload.properties.title = filename;
        upload.save();
        
        // if only version, save dummy version so snapshots can reference
        // versioned images
        if (upload.getVersionHistory().length <= 0) {
            upload.createVersion('creating the version history', false);
        }
     
        // setup model for response template
        model.upload = upload;
        status.code = 200;
    }
}

/**
 * Utility for making path if it doesn't exist
 */
function mkdir(path) 
{
    if (path.charAt(path.length) == "/") {
        path = path.substr(0, path.length-1);
    }
    var tokens = path.split("/");
    var curPath = "";
    var prevPath = "";
    for (key in tokens)
    {
      var relPath = tokens[key];
      prevPath = curPath;
      curPath += relPath + "/";

      var folder = companyhome.childByNamePath(curPath);
      if (folder == null) 
      {
        var parentFolder = companyhome.childByNamePath(prevPath);
        if (parentFolder == null) {
            parentFolder = companyhome;
        }
        var newFolder = parentFolder.createFolder(relPath);
        newFolder.save();
        folder = newFolder;
      }
    }
    
    return folder;
}
