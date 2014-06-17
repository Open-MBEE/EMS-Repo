#!/usr/bin/python
from xml.dom.minidom import parse
from pprint import pprint
import collections

CONTEXT_PATH = '../src/main/amp/config/alfresco/module/view-repo/context/'

CONTEXT_FILES = [CONTEXT_PATH + 'javawebscript-service-context.xml',
				 CONTEXT_PATH + 'mms-service-context.xml']

DESC_PATH = '../src/main/amp/config/alfresco/extension/templates/webscripts/'
url2class = {}

def main():
	for filename in CONTEXT_FILES:
		parseContext(filename)

	od = collections.OrderedDict(sorted(url2class.items()))

	html = open('class2url.html', 'w')

	html.write('<html><body><table>')
	for key, value in od.items():
		for entry in value:
			html.write('<tr><td>' + key + '</td><td>' + entry + '</td></tr>')
	html.write('</body></table></html>')


def parseContext(filename):
	dom = parse(filename)
	beans = dom.getElementsByTagName('bean')
	for bean in beans:
		beanClass = bean.getAttribute('class')
		beanId = bean.getAttribute('id')
		descFile = convertBeanId2DescFile(beanId)
		url = getUrlFromDesc(descFile)
		if not url2class.has_key(url):
			url2class[url] = []
		url2class[url].append(beanClass)


def getUrlFromDesc(descFile):
	try:
		dom = parse(descFile)
		urls = dom.getElementsByTagName('url')
		return urls[0].toxml().replace('<url>','').replace('</url>','')
	except:
		return None


def convertBeanId2DescFile(id):
	id = id.replace('webscript.', '')
	tokens = id.split('.')

	filename = DESC_PATH
	for ii in range(len(tokens)-1):
		filename += tokens[ii] + '/'
	filename = filename[:-1] + '.' + tokens[ii+1] + '.desc.xml'

	return filename


if __name__ == "__main__":
	main()

