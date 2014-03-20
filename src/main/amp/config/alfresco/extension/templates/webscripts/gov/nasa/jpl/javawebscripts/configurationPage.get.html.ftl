<!doctype html>
<html lang="en" ng-app="myApp">
<head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
  <meta charset="utf-8">
  <title>View Editor Tags</title>
  <link rel="stylesheet" href="${url.context}/scripts/vieweditor/vendor/css/bootstrap.min.css" media="screen">
  <link href="${url.context}/scripts/vieweditor/styles/styles.css" rel="stylesheet" media="screen">
  <link href="${url.context}/scripts/vieweditor/styles/fonts.css" rel="stylesheet">
  <link href="${url.context}/scripts/vieweditor/vendor/css/whhg.css" rel="stylesheet" >
  <link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>
  
</head>
<body ng-init="currentSite = '${siteName}'">
  <div class="main">
      <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="navbar-header">
    		<a class="navbar-brand" href="/share/page/site/${siteName}/dashboard">${siteTitle}</a>
    	</div>
      <ul class="nav navbar-nav">
        <li class="dropdown" id="firstDropdown">
        	<a href="#" class="dropdown-toggle" data-toggle="dropdown">${siteTitle} DocWeb <b class="caret"></b></a>
        	<ul class="dropdown-menu">
        	<#if siteName == 'europa'>
        		<li><a href="${url.context}/service/ve/index/${siteName}">${siteTitle} Document List</a></li>
        		<li><a href="${url.context}/service/ve/documents/${siteName}">${siteTitle} In-Work Document List</a></li>
        	<#else>
        		<li><a href="${url.context}/service/ve/documents/${siteName}">${siteTitle} Document List</a></li>
        	</#if>
        		<li><a href="/share/page/site/${siteName}/dashboard">${siteTitle} Dashboard</a></li>
   			</ul>
   		</li>
   	  </ul>
      <div class="pull-right">
        <img class="europa-icon" src="${url.context}/scripts/vieweditor/images/europa-icon.png" />
      </div>

      <ul class="nav navbar-nav pull-right">
        <li><a href="#" class="submit-logout">logout</a></li>
      </ul>
    </nav>

   <div class="container" ng-controller="TagsCtrl">
    <div class="row">
      <span class="h3">DocWeb for site ${siteTitle}</span> &nbsp;
      <div ng-if="messages.message" class="alert alert-info">{{messages.message}}</div>
       <!-- <div class="col-xs-4">
          <form class="form-inline">
          <input class="form-control" style="width: 49%" type="search" ng-model="configName" placeholder="filter config"/>
          <input class="form-control" style="width: 49%" type="search" ng-model="snapshotName" placeholder="filter doc"/>
        </form>
      </div> 
      <div class="col-xs-8">
        
      </div> -->
      </div>
      <div class="row">
        <hr/>
      <div class="col-xs-4">
          
     	<div>
            <span class="h5"><a ui-sref="latest">Latest Drafts</a></span>
            <hr/>
         </div>
         
        <div ng-show="loading">Loading...</div>
        <div ng-repeat="config in configurations">
          <!-- <button class="btn btn-primary btn-sm" ng-click="toggle()">{{hideButton()}}</button> -->
          <span class="h5"><a ui-sref="tag({tagId: config.simpleNodeId})">{{config.name}} </a></span><span class="pull-right">{{config.modified | date:'medium'}} </span>

          
          <hr/>

        </div>
        <a ui-sref="new" class="btn btn-primary btn-sm">Create New</a>
      </div>
      <div class="col-xs-8" ui-view>
        
      </div>
    </div>
    </div>
  </div>
</div>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap.min.js"></script>

<script type="text/javascript">
$(document).ready(function() {
	$('a.submit-logout').click(function() {
		window.location.replace('${url.context}/service/logout/info?next=${url.full}');
	});
	$.getJSON('/alfresco/service/rest/sites').done(function(data) {
		var sites = {};
		for (var i = 0; i < data.length; i++) {
			var site = data[i];
			if (site.categories.length == 0)
				site.categories.push("Other");
			for (var j = 0; j < site.categories.length; j++) {
				var cat = site.categories[j];
				if (sites.hasOwnProperty(cat)) {
					sites[cat].push(site);
				} else {
					sites[cat] = [site];
				}
			}
		}
		var stuff = "";
		for (var key in sites) {
			stuff += '<li class="dropdown"><a href="#" class="dropdown-toggle" data-toggle="dropdown">' + key + '<b class="caret"></b></a><ul class="dropdown-menu">';
        	var ssites = sites[key];
			for (var i = 0; i < ssites.length; i++) {
				stuff += '<li><a href="/share/page/site/' + ssites[i].name + '/dashboard">' + ssites[i].title + '</a></li>';
			}
        	stuff += '</ul></li>';
		};
		$('#firstDropdown').after(stuff);
		
	});
});
</script>
  <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.14/angular.min.js"></script>

<!-- <script src="lib/angular/angular.js"></script> -->
<script src="${url.context}/scripts/js/angular-ui-router.js"></script>
<script>
'use strict';


// Declare app level module which depends on filters, and services
angular.module('myApp', ['ui.router'])
  .config(function($stateProvider, $urlRouterProvider){
    $stateProvider
        .state('tag', {
            url: "/tag/:tagId", 
            templateUrl: "${url.context}/scripts/docweb/partials/tag.html",
            controller: 'TagCtrl',
            resolve: {
                config: function(TagService, $stateParams, $rootScope) {
                    return TagService.get($rootScope.currentSite).then(function(result){
                        return result.configurationsMap[$stateParams.tagId];
                    });
                }
            }
        })
        .state('new', {
            url: "/new",
            templateUrl: "${url.context}/scripts/docweb/partials/new.html",
            controller: 'NewCtrl'
        })
        .state('latest', {
            url: "/latest",
            templateUrl: "${url.context}/scripts/docweb/partials/latest.html",
            controller: 'TagsCtrl'
        });

  })
  .controller('TagsCtrl', ["$scope", "$http", "TagService", function($scope, $http, TagService) {
    $scope.messages = {message:""};
    $scope.loading = true;
    TagService.get('${siteName}').then(function(result) {
        $scope.configurations = result.configurations;
        $scope.configurationsMap = result.configurationsMap;
        $scope.products = result.products;
        $scope.snapshotMap = result.snapshotMap;
        $scope.loading = false;
    });
  }])
  .controller('TagCtrl', ["$scope", "$http", "config", function($scope, $http, config) {
    //$scope.config = $scope.configurationsMap[$stateParams.tagId]; //this needs to wait for parent scope to get things when coming from tag url
    $scope.config = config;
    $scope.nodeid = $scope.config.nodeid;
    
    $scope.newConfigName = $scope.config.name;
    $scope.newConfigDesc = $scope.config.description;
    $scope.toggles = {hideChangeForm: true, hideAddRemoveForm: true};
    $scope.toggleChangeForm = function() {
        $scope.toggles.hideChangeForm = !$scope.toggles.hideChangeForm;
    };
    $scope.toggleAddRemoveForm = function() {
        $scope.toggles.hideAddRemoveForm = !$scope.toggles.hideAddRemoveForm;
    };
    $scope.change = function() {
        //$window.alert("sending " + $scope.newConfigName + " " + $scope.newConfigDesc + " " + $scope.nodeid);
        $http.post('${url.context}/service/javawebscripts/configurations/' + '${siteName}', 
            {"name": $scope.newConfigName, "description": $scope.newConfigDesc, "nodeid": $scope.nodeid}).
            success(function(data, status, headers, config) {
                $scope.messages.message = "Change Successful";
                $scope.config.name = $scope.newConfigName;
                $scope.config.description = $scope.newConfigDesc;
                $scope.toggles.hideChangeForm = true;
            }).
            error(function(data, status, headers, config) {
                $scope.messages.message = "Change Failed!";
            });
    };
  }])
  .controller('TagAddRemoveCtrl', ["$scope", "$http", "TagService", function($scope, $http, TagService) {
    $scope.selected = [];
    for (var i = 0; i < $scope.config.snapshots.length; i++) {
        $scope.selected.push($scope.config.snapshots[i].id);
    }
    $scope.update = function() {
        var post = {"nodeid": $scope.config.nodeid, "snapshots": $scope.selected};
        $http.post('${url.context}/service/javawebscripts/configurations/' + '${siteName}', 
            post).
            success(function(data, status, headers, config) {
                $scope.messages.message = "Change Successful";
                //$scope.config.snapshots = result;
                var current = [];
                for (var i = 0; i < $scope.selected.length; i++) {
                    var id = $scope.selected[i];
                    current.push($scope.snapshotMap[id]);
                }
                $scope.config.snapshots = current;
                TagService.updateSnapshots($scope.config.simpleNodeId, current);
                $scope.toggles.hideAddRemoveForm = true;
            }).
            error(function(data, status, headers, config) {
                $scope.messages.message = "Change Failed!";
            });

    };
  }])
  .controller('TagAddRemoveDocCtrl', ["$scope", "$http", function($scope, $http) {
    $scope.showSnapshots = false;
    $scope.toggleShowSnapshots = function() {
        $scope.showSnapshots = !$scope.showSnapshots;
    };
    $scope.snapshots = [];
    for (var i = 0; i < $scope.doc.snapshots.length; i++) {
        var selected = !($scope.selected.indexOf($scope.doc.snapshots[i].id) < 0);
        $scope.snapshots.push({
            id: $scope.doc.snapshots[i].id,
            selected: selected,
            created: $scope.doc.snapshots[i].created,
            url: $scope.doc.snapshots[i].url
        });
    }
    $scope.toggleCheck = function(id) {
        var index = $scope.selected.indexOf(id);
        if (index < 0)
            $scope.selected.push(id);
        else
            $scope.selected.splice(index, 1);
    };

  }])
  .controller('NewCtrl', ["$scope", "$http", function($scope, $http) {
    $scope.newConfigName = "";
    $scope.newConfigDesc = "";
    $scope.selected = [];
    $scope.toggleCheck = function(id) {
        var index = $scope.selected.indexOf(id);
        if (index < 0)
            $scope.selected.push(id);
        else
            $scope.selected.splice(index, 1);
    };
    $scope.new = function() {
        var send = {"name": $scope.newConfigName, "description": $scope.newConfigDesc};
        if ($scope.selected.length > 1)
            send.products = $scope.selected;
        //$window.alert("sending " + $scope.newConfigName + " " + $scope.newConfigDesc);
        $http.post('${url.context}/service/javawebscripts/configurations/' + '${siteName}', send).
            success(function(data, status, headers, config) {
                //$window.alert("success, wait for email");
                $scope.messages.message = "New Configuration Created! Please wait for an email notification.";
            }).
            error(function(data, status, headers, config) {
                $scope.messages.message = "Creating new configuration failed!";
            });
    };

  }])
  .factory('TagService', ["$q", "$http", function($q, $http) {
        var deferred = $q.defer();
        var promise = deferred.promise;
        var result = {};
        var done = false;
        return {
            get: function(currentSite) {
                if (done)
                    return promise;
                $http({method: 'GET', url: '${url.context}/service/javawebscripts/configurations/' + currentSite}).
                    success(function(data, status, headers, config) {
                        result.configurations = data.configurations;
                        result.configurationsMap = {};
                        for (var i = 0; i < result.configurations.length; i++) {
                            var nodeid = result.configurations[i].nodeid.substring(24);
                            result.configurationsMap[nodeid] = result.configurations[i];
                            result.configurations[i].simpleNodeId = nodeid;
                        }
                        result.products = data.products;
                        result.snapshotMap = {};
                        for (var i = 0; i < result.products.length; i++) {
                            for (var j = 0; j < result.products[i].snapshots.length; j++) {
                                result.snapshotMap[result.products[i].snapshots[j].id] = result.products[i].snapshots[j];
                                result.products[i].snapshots[j].name = result.products[i].name;
                            }
                        }
                        deferred.resolve(result);
                        done = true;
                    });
                return promise;
            },
            updateSnapshots: function(configId, snapshots) {
            	result.configurationsMap[configId].snapshots = snapshots;
            }
        }
        
  }]);


</script>
</body>
</html>

