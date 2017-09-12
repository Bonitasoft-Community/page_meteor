package com.bonitasoft.scenario.administration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.command.DependencyNotFoundException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.session.SSessionException;

import com.bonitasoft.bdm.jpql.query.executor.command.BDMJpqlQueryExecutorCommand;
import com.bonitasoft.process.starter.command.ProcessStarterCommand;
import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;

public class CommandsAdministration {
	static private List<CommandRecord> commands = new ArrayList<CommandRecord>();
	
	static {
		commands.add(new CommandRecord(ProcessStarterCommand.NAME, ProcessStarterCommand.SUMMARY, getCommandResources("process-starter-command-1.0.jar"), ProcessStarterCommand.class.getName()));
		commands.add(new CommandRecord(BDMJpqlQueryExecutorCommand.NAME, BDMJpqlQueryExecutorCommand.SUMMARY, getCommandResources("bdm-jpql-query-executor-command-1.0.jar"), BDMJpqlQueryExecutorCommand.class.getName()));
	}
	
	static private Map<String, byte[]> getCommandResources(String...resources) {
		Map<String, byte[]> commandResources = new HashMap<String, byte[]>();
		for(String resource : resources) {
			commandResources.put(resource, getCommandResource(resource));
		}
		
		return commandResources;
	}
	
	static private byte[] getCommandResource(String resource) {
		InputStream input = null;
		try {
			input = CommandsAdministration.class.getResource(resource).openStream();
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
	        IOUtils.copy(input, output);
	        return output.toByteArray();
		} catch (Exception e) {
			ScenarioConfiguration.logger.log(Level.SEVERE, "The command resource " + resource + " could not be loaded", e);
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch(Exception e1) {}
			}
		}
		
		return null;
	}
	
	static public void registerCommands(CommandAPI commandAPI, boolean flush) throws SSessionException, CreationException, DeletionException {
		if(flush) {
			CommandsAdministration.unregisterCommands(commandAPI);
		}
		
		for(CommandRecord command : commands) {
			try {
				commandAPI.getCommand(command.name);
				ScenarioConfiguration.logger.log(Level.WARNING, "The command " + command.name + " is already registered, nothing done");
			} catch(CommandNotFoundException commandNotFoundException) {
				Set<String> keys = command.resources.keySet();
				for(String key : keys) {
					try {
						commandAPI.addDependency(key, command.resources.get(key));
						ScenarioConfiguration.logger.log(Level.INFO, "The dependency " + key + " has been added");
					} catch(AlreadyExistsException alreadyExistsException) {
						ScenarioConfiguration.logger.log(Level.WARNING, "The dependency " + key + " already exists, nothing done");
					}
				}
				
				commandAPI.register(command.name, command.summary, command.className);
				ScenarioConfiguration.logger.log(Level.INFO, "The command " + command.name + " has been registered");
			}
		}
	}
	
	static public void unregisterCommands(CommandAPI commandAPI) throws SSessionException, DeletionException {
		for(CommandRecord command : commands) {
			try {
				commandAPI.getCommand(command.name);
				Set<String> keys = command.resources.keySet();
				for(String key : keys) {
					try {
						commandAPI.removeDependency(key);
						ScenarioConfiguration.logger.log(Level.INFO, "The dependency " + key + " has been removed");
					} catch(DependencyNotFoundException dependencyNotFoundException) {
						ScenarioConfiguration.logger.log(Level.WARNING, "The dependency " + key + " does not exist, nothing done");
					}
				}
				
				commandAPI.unregister(command.name);
				ScenarioConfiguration.logger.log(Level.INFO, "The command " + command.name + " has been unregistered");
			} catch(CommandNotFoundException commandNotFoundException) {
				ScenarioConfiguration.logger.log(Level.WARNING, "The command " + command.name + " is not registered, nothing done");
			}
		}
	}
}

class CommandRecord {
	String name = null;
	String summary = "";
	Map<String, byte[]> resources = new HashMap<String, byte[]>();
	String className = null;
	
	public CommandRecord(String name, String summary, Map<String, byte[]> resources, String className) {
		super();
		this.name = name;
		this.summary = summary;
		this.resources = resources;
		this.className = className;
	}
}
