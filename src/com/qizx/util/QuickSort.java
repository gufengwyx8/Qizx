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

/**
 * Sorts an abstract array-like model using a quick-sort algorithm.
 * <p>
 * The sorting algorithm is a tuned quicksort, adapted from Jon L. Bentley and
 * M. Douglas McIlroy's "Engineering a Sort Function", Software-Practice and
 * Experience, Vol. 23(11) P. 1249-1265 (November 1993). This algorithm offers
 * n*log(n) performance on many data sets that cause other quicksorts to
 * degrade to quadratic performance.
 * <p>
 * Adapted from java.util.Arrays.sort
 */
public class QuickSort
{
    /**
     * Abstraction of the data to sort.
     */
    public interface Model
    {
        int compare(int index1, int index2);

        void swap(int index1, int index2);

        /**
         * Saves the value associated with partition position,
         * for use in compareToPivot.
         */
        void savePivot(int index);

        /**
         * Compares the value of savePivot with the value at position 'index'.
         */
        int compareToPivot(int index);
    }

    public static void sort(Model data, int fromIndex, int toIndex)
    {
        sort1(data, fromIndex, toIndex, 0);
    }

    private static void sort1(Model data, int off, int len, int depth)
    {
        
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && data.compare(j - 1, j) > 0; j--)
                    data.swap(j, j - 1);
            return;
        }

        // Choose a partition element (pivot) at pos m
        int med = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(data, l, l + s, l + 2 * s);
                med = med3(data, med - s, med, med + s);
                n = med3(data, n - 2 * s, n - s, n);
            }
            med = med3(data, l, med, n); // Mid-size, med of 3
        }
        
        data.savePivot(med);
        
        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        int cmp;
        while (true) {
            while (b <= c && (cmp = data.compareToPivot(b)) <= 0) {
                if (cmp == 0)
                    data.swap(a++, b);
                b++;
            }
            while (c >= b && (cmp = data.compareToPivot(c)) >= 0) {
                if (cmp == 0)
                    data.swap(c, d--);
                c--;
            }
            if (b > c)
                break;
            data.swap(b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecSwap(data, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecSwap(data, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(data, off, s, depth + 1);
        if ((s = d - c) > 1)
            sort1(data, n - s, s, depth + 1);
    }
    
    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(Model data, int a, int b, int c)
    {
        return (data.compare(a, b) < 0 ?
                 (data.compare(b, c) < 0 ? b : data.compare(a, c) < 0 ? c : a)
               : (data.compare(b, c) < 0 ? b : data.compare(a, c) > 0 ? c : a));
    }
    
    private static void vecSwap(Model data, int a, int b, int n)
    {
        for (int i = 0; i < n; i++)
            data.swap(a + i, b + i);
    }
   
/*
    public static void main(String[] args)
    {
        try {
            final int[] a = new int[10000000];
            for (int i = 0; i < a.length; i++) {
                a[i] = i + 1;
                
            }
            System.err.println();
            long t0 = System.currentTimeMillis();
            sort(new Data() {
                int piv;
                
                public int compare(int index1, int index2)
                {
                    return a[index1] - a[index2];
                }

                public void swap(int index1, int index2)
                {
                    int tmp = a[index1];
                    a[index1] = a[index2];
                    a[index2] = tmp;
                }

                public void savePivot(int m)
                {
                     piv = a[m];
                }

                public int compareToPivot(int index)
                {
                    return a[index] - piv;
                }
            }, 0, a.length);
            
            System.err.println("------ after sort");
            System.err.println("\ntime "+(System.currentTimeMillis() - t0));
            for (int i = 0; i < a.length; i++) {
                
                if(a[i] != i + 1)
                    System.err.println("oops at "+i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
}
