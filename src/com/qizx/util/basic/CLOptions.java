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

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Command line option analyzer.
 * <p>Options are defined in this object with:
 * <ul>
 * <li>a switch key,
 * <li>an argument mode (no arg, following arg, sticky arg ...)
 * <li>the name of a variable for storing the argument value or a method to
 * call with the argument value,
 * <li>a help message.
 * </ul>
 * <p>When an option is recognized by the parse method, either a field of a
 * target application object can be set, or a method of this object can be
 * invoked.
 */
public class CLOptions
{
    String appName;
    Option[] options = new Option[1];   // rank 0: default argument
    String[] args;
    int argp, anonymous;
    
    int HELP_TAB = 22;
    
    // syntax of argument:
    final static char ARG_SET = '.';
    final static char ARG_RESET = '-';
    final static char ARG_NEXT = '_';
    final static char ARG_STUCK = ':';
    final static char ARG_ALL = '*';
    final static char ARG_SELF = '@';
    final static char ARG_SECTION = '#';
    
    // action:
    final static char ACT_SET = '=';    // set field
    final static char ACT_ADD = '+';    // add to array
    final static char ACT_PROP = '[';   // key=value add to Properties 
    final static char ACT_CALL = '!';   // call method
    final static char ACT_HELP = '?';
    
    protected static class Option {
        String key;
        String argLabel;        // argument label
        String actName; // field or method
        String help;    // help description
        char   syntax;
        char   action;
        
        Option( String key, char syntax, String argLabel,
                char action, String actName, String help) {
            this.key = key; this.syntax = syntax; this.argLabel = argLabel;
            this.action = action; this.actName = actName;
            this.help = help;
        }
    }
    
    public static class Error extends Exception {
        public Error() { }
        public Error(String msg) { super(msg); }
    }
    
    /**
     * Creation with an application name.
     * <p>Options must then be defined by define().
     * 
     * @param appName name of the application
     */
    public CLOptions( String appName ) {
        this.appName = appName;
    }
    
    /**
     * Loads a property file from the user's home directory.
     * @param name
     * @return a non-null set of properties (may be empty if file not found)
     */
    public static Properties getDefaultProperties(String name) {
        Properties props = new Properties();
        String filename = PlatformUtil.rcFileName(name);
        try {
            InputStream input = new FileInputStream(filename);
            props.load(input);
            input.close();
        }
        catch (IOException e) {
            // ignored
        }
        return props;
    }
    
    
    /**
     * Defines an option.
     * 
     * @param keyDef appearance of the option switch.
     * <p>A null key means a stray argument. The last character defines how an
     * argument of this switch is processed (the argument value can be
     * assigned to a field or passed to a method, according to parameter
     * actionDef):
     * <ul>
     * <li>Dot '.' or ARG_SET: no following argument, pass a Boolean.TRUE
     * value.
     * <li>Dash '-' or ARG_RESET: no following arg, pass a Boolean.FALSE
     * value.
     * <li>Underscore '_' or ARG_NEXT: argument follows after a space, pass
     * its value.
     * <li>Colon ':' or ARG_STUCK: argument is stuck to this option switch.
     * <li>Star '*' or ARG_ALL: get all following command-line arguments into
     * a String array value.
     * </ul>
     * @param argLabel A description of the argument.
     * @param actionDef name of a Java field or a method of the application
     * object. The first character defines the action:<ul>
     * <li>'!' calls a method,
     * <li>'+' sets a field: error if the (String) field is already set.
     * <li>'=' adds to a field: if the field is an array, the value is appended,
     * otherwise it replaces the old value.
     * <li>'?' prints the help.
     * </ul>
     * @param help option help description.
     */
    public void define(String keyDef, String argLabel, String actionDef,
                       String help)
    {
        char syntax = ARG_SELF;
        if(keyDef != null) {
            syntax = keyDef.charAt(keyDef.length() - 1);
            keyDef = keyDef.substring(0, keyDef.length() - 1);
        }
        Option op = new Option(keyDef, syntax, argLabel, actionDef.charAt(0),
                               actionDef.substring(1), help);
        if (keyDef == null)
            options[0] = op;
        else {
            Option[] old = options;
            options = new Option[old.length + 1];
            System.arraycopy(old, 0, options, 0, old.length);
            options[old.length] = op;
        }
    }

    public void defineSection(String title)
    {
        define("#", "", "#", title);
    }

    /**
     * Parses the command line.
     * <p>Option switches can either set the value of a member of object
     * 'appli', or call a method of this object.
     */
    public void parse( String[] args, Object appli ) throws Exception {
        this.args = args;
        argp = 0;
        try {
            for( ; argp < args.length; ) {
                String arg = args[argp++];
                Option op = matchOption(arg);
                if(op == null)	// if no default argument defined
                    throw new Error("unknown option: "+arg);
                Object argument = null;
                switch(op.syntax) {
                case ARG_SELF:
                    argument = arg;
                    break;
                case ARG_SET:
                    argument = Boolean.TRUE;
                    break;
                case ARG_RESET:
                    argument = Boolean.FALSE;
                    break;
                case ARG_NEXT:
                    if(argp >= args.length)
                        throw new Error("option "+op.key+" requires an argument");
                    argument = args[argp++];
                    break;
                case ARG_STUCK:
                    argument = arg.substring(op.key.length());
                    break;
                case ARG_ALL:
                    String[] nargs = new String[args.length - argp];
                    System.arraycopy(args, argp, nargs, 0, nargs.length);
                    argument = nargs;
                    argp = args.length;
                    break;
                default:
                    throw new RuntimeException("illegal option syntax: "+op.syntax);
                }
                
                switch(op.action) {
                case ACT_SET:
                    setField(appli, op.key, op.actName, argument);
                    break;
                case ACT_ADD:
                    addToField(appli, op.key, op.actName, argument);
                    break;
                case ACT_PROP:
                    addToProperties(appli, op.key, op.actName, argument);
                    break;
                case ACT_CALL:
                    callMethod(appli, op.key, op.actName, argument);
                    break;
                case ACT_HELP:
                    throw new Error("help:");
                default:
                    throw new RuntimeException("illegal option action: "+op.action);
                }
            }
            // try to call method finish() :
            try {
                Method meth = appli.getClass().getDeclaredMethod("finish", new Class[0]);
                meth.invoke(appli, (java.lang.Object[]) null);
            }
            catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if(cause instanceof java.lang.Error)
                    throw (java.lang.Error) cause;
                throw (Exception) ex.getCause();
            }
            catch (NoSuchMethodException ex) { }
        }
        catch (Error e) {
            System.err.println("*** "+ e.getMessage());
            printHelp(System.err);
            throw e;
        }
    }
    
    public void printHelp(PrintStream out)
    {
        out.print("usage: " + appName + " [options] ");
        if (options[0] != null) {
            out.println(options[0].argLabel); // default argument
            // out.println();
        }
        if (options[0] != null)
            printHelp(out, options[0]);

        for (int op = 1; op < options.length; op++) {
            printHelp(out, options[op]);
        }
    }
    
    void printHelp(PrintStream out, Option opt)
    {
        String help = opt.help;
        if (help == null)   // hidden
            return;
        String key = opt.key == null ? "" : opt.key;
        int ptr = key.length();
        if(opt.syntax != ARG_SECTION)
        {
            out.print("  " + key);
            if(opt.syntax != ARG_STUCK) {
                out.print(' '); ++ ptr;
            }

            ptr += opt.argLabel.length();
            out.print(opt.argLabel);
            for(; ptr < HELP_TAB; ptr++)
                out.print(' ');
        }
        else out.println();
        
        String[] helps = help.split("\n");
        out.println(" " + helps[0]);
        for(int h = 1; h < helps.length; h++) {
            for(ptr = -2; ptr < HELP_TAB; ptr++)
                out.print(' '); 
            out.println(" "+helps[h]);
        }
    }
    
    // looks up the option table.
    private Option matchOption( String arg ) {
        for(int op = options.length; --op > 0; )
            if( options[op].key.equals(arg) ||
                    options[op].syntax == ARG_STUCK &&
                    arg.startsWith(options[op].key) )
                return options[op];
        // patch: do not accept default arg beginning with '-':
        if(arg.startsWith("-"))
            return null;
        return options[0];
    }
    
    void callMethod(Object target, String key, String method, Object value)
    throws Exception {
        try {
            Method meth = target.getClass().getDeclaredMethod(method,
                                                              new Class[] { value.getClass() } );
            meth.invoke(target, new Object[] { value });
        }
        catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if(cause instanceof java.lang.Error)
                throw (java.lang.Error) cause;
            throw (Exception) cause;
        }
        catch (Exception e) {
            System.err.println("Error in option "+key+":" + e);
            throw e;
        }
    }
    
    public void setField(Object target, String key, String field, Object value)
        throws Exception
    {    
        try {
            Field f = target.getClass().getField( field );
            Class fieldClass = f.getType();
            
            if (fieldClass == int.class) 
                f.setInt(target, Integer.parseInt((String) value));
            else if (fieldClass == float.class)
                f.setFloat(target, Float.parseFloat((String) value));
            else if (fieldClass == double.class)
                f.setDouble(target, Double.parseDouble((String) value));
            else if (fieldClass == boolean.class) {
                if(value instanceof String) {
                    f.setBoolean( target, value.equals("1") 
                                  || ((String) value).equalsIgnoreCase("yes")
                                  || ((String) value).equalsIgnoreCase("true"));
                }
                else f.set( target, value );
            }
            else {
                if(f.get(target) != null)
                    throw new Error("duplicate value for "+
                                    (key == null? "argument" : "option "+key));
                f.set( target, value );
            }
        }
        catch (Error err) {
            throw err;
        }
        catch (Exception e) {
            System.err.println("Error in option "+key+": " + e);
            throw e;
        }
    }
    
    public void addToField(Object target, String key, String field, Object value)
        throws Exception
    {
        try {
            Field f = target.getClass().getField(field);
            Class fieldClass = f.getType();
            
            if(fieldClass.isArray()) {
                // only String[] supported:
                String[] old = (String[]) f.get(target), na = null;
                if(value instanceof String) {
                    int olen = (old == null)? 0 : old.length;
                    na = new String[ olen + 1 ];
                    if(olen > 0)
                        System.arraycopy(old, 0, na, 0, olen);
                    na[olen] = (String) value;
                }
                else {
                    String[] nv = (String[]) value;
                    na = new String[ old.length + nv.length ];
                    System.arraycopy(old, 0, na, 0, old.length);
                    System.arraycopy(nv, 0, na, old.length, nv.length);
                }
                f.set( target, na );
            }
            else {	// only String supported
                f.set( target, value );
            }
        }
        catch (Exception e) {
            System.err.println("Error in option " + key + ": " + e);
            throw e;
        }
    }
    
    void addToProperties(Object target, String key, String field, Object value)
    throws Exception {
        try {
            Field f = target.getClass().getField( field );
            
            Properties props = (Properties) f.get(target);
            String val = (String) value;
            int eq = val.indexOf('=');
            if(eq < 0)
                throw new Error("illegal property: "+val+ " for "+key);
            props.setProperty( val.substring(0, eq), val.substring(eq + 1) );
        }
        catch (Exception e) {
            System.err.println("Error in option "+key+": " + e);
            throw e;
        }
    }
}


