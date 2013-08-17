function guid() {
    function _p8(s) {
        var p = (Math.random().toString(16)+"000000000").substr(2,8);
        return s ? "-" + p.substr(0,4) + "-" + p.substr(4,4) : p ;
    }
    return _p8() + _p8(true) + _p8(true) + _p8();
}

//modelMapping - {mdid: node}
//views - {viewid: [viewid, ...], ...}
//nosections - may be omitted - [viewid, ...]
function updateViewHierarchy(modelMapping, views, nosections) {
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
			cviewnode.properties["view:index"] = ci;
			cviewnode.save();
			pviewnode.createAssociation(cviewnode, "view:views");
		}
		pviewnode.properties["view:viewsJson"] = jsonUtils.toJSONString(cviews);
		if (nosections != undefined && nosections.indexOf(pview) > 0)
			pviewnode.properties["view:noSection"] = true;
		pviewnode.save();
	}
}