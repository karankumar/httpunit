package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000, Russell Gold
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Vector;

import java.io.*;

import org.w3c.dom.Document;


/**
 * A unit test of the httpunit parsing classes.
 **/
public class WebPageTest extends HttpUnitTest {

    public static void main(String args[]) {
        junit.textui.TestRunner.run( suite() );
    }
	
	
    public static Test suite() {
        return new TestSuite( WebPageTest.class );
    }


    public WebPageTest( String name ) {
        super( name );
    }


    public void setUp() throws Exception {
        super.setUp();
    }
	
	
    public void testNoResponse() throws Exception {
        WebConversation wc = new WebConversation();
        try {
            WebRequest request = new GetMethodWebRequest( getHostPath() + "/SimplePage.html" );
            WebResponse simplePage = wc.getResponse( request );
            fail( "Did not complain about missing page" );
        } catch (HttpNotFoundException e) {
        }
    }


    public void testTitle() throws Exception {
        defineResource( "SimplePage.html",
                        "<html><head><title>A Sample Page</title></head>\n" +
                        "<body>This has no forms but it does\n" +
                        "have <a href=\"/other.html\">an <b>active</b> link</A>\n" +
                        " and <a name=here>an anchor</a>\n" +
                        "<a href=\"basic.html\"><IMG SRC=\"/images/arrow.gif\" ALT=\"Next -->\" WIDTH=1 HEIGHT=4></a>\n" +
                        "</body></html>\n" );

        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( getHostPath() + "/SimplePage.html" );
        WebResponse simplePage = wc.getResponse( request );
        assertEquals( "Title", "A Sample Page", simplePage.getTitle() );
    }


    public void testXML() throws Exception {
        defineResource( "SimplePage.xml",
                        "<?xml version=\"1.0\" ?><main><title>See me now</title></main>",
                        "text/xml" );

        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( getHostPath() + "/SimplePage.xml" );
        WebResponse simplePage = wc.getResponse( request );
        Document doc = simplePage.getDOM();
    }


    public void testLocalFile() throws Exception {
        File file = new File( "temp.html" );
        FileWriter fw = new FileWriter( file );
        PrintWriter pw = new PrintWriter( fw );
        pw.println( "<html><head><title>A Sample Page</title></head>" );
        pw.println( "<body>This is a very simple page<p>With not much text</body></html>" );
        pw.close();

        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( "file:" + file.getAbsolutePath() );
        WebResponse simplePage = wc.getResponse( request );
        assertEquals( "Title", "A Sample Page", simplePage.getTitle() );

        file.delete();
    }


                              
}
