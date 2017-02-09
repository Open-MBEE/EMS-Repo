package gov.nasa.jpl.view_repo.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

public class EmsConfig {
    public static Properties properties = new Properties();

    public static void setProperties(String propertiesFile) throws Exception {
        InputStream resourceAsStream =  EmsConfig.class.getClassLoader().getResourceAsStream("mms.properties");

        if (resourceAsStream != null) {
            properties.load(resourceAsStream);
        }
        else {
            EmsConfig.properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFile));
        }
    }

    public static String get(String key) {
        return EmsConfig.properties.getProperty(key);
    }
    
    public static void setProperty(String key, String value) {
        EmsConfig.properties.setProperty( key, value );
    }
    
    public static void saveProperties() {
        try {
            URL resource = EmsConfig.class.getClassLoader().getResource("mms.properties");
            File f = new File(resource.getFile());
            OutputStream out = new FileOutputStream(f);
            EmsConfig.properties.store( out, "updated" );
            out.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
