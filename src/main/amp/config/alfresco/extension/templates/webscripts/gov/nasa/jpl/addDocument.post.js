<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">


//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("ViewEditor/model");

var modelMapping = {};

function main() {
	var volid = requestbody.content;
	var docid = url.templateArgs.docid;
	var dnode = modelFolder.childByNamePath(docid);
	var vnode = modelFolder.childByNamePath(volid);
	if (dnode == null) {
		dnode = modelFolder.createNode(docid, "view:DocumentView");
		dnode.properties["view:mdid"] = docid;
		dnode.properties["view:name"] = "Unexported Document";
		dnode.save();
	} 
	if (vnode == null) {
		status.code = 404;
		return;
	}
	cleanDocument(dnode);
	vnode.createAssociation(dnode, "view:documents");
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