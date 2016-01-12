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

import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.Locale;

/**
 * Utility class for manipulation of text Collators, providing collator-based
 * search functions.
 */
public class Collations
{
    public static final String CODEPOINT =
        "http://www.w3.org/2005/xpath-functions/collation/codepoint";

    public static final String CODEPOINT_SHORT = "/codepoint";

    public static final Locale[] LOCALES = Locale.getAvailableLocales();

    // dummy value:
    public static Collator CODEPOINT_COLLATOR = Collator.getInstance();

    /**
     * gets a locale-based collator from an uri of the form '/ll' or '/ll-CC'
     * where 'll' is a ISO-639 language name and CC a ISO-3166 country name.
     * 
     * @return null if no such locale
     */
    public static Collator getInstance(String uri)
    {
        if (uri.equals(CODEPOINT) || uri.equals(CODEPOINT_SHORT))
            return CODEPOINT_COLLATOR;
        if(!uri.startsWith("/"))
            return null;
        int dash = uri.indexOf('-');
        Locale loc = dash < 0 ? new Locale(uri.substring(1))
                              : new Locale(uri.substring(1, dash),
                                           uri.substring(dash + 1));
        if (!localeExists(loc))
            return null;
        return Collator.getInstance(loc);
    }

    private static boolean localeExists(Locale locale)
    {
        for (int i = LOCALES.length; --i >= 0;) {
            if (LOCALES[i].equals(locale))
                return true;
        }
        return false;
    }

    final static int END = CollationElementIterator.NULLORDER;

    /**
     * gets a locale-based collator from an uri of the form 'll' or 'll-CC'
     * where 'll' is a ISO-639 language name and CC a ISO-3166 country name.
     * The uri can be followed by an anchor of the form '#primary',
     * '#secondary' etc. that changes the strength of the collator.
     */
    public static Collator getInstanceWithStrength(String uri)
    {
        if (uri == null)
            return null;
        int sharp = uri.indexOf('#');
        String root = sharp < 0 ? uri : uri.substring(0, sharp);
        // check the locale:
        Collator coll = getInstance(root);
        if(coll == null)
            return null;
        if(sharp > 0) {
            String strength = uri.substring(sharp + 1);
            if (strength.equalsIgnoreCase("primary"))
                coll.setStrength(Collator.PRIMARY);
            else if (strength.equalsIgnoreCase("secondary"))
                coll.setStrength(Collator.SECONDARY);
            else if (strength.equalsIgnoreCase("tertiary"))
                coll.setStrength(Collator.TERTIARY);
            else if(strength.length() > 0)
                return null; // better diag?
        }
        return coll;
    }

    /**
     * Simple helper. If the collator is null, does codepoint comparison.
     */
    public static int compare(String s1, String s2, Collator collator)
    {
        return (collator == null || collator == CODEPOINT_COLLATOR)
                ? s1.compareTo(s2)
                : collator.compare(s1, s2);
    }

    // expand a pattern into an array of collation elements
    private static int[] preCollate(String pattern, RuleBasedCollator coll)
    {
        int[] tmp = new int[pattern.length()];
        int ptr = 0;
        CollationElementIterator it =
            coll.getCollationElementIterator(pattern);
        for (int n; (n = it.next()) != END;) {
            if (n == 0) // CollationElementIterator emits spurious '0'
                continue;
            if (ptr >= tmp.length) {
                int[] old = tmp;
                tmp = new int[old.length * 2];
                System.arraycopy(old, 0, tmp, 0, old.length);
            }
            tmp[ptr++] = n;
        }
        // for(int i = 0; i < ptr; i++) System.out.print("
        // "+Integer.toHexString(tmp[i]));System.out.println(" precol");
        if (ptr == tmp.length) // true in many cases
            return tmp;
        int[] result = new int[ptr];
        System.arraycopy(tmp, 0, result, 0, ptr);
        return result;
    }

    static boolean tailMatch(CollationElementIterator it, int[] elements)
    {
        for (int i = 1; i < elements.length;) {
            int e = it.next();
            if (e == 0)
                continue;
            if (e == END || e != elements[i])
                return false;
            i++;
        }
        return true;
    }

    /**
     * Returns the index of the first occurrence of pattern.
     */
    public static int indexOf(String src, String pattern, Collator collator)
    {

        if (collator == null || collator == CODEPOINT_COLLATOR)
            return src.indexOf(pattern);
        RuleBasedCollator coll = (RuleBasedCollator) collator; // checked before
        int[] pat = preCollate(pattern, coll);
        CollationElementIterator it = coll.getCollationElementIterator(src);

        for (int e; (e = it.next()) != END;) {
            // System.out.println("at "+it.getOffset()+"
            // "+Integer.toHexString(e));
            if (e == 0 || e != pat[0])
                continue;
            int pos = it.getOffset();
            if (tailMatch(it, pat)) {
                it.setOffset(pos);
                it.previous(); // not necessarily same as pos - 1
                return it.getOffset();
            }
            // resume at saved pos:
            it.setOffset(pos);
        }
        return -1;
    }

    public static boolean endsWith(String src, String pattern,
                                   Collator collator)
    {

        if (collator == null || collator == CODEPOINT_COLLATOR)
            return src.endsWith(pattern);
        RuleBasedCollator coll = (RuleBasedCollator) collator; // checked before
        int[] pat = preCollate(pattern, coll);
        CollationElementIterator it = coll.getCollationElementIterator(src);

        for (int e; (e = it.next()) != END;) {
            if (e == 0 || e != pat[0])
                continue;
            int pos = it.getOffset();
            if (tailMatch(it, pat))
                return (it.next() < 0); // must be at end
            // resume at saved pos:
            it.setOffset(pos);
        }
        return false;
    }

} // end of class Collations

