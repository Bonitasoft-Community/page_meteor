package org.bonitasoft.meteor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import org.bonitasoft.meteor.MeteorSimulation.Estimation;
import org.bonitasoft.meteor.cmd.CmdMeteor;
import org.bonitasoft.meteor.scenario.ScenarioCmd;
import org.bonitasoft.meteor.scenario.experience.MeteorScenarioExperience;
import org.bonitasoft.meteor.scenario.process.MeteorScenarioProcess;

public class MeteorOperation {

    private static BEvent eventNoSimulation = new BEvent(MeteorOperation.class.getName(), 1, Level.APPLICATIONERROR, "No simulation", "No simulation found with this ID", "No status can't be give because the simulation is not retrieved", "Check simulationId");

    private static BEvent eventCheckNothingToStart = new BEvent(MeteorOperation.class.getName(), 2, Level.APPLICATIONERROR, "Nothing to start", "No robots can start", "No test can be done if all Robot and Case are equals to 0", "If you set a number of robot, then set a number of case(or inverse)");

    public static boolean simulation = false;
    public static int countRefresh = 0;
    public static Logger logger = Logger.getLogger(MeteorOperation.class.getName());
    public static Map<Long, MeteorSimulation> simulationInProgress = new HashMap<Long, MeteorSimulation>();

    public static class MeteorResult {

        public HashMap<String, Object> result = new HashMap<>();
        public List<BEvent> listEvents = new ArrayList<>();
        public MeteorConst.STATUS status;
        

        /**
         * We must keep MashMap because the JSON parser don't know how to JSONify a Map object
         * @return
         */
        public HashMap<String, Object> getMap() {
            result.put(CmdMeteor.CSTPARAM_RESULTLISTEVENTSST, BEventFactory.getHtml(listEvents));
            // result.put(MeteorAccess.cstParamResultStatus, status == null ? ""
            // : status.toString());
            return result;
        }
    }

    /**
     * do the operation In the Command architecture, this method is call by the
     * Command ATTENTION : this call is done by the Command Thread : no
     * BonitaAPI can be call here, only on the Robot
     * 
     * @param startParameters
     * @param tenantId
     * @param processAPI
     * @return
     */
    public static MeteorResult start(final StartParameters startParameters, final APIAccessor apiAccessor) {
        final MeteorResult meteorResult = new MeteorResult();
        final MeteorSimulation meteorSimulation = new MeteorSimulation(startParameters, apiAccessor);

        try {
            logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] by [" + MeteorOperation.class.getName() + "] : " + startParameters.toString());

            // Decode here the Json
            startParameters.decodeFromJsonSt();

            if (simulation) {

                logger.info("  >>>>>>>>>>>>>>>>>> Simulation <<<<<<<<<<<<<<<< ");

                countRefresh = 0;
                meteorResult.result.put("Start", "at " + new Date());
                return meteorResult;
            }

            simulationInProgress.put(meteorSimulation.getId(), meteorSimulation);
            meteorResult.result.put(CmdMeteor.CSTPARAM_RESULTSIMULATIONID, String.valueOf(meteorSimulation.getId()));

            MeteorScenario[] listMeteorScenario = new MeteorScenario[] { new MeteorScenarioProcess(startParameters.scenarioName ), new MeteorScenarioExperience(startParameters.scenarioName ), new ScenarioCmd(startParameters.scenarioName ) };

            for (MeteorScenario meteorScenario : listMeteorScenario) {
                meteorResult.listEvents.addAll(meteorScenario.registerInSimulation(startParameters, meteorSimulation, apiAccessor));

            }

            // first, reexplore the list of process / activity

            // listEvents.addAll(
            // meteorProcessDefinitionList.calculateListProcess(processAPI));

            // second, update this list by the startParameters

            // startParameters can have multiple source : listOfProcess,
            // listOfScenario...

            // 1. ListOfProcess
            // ListProcess pilot the different information to creates robots
            // meteorResult.listEvents.addAll( meteorScenarioProcess.fromList(startParameters.listOfProcesses, null, apiAccessor.getProcessAPI()));			
            // meteorResult.listEvents.addAll( meteorScenarioProcess.initialize(tenantId));

            if (BEventFactory.isError(meteorResult.listEvents)) {
                logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : NOROBOT - ERROR in InitializeProcess, end");
                meteorResult.status = MeteorConst.STATUS.NOROBOT;
                return meteorResult;
            }

            // Ok, now let's look on the processDefinition list, and for each
            // robots
            // defined, let's register it in the simulation
            // meteorResult.listEvents.addAll(meteorScenarioProcess.registerInSimulation(meteorSimulation, apiAccessor));

            // 3. Scenario : cmd et groovy
            /*
             * for (final Map<String, Object> mapScenario : startParameters.listOfScenarii) {
             * final Scenario meteorScenario = new Scenario(apiAccessor, tenantId);
             * meteorScenario.fromMap(mapScenario);
             * meteorResult.listEvents.addAll(meteorScenario.registerInSimulation(meteorSimulation, apiAccessor));
             * }
             */

            logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : Start ? ");

            if (meteorSimulation.getNumberOfRobots() == 0) {
                logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : Nothing to start");
                // listEvents.add()
                // it's possible if we have a scenario
                meteorResult.listEvents.add(new BEvent(eventCheckNothingToStart, "Nothing to start"));

                meteorResult.status = MeteorConst.STATUS.NOROBOT;
            } else {
                meteorSimulation.runTheSimulation();
                logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : STARTED !");

                meteorResult.result.putAll(meteorSimulation.refreshDetailStatus(apiAccessor));
                
                if (meteorSimulation.getStatus() ==   MeteorConst.STATUS.STARTED)
                    meteorResult.listEvents.add(MeteorSimulation.EventStarted);
                else if (meteorSimulation.getStatus() ==   MeteorConst.STATUS.SUCCESSUNITTEST)
                    meteorResult.listEvents.add(MeteorSimulation.EventSuccessUnitTest);
                // else : will be part of the meteorSimulation.refreshDetailStatus()
                
                meteorResult.status = meteorSimulation.getStatus();
            }
        } catch (Error er) {
            StringWriter sw = new StringWriter();
            er.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            meteorSimulation.setStatus(MeteorConst.STATUS.DONE);

            meteorResult.listEvents.add(new BEvent(MeteorSimulation.EventLogBonitaException, er.toString()));
            logger.severe("meteorOperation.Error " + er + " at " + exceptionDetails);
            meteorResult.status = MeteorConst.STATUS.DONE;

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            meteorSimulation.setStatus(MeteorConst.STATUS.DONE);

            meteorResult.listEvents.add(new BEvent(MeteorSimulation.EventLogBonitaException, e, ""));
            logger.severe("meteorOperation.Error " + e + " at " + exceptionDetails);
            meteorResult.status = MeteorConst.STATUS.DONE;

        }
        return meteorResult;
    }

    /**
     * @param processAPI
     * @return
     */
    public static MeteorResult getStatus(final StatusParameters statusParameters, final APIAccessor apiAccessor) {
        final MeteorResult meteorResult = new MeteorResult();
        statusParameters.decodeFromJsonSt();
        Long currentTime = System.currentTimeMillis();

        // return all simulation in progress
        List<Map<String, Object>> listSimulations = new ArrayList<>();
        for (final MeteorSimulation simulation : simulationInProgress.values()) {
            Map<String, Object> oneSimulation = new HashMap<>();
            oneSimulation.put(MeteorConst.CSTJSON_ID, simulation.getId());
            oneSimulation.put(MeteorConst.CSTJSON_GLOBALSTATUS, simulation.getStatus().toString());
            Estimation estimation = simulation.getEstimatedAdvance();
            oneSimulation.put(MeteorConst.CSTJSON_PERCENTADVANCE, estimation.percentAdvance);
            if (estimation.percentAdvance == 0) {
                // can't calculated any time
            }
            if (estimation.percentAdvance < 100) {
                oneSimulation.put(MeteorConst.CSTJSON_TIMEESTIMATEDDELAY, MeteorToolbox.getHumanDelay(estimation.timeNeedInMs));
                oneSimulation.put(MeteorConst.CSTJSON_TIMEESTIMATEDEND, MeteorToolbox.getHumanDate(new Date(currentTime + estimation.timeNeedInMs)));
            } else
                oneSimulation.put(MeteorConst.CSTJSON_TIMEESTIMATEDEND, MeteorToolbox.getHumanDate(simulation.getDateEndSimulation()));

            listSimulations.add(oneSimulation);
        }
        meteorResult.result.put("listSimulations", listSimulations);

        final MeteorSimulation meteorSimulation = simulationInProgress.get(statusParameters.simulationId);
        if (meteorSimulation == null) {
            String allSimulations = "";
            for (final MeteorSimulation simulation : simulationInProgress.values()) {
                allSimulations += simulation.getId() + ",";
            }
            meteorResult.listEvents.add(new BEvent(eventNoSimulation, "SimulationId[" + statusParameters.simulationId + "] allSimulation=[" + allSimulations + "]"));
            meteorResult.status = MeteorConst.STATUS.NOSIMULATION;
            meteorResult.result.put(MeteorConst.CSTJSON_STATUS, MeteorConst.STATUS.NOSIMULATION.toString());
            return meteorResult;
        }

        logger.info("MeteorOperation.Status");
        meteorResult.status = meteorSimulation.getStatus();
        meteorResult.result.putAll(meteorSimulation.refreshDetailStatus(apiAccessor));

        return meteorResult;
    }
}
