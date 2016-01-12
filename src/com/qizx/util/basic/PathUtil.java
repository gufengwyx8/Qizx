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
package com.qizx.util.basic;

public class PathUtil
{
    /**
     *  Normalizes a path. A backslash is converted to a slash.
     *  If 'absolute', return a path with a leading slash.
     */
    public static String normalizePath(String path, boolean absolute)
    {
        if(path == null)
            return null;
        int L = path.length(), save = 1;
        // remove protocol if any:
        int start = path.indexOf(':') + 1;
        
        char[] cpath = new char[L + 1];
        cpath[0] = '/'; 
        for(int i = start; i < L; i++) {
            char cc = path.charAt(i);
            if(cc == '/' || cc == '\\') {
                if(cpath[save-1] == '/')
                    continue;   // duplicate separator
                save = Util.processComponent(cpath, save);
                cpath[save++] = '/';
            }
            else cpath[save++] = cc;
        }
        save = Util.processComponent(cpath, save);  // final component
        if(save > 2 && cpath[save-1] == '/')
            -- save;
        if(save == 0)
            return absolute? "/" : ".";
        start = 1;
        if(absolute || path.charAt(0) == '/' || path.charAt(0) == '\\')
            start = 0;
        return new String(cpath, start, save - start);
    }

    public static String makePath(String path, String childName)
    {
        if(path == null || "/".equals(path))
            return "/" + childName;
        return path + "/" + childName;
    }

    /**
     * Returns the path which "contains" a path.
     * @param path normalized path, or null for the root "/"
     */
    public static String getParentPath(String path)
    {
        int last = path.lastIndexOf('/');
        if(last < 0)
            return ".";
        if(last == 0)
            return (path.length() == 1) ? null : "/";
        return path.substring(0, last);
    }

    /**
     * Returns the last element of a path.
     */
    public static String getBaseName(String path)
    {
        int last = path.lastIndexOf('/');
        if(last < 0 || "/".equals(path))
            return path;
        return path.substring(last + 1);
    }
    
    // Returns true if container is a parent or ancestor of contained
    public static boolean contains(String container, boolean strictParent,
                                   String contained)
    {
        if (container == null)
            return true;
        if (!contained.startsWith(container))
            return false;
        // check slash
        int cnerLen = container.length();
        int slashPos = cnerLen;
        if (cnerLen > 0 && container.charAt(cnerLen - 1) == '/')
            --slashPos;
        if (contained.length() == cnerLen || contained.charAt(slashPos) != '/')
            return false;
        // closure?
        if (!strictParent)
            return true;
        // directly contained: no path separator in remainder
        return contained.indexOf('/', cnerLen + 1) < 0;
    }
    
    /**
     * Returns the path of specified URL path relative to specified base URL
     * path.
     * @see #relativize(java.net.URL, java.net.URL)
     */
    public static String relativize(String path, String basePath)
    {
        // Treat "/" as a special case.
        if (getParentPath(path) == null)
            return path;

//        basePath = getParentPath(basePath);
//        if (basePath == null)
//            return path;

        StringBuffer buffer = new StringBuffer();

        while (basePath != null) {
            String start = basePath;
            if (!start.endsWith("/"))
                start += '/';

            if (path.startsWith(start)) {
                buffer.append(path.substring(start.length()));
                break;
            }

            buffer.append("../");
            basePath = getParentPath(basePath);
        }

        return buffer.toString();
    }

    /**
     * Returns the path of the common ancestor of the two normalized paths.
     * @return path of the common ancestor without trailing slash, except for "/"
     */
    public static String commonAncestor(String p1, String p2)
    {
        int max = Math.min(p1.length(), p2.length());
        int lastSlash = 0, p = 0;
        for( ; p < max; p++) {
            char c1 = p1.charAt(p), c2 = p2.charAt(p);
            if(c1 != c2)
                break;
            if(c1 == '/')
                lastSlash = p;
        }
        if(p == max && (p < p1.length() && p1.charAt(p) == '/' ||
                        p < p2.length() && p2.charAt(p) == '/'))
            lastSlash = p;
        if(lastSlash == 0)
            return "/";
        return p1.substring(0, lastSlash); // or p2...
    }

    public static String rename(String path, String dstParentPath, 
                                String srcParentPath)
    {
        if(srcParentPath == null)
            srcParentPath = getParentPath(path);
        // slash comes from srcParentPath
        return dstParentPath + path.substring(srcParentPath.length());
    }
}
