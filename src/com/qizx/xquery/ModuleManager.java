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

import com.qizx.api.CompilationException;
import com.qizx.api.ModuleResolver;
import com.qizx.api.util.DefaultModuleResolver;
import com.qizx.util.basic.FileUtil;
import com.qizx.xquery.impl.NewParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

/**
 * Helper class used by a XQuery engine to compile and cache XQuery modules and
 * XSLT stylesheets.
 * <p>
 * Ensures that a module is loaded only once for the same query (i.e. two
 * references to a module from the same query must yield the same object).
 * Modules can be shared by several Connections and are thread safe.
 */
public class ModuleManager
    implements ErrorListener
{
    public static final String XSLT_OUTPUT_FILE = "output-file";

    private ModuleResolver resolver;
    private HashMap modules = new HashMap();

    private URL xslBaseURL;
    private TransformerFactory xsltFactory;
    private ArrayList templateCache = new ArrayList();
    private int templateCacheSize = 3; // TODO: config

    private StringBuffer errorMessages = new StringBuffer();

    /**
     * Builds a Module Manager with a module URI resolver.
     * <p>The XSLT templates are by default resolved relatively to the current
     * directory.
     */
    public ModuleManager(ModuleResolver resolver)
    {
        xslBaseURL = FileUtil.fileToURL("./");
        setResolver(resolver);
    }

    public ModuleResolver getResolver()
    {
        return resolver;
    }

    public void setResolver(ModuleResolver resolver)
    {
        this.resolver = resolver;
        if(resolver instanceof DefaultModuleResolver) {
            DefaultModuleResolver dmr = (DefaultModuleResolver) resolver;
            xslBaseURL = dmr.getBase();
        }
    }

    /**
     * Unloads all modules. Main Queries can still refer safely to the modules.
     * <p>
     * Note: Due to possible dependencies between modules, it would be very
     * difficult to unload a module selectively.
     */
    public synchronized void unloadAllModules()
    {
        modules = new HashMap();
    }

    public synchronized boolean alreadyLoadedModule(String uri)
    {
        return modules.get(uri) != null;
    }

    // Loads a module part for a given module URI with an optional location hint
    public synchronized 
        ModuleContext loadModule(ModuleContext initialContext,
                                 URL physicalUrl)
        throws IOException, CompilationException
    {
        // Use the resolved url to access the cache:
        ModuleContext mo = (ModuleContext) modules.get(physicalUrl);
        if (mo == null) {
            // actual loading:  // TODO encoding is platform default 
            String text = FileUtil.loadString(physicalUrl);
            NewParser p = new NewParser(this);
            // create the module and store it before parsing to avoid looping
            // on
            // cyclical module references:
            modules.put(physicalUrl, mo = new ModuleContext(initialContext));
            mo.setMessageTarget(initialContext.getMessageTarget());
            p.parseModule(mo, text, physicalUrl.toString());
        }
        return mo;
    }


    // ---------------- XSLT -------------------------------------------------

    static class CacheSlot
    {
        String path;
        Object loaded;
        
        long timeStamp;
    }

    /**
     * Sets the TransformerFactory used for XSLT transformations.
     */
    public void setXSLTFactory(TransformerFactory value)
    {
        xsltFactory = value;
    }

    /**
     * Gets the TransformerFactory used for XSLT transformations. 
     * Useful for configuring the default factory.
     */
    public TransformerFactory getXSLTFactory()
    {
        if (xsltFactory == null) {
            try {
                Class saxon = Class.forName("net.sf.saxon.TransformerFactoryImpl");
                xsltFactory = (TransformerFactory) saxon.newInstance();
            }
            catch (Exception e) {
                
                ; 
            }
            if (xsltFactory == null) // fallback
                xsltFactory = TransformerFactory.newInstance();
        }
        return xsltFactory;
    }
    
    public Templates loadTemplates(String path)
        throws TransformerException
    {
        synchronized (templateCache) {
            try {
                errorMessages.setLength(0);
                return doLoadTemplates(path);
            }
            catch(TransformerException e)
            {
                throw new TransformerException(e.getMessage() + errorMessages);
            }
        }
    }
    
    private Templates doLoadTemplates(String path)
        throws TransformerException
    {
        long now = System.currentTimeMillis();
        
        // simple implementation: block on compilation
        CacheSlot e = null;
        int c = templateCache.size();
        for (; --c >= 0;)
            if ((e = (CacheSlot) templateCache.get(c)) != null
                    && e.path.equals(path))
                break;
        if (c >= 0) {
            return (Templates) e.loaded;
        }
        
        // load or reload:
        e = new CacheSlot();
        e.path = path;
        getXSLTFactory();
        try {
            Source source = null;
            xsltFactory.setErrorListener(this);
            if (xsltFactory.getURIResolver() != null)
                source = xsltFactory.getURIResolver().resolve(path, xslBaseURL.toString());
            else
                source = new StreamSource(new URL(xslBaseURL, path).toString());
            if (source == null)
                throw new TransformerException("cannot find stylesheet "
                                               + path);
            Templates templ = xsltFactory.newTemplates(source);
            e.loaded = templ;
        }
        catch (TransformerConfigurationException ex) {
            throw new TransformerException(ex.getMessage(), ex.getCause());
        }
        catch (MalformedURLException ex) {
            throw new TransformerException(ex.getMessage(), ex.getCause());
        }
        templateCache.add(0, e);
        if (templateCache.size() > templateCacheSize)
            templateCache.remove(templateCache.size() - 1);
        
        return (Templates) e.loaded;
    }

    public void error(TransformerException exception)
        throws TransformerException
    {
        errorMessages .append("\n" + exception.getMessage());
    }

    public void warning(TransformerException exception)
    {
        errorMessages .append("\n" + exception.getMessage());
    }

    public void fatalError(TransformerException exception)
        throws TransformerException
    {
        throw exception;
    }

    public URL getXslBaseURL()
    {
        return xslBaseURL;
    }

    public void setXslBaseURL(URL xslBaseURL)
    {
        this.xslBaseURL = xslBaseURL;
    }
}
