from regression_lib import *
import commands
import optparse
import time
from Finder.Containers_and_folders import folder

# default parameters
site = 'europa'
project = '123456'
folder = 'generated'


elementsJsonStrTemplate = '\'{"elements":[%s]}\''

parser = optparse.OptionParser()

parser.add_option("-e", "--elements", default=10, type="int", help="Number of elements to post")
parser.add_option("-p", "--postElements", default=1, type="int", help="Number of elements to post at a time")
parser.add_option("-c", "--changes", default=10, type="int", help="Number of changes per element per workspace")
parser.add_option("-n", "--postChanges", default=1, type="int", help="Number of changes to post at a time")
parser.add_option("-o", "--owner", default="testData", help="An owner ID to indicate where all the elements should be gathered")
parser.add_option("-w", "--workspaces", default="master", help="A string of comma separated workspace names to be used i.e. \"workspace1,workspace2,workspace3...\" (no spaces)")
parser.add_option("-f", "--folderBranching", default=0, type="int", help="Number of branching folders to create. 0 means everything in the same folder")
parser.add_option("-x", "--execute", dest="execute", action="store_true", default=False, help="Execute the commands to create workspaces and post the elements")
parser.add_option("-v", "--verbose", dest="verbose", action="store_true", default=False, help="Print out the curl commands and, if executing, the output from sending the curl commands")
options, args = parser.parse_args()

workspaces = options.workspaces.split(",")

def createWorkspaces():
    if options.verbose:
        print "\n" + "CREATING WORKSPACES\n"
        
    for workspace in workspaces:
        workspaceName = workspace + "?sourceWorkspace=master&copyTime="+get_current_time(delay=0)
        curl_cmd = create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                                   post_type="",branch=workspaceName)
        
        if options.verbose:
            print curl_cmd
            
        if options.execute:
            (status, output) = commands.getstatusoutput(curl_cmd)
        
def post(elementsJsonStr, workspaceName):
    curl_cmd = create_curl_cmd(type="POST",data=elementsJsonStr,
                               base_url=BASE_URL_WS,
                               branch= workspaceName + "/elements",
                               project_post=True)
    if options.verbose:
        print curl_cmd + "\n"
    if options.execute:
        (status, output) = commands.getstatusoutput(curl_cmd)
        if options.verbose:
            print output
        
def writeJsonStr(branch, jStr, workspaceName, count, postNumber, jsonArrayStr):
    
    if branch == 0:
        folder = 0
    else:
        folder = branch
        
    branch = branch * options.folderBranching + 1
    
    if options.folderBranching == 0:
        
        for i in range(1, options.elements + 1):
            iStr = "%06d"%i
            id = "e_" + iStr
            name = id + "_" + jStr

            jsonStr = '{"sysmlid":"' + id + '","name":"' + name + '","owner":"' + options.owner + '"}'
            if jsonArrayStr != '':
                jsonArrayStr = jsonArrayStr + ',' + jsonStr
            else:
                jsonArrayStr = jsonStr
            count = count + 1
        
            if count % postNumber == 0:
                if jsonArrayStr != '':
                    elementsJsonStr = elementsJsonStrTemplate%jsonArrayStr
                    post(elementsJsonStr, workspaceName)
                    jsonArrayStr = ''
    #post any remaining elements
        if jsonArrayStr != '':
            elementsJsonStr = elementsJsonStrTemplate%jsonArrayStr
            post(elementsJsonStr, workspaceName)
            jsonArrayStr = '' 
    
    else:
        
        for i in range(branch, branch + options.folderBranching):
            if i > (options.elements): #amount of folders, change to amount of elements to get all the elements
                break
            
            iStr = "%06d"%i
            id = "e_" + iStr
            name = id + "_" + jStr
            
            if folder == 0:
                owner = "testData"
            else:
                owner = "e_" + "%06d"%folder
            
            jsonStr = '{"sysmlid":"' + id + '","name":"' + name + '","owner":"' + owner + '"}'
            if jsonArrayStr != '':
                jsonArrayStr = jsonArrayStr + ',' + jsonStr
            else:
                jsonArrayStr = jsonStr
            count = count + 1
        
            if count % postNumber == 0:
                if jsonArrayStr != '':
                    elementsJsonStr = elementsJsonStrTemplate%jsonArrayStr
                    post(elementsJsonStr, workspaceName)
                    jsonArrayStr = ''
                    
            jsonArrayStr, count = writeJsonStr(i, jStr, workspaceName, count, postNumber, jsonArrayStr)

    return jsonArrayStr, count
                   
     #################################################################       

def doIt():
    createWorkspaces()
    time.sleep(10)

    if options.verbose:
        print "\n" + "CREATING OWNER\n"
        
    for workspace in workspaces:
        jsonStr = '{"sysmlid":"' + options.owner + '","name":"' + options.owner + '"}'
        dataStr = elementsJsonStrTemplate%jsonStr
        curl_cmd = create_curl_cmd(type="POST", 
                                   data=dataStr,
                                   base_url=BASE_URL_WS,
                                   branch= workspace + "/elements",
                                   project_post=True)
        
        if options.verbose:
            print curl_cmd
        if options.execute:
            (status, output) = commands.getstatusoutput(curl_cmd)
            time.sleep(10)
    
    if options.verbose:
        thick_divider()
        print "POSTING ELEMENTS IN GROUPS OF " + str(options.postElements)
        thick_divider()
        
    jsonArrayStr, count = writeJsonStr(0,'0', "master", 0, options.postElements, '')
    
    #post any remaining elements
    if jsonArrayStr != '':
        elementsJsonStr = elementsJsonStrTemplate%jsonArrayStr
        post(elementsJsonStr, "master")
        jsonArrayStr = ''
    
    if options.verbose:
        thick_divider()
        print "POSTING CHANGES TO ELEMENTS IN GROUPS OF " + str(options.postChanges)
        thick_divider()
        
    for workspace in workspaces:
        for j in range(0, options.changes): #3
            jStr = "%06d"%j
            jsonArrayStr, count = writeJsonStr(0, jStr, workspace, 0, options.postChanges, '')
            
            #post any remaining elements
            if jsonArrayStr != '':
                elementsJsonStr = elementsJsonStrTemplate%jsonArrayStr
                post(elementsJsonStr, workspace)
                jsonArrayStr = '' 
    
##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    
    doIt()

