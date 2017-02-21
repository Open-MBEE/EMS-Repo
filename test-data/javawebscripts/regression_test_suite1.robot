*** Settings ***
Library		OperatingSystem
Library		regression_lib.py
Suite Setup		parse_command_line

*** Variables ***
${evaluate_only}		set_true

*** Test Cases ***
PostSite
	[Documentation]		"Regression Test: 10. Create a project and site"
	${test_num} = 		 Set Variable		10
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			develop2			parsek			
	run curl test		10		PostSite		Create a project and site		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"elements":[{"sysmlid":"PROJECT-123456","name":"JW_TEST","specialization":{"type":"Project"}}]}' "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/projects?createSite=true"		False		None		['test', 'workspaces', 'ws', 'develop', 'develop2', 'parsek']		

PostElementsNew
	[Documentation]		"Regression Test: 20. Post elements to the master branch"
	${test_num} = 		 Set Variable		20
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			develop2			parsek			
	run curl test		20		PostElementsNew		Post elements to the master branch		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsNew.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop', 'develop2', 'parsek']		

PostElementsBadOwners
	[Documentation]		"Regression Test: 21. Post elements to the master branch that have owners that cant be found"
	${test_num} = 		 Set Variable		21
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		21		PostElementsBadOwners		Post elements to the master branch that have owners that cant be found		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/badOwners.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostMultiplicityRedefines
	[Documentation]		"Regression Test: 22. Post elements to the master branch that exercise the multiplicity and redefines attributes of a Property"
	${test_num} = 		 Set Variable		22
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		22		PostMultiplicityRedefines		Post elements to the master branch that exercise the multiplicity and redefines attributes of a Property		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/multiplicityRedefines.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostViews
	[Documentation]		"Regression Test: 30. Post views"
	${test_num} = 		 Set Variable		30
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		30		PostViews		Post views		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/views.json "http://localhost:8080/alfresco/service/workspaces/master/views"		False		None		['test', 'workspaces', 'develop', 'develop2']		

PostProducts
	[Documentation]		"Regression Test: 40. Post products"
	${test_num} = 		 Set Variable		40
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		40		PostProducts		Post products		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/products.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/products"		False		None		['test', 'workspaces', 'develop', 'develop2']		

GetSites
	[Documentation]		"Regression Test: 45. Get sites"
	${test_num} = 		 Set Variable		45
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		45		GetSites		Get sites		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites"		False		None		['test', 'workspaces', 'develop', 'develop2']		

GetProject
	[Documentation]		"Regression Test: 50. Get project"
	${test_num} = 		 Set Variable		50
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		50		GetProject		Get project		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/projects/PROJECT-123456"		False		None		['test', 'workspaces', 'develop', 'develop2']		

GetProjects
	[Documentation]		"Regression Test: 51. Get all projects for master"
	${test_num} = 		 Set Variable		51
	${use_json_diff} =	 Set Variable		True
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		51		GetProjects		Get all projects for master		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/projects"		True		None		['test', 'workspaces', 'develop', 'develop2']		

GetElementsRecursively
	[Documentation]		"Regression Test: 60. Get all elements recursively"
	${test_num} = 		 Set Variable		60
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		60		GetElementsRecursively		Get all elements recursively		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/PROJECT-123456?recurse=true"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetElementsDepth0
	[Documentation]		"Regression Test: 61. Get elements recursively depth 0"
	${test_num} = 		 Set Variable		61
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		61		GetElementsDepth0		Get elements recursively depth 0		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/PROJECT-123456?depth=0"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetElementsDepth1
	[Documentation]		"Regression Test: 62. Get elements recursively depth 1"
	${test_num} = 		 Set Variable		62
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		62		GetElementsDepth1		Get elements recursively depth 1		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/PROJECT-123456?depth=1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetElementsDepth2
	[Documentation]		"Regression Test: 63. Get elements recursively depth 2"
	${test_num} = 		 Set Variable		63
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		63		GetElementsDepth2		Get elements recursively depth 2		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/PROJECT-123456?depth=2"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetElementsDepthAll
	[Documentation]		"Regression Test: 64. Get elements recursively depth -1"
	${test_num} = 		 Set Variable		64
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		64		GetElementsDepthAll		Get elements recursively depth -1		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/PROJECT-123456?depth=-1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetElementsDepthInvalid
	[Documentation]		"Regression Test: 65. Get elements recursively depth invalid"
	${test_num} = 		 Set Variable		65
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		65		GetElementsDepthInvalid		Get elements recursively depth invalid		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/PROJECT-123456?depth=invalid"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetElementsConnected
	[Documentation]		"Regression Test: 66. Get elements that are connected"
	${test_num} = 		 Set Variable		66
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		66		GetElementsConnected		Get elements that are connected		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/300?recurse=true&connected=true"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetElementsRelationship
	[Documentation]		"Regression Test: 67. Get elements that have relationship DirectedRelationship, starting with 302"
	${test_num} = 		 Set Variable		67
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		67		GetElementsRelationship		Get elements that have relationship DirectedRelationship, starting with 302		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/303?recurse=true&connected=true&relationship=DirectedRelationship"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

GetViews
	[Documentation]		"Regression Test: 70. Get views"
	${test_num} = 		 Set Variable		70
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		70		GetViews		Get views		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/views/301"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

GetViewElements
	[Documentation]		"Regression Test: 80. Get view elements"
	${test_num} = 		 Set Variable		80
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		80		GetViewElements		Get view elements		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/views/301/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

GetProducts
	[Documentation]		"Regression Test: 90. Get product"
	${test_num} = 		 Set Variable		90
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		90		GetProducts		Get product		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/products/301"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

GetSearch
	[Documentation]		"Regression Test: 110. Get search"
	${test_num} = 		 Set Variable		110
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			80
	run curl test		110		GetSearch		Get search		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=some*"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		None		None		None		80		

GetSearchPage0
	[Documentation]		"Regression Test: 111. Get search paginated 0"
	${test_num} = 		 Set Variable		111
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"sysmlid"		"owner"		"qualifiedId"		"qualifiedName"		
	[Timeout]			0
	run curl test		111		GetSearchPage0		Get search paginated 0		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/element/search?keyword=some*&maxItems=1&skipCount=0"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"sysmlid"', '"owner"', '"qualifiedId"', '"qualifiedName"']		[]		None		None		None		0		

GetSearchPage1
	[Documentation]		"Regression Test: 112. Get search paginated 1"
	${test_num} = 		 Set Variable		112
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"sysmlid"		"owner"		"qualifiedId"		"qualifiedName"		
	[Timeout]			0
	run curl test		112		GetSearchPage1		Get search paginated 1		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/element/search?keyword=some*&maxItems=1&skipCount=1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"sysmlid"', '"owner"', '"qualifiedId"', '"qualifiedName"']		[]		None		None		None		0		

GetSearchPageBad
	[Documentation]		"Regression Test: 112. Get search paginated bad"
	${test_num} = 		 Set Variable		112
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			0
	run curl test		112		GetSearchPageBad		Get search paginated bad		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/element/search?keyword=some*&maxItems=-1&skipCount=25"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		None		None		None		0		

Delete6666
	[Documentation]		"Regression Test: 120. Delete element 6666"
	${test_num} = 		 Set Variable		120
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		120		Delete6666		Delete element 6666		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/6666"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'develop', 'develop2']		

PostChange
	[Documentation]		"Regression Test: 130. Post changes to directed relationships only (without owners)"
	${test_num} = 		 Set Variable		130
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_to_gv6_delta_gv7
	run curl test		130		PostChange		Post changes to directed relationships only (without owners)		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/directedrelationships.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		None		<function set_read_to_gv6_delta_gv7 at 0x1070f01b8>		

PostConfig
	[Documentation]		"Regression Test: 140. Post configuration"
	${test_num} = 		 Set Variable		140
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		140		PostConfig		Post configuration		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/configuration.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/configurations"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"id"']		['test', 'workspaces', 'develop', 'develop2']		

GetConfig
	[Documentation]		"Regression Test: 150. Get configurations"
	${test_num} = 		 Set Variable		150
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		150		GetConfig		Get configurations		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/configurations"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"id"']		['test', 'workspaces', 'develop', 'develop2']		

PostConfigAgain
	[Documentation]		"Regression Test: 154. Post same configuration again"
	${test_num} = 		 Set Variable		154
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		154		PostConfigAgain		Post same configuration again		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/configuration.json "http://localhost:8080/alfresco/service/workspaces/master/configurations"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"id"']		['test', 'workspaces', 'develop']		

GetConfigAgain
	[Documentation]		"Regression Test: 155. Get configurations"
	${test_num} = 		 Set Variable		155
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		155		GetConfigAgain		Get configurations		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/configurations"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"id"']		['test', 'workspaces', 'develop']		

CreateWorkspace1
	[Documentation]		"Regression Test: 160. Create workspace test 1"
	${test_num} = 		 Set Variable		160
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv1
	run curl test		160		CreateWorkspace1		Create workspace test 1		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsA?sourceWorkspace=master&copyTime=$gv6"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv1 at 0x1070ed758>		

PostProjectWorkspace1
	[Documentation]		"Regression Test: 161. Post project to sync branch version for workspace 1"
	${test_num} = 		 Set Variable		161
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		161		PostProjectWorkspace1		Post project to sync branch version for workspace 1		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"elements":[{"sysmlid":"PROJECT-123456","specialization":{"type":"Project", "projectVersion":"0"}}]}' "http://localhost:8080/alfresco/service/workspaces//$gv1/sites/europa/projects?createSite=true"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

CreateWorkspace2
	[Documentation]		"Regression Test: 162. Create workspace test 2"
	${test_num} = 		 Set Variable		162
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv2
	run curl test		162		CreateWorkspace2		Create workspace test 2		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsB?sourceWorkspace=$gv1&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv2 at 0x1070ed7d0>		

PostProjectWorkspace2
	[Documentation]		"Regression Test: 163. Post project to sync branch version for workspace 2 - sub workspace"
	${test_num} = 		 Set Variable		163
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		163		PostProjectWorkspace2		Post project to sync branch version for workspace 2 - sub workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"elements":[{"sysmlid":"PROJECT-123456","specialization":{"type":"Project", "projectVersion":"0"}}]}' "http://localhost:8080/alfresco/service/workspaces//$gv2/sites/europa/projects?createSite=true"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

CreateWorkspaceWithJson
	[Documentation]		"Regression Test: 164. Create a workspace using a json"
	${test_num} = 		 Set Variable		164
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			do176
	run curl test		164		CreateWorkspaceWithJson		Create a workspace using a json		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/NewWorkspacePost.json "http://localhost:8080/alfresco/service/workspaces/"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop']		None		None		<function do176 at 0x1070ed668>		

ModifyWorkspaceWithJson
	[Documentation]		"Regression Test: 165. Modifies a workspace name/description"
	${test_num} = 		 Set Variable		165
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		165		ModifyWorkspaceWithJson		Modifies a workspace name/description		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '$gv3' "http://localhost:8080/alfresco/service/workspaces/"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop']		

GetWorkspaces
	[Documentation]		"Regression Test: 166. Get workspaces"
	${test_num} = 		 Set Variable		166
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		166		GetWorkspaces		Get workspaces		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"']		['test', 'workspaces', 'develop']		

PostToWorkspace
	[Documentation]		"Regression Test: 167. Post element to workspace"
	${test_num} = 		 Set Variable		167
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv4
	run curl test		167		PostToWorkspace		Post element to workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/x.json "http://localhost:8080/alfresco/service/workspaces/$gv2/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		None		None		<function set_read_to_gv4 at 0x1070edc08>		

CompareWorkspacesForMerge
	[Documentation]		"Regression Test: 168. Compare workspaces for a merge of the second into the first"
	${test_num} = 		 Set Variable		168
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		168		CompareWorkspacesForMerge		Compare workspaces for a merge of the second into the first		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		

CompareWorkspaces
	[Documentation]		"Regression Test: 168.5. Compare workspaces"
	${test_num} = 		 Set Variable		168.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		168.5		CompareWorkspaces		Compare workspaces		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		

CompareWorkspacesForMergeBackground1
	[Documentation]		"Regression Test: 169. Compare workspaces for a merge in the background, this will return that it is in process"
	${test_num} = 		 Set Variable		169
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		169		CompareWorkspacesForMergeBackground1		Compare workspaces for a merge in the background, this will return that it is in process		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background&changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesBackground1
	[Documentation]		"Regression Test: 169.5. Compare workspaces in the background, this will return that it is in process"
	${test_num} = 		 Set Variable		169.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		169.5		CompareWorkspacesBackground1		Compare workspaces in the background, this will return that it is in process		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesBackground2
	[Documentation]		"Regression Test: 170. Compare workspaces in the background again, this will return the results of the background diff"
	${test_num} = 		 Set Variable		170
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			20
	run curl test		170		CompareWorkspacesBackground2		Compare workspaces in the background again, this will return the results of the background diff		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background&changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		20		

CompareWorkspacesBackground2
	[Documentation]		"Regression Test: 170.5. Compare workspaces in the background again, this will return the results of the background diff"
	${test_num} = 		 Set Variable		170.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			20
	run curl test		170.5		CompareWorkspacesBackground2		Compare workspaces in the background again, this will return the results of the background diff		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		20		

CompareWorkspacesGlomForMerge1
	[Documentation]		"Regression Test: 171. Compare workspaces for a merge where there is a initial background diff stored"
	${test_num} = 		 Set Variable		171
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		171		CompareWorkspacesGlomForMerge1		Compare workspaces for a merge where there is a initial background diff stored		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		

CompareWorkspacesGlom1
	[Documentation]		"Regression Test: 171.5. Compare workspaces where there is a initial background diff stored"
	${test_num} = 		 Set Variable		171.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		171.5		CompareWorkspacesGlom1		Compare workspaces where there is a initial background diff stored		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		

PostToWorkspaceForGlom
	[Documentation]		"Regression Test: 172. Post element to workspace"
	${test_num} = 		 Set Variable		172
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		172		PostToWorkspaceForGlom		Post element to workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/glomPost.json "http://localhost:8080/alfresco/service/workspaces/$gv2/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		None		None		None		

CompareWorkspacesGlomForMerge2
	[Documentation]		"Regression Test: 173. Compare workspaces for a merge where there is a initial background diff stored and a change has been made since then."
	${test_num} = 		 Set Variable		173
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		173		CompareWorkspacesGlomForMerge2		Compare workspaces for a merge where there is a initial background diff stored and a change has been made since then.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		

CompareWorkspacesGlom2
	[Documentation]		"Regression Test: 173.5. Compare workspaces where there is a initial background diff stored and a change has been made since then."
	${test_num} = 		 Set Variable		173.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		173.5		CompareWorkspacesGlom2		Compare workspaces where there is a initial background diff stored and a change has been made since then.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		None		

CreateWorkspaceWithBranchTime
	[Documentation]		"Regression Test: 174. Create workspace with a branch time"
	${test_num} = 		 Set Variable		174
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv5
	run curl test		174		CreateWorkspaceWithBranchTime		Create workspace with a branch time		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsT?sourceWorkspace=$gv1&copyTime=$gv4"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv5 at 0x1070ed938>		

PostToWorkspaceWithBranchTime
	[Documentation]		"Regression Test: 175. Post element to workspace with a branch time"
	${test_num} = 		 Set Variable		175
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		175		PostToWorkspaceWithBranchTime		Post element to workspace with a branch time		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/y.json "http://localhost:8080/alfresco/service/workspaces/$gv5/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostToWorkspaceForConflict1
	[Documentation]		"Regression Test: 176. Post element to workspace1 so that we get a conflict"
	${test_num} = 		 Set Variable		176
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		176		PostToWorkspaceForConflict1		Post element to workspace1 so that we get a conflict		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/conflict1.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostToWorkspaceForConflict2
	[Documentation]		"Regression Test: 177. Post element to workspace with a branch time so that we get a conflict"
	${test_num} = 		 Set Variable		177
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		177		PostToWorkspaceForConflict2		Post element to workspace with a branch time so that we get a conflict		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/conflict2.json "http://localhost:8080/alfresco/service/workspaces/$gv5/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostToWorkspaceForMoved
	[Documentation]		"Regression Test: 178. Post element to workspace with a branch time so that we get a moved element"
	${test_num} = 		 Set Variable		178
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		178		PostToWorkspaceForMoved		Post element to workspace with a branch time so that we get a moved element		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/moved.json "http://localhost:8080/alfresco/service/workspaces/$gv5/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostToWorkspaceForTypeChange
	[Documentation]		"Regression Test: 179. Post element to workspace with a branch time so that we get a type change"
	${test_num} = 		 Set Variable		179
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		179		PostToWorkspaceForTypeChange		Post element to workspace with a branch time so that we get a type change		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/typeChange.json "http://localhost:8080/alfresco/service/workspaces/$gv5/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostToWorkspaceForWs1Change
	[Documentation]		"Regression Test: 180. Post element to workspace1 so that we dont detect it in the branch workspace.  Changes 303"
	${test_num} = 		 Set Variable		180
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		180		PostToWorkspaceForWs1Change		Post element to workspace1 so that we dont detect it in the branch workspace.  Changes 303		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/modified303.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

GetElement303
	[Documentation]		"Regression Test: 181. Get element 303"
	${test_num} = 		 Set Variable		181
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		181		GetElement303		Get element 303		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/$gv5/elements/303"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop']		

CompareWorkspacesWithBranchTimeForMerge
	[Documentation]		"Regression Test: 182. Compare workspaces with branch times for a merge"
	${test_num} = 		 Set Variable		182
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		182		CompareWorkspacesWithBranchTimeForMerge		Compare workspaces with branch times for a merge		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv5/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesWithBranchTime
	[Documentation]		"Regression Test: 182.5. Compare workspaces"
	${test_num} = 		 Set Variable		182.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		182.5		CompareWorkspacesWithBranchTime		Compare workspaces		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv5/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

PostToWorkspace3
	[Documentation]		"Regression Test: 183. Post element z to workspace"
	${test_num} = 		 Set Variable		183
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv7
	run curl test		183		PostToWorkspace3		Post element z to workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/z.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		None		None		<function set_read_to_gv7 at 0x1070edd70>		

CreateWorkspaceWithBranchTime2
	[Documentation]		"Regression Test: 184. Create workspace with a branch time using the current time for the branch time"
	${test_num} = 		 Set Variable		184
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv6
	run curl test		184		CreateWorkspaceWithBranchTime2		Create workspace with a branch time using the current time for the branch time		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsT2?sourceWorkspace=$gv1&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv6 at 0x1070ed9b0>		

CompareWorkspacesWithBranchTimesForMerge
	[Documentation]		"Regression Test: 185. Compare workspaces each of which with a branch time and with a modified element on the common parent for a merge"
	${test_num} = 		 Set Variable		185
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		185		CompareWorkspacesWithBranchTimesForMerge		Compare workspaces each of which with a branch time and with a modified element on the common parent for a merge		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv5/$gv6/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesWithBranchTimes
	[Documentation]		"Regression Test: 185.5. Compare workspaces both which have a branch time and with a modified element on the common parent"
	${test_num} = 		 Set Variable		185.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		185.5		CompareWorkspacesWithBranchTimes		Compare workspaces both which have a branch time and with a modified element on the common parent		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv5/$gv6/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesForMergeBackgroundOutdated
	[Documentation]		"Regression Test: 186. Compare workspaces for a merge in the background, this will return that it is outdated"
	${test_num} = 		 Set Variable		186
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		186		CompareWorkspacesForMergeBackgroundOutdated		Compare workspaces for a merge in the background, this will return that it is outdated		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background&changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesBackgroundOutdated
	[Documentation]		"Regression Test: 186.5. Compare workspaces in the background, this will return that it is outdated"
	${test_num} = 		 Set Variable		186.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		186.5		CompareWorkspacesBackgroundOutdated		Compare workspaces in the background, this will return that it is outdated		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesForMergeBackgroundRecalculate
	[Documentation]		"Regression Test: 187. Compare workspaces for a merge in the background, and forces a recalculate on a outdated diff"
	${test_num} = 		 Set Variable		187
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		187		CompareWorkspacesForMergeBackgroundRecalculate		Compare workspaces for a merge in the background, and forces a recalculate on a outdated diff		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background=true&recalculate=true&changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesBackgroundRecalculate
	[Documentation]		"Regression Test: 187.5. Compare workspaces in the background, and forces a recalculate on a outdated diff"
	${test_num} = 		 Set Variable		187.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		187.5		CompareWorkspacesBackgroundRecalculate		Compare workspaces in the background, and forces a recalculate on a outdated diff		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?background=true&recalculate=true&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CreateWorkspaceAgain1
	[Documentation]		"Regression Test: 188. Create workspace for another diff test"
	${test_num} = 		 Set Variable		188
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv1
	run curl test		188		CreateWorkspaceAgain1		Create workspace for another diff test		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsG1?sourceWorkspace=master&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv1 at 0x1070ed758>		

CreateWorkspaceAgain2
	[Documentation]		"Regression Test: 189. Create workspace for another diff test"
	${test_num} = 		 Set Variable		189
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv2
	run curl test		189		CreateWorkspaceAgain2		Create workspace for another diff test		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsG2?sourceWorkspace=master&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv2 at 0x1070ed7d0>		

PostToWorkspaceG1ForCMED533
	[Documentation]		"Regression Test: 190. Post elements to workspace wsG1 for testing CMED-533"
	${test_num} = 		 Set Variable		190
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		190		PostToWorkspaceG1ForCMED533		Post elements to workspace wsG1 for testing CMED-533		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsForBothWorkspaces.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostToWorkspaceG1
	[Documentation]		"Regression Test: 191. Post element to workspace wsG1"
	${test_num} = 		 Set Variable		191
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv3
	run curl test		191		PostToWorkspaceG1		Post element to workspace wsG1		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/x.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		None		None		<function set_read_to_gv3 at 0x1070edb90>		

PostToMaster
	[Documentation]		"Regression Test: 192. Post element to master for a later diff"
	${test_num} = 		 Set Variable		192
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		192		PostToMaster		Post element to master for a later diff		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/y.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostToWorkspaceG2ForCMED533
	[Documentation]		"Regression Test: 193. Post elements to workspace wsG2 for testing CMED-533"
	${test_num} = 		 Set Variable		193
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		193		PostToWorkspaceG2ForCMED533		Post elements to workspace wsG2 for testing CMED-533		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsForBothWorkspaces.json "http://localhost:8080/alfresco/service/workspaces/$gv2/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostToWorkspaceG2
	[Documentation]		"Regression Test: 194. Post element to workspace wsG2"
	${test_num} = 		 Set Variable		194
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv4
	run curl test		194		PostToWorkspaceG2		Post element to workspace wsG2		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/z.json "http://localhost:8080/alfresco/service/workspaces/$gv2/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		None		None		<function set_read_to_gv4 at 0x1070edc08>		

CompareWorkspacesG1G2ForMerge
	[Documentation]		"Regression Test: 195. Compare workspaces wsG1 and wsG2 with timestamps for a merge"
	${test_num} = 		 Set Variable		195
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		195		CompareWorkspacesG1G2ForMerge		Compare workspaces wsG1 and wsG2 with timestamps for a merge		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/$gv3/$gv4?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesG1G2
	[Documentation]		"Regression Test: 195.5. Compare workspaces wsG1 and wsG2 with timestamps"
	${test_num} = 		 Set Variable		195.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		195.5		CompareWorkspacesG1G2		Compare workspaces wsG1 and wsG2 with timestamps		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/$gv3/$gv4?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesG1G2ForMergeBackground
	[Documentation]		"Regression Test: 196. Compare workspaces wsG1 and wsG2 with timestamps for a merge in the background to set up a initial diff for the next test"
	${test_num} = 		 Set Variable		196
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		196		CompareWorkspacesG1G2ForMergeBackground		Compare workspaces wsG1 and wsG2 with timestamps for a merge in the background to set up a initial diff for the next test		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/$gv3/$gv4?background=true&changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"', '"diffTime"']		['test', 'workspaces', 'develop']		

CompareWorkspacesG1G2Background
	[Documentation]		"Regression Test: 196.5. Compare workspaces wsG1 and wsG2 with timestamps in background to set up a initial diff for the next test"
	${test_num} = 		 Set Variable		196.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		196.5		CompareWorkspacesG1G2Background		Compare workspaces wsG1 and wsG2 with timestamps in background to set up a initial diff for the next test		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/$gv3/$gv4?background=true&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"', '"diffTime"']		['test', 'workspaces', 'develop']		

CompareWorkspacesG1G2ForMergeGlom
	[Documentation]		"Regression Test: 197. Compare workspaces wsG1 and wsG2 with timestamps for a merge with an initial diff"
	${test_num} = 		 Set Variable		197
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		197		CompareWorkspacesG1G2ForMergeGlom		Compare workspaces wsG1 and wsG2 with timestamps for a merge with an initial diff		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/$gv3/$gv4?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

CompareWorkspacesG1G2Glom
	[Documentation]		"Regression Test: 197.5. Compare workspaces wsG1 and wsG2 with timestamps with a initial diff"
	${test_num} = 		 Set Variable		197.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		197.5		CompareWorkspacesG1G2Glom		Compare workspaces wsG1 and wsG2 with timestamps with a initial diff		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/$gv3/$gv4?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

RecursiveGetOnWorkspaces
	[Documentation]		"Regression Test: 198. Makes sure that a recursive get on a modified workspace returns the modified elements"
	${test_num} = 		 Set Variable		198
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		198		RecursiveGetOnWorkspaces		Makes sure that a recursive get on a modified workspace returns the modified elements		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/302?recurse=true"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

PostSiteInWorkspace
	[Documentation]		"Regression Test: 199. Create a project and site in a workspace"
	${test_num} = 		 Set Variable		199
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		199		PostSiteInWorkspace		Create a project and site in a workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"elements":[{"sysmlid":"proj_id_001","name":"PROJ_1","specialization":{"type":"Project"}}]}' "http://localhost:8080/alfresco/service/workspaces/$gv1/sites/site_in_ws/projects?createSite=true"		False		None		['test', 'workspaces', 'develop']		

GetSiteInWorkspace
	[Documentation]		"Regression Test: 200. Get site in workspace"
	${test_num} = 		 Set Variable		200
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		200		GetSiteInWorkspace		Get site in workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/$gv1/sites"		False		None		['test', 'workspaces', 'develop']		

GetProductsInSiteInWorkspace
	[Documentation]		"Regression Test: 201. Get products for a site in a workspace"
	${test_num} = 		 Set Variable		201
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		201		GetProductsInSiteInWorkspace		Get products for a site in a workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/$gv1/sites/europa/products"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostNotInPastToWorkspace
	[Documentation]		"Regression Test: 202. Post element to master workspace for a diff test"
	${test_num} = 		 Set Variable		202
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_delta_to_gv1
	[Timeout]			10
	run curl test		202		PostNotInPastToWorkspace		Post element to master workspace for a diff test		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/notInThePast.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		None		<function set_read_delta_to_gv1 at 0x1070f0320>		10		

CompareWorkspacesForMergeNotInPast
	[Documentation]		"Regression Test: 203. Compare workspace master with itself for a merge at the current time and a time in the past"
	${test_num} = 		 Set Variable		203
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		203		CompareWorkspacesForMergeNotInPast		Compare workspace master with itself for a merge at the current time and a time in the past		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/latest/$gv1?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop', 'develop2']		

CompareWorkspacesNotInPast
	[Documentation]		"Regression Test: 203.5. Compare workspace master with itself at the current time and a time in the past"
	${test_num} = 		 Set Variable		203.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		203.5		CompareWorkspacesNotInPast		Compare workspace master with itself at the current time and a time in the past		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/latest/$gv1?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop', 'develop2']		

CompareWorkspacesForMergeNotInPastBackground
	[Documentation]		"Regression Test: 204. Compare workspace master with itself for a merge at the current time and a time in the past in the background"
	${test_num} = 		 Set Variable		204
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		204		CompareWorkspacesForMergeNotInPastBackground		Compare workspace master with itself for a merge at the current time and a time in the past in the background		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/latest/$gv1?background&changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop', 'develop2']		

CompareWorkspacesNotInPastBackground
	[Documentation]		"Regression Test: 204.5. Compare workspace master with itself at the current time and a time in the past in the background"
	${test_num} = 		 Set Variable		204.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		204.5		CompareWorkspacesNotInPastBackground		Compare workspace master with itself at the current time and a time in the past in the background		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/latest/$gv1?background&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"diffTime"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop', 'develop2']		

CreateParentWorkspace
	[Documentation]		"Regression Test: 205. Create a workspace to be a parent of another"
	${test_num} = 		 Set Variable		205
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_wsid_to_gv1
	run curl test		205		CreateParentWorkspace		Create a workspace to be a parent of another		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/parentWorkspace1?sourceWorkspace=master&copyTime=$gv1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop', 'develop2']		None		None		<function set_wsid_to_gv1 at 0x1070ed758>		

PostToMasterAgain
	[Documentation]		"Regression Test: 206. Post new element to master"
	${test_num} = 		 Set Variable		206
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_delta_to_gv2
	run curl test		206		PostToMasterAgain		Post new element to master		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/a.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		None		<function set_read_delta_to_gv2 at 0x1070f0398>		

CreateSubworkspace
	[Documentation]		"Regression Test: 207. Create workspace inside a workspace"
	${test_num} = 		 Set Variable		207
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_wsid_to_gv3
	[Timeout]			30
	run curl test		207		CreateSubworkspace		Create workspace inside a workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/subworkspace1?sourceWorkspace=$gv1&copyTime=$gv2"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"']		['test', 'workspaces', 'develop', 'develop2']		None		None		<function set_wsid_to_gv3 at 0x1070ed848>		30		

GetElementInMasterFromSubworkspace
	[Documentation]		"Regression Test: 208. Get an element that only exists in the master from a subworkspace after its parent branch was created but before the it was created, it wont find the element"
	${test_num} = 		 Set Variable		208
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		208		GetElementInMasterFromSubworkspace		Get an element that only exists in the master from a subworkspace after its parent branch was created but before the it was created, it wont find the element		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/$gv3/elements/a"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostAToMaster
	[Documentation]		"Regression Test: 209. Post element a to master."
	${test_num} = 		 Set Variable		209
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		209		PostAToMaster		Post element a to master.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/a.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

CreateAParentWorkspace
	[Documentation]		"Regression Test: 210. Create a "parent" workspace off of master.."
	${test_num} = 		 Set Variable		210
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		210		CreateAParentWorkspace		Create a "parent" workspace off of master..		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace?sourceWorkspace=master"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'develop']		

PostBToMaster
	[Documentation]		"Regression Test: 211. Post element b to master."
	${test_num} = 		 Set Variable		211
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		211		PostBToMaster		Post element b to master.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/b.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

PostCToParent
	[Documentation]		"Regression Test: 212. Post element c to the parent workspace."
	${test_num} = 		 Set Variable		212
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		212		PostCToParent		Post element c to the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/c.json "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

CreateASubWorkspace
	[Documentation]		"Regression Test: 213. Create a "subworkspace" workspace off of the parent."
	${test_num} = 		 Set Variable		213
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		213		CreateASubWorkspace		Create a "subworkspace" workspace off of the parent.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/theSubworkspace?sourceWorkspace=theParentWorkspace"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"']		['test', 'develop']		

PostDToMaster
	[Documentation]		"Regression Test: 214. Post element d to master."
	${test_num} = 		 Set Variable		214
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		214		PostDToMaster		Post element d to master.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/d.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

PostEToParent
	[Documentation]		"Regression Test: 215. Post element e to the parent workspace."
	${test_num} = 		 Set Variable		215
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		215		PostEToParent		Post element e to the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/e.json "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

PostFToSubworkspace
	[Documentation]		"Regression Test: 216. Post element f to the subworkspace."
	${test_num} = 		 Set Variable		216
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		216		PostFToSubworkspace		Post element f to the subworkspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/f.json "http://localhost:8080/alfresco/service/workspaces/theSubworkspace/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetAInMaster
	[Documentation]		"Regression Test: 217. Get element a in the master workspace."
	${test_num} = 		 Set Variable		217
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		217		GetAInMaster		Get element a in the master workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/a"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetAInParent
	[Documentation]		"Regression Test: 218. Get element a in the parent workspace."
	${test_num} = 		 Set Variable		218
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		218		GetAInParent		Get element a in the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements/a"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetAInSubworkspace
	[Documentation]		"Regression Test: 219. Get element a in the subworkspace."
	${test_num} = 		 Set Variable		219
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		219		GetAInSubworkspace		Get element a in the subworkspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theSubworkspace/elements/a"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetBInMaster
	[Documentation]		"Regression Test: 220. Get element b in the master workspace."
	${test_num} = 		 Set Variable		220
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		220		GetBInMaster		Get element b in the master workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/b"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetBInParent
	[Documentation]		"Regression Test: 221. Get element b in the parent workspace."
	${test_num} = 		 Set Variable		221
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		221		GetBInParent		Get element b in the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements/b"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetBInSubworkspace
	[Documentation]		"Regression Test: 222. Get element b in the subworkspace."
	${test_num} = 		 Set Variable		222
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		222		GetBInSubworkspace		Get element b in the subworkspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theSubworkspace/elements/b"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetCInMaster
	[Documentation]		"Regression Test: 223. Get element c in the master workspace."
	${test_num} = 		 Set Variable		223
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		223		GetCInMaster		Get element c in the master workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/c"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetCInParent
	[Documentation]		"Regression Test: 224. Get element c in the parent workspace."
	${test_num} = 		 Set Variable		224
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		224		GetCInParent		Get element c in the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements/c"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetCInSubworkspace
	[Documentation]		"Regression Test: 225. Get element c in the subworkspace."
	${test_num} = 		 Set Variable		225
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		225		GetCInSubworkspace		Get element c in the subworkspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theSubworkspace/elements/c"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetDInMaster
	[Documentation]		"Regression Test: 226. Get element d in the master workspace."
	${test_num} = 		 Set Variable		226
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		226		GetDInMaster		Get element d in the master workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/d"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetDInParent
	[Documentation]		"Regression Test: 227. Get element d in the parent workspace."
	${test_num} = 		 Set Variable		227
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		227		GetDInParent		Get element d in the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements/d"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetDInSubworkspace
	[Documentation]		"Regression Test: 228. Get element d in the subworkspace."
	${test_num} = 		 Set Variable		228
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		228		GetDInSubworkspace		Get element d in the subworkspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theSubworkspace/elements/d"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetEInMaster
	[Documentation]		"Regression Test: 229. Get element e in the master workspace."
	${test_num} = 		 Set Variable		229
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		229		GetEInMaster		Get element e in the master workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/e"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetEInParent
	[Documentation]		"Regression Test: 230. Get element e in the parent workspace."
	${test_num} = 		 Set Variable		230
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		230		GetEInParent		Get element e in the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements/e"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetEInSubworkspace
	[Documentation]		"Regression Test: 231. Get element e in the subworkspace."
	${test_num} = 		 Set Variable		231
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		231		GetEInSubworkspace		Get element e in the subworkspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theSubworkspace/elements/e"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetFInMaster
	[Documentation]		"Regression Test: 232. Get element f in the master workspace."
	${test_num} = 		 Set Variable		232
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		232		GetFInMaster		Get element f in the master workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/f"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetFInParent
	[Documentation]		"Regression Test: 233. Get element f in the parent workspace."
	${test_num} = 		 Set Variable		233
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		233		GetFInParent		Get element f in the parent workspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theParentWorkspace/elements/f"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

GetFInSubworkspace
	[Documentation]		"Regression Test: 234. Get element f in the subworkspace."
	${test_num} = 		 Set Variable		234
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		234		GetFInSubworkspace		Get element f in the subworkspace.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/theSubworkspace/elements/f"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop']		

CompareMasterAToLatestForMerge
	[Documentation]		"Regression Test: 235. Compare master to itself for a merge between post time of a and latest"
	${test_num} = 		 Set Variable		235
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		235		CompareMasterAToLatestForMerge		Compare master to itself for a merge between post time of a and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/2015-08-24T08:46:58.502-0700/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterAToLatest
	[Documentation]		"Regression Test: 235.5. Compare master to itself between post time of a and latest"
	${test_num} = 		 Set Variable		235.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		235.5		CompareMasterAToLatest		Compare master to itself between post time of a and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/2015-08-24T08:46:58.502-0700/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterBToLatestForMerge
	[Documentation]		"Regression Test: 236. Compare master to itself for a merge between the post times of b and latest"
	${test_num} = 		 Set Variable		236
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		236		CompareMasterBToLatestForMerge		Compare master to itself for a merge between the post times of b and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/2015-08-27T15:40:26.891-0700/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterBToLatest
	[Documentation]		"Regression Test: 236.5. Compare master to itself between the post times of b and latest"
	${test_num} = 		 Set Variable		236.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		236.5		CompareMasterBToLatest		Compare master to itself between the post times of b and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/master/2015-08-27T15:40:26.891-0700/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterParentLatestToLatestForMerge
	[Documentation]		"Regression Test: 237. Compare master to theParentWorkspace for a merge with timepoints latest and latest"
	${test_num} = 		 Set Variable		237
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		237		CompareMasterParentLatestToLatestForMerge		Compare master to theParentWorkspace for a merge with timepoints latest and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/theParentWorkspace/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterParentLatestToLatest
	[Documentation]		"Regression Test: 237.5. Compare master to theParentWorkspace with timepoints latest and latest"
	${test_num} = 		 Set Variable		237.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		237.5		CompareMasterParentLatestToLatest		Compare master to theParentWorkspace with timepoints latest and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/theParentWorkspace/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterParentBranchTimeToLatest
	[Documentation]		"Regression Test: 238. Compare master to theParentWorkspace with timepoints at creation of parent and latest"
	${test_num} = 		 Set Variable		238
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		238		CompareMasterParentBranchTimeToLatest		Compare master to theParentWorkspace with timepoints at creation of parent and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/theParentWorkspace/2015-08-24T08:47:10.054-0700/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterParentBranchTimeToLatestForMerge
	[Documentation]		"Regression Test: 238.5. Compare master to theParentWorkspace with timepoints for a merge at creation of parent and latest"
	${test_num} = 		 Set Variable		238.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		238.5		CompareMasterParentBranchTimeToLatestForMerge		Compare master to theParentWorkspace with timepoints for a merge at creation of parent and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/theParentWorkspace/2015-08-24T08:47:10.054-0700/latest&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterSubworkspaceLatestToLatestForMerge
	[Documentation]		"Regression Test: 239. Compare master to theSubworkspace for a merge with timepoints at latest and latest"
	${test_num} = 		 Set Variable		239
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		239		CompareMasterSubworkspaceLatestToLatestForMerge		Compare master to theSubworkspace for a merge with timepoints at latest and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/theSubworkspace/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

CompareMasterSubworkspaceLatestToLatest
	[Documentation]		"Regression Test: 239.5. Compare master to theSubworkspace with timepoints at latest and latest"
	${test_num} = 		 Set Variable		239.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	run curl test		239.5		CompareMasterSubworkspaceLatestToLatest		Compare master to theSubworkspace with timepoints at latest and latest		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/theSubworkspace/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		[]		

TurnOnExpressionStuff
	[Documentation]		"Regression Test: 254. Make sure switch is turned on for handling expressions in viewpoints, etc."
	${test_num} = 		 Set Variable		254
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			develop			workspaces			
	run curl test		254		TurnOnExpressionStuff		Make sure switch is turned on for handling expressions in viewpoints, etc.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/flags/viewpointExpressions?on"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop', 'workspaces']		

SolveConstraint
	[Documentation]		"Regression Test: 255. Post expressions with a constraint and solve for the constraint."
	${test_num} = 		 Set Variable		255
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"modifier"		"message"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			develop			workspaces			
	run curl test		255		SolveConstraint		Post expressions with a constraint and solve for the constraint.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/expressionElementsNew.json "http://localhost:8080/alfresco/service/workspaces/master/elements?fix=true"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"modifier"', '"message"', '"qualifiedId"']		['test', 'develop', 'workspaces']		

PostDemo1
	[Documentation]		"Regression Test: 260. Post data for demo 1 of server side docgen"
	${test_num} = 		 Set Variable		260
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"sysmlid"		"qualifiedId"		"message"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		260		PostDemo1		Post data for demo 1 of server side docgen		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BluCamNameListExpr.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"sysmlid"', '"qualifiedId"', '"message"']		['test', 'workspaces', 'develop', 'develop2']		

Demo1
	[Documentation]		"Regression Test: 270. Server side docgen demo 1"
	${test_num} = 		 Set Variable		270
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		270		Demo1		Server side docgen demo 1		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/views/_17_0_2_3_e610336_1394148311476_17302_29388"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostDemo2
	[Documentation]		"Regression Test: 280. Post data for demo 2 of server side docgen"
	${test_num} = 		 Set Variable		280
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		280		PostDemo2		Post data for demo 2 of server side docgen		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/BLUCamTest.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop', 'develop2']		

Demo2
	[Documentation]		"Regression Test: 290. Server side docgen demo 2"
	${test_num} = 		 Set Variable		290
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		290		Demo2		Server side docgen demo 2		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/views/_17_0_2_3_e610336_1394148233838_91795_29332"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostSiteAndProject3
	[Documentation]		"Regression Test: 292. Create a site and a project for demo 3 of server side docgen"
	${test_num} = 		 Set Variable		292
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		292		PostSiteAndProject3		Create a site and a project for demo 3 of server side docgen		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"elements":[{"sysmlid":"PROJECT-71724d08-6d79-42b2-b9ec-dc39f20a3660","name":"BikeProject","specialization":{"type":"Project"}}]}' "http://localhost:8080/alfresco/service/workspaces/master/sites/demo3site/projects?createSite=true"		False		None		['test', 'workspaces', 'develop', 'develop2']		

PostDemo3
	[Documentation]		"Regression Test: 293. Post data for demo 3 of server side docgen"
	${test_num} = 		 Set Variable		293
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		293		PostDemo3		Post data for demo 3 of server side docgen		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/bike.json "http://localhost:8080/alfresco/service/workspaces/master/sites/demo3site/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop', 'develop2']		

PostViewDemo3
	[Documentation]		"Regression Test: 294. Post view data for demo 3 of server side docgen"
	${test_num} = 		 Set Variable		294
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		294		PostViewDemo3		Post view data for demo 3 of server side docgen		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/connectorView.json "http://localhost:8080/alfresco/service/workspaces/master/sites/demo3site/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"MMS_', 'MMS_']		['test', 'workspaces', 'develop', 'develop2']		

Demo3
	[Documentation]		"Regression Test: 295. Server side docgen demo 3"
	${test_num} = 		 Set Variable		295
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		295		Demo3		Server side docgen demo 3		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/views/_17_0_2_3_e610336_1394148311476_17302_29388_X"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TurnOffExpressionStuff
	[Documentation]		"Regression Test: 297. Make sure switch is turned off for handling expressions in viewpoints, etc."
	${test_num} = 		 Set Variable		297
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			develop			workspaces			
	run curl test		297		TurnOffExpressionStuff		Make sure switch is turned off for handling expressions in viewpoints, etc.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/flags/viewpointExpressions?off"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'develop', 'workspaces']		

GetSites2
	[Documentation]		"Regression Test: 300. Get all the sites for a workspace"
	${test_num} = 		 Set Variable		300
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		300		GetSites2		Get all the sites for a workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites"		False		None		['test', 'workspaces', 'develop', 'develop2']		

GetProductViews
	[Documentation]		"Regression Test: 310. Get all views for a product"
	${test_num} = 		 Set Variable		310
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		310		GetProductViews		Get all views for a product		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/products/301/views"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostElementX
	[Documentation]		"Regression Test: 320. Post element to the master branch/site"
	${test_num} = 		 Set Variable		320
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_to_gv7
	run curl test		320		PostElementX		Post element to the master branch/site		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/x.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		None		<function set_read_to_gv7 at 0x1070edd70>		

UpdateProject
	[Documentation]		"Regression Test: 330. Update a project"
	${test_num} = 		 Set Variable		330
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		330		UpdateProject		Update a project		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"elements":[{"sysmlid":"PROJECT-123456","name":"JW_TEST2","specialization":{"type":"Project","projectVersion":"1"}}]}' "http://localhost:8080/alfresco/service/workspaces/master/projects"		False		None		['test', 'workspaces', 'develop', 'develop2']		

GetProjectOnly
	[Documentation]		"Regression Test: 340. Get project w/o specifying the site"
	${test_num} = 		 Set Variable		340
	${use_json_diff} =	 Set Variable		True
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		340		GetProjectOnly		Get project w/o specifying the site		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/projects/PROJECT-123456"		True		None		['test', 'workspaces', 'develop', 'develop2']		

PostArtifact
	[Documentation]		"Regression Test: 350. Post artifact to the master branch"
	${test_num} = 		 Set Variable		350
	${use_json_diff} =	 Set Variable		True
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		350		PostArtifact		Post artifact to the master branch		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type: multipart/form-data;" --form "file=@JsonData/x.json" --form "title=JsonData/x.json" --form "desc=stuffs" --form "content=@JsonData/x.json" http://localhost:8080/alfresco/service/workspaces/master/sites/europa/artifacts/folder1/folder2/xartifact		True		None		['test', 'workspaces', 'develop', 'develop2']		

GetArtifact
	[Documentation]		"Regression Test: 360. Get artifact from the master branch"
	${test_num} = 		 Set Variable		360
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		360		GetArtifact		Get artifact from the master branch		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/artifacts/xartifact?extension=svg&cs=3463563326"		False		['"url"']		['test', 'workspaces', 'develop', 'develop2']		

CreateWorkspaceDelete1
	[Documentation]		"Regression Test: 370. Create workspace to be deleted"
	${test_num} = 		 Set Variable		370
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			develop			
	[Teardown]			set_wsid_to_gv1
	run curl test		370		CreateWorkspaceDelete1		Create workspace to be deleted		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/AA?sourceWorkspace=master&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"parent"', '"id"', '"qualifiedId"', '"branched"']		['test', 'develop']		None		None		<function set_wsid_to_gv1 at 0x1070ed758>		

CreateWorkspaceDelete2
	[Documentation]		"Regression Test: 380. Create workspace to be deleted"
	${test_num} = 		 Set Variable		380
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		380		CreateWorkspaceDelete2		Create workspace to be deleted		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/BB?sourceWorkspace=$gv1&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"parent"', '"id"', '"qualifiedId"', '"branched"']		['test', 'develop']		

DeleteWorkspace
	[Documentation]		"Regression Test: 390. Delete workspace and its children"
	${test_num} = 		 Set Variable		390
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		390		DeleteWorkspace		Delete workspace and its children		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"parent"', '"id"', '"qualifiedId"']		['test', 'develop']		

CheckDeleted1
	[Documentation]		"Regression Test: 400. Make sure that AA and its children no longer show up in workspaces"
	${test_num} = 		 Set Variable		400
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			
	run curl test		400		CheckDeleted1		Make sure that AA and its children no longer show up in workspaces		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"parent"', '"id"', '"qualifiedId"', '"branched"']		['test']		

CheckDeleted2
	[Documentation]		"Regression Test: 410. Make sure that AA and its children show up in deleted"
	${test_num} = 		 Set Variable		410
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			develop			
	run curl test		410		CheckDeleted2		Make sure that AA and its children show up in deleted		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces?deleted"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"parent"', '"id"', '"qualifiedId"', '"branched"']		['test', 'develop']		

UnDeleteWorkspace
	[Documentation]		"Regression Test: 420. Undelete workspace"
	${test_num} = 		 Set Variable		420
	${use_json_diff} =	 Set Variable		False
	${branch_names} =	 Set Variable		test			develop			
	run curl test		420		UnDeleteWorkspace		Undelete workspace		echo "temporary placeholder\%{http_code}rary placeholder"		False		None		['test', 'develop']		

PostSitePackage
	[Documentation]		"Regression Test: 430. Create a site package"
	${test_num} = 		 Set Variable		430
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	run curl test		430		PostSitePackage		Create a site package		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/SitePackage.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		[]		

PostElementSitePackage
	[Documentation]		"Regression Test: 440. Post a product to a site package"
	${test_num} = 		 Set Variable		440
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"message"		
	run curl test		440		PostElementSitePackage		Post a product to a site package		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/ElementSitePackage.json "http://localhost:8080/alfresco/service/workspaces/master/sites/site_package/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"message"']		[]		

GetSitePackageProducts
	[Documentation]		"Regression Test: 450. Get site package products"
	${test_num} = 		 Set Variable		450
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	run curl test		450		GetSitePackageProducts		Get site package products		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/site_package/products"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		[]		

SitePackageBugTest1
	[Documentation]		"Regression Test: 451. Create packages A2, B2, and C2, where A2/B2 are site packages for CMED-871 testing"
	${test_num} = 		 Set Variable		451
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	run curl test		451		SitePackageBugTest1		Create packages A2, B2, and C2, where A2/B2 are site packages for CMED-871 testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/SitePkgBugTest1.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		[]		

SitePackageBugTest2
	[Documentation]		"Regression Test: 452. Moves package B2 under package C2 for CMED-871 testing"
	${test_num} = 		 Set Variable		452
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	run curl test		452		SitePackageBugTest2		Moves package B2 under package C2 for CMED-871 testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/SitePkgBugTest2.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		[]		

SitePackageBugTest3
	[Documentation]		"Regression Test: 453. Makes package C2 a site package for CMED-871 testing"
	${test_num} = 		 Set Variable		453
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	run curl test		453		SitePackageBugTest3		Makes package C2 a site package for CMED-871 testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/SitePkgBugTest3.json "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		[]		

PostContentModelUpdates
	[Documentation]		"Regression Test: 460. Post content model udpates for sysml 2.0"
	${test_num} = 		 Set Variable		460
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		460		PostContentModelUpdates		Post content model udpates for sysml 2.0		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/contentModelUpdates.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostDuplicateSysmlNames1
	[Documentation]		"Regression Test: 470. Post a element that will be used in the next test to generate a error"
	${test_num} = 		 Set Variable		470
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		470		PostDuplicateSysmlNames1		Post a element that will be used in the next test to generate a error		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/cmed416_1.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostDuplicateSysmlNames2
	[Documentation]		"Regression Test: 480. Post a element with the same type, sysml name, and parent as the previous test to generate at error"
	${test_num} = 		 Set Variable		480
	${use_json_diff} =	 Set Variable		False
	run curl test		480		PostDuplicateSysmlNames2		Post a element with the same type, sysml name, and parent as the previous test to generate at error		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/cmed416_2.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		False		None		[]		

PostModelForDowngrade
	[Documentation]		"Regression Test: 500. Post model for downgrade test"
	${test_num} = 		 Set Variable		500
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		500		PostModelForDowngrade		Post model for downgrade test		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/productsDowngrade.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostModelForViewDowngrade
	[Documentation]		"Regression Test: 510. Post model for view downgrade"
	${test_num} = 		 Set Variable		510
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		510		PostModelForViewDowngrade		Post model for view downgrade		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/viewDowngrade.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostModelForElementDowngrade
	[Documentation]		"Regression Test: 520. Post model for element downgrade"
	${test_num} = 		 Set Variable		520
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_to_gv7
	run curl test		520		PostModelForElementDowngrade		Post model for element downgrade		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementDowngrade.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		None		<function set_read_to_gv7 at 0x1070edd70>		

DiffWorkspaceCreate1
	[Documentation]		"Regression Test: 530. Diff Workspace Test - Create WS 1"
	${test_num} = 		 Set Variable		530
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv1
	run curl test		530		DiffWorkspaceCreate1		Diff Workspace Test - Create WS 1		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/ws1?sourceWorkspace=master&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv1 at 0x1070ed758>		

DiffWorkspaceCreate2
	[Documentation]		"Regression Test: 540. Diff Workspace Test - Create WS 2"
	${test_num} = 		 Set Variable		540
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv2
	run curl test		540		DiffWorkspaceCreate2		Diff Workspace Test - Create WS 2		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/ws2?sourceWorkspace=master&copyTime=$gv7"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'develop']		None		None		<function set_wsid_to_gv2 at 0x1070ed7d0>		

DiffDelete_arg_ev_38307
	[Documentation]		"Regression Test: 550. Diff Workspace Test - Delete element arg_ev_38307"
	${test_num} = 		 Set Variable		550
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		
	${branch_names} =	 Set Variable		test			workspaces			
	run curl test		550		DiffDelete_arg_ev_38307		Diff Workspace Test - Delete element arg_ev_38307		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/arg_ev_38307"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"']		['test', 'workspaces']		

DiffPostToWorkspace1
	[Documentation]		"Regression Test: 560. Diff Workspace Test - Post element to workspace"
	${test_num} = 		 Set Variable		560
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		560		DiffPostToWorkspace1		Diff Workspace Test - Post element to workspace		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/newElement.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

DiffUpdateElement402
	[Documentation]		"Regression Test: 570. Diff Workspace Test - Update element 402"
	${test_num} = 		 Set Variable		570
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		570		DiffUpdateElement402		Diff Workspace Test - Update element 402		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/update402.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

DiffCompareWorkspacesForMerge
	[Documentation]		"Regression Test: 580. Diff Workspace Test - Compare workspaces for a merge"
	${test_num} = 		 Set Variable		580
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_json_output_to_gv3
	run curl test		580		DiffCompareWorkspacesForMerge		Diff Workspace Test - Compare workspaces for a merge		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv2/$gv1/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		<function set_json_output_to_gv3 at 0x1070ed5f0>		

DiffCompareWorkspaces
	[Documentation]		"Regression Test: 580.5. Diff Workspace Test - Compare workspaces"
	${test_num} = 		 Set Variable		580.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_json_output_to_gv3
	run curl test		580.5		DiffCompareWorkspaces		Diff Workspace Test - Compare workspaces		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv2/$gv1/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		None		None		<function set_json_output_to_gv3 at 0x1070ed5f0>		

PostDiff
	[Documentation]		"Regression Test: 581. Post a diff to merge workspaces"
	${test_num} = 		 Set Variable		581
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		581		PostDiff		Post a diff to merge workspaces		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '$gv3' "http://localhost:8080/alfresco/service/diff"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"timestamp"', '"sequence"']		['test', 'workspaces', 'develop']		

DiffCompareWorkspacesAgainForMerge
	[Documentation]		"Regression Test: 582. Diff Workspace Test - Compare workspaces again for a merge and make sure the diff is empty now after merging."
	${test_num} = 		 Set Variable		582
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		582		DiffCompareWorkspacesAgainForMerge		Diff Workspace Test - Compare workspaces again for a merge and make sure the diff is empty now after merging.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv2/$gv1/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

DiffCompareWorkspacesAgain
	[Documentation]		"Regression Test: 582.5. Diff Workspace Test - Compare workspaces again and make sure the diff is empty now after merging."
	${test_num} = 		 Set Variable		582.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		582.5		DiffCompareWorkspacesAgain		Diff Workspace Test - Compare workspaces again and make sure the diff is empty now after merging.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv2/$gv1/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'develop']		

ParseSimpleExpression
	[Documentation]		"Regression Test: 600. Parse "1 + 1" from URL and create expression elements"
	${test_num} = 		 Set Variable		600
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			parsek			
	run curl test		600		ParseSimpleExpression		Parse "1 + 1" from URL and create expression elements		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/operation.json "http://localhost:8080/alfresco/service/workspaces/master/elements?expression=1%2B1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_']		['test', 'workspaces', 'develop', 'develop2', 'parsek']		

ParseAndEvaluateTextExpressionInFile
	[Documentation]		"Regression Test: 601. Parse text expression in file, create expression elements for it, and then evaluate the expression elements"
	${test_num} = 		 Set Variable		601
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"evaluationResult"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			parsek			
	run curl test		601		ParseAndEvaluateTextExpressionInFile		Parse text expression in file, create expression elements for it, and then evaluate the expression elements		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/k" --data @JsonData/onePlusOne.k "http://localhost:8080/alfresco/service/workspaces/master/elements?evaluate"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"evaluationResult"']		['test', 'workspaces', 'develop', 'develop2', 'parsek']		

CreateCollaborator
	[Documentation]		"Regression Test: 610. Create Collaborator user for europa"
	${test_num} = 		 Set Variable		610
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		610		CreateCollaborator		Create Collaborator user for europa		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"userName": "Collaborator", "firstName": "Collaborator", "lastName": "user", "email": "Collaborator@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteCollaborator"]}' "http://localhost:8080/alfresco/service/api/people"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"url"']		['test', 'workspaces', 'develop', 'develop2']		

CreateContributor
	[Documentation]		"Regression Test: 611. Create Contributor user for europa"
	${test_num} = 		 Set Variable		611
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		611		CreateContributor		Create Contributor user for europa		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"userName": "Contributor", "firstName": "Contributor", "lastName": "user", "email": "Contributor@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteContributor"]}' "http://localhost:8080/alfresco/service/api/people"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"url"']		['test', 'workspaces', 'develop', 'develop2']		

CreateConsumer
	[Documentation]		"Regression Test: 612. Create Consumer user for europa"
	${test_num} = 		 Set Variable		612
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		612		CreateConsumer		Create Consumer user for europa		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"userName": "Consumer", "firstName": "Consumer", "lastName": "user", "email": "Consumer@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteConsumer"]}' "http://localhost:8080/alfresco/service/api/people"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"url"']		['test', 'workspaces', 'develop', 'develop2']		

CreateManager
	[Documentation]		"Regression Test: 613. Create Manager user for europa"
	${test_num} = 		 Set Variable		613
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		613		CreateManager		Create Manager user for europa		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"userName": "Manager", "firstName": "Manager", "lastName": "user", "email": "Manager@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteManager"]}' "http://localhost:8080/alfresco/service/api/people"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"url"']		['test', 'workspaces', 'develop', 'develop2']		

CreateNone
	[Documentation]		"Regression Test: 614. Create user with no europa priveleges"
	${test_num} = 		 Set Variable		614
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		614		CreateNone		Create user with no europa priveleges		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"userName": "None", "firstName": "None", "lastName": "user", "email": "None@jpl.nasa.gov"}' "http://localhost:8080/alfresco/service/api/people"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"url"']		['test', 'workspaces', 'develop', 'develop2']		

NoneRead
	[Documentation]		"Regression Test: 620. Read element with user None"
	${test_num} = 		 Set Variable		620
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		620		NoneRead		Read element with user None		curl -w '\n\%{http_code}\n' -u None:password -X GET http://localhost:8080/alfresco/service/workspaces/master/elements/y		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

NoneDelete
	[Documentation]		"Regression Test: 621. Delete element with user None"
	${test_num} = 		 Set Variable		621
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		621		NoneDelete		Delete element with user None		curl -w '\n\%{http_code}\n' -u None:password -X DELETE http://localhost:8080/alfresco/service/workspaces/master/elements/y		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"id"']		['test', 'workspaces', 'develop', 'develop2']		

NoneUpdate
	[Documentation]		"Regression Test: 622. Update element with user None"
	${test_num} = 		 Set Variable		622
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			
	run curl test		622		NoneUpdate		Update element with user None		curl -w '\n\%{http_code}\n' -u None:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements -d '{"elements":[{"sysmlid":"y","documentation":"y is modified by None"}]}'		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces']		

NoneCreate
	[Documentation]		"Regression Test: 623. Create element with user None"
	${test_num} = 		 Set Variable		623
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			
	run curl test		623		NoneCreate		Create element with user None		curl -w '\n\%{http_code}\n' -u None:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements -d '{"elements":[{"sysmlid":"ychild","documentation":"y child","owner":"y"}]}'		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces']		

CollaboratorRead
	[Documentation]		"Regression Test: 624. Read element with user Collaborator"
	${test_num} = 		 Set Variable		624
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			
	run curl test		624		CollaboratorRead		Read element with user Collaborator		curl -w '\n\%{http_code}\n' -u Collaborator:password -X GET http://localhost:8080/alfresco/service/workspaces/master/elements/y		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces']		

CollaboratorUpdate
	[Documentation]		"Regression Test: 625. Update element with user Collaborator"
	${test_num} = 		 Set Variable		625
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		625		CollaboratorUpdate		Update element with user Collaborator		curl -w '\n\%{http_code}\n' -u Collaborator:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements -d '{"elements":[{"sysmlid":"y","documentation":"y is modified by Collaborator"}]}'		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

CollaboratorCreate
	[Documentation]		"Regression Test: 626. Create element with user Collaborator"
	${test_num} = 		 Set Variable		626
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			
	run curl test		626		CollaboratorCreate		Create element with user Collaborator		curl -w '\n\%{http_code}\n' -u Collaborator:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements -d '{"elements":[{"sysmlid":"ychild","documentation":"y child","owner":"y"}]}'		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces']		

CollaboratorDelete
	[Documentation]		"Regression Test: 627. Delete element with user Collaborator"
	${test_num} = 		 Set Variable		627
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	${branch_names} =	 Set Variable		test			workspaces			
	run curl test		627		CollaboratorDelete		Delete element with user Collaborator		curl -w '\n\%{http_code}\n' -u Collaborator:password -X DELETE http://localhost:8080/alfresco/service/workspaces/master/elements/y		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"id"']		['test', 'workspaces']		

CollaboratorResurrect
	[Documentation]		"Regression Test: 628. Resurrect element with user Collaborator"
	${test_num} = 		 Set Variable		628
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		628		CollaboratorResurrect		Resurrect element with user Collaborator		curl -w '\n\%{http_code}\n' -u Collaborator:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements --data @JsonData/y.json		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

ConsumerRead
	[Documentation]		"Regression Test: 630. Read element with user Consumer"
	${test_num} = 		 Set Variable		630
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			
	run curl test		630		ConsumerRead		Read element with user Consumer		curl -w '\n\%{http_code}\n' -u Consumer:password -X GET http://localhost:8080/alfresco/service/workspaces/master/elements/y		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces']		

ConsumerUpdate
	[Documentation]		"Regression Test: 631. Update element with user Consumer"
	${test_num} = 		 Set Variable		631
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			
	[Teardown]			removeCmNames
	run curl test		631		ConsumerUpdate		Update element with user Consumer		curl -w '\n\%{http_code}\n' -u Consumer:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements -d '{"elements":[{"sysmlid":"y","documentation":"y is modified by Consumer"}]}'		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces']		None		<function removeCmNames at 0x1070ed410>		None		

ConsumerCreate
	[Documentation]		"Regression Test: 632. Create element with user Consumer"
	${test_num} = 		 Set Variable		632
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			removeCmNames
	run curl test		632		ConsumerCreate		Create element with user Consumer		curl -w '\n\%{http_code}\n' -u Consumer:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements -d '{"elements":[{"sysmlid":"ychildOfConsumer","documentation":"y child of Consumer","owner":"y"}]}'		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		<function removeCmNames at 0x1070ed410>		None		

ConsumerDelete
	[Documentation]		"Regression Test: 633. Delete element with user Consumer"
	${test_num} = 		 Set Variable		633
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		"message"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			removeCmNames
	run curl test		633		ConsumerDelete		Delete element with user Consumer		curl -w '\n\%{http_code}\n' -u Consumer:password -X DELETE http://localhost:8080/alfresco/service/workspaces/master/elements/y		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"id"', '"message"']		['test', 'workspaces', 'develop', 'develop2']		None		<function removeCmNames at 0x1070ed410>		None		

ConsumerResurrect
	[Documentation]		"Regression Test: 634. Resurrect element with user Consumer"
	${test_num} = 		 Set Variable		634
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			removeCmNames
	run curl test		634		ConsumerResurrect		Resurrect element with user Consumer		curl -w '\n\%{http_code}\n' -u Consumer:password -H Content-Type:application/json http://localhost:8080/alfresco/service/workspaces/master/elements --data @JsonData/y.json		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		<function removeCmNames at 0x1070ed410>		None		

PostNullElements
	[Documentation]		"Regression Test: 640. Post elements to the master branch with null properties"
	${test_num} = 		 Set Variable		640
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		640		PostNullElements		Post elements to the master branch with null properties		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/nullElements.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TestJsonCache1
	[Documentation]		"Regression Test: 650. Post elements for json cache testing."
	${test_num} = 		 Set Variable		650
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		650		TestJsonCache1		Post elements for json cache testing.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/jsonCache1.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TestJsonCache2
	[Documentation]		"Regression Test: 651. Post elements for json cache testing."
	${test_num} = 		 Set Variable		651
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		651		TestJsonCache2		Post elements for json cache testing.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/jsonCache2.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TestJsonCache3
	[Documentation]		"Regression Test: 652. Post elements for json cache testing."
	${test_num} = 		 Set Variable		652
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		652		TestJsonCache3		Post elements for json cache testing.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/jsonCache3.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TestJsonCache4
	[Documentation]		"Regression Test: 653. Post elements for json cache testing."
	${test_num} = 		 Set Variable		653
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		653		TestJsonCache4		Post elements for json cache testing.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/jsonCache4.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TestResurrection1
	[Documentation]		"Regression Test: 660. Post elements for resurrection of parents testing.  Has two parents that will be resurrected."
	${test_num} = 		 Set Variable		660
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		660		TestResurrection1		Post elements for resurrection of parents testing.  Has two parents that will be resurrected.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/resurrectParents.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

DeleteParents
	[Documentation]		"Regression Test: 661. Delete parents"
	${test_num} = 		 Set Variable		661
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		661		DeleteParents		Delete parents		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/parentToDelete1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'develop', 'develop2']		

TestResurrection2
	[Documentation]		"Regression Test: 662. Post elements for resurrection of parents testing.  Has two parents that will be resurrected."
	${test_num} = 		 Set Variable		662
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		662		TestResurrection2		Post elements for resurrection of parents testing.  Has two parents that will be resurrected.		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/resurrectParentsChild.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TestGetAfterResurrection
	[Documentation]		"Regression Test: 663. Performs a recursive get to make sure the ownedChildren were property set after resurrection."
	${test_num} = 		 Set Variable		663
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		663		TestGetAfterResurrection		Performs a recursive get to make sure the ownedChildren were property set after resurrection.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/PROJECT-123456?recurse=true"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_']		['test', 'workspaces', 'develop']		

PostElementsWithProperites
	[Documentation]		"Regression Test: 670. Post elements for the next several tests"
	${test_num} = 		 Set Variable		670
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		670		PostElementsWithProperites		Post elements for the next several tests		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsWithProperties.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

GetSearchSlotProperty
	[Documentation]		"Regression Test: 671. Searching for the property "real" having value 5.39 (slot property)"
	${test_num} = 		 Set Variable		671
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Timeout]			70
	run curl test		671		GetSearchSlotProperty		Searching for the property "real" having value 5.39 (slot property)		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=5.39&filters=value&propertyName=real"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		None		None		None		70		

GetSearchSlotPropertyOffNom
	[Documentation]		"Regression Test: 672. Searching for the property "foo" having value 5.39 (slot property).  This should fail"
	${test_num} = 		 Set Variable		672
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		672		GetSearchSlotPropertyOffNom		Searching for the property "foo" having value 5.39 (slot property).  This should fail		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=5.39&filters=value&propertyName=foo"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

GetSearchNonSlotProperty
	[Documentation]		"Regression Test: 673. Searching for the property "real55" having value 34.5 (non-slot property)"
	${test_num} = 		 Set Variable		673
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		673		GetSearchNonSlotProperty		Searching for the property "real55" having value 34.5 (non-slot property)		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=34.5&filters=value&propertyName=real55"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

GetSearchNonSlotPropertyOffNom
	[Documentation]		"Regression Test: 674. Searching for the property "real55" having value 34.5 (non-slot property).  This should fail."
	${test_num} = 		 Set Variable		674
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		674		GetSearchNonSlotPropertyOffNom		Searching for the property "real55" having value 34.5 (non-slot property).  This should fail.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=34.5&filters=value&propertyName=gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

GetSearchElementWithProperty
	[Documentation]		"Regression Test: 675. Searching for element that owns a Property"
	${test_num} = 		 Set Variable		675
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		675		GetSearchElementWithProperty		Searching for element that owns a Property		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=steroetyped&filters=name"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

PostElementsForAspectHistoryCheck
	[Documentation]		"Regression Test: 700. Post elements to check for aspect changes in version history"
	${test_num} = 		 Set Variable		700
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		700		PostElementsForAspectHistoryCheck		Post elements to check for aspect changes in version history		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsForAspectHistoryCheck.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

CheckIfPostedAspectsInHistory
	[Documentation]		"Regression Test: 701. Get the previously posted elements at timestamp=now to see if their type aspects were recorded properly."
	[Setup]				set_gv1_to_current_time
	${test_num} = 		 Set Variable		701
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		701		CheckIfPostedAspectsInHistory		Get the previously posted elements at timestamp=now to see if their type aspects were recorded properly.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/aspect_history_zzz?recurse=true&timestamp=$gv1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		<function set_gv1_to_current_time at 0x1070f0230>		

DeleteElementForAspectHistoryCheck
	[Documentation]		"Regression Test: 702. Delete a property to see if the Delete aspect is recorded in the version history"
	${test_num} = 		 Set Variable		702
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		702		DeleteElementForAspectHistoryCheck		Delete a property to see if the Delete aspect is recorded in the version history		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/property_zzz"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'develop', 'develop2']		

UpdateElementsForAspectHistoryCheck
	[Documentation]		"Regression Test: 703. Post updates to element types to check for aspect changes in version history"
	${test_num} = 		 Set Variable		703
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"message"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		703		UpdateElementsForAspectHistoryCheck		Post updates to element types to check for aspect changes in version history		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/aspectChanges.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"message"']		['test', 'workspaces', 'develop', 'develop2']		

CheckIfAspectUpdatesInHistory
	[Documentation]		"Regression Test: 704. Get the previously updated elements at timestamp=now to see if changes to their type aspects were recorded properly."
	[Setup]				set_gv1_to_current_time
	${test_num} = 		 Set Variable		704
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		704		CheckIfAspectUpdatesInHistory		Get the previously updated elements at timestamp=now to see if changes to their type aspects were recorded properly.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/aspect_history_zzz?recurse=true&timestamp=$gv1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		<function set_gv1_to_current_time at 0x1070f0230>		

CheckIfAspectDeleteInHistory
	[Documentation]		"Regression Test: 705. Get the previously deleted element at timestamp=now to see if the Deleted aspect was recorded properly."
	${test_num} = 		 Set Variable		705
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	run curl test		705		CheckIfAspectDeleteInHistory		Get the previously deleted element at timestamp=now to see if the Deleted aspect was recorded properly.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/property_zzz?timestamp=$gv1"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop']		

PostElementsMatrix1
	[Documentation]		"Regression Test: 800. Post elements to the master branch for glom matrix testing"
	${test_num} = 		 Set Variable		800
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv3
	run curl test		800		PostElementsMatrix1		Post elements to the master branch for glom matrix testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsMatrix1.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_last_read_to_gv3 at 0x1070eded8>		

CreateWorkspaceMatrixTest1
	[Documentation]		"Regression Test: 801. Create workspace1 for glom matrix testing"
	${test_num} = 		 Set Variable		801
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_wsid_to_gv1
	run curl test		801		CreateWorkspaceMatrixTest1		Create workspace1 for glom matrix testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsMatrix1?sourceWorkspace=master&copyTime=$gv3"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_wsid_to_gv1 at 0x1070ed758>		

DeleteDeleteAddWsMatrix1
	[Documentation]		"Regression Test: 802. Delete delete_add_gg"
	${test_num} = 		 Set Variable		802
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		802		DeleteDeleteAddWsMatrix1		Delete delete_add_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/delete_add_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteUpdateWsMatrix1
	[Documentation]		"Regression Test: 803. Delete delete_update_gg"
	${test_num} = 		 Set Variable		803
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		803		DeleteDeleteUpdateWsMatrix1		Delete delete_update_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/delete_update_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteDeleteWsMatrix1
	[Documentation]		"Regression Test: 804. Delete delete_delete_gg"
	${test_num} = 		 Set Variable		804
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		804		DeleteDeleteDeleteWsMatrix1		Delete delete_delete_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/delete_delete_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteNoneWsMatrix1
	[Documentation]		"Regression Test: 805. Delete delete_none_gg"
	${test_num} = 		 Set Variable		805
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		805		DeleteDeleteNoneWsMatrix1		Delete delete_none_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/delete_none_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

PostElementsWsMatrix1
	[Documentation]		"Regression Test: 806. Post elements to the wsMatrix1 branch for glom matrix testing"
	${test_num} = 		 Set Variable		806
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv4
	run curl test		806		PostElementsWsMatrix1		Post elements to the wsMatrix1 branch for glom matrix testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsWsMatrix1.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_last_read_to_gv4 at 0x1070edf50>		

DeleteUpdateAddMaster
	[Documentation]		"Regression Test: 807. Delete update_add_gg"
	${test_num} = 		 Set Variable		807
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		807		DeleteUpdateAddMaster		Delete update_add_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/update_add_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteAddMaster
	[Documentation]		"Regression Test: 808. Delete delete_add_gg"
	${test_num} = 		 Set Variable		808
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		808		DeleteDeleteAddMaster		Delete delete_add_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/delete_add_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

PostElementsMatrix2
	[Documentation]		"Regression Test: 809. Post elements to the master branch for glom matrix testing"
	${test_num} = 		 Set Variable		809
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv5
	run curl test		809		PostElementsMatrix2		Post elements to the master branch for glom matrix testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsMatrix2.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_last_read_to_gv5 at 0x1070f0050>		

CreateWorkspaceMatrixTest2
	[Documentation]		"Regression Test: 810. Create workspace2 for glom matrix testing"
	${test_num} = 		 Set Variable		810
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_wsid_to_gv2
	run curl test		810		CreateWorkspaceMatrixTest2		Create workspace2 for glom matrix testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsMatrix2?sourceWorkspace=master&copyTime=$gv5"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_wsid_to_gv2 at 0x1070ed7d0>		

DeleteAddDeleteWsMatrix2
	[Documentation]		"Regression Test: 811. Delete add_delete_gg"
	${test_num} = 		 Set Variable		811
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		811		DeleteAddDeleteWsMatrix2		Delete add_delete_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv2/elements/add_delete_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteUpdateDeleteWsMatrix2
	[Documentation]		"Regression Test: 812. Delete update_delete_gg"
	${test_num} = 		 Set Variable		812
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		812		DeleteUpdateDeleteWsMatrix2		Delete update_delete_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv2/elements/update_delete_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteDeleteWsMatrix2
	[Documentation]		"Regression Test: 813. Delete delete_delete_gg"
	${test_num} = 		 Set Variable		813
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		813		DeleteDeleteDeleteWsMatrix2		Delete delete_delete_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv2/elements/delete_delete_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteNoneDeleteWsMatrix2
	[Documentation]		"Regression Test: 814. Delete none_delete_gg"
	${test_num} = 		 Set Variable		814
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		814		DeleteNoneDeleteWsMatrix2		Delete none_delete_gg		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv2/elements/none_delete_gg"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

PostElementsWsMatrix2
	[Documentation]		"Regression Test: 815. Post elements to the wsMatrix2 branch for glom matrix testing"
	${test_num} = 		 Set Variable		815
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv6
	run curl test		815		PostElementsWsMatrix2		Post elements to the wsMatrix2 branch for glom matrix testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsWsMatrix2.json "http://localhost:8080/alfresco/service/workspaces/$gv2/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_last_read_to_gv6 at 0x1070f00c8>		

CompareWorkspacesGlomMatrixForMerge
	[Documentation]		"Regression Test: 816. Compare workspaces at latest times for glom matrix test.  Does merge style diff."
	${test_num} = 		 Set Variable		816
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		816		CompareWorkspacesGlomMatrixForMerge		Compare workspaces at latest times for glom matrix test.  Does merge style diff.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'ws', 'develop']		

CompareWorkspacesGlomMatrix
	[Documentation]		"Regression Test: 817. Compare workspaces at latest times for glom matrix test.  Does full compare style diff."
	${test_num} = 		 Set Variable		817
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		817		CompareWorkspacesGlomMatrix		Compare workspaces at latest times for glom matrix test.  Does full compare style diff.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/$gv1/$gv2/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'ws', 'develop']		

PostElementsMerge1
	[Documentation]		"Regression Test: 900. Post elements to the master branch for merge-style diff testing"
	${test_num} = 		 Set Variable		900
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv3
	run curl test		900		PostElementsMerge1		Post elements to the master branch for merge-style diff testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsMasterMerge1.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_last_read_to_gv3 at 0x1070eded8>		

DeleteDeleteDeleteBeforeMasterMerge1
	[Documentation]		"Regression Test: 900.5. Delete delete_delete_before"
	${test_num} = 		 Set Variable		900.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		900.5		DeleteDeleteDeleteBeforeMasterMerge1		Delete delete_delete_before		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/delete_delete_before"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

CreateWorkspaceMerge-style-Test1
	[Documentation]		"Regression Test: 901. Create workspace1 for merge-style diff testing"
	${test_num} = 		 Set Variable		901
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_wsid_to_gv1
	run curl test		901		CreateWorkspaceMerge-style-Test1		Create workspace1 for merge-style diff testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST "http://localhost:8080/alfresco/service/workspaces/wsMerge1?sourceWorkspace=master&copyTime=$gv3"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"branched"', '"created"', '"id"', '"qualifiedId"']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_wsid_to_gv1 at 0x1070ed758>		

DeleteDeleteDeleteMasterMerge1
	[Documentation]		"Regression Test: 902. Delete delete_delete_consistent"
	${test_num} = 		 Set Variable		902
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		902		DeleteDeleteDeleteMasterMerge1		Delete delete_delete_consistent		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/delete_delete_consistent"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteUpdateMasterMerge1
	[Documentation]		"Regression Test: 903. Delete delete_update_consistent"
	${test_num} = 		 Set Variable		903
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		903		DeleteDeleteUpdateMasterMerge1		Delete delete_update_consistent		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/master/elements/delete_update_consistent"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

PostElementsMasterMerge1
	[Documentation]		"Regression Test: 904. Post elements to the MasterMerge1 branch for merge-style diff testing"
	${test_num} = 		 Set Variable		904
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv4
	run curl test		904		PostElementsMasterMerge1		Post elements to the MasterMerge1 branch for merge-style diff testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsMasterMerge2.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_last_read_to_gv4 at 0x1070edf50>		

CompareWorkspacesForMerge-style1
	[Documentation]		"Regression Test: 905. Compare workspaces at latest times for merge-style diff test."
	${test_num} = 		 Set Variable		905
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		905		CompareWorkspacesForMerge-style1		Compare workspaces at latest times for merge-style diff test.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/$gv1/latest/latest?background&changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"', '"diffTime"']		['test', 'workspaces', 'ws', 'develop']		

CompareWorkspacesForMerge-style2
	[Documentation]		"Regression Test: 905.5. Compare workspaces at latest times for merge-style diff test."
	${test_num} = 		 Set Variable		905.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		905.5		CompareWorkspacesForMerge-style2		Compare workspaces at latest times for merge-style diff test.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/$gv1/latest/latest?background&fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"', '"diffTime"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteDeleteWs1
	[Documentation]		"Regression Test: 906. Delete delete_delete_consistent"
	${test_num} = 		 Set Variable		906
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		906		DeleteDeleteDeleteWs1		Delete delete_delete_consistent		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/delete_delete_consistent"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteUpdateDeleteWs1
	[Documentation]		"Regression Test: 907. Delete update_delete_consistent"
	${test_num} = 		 Set Variable		907
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		907		DeleteUpdateDeleteWs1		Delete update_delete_consistent		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/update_delete_consistent"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

DeleteAddAddBeforeWs1
	[Documentation]		"Regression Test: 907.5. Delete add_add_before"
	${test_num} = 		 Set Variable		907.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		907.5		DeleteAddAddBeforeWs1		Delete add_add_before		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/add_add_before"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

PostElementsMerge2
	[Documentation]		"Regression Test: 908. Post elements to the master branch for merge-style diff testing"
	${test_num} = 		 Set Variable		908
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv5
	run curl test		908		PostElementsMerge2		Post elements to the master branch for merge-style diff testing		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/elementsWsMerge-style.json "http://localhost:8080/alfresco/service/workspaces/$gv1/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'ws', 'develop']		None		None		<function set_last_read_to_gv5 at 0x1070f0050>		

DeleteNoneAddDeleteWs1
	[Documentation]		"Regression Test: 908.2. Delete none_add_delete"
	${test_num} = 		 Set Variable		908.2
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		908.2		DeleteNoneAddDeleteWs1		Delete none_add_delete		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/none_add_delete"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"', '"modifier"']		['test', 'workspaces', 'ws', 'develop']		

DeleteDeleteDeleteBeforeWs1
	[Documentation]		"Regression Test: 908.5. Delete delete_delete_before"
	${test_num} = 		 Set Variable		908.5
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		908.5		DeleteDeleteDeleteBeforeWs1		Delete delete_delete_before		curl -w "\n\%{http_code}\n" -u admin:admin -X DELETE "http://localhost:8080/alfresco/service/workspaces/$gv1/elements/delete_delete_before"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"']		['test', 'workspaces', 'ws', 'develop']		

CompareWorkspacesForMerge-style3
	[Documentation]		"Regression Test: 909. Compare workspaces at latest times for merge-style diff test."
	${test_num} = 		 Set Variable		909
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		909		CompareWorkspacesForMerge-style3		Compare workspaces at latest times for merge-style diff test.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/$gv1/latest/latest?fullCompare"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'ws', 'develop']		

CompareWorkspacesForMerge-style4
	[Documentation]		"Regression Test: 910. Compare workspaces at latest times for merge-style diff test."
	${test_num} = 		 Set Variable		910
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	run curl test		910		CompareWorkspacesForMerge-style4		Compare workspaces at latest times for merge-style diff test.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/diff/master/$gv1/latest/latest?changesForMerge"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"id"', '"qualifiedId"', '"creator"', '"modifier"']		['test', 'workspaces', 'ws', 'develop']		

GetSearchDocumentation
	[Documentation]		"Regression Test: 10000. Get search documentation"
	${test_num} = 		 Set Variable		10000
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		develop			workspaces			
	run curl test		10000		GetSearchDocumentation		Get search documentation		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=some*&filters=documentation"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['develop', 'workspaces']		

GetSearchAspects
	[Documentation]		"Regression Test: 10001. Get search aspects"
	${test_num} = 		 Set Variable		10001
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		develop			workspaces			
	run curl test		10001		GetSearchAspects		Get search aspects		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=View&filters=aspect"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['develop', 'workspaces']		

GetSearchId
	[Documentation]		"Regression Test: 10002. Get search id"
	${test_num} = 		 Set Variable		10002
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		develop			workspaces			
	run curl test		10002		GetSearchId		Get search id		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=300&filters=id"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['develop', 'workspaces']		

GetSearchValue
	[Documentation]		"Regression Test: 10003. Get search value"
	${test_num} = 		 Set Variable		10003
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"qualifiedId"		"sysmlid"		
	${branch_names} =	 Set Variable		workspaces			
	run curl test		10003		GetSearchValue		Get search value		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/search?keyword=dlam_string&filters=value"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"qualifiedId"', '"sysmlid"']		['workspaces']		

GetNodeRefHistory
	[Documentation]		"Regression Test: 10004. Get NodeRef History"
	${test_num} = 		 Set Variable		10004
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10004		GetNodeRefHistory		Get NodeRef History		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/history/303"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TurnOnCheckMmsVersionFlag
	[Documentation]		"Regression Test: 10101. Turns on a service flag on the mms for comparing mms versions"
	${test_num} = 		 Set Variable		10101
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10101		TurnOnCheckMmsVersionFlag		Turns on a service flag on the mms for comparing mms versions		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/flags/checkMmsVersions?on"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

CheckMmsVersion-Correct
	[Documentation]		"Regression Test: 10105. Checks the MMS version when requesting an element, versions SHOULD match"
	${test_num} = 		 Set Variable		10105
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10105		CheckMmsVersion-Correct		Checks the MMS version when requesting an element, versions SHOULD match		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/303?mmsVersion=2.3"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

CheckMmsVersion-Incorrect
	[Documentation]		"Regression Test: 10106. Checks the MMS version when requesting an element, versions should NOT match"
	${test_num} = 		 Set Variable		10106
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10106		CheckMmsVersion-Incorrect		Checks the MMS version when requesting an element, versions should NOT match		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/303?mmsVersion=2.0"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

CheckMmsVersion-Invalid-Argument
	[Documentation]		"Regression Test: 10107. Checks the MMS version when requesting an element, request was made with the parameter but is missing an argument, or containing an invalid argument."
	${test_num} = 		 Set Variable		10107
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	run curl test		10107		CheckMmsVersion-Invalid-Argument		Checks the MMS version when requesting an element, request was made with the parameter but is missing an argument, or containing an invalid argument.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/303?mmsVersion="		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		[]		

CheckMmsVersion-Missing-Argument
	[Documentation]		"Regression Test: 10108. Checks the MMS version when requesting an element, request was made  but the REST call was missing the parameter '?mmsVersion=2.3'."
	${test_num} = 		 Set Variable		10108
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10108		CheckMmsVersion-Missing-Argument		Checks the MMS version when requesting an element, request was made  but the REST call was missing the parameter '?mmsVersion=2.3'.		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/elements/303"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TurnOffCheckMmsVersionFlag
	[Documentation]		"Regression Test: 10120. Turns off a service flag on the mms"
	${test_num} = 		 Set Variable		10120
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10120		TurnOffCheckMmsVersionFlag		Turns off a service flag on the mms		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/flags/checkMmsVersions?off"		False		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10130_PostElements
	[Documentation]		"Regression Test: 10130. Post elements"
	${test_num} = 		 Set Variable		10130
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10130		TestCase10130_PostElements		Post elements		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/TestCase10130_elements.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10130.1_VerifyProductX
	[Documentation]		"Regression Test: 10130.1. Verify elements for ProductX"
	${test_num} = 		 Set Variable		10130.1
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10130.1		TestCase10130.1_VerifyProductX		Verify elements for ProductX		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements/MMS_1450390431247_8a4b76d2-394c-4569-9abe-5c51dc4b0711"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10130.2_VerifyViewX
	[Documentation]		"Regression Test: 10130.2. Verify elements for View X"
	${test_num} = 		 Set Variable		10130.2
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10130.2		TestCase10130.2_VerifyViewX		Verify elements for View X		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements/MMS_1450390431247_8a4b76d2-394c-4569-9abe-5c51dc4b0811"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10130.3_VerifyViewY
	[Documentation]		"Regression Test: 10130.3. Verify elements for View Y"
	${test_num} = 		 Set Variable		10130.3
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10130.3		TestCase10130.3_VerifyViewY		Verify elements for View Y		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements/MMS_1450390431247_8a4b76d2-394c-4569-9abe-5c51dc4b0812"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10130.4_VerifyViewZ
	[Documentation]		"Regression Test: 10130.4. Verify elements for View Z"
	${test_num} = 		 Set Variable		10130.4
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10130.4		TestCase10130.4_VerifyViewZ		Verify elements for View Z		curl -w "\n\%{http_code}\n" -u admin:admin -X GET "http://localhost:8080/alfresco/service/workspaces/master/sites/europa/elements/MMS_1450390431247_8a4b76d2-394c-4569-9abe-5c51dc4b0813"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10131_PostViewV
	[Documentation]		"Regression Test: 10131. Post view V"
	${test_num} = 		 Set Variable		10131
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10131		TestCase10131_PostViewV		Post view V		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/TestCase10131_postViewV.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10132_PostViewVRemove
	[Documentation]		"Regression Test: 10132. Post view V"
	${test_num} = 		 Set Variable		10132
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10132		TestCase10132_PostViewVRemove		Post view V		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/TestCase10132_postViewVRemove.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

TestCase10133_PostViewVUpdate
	[Documentation]		"Regression Test: 10133. Post view V"
	${test_num} = 		 Set Variable		10133
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	run curl test		10133		TestCase10133_PostViewVUpdate		Post view V		curl -w "\n\%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/TestCase10133_postViewVUpdate.json "http://localhost:8080/alfresco/service/workspaces/master/elements"		True		['"nodeRefId"', '"versionedRefId"', '"created"', '"read"', '"lastModified"', '"modified"', '"siteCharacterizationId"', 'time_total', 'MMS_', '"timestamp"']		['test', 'workspaces', 'develop', 'develop2']		

	*** Keywords ***
Create Curl Command
	[Arguments]			@{varargs}
	create_curl_cmd		@{varargs}

Execute Curl Command
	[Arguments]			@{varargs}
	run_curl_test		@{varargs}
Regression
	regression_test_harness.run curl test		@{varargs}
	
