package ca.bc.gov.open.jag.documentutils.adobe;

import ca.bc.gov.open.jag.documentutils.adobe.models.MergeDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;


/**
 * A set of DDX utils.
 *
 * @author shaunmillargov
 */
public class DDXUtils {

    private final static Logger logger = LoggerFactory.getLogger(DDXUtils.class);

    /**
     * Creates a Merge DDX document for Assembler using an org.w3c.dom.Document object
     *
     * @return
     * @throws Exception
     */
    public static Document createMergeDDX(LinkedList<MergeDoc> pageList, boolean addToC) throws ParserConfigurationException {

        // Create DocumentBuilderFactory and DocumentBuilder objects
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create a new Document object
        Document document = builder.newDocument();

        // Create the root element and append it to the XML DOM
        Element root = (Element) document.createElement(AdobeKeys.DDX_ELEMENT_DDX);
        root.setAttribute(AdobeKeys.DDX_NAMESPACE_ATTRIBUTE, AdobeKeys.DDX_NAMESPACE);
        document.appendChild(root);

        // Create the output element
        Element PDFs = (Element) document.createElement(AdobeKeys.DDX_ELEMENT_PDF);
        PDFs.setAttribute(AdobeKeys.DDX_OUTPUT_ATTRIBUTE, AdobeKeys.DDX_OUTPUT_NAME);
        root.appendChild(PDFs);

        // Add ToC?
        if (addToC) {
            logger.info("Adding table of contents...");
            Element ToC = (Element) document.createElement("TableOfContents");
            ToC.setAttribute("bookmarkTitle", "Table Of Contents");
            PDFs.appendChild(ToC);
        }

        // Add each pageId element to the DDX
        for (int i = 0; i < pageList.size(); i++) {
            Element PDF = (Element) document.createElement(AdobeKeys.DDX_ELEMENT_PDF);
            PDF.setAttribute(AdobeKeys.DDX_SOURCE_ATTRIBUTE, pageList.get(i).getId());
            if (addToC) {
                PDF.setAttribute("bookmarkTitle", "Document " + (i + 1));
                PDF.setAttribute("includeInTOC", "true");
            }
            PDFs.appendChild(PDF);
        }

        return document;

    }

    /**
     * Converts between org.w3c.dom.Document and com.adobe.idp.Document types.
     *
     * @param ddx
     * @return
     */
    public static com.adobe.idp.Document convertDDX(Document ddx) throws TransformerException {

        TransformerFactory transFact = TransformerFactory.newInstance();
        Transformer transForm = transFact.newTransformer();

        ByteArrayOutputStream myOutStream = new ByteArrayOutputStream();
        javax.xml.transform.dom.DOMSource myInput = new DOMSource(ddx);
        javax.xml.transform.stream.StreamResult myOutput = new StreamResult(myOutStream);

        transForm.transform(myInput, myOutput);

        byte[] byteArray = myOutStream.toByteArray();

        return new com.adobe.idp.Document(byteArray);

    }

}
