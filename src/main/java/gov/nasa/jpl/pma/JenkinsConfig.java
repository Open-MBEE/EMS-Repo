package gov.nasa.jpl.pma;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableBiMap.Builder;

public class JenkinsConfig extends JenkinsEngine {

    private DocumentBuilder builder = null;
    public JenkinsConfig() {}

    private DocumentBuilder createDocumentBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch ( ParserConfigurationException e ) {
            e.printStackTrace();
        }
        return builder;
    }

    private Document getDocument() {
        StringBuilder xmlStringBuilder = new StringBuilder();

        xmlStringBuilder.append( "<?xml version=\"1.0\"?> <class> </class>" );
        ByteArrayInputStream input = null;
        try {
            input = new ByteArrayInputStream( xmlStringBuilder.toString().getBytes( "UTF-8" ) );
        } catch ( UnsupportedEncodingException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Document doc;
        try {
            doc = builder.parse( input );
        } catch ( SAXException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return doc;
    }
}
