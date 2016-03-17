<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/${deploymentName}/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/${deploymentName}/ViewEditor/snapshots");
var res = [];
var seen = [];
var viewdone = [];
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
		var modelNode = getModelElement(modelFolder, sourceid); //modelFolder.childByNamePath(sourceid);
		if (modelNode == null)
			continue;
		if (seen.indexOf(sourceid) >= 0)
			continue;
		add(modelNode);
	}
	viewdone.push(view.properties["view:mdid"]);
	if (recurse) {
		if (view.properties["view:product"] == true) {
			var view2view = JSON.parse(view.properties["view:view2viewJson"]);
			for (var i in view2view) {
				if (viewdone.indexOf(i) >= 0)
					continue;
				var v = getModelElement(modelFolder, i);
				if (v == null)
					continue;
				handleView(v);
			}
		} else {
			var childViews = view.assocs["view:views"];
			for (var i in childViews) {
				handleView(childViews[i]);
			}
		}
	}
}

function main() {
	var topview = getModelElement(modelFolder, viewid); //modelFolder.childByNamePath(viewid);
	if (topview == null) {
		status.code = 404;
	} else {
		handleView(topview);
	}
}


if (UserUtil.hasWebScriptPermissions()) {
    status.code = 200;
    main();
} else {
    status.code = 401;
}

var response;
if (status.code == 200) {
    response = jsonUtils.toJSONString(res);
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;