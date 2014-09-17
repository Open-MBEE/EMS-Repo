#
# TODO:
#    -Fix run server if we use it
#    -Get SOAP UI tests working
#
#    -Find correct mbee_util jar file for JsonDiff
#    -Test 14/15 returning 500, 10 not returning a json output
#

import os
import commands
import re
import time
import subprocess
import sys
import optparse

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
BASE_URL_WS = SERVICE_URL+"workspaces/"
BASE_URL_JW = SERVICE_URL+"javawebscripts/"

soap_server = "128.149.16.xxx:8080"
failed_tests = 0
passed_tests = 0
result_dir = ""
baseline_dir = ""
display_width = 80
test_dir_path = "test-data/javawebscripts"
test_nums = []
create_baselines = False
common_filters = ['"read"','"lastModified"','"modified"']

def create_command_line_options():

    '''Create all the command line options for this application
    
    Returns
    --------
    An optparse.OptionParser object for parsing the command line arguments fed to this application'''
    
    usageText = '''
    python regression_test_harness.py [-t <TESTNUMS> -b]
    
    To run all tests
    
    python regression_test_harness.py -t 1,2,5-9,11

    To run test numbers 1,2,5-9,11
    
    python regression_test_harness.py -t 1,2,5-9,11
    
    To create baselines of all tests.  After doing this you will need to copy them from testBaselineDir 
    into the desired folder for that branch, ie workspacesBaselineDir, if you like the results.  
    This is b/c when running this outside of jenkins, the script will output to testBaselineDir.
    If you dont like this, then simply define the env variable GIT_BRANCH to be the name of the branch
    before running the script.
    
    python regression_test_harness.py -b
    '''
                
    versionText = 'Version 1 (9_16_2014)'
    
    parser = optparse.OptionParser(usage=usageText,version=versionText)
        
    parser.add_option("-t","--testNums",action="callback",type="string",metavar="TESTNUMS",callback=parse_test_nums,
                      help='''Specifiy the tests to run or create baselines for.  Otherwise does all tests. ie "1,2,5-9,11"  (Optional)''')
    parser.add_option("-b","--createBaselines",action="store_true",dest="create_baselines",
                      help='''Supply this option if you want to create the baseline files for the tests (Optional)''')
    return parser

def parse_command_line():
    '''Parse the command line options given to this application'''

    global test_nums, create_baselines
    
    parser = create_command_line_options()
    
    parser.test_nums = None
    
    (_options,_args) = parser.parse_args()
    
    test_nums = parser.test_nums
    create_baselines = _options.create_baselines
 
        
def parse_test_nums(option, opt, value, parser):
    '''
    Parses out the section numbers ran from the passed string and creates
    a list of the corresponding section numbers, ie "1,3-5,7" will
    create [1,3,4,5,7].  Assigns this to list to parser.test_nums
    '''
    
    keyList = []
    
    if value is not None:
        value = value.strip()
        
        # value is comma separated ie 1,3-5,7:
        if value.find(',') != -1:
            keyListArray = value.split(',')
            
            for keyStr in keyListArray:
                                          
                # testKey is a range ie 1-5:
                if keyStr.find('-') != -1:
                    keyListBounds = keyStr.split('-')
                    lowerBound = int(keyListBounds[0])
                    upperBound = int(keyListBounds[1])
        
                    if lowerBound > upperBound:
                        print ('The testKey range ' + value + ' has a lower bound greater than the upper bound')
                        print "Abadoning the script!"
                        sys.exit(1)
                    
                    for key in range(lowerBound,upperBound+1):
                        keyList.append(key)
                    
                # It was a single key:
                else:
                    keyList.append(int(keyStr))
                    
        # value is just a range ie 1-3:
        elif value.find('-') != -1:
            keyListBounds = value.split('-')
            lowerBound = int(keyListBounds[0])
            upperBound = int(keyListBounds[1])
        
            if lowerBound > upperBound:
                print ('The testKey range ' + value + ' has a lower bound greater than the upper bound')
                print "Abadoning the script!"
                sys.exit(1)
            
            keyList = range(lowerBound,upperBound+1)
            
        #value was just a single key:
        else:
            keyList = [int(value)]
                    
    parser.test_nums = keyList

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

def run_curl_test(test_num, test_desc, curl_cmd, use_json_diff=False, filters=None):
    '''
    Runs the curl test and diffs against the baseline if create_baselines is false, otherwise
    runs the curl command and creates the baseline .json file.  Assumption is this script
    is ran from the alfresco-view-repo folder.  It will create result and baseline folders
    if needed, and bases the name on the GIT_BRANCH env variable which is populated
    by Jenkins.  If ran manually, it will use "test" as the branch name.
    
    test_num: The unique test number for this test
    test_desc: The test description
    curl_cmd: The curl command to send
    use_json_diff: Set to True to use a JsonDiff when comparing to the baseline
    filters: A list of strings that should be removed from the post output, ie ['"modified"']
    '''
    
    result_json = "%s/test%d.json"%(result_dir,test_num)
    result_orig_json = "%s/test%d_orig.json"%(result_dir,test_num)
    baseline_json = "%s/test%d.json"%(baseline_dir,test_num)
    baseline_orig_json = "%s/test%d_orig.json"%(baseline_dir,test_num)

    thick_divider()
    if create_baselines:
        print "CREATING BASELINE FOR TEST NUMBER "+str(test_num)
        orig_json = baseline_orig_json
        filtered_json = baseline_json
    else:
        print "TEST NUMBER "+str(test_num)
        orig_json = result_orig_json
        filtered_json = result_json
        
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
                # TODO: search for mbee_util .jar
                cp = ".:../../../../.m2/repository/gov/nasa/jpl/mbee/util/mbee_util/0.0.16/mbee_util-0.0.16.jar:../../target/view-repo-war/WEB-INF/lib/json-20090211.jar:../../target/classes"
                diff_cmd = "java -cp %s gov.nasa.jpl.view_repo.util.JsonDiff"%cp
            else:
                 diff_cmd = "diff"
                 
            (status_diff,output_diff) = commands.getstatusoutput("%s %s %s"%(diff_cmd,baseline_json,result_json))
                 
            if output_diff:
                print_error("Diff returned bad status or diffs found, status: %s, output: '%s'"%(status_diff, output_diff))
            else:
                print_pass("Test number %s passed!  No differences in the filtered .json files (%s,%s)"%(test_num,baseline_json,result_json))

    else:
        print_error("Curl command return a bad status and output doesnt start with json: %s, output: '%s'"%(status,output))
        
    thick_divider()
    
def create_curl_cmd(type, data=None, base_url=BASE_URL_WS, post_type="elements", branch="master/", 
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
    '''%BASE_URL_WS
    
    cmd = ""
    
    if type == "POST":
        if project_post:
            cmd = 'curl %s %s %s "%s%s?fix=true&createSite=true"'%(CURL_FLAGS, CURL_POST_FLAGS, data, base_url, branch)
        elif data:
            cmd = 'curl %s %s @JsonData/%s "%s%s%s"'%(CURL_FLAGS, CURL_POST_FLAGS, data, base_url, branch, post_type)
        else:
            cmd = 'curl %s %s "%s%s%s"'%(CURL_FLAGS, CURL_POST_FLAGS, base_url, branch, post_type)
            
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
    p = subprocess.Popen("./runserver_regression.sh") # still not working, eventually hangs and server doesn't come up
    
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
        print "SERVER TIME-OUT"
        kill_server()
        exit(1)
        
        
##########################################################################################
#
# TABLE OF ALL POSSIBLE TESTS TO RUN
#     TEST MUST BE ADDED HERE WITH A UNIQUE TEST NUMBER
#
tests =[\
        
# [       
# Test Number, 
# Test Description,
# Curl Cmd, 
# Use JsonDiff, 
# Output Filters (ie lines in the .json output with these strings will be filtered out)
# ]

# POSTS: ==========================
[
1, 
"Create a project and site",
create_curl_cmd(type="POST",data='\'{"name":"JW_TEST"}\'',base_url=BASE_URL_JW,
                branch="sites/europa/projects/123456",project_post=True),
False, 
None
],
 
[
2, 
"Post elements to the master branch",
create_curl_cmd(type="POST",data="elementsNew.json",base_url=BASE_URL_WS,
                post_type="elements",branch="master/"),
True, 
common_filters
],
        
[
3,
"Post views",
create_curl_cmd(type="POST",data="views.json",base_url=BASE_URL_JW,
                post_type="views",branch=""),
False, 
None
],
        
[
4, 
"Post products",
create_curl_cmd(type="POST",data="products.json",base_url=BASE_URL_JW,
                post_type="products",branch=""),
False, 
None
],
  
# GETS: ==========================    
[
5, 
"Get project",
create_curl_cmd(type="GET",data="sites/europa/projects/123456",base_url=BASE_URL_JW,
                branch=""),
False, 
None
],
        
[
6, 
"Get elements",
create_curl_cmd(type="GET",data="elements/123456?recurse=true",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters
],
        
[
7, 
"Get views",
create_curl_cmd(type="GET",data="views/301",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters
],
        
[
8, 
"Get view elements",
create_curl_cmd(type="GET",data="views/301/elements",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters
],
     
[
9, 
"Get product",
create_curl_cmd(type="GET",data="products/301",base_url=BASE_URL_JW,
                branch=""),
True, 
common_filters
],
           
[
10, 
"Get moaproduct",
create_curl_cmd(type="GET",data="ve/products/301?format=json",base_url=SERVICE_URL,
                branch=""),
True, 
common_filters
],
        
[
11, 
"Get product list",
create_curl_cmd(type="GET",data="ve/documents/europa?format=json",base_url=SERVICE_URL,
                branch=""),
True, 
common_filters
],
        
[
12, 
"Get search",
create_curl_cmd(type="GET",data="",base_url=BASE_URL_JW,
                branch="element/search?keyword=some*"),
True, 
common_filters
],

# DELETES: ==========================    
      
[
13, 
"Delete element 6666",
create_curl_cmd(type="DELETE",data="elements/6666",base_url=BASE_URL_WS,
                branch="master/"),
True, 
common_filters
],
        
# POST CHANGES: ==========================    

[
14, 
"Post changes to directed relationships only (without owners)",
create_curl_cmd(type="POST",data="directedrelationships.json",base_url=BASE_URL_JW,
                branch="sites/europa/projects/123456/",post_type="elements"),
True, 
common_filters
],
        
# SNAPSHOTS: ==========================    

# TODO

# CONFIGURATIONS: ==========================    

[
15, 
"Post configuration",
create_curl_cmd(type="POST",data="configuration.json",base_url=BASE_URL_JW,
                branch="configurations/europa",post_type=""),
True, 
['"read"','"lastModified"']
],
        
[
16, 
"Get configurations",
create_curl_cmd(type="GET",data="configurations/europa",base_url=BASE_URL_JW,
                branch=""),
True, 
['"read"','"lastModified"']
],
        
# WORKSPACES: ==========================    

# TODO


]    

##########################################################################################    
#
# MAIN METHOD 
#
if __name__ == '__main__':
    
    # Parse the command line arguments:
    parse_command_line()
    
    #startup_server()  # this is not working yet, so assumption for now is that this will be called 
                       # by the bash script which will start up the server
    
    # Change directories to where we are used to sending curl cmds:
    if not os.path.exists(test_dir_path):
        print "ERROR: Test directory path '%s' does not exists!\n"%test_dir_path
        exit(1)
    
    os.chdir(test_dir_path)

    # Make the directories if needed:
    git_branch = os.getenv("GIT_BRANCH", "test")
    if "/" in git_branch:
        dir_name = git_branch.split("/")[1]
    else:
        dir_name = git_branch
    result_dir = "%sResultDir"%dir_name
    baseline_dir = "%sBaselineDir"%dir_name
    
    if not os.path.exists(result_dir):
        os.makedirs(result_dir)
        
    if not os.path.exists(baseline_dir):
        os.makedirs(baseline_dir)
        
    print "\nUSING BASELINE DIR: '%s'\nOUTPUT DIR: '%s'\n"%(baseline_dir, result_dir)
    
    # Run tests or create baselines:
    for test in tests:
        test_num = test[0]
        
        # If the test number is in the list of ones to run, or
        # no list of test numbers to run, then run the test:
        if not test_nums or test_num in test_nums:
                run_curl_test(test_num=test_num,
                              test_desc=test[1],
                              curl_cmd=test[2],
                              use_json_diff=test[3],
                              filters=test[4])
    
    # TODO: uncomment when done running manually
#     print "KILLING SERVER"
#     kill_server()
    
    if not create_baselines:
        print "\nNUMBER OF PASSED TESTS: "+str(passed_tests)
        print "NUMBER OF FAILED TESTS: "+str(failed_tests)+"\n"
    
    exit(failed_tests)
    
    
    