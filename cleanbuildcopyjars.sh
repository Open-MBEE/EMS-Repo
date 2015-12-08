#!/bin/bash
################################################################################
# Cleans all repos using 'mvn clean'
################################################################################


echo " ";
echo " ";
echo "----------<==========================>----------";
echo " ";
echo " ";
echo "Changing directory to BAE..."
echo " ";
echo "cd ../bae";
cd ../bae;
echo " ";
echo "Cleaning bae repository...";
echo " ";
echo "mvn clean";
echo " ";
mvn clean;
echo "Clean complete!"
echo " ";
echo " ";

echo "----------<==========================>----------";

echo " ";
echo " ";
echo "Changing directory to util...";
echo " ";
echo "cd ../util";
cd ../util;
echo " ";
echo "Cleaning util repository...";
echo " ";
echo "mvn clean";
mvn clean;
echo " ";
echo "Clean complete!";
echo " ";
echo " ";

echo "----------<==========================>----------";

echo " ";
echo " ";
echo "Changing directory to sysml...";
echo " ";
echo "cd ../sysml";
cd ../sysml;
echo " ";
echo "Cleaning sysml repository...";
echo " ";
echo "mvn clean";
mvn clean;
echo "Clean complete!"
echo " ";
echo " ";

echo "----------<==========================>----------";

echo " ";
echo " ";
echo "Changing directory to klang...";
echo " ";
echo "cd ../klang";
echo " ";
cd ../klang;
echo "Cleaning klang repository...";
echo " ";
echo "mvn clean";
mvn clean;
echo " ";
echo "Clean complete!";
echo " ";
echo " ";




################################################################################
# Builds the packages for all the respos using 'mvn package'
################################################################################

echo "----------<==========================>----------";
echo " ";
echo " ";
echo "Starting build process...";
echo " ";
echo " ";
echo "----------<==========================>----------";

################################################################################
echo " ";
echo " ";
echo "Changing directory to BAE...";
echo " ";
echo "cd ../bae";
cd ../bae;
echo " ";
echo "Building bae jar...";
mvn package;
echo " ";
echo "Packaging complete!";
echo " ";


echo " ";
echo "----------<==========================>----------";
echo " ";
echo " ";
echo "Changing directory to klang...";
echo " ";
echo "cd ../klang";
cd ../klang;
echo " ";
echo "Building klang jar...";
mvn package;
echo " ";
echo "Packaging complete!";
echo " ";
echo " ";

echo "----------<==========================>----------";

echo " ";
echo " ";
echo "Changing directory to util...";
echo " ";
echo "cd ../util";
cd ../util;
echo " ";
echo "Building util jar...";
mvn package;
echo " ";
echo "Packaging complete!";
echo " ";
echo " ";

echo "----------<==========================>----------";

echo " ";
echo " ";
echo "Changing directory to sysml...";
echo " ";
echo "cd ../sysml";
cd ../sysml;
echo " ";
echo "Building sysml jar...";
mvn package;
echo " ";
echo "Packaging complete!";
echo " ";
echo " ";
echo "----------<==========================>----------";
echo " ";
echo " ";

echo "changing to Alfresco-view-repo";
echo " ";
echo "cd ../alfresco-view-repo";
cd '../alfresco-view-repo';
echo " ";
echo " ";
echo "----------<==========================>----------";


################################################################################
# Copies the jar files from each of the repos into the alfresco-view-repo
################################################################################
echo " ";
echo " ";
echo "Starting copy process...";
echo " ";
echo " ";

echo "----------<==========================>----------";

echo " ";
echo " ";
echo "Copying jar files...";
echo " ";
echo 'mv ../bae/target/bae-2.2.0-SNAPSHOT.jar ./src/main/amp/web/WEB-INF/lib/ae.jar';
mv "../bae/target/bae-2.2.0-SNAPSHOT.jar" "./src/main/amp/web/WEB-INF/lib/AE.jar";
echo " ";
echo " ";
echo "mv ../util/target/mbee_util-2.2.0-SNAPSHOT.jar ./src/main/amp/web/WEB-INF/lib/mbee_util.jar";
mv "../util/target/mbee_util-2.2.0-SNAPSHOT.jar" "./src/main/amp/web/WEB-INF/lib/mbee_util.jar";
echo " ";
echo " ";
echo "../sysml/target/sysml-2.2.0-SNAPSHOT.jar ./src/main/amp/web/WEB-INF/lib/sysml.jar";
mv "../sysml/target/sysml-2.2.0-SNAPSHOT.jar" "./src/main/amp/web/WEB-INF/lib/sysml.jar";
echo " ";
echo " ";
echo "mv ../klang/target/klang-2.2.0-SNAPSHOT.jar ./src/main/amp/web/WEB-INF/lib/klang.jar";
mv "../klang/target/klang-2.2.0-SNAPSHOT.jar" "./src/main/amp/web/WEB-INF/lib/klang.jar";
