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
import java.util.Arrays;

/**
 * Abstract buffered ByteOutput.
 */
public abstract class ByteOutputBase
    implements ByteOutput, Serializable
{
    protected int    bufferSize;	// allocated size
    protected byte[] data;
    protected int    bufPtr;
    protected int    curFlushMark = -1;
    protected boolean trace = false;
    
    protected ByteOutputBase() {
        this(8192);
    }
    
    protected ByteOutputBase(int bufSize) {
        bufferSize = bufSize;
        data = new byte[bufferSize];
    }
    
    protected ByteOutputBase(byte[] buffer) {
        bufferSize = buffer.length;
        data = buffer;
    }
    
    /**
     * To implement in concrete classes.
     */
    protected abstract void writeBuffer(byte[] buffer, int size)
        throws IOException;
    
    /**
     * Discards any buffered data.
     */
    public void reset() {
        bufPtr = 0;
        curFlushMark = -1;
    }
    
    public void close() throws IOException {
        flush();
    }
    
    public void flush() throws IOException {
        if(bufPtr > 0)
            flushBuffer();
    }
    
    public void flushMark() throws IOException {
        curFlushMark = bufPtr;
    }
    
    protected void flushBuffer() throws IOException
    {
        if(trace) System.err.println("FlushMark "+curFlushMark+" "+bufPtr);
        int size = (curFlushMark > 0)? curFlushMark : bufPtr;
        writeBuffer(data, size);
        if(curFlushMark < 0) // normal mode (no mark)
            bufPtr = 0;
        else if(curFlushMark == 0) {
            // mark mode, cannot make room: increase buffer size
            byte[] old = data;
            bufferSize *= 2;
            data = new byte[bufferSize];
            System.arraycopy(old, 0, data, 0, bufPtr);
        }
        else { // copy leftover bytes to beginning of buffer:
            bufPtr -= size;
            System.arraycopy(data, size, data, 0, bufPtr);
            curFlushMark = 0;
        }
    }
    
    public void putByte(int b) throws IOException {
        putByte( (byte) b );
    }
    
    public void putByte(byte b) throws IOException
    {
        if(bufPtr >= bufferSize) {
            flushBuffer();
        }
        data[bufPtr ++] = b;
    }
    
    public void putBytes(byte[] buf, int length) throws IOException
    {
        putBytes(buf, 0, length);
    }
    
    public void putBytes(byte[] buf, int start, int bsize) throws IOException
    {
        if(trace) System.err.println(" put bytes "+bsize);
        for(; bsize > 0; ) {
            int L = Math.min(bufferSize - bufPtr, bsize);
            System.arraycopy(buf, start, data, bufPtr, L);
            bufPtr += L;
            if(bufPtr >= bufferSize)
                flushBuffer();
            bsize -= L;
            start += L;
        }
    }
    
    public void padding(int size, int value) throws IOException
    {
         for( ; size > 0; ) {
             int cnt = Math.min(bufferSize - bufPtr, size);
             Arrays.fill(data, bufPtr, bufferSize, (byte) value);
             size -= cnt;
             bufPtr += cnt;
             if(bufPtr >= bufferSize)
                 flushBuffer();
         }
    }

    public void putInt(int code) throws IOException
    {
        putByte((byte)(code >> 24));
        putByte((byte)(code >> 16)); 
        putByte((byte)(code >> 8));
        putByte((byte) code);
    }

    public void putVint( int code ) throws IOException
    {
        if(trace) System.err.println(" put int "+code);
        if(bufPtr <= bufferSize - 5)
            bufPtr = encodeInt(code, data, bufPtr);
        else
            slowPutVlong(code);
    }
    
    public void putLong(long code) throws IOException
    {
        putByte((byte)(code >> 56));
        putByte((byte)(code >> 48)); 
        putByte((byte)(code >> 40));
        putByte((byte)(code >> 32));
        putByte((byte)(code >> 24));
        putByte((byte)(code >> 16)); 
        putByte((byte)(code >> 8));
        putByte((byte) code);
    }

    public void putVlong( long code ) throws IOException
    {
        if(trace) System.err.println(" put long "+code);
        
        if(bufPtr <= bufferSize - 9)
            bufPtr = encodeLong(code, data, bufPtr);
        else
            slowPutVlong(code);
        
    }
    
    public void  putDouble(double value) throws IOException
    {
        if(trace) System.err.println(" put double "+value);
        long r = Double.doubleToRawLongBits(value);
        
        if(bufPtr <= bufferSize - 8) {
            data[bufPtr + 0] = (byte) (r >> 56);
            data[bufPtr + 1] = (byte) (r >> 48);
            data[bufPtr + 2] = (byte) (r >> 40);
            data[bufPtr + 3] = (byte) (r >> 32);
            data[bufPtr + 4] = (byte) (r >> 24);
            data[bufPtr + 5] = (byte) (r >> 16);
            data[bufPtr + 6] = (byte) (r >> 8);
            data[bufPtr + 7] = (byte) r;
            bufPtr += 8;
        }
        else
            slowPutBytes(r, 8);
    }
    
    public void  putString(String s) throws IOException
    {
        if(s == null)
            s = "";
        if(trace) System.err.println(" put chars "+s);
        int length = s.length();
        boolean wide = ByteOutputBase.isWideString(s);
        putVint((length << 1) + (wide ? 1 : 0));
        if(wide) {
            if(bufPtr + 2 * length <= bufferSize) {
                for(int i = 0; i < length; i++) {    
                    int p = bufPtr + 2 * i;
                    char c = s.charAt(i);
                    data[p] = (byte) (c >> 8);
                    data[p + 1] = (byte) c;
                }
                bufPtr += 2 * length;
            }
            else
                for(int i = 0; i < length; i++) {    
                    char c = s.charAt(i);
                    putByte( (byte) (c >> 8) );
                    putByte( (byte) c );
                }
        }
        else {
            if(bufPtr + length <= bufferSize) {
                for(int i = 0; i < length; i++) {    
                    char c = s.charAt(i);
                    data[bufPtr + i] = (byte) c;
                }
                bufPtr += length;
            }
            else
                for(int i = 0; i < length; i++) {    
                    char c = s.charAt(i);
                    putByte( (byte) c );
                }
        }
    }
    
    public void putChars(char[] chars, int start, int length) throws IOException
    {
        if(start < 0 || start + length > chars.length)
            throw new ArrayIndexOutOfBoundsException();
        if(trace) System.err.println(" put chars "+new String(chars));
        // putVint(length);
        boolean wide = ByteOutputBase.isWideString(chars, start, length);
        putVint((length << 1) + (wide ? 1 : 0));
        if(wide) {
            if(bufPtr + 2 * length <= bufferSize) {
                for(int i = 0; i < length; i++) {    
                    int p = bufPtr + 2 * i;
                    char c = chars[start + i];
                    data[p] = (byte) (c >> 8);
                    data[p + 1] = (byte) c;
                }
                bufPtr += 2 * length;
            }
            else
                for(int i = 0; i < length; i++) {    
                    char c = chars[start + i];
                    putByte( (byte) (c >> 8) );
                    putByte( (byte) c );
                }
        }
        else {
            if(bufPtr + length <= bufferSize) {
                for(int i = 0; i < length; i++) {
                    char c = chars[start + i];
                    data[bufPtr + i] = (byte) c;
                }
                bufPtr += length;
            }
            else
                for(int i = 0; i < length; i++) {    
                    char c = chars[start + i];
                    putByte( (byte) c );
                }
        }
    }
    
    //	fixed-width encoding (Unicode MSB first)
    //
    public void  putChars(char[] chars) throws IOException {
        putChars(chars, 0, chars.length);
    }
    
    public static int bytesForVint( int code )
    {
        if (code < 0) throw new IllegalArgumentException("negative code: "+code);
        if(code < 128)
            return 1;
        if(code < (1 << 14))
            return 2;
        if(code < (1 << 21))
            return 3;
        if(code < (1 << 28))
            return 4;
        return 5;
    }
    
    public static int  bytesForVlong( long code )
    {
        if(code < (1 << 28))
            return bytesForVint((int) code);
        if(code < (1L << 35))
            return 5;
        if(code < (1L << 42))
            return 6;
        return 7;   // no more than 500 terabytes...    
    }
    
    public static int bytesForSignedVlong( long code ) {
        return bytesForVlong( Math.abs(code) << 1 );
    }

    public static int bytesForString( String txt )
    {
        int L = txt.length();
        return bytesForVint(L << 1) + (isWideString(txt) ? 2*L : L);
    }

    public static boolean isWideString( char[] str, int start, int length )
    {
        for(int i = 0; i < length; i++ )
            if (str[start + i] >= 256)
                return true;
        return false;   
    }
    
    public static boolean isWideString( String str ) {
        for(int i = str.length(); --i >= 0; )
            if (str.charAt(i) >= 256)
                return true;
        return false;   
    }

    private void slowPutVlong( long code ) throws IOException
    {
        if(code < 0)
            throw new IllegalArgumentException("negative code "+code);
        int bytesMore = 0;
        if(code < 128)
            putByte( (int) code );
        else if(code < (1 << 14)) {
            putByte( (int) ((code >> 8) | 0x80) );
            bytesMore = 1;
        }
        else if(code < (1 << 21)) {
            putByte( (int) ((code >> 16) | 0xc0) );
            bytesMore = 2;
        }
        else if(code < (1 << 28)) {
            putByte( (int) ((code >> 24) | 0xe0) ); 
            bytesMore = 3;
        }
        else if(code < (1L << 35)) {
            putByte( (int) ((code >> 32) | 0xf0) );
            bytesMore = 4;
        }
        else if(code < (1L << 42)) {
            putByte( (int) ((code >> 40) | 0xf8) );
            bytesMore = 5;
        }
        else if(code < (1L << 48)) {
            putByte( (int) 0xfc ); 
            bytesMore = 6;
        }
        else if(code < (1L << 56)) {
            putByte( (int) 0xfd ); 
            bytesMore = 7;
        }
        else {
            putByte( (int) 0xfe ); 
            bytesMore = 8;
        }
        slowPutBytes(code, bytesMore);
    }
    
    private void slowPutBytes(long code, int bytes) throws IOException {
        int shift = (bytes - 1) * 8;
        for(int i = 0; i < bytes; i++, shift -= 8)
            putByte( (int) (code >> shift) );
    }
    
    /**
     *	Stores a positive int in variable-length encoding. Assumes that the buffer
     *	is large enough.
     *	@return the new buffer size.
     */
    public static int encodeInt(int code, byte[] buffer, int bufSize)
    {
        if(code < 0)
            throw new IllegalArgumentException("negative code "+code);
        if(code < 128) {
            buffer[bufSize] = (byte) code;
            return bufSize + 1;
        }
        else if(code < (1 << 14)) {
            buffer[bufSize] = (byte) ((code >> 8) | 0x80);
            buffer[bufSize + 1] = (byte) code;
            return bufSize + 2;
        }
        else if(code < (1 << 21)) {
            buffer[bufSize] = (byte) ((code >> 16) | 0xc0);
            buffer[bufSize + 1] = (byte) (code >> 8); 
            buffer[bufSize + 2] = (byte) code;
            return bufSize + 3;
        }
        else if(code < (1 << 28)) {
            buffer[bufSize] = (byte) ((code >> 24) | 0xe0); 
            return encodeBytes(code, 4, buffer, bufSize);
        }
        else {
            buffer[bufSize] = (byte) 0xf0; 
            return encodeBytes(code, 5, buffer, bufSize);
        }
    }
    
    public static int encodeLong(long code, byte[] buffer, int bufSize)
    {
        if(code < 0)
            throw new IllegalArgumentException("negative code "+code);
        if(code < (1 << 28))
            return encodeInt((int) code, buffer, bufSize);
        if(code < (1L << 35)) {
            buffer[bufSize] = (byte) ((code >> 32) | 0xf0);
            return encodeBytes(code, 5, buffer, bufSize);
        }
        else if(code < (1L << 42)) {
            buffer[bufSize] = (byte) ((code >> 40) | 0xf8);
            return encodeBytes(code, 6, buffer, bufSize);
        }
        else if(code < (1L << 48)) {
            buffer[bufSize] = (byte) 0xfc; 
            return encodeBytes(code, 7, buffer, bufSize);
        }
        else if(code < (1L << 56)) {
            buffer[bufSize] = (byte) 0xfd; 
            return encodeBytes(code, 8, buffer, bufSize);
        }
        else {
            buffer[bufSize] = (byte) 0xfe; 
            return encodeBytes(code, 9, buffer, bufSize);
        }
    }
    
    // puts (byteCnt - 1) lower bytes. (total -> byteCnt)
    private 
    final static int encodeBytes( long code, int byteCnt, byte[] buffer, int bufSize)
    {
        
        int shift = (byteCnt - 2) * 8;
        for(int i = 1; i < byteCnt; i++, shift -= 8)
            buffer[bufSize + i] = (byte) (code >> shift);
        return bufSize + byteCnt;
    }

    public int getPointer()
    {
        return bufPtr;
    }
}

