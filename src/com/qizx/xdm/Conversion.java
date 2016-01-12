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

import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQTypeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Conversion utility class.
 */
public class Conversion
{
    public static final QName ERR_CAST =
        ModuleContext.xqueryErrorCode("FORG0001");

    private static final int DECIMAL_PRECISION = 18;

    private static Method toPlainString;
    static {
        try {
            toPlainString = BigDecimal.class.getMethod("toPlainString",
                                                       (java.lang.Class[]) null);
        }
        catch (SecurityException e) {
            ;
        }
        catch (NoSuchMethodException e) {
            ;
        }
    }

    public static boolean toBoolean(double d)
    {
        return d != 0 && d == d /* inlining !isNaN() */;
    }

    public static boolean toBoolean(String s)
        throws XQTypeException
    {
        if ("true".equalsIgnoreCase(s) || "1".equals(s))
            return true;
        if ("false".equalsIgnoreCase(s) || "0".equals(s))
            return false;
        throw new XQTypeException(ERR_CAST, "invalid value '" + s
                                                + "' for boolean type");
    }

    public static long toInteger(boolean b)
    {
        return b ? 1 : 0;
    }

    public static long toInteger(String s)
        throws XQTypeException
    {
        try {
            return Long.parseLong(s.trim());
        }
        catch (NumberFormatException e) {
            throw new XQTypeException(ERR_CAST, "invalid value '" + s
                                                + "' for integer type");
        }
    }

    /**
     * Checks if the value is out of bounds. Can be used to detect arithmetic
     * overflow. Slightly conservative (returns false above
     * 0x7ffffffffffff000).
     */
    public static boolean isIntegerRange(double value)
    {
        // the simpler the better
        return (value >= MIN_INTVALUE && value < MAX_INTVALUE);
    }

    // cannot be represented with full precision on double:
    static double MAX_INTVALUE = (double) (1L << 62),
    MIN_INTVALUE = -MAX_INTVALUE;

    public static float toFloat(boolean b)
    {
        return b ? 1.0f : 0.0f;
    }

    public static float toFloat(String s)
        throws XQTypeException
    {
        try {
            return Float.parseFloat(s.trim());
        }
        catch (NumberFormatException e) {
            if (s.equals("INF"))
                return Float.POSITIVE_INFINITY;
            if (s.equals("-INF"))
                return Float.NEGATIVE_INFINITY;
            throw new XQTypeException(ERR_CAST, "invalid value '" + s
                                                + "' for type float");
        }
    }

    public static double toDouble(boolean b)
    {
        return b ? 1.0 : 0.0;
    }

    public static double toDouble(String s)
        throws XQTypeException
    {
        try {
            return Double.parseDouble(s.trim());
        }
        catch (NumberFormatException e) {
            if (s.equals("INF"))
                return Double.POSITIVE_INFINITY;
            if (s.equals("-INF"))
                return Double.NEGATIVE_INFINITY;
            throw new XQTypeException(ERR_CAST, "invalid value '" + s
                                                + "' for type double");
        }
    }

    public static double roundHalfToEven(double d, int prec)
    {
        double scale = Math.pow(10, prec);
        double r = Math.rint(d * scale) / scale;
        return r;
    }

    public static BigDecimal toDecimal(boolean b)
    {
        return new BigDecimal(b ? 1.0 : 0.0);
    }

    public static BigDecimal toDecimal(long i)
    {
        return new BigDecimal(Long.toString(i)); // nothing more efficient
                                                    // ???
    }

    public static BigDecimal toDecimal(String s, boolean lenient)
        throws XQTypeException
    {
        try {
            if (lenient || (s.lastIndexOf('e') < 0 && s.lastIndexOf('E') < 0))
                return new BigDecimal(s.trim());
        }
        catch (NumberFormatException e) {
        }
        throw new XQTypeException(ERR_CAST, "invalid value '" + s
                                                + "' for type decimal");
    }

    // converts with rounding to even
    public static BigDecimal toDecimal(double value)
        throws EvaluationException
    {
        BigDecimal dec = new BigDecimal(value);
        return dec.setScale(DECIMAL_PRECISION - 2, BigDecimal.ROUND_HALF_EVEN);
    }

    public static String toString(boolean b)
    {
        return b ? "true" : "false";
    }

    public static String toString(long i)
        throws EvaluationException
    {
        return Long.toString(i);
    }

    static DecimalFormatSymbols syms = new DecimalFormatSymbols(Locale.US);

    static DecimalFormat fmtSciD =
        new DecimalFormat("0.0################E0##", syms);

    static DecimalFormat fmtDecD =
        new DecimalFormat("#####0.0################", syms);

    static DecimalFormat fmtSciF = new DecimalFormat("0.0####E0##", syms);

    static DecimalFormat fmtDecF = new DecimalFormat("#####0.0######", syms);

    public static String toString(float f)
        throws EvaluationException
    {
        if (f == 0f) {
            return (1.0f / f) == Float.POSITIVE_INFINITY ? "0" : "-0";
        }
        if (f == Float.POSITIVE_INFINITY)
            return "INF";
        if (f == Float.NEGATIVE_INFINITY)
            return "-INF";
        if (f != f)
            return "NaN";

        float av = Math.abs(f);
        String mant, exp = null;
        if (av >= 1e-6f && av < 1e6f) { // must be decimal:
            mant = fmtDecF.format(f);
            // remove trailing ".0" or "0" from mantissa:
            if (mant.indexOf('.') >= 0) {
                int L = mant.length();
                while (L > 0 && mant.charAt(L - 1) == '0')
                    --L;
                if (L > 0 && mant.charAt(L - 1) == '.')
                    --L;
                mant = mant.substring(0, L);
            }
        }
        else { // must be in scientific notation:
            mant = fmtSciF.format(f);
            int pex = mant.indexOf('E');
            if (pex > 0) {
                exp = mant.substring(pex + 1, mant.length());
                mant = mant.substring(0, pex);
            }
        }

        return (exp == null) ? mant : mant.concat("E").concat(exp);
    }

    public static String toString(double d)
        throws EvaluationException
    {
        if (d == 0) {
            return (1.0 / d) == Double.POSITIVE_INFINITY ? "0" : "-0";
        }
        if (d == Double.POSITIVE_INFINITY)
            return "INF";
        if (d == Double.NEGATIVE_INFINITY)
            return "-INF";
        if (d != d)
            return "NaN";

        double av = Math.abs(d);
        String mant, exp = null;
        if (av >= 1e-6 && av < 1e6) { // must be decimal:
            synchronized (fmtDecD) { // said to be thread-unsafe
                mant = fmtDecD.format(d);
            }
            // remove trailing ".0" from mantissa:
            int L = mant.length();
            if (L > 2 && mant.charAt(L - 1) == '0'
                && mant.charAt(L - 2) == '.')
                mant = mant.substring(0, L - 2);
        }
        else { // must be in scientific notation:
            synchronized (fmtSciD) {
                mant = fmtSciD.format(d);
            }
            int pex = mant.indexOf('E');
            if (pex > 0) {
                exp = mant.substring(pex + 1, mant.length());
                mant = mant.substring(0, pex);
            }
        }

        return (exp == null) ? mant : mant.concat("E").concat(exp);
    }

    public static BigDecimal round(BigDecimal d)
    {

        switch (d.signum()) {
        case -1:
            return d.setScale(0, BigDecimal.ROUND_HALF_DOWN);
        default:
            return d;
        case 1:
            return d.setScale(0, BigDecimal.ROUND_HALF_UP);
        }
    }

    public static BigDecimal roundHalfToEven(BigDecimal d, int prec)
    {
        if (prec < 0) { // not supported by big dec
            // OOPS probable pb if too large
            long r = d.longValue();
            long div = 1;
            for (int p = prec; p < 0; p++)
                div *= 10;
            long m = r % div;
            r -= m;
            if (m * 2 >= div)
                r += div;
            return BigDecimal.valueOf(r);
        }
        else {
            return d.setScale(prec, BigDecimal.ROUND_HALF_EVEN);
        }
    }

    public static BigDecimal divide(BigDecimal decimal, int cnt)
        throws ArithmeticException
    {
        return divide(decimal, BigDecimal.valueOf(cnt));
    }

    public static BigDecimal divide(BigDecimal dec1, BigDecimal dec2)
        throws ArithmeticException
    {
        return dec1.divide(dec2, Math.max(DECIMAL_PRECISION, dec1.scale()),
                           BigDecimal.ROUND_HALF_EVEN);
    }

    public static String toString(BigDecimal value)
    {
        // Trick to turnaround change in 1.5: use reflection to invoke
        // toPlainString
        String s = null;
        if (toPlainString != null) {
            try {
                s = (String) toPlainString.invoke(value,
                                                  (java.lang.Object[]) null);
            }
            catch (IllegalAccessException e) {
                e.printStackTrace(); // should not happen
                return null;
            }
            catch (InvocationTargetException e) {
                e.printStackTrace(); // should not happen
                return null;
            }
        }
        else
            s = value.toString(); // JRE 1.4- : safe to use

        if (s.lastIndexOf('.') < 0)
            return s;
        // remove trailing zeroes:
        int last = s.length();
        for (; --last > 0;)
            if (s.charAt(last) != '0')
                break;
        if (last > 0 && s.charAt(last) == '.')
            --last; // ++ last; // Nov 2003
        return s.substring(0, last + 1);
    }

} // end of class Conversion
