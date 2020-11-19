package ca.bc.gov.open.jag.documentutils.adobe;

import ca.bc.gov.open.jag.documentutils.adobe.models.MergeDoc;
import ca.bc.gov.open.jag.documentutils.api.MediaTypes;
import ca.bc.gov.open.jag.documentutils.api.models.DocMergeRequest;
import ca.bc.gov.open.jag.documentutils.api.models.DocMergeResponse;
import ca.bc.gov.open.jag.documentutils.exception.MergeException;
import ca.bc.gov.open.jag.documentutils.utils.PDFBoxUtilities;
import com.adobe.idp.Document;
import com.adobe.idp.dsc.clientsdk.ServiceClientFactory;
import com.adobe.livecycle.assembler.client.AssemblerOptionSpec;
import com.adobe.livecycle.assembler.client.AssemblerResult;
import com.adobe.livecycle.assembler.client.AssemblerServiceClient;
import com.adobe.livecycle.assembler.client.OperationException;
import com.adobe.livecycle.docconverter.client.ConversionException;
import com.adobe.livecycle.docconverter.client.DocConverterServiceClient;
import com.adobe.livecycle.docconverter.client.PDFAConversionOptionSpec;
import com.adobe.livecycle.docconverter.client.PDFAConversionResult;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PDF Merge service.
 *
 * @author shaunmillargov
 */
@Service
public class MergeServiceImpl implements MergeService {

    private final Logger logger = LoggerFactory.getLogger(MergeServiceImpl.class);

    private final ServiceClientFactory serviceClientFactory;
    private final AssemblerServiceClient assemblerClient;

    public MergeServiceImpl(ServiceClientFactory serviceClientFactory, AssemblerServiceClient assemblerClient) {
        this.serviceClientFactory = serviceClientFactory;
        this.assemblerClient = assemblerClient;
    }

    @Override
    public DocMergeResponse mergePDFDocuments(DocMergeRequest request, String correlationId) {

        DocMergeResponse resp = new DocMergeResponse();

        try {

            logger.info("Calling mergePDFDocuments...");


            // Sort the document based on placement id in the event they are mixed. lowest to highest
            request.getDocuments().sort(Comparator.comparing(ca.bc.gov.open.jag.documentutils.api.models.Document::getIndex));

            LinkedList<MergeDoc> pageList = request.getDocuments().stream()
                    .map(doc -> buildMergeDoc(doc, request))
                    .collect(Collectors.toCollection(LinkedList::new));

            // Use DDXUtils to Dynamically generate the DDX file sent to AEM.
            org.w3c.dom.Document aDDx = DDXUtils.createMergeDDX(pageList, request.getOptions().isCreateToC());
            Document myDDX = DDXUtils.convertDDX(aDDx);

            // Create a Map object to store the PDF source documents
            Map<String, Object> inputs = pageList.stream()
                    .collect(Collectors.toMap(MergeDoc::getId, doc -> new Document(doc.getFile())));

            // Create an AssemblerOptionsSpec object
            AssemblerOptionSpec assemblerSpec = new AssemblerOptionSpec();
            assemblerSpec.setFailOnError(false);

            // Submit the job to Assembler service
            AssemblerResult jobResult = assemblerClient.invokeDDX(myDDX, inputs, assemblerSpec);
            Map<String, Document> allDocs = jobResult.getDocuments();

            // Iterate through the map object to retrieve the result PDF document
            resp.setDocument(allDocs.entrySet().stream()
                    .filter(mapEntry -> mapEntry.getKey().equalsIgnoreCase(AdobeKeys.DDX_OUTPUT_NAME))
                    .map(mapEntry -> buildOutputDocument((Document)mapEntry.getValue()))
                    .findFirst().get());

            resp.setMimeType(MediaTypes.APPLICATION_PDF);

            return resp;

        } catch (TransformerException | OperationException | ParserConfigurationException e) {

            logger.error("Exception while merging documents", e);
            throw new MergeException("Exception while merging documents", e.getCause());

        }

    }

    private MergeDoc buildMergeDoc(ca.bc.gov.open.jag.documentutils.api.models.Document doc, DocMergeRequest request) throws RuntimeException {

        byte[] docBytes = Base64Utils.decode(doc.getData().getBytes());

        if (request.getOptions().isForcePDFAOnLoad() && PDFBoxUtilities.isPDFXfa(docBytes)) {
            logger.info("forcePDFA is on and document, order {}, is XFA. Converting to PDF/A...", doc.getIndex());

            try {
                docBytes = createPDFADocument(docBytes, serviceClientFactory);
            } catch (ConversionException | IOException e) {
               logger.error("Error creating pdf a ", e);
               throw new RuntimeException(e.getMessage());
            }
        }

        logger.info("Loaded page {}", doc.getIndex());
        return new MergeDoc(docBytes);
    }

    private String buildOutputDocument(Document document) {
        try {
            return Base64Utils.encodeToString(IOUtils.toByteArray(document.getInputStream()));
        } catch (IOException e) {
            logger.error("Error creating pdf a ", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Transforms PDF (XFA or otherwise) to PDF/A type (most basic).
     *
     * @param inputFile
     * @return
     * @throws ConversionException
     * @throws IOException
     */

    private byte[] createPDFADocument(byte[] inputFile, ServiceClientFactory factory) throws ConversionException, IOException {

        // Create a DocConverterServiceClient object
        DocConverterServiceClient docConverter = new DocConverterServiceClient(factory);
        Document inDoc = new Document(inputFile);

        // Create a PDFAConversionOptionSpec object and set tracking information
        PDFAConversionOptionSpec spec = new PDFAConversionOptionSpec();

        // AEM logging level - suggest turning this off unless issues need debugging on AEM side.
        //spec.setLogLevel("INFO");
        //spec.setLogLevel("FINE");

        // Convert the PDF document to a PDF/A document
        PDFAConversionResult result = docConverter.toPDFA(inDoc, spec);

        // Save the PDF/A file
        Document pdfADoc = result.getPDFADocument();
        return IOUtils.toByteArray(pdfADoc.getInputStream());
    }

}