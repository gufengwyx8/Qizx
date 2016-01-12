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
package com.qizx.queries.iterators;

import com.qizx.api.EvaluationException;
import com.qizx.api.Node;


/**
 *  Enumerates Nodes in XMLDocuments of a XMLLibrary as the result of a query
 */
public interface NodeIterator extends DocumentIterator
{
//    /**
//     * Moves to the next node (possibly of the next document).
//     * Note: if {@link #nextDocument()} is used, this method must be called
//     * afterwards to actually get the first Node in the following document.
//     * @return true if there is a next Node, false otherwise.
//     */
//    boolean nextNode();
//
//    /**
//     * Returns the Node at current position.
//     */
//    Node currentNode();

    /**
     * [Internal use]. Returns the internal identifier of the current node.
     */
    int/*NId*/  getNodeId();

    double getFulltextScore(Node node) throws EvaluationException;
}
