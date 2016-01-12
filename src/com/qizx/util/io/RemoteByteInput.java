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



/**
 *    Reads bytes from a remote connection (TODO, this is a mock class)
 *    
 */
public class RemoteByteInput extends CoreByteInput
{
    /*
    interface Pipe extends Remote {
        byte[] getBytes();
    }
   
    public StreamedRemoteByteInput( Pipe pipe );
 */
    

    // temporary implementation
    public RemoteByteInput() { }
    
    public RemoteByteInput( CoreByteOutput source ) {
        super(source);
    }

    public void dump() {
        System.err.println(blockCount+" blocks");
        for(int b = 0; b < blockCount; b++)
            System.err.println(b+" size "+blockSizes[b]);
    }
}
