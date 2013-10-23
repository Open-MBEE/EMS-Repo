<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");

var modelMapping = {};

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	var viewid = url.templateArgs.viewid;
	var views = postjson.views;
	var nosections = postjson.noSections;
	
	var viewNode = modelFolder.childByNamePath(viewid);
	if (viewNode == null) {
		return; //should throw error
	}
	if (viewNode.properties["view:product"]) {
		viewNode.properties["view:view2viewJson"] = jsonUtils.toJSONString(views);
		viewNode.properties["view:noSectionsJson"] = jsonUtils.toJSONString(nosections);
		viewNode.save();
		return;
	}

	for (var vid in views) {
		var vNode = modelFolder.childByNamePath(vid);
		if (vNode == null) {
			continue;//should throw error
		}
		modelMapping[vid] = vNode;
	}
	updateViewHierarchy(modelMapping, views, nosections);
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