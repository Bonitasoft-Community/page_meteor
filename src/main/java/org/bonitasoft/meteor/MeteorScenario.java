package org.bonitasoft.meteor;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.scenario.process.MeteorDefProcess;

/* ************************************************************************ */
/*                                                                          */
/* Scenario */
/*
 * The scenario DESCRIBE the job to do, and not execute the job.
 * To execute the job, see the ROBOT classes.
 * Scenario register in the MeteorSimulation all jobs (i.e. all robots)
 */
/*                                                                          */
/* ************************************************************************ */

public abstract class MeteorScenario {

    public String mScenarioName;
    public MeteorScenario(String name ) {
        this.mScenarioName = name;
    }
    public abstract List<BEvent> registerInSimulation(StartParameters startParameters, MeteorSimulation meteorSimulation, APIAccessor apiAccessor);

    /**
     * Simulation need to register all robots created by the scenario. This method instanciated all robots, ready to start
     * Nb: if the user request multiple robots, this is the responsability of this method to generate ALL robots (user ask 10 robots, this method should return the 10 (or 20 ?) robots
     * 
     * @return
     */
    public abstract List<MeteorRobot> generateRobots(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor);

    /**
     * in order to calculate the cover, the list of process is mandatory
     * 
     * @return
     */
    public static class CollectResult {
        public List<MeteorDefProcess> listDefProcess;
        public List<BEvent> listEvents = new ArrayList<>();
    }
    public abstract CollectResult collectProcess(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor);

}
