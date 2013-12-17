app.datamodel = function() {

	// This is the storage container for all elements.  It is keyed by mdid
	// and functions as a single source of truth
	var pagedata = {};
	var rootView = null;
	var username = null;

	app.dispatcher.on("backend-data-ready", function(sourcedata) {
		//rootView = createView(pagedata.rootView, pagedata);
		//buildViewTree(rootView, pagedata);
		importData(sourcedata);
		app.dispatcher.trigger("datamodel-ready");
	});

	var importData = function(sourcedata) {
		// global data
		user = sourcedata.user;
		isSnapshot = sourcedata.snapshot;
		username = sourcedata.user;
		// create and store a view object for each view
		_.each(sourcedata.views, function(v) {
//TODO: check taht v.mdid doesnt exist
			pagedata[v.mdid] = createView(v, sourcedata.view2view[v.mdid]);
		});
		// store root view
		rootView = pagedata[sourcedata.rootView];
		// create and store an element object for each element
		_.each(sourcedata.elements, function(e) {
//TODO: check taht e.mdid doesnt exist
			pagedata[e.mdid] = createView(e);
		});
	};

	var createView = function(viewData, childViewIds)
	{

		var content = [];
		_.each(viewData.contains, function(e) {
			if(e.type === "Paragraph") {
				content.push(createContainedParagraph(e));
			} else if( e.type === "List") {
				content.push(createContainedList(e));
			} else if(e.type === "Table") {
				content.push(createContainedTable(e));
			} else {
				app.dispatcher.trigger("datamodel-error", "Unsupported view.contains element of type " + e.type + " expected Paragraph, List, or Table");
			}
		});

		return {	
			getViewAttr: function(attr) {
				return viewData[attr];
			},
			getChildIds: function() {
				return childViewIds;
			},
			getContent: function() {
				return content;
			}
		};
	};

	var createContainedParagraph = function(paraData) {
		return {
			getType: function() {
				return paraData.type;
			},
			getValue: function() {
				if(paraData.source === "text") {
					return text;
				} else {
//TODO: Lookup data from element or view based on paraData.useProperty
					return;//return pageData[paraData.source]
				}

			}
		};
	};

	var createContainedList = function(listData) {
		return {
			getType: function() {
				return paraData.type;
			},

		};
	};

	var createContainedTable = function(tableData) {
		return {
			getType: function() {
				return paraData.type;
			},

		};
	};

	var createElement = function(elementData) {
		return {

		};
	};

	return {
		getRootView: function() {
			return rootView;
		},
		isSnapshot: function() {
			return isSnapshot;
		},
		getUsername: function() {
			return username;
		}
	};

}();