<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");

var modelMapping = {};

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	var viewid = url.templateArgs.viewid;
	var views = postjson.views;
	var nosections = postjson.noSections;
	for (var viewid in views) {
		var viewNode = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
		if (viewNode == null || viewNode.length == 0) {
			continue;//should throw error
		}
		modelMapping[viewid] = viewNode[0];
	}
	updateViewHierarchy(modelMapping, views, nosections);
}

main();
model['res'] = "ok";