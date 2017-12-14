package org.bonitasoft.meteor.scenario.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;
import org.bonitasoft.meteor.scenario.process.MeteorRobotCreateCase;
import org.bonitasoft.meteor.scenario.process.MeteorDefInputs;
import org.bonitasoft.meteor.scenario.process.MeteorDocument;

public class SentenceCreateCase extends Sentence {

	private static BEvent EventCreateCaseNoProcessname = new BEvent(SentenceCreateCase.class.getName(), 1, Level.APPLICATIONERROR, "CreateCase(processName, ProcessVersion)", "CreateCase need 2 parameters minimum : name and version", "The sentence will not be executed", "Check the sentence");

	private static BEvent EventCreateCaseNoProcessFound = new BEvent(SentenceCreateCase.class.getName(), 2, Level.APPLICATIONERROR, "A process is not found from the name / version", "By the name and the version, no process is found", "The sentence will not be executed", "Check the sentence");

	private static BEvent EventCreateCaseParamIncorrect = new BEvent(SentenceCreateCase.class.getName(), 3, Level.APPLICATIONERROR, "Syntaxe error on parameter : variable=value expected", "CreateCase need 2 parameters minimum : name and version", "The sentence will not be executed",
			"Check the sentence");

	private static BEvent EventCreateCaseError = new BEvent(SentenceCreateCase.class.getName(), 4, Level.APPLICATIONERROR, "Error during creation", "The case can't be created", "No case will be created", "Check the message");

	public static String Verb = "CREATECASE";

	public SentenceCreateCase(final Map<String, Object> mapParam, final APIAccessor apiAccessor) {
		super(mapParam, apiAccessor);
	}

	String processName;
	String processVersion;
	Long processDefinitionId;
	long nbExecution = 1;
	MeteorDefInputs meteorInput = new MeteorDefInputs();
	List<MeteorDocument> listDocuments = new ArrayList<MeteorDocument>();

	@Override
	public List<BEvent> decodeSentence(int lineNumber) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		try {
			processName = getParam(cstParamProcessName);
			processVersion = getParam(cstParamProcessVersion);
			nbExecution = getParamLong(cstParamNbExecution, 1L);
			if (processName == null) {
				listEvents.add(EventCreateCaseNoProcessname);
			} else {
				if (processVersion == null || processVersion.trim().length() == 0)
					processDefinitionId = apiAccessor.getProcessAPI().getLatestProcessDefinitionId(processName);
				else
					processDefinitionId = apiAccessor.getProcessAPI().getProcessDefinitionId(processName, processVersion);
			}

			meteorInput.addContent(getJsonVariables(cstParamInput));

		} catch (final ProcessDefinitionNotFoundException pe) {
			listEvents.add(new BEvent(EventCreateCaseNoProcessFound, "process[" + processName + "] version[" + processVersion + "] at line " + lineNumber));
		}
		return listEvents;
	}

	@Override
	public List<BEvent> execute(int robotId, LogExecution logExecution) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();

		try {
			for (int i = 0; i < nbExecution; i++) {
				ProcessInstance processInstance = MeteorRobotCreateCase.createACase(processDefinitionId, false, meteorInput.getInputAtStep(0), listDocuments, logExecution, apiAccessor.getProcessAPI());
			}

		} catch (final Exception e) {
			BEvent event = new BEvent(EventCreateCaseError, e, "process[" + processName + "] version[" + processVersion + "] processDefinitionId[" + processDefinitionId + "]");
			logExecution.addEvent(event);
			listEvents.add(event);
		}

		return listEvents;
	}

}
