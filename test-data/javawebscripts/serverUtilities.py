from regression_test_harness import *
import commands
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
                self.jsonObj = loadJson(open(inElement))  # Function that will throw exception if it is not the
                # correct json
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
            printObj = json.dumps(self.jsonObj, sort_keys=True, indent=4, separators=(',', ': '))
            print printObj
            curl_cmd = create_curl_cmd(type="POST", data=postObject)
            # curl_cmd = regression_lib.create_curl_cmd(type="POST", data=postObject)
            print '-------------------------------------------------'
            print 'Curl Command From POST Method'
            print '-------------------------------------------------'
            print str(curl_cmd)
            print '-------------------------------------------------'
        else:
            print('No element...Posted nothing...')
            print '-------------------------------------------------'
            return

        if (verbose):
            print 'Posting element:'
            print '-------------------------------------------------'
            print str(self.jsonObj)
            print '-------------------------------------------------'
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
    obj = get_json_output_no_status(execCurlCmd(curl_cmd)).replace("n", "")
    print str(obj)
    elementsIndex = str(obj).rfind('{"elements', 0)
    returnObj = json.loads(obj[elementsIndex:])  # .get('elements')

    return returnObj


def addViewToView(toAdd, addingTo):
    print '========================'
    print 'Add View'
    if type(addingTo) is not str:
        print '========================'
        print 'Is not string'
        print '========================'
        postElement = UtilElement(addingTo)
        ownerSysmlid = postElement.sysmlid
    else:
        print '========================'
        print 'Is String'
        print '========================'
        ownerSysmlid = addingTo
    newView = TEMPLATE_NEW_VIEW
    newView.elements[0].update('sysmlid', ownerSysmlid)

    newView = json.dumps(newView)
    cmd = create_curl_cmd("POST", data=newView)
    status, output = execCurlCmd(cmd)

    response = json.loads(output)
    childSysmlid = response.elements[0].get('sysmlid')

    postView = json.loads(TEMPLATE_INSTANCE_SPEC)
    instanceString = postView.elements[0].specialization.instanceSpecificationSpecification.get('string')
    instanceString.replace('VIEW_ID', str(childSysmlid))
    postView.elements[0].specialization.instanceSpecificationSpecification.update('string', instanceString)

    postView = json.dumps(postView)

    cmd = create_curl_cmd("POST", data=postView)
    status, output = execCurlCmd(cmd)


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


def execCurlCmd(cmd):
    (status, output) = commands.getstatusoutput(cmd)
    return output


def testPOST():
    test = testProduct()
    POSTjson(test)


def testAddViewToView():
    addViewToView("./JsonData/testViewElement.json", "./JsonData/testProductElement.json")


def testAddViewToViewById():
    addViewToView("./JsonData/testViewElement.json", "MMS_1439772862289_3ee431bd-b685-437e-929a-83aa335b79be")


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
    print ''
    print '-------------------------------------------------'
    print ''
    print status
    print ''
    print '-------------------------------------------------'
    print ''
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

# def old_addViewToView(toAdd, addingTo=None,sysmlid=''):
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
