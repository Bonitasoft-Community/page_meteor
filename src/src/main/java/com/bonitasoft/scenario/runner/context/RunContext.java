package com.bonitasoft.scenario.runner.context;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.resource.Resource;

public abstract class RunContext {
	private Long tenantId = null;
	
	private ScenarioType scenarioType = null;
	private Map<String, Serializable> parameters = new HashMap<String, Serializable>();
	private ScenarioConfiguration scenarioConfiguration = null;
	private Resource resource = null;
	private Map<String, byte[]> mainResources = null;
	private Map<String, byte[]> jarDependencies = new HashMap<String, byte[]>();
	private Map<String, byte[]> gsDependencies = new HashMap<String, byte[]>();
	private String name = null;
	private Integer advancement = 0;
	
	public RunContext(Long tenantId, ScenarioConfiguration scenarioConfiguration, ScenarioType scenarioType, Map<String, Serializable> parameters, Map<String, byte[]> mainResources, Map<String, byte[]> jarDependencies, Map<String, byte[]> gsDependencies, Resource resource, String name) {
		this.tenantId = tenantId;
		
		if(parameters != null) {
			this.parameters = parameters;
		}
		
		this.scenarioConfiguration = scenarioConfiguration;
		this.resource = resource;
		this.jarDependencies = jarDependencies;
		this.gsDependencies = gsDependencies;
		this.mainResources = mainResources;
		this.scenarioType = scenarioType;
		this.name = name;
	}
	
	public Map<String, Serializable> getParameters() {
		return parameters;
	}

	public Long getTenantId() {
		return tenantId;
	}
	
	public Resource getResource() {
		return resource;
	}

	public ScenarioType getScenarioType() {
		return scenarioType;
	}

	public String getName() {
		return name;
	}
	
	public ScenarioConfiguration getScenarioConfiguration() {
		return scenarioConfiguration;
	}

	public Map<String, byte[]> getMainResources() {
		return mainResources;
	}
	
	abstract public String getGSContent();
	
	public String getScenarioRoot() {
		return scenarioConfiguration.getScenarioRoot() + File.separator + scenarioType + File.separator + name + File.separator;
	}

	public Integer getAdvancement() {
		return advancement;
	}

	public Map<String, byte[]> getJarDependencies() {
		return jarDependencies;
	}
	
	public Map<String, byte[]> getGsDependencies() {
		return gsDependencies;
	}
}
