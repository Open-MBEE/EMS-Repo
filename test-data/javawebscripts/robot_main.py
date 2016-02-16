import robottesting.robot.lib.keyword_lib
from robot.api.deco import keyword
import re

# MMS Test Library
import regression_test_harness

OUTPUT_FILENAME = "regression_test_suite.robot"


def set_test_suite_settings(file_action):
    # Add the settings for the test case
    # Setting Headers
    with open(OUTPUT_FILENAME, file_action) as file_object:
        file_object.write("*** Settings ***\n")

        # Libraries to be added
        # TODO: Dynamically adding Libraries to be used
        file_object.write("Library\t\t" + "OperatingSystem\n")
        file_object.write("Library\t\t" + "regression_lib.py\n")
        file_object.write("Suite Setup\t\t" + "parse_command_line\n")
        file_object.write("\n")


def set_test_suite_variables(file_action):
    with open(OUTPUT_FILENAME, file_action) as file_object:
        # Variable Declarations
        file_object.write("*** Variables ***\n")
        # file_object.write("@{CURL_COMMAND}\n")
        # file_object.write("${REGRESSION_TESTS}" + "\t\t" + str(regression_test_harness.tests) + "\n")
        # file_object.write("${TEST_NUMS}\t\t" + "\n")
        # file_object.write("${TEST_NAMES}\t\t" + "\n")
        # file_object.write("${CMD_GIT_BRANCH}\t\t" + "\n")
        file_object.write("${evaluate_only}\t\t" + "set_true" + "\n")
        file_object.write("\n")


def set_test_suite_keywords(file_action):
    with open(OUTPUT_FILENAME, file_action) as file_object:
        file_object.write("*** Keywords ***\n")

        file_object.write("Create Curl Command\n")
        file_object.write("\t[Arguments]\t\t\t" + "@{varargs}" + "\n")
        file_object.write("\tcreate_curl_cmd\t\t" + "@{varargs}" + "\n")

        file_object.write("\n")

        file_object.write("Execute Curl Command\n")
        file_object.write("\t[Arguments]\t\t\t" + "@{varargs}\n")
        file_object.write("\trun_curl_test\t\t" + "@{varargs}\n")

        file_object.write("Regression\n")
        file_object.write("\tregression_test_harness.run curl test\t\t" + "@{varargs}\n")


def generate_test_suite(file_action):
    generated_keywords = []
    with open(OUTPUT_FILENAME, file_action) as file_object:
        # Test Case Header
        file_object.write("*** Test Cases ***\n")

        # For each test within tests generate robot formatted test case output
        for test in regression_test_harness.tests:
            # Write the name of the test
            file_object.write(test[1] + "\n")

            test_spec_length = len(test)

            # Documentation (Description)
            file_object.write(
                "\t[Documentation]\t\t" + "\"Regression Test: " + str(test[0]) + ". " + str(test[2]) + "\"\n")

            # Setup and Teardown of the test (Both optional)
            #   Checks if a setup function was given
            if (test_spec_length > 7):
                if (test[7] is not None):
                    file_object.write("\t[Setup]\t\t\t\t" + str(test[7].__name__) + "\n")

            # Set the test number for the test case
            file_object.write("\t${test_num} = \t\t Set Variable\t\t" + str(test[0]) + "\n")

            # Use JSON Diff
            if (test[4] is not None):
                file_object.write("\t${use_json_diff} =\t Set Variable\t\t" + str(test[4]) + "\n")

            # file_object.write("\t${curl_cmd} =\t\t Set Variable\t\t" + "\""+str(test[3]) + "\"\n")
            if (test[5] is not None):
                file_object.write("\t@{output_filters} =\t Set Variable\t\t")
                for filter in test[5]:
                    file_object.write(filter + "\t\t")
                file_object.write("\n")

            # Set Branch Name Variable
            if (test[6] is not None and len(test[6]) > 0):
                if (len(test[6]) > 2):
                    file_object.write("\t@{branch_names} =\t Set Variable\t\t")
                else:
                    file_object.write("\t${branch_names} =\t Set Variable\t\t")
                for branch in test[6]:
                    file_object.write(branch + "\t\t\t")
                file_object.write("\n")

            # Curl Command
            #   Insert \ to escpace the % character that robot interprets as an environment variable
            curl_string = str(test[3])
            http_code_length = len("%{http_code}")
            hc_begin_index = curl_string.find("%{http_code}")
            new_curl_string = curl_string[0:hc_begin_index] \
                              + "\%{http_code}" \
                              + curl_string[hc_begin_index + http_code_length:len(curl_string)]

            # file_object.write("\tCreate Curl Command\t\t" + new_curl_string)
            # file_object.write("\n")

            # Teardown Functions
            if (test_spec_length > 8):
                if (test[8] is not None):
                    file_object.write("\t[Teardown]\t\t\t" + str(test[8].__name__) + "\n")
                    generated_keywords.append(test[8].__name__)

                if (test_spec_length > 9):
                    if (test[9] is not None):
                        file_object.write("\t[Teardown]\t\t\t" + str(test[9].__name__) + "\n")
                        generated_keywords.append(test[9].__name__)
                    if (test_spec_length > 10):
                        # Delay in seconds before running the test (optional)
                        file_object.write("\t[Timeout]\t\t\t" + str(test[10]) + "\n")

            # Run Curl Test Takes the arguments in the following order:
            #   test_num        test_name       test_desc           curl_cmd   use_json_diff=False
            #   filters=None    setupFcn=None   postProcessFcn=None teardownFcn=None    delay=None
            file_object.write("\trun curl test\t\t" + str(test[0]) + "\t\t" + test[1] + "\t\t" + test[2] +
                              "\t\t" + new_curl_string + "\t\t")

            # file_object.write("\trun curl test\t\t" + str(test[0]) + "\t\t" + str(test[1]) + "\t\t" + str(test[2]) +
            #                   "\t\t" + str(test[3])+ "\t\t")

            for index in range(4, test_spec_length):
                file_object.write(str(test[index]) + "\t\t")

            file_object.write("\n")
            file_object.write("\n")

            # file_object.write("Regression\t\t" + str(test[0]) + "\n")


def generate_test_case(file_action):
    with open(OUTPUT_FILENAME, file_action) as file_object:
        file_object.write()

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
    parser.add_option("-v","--evaluate",action="store_true",dest="evaluate_only",
                      help='''Do not execute the tests. Just evaluate the existing output. (Optional)''')

    return parser

def parse_command_line():
    '''Parse the command line options given to this application'''

    global test_nums, create_baselines, evaluate_only, cmd_git_branch, test_names

    parser = create_command_line_options()

    parser.test_nums = None
    parser.cmd_git_branch = None
    parser.test_names = None

    (_options,_args) = parser.parse_args()

    test_nums = parser.test_nums
    test_names = parser.test_names
    create_baselines = _options.create_baselines
    evaluate_only = _options.evaluate_only
    cmd_git_branch = parser.cmd_git_branch

    if test_nums and test_names:
        print "ERROR: Cannot supply both the -t and -n options!  Please remove one of them."
        sys.exit(1)


if __name__ == "__main__":
    # Automatically generate the Robot Framework Test Suite for the MMS when executing the python file.
    set_test_suite_settings('w')
    set_test_suite_variables('a')
    generate_test_suite('a')
    set_test_suite_keywords('a')
