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
package com.qizx.api.util;

import com.qizx.api.Node;
import com.qizx.api.XMLPushStream;
import com.qizx.xdm.CorePushBuilder;

/**
 * An implementation of XMLPushStream that builds an in-memory Node tree and
 * returns the top Node.
 * <p>
 * Nodes are built by calling methods of {@link XMLPushStream} like
 * {@link XMLPushStream#putElementStart putElementStart},
 * {@link XMLPushStream#putAttribute putAttribute},
 * {@link XMLPushStream#putText putText},
 * {@link XMLPushStream#putElementEnd putElementEnd} etc. in the proper order,
 * or the method {@link XMLPushStream#putNodeCopy putNodeCopy}, or both.
 * Finally the {@link #reap()} method returns the top-level node built.
 * <p>
 * The reset() method should be called before reusing this object for building
 * another tree.
 * @since 2.1
 */
public class PushNodeBuilder extends CorePushBuilder
{
    public PushNodeBuilder()
    {
        super("");
    }

    /**
     * Returns the top-level node built with this object. If
     * {@link #putDocumentStart()} has been used first, this will be a document
     * node, else an element.
     * @return the top-level Node built with this object.
     */
    public Node reap()
    {
        return super.harvest();
    }
}
