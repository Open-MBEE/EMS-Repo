<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("ViewEditor/model");

var modelMapping = {};

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	for (var oldid in postjson) {
		var comment = modelFolder.childByNamePath(oldid);
		if (comment == null)
			continue; //error?
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