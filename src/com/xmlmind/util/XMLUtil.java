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
 * Copyright (c) 2009 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * A collection of utility functions (static methods) related to XML.
 */
public final class XMLUtil {
    private XMLUtil() {}

    /**
     * Equivalent to {@link #newSAXParser(boolean, boolean, boolean) 
     * newSAXParser(true, false, false)}.
     */
    public static SAXParser newSAXParser()
        throws ParserConfigurationException, SAXException {
        return newSAXParser(true, false, false);
    }

    /**
     * Convenience method: creates and returns a SAXParser.
     * 
     * @param namespaceAware specifies whether the parser produced 
     * by this code will provide support for XML namespaces
     * @param validating specifies whether the parser produced by 
     * this code will validate documents against their DTD
     * @param xIncludeAware specifies whether the parser produced by 
     * this code will process XIncludes
     * @return newly created SAXParser
     * @exception ParserConfigurationException if a parser cannot be created 
     * which satisfies the requested configuration
     * @exception SAXException for SAX errors
     */
    public static SAXParser newSAXParser(boolean namespaceAware, 
                                         boolean validating, 
                                         boolean xIncludeAware)
        throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        factory.setValidating(validating);
        factory.setXIncludeAware(xIncludeAware);

        // For Xerces which otherwise, does not support "x-MacRoman".
        try {
            factory.setFeature(
                "http://apache.org/xml/features/allow-java-encodings", true);
        } catch (Exception ignored) {}

        return factory.newSAXParser();
    }

    // -----------------------------------------------------------------------

    /**
     * Loads the contents of specified XML file. 
     * <p>Unlike {@link FileUtil#loadString}, this method implements 
     * the detection of the XML encoding.
     * <p>Note that the detection of the XML encoding always works 
     * because it uses UTF-8 as a fallback value.
     *
     * @param file the filename of the XML file
     * @param encoding the detected encoding is copied there.
     * May be <code>null</code>.
     * @return the contents of the XML file
     * @exception IOException if there is an I/O problem
     * @see #loadXML(URL, String[])
     */
    public static String loadXML(File file, String[] encoding) 
        throws IOException {
        String loaded = null;

        InputStream in = new FileInputStream(file);
        try {
            loaded = loadXML(in, encoding);
        } finally {
            in.close();
        }

        return loaded;
    }

    /**
     * Loads the contents of specified XML file. 
     * <p>Unlike {@link URLUtil#loadString}, this method implements 
     * the detection of the XML encoding.
     * <p>Note that the detection of the XML encoding always works 
     * because it uses UTF-8 as a fallback value.
     *
     * @param url the location of the XML file
     * @param encoding the detected encoding is copied there.
     * May be <code>null</code>.
     * @return the contents of the XML file
     * @exception IOException if there is an I/O problem
     * @see #loadXML(File, String[])
     */
    public static String loadXML(URL url, String[] encoding) 
        throws IOException {
        String loaded = null;

        URLConnection connection = URLUtil.openConnectionNoCache(url);
        InputStream in = connection.getInputStream();
        try {
            loaded = loadXML(in, encoding);
        } finally {
            in.close();
        }

        return loaded;
    }

    /**
     * Loads the contents of specified XML source. 
     * <p>This method implements the detection of the XML encoding.
     * <p>Note that the detection of the XML encoding always works 
     * because it uses UTF-8 as a fallback value.
     *
     * @param in the XML source
     * @param encoding the detected encoding is copied there.
     * May be <code>null</code>.
     * @return the contents of the XML source
     * @exception IOException if there is an I/O problem
     * @see #loadXML(File, String[])
     * @see #loadXML(URL, String[])
     */
    public static String loadXML(InputStream in, String[] encoding) 
        throws IOException {
        Reader src = createXMLReader(in, encoding);
        return loadChars(src);
    }

    /**
     * Load the characters contained in specified source.
     * 
     * @param in the character source
     * @return the contents of the character source
     * @exception IOException if there is an I/O problem
     */
    public static String loadChars(Reader in) 
        throws IOException {
        StringBuilder buffer = new StringBuilder();
        char[] chars = new char[65536];
        int count;

        while ((count = in.read(chars, 0, chars.length)) != -1) {
            if (count > 0) {
                buffer.append(chars, 0, count);
            }
        }

        return buffer.toString();
    }

    /**
     * Creates a reader allowing to read the contents of specified XML source.
     * <p>This method implements the detection of the XML encoding.
     * <p>Note that the detection of the XML encoding always works 
     * because it uses UTF-8 as a fallback value.
     *
     * @param in the XML source
     * @param encoding the detected encoding is copied there.
     * May be <code>null</code>.
     * @return a reader allowing to read the contents of the XML source.
     * This reader will automatically skip the BOM if any.
     * @exception IOException if there is an I/O problem
     */
    public static Reader createXMLReader(InputStream in, String[] encoding) 
        throws IOException {
        byte[] bytes = new byte[1024];
        int byteCount = -1;

        PushbackInputStream in2 = new PushbackInputStream(in, bytes.length);
        try {
            int count = in2.read(bytes, 0, bytes.length);
            if (count > 0) {
                in2.unread(bytes, 0, count);
            }
            byteCount = count;
        } catch (IOException ignored) {}

        String charset = null;

        if (byteCount > 0) {
            if (byteCount >= 2) {
                // Use BOM ---

                int b0 = (bytes[0] & 0xFF);
                int b1 = (bytes[1] & 0xFF);

                switch ((b0 << 8) | b1) {
                case 0xFEFF:
                    charset = "UTF-16BE";
                    // We don't want to read the BOM.
                    in2.skip(2);
                    break;
                case 0xFFFE:
                    charset = "UTF-16LE";
                    in2.skip(2);
                    break;
                case 0xEFBB:
                    if (byteCount >= 3 && (bytes[2] & 0xFF) == 0xBF) {
                        charset = "UTF-8";
                        in2.skip(3);
                    }
                    break;
                }
            }

            if (charset == null) {
                // Unsupported characters are replaced by U+FFFD.
                String text = new String(bytes, 0, byteCount, "US-ASCII");

                if (text.startsWith("<?xml")) {
                    Pattern pattern = 
                        Pattern.compile("encoding\\s*=\\s*['\"]([^'\"]+)");
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        charset = matcher.group(1);
                    }
                }
            }
        }

        if (charset != null) {
            if (encoding != null) {
                encoding[0] = charset;
            }
            try {
                return new InputStreamReader(in2, charset);
            } catch (UnsupportedEncodingException ignored) {
                // An incorrect/unknown charset?
            }
        }

        if (encoding != null) {
            encoding[0] = "UTF-8";
        }
        return new InputStreamReader(in2, "UTF-8");
    }

    /*TEST_LOAD_XML
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: com.xmlmind.util.XMLUtil xml_file");
            System.exit(1);
        }

        File xmlFile = new File(args[0]);

        String[] encoding = new String[1];
        String xml = loadXML(xmlFile, encoding);
        System.out.println(xmlFile + ": " + Integer.toString(xml.length()) + 
                           "chars, encoding=" + encoding[0]);
        System.err.println("{" + xml + "}");
    }
    TEST_LOAD_XML*/
}
