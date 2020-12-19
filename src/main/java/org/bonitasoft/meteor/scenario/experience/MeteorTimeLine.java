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

import lombok.Data;

public @Data abstract class MeteorTimeLine {

    public final static BEvent eventNoProcessFound = new BEvent(MeteorTimeLine.class.getName(), 1, Level.APPLICATIONERROR, "No process found", "The given process does not exist on the server", "Execution can't be done", "Check process name, process version");

    public final static BEvent eventProcessDisabled = new BEvent(MeteorTimeLine.class.getName(), 2, Level.APPLICATIONERROR, "Process disabled", "The givent process is disabled", "Execution can't be done", "Enable the process name");

    private List<TimeLineStep> listTimeLineSteps = new ArrayList<>();
    private Long rootCaseId;
    private String name;
    private String processName;

    private String processVersion;

    private boolean allowRecentVersion = true;
    private Long processDefinitionId;
    private long nbRobots = 1;
    private long nbCases = 1;
    private long delaySleepMS = 0;
    private long timeBetweenSleepMS = 0;
    private String userNameCreatedBy;
    private boolean anyUserCreatedBy;
    private String executedByUserName;

    private Map<String, Serializable> listContractValues;

    // private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH24:mm:ss SSS");
    private final static String CSTJSON_NAME = "name";
    private final static String CSTJSON_TIMELINEPOLICY = "policy";
    private final static String CSTJSON_ROOTCASEID = "rootcaseid";
    private final static String CSTJSON_PROCESSNAME = "processname";
    private final static String CSTJSON_PROCESSVERSION = "processversion";
    private final static String CSTJSON_USERNAMECREATEDBY = "usernamecreatedby";
    private final static String CSTJSON_ANYUSERCREATEDBY= "anyUserCreatedBy";
    
    private final static String CSTJSON_PROCESSALLOWRECENTVERSION = "allowrecentversion";
    private final static String CSTJSON_TIMELINES = "timelines";
    private final static String CSTJSON_ACTIVITYNAME = "actname";
    private final static String CSTJSON_ACTIVITYDISPLAYNAME = "actdisplayname";
    private final static String CSTJSON_ACTIVITYISMULTIINSTANCIATION = "actismultiinstanciation";
    private final static String CSTJSON_ACTIVITYDEFINITIONID = "defid";
    private final static String CSTJSON_USERNAMEEXECUTEDBY = "executedby";
    private final static String CSTJSON_ANYUSER = "anyUser";

    private final static String CSTJSON_LISTCONTRACTVALUES = "contract";
    private final static String CSTJSON_NBCASES = "nbcases";
    private final static String CSTJSON_NBROBS = "nbrobs";
    private final static String CSTJSON_TIMELINEMS = "timeLineMs";
    private final static String CSTJSON_TIMEFROMBEGININGMS = "timeLineFromBeginningMS";
    private final static String CSTJSON_DELAYSLEEPMS = "delaysleep";
    private final static String CSTJSON_TIMEBETWEENSLEEPMS = "timesleep";
    private final static String CSTJSON_TIMEWAITMS = "timewaitms";

    public MeteorTimeLine() {
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
        Map<String, Object> synthesis = new HashMap<>();
        synthesis.put(CSTJSON_NAME, name);
        synthesis.put(CSTJSON_ROOTCASEID, rootCaseId);
        synthesis.put(CSTJSON_PROCESSNAME, processName);
        synthesis.put(CSTJSON_PROCESSVERSION, processVersion);
        synthesis.put(CSTJSON_USERNAMECREATEDBY, userNameCreatedBy);
        synthesis.put(CSTJSON_ANYUSERCREATEDBY, anyUserCreatedBy);
        
        synthesis.put(CSTJSON_LISTCONTRACTVALUES, listContractValues == null ? null : JSONValue.toJSONString(listContractValues));
        synthesis.put(CSTJSON_NBROBS, nbRobots);
        synthesis.put(CSTJSON_NBCASES, nbCases);
        synthesis.put(CSTJSON_DELAYSLEEPMS, delaySleepMS);
        synthesis.put(CSTJSON_TIMEBETWEENSLEEPMS, timeBetweenSleepMS);

        synthesis.put(CSTJSON_TIMELINEPOLICY, getPolicy());
        List<Map<String, Object>> listTimeLine = new ArrayList<>();
        synthesis.put(CSTJSON_TIMELINES, listTimeLine);
        for (TimeLineStep timeLineStep : listTimeLineSteps) {
            Map<String, Object> timeLineMap = new HashMap<>();
            timeLineMap.put( CSTJSON_ACTIVITYNAME, timeLineStep.activityName);
            timeLineMap.put( CSTJSON_ACTIVITYDISPLAYNAME, timeLineStep.displayName);
            timeLineMap.put( CSTJSON_ACTIVITYISMULTIINSTANCIATION, timeLineStep.isMultiInstanciation);
            timeLineMap.put( CSTJSON_ACTIVITYDEFINITIONID, timeLineStep.sourceActivityDefinitionId);
            timeLineMap.put( CSTJSON_LISTCONTRACTVALUES, timeLineStep.listContractValues == null ? null : JSONValue.toJSONString(timeLineStep.listContractValues));
            timeLineMap.put( CSTJSON_USERNAMEEXECUTEDBY, timeLineStep.executedByUserName);
            timeLineMap.put( CSTJSON_ANYUSER, timeLineStep.anyUser);
            timeLineMap.put( CSTJSON_TIMELINEMS, timeLineStep.timelinems);
            timeLineMap.put( CSTJSON_TIMELINEMS + "_st", MeteorToolbox.getHumanDelay(timeLineStep.timelinems));
            timeLineMap.put( CSTJSON_TIMEWAITMS, timeLineStep.timeWaitms);
            timeLineMap.put( CSTJSON_TIMEWAITMS + "_st", MeteorToolbox.getHumanDelay(timeLineStep.timeWaitms));
            timeLineMap.put( CSTJSON_TIMEFROMBEGININGMS, timeLineStep.timeFromBeginingms);
            timeLineMap.put( CSTJSON_TIMEFROMBEGININGMS+"_st", MeteorToolbox.getHumanDelay(timeLineStep.timeFromBeginingms));
            listTimeLine.add(timeLineMap);
        }

        return synthesis;
    }

    @SuppressWarnings("unchecked")
    public static MeteorTimeLine getInstanceFromJson(Map<String, Object> json) {
        String policy = MeteorToolbox.getParameterString(json, CSTJSON_TIMELINEPOLICY, "");
        MeteorTimeLine timeLine = MeteorTimeLine.getInstance(policy);
        if (timeLine == null)
            return null;
        timeLine.name = MeteorToolbox.getParameterString(json, CSTJSON_NAME, "");
        timeLine.rootCaseId = MeteorToolbox.getParameterLong(json, CSTJSON_ROOTCASEID, null);
        timeLine.processName = MeteorToolbox.getParameterString(json, CSTJSON_PROCESSNAME, "");
        timeLine.processVersion = MeteorToolbox.getParameterString(json, CSTJSON_PROCESSVERSION, "");
        timeLine.userNameCreatedBy = MeteorToolbox.getParameterString(json, CSTJSON_USERNAMECREATEDBY, null);
        timeLine.anyUserCreatedBy =  MeteorToolbox.getParameterBoolean(json, CSTJSON_ANYUSERCREATEDBY, true);
        
        timeLine.allowRecentVersion = MeteorToolbox.getParameterBoolean(json, CSTJSON_PROCESSALLOWRECENTVERSION, false);

        String jsonContract = MeteorToolbox.getParameterString(json, CSTJSON_LISTCONTRACTVALUES, "");
        Object tempList = JSONValue.parse(jsonContract);
        timeLine.listContractValues = (Map<String, Serializable>) (Map<?, ?>) tempList;
        timeLine.nbRobots = MeteorToolbox.getParameterLong(json, CSTJSON_NBROBS, 0L);
        timeLine.nbCases = MeteorToolbox.getParameterLong(json, CSTJSON_NBCASES, 0L);
        timeLine.delaySleepMS = MeteorToolbox.getParameterLong(json, CSTJSON_DELAYSLEEPMS, 0L);
        timeLine.timeBetweenSleepMS = MeteorToolbox.getParameterLong(json, CSTJSON_TIMEBETWEENSLEEPMS, 0L);

        List<Object> listTimeLine = MeteorToolbox.getParameterList(json, CSTJSON_TIMELINES, new ArrayList<>());
        for (Object timeLineMap : listTimeLine) {
            TimeLineStep timeLineStep = timeLine.addOneStep();
            timeLineStep.activityName           = MeteorToolbox.getParameterString((Map<String, Object>) timeLineMap, CSTJSON_ACTIVITYNAME, "");
            timeLineStep.displayName            = MeteorToolbox.getParameterString((Map<String, Object>) timeLineMap, CSTJSON_ACTIVITYDISPLAYNAME, "");
            timeLineStep.isMultiInstanciation   = MeteorToolbox.getParameterBoolean((Map<String, Object>) timeLineMap, CSTJSON_ACTIVITYISMULTIINSTANCIATION, Boolean.FALSE);
            timeLineStep.sourceActivityDefinitionId = MeteorToolbox.getParameterLong((Map<String, Object>) timeLineMap, CSTJSON_ACTIVITYDEFINITIONID, null);
            timeLineStep.timeWaitms = MeteorToolbox.getParameterLong((Map<String, Object>) timeLineMap, CSTJSON_TIMEWAITMS, 0L);
            jsonContract = MeteorToolbox.getParameterString((Map<String, Object>) timeLineMap, CSTJSON_LISTCONTRACTVALUES, "");
            tempList = JSONValue.parse(jsonContract);
            timeLineStep.listContractValues = (Map<String, Serializable>) (Map<?, ?>) tempList;
            timeLineStep.executedByUserName = MeteorToolbox.getParameterString((Map<String, Object>) timeLineMap, CSTJSON_USERNAMEEXECUTEDBY, null);
            timeLineStep.anyUser = MeteorToolbox.getParameterBoolean((Map<String, Object>) timeLineMap, CSTJSON_ANYUSER, true);
        }
        return timeLine;
    }

    public static MeteorTimeLine getInstance(String policy) {
        if (policy.equals(MeteorTimeLineBasic.POLICY))
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
        List<BEvent> listEvents = new ArrayList<>();
        ProcessAPI processAPI = apiAccessor.getProcessAPI();
        try {
            SearchResult<ProcessDeploymentInfo> searchResult = searchVersion(getProcessName(), getProcessVersion(), true, processAPI);
            if (searchResult.getCount() == 0 && getAllowRecentVersion())
                searchResult = searchVersion(getProcessName(), null, false, processAPI);

            if (searchResult.getCount() == 0) {
                listEvents.add(new BEvent(eventNoProcessFound, getEventInformation()));
            } else if (searchResult.getResult().get(0).getActivationState().equals(ActivationState.DISABLED)) {
                listEvents.add(new BEvent(eventProcessDisabled, "Process[" + searchResult.getResult().get(0).getName() + "] Version[" + searchResult.getResult().get(0).getVersion() + "] Disabled, " + getEventInformation()));
            } else {
                processDefinitionId = searchResult.getResult().get(0).getProcessId();
            }
        } catch (SearchException e) {
            listEvents.add(new BEvent(eventNoProcessFound, e, getEventInformation()));
        }
        return listEvents;

    }

    public SearchResult<ProcessDeploymentInfo> searchVersion(String processName, String processVersion, boolean exactVersion, ProcessAPI processAPI) throws SearchException {
        // calculated the processID
        SearchOptionsBuilder searchOptions = new SearchOptionsBuilder(0, 10);
        searchOptions.filter(ProcessDeploymentInfoSearchDescriptor.NAME, processName);
        if (exactVersion)
            searchOptions.filter(ProcessDeploymentInfoSearchDescriptor.VERSION, processVersion);
        searchOptions.sort(ProcessDeploymentInfoSearchDescriptor.DEPLOYMENT_DATE, Order.DESC);

        return processAPI.searchProcessDeploymentInfos(searchOptions.done());
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

    public boolean getAllowRecentVersion() {
        return allowRecentVersion;
    }

    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }

    public String getUserNameCreatedBy() {
        return this.userNameCreatedBy;
    }

    public void setUserNameCreatedBy(String userName) {
        this.userNameCreatedBy = userName;
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

    public String getExecutedByUserName() {
        return executedByUserName;
    }

    public void setExecutedByUserName(String executedByUserName) {
        this.executedByUserName = executedByUserName;
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
     */
    public @Data static class TimeLineStep {

        String activityName;

        /** in a case of a multiinstanciation task, the activityName is not enought, so we have to save the display name too
         * 
         */
        String displayName;
        boolean isMultiInstanciation = false;
        /**
         * Source ObjectId : for a Archive task, this is the Original ID.
         * A Human task has multiple task (INITIALIZING, READY, COMPLETED), all share the same sourceObjectId
         */
        long sourceObjectId;
        /**
         * Identifiant of this activity
         */
        Long sourceActivityDefinitionId;

        /**
         * Date where the time line is executed. It's a date from 1970. Use to sort activity
         */
        Long timelinems;

        /**
         * Relative time form the begining of the case creation
         */
        Long timeFromBeginingms;
        /**
         * time to wait before searching for this steps.
         * Example HActivity_1 executed at 12:01:00
         * Then HActivity_1(connectorOut) + ServiceTask44 + ServiceTask66+HActivity_44(ConnectorIn) => Ready at 12:01:45
         * So, we have to wait 45 s before searching this activity
         */
        long timeWaitms = 0;
        /**
         * contract to execute this activity
         */
        String executedByUserName;
        /*
         * if the username does not exist, any user can be used
         */
        boolean anyUser;

        Map<String, Serializable> listContractValues;

    }

    private String getEventInformation() {
        StringBuilder information = new StringBuilder();
        information.append("Search by Process[" + getProcessName() + "] version[" + getProcessVersion() + "] getAllowLastVersion[" + getAllowRecentVersion() + "]");
        return information.toString();
    }
}
