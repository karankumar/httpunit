package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000-2001, Russell Gold
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
import java.io.IOException;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.xml.sax.SAXException;


/**
 * The context for a series of web requests. This class manages cookies used to maintain
 * session context, computes relative URLs, and generally emulates the browser behavior
 * needed to build an automated test of a web site.
 *
 * @author Russell Gold
 * @author Jan Ohrstrom
 * @author Seth Ladd
 **/
abstract
public class WebClient {


    /**
     * Submits a GET method request and returns a response.
     * @exception SAXException thrown if there is an error parsing the retrieved page
     **/
    public WebResponse getResponse( String urlString ) throws MalformedURLException, IOException, SAXException {
        return getResponse( new GetMethodWebRequest( urlString ) );
    }


    /**
     * Submits a web request and returns a response, using all state developed so far as stored in
     * cookies as requested by the server.
     * @exception SAXException thrown if there is an error parsing the retrieved page
     **/
    public WebResponse getResponse( WebRequest request ) throws MalformedURLException, IOException, SAXException {
        if (request.getURLString().startsWith( "about:" )) return WebResponse.BLANK_RESPONSE;

        WebResponse response = newResponse( request );
        updateClient( response );
        return getFrameContents( request.getTarget() );
    }


    /**
     * Returns the name of the currently active frames.
     **/
    public String[] getFrameNames() {
        Vector names = new Vector();
        for (Enumeration e = _frameContents.keys(); e.hasMoreElements();) {
            names.addElement( e.nextElement() );
        }

        String[] result = new String[ names.size() ];
        names.copyInto( result );
        return result;
    }


    /**
     * Returns the response associated with the specified frame name.
     **/
    public WebResponse getFrameContents( String frameName ) {
        WebResponse response = (WebResponse) _frameContents.get( frameName );
        if (response == null) throw new NoSuchFrameException( frameName );
        return response;
    }


    /**
     * Defines a cookie to be sent to the server on every request.
     **/
    public void addCookie( String name, String value ) {
        _cookies.put( name, value );
    }


    /**
     * Returns the name of all the active cookies which will be sent to the server.
     **/
    public String[] getCookieNames() {
        String[] names = new String[ _cookies.size() ];
        int i = 0;
        for (Enumeration e = _cookies.keys(); e.hasMoreElements();) {
            names[i++] = (String) e.nextElement();
        }
        return names;
    }


    /**
     * Returns the value of the specified cookie.
     **/
    public String getCookieValue( String name ) {
        return (String) _cookies.get( name );
    }


    /**
     * Specifies the user agent identification. Used to trigger browser-specific server behavior.
     **/
    public void setUserAgent( String userAgent ) {
	    setHeaderField( "User-Agent", userAgent );
    }


    /**
     * Returns the current user agent setting.
     **/
    public String getUserAgent() {
	    return getHeaderField( "User-Agent" );
    }


    /**
     * Sets a username and password for a basic authentication scheme.
     **/
    public void setAuthorization( String userName, String password ) {
        setHeaderField( "Authorization", "Basic " + Base64.encode( userName + ':' + password ) );
    }


    /**
     * Sets the value for a header field to be sent with all requests. If the value set is null,
     * removes the header from those to be sent.
     **/
    public void setHeaderField( String fieldName, String fieldValue ) {
        _headers.put( fieldName, fieldValue );
    }


    /**
     * Returns the value for the header field with the specified name. This method will ignore the case of the field name.
     */
    public String getHeaderField( String fieldName ) {
        return (String) _headers.get( fieldName );
    }


//------------------------------------------ protected members -----------------------------------


    /**
     * Creates a web response object which represents the response to the specified web request.
     **/
    abstract
    protected WebResponse newResponse( WebRequest request ) throws MalformedURLException, IOException;


    /**
     * Writes the message body for the request.
     **/
    final protected void writeMessageBody( WebRequest request, OutputStream stream ) throws IOException {
        request.writeMessageBody( stream );
    }


    /**
     * Returns the value of the cookie header, or null if none is defined.
     **/
    protected String getCookieHeaderField() {
        String names[] = getCookieNames();
        if (names.length == 0) return null;

        StringBuffer sb = new StringBuffer(HttpUnitUtils.DEFAULT_BUFFER_SIZE);
        for (int i = 0; i < names.length; i++) {
            if (i != 0) sb.append( "; " );
            String name = names[i];
            sb.append( name ).append( '=' ).append( getCookieValue( name ) );
        }
        return sb.toString();
    }


    /**
     * Returns the value of all current header fields.
     **/
    protected Dictionary getHeaderFields() {
        Hashtable result = (Hashtable) _headers.clone();
        if (getCookieHeaderField() != null) result.put( "Cookie", getCookieHeaderField() );
        return result;
    }


    /**
     * Updates this web client based on a received response. This includes updating
     * cookies and frames.
     **/
    final
    protected void updateClient( WebResponse response ) throws MalformedURLException, IOException, SAXException {
        updateCookies( response );
        validateHeaders( response );
        if (HttpUnitOptions.getAutoRefresh() && response.getRefreshRequest() != null) {
            getResponse( response.getRefreshRequest() );
        } else if (!HttpUnitOptions.getAutoRedirect() || response.getHeaderField( "Location" ) == null) {
            updateFrames( response );
        } else {
            delay( HttpUnitOptions.getRedirectDelay() );
            getResponse( new RedirectWebRequest( response ) );
        }
    }


//------------------------------------------ private members -------------------------------------


    /** The currently defined cookies. **/
    private Hashtable _cookies = new Hashtable();


    /** A map of frame names to current contents. **/
    private Hashtable _frameContents = new Hashtable();


    /** A map of frame names to frames nested within them. **/
    private Hashtable _subFrames = new Hashtable();


    /** A map of header names to values. **/
    private HeaderDictionary _headers = new HeaderDictionary();


    /**
     * Examines the headers in the response and throws an exception if appropriate.
     **/
    private void validateHeaders( WebResponse response ) throws HttpException, IOException {
        if (response.getHeaderField( "WWW-Authenticate" ) != null) {
            throw new AuthorizationRequiredException( response.getHeaderField( "WWW-Authenticate" ) );
        } else if (HttpUnitOptions.getExceptionsThrownOnErrorStatus()) {
            if (response.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                throw new HttpInternalErrorException( response.getURL() );
            } else if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new HttpNotFoundException( response.getURL() );
            } else if (response.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new HttpException( response.getResponseCode(), response.getResponseMessage(), response.getURL() );
            }
        }
    }


    /**
     * Updates the cookies maintained in this client based on new cookies requested by the server.
     **/
    private void updateCookies( WebResponse response ) {
        String[] names = response.getNewCookieNames();
        for (int i = 0; i < names.length; i++) {
            addCookie( names[i], response.getNewCookieValue( names[i] ) );
        }
    }


    /**
     * Delays the specified amount of time.
     **/
    private void delay( int numMilliseconds ) {
        if (numMilliseconds == 0) return;
        try {
            Thread.sleep( numMilliseconds );
        } catch (InterruptedException e) {
            // ignore the exception
        }
    }

    private void updateFrames( WebResponse response ) throws MalformedURLException, IOException, SAXException {
        removeSubFrames( response.getTarget() );
        _frameContents.put( response.getTarget(), response );

        if (response.isHTML()) {
            createSubFrames( response.getTarget(), response.getFrameNames() );
            WebRequest[] requests = response.getFrameRequests();
            for (int i = 0; i < requests.length; i++) getResponse( requests[i] );
        }
    }


    private void createSubFrames( String targetName, String[] frameNames ) {
        _subFrames.put( targetName, frameNames );
        for (int i = 0; i < frameNames.length; i++) {
            _frameContents.put( frameNames[i], WebResponse.BLANK_RESPONSE );
        }
    }


    private void removeSubFrames( String targetName ) {
        String[] names = (String[]) _subFrames.get( targetName );
        if (names == null) return;
        for (int i = 0; i < names.length; i++) {
            if (!targetName.equals( names[i] )) removeSubFrames( names[i] );
            _frameContents.remove( names[i] );
            _subFrames.remove( names[i] );
        }
    }

//==================================================================================================


    static public class HeaderDictionary extends Hashtable {

        public void addEntries( Dictionary source ) {
            for (Enumeration e = source.keys(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                put( key, source.get( key ) );
            }
        }

        public Object get( Object fieldName ) {
            return (String) super.get( matchPreviousFieldName( fieldName.toString() ) );
        }


        public Object put( Object fieldName, Object fieldValue ) {
            fieldName = matchPreviousFieldName( fieldName.toString() );
            Object oldValue = super.get( fieldName );
            if (fieldValue == null) {
                remove( fieldName );
            } else {
                super.put( fieldName, fieldValue );
            }
            return oldValue;
        }


        /**
         * If a matching field name with different case is already known, returns the older name.
         * Otherwise, returns the specified name.
         **/
        private String matchPreviousFieldName( String fieldName ) {
            for (Enumeration e = keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                if (key.equalsIgnoreCase( fieldName )) return key;
            }
            return fieldName;
        }


    }

}


//==================================================================================================


class RedirectWebRequest extends WebRequest {


    RedirectWebRequest( WebResponse response ) throws MalformedURLException {
        super( response.getURL(), response.getHeaderField( "Location" ), response.getTarget() );
    }


    /**
     * Returns the HTTP method defined for this request.
     **/
    public String getMethod() {
        return "GET";
    }
}



//==================================================================================================


class NoSuchFrameException extends RuntimeException {

    NoSuchFrameException( String frameName ) {
        _frameName = frameName;
    }


    public String getMessage() {
        return "No frame named " + _frameName + " is currently active";
    }


    private String _frameName;
}
