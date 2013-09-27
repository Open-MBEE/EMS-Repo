<import resource="classpath:alfresco/extension/js/json2.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("ViewEditor/model");
var res = [];
var seen = [];

var viewid = url.templateArgs.viewid
var recurse = args.recurse == 'true' ? true : false;

function add(modelNode) {
	var info = {};
	info['mdid'] = modelNode.properties["view:mdid"];
	info['documentation'] = modelNode.properties["view:documentation"];
	var name = modelNode.properties["view:name"];
	if (name != null && name != undefined)
		info['name'] = name;
	var dvalue = modelNode.properties["view:defaultValue"];
	if (dvalue != null && dvalue != undefined)
		info['dvalue'] = dvalue;
	res.push(info);
	seen.push(modelNode.mdid);
}

function handleView(view) {
	var sourcesJson = view.properties["view:sourcesJson"];
	var sources = JSON.parse(sourcesJson);
	for (var i in sources) {
		var sourceid = sources[i];
		var modelNode = modelFolder.childByNamePath(sourceid);
		if (modelNode == null)
			continue;
		if (seen.indexOf(sourceid) >= 0)
			continue;
		add(modelNode);
	}
	if (recurse) {
		var childViews = view.assocs["view:views"];
		for (var i in childViews) {
			handleView(childViews[i]);
		}
	}
}

function main() {
	var topview = modelFolder.childByNamePath(viewid);
	if (topview == null) {
		status.code = 404;
	} else {
		handleView(topview);
	}
}

status.code = 200;
main();
var	response = status.code == 200 ? jsonUtils.toJSONString(res) : "NotFound";
model['res'] = response;