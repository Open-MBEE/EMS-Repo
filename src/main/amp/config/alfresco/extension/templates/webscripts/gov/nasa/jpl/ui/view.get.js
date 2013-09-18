<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">
<import resource="classpath:alfresco/extension/js/artifact_utils.js">
<import resource="classpath:alfresco/extension/js/view_utils.js">

var modelFolder = companyhome.childByNamePath("ViewEditor/model");
var viewid = url.extension
var product = false;

function main() {
	var topview = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
	if (topview == null || topview.length == 0) {
		status.code = 404;
	} else {
		topview = topview[0];
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
		getSnapshots(topview);
	}
}

status.code = 200;
main();

var info = {};
info['elements'] = elements;
info['view2view'] = view2view;
info['views'] = views;
info['rootView'] = viewid;
info['user'] = person.properties['cm:userName'];
info['snapshots'] = snapshots;

var	response = status.code == 200 ? toJson(info) : "NotFound";
if (status.code != 200) {
	status.redirect = true;
	status.message = response;
}
model['res'] = response;
