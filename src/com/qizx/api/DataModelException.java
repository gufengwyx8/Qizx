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

/**
 * Exception raised by operations on XML data.
 */
public class DataModelException extends QizxException
{
    /**
     * Default constructor, should not be used.
     */
    protected DataModelException()
    {
    }

    /**
     * Constructs a DataModelException from a simple message. The error code
     * is undefined.
     * @param message reason for the exception
     */
    public DataModelException(String message)
    {
        super(message);
    }

    /**
     * Constructs a DataModelException from a simple message and an exception.
     * The error code is undefined.
     * @param message reason for the exception
     * @param cause wrapped cause
     */
    public DataModelException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a DataModelException with a message and an XQuery error code.
     * @param errorCode the QName of the error (in principle the XQuery error
     * namespace <code>http://www.w3.org/2005/xqt-errors</code>)
     * @param message reason for the exception
     */
    public DataModelException(QName errorCode, String message)
    {
        super(errorCode, message);
    }

    /**
     * Constructs a DataModelException with a message and an XQuery error code
     * in string form (uses the err: namespace).
     * @param code XQuery code such as XPTY0004
     * @param message reason for the exception
     */
    public DataModelException(String code, String message)
    {
        super(message);
        setErrorCode(code);
    }
}
