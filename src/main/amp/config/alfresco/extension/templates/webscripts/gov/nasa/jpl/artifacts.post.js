var PATH = "path";

var filename = undefined;
var content = undefined;
var title = "";
var description = "";
var path = "";


// locate file attributes from form
for each (field in formdata.fields)
{
  if (field.name == "title")
  {
	  title = field.value;
  }
  else if (field.name == "desc")
  {
	  description = field.value;
  }
  else if (field.name == "file" && field.isFile)
  {
	  filename = field.filename;
	  content = field.content;
  }
  else if (field.name = PATH) 
  {
	  path = field.path;
  } 
}
if (PATH in args) 
{
	path = args.path;
}
// lets make sure path ends with "/"
var pathTokens = path.split("/");
path = "";
for (key in pathTokens) {
	path += pathTokens[key] + "/";
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
    
	upload.properties.content.write(content);
	upload.properties.content.setEncoding("UTF-8");
	upload.properties.content.guessMimetype(filename);
  
	upload.properties.title = title;
	upload.properties.description = description;
	upload.save();
 
	// setup model for response template
	model.upload = upload;
	status.code = 200;
}

function mkdir(path) 
{
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
        var newFolder = parentFolder.createFolder(relPath);
        newFolder.save();
        folder = newFolder;
      }
    }
    
    return folder;
}