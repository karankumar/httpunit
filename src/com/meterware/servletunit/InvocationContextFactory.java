package com.meterware.servletunit;
/********************************************************************************************************************
 * $Id$
 *
 * Copyright (c) 2001-2002, Russell Gold
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
import com.meterware.httpunit.WebRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Dictionary;

import javax.servlet.http.Cookie;


/**
 * An interface for an object which acts as a factory of InvocationContexts
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 **/
public interface InvocationContextFactory {

    /**
     * Creates and returns a new invocation context to test calling of servlet methods.
     **/
    public InvocationContext newInvocation( ServletUnitClient client, WebRequest request, Cookie[] clientCookies, Dictionary clientHeaders, byte[] messageBody ) throws IOException, MalformedURLException;

}
