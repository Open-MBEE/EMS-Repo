from regression_test_harness import *
import json
import sys
'''
Python's JSON module usage, tips and tricks
 json.dumps(jsonObject) will take jsonObject and create a python object
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
'''

def dumpJson(testView):
    try:
        dumpJsonObj = json.dumps(testView)
    except:
        print('Could not dump element...\n\nCheck json file')
        raise
    else:
        print('Dumped JSON Object')
        return dumpJsonObj

def loadJson(testView):
    try:
        loadJsonObj = json.loads(testView)
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
        loadJsonObj = json.loads(element)
    except loadJsonObj:
        print('Could not load element...\n\nAttempting to dump...\n\n')
        try:
            dumpJsonObj = json.dumps(element)
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


class utilElement:
    def __init__(self, inElement=None, base_url='', workspace='master'):
        self.data = []
        if (inElement is not None):
            # try:
            #     self.jsonObj = json.loads(inElement)
            # except self.jsonObj:
            #     print('Could not load element...\n\n')
            #
            #     raise

            self.element = self.jsonObj

            self.sysmlid = self.element.symlid
            self.owner = self.element.owner
            self.type = self.element.specialization.type
            self.name = self.element.name
            self.read = self.element.read
            self.documentation = self.element.documentation
            self.created = self.element.created
            self.qualifiedId = self.element.qualifiedId
            if (self.type == 'View'):
                self.childViews = self.element.childrenViews
            elif (self.type == 'Product'):
                self.childViews = self.element.specialization.view2view.childrenViews
        self.baseUrl = base_url
        self.workspace = workspace

    def removeOwnership(self):
        curl_cmd = create_curl_cmd("GET", CURL_FLAGS)

        # curl_cmd = create_curl_cmd("GET", data=self.jsonObj, base_url=self.baseUrl)
        # utilElement = commands.getoutput(curl_cmd)


    def post(self, entireJsonData=None, workspaceName='master/', post_type='elements', project_post=False,
             verbose=True):
        if(entireJsonData is not None):
            curl_cmd = create_curl_cmd(type="POST", data=self.element, base_url=BASE_URL_WS, branch=workspaceName)
        else:
            print('No element...\nPosted nothing...\n\n')
            return
        # no need for time delay when posting elements or changes to elements
        if (verbose):
            execCurlCmd(curl_cmd)

    def evaluate(self):
        curl_cmd = create_curl_cmd(type="GET", data=self.element,
                                   base_url=BASE_URL_WS,
                                   branch=self.workspace)

        eval_cmd = curl_cmd + '?evalute'
        execCurlCmd(eval_cmd)


# Utility functions to be used outside the class
def evalElement(jsonFile, url=HOST, workspace='master/'):
    url = 'https://'+ url + '.jpl'
    curl_cmd = create_curl_cmd(type="GET", data=jsonFile,
                               base_url=url,
                               branch=workspace )
    eval_cmd = curl_cmd + '?evalute'
    execCurlCmd(eval_cmd)

def addViewToView(toAdd, addingTo):
    if (toAdd is None or addingTo is None):
        print("Views don't exists")
        return

    temp = json.loads(toAdd)
    child = temp[0]
    temp = json.loads(addingTo)
    parent = temp[0]

    childType = child.specialization.type
    parentType = parent.specialization.type

    if (parentType == 'Product'):
        for index in parent.specialization.view2view:
            if (parent.specialization.view2view.id == parent.sysmlid):
                if (childType == 'Product'):
                    parent.specialization.view2view[index].childrenViews.append(child.sysmlid)

                parent.specialization.view2view.append()

def getElementById(sysmlid=''):
    (status,output) = commands.getstatusoutput('curl -w "%{http_code}\n" -X GET -u admin:admin http://localhost:8080/alfresco/service/workspaces/master/elements/sysmlid')
    print('Status:\n'+ status + '\n')
    print('Output:\n'+ output + '\n')
    return output

def execCurlCmd(cmd):
    (status, output)= commands.getstatusoutput(cmd)
    print('Status: ' + status + '\n' + "Output: " + output)