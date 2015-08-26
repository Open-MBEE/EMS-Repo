from regression_test_harness import *
from elementTemplates import *

DEBUG = True

print '-------------------------------------------------'
print 'Printing Temp view 2 view'
print '-------------------------------------------------'
json.dumps({"id": "", "childrenViews": []}, sort_keys=True, indent=4, separators=(',', ': '))
print '-------------------------------------------------'

class UtilElement:
    def __init__(self, inElement=None):
        self.data = []
        if (inElement is not None):

            if (str(inElement).endswith('.json')):
                print 'Constructor - inElement Open and Loads'
                print inElement
                self.jsonObj = loadJson(open(inElement))  # Function that will throw exception if it is not the
            else:
                self.jsonObj = inElement
                print self.jsonObj
            self.element = self.jsonObj.get('elements')[0]
            self.sysmlid = self.element.get('sysmlid')
            self.owner = self.element.get('owner')
            self.specialization = self.element.get('specialization')
            self.type = self.specialization.get('type')
            self.name = self.element.get('name')
            self.read = self.element.get('read')
            self.documentation = self.element.get('documentation')
            self.created = self.element.get('created')
            self.qualifiedId = self.element.get('qualifiedId')
            self.isProduct = False
            self.isView = False
            if (self.type == 'View'):
                self.isView = True
                self.childViews = []
                self.childrenViews = self.specialization.get('childrenViews')
                for each in self.childrenViews:
                    self.childViews.append(each)
            elif (self.type == 'Product'):
                self.isProduct = True
                self.childViews = []
                try:
                    if (self.specialization.get('view2view')):
                        self.view2view = self.specialization.get('view2view')
                    else:
                        self.view2view = {}

                    print ("Trying to get view2view")
                except self.view2view:
                    print ("ERROR!!!Failed to get view2view")
                    raise
                else:
                    print 'Got view2view'
                    index = 0
                    if (self.specialization.get('view2view')):

                        for each in self.view2view:
                            print '-------------------------------------------------'
                            print 'View at At Index: ' + str(index)
                            print '-------------------------------------------------'
                            print each
                            print '-------------------------------------------------'
                            self.childViews.append(each)
                            print '-------------------------------------------------'
                            print 'Print View2View Child Views'
                            print '-------------------------------------------------'
                            print self.childViews[index]
                            print '-------------------------------------------------'
                            index += 1
                        self.childrenViews = self.childViews

            self.displayed = self.specialization.get('displayedElements')

            temp = self.specialization.get('contents')
            if (temp is not None):
                self.contents = self.specialization.get('contents')
                temp = self.specialization.get('type')
                if (temp is not None):
                    self.contentType = self.contents.get('type')
            temp = self.specialization.get('valueExpression')
            if (temp is not None):
                self.valueExpression = self.contents.get('valueExpression')
            temp = self.specialization.get('operand')
            if (temp is not None):
                self.operand = self.contents.get('operand')

    # ------------------------------------
    #   JSON Get Methods
    # ------------------------------------
    # TODO: Needs Testing!
    def getId(self):
        return self.sysmlid

    # TODO: Needs Testing!
    def getOwner(self):
        return self.owner

    # TODO: Needs Testing!
    def getType(self):
        return self.type

    # TODO: Needs Testing!
    def getName(self):
        return self.name

    # TODO: Needs Testing!
    def getDocs(self):
        return self.documentation

    # TODO: Needs Testing!
    def getQualifiedId(self):
        return self.qualifiedId

    # TODO: Needs Testing!
    def getTimeCreated(self):
        return self.created

    # TODO: Needs Testing!
    def getChildrenViews(self):
        if (len(self.childViews) > 0):
            return self.childViews
        else:
            if DEBUG is True:
                print ('No child views')
            return []

    # TODO: Needs Testing!
    def getDisplayedElements(self):
        if self.displayed > 0:
            if DEBUG is True:
                print "Has displayed elements"

            return self.displayed

    # TODO: Needs Testing!
    def getChildView(self, index=None, sysmlid=''):
        numViews = len(self.childViews)
        foundView = False

        if (numViews > 0):
            if DEBUG is True:
                print "Has Child View"
            if index >= 0 and index < numViews:
                foundView = self.childViews[index]
            if sysmlid is not '':
                for view in self.childViews:
                    if str(view.getStrId) is str(sysmlid):
                        foundView = view
            return foundView

    # ------------------------------------
    #   String Get Methods
    # ------------------------------------
    def getStrId(self):
        return str(self.sysmlid)

    def getStrOwner(self):
        return str(self.owner)

    def getStrType(self):
        return str(self.type)

    def getStrName(self):
        return str(self.name)

    def getStrDocs(self):
        return str(self.documentation)

    def getStrQualifiedId(self):
        return str(self.qualifiedId)

    def getStrTimeCreated(self):
        return str(self.created)

    def getStrChildViews(self):
        return str(self.childViews)

    # TODO: REMOVE OWNER FROM ELEMENT
    # def removeOwnership(self):


    # ------------------------------------
    #   RESTful Methods
    # ------------------------------------
    #   POST Method

    # TODO: Element Calls post
    def POST(self, verbose=True):
        if (self.jsonObj is not None):
            postObject = json.dumps(self.jsonObj)
            print "Dumping object..."
            curl_cmd = create_curl_cmd(type="POST", data=postObject)
            # curl_cmd = regression_lib.create_curl_cmd(type="POST", data=postObject)
        else:
            return

        if (verbose):
            print 'Posting element:'
            return execCurlCmd(curl_cmd)

    # TODO: Element calls post, of itself or of the sysmlid that is passed in
    def GET(self, sysmlid='', verbose=True, asJson=False):
        if (sysmlid is not ''):
            print '-------------------------------------------------'
            # curl_cmd = regression_lib.create_curl_cmd(type="GET",data=sysmlid,branch="master/",post_type="elements/")
            curl_cmd = create_curl_cmd(type="GET", data=sysmlid, branch="master/", post_type="elements/")
            printObj = json.dumps(self.element, sort_keys=True, indent=4, separators=(',', ': '))
            print printObj
        elif self.jsonObj is not None:
            # curl_cmd = regression_lib.create_curl_cmd(type="GET", data=self.sysmlid,branch = "master/", post_type ="elements/")
            curl_cmd = create_curl_cmd(type="GET", data=self.sysmlid, branch="master/", post_type="elements/")
            if verbose:
                print 'Curl Command'
                print '-------------------------------------------------'
                print curl_cmd
                print '-------------------------------------------------'
                printObj = json.dumps(self.jsonObj, sort_keys=True, indent=4, separators=(',', ': '))
                print '-------------------------------------------------'
                print 'ELIF printObject GET METHOD'
                print printObj
                print '-------------------------------------------------'
        else:
            print ('No element...Posted nothing...')
            print '-------------------------------------------------'
            return
        # no need for time delay when posting elements or changes to elements
        if verbose:
            print '-------------------------------------------------'
            print 'Getting element'
            print '-------------------------------------------------'
        getObj = execCurlCmd(curl_cmd)

        print '-------------------------------------------------'
        printObj = json.dumps(getObj, sort_keys=True, indent=4, separators=(',', ': '))
        print 'Before RETURN printObject GET'
        print '-------------------------------------------------'
        print printObj
        print '-------------------------------------------------'
        if asJson:
            return json.loads(getObj)
        else:
            return getObj

    def evaluate(self):
        # curl_cmd = regression_lib.create_curl_cmd(type="GET", data=self.element)
        curl_cmd = create_curl_cmd(type="GET", data=self.element)
        eval_cmd = curl_cmd + '?evalute'
        printObj = json.dumps(self.element, sort_keys=True, indent=4, separators=(',', ': '))
        print printObj
        return execCurlCmd(eval_cmd)


# Utility functions to be used outside the class
def POSTjson(element, verbose=True):
    print str(element)
    print '-------------------------------------------------'
    print 'Printing object within post json function'
    print '-------------------------------------------------'
    # print json.dumps(element.jsonObj, sort_keys=True, indent=4, separators=(',', ': '))
    # printObj = json.loads(element.jsonObj.get('element'))
    # printObj = json.dumps(printObj, sort_keys=True, indent=4, separators=(',', ': '))
    print '-------------------------------------------------'
    # print str(printObj)
    print '-------------------------------------------------'
    print 'ENDING POSTjson print'
    print '-------------------------------------------------'

    # if (element is '' and element.jsonObj is not None):
    if (element.jsonObj is not None):
        # postObject = json.dumps(element.jsonObj)
        print str(element.jsonObj)
        # postObject = json.dumps(element.jsonObj)
        print "Dumping object..."
        # curl_cmd = regression_lib.create_curl_cmd(type="POST", data=postObject)
        curl_cmd = create_curl_cmd(type="POST", data=str(element.jsonObj))
        print '-------------------------------------------------'
        print 'Curl Command Posting by ELEMENT From POST Function'
        print '-------------------------------------------------'
        print str(curl_cmd)
        print '-------------------------------------------------'
        postByID = False
    elif element is not '':
        # postObject = str(json.dumps(element))
        postObject = json.dumps(element.jsonObj)
        # curl_cmd = regression_lib.create_curl_cmd(type="POST", data=postObject)
        postObject = json.loads(postObject)
        curl_cmd = create_curl_cmd(type="POST", data="elements/" + str(postObject), branch='master/')
        print '-------------------------------------------------'
        print 'Curl Command Posting by SYSMLID From POST Function'
        print '-------------------------------------------------'
        print str(curl_cmd)
        print '-------------------------------------------------'
        postByID = True
    else:
        print('No element...Posted nothing...')
        print '-------------------------------------------------'
        return

    if (verbose):
        print 'Posting element:'
        print '-------------------------------------------------'
        if postByID:
            print 'By sysmlid:'
            print str(element.jsonObj)
        else:
            print 'By element'
            print str(element.jsonObj)
        print '-------------------------------------------------'
    return execCurlCmd(curl_cmd)


def GETjson(sysmlid):
    curl_cmd = create_curl_cmd('GET', data="elements/" + str(sysmlid), branch='master/', post_type='elements/')
    print curl_cmd
    obj = get_json_output_no_status(execCurlCmd(curl_cmd)).replace("\n", "")
    print str(obj)
    elementsIndex = str(obj).rfind('{"elements', 0)
    returnObj = json.loads(obj[elementsIndex:])  # .get('elements')

    return returnObj

#  This process Requires 4 POSTs to complete adding a view to another view
def addViewToView(toAdd, addingTo):
    postCount = 1
    toAdd_element = UtilElement(toAdd)

    ###################################################################################################################
    # Determine what type view the child view will be added to
    #   This is for retrieve the sysmlid needed to create the new viewpoint
    ###################################################################################################################
    if type(addingTo) is not str:
        print '========================'
        print ''
        print 'Is not string'
        print ''
        print '========================'
        print ''
        postElement = UtilElement(addingTo)
        ownerSysmlid = postElement.sysmlid
    else:
        print ''
        print '========================'
        print ''
        print 'Is String'
        print ''
        print '========================'
        print ''
        ownerSysmlid = addingTo
    ###################################################################################################################
    # Loads a template of the view
    ###################################################################################################################
    newView = json.loads(TEMPLATE_NEW_VIEW)
    newView.get('elements')[0].update(owner=ownerSysmlid)
    newView.get('elements')[0].update(documentation=toAdd_element.documentation)
    newView.get('elements')[0].update(name=toAdd_element.name)
    newView.get('elements')[0].get('specialization').update(type=toAdd_element.type)

    postView = json.dumps(newView, separators=(',', ':'))
    ###################################################################################################################
    # Post new view to get instance specification
    ###################################################################################################################
    print ''
    print '---==============================---'
    print ' - Line 361 - '
    postStr = '\'' + str(postView) + '\''
    print 'Post String'
    print ''
    print postStr
    print ''
    print '---==============================---'
    print ''
    ###################################################################################################################
    # Creates the curl command that will post to the server to retrieve information to create the new view
    # This post is used to notify the server that a new view will is to be created.
    ###################################################################################################################
    cmd = create_curl_cmd("POST", data=postStr,branch='master/' + 'elements',project_post=True)
    print ' CURL COMMAND '
    print cmd
    print ''
    print '---==============================---'
    print '- Line 377 -'
    print '     output      post 1    '
    print ''
    print '---==============================---'
    print ''
    ###########################################################################################
    # Executes the curl command and returns the response from the server
    ###########################################################################################
    responseJson = get_json_output_no_status(execCurlCmd(cmd,postCount)).replace("\n", "")
    postCount += 1
    print 'PRINTING RESPONSE JSON'
    print ''
    ###################################################################################################################
    # Prints the response json and removes the \ characters. (Replaces \ with '')
    ###################################################################################################################
    print str(responseJson).replace('\\', "")
    print ''
    print '---==============================---'
    print ''
    print '- Line 390 - Prints the json with the characters removed from the string'
    print str(responseJson)
    print ''
    print '---==============================---'
    ###################################################################################################################
    # Searches for the pattern {elements and finds the index where it starts. This will be used to slice the string
    #   then create a python JSON object.
    ###################################################################################################################
    elementsIndex = str(responseJson).rfind('{\"elements', 0)
    print ''
    print ' - Line 398'
    print 'printing elements index '
    print elementsIndex
    print ''
    print '---==============================---'
    print ''
    print ''
    ###################################################################################################################
    # This will load the json string from the index where it found the pattern as a python object.
    ###################################################################################################################
    responseJson = json.loads(responseJson[elementsIndex:])

    print '---==============================---'
    print ' - Line 426 -'
    print ''
    print ' Print Return Object after loads from ' + str(elementsIndex) + ' to the end'
    print ''
    print responseJson
    print ''
    print '---==============================---'
    print ''
    responseId = responseJson.get('elements')[0].get('sysmlid')
    responseSiteCharId = responseJson.get('elements')[0].get('siteCharacterizationId')
    print '- line 436 - print owner ID'
    print ''
    print responseId
    print ''
    print '---==============================---'
    print ''
    ###################################################################################################################
    # Loads a template view element to the post view object
    ###################################################################################################################
    postView = json.loads(TEMPLATE_INSTANCE_SPEC)

    ###################################################################################################################
    # Updates the source of the template view element with the sysmlid from the response element when first posting
    #   to the server
    ###################################################################################################################
    postView.get('elements')[0].get('specialization').get('instanceSpecificationSpecification').get('string').update(source=responseId)

    ###################################################################################################################
    # Updates the owner of the new view element to post with the site characterization id and appends the
    # suffix '_no_project'
    ###################################################################################################################
    postView.get('elements')[0].update(owner=responseSiteCharId + '_no_project')

    ###################################################################################################################
    # Takes the string attribute and converts it to a proper string format with escape characters for the double quotes
    ###################################################################################################################
    stringify = postView.get('elements')[0].get('specialization').get('instanceSpecificationSpecification').get('string')

    ###################################################################################################################
    # Calling json.dumps on stringify will automatically add the \ character in where ever it is needed to escape any
    #  special cha
    ###################################################################################################################
    stringify = json.dumps(stringify)

    print '---==============================---'
    print ' - Line 471 - '
    print 'Print dumped - stringify before adding \\'
    print ''
    print stringify
    print ''
    print '---==============================---'
    print ''
    postView = json.dumps(postView, separators=(',', ':'))
    print '---==============================---'
    print ' - Line 480 - '
    print ''
    print 'Printing stringified string from the postView element with added \\ '
    print ''
    print stringify
    print ''
    print '---==============================---'
    print ''
    print 'Updating the postView object with the formatted string data member'
    postView = json.loads(postView)
    postView.get('elements')[0].get('specialization').get('instanceSpecificationSpecification').update(
        string=stringify)
    print ''
    print postView
    print ''
    postView = json.dumps(postView)
    print '---==============================---'
    print ' - Line 497 - '
    print ''
    postStr = '\'' + str(postView) + '\''
    print 'Print Post View after updating source to new owner sysmlid'
    print ''
    print postView
    print ''
    print '---==============================---'
    print ''
    cmd = create_curl_cmd("POST", data=postStr, branch='master/' + 'elements', project_post=True)
    print ''
    print '---==============================---'
    print ' - Line 509 -'
    print ''
    print ' CURL COMMAND '
    print ''
    print '---==============================---'
    print ''
    print cmd
    print ''
    print '---==============================---'
    print ' - Line 518 -'
    print ''
    print '     output      post 2     '
    print ''
    print '- CALL EXEC CURL -'
    print ''
    print '---==============================---'
    responseJson = get_json_output_no_status(execCurlCmd(cmd, postCount)).replace("\n", "")
    print ' - Line 457 - '
    print ''
    print ' Printing response json after the secondary post of the instance specification'
    print ''
    print '---==============================---'
    print ''
    print responseJson
    print ''
    print '---==============================---'
    print ''
    str(responseJson).replace('\\', "")
    elementsIndex = str(responseJson).rfind('{\"elements', 0)
    responseJson = json.loads(responseJson[elementsIndex:])
    instanceSysmlid = responseJson.get('elements')[0].get('sysmlid')
    postCount += 1
    print''
    print '---==============================---'
    print ''
    print 'Elements Index : ' + str(elementsIndex)
    print ''
    print 'Print Response JSON'
    print responseJson
    print ''
    print '---==============================---'
    postView = json.loads(TEMPLATE_NEW_VIEW_WITH_INSTANCE_SPEC)
    print 'printing post view'
    print ''
    print str(postView)
    print ''
    print '---==============================---'
    postView.get('elements')[0].update(sysmlid=responseId)
    postView.get('elements')[0].get('specialization').get('allowedElements').append(responseId)
    postView.get('elements')[0].get('specialization').get('displayedElements').append(responseId)
    postView.get('elements')[0].get('specialization').get('contents').get('operand')[0].update(instance=instanceSysmlid)
    postView = json.dumps(postView)
    print '---==============================---'
    print ' - Line 561 - '
    print ''
    postStr = '\'' + str(postView) + '\''
    print ''
    print 'Print Post String'
    print ''
    print postStr
    print ''
    print '---==============================---'
    cmd = create_curl_cmd("POST", data=postStr, branch='master/' + 'elements', project_post=True)
    print ''
    print '---==============================---'
    print ' - Line 573 -'
    print ''
    print ' CURL COMMAND '
    print ''
    print '---==============================---'
    print ''
    print cmd
    print ''
    print '---==============================---'
    print ' - Line 582 -'
    print ''
    print '     output      post 3     '
    print ''
    print '- CALL EXEC CURL -'
    print ''
    print '---==============================---'
    responseJson = get_json_output_no_status(execCurlCmd(cmd, postCount)).replace("\n", "")
    print ' - Line 590 - '
    print ''
    print ' Printing response json after the secondary post of the instance specification'
    print ''
    print '---==============================---'
    print ''
    print responseJson
    print ''
    print '---==============================---'
    print ''
    str(responseJson).replace('\\', "")
    elementsIndex = str(responseJson).rfind('{\"elements', 0)
    responseJson = json.loads(responseJson[elementsIndex:])
    postCount += 1
    print ''
    print 'Print Response JSON'
    print responseJson
    print ''
    print '---==============================---'
    print ''
    ###################################################################################################################
    # GET CALL
    ###################################################################################################################
    postParent = json.loads(TEMPLATE_POST_PARENT_PRODUCT)
    postParent.get('elements')[0].update(sysmlid=ownerSysmlid)
    curl_cmd = create_curl_cmd('GET', data="elements/" + str(ownerSysmlid), branch='master/', post_type='elements/')
    ###################################################################################################################
    # GET CALL
    ###################################################################################################################
    print ''
    print '---==============================---'
    print 'Print Curl Command'
    print ''
    print curl_cmd
    print ''
    print '---==============================---'
    parentElement = get_json_output_no_status(execCurlCmd(curl_cmd)).replace("\n", "")
    print str(parentElement)
    elementsIndex = str(parentElement).rfind('{"elements', 0)
    parentElement = json.loads(parentElement[elementsIndex:])
    parentType = parentElement.get('elements')[0].get('specialization').get('type')
    parentAllowedElements = parentElement.get('elements')[0].get('specialization').get('allowedElements')
    parentDisplayedElements = parentElement.get('elements')[0].get('specialization').get('displayedElements')
    postParent.get('elements')[0].get('specialization').update(allowedElements=parentAllowedElements)
    postParent.get('elements')[0].get('specialization').update(displayedElements=parentDisplayedElements)
    postParent.get('elements')[0].get('specialization').update(type=parentType)
    ###################################################################################################################
    # Print parent Element
    ###################################################################################################################
    print ''
    print '---==============================---'
    print ''
    print str(parentElement)
    print ''
    print '---==============================---'

    view2view_AR = parentElement.get('elements')[0].get('specialization').get('view2view')
    arIndex = 1
    for i in view2view_AR:
        print ''
        print 'Array Index at ' + str(arIndex)
        print ''
        print i
        print ''
        print '---==============================---'
        arIndex+=1

    postParent.get('elements')[0].get('specialization').update(view2view=view2view_AR)
    print '---==============================---'
    print ''
    print 'Printing post parent with new view2view elements'
    print ''
    print str(postParent)
    print ''
    print '---==============================---'
    print ''
    limit = arIndex
    view2viewTemp = json.loads(TEMPLATE_VIEW2VIEW_NEW_VIEW)
    view2viewTemp = view2viewTemp.get('view2view')[0]
    view2viewTemp.update(id=responseJson.get('elements')[0].get('sysmlid'))
    view2viewTemp.update(childrenViews=responseJson.get('elements')[0].get('specialization').get('childrenViews'))
    print ''
    print '---==============================---'
    print ''
    print 'Print view 2 view template'
    print ''
    print view2viewTemp
    print ''
    print '---==============================---'
    print ''


    for index in range(0, limit-1):
        if (postParent.get('elements')[0].get('specialization').get('view2view')[index].get('id') ==
                postParent.get('elements')[0].get('sysmlid')):
            print ''
            print 'Adding ID to parent\'s children views array'
            print ''
            postParent.get('elements')[0].get('specialization').get('view2view')[index].get('childrenViews').append(
                responseJson.get('elements')[0].get('sysmlid'))
            print ''
            print '---==============================---'
            print ''
            print str(postParent.get('elements')[0].get('specialization').get('view2view')[index].get('childrenViews'))
            print ''
            print '---==============================---'
            print ''
            postParent.get('elements')[0].get('specialization').get('view2view').append(view2viewTemp)

    print ''
    print '---==============================---'
    print ''
    print 'Print post parent'
    print str(postParent)
    print ''
    print '---==============================---'
    print ''
    print ''
    print '---==============================---'
    print ''
    print 'Print dump post parent element'
    print str(json.dumps(postParent, sort_keys=True, indent=4, separators=(',', ': ')))
    print ''
    print '---==============================---'
    print ''


    postView = json.dumps(postParent)
    print '---==============================---'
    print ' - Line 561 - '
    print ''
    postStr = '\'' + str(postView) + '\''
    print ''
    print 'Print Post String'
    print ''
    print postStr
    print ''
    print '---==============================---'
    cmd = create_curl_cmd("POST", data=postStr, branch='master/' + 'elements', project_post=True)
    print ''
    print '---==============================---'
    print ' - Line 573 -'
    print ''
    print ' CURL COMMAND '
    print ''
    print '---==============================---'
    print ''
    print cmd
    print ''
    print '---==============================---'
    print ' - Line 582 -'
    print ''
    print '     output      post 4     '
    print ''
    print '- CALL EXEC CURL -'
    print ''
    print '---==============================---'
    responseJson = get_json_output_no_status(execCurlCmd(cmd, postCount)).replace("\n", "")
    print ' - Line 590 - '
    print ''
    print ' Printing response json after the secondary post of the instance specification'
    print ''
    print '---==============================---'
    print ''
    print responseJson
    print ''
    print '---==============================---'
    print ''
    postCount += 1









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


def execCurlCmd(cmd, postCount=1):
    (status, output) = commands.getstatusoutput(cmd)
    print '---==============================---'
    print 'Post Count : '
    print ''
    print postCount
    print ''
    print '---==============================---'
    print ' - Line 507 -'
    print ''
    print 'EXECUTING CURL COMMAND'
    print ''
    print '---==============================---'
    print ' - Line 512 - '
    print ''
    print 'PRINTING STATUS'
    print status
    print ''
    print '---==============================---'
    print ' - Line 518 - '
    print ''
    print 'print no output'
    print get_json_output_no_status(output=output)
    print ''
    print '---==============================---'
    print ' - Line 524 - '
    print ''
    print 'PRINTING OUTPUT'
    print output
    print ''
    print '---==============================---'
    print ' - Line 530 - '
    print ''
    print 'returning output'
    print ''
    print '---==============================---'
    print ''
    return output


def testPOST():
    test = testProduct()
    POSTjson(test)


def testAddViewToView():
    addViewToView("./JsonData/testViewElement.json", "./JsonData/testProductElement.json")

# This test is to add a view to the view on localhost called 1.1 Create Sub View 1.1
def testAddViewToViewById(test=1):
    if test == 1:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440004666224_40c9834a-d6d0-47ff-bfe3-54d25e0462d0")
    elif test == 2:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440004700246_e9451e3f-f060-4af0-a452-9cef660b3fa4")
    elif test == 3:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440004708656_2f851026-ff97-4500-b4eb-ef28176dbc11")
    elif test == 4:
        addViewToView("./JsonData/testViewElement.json", "MMS_1440004748603_65014c15-980d-4131-a524-957d68091aa0")
    elif test == 301:
        addViewToView("./JsonData/testViewElement.json", "301")
    elif test==401:
        addViewToView("./JsonData/testViewElement.json", "401")

def testProduct():
    jsonObj = UtilElement("./JsonData/testProductElement.json")
    print '-------------------------------------------------'
    print 'Test Product Function'
    print '-------------------------------------------------'
    print ''
    print str(jsonObj.element)
    print ''
    print '-------------------------------------------------'
    return jsonObj


def testView():
    jsonObj = UtilElement("./JsonData/testViewElement.json")
    print str(jsonObj.element)
    return jsonObj


def getViewById(sysmlid=''):
    getUrl = '"http://localhost:8080/alfresco/service/rest/views/' + str(sysmlid) + '"'
    get_cmd = 'curl -w "%{http_code}n" -X GET -u admin:admin '
    send_cmd = get_cmd + getUrl
    status,view = execCurlCmd(send_cmd)
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

'''# def old_addViewToView(toAdd, addingTo=None,sysmlid=''):
#     # Checks to see if both elements exists
#     if (toAdd is None or sysmlid=='' and addingTo is None):
#         if DEBUG is True:
#             print("Elements don't exists")
#         return
#
#
#     # Creates the Get command to get the view to add to
#     # cmd = regression_lib.create_curl_cmd()
#
#     addThisView = UtilElement(toAdd)
#     instance = '{"elements":[{"name":"ViewDocumentation","specialization":{"type":"InstanceSpecification","classifier":["_17_0_5_1_407019f_1431903758416_800749_12055"], "instanceSpecificationSpecification":{"string": "{"type":"Paragraph","sourceType":"reference","source":"MMS_1439777405160_b49a88d3-0580-4930-abd1-98e00a7b98dc","sourceProperty":"documentation"}", "type":"LiteralString"}}, "owner":"server-utils_no_project"}]}'
#     tmpInstance = json.dumps(instance)
#     print tmpInstance
#     instance = json.loads(tmpInstance)
#
#     response = POSTjson(json.dumps(postInstance))
#
#     print 'Printing response'
#     print '----------------------------'
#     print response
#
#     response.update(addThisView)
#     print str(response)
#     if sysmlid=='' and addingTo is not None:
#         toThisView = UtilElement(addingTo)
#         limit = len(toThisView.childViews)
#         tmpView = VIEW2VIEW_TEMP
#         tmpView = json.loads(tmpView)
#         tmpView.update(id=addThisView.sysmlid, childrenViews=addThisView.childrenViews)
#         for index in range(0, limit):  # 0, limit):
#             if (toThisView.childViews[index].get('id') == toThisView.sysmlid):
#                 print 'Adding ID to parent's children views array'
#                 print str(toThisView.specialization.get('view2view')[index].get('childrenViews'))
#                 toThisView.specialization.get('view2view')[index].get('childrenViews').tmpView.get('id')
#
#     else:
#         toThisView = UtilElement(GETjson(sysmlid))
#         print toThisView
#         limit = 1
#         tmpView = VIEW2VIEW_TEMP
#         tmpView = json.loads(tmpView)
#
#         tmpView.update(id=addThisView.sysmlid, childrenViews=addThisView.childrenViews)
#         tmpView = json.dumps(tmpView)
#         print '---- tmp view ----'
#         print str(tmpView)
#
#         print str(toThisView.specialization)
#         if( (toThisView.specialization.has_key('view2view'))==False):
#             tmpView = json.loads(tmpView)
#             toThisView.specialization.setdefault('view2view',[tmpView])
#
#             tmpView = VIEW2VIEW_TEMP
#             tmpView = json.loads(tmpView)
#             tmpView.update(id=toThisView.sysmlid, childrenViews=[addThisView.sysmlid])
#             toThisView.specialization.get('view2view').append(tmpView)
#             print str(toThisView.specialization)
#             print str(toThisView.specialization.get('view2view'))
#         else:
#             tmpView = VIEW2VIEW_TEMP
#             tmpView = json.loads(tmpView)
#             toThisView.specialization.view2view.setdefault(tmpView)
#
#
#     # return POSTjson(toThisView)
#     return POSTjson()
'''