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

import com.qizx.api.ModuleResolver;
import com.qizx.util.basic.Check;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Default ModuleResolver implementation using a base URL.
 * <p>
 * The strategy used consists of appending the module namespace URI to the base
 * URI, and (if possible) checking the existence of the pointed resource.
 * <p>
 * If the check fails, and if location hints are provided, then each hint is
 * appended to the base URL and put to the result list, unless it can be
 * verified that the URL formed this way points to a non-existent resource.
 */
public class DefaultModuleResolver
    implements ModuleResolver
{
    private URL base;

    /**
     * Creates a resolver using a base URL. 
     * @param base an URL used as a base for resolving module URI's. 
     */
    public DefaultModuleResolver(URL base)
    {
        Check.nonNull(base, "base URL");
        this.base = base;
        // due to weird behavior of URL class, need a trailing slash:
        if(base != null && !base.getPath().endsWith("/")) {
            try {
                this.base = new URL(base.getProtocol(), base.getHost(),
                                    base.getPath() + "/");
            }
            catch (MalformedURLException e) { // what to do ?
                e.printStackTrace();
            }
        }
    }
    
    /** @see com.qizx.api.ModuleResolver#resolve
     */
    public URL[] resolve(String moduleNamespaceURI, String[] locationHints)
        throws MalformedURLException 
    {
        URL attempt = new URL(base, moduleNamespaceURI);

        if(existingFile(attempt))
            return new URL[] { attempt };
        // try hints:
        if(locationHints == null || locationHints.length == 0)
            return new URL[0]; // failure
        int hintCnt = locationHints.length;
        ArrayList<URL> result = new ArrayList<URL>(hintCnt);
        for (int i = 0; i < locationHints.length; i++) {
            URL url = new URL(base, locationHints[i]);
            
            if(existingFile(url))
                result.add(url);
        }
        return result.toArray(new URL[result.size()]);
    }
    
    public URL getBase()
    {
        return base;
    }

    // returns true if 'file:' and exists as a plain file
    private boolean existingFile(URL url)
    {
        if(!"file".equals(url.getProtocol()))
            return false;
        File file = new File(url.getPath());
        return file.isFile();
    }
}
