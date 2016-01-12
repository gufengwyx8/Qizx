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
 * Copyright (c) 2009-2010 Pixware. 
 *
 * Author: Hussein Shafie
 * Modified by XF
 * 
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.netutil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


/**
 * A utility class making it easy to send <tt>POST</tt> requests to 
 * an <tt>HTTP</tt> or <tt>HTTPS</tt> server.
 *
 * <p>This class will automatically format the body of the request as
 * "<tt>application/x-www-form-urlencoded</tt>" or as 
 * "<tt>multipart/form-data</tt>" depending on the types, String or 
 * {@link FileContent}, of the parameters added to the request object.
 * 
 * <p>This class is typically used as follows:
 * <pre>PostRequest req = new PostRequest();
 *req.addParameter("person", "John X Doe");
 *req.addParameter("photo",
 *                 new FileContentImpl(new File("photos/john_x_doe.jpg")));
 *HttpURLConnection connection = req.post(url);
 *checkResponse(connection);</pre>
 */
public class PostRequest extends Request
{
    protected HashMap<String, Object[]> parameters;
    protected boolean isMultipartRequest;

    // -----------------------------------------------------------------------

    /**
     * Constructs a PostRequest having no parameters.
     *
     * @see #addParameter
     */
    public PostRequest()
    {
        parameters = new HashMap<String, Object[]>();
        isMultipartRequest = false;
        headers = new HashMap<String, String[]>();
        charEncoding = DEFAULT_CHAR_ENCODING;
    }

    /**
     * Add specified parameter. Note that the same parameter may be added
     * several times with different values. That is, adding a parameter
     * does not overwrite its previous value, instead values accumulate.
     *
     * @param name name of the parameter
     * @param value value of the parameter.
     * Generally a {@link FileContent} or a String. Any other kind of object 
     * will be trasnmitted as a String by using <code>value.toString()</code>.
     * @see #setParameter
     */
    public void addParameter(String name, Object value)
    {
        Object[] params = parameters.get(name);
        if (params == null) {
            params = new Object[] { value };
        }
        else {
            int count = params.length;
            Object[] params2 = new Object[count + 1];
            System.arraycopy(params, 0, params2, 0, count);
            params2[count] = value;
            params = params2;
        }
        parameters.put(name, params);

        if (value instanceof FileContent) {
            isMultipartRequest = true;
        }
    }
    
    public void addParameter(String name, File file, String contentType)
    {
        addParameter(name, new FileContentImpl(file, contentType));
    }
    
    /**
     * Replaces the previous values, if any, of specified parameter by
     * specified value.
     * @param name name of the parameter
     * @param value value of the parameter. Generally a {@link FileContent} or
     *        a String. Any other kind of object will be trasnmitted as a
     *        String by using <code>value.toString()</code>.
     * @see #addParameter
     */
    public void setParameter(String name, Object value)
    {
        parameters.put(name, new Object[] { value });

        if (value instanceof FileContent) {
            isMultipartRequest = true;
        }
    }
    
    public void setParameter(String name, File file, String contentType)
    {
        setParameter(name, new FileContentImpl(file, contentType));
    }
    

    /**
     * Return the values of specified parameter. Returns <code>null</code> if
     * specified parameter is absent.
     * @see #addParameter
     */
    public Object[] getParameter(String name)
    {
        return parameters.get(name);
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
     * Post this request to the service having specified URL.
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
    public HttpURLConnection post(URL url) 
        throws IOException
    {
        connection = openConnection(url);
        connection.setRequestMethod("POST");

        // Add headers ---
        for(Map.Entry<String, String[]> e : headers.entrySet())
        {
            String name = e.getKey();
            String[] values = e.getValue();

            for (int i = 0; i < values.length; ++i) {
                connection.addRequestProperty(name, values[i]);
            }
        }

        // Write parameters ---

        OutputStream out = null;
        boolean firstPair = true;
        MultipartWriter multipartWriter = null;
        if (isMultipartRequest) {
            multipartWriter = new MultipartWriter(connection);
            out = multipartWriter.getOutputStream();
        }
        else {
            connection.setRequestProperty(CONTENT_TYPE,
                                          "application/x-www-form-urlencoded");
            out = connection.getOutputStream();
        }

        try {
            for(Map.Entry<String, Object[]> e : parameters.entrySet())
            {
                String name = e.getKey();
                byte[] nameBytes = null;
                Object[] params = e.getValue();

                for (int i = 0; i < params.length; ++i) {
                    Object param = params[i];

                    if (isMultipartRequest) {
                        if (param instanceof FileContent) {
                            FileContent fileParam = (FileContent) param;
                            multipartWriter.writePart(name, fileParam);
                        } else {
                            String textParam = param.toString();
                            multipartWriter.writePart(name, textParam, null);
                        }
                    }
                    else {
                        String textParam = param.toString();

                        if (firstPair) {
                            firstPair = false;
                        } else {
                            out.write('&');
                        }

                        String encoded;
                        if (nameBytes == null) {
                            encoded = URLEncoder.encode(name, charEncoding);
                            nameBytes = encoded.getBytes("US-ASCII");
                        }
                        out.write(nameBytes);

                        out.write('=');

                        encoded = URLEncoder.encode(textParam, charEncoding);
                        out.write(encoded.getBytes("US-ASCII"));
                    }
                }
            }

            if (isMultipartRequest) {
                multipartWriter.writeEndOfParts();
            }
            out.flush();
        }
        finally {
            if (out != null) {
                out.close();
            }
        }

        return connection;
    }
}
