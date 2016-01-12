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
import java.io.Serializable;

/**
 *    Reads bytes from a list of data blocks.
 *    Blocks are first added, then reading can begin. Blocks can be
 *    of variable size, but only the last one may be incompletely filled.
 */
public class CoreByteInput extends ByteInputBase 
    implements Serializable
{
    byte[][] blocks;
    int []   blockSizes;
    int blockCount;
    int blockPtr;
    
    public CoreByteInput() {
        super(8192);
        blocks = new byte[ 4 ][];
        blockSizes = new int[blocks.length];
    }
    
    public CoreByteInput( CoreByteOutput source ) {
        super(source.bufferSize);
        blocks = source.blocks;
        blockSizes = source.blockSizes;
        blockCount = source.blockCount;
    }

    public CoreByteInput(byte[] data, int size) {
        super(data, size);
    }
    
    public void restartOn(byte[] buffer, int size) throws IOException {
        this.data = buffer;
        this.bufSize = size;
        this.ptr = 0;
    }
    
    protected int readBuffer() {
        
        if(blockPtr >= blockCount)
            return -1;
        int bkSize = (blockSizes == null)? blocks[blockPtr].length
                     : blockSizes[blockPtr];
        System.arraycopy(blocks[blockPtr], 0, data, 0, bkSize);
        ++ blockPtr;
        return bkSize;	
    }
    
    public void store(byte[] data, int size) {
        if(blockCount >= blocks.length) {
            byte[][] old = blocks;
            blocks = new byte[ old.length * 2 ][];
            System.arraycopy(old, 0, blocks, 0, old.length);
            int[] olds = blockSizes;
            blockSizes = new int [ old.length * 2 ];
            System.arraycopy(olds, 0, blockSizes, 0, old.length);
        }
        blocks[blockCount] = data;
        blockSizes[blockCount ++] = size;
    }
}
