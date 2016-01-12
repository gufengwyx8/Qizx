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


/**
 * Enumerates Documents of a Library.
 * <p>Returned by the localGetDocuments method on XMLLibrary or XMLCollection,
 * or by a meta-query (query on metadata properties of documents).
 * <p>This iterator works like all other iterators in XQuest: a
 * <code>nextDocument</code> method attempts to move to the next item, then
 * if and only if this method returned true, current values are available,
 * here through the methods currentDocument and currentDocumentNode.
 */
public interface DocumentIterator 
{
    /**
     * Moves to the next document. Returns false if no more documents.
     */
    boolean goNextDoc() throws EvaluationException;
//
//    /**
//     * Returns the descriptor of the current document.
//     */
//    Document getCurrentDocument() throws DataModelException;
//
//    /**
//     * Returns the root node of the current document.
//     */
//    Node currentDocumentNode() throws DataModelException;
//
//    /**
//     * Returns the XMLLibrary related to this iterator.
//     */
//    XMLLibrarySession getLibrary();
//
//    /**
//     * [Internal use] Returns the data-model representation of the current document.
//     */
//    FONIDocument currentDataModel() throws DataModelException;
//
//    /**
//     * [Internal use] Returns the internal identifier of the current document.
//     */
//    int/*DId*/ currentDocId();
//
//    /**
//     * [Internal use] Returns the related set of document identifiers.
//     */
//    IntSet getDocSet() throws DataModelException;
}
