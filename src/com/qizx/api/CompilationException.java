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
package com.qizx.api;

import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xquery.MainQuery;

/**
 * Thrown at the end of the compilation of an Expression if there are errors.
 * <p>
 * Carries one or several messages (error, warning or detail).
 */
public class CompilationException extends QizxException
{
    private static final QName DEFAULT_CODE =
        IQName.get(NamespaceContext.ERR, "XPST0003");
    
    private Message[] messages;
    public MainQuery query; // parsed expression if possible
    
    /**
     * For internal use.
     */
    public CompilationException(String reason)
    {
        super(reason);
        setErrorCode(DEFAULT_CODE);
    }

    /**
     * Returns a list of compilation error or warning Messages.
     * @return a non-null array of Message objects generated by a compilation
     */
    public Message[] getMessages()
    {
        return messages;
    }

    /**
     * For internal use.
     * @param messages array of associated Message objects 
     */
    public void setMessages(Message[] messages)
    {
        this.messages = messages;
    }
    
    /**
     * Returns the number of actual errors.
     * @return an int representing the error count value
     */
    public int getErrorCount()
    {
        int count = 0;
        for (int i = 0; i < messages.length; i++) {
            if(messages[i].getType() == Message.ERROR)
                ++ count;            
        }
        return count;
    }
}
