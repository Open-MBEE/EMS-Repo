import regression_lib
import commands
from elementTemplates import *
import utilityElement

DEBUG = True




# Utility functions to be used outside the class
def POSTjson(element, verbose=True):
    if (element.jsonObj is not None):
        curl_cmd = regression_lib.create_curl_cmd(type="POST", data=str(element.jsonObj))
        printDebug('Curl Command Posting by ELEMENT From POST Function',data=curl_cmd)
        postByID = False
    elif element is not '':
        postObject = json.dumps(element.jsonObj)
        postObject = json.loads(postObject)
        curl_cmd = regression_lib.create_curl_cmd(type="POST", data="elements/" + str(postObject), branch='master/')
        printDebug('Curl Command Posting by SYSMLID From POST Function',data=curl_cmd)
        postByID = True
    else:
        printDebug('No element...Posted nothing...')

        return

    if verbose and DEBUG is True:
        out = 'Posting element '
        if postByID:
            out += 'by sysmlid'
        else:
            out += 'By element'
        printDebug(statement=out)

    return execCurlCmd(curl_cmd)


def GETjson(sysmlid):
    curl_cmd = regression_lib.create_curl_cmd('GET', data="elements/" + str(sysmlid), branch='master/', post_type='elements/')
    print curl_cmd
    obj = regression_lib.get_json_output_no_status(execCurlCmd(curl_cmd)).replace("\n", "")
    print str(obj)
    elementsIndex = str(obj).rfind('{"elements', 0)
    returnObj = json.loads(obj[elementsIndex:])  # .get('elements')

    return returnObj


#  This process Requires 4 POSTs to complete adding a view to another view
def addViewToView(toAdd, addingTo):
    # TODO: Test if JSON file, if sysmlid, or name
    
    printDebug('addViewToView(' + str(toAdd) + ", " + str(addingTo) + ')');
    
    postCount = 1
    toAdd_element = utilityElement.UtilElement(toAdd)
    if toAdd_element.isEmptyElement:
        toAdd_element.name = toAdd
        toAdd_element.type = 'View'

    # Determine what type view the child view will be added to
    #   This is for retrieve the sysmlid needed to create the new viewpoint
    if type(addingTo) is not str:
        postElement = utilityElement.UtilElement(addingTo)
        ownerSysmlid = postElement.sysmlid
    else:
        ownerSysmlid = addingTo

    # Loads a template of a new view elenent and updates its data members with the new element's values
    newView = json.loads(TEMPLATE_NEW_VIEW)
    newView.get('elements')[0].update(owner=ownerSysmlid)
    newView.get('elements')[0].update(documentation=toAdd_element.documentation)
    newView.get('elements')[0].update(name=toAdd_element.name)
    newView.get('elements')[0].get('specialization').update(type=toAdd_element.type)

    # Serializes the newView json object, appends and prepends \' character for the server to be able to read json
    #   object as a string
    postView = json.dumps(newView, separators=(',', ':'))
    printDebug('Testing post view of creating view by name',data=postView)
    postStr = '\'' + str(postView) + '\''

    # Creates the curl command that will post to the server to retrieve information to create the new view
    # This post is used to notify the server that a new view will is to be created.
    # Executes the curl command and returns the response from the server and increments postCount
    cmd = regression_lib.create_curl_cmd("POST", data=postStr, branch='master/' + 'elements', project_post=True)
    printDebug(str(cmd))
    responseJson = regression_lib.get_json_output_no_status(execCurlCmd(cmd, postCount)).replace("\n", "")
    postCount += 1

    printDebug('Checking first response json', data=responseJson)

    #   Then retrieves and stores the sysmlid and the siteCharacterizationId
    responseJson = removeOutputHeader(responseJson)
    responseId = responseJson.get('elements')[0].get('sysmlid')
    responseSiteCharId = responseJson.get('elements')[0].get('siteCharacterizationId')

    # Loads a template view element to the post view object
    postView = json.loads(TEMPLATE_INSTANCE_SPEC)
    # Updates the source of the template view element with the sysmlid from the response element when first posting
    #   to the server
    postView.get('elements')[0].get('specialization').get('instanceSpecificationSpecification').get('string').update(
        source=responseId)

    # Updates the owner of the new view element to post with the site characterization id and appends the
    # suffix '_no_project'
    postView.get('elements')[0].update(owner=responseSiteCharId + '_no_project')

    # Takes the string attribute and converts it to a proper string format with escape characters for the double quotes
    stringify = postView.get('elements')[0].get('specialization').get('instanceSpecificationSpecification').get(
        'string')

    # Calling json.dumps on stringify will automatically add the \ character in where ever it is needed to escape any
    #  special cha
    stringify = json.dumps(stringify)
    postView = json.dumps(postView, separators=(',', ':'))

    postView = json.loads(postView)
    postView.get('elements')[0].get('specialization').get('instanceSpecificationSpecification').update(
        string=stringify)

    postView = json.dumps(postView)
    postStr = '\'' + str(postView) + '\''
    cmd = regression_lib.create_curl_cmd("POST", data=postStr, branch='master/' + 'elements', project_post=True)
    responseJson = regression_lib.get_json_output_no_status(execCurlCmd(cmd, postCount)).replace("\n", "")

    responseJson = removeOutputHeader(responseJson)
    instanceSysmlid = responseJson.get('elements')[0].get('sysmlid')
    postCount += 1

    postView = json.loads(TEMPLATE_NEW_VIEW_WITH_INSTANCE_SPEC)

    postView.get('elements')[0].update(sysmlid=responseId)
    postView.get('elements')[0].get('specialization').get('allowedElements').append(responseId)
    postView.get('elements')[0].get('specialization').get('displayedElements').append(responseId)
    if toAdd_element.isEmptyElement is False:
        postView.get('elements')[0].get('specialization').update(childrenViews=toAdd_element.childrenViews)
    postView.get('elements')[0].get('specialization').get('contents').get('operand')[0].update(instance=instanceSysmlid)
    postView = json.dumps(postView)

    postStr = '\'' + str(postView) + '\''
    cmd = regression_lib.create_curl_cmd("POST", data=postStr, branch='master/' + 'elements', project_post=True)
    responseJson = regression_lib.get_json_output_no_status(execCurlCmd(cmd, postCount)).replace("\n", "")

    responseJson = removeOutputHeader(responseJson)
    postCount += 1

    ###################################################################################################################
    # POST #4 - GET CALL to retrieve the parent element
    ###################################################################################################################
    postParent = json.loads(TEMPLATE_POST_PARENT_PRODUCT)
    postParent.get('elements')[0].update(sysmlid=ownerSysmlid)
    curl_cmd = regression_lib.create_curl_cmd('GET', data="elements/" + str(ownerSysmlid), branch='master/', post_type='elements/')
    parentElement = regression_lib.get_json_output_no_status(execCurlCmd(curl_cmd)).replace("\n", "")

    elementsIndex = str(parentElement).rfind('{"elements', 0)
    parentElement = json.loads(parentElement[elementsIndex:])
    parentType = parentElement.get('elements')[0].get('specialization').get('type')
    parentAllowedElements = parentElement.get('elements')[0].get('specialization').get('allowedElements')
    parentDisplayedElements = parentElement.get('elements')[0].get('specialization').get('displayedElements')
    postParent.get('elements')[0].get('specialization').update(allowedElements=parentAllowedElements)
    postParent.get('elements')[0].get('specialization').update(displayedElements=parentDisplayedElements)
    postParent.get('elements')[0].get('specialization').update(type=parentType)

    viewArray = parentElement.get('elements')[0].get('specialization').get('view2view')
    arIndex = 1
    if viewArray is not None:
        for i in viewArray:
            arIndex += 1
        postParent.get('elements')[0].get('specialization').update(view2view=viewArray)
        limit = arIndex
        view2viewTemp = json.loads(TEMPLATE_VIEW2VIEW_NEW_VIEW)
        view2viewTemp = view2viewTemp.get('view2view')[0]
        view2viewTemp.update(id=responseJson.get('elements')[0].get('sysmlid'))
        view2viewTemp.update(childrenViews=responseJson.get('elements')[0].get('specialization').get('childrenViews'))

        for index in range(0, limit - 1):
            if (postParent.get('elements')[0].get('specialization').get('view2view')[index].get('id') ==
                    postParent.get('elements')[0].get('sysmlid')):
                postParent.get('elements')[0].get('specialization').get('view2view')[index].get('childrenViews').append(
                    responseJson.get('elements')[0].get('sysmlid'))
                postParent.get('elements')[0].get('specialization').get('view2view').append(view2viewTemp)
    else:
        # TODO: Add case for when the parent view is not a product

        print ''
        print '----===================----'
        print ''
        print 'Not a product'
    # TODO: Add case for when the parent is neither product nor a view

    postView = json.dumps(postParent)
    postStr = '\'' + str(postView) + '\''
    cmd = regression_lib.create_curl_cmd("POST", data=postStr, branch='master/' + 'elements', project_post=True)
    regression_lib.get_json_output_no_status(execCurlCmd(cmd, postCount)).replace("\n", "")


# Attempts to serialize the json file
def dumpJson(jsonObject):
    try:
        dumpJsonObj = json.dump(jsonObject, sort_keys=True, indent=4, separators=(',', ': '))
    except:
        print '-------------------------------------------------'
        print('Could not dump element...Check json file')
        print '-------------------------------------------------'
        raise
    else:
        if DEBUG is True:
            print('Dumped JSON Object')
        return dumpJsonObj

# Checks if the object sent in is a json string
def is_json(myjson):
    try:
        json_object = json.loads(myjson)
    except ValueError, e:
        return False
    return True

# Attempts to deserialize the json file
def loadJson(jsonObject):
    try:
        loadJsonObj = json.load(jsonObject)
    except:
        print '-------------------------------------------------'
        print('Could not load element...Check json file')
        print '-------------------------------------------------'
        raise
    else:
        print '-------------------------------------------------'
        if DEBUG is True:
            print('Loaded JSON Object')
        print '-------------------------------------------------'

        return loadJsonObj

def printDebug(statement=None, data=None,index=None, lineNum=None):
    starLine =     '\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n'
    print starLine + '                                   Debug Statement' + starLine
    lineOut = ''
    if statement is not None:
        lineOut = statement
    if lineNum is not None:
        lineOut = str(lineOut) + '\nAt line number : ' +str(lineNum) + str('\n')
    if index is not None:
        lineOut = str(lineOut) + '--===--\n' + 'Index is at : ' + str(index) + str('\n')
    if data is not None:
        if index is not None or lineNum is not None:
            lineOut += lineOut + '--===--\n'

        lineOut = str(lineOut)  + '\nData output: ' + str(data) + str('\n')

    print '---==============================---\n'
    print lineOut
    print '\n---==============================---\n'

def execCurlCmd(cmd, postCount=None):
    (status, output) = commands.getstatusoutput(cmd)
    data = regression_lib.get_json_output_no_status(output=output)
    if DEBUG is True:
        if postCount is not None:
            printDebug('Post Count : ' + str(postCount))
        printDebug('EXECUTING CURL COMMAND\nPRINTING STATUS', data=status)
        printDebug(statement='Get JSON Output No Status',data=data)
        printDebug(statement='Printing output', data=output)
    return output


########################################################################################################################
# removeOutputHeader
# _____________________________________________________________________________________________________________________
# Takes a json object and removes the \ characters. (Replaces \ with '') then searches for the pattern '{elements' to
#   find the index of where it starts. This will be used to slice the string. Once string has been sliced it
#   deserializes the string into a python object and returns it.
########################################################################################################################
def removeOutputHeader(dirtyJson):
    # if is_json(dirtyJson) is False:
    #     return

    diretyJson = str(dirtyJson).replace('\\', "")
    elementsIndex = dirtyJson.rfind('{\"elements', 0)
    if elementsIndex != -1:
        cleanJson = json.loads(dirtyJson[elementsIndex:])
    return cleanJson


def testPOST():
    test = testProduct()
    POSTjson(test)


def testAddViewToView():
    addViewToView("./JsonData/testViewElement.json", "./JsonData/testProductElement.json")

def addViewToViewByName(name='', sysmlid=''):
    if name is not '' and sysmlid is not '':
        addViewToView(str(name), str(sysmlid))
    else:
        print 'ID is -- ' + sysmlid + ' and name of new element is ' + name + str('\n')

def addViewToViewByJSON(json=None, sysmlid=''):
    if json is not None and sysmlid is not '':
        addViewToView(str(json), str(sysmlid))
    else:
        print 'ID is -- ' + sysmlid + ' and name of new element is ' + str(json) + str('\n')

def testAddViewToViewByName(name='',sysmlid=''):
    if name is not '' and id is not '':
        addViewToView(str(name), str(id))
    else:
        print 'ID is -- ' + sysmlid + ' and name of new element is ' + name + str('\n')

# This test is to add a view to the view on localhost called 1.1 Create Sub View 1.1
def testAddViewToViewById(test=1,sysmlid=''):
    if sysmlid is not '':
        addViewToView("./JsonData/testViewElement.json",str(sysmlid))
    elif test == 1:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440004666224_40c9834a-d6d0-47ff-bfe3-54d25e0462d0")
    elif test == 2:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440004700246_e9451e3f-f060-4af0-a452-9cef660b3fa4")
    elif test == 3:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440438813696_b91bb630-008d-4d7e-bc93-b7865ff07375")
    elif test == 4:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440004748603_65014c15-980d-4131-a524-957d68091aa0")
    elif test == 301:
        addViewToView("./JsonData/testViewElement.json", "301")
    elif test == 401:
        addViewToView("./JsonData/testViewElement.json", "401")
#

def testProduct():
    jsonObj = utilityElement.UtilElement("./JsonData/testProductElement.json")
    print '-------------------------------------------------'
    print 'Test Product Function'
    print '-------------------------------------------------'
    print ''
    print str(jsonObj.element)
    print ''
    print '-------------------------------------------------'
    return jsonObj


def testView():
    jsonObj = utilityElement.UtilElement("./JsonData/testViewElement.json")
    print str(jsonObj.element)
    return jsonObj


def getViewById(sysmlid=''):
    getUrl = '"http://localhost:8080/alfresco/service/rest/views/' + str(sysmlid) + '"'
    get_cmd = 'curl -w "%{http_code}n" -X GET -u admin:admin '
    send_cmd = get_cmd + getUrl
    status, view = execCurlCmd(send_cmd)
    return view


def testGetViewById():
    test = testProduct()
    print ''
    print '-------------------------------------------------'
    print ''
    print 'Testing testProductBy ID'
    print ''
    print '-------------------------------------------------'
    print str(test.jsonObj)
    print ''
    print '-------------------------------------------------'
    print ''
    print str(getViewById(test.sysmlid))
    print ''
    print '-------------------------------------------------'
    print ''
    return getViewById(test.sysmlid)


def runTests():
    print 'Testing ===== testProduct()'
    product = testProduct()
    print 'Testing ===== testView()'
    view = testView()
    print 'Testing ===== testGetViewById()'
    testGetViewById()

    print 'Testing ===== product.POST()'
    product.POST()
    print 'Testing ===== product.GET()'
    product.GET()
    print 'Testing ===== view.POST()'
    view.POST()
    print 'Testing ===== view.GET()'
    view.GET()

    print 'Testing ===== testAddViewToView()'
    testAddViewToView()
