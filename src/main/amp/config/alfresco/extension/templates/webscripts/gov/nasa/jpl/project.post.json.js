<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

//var europaSite = siteService.getSite("europa").node;
var modelFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/model");
var snapshotFolder = companyhome.childByNamePath("Sites/europa/ViewEditor/snapshots");

var modelMapping = {};
var merged = [];

function getOrCreateVolume(vid, name, roots) {
	var vnode = modelFolder.childByNamePath(vid);
	if (vnode == null) {
		vnode = modelFolder.createNode(vid, "view:Volume");
		vnode.properties["view:name"] = name;
		vnode.properties["view:mdid"] = vid;
	} else {
		vnode.properties["view:name"] = name;
	}
	if (roots.indexOf(vid) >= 0)
		vnode.properties["view:rootVolume"] = true;
	else
		vnode.properties["view:rootVolume"] = false;
	vnode.save();
	return vnode;
}

function getOrCreateDocument(did) {
	var dnode = modelFolder.childByNamePath(did);
	if (dnode == null) {
		dnode = modelFolder.createNode(did, "view:DocumentView");
		dnode.properties["view:mdid"] = did;
		dnode.properties["view:name"] = "Unexported Document";
		dnode.save();
	} 
	return dnode;
}

function volume2volume(v2v) {
	for (var pv in v2v) {
		var cvs = v2v[pv];
		var pvnode = modelMapping[pv];
		if (pvnode == null || pvnode == undefined) {
			continue;
		}
		var oldchildren = pvnode.assocs["view:volumes"];
		for (var i in oldchildren) {
			pvnode.removeAssociation(oldchildren[i], "view:volumes");
		}
		for (var ci in cvs) {
			var cvid = cvs[ci];
			var cvnode = modelMapping[cvid];
			if (cvnode == null || cvnode == undefined) {
				continue;
			}
			pvnode.createAssociation(cvnode, "view:volumes");
		}
	}
}

function volume2document(v2d) {
	for (var pv in v2d) {
		var cds = v2d[pv];
		var pvnode = modelMapping[pv];
		if (pvnode == null || pvnode == undefined) {
			continue;
		}
		for (var ci in cds) {
			var cdid = cds[ci];
			var cdnode = modelMapping[cdid];
			if (cdnode == null || cdnode == undefined) {
				continue;
			}
			cleanDocument(cdnode);
			pvnode.createAssociation(cdnode, "view:documents");
		}
		pvnode.save();
	}
}

function main() {
	var postjson = JSON.parse(json.toString());
	if (postjson == null || postjson == undefined)
		return;
	var projectid = url.templateArgs.projectid;
	var volumes = postjson.volumes;
	var documents = postjson.documents;
	var v2v = postjson.volume2volumes;
	var v2d = postjson.volume2documents;
	var roots = postjson.projectVolumes;
	
	for (var vid in volumes) {
		var vnode = getOrCreateVolume(vid, volumes[vid], roots);
		modelMapping[vid] = vnode;
	}
	for (var did in documents) {
		var dnode = getOrCreateDocument(documents[did]);
		modelMapping[documents[did]] = dnode;
	}
	volume2volume(v2v);
	volume2document(v2d);
}

if (UserUtil.hasWebScriptPermissions()) {
    status.code = 200;
    main();
} else {
    status.code = 401;
}

var response;
if (status.code == 200) {
    response = "ok";
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;