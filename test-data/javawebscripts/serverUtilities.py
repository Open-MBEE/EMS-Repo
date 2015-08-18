from regression_test_harness import *
import json

DEBUG = 1

VIEW2VIEW_TEMP = json.dumps({"id": "", "childrenViews": []})
print '-------------------------------------------------'
print 'Printing view 2 view'
print '-------------------------------------------------'
print VIEW2VIEW_TEMP
print '-------------------------------------------------'


class UtilElement:
    def __init__(self, inElement=None, base_url=BASE_URL_WS, workspace='master'):
        self.data = []
        if (inElement is not None):
            self.jsonObj = loadJson(open(inElement))  # Function that will throw exception if it is not the correct json
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

            if (self.type == 'View'):
                self.childViews = []
                self.childrenViews = self.specialization.get('childrenViews')
                for each in self.childrenViews:
                    self.childViews.append(each)
            elif (self.type == 'Product'):
                self.childViews = []
                try:
                    self.view2view = self.specialization.get('view2view')
                    print ("Trying to get view2view")
                except self.view2view:
                    print ("ERROR!!!Failed to get view2view")
                    raise
                else:
                    print 'Got view2view'
                    index = 0
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

            self.displayed = self.specialization.get('displayedElements')
            self.contents = self.specialization.get('contents')
            self.contentType = self.contents.get('type')
            self.valueExpression = self.contents.get('valueExpression')
            self.operand = self.contents.get('operand')

        self.base_url = base_url
        self.workspace = workspace

    # ------------------------------------
    #   JSON Get Methods
    # ------------------------------------
    def getId(self):
        return self.sysmlid

    def getOwner(self):
        return self.owner

    def getType(self):
        return self.type

    def getName(self):
        return self.name

    def getDocs(self):
        return self.documentation

    def getQualifiedId(self):
        return self.qualifiedId

    def getTimeCreated(self):
        return self.created

    def getChildrenViews(self):
        if (len(self.childViews) > 0):
            return self.childViews
        else:
            if (DEBUG):
                print ('No child views')
            return []

    def getDisplayedElements(self):
        if self.displayed > 0:
            if (DEBUG):
                print "Has displayed elements"

            return self.displayed

    def getChildView(self, index=None, sysmlid=''):
        numViews = len(self.childViews)
        foundView = False

        if (numViews > 0):
            if (DEBUG):
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

    # ------------------------------------
    #   Mutator Methods
    # ------------------------------------
    def addView(self, newView):
        viewUtil = UtilElement("./JsonData/" + newView)
        tempView = []
        index = 0
        parentHasViews = (len(self.childViews) > 0)
        childHasViews = (len(viewUtil.childViews) > 0)
        childHasDisplayed = (viewUtil.displayed is not None and viewUtil.displayed is not '')
        if (DEBUG):
            if parentHasViews:
                print '-------------------------------------------------'
                print 'Parent has views'
                print '-------------------------------------------------'
                print self.childViews
                print '-------------------------------------------------'
            if childHasViews:
                print '-------------------------------------------------'
                print 'Child has views'
                print '-------------------------------------------------'
            if childHasDisplayed:
                print '-------------------------------------------------'
                print 'Has displayed element'
                print '-------------------------------------------------'
        for each in viewUtil.childViews:
            print '-------------------------------------------------'
            print "Adding: "
            print '-------------------------------------------------'
            print "newView Child View : " + str(each)
            print '-------------------------------------------------'

            tempView.append(viewUtil.childViews[index])
            index += 1
            print '-------------------------------------------------'
            print 'Child views of view utilitiy:'
            print '-------------------------------------------------'
            print viewUtil.childViews
            print '-------------------------------------------------'
            print tempView[index]
            print '-------------------------------------------------'

        index = 0
        for each in self.childViews:
            # tempView.append(self.childViews[index])
            tempView.append(each)
            print "Adding: "
            print "Parent: Child View : " + str(each)

            index += 1
        self.childViews = tempView
        return tempView[0]

    def removeOwnership(self):
        owner = self.owner
        curl_cmd = create_curl_cmd("GET")

        # curl_cmd = create_curl_cmd("GET", data=self.jsonObj, base_url=self.baseUrl)
        # utilElement = commands.getoutput(curl_cmd)

    def appendView(self, sysmlid=''):
        if sysmlid is not '':

            self.childViews.get(self.sysmlid)

    # ------------------------------------
    #   RESTful Methods
    # ------------------------------------


    #   POST Method

    def POST(self, verbose=True):
        if (self.jsonObj is not None):
            curl_cmd = create_curl_cmd(type="POST", data=self.jsonObj, base_url=self.base_url, branch=self.workspace)
        else:
            print '-------------------------------------------------'
            print('No element...Posted nothing...')
            print '-------------------------------------------------'
            return
        # no need for time delay when posting elements or changes to elements
        if (verbose):
            return execCurlCmd(curl_cmd)

    def GET(self):
        if (self.sysmlid is not ''):
            if (self.type is 'View'):
                return getViewById(self.sysmlid)
            elif (self.type is 'Product'):
                return getElementById(self.sysmlid)
            else:
                print '-------------------------------------------------'
                print('Not a view or a product')
                print '-------------------------------------------------'

    def evaluate(self):
        curl_cmd = create_curl_cmd(type="GET", data=self.element,
                                   base_url=BASE_URL_WS,
                                   branch=self.workspace)

        eval_cmd = curl_cmd + '?evalute'
        return execCurlCmd(eval_cmd)


# Utility functions to be used outside the class

# Attempts to serialize the json file

def dumpJson(jsonObject):
    try:
        dumpJsonObj = json.dump(jsonObject)
    except:
        print '-------------------------------------------------'
        print('Could not dump element...Check json file')
        print '-------------------------------------------------'
        raise
    else:
        if (DEBUG):
            print('Dumped JSON Object')
        return dumpJsonObj


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
        if (DEBUG):
            print('Loaded JSON Object')
        print '-------------------------------------------------'

        return loadJsonObj


def sizeOf(element):
    jsonObj = testElement(element)
    return len(jsonObj)


def testElement(element):
    loadJsonObj = open(element)

    try:
        jsonObject = json.load(loadJsonObj)
    except jsonObject:
        print '-------------------------------------------------'
        print('Could not load element...')
        print '-------------------------------------------------'
        print('Attempting to dump...')
        print '-------------------------------------------------'
        try:
            jsonObject = json.dump(element)

        except:
            print '-------------------------------------------------'
            print('Could not dump element...')
            print '-------------------------------------------------'
            print('Check json file')
            print '-------------------------------------------------'
            raise
        else:
            return jsonObject
    else:
        return loadJsonObj


def getElementInJson(jsonElement=None, index=0):
    if (jsonElement is not None):
        jsonAr = UtilElement(jsonElement)
        if (index < len(jsonAr.element)):
            return jsonAr.element[index]
        else:
            print '-------------------------------------------------'
            if (DEBUG):
                print("Index: " + str(index)+ "is out of bounds!")
            print '-------------------------------------------------'
            if (DEBUG):
                print("There are: " + str(len(jsonAr.element)))
            print '-------------------------------------------------'


def evalElement(jsonFile, url=HOST, workspace='master/'):
    curl_cmd = create_curl_cmd(type="GET", data=jsonFile,
                               base_url=url,
                               branch=workspace)
    eval_cmd = curl_cmd + '?evalute'
    execCurlCmd(eval_cmd)


def addViewToView(toAdd, addingTo):
    if (toAdd is None or addingTo is None):
        if (DEBUG):
            print("Elements don't exists")
        return
    addThisView = UtilElement(toAdd)
    toThisView = UtilElement(addingTo)
    for index in range(0, len(toThisView.childViews)):

        tmpView = VIEW2VIEW_TEMP
        print '-------------------------------------------------'
        print 'Create Empty temp view 2 view'
        print '-------------------------------------------------'
        tmpView = json.loads(tmpView)
        print '-------------------------------------------------'
        print tmpView
        print '-------------------------------------------------'
        tmpView.update(id=addThisView.sysmlid, childrenViews=addThisView.childrenViews)
        json.dumps(tmpView)
        print 'Update temp view 2 view'
        print '-------------------------------------------------'
        print tmpView
        print '-------------------------------------------------'
        print 'Dumping toThisView.childViews'
        print '-------------------------------------------------'
        print json.dumps(toThisView.childViews)
        print '-------------------------------------------------'
        for i in range(0,len(toThisView.childViews)):
            print 'Printing childviews in toThisView'
            print '-------------------------------------------------'
            print 'At index : ' + str(i)
            print '-------------------------------------------------'
            print toThisView.childViews[i]
            print '-------------------------------------------------'
        toThisView.childViews.append(tmpView)

        if(toThisView.childViews[index].get('id')== toThisView.sysmlid):
            print 'Adding ID to parent\'s children views arrary'
            toThisView.childViews[index].get('childrenViews').append(tmpView.get('id'))
    if(DEBUG):
        print 'Printing toThisView'
        print '-------------------------------------------------'
        index = 0
        for views in toThisView.childViews:
            print 'Printing child Views in parentView'
            print '-------------------------------------------------'
            print views
            print '-------------------------------------------------'
    toThisView.POST()

def getElementById(sysmlid=''):
    getUrl = 'http://localhost:8080/alfresco/service/workspaces/master/elements/' + str(sysmlid)
    get_cmd = 'curl -w "%{http_code}\n" -X GET -u admin:admin '
    send_cmd = get_cmd + getUrl
    element = execCurlCmd(send_cmd)
    return element


def getViewById(sysmlid=''):
    getUrl = '\"http://localhost:8080/alfresco/service/rest/views/' + str(sysmlid) + '\"'
    get_cmd = 'curl -w "%{http_code}\n" -X GET -u admin:admin '
    send_cmd = get_cmd + getUrl
    view = execCurlCmd(send_cmd)
    return view


def execCurlCmd(cmd):
    (status, output) = commands.getstatusoutput(cmd)
    return status, output


def testCreateUtilObject():
    utilObject = UtilElement("./JsonData/testProductElement.json")
    print utilObject
