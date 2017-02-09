<html>
	<head>
		<title>Demo submission</title>
		<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
        <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.3/jquery-ui.min.js"></script>
		<script type="text/javascript">
		$(document).ready(function() {
		    $("#submit").click(function (event) {
		        var expression = $("#input").val();
		        $.ajax({
		        	url: '${url.context}/service/java_query',
		        	type: 'POST',
		        	data: expression,
		        	contentType: 'text/plain; charset=UTF-8'
		        }
		        ).done(
		        	function(data) {
			          $("#output").val(data);
					}
				);
		    });
            $("#junittest").click(function (event) {
                var expression = $("#input").val();
                var query = $("#input").val();
                var reply = $("#output").val();
                var dotest = "dojunittest";
                $.ajax({
                    url: '${url.context}/wcs/java_query',
                    type: 'POST',
                    data: dotest,
                    contentType: 'text/plain; charset=UTF-8'
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
		<textarea id="output" rows="40" cols="80" readonly="true"></textarea>
        <br/>
        <br/>
        <br/>
        <button type="submit" id="junittest" disabled>Run JUnit tests</button>
        <!-- Why doesn't "disabled" work??! -->
	</body>
</html>
