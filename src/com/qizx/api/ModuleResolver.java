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
package com.qizx.api;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Resolves a module URI specified by a XQuery 'import module' declaration.
 */
public interface ModuleResolver
{
    /**
     * Resolves a module namespace to one or several compilation units.
     * 
     * @param moduleNamespaceURI target namespace of the module to import.
     * @param locationHints optional URI's used as resolution hints.
     * @return one or several physical locations of module units.
     * @throws MalformedURLException if provided module URI's or hints are invalid
     */
    URL[] resolve(String moduleNamespaceURI, String[] locationHints)
        throws MalformedURLException;
}
