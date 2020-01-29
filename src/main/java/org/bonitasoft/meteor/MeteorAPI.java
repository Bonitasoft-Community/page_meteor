package org.bonitasoft.meteor;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

import org.bonitasoft.command.BonitaCommandDeployment;
import org.bonitasoft.command.BonitaCommandDeployment.DeployStatus;
import org.bonitasoft.command.BonitaCommandDescription;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.cmd.CmdMeteor;
import org.bonitasoft.meteor.scenario.experience.MeteorScenarioExperience;
import org.bonitasoft.meteor.scenario.experience.MeteorScenarioExperience.MeteorExperienceParameter;
import org.bonitasoft.meteor.scenario.process.MeteorScenarioProcess;
import org.bonitasoft.meteor.scenario.process.MeteorScenarioProcess.ListProcessParameter;
import org.json.simple.JSONValue;

public class MeteorAPI {

    /**
     * ********************************************************************************
     * * This class is the main/only access for the Groovy - getListProcess=>
     * ask ProcessDefinitionList The execution have to work in background. In
     * order to protect the code (because Bonita reload the JAR at every call in
     * debug mode) then the execution is done via a Command. At start, we check
     * if we need to redeploy the command (normaly, first execution or only if
     * the version change) . start ==> CmdMeteor => MeteorOperation =>
     * MeteorSimulation CmdMeteor : this is the command. Only command operation
     * (deploy, send order and then execute) MeteorOperation (CommandSite) :
     * multiple run can be executed (so multiple MeteorSimulation).
     * MeteorOperation create a new MeteorSimulation at Start and get a ID.
     * meteorOperation save all <ID>-<MeteorOperation> an ID is returned. .
     * status => CmdMeteor => MeteorOperation => MeteorSimulation
     * MeteorOperation get the correct MeteorSimulation from the Id
     * ********************************************************************************
     */

    
    private static Logger logger = Logger.getLogger(MeteorAPI.class.getName());
    private static String logHeader = "MeteorAPI ~~ ";
    // private static BEvent EventNotDeployed = new BEvent(MeteorAPI.class.getName(), 1, Level.ERROR, "Command not deployed", "The command is not deployed");
    // private static BEvent EventStartError = new BEvent(MeteorAPI.class.getName(), 2, Level.ERROR, "Error during starting the simulation", "Check the error", "No test are started", "See the error");
    // private static BEvent EventDeployCommandGroovyScenario = new BEvent(MeteorAPI.class.getName(), 3, Level.ERROR, "Groovy Command can't be created", "The Groovy Scenario needs special command to be deployed. The deployment of the command failed", "The groovy scenario can't be executed", "Check the error");

    /** all dialog between Angular and Java are saved here */
    public final static String cstJsonListEvents = "listevents";

    public final static String cstJsonConfigList = "configList";
    public final static String cstJsonConfigListName = "name";
    public final static String cstJsonConfigListDescription = "description";
    
    // result of information
    // public static String cstParamResultStatus = "simulationstatus";

    public MeteorAPI() {
    }

    /**
     * get the current object
     *
     * @return
     */
    public static MeteorAPI getMeteorAPI(final HttpSession httpSession) {
        // logger.info(logHeader+"MeteorAPI.getMeteorAPI -----------");
        // in debug mode, BonitaEngine reload the JAR file every access : so any
        // object saved in the httpSession will get a ClassCastException

        return new MeteorAPI();
    }

    /* ************************************************************ */
    /*                                                              */
    /* Process Scenario */
    /*                                                              */
    /* ************************************************************ */

    /**
     * get the list of all processes visible on the engine
     *
     * @param listProcessParameter
     * @param processAPI
     * @return
     */
    public Map<String, Object> getListProcesses(final ListProcessParameter listProcessParameter, final ProcessAPI processAPI) {
        logger.info(logHeader + "GetListProcess-2");
        final Map<String, Object> result = new HashMap<String, Object>();

        MeteorScenarioProcess processDefinitionList = new MeteorScenarioProcess();
        processDefinitionList.calculateListProcess(processAPI);

        result.put(cstJsonListEvents, BEventFactory.getHtml(processDefinitionList.getListEventCalculation()));
        result.put("processes", processDefinitionList.toJson(listProcessParameter));
        return result;
    }

    /* ************************************************************ */
    /*                                                              */
    /* Experience */
    /*                                                              */
    /* ************************************************************ */
    public Map<String, Object> experienceAction(MeteorExperienceParameter meteorExperienceParameter, ProcessAPI processAPI, IdentityAPI identityAPI) {
        MeteorScenarioExperience meteorExperience = new MeteorScenarioExperience();
        return meteorExperience.action(meteorExperienceParameter, processAPI, identityAPI);

    }

    /* ************************************************************ */
    /*                                                              */
    /* Start */
    /*                                                              */
    /* ************************************************************ */

    /**
     * the meteor motor run as a command (because at each access, the custom
     * page is reloaded, so there are no way to save the file else in Bonita)
     * Attention : the deployment are suppose to not be done at each time
     *
     * @param inputStreamJarFile
     * @param commandAPI
     * @param platFormAPI
     * @return
     */

    public DeployStatus deployCommand(File pageDirectory, final CommandAPI commandAPI, final PlatformAPI platFormAPI, long tenantId) {
        logger.info(logHeader + " Start deployCommand");

        BonitaCommandDescription commandDescription = getMeteorCommandDescription(pageDirectory);
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(commandDescription);

        DeployStatus deployStatus = bonitaCommand.checkAndDeployCommand(commandDescription, true, tenantId, commandAPI, platFormAPI);
        return deployStatus;
    }

    private BonitaCommandDescription getMeteorCommandDescription(File pageDirectory) {

        BonitaCommandDescription commandDescription = new BonitaCommandDescription(CmdMeteor.cstCommandName, pageDirectory);
        commandDescription.forceDeploy = false;
        commandDescription.mainCommandClassName = CmdMeteor.class.getName();
        commandDescription.mainJarFile = "CustomPageMeteor-3.0.0.jar";
        commandDescription.commandDescription = CmdMeteor.cstCommandDescription;

        commandDescription.addJarDependencyLastVersion("bonita-event", "1.7.0", "bonita-event-1.7.0.jar");
        commandDescription.addJarDependencyLastVersion("bonita-properties", "2.1.1", "bonita-properties-2.1.1.jar");
        commandDescription.addJarDependency("bonita-casedetails", "1.0.0", "bonita-casedetails-1.0.0.jar");

        return commandDescription;
    }

    /**
    	 *
    	 *
    	 */
    public static class StartParameters {

        public long tenantId;

        // collect all information, from the JSON. The interpretation will be
        // done in MeteorOperation.start()
        public List<Map<String, Object>> listOfProcesses;
        public List<Map<String, Object>> listOfScenarii;
        /**
         * MapOfExperience contains
         * {
         * "listCasesId": "1003",
         * "scenarii": [
         * {
         * "processname": "experience",
         * "processversion": "1.0",
         * "nbcases": 1,
         * "nbrobs": 1,
         * "timelines": [ ...
         */
        public Map<String, Object> mapOfExperience;

        /**
         * keep the parameters as a JSON to sent it to the command - ArrayList
         * to keep the serialization
         */
        public ArrayList<String> jsonListSt;

        public static StartParameters getInstanceFromJsonSt(final String jsonSt) {
            final StartParameters startParameters = new StartParameters();
            startParameters.jsonListSt = new ArrayList<String>();
            startParameters.jsonListSt.add(jsonSt);
            return startParameters;
        }

        public static StartParameters getInstanceFromJsonList(final ArrayList<String> jsonList) {
            final StartParameters startParameters = new StartParameters();
            startParameters.jsonListSt = jsonList;
            return startParameters;
        }

        /**
         *
         */
        @SuppressWarnings("unchecked")
        public void decodeFromJsonSt() {
            logger.info(logHeader + "decodeFromJsonSt : JsonSt[" + jsonListSt + "]");
            listOfProcesses = new ArrayList<Map<String, Object>>();
            listOfScenarii = new ArrayList<Map<String, Object>>();
            mapOfExperience = new HashMap<String, Object>();

            if (jsonListSt == null) {
                return;
            }
            for (final String jsonSt : jsonListSt) {

                // we can get 2 type of JSON :
                // { 'processes' : [ {..}, {...} ], 'scenarii':[{...}, {...},
                // 'process' : {..}, 'scenario': {} ] }
                // or a list of order
                // [ { 'process': {}, 'process': {}, 'scenario': {..}];

                final Object jsonObject = JSONValue.parse(jsonSt);
                logger.info(logHeader + "MeteorAPI.decodeFromJsonSt : line object [" + jsonObject.getClass().getName() + "] Map ? " + (jsonObject instanceof HashMap) + " line=[" + jsonSt + "] ");

                if (jsonObject instanceof HashMap) {
                    logger.info(logHeader + "MeteorAPI.decodeFromJsonSt : object [" + jsonObject.getClass().getName() + "] is a HASHMAP");

                    final HashMap<String, Object> jsonHash = (HashMap<String, Object>) jsonObject;
                    if (jsonHash.get("processes") != null && jsonHash.get("processes") instanceof Map) {
                        final Map<String, Object> jsonHashProcess = (Map<String, Object>) jsonHash.get("processes") ;
                        if (jsonHashProcess.get("scenarii") !=null)
                            listOfProcesses.addAll((List<Map<String, Object>>) jsonHashProcess.get("scenarii"));
                    }

                    if (jsonHash.get("scenarii") != null && jsonHash.get("scenarii") instanceof Map) {
                        final Map<String, Object> jsonHashScenarii = (Map<String, Object>) jsonHash.get("scenarii") ;
                        if (jsonHashScenarii.get("actions") !=null)
                            listOfScenarii.addAll((List<Map<String, Object>>) jsonHashScenarii.get("actions"));
                    }
                   
                    if (jsonHash.get("experience") != null && jsonHash.get("experience") instanceof Map) {
                        mapOfExperience = (Map<String, Object>) jsonHash.get("experience");
                    }
                } else if (jsonObject instanceof List) {
                    logger.info(logHeader + "MeteorAPI.decodeFromJsonSt : object [" + jsonObject.getClass().getName() + "] is a LIST");
                    final List<Map<String, Map<String, Object>>> jsonList = (List<Map<String, Map<String, Object>>>) jsonObject;
                    for (final Map<String, Map<String, Object>> oneRecord : jsonList) {
                        logger.info(logHeader + "MeteorAPI.decodeFromJsonSt : process [" + oneRecord.get("process") + "] scenario [" + oneRecord.get("scenario") + "]");

                        if (oneRecord.containsKey("process")) {
                            listOfProcesses.add(oneRecord.get("process"));
                        }
                        if (oneRecord.containsKey("scenario")) {
                            listOfScenarii.add(oneRecord.get("scenario"));
                        }
                    }
                }
            }
            logger.info(logHeader + "MeteorAPI.decodeFromJsonSt :  decodeFromJsonSt nbProcess=" + listOfProcesses.size() + " nbScenarii=" + listOfScenarii.size());
        }

        @Override
        public String toString() {
            String value = "";
            if (jsonListSt != null) {
                for (final String jsonSt : jsonListSt) {
                    value += " [" + jsonSt + " ], ";
                }
            }
            return "startParameters " + (value + "                                                                                                      ").substring(0, 60) + "...";
        }
    }

    public static class StatusParameters {

        long simulationId = -1;
        public String jsonSt = null;

        public static StatusParameters getInstanceFromJsonSt(final String jsonSt) {
            final StatusParameters statusParameters = new StatusParameters();
            statusParameters.jsonSt = jsonSt;
            return statusParameters;
        }

        public static StatusParameters getInstanceFromSimulationId(final String simulationId) {
            final StatusParameters statusParameters = new StatusParameters();
            try {
                statusParameters.simulationId = Long.valueOf(simulationId);
            } catch (final Exception e) {
                statusParameters.simulationId = -1;
            }
            return statusParameters;
        }

        public String getJson() {
            if (jsonSt != null)
                return jsonSt;
            if (simulationId != -1)
                return "{\"simulationid\": " + simulationId + "}";
            return null;
        }

        @SuppressWarnings("unchecked")
        public void decodeFromJsonSt() {
            logger.info(logHeader + "MeteorAPI JsonSt[" + jsonSt + "] simulationId[" + simulationId + "]");
            if (jsonSt == null) {
                // already set
                return;
            }
            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);

            simulationId = MeteorToolbox.getParameterLong(jsonHash, "simulationid", -1L);
        }

    }

    /**
     * start a new run test
     *
     * @param startParameters
     * @param processAPI
     * @param commandAPI
     * @return
     */
    public Map<String, Object> start(final StartParameters startParameters, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {

        logger.info(logHeader + "~~~~~~~~~~ MeteorAPI.start() parameter=" + startParameters.toString());
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(CmdMeteor.cstCommandName);
        Map<String, Object> resultCommand = new HashMap<String, Object>();

        final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(CmdMeteor.cstParamCommandNameStartParams, startParameters.jsonListSt);
        // parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStart);

        logger.info(logHeader + "~~~~~~~~~~ MeteorAPI.start() Call Command");
        resultCommand = bonitaCommand.callCommand(CmdMeteor.VERBE.START.toString(), parameters, tenantId, commandAPI);
        logger.info(logHeader + "~~~~~~~~~~ MeteorAPI.start() : END " + resultCommand);
        return resultCommand;
    }
    
    /**
     * 
     * @param name
     * @param processAPI
     * @param commandAPI
     * @param tenantId
     * @return
     */
    public Map<String, Object> startFromScenarioName(String name, ProcessAPI processAPI, CommandAPI commandAPI, long tenantId) {
        logger.info(logHeader + "~~~~~~~~~~ MeteorAPI.startFromName() name=" + name);
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(CmdMeteor.cstCommandName);
        Map<String, Object> resultCommand = new HashMap<String, Object>();

        final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(CmdMeteor.cstParamCommandNameScenarioName, name);
        resultCommand = bonitaCommand.callCommand(CmdMeteor.VERBE.STARTFROMSCENARIONAME.toString(), parameters, tenantId, commandAPI);
        logger.info(logHeader + "~~~~~~~~~~ MeteorAPI.startFromName() : END " + resultCommand);
        return resultCommand;
        
    }
    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* Start a new test game */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */

    /**
     * getStatus
     */
    public Map<String, Object> getStatus(final StatusParameters statusSimulation, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {

        logger.fine(logHeader + "MeteorAPI.getStatus()");
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(CmdMeteor.cstCommandName);
        Map<String, Object> resultCommand = new HashMap<String, Object>();

        final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(CmdMeteor.cstParamCommandNameStatusParams, statusSimulation.getJson());

        // parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStatus);

        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.start() Call Command ["+CmdMeteor.VERBE.STATUS+"]");
        resultCommand = bonitaCommand.callCommand(CmdMeteor.VERBE.STATUS.toString(), parameters, tenantId, commandAPI);

        return resultCommand;
    }

}
