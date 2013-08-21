<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");
var presentationFolder = europaSite.childByNamePath("/vieweditor/presentation");
var date = new Date();
var modelMapping = {};
var merged = [];
var user = args.user;
function updateOrCreateModelElement(element, force) {
	var modelNode = modelMapping[element.mdid];
	if (modelNode == null || modelNode == undefined) {
		modelNode = modelFolder.childrenByXPath("*[@view:mdid='" + element.mdid + "']");
		if (modelNode == null || modelNode.length == 0) {
			if (element.type == "View") {
				modelNode = modelFolder.createNode(element.mdid, "view:View");
				modelNode.properties["view:name"] = element.name;
			} else if (element.type == "Property") {
				modelNode = modelFolder.createNode(element.mdid, "view:Property");
				if (element.name != undefined)
					modelNode.properties["view:name"] = element.name;
			} else if (element.type == "Comment")
				modelNode = modelFolder.createNode(element.mdid, "view:Comment");
			else {
				modelNode = modelFolder.createNode(element.mdid, "view:ModelElement");
				modelNode.properties["view:name"] = element.name;
			}
		} else
			modelNode = modelNode[0];
	}

	if (element.name != null && element.name != undefined && element.name != modelNode.properties["view:name"]) {
		if (force)
			modelNode.properties["view:name"] = element.name;
		else
			modelNode.properties["view:name"] = modelNode.properties["view:name"] + " - MERGED - " + element.name;
		merged.push({"mdid": element.mdid, "type": "name"})
	}
	if (element.documentation != modelNode.properties["view:documentation"]) {
		if (force)
			modelNode.properties["view:documentation"] = element.documentation;
		else
			modelNode.properties["view:documentation"] = modelNode.properties["view:documentation"] + " <p><strong><i> MERGED NEED RESOLUTION! </i></strong></p> " + element.documentation;
		merged.push({"mdid": element.mdid, "type": "doc"})
	}
	if (element.type == "Property" && element.dvalue != modelNode.properties["view:defaultValue"]) {
		if (force)
			modelNode.properties["view:defaultValue"] = element.dvalue;
		else
			modelNode.properties["view:defaultValue"] = modelNode.properties["view:defaultValue"] + " - MERGED - " + element.dvalue;
		merged.push({"mdid": element.mdid, "type": "dvalue"})
	}
	modelNode.properties["view:mdid"] = element.mdid;
	modelNode.save();
	modelMapping[element.mdid] = modelNode;
	return modelNode;
}

function updateOrCreateView(view) {
	var viewNode = modelMapping[view.mdid];
	if (viewNode == null || viewNode == undefined) {
		viewNode = modelFolder.childrenByXPath("*[@view:mdid='" + view.mdid + "']");
		if (viewNode == null || viewNode.length == 0) {
			return;
		}
		viewNode = viewNode[0];
	}
	var sources = [];
	for (var i in view.contains) {
		fillSources(view.contains[i], sources);
	}
	viewNode.properties["view:sourcesJson"] = jsonUtils.toJSONString(sources);
	if (view.noSection != null && view.noSection != undefined)
		viewNode.properties["view:noSection"] = view.noSection;
	else
		viewNode.properties["view:noSection"] = false;
	viewNode.properties["view:containsJson"] = jsonUtils.toJSONString(view.contains);
	viewNode.properties["view:author"] = user;
	viewNode.properties["view:lastModified"] = date;
	viewNode.save();
	return viewNode;
}

function fillSources(contained, sources) {
	if (contained.type == "Paragraph") {
		if (contained.source == "text") {
	
		} else {
			modelNode = modelMapping[contained.source];
			if (sources.indexOf(modelNode.properties["view:mdid"]) < 0)
				sources.push(modelNode.properties["view:mdid"]);
		}
	} else if (contained.type == "Table") {
		for (var i in contained.sources) {
			var sourceid = contained.sources[i];
			if (sources.indexOf(sourceid) < 0)
				sources.push(sourceid);
		}		
	}
}

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	var viewid = url.templateArgs.viewid;
	var topview = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
	if (topview == null || topview.length == 0) {
		if (args.doc == 'true') {
			topview = modelFolder.createNode(viewid, "view:DocumentView");
		} else {
			topview = modelFolder.createNode(viewid, "view:View");
		}
		topview.properties["view:mdid"] = viewid;
		topview.save();
		modelMapping[viewid] = topview;
	} 
	var force = args.force == 'true' ? true : false;
	for (var i in postjson.elements) {
		updateOrCreateModelElement(postjson.elements[i], force);
	}
	for (var i in postjson.views) {
		updateOrCreateView(postjson.views[i]);
	}
	if (args.recurse == 'true') {
		updateViewHierarchy(modelMapping, postjson.view2view);
	}	
}

main();
var response = "ok";
if (merged.length > 0) {
	response = jsonUtils.toJSONString(merged);
}
model['res'] = response;