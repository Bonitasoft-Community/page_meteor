package org.bonitasoft.meteor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.properties.BonitaProperties;
import org.json.simple.JSONValue;

public class MeteorDAO {

    /**
     * ********************************************************************************
     * This class manage all the persistence of configuration: - load, save,
     * export, import
     * ********************************************************************************
     */

    private static Logger logger = Logger.getLogger(MeteorAPI.class.getName());
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:MM:ss");

    private static BEvent EventConfigurationSaved = new BEvent(MeteorDAO.class.getName(), 1, Level.INFO, "Configuration saved", "The configuration is saved with sucess");
    private static BEvent EventConfigurationLoaded = new BEvent(MeteorDAO.class.getName(), 2, Level.INFO, "Configuration loaded", "The configuration is loaded with sucess");
    private static BEvent EventConfigurationRemoved = new BEvent(MeteorDAO.class.getName(), 3, Level.INFO, "Configuration deleted", "The configuration is deleted");
    private static BEvent EventExportFailed = new BEvent(MeteorDAO.class.getName(), 4, Level.APPLICATIONERROR, "Export failed", "The export failed", "The zip file is not delivered", "check the exception");
    private static BEvent EventPageDirectoryExportFailed = new BEvent(MeteorDAO.class.getName(), 5, Level.ERROR, "Export failed", "The export failed", "The zip file is not delivered", "check the exception");
    private static BEvent EventFileUploadedNotFound = new BEvent(MeteorDAO.class.getName(), 6, Level.ERROR, "Temporary file not found", "The temporary file updloaded is not found", "The import failed",
            "Search where the temporary file is and updload the software (it seem that the Bonita Portal choose a different path again ? )");
    private static BEvent EventConfigurationStructureFailed = new BEvent(MeteorDAO.class.getName(), 7, Level.APPLICATIONERROR, "Import file incorrect", "The import file has an incorrect structure", "The import failed", "Check the file");
    private static BEvent EventConfigurationImported = new BEvent(MeteorDAO.class.getName(), 8, Level.INFO, "Import done", "The import is done with success");

    private static BEvent EventConfigurationNameMissing = new BEvent(MeteorDAO.class.getName(), 9, Level.APPLICATIONERROR, "Configuration name missing", "A name is missing", "A name must be given to save it", "Configuration will not be saved");

    private static String cstPropertiesConfig = "config_";
    private static String cstPropertiesDescription = "description_";

    private static String cstZipNameEntry = "meteortest.txt";

    public static MeteorDAO getInstance() {
        return new MeteorDAO();
    }

    public static class Configuration {

        public String name;
        public String description;
        public String content;
    }

    public class StatusDAO {

        public List<BEvent> listEvents = new ArrayList<BEvent>();
        public ByteArrayOutputStream containerZip;
        public String containerName;
        Configuration configuration;
        public List<Map<String, Object>> listNamesAllConfigurations = null;

        public Map<String, Object> getMap() {
            Map<String, Object> answer = new HashMap<String, Object>();
            if (listNamesAllConfigurations != null) {
                answer.put( MeteorAPI.cstJsonConfigList, listNamesAllConfigurations);
            }
            answer.put("listeventsconfig", BEventFactory.getHtml(listEvents));
            return answer;
        }
    }

    public StatusDAO getListNames(String pageName, long tenantId) {
        StatusDAO statusDAO = new StatusDAO();
        // save it
        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);
        bonitaProperties.setCheckDatabase(false);

        statusDAO.listEvents.addAll(bonitaProperties.load());

        statusDAO.listNamesAllConfigurations = getListConfig(bonitaProperties);
        return statusDAO;
    }

    public StatusDAO save(Configuration configuration, boolean getListNameConfiguration, String pageName, long tenantId) {
        StatusDAO statusDAO = new StatusDAO();
        // save it
        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);
        bonitaProperties.setCheckDatabase(false);

        statusDAO.listEvents.addAll(bonitaProperties.load());

        logger.fine("MeteorDAO.saveConfig: loadproperties done, events = " + statusDAO.listEvents.size());

        if (configuration.name == null)
        {
            // a name must be given !
            statusDAO.listEvents.add(EventConfigurationNameMissing);
        }
        else
        {
            bonitaProperties.setProperty(cstPropertiesConfig + configuration.name, configuration.content);
            bonitaProperties.setProperty(cstPropertiesDescription + configuration.name, configuration.description == null ? "" : configuration.description);
            statusDAO.listEvents.addAll(bonitaProperties.store());
            if (!BEventFactory.isError(statusDAO.listEvents))
                statusDAO.listEvents.add(EventConfigurationSaved);
        }

        logger.info("MeteorDAO.saveConfig store properties  done, events = " + statusDAO.listEvents.size());
        if (getListNameConfiguration)
            statusDAO.listNamesAllConfigurations = getListConfig(bonitaProperties);

        return statusDAO;
    }

    /**
     * @param name
     * @param pageName
     * @param tenantId
     * @return
     */
    public StatusDAO load(String name, String pageName, long tenantId) {
        StatusDAO statusDAO = new StatusDAO();
        // save it
        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);

        statusDAO.listEvents.addAll(bonitaProperties.load());

        logger.info("MeteorDAO.saveConfig: loadproperties done, events = " + statusDAO.listEvents.size());

        // due to the split, we reload a list of MAP like
        // [
        // { "processes" : { "Variables" :"", ...} },
        // { "processes" : { "Variables" :"", ...} },
        // { "processes" : { "Variables" :"", ...} },
        // { "scenarii" : { },

        // RESULT expected:
        // { "processes" : [], "scenarii": [] }
        /*
         * List< Map<String,Object>> listFromConfig =
         * JSONValue.parse(jsonConfiguration);
         * // then recreate one variable with processes and scenarii Map<String,
         * Object> finalResult = new HashMap<String,Object>(); for
         * (Map<String,Object> mapSplited : listFromConfig) { for (String key :
         * mapSplited.keySet()) { List<Object> listKey = finalResult.get( key );
         * if (listKey==null) listKey = new ArrayList(); listKey.add(
         * mapSplited.get( key )); finalResult.put( key, listKey ); } }
         */
        statusDAO.configuration = new Configuration();
        statusDAO.configuration.name = name;
        statusDAO.configuration.content = bonitaProperties.getProperty(cstPropertiesConfig + name);
        statusDAO.configuration.description = bonitaProperties.getProperty(cstPropertiesDescription + name);
        logger.info("BonitaProperties.load store properties  done, events = " + statusDAO.listEvents.size() + " jsonConfiguration=" + statusDAO.configuration.content);

        if (!BEventFactory.isError(statusDAO.listEvents))
            statusDAO.listEvents.add(EventConfigurationLoaded);
        return statusDAO;
    }

    /**
     * @param name
     * @param getListNameConfiguration
     * @param pageName
     * @param tenantId
     * @return
     */
    public StatusDAO delete(String name, boolean getListNameConfiguration, String pageName, long tenantId) {
        StatusDAO statusDAO = new StatusDAO();

        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);
        bonitaProperties.setCheckDatabase(false);

        statusDAO.listEvents.addAll(bonitaProperties.load());
        logger.info("BonitaProperties.saveConfig: loadproperties done, events = " + statusDAO.listEvents.size());

        bonitaProperties.remove(cstPropertiesConfig + name);
        bonitaProperties.remove(cstPropertiesDescription + name);

        statusDAO.listEvents.addAll(bonitaProperties.store());
        if (!BEventFactory.isError(statusDAO.listEvents))
            statusDAO.listEvents.add(new BEvent(EventConfigurationRemoved, "Configuration " + name));

        if (getListNameConfiguration)
            statusDAO.listNamesAllConfigurations = getListConfig(bonitaProperties);

        return statusDAO;
    }

    /**
     * check if environement is correct
     * 
     * @return
     */
    public List<BEvent> checkAndUpdateEnvironment(long tenantId) {
        List<BEvent> listEvents = new ArrayList<BEvent>();
        BonitaProperties bonitaProperties = new BonitaProperties("pageName", tenantId);

        bonitaProperties.setCheckDatabase(true);
        listEvents.addAll(bonitaProperties.load());
        return listEvents;
    }

    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* Import/export */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */
    private static String cstExportMapListConf = "conf";
    private static String cstExportMapDateExport = "dateexport";
    private static String cstExportMapUser = "user";

    private static String cstExportConfContent = "content";
    private static String cstExportConfName = "name";
    private static String cstExportConfDescription = "description";

    /**
     * Export
     * 
     * @param listName
     * @param pageName
     * @param tenantId
     * @return
     */
    public StatusDAO exportConfs(List<String> listNames, String userName, String pageName, long tenantId) {

        StatusDAO statusDAO = new StatusDAO();

        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);
        bonitaProperties.setCheckDatabase(false);

        statusDAO.listEvents.addAll(bonitaProperties.load());
        logger.info("BonitaProperties.exportconf: loadproperties done, events = " + statusDAO.listEvents.size());

        Map<String, Object> exportMap = new HashMap<String, Object>();
        List<Map<String, Object>> listConfResult = new ArrayList<Map<String, Object>>();
        exportMap.put(cstExportMapDateExport, sdf.format(new Date()));
        exportMap.put(cstExportMapUser, userName);
        exportMap.put(cstExportMapListConf, listConfResult);
        statusDAO.containerName = "";

        for (String name : listNames) {
            if (statusDAO.containerName.length() > 0)
                statusDAO.containerName += "_";
            statusDAO.containerName += name;
            Map<String, Object> oneExport = new HashMap<String, Object>();
            String jsonConfiguration = bonitaProperties.getProperty(cstPropertiesConfig + name);
            oneExport.put(cstExportConfContent, jsonConfiguration);
            oneExport.put(cstExportConfName, name);
            oneExport.put(cstExportConfDescription, bonitaProperties.getProperty(cstPropertiesDescription + name));
            listConfResult.add(oneExport);
        }
        if (statusDAO.containerName.length() == 0)
            statusDAO.containerName = "MeteorTest";

        statusDAO.containerName += ".zip";

        String exportSentence = JSONValue.toJSONString(exportMap);

        // now, zip the file
        statusDAO.containerZip = new ByteArrayOutputStream();
        try {
            ZipOutputStream zos = new ZipOutputStream(statusDAO.containerZip);
            ZipEntry ze = new ZipEntry(cstZipNameEntry);
            zos.putNextEntry(ze);

            zos.write(exportSentence.getBytes());

            zos.closeEntry();

            // remember close it
            zos.close();
        } catch (IOException e) {
            statusDAO.listEvents.add(new BEvent(EventExportFailed, e, "export"));
        }
        return statusDAO;
    }

    /**
     * @param fileName
     * @param pageDirectory
     * @param pageName
     * @param tenantId
     * @return
     */
    public StatusDAO importConfs(String fileName, boolean getListNameConfiguration, File pageDirectory, String pageName, long tenantId) {
        StatusDAO statusDAO = new StatusDAO();

        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);
        bonitaProperties.setCheckDatabase(false);

        statusDAO.listEvents.addAll(bonitaProperties.load());

        if (BEventFactory.isError(statusDAO.listEvents))
            return statusDAO;
        String configurationImported = "";

        List<String> listParentTmpFile = new ArrayList<String>();
        try {
            listParentTmpFile.add(pageDirectory.getCanonicalPath() + "/../../../tmp/");
            listParentTmpFile.add(pageDirectory.getCanonicalPath() + "/../../");
        } catch (Exception e) {
            statusDAO.listEvents.add(new BEvent(EventPageDirectoryExportFailed, e, ""));
            logger.info("SnowMobileAccess : error get CanonicalPath of pageDirectory[" + e.toString() + "]");
            return statusDAO;
        }
        File completefileName = null;
        String allPathChecked = "";
        for (String pathTemp : listParentTmpFile) {
            allPathChecked += pathTemp + fileName + ";";
            if (fileName.length() > 0 && (new File(pathTemp + fileName)).exists()) {
                completefileName = (new File(pathTemp + fileName)).getAbsoluteFile();
                logger.info("meteorDAO.importConfs : FOUND [" + completefileName + "]");
            }
        }

        if (!completefileName.exists()) {
            statusDAO.listEvents.add(new BEvent(EventFileUploadedNotFound, "File[" + fileName + "] not found in path[" + allPathChecked + "]"));
            return statusDAO;
        }

        boolean foundImportFile = false;
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(completefileName));

            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                if (ze.getName().equals(cstZipNameEntry)) {
                    foundImportFile = true;
                    final byte[] buffer = new byte[1024];

                    final ByteArrayOutputStream bosBuffer = new ByteArrayOutputStream();
                    int len = 0;
                    while ((len = zis.read(buffer)) > 0) {
                        bosBuffer.write(buffer, 0, len);
                    }
                    String fileContentJson = bosBuffer.toString("UTF-8");

                    // import now
                    Object exportMap = JSONValue.parse(fileContentJson);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> listConfs = (List<Map<String, Object>>) ((Map<String, Object>) exportMap).get(cstExportMapListConf);

                    for (Map<String, Object> oneExport : listConfs) {
                        String name = (String) oneExport.get(cstExportConfName);
                        String content = (String) oneExport.get(cstExportConfContent);
                        String description = (String) oneExport.get(cstExportConfDescription);
                        bonitaProperties.setProperty(cstPropertiesConfig + name, content);
                        bonitaProperties.setProperty(cstPropertiesDescription + name, description);
                        configurationImported += name + ",";

                        // return the last one directly on the screen
                        statusDAO.configuration = new Configuration();
                        statusDAO.configuration.name = name;
                        statusDAO.configuration.content = content;
                        statusDAO.configuration.description = description;

                    }

                    statusDAO.listEvents.addAll(bonitaProperties.store());
                    bosBuffer.close();
                }
                ze = zis.getNextEntry();
            }
            zis.close();

            if (!foundImportFile) {
                statusDAO.listEvents.add(new BEvent(EventConfigurationStructureFailed, "file[" + cstZipNameEntry + "] not found in the Zip file;"));
            }
        } catch (final IOException ie) {
            statusDAO.listEvents.add(new BEvent(EventConfigurationStructureFailed, ie, ""));

        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe("MeteorDAO.Import : " + exceptionDetails);
            statusDAO.listEvents.add(new BEvent(EventConfigurationStructureFailed, e, ""));

        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (final IOException e1) {
                }
            }

        }

        if (getListNameConfiguration)
            statusDAO.listNamesAllConfigurations = getListConfig(bonitaProperties);

        if (!BEventFactory.isError(statusDAO.listEvents))
            statusDAO.listEvents.add(new BEvent(EventConfigurationImported, configurationImported));
        return statusDAO;
    }

    /*
     * BonitaProperties bonitaProperties = new BonitaProperties(
     * pageResourceProvider, apiSession.getTenantId() );
     * listEventsConfig.addAll( bonitaProperties.load() );
     * logger.info("BonitaProperties.saveConfig: loadproperties done, events = "
     * +listEventsConfig.size() );
     * bonitaProperties.remove( cstPropertiesConfig+name );
     * listEventsConfig.addAll( bonitaProperties.store() ); if (!
     * BEventFactory.isError( listEventsConfig )) listEventsConfig.add( new
     * BEvent(EventConfigurationRemoved, "Configuration "+name));
     * logger.
     * info("BonitaProperties.saveConfig store properties  done, events = "
     * +listEvents.size() ); answer = new HashMap<String,Object>();
     * answer.put("listeventsconfig", BEventFactory.getHtml(listEventsConfig));
     * answer.put("configList", getListConfig( bonitaProperties ));
     */
    /*
     * return all the different configuration detected
     */
    private List<Map<String, Object>> getListConfig(BonitaProperties bonitaProperties) {
        Map<String, Map<String, Object>> mapConfig = new HashMap<String, Map<String, Object>>();

        for (Object key : bonitaProperties.keySet()) {
            String keySt = key.toString();
            if (keySt.startsWith(cstPropertiesConfig)) {
                keySt = keySt.substring(cstPropertiesConfig.length());
                if (keySt.indexOf(BonitaProperties.cstMarkerSplitTooLargeKey) > 0)
                    keySt = keySt.substring(0, keySt.indexOf(BonitaProperties.cstMarkerSplitTooLargeKey));
                Map<String, Object> oneConf = mapConfig.get(keySt);
                if (oneConf == null)
                    oneConf = new HashMap<String, Object>();
                oneConf.put( MeteorAPI.cstJsonConfigListName, keySt);
                mapConfig.put(keySt, oneConf);
            }
            if (keySt.startsWith(cstPropertiesDescription)) {
                keySt = keySt.substring(cstPropertiesDescription.length());
                if (keySt.indexOf(BonitaProperties.cstMarkerSplitTooLargeKey) > 0)
                    keySt = keySt.substring(0, keySt.indexOf(BonitaProperties.cstMarkerSplitTooLargeKey));
                Map<String, Object> oneConf = mapConfig.get(keySt);
                if (oneConf == null)
                    oneConf = new HashMap<String, Object>();
                oneConf.put( MeteorAPI.cstJsonConfigListDescription, bonitaProperties.get(keySt));
                mapConfig.put(keySt, oneConf);
            }
        } // end of the collect

        List<Map<String, Object>> listConfig = new ArrayList<Map<String, Object>>();
        for (Object key : mapConfig.keySet()) {
            if (key.toString().trim().length() > 0)
                listConfig.add(mapConfig.get(key));
        }

        Collections.sort(listConfig, new Comparator<Map<String, Object>>() {

            public int compare(Map<String, Object> s1, Map<String, Object> s2) {
                return s1.get("name").toString().toLowerCase().compareTo(s2.get("name").toString().toLowerCase());
            }
        });

        return listConfig;
    }
}
