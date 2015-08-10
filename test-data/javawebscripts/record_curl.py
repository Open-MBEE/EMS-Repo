#!/usr/bin/env python

#must be places in test-data/javawebscripts directory
import os
import sys
from regression_lib import create_curl_cmd
import optparse
import commands
from __builtin__ import True, False

#test_dir_path = "git/alfresco-view-repo/test-data/javawebscripts"
HOST = "localhost:8080"
SERVICE_URL = "http://%s/alfresco/service/"%HOST
BASE_URL_WS_NOBS = SERVICE_URL + "workspaces"
BASE_URL_WS = BASE_URL_WS_NOBS + "/"

#######################################

parser = optparse.OptionParser()

parser.add_option("-n", "--testName", help="Mandatory option: test name to create the baseline json")
parser.add_option("-g", "--gitBranch", default=os.getenv("GIT_BRANCH", "test"), help="Specify the branch to use or uses the $GIT_BRANCH value using 'test' if it doesn't exist")
parser.add_option("-c", "--curl", default="", help="Input entire desired curl command")

parser.add_option("--host", default=HOST, help="DEFAULT: " + HOST)
parser.add_option("-t", "--type", help="Type of curl command: POST, GET, DELETE")
parser.add_option("-d", "--data", default="", help="Data to post in json")
parser.add_option("-u", "--url", default=BASE_URL_WS, help="Base URL to use DEFAULT: " + BASE_URL_WS)
parser.add_option("-p", "--post", default="elements", help="Post-type: elements, views, products DEFAULT: elements")
parser.add_option("-b", "--branch", default="master/", help="The workspace branch DEFAULT: master/")
parser.add_option("-o", "--project", default="False", help="Set to True if creating a project DEFAULT: False")
parser.add_option("-f", "--filter", default="", help="A string of comma separated values to be removed from the output i.e. \"filter1,filter2,filter3...\" (no spaces)")

#options to add test to the regression test harness
parser.add_option("--description", help="Test description")
parser.add_option("--jsonDiff", help="Use jsondiff")
parser.add_option("--runBranches", default="", help="A string of comma separated branch names that will run this test by default")

options, args = parser.parse_args()

if options.host != "":
    HOST = options.host

SERVICE_URL = "http://%s/alfresco/service/"%HOST
BASE_URL_WS_NOBS = SERVICE_URL + "workspaces"
BASE_URL_WS = BASE_URL_WS_NOBS + "/"

#######################################
#Error Messages

#need a test name in order to create the baseline
if options.testName is None:
    parser.error("Test name needed to create baseline")

#if there is no -c input, the other six must be present
if options.curl is None and options.type is None:
    parser.error("Type of curl command is required to create curl command")

#######################################
#finding the git branch
if "/" in options.gitBranch:
    options.gitBranch = options.gitBranch.split("/")[1]

#creating the baseline and directory names
baseline_dir = "%sBaselineDir"%options.gitBranch
baseline_json = "%s/%s.json"%(baseline_dir, options.testName)
baseline_orig_json = "%s/%s_orig.json"%(baseline_dir, options.testName)

curl_base_url = ""
curl_data = ""
#create curl command
if options.url == "BASE_URL_WS":
    curl_base_url = BASE_URL_WS
elif options.url == "SERVICE_URL":
    curl_base_url = SERVICE_URL
elif options.url == "BASE_URL_WS_NOBS":
    curl_base_url = BASE_URL_WS_NOBS
else:
    curl_base_url = options.url

if options.data and options.data[0] == "{" and options.data[-1] == "}":
    curl_data = "'" + options.data + "'"
else:
    curl_data = options.data

if options.curl == "":
     curl_cmd = create_curl_cmd(type=options.type, data=curl_data, base_url=curl_base_url, post_type=options.post, branch=options.branch, project_post=options.project)
else:
     curl_cmd = options.curl

print "\n" + curl_cmd
user = raw_input("Is this the desired curl command? (y/n) ")

if user != "y":
    sys.exit()

#######################################

#os.chdir(test_dir_path)

#making the baseline directory if it doesn't exist
if not os.path.exists(baseline_dir):
    os.makedirs(baseline_dir)

#######################################

print "Executing curl command\n"
#returns the status and output of executing command in a shell
(status, output) = commands.getstatusoutput(curl_cmd + "> " + baseline_orig_json)
print output + "\n"
print "Creating baseline %s.json in %s"%(options.testName, baseline_dir)

if status == 0:
    file_orig = open(baseline_orig_json, "r")

    #apply filters to output of curl cmd (not using output b/c getstatusoutput pipes stderr to stdout):
    orig_output = ""
    filter_output = ""
    if options.filter is not "":
        filters = options.filter.split(",")
        for line in file_orig:
            filterFnd = False
            for filter in filters:
                #if the output contains the filter:
                if re.search(filter, line):
                    filterFnd = True
                    break

            #add line if it does not contain the filter:
            if not filterFnd:
                filter_output += (line)

            #always add lines to orig_output
            orig_output += (line)

    else:
        fileRead = file_orig.read()
        filter_output = fileRead
        orig_output = fileRead
    #write the baseline file with the output json file with filters
    file = open(baseline_json, "w")
    file.write(filter_output)
    file.close()
    file_orig.close()

def isTestNumber(testNum):
    try:
        int(testNum)
        return True
    except ValueError:
        return False
    
print "Adding test case into regression test harness"
file = open("copy_regression_test_harness.py", "r")
lines = file.readlines()
file.close()

latestTest = 0
#check the file for the latest test case number
for line in lines:
    if isTestNumber(line[:-2]):
        currentNumber = int(line[:-2])
        if currentNumber > latestTest:
            latestTest = currentNumber

#creates table for the test harness
listOfFilters = "common_filters"
listOfBranches = ""

def createListOfValues(values):
    values = values.split(",")
    listOfValues = '['
    for value in values:
        entry = '"' + value + '",'
        listOfValues += entry
    listOfValues = listOfValues[:-1] + ']'
    return listOfValues

if options.filter != "":
    listOfFilters += '+'
    listOfFilters += createListOfValues(options.filter)
if options.runBranches != "":
    listOfBranches = createListOfValues(options.runBranches)
       
value = "[\n" + str(latestTest + 1) + ',\n"' + options.testName + '",\n"' + options.description + \
        '",\n' + 'create_curl_cmd(type="' + options.type + '", data="' + curl_data + '", base_url="' + \
        curl_base_url + '", post_type="' + options.post + '", branch="' + options.branch + '", project_post=' + \
        options.project + '),\n' + options.jsonDiff + ',\n' + listOfFilters + ',\n' + listOfBranches + '\n],\n'

#insert the brace and leave room so that the next test can be input
i = lines.index("]\n")
lines.insert(i, value + "\n")
lines = "".join(lines)

file = open("regression_test_harness.py", "w")
file.write(lines)
file.close()


