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
import org.bonitasoft.meteor.MeteorAccess.StartParameters;
import org.bonitasoft.meteor.MeteorAccess.StatusParameters;
import org.bonitasoft.meteor.MeteorSimulation.STATUS;
import org.bonitasoft.meteor.scenario.Scenario;
import org.bonitasoft.log.event.BEventFactory;

public class MeteorOperation {

	private static BEvent EventNoSimulation = new BEvent(MeteorOperation.class.getName(), 1, Level.APPLICATIONERROR, "No simulation", "No simulation found with this ID", "No status can't be give because the simulation is not retrieved", "Check simulationId");

	private static BEvent EventCheckNothingToStart = new BEvent(MeteorOperation.class.getName(), 2, Level.APPLICATIONERROR, "Nothing to start", "No robots can start", "No test can be done if all Robot and Case are equals to 0", "If you set a number of robot, then set a number of case(or inverse)");

	public static boolean simulation = false;
	public static int countRefresh = 0;
	public static Logger logger = Logger.getLogger(MeteorOperation.class.getName());
	public static Map<Long, MeteorSimulation> simulationInProgress = new HashMap<Long, MeteorSimulation>();

	public static class MeteorResult {
		public HashMap<String, Object> result = new HashMap<String, Object>();
		public List<BEvent> listEvents = new ArrayList<BEvent>();
		public MeteorSimulation.STATUS status;

		public HashMap<String, Object> getMap() {
			result.put(MeteorAccess.cstParamResultListEventsSt, BEventFactory.getHtml(listEvents));
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
	public static MeteorResult start(final StartParameters startParameters, final APIAccessor apiAccessor, final long tenantId) {
		final MeteorResult meteorResult = new MeteorResult();
		final MeteorSimulation meteorSimulation = new MeteorSimulation();

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
			meteorResult.result.put(MeteorAccess.cstParamResultSimulationId, String.valueOf(meteorSimulation.getId()));

			// first, reexplore the list of process / activity
			final MeteorProcessDefinitionList meteorProcessDefinitionList = new MeteorProcessDefinitionList();
			// listEvents.addAll(
			// meteorProcessDefinitionList.calculateListProcess(processAPI));

			// second, update this list by the startParameters

			// startParameters can have multiple source : listOfProcess,
			// listOfScenario...

			// 1. ListOfProcess
			// ListProces pilot the different information to creates robots
			meteorResult.listEvents.addAll(meteorProcessDefinitionList.fromList(startParameters.listOfProcesses, null));
			meteorResult.listEvents.addAll(meteorProcessDefinitionList.initialize(tenantId));
			if (BEventFactory.isError(meteorResult.listEvents)) {
				logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : NOROBOT - ERROR in InitializeProcess, end");
				meteorResult.status = MeteorSimulation.STATUS.NOROBOT;
				return meteorResult;
			}

			// Ok, now let's look on the processDefinition list, and for each
			// robots
			// defined, let's register it in the simulation
			meteorResult.listEvents.addAll(meteorProcessDefinitionList.registerInSimulation(meteorSimulation, apiAccessor));

			// 2. Scenario : cmd et groovy
			for (final Map<String, Object> mapScenario : startParameters.listOfScenarii) {
				final Scenario meteorScenario = new Scenario(apiAccessor, tenantId);
				meteorScenario.fromMap(mapScenario);
				meteorResult.listEvents.addAll(meteorScenario.registerInSimulation(meteorSimulation, apiAccessor));

			}

			logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : Start ? ");

			if (meteorSimulation.getNumberOfRobots() == 0) {
				logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : Nothing to start");
				// listEvents.add()
				// it's possible if we have a scenario
				meteorResult.listEvents.add(new BEvent(EventCheckNothingToStart, "Nothing to start"));

				meteorResult.status = MeteorSimulation.STATUS.NOROBOT;
			} else {
				meteorSimulation.runTheSimulation();
				logger.info(" &~~~~~~~& MeteorOperation.Start SIMULID[" + meteorSimulation.getId() + "] : STARTED !");

				meteorResult.result.putAll(meteorSimulation.getDetailStatus());
				meteorResult.listEvents.add(MeteorSimulation.EventStarted);
				meteorResult.status = MeteorSimulation.STATUS.STARTED;
			}
		} catch (Error er) {
			StringWriter sw = new StringWriter();
			er.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			meteorResult.listEvents.add(new BEvent(MeteorSimulation.EventLogBonitaException, er.toString()));
			logger.severe("meteorOperation.Error " + er + " at " + exceptionDetails);
			meteorResult.status = MeteorSimulation.STATUS.DONE;

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			meteorResult.listEvents.add(new BEvent(MeteorSimulation.EventLogBonitaException, e, ""));
			logger.severe("meteorOperation.Error " + e + " at " + exceptionDetails);
			meteorResult.status = MeteorSimulation.STATUS.DONE;

		}
		return meteorResult;
	}

	/**
	 * @param processAPI
	 * @return
	 */
	public static MeteorResult getStatus(final StatusParameters statusParameters, final APIAccessor apoAccessor) {
		final MeteorResult meteorResult = new MeteorResult();
		statusParameters.decodeFromJsonSt();

		// return all simulation in progress
		List<Map<String, Object>> listSimulations = new ArrayList<Map<String, Object>>();
		for (final MeteorSimulation simulation : simulationInProgress.values()) {
			Map<String, Object> oneSimulation = new HashMap<String, Object>();
			oneSimulation.put("id", simulation.getId());
			listSimulations.add(oneSimulation);
		}
		meteorResult.result.put("listSimulations", listSimulations);

		final MeteorSimulation meteorSimulation = simulationInProgress.get(statusParameters.simulationId);
		if (meteorSimulation == null) {
			String allSimulations = "";
			for (final MeteorSimulation simulation : simulationInProgress.values()) {
				allSimulations += simulation.getId() + ",";
			}
			meteorResult.listEvents.add(new BEvent(EventNoSimulation, "SimulationId[" + statusParameters.simulationId + "] allSimulation=[" + allSimulations + "]"));
			meteorResult.status = MeteorSimulation.STATUS.NOSIMULATION;
			return meteorResult;
		}

		logger.info("MeteorOperation.Status");
		meteorResult.status = meteorSimulation.getStatus();
		meteorResult.result.putAll(meteorSimulation.getDetailStatus());

		return meteorResult;
	}
}
