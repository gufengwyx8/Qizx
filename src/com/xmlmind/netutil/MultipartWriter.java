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
 * Copyright (c) 2005-2010 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.netutil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import com.xmlmind.util.Base64;

/**
 * Utility class used by {@link PostRequest} to format the body of 
 * <tt>multipart/form-data</tt> requests.
 */
public class MultipartWriter {
    public static final String NEWLINE = "\r\n";
    public static final String BOUNDARY_PREFIX = "--";

    protected HttpURLConnection connection;
    protected String boundary;
    protected OutputStream out;

    // -----------------------------------------------------------------------

    public MultipartWriter(HttpURLConnection connection) 
        throws IOException {
        this.connection = connection;

        boundary = ("--------------------" + 
                    Long.toString(System.currentTimeMillis(), 
                                  Character.MAX_RADIX));

        connection.setRequestProperty("Content-Type",
                                      "multipart/form-data; boundary=" + 
                                      boundary);

        out = connection.getOutputStream();
    }

    /**
     * Returns the underlying HTTP connection.
     */
    public HttpURLConnection getConnection() {
        return connection;
    }

    /**
     * Returns the boundary used to separate form parts. Useful in case you
     * want to write a form part ``by hand''.
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * Returns the output stream where form parts are written to. Useful in
     * case you want to write a form part ``by hand''.
     */
    public OutputStream getOutputStream() {
        return out;
    }

    /**
     * Writes specified text part using specified charset for its value. 
     */
    public void writePart(String name, String value, String charset) 
        throws IOException {
        writeBoundary();

        writeBytes("Content-Disposition: form-data; name=" + 
                   quoteText(name));
        writeBytes(NEWLINE);
        
        if (charset == null) {
            charset = "UTF-8"; 
        }
        writeBytes("Content-Type: text/plain; charset=" + charset);
        writeBytes(NEWLINE);

        writeBytes("Content-Transfer-Encoding: binary");
        writeBytes(NEWLINE);

        writeBytes(NEWLINE);

        writeBytes(value, charset);
        writeBytes(NEWLINE);

        out.flush();
    }

    /**
     * Properly quotes/escapes specified text.
     * 
     * @param text text to be quoted/escaped
     * @return quoted/escaped text.
     */
    public static String quoteText(String text) {
        byte[] bytes = null;
        if (!isQtext(text)) {
            try {
                bytes = text.getBytes("UTF-8");
            } catch (UnsupportedEncodingException cannotHappen) {}
        }

        StringBuffer buffer = new StringBuffer();
        if (bytes == null) {
            buffer.append('\"');
            buffer.append(text);
            buffer.append('\"');
        } else {
            // RFC 2047.
            buffer.append("=?utf-8?b?");
            buffer.append(Base64.encode(bytes));
            buffer.append("?=");
        }
        return buffer.toString();
    }

    /**
     * Returns <code>true</code> if specified string is <i>qtext</i> 
     * <small>(that is, any ASCII character including space and tab and 
     * excluding <tt>'"'</tt>, <tt>'\\'</tt> and <tt>'\r'</tt>)</small> 
     * according to RFC 822. Returns <code>false</code> otherwise.
     */
    public static boolean isQtext(String text) {
        // RFC 822.
        int count = text.length();
        for (int i = 0; i < count; ++i) {
            char c = text.charAt(i);
            
            switch (c) {
            case '\"': case '\\': case '\r':
                return false;
            default:
                if (c > 127) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Write specified file part using specified content type for the file.
     */
    public void writePart(String name, FileContent fileContent) 
        throws IOException {
        writeBoundary();

        String filename = fileContent.getName();
        if (filename == null) {
            filename = "";
        }
        writeBytes("Content-Disposition: form-data; name=" + 
                   quoteText(name) +
                   "; filename=" + quoteText(filename));
        writeBytes(NEWLINE);

        // A Content-Type is mandatory with multipart/form-data.
        String contentType = fileContent.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        writeBytes("Content-Type: " + contentType);
        writeBytes(NEWLINE);

        writeBytes("Content-Length: " + fileContent.getSize());
        writeBytes(NEWLINE);

        writeBytes("Content-Transfer-Encoding: binary");
        writeBytes(NEWLINE);

        writeBytes(NEWLINE);

        InputStream in = fileContent.getInputStream();
        try {
            copyBytes(in, out);
        } finally {
            in.close();
        }

        writeBytes(NEWLINE);

        out.flush();
    }

    /**
     * Copy all bytes from specified input to specified output. Does
     * <em>not</em> close the input stream and the output stream. Useful in
     * case you want to write a form part ``by hand''.
     */
    public static final void copyBytes(InputStream in, OutputStream out)
        throws IOException {
        byte[] buffer = new byte[65536];
        int count;

        while ((count = in.read(buffer)) != -1)  {
            out.write(buffer, 0, count);
        }
    }

    /**
     * Write a part boundary. Useful in case you want to write a form part
     * ``by hand''.
     */
    public void writeBoundary() 
        throws IOException {
        writeBytes(BOUNDARY_PREFIX);
        writeBytes(boundary);
        writeBytes(NEWLINE);
    }

    /**
     * Write specified string using charset <tt>US-ASCII</tt>. Useful in case
     * you want to write a form part ``by hand''.
     */
    public void writeBytes(String s) 
        throws IOException {
        writeBytes(s, "US-ASCII");
    }

    /**
     * Write specified string using specified charset. Useful in case you want
     * to write a form part ``by hand''.
     */
    public void writeBytes(String s, String charset) 
        throws IOException {
        out.write(s.getBytes(charset));
    }

    /**
     * Finishes writing all the form data.
     * 
     * @exception IOException if writing the end of form data fails
     */
    public void writeEndOfParts()
        throws IOException {
        writeBytes(BOUNDARY_PREFIX);
        writeBytes(boundary);
        writeBytes(BOUNDARY_PREFIX); // Final boundary.
        writeBytes(NEWLINE);
    }
}
