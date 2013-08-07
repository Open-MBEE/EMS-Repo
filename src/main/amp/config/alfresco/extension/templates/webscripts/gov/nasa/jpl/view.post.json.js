<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/json_parse.js">
//<import resource="classpath:alfresco/extension/js/json2.js">
//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");
var presentationFolder = europaSite.childByNamePath("/vieweditor/presentation");

function updateOrCreateModelElement(element, force) {
	var modelNode = modelFolder.childrenByXPath("*[@view:mdid='" + element.mdid + "']");
	if (modelNode == null || modelNode.length == 0) {
		if (element.type == "View")
			modelNode = modelFolder.createNode(element.name, "view:View");
		else if (element.type == "Property")
			modelNode = modelFolder.createNode(element.name, "view:Property");
		else if (element.type == "Comment")
			modelNode = modelFolder.createNode("Comment", "view:Comment");
		else
			modelNode = modelFolder.createNode(element.name, "view:ModelElement");
	} else
		modelNode = modelNode[0];

	if (element.name != null && element.name != undefined && element.name != modelNode.properties["cm:name"])
		if (force)
			modelNode.properties["cm:name"] = element.name;
		else
			modelNode.properties["cm:name"] = modelNode.properties["cm:name"] + " - MERGED - " + element.name;
	if (element.documentation != modelNode.properties["view:documentation"])
		if (force)
			modelNode.properties["view:documentation"] = element.documentation;
		else
			modelNode.properties["view:documentation"] = modelNode.properties["view:documentation"] + " <p><strong><i> MERGED NEED RESOLUTION! </i></strong></p> " + element.documentation;
	if (element.type == "Property" && element.dvalue != modelNode.properties["view:dvalue"])
		if (force)
			modelNode.properties["view:dvalue"] = element.dvalue;
		else
			modelNode.properties["view:dvalue"] = modelNode.properties["view:dvalue"] + " - MERGED - " + element.dvalue;
	modelNode.properties["view:mdid"] = element.mdid;
	modelNode.save();
	return modelNode;
}

function updateOrCreateView(view) {
	var viewNode = modelFolder.childrenByXPath("*[@view:mdid='" + view.mdid + "']");
	if (viewNode == null || viewNode.length == 0) {
		return;
	}
	viewNode = viewNode[0];
	var contains = viewNode.assocs["view:contains"];
	for (var i in contains) {
		viewNode.removeAssociation(contains[i], "view:contains");
		contains[i].remove();
	}

	for (var i in view.contains) {
		var containedObject = createContainedElement(view.contains[i], i+1);
		viewNode.createAssociation(containedObject, "view:contains");
	}
	if (view.noSection != null && view.noSection != undefined)
		viewNode.properties["view:noSection"] = view.noSection;
	else
		viewNode.properties["view:noSection"] = false;
	viewNode.save();
	return viewNode;
}

function updateViewHierarchy(views) {
	
}

function createContainedElement(contained, i) {
	var pnode = null;
	if (contained.type == "Paragraph") {
		pnode = presentationFolder.createNode("blah", "view:Paragraph");
		if (contained.source == "text") {
			tnode = presentationFolder.createNode("blah", "view:Text");
			tnode.properties["view:text"] = contained.text;
			tnode.save();
			pnode.createAssociation(tnode, "view:source");
		} else {
			modelNode = modelFolder.childrenByXPath("*[@view:mdid='" + contained.source + "']");
			pnode.createAssociation(modelNode, "view:source");
			pnode.properties["view:useProperty"] = contained.useProperty;
			pnode.save();
		}
	
	} else if (contained.type == "Table") {
		pnode = presentationFolder.createNode(contained.title, "view:Table");
		pnode.properties["view:header"] = jsonUtils.toJSONString(contained.header);
		pnode.properties["view:body"] = jsonUtils.toJSONString(contained.body);
		pnode.properties["view:style"] = contained.style;
		pnode.save();
		
	}
	var orderable = new Array(1);
	orderable["view:index"] = i;
	pnode.addAspect("view:Orderable", orderable);
	return pnode;
}

function main() {
	//var europaSite = siteService.getSite("europa");
	//var postjson = jsonUtils.toObject(json);
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	var viewid = url.extension
	var topview = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
	if (topview == null || topview.length == 0) {
		if (args.doc == 'true') {
			topview = modelFolder.createNode("blah", "view:DocumentView");
		} else {
			topview = modelFolder.createNode("blah", "view:View");
		}
		topview.properties["view:mdid"] = viewid;
		topview.save();
	} 
	var force = args.force == 'true' ? true : false;
	for (var i in postjson.elements) {
		updateOrCreateModelElement(postjson.elements[i], force);
	}
	for (var i in postjson.views) {
		updateOrCreateView(postjson.views[i]);
	}
	if (args.recurse == 'true') {
		updateViewHierarchy(postjson.view2view);
	}	
}

main();