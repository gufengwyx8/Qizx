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

import com.qizx.api.util.DefaultModuleResolver;
import com.qizx.util.basic.Check;
import com.qizx.xdm.DocumentPool;
import com.qizx.xquery.ModuleManager;
import com.qizx.xquery.XQuerySessionImpl;

import java.net.URL;

/**
 * Manager of simple XQuery sessions without access to a XML Library.
 * <p>
 * Provides a cache of Modules and a cache of transient documents, shared among
 * the sessions created on this manager. This cache avoids reparsing XML
 * documents if different sessions access it. It can detect a change on
 * documents stored in the file-system and reload the document.
 */
public class XQuerySessionManager
{
    private ModuleManager moduleMan;
    private DocumentPool  documentCache;

    /**
     * Creates a session manager with a default Module Resolver and 
     * a default cache for parsed documents.
     * 
     * @param moduleBaseURI base URI for the default Module Resolver
     */
    public XQuerySessionManager(URL moduleBaseURI)
    {
        this(new DefaultModuleResolver(moduleBaseURI), -1);
    }

    /**
     * Creates a session manager.
     * @param moduleResolver resolver used for modules
     * @param transientDocumentCacheSize size in bytes of the document cache
     */
    public XQuerySessionManager(ModuleResolver moduleResolver,
                                int transientDocumentCacheSize)
    {
        moduleMan = new ModuleManager(moduleResolver);
        documentCache = new DocumentPool();
        if(transientDocumentCacheSize >= 0)
            documentCache.setCacheSize(transientDocumentCacheSize);
    }

    /**
     * Creates a new XQuery session.
     * @return a new XQuery session using the resources of this session manager
     */
    public XQuerySession createSession()
    {
        return new XQuerySessionImpl(this);
    }

    /**
     * For internal use.
     */
    public ModuleManager getModuleManager()
    {
        return moduleMan;
    }

    /**
     * Sets the maximum memory size for the document cache. The document cache
     * stores transient documents which are parsed in memory.
     * @param size maximum memory size in bytes. Decreasing this size will
     *        flush the cache accordingly.
     * @return the former maximum memory size in bytes
     */
    public long setTransientDocumentCacheSize(long size)
    {
        long oldSize = documentCache.getCacheSize();
        documentCache.setCacheSize(size);
        return oldSize;
    }

    /**
     * Gets the current maximum memory size for the document cache.
     * @return a size in bytes
     */
    public long getTransientDocumentCacheSize()
    {
        return documentCache.getCacheSize();
    }

    /**
     * For internal use.
     */
    public DocumentPool getDocumentCache()
    {
        return documentCache;
    }

    /**
     * For internal use.
     */
    public void setDocumentCache(DocumentPool documentCache)
    {
        this.documentCache = documentCache;
    }

    /**
     * Defines a resolver of XQuery modules.
     * @param resolver a module resolver
     */
    public void setModuleResolver(ModuleResolver resolver)
    {
        Check.nonNull(resolver, "resolver");
        moduleMan.setResolver(resolver);
    }
    
    /**
     * Returns the current Resolver of XQuery modules.
     * @return the current module resolver
     */
    public ModuleResolver getModuleResolver()
    {
        return moduleMan.getResolver();
    }
}
