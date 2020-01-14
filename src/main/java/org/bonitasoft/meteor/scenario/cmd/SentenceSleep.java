package org.bonitasoft.meteor.scenario.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;

public class SentenceSleep extends Sentence {

    public static String Verb = "SLEEP";

    public SentenceSleep(final Map<String, Object> mapParam, final APIAccessor apiAccessor) {
        super(mapParam, apiAccessor);
    }

    private Long sleepInMs;

    @Override
    public List<BEvent> decodeSentence(int lineNumber) {
        final List<BEvent> listEvents = new ArrayList<BEvent>();
        sleepInMs = getParamLong(cstParamSleepInMs, null);
        return listEvents;

    }

    @Override
    public List<BEvent> execute(int robotId, LogExecution logExecution) {
        if (sleepInMs != null)
            try {
                Thread.sleep(sleepInMs);
            } catch (InterruptedException e) {
            }
        return new ArrayList<BEvent>();
    }

}
