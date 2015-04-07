#
# The regression harness.  Define the tests to run in this file.
#
# TODO:
#    -Have script check test list to ensure unique numbers/names
#    -Fix run server if we use it
#    -Get SOAP UI tests working
#    -Add ability to run test functions instead of just curl string commands

from regression_lib import *


##########################################################################################
#
# TABLE OF ALL POSSIBLE TESTS TO RUN
#     MUST HAVE A UNIQUE TEST NUMBER AND UNIQUE TEST NAME
#
##########################################################################################    
tests =[\
        
# [       
# Test Number, 
# Test Name,
# Test Description,
# Curl Cmd, 
# Use JsonDiff, 
# Output Filters (ie lines in the .json output with these strings will be filtered out)
# Branch Names that will run this test by default
# Set up function (Optional)
# Tear down function (Optional)
# Delay in seconds before running the test (Optional)
# ]

# POSTS: ==========================
[
10,
"PostSite",
"Create a project and site",
create_curl_cmd(type="POST",data='\'{"elements":[{"sysmlid":"123456","name":"JW_TEST","specialization":{"type":"Project"}}]}\'',
                base_url=BASE_URL_WS,
                branch="master/sites/europa/projects?createSite=true",project_post=True),
False, 
None,
["test","workspaces","develop", "develop2"]
],
 
[
20,
"PostElementsNew",
"Post elements to the master branch",
create_curl_cmd(type="POST",data="elementsNew.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
[
21,
"PostElementsBadOwners",
"Post elements to the master branch that have owners that cant be found",
create_curl_cmd(type="POST",data="badOwners.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
[
30,
"PostViews",
"Post views",
create_curl_cmd(type="POST",data="views.json",base_url=BASE_URL_WS,
                post_type="views",branch="master/"),
False, 
None,
["test","workspaces","develop", "develop2"]
],

[
40,
"PostProducts",
"Post products",
create_curl_cmd(type="POST",data="products.json",base_url=BASE_URL_WS,
                post_type="products",branch="master/sites/europa/"),
False, 
None,
["test","workspaces","develop", "develop2"]
],
  
# GETS: ==========================    

[
45,
"GetSites",
"Get sites",
create_curl_cmd(type="GET",data="sites",base_url=BASE_URL_WS, branch="master/"),
False, 
None,
["test","workspaces","develop", "develop2"]
],

[
50,
"GetProject",
"Get project",
create_curl_cmd(type="GET",data="sites/europa/projects/123456",base_url=BASE_URL_WS,
                branch="master/"),
False, 
None,
["test","workspaces","develop", "develop2"]
],
        
[
51,
"GetProjects",
"Get all projects for master",
create_curl_cmd(type="GET",data="projects",base_url=BASE_URL_WS,
                branch="master/"),
True, 
None,
["test","workspaces","develop", "develop2"]
],
        
[
60,
"GetElementsRecursively",
"Get all elements recursively",
create_curl_cmd(type="GET",data="elements/123456?recurse=true",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop"]
],

[
61,
"GetElementsDepth0",
"Get elements recursively depth 0",
create_curl_cmd(type="GET",data="elements/123456?depth=0",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop"]
],

[
62,
"GetElementsDepth1",
"Get elements recursively depth 1",
create_curl_cmd(type="GET",data="elements/123456?depth=1",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop"]
],

[
63,
"GetElementsDepth2",
"Get elements recursively depth 2",
create_curl_cmd(type="GET",data="elements/123456?depth=2",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop"]
],

[
64,
"GetElementsDepthAll",
"Get elements recursively depth -1",
create_curl_cmd(type="GET",data="elements/123456?depth=-1",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop"]
],

[
65,
"GetElementsDepthInvalid",
"Get elements recursively depth invalid",
create_curl_cmd(type="GET",data="elements/123456?depth=invalid",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop"]
],

[
70,
"GetViews",
"Get views",
create_curl_cmd(type="GET",data="views/301",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
[
80,
"GetViewElements",
"Get view elements",
create_curl_cmd(type="GET",data="views/301/elements",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
     
[
90,
"GetProducts",
"Get product",
create_curl_cmd(type="GET",data="products/301",base_url=BASE_URL_WS,
                branch="master/sites/europa/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
                   
[
100,
"GetProductList",
"Get product list",
create_curl_cmd(type="GET",data="ve/documents/europa?format=json",base_url=SERVICE_URL,
                branch=""),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
   
# Note: Need a delay before doing this search, b/c it will find "some*" within the
#       documentation, which does not get indexed by alfresco right away
[
110,
"GetSearch",
"Get search",
create_curl_cmd(type="GET",data="search?keyword=some*",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters,
["test","workspaces"],
None,
None,
None,
80
],

#[
#99,
#"GetSearch",
#"Get search",
#create_curl_cmd(type="GET",data="element/search?keyword=some*",base_url=BASE_URL_WS,
#                branch="master/"),
#True, 
#common_filters,
#["foo"],
#None,
#Node,
#None,
#0
#],

# DELETES: ==========================    
      
[
120,
"Delete6666",
"Delete element 6666",
create_curl_cmd(type="DELETE",data="elements/6666",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"timestamp"','"MMS_','"id"','"qualifiedId"','"version"', '"modified"'],
["test","workspaces","develop", "develop2"]
],
        
# POST CHANGES: ==========================    

# Note: currently not an equivalent in workspaces for this URL, but we may add it
[
130,
"PostChange",
"Post changes to directed relationships only (without owners)",
create_curl_cmd(type="POST",data="directedrelationships.json",base_url=BASE_URL_JW,
                branch="sites/europa/projects/123456/",post_type="elements"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# CONFIGURATIONS: ==========================    

[
140,
"PostConfig",
"Post configuration",
create_curl_cmd(type="POST",data="configuration.json",base_url=BASE_URL_WS,
                branch="master/sites/europa/",post_type="configurations"),
True, 
common_filters+['"timestamp"','"id"'],
["test","workspaces","develop", "develop2"]
],
        
[
150,
"GetConfig",
"Get configurations",
create_curl_cmd(type="GET",data="sites/europa/configurations",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"timestamp"','"id"'],
["test","workspaces","develop", "develop2"]
],
        
[
154,
"PostConfigAgain",
"Post same configuration again",
create_curl_cmd(type="POST",data="configuration.json",base_url=BASE_URL_WS,
                branch="master/",post_type="configurations"),
True, 
common_filters+['"timestamp"','"id"'],
["test","workspaces","develop"]
],
        
[
155,
"GetConfigAgain",
"Get configurations",
create_curl_cmd(type="GET",data="sites/europa/configurations",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"timestamp"','"id"'],
["test","workspaces","develop"]
],
        
# WORKSPACES: ==========================    

[
160,
"CreateWorkspace1",
"Create workspace test 1",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsA?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"],
None,
None,
set_wsid_to_gv1
],
  
# This test case depends on the previous one and uses gv1 set by the previous test      
[
170,
"CreateWorkspace2",
"Create workspace test 2",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsB?sourceWorkspace=$gv1"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"','"parent"'],
["test","workspaces","develop", "develop2"],
None,
None,
set_wsid_to_gv2
],
        
[
175,
"CreateWorkspaceWithJson",
"Create a workspace using a json",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="",data="NewWorkspacePost.json"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop"],
None,
None,
do176 
],

# This test case depends on the previous one and uses gv3 set by the previous test
[
176,
"ModifyWorkspaceWithJson",
"Modifies a workspace name/description",
'''curl %s %s '$gv3' "%s"'''%(CURL_FLAGS,CURL_POST_FLAGS,BASE_URL_WS),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop"],
],
        
[
180,
"GetWorkspaces",
"Get workspaces",
create_curl_cmd(type="GET",base_url=BASE_URL_WS_NOBS,branch=""),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"','"parent"'],
["test","workspaces","develop", "develop2"]
],

# This test case depends on test 160/170 thats sets gv1,gv2
[
190,
"PostToWorkspace",
"Post element to workspace",
create_curl_cmd(type="POST",data="x.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv2/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],

# This test case depends on test 170 thats sets gv2
[
200,
"CompareWorkspaces",
"Compare workspaces",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=$gv1&workspace2=$gv2"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"],
None,
None,
do20 # lambda : set_gv1(json.loads(orig_json)['workspace2']['updatedElements'][0]['modified'] if (orig_json != None and len(str(orig_json)) > 0) else None)
],

# This test case depends on the previous one and uses gv4 set by the previous test
[
210,
"CreateWorkspaceWithBranchTime",
"Create workspace with a branch time",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsT?sourceWorkspace=$gv1&copyTime=$gv4"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"', '"parent"'],
["test","workspaces","develop", "develop2"],
None,
None,
set_wsid_to_gv5
],

# This test case depends on the previous one
[
220,
"PostToWorkspaceWithBranchTime",
"Post element to workspace with a branch time",
create_curl_cmd(type="POST",data="y.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv5/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This test case depends on test 160 thats sets gv1
[
221,
"PostToWorkspaceForConflict1",
"Post element to workspace1 so that we get a conflict",
create_curl_cmd(type="POST",data="conflict1.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This test case depends on test 220 thats sets gv5
[
222,
"PostToWorkspaceForConflict2",
"Post element to workspace with a branch time so that we get a conflict",
create_curl_cmd(type="POST",data="conflict2.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv5/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This test case depends on test 220 thats sets gv5
[
223,
"PostToWorkspaceForMoved",
"Post element to workspace with a branch time so that we get a moved element",
create_curl_cmd(type="POST",data="moved.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv5/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This test case depends on test 220 thats sets gv5
[
224,
"PostToWorkspaceForTypeChange",
"Post element to workspace with a branch time so that we get a type change",
create_curl_cmd(type="POST",data="typeChange.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv5/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This test case depends on test 160 thats sets gv1
[
225,
"PostToWorkspaceForWs1Change",
"Post element to workspace1 so that we dont detect it in the branch workspace.  Changes 303",
create_curl_cmd(type="POST",data="modified303.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
[
226,
"GetElement303",
"Get element 303",
create_curl_cmd(type="GET",data="elements/303",base_url=BASE_URL_WS,
                branch="$gv5/"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop"]
],
        
# This test case depends on the previous two
[
227,
"CompareWorkspacesWithBranchTime",
"Compare workspaces",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=$gv1&workspace2=$gv5"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"]
],
        
# This test case depends on previous ones
[
228,
"PostToWorkspace3",
"Post element z to workspace",
create_curl_cmd(type="POST",data="z.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This test case depends on previous ones
[
229,
"CreateWorkspaceWithBranchTime2",
"Create workspace with a branch time using the current time for the branch time",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsT2?sourceWorkspace=$gv1&copyTime="+get_current_time()),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"', '"parent"'],
["test","workspaces","develop", "develop2"],
None,
None,
set_wsid_to_gv6
],
        
# This test case depends on the previous ones
[
230,
"CompareWorkspacesWithBranchTimes",
"Compare workspaces both which have a branch time and with a modified element on the common parent",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=$gv5&workspace2=$gv6"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"]
],
        
[
231,
"CreateWorkspaceAgain1",
"Create workspace for another diff test",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsG1?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"],
None,
None,
set_wsid_to_gv1
],
        
[
232,
"CreateWorkspaceAgain2",
"Create workspace for another diff test",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsG2?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"],
None,
None,
set_wsid_to_gv2
],
        
# This is to test CMED-533.  Where we post the same elements to two different workspaces and diff.
[
233,
"PostToWorkspaceG1ForCMED533",
"Post elements to workspace wsG1 for testing CMED-533",
create_curl_cmd(type="POST",data="elementsForBothWorkspaces.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"],
],
        
# This test case depends on test 234
[
234,
"PostToWorkspaceG1",
"Post element to workspace wsG1",
create_curl_cmd(type="POST",data="x.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"],
None,
None,
set_read_to_gv3
],

[
235,
"PostToMaster",
"Post element to master for a later diff",
create_curl_cmd(type="POST",data="y.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This is to test CMED-533.  Where we post the same elements to two different workspaces and diff.
[
236,
"PostToWorkspaceG2ForCMED533",
"Post elements to workspace wsG2 for testing CMED-533",
create_curl_cmd(type="POST",data="elementsForBothWorkspaces.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv2/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"],
],
        
# This test case depends on test 235
[
237,
"PostToWorkspaceG2",
"Post element to workspace wsG2",
create_curl_cmd(type="POST",data="z.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv2/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"],
None,
None,
set_read_to_gv4
],
        
# This test case depends on test 234 and 235
[
238,
"CompareWorkspacesG1G2",
"Compare workspaces wsG1 and wsG2 with timestamps",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=$gv1&workspace2=$gv2&timestamp1=$gv3&timestamp2=$gv4"),
True, 
common_filters+['"id"','"qualifiedId"','"timestamp"'],
["test","workspaces","develop", "develop2"]
],

[
240,
"PostSiteInWorkspace",
"Create a project and site in a workspace",
create_curl_cmd(type="POST",data='\'{"elements":[{"sysmlid":"proj_id_001","name":"PROJ_1","specialization":{"type":"Project"}}]}\'',
                base_url=BASE_URL_WS,
                branch="$gv1/sites/site_in_ws/projects?createSite=true",project_post=True),
False, 
None,
["test","workspaces","develop", "develop2"]
],
 
[
241,
"GetSiteInWorkspace",
"Get site in workspace",
create_curl_cmd(type="GET",data="sites",base_url=BASE_URL_WS, branch="$gv1/"),
False, 
None,
["test","workspaces","develop", "develop2"]
],


[
242,
"GetProductsInSiteInWorkspace",
"Get products for a site in a workspace",
create_curl_cmd(type="GET",data="products",base_url=BASE_URL_WS,
                branch="$gv1/sites/europa/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
[
243,
"PostNotInPastToWorkspace",
"Post element to master workspace for a diff test",
create_curl_cmd(type="POST",data="notInThePast.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"],
None,
None,
set_read_delta_to_gv1,
10
],
        
# This test depends on the previous one:
[
244,
"CompareWorkspacesNotInPast",
"Compare workspace master with itself at the current time and a time in the past",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=master&workspace2=master&timestamp2=$gv1"),
True, 
common_filters+['"id"','"qualifiedId"','"timestamp"'],
["test","workspaces","develop", "develop2"]
],

# SNAPSHOTS: ==========================    

# This functionality is deprecated:
# [
# 240,
# "PostSnapshot",
# "Post snapshot test",
# create_curl_cmd(type="POST",base_url=BASE_URL_WS,
#                 branch="master/sites/europa/products/301/",
#                 post_type="snapshots"),
# True, 
# common_filters+['"created"','"id"','"url"'],
# ["test","workspaces","develop", "develop2"],
# None,
# None,
# None,
# 3
# ],

# EXPRESSIONS: ==========================    

# Note: currently not an equivalent in workspaces for this URL, but we may add it
[
250,
"SolveConstraint",
"Post expressions with a constraint and solve for the constraint.",
create_curl_cmd(type="POST",base_url=BASE_URL_JW,
                data="expressionElementsNew.json",
                branch="sites/europa/projects/123456/",
                post_type="elements?fix=true"),
True,
common_filters,
["test","workspaces"]
],
        
[
260,
"PostDemo1",
"Post data for demo 1 of server side docgen",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                data="BluCamNameListExpr.json",
                branch="master/sites/europa/",
                post_type="elements"),
True, 
common_filters+['"sysmlid"','"qualifiedId"','"message"'],
["test","workspaces","develop", "develop2"]
],
        
[
270,
"Demo1",
"Server side docgen demo 1",
create_curl_cmd(type="GET",data="views/_17_0_2_3_e610336_1394148311476_17302_29388",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
 
# TODO: not running test 280/290 for now b/c of issues when running it with 260/270    
[
280,
"PostDemo2",
"Post data for demo 2 of server side docgen",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                data="BLUCamTest.json",
                branch="master/sites/europa/",
                post_type="elements"),
True, 
common_filters+['"MMS_','MMS_'],
["test","workspaces","develop", "develop2"]
],
        
[
290,
"Demo2",
"Server side docgen demo 2",
create_curl_cmd(type="GET",data="views/_17_0_2_3_e610336_1394148233838_91795_29332",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],

# NEW URLS: ==========================    

[
300,
"GetSites",
"Get all the sites for a workspace",
create_curl_cmd(type="GET",data="sites",base_url=BASE_URL_WS,
                branch="master/"),
False, 
None,
["test","workspaces","develop", "develop2"]
],

[
310,
"GetProductViews",
"Get all views for a product",
create_curl_cmd(type="GET",data="products/301/views",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# Getting the view elements tested in GetViewElements

[
320,
"PostElementX",
"Post element to the master branch/site",
create_curl_cmd(type="POST",data="x.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/sites/europa/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# Posting a new project/site tested in PostSite

[
330,
"UpdateProject",
"Update a project",
create_curl_cmd(type="POST",data='\'{"elements":[{"sysmlid":"123456","name":"JW_TEST2","specialization":{"type":"Project","projectVersion":"1"}}]}\'',
                base_url=BASE_URL_WS,
                branch="master/projects",project_post=True),
False, 
None,
["test","workspaces","develop", "develop2"]
],
        
# Get project w/ site included tested in GetProject

[
340,
"GetProjectOnly",
"Get project w/o specifying the site",
create_curl_cmd(type="GET",data="projects/123456",base_url=BASE_URL_WS,
                branch="master/"),
False, 
None,
["test","workspaces","develop", "develop2"]
],
  
# ARTIFACTS: ==========================    

[
350,
"PostArtifact",
"Post artifact to the master branch",
'curl %s %s -H "Content-Type: multipart/form-data;" --form "file=@JsonData/x.json" --form "title=JsonData/x.json" --form "desc=stuffs" --form "content=@JsonData/x.json" %smaster/sites/europa/artifacts/folder1/folder2/xartifact'%(CURL_FLAGS, CURL_POST_FLAGS_NO_DATA, BASE_URL_WS),
True, 
None,
["test","workspaces","develop", "develop2"]
],
        
[
360,
"GetArtifact",
"Get artifact from the master branch",
create_curl_cmd(type="GET",data="artifacts/xartifact?extension=svg&cs=3463563326",base_url=BASE_URL_WS,
                branch="master/"),
False, 
['"url"'],
["test","workspaces","develop", "develop2"]
],
                                   
[
370,
"CreateWorkspaceDelete1",
"Create workspace to be deleted",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="AA?sourceWorkspace=master"),
True,
common_filters + ['"parent"','"id"','"qualifiedId"'],
["develop"],
None,
None,
set_wsid_to_gv1
],

# This test depends on the previous one for gv1
[
380,
"CreateWorkspaceDelete2",
"Create workspace to be deleted",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="BB?sourceWorkspace=$gv1"),
True,
common_filters + ['"parent"','"id"','"qualifiedId"'],
["develop"]
],

# This test depends on 370 for gv1
[
390,
"DeleteWorkspace",
"Delete workspace and its children",
create_curl_cmd(type="DELETE",base_url=BASE_URL_WS,
                post_type="",branch="$gv1"),
True,
common_filters + ['"parent"','"id"','"qualifiedId"'],
["develop"]
],

[
400,
"CheckDeleted1",
"Make sure that AA and its children no longer show up in workspaces",
create_curl_cmd(type="GET",base_url=BASE_URL_WS_NOBS,
                post_type="", branch=""),
True,
common_filters + ['"parent"','"id"','"qualifiedId"','"branched"'],
["develop"]
],

[
410,
"CheckDeleted2",
"Make sure that AA and its children show up in deleted",
create_curl_cmd(type="GET",base_url=BASE_URL_WS_NOBS,
                post_type="", branch="?deleted"),
True,
common_filters + ['"parent"','"id"','"qualifiedId"'],
["develop"]
],

## TODO: placeholder to put in post to get back workspace A (need the ID from 380)
[
420,
"UnDeleteWorkspace",
"Undelete workspace",
'echo "temporary placeholder"',
False,
None,
["develop"]
],

# SITE PACKAGES: ==========================    
# Cant run these tests in regression b/c you need
# to bring up share also.
[
430,
"PostSitePackage",
"Create a site package",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                data="SitePackage.json",
                branch="master/sites/europa/",
                post_type="elements"),
True, 
common_filters,
[]
],
        
[
440,
"PostElementSitePackage",
"Post a product to a site package",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                data="ElementSitePackage.json",
                branch="master/sites/site_package/",
                post_type="elements"),
True, 
common_filters+['"message"'],
[]
],
        
[
450,
"GetSitePackageProducts",
"Get site package products",
create_curl_cmd(type="GET",data="products",base_url=BASE_URL_WS,
                branch="master/sites/site_package/"),
True, 
common_filters,
[]
],
        
# CONTENT MODEL UPDATES: ==========================    

[
460,
"PostContentModelUpdates",
"Post content model udpates for sysml 2.0",
create_curl_cmd(type="POST",data="contentModelUpdates.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop"]
],
        
# CMED-416: ==========================    

[
470,
"PostDuplicateSysmlNames1",
"Post a element that will be used in the next test to generate a error",
create_curl_cmd(type="POST",data="cmed416_1.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop"]
],
        
[
480,
"PostDuplicateSysmlNames2",
"Post a element with the same type, sysml name, and parent as the previous test to generate at error",
create_curl_cmd(type="POST",data="cmed416_2.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
False, 
None,
[]
],

# DOWNGRADING: ==================    

[
500,
"PostModelForDowngrade",
"Post model for downgrade test",
create_curl_cmd(type="POST",data="productsDowngrade.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
], 

[
510,
"PostModelForViewDowngrade",
"Post model for view downgrade",
create_curl_cmd(type="POST",data="viewDowngrade.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],    

[
520,
"PostModelForElementDowngrade",
"Post model for element downgrade",
create_curl_cmd(type="POST",data="elementDowngrade.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# DiffPost (Merge) (CMED-471) Tests: ==================    

[
530,
"DiffWorkspaceCreate1",
"Diff Workspace Test - Create WS 1",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="ws1?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop"],
None,
None,
set_wsid_to_gv1
], 

[
540,
"DiffWorkspaceCreate2",
"Diff Workspace Test - Create WS 2",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="ws2?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop"],
None,
None,
set_wsid_to_gv2
],
      
# This test is dependent on 530
[
550,
"DiffDelete_arg_ev_38307",       # deletes element arg_ev_38307 from ws1
"Diff Workspace Test - Delete element arg_ev_38307",
create_curl_cmd(type="DELETE",data="elements/arg_ev_38307",base_url=BASE_URL_WS,
                branch="$gv1/"),
True, 
common_filters+['"timestamp"','"MMS_','"id"','"qualifiedId"','"version"', '"modified"'],
["test","workspaces"]
], 

[
560,
"DiffPostToWorkspace1",         # posts newElement to ws1
"Diff Workspace Test - Post element to workspace",
create_curl_cmd(type="POST",data="newElement.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],   
        
[
570,
"DiffUpdateElement402",         # changes element 402 documentation to "x is x" in branch ws1
"Diff Workspace Test - Update element 402",
create_curl_cmd(type="POST",data="update402.json",base_url=BASE_URL_WS,
                post_type="elements",branch="$gv1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
[
580,
"DiffCompareWorkspaces",
"Diff Workspace Test - Compare workspaces",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=$gv2&workspace2=$gv1"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces"],
None,
None,
set_json_output_to_gv3
], 
        
[
581,
"PostDiff",
"Post a diff to merge workspaces",
'curl %s %s \'$gv3\' "%sdiff"'%(CURL_FLAGS, CURL_POST_FLAGS, SERVICE_URL),
True, 
common_filters+['"id"','"qualifiedId"','"timestamp"'],
["test","workspaces"],
],         
       
# Diff again should be empty.  This test depends on the previous one.
[
582,
"DiffCompareWorkspacesAgain",
"Diff Workspace Test - Compare workspaces again and make sure the diff is empty now after merging.",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=$gv2&workspace2=$gv1"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces"],
], 
 
# EXPRESSION PARSING =====================================================

[
600,
"ParseSimpleExpression",
"Parse \"1 + 1\" from URL and create expression elements",
create_curl_cmd(type="POST",data="operation.json",base_url=BASE_URL_WS,
                post_type="elements?expression=1%2B1",branch="master/"),
True,
common_filters+['MMS_'],
["test","workspaces","develop", "develop2"]
],

[
601,
"ParseAndEvaluateTextExpressionInFile",
"Parse text expression in file, create expression elements for it, and then evaluate the expression elements",
create_curl_cmd(type="POST",data="onePlusOne.k",base_url=BASE_URL_WS,
                post_type="elements?evaluate",branch="master/"),
True,
common_filters+['MMS_'],
["test","workspaces","develop", "develop2"]
],
# PERMISSION TESTING =====================================================

# Creating users for user testing
[
610,
"CreateCollaborator",
"Create Collaborator user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Collaborator", "firstName": "Collaborator", "lastName": "user", "email": "Collaborator@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteCollaborator"]}\'',
                base_url=SERVICE_URL,
                post_type="",branch="api/people",project_post=True),
False, 
common_filters+['MMS_', '"url"'],
["test","workspaces","develop", "develop2"]
],
 
[
611,
"CreateContributor",
"Create Contributor user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Contributor", "firstName": "Contributor", "lastName": "user", "email": "Contributor@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteContributor"]}\'',
                base_url=SERVICE_URL,
                post_type="",branch="api/people",project_post=True),
False, 
common_filters+['MMS_', '"url"'],
["test","workspaces","develop", "develop2"]
],
 
[
612,
"CreateConsumer",
"Create Consumer user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Consumer", "firstName": "Consumer", "lastName": "user", "email": "Consumer@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteConsumer"]}\'',
                base_url=SERVICE_URL,
                post_type="",branch="api/people",project_post=True),
False, 
common_filters+['MMS_', '"url"'],
["test","workspaces","develop", "develop2"]
],
 
[
613,
"CreateManager",
"Create Manager user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Manager", "firstName": "Manager", "lastName": "user", "email": "Manager@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteManager"]}\'',
                base_url=SERVICE_URL,
                post_type="",branch="api/people",project_post=True),
False, 
common_filters+['MMS_', '"url"'],
["test","workspaces","develop", "develop2"]
],
         
[
614,
"CreateNone",
"Create user with no europa priveleges",
create_curl_cmd(type="POST",
                data='\'{"userName": "None", "firstName": "None", "lastName": "user", "email": "None@jpl.nasa.gov"}\'',
                base_url=SERVICE_URL,
                post_type="",branch="api/people",project_post=True),
False, 
common_filters+['MMS_', '"url"'],
["test","workspaces","develop", "develop2"]
],

# lets do the None permissions
[
620,
"NoneRead",
"Read element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -X GET " + BASE_URL_WS + "master/elements/y",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],
    
[
621,
"NoneDelete",
"Delete element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -X DELETE " + BASE_URL_WS + "master/elements/y",
True,
common_filters+['"timestamp"', '"id"'],
["test","workspaces","develop", "develop2"]
],
    
[
622,
"NoneUpdate",
"Update element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"y\",\"documentation\":\"y is modified by None\"}]}'",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],
    
[
623,
"NoneCreate",
"Create element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"ychild\",\"documentation\":\"y child\",\"owner\":\"y\"}]}'",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],
 
[
624,
"CollaboratorRead",
"Read element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -X GET " + BASE_URL_WS + "master/elements/y",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],
   
[
625,
"CollaboratorUpdate",
"Update element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"y\",\"documentation\":\"y is modified by Collaborator\"}]}'",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],
   
[
626,
"CollaboratorCreate",
"Create element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"ychild\",\"documentation\":\"y child\",\"owner\":\"y\"}]}'",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],
 
[
627,
"CollaboratorDelete",
"Delete element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -X DELETE " + BASE_URL_WS + "master/elements/y",
True,
common_filters+['"timestamp"', '"id"'],
["test","workspaces","develop", "develop2"]
],
  
[
628,
"CollaboratorResurrect",
"Resurrect element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements --data @JsonData/y.json",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],

# Consumer permissions
  
[
630,
"ConsumerRead",
"Read element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -X GET " + BASE_URL_WS + "master/elements/y",
True,
common_filters,
["test","workspaces","develop", "develop2"]
],
   
[
631,
"ConsumerUpdate",
"Update element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"y\",\"documentation\":\"y is modified by Consumer\"}]}'",
False,
common_filters,
["test","workspaces","develop", "develop2"],
None,
removeCmNames,
None
],
   
[
632,
"ConsumerCreate",
"Create element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"ychildOfConsumer\",\"documentation\":\"y child of Consumer\",\"owner\":\"y\"}]}'",
False,
common_filters,
["test","workspaces","develop", "develop2"],
None,
removeCmNames,
None
],
 
[
633,
"ConsumerDelete",
"Delete element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -X DELETE " + BASE_URL_WS + "master/elements/y",
False,
common_filters+['"timestamp"', '"id"'],
["test","workspaces","develop", "develop2"],
None,
removeCmNames,
None
],

[
634,
"ConsumerResurrect",
"Resurrect element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements --data @JsonData/y.json",
False,
common_filters,
["test","workspaces","develop", "develop2"],
None,
removeCmNames,
None
],

# NULL PROPERTIES =====================================================

[
640,
"PostNullElements",
"Post elements to the master branch with null properties",
create_curl_cmd(type="POST",data="nullElements.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# JSON CACHE TESTING =====================================================

# These test post the same element with updates to the embedded value spec.
# This tests that the json cache doesnt return a outdated embedded value spec
[
650,
"TestJsonCache1",
"Post elements for json cache testing.",
create_curl_cmd(type="POST",data="jsonCache1.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
 
[
651,
"TestJsonCache2",
"Post elements for json cache testing.",
create_curl_cmd(type="POST",data="jsonCache2.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
         
[
652,
"TestJsonCache3",
"Post elements for json cache testing.",
create_curl_cmd(type="POST",data="jsonCache3.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
         
[
653,
"TestJsonCache4",
"Post elements for json cache testing.",
create_curl_cmd(type="POST",data="jsonCache4.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# RESURRECTION TESTING (CMED-430): ==========================    

[
660,
"TestResurrection1",
"Post elements for resurrection of parents testing.  Has two parents that will be resurrected.",
create_curl_cmd(type="POST",data="resurrectParents.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
# This test depends on the previous one:
[
661,
"DeleteParents",
"Delete parents",
create_curl_cmd(type="DELETE",data="elements/parentToDelete1",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"timestamp"','"MMS_','"id"','"qualifiedId"','"version"', '"modified"'],
["test","workspaces","develop", "develop2"]
],
        
[
662,
"TestResurrection2",
"Post elements for resurrection of parents testing.  Has two parents that will be resurrected.",
create_curl_cmd(type="POST",data="resurrectParentsChild.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
]

##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    
    run(tests)
    
