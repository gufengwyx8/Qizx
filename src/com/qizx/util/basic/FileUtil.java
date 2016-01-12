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
/*
 * Copyright (c) 1998-2003 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of the Pixware Java utilities.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.qizx.util.basic;

import com.qizx.util.LikePattern;
import com.qizx.util.ProgressHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * A collection of utility functions (static methods) operating on files and
 * directories.
 */
public final class FileUtil
{
    // System property to disable file synchronization
    private static String syncProp = System.getProperty("use.file.sync");
    public static boolean useSync = "true".equalsIgnoreCase(syncProp);

    /**
     * Converts a <tt>file:</tt> URL to a File.
     * @param url the URL to be converted
     * @return the result of the conversion or <code>null</code> if
     *         <code>url</code> is not a <tt>file:</tt> URL
     */
    public static File urlToFile(URL url) {
        if (!url.getProtocol().equals("file"))
            return null;
        
        String path = url.getPath();
        
        if (File.separatorChar != '/')
            path = path.replace('/', File.separatorChar);
        
        return new File(path);
    }
    
    /**
     * Converts a <tt>file:</tt> URL name to a File.
     * 
     * @param urlName the URL name to be converted
     * @return the result of the conversion or <code>null</code> if
     * <code>urlName</code> is not a <tt>file:</tt> URL
     */
    public static File urlToFile(String urlName) {
        URL url;
        try {
            url = new URL(urlName);
        } catch (MalformedURLException ignored) {
            url = null;
        }
        
        return (url == null)? null : urlToFile(url);
    }
    
    /**
     * Converts a <tt>file:</tt> URL to a file name.
     * 
     * @param url the URL to be converted
     * @return the result of the conversion or <code>null</code> if
     * <code>url</code> is not a <tt>file:</tt> URL
     */
    public static String urlToFileName(URL url) {
        File file = urlToFile(url);
        return (file == null)? null : file.getPath();
    }
    
    /**
     * Converts a <tt>file:</tt> URL name to a file name.
     * 
     * @param urlName the URL name to be converted
     * @return the result of the conversion or <code>null</code> if
     * <code>urlName</code> is not a <tt>file:</tt> URL
     */
    public static String urlToFileName(String urlName) {
        File file = urlToFile(urlName);
        return (file == null)? null : file.getPath();
    }
    
    /**
     * Converts a File to a <tt>file:</tt> URL.
     * 
     * @param file the file to be converted
     * @return the result of the conversion
     */
    public static URL fileToURL(File file)
    {
        try {
            file = file.getCanonicalFile();
        }
        catch (IOException ignored) {
            file = file.getAbsoluteFile();
        }

        URL url;
        try {   // avoid going through File
            String path = file.getPath().replace('\\', '/');
            url = new URL("file", "", path);
        }
        catch (IOException ignored) {
            url = null;
        }

        return url;
    }

    /**
     * Like fileToUrl but ensures that there is a trailing slash
     */
    public static URL dirToURL(File file)
    {
        try {
            file = file.getCanonicalFile();
        }
        catch (IOException ignored) {
            file = file.getAbsoluteFile();
        }

        URL url;
        try {   // avoid going through File
            String path = file.getPath();
            if(!path.endsWith(File.separator))
                path = path + File.separator;
            url = new URL("file", "", path.replace('\\', '/'));
        }
        catch (IOException ignored) {
            url = null;
        }

        return url;
    }


    /**
     * Converts a File to a System Id suitable for SAX 
     */
    public static String fileToSystemId(File file) throws IOException
    {
        // File.toURI() is dead bugged
        file = file.getCanonicalFile();
        String path = file.getAbsolutePath();
        if(File.separatorChar == '\\') { // loosedose
            path = path.replace('\\', '/');
            if(path.length() > 2 && path.charAt(1) == ':') // windows drive
                return "file:///" + path.substring(0, 2)
                       + Util.escapeURI(path.substring(2));
        }
        return "file://" + Util.escapeURI(path);
    }

    /**
     * Converts an URL to a System Id suitable for SAX
     * fixes unescaped characters
     */
    public static String urlToSystemId(URL url) throws IOException
    {
        return Util.escapeURI(url.getPath());
    }

    /**
     * Converts a file name to a <tt>file:</tt> URL.
     * 
     * @param fileName the file name to be converted
     * @return the result of the conversion
     */
    public static URL fileToURL(String fileName) {
        return fileToURL(new File(fileName));
    }
    
    

    public static String resolve(String baseURI, String uri)
    {
        if(baseURI == null)
            return uri;
        try {
            URI base = new URI(baseURI);
            return base.resolve(uri).toString();
        }
        catch (URISyntaxException e) {
            return uri;
        }
    }

    /**
     * Converts an unknown URI string to an URI.
     * Returns null if invalid URI.
     */
    public static URI uriConvert(String uri) {
        try {
            return new URI(uri);
        }
        catch (URISyntaxException e) {
            return null;
        }
    }
    
    /**
     * Converts an unknown URI to an URL.
     * 
     * @param uri to be converted
     * @return the result of the conversion
     */
    public static URL uriToURL(String uri)
    {
        try {
            return new URL(uri);
        }
        catch (MalformedURLException ignored) {
            try {
                return new URL("file", "", uri);
            }
            catch (MalformedURLException e) {
                return null;
            }
        }
    }
    
    /**
     * Converts a String to an URL or returns null.
     * 
     * @param uri to be converted
     * @return the result of the conversion
     */
    public static URL toURL(String uri) {
        try {
            return new URL(uri);
        } catch (MalformedURLException ignored) {
            return null;
        }
    }
    
    /**
     * Converts a File to a <tt>file:</tt> URL name.
     * 
     * @param file the file to be converted
     * @return the result of the conversion
     */
    public static String fileToURLName(File file) {
        URL url = fileToURL(file);
        return (url == null)? null : url.toExternalForm();
    }
    
    /**
     * Converts a file name to a <tt>file:</tt> URL name.
     * 
     * @param fileName the file name to be converted
     * @return the result of the conversion
     */
    public static String fileToURLName(String fileName) {
        URL url = fileToURL(fileName);
        return (url == null)? null : url.toExternalForm();
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Resolves a relative/absolute path more intelligently 
     *  than new File(parent, path)
     * @param parentDirectory
     * @param path
     * @throws URISyntaxException
     */
    public static File resolve(File parentDirectory, String path)
        throws URISyntaxException
    {
        File file = new File(path);
        if(file.isAbsolute())
            return file;
        return new File(parentDirectory, path);
    }


    /**
     * Returns the directory part in a file path name. The directory part is
     * everything before the last file path name separator (that is,
     * <tt>'\'</tt> for Windows).
     * <p>On Windows, <tt>'/'</tt> is used as an alternate file path name
     * separator.
     * <p>Examples:
     * <table border="1">
     * <tr>
     * <th bgcolor="#C0C0C0" align="center">Path
     * <th bgcolor="#C0C0C0" align="center">Result
     * <tr>
     * <td>util/FileUtil.java
     * <td>util
     * <tr>
     * <td>FileUtil.java
     * <td>. (dot)
     * </table>
     * 
     * @param fileName a file path name
     * @return the directory part
     */
    public static String fileDirName(String fileName) {
        char separ = File.separatorChar;
        int slash = fileName.lastIndexOf(separ);
        if (slash < 0 && separ == '\\') {
            separ = '/';
            // On Windows, '/' is an alternate fileName separator.
            slash = fileName.lastIndexOf(separ);
        }
        
        String name;
        if (slash < 0) {
            name = ".";
        } else if (slash == 0) {
            name = File.separator;
        } else {
            name = fileName.substring(0, slash);
            if (separ != File.separatorChar) 
                name = name.replace(separ, File.separatorChar);
        }
        return name;
    }
    
    /**
     * Returns the base name part in a file path name. The base name part is
     * everything after the last file name separator (that is <tt>'\'</tt> for
     * Windows).
     * <p>On Windows, <tt>'/'</tt> is used as an alternate file path name
     * separator in addition to <tt>'\'</tt>.
     * <table border="1">
     * <tr>
     * <th bgcolor="#C0C0C0" align="center">Path
     * <th bgcolor="#C0C0C0" align="center">Result
     * <tr>
     * <td>util/FileUtil.java
     * <td>FileUtil.java
     * <tr>
     * <td>FileUtil.java
     * <td>FileUtil.java
     * </table>
     * 
     * @param fileName a file path name
     * @return the base name part
     */
    public static String fileBaseName(String fileName) {
        int slash = fileName.lastIndexOf(File.separatorChar);
        if (slash < 0 && File.separatorChar == '\\')
            // On Windows, '/' is an alternate fileName separator.
            slash = fileName.lastIndexOf('/');
        
        if (slash < 0)
            return fileName;
        else if (slash == fileName.length()-1)
            return "";
        else
            return fileName.substring(slash+1);
    }
    
    /**
     * Returns the file extension part in a file path name. The file extension
     * part is everything after the last <tt>'.'</tt>.
     * <p>On Windows, <tt>'/'</tt> is used as an alternate file path name
     * separator in addition to <tt>'\'</tt>.
     * <table border="1">
     * <tr>
     * <th bgcolor="#C0C0C0" align="center">Path
     * <th bgcolor="#C0C0C0" align="center">Result
     * <tr>
     * <td>util/FileUtil.java
     * <td>.java
     * <tr>
     * <td>makefile
     * <td>"" (empty string)
     * <tr>
     * <td>/home/hussein/.profile
     * <td>"" (empty string)
     * </table>
     * 
     * @param fileName a file path name
     * @return the file extension part; does not include the <tt>'.'</tt>
     * <p>If the base name without its extension is empty, the path is
     * considered not to have an extension part. This is the case of
     * <tt>/home/hussein/.profile</tt> in the examples above.
     */
    public static String fileExtension(String fileName)
    {
        int slash = fileName.lastIndexOf(File.separatorChar);
        if (slash < 0 && File.separatorChar == '\\')
            // On Windows, '/' is an alternate fileName separator.
            slash = fileName.lastIndexOf('/');
        if (slash < 0) 
            slash = 0;
        else 
            ++slash;
        
        int dot = fileName.lastIndexOf('.');
        if (dot <= slash || dot == fileName.length()-1)
            // '.profile' and 'foo.' have no extensions.
            return "";
        else
            return fileName.substring(dot+1);
    }
    
    /**
     * Returns a file path name without its file extension part. The file
     * extension part is everything after the last dot.
     * <p>On Windows, <tt>'/'</tt> is used as an alternate file path name
     * separator.
     * <table border="1">
     * <tr>
     * <th bgcolor="#C0C0C0" align="center">Path
     * <th bgcolor="#C0C0C0" align="center">Result
     * <tr>
     * <td>util/FileUtil.java
     * <td>util/FileUtil
     * <tr>
     * <td>makefile
     * <td>makefile
     * <tr>
     * <td>/home/hussein/.profile
     * <td>/home/hussein/.profile
     * </table>
     * 
     * @param fileName a file path name
     * @return the file path without extension part if any
     */
    public static String trimFileExtension(String fileName)
    {
        int slash = fileName.lastIndexOf(File.separatorChar);
        if (slash < 0 && File.separatorChar == '\\')
            // On Windows, '/' is an alternate fileName separator.
            slash = fileName.lastIndexOf('/');
        if (slash < 0) 
            slash = 0;
        else 
            ++slash;
        
        int dot = fileName.lastIndexOf('.');
        if (dot <= slash)
            // '.profile' has no extension!
            return fileName;
        else
            return fileName.substring(0, dot);
    }

    /**
     * Tests that the path ends with extension (case insensitive).
     * Does not care about dot.
     */
    public static boolean fileHasExtension(String path, String fileExt)
    {
        int plen = path.length(), elen = fileExt.length();
        if(elen > plen)
            return false; 
        for(int i = elen; --i >= 0; )
            if(Character.toLowerCase(fileExt.charAt(i))
               != Character.toLowerCase(path.charAt(plen - elen + i)))
                return false;
        return true;
    }
    
    /**
     * Returns the path of specified file relative to specified base.
     * <p>
     * Example: returns <tt>../local/bin/html2ps</tt> for
     * <tt>/usr/local/bin/html2ps</tt> relative to <tt>/usr/bin/grep</tt>.
     * @see URLUtil#relativize
     */
    public static String relativize(File file, File base)
    {
        if(base == null)
            return file.getAbsolutePath();
        String relativePath =
            PathUtil.relativize(fileToURL(file).getPath(),
                                fileToURL(base).getPath());

        if (File.separatorChar != '/')
            return relativePath.replace('/', File.separatorChar);
        else
            return relativePath;
    }


    /**
     * Expands a simple path pattern containing a '*' or '&#63;' in the
     * FILENAME ONLY.
     */
    public static File[] expandPathPattern(File file)
    {
        LikePattern glob = new LikePattern(file.getName());
        File dir = file.getParentFile();
        if(dir == null)
            dir = new File(".");
        String[] files;
        if(dir == null || (files = dir.list()) == null)
            return new File[0];
        int sf = 0, nf = files.length;
        for(int f = 0; f < nf; f++) {
            if(glob.matches(files[f])) {
                files[sf++] = files[f];
            }
        }
        Arrays.sort(files, 0, sf);
        File[] result = new File[sf];
        for(int f = 0; f < sf; f++)
            result[f] = new File(dir, files[f]);
        return result;
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Deletes a file or an empty directory.
     * 
     * @param fileName the name of the file or empty directory to be deleted
     * @return <code>true</code> if the file or directory has been
     * successfully deleted; <code>false</code> otherwise
     */
    public static boolean removeFile(String fileName) {
        return removeFile(fileName, /*force*/ false);
    }
    
    /**
     * Deletes a file or a directory, possibly emptying the directory before
     * deleting it.
     * 
     * @param fileName the name of the file or directory to be deleted
     * @param force if <code>true</code> and the file to be deleted is a
     * non-empty directory, empty it before attempting to delete it; if
     * <code>false</code>, do not empty directories
     * @return <code>true</code> if the file or directory has been
     * successfully deleted; <code>false</code> otherwise
     */
    public static boolean removeFile(String fileName, boolean force) {
        return removeFile(new File(fileName), force);
    }
    
    /**
     * Deletes a file or a directory, possibly emptying the directory before
     * deleting it.
     * 
     * @param file the file or directory to be deleted
     * @param force if <code>true</code> and the file to be deleted is a
     * non-empty directory, empty it before attempting to delete it; if
     * <code>false</code>, do not empty directories
     * @return <code>true</code> if the file or directory has been
     * successfully deleted; <code>false</code> otherwise
     */
    public static boolean removeFile(File file, boolean force) {
        if (file.isDirectory() && force) 
            emptyDirectory(file);
        
        return file.delete();
    }
    
    /**
     * 
     */
    public static boolean isEmptyDirectory(File directory) {
        if(!directory.isDirectory())
            return false;
        String[] files = directory.list();
        return files == null || files.length == 0;
    }
    
    /**
     * Computes total size of a directory and its contents.
     */
    public static long directorySize(File dir)
    {
        File[] children = dir.listFiles();
        long total = 0;
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                File file = children[i];
                if (file.isDirectory())
                    total += directorySize(file);
                else
                    total += file.length();
            }
        }
        return total + dir.length();
    }

    /**
     * Recursively deletes all the entries of a directory.
     * 
     * @param dirName the name of the directory to be emptied
     * @return 
     */
    public static boolean emptyDirectory(String dirName) {
        return emptyDirectory(new File(dirName));
    }
    // hack for Java binding
    public static boolean emptyDir(String dirName) {
        return emptyDirectory(new File(dirName));
    }
    
    /**
     * Recursively deletes all the entries of a directory.
     * 
     * @param dir the directory to be emptied
     */
    public static boolean emptyDirectory(File dir)
    {
        String[] children = dir.list();
        boolean ok = true;
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                File child = new File(dir, children[i]);
                
                if (child.isDirectory()) {
                    if(!removeFile(child, /*force*/ true))
                        ok = false;
                }
                else if(!child.delete())
                    ok = false;
            }
        }
        return ok;
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Copy a file.
     * 
     * @param srcFileName the name of the source file
     * @param dstFileName the name of the destination file
     * @exception IOException if there is an IO problem
     */
    public static void copyFile(String srcFileName, String dstFileName)
        throws IOException
    {
        copyFile(new File(srcFileName), new File(dstFileName), null);
    }
    
    /**
     * Copy a file.
     * 
     * @param srcFile source file
     * @param dstFile destination file
     * @param progressHandler indicates how many bytes have been copied
     * @exception IOException if there is an IO problem
     */
    public static void copyFile(File srcFile, File dstFile, 
                                ProgressHandler progressHandler)
        throws IOException
    {
        FileOutputStream dst = new FileOutputStream(dstFile);
        FileInputStream src = new FileInputStream(srcFile);

        try {
            copy(src, dst, progressHandler);
        }
        finally {
            src.close();
            dst.close();
        }
    }

    public static void copy(InputStream src, OutputStream dst,
                            ProgressHandler progressHandler)
        throws IOException
    {
        byte[] bytes = new byte[4096];
        int count;
        final long SIZEMS = 1048576 * 4;
        long totalSize = 0, sizeMilestone = SIZEMS;

        while ((count = src.read(bytes)) != -1) {
            dst.write(bytes, 0, count);
            totalSize += count;

            if (progressHandler != null && totalSize >= sizeMilestone) {
                progressHandler.progressDone(totalSize);
                sizeMilestone += SIZEMS;
            }
        }
        dst.flush();
    }
    
    public static void copyDirectory(String inDirName, String outDirName)
        throws IOException
    {
        File inDir = nameToFile(inDirName);
        File outDir = nameToFile(outDirName);

        ensureDirExists(outDir);

        copyDirectory(inDir, outDir);
    }

    private static File nameToFile(String name)
        throws IOException
    {
        File file = null;

        if (name.startsWith("file:")) {
            try {
                file = new File(new URI(name));
            }
            catch (Exception ignored) {
            }
        }

        if (file == null)
            file = (new File(name)).getCanonicalFile();

        return file;
    }

    public static void ensureDirExists(String dirPath)
        throws IOException
    {
        ensureDirExists(new File(dirPath));
    }
    
    public static void ensureDirExists(File dir)
        throws IOException
    {
        if (dir.exists()) {
            if (!dir.isDirectory())
                throw new IOException("'" + dir
                                      + "' exists but is not a directory");
        }
        else {
            File parent = dir.getParentFile();
            if (parent != null)
                ensureDirExists(parent);

            if (!dir.mkdir())
                throw new IOException("cannot create directory '" + dir + "'");
        }
    }

    public static void copyDirectory(File inDir, File outDir)
        throws IOException
    {
        String[] baseNames = inDir.list();
        for (int i = 0; i < baseNames.length; ++i) {
            String baseName = baseNames[i];

            File inFile = new File(inDir, baseName);
            File outFile = new File(outDir, baseName);

            if (inFile.isDirectory()) {
                if (!outFile.mkdir())
                    throw new IOException("cannot create directory '"
                                          + outFile + "'");

                copyDirectory(inFile, outFile);
            }
            else {
                copyFile(inFile, outFile, null);
            }
        }
    }                                          

    // -----------------------------------------------------------------------
    
    /**
     * Loads the content of a text file. The encoding of the text is assumed
     * to be the native encoding of the platform.
     * 
     * @param fileName the name of the text file
     * @return the loaded String
     * @exception IOException if there is an IO problem
     */
    public static String loadString(String fileName) throws IOException {
        return loadString(new File(fileName));
    }
    
    /**
     * Loads the content of a text file. The encoding of the text is assumed
     * to be the native encoding of the platform.
     * 
     * @param file the text file
     * @return the loaded String
     * @exception IOException if there is an IO problem
     */
    public static String loadString(File file)
        throws IOException
    {
        return loadString(file, null);
    }
    
    public static String loadString(File file, String encoding)
        throws IOException
    {
        InputStream in = new FileInputStream(file);

        String loaded = null;
        try {
            loaded = loadString(in, encoding);
        }
        finally {
            in.close();
        }

        return loaded;
    }

    /**
     * Loads the content of an URL containing text.
     * 
     * @param url the URL of the text resource
     * @return the loaded String
     * @exception IOException if there is an IO problem
     */
    public static String loadString(URL url) throws IOException {
        return loadString(url, false);
    }
    
    /**
     * Same as {@link #loadString(java.net.URL)}, but the <tt>interactive</tt>
     * argument specifies whether the connection is interactive or not. For
     * example, an interactive HTTP connection may display a dialog box to let
     * the user specify his user name and his password.
     * <p>Note that in {@link #loadString(java.net.URL)}, the connection is
     * <em>not</em> interactive.
     */
    public static String loadString(URL url, boolean interactive) 
    throws IOException {
        URLConnection connection = url.openConnection();
        connection.setAllowUserInteraction(interactive);
        connection.setUseCaches(false);
        connection.setIfModifiedSince(0);
        
        String charsetName = null;
        String contentType = connection.getContentType();
        if (contentType != null)
            charsetName = contentTypeToCharsetName(contentType);
        
        InputStream in = connection.getInputStream();
        
        String loaded = null;
        try {
            loaded = loadString(in, charsetName);
        } finally {
            in.close();
        }
        
        return loaded;
    }
    
    /**
     * Parses a content type such as "<tt>text/html; charset=ISO-8859-1</tt>"
     * and returns the name of the IANA charset (that is, the name of the
     * encoding).
     * 
     * @param contentType the content type to be parsed
     * @return the name of the IANA charset if parsing was successful or
     * <code>null</code> otherwise.
     */
    public static String contentTypeToCharsetName(String contentType) {
        contentType = contentType.toLowerCase();
        String charsetName = null;
        
        int pos = contentType.indexOf("charset=");
        if (pos >= 0 && pos+8 < contentType.length()-1) {
            charsetName = contentType.substring(pos+8).trim();
            
            int length = charsetName.length();
            if (length >= 2 && charsetName.charAt(0) == '"')
                charsetName = charsetName.substring(1, length-1);
        }
        
        return charsetName;
    }
    
    /**
     * Loads the content of an InputStream returning text.
     * 
     * @param stream the text source
     * @param charsetName the IANA charset of the text source if known;
     * <code>null</code> may be used to specify the native encoding of the
     * platform
     * @return the loaded String
     * @exception IOException if there is an IO problem
     */
    public static String loadString(InputStream stream, String charsetName) 
    throws IOException {
        InputStreamReader in;
        if (charsetName == null)
            in = new InputStreamReader(stream);
        else
            in = new InputStreamReader(stream, charsetName);
        return loadString(in);
    }
    
    public static String loadString(Reader in)
    throws IOException {
        char[] chars = new char[8192];
        StringBuffer buffer = new StringBuffer(chars.length);
        int count;
        
        while ((count = in.read(chars, 0, chars.length)) != -1) {
            if (count > 0)
                buffer.append(chars, 0, count);
        }
        return buffer.toString();
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Saves some text to a file.
     * 
     * @param string the text to be saved
     * @param fileName the name of the destination file
     * @exception IOException if there is an IO problem
     */
    public static void saveString(String string, String fileName) 
    throws IOException {
        saveString(string, new File(fileName));
    }
    
    /**
     * Saves some text to a file.
     * 
     * @param string the text to be saved
     * @param file the destination file
     * @exception IOException if there is an IO problem
     */
    public static void saveString(String string, File file) 
    throws IOException {
        saveString(string, file, null);
    }
    
    /**
     * Saves some text to a file.
     * 
     * @param string the text to be saved
     * @param file the destination file
     * @param charsetName the IANA charset of the saved file;
     * <code>null</code> may be used to specify the native encoding of the
     * platform
     * @exception IOException if there is an IO problem
     */
    public static void saveString(String string, File file, 
                                  String charsetName) 
    throws IOException {
        OutputStream out = new FileOutputStream(file);
        
        try {
            saveString(string, out, charsetName);
        } finally {
            out.close();
        }
    }
    
    /**
     * Saves some text to an OutputStream.
     * 
     * @param string the text to be saved
     * @param stream the text sink
     * @param charsetName the IANA charset of the saved characters;
     * <code>null</code> may be used to specify the native encoding of the
     * platform
     * @exception IOException if there is an IO problem
     */
    private static void saveString(String string, OutputStream stream, 
                                   String charsetName) 
    throws IOException {
        OutputStreamWriter out;
        if (charsetName == null)
            out = new OutputStreamWriter(stream);
        else
            out = new OutputStreamWriter(stream, charsetName);
        
        out.write(string, 0, string.length());
        out.flush();
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Loads the content of a binary file.
     * 
     * @param fileName the name of the binary file
     * @return the loaded bytes
     * @exception IOException if there is an IO problem
     */
    public static byte[] loadBytes(String fileName) throws IOException {
        return loadBytes(new File(fileName));
    }
    
    /**
     * Loads the content of a binary file.
     * 
     * @param file the binary file
     * @return the loaded bytes
     * @exception IOException if there is an IO problem
     */
    public static byte[] loadBytes(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        
        byte[] loaded = null;
        try {
            loaded = loadBytes(in);
        } finally {
            in.close();
        }
        
        return loaded;
    }
    
    /**
     * Loads the content of an URL containing binary data.
     * 
     * @param url the URL of the binary data
     * @return the loaded bytes
     * @exception IOException if there is an IO problem
     */
    public static byte[] loadBytes(URL url) throws IOException {
        return loadBytes(url, false);
    }
    
    /**
     * Same as {@link #loadBytes(java.net.URL)}, but the <tt>interactive</tt>
     * argument specifies whether the connection is interactive or not. For
     * example, an interactive HTTP connection may display a dialog box to let
     * the user specify his user name and his password.
     * <p>Note that in {@link #loadBytes(java.net.URL)}, the connection is
     * <em>not</em> interactive.
     */
    public static byte[] loadBytes(URL url, boolean interactive) 
    throws IOException {
        URLConnection connection = url.openConnection();
        connection.setAllowUserInteraction(interactive);
        connection.setUseCaches(false);
        connection.setIfModifiedSince(0);
        
        InputStream in = connection.getInputStream();
        
        byte[] loaded = null;
        try {
            loaded = loadBytes(in);
        } finally {
            in.close();
        }
        
        return loaded;
    }
    
    /**
     * Loads the content of an InputStream returning binary data.
     * 
     * @param in the binary data source
     * @return the loaded bytes
     * @exception IOException if there is an IO problem
     */
    public static byte[] loadBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[8192];
        int count;
        
        while ((count = in.read(bytes)) >= 0)
            out.write(bytes, 0, count);
        
        return out.toByteArray();
    }
    
    // -----------------------------------------------------------------------
    
    /**
     * Tests if a file has been compressed using gzip.
     * 
     * @param fileName the name of the file to be tested
     * @return <code>true</code> if the file has been gzip-ed,
     * <code>false</code> otherwise
     * @exception IOException if there is an IO problem
     */
    public static boolean isGzipped(String fileName) throws IOException {
        return isGzipped(new File(fileName));
    }
    
    /**
     * Tests if a file has been compressed using gzip.
     * 
     * @param file the file to be tested
     * @return <code>true</code> if the file has been gzip-ed,
     * <code>false</code> otherwise
     * @exception IOException if there is an IO problem
     */
    public static boolean isGzipped(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        int magic1 = in.read();
        int magic2 = in.read();
        in.close();
        return (magic1 == 0037 && magic2 == 0213);
    }
    
    /**
     * Loads the content of a text file compressed using gzip.
     * 
     * @param fileName the name of the gzip-ed file; the encoding of the text
     * before compression is assumed to be the default encoding of the
     * platform
     * @return the loaded String
     * @exception IOException if there is an IO problem
     * @see #defaultEncoding
     */
    public static String loadGzippedString(String fileName) 
    throws IOException {
        return loadGzippedString(new File(fileName));
    }
    
    /**
     * Loads the content of a text file compressed using gzip.
     * 
     * @param file the gzip-ed file; the encoding of the text before
     * compression is assumed to be the default encoding of the platform
     * @return the loaded String
     * @exception IOException if there is an IO problem
     * @see #defaultEncoding
     */
    public static String loadGzippedString(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        
        String loaded = null;
        try {
            loaded = loadGzippedString(in, null);
        } finally {
            in.close();
        }
        
        return loaded;
    }
    
    /**
     * Loads the content of an URL containing text compressed using gzip.
     * 
     * @param url the URL of the gzip-ed data; the encoding of the text before
     * compression is assumed to be the default encoding of the platform
     * @return the loaded String
     * @exception IOException if there is an IO problem
     * @see #defaultEncoding
     */
    public static String loadGzippedString(URL url) throws IOException {
        return loadGzippedString(url, false);
    }
    
    /**
     * Same as {@link #loadGzippedString(java.net.URL)}, but the
     * <tt>interactive</tt> argument specifies whether the connection is
     * interactive or not. For example, an interactive HTTP connection may
     * display a dialog box to let the user specify his user name and his
     * password.
     * <p>Note that in {@link #loadGzippedString(java.net.URL)}, the
     * connection is <em>not</em> interactive.
     */
    public static String loadGzippedString(URL url, boolean interactive) 
    throws IOException {
        URLConnection connection = url.openConnection();
        connection.setAllowUserInteraction(interactive);
        connection.setUseCaches(false);
        connection.setIfModifiedSince(0);
        
        InputStream in = connection.getInputStream();
        
        String loaded = null;
        try {
            loaded = loadGzippedString(in, null);
        } finally {
            in.close();
        }
        
        return loaded;
    }
    
    public static Properties loadProperties(InputStream input) throws IOException
    {
        Properties props = new Properties();
        props.load(input);
        return props;
    }
    
    public static Properties loadProperties(File location) throws IOException
    {
        FileInputStream input = new FileInputStream(location);
        try {
            return loadProperties(input);
        }
        finally {
            input.close();
        }
    }
    
    /**
     * Loads the content of an InputStream returning text compressed using
     * gzip.
     * 
     * @param source the gzip-ed data source
     * @param encoding the encoding of the text before compression
     * @return the loaded String
     * @exception IOException if there is an IO problem
     */
    private static String loadGzippedString(InputStream source, 
                                            String encoding) 
    throws IOException {
        if (encoding == null)
            encoding = defaultEncoding();
        
        Reader in = new InputStreamReader(new GZIPInputStream(source), 
                                          encoding);
        char[] chars = new char[8192];
        StringBuffer buffer = new StringBuffer(chars.length);
        int count;
        
        while ((count = in.read(chars, 0, chars.length)) != -1) {
            if (count > 0)
                buffer.append(chars, 0, count);
        }
        
        return buffer.toString();
    }
    
    // -----------------------------------------------------------------------
    
    private static String platformDefaultEncoding;
    static {
        platformDefaultEncoding = 
            (new OutputStreamWriter(System.out)).getEncoding();
    }
    
    /**
     * Returns the default character encoding for this platform.
     * 
     * @return default character encoding for this platform
     */
    public static String defaultEncoding() {
        return platformDefaultEncoding;
    }
    
    // -----------------------------------------------------------------------
    // for debugging or for XQ scripts
    
    public static void consoleWrite(String message) {
        System.out.println(message);
        System.out.flush();
    }
    
    public static String consoleRead(String prompt) {
        try {
            System.out.print(prompt);
            System.out.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));
            return rd.readLine();
        }
        catch (Exception e) { return null; }
    }
}
