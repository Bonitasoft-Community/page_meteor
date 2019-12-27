package org.bonitasoft.meteor.scenario.experience;

import java.util.List;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.scenario.experience.MeteorTimeLine.TimeLineStep;

public class MeteorRobotExperience extends MeteorRobot {

    public MeteorRobotExperience(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        super(meteorSimulation, apiAccessor);

    }
    @Override
    public void executeRobot() {

        // calculate the timeLine of the case
        MeteorTimeLine meteorTimeLine = createTimeLine();
        
        List<BEvent> listEvents = meteorTimeLine.calcul();
        if (BEventFactory.isError(listEvents))
            return;
        
        // execute the steps now
        for (int i=0;i<meteorTimeLine.getListTimeLineSteps().size();i++)
        {
            TimeLineStep timeLine = meteorTimeLine.getListTimeLineSteps().get( i );
            try {
                Thread.sleep( timeLine.timeToWaitBeforeInms );
            } catch (InterruptedException e) {
              
            }
            // now search the tasks 
            
            // find the task: execute it
        }
        
    }

    
    
    public MeteorTimeLine createTimeLine() {
        return new MeteorTimeLineBasic( 12L );
    }
}
