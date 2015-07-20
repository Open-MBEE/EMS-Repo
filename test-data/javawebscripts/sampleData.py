from regression_lib import *
import commands
import optparse
import time

# default parameters
site = 'europa'
project = '123456'
folder = 'generated'

folderBranchingFactor=10  # 0 means everything in the same folder

elementsJsonStrTemplate = '\'{"elements":[%s]}\''

parser = optparse.OptionParser()

parser.add_option("-e", "--elements", default=10, type="int", help="Number of elements to post")
parser.add_option("-p", "--postElements", default=1, type="int", help="Number of elements to post at a time")
parser.add_option("-c", "--changes", default=10, type="int", help="Number of changes per element per workspace")
parser.add_option("-n", "--postChanges", default=1, type="int", help="Number of changes to post at a time")
parser.add_option("-o", "--owner", default="testData", help="An owner ID to indicate where all the elements should be gathered")
parser.add_option("-w", "--workspaces", default="master", help="A string of comma separated workspace names to be used i.e. \"workspace1,workspace2,workspace3...\" (no spaces)")
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
        
def writeJsonStr(jStr, workspaceName, postNumber):
    count = 0
    jsonArrayStr = ''
    for i in range(0, options.elements):
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
        
    writeJsonStr('0', "master", options.postElements)
    
    if options.verbose:
        thick_divider()
        print "POSTING CHANGES TO ELEMENTS IN GROUPS OF " + str(options.postChanges)
        thick_divider()
        
    for workspace in workspaces:
        for j in range(0, options.changes): #3
            jStr = "%06d"%j
            writeJsonStr(jStr, workspace, options.postChanges)
    
##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    
    doIt()

