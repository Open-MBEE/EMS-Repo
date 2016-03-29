#! /bin/sh

project=$1

# Setting the teamwork server
if [[ $HOSTNAME == *uat* ]] ; then
	tw_server=${HOSTNAME/-ems/-tw} | sed -e s/-origin// 
elif [[ $HOSTNAME == *test* ]]
	tw_server=${HOSTNAME/-ems/-tw} | sed -e s/-origin// | sed -e s/-test/-uat/
elif [[ $HOSTNAME == *int* ]]
        tw_server=${HOSTNAME/-ems/-tw} | sed -e s/-origin// | sed -e s/-int-uat//
fi

# Setting the mms server
mms_server=${HOSTNAME/-origin/}.jpl.nasa.gov

sed -i -e 's/localhost/$mms_server/g' config/alfresco/$project.properties
