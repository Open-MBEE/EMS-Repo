ViewEditor.Router.map(function () {
  //this.resource('elements', { path: '/' });
  this.resource('documents');
  this.resource('document', {path: "document/:document_id"});
  this.resource('views');
  this.resource('view', {path: "view/:view_id"});
  this.resource('elements');
  this.resource('element', {path: "element/:element_id"});
});


// Index route
ViewEditor.IndexRoute = Ember.Route.extend({
  setupController: function(controller) {
    // Set the IndexController's `title`
    //console.log(this.store.findAll('document'));
    //console.log(ViewEditor.Document.find());
    //controller.set('title', "My App");
  }
});

// Document routes
ViewEditor.DocumentsRoute = Ember.Route.extend({
	model: function() {
		return this.store.find("document");
	}
});


// View Routes
ViewEditor.ViewsRoute = Ember.Route.extend({
	model: function() {
		return this.store.find("view");
	}
});

// Element Routes
ViewEditor.ElementsRoute = Ember.Route.extend({
	model: function() {
		return this.store.find("element");
	}
});