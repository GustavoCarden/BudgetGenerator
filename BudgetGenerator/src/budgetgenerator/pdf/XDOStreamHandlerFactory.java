package budgetgenerator.pdf;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

class XDOStreamHandlerFactory implements URLStreamHandlerFactory {

    public static final String RCS_ID = "$Header$";
    public static final String XDO_PROTOCOL = "xdo";
    public static final String FND_PROTOCOL = "fnd";
    public static final String BLOB_PROTOCOL = "blob";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String RTF2XSL_PROTOCOL = "rtf2xsl";
    public static final String PSXMLP_PROTOCOL = "psxmlp";
    private static boolean s_registerFlag = false;
    private boolean mRequireHttps;

    public XDOStreamHandlerFactory() {
    }

    public XDOStreamHandlerFactory(boolean pRequireHttps) {
        this.mRequireHttps = pRequireHttps;
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        Class c = null;
        Object o = null;

        try {
            if ("xdo".equalsIgnoreCase(protocol)) {
                c = Class.forName("oracle.apps.xdo.oa.schema.server.TemplateStreamHandler");
                o = c.newInstance();
                return (URLStreamHandler)o;
            }

            if ("fnd".equalsIgnoreCase(protocol)) {
                c = Class.forName("oracle.apps.xdo.oa.schema.server.FndLobsStreamHandler");
                o = c.newInstance();
                return (URLStreamHandler)o;
            }

            if ("rtf2xsl".equalsIgnoreCase(protocol)) {
                c = Class.forName("oracle.xdo.template.rtf.net.MemStreamHandler");
                o = c.newInstance();
                return (URLStreamHandler)o;
            }

            if ("blob".equalsIgnoreCase(protocol)) {
                c = Class.forName("oracle.apps.xdo.oa.schema.server.BlobStreamHandler");
                o = c.newInstance();
                return (URLStreamHandler)o;
            }

            if (this.mRequireHttps && "https".equalsIgnoreCase(protocol)) {
                c = Class.forName("oracle.xdo.common.security.https.HttpsURLStreamHandler");
                o = c.newInstance();
                return (URLStreamHandler)o;
            }

            if ("psxmlp".equalsIgnoreCase(protocol)) {
                c = Class.forName("com.peoplesoft.pt.xmlpublisher.PSStreamHandler");
                o = c.newInstance();
                return (URLStreamHandler)o;
            }
        } catch (ClassNotFoundException var5) {
        } catch (InstantiationException var6) {
        } catch (IllegalAccessException var7) {
        }

        return null;
    }

    public static synchronized void registerNewProtocols() {
        if (!s_registerFlag) {
            boolean requireHttps = false;

            try {
                new URL("https://oracle.com/");
            } catch (MalformedURLException var7) {
                requireHttps = true;
            }

            /*boolean java2Compatible = false;
            String javaver = System.getProperty("java.version");
            if (javaver != null) {
                if (javaver.substring(0, javaver.indexOf(46)).equals("HP-UX Java C")) {
                    javaver = javaver.substring(javaver.indexOf(46, 2) + 1);
                    javaver = javaver.substring(1, javaver.indexOf(32));
                }

                int minor = javaver.indexOf(46, 2);
                if (minor > 0) {
                    javaver = javaver.substring(0, minor);
                }

                java2Compatible = Double.valueOf(javaver) >= 1.2D;
            }

            if (java2Compatible) {
                String protocolProperty = "java.protocol.handler.pkgs";
                String existingValue = System.getProperty(protocolProperty);
                String protocolPkg = "oracle.xdo.common.net.protocol|oracle.xdo.servlet.resources";
                if (requireHttps) {
                    protocolPkg = protocolPkg + "|oracle.xdo.common.security";
                }

                if (existingValue != null) {
                    if (existingValue.indexOf(protocolPkg) < 0) {
                        protocolPkg = protocolPkg + "|" + existingValue;
                    } else {
                        protocolPkg = existingValue;
                    }
                }

                System.getProperties().put(protocolProperty, protocolPkg);
            } else {
                try {
                    URL.setURLStreamHandlerFactory(new oracle.xdo.common.net.XDOStreamHandlerFactory(requireHttps));
                } catch (Error var6) {
                }
            }*/

            s_registerFlag = true;
        }
    }
}