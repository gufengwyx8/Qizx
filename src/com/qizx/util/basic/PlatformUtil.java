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
 * Copyright (c) 1999-2003 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of the Pixware Java utilities.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.qizx.util.basic;

import java.io.*;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * A collection of utility functions (static methods) returning information
 * which is platform dependent.
 */
public final class PlatformUtil
{
    public static final int GENERIC_UNIX = 1;
    public static final int WINDOWS      = 2;
    public static final int MAC_OS       = 3;
    
    private static final int platform() {
        if (File.pathSeparatorChar == ';') {
            return WINDOWS;
        } else {
            String os = System.getProperty("os.name");
            if (os != null && os.toLowerCase().indexOf("mac") >= 0)
                return MAC_OS;
            else 
                return GENERIC_UNIX;
        }
    }
    
    public static final int PLATFORM = platform();
    
    public static final boolean IS_GENERIC_UNIX = (PLATFORM == GENERIC_UNIX);
    public static final boolean IS_WINDOWS      = (PLATFORM == WINDOWS);
    public static final boolean IS_MAC_OS       = (PLATFORM == MAC_OS);
    
    // -----------------------------------------------------------------------
    
    /**
     * Returns the name of the home directory of current user. The system
     * property <tt>HOME</tt>, if set to an existing directory name, may be
     * used to force the value returned by this function.
     * 
     * @return the name of the home directory of current user or
     * <code>null</code> if the candidate directory found by this function
     * does not exist or is not a directory.
     */
    public static String homeDir() {
        String homeDir = System.getProperty("HOME");
        if (homeDir == null || homeDir.length() == 0) 
            homeDir = System.getProperty("user.home");
        
        if (!(new File(homeDir)).isDirectory())
            homeDir = null;
        
        return homeDir;
    }
    
    /**
     * Returns the name of the user preference file associated to the
     * specified application.
     * <p>Note that <b>RC</b> means <b>R</b>untime <b>C</b>onfiguration.
     * 
     * @param appName the application name
     * @return the name of the user preference file or <code>null</code> if
     * the user <tt>HOME</tt> directory is unknown
     * @see #homeDir
     */
    public static String rcFileName(String appName) {
        String homeDir = homeDir();
        if (homeDir == null)
            return null;
        
        String rcFileName;
        if (IS_WINDOWS)
            rcFileName = homeDir + File.separatorChar + appName + ".ini";
        else
            rcFileName = homeDir + File.separatorChar + "." + appName;
        
        return rcFileName;
    }
    
    /**
     * Returns the name of a temporary file ending with extension
     * <tt>".tmp"</tt>. The directory of this file is the directory returned
     * by {@link #tmpDir}.
     * 
     * @return the name of a temporary file
     */
    public static String tmpFileName() {
        return tmpFileName(".tmp");
    }
    
    /**
     * Returns the name of a temporary file ending with the specified
     * extension. The directory of this file is the directory returned by
     * {@link #tmpDir}.
     * 
     * @param extension the desired extension for the file name; if a dot is
     * required, add it at the start of <code>extension</code>
     * @return the name of a temporary file
     */
    public static String tmpFileName(String extension) {
        String baseName = 
            Long.toString(System.currentTimeMillis(), Character.MAX_RADIX) + 
            extension;
        
        String tmpFile;
        String tmpDir = tmpDir();
        if (tmpDir == null)
            tmpFile = baseName;
        else
            tmpFile = (new File(tmpDir, baseName)).getPath();
        
        return tmpFile;
    }
    
    /**
     * Returns the name of directory where temporary files can be created
     * safely.
     * <p>Under Windows, the system property <tt>TMP</tt> or <tt>TEMP</tt>, if
     * set to an existing directory, may be used to force the value returned
     * by this function.
     * <p>Under Unix, this function always returns <tt>"/tmp"</tt>.
     * 
     * @return the name of the temporary directory or <code>null</code> if the
     * candidate directory found by this function does not exist or is not a
     * directory.
     */
    public static String tmpDir() {
        String tmpDir;
        if (IS_WINDOWS) {
            tmpDir = System.getProperty("TMP");
            if (tmpDir == null || tmpDir.length() == 0) { 
                tmpDir = System.getProperty("TEMP");
                if (tmpDir == null || tmpDir.length() == 0) 
                    tmpDir = "C:\\";
            }
        } else {
            tmpDir = "/tmp";
        }
        
        if (!(new File(tmpDir)).isDirectory())
            tmpDir = null;
        
        return tmpDir;
    }
    
    /**
     * Returns the command separator of the standard shell of the platform.
     * Example: it is <tt>';'</tt> for the bourne shell <tt>/bin/sh</tt> of
     * Unix.
     * 
     * @return the command separator of the standard shell of the platform
     */
    public static String commandSeparator() {
        return (IS_WINDOWS? "&&" : ";");
    }
    
    /**
     * Executes a command using the standard shell of the platform. Unlike
     * {@link #shellExec(String)}, does not wait until the command is completed.
     * 
     * @param command the shell command to be executed
     * @return the process of the shell
     * @exception IOException if an I/O error occurs
     */
    public static Process shellStart(String command) throws IOException { 
        Process process;
        
        if (IS_WINDOWS) 
            process = Runtime.getRuntime().exec(new String[] {
                    "cmd.exe", "/c", command });
        else 
            process = Runtime.getRuntime().exec(new String[] {
                    "/bin/sh", "-c", command });
        
        return process;
    }
    
    /**
     * Executes a command using the standard shell of the platform.
     * 
     * @param command the shell command to be executed
     * @return the exit status returned by the shell
     * @exception IOException if an I/O error occurs
     * @exception InterruptedException if the current thread is interrupted by
     * another thread while it is waiting the completion of the shell command
     */
    public static int shellExec(String command) 
    throws IOException, InterruptedException { 
        return shellExec(command, false);
    }
    
    /**
     * Executes a command using the standard shell of the platform.
     * 
     * @param command the shell command to be executed
     * @param verbose if true, the shell command output on
     * <code>System.out</code> and <code>System.err</code> is echoed; if
     * false, this output is discarded
     * @return the exit status returned by the shell
     * @exception IOException if an I/O error occurs
     * @exception InterruptedException if the current thread is interrupted by
     * another thread while it is waiting the completion of the shell command
     */
    public static int shellExec(String command, boolean verbose) 
    throws IOException, InterruptedException { 
        if (verbose)
            System.out.println(command);
        
        Process process = shellStart(command);
        
        // Without these consumer threads, any shell command that outputs
        // something lengthy will block on Windows NT.
        
        InputConsumer consumer1;
        InputConsumer consumer2;
        if (verbose) {
            consumer1 = new InputConsumer(process.getInputStream(),System.out);
            consumer2 = new InputConsumer(process.getErrorStream(),System.err);
        } else {
            consumer1 = new InputConsumer(process.getInputStream());
            consumer2 = new InputConsumer(process.getErrorStream());
        }
        consumer1.start();
        consumer2.start();
        
        int exitStatus = process.waitFor();
        
        consumer1.join();
        consumer2.join();
        
        return exitStatus;
    }
    
    /**
     * Executes a command using the standard shell of the platform, capturing
     * output to <code>System.out</code> and <code>System.err</code>.
     * 
     * @param command the shell command to be executed
     * @param capture output to <code>System.out</code> is captured and saved
     * to <code>capture[0]</code> and output to <code>System.err</code> is
     * captured and saved to <code>capture[1]</code>.
     * @return the exit status returned by the shell
     * @exception IOException if an I/O error occurs
     * @exception InterruptedException if the current thread is interrupted by
     * another thread while it is waiting the completion of the shell command
     */
    public static int shellExec(String command, String[] capture) 
    throws IOException, InterruptedException { 
        Process process = shellStart(command);
        return captureOutput(process, capture);
    }
    
    /**
     * Captures output of specified newly started process (see {@link
     * #shellStart(String)}).
     * 
     * @param process newly started process
     * @param capture output to <code>System.out</code> is captured and saved
     * to <code>capture[0]</code> and output to <code>System.err</code> is
     * captured and saved to <code>capture[1]</code>.
     * @return exit status of process
     * @exception InterruptedException if the current thread is interrupted by
     * another thread while it is waiting the completion of the process
     */
    public static int captureOutput(Process process, String[] capture) 
    throws InterruptedException { 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        
        InputConsumer consumer1 = 
            new InputConsumer(process.getInputStream(), out);
        InputConsumer consumer2 = 
            new InputConsumer(process.getErrorStream(), err);
        consumer1.start();
        consumer2.start();
        
        int status = process.waitFor();
        
        consumer1.join();
        consumer2.join();
        
        capture[0] = out.toString();
        capture[1] = err.toString();
        
        return status;
    }
    
    /**
     * Returns all environment variables in a <code>HashMap</code>.
     * <p><code>entrySet().iterator()</code> can be used to enumerate
     * <code>Map.Entry</code> where the key is (case-sensitive) name of the
     * environment variable and the value is the value of the environment
     * variable.
     */
    public static HashMap getEnvironment() {
        HashMap env = new HashMap();
        getEnvironment(env);
        return env;
    }
    
    /**
     * Adds all environment variables to specified <code>HashMap</code>.
     * <p><code>entrySet().iterator()</code> can be used to enumerate
     * <code>Map.Entry</code> where the key is (case-sensitive) name of the
     * environment variable and the value is the value of the environment
     * variable.
     */
    public static void getEnvironment(HashMap env) {
        String capture = "";
        
        try {
            Process process;
            if (IS_WINDOWS) 
                process = Runtime.getRuntime().exec(
                                                    new String[] { "cmd.exe", "/c", "set" });
            else 
                process = Runtime.getRuntime().exec(
                                                    new String[] { "/bin/sh", "-c", "env" });
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputConsumer consumer1 = 
                new InputConsumer(process.getInputStream(), out);
            InputConsumer consumer2 = 
                new InputConsumer(process.getErrorStream());
            consumer1.start();
            consumer2.start();
            
            process.waitFor();
            
            consumer1.join();
            consumer2.join();
            
            capture = out.toString();
        } catch (Exception ignored) {
            //ignored.printStackTrace();
            return;
        }
        
        StringTokenizer lines = new StringTokenizer(capture, "\r\n");
        while (lines.hasMoreTokens()) {
            String line = lines.nextToken();
            int lineLength = line.length();
            if (lineLength == 0)
                continue;
            
            // Env and set use the same format.
            int pos = line.indexOf('=');
            if (pos > 0 && pos < lineLength-1)
                env.put(line.substring(0, pos), line.substring(pos+1));
        }
    }
    
    // -----------------------------------------------------------------------
    
    private static class InputConsumer extends Thread {
        private InputStream in;
        private OutputStream out;
        private byte[] bytes = new byte[4096];
        
        public InputConsumer(InputStream in) {
            this(in, null);
        }
        
        public InputConsumer(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }
        
        public void run() {
            for (;;) {
                int count;
                try {
                    count = in.read(bytes);
                } catch (IOException e) {
                    //e.printStackTrace();
                    count = -1;
                }
                if (count < 0)
                    break;
                
                if (count > 0 && out != null) {
                    try {
                        out.write(bytes, 0, count);
                        out.flush();
                    } catch (IOException e) {
                        //e.printStackTrace();
                        out = null;
                    }
                }
            }
        }
    }
}
