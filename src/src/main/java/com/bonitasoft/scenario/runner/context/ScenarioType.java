package com.bonitasoft.scenario.runner.context;

public enum ScenarioType {
	SINGLE("single"), 
	TEST_SUITE("testSuite");
	
	private String name = null;
	
	ScenarioType(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
