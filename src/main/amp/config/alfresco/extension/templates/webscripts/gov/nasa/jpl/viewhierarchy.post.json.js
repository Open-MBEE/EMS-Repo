<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");
var presentationFolder = europaSite.childByNamePath("/vieweditor/presentation");

var modelMapping = {};
var merged = [];

function updateViewHierarchy(views, nosections) {
	
}

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	var viewid = url.templateArgs.viewid;
	var views = postjson.views;
	var nosections = postjson.noSections;
	updateViewHierarchy(views, nosections);
	
}

main();
var response = "ok";
if (merged.length > 0) {
	response = jsonUtils.toJSONString(merged);
}
model['res'] = response;