<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

var modelFolder = companyhome.childByNamePath("ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("ViewEditor/snapshots");
var res = null;

function main() {
	var snapshotid = url.templateArgs.id;
	var docid = url.templateArgs.viewid;
	var node = snapshotFolder.childByNamePath(docid + "/" + snapshotid);
	if (node == null) {
		status.code = 404;
		return;
	}
	res = node.properties["view:productJson"];
}

status.code = 200;
main();
if (status.code == 200)
	model['res'] = res;
else
	model['res'] = "NotFound";