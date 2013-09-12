<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">


//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("ViewEditor/snapshots");
var snapshotid = guid();

function main() {
	var viewid = url.templateArgs.viewid;
	var vnode = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
	if (vnode == null || vnode.length == 0) {
		status.code = 404;
		return;
	}
	vnode = vnode[0];
	var docsnapshot = snapshotFolder.childByNamePath(viewid);
	if (docsnapshot == null)
		docsnapshot = snapshotFolder.createFolder(viewid);
	var snapshotNode = docsnapshot.createNode(snapshotid, "view:Snapshot");
	snapshotNode.properties["view:productJson"] = jsonUtils.toJSONString(json);
	snapshotNode.save();
	vnode.createAssociation(snapshotNode, "view:snapshots");
	
}

status.code = 200;
main();
if (status.code == 200)
	model['res'] = snapshotid;
else
	model['res'] = "NotFound";