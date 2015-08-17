from regression_test_harness import *
import json
'''
Python's JSON module usage, tips and tricks
 json.dump(jsonObject) will take jsonObject and create a python object
 Differences of importing json objects
 If importing as a string, attributes and values of the json objects require to have double quotations ""
 >>> testElement = importTestViewJson('[{'name':'example'},{'name':'ex2'}]')
   File "<input>", line 1
     testElement = importTestViewJson('[{'name':'example'},{'name':'ex2'}]')                                            ^
 SyntaxError: invalid syntax

 >>> testElement = importTestViewJson('[{"name":"example"},{"name":"ex2"}]')
 >>> testElement
 '"[{\\"name\\":\\"example\\"},{\\"name\\":\\"ex2\\"}]"'

 If importing as a json object
 json.dumps('jsonObj') returns a python string object
 This serializes the object that is passed in
 >>> testElement = json.dumps([{'name':'example'},{'name':'ex2'}])

 >>> testElement
 '[{"name": "example"}, {"name": "ex2"}]'
 >>> testElement[1]
 '{'
 >>> testElement
 '[{"name": "example"}, {"name": "ex2"}]'
 >>> testElement[2]
 '"'

 testElement is a str obj
 >>> testElement[1].has_key('name')
 Traceback (most recent call last):
   File "<input>", line 1, in <module>
 AttributeError: 'str' object has no attribute 'has_key'

 Call json.loads to deserialize json object
 Returns an array of objects
 >>> elemAr = json.loads(testElement)

 >>> elemAr[1]
 {u'name': u'ex2'}
 >>> elemAr[0]
 {u'name': u'example'}
 >>> elemAr[0].get('name')
 u'example'

 Cases to take care of :
 If parent is a documentation
 if parent is a view
 If child already has a parent
	Remove view from its parent
	Set qualified ID
	Set owner
*************
	EXAMPLE COMMAND AND RESULT
*************
	python -c 'from serverUtilities import *;jsonObjFile = open("./JsonData/testProductElement.json");jsonObj = json.load(jsonObjFile);print jsonObj.get("elements"); utilObj = UtilElement("./JsonData/testProductElement.json");print utilObj.getId(); viewObj = UtilElement("./JsonData/testViewElement.json");print viewObj;utilObj.addView("./testViewElement.json")'
[{u'qualifiedName': u'/server-utils/server-utils_no_project/holding_bin/Test Doc 1', u'name': u'Test Doc 1', u'created': u'Sun Aug 16 18:26:50 PDT 2015', u'sysmlid': u'MMS_1439774810522_0ba947a4-ac47-4d7d-b17b-e0c692243e88', u'documentation': u'This element is on ems-stg under Server Utilities', u'editable': True, u'modified': u'2015-08-16T19:09:11.973-0700', u'read': u'2015-08-16T19:09:12.373-0700', u'siteCharacterizationId': u'server-utils', u'owner': u'holding_bin_server-utils_no_project', u'qualifiedId': u'/server-utils/server-utils_no_project/holding_bin_server-utils_no_project/MMS_1439774810522_0ba947a4-ac47-4d7d-b17b-e0c692243e88', u'modifier': u'example', u'creator': u'dank', u'specialization': {u'type': u'Product', u'displayedElements': [u'MMS_1439774810522_0ba947a4-ac47-4d7d-b17b-e0c692243e88'], u'allowedElements': [u'MMS_1439774810522_0ba947a4-ac47-4d7d-b17b-e0c692243e88'], u'view2view': [{u'childrenViews': [], u'id': u'MMS_1439774810522_0ba947a4-ac47-4d7d-b17b-e0c692243e88'}], u'contents': {u'operand': [{u'instance': u'MMS_1439774811887_0ecc5284-0e8a-4263-a0f1-51ba51af6ad4', u'valueExpression': None, u'type': u'InstanceValue'}], u'valueExpression': None, u'type': u'Expression'}}}]
Loaded JSON Object
None
Loaded JSON Object
<serverUtilities.UtilElement instance at 0x1018df830>
Loaded JSON Object



'''


class UtilElement:
    def __init__(self, inElement=None, base_url=BASE_URL_WS, workspace='master'):
        self.data = []
        if (inElement is not None):
            self.jsonObj = loadJson(open(inElement))  # Function that will throw exception if it is not the correct json
            self.element = self.jsonObj.get('elements')[0]
            self.sysmlid = self.element.get('symlid')
            self.owner = self.element.get('owner')
            self.specialization = self.element.get('specialization')
            self.type = self.specialization.get('type')
            self.name = self.element.get('name')
            self.read = self.element.get('read')
            self.documentation = self.element.get('documentation')
            self.created = self.element.get('created')
            get = self.element.get('qualifiedId')
            self.qualifiedId = get
            if (self.type == 'View'):
                self.childViews = []
                self.childViews = self.specialization.get('childrenViews')

            elif (self.type == 'Product'):
                self.view2views = self.specialization.get('view2view')
                index = 0
                self.childViews = []
                for each in self.view2views:
                    addChild = self.view2views[index].get('childrenViews')
                    self.childViews.append(addChild)
                    index = index + 1

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
    def getChildViews(self):
        return self.childViews

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
    def addView(self,newView):
        newViewUtil = UtilElement("./JsonData/"+newView)
        tempView = []
        index = 0
        for each in newViewUtil.childViews:
            tempView.append(newViewUtil.childViews[index])
            index = index + 1
        index = 0
        for each in self.childViews:
            tempView.append(self.childViews[index])
            index = index + 1
        self.childViews = tempView

    # ------------------------------------
    #   RESTful Methods
    # ------------------------------------


    #   POST Method

    def POST(self, verbose=True):
        if (self.jsonObj is not None):
            curl_cmd = create_curl_cmd(type="POST", data=self.jsonObj, base_url=self.base_url, branch=self.workspace)
        else:
            print('No element...\nPosted nothing...\n\n')
            return
        # no need for time delay when posting elements or changes to elements
        if (verbose):
            return execCurlCmd(curl_cmd)

    def GET(self):
        if(self.sysmlid is not ''):
            if(self.type is 'View'):
                return getViewById(self.sysmlid)
            elif(self.type is 'Product'):
                return getElementById(self.sysmlid)
            else:
                print('Not a view or a product')


    def evaluate(self):
        curl_cmd = create_curl_cmd(type="GET", data=self.element,
                                   base_url=BASE_URL_WS,
                                   branch=self.workspace)

        eval_cmd = curl_cmd + '?evalute'
        return execCurlCmd(eval_cmd)


    def removeOwnership(self):
        owner = self.owner
        curl_cmd = create_curl_cmd("GET")

    # curl_cmd = create_curl_cmd("GET", data=self.jsonObj, base_url=self.baseUrl)
    # utilElement = commands.getoutput(curl_cmd)

# Utility functions to be used outside the class

# Attempts to serialize the json file
def dumpJson(jsonObject):
    try:
        dumpJsonObj = json.dump(jsonObject)
    except:
        print('Could not dump element...\n\nCheck json file')
        raise
    else:
        print('Dumped JSON Object')
        return dumpJsonObj

def loadJson(jsonObject):
    try:
        loadJsonObj = json.load(jsonObject)
    except:
        print('Could not load element...\n\nCheck json file')
        raise
    else:
        print('Loaded JSON Object')
        return loadJsonObj

def sizeOf(element):
    jsonObj = testElement(element)
    return len(jsonObj)

def testElement(element):
    loadJsonObj = "{[{}]}"
    try:
        loadJsonObj = json.load(open(element))
    except loadJsonObj:
        print('Could not load element...\n\nAttempting to dump...\n\n')
        try:
            dumpJsonObj = json.dump(element)
        except:
            print('Could not dump element...\n\nCheck json file')
            raise
        else:
            return dumpJsonObj
    else:
        return loadJsonObj


def getElementInJson(jsonElement=None, index=0):
    if(jsonElement is not None):
        jsonAr = loadJson(jsonElement)
        if(index < len(jsonAr)):
            return jsonAr[index]
        else:
            print('\n\nIndex: ' + index + ' is out of bounds!\n\nThere are: ' + len(jsonAr))



def evalElement(jsonFile, url=HOST, workspace='master/'):
    curl_cmd = create_curl_cmd(type="GET", data=jsonFile,
                               base_url=url,
                               branch=workspace )
    eval_cmd = curl_cmd + '?evalute'
    execCurlCmd(eval_cmd)

def addViewToView(toAdd, addingTo):
    if (toAdd is None or addingTo is None):
        print("Views don't exists")
        return
    # Testing basic function first then implementing class
    # child = UtilElement(toAdd)
    # parent = UtilElement(addingTo)

    child = UtilElement("./JsonData/"+ toAdd)
    parent = UtilElement("./JsonData/" + addingTo)

    # temp = loadJson(toAdd)
    # child = temp[0]
    # temp = loadJson(addingTo)
    # parent = temp[0]



    if (parent.getStrType() == 'Product'):
        index = 0
        for each in parent:

            if (parent.specialization.view2view[index].id == parent.sysmlid):
                if (child.getStrtype() == 'Product'):
                    tempView = parent.specialization.view2view[0]
                    tempView.set
                    parent.specialization.view2view[index].childrenViews.append(child.sysmlid)

                parent.specialization.view2view.append()
            index = index + 1



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
    (status, output)= commands.getstatusoutput(cmd)
    return status, output

def testCreateUtilObject():
    utilObject = UtilElement("./JsonData/testProductElement.json")
    print utilObject
    print utilObject