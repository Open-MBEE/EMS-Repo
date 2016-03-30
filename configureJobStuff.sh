#! /bin/sh

project=$1

tw_server=${HOSTNAME/-ems/-tw}

# Setting the teamwork server
if [[ "${tw_server}" == *uat* ]] ; then
        tw_server=`echo $tw_server | sed -e 's/-origin//'`
elif [[ "${tw_server}" == *test* ]] ; then
        tw_server=`echo $tw_server | sed -e 's/-origin//' -e 's/-test/-uat/'`
elif [[ "${tw_server}" == *int* ]] ; then
        tw_server=`echo $tw_server | sed -e 's/-origin//' -e 's/-int-uat//'`
fi

# Setting the mms server
mms_server=${HOSTNAME/-origin/}.jpl.nasa.gov
tw_server=${tw_server}.jpl.nasa.gov

sed -i -e "s/localhost/$mms_server/g" config/alfresco/ems.properties
sed -i -e "s/teamwork/$tw_server/g" config/alfresco/ems.properties

