package com.bonitasoft.scenario.runner;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;

import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.runner.context.TestSuiteRunContext;

public class TestSuiteRunner extends Runner {
	protected TestSuiteRunContext testSuiteRunContext = null;

	public TestSuiteRunner(TestSuiteRunContext testSuiteRunContext, List<RunListener> runListeners) throws Exception {
		super(testSuiteRunContext, runListeners);

		this.testSuiteRunContext = (TestSuiteRunContext) runContext;

		// TODO: add a runListener to log the result of a test each time a test
		// is over
	}

	@Override
	public void execute() throws Exception {
		Map<String, Map<String, Throwable>> result = new HashMap<String, Map<String, Throwable>>();

		if (testSuiteRunContext.getTestSuiteNames() == null || testSuiteRunContext.getTestSuiteNames().length == 0) {
			File testSuiteLocationFolder = new File(testSuiteRunContext.getScenarioRoot());
			if (testSuiteLocationFolder.exists() && testSuiteLocationFolder.isDirectory()) {
				File[] testSuites = testSuiteLocationFolder.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return file.isDirectory();
					}
				});

				String[] testSuiteNames = new String[testSuites.length];
				for (int i = 0; i < testSuites.length; i++) {
					testSuiteNames[i] = testSuites[i].getName();
				}

				testSuiteRunContext.setTestSuiteNames(testSuiteNames);
			} else {
				log(Level.SEVERE, "The provided Test Suite repository cannot be found", null);
				// return (Serializable)result;
			}
		}

		for (int i = 0; i < testSuiteRunContext.getTestSuiteNames().length; i++) {
			testSuiteRunContext.setCurrentTestSuiteIndex(testSuiteRunContext.getCurrentTestSuiteIndex() + 1);

			Map<String, Throwable> testSuiteResult = new HashMap<String, Throwable>();
			result.put(testSuiteRunContext.getCurrentTestSuiteName(), testSuiteResult);

			// String testSuiteLocation = testSuiteRunContext.getMainContent() +
			// File.separator;
			String testSuiteLocation = File.separator;
			File testSuiteFolder = new File(testSuiteLocation);
			File testSuiteTestsFolder = new File(testSuiteLocation + "test");

			if (testSuiteFolder.exists() && testSuiteFolder.isDirectory() && testSuiteTestsFolder.exists() && testSuiteTestsFolder.isDirectory()) {
				File[] testScenarios = testSuiteTestsFolder.listFiles();
				if (testScenarios.length > 0) {
					// Generate the Test Suite GS content
					String testSuiteGSContent = IOUtils.toString(TestSuiteRunner.class.getResource("TestRunner.groovy")).replaceAll("FUNCTIONS", IOUtils.toString(SingleRunner.class.getResource("functions.groovy")));
					// testSuiteGSContent =
					// testSuiteGSContent.replaceAll("PUT_UP",
					// extractResourceFileContent(testSuiteFolder +
					// File.separator + "putUp.groovy"));
					// testSuiteGSContent =
					// testSuiteGSContent.replaceAll("BEFORE_EACH_TEST",
					// extractResourceFileContent(testSuiteFolder +
					// File.separator + "beforeEachTest.groovy"));
					// testSuiteGSContent =
					// testSuiteGSContent.replaceAll("AFTER_EACH_TEST",
					// extractResourceFileContent(testSuiteFolder +
					// File.separator + "afterEachTest.groovy"));
					// testSuiteGSContent =
					// testSuiteGSContent.replaceAll("TEAR_DOWN",
					// extractResourceFileContent(testSuiteFolder +
					// File.separator + "tearDown.groovy"));

					String testWrapper = IOUtils.toString(TestSuiteRunner.class.getResource("testWrapper.groovy"));
					StringBuffer stringBuffer = new StringBuffer("");
					for (File testScenario : testScenarios) {
						stringBuffer.append(testWrapper.replaceAll("TEST_NAME", testScenario.getName().replaceAll("\\..*", "")).replaceAll("TEST_SCENARIO", IOUtils.toString(testScenario.toURL())));
					}
					testSuiteGSContent = testSuiteGSContent.replaceAll("TESTS", stringBuffer.toString());
					log(Level.FINE, "Generated GS to run:" + Constants.LINE_SEPARATOR + testSuiteGSContent, null);

					// Launch the GS with a suitable context for the Test Suite
					// to run over BOS API
					// testSuiteResult = (Map<String,
					// Throwable>)groovyEvaluation(testSuiteGSContent,
					// testSuiteRunContext.getDependencies());
					// testSuiteFolder.getAbsolutePath()

					// Log the result
					// TODO: use the listener pattern as done for the
					// RunListener instead of logging all tests at the end
					Iterator<String> resultInterator = testSuiteResult.keySet().iterator();
					int successes = 0;
					int failures = 0;
					log(Level.INFO, "RESULT", null);
					while (resultInterator.hasNext()) {
						String key = resultInterator.next();
						if (testSuiteResult.get(key) == null) {
							successes++;
							log(Level.INFO, "SUCCESS: " + key, null);
						} else {
							failures++;
							log(Level.SEVERE, "FAILURE: " + key + Constants.LINE_SEPARATOR + ScenarioResult.extractThrowableStackTrace(testSuiteResult.get(key)), null);
						}
					}
					log(Level.INFO, "Run: " + (successes + failures) + ", Successful: " + successes + ", Failed: " + failures, null);
				} else {
					log(Level.WARNING, "There is no tests to run in this Test Suite", null);
				}
			} else {
				log(Level.SEVERE, "The provided Test Suite cannot be found in the Test Suite repository", null);
			}
			log(Level.INFO, "END", null);
		}

		// return (Serializable)result;
		return;
	}

	// static public void main(String[] args) throws Exception {
	// String[] testSuiteNames = {"testSuite1", "testSuite2"};
	//// String[] testSuiteNames = {"testSuite3"};
	//// TestSuiteRunner testRunner = new TestSuiteRunner(new
	// TestSuiteRunContext("example" + File.separator + "testSuite"));
	// TestSuiteRunner testRunner = new TestSuiteRunner(new
	// TestSuiteRunContext("example" + File.separator + "testSuite",
	// testSuiteNames));
	// testRunner.run();
	// }
}
