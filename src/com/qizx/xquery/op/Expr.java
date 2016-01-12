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

import com.qizx.util.basic.Check;
import com.qizx.xdm.IQName;
import com.qizx.xquery.XMLExprDisplay;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.fn.Position;
import com.qizx.xquery.fn.Last;

/**
 * Static methods and objects for expression rewriting and transforming.
 */
public abstract class Expr extends Expression
{
    // extended 'self' can be SelfStep with wildcard
    // or a local VarRef with null vardecl (produced by rewriting)
    public static boolean isDot(Expression e)
    {
        if (e instanceof SelfStep && ((SelfStep) e).nodeTest == null)
            return true;
        return (e instanceof VarReference.Local)
               && ((VarReference.Local) e).decl == null;
    }

    public static boolean pathLike(Expression e)
    {
        return (e instanceof PathExpr || e instanceof FilterExpr
                || e instanceof BasicStep || isDot(e));
    }

    public static boolean positionalPredicate(Expression pred)
    {
        Check.nonNull(pred, "predicate");

        //XQItemType predType = pred.getType().itemType();
        return (pred instanceof FilterExpr.PosTest)
               || (pred.findSubExpression(Position.Exec.class) != null
                   || pred.findSubExpression(Last.Exec.class) != null
                   || pred.findSubExpression(SelfStep.class) != null);
    }

    // Filters a source expr by a predicate.
    // If source is a path, adds predicate to last step.
    //
    static Expression addPredicate(Expression source, Expression predicate)
    {
        if (source instanceof NodeSortExpr) {
            NodeSortExpr sort = (NodeSortExpr) source;
            sort.expr = addPredicate(sort.expr, predicate);
        }
        else if (source instanceof PathExpr) {
            PathExpr path = (PathExpr) source;
            int rankLast = path.steps.length - 1;
            path.steps[rankLast] =
                addPredicate(path.steps[rankLast], predicate);
        }
        else {
            FilterExpr filt = new FilterExpr(source);
            filt.addPredicate(predicate);
            source = filt;
        }
        return source;
    }

    // scans an expression looking for local variables that are declared
    // in a 'for' clause. Fails if more than one.
    public static class ForVarCollector extends Visitor
    {
        LocalVariable found;

        LocalVariable found2; // detects failure

        public void reset()
        {
            found = found2 = null;
        }

        public boolean preTest(Expression focus)
        {
            if (!(focus instanceof VarReference.Local))
                return true;
            VarReference.Local local = (VarReference.Local) focus;
            Expression owner = local.decl.owner;
            if (!(owner instanceof ForClause))
                return true;
            // - System.err.println("**** var "+local.name+" decl par "+owner);
            if (found == null || found == local.decl) {
                found = local.decl;
                return true;
            }
            found2 = local.decl;
            return false;
        }
    }

    static boolean depends(Expression expr, LocalVariable var)
    {
        if (false) {
            System.err.println("=== depends on " + var.name + " ?");
            expr.dump(new XMLExprDisplay());
        }
        return !new VarDependence(var).visit(expr);
    }

    public static class VarDependence extends Visitor
    {
        LocalVariable var;

        VarDependence(LocalVariable var)
        {
            this.var = var;
        }

        public boolean preTest(Expression focus)
        {
            if (!(focus instanceof VarReference.Local))
                return true;
            VarReference.Local local = (VarReference.Local) focus;
            return (local.decl != var); // ie stop if found
        }
    }

    // detects a dependence to a variable such that it can be replaced by '.'
    // if the
    // expression becomes a filter predicate. It means that the var reference
    // cannot
    // itself be inside a predicate.
    public static class DottingVarFinder extends Visitor
    {
        LocalVariable var;

        boolean OK = false;

        DottingVarFinder(LocalVariable var)
        {
            this.var = var;
        }

        public boolean preTest(Expression focus)
        {
            if (!(focus instanceof VarReference.Local))
                return true;
            VarReference.Local local = (VarReference.Local) focus;
            if (local.decl != var)
                return true;
            // check the context:
            for (int p = ctxPtr; --p >= 0;)
                if (context[p] instanceof FilterExpr) {
                    // inside a predicate (bug? we dont know which child we
                    // are) TODO
                    return OK = false;
                }
            return OK = true;
        }
    }

    public static class VarReplacer extends Visitor
    {
        LocalVariable replaced, replacing;

        VarReplacer(LocalVariable replaced, LocalVariable replacing)
        {
            this.replaced = replaced;
            this.replacing = replacing;
        }

        public boolean preTest(Expression focus)
        {
            if (!(focus instanceof VarReference.Local))
                return true;
            VarReference.Local local = (VarReference.Local) focus;
            if (local.decl == replaced) {
                local.decl = replacing;
                if (replacing != null)
                    local.name = replacing.name;
                else // for debug
                    local.name = IQName.get(".");
            }
            return true;
        }
    }
}
