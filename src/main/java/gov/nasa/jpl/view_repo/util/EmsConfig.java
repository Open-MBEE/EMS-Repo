package gov.nasa.jpl.view_repo.util;

import java.util.Properties;

public class EmsConfig {

    public static Properties properties = new Properties();
    
    public static void setProperties(Properties props) {
        EmsConfig.properties = props;
    }
    
    public static String get(String key) {
        return EmsConfig.properties.getProperty(key);
    }
}
