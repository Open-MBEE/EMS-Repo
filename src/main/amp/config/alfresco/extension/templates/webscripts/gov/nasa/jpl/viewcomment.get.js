<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");

var viewid = url.templateArgs.viewid;
var recurse = args.recurse == 'true' ? true : false;

var view2comment = {};
var comments = [];

function handleView(viewnode) {
	var commentids = [];
	var viewcomments = viewnode.assocs["view:comments"];;
	for (var i in viewcomments) {
		var commentNode = viewcomments[i];
		if (commentNode.properties["view:committed"])
			continue;
		commentids.push(commentNode.properties["view:mdid"]);
		var commentdetail = {
				"id": commentNode.properties["view:mdid"], 
				"body": commentNode.properties["view:documentation"],
				"deleted": commentNode.properties["view:deleted"],
				"author": commentNode.properties["view:author"],
				"modified": dateToString(commentNode.properties["view:lastModified"])
		};
		comments.push(commentdetail);
	}
	view2comment[viewnode.properties["view:mdid"]] = commentids;
	
	if (recurse) {
		var cviews = viewnode.assocs["view:views"];
		for (var i in cviews) {
			handleView(cviews[i]);
		}
	}
}

function main() {
	var topview = modelFolder.childByNamePath(viewid);
	if (topview == null) {
		status.code  = 404;
	} else {
		handleView(topview);
	}
}


if (UserUtil.hasWebScriptPermissions()) {
    status.code = 200;
    main();
} else {
    status.code = 401;
}

var response;
if (status.code == 200) {
    response = jsonUtils.toJSONString({"comments": comments, "view2comment": view2comment}) ;
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;