package com.bonitasoft.custompage.meteor;

import java.io.Serializable;
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
import org.bonitasoft.log.event.BEventFactory;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.meteor.MeteorProcessDefinitionList.ListProcessParameter;
import com.bonitasoft.custompage.meteor.cmd.CmdMeteor;
import com.bonitasoft.custompage.meteor.cmd.CmdMeteor.JarDependencyCommand;

public class MeteorAccess {

    /**
     * ******************************************************************************** *
     * This class is the main/only access for the Groovy
     * - getListProcess=> ask ProcessDefinitionList
     * The execution have to work in background. In order to protect the code (because Bonita reload the JAR at every call in debug mode)
     * then the execution is done via a Command.
     * At start, we check if we need to redeploy the command (normaly, first execution or only if the version change)
     * .
     * start ==> CmdMeteor => MeteorOperation => MeteorSimulation
     * CmdMeteor : this is the command. Only command operation (deploy, send order and then execute)
     * MeteorOperation (CommandSite) : multiple run can be executed (so multiple MeteorSimulation).
     * MeteorOperation create a new MeteorSimulation at Start and get a ID. meteorOperation save all <ID>-<MeteorOperation>
     * an ID is returned.
     * .
     * status => CmdMeteor => MeteorOperation => MeteorSimulation
     * MeteorOperation get the correct MeteorSimulation from the Id
     * ********************************************************************************
     */

    private static Logger logger = Logger.getLogger(MeteorAccess.class.getName());

    private static BEvent EventNotDeployed = new BEvent(MeteorAccess.class.getName(), 1, Level.ERROR,
            "Command not deployed", "The command is not deployed");
    private static BEvent EventStartError = new BEvent(MeteorAccess.class.getName(), 2, Level.ERROR,
            "Error during starting the simulation", "Check the error", "No test are started", "See the error");

    MeteorProcessDefinitionList processDefinitionList = new MeteorProcessDefinitionList();

    // result of information
    public final static String cstParamResultListEventsSt = "listevents";
    public final static String cstParamResultSimulationId = "simulationid";
    public static String cstParamResultStatus = "simulationstatus";

    MeteorSimulation meteorSimulation = new MeteorSimulation();


    public MeteorAccess() {
    }

    /**
     * get the current object
     *
     * @return
     */
    public static MeteorAccess getMeteorAccess(final HttpSession httpSession)
    {
        logger.info("----------- getMeteorAccess from Session");
        // in debug mode, BonitaEngine reload the JAR file every access : so any object saved in the httpSession will get a ClassCastException

        return new MeteorAccess();
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Get Information to start a new test */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * get the list of all processes visible on the engine
     *
     * @param listProcessParameter
     * @param processAPI
     * @return
     */
    public Map<String, Object> getListProcesses(final ListProcessParameter listProcessParameter, final ProcessAPI processAPI) {
        logger.info("---------- GetListProcess");
        final Map<String, Object> result = new HashMap<String, Object>();
        processDefinitionList.calculateListProcess(processAPI);

        result.put("listevents", BEventFactory.getHtml(processDefinitionList.getListEventCalculation()));
        result.put("processes", processDefinitionList.toJson(listProcessParameter));
        return result;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Start a new test game */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * the meteor motor run as a command (because at each access, the custom page is reloaded, so there are no way to save the file else in Bonita)
     * Attention : the deployment are suppose to not be done at each time
     *
     * @param inputStreamJarFile
     * @param commandAPI
     * @param platFormAPI
     * @return
     */
    public List<BEvent> deployCommand(final boolean forceDeploy, final String version, final List<JarDependencyCommand> jarDependencies,
            final CommandAPI commandAPI,
            final PlatformAPI platFormAPI)
    {
        return CmdMeteor.deployCommand(forceDeploy, version, jarDependencies, commandAPI, platFormAPI);
    }

    /**
		 *
		 *
		 */
    public static class StartParameters {

        public List<Map<String, String>> listOfProcesses;
        /**
         * keep the parameters as a JSON to sent it to the command
         */
        public String jsonSt;

        public static StartParameters getInstanceFromJsonSt(final String jsonSt) {
            final StartParameters startParameters = new StartParameters();
            startParameters.jsonSt = jsonSt;
            return startParameters;
        }

        /**
         *
         */
        public void decodeFromJsonSt() {
            logger.info("JsonSt[" + jsonSt + "]");
            if (jsonSt == null)
            {
                listOfProcesses = new ArrayList<Map<String, String>>();
                return;
            }
            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);

            listOfProcesses = (ArrayList<Map<String, String>>) jsonHash.get("processes");
        }


        @Override
        public String toString()
        {
            return "startParameters " + (jsonSt == null ? "null" : (jsonSt + "                          ").substring(0, 20)) + "...";
        }
    }

    public static class StatusParameters {

        long simulationId;
        public String jsonSt;

        public static StatusParameters getInstanceFromJsonSt(final String jsonSt) {
            final StatusParameters statusParameters = new StatusParameters();
            statusParameters.jsonSt = jsonSt;
            return statusParameters;
        }

        public void decodeFromJsonSt() {
            logger.info("JsonSt[" + jsonSt + "]");
            if (jsonSt == null)
            {
                simulationId = -1;
                return;
            }
            final HashMap<String, String> jsonHash = (HashMap<String, String>) JSONValue.parse(jsonSt);

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

        logger.info("MeteorAccess.start() parameteres="+startParameters.toString());
        final List<BEvent> listEvents = new ArrayList<BEvent>();

        Map<String, Object> resultCommandHashmap = new HashMap<String, Object>();

        final CommandDescriptor command = CmdMeteor.getCommandDescriptor(commandAPI);
        if (command == null)
        {
            listEvents.add(EventNotDeployed);
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));
            return resultCommandHashmap;
        }

        final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put( CmdMeteor.cstParamCommandNameStartParams, startParameters.jsonSt);
        parameters.put( CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStart);

        try {
            final Serializable resultCommand = commandAPI.execute(command.getId(), parameters);

            resultCommandHashmap = (Map<String, Object>) resultCommand;
            /*
             * final String listEventsCommand = (String) resultCommandHashmap.get(CmdMeteor.cstParamResultListEvents);
             * if (listEventsCommand != null) {
             * resultCommandHashmap.put("listeventsst", listEventsCommand);
             * }
             */

        } catch (final CommandNotFoundException e) {
            logger.severe("MeteorAccess.start " + e);
            listEvents.add(new BEvent(EventStartError, e, ""));
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

        } catch (final CommandExecutionException e) {
            logger.severe("MeteorAccess.start " + e);
            listEvents.add(new BEvent(EventStartError, e, ""));
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

        } catch (final CommandParameterizationException e) {
            logger.severe("MeteorAccess.start " + e);
            listEvents.add(new BEvent(EventStartError, e, ""));
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

        }

        logger.info("MeteorAccess.start() end " + resultCommandHashmap);
        return resultCommandHashmap;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Start a new test game */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * getStatus
     */
    public Map<String, Object> getStatus(final StatusParameters statusSimulation, final ProcessAPI processAPI, final CommandAPI commandAPI) {
        logger.info("MeteorAccess.getStatus()");
        Map<String, Object> resultCommandHashmap = new HashMap<String, Object>();

        final List<BEvent> listEvents = new ArrayList<BEvent>();
        final CommandDescriptor command = CmdMeteor.getCommandDescriptor(commandAPI);
        if (command == null)
        {
            listEvents.add(EventNotDeployed);
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));
            return resultCommandHashmap;
        }

        final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(CmdMeteor.cstParamCommandNameStatusParams, statusSimulation.jsonSt);

        parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStatus);

        try {
            final Serializable resultCommand = commandAPI.execute(command.getId(), parameters);
            resultCommandHashmap = (HashMap<String, Object>) resultCommand;
            //final List<BEvent> listEventsCommand = (List<BEvent>) resultCommandHashmap.get("listevents");
            //if (listEventsCommand != null) {
            //    listEvents.addAll(listEventsCommand);
            //}

        } catch (final CommandNotFoundException e) {
            logger.severe("MeteorAccess.start " + e);
            listEvents.add(new BEvent(EventStartError, e, ""));
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

        } catch (final CommandExecutionException e) {
            logger.severe("MeteorAccess.start " + e);
            listEvents.add(new BEvent(EventStartError, e, ""));
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));

        } catch (final CommandParameterizationException e) {
            logger.severe("MeteorAccess.start " + e);
            listEvents.add(new BEvent(EventStartError, e, ""));
            resultCommandHashmap.put("listevents", BEventFactory.getHtml(listEvents));
        }


        return resultCommandHashmap;
    }




}
