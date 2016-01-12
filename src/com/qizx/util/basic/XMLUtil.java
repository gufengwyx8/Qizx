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
/*
 * Copyright (c) 2001-2003 Pixware. 
 *
 * Author: Hussein Shafie
 * Modified by Xavier Franc
 */
package com.qizx.util.basic;

import java.util.StringTokenizer;

/**
 * A collection of utility functions (static methods) related to XML.
 */
public class XMLUtil
{
    // supplemental character range:
    private static final int SUPPLEM_LB = 0x10000;
    private static final int SUPPLEM_UB = 0x10FFFF;
    // High surrogate range:
    private static final char HI_SURROG_LB = 0xd800;   // lower bound
    private static final char HI_SURROG_UB = 0xdbff;   // upper bound
    private static final char LO_SURROG_LB = 0xdc00;
    private static final char LO_SURROG_UB = 0xdf00;
    private static final char SURROG_LB = HI_SURROG_LB; // lower bound
    private static final char SURROG_UB = LO_SURROG_UB; // upper bound

    /**
     * Tests if specified string is a lexically correct target for a process
     * instruction.
     * <p>
     * Note that Names starting with "<tt>xml</tt>" (case-insensitive) are
     * rejected.
     * 
     * @param s string to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     *         otherwise
     */
    public static final boolean isPITarget(String s)
    {
        if (s == null || s.length() == 0)
            return false;
        return (isName(s) &&
                !s.regionMatches(/* ignoreCase */true, 0, "xml", 0, 3));
    }

    /**
     * Tests if specified string is a lexically correct NMTOKEN.
     * 
     * @param s string to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     *         otherwise
     */
    public static final boolean isNmtoken(String s)
    {
        int count;
        if (s == null || (count = s.length()) == 0)
            return false;

        for (int i = 0; i < count; ++i) {
            if (!isNameChar(s.charAt(i)))
                return false;
        }
        return true;
    }
    
    /**
     * Tests if specified string is a lexically correct NCName.
     * 
     * @param s string to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     * otherwise
     */
    public static final boolean isNCName(String s)
    {
        int count;
        if (s == null || (count = s.length()) == 0)
            return false;
        
        char c = s.charAt(0);
        switch (c) {
        case '_':
            break;
        default:
            if (!Character.isLetter(c))
                return false;
        }
        
        for (int i = 1; i < count; ++i) {
            c = s.charAt(i);
            if (c == ':' || !isNameChar(c))
                return false;
        }
        
        return true;
    }
    
    /**
     * Tests if specified string is a lexically correct Name.
     * 
     * @param s string to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     *         otherwise
     */
    public static final boolean isName(String s)
    {
        int count;
        if (s == null || (count = s.length()) == 0)
            return false;

        char c = s.charAt(0);
        switch (c) {
        case '_':
        case ':':
            break;
        default:
            if (!Character.isLetter(c))
                return false;
        }

        for (int i = 1; i < count; ++i) {
            if (!isNameChar(s.charAt(i)))
                return false;
        }

        return true;
    }
    
    public static final boolean isNameChar(char c)
    {
        switch (c) {
        case '.':
        case '-':
        case '_':
        case ':':
            break;
            
        case 0x05BF: case 0x05C4: case 0x0670: case 0x093C: case 0x094D: 
        case 0x09BC: case 0x09BE: case 0x09BF: case 0x09D7: case 0x0A02: 
        case 0x0A3C: case 0x0A3E: case 0x0A3F: case 0x0ABC: case 0x0B3C: 
        case 0x0BD7: case 0x0D57: case 0x0E31: case 0x0EB1: case 0x0F35: 
        case 0x0F37: case 0x0F39: case 0x0F3E: case 0x0F3F: case 0x0F97: 
        case 0x0FB9: case 0x20E1: case 0x3099: case 0x309A:
            // CombiningChar.
            break;
            
        case 0x00B7: case 0x02D0: case 0x02D1: case 0x0387: case 0x0640: 
        case 0x0E46: case 0x0EC6: case 0x3005:
            // Extender.
            break;
            
        default:
            if (Character.isLetterOrDigit(c))
                break;

            if ((c >= 0x0300 && c <= 0x0345)
                || (c >= 0x0360 && c <= 0x0361)
                || (c >= 0x0483 && c <= 0x0486)
                || (c >= 0x0591 && c <= 0x05A1)
                || (c >= 0x05A3 && c <= 0x05B9)
                || (c >= 0x05BB && c <= 0x05BD)
                || (c >= 0x05C1 && c <= 0x05C2)
                || (c >= 0x064B && c <= 0x0652)
                || (c >= 0x06D6 && c <= 0x06DC)
                || (c >= 0x06DD && c <= 0x06DF)
                || (c >= 0x06E0 && c <= 0x06E4)
                || (c >= 0x06E7 && c <= 0x06E8)
                || (c >= 0x06EA && c <= 0x06ED)
                || (c >= 0x0901 && c <= 0x0903)
                || (c >= 0x093E && c <= 0x094C)
                || (c >= 0x0951 && c <= 0x0954)
                || (c >= 0x0962 && c <= 0x0963)
                || (c >= 0x0981 && c <= 0x0983)
                || (c >= 0x09C0 && c <= 0x09C4)
                || (c >= 0x09C7 && c <= 0x09C8)
                || (c >= 0x09CB && c <= 0x09CD)
                || (c >= 0x09E2 && c <= 0x09E3)
                || (c >= 0x0A40 && c <= 0x0A42)
                || (c >= 0x0A47 && c <= 0x0A48)
                || (c >= 0x0A4B && c <= 0x0A4D)
                || (c >= 0x0A70 && c <= 0x0A71)
                || (c >= 0x0A81 && c <= 0x0A83)
                || (c >= 0x0ABE && c <= 0x0AC5)
                || (c >= 0x0AC7 && c <= 0x0AC9)
                || (c >= 0x0ACB && c <= 0x0ACD)
                || (c >= 0x0B01 && c <= 0x0B03)
                || (c >= 0x0B3E && c <= 0x0B43)
                || (c >= 0x0B47 && c <= 0x0B48)
                || (c >= 0x0B4B && c <= 0x0B4D)
                || (c >= 0x0B56 && c <= 0x0B57)
                || (c >= 0x0B82 && c <= 0x0B83)
                || (c >= 0x0BBE && c <= 0x0BC2)
                || (c >= 0x0BC6 && c <= 0x0BC8)
                || (c >= 0x0BCA && c <= 0x0BCD)
                || (c >= 0x0C01 && c <= 0x0C03)
                || (c >= 0x0C3E && c <= 0x0C44)
                || (c >= 0x0C46 && c <= 0x0C48)
                || (c >= 0x0C4A && c <= 0x0C4D)
                || (c >= 0x0C55 && c <= 0x0C56)
                || (c >= 0x0C82 && c <= 0x0C83)
                || (c >= 0x0CBE && c <= 0x0CC4)
                || (c >= 0x0CC6 && c <= 0x0CC8)
                || (c >= 0x0CCA && c <= 0x0CCD)
                || (c >= 0x0CD5 && c <= 0x0CD6)
                || (c >= 0x0D02 && c <= 0x0D03)
                || (c >= 0x0D3E && c <= 0x0D43)
                || (c >= 0x0D46 && c <= 0x0D48)
                || (c >= 0x0D4A && c <= 0x0D4D)
                || (c >= 0x0E34 && c <= 0x0E3A)
                || (c >= 0x0E47 && c <= 0x0E4E)
                || (c >= 0x0EB4 && c <= 0x0EB9)
                || (c >= 0x0EBB && c <= 0x0EBC)
                || (c >= 0x0EC8 && c <= 0x0ECD)
                || (c >= 0x0F18 && c <= 0x0F19)
                || (c >= 0x0F71 && c <= 0x0F84)
                || (c >= 0x0F86 && c <= 0x0F8B)
                || (c >= 0x0F90 && c <= 0x0F95)
                || (c >= 0x0F99 && c <= 0x0FAD)
                || (c >= 0x0FB1 && c <= 0x0FB7)
                || (c >= 0x20D0 && c <= 0x20DC)
                || (c >= 0x302A && c <= 0x302F))
                // CombiningChar
                break;

            if ((c >= 0x3031 && c <= 0x3035)
                || (c >= 0x309D && c <= 0x309E)
                || (c >= 0x30FC && c <= 0x30FE))
                // Extender.
                break;

            return false;
        }

        return true;
    }
    
    /**
     * Tests if specified character is a XML space (<tt>'\t'</tt>,
     * <tt>'\r'</tt>, <tt>'\n'</tt>, <tt>' '</tt>).
     * 
     * @param c character to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     *         otherwise
     */
    public static final boolean isXMLSpace(char c)
    {
        switch (c) {
        case ' ':
        case '\n':
        case '\r':
        case '\t':
            return true;
        default:
            return false;
        }
    }

    /**
     * Tests if all characters are XML space (<tt>'\t'</tt>, <tt>'\r'</tt>,
     * <tt>'\n'</tt>, <tt>' '</tt>).
     * 
     * @param s characters to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     *         otherwise
     */
    public static final boolean isXMLSpace(String s)
    {
        for (int c = s.length(); --c >= 0;)
            if (!isXMLSpace(s.charAt(c)))
                return false;
        return true;
    }

    /**
     * Tests if all characters are XML space (<tt>'\t'</tt>, <tt>'\r'</tt>,
     * <tt>'\n'</tt>, <tt>' '</tt>).
     * 
     * @param s characters to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     *         otherwise
     */
    public static final boolean isXMLSpace(char[] s, int start, int length)
    {
        for (int c = 0; c < length; c++)
            if (!isXMLSpace(s[c + start]))
                return false;
        return true;
    }
    
    /**
     * Tests if specified character is a character which can be contained in a
     * XML document.
     * 
     * @param c character to be tested
     * @return <code>true</code> if test is successful; <code>false</code>
     * otherwise
     */
    public static final boolean isXMLChar(int c)
    {
        // Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
        // [#x10000-#x10FFFF]
        switch (c) {
        case 0x9:
        case 0xA:
        case 0xD:
            return true;
        default:
            if ((c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD))
                return true;
        }

        return false;
    }
    
    /**
     * Tests whether the codepoint is a supplemental character (not in BMP).
     */
    public static boolean isSupplementalChar(int c)
    {
        return c >= SUPPLEM_LB && c <= SUPPLEM_UB;
    }

    /**
     * Tests whether the codepoint is a low or high surrogate code.
     */
    public static boolean isSurrogateChar(char c)
    {
        return c >= SURROG_LB && c <= SURROG_UB;
    }

    /**
     * Returns the high surrogate code of a supplemental character.
     */
    public static char highSurrogate(int c)
    {
        return (char) (((c - SUPPLEM_LB) >> 10) + HI_SURROG_LB);
    }

    /**
     * Returns the low surrogate code of a supplemental character.
     */
    public static char lowSurrogate(int c)
    {
        return (char) (((c - SUPPLEM_LB) & 0x3FF) + LO_SURROG_LB);
    }

    /**
     * Rebuilds a supplemental character codepoint from surrogate pair.
     * 
     * @param hiSurrog a valid hi surrogate code
     * @param loSurrog
     * @return supplemental character on 32 bits
     */
    public static int supplementalChar(char hiSurrog, char loSurrog)
    {
        return ((hiSurrog - HI_SURROG_LB) << 10) + (loSurrog - LO_SURROG_LB);
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Replaces successive XML space characters by a single space character (<tt>' '</tt>)
     * then removes leading and trailing space characters if any.
     * 
     * @param value string to be processed
     * @return processed string
     */
    public static final String collapseWhiteSpace(CharSequence value)
    {
        StringBuffer buffer = new StringBuffer();
        compressWhiteSpace(value, buffer);

        int last = buffer.length() - 1;
        if (last >= 0) {
            if (buffer.charAt(last) == ' ') {
                buffer.deleteCharAt(last);
                --last;
            }

            if (last >= 0 && buffer.charAt(0) == ' ')
                buffer.deleteCharAt(0);
        }

        return buffer.toString();
    }

    /**
     * Replaces successive XML space characters (<tt>'\t'</tt>,
     * <tt>'\r'</tt>, <tt>'\n'</tt>, <tt>' '</tt>) by a single space
     * character (<tt>' '</tt>).
     * 
     * @param value string to be processed
     * @return processed string
     */
    public static final String compressWhiteSpace(String value)
    {
        StringBuffer buffer = new StringBuffer();
        compressWhiteSpace(value, buffer);
        return buffer.toString();
    }

    /**
     * Replaces successive XML space characters (<tt>'\t'</tt>,
     * <tt>'\r'</tt>, <tt>'\n'</tt>, <tt>' '</tt>) by a single space
     * character (<tt>' '</tt>).
     * 
     * @param value string to be processed
     * @param buffer buffer used to store processed characters (characters are
     *        appended to this buffer)
     */
    private static void compressWhiteSpace(CharSequence value, StringBuffer buffer)
    {
        // No need to convert "\r\n" to a single '\n' because white spaces
        // are compressed.
        
        int length = value.length();
        char prevChar = '?';
        for (int i = 0; i < length; ++i) {
            char c = value.charAt(i);

            switch (c) {
            case '\t':
            case '\r':
            case '\n':
                c = ' ';
                break;
            }

            if (c == ' ') {
                if (prevChar != ' ') {
                    buffer.append(c);
                    prevChar = c;
                }
            }
            else {
                buffer.append(c);
                prevChar = c;
            }
        }
    }
    
    /**
     * Replaces sequence "<tt>\r\n</tt>" and characters <tt>'\t'</tt>,
     * <tt>'\r'</tt>, <tt>'\n'</tt> by a single space character <tt>' '</tt>.
     * 
     * @param value string to be processed
     * @return processed string
     */
    public static final String replaceWhiteSpace(String value)
    {
        StringBuffer buffer = new StringBuffer();

        int length = value.length();
        char prevChar = '?';
        for (int i = 0; i < length; ++i) {
            char c = value.charAt(i);
            switch (c) {
            case '\t':
            case '\r':
                buffer.append(' ');
                break;
            case '\n':
                // Equivalent to converting "\r\n" to a single '\n' then
                // converting '\n' to ' '.
                if (prevChar != '\r')
                    buffer.append(' ');
                break;
            default:
                buffer.append(c);
            }

            prevChar = c;
        }

        return buffer.toString();
    }
    
    /**
     * Splits specified string at XML space character boundaries (<tt>'\t'</tt>,
     * <tt>'\r'</tt>, <tt>'\n'</tt>, <tt>' '</tt>). Returns list of
     * parts.
     * 
     * @param s string to be split
     * @return list of parts
     */
    public static final String[] splitList(String s)
    {
        StringTokenizer tokens = new StringTokenizer(s, " \n\r\t");
        String[] split = new String[tokens.countTokens()];

        for (int i = 0; i < split.length; ++i)
            split[i] = tokens.nextToken();

        return split;
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Escapes specified string (that is, <tt>'&lt;'</tt> is replaced by "<tt>&amp;#60</tt>;",
     * <tt>'&amp;'</tt> is replaced by "<tt>&amp;#38;</tt>", etc).
     * 
     * @param string string to be escaped
     * @return escaped string
     */
    public static final String escapeXML(String string)
    {
        StringBuffer escaped = new StringBuffer();
        escapeXML(string, escaped);
        return escaped.toString();
    }

    /**
     * Escapes specified string (that is, <tt>'&lt;'</tt> is replaced by "<tt>&amp;#60</tt>;",
     * <tt>'&amp;'</tt> is replaced by "<tt>&amp;#38;</tt>", etc) then
     * quotes the escaped string.
     * 
     * @param string string to be escaped and quoted
     * @return escaped and quoted string
     */
    public static final String quoteXML(String string)
    {
        StringBuffer quoted = new StringBuffer();
        quoted.append('\"');
        escapeXML(string, quoted);
        quoted.append('\"');
        return quoted.toString();
    }

    /**
     * Escapes specified string (that is, <tt>'&lt;'</tt> is replaced by "<tt>&amp;#60</tt>;",
     * <tt>'&amp;'</tt> is replaced by "<tt>&amp;#38;</tt>", etc).
     * 
     * @param string string to be escaped
     * @param escaped buffer used to store escaped string (characters are
     *        appended to this buffer)
     */
    public static final void escapeXML(String string, StringBuffer escaped)
    {
        char[] chars = string.toCharArray();
        escapeXML(chars, 0, chars.length, escaped);
    }

    /**
     * Escapes specified character array (that is, <tt>'&lt;'</tt> is
     * replaced by "<tt>&amp;#60</tt>;", <tt>'&amp;'</tt> is replaced by "<tt>&amp;#38;</tt>",
     * etc).
     * 
     * @param chars character array to be escaped
     * @param offset specifies first character in array to be escaped
     * @param length number of characters in array to be escaped
     * @param escaped buffer used to store escaped string (characters are
     *        appended to this buffer)
     */
    public static final void escapeXML(char[] chars, int offset, int length,
                                       StringBuffer escaped)
    {
        escapeXML(chars, offset, length, escaped, false);
    }

    /**
     * Escapes specified character array (that is, <tt>'&lt;'</tt> is
     * replaced by "<tt>&amp;#60</tt>;", <tt>'&amp;'</tt> is replaced by "<tt>&amp;#38;</tt>",
     * etc).
     * 
     * @param chars character array to be escaped
     * @param offset specifies first character in array to be escaped
     * @param length number of characters in array to be escaped
     * @param escaped buffer used to store escaped string (characters are
     *        appended to this buffer)
     * @param ascii if true, characters with code &gt; 127 are escaped as
     *        <tt>&amp;#<i>code</i>;</tt>
     */
    public static final void escapeXML(char[] chars, int offset, int length,
                                       StringBuffer escaped, boolean ascii)
    {
        int end = offset + length;
        for (int i = offset; i < end; ++i) {
            char c = chars[i];
            switch (c) {
            case '\'':
                escaped.append("&#39;");
                break;
            case '\"':
                escaped.append("&#34;");
                break;
            case '<':
                escaped.append("&#60;");
                break;
            case '>':
                escaped.append("&#62;");
                break;
            case '&':
                escaped.append("&#38;");
                break;
            case 0x00A0:
                // &nbsp;
                escaped.append("&#x00A0;");
                break;
            default:
                if (ascii && c > 127) {
                    escaped.append("&#");
                    escaped.append(Integer.toString((int) c));
                    escaped.append(';');
                }
                else {
                    escaped.append(c);
                }
            }
        }
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Unescapes specified string. Inverse operation of escapeXML(...).
     * 
     * @param text string to be unescaped
     * @return unescaped string
     */
    public static final String unescapeXML(String text)
    {
        StringBuffer unescaped = new StringBuffer();
        unescapeXML(text, 0, text.length(), unescaped);
        return unescaped.toString();
    }

    /**
     * Unescapes specified string. Inverse operation of escapeXML().
     * 
     * @param text string to be unescaped
     * @param offset specifies first character in string to be unescaped
     * @param length number of characters in string to be unescaped
     * @param unescaped buffer used to store unescaped string (characters are
     *        appended to this buffer)
     */
    public static final void unescapeXML(String text, int offset, int length,
                                         StringBuffer unescaped)
    {
        int end = offset + length;

        for (int i = offset; i < end; ++i) {
            char c = text.charAt(i);

            if (c == '&') {
                StringBuffer charRef = new StringBuffer();

                ++i;
                while (i < end) {
                    c = text.charAt(i);
                    if (c == ';')
                        break;
                    charRef.append(c);
                    ++i;
                }

                c = parseCharRef(charRef.toString());
            }

            unescaped.append(c);
        }
    }

    private static char parseCharRef(String charRef)
    {
        if (charRef.length() >= 2 && charRef.charAt(0) == '#') {
            int i;

            try {
                if (charRef.charAt(1) == 'x')
                    i = Integer.parseInt(charRef.substring(2), 16);
                else
                    i = Integer.parseInt(charRef.substring(1));
            }
            catch (NumberFormatException e) {
                i = -1;
            }

            if (i < 0 || i > Character.MAX_VALUE)
                return '?';
            else
                return (char) i;
        }
        if (charRef.equals("amp")) {
            return '&';
        }
        if (charRef.equals("apos")) {
            return '\'';
        }
        if (charRef.equals("quot")) {
            return '\"';
        }
        if (charRef.equals("lt")) {
            return '<';
        }
        if (charRef.equals("gt")) {
            return '>';
        }
        else {
            return '?';
        }
    }
    
    // -----------------------------------------------------------------------
    
    private static int uniqueId = 0;
    
    /**
     * Returns a unique ID which can be used for example as the value of an
     * attribute of type ID.
     */
    public static final String getUniqueId()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("___");
        buffer.append(Long.toString(System.currentTimeMillis(),
                                    Character.MAX_RADIX));
        buffer.append(".");
        buffer.append(Integer.toString(uniqueId++, Character.MAX_RADIX));

        return buffer.toString();
    }

    public static String normalizePI(String contents)
    {
        if(contents == null || contents.indexOf("?>") >= 0)
            return null;
        int i = 0, len = contents.length();
        for(; i < len; i++)
            if(!Character.isWhitespace(contents.charAt(i)))
                break;
        return (i == 0)? contents : contents.substring(i);
    }

    public static boolean checkComment(String contents)
    {
        if(contents == null)
            return true; // ?
        return contents.indexOf("--") < 0 &&
               !contents.startsWith("-") && !contents.endsWith("-");
    }
}
