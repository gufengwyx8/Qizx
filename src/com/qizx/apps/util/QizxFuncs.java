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
package com.qizx.apps.util;

import com.qizx.api.QizxException;

import java.io.File;


/**
 * Utility functions that can be called in XQuery through Java binding.
 */
public class QizxFuncs
{
    
    public static void fatal(String message)
    {
        System.err.println(message);
        System.exit(1);
    }
    
    public static void usage(String message)
    {
        System.err.println("usage: " + message);
        System.exit(2);
    }
}
