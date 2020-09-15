package com.bonitasoft.scenario.accessor.resource;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.exception.FunctionUnvalidParameterException;

import groovy.json.JsonSlurper;
import groovy.json.internal.LazyMap;

abstract public class Resource {

    static public String RESOURCE_FOLDER = "resource";
    static public String LIB_FOLDER = "lib";

    private Accessor accessor = null;

    private JsonSlurper jsonParser = new JsonSlurper();

    static public String DEFAULT_RESOURCE = "__DEFAULT_RESOURCE__";

    static private String DEFAULT_ORGANIZATION_RESOURCE_NAME = "Organization_Data-ACME" + Constants.EXTENSION_XML;
    static protected String defaultOrganizationResource = null;

    protected Resource(Accessor accessor) {
        this.accessor = accessor;
        byte[] defaultOrganization = getDefaultResource(DEFAULT_ORGANIZATION_RESOURCE_NAME);
        if (defaultOrganization != null)
            defaultOrganizationResource = new String(defaultOrganization);
    }

    protected String getRoot() {
        return accessor.getScenarioConfiguration().getScenarioRoot() + File.separator + accessor.getScenarioType() + File.separator + accessor.getScenarioName() + File.separator + RESOURCE_FOLDER + File.separator;
    }

    protected String getResourceName(ResourceType resourceType, String name) {
        return getRoot() + name + resourceType.getExtension();
    }

    abstract public Object getResource(ResourceType resourceType, String resourceName) throws FunctionUnvalidParameterException;

    public Object getJSON(String resourceName) throws FunctionUnvalidParameterException {
        return getResource(ResourceType.JSON, resourceName);
    }

    protected Object parseJSON(String jsonString) {
        Object parsedObject = jsonParser.parseText(jsonString);

        if (parsedObject instanceof LazyMap) {
            parsedObject = new HashMap((Map) parsedObject);
        }

        return parsedObject;
    }

    public Accessor getAccessor() {
        return accessor;
    }

    public void setAccessor(Accessor accessor) {
        this.accessor = accessor;
    }

    protected void handleError(String resourceName, Throwable throwable) throws FunctionUnvalidParameterException {
        String expectationMessage = "reference an available resource of the scenario";
        if (throwable != null) {
            throw new FunctionUnvalidParameterException(resourceName, Constants.RESOURCE_NAME, expectationMessage, throwable);
        } else {
            throw new FunctionUnvalidParameterException(resourceName, Constants.RESOURCE_NAME, expectationMessage);
        }
    }

    static private byte[] getDefaultResource(String resource) {
        InputStream input = null;
        try {
            input = Resource.class.getResource(resource).openStream();
            if (input == null)
                return null;
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(input, output);
            return output.toByteArray();
        } catch (Exception e) {
            ScenarioConfiguration.logger.log(Level.INFO, "The default resource " + resource + " could not be loaded", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e1) {
                }
            }
        }

        return null;
    }
}
