package budgetgenerator.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oracle.xdo.template.RTFProcessor;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BIUtils {

    public BIUtils() {
        super();
    }

    public static byte[] createPDF(String xml, InputStream isRtf) {
        byte[] by = null;
        try {
            Element node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8"))).getDocumentElement();
            BIUtils utils = new BIUtils();
            by = utils.createReport(node, isRtf, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return by;
    }

    public byte[] createReport(Node node, InputStream is, HashMap mapFields) throws Exception {
        ByteArrayOutputStream fileOutputStream = null;
        ByteArrayOutputStream rtfOutputStream = null;
        RTFProcessor rtfProcessor = null;
        FOProcessor processor = null;
        byte pdfBytes[] = null;
        try {
            pdfBytes = toByteArray(toString(node));
            fileOutputStream = new ByteArrayOutputStream();
            rtfOutputStream = new ByteArrayOutputStream();
            rtfProcessor = new RTFProcessor(is);
            rtfProcessor.setOutput(rtfOutputStream);
            rtfProcessor.process();
            processor = new FOProcessor();
            processor.setData(new ByteArrayInputStream(pdfBytes));
            processor.setTemplate(new ByteArrayInputStream(rtfOutputStream.toByteArray()));
            processor.setOutput(fileOutputStream);
            processor.setOutputFormat(FOProcessor.FORMAT_PDF);
            processor.generate();
            return fileOutputStream.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new byte[]{};
    }

    /**
     * Convierte un Nodo a String
     *
     * @param node Nodo a convertir
     * @return Nodo convertido a String
     */
    private static String toString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            te.printStackTrace();
        }
        return sw.toString();
    }

    private static byte[] toByteArray(String content) {
        byte response[];
        try {
            response = content.getBytes("UTF-8");
        } catch (Exception e) {
            response = new byte[]{};
        }
        return response;
    }

}
