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
 *	
 */
public abstract class ByteInputBase //extends InputStream
    implements ByteInput, Serializable

{
    protected int    maxBufferSize;
    protected byte[] data;
    protected int    bufSize;
    protected int    ptr;
    protected char[] strBuffer = new char[128];
    protected int    blocksRead;    // count
    
    public ByteInputBase() {
        this(4096);
    }
    
    public ByteInputBase( int bufferSize ) {
        data = new byte[maxBufferSize = bufferSize];
        reset();
    }
    
    public ByteInputBase( byte[] buffer, int size )
    {
        data = buffer;
        bufSize = size;
        ptr = 0;
    }
    
    public void reset()
    {
        bufSize = ptr = 0;
        blocksRead = 0;
    }
    
    public void close()
        throws IOException
    {
    }

    // fills the buffer and returns the size read (or -1 on eof)
    protected abstract int readBuffer()
        throws IOException;

    public long tell()
    {
        return (blocksRead - 1) * data.length + ptr;
    }
    
    public int getByte()
        throws IOException
    {
        if (ptr >= bufSize) {
            int L = readBuffer();
            if (L <= 0) {
                return -1;
            }
            ++blocksRead;
            bufSize = L;
            ptr = 1;
            return data[0] & 0xff;
        }
        return data[ptr++] & 0xff;
    }

    public int getBytes(byte[] buffer)
        throws IOException
    {
        return getBytes(buffer, 0, buffer.length);
    }

    public int getBytes(byte[] buffer, int start, int reqsize)
        throws IOException
    {
        int readsize = 0, L;
        
        for(; readsize < reqsize; ) {
            if(ptr >= bufSize) {
                L = readBuffer();
                ++ blocksRead;
                if(L <= 0)
                    return readsize;
                bufSize = L;
                ptr = 0;
            }
            L = Math.min(bufSize - ptr, reqsize - readsize);
            System.arraycopy(data, ptr, buffer, start, L);
            ptr += L;
            readsize += L;
            start += L;
        }
        return readsize;
    }
 
    public int getInt()   throws IOException
    {
        int b3 = getByte() & 0xff;
        int b2 = getByte() & 0xff;
        int b1 = getByte() & 0xff;
        int b0 = getByte() & 0xff;
        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    public int getVint() throws IOException {
        if(ptr <= bufSize - 5)
            return (int) decode();
        else
            return (int) slowGetVlong();
    }
    
    public long getLong() throws IOException {
        long r = getByte() & 0xff;
        for (int i = 0; i < 7; i++) {
            r = (r << 8) | (getByte() & 0xff);
        }
        return r;
    }
    
    public long getVlong() throws IOException {
        if(ptr <= bufSize - 9)
            return decode();
        else
            return slowGetVlong();
    }
    
    public double getDouble() throws IOException {
        long r = getByte();
        for(int i = 0; i < 7; i++)
            r = (r << 8) | getByte();
        
        return Double.longBitsToDouble(r);
    }
    
    public void getChars (char[] buffer, int pos, int length, boolean wide)
        throws IOException
    {
        if(length < 0 || pos + length > buffer.length)
            length = buffer.length - pos;
        if (wide) {
            if(ptr + 2 * length < bufSize) {    // optimize
                for(int i = 0; i < length; i++) {
                    int p = ptr + 2 * i;
                    int msb = data[p], lsb = data[p + 1] & 0xff;
                    buffer[pos + i] = (char) ((msb << 8) + lsb);
                }
                ptr += 2 * length;
            }
            else
                for(int i = 0; i < length; i++) {
                    int hi = getByte();
                    int c = (hi << 8) + (getByte() & 0xff);
                    buffer[pos + i] = (char) c;
                }
        }
        else {
            if(ptr + length < bufSize) {	// optimize
                for(int i = 0; i < length; i++)
                    buffer[pos + i] = (char) (data[ptr + i] & 0xff);
                ptr += length;
            }
            else
                for(int i = 0; i < length; i++)
                    buffer[pos + i] = (char) (getByte() & 0xff);
        }
    }
    
    public String getString() throws IOException
    {
        int L = getVint();
        boolean wide = (L & 1) != 0;
        L >>= 1;
        if(L > strBuffer.length)
            strBuffer = new char[L + L / 4];
        getChars(strBuffer, 0, L, wide);
        return new String(strBuffer, 0, L);
    }
    
    public char[] getChars() throws IOException
    {
        int L = getVint();
        boolean wide = (L & 1) != 0;
        L >>= 1;
        char[] chars = new char[L];
        getChars(chars, 0, L, wide);
        return chars;
    }
    
    public void getChars(char[] chars, int length) throws IOException
    {
        getChars(chars, 0, length, true);
    }
    
    private long slowGetVlong() throws IOException
    {
        int b = getByte(), b2, b3;
        
        if(b < 128)
            return b;
        switch( b & 0x7f ) {
        default:
            // 14 bits
            return ((b & 0x3f) << 8) | getByte();
        
        case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71: 
        case 72: case 73: case 74: case 75: case 76: case 77: case 78: case 79: 
        case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87: 
        case 88: case 89: case 90: case 91: case 92: case 93: case 94: case 95:
            // 21 bits:
            b2 = getByte();
            b3 = getByte();
            return ((b & 0x1f) << 16) | (b2 << 8) | b3;
            
        case 96: case 97: case 98: case 99:
        case 100: case 101: case 102: case 103: 
        case 104: case 105: case 106: case 107:
        case 108: case 109: case 110: case 111:
            // 28 bits:
            return slowGetBytes(b & 0x0f, 3);
            
        case 112: case 113: case 114: case 115:
        case 116: case 117: case 118: case 119:
            // 35 bits:
            return slowGetBytes(b & 0x07, 4);
            
        case 120: case 121: case 122: case 123:  // 42 bits:
            return slowGetBytes(b & 0x03, 5);
        case 124:
            return slowGetBytes(0, 6);	// 48 bits
        case 125:
            return slowGetBytes(0, 7);	// 56 bits
        case 126:
            return slowGetBytes(0, 8);	// 64 bits
        case 127:	    
            throw new RuntimeException("bad header for Vlong");
        }
    }
    
    private long slowGetBytes(long prefix, int bytes) throws IOException
    {
        int shift = (bytes - 1) * 8;
        prefix <<= shift + 8;
        for(int i = 0; i < bytes; i++, shift -= 8)
            prefix |= ((long) getByte()) << shift;
        return prefix;
    }
    
    public long decode()
    {
        byte b = data[ptr];
        
        if(b >= 0) {
            ++ ptr; return b;
        }
        
        switch( b & 0x7f ) {
        default:
            // 14 bits
            ptr += 2;
        return ((b & 0x3f) << 8) | (data[ptr - 1] & 0xff);
        
        case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71: 
        case 72: case 73: case 74: case 75: case 76: case 77: case 78: case 79: 
        case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87: 
        case 88: case 89: case 90: case 91: case 92: case 93: case 94: case 95:
            // 21 bits:
            ptr += 3;
            return ((b & 0x1f) << 16)
                | ((data[ptr-2] & 0xff) << 8)
                | (data[ptr-1] & 0xff);
            
        case 96: case 97: case 98: case 99:
        case 100: case 101: case 102: case 103: 
        case 104: case 105: case 106: case 107:
        case 108: case 109: case 110: case 111:
            // 28 bits:
            ptr += 4;
            int lo = ((b & 0xf) << 24) | ((data[ptr-3] & 0xff) << 16)
                   | ((data[ptr-2] & 0xff) << 8) | (data[ptr-1] & 0xff);
            return lo & 0xffffffffL;

        case 112: case 113: case 114: case 115:
        case 116: case 117: case 118: case 119:
            // 35 bits:
            return decodeBytes(b & 0x07, 4);
        case 120: case 121: case 122: case 123:  // 42 bits:
            return decodeBytes(b & 0x03, 5);
        case 124:
            return decodeBytes(0, 6);	// 48 bits
        case 125:
            return decodeBytes(0, 7);	// 56 bits
        case 126:
            return decodeBytes(0, 8);	// 64 bits
        case 127:	    
            throw new RuntimeException("bad header for Vlong");
        }
    }
    
    private long decodeBytes(long prefix, int bytes)
    {
        int shift = (bytes - 1) * 8;
        prefix <<= shift + 8;
        for(int i = 1; i <= bytes; i++, shift -= 8)
            prefix |= ((long)(data[ptr + i] & 0xff)) << shift;
        ptr += bytes + 1;
        return prefix;
    }
    
    // ------------- InputStream ----------------------
    
    public int read(byte[] buffer) throws IOException {
        return getBytes(buffer, 0, buffer.length);
    }
    
    public int read(byte[] buffer, int off, int len) throws IOException {
        int L = getBytes(buffer, off, len);
        
        return L;
    }
    
    public int read() throws IOException {
        return getByte();
    }

    public void inspect() {
         System.err.println(getClass()+" ptr="+ptr+" bufSize="+bufSize);
    }
}
