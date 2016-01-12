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
package com.qizx.xdm;

import com.qizx.api.Node;
import com.qizx.api.QName;

public interface NodeFilter
{
    /**
     * Optimized test method. Can be used if only the node-kind and its name
     * are necessary. This condition can be tested with method
     * {@link #needsNode}.
     * 
     * @return true if node matched. A null name is always accepted.
     */
    boolean accepts(int kind, QName name);

    /**
     * Generic test method.
     * 
     * @return true if node matched. Used for extended tests like DocumentTest.
     */
    boolean accepts(Node node);

    /**
     * Tells whether more information than the node kind and its name are
     * necessary to perform the test.
     * @return true if the node itself is necessary for checking (not only the
     *         kind and name). Allows optimization (avoids building a Node just
     *         for testing). Returns true for extended tests like DocumentTest.
     */
    boolean needsNode();

    /**
     * Returns the node kind (Node.ELEMENT, Node.TEXT etc) specifically
     * matched, or -1 if several kinds can be matched.
     */
    int getNodeKind();

    /**
     * Returns true for simple node test (node kind only).
     */
    boolean staticallyCheckable();

}
