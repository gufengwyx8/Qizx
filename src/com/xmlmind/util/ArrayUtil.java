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
 * Copyright (c) 2002-2010 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.util;

import java.lang.reflect.Array;

/**
 * A collection of utility functions (static methods) operating on arrays.
 * Complements what's found in java.util.Arrays.
 */
public final class ArrayUtil {
    private ArrayUtil() {}

    /**
     * Returns specified array if its length is equal to specified size;
     * otherwise returns a copy of specified array having specified size.
     * 
     * @param <T> the type of the components of the array
     * @param list array whose size needs to be adjusted
     * @param listSize the size of the returned array
     * @return an array having <code>listSize</code> components
     */
    public static final <T> T[] trimToSize(T[] list, int listSize) {
        if (list.length == listSize)
            return list;

        T[] newList = 
            (T[]) Array.newInstance(list.getClass().getComponentType(), 
                                    listSize);
        System.arraycopy(list, 0, newList, 0, listSize);

        return newList;
    }

    /**
     * Equivalent to {@link #insert(Object[], int, Object[]) insert(list,
     * index, new T[] { item})}.
     */
    public static final <T> T[] insert(T[] list, int index, T item) {
        T[] newList = 
            (T[]) Array.newInstance(list.getClass().getComponentType(), 
                                    list.length + 1);

        if (index > 0)
            System.arraycopy(list, 0, newList, 0, index);

        int tail = list.length - index;
        if (tail > 0) 
            System.arraycopy(list, index, newList, index + 1, tail);

        newList[index] = item;

        return newList;
    }

    /**
     * Inserts specified items in specified array at specified index.
     * 
     * @param <T> the type of the components of the array
     * @param list list to be modified
     * @param index items are inserted at this position.
     * <p>Note that <tt>index</tt> may be equal to the size of the array. This
     * means: insert at end.
     * @param items items to be inserted
     * @return new array having original size+<code>items.length</code>
     * components
     */
    public static final <T> T[] insert(T[] list, int index, T[] items) {
        int count = items.length;
        T[] newList = 
            (T[]) Array.newInstance(list.getClass().getComponentType(), 
                                    list.length + count);

        if (index > 0)
            System.arraycopy(list, 0, newList, 0, index);

        int tail = list.length - index;
        if (tail > 0) 
            System.arraycopy(list, index, newList, index + count, tail);

        for (int k = 0; k < count; ++k) 
            newList[index+k] = items[k];

        return newList;
    }

    /**
     * Inserts specified item in specified array at the beginning of specified
     * array.
     * 
     * @param <T> the type of the components of the array
     * @param list list to be modified
     * @param item item to be inserted
     * @return new array having original size+1 components
     */
    public static final <T> T[] prepend(T[] list, T item) {
        return insert(list, 0, item);
    }

    /**
     * Inserts specified item in specified array at the end of specified
     * array.
     * 
     * @param <T> the type of the components of the array
     * @param list list to be modified
     * @param item item to be inserted
     * @return new array having original size+1 components
     */
    public static final <T> T[] append(T[] list, T item) {
        return insert(list, list.length, item);
    }

    /**
     * Remove specified item from specified array. Items are compared using
     * <code>equals()</code> and not <code>==</code> (using {@link #indexOf}).
     * 
     * @param <T> the type of the components of the array
     * @param list list to be modified
     * @param item item to be removed
     * @return new array having original size-1 components
     */
    public static final <T> T[] remove(T[] list, T item) {
        int index = indexOf(list, item);
        if (index < 0)
            return list;

        return removeAt(list, index);
    }

    /**
     * Searches specified array for specified item. Unlike {@link #find},
     * items are compared using <code>equals()</code> and not <code>==</code>.
     * 
     * @param <T> the type of the components of the array
     * @param list a list possibly containing item
     * @param item searched item
     * @return the index of searched item if found; -1 otherwise
     */
    public static final <T> int indexOf(T[] list, T item) {
        for (int i = 0; i < list.length; ++i) {
            T e = list[i];
            if ((e == null && item == null) ||
                (e != null && e.equals(item)))
                return i;
        }
        return -1;
    }

    /**
     * Searches specified array for specified item. Unlike {@link #indexOf},
     * items are compared using <code>==</code> and not <code>equals()</code>.
     * 
     * @param <T> the type of the components of the array
     * @param list a list possibly containing item
     * @param item searched item
     * @return the index of searched item if found; -1 otherwise
     */
    public static final <T> int find(T[] list, T item) {
        for (int i = 0; i < list.length; ++i) {
            if (list[i] == item)
                return i;
        }
        return -1;
    }

    /**
     * Equivalent to {@link #removeAt(Object[], int, int) remove(list, index,
     * 1)}.
     */
    public static final <T> T[] removeAt(T[] list, int index) {
        return removeAt(list, index, 1);
    }

    /**
     * Removes a range of items found at specified index from specified array.
     * 
     * @param <T> the type of the components of the array
     * @param list list to be modified
     * @param index items are removed at this position
     * @param count number of items to be removed
     * @return new array having original size-<tt>count</tt>
     */
    public static final <T> T[] removeAt(T[] list, int index, int count) {
        T[] newList = 
            (T[]) Array.newInstance(list.getClass().getComponentType(), 
                                    list.length - count);

        if (index > 0)
            System.arraycopy(list, 0, newList, 0, index);

        int first = index + count;
        int tail = list.length - first;
        if (tail > 0) 
            System.arraycopy(list, first, newList, index, tail);

        return newList;
    }

    /**
     * Reverses the order of the items in the specified array.
     */
    public static final <T> void reverse(T[] list) {
        int begin = 0;
        int end = list.length-1;
  
        while (begin < end) {
            T temp = list[begin]; 
            list[begin] = list[end]; 
            list[end] = temp;

            ++begin;
            --end;
        }
    }

    /*TEST_ARRAY
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println(
             "usage: java com.xmlmind.util.ArrayUtil index count arg+");
            System.exit(1);
        }

        int index = Integer.parseInt(args[0]);
        int count = Integer.parseInt(args[1]);
        String[] list0 =  new String[args.length - 2];
        System.arraycopy(args, 2, list0, 0, list0.length);
        
        System.out.println("Original list:");
        for (int i = 0; i < list0.length; ++i) {
            if (i > 0)
                System.out.print(';');
            System.out.print(list0[i]);
        }
        System.out.println();

        String[] removed =  new String[count];
        System.arraycopy(list0, index, removed, 0, count);
        
        String[] list1 = removeAt(list0, index, count);
        
        System.out.println("Original list less range index=" + index + 
                           ", count=" + count);
        for (int i = 0; i < list1.length; ++i) {
            if (i > 0)
                System.out.print(';');
            System.out.print(list1[i]);
        }
        System.out.println();
        
        String[] list2 = insert(list1, index, removed);
        if (!java.util.Arrays.equals(list2, list0))
            System.exit(2);
    }
    TEST_ARRAY*/
}
