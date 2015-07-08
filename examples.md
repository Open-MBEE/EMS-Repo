#Example curl commands for exercising the MMS REST API

The following commands can be tested in the alfresco-view-repo/test-data/javawebscripts directory.  For pairs of commands, the first is a local script invocation that executes the curl command that follows. 

To run all of the following commands in sequence, run command "runexamples"

1) create a project (in master/europa)

	projectpost JsonData/project.json
	curl -w "\n%{http_code}\n" -u admin:admin -X POST -H Content-Type:application/json --data @JsonData/project.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/projects?createSite=true"


2) get a project  (from master/europa)

    projectget 123456
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/projects/123456"
    
3) add elements to project (project name is determined by "owner" of each element in jsonFileName)

	modelpost JsonData/elementsNew.json
	curl -w "\n%{http_code}\n" -u admin:admin -X POST -H Content-Type:application/json --data @JsonData/elementsNew.json "http://localhost:8080/alfresco/service/workspaces/master/elements"

    
4) get elements in a project

	modelget 301 recursive
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/javawebscripts/elements/301?recurse=true"


5) getting an element

	modelget 301
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/301"
         

6) creating a view

	viewpost JsonData/viewsNew.json"
	curl -w "\n%{http_code}\n" -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/viewsNew.json "http://localhost:8080/alfresco/service/workspaces/master/views"

7) getting a view

	viewget 301
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/views/301"


8) changing a view

	viewpost JsonData/viewChange.json
	curl -w "\n%{http_code}\n" -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/viewChange.json "http://localhost:8080/alfresco/service/workspaces/master/views"

9) verifying view changed

	viewget 301
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/views/301"

    
10)a) creating a product

	productpost JsonData/productsNew.json
	curl -w "\n%{http_code}\n" -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/productsNew.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/products"

  b) get product
  
	productget 301
	curl -w "\n%{http_code}\n" -X GET -u admin:admin -H Content-Type:application/json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/products/301"

11) view generation

  a) create a new project
  
	projectpost JsonData/blu.json
	curl -w "\n%{http_code}\n" -u admin:admin -X POST -H Content-Type:application/json --data @JsonData/blu.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/projects?createSite=true"

  b) load model into project
  
	modelpost JsonData/BluCamNameListExpr.json
	curl -w "\n%{http_code}\n" -u admin:admin -X POST -H Content-Type:application/json --data @JsonData/BluCamNameListExpr.json "http://localhost:8080/alfresco/service/workspaces/master/elements"

  c) get the generated content in the view

	viewget _17_0_2_3_e610336_1394148311476_17302_29388
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/views/_17_0_2_3_e610336_1394148311476_17302_29388"

12) View generation with user-defined operations 

  a) load model into project

	modelpost JsonData/BLUCamTest.json
	curl -w "\n%{http_code}\n" -u admin:admin -X POST -H Content-Type:application/json --data @JsonData/BLUCamTest.json "http://localhost:8080/alfresco/service/workspaces/master/elements"

  b) get the generated content in the view

	viewget _17_0_2_3_e610336_1394148233838_91795_29332
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/master/views/_17_0_2_3_e610336_1394148233838_91795_29332"
   
 
13) Create workspace

	wspost ws3
	curl -w "\n%{http_code}\n" -X POST -u admin:admin "http://localhost:8080/alfresco/service/workspaces/ws3?sourceWorkspace=master"
    
  b) create ws4 with ws3 as parent
  
	wspost ws4 ws3
	curl -w "\n%{http_code}\n" -X POST -u admin:admin "http://localhost:8080/alfresco/service/workspaces/ws4?sourceWorkspace=ws3"

14) List workspaces

	wsget
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces"

    wsget ws1
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/workspaces/ws1"

15) Post to workspace (creates workspace if it doesn't exist)

	modelpost ws1 JsonData/303.json
	curl -w "\n%{http_code}\n" -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/303.json "http://localhost:8080/alfresco/service/workspaces/ws1/elements"
    
	modelpost ws2 JsonData/no_id.json
	curl -w "\n%{http_code}\n" -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/no_id.json "http://localhost:8080/alfresco/service/workspaces/ws2/elements"

16) Compare workspaces

	wsdiff master ws1
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/diff?workspace1=master&workspace2=ws1"

	wsdiff ws2 master
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/diff?workspace1=ws2&workspace2=master"

 	wsdiff ws1 ws2
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/diff?workspace1=ws1&workspace2=ws2"

    # now edit 303.json and compare ws1 at the two times
    
	modelpost ws1 JsonData/303.json
	curl -w "\n%{http_code}\n" -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/303.json "http://localhost:8080/alfresco/service/workspaces/ws1/elements"

    # get times between changes
    
	wsdiff ws1 ws1 2014-08-28T07:22:00.000-0800 2014-08-28T07:23:00.000-0800
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/diff?workspace1=ws1&workspace2=ws1&timestamp1=2014-08-28T07:22:00.000-0800&timestamp2=2014-08-28T07:23:00.000-0800"

17) Save/merge workspace

	modelmerge ws1
	curl -w "\n%{http_code}\n" -X POST -u admin:admin "http://localhost:8080/alfresco/service/merge?target=master&source=ws1"
    
	wsdiff master ws1
	curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/diff?workspace1=master&workspace2=ws1"

18) Post expression

	exppost JsonData/operation.json
	curl -w "\n%{http_code}\n" -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/operation.json "http://localhost:8080/alfresco/service/workspaces/master/elements?expression=1%2B1"

19) Get expressions

	modelget arg_ev_33001
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/arg_ev_33001"

	modelget arg_ev_38307_1
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/arg_ev_38307_1"

20) Fixing constraint violations

	fixconstraint JsonData/expressionElementsNew.json
	curl -w "\n%{http_code}\n" -u admin:admin -X POST -H Content-Type:application/json --data @JsonData/expressionElementsNew.json "http://localhost:8080/alfresco/service/workspaces/master/elements?fix=true"

	# Get expressions again to show functionality of fixconstraint

	modelget arg_ev_33001  (where arg_ev_33001 is the sysmlid of a generated Expression)
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/arg_ev_33001"

	modelget arg_ev_38307_1
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/arg_ev_38307_1"

21) Evaluating expressions

	expeval arg_ev_33001
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/arg_ev_33001?evaluate=true"
  
  	expeval arg_ev_38307_1
	curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/arg_ev_38307_1?evaluate=true"
  
NOT YET AVAILABLE
    
22) snapshots

23) configuration sets
    
