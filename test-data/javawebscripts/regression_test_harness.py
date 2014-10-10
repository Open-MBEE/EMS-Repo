#
# TODO:
#    -Fix run server if we use it
#    -Get SOAP UI tests working
#    -Add ability to run test functions instead of just curl string commands
#
# BUGS (filed defects for these on JIRA):
#    -Test 14 returning 500 if doing more than once.  This is bug w/ the lock file.
#     Test 16 returns 404 after doing it more than once, and created time changes.  This is also a bug


import os
import commands
import re
import time
import subprocess
import sys
import optparse
import glob

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
passed_tests = 0
result_dir = ""
baseline_dir = ""
display_width = 100
test_dir_path = "test-data/javawebscripts"
test_nums = []
test_names = []
create_baselines = False
common_filters = ['"read"','"lastModified"','"modified"']
cmd_git_branch = None


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

def print_error(msg):
    global failed_tests
    failed_tests += 1
    print "\nFAIL: "+str(msg)
    
def mbee_util_jar_path():
    path = "../../../../.m2/repository/gov/nasa/jpl/mbee/util/mbee_util/"
    pathList = glob.glob(path+"*SNAPSHOT/*SNAPSHOT.jar")
    if pathList:
        return pathList[0]
    else:
        return path+"0.0.16/mbee_util-0.0.16.jar"

def run_curl_test(test_num, test_name, test_desc, curl_cmd, use_json_diff=False, filters=None,
                  delay=None):
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

    result_json = "%s/test%d.json"%(result_dir,test_num)
    result_orig_json = "%s/test%d_orig.json"%(result_dir,test_num)
    baseline_json = "%s/test%d.json"%(baseline_dir,test_num)
    baseline_orig_json = "%s/test%d_orig.json"%(baseline_dir,test_num)

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
    print "Executing curl cmd: \n"+str(curl_cmd)
    
    (status,output) = commands.getstatusoutput(curl_cmd+"> "+orig_json)
        
    if status == 0:
                
        file_orig = open(orig_json, "r")
        
        # Apply filters to output of curl cmd (not using output b/c getstatusoutput pipes stderr to stdout):
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
        else:
            filter_output = file_orig.read()
        
        # Write to result .json file:
        file = open(filtered_json, "w")
        file.write(filter_output)
        file.close()
        file_orig.close()
        
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
                print_error("Test number %s failed!  Diff returned bad status or diffs found in the filtered .json files (%s,%s), status: %s, output: '%s'"%(test_num,baseline_json,result_json,status_diff, output_diff))
            else:
                print_pass("Test number %s passed!  No differences in the filtered .json files (%s,%s)"%(test_num,baseline_json,result_json))

    else:
        print_error("Curl command return a bad status and output doesnt start with json: %s, output: '%s'"%(status,output))

    thick_divider()
    
def run_test(test):
    '''
    Runs the curl test specified the passed test list
    '''
    
    run_curl_test(test_num=test[0],test_name=test[1],
                  test_desc=test[2],curl_cmd=test[3],
                  use_json_diff=test[4],filters=test[5],
                  delay=test[7] if (len(test) > 7) else None)
    
    
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
            cmd = 'curl %s %s %s "%s%s?createSite=true"'%(CURL_FLAGS, CURL_POST_FLAGS, data, base_url, branch)
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
1,
"PostSite",
"Create a project and site",
create_curl_cmd(type="POST",data='\'{"elements":[{"sysmlid":"123456","name":"JW_TEST","specialization":{"type":"Project"}}]}\'',
                base_url=BASE_URL_WS,
                branch="master/sites/europa/projects",project_post=True),
False, 
None,
["test","workspaces","develop"]
],
 
[
2,
"PostElementsNew",
"Post elements to the master branch",
create_curl_cmd(type="POST",data="elementsNew.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters,
["test","workspaces","develop"]
],
        
[
3,
"PostViews",
"Post views",
create_curl_cmd(type="POST",data="views.json",base_url=BASE_URL_JW,
                post_type="views",branch=""),
False, 
None,
["test","workspaces","develop"]
],
        
[
4,
"PostProducts",
"Post products",
create_curl_cmd(type="POST",data="products.json",base_url=BASE_URL_JW,
                post_type="products",branch=""),
False, 
None,
["test","workspaces","develop"]
],
  
# GETS: ==========================    
[
5,
"GetProject",
"Get project",
create_curl_cmd(type="GET",data="sites/europa/projects/123456",base_url=BASE_URL_WS,
                branch="master/"),
False, 
None,
["test","workspaces","develop"]
],
        
[
6,
"GetElements",
"Get elements",
create_curl_cmd(type="GET",data="elements/123456?recurse=true",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters,
["test","workspaces","develop"]
],
        
[
7,
"GetViews",
"Get views",
create_curl_cmd(type="GET",data="views/301",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters,
["test","workspaces","develop"]
],
        
[
8,
"GetViewElements",
"Get view elements",
create_curl_cmd(type="GET",data="views/301/elements",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters,
["test","workspaces","develop"]
],
     
[
9,
"GetProducts",
"Get product",
create_curl_cmd(type="GET",data="products/301",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters,
["test","workspaces","develop"]
],
                   
[
10,
"GetProductList",
"Get product list",
create_curl_cmd(type="GET",data="ve/documents/europa?format=json",base_url=SERVICE_URL,
                branch=""),
True, 
common_filters,
["test","workspaces","develop"]
],
   
# Note: Need a delay before doing this search, b/c it will find "some*" within the
#       documentation, which does not get indexed by alfresco right away
[
11,
"GetSearch",
"Get search",
create_curl_cmd(type="GET",data="",base_url=BASE_URL_JW,
                branch="element/search?keyword=some*"),
True, 
common_filters,
["test","workspaces","develop"],
80
],

# DELETES: ==========================    
      
[
12,
"Delete6666",
"Delete element 6666",
create_curl_cmd(type="DELETE",data="elements/6666",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters+['"timestamp"','"sysmlid"','"id"','"qualifiedId"','"version"'],
["test","workspaces","develop"]
],
        
# POST CHANGES: ==========================    

[
13,
"PostChange",
"Post changes to directed relationships only (without owners)",
create_curl_cmd(type="POST",data="directedrelationships.json",base_url=BASE_URL_JW,
                branch="sites/europa/projects/123456/",post_type="elements"),
True, 
common_filters,
["test","workspaces","develop"]
],
        
# CONFIGURATIONS: ==========================    

[
14,
"PostConfig",
"Post configuration",
create_curl_cmd(type="POST",data="configuration.json",base_url=BASE_URL_JW,
                branch="configurations/europa",post_type=""),
True, 
common_filters+['"timestamp"','"id"'],
["test","workspaces","develop"]
],
        
[
15,
"GetConfig",
"Get configurations",
create_curl_cmd(type="GET",data="configurations/europa",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters+['"timestamp"','"id"'],
["test","workspaces","develop"]
],
        
# WORKSPACES: ==========================    

[
16,
"CreateWorkspace1",
"Create workspace test 1",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsA?sourceWorkspace=master"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"'],
["test","workspaces","develop"]
],
        
[
17,
"CreateWorkspace2",
"Create workspace test 2",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                post_type="",branch="wsB?sourceWorkspace=wsA"),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"','"parent"'],
["test","workspaces","develop"]
],
        
[
18, 
"GetWorkspaces",
"Get workspaces",
create_curl_cmd(type="GET",base_url=BASE_URL_WS_NOBS,branch=""),
True, 
common_filters+['"branched"','"created"','"id"','"qualifiedId"','"parent"'],
["test","workspaces","develop"]
],

[
19,
"PostToWorkspace",
"Post element to workspace",
create_curl_cmd(type="POST",data="x.json",base_url=BASE_URL_WS,
                post_type="elements",branch="wsB/"),
True, 
common_filters,
["test","workspaces","develop"]
],

[
20,
"CompareWorkspaces",
"Compare workspaces",
create_curl_cmd(type="GET",base_url=SERVICE_URL,
                branch="diff?workspace1=wsA&workspace2=wsB"),
True, 
common_filters+['"id"','"qualifiedId"'],
["test","workspaces","develop"]
],

# SNAPSHOTS: ==========================    

[
21,
"PostSnapshot",
"Post snapshot test",
create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                branch="master/sites/europa/products/301/",
                post_type="snapshots"),
True, 
common_filters+['"created"','"id"','"url"'],
["test","workspaces","develop"]
],

# EXPRESSIONS: ==========================    

[
22,
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
    
    sys.exit(failed_tests)
    
    
    
