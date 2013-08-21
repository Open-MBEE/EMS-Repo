<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");

var modelMapping = {};

function main() {
	var volid = requestbody.content;
	var docid = url.templateArgs.docid;
	var dnode = modelFolder.childrenByXPath("*[@view:mdid='" + docid + "']");
	var vnode = modelFolder.childrenByXPath("*[@view:mdid='" + volid + "']");
	if (dnode == null || dnode.length == 0) {
		dnode = modelFolder.createNode(docid, "view:DocumentView");
		dnode.properties["view:mdid"] = docid;
		dnode.properties["view:name"] = "Unexported Document";
		dnode.save();
	} else
		dnode = dnode[0];
	if (vnode == null || vnode.length == 0) {
		status.code = 404;
		return;
	} else
		vnode = vnode[0];
	cleanDocument(dnode);
	vnode.createAssociation(dnode, "view:documents");
}
status.code = 200;
main();
if (status.code == 200)
	model['res'] = "ok";
else
	model['res'] = "NotFound";