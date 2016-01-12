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
 * Copyright (c) 2002-2008 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.util;

/**
 * A collection of utility functions (static methods) operating on lists of
 * Strings.
 * <p>What is called a list of Strings here is simply an array of Strings.
 * Such array may not contain <code>null</code> elements.
 */
public final class StringList {
    private StringList() {}

    /**
     * Searches String <code>string</code> within list <code>strings</code>.
     * 
     * @param strings the list to be searched
     * @param string the String to search for
     * @return the index of the searched string within list or -1 if not found
     */
    public static int indexOf(String[] strings, String string) {
        for (int i = 0; i < strings.length; ++i) {
            // string may be null, strings[i] cannot.
            if (strings[i].equals(string))
                return i;
        }
        return -1;
    }

    /**
     * Tests if list <code>strings</code> contains String <code>string</code>.
     * 
     * @param strings the list to be searched
     * @param string the String to search for
     * @return <code>true</code> the string is found and <code>false</code>
     * otherwise
     */
    public static boolean contains(String[] strings, String string) {
        for (int i = 0; i < strings.length; ++i) {
            // string may be null, strings[i] cannot.
            if (strings[i].equals(string))
                return true;
        }
        return false;
    }

    /**
     * Same as {@link #contains} but string comparison is case-insensitive.
     */
    public static boolean containsIgnoreCase(String[] strings, String string) {
        for (int i = 0; i < strings.length; ++i) {
            // string may be null, strings[i] cannot.
            if (strings[i].equalsIgnoreCase(string))
                return true;
        }
        return false;
    }

    /**
     * Inserts a String inside a list of Strings.
     * 
     * @param strings the list where a String is to be inserted
     * @param string the String to insert
     * @param index the insertion index
     * @return a list containing all the items of list <code>strings</code>
     * plus String <code>string</code> inserted at position <code>index</code>
     */
    public static String[] insertAt(String[] strings, String string, 
                                    int index) {
        String[] newStrings = new String[strings.length+1];

        if (index > 0)
            System.arraycopy(strings, 0, newStrings, 0, index);

        int tail = strings.length - index;
        if (tail > 0) 
            System.arraycopy(strings, index, newStrings, index+1, tail);

        newStrings[index] = string;

        return newStrings;
    }

    /**
     * Inserts a String as first item of a list of Strings.
     * 
     * @param strings the list where a String is to be inserted
     * @param string the String to insert
     * @return a list containing all the items of list <code>strings</code>
     * plus String <code>string</code> inserted at its beginning
     */
    public static String[] prepend(String[] strings, String string) {
        String[] newStrings = new String[strings.length+1];

        newStrings[0] = string;
        System.arraycopy(strings, 0, newStrings, 1, strings.length);

        return newStrings;
    }

    /**
     * Inserts a String as last item of a list of Strings.
     * 
     * @param strings the list where a String is to be inserted
     * @param string the String to insert
     * @return a list containing all the items of list <code>strings</code>
     * plus String <code>string</code> inserted at its end
     */
    public static String[] append(String[] strings, String string) {
        String[] newStrings = new String[strings.length+1];

        System.arraycopy(strings, 0, newStrings, 0, strings.length);
        newStrings[strings.length] = string;

        return newStrings;
    }

    /**
     * Removes a String from a list of Strings.
     * 
     * @param strings the list where a String is to be removed
     * @param string the String to remove
     * @return a list containing all the items of list <code>strings</code>
     * less String <code>string</code> if such String is contained in the
     * list; the original list otherwise
     */
    public static String[] remove(String[] strings, String string) {
        int index = indexOf(strings, string);
        if (index < 0)
            return strings;
        else
            return removeAt(strings, index);
    }

    /**
     * Removes an item specified by its position from a list of Strings.
     * 
     * @param strings the list where an item is to be removed
     * @param index the position of the item to remove
     * @return a list containing all the items of list <code>strings</code>
     * less the item at position <code>index</code>
     */
    public static String[] removeAt(String[] strings, int index) {
        String string = strings[index];
        String[] newStrings = new String[strings.length-1];

        if (index > 0)
            System.arraycopy(strings, 0, newStrings, 0, index);

        int first = index + 1;
        int tail = strings.length - first;
        if (tail > 0) 
            System.arraycopy(strings, first, newStrings, index, tail);

        return newStrings;
    }
}
