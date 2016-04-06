#! /bin/sh

profile=$1

echo "Using dev profile  : " $profile-dev

if [ $1 = 'mms' ]; then
	echo "sed -i -e "s/mbee/$profile/g" runserver.sh .settings/org.eclipse.m2e.core.prefs"
	sed -i -e "s/mbee/$profile/g" runserver.sh .settings/org.eclipse.m2e.core.prefs
	sleep 2
	cd ../alfresco-view-repo && ./cleanbuildcopyjars.sh
else
	echo "sed -i -e "s/mbee/$profile/g" runserver.sh .settings/org.eclipse.m2e.core.prefs"
	sed -i -e "s/mms/$profile/g" runserver.sh .settings/org.eclipse.m2e.core.prefs
fi
