<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">


var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");
var commentFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/comments");

var viewid = url.templateArgs.viewid;

function main() {
	var newid = "comment" + guid();
	var	commentNode = commentFolder.createNode(newid, "view:Comment");
	commentNode.properties["view:mdid"] = newid;

	commentNode.properties["view:documentation"] = requestbody.content;
	commentNode.properties["view:author"] = person.properties["cm:userName"];
	commentNode.properties["view:lastModified"] = new Date();
	commentNode.properties["view:deleted"] = false;
	commentNode.properties["view:committed"] = false;
	commentNode.save();
	
	var view = getModelElement(modelFolder, viewid); //modelFolder.childByNamePath(viewid);
	if (view == null)
		return;
	view.createAssociation(commentNode, "view:comments");
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