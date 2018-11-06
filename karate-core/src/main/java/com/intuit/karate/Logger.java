/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * derived from org.slf4j.simple.SimpleLogger
 *
 * @author pthomas3
 */
public class Logger {
    
    private final org.slf4j.Logger LOGGER;
    
    // not static, has to be per thread
    private final DateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");

    private LogAppender logAppender;

    public void setLogAppender(LogAppender logAppender) {
        this.logAppender = logAppender;
    }        

    public Logger() {
        LOGGER = LoggerFactory.getLogger("com.intuit.karate");
    }

    public void trace(String format, Object... arguments) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format, arguments);
        }
        // we never do trace in html logs
    }

    public void debug(String format, Object... arguments) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format, arguments);
        }
        formatAndAppend(format, arguments);
    }

    public void info(String format, Object... arguments) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format, arguments);
        }
        formatAndAppend(format, arguments);
    }

    public void warn(String format, Object... arguments) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(format, arguments);
        }
        formatAndAppend(format, arguments);
    }

    public void error(String format, Object... arguments) {
        LOGGER.error(format, arguments);
        formatAndAppend(format, arguments);
    }

    private String getFormattedDate() {
        Date now = new Date();
        String dateText;
        dateText = dateFormatter.format(now);
        return dateText;
    }

    private void formatAndAppend(String format, Object... arguments) {
        if (logAppender == null) {
            return;
        }
        FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
        append(tp.getMessage());
    }

    private void append(String message) {
        StringBuilder buf = new StringBuilder();
        buf.append(getFormattedDate()).append(' ').append(message).append('\n');
        logAppender.append(buf.toString());
    }

}
