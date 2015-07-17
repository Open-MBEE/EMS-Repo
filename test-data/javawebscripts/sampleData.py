

from regression_lib import *
import commands

# default parameters
site = 'europa'
project = '123456'
folder = 'generated'
numberOfElements=5
numberOfElementsToPostAtATime = 2
numberOfChangesPerElement = 3
numberOfChangesToPostAtATime = 2
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
        if elementsJsonArrayStr != '':
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
    for j in range(0,numberOfChangesPerElement):
        jStr = "%06d"%j
        for i in range(0,numberOfElements):
            iStr = "%06d"%i
            id = "e_" + iStr
            name = id + "_" + jStr
            elementJsonStr = '{"sysmlid":"' + id + '","name":"' + name + '"}'
            if elementsJsonArrayStr == '':
                elementsJsonArrayStr = elementsJsonArrayStr + ',' + elemJsonStr
            else:
                elementsJsonArrayStr = elemJsonStr
            ct = ct + 1
            if ct % numberOfChangesToPostAtATime != 0:
                elementsJsonStr = elementsJsonStrTemplate%elementsJsonArrayStr
                post(elementsJsonStr)
                elementsJsonArrayStr = ''
    # post any remaining elements
    if elementsJsonArrayStr != '':
        elementsJsonStr = elementsJsonStrTemplate%elementsJsonArrayStr
        post(elementsJsonStr)
        elementsJsonArrayStr = ''

def post(elementsJsonStr):
    curl_cmd = create_curl_cmd(type="POST",data=elementsJsonStr, \
                               base_url=BASE_URL_WS, \
                               post_type="elements",branch="master/", \
                               project_post=True)
    print curl_cmd
    #(status,output) = commands.getstatusoutput(curl_cmd)
    
##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    
    doIt()

