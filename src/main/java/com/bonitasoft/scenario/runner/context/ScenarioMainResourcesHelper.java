package com.bonitasoft.scenario.runner.context;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.resource.Resource;

public class ScenarioMainResourcesHelper {

    static public Map<String, byte[]> generateResourcesFromFileSystem(String scenarioPath, Map<String, byte[]> resources) throws Exception {
        File file = new File(scenarioPath);

        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File aFile : files) {
                    generateResourcesFromFileSystem(scenarioPath + File.separator + aFile.getName(), resources);
                }
            } else if (file.isFile()) {
                resources.put(scenarioPath, IOUtils.toByteArray(new FileInputStream(file)));
            }
        }

        return resources;
    }

    static public Map<String, byte[]> generateByteArrayJarDependenciesFromFileSystem(String scenarioPath) throws Exception {
        return generateByteArrayDependenciesFromFileSystem(scenarioPath, new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(Constants.EXTENSION_JAR);
            }
        });
    }

    static public Map<String, byte[]> generateByteArrayGsDependenciesFromFileSystem(String scenarioPath) throws Exception {
        return generateByteArrayDependenciesFromFileSystem(scenarioPath, new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(Constants.EXTENSION_GROOVY);
            }
        });
    }

    static private Map<String, byte[]> generateByteArrayDependenciesFromFileSystem(String scenarioPath, FilenameFilter filenameFilter) throws Exception {
        Map<String, byte[]> dependencies = new HashMap<String, byte[]>();

        File file = new File(scenarioPath);
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles(new FileFilter() {

                    public boolean accept(File file) {
                        return file.isDirectory() && Resource.LIB_FOLDER.equals(file.getName());
                    }
                });
                if (files.length > 0) {
                    File[] libFiles = files[0].listFiles(filenameFilter);
                    for (File libFile : libFiles) {
                        dependencies.put(libFile.getName(), IOUtils.toByteArray(new FileInputStream(libFile)));
                    }
                }
            }
        }

        return dependencies;
    }

    static public Map<String, byte[]> generateMainResourcesFromFileSystem(ScenarioType scenarioType, String scenarioPath) throws Exception {
        Map<String, byte[]> mainResources = new HashMap<String, byte[]>();

        File file = new File(scenarioPath);
        if (file.exists() && file.isDirectory()) {
            if (ScenarioType.SINGLE.equals(scenarioType)) {
                File singleScript = new File(file.getAbsolutePath() + File.separator + SingleRunContext.SCENARIO_FILE_NAME);
                if (singleScript.exists()) {
                    mainResources.put(SingleRunContext.SCENARIO_FILE_NAME, IOUtils.toByteArray(new FileInputStream(singleScript)));
                }
            } else if (ScenarioType.TEST_SUITE.equals(scenarioType)) {
                File mainResourcesFolder = new File(file.getAbsolutePath() + File.separator + TestSuiteRunContext.SCENARIOS_FOLDER_NAME);
                if (mainResourcesFolder.exists()) {
                    File[] files = mainResourcesFolder.listFiles();
                    for (File aFile : files) {
                        mainResources.put(aFile.getName().replaceAll("\\..*", ""), IOUtils.toByteArray(new FileInputStream(file)));
                    }
                }
            }
        }

        return mainResources;
    }

    static public Map<String, byte[]> generateSingleScenarioMainResourcesFromScriptContent(String singleScenarioScriptContent) {
        Map<String, byte[]> mainResources = new HashMap<String, byte[]>();
        mainResources.put(SingleRunContext.SCENARIO_FILE_NAME, singleScenarioScriptContent.getBytes());
        return mainResources;
    }
}
