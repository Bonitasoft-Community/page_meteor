package com.bonitasoft.custompage.meteor.scenario.groovy;

import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;

import com.bonitasoft.custompage.meteor.MeteorRobot;
import com.bonitasoft.custompage.meteor.MeteorSimulation;
import com.bonitasoft.custompage.meteor.scenario.Scenario;


public class MeteorRobotGroovyScenario extends MeteorRobot {

    Logger logger = Logger.getLogger(MeteorSimulation.class.getName());

    private String mContent;

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
        try
        {
            Thread.sleep(5000);
        } catch (final Exception e)
        {

        }

        setOperationIndex(100);
        setFinalStatus(FINALSTATUS.SUCCESS);

    }


}
