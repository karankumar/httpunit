package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000-2002, Russell Gold
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
import com.meterware.httpunit.scripting.ScriptableDelegate;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class represents a link in an HTML page. Users of this class may examine the
 * structure of the link (as a DOM), or create a {@link WebRequest} to simulate clicking
 * on the link.
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 * @author <a href="mailto:benoit.xhenseval@avondi.com>Benoit Xhenseval</a>
 **/
public class WebLink extends WebRequestSource {

    private Scriptable _scriptable;


    /**
     * Returns the URL referenced by this link. This may be a relative URL.
     **/
    public String getURLString() {
        String href = NodeUtils.getNodeAttribute( getNode(), "href" );
        final int hashIndex = href.indexOf( '#' );
        if (hashIndex < 0) {
            return href;
        } else {
            return href.substring( 0, hashIndex );
        }
    }


    /**
     * Returns the text value of this link.
     **/
    public String asText() {
        if (getNode().getNodeName().equals( "area" )) {
            return NodeUtils.getNodeAttribute( getNode(), "alt" );
        } else if (!getNode().hasChildNodes()) {
            return "";
        } else {
            return NodeUtils.asText( getNode().getChildNodes() );
        }
    }


    /**
     * Submits a request as though the user had clicked on this link. Will also fire the 'onClick' event if defined.
     **/
    public WebResponse click() throws IOException, SAXException {
        String event = NodeUtils.getNodeAttribute( getNode(), "onclick" );
        if (event.length() == 0 || getScriptableObject().doEvent( event )) return submitRequest();
        return getBaseResponse();
    }


    /**
     * Simulates moving the mouse over the link. Will fire the 'onMouseOver' event if defined.
     **/
    public void mouseOver() {
        String event = NodeUtils.getNodeAttribute( getNode(), "onmouseover" );
        if (event.length() > 0) getScriptableObject().doEvent( event );
    }


//----------------------------------------- WebRequestSource methods ---------------------------------------------------


    /**
     * Creates and returns a web request which will simulate clicking on this link.
     **/
    public WebRequest getRequest() {
        return new GetMethodWebRequest( this );
    }


    /**
     * Returns the scriptable delegate.
     */
    ScriptableDelegate getScriptableDelegate() {
        return getScriptableObject();
    }


    /**
     * Returns an array containing the names of any parameters defined as part of this link's URL.
     **/
    public String[] getParameterNames() {
        ArrayList parameterNames = new ArrayList( getPresetParameterMap().keySet() );
        return (String[]) parameterNames.toArray( new String[ parameterNames.size() ] );
    }


    /**
     * Returns the multiple default values of the named parameter.
     **/
    public String[] getParameterValues( String name ) {
        final String[] values = (String[]) getPresetParameterMap().get( name );
        return values == null ? NO_VALUES : values;
    }


//--------------------------------- ParameterHolder methods --------------------------------------


    /**
     * Specifies the position at which an image button (if any) was clicked.
     **/
    void selectImageButtonPosition( SubmitButton imageButton, int x, int y ) {
        throw new IllegalLinkParametersRequest();
    }


    /**
     * Iterates through the fixed, predefined parameters in this holder, recording them in the supplied parameter processor.\
     * These parameters always go on the URL, no matter what encoding method is used.
     **/

    void recordPredefinedParameters( ParameterProcessor processor ) throws IOException {
    }


    /**
     * Iterates through the parameters in this holder, recording them in the supplied parameter processor.
     **/
    void recordParameters( ParameterProcessor processor ) throws IOException {
        Iterator i = getPresetParameterList().iterator();
        while (i.hasNext()) {
            LinkParameter o = (LinkParameter) i.next();
            processor.addParameter( o.getName(), o.getValue(), getCharacterSet() );
         }
    }


    /**
     * Removes a parameter name from this collection.
     **/
    void removeParameter( String name ) {
        throw new IllegalLinkParametersRequest();
    }


    /**
     * Sets the value of a parameter in a web request.
     **/
    void setParameter( String name, String value ) {
        setParameter( name, new String[] { value } );
    }


    /**
     * Sets the multiple values of a parameter in a web request.
     **/
    void setParameter( String name, String[] values ) {
        if (values == null) {
            throw new IllegalArgumentException( "May not supply a null argument array to setParameter()" );
        } else if (!getPresetParameterMap().containsKey( name )) {
            throw new IllegalLinkParametersRequest();
        } else if (!equals( getParameterValues( name ), values )) {
            throw new IllegalLinkParametersRequest();
        }
    }


    private boolean equals( String[] left, String[] right ) {
        if (left.length != right.length) return false;
        List rightValues = Arrays.asList( right );
        for (int i = 0; i < left.length; i++) {
            if (!rightValues.contains( left[i] )) return false;
        }
        return true;
    }


    /**
     * Sets the multiple values of a file upload parameter in a web request.
     **/
    void setParameter( String name, UploadFileSpec[] files ) {
        throw new IllegalLinkParametersRequest();
    }


    /**
     * Returns true if the specified parameter is a file field.
     **/
    boolean isFileParameter( String name ) {
        return false;
    }


    boolean isSubmitAsMime() {
        return false;
    }


    void setSubmitAsMime( boolean mimeEncoded ) {
        throw new IllegalStateException( "May not change the encoding for a validated request created from a link" );
    }


    public class Scriptable extends ScriptableDelegate {

        public Object get( String propertyName ) {
            if (propertyName.equalsIgnoreCase( "href" )) {
                return getReference().toExternalForm();
            } else {
               return super.get( propertyName );
            }
        }


        private URL getReference() {
            try {
                return getRequest().getURL();
            } catch (MalformedURLException e) {
                return WebLink.this.getBaseURL();
            }
        }
    }


//--------------------------------------------------- package members --------------------------------------------------


    /**
     * Contructs a web link given the URL of its source page and the DOM extracted
     * from that page.
     **/
    WebLink( WebResponse response, URL baseURL, String parentTarget, Node node ) {
        super( response, node, baseURL, NodeUtils.getNodeAttribute( node, "href" ), parentTarget );
    }


    /**
     * Returns an object which provides scripting access to this link.
     **/
    Scriptable getScriptableObject() {
        if (_scriptable == null) _scriptable = new Scriptable();
        return _scriptable;
    }


//--------------------------------------------------- private members --------------------------------------------------


    private static final String[] NO_VALUES = new String[0];

    private Map       _presetParameterMap;
    private ArrayList _presetParameterList;


    private Map getPresetParameterMap() {
        if (_presetParameterMap == null) loadPresetParameters();
        return _presetParameterMap;
    }


    private ArrayList getPresetParameterList() {
        if (_presetParameterList == null) loadPresetParameters();
        return _presetParameterList;
    }


    private void loadPresetParameters() {
        _presetParameterMap = new HashMap();
        _presetParameterList = new ArrayList();
        loadDestinationParameters();
    }


    protected void addPresetParameter( String name, String value ) {
        _presetParameterMap.put( name, HttpUnitUtils.withNewValue( (String[]) _presetParameterMap.get( name ), value ) );
        _presetParameterList.add( new LinkParameter( name, value ) );
    }


}


class LinkParameter {
    private String _name;
    private String _value;


    public LinkParameter( String name, String value ) {
        _name = name;
        _value = value;
    }


    public String getName() {
        return _name;
    }


    public String getValue() {
        return _value;
    }
}


class IllegalLinkParametersRequest extends IllegalRequestParameterException {

    public IllegalLinkParametersRequest() {
    }

    public String getMessage() {
        return "May not modify parameters for a request derived from a link with parameter checking enabled.";
     }


}
