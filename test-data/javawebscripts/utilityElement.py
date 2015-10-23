__author__ = 'dank'
DEBUG = 1

import serverUtilities
import regression_test_harness
import json
import elementTemplates

class UtilElement:
    def __init__(self, inElement=None):
        self.data = []
        if inElement is not None:
            self.isElement = False
            self.isEmptyElement = False
            self.isProduct = False
            self.isView = False
            if (str(inElement).endswith('.json')):
                print 'Constructor - inElement Open and Loads'
                print inElement
                self.jsonObj = serverUtilities.loadJson(open(inElement))  # Function that will throw exception if it is not the
                self.isElement = True
            elif serverUtilities.is_json(inElement):
                serverUtilities.printDebug('Is Element',data=inElement)
                self.jsonObj = inElement
                print self.jsonObj
                self.isElement = True
            else:
                self.jsonObj = json.loads(elementTemplates.TEMPLATE_EMPTY_ELEMENT)
                serverUtilities.printDebug('Is Empty Element', data=self.jsonObj)
                self.isEmptyElement = True

            if self.isElement:
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

                if self.type == 'View':
                    self.isView = True
                    self.childViews = []
                    self.childrenViews = self.specialization.get('childrenViews')
                    for each in self.childrenViews:
                        self.childViews.append(each)
                elif self.type == 'Product':
                    self.isProduct = True
                    self.childViews = []
                    try:
                        if self.specialization.get('view2view'):
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
                        if self.specialization.get('view2view'):

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
                if temp is not None:
                    self.contents = self.specialization.get('contents')
                    temp = self.specialization.get('type')
                    if temp is not None:
                        self.contentType = self.contents.get('type')
                temp = self.specialization.get('valueExpression')
                if temp is not None:
                    self.valueExpression = self.contents.get('valueExpression')
                temp = self.specialization.get('operand')
                if temp is not None:
                    self.operand = self.contents.get('operand')
            elif self.isEmptyElement:
                self.element = self.jsonObj.get('elements')[0]
                self.specialization = self.element.get('specialization')
                self.type = self.specialization.get('type')
                self.name = self.element.get('name')
                self.documentation = self.element.get('documentation')
                self.sysmlid = self.specialization.get('sysmlid')
                self.value = ''
                self.valueString = ''
                self.valueType = ''
                self.owner = None
                self.read = None
                self.isProduct = False
                self.isView = False
                self.created = None
                self.qualifiedId = None
                self.childViews = None
                if self.name[0] is '_':
                    self.sysmlid = self.name
                    self.name = 'Empty Element'


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
        if self.isEmptyElement:
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
            curl_cmd = regression_test_harness.create_curl_cmd(type="POST", data=postObject)
            # curl_cmd = regression_lib.regression_test_harness.create_curl_cmd(type="POST", data=postObject)
        else:
            return

        if (verbose):
            print 'Posting element:'
            return serverUtilities.execCurlCmd(curl_cmd)

    # TODO: Element calls post, of itself or of the sysmlid that is passed in
    def GET(self, sysmlid='', verbose=True, asJson=False):
        if (sysmlid is not ''):
            print '-------------------------------------------------'
            # curl_cmd = regression_lib.regression_test_harness.create_curl_cmd(type="GET",data=sysmlid,branch="master/",post_type="elements/")
            curl_cmd = regression_test_harness.create_curl_cmd(type="GET", data=sysmlid, branch="master/", post_type="elements/")
            printObj = json.dumps(self.element, sort_keys=True, indent=4, separators=(',', ': '))
            print printObj
        elif self.jsonObj is not None:
            # curl_cmd = regression_lib.regression_test_harness.create_curl_cmd(type="GET", data=self.sysmlid,branch = "master/", post_type ="elements/")
            curl_cmd = regression_test_harness.create_curl_cmd(type="GET", data=self.sysmlid, branch="master/", post_type="elements/")
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
        getObj = serverUtilities.execCurlCmd(curl_cmd)

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
        # curl_cmd = regression_lib.regression_test_harness.create_curl_cmd(type="GET", data=self.element)
        curl_cmd = regression_test_harness.create_curl_cmd(type="GET", data=self.element)
        eval_cmd = curl_cmd + '?evalute'
        printObj = json.dumps(self.element, sort_keys=True, indent=4, separators=(',', ': '))
        print printObj
        return serverUtilities.execCurlCmd(eval_cmd)

    def is_json(myjson):
        try:
            json_object = json.loads(myjson)
        except ValueError, e:
            return False
        return True
