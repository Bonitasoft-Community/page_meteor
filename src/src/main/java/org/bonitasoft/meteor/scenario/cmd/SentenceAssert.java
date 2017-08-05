package org.bonitasoft.meteor.scenario.cmd;

import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;

public class SentenceAssert extends Sentence {

	public static String Verb = "ASSERT";

	public SentenceAssert(final Map<String,Object> mapParam, final APIAccessor apiAccessor) {
		super(mapParam, apiAccessor);
	}

	@Override
	public List<BEvent> decodeSentence( int lineNumber) {
		return null;
	}

	@Override
	public List<BEvent> execute(int robotId, LogExecution logExecution) {
		return null;
	}

}
