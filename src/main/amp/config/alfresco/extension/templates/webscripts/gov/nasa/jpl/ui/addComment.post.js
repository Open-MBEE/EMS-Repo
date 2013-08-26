<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");

var viewid = url.templateArgs.viewid;

function main() {
	var newid = "comment" + guid();
	var	commentNode = modelFolder.createNode(newid, "view:Comment");
	commentNode.properties["view:mdid"] = newid;

	commentNode.properties["view:documentation"] = requestbody.content;
	commentNode.properties["view:author"] = person.properties["cm:userName"];
	commentNode.properties["view:lastModified"] = new Date();
	commentNode.properties["view:deleted"] = false;
	commentNode.properties["view:committed"] = false;
	commentNode.save();
	
	var view = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
	if (view == null || view.length == 0)
		return;
	
	view.createAssociation(commentNode, "view:comments");
}

main();
model['res'] = "ok";