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
package com.qizx.xquery.op;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.BooleanValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.impl.EmptyException;

import java.lang.reflect.Array;
import java.math.BigDecimal;

/**
 * Superclass of all compiled language constructs: operators, function calls.
 */
public abstract class Expression
{
    public static final String ERRC_BADTYPE = "XPTY0004";

    /**
     * Static optimization of Path: indicates whether all the nodes generated
     * by a path step are within the subtree of the origin.
     */
    public final static int WITHIN_SUBTREE = 1;

    /**
     * Static optimization of Paths: indicates whether all the nodes generated
     * by a path step belong to the origin (true for attributes or namespace
     * nodes).
     */
    public final static int WITHIN_NODE = 2;

    /**
     * Static optimization of Paths: indicates whether all the nodes generated
     * by a path step are at same tree depth.
     */
    public final static int SAME_DEPTH = 4;

    /**
     * Static optimization of Paths: indicates whether all the nodes generated
     * by a path step are in document order and not duplicated.
     */
    public final static int DOCUMENT_ORDER = 8;

    /**
     * Static optimization: indicates that this sequence needs not be ordered.
     * Used in static context.
     */
    public final static int UNORDERED = 0x10;

    /**
     * Static optimization: indicates a constant or literal expression.
     */
    public final static int CONSTANT = 0x20;

    public final static int NUMERIC = 0x40;

    /**
     * XQuery Update: indicates an Updating expression
     */
    public final static int UPDATING = 0x80;
    
    private static final BigDecimal ZERO = BigDecimal.valueOf(0);

    
    // Compilation Module where the expression is defined.
    public ModuleContext module;

    // Character offset in module source code.
    public int offset;

    // Statically inferred type.
    protected XQType type = XQType.ANY;
    


    public void dump(ExprDisplay d)
    {
        d.header(this);
        Expression kid;
        for(int r = 0; (kid = child(r)) != null; ++r)
            d.child(kid);
        // general contract: d.end() is called by ExprDisplay
    }

    public Expression atSamePlaceAs(Expression origin)
    {
        this.module = origin.module;
        offset = origin.offset;
        return this;
    }

    public static Expression[] addExpr(Expression[] exprs, Expression e)
    {
        Expression[] nexprs =
            (Expression[]) Array.newInstance(exprs.getClass().getComponentType(),
                                             exprs.length + 1);
        System.arraycopy(exprs, 0, nexprs, 0, exprs.length);
        nexprs[exprs.length] = e;
        return nexprs;
    }

    // ---- for SAX parsing (XSLT, XQueryX) :

    public Expression addChild(Expression child, QName name)
    {
        System.err.println("addChild not implemented in " + getClass());
        return null;
    }

    public void addAttribute(QName name, String value)
    {
        System.err.println("addAttribute no implemented in " + getClass());
    }

    public void putCharacters(char ch[], int start, int length)
    {
        int c = length;
        for (; --c >= 0;)
            if (!XMLUtil.isXMLSpace(ch[start + c]))
                break;
        if (c < 0)
            return;
        System.err.println("putCharacters no implemented in " + getClass()
                           + " |" + new String(ch, start, length) + "|");
    }

    /**
     * Static analysis. Checks sub-expressions and potentially replaces this
     * expression by an other construct.
     */
    public Expression staticCheck(ModuleContext context, int flags)
    {
        return this;
    }

    /**
     * Returns the (inferred) static type of the expression.
     *  Valid only after Static Analysis.
     */
    public XQType getType()
    {
        return type;
    }

    public void setType(XQType type)
    {
        this.type = type;
    }

    public static final int combinedTypes(int t1, int t2)
    {
        return (t1 << 4) + t2;
    }

    public static final int combinedTypes(XQItemType t1, XQItemType t2)
    {
        return combinedTypes(t1.quickCode(), t2.quickCode());
    }

    public Expression transfer(Function.Call newExpr, Expression[] operands)
    {
        newExpr.args = operands;
        newExpr.module = this.module;
        newExpr.offset = this.offset;
        return newExpr;
    }


    public int getFlags()
    {
        return 0;
    }

    public boolean isConstant()
    {
        return (getFlags() & CONSTANT) != 0;
    }

    public boolean isUpdating() {
        return (getFlags() & UPDATING) != 0;
    }
    
    public boolean isVacuous() { // override in typeswitch, if, error, sequ
        return false;
    }
    
    protected XQItem checkFocus(Focus focus, EvalContext context)
        throws EvaluationException
    {
        if (focus == null || focus.currentItem() == null) {
            context.error("XPDY0002", this, "no context item");
            return null; // dummy
        }
        return focus.currentItem();
    }

    /**
     * Abstract Expression visitor. Manages a context stack so that the
     * ancestors of an expression can be checked.
     */
    public static abstract class Visitor
    {
        Expression[] context = new Expression[16];

        int ctxPtr = 0;

        abstract public boolean preTest(Expression expr);

        public boolean postTest(Expression expr)
        {
            return true;
        }

        public boolean visit(Expression expr)
        {
            if (!preTest(expr))
                return false;
            // add to stack
            if (ctxPtr >= context.length) {
                Expression[] old = context;
                context = new Expression[old.length * 2];
                System.arraycopy(old, 0, context, 0, old.length);
            }
            context[ctxPtr++] = expr;
            Expression kid = null;
            for (int rank = 0; (kid = expr.child(rank)) != null; rank++)
                if (!visit(kid)) {
                    --ctxPtr;
                    return false;
                }
            --ctxPtr;
            return postTest(expr);
        }
    }

    /**
     * Returns child of rank N (starting at 0) or null. Used by Visitors.
     */
    public abstract Expression child(int rank);

    /**
     * Finds the first subexpression matching the given class.
     */
    public Expression findSubExpression(Class classe)
    {
        Finder f = new Finder(classe);
        f.visit(this);
        return f.found;
    }

    private static class Finder extends Visitor
    {
        Class searched;

        Expression found = null;

        Finder(Class searched)
        {
            this.searched = searched;
        }

        public boolean preTest(Expression focus)
        {
            if (focus.getClass().isAssignableFrom(searched)) {
                found = focus;
                return false;
            }
            return true;
        }
    }

    protected XQValue errorEmpty(EvalContext context)
        throws EvaluationException
    {
        return context.error("XPTY0004", this, XQType.ERR_EMPTY_UNEXPECTED);
    }

    protected XQValue errorMoreThanOne(EvalContext context)
        throws EvaluationException
    {
        return context.error("XPTY0004", this, XQType.ERR_TOO_MANY);
    }

    protected XQValue dmError(EvalContext context, DataModelException e)
        throws EvaluationException
    {
        context.error(e.getErrorCode(), this,
                      "Data model error: " + e.getMessage(), e);
        return null;
    }


    /**
     * Computes the result in the specified evaluation context. The current
     * context item (denoted by '.') is specified by 'dot'.
     */
    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        throw new EvaluationException(getClass()
                                      + " evaluation not implemented");
    }

    /**
     * Evaluates directly as one single boolean. Can be optimised by concrete
     * boolean expressions.
     */
    public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                errorEmpty(context);
            boolean value = v.getBoolean();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (EmptyException e) {
            errorEmpty(context);
            return false;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return false;
        }
    }

    /**
     * Evaluates the effective boolean value. Can be optimised by concrete
     * expressions.
     */
    public boolean evalEffectiveBooleanValue(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                return false;
            XQItem item = v.getItem();
            if (item.isNode())
                return true; // any length
            int type = item.getItemType().quickCode();
            boolean ebv = false;
            switch (type) {
            case XQType.QT_BOOL:
                ebv = item.getBoolean();
                break;
            case XQType.QT_STRING:
            case XQType.QT_ANYURI: // why? wanted by tests but not in specs
            case XQType.QT_UNTYPED: // untypedAtomic which is not a node
                ebv = !"".equals(item.getString());
                break;
            case XQType.QT_DOUBLE:
            case XQType.QT_FLOAT:
                double d = item.getDouble();
                ebv = (d == d && d != 0);
                break;
            case XQType.QT_INT:
                ebv = (item.getInteger() != 0);
                break;
            case XQType.QT_DEC:
                ebv = (item.getDecimal().compareTo(ZERO) != 0);
                break;
            default:
                context.error(Function.ERR_ARGTYPE, this, "invalid type for condition");
            }
            if (v.next()) // 2 or more items is now an error
                context.error(Function.ERR_ARGTYPE, this, "several items in condition");
            return ebv;
        }
        catch(EmptyException ee) {
            return false;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return false;
        }
    }

    /**
     * Evaluates as numeric value or effective boolean value. Returns a numeric
     * or boolean item. Used by predicates.
     */
    public XQItem evalForPredicate(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                return null; // ie false
            XQItem item = v.getItem();
            if (item.isNode())
                return BooleanValue.TRUE; // any length
            int type = item.getItemType().quickCode();
            switch (type) {
            case XQType.QT_BOOL:
                break; // itself
            case XQType.QT_STRING:
            case XQType.QT_ANYURI: // why? wanted by tests but not in specs
            case XQType.QT_UNTYPED: // untypedAtomic which is not a node
                item =
                    ("".equals(item.getString())) ? null : BooleanValue.TRUE;
                break;
            case XQType.QT_DOUBLE:
            case XQType.QT_FLOAT:
            case XQType.QT_INT:
            case XQType.QT_DEC:
                break; // itself
            default:
                context.error(Function.ERR_ARGTYPE, this, "invalid type for condition");
            }
            if (v.next()) // 2 or more items is now an error
                context.error(Function.ERR_ARGTYPE, this, "several items in condition");
            return item;
        }
        catch (EmptyException e) {  // amazing: bug fixed after 5 years!
            return null;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return null;
        }
    }

    /**
     * Evaluates directly as one mandatory integer. Can be optimised by
     * concrete Int expressions.
     */
    public long evalAsInteger(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                errorEmpty(context);
            int type = v.getItemType().quickCode();
            if (type != XQType.QT_INT && type != XQType.QT_UNTYPED)
                throw new XQTypeException(ERRC_BADTYPE,
                                          "expecting integer value");
            long value = v.getInteger();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (EmptyException e) {
            errorEmpty(context);
            return 0;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return 0;
        }
    }

    /**
     * Evaluates directly as an optional integer. If empty, raises an
     * EmptyException that can be caught by enclosing expressions. An enclosing
     * expression that returns () if this argument is () juste doesn't need to
     * care about that. An enclosing expression that needs to care about an
     * empty argument has to catch the expression. Note: the runtime cost of
     * raising en exception is quite acceptable because the exception is
     * statically created, and there are probably few stack frames to pop.
     */
    public long evalAsOptInteger(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                throw EmptyException.instance();
            int type = v.getItemType().quickCode();
            if (type != XQType.QT_INT && type != XQType.QT_UNTYPED)
                throw new XQTypeException(ERRC_BADTYPE,
                                          "expecting integer value");
            long value = v.getInteger();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return 0;
        }
    }

    /**
     * Evaluates directly as one float. Can be optimised by actual Float
     * expressions.
     */
    public float evalAsFloat(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                errorEmpty(context);
            float value = v.getFloat();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (EmptyException e) {
            errorEmpty(context);
            return 0;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return 0;
        }
    }

    /**
     * Evaluates directly as an optional float. If empty, raises an
     * EmptyException that can be caught by enclosing expressions. (See
     * comments of {@link #evalAsOptInteger})
     */
    public float evalAsOptFloat(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                throw EmptyException.instance();
            float value = v.getFloat();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return 0;
        }
    }

    /**
     * Evaluates directly as one double. Can be optimised by actual Double
     * expressions.
     */
    public double evalAsDouble(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                errorEmpty(context);
            double value = v.getDouble();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (EmptyException e) {
            errorEmpty(context);
            return 0;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return 0;
        }
    }

    /**
     * Evaluates directly as an optional double. If empty, raises an
     * EmptyException that can be caught by enclosing expressions. (See
     * comments of {@link #evalAsOptInteger})
     */
    public double evalAsOptDouble(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                throw EmptyException.instance();
            double value = v.getDouble();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return 0;
        }
    }

    /**
     * Evaluates directly as one string. Can be optimised by actual String
     * expressions.
     */
    public String evalAsString(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                errorEmpty(context);
            // new: check type!
            int type = v.getItemType().quickCode();
            if (type != XQType.QT_STRING && type != XQType.QT_UNTYPED)
                throw new XQTypeException(ERRC_BADTYPE,
                                          "expecting string value");
            String value = v.getString();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(e.getErrorCode(), this, e.getMessage());
            return null;
        }
    }

    /**
     * Evaluates directly as one string, or null if empty (mechanism simpler
     * than for numerics). Can be optimised by actual String expressions.
     */
    public String evalAsOptString(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                return null;
            int type = v.getItemType().quickCode();
            if (type != XQType.QT_STRING && type != XQType.QT_UNTYPED
                    && type != XQType.QT_ANYURI)
                throw new XQTypeException(ERRC_BADTYPE,
                                          "expecting string value");
            String value = v.getString();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(e.getErrorCode(), this, e.getMessage());
            return null;
        }
    }

    /**
     * Evaluates directly as one Node. Can be optimised by actual Node
     * expressions.
     */
    public BasicNode evalAsNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                errorEmpty(context);
            BasicNode value = v.basicNode();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return null;
        }
    }

    /**
     * Evaluates directly as one Node, or null if empty.
     */
    public BasicNode evalAsOptNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                return null;
            BasicNode value = v.basicNode();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return null;
        }
    }

    /**
     * Evaluates directly as one single Item.
     */
    public XQItem evalAsItem(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                errorEmpty(context);
            XQItem value = v.getItem();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return null;
        }
    }

    /**
     * Evaluates directly as one Item, or null if empty.
     */
    public XQItem evalAsOptItem(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = eval(focus, context);
            if (!v.next())
                return null;
            XQItem value = v.getItem();
            if (v.next())
                errorMoreThanOne(context);
            return value;
        }
        catch (XQTypeException e) {
            context.error(this, e);
            return null;
        }
    }

    /**
     * Evaluation of trees directly into a serial output, without actual
     * construction of Nodes. This working mode allows the processing of large
     * documents with a low memory footprint (assuming that the source document
     * is a FONIDocument).
     */
    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        XQValue seq = eval(focus, context);
        try {
            for (; seq.next();)
                if (seq.isNode()) {
                    BasicNode srcNode = seq.basicNode();
                    // traversal copies source node to output
                    // namespace copy according to context flags:
                    output.putNodeCopy(srcNode,
                                       context.getStaticContext().getCopyNSMode());
                }
                else {
                    output.putAtomText(seq.getString());
                }
        }
        catch (QizxException e) {
            context.error(e.getErrorCode(), this, "error in constructor: "
                                                  + e.getMessage());
        }
    }
}
