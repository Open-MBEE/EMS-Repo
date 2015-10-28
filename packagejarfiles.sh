#!/bin/bash

echo "Changing directory to BAE..."
echo "cd .../bae";
cd ../bae;
echo "Building bae jar...";
mvn package;
echo "Packaging complete!"

echo "Changing directory to util..."
echo "cd ../util";
cd ../util;
echo "Building util jar...";
mvn package;
echo "Packaging complete!"

echo "Changing directory to sysml..."
echo "cd ../sysml"
cd ../sysml;
echo "Building sysml jar...";
mvn package;
echo "Packaging complete!"

echo "Changing directory to klang..."
echo "cd ../klang"
cd ../klang;
echo "Building klang jar...";
mvn package;
echo "Packaging complete!"
