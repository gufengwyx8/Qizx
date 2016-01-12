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
package com.qizx.xdm;

/**
 * Conversion utility from float or double to String. Handles peculiarities of
 * XQuery foat formatting and offers better control than Java DecimalFormat.
 * <p>
 * Incidentally this conversion is about 10x faster...
 */
public class FloatConversion
{
    // a bit of magic: instead of rounding using an extra digit, we multiply
    // the mantissa by this number and truncate brutally: in fact 23
    // corresponds
    // to the # of bits in IEEE float mantissa (ie 24 bits precision).
    private static final double ROUNDING_BIAS = 1 + 0.5 * Math.pow(2, -24);

    public static String toString(float f)
    {
        if (f == 0.0) {
            return (1 / f) < 0 ? "-0" : "0";
        }
        if (f != f)
            return "NaN";
        if (f == Float.POSITIVE_INFINITY)
            return "INF";
        if (f == Float.NEGATIVE_INFINITY)
            return "-INF";

        float abs = Math.abs(f);
        if (abs >= 1e-6f && abs < 1e6f) {
            return format(f, -1, 7); // decimal notation
        }
        return format(f, 0, 7); // scientific notation
    }

    public static String toString(double f)
    {
        if (f == 0.0) {
            return (1 / f) < 0 ? "-0" : "0";
        }
        if (f != f)
            return "NaN";
        if (f == Double.POSITIVE_INFINITY)
            return "INF";
        if (f == Double.NEGATIVE_INFINITY)
            return "-INF";

        double abs = Math.abs(f);
        if (abs >= 1e-6 && abs < 1e6) {
            return format(f, -1, 15); // decimal notation
        }
        return format(f, 0, 15); // scientific notation
    }

    private static int insert(char[] buf, int pos, int length, char c)
    {
        for (int i = length; --i >= pos;) {
            buf[i + 1] = buf[i];
        }
        buf[pos] = c;
        return length + 1;
    }

    private static String format(double f, int decimalPos, int maxDigits)
    {
        // compute abs value, exponent
        boolean neg = f < 0;
        double v = neg ? -f : f;
        // compute power of 10 and scale value between 1 and 10
        int expo = 0;
        if (v < 1) {
            while (v < 1e-5) {
                v *= 1e5;
                expo -= 5;
            }
            while (v < 1) {
                v *= 10;
                expo -= 1;
            }
        }
        else {
            while (v >= 1e6) {
                v *= 1e-5;
                expo += 5;
            }
            while (v >= 10) {
                v *= 1e-1;
                expo += 1;
            }
        }

        // raw conversion:
        char[] buf = new char[maxDigits + 7 + 6];
        int last = maxDigits + 1;
        rawConvert(v, buf, maxDigits + 2);

        // correct only if following digit is not 0
        if (maxDigits < 13 && buf[maxDigits + 1] > '0') {
            v *= ROUNDING_BIAS;
            if (rawConvert(v, buf, maxDigits + 2))
                ++expo;
        }

        boolean sci = (decimalPos >= 0);
        if (!sci) { // no exponent: adapt point pos
            // handle expo < 0 by inserting leading zeroes
            while (expo < 0) {
                last = insert(buf, 0, last, '0');
                ++expo;
            }
            decimalPos = expo; // floating point
        }

        // eliminate trailing zeroes
        int lastZero = sci ? (decimalPos + 2) : (decimalPos + 1);
        for (; last > lastZero; --last)
            if (buf[last - 1] != '0')
                break;

        // insert decimal point (except in last position):
        if (decimalPos + 1 != last)
            last = insert(buf, decimalPos + 1, last, '.');

        // add sign:
        if (neg)
            last = insert(buf, 0, last, '-');

        // add exponent:
        expo -= decimalPos;
        if (expo != 0) {
            buf[last++] = 'E';
            if (expo < 0) {
                buf[last++] = '-';
                expo = -expo;
            }
            if (expo >= 100) {
                int q = expo / 100;
                buf[last++] = (char) ('0' + q);
                expo -= q * 100;
            }
            if (expo >= 10) {
                int q = expo / 10;
                buf[last++] = (char) ('0' + q);
                expo -= q * 10;
            }
            buf[last++] = (char) ('0' + expo);
        }
        return new String(buf, 0, last);
    }

    private static boolean rawConvert(double v, char[] buf, int maxDigits)
    {
        boolean correct = false;
        if (v >= 10) {
            v /= 10;
            correct = true;
        }
        for (int d = 0; d < maxDigits; d++) {
            int digit = (int) v;
            buf[d] = (char) (digit + '0');
            v = 10 * (v - digit);
        }
        return correct;
    }
}
