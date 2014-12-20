#
# TODO:
#    -Have script check test list to ensure unique numbers/names
#    -Fix run server if we use it
#    -Get SOAP UI tests working
#    -Add ability to run test functions instead of just curl string commands
#
# BUGS (filed defects for these on JIRA):
#    -Test 14 returning 500 if doing more than once.  This is bug w/ the lock file.

import os
import commands
import re
import time
import subprocess
import sys
import optparse
import glob
import json

CURL_STATUS = '-w "\\n%{http_code}\\n"'
CURL_POST_FLAGS_NO_DATA = "-X POST"
CURL_POST_FLAGS = '-X POST -H "Content-Type:application/json" --data'
CURL_PUT_FLAGS = "-X PUT"
CURL_GET_FLAGS = "-X GET"
CURL_DELETE_FLAGS = "-X DELETE"
CURL_USER = " -u admin:admin"
CURL_FLAGS = CURL_STATUS+CURL_USER
HOST = "localhost:8080" 
SERVICE_URL = "http://%s/alfresco/service/"%HOST
BASE_URL_WS_NOBS = SERVICE_URL+"workspaces"
BASE_URL_WS = BASE_URL_WS_NOBS+"/"
BASE_URL_JW = SERVICE_URL+"javawebscripts/"

failed_tests = 0
errs = []
passed_tests = 0
result_dir = ""
baseline_dir = ""
display_width = 100
test_dir_path = "test-data/javawebscripts"
test_nums = []
test_names = []
create_baselines = False
common_filters = ['"created"','"read"','"lastModified"','"modified"']
cmd_git_branch = None

# Some global variables for lambda functions in tests
gv1 = None
gv2 = None
gv3 = None
gv4 = None
# These capture the curl output for any teardown functions
orig_output = None
filtered_output = None
orig_json = None
filtered_json = None

def set_gv1( v ):
    global gv1
    gv1 = v
def set_gv2( v ):
    global gv2
    gv2 = v
def set_gv3( v ):
    global gv3
    gv3 = v
def set_gv4( v ):
    global gv4
    gv4 = v

import re

def do20():
    ''' Gets the "modified" date out of the json output and sets gv1 to it.'''
    modDate = None
    json_output = ""
    #print 'orig_output=' + str(orig_output)
    if orig_output != None and len(str(orig_output)) > 5:
        # find the status code at the end of the string and remove it
        # comput i as the index to the start of the status code
        # walk backwards over whitespace 
        i=len(orig_output)-1
        while i >= 0:
            if not (orig_output[i] in [' ', '\n', '\t' ]):
                break
            i = i - 1
        # walk backwards over digits
        while i >= 0:
            if not (orig_output[i] >= '0' and orig_output[i] <= '9'):
                break
            i = i - 1
        # set json_output to orig_output without the ststus code
        if i > 0:
            json_output = orig_output[0:i]
#         json_output = re.sub(r'^[2][0-9][0-9]', r'', orig_output, 1)
#         #json_output = re.sub("^$", "", json_output)
        #print 'json_output=' + str(json_output)
        j = json.loads(json_output)
        #print "j=" + str(j)
        modDate = j['workspace2']['updatedElements'][0]['modified']
    set_gv1(modDate)

def create_command_line_options():

    '''Create all the command line options for this application
    
    Returns
    --------
    An optparse.OptionParser object for parsing the command line arguments fed to this application'''
    
    usageText = '''
    python regression_test_harness.py [-t <TESTNUMS> -n <TESTNAMES> -b -g <GITBRANCH>]
    
    To run all tests for the branch:
    
    python regression_test_harness.py 
    
    To run test numbers 1,2,11,5-9:
    
    python regression_test_harness.py -t 1,2,11,5-9
    
    To create baselines of all tests for the branch:
    
    python regression_test_harness.py -b
    
    To run tests with names "test1" and "test2"
    
    python regression_test_harness.py -n test1,test2
     
    After generating the baselines you will need to copy them from testBaselineDir 
    into the desired folder for that branch, ie workspacesBaselineDir, if you like the results.  
    This is because when running this script outside of jenkins, the script will output to testBaselineDir.
    Alternatively, change the branch name used using the -g command line arg when creating the baselines,
    to output to the correct baseline folder.
    
    When all tests are ran, it runs all the tests mapped to the current branch, which is specified in
    the tests table.  The current branch is determined via the GIT_BRANCH environment variable, or the
    -g command line argument.
    
    The -t option can take test numbers in any order, and it will run them in the order specified.
    Similarly for -n option.
    '''
                
    versionText = 'Version 1 (9_16_2014)'
    
    parser = optparse.OptionParser(usage=usageText,version=versionText)
        
    parser.add_option("-t","--testNums",action="callback",type="string",metavar="TESTNUMS",callback=parse_test_nums,
                      help='''Specify the test numbers to run or create baselines for, ie "1,2,5-9,11".  Can only supply this if not supplying -n also.  (Optional)''')
    parser.add_option("-n","--testNames",action="callback",type="string",metavar="TESTNAMES",callback=parse_test_names,
                      help='''Specify the test names to run or create baselines for, ie "test1,test2".  Can only supply this if not supplying -t also.  (Optional)''')
    parser.add_option("-b","--createBaselines",action="store_true",dest="create_baselines",
                      help='''Supply this option if you want to create the baseline files for the tests (Optional)''')
    parser.add_option("-g","--gitBranch",action="callback",type="string",metavar="GITBRANCH",callback=parse_git_branch,
                      help='''Specify the branch to use, otherwise uses the value of $GIT_BRANCH, and if that env variable is not defined uses 'test'. (Optional)''')
        
    return parser

def parse_command_line():
    '''Parse the command line options given to this application'''

    global test_nums, create_baselines, cmd_git_branch, test_names
    
    parser = create_command_line_options()
    
    parser.test_nums = None
    parser.cmd_git_branch = None
    parser.test_names = None
    
    (_options,_args) = parser.parse_args()
    
    test_nums = parser.test_nums
    test_names = parser.test_names
    create_baselines = _options.create_baselines
    cmd_git_branch = parser.cmd_git_branch
    
    if test_nums and test_names:
        print "ERROR: Cannot supply both the -t and -n options!  Please remove one of them."
        sys.exit(1)
 
def parse_git_branch(option, opt, value, parser):
    '''
    Parses the GIT_BRANCH command line arg
    '''
    
    if value is not None:
        parser.cmd_git_branch = value.strip()

def parse_test_nums(option, opt, value, parser):
    '''
    Parses out the section numbers ran from the passed string and creates
    a list of the corresponding section numbers, ie "1,3-5,7" will
    create [1,3,4,5,7].  Assigns this to list to parser.test_nums
    '''
    
    def parse_range(str):
    
        myList = []
        keyListBounds = str.split('-')
        bound1 = int(keyListBounds[0])
        bound2 = int(keyListBounds[1])
        
        mult = 1 if bound2 >= bound1 else -1

        for key in range(bound1,bound2+mult,mult):
            myList.append(key)
            
        return myList
            
    keyList = []
    
    if value is not None:
        value = value.strip()
        
        # value is comma separated ie 1,3-5,7:
        if value.find(',') != -1:
            keyListArray = value.split(',')
            
            for keyStr in keyListArray:
                                          
                # testKey is a range ie 1-5:
                if keyStr.find('-') != -1:
                    keyList += parse_range(keyStr)
                    
                # It was a single key:
                else:
                    keyList.append(int(keyStr))
                    
        # value is just a range ie 1-3:
        elif value.find('-') != -1:
            keyList += parse_range(value)

        #value was just a single key:
        else:
            keyList = [int(value)]
                    
    parser.test_nums = keyList
    
def parse_test_names(option, opt, value, parser):
    '''
    Parses out the test names from the command line arg.  Assigns this to list to parser.test_names
    '''
    
    keyList = []
    
    if value is not None:
        value = value.strip()
        
        # value is comma separated:
        if value.find(',') != -1:
            keyList = value.split(',')
                       
        #value was just a single key:
        else:
            keyList = [value]
                    
    parser.test_names = keyList

def thick_divider():
    print "\n"+"="*display_width+"\n"

def thin_divider():
    print "-"*display_width
    
def print_pass(msg):
    global passed_tests
    passed_tests += 1
    print "\nPASS: "+str(msg)

def print_error(msg, outpt):
    global failed_tests
    failed_tests += 1
    errs.append(msg)
    print "\nFAIL: "+str(msg)
    print str(outpt)
    
def mbee_util_jar_path():
    path = "../../../../.m2/repository/gov/nasa/jpl/mbee/util/mbee_util/"
    pathList = glob.glob(path+"*SNAPSHOT/*SNAPSHOT.jar")
    if pathList:
        return pathList[0]
    else:
        return path+"0.0.16/mbee_util-0.0.16.jar"

def run_curl_test(test_num, test_name, test_desc, curl_cmd, use_json_diff=False, filters=None,
                  setupFcn=None, teardownFcn=None, delay=None):
    '''
    Runs the curl test and diffs against the baseline if create_baselines is false, otherwise
    runs the curl command and creates the baseline .json file. 
    
    test_num: The unique test number for this test
    test_name: The name of the test
    test_desc: The test description
    curl_cmd: The curl command to send
    use_json_diff: Set to True to use a JsonDiff when comparing to the baseline
    filters: A list of strings that should be removed from the post output, ie ['"modified"']
    delay: Delay time in seconds before running the test
    '''
    
    global orig_output
    global filtered_output
    global orig_json
    global filtered_json
    global gv1, gv2, gv3, gv4

#     result_json = "%s/test%d.json"%(result_dir,test_num)
#     result_orig_json = "%s/test%d_orig.json"%(result_dir,test_num)
#     baseline_json = "%s/test%d.json"%(baseline_dir,test_num)
#     baseline_orig_json = "%s/test%d_orig.json"%(baseline_dir,test_num)
    result_json = "%s/%s.json"%(result_dir,test_name)
    result_orig_json = "%s/%s_orig.json"%(result_dir,test_name)
    baseline_json = "%s/%s.json"%(baseline_dir,test_name)
    baseline_orig_json = "%s/%s_orig.json"%(baseline_dir,test_name)

    thick_divider()
    if create_baselines:
        print "CREATING BASELINE FOR TEST %s (%s)"%(test_num, test_name)
        orig_json = baseline_orig_json
        filtered_json = baseline_json
    else:
        print "TEST %s (%s)"%(test_num, test_name)
        orig_json = result_orig_json
        filtered_json = result_json
        
    if delay:
        print "Delaying %s seconds before running the test"%delay
        time.sleep(delay)
        
    print "TEST DESCRIPTION: "+test_desc
    
    if setupFcn:
        print "calling setup function"
        setupFcn()

    #replace gv variable references in curl command
    curl_cmd = str(curl_cmd).replace("$gv1", str(gv1))
    curl_cmd = str(curl_cmd).replace("$gv2", str(gv2))
    curl_cmd = str(curl_cmd).replace("$gv3", str(gv3))
    curl_cmd = str(curl_cmd).replace("$gv4", str(gv4))

    print "Executing curl cmd: \n"+str(curl_cmd)
    
    (status,output) = commands.getstatusoutput(curl_cmd+"> "+orig_json)
        
    if status == 0:
                
        file_orig = open(orig_json, "r")
        
        # Apply filters to output of curl cmd (not using output b/c getstatusoutput pipes stderr to stdout):
        orig_output = ""
        filter_output = ""
        if filters:
            for line in file_orig:
                filterFnd = False
                for filter in filters:
                    # If the contains the filter:
                    if re.search(filter,line):
                        filterFnd = True
                        break
                    
                # Add line if it does not contain the filter:
                if not filterFnd:
                    filter_output += (line+"\n")
                    
                # Always add lines to orig_output
                orig_output += (line+"\n")
        else:
            stuffRead = file_orig.read()
            filter_output = stuffRead
            orig_output = stuffRead
        
        # Write to result .json file:
        file = open(filtered_json, "w")
        file.write(filter_output)
        file.close()
        file_orig.close()
     
        if teardownFcn:
            print "calling teardown function"
            teardownFcn()
        
        if create_baselines:
            
            print "Filtered output of curl command:\n"+filter_output
            
        else:
            # Perform diff:
            if use_json_diff:
                cp = ".:%s:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/classes"%mbee_util_jar_path()
                diff_cmd = "java -cp %s gov.nasa.jpl.view_repo.util.JsonDiff"%cp
            else:
                diff_cmd = "diff"

            (status_diff,output_diff) = commands.getstatusoutput("%s %s %s"%(diff_cmd,baseline_json,result_json))

            if output_diff:
                print_error("Test number %s (%s) failed!"%(test_num,test_name), "  Diff returned bad status or diffs found in the filtered .json files (%s,%s), status: %s, output: \n'%s'"%(baseline_json,result_json,status_diff,output_diff))
            else:
                print_pass("Test number %s (%s) passed!  No differences in the filtered .json files (%s,%s)"%(test_num,test_name,baseline_json,result_json))
    else:
        print_error("Test number %s (%s) failed!"%(test_num,test_name), "Curl command return a bad status and output doesnt start with json: %s, output: '%s'"%(status,output))

    thick_divider()
    
def run_test(test):
    '''
    Runs the curl test specified the passed test list
    '''
    
    run_curl_test(test_num=test[0],test_name=test[1],
                  test_desc=test[2],curl_cmd=test[3],
                  use_json_diff=test[4],filters=test[5],
                  setupFcn=test[7] if (len(test) > 7) else None,
                  teardownFcn=test[8] if (len(test) > 8) else None,
                  delay=test[9] if (len(test) > 9) else None)
    
    
def create_curl_cmd(type, data="", base_url=BASE_URL_WS, post_type="elements", branch="master/", 
                    project_post=False):
    '''
    Helper method to create curl commands.  Returns the curl cmd (string).
    
    type: POST, GET, DELETE
    data: Data to post in JsonData ie elementsNew.json, or the key/value pair when making a project ie "'{"name":"JW_TEST"}'",
          or the data to get ie views/301 or data to delete ie workspaces/master/elements/771
    base_url:  What base url to use, ie %s
    post_type: "elements", "views", "products"
    branch: The workspace branch, ie "master/", or the project/site to use to ie "sites/europa/projects/123456/"
    project_post: Set to True if creating a project
    post_no_data: Set to True if posting with no data
    '''%BASE_URL_WS
    
    cmd = ""
    
    if type == "POST":
        if project_post:
            cmd = 'curl %s %s %s "%s%s"'%(CURL_FLAGS, CURL_POST_FLAGS, data, base_url, branch)
        elif data:
            cmd = 'curl %s %s @JsonData/%s "%s%s%s"'%(CURL_FLAGS, CURL_POST_FLAGS, data, base_url, branch, post_type)
        else:
            cmd = 'curl %s %s "%s%s%s"'%(CURL_FLAGS, CURL_POST_FLAGS_NO_DATA, base_url, branch, post_type)
            
    elif type == "GET":
        cmd = 'curl %s %s "%s%s%s"'%(CURL_FLAGS, CURL_GET_FLAGS, base_url, branch, data)
        
    elif type == "DELETE":
        cmd = 'curl %s %s "%s%s%s"'%(CURL_FLAGS, CURL_DELETE_FLAGS, base_url, branch, data)

    return cmd

def kill_server():
    (status,output) = commands.getstatusoutput("pkill -fn 'integration-test'")

def startup_server():
    
    print "KILLING SERVER IF ONE IS RUNNING"
    kill_server()
    time.sleep(1)
    
    print "STARTING UP SERVER"
    #subprocess.call("./runserver_regression.sh")
    # Is this inheriting the correct environment variables?
    #p = subprocess.Popen("./runserver_regression.sh", shell=True) # still not working, eventually hangs and server doesn't come up
    commands.getstatusoutput("./runserver_regression.sh")
    #time.sleep(30)
    
    print "POLLING SERVER"
    server_log = open("runserver.log","r")
    seek = 0
    fnd_line = False
    for timeout in range(0,600):
        server_log.seek(seek)
        for line in server_log:
            if "Starting ProtocolHandler" in line:
                fnd_line = True
                break
            
        if fnd_line:
            break
        
        seek = server_log.tell()
        time.sleep(1)
        
        if timeout%10 == 0:
            print ".."

    if fnd_line:
        print "SERVER CONNECTED"
        
    else:
        print "ERROR: SERVER TIME-OUT"
        kill_server()
        sys.exit(1)
        
        
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
60,
"GetElements",
"Get elements",
create_curl_cmd(type="GET",data="elements/123456?recurse=true",base_url=BASE_URL_WS,
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
["test","workspaces","develop"],
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
54,
"PostConfigAgain",
"Post same configuration again",
create_curl_cmd(type="POST",data="configuration.json",base_url=BASE_URL_WS,
                branch="master/",post_type="configurations"),
True, 
common_filters+['"timestamp"','"id"'],
["test","workspaces","develop"]
],
        
[
55,
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
["test","workspaces","develop", "develop2"]
],
        
[
170,
"CreateWorkspace2",
"Create workspace test 2",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsB?sourceWorkspace=wsA"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"','"parent"'],
["test","workspaces","develop", "develop2"]
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

[
190,
"PostToWorkspace",
"Post element to workspace",
create_curl_cmd(type="POST",data="x.json",base_url=BASE_URL_WS,
                post_type="elements",branch="wsB/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],

[
200,
"CompareWorkspaces",
"Compare workspaces",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=wsA&workspace2=wsB"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"],
None,
do20 # lambda : set_gv1(json.loads(orig_json)['workspace2']['updatedElements'][0]['modified'] if (orig_json != None and len(str(orig_json)) > 0) else None)
],

# This test case depends on the previous one and uses gv1 set by the previous test
[
210,
"CreateWorkspaceWithBranchTime",
"Create workspace with a branch time",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsT?sourceWorkspace=wsA&copyTime=$gv1"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"', '"parent"'],
["test","workspaces","develop", "develop2"],
],

# This test case depends on the previous one
[
220,
"PostToWorkspaceWithBranchTime",
"Post element to workspace with a branch time",
create_curl_cmd(type="POST",data="y.json",base_url=BASE_URL_WS,
                post_type="elements",branch="wsT/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],

# This test case depends on the previous two
[
230,
"CompareWorkspacesWithBranchTime",
"Compare workspaces",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=wsA&workspace2=wsT"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"]
],

# SNAPSHOTS: ==========================    

[
240,
"PostSnapshot",
"Post snapshot test",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                branch="master/sites/europa/products/301/",
                post_type="snapshots"),
True, 
common_filters+['"created"','"id"','"url"'],
["test","workspaces","develop", "develop2"],
None,
None,
3
],

# EXPRESSIONS: ==========================    

# Note: currently not an equivalent in workspaces for this URL, but we may add it
[
250,
"SolveConstraint",
"Post expressions with a constraint and solves for the constraint.",
create_curl_cmd(type="POST",base_url=BASE_URL_JW,
                data="expressionElementsNew.json",
                branch="sites/europa/projects/123456/",
                post_type="elements?fix=true"),
True,
common_filters+['"specification"'],
["test","workspaces","develop"]
],
        
        
# Note: this is same as 250 but calls different json for second run in order to avoid resetting variables
[
255,
"SolveConstraint",
"Post expressions with a constraint and solves for the constraint.",
create_curl_cmd(type="POST",base_url=BASE_URL_JW,
                data="expressionElementsFix.json",
                branch="sites/europa/projects/123456/",
                post_type="elements?fix=true"),
True,
common_filters+['"specification"'],
["develop2"]
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
create_curl_cmd(type="GET",data="views/_17_0_2_3_e610336_1394148311476_17302_29388/elements",base_url=BASE_URL_WS,
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
common_filters,
[]
],
        
[
290,
"Demo2",
"Server side docgen demo 2",
create_curl_cmd(type="GET",data="views/_17_0_2_3_e610336_1394148233838_91795_29332/elements",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters,
[]
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
["develop"]
],

[
380,
"CreateWorkspaceDelete2",
"Create workspace to be deleted",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="BB?sourceWorkspace=AA"),
True,
common_filters + ['"parent"','"id"','"qualifiedId"'],
["develop"]
],

[
390,
"DeleteWorkspace",
"Delete workspace and its children",
create_curl_cmd(type="DELETE",base_url=BASE_URL_WS,
                post_type="",branch="AA"),
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
["test","workspaces","develop"]
],

# DOWNGRADE TO VIEW AND ELEMENT: ==================    

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
        
# CMED-471 Tests: ==================    

[
530,
"DiffWorkspaceCreate1",
"Diff Workspace Test - Create WS 1",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="ws1?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"]
], 

[
540,
"DiffWorkspaceCreate2",
"Diff Workspace Test - Create WS 2",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="ws2?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"]
],
      
[
550,
"DiffDelete_arg_ev_38307",       # deletes element arg_ev_38307 from ws1
"Diff Workspace Test - Delete element arg_ev_38307",
create_curl_cmd(type="DELETE",data="elements/arg_ev_38307",base_url=BASE_URL_WS,
                branch="ws1/"),
True, 
common_filters+['"timestamp"','"MMS_','"id"','"qualifiedId"','"version"', '"modified"'],
["test","workspaces","develop", "develop2"]
], 

[
560,
"DiffPostToWorkspace1",         # posts newElement to ws1
"Diff Workspace Test - Post element to workspace",
create_curl_cmd(type="POST",data="newElement.json",base_url=BASE_URL_WS,
                post_type="elements",branch="ws1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],   
        
[
570,
"DiffUpdateElement402",         # changes element 402 documentation to "x is x" in branch ws1
"Diff Workspace Test - Update element 402",
create_curl_cmd(type="POST",data="update402.json",base_url=BASE_URL_WS,
                post_type="elements",branch="ws1/"),
True, 
common_filters,
["test","workspaces","develop", "develop2"]
],
        
[
580,
"DiffCompareWorkspaces",
"Diff Workspace Test - Compare workspaces",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=ws2&workspace2=ws1"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces","develop", "develop2"]
],     
        
# EXPRESSION PARSING

[
600,
"ParseSimpleExpression",
"Parse \"1 + 1\" and create expression elements",
create_curl_cmd(type="POST",data="operation.json",base_url=BASE_URL_WS,
                post_type="elements?expression=1%2B1",branch="master/"),
True, 
common_filters+['MMS_'],
["test","workspaces","develop", "develop2"]
]
        
]

##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    
    # Parse the command line arguments:
    parse_command_line()
    
    # this is not working yet, so assumption for now is that this will be called 
    # by the bash script which will start up the server
    #startup_server() 
    
    # Change directories to where we are used to sending curl cmds:
    if not os.path.exists(test_dir_path):
        print "ERROR: Test directory path '%s' does not exists!\n"%test_dir_path
        sys.exit(1)
    
    os.chdir(test_dir_path)

    # Determine the branch to use based on the command line arg if supplied, otherwise
    # use the environment variable:
    if cmd_git_branch:
        git_branch = cmd_git_branch
    else:
        git_branch = os.getenv("GIT_BRANCH", "test")
    
    # Make the directories if needed:
    if "/" in git_branch:
        git_branch = git_branch.split("/")[1]

    result_dir = "%sResultDir"%git_branch
    baseline_dir = "%sBaselineDir"%git_branch
    
    if not os.path.exists(result_dir):
        os.makedirs(result_dir)
        
    if not os.path.exists(baseline_dir):
        os.makedirs(baseline_dir)
        
    print "\nUSING BASELINE DIR: '%s'\nOUTPUT DIR: '%s'\n"%(baseline_dir, result_dir)
    
    # Run tests or create baselines:
    # If there were test numbers specified:
    if test_nums:
        for test_num in test_nums:
            # If it is a valid test number then run the test:
            for test in tests:
                if test_num == test[0]:
                    run_test(test)
                     
    # If there were test names specified:
    elif test_names:
        for test_name in test_names:
                # If it is a valid test number then run the test:
                for test in tests:
                    if test_name == test[1]:
                        run_test(test)

    # Otherwise, run all the tests for the branch:
    else:
        for test in tests:            
            if git_branch in test[6]:
                run_test(test)

    # uncomment once startup_server() works
#     print "KILLING SERVER"
#     kill_server()
    
    if not create_baselines:
        print "\nNUMBER OF PASSED TESTS: "+str(passed_tests)
        print "NUMBER OF FAILED TESTS: "+str(failed_tests)+"\n"
        if failed_tests > 0:
            print "FAILED TESTS:"
            for item in errs[:]:
                print item
            print "\n"
    
    sys.exit(failed_tests)
    
    
    
