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
 * Copyright (c) 2009-2010 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.netutil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract representation of the file to be uploaded. 
 * (Should allow to upload objects other than mere files.)
 *
 * @see PostRequest
 */
public interface FileContent {
    /**
     * Returns the content type of this file content object.
     * May return <code>null</code> if unknown.
     */
    public String getContentType();

    /**
     * Returns the basename of this file content object.
     * May return <code>null</code> if not applicable.
     */
    public String getName();

    /**
     * Returns the size in bytes of this file content object.
     */
    public long getSize();

    /**
     * Returns an input stream allowing the read the contents of 
     * this file content object.
     * 
     * @exception IOException if an I/O error occurs.
     */
    public InputStream getInputStream() throws IOException;
}
