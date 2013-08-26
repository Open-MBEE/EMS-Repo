<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");
var date = new Date();

function updateModelElement(element) {
	var modelNode = modelFolder.childrenByXPath("*[@view:mdid='" + element.mdid + "']");
	if (modelNode == null || modelNode.length == 0) {
		return;
	} else
		modelNode = modelNode[0];

	if (element.name != null && element.name != undefined && element.name != modelNode.properties["view:name"]) {
		modelNode.properties["view:name"] = element.name;
	}
	if (element.documentation != modelNode.properties["view:documentation"]) {
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
	var viewnode = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
	viewnode.properties['author'] = person.properties['cm:userName'];
	viewnode.properties['lastModified'] = date;
	viewnode.save();
}

main();
model['res'] = "ok";