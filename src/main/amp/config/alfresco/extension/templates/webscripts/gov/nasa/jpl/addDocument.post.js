<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">


//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");

var modelMapping = {};

function main() {
	var volid = requestbody.content;
	var docid = url.templateArgs.docid;
	var dnode = getModelElement(modelFolder, docid); //modelFolder.childByNamePath(docid);
	var vnode = modelFolder.childByNamePath(volid);
	if (dnode == null) {
		dnode = createModelElement(modelFolder, docid, "view:DocumentView"); //modelFolder.createNode(docid, "view:DocumentView");
		dnode.properties["view:mdid"] = docid;
		setName(dnode, "Unexported Document");
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