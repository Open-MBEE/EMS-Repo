<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<title>View Editor: Logged out</title>
</head>

<script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js"></script>
	<body>
         <p>Successfully logged out of View Editor. Please reauthenticate to access.</p>
         <p>Go back to logged out <a href="${url.full?substring(url.full?last_index_of('=')+1)}">page</a>.</p>
    </body>
    <script type="text/javascript">
	$(document).ready(function() {
			var userAgent = navigator.userAgent.toLowerCase();
		if (userAgent.indexOf('ie') >= 0) {
		   document.execCommand("ClearAuthenticationCache", "false");
		} else if (userAgent.indexOf('chrome') >= 0 || userAgent.indexOf('firefox') >= 0 || userAgent.indexOf('safari') >= 0) {
			// firefox requires a username, safari injects the username into the reauthentication popup
				$.ajax({
					type: 'GET',
					url: '${url.context}/service/logout',
					success: function (data) {
						window.location.replace('${next}');
					},
					error: function(data) {
					},
					username: 'enterusername',
					password: 'badpassword'
				});
		}
	});
</script>
</html>
