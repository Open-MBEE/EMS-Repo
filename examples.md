#Example curl commands for exercising the MMS REST API

1) create a project

    projectpost 123456
    
1) get a project

    projectget 123456
    
2) add elements to project

    projectpost 123456 elementsNew.json

3) get elements in a project

    modelget 123456 recursive 

1) getting an element

    modelget 303

1) creating a view

    viewpost viewsNew.json
     
1) getting a view

    viewget 301      

2) changing a view

    viewpost viewChange.json

3) verifying view changed

    viewget 301


2) view generation

  a) create a new project
  
    projectpost blu

  b) load model into project
  
    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BluCamNameListExpr.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/blu/elements"

  c) get the generated content in the view

    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/_17_0_2_3_e610336_1394148311476_17302_29388"


4) View generation with user-defined operations 

  a) load model into project

    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BLUCamTest.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/blu/elements"

  b) get the generated content in the view

    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/_17_0_2_3_e610336_1394148233838_91795_29332"
 
5) Create workspace

6) List workspaces

7) Post to workspace

8) Compare workspaces

9) Post expression

10) Get expression

11) Evaluating expressions

11) Fixing constraint violations

    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/expressionElementsNew.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456/elements?fix=true"
