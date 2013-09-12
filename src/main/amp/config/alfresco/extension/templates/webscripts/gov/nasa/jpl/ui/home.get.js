<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

var volumes = {};
var volume2volumes = {};
var documents = {};
var volume2documents = {};
var projectVolumes = [];

function handleVolume(volume) {
	volumes[volume.properties['view:mdid']] = volume.properties['view:name'];
	childrenVolumes = volume.assocs['view:volumes'];
	cvs = [];
	for (var i in childrenVolumes) {
		var cv = childrenVolumes[i];
		cvs.push(cv.properties['view:mdid']);
	}
	volume2volumes[volume.properties['view:mdid']] = cvs;
	
	childrenDocuments = volume.assocs['view:documents'];
	cds = [];
	for (var i in childrenDocuments) {
		var cd = childrenDocuments[i];
		cds.push(cd.properties['view:mdid']);
		documents[cd.properties['view:mdid']] = cd.properties['view:name'];
	}
	volume2documents[volume.properties['view:mdid']] = cds;
}

function main() {
	var roots = modelFolder.childrenByXPath("*[@view:rootVolume='true']");
	for (var i in roots) {
		var root = roots[i];
		projectVolumes.push(root.properties['view:mdid']);
		handleVolume(root);
	}
}

status.code = 200;
var project = url.extension;
//var europaSite = siteService.getSite(project).node;
var modelFolder = companyhome.childByNamePath("ViewEditor/model");

main();
var info = {
	"volumes": volumes, 
	"volume2volumes": volume2volumes, 
	"documents": documents, 
	"volume2documents": volume2documents, 
	"projectVolumes": projectVolumes,
	"name": "Europa"
};

var	response = status.code == 200 ? jsonUtils.toJSONString(info) : "NotFound";
if (status.code != 200) {
	status.redirect = true;
	status.message = response;
}
model['res'] = response;