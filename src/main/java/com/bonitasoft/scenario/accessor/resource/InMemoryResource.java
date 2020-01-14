package com.bonitasoft.scenario.accessor.resource;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;

import com.bonitasoft.scenario.accessor.exception.FunctionUnvalidParameterException;

public class InMemoryResource extends Resource {

    private Map<String, byte[]> resources = new HashMap<String, byte[]>();

    public InMemoryResource() {
        super(null);
    }

    public InMemoryResource(Map<String, byte[]> resources) {
        super(null);

        this.resources = resources;
    }

    @Override
    public Object getResource(ResourceType resourceType, String name) throws FunctionUnvalidParameterException {
        // Handle default value
        if (DEFAULT_RESOURCE.equals(name)) {
            if (resourceType == ResourceType.ORGANIZATION) {
                return defaultOrganizationResource;
            } else {
                return null;
            }
        }

        // Generate the resource name
        String resourceName = getResourceName(resourceType, name);

        // Return the resource in the right format
        if (resources.containsKey(resourceName)) {
            try {
                if (resourceType.equals(ResourceType.ORGANIZATION)) {
                    return new String(resources.get(resourceName));
                } else if (resourceType.equals(ResourceType.PROCESS)) {
                    return BusinessArchiveFactory.readBusinessArchive(new ByteArrayInputStream(resources.get(resourceName)));
                } else if (resourceType.equals(ResourceType.PROCESS_ACTORS)) {
                    return new String(resources.get(resourceName));
                } else if (resourceType.equals(ResourceType.PROFILES)) {
                    return resources.get(resourceName);
                } else if (resourceType.equals(ResourceType.PROCESS_PARAMETERS)) {
                    return resources.get(resourceName);
                } else if (resourceType.equals(ResourceType.BDM)) {
                    return resources.get(resourceName);
                } else if (resourceType.equals(ResourceType.JSON)) {
                    return parseJSON(new String(resources.get(resourceName)));
                } else if (resourceType.equals(ResourceType.CONNECTOR_IMPLEMENTATION)) {
                    return resources.get(resourceName);
                }
            } catch (Exception e) {
                handleError(resourceName, e);
            }
        } else {
            handleError(resourceName, null);
        }

        return null;
    }
}
