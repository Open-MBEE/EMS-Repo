#!/bin/bash
# Utility for doing diff between Baseline and Result directories

tkdiff developBaselineDir/$1.json developResultDir/$1.json
