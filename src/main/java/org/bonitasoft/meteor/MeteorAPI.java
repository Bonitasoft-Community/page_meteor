package org.bonitasoft.meteor;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

import org.json.simple.JSONValue;

import org.bonitasoft.command.BonitaCommandDeployment;
import org.bonitasoft.command.BonitaCommandDeployment.DeployStatus;
import org.bonitasoft.command.BonitaCommandDescription;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

import org.bonitasoft.meteor.cmd.CmdMeteor;
import org.bonitasoft.meteor.scenario.experience.MeteorScenarioExperience;
import org.bonitasoft.meteor.scenario.experience.MeteorScenarioExperience.MeteorExperienceParameter;
import org.bonitasoft.meteor.scenario.process.MeteorScenarioProcess;
import org.bonitasoft.meteor.scenario.process.MeteorScenarioProcess.ListProcessParameter;

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
    /** all dialog between Angular and Java are saved here */
    public final static String CST_JSON_LISTEVENTS = "listevents";

    public final static String CST_JSON_CONFIGLIST = "configList";
    public final static String CST_JSON_CONFIG_LISTNAME = "name";
    public final static String CST_JSON_CONFIGLIST_DESCRIPTION = "description";
    public final static String CSTJSON_PROCESSES = "processes";
    public final static String CSTJSON_MODE = "mode";
    public final static String CSTJSON_SCENARIONAME = "scenarioname";
    public final static String CSTJSON_TIMEMAXINMS = "timemaxinms";

    static Logger logger = Logger.getLogger(MeteorAPI.class.getName());
    static String logHeader = "MeteorAPI ~~ ";

  
    // result of information
    // public static String cstParamResultStatus = "simulationstatus";

    public MeteorAPI() {
        // Nothing special
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

    public Map<String, Object> startup(File pageDirectory, final CommandAPI commandAPI, final ProcessAPI processAPI, final PlatformAPI platFormAPI, long tenantId) {

        // deploy the command now, initialise all which is needed
        DeployStatus deployStatus = deployCommand(pageDirectory, commandAPI, platFormAPI, tenantId);

        // first process
        // ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJsonSt );
        // actionAnswer.setResponse( meteorAPI.getListProcesses( listProcessParameter, processAPI));

        Map<String, Object> responseMap = null;
        // second configuration
        MeteorDAO meteorDAO = MeteorDAO.getInstance();
        MeteorDAO.StatusDAO statusDao = meteorDAO.initialize(tenantId);
        if (BEventFactory.isError(statusDao.listEvents))
            responseMap = statusDao.getMap();
        else {
            statusDao = meteorDAO.getListNames(tenantId);
            responseMap = statusDao.getMap();
        }

        responseMap.put("StatusDeployment ", "New deploy" + deployStatus.newDeployment + " alread" + deployStatus.alreadyDeployed);
        // complete with deployment status
        if (BEventFactory.isError(deployStatus.listEvents)) {
            responseMap.put("deploimenterr", "Error during deploiment");
        } else {
            if (deployStatus.newDeployment)
                responseMap.put("deploimentsuc", "Command deployed with success;");
            else if (!deployStatus.alreadyDeployed)
                responseMap.put("deploimentsuc", "Command already deployed;");
            
            // get the status then
            MeteorClientAPI meteorClientAPI = new MeteorClientAPI();
            StatusParameters statusParameters = new StatusParameters();
            responseMap.putAll( meteorClientAPI.getStatus(statusParameters, processAPI, commandAPI, tenantId));

            
        }
        return responseMap;
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
        logger.fine(logHeader + "GetListProcess-2");
        final Map<String, Object> result = new HashMap<>();

        MeteorScenarioProcess processDefinitionList = new MeteorScenarioProcess("");
        processDefinitionList.calculateListProcess(processAPI);

        result.put(CST_JSON_LISTEVENTS, BEventFactory.getHtml(processDefinitionList.getListEventCalculation()));
        result.put(CSTJSON_PROCESSES, processDefinitionList.toJson(listProcessParameter));
        return result;
    }

    /* ************************************************************ */
    /*                                                              */
    /* Experience */
    /*                                                              */
    /* ************************************************************ */
    public Map<String, Object> experienceAction(MeteorExperienceParameter meteorExperienceParameter, ProcessAPI processAPI, IdentityAPI identityAPI) {
        MeteorScenarioExperience meteorExperience = new MeteorScenarioExperience("");
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

        return bonitaCommand.checkAndDeployCommand(commandDescription, true, tenantId, commandAPI, platFormAPI);

    }

    private BonitaCommandDescription getMeteorCommandDescription(File pageDirectory) {

        BonitaCommandDescription commandDescription = new BonitaCommandDescription(CmdMeteor.CSTCOMMANDNAME, pageDirectory);
        commandDescription.forceDeploy = false;
        commandDescription.mainCommandClassName = CmdMeteor.class.getName();
        commandDescription.mainJarFile = "bonita-meteor-4.0.0.jar";
        commandDescription.commandDescription = CmdMeteor.CSTCOMMANDDESCRIPTION;

        commandDescription.addJarDependencyLastVersion("bonita-event", "1.10.0", "bonita-event-1.10.0.jar");
        commandDescription.addJarDependencyLastVersion("bonita-properties", "2.8.2", "bonita-properties-2.8.2.jar");
        commandDescription.addJarDependency("bonita-casedetails", "1.1.3", "bonita-casedetails-1.1.3.jar");

        return commandDescription;
    }

    public static class StatusParameters {

        public long simulationId = -1;
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
            logger.fine(logHeader + "MeteorAPI JsonSt[" + jsonSt + "] simulationId[" + simulationId + "]");
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
    @SuppressWarnings("rawtypes")
    public Map<String, Object> start(final MeteorStartParameters startParameters, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {

        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.start() parameter=" + startParameters.toString());
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(CmdMeteor.CSTCOMMANDNAME);

        final HashMap<String, Serializable> parameters = new HashMap<>();
        parameters.put(CmdMeteor.CSTPARAM_COMMANDNAMESTARTPARAMS, startParameters.toJson());
        // parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStart);

        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.start() Call Command");
        Map<String, Object> resultCommand = bonitaCommand.callCommand(CmdMeteor.VERBE.START.toString(), parameters, tenantId, commandAPI);
        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.start() : END " + resultCommand);
        return resultCommand;
    }

    /**
     * @param name
     * @param processAPI
     * @param commandAPI
     * @param tenantId
     * @return
     */
    public Map<String, Object> startFromScenarioName(String name, ProcessAPI processAPI, CommandAPI commandAPI, long tenantId) {
        MeteorClientAPI meteorClientAPI = new MeteorClientAPI();
        return meteorClientAPI.startFromScenarioName(name, processAPI, commandAPI, tenantId);

    }
    /*
     * ********************************************************************
     */
    /*                                                                      */
    /* Start a new test game */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * getStatus
     */
    public Map<String, Object> getStatus(final StatusParameters statusSimulation, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {

        MeteorClientAPI meteorClientAPI = new MeteorClientAPI();
        return meteorClientAPI.getStatus(statusSimulation, processAPI, commandAPI, tenantId);
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* API To get information for an application */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public class UnitTestResult {

        public int percentunittest = 0;
        public long simulationId=0;
        public MeteorConst.SIMULATIONSTATUS status;
        public String detailExplanation;
        public List<BEvent> listEvents = new ArrayList();
        // complete detail on each robots
        public List<Map<String, Object>> listRobots;

        private StringBuilder log = new StringBuilder();
        public void addLog( String message ) {
            log.append( message+",");
            logger.info("MeteorAPI "+message);
        }
        public String getLog() {
            return log.toString();
        }
    }

    /**
     * return the list of scenario available
     * 
     * @param tenantId
     * @return
     */
    public List<String> getListScenarii(long tenantId) {
        List<String> listNamesScenario = new ArrayList<>();
        MeteorDAO meteorDAO = MeteorDAO.getInstance();
        MeteorDAO.StatusDAO statusDao = meteorDAO.getListNames(tenantId);
        for (Map<String, Object> scenario : statusDao.listNamesAllConfigurations) {
            listNamesScenario.add(MeteorToolbox.getParameterString(scenario, "name", ""));
        }
        return listNamesScenario;
    }

    /**
     * Start a unit test. The thread waits unit the scenario is executed.
     * 
     * @param scenarioName
     * @param processAPI
     * @param commandAPI
     * @param tenantId
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public UnitTestResult startUnitTest(final String scenarioName, final long timeMaxInMs, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {

        UnitTestResult unitTestResult = new UnitTestResult();

        // first, load the scenario
        MeteorDAO meteorDAO = MeteorDAO.getInstance();
        MeteorDAO.StatusDAO statusDao = meteorDAO.load(scenarioName, tenantId);
        if (BEventFactory.isError(statusDao.listEvents)) {
            unitTestResult.status = MeteorConst.SIMULATIONSTATUS.NOSCENARIO;
            unitTestResult.detailExplanation = "Scenario [" + scenarioName + "] does not exist";
            unitTestResult.listEvents.addAll(statusDao.listEvents);
            return unitTestResult;
        }
        // scenario is loaded in configuration.content
        MeteorStartParameters startParameters = MeteorStartParameters.getInstanceFromJsonSt(statusDao.configuration.content);
        // complete information
        startParameters.setExecutionMode( MeteorConst.EXECUTIONMODE.UNITTEST );
        startParameters.setScenarioName( scenarioName );
        startParameters.setTenantId( tenantId);
        startParameters.setTimeMaxInMs( timeMaxInMs);
        
        // now we can execute this scenario
        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.startUnitTest() parameter=" + startParameters.toString());
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(CmdMeteor.CSTCOMMANDNAME);

        final HashMap<String, Serializable> parameters = new HashMap<>();
        parameters.put(CmdMeteor.CSTPARAM_COMMANDNAMESTARTPARAMS, startParameters.toJson());
        // parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStart);

        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.startUnitTest() Call Command");
        Map<String, Object> resultCommand = bonitaCommand.callCommand(CmdMeteor.VERBE.START.toString(), parameters, tenantId, commandAPI);
        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.startUnitTest() : END " + resultCommand);
        unitTestResult.simulationId = MeteorToolbox.getParameterLong(resultCommand, MeteorConst.CSTJSON_SIMULATIONID, 0L);
        unitTestResult.percentunittest = MeteorToolbox.getParameterInteger(resultCommand, MeteorConst.CSTJSON_PERCENTUNITTEST, 0);
        try {
            unitTestResult.status = MeteorConst.SIMULATIONSTATUS.valueOf(MeteorToolbox.getParameterString(resultCommand, MeteorConst.CSTJSON_GLOBALSTATUS, null));
        } catch (Exception e) {
            unitTestResult.status = MeteorConst.SIMULATIONSTATUS.NOSIMULATION;
        }
        StringBuilder detailedStatus = new StringBuilder();
        List<Map<String, Object>> listRobots = (List<Map<String, Object>>) MeteorToolbox.getParameterList(resultCommand, MeteorConst.CSTJSON_ROBOTS, new ArrayList());
        // listRobots can't be null here
        StringBuilder logAggregate = new StringBuilder();
        logAggregate.append(resultCommand.get("log"));
        
        detailedStatus.append("Numbers of tests: " + listRobots.size());
        for (Map<String, Object> robotInfo : listRobots) {
            detailedStatus.append( robotInfo.get("name") + ":");
            detailedStatus.append("[" + robotInfo.get("status") + "] ");
            detailedStatus.append(robotInfo.get("explanationerror") + " - ");
            logAggregate.append( "robot["+robotInfo.get("name")+"]: "+robotInfo.get("log") +" // ");
        }
        unitTestResult.listRobots=listRobots;
        unitTestResult.detailExplanation = detailedStatus.toString();
        unitTestResult.addLog( logAggregate.toString() );
        return unitTestResult;
    }

    /**
     * @param scenarioName
     * @param processAPI
     * @param commandAPI
     * @param tenantId
     * @param timeMaxInMs
     * @return
     */
    public UnitTestResult startUnitTestLimited(final String scenarioName,  long  timeMaxInMs, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {
        
        ExecutorService executor = Executors.newFixedThreadPool(1);
        
        ExecutorUnitTest executorUnitTest = new ExecutorUnitTest(this, scenarioName, timeMaxInMs, processAPI, commandAPI, tenantId);
        Future<?> future = executor.submit(executorUnitTest);
        long beginTest = System.currentTimeMillis();
        try {
            if (timeMaxInMs < 3*60*1000)
                timeMaxInMs=3*60*1000; // 3 mn
            future.get(timeMaxInMs, TimeUnit.MILLISECONDS); 
        } catch (InterruptedException e) { //     <-- possible error cases
            executorUnitTest.unitTestResult.addLog("InterruptedException : "+e.getMessage());
            executorUnitTest.unitTestResult.status = MeteorConst.SIMULATIONSTATUS.FAILEDUNITTEST;
        } catch (ExecutionException e) {
            executorUnitTest.unitTestResult.addLog("ExecutionException : "+e.getMessage());
            executorUnitTest.unitTestResult.status = MeteorConst.SIMULATIONSTATUS.FAILEDUNITTEST;
        } catch (TimeoutException e) {
            executorUnitTest.unitTestResult.addLog("Kill Execution (longueur than "+timeMaxInMs+")");
            future.cancel(true); //     <-- interrupt the job
            executorUnitTest.unitTestResult.status = MeteorConst.SIMULATIONSTATUS.FAILEDUNITTEST;
            
        }
        long endTest = System.currentTimeMillis();
        executorUnitTest.unitTestResult.addLog("End of execution. Duration["+ (endTest -beginTest)+"] Max is ("+timeMaxInMs+")");
        

        // wait all unfinished tasks for 2 sec

        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executorUnitTest.unitTestResult.addLog("Task still running, ForceShutdown now");
                // force them to quit by interrupting
                executor.shutdownNow();
            }
        } catch (Exception e) {

        }
        return executorUnitTest.unitTestResult;
    }

    private class ExecutorUnitTest implements Runnable {

        private MeteorAPI meteorAPI;
        protected UnitTestResult unitTestResult;
        private String scenarioName;
        private long timeMaxInMs;
        private ProcessAPI processAPI;
        private CommandAPI commandAPI;
        private long tenantId;

        protected ExecutorUnitTest(MeteorAPI meteorAPI, final String scenarioName, final long timeMaxInMs, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {
            this.meteorAPI = meteorAPI;
            this.scenarioName = scenarioName;
            this.processAPI = processAPI;
            this.commandAPI = commandAPI;
            this.tenantId = tenantId;
            this.timeMaxInMs  = timeMaxInMs;
        }

        @Override
        public void run() {
            unitTestResult = meteorAPI.startUnitTest(scenarioName, timeMaxInMs, processAPI, commandAPI, tenantId);
            unitTestResult.addLog("End of ExecutorUnitTest");
        }

    }
}
