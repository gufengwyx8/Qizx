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
 * Home-made assertions, used both internally and for API checks
 */
public final class Check
{

    public static void equals(long v1, long v2, String message) {
        if(v1 != v2)
            throw new AssertionError(message + ": " + v1 + " != " + v2);
    }

    public static void equals(int v1, int v2, String message) {
        if(v1 != v2)
            throw new AssertionError(message + ": " + v1 + " != " + v2);
    }
   
    public static void condition(boolean condition, String message) {
        if(!condition)
            throw new AssertionError(message);
    }
   
    public static void nonNull(Object obj, String name) {
        if(obj == null)
            throw new AssertionError(name + " should not be null");
    }

    public static void mustBe(String that)
    {
        throw new AssertionError("argument must be " + that);
    }

    public static void implementation(Object object, Class classe, Class interf)
    {
         if(!(classe.isInstance(object)))
             throw new AssertionError("unsupported implementation of interface "
                                      + interf.getName());
    }
   
}
