from regression_lib import *
import commands
import optparse
import time
#from Finder.Containers_and_folders import folder

# default parameters
site = 'europa'
project = '123456'
folder = 'generated'
DELAY_TIME = 5


elementsJsonStrTemplate = '\'{"elements":[%s]}\''

parser = optparse.OptionParser()

#debug purposes
parser.add_option("-r", "--prefix", default="e", help="prefix")

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

def verboseExecuteOptions(curl_cmd, timeDelay=False):
    if options.verbose:
        print curl_cmd + "\n"
    
    if options.execute:
        (status, output) = commands.getstatusoutput(curl_cmd)
        if options.verbose:
            print output
        if timeDelay:
            time.sleep(DELAY_TIME)

def createWorkspaces():
    if options.verbose:
        print "\n" + "CREATING WORKSPACES\n"
        
    for workspace in workspaces:
        #master already exists
        if workspace == "master":
            continue
        workspaceName = workspace + "?sourceWorkspace=master&copyTime="+get_current_time(delay=0)
        curl_cmd = create_curl_cmd(type="POST",base_url=BASE_URL_WS,
                                   post_type="",branch=workspaceName)
        
        #make sure the workspace is actually created
        verboseExecuteOptions(curl_cmd, False)
        
def post(entireJsonData, workspaceName):
    curl_cmd = create_curl_cmd(type="POST",data=entireJsonData,
                               base_url=BASE_URL_WS,
                               branch= workspaceName + "/elements",
                               project_post=True)
    #no need for time delay when posting elements or changes to elements
    verboseExecuteOptions(curl_cmd, False)
        
def writeJsonStr(branch, changesToElement, workspaceName, count, postNumber, listOfJsonStrToPost):
    
    #stores the parent node number in order to set the owner
    if branch == 0:
        parent = 0
    else:
        parent = branch
        
    #creates starting number for the child node
    child = branch * options.folderBranching + 1
    
    if options.folderBranching == 0:
        #stores everything under options.owner since there is no branching factor
        for i in range(1, options.elements + 1):
            idNumbers = "%06d"%i
            id = options.prefix + "_" + idNumbers #"e_"
            name = id + "_" + changesToElement

            jsonStr = '{"sysmlid":"' + id + '","name":"' + name + '","owner":"' + options.owner + '"}'
            #creates the list of elements to post if it doesn't exist, otherwise adds to the existing list
            if listOfJsonStrToPost != '':
                listOfJsonStrToPost = listOfJsonStrToPost + ',' + jsonStr
            else:
                listOfJsonStrToPost = jsonStr
            count = count + 1
            #once it reaches the number of elements to post, creates the json data and posts it
            if count % postNumber == 0:
                if listOfJsonStrToPost != '':
                    entireJsonData = elementsJsonStrTemplate%listOfJsonStrToPost
                    post(entireJsonData, workspaceName)
                    listOfJsonStrToPost = ''
        #post any remaining elements
        if listOfJsonStrToPost != '':
            entireJsonData = elementsJsonStrTemplate%listOfJsonStrToPost
            post(entireJsonData, workspaceName)
            listOfJsonStrToPost = '' 
    
    else:
        #pre-order traversal
        for i in range(child, child + options.folderBranching):
            if i > (options.elements): #total amount of branches
                break
            
            idNumbers = "%06d"%i
            id = options.prefix + "_" + idNumbers #"e_"
            name = id + "_" + changesToElement
            
            if parent == 0:
                owner = options.owner
            else:
                owner = options.prefix + "_" + "%06d"%parent #"e_"
            
            jsonStr = '{"sysmlid":"' + id + '","name":"' + name + '","owner":"' + owner + '"}'
            #creates the list of elements to post if it doesn't exist, otherwise adds to the existing list
            if listOfJsonStrToPost != '':
                listOfJsonStrToPost = listOfJsonStrToPost + ',' + jsonStr
            else:
                listOfJsonStrToPost = jsonStr
            count = count + 1
            #once it reaches the number of elements to post, creates the json data and posts it
            if count % postNumber == 0:
                if listOfJsonStrToPost != '':
                    entireJsonData = elementsJsonStrTemplate%listOfJsonStrToPost
                    post(entireJsonData, workspaceName)
                    listOfJsonStrToPost = ''
            #passes on the existing list of elements and count so next iteration can keep track of previous ones     
            listOfJsonStrToPost, count = writeJsonStr(i, changesToElement, workspaceName, count, postNumber, listOfJsonStrToPost)

    return listOfJsonStrToPost, count
                   
     #################################################################       

def doIt():
    
    curl_cmd = create_curl_cmd(type="POST",data='\'{"elements":[{"sysmlid":"123456","name":"JW_TEST","specialization":{"type":"Project"}}]}\'',
                base_url=BASE_URL_WS,
                branch="master/sites/europa/projects?createSite=true",project_post=True)
    verboseExecuteOptions(curl_cmd, False)
    
    createWorkspaces()

    if options.verbose:
        print "\n" + "CREATING OWNER IN EACH WORKSPACE\n"
        
    for workspace in workspaces:
        jsonStr = '{"sysmlid":"' + options.owner + '","name":"' + options.owner + '","owner":"123456"}'
        entireJsonData = elementsJsonStrTemplate%jsonStr
        curl_cmd = create_curl_cmd(type="POST", 
                                   data=entireJsonData,
                                   base_url=BASE_URL_WS,
                                   branch= workspace + "/elements",
                                   project_post=True)
        
        verboseExecuteOptions(curl_cmd, False)
    
    if options.verbose:
        thick_divider()
        print "POSTING ELEMENTS IN GROUPS OF " + str(options.postElements)
        thick_divider()
    
    #no changes to any elements to parameter is set as constant '0'
    #only post initial elements to master
    listOfJsonStrToPost, count = writeJsonStr(0,'0', "master", 0, options.postElements, '')
    
    #post any remaining elements
    if listOfJsonStrToPost != '':
        entireJsonData = elementsJsonStrTemplate%listOfJsonStrToPost
        post(entireJsonData, "master")
        listOfJsonStrToPost = ''
    
    if options.verbose:
        thick_divider()
        print "POSTING CHANGES TO ELEMENTS IN GROUPS OF " + str(options.postChanges)
        thick_divider()
        
    for workspace in workspaces:
        for j in range(0, options.changes): 
            changesToElement = "%06d"%j
            listOfJsonStrToPost, count = writeJsonStr(0, changesToElement, workspace, 0, options.postChanges, '')
            
            #post any remaining elements
            if listOfJsonStrToPost != '':
                entireJsonData = elementsJsonStrTemplate%listOfJsonStrToPost
                post(entireJsonData, workspace)
                listOfJsonStrToPost = '' 
    
##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    
    doIt()
    
    
    