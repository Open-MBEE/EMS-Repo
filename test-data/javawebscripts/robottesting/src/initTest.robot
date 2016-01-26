| *Setting*  |     *Value*     |
| Library    | ../../test-data/javawebscript/regression_lib.py |
| Library    | ../../test-data/javawebscript/regression_test_harness.py |

# | *Variable* |     *Value*     |
# | ${MESSAGE} | Hello, world!   |

| *Test Case*  | *Action*        | *Argument*   |
| Test 10      | [Documentation] | Test 10      |
|              | Log             | ${MESSAGE}   |
|              | My Keyword      | /tmp         |
| Another Test | Should Be Equal | ${MESSAGE}   | Hello, world!

| *Keyword*  |
| My Keyword | [Arguments] | ${path}
|            | Directory Should Exist | ${path}