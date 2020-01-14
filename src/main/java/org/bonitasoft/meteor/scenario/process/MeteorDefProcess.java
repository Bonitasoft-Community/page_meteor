package org.bonitasoft.meteor.scenario.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractDefinition;
import org.bonitasoft.engine.bpm.flownode.ActivityDefinition;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.flownode.FlowNodeDefinition;
import org.bonitasoft.engine.bpm.flownode.StartEventDefinition;
import org.bonitasoft.engine.bpm.flownode.TransitionDefinition;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.MeteorToolbox;

/*
 * The process Meteor correspond to a process found in the Server. Then, this class will create one line for the Process Creation, an done line for each Human
 * Task found in the process
 */
public class MeteorDefProcess extends MeteorDefBase {

    private final Logger logger = Logger.getLogger(MeteorScenarioProcess.class.getName());

    /**
     * Keep information on a process A MeteorProcessDefinition will create one
     * or multiple robot.
     */
    private static BEvent EventGetContract = new BEvent(MeteorScenarioProcess.class.getName(), 1, Level.ERROR, "Accessing contract", "Check error ", "The contact can't be accessed", "Check Exception");
    private static BEvent EventGetProcessDesign = new BEvent(MeteorScenarioProcess.class.getName(), 2, Level.ERROR, "Accessing Process Design", "Check error ", "The happyPath can't be calculated, and then the cover percentage", "Check Exception");
    private static BEvent EventSearchActivities = new BEvent(MeteorScenarioProcess.class.getName(), 2, Level.ERROR, "Searching activities", "Check error", "To calculate the cover, all activities ran from the list of process is searched. The operation failed", "Check Exception");
    private static BEvent EventGetProcess = new BEvent(MeteorScenarioProcess.class.getName(), 3, Level.ERROR, "Accessing process information", "Can't get information about the process", "The call to collect information about the process failed ", "Check Exception");
       // Attention, the processDefinitionID must be recalculated each time:
    // process may be redeployed
    public Long mProcessDefinitionId;
    public String mProcessName;
    public String mProcessVersion;

    /**
     * all MeteorActivity to run by a robot
     */
    private List<MeteorDefActivity> mListActivities = new ArrayList<MeteorDefActivity>();
    /**
     * all activities of the process, in order to calculate the cover
     */
    private List<ActivityDefinition> mListAllActivities = new ArrayList<ActivityDefinition>();

    public MeteorDefProcess(Long processDefinitionId) {
        mProcessDefinitionId = processDefinitionId;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*Initialisation */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */
    public List<BEvent> initialise(ProcessAPI processAPI) {
        List<BEvent> listEvents = new ArrayList<BEvent>();
        ProcessDefinition processDefinition;
        try {
            processDefinition = processAPI.getProcessDefinition(mProcessDefinitionId);
            mProcessName = processDefinition.getName();
            mProcessVersion = processDefinition.getVersion();
        } catch (ProcessDefinitionNotFoundException e) {
            listEvents.add( new BEvent( EventGetProcess, e, "ProcessDefinitionId ["+mProcessDefinitionId+"]"));
        }
        return listEvents;
    }
    /* ******************************************************************** */
    /*                                                                      */
    /* GetterSetter */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */
    public String getInformation() {
        return mProcessName + "(" + mProcessVersion + ")";
    }

    /** return an activity */
    public MeteorDefActivity getActivity(final String activityName) {
        for (final MeteorDefActivity meteorActivity : mListActivities) {
            if (meteorActivity.mActivityName.equals(activityName)) {
                return meteorActivity;
            }
        }
        return null;
    }

    /**
     * return the list of activities
     * 
     * @return
     */
    public List<MeteorDefActivity> getListActivities() {
        return mListActivities;
    }

    public void addActivity(MeteorDefActivity activity) {
        mListActivities.add(activity);
    }

    public void addBonitaActivity(ActivityDefinition activity) {
        mListAllActivities.add(activity);
    }

    public Map<String, Object> getMap() {
        final Map<String, Object> oneProcess = new HashMap<String, Object>();

        // attention : the processdefinitionId is very long it has to be set
        // in STRING else JSON will do an error
        oneProcess.put(MeteorScenarioProcess.cstHtmlId, mProcessDefinitionId.toString());

        oneProcess.put(MeteorScenarioProcess.cstJsonProcessName, mProcessName);
        oneProcess.put(MeteorScenarioProcess.cstHtmlProcessVersion, mProcessVersion);
        fullfillMap(oneProcess);
        return oneProcess;
    }

    public static MeteorDefProcess getInstanceFromMap(final Map<String, Object> oneProcess, ProcessAPI processAPI) {

        // attention : the processdefinitionId is very long it has to be set
        // in STRING else JSON will do an error
        Long processDefinitionId = MeteorToolbox.getParameterLong(oneProcess, MeteorScenarioProcess.cstHtmlId, -1L);
        MeteorDefProcess meteorDefProcess = new MeteorDefProcess(processDefinitionId);
        meteorDefProcess.initialise( processAPI );

     
        meteorDefProcess.decodeFromMap(oneProcess, processAPI);
        return meteorDefProcess;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* ContractDefinition */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public void calculContractDefinition(ProcessAPI processAPI) {
        try {
            ContractDefinition contractDefinition = processAPI.getProcessContract(mProcessDefinitionId);
            setContractDefinition(contractDefinition);
        } catch (Exception e) {
            mListEvents.add(new BEvent(EventGetContract, e, "Process[" + mProcessName + "] Version[" + mProcessVersion + "]"));
        }

    }

    /**
     * override the methid : first, get the contract
     */
    public void prepareInputs(ProcessAPI processAPI) {
        calculContractDefinition(processAPI);
        prepareInputs();
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* Calcul cover */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public class CoverResult {

        public boolean mCoverAll;
        /**
         * 0=>100
         */
        public int mCoverPercent = 0;
        public int mCoverHappyPathPercent = 0;

        /* we conserve the number of execution per activityDefinitionId */
        Map<String, Long> mActivitiesExecutedNb = new HashMap<String, Long>();
        /**
         * activity Not Executed
         */
        List<String> mActivitiesNotExecuted = new ArrayList<String>();
        /*
         * Activities of the Happypath : this activities can be reach not on an error, or timer
         */
        List<String> mDefinitionActivitiesHappyPath = new ArrayList<String>();

        public List<BEvent> mListEvents = new ArrayList<BEvent>();

        public Map<String, Object> getMap() {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put(MeteorScenarioProcess.cstJsonProcessName, mProcessName);
            result.put(MeteorScenarioProcess.cstHtmlProcessVersion, mProcessVersion);
            result.put(MeteorScenarioProcess.cstHtmlCoverAll, mCoverAll);
            result.put(MeteorScenarioProcess.cstHtmlCoverPercent, mCoverPercent);
            result.put(MeteorScenarioProcess.cstHtmlCoverHappyPathPercent, mCoverHappyPathPercent);
            result.put(MeteorScenarioProcess.cstJsonActivitiesNotCovered, mActivitiesNotExecuted);
            result.put(MeteorScenarioProcess.cstJsonListEvents, BEventFactory.getHtml(mListEvents));
            /*
             * {
             * "type": "ColumnChart",
             * "cssStyle": "height:200px; width:300px;",
             * "data": { [ {'Task', 'Hours per Day'],
             * { "name":"Work", y:11},
             * {"name":"Eat", y:2},
             */
            List<Map<String, Object>> listData = new ArrayList<Map<String, Object>>();
            listData.add(getRecord("Cover", 12));
            listData.add(getRecord("Happy", 34));
            Map<String, Object> piechart = new HashMap<String, Object>();
            piechart.put("type", "PieChart");
            piechart.put("displayed", Boolean.TRUE);
            piechart.put("data", listData);

            result.put("piechart", piechart);

            Map<String, Object> columnChart = new HashMap<String, Object>();
            columnChart.put("type", "ColumnChart");
            columnChart.put("displayed", Boolean.TRUE);
            Map<String, Object> colData = new HashMap<String, Object>();
            colData.put("cols", Arrays.asList("Cover", "Happy"));
            colData.put("rows", Arrays.asList(12, 34));

            columnChart.put("data", colData);

            result.put("columChart", columnChart);

            // final String valueChart = "	{" + "\"type\": \"ColumnChart\", " + "\"displayed\": true, " + "\"data\": {" + "\"cols\": [" + resultLabel + "], " + "\"rows\": [" + resultValue + "] "

            return result;
        }
    }

    private Map<String, Object> getRecord(String name, int value) {
        Map<String, Object> record = new HashMap<String, Object>();
        record.put("name", name);
        record.put("y", value);
        return record;
    }

    private CoverResult coverResult;

    public CoverResult calculCover(List<Long> listProcessInstances, ProcessAPI processAPI) {
        coverResult = new CoverResult();
       
        // Search all activities according this lists
        Map<Long, Long> activitiesExecutedNb = calculActivities(listProcessInstances, processAPI);

        // calcul the HappyPath
        coverResult.mDefinitionActivitiesHappyPath = calculHappyPath(processAPI);

        // Check if all Activity is cover

        for (ActivityDefinition actDefinition : mListAllActivities) {
            if (activitiesExecutedNb.containsKey(actDefinition.getId()))
                coverResult.mActivitiesExecutedNb.put(actDefinition.getName(), activitiesExecutedNb.get(actDefinition.getId()));
            else
                coverResult.mActivitiesNotExecuted.add(actDefinition.getName());
        }
        // calcul the percentage
        coverResult.mCoverPercent = (int) (100.0 * coverResult.mActivitiesExecutedNb.size() / mListAllActivities.size());

        // calcul the happy path
        int nbHappyExecuted = 0;
        for (String happyActivity : coverResult.mDefinitionActivitiesHappyPath) {
            if (coverResult.mActivitiesExecutedNb.containsKey(happyActivity))
                nbHappyExecuted++;
        }
        coverResult.mCoverHappyPathPercent = (int) (100.0 * nbHappyExecuted / coverResult.mDefinitionActivitiesHappyPath.size());

        coverResult.mListEvents = mListEvents;

        return coverResult;
    }

    public CoverResult getCoverResult() {
        return coverResult;
    }

    /**
     * calcul the HappyPath, by the StartEvent, and then follow all transitions
     * 
     * @param processAPI
     * @return
     */
    private List<String> calculHappyPath(ProcessAPI processAPI) {
        List<String> listHappyPath = new ArrayList<String>();
        // We have to identify all the activity in the Happy Path. Start by the Start Event
        List<Long> exploreList = new ArrayList<Long>();
        try {
            final DesignProcessDefinition designProcessDefinition = processAPI.getDesignProcessDefinition(mProcessDefinitionId);
            final FlowElementContainerDefinition flowElementContainerDefinition = designProcessDefinition.getFlowElementContainer();

            List<StartEventDefinition> listStartEventsDefinition = flowElementContainerDefinition.getStartEvents();
            mListAllActivities = flowElementContainerDefinition.getActivities();

            for (StartEventDefinition startEvent : listStartEventsDefinition)
                exploreList.add(startEvent.getId());

            Set<Long> markList = new HashSet<Long>();
            while (exploreList.size() > 0) {
                Long idToExplore = exploreList.get(0);
                exploreList.remove(0);

                markList.add(idToExplore);
                FlowNodeDefinition flowNodeDefinition = flowElementContainerDefinition.getFlowNode(idToExplore);
                logger.info("MeteorDefProcess.calculHappyPath on [" + flowNodeDefinition.getName() + "]");
                if (flowNodeDefinition instanceof ActivityDefinition) {

                    // manage only the activityDefinition
                    listHappyPath.add(flowNodeDefinition.getName());
                }

                List<TransitionDefinition> listOutgoing = flowNodeDefinition.getOutgoingTransitions();
                for (TransitionDefinition transition : listOutgoing) {
                    Long idTarget = transition.getTarget();
                    if (! markList.contains(idTarget))
                        exploreList.add(idTarget);
                }
                TransitionDefinition transition = flowNodeDefinition.getDefaultTransition();
                if (transition != null) {
                    Long idTarget = transition.getTarget();
                    if (! markList.contains(idTarget))
                        exploreList.add(idTarget);
                }

            }
        } catch (Exception e) {
            mListEvents.add(new BEvent(EventGetProcessDesign, e, "ProcessId[" + mProcessDefinitionId + "] Name[" + mProcessName + "] Version[" + mProcessVersion + "]"));
        }
        return listHappyPath;
    }

    /**
     * return the number of execution per activityId
     * 
     * @param listProcessInstances
     * @param processAPI
     * @return
     */
    private Map<Long, Long> calculActivities(List<Long> listProcessInstances, ProcessAPI processAPI) {
        Map<Long, Long> activitiesExecutedNb = new HashMap<Long, Long>();
        // listprocessinstance maybe big, search by page of pageProcess 
        int pageProcessInstance = 50; /* limited by the filter size construction */
        int processIndex = 0;
        while (processIndex < listProcessInstances.size()) {
            int lastIndex = processIndex + pageProcessInstance;
            if (lastIndex >= listProcessInstances.size())
                lastIndex = listProcessInstances.size() - 1;
            referenceActivities(activitiesExecutedNb, listProcessInstances, processIndex, lastIndex, processAPI);

            processIndex += pageProcessInstance;
        }

        return activitiesExecutedNb;
    }

    /**
     * reference the activities for the list of processInstance
     * 
     * @param activitiesExecutedNb
     * @param listProcessInstances
     * @param firstIndex
     * @param lastIndex
     * @param processAPI
     */
    private void referenceActivities(Map<Long, Long> activitiesExecutedNb, List<Long> listProcessInstances, int firstIndex, int lastIndex, ProcessAPI processAPI) {
        // search for processIndex to processIndex + pageProcess page
        int pageActivity = 500;
        int activityIndex = 0;
        int protectTheLoop = 0;
        SearchResult<ArchivedActivityInstance> searchResult = null;
        do {
            protectTheLoop++;
            String listInstanceSearched = "";
            SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(activityIndex * pageActivity, pageActivity);
            // build the filter from processIndex -> processIndex+pageProcess

            for (int i = firstIndex; i <= lastIndex; i++) {
                searchOptionsBuilder.filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, listProcessInstances.get(i));
                searchOptionsBuilder.or();
                listInstanceSearched += listProcessInstances.get(i) + ",";
            }
            // remember that we have a or()
            searchOptionsBuilder.filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, -1);
            try {
                searchResult = processAPI.searchArchivedActivities(searchOptionsBuilder.done());
                for (ArchivedActivityInstance archiveInstance : searchResult.getResult()) {
                    Long nbExecution = activitiesExecutedNb.get(archiveInstance.getFlownodeDefinitionId());
                    nbExecution = Long.valueOf(nbExecution == null ? 1 : nbExecution.longValue() + 1);
                    activitiesExecutedNb.put(archiveInstance.getFlownodeDefinitionId(), nbExecution);
                }
            } catch (Exception e) {
                logger.severe("MeteorDefProcess.calculActivity " + e.toString());
                mListEvents.add(new BEvent(EventSearchActivities, e, listInstanceSearched));
                searchResult = null; // stop the loop
            }
            activityIndex += pageActivity;
        } while (searchResult != null && searchResult.getResult().size() == pageActivity && protectTheLoop < 1000);
    }
}
