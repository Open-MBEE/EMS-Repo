*** Settings ***
Documentation       This is the main robot test suite for the Alfresco View Repo.
...
...                 Suite contains all regression tests that are ran within the regression_test_harness.py
...                 The test code and setup
...
...
...
Library     regression_lib.py
Library     regression_test_harness.py
Library     OperatingSystem
#Library     keyword_lib


#run_curl_test(test_num=test[0],test_name=test[1],
#                  test_desc=test[2],curl_cmd=test[3],
#                  use_json_diff=test[4],filters=test[5],
#                  setupFcn=test[7] if (len(test) > 7) else None,
#                  postProcessFcn=test[8] if (len(test) > 8) else None,
#                  teardownFcn=test[9] if (len(test) > 9) else None,
#                  delay=test[10] if (len(test) > 10) else None)

*** Variables ***
${TEST_NUM}
${TEST_NAME}
${TEST_DESC}
${CURL_CMD}
${USE_JSON_DIFF}
${TEST_FILTERS}
${SETUP_FUNC}
${POST_PROC}
${TEARDOWN_FUNC}
${DELAY}
${REGRESSION_TESTS}     regression_test_harness.tests

*** Test Cases ***
Initial Test
    [Documentation]     This is the initial test to attempt to get the currently implemented MMS test library.


Another Test
*** Keywords ***
    [Arguments]     