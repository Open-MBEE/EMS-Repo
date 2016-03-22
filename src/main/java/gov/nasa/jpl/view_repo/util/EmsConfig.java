package gov.nasa.jpl.view_repo.util;

import java.util.Properties;

public class EmsConfig {
    public static Properties properties = new Properties();

    public static void setProperties(String propertiesFile) throws Exception {
        System.out.println("Properties: " + propertiesFile);
        EmsConfig.properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFile));
//        String[] propertiesFiles = propertiesFile.split( "," );
//        for (String file: propertiesFiles) { 
//            EmsConfig.properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(file));
//        }
    }

    public static String get(String key) {
        return EmsConfig.properties.getProperty(key);
    }
}
