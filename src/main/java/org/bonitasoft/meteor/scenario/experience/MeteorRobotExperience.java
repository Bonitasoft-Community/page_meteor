package org.bonitasoft.meteor.scenario.experience;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseContract;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.flownode.ActivityStates;
import org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorConst;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.scenario.experience.MeteorTimeLine.TimeLineStep;

public class MeteorRobotExperience extends MeteorRobot {

    private final static Logger logger = Logger.getLogger(MeteorRobotExperience.class.getName());

    private static String loggerLabel = "MeteorRobotExperience ##";

    private APIAccessor apiAccessor = null;

    private MeteorTimeLine meteorTimeLine;

    private List<Long> mListProcessInstanceCreated = new ArrayList<>();

    public MeteorRobotExperience(String robotName, MeteorTimeLine meteorTimeLine, MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        super(robotName, meteorSimulation, apiAccessor);
        this.meteorTimeLine = meteorTimeLine;
        this.apiAccessor = apiAccessor;
        this.meteorSimulation = meteorSimulation;
        this.mTitle = "Experience";
    }

    @Override
    public void executeRobot() {
        setStatus(MeteorConst.ROBOTSTATUS.STARTED);
        mTitle = "Experience:" + meteorTimeLine.getName() + " #" + getRobotId();
        setSignatureInfo("Experience 1." + meteorTimeLine.getName());
        mLogExecution.addLog("StartedRobotExperience [" + mTitle + "]");
        try {
            // find a userId to assign task
            Set<Long> setTasksExecuted = new HashSet<>();
            mLogExecution.addLog("meteorTimeLine? " + (meteorTimeLine == null ? "null" : "Yes"));
            mLogExecution.addLog("getListTimeLineSteps ? " + (meteorTimeLine.getListTimeLineSteps() == null ? "null" : meteorTimeLine.getListTimeLineSteps().size()));

            mCollectPerformance.mOperationTotal = (meteorTimeLine.getListTimeLineSteps().size() + 1) * meteorTimeLine.getNbCases();

            mLogExecution.addLog("Call getprocessAPI ");

            ProcessAPI processAPI = apiAccessor.getProcessAPI();
            mLogExecution.addLog("ProcessAPI ? " + (processAPI == null ? "null" : "Yes"));

            Map<String, Serializable> mapContract = null;
            mLogExecution.addLog("Create case - get InstantiationContract");

            mapContract = CaseContract.recalculateContractValue(meteorTimeLine.getListContractValues());
            mLogExecution.addLog("Contract Ready, create " + meteorTimeLine.getNbCases() + " cases sleep[" + meteorTimeLine.getDelaySleepMs() + "] ms");

            for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < meteorTimeLine.getNbCases(); mCollectPerformance.mOperationIndex++) {
                if (pleaseStop()) {
                    mLogExecution.addLog(" ** PleaseStop requested ** ");
                    return;
                }
                mLogExecution.addLog("GeneratedCase index: " + (mCollectPerformance.mOperationIndex + 1) + "/" + meteorTimeLine.getNbCases());
                long timeStart = System.currentTimeMillis();
                try {
                    Thread.sleep(meteorTimeLine.getDelaySleepMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mLogExecution.addLog("InterruptedException on getDelaySleepMs() [" + meteorTimeLine.getDelaySleepMs() + "]");

                }
                ProcessInstance processInstance = null;
                Long userIdStartProcess = null;
                mLogExecution.addLog("Find user [" + meteorTimeLine.getUserNameCreatedBy() + "] anyUser? " + meteorTimeLine.isAnyUserCreatedBy());
                if (meteorTimeLine.getUserNameCreatedBy() != null) {
                    userIdStartProcess = findUserId(meteorTimeLine.getUserNameCreatedBy(), meteorTimeLine.isAnyUserCreatedBy());
                }
                mLogExecution.addLog("Start case by user[" + userIdStartProcess + "]");

                if (userIdStartProcess != null) {
                    processInstance = processAPI.startProcessWithInputs(userIdStartProcess, meteorTimeLine.getProcessDefinitionId(), mapContract);
                } else {
                    processInstance = processAPI.startProcessWithInputs(meteorTimeLine.getProcessDefinitionId(), mapContract);
                }
                mLogExecution.addLog("CaseCreated:" + processInstance.getId());
                mListProcessInstanceCreated.add(processInstance.getId());

                mCollectPerformance.collectOneStep(System.currentTimeMillis() - timeStart);
                mLogExecution.addLog("ExecuteSteps :" + meteorTimeLine.getListTimeLineSteps().size());

                // execute the steps now
                for (int i = 0; i < meteorTimeLine.getListTimeLineSteps().size(); i++) {
                    if (pleaseStop()) {
                        mLogExecution.addLog("PleaseStop requested");
                        return;
                    }

                    TimeLineStep timeLine = meteorTimeLine.getListTimeLineSteps().get(i);
                    try {
                        Thread.sleep(timeLine.timeWaitms + meteorTimeLine.getTimeBetweenSleepMS());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        mLogExecution.addLog("InterruptedException on TimeLineSteps() [" + (timeLine.timeWaitms + meteorTimeLine.getTimeBetweenSleepMS()) + "]");
                    }
                    timeStart = System.currentTimeMillis();
                    // now search the tasks 
                    SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 100);
                    // 7.9 : ROOT_PROCESS_INSTANCE_ID
                    // 7.5 : HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID but then not working with sub process
                    sob.filter(HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, processInstance.getId());
                    sob.filter(HumanTaskInstanceSearchDescriptor.NAME, timeLine.activityName);
                    sob.filter(HumanTaskInstanceSearchDescriptor.STATE_NAME, ActivityStates.READY_STATE);
                    if (timeLine.isMultiInstanciation) {
                        // we have to search based on the display name too
                        sob.filter(HumanTaskInstanceSearchDescriptor.DISPLAY_NAME, timeLine.getDisplayName());
                    }
                    // sob.sort(HumanTaskInstanceSearchDescriptor.PRIORITY, Order.ASC );
                    mLogExecution.addLog("FilterSearchTask[" + timeLine.activityName + "]");

                    HumanTaskInstance foundHumanTask = null;
                    // flowNodeExecution may failed... even if the task show up, it may failed with an exception 
                    // foundHumanTask."org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException: USERNAME=ConnectorAPIAccessorImpl | 
                    //  org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeExecutionException: Unable to execute flow node 60280 because it is in an incompatible state (transitioning from state ready). 
                    //  Someone probably already called execute on it.
                    // use case : if the same taskName is executed twice. After the first execution, scenario define the same task. So Robot search again using the same task name and find... the previous one !
                    // even the tasks was executed, it's still mark a READY until a Workers change it to a different status...
                    int countExecutionError = 0;
                    boolean failedExecution = true; // go in the loop
                    while (countExecutionError < 5 && failedExecution) {
                        countExecutionError++;

                        int count = 0;
                        while (count < meteorSimulation.getMaxTentatives() && foundHumanTask == null) {
                            count++;
                            mLogExecution.addLog("TentativeSearchTask[" + count + "]");

                            SearchResult<HumanTaskInstance> searchHumanTask = processAPI.searchHumanTaskInstances(sob.done());
                            // protection: if a task already executed show up, just waits. This is possible when the task take time to be executed AND a task name show up multiple time
                            // Bonita keep the state READY until all operations are executed. Robot may then be faster.
                            if (searchHumanTask.getCount() == 0
                                    || (searchHumanTask.getCount() > 0 && setTasksExecuted.contains(searchHumanTask.getResult().get(0).getId()))) {
                                if (meteorSimulation.getDurationOfSimulation() != null && System.currentTimeMillis() > meteorSimulation.getDurationOfSimulation()) {
                                    setStatus(MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION);
                                    addError("Wait too long [" + timeLine.activityName + "] is expected and never show up");

                                    return;
                                }
                                try {
                                    Thread.sleep(meteorSimulation.getSleepBetweenTwoTentatives());
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    mLogExecution.addLog("InterruptedException on SleepBetweenTentative[" + (meteorSimulation.getSleepBetweenTwoTentatives()) + "]");
                                }
                            } else {
                                foundHumanTask = searchHumanTask.getResult().get(0);
                                mLogExecution.addLog("FoundHumanTask Id[" + foundHumanTask.getId() + "]");
                            }
                        } // end search humanTasks 
                        if (foundHumanTask == null) {
                            setStatus(MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION);
                            addError("Task [" + timeLine.activityName + "] is expected and never show up");
                            return;
                        } else {
                            // execute it
                            Map<String, Serializable> mapContractTask = CaseContract.recalculateContractValue(timeLine.listContractValues);
                            failedExecution = false;

                            try {
                                // if the getExecutedBy return null, then we use the first user we found
                                logger.info(loggerLabel + " try to execute task " + foundHumanTask.getId() + " [" + foundHumanTask.getName() + "]");
                                Long userIdTask = findUserId(timeLine.executedByUserName, timeLine.anyUser);
                                if (userIdTask == null) {
                                    if (timeLine.anyUser) {
                                        mLogExecution.addEvent(new BEvent(MeteorSimulation.EventNoActiveUser, getEventInformation(i) + "] ActivityId[" + (foundHumanTask == null ? null : foundHumanTask.getId()) + "]"));
                                        addError("Task [" + timeLine.activityName + "], can't be executed, no active user");
                                    } else {
                                        mLogExecution.addEvent(new BEvent(MeteorSimulation.EventUnknownUser, getEventInformation(i) + "] UserName[" + timeLine.executedByUserName + "] ActivityId[" + (foundHumanTask == null ? null : foundHumanTask.getId()) + "]"));
                                        addError("Task [" + timeLine.activityName + "], UserName[" + timeLine.executedByUserName + "] does not exists");
                                    }
                                    setStatus(MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION);
                                    return;
                                }
                                processAPI.assignUserTask(foundHumanTask.getId(), userIdTask);

                                mLogExecution.addLog("Assigned task " + foundHumanTask.getId() + " [" + foundHumanTask.getName() + "] ok");
                                processAPI.executeUserTask(userIdTask, foundHumanTask.getId(), mapContractTask);
                                mLogExecution.addLog("Executed task " + foundHumanTask.getId() + " [" + foundHumanTask.getName() + "] with success");
                                setTasksExecuted.add(foundHumanTask.getId());
                            } catch (FlowNodeExecutionException fe) {
                                if (countExecutionError > 2)
                                    mLogExecution.addEvent(new BEvent(MeteorSimulation.EventFlowNodeExecution, getEventInformation(i) + "] ActivityId[" + (foundHumanTask == null ? null : foundHumanTask.getId()) + "] e:" + fe.getMessage()));
                                failedExecution = true;
                                foundHumanTask = null;
                                Thread.sleep(500);
                            } catch (Exception e) {
                                mLogExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, getEventInformation(i) + "] ActivityId[" + (foundHumanTask == null ? null : foundHumanTask.getId()) + "] e:" + e.getMessage()));
                                addError("Task [" + timeLine.activityName + "], can't be executed, contract violation " + e.toString());
                                setStatus(MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION);

                                return;
                            }
                        }
                    }
                    mCollectPerformance.collectOneStep(System.currentTimeMillis() - timeStart);
                } // end execute a timeLine 
                mLogExecution.addLog("End case index: " + (mCollectPerformance.mOperationIndex + 1) + "/" + meteorTimeLine.getNbCases());

            } // end execute nbCases
            mLogExecution.addLog("End All cases");
            setStatus(MeteorConst.ROBOTSTATUS.DONE);

        } catch (ContractViolationException vc) {
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, getEventInformation(-1) + " Message=" + vc.getMessage()));
            setStatus(MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION);
            addError("Contract violation, can't be executed " + vc.toString());

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + getSignature() + " exception " + e.toString() + " at " + sw.toString());
            // not yet logged ? Add in the logExecution
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "Robot #:[" + getSignature() + "]:" + e.getMessage()));
            setStatus(MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION);
            addError("Exception, can't be executed " + e.toString() + " at " + sw.toString());
        }
    }

    /**
     * return the information to add to any event
     * 
     * @param timeLineStep
     * @return
     */
    private String getEventInformation(int timeLineStep) {
        StringBuilder information = new StringBuilder();
        information.append("ProcessDefinition[" + meteorTimeLine.getProcessDefinitionId() + "]");
        if (timeLineStep >= 0) {
            TimeLineStep timeLine = meteorTimeLine.getListTimeLineSteps().get(timeLineStep);
            information.append(" TimeLineStep[" + timeLineStep + "] ActivityName[" + timeLine.activityName + "]");
        }
        return information.toString();
    }

    public List<Long> getListProcessInstanceCreated() {
        return mListProcessInstanceCreated;
    }

    public MeteorTimeLine getMeteorTimeLine() {
        return meteorTimeLine;
    }

    /**
     * Find UserID
     * 
     * @param userName
     * @param anyUser
     * @return
     */
    private Long findUserId(String userName, boolean anyUser) {
        IdentityAPI identityAPI = apiAccessor.getIdentityAPI();
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 5);
        sob.filter(UserSearchDescriptor.ENABLED, Boolean.TRUE);
        if (userName != null) {
            sob.filter(UserSearchDescriptor.USER_NAME, userName);
            mLogExecution.addLog("findUserId filter(userName[" + userName + "])");
        }
        SearchResult<User> searchUser;
        try {
            
            searchUser = identityAPI.searchUsers(sob.done());
            

            if (searchUser.getCount() > 0) {
                mLogExecution.addLog("finUserId: return userId[" + searchUser.getResult().get(0).getId() + "]");
                return searchUser.getResult().get(0).getId();
            }
            // no user is found? So pickup any users ?
            if (anyUser) {
                mLogExecution.addLog("finUserId: findAnyUser");
                sob = new SearchOptionsBuilder(0, 5);
                sob.filter(UserSearchDescriptor.ENABLED,  Boolean.TRUE);
                sob.sort(UserSearchDescriptor.USER_NAME, Order.ASC);
                mLogExecution.addLog("finUserId: beforeSearch");
                searchUser = identityAPI.searchUsers(sob.done());
                mLogExecution.addLog("finUserId: afterSearch count=" + searchUser.getCount());
                if (searchUser.getCount() > 0) {
                    mLogExecution.addLog("findUserId: return userId[" + searchUser.getResult().get(0).getId() + "]");
                    return searchUser.getResult().get(0).getId();
                }
            }
            mLogExecution.addLog("finUserId: return null");
            return null;
        } catch (SearchException e) {
            mLogExecution.addLog("SearchException " + e.getMessage());
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "Robot #:[" + getSignature() + "] can't find a user " + e.getMessage()));
            return null;
        } catch (Exception e) {
            mLogExecution.addLog("Exception " + e.getMessage());
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "Robot #:[" + getSignature() + "] can't find a user " + e.getMessage()));
            return null;
        } catch (Error e) {
            mLogExecution.addLog("Error " + e.getMessage());
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, "Robot #:[" + getSignature() + "] can't find a user " + e.getMessage()));
            return null;
        }
    }
}
