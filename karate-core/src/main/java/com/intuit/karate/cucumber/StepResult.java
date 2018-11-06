/*
 * The MIT License
 *
 * Copyright 2017 Intuit Inc.
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
package com.intuit.karate.cucumber;

import gherkin.formatter.model.Result;
import gherkin.formatter.model.Step;

/**
 *
 * @author pthomas3
 */
public class StepResult {
    
    public static final String ABORTED = "aborted";
    public static final Result PASSED = new Result(Result.PASSED, null, null);
    public static final Object DUMMY_OBJECT = new Object();
    
    private final Step step;
    private final Result result;
    private final boolean pass;
    private final boolean abort;

    public StepResult(Step step, Result result) {
        this.step = step;
        this.result = result;
        pass = result.getError() == null;
        abort = result.getStatus().equals(ABORTED);
    }

    public Step getStep() {
        return step;
    }

    public Result getResult() {
        return result;
    }        

    public Throwable getError() {
        return result.getError();
    }    

    public boolean isPass() {
        return pass;
    }    

    public boolean isAbort() {
        return abort;
    }
        
}
