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
 * Maps Unicode character codes to values (any object or integer).
 */
public class CharTable
{
    private Object[][] pages;
    private int[][] intPages;
    
    public CharTable()
    {
        pages = new Object[256][];
        intPages = new int[256][];
    }
    
    public Object get(int c)
    {
        Object[] page = pages[c >>> 8];
        return (page == null) ? null : page[c & 0xff];
    }
    
    public int getInt(int c)
    {
        int[] page = intPages[c >>> 8];
        return (page == null) ? 0 : page[c & 0xff];
    }
    
    public void put(int c, Object value)
    {
        int pageId = c >>> 8;
        Object[] page = pages[pageId];
        if(page == null)
            page = pages[pageId] = new Object[256];
        page[c & 0xff] = value;
    }
    
    public void putInt(int c, int value)
    {
        int pageId = c >>> 8;
        int[] page = intPages[pageId];
        if(page == null)
            page = intPages[pageId] = new int[256];
        page[c & 0xff] = value;
    }
}
