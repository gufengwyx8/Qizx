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
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

/**
 * Implementation of {@link FileContent} based on an actual file.
 */
public final class FileContentImpl implements FileContent {
    /**
     * The underlying file.
     */
    public final File file;

    /**
     * The content type of the above file. May be <code>null</code>.
     */
    public final String contentType;

    // -----------------------------------------------------------------------

    /**
     * Constructs a file content object based on specified file and 
     * having a <code>null</code> content type.
     */
    public FileContentImpl(File file) {
        this(file, null);
    }

    /**
     * Constructs a file content object based on specified file and 
     * having specified content type.
     */
    public FileContentImpl(File file, String contentType) {
        this.file = file;
        this.contentType = contentType;
    }

    public String getName() {
        return file.getName();
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return file.length();
    }

    public InputStream getInputStream() 
        throws IOException {
        return new FileInputStream(file);
    }
}
