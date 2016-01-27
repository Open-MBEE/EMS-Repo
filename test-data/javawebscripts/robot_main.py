import robottesting.robot.lib.keyword_lib
from robot.api.deco import keyword

# MMS Test Library
import regression_test_harness

OUTPUT_FILENAME = "regression_test_cases.robot"


def generate_test_suite():
    generated_keywords = []
    with open(OUTPUT_FILENAME, 'w') as file_object:
        # Add the settings for the test case
        # Setting Headers
        file_object.write("*** Settings ***\n")
        # Libraries to be added
        file_object.write("Library\t\t" + "OperatingSystem\n")
        file_object.write("Library\t\t" + "regression_lib.py\n")

        file_object.write("\n")

        file_object.write("*** Variables ***\n")
        # file_object.write("%{http_code}\n")
        file_object.write("\n")

        file_object.write("*** Test Cases ***\n")
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

            # Use JSON Diff
            if (test[4] is not None):
                file_object.write("\t${use_json_diff} =\t Set Variable\t\t" + str(test[4]) + "\n")

            # file_object.write("\t${curl_cmd} =\t\t Set Variable\t\t" + "\""+str(test[3]) + "\"\n")
            if (test[5] is not None):
                file_object.write("\t@{output_filters} =\t Set Variable\t\t")
                for filter in test[5]:
                    file_object.write(filter + "\t\t")
                file_object.write("\n")

            if (test[6] is not None and len(test[6]) > 0):
                if (len(test[6]) > 2):
                    file_object.write("\t@{branch_names} =\t Set Variable\t\t")
                else:
                    file_object.write("\t${branch_names} =\t Set Variable\t\t")
                for branch in test[6]:
                    file_object.write(branch + "\t\t\t")
                file_object.write("\n")

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

            file_object.write("\n")

            # file_object.write("*** Keywords ***\n")
            # file_object.write("use_json_diff\n")
            # file_object.write("\t[Arguments]\t\t" + "${arg1}" + "\n")
            # file_object.write("\n")
            #
            # file_object.write("curl_cmd\n")
            # file_object.write("\t[Arguments]\t\t" + "${arg1}" + "\n")
            # file_object.write("\n")
            #
            # file_object.write("output_filters\n")
            # file_object.write("\t[Arguments]\t\t" + "@{varargs}" + "\n")
            # file_object.write("\n")
            #
            # file_object.write("branch_names\n")
            # file_object.write("\t[Arguments]\t\t" + "@{varargs}" + "\n")
            # file_object.write("\n")


# Automatically generate the Robot Framework Test Suite for the MMS when executing the python file.
generate_test_suite()
