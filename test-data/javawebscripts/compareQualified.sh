#!/bin/bash

grep qualifiedId developBaselineDir/$1 | sort > t1.json
grep qualifiedId developResultDir/$1 | sort > t2.json

tkdiff t1.json t2.json
