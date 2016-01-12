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
 * Copyright (c) 2010 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.netutil;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * Implementation of {@link FileContent} based on an byte array.
 */
public final class ByteArrayContent implements FileContent {
    /**
     * The underlying byte array.
     */
    public final byte[] bytes;

    /**
     * The content type returned by {@link #getContentType}.
     * May be <code>null</code>.
     */
    public final String contentType;

    /**
     * The basename returned by {@link #getName}.
     * May be <code>null</code>.
     */
    public final String baseName;

    // -----------------------------------------------------------------------

    /**
     * Equivalent to {@link #ByteArrayContent(byte[], String, String)
     * this(bytes, null, null)}.
     */
    public ByteArrayContent(byte[] bytes) {
        this(bytes, null, null);
    }

    /**
     * Equivalent to {@link #ByteArrayContent(byte[], String, String)
     * this(bytes, contentType, null)}.
     */
    public ByteArrayContent(byte[] bytes, String contentType) {
        this(bytes, contentType, null);
    }

    /**
     * Constructs a file content object based on specified byte array.
     *
     * @param bytes the underlying byte array
     * @param contentType the content type associated to the contents 
     * of the byte array. May be <code>null</code>.
     * @param baseName a basename for the contents of the byte array.
     * May be <code>null</code>.
     */
    public ByteArrayContent(byte[] bytes, String contentType, String baseName) {
        this.bytes = bytes;
        this.contentType = contentType;
        this.baseName = baseName;
    }

    public String getName() {
        return baseName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return bytes.length;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }
}
