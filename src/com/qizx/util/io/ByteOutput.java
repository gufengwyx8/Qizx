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
 *	Abstract interface for binary IO.	
 *  See also static methods bytesForXXX in ByteOutputBase.
 */
public interface ByteOutput
{
    void putByte(int b)
        throws IOException;
    
    void putBytes(byte[] buf, int length)
        throws IOException;
    
    void padding(int size, int value)
        throws IOException;

    //void putBytes(byte[] buf, int start, int length) throws IOException;
    
    /**
     * Writes a signed int as 4 bytes (MSB first)
     */
    void putInt( int code )
        throws IOException;
    /**
     * Writes an unsigned int in variable size.
     */
    void putVint( int code )
        throws IOException;

    /**
     * Writes a signed long as 8 bytes (MSB first)
     */
    void putLong(long l)
        throws IOException;

    void putVlong( long code )
        throws IOException;
    
    void putDouble(double value)
        throws IOException;
    
    void putString(String s)
        throws IOException;
    
    void putChars(char[] chars, int start, int length)
        throws IOException;

    /** 
     * Always performs a flush.
     */ 
    void close()
        throws IOException;

    /** 
     * Forces writing of the buffer.
     */ 
    void flush()
        throws IOException;
    
    /** marks the beginning of a non-breakable segment: if used, a flush
     * should happen only on such a mark.
     */ 
    void flushMark()
        throws IOException;
}
