package org.bonitasoft.meteor.scenario.groovy;

import java.io.Serializable;
import java.util.logging.Level;

import com.bonitasoft.scenario.runner.RunListener;
import com.bonitasoft.scenario.runner.context.RunContext;

class AdvancementListener extends RunListener {

    private MeteorRobotGroovyScenario meteorRobotGroovyScenario = null;

    public AdvancementListener(RunContext runContext, MeteorRobotGroovyScenario meteorRobotGroovyScenario) {
        this.meteorRobotGroovyScenario = meteorRobotGroovyScenario;
    }

    @Override
    public void advancementCallback(Integer advancement) {
        meteorRobotGroovyScenario.setOperationIndex(advancement);
    }

    @Override
    public void logCallback(Level level, Serializable message) {
        // Do nothing
    }

    @Override
    public void catchEvent(Serializable event) {
        // Do nothing
    }
}
