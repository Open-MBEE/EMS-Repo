<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

var volumes = {};
var volume2volumes = {};
var documents = {};
var volume2documents = {};
var projectVolumes = [];

function handleVolume(volume) {
	volumes[volume.properties['view:mdid']] = volume.properties['view:name'];
	var childrenVolumes = volume.assocs['view:volumes'];
	var cvs = [];
	for (var i in childrenVolumes) {
		var cv = childrenVolumes[i];
		if (cv.hasPermission("Read")) {
    		cvs.push(cv.properties['view:mdid']);
    		handleVolume(cv);
		}
	}
	cvs = cvs.sort(function(a,b) {
		var aname = volumes[a];
		var bname = volumes[b];
		return aname.localeCompare(bname);
	});
	volume2volumes[volume.properties['view:mdid']] = cvs;
	
	var childrenDocuments = volume.assocs['view:documents'];
	var cds = [];
	for (var i in childrenDocuments) {
		var cd = childrenDocuments[i];
		if (cd.hasPermission("Read")) {
    		cds.push(cd.properties['view:mdid']);
    		documents[cd.properties['view:mdid']] = cd.properties['view:name'];
		}
	}
	cds = cds.sort(function(a,b) {
		var aname = documents[a];
		var bname = documents[b];
		return aname.localeCompare(bname);
	});
	volume2documents[volume.properties['view:mdid']] = cds;
}

function main() {
	var roots = modelFolder.childrenByXPath("*[@view:rootVolume='true']");
	for (var i in roots) {
		var root = roots[i];
		if (root.hasPermission("Read")) {
    		projectVolumes.push(root.properties['view:mdid']);
    		handleVolume(root);
		}
	}
	projectVolumes = projectVolumes.sort(function(a,b) {
		var aname = volumes[a];
		var bname = volumes[b];
		return aname.localeCompare(bname);
	});
	
}

status.code = 200;
var project = url.extension;
//var europaSite = siteService.getSite(project).node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");
if (UserUtil.hasWebScriptPermissions()) {
    status.code = 200;
    main();
    var info = {
        "volumes": volumes,
        "volume2volumes": volume2volumes,
        "documents": documents,
        "volume2documents": volume2documents,
        "projectVolumes": projectVolumes,
        "name": "Europa"
    };
} else {
    status.code = 401;
}

/*
var response;
if (status.code == 200) {
    response = jsonUtils.toJSONString(info);
} else {
    switch(status.code) {
    case 401:
        response = "unauthorized";
        break;
    default:
        response = "NotFound";
        break;
    }
    status.redirect = true;
    status.message = response;
}
model['res'] = response;
*/
status.code = 200;
model['res'] = '{}';