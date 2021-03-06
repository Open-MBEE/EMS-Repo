<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">
<import resource="classpath:alfresco/extension/js/artifact_utils.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");
var date = new Date();
var modelMapping = {};
var merged = [];
var user = args.user;
function updateOrCreateModelElement(element, force) {
	var modelNode = modelMapping[element.mdid];
	if (modelNode == null || modelNode == undefined) {
		modelNode = getModelElement(modelFolder, element.mdid); //modelFolder.childByNamePath(element.mdid);
		if (modelNode == null) {
			if (element.type == "View") {
				modelNode = createModelElement(modelFolder, element.mdid, "view:View"); //modelFolder.createNode(element.mdid, "view:View");
				setName(modelNode, element.name);
			} else if (element.type == "Property") {
				modelNode = createModelElement(modelFolder, element.mdid, "view:Property"); //modelFolder.createNode(element.mdid, "view:Property");
				if (element.name != undefined) {
					setName(modelNode, element.name);
				}
			} else if (element.type == "Comment")
				modelNode = createModelElement(modelFolder, element.mdid, "view:Comment"); //modelFolder.createNode(element.mdid, "view:Comment");
			else {
				modelNode = createModelElement(modelFolder, element.mdid, "view:ModelElement"); //modelFolder.createNode(element.mdid, "view:ModelElement");
				if (element.name != null || element.name != undefined) {
					setName(modelNode, element.name);
				}
			}
			modelNode.save();
		}
	}

	if (element.name != null && element.name != undefined && element.name != modelNode.properties["view:name"]) {
		if (force)
			setName(modelNode, element.name);
		else
			setName(modelNode, modelNode.properties["view:name"] + " - MERGED - " + element.name);
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

function updateOrCreateView(view, ignoreNoSection) {
	var viewNode = modelMapping[view.mdid];
	if (viewNode == null || viewNode == undefined) {
		viewNode = getModelElement(modelFolder, view.mdid); //modelFolder.childByNamePath(view.mdid);
		if (viewNode == null) {
			return;
		}
	}
	if (viewNode.typeShort != "view:DocumentView" && viewNode.typeShort != "view:View") {
		viewNode.specializeType("view:View");
		viewNode.save();
	}
	var sources = [];
	for (var i in view.contains) {
		fillSources(view.contains[i], sources);
	}
	viewNode.properties["view:sourcesJson"] = jsonUtils.toJSONString(sources);
	if (view.noSection != null && view.noSection != undefined && !ignoreNoSection)
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
	} else if (contained.type == "List") {
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

	// save off JSON file
    var vepath = "Sites/europa/ViewEditor/";
    //saveFile(vepath, "VIEW_" + viewid, json.toString());
    
	var topview = getModelElement(modelFolder, viewid); //modelFolder.childByNamePath(viewid);

	var product = false;
	if (args.product == 'true')
		product = true;
	if (topview == null) {
		if (args.doc == 'true') {
			topview = createModelElement(modelFolder, viewid, "view:DocumentView"); //modelFolder.createNode(viewid, "view:DocumentView");
			if (product) {
				topview.properties["view:product"] = true;
			}
		} else {
			topview = createModelElement(modelFolder, viewid, "view:View");//modelFolder.createNode(viewid, "view:View");
		}
		topview.properties["view:mdid"] = viewid;
		topview.save();
	}
	modelMapping[viewid] = topview;
	
	if (topview.typeShort != "view:DocumentView" && args.doc == 'true') {
		topview.specializeType("view:DocumentView");
		if (product)
			topview.properties["view:product"] = true;
		topview.save();
	}
	var force = args.force == 'true' ? true : false;
	for (var i in postjson.elements) {
		updateOrCreateModelElement(postjson.elements[i], force);
	}
	for (var i in postjson.views) {
		updateOrCreateView(postjson.views[i], product);
	}
	if (args.recurse == 'true' && !product) {
		updateViewHierarchy(modelMapping, postjson.view2view);
	}	
	if (product) {
		var noSections = [];
		for (var i in postjson.views) {
			var view = postjson.views[i];
			if (view.noSection)
				noSections.push(view.mdid);
		}
		topview.properties["view:view2viewJson"] = jsonUtils.toJSONString(postjson.view2view);
		topview.properties["view:noSectionsJson"] = jsonUtils.toJSONString(noSections);
		topview.save();
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
    response = "ok";
    if (merged.length > 0) {
        response = jsonUtils.toJSONString(merged);
    }
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;
finishDate = new Date();
logger.log(finishDate-date);
