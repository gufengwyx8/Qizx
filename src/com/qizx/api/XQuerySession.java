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

import com.qizx.api.Indexing.WordSieve;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.util.fulltext.DefaultFullTextFactory;
import com.qizx.api.util.text.DefaultWordSieve;

/**
 * Simple XQuery session without access to XML Libraries, used in Qizx/open.
 */
public interface XQuerySession extends ItemFactory
{
    /**
     * Access to the default XQuery Context. This context is inherited
     * by expressions compiled from this session.
     * @return the XQuery context used as a basis for compiled expressions.
     */
    XQueryContext getContext();

    /**
     * Creates an executable Expression by compiling a XQuery script.
     * @param xquery a string containing a XQuery script.
     * @return an executable Expression that can be used for several
     *         executions. Before an execution, the expression can be prepared
     *         by binding values with variables
     * @exception CompilationException thrown after compilation if parsing or 
     * static analysis errors are detected. A CompilationException bears a list
     * of {@link Message}s.
     */
    Expression compileExpression(String xquery)
        throws CompilationException;
    
    /**
     * Defines a private Module resolver for the session. By default, the
     * Module resolver is defined globally on the XQuerySessionManager or
     * the LibraryManager.
     * @param resolver a Module resolver; if set to null, the default module
     * resolver will then be used for the session 
     */
    void setModuleResolver(ModuleResolver resolver);
    
    /**
     * Returns the private module resolver defined for the session.
     * @return null if no private module resolver was defined for the session
     */
    ModuleResolver getModuleResolver();
    
    
    /**
     * Defines the private full-text word tokenizer for the session. By
     * default it is an instance of {@link DefaultWordSieve}.
     * @throws DataModelException if the session is in fact an XML Library,
     * changing the word sieve can generate an exception (for example if it
     * is read-only).
     * @deprecated see {@link #setFullTextFactory}
     */
    void setWordSieve(WordSieve wordSieve) throws DataModelException;
    
    /**
     * Returns the private full-text word tokenizer defined for the session.
     * @return the private full-text word tokenizer defined for the session. By
     * default it is an instance of {@link DefaultWordSieve}
     * @deprecated see {@link #getFullTextFactory} {@link #setFullTextFactory}
     */
    WordSieve getWordSieve();
    
    /**
     * Enables a class for the 'Java Binding' mechanism.
     * <p>
     * <b>Attention: </b>the Java Binding mechanism is not enabled by default
     * when using the API. Each Java class has to be enabled specifically. It
     * is possible to enable all classes at once (though not advisable for
     * security reasons) by passing a null name.
     * <p><b>Attention: </b>once a class has been enabled, it can no longer 
     * be disabled in the same session.
     * @param className fully qualified class name (e.g
     *        java.sql.DriverManager). If the value 'null' is used instead of a
     *        class name, then <em>all Java</em> to Java extensions are
     *        enabled or disabled.
     */
    void enableJavaBinding(String className);

    /**
     * Redefines the full-text factory associated with the session. 
     * <p>A FullTextFactory provides access to- or allows redefining full-text
     * resources such as text tokenizer, stemming, thesaurus and scoring method.
     * <p>By default, an instance of {@link DefaultFullTextFactory} is used. It
     * is also possible to define a FullTextFactory on the {@link LibraryManager}
     * which controls a group of Libraries.
     * @param fulltextProvider a new Fulltext Provider.
     */
    void setFullTextFactory(FullTextFactory fulltextProvider);
    
    /**
     * Returns the Fulltext Provider associated with the session.
     * @return the Fulltext Provider associated with the session.
     */
    FullTextFactory getFullTextFactory();
}
