package org.bonitasoft.meteor.cmd;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.command.BonitaCommand;
import org.bonitasoft.command.BonitaCommandApiAccessor;
import org.bonitasoft.command.BonitaCommand.ExecuteAnswer;
import org.bonitasoft.command.BonitaCommand.ExecuteParameters;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.connector.ConnectorAPIAccessorImpl;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import org.bonitasoft.meteor.MeteorDAO;
import org.bonitasoft.meteor.MeteorOperation;


public class CmdMeteor extends BonitaCommandApiAccessor {

	public static String cstParamPing = "ping";

	static Logger logger = Logger.getLogger(CmdMeteor.class.getName());
	private static String logHeader="CommandMeteor ~~~~~~ ";
	
	 public enum VERBE {
	        START, PING, STATUS, ABORT
	    };
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
	 * chaque start execution receive a uniq Id. Then, STATUS and ABORT use this
	 * id.
	 */
	public final static String cstParamCommandExecId = "ExecId";

	
	public final static String cstParamResultListEventsSt = "listevents";
	public final static String cstParamResultSimulationId = "simulationid";

	
	/**
	 * Change the time of an timer. parameters are tenantid : optional, 1 per
	 * default activityid : name of the activity ELSE the activityName +
	 * processinstanceid shoud be provided activityname (if not activityid is
	 * given) processinstanceid processinstanceid of the case to change
	 * timername the name of the boundary timer newtimerdate the new date of
	 * this process intance. Format is yyyyMMdd HH:MM:ss
	 */
	 @Override
	    public ExecuteAnswer executeCommandApiAccessor(ExecuteParameters executeParameters, APIAccessor apiAccessor) {

	     logger.info("Execute Command");
	     ExecuteAnswer executeAnswer = new ExecuteAnswer();
	        
		// keep HashMap to return a Serializable

		logger.info("Start command CmdMeteor");

		long tenantId = executeParameters.tenantId;

		final ConnectorAPIAccessorImpl connectorAccessorAPI = new ConnectorAPIAccessorImpl(tenantId);
		//VERBE verbEnum = null;
		// verbEnum = VERBE.valueOf(executeParameters.verb);
	            
		final String commandName = executeParameters.verb;
		if (cstParamCommandNamePing.equals(commandName)) {
			logger.info("COMMANDMETEOR.Ping ");

			final SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy HH:MM:SS");
			 executeAnswer.result.put("pingstatus", "Ping is ok at " + sdf.format(new Date()));

		} else if (cstParamCommandNameStart.equals(commandName)) {

			final StartParameters startParameters = StartParameters.getInstanceFromJsonList((ArrayList<String>) executeParameters.parametersCommand.get(cstParamCommandNameStartParams));
			logger.info("COMMANDMETEOR.Start params[" + startParameters.toString() + "]");

			 executeAnswer.result = MeteorOperation.start(startParameters, connectorAccessorAPI, tenantId).getMap();
		} else if (cstParamCommandNameStatus.equals(commandName)) {
			logger.info("COMMANDMETEOR.Status ");
			final StatusParameters statusParameters = StatusParameters.getInstanceFromJsonSt((String) executeParameters.parametersCommand.get(cstParamCommandNameStatusParams));

			 executeAnswer.result = MeteorOperation.getStatus(statusParameters, connectorAccessorAPI).getMap();

		} else if (cstParamCommandNameAbort.equals(commandName)) {
			logger.info("COMMANDMETEOR.Abort ");

		} else {

		    executeAnswer.result.put("status", "Unknow command [" + commandName + "]");
		}
		return  executeAnswer;
	}

	 
	    public ExecuteAnswer afterDeployment(ExecuteParameters executeParameters, TenantServiceAccessor serviceAccessor) {
	        ExecuteAnswer executeAnswer = new ExecuteAnswer();
	        executeAnswer.listEvents = MeteorDAO.getInstance().checkAndUpdateEnvironment(executeParameters.tenantId);
	        executeAnswer.result.put("status", BEventFactory.isError(executeAnswer.listEvents) ? "FAIL" : "OK");
	        return executeAnswer;
	    }

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Command management */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */
	public static String cstCommandName = "meteorcmd";
	public static String cstCommandDescription = "Run the meteor robots, to creates cases / execute task";

	// public static String jarName = "CustomPageMeteor-1.0.0.jar";

	/**
	 * get the command from its name
	 *
	 * @param commandAPI
	 * @return
	 *
	public static CommandDescriptor getCommandDescriptor(final CommandAPI commandAPI) {
		final List<CommandDescriptor> listCommands = commandAPI.getAllCommands(0, 1000, CommandCriterion.NAME_ASC);
		for (final CommandDescriptor command : listCommands) {
			if (commandName.equals(command.getName())) {
				return command;
			}
		}
		return null;
	}

	// use InputStream inputStreamJarFile =
	// pageResourceProvider.getResourceAsStream("lib/CustomPageMeteor-1.0.0.jar");

	/**
	 * deploy the command
	 *
	 * @param forceDeploy
	 * @param version
	 * @param inputStreamJarFile
	 * @param commandAPI
	 * @param platFormAPI
	 * @return
	 *
	public static class JarDependencyCommand {
		public String jarName;
		public File pageDirectory;

		public JarDependencyCommand(final String name, File pageDirectory) {
			this.jarName = name;
			this.pageDirectory = pageDirectory;
		}

		public String getCompleteFileName() {
			return pageDirectory.getAbsolutePath() + "/lib/" + jarName;
		}
	}

	public static JarDependencyCommand getInstanceJarDependencyCommand(final String name, File pageDirectory) {
		return new JarDependencyCommand(name, pageDirectory);
	}
*/
	
	/*
	public static List<BEvent> deployCommand(final boolean forceDeploy, final String version, File pageDirectory, final CommandAPI commandAPI, final PlatformAPI platFormAPI) {
		// String commandName, String commandDescription, String className,
		// InputStream inputStreamJarFile, String jarName, ) throws IOException,
		// AlreadyExistsException, CreationException, CommandNotFoundException,
		// DeletionException {
		final List<BEvent> listEvents = new ArrayList<BEvent>();

		List<JarDependencyCommand> jarDependencies = new ArrayList<JarDependencyCommand>();
		
		// execute the meteor command
		jarDependencies.add(CmdMeteor.getInstanceJarDependencyCommand("CustomPageMeteor-1.0.0.jar", pageDirectory));
		jarDependencies.add(CmdMeteor.getInstanceJarDependencyCommand("bonita-event-1.1.0.jar", pageDirectory));

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
					if (!forceDeploy && description.startsWith("V " + version)) {
						logger.info("Meteor.cmdMeteor >>>>>>>>>>>>>>>>>>>>>>>>> No deployment Command [" + commandName + "] Version[V " + version + "]");

						listEvents.add(new BEvent(EventAlreadyDeployed, "V " + version));
						return listEvents;
					}

					commandAPI.unregister(command.getId());
				}
			}
			logger.info("Meteor.cmdMeteor >>>>>>>>>>>>>>>>>>>>>>>>> DEPLOIEMENT Command [" + commandName + "] Version[V " + version + "]");
		
			for (final JarDependencyCommand onejar : jarDependencies) {
				final ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
				final byte[] buffer = new byte[10000];
				int nbRead = 0;
				InputStream fileJar = null;
				try {
					fileJar = new FileInputStream(onejar.getCompleteFileName());

					while ((nbRead = fileJar.read(buffer)) > 0) {
						fileContent.write(buffer, 0, nbRead);
					}

					commandAPI.removeDependency(onejar.jarName);
				} catch (final Exception e) {
					logger.info( logHeader+" Remove dependency["+e.toString()+"]");
					message+="Exception remove["+onejar.jarName+"]:"+e.toString();
				} finally {
					if (fileJar != null)
						fileJar.close();
				}				
//				message += "Adding jarName [" + onejar.jarName + "] size[" + fileContent.size() + "]...";
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

		} catch (final StopNodeException e) {
			logger.severe("Can't stop  [" + e.toString() + "]");
			message += e.toString();
			listEvents.add(new BEvent(EventErrorAtDeployment, e, "Command["+commandName + "V " + version + " " + commandDescription+"]"));
			return null;
		} catch (final StartNodeException e) {
			logger.severe("Can't  start [" + e.toString() + "]");
			message += e.toString();
			listEvents.add(new BEvent(EventErrorAtDeployment, e, "Command["+commandName + "V " + version + " " + commandDescription+"]"));
			return null;
		} catch (final CommandNotFoundException e) {
			logger.severe("Error during deploy command " + e);
		} catch (final DeletionException e) {
			logger.severe("Error during deploy command " + e);
			listEvents.add(new BEvent(EventErrorAtDeployment, e, "Command["+commandName + "V " + version + " " + commandDescription+"]"));
		} catch (final IOException e) {
			logger.severe("Error during deploy command " + e);
			listEvents.add(new BEvent(EventErrorAtDeployment, e, "Command["+commandName + "V " + version + " " + commandDescription+"]"));
		} catch (final AlreadyExistsException e) {
			logger.severe("Error during deploy command " + e);
			listEvents.add(new BEvent(EventErrorAtDeployment, e, "Command["+commandName + "V " + version + " " + commandDescription+"]"));
		} catch (final CreationException e) {
			logger.severe("Error during deploy command " + e);
			listEvents.add(new BEvent(EventErrorAtDeployment, e, "Command["+commandName + "V " + version + " " + commandDescription+"]"));
		}
		return listEvents;
	}
	*/

   
}
