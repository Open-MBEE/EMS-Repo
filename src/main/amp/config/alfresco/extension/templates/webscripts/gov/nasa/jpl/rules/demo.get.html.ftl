<html>
	<head>
		<title>Demo submission</title>
		<script src="${url.context}/scripts/vieweditor/vendor/jquery.min.js"></script>
		<script src="${url.context}/scripts/vieweditor/vendor/jquery-ui.min.js"></script>
		<script type="text/javascript">
		$(document).ready(function() {
		    $("#submit").click(function (event) {
		    /*
		        var expression = $("#input").val();
		        $.ajax({
		        	url: '${url.context}/wcs/demo',
		        	type: 'POST',
		        	data: JSON.stringify(expression);
		        }
		        ).done(
		        	function(data) {
			          $("#output").val(data);
					}
				);
				*/
				$.post('${url.context}/wcs/demo?expression="' + expression + '"',
				   {data: expression}, 
				   function(data) {
			          $("#output").val(data);
				   });
		    });
		});
		</script>
	</head>
	<body>
		<textarea id="input" rows="5" cols="80"></textarea>
		<br/>
		<button type="submit" id="submit">Submit</button>
		<br/>
		<br/>
		<textarea id="output" rows="5" cols="80"></textarea>
	</body>
</html>