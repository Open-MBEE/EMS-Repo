import os
#import commands
#import re
import time
#import subprocess
import sys
#import optparse
#import glob
#import json

def waitOnServer():
    print "POLLING SERVER"
    server_log = open("runserver.log","r")
    seek = 0
    fnd_line = False
    for timeout in range(0,600):
        server_log.seek(seek)
        for line in server_log:
            if "Starting ProtocolHandler" in line:
                fnd_line = True
                break

        if fnd_line:
            break

        seek = server_log.tell()
        time.sleep(1)

        if timeout%10 == 0:
            print ".."

    if fnd_line:
        print "SERVER CONNECTED"

    else:
        print "ERROR: SERVER TIME-OUT"

##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    waitOnServer()

