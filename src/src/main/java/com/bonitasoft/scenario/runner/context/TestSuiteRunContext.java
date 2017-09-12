package com.bonitasoft.scenario.runner.context;

import java.io.Serializable;
import java.util.Map;

import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.resource.Resource;

public class TestSuiteRunContext extends RunContext {
	static public String SCENARIOS_FOLDER_NAME = "test";
	
	private String[] testSuiteNames = null;
	private int currentTestSuiteIndex = -1;
	
	public TestSuiteRunContext(Long tenantId, ScenarioConfiguration scenarioConfiguration, Map<String, Serializable> parameters, Map<String, byte[]> mainResources, Map<String, byte[]> jarDependencies, Map<String, byte[]> gsDependencies, Resource resource, String scenarioName, String testSuiteLocationFolderPath) {
		super(tenantId, scenarioConfiguration, ScenarioType.TEST_SUITE, parameters, mainResources, jarDependencies, gsDependencies, resource, scenarioName);
	}
	
	public TestSuiteRunContext(Long tenantId, ScenarioConfiguration scenarioConfiguration, Map<String, Serializable> parameters, Map<String, byte[]> mainResources, Map<String, byte[]> jarDependencies, Map<String, byte[]> gsDependencies, Resource resource, String scenarioName, String testSuiteLocationFolderPath, String[] testSuiteNames) {
		this(tenantId, scenarioConfiguration, parameters, mainResources, jarDependencies, gsDependencies, resource, scenarioName, testSuiteLocationFolderPath);
		
		this.testSuiteNames = testSuiteNames;
	}
	
	public String[] getTestSuiteNames() {
		return testSuiteNames;
	}
	
	public void setTestSuiteNames(String[] testSuiteNames) {
		this.testSuiteNames = testSuiteNames;
	}

	public int getCurrentTestSuiteIndex() {
		return currentTestSuiteIndex;
	}

	public void setCurrentTestSuiteIndex(int currentTestSuiteIndex) {
		if(currentTestSuiteIndex > -1 && currentTestSuiteIndex < testSuiteNames.length) {
			this.currentTestSuiteIndex = currentTestSuiteIndex;
		}
	}
	
	public String getCurrentTestSuiteName() {
		return testSuiteNames[currentTestSuiteIndex];
	}

	@Override
	public String getGSContent() {
		return new String(getMainResources().get(getCurrentTestSuiteName()));
	}
	
	@Override
	public String toString() {
		if(currentTestSuiteIndex > -1 && currentTestSuiteIndex < testSuiteNames.length) {
			return getCurrentTestSuiteName();
		}
		
		return "General";
	}
}
