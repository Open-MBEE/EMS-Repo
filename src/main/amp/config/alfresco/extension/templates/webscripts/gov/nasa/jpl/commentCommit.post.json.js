<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var modelFolder = roothome.childByNamePath("/Sites/europa/vieweditor/model");
//var presentationFolder = roothome.childByNamePath("/Sites/europa/vieweditor/presentation");
var europaSite = siteService.getSite("europa").node;
var modelFolder = europaSite.childByNamePath("/vieweditor/model");

var modelMapping = {};

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	for (var oldid in postjson) {
		var comment = modelFolder.childrenByXPath("*[@view:mdid='" + oldid + "']");
		if (comment == null || comment.length == 0)
			continue; //error?
		comment = comment[0];
		if (oldid != postjson[oldid]) {
			comment.properties["view:mdid"] = postjson[oldid];
			comment.properties["cm:name"] = postjson[oldid];
		}
		comment.properties["view:committed"] = true;
		comment.properties["view:deleted"] = false;
		comment.save();
	}	
}

main();
model['res'] = "ok";