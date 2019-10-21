package org.bonitasoft.meteor.scenario.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;
import org.bonitasoft.meteor.scenario.process.MeteorRobotActivity;
import org.bonitasoft.meteor.scenario.process.MeteorDefInputs;
import org.bonitasoft.meteor.scenario.process.MeteorDocument;

public class SentenceExecuteTask extends Sentence {

	public static String Verb = "EXECUTETASK";

	private static BEvent EventNoProcessname = new BEvent(SentenceExecuteTask.class.getName(), 1, Level.APPLICATIONERROR, "No process name/version", "The ExecuteTask sentence need 2 the process name and version", "The sentence will not be executed", "Check the sentence");

	private static BEvent EventNoProcessFound = new BEvent(SentenceExecuteTask.class.getName(), 2, Level.APPLICATIONERROR, "The process is not found from the name / version", "By the name and the version, no process is found", "The sentence will not be executed", "Check the sentence");
	private static BEvent EventNoTaskFound = new BEvent(SentenceExecuteTask.class.getName(), 3, Level.APPLICATIONERROR, "No task name is given", "The ExecuteTask sentence need a task name", "The sentence will not be executed", "Check the sentence");

	private static BEvent EventTaskExecutionError = new BEvent(SentenceExecuteTask.class.getName(), 4, Level.APPLICATIONERROR, "A task can't be created", "The task execution failed", "The task is not executed", "Check the sentence");
	private static BEvent EventNoTaskExecuted = new BEvent(SentenceExecuteTask.class.getName(), 5, Level.APPLICATIONERROR, "No task is ready to be executed", "The robot failed to execute a tasks", "The execution is not completed", "Check your scenario");

	/** execute a task */
	/**
	 * ExecuteTask(processName, ProcessVersion, TaskName, WaitTaskArriveInMs,
	 * SleepTaskInMs [ , contrat=value] ); Default Constructor.
	 *
	 * @param listParams
	 * @param apiAccessor
	 */
	public SentenceExecuteTask(final Map<String, Object> mapParam, final APIAccessor apiAccessor) {
		super(mapParam, apiAccessor);
	}

	String processName;
	String processVersion;
	Long processDefinitionId;
	public String taskName;
	public Long taskId;
	long nbExecution = 1;

	MeteorDefInputs meteorInputs = new MeteorDefInputs();

	List<MeteorDocument> listDocuments = new ArrayList<MeteorDocument>();

	@Override
	public List<BEvent> decodeSentence(int lineNumber) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		try {
			processName = getParam(cstParamProcessName);
			processVersion = getParam(cstParamProcessVersion);
			taskName = getParam(cstParamTaskName);
			nbExecution = getParamLong(cstParamNbExecution, 1L);

			if (processName == null) {
				listEvents.add(EventNoProcessname);
			} else {
				if (processVersion == null || processVersion.trim().length() == 0)
					processDefinitionId = apiAccessor.getProcessAPI().getLatestProcessDefinitionId(processName);
				else
					processDefinitionId = apiAccessor.getProcessAPI().getProcessDefinitionId(processName, processVersion);

				// taskId = apiAccessor.getProcessAPI().get
				// ProcessDefinitionId(processName, processVersion);
			}
			if (taskName == null || taskName.trim() == "") {
				listEvents.add(new BEvent(EventNoTaskFound, "line " + lineNumber));

			}
			meteorInputs.addContent(getJsonVariables(cstParamInput));

		} catch (final ProcessDefinitionNotFoundException pe) {
			listEvents.add(new BEvent(EventNoProcessFound, "process[" + processName + "] version[" + processVersion + "] at line " + lineNumber));

		}
		return listEvents;
	}

	@Override
	public List<BEvent> execute(int robotId, LogExecution logExecution) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();

		try {
			for (int i = 0; i < nbExecution; i++) {
				Long taskId = MeteorRobotActivity.executeActivity(processDefinitionId, taskName,meteorInputs, meteorInputs.getInputAtStep(0), 0, robotId, logExecution, apiAccessor.getProcessAPI(), apiAccessor.getIdentityAPI());
				if (taskId == null)
					logExecution.addEvent(new BEvent(EventNoTaskExecuted, "taskName[" + taskName + "]"));
				else
					logExecution.addLog("task(" + String.valueOf(taskId) + ")");
			}
		} catch (final Exception e) {
			BEvent event = new BEvent(EventTaskExecutionError, e, "process[" + processName + "] version[" + processVersion + "] processDefinitionId[" + processDefinitionId + "]");
			logExecution.addEvent(event);
			listEvents.add(event);
		}

		return listEvents;
	}

}
