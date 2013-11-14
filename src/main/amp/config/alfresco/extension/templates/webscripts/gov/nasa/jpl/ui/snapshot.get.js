<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">
<import resource="classpath:alfresco/extension/js/view_utils.js">

var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");
var res = null;

function main() {
	var snapshotid = url.templateArgs.id;
	var docid = url.templateArgs.viewid;
	var node = snapshotFolder.childByNamePath(docid + "/" + snapshotid);
	
	if (node == null) {
		status.code = 404;
		return;
	}
	
	var topview = getModelElement(modelFolder, docid); //modelFolder.childByNamePath(docid);
	var jsonstring = node.properties["view:productJson"];
	res = JSON.parse(jsonstring);
	res['snapshots'] = [];
	res['user'] = person.properties['cm:userName'];
	res['snapshoted'] = utils.toISO8601(node.properties['cm:created']);
	res = toJson(res);
	
	/*var html = node.assocs["view:html"];
	if (html.length > 0)
		res = html[0].content;
	else
		status.code = 404;
	*/
}

if (UserUtil.hasWebScriptPermissions()) {
    status.code = 200;
    main();
} else {
    status.code = 401;
}

var response;
if (status.code == 200) {
    response = res;
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;