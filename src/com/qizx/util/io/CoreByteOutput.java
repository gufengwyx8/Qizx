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


import java.io.IOException;

/**
 *  Output to a list of byte blocks.
 *  May be configured to work with a single block.
 */
public class CoreByteOutput extends ByteOutputBase
{
    byte[][] blocks;
    int []   blockSizes;
    int blockCount;
    
    public CoreByteOutput() {
        super(8192);
        blocks = new byte[8][];
        blockSizes = new int[blocks.length];
    }

    /**
     * Single block output (error if overflow).
     */
    public CoreByteOutput(byte[] data) {
        super(data);
        blocks = null;
    }
    
    public long getLength()
    {
        long len = 0;
        for (int i = 0; i < blockCount; i++)
            len += blockSizes[i];
        return len;
    }

    protected void writeBuffer(byte[] buffer, int size) {
        if(blocks == null)
            return;
        
        if(blockCount >= blocks.length) {
            byte[][] old = blocks;
            blocks = new byte[ old.length * 2 ][];
            System.arraycopy(old, 0, blocks, 0, old.length);
            int[] oldSizes = blockSizes;
            blockSizes = new int[ blocks.length ];
            System.arraycopy(oldSizes, 0, blockSizes, 0, oldSizes.length);
        }
        blockSizes[blockCount] = size;
        byte[] buf = new byte[size];
        System.arraycopy(data, 0, buf, 0, size); 
        blocks[blockCount ++] = buf;
    }
    
    public void close()  throws IOException {
        if(bufPtr > 0)
            flushBuffer();
        bufPtr = 0;
    }
}
