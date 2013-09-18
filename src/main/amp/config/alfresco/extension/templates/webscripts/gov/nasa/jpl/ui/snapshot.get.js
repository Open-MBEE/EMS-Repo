<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">
<import resource="classpath:alfresco/extension/js/view_utils.js">

var modelFolder = companyhome.childByNamePath("ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("ViewEditor/snapshots");
var res = null;

function main() {
	var snapshotid = url.templateArgs.id;
	var docid = url.templateArgs.viewid;
	var node = snapshotFolder.childByNamePath(docid + "/" + snapshotid);
	var topview = modelFolder.childByNamePath(docid);
	if (node == null || topview == null) {
		status.code = 404;
		return;
	}
	var jsonstring = node.properties["view:productJson"];
	var res = JSON.parse(jsonstring);
	getSnapsnots(topview);
	res['snapshots'] = snapshots;
	res['user'] = person.properties['cm:userName'];
	res = toJson(res);
	
}

status.code = 200;
main();
if (status.code == 200)
	model['res'] = res;
else
	model['res'] = "NotFound";