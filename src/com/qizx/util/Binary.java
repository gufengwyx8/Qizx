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
package com.qizx.util;

import java.util.Arrays;

/**
 *	
 */
public class Binary
{
    byte[] value;

    public Binary(byte[] value)
    {
        this.value = value;
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof Binary))
            return false;
        Binary thatBin = (Binary) that;
        return Arrays.equals(value, thatBin.value);
    }

    public int hashCode()
    {
        int h = 0;
        for (int b = 0; b < value.length; b++)
            h = h << 1 + value[b];
        return h;
    }

    public int compareTo(Binary that)
    {
        return equals(that) ? 0 : 1; // no order
    }

    public static class Exception extends IllegalArgumentException
    {
        Exception(String msg)
        {
            super(msg);
        }
    }

    // ------------------------------------------------------------

    private static final char[] toBase64Digit =
        ("ABCDEFGHIJKLMNOPQRSTUVWXYZ"
         + "abcdefghijklmnopqrstuvwxyz0123456789+/=").toCharArray();

    private static byte[] fromBase64Digit = new byte[256];
    static {
        for (int i = 0; i < 256; ++i)
            fromBase64Digit[i] = -1;

        for (int i = 'A'; i <= 'Z'; ++i)
            fromBase64Digit[i] = (byte) (i - 'A');

        for (int i = 'a'; i <= 'z'; ++i)
            fromBase64Digit[i] = (byte) (26 + i - 'a');

        for (int i = '0'; i <= '9'; ++i)
            fromBase64Digit[i] = (byte) (52 + i - '0');

        fromBase64Digit['+'] = 62;
        fromBase64Digit['/'] = 63;
    }

    private static final int SPACE_OR_DIGIT1 = 0;

    private static final int DIGIT1 = 1;

    private static final int DIGIT2 = 2;

    private static final int DIGIT3_OR_EQUAL = 3;

    private static final int EQUAL = 4;

    private static final int DIGIT4_OR_EQUAL = 5;

    private static final int END_OR_SPACE = 6;

    private static void unexpected(char c)
        throws Exception
    {
        throw new Exception("unexpected base64 char #x"
                            + Integer.toHexString(c));
    }

    private static boolean isXMLSpace(char c)
    {
        return c <= ' ' && (c == ' ' || c == '\n' || c == '\r' || c == '\t');
    }

    public static Binary parseBase64Binary(String s)
    {
        // Note that a zero-length base 64 string is valid.
        int length = s.length();
        byte[] b = new byte[3 * (length / 4) + 2];
        int j = 0;
        int state = SPACE_OR_DIGIT1;
        int bits = 0;
        int bitCount = 0;

        for (int i = 0; i < length; ++i) {
            char c = s.charAt(i);
            int value = 0;
            if (isXMLSpace(c)) {
                // Same state.
                continue;
            }
            switch (state) {
            case SPACE_OR_DIGIT1:
                if (c > 255 || (value = fromBase64Digit[c]) < 0)
                    unexpected(c);
                state = DIGIT2;
                break;
            case DIGIT1:
                if (c > 255 || (value = fromBase64Digit[c]) < 0)
                    unexpected(c);
                state = DIGIT2;
                break;
            case DIGIT2:
                if (c > 255 || (value = fromBase64Digit[c]) < 0)
                    unexpected(c);
                state = DIGIT3_OR_EQUAL;
                break;
            case DIGIT3_OR_EQUAL:
                if (c == '=') {
                    state = EQUAL;
                    continue;
                }
                else {
                    if (c > 255 || (value = fromBase64Digit[c]) < 0)
                        unexpected(c);
                    state = DIGIT4_OR_EQUAL;
                }
                break;
            case EQUAL:
                if (c == '=') {
                    state = END_OR_SPACE;
                    continue;
                }
                else
                    unexpected(c);
                /* break; */
            case DIGIT4_OR_EQUAL:
                if (c == '=') {
                    state = END_OR_SPACE;
                    continue;
                }
                else {
                    if (c > 255 || (value = fromBase64Digit[c]) < 0)
                        unexpected(c);
                    state = SPACE_OR_DIGIT1;
                }
                break;
            case END_OR_SPACE:
                if (isXMLSpace(c)) {
                    // Same state.
                    continue;
                }
                else
                    unexpected(c);
                /* break; */
            default:
                throw new Exception("unknown state " + state);
            }

            bits <<= 6;
            bits |= value;
            bitCount += 6;

            if (bitCount >= 8) {
                bitCount -= 8;
                b[j++] = (byte) ((bits >> bitCount) & 0xFF);
            }
        }

        switch (state) {
        case SPACE_OR_DIGIT1:
        case DIGIT1:
        case END_OR_SPACE:
            break;
        default:
            throw new Exception("truncated base64 string");
        }

        if (j != b.length) {
            byte[] b2 = new byte[j];
            System.arraycopy(b, 0, b2, 0, j);
            b = b2;
        }
        return new Binary(b);
    }

    public String toBase64String()
    {
        byte[] b = value;
        int charCount = ((b.length + 2) / 3) * 4;
        char[] chars = new char[charCount + ((charCount + 75) / 76)];
        int j = 0;
        int k = 0;

        for (int i = 0; i < b.length; i += 3) {
            boolean char4 = false;
            boolean char3 = false;

            int bits = ((int) b[i]) & 0xFF;
            bits <<= 8;
            if (i + 1 < b.length) {
                bits |= ((int) b[i + 1]) & 0xFF;
                char3 = true;
            }
            bits <<= 8;
            if (i + 2 < b.length) {
                bits |= ((int) b[i + 2]) & 0xFF;
                char4 = true;
            }
            chars[j + 3] = toBase64Digit[(char4 ? (bits & 0x3F) : 64)];
            bits >>>= 6;
            chars[j + 2] = toBase64Digit[(char3 ? (bits & 0x3F) : 64)];
            bits >>>= 6;
            chars[j + 1] = toBase64Digit[bits & 0x3F];
            bits >>>= 6;
            chars[j] = toBase64Digit[bits & 0x3F];

            j += 4;

            k += 4;
            if (k == 76) {
                chars[j++] = '\n';
                k = 0;
            }
        }

        // if (k > 0)
        // chars[j] = '\n';
        return new String(chars, 0, j);
    }

    // ------------------------------------------------------------

    public static Binary parseHexBinary(String s)
    {
        // Note that a zero-length hex string is valid.
        int length = s.length();
        if ((length % 2) != 0)
            throw new Exception("odd hexBinary length");

        byte[] b = new byte[length / 2];
        int j = 0;

        for (int i = 0; i < length; i += 2) {
            int c1 = s.charAt(i);
            int c2 = s.charAt(i + 1);
            int value1;
            int value2;

            if (c1 < '0' || c1 > 'f' || (value1 = fromHexDigit[c1 - '0']) < 0)
                throw new Exception("invalid hex char #x"
                                    + Integer.toHexString(c1));

            if (c2 < '0' || c2 > 'f' || (value2 = fromHexDigit[c2 - '0']) < 0)
                throw new Exception("invalid hex char #x"
                                    + Integer.toHexString(c2));

            b[j++] = (byte) (((value1 & 0xF) << 4) | (value2 & 0xF));
        }
        return new Binary(b);
    }

    public String toHexString()
    {
        char[] chars = new char[2 * value.length];
        int j = 0;

        for (int i = 0; i < value.length; ++i) {
            byte bits = value[i];
            chars[j++] = toHexDigit[((bits >>> 4) & 0xF)];
            chars[j++] = toHexDigit[(bits & 0xF)];
        }

        return new String(chars);
    }

    private static final char[] toHexDigit =
        ("0123456789ABCDEF").toCharArray();

    private static final int[] fromHexDigit =
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1, -1, 10, 11,
            12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 10, 11,
            12, 13, 14, 15 };
}
