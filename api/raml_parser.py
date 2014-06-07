#!#/usr/bin/python

'''
Need to install the following Python packages for this to work.

pyraml-parser: https://github.com/an2deg/pyraml-parser
cheetah: http://pythonhosted.org//Cheetah/
'''

import pyraml.parser
import commands
from sets import Set
from Cheetah.Template import Template

FILENAME_PREFIX = '../src/main/amp/config/alfresco/extension/templates/webscripts/gov/nasa/jpl/mms'

# keep track of all the template variables
templateVars = Set([])

readCollection = [
	'workspaces',
	'sites',
	'changesets'
]

readCollectionOps = ['get']

collection = [
	'elements',
	'comments',
	'versions',
	'configurations',
	'snapshots',
	'projects',
	'products',
	'views',
	'artifacts'
]

collectionOps = ['get', 'delete', 'post', 'put']

def main():
	raml = pyraml.parser.load('api.raml')

	getResources(raml.resources)

	return raml


def getResources(resources, parent=''):
	for name in resources.keys():
		resource = resources[name]
		newparent = parent + name
		print newparent
		createFiles(parent, name, resource)
		getResources(resource.resources, newparent)


def loadTemplate(filename):
	templateDef = ''
	f = open(filename)
	for line in f:
		templateDef += line
	f.close()
	return templateDef

template = loadTemplate('desc.xml.cheetah')


def createFiles(path, name, resource):
	# make directories up to leaf and make leaf descriptor
	if name.find('/{') >= 0:
		templateVars.add(name)
		writeMethodFiles(path, name)
	else:
		execCmd('mkdir -p ' + FILENAME_PREFIX + cleanPath(path) + name)
		writeMethodFiles(path, name)


def writeMethodFiles(path, name):
	cpath = cleanPath(path)
	print '\twriteMethodFiles', cpath, name
	lname = getLastPath(path, name)

	methods = None
	if lname.replace('/', '') in collection:
		methods = collectionOps
	elif lname.replace('/','') in readCollection:
		methods = readCollectionOps

	# remove the trailing plural if putting into owning folder
	if name.find('{') >= 0:
		lname = lname[:-1]
	
	if methods:
		for method in methods:
			ns = loadNameSpace(path, name, method)
			filename = cpath + lname + '.' + method + '.desc.xml'
			print '\t\t' + filename
			writeFiles(filename, template, ns)


def loadNameSpace(path, name, method):
	if method == 'get':
		permission = 'readonly'
	else:
		permission = 'readwrite'
	if name.find('{') >= 0:
		shortname = method + ' ' + getLastPath(path, name).replace('/', '')[:-1] + ' with ' + name.replace('/','')
	else:
		shortname = method + ' ' + getLastPath(path, name).replace('/', '') 
	return {'shortname': shortname, 
			'description': shortname, 
			'url': path + name, 
			'permission': permission}


def writeFiles(filename, templateDef, namespace):
	t = Template(templateDef, searchList=[namespace])
	filename = FILENAME_PREFIX + filename
	f = open(filename, 'w')
	f.write(t.__str__())
	f.close()

	f = open(filename.replace('desc.xml', 'json.ftl'), 'w')
	f.write('${res}')
	f.close()


def cleanPath(path):
	for var in templateVars:
		path = path.replace(var, '')

	return path


def getLastPath(path, name):
	'''
	Get last path name based on the name. If the name is a template variable
	return the end of path, otherwise the name is the extension to the path
	'''
	if name.find('{') >= 0:
		if path.find('/') < 0:
			return path
		return path[path.rindex('/'):]
	else:
		return name


# execute command and return results
def execCmd(cmd):
  print '\t', cmd
  results = commands.getoutput(cmd)
  return results


if __name__ == "__main__":
	main()

