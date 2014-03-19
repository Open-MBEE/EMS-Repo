DS.AlfrescoAdapter = DS.Adapter.extend(
{

	find: function(store, type, id) {
		store.push(type, id, {});
	},
	findAll: function(store, type, id) {
		return [];
	},
	deleteRecord: function(store, type, record) {

	},
	createRecord: function(store, type, record) {
		// call didCreateRecord or didError
	},
	updateRecord: function(store, type, record) {
		// call didCreateRecord or didError
	}
	
});