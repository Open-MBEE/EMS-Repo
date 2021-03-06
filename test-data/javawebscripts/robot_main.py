import robottesting.robot.lib.keyword_lib
from robot.api.deco import keyword
import sys
import optparse
import re

# MMS Test Library
import regression_test_harness
from docutils.nodes import option_string

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


def generate_full_robot_test_suite(file_action):
    with open(OUTPUT_FILENAME, file_action) as file_object:
        generated_keywords = []
        # Test Case Header
        file_object.write("*** Test Cases ***\n")

        # For each test within tests generate robot formatted test case output
        for test in regression_test_harness.tests:
            if test[0] is not 112:
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


def generate_bash_script_robot_test_suite(file_action, local_robot=True, eval=True):
    generated_keywords = []
    with open(OUTPUT_FILENAME, file_action) as file_object:
        # Test Case Header
        file_object.write("*** Test Cases ***\n")

        file_object.write("\n")

        if eval:
            option_string = "-v -g develop -t "
        else:
            option_string = "-g develop -t "

        # For each test within tests generate robot formatted test case output
        for test in regression_test_harness.tests:
            gen_test_case = False
            if test[6] is not None:
                branches = test[6]
                for branch in branches:
                    if branch == "develop":
                        gen_test_case = True
                    if test[0] == 112:
                        gen_test_case = False

            if gen_test_case:
                # Write the name of the test
                file_object.write(test[1] + "\n")

                test_spec_length = len(test)

                # Documentation (Description)
                file_object.write(
                    "\t[Documentation]\t\t" + "\"Regression Test: " + str(test[0]) + ". " + str(test[2]) + "\"\n")

                if local_robot:
                    file_object.write(
                        "\t${output}= \t\t OperatingSystem.Run\t\t" + "./regress.sh " + option_string + str(
                            test[0]) + "\n")
                else:
                    file_object.write(
                        "\t${output}= \t\t OperatingSystem.Run\t\t" + "./test-data/javawebscripts/regress.sh " + option_string + str(
                            test[0]) + "\n")

                file_object.write("\tlog\t\t ${output}\n")
                file_object.write("\tlog to console\t\t ${output}\n")
                file_object.write("\tShould Not Contain\t\t${output}\t\tFAIL:\n")
                file_object.write("\tShould Contain\t\t\t${output}\t\tPASS:\n")
                file_object.write("\n")


def set_full_regression_suite():
    with open(OUTPUT_FILENAME, 'a') as file_object:
        file_object.write("\nRegression Suite\n")
        file_object.write(
            "\t[Documentation]\t\t" + "This will execute the entire regression suite to generate the output required for the following tests to compare.\n")
        file_object.write("\t${output}= \t\tOperatingSystem.Run\t\t" + "./regress.sh -g develop\n")
        file_object.write("\tlog\t\t ${output}\n")
        file_object.write("\tlog to console\t\t ${output}\n")
        file_object.write("\tShould Not Contain\t\t${output}\t\tFAIL:\n")
        file_object.write("\tShould Contain\t\t${output}\t\tPASS:\n")


def generate_test_case(test_num=None, test_name=None):
    with open(OUTPUT_FILENAME, 'a') as file_object:
        for test in regression_test_harness.tests:
            if test[0] == test_num or test[1] == test_name:
                # Documentation (Description)
                file_object.write(test[1])
                file_object.write(
                    "\t[Documentation]\t\t" + "\"Regression Test: " + str(test[0]) + ". " + str(test[2]) + "\"\n")
                file_object.write(
                    "\t${output}= \t\tOperatingSystem.Run\t\t" + "./regress.sh -g develop -t " + test[0] + "\n")
                file_object.write("\tlog\t\t ${output}\n")
                file_object.write("\tlog to console\t\t ${output}\n")
                file_object.write("\tShould Not Contain\t\t${output}\t\tFAIL:\n")
                file_object.write("\tShould Contain\t\t${output}\t\tPASS:\n")


def generate_tests(test_names=None, test_numbers=None):
    if test_names is not None:
        for test in test_names:
            generate_test_case(test_name=test[1])
    if test_numbers is not None:
        for test in test_numbers:
            generate_test_case(test_num=test[0])

if __name__ == '__main__':
    # Automatically generate the Robot Framework Test Suite for the MMS when executing the python file.
    set_test_suite_settings('w')
    set_test_suite_variables('a')
    generate_bash_script_robot_test_suite('a', False, True)

