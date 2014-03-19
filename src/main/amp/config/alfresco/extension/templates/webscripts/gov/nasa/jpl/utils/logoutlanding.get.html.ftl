<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<title>View Editor: Logged out</title>
		<link rel="stylesheet" href="${url.context}/scripts/vieweditor/vendor/css/bootstrap.min.css" media="screen">
		<link href="${url.context}/scripts/vieweditor/styles/jquery.tocify.css" rel="stylesheet" media="screen">
		<link href="${url.context}/scripts/vieweditor/styles/styles.css" rel="stylesheet" media="screen">
		<link href="${url.context}/scripts/vieweditor/styles/print.css" rel="stylesheet" media="print">
		<link href="${url.context}/scripts/vieweditor/styles/fonts.css" rel="stylesheet">
		<link href="${url.context}/scripts/vieweditor/styles/section-numbering.css" rel="stylesheet">
		<link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>
</head>

	<body class="{{ meta.pageName }} {{ settings.currentWorkspace }}">
<div id="main"></div>

    <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="pull-right">
        <img class="europa-icon" src="${url.context}/scripts/vieweditor/images/europa-icon.png" />
      </div>
    </nav>
    
<script type="text/javascript">
	var docsmenu=document.getElementById('docsmenu');
	docsmenu.onchange = function() {
	 window.open(this.options[this.selectedIndex].value, '_self');
	}
</script>

		<div id="top-alert" class="alert alert-danger alert-dismissable" style="display:none">
		  <button type="button" class="close" proxy-click="hideErrorMessage" aria-hidden="true">&times;</button>
		  <span class="message"></span>
		</div>

		<div class="wrapper">
			<div class="row">
  
  <div class="col-md-10">
    
 <p>Successfully logged out of View Editor. Please reauthenticate to access.</p>
 <p>Go back to logged out <a href="${url.full?substring(url.full?last_index_of('=')+1)}">page</a>.</p>

<script src="${url.context}/scripts/vieweditor/vendor/jquery.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery-ui.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.hotkeys.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap-wysiwyg.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.tocify.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/underscore.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/moment.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap.min.js"></script>
<script type="text/javascript" src="${url.context}/scripts/vieweditor/vendor/Ractive.js"></script>
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
</body>
</html>
