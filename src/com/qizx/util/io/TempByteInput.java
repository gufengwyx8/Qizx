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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 *    Reads bytes from a list of data blocks.
 *    Blocks are first added, then reading can begin. Blocks can be
 *    of variable size, but only the last one may be incompletely filled.
 */
public class TempByteInput extends ByteInputBase 
    implements Serializable
{
    private File tempFile;
    private FileInputStream input;
    
    public TempByteInput(TempByteOutput source) throws IOException
    {
        super(source.bufferSize);
        tempFile = source.tempFile;
        if(tempFile == null) {  // data only in buffer
            bufSize = source.bufPtr;
            System.arraycopy(source.data, 0, data, 0, bufSize);
        }
        else {
            input = new FileInputStream(tempFile);
        }
    }

//    public void restartOn(byte[] buffer, int size)
//        throws IOException
//    {
//        this.data = buffer;
//        this.bufSize = size;
//        this.ptr = 0;
//    }
    
    @Override
    public void close()
        throws IOException
    {
        if(tempFile != null) {
            tempFile.delete();
            tempFile = null;
        }
    }

    protected int readBuffer() throws IOException
    {
        return (input == null)? -1 : input.read(data, 0, data.length);
    }
}
