package gov.nasa.jpl.pma;

// import org.w3c.dom.*;
import javax.xml.parsers.*;
// import java.io.*;

public class JenkinsConfig extends JenkinsEngine {

    public JenkinsConfig() {
        StringBuilder xmlStringBuilder = new StringBuilder();
        xmlStringBuilder.append( "<?xml )
    }

    private DocumentBuilder createDocumentBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch ( ParserConfigurationException e ) {
            e.printStackTrace();
        }
        return builder;
    }

}
