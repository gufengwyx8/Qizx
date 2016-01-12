/*
 *    Qizx/open 4.1
 *
 * This code is the open-source version of Qizx.
 * Copyright (C) 2004-2009 Axyana Software -- All rights reserved.
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
package com.qizx.api.util;

import com.qizx.api.DataModelException;
import com.qizx.api.Item;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.XMLPushStream;
import com.qizx.util.CharTable;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Unicode;
import com.qizx.util.basic.Util;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.XMLPushStreamBase;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;

/**
 * An implementation of XMLPushStream that serializes into XML, XHTML, HTML or
 * plain text.
 * <p>
 * Can be used to export a {@link Node}: see {@link Item#export(XMLPushStream)}
 */
public class XMLSerializer extends XMLPushStreamBase
    implements XMLPushStream /* redundant on purpose */
{
    /** name of option specifying the output style: the value must be "XML",
     * "HTML", "XHTML", "TEXT".   */
    public static final String METHOD = "method";

    /** name of option specifying the output encoding: the value must be a valid
     * encoding name, supported by the JRE.   */
    public static final String ENCODING = "encoding";
    /** name of option specifying the XML version. Value must be "1.0" or "1.1" */
    public static final String VERSION = "version";
    /** name of option enabling or disabling the XML declaration. Value must be
     * boolean "true", "false, "yes", "no". Automatically enabled when the
     * options VERSION, STANDALONE or ENCODING (other than UTF-8) are set */
    public static final String OMIT_XML_DECLARATION = "omit-xml-declaration";
    /** name of option specifying 'standalone' in the XML declaration. Value 
     * must be "yes" or "no" */
    public static final String STANDALONE = "standalone";

    /** name of option specifying the System Id of the DTD declaration */
    public static final String DOCTYPE_SYSTEM = "doctype-system";
    /** name of option specifying the Public Id of the DTD declaration */
    public static final String DOCTYPE_PUBLIC = "doctype-public";
    
    /** name of option specifying the Content-Type meta in HTML */
    public static final String INCLUDE_CONTENT_TYPE = "include-content-type";
    /** name of option specifying the media type (NOT IMPLEMENTED) */
    public static final String MEDIA_TYPE = "media-type";
    /** name of option escape-uri-attributes (NOT IMPLEMENTED) */
    public static final String ESCAPE_URI_ATTRIBUTES = "escape-uri-attributes";
    /**
     * name of option specifying the whether the output is indented: value is a
     * boolean "yes" or "no". The number of speces (default 2) is specified by
     * option INDENT_VALUE
     */
    public static final String INDENT = "indent";
    /**
     * name of option specifying the indentation (extension): value is an
     * integer which represents the number of spaces. If negative, the
     * indentation is disabled.
     */
    public static final String INDENT_VALUE = "indent-value";

    /**
     * name of option specifying the omission of processing instructions
     * (extension): value is a boolean (false by default) that indicates that
     * PI must be stripped (when set to true).
     */
    public static final String STRIP_PI = "strip-pi";
    /**
     * name of option specifying the omission of comments (extension): value is
     * a boolean (false by default) that indicates that comments must be
     * stripped (when set to true).
     */
    public static final String STRIP_COMMENT = "strip-comment";
    
    /**
     * name of option used to inhibit the automatic generation of DOCTYPE
     * (extension): value is a boolean (true by default) that 
     * indicates that a DOCTYPE should be generated on documents stored in
     * XML Libraries, having properties System/Public ID.
     */
    public static final String AUTO_DTD = "auto-dtd";

    private static final String UTF8 = "UTF-8";
    final static String XHTML =
        NamespaceContext.unique("http://www.w3.org/1999/xhtml");

    private static final HashMap Encodings = new HashMap();
    static {
        Encodings.put("UTF8", UTF8);
        Encodings.put("UTF16", "UTF-16");
        Encodings.put("ISO8859-1", "ISO-8859-1");
        Encodings.put("ISO8859_1", "ISO-8859-1");
        Encodings.put("ISO8859-2", "ISO-8859-2");
        Encodings.put("ISO8859_2", "ISO-8859-2");
        Encodings.put("ISO8859-3", "ISO-8859-3");
        Encodings.put("ISO8859_3", "ISO-8859-3");
        Encodings.put("ISO8859-4", "ISO-8859-4");
        Encodings.put("ISO8859_4", "ISO-8859-4");
        Encodings.put("ISO8859-5", "ISO-8859-5");
        Encodings.put("ISO8859_5", "ISO-8859-5");
        Encodings.put("ISO8859-6", "ISO-8859-6");
        Encodings.put("ISO8859_6", "ISO-8859-6");
        Encodings.put("ISO8859-7", "ISO-8859-7");
        Encodings.put("ISO8859_7", "ISO-8859-7");
        Encodings.put("ISO8859-9", "ISO-8859-9");
        Encodings.put("ISO8859_9", "ISO-8859-9");
        Encodings.put("ISO8859-13", "ISO-8859-13");
        Encodings.put("ISO8859_13", "ISO-8859-13");
        Encodings.put("ISO8859-15", "ISO-8859-15");
        Encodings.put("ISO8859_15", "ISO-8859-15");
        Encodings.put("KOI8R","KOI8-R");
    }
    
    
    
    private BufferedWriter out =
        new BufferedWriter(new OutputStreamWriter(System.out));

    private Method method = new XMLMethod();
    private boolean omitXmlDecl = false;
    private boolean escapeUriAttr = true;
    private boolean includeContentType = true;
    private boolean standalone = false;
    private boolean indents = false;
    private boolean stripPI = false;
    private boolean stripComment = false;
    private boolean autoDTD = true;
    private boolean dummy = false;
    private int maxDepth = -1;  //TODO
    private int indentValue = -1;
    private String encoding = UTF8;
    private String version = "1.0";
    private String mediaType;  //TODO
    //private String publicId = null, systemId = null;

    private boolean enableIndent, firstElement;

    private String indentPadding;
    private int depth;
    private boolean atBol = true;

    private CharsetEncoder encoder;


    /**
     * Constructs a XMLSerializer with default XML output method.
     */
    public XMLSerializer()
    {
    }

    /**
     * Constructs a XMLSerializer with an output stream and an encoding name.
     * @param output an open output stream 
     * @param encoding the name of a supported encoding, or null to use the 
     * default platform encoding
     * @throws DataModelException for an invalid encoding
     */
    public XMLSerializer(OutputStream output, String encoding)
        throws DataModelException
    {
        setOutput(output, encoding);
    }

    /**
     * Constructs a XMLSerializer with an output writer.
     * <p>
     * Attention: Encoding must be specified separately through options. Should
     * be compatible with the writer.
     * @param output an open Writer, does not need to be buffered.
     */
    public XMLSerializer(Writer output)
    {
        try {
            setOutput(output);
        }
        catch (DataModelException ignored) {
            ;
        }
    }
    
    /**
     * Constructs a XMLSerializer with specification of an output method.
     * 
     * @param method output method name (case-insensitive): XML, XHTML, HTML,
     *        or TEXT.
     * @exception DataModelException when the method name is invalid.
     */
    public XMLSerializer(String method) throws DataModelException
    {
        setOption(METHOD, method);
    }


    /**
     * Defines or redefines the output.
     * @param output an open output stream 
     * @param encoding the name of a supported encoding, or null to use the 
     * default platform encoding
     * @throws DataModelException for an invalid encoding
     */
    public void setOutput(OutputStream output, String encoding)
        throws DataModelException
    {
        Writer w = (encoding == null)? new OutputStreamWriter(output)
                   : new OutputStreamWriter(output, Charset.forName(encoding));
        out = new BufferedWriter(w);
        // there can be some discrepancies in encoding names:
        setEncoding(encoding);
    }

    /**
     * Defines or redefines the output.
     * @throws DataModelException for an invalid encoding
     */
    public void setOutput(Writer output)
        throws DataModelException
    {
        if (output instanceof OutputStreamWriter)
            setEncoding(((OutputStreamWriter) output).getEncoding());
        if (output != null)
            out = new BufferedWriter(output);
    }

    /**
     * Defines or redefines the output.
     * <p>
     * The encoding is UTF-8.
     * @throws DataModelException 
     */
    public void setOutput(OutputStream output) throws DataModelException
    {
        setOutput(output, UTF8);
    }

    /**
     * Gets the current output as a BufferedWriter.
     */
    public BufferedWriter getOutput()
    {
        return out;
    }


    /**
     * Returns the current encoding.
     * <p>
     * The encoding can have been defined by setOutput or by setOption.
     * @return the current encoding as a String (default is UTF-8)
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * Sets the option ENCODING.
     * Caution: does not affect the encoding of the output stream if any.
     * @param encoding a supported encoding or null for UTF-8
     * @throws DataModelException for an invalid encoding
     */
    public void setEncoding(String encoding) throws DataModelException
    {
        if(encoding == null) {
            this.encoding = UTF8;
            encoder = null;
            return;
        }
        // canonicalize encoding name:
        // (there can be some discrepancies in encoding names)
        String canon = (String) Encodings.get(encoding);
        if(canon == null)
            canon = encoding;
        this.encoding = canon;
        try {
            Charset charset = Charset.forName(canon);
            encoder = charset.newEncoder();
        }
        catch (IllegalArgumentException e) {
            throw new DataModelException(e.getMessage());
        }
    }

    public boolean hasAutoDTD()
    {
        return autoDTD;
    }

    /**
     * Sets the option OMIT_XML_DECLARATION
     * @param omit true to omit the declaration
     */
    public void setOmitXMLDeclaration(boolean omit)
    {
        omitXmlDecl = omit;
    }
    
    /**
     * Serializes a node and its subtree. Calls reset() before serialization
     * and flush() after serialization.
     * @param node node to serialize. If it is not a document, the XML header
     *        is not generated.
     * @throws DataModelException wraps an output error
     */
    public void output(Node node)
        throws DataModelException
    {
        reset();
        putNodeCopy(node, 0);
        flush();
    }

    public void putNodeCopy(Node node, int copyNsMode)
        throws DataModelException
    {
        // auto DTD:
        if(autoDTD && !omitXmlDecl && node instanceof BasicNode) {
            String[] dtdInfo = ((BasicNode) node).getDTDInfo();
            if(dtdInfo != null) {
                dtdName = dtdInfo[0];
                dtdSystemId = dtdInfo[1];
                if(dtdSystemId != null && dtdSystemId.length() == 0)
                    dtdSystemId = null;
                dtdPublicId = dtdInfo[2];
                if(dtdPublicId != null && dtdPublicId.length() == 0)
                    dtdPublicId = null;
            }
        }
        super.putNodeCopy(node, copyNsMode);
    }

    /**
     * Serializes a node and its subtree as a string.
     * @param node node to serialize. If it is not a document, the XML header
     *        is not generated.
     * @return a String representing the serialization of the node. The
     *         'encoding' option is not used.
     * @throws DataModelException wraps an output error
     */
    public String serializeToString(Node node)
        throws DataModelException
    {
        reset();
        startDocumentDone = true;
        StringWriter out = new StringWriter(20);
        setOutput(out);
        // putDocumentStart();
        putNodeCopy(node, 0);
        // putDocumentEnd();
        flush();
        return out.toString();
    }

    /**
     * Sets an option.
     * <p>
     * Supported options:
     * <ul>
     * <li>"method" : value is "XML" (default), "XHTML", "HTML" or "TEXT"
     * <li>"encoding" : an encoding supported by Java.
     * <li>"version" : simply generated in XML declaration, not checked.
     * <li>"omit-xml-declaration" : value is "yes" or "no".
     * <li>"standalone" : value is "yes" or "no".
     * <li>"doctype-system" : system id of DTD.
     * <li>"doctype-public" : public id of DTD.
     * <li>"media-type" :
     * <li>"escape-uri-attributes" : value is "yes" or "no".
     * <li>"include-content-type" :
     * <li>"indent" : value is "yes" or "no".
     * <li>"indent-value" : integer number of spaces used for indenting
     * <li>"auto-dtd": boolean (defaults to true) if DTD information is found
     * in document, then generate a DOCTYPE declaration from it.
     * (non-standard).
     * </ul>
     * 
     * @param option name of the option (see above).
     * @param value option value in string form.
     * @exception DataModelException on bad option name or value.
     */
    public void setOption(String option, String value)
        throws DataModelException
    {
        if (option.equalsIgnoreCase(METHOD)) {
            if (value.equalsIgnoreCase("XML")) {
                method = new XMLMethod();
            }
            else if (value.equalsIgnoreCase("XHTML")) {
                method = new HTMLMethod(true);
            }
            else if (value.equalsIgnoreCase("HTML")) {
                method = new HTMLMethod(false);
                omitXmlDecl = true;
                setEncoding("ISO-8859-1");
            }
            else if (value.equalsIgnoreCase("TEXT")) {
                omitXmlDecl = true;
                method = new TextMethod();
            }
            else
                throw new DataModelException("invalid method: " + value);
        }
        else if (option.equalsIgnoreCase(ENCODING))
            setEncoding(value);
        else if (option.equalsIgnoreCase(VERSION))
            version = value;
        else if (option.equalsIgnoreCase(OMIT_XML_DECLARATION))
            omitXmlDecl = boolOption(option, value);
        else if (option.equalsIgnoreCase(STANDALONE))
            standalone = boolOption(option, value);
        else if (option.equalsIgnoreCase(DOCTYPE_SYSTEM))
            dtdSystemId = value;
        else if (option.equalsIgnoreCase(DOCTYPE_PUBLIC))
            dtdPublicId = value;
        else if (option.equalsIgnoreCase(MEDIA_TYPE))
            mediaType = value;
        else if (option.equalsIgnoreCase(ESCAPE_URI_ATTRIBUTES))
            escapeUriAttr = boolOption(option, value);
        else if (option.equalsIgnoreCase(INCLUDE_CONTENT_TYPE))
            includeContentType = boolOption(option, value);
        else if (option.equalsIgnoreCase(INDENT)) {
            indents = boolOption(option, value);
            if(indents && indentValue < 0)
                setIndent(2);    // default
        }
        else if (option.equalsIgnoreCase(INDENT_VALUE))
            setIndent(Integer.parseInt(value));
        else if (option.equalsIgnoreCase(STRIP_PI))
            stripPI = boolOption(option, value);
        else if (option.equalsIgnoreCase(STRIP_COMMENT))
            stripComment = boolOption(option, value);
        else if (option.equalsIgnoreCase(AUTO_DTD))
            autoDTD = boolOption(option, value);
        else if (option.equalsIgnoreCase("dummy-display"))
            dummy = boolOption(option, value);
        else
            throw new DataModelException("invalid option: " + option);
    }

    private boolean boolOption(String option, String v)
        throws DataModelException
    {
        if (v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("true")
            || v.equals("1"))
            return true;
        if (v.equalsIgnoreCase("no") || v.equalsIgnoreCase("false")
            || v.equals("0"))
            return false;
        throw new DataModelException("invalid value of option '" + option
                                     + "': " + v);
    }

    /**
     * Extension: defines the number of spaces used for one level of
     * indentation.
     * @param value the number of spaces for indentation. If < 0, disables
     * indentation.
     */
    public void setIndent(int value)
    {
        indents = value >= 0;
        indentValue = value;
        int ind = Math.min(indentValue, 16);
        indentPadding = indents ? "                ".substring(0, ind) : null;
    }

    // ------------ interface XMLPushStream: ------------------------------

    /**
     * Prepares the serialization of another tree.
     */
    public void reset()
    {
        super.reset();
        depth = 0;
        enableIndent = false;
        atBol = true;
        // trace = false;
        firstElement = true;
        if (standalone || !UTF8.equals(encoding))
            omitXmlDecl = false;
        
    }

    /**
     * Flush of the output flow.
     * <p>
     * It is strongly recommended to call this method at end of a serialization
     * except if you use {@link #output(Node)} which calls it, or if the Node
     * to serialize is a document-node (because putDocumentEnd calls this
     * method automatically).
     * @exception DataModelException wraps a possible output error
     *            (IOException).
     */
    public void flush()
        throws DataModelException
    {
        if (indents && !atBol)
            println();
        try {
            out.flush();
        }
        catch (java.io.IOException e) {
            throw new DataModelException("IO error", e);
        }
    }

    public boolean putDocumentStart()
        throws DataModelException
    {
        if (startDocumentDone)
            return false;
        
        boolean ok = super.putDocumentStart();
        method.outputHeader();
        return ok;
    }

    public void putDocumentEnd()
        throws DataModelException
    {
        super.putDocumentEnd();
        try {
            out.flush();
        }
        catch (IOException e) {
        }
    }

    protected void flushElementStart(boolean empty) throws DataModelException
    {
        if (!elementStarted || method.onlyText())
            return;
        if (firstElement) { 
            // TODO problem with leading PI/comments
            if (!omitXmlDecl && (dtdPublicId != null || dtdSystemId != null)) {
                print("<!DOCTYPE ");
                if(dtdName == null)
                    dtdName = elementName.getLocalPart();
                print(dtdName);
                if (dtdPublicId != null) {
                    print(" PUBLIC \"");
                    print(dtdPublicId);
                    print("\"");
                    if (dtdSystemId != null) { // should not be void
                        print(" \"");
                        print(dtdSystemId);
                        print("\"");
                    }
                }
                else if (dtdSystemId != null) {
                    print(" SYSTEM \"");
                    print(dtdSystemId);
                    print("\"");
                }
                print(">");
                println();
            }
            firstElement = false;
        }

        completeNameMappings();

        // Output:
        doIndent();
        print('<');
        printQName(elementName, /* attr */false);
        for (int a = 0; a < attrCnt; a++) {
            print(' ');
            printQName(attrNames[a], /* attr */true);
            print("=\"");
            method.outputAttr(attrValues[a], attrNames[a]);
            print('\"');
        }

        // generate xmlns: pseudo-attributes for added NS:
        // Only those defined on current node
        for (int n = nsContext.mark(), cnt = nsContext.size(); n < cnt; n++) {
            print(" xmlns");
            String ns = nsContext.getNamespaceURI(n);
            if (ns == null)
                ns = "";
            String prefix = nsContext.getPrefix(n);
            if (prefix.length() > 0) {
                print(':');
                print(prefix);
            }
            print("=\"");
            method.outputAttr(ns, null);
            print("\"");
        }
        print(empty ? method.endOfEmptyTag() : ">");

        elementStarted = false;
        enableIndent = true;
        spaceNeeded = false;
        method.afterStartTag();
        ++depth;
    }

    public void putElementEnd(QName name)
        throws DataModelException
    {
        if (method.onlyText())
            return;
        boolean noEndTag = elementStarted;
        if (elementStarted) { // empty content
            flushElementStart(noEndTag = method.isMinimized(name));
        }
        --depth;
        if (depth < 0)
            throw new DataModelException("unbalanced element " + name);
        if (!noEndTag) {
            doIndent();
            print("</");
            printQName(name, false);
            print('>');
        }
        super.putElementEnd(name);
        enableIndent = true;
        spaceNeeded = false;
    }

    public void putText(String value) throws DataModelException
    {
        if (trace)
            System.err.println("-- text |" + value + '|');
        if (value == null || value.length() == 0)
            return;
        if (elementStarted)
            flushElementStart(false);
        // detect ignorable WS
        int c = value.length();
        if (indentPadding != null)
            for (; --c >= 0;)
                if (!Character.isWhitespace(value.charAt(c)))
                    break;
        enableIndent = c < 0; // may indent after ignorable WS
        method.outputText(value);
        spaceNeeded = false;
    }

    public void putAtomText(String value)
        throws DataModelException
    {
        if (spaceNeeded && !atBol)   // special
            putText(" ");
        putText(value);
        spaceNeeded = true;
    }


    public void putProcessingInstruction(String target, String contents)
        throws DataModelException
    {
        super.putProcessingInstruction(target, contents);
        if (method.onlyText() || stripPI)
            return;
        doIndent();
        print("<?");
        print(target);
        print(' ');
        print(contents);
        print(method.endOfPI());
        spaceNeeded = false;
    }

    public void putComment(String contents)
        throws DataModelException
    {
        super.putComment(contents);
        if (method.onlyText() || stripComment)
            return;
        doIndent();
        print("<!--");
        print(contents);
        print("-->");
        spaceNeeded = false;
    }

    void printQName(QName name, boolean isAttribute)
    {
        String ns = name.getNamespaceURI(); // interned
        if (ns == NamespaceContext.XML) { // needs not be declared
            print("xml:");
            print(name.getLocalPart());
        }
        else {
            String prefix = nsContext.getPrefix(ns);

            if (prefix != null && prefix.length() > 0) {
                print(prefix);
                print(':');
            }
            print(name.getLocalPart());
        }
    }

    private void doIndent()
    {
        if (enableIndent && indentPadding != null) {
            println();
            for (int i = 0; i < depth; i++)
                print(indentPadding);
            enableIndent = false;
            atBol = false;
        }
    }

    private void print(String s)
    {
        if (maxVolume > 0 && volume > maxVolume)
            return;
        try {
            if (!dummy)
                out.write(s);
            volume += s.length();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print(CharSequence s)
    {
        if (maxVolume > 0 && volume > maxVolume)
            return;
        try {
            int length = s.length();
            if (!dummy)
                for (int i = 0; i < length; i++)
                    out.write(s.charAt(i));
            volume += length;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print(char c)
    {
        if (maxVolume > 0 && volume > maxVolume || dummy)
            return;
        try {
            if(XMLUtil.isSurrogateChar(c)) {
                // TODO
            }
            else if(encoder != null && !encoder.canEncode(c)) {
                print("&#"); print(Integer.toString(c)); print(";");
            }
            else
                out.write(c);
            volume++;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Outputs an end-of-line.
     */
    public void println()
    {
        if (maxVolume > 0 && volume > maxVolume)
            return;
        try {
            if (!dummy)
                out.newLine();
            volume++;
            atBol = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected interface Method
    {
        boolean onlyText();
    
        boolean isMinimized(QName elementName);
    
        void afterStartTag();
    
        String endOfEmptyTag();
    
        String endOfPI();
    
        void outputAttr(String s, QName attrName);
    
        void outputText(CharSequence s);
    
        void outputHeader();
    }

    protected class XMLMethod
        implements Method
    {

        public boolean onlyText()
        {
            return false;
        }

        public String endOfEmptyTag()
        {
            return "/>";
        }

        public void afterStartTag()
        {
        }

        public boolean isMinimized(QName element)
        {
            return true;
        }

        public void outputText(CharSequence s)
        {
            for (int i = 0, L = s.length(); i < L; i++) {
                char c = s.charAt(i);
                switch (c) {
                case '\n':
                    println();
                    break;
                case '\r':
                    print("&#13;");
                    break;
                case '&':
                    print("&amp;");
                    break;
                case '<':
                    print("&lt;");
                    break;
                case '>':
                    print("&gt;");
                    break;
                default:
                    print(c);
                    break;
                }
            }
        }

        public void outputAttr(String s, QName attrName)
        {
            for (int i = 0, L = s.length(); i < L; i++) {
                char c = s.charAt(i);
                switch (c) {
                case '\t':
                    print("&#9;");
                    break;
                case '\n':
                    print("&#10;");
                    break;
                case '\r':
                    print("&#13;");
                    break;
                case '&':
                    print("&amp;");
                    break;
                case '<':
                    print("&lt;");
                    break;
                case '"':
                    print("&quot;");
                    break;
                default:
                    print(c);
                    break;
                }
            }
        }

        public String endOfPI()
        {
            return "?>";
        }

        public void outputHeader()
        {
            if (!omitXmlDecl) {
                print("<?xml version='");
                print(version);
                print('\'');
                print(" encoding='" + encoding + '\'');
                print("?>");
                println();
            }
        }
    }

    protected class HTMLMethod extends XMLMethod
    {
        boolean xhtml;

        boolean noEscape = false;

        HTMLMethod(boolean xhtml)
        {
            this.xhtml = xhtml;
            if(!xhtml)
                dtdPublicId = "-//W3C//DTD HTML 4.01 Transitional//EN";
        }

        boolean isHTML(String ns) // ns assumed to be interned
        { // recognize xhtml even in HTML
            return ns == XHTML || (!xhtml && ns == NamespaceContext.EMPTY);
        }

        public String endOfEmptyTag()
        {
            // required space in XHTML: for old user agents
            return xhtml ? " />" : ">";
        }

        public boolean isMinimized(QName tag)
        {
            if (!isHTML(tag.getNamespaceURI()))
                return false;
            String name = tag.getLocalPart();
            switch (Character.toLowerCase(name.charAt(0))) {
            case 'a':
                return name.equalsIgnoreCase("area");
            case 'b':
                return name.equalsIgnoreCase("br")
                       || name.equalsIgnoreCase("base")
                       || name.equalsIgnoreCase("basefont");
            case 'c':
                return name.equalsIgnoreCase("col");
            case 'f':
                return name.equalsIgnoreCase("frame");
            case 'h':
                return name.equalsIgnoreCase("hr");
            case 'i':
                return name.equalsIgnoreCase("img")
                       || name.equalsIgnoreCase("input")
                       || name.equalsIgnoreCase("isindex");
            case 'l':
                return name.equalsIgnoreCase("link");
            case 'm':
                return name.equalsIgnoreCase("meta");
            case 'p':
                return name.equalsIgnoreCase("param");
            default:
                return false;
            }
        }

        public void afterStartTag()
        {
            if (isHTML(elementName.getNamespaceURI())) {
                String ncname = elementName.getLocalPart();
                // include meta after head ?
                if (includeContentType && ncname.equalsIgnoreCase("head")) {
                    print("<meta http-equiv='Content-Type' content='text/html; charset="
                          + encoding + "'" + endOfEmptyTag());
                }
                // do not escape SCRIPT and STYLE
                if (!xhtml
                    && (ncname.equalsIgnoreCase("script") ||
                        ncname.equalsIgnoreCase("style")))
                    noEscape = true;
            }
        }
        
        public void outputHeader()
        {
            // no <?xml, DOCTYPE done normally
            if(xhtml)
                super.outputHeader();
        }

        public void outputText(CharSequence s)
        {
            if (noEscape) {
                print(s);
                noEscape = false;
                return;
            }
            for (int i = 0, L = s.length(); i < L; i++) {
                char c = s.charAt(i);
                switch (c) {
                case '\n':
                    println();
                    break;
                case '\r':
                    print("&#13;");
                    break;
                case '&':
                    print("&amp;");
                    break;
                case '<':
                    print("&lt;");
                    break;
                case '>':
                    print("&gt;");
                    break;
                default:
                    outputHTMLChar(c);
                    break;
                }
            }
        }

        public void outputAttr(String s, QName attrName)
        {
            if(escapeUriAttr && attrName != null && isUriAttribute(attrName))
                s = Util.escapeHtmlURI(s);
            boolean prevBlank = false;
            for (int i = 0, L = s.length(); i < L; i++) {
                char c = s.charAt(i);
                switch (c) {
                case '\n':
                case '\r':
                case ' ':
                    if (!prevBlank)
                        print(' '); // collapse spaces
                    prevBlank = true;
                    break;
                case '&':
                    print("&amp;");
                    break;
                case '<':
                    if (xhtml)
                        print("&lt;");
                    else
                        print(c);
                    break;
                case '"':
                    print("&#34;");
                    break;
                default:
                    outputHTMLChar(c);
                    prevBlank = false;
                    break;
                }
            }
        }

        private boolean isUriAttribute(QName attrName)
        {
            String name = attrName.getLocalPart();
            switch(Character.toLowerCase(name.charAt(0))) {
            case 'a':
                return name.equalsIgnoreCase("action") ||
                       name.equalsIgnoreCase("archive");
            case 'b':
                return name.equalsIgnoreCase("background");
            case 'c':
                return name.equalsIgnoreCase("cite") ||
                       name.equalsIgnoreCase("classid") ||
                       name.equalsIgnoreCase("codebase");
            case 'd':
                return name.equalsIgnoreCase("data") ||
                       name.equalsIgnoreCase("datasrc");
            case 'f':
                return name.equalsIgnoreCase("for");
            case 'h':
                return name.equalsIgnoreCase("href");
            case 'l':
                return name.equalsIgnoreCase("longdesc");
            case 'n':
                return name.equalsIgnoreCase("name");
            case 'p':
                return name.equalsIgnoreCase("profile");
            case 's':
                return name.equalsIgnoreCase("src");
            case 'u':
                return name.equalsIgnoreCase("usemap");
            default:
                return false;
            }
        }

        private void outputHTMLChar(char c)
        {
            // done only if necessary:
            if(!xhtml && encoder != null && !encoder.canEncode(c)) {
                String eq = (String) HTML4Entities.get(c);
                if(eq != null) {
                    print("&"); print(eq); print(";");
                    return;
                }
            }
            // normal case
            print(c);
        }

        public String endOfPI()
        {
            return xhtml ? "?>" : ">";
        }
    }

    static CharTable HTML4Entities = new CharTable();
    static {
        HTML4Entities.put('&', "amp");
        HTML4Entities.put(160, "nbsp");
        // -- Greek --
        HTML4Entities.put(913, "Alpha");
        HTML4Entities.put(914, "Beta");
        HTML4Entities.put(915, "Gamma");
        HTML4Entities.put(916, "Delta");
        HTML4Entities.put(917, "Epsilon");
        HTML4Entities.put(918, "Zeta");
        HTML4Entities.put(919, "Eta");
        HTML4Entities.put(920, "Theta");
        HTML4Entities.put(921, "Iota");
        HTML4Entities.put(922, "Kappa");
        HTML4Entities.put(923, "Lambda");
        HTML4Entities.put(924, "Mu");
        HTML4Entities.put(925, "Nu");
        HTML4Entities.put(926, "Xi");
        HTML4Entities.put(927, "Omicron");
        HTML4Entities.put(928, "Pi");
        HTML4Entities.put(929, "Rho");
        HTML4Entities.put(931, "Sigma");
        HTML4Entities.put(932, "Tau");
        HTML4Entities.put(933, "Upsilon");
        HTML4Entities.put(934, "Phi");
        HTML4Entities.put(935, "Chi");
        HTML4Entities.put(936, "Psi");
        HTML4Entities.put(937, "Omega");
        HTML4Entities.put(945, "alpha");
        HTML4Entities.put(946, "beta");
        HTML4Entities.put(947, "gamma");
        HTML4Entities.put(948, "delta");
        HTML4Entities.put(949, "epsilon");
        HTML4Entities.put(950, "zeta");
        HTML4Entities.put(951, "eta");
        HTML4Entities.put(952, "theta");
        HTML4Entities.put(953, "iota");
        HTML4Entities.put(954, "kappa");
        HTML4Entities.put(955, "lambda");
        HTML4Entities.put(956, "mu");
        HTML4Entities.put(957, "nu");
        HTML4Entities.put(958, "xi");
        HTML4Entities.put(959, "omicron");
        HTML4Entities.put(960, "pi");
        HTML4Entities.put(961, "rho");
        HTML4Entities.put(962, "sigmaf");
        HTML4Entities.put(963, "sigma");
        HTML4Entities.put(964, "tau");
        HTML4Entities.put(965, "upsilon");
        HTML4Entities.put(966, "phi");
        HTML4Entities.put(967, "chi");
        HTML4Entities.put(968, "psi");
        HTML4Entities.put(969, "omega");
        HTML4Entities.put(977, "thetasym");
        HTML4Entities.put(978, "upsih");
        HTML4Entities.put(982, "piv");

        // -- General Punctuation --
        HTML4Entities.put(8226, "bull");
        HTML4Entities.put(8230, "hellip");
        HTML4Entities.put(8242, "prime");
        HTML4Entities.put(8243, "Prime");
        HTML4Entities.put(8254, "oline");
        HTML4Entities.put(8260, "frasl");

        // -- Letterlike Symbols --
        HTML4Entities.put(8472, "weierp");
        HTML4Entities.put(8465, "image");
        HTML4Entities.put(8476, "real");
        HTML4Entities.put(8482, "trade");
        HTML4Entities.put(8501, "alefsym");

        // -- Arrows --
        HTML4Entities.put(8592, "larr");
        HTML4Entities.put(8593, "uarr");
        HTML4Entities.put(8594, "rarr");
        HTML4Entities.put(8595, "darr");
        HTML4Entities.put(8596, "harr");
        HTML4Entities.put(8629, "crarr");
        HTML4Entities.put(8656, "lArr");
        HTML4Entities.put(8657, "uArr");
        HTML4Entities.put(8658, "rArr");
        HTML4Entities.put(8659, "dArr");
        HTML4Entities.put(8660, "hArr");

        // -- Mathematical Operators --
        HTML4Entities.put(8704, "forall");
        HTML4Entities.put(8706, "part");
        HTML4Entities.put(8707, "exist");
        HTML4Entities.put(8709, "empty");
        HTML4Entities.put(8711, "nabla");
        HTML4Entities.put(8712, "isin");
        HTML4Entities.put(8713, "notin");
        HTML4Entities.put(8715, "ni");
        HTML4Entities.put(8719, "prod");
        HTML4Entities.put(8721, "sum");
        HTML4Entities.put(8722, "minus");
        HTML4Entities.put(8727, "lowast");
        HTML4Entities.put(8730, "radic");
        HTML4Entities.put(8733, "prop");
        HTML4Entities.put(8734, "infin");
        HTML4Entities.put(8736, "ang");
        HTML4Entities.put(8743, "and");
        HTML4Entities.put(8744, "or");
        HTML4Entities.put(8745, "cap");
        HTML4Entities.put(8746, "cup");
        HTML4Entities.put(8747, "int");
        HTML4Entities.put(8756, "there4");
        HTML4Entities.put(8764, "sim");
        HTML4Entities.put(8773, "cong");
        HTML4Entities.put(8776, "asymp");
        HTML4Entities.put(8800, "ne");
        HTML4Entities.put(8801, "equiv");
        HTML4Entities.put(8804, "le");
        HTML4Entities.put(8805, "ge");
        HTML4Entities.put(8834, "sub");
        HTML4Entities.put(8835, "sup");
        HTML4Entities.put(8836, "nsub");
        HTML4Entities.put(8838, "sube");
        HTML4Entities.put(8839, "supe");
        HTML4Entities.put(8853, "oplus");
        HTML4Entities.put(8855, "otimes");
        HTML4Entities.put(8869, "perp");
        HTML4Entities.put(8901, "sdot");

        // -- Miscellaneous Technical --
        HTML4Entities.put(8968, "lceil");
        HTML4Entities.put(8969, "rceil");
        HTML4Entities.put(8970, "lfloor");
        HTML4Entities.put(8971, "rfloor");
        HTML4Entities.put(9001, "lang");
        HTML4Entities.put(9002, "rang");

        // -- Geometric Shapes --
        HTML4Entities.put(9674, "loz");

        // -- Miscellaneous Symbols --
        HTML4Entities.put(9824, "spades");
        HTML4Entities.put(9827, "clubs");
        HTML4Entities.put(9829, "hearts");
        HTML4Entities.put(9830, "diams");
        
        HTML4Entities.put(34, "quot");
        HTML4Entities.put(38, "amp");
        HTML4Entities.put(60, "lt");
        HTML4Entities.put(62, "gt");

        // -- Latin Extended-A --
        HTML4Entities.put(338, "OElig");
        HTML4Entities.put(339, "oelig");
        HTML4Entities.put(352, "Scaron");
        HTML4Entities.put(353, "scaron");
        HTML4Entities.put(376, "Yuml");

        // -- Spacing Modifier Letters --
        HTML4Entities.put(710, "circ");
        HTML4Entities.put(732, "tilde");
        // -- General Punctuation --
        HTML4Entities.put(8194, "ensp");
        HTML4Entities.put(8195, "emsp");
        HTML4Entities.put(8201, "thinsp");
        HTML4Entities.put(8204, "zwnj");
        HTML4Entities.put(8205, "zwj");
        HTML4Entities.put(8206, "lrm");
        HTML4Entities.put(8207, "rlm");
        HTML4Entities.put(8211, "ndash");
        HTML4Entities.put(8212, "mdash");
        HTML4Entities.put(8216, "lsquo");
        HTML4Entities.put(8217, "rsquo");
        HTML4Entities.put(8218, "sbquo");
        HTML4Entities.put(8220, "ldquo");
        HTML4Entities.put(8221, "rdquo");
        HTML4Entities.put(8222, "bdquo");
        HTML4Entities.put(8224, "dagger");
        HTML4Entities.put(8225, "Dagger");
        HTML4Entities.put(8240, "permil");
        HTML4Entities.put(8249, "lsaquo");
        HTML4Entities.put(8250, "rsaquo");
        HTML4Entities.put(8364, "euro");
    }
    
    protected class TextMethod extends XMLMethod
    {
        public void outputHeader() {
        }
        
        public boolean onlyText()
        {
            return true;
        }

        public void outputText(CharSequence s)
        {
            print(s);
        }
    }
}
