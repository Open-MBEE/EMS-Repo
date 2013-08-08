<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/json_parse.js">
//<import resource="classpath:alfresco/extension/js/json2.js">
//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");
var presentationFolder = europaSite.childByNamePath("/vieweditor/presentation");

var modelMapping = {};

function guid() {
    function _p8(s) {
        var p = (Math.random().toString(16)+"000000000").substr(2,8);
        return s ? "-" + p.substr(0,4) + "-" + p.substr(4,4) : p ;
    }
    return _p8() + _p8(true) + _p8(true) + _p8();
}

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

	if (element.name != null && element.name != undefined && element.name != modelNode.properties["view:name"])
		if (force)
			modelNode.properties["view:name"] = element.name;
		else
			modelNode.properties["view:name"] = modelNode.properties["view:name"] + " - MERGED - " + element.name;
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
	modelMapping[element.mdid] = modelNode;
	return modelNode;
}

function updateOrCreateView(view) {
	//var viewNode = modelFolder.childrenByXPath("*[@view:mdid='" + view.mdid + "']");
	var viewNode = modelMapping[view.mdid];
	if (viewNode == null || viewNode == undefined) {
		viewNode = modelFolder.childrenByXPath("*[@view:mdid='" + view.mdid + "']");
		if (viewNode == null || viewNode.length == 0) {
			return;
		}
		viewNode = viewNode[0];
	}
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
	viewNode.properties["view:containsJson"] = jsonUtils.toJSONString(view.contains);
	viewNode.save();
	return viewNode;
}

function updateViewHierarchy(views) {
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
			//var oldparents = cviewnode.sourceAssocs["view:views"];
			cviewnode.properties["view:index"] = ci;
			cviewnode.save();
			pviewnode.createAssociation(cviewnode, "view:views");
		}
		pviewnode.properties["view:viewsJson"] = jsonUtils.toJSONString(cviews);
		pviewnode.save();
	}
}

function createContainedElement(contained, i) {
	var pnode = null;
	if (contained.type == "Paragraph") {
		pnode = presentationFolder.createNode(guid(), "view:Paragraph");
		if (contained.source == "text") {
			tnode = presentationFolder.createNode(guid(), "view:Text");
			tnode.properties["view:text"] = contained.text;
			tnode.save();
			pnode.createAssociation(tnode, "view:source");
		} else {
			//modelNode = modelFolder.childrenByXPath("*[@view:mdid='" + contained.source + "']");
			modelNode = modelMapping[contained.source];
			pnode.createAssociation(modelNode, "view:source");
			pnode.properties["view:useProperty"] = contained.useProperty;
			pnode.save();
		}
	
	} else if (contained.type == "Table") {
		pnode = presentationFolder.createNode(guid(), "view:Table");
		pnode.properties["view:title"] = contained.title;
		pnode.properties["view:header"] = jsonUtils.toJSONString(contained.header);
		pnode.properties["view:body"] = jsonUtils.toJSONString(contained.body);
		pnode.properties["view:style"] = contained.style;
		pnode.save();
		
	}
	pnode.properties["view:index"] = i;
	//var orderable = new Array(1);
	//orderable["view:index"] = i;
	//pnode.addAspect("view:Orderable", orderable);
	pnode.save();
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
		updateViewHierarchy(postjson.view2view);
	}	
}

main();