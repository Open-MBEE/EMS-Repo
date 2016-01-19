#!/bin/bash

# Adds a view to another view
#   Currently only adds a view to a product
if [ "$#" -eq 3 ]; then

echo "Adding $1 to the element $2"
#$JSON = json;
#$NAME = name;
echo "--------====================--------"
if [ $1 == json ]; then
    echo "python -c 'from serverUtilities import *; addViewToViewByJSON(\"$2\",\"$3\");'"
    python -c "from serverUtilities import *; addViewToViewByJSON('$2','$3');"

elif [ $1 == name ]; then
    echo "python -c 'from serverUtilities import *; addViewToViewByName(\"$2\",\"$3\");'"
    python -c "from serverUtilities import *; addViewToViewByName('$2','$3');"

else
    echo "Invalid Input"
fi
fi