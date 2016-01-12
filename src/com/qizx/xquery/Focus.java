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
import com.qizx.api.Node;

/**
 *	Provides access to context item, position and size.
 *	Passed to every eval method, and typically implemented by 
 *	expressions that modify the focus, namely Path and Filter.
 *	(internal use).
 */
public interface Focus
{
    /**
     *	Gets the current item.
     */
    XQItem currentItem() throws EvaluationException;

    /**
     *	Gets the current item as an integer for optimization.
     */
    long currentItemAsInteger() throws EvaluationException;

    /**
     *	Gets the current item as a double for optimization.
     */
    double currentItemAsDouble() throws EvaluationException;

    /**
     *  Gets the current item as a String for optimization.
     */
    String currentItemAsString() throws EvaluationException;

    /**
     *  Gets the current item as a Node for optimization.
     */
    Node currentItemAsNode() throws EvaluationException;

    /**
     * Gets current item position.
     */
    long  getPosition();

    /**
     *	Gets context size.
     */
    long  getLast() throws EvaluationException;
}
