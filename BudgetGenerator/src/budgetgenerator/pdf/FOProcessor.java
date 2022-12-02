package budgetgenerator.pdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import oracle.xdo.XDOException;
import oracle.xdo.XDOIOException;
import oracle.xdo.common.io.FileUtil;
import oracle.xdo.common.io.PipeOutputStream;
import oracle.xdo.common.lang.LocaleUtil;
import oracle.xdo.common.log.LogOutputStream;
import oracle.xdo.common.log.Logger;
import oracle.xdo.common.tmp.TmpFile;
import oracle.xdo.common.util.AttributeParser;
import oracle.xdo.common.util.ThreadContext;
import oracle.xdo.common.xml.ManualImport;
import oracle.xdo.common.xml.SAXPrintHandler;
import oracle.xdo.common.xml.XSLTOptimizer;
import oracle.xdo.common.xml.XSLTSectionCollection;
import oracle.xdo.pdf2x.common.options.PageRanges;
import oracle.xdo.pdf2x.pdf2image.PDF2Image;
import oracle.xdo.template.PropertyManager;
import oracle.xdo.template.fo.FOProcessingEngine;
import oracle.xdo.template.fo.util.FOUtility;
import oracle.xdo.template.fo.xml2xsd.DataModel;
import oracle.xdo.template.fo.xml2xsd.DocumentParser;
import oracle.xdo.template.rtf.xliff.XLIFFUtil;
import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.SAXParser;
import oracle.xml.parser.v2.XMLDocument;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

public class FOProcessor implements Runnable {
    public static final byte FORMAT_AWT = 0;
    public static final byte FORMAT_PDF = 1;
    public static final byte FORMAT_RTF = 2;
    public static final byte FORMAT_HTML = 3;
    public static final byte FORMAT_PagedHTML = 37;
    public static final byte FORMAT_UIX = 3;
    public static final byte FORMAT_EXCEL = 4;
    public static final byte FORMAT_EXCEL_MHTML = 5;
    public static final byte FORMAT_FO = 19;
    public static final byte FORMAT_PPTMHT = 25;
    public static final byte FORMAT_PPTX = 26;
    public static final byte FORMAT_MHTML = 35;
    public static final byte FORMAT_XLSX = 36;
    public static final byte FORMAT_PDFZ = 38;
    private static final byte FORMAT_IMAGE_START = 40;
    public static final byte FORMAT_IMAGE_PNG = 40;
    public static final byte FORMAT_IMAGE_JPG = 41;
    public static final byte FORMAT_IMAGE_BMP = 42;
    public static final byte FORMAT_IMAGE_GIF = 43;
    private static final byte FORMAT_IMAGE_END = 43;
    public static final byte FORMAT_USERSPECIFIED = -1;
    private FOProcessingEngine mFope;
    private PropertyManager mPropMan;
    private String mLocale;
    private Object[] mXmlInputs;
    private Object[] mXslInputs;
    private Object[] mXliffInputs;
    private Object mXmlInput;
    private Object mXslInput;
    private Object mXliffInput;
    private OutputStream mOutStream;
    private Object mOutStreamParam;
    private XDOException mPreviousException;
    private Vector mToBeClosed;
    private String mSessionTimestamp;
    private int mXmlCount;
    private int mXslCount;
    private int mFoCount;
    private byte mFormat;
    private volatile Exception mRunException = null;
    private static int sLogFileNameIndexNum = 0;
    private String mXdoDebugLevel = null;
    private String mXmlDataEncoding = null;
    private static final String XDO_PREFIX = "xdo_";
    private static final String DATA_POSTFIX = "_fo_data_";
    private static final String TEMPLATE_POSTFIX = "_fo_data_";
    private static final String TRANSLATION_POSTFIX = "_fo_xliff_";
    private static final String FO_POSTFIX = "_fo_fo_";
    private static final String OUT_POSTFIX = "_fo_out_";
    private static final String MERGED_POSTFIX = "_fo_merged_";
    private static final String TEMPLATE_EXT = ".xsl";
    private static final String XML_EXT = ".xml";
    private static final String TRANSLATION_EXT = ".xml";
    private static final String FO_EXT = ".fo";
    private static final String OUT_EXT = ".out";
    protected XSLTSectionCollection mOptimizedSections = null;

    public FOProcessor() {
        this.initParams();
        this.mPropMan = new PropertyManager();
        Logger.log(this, "FOProcessor has been initialized without default config.", 1);
    }

    private void initParams() {
        XDOStreamHandlerFactory.registerNewProtocols();
        Logger.init();
        Logger.log(this, "FOProcessor constructor is called.", 1);
        this.mLocale = LocaleUtil.getDefaultLocaleString();
        this.mToBeClosed = new Vector(6);
        this.mSessionTimestamp = null;
        this.mXmlCount = 0;
        this.mXslCount = 0;
        this.mFoCount = 0;
        this.mXmlInputs = null;
        this.mXslInputs = null;
        this.mXmlInput = null;
        this.mXslInput = null;
        this.mOutStream = null;
        this.mOutStreamParam = null;
        this.mPreviousException = null;
        this.mFope = new FOProcessingEngine();
        this.mFormat = -1;
    }

    public void setLocale(String locale) {
        this.mLocale = LocaleUtil.validate(locale);
        this.mPropMan.setLocale(this.mLocale);
        Logger.log(this, "FOProcessor.setLocale is called with '" + locale + "'.", 1);
    }

    public void setLocale(Locale locale) {
        this.setLocale(LocaleUtil.getLocaleString(locale));
    }

    public void setConfig(InputStream docLevelInputStream) {
        this.mPropMan.setConfig(docLevelInputStream);
        Logger.log(this, "FOProcessor.setConfig(InputStream) is called", 1);
    }

    public void setConfig(String docLevelConfigPath) {
        this.mPropMan.setConfig(docLevelConfigPath);
        Logger.log(this, "FOProcessor.setConfig(String) is called with '" + docLevelConfigPath + "'.", 1);
    }

    public void setData(String xmlFile) {
        Logger.log(this, "FOProcessor.setData(String) is called with '" + xmlFile + "'.", 1);

        try {
            this.mXmlInput = this.checkXdoDebugDir(this.getInputStream(xmlFile));
        } catch (IOException var3) {
            this.mPreviousException = new XDOIOException(var3);
            Logger.log(this, "IOException is occurred in FOProcessor.setData(" + xmlFile + ").", 4);
        }

    }

    public void setData(InputStream stream) {
        Logger.log(this, "FOProcessor.setData(InputStream) is called.", 1);
        this.mXmlInput = this.checkXdoDebugDir(stream);
    }

    public void setData(Reader reader) {
        Logger.log(this, "FOProcessor.setData(Reader) is called.", 1);
        this.mXmlInput = this.checkXdoDebugDir(reader);
    }

    public void setData(InputStream[] xmlInput) {
        this.mXmlInputs = xmlInput;
        Logger.log(this, "FOProcessor.setData(InputStream[]) is called.", 1);
    }

    public void setData(Reader[] xmlReaders) {
        this.mXmlInputs = xmlReaders;
        Logger.log(this, "FOProcessor.setData(Reader[]) is called.", 1);
    }

    public void setData(String[] xmlFilepaths) {
        this.mXmlInputs = xmlFilepaths;
        Logger.log(this, "FOProcessor.setData(String[]) is called.", 1);
    }

    public void setTemplate(String xslFile) {
        if (xslFile != null) {
            Logger.log(this, "FOProcessor.setTemplate(String) is called with '" + xslFile + "'.", 1);

            try {
                this.mXslInput = this.checkXdoDebugDir(this.getInputStream(xslFile));
            } catch (IOException var3) {
                this.mPreviousException = new XDOIOException(var3);
                Logger.log(this, "IOException is occurred in FOProcessor.setData(" + xslFile + ").", 4);
            }
        } else {
            this.setTemplate((InputStream)null);
        }

    }

    public void setTemplate(InputStream stream) {
        Logger.log(this, "FOProcessor.setTemplate(InputStream)is called.", 1);
        this.mXslInput = this.checkXdoDebugDir(stream);
    }

    public void setTemplate(Reader reader) {
        Logger.log(this, "FOProcessor.setTemplate(Reader)is called.", 1);
        this.mXslInput = this.checkXdoDebugDir(reader);
    }

    public void setTemplate(InputStream[] xslInput) {
        this.mXslInputs = xslInput;
        Logger.log(this, "FOProcessor.setTemplate(InputStream[])is called.", 1);
    }

    public void setTemplate(Reader[] xslReaders) {
        this.mXslInputs = xslReaders;
        Logger.log(this, "FOProcessor.setTemplate(Reader[])is called.", 1);
    }

    public void setTemplate(String[] xslFilepaths) {
        this.mXslInputs = xslFilepaths;
        Logger.log(this, "FOProcessor.setTemplate(String[])is called.", 1);
    }

    public void setXLIFF(String xliffFile) {
        if (xliffFile != null) {
            Logger.log(this, "FOProcessor.setXLIFF(String) is called with '" + xliffFile + "'.", 1);
            this.mXliffInput = xliffFile;
        } else {
            this.setXLIFF((InputStream)null);
        }

    }

    public void setXLIFF(InputStream stream) {
        Logger.log(this, "FOProcessor.setXLIFF(InputStream)is called.", 1);
        this.mXliffInput = stream;
    }

    public void setXLIFF(Reader reader) {
        Logger.log(this, "FOProcessor.setXLIFF(Reader)is called.", 1);
        this.mXliffInput = reader;
    }

    public void setXLIFF(InputStream[] xliffInput) {
        this.mXliffInputs = xliffInput;
        Logger.log(this, "FOProcessor.setXLIFF(InputStream[])is called.", 1);
    }

    public void setXLIFF(Reader[] xliffReaders) {
        this.mXliffInputs = xliffReaders;
        Logger.log(this, "FOProcessor.setXLIFF(Reader[])is called.", 1);
    }

    public void setXLIFF(String[] xliffFilepaths) {
        this.mXliffInputs = xliffFilepaths;
        Logger.log(this, "FOProcessor.setXLIFF(String[])is called.", 1);
    }

    public void setOutput(String outFile) {
        Logger.log(this, "FOProcessor.setOutput(String)is called with '" + outFile + "'.", 1);
        this.mOutStreamParam = outFile;
    }

    public void setOutput(OutputStream stream) {
        Logger.log(this, "FOProcessor.setOutput(OutputStream)is called.", 1);
        this.mOutStreamParam = stream;
    }

    public boolean setOutputFormat(byte formatID) {
        Logger.log(this, "FOProcessor.setOutputFormat(byte)is called with ID=" + formatID + ".", 1);
        this.mFormat = formatID;
        return formatID == 19 ? true : this.mFope.setOutputFormat(formatID);
    }

    /** @deprecated */
    public boolean process() {
        Logger.log(this, "FOProcessor.process() is called.", 1);

        try {
            this.generate();
            return true;
        } catch (Throwable var2) {
            Logger.log(this, var2, 4);
            return false;
        }
    }

    public void generate() throws XDOException {
        Properties runtimeProperties = this.mPropMan.getRuntimeProperties();
        Object foInput = null;
        int printMemoryStatusLevel = 5;
        OutputStream imgOutStream = null;
        File tmpPDF = null;
        String tmpDir = runtimeProperties.getProperty("system-temp-dir");
        if (tmpDir == null) {
            tmpDir = System.getProperty("java.io.tmpdir");
        }

        this.mFope.setMaxPageCounter(-1);
        if (this.mXdoDebugLevel != null) {
            runtimeProperties.put("xdo-debug-level", this.mXdoDebugLevel);
        }

        Logger.threadDebugInit(runtimeProperties);
        this.logMemoryStatus("Start", 1);
        if (this.mFormat < 0) {
            this.setOutputFormat((byte)1);
        }

        Logger.log(this, "FOProcessor.generate() called.", 1);
        addDerivedProperties(runtimeProperties, this.mFormat);
        if (this.mOutStreamParam != null) {
            try {
                this.mOutStream = (OutputStream)this.getOutput(this.mOutStreamParam);
            } catch (IOException var34) {
                this.mPreviousException = new XDOIOException(var34);
                Logger.log(this, "IOException is occurred in FOProcessor.setOutput(" + getObjDesc(this.mOutStreamParam) + ").", 4);
            } catch (Exception var35) {
                this.mPreviousException = new XDOException(var35);
                Logger.log(this, "IOException is occurred in FOProcessor.setOutput(" + getObjDesc(this.mOutStreamParam) + ").", 4);
            }
        }

        String xsltStaticOpt;
        if (this.mFormat >= 40 && this.mFormat <= 43) {
            xsltStaticOpt = runtimeProperties.getProperty("pdf2x-page-ranges");
            if (xsltStaticOpt != null && xsltStaticOpt.length() > 0) {
                PageRanges pr = new PageRanges(xsltStaticOpt);
                this.mFope.setMaxPageCounter(pr.getMaxPage());
            } else {
                this.mFope.setMaxPageCounter(1);
            }

            imgOutStream = this.mOutStream;

            try {
                tmpPDF = TmpFile.createTmpFile("xdo", ".tmp", tmpDir);
                this.mOutStream = (OutputStream)this.getOutput(tmpPDF.getAbsolutePath());
            } catch (IOException var33) {
                this.mPreviousException = new XDOIOException(var33);
                Logger.log(this, "IOException is occurred in TmpFile.createTmpFile(\"xdo\", \".tmp\", \"" + tmpDir + "\")", 4);
            }
        }

        String strStatus;
        if (this.mXmlInput != null) {
            try {
                this.mXmlInput = this.getXMLInput(this.mXmlInput);
                xsltStaticOpt = runtimeProperties.getProperty("xslt-xpath-optimization", "false");
                if ("true".equalsIgnoreCase(xsltStaticOpt)) {
                    File xdoDataFile = TmpFile.createTmpFile("xdo-data", ".xml", tmpDir);
                    strStatus = xdoDataFile.getCanonicalPath();
                    this.xmlDataSave(this.mXmlInput, xdoDataFile);
                    DocumentParser docParser = new DocumentParser();
                    docParser.parse(xdoDataFile, (ErrorHandler)null);
                    if (docParser.getStatus()) {
                        DataModel dmodel = docParser.getDataModel();
                        ThreadContext.contextPut("DataModel", dmodel);
                    }

                    this.mXmlInput = this.getInputStream(strStatus);
                    this.mToBeClosed.addElement(xdoDataFile);
                }
            } catch (IOException var31) {
                this.mPreviousException = new XDOIOException(var31);
                Logger.log(this, "IOException is occurred in FOProcessor.setData(" + getObjDesc(this.mXmlInput) + ").", 4);
            } catch (Exception var32) {
                this.mPreviousException = new XDOException(var32);
                Logger.log(this, "IOException is occurred in FOProcessor.setData(" + getObjDesc(this.mXmlInput) + ").", 4);
            }
        }

        if (this.mXslInput != null) {
            try {
                this.mXslInput = this.getXSLInput(this.mXslInput);
            } catch (IOException var29) {
                this.mPreviousException = new XDOIOException(var29);
                Logger.log(this, "IOException is occurred in FOProcessor.setTemplate(" + getObjDesc(this.mXslInput) + ")", 4);
            } catch (Exception var30) {
                this.mPreviousException = new XDOException(var30);
                Logger.log(this, "IOException is occurred in FOProcessor.setTemplate(" + getObjDesc(this.mXslInput) + ")", 4);
            }
        }

        if (this.mXliffInput != null) {
            try {
                this.mXliffInput = this.getXLIFFInput(this.mXliffInput);
            } catch (IOException var27) {
                this.mPreviousException = new XDOIOException(var27);
                Logger.log(this, "IOException is occurred in FOProcessor.setXLIFF(" + getObjDesc(this.mXliffInput) + ").", 4);
            } catch (Exception var28) {
                this.mPreviousException = new XDOException(var28);
                Logger.log(this, "IOException is occurred in FOProcessor.setXLIFF(" + getObjDesc(this.mXliffInput) + ").", 4);
            }
        }

        if (this.mPreviousException != null) {
            this.clearInputs(foInput);
            XDOException e = this.mPreviousException;
            this.mPreviousException = null;
            Logger.log(this, e, 4);
            throw e;
        } else {
            try {
                if (this.mXmlInput != null && this.mXslInput != null) {
                    foInput = this.createFO(this.mXmlInput, this.mXslInput, this.mXliffInput, runtimeProperties, false);
                } else if (this.mXmlInput != null && this.mXslInput == null) {
                    foInput = this.mXmlInput;
                } else {
                    if (this.mXmlInputs == null || this.mXmlInputs.length <= 0 || this.mXslInputs == null || this.mXslInputs.length <= 0) {
                        this.clearInputs(foInput);
                        Logger.log(this, "Invalid input combination.", 5);
                        throw new XDOException("Invalid input combination.");
                    }

                    foInput = this.processMultipleInputs(runtimeProperties);
                }

                if (this.mFormat == 19) {
                    int i;
                    if (foInput instanceof InputStream) {
                        for(i = ((InputStream)foInput).read(); i != -1; i = ((InputStream)foInput).read()) {
                            this.mOutStream.write(i);
                        }
                    } else if (foInput instanceof Reader) {
                        for(i = ((Reader)foInput).read(); i != -1; i = ((Reader)foInput).read()) {
                            this.mOutStream.write(i);
                        }
                    }
                } else {
                    if (foInput instanceof InputStream) {
                        this.mFope.setXSLFO((InputStream)foInput);
                    } else {
                        if (!(foInput instanceof Reader)) {
                            this.clearInputs(foInput);
                            Logger.log(this, "foInput is not valid.", 5);
                            throw new XDOException("Invalid input.");
                        }

                        this.mFope.setXSLFO((Reader)foInput);
                    }

                    this.mFope.setProperties(runtimeProperties);
                    this.mFope.setLocale(this.mLocale);
                    this.mFope.setOutput(this.mOutStream);
                    this.mFope.setOptimizedSections(this.mOptimizedSections);
                    Logger.log(this, "Calling FOProcessingEngine.process()", 1);
                    long t1 = System.currentTimeMillis();
                    this.mFope.process();
                    Logger.log(this, "FO+Gen time used: " + (System.currentTimeMillis() - t1) + " msecs", 2);
                    if (imgOutStream != null) {
                        t1 = System.currentTimeMillis();
                        this.closeObject(this.mOutStream);
                        this.mOutStream = imgOutStream;
                        PDF2Image pdf2img = new PDF2Image(tmpPDF.getAbsolutePath());
                        pdf2img.setConfig(runtimeProperties);
                        pdf2img.convertAndSavePage(0, this.mapId2ImageString(this.mFormat), imgOutStream);
                        imgOutStream.flush();
                        pdf2img.close();
                        this.mToBeClosed.addElement(tmpPDF);
                        Logger.log(this, "PDF2image time used: " + (System.currentTimeMillis() - t1) + " msecs", 2);
                    }

                    strStatus = this.mFope.getReturnStatus((String)null);
                    if (strStatus != null) {
                        Logger.log(this, "Completion status : '" + strStatus + "'.", 2);
                    }
                }

                printMemoryStatusLevel = 1;
            } catch (IOException var36) {
                throw new XDOIOException(var36);
            } catch (XDOException var37) {
                Logger.log(this, var37);
                throw var37;
            } catch (RuntimeException var38) {
                Logger.log(this, var38);
                throw var38;
            } finally {
                this.clearInputs(foInput);
                this.logMemoryStatus("End", printMemoryStatusLevel);
                Logger.threadDebugClear();
            }

        }
    }

    public void cancelProcess() {
        this.mFope.setMaxPageCounter(1);
    }

    public OutputStream[] getPipeDataStream(int count) {
        PipeOutputStream[] oStream = null;
        if (count > 0) {
            InputStream[] iStream = new InputStream[count];
            oStream = new PipeOutputStream[count];

            for(int i = 0; i < count; ++i) {
                PipeOutputStream poStream = new PipeOutputStream(262144);
                oStream[i] = poStream;
                iStream[i] = poStream.getInputStream();
            }

            if (count == 1) {
                this.setData(iStream[0]);
            } else {
                this.setData(iStream);
            }
        }

        return oStream;
    }

    public void run() {
        try {
            this.mRunException = null;
            this.generate();
        } catch (Exception var4) {
            Exception ex = var4;
            this.mRunException = var4;
            if (this.mXmlInput != null) {
                PipeOutputStream.setException(this.mXmlInput, var4);
            } else if (this.mXmlInputs != null) {
                int n = this.mXmlInputs.length;

                for(int i = 0; i < n; ++i) {
                    PipeOutputStream.setException(this.mXmlInputs[i], ex);
                }
            }
        }

    }

    public Exception getRunException() {
        return this.mRunException;
    }

    public int getTotalPageNumber() {
        return this.mFope != null ? this.mFope.getTotalPageNumber() : -1;
    }

    public void setConfig(Properties prop) {
        this.mPropMan.setConfig(prop);
    }

    private void clearInputs(Object foInput) {
        Logger.log(this, "clearInputs(Object) is called.", 1);
        this.closeObject(foInput);
        int size = this.mToBeClosed.size();
        if (size > 0) {
            for(int i = 0; i < size; ++i) {
                this.closeObject(this.mToBeClosed.elementAt(i));
            }

            this.mToBeClosed.removeAllElements();
        }

        this.mXmlInput = null;
        this.mXmlInputs = null;
        this.mXslInput = null;
        this.mXslInputs = null;
        this.mOutStream = null;
        this.mOutStreamParam = null;
        this.mSessionTimestamp = null;
        this.mXmlCount = 0;
        this.mXslCount = 0;
        this.mFoCount = 0;
        Logger.log(this, "clearInputs(Object) done. All inputs are cleared.", 1);
    }

    private Object getXMLInput(Object xml) throws IOException {
        if (xml instanceof String) {
            xml = this.getInputStream((String)xml);
        }

        if (xml instanceof InputStream) {
            ++this.mXmlCount;
            Object log = this.logFile((InputStream)xml, "_fo_data_" + this.mXmlCount + "_", ".xml");
            if (log != xml) {
                xml = log;
                this.mToBeClosed.addElement(log);
            }

            return (InputStream)xml;
        } else if (xml instanceof Reader) {
            ++this.mXmlCount;
            Object log = this.logFile((Reader)xml, "_fo_data_" + this.mXmlCount + "_", ".xml");
            if (log != xml) {
                xml = log;
                this.mToBeClosed.addElement(log);
            }

            return (Reader)xml;
        } else {
            return null;
        }
    }

    private Object getXSLInput(Object xsl) throws IOException {
        if (xsl instanceof String) {
            xsl = this.getInputStream((String)xsl);
        }

        if (xsl instanceof InputStream) {
            this.mXslCount = Math.max(this.mXmlCount, this.mXslCount + 1);
            Object log = this.logFile((InputStream)xsl, "_fo_data_" + this.mXslCount + "_", ".xsl");
            if (log != xsl) {
                xsl = log;
                this.mToBeClosed.addElement(log);
            }

            return (InputStream)xsl;
        } else if (xsl instanceof Reader) {
            this.mXslCount = Math.max(this.mXmlCount, this.mXslCount + 1);
            Object log = this.logFile((Reader)xsl, "_fo_data_" + this.mXslCount + "_", ".xsl");
            if (log != xsl) {
                xsl = log;
                this.mToBeClosed.addElement(log);
            }

            return (Reader)xsl;
        } else {
            return null;
        }
    }

    private InputStream getMergedXSLInput(InputStream xsl) {
        this.mXslCount = Math.max(this.mXmlCount, this.mXslCount + 1);
        InputStream log = this.logFile(xsl, "_fo_xliff_" + this.mXslCount + "_", ".xsl");
        if (log != xsl) {
            xsl = log;
            this.mToBeClosed.addElement(log);
        }

        return xsl;
    }

    private Object getXLIFFInput(Object xliff) throws IOException {
        if (xliff instanceof String) {
            xliff = this.getInputStream((String)xliff);
        }

        if (xliff instanceof InputStream) {
            this.mXslCount = Math.max(this.mXmlCount, this.mXslCount + 1);
            Object log = this.logFile((InputStream)xliff, "_fo_xliff_" + this.mXslCount + "_", ".xml");
            if (log != xliff) {
                xliff = log;
                this.mToBeClosed.addElement(log);
            }

            return (InputStream)xliff;
        } else if (xliff instanceof Reader) {
            this.mXslCount = Math.max(this.mXmlCount, this.mXslCount + 1);
            Object log = this.logFile((Reader)xliff, "_fo_xliff_" + this.mXslCount + "_", ".xml");
            if (log != xliff) {
                xliff = log;
                this.mToBeClosed.addElement(log);
            }

            return (Reader)xliff;
        } else {
            return null;
        }
    }

    private Object getOutput(Object out) throws IOException {
        if (out instanceof String) {
            out = this.getOutputStream((String)out);
            this.mToBeClosed.addElement(out);
        }

        return this.logOutputStream((OutputStream)out, "_fo_out_", ".out");
    }

    private Object getFOInputStream(Object fo) {
        if (fo instanceof File) {
            this.mToBeClosed.addElement(fo);

            try {
                fo = new FileInputStream((File)fo);
            } catch (IOException var4) {
            }
        }

        if (fo instanceof InputStream) {
            this.mFoCount = Math.max(this.mXmlCount, this.mXslCount);
            Object log = this.logFile((InputStream)fo, "_fo_fo_" + this.mFoCount + "_", ".fo");
            if (log != fo) {
                fo = log;
                this.mToBeClosed.addElement(log);
            }

            return (InputStream)fo;
        } else if (fo instanceof Reader) {
            this.mFoCount = Math.max(this.mXmlCount, this.mXslCount);
            Object log = this.logFile((Reader)fo, "_fo_fo_" + this.mFoCount + "_", ".fo");
            if (log != fo) {
                fo = log;
                this.mToBeClosed.addElement(log);
            }

            return (Reader)fo;
        } else {
            return fo;
        }
    }

    private InputStream getInputStream(String filePath) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filePath), 131000);
        this.mToBeClosed.addElement(is);
        return is;
    }

    private OutputStream getOutputStream(String filePath) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(filePath), 262144);
    }

    private void closeOutputStream(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException var3) {
            }

        }
    }

    private void closeObject(Object toBeClose) {
        if (toBeClose instanceof InputStream) {
            this.closeInput((InputStream)toBeClose);
        } else if (toBeClose instanceof Reader) {
            this.closeInput((Reader)toBeClose);
        } else if (toBeClose instanceof OutputStream) {
            this.closeOutputStream((OutputStream)toBeClose);
        } else if (toBeClose instanceof File) {
            ((File)toBeClose).delete();
        }

    }

    private void closeInput(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException var3) {
            }

        }
    }

    private void closeInput(Reader in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException var3) {
            }

        }
    }

    private InputStream processMultipleInputs(Properties prop) throws IOException, XDOException {
        Logger.log(this, "processMultipleInputs() is called.", 1);
        Vector foInputs = new Vector();
        Vector toBeClosed = new Vector();
        Object xmlInput = null;
        Object xslInput = null;
        Object xliffInput = null;
        int len = Math.min(this.mXmlInputs.length, this.mXslInputs.length);

        for(int i = 0; i < len; ++i) {
            String target;
            if (this.mXmlInputs instanceof String[]) {
                target = (String)this.mXmlInputs[i];
                if (target == null) {
                    continue;
                }

                xmlInput = this.getXMLInput(target);
                toBeClosed.addElement(xmlInput);
            } else if (this.mXmlInputs instanceof InputStream[]) {
                xmlInput = this.getXMLInput(this.mXmlInputs[i]);
                if (xmlInput == null) {
                    continue;
                }
            } else if (this.mXmlInputs instanceof Reader[]) {
                xmlInput = this.getXMLInput(this.mXmlInputs[i]);
                if (xmlInput == null) {
                    continue;
                }
            }

            if (this.mXslInputs instanceof String[]) {
                target = (String)this.mXslInputs[i];
                if (target != null) {
                    xslInput = this.getXSLInput(target);
                    toBeClosed.addElement(xmlInput);
                } else {
                    xslInput = null;
                }
            } else if (this.mXslInputs instanceof InputStream[]) {
                xslInput = this.getXSLInput(this.mXslInputs[i]);
            } else if (this.mXslInputs instanceof Reader[]) {
                xslInput = this.getXSLInput(this.mXslInputs[i]);
            }

            if (this.mXliffInputs instanceof String[]) {
                target = (String)this.mXliffInputs[i];
                if (target != null) {
                    xliffInput = this.getXLIFFInput(target);
                    toBeClosed.addElement(xliffInput);
                } else {
                    xliffInput = null;
                }
            } else if (this.mXliffInputs instanceof InputStream[]) {
                xliffInput = this.getXLIFFInput(this.mXslInputs[i]);
            } else if (this.mXliffInputs instanceof Reader[]) {
                xliffInput = this.getXLIFFInput(this.mXslInputs[i]);
            }

            if (xmlInput != null && xslInput == null) {
                foInputs.addElement(xmlInput);
            } else {
                foInputs.addElement(this.createFO(xmlInput, xslInput, xliffInput, prop, true));
            }
        }

        InputStream foInput = FOUtility.mergeFOs(foInputs, prop);
        if (foInput == null) {
            throw new IOException("invalid inputs");
        } else {
            len = toBeClosed.size();

            for(int i = 0; i < len; ++i) {
                InputStream in = (InputStream)toBeClosed.elementAt(i);
                in.close();
            }

            InputStream result = this.logFile(foInput, "_fo_merged_", ".fo");
            if (result != foInput) {
                foInput.close();
                foInput = result;
            }

            return foInput;
        }
    }

    private InputStream createFO(Object xml, Object xsl, Object xliff, Properties runtimeProperties, boolean multiple) throws XDOException {
        FOUtility foUt = new FOUtility();
        Logger.log(this, "createFO(Object, Object) is called.", 1);
        if (xliff != null) {
            InputStream result = this.mergeTranslation(xsl, xliff);
            if (result == null) {
                Logger.log(this, "translation failed.  Continue.", 1);
            } else {
                xsl = this.getMergedXSLInput(result);
            }
        }

        boolean optimize = true;
        String val = runtimeProperties.getProperty("xslt-runtime-optimization", "true");
        if (val.equals("false")) {
            optimize = false;
        }

        if (optimize && this.mFormat != 19 && !multiple) {
            Object oldxsl = xsl;

            try {
                xsl = duplicateInput(xsl);
                if (oldxsl != xsl) {
                    if ("true".equalsIgnoreCase(runtimeProperties.getProperty("xslt-do-import", "false"))) {
                        ManualImport mi = new ManualImport(runtimeProperties, (URL)null);
                        xsl = mi.doManualImport(xsl);
                    }

                    SAXParser parser = new SAXParser();
                    parser.setPreserveWhitespace(false);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1048576);
                    this.mOptimizedSections = new XSLTSectionCollection(500);
                    XSLTOptimizer optimizer = new XSLTOptimizer(new SAXPrintHandler(baos), this.mOptimizedSections);
                    parser.setDocumentHandler(optimizer);
                    if (xsl instanceof InputStream) {
                        parser.parse((InputStream)xsl);
                    } else {
                        parser.parse((Reader)xsl);
                    }

                    this.mOptimizedSections = optimizer.getCollection();
                    xsl = new ByteArrayInputStream(baos.toByteArray());
                }
            } catch (Exception var15) {
                if (var15 instanceof SAXException) {
                    Logger.log(this, var15.getMessage(), 1);
                } else {
                    Logger.log(this, var15.getMessage(), 5);
                }

                this.mOptimizedSections = null;

                try {
                    if (oldxsl instanceof ByteArrayInputStream) {
                        ((ByteArrayInputStream)oldxsl).reset();
                    } else if (oldxsl instanceof StringReader) {
                        ((StringReader)oldxsl).reset();
                    }
                } catch (Exception var14) {
                }

                xsl = xsl;
            }
        }

        long ct = System.currentTimeMillis();
        foUt.setXml((Reader)null);
        foUt.setXsl((Reader)null);
        if (xml instanceof Reader) {
            foUt.setXml((Reader)xml);
        } else if (xml instanceof InputStream) {
            foUt.setXml((InputStream)xml);
        }

        if (xsl instanceof Reader) {
            foUt.setXsl((Reader)xsl);
        } else if (xsl instanceof InputStream) {
            foUt.setXsl((InputStream)xsl);
        }

        val = runtimeProperties.getProperty("fo-multi-threads", "false");
        boolean runMultiThreaded = val.equalsIgnoreCase("true") && !Logger.isDebugMode();
        File fo = null;
        if (!runMultiThreaded) {
            fo = getTmpFOFile(runtimeProperties);
        }

        InputStream result = foUt.generateFO(runtimeProperties, fo, runMultiThreaded);
        if (fo != null) {
            this.mToBeClosed.addElement(fo);
        }

        result = (InputStream)this.getFOInputStream(result);
        Logger.log(this, "XSL-T time used: " + (System.currentTimeMillis() - ct) + " msec", 2);
        return result;
    }

    private static File getTmpFOFile(Properties props) throws XDOException {
        File outFile = null;
        String tmpDir = props.getProperty("system-temp-dir");
        if (tmpDir == null) {
            return outFile;
        } else {
            try {
                outFile = TmpFile.createTmpFile("xdo", ".fo", tmpDir);
                return outFile;
            } catch (IOException var4) {
                throw new XDOIOException(var4);
            }
        }
    }

    private static Object duplicateInput(Object xsl) throws IOException {
        if (xsl instanceof InputStream) {
            return duplicateStream((InputStream)xsl);
        } else {
            return xsl instanceof Reader ? duplicateReader((Reader)xsl) : xsl;
        }
    }

    private static final ByteArrayInputStream duplicateStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = null;

        try {
            os = new ByteArrayOutputStream(1048576);
            byte[] buffer = new byte[16384];

            int i;
            while((i = is.read(buffer)) >= 0) {
                os.write(buffer, 0, i);
            }

            ByteArrayInputStream var4 = new ByteArrayInputStream(os.toByteArray());
            return var4;
        } catch (IOException var13) {
            throw var13;
        } finally {
            try {
                is.close();
            } catch (Exception var12) {
            }

        }
    }

    private static final Reader duplicateReader(Reader is) throws IOException {
        StringWriter os = null;

        try {
            os = new StringWriter();
            char[] buffer = new char[16384];

            int i;
            while((i = is.read(buffer)) >= 0) {
                os.write(buffer, 0, i);
            }

            StringReader var4 = new StringReader(os.toString());
            return var4;
        } catch (IOException var13) {
            throw var13;
        } finally {
            try {
                is.close();
            } catch (Exception var12) {
            }

        }
    }

    private InputStream mergeTranslation(Object xsl, Object xliff) {
        DOMParser xslDom = new DOMParser();
        DOMParser xliffDom = new DOMParser();

        try {
            if (xsl instanceof InputStream) {
                xslDom.parse((InputStream)xsl);
            } else if (xsl instanceof Reader) {
                xslDom.parse((Reader)xsl);
            }

            if (xliff instanceof InputStream) {
                xliffDom.parse((InputStream)xliff);
            } else if (xsl instanceof Reader) {
                xliffDom.parse((Reader)xliff);
            }

            XMLDocument xsldoc = xslDom.getDocument();
            XLIFFUtil.applyXLIFF(xsldoc, xliffDom.getDocument(), "EN", "United States");
            return FOUtility.toInputStream(xsldoc);
        } catch (Exception var6) {
            Logger.log(this, var6, 4);
            return null;
        }
    }

    private static final String getObjDesc(Object obj) {
        String name = "NULL";
        if (obj != null) {
            if (obj instanceof String) {
                name = "\"" + (String)obj + "\"";
            } else {
                String s = obj.getClass().getName();
                int idx = s.lastIndexOf(46);
                name = s.substring(idx + 1);
            }
        }

        return name;
    }

    private InputStream logFile(InputStream ins, String base, String ext) {
        if (this.mSessionTimestamp == null) {
            this.mSessionTimestamp = Logger.getTimeStampStr();
        }

        String prefix = "xdo_" + this.mSessionTimestamp + base;
        if (Logger.isDebugMode()) {
            Logger.log(this, "   Log file '" + prefix + ext + "' is created.", 1);
        }

        return Logger.logFile(ins, prefix, ext);
    }

    private Reader logFile(Reader reader, String base, String ext) {
        if (this.mSessionTimestamp == null) {
            this.mSessionTimestamp = Logger.getTimeStampStr();
        }

        String prefix = "xdo_" + this.mSessionTimestamp + base;
        if (Logger.isDebugMode()) {
            Logger.log(this, "   Log file '" + prefix + ext + "' is created.", 1);
        }

        return Logger.logFile(reader, prefix, ext);
    }

    private OutputStream logOutputStream(OutputStream out, String base, String ext) {
        if (this.mSessionTimestamp == null) {
            this.mSessionTimestamp = Logger.getTimeStampStr();
        }

        Object result;
        try {
            String fname = getLogFileName(this.mSessionTimestamp, base, ext);
            if (Logger.isDebugMode()) {
                Logger.log(this, "   Log file '" + fname + "' is created.", 1);
            }

            result = new LogOutputStream(out, fname);
            this.mToBeClosed.addElement(result);
        } catch (IOException var6) {
            Logger.log(this, var6, 4);
            result = out;
        }

        return (OutputStream)result;
    }

    private static final synchronized String getLogFileName(String time, String base, String ext) {
        ++sLogFileNameIndexNum;
        if (sLogFileNameIndexNum >= 2147483647) {
            sLogFileNameIndexNum = 1;
        }

        int ranNo = (int)(Math.random() * 2.147483647E9D + (double)sLogFileNameIndexNum) % 1048575;
        return "xdo_" + time + base + Integer.toHexString(ranNo) + ext;
    }

    private void logMemoryStatus(String msg, int level) {
        Runtime rt = Runtime.getRuntime();
        Logger.log(this, msg + " Memory: max=" + rt.maxMemory() / 1048576L + "MB, total=" + rt.totalMemory() / 1048576L + "MB, free=" + rt.freeMemory() / 1048576L + "MB", level);
    }

    private void xmlDataSave(Object xml, File file) throws IOException {
        if (xml instanceof Reader) {
            FileUtil.writeLargeFile((Reader)xml, file, this.mXmlDataEncoding);
        } else if (xml instanceof InputStream) {
            FileUtil.writeLargeFile((InputStream)xml, file);
        }

    }

    public static void main(String[] args) {
        oracle.xdo.template.FOProcessor processor = null;
        Properties prop = new Properties();
        String foInput = null;
        String xmlInput = null;
        String xslInput = null;
        String xliffInput = null;
        String docConf = null;
        String locale = null;
        Vector xmls = new Vector(3);
        Vector xsls = new Vector(3);
        Vector xliffs = new Vector(3);
        byte format = 1;
        String output = null;
        boolean debug = false;
        long startTime = 0L;

        for(int i = 0; i < args.length; ++i) {
            if (args[i].equals("-fo")) {
                ++i;
                foInput = args[i];
            } else if (args[i].equals("-xml")) {
                ++i;
                xmlInput = args[i];
            } else if (args[i].equals("-xsl")) {
                ++i;
                xslInput = args[i];
            } else if (args[i].equals("-xliff")) {
                ++i;
                xliffInput = args[i];
            } else if (args[i].equals("-xmls")) {
                ++i;
                xmls.addElement(args[i]);
            } else if (args[i].equals("-xsls")) {
                ++i;
                xsls.addElement(args[i]);
            } else if (args[i].equals("-xliffs")) {
                ++i;
                if (args[i].equalsIgnoreCase("null")) {
                    xliffs.addElement((Object)null);
                } else {
                    xliffs.addElement(args[i]);
                }
            } else if (args[i].equals("-fos")) {
                ++i;
                xmls.addElement(args[i]);
                xsls.addElement((Object)null);
                xliffs.addElement((Object)null);
            } else if (args[i].equals("-pdf")) {
                ++i;
                output = args[i];
            } else if (args[i].equals("-pdfz")) {
                ++i;
                output = args[i];
                format = 38;
            } else if (args[i].equals("-rtf")) {
                ++i;
                output = args[i];
                format = 2;
            } else if (args[i].equals("-excel")) {
                ++i;
                output = args[i];
                format = 4;
            } else if (args[i].equals("-xlsx")) {
                ++i;
                output = args[i];
                format = 36;
            } else if (args[i].equals("-html")) {
                ++i;
                output = args[i];
                format = 3;
            } else if (args[i].equals("-mhtml")) {
                ++i;
                output = args[i];
                format = 35;
            } else if (args[i].equals("-pptmht")) {
                ++i;
                output = args[i];
                format = 25;
            } else if (args[i].equals("-pptx")) {
                ++i;
                output = args[i];
                format = 26;
            } else if (args[i].equals("-png")) {
                ++i;
                output = args[i];
                format = 40;
            } else if (args[i].equals("-jpg")) {
                ++i;
                output = args[i];
                format = 41;
            } else if (args[i].equals("-gif")) {
                ++i;
                output = args[i];
                format = 43;
            } else if (args[i].equals("-bmp")) {
                ++i;
                output = args[i];
                format = 42;
            } else if (args[i].equals("-docconf")) {
                ++i;
                docConf = args[i];
            } else if (args[i].equals("-locale")) {
                ++i;
                locale = args[i];
            } else if (args[i].equals("-d")) {
                debug = true;
            } else if (args[i].equals("-h")) {
                usage();
                System.exit(0);
            } else if (args[i].equals("-property")) {
                prop.put(args[i + 1], args[i + 2]);
                i += 2;
            } else if (args[i].equals("-version")) {
                Logger.log("Oracle BI Publisher 11.1.1.4.0", 5);
                System.exit(0);
            } else if (args[i].equals("-shortversion")) {
                Logger.log("11.1.1.4.0", 5);
                System.exit(0);
            }
        }

        if (debug) {
            startTime = System.currentTimeMillis();
            Logger.setLevel(1);
            Logger.log("Oracle BI Publisher 11.1.1.4.0", 2);
        }

        processor = new oracle.xdo.template.FOProcessor();
        if (foInput != null) {
            processor.setData(foInput);
        } else if (xmlInput != null) {
            processor.setData(xmlInput);
            processor.setTemplate(xslInput);
            processor.setXLIFF(xliffInput);
        } else {
            String[] xmlIn = new String[xmls.size()];
            String[] xslIn = new String[xsls.size()];
            String[] xliffIn = xliffs.size() > 0 ? new String[xliffs.size()] : null;

            int i;
            for(i = 0; i < xmls.size(); ++i) {
                Array.set(xmlIn, i, (String)xmls.elementAt(i));
            }

            for(i = 0; i < xsls.size(); ++i) {
                Array.set(xslIn, i, (String)xsls.elementAt(i));
            }

            for(i = 0; i < xliffs.size(); ++i) {
                Array.set(xliffIn, i, (String)xliffs.elementAt(i));
            }

            processor.setData(xmlIn);
            processor.setTemplate(xslIn);
            processor.setXLIFF(xliffIn);
        }

        if (locale != null) {
            processor.setLocale(locale);
        }

        processor.setConfig(docConf);
        processor.setConfig(prop);
        if (output != null) {
            processor.setOutput(output);
            if (!processor.setOutputFormat(format)) {
                Logger.log(processor, "Initialize generator failed.", 4);
                usage();
                System.exit(1);
            }

            if (debug) {
                Logger.setLevel(1);
            }

            try {
                processor.generate();
            } catch (Throwable var22) {
                Logger.log(processor, var22);
            }

            if (debug) {
                long end = System.currentTimeMillis();
                Logger.log(processor, "FOProcessor total time used: " + (end - startTime) + " msec", 2);
            }

            System.exit(0);
        } else {
            processor.setOutputFormat((byte)0);
            processor.setConfig(prop);

            try {
                processor.generate();
            } catch (Throwable var21) {
                Logger.log(processor, var21);
            }
        }

    }

    public String getCompletionStatus(String name) {
        String status = this.mFope.getReturnStatus(name);
        return status;
    }

    private static void addDerivedProperties(Properties props, byte format) {
        String chartCType;
        switch(format) {
            case 1:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 37:
            case 38:
            default:
                break;
            case 2:
                props.put("xslt._XDOCHARTTYPE", "'image/png'");
                break;
            case 3:
            case 35:
                chartCType = "xslt._XDOCHARTTYPE";
                if (props.getProperty(chartCType) == null) {
                    props.put(chartCType, "'image/png'");
                }
                break;
            case 4:
            case 5:
                props.put("xslt._XDOCHARTTYPE", "'image/png'");
                props.put("xslt._XDOOUTPUTFORMAT", "'text/html'");
                break;
            case 25:
                props.put("xslt._XDOCHARTTYPE", "'image/png'");
                break;
            case 26:
                chartCType = props.getProperty("pptx-native-chart", "true");
                if (chartCType != null && chartCType.equalsIgnoreCase("true")) {
                    props.put("xslt._XDOCHARTTYPE", "'image/dvt+xml'");
                } else {
                    props.put("xslt._XDOCHARTTYPE", "'image/png'");
                }
                break;
            case 36:
                props.put("xslt._XDOCHARTTYPE", "'image/png'");
                props.put("xslt._XDOOUTPUTFORMAT", "'application/xlsx'");
        }

    }

    private InputStream checkXdoDebugDir(InputStream isp) {
        InputStream newIs = isp;
        if (isp != null && this.mXdoDebugLevel == null) {
            if (!isp.markSupported()) {
                newIs = new BufferedInputStream(isp, 32768);
                this.mToBeClosed.addElement(newIs);
            }

            byte[] buf = new byte[512];

            try {
                ((InputStream)newIs).mark(buf.length + 16);
                int count = ((InputStream)newIs).read(buf);
                if (count >= 256) {
                    String txt = new String(buf, 0, count);
                    int pos = txt.indexOf("xdo-debug-level");
                    AttributeParser ap;
                    if (pos >= 0) {
                        ap = new AttributeParser(txt, pos);
                        this.mXdoDebugLevel = ap.getValue();
                    }

                    pos = txt.indexOf("encoding");
                    if (pos >= 0) {
                        ap = new AttributeParser(txt, pos);
                        this.mXmlDataEncoding = ap.getValue();
                    }
                }
            } catch (IOException var17) {
            } finally {
                try {
                    ((InputStream)newIs).reset();
                } catch (IOException var16) {
                }

            }

            return (InputStream)newIs;
        } else {
            return isp;
        }
    }

    private Reader checkXdoDebugDir(Reader isp) {
        Reader newIs = isp;
        if (isp != null && this.mXdoDebugLevel == null) {
            if (!isp.markSupported()) {
                newIs = new BufferedReader(isp, 32768);
                this.mToBeClosed.addElement(newIs);
            }

            char[] buf = new char[512];

            try {
                ((Reader)newIs).mark(buf.length + 16);
                int count = ((Reader)newIs).read(buf);
                if (count >= 256) {
                    String txt = new String(buf, 0, count);
                    int pos = txt.indexOf("xdo-debug-level");
                    AttributeParser ap;
                    if (pos >= 0) {
                        ap = new AttributeParser(txt, pos);
                        this.mXdoDebugLevel = ap.getValue();
                    }

                    pos = txt.indexOf("encoding");
                    if (pos >= 0) {
                        ap = new AttributeParser(txt, pos);
                        this.mXmlDataEncoding = ap.getValue();
                    }
                }
            } catch (IOException var17) {
            } finally {
                try {
                    ((Reader)newIs).reset();
                } catch (IOException var16) {
                }

            }

            return (Reader)newIs;
        } else {
            return isp;
        }
    }

    private String mapId2ImageString(byte id) {
        switch(id) {
            case 40:
                return "png";
            case 41:
                return "jpg";
            case 42:
                return "bmp";
            case 43:
                return "gif";
            default:
                return null;
        }
    }

    private static void usage() {
        Logger.log("Usage: java oracle.xdo.template.FOProcessor [-d] -fo FOInput [Output]", 5);
        Logger.log("       java oracle.xdo.template.FOProcessor [-d] -xml XMLInput -xsl XSLInput [Output]", 5);
        Logger.log("       java oracle.xdo.template.FOProcessor [-d] -xmls XMLInput1 -xsls XSLInput1 -xmls XMLInput2 -xsls XSLInput2 [Output]", 5);
        Logger.log("       java oracle.xdo.template.FOProcessor [-d] -fos FOInput1 -fos FOInput2 -xmls XMLInput1 -xsls XSLInput1 [Output]", 5);
        Logger.log("", 5);
        Logger.log("       -fo XSL-FO Input Filepath", 5);
        Logger.log("       -xml XML Input Filepath", 5);
        Logger.log("       -xsl XSL Input Filepath", 5);
        Logger.log("       -xliff XLIFF Input Filepath", 5);
        Logger.log("       -xmls XML Input Filepath (one of multiple input files)", 5);
        Logger.log("       -xsls XSL Input Filepath (one of multiple input files)", 5);
        Logger.log("       -xliffs XLIFF Input Filepath (one of multiple input files, set text \"null\" (without quote) for no translation file..)", 5);
        Logger.log("       -fos XSL-FO Input Filepath (one of multiple input files)", 5);
        Logger.log("       -docconf Document level confiugarion Filepath", 5);
        Logger.log("       -property Key Value", 5);
        Logger.log("       -version show XDO version string", 5);
        Logger.log("       -shortversion show XDO short version string", 5);
        Logger.log("       -locale Local String", 5);
        Logger.log("       -d  Debug Output", 5);
        Logger.log("       -h  Show this message", 5);
        Logger.log("       Output can be:", 5);
        Logger.log("       -pdf PDF Output Filepath", 5);
        Logger.log("       -pdfz Zipped PDF Output Filepath", 5);
        Logger.log("       -rtf RTF Output Filepath", 5);
        Logger.log("       -excel Excel Output Filepath", 5);
        Logger.log("       -html HTML Output Filepath", 5);
        Logger.log("       -mhtml MHTML Output Filepath", 5);
        Logger.log("       -pptmht PowerPoint MHTML Filepath", 5);
        Logger.log("       -pptx PowerPoint 2007 Filepath", 5);
        Logger.log("       -jpg image-file-path", 5);
        Logger.log("       -png image-file-path", 5);
        Logger.log("       -gif image-file-path", 5);
        Logger.log("       -bmp image-file-path", 5);
    }
}
