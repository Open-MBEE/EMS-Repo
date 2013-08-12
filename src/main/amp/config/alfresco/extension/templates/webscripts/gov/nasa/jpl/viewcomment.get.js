<import resource="classpath:alfresco/extension/js/json2.js">

var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");
var res = [];
var seen = [];

var viewid = url.templateArgs.viewid;
var recurse = args.recurse == 'true' ? true : false;


function main() {
	var topview = modelFolder.childrenByXPath("*[@view:mdid='" + viewid + "']");
	if (topview == null || topview.length == 0) {
		response = "NotFound";
	} else {
		topview = topview[0];
		handleView(topview);
	}
}

var response = "[]";
main();

if (res.length > 0) {
	response = jsonUtils.toJSONString(res);
}
model['res'] = response;