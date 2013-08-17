<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");

var modelMapping = {};
var merged = [];

/*function updateViewHierarchy(views, nosections) {
	for (var viewid in views) {
		var viewNode = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
		if (viewNode == null || viewNode.length == 0) {
			continue;//should throw error
		}
		modelMapping[viewid] = viewNode[0];
	}
	for (var pview in views) {
		var cviews = views[pview];
		var pviewnode = modelMapping[pview];
		if (pviewnode == null || pviewnode == undefined) {
			continue;
		}
		var oldchildren = pviewnode.assocs["view:views"];
		for (var i in oldchildren) {
			pviewnode.removeAssociation(oldchildren[i], "view:views");
		}
		for (var ci in cviews) {
			var cvid = cviews[ci];
			var cviewnode = modelMapping[cvid];
			if (cviewnode == null || cviewnode == undefined) {
				continue;
			}
			cviewnode.properties["view:index"] = ci;
			cviewnode.save();
			pviewnode.createAssociation(cviewnode, "view:views");
		}
		pviewnode.properties["view:viewsJson"] = jsonUtils.toJSONString(cviews);
		if (nosections != undefined && nosections.indexOf(pview) > 0)
			pviewnode.properties["view:noSection"] = true;
		pviewnode.save();
	}
}*/

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