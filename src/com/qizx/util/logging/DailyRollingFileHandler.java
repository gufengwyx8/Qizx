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
package com.qizx.util.logging;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Implementation of {@link Handler} that changes of log file each day.
 */
public class DailyRollingFileHandler extends StreamHandler
{
    private static long PERIOD = 24 * 3600000L;  // 24 h 

    private SimpleDateFormat datePattern =
        new SimpleDateFormat("-yyyy-MM-dd");

    protected String pathPattern;
    protected long currentDay;
    
    
    /**
     * Builds a DailyRollingFileHandler writing to 
     * @param pathPattern path of the current output file
     */
    public DailyRollingFileHandler(String pathPattern)
    {
        this.pathPattern = pathPattern;
        setFormatter(new LogFormatter());
    }
    
    @Override
    public void publish(LogRecord record)
    {
        long time = record.getMillis();
        long day = getDay(time);
        if(day != currentDay) {
            try {
                startFile(time);
            }
            catch (Exception e) {
                System.err.println("*** Logger: " + e);
            }
            currentDay = day;
        }
        super.publish(record);
        flush();
    }

    // start proper file for record
    private void startFile(long time)
        throws SecurityException, FileNotFoundException
    {
        File current = new File(pathPattern);
        if(current.exists()) {
            long modif = current.lastModified();
            int modifDay = getDay(modif);
            if(getDay(time) != modifDay)
                rollTo(modifDay);
        }
        setOutputStream(new FileOutputStream(current, true));
    }

    private void rollTo(int oldDay)
    {
        File current = new File(pathPattern);
        File old = new File(fileName(oldDay * PERIOD));
        current.renameTo(old);
        
         // TODO delete logs older than D days
    }

    private String fileName(long time)
    {
        return pathPattern + datePattern.format(new Date(time));
    }

    private int getDay(long time)
    {
        return (int) (time / PERIOD);
    }
}
