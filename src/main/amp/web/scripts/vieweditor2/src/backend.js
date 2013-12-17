

app.backend = function() {


	var ajaxWithHandlers = function(options, successMessage, errorMessage) {
	$.ajax(options)
		.done(function() { 
			app.dispatcher.trigger("backend-success", successMessage);   
		})
		.fail(function(e) { 
			if (e && e.status && e.status === 200) {
				// we got a 200 back, but json parsing might have failed
				app.dispatcher.trigger("backend-error", errorMessage);   
				return;
			} else {
				if (console && console.log) {
					console.log("ajax error:", e);
				}
				app.dispatcher.trigger("backend-error", errorMessage);    
			}
		});
	};

	var loadDataFromURL = function(url)	{
		ajaxWithHandlers({ 
			type: "GET",
			url: url,
			//data: html,
			contentType: "application/json; charset=UTF-8",
			success: function(result){
				// In some situations result can be a string instead of an object, if this occurse, lets parse it
				if(result.constructor === String)
				{
					try {
						result = JSON.parse(result);
					}
					catch(e) {
						result = {};
						app.dispatcher.trigger("backend-error", "Error parsing json data.  If running in development you will need to specify ?test_mdid={mdid} as a url paramater.");
					}
					
				}
				app.dispatcher.trigger("backend-data-ready", result);
			}
		}, "Success downloaded page data", "Error downloading page data");
	};

	app.dispatcher.on("application-start", function() {
		var jsonURL = '';
		var test_mdid =  $.url().param("test_mdid");
		if(test_mdid !== undefined) {
			// Load test data
			jsonURL = "../test_data/" + test_mdid + ".json";
		}
		else {
			// Read data based on url pattern
			jsonURL = $.url().attr('source') + "?format=json";
		}
		console.log("Loading data from: " + jsonURL);
		loadDataFromURL(jsonURL);
	});

	return {
		loadDataFromURL: loadDataFromURL
	};

}();