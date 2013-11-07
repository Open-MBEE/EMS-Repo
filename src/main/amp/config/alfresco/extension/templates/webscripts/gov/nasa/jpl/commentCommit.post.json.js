<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");
var commentFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/comments");
var modelMapping = {};

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	for (var oldid in postjson) {
		var comment = commentFolder.childByNamePath(oldid);
		if (comment == null)
			continue; //error?
		if (oldid != postjson[oldid]) {
			comment.properties["view:mdid"] = postjson[oldid];
			comment.properties["cm:name"] = postjson[oldid];
		}
		comment.properties["view:committed"] = true;
		comment.properties["view:deleted"] = false;
		comment.save();
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
    response = "ok";
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;