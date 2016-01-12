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


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *  Temporary buffer, using a temp file if size goes over buffer size.
 */
public class TempByteOutput extends ByteOutputBase
{
    protected File tempFile;
    protected FileOutputStream out;
    protected File tempDir;
    
    public TempByteOutput(int bufferSize, File tempDir)
    {
        super(bufferSize);
        this.tempDir = tempDir;
    }
    
    public TempByteOutput(int bufferSize)
    {
        this(bufferSize, null);
    }
    
    public long getLength()
    {
        long len = bufPtr;
        if(tempFile != null)
            len += tempFile.length();
        return len;
    }

    protected void writeBuffer(byte[] buffer, int size) throws IOException
    {
        if(tempFile == null) {
            tempFile = File.createTempFile("bio", null, tempDir);
            out = new FileOutputStream(tempFile);
        }
        out.write(buffer, 0, size);
    }
    
    public void close()  throws IOException
    {
        // flush only if file used
        if(bufPtr > 0 && out != null) {
            flushBuffer();
            bufPtr = 0;
            out.close();
        }
    }

    public File getFile()
    {
        return tempFile;
    }
}
