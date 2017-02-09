<import resource="classpath:alfresco/extension/js/json2.js">
<import resource="classpath:alfresco/extension/js/utils.js">

var sites = siteService.listSites(null, null);

var sitesJson =[];
sites.forEach(function (site) {
  var siteJson = {};
  
  var node = site.node;
  var tags = node.tags;
  if (node.name != "no_site") {
	  siteJson["name"] = node.name;
	  siteJson["title"] = site.title;
	  siteJson["categories"] = [];
	  tags.forEach(function(tag) {
		  siteJson["categories"].push(tag);
	  });
	  
	  sitesJson.push(siteJson);
  }
});

var response;
if (status.code == 200) {
    response = jsonUtils.toJSONString(sitesJson);
} else if (status.code == 401) {
    response = "unauthorized";
} else {
    response = "NotFound";
}
model['res'] = response;
