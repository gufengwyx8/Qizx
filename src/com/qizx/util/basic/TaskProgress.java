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

import com.qizx.util.ProgressHandler;

/**
 * Helps managing progress handlers.
 * Represents a part of a more complex task,
 */
public class TaskProgress
{
    public String name;
    public TaskProgress parent;
    public double share;   // in parent
    public double done;    // fraction done so far

    public double size;    // of self: for use by children in general
    protected ProgressHandler handler;
    
    public TaskProgress(String name, TaskProgress parent) {
        this(name, parent, 1);
    }
    
    public TaskProgress(String name, TaskProgress parent, double share) {
        this.name = name;
        this.parent = parent;
        this.share = share;
    }
    
    /**
     * Returns the completed fraction of the uppermost task, given the 
     * estimate completed fraction of this task.
     * @param done estimate completed fraction of this task
     */
    public double globalDone(double done)
    {
        double fdone = done * share;
        for(TaskProgress t = parent; t != null; t = t.parent) {
            fdone *= t.share;
            if(t.handler != null) // normally toplevel
                t.handler.progressDone(fdone + t.done);
        }
        return fdone;
    }
    
    /**
     * Declares a task completed. Updates the 'done' part of the parent,
     * iteratively propagated to ancestors.
     * TO CALL ONLY on leaf tasks!
     */
    public void complete()
    {
        double propagated = share;
        for(TaskProgress t = parent; t != null; t = t.parent) {
            t.done += propagated;
            propagated *= t.share;
        }
    }

    public void setProgressHandler(ProgressHandler handler)
    {
        this.handler = handler; 
    }
}
