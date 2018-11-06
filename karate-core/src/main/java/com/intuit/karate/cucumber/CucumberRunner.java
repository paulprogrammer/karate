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

import com.intuit.karate.CallContext;
import com.intuit.karate.FileUtils;
import com.intuit.karate.ScriptValueMap;
import com.intuit.karate.filter.TagFilter;
import com.intuit.karate.filter.TagFilterException;
import cucumber.runtime.model.CucumberFeature;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cucumber.runtime.model.CucumberTagStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class CucumberRunner {

    private static final Logger logger = LoggerFactory.getLogger(CucumberRunner.class);

    public static KarateStats parallel(Class clazz, int threadCount) {
        return parallel(clazz, threadCount, null);
    }

    public static KarateStats parallel(Class clazz, int threadCount, String reportDir) {
        KarateRuntimeOptions kro = new KarateRuntimeOptions(clazz);
        List<KarateFeature> karateFeatures = KarateFeature.loadFeatures(kro);
        return parallel(karateFeatures, threadCount, reportDir);
    }

    /**
     * 
     * @param tags - use a single list item (comma delimited) for OR, use multiple items for AND
     * @param paths - can be paths to feature files or directories that will be scanned
     * @param threadCount - number of threads for parallel runner
     * @param reportDir - can be null, and defaults to "target/surefire-reports"
     * @return stats object
     */
    public static KarateStats parallel(List<String> tags, List<String> paths, int threadCount, String reportDir) {
        KarateRuntimeOptions kro = new KarateRuntimeOptions(tags, paths);
        List<KarateFeature> karateFeatures = KarateFeature.loadFeatures(kro);
        return parallel(karateFeatures, threadCount, reportDir);
    }

    public static KarateStats parallel(List<KarateFeature> karateFeatures, int threadCount, String userReportDir) {
        String reportDir = userReportDir == null ? "target/surefire-reports" : userReportDir;
        logger.info("Karate version: {}", FileUtils.getKarateVersion());
        KarateStats stats = KarateStats.startTimer();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            int count = karateFeatures.size();
            int filteredCount = 0;
            List<Callable<KarateJunitAndJsonReporter>> callables = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                KarateFeature karateFeature = karateFeatures.get(i);
                int index = i + 1;
                CucumberFeature feature = karateFeature.getFeature();
                filterOnTags(feature);
                if (!feature.getFeatureElements().isEmpty()) {
                    callables.add(() -> {
                        // we are now within a separate thread. the reporter filters logs by self thread
                        String threadName = Thread.currentThread().getName();
                        KarateJunitAndJsonReporter reporter = karateFeature.getReporter(reportDir);
                        KarateRuntime runtime = karateFeature.getRuntime(reporter);
                        try {
                            feature.run(reporter, reporter, runtime);
                            runtime.afterFeature();
                            logger.info("<<<< feature {} of {} on thread {}: {}", index, count, threadName, feature.getPath());
                        } catch (Exception e) {
                            logger.error("karate xml/json generation failed for: {}", feature.getPath());
                            reporter.setFailureReason(e);
                        } finally { // try our best to close the report file gracefully so that report generation is not broken
                            reporter.done();
                        }
                        return reporter;
                    });
                } else {
                    filteredCount++;
                }
            }
            stats.setFeatureCount(count - filteredCount);

            List<Future<KarateJunitAndJsonReporter>> futures = executor.invokeAll(callables);
            stats.stopTimer();
            for (Future<KarateJunitAndJsonReporter> future : futures) {
                KarateJunitAndJsonReporter reporter = future.get(); // guaranteed to be not-null
                KarateJunitFormatter formatter = reporter.getJunitFormatter();
                if (reporter.getFailureReason() != null) {
                    logger.error("karate xml/json generation failed: {}", formatter.getFeaturePath());
                    logger.error("karate xml/json error stack trace", reporter.getFailureReason());
                }
                stats.addToTestCount(formatter.getTestCount());
                stats.addToFailCount(formatter.getFailCount());
                stats.addToSkipCount(formatter.getSkipCount());
                stats.addToTimeTaken(formatter.getTimeTaken());
                if (formatter.isFail()) {
                    stats.addToFailedList(formatter.getFeaturePath(), formatter.getFailMessages() + "");
                }
            }
        } catch (Exception e) {
            logger.error("karate parallel runner failed: ", e.getMessage());
            stats.setFailureReason(e);
        } finally {
            executor.shutdownNow();
        }
        stats.printStats(threadCount);
        return stats;
    }

    private static void filterOnTags(CucumberFeature feature) throws TagFilterException {
        final List<CucumberTagStatement> featureElements = feature.getFeatureElements();
        ServiceLoader<TagFilter> loader = ServiceLoader.load(TagFilter.class);
        for (Iterator<CucumberTagStatement> iterator = featureElements.iterator(); iterator.hasNext();) {
            CucumberTagStatement cucumberTagStatement = iterator.next();
            for (TagFilter implClass : loader) {
                logger.info("Tag filter found: {}", implClass.getClass().getSimpleName());
                final boolean isFiltered = implClass.filter(feature, cucumberTagStatement);
                if (isFiltered) {
                    logger.info("skipping feature element {} of feature {} due to feature-tag-filter {} ",
                            cucumberTagStatement.getVisualName(),
                            feature.getPath(), implClass.getClass().getSimpleName());
                    iterator.remove();
                    break;
                }
            }
        }
    }

    public static Map<String, Object> runFeature(File file, Map<String, Object> vars, boolean evalKarateConfig) {
        CallContext callContext = new CallContext(vars, evalKarateConfig);
        return runFeature(file, callContext, null);
    }

    public static Map<String, Object> runFeature(File file, CallContext callContext, KarateReporter reporter) {
        FeatureWrapper featureWrapper = FeatureWrapper.fromFile(file, reporter);
        ScriptValueMap scriptValueMap = CucumberUtils.callSync(featureWrapper, callContext);
        return scriptValueMap.toPrimitiveMap();
    }

    public static Map<String, Object> runFeature(Class relativeTo, String path, Map<String, Object> vars, boolean evalKarateConfig) {
        File file = FileUtils.getFileRelativeTo(relativeTo, path);
        return runFeature(file, vars, evalKarateConfig);
    }

    public static Map<String, Object> runClasspathFeature(String classPath, Map<String, Object> vars, boolean evalKarateConfig) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(classPath);
        if (url == null) {
            throw new RuntimeException("file not found: " + classPath);
        }
        File file = new File(url.getFile());
        return runFeature(file, vars, evalKarateConfig);
    }

}
