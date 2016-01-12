/*
 *    Qizx/open 4.1
 *
 * This code is part of the Qizx application components
 * Copyright (C) 2004-2010 Axyana Software -- All rights reserved.
 *
 * The contents of this file are subject to the Mozilla Public License 
 *  Version 1.1 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 * The Initial Developer of the Original Code is Xavier Franc - Axyana Software.
 *
 */
package com.xmlmind.netutil;

import com.xmlmind.util.FileUtil;
import com.xmlmind.util.URLUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Extracted base-class for {@link GetRequest} and {@link PostRequest}.
 */
public class Request
{
    /**
     * The character encoding returned by default by {@link #getCharEncoding}.
     * <p>
     * By default, it's ISO-8859-1 because the encoding used by the internal "
     * <tt>application/x-www-form-urlencoded</tt>" decoder of a Servlet
     * Container such as Tomcat seems to be ISO-8859-1.
     */
    public static final String DEFAULT_CHAR_ENCODING = "UTF-8";
    private static final String QIZX_ERROR = "text/x-qizx-error";
    public static final String CONTENT_TYPE = "Content-Type";

    protected HashMap<String, String[]> headers;
    protected String charEncoding;
    
    protected HttpURLConnection connection;

    /**
     * Specifies the character encoding used to escape accented characters in "
     * <tt>application/x-www-form-urlencoded</tt>".
     * @param encoding a character encoding: "<tt>ISO-8859-1</tt>" or "
     *        <tt>UTF-8</tt>". Specifying <code>null</code> is equivalent to
     *        resetting the encoding to the default value.
     * @see #getCharacterEncoding
     */
    public void setCharacterEncoding(String encoding)
    {
        if (encoding == null) {
            encoding = DEFAULT_CHAR_ENCODING;
        }
        charEncoding = encoding;
    }

    /**
     * Returns the character encoding used to escape accented characters in "
     * <tt>application/x-www-form-urlencoded</tt>".
     * @see #setCharacterEncoding
     */
    public String getCharacterEncoding()
    {
        return charEncoding;
    }

    /**
     * Replaces the previous values, if any, of specified header by specified
     * value.
     * @param name name of the header
     * @param value value of the header
     * @see #addHeader
     */
    public void setHeader(String name, String value)
    {
        headers.put(name, new String[] { value });
    }
    
    /**
     * Add specified header. Note that the same header may be added
     * several times with different values. That is, adding a header
     * does overwrite its previous value, instead values accumulate.
     *
     * @param name name of the header
     * @param value value of the header
     * @see #setHeader
     */
    public void addHeader(String name, String value)
    {
        String[] values = headers.get(name);
        if (values == null) {
            values = new String[] { value };
        }
        else {
            int count = values.length;
            String[] values2 = new String[count + 1];
            System.arraycopy(values, 0, values2, 0, count);
            values2[count] = value;
            values = values2;
        }
        headers.put(name, values);
    }

    /**
     * Return the values of specified header. Returns <code>null</code>
     * if specified header is absent.
     *
     * @see #addHeader
     */
    public String[] getHeader(String name)
    {
        return (String[]) headers.get(name);
    }

    /**
     * Return the names of all headers added using {@link #addHeader} or
     * {@link #setHeader}.
     * @see #addHeader
     */
    public String[] getHeaderNames()
    {
        String[] names = new String[headers.size()];
        return headers.keySet().toArray(names);
    }
    
    public String getResultMimeType()
    {
        checkState(connection);
        return connection.getContentType();
    }

    public void checkError()  throws IOException
    {
        checkResponse(connection);
        String ctype = connection.getContentType();
        if(ctype != null && ctype.startsWith(QIZX_ERROR))
            throw new IOException(getStringResponse());       
    }
    
    public boolean isError()
        throws IOException
    {
        checkState(connection);
        int code = connection.getResponseCode();
        if(code >= 400)
            return true;
        String ctype = connection.getContentType();
        return ctype != null && ctype.startsWith(QIZX_ERROR);
    }

    public String getErrorString() throws IOException
    {
        checkState(connection);
        if (connection.getResponseCode() >= 400)
            return connection.getResponseMessage();
        return getStringResponse();
    }

    public String getStringResponse() throws IOException
    {
        checkState(connection);
        InputStream in = connection.getInputStream();
        try {
            return FileUtil.loadString(in, DEFAULT_CHAR_ENCODING);
        }
        finally {
            in.close();
        }
    }
    
    public InputStream getStreamResponse() throws IOException
    {
        return getResponseStream(connection);
    }

    public HttpURLConnection getConnection()
    {
        return connection;
    }

    /**
     * Low-level helper: opens a connection to the service having 
     * specified URL. This connection is configured to send GET 
     * requests to the service and to read the response.
     *
     * @param url <tt>http:</tt> or <tt>https:</tt> URL of the service
     * @exception IOException if a network problem occurs
     */
    public static HttpURLConnection openConnection(URL url)
        throws IOException
    {
        HttpURLConnection connection = 
            (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        return connection;
    }

    /**
     * Convenience function: throws an IOException having a detailed 
     * error message if the service returns a response code greater 
     * or equal to 300.
     * 
     * @param connection connection to the service
     * @return response code of the service. Example: 200 (Success).
     * @exception IOException if an I/O problem or a network problem occurs
     * @see #GET
     */
    static int checkResponse(HttpURLConnection connection)
        throws IOException
    {
        checkState(connection);
        //DEBUG
//        Map<String, List<String>> hm = connection.getHeaderFields();
//        for(Map.Entry e : hm.entrySet()) {
//            System.err.println("header "+e.getKey()+" = "+e.getValue());
//        }
        
        int code = connection.getResponseCode();
        if (code >= 300) {
            StringBuilder msg = new StringBuilder("failed request on ");
            msg.append(connection.getURL());
            msg.append(": error ");
            msg.append(code);
            String explain = connection.getResponseMessage();
            if (explain != null) {
                msg.append(": ");
                msg.append(explain);
            }
            throw new IOException(msg.toString());
        }
        return code;
    }

    private static void checkState(HttpURLConnection connection)
    {
        if(connection == null)
            throw new IllegalStateException("not connected");
    }

    public static InputStream getResponseStream(HttpURLConnection connection)
        throws IOException
    {
        return connection.getInputStream();
    }
    
    /**
     * Convenience function: reads and returns the text (that is, 
     * <tt>text/plain</tt>, <tt>text/html</tt>, etc) body sent by the service 
     * as a response to the request.
     * 
     * @param connection connection to the service
     * @param defaultCharset which charset to use when this is not 
     * specified in the response. 
     * May be <code>null</code> in which the platform native encoding is used.
     * @return text body of the response or <code>null</code> if 
     * there is no body or if the body does not contain text
     * @exception IOException if an I/O problem or a network problem occurs
     * @see #GET
     */
    public static String getResponseText(HttpURLConnection connection,
                                         String defaultCharset)
        throws IOException
    {
        if(connection == null)
            throw new IllegalArgumentException("null connection");
        String contentType = connection.getContentType();
        if (contentType == null || 
            !contentType.toLowerCase().startsWith("text/")) {
            return null;
        }
    
        String charset = URLUtil.contentTypeToCharset(contentType);
        if (charset == null) {
            charset = defaultCharset;
        }
    
        String result = null;
    
        InputStream in = connection.getInputStream();
        try {
            result = FileUtil.loadString(in, charset);
        } finally {
            in.close();
        }
    
        return result;
    }

}
