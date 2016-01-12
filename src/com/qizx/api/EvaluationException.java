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

import com.qizx.xdm.IQName;
import com.qizx.xquery.XQValue;


/**
 * Exception thrown by an error in the execution of an XQuery Expression.
 */
public class EvaluationException extends QizxException
{
    /** Error code used when the time limit for evaluation has been reached. */
    public static final QName TIME_LIMIT = IQName.get("TIME_LIMIT");
    /** Error code used when the evaluation has been explicitly cancelled. */
    public static final QName CANCELLED = IQName.get("CANCELLED");
    
    protected static final QName DEFAULT_ERR_CODE = IQName.get("XQST0054");
    
    private static final EvaluationStackTrace[] NO_STACK =
        new EvaluationStackTrace[0];
    
    private EvaluationStackTrace[] stack;
    private XQValue value;      // for fn:error
    
    /**
     * Constructs an EvaluationException from a simple message.
     * The error code is undefined.
     */
    public EvaluationException(String message)
    {
        super(DEFAULT_ERR_CODE, message);
        stack = NO_STACK;
    }

    /**
     * Constructs an EvaluationException from a simple message and an error code.
     */
    public EvaluationException(QName xqueryCode, String message)
    {
        super(xqueryCode, message);
        stack = NO_STACK;
    }

    /**
     * Constructs an EvaluationException from a simple message and an exception.
     * The error code is undefined.
     */
    public EvaluationException(String message, Throwable cause)
    {
        super(message, cause);
        setErrorCode(DEFAULT_ERR_CODE);
        stack = NO_STACK;
    }

    /**
     * Constructs an EvaluationException from a simple message, an error code
     * and an exception.
     */
    public EvaluationException(QName xqueryCode, String message, Throwable cause)
    {
        super(message, cause);
        setErrorCode(xqueryCode);
        stack = NO_STACK;
    }

    /**
     * Returns the execution stack at the moment of the error.
     * <p>
     * @return an array of EvaluationStackTrace objects. The first array
     *         element corresponds to the innermost execution level. The last
     *         element corresponds to the main query.
     */
    public EvaluationStackTrace[] getStack()
    {
        return stack;
    }

    /**
     * For internal use.
     */
    public void setStack(EvaluationStackTrace[] stack)
    {
        this.stack = stack;
    }

    public XQValue getValue()
    {
        return value;
    }

    public void setValue(XQValue value)
    {
        this.value = value;
    }
}
