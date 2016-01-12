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

import com.qizx.api.DataModelException;
import com.qizx.api.Product;
import com.qizx.api.QName;
import com.qizx.api.SequenceType;
import com.qizx.api.XQueryContext;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.util.Collations;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Check;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xquery.op.GlobalVariable;

import java.net.URI;
import java.text.Collator;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class BasicStaticContext
    implements XQueryContext
{
    protected String defaultFunctionNS = NamespaceContext.FN;
    protected String defaultElementNS = NamespaceContext.EMPTY;

    protected boolean preserveBoundarySpace = false;
    protected boolean preserveSchemaTypes = true; // dummy
    protected boolean preserveNamespaces = true;
    protected boolean inheritNamespaces = true;
    protected boolean emptyGreatest;
    protected boolean ordered = true;
    protected boolean sobs; // specs make me cry sometimes...
    protected boolean strictTyping;

    protected PredefinedModule predefined;
    protected HashMap<QName, GlobalVariable> globalMap = 
        new HashMap<QName, GlobalVariable>();
    protected HashMap<QName, String> optionMap;
    
    protected HashMap<String, Collator> registeredCollations;
    protected String  defaultCollation = Collations.CODEPOINT;
    protected Collator defaultCollator = null; // ie codepoints

    private String baseURI;
    private NamespaceContext nsContext;
    private TimeZone timezone;
    private Date forcedDate;
    private FullTextFactory fulltextFactory;
    
    public BasicStaticContext()
    {
        this.timezone = TimeZone.getDefault();
        
        nsContext = new NamespaceContext();
        declarePrefix("xml", NamespaceContext.XML);
        declarePrefix("xs", NamespaceContext.XSD);
        declarePrefix("xsi", NamespaceContext.XSI);
        // temporary hack: prefix math: == fn:
        declarePrefix("math", NamespaceContext.FN);
        declarePrefix("fn", NamespaceContext.FN);
        declarePrefix("xdt", NamespaceContext.XDT);
        declarePrefix("local", NamespaceContext.LOCAL_NS);
        declarePrefix("output", NamespaceContext.OUTPUT_NS);
        // mark to avoid redefining predefined ns:
        nsContext.newLevel();
    }

    // copy all properties
    protected void copy(BasicStaticContext parent)
    {
        this.baseURI = parent.baseURI;

        this.defaultCollation = parent.defaultCollation;
        this.defaultCollator = parent.defaultCollator;
       
        this.defaultFunctionNS = parent.defaultFunctionNS;
        this.defaultElementNS = parent.defaultElementNS;

        this.preserveBoundarySpace = parent.preserveBoundarySpace;
        this.preserveSchemaTypes = parent.preserveSchemaTypes;
        this.preserveNamespaces = parent.preserveNamespaces;
        this.inheritNamespaces = parent.inheritNamespaces;
        this.emptyGreatest = parent.emptyGreatest;
        this.ordered = parent.ordered;
        this.predefined = parent.getPredefinedModule(); // created on demand
        this.sobs = parent.sobs;
        this.strictTyping = parent.strictTyping;
        this.timezone = parent.timezone;
        this.forcedDate = parent.forcedDate;
        this.globalMap = (HashMap<QName, GlobalVariable>) parent.globalMap.clone();
        
       // ns are merged:
        nsContext.popLevel(); // break seal, be sure to merge
        String[] prefixes = parent.getInScopePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            declarePrefix(prefixes[i], parent.getNamespaceURI(prefixes[i]));
        }
        nsContext.newLevel(); // seal
    }

    public String getBaseURI()
    {
        return baseURI;
    }

    // should check its a real URI
    public void setBaseURI(String baseURI)
    {
        this.baseURI = XMLUtil.collapseWhiteSpace(baseURI);
    }

    public int getBoundarySpacePolicy()
    {
        return preserveBoundarySpace ? PRESERVE : NO_PRESERVE;
    }

    public void setBoundarySpacePolicy(int boundarySpacePolicy)
    {
        preserveBoundarySpace =
            checkBoolArg(boundarySpacePolicy, PRESERVE, NO_PRESERVE, 
                         "PRESERVE or NO_PRESERVE");
    }

    public int getConstructionMode()
    {
        return preserveSchemaTypes ? PRESERVE : NO_PRESERVE;
    }

    public void setConstructionMode(int constructionMode)
    {
        preserveSchemaTypes =
            checkBoolArg(constructionMode, PRESERVE, NO_PRESERVE, 
                         "PRESERVE or NO_PRESERVE");
    }

    public boolean hasDefaultCollation()
    {
        return defaultCollation == Collations.CODEPOINT;
    }

    public String getDefaultCollation()
    {
        return defaultCollation; 
    }

    public void setDefaultCollation(String collation)
        throws DataModelException
    {
        this.defaultCollation = collation;
        
        if (getBaseURI() != null)
            try {
                URI result = new URI(getBaseURI()).resolve(defaultCollation);
                defaultCollation = result.toString();
                // ugly patch:
                if(defaultCollation.startsWith("file:"))
                    defaultCollation = defaultCollation.substring(5);
            }
            catch (Exception e) {
                return;
            }

        defaultCollator = getCollator(defaultCollation);
        if(defaultCollator == null && collation != null)
            throw new DataModelException("invalid collation " + collation);
    }

    public synchronized Collator getCollator(String uri)
    {
        if (uri == null)
            return defaultCollator;
        Collator col =
          (registeredCollations == null) ? null : registeredCollations.get(uri);
        if (col == null)
            col = Collations.getInstanceWithStrength(uri);
        return col;
    }

    public boolean getDefaultOrderEmptyGreatest()
    {
        return emptyGreatest;
    }

    public void setDefaultOrderEmptyGreatest(boolean emptyGreatest)
    {
        this.emptyGreatest = emptyGreatest;
    }

    public int getOrderingMode()
    {
        return ordered? ORDERED : UNORDERED;
    }

    public void setOrderingMode(int orderingMode)
    {
        ordered = checkBoolArg(orderingMode, ORDERED, UNORDERED,
                               "ORDERED or UNORDERED");
    }

    private boolean checkBoolArg(int value, int valueTrue, int valueFalse,
                                 String message)
    {
        if(value == valueTrue)
            return true;
        else if(value == valueFalse)
            return false;
        else
            throw new AssertionError("argument must be " + message);
    }

    public String getDefaultElementNamespace()
    {
        return defaultElementNS;
    }

    public void setDefaultElementNamespace(String defaultElementNamespace)
    {
        defaultElementNS = NamespaceContext.unique(defaultElementNamespace);
    }

    public String getDefaultFunctionNamespace()
    {
        return defaultFunctionNS;
    }

    public void setDefaultFunctionNamespace(String defaultFunctionNamespace)
    {
        this.defaultFunctionNS =
            NamespaceContext.unique(defaultFunctionNamespace);
    }

    public int getNamespaceInheritMode()
    {
        return inheritNamespaces? INHERIT : NO_INHERIT;
    }

    public void setNamespaceInheritMode(int namespaceInheritMode)
    {
        inheritNamespaces =
            checkBoolArg(namespaceInheritMode, INHERIT, NO_INHERIT, 
                         "INHERIT or NO_INHERIT");
    }

    public int getNamespacePreserveMode()
    {
        return preserveNamespaces? PRESERVE : NO_PRESERVE;
    }

    public void setNamespacePreserveMode(int namespacePreserveMode)
    {
        preserveNamespaces = 
            checkBoolArg(namespacePreserveMode, PRESERVE, NO_PRESERVE, 
                         "PRESERVE or NO_PRESERVE");
    }

    public QName[] getOptionNames()
    {
        if(optionMap == null)
            return new QName[0];
        QName[] names = new QName[optionMap.size()];
        int n = 0;
        for(QName name : optionMap.keySet())
            names[n++] = name;        
        return names;
    }

    public String getOptionValue(QName optionName)
    {
        return optionMap == null? null : optionMap.get(optionName);
    }

    public QName[] getVariableNames()
    {
        QName[] names = new QName[globalMap.size()];
        int n = 0;
        for(QName name : globalMap.keySet())
            names[n++] = name;        
        return names;
    }

    public SequenceType getVariableType(QName variableName)
    {
        Check.nonNull(variableName, "variableName");
        GlobalVariable g = localGlobalVarLookup(variableName);
        return (g == null)? null
                : (g.declaredType == null) ? XQType.ANY : g.declaredType; 
    }

    public void declareVariable(QName variableName, SequenceType variableType)
    {
        if(variableType != null)
            Check.implementation(variableType, com.qizx.xquery.SequenceType.class,
                                 SequenceType.class);
        declareGlobal(variableName,
                      (com.qizx.xquery.SequenceType) variableType);
    }

    public void declarePrefix(String prefix, String namespaceURI)
    {
         nsContext.addMapping(prefix, namespaceURI);
    }

    public String[] getInScopePrefixes()
    {
        String[] prefixes = nsContext.getPrefixes();
        // remove empty prefix
        if(prefixes.length > 0 && prefixes[0].length() == 0) {
            String[] old = prefixes;
            prefixes = new String[old.length - 1];
            System.arraycopy(old, 1, prefixes, 0, prefixes.length);
        }
        return prefixes;
    }

    public String getNamespaceURI(String prefix)
    {
        return nsContext.getNamespaceURI(prefix);
    }

    public String getNSPrefix(String uri)
    {
        return nsContext.getPrefix(uri);
    }

    public String getXQueryVersion()
    {
        return Product.XQUERY_VERSION;
    }

    public TimeZone getImplicitTimeZone()
    {
        return timezone;
    }

    public void setImplicitTimeZone(TimeZone implicitTimeZone)
    {
        timezone = implicitTimeZone;
    }

    public Date getCurrentDate()
    {
        return forcedDate;
    }

    public void setCurrentDate(Date forcedDate)
    {
         this.forcedDate = forcedDate;
    }

    public boolean getStrictCompliance()
    {
        return sobs;
    }

    public void setStrictCompliance(boolean strictCompliance)
    {
        sobs = strictCompliance;
    }

    // -------------------- implementation ----------------------------------

    protected GlobalVariable localGlobalVarLookup(QName qname)
    {
        Check.nonNull(qname, "qname");
        return globalMap.get(qname);
    }

    protected GlobalVariable declareGlobal(QName qname,
                                           com.qizx.xquery.SequenceType type)
    {
        Check.nonNull(qname, "qname");
        GlobalVariable g = new GlobalVariable(qname, type, null);
        globalMap.put(qname, g);
        return g;
    }
    
    public NamespaceContext getInScopeNS()
    {
        return nsContext;
    }

    public String convertPrefixToNamespace(String prefix)
    {
        Check.nonNull(prefix, "prefix");
        return nsContext.getNamespaceURI(prefix);
    }

    public String prefixedName(QName name)
    {
        return nsContext.prefixedName(name);
    }

    public PredefinedModule getPredefinedModule()
    {
        // instance shared by sub-contexts ' attached to a session/Library
        if(predefined == null)
            predefined = new PredefinedModule();
        return predefined;
    }

    public FullTextFactory getFulltextFactory()
    {
        return fulltextFactory;
    }

    public void setFulltextProvider(FullTextFactory fulltextProvider)
    {
        this.fulltextFactory = fulltextProvider;
    }

    public boolean getStrictTyping()
    {
        return strictTyping;
    }

    public void setStrictTyping(boolean strictTyping)
    {
        this.strictTyping = strictTyping;
    }
}
