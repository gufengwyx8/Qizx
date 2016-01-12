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
package com.qizx.xquery.ext;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.util.XMLSerializer;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

import java.io.FileOutputStream;
import java.io.StringWriter;

/**
 * Implementation of function x:serialize(). Options are defined by the
 * attributes of a node passed as optional second argument:
 * 
 * <pre>
 *   output  CDATA
 *   method (XML | XHTML | HTML | TEXT)
 *   encoding CDATA
 *   omit-xml-declaration (yes | no)
 *   standalone (yes | no)
 *   doctype-system
 *   doctype-public
 *   escape-uri-attributes (yes | no)
 *   include-content-type (yes | no)
 *   indent (yes | no)
 *   indent-value integer
 * </pre>
 */
public class XfnSerialize extends ExtensionFunction
{

    static QName qfname =
        IQName.get(EXTENSION_NS, "serialize");
    static Prototype[] protos = {
            new Prototype(qfname, XQType.STRING.opt, Exec.class)
                .arg("node", XQType.NODE),
            new Prototype(qfname, XQType.STRING.opt, Exec.class)
                .arg("node", XQType.NODE)
                .arg("options", XQType.ELEMENT)
        };

    public Prototype[] getProtos() {
        return protos;
    }

    private static String getOption(Node options, String name) 
        throws DataModelException
    {
        if (options == null)
            return null;
        Node attr = options.getAttribute(IQName.get(name));
        return attr == null ? null : attr.getStringValue();
    }


    public static class Exec extends Function.OptStringCall
    {

        private static final String OPTION_FILE = "file";
        private static final String OPTION_OUTPUT = "output";

        public String evalAsOptString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            Node options =
                args.length < 2 ? null : args[1].evalAsNode(focus, context);
            context.at(this);
            // special option file/output
            String output = null;
            try {
                output = getOption(options, OPTION_FILE);
                if (output == null)
                    output = getOption(options, OPTION_OUTPUT);
            }
            catch (DataModelException e1) {
                dmError(context, e1); 
            }

            XMLSerializer serial = new XMLSerializer();
            try {
                // other options:
                if (options != null) {
                    Node[] optlist = options.getAttributes();
                    if(optlist != null)
                        for (int a = 0; a < optlist.length; a++) {
                            Node attr = optlist[a];
                            String option = attr.getNodeName().getLocalPart();
                            if (!option.equals(OPTION_OUTPUT) &&
                                !option.equals(OPTION_FILE))
                               serial.setOption(option, attr.getStringValue());
                        }
                }
                // use in-scope NS as hints for xmlns:* generation:
                //serial.defineContextMappings(context.getStaticContext().getInScopeNS());

                serial.reset(); // mandatory
                FileOutputStream outs = null;
                StringWriter stringResult = null;
                if (output != null)
                    serial.setOutput(outs = new FileOutputStream(output),
                                     serial.getEncoding());
                // return a string by default
                else
                    serial.setOutput(stringResult = new StringWriter());

                args[0].evalAsEvents(serial, focus, context);
                serial.flush();

                if (outs != null)
                    outs.close();
                // no output specified: return the serialized text
                if (stringResult != null)
                    return stringResult.toString();
            }
            catch (EvaluationException e) {
                throw e; // just to avoid the catch below
            }
            catch (Exception e) {
                // e.printStackTrace();
                context.error("XQSE0001", this,
                              new EvaluationException("serialization error: "
                                                      + e.toString(), e));
                return null;
            }
            return output;
        }
    }
}
