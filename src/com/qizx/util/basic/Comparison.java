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

/**
 * Comparison utility for XQuery.
 */
public final class Comparison
{
    /**
     *  Returned by method compareTo to say "less than".
     */
    public static final int LT = -1;

    /**
     *  Returned by method compareTo when the two values are 
     */
    public static final int GT = 1;

    /**
     *  Returned by method compareTo when the two values are equal.
     */
    public static final int EQ = 0;

    /**
     *  Returned by compare methods when the two values are not comparable.
     *  (generates an error)
     */
    public static final int ERROR = 2;

    /**
     *  Returned by method compareTo when the test must fail 
     *  whatever the values (meant for NaN).
     */
    public static final int FAIL = 3;

    
    public static final int of( int v1, int v2 ) {
        return v1 < v2 ? LT : v1 > v2 ? GT : EQ;
    }
    
    public static final int of( long v1, long v2 ) {
        return v1 < v2 ? LT : v1 > v2 ? GT : EQ;
    }
    
    public static final int of( double v1, double v2 ) {
        return v1 < v2 ? LT : v1 > v2 ? GT : EQ;
    }
    
    // Used in sorts
    public static final int of(double d1, double d2, boolean emptyGreatest)
    {
        if(d1 != d1)   //  NaN
            return emptyGreatest? GT : LT;
        if(d2 != d2)
            return emptyGreatest? LT : GT;
        // no NaN:
        return of(d1, d2);
    }

}
