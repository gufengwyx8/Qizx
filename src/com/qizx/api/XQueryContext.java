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

import java.util.Date;
import java.util.TimeZone;

/**
 * Definition of the XQuery Context used for compiling and executing
 * Expressions. This serves both as Static and Dynamic context (in the sense of
 * XQuery).
 * <p>
 * A XML {@link Library} provides a default XQueryContext which can be
 * modified. This default context should be suitable for most applications,
 * with perhaps only a few properties to define, such as base-uri.
 * <p>
 * Before an {@link Expression} is compiled, it inherits the context of the
 * Library from which the expression is created.
 * <p>
 * After an Expression is compiled, its XQueryContext reflects declarations
 * made in the expression itself. The XQuery context can be inspected and
 * runtime properties modified (mainly the default collation, implicit timezone,
 * current date).
 */
public interface XQueryContext
{
    /**
     * Value returned by getOrderingMode(): indicates that by default result
     * sequences must be ordered.
     */
    int ORDERED = 11;

    /**
     * Value returned by getOrderingMode(): indicates that by default result
     * sequences are not required to be ordered.
     */
    int UNORDERED = 10;

    /**
     * Value used for properties BoundarySpacePolicy, NamespacePreserveMode,
     * ConstructionMode, meaning that - respectively - boundary space,
     * namespaces and schema types should be preserved.
     */
    int PRESERVE = 21;

    /**
     * Value used for properties BoundarySpacePolicy, NamespacePreserveMode,
     * ConstructionMode, meaning that - respectively - boundary space,
     * namespaces and schema types should not be preserved.
     */
    int NO_PRESERVE = 20;

    /**
     * Value used for property NamespaceInheritMode, meaning that namespaces
     * should be inherited in a node copy operation.
     */
    int INHERIT = 31;

    /**
     * Value used for property NamespaceInheritMode, meaning that namespaces
     * should not be inherited in a node copy operation.
     */
    int NO_INHERIT = 30;

    /**
     * Gets the supported version of XQuery.
     * @return a String representing the XQuery version value
     */
    String getXQueryVersion();

    /**
     * Gets the default order for empty sequences in 'order by' clauses.
     * @return true if the empty sequence or NaN are considered greatest.
     */
    boolean getDefaultOrderEmptyGreatest();

    /**
     * Sets the default order for empty sequences in 'order by' clauses.
     * @param emptyGreatest true if the empty sequence or NaN are considered
     *        greatest.
     */
    void setDefaultOrderEmptyGreatest(boolean emptyGreatest);

    /**
     * Gets the default inherit part of the copy-namespaces mode.
     * @return the inherit mode, one of the two values INHERIT or NO_INHERIT
     */
    int getNamespaceInheritMode();

    /**
     * Sets the default inherit part of the copy-namespaces mode.
     * @param namespaceInheritMode the inherit mode to set, one of the two
     *        values INHERIT or NO_INHERIT
     */
    void setNamespaceInheritMode(int namespaceInheritMode);

    /**
     * Gets the default preserve part of the copy-namespaces mode.
     * 
     * @return the default namespace preserve mode, one of the two values
     *         PRESERVE or NO_PRESERVE.
     */
    int getNamespacePreserveMode();

    /**
     * Sets the default preserve part of the copy-namespaces mode.
     * 
     * @param namespacePreserveMode the namespace preserve mode to set, one of
     *        the two values PRESERVE or NO_PRESERVE.
     */
    void setNamespacePreserveMode(int namespacePreserveMode);

    /**
     * Gets the default boundary space policy for element constructors.
     * 
     * @return the default boundary space policy, one of the two values
     *         PRESERVE or NO_PRESERVE.
     */
    int getBoundarySpacePolicy();

    /**
     * Sets the default boundary space policy for element constructors.
     * 
     * @param boundarySpacePolicy the boundary space policy to set, one of the
     *        two values PRESERVE or NO_PRESERVE.
     */
    void setBoundarySpacePolicy(int boundarySpacePolicy);

    /**
     * Gets the construction mode defined in the static context.
     * 
     * @return the default construction mode to set, one of the two values
     *         PRESERVE or NO_PRESERVE.
     */
    int getConstructionMode();

    /**
     * Sets the construction mode defined in the static context.
     * 
     * @param constructionMode the construction mode to set, one of the two
     *        values PRESERVE or NO_PRESERVE.
     */
    void setConstructionMode(int constructionMode);

    /**
     * Gets the URI of the default collation defined in the static context. By
     * default it is the Unicode codepoints collation.
     * 
     * @return the defaultCollation
     */
    String getDefaultCollation();

    /**
     * Sets the URI of the default collation defined in the static context.
     * 
     * @param defaultCollation the URI of the default collation to set
     * @throws DataModelException if the collation cannot be resolved
     */
    void setDefaultCollation(String defaultCollation)
        throws DataModelException;

    /**
     * Gets the URI of the default element namespace. By default it is the
     * blank namespace.
     * 
     * @return the default element namespace
     */
    String getDefaultElementNamespace();

    /**
     * Sets the URI of the default element namespace.
     * 
     * @param defaultElementNamespace default element namespace to set
     */
    void setDefaultElementNamespace(String defaultElementNamespace);

    /**
     * Gets the URI of the default function namespace. By default it
     * corresponds to predefined functions (prefix fn:).
     * 
     * @return the URI of the default function namespace, if set, else the
     *         empty string.
     */
    String getDefaultFunctionNamespace();

    /**
     * Sets the URI of the default function namespace.
     * 
     * @param defaultFunctionNamespace the default Function Namespace to set,
     *        may not be null
     */
    void setDefaultFunctionNamespace(String defaultFunctionNamespace);

    /**
     * Gets the default ordering mode for returned sequences.
     * 
     * @return the orderingMode, one of the two values ORDERED or UNORDERED
     */
    int getOrderingMode();

    /**
     * Sets the default ordering mode for returned sequences.
     * 
     * @param orderingMode the ordering mode to set, one of the two values
     *        ORDERED or UNORDERED
     */
    void setOrderingMode(int orderingMode);

    /**
     * Gets the base URI used to resolve relative URIs in expressions.
     * 
     * @return the base URI, if set, else the empty string. Cannot be null
     */
    String getBaseURI();

    /**
     * Sets the base URI used to resolve relative URIs in expressions.
     * 
     * @param uri the base URI, or the empty string. May not be null
     */
    void setBaseURI(String uri);

    /**
     * Returns the prefixes of in-scope namespace definitions. If used on the
     * context of a compiled Expression,
     * 
     * @return String array containing the namespace prefixes. Cannot be null.
     */
    String[] getInScopePrefixes();

    /**
     * Adds a predefined namespace declaration.
     * <p>
     * If a namespace with the same prefix already exists, it is replaced.
     * 
     * @param prefix namespace prefix to be declared
     * @param namespaceURI URi of the namespace to be declared
     */
    void declarePrefix(String prefix, String namespaceURI);

    /**
     * Retrieves the namespace URI associated with a prefix.
     * @param prefix a namespace prefix
     * @return the namespaceURI associated with the prefix, or null if the
     *         prefix is undefined.
     */
    String getNamespaceURI(String prefix);

    /**
     * Gets the list of global variables declared in the static context.
     * <p>
     * In the context of a compiled Expression, the variables are those declared
     * in the expression. In the context of a Library session or XQuerySession,
     * the variables can only have been predefined through the method
     * {@link #declareVariable(QName, SequenceType)}.
     * @return array of QNames of the variables. An empty array is returned when
     *         there are no variables.
     */
    QName[] getVariableNames();

    /**
     * Returns the type associated with a declared variable.
     * @param variableName qualified name of the variable
     * @return the type declared for the variable. If the variable is not
     *         declared, returns null.
     */
    SequenceType getVariableType(QName variableName);

    /**
     * Adds a predefined variable declaration.
     * 
     * @param variableName qualified name of the variable
     * @param variableType optional type (may be null) declared for the
     *        variable
     */
    void declareVariable(QName variableName, SequenceType variableType);

    /**
     * Gets the list of options declared in the static context of a query.
     * <p>
     * In the context of a compiled Expression, the options are those declared
     * in the prolog. 
     * @return array of QNames of the options. An empty array is returned when
     *         there are no options.
     */
    QName[] getOptionNames();

    /**
     * Returns the string literal declared as value of the specified option.
     * @param optionName name of the option
     * @return a string literal value
     */
    String getOptionValue(QName optionName);
    
    /**
     * Defines the implicit time-zone in the execution context.
     * @param implicitTimeZone a TimeZone to be used as implicit time-zone
     */
    void setImplicitTimeZone(TimeZone implicitTimeZone);

    /**
     * Returns the current implicit time-zone in the execution context. By
     * default it is the system time-zone.
     * @return the current implicit time-zone
     */
    TimeZone getImplicitTimeZone();

    /**
     * Returns true if Strict Typing is enabled in this context.
     * 
     * @return true if Strict Typing is enabled
     * @see #setStrictTyping
     */
    boolean getStrictTyping();

    /**
     * Sets the Strict Typing flag.
     * <p>
     * If enabled (not by default), the XQuery compiler will treat as an error
     * any weakly typed expression. This implies that nearly all variables or
     * function parameters need to be declared with a type, and all user
     * functions should have a declared return type. Since Qizx does not
     * support Schema import, some expressions might in addition have to be
     * typed through the use of treat as, casts and functions like
     * fn:exactly-one, fn:one-or-more etc.
     * @param strictTyping true to enforce Strict Typing
     * @see #getStrictTyping
     */
    void setStrictTyping(boolean strictTyping);

    /**
     * Returns true if a strict compliance with XQuery Specifications is
     * enforced (not allowing extensions).
     * @return true if strict compliance is enforced
     * @see #setStrictCompliance
     */
    boolean getStrictCompliance();

    /**
     * Sets the strict compliance flag with XQuery Specifications.
     * <p>
     * If not enabled (the default), the following extensions are available:
     * <li>Casting numeric values from/to xs:date, xs:time, xs:dateTime,
     * xs:dayTimeDuration, xs:yearMonthDuration.
     * <li>Allow concat() to have a single argument which is a sequence.
     * @param strictCompliance true to enforce strict compliance
     */
    void setStrictCompliance(boolean strictCompliance);

    /**
     * Sets a specific date/time for use in query evaluation, instead of the
     * normal value (the system time at start of evaluation).
     * @param forcedDate a date/time, or null to use again the system time.
     */
    void setCurrentDate(Date forcedDate);

    /**
     * Gets the specific date/time set by setCurrentDate().
     * @return the value set by setCurrentDate(), or null by default (use
     *         system time)
     */
    Date getCurrentDate();
}
