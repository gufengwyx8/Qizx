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
package com.qizx.apps.studio.gui;

import java.io.File;
import javax.swing.filechooser.*;

public class GlobFileFilter extends FileFilter
{
    private String suffix;
    
    public GlobFileFilter(String extension) {
        suffix = "." + extension.toLowerCase();
    }
    
    public String getFileNameSuffix() {
        return suffix;
    }
    
    public boolean accept(File file)
    {
        if (file.isDirectory()) { 
            return true; // always wanted.
        }
        else {
            String name = file.getName().toLowerCase();
            return name.endsWith(suffix);
        }
    }
    
    public String getDescription() {
        return "*" + suffix;
    }
    
    public String toString() {
        return getDescription();
    }
    
    public int hashCode() {
        return suffix.hashCode();
    }
    
    public boolean equals(Object other) {
        if (other == null || !(other instanceof GlobFileFilter))
            return false;
        return suffix.equals(((GlobFileFilter) other).suffix);
    }
}
