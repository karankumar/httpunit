package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2001, Russell Gold
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
import java.net.URL;

import java.net.*;
import java.io.*;
import java.util.*;


class HttpRequestStream {
    HttpRequestStream( InputStream stream ) throws IOException {
        _stream = new BufferedInputStream( stream );

        StringTokenizer st = new StringTokenizer( readHeaderLine() );
        _command  = st.nextToken();
        _uri      = st.nextToken();
        String protocol = st.nextToken();
    
        if (!_command.equals( "GET" ) && !_command.equals( "POST" ) && !_command.equals( "PUT" )) {
            throw new UnknownMethodException( _command );
        }
        readHeaders();
        readContent();
    }


    Reader getReader() {
        return _reader;
    }

    String getCommand() {
        return _command;
    }

    String getURI() {
        return _uri;
    }


    String getHeader( String name ) {
        return (String) _headers.get( name.toUpperCase() );
    }


    byte[] getBody() {
        return _requestBody;
    }


    /**
     * Returns the parameter with the specified name. If no such parameter exists, will
     * return null.
     **/
    String[] getParameter( String name ) {
        if (_parameters == null) {
            _parameters = readParameters( new String( _requestBody ) );
        }
        return (String[]) _parameters.get( name );
    }


    private static final int CR = 13;
    private static final int LF = 10;

    private InputStream    _stream;
    private Reader         _reader;
    private String         _command;
    private String         _uri;
    private Hashtable      _headers = new Hashtable();
    private Hashtable      _parameters;
    private byte[]         _requestBody;


    private void readContent() throws IOException {
        _requestBody = new byte[ getContentLength() ];
        try {
            _stream.read( _requestBody );
            _reader = new InputStreamReader( new ByteArrayInputStream( _requestBody ) );
        } catch (NumberFormatException e) {
        }
    }


    private int getContentLength() {
        try {
            return Integer.parseInt( getHeader( "Content-Length" ) );
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    private void readHeaders() throws IOException {
        String lastHeader = null;

        String header = readHeaderLine();
        while (header.length() > 0) {
    	    if (header.charAt(0) <= ' ') {
    	        if (lastHeader == null) continue;
    		    _headers.put( lastHeader, _headers.get( lastHeader ) + header.trim() );
    	    } else {
    	        lastHeader = header.substring( 0, header.indexOf(':') ).toUpperCase();
                _headers.put( lastHeader, header.substring( header.indexOf(':')+1 ).trim() );
    	    }
            header = readHeaderLine();
        }
    }


    private String readHeaderLine() throws IOException {
        StringBuffer sb = new StringBuffer();
        int b = _stream.read();
        while (b != CR) {
            sb.append( (char) b );
            b = _stream.read();
        }

        b = _stream.read();
        if (b != LF) throw new IOException( "Bad header line termination: " + b );

        return sb.toString();
    }


    private Hashtable readParameters( String content ) {
        Hashtable parameters = new Hashtable();
	    if (content == null || content.trim().length() == 0) return parameters;

        StringTokenizer st = new StringTokenizer( content, "&=" );
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            if (st.hasMoreTokens()) {
                addParameter( parameters, decode( name ), decode( st.nextToken() ) );
            }
        }
        return parameters;
    }


    private void addParameter( Hashtable parameters, String name, String value ) {
        String[] oldValues = (String[]) parameters.get( name );
        if (oldValues == null) {
            parameters.put( name, new String[] { value } );
        } else {
            String[] values = new String[ oldValues.length+1 ];
            System.arraycopy( oldValues, 0, values, 0, oldValues.length );
            values[ oldValues.length ] = value;
            parameters.put( name, values );
        }
    }


    private String decode( String byteString ) {
        StringBuffer sb = new StringBuffer();
        char[] chars = byteString.toCharArray();
        char[] hexNum = { '0', '0', '0' };

        int i = 0;
        while (i < chars.length) {
            if (chars[i] == '+') {
                i++;
                sb.append( ' ' );
            } else if (chars[i] == '%') {
                i++;
                hexNum[1] = chars[i++];
                hexNum[2] = chars[i++];
                sb.append( (char) Integer.parseInt( new String( hexNum ), 16 ) );
            } else {
                sb.append( chars[i++] );
            }
        }
        return sb.toString();
    }

}
