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
package com.qizx.util.basic;

import com.qizx.util.CharTable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collection of static utility methods.
 */
public class Util
{
    public static int setFlag(int mask, int flag)
    {
        return mask | (1 << flag);
    }

    public static int changeFlag(int mask, int flag, boolean set)
    {
        return set ? setFlag(mask, flag) : clearFlag(mask, flag);
    }

    public static int clearFlag(int mask, int flag)
    {
        return mask & ~(1 << flag);
    }

    public static boolean testFlag(int mask, int flag)
    {
        return (mask & (1 << flag)) != 0;
    }

    public static int countBitsInMask(int mask)
    {
        switch (mask) {
        case -1:
            return 32;
        case 0:
            return 0;
        case 1:
        case 2:
        case 4:
        case 8:
            return 1;
        case 3:
        case 5:
        case 6:
            return 2;
        case 7:
            return 3;
        default:
            int count = 0;
            for (; mask != 0;) {
                if ((mask & 1) != 0)
                    ++count;
                mask >>>= 1;
            }
            return count;
        }
    }

    // ---------------- String manipulations --------------------------------

    public static void printf(char[] buffer)
    {
        fprintf(System.out, buffer);
    }

    public static void fprintf(java.io.PrintStream out, char[] buffer)
    {
        for (int i = 0; i < buffer.length; i++)
            if (buffer[i] < ' ')
                out.print("&" + (int) buffer[i] + ";");
            else
                out.print(buffer[i]);
    }

    final static char hexDigits[] =
        {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
            'D', 'E', 'F'
        };

    public static String toHex(int h, int minDigit)
    {
        char[] digits = new char[8];
        int d = 7;
        do {
            digits[d--] = hexDigits[h & 0xf];
            h >>>= 4;
            --minDigit;
        }
        while (h != 0 || minDigit > 0);
        return new String(digits, d + 1, 7 - d);
    }

    /**
     *	Converts a name to 'camelCase': hyphens are removed,
     *	letters following hyphens are converted to uppercase.
     *	Initial letter converted to uppercase if 'capitalize'.
     *	eg: this-to-that becomes ThisToThat
     */
    public static String camelCase(String name, boolean capitalize)
    {
        if (name == null || name.length() < 1)
            return name;
        StringBuffer n = new StringBuffer(name.length());
        n.append(capitalize ? Character.toUpperCase(name.charAt(0))
                : name.charAt(0));
        for (int i = 1, L = name.length(); i < L; i++) {
            char c = name.charAt(i);
            if (c != '-')
                n.append(c);
            else if (++i < L)
                n.append(Character.toUpperCase(name.charAt(i)));
        }
        return n.toString();
    }

    public static String trimSuffix(String s, String suffix)
    {
        if (!s.endsWith(suffix))
            return s;
        return s.substring(0, s.length() - suffix.length());
    }

    public static String trimPrefix(String s, String suffix)
    {
        if (!s.startsWith(suffix))
            return s;
        return s.substring(suffix.length());
    }

    public static String stringAfter(String src, String prefix)
    {
        int pos = src.indexOf(prefix);
        return pos < 0 ? src : src.substring(pos + prefix.length());
    }

    public static String stringBefore(String src, String prefix)
    {
        int pos = src.indexOf(prefix);
        return pos < 0 ? src : src.substring(0, pos);
    }

    public static char[] subArray(char[] pattern, int start, int length)
    {
        char[] res = new char[length];
        System.arraycopy(pattern, start, res, 0, length);
        return res;
    }

    /**
     * Compares the first characters of two strings
     * @param length assumed to be <= lengths of both strings
     * @return 0 if equal, negative number if s1 < s2, positive otherwise
     */
    public static int prefixCompare(char[] s1, char[] s2, int length)
    {
        for (int i = 0; i < length; i++) {
            int diff = s1[i] - s2[i];
            if (diff != 0)
                return diff;
        }
        return 0;
    }

    /**
     * Compares the first characters of two strings
     * @param length assumed to be <= lengths of both strings
     * @return 0 if equal, negative number if s1 < s2, positive otherwise
     */
    public static int prefixCompare(CharSequence s1, char[] s2, int length)
    {
        for (int i = 0; i < length; i++) {
            int diff = s1.charAt(i) - s2[i];
            if (diff != 0)
                return diff;
        }
        return 0;
    }

    public static int prefixCompare(char[] s1, char[] s2, int length, 
                                    boolean caseSense, boolean diacSensitive)
    {
        for (int i = 0; i < length; i++) {
            char c1 = s1[i];
            char c2 = s2[i];
            if(!caseSense) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if(!diacSensitive) {
                c1 = Unicode.collapseDiacritic(c1);
                c2 = Unicode.collapseDiacritic(c2);
            }
            int diff = c1 - c2;
            if (diff != 0)
                return diff;
        }
        return 0;
    }
    
    // ---------------------------------------------------------------------

    public static Properties properties(Class classe, String name)
    {
        Properties props = new Properties();
        try {
            URL url = classe.getResource(name);
            if (url == null)
                return props;
            InputStream input = url.openStream();
            props.load(input);
            input.close();
        }
        catch (IOException e) {
            // ignored
        }
        return props;
    }

    public static Object getProp(Map properties, String name, Class classe,
                                 Object defaultValue)
    {
        Object v = (properties != null) ? properties.get(name) : null;
        if (v == null)
            v = defaultValue;
        if (classe != null && !classe.isInstance(v))
            return null;
        return v;
    }

    public static String getStringProp(Map properties, String name,
                                       String defaultValue)
    {
        Object v = (properties != null) ? properties.get(name) : null;
        if (v == null)
            return defaultValue;
        if (v instanceof String)
            return (String) v;
        if (v instanceof char[])
            return new String((char[]) v);
        return null;
    }

    public static RuntimeException unimplemented()
    {
        return new RuntimeException("unimplemented");
    }

    public static RuntimeException shouldNotHappen()
    {
        return new RuntimeException("should not happen");
    }

    public static RuntimeException shouldNotHappen(Class classe)
    {
        return new RuntimeException("should not happen, in " + classe);
    }

    /**
     * Sleep without this annoying exception.
     */
    public static void sleep(int time)
    {
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns true if the object is a handle to a RMI remote object.
     */
    public static boolean isRemote(Object object)
    {
        return object instanceof java.rmi.server.RemoteStub;
    }

    // ----------------------------------------------------------------------

    private static final char LOCALE_SEP1 = '-', LOCALE_SEP2 = '_';

    public static Locale getLocale(String name)
    {
        int sep1 = -1, sep2 = -1;
        for (int i = 0, len = name.length(); i < len; i++) {
            char c = name.charAt(i);
            if (c == LOCALE_SEP1 || c == LOCALE_SEP2) {
                if (sep1 < 0)
                    sep1 = i;
                else if (sep2 < 0)
                    sep2 = i;
                else
                    break;
            }
        }

        if (sep1 < 0)
            return new Locale(name);
        String language = name.substring(0, sep1);

        if (sep2 < 0)
            return new Locale(language, name.substring(sep1 + 1));

        return new Locale(language, name.substring(sep1 + 1, sep2),
                          name.substring(sep2 + 1));
    }

    // ----------------------------------------------------------------------

    /**
     * Converts a relative or absolute path or an uri to an absolute URL
     */
    public static URL uriToURL(String uri)
        throws IOException
    {
        if (uri == null)
            uri = ".";
        if (!uri.endsWith("/"))
            uri = uri.concat("/");
        URL rurl = null;
        try {
            rurl = new URL(uri);
        }
        catch (MalformedURLException e) {
            rurl = new File(uri).getCanonicalFile().toURL();
        }
        return rurl;
    }

    // treats /. or /.. and trim appropriately
    static int processComponent(char[] cpath, int pos)
    {
        if (pos < 2 || cpath[pos - 1] != '.')
            return pos;
        if (cpath[pos - 2] == '/')
            // remove trailing /.
            return pos - 2;
        if (pos < 3 || cpath[pos - 2] != '.' || cpath[pos - 3] != '/')
            return pos;
        // we have a trailing /.. : go back to preceding slash
        pos -= 3;
        while (pos > 0 && cpath[pos - 1] != '/')
            --pos;
        return pos;
    }

    // ------------ escape URI ---------------------------------------------

    public static final String HEX = "0123456789ABCDEFabcdef";

    static Charset UTF8;
    public static final String URI_KEPT = "-_.~";
    public static final String IRI_KEPT = "!~*\'()#;/?:@&=+$,[]%";

    static final IntSet URI_SET = new IntSet();
    static IntSet URI2_SET; // to escape illegal characters in URI
    static IntSet IRI_SET;
    static final IntSet HTML_SET = new IntSet();

    static {
        try {
            UTF8 = Charset.forName("UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        URI_SET.add('A', 'Z');
        URI_SET.add('a', 'z');
        URI_SET.add('0', '9');
        for (int c = URI_KEPT.length(); --c >= 0;)
            URI_SET.add((int) URI_KEPT.charAt(c));

        URI2_SET = URI_SET.copy();
        URI2_SET.add('/');

        IRI_SET = URI_SET.copy();
        for (int c = IRI_KEPT.length(); --c >= 0;)
            IRI_SET.add((int) IRI_KEPT.charAt(c));

        HTML_SET.add(32, 126);
    }

    static boolean isHexa(char c)
    {
        return HEX.indexOf(c) >= 0;
    }

    /**
     * Escapes all characters which don't belong to 'kept', after UTF8
     * encoding.
     */
    private static String utf8EncodeURI(String uri, IntSet kept)
    {
        int len = uri.length();
        StringBuffer buf = new StringBuffer(len + len / 4);

        ByteBuffer bb = UTF8.encode(uri);
        for (; bb.hasRemaining();) {
            int c = bb.get() & 0xff;
            if (kept.test(c))
                buf.append((char) c);
            else {
                buf.append('%');
                buf.append(HEX.charAt(c >>> 4));
                buf.append(HEX.charAt(c & 0xf));
            }
        }
        return buf.toString();
    }

    /**
     * Escapes entirely an URI
     * @param uri a non-escaped URI (e.g containing accents and whitespace)
     * @return
     */
    public static String encodeForURI(String uri)
    {
        return utf8EncodeURI(uri, URI_SET);
    }

    /**
     * Escapes illegal characters in an URI
     * @param uri a non-escaped URI (e.g containing accents and whitespace)
     * @return
     */
    public static String escapeURI(String uri)
    {
        int colon = uri.lastIndexOf(':');
        if (colon > 0)
            return uri.substring(0, colon + 1)
                   + utf8EncodeURI(uri.substring(colon + 1), URI2_SET);
        return utf8EncodeURI(uri, URI2_SET);
    }

    public static String unescapeURI(String uri)
    {
        try {
            return URLDecoder.decode(uri, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return uri; // whatever
        }
    }

    public static String iriToURI(String uri)
    {
        return utf8EncodeURI(uri, IRI_SET);
    }

    public static String escapeHtmlURI(String uri)
    {
        return utf8EncodeURI(uri, HTML_SET);
    }

    // -----------------------------------------------------------------------

    public static void dumpBytes(byte[] bytes, int maxLen)
    {
        for (int i = 0, max = Math.min(maxLen, bytes.length); i < max; i++) {
            System.err.print(" ");
            System.err.print(Integer.toHexString(bytes[i] & 0xff));
        }
        System.err.println();
    }

    public static int hashDouble(double v)
    {
// long bits = Double.doubleToRawLongBits(v);
// return ((int) (bits >>> 32)) ^ ((int) bits);
        return (int) (10 * v);
    }

    public static String shortClassName(Class classe)
    {
        String name = classe.getName();
        int dot = name.lastIndexOf('.');
        return name.substring(dot + 1).replace('$', '.');
    }

    public static String shortestClassName(Class classe)
    {
        String name = classe.getName();
        int dot = name.lastIndexOf('$');
        if(dot < 0)
            dot = name.lastIndexOf('.');
        return name.substring(dot + 1).replace('$', '.');
    }

    public static String[] toStringArray(ArrayList stringList)
    {
        if (stringList == null)
            return null;
        return (String[]) stringList.toArray(new String[stringList.size()]);
    }

    public static int[] enlarge(int[] array, int newSize)
    {
        int[] newArray = new int[newSize];
        if(array != null)
            System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static long[] enlarge(long[] array, int newSize)
    {
        long[] newArray = new long[newSize];
        if(array != null)
            System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static byte[] enlarge(byte[] array, int newSize)
    {
        byte[] newArray = new byte[newSize];
        if(array != null)
            System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static char[] enlarge(char[] array, int newSize)
    {
        char[] newArray = new char[newSize];
        if(array != null)
            System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static short[] enlarge(short[] array, int newSize)
    {
        short[] newArray = new short[newSize];
        if(array != null)
            System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static Logger createLogger(String name)
    {
        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false);
//        for(Handler h : logger.getHandlers()) {
//            logger.removeHandler(h);
//        }
        logger.setLevel(Level.INFO);
        return logger;
    }
}
