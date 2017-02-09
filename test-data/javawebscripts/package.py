#!/usr/bin/env python

import os
import modulefinder

library_files = os.getcwd()


FILES_EXT = ['regression_lib.py',
         'regression_test_harness.py',
         'initialize_caches.py',
         'createElements.py',
         'utilityElement.py',
         'record_curl.py',
         'sampleData.py',
         'serverUtilities.py',
         'waitOnServer.py']

FILES = ['regression_lib',
         'regression_test_harness',
         'initialize_caches',
         'createElements',
         'utilityElement',
         'record_curl',
         'sampleData',
         'serverUtilities',
         'waitOnServery']

# Test output for concatenating the Python library files, used for debugging
for filename in FILES:
    print(os.path.join(library_files, filename))


# Imports all the files specified within the FILES array
for filename in FILES:
    modulefinder.AddPackagePath(filename, library_files)
