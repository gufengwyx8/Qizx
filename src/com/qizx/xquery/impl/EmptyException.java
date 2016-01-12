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
package com.qizx.xquery.impl;

import com.qizx.api.EvaluationException;

/**
 * [Internal use] Thrown when a empty Value is encountered in a context that
 * cannot handle it. Can be caught be enclosing expressions that accept empty
 * sequences.
 */
public class EmptyException extends EvaluationException
{
    public EmptyException(String reason)
    {
        super(reason);
    }

    /**
     * Unique instance.
     * <p>
     * Throwing a statically built exception is perfectly possible in Java.
     * This saves the time needed to copy the call stack into the exception, at
     * the cost of a meaningless printStackTrace(). But we dont care here,
     * because this is meant to simplify the handling of optionally empty
     * arguments.
     */
    public static EmptyException instance()
    {
        return instance;
        // return new EmptyException("unexpected empty sequence");
    }

    /**
     * Unique instance.
     */
    public static EmptyException instance =
        new EmptyException("unexpected empty sequence");
}
