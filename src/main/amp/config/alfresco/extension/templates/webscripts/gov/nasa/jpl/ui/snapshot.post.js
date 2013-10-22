<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">
<import resource="classpath:alfresco/extension/js/artifact_utils.js">
<import resource="classpath:alfresco/extension/js/view_utils.js">

var modelFolder = companyhome.childByNamePath("ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("ViewEditor/snapshots");
var snapshotid = guid();
var viewid = url.templateArgs.viewid;
var product = false;

/* this was an old code where posting a snapshot means we recreate the json on server side and save it as a blob in the snapshot class, might be useful in future
function main() {
	var topview = modelFolder.childByNamePath(viewid);
	
	var elements = [];
	var seen = [];
	var views = [];
	var view2view = {};
	
	if (topview == null) {
		status.code = 404;
	} else {
		if (topview.properties["view:product"])
			product = true;
		if (product) {
			view2view = JSON.parse(topview.properties["view:view2viewJson"]);
			var noSections = JSON.parse(topview.properties["view:noSectionsJson"]);
			for (var viewmdid in view2view) {
				var view = modelFolder.childByNamePath(viewmdid);
				if (view == null) {
					status.code = 404;
					return;
				}
				var viewinfo = handleView(view, seen, elements, views, view2view);
				if (noSections.indexOf(viewmdid) >= 0)
					viewinfo.noSection = true;
				else
					viewinfo.noSection = false;
			}
		} else {
			handleView(topview, seen, elements, views, view2view);
		}
	}
	var info = {};
	info['elements'] = elements;
	info['view2view'] = view2view;
	info['views'] = views;
	info['rootView'] = viewid;
	info['snapshot'] = true;
	
	var docsnapshot = snapshotFolder.childByNamePath(viewid);
	if (docsnapshot == null)
		docsnapshot = snapshotFolder.createFolder(viewid);
	var snapshotNode = docsnapshot.createNode(snapshotid, "view:Snapshot");
	snapshotNode.properties["view:productJson"] = toJson(info);
	snapshotNode.save();
	topview.createAssociation(snapshotNode, "view:snapshots");
	
	var html = requestbody.content;
	var htmlNode = docsnapshot.createFile(snapshotid + ".html");
	htmlNode.content = html;
    htmlNode.properties.content.setEncoding("UTF-8");
	htmlNode.save();
	snapshotNode.createAssociation(htmlNode, "view:html");
	snapshoturl.url = url.context + "wcs/ui/views/" + viewid + "/snapshots/" + snapshotid;
	snapshoturl.creator = person.properties['cm:userName'];
	snapshoturl.created = utils.toISO8601(htmlNode.properties["cm:created"]);
	snapshoturl.id = snapshotid;
}
*/
var snapshoturl = {};
function main() {
	var html = requestbody.content;
	var topview = modelFolder.childByNamePath(viewid);
	if (topview == null) {
		status.code = 404;
		return;
	}
	var docsnapshot = snapshotFolder.childByNamePath(viewid);
	if (docsnapshot == null)
		docsnapshot = snapshotFolder.createFolder(viewid);
	var snapshotNode = docsnapshot.createNode(snapshotid, "view:Snapshot");
	snapshotNode.save();
	var htmlNode = docsnapshot.createFile(snapshotid + ".html");
	//snapshotNode.properties["view:productJson"] = toJson(info);
	htmlNode.content = html;
    htmlNode.properties.content.setEncoding("UTF-8");
	htmlNode.save();
	snapshotNode.createAssociation(htmlNode, "view:html");
	topview.createAssociation(snapshotNode, "view:snapshots");
	snapshoturl.url = url.context + htmlNode.url;
	snapshoturl.creator = person.properties['cm:userName'];
	snapshoturl.created = utils.toISO8601(htmlNode.properties["cm:created"]);
	snapshoturl.id = snapshotid;
}

if (UserUtil.hasWebScriptPermissions()) {
    status.code = 200;
    main();
} else {
    status.code = 401;
}

var response;
if (status.code == 200) {
    response = jsonUtils.toJSONString(snapshoturl);
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;