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
public class ByteInputStream 
    extends ByteInputBase
{
    InputStream in;
    
    public ByteInputStream( File file ) throws FileNotFoundException {
        this(new FileInputStream(file));
    }
    
    public ByteInputStream( InputStream in ) {
        super(4096);
        this.in = in;
    }
    
    public ByteInputStream() {
        this( (InputStream) null );
    }
    
    protected int readBuffer() throws IOException {
        return in.read(data, 0, maxBufferSize);
    }
    
    public void close() throws IOException {
        in.close();
    }
}
