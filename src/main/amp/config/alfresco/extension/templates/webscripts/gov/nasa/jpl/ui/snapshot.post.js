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
				var view = modelFolder.childrenByXPath("*[@view:mdid='" + viewmdid + "']");
				if (view == null || view.length == 0) {
					status.code = 404;
					return;
				}
				var viewinfo = handleView(view[0]);
				if (noSections.indexOf(viewmdid) >= 0)
					viewinfo.noSection = true;
				else
					viewinfo.noSection = false;
			}
		} else {
			handleView(topview);
		}
	}
	var info = {};
	info['elements'] = elements;
	info['view2view'] = view2view;
	info['views'] = views;
	info['rootView'] = viewid;
	
	var docsnapshot = snapshotFolder.childByNamePath(viewid);
	if (docsnapshot == null)
		docsnapshot = snapshotFolder.createFolder(viewid);
	var snapshotNode = docsnapshot.createNode(snapshotid, "view:Snapshot");
	snapshotNode.properties["view:productJson"] = toJson(info);
	snapshotNode.save();
	topview.createAssociation(snapshotNode, "view:snapshots");
}
*/

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
}

status.code = 200;
main();
if (status.code == 200)
	model['res'] = snapshotid;
else
	model['res'] = "NotFound";