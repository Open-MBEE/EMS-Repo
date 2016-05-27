*** Settings ***
Documentation       Suite description
Library             OperatingSystem

*** Test Cases ***
Test title
    [Tags]    DEBUG
    ${output}=  run    ./regress.sh -g develop
    log                 ${output}
    log to console      ${output}
