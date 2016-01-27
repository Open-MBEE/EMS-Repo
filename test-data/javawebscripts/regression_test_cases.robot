*** Settings ***
Library		OperatingSystem
Library		regression_lib.py

*** Variables ***

*** Test Cases ***
PostSite
	[Documentation]		"Regression Test: 10. Create a project and site"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			develop2			parsek			

PostElementsNew
	[Documentation]		"Regression Test: 20. Post elements to the master branch"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			develop2			parsek			

PostElementsBadOwners
	[Documentation]		"Regression Test: 21. Post elements to the master branch that have owners that cant be found"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostMultiplicityRedefines
	[Documentation]		"Regression Test: 22. Post elements to the master branch that exercise the multiplicity and redefines attributes of a Property"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostViews
	[Documentation]		"Regression Test: 30. Post views"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostProducts
	[Documentation]		"Regression Test: 40. Post products"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetSites
	[Documentation]		"Regression Test: 45. Get sites"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetProject
	[Documentation]		"Regression Test: 50. Get project"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetProjects
	[Documentation]		"Regression Test: 51. Get all projects for master"
	${use_json_diff} =	 Set Variable		True
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetElementsRecursively
	[Documentation]		"Regression Test: 60. Get all elements recursively"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElementsDepth0
	[Documentation]		"Regression Test: 61. Get elements recursively depth 0"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElementsDepth1
	[Documentation]		"Regression Test: 62. Get elements recursively depth 1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElementsDepth2
	[Documentation]		"Regression Test: 63. Get elements recursively depth 2"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElementsDepthAll
	[Documentation]		"Regression Test: 64. Get elements recursively depth -1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElementsDepthInvalid
	[Documentation]		"Regression Test: 65. Get elements recursively depth invalid"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElementsConnected
	[Documentation]		"Regression Test: 66. Get elements that are connected"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElementsRelationship
	[Documentation]		"Regression Test: 67. Get elements that have relationship DirectedRelationship, starting with 302"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetViews
	[Documentation]		"Regression Test: 70. Get views"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetViewElements
	[Documentation]		"Regression Test: 80. Get view elements"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetProducts
	[Documentation]		"Regression Test: 90. Get product"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetSearch
	[Documentation]		"Regression Test: 110. Get search"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			80

GetSearchPage0
	[Documentation]		"Regression Test: 111. Get search paginated 0"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"sysmlid"		"owner"		"qualifiedId"		"qualifiedName"		
	[Timeout]			0

GetSearchPage1
	[Documentation]		"Regression Test: 112. Get search paginated 1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"sysmlid"		"owner"		"qualifiedId"		"qualifiedName"		
	[Timeout]			0

GetSearchPageBad
	[Documentation]		"Regression Test: 112. Get search paginated bad"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			0

Delete6666
	[Documentation]		"Regression Test: 120. Delete element 6666"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostChange
	[Documentation]		"Regression Test: 130. Post changes to directed relationships only (without owners)"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_to_gv6_delta_gv7

PostConfig
	[Documentation]		"Regression Test: 140. Post configuration"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetConfig
	[Documentation]		"Regression Test: 150. Get configurations"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostConfigAgain
	[Documentation]		"Regression Test: 154. Post same configuration again"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetConfigAgain
	[Documentation]		"Regression Test: 155. Get configurations"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CreateWorkspace1
	[Documentation]		"Regression Test: 160. Create workspace test 1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv1

PostProjectWorkspace1
	[Documentation]		"Regression Test: 161. Post project to sync branch version for workspace 1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

CreateWorkspace2
	[Documentation]		"Regression Test: 162. Create workspace test 2"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv2

PostProjectWorkspace2
	[Documentation]		"Regression Test: 163. Post project to sync branch version for workspace 2 - sub workspace"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

CreateWorkspaceWithJson
	[Documentation]		"Regression Test: 164. Create a workspace using a json"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			do176

ModifyWorkspaceWithJson
	[Documentation]		"Regression Test: 165. Modifies a workspace name/description"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetWorkspaces
	[Documentation]		"Regression Test: 166. Get workspaces"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspace
	[Documentation]		"Regression Test: 167. Post element to workspace"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv4

CompareWorkspacesForMerge
	[Documentation]		"Regression Test: 168. Compare workspaces for a merge of the second into the first"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspaces
	[Documentation]		"Regression Test: 168.5. Compare workspaces"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesForMergeBackground1
	[Documentation]		"Regression Test: 169. Compare workspaces for a merge in the background, this will return that it is in process"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesBackground1
	[Documentation]		"Regression Test: 169.5. Compare workspaces in the background, this will return that it is in process"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesBackground2
	[Documentation]		"Regression Test: 170. Compare workspaces in the background again, this will return the results of the background diff"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			20

CompareWorkspacesBackground2
	[Documentation]		"Regression Test: 170.5. Compare workspaces in the background again, this will return the results of the background diff"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Timeout]			20

CompareWorkspacesGlomForMerge1
	[Documentation]		"Regression Test: 171. Compare workspaces for a merge where there is a initial background diff stored"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesGlom1
	[Documentation]		"Regression Test: 171.5. Compare workspaces where there is a initial background diff stored"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceForGlom
	[Documentation]		"Regression Test: 172. Post element to workspace"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesGlomForMerge2
	[Documentation]		"Regression Test: 173. Compare workspaces for a merge where there is a initial background diff stored and a change has been made since then."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesGlom2
	[Documentation]		"Regression Test: 173.5. Compare workspaces where there is a initial background diff stored and a change has been made since then."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CreateWorkspaceWithBranchTime
	[Documentation]		"Regression Test: 174. Create workspace with a branch time"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv5

PostToWorkspaceWithBranchTime
	[Documentation]		"Regression Test: 175. Post element to workspace with a branch time"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceForConflict1
	[Documentation]		"Regression Test: 176. Post element to workspace1 so that we get a conflict"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceForConflict2
	[Documentation]		"Regression Test: 177. Post element to workspace with a branch time so that we get a conflict"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceForMoved
	[Documentation]		"Regression Test: 178. Post element to workspace with a branch time so that we get a moved element"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceForTypeChange
	[Documentation]		"Regression Test: 179. Post element to workspace with a branch time so that we get a type change"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceForWs1Change
	[Documentation]		"Regression Test: 180. Post element to workspace1 so that we dont detect it in the branch workspace.  Changes 303"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetElement303
	[Documentation]		"Regression Test: 181. Get element 303"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesWithBranchTimeForMerge
	[Documentation]		"Regression Test: 182. Compare workspaces with branch times for a merge"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesWithBranchTime
	[Documentation]		"Regression Test: 182.5. Compare workspaces"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspace3
	[Documentation]		"Regression Test: 183. Post element z to workspace"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv7

CreateWorkspaceWithBranchTime2
	[Documentation]		"Regression Test: 184. Create workspace with a branch time using the current time for the branch time"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv6

CompareWorkspacesWithBranchTimesForMerge
	[Documentation]		"Regression Test: 185. Compare workspaces each of which with a branch time and with a modified element on the common parent for a merge"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesWithBranchTimes
	[Documentation]		"Regression Test: 185.5. Compare workspaces both which have a branch time and with a modified element on the common parent"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesForMergeBackgroundOutdated
	[Documentation]		"Regression Test: 186. Compare workspaces for a merge in the background, this will return that it is outdated"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesBackgroundOutdated
	[Documentation]		"Regression Test: 186.5. Compare workspaces in the background, this will return that it is outdated"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesForMergeBackgroundRecalculate
	[Documentation]		"Regression Test: 187. Compare workspaces for a merge in the background, and forces a recalculate on a outdated diff"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesBackgroundRecalculate
	[Documentation]		"Regression Test: 187.5. Compare workspaces in the background, and forces a recalculate on a outdated diff"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CreateWorkspaceAgain1
	[Documentation]		"Regression Test: 188. Create workspace for another diff test"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv1

CreateWorkspaceAgain2
	[Documentation]		"Regression Test: 189. Create workspace for another diff test"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv2

PostToWorkspaceG1ForCMED533
	[Documentation]		"Regression Test: 190. Post elements to workspace wsG1 for testing CMED-533"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceG1
	[Documentation]		"Regression Test: 191. Post element to workspace wsG1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv3

PostToMaster
	[Documentation]		"Regression Test: 192. Post element to master for a later diff"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostToWorkspaceG2ForCMED533
	[Documentation]		"Regression Test: 193. Post elements to workspace wsG2 for testing CMED-533"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostToWorkspaceG2
	[Documentation]		"Regression Test: 194. Post element to workspace wsG2"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_read_to_gv4

CompareWorkspacesG1G2ForMerge
	[Documentation]		"Regression Test: 195. Compare workspaces wsG1 and wsG2 with timestamps for a merge"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesG1G2
	[Documentation]		"Regression Test: 195.5. Compare workspaces wsG1 and wsG2 with timestamps"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesG1G2ForMergeBackground
	[Documentation]		"Regression Test: 196. Compare workspaces wsG1 and wsG2 with timestamps for a merge in the background to set up a initial diff for the next test"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesG1G2Background
	[Documentation]		"Regression Test: 196.5. Compare workspaces wsG1 and wsG2 with timestamps in background to set up a initial diff for the next test"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesG1G2ForMergeGlom
	[Documentation]		"Regression Test: 197. Compare workspaces wsG1 and wsG2 with timestamps for a merge with an initial diff"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CompareWorkspacesG1G2Glom
	[Documentation]		"Regression Test: 197.5. Compare workspaces wsG1 and wsG2 with timestamps with a initial diff"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

RecursiveGetOnWorkspaces
	[Documentation]		"Regression Test: 198. Makes sure that a recursive get on a modified workspace returns the modified elements"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

PostSiteInWorkspace
	[Documentation]		"Regression Test: 199. Create a project and site in a workspace"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetSiteInWorkspace
	[Documentation]		"Regression Test: 200. Get site in workspace"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			

GetProductsInSiteInWorkspace
	[Documentation]		"Regression Test: 201. Get products for a site in a workspace"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostNotInPastToWorkspace
	[Documentation]		"Regression Test: 202. Post element to master workspace for a diff test"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_delta_to_gv1
	[Timeout]			10

CompareWorkspacesForMergeNotInPast
	[Documentation]		"Regression Test: 203. Compare workspace master with itself for a merge at the current time and a time in the past"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CompareWorkspacesNotInPast
	[Documentation]		"Regression Test: 203.5. Compare workspace master with itself at the current time and a time in the past"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CompareWorkspacesForMergeNotInPastBackground
	[Documentation]		"Regression Test: 204. Compare workspace master with itself for a merge at the current time and a time in the past in the background"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CompareWorkspacesNotInPastBackground
	[Documentation]		"Regression Test: 204.5. Compare workspace master with itself at the current time and a time in the past in the background"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		"diffTime"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CreateParentWorkspace
	[Documentation]		"Regression Test: 205. Create a workspace to be a parent of another"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_wsid_to_gv1

PostToMasterAgain
	[Documentation]		"Regression Test: 206. Post new element to master"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_delta_to_gv2

CreateSubworkspace
	[Documentation]		"Regression Test: 207. Create workspace inside a workspace"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_wsid_to_gv3
	[Timeout]			30

GetElementInMasterFromSubworkspace
	[Documentation]		"Regression Test: 208. Get an element that only exists in the master from a subworkspace after its parent branch was created but before the it was created, it wont find the element"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostAToMaster
	[Documentation]		"Regression Test: 209. Post element a to master."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

CreateAParentWorkspace
	[Documentation]		"Regression Test: 210. Create a "parent" workspace off of master.."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	${branch_names} =	 Set Variable		test			develop			

PostBToMaster
	[Documentation]		"Regression Test: 211. Post element b to master."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

PostCToParent
	[Documentation]		"Regression Test: 212. Post element c to the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

CreateASubWorkspace
	[Documentation]		"Regression Test: 213. Create a "subworkspace" workspace off of the parent."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		"parent"		
	${branch_names} =	 Set Variable		test			develop			

PostDToMaster
	[Documentation]		"Regression Test: 214. Post element d to master."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

PostEToParent
	[Documentation]		"Regression Test: 215. Post element e to the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

PostFToSubworkspace
	[Documentation]		"Regression Test: 216. Post element f to the subworkspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetAInMaster
	[Documentation]		"Regression Test: 217. Get element a in the master workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetAInParent
	[Documentation]		"Regression Test: 218. Get element a in the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetAInSubworkspace
	[Documentation]		"Regression Test: 219. Get element a in the subworkspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetBInMaster
	[Documentation]		"Regression Test: 220. Get element b in the master workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetBInParent
	[Documentation]		"Regression Test: 221. Get element b in the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetBInSubworkspace
	[Documentation]		"Regression Test: 222. Get element b in the subworkspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetCInMaster
	[Documentation]		"Regression Test: 223. Get element c in the master workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetCInParent
	[Documentation]		"Regression Test: 224. Get element c in the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetCInSubworkspace
	[Documentation]		"Regression Test: 225. Get element c in the subworkspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetDInMaster
	[Documentation]		"Regression Test: 226. Get element d in the master workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetDInParent
	[Documentation]		"Regression Test: 227. Get element d in the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetDInSubworkspace
	[Documentation]		"Regression Test: 228. Get element d in the subworkspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetEInMaster
	[Documentation]		"Regression Test: 229. Get element e in the master workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetEInParent
	[Documentation]		"Regression Test: 230. Get element e in the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetEInSubworkspace
	[Documentation]		"Regression Test: 231. Get element e in the subworkspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetFInMaster
	[Documentation]		"Regression Test: 232. Get element f in the master workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetFInParent
	[Documentation]		"Regression Test: 233. Get element f in the parent workspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

GetFInSubworkspace
	[Documentation]		"Regression Test: 234. Get element f in the subworkspace."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			develop			

CompareMasterAToLatestForMerge
	[Documentation]		"Regression Test: 235. Compare master to itself for a merge between post time of a and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterAToLatest
	[Documentation]		"Regression Test: 235.5. Compare master to itself between post time of a and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterBToLatestForMerge
	[Documentation]		"Regression Test: 236. Compare master to itself for a merge between the post times of b and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterBToLatest
	[Documentation]		"Regression Test: 236.5. Compare master to itself between the post times of b and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterParentLatestToLatestForMerge
	[Documentation]		"Regression Test: 237. Compare master to theParentWorkspace for a merge with timepoints latest and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterParentLatestToLatest
	[Documentation]		"Regression Test: 237.5. Compare master to theParentWorkspace with timepoints latest and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterParentBranchTimeToLatest
	[Documentation]		"Regression Test: 238. Compare master to theParentWorkspace with timepoints at creation of parent and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterParentBranchTimeToLatestForMerge
	[Documentation]		"Regression Test: 238.5. Compare master to theParentWorkspace with timepoints for a merge at creation of parent and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterSubworkspaceLatestToLatestForMerge
	[Documentation]		"Regression Test: 239. Compare master to theSubworkspace for a merge with timepoints at latest and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

CompareMasterSubworkspaceLatestToLatest
	[Documentation]		"Regression Test: 239.5. Compare master to theSubworkspace with timepoints at latest and latest"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		

SolveConstraint
	[Documentation]		"Regression Test: 255. Post expressions with a constraint and solve for the constraint."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			

PostDemo1
	[Documentation]		"Regression Test: 260. Post data for demo 1 of server side docgen"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"sysmlid"		"qualifiedId"		"message"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

Demo1
	[Documentation]		"Regression Test: 270. Server side docgen demo 1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop2			

PostDemo2
	[Documentation]		"Regression Test: 280. Post data for demo 2 of server side docgen"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

Demo2
	[Documentation]		"Regression Test: 290. Server side docgen demo 2"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostSiteAndProject3
	[Documentation]		"Regression Test: 292. Create a site and a project for demo 3 of server side docgen"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostDemo3
	[Documentation]		"Regression Test: 293. Post data for demo 3 of server side docgen"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostViewDemo3
	[Documentation]		"Regression Test: 294. Post view data for demo 3 of server side docgen"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

Demo3
	[Documentation]		"Regression Test: 295. Server side docgen demo 3"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop2			

GetSites2
	[Documentation]		"Regression Test: 300. Get all the sites for a workspace"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetProductViews
	[Documentation]		"Regression Test: 310. Get all views for a product"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostElementX
	[Documentation]		"Regression Test: 320. Post element to the master branch/site"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_to_gv7

UpdateProject
	[Documentation]		"Regression Test: 330. Update a project"
	${use_json_diff} =	 Set Variable		False
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetProjectOnly
	[Documentation]		"Regression Test: 340. Get project w/o specifying the site"
	${use_json_diff} =	 Set Variable		True
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostArtifact
	[Documentation]		"Regression Test: 350. Post artifact to the master branch"
	${use_json_diff} =	 Set Variable		True
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetArtifact
	[Documentation]		"Regression Test: 360. Get artifact from the master branch"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CreateWorkspaceDelete1
	[Documentation]		"Regression Test: 370. Create workspace to be deleted"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			develop			
	[Teardown]			set_wsid_to_gv1

CreateWorkspaceDelete2
	[Documentation]		"Regression Test: 380. Create workspace to be deleted"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			develop			

DeleteWorkspace
	[Documentation]		"Regression Test: 390. Delete workspace and its children"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		
	${branch_names} =	 Set Variable		test			develop			

CheckDeleted1
	[Documentation]		"Regression Test: 400. Make sure that AA and its children no longer show up in workspaces"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			

CheckDeleted2
	[Documentation]		"Regression Test: 410. Make sure that AA and its children show up in deleted"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"parent"		"id"		"qualifiedId"		"branched"		
	${branch_names} =	 Set Variable		test			develop			

UnDeleteWorkspace
	[Documentation]		"Regression Test: 420. Undelete workspace"
	${use_json_diff} =	 Set Variable		False
	${branch_names} =	 Set Variable		test			develop			

PostSitePackage
	[Documentation]		"Regression Test: 430. Create a site package"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		

PostElementSitePackage
	[Documentation]		"Regression Test: 440. Post a product to a site package"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"message"		

GetSitePackageProducts
	[Documentation]		"Regression Test: 450. Get site package products"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		

SitePackageBugTest1
	[Documentation]		"Regression Test: 451. Create packages A2, B2, and C2, where A2/B2 are site packages for CMED-871 testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		

SitePackageBugTest2
	[Documentation]		"Regression Test: 452. Moves package B2 under package C2 for CMED-871 testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		

SitePackageBugTest3
	[Documentation]		"Regression Test: 453. Makes package C2 a site package for CMED-871 testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		

PostContentModelUpdates
	[Documentation]		"Regression Test: 460. Post content model udpates for sysml 2.0"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostDuplicateSysmlNames1
	[Documentation]		"Regression Test: 470. Post a element that will be used in the next test to generate a error"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostDuplicateSysmlNames2
	[Documentation]		"Regression Test: 480. Post a element with the same type, sysml name, and parent as the previous test to generate at error"
	${use_json_diff} =	 Set Variable		False

PostModelForDowngrade
	[Documentation]		"Regression Test: 500. Post model for downgrade test"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostModelForViewDowngrade
	[Documentation]		"Regression Test: 510. Post model for view downgrade"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostModelForElementDowngrade
	[Documentation]		"Regression Test: 520. Post model for element downgrade"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			set_read_to_gv7

DiffWorkspaceCreate1
	[Documentation]		"Regression Test: 530. Diff Workspace Test - Create WS 1"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv1

DiffWorkspaceCreate2
	[Documentation]		"Regression Test: 540. Diff Workspace Test - Create WS 2"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_wsid_to_gv2

DiffDelete_arg_ev_38307
	[Documentation]		"Regression Test: 550. Diff Workspace Test - Delete element arg_ev_38307"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		
	${branch_names} =	 Set Variable		test			workspaces			

DiffPostToWorkspace1
	[Documentation]		"Regression Test: 560. Diff Workspace Test - Post element to workspace"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

DiffUpdateElement402
	[Documentation]		"Regression Test: 570. Diff Workspace Test - Update element 402"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

DiffCompareWorkspacesForMerge
	[Documentation]		"Regression Test: 580. Diff Workspace Test - Compare workspaces for a merge"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_json_output_to_gv3

DiffCompareWorkspaces
	[Documentation]		"Regression Test: 580.5. Diff Workspace Test - Compare workspaces"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			
	[Teardown]			set_json_output_to_gv3

PostDiff
	[Documentation]		"Regression Test: 581. Post a diff to merge workspaces"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

DiffCompareWorkspacesAgainForMerge
	[Documentation]		"Regression Test: 582. Diff Workspace Test - Compare workspaces again for a merge and make sure the diff is empty now after merging."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

DiffCompareWorkspacesAgain
	[Documentation]		"Regression Test: 582.5. Diff Workspace Test - Compare workspaces again and make sure the diff is empty now after merging."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

ParseSimpleExpression
	[Documentation]		"Regression Test: 600. Parse "1 + 1" from URL and create expression elements"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			parsek			

ParseAndEvaluateTextExpressionInFile
	[Documentation]		"Regression Test: 601. Parse text expression in file, create expression elements for it, and then evaluate the expression elements"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			parsek			

CreateCollaborator
	[Documentation]		"Regression Test: 610. Create Collaborator user for europa"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CreateContributor
	[Documentation]		"Regression Test: 611. Create Contributor user for europa"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CreateConsumer
	[Documentation]		"Regression Test: 612. Create Consumer user for europa"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CreateManager
	[Documentation]		"Regression Test: 613. Create Manager user for europa"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CreateNone
	[Documentation]		"Regression Test: 614. Create user with no europa priveleges"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		MMS_		"url"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

NoneRead
	[Documentation]		"Regression Test: 620. Read element with user None"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

NoneDelete
	[Documentation]		"Regression Test: 621. Delete element with user None"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

NoneUpdate
	[Documentation]		"Regression Test: 622. Update element with user None"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			

NoneCreate
	[Documentation]		"Regression Test: 623. Create element with user None"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			

CollaboratorRead
	[Documentation]		"Regression Test: 624. Read element with user Collaborator"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			

CollaboratorUpdate
	[Documentation]		"Regression Test: 625. Update element with user Collaborator"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CollaboratorCreate
	[Documentation]		"Regression Test: 626. Create element with user Collaborator"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			

CollaboratorDelete
	[Documentation]		"Regression Test: 627. Delete element with user Collaborator"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		
	${branch_names} =	 Set Variable		test			workspaces			

CollaboratorResurrect
	[Documentation]		"Regression Test: 628. Resurrect element with user Collaborator"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

ConsumerRead
	[Documentation]		"Regression Test: 630. Read element with user Consumer"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			

ConsumerUpdate
	[Documentation]		"Regression Test: 631. Update element with user Consumer"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		test			workspaces			
	[Teardown]			removeCmNames

ConsumerCreate
	[Documentation]		"Regression Test: 632. Create element with user Consumer"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			removeCmNames

ConsumerDelete
	[Documentation]		"Regression Test: 633. Delete element with user Consumer"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"id"		"message"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			removeCmNames

ConsumerResurrect
	[Documentation]		"Regression Test: 634. Resurrect element with user Consumer"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Teardown]			removeCmNames

PostNullElements
	[Documentation]		"Regression Test: 640. Post elements to the master branch with null properties"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TestJsonCache1
	[Documentation]		"Regression Test: 650. Post elements for json cache testing."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TestJsonCache2
	[Documentation]		"Regression Test: 651. Post elements for json cache testing."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TestJsonCache3
	[Documentation]		"Regression Test: 652. Post elements for json cache testing."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TestJsonCache4
	[Documentation]		"Regression Test: 653. Post elements for json cache testing."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TestResurrection1
	[Documentation]		"Regression Test: 660. Post elements for resurrection of parents testing.  Has two parents that will be resurrected."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

DeleteParents
	[Documentation]		"Regression Test: 661. Delete parents"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TestResurrection2
	[Documentation]		"Regression Test: 662. Post elements for resurrection of parents testing.  Has two parents that will be resurrected."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TestGetAfterResurrection
	[Documentation]		"Regression Test: 663. Performs a recursive get to make sure the ownedChildren were property set after resurrection."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"MMS_		MMS_		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostElementsWithProperites
	[Documentation]		"Regression Test: 670. Post elements for the next several tests"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetSearchSlotProperty
	[Documentation]		"Regression Test: 671. Searching for the property "real" having value 5.39 (slot property)"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			
	[Timeout]			70

GetSearchSlotPropertyOffNom
	[Documentation]		"Regression Test: 672. Searching for the property "foo" having value 5.39 (slot property).  This should fail"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetSearchNonSlotProperty
	[Documentation]		"Regression Test: 673. Searching for the property "real55" having value 34.5 (non-slot property)"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetSearchNonSlotPropertyOffNom
	[Documentation]		"Regression Test: 674. Searching for the property "real55" having value 34.5 (non-slot property).  This should fail."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

GetSearchElementWithProperty
	[Documentation]		"Regression Test: 675. Searching for element that owns a Property"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

PostElementsForAspectHistoryCheck
	[Documentation]		"Regression Test: 700. Post elements to check for aspect changes in version history"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CheckIfPostedAspectsInHistory
	[Documentation]		"Regression Test: 701. Get the previously posted elements at timestamp=now to see if their type aspects were recorded properly."
	[Setup]				set_gv1_to_current_time
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

DeleteElementForAspectHistoryCheck
	[Documentation]		"Regression Test: 702. Delete a property to see if the Delete aspect is recorded in the version history"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

UpdateElementsForAspectHistoryCheck
	[Documentation]		"Regression Test: 703. Post updates to element types to check for aspect changes in version history"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CheckIfAspectUpdatesInHistory
	[Documentation]		"Regression Test: 704. Get the previously updated elements at timestamp=now to see if changes to their type aspects were recorded properly."
	[Setup]				set_gv1_to_current_time
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

CheckIfAspectDeleteInHistory
	[Documentation]		"Regression Test: 705. Get the previously deleted element at timestamp=now to see if the Deleted aspect was recorded properly."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			develop			

PostElementsMatrix1
	[Documentation]		"Regression Test: 800. Post elements to the master branch for glom matrix testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv3

CreateWorkspaceMatrixTest1
	[Documentation]		"Regression Test: 801. Create workspace1 for glom matrix testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_wsid_to_gv1

DeleteDeleteAddWsMatrix1
	[Documentation]		"Regression Test: 802. Delete delete_add_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteUpdateWsMatrix1
	[Documentation]		"Regression Test: 803. Delete delete_update_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteDeleteWsMatrix1
	[Documentation]		"Regression Test: 804. Delete delete_delete_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteNoneWsMatrix1
	[Documentation]		"Regression Test: 805. Delete delete_none_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

PostElementsWsMatrix1
	[Documentation]		"Regression Test: 806. Post elements to the wsMatrix1 branch for glom matrix testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv4

DeleteUpdateAddMaster
	[Documentation]		"Regression Test: 807. Delete update_add_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteAddMaster
	[Documentation]		"Regression Test: 808. Delete delete_add_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

PostElementsMatrix2
	[Documentation]		"Regression Test: 809. Post elements to the master branch for glom matrix testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv5

CreateWorkspaceMatrixTest2
	[Documentation]		"Regression Test: 810. Create workspace2 for glom matrix testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_wsid_to_gv2

DeleteAddDeleteWsMatrix2
	[Documentation]		"Regression Test: 811. Delete add_delete_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteUpdateDeleteWsMatrix2
	[Documentation]		"Regression Test: 812. Delete update_delete_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteDeleteWsMatrix2
	[Documentation]		"Regression Test: 813. Delete delete_delete_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteNoneDeleteWsMatrix2
	[Documentation]		"Regression Test: 814. Delete none_delete_gg"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

PostElementsWsMatrix2
	[Documentation]		"Regression Test: 815. Post elements to the wsMatrix2 branch for glom matrix testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv6

CompareWorkspacesGlomMatrixForMerge
	[Documentation]		"Regression Test: 816. Compare workspaces at latest times for glom matrix test.  Does merge style diff."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

CompareWorkspacesGlomMatrix
	[Documentation]		"Regression Test: 817. Compare workspaces at latest times for glom matrix test.  Does full compare style diff."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

PostElementsMerge1
	[Documentation]		"Regression Test: 900. Post elements to the master branch for merge-style diff testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv3

DeleteDeleteDeleteBeforeMasterMerge1
	[Documentation]		"Regression Test: 900.5. Delete delete_delete_before"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

CreateWorkspaceMerge-style-Test1
	[Documentation]		"Regression Test: 901. Create workspace1 for merge-style diff testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"branched"		"created"		"id"		"qualifiedId"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_wsid_to_gv1

DeleteDeleteDeleteMasterMerge1
	[Documentation]		"Regression Test: 902. Delete delete_delete_consistent"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteUpdateMasterMerge1
	[Documentation]		"Regression Test: 903. Delete delete_update_consistent"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

PostElementsMasterMerge1
	[Documentation]		"Regression Test: 904. Post elements to the MasterMerge1 branch for merge-style diff testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv4

CompareWorkspacesForMerge-style1
	[Documentation]		"Regression Test: 905. Compare workspaces at latest times for merge-style diff test."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

CompareWorkspacesForMerge-style2
	[Documentation]		"Regression Test: 905.5. Compare workspaces at latest times for merge-style diff test."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		"diffTime"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteDeleteWs1
	[Documentation]		"Regression Test: 906. Delete delete_delete_consistent"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteUpdateDeleteWs1
	[Documentation]		"Regression Test: 907. Delete update_delete_consistent"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteAddAddBeforeWs1
	[Documentation]		"Regression Test: 907.5. Delete add_add_before"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

PostElementsMerge2
	[Documentation]		"Regression Test: 908. Post elements to the master branch for merge-style diff testing"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			
	[Teardown]			set_last_read_to_gv5

DeleteNoneAddDeleteWs1
	[Documentation]		"Regression Test: 908.2. Delete none_add_delete"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

DeleteDeleteDeleteBeforeWs1
	[Documentation]		"Regression Test: 908.5. Delete delete_delete_before"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		"MMS_		"id"		"qualifiedId"		"version"		"modified"		"sequence"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

CompareWorkspacesForMerge-style3
	[Documentation]		"Regression Test: 909. Compare workspaces at latest times for merge-style diff test."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

CompareWorkspacesForMerge-style4
	[Documentation]		"Regression Test: 910. Compare workspaces at latest times for merge-style diff test."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"id"		"qualifiedId"		"creator"		"modifier"		
	@{branch_names} =	 Set Variable		test			workspaces			ws			develop			

GetSearchDocumentation
	[Documentation]		"Regression Test: 10000. Get search documentation"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		develop			workspaces			

GetSearchAspects
	[Documentation]		"Regression Test: 10001. Get search aspects"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		develop			workspaces			

GetSearchId
	[Documentation]		"Regression Test: 10002. Get search id"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		
	${branch_names} =	 Set Variable		develop			workspaces			

GetSearchValue
	[Documentation]		"Regression Test: 10003. Get search value"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"qualifiedId"		"sysmlid"		
	${branch_names} =	 Set Variable		workspaces			

GetNodeRefHistory
	[Documentation]		"Regression Test: 10004. Get NodeRef History"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TurnOnCheckMmsVersionFlag
	[Documentation]		"Regression Test: 10101. Turns on a service flag on the mms for comparing mms versions"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CheckMmsVersion-Correct
	[Documentation]		"Regression Test: 10105. Checks the MMS version when requesting an element, versions SHOULD match"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CheckMmsVersion-Incorrect
	[Documentation]		"Regression Test: 10106. Checks the MMS version when requesting an element, versions should NOT match"
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

CheckMmsVersion-Invalid-Argument
	[Documentation]		"Regression Test: 10107. Checks the MMS version when requesting an element, request was made with the parameter but is missing an argument, or containing an invalid argument."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		

CheckMmsVersion-Missing-Argument
	[Documentation]		"Regression Test: 10108. Checks the MMS version when requesting an element, request was made  but the REST call was missing the parameter '?mmsVersion=2.3'."
	${use_json_diff} =	 Set Variable		True
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

TurnOffCheckMmsVersionFlag
	[Documentation]		"Regression Test: 10120. Turns off a service flag on the mms"
	${use_json_diff} =	 Set Variable		False
	@{output_filters} =	 Set Variable		"nodeRefId"		"versionedRefId"		"created"		"read"		"lastModified"		"modified"		"siteCharacterizationId"		time_total		"timestamp"		
	@{branch_names} =	 Set Variable		test			workspaces			develop			develop2			

