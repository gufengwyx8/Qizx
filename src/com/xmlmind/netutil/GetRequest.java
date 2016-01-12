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
/*
 * Copyright (c) 2009-2010 Axyana Software. 
 *
 * Author: XF from PostRequest
 *
 */
package com.xmlmind.netutil;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


/**
 * A utility class making it easy to send <tt>GET</tt> requests to 
 * an <tt>HTTP</tt> or <tt>HTTPS</tt> server.
 *
 * <p>This class will automatically format the URL of the request as
 * "<tt>application/x-www-form-urlencoded</tt>" or as 
 * "<tt>multipart/form-data</tt>" depending on the types, String or 
 * {@link FileContent}, of the parameters added to the request object.
 * 
 * <p>This class is typically used as follows:
 * <pre>GetRequest req = new GetRequest();
 *req.addParameter("person", "John X Doe");
 *HttpURLConnection connection = req.get(url);
 *checkResponse(connection);</pre>
 */
public class GetRequest extends Request
{
    protected HashMap<String, String> parameters;
    
    /**
     * Constructs a GetRequest having no parameters.
     *
     * @see #addParameter
     */
    public GetRequest()
    {
        parameters = new HashMap<String, String>();
        headers = new HashMap<String, String[]>();
        charEncoding = DEFAULT_CHAR_ENCODING;
    }

    /**
     * Replaces the previous values, if any, of specified parameter 
     * by specified value.
     *
     * @param name name of the parameter
     * @param value value of the parameter.
     * Generally a {@link FileContent} or a String. Any other kind of object 
     * will be trasnmitted as a String by using <code>value.toString()</code>.
     * @see #addParameter
     */
    public void setParameter(String name, String value)
    {
        parameters.put(name, value);
    }

    /**
     * Return the values of specified parameter. Returns <code>null</code>
     * if specified parameter is absent.
     *
     * @see #addParameter
     */
    public String getParameter(String name)
    {
        return (String) parameters.get(name);
    }

    /**
     * Return the names of all parameters added using {@link #addParameter} or
     * {@link #setParameter}.
     */
    public String[] getParameterNames()
    {
        String[] names = new String[parameters.size()];
        return parameters.keySet().toArray(names);
    }

    /**
     * GET this request to the service having specified URL.
     * <p>Will automatically format the body of the request as
     * "<tt>application/x-www-form-urlencoded</tt>" or as 
     * "<tt>multipart/form-data</tt>" depending on the types, String or 
     * {@link FileContent}, of the parameters added to the request object.
     *
     * @param url <tt>http:</tt> or <tt>https:</tt> URL of the service
     * @return the connection to the service. This connection allows
     * to read the response of the server.
     * @exception IOException if an I/O problem or a network problem occurs
     * @see #addParameter
     */
    public HttpURLConnection get(String url) 
        throws IOException
    {
        boolean firstPair = url.lastIndexOf('?') < 0; // detect existing '?'

        // Append parameters to URL ---
        StringBuilder urlBuf = new StringBuilder(url);
        if(firstPair)
            urlBuf.append('?');
        for (Map.Entry<String, String> e : parameters.entrySet())
        {
            String name = e.getKey();
            String value = e.getValue();

            if (!firstPair)
                urlBuf.append('&');
            firstPair = false;

            String encoded = URLEncoder.encode(name, charEncoding);
            urlBuf.append(encoded);

            urlBuf.append('=');
            encoded = URLEncoder.encode(value, charEncoding);
            urlBuf.append(encoded);
        }

        connection = openConnection(new URL(urlBuf.toString()));

        // Add headers ---

        for(Map.Entry<String,String[]> e : headers.entrySet())
        {
            String name = e.getKey();
            String[] values = e.getValue();

            for (int i = 0; i < values.length; ++i) {
                connection.addRequestProperty(name, values[i]);
            }
        }

        connection.connect();
        return connection;
    }
}
