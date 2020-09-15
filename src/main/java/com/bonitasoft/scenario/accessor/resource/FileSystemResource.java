package com.bonitasoft.scenario.accessor.resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;

import com.bonitasoft.scenario.accessor.exception.FunctionUnvalidParameterException;

public class FileSystemResource extends Resource {

    public FileSystemResource() {
        super(null);
    }

    @Override
    public Object getResource(ResourceType resourceType, String name) throws FunctionUnvalidParameterException {
        // Handle default value
        if (DEFAULT_RESOURCE.equals(name)) {
            if (resourceType == ResourceType.ORGANIZATION) {
                return defaultOrganizationResource;
            }
        }

        // Generate the resource name
        String resourceName = getResourceName(resourceType, name);

        // Return the resource in the right format
        try {
            if (resourceType.equals(ResourceType.ORGANIZATION)) {
                return IOUtils.toString(new File(resourceName).toURL());
            } else if (resourceType.equals(ResourceType.PROCESS)) {
                return BusinessArchiveFactory.readBusinessArchive(new ByteArrayInputStream(IOUtils.toByteArray(new FileInputStream(new File(resourceName)))));
            } else if (resourceType.equals(ResourceType.PROCESS_ACTORS)) {
                return IOUtils.toString(new File(resourceName).toURL());
            } else if (resourceType.equals(ResourceType.PROFILES)) {
                return IOUtils.toString(new File(resourceName).toURL()).getBytes(StandardCharsets.UTF_8);
            } else if (resourceType.equals(ResourceType.PROCESS_PARAMETERS)) {
                return IOUtils.toByteArray(new FileInputStream(new File(resourceName)));
            } else if (resourceType.equals(ResourceType.BDM)) {
                return IOUtils.toByteArray(new FileInputStream(new File(resourceName)));
            } else if (resourceType.equals(ResourceType.JSON)) {
                return parseJSON(IOUtils.toString(new File(resourceName).toURL()));
            } else if (resourceType.equals(ResourceType.CONNECTOR_IMPLEMENTATION)) {
                return IOUtils.toByteArray(new FileInputStream(new File(resourceName)));
            }
        } catch (Exception e) {
            handleError(resourceName, e);
        }

        return null;
    }
}
