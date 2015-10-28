#!/bin/bash

echo "Changing directory to BAE..."
echo "cd ../bae";
cd ../bae;
echo "Cleaning bae repository...";
echo "mvn clean";
mvn clean;
echo "Clean complete!"

echo "Changing directory to util..."
echo "cd ../util";
cd ../util;
echo "Cleaning util repository...";
echo "mvn clean";
mvn clean;
echo "Clean complete!"

echo "Changing directory to sysml..."
echo "cd ../sysml"
cd ../sysml;
echo "Cleaning sysml repository...";
echo "mvn clean";
mvn clean;
echo "Clean complete!"

echo "Changing directory to klang..."
echo "cd ../klang"
cd ../klang;
echo "Cleaning klang repository...";
echo "mvn clean";
mvn clean;
echo "Clean complete!";

