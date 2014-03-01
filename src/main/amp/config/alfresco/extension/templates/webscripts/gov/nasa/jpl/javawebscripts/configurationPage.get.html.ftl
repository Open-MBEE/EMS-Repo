<!doctype html>
<html lang="en" ng-app="myApp">
<head>
  <meta charset="utf-8">
  <title>Site Configuration Points</title>
  <link rel="stylesheet" href="/alfresco/scripts/vieweditor/vendor/css/bootstrap.min.css" media="screen">
  <link href="/alfresco/scripts/vieweditor/styles/styles.css" rel="stylesheet" media="screen">
  <link href="/alfresco/scripts/vieweditor/styles/fonts.css" rel="stylesheet">
  <link href="/alfresco/scripts/vieweditor/vendor/css/whhg.css" rel="stylesheet" >
  <link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>
  
</head>
<body ng-init="currentSite = '${site}'">
  <div class="main">
    <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="navbar-header">
        <a class="navbar-brand" href="/alfresco/service/ve/documents/{{currentSite}}">Europa View Editor</a>
      </div>

      <ul class="nav navbar-nav">
        <li><a  href="/share/page/">Europa EMS Dashboard</a></li>
      </ul>   

      <div class="pull-right">
        <img class="europa-icon" src="/alfresco/scripts/vieweditor/images/europa-icon.png" />
      </div>

      <ul class="nav navbar-nav pull-right">
       <li><a href="#" class="submit-logout">logout</a></li>
     </ul>

     <ul class="nav navbar-nav pull-right">
     </ul>
   </nav>
   <div class="wrapper">
    <div class="row" ng-controller="ConfigsCtrl">
      <h3>Configurations for site {{currentSite}}</h3>

      <div class="col-xs-3">
          <div ng-if="messages.message" class="alert alert-info">{{messages.message}}</div>
          <form class="form-inline">
        <input class="form-control" style="width: 49%" type="search" ng-model="configName" placeholder="filter config"/>
        <input class="form-control" style="width: 49%" type="search" ng-model="snapshotName" placeholder="filter doc"/>
      </form>
      <hr/>
        <div ng-show="loading">Loading...</div>
        <div ng-repeat="config in configurations | filter:configName" ng-controller="ConfigCtrl">
          <!-- <button class="btn btn-primary btn-sm" ng-click="toggle()">{{hideButton()}}</button> -->
          <a href=""><h5 style="display: inline" ng-click="toggle()">- {{config.name}} </h5></a>

          <div>{{config.modified | date:'medium'}}</div>
          <div>{{config.description}}</div>

          <ul ng-hide="hide">
            <li ng-repeat="snapshot in config.snapshots | filter:snapshotName">
              <a href="{{snapshot.url}}">{{snapshot.name}}</a>
            </li>
          </ul>
          <button class="btn btn-primary btn-sm" ng-click="toggleChangeForm()">{{toggleChangeFormButton()}}</button>
          <form ng-hide="hideChangeForm" ng-submit="change()">
            <div>Name:</div> <input class="form-control" type="text" ng-model="newConfigName"/>
            <div>Description:</div> <textarea class="form-control" ng-model="newConfigDesc"></textarea>
            <div><input class="btn btn-primary btn-sm" type="submit" value="Change"/>
              </div>
          </form>
          <hr/>
        </div>
      </div>
      <div class="col-xs-3">
        <button class="btn btn-primary btn-sm" ng-click="toggleNewForm()">{{newFormButton()}}</button>
        <form ng-hide="hideNewForm" ng-submit="createNew()">
          <div>Name: </div><input class="form-control" type="text" ng-model="newConfigName"/>
          <div>Description: </div><textarea class="form-control" ng-model="newConfigDesc"></textarea>
          <div><input class="btn btn-primary btn-sm" type="submit" value="Submit"/>
           </div>
        </form>
      </div>
    </div>
  </div>
</div>

  <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.13/angular.min.js"></script>

<!--<script src="lib/angular/angular.js"></script>-->
<script src="/alfresco/scripts/js/app.js"></script>
</body>
</html>
