model['siteName'] = url.templateArgs.site;
var site = siteService.getSite(model['siteName']);
if (site != null) {
	model['siteTitle'] = site.title;
} else {
	model['siteTitle'] = 'Site Not Found';
}