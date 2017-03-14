package com.bonitasoft.custompage.meteor.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

import com.bonitasoft.custompage.meteor.MeteorRobotCreateCase;
import com.bonitasoft.custompage.meteor.MeteorProcessDefinitionList.meteorDocument;


public class SentenceExecuteTask extends Sentence {

    public static String Verb = "EXECUTETASK";

    private static BEvent EventCreateCaseNoProcessname = new BEvent(SentenceExecuteTask.class.getName(), 1, Level.APPLICATIONERROR,
            "CreateCase(processName, ProcessVersion)",
            "CreateCase need 2 parameters minimum : name and version",
            "The sentence will not be executed",
            "Check the sentence");

    private static BEvent EventCreateCaseNoProcessFound = new BEvent(SentenceCreateCase.class.getName(), 2, Level.APPLICATIONERROR,
            "A process is not found from the name / version",
            "By the name and the version, no process is found",
            "The sentence will not be executed",
            "Check the sentence");

    /** execute a task */
    /**
     * ExecuteTask(processName, ProcessVersion, TaskName, WaitTaskArriveInMs, SleepTaskInMs [ , contrat=value] );
     * Default Constructor.
     *
     * @param listParams
     * @param apiAccessor
     */
    public SentenceExecuteTask(final List<String> listParams, final APIAccessor apiAccessor) {
        super(listParams, apiAccessor);
    }

    String processName;
    String processVersion;
    Long processDefinitionId;
    public String taskName;
    public Long taskId;
    public long waitTaskArriveInMs;
    public long sleepTaskInMs;

    Map<String, Object> variables = new HashMap<String, Object>();
    List<meteorDocument> listDocuments = new ArrayList<meteorDocument>();

    @Override
    public List<BEvent> decodeSentence() {
        final List<BEvent> listEvents = new ArrayList<BEvent>();
        try
        {
                processName = removeQuote( getParam( 0 ) );
                processVersion = removeQuote( getParam( 1 )  );
                taskName= removeQuote( getParam( 2 )  );
                waitTaskArriveInMs = getParamLong( 3 );
                sleepTaskInMs = getParamLong( 4 );

                if (processVersion==null)
                {
                    listEvents.add(EventCreateCaseNoProcessname);
                }
                else
                {
                    processDefinitionId = apiAccessor.getProcessAPI().getProcessDefinitionId(processName, processVersion);
                    taskId = apiAccessor.getProcessAPI().get ProcessDefinitionId(processName, processVersion);
                }

                variables = getMapVariables( 2 );

        } catch (final ProcessDefinitionNotFoundException pe)
        {
            listEvents.add(new BEvent(EventCreateCaseNoProcessFound, "process[" + processName + "] version[" + processVersion + "]"));

        }
        return listEvents;
    }

    @Override
    public List<BEvent> execute() {
        final List<BEvent> listEvents = new ArrayList<BEvent>();

        try
        {
            MeteorRobotActivity.executeATask(processDefinitionId, taskName, variables, listDocuments, apiAccessor.getProcessAPI());
        } catch (final Exception e)
        {
            listEvents.add(new BEvent(EventCreateCaseError, e, "process[" + processName + "] version[" + processVersion + "] processDefinitionId["
                    + processDefinitionId + "]"));
        }

        return listEvents;
    }


}
