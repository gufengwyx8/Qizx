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

import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.api.util.fulltext.DefaultTextTokenizer;
import com.qizx.api.util.text.FormatDateSieve;
import com.qizx.api.util.text.FormatNumberSieve;
import com.qizx.api.util.text.ISODateSieve;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Check;
import com.qizx.util.basic.Util;
import com.qizx.xdm.DataConversion;
import com.qizx.xdm.DocumentParser;
import com.qizx.xdm.IQName;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.beans.Beans;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Specification of rules and parameters used to build indexes in a XML
 * Library.
 * <p>
 * An instance of this class is associated with each XML Library. It can be
 * represented in XML, thus read and written in this format. A DTD is provided
 * in the documentation.
 * <p>
 * The direct use of this class is recommended only for very advanced problems,
 * for example when an indexing specification needs to be generated
 * dynamically. In most cases, an indexing specification is written in
 * XML. Its most common use consists of defining specific Numeric or Date
 * Sieves, or to tune parameters like minWordLength.
 */
public class Indexing implements DataConversion
{
    /**
     * Indexing type for a rule: text fragment directly put to string index if
     * length is less than max-string-length.
     */
    public static final byte STRING = 1;
    /**
     * Indexing type for a rule: attempt to convert element or attribute text
     * content with a NumberSieve then put it to numeric index.
     */
    public static final byte NUMERIC = 2;
    /**
     * Indexing type for a rule: same as both NUMERIC and STRING.
     */
    public static final byte NUMERIC_AND_STRING = NUMERIC + STRING;
    /**
     * Indexing type for a rule: attempt to convert element or attribute text
     * content with a DateSieve then put it to date index.
     */
    public static final byte DATE = 4;
    /**
     * Indexing type for a rule: same as both DATE and STRING.
     */
    public static final byte DATE_AND_STRING = DATE + STRING;

    
    /**
     * Default value for full-text on a Rule: inherit from parent
     * element or from global setting.
     */
    public static final byte INHERIT = 0;
    /**
     * Value for full-text option (global or per rule) disabling full-text.
     */
    public static final byte DISABLE_FULL_TEXT = -1;
    /**
     * Value for full-text option (global or per rule) enabling full-text.
     */
    public static final byte ENABLE_FULL_TEXT = 1;
    

    private static final QName INDEXING_ELEM = IQName.get("indexing");
    private static final QName ELEMENT_RULE = IQName.get("element");
    private static final QName ATTRIBUTE_RULE = IQName.get("attribute");

    private static final QName WORD_SIEVE = IQName.get("word-sieve");
    private static final QName WORD_MIN = IQName.get("word-min");
    private static final QName WORD_MAX = IQName.get("word-max");
    private static final QName STRING_MAX = IQName.get("string-max");

    private static final QName NAME = IQName.get("name");
    private static final QName CONTEXT = IQName.get("context");
    private static final QName FULLTEXT = IQName.get("full-text");
    private static final QName ATTR_FULLTEXT = IQName.get("attr-full-text");
    private static final QName AS = IQName.get("as");
    private static final QName SIEVE = IQName.get("sieve");

    private static final int DEFAULT_STRING_MAX = 50;

    private static final String[] INDEXING_TYPES = {
        null, "STRING", "NUMERIC", "NUMERIC+STRING", "DATE", "DATE+STRING",
        // aliases:
        "NUMBER", "NUMBER+STRING"
    };
    private static final byte[] INDEXING_CODES = {
        0,    STRING,   NUMERIC,  NUMERIC_AND_STRING,  DATE, DATE_AND_STRING,
        NUMERIC,  NUMERIC_AND_STRING
    };

    private static final String DEFAULT_WORDSIEVE_PACKAGE =
        DefaultTextTokenizer.class.getPackage().getName();
    private static final String DEFAULT_SIEVE_PACKAGE =
        FormatNumberSieve.class.getPackage().getName();

    // ---------------------------------------------------------------------

    //private TextTokenizer tokenizer;

    private int maxStringLength = DEFAULT_STRING_MAX;
    private int maxWordLength = 30;
    private int minWordLength = 1;
    private boolean fulltext = true;
    private boolean attrFulltext = false;   // full-text on attributes
    
    private ArrayList<Rule> elementRules;
    private ArrayList<Rule> attributeRules;
    // keep a trace of namespaces declared in the sheet if any
    private NamespaceContext namespaces = new NamespaceContext();
    private int ruleRank;
    
    // direct matching on nodes: used as a fallback for non-indexable queries
    // if the standard conversion of a value to double or date fails, then we
    // use Indexing rules to try to convert the value.
    private HashMap<QName, ArrayList<Rule>> fastElemMap; // maps QNames to an ordered rule list
    private HashMap<QName, ArrayList<Rule>> fastAttrMap;
    private static final QName DEF = IQName.get("*");


    /**
     * Creates an empty indexing configuration.
     */
    public Indexing()
    {
        elementRules = new ArrayList<Rule>();
        attributeRules = new ArrayList<Rule>();
    }

    /**
     * Creates a default indexing configuration.
     * @return a new instance which contains default indexing rules.
     */
    public static Indexing defaultRules()
    {
        Indexing rules = new Indexing();

        rules.addDefaultElementRule(null, DATE_AND_STRING);
        rules.addDefaultElementRule(null, NUMERIC_AND_STRING);
        rules.addDefaultElementRule(null, STRING);

        rules.addDefaultAttributeRule(null, DATE_AND_STRING);
        rules.addDefaultAttributeRule(null, NUMERIC_AND_STRING);
        rules.addDefaultAttributeRule(null, STRING);

        //rules.tokenizer = new DefaultTextTokenizer();
        return rules;
    }

    /**
     * Parses an Indexing specification from XML text representation.
     * @param source a SAX input source for the specification
     * @throws QizxException on on IO error, SAX parsing error, if the indexing
     * specification is invalid
     */
    public void parse(InputSource source)
        throws QizxException
    {
        Check.nonNull(source, "source");
        try {
            parse(DocumentParser.parse(source));
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new QizxException("parse error: " + e.getMessage(), e);
        }
    }

    /**
     * Parses an Indexing specification from XML text representation.
     * @param source a SAX input source for the specification
     * @param parser a SAX parser explicitly specified
     * @throws QizxException on on IO error, SAX parsing error, if the indexing
     * specification is invalid
     */
    public void parse(InputSource source, XMLReader parser)
        throws QizxException
    {
        try {
            Check.nonNull(source, "source");
            parse(DocumentParser.parse(source, parser));
        }
        catch (Exception e) {
            throw new QizxException("parse error: " + e.getMessage(), e);
        }
    }

    /**
     * Parses an Indexing specification from a Node.
     * @param specification a node of type document or element which is the root
     * of an indexing specification.
     * @throws DataModelException if the indexing specification is invalid
     */
    public void parse(Node specification)
        throws DataModelException
    {
        Check.nonNull(specification, "specification");
        ruleRank = 0;
        if (specification.getNodeNature() == Node.DOCUMENT) {
            Node tops = specification.getFirstChild();
            for (; tops != null && tops.getNodeNature() != Node.ELEMENT;)
                tops = tops.getNextSibling();
            specification = tops;
        }
        if (specification == null || specification.getNodeName() != INDEXING_ELEM)
            error("'indexing' element expected");

        // parse attributes of main element:
        Node[] props = specification.getAttributes();
        if (props != null)
            for (int i = 0, asize = props.length; i < asize; i++) {
                QName name = props[i].getNodeName();
                if (name == WORD_MIN)
                    minWordLength = integerProp(props[i]);
                else if (name == WORD_MAX)
                    maxWordLength = integerProp(props[i]);
                else if (name == STRING_MAX)
                    maxStringLength = integerProp(props[i]);
                else if (name == FULLTEXT)
                    fulltext = booleanProp(props[i]);
                else if (name == ATTR_FULLTEXT)
                    attrFulltext = booleanProp(props[i]);
                else if (name == WORD_SIEVE) {
                    // silently ignored
                }
                else 
                    error("unknown property: " + name);
            }

        // scan rules:
        Node ruleNode = specification.getFirstChild();
        ruleRank = 1;
        for (; ruleNode != null; ruleNode = ruleNode.getNextSibling()) {
            QName name = ruleNode.getNodeName();
            if (name == null)
                continue;
            if (name == ELEMENT_RULE) {
                Rule rule = parseRule(ruleNode);
                elementRules.add(rule);
            }
            else if (name == ATTRIBUTE_RULE) {
                Rule rule = parseRule(ruleNode);
                attributeRules.add(rule);
            }
            else
                error("invalid rule " + ruleRank + ": " + name
                      + ": 'element' or 'attribute' expected");
            ++ruleRank;
        }
    }

    /**
     * Converts to an external representation.
     * @param stream a Push style interface (for example XMLSerializer).
     * @throws DataModelException if generated by the output stream
     */
    public void export(XMLPushStream stream) throws DataModelException
    {
        Check.nonNull(stream, "writer");
        stream.putDocumentStart();
        stream.putElementStart(INDEXING_ELEM);
        // namespaces definitions for QNames of the sheet:
        for (int r = 0, size = elementRules.size(); r < size; r++)
            ruleNamespaces(getElementRule(r));
        for (int r = 0, size = attributeRules.size(); r < size; r++)
            ruleNamespaces(getAttributeRule(r));

        for (int ns = 0, cnt = namespaces.getLocalSize(); ns < cnt; ns++)
            stream.putNamespace(namespaces.getLocalPrefix(ns),
                                namespaces.getLocalNamespaceURI(ns));

        // general properties as attributes:
        stream.putAttribute(WORD_MIN, "" + minWordLength, null);
        stream.putAttribute(WORD_MAX, "" + maxWordLength, null);
        stream.putAttribute(STRING_MAX, "" + maxStringLength, null);
        if(!fulltext) // a bit weird
            stream.putAttribute(FULLTEXT, "no", null);

        // rules in order
        for (int r = 0, size = elementRules.size(); r < size; r++) {
            exportRule(stream, ELEMENT_RULE, getElementRule(r));
        }
        for (int r = 0, size = attributeRules.size(); r < size; r++) {
            exportRule(stream, ATTRIBUTE_RULE, getAttributeRule(r));
        }

        stream.putElementEnd(INDEXING_ELEM);
        stream.putDocumentEnd();
    }

    private void exportRule(XMLPushStream writer, QName ruleName, Rule rule)
        throws DataModelException
    {
        writer.putElementStart(ruleName);
        if (rule.name != null)
            writer.putAttribute(NAME, exportName(rule.name), null);
        writer.putAttribute(AS, INDEXING_TYPES[rule.getIndexingType()], null);
        // context, sieve, fulltext
        QName[] ectx = rule.getContext();
        if (ectx != null) {
            StringBuffer buf = new StringBuffer(10 * ectx.length);
            for (int i = 0; i < ectx.length; i++) {
                if (i > 0)
                    buf.append('/');
                buf.append(exportName(ectx[i]));
            }
            writer.putAttribute(CONTEXT, buf.toString(), null);
        }
        
        // full text:
        if(rule.fullText != INHERIT)
            writer.putAttribute(FULLTEXT, 
                                rule.fullText == ENABLE_FULL_TEXT? "yes" : "no", null);
        
        // sieve and parameters:
        if(rule.sieve != null) {
            Class<? extends NumberSieve> sieveClass = rule.sieve.getClass();
            String sieveName = Util.stringAfter(sieveClass.getName(),
                                                "com.qizx.api.util.text.");
            
            if(sieveClass == ISODateSieve.class && 
                    (rule.indexingType == DATE || rule.indexingType == DATE_AND_STRING))
                     sieveName = null;
            if(sieveClass == FormatNumberSieve.class && 
                    (rule.indexingType == NUMERIC || rule.indexingType == NUMERIC_AND_STRING))
                     sieveName = null;
            
            if(sieveName != null)
                writer.putAttribute(SIEVE, sieveName, null);
            String[] param = rule.sieve.getParameters();
            if (param != null)
                for (int i = 0; i < param.length; i += 2) {
                    String name = param[i];
                    String value = param[i + 1];
                    writer.putAttribute(IQName.get(name), value, null);
                }
        }

        writer.putElementEnd(ruleName);
    }

    private void ruleNamespaces(Rule rule)
    {
        if(rule.name != null)
            checkName(rule.name);
        QName[] names = rule.getContext();
        if(names != null)
            for (int i = 0; i < names.length; i++)
                checkName(names[i]);
   
    }

    private void checkName(QName name)
    {
        String uri = name.getNamespaceURI();
        String prefix = (uri.length() == 0)? "" : namespaces.getPrefix(uri);
        if(prefix == null) {
            int rank = 1;
            for(;; ++rank){
                if(namespaces.getNamespaceURI("ns" + rank) == null)
                    break;
            }
            namespaces.addMapping("ns" + rank, uri);
        }
    }

    private String exportName(QName name)
    {
        return namespaces.prefixedName(name);
    }

    /**
     * Adds a new Rule applicable to a specific element.
     * @param elementName specific element name
     * @param context enclosing elements (optional, may be null)
     * @param indexingType possible values are STRING, NUMERIC, DATE, 
         * NUMERIC_AND_STRING, DATE_AND_STRING
     * @return the new Rule
     */
    public Rule addElementRule(QName elementName, QName[] context,
                               int indexingType)
    {
        Rule rule = new Rule(elementName, context, indexingType);
        rule.setDefaultSieve();
        elementRules.add(rule);
        return rule;
    }

    /**
     * Adds a new Rule applicable to all elements.
     * @param context enclosing elements (optional, may be null)
     * @param indexingType possible values are STRING, NUMERIC, DATE, 
         * NUMERIC_AND_STRING, DATE_AND_STRING
     * @return the new Rule
     */
    public Rule addDefaultElementRule(QName[] context, int indexingType)
    {
        return addElementRule(null, context, indexingType);
    }


    /**
     * Returns the number of defined Element rules, including default rules.
     * @return the number of defined Element rules
     */
    public int getElementRuleCount()
    {
        return elementRules.size();
    }

    /**
     * Returns the n-th element Rule.
     * @param index an index starting from 0
     * @return the element Rule at rank 'index'
     */
    public Rule getElementRule(int index)
    {
        return (index < 0 || index >= elementRules.size()) ? null
                : elementRules.get(index);
    }
    

    /**
     * Adds a new Rule applicable to a specific attribute.
     * @param attributeName specific attribute name
     * @param context enclosing elements (optional, may be null)
     * @param indexingType possible values are STRING, NUMERIC, DATE, 
         * NUMERIC_AND_STRING, DATE_AND_STRING
     * @return the new Rule
     */
    public Rule addAttributeRule(QName attributeName, QName[] context,
                                 int indexingType)
    {
        Rule rule = new Rule(attributeName, context, indexingType);
        rule.fullText = DISABLE_FULL_TEXT;
        rule.setDefaultSieve();
        attributeRules.add(rule);
        return rule;
    }

    /**
     * Adds a new Rule applicable to all attributes.
     * @param context enclosing elements (optional, may be null)
     * @param indexingType possible values are STRING, NUMERIC, DATE, 
         * NUMERIC_AND_STRING, DATE_AND_STRING
     * @return the new Rule
     */
    public Rule addDefaultAttributeRule(QName[] context, int indexingType)
    {
        return addAttributeRule(null, context, indexingType);
    }

    /**
     * Returns the number of defined Attribute rules, including default rules.
     * @return the number of defined Attribute rules
     */
    public int getAttributeRuleCount()
    {
        return attributeRules.size();
    }

    /**
     * Returns the n-th attribute Rule.
     * @param index an index starting from 0
     * @return the attribute Rule at rank 'index'
     */
    public Rule getAttributeRule(int index)
    {
        return (index < 0 || index >= attributeRules.size()) ? null
                : attributeRules.get(index);
    }

    
    /**
     * Returns the word sieve used for full-text indexing.
     * @return the current word sieve
     */
    public WordSieve getWordSieve()
    {
        return null;
    }

    /**
     * Defines the word sieve used for full-text indexing.
     * <p>
     * Note: the word sieve is unique because it is also used for parsing
     * full-text queries.
     * @param wordSieve in implementation of WordSieve. Must not be null.
     */
    public void setWordSieve(WordSieve wordSieve)
    {
    }

    /**
     * Returns the maximum length of text chunks that can be indexed in value
     * indexes (Attribute and Simple element content).
     * <p>The default value is 50
     * @return the current maximum length
     */
    public int getMaxStringLength()
    {
        return maxStringLength;
    }

    /**
     * Defines the maximum length of text chunks that can be indexed in value
     * indexes (Attribute and Simple element content).
     * Text chunks longer than this value are regarded as not meaningful 
     * therefore not indexed. But they can still be indexed in full-text index.
     * @param maxStringLength the maximum length desired
     */
    public void setMaxStringLength(int maxStringLength)
    {
        this.maxStringLength = maxStringLength;
    }

    /**
     * Returns the maximum length of words that can be indexed in the Full-text
     * index. Words longer than this value are regarded as not meaningful
     * therefore not indexed. The default value is 30.
     * @return the current maximum length
     */
    public int getMaxWordLength()
    {
        return maxWordLength;
    }

    /**
     * Defines the maximum length of words that can be indexed in the Full-text
     * index. Words longer than this value are regarded as not meaningful
     * therefore not indexed.
     * @param maxWordLength word length in characters
     */
    public void setMaxWordLength(int maxWordLength)
    {
        this.maxWordLength = maxWordLength;
    }

    /**
     * Returns the minimum length of words that can be indexed in the Full-text
     * index. Words shorter than this value are regarded as not meaningful
     * therefore not indexed. The default value is 2 (one-letter words are
     * discarded).
     * @return the current minimum length
     */
    public int getMinWordLength()
    {
        return minWordLength;
    }

    /**
     * Defines the minimum length of words that can be indexed in the Full-text
     * index. Words shorter than this value are regarded as not meaningful
     * therefore not indexed.
     * @param minWordLength word length in characters
     */
    public void setMinWordLength(int minWordLength)
    {
        this.minWordLength = minWordLength;
    }

    /**
     * Returns true if full-text indexing is globally enabled for elements.
     * <p>
     * By default, full-text indexing is globally enabled. It can be disabled
     * with setFulltextEnabled(), or by an attribute full-text='false' in an
     * indexing specification.
     * @return true if full-text indexing is globally enabled (the default)
     */
    public boolean isFulltextEnabled()
    {
        return fulltext;
    }

    /**
     * Enables or disables full-text indexing globally. Full-text indexing can
     * be enabled on specific XML elements by a rule with attribute 
     * full-text='true'.
     * @param fulltext true to enable full-text indexing globally
     */
    public void setFulltextEnabled(boolean fulltext)
    {
        this.fulltext = fulltext;
    }

    // ------------- convenience methods -------------------------------------
    
    /**
     * A Convenience method that adds a format-based DateSieve for a specific
     * element
     * @param format a date format as supported by {@link SimpleDateFormat} and
     *        FormatDateSieve.
     * @param locale optional locale used for creating the format (if null, 
     *        the default locale is used).
     * @param elementName required name of the element
     * @param context optional element context
     * @return the DateSieve created for the added rule, so that it can be
     * further modified. 
     */
    public DateSieve addDateSieve(String format, Locale locale, 
                                  QName elementName, QName[] context)
    {
        Rule rule = addElementRule(elementName, context, DATE_AND_STRING);
        FormatDateSieve fmt = new FormatDateSieve();
        fmt.setFormat(format, locale);
        rule.setSieve(fmt);
        return fmt;
    }
    
    /**
     * A Convenience method that adds a format-based DateSieve for a specific
     * attribute
     * @param format a date format as supported by {@link SimpleDateFormat} and
     *        FormatDateSieve.
     * @param locale optional locale used for creating the format (if null, the
     *        default locale is used).
     * @param attributeName required name of the attribute
     * @param context optional element context
     * @return the DateSieve created for the added rule.
     */
    public FormatDateSieve addAttrDateSieve(String format, Locale locale, 
                                            QName attributeName, QName[] context)
    {
        Rule rule = addAttributeRule(attributeName, context, DATE_AND_STRING);
        FormatDateSieve fmt = new FormatDateSieve();
        fmt.setFormat(format, locale);
        rule.setSieve(fmt);
        return fmt;
    }

    /**
     * A Convenience method that adds a format-based DateSieve for a specific
     * element
     * @param format a date format as supported by {@link SimpleDateFormat} and
     *        FormatDateSieve.
     * @param locale optional locale used for creating the format (if null, the
     *        default locale is used).
     * @param elementName required name of the element
     * @param context optional element context
     * @return the DateSieve created for the added rule.
     */
    public FormatNumberSieve addNumberSieve(String format, Locale locale, 
                                            QName elementName, QName[] context)
    {
        Rule rule = addElementRule(elementName, context, NUMERIC_AND_STRING);
        FormatNumberSieve fmt = new FormatNumberSieve();
        fmt.setFormat(format, locale);
        rule.setSieve(fmt);
        return fmt;
    }
    
    /**
     * A Convenience method that adds a format-based NumberSieve for a specific
     * attribute
     * @param format a date format as supported by {@link DecimalFormat} and
     *        FormatNumberSieve.
     * @param locale optional locale used for creating the format (if null, 
     * the default locale is used).
     * @param attributeName required name of the attribute
     * @param context optional element context
     * @return the DateSieve created for the added rule.
     */
    public FormatNumberSieve addAttrNumberSieve(String format, Locale locale, 
                                           QName attributeName, QName[] context)
    {
        Rule rule = addAttributeRule(attributeName, context, NUMERIC_AND_STRING);
        FormatNumberSieve fmt = new FormatNumberSieve();
        fmt.setFormat(format, locale);
        rule.setSieve(fmt);
        return fmt;
    }

    // ---------------------------------------------------------------

    public double convertNumber(Node node)
    {
        if(fastElemMap == null)
            compileForNodeMatching();
        try {
            String value = node.getStringValue();
            ArrayList<Rule> rules = getRules(node);
            if(rules != null)
                // rules are expanded
                for(int r = 0, rcnt = rules.size(); r < rcnt; r++) {
                    Rule rule = ruleAt(r, rules);
                    if((rule.indexingType == NUMERIC ||
                        rule.indexingType == NUMERIC_AND_STRING)
                        && contextMatch(rule.context, node)) {
                        double d = rule.sieve.convert(value);
                        if(d == d)
                            return d;
                    }
                }
        }
        catch (DataModelException justfail) { ; }
        return Double.NaN;
    }

    public double convertDate(Node node)
    {
        if(fastElemMap == null)
            compileForNodeMatching();
        try {
            String value = node.getStringValue();
            ArrayList<Rule> rules = getRules(node);
            if(rules != null)
                // rules are expanded
                for(int r = 0, rcnt = rules.size(); r < rcnt; r++) {
                    Rule rule = ruleAt(r, rules);
                    if((rule.indexingType == DATE ||
                        rule.indexingType == DATE_AND_STRING)
                        && contextMatch(rule.context, node)) {
                        double d = rule.sieve.convert(value);
                        if(d == d) // NaN
                            return d;
                    }
                }
        }
        catch (DataModelException justfail) { ; }
        return Double.NaN;
    }
    
    private ArrayList<Rule> getRules(Node node)
        throws DataModelException
    {
        QName name = node.getNodeName();
        HashMap<?, ?> map = node.isElement()? fastElemMap : fastAttrMap;
        ArrayList<Rule> rules = (ArrayList<Rule>) map.get(name);
        if(rules == null)
            rules = fastElemMap.get(DEF);
        return rules;
    }

    private boolean contextMatch(QName[] names, Node node)
    {
        if(names == null)
            return true;
        
        return false; // TODO
    }

    private void compileForNodeMatching()
    {
        fastElemMap = compileRuleSet(elementRules);
        fastAttrMap = compileRuleSet(attributeRules);
    }
    
    private HashMap<QName, ArrayList<Rule>> compileRuleSet(ArrayList<Rule> rules)
    {
        HashMap<QName, ArrayList<Rule>> map = new HashMap<QName, ArrayList<Rule>>();
        for (int r = 0, rcnt = rules.size(); r < rcnt; r++) {
            Rule rule = ruleAt(r, rules);
            if(rule.indexingType == STRING)
                continue;
            QName nam = (rule.name == null)? DEF : rule.name;
            ArrayList<Rule> rlist = map.get(nam);
            if(rlist == null)
                map.put(nam, rlist = new ArrayList<Rule>());
            int p = 0;
            for(; p < rlist.size() && ruleAt(p, rlist).precedes(rule); )
                ++p;
            rlist.add(p, rule);
        }
        // append default rules at end of all named rules:
        ArrayList<Rule> defRules = map.get(DEF);
        if(defRules != null)
            for (Iterator<QName> iter = map.keySet().iterator(); iter.hasNext();) {
                QName name = iter.next();
                if(name == DEF)
                    continue;
                ArrayList<Rule> list = map.get(name);
                list.addAll(defRules);
            }
        return map;
    }

    private Rule ruleAt(int pos, ArrayList<Rule> rules)
    {
        return rules.get(pos);
    }

    // ---------------------------------------------------------------
    
    /**
     * Indexing properties associated with an element or an attribute (for
     * advanced indexing).
     * <p>A Rule can have a context, which is a list of QNames of enclosing elements.
     * If a context is defined the Rule is applicable only if the enclosing
     * elements match the context. Default rules have a null context.
     */
    public static class Rule
    {        
        private QName name;
        private QName[] context; // may be null

        private NumberSieve sieve;
        private byte indexingType; // STRING, NUMERIC, DATE etc
        private byte fullText = INHERIT; // DISABLE_FULL_TEXT, ENABLE_FULL_TEXT

        /**
         * Constructs a Rule for an element or for an attribute.
         * @param name name of the element or attribute
         * @param context enclosing elements (optional)
         * @param indexingType possible values are STRING, NUMERIC, DATE, 
         * NUMERIC_AND_STRING, DATE_AND_STRING
         */
        public Rule(QName name, QName[] context, int indexingType)
        {
            super();
            this.name = name;
            this.context = context;
            this.indexingType = (byte) indexingType;
        }

        /**
         * Returns true if this rule is more specific than the rule in argument
         */
        public boolean precedes(Rule rule)
        {
            // assume same name
            return getContextDepth() >= rule.getContextDepth();
        }

        /**
         * Sets a default Sieve according to the indexing type.
         */
        public void setDefaultSieve()
        {
            if(indexingType == NUMERIC || indexingType == NUMERIC_AND_STRING)
                sieve = new FormatNumberSieve();
            else if(indexingType == DATE || indexingType == DATE_AND_STRING)
                sieve = new ISODateSieve();
        }

        /**
         * Returns the element context constraint.
         * @return a stack of element names from topmost to deepest; null if
         * no context constraint is defined
         */
        public QName[] getContext()
        {
            return context;
        }

        /**
         * Returns the element context depth.
         */
        public int getContextDepth()
        {
            return context == null? 0 : context.length;
        }
        
        /**
         * Defines an optional context of ancestors to be matched.
         * @param context an array of ancestor names, from outermost to
         *        innermost (parent).
         */
        public void setContext(QName[] context)
        {
            this.context = context;
        }

        /**
         * Returns TRUE, FALSE, or INHERIT if full-text indexing is applied to
         * the element matched by this rule.
         * @return a code defining how full-text indexing applies to an element
         */
        public final byte getFullText()
        {
            return fullText;
        }

        /**
         * Specifies whether full-text indexing is applied to the element
         * matched by this rule and to its descendants.
         * @param fulltext possible values are ENABLE_FULL_TEXT, 
         * DISABLE_FULL_TEXT, and INHERIT.
         */
        public void setFullText(byte fulltext)
        {
            this.fullText = fulltext;
        }

        /**
         * Returns the name of the element or attribute concerned by the rule.
         * @return the name of an element or attribute, or null for a default rule.
         */
        public QName getName()
        {
            return name;
        }

        /**
         * Defines the name of the element/attribute concerned by the rule.
         * @param name an element or attribute name
         */
        public void setName(QName name)
        {
            this.name = name;
        }

        /**
         * Returns the Numeric or Date Sieve associated with the rule.
         */
        public final NumberSieve getSieve()
        {
            return sieve;
        }

        /**
         * Defines the Numeric or Date Sieve associated with the rule.
         */
        public void setSieve(NumberSieve sieve)
        {
            this.sieve = sieve;
        }

        /**
         * Gets the indexes targeted by this rule (STRING, NUMERIC, DATE or
         * combination). Default is STRING.
         * @return the indexing type
         */
        public final int getIndexingType()
        {
            return indexingType;
        }

        /**
         * Sets the indexxes targeted by this rule.
         * 
         * @param indexingType STRING, NUMERIC, DATE or combination
         */
        public void setIndexingType(int indexingType)
        {
            this.indexingType = (byte) indexingType;
        }
    }
//\end

    /**
     * Abstraction of Sieves used for indexing XML data.
     */
    public interface Sieve
    {
        /**
         * Defines optional parameters for the sieve.
         * @param parameters an array of even size containing alternately a
         *        parameter name and a parameter value.
         * @throws DataModelException if the option is unknown or the value is
         *         invalid.
         */
        void setParameters(String[] parameters)
            throws DataModelException;

        /**
         * Returns parameters specified by setParameters().
         * @return an array of even size containing alternately a parameter
         *         name and a parameter value.
         */
        String[] getParameters();
    }

    /**
     * Pluggable text analyzer for custom full-text indexing and query.
     * <p>Kept for compatibility with Qizx v2.1.
     * <p>
     * To parse words, the sieve is first initialized with method
     * {@link #start} on a text chunk. Then the {@link #nextToken()}
     * method is called repeatedly until the last word is parsed.
     * @deprecated replaced by {@link TextTokenizer}
     */
    public interface WordSieve
        extends TextTokenizer
    {
        /**
         * Creates a carbon copy of this object.
         * @return a new copy of this object
         */
        WordSieve copy();
    }


    /**
     * Pluggable analyzer/converter of numeric values, for custom indexing.
     */
    public interface NumberSieve
        extends Sieve
    {
        /**
         * Attempts to convert the text fragment to a double value.
         * 
         * @param text an alleged numeric value in text form.
         * @return the converted value, or NaN if the conversion is not
         *         possible. Should raise no exception
         */
        double convert(String text);
    }


    /**
     * Pluggable analyzer/converter of date-time values, for custom indexing.
     */
    public interface DateSieve
        extends NumberSieve
    {
        /**
         * Attempts to convert the date or date-time contained in the text
         * fragment to a double value (in milliseconds from 1970-01-01 00:00:00
         * UTC).
         * 
         * @param text an alleged date-time value in text form.
         * @return the converted value, or NaN if the conversion is not
         *         possible.
         */
        double convert(String text);
    }
    

    private Rule parseRule(Node ruleNode)
        throws DataModelException
    {
        Node[] props = ruleNode.getAttributes();

        QName mainName = null;
        int indexingType = STRING;
        QName[] context = null;
        byte fulltext = (ruleNode.getNodeName() == ELEMENT_RULE)? INHERIT : DISABLE_FULL_TEXT;
        NumberSieve sieve = null;
        String[] parameters = new String[0];

        int propCount = (props == null) ? 0 : props.length;
        for (int i = 0; i < propCount; i++) {
            Node prop = props[i];
            QName name = prop.getNodeName();

            if (name == NAME) {
                mainName = convertName(prop.getStringValue(), ruleNode);
            }
            else if (name == CONTEXT) {
                StringTokenizer pnames =
                    new StringTokenizer(prop.getStringValue(), "/ ");
                for (; pnames.hasMoreTokens();) {
                    String pname = pnames.nextToken();
                    QName ctxName = convertName(pname, ruleNode);
                    if (context == null)
                        context = new QName[] { ctxName };
                    else {
                        QName[] old = context;
                        context = new QName[old.length + 1];
                        System.arraycopy(old, 0, context, 0, old.length);
                        context[old.length] = ctxName;
                    }
                }
            }
            else if (name == FULLTEXT) {
                fulltext = booleanProp(prop)? ENABLE_FULL_TEXT : DISABLE_FULL_TEXT;
            }
            else if (name == AS) {
                String value = prop.getStringValue();
                indexingType = -1;
                for (int t = INDEXING_TYPES.length; --t > 0;) {
                    if (INDEXING_TYPES[t].equalsIgnoreCase(value)) {
                        indexingType = INDEXING_CODES[t];
                        break;
                    }
                }
                if (indexingType < 0)
                    error("invalid indexing type: " + value);
            }
            else if (name == SIEVE) {
                sieve = sieveProp(prop);
            }
            else { // parameter passed to Sieve
                String[] old = parameters;
                int len = old.length;
                parameters = new String[len + 2];
                System.arraycopy(old, 0, parameters, 0, len);
                parameters[len] = name.getLocalPart();
                parameters[len + 1] = prop.getStringValue();
            }
        }

        Rule rule = new Rule(mainName, context, indexingType);

        // check the sieve class vs rule type:
        switch (indexingType) {
        case NUMERIC:
        case NUMERIC_AND_STRING:
            if (sieve == null) // no explicit class
                sieve = new FormatNumberSieve();
            break;
        case DATE_AND_STRING:
        case DATE:
            if (sieve == null) // no explicit class
                sieve = new ISODateSieve();
            else if (!(sieve instanceof DateSieve))
                error("sieve class should implement " + DateSieve.class);
            break;
        }
        rule.sieve = sieve;
        rule.setFullText(fulltext);
        // pass extra parameters to sieve instance:
        if (sieve != null)
            sieve.setParameters(parameters);
        return rule;
    }

    private QName convertName(String qname, Node context)
        throws DataModelException
    {
        if("*".equals(qname))   // wildcard
            return null;
        String localPart;
        String ns;
        try {
            String prefix = IQName.extractPrefix(qname);
            localPart = IQName.extractLocalName(qname);
            ns = context.getNamespaceUri(prefix);
            if (ns == null)
                if (prefix.length() == 0)
                    ns = "";
                else
                    error("undefined namespace prefix: " + prefix);
            if (ns != "") {
                String uri = namespaces.getNamespaceURI(prefix);
                if (uri == null)
                    namespaces.addMapping(prefix, ns);
                else if (!uri.equals(ns))
                    error("redefinition of namespace prefix " + " '" + prefix
                          + "' (not supported)");

            }
        }
        catch (RuntimeException e) {
            throw new DataModelException("Indexing parse error: " + e.getMessage());
        }
        return IQName.get(ns, localPart);
    }

    private boolean booleanProp(Node prop)
        throws DataModelException
    {
        String v = prop.getStringValue();
        if (v.equals("yes") || v.equals("true"))
            return true;
        if (v.equals("no") || v.equals("false"))
            return false;
        error("invalid boolean property: " + prop);
        return false; // dummy
    }

    private int integerProp(Node prop)
        throws DataModelException
    {
        String v = prop.getStringValue();
        try {
            return Integer.parseInt(v);
        }
        catch (NumberFormatException e) {
            error("invalid integer property: " + prop);
            return 0; // dummy
        }
    }

    // number or date sieve: if no package,
    // try DEFAULT_SIEVE_PACKAGE.X
    private NumberSieve sieveProp(Node prop)
        throws DataModelException
    {
        String classe = prop.getStringValue();
        if (classe == null || classe.length() == 0)
            throw new DataModelException("invalid sieve class");
        
        NumberSieve sieve = tryClass(classe);
        if(sieve != null)
            return sieve;
        
        // if no package, use default package
        int dot = classe.indexOf('.');
        if (dot < 0)
            classe = DEFAULT_SIEVE_PACKAGE + "." + classe;
        
        sieve = tryClass(classe);
        if(sieve == null)
            error("cannot instantiate Sieve class " + classe);
        return sieve;
    }

    private NumberSieve tryClass(String classe)
        throws DataModelException
    {
        try {
            return (NumberSieve) Beans.instantiate(getClass().getClassLoader(),
                                                   classe);
        }
        catch (Exception e) {
            return null;
        }
    }
    private void error(String message)
        throws DataModelException
    {
        if (ruleRank > 0)
            message = "[rule " + ruleRank + "] " + message;
        throw new DataModelException(message);
    }

}
