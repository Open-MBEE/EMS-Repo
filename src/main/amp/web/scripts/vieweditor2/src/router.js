ViewEditor.Router.map(function () {
  this.resource('view', { path: '/' });
});

ViewEditor.ViewRoute = Ember.Route.extend({
	model: function() {
		return this.store.find("view");
	}
});

ViewEditor.ElementRoute = Ember.Route.extend({
	model: function() {
		return this.store.find("element");
	}
});