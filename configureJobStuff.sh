#! /bin/sh

# Setting the Doors-NG Server
doors_server=${HOSTNAME/$HOSTNAME/doors-ng}

tw_server=${HOSTNAME/-ems/-tw}

# Setting the teamwork server

if [[ "${tw_server}" == *uat* ]] ; then
        tw_server=`echo $tw_server | sed -e 's/-origin//'`
        doors_server=${doors_server}-uat
elif [[ "${tw_server}" == *test* ]] ; then
        tw_server=`echo $tw_server | sed -e 's/-origin//' -e 's/-test/-uat/'`
        doors_server=${doors_server}-uat
elif [[ "${tw_server}" == *int* ]] ; then
        tw_server=`echo $tw_server | sed -e 's/-origin//' -e 's/-int/-uat/'`
        doors_server=${doors_server}-uat
fi

# Setting the MMS Server
mms_server=${HOSTNAME/-origin/}

echo $mms_server
echo $tw_server
echo $doors_server

# Setting the properties file for the server
sed -i -e "s/mms/$mms_server/g" config/alfresco/mms.properties
sed -i -e "s/teamwork/$tw_server/g" config/alfresco/mms.properties
sed -i -e "s/doors/$doors_server/g" config/alfresco/mms.properties
