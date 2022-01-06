/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.quorum.gauge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Read JUNIT xml report produced by Gauge and provide a summary like Gauge does.
 *
 * E.g.:
 * <pre>
 * Specifications: 16 executed     16 passed       0 failed        1 skipped
 * Scenarios:      42 executed     42 passed       0 failed        1 skipped
 * </pre>
 *
 * Also produce a json file to be read by Github Actions in order to aggregate multiple runs.
 *
 */
public class TestSummaryMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Missing args");
            System.exit(1);
        }
        String jobId = args[0].strip();
        File xmlFile = new File(args[1]);
        File outputDir = xmlFile.getParentFile();
        if (args.length > 2) {
            outputDir = new File(args[2]);
        }
        Summary aggregatedSpecSummary = new Summary();
        Summary aggregatedScenarioSummary = new Summary();
        if (xmlFile.exists()) {
            XmlMapper xmlMapper = new XmlMapper();
            TestSuites suites = xmlMapper.readValue(xmlFile, TestSuites.class);
            List<FailureSummary> failures = new ArrayList<>();
            List<SkippedSummary> skipped = new ArrayList<>();
            for (TestSuite ts : suites.getTestsuite()) {
                Summary spec = new Summary();
                Summary scenario = new Summary();
                spec.addExecuted(1);
                boolean hasFailedOrSkipped = false;
                String s = "";
                if (ts.failures + ts.errors > 0) {
                    spec.addFailed(1);
                    hasFailedOrSkipped = true;
                    s = "FAILED";
                }
                if (ts.skipped > 0) {
                    hasFailedOrSkipped = true;
                    spec.addSkipped(1);
                    s = "SKIPPED";
                }
                if (!hasFailedOrSkipped) {
                    spec.addPassed(1);
                    s = "PASSED";
                }
                System.out.printf("\n%s - %s - took %.2fs\n", ts.getName(), s, ts.getTime());
                if (ts.getTestcase() != null) { // some exception before test executed
                    scenario.addExecuted(ts.getTestcase().size());
                    for (TestCase tc : ts.getTestcase()) {
                        s = "PASSED";
                        if (tc.getFailure() != null) {
                            FailureSummary fs = new FailureSummary();
                            fs.setFile(StringUtils.removeStart(ts.getSpec(), StringUtils.substringBefore(ts.getSpec(), "src/specs")));
                            fs.setLine(1);
                            fs.setCol(1);
                            fs.setMessage(String.format("Scenario: %s\nStep: %s", tc.getName(), tc.getFailure().getMessage()).replaceAll("\\n", "%0A"));
                            failures.add(fs);
                            scenario.addFailed(1);
                            s = "FAILED";
                        }
                        if (tc.getSkipped() != null) {
                            SkippedSummary ss = new SkippedSummary();
                            ss.setMessage(String.format("Scenario: %s\nStep: %s", tc.getName(), tc.getSkipped().getMessage()).replaceAll("\\n", "%0A"));
                            skipped.add(ss);
                            scenario.addSkipped(1);
                            s = "SKIPPED";
                        }
                        System.out.printf("  %s - %s - took %.2fs\n", tc.getName(), s, tc.getTime());
                    }
                    scenario.addPassed(Math.max(0, scenario.getExecuted() - scenario.getFailed() - scenario.getSkipped()));
                }
                aggregatedSpecSummary.aggregate(spec);
                aggregatedScenarioSummary.aggregate(scenario);
            }
            TeeOutputStream failureTee = new TeeOutputStream(new FileOutputStream(new File(outputDir, "failures.txt"), false), System.out);
            new ObjectMapper()
                    .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET) // avoid Jackson to close the output streams
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(failureTee, failures);
            failureTee.flush();

            TeeOutputStream skippedTee = new TeeOutputStream(new FileOutputStream(new File(outputDir, "skipped.txt"), false), System.out);
            new ObjectMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET) // avoid Jackson to close the output streams
                .writerWithDefaultPrettyPrinter()
                .writeValue(skippedTee, skipped);
            skippedTee.flush();
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        // summary
        File overallSummaryFile = new File(outputDir, "summary.txt");
        System.out.println("\nOverall Summary in " + overallSummaryFile.getPath());
        TeeOutputStream tee = new TeeOutputStream(new FileOutputStream(overallSummaryFile, false), System.out);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(tee),true);
        writer.printf("Specifications: %6d executed %5d passed %5d failed %5d skipped\n", aggregatedSpecSummary.getExecuted(), aggregatedSpecSummary.getPassed(), aggregatedSpecSummary.getFailed(), aggregatedSpecSummary.getSkipped());
        writer.printf("Scenarios:      %6d executed %5d passed %5d failed %5d skipped\n", aggregatedScenarioSummary.getExecuted(), aggregatedScenarioSummary.getPassed(), aggregatedScenarioSummary.getFailed(), aggregatedScenarioSummary.getSkipped());
        writer.flush();
        // scenario summary in JSON
        File scenarioSummaryFile = new File(outputDir, jobId + ".json");
        System.out.println("\nScenarios Summary in JSON " + scenarioSummaryFile.getPath());
        TeeOutputStream scenarioTee = new TeeOutputStream(new FileOutputStream(scenarioSummaryFile, false), System.out);
        new ObjectMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET) // avoid Jackson to close the output streams
                .writerWithDefaultPrettyPrinter()
                .writeValue(scenarioTee, aggregatedScenarioSummary);
        scenarioTee.flush();
        System.out.println("\n");
        if (aggregatedSpecSummary.getFailed() > 0 || aggregatedSpecSummary.getPassed() == 0 || aggregatedScenarioSummary.getPassed() == 0) {
            // delegate failing Gauge execution here
            // fail Gauge execution if there are test failures or no tests pass (e.g. if all tests were skipped or no tests were executed by given tags)
            System.err.println("ERROR: There are test failures or no tests were run to completion");
            System.exit(1);
        }
    }

    private static class Summary {
        private int executed;
        private int passed;
        private int failed;
        private int skipped;

        public void aggregate(Summary other) {
            this.addExecuted(other.getExecuted());
            this.addPassed(other.getPassed());
            this.addFailed(other.getFailed());
            this.addSkipped(other.getSkipped());
        }

        public void addExecuted(int a) {
            this.executed += a;
        }

        public void addPassed(int a) {
            this.passed += a;
        }

        public void addFailed(int a) {
            this.failed += a;
        }

        public void addSkipped(int a) {
            this.skipped += a;
        }

        public int getExecuted() {
            return executed;
        }

        public int getPassed() {
            return passed;
        }

        public int getFailed() {
            return failed;
        }

        public int getSkipped() {
            return skipped;
        }
    }

    private static class SkippedSummary {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private static class FailureSummary {
        private String file;
        private int line;
        private int col;
        private String message;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JacksonXmlRootElement(localName = "testsuites")
    private static class TestSuites {
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<TestSuite> testsuite;

        public TestSuites() {}

        public List<TestSuite> getTestsuite() {
            return testsuite;
        }

        public void setTestsuite(List<TestSuite> testsuite) {
            this.testsuite = testsuite;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TestSuite {
        @JacksonXmlProperty(isAttribute = true)
        private int tests;
        @JacksonXmlProperty(isAttribute = true)
        private int failures;
        @JacksonXmlProperty(isAttribute = true)
        private int errors;
        @JacksonXmlProperty(isAttribute = true)
        private int skipped;
        @JacksonXmlProperty(isAttribute = true)
        private double time;
        @JacksonXmlProperty(isAttribute = true)
        private String name;
        @JacksonXmlProperty(isAttribute = true, localName = "package")
        private String spec;
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<TestCase> testcase;
        @JacksonXmlProperty(localName = "system-out")
        private String stdOut;
        @JacksonXmlProperty(localName = "system-err")
        private String stdErr;

        public TestSuite() {}

        public int getTests() {
            return tests;
        }

        public int getFailures() {
            return failures;
        }

        public int getErrors() {
            return errors;
        }

        public int getSkipped() {
            return skipped;
        }

        public double getTime() {
            return time;
        }

        public void setTime(double time) {
            this.time = time;
        }

        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }

        public void setErrors(int errors) {
            this.errors = errors;
        }

        public void setFailures(int failures) {
            this.failures = failures;
        }

        public void setTests(int tests) {
            this.tests = tests;
        }

        public String getSpec() {
            return spec;
        }

        public void setSpec(String spec) {
            this.spec = spec;
        }

        public List<TestCase> getTestcase() {
            return testcase;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TestCase {
        @JacksonXmlProperty(isAttribute = true)
        private String name;
        @JacksonXmlProperty(isAttribute = true)
        private double time;
        private Failure failure;
        private Skipped skipped;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Failure getFailure() {
            return failure;
        }

        public void setFailure(Failure failure) {
            this.failure = failure;
        }

        public Skipped getSkipped() {
            return skipped;
        }

        public void setSkipped(Skipped skipped) {
            this.skipped = skipped;
        }

        public double getTime() {
            return time;
        }

        public void setTime(double time) {
            this.time = time;
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Skipped {
        @JacksonXmlProperty(isAttribute = true)
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Failure {
        @JacksonXmlProperty(isAttribute = true)
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
