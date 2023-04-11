package home.barcooda.dlt.utils.xml;

import home.barcooda.dlt.utils.maven.Constants;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class XmlUtils {

    public static void updateXmlFile(String filePath, Consumer<Document> callFunction) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try (InputStream is = new FileInputStream(filePath)) {

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);

            callFunction.accept(doc);

            try (FileOutputStream output =
                         new FileOutputStream(filePath)) {
                XmlUtils.writeXml(doc, output);
            }

        } catch (ParserConfigurationException | SAXException
                 | IOException | TransformerException e) {
            e.printStackTrace();
        }
    }

    public static Document readXmlFile(String filePath) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = null;

        try (InputStream is = new FileInputStream(filePath)) {

            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(is);

        } catch (ParserConfigurationException | SAXException
                 | IOException e) {
            e.printStackTrace();
            return null;
        }

        return doc;
    }

    public static Document readXmlString(String xmlContent) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = null;

        try (InputStream is = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {

            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(is);

        } catch (ParserConfigurationException | SAXException
                 | IOException e) {
            e.printStackTrace();
            return null;
        }

        return doc;
    }

    // write doc to output stream
    public static void writeXml(Document doc, OutputStream output)
            throws TransformerException, UnsupportedEncodingException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        ClassLoader classLoader = XmlUtils.class.getClassLoader();
        InputStream resourceAsStream = classLoader.getResourceAsStream(Constants.XML_XSLT_FILE_NAME);
        Transformer transformer = transformerFactory.newTransformer(
                new StreamSource(resourceAsStream));

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);

    }
}
