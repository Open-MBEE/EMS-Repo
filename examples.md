#Example curl commands for exercising the MMS REST API

The following commands can be tested from the alfresco-view-repo/test-data/javawebscripts directory.  For pairs of commands, the first is a local script invocation that executes the curl command that follows. 

1) create a project

    projectpost 123456
    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"name":"TEST"}' "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456?fix=true&createSite=true"
    
2) get a project

    projectget 123456
    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456"
    
3) add elements to project

    projectpost 123456 elementsNew.json
    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsNew.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456/elements"

4) get elements in a project

    modelget 123456 recursive
    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/elements/123456?recurse=true"

5) getting an element

    modelget 303
    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/elements/303"

6) creating a view

    viewpost viewsNew.json
    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/viewsNew.json "http://localhost:8080/alfresco/service/javawebscripts/views"

7) getting a view

    viewget 301
    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/301"

8) changing a view

    viewpost viewChange.json
    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/viewChange.json "http://localhost:8080/alfresco/service/javawebscripts/views"

9) verifying view changed, [http://localhost:8080/alfresco/service/javawebscripts/views/301]

    viewget 301
    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/301"
    
10) creating a product

    productpost productsNew.json
    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/productsNew.json "http://localhost:8080/alfresco/service/javawebscripts/products"

11) view generation

  a) create a new project
  
    projectpost blu

  b) load model into project
  
    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BluCamNameListExpr.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/blu/elements"

  c) get the generated content in the view

    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/_17_0_2_3_e610336_1394148311476_17302_29388"


12) View generation with user-defined operations 

  a) load model into project

    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BLUCamTest.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/blu/elements"

  b) get the generated content in the view

    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/_17_0_2_3_e610336_1394148233838_91795_29332"
 
13) Create workspace

    wspost ws1
    curl -w %{http_code}\n -X POST -u admin:admin http://localhost:8080/alfresco/service/workspaces/ws1?sourceWorkspace=master
    wspost ws2 ws1
    curl -w %{http_code}\n -X POST -u admin:admin http://localhost:8080/alfresco/service/workspaces/ws2?sourceWorkspace=ws1

14) List workspaces

    wsget
    curl -w %{http_code}\n -X GET -u admin:admin http://localhost:8080/alfresco/service/workspaces
    wsget ws1
    curl -w %{http_code}\n -X GET -u admin:admin http://localhost:8080/alfresco/service/workspaces/ws1

15) Post to workspace

    modelpost ws1 JsonData/303.json
    curl -w %{http_code}\n -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/303.json http://localhost:8080/alfresco/service/workspaces/ws1/elements
    
    modelpost ws2 JsonData/no_id.json
    curl -w %{http_code}\n -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/no_id.json http://localhost:8080/alfresco/service/workspaces/ws2/elements

16) Compare workspaces

    wsdiff master ws1
    curl -w %{http_code}\n -X GET -u admin:admin http://localhost:8080/alfresco/service/diff?workspace1=master&workspace2=ws1

    wsdiff ws2 master
    curl -w %{http_code}\n -X GET -u admin:admin http://localhost:8080/alfresco/service/diff?workspace1=ws2&workspace2=master

    wsdiff ws1 ws2
    curl -w %{http_code}\n -X GET -u admin:admin http://localhost:8080/alfresco/service/diff?workspace1=ws1&workspace2=ws2

    # edit JsonData/303.json
    modelpost ws1 JsonData/303.json
    curl -w %{http_code}\n -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/303.json http://localhost:8080/alfresco/service/workspaces/ws1/elements

    modelpost ws1 JsonData/303.json
    curl -w %{http_code}\n -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/303.json http://localhost:8080/alfresco/service/workspaces/ws1/elements

    # get times between changes
    wsdiff ws1 ws1 2014-08-28T07:22:00.000-0800 2014-08-28T07:23:00.000-0800
    curl -w %{http_code}\n -X GET -u admin:admin http://localhost:8080/alfresco/service/diff?workspace1=master&workspace2=ws1?timestamp1=2014-08-28T07:22:00.000-0800?timestamp2=2014-08-28T07:23:00.000-0800

17) Post expression

    curl -w %{http_code}\n -X POST -u admin:admin -H Content-Type:application/json --data @JsonData/operations.json http://localhost:8080/alfresco/service/workspaces/master/elements?expression=1%2B1

18) Get expression

    modelget <sysmlid of generated Expression> 

19) Evaluating expressions

    

11) Fixing constraint violations

    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/expressionElementsNew.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456/elements?fix=true"

12) snapshots

13) configuration sets

