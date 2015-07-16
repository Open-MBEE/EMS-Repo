

from regression_lib import *
import commands

# default parameters
site = 'europa'
project = '123456'
folder = 'generated'
numberOfElements=100
numberOfElementsToPostAtATime = 3
numberOfChangesPerElement = 10
numberOfChangesToPostAtATime = 3
folderBranchingFactor=10  # 0 means everything in the same folder

def doIt():
    elementsJsonStrTemplate = '\'{"elements":[%s]}\''
    elementsJsonArrayStr = ''
    ct = 0
    for i in range(0,numberOfElements):
        iStr = "%06d"%i
        id = "e_" + iStr
        jStr = '0'
        name = id + "_" + jStr
        elemJsonStr = '{"sysmlid":"' + id + '","name":"' + name + '"}'
        if elementsJsonArrayStr == '':
            elementsJsonArrayStr = elementsJsonArrayStr + ',' + elemJsonStr
        else:
            elementsJsonArrayStr = elemJsonStr
            ct = ct + 1
        if ct % numberOfElementsToPostAtATime == 0:
            if elementsJsonArrayStr != '':
                elementsJsonStr = elementsJsonStrTemplate%elementsJsonArrayStr
                post(elementsJsonStr)
                elementsJsonArrayStr = ''
    # post any remaining elements
    if elementsJsonArrayStr != '':
        elementsJsonStr = elementsJsonStrTemplate%elementsJsonArrayStr
        post(elementsJsonStr)
        elementsJsonArrayStr = ''
    ct = 0
    for i in range(0,numberOfElements):
        elementsJsonStr = '\'{"elements":[%s]}\''
        iStr = "%06d"%i
        id = "e_" + iStr
        for j in range(0,numberOfChangesPerElement):
            jStr = "%06d"%j
            name = id + "_" + jStr
            elementJsonStr = '{"sysmlid":"' + id + '","name":"' + name + '"}'
            if elementsJsonArrayStr == '':
                elementsJsonArrayStr = elementsJsonArrayStr + ',' + elemJsonStr
            else:
                elementsJsonArrayStr = elemJsonStr
            ct = ct + 1
            if ct % numberOfChangesToPostAtATime == 0:
                elementsJsonStr = elementsJsonStrTemplate%elementsJsonArrayStr
                post(elementsJsonStr)
                elementsJsonArrayStr = ''
    # post any remaining elements
    if elementsJsonArrayStr != '':
        elementsJsonStr = elementsJsonStrTemplate%elementsJsonArrayStr
        post(elementsJsonStr)
        elementsJsonArrayStr = ''

def post(elementsJsonStr):
    curl_cmd = create_curl_cmd(type="POST",data='\'{"elements":[{]}\'', \
                               base_url=BASE_URL_WS, \
                               branch="master/sites/europa/projects?createSite=true", \
                               project_post=True)
    (status,output) = commands.getstatusoutput(curl_cmd)
