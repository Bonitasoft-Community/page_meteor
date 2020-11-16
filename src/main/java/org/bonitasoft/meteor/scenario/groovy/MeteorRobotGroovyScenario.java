package org.bonitasoft.meteor.scenario.groovy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.scenario.ScenarioCmd;

import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.resource.InMemoryResource;
import com.bonitasoft.scenario.accessor.resource.Resource;
import com.bonitasoft.scenario.runner.RunListener;
import com.bonitasoft.scenario.runner.ScenarioResult;
import com.bonitasoft.scenario.runner.SingleRunner;
import com.bonitasoft.scenario.runner.context.ScenarioMainResourcesHelper;
import com.bonitasoft.scenario.runner.context.SingleRunContext;

// visit
// https://bitbucket.org/ps_ip/scenarioframework/wiki/Home#markdown-header-run-a-scenario
public class MeteorRobotGroovyScenario extends MeteorRobot {

    static Logger logger = Logger.getLogger(MeteorRobotGroovyScenario.class.getName());

    public final static BEvent EventLoadGroovyScenarioCommand = new BEvent("org.bonitasoft.custompage.meteor.MeteorRobotGroovyScenario", 1, Level.ERROR, "Groovy Command can't be created", "The Groovy Scenario needs special command to be deployed. The deployment of the command failed",
            "The groovy scenario can't be executed", "Check the error");

    private ScenarioCmd scenario = null;

    static private String scenarioName = "meteorGSScenario";

    public MeteorRobotGroovyScenario(String robotName, MeteorSimulation meteorSimulation, ScenarioCmd scenario, final APIAccessor apiAccessor) {
        super(robotName, meteorSimulation, apiAccessor);
        this.scenario = scenario;
    }

    // public setRessource()
    @Override
    public void executeRobot() {
        logger.info("MeteorRobotGroovyScenario.executeScenario SID[" + meteorSimulation.getId() + "] ROBOT[" + getRobotId() + "] Execute scenario[" + scenario.mScenario + "]");
        setOperationTotal(100);

        // Create and launch the runner
        try {
            // The scenario Groovy Script resource (only one file)
            Map<String, byte[]> mainResources = ScenarioMainResourcesHelper.generateSingleScenarioMainResourcesFromScriptContent(scenario.mScenario);

            // All the JAR dependencies
            Map<String, byte[]> jarDependencies = new HashMap<>();
            // All the Groovy Script dependencies
            Map<String, byte[]> gsDependencies = new HashMap<>();

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
            if ( ! scenarioResult.getErrors().isEmpty()|| ! scenarioResult.getWarns().isEmpty()) {
                String msg = scenarioResult.generateVisualResult().toString();
                mLogExecution.addLog(msg);
                mStatus = ROBOTSTATUS.FAIL;
                addError("Errors");
            } else
                mStatus = ROBOTSTATUS.DONE;
        } catch (Throwable e) {
            logger.info(" ROBOT " + getSignature() + " Scenario execution error[" + e.getCause() + " - " + e.getMessage() + "]");
            mStatus = ROBOTSTATUS.FAIL;

        } finally {
            setOperationIndex(100);
        }
    }

    /*
     * public static List<BEvent> deployCommandGroovyScenario(final boolean forceDeploy, final String version, final List<JarDependencyCommand> jarDependencies,
     * final CommandAPI commandAPI, final PlatformAPI platFormAPI) {
     * logger.info("MeteorRobotGroovyScenario.deployCommandGroovyScenario ---------- Start deployCommandGroovyScenario");
     * List<BEvent> listEvents = new ArrayList<BEvent>();
     * // remove fail ? No worry
     * removeDependency("process-starter-command-1.0.jar", commandAPI);
     * removeDependency("bdm-jpql-query-executor-command-1.0.jar", commandAPI);
     * removeDependency("scenario-utils-2.0.jar", commandAPI);
     * try {
     * CommandsAdministration.registerCommands(commandAPI, false);
     * } catch (SSessionException e) {
     * logger.severe("MeteorRobotGroovyScenario.Can't deploy correctly the GroovyScenario command : " + e.toString());
     * listEvents.add(new BEvent(EventLoadGroovyScenarioCommand, e, ""));
     * } catch (CreationException e) {
     * logger.severe("MeteorRobotGroovyScenario.Can't deploy correctly the GroovyScenario command : " + e.toString());
     * listEvents.add(new BEvent(EventLoadGroovyScenarioCommand, e, ""));
     * } catch (DeletionException e) {
     * logger.severe("MeteorRobotGroovyScenario.Can't deploy correctly the GroovyScenario command : " + e.toString());
     * listEvents.add(new BEvent(EventLoadGroovyScenarioCommand, e, ""));
     * }
     * return listEvents;
     * }
     */
    /*
    private static void removeDependency(String depencencyName, CommandAPI commandAPI) {
        try {
            commandAPI.removeDependency(depencencyName);
        } catch (Exception e) {
            logger.info("MeteorRobotGroovyScenario.Can't remove dependancy  command : " + e.toString());

        } ;

    }
    */

}
