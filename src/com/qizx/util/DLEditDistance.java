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

import com.qizx.util.basic.Unicode;

/**
 * Computes a parametered Damerau-Levenshtein distance,
 * with a threshold for quick rejection.
 */
public class DLEditDistance
{
    private int insertCost;
    private int transposeCost;
    private int substituteCost;
    private int caseDiacCost;
    private int threshold;
    
    private int[] costs;   // current column of size word2.len
    private int[] costs_1;   // previous column
    private int[] costs_2;
       
    public DLEditDistance(int insertCost, int transposeCost,
                          int substituteCost, int caseDiacCost)
    {
        super();
        this.insertCost = insertCost;
        this.transposeCost = transposeCost;
        this.substituteCost = substituteCost;
        this.caseDiacCost = caseDiacCost;
        threshold = Integer.MAX_VALUE;
        costs = new int[20];
        costs_1 = new int[20];
        costs_2 = new int[20];
    }

    /**
     * Returns the maximum distance allowed.
     */
    public int getThreshold()
    {
        return threshold;
    }

    /**
     * Maximum distance allowed, for fast rejection.
     * Stops computation of distance if threshold is exceeded.
     * @param threshold maximum cost
     */
    public void setThreshold(int threshold)
    {
        this.threshold = threshold;
    }

    public int distance(char[] word1, char[] word2)
    {
        

        int len2 = word2.length, len1 = word1.length; 
        int diffLen = Math.abs(len2 - len1);
        if(diffLen * insertCost > threshold)
            return threshold;

        if(len2 + 1 > costs.length) {   // one more needed
            costs = new int[len2 + 10];
            costs_1 = new int[len2 + 10];
            costs_2 = new int[len2 + 10];
        }
        for (int y = 0; y <= len2; y++) {
            costs_2[y] = 0;
            costs_1[y] = y * insertCost;
        }
        
        int min = threshold;
        // iterate on word1 chars
        for (int x = 0; x < len1; x++)
        {

            min = threshold;
            costs[0] = costs_1[0] + insertCost;
            // 
            for(int y = 0; y < len2; y++) {
                // insertion char1:
                int cost = costs_1[y + 1] + insertCost;
                // deletion char2:
                int insCost = costs[y] + insertCost;
                if(insCost < cost)
                    cost = insCost;
                // substitution:
                int subCost = costs_1[y];
                char ch2 = word2[y];
                if(word1[x] != ch2) {
                    if(Unicode.collapse(word1[x]) == Unicode.collapse(ch2))
                        subCost += caseDiacCost;
                    else
                        subCost += substituteCost;
                    if(subCost < cost)
                        cost = subCost;
                    // transposition: (only if !=)
                    if(x > 1 && y > 1 && word1[x] == word2[y - 1]
                                      && word1[x - 1] == ch2)
                        cost = Math.min(cost, costs_2[y - 1] + transposeCost);
                }
                else if(subCost < cost)
                    cost = subCost;
                
                costs[y + 1] = cost; // shifted in y

                if(cost < min)
                    min = cost;
            }

            if(min > threshold)
                return threshold; // give up

            int[] tmp = costs_2;
            costs_2 = costs_1;
            costs_1 = costs;
            costs = tmp; // recycled
        }
        
        return costs_1[len2];   // beware shift above
    }
    
  
//    public static void main(String[] args)
//    {
//        DLEditDistance dist = new DLEditDistance(3/*ins del*/, 2/*transp*/,
//                                                 2/*subst*/, 1/*case diac*/);
//        try {
//            char[] w1 = "compromise".toCharArray(), w2 = "promise".toCharArray();
//            int sum = 0;
//            long t0 = System.currentTimeMillis();
//            dist.setThreshold(10);
//            for (int i = 0; i < 1; i++) {
//                int d = dist.distance(w1, w2);
//                System.err.println("dist = "+d);
//                sum += d;
//            }
//            System.err.println("time " + (System.currentTimeMillis() - t0));
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void usage()
//    {
//        System.err.println("DLEditDistance usage: ");
//        System.exit(1);
//    }
}
