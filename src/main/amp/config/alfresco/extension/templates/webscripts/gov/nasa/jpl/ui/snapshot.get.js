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
	
	if (node == null) {
		status.code = 404;
		return;
	}
	/*
	var topview = modelFolder.childByNamePath(docid);
	var jsonstring = node.properties["view:productJson"];
	var res = JSON.parse(jsonstring);
	res['snapshots'] = getSnapsnots(topview);
	res['user'] = person.properties['cm:userName'];
	res = toJson(res);
	*/
	var html = node.assocs["view:html"];
	if (html.length > 0)
		res = html[0].content;
	else
		status.code = 404;
	
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