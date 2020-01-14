package com.bonitasoft.scenario.runner.context;

import java.io.Serializable;
import java.util.Map;

import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.resource.Resource;

public class SingleRunContext extends RunContext {

    static public String SCENARIO_FILE_NAME = "Scenario.groovy";

    public SingleRunContext(Long tenantId, ScenarioConfiguration scenarioConfiguration, Map<String, Serializable> parameters, Map<String, byte[]> mainResources, Map<String, byte[]> jarDependencies, Map<String, byte[]> gsDependencies, Resource resource, String scenarioName) {
        super(tenantId, scenarioConfiguration, ScenarioType.SINGLE, parameters, mainResources, jarDependencies, gsDependencies, resource, scenarioName);
    }

    public String getGSContent() {
        return new String(getMainResources().get(SCENARIO_FILE_NAME));
    }

    @Override
    public String toString() {
        return "Scenario " + getName();
    }
}
