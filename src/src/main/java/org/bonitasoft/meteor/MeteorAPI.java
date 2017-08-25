package org.bonitasoft.meteor;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandExecutionException;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.command.CommandParameterizationException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.ListProcessParameter;
import org.bonitasoft.meteor.cmd.CmdMeteor;
import org.bonitasoft.meteor.cmd.CmdMeteor.JarDependencyCommand;
import org.bonitasoft.meteor.scenario.groovy.MeteorRobotGroovyScenario;
import org.bonitasoft.log.event.BEventFactory;
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

	private static BEvent EventNotDeployed = new BEvent(MeteorAPI.class.getName(), 1, Level.ERROR, "Command not deployed", "The command is not deployed");
	private static BEvent EventStartError = new BEvent(MeteorAPI.class.getName(), 2, Level.ERROR, "Error during starting the simulation", "Check the error", "No test are started", "See the error");
	private static BEvent EventDeployCommandGroovyScenario = new BEvent(MeteorAPI.class.getName(),3, Level.ERROR, "Groovy Command can't be created", "The Groovy Scenario needs special command to be deployed. The deployment of the command failed", "The groovy scenario can't be executed", "Check the error");

	MeteorProcessDefinitionList processDefinitionList = new MeteorProcessDefinitionList();

	// result of information
	public final static String cstParamResultListEventsSt = "listevents";
	public final static String cstParamResultSimulationId = "simulationid";
	// public static String cstParamResultStatus = "simulationstatus";

	MeteorSimulation meteorSimulation = new MeteorSimulation();

	public MeteorAPI() {
	}

	/**
	 * get the current object
	 *
	 * @return
	 */
	public static MeteorAPI getMeteorAPI(final HttpSession httpSession) {
		logger.info("MeteorAPI.getMeteorAPI -----------");
		// in debug mode, BonitaEngine reload the JAR file every access : so any
		// object saved in the httpSession will get a ClassCastException

		return new MeteorAPI();
	}

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Get Information to start a new test */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */

	/**
	 * get the list of all processes visible on the engine
	 *
	 * @param listProcessParameter
	 * @param processAPI
	 * @return
	 */
	public Map<String, Object> getListProcesses(final ListProcessParameter listProcessParameter, final ProcessAPI processAPI) {
		logger.info("MeteorAPI---------- GetListProcess");
		final Map<String, Object> result = new HashMap<String, Object>();
		processDefinitionList.calculateListProcess(processAPI);

		result.put("listevents", BEventFactory.getHtml(processDefinitionList.getListEventCalculation()));
		result.put("processes", processDefinitionList.toJson(listProcessParameter));
		return result;
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
	 * the meteor motor run as a command (because at each access, the custom
	 * page is reloaded, so there are no way to save the file else in Bonita)
	 * Attention : the deployment are suppose to not be done at each time
	 *
	 * @param inputStreamJarFile
	 * @param commandAPI
	 * @param platFormAPI
	 * @return
	 */
	public List<BEvent> deployCommand(final boolean forceDeploy, final String version, final List<JarDependencyCommand> jarDependencies, final CommandAPI commandAPI, final PlatformAPI platFormAPI) {
		logger.info("MeteorAPI.deployCommandGroovyScenario ---------- Start deployCommand");
		return CmdMeteor.deployCommand(forceDeploy, version, jarDependencies, commandAPI, platFormAPI);
	}


	public List<BEvent> deployCommandGroovyScenario(final boolean forceDeploy, final String version, final List<JarDependencyCommand> jarDependencies, final CommandAPI commandAPI, final PlatformAPI platFormAPI) {
		try {
			logger.info("MeteorAPI.deployCommandGroovyScenario ---------- Start deployCommandGroovyScenario");
			return MeteorRobotGroovyScenario.deployCommandGroovyScenario(forceDeploy, version, jarDependencies, commandAPI, platFormAPI);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.info("MeteorAPI.deployCommandGroovyScenario Exception " + e.toString() + " at " + exceptionDetails);
			List<BEvent> listEvents = new ArrayList<BEvent>();
			listEvents.add(new BEvent( EventDeployCommandGroovyScenario, e, ""));

			return listEvents;
		} catch (Error er) {
			StringWriter sw = new StringWriter();
			er.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.info("MeteorAPI.deployCommandGroovyScenario Error " + er.toString() + " at " + exceptionDetails);
			List<BEvent> listEvents = new ArrayList<BEvent>();
			listEvents.add(new BEvent(EventDeployCommandGroovyScenario, "Error [" + er.toString() + "]"));

			return listEvents;
		}
	}

	/**
		 *
		 *
		 */
	public static class StartParameters {

		// collect all information, from the JSON. The interpretation will be
		// done in MeteorOperation.start()
		public List<Map<String, Object>> listOfProcesses;
		public List<Map<String, Object>> listOfScenarii;

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
		public void decodeFromJsonSt() {
			logger.info("MeteorAPI.decodeFromJsonSt : JsonSt[" + jsonListSt + "]");
			listOfProcesses = new ArrayList<Map<String, Object>>();
			listOfScenarii = new ArrayList<Map<String, Object>>();

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
				logger.info("MeteorAPI.decodeFromJsonSt : line object [" + jsonObject.getClass().getName() + "] Map ? " + (jsonObject instanceof HashMap) + " line=[" + jsonSt + "] ");

				if (jsonObject instanceof HashMap) {
					logger.info("MeteorAPI.decodeFromJsonSt : object [" + jsonObject.getClass().getName() + "] is a HASHMAP");

					final HashMap<String, Object> jsonHash = (HashMap<String, Object>) jsonObject;
					if (jsonHash.get("processes") != null) {
						listOfProcesses.addAll((ArrayList<Map<String, Object>>) jsonHash.get("processes"));
					}
					// old way
					if (jsonHash.get("process") != null) {
						listOfProcesses.add((Map<String, Object>) jsonHash.get("process"));
					}

					if (jsonHash.get("scenarii") != null) {
						listOfScenarii.addAll((ArrayList<Map<String, Object>>) jsonHash.get("scenarii"));
					}
					// old way
					if (jsonHash.get("scenario") != null) {
						listOfScenarii.add((Map<String, Object>) jsonHash.get("scenario"));
					}
				} else if (jsonObject instanceof List) {
					logger.info("MeteorAPI.decodeFromJsonSt : object [" + jsonObject.getClass().getName() + "] is a LIST");
					final List<Map<String, Map<String, Object>>> jsonList = (List<Map<String, Map<String, Object>>>) jsonObject;
					for (final Map<String, Map<String, Object>> oneRecord : jsonList) {
						logger.info("MeteorAPI.decodeFromJsonSt : process [" + oneRecord.get("process") + "] scenario [" + oneRecord.get("scenario") + "]");

						if (oneRecord.containsKey("process")) {
							listOfProcesses.add(oneRecord.get("process"));
						}
						if (oneRecord.containsKey("scenario")) {
							listOfScenarii.add(oneRecord.get("scenario"));
						}
					}
				}
			}
			logger.info("MeteorAPI.decodeFromJsonSt :  decodeFromJsonSt nbProcess=" + listOfProcesses.size() + " nbScenarii=" + listOfScenarii.size());
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

		public void decodeFromJsonSt() {
			logger.info("MeteorAPI JsonSt[" + jsonSt + "] simulationId[" + simulationId + "]");
			if (jsonSt == null) {
				// already set
				return;
			}
			final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);

			simulationId = MeteorToolbox.getParameterLong(jsonHash, "simulationid", -1);
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
	public Map<String, Object> start(final StartParameters startParameters, final ProcessAPI processAPI, final CommandAPI commandAPI) {

		logger.info("~~~~~~~~~~ MeteorAPI.start() parameter=" + startParameters.toString());
		final List<BEvent> listEvents = new ArrayList<BEvent>();

		Map<String, Object> resultCommandHashmap = new HashMap<String, Object>();

		final CommandDescriptor command = CmdMeteor.getCommandDescriptor(commandAPI);
		if (command == null) {
			logger.info("~~~~~~~~~~ MeteorAPI.start() No Command deployed, stop");
			listEvents.add(EventNotDeployed);
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));
			return resultCommandHashmap;
		}

		final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
		parameters.put(CmdMeteor.cstParamCommandNameStartParams, startParameters.jsonListSt);
		parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStart);

		try {

			// see the command in CmdMeteor
			logger.info("~~~~~~~~~~ MeteorAPI.start() Call Command [" + command.getId() + "]");
			final Serializable resultCommand = commandAPI.execute(command.getId(), parameters);

			resultCommandHashmap = (Map<String, Object>) resultCommand;
			/*
			 * final String listEventsCommand = (String)
			 * resultCommandHashmap.get(CmdMeteor.cstParamResultListEvents); if
			 * (listEventsCommand != null) {
			 * resultCommandHashmap.put("listeventsst", listEventsCommand); }
			 */

		} catch (final CommandNotFoundException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();

			logger.severe("~~~~~~~~~~ MeteorAPI.start() : ERROR " + e + " at " + exceptionDetails);
			listEvents.add(new BEvent(EventStartError, e, ""));
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

		} catch (final CommandExecutionException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();

			logger.severe("~~~~~~~~~~ MeteorAPI.start() : ERROR " + e + " at " + exceptionDetails);
			listEvents.add(new BEvent(EventStartError, e, ""));
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

		} catch (final CommandParameterizationException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();

			logger.severe("~~~~~~~~~~ MeteorAPI.start() : ERROR " + e + " at " + exceptionDetails);
			listEvents.add(new BEvent(EventStartError, e, ""));
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

		} catch (final Exception e) {

			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();

			logger.severe("~~~~~~~~~~ MeteorAPI.start() : ERROR " + e + " at " + exceptionDetails);
			listEvents.add(new BEvent(EventStartError, e, ""));
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

		}
		logger.info("~~~~~~~~~~ MeteorAPI.start() : END " + resultCommandHashmap);
		return resultCommandHashmap;
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
	public Map<String, Object> getStatus(final StatusParameters statusSimulation, final ProcessAPI processAPI, final CommandAPI commandAPI) {
		logger.info("MeteorAPI.getStatus()");
		Map<String, Object> resultCommandHashmap = new HashMap<String, Object>();

		final List<BEvent> listEvents = new ArrayList<BEvent>();
		final CommandDescriptor command = CmdMeteor.getCommandDescriptor(commandAPI);
		if (command == null) {
			listEvents.add(EventNotDeployed);
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));
			return resultCommandHashmap;
		}

		final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
		parameters.put(CmdMeteor.cstParamCommandNameStatusParams, statusSimulation.getJson());

		parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStatus);

		try {
			final Serializable resultCommand = commandAPI.execute(command.getId(), parameters);
			resultCommandHashmap = (HashMap<String, Object>) resultCommand;
			// final List<BEvent> listEventsCommand = (List<BEvent>)
			// resultCommandHashmap.get("listevents");
			// if (listEventsCommand != null) {
			// listEvents.addAll(listEventsCommand);
			// }

		} catch (final CommandNotFoundException e) {
			logger.severe("MeteorAPI.getStatus " + e);
			listEvents.add(new BEvent(EventStartError, e, ""));
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

		} catch (final CommandExecutionException e) {
			logger.severe("MeteorAPI.getStatus " + e);
			listEvents.add(new BEvent(EventStartError, e, ""));
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

		} catch (final CommandParameterizationException e) {
			logger.severe("MeteorAPI.getStatus " + e);
			listEvents.add(new BEvent(EventStartError, e, ""));
			resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));
		}

		return resultCommandHashmap;
	}

}
