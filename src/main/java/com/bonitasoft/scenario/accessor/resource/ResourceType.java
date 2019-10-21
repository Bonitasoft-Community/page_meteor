package com.bonitasoft.scenario.accessor.resource;

import com.bonitasoft.scenario.accessor.Constants;

public enum ResourceType {
	ORGANIZATION(Constants.EXTENSION_XML), PROFILES(Constants.EXTENSION_XML), PROCESS(Constants.EXTENSION_BAR), PROCESS_ACTORS(Constants.EXTENSION_XML), PROCESS_PARAMETERS(Constants.EXTENSION_PROPERTIES), BDM(Constants.EXTENSION_ZIP), JSON(Constants.EXTENSION_JSON), CONNECTOR_IMPLEMENTATION(
			Constants.EXTENSION_ZIP);

	private String extension = null;

	ResourceType(String extension) {
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}
}
