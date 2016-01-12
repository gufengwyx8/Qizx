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
 *      Abstract interface for binary IO.
 */
public interface ByteInput
{
    int getByte()	throws IOException;
    
    int getBytes(byte[] buf)	throws IOException;
    
    /**
     * Reads an int on 4 bytes, MSB first.
     */
    int getInt()   throws IOException;
    /**
     * Reads an unsigned int in variable length.
     */
    int getVint()  throws IOException;
    /**
     * Reads an unsigned long in 8 bytes, MSB first.
     */
    long  getLong()    throws IOException;
    /**
     * Reads an unsigned long in variable length.
     */
    long  getVlong()    throws IOException;
    /**
     * Reads a IEEE double on 8 bytes.
     */
    double getDouble()	throws IOException;

    char[] getChars() throws IOException;
    
    void getChars (char[] buffer, int pos, int length, boolean wide)
	    throws IOException;

    String getString()	throws IOException;
    
    void close() throws IOException;
    
    void inspect();
}
