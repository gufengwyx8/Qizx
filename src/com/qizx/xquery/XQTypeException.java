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
package com.qizx.xquery;

import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.xquery.op.Expression;

/**
 *	Dynamic Type checking exception (could also be named CastException).
 */
public class XQTypeException extends EvaluationException
{
    static QName ERR_TYPE = 
        ModuleContext.xqueryErrorCode(Expression.ERRC_BADTYPE);
    
    public XQTypeException( String reason ) {
        super(reason);
        setErrorCode(ERR_TYPE);
    }
    
    public XQTypeException( String reason, Exception e ) {
        super(reason, e);
        setErrorCode(ERR_TYPE);
    }
    
    public XQTypeException( String xqcode, String reason ) {
        super(reason);
        setErrorCode(ModuleContext.xqueryErrorCode(xqcode));
    }

    public XQTypeException(QName errorCode, String message)
    {
        super(errorCode, message);
    }
} 
