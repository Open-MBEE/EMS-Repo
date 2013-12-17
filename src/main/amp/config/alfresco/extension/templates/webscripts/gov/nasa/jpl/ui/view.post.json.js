<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");
var date = new Date();
var modelElements = {}

function updateModelElement(element) {
	var	modelNode = getModelElement(modelFolder, element.mdid); //modelFolder.childByNamePath(element.mdid);
	if (modelNode == null) {
		return;
	} else {
		modelElements[element.mdid] = modelNode;
	}

	if (element.name != null && element.name != undefined && element.name != modelNode.properties["view:name"]) {
		setName(modelNode, element.name);
	}
	if (element.documentation != undefined && element.documentation != modelNode.properties["view:documentation"]) {
		modelNode.properties["view:documentation"] = element.documentation;
	}
	if (element.dvalue != null && element.dvalue != undefined && element.dvalue != modelNode.properties["view:defaultValue"]) {
		modelNode.properties["view:defaultValue"] = element.dvalue;
	}
	modelNode.save();
}

function main() {
	var elements = JSON.parse(json.toString());
	if (elements == null || elements == undefined)
		return;
	
	for (var i in elements) {
		updateModelElement(elements[i]);
	}
	
	var viewid = url.templateArgs.viewid;
	var viewnode = null;
	if (viewid in modelElements) {
		viewnode = modelElements[viewid];
	} else {
		viewnode = getModelElement(modelFolder, viewid); //modelFolder.childByNamePath(viewid);
	}
	viewnode.properties['author'] = person.properties['cm:userName'];
	viewnode.properties['lastModified'] = date;
	viewnode.save();
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