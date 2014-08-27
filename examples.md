#Example curl commands for exercising the MMS REST API

1) 1st demo

    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BluCamNameListExpr.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456/elements"

    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/_17_0_2_3_e610336_1394148311476_17302_29388/elements"

2) Modepost w/ fix=true

    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/expressionElementsNew.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456/elements?fix=true"

3) Second demo

    curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BLUCamTest.json "http://localhost:8080/alfresco/service/javawebscripts/sites/europa/projects/123456/elements"

    curl -w "\n%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/javawebscripts/views/_17_0_2_3_e610336_1394148233838_91795_29332/elements"
 
