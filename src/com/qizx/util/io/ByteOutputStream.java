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

package com.qizx.util.io;

import java.io.*;

/**
 *	OutputByteStream writing to a standard OutputStream.
 */
public class ByteOutputStream
    extends ByteOutputBase
{
    OutputStream out;
    
    public ByteOutputStream( File file ) throws FileNotFoundException {
        this(new FileOutputStream(file));
    }
    
    public ByteOutputStream( OutputStream out ) {
        super(4096);
        this.out = out;
    }
    
    protected void writeBuffer(byte[] buffer, int size) throws IOException {
        out.write(data, 0, size);
    }
    
    public void flush() throws IOException {
        flushBuffer();
        out.flush();
    }
    
    public void close() throws IOException {
        flush();
        out.close();
    }
    
    /**
     *	Closes and synchronizes the file on disk.
     *	CAUTION: assumes the underlying OutputStream is a FileOutputStream.
     */
    public void syncClose() throws IOException {
        flush();
        ((FileOutputStream) out).getFD().sync();
        close();
    }
}
