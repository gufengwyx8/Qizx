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

import com.qizx.api.DataModelException;
import com.qizx.api.Node;
import com.qizx.util.basic.FileUtil;

import org.xml.sax.InputSource;

import java.io.File;
import java.util.ArrayList;

/**
 * Provides access to the data-model of XML documents.
 * <p>
 * Expects resolved document URI/path.
 * <p>
 * Parsed documents are managed in a cache with configurable size (see
 * {@link #setCacheSize}).
 */
public class DocumentPool extends DocumentParser
{
    private static final int MIN_CACHE_SIZE = 128 * 1024;
    private static final String CACHE_SIZE_PROP =
        "com.qizx.docpool.maxsize";
    // default size in Mb, configurable by system prop:
    private long cacheSize = 12 * 1048576L;
    protected ArrayList<FONIDataModel> cache = new ArrayList<FONIDataModel>();
    private ArrayList timeCache = new ArrayList(); // parse time for files

    /**
     * Creates an empty document pool.
     */
    public DocumentPool()
    {
        initSize();
    }

    public long getCacheSize()
    {
        return cacheSize;
    }

    /**
     * Defines the maximal memory size of the document cache. This size is
     * otherwise defined by the system property
     * "com.qizx.docman.cachesize".
     * 
     * @param size in bytes (hint).
     */
    public synchronized void setCacheSize(long size)
    {
        cacheSize = Math.max(size, MIN_CACHE_SIZE);
    }

    private void initSize()
    {
        String sysp = System.getProperty(CACHE_SIZE_PROP);
        if (sysp != null) {
            try {
                setCacheSize(Integer.parseInt(sysp));
            }
            catch (Exception ignored) { // ignored
            }
        }
    }
    
    public synchronized void clearCache()
    {
        cache = new ArrayList();
        timeCache = new ArrayList();
    }
    
    /**
     * Cached access by URI.
     * 
     * @param uri an absolute uri (no more resolving) WITH PROTOCOL
     * @return the root node (or document node) of the document.
     */
    public Node findDocumentNode(String uri)
        throws DataModelException
    {
        FONIDataModel doc = findDocument(uri);
        return doc.getDocumentNode();
    }
    
    /**
     * Cached access by URI.
     * @param uri now a resolved uri with protocol, correctly escaped
     * @throws DataModelException
     */
    public FONIDataModel findDocument(String uri)
        throws DataModelException
    {
        try {
            FONIDataModel dm = getCachedDocument(uri);
            if (dm != null)
                return dm;
            dm = new FONIDataModel(parseDocument(new InputSource(uri)));
            cacheDocument(dm, getFileDate(uri));
            return dm;
        }
        catch (org.xml.sax.SAXException sax) {
            throw new DataModelException("XML parsing error in " + uri
                                         + ": " + sax.getMessage(),
                                         sax.getException());
        }
        catch (Exception e) {
            //e.printStackTrace();
            throw new DataModelException("Document access error in " + uri
                                         + ": " + e.getMessage(), e);
        }

        // 	long T0 = System.currentTimeMillis();
        // 	System.err.println("load, start encoding");
        // 	CoreByteOutput out = new CoreByteOutput();
        // 	new TreeEncoder().encode(new FONIDataModel(dm).documentNode(), out);
        // 	out.close();
        // 	long T1 = System.currentTimeMillis();
        // 	System.err.println("ENCODED in "+(T1-T0)+" ms");
        // 	XMLSerializer handler = new XMLSerializer();
        // 	new TreeDecoder(new CoreByteInput(out)).decode(handler);
        // 	long T2 = System.currentTimeMillis();
        // 	System.err.println("DECODED in "+(T2-T1)+" ms");
    }
    
    // returns null if not found OR stale
    protected synchronized FONIDataModel getCachedDocument(String uri)
    {
        // linear search: never mind!
        for (int d = 0; d < cache.size(); d++) {
            FONIDataModel doc = (FONIDataModel) cache.get(d);

            try {
                String baseURI = doc.getDom().getBaseURI();
                if (uri.equals(baseURI)) {
                    cache.remove(d);
                    Long readTime = (Long) timeCache.remove(d);
                    Long fileDate = getFileDate(baseURI);
                    if (fileDate != null && readTime != null
                        && fileDate.longValue() > readTime.longValue()) {
                        return null; // already removed
                    }
                    // put at head:
                    cache.add(0, doc);
                    timeCache.add(0, readTime);
                    return doc;
                }
            }
            catch (DataModelException shouldNotHappen) {
                shouldNotHappen.printStackTrace();
            }
        }
        return null;
    }
    
    private Long getFileDate(String baseURI)
    {
        // if file, check the date:
        if (!baseURI.startsWith("file:"))
            return null;
        File file = FileUtil.urlToFile(baseURI);
        if (file == null) // malformed?
            return null;
        return new Long(file.lastModified());
    }

    protected synchronized void cacheDocument(FONIDataModel doc, Long fileDate)
        throws DataModelException   // should not happen
    {
        int pos = cache.indexOf(doc);
        if (pos > 0) {
            cache.remove(pos);
            timeCache.remove(pos);
        }
        cache.add(0, doc);
        timeCache.add(0, fileDate);
        int/*NId*/ cumulatedSize = doc.getDom().estimateMemorySize();

        for (int d = 1, D = cache.size(); d < D; d++) {
            FONIDataModel doc2 = (FONIDataModel) cache.get(d);

            int/*NId*/ size = doc2.getDom().estimateMemorySize();
            cumulatedSize += size;
                
            if (cumulatedSize > cacheSize) {
                for( ; d < cache.size(); d++)
                    cache.remove(d);
                cache.trimToSize();
                break;
            }
        }
        
    }
}
