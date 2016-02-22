#!/bin/bash
# Utility for checking the sysmlids between Baseline and Result

grep sysmlid developBaselineDir/$1.json | sort > t1.json
grep sysmlid developResultDir/$1.json | sort > t2.json

tkdiff t1.json t2.json
