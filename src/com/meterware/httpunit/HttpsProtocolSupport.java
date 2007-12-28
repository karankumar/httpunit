package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2003-2007, Russell Gold
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions
* of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*******************************************************************************************************************/
import java.security.Provider;
import java.security.Security;


/**
 * Encapsulates support for the HTTPS protocol.
 *
 * @author <a href="mailto:russgold@httpunit.org">Russell Gold</a>
 **/
public abstract class HttpsProtocolSupport {

    /** The name of the system parameter used by java.net to locate protocol handlers. **/
    private final static String PROTOCOL_HANDLER_PKGS  = "java.protocol.handler.pkgs";
        
       
    // Sun Microsystems:
    private final static String SunJSSE_PROVIDER_CLASS    = "com.sun.net.ssl.internal.ssl.Provider";
    // 741145: "sun.net.www.protocol.https";
    private final static String SunJSSE_PROVIDER_CLASS2   = "sun.net.www.protocol.https";    
    private final static String SunSSL_PROTOCOL_HANDLER   = "com.sun.net.ssl.internal.www.protocol";
    
    // IBM WebSphere
    // 	both ibm packages are inside ibmjsseprovider.jar that comes with WebSphere
    private final static String IBMJSSE_PROVIDER_CLASS    = "com.ibm.jsse.IBMJSSEProvider";
    private final static String IBMSSL_PROTOCOL_HANDLER   = "com.ibm.net.ssl.www.protocol";

    /** The name of the JSSE class which provides support for SSL. **/
    private static String JSSE_PROVIDER_CLASS=SunJSSE_PROVIDER_CLASS;
    /** The name of the JSSE class which supports the https protocol. **/
    private static String SSL_PROTOCOL_HANDLER   = SunSSL_PROTOCOL_HANDLER ;

    private static Class _httpsProviderClass;

    private static boolean _httpsSupportVerified;

    private static boolean _httpsProtocolSupportEnabled;
    
    /**
     * use the given SSL providers
     * @param className
     * @param handlerName
     */
    public static void useProvider(String className,String handlerName) {
    	JSSE_PROVIDER_CLASS  =className;
    	SSL_PROTOCOL_HANDLER =handlerName;
    }
    
    /**
     * use the IBM WebShpere handlers
     */
    public static void useIBM() {
    	useProvider(IBMJSSE_PROVIDER_CLASS,IBMSSL_PROTOCOL_HANDLER);
    }

    /**
     * Returns true if the JSSE extension is installed.
     */
    static boolean hasHttpsSupport() {
        if (!_httpsSupportVerified) {
            try {
                getHttpsProviderClass();
            } catch (ClassNotFoundException e) {
            }
            _httpsSupportVerified = true;
        }
        return _httpsProviderClass != null;
    }


    /**
     * Attempts to register the JSSE extension if it is not already registered. Will throw an exception if unable to
     * register the extension.
     */
    static void verifyProtocolSupport( String protocol ) {
        if (protocol.equalsIgnoreCase( "http" )) {
            return;
        } else if (protocol.equalsIgnoreCase( "https" )) {
            validateHttpsProtocolSupport();
        }
    }


    private static void validateHttpsProtocolSupport() {
        if (!_httpsProtocolSupportEnabled) {
            verifyHttpsSupport();
            _httpsProtocolSupportEnabled = true;
        }
    }

    private static void verifyHttpsSupport() {
        try {
            Class providerClass = getHttpsProviderClass();
            if (!hasProvider( providerClass )) Security.addProvider( (Provider) providerClass.newInstance() );
            registerSSLProtocolHandler();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException( "https support requires the Java Secure Sockets Extension. See http://java.sun.com/products/jsse" );
        } catch (Throwable e) {
            throw new RuntimeException( "Unable to enable https support. Make sure that you have installed JSSE " +
                                        "as described in http://java.sun.com/products/jsse/install.html: " + e );
        }
    }


    private static Class getHttpsProviderClass() throws ClassNotFoundException {
        if (_httpsProviderClass == null) {
            _httpsProviderClass = Class.forName( JSSE_PROVIDER_CLASS );
        }
        return _httpsProviderClass;
    }


    private static boolean hasProvider( Class providerClass ) {
        Provider[] list = Security.getProviders();
        for (int i = 0; i < list.length; i++) {
            if (list[i].getClass().equals( providerClass )) return true;
        }
        return false;
    }


    private static void registerSSLProtocolHandler() {
        String list = System.getProperty( PROTOCOL_HANDLER_PKGS );
        if (list == null || list.length() == 0) {
            System.setProperty( PROTOCOL_HANDLER_PKGS, SSL_PROTOCOL_HANDLER );
        } else if (list.indexOf( SSL_PROTOCOL_HANDLER ) < 0) {
            System.setProperty( PROTOCOL_HANDLER_PKGS, SSL_PROTOCOL_HANDLER + " | " + list );
        }
    }
}
