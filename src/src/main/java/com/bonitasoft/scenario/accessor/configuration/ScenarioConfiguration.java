package com.bonitasoft.scenario.accessor.configuration;

import java.util.Properties;
import java.util.logging.Logger;

import org.bonitasoft.engine.commons.io.PropertiesManager;

public class ScenarioConfiguration {
	final static private String DEFAULT_CONFIGURATION_FILE_LOCATION = "defaultScenario.properties";
	
	final static private String SCENARIO_ROOT_LOCATION = "scenarioRootLocation";
	final static private String SCENARIO_ROOT_LOCATION_DEFAULT = ".";
	
	final static public Logger logger = Logger.getLogger("com.bonitasoft.scenario");
	
	private Properties configuration = new Properties();
	
	public ScenarioConfiguration() {
		this(null);
	}
	
	public ScenarioConfiguration(Properties properties) {
		logger.info("Initialize the Scenario configuration");
		if(properties != null) {
			configuration = properties;
		} else {
			logger.info("No Scenario configuration properties have been provided, the default ones are being loaded");
			try {
				configuration = PropertiesManager.getProperties(ScenarioConfiguration.class.getResource(ScenarioConfiguration.DEFAULT_CONFIGURATION_FILE_LOCATION));
			} catch(Exception e2) {
				logger.severe("The default Scenario configuration properties could not be loaded");
			}
		}
	}
	
	public String getScenarioRoot() {
		if(configuration != null && configuration.containsKey(SCENARIO_ROOT_LOCATION)) {
			return configuration.getProperty(SCENARIO_ROOT_LOCATION);
		} else {
			return SCENARIO_ROOT_LOCATION_DEFAULT;
		}
	}
}
