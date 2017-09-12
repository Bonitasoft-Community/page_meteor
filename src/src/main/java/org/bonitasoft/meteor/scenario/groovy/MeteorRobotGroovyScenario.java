package org.bonitasoft.meteor.scenario.groovy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.command.DependencyNotFoundException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.session.SSessionException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.cmd.CmdMeteor.JarDependencyCommand;
import org.bonitasoft.meteor.scenario.Scenario;


import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.resource.InMemoryResource;
import com.bonitasoft.scenario.accessor.resource.Resource;
import com.bonitasoft.scenario.administration.CommandsAdministration;
import com.bonitasoft.scenario.accessor.resource.Resource;
import com.bonitasoft.scenario.runner.RunListener;
import com.bonitasoft.scenario.runner.ScenarioResult;
import com.bonitasoft.scenario.runner.SingleRunner;
import com.bonitasoft.scenario.runner.context.ScenarioMainResourcesHelper;
import com.bonitasoft.scenario.runner.context.SingleRunContext;

// visit
// https://bitbucket.org/ps_ip/scenarioframework/wiki/Home#markdown-header-run-a-scenario
public class MeteorRobotGroovyScenario extends MeteorRobot {

	static Logger logger = Logger.getLogger(MeteorSimulation.class.getName());

	public static BEvent EventLoadGroovyScenarioCommand = new BEvent("org.bonitasoft.custompage.meteor.MeteorRobotGroovyScenario", 1, Level.ERROR, "Groovy Command can't be created", "The Groovy Scenario needs special command to be deployed. The deployment of the command failed", "The groovy scenario can't be executed", "Check the error");

	private Scenario scenario = null;

	static private String scenarioName = "meteorGSScenario";

	public MeteorRobotGroovyScenario(MeteorSimulation meteorSimulation,final APIAccessor apiAccessor) {
		super( meteorSimulation,apiAccessor);
	}

	public void setScenario(final Scenario scenario) {
		logger.info("MeteorRobotGroovyScenario.setScenario SID["+meteorSimulation.getId()+"] ROBOT[" + mRobotId + "] Receive scenario[" + scenario.mScenario + "]");
		this.scenario = scenario;
	}

	// public setRessource()
	@Override
	public void executeRobot() {
		logger.info("MeteorRobotGroovyScenario.executeScenario SID["+meteorSimulation.getId()+"] ROBOT[" + mRobotId + "] Execute scenario[" + scenario.mScenario + "]");
		setOperationTotal(100);

		// Create and launch the runner
		try {
			// The scenario Groovy Script resource (only one file)
			Map<String, byte[]> mainResources = ScenarioMainResourcesHelper.generateSingleScenarioMainResourcesFromScriptContent(scenario.mScenario);

			// All the JAR dependencies
			Map<String, byte[]> jarDependencies = new HashMap<String, byte[]>();
			// All the Groovy Script dependencies
			Map<String, byte[]> gsDependencies = new HashMap<String, byte[]>();

			ScenarioConfiguration scenarioConfiguration = new ScenarioConfiguration();
			Resource resource = new InMemoryResource(mainResources);

			/*
			 * SingleRunContext singleRunContext = new SingleRunContext(1L *
			 * tenant id *, scenarioConfiguration, parameters, mainResources,
			 * jarDependencies, gsDependencies, resource, scenarioName);
			 */

			SingleRunContext singleRunContext = new SingleRunContext(scenario.getTenantId(), scenarioConfiguration, new HashMap<String, Serializable>(), mainResources, jarDependencies, gsDependencies, resource, scenarioName);
			List<RunListener> runListeners = new ArrayList<RunListener>();
			runListeners.add(new AdvancementListener(singleRunContext, this));
			SingleRunner runner = new SingleRunner(singleRunContext, runListeners);
			runner.run();
			ScenarioResult scenarioResult = runner.getScenarioResult();
			if (scenarioResult.getErrors().size()>0 || scenarioResult.getWarns().size()>0)
			{
				String msg= scenarioResult.generateVisualResult().toString();
				mLogExecution.addLog(msg);
				setFinalStatus(FINALSTATUS.FAIL);
			}
			else
				setFinalStatus(FINALSTATUS.SUCCESS);
		} catch (Throwable e) {
			logger.info(" ROBOT " + mRobotId + " Scenario execution error[" + e.getCause() + " - " + e.getMessage() + "]");
			setFinalStatus(FINALSTATUS.FAIL);
		} finally {
			setOperationIndex(100);
		}
	}

	
	public static List<BEvent> deployCommandGroovyScenario(final boolean forceDeploy, final String version, final List<JarDependencyCommand> jarDependencies, final CommandAPI commandAPI, final PlatformAPI platFormAPI) {
		logger.info("MeteorRobotGroovyScenario.deployCommandGroovyScenario ---------- Start deployCommandGroovyScenario" );
		
		List<BEvent> listEvents = new ArrayList<BEvent>();
		// remove fail ? No worry
		removeDependency("process-starter-command-1.0.jar", commandAPI);
		removeDependency("bdm-jpql-query-executor-command-1.0.jar", commandAPI);
		removeDependency("scenario-utils-2.0.jar", commandAPI);
		try
		{
			CommandsAdministration.registerCommands(commandAPI, false);
		} catch (SSessionException e) {
			logger.severe("MeteorRobotGroovyScenario.Can't deploy correctly the GroovyScenario command : "+e.toString());
			listEvents.add( new BEvent( EventLoadGroovyScenarioCommand, e, ""));
		} catch (CreationException e) {
			logger.severe("MeteorRobotGroovyScenario.Can't deploy correctly the GroovyScenario command : "+e.toString());
			listEvents.add( new BEvent( EventLoadGroovyScenarioCommand, e, ""));
		} catch (DeletionException e) {
			logger.severe("MeteorRobotGroovyScenario.Can't deploy correctly the GroovyScenario command : "+e.toString());
			listEvents.add( new BEvent( EventLoadGroovyScenarioCommand, e, ""));
		}
		return listEvents;
		

	}
	
	private static void removeDependency(String depencencyName, CommandAPI commandAPI)
	{
		try {
			commandAPI.removeDependency(depencencyName);
		}
		catch(Exception e)
		{
			logger.info("MeteorRobotGroovyScenario.Can't remove dependancy  command : "+e.toString());

		};

	}
		
		
}
