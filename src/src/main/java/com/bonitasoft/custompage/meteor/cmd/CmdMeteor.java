package com.bonitasoft.custompage.meteor.cmd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.SCommandParameterizationException;
import org.bonitasoft.engine.command.TenantCommand;
import org.bonitasoft.engine.connector.ConnectorAPIAccessorImpl;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.platform.StartNodeException;
import org.bonitasoft.engine.platform.StopNodeException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

import com.bonitasoft.custompage.meteor.MeteorAccess.StartParameters;
import com.bonitasoft.custompage.meteor.MeteorAccess.StatusParameters;
import com.bonitasoft.custompage.meteor.MeteorOperation;

public class CmdMeteor extends TenantCommand {

    public static String cstParamPing = "ping";

    static Logger logger = Logger.getLogger(CmdMeteor.class.getName());

    private static BEvent EventAlreadyDeployed = new BEvent("org.bonitasoft.custompage.meteor.cmdmeteor", 1, Level.INFO,
            "Command already deployed", "The command at the same version is already deployed");
    private static BEvent EventDeployedWithSuccess = new BEvent("org.bonitasoft.custompage.meteor.cmdmeteor", 2, Level.INFO,
            "Command deployed with success", "The command are correctly deployed");

    public final static String cstParamCommandName = "CommandName";
    public final static String cstParamCommandNameStart = "START";
    public final static String cstParamCommandNameStartParams = "startparameters";
    public final static String cstParamCommandNameStatusParams = "statusparameters";

    public final static String cstParamCommandNamePing = "PING";
    public final static String cstParamCommandNameStatus = "STATUS";
    public final static String cstParamCommandNameAbort = "ABORT";

    public final static String cstParamTenantId = "tenantid";
    public final static String cstParamStartParameter = "startparameters";


    /**
     * chaque start execution receive a uniq Id. Then, STATUS and ABORT use this id.
     */
    public final static String cstParamCommandExecId = "ExecId";


    /**
     * Change the time of an timer. parameters are tenantid : optional, 1 per
     * default activityid : name of the activity ELSE the activityName +
     * processinstanceid shoud be provided activityname (if not activityid is
     * given) processinstanceid processinstanceid of the case to change
     * timername the name of the boundary timer newtimerdate the new date of
     * this process intance. Format is yyyyMMdd HH:MM:ss
     */

    @Override
    public Serializable execute(final Map<String, Serializable> parameters, final TenantServiceAccessor serviceAccessor)
            throws SCommandParameterizationException, SCommandExecutionException {
        logger.info("Execute Command");

        //  keep HashMap to return a Serializable
        HashMap<String, Object> result = new HashMap<String, Object>();
        logger.info("Start command CmdMeteor");

        Integer tenantId = (Integer) parameters.get(cstParamTenantId);
        if (tenantId == null) {
            tenantId = Integer.valueOf(1);
        }

        final ConnectorAPIAccessorImpl connectorAccessorAPI = new ConnectorAPIAccessorImpl(tenantId);

        final String commandName = (String) parameters.get(cstParamCommandName);
        if (cstParamCommandNamePing.equals(commandName)) {
            logger.info("COMMANDMETEOR.Ping ");

            final SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy HH:MM:SS");
            result.put("pingstatus", "Ping is ok at " + sdf.format(new Date()));

        }
        else if (cstParamCommandNameStart.equals(commandName)) {

            final StartParameters startParameters = StartParameters.getInstanceFromJsonList((ArrayList<String>) parameters.get(cstParamCommandNameStartParams));
            logger.info("COMMANDMETEOR.Start params[" + startParameters.toString() + "]");

            result = MeteorOperation.start(startParameters, connectorAccessorAPI, tenantId).getMap();
        }
        else if (cstParamCommandNameStatus.equals(commandName)) {
            logger.info("COMMANDMETEOR.Status ");
            final StatusParameters statusParameters = StatusParameters.getInstanceFromJsonSt((String) parameters.get(cstParamCommandNameStatusParams));

            result = MeteorOperation.status(statusParameters, connectorAccessorAPI).getMap();

        }
        else if (cstParamCommandNameAbort.equals(commandName)) {
            logger.info("COMMANDMETEOR.Abort ");

        }
        else {

            result.put("status", "Unknow command [" + commandName + "]");
        }
        return result;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Command management */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public static String commandName = "meteorcmd";
    public static String commandDescription = "Run the meteor robots, to creates cases / execute task";

    // public static String jarName = "CustomPageMeteor-1.0.0.jar";

    /**
     * get the command from its name
     *
     * @param commandAPI
     * @return
     */
    public static CommandDescriptor getCommandDescriptor(final CommandAPI commandAPI)
    {
        final List<CommandDescriptor> listCommands = commandAPI.getAllCommands(0, 1000, CommandCriterion.NAME_ASC);
        for (final CommandDescriptor command : listCommands) {
            if (commandName.equals(command.getName())) {
                return command;
            }
        }
        return null;
    }

    // use  InputStream inputStreamJarFile = pageResourceProvider.getResourceAsStream("lib/CustomPageMeteor-1.0.0.jar");

    /**
     * deploy the command
     *
     * @param forceDeploy
     * @param version
     * @param inputStreamJarFile
     * @param commandAPI
     * @param platFormAPI
     * @return
     */
    public static class JarDependencyCommand
    {
        public String jarName;
        public InputStream inputStreamJarFiles;

        public JarDependencyCommand(final String name, final InputStream inputStream)
        {
            jarName = name;
            inputStreamJarFiles = inputStream;
        }
    }

    public static JarDependencyCommand getInstanceJarDependencyCommand(final String name, final InputStream inputStream)
    {
        return new JarDependencyCommand(name, inputStream);
    }

    public static List<BEvent> deployCommand(final boolean forceDeploy, final String version, final List<JarDependencyCommand> jarDependency,
            final CommandAPI commandAPI, final PlatformAPI platFormAPI)
    {
        // String commandName, String commandDescription, String className, InputStream inputStreamJarFile, String jarName, ) throws IOException, AlreadyExistsException, CreationException, CommandNotFoundException, DeletionException {
        final List<BEvent> listEvents = new ArrayList<BEvent>();

        String message = "";

        try {
            // pause the engine to deploy a command
            if (platFormAPI != null) {
                platFormAPI.stopNode();
            }

            final List<CommandDescriptor> listCommands = commandAPI.getAllCommands(0, 1000, CommandCriterion.NAME_ASC);
            for (final CommandDescriptor command : listCommands) {
                if (commandName.equals(command.getName())) {
                    final String description = command.getDescription();
                    if (!forceDeploy && description.startsWith("V " + version))
                    {
                        listEvents.add(new BEvent(EventAlreadyDeployed, "V " + version));
                        return listEvents;
                    }

                    commandAPI.unregister(command.getId());
                }
            }

            /*
             * File commandFile = new File(jarFileServer); FileInputStream fis =
             * new FileInputStream(commandFile); byte[] fileContent = new
             * byte[(int) commandFile.length()]; fis.read(fileContent);
             * fis.close();
             */
            for (final JarDependencyCommand onejar : jarDependency)
            {
                final ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
                final byte[] buffer = new byte[10000];
                int nbRead = 0;
                while ((nbRead = onejar.inputStreamJarFiles.read(buffer)) > 0) {
                    fileContent.write(buffer, 0, nbRead);
                }

                try {
                    commandAPI.removeDependency(onejar.jarName);
                } catch (final Exception e) {
                };

                message += "Adding jarName [" + onejar.jarName + "] size[" + fileContent.size() + "]...";
                commandAPI.addDependency(onejar.jarName, fileContent.toByteArray());
                message += "Done.";
            }

            message += "Registering...";
            final CommandDescriptor commandDescriptor = commandAPI.register(commandName, "V " + version + " " + commandDescription, CmdMeteor.class.getName());

            if (platFormAPI != null) {
                platFormAPI.startNode();
            }

            listEvents.add(new BEvent(EventDeployedWithSuccess, message));
            return listEvents;

        } catch (final StopNodeException e1) {

            logger.severe("Can't stop  [" + e1.toString() + "]");
            message += e1.toString();
            return null;
        } catch (final StartNodeException e1) {

            logger.severe("Can't  start [" + e1.toString() + "]");
            message += e1.toString();
            return null;
        } catch (final CommandNotFoundException e) {
            logger.severe("Error during deploy command " + e);
        } catch (final DeletionException e) {
            logger.severe("Error during deploy command " + e);
        } catch (final IOException e) {
            logger.severe("Error during deploy command " + e);
        } catch (final AlreadyExistsException e) {
            logger.severe("Error during deploy command " + e);
        } catch (final CreationException e) {
            logger.severe("Error during deploy command " + e);
        }
        return listEvents;
    }
}
