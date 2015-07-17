

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

elementsJsonStrTemplate = '\'{"elements":[%s]}\''

def writeJsonStr(jStr):
    count = 0
    jsonArrayStr = ''
    for i in range(0, numberOfElements):
        iStr = "%06d"%i
        id = "e_" + iStr
        name = id + "_" + jStr
        jsonStr = '{"sysmlid":"' + id + '","name":"' + name + '"}'
        if jsonArrayStr != '':
            jsonArrayStr = jsonArrayStr + ',' + jsonStr
        else:
            jsonArrayStr = jsonStr
        count = count + 1
        
        if count % numberOfElementsToPostAtATime == 0:
            if jsonArrayStr != '':
                elementsJsonStr = elementsJsonStrTemplate%jsonArrayStr
                post(elementsJsonStr)
                jsonArrayStr = ''
    #post any remaining elements
    if jsonArrayStr != '':
        elementsJsonStr = elementsJsonStrTemplate%jsonArrayStr
        post(elementsJsonStr)
        jsonArrayStr = ''  
        
def post(elementsJsonStr):
    curl_cmd = create_curl_cmd(type="POST",data=elementsJsonStr,
                               base_url=BASE_URL_WS,
                               branch="master/elements",
                               project_post=True)
    print curl_cmd + "\n"
    #(status,output) = commands.getstatusoutput(curl_cmd)              
    
def doIt():
    writeJsonStr('0')

    for j in range(0,numberOfChangesPerElement): #3
        jStr = "%06d"%j
        writeJsonStr(jStr)
    
##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    
    doIt()

