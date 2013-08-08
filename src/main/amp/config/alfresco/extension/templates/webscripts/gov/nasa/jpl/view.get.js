<import resource="classpath:alfresco/extension/js/json2.js">

var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");
var res = [];
var seen = [];

var viewid = url.extension
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
		var modelNode = modelFolder.childrenByXPath("*[@view:mdid='" + sourceid + "']");
		if (modelNode == null || modelNode == undefined)
			continue;
		if (seen.indexOf(sourceid) >= 0)
			continue;
		add(modelNode[0]);
	}
	if (recurse) {
		var childViews = view.assocs["view:views"];
		for (var i in childViews) {
			handleView(childViews[i]);
		}
	}
}
var response = "NotFound";
var topview = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
if (topview == null || topview.length == 0) {
	response = "NotFound";
} else {
	topview = topview[0];
	handleView(topview);
	response = jsonUtils.toJSONString(res);
}

model['res'] = response;