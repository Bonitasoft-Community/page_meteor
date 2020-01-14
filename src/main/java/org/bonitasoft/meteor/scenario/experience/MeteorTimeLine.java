package org.bonitasoft.meteor.scenario.experience;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorToolbox;
import org.json.simple.JSONValue;

public abstract class MeteorTimeLine {

    public static BEvent EVENT_NOPROCESS_FOUND = new BEvent(MeteorTimeLine.class.getName(), 1, Level.APPLICATIONERROR, "No process found", "The given process does not exist on the server", "Execution can't be done", "Check process name, process version");

    public static BEvent EVENT_PROCESS_DISABLED = new BEvent(MeteorTimeLine.class.getName(), 2, Level.APPLICATIONERROR, "Process disabled", "The givent process is disabled", "Execution can't be done", "Enable the process name");

    private List<TimeLineStep> listTimeLineSteps = new ArrayList<TimeLineStep>();
    private Long rootCaseId;
    private String name;
    private String processName;

    private String processVersion;
    private Long processDefinitionId;
    private long nbRobots = 1;
    private long nbCases = 1;
    private long delaySleepMS = 0;
    private long timeBetweenSleepMS = 0;

    Map<String, Serializable> listContractValues;

    // private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH24:mm:ss SSS");
    private static String cstJsonName = "name";
    private static String cstJsonTimeLinePolicy = "policy";
    private static String cstJsonRootCaseId = "rootcaseid";
    private static String cstJsonProcessName = "processname";
    private static String cstJsonProcessVersion = "processversion";
    private static String cstJsonTimeLines = "timelines";
    private static String cstJsonActivityName = "actname";
    private static String cstJsonActivityDefinitionId = "defid";
    private static String cstJsonListContractValues = "contract";
    private static String cstJsonnbcases = "nbcases";
    private static String cstJsonnbrobs = "nbrobs";
    private static String cstJsonTimelineMS = "timeLineMs";
    private static String cstJsonTimeFromBeginingMS = "timeLineFromBeginningMS";
    private static String cstJsonDelaySleepMS = "delaysleep";
    private static String cstJsonTimeBetweenSleepMs = "timesleep";
    private static String cstJsonTimeWaitMs = "timewaitms";

    public MeteorTimeLine() {
        ;
    }

    /**
     * calcul the timeLine
     * 
     * @return
     */
    public abstract List<BEvent> calcul(Long rootCaseId, ProcessAPI processAPI, IdentityAPI identityAPI);

    public abstract String getPolicy();

    /** return a synthesis of calculation to show to user, after calculation */
    public Map<String, Object> getJson() {
        Map<String, Object> synthesis = new HashMap<String, Object>();
        synthesis.put(cstJsonName, name);
        synthesis.put(cstJsonRootCaseId, rootCaseId);
        synthesis.put(cstJsonProcessName, processName);
        synthesis.put(cstJsonProcessVersion, processVersion);
        synthesis.put(cstJsonListContractValues, listContractValues == null ? null : JSONValue.toJSONString(listContractValues));
        synthesis.put(cstJsonnbrobs, nbRobots);
        synthesis.put(cstJsonnbcases, nbCases);
        synthesis.put(cstJsonDelaySleepMS, delaySleepMS);
        synthesis.put(cstJsonTimeBetweenSleepMs, timeBetweenSleepMS);

        synthesis.put(cstJsonTimeLinePolicy, getPolicy());
        List<Map<String, Object>> listTimeLine = new ArrayList<Map<String, Object>>();
        synthesis.put(cstJsonTimeLines, listTimeLine);
        for (TimeLineStep timeLineStep : listTimeLineSteps) {
            Map<String, Object> timeLineMap = new HashMap<String, Object>();
            timeLineMap.put(cstJsonActivityName, timeLineStep.activityName);
            timeLineMap.put(cstJsonActivityDefinitionId, timeLineStep.sourceActivityDefinitionId);
            timeLineMap.put(cstJsonListContractValues, timeLineStep.listContractValues == null ? null : JSONValue.toJSONString(timeLineStep.listContractValues));
            timeLineMap.put(cstJsonTimelineMS, timeLineStep.timelinems);
            timeLineMap.put(cstJsonTimelineMS + "_st", MeteorToolbox.getHumanDelay(timeLineStep.timelinems));
            timeLineMap.put(cstJsonTimeWaitMs, MeteorToolbox.getHumanDelay(timeLineStep.timeWaitms));
            timeLineMap.put(cstJsonTimeFromBeginingMS, timeLineStep.timeFromBeginingms);

            listTimeLine.add(timeLineMap);
        }

        return synthesis;
    }

    @SuppressWarnings("unchecked")
    public static MeteorTimeLine getInstanceFromJson(Map<String, Object> json) {
        String policy = MeteorToolbox.getParameterString(json, cstJsonTimeLinePolicy, "");
        MeteorTimeLine timeLine = MeteorTimeLine.getInstance(policy);

        timeLine.name = MeteorToolbox.getParameterString(json, cstJsonName, "");
        timeLine.rootCaseId = MeteorToolbox.getParameterLong(json, cstJsonRootCaseId, null);
        timeLine.processName = MeteorToolbox.getParameterString(json, cstJsonProcessName, "");
        timeLine.processVersion = MeteorToolbox.getParameterString(json, cstJsonProcessVersion, "");
        String jsonContract = MeteorToolbox.getParameterString(json, cstJsonListContractValues, "");
        Object tempList = JSONValue.parse(jsonContract);
        timeLine.listContractValues = (Map<String, Serializable>) (Map<?,?>) tempList;
        timeLine.nbRobots = MeteorToolbox.getParameterLong(json, cstJsonnbrobs, 0L);
        timeLine.nbCases = MeteorToolbox.getParameterLong(json, cstJsonnbcases, 0L);
        timeLine.delaySleepMS = MeteorToolbox.getParameterLong(json, cstJsonDelaySleepMS, 0L);
        timeLine.timeBetweenSleepMS = MeteorToolbox.getParameterLong(json, cstJsonTimeBetweenSleepMs, 0L);

        List<Object> listTimeLine = MeteorToolbox.getParameterList(json, cstJsonTimeLines, new ArrayList<Object>());
        for (Object timeLineMap : listTimeLine) {
            TimeLineStep timeLineStep = timeLine.addOneStep();
            timeLineStep.activityName = MeteorToolbox.getParameterString((Map<String, Object>) timeLineMap, cstJsonActivityName, "");
            timeLineStep.sourceActivityDefinitionId = MeteorToolbox.getParameterLong((Map<String, Object>) timeLineMap, cstJsonActivityDefinitionId, null);
            timeLineStep.timeWaitms = MeteorToolbox.getParameterLong((Map<String, Object>) timeLineMap, cstJsonTimeWaitMs, 0L);
            jsonContract = MeteorToolbox.getParameterString((Map<String, Object>) timeLineMap, cstJsonListContractValues, "");
            tempList = JSONValue.parse(jsonContract);
            timeLineStep.listContractValues = (Map<String, Serializable>) (Map<?,?>) tempList;

        }
        return timeLine;
    }

    public static MeteorTimeLine getInstance(String policy) {
        if (policy.equals(MeteorTimeLineBasic.policy))
            return new MeteorTimeLineBasic();
        return null;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Initialise */
    /*                                                                          */
    /* ************************************************************************ */
    /**
     * @param apiAccessor
     * @return
     */
    public List<BEvent> initialize(APIAccessor apiAccessor) {
        List<BEvent> listEvents = new ArrayList<BEvent>();
        ProcessAPI processAPI = apiAccessor.getProcessAPI();
        // calculated the processID
        SearchOptionsBuilder searchOptions = new SearchOptionsBuilder(0, 10);
        searchOptions.filter(ProcessDeploymentInfoSearchDescriptor.NAME, getProcessName());
        if (getProcessVersion().trim().length() > 0)
            searchOptions.filter(ProcessDeploymentInfoSearchDescriptor.VERSION, getProcessVersion());
        searchOptions.sort(ProcessDeploymentInfoSearchDescriptor.DEPLOYMENT_DATE, Order.DESC);
        SearchResult<ProcessDeploymentInfo> searchResult;
        try {
            searchResult = processAPI.searchProcessDeploymentInfos(searchOptions.done());

            if (searchResult.getCount() == 0) {
                listEvents.add(new BEvent(EVENT_NOPROCESS_FOUND, "Process[" + getProcessName() + "] version[" + getProcessVersion() + "]"));
            } else if (searchResult.getResult().get(0).getActivationState().equals(ActivationState.DISABLED)) {
                listEvents.add(new BEvent(EVENT_PROCESS_DISABLED, "Process[" + getProcessName() + "] version[" + getProcessVersion() + "]"));
            } else {
                processDefinitionId = searchResult.getResult().get(0).getProcessId();
            }
        } catch (SearchException e) {
            listEvents.add(new BEvent(EVENT_NOPROCESS_FOUND, e, "Process[" + getProcessName() + "] version[" + getProcessVersion() + "]"));
        }
        return listEvents;

    }

    /* ************************************************************************ */
    /*                                                                          */
    /* getter/setter */
    /*                                                                          */
    /* ************************************************************************ */

    public Long getRootCaseId() {
        return rootCaseId;
    }

    public void setRootCaseId(Long rootCaseId) {
        this.rootCaseId = rootCaseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessVersion() {
        return processVersion;
    }

    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }

    public Map<String, Serializable> getListContractValues() {
        return listContractValues;
    }

    public void setListContractValues(Map<String, Serializable> listContractValues) {
        this.listContractValues = listContractValues;
    }

    public long getNbRobots() {
        return nbRobots;
    }

    public void setNbRobots(long nbRobots) {
        this.nbRobots = nbRobots;
    }

    public long getNbCases() {
        return nbCases;
    }

    public void setNbCases(long nbCases) {
        this.nbCases = nbCases;
    }

    public long getTimeBetweenSleepMS() {
        return timeBetweenSleepMS;
    }

    public void setTimeBetweenSleepMS(long timeBetweenSleepMS) {
        this.timeBetweenSleepMS = timeBetweenSleepMS;
    }

    public long getDelaySleepMs() {
        return delaySleepMS;
    }

    public void setDelaySleepMS(long delaySleepMS) {
        this.delaySleepMS = delaySleepMS;
    }

    public Long getProcessDefinitionId() {
        return processDefinitionId;
    }
    /* ************************************************************************ */
    /*                                                                          */
    /* Steps */
    /*                                                                          */
    /* ************************************************************************ */

    public TimeLineStep addOneStep() {
        TimeLineStep timeLineActivity = new TimeLineStep();
        listTimeLineSteps.add(timeLineActivity);
        return timeLineActivity;
    }

    public List<TimeLineStep> getListTimeLineSteps() {
        return listTimeLineSteps;
    }

    /**
     * timeLineStep
     * 
     * @author Firstname Lastname
     */
    public static class TimeLineStep {

        String activityName;

        /**
         * Identifiant of this activity
         */
        Long sourceActivityDefinitionId;

        /**
         * Date where the time line is executed
         */

        Long timelinems;

        Long timeFromBeginingms;
        /**
         * time to wait before searching for this steps
         */
        long timeWaitms = 0;
        /**
         * contract to execute this activity
         */
        Map<String, Serializable> listContractValues;
    }

}
