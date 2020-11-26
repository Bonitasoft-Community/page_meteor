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
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
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

    }

    @Override
    public void executeRobot() {
        mStatus = MeteorConst.ROBOTSTATUS.STARTED;
        mCollectPerformance.mTitle = "EXECUTE EXPERIENCE1: " + meteorTimeLine.getName() + " #" + getRobotId();
        setSignatureInfo("Experience "+ meteorTimeLine.getName());

        // find a userId to assign task
       
        Set<Long> setTasksExecuted = new HashSet<> ();
        
        mCollectPerformance.mOperationTotal = (meteorTimeLine.getListTimeLineSteps().size() + 1) * meteorTimeLine.getNbCases();
        ProcessAPI processAPI = apiAccessor.getProcessAPI();
        Map<String, Serializable> mapContract=null;
        try {
            mapContract = CaseContract.recalculateContractValue( meteorTimeLine.getListContractValues());

            for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < meteorTimeLine.getNbCases(); mCollectPerformance.mOperationIndex++) {
                long timeStart = System.currentTimeMillis();
                try 
                {
                    Thread.sleep(meteorTimeLine.getDelaySleepMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ProcessInstance processInstance =null;
                if (meteorTimeLine.getUserNameCreatedBy() !=null)
                {
                    Long userIdStartProcess = findUserId(meteorTimeLine.getUserNameCreatedBy() );
                    processInstance = processAPI.startProcessWithInputs(userIdStartProcess, meteorTimeLine.getProcessDefinitionId(), mapContract);
                }
                else {
                    processInstance = processAPI.startProcessWithInputs( meteorTimeLine.getProcessDefinitionId(), mapContract);
                }
                mLogExecution.addLog("Case:" + processInstance.getId());
                mListProcessInstanceCreated.add( processInstance.getId() );
                
                mCollectPerformance.collectOneStep(System.currentTimeMillis() - timeStart);
                // execute the steps now
                for (int i = 0; i < meteorTimeLine.getListTimeLineSteps().size(); i++) {
                    TimeLineStep timeLine = meteorTimeLine.getListTimeLineSteps().get(i);
                    try {
                        Thread.sleep(timeLine.timeWaitms + meteorTimeLine.getTimeBetweenSleepMS());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    timeStart = System.currentTimeMillis();
                    // now search the tasks 
                    SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 100);
                    // 7.9 : ROOT_PROCESS_INSTANCE_ID
                    // 7.5 : HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID but then not working with sub process
                    sob.filter(HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, processInstance.getId());
                    sob.filter(HumanTaskInstanceSearchDescriptor.NAME, timeLine.activityName);
                    sob.filter(HumanTaskInstanceSearchDescriptor.STATE_NAME, ActivityStates.READY_STATE);

                    HumanTaskInstance foundHumanTask = null;
                    // flowNodeExecution may failed... even if the task show up, it may failed with an exception 
                    // foundHumanTask."org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException: USERNAME=ConnectorAPIAccessorImpl | 
                    //  org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeExecutionException: Unable to execute flow node 60280 because it is in an incompatible state (transitioning from state ready). 
                    //  Someone probably already called execute on it.
                    // use case : if the same taskName is executed twice. After the first execution, scenario define the same task. So Robot search again using the same task name and find... the previous one !
                    // even the tasks was executed, it's still mark a READY until a Workers change it to a different status...
                    int countExecutionError = 0;
                    boolean failedExecution=true; // go in the loop
                    while (countExecutionError < 5 && failedExecution) {
                        countExecutionError++;
                        
                        int count = 0;
                        while (count < meteorSimulation.getMaxTentatives() && foundHumanTask == null) {
                            count++;
                            SearchResult<HumanTaskInstance> searchHumanTask = processAPI.searchHumanTaskInstances(sob.done());
                            // protection: if a task already executed show up, just waits. This is possible when the task take time to be executed AND a task name show up multiple time
                            // Bonita keep the state READY until all operations are executed. Robot may then be faster.
                            if (searchHumanTask.getCount() == 0 
                                    || (searchHumanTask.getCount()>0 && setTasksExecuted.contains(searchHumanTask.getResult().get(0).getId()))) {
                                if (meteorSimulation.getDurationOfSimulation() != null && System.currentTimeMillis() > meteorSimulation.getDurationOfSimulation()) {
                                    mStatus = MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION;                                    return;
                                }
                                try {
                                    Thread.sleep( meteorSimulation.getSleepBetweenTwoTentatives() );
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            } else
                                foundHumanTask = searchHumanTask.getResult().get(0);
                        } // end search humanTasks 
                        if (foundHumanTask == null) {
                            mStatus = MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION;
                            addError("Task ["+timeLine.activityName+"] is expected and never show up");
                            return; 
                        } else {
                            // execute it
                            Map<String, Serializable> mapContractTask = CaseContract.recalculateContractValue(timeLine.listContractValues);
                            failedExecution=false;

                            try {
                                // if the getExecutedBy return null, then we use the first user we found
                                logger.info( loggerLabel+" try to execute task "+foundHumanTask.getId()+" ["+foundHumanTask.getName()+"]");
                                Long userIdTask = findUserId(timeLine.executedByUserName );
                                processAPI.assignUserTask(foundHumanTask.getId(), userIdTask);
                                logger.info( loggerLabel+" assigned task "+foundHumanTask.getId()+" ["+foundHumanTask.getName()+"] ok");
                                processAPI.executeUserTask(userIdTask, foundHumanTask.getId(), mapContractTask);
                                logger.info( loggerLabel+" Executed task "+foundHumanTask.getId()+" ["+foundHumanTask.getName()+"] with success");
                                mLogExecution.addLog("Task:"+foundHumanTask.getName()+"("+foundHumanTask.getId()+");");
                                setTasksExecuted.add(foundHumanTask.getId());
                            }
                            catch (FlowNodeExecutionException fe ) {
                                if (countExecutionError >2)
                                    mLogExecution.addEvent(new BEvent(MeteorSimulation.EventFlowNodeExecution, getEventInformation( i )+"] ActivityId[" + (foundHumanTask==null ? null : foundHumanTask.getId())+"] e:"+fe.getMessage()));
                                failedExecution=true;
                                foundHumanTask=null;
                                Thread.sleep(500);
                            } catch (Exception e) {
                                mLogExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, getEventInformation( i )+"] ActivityId[" + (foundHumanTask==null ? null : foundHumanTask.getId()) + "] e:"+e.getMessage()));
                                addError("Task ["+timeLine.activityName+"], can't be executed, contract violation "+e.toString());
                                return;
                            }
                        }
                    }
                    mCollectPerformance.collectOneStep( System.currentTimeMillis() - timeStart);
                } // end execute a timeLine 
            } // end execute nbCases
            mStatus = MeteorConst.ROBOTSTATUS.DONE;

        } catch (ContractViolationException vc) {
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, getEventInformation(-1)+" Message="+vc.getMessage()));
            mStatus = MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION;
            addError("Contract violation, can't be executed "+vc.toString());


        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + getSignature() + " exception " + e.toString() + " at " + sw.toString());
            // not yet logged ? Add in the logExecution
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "Robot #:[" + getSignature() + "]:"+e.getMessage()));
            mStatus = MeteorConst.ROBOTSTATUS.INCOMPLETEEXECUTION;
            addError("Exception, can't be executed "+e.toString());
        }
    }
    
    /**
     * return the information to add to any event
     * @param timeLineStep
     * @return
     */
    private String getEventInformation(int timeLineStep) {
        StringBuilder information = new StringBuilder();
        information.append("ProcessDefinition[" + meteorTimeLine.getProcessDefinitionId() + "]");
        if (timeLineStep>=0)
        {
            TimeLineStep timeLine = meteorTimeLine.getListTimeLineSteps().get(timeLineStep);
            information.append(" TimeLineStep["+timeLineStep+"] ActivityName["+timeLine.activityName+"]");
        }
        return information.toString();
    }
    public List<Long> getListProcessInstanceCreated() {
        return mListProcessInstanceCreated;
    }
    
    public MeteorTimeLine getMeteorTimeLine() {
        return meteorTimeLine;
    }
    private Long findUserId( String userName ) {
        IdentityAPI identityAPI = apiAccessor.getIdentityAPI();
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0,5);
        sob.filter(UserSearchDescriptor.ENABLED, true);
        if (userName != null)
            sob.filter(UserSearchDescriptor.USER_NAME, userName );
        SearchResult<User> searchUser;
        try {
            searchUser = identityAPI.searchUsers( sob.done());
            if (searchUser.getCount()>0)
                return searchUser.getResult().get(0).getId();
        } catch (SearchException e) {
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "Robot #:[" + getSignature() + "] can't find a user "+e.getMessage()));

        }
        return null;
        
    }
}
