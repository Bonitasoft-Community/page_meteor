package com.bonitasoft.custompage.meteor.scenario.groovy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;

import com.bonitasoft.custompage.meteor.MeteorRobot;
import com.bonitasoft.custompage.meteor.MeteorSimulation;
import com.bonitasoft.custompage.meteor.scenario.Scenario;
import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.resources.InMemoryResource;
import com.bonitasoft.scenario.runner.RunListener;
import com.bonitasoft.scenario.runner.SingleRunner;
import com.bonitasoft.scenario.runner.context.ScenarioMainResourcesHelper;
import com.bonitasoft.scenario.runner.context.SingleRunContext;


public class MeteorRobotGroovyScenario extends MeteorRobot {

    Logger logger = Logger.getLogger(MeteorSimulation.class.getName());

    private String mContent;
    
    static private ScenarioConfiguration scenarioConfiguration = new ScenarioConfiguration();
    static private String scenarioName = "meteorGSScenario";

    public MeteorRobotGroovyScenario(final APIAccessor apiAccessor)
    {
        super(apiAccessor);
    }

    public void setScenario(final Scenario scenario)
    {
        logger.info(" ROBOT " + mRobotId + " Receive scenario[" + scenario.mScenario + "]");
        mContent = scenario.mScenario;
    }

    // public setRessource()
    @Override
    public void executeRobot() {
        logger.info(" ROBOT " + mRobotId + " Execute scenario[" + mContent + "]");
        setNumberTotalOperation(100);
        
        // Create and launch the runner
        // tenantId: TODO, 1L by default for now
        try {
			SingleRunContext singleRunContext = new SingleRunContext(1L, scenarioConfiguration, new HashMap<String, Serializable>(), ScenarioMainResourcesHelper.generateSingleScenarioMainResourcesFromScriptContent(mContent), new InMemoryResource(), scenarioName);
			List<RunListener> runListeners = new ArrayList<RunListener>();
			runListeners.add(new AdvancementListener(singleRunContext, this));
			SingleRunner runner = new SingleRunner(singleRunContext, runListeners);
			runner.run();
        } catch(Throwable e) {
            logger.info(" ROBOT " + mRobotId + " Scenario execution error[" + e.getCause() + " - " + e.getMessage() + "]");
            setFinalStatus(FINALSTATUS.FAIL);
        } finally {
            setOperationIndex(100);
        }
        
        setFinalStatus(FINALSTATUS.SUCCESS);
    }


}
